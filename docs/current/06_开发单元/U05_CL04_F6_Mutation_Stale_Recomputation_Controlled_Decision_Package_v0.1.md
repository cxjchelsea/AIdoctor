# U05 CL-04 F6 Mutation-Stale Recalculation Controlled Decision Package v0.1

> Target blocker: `BF-U05-RG02-CL-04`  
> Design baseline: `268d9c5e9075420aa76c5c1aa37c254a3a99e2b8`  
> Closure finding source: PR #159 / exact review head `193a280cce0bb7c99ca0566166bffc7bb3aaec6a`  
> Scope: design-only / governance-only / no frozen amendment yet  
> Current status: **DESIGN / INDEPENDENT_REVIEW_PENDING**

---

## 1. Problem statement

Current frozen semantics already establish:

```text
F6 Offline Evidence Assessment lifecycle:
NOT_ASSESSED
ASSESSING
VALID
STALE
FAILED

F6 STALE
= new facts / DDx / Must-Exclude make the prior assessment non-current

STALE_BY_UPSTREAM_MUTATION
!= FAILED
!= UNAVAILABLE
```

and:

```text
POST_USER_FACT_UPDATE
may legally contain prior-activated F6 = STALE / INVALIDATED.
```

However the current `ClinicalContinuationRoutingDecision` vocabulary contains only:

```text
TO_U05_CLINICAL_READINESS
TO_F3_CURRENT_VERSION_REVALIDATION
TO_U08_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
FAILURE_ROUTE
```

No consequence lawfully asks F6 Owner to produce a current-version F6 assessment.

Therefore CL-04 remains blocking.

---

## 2. Frozen invariants that the solution must preserve

The remediation must preserve all of the following:

```text
F6 = unique Offline Evidence semantic Owner
U10 = existing F6 execution Unit
C05 = existing Offline Evidence / Workup Capability family

U05/D03 = unique Clinical Readiness Resolver
U09 / ClinicalContinuationRoutingDecision = routing policy host only
F7 = unique Delivery Readiness Owner

Capability Result != Clinical Truth
Deterministic Routing Decision != Clinical Readiness
F6 STALE != F6 FAILED
F6 STALE != NOT_NEEDED
absence/staleness != negative business result
```

No solution may infer:

```text
prior F6 JUSTIFIED -> current JUSTIFIED
prior F6 NOT_NEEDED -> current NOT_NEEDED
F6 STALE -> failure
F6 STALE -> no offline blocker
```

---

## 3. Candidate options

### Candidate A — explicit F6 current-version reassessment consequence hosted by U10

Add one typed continuation consequence:

```text
TO_F6_CURRENT_VERSION_REASSESSMENT
```

Execution host:

```text
U10
invocation_mode = F6_CURRENT_VERSION_REASSESSMENT
```

High-level flow:

```text
POST_USER_FACT_UPDATE
-> current U03/U04 Safety basis
-> ClinicalContinuationRoutingDecision
-> TO_F6_CURRENT_VERSION_REASSESSMENT
-> U10/F6 Owner reassesses current version
-> governed F6 commit
-> reload authoritative state
-> re-establish current routable Safety basis as required
-> re-enter ClinicalContinuationRoutingDecision
```

This preserves F6 ownership and keeps stale recomputation outside D03.

### Candidate B — implicit Scheduler pre-refresh before continuation router

Scheduler would silently inspect F6 staleness and invoke U10 before constructing a continuation routing decision.

Rejected as the preferred V1 design because:

```text
Scheduler would gain semantic knowledge of F6 lifecycle;
the routing decision would no longer be the explicit source of the business consequence;
trace/replay would become harder to prove;
the rule would be hidden outside the existing continuation policy host.
```

### Candidate C — route F6 mutation-stale through FAILURE_ROUTE

Rejected:

```text
STALE_BY_UPSTREAM_MUTATION != FAILED
```

### Candidate D — reuse prior F6 result or route directly to U11/U12

Rejected because it would either:

```text
reuse stale Clinical Truth
or
bypass U05/D03 Clinical Readiness ownership
or
infer delivery eligibility from a non-current F6 result.
```

### Design selection for independent review

```text
Candidate A
= PROPOSED V1 REMEDIATION
```

This package does not itself authorize the frozen amendment.

---

## 4. Proposed routing consequence

Add to `ClinicalContinuationRoutingDecision`:

```text
TO_F6_CURRENT_VERSION_REASSESSMENT
```

The generalized consequence vocabulary would become:

```text
TO_U05_CLINICAL_READINESS
TO_F3_CURRENT_VERSION_REVALIDATION
TO_F6_CURRENT_VERSION_REASSESSMENT
TO_U08_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
FAILURE_ROUTE
```

This consequence means only:

```text
a prior legally activated F6 assessment is mutation-stale
and F6 Owner must establish a current-version F6 assessment
before final continuation routing can proceed.
```

It does not mean:

```text
Clinical Readiness = NEEDS_OFFLINE_EVIDENCE
Offline Evidence Need = JUSTIFIED
Offline Evidence Need = NOT_NEEDED
Delivery Readiness = READY
failure
```

---

## 5. Exact admission rule

`TO_F6_CURRENT_VERSION_REASSESSMENT` is eligible only when all required conditions are true:

```text
evaluation_context = POST_USER_FACT_UPDATE

current accepted fact/correction mutation ref exists
current invalidation ref exists
prior F6 activation/assessment ref exists

prior F6 lifecycle
= STALE / INVALIDATED
and the staleness is attributable to the current accepted mutation

=> classification = STALE_BY_UPSTREAM_MUTATION

current U04 Safety basis permits the relevant ordinary continuation

all F6 upstream dependencies required for the new assessment
are current/compatible or lawfully not applicable

no higher unresolved owner prerequisite requires earlier recomputation
```

A missing mutation/invalidation/prior-activation provenance cannot use this route.

---

## 6. Owner-prerequisite ordering

F6 depends on upstream clinical semantics such as F3/F5 and governed evidence/rule context.

Therefore F6 reassessment must not run on known-stale prerequisites.

Required ordering:

```text
current Safety basis
-> owner prerequisite resolution
-> F6 current-version reassessment
-> continuation routing
```

At minimum:

```text
if required F3 is stale/not current:
    -> TO_F3_CURRENT_VERSION_REVALIDATION / fresh F3 path first

if prior F5 is activated and mutation-stale:
    -> governed F5/U08 reassessment first
       when its own route guards are satisfied

only when F6's declared required dependency refs are current/compatible:
    -> TO_F6_CURRENT_VERSION_REASSESSMENT
```

Important:

```text
F6 STALE does not become a "higher offline path".
It is unresolved owner state.

F5 mutation-stale may need to be resolved before F6
because a fresh F5 result may change F6's assessment basis.
```

The implementation must rely on declared dependency refs rather than hard-coded assumptions where possible.

---

## 7. U10 mode extension

U10 remains the only execution host for F6/C05.

Add a scoped invocation mode:

```text
U10
mode = F6_CURRENT_VERSION_REASSESSMENT
```

Existing ordinary U10 behavior remains a separate normal path:

```text
Clinical Readiness = NEEDS_OFFLINE_EVIDENCE
-> U10 ordinary offline-evidence action
```

No new Business Unit is introduced.

### 7.1 Reassessment-mode S_in

```text
mode = F6_CURRENT_VERSION_REASSESSMENT
evaluation_context = POST_USER_FACT_UPDATE

consultation_id / cdp_id
target authoritative Clinical State Version
current U04 Gate ref / restricted context when applicable

prior F6 assessment ref
current mutation ref
current F6 invalidation ref

current declared F6 dependency refs
current CapabilityBindingRef for C05
current KnowledgeReleaseRef / RuleReleaseRef
trace / correlation refs
```

### 7.2 Reassessment-mode action

U10 may invoke C05 under the existing governed capability boundary.

```text
C05 result
-> U10 / F6 Owner interpretation
-> canonical F6 intended effect
-> K09 proposal
-> G2/P01 commit
```

Reassessment mode may establish only current F6 truth needed for continuation:

```text
Offline Evidence Assessment = VALID
+ governed Offline Evidence Need = JUSTIFIED or NOT_NEEDED

or

Offline Evidence Assessment = FAILED
```

No Clinical Readiness value is emitted.

### 7.3 No premature delivery side effect

In `F6_CURRENT_VERSION_REASSESSMENT` mode:

```text
no patient-facing examination suggestion delivery
no U11 execution
no U12 execution
no Consultation completion
no Delivery Readiness decision
```

If C05 produces examination-suggestion candidate material during reassessment, it is:

```text
support/trace-only until the downstream ordinary U10 path is lawfully reached.
```

This mirrors the existing principle that pre-readiness clinical capability output cannot create downstream side effects by itself.

---

## 8. Reassessment result semantics

### 8.1 VALID + JUSTIFIED

```text
F6 current-version assessment
= VALID

Offline Evidence Need
= JUSTIFIED
```

After commit:

```text
re-enter governed continuation routing
-> current F6 exposes an actual Clinical Readiness path
-> TO_U05_CLINICAL_READINESS
-> D03
-> NEEDS_OFFLINE_EVIDENCE when frozen precedence resolves it
```

D03 remains the unique Clinical Readiness Resolver.

### 8.2 VALID + NOT_NEEDED

```text
F6 current-version assessment
= VALID

Offline Evidence Need
= NOT_NEEDED
```

After commit:

```text
re-enter ClinicalContinuationRoutingDecision
```

The router may then choose the unique next consequence from current F3/F5/F6/Safety state.

```text
NOT_NEEDED
!= READY
!= delivery
!= completion
```

### 8.3 FAILED / unavailable execution

```text
F6 reassessment FAILED / capability unavailable / governed execution failure
-> FAILURE_ROUTE / U14 eligibility as applicable
```

It must not emit `NOT_NEEDED`.

---

## 9. Avoiding a U05 -> U10 -> U05 -> U10 duplicate-assessment loop

The new pre-D03 reassessment creates a current F6 assessment before D03.

If D03 subsequently resolves:

```text
Clinical Readiness = NEEDS_OFFLINE_EVIDENCE
```

the ordinary U10 path must not blindly perform the same F6 assessment again.

Required reuse rule:

```text
U10 ordinary path
+ authoritative current F6 assessment exists
+ same current Clinical State/dependency basis is compatible
+ F6 assessment validity = CURRENT/VALID
-> reuse/attach the authoritative current F6 assessment
-> do not create a second Offline Evidence Assessment truth effect
```

The ordinary U10 path may then perform the remaining governed work allowed by the existing Unit, such as:

```text
examination-suggestion generation/validation
patient-safe downstream preparation
U11 handoff when the existing frozen business rule allows
```

If the current F6 assessment is absent or non-current, the ordinary U10 path follows its normal assessment behavior.

This rule preserves:

```text
one semantic F6 assessment effect per exact current basis
```

while allowing the existing post-readiness U10 workflow to continue.

---

## 10. Version safety after the F6 commit

F6 is canonical Clinical State.

Therefore the reassessment result must use:

```text
K09 StateChangeProposal
-> G2/P01 commit
-> authoritative Clinical State Version
```

After commit:

```text
the prior ClinicalContinuationRoutingDecision is stale/non-routable
Scheduler reloads authoritative state
```

The old U04 routing authorization must not be blindly reused.

Before re-entering ordinary continuation:

```text
current U04 Safety basis must be proven current-compatible
under existing dependency/version rules

if not current-compatible:
    -> re-establish required U03/U04 basis
```

This package does not invent a new Risk/Safety rule.

It only requires:

```text
F6 commit
!= permission to bypass current Safety routing requirements
```

---

## 11. Idempotency / replay identity

A reassessment effect must bind at least:

```text
consultation_id
cdp_id
target Clinical State Version before reassessment
evaluation_context = POST_USER_FACT_UPDATE

accepted mutation/correction event ref
F6 invalidation ref
prior F6 assessment/activation ref

declared current dependency refs
C05 CapabilityBindingRef
KnowledgeReleaseRef
RuleReleaseRef
contract/policy version

mode = F6_CURRENT_VERSION_REASSESSMENT
```

Proposed identity:

```text
F6_CURRENT_VERSION_REASSESSMENT_ID
=
consultation_id
+ target input Clinical State Version
+ accepted mutation ref
+ F6 invalidation ref
+ prior F6 assessment ref
+ declared dependency refs
+ capability/release binding refs
+ reassessment policy version
```

Same exact replay:

```text
-> attach/return authoritative prior reassessment effect
-> no duplicate C05-owned clinical effect
-> no duplicate K09/P01 state commit
```

If authoritative state advances or dependencies change before commit:

```text
stale-before-commit / commit conflict
-> reload
-> re-evaluate routing
-> no blind replay
```

---

## 12. Termination / no-cycle proof

The new route must not create an unbounded continuation loop.

Required progress invariant:

```text
TO_F6_CURRENT_VERSION_REASSESSMENT
may be selected only while the exact prior F6 effect
is mutation-stale for the exact current input basis.

successful reassessment commit
-> produces a new current F6 assessment ref
-> the same exact route condition becomes false.
```

A subsequent F6 reassessment is lawful only if:

```text
a new upstream mutation/dependency change
creates a new invalidation identity
```

If F5/F3 changes during prerequisite recomputation and invalidates F6 again, that is a distinct governed effect with distinct provenance, not replay of the same route.

Existing U08 no-progress/convergence protection remains unchanged.

---

## 13. D03 boundary

D03 must not receive a required mutation-stale F6 input.

Freeze:

```text
POST_USER_FACT_UPDATE
+ prior F6 activated
+ F6 = STALE_BY_UPSTREAM_MUTATION
+ F6 is required for the current continuation basis
-> D03 NOT ELIGIBLE
-> no D03 decision object
-> no Clinical Readiness commit
```

Only after F6 is current, or lawfully not required in the exact context, may:

```text
TO_U05_CLINICAL_READINESS
-> D03
```

This is a pre-D03 continuation-routing consequence, not D03 INPUT_FAILURE.

---

## 14. Failure semantics

The following remain distinct:

```text
F6 mutation-stale requiring reassessment
!= F6 Assessment FAILED

C05 invocation failure
!= Offline Evidence Need NOT_NEEDED

missing mutation provenance
!= normal F6 reassessment eligibility

invalid dependency basis
!= successful current F6 assessment
```

Missing/invalid provenance or impossible dependency reconciliation must fail closed through governed failure/admission handling.

---

## 15. Capability boundary

No new Clinical Capability family is introduced.

```text
C05 remains the F6 / Workup capability
FIRST_CONSUMER_UNIT remains U10
```

The new path changes U10 invocation timing/mode, not the Capability Owner.

Required bindings remain:

```text
P01
P04
P05
P06
C05
```

C05 still cannot directly own:

```text
Offline Evidence truth
Clinical Readiness
Delivery Readiness
Clinical State commit
```

Therefore a Phase-7 semantic amendment is not required by this design unless independent review finds that the existing Capability document over-constrains U10 invocation timing.

---

## 16. Exact frozen-artifact impact inventory

If this design passes independent review, a controlled amendment is expected to be required for exactly these six current frozen artifacts:

### A. Phase 5 — Business Loops

Add to BL-11:

```text
F6 mutation-stale
-> TO_F6_CURRENT_VERSION_REASSESSMENT
-> current F6
-> continuation routing
```

Preserve F6 Owner and Clinical Readiness ownership.

### B. Phase 6 — Verifiable Units

Amend U10 with:

```text
F6_CURRENT_VERSION_REASSESSMENT mode
assessment-only pre-D03 behavior
current-assessment reuse guard for later ordinary U10 path
```

Extend continuation routing consequences.

### C. Phase 8 — Contract & Data

Amend:

```text
ClinicalContinuationRoutingDecision vocabulary
K08 / F6 reassessment provenance/currentness/idempotency envelope
routing identity inputs
```

No new Clinical Readiness enum.

### D. Phase 9 — Runtime

Add Scheduler handling:

```text
TO_F6_CURRENT_VERSION_REASSESSMENT
-> U10 reassessment mode
-> commit/reload
-> current Safety compatibility check
-> re-enter continuation routing
```

Prevent stale route replay.

### E. U05-RDP-02

Freeze:

```text
F6 mutation-stale expected recomputation
= pre-D03 NON-ENTRY
!= INPUT_FAILURE
```

D03 becomes eligible only after current F6 requirements are satisfied.

### F. U05-RDP-05

Extend POST_USER_FACT_UPDATE applicability and requiredness:

```text
prior F6 activation
+ F6 mutation-stale
-> F6 reassessment required before D03 when F6 is required by the current path
```

### Explicitly not expected to require semantic amendment

```text
Phase 4 state ownership
Phase 7 Capability family ownership
U04-RDP-04 Safety ownership
Clinical Readiness six-value vocabulary
F7 Delivery Readiness ownership
```

If independent review finds any of these assumptions false, the amendment inventory must be expanded before authorization.

---

## 17. Required verification cases for later amendment/runtime phases

This design review does not authorize implementation, but the frozen amendment should make the following testable:

```text
CASE-01
prior F6 JUSTIFIED -> fact mutation -> F6 mutation-stale
-> F6 reassessment
-> current JUSTIFIED
-> U05/D03
-> NEEDS_OFFLINE_EVIDENCE

CASE-02
prior F6 JUSTIFIED -> fact mutation
-> reassessment -> current NOT_NEEDED
-> no automatic READY
-> continuation router selects next route from all current inputs

CASE-03
prior F6 NOT_NEEDED -> fact mutation
-> reassessment -> current JUSTIFIED
-> stale prior NOT_NEEDED is never reused

CASE-04
F3 stale + F6 stale
-> F3 owner prerequisite first when required
-> F6 reassessment only on current dependencies

CASE-05
F5 stale + F6 stale and F6 depends on F5
-> F5 reassessment first
-> resulting F5 change may invalidate/rebase F6
-> F6 reassessment second

CASE-06
F6 mutation-stale + C05 failure
-> failure route
-> never NOT_NEEDED

CASE-07
missing mutation/invalidation provenance
-> no F6 normal reassessment eligibility

CASE-08
same reassessment replay
-> no duplicate F6 clinical effect / state commit

CASE-09
F6 commit advances state
-> old continuation decision non-routable
-> reload/current Safety compatibility required

CASE-10
D03 = NEEDS_OFFLINE_EVIDENCE after pre-D03 F6 reassessment
-> ordinary U10 reuses current assessment
-> does not duplicate the assessment effect

CASE-11
reassessment-mode C05 returns suggestion candidate
-> no patient-facing delivery before lawful downstream U10/F7 path

CASE-12
FAILED / UNAVAILABLE
!= STALE_BY_UPSTREAM_MUTATION
```

---

## 18. Proposed blocker disposition

If independent review passes this design:

```text
BF-U05-RG02-CL-04
= DESIGN_SOLUTION_REVIEW_PASS / NOT_YET_CLOSED

design
= READY_FOR_CONTROLLED_AMENDMENT_AUTHORIZATION_DECISION
```

It must not move directly to CLOSED.

Closure still requires:

```text
explicit frozen amendment authorization
-> exact six-artifact amendment
-> independent amendment re-review
-> explicit re-freeze
-> repeat BF-U05-RG-02 Full Closure Re-Evaluation
```

---

## 19. Authorization boundary

This document does not authorize:

```text
modifying any frozen artifact
adding TO_F6_CURRENT_VERSION_REASSESSMENT to runtime code
adding a new U10 mode in code
C05 implementation/activation
U10 live execution
U04->U05/U10 live routing
merge
production Clinical Runtime
release activation
real-patient traffic
```
