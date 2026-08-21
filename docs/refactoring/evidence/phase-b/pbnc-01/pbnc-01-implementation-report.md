# PBNC-01 State Committer Mechanical Core Implementation Report

> Dated: 2026-08-21
>
> Classification: `CONTROLLED NON-CLINICAL IMPLEMENTATION` / `SYNTHETIC ONLY` / `NO CLINICAL RUNTIME` / `NO PRODUCTION WIRING`
>
> Batch: `PBNC-01` / `B2_STATE_COMMITTER_MECHANICAL_CORE`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `PBNC_01_STATE_COMMITTER_MECHANICAL_CORE_IMPLEMENTATION_AUTHORIZATION_GRANTED`
>
> Authorized Enterprise baseline:
> `c716210715c14b713884091252447b48c28eacd4` /
> `cb26aef038e51677d6b33b75b628fd6c55da5469`

```text
PBNC-01 IMPLEMENTED
!=
B2 COMPLETE
!=
PHASE B
!=
PRODUCTION READY
```

## 1. Pre-flight

- Repository: `cxjchelsea/AIdoctor`
- Enterprise branch: `agent/enterprise-agent-refactoring-plan`
- GitHub LIVE HEAD: `c716210715c14b713884091252447b48c28eacd4`
- GitHub LIVE tree: `cb26aef038e51677d6b33b75b628fd6c55da5469`
- Local worktree: `D:/project/AIdoctor-pbnc-01`
- Implementation branch: `agent/pbnc-01-state-committer-mechanical-core`
- Created from exact Enterprise baseline: YES
- Drift at start: NONE

Governance consumed, not rewritten:

```text
PR72_POST_MERGE_VERIFICATION = PASS
PHASE_B_NC_STATE_FOUNDATION_ROADMAP_AMENDMENT = DURABLY_CLOSED
PHASE_B_NC_STATE_FOUNDATION_GOVERNANCE = DURABLE
READY_FOR_PBNC_01_IMPLEMENTATION_AUTHORIZATION = YES
PHASE_B = NOT_AUTHORIZED
PBNC-02 = NOT_AUTHORIZED
A7 = NOT_COMPLETE
A7-CL-02 = NOT_AUTHORIZED
FB-11 = UNSATISFIED / PRESERVED
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
```

## 2. Authorization

Token presented and consumed:

`PBNC_01_STATE_COMMITTER_MECHANICAL_CORE_IMPLEMENTATION_AUTHORIZATION_GRANTED`

```text
PBNC-01 AUTHORIZATION
!=
PHASE B AUTHORIZATION
```

This batch implements only the Java-side mechanical core. It does not
authorize PBNC-02, full B1, B3–B7, Clinical Runtime, or Production.

## 3. Implementation Placement

Package:

`com.aidoctor.diagnosis.state.committer`

Placement reason:

- Transitional CDP / SoR path is Java-owned.
- `diagnosis-service` already consumes Shared Contracts v1 Java binding.
- State Committer belongs to Clinical State & Data Foundation, not Agent Runtime.
- Python Runtime / LangGraph is not SoR and was not used.

No production service was created. Java / Python ownership is unchanged.

## 4. Files Changed

NEW only. `diagnosis-service/pom.xml` was not modified.
`contracts/v1/**` was not modified.

Main:

- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/package-info.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/StateCommitter.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/CommitReasonCodes.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/CanonicalPatchFingerprint.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/CommitResultAssembler.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/InternalCommitEventEvidence.java`
- eight internal ports under `.../state/committer/ports/`

Test:

- mechanical / contract-boundary / architecture tests
- deterministic fakes
- synthetic factory and schema assertions

Evidence:

- this report

## 5. State Committer Architecture

Public method:

```text
StateCommitter.commit(StatePatch) -> CommitResult
```

Constructor-injected internal ports only:

- `StateRepositoryPort`
- `CapabilityPolicyPort`
- `FieldPermissionPort`
- `ConsentPolicyPort`
- `SourceValidationPort`
- `IdempotencyPort`
- `AuditPort`
- `CommitEventEvidencePort`

These ports are PBNC-01 implementation detail. They are not Shared Contracts.

`StateRepositoryPort` only reads a current version and executes one atomic
commit attempt. The test fake is a version counter, not a PBNC-02 store.

Deterministic order:

1. contract / version identity
2. idempotency lookup
3. capability policy
4. consent
5. field permission
6. source validation
7. current state version
8. atomic commit
9. audit
10. internal non-authoritative event evidence
11. `CommitResult`

No LLM, Model Runtime, RAG, Tool, or Clinical Safety calls.

No Spring stereotype, controller, Feign, JPA, HTTP endpoint, or
`CDPManager` wiring.

## 6. Shared Contracts Consumption

Reused existing v1 Java binding types only:

- `StateTypes.StatePatch`
- `StateTypes.CommitResult`
- `FoundationTypes.ContractConflict`
- `FoundationTypes.ContractEnvelope`
- `FoundationTypes.AuditRef`
- `ExactVersion`
- `ContractVersion`
- `SharedContractsMapper`

Not created:

- `StatePatchV2`
- `InternalStatePatch`
- `ExpectedVersionPatch`
- `ClinicalStatePatch`
- `StatePatch.expected_version`

Concurrency remains:

```text
StatePatch.base_version = caller optimistic-concurrency precondition
internal current_version = repository-side current version
mismatch -> CONFLICT
successful commit -> previous_version + 1
```

Structural validation authority remains `contracts/v1` validator.
The core receives already parsed / contract-valid `StatePatch`.

## 7. Deterministic Status Mapping

- `COMMITTED`: policy + version + atomic commit succeed; version +1;
  empty conflicts / rejected / errors; `retryable = false`.
- `REJECTED`: capability / consent / field / source / identity / operation
  policy failure; empty conflicts / errors; rejected operations present;
  `retryable = false`.
- `CONFLICT`: `VERSION_MISMATCH` or `IDEMPOTENCY_MISMATCH`; conflicts
  present; `retryable = true` per frozen schema.
- `FAILED`: repository or audit infrastructure failure, or invariant
  failure; errors present; no committed version; no partial mutation.
- Idempotent replay: same key + same logical patch returns the original
  `CommitResult`. It is not rewritten as `NO_OP`.

## 8. Consent Semantics

Frozen MINOR #1:

```text
missing / denied / invalid synthetic consent
→ REJECTED
reason_code = CONSENT_NOT_AUTHORIZED
retryable = false
conflicts = []
errors = []
rejected_operations covers every operation
```

Consent failure is a policy rejection, not `FAILED`.
Lookup is synthetic only.

## 9. Idempotency Semantics

Frozen MINOR #2:

```text
same idempotency_key + same logical StatePatch
→ original CommitResult
→ no second commit
→ no second version increment

same idempotency_key + different logical content
→ CONFLICT / IDEMPOTENCY_MISMATCH
→ no commit
```

Canonical fingerprint excludes routing ids such as `patch_id` and
envelope message ids. Replay may emit non-authoritative replay evidence
only.

## 10. Version Semantics

```text
StatePatch.base_version != internal current_version
→ CONFLICT
ContractConflict.type = VERSION_MISMATCH
expected_version = StatePatch.base_version
actual_version = internal current_version
```

`ContractConflict.expected_version` is the existing conflict evidence
field. `StatePatch.expected_version` was not added.

## 11. Audit / Event Evidence

Every `CommitResult` has `AuditRef`.

- `COMMITTED` → `audit_type = STATE_COMMITTED`
- `REJECTED` / `CONFLICT` / `FAILED` → `audit_type = STATE_PATCH_REQUESTED`
- all synthetic audit: `phi_capable = false`

`AuditRef` is a reference, not AuditTrail SoR. No real AuditTrail DB write.

Internal event evidence is non-authoritative, not a new `contracts/v1`
event type, and not clinical truth.

## 12. Positive Tests

`StateCommitterMechanicalCoreTest` + contract/architecture suites prove:

1. valid synthetic patch → `COMMITTED`
2. successful commit → version exactly +1
3. same key + same patch replay → original result, no second commit
4. same key + different patch → `CONFLICT` / `IDEMPOTENCY_MISMATCH`
5. stale `base_version` → `CONFLICT` / `VERSION_MISMATCH`
6. unauthorized field → `REJECTED` before mutation
7. invalid capability → `REJECTED` before mutation
8. missing consent → `REJECTED`
9. denied consent → `REJECTED`
10. invalid synthetic consent → `REJECTED`
11. invalid source → `REJECTED` before mutation
12. multi-operation validation failure → repository commit not called
13. repository atomic failure → version unchanged
14–17. `COMMITTED` / `REJECTED` / `CONFLICT` / `FAILED` all carry valid `AuditRef`
18. `CommitResult` schema constraints remain satisfied
19. Tool / LLM / Agent have no State Committer production wiring
20. State Committer has no Spring component / runtime wiring

## 13. Negative / Architecture Proofs

Automated by `StateCommitterArchitectureGuardTest`:

- no `@Component` `@Service` `@Configuration` `@RestController`
  `@Controller` `@Bean` `@ConditionalOnProperty` `@FeignClient`
- no `CDPManager.update*`, `EntityManager`, `Repository.save`,
  HTTP provider, or Model Gateway references in the core
- no production wiring from `AgentLoop`, `ClinicalAgentBrain`,
  `ToolCaller`, `CDPManager`, `DiagnosisController`, or
  `DiagnosisOrchestrationService`
- no Shared Contract fork types
- no Encounter / EncounterCDP / ClinicalObservation implementation

## 14. Zero Counts

```text
CLINICAL_CONTENT_COUNT = 0
PHI_COUNT = 0
REAL_PATIENT_COUNT = 0
REAL_PROVIDER_CALLS = 0
Clinical Runtime activation = 0
Safety implementation = 0
production DB write = 0
production API wiring = 0
Shared Contracts semantic diff = 0
Capability activation = 0
legacy CDP dual write = 0
```

Synthetic fixtures only, for example:

- `cdp_id = synthetic-cdp-001`
- `capability_id = synthetic-capability-v1`
- `path = /patient_state/synthetic_test_value`
- `value = alpha`
- `source = TOOL_OUTPUT`
- `sensitivity = PUBLIC`

## 15. Validation

Local, from worktree `D:/project/AIdoctor-pbnc-01`:

```text
python contracts/v1/validator/validate_contracts.py
→ A5 CONTRACT VALIDATION PASSED
  13 schemas, 13 valid fixtures, 33 invalid fixtures

python -m pytest -p no:cacheprovider contracts/v1/tests -q
→ 110 passed

mvn -f contracts/v1/bindings/java/pom.xml install -DskipTests
→ BUILD SUCCESS

mvn -f diagnosis-service/pom.xml test -Dtest=StateCommitter*
→ Tests run: 23, Failures: 0, Errors: 0, Skipped: 0

mvn -f diagnosis-service/pom.xml test
→ Tests run: 78, Failures: 0, Errors: 0, Skipped: 0
  including 23 PBNC-01 tests
```

Phase A CI MVP runs on pull_request to Enterprise. Exact Head CI is
recorded after the Draft PR opens.

```text
unit test PASS
!=
B2 COMPLETE
!=
Phase B COMPLETE
!=
Production Ready
```

## 16. Explicit Non-Implementation

Not implemented and not authorized:

- PBNC-02 synthetic versioned state store / snapshot / restart / concurrency
- B1 Encounter / EncounterCDP / ClinicalObservation
- B3 Legacy CDP Adapter
- B4 Safety
- B5 Clinical Parsing
- B6 Business API v2
- B7 Exit
- Clinical Runtime
- Production wiring / cutover / dual write
- Shared Contracts semantic change
- clinical content / PHI / real patient / MIMIC / eICU / real provider

```text
PBNC-01 test fake
!=
PBNC-02 synthetic state store
```

## 17. Commit

- SHA: `23e3e8c3766a539823870fe4fb4d6f2569907245`
- tree: `34c23a5f1339bc8e960b8471b93efe132516bd3a`
- message: `PBNC-01 implement State Committer mechanical core.`
- A later evidence-only commit may add Draft PR metadata to this file.

## 18. Draft PR

Recorded after `gh pr create --draft`.

Must remain Draft. Must not be marked Ready. Must not be merged.

## 19. Final Machine State

```text
PBNC_01_IMPLEMENTATION = IMPLEMENTED_PENDING_INDEPENDENT_REVIEW
PBNC-01 = IMPLEMENTED_PENDING_INDEPENDENT_REVIEW
PBNC-02 = NOT_AUTHORIZED
B2 = NOT_COMPLETE
PHASE_B = NOT_AUTHORIZED
STATE_COMMITTER_PRODUCTION_WIRED = NO
STATE_COMMITTER_AUTHORITATIVE_CUTOVER = NO
CLINICAL_CONTENT_COUNT = 0
PHI_COUNT = 0
REAL_PATIENT_COUNT = 0
REAL_PROVIDER_CALLS = 0
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
READY_FOR_PBNC_01_INDEPENDENT_REVIEW = YES
```
