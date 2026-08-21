"""Canonical Shared Contracts request-level validation adapter.

分类：NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF

本模块只加载 contracts/v1/validator/validate_contracts.py
作为唯一语义源，不复制 semantic_errors 规则，
也不在请求路径调用 validate_package()。
"""

from __future__ import annotations

import importlib.util
from collections.abc import Mapping
from functools import lru_cache
from pathlib import Path
from typing import Any


_CANONICAL_VALIDATOR_PATH = (
    Path(__file__).resolve().parents[3]
    / "contracts"
    / "v1"
    / "validator"
    / "validate_contracts.py"
)
_CANONICAL_MODULE_NAME = "aidoctor_canonical_validate_contracts"

# identifier-set.schema.json: optional properties typed as opaqueIdentifier
# (string), not ["string","null"]. Java Feign emits explicit null for unset
# optionals. contract_version is required and is never omitted.
_IDENTIFIER_SET_OPTIONAL_NONNULLABLE_FIELDS = (
    "cdp_id",
    "patient_id",
    "encounter_id",
    "session_id",
    "tenant_id",
    "review_id",
    "delivery_id",
)


@lru_cache(maxsize=1)
def _canonical_validator_module():
    """Process-lifetime load of the canonical validator module."""

    if not _CANONICAL_VALIDATOR_PATH.is_file():
        raise RuntimeError("canonical Shared Contracts validator is missing")
    spec = importlib.util.spec_from_file_location(
        _CANONICAL_MODULE_NAME,
        _CANONICAL_VALIDATOR_PATH,
    )
    if spec is None or spec.loader is None:
        raise RuntimeError("canonical Shared Contracts validator cannot be loaded")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _normalize_tool_context_identifier_nulls(instance: dict[str, Any]) -> dict[str, Any]:
    """Omit ToolContext.identifiers optional/non-nullable fields that are null.

    Justified only by IdentifierSet canonical schema semantics.
    Does not walk other objects or delete schema-allowed explicit nulls.
    anyOf (at least one real identifier) remains the canonical validator's job.
    """

    identifiers = instance.get("identifiers")
    if not isinstance(identifiers, dict):
        return instance
    normalized_ids = dict(identifiers)
    changed = False
    for field in _IDENTIFIER_SET_OPTIONAL_NONNULLABLE_FIELDS:
        if field in normalized_ids and normalized_ids[field] is None:
            del normalized_ids[field]
            changed = True
    if not changed:
        return instance
    normalized = dict(instance)
    normalized["identifiers"] = normalized_ids
    return normalized


def normalize_runtime_contract_instance(name: str, instance: Mapping[str, Any]) -> dict[str, Any]:
    """Narrow compatibility normalization. Not a generic JSON-null eraser."""

    raw = dict(instance)
    if name == "ToolContext":
        return _normalize_tool_context_identifier_nulls(raw)
    return raw


def validate_runtime_contract_instance(name: str, instance: Mapping[str, Any]) -> list[str]:
    """Delegate to canonical validate_contract_instance; no local semantic rules."""

    validator = _canonical_validator_module()
    return list(
        validator.validate_contract_instance(
            name,
            normalize_runtime_contract_instance(name, instance),
        )
    )
