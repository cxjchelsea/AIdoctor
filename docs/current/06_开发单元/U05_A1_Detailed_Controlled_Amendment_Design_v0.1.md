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

V1 freezes a strict non-reuse rule：

```text
PRE_READINESS C03 question candidates
= support-only ephemeral artifacts
= trace/evidence refs only
!= Pending Question
!= reusable Question Candidate Set
```

After U06/F3 Owner interpretation has produced the canonical F3 effect：

```text
pre-readiness question candidate payload
→ MUST NOT be reused by QUESTION_SELECTION_DELIVERY
```

even when the later Question mode happens to run on the same Clinical State Version.

If later D03 returns CAN_ASK_MORE：

```text
U05
→ U06 QUESTION_SELECTION_DELIVERY
→ fresh C03 invocation / fresh governed candidate evaluation
```

MODE-2 must independently validate current Gap/current version/current CapabilityBindingRef/current question policy before selection/delivery.

This rule trades extra Capability invocation for deterministic lifecycle semantics and removes cross-mode candidate ownership ambiguity.

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

After F3 commit, U05 is blocked until all dependencies required for a routable current Safety decision and a current-compatible F3 readiness input are valid.

Barrier completion does NOT require：

```text
Risk Decision version
= final Safety Gate commit version
= final authoritative Clinical State Version
```

Instead it requires dependency-validity semantics：

```text
A. the Risk Decision consumed by U04 is valid for the exact state/dependency basis U04 evaluated;

B. the U04 Safety Gate is the current committed Gate for the authoritative post-U04 state;

C. no change after the U04 decision has modified a declared Risk/Safety dependency in a way that invalidates that Gate;

D. the F3 readiness input has been current-version revalidated/reference-bound for the authoritative state that D03 will consume;

E. a current routing authorization permits U05.
```

Version advancement caused only by committing downstream derived Risk/Safety decisions does not by itself invalidate their declared upstream basis.

---

## 7.2 Barrier entry

Barrier enters when：

```text
A1 canonical F3 commit succeeded
and Clinical State Version advanced
```

Barrier state is Runtime execution control, not Clinical Truth.

The barrier records dependency refs, not a second copy of Risk/Safety/F3 truth.

---

## 7.3 Risk dependency re-establishment

After canonical F3 commit：

```text
reload authoritative Clinical State
→ determine whether the previously valid Risk Decision is invalidated by the F3 state effect
```

For A1 V1, canonical F3 Gap mutation is conservatively treated as：

```text
RISK_REEVALUATION_REQUIRED
```

unless a later independently reviewed dependency rule narrows this behavior.

Therefore the barrier normally schedules：

```text
U03
→ Risk Decision for the post-F3 clinical-state basis
```

The resulting Risk Decision must bind：

```text
risk_basis_state_version
declared risk dependency refs
RuleReleaseRef / KnowledgeReleaseRef / CapabilityBindingRef
decision_ref
validity
```

Important：

```text
a later U04 Gate commit may advance Clinical State Version
without automatically making this Risk Decision stale
```

when the U04 commit changes only downstream Safety-derived state and does not change any declared Risk dependency.

Risk invalidation is dependency-driven, not version-number-only.

If U03 FAILED：

```text
U04 first forms the governed Safety consequence
→ ordinary A1/U05 continuation prohibited
→ U14 according to existing failure boundary
```

---

## 7.4 Safety Gate re-establishment

U04 consumes a Risk Decision that is valid for its evaluation basis and forms a governed Safety Gate.

The committed Gate must bind at least：

```text
u04_gate_ref
risk_decision_ref
risk_basis_state_version
safety evaluation basis refs
capability/policy/scope/authorization refs
gate_commit_state_version
validity
```

The Gate is CURRENT when：

```text
it is the latest authoritative committed U04 Gate
and none of its declared dependencies has changed since its decision basis was validated
```

It is NOT required that：

```text
risk_basis_state_version
= gate_commit_state_version
```

merely because the Safety Gate itself is a downstream governed state commit.

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

the Scheduler proceeds to F3 current-version revalidation.

---

## 7.5 No-cycle / dependency invalidation rule

The barrier must not create：

```text
U03
→ U04 commit
→ version advanced
→ U03 again solely because U04 committed
```

Frozen A1 amendment rule：

```text
version advancement alone
!= dependency invalidation
```

Risk must be re-run only when a declared Risk dependency changed.

Safety Gate must be re-run only when a declared Safety dependency changed or no current committed Gate exists.

F3 canonical assessment must be re-run only when a true F3 invalidation dependency changed：

```text
F1 framing changed
F2 patient facts changed/corrected
explicit F3 evidence dependency changed
F3 governing semantic binding is no longer compatible for current use
prior F3 assessment failed/invalidated under governed rule
```

Changes limited to：

```text
Risk derived decision
Safety Gate derived decision
routing authorization
Runtime barrier/checkpoint state
```

do not by themselves invalidate F3.

Same：

```text
F3_CANONICAL_EFFECT_ID
```

must never be committed twice.

---

# 8. F3 current-version revalidation decision and readiness-input binding

## 8.1 Semantic owner

```text
Owner = F3
```

Runtime/Scheduler may trigger and host execution control, but cannot decide whether an older canonical F3 semantic result is still clinically/business valid for the current state.

---

## 8.2 Execution host and mode

U06 gains a third explicit F3-owned mode：

```text
MODE-1 PRE_READINESS_GAP_ASSESSMENT
MODE-2 QUESTION_SELECTION_DELIVERY
MODE-3 F3_CURRENT_VERSION_REVALIDATION
```

MODE-3 is a governed deterministic F3 Owner consequence.

By default：

```text
MODE-3 DOES NOT invoke C03
MODE-3 DOES NOT mutate canonical F3 Clinical State
MODE-3 DOES NOT select/deliver Question
MODE-3 DOES NOT enter WAITING_USER
```

---

## 8.3 Trigger

```text
POST_F3_SAFETY_BARRIER_CURRENT_GATE_READY
```

Trigger requires：

```text
current authoritative Clinical State
current committed U04 Gate = ALLOW / permitted RESTRICTED
canonical F3 source state/effect exists
F3_CANONICAL_EFFECT_ID known
barrier has no unresolved Risk/Safety dependency invalidation
```

---

## 8.4 Deterministic revalidation input

MODE-3 consumes：

```text
consultation_id
cdp_id
target authoritative Clinical State Version
current U04 Gate ref
canonical F3 state ref
F3_CANONICAL_EFFECT_ID
original F3 assessment decision ref
original C03 CapabilityBindingRef
original RuleReleaseRef / KnowledgeReleaseRef / F3 policy refs
F3 dependency fingerprint from canonical assessment
current F1 framing ref
current F2 fact basis refs
current explicit F3 evidence dependency refs
current active governance compatibility refs
```

The dependency fingerprint excludes downstream-only：

```text
Risk decision commit
Safety Gate commit
routing authorization
Runtime checkpoint/barrier state
```

unless one of those records proves that an actual upstream F3 dependency changed.

---

## 8.5 Semantic binding / release rule

Historical F3 provenance is never rewritten.

For current-version reuse, MODE-3 must prove both：

```text
A. F3 dependency basis is unchanged;
B. the historical semantic binding remains legally interpretable for current reuse
   under an approved compatibility rule.
```

If an original Capability/Rule/Knowledge binding is：

```text
WITHDRAWN
EXPIRED
incompatible
or cannot be proven compatible for current reuse
```

MODE-3 must NOT silently switch to a new binding and declare the old F3 effect current.

Instead：

```text
F3_REASSESSMENT_REQUIRED
→ no current F3 readiness input
→ Scheduler returns to A1 PRE_READINESS_GAP_ASSESSMENT
→ new active bindings are resolved explicitly
→ a new F3_CANONICAL_EFFECT_ID is produced because governing semantic binding changed
```

Historical effect remains auditable historical truth for its original basis.

---

## 8.6 Deterministic revalidation decision contract

Introduce proposed decision type：

```text
F3CurrentVersionRevalidationDecision
```

Minimum fields：

```text
revalidation_decision_id
consultation_id
cdp_id
F3_CANONICAL_EFFECT_ID
source_f3_state_ref
source_f3_decision_ref
source_clinical_state_version
target_clinical_state_version
current_u04_gate_ref
dependency_fingerprint_before
dependency_fingerprint_current
semantic_binding_compatibility_ref
outcome
reason_codes[]
policy_id = F3_CURRENT_VERSION_REVALIDATION
policy_version
rule_release_refs[]
knowledge_release_refs[]
historical_capability_binding_ref
created_at
validity
trace_refs[]
```

Allowed outcome vocabulary：

```text
REVALIDATED_CURRENT
REASSESSMENT_REQUIRED
FAILED
```

These outcomes are internal F3 revalidation outcomes：

```text
!= Clinical Readiness
!= D03 decision_status
!= new canonical F3 lifecycle state
```

---

## 8.7 Output semantics

### REVALIDATED_CURRENT

May emit one normalized F3 readiness input：

```text
readiness_input_id
source_domain = F3
source_owner = F3
input_kind = GAP_READINESS
applicability_status = PRESENT
business_signal
consultation_id
cdp_id
clinical_state_version = target current version
source_decision_ref = F3CurrentVersionRevalidationDecision ref
source_state_ref = canonical F3 state ref
current_version_revalidation_ref
u04_gate_ref = current Gate
evidence_refs[]
policy_or_rule_refs[]
produced_at
validity = CURRENT
```

This projection：

```text
DOES NOT duplicate canonical F3 state
DOES NOT rewrite historical provenance
DOES NOT create K09 StateChangeProposal
DOES NOT advance Clinical State Version
```

### REASSESSMENT_REQUIRED

```text
no current F3 readiness input
no D03
→ return to governed A1 PRE_READINESS_GAP_ASSESSMENT
```

A fresh C03 invocation uses current approved bindings.

### FAILED

```text
no current F3 readiness input
no D03
→ typed failure
→ retry/reload when explicitly safe
→ otherwise U14 eligibility
```

Failure must not be converted to：

```text
NO_ACTIVE_ONLINE_BLOCKING_GAP
CAN_ASK_MORE
READY_FOR_CLINICAL_ANALYSIS
```

---

## 8.8 Idempotency

Revalidation identity：

```text
F3_REVALIDATION_ID
=
consultation_id
+ F3_CANONICAL_EFFECT_ID
+ target Clinical State Version
+ current U04 Gate ref
+ revalidation policy version
+ semantic binding compatibility ref
```

Same replay：

```text
returns/attaches authoritative prior revalidation decision/input
→ no duplicate revalidation decision effect
→ no Clinical State mutation
```

If target Clinical State Version or Gate changes before publication：

```text
result = STALE_BEFORE_PUBLISH
→ do not publish CURRENT readiness input
→ reload authoritative state
```

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
→ Risk/Safety dependency revalidation barrier
→ current committed Safety Gate
→ F3 current-version revalidation
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
add F3_CURRENT_VERSION_REVALIDATION mode
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

F3_CURRENT_VERSION_REVALIDATION mode S_in：

```text
current U04 Gate
canonical F3 effect/state
target current Clinical State Version
dependency fingerprint
semantic binding compatibility refs
```

F3_CURRENT_VERSION_REVALIDATION mode S_out：

```text
REVALIDATED_CURRENT
or REASSESSMENT_REQUIRED
or FAILED
```

MODE-3 performs no Clinical State mutation.

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

Pre-readiness question candidates have V1 lifetime：

```text
support-only / trace-only
never reusable by QUESTION_SELECTION_DELIVERY
```

Therefore later Question mode always performs fresh governed candidate evaluation through its own current C03 invocation, even if Clinical State Version did not change.

F3_CURRENT_VERSION_REVALIDATION does not call C03 unless it returns REASSESSMENT_REQUIRED and Scheduler explicitly starts a new PRE_READINESS assessment.

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

## 14.5 F3 current-version revalidation decision

Add proposed：

```text
F3CurrentVersionRevalidationDecision
F3_REVALIDATION_ID
dependency_fingerprint_before/current
semantic_binding_compatibility_ref
historical CapabilityBindingRef / release refs
target Clinical State Version
current U04 Gate ref
outcome = REVALIDATED_CURRENT / REASSESSMENT_REQUIRED / FAILED
```

Owner：

```text
F3
```

Execution host：

```text
U06 F3_CURRENT_VERSION_REVALIDATION
```

This decision is deterministic and creates no Clinical State mutation when only reference-binding is performed.

## 14.6 Current-version F3 readiness-input record

Add/clarify：

```text
current_version_revalidation_ref
source_state_ref
source_decision_ref = F3CurrentVersionRevalidationDecision ref
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
→ U03 Risk when declared Risk dependencies require reevaluation
→ U04 current Gate from a valid Risk/Safety evaluation basis
→ U06 F3_CURRENT_VERSION_REVALIDATION
→ deterministic F3 revalidation decision
→ current-version F3 readiness input
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

Under selected A1 architecture, this profile is no longer an executable initial D03 invocation path.

Amend to：

```text
when A1 is active and bootstrap F3 has not completed/current-version revalidated:

U04 routing / U05 inbound admission
→ U05 ordinary readiness evaluation is not eligible
→ D03 is not invoked
→ no D03 decision_id exists
→ no D03 decision_status exists
→ route remains in A1 pre-readiness / Safety-barrier / F3-revalidation path.

after A1 completes and a current-version compatible F3 input exists:
U05 admission may succeed
→ D03 evaluates ordinary precedence using current F3 signal.
```

The exact inbound-admission enforcement belongs to U05-RDP-01 when that contract is frozen; RDP-02 only preserves the rule that pre-D03 non-entry does not create a D03 runtime status.

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
A1-E17 U04 Gate commit version advance alone does not force U03 rerun
A1-E18 declared Risk dependency change does force U03 rerun
A1-E19 F3 revalidation unchanged dependencies -> REVALIDATED_CURRENT with no state mutation
A1-E20 F3 dependency/binding incompatibility -> REASSESSMENT_REQUIRED, no D03
A1-E21 F3 revalidation failure -> no readiness input / governed failure route
A1-E22 pre-readiness C03 question candidates are never reused by Question mode
A1-E23 A1 bootstrap incomplete -> no D03 object/status exists
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
= REVISED / READY_FOR_TARGETED_INDEPENDENT_DESIGN_REVIEW

Exact Frozen-Artifact Diff Inventory
= REVISED PAIRED ARTIFACT

Frozen amendment authorization
= NOT_GRANTED

Implementation authorization
= NOT_GRANTED
```


---

# 23. Independent-review remediation status

```text
BF-U05-A1-IR-01
= REMEDIATED / TARGETED_REVIEW_PENDING

BF-U05-A1-IR-02
= REMEDIATED / TARGETED_REVIEW_PENDING

BF-U05-A1-IR-03
= REMEDIATED / TARGETED_REVIEW_PENDING

RQ-U05-A1-IR-04
= REMEDIATED / TARGETED_REVIEW_PENDING
```

Remediation summary：

```text
IR-01:
Barrier now uses dependency-validity semantics.
Risk is valid for U04's evaluation basis;
U04's own downstream commit does not recursively stale Risk by version number alone.

IR-02:
F3 current-version revalidation now has:
Owner = F3
Host = U06 F3_CURRENT_VERSION_REVALIDATION
explicit trigger/input/decision/outcome/idempotency/failure/binding compatibility contract.

IR-03:
A1 bootstrap incomplete now means:
U05/D03 not invoked,
no D03 decision_id,
no D03 decision_status.
No NOT_ADMITTED/NOT_REACHED D03 vocabulary.

IR-04:
V1 freezes non-reuse:
pre-readiness C03 question candidates are support/trace-only
and are never reused by QUESTION_SELECTION_DELIVERY.
```
