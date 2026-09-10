"""Deterministic Output Schema Registry / Shared Contract adapter errors."""

from __future__ import annotations

from enum import Enum


class SchemaRegistryErrorCode(str, Enum):
    CONTRACT_UNKNOWN = "CONTRACT_UNKNOWN"
    CONTRACT_VERSION_UNKNOWN = "CONTRACT_VERSION_UNKNOWN"
    CONTRACT_REGISTRY_INVALID = "CONTRACT_REGISTRY_INVALID"
    CONTRACT_VALIDATION_INVALID = "CONTRACT_VALIDATION_INVALID"


class SchemaRegistryError(ValueError):
    """One stable schema-registry failure with a machine-readable code."""

    def __init__(self, code: SchemaRegistryErrorCode, detail: str) -> None:
        self.code = code
        self.detail = detail
        super().__init__(f"{code.value}: {detail}")
