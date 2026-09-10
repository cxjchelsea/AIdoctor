"""Strict local structures for synthetic prompt resources and rendering."""

from __future__ import annotations

import re
from typing import Annotated

from pydantic import Field, StrictStr, field_validator, model_validator

from ..api.models import PromptSpec, StructuralModel
from ..api.types import FrozenJsonObject, validate_identifier, validate_semver


PLACEHOLDER_NAME_PATTERN = re.compile(r"^[a-z][a-z0-9_]*$")
# PromptMessage 与 RenderedMessage 共用同一 role 权威约束
MESSAGE_ROLE_PATTERN = r"^(system|developer|user|assistant)$"
RESOURCE_CHECKSUM_PATTERN = r"^[0-9a-f]{64}$"
ResourceIdentifier = Annotated[
    StrictStr,
    Field(pattern=r"^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*/[0-9A-Za-z.+-]+\.yaml$"),
]
MessageRole = Annotated[StrictStr, Field(pattern=MESSAGE_ROLE_PATTERN)]


class PromptMessage(StructuralModel):
    """One ordered, non-executable message template."""

    role: MessageRole
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
    """Builder-owned rendered message; role contract matches PromptMessage."""

    role: MessageRole
    content: StrictStr


class RenderedPrompt(StructuralModel):
    """Deterministic local artifact; never a provider or Gateway request."""

    prompt_id: StrictStr
    version: StrictStr
    messages: Annotated[tuple[RenderedMessage, ...], Field(min_length=1)]
    resource_checksum: Annotated[StrictStr, Field(pattern=RESOURCE_CHECKSUM_PATTERN)]
    manifest: FrozenJsonObject

    _prompt_id = field_validator("prompt_id")(validate_identifier)
    _version = field_validator("version")(validate_semver)

    @model_validator(mode="after")
    def validate_manifest_coherence(self) -> "RenderedPrompt":
        """公开构造也不得绕过 manifest 与顶层字段一致性。"""

        expected = {
            "prompt_id": self.prompt_id,
            "prompt_version": self.version,
            "resource_checksum": self.resource_checksum,
        }
        for field_name, expected_value in expected.items():
            if field_name not in self.manifest or self.manifest[field_name] != expected_value:
                raise ValueError(f"manifest.{field_name} must equal the corresponding top-level field")
        return self
