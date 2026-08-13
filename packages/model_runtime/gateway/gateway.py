"""Provider-independent Model Gateway preparation and output validation boundary."""

from __future__ import annotations

from collections.abc import Iterable, Mapping
from types import MappingProxyType
from typing import Any

from ..api.models import ModelSpec
from ..prompts import PromptBuilder, PromptLoader, PromptRegistry, PromptRuntimeError, compute_prompt_checksum
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
)
from .selection import (
    assert_route_eligible,
    assert_route_match,
    model_is_structurally_eligible,
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

        # F002/F006：Schema 权威唯一链 — exact registry → Gateway-owned validator
        if output_schema_registry is None:
            schema_registry = OutputSchemaRegistry()
        else:
            if not isinstance(output_schema_registry, OutputSchemaRegistry):
                raise TypeError("output_schema_registry must be an OutputSchemaRegistry")
            if len(output_schema_registry) != 13:
                raise ValueError("output_schema_registry must be the exact Shared Contracts v1 catalog")
            schema_registry = output_schema_registry

        # F005：Prompt 权威唯一链 — registry → Gateway-owned loader → builder
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
        provenance = GatewayProvenance(
            request_id=request.request_id,
            route_id=request.route_id,
            route_version=request.route_version,
            selected_model=selected_reference,
            prompt_id=rendered.prompt_id,
            prompt_version=rendered.version,
            prompt_checksum=rendered.resource_checksum,
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
        )

    def _assert_prepared_compatible(self, prepared: PreparedInvocation) -> None:
        """F007：PreparedInvocation 必须与当前 Gateway 不可变配置相容。"""

        route_key = (prepared.route_id, prepared.route_version)
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

        if policy.primary_model is None:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared route lacks primary model in current Gateway",
            )
        topology = (policy.primary_model, *policy.fallback_models)
        if prepared.candidate_models != topology:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared candidate topology does not match current route policy",
            )
        if prepared.selected_model not in topology:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared selected_model is outside current route topology",
            )
        selected_index = topology.index(prepared.selected_model)
        if prepared.provenance.selection_index != selected_index:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared selection_index does not match current topology",
            )

        identity = (
            prepared.selected_model.provider_id,
            prepared.selected_model.model_id,
            prepared.selected_model.model_version,
        )
        selected_spec = self._models.get(identity)
        if selected_spec is None:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared selected model is not present in current Gateway ModelSpec catalog",
            )
        if not model_is_structurally_eligible(selected_spec, policy.required_capabilities):
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared selected model is not structurally eligible in current Gateway",
            )

        try:
            self._output_schema_registry.get(
                prepared.output_contract_id, prepared.output_contract_version
            )
        except SchemaRegistryError as exc:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared output contract is not in current exact schema registry",
            ) from exc

        try:
            document = self._prompt_loader.load(
                prepared.rendered_prompt.prompt_id,
                prepared.rendered_prompt.version,
            )
        except PromptRuntimeError as exc:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared prompt is not loadable from current Gateway Prompt authority",
            ) from exc
        current_checksum = compute_prompt_checksum(document)
        if current_checksum != prepared.rendered_prompt.resource_checksum:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared prompt checksum does not match current Prompt authority",
            )
        if prepared.provenance.prompt_checksum != prepared.rendered_prompt.resource_checksum:
            raise GatewayRuntimeError(
                GatewayErrorCode.PROVENANCE_INVALID,
                "prepared provenance checksum is incoherent",
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
