"""EG-15 / EG-16 / AC-P6：DEFAULT BLOCKED route shells 确定性回归。"""

from __future__ import annotations

import pytest
from pydantic import ValidationError

from packages.model_runtime.gateway.errors import GatewayErrorCode
from packages.model_runtime.gateway.selection import assert_route_eligible
from packages.model_runtime.routing import (
    AUTHORIZED_CONTRACT_VERSION,
    AUTHORIZED_INPUT_CONTRACT_ID,
    AUTHORIZED_OUTPUT_CONTRACT_ID,
    AUTHORIZED_SHELL_VERSION,
    OBSERVATION_EXTRACTION_ROUTE_ID,
    QUESTION_WORDING_ROUTE_ID,
    STRUCTURAL_BLOCKED_ROUTE_CAPABILITY,
    BlockedRouteShell,
    ModelRoutePolicy,
    RouteCategory,
    RouteMatch,
    RouteStatus,
    assert_blocked_shell_fail_closed,
    get_observation_extraction_shell,
    get_question_wording_shell,
    is_official_blocked_route_shell,
    list_official_blocked_route_shells,
    resolve_shell_contract_refs,
)
from packages.model_runtime.schemas import OutputSchemaRegistry


def test_official_shells_are_exactly_two_and_stable() -> None:
    shells = list_official_blocked_route_shells()
    assert len(shells) == 2
    observation = get_observation_extraction_shell()
    wording = get_question_wording_shell()
    assert shells == (observation, wording)
    for _ in range(10):
        assert get_observation_extraction_shell() is observation
        assert get_question_wording_shell() is wording
        assert get_observation_extraction_shell() == observation
        assert get_question_wording_shell() == wording


@pytest.mark.parametrize(
    ("factory", "route_id"),
    [
        (get_observation_extraction_shell, OBSERVATION_EXTRACTION_ROUTE_ID),
        (get_question_wording_shell, QUESTION_WORDING_ROUTE_ID),
    ],
)
def test_official_shell_invariants(factory, route_id: str) -> None:
    shell = factory()
    assert shell.route_id == route_id
    assert shell.version == AUTHORIZED_SHELL_VERSION
    assert shell.policy.route_id == route_id
    assert shell.policy.version == AUTHORIZED_SHELL_VERSION
    assert shell.policy.category is RouteCategory.CLINICAL
    assert shell.policy.status is RouteStatus.BLOCKED
    assert shell.policy.eligible is False
    assert shell.policy.primary_model is None
    assert shell.policy.fallback_models == ()
    assert shell.policy.required_capabilities == (STRUCTURAL_BLOCKED_ROUTE_CAPABILITY,)
    assert shell.input_contract_id == AUTHORIZED_INPUT_CONTRACT_ID
    assert shell.output_contract_id == AUTHORIZED_OUTPUT_CONTRACT_ID
    assert shell.input_contract_version == AUTHORIZED_CONTRACT_VERSION
    assert shell.output_contract_version == AUTHORIZED_CONTRACT_VERSION
    assert is_official_blocked_route_shell(shell) is True


def test_contract_refs_resolve_via_output_schema_registry() -> None:
    registry = OutputSchemaRegistry()
    for shell in list_official_blocked_route_shells():
        input_entry, output_entry = resolve_shell_contract_refs(shell, registry)
        assert input_entry.contract_id == "tool-context"
        assert input_entry.version == "1.0.0"
        assert output_entry.contract_id == "tool-result"
        assert output_entry.version == "1.0.0"


def test_integrated_validation_fail_closed_before_provider() -> None:
    """SUP-02 P6：结构壳 → registry → assert_route_eligible → ROUTE_INELIGIBLE。"""

    provider_hits = {"count": 0}

    def fake_provider_invoke() -> None:
        provider_hits["count"] += 1
        raise AssertionError("provider must not be invoked for blocked shells")

    for shell in list_official_blocked_route_shells():
        error = assert_blocked_shell_fail_closed(shell)
        assert error.code is GatewayErrorCode.ROUTE_INELIGIBLE
        # 直接复用现有 Gateway 权威，确认不会走到 provider
        with pytest.raises(Exception) as raised:
            assert_route_eligible(shell.policy)
        assert raised.value.code is GatewayErrorCode.ROUTE_INELIGIBLE
        fake_provider_invoke_guard = provider_hits["count"]
        assert fake_provider_invoke_guard == 0
    assert provider_hits["count"] == 0


def test_official_identity_requires_factory_object() -> None:
    """结构等价的 caller 构造物不得冒充官方 identity。"""

    lookalike = BlockedRouteShell(
        route_id=OBSERVATION_EXTRACTION_ROUTE_ID,
        version=AUTHORIZED_SHELL_VERSION,
        policy=ModelRoutePolicy(
            route_id=OBSERVATION_EXTRACTION_ROUTE_ID,
            version=AUTHORIZED_SHELL_VERSION,
            category=RouteCategory.CLINICAL,
            match=RouteMatch(task_type=OBSERVATION_EXTRACTION_ROUTE_ID),
            required_capabilities=(STRUCTURAL_BLOCKED_ROUTE_CAPABILITY,),
            primary_model=None,
            fallback_models=(),
            status=RouteStatus.BLOCKED,
            eligible=False,
        ),
        input_contract_id=AUTHORIZED_INPUT_CONTRACT_ID,
        input_contract_version=AUTHORIZED_CONTRACT_VERSION,
        output_contract_id=AUTHORIZED_OUTPUT_CONTRACT_ID,
        output_contract_version=AUTHORIZED_CONTRACT_VERSION,
    )
    official = get_observation_extraction_shell()
    assert lookalike == official
    assert lookalike is not official
    assert is_official_blocked_route_shell(lookalike) is False
    assert is_official_blocked_route_shell(official) is True


def test_mutation_to_eligible_is_rejected() -> None:
    shell = get_observation_extraction_shell()
    with pytest.raises(ValidationError):
        shell.model_copy(update={"policy": shell.policy.model_copy(update={"eligible": True})})


def test_mutation_to_non_clinical_rejected_as_shell() -> None:
    shell = get_question_wording_shell()
    with pytest.raises(ValidationError):
        shell.model_copy(
            update={
                "policy": shell.policy.model_copy(
                    update={
                        "category": RouteCategory.NON_CLINICAL,
                        "status": RouteStatus.BLOCKED,
                        "eligible": False,
                    }
                )
            }
        )


def test_primary_model_binding_rejected() -> None:
    shell = get_observation_extraction_shell()
    with pytest.raises(ValidationError):
        shell.model_copy(
            update={
                "policy": shell.policy.model_copy(
                    update={
                        "primary_model": {
                            "provider_id": "demo-provider",
                            "model_id": "demo-model",
                            "model_version": "1.0.0",
                        }
                    }
                )
            }
        )


def test_wrong_capability_token_rejected() -> None:
    with pytest.raises(ValidationError):
        BlockedRouteShell(
            route_id=OBSERVATION_EXTRACTION_ROUTE_ID,
            version=AUTHORIZED_SHELL_VERSION,
            policy=ModelRoutePolicy(
                route_id=OBSERVATION_EXTRACTION_ROUTE_ID,
                version=AUTHORIZED_SHELL_VERSION,
                category=RouteCategory.CLINICAL,
                match=RouteMatch(task_type=OBSERVATION_EXTRACTION_ROUTE_ID),
                required_capabilities=("adult_respiratory_v1",),
                status=RouteStatus.BLOCKED,
                eligible=False,
            ),
            input_contract_id=AUTHORIZED_INPUT_CONTRACT_ID,
            input_contract_version=AUTHORIZED_CONTRACT_VERSION,
            output_contract_id=AUTHORIZED_OUTPUT_CONTRACT_ID,
            output_contract_version=AUTHORIZED_CONTRACT_VERSION,
        )


def test_state_patch_contract_not_authorized_for_shell_output() -> None:
    with pytest.raises(ValidationError):
        BlockedRouteShell(
            route_id=QUESTION_WORDING_ROUTE_ID,
            version=AUTHORIZED_SHELL_VERSION,
            policy=ModelRoutePolicy(
                route_id=QUESTION_WORDING_ROUTE_ID,
                version=AUTHORIZED_SHELL_VERSION,
                category=RouteCategory.CLINICAL,
                match=RouteMatch(task_type=QUESTION_WORDING_ROUTE_ID),
                required_capabilities=(STRUCTURAL_BLOCKED_ROUTE_CAPABILITY,),
                status=RouteStatus.BLOCKED,
                eligible=False,
            ),
            input_contract_id=AUTHORIZED_INPUT_CONTRACT_ID,
            input_contract_version=AUTHORIZED_CONTRACT_VERSION,
            output_contract_id="state-patch",
            output_contract_version=AUTHORIZED_CONTRACT_VERSION,
        )


def test_no_prompt_or_capability_fields_on_shell() -> None:
    shell = get_observation_extraction_shell()
    dumped = shell.model_dump()
    assert "prompt_id" not in dumped
    assert "prompt_version" not in dumped
    assert "capability_id" not in dumped
    assert STRUCTURAL_BLOCKED_ROUTE_CAPABILITY not in ("adult_respiratory_v1",)
