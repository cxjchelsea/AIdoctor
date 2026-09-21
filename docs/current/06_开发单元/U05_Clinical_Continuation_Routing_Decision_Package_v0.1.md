# U05 Clinical Continuation Routing Controlled Decision Package v0.1

> Trigger blocker:
>
> ```text
> BF-U05-RG02-CL-03
> = POST_USER_FACT_UPDATE_RECOMPUTATION_ROUTING_GAP
> = OPEN / BLOCKING
> ```
>
> Baseline:
>
> ```text
> 460c240927fea3797f9ff566cd72807b9dcd41bc
> ```
>
> Status:
>
> ```text
> PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW
> ```

# 1. Problem

RDP-05 freezes:

```text
POST_USER_FACT_UPDATE
F3 = NOT_YET_APPLICABLE / STALE / PRESENT
F5 = NOT_YET_APPLICABLE / STALE until recalculated
F6 = NOT_YET_APPLICABLE / STALE
```

BL-11 freezes that a valid user correction/fact update may invalidate:

```text
F3
F4
F5
F6
F7
Clinical Readiness
```

Phase 6 U13 currently says:

```text
U02/G2
→ new Clinical State Version
→ U03/U04/U05
```

but does not freeze a sequencing barrier preventing U05 from seeing normal upstream-mutation STALE inputs before their owners have recomputed/revalidated them.

# 2. Architecture decision

Generalize the already reviewed:

```text
PostAnalysisRoutingDecision
```

into:

```text
ClinicalContinuationRoutingDecision
```

This generalized router is still narrow.

Allowed evaluation contexts only:

```text
POST_USER_FACT_UPDATE
POST_DDX_REEVALUATION
POST_OFFLINE_ASSESSMENT
```

Initial A1 bootstrap is NOT part of this router and remains governed by U04/A1 pre-readiness semantics.

# 3. Allowed routing consequences

Exactly:

```text
TO_U05_CLINICAL_READINESS
TO_F3_CURRENT_VERSION_REVALIDATION
TO_U08_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
FAILURE_ROUTE
```

No new Clinical Readiness value.

# 4. POST_USER_FACT_UPDATE sequencing

After a committed user fact update:

```text
U02/G2 commit
→ U03 Risk re-evaluation as required
→ U04 current Safety Gate
→ ClinicalContinuationRoutingDecision
   evaluation_context = POST_USER_FACT_UPDATE
```

The router consumes current owner applicability/validity metadata and invalidation provenance.

## 4.1 F3 not current

If:

```text
prior canonical F3 exists
+ current F3 readiness input is STALE / not materialized due to upstream fact change
```

then:

```text
TO_F3_CURRENT_VERSION_REVALIDATION
→ U06 MODE-3
```

If no lawful prior canonical F3 exists and A1 bootstrap semantics are required, existing A1 pre-readiness path applies instead; the continuation router must not invent a bootstrap shortcut.

## 4.2 Clinical Readiness path exists

After current F3 exists, if inputs expose an actual Clinical Readiness consequence:

```text
OUT_OF_SCOPE
blocking NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
NO_RELIABLE_DIRECTION
```

then:

```text
TO_U05_CLINICAL_READINESS
```

D03 applies frozen precedence.

## 4.3 Prior F5 never activated

If:

```text
F5 = NOT_YET_APPLICABLE
+ current F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher path
```

then:

```text
TO_U05_CLINICAL_READINESS
```

so first-analysis-entry D03-POL-005 may lawfully produce READY_FOR_CLINICAL_ANALYSIS.

## 4.4 Prior F5 activated but now invalidated/stale by fact update

If:

```text
prior F5 analysis was lawfully activated
+ current F5 = STALE / INVALIDATED
+ invalidation provenance = current accepted user fact/correction update
+ current F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher Clinical Readiness path
```

then:

```text
TO_U08_REASSESSMENT
```

This is normal recomputation, not D03 INPUT_FAILURE.

## 4.5 Failed/unavailable is different

```text
STALE_BY_UPSTREAM_MUTATION
!= FAILED
!= UNAVAILABLE
```

If the source is actually FAILED / UNAVAILABLE, use failure governance rather than reassessment-by-default.

# 5. Context-specific semantics retained

POST_DDX_REEVALUATION retains:

```text
ANALYSIS_RESULT_AVAILABLE -> TO_U12_DELIVERY_PREPARATION
REASSESSMENT_REQUIRED -> TO_U08_REASSESSMENT
Clinical Readiness path -> TO_U05
```

POST_OFFLINE_ASSESSMENT retains:

```text
F6 NOT_NEEDED -> continuation routing
F3 ABSENT_BY_DESIGN when materialization is required
-> TO_F3_CURRENT_VERSION_REVALIDATION
```

# 6. Contract

Rename generalized contract:

```text
ClinicalContinuationRoutingDecision
decision_type = CLINICAL_CONTINUATION_ROUTING
```

It reuses generic DeterministicDecision and is:

```text
!= D11
!= Clinical Readiness
!= Delivery Readiness
!= Clinical State truth category
```

Mandatory fields include:

```text
evaluation_context
input_clinical_state_version
accepted F3/F5/F6 refs
invalidation_refs[]
prior_activation_refs[]
current_u04_gate_ref
decision
reason_codes[]
policy version / rule refs / trace refs
```

# 7. Idempotency

```text
CLINICAL_CONTINUATION_ROUTING_ID
=
consultation_id
+ input Clinical State Version
+ evaluation_context
+ accepted owner input refs
+ invalidation refs
+ current U04 Gate ref
+ policy version
```

# 8. Required provenance for stale-to-reassessment

TO_U08_REASSESSMENT due to F5 STALE is permitted only when:

```text
F5 prior_activation_ref exists
F5 invalidation_ref binds the current accepted fact/correction mutation
current F5 state is STALE / INVALIDATED, not FAILED
current F3 no-gap input is PRESENT/current
current U08 binding/release context is valid
Safety permits
bounded/no-progress policy permits
```

Absence of provenance cannot be treated as reassessment eligibility.

# 9. Why this does not duplicate D03

The continuation router decides only which governed owner/resolver must act next.

It does not decide:

```text
OUT_OF_SCOPE
NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
READY_FOR_CLINICAL_ANALYSIS
NO_RELIABLE_DIRECTION
```

Those remain D03 values.

# 10. Impact inventory

Affected frozen artifacts:

```text
Phase 5
Phase 6
Phase 8
Phase 9
U05-RDP-02
U05-RDP-05
```

No Phase 4 ownership change.
No U04 ownership change.
No Clinical Readiness enum change.

# 11. Verification scenarios

```text
CC-E01 pre-DDx fact update + F5 NOT_YET_APPLICABLE + current F3 no-gap
→ TO_U05 → first-entry READY policy

CC-E02 post-DDx fact update + F5 STALE with valid invalidation provenance + current F3 no-gap
→ TO_U08_REASSESSMENT

CC-E03 post-DDx fact update + F3 CAN_ASK_MORE
→ TO_U05 → CAN_ASK_MORE

CC-E04 F3 stale after fact update with canonical prior F3
→ TO_F3_CURRENT_VERSION_REVALIDATION

CC-E05 F5 FAILED
→ FAILURE_ROUTE, not reassessment

CC-E06 F5 STALE without valid mutation provenance
→ no reassessment eligibility / failure-design guard

CC-E07 POST_DDX behavior unchanged

CC-E08 POST_OFFLINE behavior unchanged

CC-E09 same version/context replay
→ same routing decision

CC-E10 contexts differ
→ routing IDs differ
```

# 12. Current status

```text
BF-U05-RG02-CL-01
= REMEDIATED / REFROZEN / CLOSURE_REEVALUATION_PENDING

BF-U05-RG02-CL-02
= REMEDIATED / REFROZEN / CLOSURE_REEVALUATION_PENDING

BF-U05-RG02-CL-03
= DESIGN_SOLUTION_IDENTIFIED / REVIEW_PENDING

BF-U05-RG-02
= NOT_CLOSED

U05 Implementation Readiness
= NOT_READY
```
