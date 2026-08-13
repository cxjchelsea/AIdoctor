"""P4 provider structural models (candidate-only invocation results)."""

from __future__ import annotations

from enum import Enum
from typing import Annotated, Literal

from pydantic import Field, StrictStr, field_validator, model_validator

from ..api.models import Identifier, StructuralModel, Version
from ..api.types import FrozenJsonObject, validate_identifier, validate_semver
from ..gateway.models import PreparedInvocation
from ..routing.policies import ModelReference
from .errors import ProviderAdapterError, ProviderAdapterErrorCode


# 结果语义：仅表示 provider 候选输出，不是临床/状态/生产审批
RESULT_SEMANTICS: Literal["PROVIDER_CANDIDATE_RESULT"] = "PROVIDER_CANDIDATE_RESULT"

# Fake 场景结果元数据（非 HTTP/SDK 真实错误分类）
SIMULATED_FAILURE_CODE = "SIMULATED_FAILURE"
SIMULATED_TIMEOUT_CODE = "SIMULATED_TIMEOUT"

BoundedErrorDetail = Annotated[StrictStr, Field(min_length=1, max_length=200)]
OptionalErrorCode = Annotated[StrictStr, Field(min_length=1, max_length=64)] | None
OptionalErrorDetail = BoundedErrorDetail | None


class ProviderInvocationOutcome(str, Enum):
    """Bounded fake/provider invocation outcomes for P4."""

    SUCCESS = "SUCCESS"
    FAILURE = "FAILURE"
    TIMEOUT = "TIMEOUT"
    INVALID_OUTPUT = "INVALID_OUTPUT"


class ModelInvocationResult(StructuralModel):
    """Provider candidate result only — not validated clinical/state approval.

    Semantics: PROVIDER_CANDIDATE_RESULT.
    """

    request_id: Identifier
    provider_id: Identifier
    selected_model: ModelReference
    outcome: ProviderInvocationOutcome
    candidate_payload: FrozenJsonObject | None = None
    output_contract_id: Identifier
    output_contract_version: Version
    rendered_prompt_digest: Annotated[StrictStr, Field(min_length=1, max_length=128)]
    fixture_id: Identifier
    error_code: OptionalErrorCode = None
    error_detail: OptionalErrorDetail = None

    _request_id = field_validator("request_id")(validate_identifier)
    _provider_id = field_validator("provider_id")(validate_identifier)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _output_contract_version = field_validator("output_contract_version")(validate_semver)
    _fixture_id = field_validator("fixture_id")(validate_identifier)

    @model_validator(mode="after")
    def validate_outcome_coherence(self) -> "ModelInvocationResult":
        """Enforce SUCCESS/FAILURE/TIMEOUT/INVALID_OUTPUT payload/error invariants."""

        if self.outcome == ProviderInvocationOutcome.SUCCESS:
            if self.candidate_payload is None:
                raise ValueError("SUCCESS requires candidate_payload")
            if self.error_code is not None or self.error_detail is not None:
                raise ValueError("SUCCESS forbids error_code and error_detail")
            return self

        if self.outcome == ProviderInvocationOutcome.INVALID_OUTPUT:
            if self.candidate_payload is None:
                raise ValueError("INVALID_OUTPUT requires candidate_payload")
            if self.error_code is not None or self.error_detail is not None:
                raise ValueError("INVALID_OUTPUT forbids provider error fields")
            return self

        if self.outcome == ProviderInvocationOutcome.FAILURE:
            if self.candidate_payload is not None:
                raise ValueError("FAILURE forbids candidate_payload")
            if self.error_code != SIMULATED_FAILURE_CODE:
                raise ValueError("FAILURE requires error_code=SIMULATED_FAILURE")
            if self.error_detail is None:
                raise ValueError("FAILURE requires error_detail")
            return self

        if self.outcome == ProviderInvocationOutcome.TIMEOUT:
            if self.candidate_payload is not None:
                raise ValueError("TIMEOUT forbids candidate_payload")
            if self.error_code != SIMULATED_TIMEOUT_CODE:
                raise ValueError("TIMEOUT requires error_code=SIMULATED_TIMEOUT")
            if self.error_detail is None:
                raise ValueError("TIMEOUT requires error_detail")
            return self

        raise ValueError("unsupported provider invocation outcome")


class FakeProviderFixture(StructuralModel):
    """Immutable exact fixture row for DeterministicFakeProviderAdapter."""

    fixture_id: Identifier
    provider_id: Identifier
    outcome: ProviderInvocationOutcome
    output_contract_id: Identifier
    output_contract_version: Version
    candidate_payload: FrozenJsonObject | None = None
    error_code: OptionalErrorCode = None
    error_detail: OptionalErrorDetail = None

    _fixture_id = field_validator("fixture_id")(validate_identifier)
    _provider_id = field_validator("provider_id")(validate_identifier)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _output_contract_version = field_validator("output_contract_version")(validate_semver)

    @model_validator(mode="after")
    def validate_fixture_coherence(self) -> "FakeProviderFixture":
        """Mirror result invariants so catalog data cannot be incoherent."""

        if self.outcome in {
            ProviderInvocationOutcome.SUCCESS,
            ProviderInvocationOutcome.INVALID_OUTPUT,
        }:
            if self.candidate_payload is None:
                raise ValueError(f"{self.outcome.value} fixture requires candidate_payload")
            if self.error_code is not None or self.error_detail is not None:
                raise ValueError(f"{self.outcome.value} fixture forbids error fields")
            return self

        if self.outcome == ProviderInvocationOutcome.FAILURE:
            if self.candidate_payload is not None:
                raise ValueError("FAILURE fixture forbids candidate_payload")
            if self.error_code != SIMULATED_FAILURE_CODE or self.error_detail is None:
                raise ValueError("FAILURE fixture requires SIMULATED_FAILURE and detail")
            return self

        if self.outcome == ProviderInvocationOutcome.TIMEOUT:
            if self.candidate_payload is not None:
                raise ValueError("TIMEOUT fixture forbids candidate_payload")
            if self.error_code != SIMULATED_TIMEOUT_CODE or self.error_detail is None:
                raise ValueError("TIMEOUT fixture requires SIMULATED_TIMEOUT and detail")
            return self

        raise ValueError("unsupported fixture outcome")


def assert_result_compatible(
    prepared: PreparedInvocation,
    result: ModelInvocationResult,
) -> None:
    """Structural binding check between PreparedInvocation and ModelInvocationResult.

    This is deterministic configuration coherence only — not cryptographic proof.
    """

    if result.request_id != prepared.request_id:
        raise ProviderAdapterError(
            ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID,
            "result request_id does not match prepared request",
        )
    if result.provider_id != prepared.selected_model.provider_id:
        raise ProviderAdapterError(
            ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID,
            "result provider_id does not match prepared selected provider",
        )
    if result.selected_model != prepared.selected_model:
        raise ProviderAdapterError(
            ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID,
            "result selected_model does not match prepared selected_model",
        )
    if result.output_contract_id != prepared.output_contract_id:
        raise ProviderAdapterError(
            ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID,
            "result output_contract_id does not match prepared contract",
        )
    if result.output_contract_version != prepared.output_contract_version:
        raise ProviderAdapterError(
            ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID,
            "result output_contract_version does not match prepared contract",
        )
    if result.rendered_prompt_digest != prepared.provenance.rendered_prompt_digest:
        raise ProviderAdapterError(
            ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID,
            "result rendered_prompt_digest does not match prepared provenance",
        )
