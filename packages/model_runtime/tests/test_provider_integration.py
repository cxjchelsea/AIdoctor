"""AC-P4 / EG-12：离线 prepare → Fake.invoke → Gateway.validate_output 集成。"""

from __future__ import annotations

from packages.model_runtime.providers.fake import (
    DeterministicFakeProviderAdapter,
    load_builtin_synthetic_fixture_catalog,
)
from packages.model_runtime.providers.models import (
    ProviderInvocationOutcome,
    assert_result_compatible,
)
from packages.model_runtime.tests.conftest_p3 import build_gateway, gateway_request


def test_success_offline_integration_gateway_validates() -> None:
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=load_builtin_synthetic_fixture_catalog(),
        fixture_id="classify-color-success",
    )
    result = fake.invoke(prepared)
    assert_result_compatible(prepared, result)
    assert result.outcome == ProviderInvocationOutcome.SUCCESS
    assert result.candidate_payload is not None
    validation = gateway.validate_output(prepared, result.candidate_payload.to_json_value())
    assert validation.valid is True
    assert validation.errors == ()


def test_invalid_output_offline_integration_gateway_rejects() -> None:
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=load_builtin_synthetic_fixture_catalog(),
        fixture_id="classify-color-invalid-output",
    )
    result = fake.invoke(prepared)
    assert_result_compatible(prepared, result)
    assert result.outcome == ProviderInvocationOutcome.INVALID_OUTPUT
    assert result.candidate_payload is not None
    validation = gateway.validate_output(prepared, result.candidate_payload.to_json_value())
    assert validation.valid is False
    assert validation.errors


def test_invalid_output_validation_is_deterministic() -> None:
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=load_builtin_synthetic_fixture_catalog(),
        fixture_id="classify-color-invalid-output",
    )
    validations = []
    for _ in range(5):
        result = fake.invoke(prepared)
        validations.append(
            gateway.validate_output(prepared, result.candidate_payload.to_json_value())
        )
    assert all(item.valid is False for item in validations)
    assert all(item.errors == validations[0].errors for item in validations)


def test_reuses_classify_color_prompt_and_tool_result_contract() -> None:
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    assert prepared.rendered_prompt.prompt_id == "classify-color"
    assert prepared.rendered_prompt.version == "1.0.0"
    assert prepared.output_contract_id == "tool-result"
    assert prepared.output_contract_version == "1.0.0"
