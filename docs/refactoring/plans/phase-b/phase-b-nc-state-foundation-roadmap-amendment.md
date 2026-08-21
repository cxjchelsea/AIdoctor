# Phase B-NC State Foundation Roadmap Amendment

> Title: Phase B-NC State Foundation Roadmap Amendment
>
> Classification: `IMPLEMENTATION_ORDER_AMENDMENT` / `DOCS / GOVERNANCE ONLY`
>
> Lane ID: `PHASE_B_NC_STATE_FOUNDATION`
>
> Authoring status: `AUTHORED_PENDING_INDEPENDENT_REVIEW`
>
> Lane status at authoring time: `PROPOSED_NOT_AUTHORIZED`
>
> Architecture Change: `NO`
>
> Architecture Refreeze: `NOT_REQUIRED`
>
> Clinical Runtime: `NOT_ENABLED`
>
> Production: `BLOCKED`
>
> Real Provider: `FORBIDDEN`
>
> Phase B: `NOT_AUTHORIZED`
>
> PBNC-01: `NOT_AUTHORIZED`
>
> PBNC-02: `NOT_AUTHORIZED`
>
> Authorization token:
> `PHASE_B_NC_STATE_FOUNDATION_ROADMAP_AMENDMENT_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Enterprise baseline at authoring:
> `c19c5363c8a2c6eed4a7049f2a7ae8f298ac85a6` /
> `cd2dbfe49dd7208bf143b2b0e130bca4d9c324a9`
>
> Companion scope register:
> [phase-b-nc-state-foundation-scope-register.csv](./phase-b-nc-state-foundation-scope-register.csv)

```text
ROADMAP AMENDMENT
!=
IMPLEMENTATION AUTHORIZATION

PHASE_B_NC COMPLETE
!=
PHASE_B COMPLETE

PBNC-01 COMPLETE
!=
B2 COMPLETE

PBNC-02 COMPLETE
!=
B1 COMPLETE

PHASE_B_NC EXIT
!=
B7 EXIT
```

---

## 1. Authorization basis

This amendment is created under explicit Repository Owner authorization for
docs/governance authoring only.

It may define a future non-clinical Phase B engineering continuation path.

It does **not** authorize:

- Phase B implementation
- State Committer implementation
- Clinical Runtime
- clinical content authoring
- real patient data / PHI
- real provider
- Production
- A7-CL-02
- B4 Safety implementation
- Shared Contracts v1 semantic change

```text
PHASE_B_NC_STATE_FOUNDATION = PROPOSED_NOT_AUTHORIZED
PBNC-01 = NOT_AUTHORIZED
PBNC-02 = NOT_AUTHORIZED
STATE_COMMITTER_IMPLEMENTED = NO
PHASE_B = NOT_AUTHORIZED
PHASE_B_COMPLETE = NO
```

---

## 2. Current-effective governance consumed here

This amendment consumes, and does not rewrite, the following current-effective
truth at authoring-time Enterprise `c19c536`:

```text
PHASE_A_ENGINEERING_BASELINE = FROZEN
A7-NC = COMPLETE / CLOSED / STABLE
PHASE_A_NC_CLOSURE = COMPLETE
A7 = NOT_COMPLETE
A7-CL = DEFERRED_OUT_OF_CURRENT_BASELINE
A7-CL-01 = GOVERNANCE_AMENDMENT_MERGED_AND_VERIFIED
A7-CL-02 = NOT_AUTHORIZED
FB-11 = UNSATISFIED / PRESERVED
A11 = NOT_PASSED
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
Phase B = NOT_AUTHORIZED
POSTFREEZE-04 = DURABLY_CLOSED
JAVA_PYTHON_RAW_OCR_CONTROLLED_CUTOVER_VERIFIED = YES
JAVA_PYTHON_CUTOVER_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
```

No POSTFREEZE-06 is invented.

---

## 3. Why Phase B-NC exists

The historical Phase B sequence remains:

```text
B1 Clinical State
→ B2 State Committer
→ B3 Legacy CDP Adapter
→ B4 adult_respiratory_v1 Safety
→ B5 Clinical Parsing Adapter
→ B6 Business API v2
→ B7 Exit Gate
```

That sequence is not deleted. Current resources and gates prevent starting
the full Phase B:

1. Clinical expert resources remain unassigned.
2. The A7-CL clinical-content lane remains deferred.
3. B4 Safety requires real clinical rules and must not enter this engineering lane.
4. B1 / B3 / B5 / B6 depend on canonical clinical contracts that are not
   authorized for semantic addition in this lane.
5. Shared Contracts v1 already contains enough mechanical types for a
   bounded State Committer core:
   `StatePatch`, `CommitResult`, `ContractEnvelope`, `AuditRef`.
6. Frozen ADR already states that State Committer is the unique future
   authoritative clinical-state writer.
7. Therefore the B2 mechanical core is the smallest Phase B subset that can
   later be implemented without waiving clinical, runtime, or Production gates.

```text
PHASE_B_NC_STATE_FOUNDATION
=
IMPLEMENTATION_ORDER_AMENDMENT
```

It is **not**:

- an architecture refreeze
- a clinical gate waiver
- Phase B authorization
- Phase B completion
- A7 completion

---

## 4. Core evidence question

```text
Can the repository implement and mechanically verify the frozen unique
State Committer boundary using existing Shared Contracts v1
StatePatch / CommitResult semantics and synthetic non-clinical state only,
while preserving all clinical, runtime and production gates?
```

A future independently authorized implementation may answer that question.
This amendment only records the path.

---

## 5. Lane identity

```text
Formal name: PHASE_B_NC_STATE_FOUNDATION
Current status: PROPOSED_NOT_AUTHORIZED
Initial future batches:
  PBNC-01  B2_STATE_COMMITTER_MECHANICAL_CORE
  PBNC-02  SYNTHETIC_VERSIONED_STATE_INTEGRATION
```

Each future batch requires its own explicit implementation authorization.

---

## 6. Initial allowed future implementation scope

### 6.1 PBNC-01 State Committer Mechanical Core

After a later explicit authorization, PBNC-01 may implement only:

- `StatePatch` input boundary
- `CommitResult` output boundary
- schema validation
- capability / policy lookup abstraction
- field-permission validation
- source validation
- consent fail-closed validation
- idempotency
- expected / base version validation
- conflict detection
- atomic operation semantics
- audit emission
- non-authoritative internal event evidence
- deterministic error mapping
- architecture guard against bypass / direct write

### 6.2 PBNC-02 Synthetic Versioned State Integration

After a later explicit authorization, PBNC-02 may implement only:

- synthetic / in-memory versioned state store
- `base_version`
- `expected_version`
- snapshot
- idempotency replay
- concurrent conflict
- atomic commit
- deterministic rollback / no partial success
- synthetic policy fixtures
- synthetic consent fixtures
- synthetic source fixtures

Restart / durable persistence semantics are **out of PBNC-02** unless a
later separate authorization adds them.

---

## 7. Explicit exclusions from the initial lane

```text
B1_FULL = BLOCKED_PENDING_A5_CONTRACT_IMPACT_REVIEW
B3      = BLOCKED_PENDING_A5_CONTRACT_IMPACT_REVIEW
B4      = EXCLUDED
B5      = BLOCKED_PENDING_A5_CONTRACT_IMPACT_REVIEW
B6      = BLOCKED_PENDING_A5_CONTRACT_IMPACT_REVIEW
B7      = EXCLUDED
```

The initial lane must not implement or author:

- Encounter canonical implementation
- EncounterCDP canonical implementation
- ClinicalObservation canonical implementation
- ObservationCandidate canonical implementation
- AgentEvent canonical contract addition
- clinical Safety rules
- red flags
- triage thresholds
- emergency rules
- respiratory clinical logic
- diagnosis
- treatment
- clinical Prompt
- Question / Hypothesis authoring
- Clinical Runtime wiring
- real provider / provider API
- production DB wiring
- real patient data / PHI
- real clinical traffic
- dual write
- production API exposure

---

## 8. Shared Contracts boundary

PBNC-01 / PBNC-02 initial design must reuse existing v1 types:

```text
StatePatch
CommitResult
ContractEnvelope
AuditRef
```

This amendment does **not** modify `contracts/v1/**` and does **not** add
or change contract semantics.

If a later implementation discovers that it must add or change:

- Encounter
- EncounterCDP
- ClinicalObservation
- ObservationCandidate
- AgentEvent
- StatePatch semantics
- CommitResult semantics
- ContractEnvelope semantics

it must:

```text
STOP
A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW
```

PBNC implementation must not silently mutate contracts.

---

## 9. Frozen architecture boundary

`PHASE_B_NC_STATE_FOUNDATION` does not change these frozen principles:

1. State Committer is the unique authoritative clinical-state writer.
2. Tool / LLM / Agent do not directly commit clinical truth.
3. `StatePatch` is a proposal, not committed fact.
4. `CommitResult` is the authoritative commit outcome.
5. Checkpoint is not clinical truth.
6. Trace is not clinical truth.
7. Model output is not clinical truth.
8. Mandatory Safety cannot be bypassed.
9. Java / Python ownership boundary is unchanged.
10. Capability lifecycle is unchanged.
11. Production authority model is unchanged.

If a later implementation requires:

- a second authoritative writer
- checkpoint as system of record
- LLM / Tool direct write
- a Safety-ownership change
- a Java / Python ownership change
- a clinical-truth ownership change

it must:

```text
STOP
ARCHITECTURE_REVIEW_REFREEZE_REQUIRED
```

---

## 10. Synthetic-only data rule

Future PBNC-01 / PBNC-02 implementation must keep:

```text
REAL_PATIENT_COUNT = 0
PHI_COUNT = 0
CLINICAL_CONTENT_COUNT = 0
REAL_PROVIDER_CALLS = 0
```

Allowed data class:

```text
synthetic
non-medical
deterministic
fake
in-memory / local-test-only
```

Allowed examples:

```text
test.subject.001
test.value
synthetic-capability-v1
synthetic-source
synthetic-consent
```

Forbidden even as “example” clinical content:

```text
cough
dyspnea
pneumonia
SpO2 clinical threshold
emergency
red flag
diagnosis
treatment
medication
```

Public de-identified medical corpora, including MIMIC / eICU, are **not**
in PBNC-01 / PBNC-02. They belong to a later separately authorized data
adaptation / validation stage.

---

## 11. PBNC-01 future exit evidence

PBNC-01 is not authorized here. When later authorized, Exit requires the
following **positive proofs**:

1. valid `StatePatch` → `COMMITTED`
2. version increments exactly once
3. duplicate idempotency key does not double-commit
4. stale `base_version` → `CONFLICT`
5. unauthorized field → `REJECTED`
6. invalid capability / policy → `REJECTED`
7. missing / failed consent → fail closed
8. invalid source → `REJECTED`
9. unsupported operation → `REJECTED`
10. multi-operation patch is atomic
11. one failed operation causes no partial state mutation
12. audit evidence is emitted
13. deterministic `CommitResult` is produced
14. no Tool / LLM direct-write route exists

**Negative proofs**:

- no Clinical Runtime activation
- no provider call
- no PHI
- no real patient
- no clinical content
- no Safety implementation
- no production DB write
- no production API wiring
- no Shared Contracts semantic diff
- no Capability activation
- no direct legacy CDP dual write

```text
PBNC-01 COMPLETE != B2 COMPLETE
```

---

## 12. PBNC-02 future exit evidence

PBNC-02 is not authorized here. When later authorized, Exit requires at
least:

- initial version is deterministic
- valid commit advances version
- stale expected version conflicts
- concurrent write produces a deterministic winner / conflict
- replay with the same idempotency key is safe
- snapshot remains internally consistent
- failed transaction leaves the original state unchanged
- restart / persistence semantics remain out of scope unless separately authorized
- synthetic state never becomes clinical truth

```text
PBNC-02 COMPLETE != B1 COMPLETE
```

---

## 13. STOP / expansion gate

After PBNC-02 completes under a later authorization:

```text
MUST STOP
PHASE_B_NC_EXPANSION_CONTRACT_IMPACT_ASSESSMENT_REQUIRED
```

That later assessment decides:

- whether B1 needs new canonical contracts
- B3 Legacy Adapter target schema
- B5 ObservationCandidate contract
- B6 Encounter API contract

If `contracts/v1/**` must change:

```text
A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW
```

The lane must not automatically continue into B1 / B3 / B5 / B6.

---

## 14. Relationship to Phase B

```text
PHASE_B_NC COMPLETE != PHASE_B COMPLETE
PHASE_B_NC EXIT != B7 EXIT
```

Even if future PBNC-01 and PBNC-02 both complete, Phase B may remain:

```text
NOT_AUTHORIZED
```

or later be recorded as:

```text
PARTIALLY_IMPLEMENTED_NOT_CLINICALLY_ENABLED
```

The exact Phase B control label after PBNC Exit must be set by a future
independent governance batch.

This amendment must not write:

```text
PHASE_B = COMPLETE
B7 = PASS
```

---

## 15. Clinical track remains unchanged

```text
RG-06 PRE_CONTENT_GATE = PRESERVED
RG-07 PRE_CONTENT_GATE = PRESERVED
RG-08 PRE_CONTENT_GATE = PRESERVED
FB-11 = UNSATISFIED / PRESERVED
A7-CL-02 = NOT_AUTHORIZED
CLINICAL_CONTENT_AUTHORING_AUTHORIZED = NO
```

`PHASE_B_NC` must not:

- satisfy FB-11
- bypass FB-11
- generate clinical Safety
- generate clinical Question
- generate Hypothesis
- activate `adult_respiratory_v1` clinical content

---

## 16. Suggested future batch graph

```text
Roadmap Amendment (this document)
  AUTHORED_PENDING_INDEPENDENT_REVIEW
  → independent review
  → standard merge
  → post-merge verification
  = amendment durable
  != implementation authorized

Later, only after separate tokens:

PBNC-01 implementation authorization
  → B2 mechanical core
  → PBNC-01 Exit

PBNC-02 implementation authorization
  → synthetic versioned store
  → PBNC-02 Exit
  → STOP

PHASE_B_NC_EXPANSION_CONTRACT_IMPACT_ASSESSMENT_REQUIRED
```

No batch in this graph is authorized by this document except the
docs/governance authoring of the amendment itself.

---

## 17. Zero counts

At amendment authoring and after any future PBNC-01 / PBNC-02 work
authorized under this lane:

```text
CLINICAL_CONTENT_COUNT = 0
PHI_COUNT = 0
REAL_PATIENT_COUNT = 0
REAL_PROVIDER_CALLS = 0
```

---

## 18. Immediate STOP conditions

Any of the following requires immediate STOP and a separate review.
Scope must not be expanded inside this lane:

1. Shared Contracts v1 semantic change
2. Encounter / ClinicalObservation / ObservationCandidate implementation
3. real medical rules
4. red-flag / threshold / triage content
5. Clinical Runtime
6. real provider
7. PHI / real patient
8. production DB
9. dual write
10. change to State Committer unique-writer architecture
11. change to Java / Python ownership
12. unexplained Enterprise / current-effective governance drift

---

## 19. Machine state after this amendment is authored

```text
PHASE_B_NC_STATE_FOUNDATION_ROADMAP_AMENDMENT =
  AUTHORED_PENDING_INDEPENDENT_REVIEW
PHASE_B_NC = PROPOSED_NOT_AUTHORIZED
PBNC-01 = NOT_AUTHORIZED
PBNC-02 = NOT_AUTHORIZED
STATE_COMMITTER_IMPLEMENTED = NO
B1_FULL = BLOCKED_PENDING_A5_CONTRACT_IMPACT_REVIEW
B3 = BLOCKED_PENDING_A5_CONTRACT_IMPACT_REVIEW
B4 = EXCLUDED
B5 = BLOCKED_PENDING_A5_CONTRACT_IMPACT_REVIEW
B6 = BLOCKED_PENDING_A5_CONTRACT_IMPACT_REVIEW
B7 = EXCLUDED
A7 = NOT_COMPLETE
A7-CL = DEFERRED_OUT_OF_CURRENT_BASELINE
A7-CL-02 = NOT_AUTHORIZED
FB-11 = UNSATISFIED / PRESERVED
PHASE_B = NOT_AUTHORIZED
PHASE_B_COMPLETE = NO
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
READY_FOR_INDEPENDENT_ROADMAP_AMENDMENT_REVIEW = YES
```
