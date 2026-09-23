# U06 IRR Targeted Physical Design Remediation v0.1

> Parent: **U06 Implementation Readiness Re-Evaluation / PR #238**  
> Readiness baseline: **498d8ae086c7d9fd0e33017360d07ea1ba89ee73**  
> Scope: **CA-U06-IRR-01..03 / PHYSICAL DESIGN ONLY**  
> Status: **PASS / PHYSICAL_DESIGN_REMEDIATION_COMPLETE**  
> No implementation authorization is granted.

---

# 1. Purpose

The post-aggregate readiness review identified exactly three bounded PROFILE-B pre-coding blockers:

~~~text
BF-U06-IRR-01
P01 U06 State Storage / Permission / Producer Authorization

BF-U06-IRR-02
Consultation WAITING Effect / Idempotency / Provenance Persistence

BF-U06-IRR-03
Runtime Wait / Checkpoint / Thread AWAITING_USER
~~~

This remediation package selects one concrete physical architecture for each blocker without reopening U06 business semantics.

---

# 2. Package contents

~~~text
CA-U06-IRR-01
docs/current/06_开发单元/
U06_CA_IRR01_P01_State_Storage_Permission_Producer_Physical_Design_v0.1.md

CA-U06-IRR-02
docs/current/06_开发单元/
U06_CA_IRR02_Consultation_WAITING_Persistence_Design_v0.1.md

CA-U06-IRR-03
docs/current/06_开发单元/
U06_CA_IRR03_Runtime_Wait_Checkpoint_Thread_Design_v0.1.md
~~~

All three are independently reviewable and all three must PASS before the combined readiness re-evaluation.

---

# 3. Selected architecture at a glance

## 3.1 CA-U06-IRR-01

PROFILE-B StatePatch path:

~~~text
reviewed synthetic fixture
→ SyntheticVersionedStateRepository
→ U06StateReadPort
→ U06StateWriteAuthorityGuard
→ U06StatePatchFactory
→ real StateCommitter mechanical core
→ synthetic repository commit
→ authoritative synthetic read-back
~~~

Frozen PROFILE-B physical identities:

~~~text
producer = u06-runtime
state-write capability = u06-state-writer@1.0.0
source = RULE_DERIVED
~~~

No production CDP adapter write.

## 3.2 CA-U06-IRR-02

Consultation WAITING path:

~~~text
ConsultationRecord
+ current_wait_effect_id

clinical_consultation_wait_effect
immutable companion effect ledger

one local DB transaction:
ACTIVE
→ WAITING_USER
+ effect ledger COMMITTED
~~~

The companion ledger provides exact replay provenance.

## 3.3 CA-U06-IRR-03

Runtime wait path:

~~~text
RuntimeBindingRecord
= immutable thread identity

RuntimeThreadStateRecord
= mutable Thread ACTIVE/AWAITING_USER

RuntimeWaitCheckpointRecord
= durable wait checkpoint

business wait authoritative
→ checkpoint commit
→ Thread AWAITING_USER commit
→ U07ResumeEligibility projection
~~~

Python test checkpoint remains non-authoritative.

---

# 4. Cross-CA physical chain

The three designs compose as:

~~~text
RDP-03 K09 proposal
↓
CA-IRR-01
P01 synthetic Clinical State commit/read-back
↓
RDP-04 delivery confirmation
↓
RDP-03 delivered Clinical State child
using CA-IRR-01
↓
CA-IRR-02
Consultation WAITING transaction/read-back
↓
CA-IRR-03
Runtime wait checkpoint
↓
Thread AWAITING_USER
↓
U07ResumeEligibility projection
~~~

No stage may skip the authoritative read-back of the previous business owner.

---

# 5. Store/authority separation

~~~text
SyntheticVersionedStateRepository
= PROFILE-B Clinical State store

clinical_consultation
= Consultation lifecycle store

clinical_consultation_wait_effect
= Consultation wait provenance store

clinical_runtime_wait_checkpoint
= Runtime checkpoint store

clinical_runtime_thread_state
= Runtime Thread state

U06 delivery ledger
= non-clinical delivery side-effect store

U06 parent trace
= governance/evidence store
~~~

None may be treated as another store's truth.

---

# 6. Atomicity boundaries

Selected physical atomicity:

~~~text
P01 Clinical State child
= atomic only inside StateCommitter repository commit

Consultation WAITING
= Consultation row + wait-effect ledger
  one local DB transaction

Runtime checkpoint
= own durable commit

Runtime Thread transition
= later separate durable commit
~~~

There is deliberately no claim of one global transaction across all stores.

Cross-store consistency uses stable effect identities + reconciliation.

---

# 7. PROFILE-B only

The package closes physical design only for:

~~~text
SYNTHETIC_STRUCTURAL_NONPROD
~~~

It does not make PROFILE-A READY.

Explicitly still out of scope:
- real CDP U06 storage/materialization;
- real P01 U06 authorization;
- real C03/D04;
- real Question content;
- real delivery;
- live upstream producers;
- live U07;
- production runtime.

---

# 8. Shared-runtime change candidates

The package identifies potential future shared changes but does not authorize them.

CA-IRR-01:
- ideally no StateCommitter/core/CDP adapter semantic change for PROFILE-B.

CA-IRR-02:
- ConsultationRecord adds WAITING_USER + current_wait_effect_id;
- ConsultationRepository adds locked read;
- new wait-effect persistence;
- MySQL + Oracle migrations.

CA-IRR-03:
- new RuntimeThreadState and RuntimeWaitCheckpoint persistence;
- MySQL + Oracle migrations;
- Runtime services.

These exact files/paths must be reviewed again during Implementation Authorization and captured in the RDP-06 authorized shared-runtime change manifest.

---

# 9. No semantic reopen

This package preserves:
- RDP-01 admission;
- RDP-02 F3/D04/Question semantics;
- RDP-03 effect/mutation/replay semantics;
- RDP-04 delivery/wait ordering;
- RDP-05 real/synthetic dependency separation;
- RDP-06 evidence thresholds;
- AC-U06-01..10.

If implementation requires a semantic deviation:

~~~text
STOP
→ aggregate controlled amendment
→ re-freeze
→ readiness re-evaluation
~~~

---

# 10. Combined acceptance rule

The package passes only when:

~~~text
CA-U06-IRR-01 = PASS
CA-U06-IRR-02 = PASS
CA-U06-IRR-03 = PASS

and

combined cross-CA review = PASS
~~~

Then:

~~~text
BF-U06-IRR-01
BF-U06-IRR-02
BF-U06-IRR-03

→ PHYSICAL_DESIGN_COMPLETE
  / PENDING_COMBINED_READINESS_REEVALUATION
~~~

This still does not declare PROFILE-B READY.

---

# 11. Next governance step after PASS

~~~text
U06 Implementation Readiness Re-Evaluation
(post physical-design remediation)
~~~

That review must confirm:
- no remaining DESIGN_REQUIRED_BEFORE_IMPLEMENTATION impact;
- physical designs are mutually compatible;
- future implementation scope is exact;
- RDP-06 verifier authority can cover all new physical seams.

Only then may:

~~~text
PROFILE-B Implementation Readiness
= READY_FOR_IMPLEMENTATION_AUTHORIZATION_REVIEW
~~~

be considered.

---

# 12. Authorization boundary

No code implementation, schema migration execution, synthetic adapter implementation, shared-runtime modification, merge, production or real-patient traffic is authorized here.

---

# 13. Independent Combined Review Provenance

~~~text
Initial Independent Combined Review
= REVISE_REQUIRED
review_id = 5288434120
reviewed_head = 0533fa05a896b8588983371038218902a274d208

Targeted Independent Combined Re-Review
= PASS
review_id = 5288454931
reviewed_head = 9c0893a2892a8b08a9c03ce3a8f214397d893df4
~~~

# 14. Final physical-design verdict

~~~text
CA-U06-IRR-01
= PASS

CA-U06-IRR-02
= PASS

CA-U06-IRR-03
= PASS

CA-U06-IRR-01..03 Targeted Physical Design Remediation
= PASS

BF-U06-IRR-01
= PHYSICAL_DESIGN_COMPLETE / PENDING_COMBINED_READINESS_REEVALUATION

BF-U06-IRR-02
= PHYSICAL_DESIGN_COMPLETE / PENDING_COMBINED_READINESS_REEVALUATION

BF-U06-IRR-03
= PHYSICAL_DESIGN_COMPLETE / PENDING_COMBINED_READINESS_REEVALUATION

U06 PROFILE-B Implementation Readiness
= NOT_YET_REEVALUATED_AFTER_PHYSICAL_DESIGN

U06 Implementation Authorization
= NOT_GRANTED
~~~

Next governance step:

~~~text
U06 Implementation Readiness Re-Evaluation
(post physical-design remediation)
~~~

No code implementation, schema migration execution, shared-runtime modification, synthetic adapter implementation, merge, production, or real-patient authorization is granted.
