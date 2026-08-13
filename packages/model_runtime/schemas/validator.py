"""Thin adapter that validates payloads against Shared Contracts v1 authority."""

from __future__ import annotations

import copy
import importlib.util
from collections.abc import Mapping
from pathlib import Path
from types import ModuleType
from typing import Any

from .errors import SchemaRegistryError, SchemaRegistryErrorCode
from .registry import OutputSchemaRegistry, shared_contracts_v1_root


def _load_shared_validator_module() -> ModuleType:
    """Load repository-owned contracts/v1/validator/validate_contracts.py only."""

    root = shared_contracts_v1_root()
    module_path = (root / "validator" / "validate_contracts.py").resolve()
    try:
        module_path.relative_to(root)
    except ValueError as exc:
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "Shared Contracts v1 validator path escapes repository-owned root",
        ) from exc
    if not module_path.is_file():
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "Shared Contracts v1 validator module is missing under repository-owned root",
        )
    spec = importlib.util.spec_from_file_location(
        "aidoctor_shared_contracts_v1_validate_contracts",
        module_path,
    )
    if spec is None or spec.loader is None:
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "unable to load Shared Contracts v1 validator module",
        )
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _safe_schema_error_message(error: Any, pointer_fn: Any) -> str:
    """Deterministic schema error without echoing candidate instance values."""

    path = pointer_fn(error.absolute_path)
    validator = getattr(error, "validator", "schema")
    # 不使用默认 error.message（可能回显实例值）；仅保留 keyword/path/类型信息
    instance_type = type(getattr(error, "instance", None)).__name__
    return f"{validator}:{path}:type={instance_type}"


class SharedContractValidator:
    """Observational fail-closed validator; never mutates input or coerces validity."""

    __slots__ = ("_registry", "_contracts_root", "_shared", "_manifest", "_schemas", "_schema_registry")

    def __init__(self, registry: OutputSchemaRegistry) -> None:
        # REREV-F003：拒绝 subclass / duck-typed registry
        if type(registry) is not OutputSchemaRegistry:
            raise TypeError("registry must be exact OutputSchemaRegistry")
        if len(registry) != 13:
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
                "OutputSchemaRegistry must be the exact Shared Contracts v1 catalog",
            )
        root = shared_contracts_v1_root()
        shared = _load_shared_validator_module()
        # load_package 使用模块内 ROOT；必须与 repository-owned root 一致
        module_root = Path(shared.ROOT).resolve()
        if module_root != root:
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
                "loaded validator ROOT is not repository-owned contracts/v1",
            )
        manifest, schemas, _valid, _invalid = shared.load_package()
        for contract_id in registry.list_ids():
            entry = registry.get(contract_id, "1.0.0")
            manifest_name = registry.manifest_name(contract_id)
            if manifest_name not in schemas:
                raise SchemaRegistryError(
                    SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
                    f"registry entry missing from Shared Contracts schemas: {manifest_name}",
                )
            expected_path = next(
                item["path"] for item in manifest["contracts"] if item["name"] == manifest_name
            )
            if entry.schema_path != expected_path:
                raise SchemaRegistryError(
                    SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
                    f"schema_path mismatch for {contract_id}",
                )
        object.__setattr__(self, "_registry", registry)
        object.__setattr__(self, "_contracts_root", root)
        object.__setattr__(self, "_shared", shared)
        object.__setattr__(self, "_manifest", manifest)
        object.__setattr__(self, "_schemas", schemas)
        object.__setattr__(self, "_schema_registry", shared.build_registry(schemas))

    def __setattr__(self, _name: str, _value: object) -> None:
        raise TypeError("SharedContractValidator is immutable")

    def validate(
        self,
        contract_id: str,
        version: str,
        payload: Mapping[str, Any],
    ) -> tuple[bool, tuple[str, ...]]:
        """Validate a candidate payload; returns (valid, deterministic errors)."""

        if not isinstance(payload, Mapping):
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_VALIDATION_INVALID,
                "payload must be a JSON object mapping",
            )
        # 观测性校验：深拷贝后验证，保证调用方对象不被修改
        candidate = copy.deepcopy(dict(payload))
        self._registry.get(contract_id, version)
        manifest_name = self._registry.manifest_name(contract_id)

        version_errors = list(self._shared.validate_exact_version(self._manifest, manifest_name, candidate))
        schema_errors = [
            _safe_schema_error_message(error, self._shared.pointer)
            for error in self._shared.validator_for(
                manifest_name, self._schemas, self._schema_registry
            ).iter_errors(candidate)
        ]
        # F003：结构/版本失败后不得进入语义层
        if version_errors or schema_errors:
            ordered = tuple(sorted({*version_errors, *schema_errors}))
            return False, ordered

        try:
            semantic = list(self._shared.semantic_errors(manifest_name, candidate))
        except Exception:  # noqa: BLE001 - 归一化为确定性 invalid，禁止 raw escape
            semantic = ["semantic:validation-failed"]
        ordered = tuple(sorted(set(semantic)))
        return (len(ordered) == 0, ordered)
