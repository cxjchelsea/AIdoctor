export { CONTRACT_VERSION, SCHEMA_FAMILY, SUPPORTED_VERSIONS, VERSION_NEGOTIATION } from "./version";
export { SCHEMA_NAMES } from "./types";
export type { AuditRef, ClinicalObservation, ClinicalStateSnapshot, CommitResult, ContractConflict, ContractEnvelope, Encounter, EvidencePack, IdentifierSet, KnowledgeReleaseRef, ObservationValue, PatientDeliveryView, SchemaName, SourceArtifact, StatePatch, StatePatchOperation, ToolContext, ToolResult, TraceRef } from "./types";
export function requireExactVersion(contractVersion: string): void {
  if (contractVersion !== "1.1.0") throw new Error("unknown contract_version rejected: " + contractVersion);
}
