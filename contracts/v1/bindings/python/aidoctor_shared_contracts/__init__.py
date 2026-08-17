# REVIEWED_BINDING — structural Pydantic v2 types for Shared Contracts v1.
# SCHEMA_SEMANTIC_AUTHORITY remains contracts/v1/manifest.json + schemas.
# 禁止手改本包以迁就遗留 DTO；语义规则以 validator/validate_contracts.py 为准。

from .models import (
    SCHEMA_NAMES,
    AuditRef,
    CommitResult,
    ContractConflict,
    ContractEnvelope,
    EvidencePack,
    IdentifierSet,
    KnowledgeReleaseRef,
    PatientDeliveryView,
    SourceArtifact,
    StatePatch,
    ToolContext,
    ToolResult,
    TraceRef,
    model_for,
)
from .version import CONTRACT_VERSION, SCHEMA_FAMILY, VERSION_NEGOTIATION

__all__ = [
    "CONTRACT_VERSION",
    "SCHEMA_FAMILY",
    "SCHEMA_NAMES",
    "VERSION_NEGOTIATION",
    "AuditRef",
    "CommitResult",
    "ContractConflict",
    "ContractEnvelope",
    "EvidencePack",
    "IdentifierSet",
    "KnowledgeReleaseRef",
    "PatientDeliveryView",
    "SourceArtifact",
    "StatePatch",
    "ToolContext",
    "ToolResult",
    "TraceRef",
    "model_for",
]
