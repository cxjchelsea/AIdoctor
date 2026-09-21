# U05 Post-DDx Policy / Routing Controlled Decision Package v0.1

> Trigger blocker:
>
> ```text
> BF-U05-RG02-CL-01
> = POST_DDX_NORMAL_PROGRESS_POLICY_COVERAGE_GAP
> = OPEN / BLOCKING
> ```
>
> Baseline:
>
> ```text
> RDP-02 READY-policy re-frozen exact head
> = 8efd6a4c17d07752900e90026e5e1496d0c9f001
> ```
>
> Status:
>
> ```text
> CONTROLLED_DECISION_PACKAGE
> = PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW
>
> Frozen amendment
> = NOT_AUTHORIZED
>
> Runtime implementation
> = NOT_AUTHORIZED
> ```

---

# 1. Problem statement

After U08/DDx, frozen Phase 6 U09 sends new readiness inputs toward U05.

RDP-05 legally admits POST_DDX_REEVALUATION profiles with:

```text
F3 = PRESENT expected
F5 = PRESENT expected
```

F5 may emit:

```text
ANALYSIS_RESULT_AVAILABLE
NO_RELIABLE_DIRECTION
NEEDS_OFFLINE_EVIDENCE
REASSESSMENT_REQUIRED
```

The current D03 policy fully covers:

```text
OUT_OF_SCOPE
blocking NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
FIRST_CLINICAL_ANALYSIS_ENTRY READY
NO_RELIABLE_DIRECTION
```

but does not cover the lawful post-DDx profiles:

```text
F5 = ANALYSIS_RESULT_AVAILABLE
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher blocker
```

or:

```text
F5 = REASSESSMENT_REQUIRED
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher blocker
```

Therefore D03 currently has a lawful POLICY_EXPECTATION_GAP.

---

# 2. Existing frozen authority that should be reused

Phase 4 already freezes:

```text
F7 = Delivery Package + Delivery Readiness Owner
```

System-level Delivery Readiness already exists:

```text
NOT_READY
READY
BLOCKED
DELIVERED
```

Phase 5 BL-10 already freezes normal-delivery preconditions:

```text
Consultation = ACTIVE
Safety Gate permits normal delivery
current Clinical State Version valid
Risk valid
F5 DDx Assessment = VALID
key Must-Exclude appropriately handled
no online/offline critical Gap blocks normal result
```

Phase 6 U12 already freezes:

```text
NORMAL_DELIVERY_ALLOWED
→ F7 assemble/validate Delivery
→ Delivery Readiness READY/DELIVERED
→ Consultation COMPLETED
```

Therefore the correct fix should reuse Delivery Readiness instead of inventing a new Clinical Readiness value.

---

# 3. Architecture decision

Proposed architecture:

```text
Clinical Readiness
= acquisition / analysis-entry decision axis

Delivery Readiness
= delivery legality / package validation axis
```

The two are different governed system-level states.

Post-DDx routing must no longer require every normal-progress consequence to be encoded as a Clinical Readiness value.

---

# 4. Post-DDx governed consequence model

Introduce a deterministic non-clinical-enum routing decision:

```text
PostDdxRoutingDecision
```

Owner semantics are composed from existing owners:

```text
F3 owns current Gap semantics
F5 owns current DDx/Must-Exclude semantics
F6 owns offline-evidence need
F7 owns Delivery Readiness
U05/D03 owns Clinical Readiness only
Scheduler executes the selected governed consequence
```

The routing decision itself does not invent medical truth.

It consumes committed/current owner outputs and selects which existing governed axis must act next.

---

# 5. Decision host

Execution host:

```text
U09 POST_DDX_ROUTING
```

U09 remains a coordinator, not an owner of F3/F5/F6/F7 truth.

U09 may host a deterministic routing policy using already-governed outputs.

U09 must not:

```text
invent F3 gap
invent F5 DDx
invent F6 offline evidence
invent F7 Delivery Readiness
invent Clinical Readiness
```

---

# 6. Post-DDx routing precedence

Safety remains first and external to ordinary routing.

For a current admitted post-DDx state:

```text
P0 Safety BLOCKED / UNAVAILABLE
→ existing Safety route

P1 input failure / stale / conflict
→ governed failure / reevaluation path

P2 OUT_OF_SCOPE
→ U11

P3 qualified blocking NEEDS_OFFLINE_EVIDENCE
→ U10

P4 lawful NEEDS_CLARIFICATION
→ U06

P5 F3 = CAN_ASK_MORE
→ U06

P6 F5 = NO_RELIABLE_DIRECTION
   + F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
   + no higher path
→ U11

P7 F5 = REASSESSMENT_REQUIRED
   + F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
   + no higher path
→ U08

P8 F5 = ANALYSIS_RESULT_AVAILABLE
   + F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
   + no qualified blocking offline evidence
   + no higher path
→ F7 Delivery Readiness evaluation
→ if F7 READY, U12 NORMAL_DELIVERY_ALLOWED
```

This precedence is a post-DDx routing policy, not a replacement for D03 P0-P7.

---

# 7. Relationship to D03

D03 remains authoritative for Clinical Readiness.

For POST_DDX_REEVALUATION:

```text
if the next consequence is still a Clinical Readiness consequence:
→ use D03
```

Examples:

```text
OUT_OF_SCOPE
NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
NO_RELIABLE_DIRECTION
```

But:

```text
F5 ANALYSIS_RESULT_AVAILABLE + no remaining blocker
```

does not need to manufacture another Clinical Readiness value.

Likewise:

```text
F5 REASSESSMENT_REQUIRED + no higher acquisition need
```

is a post-analysis routing consequence back to U08, not a Clinical Readiness result.

Therefore:

```text
PostDdxRoutingDecision
!= Clinical Readiness
!= D03 decision_status
```

---

# 8. Normal-delivery path

For:

```text
F5 = ANALYSIS_RESULT_AVAILABLE
F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
no blocking F6 need
current Safety permits ordinary/normal-delivery preparation
```

U09 emits:

```text
TO_U12_DELIVERY_PREPARATION
```

This consequence means only:

```text
the current post-DDx state is eligible to enter U12's existing normal-delivery preparation/validation path
```

It does NOT mean:

```text
Delivery Readiness = READY
delivery side effect is already authorized
Consultation may already complete
```

U12 admission must still validate the existing BL-10/U12 prerequisites:

```text
Consultation ACTIVE
current Safety permits normal delivery
current Clinical State Version valid
Risk valid for the required basis
F5 DDx Assessment VALID/current
key Must-Exclude appropriately handled
no blocking online/offline critical Gap
required governance/binding refs current
```

Inside U12, F7 remains the unique Delivery Readiness Owner:

```text
F7 NOT_STARTED / ASSEMBLING / VALIDATING
→ Delivery Readiness NOT_READY

F7 VALIDATION_FAILED
→ Delivery Readiness BLOCKED
→ U14 / repair path

F7 VALIDATED
→ Delivery Readiness READY
→ delivery side effect may execute

F7 DELIVERED
→ Delivery Readiness DELIVERED
→ Consultation COMPLETED
```

Thus U12 creates/advances Delivery Readiness; Delivery Readiness READY is not a prerequisite for entering U12.

No Clinical Readiness enum is added.

---

# 9. Reassessment path

For:

```text
F5 = REASSESSMENT_REQUIRED
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no blocking offline evidence
+ no clarification need
+ no OUT_OF_SCOPE
+ current F5 result valid/current
```

U09 may emit:

```text
TO_U08_REASSESSMENT
```

only after proving no higher Safety / acquisition / offline path exists.

Scheduler invokes U08 only when:

```text
required U08 CapabilityBindingRef / RuleReleaseRef / KnowledgeReleaseRef are current
current Clinical State Version is compatible
current U04 Gate permits the action
existing bounded/no-progress protection permits another analysis cycle
```

If a new CAN_ASK_MORE, NEEDS_CLARIFICATION, blocking NEEDS_OFFLINE_EVIDENCE, or OUT_OF_SCOPE signal exists, U09 must emit:

```text
TO_U05_CLINICAL_READINESS
```

instead.

This path must be idempotent and bounded.

A repeated:

```text
REASSESSMENT_REQUIRED
→ U08
→ REASSESSMENT_REQUIRED
```

cycle must be subject to the existing convergence/no-progress protections.

---

# 10. Clinical Readiness uniqueness qualification

Existing statement:

```text
same Clinical State Version
→ one unique Clinical Readiness
→ one unique ordinary next business path
```

must be narrowed.

Proposed frozen qualification:

```text
same Clinical State Version
→ at most one authoritative Clinical Readiness when a Clinical Readiness decision is applicable

but

post-DDx next business consequence may instead be governed by:
- PostDdxRoutingDecision
- Delivery Readiness
when the next step is no longer a Clinical Readiness question.
```

The system must still have:

```text
one unique ordinary next business consequence
```

but not every consequence must be represented by Clinical Readiness.

---

# 11. U05 / U09 / U12 ownership boundary

## U05

Owns only Clinical Readiness resolution.

It does not own normal-delivery legality.

## U09

Hosts deterministic post-DDx consequence routing using current owner outputs.

It does not create clinical truth.

## F7

Owns Delivery Readiness and Delivery Package validity.

## U12

Executes normal Delivery after the governed delivery preconditions/trigger are established.

U12 must not infer its own eligibility from absence of blockers.

---

# 12. Proposed PostDdxRoutingDecision contract

Reuse Phase-8 DeterministicDecision structure.

Proposed decision type:

```text
POST_DDX_ROUTING
```

Minimum fields:

```text
decision_id
decision_type = POST_DDX_ROUTING
consultation_id
input_clinical_state_version
evaluation_context = POST_DDX_REEVALUATION

f1_input_ref
f3_input_ref
f5_input_ref
f6_input_ref?
current_u04_gate_ref

decision
reason_codes[]
basis_refs[]

policy_id = POST_DDX_ROUTING
policy_version
rule_release_refs[]
knowledge_release_refs[]
created_at
validity/staleness
trace_refs[]
```

Allowed decisions:

```text
TO_U05_CLINICAL_READINESS
TO_U08_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
FAILURE_ROUTE
```

These are Unit-level routing consequences only.

They are:

```text
!= Clinical Readiness
!= Delivery Readiness
!= D11
!= new D01-D10 system-level policy family
!= new Clinical State truth category
```

The decision reuses the generic Phase-8 `DeterministicDecision` contract as a scoped U09 routing decision.

It must never directly emit:

```text
OUT_OF_SCOPE
NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
NO_RELIABLE_DIRECTION
```

Those remain exclusively resolved by U05/D03.

---

# 13. Avoid duplicate decision ownership

The routing policy should not re-implement all D03 semantics independently.

Preferred V1 split:

```text
U09 first evaluates explicit post-DDx-only consequences:

1. F5 REASSESSMENT_REQUIRED with no higher acquisition blocker
   → TO_U08_REASSESSMENT

2. F5 ANALYSIS_RESULT_AVAILABLE with no acquisition/offline blocker
   → TO_U12_DELIVERY_PREPARATION

Otherwise:
   → TO_U05_CLINICAL_READINESS
```

Then U05/D03 continues to resolve:

```text
OUT_OF_SCOPE
NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
NO_RELIABLE_DIRECTION
```

This avoids duplicating D03 precedence inside U09.

---

# 14. Exact V1 routing algorithm

```text
Input: current post-DDx owner outputs

Step 1
If Safety does not permit ordinary progression
→ existing Safety route outside PostDdxRoutingDecision

Step 2
If required post-DDx inputs are stale/failed/conflicting
or required routing/binding context is invalid
→ FAILURE_ROUTE

Step 3
If any current owner output requires a Clinical Readiness consequence:
- OUT_OF_SCOPE
- qualified blocking NEEDS_OFFLINE_EVIDENCE
- NEEDS_CLARIFICATION
- F3 CAN_ASK_MORE
- F5 NO_RELIABLE_DIRECTION

→ TO_U05_CLINICAL_READINESS

U09 does not decide which Clinical Readiness value wins.
D03 applies its existing frozen precedence.

Step 4
Else if:
F5 = REASSESSMENT_REQUIRED
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher acquisition/offline/scope path
+ current U08 bindings valid
+ bounded/no-progress policy permits another analysis cycle

→ TO_U08_REASSESSMENT

Step 5
Else if:
F5 = ANALYSIS_RESULT_AVAILABLE
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no blocking F6 need
+ normal-delivery preparation prerequisites are current enough to enter U12

→ TO_U12_DELIVERY_PREPARATION

Step 6
Else
→ POLICY_EXPECTATION_GAP / design blocker
```

Important:

```text
absence of blocker
!= Delivery Readiness READY
```

Only F7 inside the governed U12 path can interpret Delivery Readiness.

Likewise:

```text
TO_U12_DELIVERY_PREPARATION
!= normal delivery completed
!= Delivery Readiness READY
```

---

# 15. BF-U05-RG02-CL-01 resolution

If this design is approved and frozen consistently across affected artifacts:

```text
ANALYSIS_RESULT_AVAILABLE
→ TO_U12_DELIVERY_PREPARATION
→ U12/F7 assembles and validates
→ Delivery Readiness NOT_READY → READY → DELIVERED

REASSESSMENT_REQUIRED
→ TO_U08_REASSESSMENT
```

Then the two currently uncovered post-DDx profiles no longer require a fabricated D03 result.

Expected effect:

```text
POLICY_EXPECTATION_GAP
no longer legally triggerable for the known CL-01 profiles
```

RG-02 can then be re-evaluated for closure.

---

# 16. Frozen-artifact impact inventory

This design would materially affect at least:

```text
1. Phase 5 Business Loops
   - qualify Clinical Readiness uniqueness
   - explicit post-DDx Delivery Readiness handoff

2. Phase 6 Units
   - U09 gains deterministic POST_DDX_ROUTING host
   - U12 trigger source clarified

3. Phase 8 Contracts
   - PostDdxRoutingDecision contract

4. Phase 9 Runtime
   - Scheduler consumes post-DDx routing decision + Delivery Readiness

5. U05-RDP-02
   - scope D03 to Clinical Readiness consequences
   - mark ANALYSIS_RESULT_AVAILABLE / REASSESSMENT_REQUIRED as post-DDx routing-owned, not D03 gaps

6. U05-RDP-05
   - clarify F5 post-DDx signals may be consumed by U09 routing before/without D03
```

Phase 4 F7 ownership itself does not need semantic change; it already owns Delivery Readiness.

U04 Safety ownership does not change.

---

# 17. Verification requirements

At minimum:

```text
PDX-E01 ANALYSIS_RESULT_AVAILABLE + no gap/blocker
→ U09 TO_U12_DELIVERY_PREPARATION
→ no D03 READY fabrication

PDX-E02 TO_U12_DELIVERY_PREPARATION
→ Delivery Readiness initially NOT_READY
→ no delivery side effect before F7 validation

PDX-E03 F7 VALIDATED
→ Delivery Readiness READY
→ delivery side effect may execute

PDX-E04 F7 VALIDATION_FAILED / BLOCKED
→ no COMPLETED

PDX-E05 REASSESSMENT_REQUIRED + no higher blocker
→ U08

PDX-E06 CAN_ASK_MORE + ANALYSIS_RESULT_AVAILABLE
→ U05/D03 CAN_ASK_MORE, not delivery

PDX-E07 blocking offline evidence + ANALYSIS_RESULT_AVAILABLE
→ U05/D03 NEEDS_OFFLINE_EVIDENCE, not delivery

PDX-E08 NO_RELIABLE_DIRECTION
→ U05/D03 NO_RELIABLE_DIRECTION, not delivery

PDX-E09 stale/failed F5
→ no delivery/reassessment routing

PDX-E10 repeated REASSESSMENT_REQUIRED
→ bounded/no-progress protection

PDX-E11 same event replay
→ no duplicate Delivery / U08 effect

PDX-E12 Delivery Readiness remains F7-owned
```

---

# 18. Decision package recommendation

Recommended V1 architecture:

```text
KEEP six-value Clinical Readiness unchanged

ADD scoped deterministic PostDdxRoutingDecision hosted by U09

allowed routing outputs only:
TO_U05_CLINICAL_READINESS
TO_U08_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
FAILURE_ROUTE

ANALYSIS_RESULT_AVAILABLE
→ U12 delivery preparation
→ F7 owns Delivery Readiness inside U12

REASSESSMENT_REQUIRED
→ U08 reassessment

all actual Clinical Readiness consequences
→ U05/D03
```

This recommendation reuses existing Delivery Readiness ownership and avoids overloading READY_FOR_CLINICAL_ANALYSIS.

---

# 19. Current status

```text
BF-U05-RG02-CL-01
= OPEN / BLOCKING

Post-DDx Controlled Decision Package
= PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW

Frozen amendment
= NOT_AUTHORIZED

Runtime implementation
= NOT_AUTHORIZED

BF-U05-RG-02
= NOT_CLOSED

U05 Implementation Readiness
= NOT_READY
```


---

# 20. Independent-review remediation status

```text
BF-U05-PDX-IR-01
= REMEDIATED / TARGETED_REVIEW_PENDING

BF-U05-PDX-IR-02
= REMEDIATED / TARGETED_REVIEW_PENDING

RQ-U05-PDX-IR-03
= REMEDIATED / TARGETED_REVIEW_PENDING

RQ-U05-PDX-IR-04
= REMEDIATED / TARGETED_REVIEW_PENDING
```

Remediation summary:

```text
IR-01:
PostDdxRoutingDecision no longer emits U06/U10/U11 outcomes.
All Clinical Readiness values remain exclusively resolved by U05/D03.

IR-02:
Delivery path is now TO_U12_DELIVERY_PREPARATION.
U12/F7 creates Delivery Readiness; READY is not an entry prerequisite.

IR-03:
POST_DDX_ROUTING explicitly reuses generic DeterministicDecision,
is not D11, and is not a new Clinical State truth family.

IR-04:
REASSESSMENT_REQUIRED -> U08 requires no higher Safety/acquisition/offline path,
current F5 and U08 bindings, current Gate, and bounded/no-progress eligibility.
```
