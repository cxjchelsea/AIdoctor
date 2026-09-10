"""NC-08 route/model selection tests."""

from __future__ import annotations

import pytest

from packages.model_runtime.api.models import ModelLifecycle
from packages.model_runtime.gateway import GatewayRuntimeError
from packages.model_runtime.gateway.errors import GatewayErrorCode
from packages.model_runtime.routing.policies import (
    ModelReference,
    ModelRoutePolicy,
    RouteCategory,
    RouteMatch,
    RouteStatus,
)
from packages.model_runtime.tests.conftest_p3 import (
    build_gateway,
    eligible_route,
    gateway_request,
    model_spec,
)


def test_eligible_non_clinical_route_prepares():
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    assert prepared.selected_model.model_id == "color-classifier"
    assert prepared.provenance.selection_index == 0
    assert prepared.provenance.fallback_used is False


@pytest.mark.parametrize(
    ("status", "eligible", "category", "code"),
    [
        (RouteStatus.BLOCKED, False, RouteCategory.NON_CLINICAL, GatewayErrorCode.ROUTE_BLOCKED),
        (RouteStatus.DISABLED, False, RouteCategory.NON_CLINICAL, GatewayErrorCode.ROUTE_BLOCKED),
        (RouteStatus.BLOCKED, False, RouteCategory.CLINICAL, GatewayErrorCode.ROUTE_INELIGIBLE),
        (RouteStatus.BLOCKED, False, RouteCategory.UNKNOWN, GatewayErrorCode.ROUTE_INELIGIBLE),
    ],
)
def test_unsafe_routes_fail_closed(status, eligible, category, code):
    route = ModelRoutePolicy(
        route_id="classify-color",
        version="1.0.0",
        category=category,
        match=RouteMatch(task_type="classify-color", required_tags=("synthetic",)),
        required_capabilities=("structured-output",),
        primary_model=ModelReference(
            provider_id="synthetic-provider",
            model_id="color-classifier",
            model_version="1.0.0",
        ),
        status=status,
        eligible=eligible,
    )
    gateway = build_gateway(routes=(route,))
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.prepare(gateway_request())
    assert error.value.code is code


def test_ineligible_flag_rejected():
    route = eligible_route()
    # 直接构造违反 NON_CLINICAL ELIGIBLE 一致性会在 ModelRoutePolicy 层失败；
    # 这里用 DISABLED 表达不可继续路径
    blocked = route.model_copy(update={"status": RouteStatus.DISABLED, "eligible": False})
    gateway = build_gateway(routes=(blocked,))
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.prepare(gateway_request())
    assert error.value.code is GatewayErrorCode.ROUTE_BLOCKED


def test_task_and_tag_mismatch_fail_closed():
    gateway = build_gateway()
    with pytest.raises(GatewayRuntimeError) as task_error:
        gateway.prepare(gateway_request(task_type="other-task"))
    assert task_error.value.code is GatewayErrorCode.SELECTION_FAILED
    with pytest.raises(GatewayRuntimeError) as tag_error:
        gateway.prepare(gateway_request(tags=()))
    assert tag_error.value.code is GatewayErrorCode.SELECTION_FAILED


def test_unknown_route_fail_closed():
    gateway = build_gateway()
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.prepare(gateway_request(route_id="missing-route"))
    assert error.value.code is GatewayErrorCode.ROUTE_NOT_FOUND


def test_duplicate_route_identity_rejected():
    route = eligible_route()
    with pytest.raises(ValueError, match="duplicate route"):
        build_gateway(routes=(route, route))


def test_primary_then_fallback_selection_order():
    primary = ModelReference(
        provider_id="synthetic-provider",
        model_id="primary-model",
        model_version="1.0.0",
    )
    fallback = ModelReference(
        provider_id="synthetic-provider",
        model_id="fallback-model",
        model_version="1.0.0",
    )
    route = eligible_route(primary=primary, fallbacks=(fallback,))
    # primary 缺少 capability，应落到 fallback
    models = (
        model_spec(model_id="primary-model", capabilities=("classification",)),
        model_spec(model_id="fallback-model"),
    )
    gateway = build_gateway(routes=(route,), models=models)
    prepared = gateway.prepare(gateway_request())
    assert prepared.selected_model == fallback
    assert prepared.provenance.selection_index == 1
    assert prepared.provenance.fallback_used is True
    assert prepared.candidate_models == (primary, fallback)


def test_disabled_and_deprecated_models_not_selected():
    primary = ModelReference(
        provider_id="synthetic-provider",
        model_id="disabled-model",
        model_version="1.0.0",
    )
    fallback = ModelReference(
        provider_id="synthetic-provider",
        model_id="ok-model",
        model_version="1.0.0",
    )
    route = eligible_route(primary=primary, fallbacks=(fallback,))
    models = (
        model_spec(model_id="disabled-model", status=ModelLifecycle.DISABLED),
        model_spec(model_id="ok-model"),
    )
    gateway = build_gateway(routes=(route,), models=models)
    prepared = gateway.prepare(gateway_request())
    assert prepared.selected_model.model_id == "ok-model"


def test_no_eligible_model():
    route = eligible_route()
    gateway = build_gateway(routes=(route,), models=(model_spec(status=ModelLifecycle.DEPRECATED),))
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.prepare(gateway_request())
    assert error.value.code is GatewayErrorCode.NO_ELIGIBLE_MODEL


def test_unknown_model_reference():
    route = eligible_route()
    gateway = build_gateway(routes=(route,), models=())
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.prepare(gateway_request())
    assert error.value.code is GatewayErrorCode.MODEL_NOT_FOUND


def test_selection_determinism():
    gateway = build_gateway()
    first = gateway.prepare(gateway_request())
    second = gateway.prepare(gateway_request())
    assert first == second
    assert first.provenance == second.provenance
