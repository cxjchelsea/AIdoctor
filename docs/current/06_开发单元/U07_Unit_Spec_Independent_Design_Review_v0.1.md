# U07 Unit Spec v0.1 Independent Design Review

> Review target: `U07 Unit Spec v0.1`  
> Reviewed exact head: `9c899fdbe2136d88ed1d3bf5c2dd9b6d2b702272`  
> Parent entry assessment: `a8301e24a8cb2f5c2fe5a52886b0b768030712ed`  
> Frozen repository basis referenced by the Unit Spec: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`  
> Review branch: `review/u07-unit-spec-independent-design-v1`  
> Scope: **INDEPENDENT DESIGN REVIEW ONLY**  
> Verdict: **PASS / NO_BLOCKER**  
> This review grants no U07 implementation, merge, production, live-resume, PROFILE-A, release activation, real-patient, or PHI authorization.

---

# 1. Executive decision

Independent review of the exact Unit Spec head concludes:

~~~text
U07 Unit Spec v0.1
= PASS

Design blockers
= NONE

Ownership drift
= NOT_FOUND

Business Resume / Runtime Resume conflation
= NOT_FOUND

Accepted answer / Clinical Fact conflation
= NOT_FOUND

F3 Question / Gap ownership takeover
= NOT_FOUND

U02 bypass
= NOT_FOUND

U14 / U15 ownership override
= NOT_FOUND

Crash / replay / idempotency design omission at Unit-Spec level
= NOT_FOUND

Implementation authorization leakage
= NOT_FOUND
~~~

The Unit Spec is sufficiently complete to become an input to the next formal gate:

~~~text
U07 Initial Implementation Readiness / Gap Review
~~~

This PASS does **not** mean U07 implementation is ready or authorized. The Unit Spec intentionally leaves exact implementation-governance contracts to U07-RDP-01..06.

---

# 2. Review method and authority set

This review did not treat the Unit Spec's own verdict as authoritative.

The exact-head design was checked against the repository's frozen/refrozen semantic authorities available at the reviewed head, including:

1. Phase 5 business-loop semantics:
   - `docs/current/05_业务闭环/业务闭环设计_V1.md`
2. Phase 6 Development Unit semantics:
   - `docs/current/06_开发单元/可验证开发单元拆分_V1.md`
3. Phase 7 Capability / Platform dependency semantics:
   - `docs/current/07_能力设计/按开发单元的Capability设计.md`
4. Phase 8 Contract / data semantics:
   - `docs/current/08_契约与数据/Contract与数据语义设计.md`
5. Phase 9 Runtime semantics:
   - `docs/current/09_Runtime与技术架构/Runtime与技术架构设计_V1.md`
6. Phase 4 module ownership:
   - `docs/current/03_状态/模块级状态与状态所有权_V1.md`
7. U06 state ownership / mutation boundary:
   - `U06_RDP03_State_Ownership_K09_P01_Mutation_Trace_Contract_v0.1.md`
8. U06 delivery / WAITING_USER / U07 eligibility boundary:
   - `U06_RDP04_Delivery_Downstream_SideEffect_Boundary_v0.1.md`
9. U07 entry assessment:
   - `U07_Entry_Gate_Assessment_v0.1.md`

The design PR was also checked for scope purity:

~~~text
Entry assessment base
= a8301e24a8cb2f5c2fe5a52886b0b768030712ed

Unit Spec head
= 9c899fdbe2136d88ed1d3bf5c2dd9b6d2b702272

Delta
= 1 commit
= 1 added design file
= 0 runtime src/main changes
~~~

Therefore this is an exact-head design review, not a mixed design/implementation review.

---

# 3. Finding DR-U07-01 — Business Resume validity remains separate from Runtime Resume compatibility

## Authority

Frozen semantics require:

~~~text
Business Resume validity
!= Runtime Resume compatibility
~~~

F8 owns business-level Resume validity.

P02 owns Thread / Run / Checkpoint / resume compatibility and rehydration.

A stale or missing checkpoint alone cannot invalidate a lawful business event.

## Unit Spec check

The Unit Spec explicitly freezes:

~~~text
ACCEPTED
!= checkpoint definitely recoverable

Runtime INCOMPATIBLE
!= Business REJECTED

checkpoint missing/stale
→ reconstruct / repair when safe
→ do not rewrite lawful ACCEPTED to fake REJECTED
~~~

It also requires Business Resume Decision before Runtime Resume.

## Decision

~~~text
DR-U07-01
= PASS
~~~

No business/runtime ownership conflation was found.

---

# 4. Finding DR-U07-02 — F8 ownership is correctly bounded

## Authority

F8 owns:

- Resume Event / Resume Request validity;
- duplicate decision;
- expiry decision;
- correction/resume business coordination.

F8 does not own why the system is waiting and does not own Runtime checkpoint/run state.

## Unit Spec check

The Unit Spec assigns U07/F8 exactly:

~~~text
ACCEPTED
DUPLICATE
EXPIRED
REJECTED
APPLIED
~~~

while leaving:

~~~text
Question business lifecycle
= F3

Thread / Run / Checkpoint
= P02 Runtime

Clinical Fact formation
= U02/F2/G2
~~~

## Decision

~~~text
DR-U07-02
= PASS
~~~

No F8 overreach was found.

---

# 5. Finding DR-U07-03 — Accepted answer is not promoted directly to Clinical Truth

## Authority

Frozen Phase 5/6 semantics require:

~~~text
用户回答
→ F2 / U02
→ G2 commit
→ new Clinical State Version
~~~

and explicitly prohibit direct answer → Clinical Truth promotion.

## Unit Spec check

The Unit Spec repeatedly preserves:

~~~text
Accepted user answer
!= Clinical Fact

U07 APPLIED
!= U02 fact commit completed
~~~

The normal downstream path is:

~~~text
U07
→ U02
→ Clinical Observation / Fact interpretation
→ governed commit
~~~

It also forbids direct U07 → U05/U08 routing.

## Decision

~~~text
DR-U07-03
= PASS
~~~

No Clinical Truth ownership leak was found.

---

# 6. Finding DR-U07-04 — F3 Question / Gap lifecycle ownership remains intact

## Authority

Frozen module ownership states:

~~~text
F3
= Question business lifecycle owner

Question:
PROPOSED
SELECTED
DELIVERED_TO_USER
ANSWER_RECEIVED
EXPIRED
SUPERSEDED

Gap:
...
ASKED
ANSWERED
...
~~~

U06 RDP-03/RDP-04 also preserve F3 ownership and establish the delivered-wait parent boundary.

## Unit Spec check

The Unit Spec explicitly prohibits U07 from seizing:

~~~text
Question DELIVERED_TO_USER → ANSWER_RECEIVED
Gap ASKED → ANSWERED
~~~

Instead, it requires U07-RDP-03 to freeze a reviewed F3-owner bridge and governed K09/P01 mutation path.

The Unit Spec only freezes the required postcondition:

~~~text
after a successfully applied answer,
the old delivered Question / Pending Question cannot remain falsely current
~~~

without prematurely inventing the exact physical mutation grouping.

## Decision

~~~text
DR-U07-04
= PASS
~~~

This is the correct Unit-Spec-level boundary.

---

# 7. Finding DR-U07-05 — U06 → U07 inbound boundary is preserved without turning checkpoint into business truth

## Authority

U06 normal eligibility is emitted only after authoritative establishment of:

~~~text
Question DELIVERED_TO_USER
+ current Pending Question
+ Consultation WAITING_USER
+ durable checkpoint
+ Thread AWAITING_USER
+ matching delivered-wait provenance
~~~

But Phase 9 separately freezes:

~~~text
stale/missing checkpoint alone
cannot invalidate a valid business event
~~~

## Unit Spec check

The Unit Spec correctly distinguishes:

1. the normal U06 eligibility boundary at wait establishment; and
2. a later U07 answer-time condition where checkpoint evidence may have become stale/missing and must be reconstructed rather than used to fabricate a business rejection.

It uses the concept:

~~~text
current / reattachable U07 Resume Eligibility
~~~

and defers exact stale-eligibility/currentness rules to RDP-01/RDP-04.

## Decision

~~~text
DR-U07-05
= PASS
~~~

No contradiction with U06 wait-establishment semantics was found.

---

# 8. Finding DR-U07-06 — Idempotency and duplicate semantics are structurally complete

## Authority

Frozen invariants include:

~~~text
same event_id transport replay
→ same canonical event

Duplicate Event
→ no second clinical effect

Idempotent effect
!= replay stale clinical truth
~~~

## Unit Spec check

The Unit Spec covers:

- same canonical event before full apply;
- same canonical event after APPLIED;
- changed payload under protected identity;
- two competing answer events for the same Pending Question;
- no second Runtime resume;
- no second Consultation ACTIVE effect;
- no second U02 handoff;
- no stale diagnosis/recommendation/delivery replay.

## Decision

~~~text
DR-U07-06
= PASS
~~~

The remaining exact identity/fingerprint rules are properly deferred to RDP-01/RDP-03 rather than omitted.

---

# 9. Finding DR-U07-07 — Crash / recovery windows are sufficient for Unit Spec

The Unit Spec explicitly covers crash windows:

~~~text
C1 canonicalization → before F8
C2 ACCEPTED → before Runtime compatibility
C3 Runtime compatible → before rehydrate completion
C4 Runtime resume → before APPLIED
C5 APPLIED → before Consultation ACTIVE
C6 ACTIVE → before Pending Question reconciliation
C7 reconciliation → before U02 handoff
C8 U02 handoff intent → before durable downstream acknowledgement
~~~

For each it requires:

~~~text
same canonical event
same intended effect
no duplicate clinical side effect
no duplicate Runtime resume
no duplicate U02 handoff
no silent WAITING rollback after ACTIVE
no silent ACTIVE while business apply is incomplete
~~~

This is consistent with Phase 9's event/effect ledger and checkpoint-recovery model.

## Decision

~~~text
DR-U07-07
= PASS
~~~

The exact transaction/outbox/saga/reconciliation mechanics correctly remain RDP-03/RDP-04 work.

---

# 10. Finding DR-U07-08 — Failure ownership and U14/U15 boundaries are preserved

The Unit Spec correctly separates:

~~~text
Business REJECTED
!= Runtime FAILED

Business EXPIRED
!= checkpoint stale

Runtime INCOMPATIBLE
!= Business REJECTED

U02 downstream failure
!= U07 Business Resume invalid
~~~

It does not let U07 override:

~~~text
U15 cancel / expire truth
U14 final failure routing
~~~

It also prohibits:

- silent latest-binding fallback;
- fake ACTIVE;
- direct U05/U08 continuation;
- invented Clinical Fact.

## Decision

~~~text
DR-U07-08
= PASS
~~~

---

# 11. Finding DR-U07-09 — Capability / platform dependency model is aligned

Frozen Phase 7 semantics require:

~~~text
Clinical AI
= NONE

U07 Platform
= P01 + P02 + P05 + P06

P02 Durable Resume
= FIRST_CONSUMER_UNIT U07
~~~

The Unit Spec preserves this exactly and does not introduce P03/P04/clinical-model dependency into Business Resume.

It also preserves historical binding compatibility and prohibits silent upgrade-to-latest on resume.

## Decision

~~~text
DR-U07-09
= PASS
~~~

---

# 12. Finding DR-U07-10 — RDP package is complete enough for the next readiness gate

The Unit Spec defines all six required RDP workstreams:

~~~text
U07-RDP-01 Consumer Inbound / Event Admission
U07-RDP-02 F8 Business Resume Decision
U07-RDP-03 State Ownership / Mutation / Idempotent Apply / Trace
U07-RDP-04 Runtime Resume / Checkpoint / Downstream Boundary
U07-RDP-05 Capability / Dependency / Applicability
U07-RDP-06 Verification / Durable Evidence
~~~

The unresolved implementation-governance decisions have been placed into the appropriate RDPs instead of being silently assumed.

## Decision

~~~text
DR-U07-10
= PASS
~~~

---

# 13. Finding DR-U07-11 — Acceptance and regression coverage is adequate at Unit-Spec level

The Unit Spec includes 26 unit-level acceptance scenarios covering:

- lawful USER_ANSWER / RESUME_REQUEST;
- same-event replay before/after apply;
- protected-identity payload conflict;
- concurrent answer events;
- wrong consultation/question;
- superseded/expired/cancelled waits;
- missing/stale/incompatible checkpoint;
- historical binding mismatch;
- Pending Question reconciliation;
- F3 ownership bridge;
- accepted answer != Clinical Fact;
- U02 failure;
- crash repair;
- no stale-truth replay;
- no U05/U08 bypass;
- non-production zero-real-side-effect requirement.

It also includes 10 regression assertions protecting Foundation/U01-U06 boundaries.

## Decision

~~~text
DR-U07-11
= PASS
~~~

RDP-06 still needs to turn these semantic cases into executable evidence, independent oracle, fixture and durable artifact contracts.

---

# 14. Finding DR-U07-12 — No authorization leakage

The Unit Spec is explicit that:

~~~text
U07 Implementation Readiness
= NOT_READY

U07 Implementation Authorization
= NOT_GRANTED

U07 Implementation
= NOT_STARTED AS GOVERNED UNIT
~~~

It does not authorize:

- implementation;
- merge;
- production;
- live USER_ANSWER traffic;
- real patient traffic;
- PHI;
- PROFILE-A;
- release activation.

## Decision

~~~text
DR-U07-12
= PASS
~~~

---

# 15. Non-blocking observations carried forward

These are **not Unit Spec blockers**. They must become explicit decisions in the RDP package.

## NB-U07-01 — Same semantic answer under a new event ID

The Unit Spec intentionally does not decide whether:

~~~text
same semantic answer
+ new business_event_id
~~~

must become:

~~~text
DUPLICATE
REJECTED / conflict
or independent event
~~~

This is correctly assigned to U07-RDP-01/RDP-02.

Status:

~~~text
NON_BLOCKING
REQUIRED_RDP_DECISION
~~~

## NB-U07-02 — Exact APPLIED / ACTIVE / PendingQuestion / F3 / U02 choreography

The Unit Spec freezes the required end-state and crash boundaries, but not the exact transaction/choreography grouping among:

~~~text
F8 APPLIED
Question ANSWER_RECEIVED / Gap ANSWERED owner bridge
Pending Question consume/clear
Consultation ACTIVE
Runtime resume state
U02 handoff intent / durable acknowledgement
~~~

This is correctly assigned to U07-RDP-03/RDP-04.

Status:

~~~text
NON_BLOCKING
REQUIRED_RDP_DECISION
~~~

## NB-U07-03 — RESUME_REQUEST physical inbound semantics

Phase 8 recognizes `RESUME_REQUEST` as a valid Business Event type, while the exact relation among:

~~~text
RESUME_REQUEST
USER_ANSWER
already-governed answer event ref
transport retry
~~~

remains intentionally unfrozen.

The Unit Spec does not need to resolve that physical shape, but U07-RDP-01 must.

Status:

~~~text
NON_BLOCKING
REQUIRED_RDP_DECISION
~~~

---

# 16. Blocker register

~~~text
B-U07-DSR-01
= NONE

B-U07-DSR-02
= NONE

B-U07-DSR-03
= NONE
~~~

No blocker requiring revision of `U07 Unit Spec v0.1` was identified.

---

# 17. Independent design review verdict

~~~text
Reviewed exact head
= 9c899fdbe2136d88ed1d3bf5c2dd9b6d2b702272

U07 Unit Spec v0.1
= PASS

Independent Design Review
= CLOSED / PASS

Design blocker count
= 0

Non-blocking carry-forward items
= 3

U07 Implementation Readiness
= NOT_READY / NOT_YET_REEVALUATED

U07 Implementation Authorization
= NOT_GRANTED

U07 Implementation
= NOT_STARTED AS GOVERNED UNIT
~~~

The next permitted formal gate is:

~~~text
U07 Initial Implementation Readiness / Gap Review
~~~

That review must consume this Unit Spec, this independent review, the frozen Phase 5/6/7/8/9 authorities, U06 RDP-03/RDP-04, and the current repository state, and must produce explicit blockers and the concrete U07-RDP-01..06 design workload.

No implementation may begin solely because this independent design review passed.
