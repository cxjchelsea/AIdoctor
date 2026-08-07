# Legacy Contract Extraction Report (TASK-C01)

## 1. Execution Metadata

```text
execution_id: A65C-C01-20260807-FD246A2
task_id: TASK-C01
gate: Gate-CI0
executed_at_utc: 2026-08-07T01:55:44Z
executor_role: architecture
repository: cxjchelsea/AIdoctor
```

## 2. Exact Enterprise Base

```text
enterprise_branch: agent/enterprise-agent-refactoring-plan
enterprise_head: fd246a21485a58e34a7608fff0e0040495387de9
implementation_branch: agent/phase-a6-5-c-c01-contract-mapping
worktree: D:\project\AIdoctor-a6-5-c-c01-contract-mapping
```

## 3. Gate-CI0 Authorization

```text
Gate-CI0: AUTHORIZED
TASK-C01 implementation: AUTHORIZED
Gate-CI1: NOT_AUTHORIZED
TASK-C03 implementation: NOT_AUTHORIZED
```

## 4. Explicit Non-Authorization

```text
Runtime migration: NOT_AUTHORIZED
Adapter implementation: NOT_AUTHORIZED
contracts/v1 mutation: NOT_AUTHORIZED
TASK-C02: BLOCKED_BY_TASK_B04
TASK-B04: BLOCKED
HUMAN_SUPERVISED_CLINICAL_READ: inactive
A7: NOT_STARTED
Production: BLOCKED
```

## 5. Extraction Method

```text
method: DETERMINISTIC_STATIC_SCAN
languages: Java field/annotation regex scanner + JSON Schema property walk
libraries: Python stdlib (re, json, csv, hashlib, pathlib, subprocess)
network_installs: none
runtime_stores: none
```

## 6. Scanner / Parser Limitations

```text
1. Java scanner uses deterministic regex over private field declarations and nested static class bodies; it does not execute javac or bind generics beyond declared text.
2. Method-local variables and Lombok-generated members are out of scanner scope.
3. Comment-described enum domains are recorded as COMMENT_OR_NAME_DOMAIN_UNVERIFIED and never treated as Schema enums.
4. LocalDateTime timezone offset cannot be proven from field type alone -> TIMESTAMP mappings use UNKNOWN_FAIL_CLOSED where applicable.
5. Inherited superclass fields outside authorized targets are not recursively expanded.
6. CTR-001 is inventory/consumer scope only; clinical DTO field bodies are not extracted.
If a construct cannot be proven: INSUFFICIENT_EVIDENCE or UNKNOWN_FAIL_CLOSED (never EXACT/COMPATIBLE_AS_IS by guess).
```

## 7. C01 Target Asset Inventory

| Asset | Path | Role |
| --- | --- | --- |
| WF-004 | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/entity/CDP.java` | LEGACY_STATE_ENTITY_SOURCE |
| WF-005 | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/entity/CDPVersion.java` | LEGACY_VERSION_STATE_SOURCE |
| WF-006 | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/entity/AgentState.java` | LEGACY_AGENT_STATE_SOURCE |
| WF-007 | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/entity/AuditTrail.java` | LEGACY_AUDIT_SOURCE |
| WF-015 | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/dto/tool/ToolContext.java` | LEGACY_TOOL_INPUT_DTO |
| WF-016 | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/dto/tool/ToolResult.java` | LEGACY_TOOL_RESULT_DTO |
| CTR-001 | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/dto` | DTO_TREE_AND_CONSUMER_SCOPE |
| CTR-009 | `docs/refactoring/evidence/phase-a/a2/python-cdp-access-inventory.csv` | PRIOR_CDP_ACCESS_EVIDENCE |
| BOUND-001 | `contracts/v1` | MAPPING_DESTINATION (READ_ONLY) |

```text
authorized_targets: 9
missing: 0
unknown: 0
```

## 8. Source Git Blob / File Hash Evidence

```text
WF-004: bfdfe43ac328feb4280ba0a77a9024b77f1a8b44
WF-005: 5ab421ac25d7b2044bb0a393a5c873bd8c64aa43
WF-006: ca917adb8db6835d340d16d917cf5622fe9878dd
WF-007: 81a361fd75436ae6e318563aae3fe6accddec961
WF-015: c2b3a25a7506b3d7923ef6e510474a4def03de11
WF-016: 76789a61bb6ce2c7a1bd920093f4db045ca1b855
CTR-009: 19173c297a9bacac2cbde835b772bc5e4ac7ab7f
manifest.json: d8cb373804d9fda3894741dd3964c69c2bb16607
```

### Candidate schema blobs

```text
ContractEnvelope: c2039eb7a5b4c30a0aec1055cd5fb2a07df29d2d
IdentifierSet: 821351057699ecbf78099c6ad28738176e2c3a95
StatePatch: ca0096e08c4be10f7cd1393ae46b1aea3518ed69
CommitResult: afa1658a0f4f6cb4cd3ee04a743818aeece54427
ToolContext: ee77ff00d3c941a96a6497e0d3e401e9ca671999
ToolResult: 2116da3c3a3888102306cbbd14d8c1840096a28b
AuditRef: 4854b2cc2c1965398ce6aa7944afff1b00bfeea6
TraceRef: 8ab3bef870442afd6c0efc4f9cb7b6be115ea6f0
```

## 9. Concrete Java Symbol Inventory

```text
WF-004 symbol: CDP
WF-005 symbol: CDPVersion
WF-006 symbol: AgentState
WF-007 symbol: AuditTrail
WF-015 symbol: ToolContext (+ nested CDPReference, AgentStateSummary, Constraints)
WF-016 symbol: ToolResult (+ nested Evidence, Quality, SuggestedWrite, ErrorInfo)
```

## 10. Per-Source Field Counts

```text
WF-004 declared fields: 18
WF-005 declared fields: 5
WF-006 declared fields: 13
WF-007 declared fields: 9
WF-015 declared fields (incl. nested): 13
WF-016 declared fields (incl. nested): 22
```

## 11. DTO Tree Scope and Consumer Inventory

```text
CTR-001 java_file_count: 38
tool_dto_paths:
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/dto/tool/ToolContext.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/dto/tool/ToolResult.java
consumer_reference_count: 11
consumer_paths:
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/agent/ToolCaller.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/agent/AgentLoop.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/agent/EvidenceFusion.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/WorkupPlannerClient.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/TreatmentEngineClient.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/RiskAssessmentClient.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/ExplanationServiceClient.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/HealthStateAssessmentClient.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/DialogServiceClient.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/DiagnosisEngineClient.java
  - diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/ClinicalParsingClient.java
full_clinical_dto_field_extraction: NOT_AUTHORIZED
C-OD-004: UNRESOLVED / FAIL_CLOSED
```

## 12. CTR-009 Reconciliation

```text
prior_evidence_role: PRIOR_EVIDENCE (not AUTHORITATIVE_RUNTIME_SOURCE)
overlapping_cdp_domains_observed: ['ddx', 'evidence_graph', 'health_state_assessment', 'management_plan', 'patient_state', 'triage', 'wellness_plan', 'workup_plan']
additional_write_domains_in_prior_evidence: ['evidence_chain', 'reasoning_paths', 'conclusion_package']
reconciliation_class: IDENTIFIER_SEMANTICS_MISMATCH
C-OD-005: UNRESOLVED / FAIL_CLOSED
```

## 13. Shared Contract Manifest

```text
contract_version: 1.0.0
version_negotiation: EXACT
supported_versions: ['1.0.0']
schema_family: v1
contracts_v1_modified: no
```

## 14. Shared Contract Schema Inventory

```text
manifest_contract_count: 13
primary_candidates: ContractEnvelope, IdentifierSet, StatePatch, CommitResult, ToolContext, ToolResult, AuditRef, TraceRef
support_only: ContractConflict, EvidencePack
out_of_current_scope: PatientDeliveryView, KnowledgeReleaseRef, SourceArtifact
```

## 15. Contract Version / Negotiation

```text
contract_version: 1.0.0
version_negotiation: EXACT
legacy_entity_version_fields: NOT equal to contract schema version
```

## 16. Field Mapping Summary

```text
total_mapping_rows: 106
unique_mapping_ids: 106
duplicate_mapping_ids: 0
WF-004: 18
WF-005: 5
WF-006: 13
WF-007: 9
WF-015: 23
WF-016: 30
CTR-001: 3
CTR-009: 1
BOUND-001: 4
```

## 17. Mapping-Class Distribution

```text
BLOCKED_PHI_REVIEW: 0
ENUM_TRANSLATION_REQUIRED: 2
EXACT: 1
INSUFFICIENT_EVIDENCE: 6
MERGE_REQUIRED: 0
NOT_APPLICABLE: 5
RENAMED: 15
SOURCE_ONLY: 47
SPLIT_REQUIRED: 0
TARGET_ONLY: 19
TYPE_COERCION_REQUIRED: 7
WRAPPER_REQUIRED: 4
```

## 18. Transform-Class Distribution

```text
ARRAY_NORMALIZATION: 1
ENUM_LOOKUP: 5
IDENTIFIER_REBINDING: 1
MANUAL_ADAPTER_REQUIRED: 23
NONE: 51
NULLABILITY_ADAPTER: 0
NUMERIC_TO_DECIMAL_STRING: 1
OBJECT_WRAPPER: 14
STRING_NORMALIZATION: 1
TIMESTAMP_TO_RFC3339: 7
UNKNOWN_FAIL_CLOSED: 2
```

## 19. Compatibility-Class Distribution

```text
ADAPTER_REQUIRED: 44
COMPATIBLE_AS_IS: 13
CONTRACT_EXTENSION_CANDIDATE: 24
DOCUMENTED_ONLY: 12
MAJOR_VERSION_REQUIRED: 2
UNKNOWN_FAIL_CLOSED: 11
forbidden_values_present: 0
runtime_compatibility_claimed: no
```

## 20. Gap Summary

```text
total_gap_rows: 91
unique_gap_ids: 91
duplicate_gap_ids: 0
OPEN: 91
NOT_APPLICABLE: 0
closed_substantive_gaps: 0
non_compatible_mappings_without_required_gap: 0
```

## 21. Gap-Type Distribution

```text
AUDIT_SEMANTICS_MISMATCH: 5
ENUM_MISMATCH: 2
IDENTIFIER_SEMANTICS_MISMATCH: 4
NO_TARGET_FIELD: 58
NULLABILITY_MISMATCH: 0
REQUIREDNESS_MISMATCH: 0
TRACE_SEMANTICS_MISMATCH: 2
TYPE_MISMATCH: 11
UNKNOWN_FAIL_CLOSED: 6
VERSIONING_MISMATCH: 3
```

## 22. Open Decision Evidence

```text
C-OD-001..C-OD-010: UNRESOLVED / FAIL_CLOSED (unchanged; evidence may support candidates only)
C-OD-011: ACKNOWLEDGED_BLOCKED
decisions_closed_by_implementation: 0
C-RISK-001..012: remain OPEN (risk register not modified)
```

## 23. Safety Boundary

```text
static_phi_capable_source_read: yes
patient_data_content_accessed: no
live_staging_accessed: no
runtime_stores_accessed: no
clinical_policy_body_accessed: no
prompt_bodies_accessed: no
medical_records_accessed: no
external_model_api_called: no
approved_rules: 0
approved_thresholds: 0
approved_hypotheses: 0
approved_medical_sources: 0
```

## 24. Scope Boundary

```text
application_source_modified: no
shared_contracts_modified: no
capability_modified: no
runtime_modified: no
adapters_created: no
runtime_wiring_created: no
C03_evidence_files_created: 0
C02_B04_artifacts_created: 0
overall_c01_c03_report_created: no
```

## 25. Validation Summary

```text
required_validation_checks: 54
validation_rows: 54
missing_required_checks: 0
duplicate_required_check_names: 0
fail_rows: 0
false_pass: 0
invented_contract_names: 0
broken_json_pointers: 0
CI: NO_CI_CONFIGURED
```

## 26. Final Implementation Conclusion

```text
TASK-C01 implementation produced structural field-mapping Evidence only.
field-level mapping != runtime service migration
schema-compatible candidate != runtime-compatible implementation
StatePatch validation != field authorization
COMPATIBLE_AS_IS != PRODUCTION_COMPATIBLE
Adapter required != Adapter authorized
contract_mutation_required evidence != contract_mutation_authorized
AuditRef != TraceRef
status: READY_FOR_A6_5_C_C01_INDEPENDENT_REVIEW
```
