# PBNC-01 State Committer Mechanical Core Implementation Report

> Dated: 2026-08-21
>
> Classification: `CONTROLLED NON-CLINICAL IMPLEMENTATION` / `SYNTHETIC ONLY` / `NO CLINICAL RUNTIME` / `NO PRODUCTION WIRING`
>
> Batch: `PBNC-01` / `B2_STATE_COMMITTER_MECHANICAL_CORE`
>
> Status: `BOUNDED_SEMANTIC_CORRECTION_IMPLEMENTED_PENDING_RE_REVIEW`
>
> Authorization token:
> `PBNC_01_STATE_COMMITTER_MECHANICAL_CORE_IMPLEMENTATION_AUTHORIZATION_GRANTED`
>
> Remediation authorization token:
> `PBNC_01_POST_COMMIT_ATOMICITY_REMEDIATION_AUTHORIZATION_GRANTED`
>
> Bounded semantic correction authorization token:
> `PBNC_01_PR73_BOUNDED_SEMANTIC_CORRECTION_EXPLICIT_AUTHORIZATION_GRANTED`
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

Corrected deterministic order:

1. bounded StatePatch / envelope validation
2. completed idempotency replay / key inspection
3. capability policy
4. consent
5. field permission
6. source validation
7. current state version
8. atomic idempotency reservation
9. successful actual pre-commit audit emission
10. authoritative mechanical repository commit
11. `CommitResult` assembly
12. idempotency completion
13. internal non-authoritative event evidence

The repository `COMMITTED` outcome is the authoritative mechanical commit
point. Audit is a prerequisite gate. Idempotency completion and internal event
evidence occur after that point and cannot downgrade `COMMITTED`.

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

Structural contract authority remains `contracts/v1`. The core additionally
performs bounded manual consumer validation on the already parsed Java POJO;
successful deserialization is not treated as proof that the boundary is valid.
This is not a claim of full JSON Schema runtime validation.

## 7. Deterministic Status Mapping

- `COMMITTED`: policy + version + atomic commit succeed; version +1;
  empty conflicts / rejected / errors; `retryable = false`.
- `REJECTED`: capability / consent / field / source / identity / operation
  policy failure; empty conflicts / errors; rejected operations present;
  `retryable = false`.
- `CONFLICT`: `VERSION_MISMATCH` or `IDEMPOTENCY_MISMATCH`; conflicts
  present; `retryable = true` per frozen schema.
- `FAILED`: repository or audit infrastructure failure before authoritative
  commit, or invariant failure; errors present; no committed version; no
  partial mutation.
- Once the repository atomic commit succeeds, the result remains `COMMITTED`
  if later idempotency completion or non-authoritative event evidence fails.
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

Canonical fingerprint excludes routing ids such as `patch_id` and envelope
message ids. The key is atomically reserved before repository mutation. If
post-commit result completion fails, the reservation remains and retries cannot
commit again; an exact retry receives `IDEMPOTENCY_RESULT_UNAVAILABLE`, while a
changed fingerprint receives `IDEMPOTENCY_MISMATCH`. Pre-commit rejections and
ordinary conflicts are not permanently memoized.

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

Correction notice: the historical audit-fallback statements below are
superseded by section 21. Current PBNC-01 requires an actual valid pre-commit
`STATE_PATCH_REQUESTED` audit reference and never uses fallback audit evidence
for `COMMITTED`.

Every `CommitResult` has `AuditRef`.

- `COMMITTED` → `audit_type = STATE_COMMITTED`
- `REJECTED` / `CONFLICT` / `FAILED` → `audit_type = STATE_PATCH_REQUESTED`
- all synthetic audit: `phi_capable = false`

`AuditRef` is a reference, not AuditTrail SoR. No real AuditTrail DB write.

Internal event evidence is non-authoritative, not a new `contracts/v1`
event type, and not clinical truth.

Post-commit audit failure uses a schema-valid synthetic
`STATE_COMMITTED` fallback reference. Post-commit idempotency and event sinks
are isolated as best-effort auxiliary operations. Their failure is not
reinterpreted as failure of state already committed by the repository.

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

Published PR HEAD (content-identical to local worktree HEAD tree
`3d1974844e6d0ca7c236bf29cdb920e6e4d1bad4`):

- SHA: `e76b810270a2ebb3a63408dde9d35eb167f2d5f3`
- tree: `3d1974844e6d0ca7c236bf29cdb920e6e4d1bad4`
- message: `PBNC-01 implement State Committer mechanical core.`
- parent: `c716210715c14b713884091252447b48c28eacd4`

Local worktree also contains the same tree as two local commits
`23e3e8c3766a539823870fe4fb4d6f2569907245` and
`b194cd62474d5ad947db2a78c1bd445616f57f04`. Git HTTPS push was
unavailable in this environment; the branch was published through the
GitHub Git Data API with a single content-identical commit.

A later evidence-only commit on the same branch may refresh this section
after Draft PR metadata is known.

## 18. Draft PR

- number: 73
- URL: https://github.com/cxjchelsea/AIdoctor/pull/73
- base: `agent/enterprise-agent-refactoring-plan`
- head: `agent/pbnc-01-state-committer-mechanical-core`
- draft: YES
- head SHA at open: `e76b810270a2ebb3a63408dde9d35eb167f2d5f3`

Must remain Draft. Must not be marked Ready. Must not be merged.

## 19. Final Machine State

```text
PBNC_01_IMPLEMENTATION = POST_COMMIT_ATOMICITY_REMEDIATED_PENDING_INDEPENDENT_REVIEW
PBNC_01_POST_COMMIT_ATOMICITY = REMEDIATED_PENDING_INDEPENDENT_REVIEW
PBNC-01 = POST_COMMIT_ATOMICITY_REMEDIATED_PENDING_INDEPENDENT_REVIEW
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

## 20. Historical POST_COMMIT_FAILURE_CAN_INVERT_AUTHORITATIVE_RESULT Remediation (superseded)

Remediation baseline:

`fd00546fffe0bcf9aaecba31e20397a4e2fe9bad`

Invariant:

```text
ONCE_AUTHORITATIVE_COMMIT_SUCCEEDS
-> COMMIT_RESULT_MUST_NOT_BE_DOWNGRADED_TO_FAILED
```

Mechanical changes only:

- a failed post-commit audit produces a synthetic `STATE_COMMITTED`
  fallback `AuditRef`;
- idempotency remember failure is contained after the authoritative point;
- non-authoritative event evidence failure is contained after the
  authoritative point;
- replay-event evidence failure cannot downgrade the stored original
  authoritative result;
- deterministic fakes inject each failure independently and simultaneously;
- repository failure before the authoritative point still returns `FAILED`
  with no version mutation;
- rejection/conflict/pre-commit audit behavior remains unchanged.

Focused remediation validation:

```text
mvn -f diagnosis-service/pom.xml test -Dtest=StateCommitter*
-> Tests run: 28, Failures: 0, Errors: 0, Skipped: 0

mvn -f diagnosis-service/pom.xml test
-> Tests run: 83, Failures: 0, Errors: 0, Skipped: 0

python contracts/v1/validator/validate_contracts.py
-> A5 CONTRACT VALIDATION PASSED
   13 schemas, 13 valid fixtures, 33 invalid fixtures

python -m pytest -p no:cacheprovider contracts/v1/tests -q
-> 110 passed
```

No production database, Spring/CDPManager wiring, Shared Contracts semantic
change, PBNC-02 state store, Clinical Runtime, PHI, real-patient data, or
clinical content was introduced.

The later bounded semantic correction supersedes the historical audit-fallback
design retained above as chronology.

## 21. PR #73 Bounded Semantic Correction

Correction target:

- base: `743ea5456545c545cf651189ad1a25751ef405ac`
- reviewed head: `5cccab3fc0170519cbcfd4e1beae85c4ac30d3e2`
- reviewed tree: `b944e602448c5fcf48dc0638f5a5553ee2cf3273`

Corrected guarantees:

- `COMMITTED` means mechanical admission plus authoritative version-boundary
  commit; it does not mean ADD / REPLACE / REMOVE were applied to state.
- `TEST` and unsupported operation kinds are rejected before repository commit.
- `AtomicCommitOutcome.committed(previousVersion)` alone constructs a committed
  outcome and computes exactly `previousVersion + 1`.
- only an idempotency reservation winner can attempt mutation; completion
  failure leaves the reservation fail-safe against a duplicate commit.
- an actual, complete Shared Contracts v1 `AuditRef` is required before the
  repository commit attempt; no committed fallback exists.
- output and conflict identifiers are stable request-derived hashes under fixed
  deterministic dependencies and state.
- invariant failures are non-retryable; repository, audit, and idempotency
  infrastructure failures are retryable. Incomplete idempotency results never
  re-commit.

Bounded validator evidence:

```text
STATE_PATCH_BOUNDARY_VALIDATION_TEST_VERIFIED = YES
FULL_JSON_SCHEMA_RUNTIME_VALIDATION = NO
```

The validator maps applicable v1 envelope, identity, version, cardinality,
path, source, sensitivity, reason, evidence, producer, timestamp, and
controlled-value constraints to the parsed binding. Java null cannot retain
the distinction between an absent JSON property and an explicitly supplied
JSON null; that representation limit is not overclaimed.

Focused correction validation:

```text
mvn -f diagnosis-service/pom.xml -Dtest='StateCommitter*' test
-> Tests run: 42, Failures: 0, Errors: 0, Skipped: 0

mvn -f diagnosis-service/pom.xml test
-> Tests run: 97, Failures: 0, Errors: 0, Skipped: 0
```

Evidence labels:

```text
STATE_PATCH_BOUNDARY_VALIDATION_TEST_VERIFIED = YES
IDEMPOTENCY_NO_DOUBLE_COMMIT_TEST_VERIFIED = YES
PRE_COMMIT_AUDIT_GATE_TEST_VERIFIED = YES
MECHANICAL_PATCH_ADMISSION_ATOMICITY_VERIFIED = YES
STATE_COMMITTER_POST_COMMIT_RESULT_STABILITY_TEST_VERIFIED = YES
STATE_OPERATION_APPLICATION_VERIFIED = NO
STATE_LEVEL_ATOMICITY_VERIFIED = NO
TRANSACTIONAL_COMMIT_AND_AUDIT_ATOMICITY = NO
DURABLE_PRODUCTION_AUDIT_VERIFIED = NO
PBNC_01_INDEPENDENTLY_REVIEWED = NO
PBNC_01_MERGE_REVIEWED = NO
PBNC_01_DURABLY_CLOSED = NO
PBNC-02 = NOT_AUTHORIZED
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
```

## 22. PR #73 Second Bounded Semantic Correction

Authorization:

`PBNC_01_PR73_SECOND_BOUNDED_CORRECTION_EXPLICIT_AUTHORIZATION_GRANTED`

Reviewed failure being corrected:

- reviewed head: `486fd9f3f0764169dc595b7e81d246875dfc06e9`
- reviewed tree: `cb61e0ade2c9a08c45540268ebdfce4e7cdbdb7b`
- re-review result: `PBNC_01_PR73_CORRECTION_RE_REVIEW_CORRECTION_REQUIRED`
- stop token: `PBNC_01_PR73_POST_COMMIT_AMBIGUITY_REMAINS`
- SC-05 before this correction: `PARTIALLY_CLOSED`
- CR-05 before this correction: `CONFIRMED_PROBLEM / MAJOR`
- CR-06 before this correction: `CONFIRMED_PROBLEM / MAJOR`

Exact bounded defect:

```text
current = currentVersion(...)
next = current + 1
versions.put(..., next)
return AtomicCommitOutcome.committed(current)
```

At `Integer.MAX_VALUE`, the fake could wrap to `Integer.MIN_VALUE`, mutate its
version map, then throw while constructing the committed outcome. That made a
repository exception with prior mutation representable in PBNC-01 evidence
infrastructure.

Bounded fix:

```text
AtomicCommitOutcome outcome = AtomicCommitOutcome.committed(current)
versions.put(..., outcome.committedVersion)
return outcome
```

The committed transition is now constructed and proven representable before the
mechanical fake mutates its version map. At `Integer.MAX_VALUE`, construction
fails before mutation. At `Integer.MAX_VALUE - 1`, the fake commits exactly
once to `Integer.MAX_VALUE`.

New focused tests:

- `nearIntegerMaxVersionCommitsToIntegerMaxExactlyOnce`
- `integerMaxVersionFailureDoesNotMutateBeforeFailedResult`
- `retryAfterIntegerMaxOverflowSeesNoHiddenMutationOrCompletedResult`
- `committedOutcomeFactoryRefusesIntegerMaxOverflowTransition`

Evidence labels after authoring:

```text
STATE_CHANGED_BUT_FAILED_OVERFLOW_PATH_REMOVED = YES
INTEGER_MAX_VERSION_WRAPAROUND_PREVENTED = YES
REPOSITORY_EXCEPTION_NO_MUTATION_TEST_VERIFIED = YES
SC_05_SECOND_CORRECTION_IMPLEMENTED = YES
STATE_COMMITTER_MECHANICAL_CORE_VERIFIED = NO
PBNC_01_DURABLY_CLOSED = NO
PBNC-02 = NOT_AUTHORIZED
B2 = NOT_COMPLETE
PHASE_B = NOT_AUTHORIZED
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
```

Current authoring status:

```text
PBNC-01 = SECOND_CORRECTION_IMPLEMENTED_PENDING_RE_REVIEW
```
