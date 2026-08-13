"""Minimal deterministic renderer for validated non-clinical Prompt documents."""

from __future__ import annotations

import json
import math
import re
from collections.abc import Mapping
from typing import Any

from ..api.types import FrozenJsonObject
from .errors import PromptErrorCode, PromptRuntimeError
from .loader import compute_prompt_checksum
from .models import PromptDocument, RenderedMessage, RenderedPrompt


PLACEHOLDER_PATTERN = re.compile(r"\$\{([a-z][a-z0-9_]*)\}")


class PromptBuilder:
    """Render local Prompt artifacts without execution, providers, or runtime context access."""

    def build(self, document: PromptDocument, variables: Mapping[str, Any]) -> RenderedPrompt:
        # 公开 Builder 边界必须自行 fail-closed：不得假设调用方已走过 Loader
        actual_checksum = compute_prompt_checksum(document)
        declared_checksum = document.spec.checksum.removeprefix("sha256:")
        if declared_checksum != actual_checksum:
            raise PromptRuntimeError(
                PromptErrorCode.PROMPT_CHECKSUM_MISMATCH,
                f"checksum mismatch for {document.spec.prompt_id}@{document.spec.version}",
            )

        declarations = {item.name: item for item in document.spec.variables}
        supplied = set(variables)
        extra = supplied - declarations.keys()
        if extra:
            raise PromptRuntimeError(PromptErrorCode.PROMPT_EXTRA_VARIABLE, f"extra variables: {sorted(extra)}")
        missing = {name for name, declaration in declarations.items() if declaration.required and name not in supplied}
        if missing:
            raise PromptRuntimeError(
                PromptErrorCode.PROMPT_REQUIRED_VARIABLE_MISSING,
                f"required variables missing: {sorted(missing)}",
            )
        rendered_values = {
            name: self._validate_and_render_value(name, declarations[name].value_type, value)
            for name, value in variables.items()
        }
        rendered_messages = tuple(
            RenderedMessage(role=message.role, content=self._render_template(message.content, rendered_values))
            for message in document.messages
        )
        return RenderedPrompt(
            prompt_id=document.spec.prompt_id,
            version=document.spec.version,
            messages=rendered_messages,
            resource_checksum=actual_checksum,
            manifest=FrozenJsonObject(
                {
                    "prompt_id": document.spec.prompt_id,
                    "prompt_version": document.spec.version,
                    "resource_checksum": actual_checksum,
                }
            ),
        )

    @staticmethod
    def _render_template(template: str, values: Mapping[str, str]) -> str:
        def replace(match: re.Match[str]) -> str:
            name = match.group(1)
            if name not in values:
                raise PromptRuntimeError(PromptErrorCode.PROMPT_RENDER_INVALID, f"unknown placeholder: {name}")
            return values[name]

        rendered = PLACEHOLDER_PATTERN.sub(replace, template)
        if "${" in rendered:
            raise PromptRuntimeError(PromptErrorCode.PROMPT_RENDER_INVALID, "malformed or unsupported placeholder")
        return rendered

    @staticmethod
    def _validate_json_value(value: Any, *, path: str) -> None:
        """递归严格校验 JSON-native 值；禁止依赖 json.dumps 隐式 coercion。"""

        value_type = type(value)
        if value is None or value_type is str or value_type is bool or value_type is int:
            return
        if value_type is float:
            if not math.isfinite(value):
                raise PromptRuntimeError(
                    PromptErrorCode.PROMPT_VARIABLE_TYPE_INVALID,
                    f"non-finite JSON number at {path}",
                )
            return
        if value_type is list:
            for index, item in enumerate(value):
                PromptBuilder._validate_json_value(item, path=f"{path}[{index}]")
            return
        if value_type is dict:
            for key, item in value.items():
                if type(key) is not str:
                    raise PromptRuntimeError(
                        PromptErrorCode.PROMPT_VARIABLE_TYPE_INVALID,
                        f"object keys must be strings at {path}",
                    )
                PromptBuilder._validate_json_value(item, path=f"{path}.{key}")
            return
        raise PromptRuntimeError(
            PromptErrorCode.PROMPT_VARIABLE_TYPE_INVALID,
            f"non JSON-native value at {path}: {value_type.__name__}",
        )

    @staticmethod
    def _validate_and_render_value(name: str, value_type: str, value: Any) -> str:
        valid = {
            "string": lambda item: type(item) is str,
            "integer": lambda item: type(item) is int,
            "number": lambda item: type(item) in {int, float} and not (type(item) is float and not math.isfinite(item)),
            "boolean": lambda item: type(item) is bool,
            "object": lambda item: type(item) is dict,
            "array": lambda item: type(item) is list,
        }[value_type](value)
        if not valid:
            raise PromptRuntimeError(
                PromptErrorCode.PROMPT_VARIABLE_TYPE_INVALID,
                f"variable {name} must be {value_type}",
            )
        if value_type == "string":
            return value
        # object/array：先严格递归校验，再做 canonical serialization
        if value_type in {"object", "array"}:
            PromptBuilder._validate_json_value(value, path=name)
        else:
            # number/integer/boolean 的嵌套不适用；标量已在上方校验
            pass
        try:
            return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False, allow_nan=False)
        except (TypeError, ValueError) as exc:
            raise PromptRuntimeError(
                PromptErrorCode.PROMPT_VARIABLE_TYPE_INVALID,
                f"variable {name} must contain finite JSON values",
            ) from exc
