# U05 Post-Analysis Routing Extension Decision Package v0.1

> New blocker discovered by full RG-02 closure sweep:
>
> ```text
> BF-U05-RG02-CL-02
> = POST_OFFLINE_NOT_NEEDED_ROUTING_GAP
> = OPEN / BLOCKING
> ```
>
> Baseline:
>
> ```text
> post-DDx routing re-frozen exact head
> = 5f3ccf7879b118695b5d376251bed45baf2f3a1c
> ```
>
> Status:
>
> ```text
> PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW
> ```

# 1. Gap

F6 owns:

```text
Offline Evidence Need:
IDENTIFIED / JUSTIFIED / NOT_NEEDED / SUPERSEDED / INVALIDATED
```

RDP-05 allows:

```text
POST_OFFLINE_ASSESSMENT
F6 = PRESENT expected
```

and:

```text
NO_BLOCKING_OFFLINE_EVIDENCE_NEED
```

But frozen U10 currently has only:

```text
VALID + JUSTIFIED -> U11
FAILED -> U14
```

No governed consequence exists for:

```text
F6 VALID + NOT_NEEDED
```

or equivalent current:

```text
NO_BLOCKING_OFFLINE_EVIDENCE_NEED
```

Therefore a lawful post-offline state can be stranded.

# 2. Architecture decision

Generalize:

```text
PostDdxRoutingDecision
```

to:

```text
PostAnalysisRoutingDecision
```

with allowed evaluation contexts:

```text
POST_DDX_REEVALUATION
POST_OFFLINE_ASSESSMENT
```

This is the same scoped deterministic routing family, not a second router.

# 3. Owner boundary

Preserve:

```text
U05/D03 -> Clinical Readiness
F3 -> Gap
F5 -> DDx/Must-Exclude
F6 -> Offline Evidence
F7 -> Delivery Readiness
```

PostAnalysisRoutingDecision owns none of those truths.

It only chooses the next Unit consequence from current governed owner outputs.

# 4. Allowed routing vocabulary

Exactly:

```text
TO_U05_CLINICAL_READINESS
TO_F3_CURRENT_VERSION_REVALIDATION
TO_U08_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
FAILURE_ROUTE
```

No U06/U10/U11 direct Clinical Readiness result duplication.

# 5. POST_DDX behavior

Retain the already reviewed semantics:

```text
Clinical Readiness path exists
→ TO_U05_CLINICAL_READINESS

F5 REASSESSMENT_REQUIRED + no higher path
→ TO_U08_REASSESSMENT

F5 ANALYSIS_RESULT_AVAILABLE + no gap/offline blocker
→ TO_U12_DELIVERY_PREPARATION
```

# 6. POST_OFFLINE behavior

U10 S_out is extended:

```text
F6 VALID + JUSTIFIED
→ U11 Safe Exit

F6 VALID + NOT_NEEDED
→ PostAnalysisRoutingDecision
   evaluation_context = POST_OFFLINE_ASSESSMENT

F6 FAILED
→ U14
```

For POST_OFFLINE_ASSESSMENT:

```text
if current owner outputs expose a Clinical Readiness path
→ TO_U05_CLINICAL_READINESS

else if:
F3 = ABSENT_BY_DESIGN
and canonical F3 source state/effect exists
and current context now requires an F3 artifact to decide delivery/reanalysis
→ TO_F3_CURRENT_VERSION_REVALIDATION

else if F5 = REASSESSMENT_REQUIRED
→ TO_U08_REASSESSMENT

else if:
F5 = ANALYSIS_RESULT_AVAILABLE
+ F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
+ F6 = NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ Safety permits
+ U12 preparation prerequisites current
→ TO_U12_DELIVERY_PREPARATION

else
→ POLICY_EXPECTATION_GAP / design blocker
```

# 7. Important semantic guard

```text
F6 NOT_NEEDED
!= Delivery Readiness READY
!= Consultation complete
```

It only removes one offline-evidence blocker.

Normal delivery still requires U12/F7 validation.

# 8. F3 ABSENT_BY_DESIGN

RDP-05 already permits in POST_OFFLINE_ASSESSMENT:

```text
F3 = ABSENT_BY_DESIGN
```

only if F3 was lawfully activated previously and the prior context required no current F3 input artifact.

For V1, ABSENT_BY_DESIGN is never sufficient evidence for normal-delivery preparation.

If the current post-offline route now needs to determine whether an online blocking Gap remains, and:

```text
canonical F3 source state/effect exists
F3 readiness input = ABSENT_BY_DESIGN
```

the only lawful consequence is:

```text
TO_F3_CURRENT_VERSION_REVALIDATION
→ U06 MODE-3 F3_CURRENT_VERSION_REVALIDATION
```

MODE-3 applies the already frozen F3 revalidation contract:

```text
REVALIDATED_CURRENT
→ current F3 readiness input is materialized
→ re-enter PostAnalysisRoutingDecision

REASSESSMENT_REQUIRED
→ U06 MODE-1 / fresh governed F3 assessment
→ re-enter routing after canonical/current F3 exists

FAILED
→ FAILURE_ROUTE / governed failure handling
```

Only:

```text
F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
```

may satisfy the F3 guard for:

```text
TO_U12_DELIVERY_PREPARATION
```

Therefore:

```text
ABSENT_BY_DESIGN
!= no active gap
!= delivery evidence
```

# 9. Contract rename/compatibility

Phase-8 decision type becomes:

```text
POST_ANALYSIS_ROUTING
```

Previous:

```text
POST_DDX_ROUTING
```

is superseded by the generalized V1 contract before runtime implementation.

No runtime compatibility migration is required because implementation is not yet authorized.

Decision contract MUST include:

```text
evaluation_context
```

and the idempotency identity MUST be:

```text
POST_ANALYSIS_ROUTING_ID
=
consultation_id
+ input Clinical State Version
+ evaluation_context
+ accepted F3/F5/F6 refs
+ current U04 Gate ref
+ routing policy version
```

Therefore POST_DDX_REEVALUATION and POST_OFFLINE_ASSESSMENT decisions cannot collide even on the same Consultation/version.

# 10. Unit changes

U09 title/scope is broadened from DDx-only routing coordinator to:

```text
Post-Analysis Gap / Routing Coordinator
```

It may host routing for:

```text
POST_DDX_REEVALUATION
POST_OFFLINE_ASSESSMENT
```

U10 adds:

```text
VALID + NOT_NEEDED
→ U09 POST_OFFLINE_ROUTING
```

U09 still does not own F6 truth.

# 11. Verification

```text
PA-E01 F6 VALID+JUSTIFIED -> U11 unchanged
PA-E02 F6 FAILED -> U14 unchanged
PA-E03 F6 VALID+NOT_NEEDED -> PostAnalysisRoutingDecision
PA-E04 post-offline F3 ABSENT_BY_DESIGN -> TO_F3_CURRENT_VERSION_REVALIDATION
PA-E05 REVALIDATED_CURRENT -> re-enter router with F3 PRESENT/current
PA-E06 post-offline F5 ANALYSIS_RESULT_AVAILABLE + current F3 no-gap + no blockers -> U12 preparation
PA-E07 F6 NOT_NEEDED alone never implies delivery ready
PA-E08 post-offline CAN_ASK_MORE -> U05/D03
PA-E09 post-offline F5 REASSESSMENT_REQUIRED -> U08 if no higher path
PA-E10 F3 ABSENT_BY_DESIGN never routes directly to delivery
PA-E11 POST_DDX behavior unchanged
PA-E12 routing idempotency identity differs by evaluation_context
```

# 12. Impact inventory

Affected frozen artifacts remain:

```text
Phase 5
Phase 6
Phase 8
Phase 9
U05-RDP-02
U05-RDP-05
```

No Phase 4 owner change.
No U04 change.
No new Clinical Readiness value.

# 13. Current status

```text
BF-U05-RG02-CL-01
= REMEDIATED / REFROZEN / CLOSURE_REEVALUATION_PENDING

BF-U05-RG02-CL-02
= OPEN / BLOCKING

BF-U05-RG-02
= NOT_CLOSED

U05 Implementation Readiness
= NOT_READY
```


# 14. Independent-review remediation status

```text
BF-U05-PA-IR-01
= REMEDIATED / TARGETED_REVIEW_PENDING

RQ-U05-PA-IR-02
= REMEDIATED / TARGETED_REVIEW_PENDING
```

Remediation:

```text
IR-01:
F3 ABSENT_BY_DESIGN now routes only to TO_F3_CURRENT_VERSION_REVALIDATION
when current routing requires a materialized F3 input.
No direct delivery preparation is allowed until F3 = PRESENT/current.

IR-02:
evaluation_context is mandatory in the decision contract and POST_ANALYSIS_ROUTING_ID.
```
