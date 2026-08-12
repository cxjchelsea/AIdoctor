"""Minimal shared value types and validators for model-runtime structures."""

from __future__ import annotations

import math
import re
from collections.abc import Iterator, Mapping
from types import MappingProxyType
from typing import Any

from pydantic_core import core_schema


IDENTIFIER_PATTERN = re.compile(r"^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$")
SEMVER_PATTERN = re.compile(
    r"^(0|[1-9][0-9]*)\."
    r"(0|[1-9][0-9]*)\."
    r"(0|[1-9][0-9]*)"
    r"(?:-((?:0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)"
    r"(?:\.(?:0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*))*))?"
    r"(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?$"
)


def validate_identifier(value: str) -> str:
    """Validate a stable lowercase structural identifier."""

    if not IDENTIFIER_PATTERN.fullmatch(value):
        raise ValueError("must be a stable lowercase identifier")
    return value


def validate_semver(value: str) -> str:
    """Validate an explicit SemVer 2.0.0-compatible version."""

    if value.lower() == "latest" or not SEMVER_PATTERN.fullmatch(value):
        raise ValueError("must be an explicit semantic version; implicit latest is forbidden")
    return value


FrozenJsonValue = Any


def freeze_json_value(value: Any) -> FrozenJsonValue:
    """Validate and recursively freeze one JSON value."""

    if value is None or type(value) in {bool, int, str}:
        return value
    if type(value) is float:
        if not math.isfinite(value):
            raise ValueError("metadata numbers must be finite")
        return value
    if type(value) in {list, tuple}:
        return tuple(freeze_json_value(item) for item in value)
    if isinstance(value, FrozenJsonObject):
        return value
    if type(value) is dict:
        return FrozenJsonObject(value)
    raise ValueError("metadata must contain JSON-safe values only")


def thaw_json_value(value: FrozenJsonValue) -> Any:
    """Convert an immutable JSON value to its canonical JSON representation."""

    if isinstance(value, FrozenJsonObject):
        return value.to_json_value()
    if isinstance(value, tuple):
        return [thaw_json_value(item) for item in value]
    return value


class FrozenJsonObject(Mapping[str, FrozenJsonValue]):
    """Recursively immutable JSON object with canonical key ordering."""

    __slots__ = ("_items", "_values")

    def __init__(self, value: Mapping[str, Any] | None = None) -> None:
        source = {} if value is None else value
        if isinstance(source, FrozenJsonObject):
            source = source.to_json_value()
        if type(source) is not dict:
            raise ValueError("metadata must be a JSON object")
        if any(type(key) is not str for key in source):
            raise ValueError("metadata object keys must be strings")
        items = tuple((key, freeze_json_value(source[key])) for key in sorted(source))
        object.__setattr__(self, "_items", items)
        object.__setattr__(self, "_values", MappingProxyType(dict(items)))

    def __setattr__(self, _name: str, _value: Any) -> None:
        raise TypeError("FrozenJsonObject is immutable")

    def __getitem__(self, key: str) -> FrozenJsonValue:
        return self._values[key]

    def __iter__(self) -> Iterator[str]:
        return (key for key, _ in self._items)

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"FrozenJsonObject({self.to_json_value()!r})"

    def __eq__(self, other: object) -> bool:
        if isinstance(other, FrozenJsonObject):
            return self._items == other._items
        if isinstance(other, Mapping):
            try:
                return self._items == FrozenJsonObject(other)._items
            except ValueError:
                return False
        return False

    def to_json_value(self) -> dict[str, Any]:
        return {key: thaw_json_value(value) for key, value in self._items}

    @classmethod
    def __get_pydantic_core_schema__(cls, _source_type: Any, _handler: Any) -> core_schema.CoreSchema:
        return core_schema.no_info_plain_validator_function(
            cls,
            serialization=core_schema.plain_serializer_function_ser_schema(
                lambda value: value.to_json_value(),
                return_schema=core_schema.dict_schema(core_schema.str_schema(), core_schema.any_schema()),
                when_used="always",
            ),
        )
