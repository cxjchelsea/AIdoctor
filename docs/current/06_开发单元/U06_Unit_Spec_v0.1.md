# U06 Unit Spec v0.1

> Unit: **U06 — F3 Gap评估 / 版本重验证 / 关键问题选择与进入 WAITING_USER**  
> Design lineage: `U06 Entry Gate Assessment v0.1` / reviewed head `16f10008d256a2d5f4436148a7410ef38bfb21e2`  
> Design basis: current `main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8` + frozen/refrozen Phase 5/6/7/8/9 + U05 RDP-04  
> Scope: **UNIT DEFINITION / READINESS INPUT ONLY**  
> Status: **PASS / SUFFICIENT_FOR_INITIAL_IMPLEMENTATION_READINESS_GAP_REVIEW**  
> This document grants no implementation, merge, production, live delivery, release activation, or real-patient authorization.

---

# 1. Purpose

U06 是 V1 中唯一负责把“当前仍需获得什么信息”转化为**受治理的 F3 Gap 业务效果、当前版本 F3 重验证结果，或下一条合法 Question**的开发单元。

U06 的职责不是“让模型自由追问”，而是：

```
current governed context
+ lawful U06 admission
+ C03 candidate capability when applicable
+ F3 Owner interpretation
+ D04 stopping when applicable
→ one governed U06 consequence
```

U06 必须保持：

```
C03 Capability Result
!= canonical F3 truth
!= Question truth
!= WAITING_USER

Question SELECTED
!= Question DELIVERED_TO_USER

Question DELIVERED_TO_USER
→ only then may Consultation become WAITING_USER
```

U06 是 Wave 1 中 U05 之后、U07 之前的下一个核心开发单元。

---

# 2. Covered Business Loops

U06 直接覆盖：

```
BL-01 新问诊建立与入口澄清
BL-04 主动问诊与信息补全
```

并为下列闭环提供必要输入/状态：

```
BL-03 用户等待 / 回答 / Resume
→ U06 establishes lawful WAITING_USER entry
→ U07 owns answer/resume

BL-05 DDx/Must-Exclude/再问诊
→ later U09 may route to U06 MODE-3 or fresh F3 path

BL-09 Failure/降级
→ U06 failure may hand off to governed U14 path

BL-12 取消与过期
→ U15 may terminate a WAITING_USER state established by U06
```

U06 单元完成本身：

```
!= complete multi-turn loop
```

完整普通跨轮闭环仍需要：

```
U06 → U07 → U02
```

---

# 3. Frozen upstream/downstream ownership

## 3.1 Upstream owners

U06 不重算或覆盖：

```
F1 Subject / Problem Framing truth
U03 Clinical Risk
U04 Safety Gate
U05 Clinical Readiness
U09 ClinicalContinuationRoutingDecision
P06 Capability activation / version governance
```

U06 只消费这些 owner 的**当前、合法、可验证引用/eligibility**。

## 3.2 Downstream owners

U06 不拥有：

```
U07 Business Resume validity
U02 Clinical Fact interpretation
U03 Risk reevaluation result
U04 Safety Gate result
U05 Clinical Readiness result
U14 final failure routing outcome
U15 cancel/expiry outcome
```

## 3.3 Scheduler boundary

Scheduler 可以：

```
consume current governed eligibility/routing decision
validate currentness
create U06 target execution intent
invoke the authorized U06 mode
```

Scheduler 不可以：

```
choose F3 truth
choose Question content
reinterpret D04
switch U06 mode ad hoc
convert U06 failure into another clinical route
set WAITING_USER itself
```

---

# 4. Required Capability / Platform / Policy dependencies

Frozen Phase 7 U06 dependency row:

```
Clinical Capability:
C03

Platform:
P01
P05
P06

Deterministic Policy:
D04
```

## 4.1 C03 — Question / Information Gap Capability

C03 may provide:

```
Gap Detection
Question Candidate Generation
Question Value / Priority Assessment
Duplicate / Already-answered Filtering
Question Rendering
Decision Impact Estimation
```

and later U09-scoped extensions.

C03 does not own:

```
canonical F3 state/effect
WAITING_USER
final continue/stop decision
Clinical Readiness
```

## 4.2 P01 / G2 / K09

Any formal governed Clinical State mutation from U06 must pass:

```
Business Owner / Deterministic Decision
→ StateChangeProposal
→ P01/G2
→ CommitResult
```

C03 cannot directly create an authoritative K09 proposal.

## 4.3 P05 Trace/Audit

U06 must preserve traceable linkage across:

```
admission
mode
source eligibility/routing
C03 call when applicable
F3 owner decision
D04 decision when applicable
proposal
commit
delivery intent
delivery receipt
WAITING transition
failure handoff
```

## 4.4 P06 Binding / Version governance

Before any C03 invocation, U06 must validate the current authorized:

```
CapabilityBindingRef
scope/population/region/language/channel compatibility
contract compatibility
allowed Prompt/Model/Tool/Skill refs when applicable
RuleReleaseRef / KnowledgeReleaseRef when applicable
effective/active status
```

## 4.5 D04 — Question Stopping

D04 is first consumed by U06 and decides whether a candidate question path may continue or must stop.

D04 does not create Question truth or WAITING_USER itself.

## 4.6 Conditional P03 Model Runtime dependency

The frozen U06 dependency row remains:

```
C03
+ P01 / P05 / P06
+ D04
```

This Unit Spec does not add P03 as an unconditional U06 dependency.

However Phase 7 also freezes the cross-cutting rule:

```
if the approved C03 implementation performs a formal model call
→ that call must use the governed P03 Model Runtime / Prompt Registry path
```

Therefore:

```
C03 deterministic/tool-only implementation
→ P03 may be NOT_APPLICABLE

C03 model/mixed implementation
→ P03 becomes a conditional implementation dependency
→ PromptReleaseRef / ModelRouteRef must be approved by P06 binding
→ direct legacy/common LLM invocation remains prohibited
```

This conditional rule:

```
does not change the frozen U06 dependency row
does not authorize a model implementation
does not pre-select C03 architecture
```

The U06 Initial Implementation Readiness / Gap Review must inspect the actual proposed C03 implementation/binding before deciding P03 applicability.

---

# 5. U06 controlled mode model

U06 has exactly three governed modes in the current A1 baseline:

```
MODE-1 PRE_READINESS_GAP_ASSESSMENT
MODE-2 QUESTION_SELECTION_DELIVERY
MODE-3 F3_CURRENT_VERSION_REVALIDATION
```

A U06 execution must have one explicit mode.

Forbidden:

```
mode omitted
mode guessed from nullable fields
one execution performing MODE-1 + MODE-2 side effects
MODE-3 silently invoking C03
Scheduler changing mode after admission
```

Exact mode-admission contract is deferred to U06-RDP-01.

---

# 6. MODE-1 — PRE_READINESS_GAP_ASSESSMENT

## 6.1 Purpose

在 A1 bootstrap 中，U05/D03 之前先形成 canonical current F3 basis。

MODE-1：

```
does F3 assessment
does NOT ask the user
```

## 6.2 S_in

Frozen minimum semantic input:

```
current A1PreReadinessEligibility
eligibility_type = PRE_READINESS_A1_F3_C03_ELIGIBLE

current U04 Gate / routing authorization
current authoritative Clinical State Version
current committed facts
current F1 framing context

valid C03 CapabilityBindingRef
valid RuleReleaseRef / KnowledgeReleaseRef as applicable

no current valid A1 F3 bootstrap completion
```

For RESTRICTED Safety, exact action-specific permission must be current and preserved.

## 6.3 Trigger

Logical trigger:

```
PRE_READINESS_GAP_ASSESSMENT_REQUIRED
```

The physical event/type name is not frozen by this Unit Spec.

The execution must still bind the upstream eligibility / routing authorization identity.

## 6.4 Preconditions

At minimum:

```
eligibility is current
target_unit = U06
mode = MODE-1
U04 Gate allows the action
restricted permission is valid when applicable
Clinical State basis is current
C03 binding is valid and authorized
required releases/contract versions are compatible
no equivalent canonical F3 effect already requires no new effect
```

## 6.5 Action / Decision

```
current governed context
→ C03 Gap Detection / Decision Impact
→ structured C03 result
→ U06/F3 Owner interpretation
→ canonical F3 intended effect
→ K09 StateChangeProposal
→ P01/G2 commit
```

The Owner interpretation must distinguish at least the frozen Gap semantics necessary to support later readiness/question decisions.

It must not convert:

```
C03 NO_RESULT
C03 INSUFFICIENT_INFORMATION
C03 DEPENDENCY_FAILURE
C03 TIMEOUT
C03 INVALID_OUTPUT
```

into invented canonical F3 truth.

## 6.6 Canonical effect identity

Current Phase 8 freezes:

```
F3_CANONICAL_EFFECT_ID
```

with minimum semantic derivation:

```
consultation_id
+ fact/framing basis identity
+ source Clinical State Version
+ C03 CapabilityBindingRef
+ F3 assessment policy/version
+ assessment trigger/event identity
```

MODE-1 must use this governed identity semantics rather than random retry identity.

## 6.7 S_out

On successful authoritative commit:

```
canonical F3 effect/state committed
+ F3_CANONICAL_EFFECT_ID
+ commit evidence
+ RISK_REEVALUATION_REQUIRED
→ POST_F3_SAFETY_REVALIDATION_BARRIER
→ U03
→ U04
→ U06 MODE-3
```

MODE-1 does **not** route directly to U05 after F3 commit.

## 6.8 Forbidden side effects

MODE-1 must never produce:

```
Question SELECTED
Question DELIVERED_TO_USER
external question delivery
Consultation WAITING_USER
Thread AWAITING_USER
U07 resume eligibility
```

C03 question candidate material generated during MODE-1 is:

```
support/trace-only ephemeral material
```

and must never be reused by MODE-2.

---

# 7. MODE-2 — QUESTION_SELECTION_DELIVERY

## 7.1 Purpose

将一个**合法的澄清需求或可在线询问且具有决策价值的 F3 Gap**转化为一条受治理、已实际交付给用户的下一问题，并在交付成功后建立等待态。

## 7.2 Legal Question Need classes

U06 MODE-2 supports two semantic classes:

### A. F1 Clarification Requirement

```
Subject / Problem Framing requires minimal clarification
```

This path must remain minimal and must not silently expand into general clinical interrogation.

### B. F3 Information Gap

```
current F3 actionable online gap
+ sufficient expected decision value
```

This path may be associated with U05:

```
Clinical Readiness = CAN_ASK_MORE
→ TO_U06_QUESTION_PATH
```

U05 also freezes:

```
Clinical Readiness = NEEDS_CLARIFICATION
→ TO_U06_QUESTION_PATH
```

The exact legal admission source for every F1 clarification variant is not fixed here. In particular, this Unit Spec does not invent whether every F1 clarification must first traverse U05 or whether a separately governed direct pre-readiness clarification handoff is legal.

That question must be frozen in U06-RDP-01.

## 7.3 S_in

Minimum semantic input:

```
one lawful current Question Need
+ current authoritative Clinical State Version/context
+ current U04 Safety/permission context
+ governed routing/eligibility permitting U06 MODE-2
+ current C03 CapabilityBindingRef
+ current Question Policy / D04 policy binding
```

For an F3 path, the relevant Gap must be current and not already answered/resolved/invalidated/superseded.

For an F1 path, the clarification requirement must remain current and minimal.

## 7.4 Trigger

Logical trigger:

```
QUESTION_REQUIRED
```

with explicit mode:

```
QUESTION_SELECTION_DELIVERY
```

## 7.5 Candidate generation rule

MODE-2 must perform:

```
fresh governed C03 invocation / fresh candidate evaluation
```

It must not reuse:

```
MODE-1 pre-readiness question candidate material
```

even when the Clinical State Version happens to be the same.

## 7.6 D04 stopping / convergence

Before selecting a question, U06 must prove that continuing to ask can reasonably affect at least one:

```
Safety
DDx Candidate / ordering
Must-Exclude
Clinical Readiness
Delivery eligibility
material decision uncertainty
```

If not, D04 must stop the question path.

Forbidden:

```
unfilled field exists
→ therefore ask

more information might be nice
→ therefore ask

same unknown/unmeasured value
→ mechanical repeated question
```

The exact maximum turn/time/cost budget remains outside this Unit Spec unless already governed by later RDP/policy.

## 7.7 Question selection

A legal selected question must bind at least the existing K06 semantics:

```
question_id
question_purpose
source_requirement_ref
candidate/rendered content reference
expected_decision_value
target_concepts
clinical_state_version
capability_binding_ref
question_policy_ref
status
```

Lifecycle remains:

```
PROPOSED
→ SELECTED
→ DELIVERED_TO_USER
→ ANSWER_RECEIVED / EXPIRED / SUPERSEDED
```

Exactly one current next question may be selected for one authoritative U06 question effect.

## 7.8 Delivery boundary

Frozen Runtime ordering:

```
Question SELECTED commit
→ durable delivery intent
→ stable delivery_id / idempotency
→ transport send
→ durable receipt
→ delivery confirmation
→ governed commit:
   Question DELIVERED_TO_USER
   + Consultation WAITING_USER
→ checkpoint
```

Thread transition:

```
Thread → AWAITING_USER
```

must correspond to the same delivered Pending Question/runtime wait boundary.

Exact physical atomicity between Clinical State and Runtime Thread storage is an implementation/RDP concern; semantic truth must nevertheless never expose:

```
WAITING_USER
without authoritative delivered Question
```

## 7.9 Gap state relationship

Existing Phase 5/8 semantics require relevant Gap states such as:

```
QUESTIONABLE_ONLINE
ASKED
ANSWERED
USER_UNKNOWN
UNMEASURED
WAIVED
RESOLVED
INVALIDATED
```

This Unit Spec freezes only the semantic invariant:

```
a Gap cannot be treated as ASKED/current-question basis
without a corresponding governed question effect
```

The exact commit grouping/timing of:

```
Gap -> ASKED
Question -> SELECTED
Question -> DELIVERED_TO_USER
WAITING_USER
```

must be frozen by U06-RDP-03/RDP-04 rather than invented here.

## 7.10 S_out

Success:

```
Question = DELIVERED_TO_USER
+ current Pending Question ref
+ source requirement/gap ref
+ bound Clinical State Version/context
+ Consultation = WAITING_USER
+ Thread = AWAITING_USER
→ U07
```

No-question / no-progress:

```
D04 STOP
or no lawful valuable candidate
→ no Question delivery
→ no WAITING_USER
→ governed reevaluation/no-progress consequence
```

The exact no-progress return contract must be frozen by U06 RDP design; U06 must not invent a Clinical Readiness value itself.

---

# 8. MODE-3 — F3_CURRENT_VERSION_REVALIDATION

## 8.1 Purpose

在 canonical F3 已存在、但 authoritative Clinical State Version / Safety basis 已推进后，确定旧 canonical F3 effect 是否可以**受治理地投影为当前版本 readiness input**。

MODE-3 is:

```
deterministic owner revalidation
not reassessment by default
```

## 8.2 S_in

Frozen minimum semantic input:

```
current U04 Gate
canonical F3 state/effect
F3_CANONICAL_EFFECT_ID
source F3 decision/state refs
source Clinical State Version
target authoritative Clinical State Version
dependency fingerprint before/current
historical CapabilityBindingRef
RuleReleaseRef / KnowledgeReleaseRef
semantic binding compatibility evidence
current routing authorization
```

## 8.3 Trigger

Frozen runtime trigger semantic:

```
POST_F3_SAFETY_BARRIER_CURRENT_GATE_READY
```

or a governed continuation consequence:

```
TO_F3_CURRENT_VERSION_REVALIDATION
```

depending on the caller context.

## 8.4 Action / Decision

MODE-3 forms:

```
F3CurrentVersionRevalidationDecision
```

Allowed outcomes only:

```
REVALIDATED_CURRENT
REASSESSMENT_REQUIRED
FAILED
```

MODE-3 is not:

```
D11
Clinical Readiness
canonical F3 lifecycle mutation
```

## 8.5 Default forbidden actions

By default MODE-3:

```
C03 = NOT_INVOKED
Clinical State mutation = NONE
Question selection = NONE
Question delivery = NONE
WAITING_USER transition = NONE
```

## 8.6 S_out

### REVALIDATED_CURRENT

```
materialize current-version F3 readiness input
→ no new canonical F3 commit
→ no Clinical State Version advance from the projection itself
→ continue governed routing/readiness path
```

The projection must preserve the Phase 8 contract:

```
source_domain = F3
source_owner = F3
input_kind = ONLINE_INFORMATION_GAP
applicability_status = PRESENT
source_decision_ref = revalidation decision
source_state_ref = canonical F3 state
current_version_revalidation_ref
u04_gate_ref
validity = CURRENT
```

### REASSESSMENT_REQUIRED

```
no current F3 readiness projection
no U05/D03
→ Scheduler starts fresh MODE-1
→ current bindings must be resolved again
```

### FAILED

```
no current F3 readiness projection
no U05/D03
→ governed retry/reload or U14 eligibility
```

U06 itself does not choose U14 final failure outcome.

## 8.7 Revalidation identity / replay

Frozen minimum:

```
F3_REVALIDATION_ID
=
consultation_id
+ F3_CANONICAL_EFFECT_ID
+ target Clinical State Version
+ current U04 Gate ref
+ revalidation policy version
+ semantic binding compatibility ref
```

Exact replay:

```
→ attach authoritative prior revalidation decision/input
→ no duplicate canonical F3 effect
```

If target version/Gate changes before publication:

```
STALE_BEFORE_PUBLISH
→ do not publish CURRENT readiness input
→ reload authoritative state
```

---

# 9. StateChangeProposal / mutation boundary by mode

U06 must explicitly separate:

```
Candidate
Decision / Owner interpretation
StateChangeProposal
Commit
External delivery side effect
Runtime state
Non-mutating projection
```

These are not interchangeable.

## 9.1 MODE-1 mutation boundary

MODE-1 may form a governed proposal for the canonical F3 business effect only after:

```
C03 structured result
→ U06/F3 Owner interpretation
```

Minimum Phase-8 binding remains:

```
source_clinical_state_version
F3_CANONICAL_EFFECT_ID
source C03 result ref
C03 CapabilityBindingRef
RuleReleaseRef / KnowledgeReleaseRef as applicable
F3 policy/version
effect idempotency key
expected current version
trace/audit refs
```

Then:

```
U06/F3 Owner
→ K09 StateChangeProposal
→ P01/G2
→ CommitResult
```

MODE-1 may not use a Question candidate as a shortcut to mutate Question/WAITING state.

## 9.2 MODE-2 mutation and side-effect boundary

MODE-2 contains multiple distinct semantic stages.

### A. Question selection state

A governed authoritative Question selection may require:

```
candidate
→ D04 / U06 owner decision
→ Question SELECTED proposal
→ P01/G2 commit
```

The exact physical proposal shape and whether related Gap updates share or separate the same proposal are deferred to U06-RDP-03.

### B. External delivery side effect

After authoritative selection:

```
durable delivery intent
→ stable delivery identity
→ transport
→ durable receipt/reconciliation
```

This is an external/runtime side-effect process.

It is not itself a Clinical StateChangeProposal.

### C. Delivered/wait business state

Only after delivery is authoritatively confirmed may U06 cause governed business-state mutation such as:

```
Question DELIVERED_TO_USER
Consultation WAITING_USER
relevant Pending Question state
```

through the approved P01/G2 lifecycle boundary.

The corresponding:

```
Thread AWAITING_USER
checkpoint
```

belong to Runtime state and are not Clinical State truth.

The exact cross-store sequencing/atomicity protocol is deferred to U06-RDP-03/RDP-04, but the semantic invariant is already frozen:

```
no authoritative delivered Question
→ no WAITING_USER
```

## 9.3 MODE-3 non-mutation boundary

MODE-3 forms:

```
F3CurrentVersionRevalidationDecision
```

When outcome is:

```
REVALIDATED_CURRENT
```

U06 may materialize the frozen current-version F3 readiness input projection.

That projection is explicitly:

```
NOT a StateChangeProposal
NOT a canonical F3 mutation
NOT a Clinical State Version advance
```

Therefore MODE-3 default mutation profile is:

```
Clinical StateChangeProposal = NONE
canonical F3 commit = NONE
Question mutation = NONE
external delivery = NONE
Runtime WAIT transition = NONE
```

If outcome is:

```
REASSESSMENT_REQUIRED
```

the next fresh MODE-1 execution owns any later canonical F3 mutation.

If outcome is:

```
FAILED
```

no U06 Clinical State mutation may be fabricated to represent the failure.

## 9.4 Cross-mode mutation prohibition

Forbidden:

```
MODE-1
→ Question/WAITING mutation

MODE-2
→ silently overwrite canonical F3 assessment as if it were MODE-1

MODE-3
→ StateChangeProposal for revalidation projection

Runtime checkpoint
→ treated as Clinical State commit

delivery receipt
→ treated as Question DELIVERED truth without governed commit
```

---

# 10. State ownership matrix

| Object / state | Owner | U06 authority |
|---|---|---|
| F1 Subject / Problem Framing | F1/U01 owner | consume only |
| F3 canonical Gap semantics | F3 owner hosted by U06 | interpret and propose governed effect |
| C03 Capability Result | C03 | consume as candidate/evidence only |
| D04 stopping decision | D04 | invoke/consume; not override |
| Clinical Risk | U03/F4/D09 | consume only |
| Safety Gate | U04/G4/D02 | consume only |
| Clinical Readiness | U05/D03 | consume only |
| Question candidate | C03 candidate layer | consume/filter |
| authoritative Question lifecycle | U06 business owner + G2 governed state | own within frozen boundary |
| Pending Question | U06 business effect / governed state | establish after governed selection/delivery semantics |
| Consultation WAITING_USER | lifecycle governed state | may cause only after delivered Question |
| Thread AWAITING_USER | Runtime | request/coordinate corresponding runtime wait; not Clinical Truth |
| Business Resume decision | U07 | no authority |
| Runtime Resume | P02/Runtime | no authority |
| final failure route | U14/D07 | no authority |

---

# 11. Clinical State vs Runtime State

U06 must preserve:

```
Clinical State
!= Runtime State
!= Trace
```

Clinical/governed state includes or references:

```
information_gaps
questions / pending question refs
canonical F3 effects
Consultation lifecycle
```

Runtime state includes:

```
Thread
Run
Checkpoint
AWAITING_USER execution state
delivery reconciliation metadata
retry/repair state
```

Trace includes references to what occurred but is not business truth.

A Runtime checkpoint may reference authoritative Clinical State; it must not become a second F3/Question truth store.

---

# 12. Failure policy

U06 failure is not a business-negative shortcut.

Must preserve:

```
NO_RESULT
!= DEPENDENCY_FAILURE

INSUFFICIENT_INFORMATION
!= INVALID_OUTPUT

UNSUPPORTED
!= TIMEOUT

delivery failure
!= Question DELIVERED_TO_USER

delivery failure
!= WAITING_USER

revalidation FAILED
!= REASSESSMENT_REQUIRED
```

Failure classes include at least:

```
A. invalid/stale/unauthorized U06 admission
B. Safety/permission no longer permits action
C. CapabilityBindingRef/release/contract incompatibility
D. C03 execution/business failure
E. F3 owner interpretation cannot form lawful effect
F. K09/P01 proposal/commit failure or conflict
G. D04 policy/stopping failure
H. delivery intent/send/receipt/reconciliation failure
I. stale-before-commit / stale-before-publish
J. Runtime/checkpoint/recovery failure
```

For any failure:

```
no invented F3 truth
no invented question
no silent alternate clinical route
no WAITING_USER without delivery
```

The final retry/repair/degraded-safe-exit/terminal decision remains governed by U14/D07 where applicable.

---

# 13. Idempotency / replay / concurrency expectations

## 13.1 MODE-1

Same authoritative assessment identity:

```
same F3_CANONICAL_EFFECT_ID
+ semantically identical canonical payload
→ attach/reuse authoritative prior effect
→ no second canonical F3 commit
```

Changed semantic payload under same protected identity:

```
→ replay conflict / fail closed
```

Only downstream Risk/Safety/routing/checkpoint changes:

```
→ must not create a second canonical F3 effect
```

## 13.2 MODE-2

Question effect and delivery must use stable identities.

At minimum, retry/recovery must not cause:

```
duplicate selected authoritative question
duplicate external delivery
duplicate WAITING_USER transition
multiple current Pending Questions for one single-question effect
```

Crash windows that must be reconcilable:

```
after selection commit / before delivery intent
after delivery intent / before send
after send / before durable receipt
after receipt / before DELIVERED_TO_USER commit
after WAITING commit / before checkpoint
```

The exact Question effect identity and delivery identity derivation are deferred to RDP-03/RDP-04.

## 13.3 MODE-3

Same F3_REVALIDATION_ID replay:

```
→ same authoritative decision/projection
→ no C03 call
→ no canonical F3 mutation
```

## 13.4 Concurrency

Same Consultation:

```
one authoritative Clinical State writer path at a time
```

On commit conflict:

```
reload authoritative state
→ reconcile prior intended/effect identity
→ re-check admission/currentness/binding
→ only then form a new proposal if still lawful
```

Forbidden:

```
blind baseVersion rewrite
blind retry of stale proposal
```

---

# 14. Observability requirements

Every U06 execution must be able to trace at least:

```
consultation_id
cdp_id
clinical_state_version
unit_id = U06
u06_mode
business_event / trigger identity
routing / eligibility ref
current_u04_gate_ref
restricted_context_ref when applicable

C03 capability_call_id when invoked
CapabilityBindingRef
KnowledgeReleaseRef / RuleReleaseRef when applicable
PromptReleaseRef / ModelRouteRef when applicable

F3_CANONICAL_EFFECT_ID when applicable
F3 revalidation decision/ref when applicable
D04 decision/ref when applicable
question_id when applicable
source_requirement_ref / gap_ref
proposal_id
commit_result_ref
delivery_id / receipt ref when applicable
thread_id / checkpoint_id when applicable
failure_ref
trace_id / correlation_id
```

Trace/Audit should store refs/digests/typed summaries rather than unrestricted PHI payload.

---

# 15. Acceptance cases — Unit-level minimum

These are Unit Spec acceptance scenarios, not the final RDP-06 evidence IDs.

## 15.1 MODE-1

```
U06-US-001
valid A1 pre-readiness eligibility
+ valid C03 binding
→ C03 invoked
→ F3 Owner forms canonical effect
→ P01 commit
→ barrier continuation
→ no Question/no WAITING

U06-US-002
MODE-1 exact replay
→ same canonical effect
→ no duplicate commit

U06-US-003
MODE-1 C03 DEPENDENCY_FAILURE/TIMEOUT/INVALID_OUTPUT
→ no invented canonical F3
→ governed failure

U06-US-004
MODE-1 stale Gate/eligibility/binding
→ no C03 invocation or no commit as appropriate
→ fail closed

U06-US-005
MODE-1 candidate material exists
→ later MODE-2 cannot reuse it
```

## 15.2 MODE-2

```
U06-US-006
lawful F1 clarification need
→ minimal clarification question
→ successful delivery
→ WAITING_USER / AWAITING_USER

U06-US-007
current actionable F3 gap
+ sufficient decision value
→ fresh C03 candidate
→ D04 continue
→ exactly one Question SELECTED
→ delivered
→ WAITING_USER

U06-US-008
already answered/resolved/invalidated gap
→ not selected again

U06-US-009
USER_UNKNOWN / UNMEASURED
→ not coerced to NO/NORMAL
→ no mechanical duplicate question

U06-US-010
no candidate has sufficient decision value
→ D04 stop
→ no delivery
→ no WAITING_USER
→ governed no-progress/reevaluation

U06-US-011
Question selected but delivery fails
→ no DELIVERED_TO_USER
→ no WAITING_USER

U06-US-012
delivery retry after prior successful transport
→ reconcile stable delivery identity/receipt
→ no duplicate external send
→ one authoritative delivered/wait effect

U06-US-013
Safety/permission becomes stale before delivery
→ no ordinary delivery
→ no WAITING_USER

U06-US-014
two concurrent U06 question attempts
→ at most one authoritative current next-question effect
```

## 15.3 MODE-3

```
U06-US-015
compatible canonical F3 + current Gate
→ REVALIDATED_CURRENT
→ current F3 readiness input
→ no C03/no mutation

U06-US-016
historical binding/release incompatible
→ REASSESSMENT_REQUIRED
→ no current projection
→ fresh MODE-1 required

U06-US-017
revalidation cannot be safely resolved
→ FAILED
→ no U05/D03
→ governed failure path

U06-US-018
exact revalidation replay
→ reattach authoritative prior decision/input
→ no duplicate effect

U06-US-019
target state/Gate changes before projection publish
→ STALE_BEFORE_PUBLISH
→ no CURRENT F3 readiness input
```

---

# 16. Regression cases

U06 verification must prove no regression to already integrated units/foundation.

At minimum:

```
REG-U06-01
U05 readiness ownership remains unique;
U06 never emits Clinical Readiness.

REG-U06-02
U04 Safety Gate preempts U06 ordinary action when no longer permitted.

REG-U06-03
U03 Risk/Safety barrier after MODE-1 remains enforced.

REG-U06-04
U02 remains the only path that interprets future user answer into Clinical Facts.

REG-U06-05
P01/G2 remains the only governed Clinical State mutation path.

REG-U06-06
C03 cannot write CDP/Clinical State directly.

REG-U06-07
Question capability does not own WAITING_USER.

REG-U06-08
U07 is not bypassed by treating delivery as resume.

REG-U06-09
U14 owns final failure routing; U06 failure does not become SAFE_EXIT/FAILED_TERMINAL by itself.

REG-U06-10
Legacy fixed five-step workflow / Dialog private state is not restored as authoritative control/truth.
```

---

# 17. Legacy Asset Mapping

Current frozen legacy disposition:

```
Dialog question/gap assets
= REFACTOR

Question Planner
= ADAPT / REFACTOR

Question Renderer
= ADAPT / REFACTOR

Dialog direct CDP write
= REMOVE

Dialog private truth state
= REPLACE BY governed state inputs

DiagnosisWorkflowOrchestrator fixed five-step scheduling
= REPLACE incrementally

AgentLoop
= DO NOT PROMOTE to open clinical control

StateCommitter
= REUSE_FOUNDATION + ADAPT

Execution Trace
= REUSE_FOUNDATION + ADAPT
```

A legacy asset may be reused only if it preserves this Unit Spec's owner, state, binding, replay and Safety boundaries.

---

# 18. Out of Scope

This U06 Unit Spec does not authorize or define:

```
actual Java/Python class names
HTTP/RPC endpoints
database tables
queue/topic selection
specific model/provider
specific Prompt content
specific medical Question Pack content
final clinical C03 EvalSet cases/content
production activation
live user delivery
real-patient traffic
U07 Resume implementation
U08/U09 DDx implementation
U10/U11/U12 implementation
U14/U15 complete implementation
```

It also does not freeze:

```
exact maximum question count
exact time/cost budget
exact transport provider
exact transaction technology for Clinical State + Runtime wait
exact F1 direct-vs-U05 admission route
exact Gap ASKED commit timing
exact Question/delivery effect ID formula
exact failure reason-code enumeration
```

Those belong to U06 RDP and subsequent implementation design/review.

---

# 19. Required downstream RDP design questions

This Unit Spec intentionally leaves the following for the Initial Implementation Readiness / Gap Review and U06 RDP package.

## RDP-01 Consumer Inbound / Admission

Must decide/freeze:

```
all legal mode-specific inbound objects
MODE-2 F1 clarification admission topology
exact U05 eligibility consumption rules
A1PreReadinessEligibility consumption
ClinicalContinuationRoutingDecision consumption
currentness / Safety / RESTRICTED permission checks
mode identity / admission identity / replay
```

## RDP-02 F3 Owner / D04 Policy

Must decide/freeze:

```
canonical F3 business signal vocabulary required by U06
F1 minimal-clarification constraints
F3 decision-value rules
D04 stop/continue precedence
exactly-one selection
no-progress semantics
C03 status -> owner interpretation boundaries
```

No new clinical content may be invented by engineering where medical/product authority is required.

## RDP-03 State Ownership / Mutation / Trace

Must decide/freeze:

```
canonical F3 state/effect schema integration
Question effect identity
Question lifecycle proposal/commit boundaries
Gap state mutation timing
Pending Question ownership
WAITING_USER Clinical State mutation
Thread AWAITING_USER coordination
replay/conflict semantics
trace/provenance equality
```

## RDP-04 Delivery / Downstream / Side-effect

Must decide/freeze:

```
delivery intent identity
external-send idempotency
receipt/reconciliation
crash windows
delivery failure
WAITING transition ordering
U07 handoff eligibility
no-progress handoff
failure handoff to U14
no live side effects in non-production verification
```

## RDP-05 Capability / Dependency / Applicability

Must decide/freeze:

```
C03 binding requirements per mode
D04 release/policy requirements
P06 Question Policy binding
Rule/Knowledge/Prompt/Model refs
RESTRICTED action permission
binding unavailable/expired/incompatible semantics
C03 business_status handling
C03 Quality Gate applicability
```

## RDP-06 Verification / Durable Evidence

Must freeze exact executable case matrix, oracle independence, synthetic fixtures, effect counts, exact-head CI, durable evidence and review gates.

---

# 20. Initial Readiness inputs established by this Unit Spec

After this Unit Spec passes design review, the next formal gate should consume:

```
1. this U06 Unit Spec
2. frozen Phase 5 BL-01/BL-04 semantics
3. Phase 6 U06 + A1 mode split
4. Phase 7 C03/P01/P05/P06/D04 dependencies
5. Phase 8 K06/K09/K10 + A1 F3 contracts
6. Phase 9 Scheduler/Delivery/Barrier runtime semantics
7. U05 RDP-04 TO_U06_QUESTION_PATH boundary
8. current main repository archaeology
```

and produce:

```
U06 Initial Implementation Readiness / Gap Review
→ explicit blockers
→ dependency gap inventory
→ U06-RDP-01..06 concrete design tasks
```

---

# 21. Unit completion definition

U06 cannot be called complete merely because C03 returns a question.

A future authorized implementation may be considered U06-verified only when evidence proves all applicable modes and boundaries, including:

```
legal mode admission
currentness/Safety/binding checks
F3 owner semantics
D04 stopping
governed state mutation
stable replay/idempotency
exactly-one current question effect
crash-safe delivery
DELIVERED before WAITING
no duplicate delivery
MODE-1 no-question boundary
MODE-3 no-C03/default-no-mutation boundary
failure/no-progress handling
trace/provenance
regression against U01-U05/Foundation
```

Even then:

```
U06 verified
!= Slice A complete
!= U07 verified
!= Production authorized
```

---

# 22. Design verdict

At this stage:

```
U06 Unit Spec v0.1
= PASS / SUFFICIENT_FOR_INITIAL_IMPLEMENTATION_READINESS_GAP_REVIEW

U06 Business-Semantic Baseline
= AVAILABLE / A1 REFROZEN V1

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED

U06 Implementation
= NOT_STARTED
```

If this Unit Spec passes independent design review, the next permitted step is:

```
U06 Initial Implementation Readiness / Gap Review
```

No code implementation is authorized by this document.


---

# 23. Independent Design Review Remediation

Initial Independent Design Review:

```
PR #228
review_id = 5287035179
verdict = REVISE_REQUIRED
```

Findings:

```
BF-U06-US-IR-01
= MODE_SPECIFIC_STATE_CHANGE_PROPOSAL_BOUNDARY_NOT_EXPLICIT

RQ-U06-US-IR-02
= CONDITIONAL_P03_MODEL_RUNTIME_DEPENDENCY_MUST_BE_EXPLICIT
```

Remediation:

1. added a dedicated mode-specific StateChangeProposal/mutation section separating:
   - MODE-1 canonical F3 proposal/commit;
   - MODE-2 Question selection, external delivery, delivered/wait governed state and Runtime wait;
   - MODE-3 non-mutating readiness projection;

2. explicitly froze:

```
MODE-3 REVALIDATED_CURRENT
→ NO StateChangeProposal
→ NO canonical F3 mutation
→ NO Clinical State Version advance
```

3. added P03 as a **conditional implementation dependency only** if the approved C03 implementation actually performs formal model calls;

4. preserved the frozen U06 dependency row and did not authorize any model/runtime implementation.

Current:

```
BF-U06-US-IR-01
= CLOSED

RQ-U06-US-IR-02
= SATISFIED

Targeted Independent Design Re-Review
= PASS
review_id = 5287044358
reviewed_head = 1b3ac2573f0bf6529c1fc88b7707d78e5d7eb361
```


---

# 24. Final Design Review Provenance

```
Initial Independent Design Review
= REVISE_REQUIRED
review_id = 5287035179

Targeted Independent Design Re-Review
= PASS
review_id = 5287044358

Reviewed semantic head
= 1b3ac2573f0bf6529c1fc88b7707d78e5d7eb361

BF-U06-US-IR-01
= CLOSED

RQ-U06-US-IR-02
= SATISFIED

U06 Unit Spec v0.1
= PASS / SUFFICIENT_FOR_INITIAL_IMPLEMENTATION_READINESS_GAP_REVIEW
```

This status synchronization changes no Unit semantic, contract, capability, state ownership, routing, failure, verification or authorization rule.

Next permitted formal review:

```
U06 Initial Implementation Readiness / Gap Review
```

Still:

```
U06 Implementation Readiness
= NOT_READY / NOT_YET_EVALUATED_AGAINST_THE_APPROVED_UNIT_SPEC

U06 Implementation Authorization
= NOT_GRANTED

U06 code implementation
= NOT_STARTED
```
