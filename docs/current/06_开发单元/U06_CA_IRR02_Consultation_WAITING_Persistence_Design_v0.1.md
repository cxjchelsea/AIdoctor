# CA-U06-IRR-02 — Consultation WAITING Effect / Idempotency / Provenance Persistence Design v0.1

> Parent readiness finding: **BF-U06-IRR-02**  
> Readiness baseline: **498d8ae086c7d9fd0e33017360d07ea1ba89ee73**  
> Aggregate authority: **eb8c52a4e2da1feab1297d820fef0f033ac27698**  
> Scope: **PROFILE-B bounded synthetic structural implementation only**  
> Status: **DRAFT / READY_FOR_INDEPENDENT_PHYSICAL_DESIGN_REVIEW**  
> This document does not authorize schema migration, implementation, production write, or live Consultation transition.

---

# 1. Purpose

RDP-03/RDP-04 already freeze the business semantics:

~~~text
Clinical State delivered child
→ authoritative read-back
→ Consultation WAITING child
→ authoritative Consultation read-back
~~~

and:

~~~text
CONSULTATION_WAITING_EFFECT_ID
must be exact-replay safe
~~~

This CA selects the physical persistence model required before bounded PROFILE-B code may be authorized.

---

# 2. Current persistence reality

Current ConsultationRecord contains:
- consultation_id;
- row_version via JPA @Version;
- lifecycle_status;
- U01 subject/problem/scope fields.

Current lifecycle constant:

~~~text
ACTIVE
~~~

There is no authoritative physical representation for:
- WAITING_USER;
- current wait effect identity;
- parent delivered-wait effect;
- question_id;
- delivery_id;
- wait payload fingerprint;
- wait idempotency key;
- exact replay result.

Therefore:

~~~text
lifecycle_status = WAITING_USER
alone
!= exact replay proof
~~~

---

# 3. Selected physical strategy

This CA selects:

~~~text
ConsultationRecord
+ minimal current_wait_effect_id pointer

plus

clinical_consultation_wait_effect
companion effect ledger
~~~

Rationale:
- ConsultationRecord remains the authoritative current lifecycle state.
- The companion table preserves immutable wait-effect provenance/history.
- A single pointer on ConsultationRecord binds the current WAITING_USER state to the exact effect.
- Full delivery/question provenance is not duplicated into the core Consultation row.
- Future multiple wait/resume cycles can preserve historical effect rows.

The alternative of storing all provenance directly on ConsultationRecord is rejected.

---

# 4. ConsultationRecord physical extension

Future implementation design permits these additions:

~~~text
public static final String WAITING_USER = "WAITING_USER";

@Column(name = "current_wait_effect_id", length = 128)
private String currentWaitEffectId;
~~~

Current lifecycle invariant:

~~~text
ACTIVE
→ current_wait_effect_id = null

WAITING_USER
→ current_wait_effect_id = exact committed CONSULTATION_WAITING_EFFECT_ID
~~~

U06 may only perform:

~~~text
ACTIVE → WAITING_USER
~~~

U06 does not own:
- WAITING_USER → ACTIVE resume;
- cancellation;
- expiry;
- terminal consultation transitions.

Those remain U07/U15 or their future owners.

---

# 5. Companion effect table

Define physical table:

~~~text
clinical_consultation_wait_effect
~~~

Required columns:

~~~text
wait_effect_id                VARCHAR(128) PRIMARY KEY

parent_delivered_wait_effect_id VARCHAR(128) NOT NULL
consultation_id               VARCHAR(128) NOT NULL
question_id                   VARCHAR(128) NOT NULL
delivery_id                   VARCHAR(128) NOT NULL

payload_fingerprint           VARCHAR(128) NOT NULL
idempotency_key               VARCHAR(128) NOT NULL

expected_prior_lifecycle      VARCHAR(32) NOT NULL
expected_prior_row_version    BIGINT NOT NULL

committed_lifecycle           VARCHAR(32) NOT NULL
committed_row_version         BIGINT NOT NULL

effect_status                 VARCHAR(32) NOT NULL

created_at                    TIMESTAMP/DATETIME NOT NULL
committed_at                  TIMESTAMP/DATETIME NOT NULL
~~~

Required constraints:

~~~text
PRIMARY KEY(wait_effect_id)

UNIQUE(parent_delivered_wait_effect_id)

UNIQUE(idempotency_key)

INDEX(consultation_id)
INDEX(question_id)
INDEX(delivery_id)
~~~

MySQL and Oracle migrations must be semantically equivalent.

---

# 6. Companion record semantics

Define:

~~~text
ConsultationWaitEffectRecord
~~~

Allowed effect_status for bounded U06:

~~~text
COMMITTED
~~~

Failed attempts are not inserted as fake committed truth.

Failure/audit evidence remains in U06 trace/failure records.

The durable row means:

~~~text
the exact Consultation WAITING business transition committed
~~~

It does not mean:
- user answered;
- Runtime is AWAITING_USER;
- U07 is eligible;
- delivery content is clinically correct.

---

# 7. Canonical wait payload fingerprint

Define:

~~~text
CONSULTATION_WAITING_PAYLOAD_FINGERPRINT
~~~

over:

~~~text
consultation_id
CONSULTATION_WAITING_EFFECT_ID
QUESTION_DELIVERED_WAIT_EFFECT_ID
question_id
delivery_id
expected_prior_lifecycle
expected_prior_row_version
target_lifecycle = WAITING_USER
transition_contract_version
~~~

The fingerprint excludes wall-clock timestamps.

Same effect id + different fingerprint:

~~~text
U06_CONSULTATION_WAITING_REPLAY_CONFLICT
→ fail closed
~~~

---

# 8. Stable idempotency identity

Use the already frozen semantic identity:

~~~text
CONSULTATION_WAITING_IDEMPOTENCY_KEY
=
CONSULTATION_WAITING_EFFECT_ID
+ transition_contract_version
~~~

Physical implementation stores the resulting stable key in the companion table.

No random idempotency key may be generated per retry.

---

# 9. Transition service

Define:

~~~text
ConsultationWaitTransitionService
~~~

Input:

~~~text
ConsultationWaitingTransitionCommand
~~~

Required fields are those frozen by RDP-03 plus:
- canonical payload fingerprint;
- exact idempotency key.

The service owns only:
- current Consultation lifecycle transition;
- companion effect persistence.

It does not own Clinical State or Runtime Thread state.

---

# 10. Repository locking strategy

Add a ConsultationRepository method using a database row lock:

~~~text
findByIdForUpdate(consultation_id)
~~~

with:

~~~text
PESSIMISTIC_WRITE
~~~

The existing JPA @Version remains active.

Reason:
- exact U06 transition must serialize competing wait establishment;
- row_version remains durable evidence of before/after state;
- a different concurrent lifecycle transition must not be overwritten.

---

# 11. Transaction boundary

The following occur in one local database transaction:

~~~text
1. inspect companion effect by wait_effect_id / idempotency_key
2. exact replay reconciliation if existing
3. lock ConsultationRecord
4. verify consultation_id
5. verify row_version = expected_prior_row_version
6. verify lifecycle = ACTIVE
7. verify current_wait_effect_id = null
8. set lifecycle_status = WAITING_USER
9. set current_wait_effect_id = CONSULTATION_WAITING_EFFECT_ID
10. save ConsultationRecord
11. flush / obtain committed row_version
12. insert COMMITTED ConsultationWaitEffectRecord
13. commit transaction
~~~

Because Consultation row and companion effect table are in the same database, they form one local atomic transaction.

This does not create atomicity with Clinical State/P01 or Runtime tables.

---

# 12. Exact replay path

Before attempting a new transition, inspect by:

~~~text
wait_effect_id
and/or
idempotency_key
~~~

If an existing COMMITTED row has:
- same fingerprint;
- same consultation;
- same question;
- same delivery;
- same parent effect;

then require current ConsultationRecord:

~~~text
lifecycle_status = WAITING_USER
current_wait_effect_id = same wait_effect_id
~~~

and return the original committed transition result.

No second lifecycle mutation occurs.

---

# 13. Replay inconsistency

If effect ledger says COMMITTED but ConsultationRecord is not:

~~~text
WAITING_USER
+ same current_wait_effect_id
~~~

then:

~~~text
CONSULTATION_WAIT_STATE_INCONSISTENT
→ reconciliation/failure
→ no blind rewrite
~~~

This case must be durable evidence for RDP-06.

---

# 14. Conflict rules

Fail closed when:

~~~text
Consultation lifecycle != ACTIVE
and not exact same WAITING replay

current_wait_effect_id != null
and not same effect

row_version mismatch

same effect id + changed payload fingerprint

same idempotency key + changed effect/payload

Question/delivery/parent effect mismatch
~~~

In particular:

~~~text
WAITING_USER for another question/effect
!= replay success
~~~

---

# 15. Crash semantics

Because Consultation lifecycle update and wait-effect record commit share one local transaction:

~~~text
crash before transaction commit
→ neither is authoritative

crash after transaction commit before caller response
→ both are authoritative
→ exact replay returns existing commit
~~~

RDP-04 C6 remains the cross-store window:

~~~text
Clinical State delivered child committed
but Consultation transaction not yet committed
~~~

The local Consultation transition itself does not introduce another half-committed row/effect window.

---

# 16. Authoritative read-back

Define:

~~~text
ConsultationWaitReadPort
~~~

Output:

~~~text
ConsultationWaitAuthoritativeView

consultation_id
row_version
lifecycle_status
current_wait_effect_id

wait_effect_record_ref?
parent_delivered_wait_effect_id?
question_id?
delivery_id?
payload_fingerprint?
~~~

WAITING_USER is authoritative for U06 only when:

~~~text
lifecycle_status = WAITING_USER
current_wait_effect_id != null
companion COMMITTED record exists
all identities match
~~~

---

# 17. PROFILE-B environment boundary

The bounded implementation may exercise this schema/service only against:
- isolated non-production/test database;
- synthetic consultation records;
- SyntheticDeliveryScopeAuthorization-compatible fixture scope.

It must not connect to production consultation storage.

RDP-06 proof remains:

~~~text
production_store_write_count = 0
real_phi_count = 0
~~~

The schema design may be production-compatible, but its bounded implementation authorization remains non-production only.

---

# 18. Migration strategy

Future implementation must add matching MySQL and Oracle migrations.

Migration semantics:
- add nullable current_wait_effect_id to clinical_consultation;
- create clinical_consultation_wait_effect;
- add indexes/unique constraints above.

No backfill to WAITING_USER is permitted.

Existing rows remain:

~~~text
current_wait_effect_id = null
~~~

No historical wait effect may be invented.

---

# 19. U01 compatibility

U01 start behavior remains:

~~~text
new ConsultationRecord
lifecycle_status = ACTIVE
current_wait_effect_id = null
~~~

U01 scope/business semantics do not change.

U01 must not generate wait effect records.

---

# 20. Future U07 contract seam

This CA does not implement resume.

It reserves the invariant:

~~~text
future owner exiting WAITING_USER
must consume current_wait_effect_id
and preserve/close effect provenance
~~~

U07 cannot infer pending Question from lifecycle_status alone.

Any future resume design must explicitly define:
- WAITING_USER → ACTIVE or other lifecycle;
- clearing/replacing current_wait_effect_id;
- answer-event binding.

---

# 21. Candidate implementation surface

Future authorization may permit:

~~~text
runtime/u06/wait/
  ConsultationWaitTransitionService
  ConsultationWaitingTransitionCommand
  ConsultationWaitEffectRecord
  ConsultationWaitEffectRepository
  ConsultationWaitReadPort
  ConsultationWaitAuthoritativeView

runtime/u01/
  ConsultationRecord minimal WAITING_USER/currentWaitEffectId extension
  ConsultationRepository locked-read method

db/migration/
  next version: consultation wait effect schema

db/migration-oracle/
  matching Oracle migration
~~~

No U01 semantic-policy change is required.

---

# 22. Verification obligations

~~~text
IRR02-V01 ACTIVE + null current_wait_effect_id is initial state
IRR02-V02 first valid transition produces WAITING_USER + pointer + COMMITTED effect
IRR02-V03 row_version advances exactly once
IRR02-V04 exact replay produces zero second mutation
IRR02-V05 same effect + changed fingerprint fails closed
IRR02-V06 WAITING_USER for different effect conflicts
IRR02-V07 row-version mismatch conflicts
IRR02-V08 crash-before-commit leaves no committed lifecycle/effect
IRR02-V09 crash-after-commit replay returns original result
IRR02-V10 ledger/record inconsistency fails closed
IRR02-V11 synthetic test DB only / production writes zero
IRR02-V12 U01 start remains ACTIVE/null pointer
~~~

---

# 23. Readiness finding disposition

If independent review passes:

~~~text
BF-U06-IRR-02
= PHYSICAL_DESIGN_COMPLETE / PENDING_COMBINED_READINESS_REEVALUATION
~~~

---

# 24. Authorization boundary

This design grants no migration execution, code implementation, production Consultation mutation, U07 activation, merge or real-patient traffic.

---

# 25. Draft verdict

~~~text
CA-U06-IRR-02
= DRAFT / READY_FOR_INDEPENDENT_PHYSICAL_DESIGN_REVIEW

BF-U06-IRR-02
= OPEN / DESIGN_REVIEW_PENDING
~~~
