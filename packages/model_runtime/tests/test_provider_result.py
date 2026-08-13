"""AC-P4 / F001：generic ModelInvocationResult 与绑定探针。"""

from __future__ import annotations

import copy

import pytest
from pydantic import ValidationError

from packages.model_runtime.api.types import FrozenJsonObject
from packages.model_runtime.providers.errors import ProviderAdapterError, ProviderAdapterErrorCode
from packages.model_runtime.providers.fake import (
    DeterministicFakeProviderAdapter,
    load_builtin_synthetic_fixture_catalog,
)
from packages.model_runtime.providers.models import (
    RESULT_SEMANTICS,
    ModelInvocationResult,
    ProviderInvocationOutcome,
    assert_result_compatible,
)
from packages.model_runtime.routing.policies import ModelReference
from packages.model_runtime.tests.conftest_p3 import build_gateway, gateway_request


def _success_result_from_prepare():
    catalog = load_builtin_synthetic_fixture_catalog()
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=catalog,
        fixture_id="classify-color-success",
    )
    prepared = build_gateway().prepare(gateway_request())
    result = fake.invoke(prepared)
    return prepared, result, fake


def test_result_semantics_are_candidate_only() -> None:
    assert RESULT_SEMANTICS == "PROVIDER_CANDIDATE_RESULT"
    assert not hasattr(ProviderInvocationOutcome, "INVALID_OUTPUT")


def test_f001_generic_future_failure_without_fixture_id() -> None:
    result = ModelInvocationResult(
        request_id="req-future-1",
        provider_id="future-provider",
        selected_model=ModelReference(
            provider_id="future-provider",
            model_id="future-model",
            model_version="1.0.0",
        ),
        outcome=ProviderInvocationOutcome.FAILURE,
        candidate_payload=None,
        output_contract_id="tool-result",
        output_contract_version="1.0.0",
        rendered_prompt_digest="a" * 64,
        error_code="PROVIDER_FAILURE",
        error_detail="future provider failed",
    )
    assert result.error_code == "PROVIDER_FAILURE"
    assert "fixture_id" not in ModelInvocationResult.model_fields


def test_f001_generic_future_timeout_without_simulated_code() -> None:
    result = ModelInvocationResult(
        request_id="req-future-2",
        provider_id="future-provider",
        selected_model=ModelReference(
            provider_id="future-provider",
            model_id="future-model",
            model_version="1.0.0",
        ),
        outcome=ProviderInvocationOutcome.TIMEOUT,
        candidate_payload=None,
        output_contract_id="tool-result",
        output_contract_version="1.0.0",
        rendered_prompt_digest="b" * 64,
        error_code="PROVIDER_TIMEOUT",
        error_detail="future provider timed out",
    )
    assert result.error_code == "PROVIDER_TIMEOUT"


def test_success_invariant_requires_payload_and_forbids_errors() -> None:
    prepared, result, _fake = _success_result_from_prepare()
    assert result.outcome == ProviderInvocationOutcome.SUCCESS
    assert result.candidate_payload is not None
    assert result.error_code is None
    assert result.error_detail is None
    assert_result_compatible(prepared, result)


def test_json_strictness_rejects_nan() -> None:
    with pytest.raises(ValueError):
        FrozenJsonObject({"value": float("nan")})


def test_binding_rejects_forged_prepared_fields() -> None:
    prepared, result, _fake = _success_result_from_prepare()
    probes = [
        {"request_id": "other-request"},
        {"provider_id": "other-provider"},
        {
            "selected_model": ModelReference(
                provider_id="synthetic-provider",
                model_id="other-model",
                model_version="1.0.0",
            )
        },
        {"output_contract_id": "audit-ref"},
        {"rendered_prompt_digest": "forged-digest-value"},
    ]
    for update in probes:
        forged = result.model_copy(update=update)
        with pytest.raises(ProviderAdapterError) as captured:
            assert_result_compatible(prepared, forged)
        assert captured.value.code == ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID


def test_model_copy_rejects_outcome_payload_mismatch() -> None:
    _prepared, result, _fake = _success_result_from_prepare()
    with pytest.raises(ValidationError):
        result.model_copy(
            update={
                "outcome": ProviderInvocationOutcome.FAILURE,
                "error_code": "PROVIDER_FAILURE",
                "error_detail": "x",
            }
        )


def test_prepared_is_not_mutated_by_invoke() -> None:
    catalog = load_builtin_synthetic_fixture_catalog()
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=catalog,
        fixture_id="classify-color-success",
    )
    prepared = build_gateway().prepare(gateway_request())
    before = copy.deepcopy(prepared.model_dump(mode="python"))
    fake.invoke(prepared)
    after = prepared.model_dump(mode="python")
    assert before == after
