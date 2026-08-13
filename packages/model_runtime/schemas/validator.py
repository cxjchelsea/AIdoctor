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


def _load_shared_validator_module(contracts_root: Path) -> ModuleType:
    """Load contracts/v1/validator/validate_contracts.py without forking semantics."""

    module_path = contracts_root / "validator" / "validate_contracts.py"
    if not module_path.is_file():
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "Shared Contracts v1 validator module is missing",
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


class SharedContractValidator:
    """Observational fail-closed validator; never mutates input or coerces validity."""

    __slots__ = ("_registry", "_contracts_root", "_shared", "_manifest", "_schemas", "_schema_registry")

    def __init__(
        self,
        registry: OutputSchemaRegistry,
        *,
        contracts_root: Path | None = None,
    ) -> None:
        if not isinstance(registry, OutputSchemaRegistry):
            raise TypeError("registry must be an OutputSchemaRegistry")
        root = shared_contracts_v1_root() if contracts_root is None else Path(contracts_root)
        shared = _load_shared_validator_module(root)
        manifest, schemas, _valid, _invalid = shared.load_package()
        # 再校验 registry 与 manifest 一致，防止调用方注入分叉目录
        for contract_id in registry.list_ids():
            entry = registry.get(contract_id, "1.0.0")
            if entry.manifest_name not in schemas:
                raise SchemaRegistryError(
                    SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
                    f"registry entry missing from Shared Contracts schemas: {entry.manifest_name}",
                )
            expected_path = next(
                item["path"] for item in manifest["contracts"] if item["name"] == entry.manifest_name
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
        entry = self._registry.get(contract_id, version)
        manifest_name = entry.manifest_name

        version_errors = list(self._shared.validate_exact_version(self._manifest, manifest_name, candidate))
        schema_errors = [
            f"{error.validator}:{self._shared.pointer(error.absolute_path)}:{error.message}"
            for error in self._shared.validator_for(
                manifest_name, self._schemas, self._schema_registry
            ).iter_errors(candidate)
        ]
        semantic = list(self._shared.semantic_errors(manifest_name, candidate))
        ordered = tuple(sorted({*version_errors, *schema_errors, *semantic}))
        return (len(ordered) == 0, ordered)
