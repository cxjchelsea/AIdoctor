"""Strict loader for explicitly registered packaged Prompt resources."""

from __future__ import annotations

import hashlib
import json
from importlib import resources
from pathlib import Path, PurePosixPath
from typing import Any

import yaml
from pydantic import ValidationError
from yaml.constructor import ConstructorError

from .errors import PromptErrorCode, PromptRuntimeError
from .models import PromptDocument
from .registry import PromptRegistry


PROMPT_RESOURCE_PACKAGE = "packages.model_runtime.resources.prompts"


class StrictPromptSafeLoader(yaml.SafeLoader):
    """SafeLoader that also rejects ambiguous mappings."""


def _construct_unique_string_mapping(
    loader: StrictPromptSafeLoader, node: yaml.MappingNode, deep: bool = False
) -> dict[str, Any]:
    seen: set[str] = set()
    result: dict[str, Any] = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if type(key) is not str:
            raise ConstructorError(None, None, "mapping keys must be strings", key_node.start_mark)
        if key in seen:
            raise ConstructorError(None, None, f"duplicate mapping key: {key}", key_node.start_mark)
        seen.add(key)
        result[key] = loader.construct_object(value_node, deep=deep)
    return result


StrictPromptSafeLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
    _construct_unique_string_mapping,
)


def parse_prompt_document(raw: bytes) -> PromptDocument:
    """Decode, strictly parse, and validate exactly one Prompt document."""

    try:
        text = raw.decode("utf-8-sig")
    except UnicodeDecodeError as exc:
        raise PromptRuntimeError(PromptErrorCode.PROMPT_SCHEMA_INVALID, "resource must be strict UTF-8") from exc
    try:
        documents = list(yaml.load_all(text, Loader=StrictPromptSafeLoader))
    except yaml.YAMLError as exc:
        raise PromptRuntimeError(PromptErrorCode.PROMPT_SCHEMA_INVALID, f"invalid YAML: {exc}") from exc
    if len(documents) != 1:
        raise PromptRuntimeError(PromptErrorCode.PROMPT_SCHEMA_INVALID, "exactly one YAML document is required")
    try:
        return PromptDocument.model_validate(documents[0])
    except ValidationError as exc:
        raise PromptRuntimeError(PromptErrorCode.PROMPT_SCHEMA_INVALID, str(exc)) from exc


def canonical_prompt_payload(document: PromptDocument) -> bytes:
    """Return checksum-authoritative canonical JSON without spec.checksum."""

    data = document.model_dump(mode="json")
    del data["spec"]["checksum"]
    try:
        serialized = json.dumps(
            data,
            sort_keys=True,
            separators=(",", ":"),
            ensure_ascii=False,
            allow_nan=False,
        )
    except (TypeError, ValueError) as exc:
        raise PromptRuntimeError(PromptErrorCode.PROMPT_SCHEMA_INVALID, "document is not canonical JSON") from exc
    return serialized.encode("utf-8")


def compute_prompt_checksum(document: PromptDocument) -> str:
    return hashlib.sha256(canonical_prompt_payload(document)).hexdigest()


class PromptLoader:
    """Load exact Registry entries from one fixed resource package."""

    __slots__ = ("_registry",)

    def __init__(self, registry: PromptRegistry) -> None:
        self._registry = registry

    def load(self, prompt_id: str, prompt_version: str) -> PromptDocument:
        entry = self._registry.get(prompt_id, prompt_version)
        resource = self._validate_resource_identifier(entry.resource)
        try:
            root = resources.files(PROMPT_RESOURCE_PACKAGE)
        except (ImportError, ModuleNotFoundError) as exc:
            raise PromptRuntimeError(PromptErrorCode.PROMPT_RESOURCE_NOT_FOUND, "prompt resource package is missing") from exc
        target = root.joinpath(*resource.parts)
        self._validate_resolved_resource(root, target)
        try:
            if not target.is_file():
                raise PromptRuntimeError(PromptErrorCode.PROMPT_RESOURCE_NOT_FOUND, f"resource not found: {entry.resource}")
            raw = target.read_bytes()
        except PromptRuntimeError:
            raise
        except OSError as exc:
            raise PromptRuntimeError(PromptErrorCode.PROMPT_RESOURCE_NOT_FOUND, f"resource unreadable: {entry.resource}") from exc
        document = parse_prompt_document(raw)
        if document.spec.prompt_id != entry.prompt_id or document.spec.version != entry.version:
            raise PromptRuntimeError(PromptErrorCode.PROMPT_SCHEMA_INVALID, "resource identity does not match registry entry")
        actual = compute_prompt_checksum(document)
        expected = document.spec.checksum.removeprefix("sha256:")
        if actual != expected:
            raise PromptRuntimeError(
                PromptErrorCode.PROMPT_CHECKSUM_MISMATCH,
                f"checksum mismatch for {entry.prompt_id}@{entry.version}",
            )
        return document

    @staticmethod
    def _validate_resource_identifier(value: str) -> PurePosixPath:
        resource = PurePosixPath(value)
        if resource.is_absolute() or len(resource.parts) != 2 or ".." in resource.parts or resource.suffix != ".yaml":
            raise PromptRuntimeError(PromptErrorCode.PROMPT_RESOURCE_FORBIDDEN, "invalid prompt resource identifier")
        if any(part in {"", "."} for part in resource.parts):
            raise PromptRuntimeError(PromptErrorCode.PROMPT_RESOURCE_FORBIDDEN, "invalid prompt resource identifier")
        return resource

    @staticmethod
    def _validate_resolved_resource(root: Any, target: Any) -> None:
        if isinstance(root, Path) and isinstance(target, Path):
            root_resolved = root.resolve()
            target_resolved = target.resolve()
            try:
                target_resolved.relative_to(root_resolved)
            except ValueError as exc:
                raise PromptRuntimeError(PromptErrorCode.PROMPT_RESOURCE_FORBIDDEN, "resource escapes package root") from exc
            if target.is_symlink():
                raise PromptRuntimeError(PromptErrorCode.PROMPT_RESOURCE_FORBIDDEN, "symlink resources are forbidden")
