# U05 A1 Detailed Controlled Amendment Design v0.1

> Decision basis:
>
> ```text
> OD-U05-BOOTSTRAP-01 = A1
> ```
>
> Owner-selection-ready baseline:
>
> ```text
> PR #127 exact head
> = 2d9c2f3c98085e6938441919f5a74fab5e94cc71
>
> main
> = 6e68fd9fb7cd19e87aadae30f3bb53a2264d1920
> ```
>
> Status:
>
> ```text
> DETAILED_CONTROLLED_AMENDMENT_DESIGN
> = PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW
>
> Frozen artifact amendment
> = NOT_AUTHORIZED
>
> Runtime implementation
> = NOT_AUTHORIZED
> ```

---

# 1. Purpose

本文件把已由 Owner 选择的 A1 架构：

```text
U04 current Safety Gate
→ PRE_READINESS_A1_F3_C03_ELIGIBLE
→ U06 PRE_READINESS_GAP_ASSESSMENT
→ C03
→ canonical F3 Gap commit
→ POST_F3_SAFETY_REVALIDATION_BARRIER
→ current U04 Gate + current-version F3 readiness input
→ U05 / D03
```

细化为可独立审查、可生成 exact frozen-artifact amendment diff 的设计。

本文件不直接修改任何 Frozen artifact。

---

# 2. Scope

A1 正式 amendment scope 至少包括：

```text
A01  Phase 4 / F3 Owner + activation timing
A02  U04-RDP-04 Downstream Routing Boundary
A03  Phase 5 Business Loop
A04  Phase 6 / U06 Unit boundary
A05  Phase 7 / C03 usage timing and U06 capability contract
A06  Phase 8 / canonical F3 proposal + commit + readiness-input binding
A07  Phase 9 / Scheduler + POST_F3_SAFETY_REVALIDATION_BARRIER
A08  U05-RDP-05 readiness input applicability/version contract
A09  U05-RDP-02 D03 bootstrap policy contract
```

不在本轮 scope：

```text
D03 positive READY policy approval
OD-U05-READY-01
U05 implementation
production Clinical Runtime
live routing
real-patient traffic
new clinical truth
new C03 medical semantics
```

---

# 3. Non-negotiable invariants

必须保持：

```text
Clinical Truth
!= Model Output
!= Capability Result
!= Runtime State
!= Trace
```

以及：

```text
Capability Result
→ Business Owner interpretation
→ K09 StateChangeProposal when state mutation is required
→ G2/P01 validation + commit
→ authoritative Clinical State
```

A1 不改变：

```text
F3 = Gap / Question semantic Owner
G2/U05/D03 = unique Clinical Readiness Resolver
U04 = Safety Gate Owner
C03 = Capability, not business truth owner
Scheduler = execution coordinator, not clinical truth owner
```

禁止：

```text
U05 directly invokes C03
C03 directly commits Clinical State
C03 output directly becomes Clinical Readiness
U06 pre-readiness mode selects/delivers Question
U06 pre-readiness mode enters WAITING_USER
old U04 Gate reused after F3 state commit
version advancement alone retriggers same canonical F3 effect
```

---

# 4. A1 end-to-end runtime design

## 4.1 Initial current state

A1 ordinary bootstrap path starts only when：

```text
Consultation = ACTIVE
current Clinical State Version = Vn
current Risk Assessment @ Vn = VALID
current U04 Safety Gate @ Vn = ALLOW
or permitted RESTRICTED
F1 current framing = PRESENT
no current-version canonical F3 bootstrap assessment exists
BootstrapArchitectureBindingRef = A1
```

Then U04 routing projection may expose：

```text
PRE_READINESS_A1_F3_C03_ELIGIBLE
```

This eligibility is not a business truth and does not itself execute U06.

---

## 4.2 Routing authorization

A current U04 result creates one governed：

```text
routing_authorization_id
```

bound to：

```text
business_event_identity
u04_gate_ref
clinical_state_version = Vn
BootstrapArchitectureBindingRef = A1
restricted_context_ref when applicable
routing_policy_version
```

For bootstrap-required A1 path：

```text
first eligible consequence
= PRE_READINESS_A1_F3_C03_ELIGIBLE
```

Scheduler consumes this authorization and invokes U06 in：

```text
PRE_READINESS_GAP_ASSESSMENT
```

mode.

---

# 5. U06 PRE_READINESS_GAP_ASSESSMENT mode

## 5.1 Mode boundary

U06 gains two explicit modes：

```text
MODE-1 PRE_READINESS_GAP_ASSESSMENT
MODE-2 QUESTION_SELECTION_DELIVERY
```

They share U06 business ownership but have different admission, allowed Capability use, side effects and completion semantics.

---

## 5.2 PRE_READINESS admission

Required：

```text
Consultation = ACTIVE
current U04 Gate permits ordinary/restricted continuation
PRE_READINESS_A1_F3_C03_ELIGIBLE = CURRENT
routing_authorization_id = CURRENT
BootstrapArchitectureBindingRef = A1
current Clinical State Version = Vn
current committed Clinical Facts @ Vn
F1 framing current
no current valid A1 F3 bootstrap completion for the same basis
valid C03 CapabilityBindingRef
valid applicable RuleReleaseRef / KnowledgeReleaseRef
```

Rejected when：

```text
Gate = BLOCKED
Gate = UNAVAILABLE
eligibility stale
routing authorization stale
Clinical State Version mismatch
C03 binding missing/invalid
required rule/knowledge binding missing/invalid
same F3_CANONICAL_EFFECT_ID already applied and still valid
```

---

## 5.3 PRE_READINESS trigger

```text
PRE_READINESS_GAP_ASSESSMENT_REQUIRED
```

Trigger identity must bind：

```text
consultation_id
clinical_state_version
routing_authorization_id
u04_gate_ref
F1 framing ref
current fact basis refs
BootstrapArchitectureBindingRef
```

---

## 5.4 C03 invocation contract

PRE_READINESS mode may invoke：

```text
C03
```

for：

```text
Gap Detection
Decision Impact Estimation
online-obtainability assessment
question-value assessment as supporting evidence
```

C03 raw output may contain question candidates, but PRE_READINESS mode must not create：

```text
Question SELECTED
Question DELIVERED_TO_USER
Pending Question
Consultation WAITING_USER
Thread AWAITING_USER
```

Question candidate material is not an authoritative Question business effect in PRE_READINESS mode.

If later D03 returns CAN_ASK_MORE：

```text
U05
→ U06 QUESTION_SELECTION_DELIVERY
```

and MODE-2 must independently validate current Gap/current version/current question policy before selection/delivery.

No automatic reuse of a stale pre-readiness question candidate is allowed.

---

## 5.5 PRE_READINESS interpreted outputs

U06 as F3 Owner interprets C03 output into canonical F3 business semantics.

Allowed canonical F3 effects include：

```text
Information Gap lifecycle records
Gap Decision Impact
online/offline obtainability
Gap validity/invalidation metadata
```

Allowed normalized F3 readiness signals include：

```text
CAN_ASK_MORE
NEEDS_OFFLINE_EVIDENCE
NO_ACTIVE_ONLINE_BLOCKING_GAP
```

But：

```text
NO_ACTIVE_ONLINE_BLOCKING_GAP
!= READY_FOR_CLINICAL_ANALYSIS
```

U06/F3 never owns final Clinical Readiness.

---

# 6. Canonical F3 state proposal and commit

## 6.1 Canonical effect identity

Each A1 canonical F3 bootstrap effect must bind：

```text
F3_CANONICAL_EFFECT_ID
=
consultation_id
+ fact/framing basis identity
+ source Clinical State Version
+ C03 CapabilityBindingRef
+ F3 assessment policy/version
+ assessment trigger/event identity
```

The identity is used for no-duplicate-effect and termination semantics.

---

## 6.2 Capability result is not state

Required transformation：

```text
C03 Capability Result
→ U06/F3 Owner interpretation
→ canonical F3 intended effect
→ K09 StateChangeProposal
→ G2/P01 validation
→ commit
```

C03 cannot write F3 Clinical State directly.

---

## 6.3 K09 proposal requirements

A1 F3 proposal must carry at least：

```text
proposal_id
consultation_id
cdp_id
source_clinical_state_version
F3_CANONICAL_EFFECT_ID
F3 owner/module identity
canonical Gap mutations
source capability result ref
C03 CapabilityBindingRef
RuleReleaseRef / KnowledgeReleaseRef as applicable
reason/evidence refs
effect idempotency key
trace/audit refs
expected current version
```

---

## 6.4 Commit semantics

If proposal is accepted：

```text
Clinical State Version Vn
→ Vn+1
```

Then：

```text
prior Risk Assessment @ Vn
prior U04 Gate @ Vn
routing_authorization_id @ Vn
PRE_READINESS_A1 eligibility @ Vn
```

must not be used for U05.

No silent version rebinding.

---

## 6.5 Commit conflict

If commit conflict occurs：

```text
reload authoritative Clinical State
→ reconcile F3_CANONICAL_EFFECT_ID
→ if effect already applied, attach authoritative effect
→ if basis changed, invalidate prior intended effect
→ re-run governed assessment only when still applicable
```

Blind stale proposal retry is prohibited.

---

# 7. POST_F3_SAFETY_REVALIDATION_BARRIER

## 7.1 Purpose

After F3 commit, U05 is blocked until：

```text
current Risk
+ current U04 Gate
+ current-version compatible F3 readiness input
```

coexist for the same authoritative current Clinical State Version.

---

## 7.2 Barrier entry

Barrier enters when：

```text
A1 canonical F3 commit succeeded
and Clinical State Version advanced
```

Barrier state is Runtime execution control, not Clinical Truth.

---

## 7.3 Risk re-establishment

Because Risk Assessment is version-bound：

```text
F3 commit
→ Clinical State Version changed
→ Risk Assessment for new version required
```

Scheduler must route：

```text
U03 @ current version
```

under existing U03 semantics.

U03 produces：

```text
Risk Assessment VALID / FAILED
```

for the current version.

If FAILED：

```text
U04 first forms governed Safety consequence
→ ordinary A1/U05 continuation prohibited
→ U14 according to existing failure boundary
```

---

## 7.4 Safety re-establishment

After current Risk input is available：

```text
U04
→ current Safety Gate
```

For：

```text
BLOCKED
UNAVAILABLE
```

ordinary A1/U05 continuation is prohibited.

For：

```text
ALLOW
permitted RESTRICTED
```

the routing projection may evaluate whether the already-applied A1 F3 bootstrap effect is current-version usable.

---

## 7.5 No-cycle rule

The barrier must not retrigger canonical F3 merely because：

```text
Vn
→ F3 commit
→ Vn+1
→ U03 risk commit/revalidation
→ Vk
→ U04 gate commit/revalidation
→ Vm
```

changed versions.

Frozen rule：

```text
Risk/Safety-only version advancement
!= F3 invalidation
```

A new F3 canonical effect is allowed only when a true F3 invalidation dependency changed：

```text
F1 framing changed
F2 patient facts changed/corrected
explicit F3 evidence dependency changed
prior F3 assessment failed/invalidated under governed rule
```

Same：

```text
F3_CANONICAL_EFFECT_ID
```

must never be committed twice.

---

# 8. Current-version F3 readiness-input binding

## 8.1 Problem

Canonical F3 may be committed at：

```text
Vn+1
```

while after barrier the authoritative current version may be：

```text
Vk
```

RDP-05 requires all D03 inputs to be current-version compatible.

---

## 8.2 Revalidation/reference binding

A current F3 readiness input may be projected only when：

```text
canonical F3 source state still valid
F3 invalidation dependencies unchanged
current Risk/Safety barrier complete
current U04 Gate routable
```

Envelope must carry：

```text
readiness_input_id
source_domain = F3
source_owner = F3
input_kind = GAP_READINESS
applicability_status = PRESENT
business_signal
consultation_id
cdp_id
clinical_state_version = Vk
source_decision_ref = F3 assessment/revalidation ref
source_state_ref = canonical F3 state ref
current_version_revalidation_ref
u04_gate_ref = current Gate @ Vk
evidence_refs[]
policy_or_rule_refs[]
produced_at
validity = CURRENT
```

This projection：

```text
DOES NOT duplicate canonical F3 state
DOES NOT rewrite historical F3 provenance
DOES NOT create new Clinical State mutation
```

unless a true F3 invalidation dependency changed.

---

# 9. U04-RDP-04 amendment design

## 9.1 Current frozen boundary

Current ordinary route：

```text
ALLOW
→ U05 eligibility
```

A1 requires controlled replacement：

```text
ALLOW / permitted RESTRICTED
→ if A1 bootstrap required and not current:
   PRE_READINESS_A1_F3_C03_ELIGIBLE

→ if A1 bootstrap already current/current-version revalidated:
   U05_ELIGIBLE
```

---

## 9.2 U04 ownership preserved

U04 still owns only：

```text
ALLOW
RESTRICTED
BLOCKED
UNAVAILABLE
```

U04 does not execute U06/C03.

Routing projection uses：

```text
current U04 Gate
+ BootstrapArchitectureBindingRef = A1
+ current A1 completion/revalidation ref
```

to expose typed eligibility.

---

## 9.3 Gate semantics

ALLOW：

```text
A1 bootstrap required
→ PRE_READINESS_A1_F3_C03_ELIGIBLE
```

A1 bootstrap current：

```text
→ U05_ELIGIBLE
```

RESTRICTED：

```text
only if governed restricted policy permits A1 pre-readiness assessment/U05 continuation
+ restricted_context_ref required
```

BLOCKED：

```text
no A1 pre-readiness
no U05
```

UNAVAILABLE：

```text
no A1 pre-readiness
no U05
```

---

## 9.4 Routing authorization

A current Gate creates one：

```text
routing_authorization_id
```

for its current version/path identity.

Before F3 commit：

```text
RA-n
→ PRE_READINESS_A1_F3_C03_ELIGIBLE
```

After F3 state mutation：

```text
RA-n = STALE
```

After barrier：

```text
new current Gate
→ RA-k
```

If same F3 canonical effect is already current/revalidated：

```text
RA-k
→ U05_ELIGIBLE
```

not another pre-readiness invocation.

---

# 10. Phase 4 amendment design

Affected artifact：

```text
docs/current/03_状态/模块级状态与状态所有权_V1.md
```

A1 must preserve：

```text
F3 owns information-gap/information-sufficiency semantics
Clinical Readiness remains G2/U05 resolver output
```

Required semantic amendment：

```text
F3 may be lawfully activated in a PRE_READINESS bootstrap context
after current Safety Gate
and before first U05 D03 evaluation
when active BootstrapArchitectureBindingRef = A1
```

Canonical F3 lifecycle does not change：

```text
IDENTIFIED
QUESTIONABLE_ONLINE
ASKED
ANSWERED
USER_UNKNOWN
UNMEASURED
OFFLINE_REQUIRED
WAIVED
RESOLVED
INVALIDATED
```

Only legal activation timing changes.

Phase 4 must also state：

```text
pre-readiness F3 assessment
→ canonical F3 state
→ may invalidate/retrigger downstream Risk/Safety/Readiness dependencies
```

without granting F3 ownership of final Clinical Readiness.

---

# 11. Phase 5 Business Loop amendment design

Affected artifact：

```text
docs/current/05_业务闭环/业务闭环设计_V1.md
```

Current initial sequence：

```text
F2 facts
→ F4 Risk
→ G4 Safety Gate
→ Clinical Readiness Resolver
→ CAN_ASK_MORE
→ F3
```

A1 bootstrap sequence becomes：

```text
F2 facts
→ F4 Risk
→ G4 current Safety Gate
→ A1 pre-readiness F3 assessment
→ canonical F3 commit
→ Risk/Safety revalidation barrier
→ current Safety Gate
→ Clinical Readiness Resolver
```

Ordinary post-D03 loop remains：

```text
NEEDS_CLARIFICATION / CAN_ASK_MORE
→ BL-04 / U06 QUESTION_SELECTION_DELIVERY
```

A1 pre-readiness mode：

```text
!= BL-04 Question Delivery
!= WAITING_USER
```

Phase 5 must explicitly distinguish：

```text
bootstrap F3 assessment
vs
post-readiness active questioning
```

---

# 12. Phase 6 / U06 amendment design

Affected artifact：

```text
docs/current/06_开发单元/可验证开发单元拆分_V1.md
```

Required changes：

```text
U04 S_out:
ALLOW / permitted RESTRICTED
→ A1 pre-readiness eligibility when A1 bootstrap required
→ otherwise U05

U05 S_in:
requires current Gate
+ current-version compatible F3 input after A1 bootstrap

U06:
add PRE_READINESS_GAP_ASSESSMENT mode
while preserving QUESTION_SELECTION_DELIVERY mode
```

PRE_READINESS mode S_in：

```text
current A1 eligibility
current Gate
current facts/framing
no current valid A1 F3 completion
```

PRE_READINESS mode S_out：

```text
canonical F3 intended effect
→ commit result
→ Safety revalidation barrier
```

Forbidden PRE_READINESS S_out：

```text
Question SELECTED
Question DELIVERED_TO_USER
WAITING_USER
AWAITING_USER
```

Question mode remains subject to existing：

```text
Clinical Readiness = CAN_ASK_MORE
or lawful F1/F2 clarification need
```

---

# 13. Phase 7 / C03 usage amendment design

Affected artifact：

```text
docs/current/07_能力设计/按开发单元的Capability设计.md
```

Preserve：

```text
C03 FIRST_CONSUMER_UNIT = U06
```

Amend usage semantics：

```text
U06 PRE_READINESS_GAP_ASSESSMENT
→ C03 Gap Detection / Decision Impact
→ no Question delivery side effect

U06 QUESTION_SELECTION_DELIVERY
→ C03 question/gap
→ D04 stopping
→ Question SELECTED
→ delivery
```

C03 capability itself does not become a Readiness Resolver.

CapabilityBindingRef must be validated separately for each invocation/effect context.

If pre-readiness C03 call succeeded but later Question mode runs on a newer Clinical State Version：

```text
old candidate output cannot be silently reused
```

without explicit current-version validation.

---

# 14. Phase 8 Contract/Data amendment design

Affected artifact：

```text
docs/current/08_契约与数据/Contract与数据语义设计.md
```

Required additions/clarifications：

## 14.1 A1 pre-readiness eligibility envelope

```text
eligibility_id
eligibility_type = PRE_READINESS_A1_F3_C03_ELIGIBLE
routing_authorization_id
consultation_id
cdp_id
clinical_state_version
u04_gate_ref
BootstrapArchitectureBindingRef = A1
restricted_context_ref?
validity
created_at
trace_refs[]
```

## 14.2 F3 canonical effect identity

```text
F3_CANONICAL_EFFECT_ID
```

must be durable/auditable.

## 14.3 F3 StateChangeProposal

Must include：

```text
source Clinical State Version
canonical effect identity
C03 result ref
CapabilityBindingRef
Rule/Knowledge refs
effect idempotency key
expected current version
```

## 14.4 Barrier/revalidation artifact

Runtime may maintain：

```text
POST_F3_SAFETY_REVALIDATION_BARRIER
```

as execution state/checkpoint metadata.

It must not be Clinical Truth.

## 14.5 Current-version F3 readiness-input revalidation record

Add/clarify：

```text
current_version_revalidation_ref
source_state_ref
source_decision_ref
bound current Clinical State Version
bound current U04 Gate ref
validity
```

No duplicate canonical state is created.

---

# 15. Phase 9 Scheduler/Runtime amendment design

Affected artifact：

```text
docs/current/09_Runtime与技术架构/Runtime与技术架构设计_V1.md
```

Current ordinary chain：

```text
Facts
→ U03
→ U04
→ U05
```

A1 chain：

```text
Facts
→ U03
→ U04
→ route projection
→ U06 PRE_READINESS_GAP_ASSESSMENT
→ C03
→ F3 K09/G2/P01 commit
→ reload authoritative Clinical State
→ POST_F3_SAFETY_REVALIDATION_BARRIER
→ U03 current Risk
→ U04 current Gate
→ F3 current-version revalidation/ref-binding
→ route projection
→ U05
```

Scheduler must route only from committed/current state.

Barrier exit conditions：

```text
current Risk established
current U04 Gate established
current-version compatible F3 readiness input established
routing authorization for current Gate exists
```

Barrier abnormal exits：

```text
Risk failure
Safety BLOCKED
Safety UNAVAILABLE
C03 failure
F3 commit failure/conflict not recoverable
revalidation failure
```

must not fall through to ordinary U05.

Replay/crash recovery must preserve：

```text
event identity
routing_authorization_id
F3_CANONICAL_EFFECT_ID
commit result
barrier stage
current Gate ref
revalidation ref
```

and must not duplicate canonical F3 effects.

---

# 16. U05-RDP-05 amendment design

Affected artifact：

```text
docs/current/06_开发单元/U05_RDP05_Readiness_Input_Dependency_Applicability_Contract_v0.1.md
```

Current frozen POST_SAFETY_INITIAL：

```text
F3 = NOT_YET_APPLICABLE
```

A1 requires replacing the A1-bound initial context semantics.

Proposed applicability states：

```text
POST_SAFETY_INITIAL / A1 bootstrap required but not started:
F3 = NOT_YET_APPLICABLE for U05 admission
and U05 ordinary resolution is not entered;
routing goes to A1 pre-readiness eligibility.

A1 pre-readiness in progress:
F3 = NOT_YET_APPLICABLE / pending bootstrap producer
U05 admission prohibited.

A1 canonical F3 committed but Safety barrier incomplete:
F3 source state exists
but readiness input = STALE/NOT_CURRENT_FOR_D03
U05 admission prohibited.

A1 barrier complete + current-version revalidation:
F3 = PRESENT / CURRENT
U05 may admit.
```

The original statement：

```text
U05 must not wait for U06/C03 before U05 can run
```

cannot remain universally true under A1.

It must be narrowed to：

```text
outside an explicitly governed A1 bootstrap path,
U05 does not invent a dependency on U06/C03.

when BootstrapArchitectureBindingRef = A1
and bootstrap F3 is required,
ordinary U05 admission occurs only after the governed pre-readiness A1 path completes.
```

Still prohibited：

```text
U05 directly calls C03
missing F3 implies READY
stale F3 is silently reused
```

---

# 17. U05-RDP-02 amendment design

Affected artifact：

```text
docs/current/06_开发单元/U05_RDP02_D03_Policy_Owner_Decision_Contract_v0.1.md
```

Current bootstrap sentinel：

```text
F1 FRAMED_IN_SCOPE
+ F3/F5/F6 NOT_YET_APPLICABLE
→ POLICY_EXPECTATION_GAP
```

Under selected A1 architecture, this profile is no longer the executable initial D03 path.

Amend to：

```text
when A1 is active and F3 bootstrap has not completed:
D03 ordinary execution = NOT_ADMITTED
because U04 routing sends the consultation to A1 pre-readiness first.

after A1 completes and current-version F3 input exists:
D03 evaluates ordinary precedence using current F3 signal.
```

D03 runtime statuses remain：

```text
DECIDED
INPUT_FAILURE
INPUT_CONFLICT
```

No new status is added.

POLICY_EXPECTATION_GAP remains a design/readiness sentinel for configurations where executable policy coverage is incomplete; it is not used as an ordinary patient runtime result.

OD-U05-READY-01 remains separately pending.

The proposed positive rule：

```text
F1 FRAMED_IN_SCOPE
+ current F3 NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher blocker
→ READY_FOR_CLINICAL_ANALYSIS
```

remains：

```text
PROPOSED
NOT FROZEN EXECUTABLE
until OD-U05-READY-01 approval
```

A1 selection does not approve it.

---

# 18. Failure model

## 18.1 C03 failure

```text
C03 failed/unavailable
→ no canonical F3 effect
→ no D03
→ typed failure
→ U14 eligibility where governed
```

No fake：

```text
NO_ACTIVE_ONLINE_BLOCKING_GAP
```

---

## 18.2 F3 interpretation/proposal failure

```text
Capability succeeded
but Owner interpretation/proposal invalid
→ no commit
→ typed failure
→ no D03
```

---

## 18.3 Commit conflict

```text
reload authoritative state
→ reconcile effect
→ no blind retry
```

---

## 18.4 Barrier Risk failure

```text
U03 FAILED
→ U04 produces governed Gate consequence
→ ordinary route prohibited
→ U14 according to existing policy
```

---

## 18.5 Barrier Safety BLOCKED/UNAVAILABLE

```text
no U05 admission
```

Existing U11/U14 ownership remains unchanged.

---

# 19. Trace / audit requirements

Trace must allow reconstruction of：

```text
which U04 Gate exposed A1 eligibility
which routing authorization invoked pre-readiness U06
which C03 binding/result was used
how U06 interpreted C03 into F3 effect
which F3_CANONICAL_EFFECT_ID was proposed/committed
which Clinical State Version was produced
why old U04 eligibility became stale
how Risk/Safety were re-established
which F3 current-version revalidation ref was used
which new routing authorization entered U05
which D03 input set was consumed
```

Trace remains evidence, not truth.

---

# 20. Evaluation / regression plan

Before any implementation authorization, amendment must define/verify at least：

```text
A1-E01 ALLOW initial path enters pre-readiness U06 before U05
A1-E02 RESTRICTED allowed path preserves restricted_context_ref
A1-E03 BLOCKED never invokes pre-readiness U06
A1-E04 UNAVAILABLE never invokes pre-readiness U06
A1-E05 pre-readiness U06 cannot enter WAITING_USER
A1-E06 C03 failure yields no F3 fake result/no D03
A1-E07 canonical F3 commit advances version and stales prior Gate/auth
A1-E08 barrier rebuilds current Risk/Gate
A1-E09 Risk/Safety-only version advancement does not duplicate F3 commit
A1-E10 true fact/framing change can invalidate/retrigger F3
A1-E11 current F3 revalidation creates no duplicate canonical Gap
A1-E12 stale F3 cannot enter D03
A1-E13 same F3_CANONICAL_EFFECT_ID replay is idempotent
A1-E14 after D03 CAN_ASK_MORE, ordinary U06 Question mode still works
A1-E15 Question mode alone may SELECT/DELIVER and enter WAITING_USER
A1-E16 OD-U05-READY-01 not approved => no executable READY expectation from D03-POL-005
```

---

# 21. Exact amendment order after authorization

If the detailed amendment later receives independent PASS and explicit amendment authorization, apply in dependency order：

```text
1. Phase 4 F3 activation timing / ownership clarification
2. U04-RDP-04 eligibility projection
3. Phase 5 business-loop sequence
4. Phase 6 U04/U05/U06 Unit contracts
5. Phase 7 C03 usage timing
6. Phase 8 eligibility/F3 proposal/revalidation contracts
7. Phase 9 Scheduler + Safety barrier
8. U05-RDP-05 applicability/version contract
9. U05-RDP-02 bootstrap admission/policy contract
10. cross-artifact consistency review
11. independent re-review all amended frozen artifacts
12. re-freeze at exact heads
```

No step may claim prior frozen PASS remains valid after material semantic amendment without re-review.

---

# 22. Review readiness

This detailed design is ready for independent design review only if the paired exact diff inventory is present.

Current status：

```text
A1 Detailed Controlled Amendment Design
= PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW

Exact Frozen-Artifact Diff Inventory
= REQUIRED PAIRED ARTIFACT

Frozen amendment authorization
= NOT_GRANTED

Implementation authorization
= NOT_GRANTED
```
