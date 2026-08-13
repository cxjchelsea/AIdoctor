"""AC-P4：ModelInvocationResult 不变量、绑定与 model_copy 探针。"""

from __future__ import annotations

import copy

import pytest
from pydantic import ValidationError

from packages.model_runtime.api.types import FrozenJsonObject
from packages.model_runtime.providers.errors import ProviderAdapterError, ProviderAdapterErrorCode
from packages.model_runtime.providers.models import (
    RESULT_SEMANTICS,
    SIMULATED_FAILURE_CODE,
    ModelInvocationResult,
    ProviderInvocationOutcome,
    assert_result_compatible,
)
from packages.model_runtime.providers.fake import (
    DeterministicFakeProviderAdapter,
    load_builtin_synthetic_fixture_catalog,
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
    return prepared, result


def test_result_semantics_are_candidate_only() -> None:
    assert RESULT_SEMANTICS == "PROVIDER_CANDIDATE_RESULT"


def test_success_invariant_requires_payload_and_forbids_errors() -> None:
    prepared, result = _success_result_from_prepare()
    assert result.outcome == ProviderInvocationOutcome.SUCCESS
    assert result.candidate_payload is not None
    assert result.error_code is None
    assert result.error_detail is None
    assert_result_compatible(prepared, result)


def test_failure_invariant_requires_simulated_failure() -> None:
    with pytest.raises(ValidationError):
        ModelInvocationResult(
            request_id="req-1",
            provider_id="synthetic-provider",
            selected_model=ModelReference(
                provider_id="synthetic-provider",
                model_id="color-classifier",
                model_version="1.0.0",
            ),
            outcome=ProviderInvocationOutcome.FAILURE,
            candidate_payload=None,
            output_contract_id="tool-result",
            output_contract_version="1.0.0",
            rendered_prompt_digest="digest",
            fixture_id="classify-color-failure",
            error_code="HTTP_401",
            error_detail="no",
        )


def test_json_strictness_rejects_nan() -> None:
    with pytest.raises(ValueError):
        FrozenJsonObject({"value": float("nan")})


def test_binding_rejects_forged_results() -> None:
    prepared, result = _success_result_from_prepare()
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
    _, result = _success_result_from_prepare()
    with pytest.raises(ValidationError):
        result.model_copy(
            update={
                "outcome": ProviderInvocationOutcome.FAILURE,
                "error_code": SIMULATED_FAILURE_CODE,
                "error_detail": "simulated provider failure",
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
