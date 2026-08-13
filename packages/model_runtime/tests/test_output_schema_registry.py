"""NC-07 Output Schema Registry tests."""

from __future__ import annotations

import pytest
from pydantic import ValidationError

from packages.model_runtime.api.models import SHARED_CONTRACT_V1_IDS
from packages.model_runtime.schemas import (
    OutputSchemaRegistry,
    OutputSchemaRegistryEntry,
    SchemaRegistryError,
    SchemaRegistryErrorCode,
    build_entries_from_shared_contracts_v1,
)


def test_registry_contains_exact_13_shared_contracts():
    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    assert len(registry) == 13
    assert registry.list_ids() == tuple(sorted(SHARED_CONTRACT_V1_IDS))
    for contract_id in SHARED_CONTRACT_V1_IDS:
        assert registry.contains(contract_id, "1.0.0")
        assert registry.list_versions(contract_id) == ("1.0.0",)
        entry = registry.get(contract_id, "1.0.0")
        assert entry.schema_path == f"schemas/{contract_id}.schema.json"


def test_list_ids_and_versions_are_deterministic():
    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    assert registry.list_ids() == tuple(sorted(registry.list_ids()))
    assert registry.list_versions("tool-result") == ("1.0.0",)


def test_unknown_id_and_version_fail_closed():
    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    with pytest.raises(SchemaRegistryError) as unknown_id:
        registry.get("not-a-contract", "1.0.0")
    assert unknown_id.value.code is SchemaRegistryErrorCode.CONTRACT_UNKNOWN
    with pytest.raises(SchemaRegistryError) as unknown_version:
        registry.get("tool-result", "9.9.9")
    assert unknown_version.value.code is SchemaRegistryErrorCode.CONTRACT_VERSION_UNKNOWN


def test_duplicate_entry_rejected():
    entry = OutputSchemaRegistryEntry(
        contract_id="tool-result",
        version="1.0.0",
        manifest_name="ToolResult",
        schema_path="schemas/tool-result.schema.json",
    )
    with pytest.raises(ValueError, match="duplicate"):
        OutputSchemaRegistry([entry, entry])


def test_registry_immutable_and_no_mutation_api():
    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    with pytest.raises(TypeError):
        registry.foo = 1  # type: ignore[attr-defined]
    for forbidden in ("register", "add", "remove", "replace", "update", "latest"):
        assert not hasattr(registry, forbidden)


def test_caller_collection_mutation_does_not_affect_registry():
    entries = list(build_entries_from_shared_contracts_v1())
    registry = OutputSchemaRegistry(entries)
    entries.clear()
    assert len(registry) == 13
    assert registry.contains("tool-result", "1.0.0")


def test_entry_model_copy_rejects_invalid_identity():
    entry = OutputSchemaRegistryEntry(
        contract_id="tool-result",
        version="1.0.0",
        manifest_name="ToolResult",
        schema_path="schemas/tool-result.schema.json",
    )
    with pytest.raises(ValidationError):
        entry.model_copy(update={"contract_id": "!!!"})
    with pytest.raises(ValidationError):
        entry.model_copy(update={"version": "latest"})
    with pytest.raises(ValidationError):
        entry.model_copy(update={"schema_path": "schemas/other.schema.json"})


def test_manifest_alignment_helper_matches_shared_ids():
    entries = build_entries_from_shared_contracts_v1()
    assert {item.contract_id for item in entries} == set(SHARED_CONTRACT_V1_IDS)
    assert all(item.version == "1.0.0" for item in entries)
