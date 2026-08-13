"""NC-07 Shared Contract validation adapter tests."""

from __future__ import annotations

import copy

import pytest

from packages.model_runtime.schemas import (
    OutputSchemaRegistry,
    SchemaRegistryError,
    SchemaRegistryErrorCode,
    SharedContractValidator,
)
from packages.model_runtime.tests.conftest_p3 import valid_tool_result_payload


def test_valid_payload_passes_and_does_not_mutate_input():
    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    original = copy.deepcopy(payload)
    valid, errors = validator.validate("tool-result", "1.0.0", payload)
    assert valid is True
    assert errors == ()
    assert payload == original


def test_schema_invalid_payload_fails():
    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    del payload["status"]
    valid, errors = validator.validate("tool-result", "1.0.0", payload)
    assert valid is False
    assert errors
    assert errors == tuple(sorted(errors))


def test_semantic_invalid_but_schema_shaped_payload_fails():
    """EG-07：不得把 Shared Contract 降级为纯 JSON Schema。"""

    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    payload["completed_at"] = "2026-08-04T02:00:00Z"  # precedes started_at
    valid, errors = validator.validate("tool-result", "1.0.0", payload)
    assert valid is False
    assert any("completed_at must not precede started_at" in item for item in errors)


def test_unknown_contract_and_version_fail_closed():
    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    with pytest.raises(SchemaRegistryError) as unknown_id:
        validator.validate("missing-contract", "1.0.0", payload)
    assert unknown_id.value.code is SchemaRegistryErrorCode.CONTRACT_UNKNOWN
    with pytest.raises(SchemaRegistryError) as unknown_version:
        validator.validate("tool-result", "2.0.0", payload)
    assert unknown_version.value.code is SchemaRegistryErrorCode.CONTRACT_VERSION_UNKNOWN


def test_validator_is_immutable():
    registry = OutputSchemaRegistry.from_shared_contracts_v1()
    validator = SharedContractValidator(registry)
    with pytest.raises(TypeError):
        validator.foo = 1  # type: ignore[attr-defined]
