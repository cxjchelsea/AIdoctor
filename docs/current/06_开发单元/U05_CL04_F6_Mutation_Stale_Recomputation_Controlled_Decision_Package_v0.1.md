# U05 CL-04 F6 Mutation-Stale Recalculation Controlled Decision Package v0.1

> Target blocker: `BF-U05-RG02-CL-04`  
> Design baseline: `268d9c5e9075420aa76c5c1aa37c254a3a99e2b8`  
> Closure finding source: PR #159 / exact review head `193a280cce0bb7c99ca0566166bffc7bb3aaec6a`  
> Scope: design-only / governance-only / no frozen amendment yet  
> Current status: **REVISED / TARGETED_INDEPENDENT_AMENDMENT_DESIGN_REVIEW_PENDING**

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
mode = F6_CURRENT_VERSION_REASSESSMENT
or
mode = F6_CURRENT_VERSION_REVALIDATION
```

High-level flow:

```text
POST_USER_FACT_UPDATE
-> current U03/U04 Safety basis
-> ClinicalContinuationRoutingDecision
-> TO_F6_CURRENT_VERSION_REASSESSMENT
-> U10/F6 Owner reassesses current version
-> governed F6 commit
-> prior continuation decision / routing authorization becomes stale
-> reload authoritative state
-> mandatory post-F6 Safety barrier
-> establish a new current committed U04 Gate / routing authorization
-> if BLOCKED: Safety preempts; no F6 revalidation
-> if UNAVAILABLE: failure/safe handling preempts; no F6 revalidation
-> if ALLOW or action-permitted RESTRICTED:
   U10 F6_CURRENT_VERSION_REVALIDATION
-> current F6 readiness-input projection
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

current committed U04 Safety basis permits the relevant action

if Gate = RESTRICTED:
the current governed restricted policy explicitly permits
F6_CURRENT_VERSION_REASSESSMENT / C05 assessment for this context,
and restricted_context_ref is present

all F6 upstream dependencies required for the new assessment
are current/compatible or lawfully not applicable according to
the explicit F6 dependency-requiredness manifest

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

The reassessment contract MUST carry an explicit F6 dependency-requiredness manifest.

Minimum semantics:

```text
dependency_domain
dependency_ref
dependency_role
required_for_current_assessment = true / false
currentness
compatibility_status
lawful_not_applicable_reason when applicable
```

This manifest is authoritative for prerequisite ordering.

```text
Router / Scheduler
must not infer F3/F5 requiredness ad hoc
must not treat missing dependency metadata as NOT_NEEDED
```

The manifest must participate in reassessment admission, effect identity,
stale-before-commit validation, replay, and audit.

---

## 7. U10 mode extension

U10 remains the only execution host for F6/C05.

Add two scoped invocation modes:

```text
U10
mode = F6_CURRENT_VERSION_REASSESSMENT
mode = F6_CURRENT_VERSION_REVALIDATION
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

current F6 dependency-requiredness manifest
current CapabilityBindingRef for C05
current KnowledgeReleaseRef / RuleReleaseRef
restricted_context_ref when Gate = RESTRICTED
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

It must never be promoted merely because it already exists.

Later ordinary U10 must perform the existing governed suggestion validation
against the then-current:

```text
Clinical State basis
F6 assessment ref
CapabilityBindingRef
KnowledgeReleaseRef
RuleReleaseRef
restricted_context_ref when applicable
suggestion policy / validation rules
```

Pre-D03 candidate material may be referenced as evidence/support only.
It is never an authorization shortcut to VALIDATED / DELIVERABLE / patient-facing output.

This mirrors the existing principle that pre-readiness clinical capability output cannot create downstream side effects by itself.

### 7.4 F6 current-version revalidation mode

After a successful canonical F6 reassessment commit and the mandatory post-F6 Safety barrier, U10 must execute:

```text
mode = F6_CURRENT_VERSION_REVALIDATION
```

This mode is a deterministic F6 Owner decision.

It must NOT invoke C05 merely because the authoritative Clinical State Version advanced through downstream Risk/Safety commits.

Its purpose is only to determine whether the already-committed canonical F6 effect can be projected as current for the new authoritative continuation basis.

Required inputs:

```text
prior canonical F6 effect / assessment ref
F6 canonical effect identity
target current Clinical State Version
current committed U04 Gate ref
current routing_authorization_id
F6 dependency-requiredness manifest
current dependency refs
CapabilityBindingRef used by the canonical F6 effect
KnowledgeReleaseRef / RuleReleaseRef
restricted_context_ref when applicable
F6 revalidation policy/version
```

Allowed outcomes:

```text
REVALIDATED_CURRENT
REASSESSMENT_REQUIRED
FAILED
```

Semantics:

```text
REVALIDATED_CURRENT
-> create durable/auditable current F6 readiness-input projection
-> if canonical F6 = VALID + JUSTIFIED:
   project existing F6 business signal NEEDS_OFFLINE_EVIDENCE
-> if canonical F6 = VALID + NOT_NEEDED:
   project existing F6 business signal NO_BLOCKING_OFFLINE_EVIDENCE_NEED
-> no new F6 business vocabulary
-> no canonical F6 state mutation
-> no Clinical State Version advance
-> continuation routing may resume

REASSESSMENT_REQUIRED
-> no D03
-> return to U10 F6_CURRENT_VERSION_REASSESSMENT
   against the now-current dependency basis

FAILED
-> no D03
-> governed failure route
```

Mandatory invariant:

```text
F6_CURRENT_VERSION_REVALIDATION
!= C05 assessment
!= canonical F6 commit
!= Clinical Readiness
!= Delivery Readiness
```

Version advancement alone does not prove semantic invalidation, but currentness must be explicitly revalidated rather than assumed.

---

## 8. Reassessment result semantics

### 8.1 VALID + JUSTIFIED

```text
F6 current-version assessment
= VALID

Offline Evidence Need
= JUSTIFIED
```

After commit and successful post-Safety F6 current-version revalidation:

```text
REVALIDATED_CURRENT
-> re-enter governed continuation routing
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

After commit and successful post-Safety F6 current-version revalidation, the next routing host is context-specific.

For:

```text
POST_USER_FACT_UPDATE
POST_OFFLINE_ASSESSMENT
```

use:

```text
REVALIDATED_CURRENT
-> ClinicalContinuationRoutingDecision
```

For:

```text
A1_POST_BARRIER_CURRENT
```

do NOT enter ClinicalContinuationRoutingDecision.
Use the existing A1 routing projection after the current Gate/F3-currentness requirements are satisfied.

In all contexts, `NOT_NEEDED` itself is never READY.

For an Owner-approved first-analysis profile:

```text
first-entry eligibility = satisfied
evaluation_context in:
  A1_POST_BARRIER_CURRENT
  POST_USER_FACT_UPDATE
  POST_OFFLINE_ASSESSMENT

F1 = PRESENT / FRAMED_IN_SCOPE
F3 = PRESENT / CURRENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
F5 = NOT_YET_APPLICABLE
    = F5 has never lawfully activated in this Consultation path
F6 = PRESENT / CURRENT / VALID
F6 signal = NO_BLOCKING_OFFLINE_EVIDENCE_NEED
no higher blocker
all admission/currentness/provenance checks pass
```

the context-specific routing host may expose U05:

```text
A1_POST_BARRIER_CURRENT
-> existing A1 routing projection
-> U05_ELIGIBLE / U05

POST_USER_FACT_UPDATE
or POST_OFFLINE_ASSESSMENT
-> ClinicalContinuationRoutingDecision
-> TO_U05_CLINICAL_READINESS
-> U05
```

and D03 may apply the Owner-approved:

```text
D03-POL-011
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_AFTER_CURRENT_F6_NOT_NEEDED
-> DECIDED / READY_FOR_CLINICAL_ANALYSIS
```

For all other current profiles, the router selects the unique existing consequence according to current F3/F5/F6/Safety semantics.

Mandatory invariant:

```text
F6 NOT_NEEDED
!= READY by itself
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
fresh/current suggestion generation when needed
governed suggestion validation under current bindings
patient-safe downstream preparation
U11 handoff when the existing frozen business rule allows
```

Any support-only suggestion candidate produced during pre-D03 reassessment
must be revalidated and cannot be promoted by identity/replay alone.

If the current F6 assessment is absent or non-current, the ordinary U10 path follows its normal assessment behavior.

This rule preserves:

```text
one semantic F6 assessment effect per exact current basis
```

while allowing the existing post-readiness U10 workflow to continue.

---

## 10. Mandatory post-F6 Safety barrier

F6 is canonical Clinical State.

Therefore the reassessment result must use:

```text
K09 StateChangeProposal
-> G2/P01 commit
-> authoritative Clinical State Version advances
```

Current U04-RDP-04 semantics are authoritative:

```text
Clinical State Version advance
-> prior routing authorization = STALE / NON_ROUTABLE
```

Therefore the F6 reassessment path MUST be:

```text
F6 reassessment commit
-> prior ClinicalContinuationRoutingDecision stale
-> prior routing authorization stale
-> reload authoritative Clinical State
-> mandatory post-F6 Safety barrier
-> establish current U03/U04 basis as required
-> new current committed U04 Gate
-> new routing authorization

BLOCKED
-> Safety preempts
-> no F6 current-version revalidation
-> no ordinary continuation

UNAVAILABLE
-> governed failure/safe handling preempts
-> no F6 current-version revalidation
-> no ordinary continuation

ALLOW
or RESTRICTED with explicit F6-revalidation permission
-> U10 F6_CURRENT_VERSION_REVALIDATION
-> REVALIDATED_CURRENT / REASSESSMENT_REQUIRED / FAILED
-> only REVALIDATED_CURRENT may re-enter ClinicalContinuationRoutingDecision
```

There is no design-level shortcut of:

```text
"old Gate appears compatible"
-> reuse old routing authorization
```

unless a separately frozen future contract explicitly authorizes such a path.

This package does not invent a new Risk/Safety semantic.
It reuses existing U03/U04 machinery and existing Gate values.

For `RESTRICTED`:

```text
new Gate / routing authorization
must preserve the current restricted_context_ref
and must explicitly permit:
F6_CURRENT_VERSION_REVALIDATION
and any subsequent continuation consequence.
```

The post-F6 barrier must not widen:

```text
RESTRICTED -> generic ALLOW
RESTRICTED -> suggestion delivery
RESTRICTED -> U11/U12
```

without the relevant governed permission.

The newly committed F6 assessment must not be treated as D03-current merely because the Safety barrier completed.
Currentness is established only by the subsequent deterministic F6 revalidation decision.

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

F6 dependency-requiredness manifest
C05 CapabilityBindingRef
KnowledgeReleaseRef
RuleReleaseRef
restricted_context_ref when applicable
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
+ dependency-requiredness manifest identity
+ capability/release binding refs
+ restricted_context_ref when applicable
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

### 11.1 F6 current-version revalidation identity

Proposed deterministic revalidation identity:

```text
F6_CURRENT_VERSION_REVALIDATION_ID
=
consultation_id
+ canonical F6 effect identity
+ target current Clinical State Version
+ current U04 Gate ref
+ current routing_authorization_id
+ dependency-requiredness manifest identity
+ current dependency refs
+ capability/release compatibility refs
+ restricted_context_ref when applicable
+ F6 revalidation policy version
```

Same exact replay:

```text
-> attach authoritative prior revalidation decision
-> no C05 invocation
-> no second canonical F6 commit
-> no Clinical State Version advance
```

If dependency/release compatibility cannot be proven:

```text
-> REASSESSMENT_REQUIRED or FAILED according to frozen failure semantics
-> never silently REVALIDATED_CURRENT
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
-> produces a new canonical F6 assessment ref
-> mandatory Safety barrier runs
-> deterministic F6 current-version revalidation runs
-> REVALIDATED_CURRENT makes the same exact mutation-stale route condition false.
```

A subsequent F6 reassessment is lawful only if:

```text
a new upstream mutation/dependency change creates a new invalidation identity
or
F6_CURRENT_VERSION_REVALIDATION returns REASSESSMENT_REQUIRED
because the post-barrier current dependency basis is not compatible
with the canonical F6 effect.
```

F6 current-version revalidation itself is non-state-mutating, so successful revalidation cannot create a version-chasing loop.

If F5/F3 changes during prerequisite recomputation and invalidates F6 again, that is a distinct governed effect with distinct provenance, not replay of the same route.

Existing U08 no-progress/convergence protection remains unchanged.

---

## 13. D03 boundary and Owner-approved D03-POL-011

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

This remains a pre-D03 continuation-routing consequence, not D03 INPUT_FAILURE.

Owner decision:

```text
OD-U05-READY-02
= APPROVE_OPTION_A

decision source:
PR #161 exact reviewed head
d73b16d272e2096d0461850c9b482cff982aa7fc

decision record:
PR #162
```

Therefore the amendment design must add a separate D03 rule:

```text
D03-POL-011
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_AFTER_CURRENT_F6_NOT_NEEDED
```

Exact positive guards:

```text
first-entry eligibility is satisfied

evaluation_context in:
  A1_POST_BARRIER_CURRENT
  POST_USER_FACT_UPDATE
  POST_OFFLINE_ASSESSMENT

F1 = PRESENT / FRAMED_IN_SCOPE

F3 = PRESENT / CURRENT
F3 signal = NO_ACTIVE_ONLINE_BLOCKING_GAP

F5 = NOT_YET_APPLICABLE
= F5 has never lawfully activated in this Consultation path

F6 = PRESENT / CURRENT
F6 Assessment = VALID
F6 signal = NO_BLOCKING_OFFLINE_EVIDENCE_NEED

no qualified blocking offline signal
no lawful NEEDS_CLARIFICATION
no OUT_OF_SCOPE
no CAN_ASK_MORE
no required input failure / stale / unavailable
all required provenance/currentness/admission checks pass

-> DECIDED / READY_FOR_CLINICAL_ANALYSIS
```

D03-POL-011 is explicitly NOT_APPLICABLE when:

```text
POST_DDX_REEVALUATION
any prior/current F5 activation provenance
F5 PRESENT / STALE / INVALIDATED / FAILED / UNAVAILABLE
F6 STALE / FAILED / UNAVAILABLE
F6 has a JUSTIFIED blocking offline need
F3 is not current
F3 = CAN_ASK_MORE
OUT_OF_SCOPE
NEEDS_CLARIFICATION
Safety/admission does not permit first-analysis entry
```

Precedence remains:

```text
P2 OUT_OF_SCOPE
P3 blocking NEEDS_OFFLINE_EVIDENCE
P4 NEEDS_CLARIFICATION
P5 CAN_ASK_MORE
P6 positive READY layer:
   D03-POL-005
   D03-POL-011
P7 NO_RELIABLE_DIRECTION
```

Within P6:

```text
D03-POL-005
and
D03-POL-011
are mutually exclusive on F6 applicability/current-result state.

D03-POL-005:
  F6 = NOT_YET_APPLICABLE

D03-POL-011:
  F6 = PRESENT / CURRENT / VALID
  + NO_BLOCKING_OFFLINE_EVIDENCE_NEED

No 005-vs-011 internal ordering may affect the result.
```

D03-POL-006 remains P7 and cannot be shadowed because F5-present / NO_RELIABLE_DIRECTION profiles make both first-entry P6 subrules inapplicable.

and:

```text
D03-POL-005
= unchanged / exact frozen meaning
= F6 NOT_YET_APPLICABLE first-entry case

D03-POL-011
= separate current-F6-NOT_NEEDED first-entry case

D03-POL-006
= unchanged / authoritative for F5 NO_RELIABLE_DIRECTION
```

No new Clinical Readiness value is introduced.

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

Mode-aware dependency semantics are required:

```text
U10 STANDARD_OFFLINE_EVIDENCE_ACTION
-> C05 as required by existing behavior

U10 F6_CURRENT_VERSION_REASSESSMENT
-> C05 required

U10 F6_CURRENT_VERSION_REVALIDATION
-> no C05 invocation
-> deterministic F6 Owner decision only
```

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

Therefore Phase 7 requires a controlled mode-aware clarification/amendment analogous to the existing U06/C03 timing rule.

The aggregate dependency row may remain:

```text
U10 | C05
```

only if the same frozen section explicitly states that it is aggregate Unit-level dependency and that F6_CURRENT_VERSION_REVALIDATION is a no-C05 deterministic Owner mode.

---

## 16. Exact frozen-artifact impact inventory

If this design passes independent review, a controlled amendment is expected to be required for exactly these seven current frozen artifacts:

### A. Phase 5 — Business Loops

Add to BL-11:

```text
F6 mutation-stale
-> TO_F6_CURRENT_VERSION_REASSESSMENT
-> current F6
-> continuation routing

if current F6 = VALID / NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ F5 never activated
+ first-entry eligibility satisfied
-> TO_U05_CLINICAL_READINESS
-> D03-POL-011
-> READY_FOR_CLINICAL_ANALYSIS
-> governed U08/F5 first analysis
```

Preserve F6 Owner and Clinical Readiness ownership.
D03-POL-005 remains unchanged.

### B. Phase 6 — Verifiable Units

Amend U10 with:

```text
F6_CURRENT_VERSION_REASSESSMENT mode
F6_CURRENT_VERSION_REVALIDATION mode
assessment-only pre-D03 behavior
current-assessment reuse guard for later ordinary U10 path
```

Amend U05 verification/admission semantics with:

```text
D03-POL-011
FIRST_CLINICAL_ANALYSIS_ENTRY_AFTER_CURRENT_F6_NOT_NEEDED
```

including explicit first-entry context and prior-F5-activation exclusion.

Extend continuation routing consequences.

### C. Phase 7 — Capability Design

Add mode-aware U10/C05 usage timing:

```text
STANDARD_OFFLINE_EVIDENCE_ACTION -> C05 as applicable
F6_CURRENT_VERSION_REASSESSMENT -> C05 required
F6_CURRENT_VERSION_REVALIDATION -> deterministic Owner decision / no C05
```

Preserve C05 ownership boundary and U10 as first/only consumer Unit.

### D. Phase 8 — Contract & Data

Amend:

```text
ClinicalContinuationRoutingDecision vocabulary
K08 / F6 reassessment provenance/currentness/idempotency envelope
F6_CURRENT_VERSION_REVALIDATION deterministic decision envelope
D03-POL-011 deterministic-decision evidence fields / rule identity
first-entry eligibility/context provenance needed to distinguish never-activated F5
mandatory dependency-requiredness manifest
restricted-context propagation
routing identity inputs
```

No new Clinical Readiness enum.

### E. Phase 9 — Runtime

Add Scheduler handling:

```text
TO_F6_CURRENT_VERSION_REASSESSMENT
-> U10 reassessment mode
-> commit/reload
-> prior routing authorization stale
-> mandatory post-F6 Safety barrier
-> new current U04 Gate / routing authorization
-> U10 F6_CURRENT_VERSION_REVALIDATION

context = POST_USER_FACT_UPDATE / POST_OFFLINE_ASSESSMENT
-> only REVALIDATED_CURRENT enters ClinicalContinuationRoutingDecision
-> if first-entry current-F6-NOT_NEEDED profile is satisfied:
   TO_U05_CLINICAL_READINESS
   -> D03-POL-011

context = A1_POST_BARRIER_CURRENT
-> do not enter ClinicalContinuationRoutingDecision
-> preserve existing A1 routing projection
-> U05_ELIGIBLE / U05 when D03-POL-011 guards hold
```

Prevent stale route replay, prevent Router -> U08 bypass, and do not expand ClinicalContinuationRoutingDecision with an A1 context.

### F. U05-RDP-02

Freeze:

```text
F6 mutation-stale expected recomputation
= pre-D03 NON-ENTRY
!= INPUT_FAILURE
```

Add Owner-approved:

```text
D03-POL-011
= FIRST_CLINICAL_ANALYSIS_ENTRY_AFTER_CURRENT_F6_NOT_NEEDED
= separate positive READY rule
```

Preserve:

```text
D03-POL-005 unchanged
D03-POL-006 unchanged
six-value readiness vocabulary unchanged
```

D03 becomes eligible only after current F6 requirements are satisfied.

### G. U05-RDP-05

Extend F6 continuation applicability and requiredness:

```text
prior F6 activation
+ F6 mutation-stale
-> F6 reassessment required before D03 when F6 is required by the current path
```

Freeze the D03-POL-011 first-entry eligibility axis:

```text
F5 = NOT_YET_APPLICABLE
= never lawfully activated in this Consultation path

allowed contexts:
A1_POST_BARRIER_CURRENT
POST_USER_FACT_UPDATE
POST_OFFLINE_ASSESSMENT

POST_DDX_REEVALUATION
or any prior/current F5 activation provenance
-> D03-POL-011 NOT_APPLICABLE
```

### Explicitly not expected to require semantic amendment

```text
Phase 4 state ownership
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
-> old continuation decision / routing authorization non-routable
-> mandatory post-F6 Safety barrier
-> new current U04 Gate / routing authorization
-> F6_CURRENT_VERSION_REVALIDATION required
-> only REVALIDATED_CURRENT resumes continuation routing

CASE-09A
post-F6 Safety barrier = ALLOW or action-permitted RESTRICTED
and advances state version
but declared F6 dependencies remain compatible
-> deterministic revalidation
-> REVALIDATED_CURRENT
-> no C05 invocation
-> no second canonical F6 commit

CASE-09B
post-F6 Safety barrier permits continuation
but dependency change makes F6 basis incompatible
-> F6 revalidation = REASSESSMENT_REQUIRED
-> U10 F6_CURRENT_VERSION_REASSESSMENT
-> no silent reuse

CASE-09C
post-F6 Safety barrier = BLOCKED / UNAVAILABLE
-> no F6 current-version revalidation
-> no ordinary continuation
-> governed Safety/failure route

CASE-10
D03 = NEEDS_OFFLINE_EVIDENCE after pre-D03 F6 reassessment
-> ordinary U10 reuses current assessment
-> does not duplicate the assessment effect

CASE-11
reassessment-mode C05 returns suggestion candidate
-> support/trace-only
-> later ordinary U10 must revalidate under current bindings
-> no promotion-by-replay
-> no patient-facing delivery before lawful downstream U10/F7 path

CASE-11A
Gate = RESTRICTED
-> F6 reassessment only when restricted policy explicitly permits this action
-> restricted_context_ref preserved through routing / C05 / commit / barrier / re-entry

CASE-11B
dependency manifest missing/incomplete
-> no F6 normal reassessment eligibility
-> no ad-hoc Router/Scheduler requiredness inference

CASE-12
FAILED / UNAVAILABLE
!= STALE_BY_UPSTREAM_MUTATION

CASE-13
A1_POST_BARRIER_CURRENT
+ F5 never activated
+ F6 current VALID / NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ F1 framed in scope
+ F3 current no-gap
+ no higher blocker
-> existing A1 routing projection
-> U05_ELIGIBLE / U05
-> D03-POL-011
-> READY_FOR_CLINICAL_ANALYSIS
-> ClinicalContinuationRoutingDecision NOT invoked

CASE-14
POST_USER_FACT_UPDATE
+ F6 mutation-stale
-> reassessment -> post-Safety revalidation
-> F6 current VALID / NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ F5 never activated
+ first-entry guards complete
-> ClinicalContinuationRoutingDecision
-> TO_U05_CLINICAL_READINESS
-> D03-POL-011
-> READY_FOR_CLINICAL_ANALYSIS

CASE-15
POST_OFFLINE_ASSESSMENT
+ pre-F5 path
+ F5 never activated
+ F6 current VALID / NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ first-entry guards complete
-> ClinicalContinuationRoutingDecision
-> TO_U05_CLINICAL_READINESS
-> D03-POL-011
-> READY_FOR_CLINICAL_ANALYSIS

CASE-16
same positive profile
but F6 = NOT_YET_APPLICABLE
-> D03-POL-005
-> D03-POL-011 NOT_APPLICABLE

CASE-17
F6 current VALID / JUSTIFIED blocking need
-> NEEDS_OFFLINE_EVIDENCE precedence
-> D03-POL-011 NOT_APPLICABLE

CASE-18
F3 = CAN_ASK_MORE
+ F6 current NOT_NEEDED
-> CAN_ASK_MORE precedence
-> no READY shadowing

CASE-19
POST_DDX_REEVALUATION
or prior F5 activation provenance
-> D03-POL-011 NOT_APPLICABLE

CASE-20
F5 = NO_RELIABLE_DIRECTION
-> D03-POL-006 remains authoritative
-> D03-POL-011 NOT_APPLICABLE

CASE-21
F5 artifact absent
but prior F5 activation provenance exists
-> must not relabel F5 as NOT_YET_APPLICABLE
-> no D03-POL-011
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
-> exact seven-artifact amendment
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


---

## 20. Independent Design Review Remediation

Review source:

```text
PR #160
reviewed head = c67055c524a897e8ace66814bc133006f1380d20
review_id = 5263166691
verdict = REVISE_REQUIRED
```

Remediation applied in this revision:

### BF-U05-F6R-IR-01

```text
POST_F6_COMMIT_SAFETY_BARRIER_UNDERSPECIFIED
-> REMEDIATED
```

The design now requires a mandatory post-F6 Safety barrier and a new current committed U04 Gate / routing authorization after the F6 state commit.

### BF-U05-F6R-IR-02

```text
RESTRICTED_ACTION_PERMISSION_NOT_EXPLICIT
-> REMEDIATED
```

RESTRICTED may expose F6 reassessment only under explicit action-specific governed permission; the restricted context is carried through the entire effect chain.

### RQ-U05-F6R-IR-03

```text
PRE_D03_SUGGESTION_REUSE_BOUNDARY
-> REMEDIATED
```

Pre-D03 suggestion material remains support/trace-only and must be revalidated under current bindings before any formal suggestion/delivery state.

### RQ-U05-F6R-IR-04

```text
F6_DEPENDENCY_REQUIREDNESS_MANIFEST
-> REMEDIATED
```

The F6 dependency-requiredness manifest is now mandatory and participates in admission, ordering, identity, stale detection, replay, and audit.

Current status:

```text
F6 Mutation-Stale Reassessment Decision Package
= REVISED / TARGETED_INDEPENDENT_REVIEW_PENDING

BF-U05-RG02-CL-04
= OPEN / BLOCKING pending targeted re-review
```


---

## 21. Targeted Re-Review #1 Remediation

Review source:

```text
PR #160
reviewed head = 330e4b76e7fe6f89ecc32b6ab98678e004d1814b
review_id = 5263173177
verdict = REVISE_REQUIRED
```

### BF-U05-F6R-TR-01

```text
POST_SAFETY_F6_CURRENTNESS_REVALIDATION_MISSING
-> REMEDIATED
```

The design now adds:

```text
F6 reassessment commit
-> mandatory post-F6 Safety barrier
-> current U04 Gate / routing authorization
-> U10 F6_CURRENT_VERSION_REVALIDATION
-> REVALIDATED_CURRENT / REASSESSMENT_REQUIRED / FAILED
```

The revalidation mode is deterministic, does not invoke C05 when compatibility can be proven, does not commit a second canonical F6 effect, and does not advance Clinical State.

### RQ-U05-F6R-TR-02

```text
PHASE7_MODE_AWARE_U10_C05_DEPENDENCY_IMPACT
-> REMEDIATED
```

The exact controlled-amendment inventory is expanded to seven frozen artifacts and Phase 7 gains explicit mode-aware U10/C05 semantics.

Current status:

```text
F6 Mutation-Stale Reassessment + READY-02 Option A Amendment Design
= OWNER_OPTION_A_INCORPORATED
= INDEPENDENT_AMENDMENT_DESIGN_REVIEW_PENDING

OD-U05-READY-02
= APPROVE_OPTION_A

BF-U05-F6R-TR-03
= OWNER_POLICY_SELECTED / DESIGN_INCORPORATED

BF-U05-RG02-CL-04
= OPEN / BLOCKING pending independent amendment design review
```


---

## 22. OD-U05-READY-02 Option A Incorporation

Owner decision:

```text
OD-U05-READY-02
= APPROVE_OPTION_A
```

Decision package:

```text
PR #161
exact reviewed head = d73b16d272e2096d0461850c9b482cff982aa7fc
Targeted Independent Re-Review = PASS
review_id = 5263206685
```

Decision record:

```text
PR #162
decision-record commit = 1b3e7c8786c65d8a40bd2ad753410b83d29746f8
```

Selected policy:

```text
D03-POL-011
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_AFTER_CURRENT_F6_NOT_NEEDED
```

The selected policy is incorporated into this CL-04 controlled amendment design.

Important:

```text
Owner APPROVE_OPTION_A
!= frozen artifact amendment authorization
!= D03-POL-011 refrozen
!= implementation authorization
```

### 22.1 Exact combined frozen-artifact inventory

The F6 recomputation design and D03-POL-011 overlap on existing artifacts.

The combined amendment remains exactly seven frozen artifacts:

```text
1. docs/current/05_业务闭环/业务闭环设计_V1.md
2. docs/current/06_开发单元/可验证开发单元拆分_V1.md
3. docs/current/07_能力设计/按开发单元的Capability设计.md
4. docs/current/08_契约与数据/Contract与数据语义设计.md
5. docs/current/09_Runtime与技术架构/Runtime与技术架构设计_V1.md
6. docs/current/06_开发单元/U05_RDP02_D03_Policy_Owner_Decision_Contract_v0.1.md
7. docs/current/06_开发单元/U05_RDP05_Readiness_Input_Dependency_Applicability_Contract_v0.1.md
```

No eighth artifact is required merely because D03-POL-011 was selected.

### 22.2 Semantic amendment summary

```text
Phase 5:
  continuation loop includes F6 reassessment/revalidation
  + D03-POL-011 first-entry return path

Phase 6:
  U10 reassessment/revalidation modes
  + U05 D03-POL-011 admission/verification semantics

Phase 7:
  U10/C05 mode-aware dependency timing

Phase 8:
  typed F6 reassessment/revalidation envelopes
  + D03-POL-011 decision evidence/context provenance

Phase 9:
  explicit Scheduler consequences
  + post-F6 Safety barrier
  + no Router->U08 bypass
  + U05 routing only after actual D03 path exists

RDP-02:
  mutation-stale pre-D03 NON-ENTRY
  + D03-POL-011 executable expectation
  + D03-POL-005/006 preserved

RDP-05:
  F6 currentness/applicability
  + never-activated-F5 first-entry axis
  + cross-context eligibility/exclusion
```

### 22.3 Current governance state

```text
OD-U05-READY-02
= APPROVE_OPTION_A

D03-POL-011
= OWNER_APPROVED_DESIGN_EXPECTATION
= NOT_YET_FROZEN

CL-04 combined controlled amendment design
= READY_FOR_INDEPENDENT_AMENDMENT_DESIGN_REVIEW

Frozen artifact modification
= NOT_AUTHORIZED

Runtime implementation
= NOT_AUTHORIZED
```


---

## 23. Combined Amendment Design Independent Review Remediation

Review source:

```text
PR #160
reviewed head = f6f2200a874707ce687162052a17be30ab70a502
review_id = 5263224396
verdict = REVISE_REQUIRED
```

### BF-U05-F6R-ADR-01

```text
FIRST_ENTRY_ROUTING_HOST_CONFLATION
-> REMEDIATED
```

The design now keeps one D03-POL-011 but separates routing hosts:

```text
A1_POST_BARRIER_CURRENT
-> existing A1 routing projection
-> U05

POST_USER_FACT_UPDATE
POST_OFFLINE_ASSESSMENT
-> ClinicalContinuationRoutingDecision
-> TO_U05_CLINICAL_READINESS
-> U05
```

A1 is not added as a ClinicalContinuationRoutingDecision context.

### RQ-U05-F6R-ADR-02

```text
POSITIVE_READY_SUBRULE_PRECEDENCE_EXPLICITNESS
-> REMEDIATED
```

D03-POL-005 and D03-POL-011 are explicit mutually exclusive P6 positive READY subrules.
D03-POL-006 remains P7 and cannot be shadowed by either first-entry subrule.

Current:

```text
Combined Amendment Design
= REVISED / TARGETED_INDEPENDENT_AMENDMENT_DESIGN_REVIEW_PENDING

D03-POL-011
= OWNER_APPROVED_DESIGN_EXPECTATION / NOT_YET_FROZEN

Frozen artifact modification
= NOT_AUTHORIZED
```
