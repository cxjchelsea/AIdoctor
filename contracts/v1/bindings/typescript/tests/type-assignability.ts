// tsc --noEmit 用：确认 13 个 schema 的类型可被引用。
import {
  SCHEMA_NAMES,
  type AuditRef,
  type CommitResult,
  type ContractConflict,
  type ContractEnvelope,
  type EvidencePack,
  type IdentifierSet,
  type KnowledgeReleaseRef,
  type PatientDeliveryView,
  type SourceArtifact,
  type StatePatch,
  type ToolContext,
  type ToolResult,
  type TraceRef,
} from "../src/index";
import { CONTRACT_VERSION } from "../src/version";

const _names: typeof SCHEMA_NAMES = SCHEMA_NAMES;
const _version: "1.0.0" = CONTRACT_VERSION;

type AllBindings =
  | ContractEnvelope
  | IdentifierSet
  | StatePatch
  | CommitResult
  | ContractConflict
  | ToolContext
  | ToolResult
  | EvidencePack
  | SourceArtifact
  | KnowledgeReleaseRef
  | TraceRef
  | AuditRef
  | PatientDeliveryView;

export function assertBindingUnion(_value: AllBindings): number {
  return _names.length;
}
