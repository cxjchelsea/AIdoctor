"""Immutable exact-version Prompt Registry."""

from __future__ import annotations

from collections.abc import Iterable
from types import MappingProxyType

from .errors import PromptErrorCode, PromptRuntimeError
from .models import PromptRegistryEntry


class PromptRegistry:
    """An immutable-at-construction registry with no discovery or latest lookup."""

    __slots__ = ("_entries", "_prompt_ids")

    def __init__(self, entries: Iterable[PromptRegistryEntry] = ()) -> None:
        indexed: dict[tuple[str, str], PromptRegistryEntry] = {}
        prompt_ids: set[str] = set()
        for entry in entries:
            if not isinstance(entry, PromptRegistryEntry):
                raise TypeError("registry entries must be PromptRegistryEntry instances")
            key = (entry.prompt_id, entry.version)
            if key in indexed:
                raise ValueError(f"duplicate prompt registry entry: {entry.prompt_id}@{entry.version}")
            indexed[key] = entry
            prompt_ids.add(entry.prompt_id)
        object.__setattr__(self, "_entries", MappingProxyType(indexed))
        object.__setattr__(self, "_prompt_ids", frozenset(prompt_ids))

    def __setattr__(self, _name: str, _value: object) -> None:
        raise TypeError("PromptRegistry is immutable")

    def __len__(self) -> int:
        return len(self._entries)

    def contains(self, prompt_id: str, version: str) -> bool:
        return (prompt_id, version) in self._entries

    def get(self, prompt_id: str, version: str) -> PromptRegistryEntry:
        try:
            return self._entries[(prompt_id, version)]
        except KeyError as exc:
            if prompt_id not in self._prompt_ids:
                code = PromptErrorCode.PROMPT_NOT_REGISTERED
                detail = f"prompt is not registered: {prompt_id}"
            else:
                code = PromptErrorCode.PROMPT_VERSION_NOT_REGISTERED
                detail = f"prompt version is not registered: {prompt_id}@{version}"
            raise PromptRuntimeError(code, detail) from exc

    def list_versions(self, prompt_id: str) -> tuple[str, ...]:
        return tuple(sorted(version for current_id, version in self._entries if current_id == prompt_id))
