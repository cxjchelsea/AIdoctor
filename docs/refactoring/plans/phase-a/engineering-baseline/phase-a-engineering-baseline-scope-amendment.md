# RESCOPE-01 Phase A Engineering Baseline Scope Amendment

> Dated: 2026-08-18
>
> Classification: `HIGH_PRODUCT_SAFETY_GOVERNANCE`
>
> Batch: `RESCOPE-01`
>
> Status: `ENGINEERING_BASELINE_SCOPE_AMENDMENT_AUTHORED_PENDING_REVIEW`
>
> Authorization token:
> `RESCOPE_01_ENGINEERING_BASELINE_SPLIT_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Exact Base: `9e40746fca30caaa2c5da6af0a4c0c900ab61390`
>
> Companion register:
> [phase-a-engineering-baseline-gate-register.csv](./phase-a-engineering-baseline-gate-register.csv)

```text
RESCOPE_01_DEFINES_PHASE_A_ENGINEERING_BASELINE
THE_OWNER_AUTHORIZED_PHASE_A_ENGINEERING_BASELINE_SCOPE_AMENDMENT
RESCOPE_01_DOES_NOT_REDEFINE_ORIGINAL_PHASE_A_FROZEN_BASELINE
RESCOPE_01_DOES_NOT_WAIVE_FB11_OR_FB21
RESCOPE_01_DOES_NOT_MARK_A7_COMPLETE
RESCOPE_01_DOES_NOT_AUTHORIZE_A11
RESCOPE_01_DOES_NOT_AUTHORIZE_CLINICAL_CONTENT
```

Until this amendment is independently reviewed and merged, proposed
dispositions below remain `PENDING_REVIEW`. They are **not** durable
Enterprise authority.

## 1. Purpose

Create a separately named current-product governance object:

```text
PHASE_A_ENGINEERING_BASELINE
```

because the original Phase A Frozen Baseline includes mandatory
clinical content and readiness gates that are intentionally
unreachable under the current clinical-expert resource model.

This batch defines the object, its included/excluded scope, claim
boundaries, debt policy, and a later independent review path. It does
**not** freeze the engineering baseline.

## 2. Owner resource constraint

```text
CLINICAL_EXPERT_RESOURCE_MODEL: NOT_AVAILABLE / NOT_PLANNED
qualified Clinical Owner:         NOT_AVAILABLE
qualified Human Clinical Reviewer: NOT_AVAILABLE
future expert acquisition:        NOT_PLANNED
```

Current product scope must not depend on eventual arrival of clinical
experts.

This does **not** mean clinical review is unnecessary, that AI may
substitute for a clinical expert, or that clinical gates may be waived.

## 3. Assessment provenance

Read-only assessment result:

```text
PHASE_A_ENGINEERING_BASELINE_SPLIT_RECOMMENDED
```

Core conclusion: original Phase A Frozen Baseline + A11 + FB-11 +
FB-21 / Full A7 are too clinically coupled to reinterpret as an
engineering-only freeze. Do **not** redefine that object. Create a
separate engineering baseline with its own later
`ENGINEERING_FREEZE_REVIEW`.

## 4. Current durable state

Unchanged until this amendment merges:

```text
PHASE_A_NC_CLOSURE:           COMPLETE
PHASE_A_NC_CLOSURE_EXIT:      PASS
A6.5:                         LEGACY_GOVERNANCE_CLOSED
A7-NC:                        COMPLETE / CLOSED / STABLE
A7-CL-01:                     GOVERNANCE_AMENDMENT_MERGED_AND_VERIFIED
A7 roadmap amendment:         SATISFIED
A7_CL_GOVERNANCE_DEFINED:     YES
A7_CL_CLINICAL_EXPERT_REVIEW_REQUIRED: YES
A7-CL:                        BLOCKED
FB-11:                        UNSATISFIED
A7-CL-02:                     NOT_AUTHORIZED
A7:                           NOT_COMPLETE
FB-21:                        PRESERVED / UNSATISFIED THROUGH A7 NOT_COMPLETE
A11:                          NOT_PASSED
Phase A Frozen Baseline:      NOT_READY_BLOCKED_CLINICAL
Clinical Runtime:             NOT_ENABLED
Production:                   BLOCKED
Phase B:                      NOT_AUTHORIZED
```

## 5. Original Frozen Baseline preservation

Object A remains the original clinical + engineering freeze object
defined by `架构冻结基线.md` §12 and A11.

It continues to require, as applicable, FB-11, FB-21 / Full A7, A7-CL,
clinical Capability first-edition content, clinical Prompt / model /
route evidence, clinical E2E expectations, and other existing A11
prerequisites. §12.4 is **not** weakened.

```text
ORIGINAL_PHASE_A_FROZEN_BASELINE: NOT_READY_BLOCKED_CLINICAL
ORIGINAL_PHASE_A_FROZEN_BASELINE: UNREACHABLE_UNDER_CURRENT_RESOURCE_MODEL
```

`UNREACHABLE_UNDER_CURRENT_RESOURCE_MODEL` is **not** WAIVED, PASSED,
SUPERSEDED_AS_HISTORY, or SATISFIED.

## 6. New Engineering Baseline identity

Object B is a separate current-product engineering governance object.

```text
PHASE_A_ENGINEERING_BASELINE_SCOPE: DEFINED_PENDING_REVIEW
PHASE_A_ENGINEERING_BASELINE:       NOT_FROZEN
ENGINEERING_FREEZE_ELIGIBILITY:     NOT_YET_INDEPENDENTLY_REVIEWED
```

Do **not** call it Phase A Frozen Baseline, A11 Frozen Baseline, or
Clinical Frozen Baseline. Future review name:

```text
ENGINEERING_FREEZE_REVIEW
```

A future engineering freeze PASS does **not** satisfy the original
Phase A Frozen Baseline or A11.

## 7. Product claim boundary

```text
TARGET_ARCHITECTURE:       retains future clinical-capable design
CURRENT_PRODUCT_BASELINE:  engineering-only / non-clinically-activated
```

Current truthful description:

medical-AI / clinical-agent engineering platform with clinical
activation excluded from the current product baseline.

Repository / project name is not changed here.

### Supportable claims

Where evidence already exists: medical-AI engineering platform;
clinical-agent **target architecture** prototype; Shared Contracts v1
structure; A7-NC structural Model Runtime governance complete;
NC Closure complete; clinical activation intentionally excluded;
legacy clinical content not migrated; Fake / offline / non-PHI
engineering validation; evidence / audit / trace / governance
structures; Capability Package structural framework.

### Forbidden claims

clinically validated; diagnostically accurate; approved for patient
care; safe for clinical use; approved triage; approved differential
diagnosis; approved medical recommendation; production clinical
decision support; production-ready medical agent; replacement for
clinician; clinically frozen; A11 PASS; Phase A Frozen Baseline PASS;
FB-11 SATISFIED; A7 COMPLETE.

## 8. Clinical permanent deferral

Proposed pending review:

```text
CLINICAL_TRACK: DEFERRED_OUT_OF_CURRENT_PRODUCT_BASELINE
A7-CL:          DEFERRED_OUT_OF_CURRENT_BASELINE
```

Permanent means `PERMANENT_FOR_CURRENT_PRODUCT_SCOPE_AND_RESOURCE_MODEL`.

```text
CLINICAL_TRACK_REENTRY: NOT_PLANNED_EXPLICIT_REAUTHORIZATION_REQUIRED
```

Do **not** write NEVER_ALLOWED. Do **not** write A7-CL COMPLETE.
A7-CL-01 remains `GOVERNANCE_AMENDMENT_MERGED_AND_VERIFIED`.
A7-CL-02 remains `NOT_AUTHORIZED`.

Durable Enterprise `A7-CL` remains `BLOCKED` until this amendment
merges.

## 9. A7 / FB-11 / FB-21 treatment

### FB-11

Original Frozen Baseline (unqualified object):

```text
FB-11: UNSATISFIED
FB-11: PRESERVED
```

Meaning remains: new clinical Safety / Question / Hypothesis
first-edition approval.

Engineering Baseline only (object-qualified):

```text
FB-11: NOT_APPLICABLE_TO_PHASE_A_ENGINEERING_BASELINE
```

Never publish unqualified `FB-11 = NOT_APPLICABLE`.
Never write FB-11 WAIVED / SATISFIED / PASS_BY_EXCEPTION / DEEMED_COMPLETE.

### FB-21 / Full A7 / A7

```text
A7:             NOT_COMPLETE
FB-21:          PRESERVED / UNSATISFIED THROUGH A7 NOT_COMPLETE
A7-ENGINEERING: COMPLETE
```

`A7-ENGINEERING COMPLETE` is an alias of `A7-NC COMPLETE / CLOSED /
STABLE`. It is **not** `A7 COMPLETE`. The Engineering Baseline
consumes A7-NC, not A7 overall COMPLETE.

## 10. A11 separation

```text
A11: NOT_PASSED
```

A11 is **not** reused as the engineering review. A11 eligibility and
PASS semantics are unchanged. The engineering object uses
`ENGINEERING_FREEZE_REVIEW` only after a later explicit authorization.

## 11. Engineering baseline included scope

Candidate included categories (not automatically SATISFIED):

- current build / test baseline (CI MVP evidence is not runtime PASS);
- Shared Contracts v1 structure (bindings created ≠ runtimes migrated);
- Capability **structural** package;
- A7-NC Model Runtime boundary;
- ADR **decision** coverage;
- Workflow / Trace engineering evidence as already recorded;
- NC Closure and NC-CLOSE-05 runtime **accounting**;
- inventory / coverage / migration reconciliation as recorded;
- Fake / offline / non-PHI verification where already used;
- governance / audit / ownership boundaries;
- provider = 0 and PHI = 0 as hard engineering constraints.

Lower-level satisfied facts may be recorded as SATISFIED. The
engineering baseline object itself remains `NOT_FROZEN`.

## 12. Clinical excluded scope

Outside `PHASE_A_ENGINEERING_BASELINE`:

- clinical S/Q/H approval and clinical Prompt approval;
- clinical activation / Clinical Runtime / clinical production;
- real provider / PHI / real patient data;
- medical source approval / clinical Knowledge Release;
- patient-care citation approval;
- executable State Committer / EncounterCDP clinical write /
  executable Safety Engine;
- A7-CL-02 / A7-CL-03;
- A11 Freeze Review / original Frozen Baseline PASS.

## 13. Engineering debt policy

Removing clinical scope does **not** lower unrelated engineering
quality requirements.

```text
PARTIALLY_VALIDATED != VERIFIED
RUNTIME_EVIDENCE_ACCOUNTING_COMPLETE != RUNTIME_VERIFIED
CI success != runtime success
```

Do not automatically forgive deferred ADR **implementation**.
RESCOPE-01 records classification. RESCOPE-EXIT independently judges
freeze eligibility.

## 14. A2 / NC04 treatment rules

### A2

Inventoried A2 production-candidate services with documented
`FAIL_CURRENT_IMPLEMENTATION` remain inside Engineering Baseline
runtime-startup scope unless independently classified otherwise.

- `workup-planner-service`: `ENGINEERING_FREEZE_BLOCKER`
- `diagnosis-engine-service`: `ENGINEERING_FREEZE_BLOCKER`
- `ocr-service`: `ENGINEERING_FREEZE_BLOCKER`
- `dialog-service`: `REQUIRES_RESCOPE_EXIT_JUDGMENT`
  (failure includes A7-NC legacy containment; do not reopen A7-NC)

Do not manufacture PASS.

### NC04

```text
NC-CLOSE-04: PARTIALLY_VALIDATED
TRACE:       PARTIALLY_VALIDATED
```

Do **not** upgrade. Historical NC Exit adjudication is preserved.
Remaining Trace debt for Engineering Baseline freeze eligibility:
`REQUIRES_RESCOPE_EXIT_JUDGMENT`.

## 15. Capability / Safety / Evidence boundary

`adult_respiratory_v1` remains DRAFT / PARTIALLY_VALIDATED /
REQUIRES_CLINICAL_REVIEW / NOT_IMPLEMENTED / Production BLOCKED /
owner_status UNASSIGNED.

Governance interpretation only:

```text
STRUCTURAL_CAPABILITY_PACKAGE
DESIGN_REFERENCE_ONLY
CLINICAL_ACTIVATION_DEFERRED
```

No lifecycle upgrade. No clinical content authoring.

Target architecture retains Mandatory Safety, fail-closed, and
human-control principles. Approved clinical red flags, triage rules,
thresholds, and validated Safety Engine behavior remain deferred.

Engineering may retain SourceArtifact / EvidencePack /
Claim↔SourceSpan / provenance / source-registry schema / Knowledge
Release **design**. Approved medical sources, clinical Knowledge
Release, and patient-care citation approval remain deferred.

Patient / clinician evidence UI contracts remain DESIGNED /
STRUCTURAL / NOT_CLINICALLY_ACTIVATED. Their existence does not
authorize unapproved medical claims.

## 16. Provider / PHI boundary

```text
real provider = 0 / FORBIDDEN
PHI = 0
real patient data = 0
```

Engineering Baseline verification uses Fake / offline / synthetic /
non-PHI. Real provider or PHI is outside RESCOPE-01.

## 17. Clinical re-entry gate

Re-entry is `NOT_PLANNED`. If product scope ever changes, minimum
requirements:

1. new Repository Owner explicit product-scope decision;
2. qualified Clinical Owner;
3. qualified Human Clinical Reviewer;
4. written clinical content-access authorization;
5. FB-11 Safety / Question / Hypothesis governance;
6. clinical Prompt review;
7. bounded clinical evaluation;
8. Capability binding / release review;
9. provider / privacy review where applicable;
10. independent clinical / safety governance review.

None of the following satisfies a clinical gate: LLM as physician;
AI self-review; AI-generated Owner / Reviewer / license / credential /
signature; internet or textbook references alone; synthetic doctor
approval; unqualified project owner acting as clinician.

Absence of experts changes product scope, not the definition of
clinical approval.

## 18. Future RESCOPE-EXIT

RESCOPE-01 defines the object and gate set. It does **not** pre-judge
freeze eligibility.

A later separately authorized `RESCOPE-EXIT` independently determines:

- which engineering gates are satisfied;
- which are blockers;
- whether open engineering debt prevents freeze eligibility;
- whether the engineering baseline is eligible for a separate
  `ENGINEERING_FREEZE_REVIEW`.

Do **not** run RESCOPE-EXIT in this batch.

## 19. Stop conditions

- `RESCOPE_01_BASE_DRIFT_STOPPED`
- `RESCOPE_01_FILE_SCOPE_EXPANSION_STOPPED`
- `RESCOPE_01_ENGINEERING_GATE_AUTHORITY_AMBIGUITY_STOPPED`
- `RESCOPE_01_PRODUCT_IDENTITY_CHANGE_REQUIRES_ARCHITECTURE_REVIEW`
- `PHASE_A_CLINICAL_GATE_WAIVER_NOT_ALLOWED`
- `A7_NC_SEMANTIC_REOPEN_REQUIRES_SEPARATE_ARCHITECTURE_REVIEW`
- `A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW`
- `PHASE_A_CLINICAL_CONTENT_NOT_AUTHORIZED`
- `PHASE_A_CLINICAL_REAL_PROVIDER_SEPARATE_AUTHORIZATION_REQUIRED`
- `PHASE_A_CLINICAL_PHI_SEPARATE_AUTHORIZATION_REQUIRED`
- `PHASE_B_IMPLEMENTATION_NOT_AUTHORIZED`
- `A11_FREEZE_REVIEW_NOT_AUTHORIZED`

## 20. Non-claims

This batch does **not** claim:

- `ENGINEERING_FROZEN_BASELINE_PASS`
- `ENGINEERING_BASELINE_COMPLETE`
- `RESCOPE-EXIT PASS`
- A11 PASS / original Frozen Baseline PASS
- A7 COMPLETE / FB-11 SATISFIED / FB-21 SATISFIED
- A7-CL COMPLETE / A7-CL STARTED / A7-CL-02 AUTHORIZED
- clinical validation / runtime verification / production readiness
- any clinical Safety / Question / Hypothesis / Prompt / threshold / gold
- waiver of FB-11 or FB-21
- removal of clinical modules from TARGET_ARCHITECTURE
