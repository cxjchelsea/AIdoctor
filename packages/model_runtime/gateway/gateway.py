"""Provider-independent Model Gateway preparation and output validation boundary."""

from __future__ import annotations

from collections.abc import Iterable, Mapping
from types import MappingProxyType
from typing import Any

from ..api.models import ModelSpec
from ..prompts import PromptBuilder, PromptLoader, PromptRegistry, PromptRuntimeError
from ..routing.policies import ModelRoutePolicy
from ..schemas import (
    OutputSchemaRegistry,
    SchemaRegistryError,
    SchemaRegistryErrorCode,
    SharedContractValidator,
)
from .errors import GatewayErrorCode, GatewayRuntimeError
from .models import (
    GatewayProvenance,
    GatewayRequest,
    OutputValidationResult,
    PreparedInvocation,
    compute_rendered_prompt_digest,
)
from .selection import (
    assert_route_eligible,
    assert_route_match,
    select_structural_model,
)


class ModelGateway:
    """Deterministic prepare/validate boundary; never invokes providers or network."""

    __slots__ = (
        "_routes",
        "_models",
        "_prompt_registry",
        "_prompt_loader",
        "_prompt_builder",
        "_output_schema_registry",
        "_shared_validator",
    )

    def __init__(
        self,
        *,
        routes: Iterable[ModelRoutePolicy],
        models: Iterable[ModelSpec],
        prompt_registry: PromptRegistry,
        prompt_builder: PromptBuilder,
        output_schema_registry: OutputSchemaRegistry | None = None,
    ) -> None:
        route_index: dict[tuple[str, str], ModelRoutePolicy] = {}
        for policy in routes:
            if not isinstance(policy, ModelRoutePolicy):
                raise TypeError("routes must contain ModelRoutePolicy instances")
            key = (policy.route_id, policy.version)
            if key in route_index:
                raise ValueError(f"duplicate route identity: {policy.route_id}@{policy.version}")
            route_index[key] = policy

        model_index: dict[tuple[str, str, str], ModelSpec] = {}
        for spec in models:
            if not isinstance(spec, ModelSpec):
                raise TypeError("models must contain ModelSpec instances")
            key = (spec.provider_id, spec.model_id, spec.version)
            if key in model_index:
                raise ValueError(
                    f"duplicate ModelSpec identity: {spec.provider_id}/{spec.model_id}@{spec.version}"
                )
            model_index[key] = spec

        if not isinstance(prompt_registry, PromptRegistry):
            raise TypeError("prompt_registry must be a PromptRegistry")
        if not isinstance(prompt_builder, PromptBuilder):
            raise TypeError("prompt_builder must be a PromptBuilder")

        # REREV-F003：Schema 权威必须是 sealed exact OutputSchemaRegistry（禁止 subclass）
        if output_schema_registry is None:
            schema_registry = OutputSchemaRegistry()
        else:
            if type(output_schema_registry) is not OutputSchemaRegistry:
                raise TypeError("output_schema_registry must be exact OutputSchemaRegistry")
            if len(output_schema_registry) != 13:
                raise ValueError("output_schema_registry must be the exact Shared Contracts v1 catalog")
            schema_registry = output_schema_registry

        # Prompt 权威唯一链 — registry → Gateway-owned loader → builder
        prompt_loader = PromptLoader(prompt_registry)
        shared_validator = SharedContractValidator(schema_registry)

        object.__setattr__(self, "_routes", MappingProxyType(route_index))
        object.__setattr__(self, "_models", MappingProxyType(model_index))
        object.__setattr__(self, "_prompt_registry", prompt_registry)
        object.__setattr__(self, "_prompt_loader", prompt_loader)
        object.__setattr__(self, "_prompt_builder", prompt_builder)
        object.__setattr__(self, "_output_schema_registry", schema_registry)
        object.__setattr__(self, "_shared_validator", shared_validator)

    def __setattr__(self, _name: str, _value: object) -> None:
        raise TypeError("ModelGateway is immutable")

    def prepare(self, request: GatewayRequest) -> PreparedInvocation:
        """Validate request, select route/model, render prompt, and emit PreparedInvocation."""

        if not isinstance(request, GatewayRequest):
            raise GatewayRuntimeError(GatewayErrorCode.REQUEST_INVALID, "request must be GatewayRequest")

        route_key = (request.route_id, request.route_version)
        policy = self._routes.get(route_key)
        if policy is None:
            raise GatewayRuntimeError(
                GatewayErrorCode.ROUTE_NOT_FOUND,
                f"route is not registered: {request.route_id}@{request.route_version}",
            )

        assert_route_eligible(policy)
        assert_route_match(policy, request)

        try:
            self._output_schema_registry.get(request.output_contract_id, request.output_contract_version)
        except SchemaRegistryError as exc:
            if exc.code is SchemaRegistryErrorCode.CONTRACT_UNKNOWN:
                raise GatewayRuntimeError(GatewayErrorCode.CONTRACT_UNKNOWN, exc.detail) from exc
            if exc.code is SchemaRegistryErrorCode.CONTRACT_VERSION_UNKNOWN:
                raise GatewayRuntimeError(GatewayErrorCode.CONTRACT_VERSION_UNKNOWN, exc.detail) from exc
            raise GatewayRuntimeError(GatewayErrorCode.SELECTION_FAILED, exc.detail) from exc

        selected_reference, selected_spec, topology, selection_index = select_structural_model(
            policy, dict(self._models)
        )

        try:
            document = self._prompt_loader.load(request.prompt_id, request.prompt_version)
        except PromptRuntimeError:
            raise

        if (
            document.spec.output_contract_id != request.output_contract_id
            or document.spec.output_contract_version != request.output_contract_version
        ):
            raise GatewayRuntimeError(
                GatewayErrorCode.CONTRACT_MISMATCH,
                "prompt output contract does not match gateway request output contract",
            )

        rendered = self._prompt_builder.build(document, dict(request.variables))
        rendered_digest = compute_rendered_prompt_digest(rendered)
        provenance = GatewayProvenance(
            request_id=request.request_id,
            route_id=request.route_id,
            route_version=request.route_version,
            selected_model=selected_reference,
            prompt_id=rendered.prompt_id,
            prompt_version=rendered.version,
            prompt_checksum=rendered.resource_checksum,
            rendered_prompt_digest=rendered_digest,
            output_contract_id=request.output_contract_id,
            output_contract_version=request.output_contract_version,
            selection_index=selection_index,
            fallback_used=selection_index > 0,
            candidate_model_count=len(topology),
        )
        return PreparedInvocation(
            request_id=request.request_id,
            route_id=request.route_id,
            route_version=request.route_version,
            selected_model=selected_reference,
            rendered_prompt=rendered,
            output_contract_id=request.output_contract_id,
            output_contract_version=request.output_contract_version,
            timeout_policy=selected_spec.timeout_policy,
            provenance=provenance,
            candidate_models=topology,
            request_snapshot=request,
        )

    def _assert_prepared_compatible(self, prepared: PreparedInvocation) -> None:
        """证明 PreparedInvocation 可由当前 Gateway 配置重新独立准备得到。"""

        if not isinstance(prepared, PreparedInvocation):
            raise GatewayRuntimeError(
                GatewayErrorCode.REQUEST_INVALID,
                "prepared must be PreparedInvocation",
            )

        snapshot = prepared.request_snapshot
        route_key = (snapshot.route_id, snapshot.route_version)
        policy = self._routes.get(route_key)
        if policy is None:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared route is not present in current Gateway configuration",
            )
        try:
            assert_route_eligible(policy)
        except GatewayRuntimeError as exc:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                f"prepared route is not eligible in current Gateway: {exc.detail}",
            ) from exc
        try:
            assert_route_match(policy, snapshot)
        except GatewayRuntimeError as exc:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                f"prepared request_snapshot does not match current route: {exc.detail}",
            ) from exc

        # REREV-F001：复用 authoritative selection，禁止仅检查 topology 成员资格
        try:
            (
                expected_selected_reference,
                expected_selected_spec,
                expected_topology,
                expected_selection_index,
            ) = select_structural_model(policy, dict(self._models))
        except GatewayRuntimeError as exc:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                f"current Gateway cannot recompute selection for prepared artifact: {exc.detail}",
            ) from exc

        if prepared.candidate_models != expected_topology:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared candidate topology does not match current deterministic selection",
            )
        if prepared.selected_model != expected_selected_reference:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared selected_model is not the current deterministic winner",
            )
        if prepared.provenance.selected_model != expected_selected_reference:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared provenance.selected_model is not the current deterministic winner",
            )
        if prepared.provenance.selection_index != expected_selection_index:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared selection_index does not match current deterministic winner",
            )
        if prepared.provenance.fallback_used != (expected_selection_index > 0):
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared fallback_used does not match current deterministic winner",
            )
        if prepared.provenance.candidate_model_count != len(expected_topology):
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared candidate_model_count does not match current topology",
            )

        # REREV-F002：timeout 必须绑定当前 deterministic selected ModelSpec
        if prepared.timeout_policy != expected_selected_spec.timeout_policy:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared timeout_policy does not match current selected ModelSpec",
            )

        try:
            self._output_schema_registry.get(
                snapshot.output_contract_id, snapshot.output_contract_version
            )
        except SchemaRegistryError as exc:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared output contract is not in current exact schema registry",
            ) from exc
        if (
            prepared.output_contract_id != snapshot.output_contract_id
            or prepared.output_contract_version != snapshot.output_contract_version
        ):
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared output contract diverges from request_snapshot",
            )

        # REREV-F004：加载当前 Prompt，并完整 re-render 后比较 RenderedPrompt
        try:
            document = self._prompt_loader.load(snapshot.prompt_id, snapshot.prompt_version)
        except PromptRuntimeError as exc:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared prompt is not loadable from current Gateway Prompt authority",
            ) from exc
        if (
            document.spec.output_contract_id != snapshot.output_contract_id
            or document.spec.output_contract_version != snapshot.output_contract_version
        ):
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "current prompt output contract does not match prepared request_snapshot",
            )
        expected_rendered = self._prompt_builder.build(document, dict(snapshot.variables))
        if expected_rendered != prepared.rendered_prompt:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared rendered_prompt does not match current Gateway re-render",
            )
        expected_digest = compute_rendered_prompt_digest(expected_rendered)
        if prepared.provenance.rendered_prompt_digest != expected_digest:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared rendered_prompt_digest does not match current re-render",
            )
        if prepared.provenance.prompt_checksum != expected_rendered.resource_checksum:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared prompt_checksum does not match current Prompt authority",
            )

    def validate_output(
        self,
        prepared: PreparedInvocation,
        candidate: Mapping[str, Any],
        *,
        contract_id: str | None = None,
        contract_version: str | None = None,
    ) -> OutputValidationResult:
        """Validate a caller-supplied candidate against the prepared output contract."""

        if not isinstance(prepared, PreparedInvocation):
            raise GatewayRuntimeError(
                GatewayErrorCode.REQUEST_INVALID,
                "prepared must be PreparedInvocation",
            )
        # 先证明 configuration-bound provenance，再进入 candidate schema validation
        self._assert_prepared_compatible(prepared)
        if contract_id is not None or contract_version is not None:
            if (
                contract_id != prepared.output_contract_id
                or contract_version != prepared.output_contract_version
            ):
                raise GatewayRuntimeError(
                    GatewayErrorCode.CONTRACT_MISMATCH,
                    "validation contract identity must match PreparedInvocation",
                )
        try:
            valid, errors = self._shared_validator.validate(
                prepared.output_contract_id,
                prepared.output_contract_version,
                candidate,
            )
        except SchemaRegistryError as exc:
            if exc.code is SchemaRegistryErrorCode.CONTRACT_UNKNOWN:
                raise GatewayRuntimeError(GatewayErrorCode.CONTRACT_UNKNOWN, exc.detail) from exc
            if exc.code is SchemaRegistryErrorCode.CONTRACT_VERSION_UNKNOWN:
                raise GatewayRuntimeError(
                    GatewayErrorCode.CONTRACT_VERSION_UNKNOWN, exc.detail
                ) from exc
            raise GatewayRuntimeError(GatewayErrorCode.OUTPUT_INVALID, exc.detail) from exc

        return OutputValidationResult(
            valid=valid,
            errors=errors,
            contract_id=prepared.output_contract_id,
            contract_version=prepared.output_contract_version,
            request_id=prepared.request_id,
        )
