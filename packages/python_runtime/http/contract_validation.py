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


def validate_runtime_contract_instance(name: str, instance: Mapping[str, Any]) -> list[str]:
    """Delegate to canonical validate_contract_instance; no local semantic rules."""

    validator = _canonical_validator_module()
    return list(validator.validate_contract_instance(name, dict(instance)))
