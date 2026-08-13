"""Deterministic structural route/model selection for the P3 Gateway."""

from __future__ import annotations

from ..api.models import ModelLifecycle, ModelSpec
from ..routing.policies import ModelReference, ModelRoutePolicy, RouteCategory, RouteStatus
from .errors import GatewayErrorCode, GatewayRuntimeError
from .models import GatewayRequest


def assert_route_eligible(policy: ModelRoutePolicy) -> None:
    """Fail closed for blocked / clinical / unknown / ineligible routes."""

    if policy.category in {RouteCategory.CLINICAL, RouteCategory.UNKNOWN}:
        raise GatewayRuntimeError(
            GatewayErrorCode.ROUTE_INELIGIBLE,
            f"route category is not eligible for P3 preparation: {policy.category.value}",
        )
    if policy.status is RouteStatus.BLOCKED:
        raise GatewayRuntimeError(
            GatewayErrorCode.ROUTE_BLOCKED,
            f"route is blocked: {policy.route_id}@{policy.version}",
        )
    if policy.status is RouteStatus.DISABLED:
        raise GatewayRuntimeError(
            GatewayErrorCode.ROUTE_BLOCKED,
            f"route is disabled: {policy.route_id}@{policy.version}",
        )
    if not policy.eligible or policy.status is not RouteStatus.ELIGIBLE:
        raise GatewayRuntimeError(
            GatewayErrorCode.ROUTE_INELIGIBLE,
            f"route is not eligible: {policy.route_id}@{policy.version}",
        )
    if policy.category is not RouteCategory.NON_CLINICAL:
        raise GatewayRuntimeError(
            GatewayErrorCode.ROUTE_INELIGIBLE,
            f"only NON_CLINICAL routes may proceed: {policy.route_id}@{policy.version}",
        )


def assert_route_match(policy: ModelRoutePolicy, request: GatewayRequest) -> None:
    """Exact deterministic match against ModelRoutePolicy.match."""

    if request.task_type != policy.match.task_type:
        raise GatewayRuntimeError(
            GatewayErrorCode.SELECTION_FAILED,
            f"task_type mismatch for route {policy.route_id}@{policy.version}",
        )
    required = set(policy.match.required_tags)
    provided = set(request.tags)
    if not required.issubset(provided):
        missing = sorted(required - provided)
        raise GatewayRuntimeError(
            GatewayErrorCode.SELECTION_FAILED,
            f"required tags missing for route {policy.route_id}@{policy.version}: {missing}",
        )


def model_is_structurally_eligible(spec: ModelSpec, required_capabilities: tuple[str, ...]) -> bool:
    """Structural eligibility only; never clinical/provider authorization."""

    if spec.status in {ModelLifecycle.DISABLED, ModelLifecycle.DEPRECATED}:
        return False
    if not set(required_capabilities).issubset(set(spec.capabilities)):
        return False
    # P3 Gateway 准备结构化输出路径时要求模型声明支持 structured output
    if not spec.supports_structured_output:
        return False
    return True


def select_structural_model(
    policy: ModelRoutePolicy,
    models_by_identity: dict[tuple[str, str, str], ModelSpec],
) -> tuple[ModelReference, ModelSpec, tuple[ModelReference, ...], int]:
    """
    Select the first structurally eligible model from primary then fallbacks.

    Returns:
        selected reference, selected ModelSpec, full candidate topology, selection index
    """

    if policy.primary_model is None:
        raise GatewayRuntimeError(
            GatewayErrorCode.NO_ELIGIBLE_MODEL,
            f"eligible route lacks primary model: {policy.route_id}@{policy.version}",
        )
    topology = (policy.primary_model, *policy.fallback_models)
    for index, reference in enumerate(topology):
        identity = (reference.provider_id, reference.model_id, reference.model_version)
        spec = models_by_identity.get(identity)
        if spec is None:
            # 未知引用：继续扫描后续候选；若全部失败则统一 NO_ELIGIBLE_MODEL / MODEL_NOT_FOUND
            continue
        if model_is_structurally_eligible(spec, policy.required_capabilities):
            return reference, spec, topology, index

    # 区分“引用完全缺失”与“均不符合结构资格”
    missing = [
        reference
        for reference in topology
        if (reference.provider_id, reference.model_id, reference.model_version) not in models_by_identity
    ]
    if len(missing) == len(topology):
        raise GatewayRuntimeError(
            GatewayErrorCode.MODEL_NOT_FOUND,
            f"no ModelSpec resolves route model references: {policy.route_id}@{policy.version}",
        )
    raise GatewayRuntimeError(
        GatewayErrorCode.NO_ELIGIBLE_MODEL,
        f"no structurally eligible model for route: {policy.route_id}@{policy.version}",
    )
