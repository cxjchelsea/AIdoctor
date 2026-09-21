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

else if F5 = REASSESSMENT_REQUIRED
→ TO_U08_REASSESSMENT

else if:
F5 = ANALYSIS_RESULT_AVAILABLE
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP or lawful ABSENT_BY_DESIGN
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

only if F3 was lawfully activated previously and current context requires no F3 input artifact.

For normal-delivery preparation this may be accepted only when a frozen reference/basis proves:

```text
no current online blocking Gap obligation remains
```

It must not be inferred from missing F3 data.

If that proof is absent:

```text
TO_U05_CLINICAL_READINESS or failure/reevaluation
```

must occur instead of delivery preparation.

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

Decision identity includes:

```text
evaluation_context
```

so post-DDx and post-offline decisions cannot collide.

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
PA-E04 post-offline F5 ANALYSIS_RESULT_AVAILABLE + no blockers -> U12 preparation
PA-E05 F6 NOT_NEEDED alone never implies delivery ready
PA-E06 post-offline CAN_ASK_MORE -> U05/D03
PA-E07 post-offline F5 REASSESSMENT_REQUIRED -> U08 if no higher path
PA-E08 F3 ABSENT_BY_DESIGN without proof -> no delivery preparation
PA-E09 POST_DDX behavior unchanged
PA-E10 decision identity differs by evaluation_context
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
