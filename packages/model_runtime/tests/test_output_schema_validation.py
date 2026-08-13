"""NC-07 Shared Contract validation adapter tests + F003 regressions."""

from __future__ import annotations

import copy
import inspect
import json
import tempfile
from pathlib import Path

import pytest

from packages.model_runtime.schemas import (
    OutputSchemaRegistry,
    SchemaRegistryError,
    SchemaRegistryErrorCode,
    SharedContractValidator,
)
from packages.model_runtime.tests.conftest_p3 import valid_tool_result_payload


def test_valid_payload_passes_and_does_not_mutate_input():
    registry = OutputSchemaRegistry()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    original = copy.deepcopy(payload)
    valid, errors = validator.validate("tool-result", "1.0.0", payload)
    assert valid is True
    assert errors == ()
    assert payload == original


def test_schema_invalid_payload_fails_without_raw_exception():
    registry = OutputSchemaRegistry()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    del payload["status"]
    valid, errors = validator.validate("tool-result", "1.0.0", payload)
    assert valid is False
    assert errors
    assert errors == tuple(sorted(errors))


def test_semantic_invalid_but_schema_shaped_payload_fails():
    registry = OutputSchemaRegistry()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    payload["completed_at"] = "2026-08-04T02:00:00Z"
    valid, errors = validator.validate("tool-result", "1.0.0", payload)
    assert valid is False
    assert any("completed_at must not precede started_at" in item for item in errors)


def test_f003_malformed_payloads_fail_closed_no_raw_exception():
    registry = OutputSchemaRegistry()
    validator = SharedContractValidator(registry)
    interaction = json.loads(Path("contracts/v1/fixtures/valid/interaction-cases.json").read_text(encoding="utf-8"))
    cases = [
        ("tool-result", lambda payload: payload.pop("started_at", None)),
        ("tool-result", lambda payload: payload.pop("completed_at", None)),
        ("tool-context", lambda payload: payload.pop("deadline", None)),
        ("tool-context", lambda payload: payload.pop("envelope", None)),
        ("evidence-pack", lambda payload: payload.__setitem__("claims", "not-a-list")),
        ("patient-delivery-view", lambda payload: payload.__setitem__("evidence_sections", "not-a-list")),
    ]
    payloads = {
        "tool-result": interaction["ToolResult"],
        "tool-context": interaction["ToolContext"],
        "evidence-pack": interaction["EvidencePack"],
        "patient-delivery-view": interaction["PatientDeliveryView"],
    }
    for contract_id, mutate in cases:
        payload = copy.deepcopy(payloads[contract_id])
        mutate(payload)
        valid, errors = validator.validate(contract_id, "1.0.0", payload)
        assert valid is False
        assert errors == tuple(sorted(errors))


def test_f003_payload_value_not_leaked_in_errors():
    registry = OutputSchemaRegistry()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    payload["tool_name"] = "synthetic-secret-value-12345"
    payload["status"] = 123
    valid, errors = validator.validate("tool-result", "1.0.0", payload)
    assert valid is False
    assert all("synthetic-secret-value-12345" not in item for item in errors)


def test_unknown_contract_and_version_fail_closed():
    registry = OutputSchemaRegistry()
    validator = SharedContractValidator(registry)
    payload = valid_tool_result_payload()
    with pytest.raises(SchemaRegistryError) as unknown_id:
        validator.validate("missing-contract", "1.0.0", payload)
    assert unknown_id.value.code is SchemaRegistryErrorCode.CONTRACT_UNKNOWN
    with pytest.raises(SchemaRegistryError) as unknown_version:
        validator.validate("tool-result", "2.0.0", payload)
    assert unknown_version.value.code is SchemaRegistryErrorCode.CONTRACT_VERSION_UNKNOWN


def test_f001_validator_rejects_contracts_root_kwarg():
    registry = OutputSchemaRegistry()
    assert "contracts_root" not in inspect.signature(SharedContractValidator.__init__).parameters
    with pytest.raises(TypeError):
        SharedContractValidator(registry, contracts_root=tempfile.gettempdir())  # type: ignore[call-arg]


def test_validator_is_immutable():
    registry = OutputSchemaRegistry()
    validator = SharedContractValidator(registry)
    with pytest.raises(TypeError):
        validator.foo = 1  # type: ignore[attr-defined]
