"""AC-P4 / F002–F003：Fake 不可变、场景确定性与 Fake 绑定。"""

from __future__ import annotations

import pytest

from packages.model_runtime.api.types import FrozenJsonObject
from packages.model_runtime.providers.errors import ProviderAdapterError, ProviderAdapterErrorCode
from packages.model_runtime.providers.fake import (
    DeterministicFakeProviderAdapter,
    FakeProviderFixture,
    FakeProviderFixtureCatalog,
    FakeProviderScenario,
    SIMULATED_FAILURE_CODE,
    SIMULATED_TIMEOUT_CODE,
    load_builtin_synthetic_fixture_catalog,
)
from packages.model_runtime.providers.models import (
    ProviderInvocationOutcome,
    assert_result_compatible,
)
from packages.model_runtime.routing.policies import ModelReference
from packages.model_runtime.tests.conftest_p3 import (
    build_gateway,
    eligible_route,
    gateway_request,
    model_spec,
)


def _adapter(fixture_id: str) -> DeterministicFakeProviderAdapter:
    return DeterministicFakeProviderAdapter(
        fixture_catalog=load_builtin_synthetic_fixture_catalog(),
        fixture_id=fixture_id,
    )


def test_provider_mismatch_fail_closed() -> None:
    fake = _adapter("classify-color-success")
    gateway = build_gateway(
        models=(model_spec(provider_id="other-provider"),),
        routes=(
            eligible_route(
                primary=ModelReference(
                    provider_id="other-provider",
                    model_id="color-classifier",
                    model_version="1.0.0",
                )
            ),
        ),
    )
    prepared = gateway.prepare(gateway_request())
    with pytest.raises(ProviderAdapterError) as captured:
        fake.invoke(prepared)
    assert captured.value.code == ProviderAdapterErrorCode.PROVIDER_MISMATCH


def test_contract_mismatch_fail_closed() -> None:
    success = load_builtin_synthetic_fixture_catalog().get("classify-color-success")
    mismatched = success.model_copy(
        update={
            "fixture_id": "mismatched-contract-fixture",
            "output_contract_id": "audit-ref",
        }
    )
    catalog = FakeProviderFixtureCatalog([mismatched])
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=catalog,
        fixture_id="mismatched-contract-fixture",
    )
    prepared = build_gateway().prepare(gateway_request())
    with pytest.raises(ProviderAdapterError) as captured:
        fake.invoke(prepared)
    assert captured.value.code == ProviderAdapterErrorCode.CONTRACT_MISMATCH


def test_failure_returns_deterministic_result_not_exception() -> None:
    fake = _adapter("classify-color-failure")
    prepared = build_gateway().prepare(gateway_request())
    results = [fake.invoke(prepared) for _ in range(10)]
    assert all(item.outcome == ProviderInvocationOutcome.FAILURE for item in results)
    assert all(item.error_code == SIMULATED_FAILURE_CODE for item in results)
    assert all(item.candidate_payload is None for item in results)
    assert all(item == results[0] for item in results)


def test_timeout_returns_immediately_without_wait() -> None:
    fake = _adapter("classify-color-timeout")
    prepared = build_gateway().prepare(gateway_request())
    results = [fake.invoke(prepared) for _ in range(10)]
    assert all(item.outcome == ProviderInvocationOutcome.TIMEOUT for item in results)
    assert all(item.error_code == SIMULATED_TIMEOUT_CODE for item in results)
    assert all(item == results[0] for item in results)


def test_success_and_invalid_output_determinism() -> None:
    prepared = build_gateway().prepare(gateway_request())
    for fixture_id, scenario, outcome in (
        ("classify-color-success", FakeProviderScenario.SUCCESS, ProviderInvocationOutcome.SUCCESS),
        (
            "classify-color-invalid-output",
            FakeProviderScenario.INVALID_OUTPUT,
            ProviderInvocationOutcome.SUCCESS,
        ),
    ):
        fake = _adapter(fixture_id)
        assert fake.scenario == scenario
        results = [fake.invoke(prepared) for _ in range(10)]
        assert all(item.outcome == outcome for item in results)
        assert all(item == results[0] for item in results)
        serialized = [item.model_dump(mode="json") for item in results]
        assert all(row == serialized[0] for row in serialized)


def test_fixture_provider_binding_at_construction() -> None:
    success = load_builtin_synthetic_fixture_catalog().get("classify-color-success")
    foreign = success.model_copy(
        update={
            "fixture_id": "foreign-provider-fixture",
            "provider_id": "other-provider",
        }
    )
    catalog = FakeProviderFixtureCatalog([foreign])
    with pytest.raises(ProviderAdapterError) as captured:
        DeterministicFakeProviderAdapter(
            provider_id="synthetic-provider",
            fixture_catalog=catalog,
            fixture_id="foreign-provider-fixture",
        )
    assert captured.value.code == ProviderAdapterErrorCode.PROVIDER_MISMATCH


def test_f002_fake_attribute_mutation_rejected() -> None:
    fake = _adapter("classify-color-success")
    prepared = build_gateway().prepare(gateway_request())
    before = fake.invoke(prepared)
    with pytest.raises(TypeError):
        fake._provider_id = "other-provider"  # type: ignore[misc]
    with pytest.raises(TypeError):
        fake._fixture = load_builtin_synthetic_fixture_catalog().get("classify-color-failure")  # type: ignore[misc]
    with pytest.raises(TypeError):
        fake._fixture_catalog = FakeProviderFixtureCatalog([])  # type: ignore[attr-defined]
    with pytest.raises(TypeError):
        del fake._fixture  # type: ignore[attr-defined]
    after_rows = [fake.invoke(prepared) for _ in range(10)]
    assert all(item == before for item in after_rows)
    assert all(item.model_dump(mode="json") == before.model_dump(mode="json") for item in after_rows)


def test_f002_fake_subclass_rejected() -> None:
    with pytest.raises(TypeError):

        class EvilFake(DeterministicFakeProviderAdapter):
            pass


def test_f003_forged_candidate_fails_fake_binding() -> None:
    fake = _adapter("classify-color-success")
    prepared = build_gateway().prepare(gateway_request())
    honest = fake.invoke(prepared)
    forged = honest.model_copy(update={"candidate_payload": FrozenJsonObject({"forged": True})})
    # 泛型 Prepared 绑定可按设计 PASS
    assert_result_compatible(prepared, forged)
    with pytest.raises(ProviderAdapterError) as captured:
        fake.assert_result_compatible(prepared, forged)
    assert captured.value.code == ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID


def test_f003_forged_failure_scenario_fails_fake_binding() -> None:
    fake = _adapter("classify-color-success")
    prepared = build_gateway().prepare(gateway_request())
    honest = fake.invoke(prepared)
    forged = honest.model_copy(
        update={
            "outcome": ProviderInvocationOutcome.FAILURE,
            "candidate_payload": None,
            "error_code": SIMULATED_FAILURE_CODE,
            "error_detail": "simulated provider failure",
        }
    )
    assert_result_compatible(prepared, forged)
    with pytest.raises(ProviderAdapterError) as captured:
        fake.assert_result_compatible(prepared, forged)
    assert captured.value.code == ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID


def test_f003_exact_equivalent_result_may_pass_fake_binding() -> None:
    fake = _adapter("classify-color-success")
    prepared = build_gateway().prepare(gateway_request())
    honest = fake.invoke(prepared)
    equivalent = honest.model_copy()
    fake.assert_result_compatible(prepared, equivalent)
