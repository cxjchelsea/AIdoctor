# REVIEWED_BINDING structural Python models for Shared Contracts v1.1.0.
# extra=forbid mirrors additionalProperties:false where modeled.
# Cross-field and path semantics remain owned by contracts/releases/v1/1.1.0/validator/validate_contracts.py.

from __future__ import annotations

from typing import Any, Dict, List, Literal, Optional, Type, Union

from pydantic import BaseModel, ConfigDict

from .version import CONTRACT_VERSION

ControlledValue = Union[str, int, float, bool, None, List[Union[str, int, float, bool, None]]]


class _StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class ContractEnvelope(_StrictModel):
    contract_name: str
    contract_version: Literal["1.1.0"]
    message_id: str
    correlation_id: str
    trace_id: str
    created_at: str
    producer: str
    capability_id: str
    capability_version: str


class IdentifierSet(_StrictModel):
    contract_version: Literal["1.1.0"]
    cdp_id: Optional[str] = None
    patient_id: Optional[str] = None
    encounter_id: Optional[str] = None
    session_id: Optional[str] = None
    tenant_id: Optional[str] = None
    review_id: Optional[str] = None
    delivery_id: Optional[str] = None


class TextObservationValue(_StrictModel):
    kind: Literal["TEXT"]
    value: str

class NumberObservationValue(_StrictModel):
    kind: Literal["NUMBER"]
    value: Union[int, float]

class BooleanObservationValue(_StrictModel):
    kind: Literal["BOOLEAN"]
    value: bool

class CodedObservationValue(_StrictModel):
    kind: Literal["CODED"]
    code: str
    system: str
    display: Optional[str] = None

class QuantityObservationValue(_StrictModel):
    kind: Literal["QUANTITY"]
    value: Union[int, float]
    unit: str

class ReferenceObservationValue(_StrictModel):
    kind: Literal["REFERENCE"]
    reference_type: str
    reference_id: str

ObservationValue = Union[TextObservationValue, NumberObservationValue, BooleanObservationValue, CodedObservationValue, QuantityObservationValue, ReferenceObservationValue]

class ClinicalObservation(_StrictModel):
    contract_version: Literal["1.1.0"]
    observation_id: str
    encounter_id: str
    subject_ref: str
    value: ObservationValue
    recorded_time: str
    effective_time: Optional[str] = None
    status: Literal["COMMITTED"]
    sensitivity: Literal["PUBLIC", "INTERNAL", "INTERNAL_SENSITIVE", "PHI"]
    provenance_refs: List[str]
    evidence_refs: Optional[List[str]] = None

class LegacyStatePatchOperation(_StrictModel):
    op: Literal["ADD", "REPLACE", "REMOVE", "TEST"]
    path: str
    value: Optional[ControlledValue] = None
    expected_current_value: Optional[ControlledValue] = None
    source: Literal["PATIENT_FACT", "MEDICAL_EVIDENCE", "CLINICIAN_DECISION", "SAFETY_RULE", "TOOL_OUTPUT"]
    sensitivity: Literal["PUBLIC", "INTERNAL", "INTERNAL_SENSITIVE", "PHI"]

class CanonicalObservationOperation(_StrictModel):
    op: Literal["ADD", "REPLACE", "REMOVE"]
    path: str
    value: Optional[ClinicalObservation] = None
    source: Literal["PATIENT_FACT", "MEDICAL_EVIDENCE", "CLINICIAN_DECISION", "SAFETY_RULE", "TOOL_OUTPUT"]
    sensitivity: Literal["PUBLIC", "INTERNAL", "INTERNAL_SENSITIVE", "PHI"]

StatePatchOperation = Union[LegacyStatePatchOperation, CanonicalObservationOperation]


class StatePatch(_StrictModel):
    contract_version: Literal["1.1.0"]
    envelope: ContractEnvelope
    cdp_id: Optional[str] = None
    encounter_id: Optional[str] = None
    base_version: int
    patch_id: str
    idempotency_key: str
    operations: List[StatePatchOperation]
    reason_code: str
    evidence_refs: List[str]
    producer: str
    created_at: str


class ContractConflict(_StrictModel):
    contract_version: Literal["1.1.0"]
    conflict_id: str
    type: Literal[
        "VERSION_MISMATCH",
        "VALUE_MISMATCH",
        "IDEMPOTENCY_MISMATCH",
        "PROTECTED_PATH",
        "VALIDATION_FAILURE",
    ]
    path: str
    expected_version: Optional[int]
    actual_version: Optional[int]
    expected_value: ControlledValue
    actual_value: ControlledValue
    resolution: Literal[
        "RETRY_WITH_CURRENT_VERSION",
        "REFRESH_AND_REVIEW",
        "MANUAL_REVIEW",
        "REJECT_PATCH",
        "POLICY_REQUIRED",
    ]
    retryable: bool
    details: str


class AuditRef(_StrictModel):
    contract_version: Literal["1.1.0"]
    audit_id: str
    audit_type: Literal[
        "CONTRACT_RECEIVED",
        "TOOL_INVOKED",
        "STATE_PATCH_REQUESTED",
        "STATE_COMMITTED",
        "PATIENT_DELIVERY_CREATED",
        "REVIEW_DECISION_RECORDED",
    ]
    audit_version: int
    created_at: str
    access_level: Literal["INTERNAL", "RESTRICTED", "SECURITY_REVIEW_REQUIRED"]
    phi_capable: bool


class RejectedOperation(_StrictModel):
    operation_index: int
    reason_code: str


class CommitError(_StrictModel):
    code: str
    message: str


class CommitResult(_StrictModel):
    contract_version: Literal["1.1.0"]
    envelope: ContractEnvelope
    patch_id: str
    cdp_id: Optional[str] = None
    encounter_id: Optional[str] = None
    status: Literal["COMMITTED", "REJECTED", "CONFLICT", "NO_OP", "FAILED"]
    previous_version: int
    committed_version: Optional[int] = None
    committed_at: Optional[str] = None
    reason_code: str
    conflicts: List[ContractConflict]
    rejected_operations: List[RejectedOperation]
    errors: List[CommitError]
    audit_ref: AuditRef
    retryable: bool


class ToolActor(_StrictModel):
    actor_id: str
    actor_type: Literal["SERVICE", "AGENT", "CLINICIAN", "PATIENT", "SYSTEM"]


class ToolCapability(_StrictModel):
    capability_id: str
    capability_version: str


class CurrentStateRef(_StrictModel):
    cdp_id: str
    version: int
    read_fields: List[str]


class AuthorizationScope(_StrictModel):
    granted: List[Literal["CDP_READ", "EVIDENCE_READ", "KNOWLEDGE_READ", "STATE_PATCH_PROPOSE", "PATIENT_DELIVERY_READ"]]
    requested: List[Literal["CDP_READ", "EVIDENCE_READ", "KNOWLEDGE_READ", "STATE_PATCH_PROPOSE", "PATIENT_DELIVERY_READ"]]


class InputRef(_StrictModel):
    ref_type: Literal["ARTIFACT", "EVIDENCE_PACK", "KNOWLEDGE_RELEASE", "STATE_VIEW"]
    ref_id: str
    ref_version: int


class ToolContext(_StrictModel):
    contract_version: Literal["1.1.0"]
    envelope: ContractEnvelope
    actor: ToolActor
    identifiers: IdentifierSet
    capability: ToolCapability
    current_state_ref: CurrentStateRef
    authorization_scope: AuthorizationScope
    deadline: str
    locale: str
    requested_operation: str
    input_refs: List[InputRef]


class NamedValue(_StrictModel):
    name: str
    value: ControlledValue


class ToolError(_StrictModel):
    code: str
    category: Literal["VALIDATION", "DEPENDENCY", "TIMEOUT", "AUTHORIZATION", "CONFLICT", "POLICY", "INTERNAL"]
    message: str
    retryable: bool


class ToolResult(_StrictModel):
    contract_version: Literal["1.1.0"]
    envelope: ContractEnvelope
    tool_name: str
    tool_version: str
    invocation_id: str
    status: Literal[
        "SUCCEEDED",
        "NO_RESULT",
        "RETRYABLE_FAILURE",
        "NON_RETRYABLE_FAILURE",
        "TIMED_OUT",
        "POLICY_BLOCKED",
    ]
    reason_code: str
    retryable: bool
    output: List[NamedValue]
    suggested_patches: List[StatePatch]
    evidence_refs: List[str]
    errors: List[ToolError]
    started_at: str
    completed_at: str


class EvidenceClaim(_StrictModel):
    claim_id: str
    text: str
    claim_type: Literal["SAFETY_RULE", "MEDICAL_EVIDENCE", "CLINICIAN_DECISION", "PATIENT_FACT"]
    support_status: Literal[
        "SUPPORTED",
        "PARTIALLY_SUPPORTED",
        "CONFLICTING",
        "INSUFFICIENT_EVIDENCE",
        "NOT_REQUIRED",
    ]
    citation_refs: List[str]
    applicability: Literal["GENERAL", "PATIENT_SPECIFIC", "NOT_APPLICABLE", "UNKNOWN"]
    limitations: List[str]


class EvidenceSourceSpan(_StrictModel):
    source_id: str
    source_type: Literal[
        "GUIDELINE",
        "KNOWLEDGE_GRAPH",
        "RULE",
        "REFERENCE_TABLE",
        "CLINICIAN_RECORD",
        "PATIENT_RECORD",
        "RESEARCH",
    ]
    title: str
    publisher: Optional[str]
    release_or_version: Optional[str]
    jurisdiction: Optional[str]
    valid_from: Optional[str]
    valid_to: Optional[str]
    locator: str
    span: str
    license: Optional[str]
    retrieved_at: str


class EvidenceConflict(_StrictModel):
    conflict_id: str
    claim_refs: List[str]
    source_refs: List[str]
    summary: str


class EvidencePack(_StrictModel):
    contract_version: Literal["1.1.0"]
    evidence_pack_id: str
    knowledge_release_id: Optional[str]
    created_at: str
    claims: List[EvidenceClaim]
    sources: List[EvidenceSourceSpan]
    conflicts: List[EvidenceConflict]
    limitations: List[str]


class ArtifactChecksum(_StrictModel):
    algorithm: Literal["SHA-256"]
    value: str


class SourceArtifact(_StrictModel):
    contract_version: Literal["1.1.0"]
    artifact_id: str
    artifact_type: Literal[
        "UPLOAD",
        "EXAMINATION_REPORT",
        "OCR_TEXT",
        "OCR_STRUCTURED_DATA",
        "KNOWLEDGE_SOURCE",
        "KNOWLEDGE_DERIVATIVE",
    ]
    owner_ref: str
    content_type: str
    original_filename: str
    size_bytes: int
    checksum: ArtifactChecksum
    storage_ref: str
    created_at: str
    processing_status: Literal["RECEIVED", "PROCESSING", "PROCESSED", "FAILED", "QUARANTINED"]
    derived_artifacts: List[str]
    source_artifact_id: Optional[str] = None
    sensitivity: Literal["PUBLIC", "INTERNAL", "INTERNAL_SENSITIVE", "PHI"]
    retention_class: str


class KnowledgeReleaseRef(_StrictModel):
    contract_version: Literal["1.1.0"]
    knowledge_release_id: str
    source_registry_version: str
    released_at: str
    status: Literal["DRAFT", "ACTIVE", "SUPERSEDED", "WITHDRAWN"]
    source_refs: List[str]
    checksum_manifest_ref: str
    withdrawn_at: Optional[str] = None
    superseded_by: Optional[str] = None


class TraceRef(_StrictModel):
    contract_version: Literal["1.1.0"]
    trace_id: str
    trace_type: Literal["REQUEST_TRACE", "TOOL_TRACE", "WORKFLOW_TRACE", "MODEL_TRACE"]
    trace_version: int
    created_at: str
    access_level: Literal["INTERNAL", "RESTRICTED", "SECURITY_REVIEW_REQUIRED"]
    phi_capable: bool


class SafetyNotice(_StrictModel):
    notice_id: str
    level_ref: str
    message: str


class PatientSourceSummary(_StrictModel):
    citation_id: str
    source_id: str
    title: str
    organization: str
    publication_date: Optional[str] = None
    source_version: Optional[str] = None
    source_type: Literal[
        "GUIDELINE",
        "SYSTEMATIC_REVIEW",
        "GOVERNMENT_HEALTH_INFORMATION",
        "PEER_REVIEWED_STUDY",
        "OTHER_APPROVED_SOURCE",
    ]
    applicable_population: Optional[str] = None
    region: Optional[str] = None
    patient_friendly_excerpt: Optional[str] = None
    access_url: Optional[str] = None
    link_policy: Literal["DIRECT", "LANDING_PAGE", "NO_EXTERNAL_LINK"]
    freshness_status: Literal["CURRENT", "REVIEW_DUE", "STALE", "UNKNOWN"]


class PatientConflictSummary(_StrictModel):
    exists: Literal[True]
    patient_friendly_message: str
    affected_claim_ids: List[str]
    requires_clinician_review: bool


class PatientApplicabilitySummary(_StrictModel):
    population_match: Literal["MATCH", "PARTIAL", "MISMATCH", "UNKNOWN"]
    region_match: Literal["MATCH", "PARTIAL", "MISMATCH", "UNKNOWN"]
    patient_friendly_message: Optional[str] = None


class PatientLimitation(_StrictModel):
    code: Literal[
        "MISSING_INFORMATION",
        "INSUFFICIENT_EVIDENCE",
        "CONFLICTING_EVIDENCE",
        "POPULATION_MISMATCH",
        "REGION_MISMATCH",
        "STALE_SOURCE",
        "SOURCE_ACCESS_LIMITED",
        "CLINICIAN_REVIEW_REQUIRED",
        "CAPABILITY_SCOPE_LIMIT",
    ]
    message: str
    severity: Literal["INFO", "WARNING", "CRITICAL"]
    user_action: Optional[str] = None


class EvidenceCard(_StrictModel):
    card_id: str
    claim_id: str
    basis_type: Literal["SAFETY_RULE", "MEDICAL_EVIDENCE", "CLINICIAN_DECISION", "PATIENT_FACT"]
    patient_friendly_claim: str
    rationale_summary: Optional[str] = None
    evidence_status: Literal[
        "SUPPORTED",
        "PARTIALLY_SUPPORTED",
        "CONFLICTING",
        "INSUFFICIENT_EVIDENCE",
        "POPULATION_MISMATCH",
        "STALE_SOURCE",
        "OUT_OF_SCOPE",
        "NOT_REQUIRED",
    ]
    certainty_label: Optional[
        Literal[
            "STRONGER_BASIS",
            "LIMITED_BASIS",
            "SOURCES_DIFFER",
            "MORE_INFORMATION_NEEDED",
            "MAY_NOT_FULLY_APPLY",
            "CLINICIAN_REVIEWED",
        ]
    ] = None
    related_patient_fact_ids: List[str]
    sources: List[PatientSourceSummary]
    conflict_summary: Optional[PatientConflictSummary] = None
    applicability: Optional[PatientApplicabilitySummary] = None
    limitations: List[PatientLimitation]
    clinician_reviewed: bool
    reviewed_at: Optional[str] = None
    display_priority: Literal["CRITICAL", "HIGH", "NORMAL", "LOW"]
    expandable: bool


class EvidenceSection(_StrictModel):
    section_id: str
    title: str
    description: Optional[str] = None
    display_order: int
    cards: List[EvidenceCard]


class VersionBindings(_StrictModel):
    delivery_version: int
    contract_version: Literal["1.1.0"]
    cdp_version: int
    review_decision_version: Optional[int]
    knowledge_release_id: Optional[str]
    knowledge_release_version: Optional[str]


class PatientDeliveryView(_StrictModel):
    contract_version: Literal["1.1.0"]
    delivery_id: str
    delivery_version: int
    delivery_status: Literal[
        "PREPARING",
        "AWAITING_CLINICIAN",
        "READY",
        "READY_WITH_LIMITED_EVIDENCE",
        "DEGRADED",
        "SUPERSEDED",
        "CANCELLED",
    ]
    cdp_id: str
    review_status: Literal[
        "NOT_REVIEWED",
        "PENDING_CLINICIAN",
        "CLINICIAN_APPROVED",
        "CLINICIAN_MODIFIED",
        "CLINICIAN_REJECTED",
    ]
    generated_at: str
    title: str
    summary: str
    safety_notice: List[SafetyNotice]
    recommended_actions: List[str]
    evidence_sections: List[EvidenceSection]
    limitations: List[PatientLimitation]
    follow_up: Optional[List[str]] = None
    version_bindings: VersionBindings


class Encounter(_StrictModel):
    contract_version: Literal["1.1.0"]
    envelope: ContractEnvelope
    encounter_id: str
    subject_ref: str
    session_ref: Optional[str] = None
    lifecycle_status: Literal["OPEN", "CLOSED"]
    started_at: str
    updated_at: str
    closed_at: Optional[str] = None
    current_state_version: int


class ClinicalStateSnapshot(_StrictModel):
    contract_version: Literal["1.1.0"]
    envelope: ContractEnvelope
    encounter_id: str
    state_version: int
    committed_at: str
    observations: Dict[str, ClinicalObservation]
    audit_ref: Optional[AuditRef] = None
    trace_ref: Optional[TraceRef] = None
    evidence_refs: Optional[List[str]] = None


SCHEMA_NAMES = (
    "ContractEnvelope",
    "IdentifierSet",
    "StatePatch",
    "CommitResult",
    "ContractConflict",
    "ToolContext",
    "ToolResult",
    "EvidencePack",
    "SourceArtifact",
    "KnowledgeReleaseRef",
    "TraceRef",
    "AuditRef",
    "PatientDeliveryView",
    "Encounter",
    "ClinicalStateSnapshot",
    "ClinicalObservation",
)

_MODELS: Dict[str, Type[_StrictModel]] = {
    "ContractEnvelope": ContractEnvelope,
    "IdentifierSet": IdentifierSet,
    "StatePatch": StatePatch,
    "CommitResult": CommitResult,
    "ContractConflict": ContractConflict,
    "ToolContext": ToolContext,
    "ToolResult": ToolResult,
    "EvidencePack": EvidencePack,
    "SourceArtifact": SourceArtifact,
    "KnowledgeReleaseRef": KnowledgeReleaseRef,
    "TraceRef": TraceRef,
    "AuditRef": AuditRef,
    "PatientDeliveryView": PatientDeliveryView,
    "Encounter": Encounter,
    "ClinicalStateSnapshot": ClinicalStateSnapshot,
    "ClinicalObservation": ClinicalObservation,
}


def model_for(schema_name: str) -> Type[_StrictModel]:
    try:
        return _MODELS[schema_name]
    except KeyError as exc:
        raise KeyError("unknown Shared Contract schema: %s" % schema_name) from exc


def dump_binding(instance: _StrictModel) -> Dict[str, Any]:
    """Dump a structural binding instance to JSON-compatible data for validator handoff.

    exclude_unset preserves fixture-style required-null behavior without inventing absent optional fields.
    """
    return instance.model_dump(mode="json", exclude_unset=True)


assert CONTRACT_VERSION == "1.1.0"
assert len(SCHEMA_NAMES) == 16
