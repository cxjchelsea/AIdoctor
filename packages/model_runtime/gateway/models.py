"""Provider-independent Model Gateway structural envelopes."""

from __future__ import annotations

import hashlib
import json
from typing import Annotated

from pydantic import Field, StrictBool, StrictInt, StrictStr, field_validator, model_validator

from ..api.models import (
    SHARED_CONTRACT_V1_IDS,
    SHARED_CONTRACT_V1_VERSION,
    Identifier,
    StructuralModel,
    TimeoutPolicy,
    Version,
)
from ..api.types import FrozenJsonObject, validate_identifier, validate_semver
from ..prompts.models import RenderedPrompt
from ..routing.policies import ModelReference


def compute_rendered_prompt_digest(rendered: RenderedPrompt) -> str:
    """Deterministic offline SHA-256 over complete RenderedPrompt canonical JSON."""

    if not isinstance(rendered, RenderedPrompt):
        raise TypeError("rendered must be RenderedPrompt")
    payload = rendered.model_dump(mode="json")
    canonical = json.dumps(
        payload,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
        allow_nan=False,
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


class GatewayRequest(StructuralModel):
    """Explicit, immutable gateway preparation request; no provider or clinical fields."""

    request_id: Identifier
    route_id: Identifier
    route_version: Version
    task_type: Identifier
    tags: tuple[Identifier, ...] = ()
    prompt_id: Identifier
    prompt_version: Version
    variables: FrozenJsonObject = Field(default_factory=FrozenJsonObject)
    output_contract_id: Identifier
    output_contract_version: Version

    _request_id = field_validator("request_id")(validate_identifier)
    _route_id = field_validator("route_id")(validate_identifier)
    _task_type = field_validator("task_type")(validate_identifier)
    _prompt_id = field_validator("prompt_id")(validate_identifier)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _versions = field_validator("route_version", "prompt_version", "output_contract_version")(
        validate_semver
    )

    @field_validator("tags")
    @classmethod
    def validate_tags(cls, value: tuple[str, ...]) -> tuple[str, ...]:
        validated = tuple(validate_identifier(item) for item in value)
        if len(set(validated)) != len(validated):
            raise ValueError("tags must be unique")
        return validated

    @field_validator("output_contract_id")
    @classmethod
    def validate_output_contract_id(cls, value: str) -> str:
        if value not in SHARED_CONTRACT_V1_IDS:
            raise ValueError("must reference an existing Shared Contracts v1 identifier")
        return value

    @field_validator("output_contract_version")
    @classmethod
    def validate_output_contract_version(cls, value: str) -> str:
        if value != SHARED_CONTRACT_V1_VERSION:
            raise ValueError(f"must equal Shared Contracts v1 version {SHARED_CONTRACT_V1_VERSION}")
        return value


class GatewayProvenance(StructuralModel):
    """Deterministic structural provenance; no clock/random/provider/patient fields."""

    request_id: Identifier
    route_id: Identifier
    route_version: Version
    selected_model: ModelReference
    prompt_id: Identifier
    prompt_version: Version
    prompt_checksum: Annotated[StrictStr, Field(pattern=r"^[0-9a-f]{64}$")]
    rendered_prompt_digest: Annotated[StrictStr, Field(pattern=r"^[0-9a-f]{64}$")]
    output_contract_id: Identifier
    output_contract_version: Version
    selection_index: StrictInt = Field(ge=0)
    fallback_used: StrictBool
    candidate_model_count: StrictInt = Field(ge=1)

    _request_id = field_validator("request_id")(validate_identifier)
    _route_id = field_validator("route_id")(validate_identifier)
    _prompt_id = field_validator("prompt_id")(validate_identifier)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _versions = field_validator("route_version", "prompt_version", "output_contract_version")(
        validate_semver
    )

    @field_validator("output_contract_id")
    @classmethod
    def validate_output_contract_id(cls, value: str) -> str:
        if value not in SHARED_CONTRACT_V1_IDS:
            raise ValueError("must reference an existing Shared Contracts v1 identifier")
        return value

    @field_validator("output_contract_version")
    @classmethod
    def validate_output_contract_version(cls, value: str) -> str:
        if value != SHARED_CONTRACT_V1_VERSION:
            raise ValueError(f"must equal Shared Contracts v1 version {SHARED_CONTRACT_V1_VERSION}")
        return value

    @model_validator(mode="after")
    def validate_selection_coherence(self) -> "GatewayProvenance":
        if self.fallback_used and self.selection_index < 1:
            raise ValueError("fallback_used requires selection_index >= 1")
        if not self.fallback_used and self.selection_index != 0:
            raise ValueError("primary selection must use selection_index 0")
        if self.selection_index >= self.candidate_model_count:
            raise ValueError("selection_index must be within candidate_model_count")
        return self


class PreparedInvocation(StructuralModel):
    """Provider-independent preparation result; not a ModelInvocationResult."""

    request_id: Identifier
    route_id: Identifier
    route_version: Version
    selected_model: ModelReference
    rendered_prompt: RenderedPrompt
    output_contract_id: Identifier
    output_contract_version: Version
    timeout_policy: TimeoutPolicy
    provenance: GatewayProvenance
    candidate_models: tuple[ModelReference, ...]
    # REREV-F004：完整不可变请求快照，供 Gateway 重新证明 preparation
    request_snapshot: GatewayRequest

    _request_id = field_validator("request_id")(validate_identifier)
    _route_id = field_validator("route_id")(validate_identifier)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _versions = field_validator("route_version", "output_contract_version")(validate_semver)

    @field_validator("candidate_models")
    @classmethod
    def validate_candidates(cls, value: tuple[ModelReference, ...]) -> tuple[ModelReference, ...]:
        if not value:
            raise ValueError("at least one candidate model is required")
        identities = [(item.provider_id, item.model_id, item.model_version) for item in value]
        if len(set(identities)) != len(identities):
            raise ValueError("candidate models must be unique")
        return value

    @field_validator("output_contract_id")
    @classmethod
    def validate_output_contract_id(cls, value: str) -> str:
        if value not in SHARED_CONTRACT_V1_IDS:
            raise ValueError("must reference an existing Shared Contracts v1 identifier")
        return value

    @field_validator("output_contract_version")
    @classmethod
    def validate_output_contract_version(cls, value: str) -> str:
        if value != SHARED_CONTRACT_V1_VERSION:
            raise ValueError(f"must equal Shared Contracts v1 version {SHARED_CONTRACT_V1_VERSION}")
        return value

    @model_validator(mode="after")
    def validate_coherence(self) -> "PreparedInvocation":
        if self.selected_model not in self.candidate_models:
            raise ValueError("selected_model must appear in candidate_models")
        if self.provenance.request_id != self.request_id:
            raise ValueError("provenance.request_id must equal request_id")
        if self.provenance.route_id != self.route_id or self.provenance.route_version != self.route_version:
            raise ValueError("provenance route identity must match prepared invocation")
        if self.provenance.selected_model != self.selected_model:
            raise ValueError("provenance.selected_model must match selected_model")
        if self.provenance.prompt_id != self.rendered_prompt.prompt_id:
            raise ValueError("provenance.prompt_id must match rendered prompt")
        if self.provenance.prompt_version != self.rendered_prompt.version:
            raise ValueError("provenance.prompt_version must match rendered prompt")
        if self.provenance.prompt_checksum != self.rendered_prompt.resource_checksum:
            raise ValueError("provenance.prompt_checksum must match rendered prompt checksum")
        if (
            self.provenance.output_contract_id != self.output_contract_id
            or self.provenance.output_contract_version != self.output_contract_version
        ):
            raise ValueError("provenance output contract must match prepared invocation")
        if self.provenance.candidate_model_count != len(self.candidate_models):
            raise ValueError("provenance.candidate_model_count must equal candidate_models length")
        selected_index = self.candidate_models.index(self.selected_model)
        if self.provenance.selection_index != selected_index:
            raise ValueError("provenance.selection_index must match selected model position")
        if self.provenance.fallback_used != (selected_index > 0):
            raise ValueError("provenance.fallback_used must match selection position")

        # request_snapshot 与顶层 convenience 字段不得分裂
        snapshot = self.request_snapshot
        if snapshot.request_id != self.request_id:
            raise ValueError("request_snapshot.request_id must equal request_id")
        if snapshot.route_id != self.route_id or snapshot.route_version != self.route_version:
            raise ValueError("request_snapshot route identity must match prepared invocation")
        if (
            snapshot.output_contract_id != self.output_contract_id
            or snapshot.output_contract_version != self.output_contract_version
        ):
            raise ValueError("request_snapshot output contract must match prepared invocation")
        if snapshot.prompt_id != self.rendered_prompt.prompt_id:
            raise ValueError("request_snapshot.prompt_id must match rendered prompt")
        if snapshot.prompt_version != self.rendered_prompt.version:
            raise ValueError("request_snapshot.prompt_version must match rendered prompt")

        # rendered_prompt_digest 与完整 RenderedPrompt 必须一致
        expected_digest = compute_rendered_prompt_digest(self.rendered_prompt)
        if self.provenance.rendered_prompt_digest != expected_digest:
            raise ValueError("provenance.rendered_prompt_digest must match rendered prompt digest")
        return self


class OutputValidationResult(StructuralModel):
    """Deterministic observational validation result for a caller-supplied candidate."""

    valid: StrictBool
    errors: tuple[StrictStr, ...] = ()
    contract_id: Identifier
    contract_version: Version
    request_id: Identifier

    _contract_id = field_validator("contract_id")(validate_identifier)
    _request_id = field_validator("request_id")(validate_identifier)
    _contract_version = field_validator("contract_version")(validate_semver)

    @field_validator("errors")
    @classmethod
    def validate_errors(cls, value: tuple[str, ...]) -> tuple[str, ...]:
        if any(not item for item in value):
            raise ValueError("error messages must be non-empty")
        if tuple(sorted(value)) != value:
            raise ValueError("errors must be deterministically sorted")
        return value

    @model_validator(mode="after")
    def validate_validity_coherence(self) -> "OutputValidationResult":
        if self.valid and self.errors:
            raise ValueError("valid result cannot carry errors")
        if not self.valid and not self.errors:
            raise ValueError("invalid result must carry at least one error")
        if self.contract_id not in SHARED_CONTRACT_V1_IDS:
            raise ValueError("must reference an existing Shared Contracts v1 identifier")
        if self.contract_version != SHARED_CONTRACT_V1_VERSION:
            raise ValueError(f"must equal Shared Contracts v1 version {SHARED_CONTRACT_V1_VERSION}")
        return self
