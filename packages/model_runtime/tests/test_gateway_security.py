"""NC-08 / P3 adversarial detectors + F001/F005/F006/F007 regressions."""

from __future__ import annotations

import ast
import inspect
import os
from pathlib import Path

import pytest
from pydantic import ValidationError

from packages.model_runtime.gateway import ModelGateway
from packages.model_runtime.gateway.errors import GatewayErrorCode, GatewayRuntimeError
from packages.model_runtime.prompts import PromptBuilder, PromptLoader, PromptRegistry, PromptRegistryEntry
from packages.model_runtime.routing.policies import (
    ModelReference,
    ModelRoutePolicy,
    RouteCategory,
    RouteMatch,
    RouteStatus,
)
from packages.model_runtime.schemas import OutputSchemaRegistry, SharedContractValidator
from packages.model_runtime.tests.conftest_p3 import (
    build_gateway,
    eligible_route,
    gateway_request,
    model_spec,
    prompt_registry,
    valid_tool_result_payload,
)


P3_SOURCE_DIRS = (
    Path("packages/model_runtime/schemas"),
    Path("packages/model_runtime/gateway"),
)

FORBIDDEN_IMPORT_NAMES = {
    "socket",
    "requests",
    "httpx",
    "aiohttp",
    "openai",
    "anthropic",
    "urllib.request",
    "http.client",
}

FORBIDDEN_NAME_FRAGMENTS = (
    "ProviderAdapter",
    "FakeProvider",
    "DeterministicFakeProviderAdapter",
    "ModelInvocationResult",
    "call_provider",
    "os.getenv",
    "os.environ",
)


def _iter_p3_python_files():
    for directory in P3_SOURCE_DIRS:
        for path in sorted(directory.rglob("*.py")):
            yield path


def test_p3_source_has_no_provider_or_network_implementation():
    for path in _iter_p3_python_files():
        source = path.read_text(encoding="utf-8")
        tree = ast.parse(source, filename=str(path))
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                for alias in node.names:
                    assert alias.name.split(".")[0] not in FORBIDDEN_IMPORT_NAMES, path
            if isinstance(node, ast.ImportFrom) and node.module:
                root = node.module.split(".")[0]
                assert root not in FORBIDDEN_IMPORT_NAMES, path
                assert node.module not in FORBIDDEN_IMPORT_NAMES, path
        for fragment in FORBIDDEN_NAME_FRAGMENTS:
            if fragment in {"os.getenv", "os.environ"}:
                assert fragment not in source, path
            elif f"class {fragment}" in source or f"def {fragment}" in source:
                raise AssertionError(f"forbidden implementation symbol {fragment} in {path}")


def test_network_entrypoints_unused_during_prepare(monkeypatch):
    def boom(*_args, **_kwargs):
        raise AssertionError("network entrypoint must not be used by P3 Gateway")

    monkeypatch.setattr("socket.socket", boom, raising=False)
    monkeypatch.setattr("urllib.request.urlopen", boom, raising=False)
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    assert prepared.request_id == "req-classify-1"


def test_env_secret_lookup_not_used(monkeypatch):
    def boom_getenv(key, default=None):
        raise AssertionError(f"os.getenv must not be used by P3 Gateway: {key}")

    class BoomEnviron(dict):
        def __getitem__(self, key):
            raise AssertionError(f"os.environ must not be used by P3 Gateway: {key}")

        def get(self, key, default=None):
            raise AssertionError(f"os.environ.get must not be used by P3 Gateway: {key}")

    monkeypatch.setattr(os, "getenv", boom_getenv)
    monkeypatch.setattr(os, "environ", BoomEnviron())
    gateway = build_gateway()
    gateway.prepare(gateway_request())


def test_gateway_immutable_and_no_invoke_api():
    gateway = build_gateway()
    with pytest.raises(TypeError):
        gateway.foo = 1  # type: ignore[attr-defined]
    for forbidden in ("invoke", "send", "chat", "generate", "complete", "call_provider"):
        assert not hasattr(gateway, forbidden)


def test_f005_prompt_loader_injection_impossible():
    params = inspect.signature(ModelGateway.__init__).parameters
    assert "prompt_loader" not in params
    with pytest.raises(TypeError):
        ModelGateway(
            routes=(eligible_route(),),
            models=(model_spec(),),
            prompt_registry=prompt_registry(),
            prompt_loader=PromptLoader(prompt_registry()),
            prompt_builder=PromptBuilder(),
            output_schema_registry=OutputSchemaRegistry(),
        )  # type: ignore[call-arg]


def test_f006_shared_validator_injection_impossible():
    params = inspect.signature(ModelGateway.__init__).parameters
    assert "shared_validator" not in params
    with pytest.raises(TypeError):
        ModelGateway(
            routes=(eligible_route(),),
            models=(model_spec(),),
            prompt_registry=prompt_registry(),
            prompt_builder=PromptBuilder(),
            output_schema_registry=OutputSchemaRegistry(),
            shared_validator=SharedContractValidator(OutputSchemaRegistry()),
        )  # type: ignore[call-arg]


def test_f007_foreign_different_config_rejected():
    gateway_a = build_gateway()
    prepared = gateway_a.prepare(gateway_request())

    # route 配置不同：当前 Gateway 无该 route
    gateway_other_route = build_gateway(routes=(eligible_route(route_id="other-color"),))
    with pytest.raises(GatewayRuntimeError) as route_error:
        gateway_other_route.validate_output(prepared, valid_tool_result_payload())
    assert route_error.value.code is GatewayErrorCode.PROVENANCE_INVALID

    # model catalog 不同：selected model 不在当前 Gateway
    gateway_other_models = build_gateway(models=(model_spec(model_id="other-model"),))
    with pytest.raises(GatewayRuntimeError) as model_error:
        gateway_other_models.validate_output(prepared, valid_tool_result_payload())
    assert model_error.value.code is GatewayErrorCode.PROVENANCE_INVALID

    # prompt authority 不同：无法从当前 PromptRegistry 加载 prepared prompt
    foreign_prompts = PromptRegistry(
        [PromptRegistryEntry(prompt_id="other-prompt", version="1.0.0", resource="other-prompt/1.0.0.yaml")]
    )
    gateway_other_prompts = build_gateway(prompt_registry_override=foreign_prompts)
    with pytest.raises(GatewayRuntimeError) as prompt_error:
        gateway_other_prompts.validate_output(prepared, valid_tool_result_payload())
    assert prompt_error.value.code is GatewayErrorCode.PROVENANCE_INVALID


def test_f007_identical_config_portability_allowed():
    gateway_a = build_gateway()
    gateway_b = build_gateway()
    prepared = gateway_a.prepare(gateway_request())
    result = gateway_b.validate_output(prepared, valid_tool_result_payload())
    assert result.valid is True


def test_clinical_route_rejected():
    route = ModelRoutePolicy(
        route_id="classify-color",
        version="1.0.0",
        category=RouteCategory.CLINICAL,
        match=RouteMatch(task_type="classify-color", required_tags=("synthetic",)),
        required_capabilities=("structured-output",),
        primary_model=ModelReference(
            provider_id="synthetic-provider",
            model_id="color-classifier",
            model_version="1.0.0",
        ),
        status=RouteStatus.BLOCKED,
        eligible=False,
    )
    gateway = build_gateway(routes=(route,), models=(model_spec(),))
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.prepare(gateway_request())
    assert error.value.code is GatewayErrorCode.ROUTE_INELIGIBLE


def test_duplicate_model_identity_rejected():
    with pytest.raises(ValueError, match="duplicate ModelSpec"):
        build_gateway(models=(model_spec(), model_spec()))


def test_no_direct_clinical_state_imports_in_p3_source():
    forbidden = ("EncounterCDP", "ClinicalObservation", "TriageAssessment", "StateCommitter")
    for path in _iter_p3_python_files():
        source = path.read_text(encoding="utf-8")
        for name in forbidden:
            assert name not in source, path


def test_prepared_invocation_model_copy_revalidates():
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    with pytest.raises(ValidationError):
        prepared.model_copy(update={"output_contract_id": "missing-contract"})


def test_eg09_offline_gateway_path():
    gateway = build_gateway()
    assert isinstance(gateway, ModelGateway)
    prepared = gateway.prepare(gateway_request())
    result = gateway.validate_output(prepared, valid_tool_result_payload())
    assert result.valid is True
    assert prepared.provenance.prompt_checksum
    assert prepared.provenance.rendered_prompt_digest
    assert prepared.request_snapshot.variables["color_name"] == "blue"
    assert prepared.timeout_policy.timeout_ms == 1000


def _dual_eligible_gateway():
    primary = ModelReference(
        provider_id="synthetic-provider", model_id="primary-model", model_version="1.0.0"
    )
    fallback = ModelReference(
        provider_id="synthetic-provider", model_id="fallback-model", model_version="1.0.0"
    )
    route = eligible_route(primary=primary, fallbacks=(fallback,))
    models = (model_spec(model_id="primary-model"), model_spec(model_id="fallback-model"))
    return build_gateway(routes=(route,), models=models), primary, fallback


def test_rerev_f001_forged_non_winner_rejected_honest_fallback_pass():
    from packages.model_runtime.api.models import ModelLifecycle
    from packages.model_runtime.gateway.models import PreparedInvocation

    gateway, primary, fallback = _dual_eligible_gateway()
    honest = gateway.prepare(gateway_request())
    assert honest.selected_model == primary
    assert honest.provenance.selection_index == 0

    # primary/fallback 均 eligible 时 forged fallback winner 必须 REJECT
    forged = PreparedInvocation(
        request_id=honest.request_id,
        route_id=honest.route_id,
        route_version=honest.route_version,
        selected_model=fallback,
        rendered_prompt=honest.rendered_prompt,
        output_contract_id=honest.output_contract_id,
        output_contract_version=honest.output_contract_version,
        timeout_policy=honest.timeout_policy,
        provenance=honest.provenance.model_copy(
            update={
                "selected_model": fallback,
                "selection_index": 1,
                "fallback_used": True,
            }
        ),
        candidate_models=honest.candidate_models,
        request_snapshot=honest.request_snapshot,
    )
    with pytest.raises(GatewayRuntimeError) as forged_error:
        gateway.validate_output(forged, valid_tool_result_payload())
    assert forged_error.value.code is GatewayErrorCode.PROVENANCE_INVALID

    # primary 不可用时 honest fallback PASS
    for status in (ModelLifecycle.DISABLED, ModelLifecycle.DEPRECATED):
        gw_fallback = build_gateway(
            routes=(eligible_route(primary=primary, fallbacks=(fallback,)),),
            models=(
                model_spec(model_id="primary-model", status=status),
                model_spec(model_id="fallback-model"),
            ),
        )
        prepared = gw_fallback.prepare(gateway_request())
        assert prepared.selected_model == fallback
        assert gw_fallback.validate_output(prepared, valid_tool_result_payload()).valid is True

    # capability / structured-output mismatch → fallback
    gw_cap = build_gateway(
        routes=(eligible_route(primary=primary, fallbacks=(fallback,)),),
        models=(
            model_spec(model_id="primary-model", supports_structured_output=False),
            model_spec(model_id="fallback-model"),
        ),
    )
    prepared_cap = gw_cap.prepare(gateway_request())
    assert prepared_cap.selected_model == fallback
    assert gw_cap.validate_output(prepared_cap, valid_tool_result_payload()).valid is True

    # all ineligible → prepare fail closed
    with pytest.raises(GatewayRuntimeError):
        build_gateway(
            routes=(eligible_route(primary=primary, fallbacks=(fallback,)),),
            models=(
                model_spec(model_id="primary-model", status=ModelLifecycle.DISABLED),
                model_spec(model_id="fallback-model", status=ModelLifecycle.DISABLED),
            ),
        ).prepare(gateway_request())


def test_rerev_f002_timeout_forgery_rejected():
    from packages.model_runtime.api.models import TimeoutPolicy
    from packages.model_runtime.gateway.models import PreparedInvocation

    gateway = build_gateway()
    honest = gateway.prepare(gateway_request())
    assert honest.timeout_policy.timeout_ms == 1000
    forged = PreparedInvocation(
        request_id=honest.request_id,
        route_id=honest.route_id,
        route_version=honest.route_version,
        selected_model=honest.selected_model,
        rendered_prompt=honest.rendered_prompt,
        output_contract_id=honest.output_contract_id,
        output_contract_version=honest.output_contract_version,
        timeout_policy=TimeoutPolicy(timeout_ms=9999),
        provenance=honest.provenance,
        candidate_models=honest.candidate_models,
        request_snapshot=honest.request_snapshot,
    )
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.validate_output(forged, valid_tool_result_payload())
    assert error.value.code is GatewayErrorCode.PROVENANCE_INVALID
    # model_copy 可构造结构合法但 timeout 错配的对象；compatibility 边界必须 REJECT
    copied = honest.model_copy(update={"timeout_policy": TimeoutPolicy(timeout_ms=9999)})
    with pytest.raises(GatewayRuntimeError) as copy_error:
        gateway.validate_output(copied, valid_tool_result_payload())
    assert copy_error.value.code is GatewayErrorCode.PROVENANCE_INVALID


def test_rerev_f003_registry_subclass_impossible():
    with pytest.raises(TypeError):

        class FakeRegistry(OutputSchemaRegistry):  # type: ignore[misc]
            pass

    # 正常 exact registry 仍可注入
    assert build_gateway(output_schema_registry=OutputSchemaRegistry())


def test_rerev_f004_rendered_message_and_variables_binding():
    from packages.model_runtime.gateway.models import PreparedInvocation, compute_rendered_prompt_digest

    gateway = build_gateway()
    honest = gateway.prepare(gateway_request())
    payload = valid_tool_result_payload()
    assert gateway.validate_output(honest, payload).valid is True

    forged_messages = tuple(
        message.model_copy(update={"content": "SYNTHETIC_FORGED_PROMPT_CONTENT_XYZ"})
        for message in honest.rendered_prompt.messages
    )
    forged_rendered = honest.rendered_prompt.model_copy(update={"messages": forged_messages})
    # digest 必须与 forged rendered 一致才能通过模型层；Gateway 仍应因 re-render 不一致 REJECT
    forged_digest = compute_rendered_prompt_digest(forged_rendered)
    forged = PreparedInvocation(
        request_id=honest.request_id,
        route_id=honest.route_id,
        route_version=honest.route_version,
        selected_model=honest.selected_model,
        rendered_prompt=forged_rendered,
        output_contract_id=honest.output_contract_id,
        output_contract_version=honest.output_contract_version,
        timeout_policy=honest.timeout_policy,
        provenance=honest.provenance.model_copy(update={"rendered_prompt_digest": forged_digest}),
        candidate_models=honest.candidate_models,
        request_snapshot=honest.request_snapshot,
    )
    with pytest.raises(GatewayRuntimeError) as content_error:
        gateway.validate_output(forged, payload)
    assert content_error.value.code is GatewayErrorCode.PROVENANCE_INVALID

    # 仅改 variables，不改 rendered → REJECT
    changed_snapshot = honest.request_snapshot.model_copy(
        update={"variables": {"color_name": "red"}}
    )
    vars_forged = PreparedInvocation(
        request_id=honest.request_id,
        route_id=honest.route_id,
        route_version=honest.route_version,
        selected_model=honest.selected_model,
        rendered_prompt=honest.rendered_prompt,
        output_contract_id=honest.output_contract_id,
        output_contract_version=honest.output_contract_version,
        timeout_policy=honest.timeout_policy,
        provenance=honest.provenance,
        candidate_models=honest.candidate_models,
        request_snapshot=changed_snapshot,
    )
    with pytest.raises(GatewayRuntimeError) as vars_error:
        gateway.validate_output(vars_forged, payload)
    assert vars_error.value.code is GatewayErrorCode.PROVENANCE_INVALID

    # route task/tag snapshot mismatch → REJECT
    tag_snapshot = honest.request_snapshot.model_copy(update={"tags": ()})
    tag_forged = PreparedInvocation(
        request_id=honest.request_id,
        route_id=honest.route_id,
        route_version=honest.route_version,
        selected_model=honest.selected_model,
        rendered_prompt=honest.rendered_prompt,
        output_contract_id=honest.output_contract_id,
        output_contract_version=honest.output_contract_version,
        timeout_policy=honest.timeout_policy,
        provenance=honest.provenance,
        candidate_models=honest.candidate_models,
        request_snapshot=tag_snapshot,
    )
    with pytest.raises(GatewayRuntimeError) as tag_error:
        gateway.validate_output(tag_forged, payload)
    assert tag_error.value.code is GatewayErrorCode.PROVENANCE_INVALID

    # digest 篡改 → 模型层或 compatibility REJECT
    with pytest.raises(ValidationError):
        honest.model_copy(
            update={
                "provenance": honest.provenance.model_copy(
                    update={"rendered_prompt_digest": "0" * 64}
                )
            }
        )
