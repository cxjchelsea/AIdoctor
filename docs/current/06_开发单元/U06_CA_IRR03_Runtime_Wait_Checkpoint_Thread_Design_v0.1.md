# CA-U06-IRR-03 — Runtime Wait / Checkpoint / Thread AWAITING_USER Physical Design v0.1

> Parent readiness finding: **BF-U06-IRR-03**  
> Readiness baseline: **498d8ae086c7d9fd0e33017360d07ea1ba89ee73**  
> Aggregate authority: **eb8c52a4e2da1feab1297d820fef0f033ac27698**  
> Scope: **PROFILE-B bounded synthetic structural implementation only**  
> Status: **DRAFT / READY_FOR_INDEPENDENT_PHYSICAL_DESIGN_REVIEW**  
> This document does not authorize implementation, runtime schema migration, live U07, or production execution.

---

# 1. Purpose

RDP-04 requires:

~~~text
Clinical State delivered child authoritative
+
Consultation WAITING_USER authoritative
+
durable compatible checkpoint
→ Thread AWAITING_USER
→ U07ResumeEligibility
~~~

Current runtime has:
- immutable RuntimeBindingRecord with thread_id;
- ClinicalRunRecord with OPEN/CLOSED only;
- no mutable Thread state record;
- test-only Python checkpoint too small for U06.

This CA selects one physical runtime representation.

---

# 2. Selected physical strategy

Add two Java-side runtime persistence concepts:

~~~text
clinical_runtime_thread_state
clinical_runtime_wait_checkpoint
~~~

Keep RuntimeBindingRecord immutable.

Do not overload RuntimeBindingRecord with mutable wait state.

Do not use Python test-only checkpoint.py as the U06 runtime authority.

Physical roles:

~~~text
RuntimeBindingRecord
= immutable consultation ↔ thread/runtime/version binding

RuntimeThreadStateRecord
= mutable authoritative Runtime Thread state

RuntimeWaitCheckpointRecord
= durable U06 wait checkpoint/provenance
~~~

---

# 3. Runtime thread-state table

Define:

~~~text
clinical_runtime_thread_state
~~~

Columns:

~~~text
thread_id                   VARCHAR(128) PRIMARY KEY
consultation_id             VARCHAR(128) NOT NULL UNIQUE

row_version                 BIGINT NOT NULL

runtime_status              VARCHAR(32) NOT NULL
current_run_id              VARCHAR(128) NULL
current_wait_checkpoint_id  VARCHAR(128) NULL
current_wait_effect_id      VARCHAR(128) NULL

updated_at                  TIMESTAMP/DATETIME NOT NULL
created_at                  TIMESTAMP/DATETIME NOT NULL
~~~

Allowed bounded statuses:

~~~text
ACTIVE
WAIT_CHECKPOINTED
AWAITING_USER
~~~

WAIT_CHECKPOINTED is an internal Runtime reconciliation state.

It means:
- authoritative business WAITING prerequisites were validated;
- a durable wait checkpoint exists;
- this Thread owns that exact checkpoint/effect claim;
- Thread has not yet reached AWAITING_USER;
- U07 eligibility is absent.

This table is Runtime metadata only.

It is not Clinical State and not Consultation lifecycle truth.

---

# 4. Thread-state initialization

When a CLINICAL_RUNTIME_V1 binding exists and bounded U06 needs Runtime Thread state:

~~~text
thread state absent
→ create exactly one ACTIVE state
~~~

Required binding equality:

~~~text
thread_state.thread_id
= RuntimeBindingRecord.thread_id

thread_state.consultation_id
= RuntimeBindingRecord.consultation_id
~~~

Creation is idempotent for the same immutable RuntimeBindingRecord.

A mismatched existing thread-state row is a runtime binding conflict.

For existing consultations, no historical AWAITING_USER state is backfilled.

---

# 5. Wait-checkpoint table

Define:

~~~text
clinical_runtime_wait_checkpoint
~~~

Columns:

~~~text
checkpoint_id                    VARCHAR(128) PRIMARY KEY

consultation_id                  VARCHAR(128) NOT NULL
thread_id                        VARCHAR(128) NOT NULL
run_id                           VARCHAR(128) NOT NULL

question_id                      VARCHAR(128) NOT NULL
pending_question_ref             VARCHAR(128) NOT NULL

question_selection_effect_id     VARCHAR(128) NOT NULL
question_delivery_effect_id      VARCHAR(128) NOT NULL
question_delivered_wait_effect_id VARCHAR(128) NOT NULL

delivery_id                      VARCHAR(128) NOT NULL
delivery_confirmation_ref        VARCHAR(128) NOT NULL

clinical_state_version           INT NOT NULL
consultation_wait_effect_id      VARCHAR(128) NOT NULL

dependency_binding_ref           VARCHAR(128) NULL
question_policy_ref              VARCHAR(128) NULL

payload_fingerprint              VARCHAR(128) NOT NULL
checkpoint_status                VARCHAR(32) NOT NULL

created_at                       TIMESTAMP/DATETIME NOT NULL
~~~

Required constraints:

~~~text
PRIMARY KEY(checkpoint_id)

UNIQUE(question_delivered_wait_effect_id)

INDEX(thread_id)
INDEX(run_id)
INDEX(consultation_id)
~~~

Allowed checkpoint_status for bounded U06:

~~~text
ACTIVE
~~~

Future consumed/resumed status is U07 scope.

---

# 6. Stable checkpoint identity

Use frozen U06 semantics:

~~~text
U06_WAIT_CHECKPOINT_ID
=
QUESTION_DELIVERED_WAIT_EFFECT_ID
+ runtime_wait_checkpoint_contract_version
~~~

The physical checkpoint_id is this stable identity or a deterministic encoding of it.

No random checkpoint id per retry.

---

# 7. Checkpoint canonical fingerprint

Define:

~~~text
U06_WAIT_CHECKPOINT_PAYLOAD_FINGERPRINT
~~~

over:

~~~text
consultation_id
thread_id
run_id

question_id
pending_question_ref

question_selection_effect_id
question_delivery_effect_id
question_delivered_wait_effect_id

delivery_id
delivery_confirmation_ref

clinical_state_version
consultation_wait_effect_id

dependency_binding_ref
question_policy_ref

checkpoint_contract_version
~~~

Wall-clock timestamp is excluded.

Same checkpoint id + different fingerprint:

~~~text
U06_WAIT_CHECKPOINT_REPLAY_CONFLICT
→ fail closed
~~~

---

# 8. Why a separate Thread state is required

ClinicalRunRecord is event/run-scoped.

Thread is consultation-scoped through RuntimeBindingRecord.

A user may have multiple runs over one thread across canonical business events.

Therefore:

~~~text
ClinicalRunRecord.status
alone
~~~

is not selected as the authoritative Thread state.

The new RuntimeThreadStateRecord provides one current mutable runtime state per thread.

ClinicalRunRecord may retain OPEN/CLOSED semantics for run lifecycle.

U06 does not overload OPEN/CLOSED into AWAITING_USER.

---

# 9. Wait coordinator

Define:

~~~text
RuntimeWaitCoordinator
~~~

It consumes only authoritative business-state evidence:
- Clinical State delivered read-back;
- Consultation WAITING read-back;
- exact parent wait effect;
- delivery confirmation;
- current RuntimeBindingRecord;
- current ClinicalRunRecord.

It may not infer business truth from delivery transport alone.

---

# 10. Checkpoint reservation and Thread ordering

Normal physical order:

~~~text
R1. validate authoritative Clinical State delivered child

R2. validate authoritative Consultation WAITING child

R3. resolve RuntimeBindingRecord / thread_id

R4. derive stable checkpoint id/fingerprint

R5. checkpoint-reservation transaction:
    lock RuntimeThreadStateRecord FOR UPDATE

    ACTIVE + no wait claim
    → insert/reconcile exact checkpoint
    → set:
       runtime_status = WAIT_CHECKPOINTED
       current_run_id = run_id
       current_wait_checkpoint_id = checkpoint_id
       current_wait_effect_id = QUESTION_DELIVERED_WAIT_EFFECT_ID
    → commit

R6. authoritative read-back:
    WAIT_CHECKPOINTED
    + exact checkpoint/effect

R7. separate Thread transition transaction:
    lock same Thread state
    WAIT_CHECKPOINTED → AWAITING_USER
    preserve run/checkpoint/effect identity
    → commit

R8. authoritative Thread read-back

R9. emit/reattach U07ResumeEligibility
~~~

The checkpoint reservation and Thread claim are committed together.

This prevents two different wait effects from each creating an independently active checkpoint for one Thread.

The later WAIT_CHECKPOINTED → AWAITING_USER transition remains a separate commit, preserving the RDP-04 C8 recovery boundary.

---

# 11. Checkpoint reservation transaction

Define:

~~~text
RuntimeWaitCheckpointService.reserveCheckpoint(command)
~~~

The service must lock RuntimeThreadStateRecord before creating or reattaching a checkpoint.

Behavior:

~~~text
Thread ACTIVE + no wait claim
+ checkpoint absent
→ insert ACTIVE checkpoint
→ Thread ACTIVE becomes WAIT_CHECKPOINTED
→ bind checkpoint/effect/run
→ one local transaction

Thread WAIT_CHECKPOINTED
+ same checkpoint/effect/run
+ same checkpoint fingerprint
→ exact replay / reattach

Thread AWAITING_USER
+ same checkpoint/effect/run
+ same checkpoint fingerprint
→ already beyond reservation
→ exact replay path may continue to eligibility

Thread WAIT_CHECKPOINTED or AWAITING_USER
+ different effect/checkpoint
→ U06_RUNTIME_WAIT_CONFLICT

same checkpoint id + different fingerprint
→ U06_WAIT_CHECKPOINT_REPLAY_CONFLICT
~~~

If a unique-key race occurs on checkpoint id or parent wait effect, reload under Thread lock and apply the same equality rules.

A checkpoint cannot be rewritten to another Question/delivery/wait effect.

---

# 12. Thread transition transaction

Define:

~~~text
RuntimeThreadWaitTransitionService.enterAwaitingUser(command)
~~~

Use a locked read of RuntimeThreadStateRecord.

First AWAITING transition requires:

~~~text
runtime_status = WAIT_CHECKPOINTED

current_run_id = run_id
current_wait_checkpoint_id = checkpoint_id
current_wait_effect_id = question_delivered_wait_effect_id

checkpoint exists and is ACTIVE
checkpoint identities/fingerprint match command
~~~

Then atomically update only:

~~~text
runtime_status = AWAITING_USER
~~~

while preserving the same run/checkpoint/effect identities.

The Thread row_version advances once for reservation and once for AWAITING transition.

RDP-06 evidence must distinguish these two transitions.

---

# 13. Exact Thread replay

If current Thread state is:

~~~text
AWAITING_USER
~~~

exact replay succeeds only when:

~~~text
current_wait_checkpoint_id = same checkpoint_id
current_wait_effect_id = same parent effect
current_run_id = same run_id

checkpoint fingerprint = same
~~~

Then:
- no second thread transition;
- no new checkpoint;
- no new resume eligibility identity.

Different current wait identity:

~~~text
U06_RUNTIME_WAIT_CONFLICT
→ fail closed
~~~

---

# 14. C7 recovery — Consultation WAITING / checkpoint absent

If:
- Clinical State delivered authoritative;
- Consultation WAITING authoritative;
- checkpoint absent;

then:

~~~text
derive same U06_WAIT_CHECKPOINT_ID
→ insert/reconcile checkpoint
→ continue
~~~

No delivery resend.
No Clinical State recommit.
No Consultation recommit.

---

# 15. C8 recovery — checkpoint exists / Thread not AWAITING_USER

If checkpoint reservation is authoritative and Thread state is:

~~~text
WAIT_CHECKPOINTED
~~~

then:

~~~text
validate checkpoint/business-state compatibility
→ execute exact WAIT_CHECKPOINTED → AWAITING_USER transition
→ no new checkpoint
→ no delivery resend
~~~

This is the machine-readable C8 state:

~~~text
WAIT_RUNTIME_RECONCILIATION_REQUIRED
~~~

until transition succeeds.

An orphan checkpoint with Thread still ACTIVE is inconsistent and must fail/reconcile; normal reservation protocol must not create it.

No U07 eligibility exists yet.

---

# 16. C9 recovery — Thread AWAITING_USER / eligibility projection missing

If:
- checkpoint authoritative;
- Thread exact AWAITING_USER state authoritative;
- eligibility projection absent;

then:

~~~text
derive same U07_RESUME_ELIGIBILITY_ID
→ re-emit/reattach projection
~~~

No state mutation is required.

---

# 17. U07ResumeEligibility physical projection

Define:

~~~text
U07ResumeEligibilityProjector
~~~

It is pure/deterministic.

Input:
- authoritative Clinical State read-back;
- Consultation WAITING read-back;
- Runtime Thread AWAITING_USER read-back;
- checkpoint.

Output:
- U07ResumeEligibility.

It may emit only when all identities match.

The projector does not:
- parse USER_ANSWER;
- change Thread state;
- resume Runtime;
- call U02;
- mutate Clinical State.

---

# 18. Stable U07 eligibility identity

Use frozen identity:

~~~text
U07_RESUME_ELIGIBILITY_ID
=
QUESTION_DELIVERED_WAIT_EFFECT_ID
+ checkpoint compatibility identity
+ eligibility_contract_version
~~~

Exact replay returns the same logical eligibility identity.

This prevents multiple independent resume windows for the same pending Question.

---

# 19. Run binding

The checkpoint binds the run that reached the wait boundary:

~~~text
run_id
~~~

The run must:
- belong to the same consultation;
- belong to the same thread;
- be based on a Clinical State version not later than the committed delivered version;
- be the current U06 execution run.

This CA does not redefine ClinicalRunRecord OPEN/CLOSED.

Future U07 may open a new run for USER_ANSWER rather than mutating the old run; that decision remains U07 scope.

---

# 20. Runtime-state consistency

Authoritative Runtime AWAITING_USER for U06 requires:

~~~text
RuntimeThreadStateRecord.runtime_status = AWAITING_USER

current_wait_checkpoint_id
→ exact ACTIVE RuntimeWaitCheckpointRecord

current_wait_effect_id
= checkpoint.question_delivered_wait_effect_id

thread/consultation/run identities match
~~~

If any mismatch:

~~~text
RUNTIME_WAIT_STATE_INCONSISTENT
→ reconciliation/failure
→ no U07 eligibility
~~~

---

# 21. PROFILE-B environment boundary

Bounded implementation may use these tables only in isolated non-production/test database.

It may not connect to production runtime storage.

No real external delivery is involved.

The physical schema may be future-compatible but current authorization remains non-production only.

---

# 22. Migration strategy

Future implementation must add matching MySQL and Oracle migrations for:
- clinical_runtime_thread_state;
- clinical_runtime_wait_checkpoint.

No existing RuntimeBindingRecord row is mutated during migration except optional creation of ACTIVE thread-state rows may occur only through an explicitly reviewed runtime bootstrap, not an unconditional historical backfill.

No historical AWAITING_USER state is invented.

---

# 23. Why Python checkpoint.py is not reused

Current packages/python_runtime/checkpoint.py is explicitly:
- TEST_ONLY;
- NON_PRODUCTION;
- EXECUTION_METADATA_ONLY;
- run_id/step_name/status only.

It lacks U06 wait identity and Java Clinical Runtime persistence integration.

Therefore it remains unchanged and is not the authoritative U06 wait checkpoint.

---

# 24. Candidate implementation surface

Future authorization may permit:

~~~text
runtime/foundation/
  RuntimeThreadStateRecord
  RuntimeThreadStateRepository
  RuntimeWaitCheckpointRecord
  RuntimeWaitCheckpointRepository
  RuntimeWaitCheckpointService
  RuntimeThreadWaitTransitionService

runtime/u06/wait/
  RuntimeWaitCoordinator
  U07ResumeEligibilityProjector
  wait commands/views

db/migration/
  next version runtime wait tables

db/migration-oracle/
  matching Oracle migration
~~~

RuntimeBindingRecord remains immutable.

ClinicalRunRecord need not gain AWAITING_USER.

---

# 25. Verification obligations

~~~text
IRR03-V01 one RuntimeThreadState per runtime thread
IRR03-V02 initial status ACTIVE
IRR03-V03 checkpoint id stable for same parent wait effect
IRR03-V04 same checkpoint id + changed payload conflicts
IRR03-V05 checkpoint commits before Thread AWAITING transition
IRR03-V06 checkpoint reservation sets WAIT_CHECKPOINTED + claim atomically
IRR03-V07 reservation advances Thread row_version exactly once
IRR03-V08 AWAITING transition advances Thread row_version exactly once
IRR03-V09 exact Thread replay causes zero second transition
IRR03-V10 C7 recovery creates/reserves same checkpoint only
IRR03-V11 C8 WAIT_CHECKPOINTED recovery transitions Thread without resend/recommit
IRR03-V12 C9 recovery re-emits same eligibility only
IRR03-V13 business WAITING + Thread not AWAITING_USER → no U07 eligibility
IRR03-V14 competing different wait effects cannot create two active checkpoints
IRR03-V15 mismatched checkpoint/effect/thread fails closed
IRR03-V16 Python test checkpoint is not used as authority
IRR03-V17 production runtime store writes = 0 in PROFILE-B verification
~~~

---

# 26. Readiness finding disposition

If independent review passes:

~~~text
BF-U06-IRR-03
= PHYSICAL_DESIGN_COMPLETE / PENDING_COMBINED_READINESS_REEVALUATION
~~~

---

# 27. Authorization boundary

This design grants no runtime code, DB migration execution, live U07, Scheduler activation, production execution, merge or real-patient traffic.

---

# 28. Draft verdict

~~~text
CA-U06-IRR-03
= REVISED / READY_FOR_TARGETED_PHYSICAL_DESIGN_RE_REVIEW

BF-U06-CA-IRR03-IR-01
= REMEDIATED / RE_REVIEW_PENDING

BF-U06-IRR-03
= OPEN / DESIGN_RE_REVIEW_PENDING
~~~
