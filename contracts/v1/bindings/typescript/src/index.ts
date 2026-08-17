export {
  CONTRACT_VERSION,
  SCHEMA_FAMILY,
  SUPPORTED_VERSIONS,
  VERSION_NEGOTIATION,
} from "./version";
export {
  SCHEMA_NAMES,
} from "./types";
export type {
  AuditRef,
  CommitResult,
  ContractConflict,
  ContractEnvelope,
  EvidencePack,
  IdentifierSet,
  KnowledgeReleaseRef,
  PatientDeliveryView,
  SchemaName,
  SourceArtifact,
  StatePatch,
  ToolContext,
  ToolResult,
  TraceRef,
} from "./types";

export function requireExactVersion(contractVersion: string): void {
  if (contractVersion !== "1.0.0") {
    throw new Error("unknown contract_version rejected: " + contractVersion);
  }
}
