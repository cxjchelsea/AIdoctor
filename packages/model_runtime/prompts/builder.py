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
        checksum = compute_prompt_checksum(document)
        return RenderedPrompt(
            prompt_id=document.spec.prompt_id,
            version=document.spec.version,
            messages=rendered_messages,
            resource_checksum=checksum,
            manifest=FrozenJsonObject(
                {
                    "prompt_id": document.spec.prompt_id,
                    "prompt_version": document.spec.version,
                    "resource_checksum": checksum,
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
        try:
            return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False, allow_nan=False)
        except (TypeError, ValueError) as exc:
            raise PromptRuntimeError(
                PromptErrorCode.PROMPT_VARIABLE_TYPE_INVALID,
                f"variable {name} must contain finite JSON values",
            ) from exc
