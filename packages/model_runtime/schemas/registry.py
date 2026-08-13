"""Immutable exact-version Output Schema Registry over Shared Contracts v1."""

from __future__ import annotations

import json
from pathlib import Path
from types import MappingProxyType

from ..api.models import SHARED_CONTRACT_V1_IDS, SHARED_CONTRACT_V1_VERSION
from .errors import SchemaRegistryError, SchemaRegistryErrorCode
from .models import OutputSchemaRegistryEntry


def shared_contracts_v1_root() -> Path:
    """Repository-owned contracts/v1 root relative to this package（唯一权威根）。"""

    # packages/model_runtime/schemas -> repo root
    root = (Path(__file__).resolve().parents[3] / "contracts" / "v1").resolve()
    if root.name != "v1" or root.parent.name != "contracts":
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "resolved Shared Contracts root is not repository-owned contracts/v1",
        )
    return root


def _build_exact_shared_contracts_v1_catalog() -> tuple[
    tuple[OutputSchemaRegistryEntry, ...],
    MappingProxyType,
]:
    """Deterministically materialize the exact Shared Contracts v1 catalog."""

    root = shared_contracts_v1_root()
    manifest_path = (root / "manifest.json").resolve()
    try:
        manifest_path.relative_to(root)
    except ValueError as exc:
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "manifest path escapes repository-owned contracts/v1 root",
        ) from exc
    if not manifest_path.is_file():
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "Shared Contracts v1 manifest is missing",
        )
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("version_negotiation") != "EXACT":
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "Shared Contracts v1 version_negotiation must be EXACT",
        )
    if manifest.get("contract_version") != SHARED_CONTRACT_V1_VERSION:
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            f"Shared Contracts v1 contract_version must be {SHARED_CONTRACT_V1_VERSION}",
        )
    if manifest.get("supported_versions") != [SHARED_CONTRACT_V1_VERSION]:
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "Shared Contracts v1 supported_versions must be exact singleton",
        )

    entries: list[OutputSchemaRegistryEntry] = []
    names: dict[str, str] = {}
    logical_ids: set[str] = set()
    for item in manifest.get("contracts", []):
        schema_path = str(item["path"])
        contract_id = Path(schema_path).name.removesuffix(".schema.json")
        entry = OutputSchemaRegistryEntry(
            contract_id=contract_id,
            version=SHARED_CONTRACT_V1_VERSION,
            schema_path=schema_path,
        )
        if contract_id in logical_ids:
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
                f"duplicate logical contract identity in manifest: {contract_id}",
            )
        schema_file = (root / schema_path).resolve()
        try:
            schema_file.relative_to(root)
        except ValueError as exc:
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
                f"schema file escapes root for {contract_id}",
            ) from exc
        if not schema_file.is_file():
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
                f"schema file missing for {contract_id}",
            )
        logical_ids.add(contract_id)
        names[contract_id] = str(item["name"])
        entries.append(entry)

    if set(logical_ids) != set(SHARED_CONTRACT_V1_IDS):
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            "manifest logical identities must exactly match SHARED_CONTRACT_V1_IDS",
        )
    if len(entries) != len(SHARED_CONTRACT_V1_IDS):
        raise SchemaRegistryError(
            SchemaRegistryErrorCode.CONTRACT_REGISTRY_INVALID,
            f"expected {len(SHARED_CONTRACT_V1_IDS)} contracts, got {len(entries)}",
        )
    ordered = tuple(sorted(entries, key=lambda item: item.contract_id))
    return ordered, MappingProxyType(names)


class OutputSchemaRegistry:
    """Exact Shared Contracts v1 catalog; immutable; no caller entries or custom root."""

    __slots__ = ("_entries", "_contract_ids", "_manifest_names")

    def __init__(self) -> None:
        # F002：唯一 public 构造路径直接物化 exact 13@1.0.0 catalog
        entries, names = _build_exact_shared_contracts_v1_catalog()
        indexed: dict[tuple[str, str], OutputSchemaRegistryEntry] = {
            (entry.contract_id, entry.version): entry for entry in entries
        }
        object.__setattr__(self, "_entries", MappingProxyType(indexed))
        object.__setattr__(self, "_contract_ids", frozenset(names))
        object.__setattr__(self, "_manifest_names", names)

    @classmethod
    def from_shared_contracts_v1(cls) -> "OutputSchemaRegistry":
        """Construct the authorized exact Shared Contracts v1 registry."""

        return cls()

    def __setattr__(self, _name: str, _value: object) -> None:
        raise TypeError("OutputSchemaRegistry is immutable")

    def __len__(self) -> int:
        return len(self._entries)

    def contains(self, contract_id: str, version: str) -> bool:
        return (contract_id, version) in self._entries

    def get(self, contract_id: str, version: str) -> OutputSchemaRegistryEntry:
        try:
            return self._entries[(contract_id, version)]
        except KeyError as exc:
            if contract_id not in self._contract_ids:
                raise SchemaRegistryError(
                    SchemaRegistryErrorCode.CONTRACT_UNKNOWN,
                    f"contract is not registered: {contract_id}",
                ) from exc
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_VERSION_UNKNOWN,
                f"contract version is not registered: {contract_id}@{version}",
            ) from exc

    def manifest_name(self, contract_id: str) -> str:
        """Return authoritative manifest contract name for a logical id."""

        try:
            return self._manifest_names[contract_id]
        except KeyError as exc:
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_UNKNOWN,
                f"contract is not registered: {contract_id}",
            ) from exc

    def list_ids(self) -> tuple[str, ...]:
        return tuple(sorted(self._contract_ids))

    def list_versions(self, contract_id: str) -> tuple[str, ...]:
        if contract_id not in self._contract_ids:
            raise SchemaRegistryError(
                SchemaRegistryErrorCode.CONTRACT_UNKNOWN,
                f"contract is not registered: {contract_id}",
            )
        return tuple(
            sorted(version for current_id, version in self._entries if current_id == contract_id)
        )
