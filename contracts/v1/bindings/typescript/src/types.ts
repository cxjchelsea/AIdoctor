// REVIEWED_BINDING — structural TypeScript types for Shared Contracts v1.
// SCHEMA_SEMANTIC_AUTHORITY remains contracts/v1/manifest.json + schemas.

export type ContractVersionLiteral = "1.0.0";
export type ControlledValue = string | number | boolean | null | Array<string | number | boolean | null>;

export interface ContractEnvelope {
  contract_name: string;
  contract_version: ContractVersionLiteral;
  message_id: string;
  correlation_id: string;
  trace_id: string;
  created_at: string;
  producer: string;
  capability_id: string;
  capability_version: string;
}

export interface IdentifierSet {
  contract_version: ContractVersionLiteral;
  cdp_id?: string;
  patient_id?: string;
  encounter_id?: string;
  session_id?: string;
  tenant_id?: string;
  review_id?: string;
  delivery_id?: string;
}

export interface StatePatchOperation {
  op: "ADD" | "REPLACE" | "REMOVE" | "TEST";
  path: string;
  value?: ControlledValue;
  expected_current_value?: ControlledValue;
  source: "PATIENT_FACT" | "MEDICAL_EVIDENCE" | "CLINICIAN_DECISION" | "SAFETY_RULE" | "TOOL_OUTPUT";
  sensitivity: "PUBLIC" | "INTERNAL" | "INTERNAL_SENSITIVE" | "PHI";
}

export interface StatePatch {
  contract_version: ContractVersionLiteral;
  envelope: ContractEnvelope;
  cdp_id: string;
  base_version: number;
  patch_id: string;
  idempotency_key: string;
  operations: StatePatchOperation[];
  reason_code: string;
  evidence_refs: string[];
  producer: string;
  created_at: string;
}

export interface ContractConflict {
  contract_version: ContractVersionLiteral;
  conflict_id: string;
  type: "VERSION_MISMATCH" | "VALUE_MISMATCH" | "IDEMPOTENCY_MISMATCH" | "PROTECTED_PATH" | "VALIDATION_FAILURE";
  path: string;
  expected_version: number | null;
  actual_version: number | null;
  expected_value: ControlledValue;
  actual_value: ControlledValue;
  resolution: "RETRY_WITH_CURRENT_VERSION" | "REFRESH_AND_REVIEW" | "MANUAL_REVIEW" | "REJECT_PATCH" | "POLICY_REQUIRED";
  retryable: boolean;
  details: string;
}

export interface AuditRef {
  contract_version: ContractVersionLiteral;
  audit_id: string;
  audit_type:
    | "CONTRACT_RECEIVED"
    | "TOOL_INVOKED"
    | "STATE_PATCH_REQUESTED"
    | "STATE_COMMITTED"
    | "PATIENT_DELIVERY_CREATED"
    | "REVIEW_DECISION_RECORDED";
  audit_version: number;
  created_at: string;
  access_level: "INTERNAL" | "RESTRICTED" | "SECURITY_REVIEW_REQUIRED";
  phi_capable: boolean;
}

export interface CommitResult {
  contract_version: ContractVersionLiteral;
  envelope: ContractEnvelope;
  patch_id: string;
  cdp_id: string;
  status: "COMMITTED" | "REJECTED" | "CONFLICT" | "NO_OP" | "FAILED";
  previous_version: number;
  committed_version?: number;
  committed_at?: string;
  reason_code: string;
  conflicts: ContractConflict[];
  rejected_operations: Array<{ operation_index: number; reason_code: string }>;
  errors: Array<{ code: string; message: string }>;
  audit_ref: AuditRef;
  retryable: boolean;
}

export interface ToolContext {
  contract_version: ContractVersionLiteral;
  envelope: ContractEnvelope;
  actor: { actor_id: string; actor_type: "SERVICE" | "AGENT" | "CLINICIAN" | "PATIENT" | "SYSTEM" };
  identifiers: IdentifierSet;
  capability: { capability_id: string; capability_version: string };
  current_state_ref: { cdp_id: string; version: number; read_fields: string[] };
  authorization_scope: {
    granted: Array<"CDP_READ" | "EVIDENCE_READ" | "KNOWLEDGE_READ" | "STATE_PATCH_PROPOSE" | "PATIENT_DELIVERY_READ">;
    requested: Array<"CDP_READ" | "EVIDENCE_READ" | "KNOWLEDGE_READ" | "STATE_PATCH_PROPOSE" | "PATIENT_DELIVERY_READ">;
  };
  deadline: string;
  locale: string;
  requested_operation: string;
  input_refs: Array<{ ref_type: "ARTIFACT" | "EVIDENCE_PACK" | "KNOWLEDGE_RELEASE" | "STATE_VIEW"; ref_id: string; ref_version: number }>;
}

export interface ToolResult {
  contract_version: ContractVersionLiteral;
  envelope: ContractEnvelope;
  tool_name: string;
  tool_version: string;
  invocation_id: string;
  status: "SUCCEEDED" | "NO_RESULT" | "RETRYABLE_FAILURE" | "NON_RETRYABLE_FAILURE" | "TIMED_OUT" | "POLICY_BLOCKED";
  reason_code: string;
  retryable: boolean;
  output: Array<{ name: string; value: ControlledValue }>;
  suggested_patches: StatePatch[];
  evidence_refs: string[];
  errors: Array<{
    code: string;
    category: "VALIDATION" | "DEPENDENCY" | "TIMEOUT" | "AUTHORIZATION" | "CONFLICT" | "POLICY" | "INTERNAL";
    message: string;
    retryable: boolean;
  }>;
  started_at: string;
  completed_at: string;
}

export interface EvidencePack {
  contract_version: ContractVersionLiteral;
  evidence_pack_id: string;
  knowledge_release_id: string | null;
  created_at: string;
  claims: Array<{
    claim_id: string;
    text: string;
    claim_type: "SAFETY_RULE" | "MEDICAL_EVIDENCE" | "CLINICIAN_DECISION" | "PATIENT_FACT";
    support_status: "SUPPORTED" | "PARTIALLY_SUPPORTED" | "CONFLICTING" | "INSUFFICIENT_EVIDENCE" | "NOT_REQUIRED";
    citation_refs: string[];
    applicability: "GENERAL" | "PATIENT_SPECIFIC" | "NOT_APPLICABLE" | "UNKNOWN";
    limitations: string[];
  }>;
  sources: Array<{
    source_id: string;
    source_type: "GUIDELINE" | "KNOWLEDGE_GRAPH" | "RULE" | "REFERENCE_TABLE" | "CLINICIAN_RECORD" | "PATIENT_RECORD" | "RESEARCH";
    title: string;
    publisher: string | null;
    release_or_version: string | null;
    jurisdiction: string | null;
    valid_from: string | null;
    valid_to: string | null;
    locator: string;
    span: string;
    license: string | null;
    retrieved_at: string;
  }>;
  conflicts: Array<{ conflict_id: string; claim_refs: string[]; source_refs: string[]; summary: string }>;
  limitations: string[];
}

export interface SourceArtifact {
  contract_version: ContractVersionLiteral;
  artifact_id: string;
  artifact_type: "UPLOAD" | "EXAMINATION_REPORT" | "OCR_TEXT" | "OCR_STRUCTURED_DATA" | "KNOWLEDGE_SOURCE" | "KNOWLEDGE_DERIVATIVE";
  owner_ref: string;
  content_type: string;
  original_filename: string;
  size_bytes: number;
  checksum: { algorithm: "SHA-256"; value: string };
  storage_ref: string;
  created_at: string;
  processing_status: "RECEIVED" | "PROCESSING" | "PROCESSED" | "FAILED" | "QUARANTINED";
  derived_artifacts: string[];
  source_artifact_id?: string;
  sensitivity: "PUBLIC" | "INTERNAL" | "INTERNAL_SENSITIVE" | "PHI";
  retention_class: string;
}

export interface KnowledgeReleaseRef {
  contract_version: ContractVersionLiteral;
  knowledge_release_id: string;
  source_registry_version: string;
  released_at: string;
  status: "DRAFT" | "ACTIVE" | "SUPERSEDED" | "WITHDRAWN";
  source_refs: string[];
  checksum_manifest_ref: string;
  withdrawn_at?: string;
  superseded_by?: string;
}

export interface TraceRef {
  contract_version: ContractVersionLiteral;
  trace_id: string;
  trace_type: "REQUEST_TRACE" | "TOOL_TRACE" | "WORKFLOW_TRACE" | "MODEL_TRACE";
  trace_version: number;
  created_at: string;
  access_level: "INTERNAL" | "RESTRICTED" | "SECURITY_REVIEW_REQUIRED";
  phi_capable: boolean;
}

export interface PatientDeliveryView {
  contract_version: ContractVersionLiteral;
  delivery_id: string;
  delivery_version: number;
  delivery_status: "PREPARING" | "AWAITING_CLINICIAN" | "READY" | "READY_WITH_LIMITED_EVIDENCE" | "DEGRADED" | "SUPERSEDED" | "CANCELLED";
  cdp_id: string;
  review_status: "NOT_REVIEWED" | "PENDING_CLINICIAN" | "CLINICIAN_APPROVED" | "CLINICIAN_MODIFIED" | "CLINICIAN_REJECTED";
  generated_at: string;
  title: string;
  summary: string;
  safety_notice: Array<{ notice_id: string; level_ref: string; message: string }>;
  recommended_actions: string[];
  evidence_sections: Array<{
    section_id: string;
    title: string;
    description?: string | null;
    display_order: number;
    cards: Array<{
      card_id: string;
      claim_id: string;
      basis_type: "SAFETY_RULE" | "MEDICAL_EVIDENCE" | "CLINICIAN_DECISION" | "PATIENT_FACT";
      patient_friendly_claim: string;
      rationale_summary?: string | null;
      evidence_status: "SUPPORTED" | "PARTIALLY_SUPPORTED" | "CONFLICTING" | "INSUFFICIENT_EVIDENCE" | "POPULATION_MISMATCH" | "STALE_SOURCE" | "OUT_OF_SCOPE" | "NOT_REQUIRED";
      certainty_label?: "STRONGER_BASIS" | "LIMITED_BASIS" | "SOURCES_DIFFER" | "MORE_INFORMATION_NEEDED" | "MAY_NOT_FULLY_APPLY" | "CLINICIAN_REVIEWED" | null;
      related_patient_fact_ids: string[];
      sources: Array<{
        citation_id: string;
        source_id: string;
        title: string;
        organization: string;
        publication_date?: string | null;
        source_version?: string | null;
        source_type: "GUIDELINE" | "SYSTEMATIC_REVIEW" | "GOVERNMENT_HEALTH_INFORMATION" | "PEER_REVIEWED_STUDY" | "OTHER_APPROVED_SOURCE";
        applicable_population?: string | null;
        region?: string | null;
        patient_friendly_excerpt?: string | null;
        access_url?: string | null;
        link_policy: "DIRECT" | "LANDING_PAGE" | "NO_EXTERNAL_LINK";
        freshness_status: "CURRENT" | "REVIEW_DUE" | "STALE" | "UNKNOWN";
      }>;
      conflict_summary?: {
        exists: true;
        patient_friendly_message: string;
        affected_claim_ids: string[];
        requires_clinician_review: boolean;
      } | null;
      applicability?: {
        population_match: "MATCH" | "PARTIAL" | "MISMATCH" | "UNKNOWN";
        region_match: "MATCH" | "PARTIAL" | "MISMATCH" | "UNKNOWN";
        patient_friendly_message?: string | null;
      } | null;
      limitations: Array<{
        code: "MISSING_INFORMATION" | "INSUFFICIENT_EVIDENCE" | "CONFLICTING_EVIDENCE" | "POPULATION_MISMATCH" | "REGION_MISMATCH" | "STALE_SOURCE" | "SOURCE_ACCESS_LIMITED" | "CLINICIAN_REVIEW_REQUIRED" | "CAPABILITY_SCOPE_LIMIT";
        message: string;
        severity: "INFO" | "WARNING" | "CRITICAL";
        user_action?: string;
      }>;
      clinician_reviewed: boolean;
      reviewed_at?: string | null;
      display_priority: "CRITICAL" | "HIGH" | "NORMAL" | "LOW";
      expandable: boolean;
    }>;
  }>;
  limitations: Array<{
    code: "MISSING_INFORMATION" | "INSUFFICIENT_EVIDENCE" | "CONFLICTING_EVIDENCE" | "POPULATION_MISMATCH" | "REGION_MISMATCH" | "STALE_SOURCE" | "SOURCE_ACCESS_LIMITED" | "CLINICIAN_REVIEW_REQUIRED" | "CAPABILITY_SCOPE_LIMIT";
    message: string;
    severity: "INFO" | "WARNING" | "CRITICAL";
    user_action?: string;
  }>;
  follow_up?: string[];
  version_bindings: {
    delivery_version: number;
    contract_version: ContractVersionLiteral;
    cdp_version: number;
    review_decision_version: number | null;
    knowledge_release_id: string | null;
    knowledge_release_version: string | null;
  };
}

export const SCHEMA_NAMES = [
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
] as const;

export type SchemaName = (typeof SCHEMA_NAMES)[number];
