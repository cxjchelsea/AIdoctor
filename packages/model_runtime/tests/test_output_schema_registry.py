"""NC-07 Output Schema Registry tests + F001/F002/F004 regressions."""

from __future__ import annotations

import inspect
import tempfile
from pathlib import Path

import pytest
from pydantic import ValidationError

from packages.model_runtime.api.models import SHARED_CONTRACT_V1_IDS
from packages.model_runtime.schemas import (
    OutputSchemaRegistry,
    OutputSchemaRegistryEntry,
    SchemaRegistryError,
    SchemaRegistryErrorCode,
)
from packages.model_runtime.schemas import registry as registry_module


def test_registry_contains_exact_13_shared_contracts():
    registry = OutputSchemaRegistry()
    assert len(registry) == 13
    assert registry.list_ids() == tuple(sorted(SHARED_CONTRACT_V1_IDS))
    for contract_id in SHARED_CONTRACT_V1_IDS:
        assert registry.contains(contract_id, "1.0.0")
        assert registry.list_versions(contract_id) == ("1.0.0",)
        entry = registry.get(contract_id, "1.0.0")
        assert entry.schema_path == f"schemas/{contract_id}.schema.json"
        assert registry.manifest_name(contract_id)


def test_from_shared_contracts_v1_is_exact_alias():
    assert len(OutputSchemaRegistry.from_shared_contracts_v1()) == 13


def test_list_ids_and_versions_are_deterministic():
    registry = OutputSchemaRegistry()
    assert registry.list_ids() == tuple(sorted(registry.list_ids()))
    assert registry.list_versions("tool-result") == ("1.0.0",)


def test_unknown_id_and_version_fail_closed():
    registry = OutputSchemaRegistry()
    with pytest.raises(SchemaRegistryError) as unknown_id:
        registry.get("not-a-contract", "1.0.0")
    assert unknown_id.value.code is SchemaRegistryErrorCode.CONTRACT_UNKNOWN
    with pytest.raises(SchemaRegistryError) as unknown_version:
        registry.get("tool-result", "9.9.9")
    assert unknown_version.value.code is SchemaRegistryErrorCode.CONTRACT_VERSION_UNKNOWN
    with pytest.raises(SchemaRegistryError) as list_unknown:
        registry.list_versions("not-a-contract")
    assert list_unknown.value.code is SchemaRegistryErrorCode.CONTRACT_UNKNOWN


def test_registry_immutable_and_no_mutation_api():
    registry = OutputSchemaRegistry()
    with pytest.raises(TypeError):
        registry.foo = 1  # type: ignore[attr-defined]
    for forbidden in ("register", "add", "remove", "replace", "update", "latest"):
        assert not hasattr(registry, forbidden)


def test_f002_empty_or_subset_constructor_impossible():
    # 不再接受 entries= 参数
    with pytest.raises(TypeError):
        OutputSchemaRegistry([])  # type: ignore[call-arg]
    with pytest.raises(TypeError):
        OutputSchemaRegistry(
            [
                OutputSchemaRegistryEntry(
                    contract_id="tool-result",
                    version="1.0.0",
                    schema_path="schemas/tool-result.schema.json",
                )
            ]
        )  # type: ignore[call-arg]


def test_f001_public_custom_root_impossible_and_sentinel_not_executed(tmp_path: Path):
    """恶意 temp contracts_root 不得被接受，sentinel validator 不得执行。"""

    sentinel = tmp_path / "a7_nc_p3_f001_sentinel.txt"
    fake_root = tmp_path / "contracts" / "v1"
    (fake_root / "validator").mkdir(parents=True)
    # harmless sentinel：若被 exec_module 加载会落盘
    (fake_root / "validator" / "validate_contracts.py").write_text(
        f"from pathlib import Path\nPath(r'{sentinel}').write_text('executed', encoding='utf-8')\n",
        encoding="utf-8",
    )
    assert "contracts_root" not in inspect.signature(OutputSchemaRegistry.from_shared_contracts_v1).parameters
    assert "contracts_root" not in inspect.signature(OutputSchemaRegistry.__init__).parameters
    with pytest.raises(TypeError):
        OutputSchemaRegistry.from_shared_contracts_v1(fake_root)  # type: ignore[call-arg]
    with pytest.raises(TypeError):
        OutputSchemaRegistry.from_shared_contracts_v1(contracts_root=fake_root)  # type: ignore[call-arg]
    # 内部 helper 也不得再公开 export
    assert not hasattr(registry_module, "build_entries_from_shared_contracts_v1")
    from packages.model_runtime import schemas as schemas_pkg
    from packages.model_runtime.schemas import SharedContractValidator

    assert "build_entries_from_shared_contracts_v1" not in schemas_pkg.__all__
    assert "contracts_root" not in inspect.signature(SharedContractValidator.__init__).parameters
    with pytest.raises(TypeError):
        SharedContractValidator(OutputSchemaRegistry(), contracts_root=fake_root)  # type: ignore[call-arg]
    # 仍可构造正常 registry/validator，且不得执行 temp sentinel
    registry = OutputSchemaRegistry()
    SharedContractValidator(registry)
    assert not sentinel.exists()


def test_f004_manifest_name_not_caller_constructible():
    # manifest_name 已从 Entry 删除；传入必须被 API 拒绝
    with pytest.raises((TypeError, ValidationError)):
        OutputSchemaRegistryEntry(
            contract_id="tool-result",
            version="1.0.0",
            manifest_name="AuditRef",
            schema_path="schemas/tool-result.schema.json",
        )  # type: ignore[call-arg]
    entry = OutputSchemaRegistryEntry(
        contract_id="tool-result",
        version="1.0.0",
        schema_path="schemas/tool-result.schema.json",
    )
    with pytest.raises(ValidationError):
        entry.model_copy(update={"schema_path": "schemas/audit-ref.schema.json"})


def test_entry_model_copy_rejects_invalid_identity():
    entry = OutputSchemaRegistryEntry(
        contract_id="tool-result",
        version="1.0.0",
        schema_path="schemas/tool-result.schema.json",
    )
    with pytest.raises(ValidationError):
        entry.model_copy(update={"contract_id": "!!!"})
    with pytest.raises(ValidationError):
        entry.model_copy(update={"version": "latest"})
