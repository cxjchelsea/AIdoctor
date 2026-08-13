"""Read-only Output Schema Registry entry structures."""

from __future__ import annotations

from typing import Annotated

from pydantic import Field, StrictStr, field_validator, model_validator

from ..api.models import SHARED_CONTRACT_V1_IDS, SHARED_CONTRACT_V1_VERSION, StructuralModel, Version
from ..api.types import validate_identifier, validate_semver


# 逻辑 contract_id 必须与 Shared Contracts v1 清单一致（非 JSON Schema $id URI）
ContractIdentifier = Annotated[StrictStr, Field(min_length=1, max_length=128)]
SchemaRelativePath = Annotated[
    StrictStr,
    Field(pattern=r"^schemas/[a-z0-9]+(?:-[a-z0-9]+)*\.schema\.json$"),
]


class OutputSchemaRegistryEntry(StructuralModel):
    """Exact Shared Contracts v1 identity bound to one fixed schema reference."""

    contract_id: ContractIdentifier
    version: Version
    manifest_name: StrictStr
    schema_path: SchemaRelativePath

    _contract_id = field_validator("contract_id")(validate_identifier)
    _version = field_validator("version")(validate_semver)

    @field_validator("contract_id")
    @classmethod
    def validate_known_shared_contract(cls, value: str) -> str:
        if value not in SHARED_CONTRACT_V1_IDS:
            raise ValueError("must reference an existing Shared Contracts v1 logical identifier")
        return value

    @field_validator("version")
    @classmethod
    def validate_shared_contract_version(cls, value: str) -> str:
        if value != SHARED_CONTRACT_V1_VERSION:
            raise ValueError(f"must equal Shared Contracts v1 version {SHARED_CONTRACT_V1_VERSION}")
        return value

    @field_validator("manifest_name")
    @classmethod
    def validate_manifest_name(cls, value: str) -> str:
        if not value or not value[0].isupper() or not value.replace("_", "").isalnum():
            raise ValueError("manifest_name must be a non-empty PascalCase Shared Contract name")
        return value

    @model_validator(mode="after")
    def validate_path_coherence(self) -> "OutputSchemaRegistryEntry":
        expected_path = f"schemas/{self.contract_id}.schema.json"
        if self.schema_path != expected_path:
            raise ValueError("schema_path must equal schemas/<contract_id>.schema.json")
        return self
