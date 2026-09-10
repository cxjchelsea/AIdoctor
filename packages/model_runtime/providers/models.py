"""NC-09 provider-independent structural models (candidate-only results)."""

from __future__ import annotations

import re
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

# 通用错误码语法（非 Fake 专用 SIMULATED_* 约束）
PROVIDER_ERROR_CODE_PATTERN = re.compile(r"^[A-Z][A-Z0-9_]{0,63}$")

BoundedErrorDetail = Annotated[StrictStr, Field(min_length=1, max_length=200)]
ProviderErrorCode = Annotated[StrictStr, Field(min_length=1, max_length=64)]


class ProviderInvocationOutcome(str, Enum):
    """Generic provider-independent invocation outcomes (NC-09)."""

    SUCCESS = "SUCCESS"
    FAILURE = "FAILURE"
    TIMEOUT = "TIMEOUT"


class ModelInvocationResult(StructuralModel):
    """Provider-independent candidate result — not validated clinical/state approval.

    Semantics: PROVIDER_CANDIDATE_RESULT.
    Fake-only concepts (fixture_id / SIMULATED_* / INVALID_OUTPUT) do not belong here.
    """

    request_id: Identifier
    provider_id: Identifier
    selected_model: ModelReference
    outcome: ProviderInvocationOutcome
    candidate_payload: FrozenJsonObject | None = None
    output_contract_id: Identifier
    output_contract_version: Version
    rendered_prompt_digest: Annotated[StrictStr, Field(min_length=1, max_length=128)]
    error_code: ProviderErrorCode | None = None
    error_detail: BoundedErrorDetail | None = None

    _request_id = field_validator("request_id")(validate_identifier)
    _provider_id = field_validator("provider_id")(validate_identifier)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _output_contract_version = field_validator("output_contract_version")(validate_semver)

    @field_validator("error_code")
    @classmethod
    def validate_error_code_syntax(cls, value: str | None) -> str | None:
        if value is None:
            return None
        if not PROVIDER_ERROR_CODE_PATTERN.fullmatch(value):
            raise ValueError("error_code must match ^[A-Z][A-Z0-9_]{0,63}$")
        return value

    @model_validator(mode="after")
    def validate_outcome_coherence(self) -> "ModelInvocationResult":
        """Enforce generic SUCCESS/FAILURE/TIMEOUT payload/error invariants."""

        if self.outcome == ProviderInvocationOutcome.SUCCESS:
            if self.candidate_payload is None:
                raise ValueError("SUCCESS requires candidate_payload")
            if self.error_code is not None or self.error_detail is not None:
                raise ValueError("SUCCESS forbids error_code and error_detail")
            return self

        if self.outcome == ProviderInvocationOutcome.FAILURE:
            if self.candidate_payload is not None:
                raise ValueError("FAILURE forbids candidate_payload")
            if self.error_code is None or self.error_detail is None:
                raise ValueError("FAILURE requires error_code and error_detail")
            return self

        if self.outcome == ProviderInvocationOutcome.TIMEOUT:
            if self.candidate_payload is not None:
                raise ValueError("TIMEOUT forbids candidate_payload")
            if self.error_code is None or self.error_detail is None:
                raise ValueError("TIMEOUT requires error_code and error_detail")
            return self

        raise ValueError("unsupported provider invocation outcome")


def assert_result_compatible(
    prepared: PreparedInvocation,
    result: ModelInvocationResult,
) -> None:
    """Generic PreparedInvocation ↔ ModelInvocationResult coherence only.

    Does not prove Fake fixture provenance.
    """

    if result.request_id != prepared.request_id:
        raise ProviderAdapterError(ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID)
    if result.provider_id != prepared.selected_model.provider_id:
        raise ProviderAdapterError(ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID)
    if result.selected_model != prepared.selected_model:
        raise ProviderAdapterError(ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID)
    if result.output_contract_id != prepared.output_contract_id:
        raise ProviderAdapterError(ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID)
    if result.output_contract_version != prepared.output_contract_version:
        raise ProviderAdapterError(ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID)
    if result.rendered_prompt_digest != prepared.provenance.rendered_prompt_digest:
        raise ProviderAdapterError(ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID)
