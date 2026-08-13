"""Strict local structures for synthetic prompt resources and rendering."""

from __future__ import annotations

import re
from typing import Annotated

from pydantic import Field, StrictStr, field_validator, model_validator

from ..api.models import PromptSpec, StructuralModel
from ..api.types import FrozenJsonObject, validate_identifier, validate_semver


PLACEHOLDER_NAME_PATTERN = re.compile(r"^[a-z][a-z0-9_]*$")
ResourceIdentifier = Annotated[
    StrictStr,
    Field(pattern=r"^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*/[0-9A-Za-z.+-]+\.yaml$"),
]


class PromptMessage(StructuralModel):
    """One ordered, non-executable message template."""

    role: Annotated[StrictStr, Field(pattern=r"^(system|developer|user|assistant)$")]
    content: Annotated[StrictStr, Field(min_length=1)]


class PromptDocument(StructuralModel):
    """A PromptSpec plus its ordered local message templates."""

    spec: PromptSpec
    messages: Annotated[tuple[PromptMessage, ...], Field(min_length=1)]

    @model_validator(mode="after")
    def validate_template_variable_names(self) -> "PromptDocument":
        invalid = [item.name for item in self.spec.variables if not PLACEHOLDER_NAME_PATTERN.fullmatch(item.name)]
        if invalid:
            raise ValueError("template variable names must use lowercase letters, digits, and underscores")
        return self


class PromptRegistryEntry(StructuralModel):
    """Exact Prompt identity bound to one controlled packaged resource."""

    prompt_id: StrictStr
    version: StrictStr
    resource: ResourceIdentifier

    _prompt_id = field_validator("prompt_id")(validate_identifier)
    _version = field_validator("version")(validate_semver)

    @model_validator(mode="after")
    def validate_resource_identity(self) -> "PromptRegistryEntry":
        expected = f"{self.prompt_id}/{self.version}.yaml"
        if self.resource != expected:
            raise ValueError("resource must exactly match prompt_id/version.yaml")
        return self


class RenderedMessage(StructuralModel):
    role: StrictStr
    content: StrictStr


class RenderedPrompt(StructuralModel):
    """Deterministic local artifact; never a provider or Gateway request."""

    prompt_id: StrictStr
    version: StrictStr
    messages: tuple[RenderedMessage, ...]
    resource_checksum: StrictStr
    manifest: FrozenJsonObject
