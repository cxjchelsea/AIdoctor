# U07 Entry Gate Assessment v0.1

> Review target: post-U06 next Development Unit / Gate  
> Review basis: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`  
> Scope: repository planning / readiness only  
> This assessment grants no U07 implementation, merge, production, live-resume, or real-patient authorization.

---

# 1. Executive verdict

~~~text
U06 Implementation Verification
= PASS / CLOSED

U06 M1 repository integration
= COMPLETE / PMV PASS

U06 M2 accepted evidence / closure integration
= COMPLETE / PMV PASS

Current main
= 6d4fd787600e3a57f01f3e17893e6d98893ac546

Next Development Unit
= U07

U07 frozen business-semantic baseline
= AVAILABLE / A1 REFROZEN V1

U07 dedicated Unit Spec
= MISSING

U07 unit-specific RDP package
= MISSING

U07 implementation authorization
= NOT_GRANTED

U07 production/live authorization
= NOT_GRANTED
~~~

Therefore the next concrete deliverable and formal review gate are:

~~~text
NEXT CONCRETE DELIVERABLE
= U07 Unit Spec v0.1

NEXT FORMAL REVIEW GATE
= U07 Initial Implementation Readiness / Gap Review
~~~

The initial readiness review should consume the new Unit Spec together with the already-frozen Phase 6/7/8/9 semantics and the current post-U06 repository state.

---

# 2. Why U07 is the next Development Unit

The frozen Phase 6 first Intake / Wait / Resume slice is:

~~~text
U01
→ U02
→ U03
→ U04
→ U05
→ U06
→ U07
→ U02
~~~

U06 now proves the upstream wait boundary:

~~~text
Question = DELIVERED_TO_USER
+ Pending Question = current
+ Consultation = WAITING_USER
+ Thread = AWAITING_USER
+ compatible durable checkpoint
+ U07ResumeEligibility
~~~

U06 then stops.

Frozen downstream sequence:

~~~text
USER_ANSWER
→ U07
→ Business Resume Decision
→ Runtime Resume
→ U02
~~~

Therefore U07 is the first unfinished Development Unit required to close the normal cross-turn BL-03 / BL-04 loop.

---

# 3. Why U08 is not the immediate next default Unit

U08 remains a lawful U05 downstream path when:

~~~text
Clinical Readiness
= READY_FOR_CLINICAL_ANALYSIS
~~~

However the frozen construction order defines:

~~~text
Wave 1 Core
= U01 U02 U03 U04 U05 U06 U07

Wave 2
= U08 U09
~~~

U06 is now complete, but U07 is not.

The current Slice-A-oriented dependency also states:

~~~text
U01–U07
+
U11 / U14 / U15 slice-required closure support
~~~

Therefore:

~~~text
Immediate next default Development Unit
= U07

U08
= NOT_THE_NEXT_DEFAULT_UNIT
~~~

This does not make U08 semantically invalid; it only preserves the frozen construction order.

---

# 4. U07 frozen responsibility

Phase 6 freezes:

~~~text
Unit
= U07 用户回答 Resume 与幂等恢复

S_in
= Consultation WAITING_USER
+ Thread AWAITING_USER
+ current Pending Question

Event
= USER_ANSWER / RESUME_REQUEST
~~~

F8 / Business Resume decision:

~~~text
ACCEPTED
DUPLICATE
EXPIRED
REJECTED
~~~

If ACCEPTED:

~~~text
validate authoritative wait boundary
→ validate Pending Question / Clinical State Version compatibility
→ validate durable Runtime checkpoint compatibility
→ Runtime resume / rehydrate
→ mark event APPLIED
→ Consultation ACTIVE
→ hand user answer to U02
~~~

If DUPLICATE:

~~~text
0 second clinical effect
→ return idempotent confirmation / original processing ref / current authoritative state
~~~

If EXPIRED / REJECTED:

~~~text
0 Clinical State write
0 ordinary resume side effect
~~~

Critical invariant:

~~~text
Idempotent effect
!= replay stale clinical truth
~~~

---

# 5. U07 ownership boundary

U07 must keep three authorities separate.

## 5.1 Business Resume validity

U07 / F8 owns business-event applicability:

~~~text
USER_ANSWER / RESUME_REQUEST
→ ACCEPTED / DUPLICATE / EXPIRED / REJECTED
~~~

This is not Runtime resume compatibility.

## 5.2 Runtime Resume compatibility

P02 / Runtime owns:

~~~text
Thread
Run
Checkpoint
interrupt/wait execution context
pending refs
runtime expiry
retry/repair metadata
resume compatibility
rehydration
~~~

P02 does **not** own Business Resume validity.

## 5.3 Clinical Truth formation

U07 does not interpret the answer directly into Clinical Truth.

Accepted answer:

~~~text
U07
→ valid canonical answer event / resume consequence
→ U02
→ Clinical Fact formation / versioned commit
~~~

Therefore:

~~~text
U07 answer acceptance
!= Clinical Fact acceptance
!= direct Clinical State truth write
~~~

---

# 6. Existing repository assets

The current repository contains one explicit U07-related runtime artifact:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/
U07ResumeEligibilityProjector.java
~~~

Its current behavior only proves an upstream U06-side handoff projection:

~~~text
Thread must already be AWAITING_USER
+ parent wait effect ID
+ checkpoint ID
→ stable U07 eligibility ID
~~~

This asset is useful and should be treated as a reviewed upstream dependency.

It is **not** a U07 implementation.

It does not implement:

- USER_ANSWER / RESUME_REQUEST admission;
- canonical Event identity;
- F8 Business Resume Decision;
- ACCEPTED / DUPLICATE / EXPIRED / REJECTED;
- Pending Question/currentness validation;
- Question/Clinical State Version compatibility;
- answer-window expiry;
- P02 checkpoint compatibility;
- Runtime rehydrate/resume;
- APPLIED idempotency;
- Consultation WAITING_USER → ACTIVE;
- Pending Question consume/clear semantics;
- Question ANSWER_RECEIVED semantics;
- exact-once downstream handoff to U02;
- crash/replay repair after partial resume;
- U07-specific trace/evidence package.

Therefore:

~~~text
U07 implementation
= NOT_STARTED as a governed Development Unit

U07 upstream eligibility prerequisite
= PARTIALLY_AVAILABLE_FROM_U06
~~~

---

# 7. Frozen cross-phase inputs available for U07

## 7.1 Phase 6

Available:

- U07 S_in / Event / decision statuses / S_out;
- duplicate/expired/rejected semantics;
- U02 downstream ownership;
- no stale Clinical Truth replay.

## 7.2 Phase 7

U07 is frozen as:

~~~text
Clinical AI
= NONE

Platform
= P01 + P02 + P05 + P06

Deterministic owner/policy
= Resume validation / lifecycle rules

P02 Durable Clinical Resume
= FIRST_CONSUMER_UNIT U07
~~~

Primary concerns:

- Pending Question identity;
- Duplicate detection;
- Expiry;
- Clinical State Version compatibility;
- Checkpoint compatibility;
- Idempotent apply;
- crash / replay.

Frozen separation:

~~~text
Business Resume validity
!= Runtime Resume compatibility
~~~

## 7.3 Phase 8

Available contract/data semantics:

~~~text
K02 Business Event
- USER_ANSWER
- RESUME_REQUEST

K10 Business Resume Event Decision
- RECEIVED
- VALIDATING
- ACCEPTED
- DUPLICATE
- EXPIRED
- REJECTED
- APPLIED

K10 Runtime Execution Resume Result
- thread_id
- checkpoint_id
- referenced_clinical_state_version
- checkpoint_compatible
- runtime_resume_status
- run_id
- failure_ref
~~~

Optional bindings include:

~~~text
pending_event_id
pending_question_id
resume_event_id
expires_at
~~~

Frozen invariants:

~~~text
Duplicate Event
→ no second clinical effect

Business Resume validity
!= Runtime Execution Resume

Checkpoint
!= Clinical State
~~~

## 7.4 Phase 9

Available runtime semantics:

~~~text
USER_ANSWER
→ canonical Event identity
→ Business Resume Decision
→ only ACCEPTED may produce new effects
→ Runtime resume / rehydrate
→ reconcile already-applied effects
→ Scheduler continues from committed state
~~~

Resume compatibility must at minimum consider:

~~~text
checkpoint ↔ Clinical State Version
checkpoint ↔ runtime schema
checkpoint ↔ CapabilityBindingRef
checkpoint ↔ contract compatibility
pending interaction validity
~~~

---

# 8. U06 → U07 exact inbound boundary

U06 RDP-04 already freezes that U07 eligibility exists only when:

~~~text
Question = DELIVERED_TO_USER
PendingQuestion = current same Question
Consultation = WAITING_USER
Thread = AWAITING_USER
delivery / parent-effect provenance match
wait checkpoint compatible
Question not expired/superseded
~~~

If business wait exists but Runtime wait/checkpoint is incomplete:

~~~text
WAIT_RUNTIME_RECONCILIATION_REQUIRED
→ no U07 eligibility
~~~

Eligibility is explicitly:

~~~text
!= user answer
!= Business Resume Decision
!= Runtime Resume
~~~

U07 owns those later semantics.

This provides a concrete, already-verified inbound handoff for U07 Unit Spec design.

---

# 9. Initial readiness blockers expected for U07

The U07 Unit Spec can rely on frozen cross-phase semantics, but implementation is not ready because U07-specific implementation-governance contracts are missing.

Expected blocker-resolution package:

## U07-RDP-01 — Consumer Inbound / Event Admission Contract

Must freeze:

- USER_ANSWER vs RESUME_REQUEST entry semantics;
- required U07ResumeEligibility identity;
- consultation/thread/question/wait-effect/checkpoint binding;
- canonical business_event_id;
- same-event transport replay;
- illegal caller / illegal state / stale eligibility rejection;
- terminal/cancelled/expired Consultation rejection.

## U07-RDP-02 — F8 Business Resume Decision Contract

Must freeze:

~~~text
ACCEPTED
DUPLICATE
EXPIRED
REJECTED
~~~

and exact precedence.

Must define:

- who owns F8;
- duplicate semantics;
- expiration semantics;
- wrong Question / wrong wait-effect / wrong version semantics;
- whether an ACCEPTED event remains historically accepted after later Runtime failure;
- no conversion of runtime incompatibility into fake business rejection unless frozen rule requires it.

## U07-RDP-03 — State Ownership / Mutation / Idempotent Apply / Trace Contract

Must freeze exact authoritative effects for accepted resume, including:

- event lifecycle RECEIVED / VALIDATING / ACCEPTED / APPLIED;
- Question answer-received lifecycle ownership;
- pending_question consume/clear semantics;
- Consultation WAITING_USER → ACTIVE;
- exact effect identity;
- K09/P01 mutation ownership;
- optimistic concurrency / base version;
- duplicate event zero-new-effect proof;
- trace/provenance requirements.

It must explicitly preserve:

~~~text
user answer
!= Clinical Fact

U02
= owner of answer interpretation into new Clinical State Version
~~~

## U07-RDP-04 — Runtime Resume / Checkpoint / Downstream Boundary

Must freeze:

- Business Resume Decision before Runtime Resume;
- P02 checkpoint compatibility;
- rehydrate / repair / replay;
- checkpoint missing/stale/incompatible handling;
- resume exactly-once effect;
- crash windows before/after business ACCEPTED, Runtime resume, APPLIED;
- Scheduler continuation;
- exact-once handoff to U02;
- failure handoff boundary to U14;
- no direct U08/U05 bypass.

## U07-RDP-05 — Capability / Dependency / Applicability Contract

Must freeze:

~~~text
Clinical AI dependency
= NONE

P01 State Governance
= REQUIRED as applicable

P02 Durable Clinical Resume
= CORE / FIRST CONSUMER

P05 Trace / Audit
= REQUIRED

P06 Scope / Version Binding
= REQUIRED
~~~

Also freeze applicability of:

- current U06 eligibility;
- current Pending Question;
- current Clinical State Version;
- current Consultation lifecycle;
- current Thread/checkpoint;
- bound contract/capability semantics;
- answer-window expiry / cancellation state.

## U07-RDP-06 — Verification / Durable Evidence Contract

Must cover at minimum:

- legal ACCEPTED answer;
- duplicate same event;
- same payload/new event identity policy;
- wrong Question;
- wrong Consultation;
- wrong wait effect;
- stale eligibility;
- superseded/expired Question;
- cancelled/expired Consultation;
- current-version mismatch;
- checkpoint missing/stale/incompatible;
- business ACCEPTED but runtime resume retry;
- crash after ACCEPTED before runtime resume;
- crash after runtime resume before APPLIED;
- exact replay after APPLIED;
- duplicate produces zero second clinical effect;
- U02 handoff exactly once;
- no direct Clinical Truth write by U07;
- no stale clinical truth replay;
- no external/production side effect in non-production profile.

---

# 10. Initial implementation-readiness interpretation

Current U07 status:

~~~text
frozen business semantics
= AVAILABLE

cross-phase Capability / Contract / Runtime semantics
= AVAILABLE

U06 upstream wait/eligibility boundary
= VERIFIED / IN_MAIN

dedicated U07 Unit Spec
= MISSING

U07-RDP-01..06
= MISSING

P02 U07-specific physical contract
= NOT_ASSESSED

U07 Aggregate Contract Compatibility
= NOT_ASSESSED

U07 Implementation Readiness
= NOT_READY

U07 Implementation Authorization
= NOT_GRANTED

U07 code implementation
= NOT_STARTED as governed unit

production/live authorization
= NOT_GRANTED
~~~

The presence of `U07ResumeEligibilityProjector` does not change this verdict.

---

# 11. Exact gate sequence from current state

The governed sequence should be:

~~~text
U07 Business-Semantic Baseline
= AVAILABLE / REFROZEN

↓

U07 Unit Spec v0.1

↓

U07 Initial Implementation Readiness / Gap Review

↓

U07-RDP-01..06
+ any targeted physical-design remediation

↓

U07 Aggregate Contract Compatibility Review

↓

U07 Implementation Readiness Re-Evaluation

↓

U07 Implementation Authorization Decision

↓

U07 Formal Implementation

↓

Exact-Head Implementation Review / Semantic Freeze

↓

Authoritative Verification

↓

Independent Evidence Review

↓

Combined Implementation / Evidence Review

↓

Implementation Verification Closure

↓

Merge authorization / repository integration / PMV
~~~

Therefore the immediate next formal step is **not**:

~~~text
U07 implementation
U08
production activation
live resume
~~~

The immediate next concrete deliverable is:

~~~text
U07 Unit Spec v0.1
~~~

and the next formal review gate after that design is:

~~~text
U07 Initial Implementation Readiness / Gap Review
~~~

---

# 12. Final decision

~~~text
POST-U06 NEXT DEVELOPMENT UNIT
= U07

U07 ENTRY POSITION
= CONFIRMED

U07 upstream U06 wait/eligibility dependency
= VERIFIED / AVAILABLE

U07 dedicated Unit Spec
= REQUIRED NEXT

NEXT CONCRETE DELIVERABLE
= U07 Unit Spec v0.1

NEXT FORMAL REVIEW GATE
= U07 Initial Implementation Readiness / Gap Review

IMPLEMENTATION MAY BEGIN NOW
= NO

U07 IMPLEMENTATION AUTHORIZATION
= NOT_GRANTED

U08
= NOT_THE_IMMEDIATE_DEFAULT_NEXT_UNIT
~~~

No production/live/resume authorization is granted by this assessment.
