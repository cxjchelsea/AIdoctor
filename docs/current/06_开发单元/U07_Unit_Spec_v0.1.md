# U07 Unit Spec v0.1

> Unit: **U07 — 用户回答 Resume 与幂等恢复**  
> Design lineage: `U07 Entry Gate Assessment v0.1` / reviewed head `a8301e24a8cb2f5c2fe5a52886b0b768030712ed`  
> Design basis: current `main@6d4fd787600e3a57f01f3e17893e6d98893ac546` + frozen/refrozen Phase 5/6/7/8/9 + verified U06 wait/resume-eligibility boundary  
> Scope: **UNIT DEFINITION / READINESS INPUT ONLY**  
> Status: **DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document grants no implementation, merge, production, live resume, release activation, PROFILE-A, or real-patient authorization.

---

# 1. Purpose

U07 是 V1 中唯一负责把一个**合法等待态中的用户回答事件**转化为受治理的 Business Resume Decision，并在 Runtime 可恢复时完成幂等 Resume、恢复 Consultation 的普通推进资格，然后把原始用户答案交给 U02 重新形成 Clinical Facts 的开发单元。

U07 的职责不是“收到文本就继续跑 Workflow”，而是：

```text
canonical USER_ANSWER / RESUME_REQUEST
+ current authoritative waiting business state
+ current Pending Question
+ current/reattachable U07 Resume Eligibility
+ F8 Business Resume validation
+ P02 Runtime resume compatibility
+ idempotent effect reconciliation
→ one governed U07 consequence
```

U07 必须保持：

```text
Business Resume validity
!= Runtime Resume compatibility

Accepted user answer
!= Clinical Fact

Runtime checkpoint
!= Clinical Truth

Duplicate event
!= replay stale clinical truth

Resume success
!= direct U08/U05 continuation
```

普通成功路径必须保持：

```text
USER_ANSWER
→ U07 Business Resume
→ Runtime Resume / Rehydrate
→ governed resume consequences
→ Consultation ACTIVE
→ U02
```

U07 是 Wave 1 Core 中 U06 之后的最后一个核心开发单元。

---

# 2. Covered Business Loops

U07 直接覆盖：

```text
BL-03 用户等待 / 回答 / Resume
```

并闭合 U06 建立的跨轮等待入口：

```text
U06
→ Question DELIVERED_TO_USER
→ Pending Question current
→ Consultation WAITING_USER
→ Thread AWAITING_USER
→ U07 Resume Eligibility
→ U07
→ U02
```

U07 也参与：

```text
BL-04 主动问诊与信息补全
→ 用户回答后返回事实形成主干

BL-09 Failure / 降级
→ Runtime resume 不可安全恢复时进入 governed failure handling

BL-12 取消与过期
→ late answer / expired wait 必须被拒绝或判定为 EXPIRED
```

U07 单元完成本身：

```text
!= answer has become Clinical Truth
!= post-answer Safety complete
!= DDx complete
!= Slice A complete
```

成功 Resume 后仍必须经过：

```text
U02 fact interpretation / commit
→ U03 Risk
→ U04 Safety
→ governed continuation
```

---

# 3. Frozen upstream / downstream ownership

## 3.1 Upstream owners

U07 不重算或覆盖：

```text
U06 Question selection / delivery truth
U06 delivered-wait parent effect
F3 Question lifecycle business truth
Consultation authoritative lifecycle
Thread / checkpoint persistence truth
Clinical State Version truth
P06 binding/version governance
U15 cancel/expire truth
```

U07 只能消费这些 owner 的**当前、合法、可验证引用或历史有效绑定**。

## 3.2 Downstream owners

U07 不拥有：

```text
U02 Clinical Observation / Fact interpretation
U02 new Clinical State Version commit
U03 Risk result
U04 Safety Gate result
U05 Clinical Readiness result
U08 DDx result
U14 final failure route
U15 cancel/expire lifecycle outcome
```

## 3.3 Scheduler boundary

Scheduler 可以：

```text
consume committed U07 eligibility / event decision / runtime result
validate currentness
resume or create bounded Run
route ACCEPTED+APPLIED result to U02
```

Scheduler 不可以：

```text
invent ACCEPTED
reinterpret DUPLICATE/EXPIRED/REJECTED
turn checkpoint compatibility into business validity
treat raw answer as Clinical Fact
skip U02
route directly to U08/U05
convert Runtime failure into business negative result
```

---

# 4. Required Capability / Platform / Policy dependencies

Frozen Phase 7 U07 dependency row:

```text
Clinical AI Capability
= NONE

Platform
= P01
+ P02
+ P05
+ P06

Deterministic Business Owner / Policy
= Resume validation / lifecycle rules
```

U07 does not require a clinical LLM/Agent capability to decide whether an event is a lawful Resume.

## 4.1 P01 — State Governance / Clinical CDP Adapter

Any governed business-state mutation caused by U07 must remain:

```text
Business Decision
→ intended effect
→ K09 StateChangeProposal
→ P01/G2 validation
→ CommitResult
```

U07 may not write Clinical State/CDP directly.

## 4.2 P02 — Durable Clinical Resume

U07 is the first consumer where P02 becomes a **core dependency**.

P02 owns:

```text
Thread
Run
Checkpoint
interrupt/wait execution context
pending runtime refs
runtime expiry
resume compatibility
rehydration
retry / repair
crash / replay
```

P02 does not own:

```text
Business Resume ACCEPTED / DUPLICATE / EXPIRED / REJECTED
Clinical Fact interpretation
Question business lifecycle truth
Consultation lifecycle truth
```

## 4.3 P05 — Trace / Audit

U07 must preserve traceable linkage across:

```text
ingress / canonical event
U07 eligibility
Pending Question
parent wait effect
Business Resume Decision
runtime compatibility
checkpoint / rehydrate
governed state effects
APPLIED
Consultation ACTIVE
U02 handoff
failure / repair
```

## 4.4 P06 — Scope / Binding / Version Governance

Resume must preserve or validate the governance context bound to the waiting execution.

At minimum:

```text
contract version compatibility
historical CapabilityBindingRef compatibility
runtime schema compatibility
required Rule/Knowledge refs when historically bound
scope/population/region/language/channel constraints
```

U07 must not silently “upgrade to latest” when resuming an old waiting Run.

## 4.5 P03 / P04 / Clinical AI

U07 Business Resume itself has:

```text
P03 Model Runtime
= NOT_REQUIRED

P04 Knowledge/Evidence Intelligence
= NOT_REQUIRED

Clinical AI
= NONE
```

If a future implementation adds model/knowledge use inside U07, that is a semantic/dependency expansion and requires controlled redesign; it is not authorized by this Unit Spec.

---

# 5. U07 controlled execution model

U07 has one governed resume pipeline with outcome branches, not multiple clinical reasoning modes.

The controlled stages are:

```text
Stage 1
Canonical Event Resolution

Stage 2
Business Wait / Pending Question Validation

Stage 3
F8 Business Resume Decision

Stage 4
Runtime Resume Compatibility / Rehydrate

Stage 5
Governed Resume Apply / Reconciliation

Stage 6
Consultation ACTIVE + U02 Handoff

Stage 7
Failure / Repair / Terminal Handoff when needed
```

Forbidden:

```text
skip Stage 3 and resume Thread directly
use checkpoint state to invent ACCEPTED
mark APPLIED before governed consequences are recoverable
send raw answer directly to Clinical State
route to U08/U05 before U02
```

Exact physical service/class boundaries remain outside this Unit Spec.

---

# 6. S_in — lawful waiting boundary

Minimum semantic input:

```text
Consultation = WAITING_USER

Thread = AWAITING_USER
or authoritative evidence exists that the same valid wait can be reconstructed

current Pending Question exists

Question = DELIVERED_TO_USER

same delivered-wait parent effect provenance

current / reattachable U07 Resume Eligibility

Business Event
= USER_ANSWER or RESUME_REQUEST

current Consultation / Question not terminally invalidated

bound Clinical State Version / governance context references
```

The U06 upstream eligibility contract already establishes that normal eligibility is emitted only when:

```text
Question = DELIVERED_TO_USER
PendingQuestion = same current Question
Consultation = WAITING_USER
Thread = AWAITING_USER
checkpoint compatible
parent wait-effect provenance matches
Question not expired/superseded
```

However, Runtime invariant must also remain:

```text
stale/missing checkpoint alone
!= business event automatically invalid
```

Therefore U07-RDP-01/RDP-04 must distinguish:

```text
business event validity
from
runtime reconstruction / resume compatibility
```

---

# 7. Trigger / Business Event

U07 accepts only governed Business Events in its current scope:

```text
USER_ANSWER
RESUME_REQUEST
```

A Business Event must become or resolve to a canonical event identity before effects are applied.

Frozen invariant:

```text
same event_id transport replay
→ same canonical event
→ no second independent Business Resume Decision
```

This Unit Spec does not freeze the exact event ID hash/schema formula.

U07-RDP-01 must freeze:

- required inbound identity fields;
- canonical event resolution;
- transport retry mapping;
- answer payload/ref binding;
- whether same semantic payload under a new event_id is duplicate, conflict, or independent event;
- replay/currentness behavior.

---

# 8. Stage 1 — Canonical Event Resolution

Ingress must not directly execute resume side effects.

Logical flow:

```text
incoming USER_ANSWER / RESUME_REQUEST
→ canonical event resolution
→ canonical event ledger
→ RECEIVED
→ U07 validation
```

The canonical event must bind enough evidence to identify at least:

```text
consultation_id
business_event_id
event_type
question_id / pending_question_ref
parent wait effect / resume eligibility ref
answer payload ref or digest where applicable
occurred_at
trace / correlation refs
```

Exact field names are deferred to RDP-01.

Forbidden:

```text
frontend retry creates new effect identity automatically
raw transport request == authoritative Business Event
random retry ID == new clinical effect permission
```

---

# 9. Stage 2 — Business Wait / Pending Question Validation

Before F8 can decide ACCEPTED, U07 must validate business-state applicability.

At minimum:

```text
Consultation still represents the relevant waiting consultation
Pending Question is current
Question identity matches
Question was actually DELIVERED_TO_USER
parent delivered-wait provenance matches
Question is not superseded
Question / answer window is not authoritatively expired
Consultation is not cancelled/expired/terminal
event is not already APPLIED under the same canonical identity
```

Important separation:

```text
business-state mismatch
→ may cause EXPIRED / REJECTED / DUPLICATE

checkpoint missing/stale
→ Runtime problem
→ must not by itself fabricate REJECTED
```

The exact precedence among duplicate/expired/rejected conditions is deferred to U07-RDP-02.

---

# 10. Stage 3 — F8 Business Resume Decision

F8 / U07 business owner decides exactly one:

```text
ACCEPTED
DUPLICATE
EXPIRED
REJECTED
```

Business Resume lifecycle may include:

```text
RECEIVED
→ VALIDATING
→ ACCEPTED | DUPLICATE | EXPIRED | REJECTED
→ APPLIED when applicable
```

## 10.1 ACCEPTED

ACCEPTED means:

```text
the canonical user event is a lawful business resume event
against the current authoritative wait/question boundary
```

It does **not** mean:

```text
checkpoint is definitely recoverable
answer is Clinical Truth
U02 has committed facts
post-answer Safety is satisfied
```

## 10.2 DUPLICATE

DUPLICATE means:

```text
this canonical/semantically governed event has already been processed
or is equivalent to an already-applied resume under frozen duplicate rules
```

DUPLICATE must produce:

```text
zero new clinical effect
zero second resume apply
zero duplicate U02 handoff
```

Response may reference:

```text
original event processing
idempotent confirmation
current authoritative business state
```

It must not replay stale Clinical Truth as if it were current.

## 10.3 EXPIRED

EXPIRED means the event cannot lawfully resume the waiting business interaction because the relevant answer/resume window or authoritative lifecycle has expired.

EXPIRED:

```text
→ no Clinical State write from the answer
→ no Runtime resume as ordinary continuation
→ no direct U02 handoff
```

U07 does not own the lifecycle mutation that made the Consultation/Question expired if that belongs to U15 or another owner.

## 10.4 REJECTED

REJECTED covers a non-expiry business invalidity, such as wrong/incompatible business binding under the frozen decision policy.

REJECTED:

```text
→ no Runtime resume
→ no Clinical State write
→ no U02 handoff
```

## 10.5 Runtime failure must not rewrite business decision

If F8 has lawfully decided:

```text
ACCEPTED
```

and Runtime later reports checkpoint incompatibility/failure:

```text
Business Resume Decision remains ACCEPTED
Runtime Resume Result = separate failure/incompatibility
```

U07 must not retroactively rewrite ACCEPTED into REJECTED merely because P02 failed.

This separation is mandatory.

---

# 11. Stage 4 — Runtime Resume Compatibility / Rehydrate

Only an ACCEPTED Business Resume event may proceed to ordinary Runtime Resume.

P02 must validate at least:

```text
checkpoint ↔ Clinical State Version compatibility
checkpoint ↔ runtime schema compatibility
checkpoint ↔ historical binding refs compatibility
checkpoint ↔ contract compatibility
pending interaction validity
Thread / Run identity
parent wait effect / eligibility provenance
```

Runtime Result must remain separate from Business Decision.

Logical outcomes should distinguish at least:

```text
RESUMABLE / RESUMED
RECONSTRUCTION_REQUIRED
INCOMPATIBLE
FAILED
```

Exact enum names are not frozen here and belong to U07-RDP-04.

## 11.1 Missing or stale checkpoint

Frozen runtime rule:

```text
stale/missing checkpoint alone
cannot invalidate a valid business event
```

Preferred handling:

```text
reload authoritative business/clinical state
+ canonical event
+ effect ledger
+ historical bound refs
→ reconstruct / repair when safe
```

Only when safe reconstruction is impossible may Runtime produce a governed incompatibility/failure.

## 11.2 Historical bindings

Resume must not silently substitute:

```text
latest CapabilityBindingRef
latest RuleReleaseRef
latest KnowledgeReleaseRef
latest contract version
```

for a historical waiting Run if the frozen semantics require historical reproducibility.

If continuing safely requires semantic migration, that needs explicit governed handling; not an implicit U07 fallback.

---

# 12. Stage 5 — Governed Resume Apply / Reconciliation

A successful Runtime resume permits U07 to complete its intended business consequences.

At unit level, U07 must ensure that successful application can establish/reconcile:

```text
Business Resume Event → APPLIED

Consultation WAITING_USER → ACTIVE

current pending answer interaction is no longer left as an unresolved active wait

the accepted answer is durably available as the next U02 input

one exact downstream U02 handoff eligibility/effect
```

Question/F3 lifecycle effects require special ownership discipline.

## 12.1 Question lifecycle owner boundary

Frozen state ownership says:

```text
F3 owns Question business lifecycle
Thread owns wait/resume execution
```

Therefore U07 must **not seize F3 ownership** of:

```text
Question DELIVERED_TO_USER → ANSWER_RECEIVED
Gap ASKED → ANSWERED
```

Instead, U07-RDP-03 must freeze the exact governed bridge:

```text
accepted/applied answer event
→ F3-owned lifecycle transition/effect
→ P01/G2 governed commit
```

or another already-authorized owner pattern.

This Unit Spec freezes only the required outcome:

```text
after a successfully applied answer,
the old delivered Question / Pending Question cannot remain falsely current
```

without inventing the exact mutation grouping.

## 12.2 Pending Question consume/clear

A successfully applied resume must not leave the same Pending Question as an active unanswered pointer.

Exact semantics may be:

```text
clear
consume
replace with governed answered reference
```

but the exact K09 patch shape must be frozen by U07-RDP-03.

Blind REPLACE is prohibited.

## 12.3 Consultation ACTIVE

Phase 5 freezes:

```text
Runtime Resume success
→ F8 APPLIED
→ Consultation ACTIVE
→ U02
```

The exact transaction/choreography grouping between:

- APPLIED;
- Question/Gap lifecycle consequence;
- Pending Question consume;
- Consultation ACTIVE;
- Runtime state transition;

must be designed in RDP-03/RDP-04.

The system must never expose a state combination that permits two simultaneous ordinary answer applications for the same wait.

---

# 13. Stage 6 — U02 handoff

U07 does not convert the answer into Clinical Facts.

Successful path:

```text
U07 applied answer
→ exact downstream answer input / handoff
→ U02
→ F2 Clinical Observation / Fact interpretation
→ G2/P01 commit
→ new Clinical State Version
```

The handoff must preserve:

```text
business_event_id
question/pending interaction provenance
source waiting Clinical State Version
answer payload ref
U07 Business Resume Decision ref
Runtime Resume Result ref
trace / correlation refs
```

Exact physical message/object names are deferred.

Critical invariant:

```text
U07 APPLIED
!= U02 fact commit completed
```

If U02 later fails:

```text
the historical U07 event may remain truthfully ACCEPTED/APPLIED
while downstream fact-processing failure is handled separately
```

U07 must not duplicate the answer event to “make U02 succeed.”

---

# 14. State ownership matrix

| Object / state | Owner | U07 authority |
|---|---|---|
| Canonical Business Event identity | Runtime/Event Ledger | consume/resolve; not reinterpret transport retry as new truth |
| F8 Business Resume Decision | U07 business owner | own |
| Question selection / delivery | U06/F3 owner | consume only |
| Question business lifecycle | F3 owner | request/coordinate answer consequence; no ownership takeover |
| Gap business lifecycle | F3 owner | request/coordinate answer consequence when applicable |
| Pending Question | governed Clinical State | consume/clear only via reviewed U07/F3 bridge |
| Consultation WAITING_USER → ACTIVE | governed lifecycle | cause after successful resume boundary |
| Thread / Run / Checkpoint | P02 Runtime | consume/resume/reconstruct through P02 |
| Runtime Resume Result | P02 Runtime | consume; not override |
| User answer → Clinical Fact | U02/F2/G2 | no authority |
| Clinical State Version advance from answer facts | U02/P01/G2 | no authority |
| Safety/Risk/readiness/DDx | U03/U04/U05/U08 etc. | no authority |
| final failure route | U14/D07 | no authority |
| cancel/expiry lifecycle | U15 / lifecycle owner | consume only |

---

# 15. Clinical State vs Runtime State vs Event Ledger

U07 must preserve:

```text
Clinical State
!= Runtime State
!= Canonical Event Ledger
!= Trace
```

Clinical/governed state includes:

```text
Question / Gap lifecycle
Pending Question
Consultation lifecycle
Clinical State Version
```

Runtime state includes:

```text
Thread
Run
Checkpoint
AWAITING_USER / resumed execution context
repair / retry state
```

Event ledger includes:

```text
canonical business_event identity
decision refs
effect refs
processing status
```

Trace records what happened but is not authoritative business state.

---

# 16. Idempotency / replay / concurrency

## 16.1 Same canonical event replay

```text
same canonical business_event_id replay
→ attach original canonical event
→ do not create a second independent F8 decision
→ reconcile prior effects
```

If prior outcome:

```text
DUPLICATE / EXPIRED / REJECTED
→ return same authoritative decision semantics
→ 0 new effects
```

If prior outcome:

```text
ACCEPTED but not fully APPLIED
→ continue/reconcile the same intended effect
→ no second independent resume window
```

If prior outcome:

```text
APPLIED
→ exact replay / idempotent confirmation
→ no second Runtime resume
→ no second Consultation ACTIVE effect
→ no second U02 handoff
```

## 16.2 Changed payload under protected identity

If the same protected event identity arrives with semantically different answer payload/bindings:

```text
→ replay conflict / fail closed
```

Exact canonical fingerprint rules belong to RDP-01/RDP-03.

## 16.3 Competing answer events

Two different events racing for the same current Pending Question must not both produce ordinary applied resume effects.

At most:

```text
one authoritative current answer application
```

The loser must re-evaluate current state and converge to DUPLICATE / REJECTED / EXPIRED / conflict semantics defined by RDP-02.

## 16.4 No stale truth replay

A duplicate response may return:

```text
current authoritative business state
or original event-processing reference
```

It may not blindly replay:

```text
old Clinical State
old diagnosis
old recommendation
old delivery
```

as if still current.

---

# 17. Crash / recovery expectations

U07 must be designed for crash-safe recovery across at least:

```text
C1 after event canonicalization / before F8 decision

C2 after F8 ACCEPTED / before Runtime compatibility check

C3 after Runtime compatibility success / before rehydrate completion

C4 after Runtime resume / before APPLIED persistence

C5 after APPLIED / before Consultation ACTIVE effect

C6 after Consultation ACTIVE / before Pending Question reconciliation

C7 after business-state reconciliation / before U02 handoff

C8 after U02 handoff intent / before durable downstream acknowledgement
```

For every crash window, recovery must prove:

```text
same canonical event
same intended effect
no second clinical side effect
no duplicate Runtime resume
no duplicate U02 handoff
no silent rollback to WAITING when ACTIVE is already authoritative
no silent ACTIVE when business apply is incomplete
```

Exact transaction/outbox/saga/checkpoint technology is deferred to U07-RDP-03/RDP-04.

---

# 18. Failure policy

U07 failure must keep business result and Runtime result separate.

Must preserve:

```text
Business REJECTED
!= Runtime FAILED

Business EXPIRED
!= checkpoint stale

Business DUPLICATE
!= replay failure

Runtime INCOMPATIBLE
!= Business REJECTED

U02 downstream failure
!= U07 Business Resume invalid
```

Failure classes include at least:

```text
A. invalid/unauthorized inbound event
B. canonical event replay conflict
C. wrong Consultation / Question / wait-effect binding
D. expired/superseded/cancelled business state
E. business decision persistence failure
F. checkpoint missing/stale
G. checkpoint/runtime schema incompatibility
H. historical binding/contract incompatibility
I. Runtime rehydrate/resume failure
J. P01/K09 commit conflict for resume consequences
K. partial apply / reconciliation required
L. U02 handoff failure
M. trace/evidence durability failure
```

For any failure:

```text
no invented Clinical Fact
no silent latest-binding fallback
no duplicate event effect
no direct U08/U05 route
no fake ACTIVE state
```

Final retry/repair/safe-exit/terminal routing remains governed by U14/D07 where applicable.

---

# 19. Observability requirements

Every U07 execution must be able to trace at least:

```text
consultation_id
cdp_id
business_event_id
event_type
canonical_event_ref

question_id
pending_question_ref
parent_wait_effect_ref
u07_resume_eligibility_ref

source_clinical_state_version
current_consultation_lifecycle_ref

f8_business_resume_decision_ref
business_resume_status

thread_id
run_id
checkpoint_id
runtime_resume_result_ref
runtime_compatibility_status
reconstruction/repair_ref when applicable

proposal_id / commit_result_ref when applicable
question/gap lifecycle effect ref when applicable
pending_question consume/clear effect ref when applicable
consultation_active_effect_ref when applicable
u02_handoff_ref

failure_ref
trace_id
correlation_id
```

PHI minimization applies:

```text
trace prefers refs / digests / typed summaries
over unrestricted raw answer text
```

---

# 20. Unit-level acceptance cases

These are Unit Spec acceptance scenarios, not final RDP-06 evidence IDs.

## 20.1 Legal acceptance / resume

```text
U07-US-001
valid current wait boundary
+ current Pending Question
+ matching USER_ANSWER event
→ F8 ACCEPTED
→ Runtime resume compatible
→ resume succeeds
→ APPLIED
→ Consultation ACTIVE
→ exactly one U02 handoff

U07-US-002
valid RESUME_REQUEST carrying already-governed answer event reference
→ same business/resume checks
→ no duplicate answer event creation
```

## 20.2 Duplicate / replay

```text
U07-US-003
same canonical event replay before full apply
→ attach original event
→ continue same intended effect
→ no second F8 decision/effect

U07-US-004
same canonical event replay after APPLIED
→ idempotent confirmation/current state
→ no Runtime resume
→ no second U02 handoff

U07-US-005
same protected event identity + changed answer payload
→ replay conflict
→ fail closed

U07-US-006
two concurrent answer events for same Pending Question
→ at most one applied resume effect
```

## 20.3 Business invalidity

```text
U07-US-007
wrong consultation_id
→ REJECTED
→ no Runtime resume

U07-US-008
wrong question_id / pending_question_ref
→ REJECTED
→ no Runtime resume

U07-US-009
question superseded
→ non-ACCEPTED governed result
→ no Runtime resume

U07-US-010
answer window / waiting lifecycle expired
→ EXPIRED
→ no Clinical State write
→ no Runtime resume

U07-US-011
Consultation cancelled / terminal
→ non-ACCEPTED
→ no ordinary resume
```

Exact EXPIRED vs REJECTED precedence for some cases belongs to RDP-02.

## 20.4 Runtime separation

```text
U07-US-012
business event ACCEPTED
+ checkpoint missing
→ do not rewrite business decision
→ reconstruct/repair if safe

U07-US-013
business event ACCEPTED
+ checkpoint stale but reconstructable
→ rehydrate from authoritative state/bindings
→ one resume effect

U07-US-014
business event ACCEPTED
+ checkpoint/runtime contract incompatible and unrecoverable
→ Runtime failure/incompatibility
→ Business ACCEPTED preserved historically
→ no fake REJECTED
→ governed failure handling

U07-US-015
historical waiting Run binding differs from current latest release
→ no silent latest-version substitution
```

## 20.5 Apply / state ownership

```text
U07-US-016
successful resume
→ old Pending Question not left falsely current
→ Consultation becomes ACTIVE only through governed effect

U07-US-017
accepted F3 question answer
→ U07 does not directly seize F3 Question/Gap ownership
→ reviewed owner bridge required for ANSWER_RECEIVED/ANSWERED semantics

U07-US-018
accepted answer payload
→ not written as Clinical Fact by U07
→ must reach U02

U07-US-019
U02 downstream processing fails
→ U07 does not duplicate resume event
→ downstream failure handled separately
```

## 20.6 Crash / repair

```text
U07-US-020
crash after ACCEPTED before Runtime resume
→ same event resumes/reconciles
→ no second business effect

U07-US-021
crash after Runtime resume before APPLIED
→ reconcile Runtime state
→ no second Runtime resume

U07-US-022
crash after APPLIED before Consultation ACTIVE
→ recover exact remaining effect
→ no duplicate downstream answer

U07-US-023
crash after ACTIVE before U02 handoff
→ exactly-one handoff repair
```

## 20.7 Hard boundaries

```text
U07-US-024
duplicate event
→ never replays stale Clinical Truth

U07-US-025
accepted answer
→ cannot route directly U07→U05/U08

U07-US-026
non-production verification
→ zero real external side effects / real-patient traffic
```

---

# 21. Regression cases

U07 verification must prove no regression to Foundation/U01-U06.

At minimum:

```text
REG-U07-01
U06 remains owner of lawful delivered-wait establishment;
U07 cannot create WAITING_USER from an undelivered Question.

REG-U07-02
F3 remains owner of Question/Gap business lifecycle;
U07 does not seize lifecycle ownership.

REG-U07-03
U02 remains the only ordinary path that interprets accepted answer into Clinical Facts.

REG-U07-04
P01/G2 remains the only governed Clinical State mutation path.

REG-U07-05
Business Resume validity remains separate from Runtime Resume compatibility.

REG-U07-06
same event replay does not create duplicate clinical effects.

REG-U07-07
checkpoint loss does not automatically invalidate a lawful business event.

REG-U07-08
U15 cancel/expire truth cannot be overridden by U07.

REG-U07-09
U14 remains final failure-routing owner.

REG-U07-10
legacy fixed workflow / controller session state is not restored as authoritative Resume truth.
```

---

# 22. Current repository asset mapping

Current post-U06 main provides useful foundation assets.

## 22.1 Reusable / candidate assets

```text
CanonicalBusinessEventLedger
CanonicalBusinessEventRecord
CanonicalBusinessEventRepository
= REUSE / ASSESS FOR U07 EVENT IDENTITY

CanonicalEffectLedger*
= REUSE / ASSESS FOR U07 EFFECT IDEMPOTENCY

RuntimeThreadStateRecord / Repository
= REUSE_FOUNDATION

RuntimeWaitCheckpointRecord / Repository / Service
= REUSE_FOUNDATION

RuntimeThreadWaitTransitionService
= REUSE_FOUNDATION / EXTEND FOR RESUME

ConsultationRecord / Repository
= REUSE / GOVERNED LIFECYCLE EXTENSION REQUIRED

U07ResumeEligibilityProjector
= REUSE AS U06→U07 INBOUND HANDOFF ONLY
```

## 22.2 Explicit non-equivalence

```text
U07ResumeEligibilityProjector
!= U07 implementation

RuntimeWaitCheckpointService
!= Business Resume Decision

CanonicalBusinessEventLedger
!= F8 owner

Thread state
!= Consultation lifecycle

raw answer DTO
!= Clinical Fact
```

## 22.3 Expected new/extended surfaces

U07 likely needs reviewed physical designs for:

```text
Business Resume Decision model/persistence
canonical U07 event admission
resume effect identity
P02 resume/reconstruction coordinator
Question/PendingQuestion/F3 owner bridge
Consultation ACTIVE transition
exact-once U02 handoff
U07 trace/evidence
```

Exact classes/tables/APIs are not frozen here.

---

# 23. Out of Scope

This U07 Unit Spec does not authorize or define:

```text
actual Java/Python class names
HTTP/RPC endpoint shape
database table names
queue/topic technology
exact event-id hash formula
exact effect-id hash formula
exact transaction/outbox/saga technology
exact Runtime resume enum names
exact failure reason-code enumeration
production activation
live USER_ANSWER traffic
real-patient traffic
real PHI
PROFILE-A
U08/U09 implementation
U14/U15 complete implementation
release activation
```

It also does not yet freeze:

```text
exact duplicate-vs-rejected precedence for every edge case
same answer/new event identity policy
exact Question ANSWER_RECEIVED commit owner bridge
exact Gap ANSWERED timing
exact Pending Question consume patch
exact APPLIED vs Consultation ACTIVE commit grouping
exact checkpoint reconstruction algorithm
exact U02 handoff effect identity
exact retry budget / timeout
```

Those belong to U07 RDP design.

---

# 24. Required downstream RDP design questions

This Unit Spec intentionally leaves implementation-governance detail for the Initial Implementation Readiness / Gap Review and U07 RDP package.

## U07-RDP-01 — Consumer Inbound / Event Admission Contract

Must decide/freeze:

```text
legal USER_ANSWER / RESUME_REQUEST inbound shapes
required U07 Resume Eligibility / wait provenance
canonical event identity
transport retry mapping
answer payload/ref fingerprint
currentness checks
terminal/cancelled/expired admission
same event replay
same semantic answer under new event ID
stale eligibility vs reconstructable wait
```

## U07-RDP-02 — F8 Business Resume Decision Contract

Must decide/freeze:

```text
F8 owner
ACCEPTED / DUPLICATE / EXPIRED / REJECTED precedence
wrong Question / wrong wait-effect behavior
expiry semantics
duplicate semantics
business decision persistence
business ACCEPTED durability across Runtime failure
no Runtime failure → fake REJECTED conversion
```

No medical content may be invented by engineering.

## U07-RDP-03 — State Ownership / Mutation / Idempotent Apply / Trace

Must decide/freeze:

```text
Business Resume lifecycle persistence
APPLIED semantics
resume parent effect identity
Question ANSWER_RECEIVED owner bridge
Gap ANSWERED owner bridge when applicable
Pending Question consume/clear
Consultation WAITING_USER → ACTIVE
K09/P01 exact proposal permissions
base-version/current-value guards
replay conflict semantics
trace/provenance equality
```

## U07-RDP-04 — Runtime Resume / Checkpoint / Downstream Boundary

Must decide/freeze:

```text
Business Decision before Runtime Resume
P02 compatibility matrix
checkpoint reconstruction
rehydrate/resume effect identity
crash windows
partial apply reconciliation
Runtime failure / U14 handoff
exact-once U02 handoff
no direct U05/U08 bypass
post-resume Thread/Run state
```

## U07-RDP-05 — Capability / Dependency / Applicability Contract

Must decide/freeze:

```text
P01/P02/P05/P06 exact applicability
historical binding compatibility
runtime schema compatibility
contract compatibility
current Pending Question applicability
current Consultation lifecycle applicability
Question expiry/supersede applicability
U15 cancel/expire interaction
no Clinical AI dependency
```

## U07-RDP-06 — Verification / Durable Evidence Contract

Must freeze:

```text
exact executable case matrix
independent oracle
synthetic fixtures
event/effect counts
exact-head CI
crash/replay evidence
zero duplicate U02 handoff
zero direct Clinical Truth write
zero production/live side effects
durable artifact manifest/checksums
independent evidence review gates
```

---

# 25. Initial Readiness inputs established by this Unit Spec

After this Unit Spec passes independent design review, the next formal gate should consume:

```text
1. this U07 Unit Spec
2. frozen Phase 5 BL-03 Resume semantics
3. Phase 6 U07 semantics
4. Phase 7 P01/P02/P05/P06 dependency row
5. Phase 8 K02/K09/K10 event/resume contracts
6. Phase 9 canonical event / effect ledger / WAIT-Resume runtime semantics
7. U06 RDP-03 Question/PendingQuestion state ownership
8. U06 RDP-04 exact wait + U07 eligibility contract
9. verified U06 implementation/evidence currently on main
10. current main repository archaeology
```

and produce:

```text
U07 Initial Implementation Readiness / Gap Review
→ explicit blockers
→ dependency gap inventory
→ U07-RDP-01..06 concrete design tasks
```

---

# 26. Unit completion definition

U07 cannot be called complete merely because a Thread resumes.

A future authorized implementation may be considered U07-verified only when evidence proves:

```text
legal event admission
canonical event idempotency
F8 decision correctness
duplicate/expired/rejected zero-new-effect behavior
Business Resume vs Runtime Resume separation
checkpoint compatibility/reconstruction
historical binding safety
crash-safe resume
APPLIED idempotency
Question/PendingQuestion lifecycle reconciliation
Consultation ACTIVE correctness
exactly-one U02 handoff
no direct Clinical Fact write
no stale Clinical Truth replay
failure/repair handoff
trace/provenance
regression against Foundation/U01-U06
```

Even then:

```text
U07 verified
!= U02 post-answer fact commit verified by U07
!= Slice A complete
!= U08 verified
!= Production authorized
```

---

# 27. Design verdict

At this design-candidate stage:

```text
U07 Business-Semantic Baseline
= AVAILABLE / A1 REFROZEN V1

U07 Entry Position
= CONFIRMED

U07 Unit Spec v0.1
= DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW

U07 Implementation Readiness
= NOT_READY

U07 Implementation Authorization
= NOT_GRANTED

U07 Implementation
= NOT_STARTED AS GOVERNED UNIT
```

If this Unit Spec passes independent design review, the next permitted formal gate is:

```text
U07 Initial Implementation Readiness / Gap Review
```

No code implementation is authorized by this document.
