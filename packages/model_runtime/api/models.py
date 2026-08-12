"""Declarative model and prompt specifications.

These types contain metadata only. They deliberately have no provider, prompt
loading, rendering, registry, or execution behavior.
"""

from __future__ import annotations

import re
from enum import Enum
from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, StrictBool, StrictInt, StrictStr, field_validator

from .types import FrozenJsonObject, validate_identifier, validate_semver


CHECKSUM_PATTERN = re.compile(r"^(?:sha256:)?[0-9a-f]{64}$")
SHARED_CONTRACT_V1_VERSION = "1.0.0"
SHARED_CONTRACT_V1_IDS = frozenset(
    {
        "audit-ref",
        "commit-result",
        "contract-conflict",
        "contract-envelope",
        "evidence-pack",
        "identifier-set",
        "knowledge-release-ref",
        "patient-delivery-view",
        "source-artifact",
        "state-patch",
        "tool-context",
        "tool-result",
        "trace-ref",
    }
)

Identifier = Annotated[StrictStr, Field(min_length=1, max_length=128)]
Version = Annotated[StrictStr, Field(min_length=5, max_length=128)]
PositiveInt = Annotated[StrictInt, Field(gt=0)]
NonNegativeInt = Annotated[StrictInt, Field(ge=0)]


class StructuralModel(BaseModel):
    """Strict immutable base for deterministic structural metadata."""

    model_config = ConfigDict(extra="forbid", frozen=True)


class ModelLifecycle(str, Enum):
    DRAFT = "DRAFT"
    DISABLED = "DISABLED"
    DEPRECATED = "DEPRECATED"


class PromptLifecycle(str, Enum):
    DRAFT = "DRAFT"
    DISABLED = "DISABLED"
    DEPRECATED = "DEPRECATED"


class RetryClass(str, Enum):
    NONE = "NONE"
    TRANSIENT = "TRANSIENT"
    RATE_LIMITED = "RATE_LIMITED"


class ContextLimits(StructuralModel):
    max_input_tokens: PositiveInt
    max_output_tokens: PositiveInt


class TimeoutPolicy(StructuralModel):
    timeout_ms: PositiveInt


class CostMetadata(StructuralModel):
    currency: Annotated[StrictStr, Field(pattern=r"^[A-Z]{3}$")]
    input_microunits_per_million_tokens: NonNegativeInt
    output_microunits_per_million_tokens: NonNegativeInt


class ModelSpec(StructuralModel):
    provider_id: Identifier
    model_id: Identifier
    version: Version
    capabilities: tuple[Identifier, ...]
    context_limits: ContextLimits
    supports_structured_output: StrictBool
    timeout_policy: TimeoutPolicy
    retry_class: RetryClass
    cost_metadata: CostMetadata
    status: ModelLifecycle = ModelLifecycle.DRAFT

    _provider_id = field_validator("provider_id")(validate_identifier)
    _model_id = field_validator("model_id")(validate_identifier)
    _version = field_validator("version")(validate_semver)

    @field_validator("capabilities")
    @classmethod
    def validate_capabilities(cls, value: tuple[str, ...]) -> tuple[str, ...]:
        if not value:
            raise ValueError("at least one explicit capability is required")
        validated = tuple(validate_identifier(item) for item in value)
        if len(set(validated)) != len(validated):
            raise ValueError("capabilities must be unique")
        return validated


class PromptVariable(StructuralModel):
    name: Identifier
    value_type: Annotated[StrictStr, Field(pattern=r"^(string|integer|number|boolean|object|array)$")]
    required: StrictBool = True

    _name = field_validator("name")(validate_identifier)


class PromptSpec(StructuralModel):
    prompt_id: Identifier
    version: Version
    variables: tuple[PromptVariable, ...]
    output_contract_id: Identifier
    output_contract_version: Version
    checksum: StrictStr
    metadata: FrozenJsonObject = Field(default_factory=FrozenJsonObject)
    status: PromptLifecycle = PromptLifecycle.DRAFT

    _prompt_id = field_validator("prompt_id")(validate_identifier)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _version = field_validator("version", "output_contract_version")(validate_semver)

    @field_validator("variables")
    @classmethod
    def validate_variables(cls, value: tuple[PromptVariable, ...]) -> tuple[PromptVariable, ...]:
        names = [item.name for item in value]
        if len(set(names)) != len(names):
            raise ValueError("variable names must be unique")
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
            raise ValueError(f"must equal the existing Shared Contracts v1 version {SHARED_CONTRACT_V1_VERSION}")
        return value

    @field_validator("checksum")
    @classmethod
    def validate_checksum(cls, value: str) -> str:
        if not CHECKSUM_PATTERN.fullmatch(value):
            raise ValueError("checksum must be a lowercase 64-character SHA-256 digest or sha256:<digest>")
        return value
