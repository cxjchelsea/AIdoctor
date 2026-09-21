# U05 Post-DDx Continuation / Delivery Handoff Controlled Decision Package v0.1

> Trigger blocker:
>
> ```text
> BF-U05-RG02-CL-01
> = POST_DDX_NORMAL_PROGRESS_POLICY_COVERAGE_GAP
> = OPEN / BLOCKING
> ```
>
> Design baseline:
>
> ```text
> U05-RDP-02 READY-policy re-frozen exact head
> = 8efd6a4c17d07752900e90026e5e1496d0c9f001
> ```
>
> Status:
>
> ```text
> CONTROLLED_DECISION_PACKAGE
> = PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW
>
> Owner decision
> = NOT_YET_GRANTED
>
> Frozen amendment
> = NOT_AUTHORIZED
>
> Runtime implementation
> = NOT_AUTHORIZED
> ```

---

# 1. Problem to solve

After U08/DDx and U09 Gap re-evaluation, the frozen flow legally sends post-DDx inputs toward the next governed route.

RDP-05 allows F5 inputs:

```text
ANALYSIS_RESULT_AVAILABLE
NO_RELIABLE_DIRECTION
NEEDS_OFFLINE_EVIDENCE
REASSESSMENT_REQUIRED
```

Current D03 covers:

```text
OUT_OF_SCOPE
NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
FIRST-ENTRY READY_FOR_CLINICAL_ANALYSIS
NO_RELIABLE_DIRECTION
```

but has no unique result for at least:

```text
POST_DDX_REEVALUATION
+ F5 ANALYSIS_RESULT_AVAILABLE
+ F3 NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher blocker
```

or:

```text
POST_DDX_REEVALUATION
+ F5 REASSESSMENT_REQUIRED
+ no higher blocker
```

The first profile is a **delivery-handoff problem**.

The second profile is a **clinical re-analysis readiness problem**.

They must not be collapsed into one value.

---

# 2. Frozen ownership constraints

Preserve:

```text
D03 / U05
= Clinical Readiness unique resolver

F7
= Delivery Package + Delivery Readiness semantic Owner

Delivery Readiness values
= NOT_READY / READY / BLOCKED / DELIVERED

Runtime / Scheduler
!= Clinical Truth Owner
!= Delivery Readiness Owner

C03/C04/C06
!= final business state Owner
```

Also preserve:

```text
D03-POL-005
= FROZEN_EXECUTABLE_EXPECTATION
= FIRST_CLINICAL_ANALYSIS_ENTRY_ONLY
```

Therefore post-DDx normal delivery cannot reuse D03-POL-005.

---

# 3. Rejected architecture alternatives

## ALT-A — add READY_FOR_DELIVERY to Clinical Readiness

Rejected.

Reason:

```text
Delivery readiness already has a separate governed system-level state
and F7 is its frozen Owner.
```

Adding a delivery value to Clinical Readiness would create overlapping owners and collapse two governance planes.

## ALT-B — reuse READY_FOR_CLINICAL_ANALYSIS for normal delivery

Rejected.

Reason:

```text
READY_FOR_CLINICAL_ANALYSIS
routes U05 -> U08

D03-POL-005
is first-entry-only
```

Using it post-DDx for delivery would either loop back into U08 or violate the frozen owner decision.

## ALT-C — let Scheduler infer U12 directly

Rejected.

Reason:

```text
Runtime/Scheduler
cannot invent a business routing semantic from raw F3/F5 combinations.
```

## ALT-D — selected architecture

```text
Split-plane post-DDx continuation:
- Clinical continuation/reanalysis remains governed by U05/D03.
- Normal delivery handoff is a typed eligibility into U12/F7 Delivery Readiness evaluation.
- A deterministic post-DDx continuation policy at U09 produces that handoff eligibility.
```

Candidate policy identifier:

```text
D11
= POST_DDX_CONTINUATION_POLICY
```

D11 is proposed by this package and is NOT frozen until separately authorized/amended/reviewed.

---

# 4. D11 responsibility

D11 is a deterministic routing policy hosted by U09 after current post-DDx F3/F5 inputs have been formed.

D11 does NOT own:

```text
Clinical Readiness
Delivery Readiness
DDx truth
Gap truth
Safety Gate
Consultation terminal state
```

It only decides which governed resolver/Unit is eligible to act next.

Allowed D11 outcomes:

```text
HANDOFF_TO_U05_READINESS
NORMAL_DELIVERY_EVALUATION_ELIGIBLE
POST_DDX_REANALYSIS_READINESS_REQUIRED
FAILURE_OR_CONFLICT
```

These are routing/continuation outcomes:

```text
!= Clinical Readiness
!= Delivery Readiness
!= D03 decision_status
```

---

# 5. D11 admission

D11 may run only when:

```text
evaluation_context = POST_DDX_REEVALUATION
Consultation = ACTIVE
current Clinical State Version is authoritative
current Safety Gate permits ordinary/restricted continuation
U09 post-DDx reevaluation inputs are current
F5 is PRESENT
F3 is PRESENT when required by RDP-05
no input identity/version conflict
```

If Safety is BLOCKED/UNAVAILABLE:

```text
D11 ordinary continuation = not invoked
existing Safety path wins
```

---

# 6. D11 policy matrix

## D11-POL-001 — readiness blocker/continuation still exists

If any current higher-priority Clinical Readiness signal exists:

```text
OUT_OF_SCOPE
qualified blocking NEEDS_OFFLINE_EVIDENCE
lawful NEEDS_CLARIFICATION
F3 CAN_ASK_MORE
F5 NO_RELIABLE_DIRECTION
```

then:

```text
D11
→ HANDOFF_TO_U05_READINESS
→ U05 / D03
```

D03 continues to produce the formal Clinical Readiness.

D11 does not duplicate D03 precedence.

## D11-POL-002 — post-DDx reanalysis

If:

```text
F5 = REASSESSMENT_REQUIRED
+ no higher-priority Clinical Readiness blocker
+ current Risk/Safety permits analysis
```

then:

```text
D11
→ POST_DDX_REANALYSIS_READINESS_REQUIRED
→ U05
```

U05/D03 must use a separate post-DDx reanalysis rule:

```text
D03-POL-011 [PROPOSED]
policy_scope = POST_DDX_REANALYSIS_ONLY

F5 = REASSESSMENT_REQUIRED
+ no higher blocker
+ current valid inputs
→ READY_FOR_CLINICAL_ANALYSIS
→ U08
```

This does NOT modify D03-POL-005.

The semantic meaning remains:

```text
READY_FOR_CLINICAL_ANALYSIS
= lawful entry/re-entry to U08/F5 clinical analysis
```

Only the policy scope differs.

## D11-POL-003 — normal delivery evaluation handoff

If:

```text
F5 = ANALYSIS_RESULT_AVAILABLE
+ underlying DDx Assessment = VALID
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no qualified blocking offline evidence need
+ no OUT_OF_SCOPE
+ no clarification need
+ critical Must-Exclude disposition satisfies current normal-delivery entry contract
+ current Risk/Safety permits normal delivery evaluation
+ all required inputs current/valid
```

then:

```text
D11
→ NORMAL_DELIVERY_EVALUATION_ELIGIBLE
→ Scheduler may invoke U12
```

This outcome means only:

```text
U12/F7 is allowed to evaluate/build/validate a normal Delivery Package
```

It does NOT mean:

```text
Delivery Readiness = READY
Delivery is safe
Delivery succeeded
Consultation = COMPLETED
```

U12/F7 still performs:

```text
Delivery Package assembly
→ C06 rendering as applicable
→ D06 validation
→ F7 interpretation
→ Delivery Readiness NOT_READY / READY / BLOCKED / DELIVERED
```

## D11-POL-004 — conflict/failure

If required post-DDx inputs are:

```text
FAILED
UNAVAILABLE
STALE
identity/version mismatched
mutually exclusive
```

D11 produces no ordinary continuation eligibility.

Use governed failure/conflict handling.

---

# 7. Typed eligibility contract

Proposed:

```text
PostDdxContinuationDecision
```

minimum fields:

```text
decision_id
policy_id = D11
policy_version
consultation_id
cdp_id
clinical_state_version
evaluation_context = POST_DDX_REEVALUATION
current_u04_gate_ref
f3_readiness_input_ref
f5_readiness_input_ref
f6_readiness_input_ref?
ddx_state_ref
must_exclude_state_refs[]
risk_ref
outcome
reason_codes[]
evidence_refs[]
rule_or_policy_refs[]
created_at
validity
trace_refs[]
```

For normal delivery handoff, additionally emit:

```text
NormalDeliveryEvaluationEligibility
```

with:

```text
eligibility_id
source_decision_ref = D11 decision ref
consultation_id
cdp_id
clinical_state_version
u04_gate_ref
ddx_state_ref
f3_input_ref
f5_input_ref
must_exclude_basis_refs[]
risk_ref
restricted_context_ref?
validity = CURRENT
trace_refs[]
```

This eligibility:

```text
!= Delivery Readiness
!= DeliveryValidationResult
!= Clinical State terminal decision
```

---

# 8. U12 admission after D11

U12 normal-delivery entry becomes:

```text
Consultation = ACTIVE
+ NORMAL_DELIVERY_EVALUATION_ELIGIBLE = CURRENT
+ current Safety Gate permits normal delivery evaluation
+ current Clinical State Version
+ Risk basis valid
+ DDx Assessment = VALID
+ Must-Exclude entry prerequisites satisfied
+ no blocking online/offline Gap
```

Then U12/F7 owns actual delivery validation and Delivery Readiness.

If U12/F7 validation fails:

```text
Delivery Readiness = BLOCKED
or typed delivery failure
→ U14 / governed repair
```

It must not fall back to D03 READY.

---

# 9. REASSESSMENT_REQUIRED semantics

This package does not invent clinical content for F5.

It freezes only the routing meaning of the already-allowed RDP-05 signal:

```text
F5 = REASSESSMENT_REQUIRED
= current post-DDx state requires a new governed U08/F5 analysis cycle
  before normal delivery may be evaluated
```

It is NOT:

```text
CAN_ASK_MORE
NEEDS_OFFLINE_EVIDENCE
NO_RELIABLE_DIRECTION
Delivery Readiness
```

Higher-priority D03 signals still preempt it.

---

# 10. Resulting post-DDx flow

```text
U08 DDx
→ U09 F3/F5 post-DDx reevaluation
→ Safety check/current Gate
→ D11 Post-DDx Continuation Policy

D11:
  blocker/online/offline/no-direction
  → U05/D03
      → U06 / U10 / U11 as frozen

  F5 REASSESSMENT_REQUIRED
  → U05/D03-POL-011
      → READY_FOR_CLINICAL_ANALYSIS
      → U08

  F5 ANALYSIS_RESULT_AVAILABLE
  + no blocking Gap
  + delivery-entry prerequisites
  → NORMAL_DELIVERY_EVALUATION_ELIGIBLE
  → U12
      → F7 Delivery Package
      → D06 validation
      → F7 Delivery Readiness
      → DELIVERED / BLOCKED
```

---

# 11. Ownership after the change

```text
F3
= Gap truth

F5
= DDx/Must-Exclude truth + F5 readiness input semantics

D03/U05
= Clinical Readiness

D11/U09
= post-DDx continuation/handoff eligibility only

F7/U12
= Delivery Package + Delivery Readiness

Scheduler
= executes only committed/current eligibility/decision refs
```

No duplicate Owner is introduced.

---

# 12. Why D11 is preferable to expanding D03

D03 answers:

```text
What clinical acquisition/analysis state is the consultation in?
```

D11 answers:

```text
After DDx reevaluation, which governed plane must act next?
```

F7 answers:

```text
May this current result actually be delivered?
```

Keeping these separate prevents:

```text
Clinical Readiness = Delivery Readiness
```

and avoids a seventh Clinical Readiness value.

---

# 13. Proposed frozen-artifact impact inventory

If Owner approves this architecture, controlled amendment is expected in:

```text
1. Phase 4 state ownership
   - clarify F7 delivery plane handoff
   - clarify F5 REASSESSMENT_REQUIRED routing meaning if needed

2. Phase 5 business loops
   - post-DDx split: Clinical Readiness vs Delivery handoff
   - BL-05 -> BL-10 handoff semantics

3. Phase 6 Units
   - U09 hosts D11
   - U09 no longer says every post-DDx path is uniquely routed by U05
   - U12 S_in requires current delivery-evaluation eligibility

4. Phase 7 capability/policy design
   - add D11 deterministic policy
   - first consumer U09
   - no AI capability

5. Phase 8 contracts
   - PostDdxContinuationDecision
   - NormalDeliveryEvaluationEligibility

6. Phase 9 Runtime
   - Scheduler consumes D11/current eligibility
   - Runtime cannot infer handoff

7. U05-RDP-02
   - add D03-POL-011 POST_DDX_REANALYSIS_ONLY
   - narrow D03 policy coverage to actual D03-routed post-DDx cases

8. U05-RDP-05
   - clarify F5 REASSESSMENT_REQUIRED and ANALYSIS_RESULT_AVAILABLE handling
   - post-DDx applicability/handoff

9. Phase 3/top-level system state doc only if its invariant literally requires every ordinary path
   to be determined by Clinical Readiness rather than governed Clinical + Delivery planes.
   Exact impact must be checked before amendment authorization.
```

No implementation is authorized by this inventory.

---

# 14. Closure criteria for BF-U05-RG02-CL-01

The blocker may close only when:

```text
A. ANALYSIS_RESULT_AVAILABLE normal-progress profile
   has one governed route to U12/F7 delivery evaluation;

B. REASSESSMENT_REQUIRED profile
   has one governed route to U08 reanalysis without abusing D03-POL-005;

C. D03 no longer receives legally admitted profiles for which it has no policy;

D. U09/U05/U12 ownership is non-overlapping;

E. Delivery Readiness remains F7-owned;

F. all affected frozen artifacts are amended, independently reviewed, and re-frozen.
```

---

# 15. Decision request

Proposed owner decision:

```text
OD-U05-POSTDDX-01
```

Options:

```text
APPROVE
= approve split-plane architecture with D11 + D03-POL-011 + F7 delivery handoff.

REVISE
= retain blocker open and revise the architecture.

REJECT
= reject this approach; blocker remains open pending another governed design.
```

Current:

```text
OD-U05-POSTDDX-01
= NOT_DECIDED

BF-U05-RG02-CL-01
= OPEN / BLOCKING

BF-U05-RG-02
= NOT_CLOSED

U05 Implementation Readiness
= NOT_READY
```
