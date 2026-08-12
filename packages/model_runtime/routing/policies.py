"""Fail-closed declarative routing policy metadata.

This module does not select models, execute routes, or invoke fallbacks.
"""

from __future__ import annotations

from enum import Enum
from pydantic import StrictBool, field_validator, model_validator

from ..api.models import Identifier, StructuralModel, Version
from ..api.types import validate_identifier, validate_semver


class RouteCategory(str, Enum):
    UNKNOWN = "UNKNOWN"
    NON_CLINICAL = "NON_CLINICAL"
    CLINICAL = "CLINICAL"


class RouteStatus(str, Enum):
    BLOCKED = "BLOCKED"
    DISABLED = "DISABLED"
    ELIGIBLE = "ELIGIBLE"


class RouteMatch(StructuralModel):
    task_type: Identifier
    required_tags: tuple[Identifier, ...] = ()

    _task_type = field_validator("task_type")(validate_identifier)

    @field_validator("required_tags")
    @classmethod
    def validate_tags(cls, value: tuple[str, ...]) -> tuple[str, ...]:
        validated = tuple(validate_identifier(item) for item in value)
        if len(set(validated)) != len(validated):
            raise ValueError("required tags must be unique")
        return validated


class ModelReference(StructuralModel):
    provider_id: Identifier
    model_id: Identifier
    model_version: Version

    _provider_id = field_validator("provider_id")(validate_identifier)
    _model_id = field_validator("model_id")(validate_identifier)
    _model_version = field_validator("model_version")(validate_semver)


class ModelRoutePolicy(StructuralModel):
    route_id: Identifier
    version: Version
    category: RouteCategory = RouteCategory.UNKNOWN
    match: RouteMatch
    required_capabilities: tuple[Identifier, ...]
    primary_model: ModelReference | None = None
    fallback_models: tuple[ModelReference, ...] = ()
    status: RouteStatus = RouteStatus.BLOCKED
    eligible: StrictBool = False

    _route_id = field_validator("route_id")(validate_identifier)
    _version = field_validator("version")(validate_semver)

    @field_validator("required_capabilities")
    @classmethod
    def validate_capabilities(cls, value: tuple[str, ...]) -> tuple[str, ...]:
        if not value:
            raise ValueError("at least one required capability is required")
        validated = tuple(validate_identifier(item) for item in value)
        if len(set(validated)) != len(validated):
            raise ValueError("required capabilities must be unique")
        return validated

    @model_validator(mode="after")
    def validate_fail_closed_topology(self) -> "ModelRoutePolicy":
        refs = ([self.primary_model] if self.primary_model else []) + list(self.fallback_models)
        identities = [(item.provider_id, item.model_id, item.model_version) for item in refs]
        if len(set(identities)) != len(identities):
            raise ValueError("primary and fallback model references must be unique")
        if self.fallback_models and self.primary_model is None:
            raise ValueError("fallback models require a primary model")
        if self.eligible and self.primary_model is None:
            raise ValueError("an eligible route requires a primary model")
        if self.category in {RouteCategory.UNKNOWN, RouteCategory.CLINICAL} and self.status is not RouteStatus.BLOCKED:
            raise ValueError("unknown and clinical routes must remain BLOCKED")
        if self.category in {RouteCategory.UNKNOWN, RouteCategory.CLINICAL} and self.eligible:
            raise ValueError("unknown and clinical routes must remain ineligible")
        if self.category is RouteCategory.NON_CLINICAL and (self.status is RouteStatus.ELIGIBLE) != self.eligible:
            raise ValueError("non-clinical ELIGIBLE status and eligible flag must agree")
        return self
