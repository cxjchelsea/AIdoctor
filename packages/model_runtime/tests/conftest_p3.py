"""共享 P3 测试夹具（仅服务 NC-07/NC-08 单测，非 SUP-02 provider subset）。"""

from __future__ import annotations

import json
from pathlib import Path

from packages.model_runtime.api.models import ModelLifecycle, ModelSpec
from packages.model_runtime.gateway import GatewayRequest, ModelGateway
from packages.model_runtime.prompts import PromptBuilder, PromptRegistry, PromptRegistryEntry
from packages.model_runtime.routing.policies import (
    ModelReference,
    ModelRoutePolicy,
    RouteCategory,
    RouteMatch,
    RouteStatus,
)
from packages.model_runtime.schemas import OutputSchemaRegistry


def valid_tool_result_payload() -> dict:
    path = Path("contracts/v1/fixtures/valid/interaction-cases.json")
    return json.loads(path.read_text(encoding="utf-8"))["ToolResult"]


def prompt_registry() -> PromptRegistry:
    return PromptRegistry(
        [PromptRegistryEntry(prompt_id="classify-color", version="1.0.0", resource="classify-color/1.0.0.yaml")]
    )


def model_spec(
    *,
    provider_id="synthetic-provider",
    model_id="color-classifier",
    version="1.0.0",
    capabilities=("structured-output", "classification"),
    status=ModelLifecycle.DRAFT,
    supports_structured_output=True,
) -> ModelSpec:
    return ModelSpec(
        provider_id=provider_id,
        model_id=model_id,
        version=version,
        capabilities=capabilities,
        context_limits={"max_input_tokens": 1024, "max_output_tokens": 256},
        supports_structured_output=supports_structured_output,
        timeout_policy={"timeout_ms": 1000},
        retry_class="NONE",
        cost_metadata={
            "currency": "USD",
            "input_microunits_per_million_tokens": 0,
            "output_microunits_per_million_tokens": 0,
        },
        status=status,
    )


def eligible_route(
    *,
    route_id="classify-color",
    version="1.0.0",
    primary=None,
    fallbacks=(),
    required_capabilities=("structured-output", "classification"),
    required_tags=("synthetic",),
) -> ModelRoutePolicy:
    return ModelRoutePolicy(
        route_id=route_id,
        version=version,
        category=RouteCategory.NON_CLINICAL,
        match=RouteMatch(task_type="classify-color", required_tags=required_tags),
        required_capabilities=required_capabilities,
        primary_model=primary
        or ModelReference(
            provider_id="synthetic-provider",
            model_id="color-classifier",
            model_version="1.0.0",
        ),
        fallback_models=fallbacks,
        status=RouteStatus.ELIGIBLE,
        eligible=True,
    )


def gateway_request(**overrides) -> GatewayRequest:
    payload = {
        "request_id": "req-classify-1",
        "route_id": "classify-color",
        "route_version": "1.0.0",
        "task_type": "classify-color",
        "tags": ("synthetic",),
        "prompt_id": "classify-color",
        "prompt_version": "1.0.0",
        "variables": {"color_name": "blue"},
        "output_contract_id": "tool-result",
        "output_contract_version": "1.0.0",
    }
    payload.update(overrides)
    return GatewayRequest(**payload)


def build_gateway(
    *,
    routes=None,
    models=None,
    prompt_registry_override=None,
    output_schema_registry=None,
) -> ModelGateway:
    # 注意：空 tuple 是合法注入，不能用 `or` 回退默认值
    resolved_routes = (eligible_route(),) if routes is None else routes
    resolved_models = (model_spec(),) if models is None else models
    prompts = prompt_registry() if prompt_registry_override is None else prompt_registry_override
    kwargs = {
        "routes": resolved_routes,
        "models": resolved_models,
        "prompt_registry": prompts,
        "prompt_builder": PromptBuilder(),
    }
    if output_schema_registry is not None:
        kwargs["output_schema_registry"] = output_schema_registry
    else:
        kwargs["output_schema_registry"] = OutputSchemaRegistry()
    return ModelGateway(**kwargs)
