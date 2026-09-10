"""NC-08 output validation and prompt integration tests."""

from __future__ import annotations

import copy

import pytest
from pydantic import ValidationError

from packages.model_runtime.gateway import GatewayRuntimeError
from packages.model_runtime.gateway.errors import GatewayErrorCode
from packages.model_runtime.gateway.models import OutputValidationResult
from packages.model_runtime.prompts import PromptRuntimeError
from packages.model_runtime.tests.conftest_p3 import (
    build_gateway,
    gateway_request,
    valid_tool_result_payload,
)


def test_prepare_uses_explicit_prompt_chain_and_validates_output():
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    assert prepared.rendered_prompt.prompt_id == "classify-color"
    assert prepared.rendered_prompt.messages
    payload = valid_tool_result_payload()
    original = copy.deepcopy(payload)
    result = gateway.validate_output(prepared, payload)
    assert result.valid is True
    assert result.errors == ()
    assert result.contract_id == "tool-result"
    assert payload == original


def test_invalid_and_semantic_invalid_outputs():
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    schema_invalid = valid_tool_result_payload()
    del schema_invalid["status"]
    schema_result = gateway.validate_output(prepared, schema_invalid)
    assert schema_result.valid is False
    assert schema_result.errors == tuple(sorted(schema_result.errors))

    semantic_invalid = valid_tool_result_payload()
    semantic_invalid["completed_at"] = "2026-08-04T02:00:00Z"
    semantic_result = gateway.validate_output(prepared, semantic_invalid)
    assert semantic_result.valid is False
    assert any("completed_at must not precede started_at" in item for item in semantic_result.errors)


def test_contract_mismatch_on_validate_fail_closed():
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.validate_output(
            prepared,
            valid_tool_result_payload(),
            contract_id="audit-ref",
            contract_version="1.0.0",
        )
    assert error.value.code is GatewayErrorCode.CONTRACT_MISMATCH


def test_prompt_unknown_and_contract_mismatch():
    gateway = build_gateway()
    with pytest.raises(PromptRuntimeError):
        gateway.prepare(gateway_request(prompt_id="missing-prompt"))
    with pytest.raises(PromptRuntimeError):
        gateway.prepare(gateway_request(prompt_version="9.0.0"))
    with pytest.raises(GatewayRuntimeError) as mismatch:
        gateway.prepare(gateway_request(output_contract_id="audit-ref"))
    assert mismatch.value.code is GatewayErrorCode.CONTRACT_MISMATCH


def test_output_validation_result_model_copy_probe():
    result = OutputValidationResult(
        valid=False,
        errors=("x",),
        contract_id="tool-result",
        contract_version="1.0.0",
        request_id="req-classify-1",
    )
    with pytest.raises(ValidationError):
        result.model_copy(update={"valid": True})
    with pytest.raises(ValidationError):
        result.model_copy(update={"errors": ("b", "a")})
