# U06 Implementation Readiness Re-Evaluation v0.1

> Evaluation type: **post-aggregate implementation-readiness re-evaluation**  
> Evaluated aggregate baseline: **eb8c52a4e2da1feab1297d820fef0f033ac27698**  
> Aggregate semantic PASS head: **d4a709ee20564f123e996a572ac760797a25f593**  
> Aggregate review: **PASS / REFROZEN_FOR_U06_AGGREGATE_BASELINE**  
> Scope: **READINESS ONLY — NO IMPLEMENTATION AUTHORIZATION**  
> This document grants no code implementation, shared-runtime modification, synthetic adapter implementation, direct F1 activation, real C03/D04 activation, external delivery, merge, production, or real-patient authorization.

---

# 1. Executive verdict

The six original U06 readiness-gap families are now contract-complete and aggregate-compatible:

~~~text
BF-U06-RG-01
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-02
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-03
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-04
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-05
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-06
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED
~~~

Aggregate amendments are also closed:

~~~text
AC-U06-01..10
= CLOSED
~~~

However the bounded PROFILE-B implementation target still has three implementation-design blockers.

Therefore:

~~~text
U06 Definition / Business-Semantic Readiness
= READY

U06 Aggregate Contract Compatibility
= READY

PROFILE-B
Bounded Synthetic Structural Non-Production Implementation Readiness
= NOT_READY

Open PROFILE-B readiness blockers
= 3

PROFILE-A
Real Governed Clinical / Patient-Facing Implementation Readiness
= NOT_READY

U06 Implementation Authorization Review
= NOT_PERMITTED_YET

U06 Implementation Authorization
= NOT_GRANTED
~~~

Important:

~~~text
contract complete
!= physical implementation seam selected

physical implementation seam selected
!= implementation authorized

implementation authorized
!= implemented
!= verified
!= merge authorized
!= production authorized
~~~

---

# 2. Evaluated target split

This re-evaluation follows the aggregate handoff and evaluates two targets separately.

## 2.1 PROFILE-B bounded target

~~~text
execution_profile
= SYNTHETIC_STRUCTURAL_NONPROD

delivery_profile
= SYNTHETIC_NONLIVE_DURABLE_DELIVERY

real PHI
= 0

real external I/O
= 0

production store
= 0

real C03 / D04
= excluded

real patient-facing Question content
= excluded

real upstream missing producers
= excluded

direct F1 runtime activation
= disabled

live U07
= excluded

production Scheduler/live routing
= excluded
~~~

The bounded target proves architecture/runtime mechanics only.

## 2.2 PROFILE-A real target

PROFILE-A remains evaluated only against real governed dependencies and patient-facing requirements.

Synthetic fixtures/adapters cannot close PROFILE-A blockers.

---

# 3. Current implementation archaeology

Current repository state still contains no U06 runtime package:

~~~text
diagnosis-service/.../runtime/u06
= absent
~~~

This absence is expected and is not by itself a readiness blocker.

Readiness depends on whether the frozen contracts are sufficiently precise to authorize construction without unresolved architecture decisions.

## 3.1 P01 foundation exists

Current StateCommitter already provides:

~~~text
ADD / REPLACE / REMOVE
base_version
idempotency inspection/reservation/completion
capability authorization port
field permission port
consent port
source validation port
atomic repository commit port
COMMITTED / REJECTED / CONFLICT / FAILED
exact replay of completed same-fingerprint patch
~~~

Current StateCommitter does not support:

~~~text
expected_current_value / TEST CAS
~~~

which is consistent with RDP-03.

## 3.2 Current CDP adapter limitation

Current ClinicalCdpStateRepositoryAdapter navigates an existing JSON-object path.

For nested paths:

~~~text
/patient_state/information_gaps/{gap_id}
/patient_state/questions/{question_id}
~~~

the parent object must already exist.

If an intermediate parent is absent or not an object:

~~~text
UNSUPPORTED_CLINICAL_PATH
~~~

is returned.

The adapter does not create missing intermediate maps.

This matters because U06 requires independently addressable:

~~~text
information_gaps
questions
pending_question
f3_gap_assessment
~~~

under patient_state.

## 3.3 Current P01 policy seams are ports

Current:

~~~text
FieldPermissionPort
CapabilityPolicyPort
SourceValidationPort
~~~

exist as mechanical ports.

The evaluated main baseline does not expose one already-frozen production U06 registration defining:
- exact U06 physical producer string;
- exact U06 P01 state-write capability id/version;
- exact field-permission mapping for U06 F3/Gap/Question/PendingQuestion paths.

## 3.4 Current P05 trace is leaf-oriented

Current:

~~~text
CapabilityCallTraceRecord
CapabilityTraceService
~~~

remain capability-call oriented.

RDP-03/A04 already selected:

~~~text
U06 governed parent trace companion
+
CapabilityCallTraceRecord leaf
~~~

so the remaining work is physical implementation, not a semantic choice.

## 3.5 Current Consultation persistence lacks U06 wait semantics

Current ConsultationRecord has:

~~~text
lifecycle_status
row_version
ACTIVE
~~~

but no current U06 implementation for:

~~~text
WAITING_USER
CONSULTATION_WAITING_EFFECT_ID
QUESTION_DELIVERED_WAIT_EFFECT_ID
question_id
delivery_id
canonical wait payload fingerprint
wait idempotency/result provenance
~~~

Current repository has no Consultation waiting transition service with exact replay semantics.

## 3.6 Current Runtime wait surface is insufficient

Current ClinicalRunRecord has:

~~~text
OPEN
CLOSED
~~~

but no:

~~~text
AWAITING_USER
pending Question binding
wait effect identity
delivery identity
checkpoint identity
resume-window identity
~~~

Current Python checkpoint implementation is explicitly:

~~~text
TEST_ONLY
NON_PRODUCTION
EXECUTION_METADATA_ONLY
~~~

and stores only:

~~~text
run_id
step_name
status
~~~

There is no current durable U06 wait/checkpoint/thread contract implementation.

## 3.7 Delivery infrastructure is absent

There is no current governed U06:

~~~text
DeliveryIntent
DeliveryAuthority
DeliveryAttempt
DeliveryReceipt
DeliveryConfirmationDecision
DeliveryLedger
SyntheticDeliveryScopeAuthorization
~~~

but RDP-04 freezes these semantics in enough detail that storage/entity implementation may remain within the frozen contract, subject to the blockers below.

---

# 4. Required IMP-U06-AGG-01..12 classification

Classification vocabulary:

~~~text
DESIGN_REQUIRED_BEFORE_IMPLEMENTATION
IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT
OUT_OF_SCOPE_FOR_BOUNDED_SLICE
~~~

Final classification in this re-evaluation:

| Impact | Classification | Current readiness interpretation |
|---|---|---|
| IMP-U06-AGG-01 normalized dependency binding fields/adapters | IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT | A01 fixes canonical fields and real/synthetic meaning; local DTO/mapper implementation may proceed after authorization |
| IMP-U06-AGG-02 normalized policy refs | IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT | mode-specific required/optional refs are already frozen |
| IMP-U06-AGG-03 P01 exact field permissions for F3/Gap/Question/PendingQuestion | **DESIGN_REQUIRED_BEFORE_IMPLEMENTATION** | logical paths are frozen, but current CDP parent materialization + exact U06 permission mapping is not |
| IMP-U06-AGG-04 P01 physical producer + state-write capability identity | **DESIGN_REQUIRED_BEFORE_IMPLEMENTATION** | identity classes are separated, but exact registered producer/capability/version and policy binding are not frozen |
| IMP-U06-AGG-05 U06 parent trace / P05 compatibility surface | IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT | aggregate selected parent companion + leaf trace; exact table/class may be implementation detail |
| IMP-U06-AGG-06 Consultation WAITING effect/idempotency/provenance persistence | **DESIGN_REQUIRED_BEFORE_IMPLEMENTATION** | current ConsultationRecord lacks required lifecycle/effect provenance and exact replay mechanism |
| IMP-U06-AGG-07 delivery intent/ledger/receipt/confirmation persistence | IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT | RDP-04 freezes entities/identities/lifecycle/replay sufficiently |
| IMP-U06-AGG-08 Runtime wait checkpoint + Thread AWAITING_USER surface | **DESIGN_REQUIRED_BEFORE_IMPLEMENTATION** | current runtime has no authoritative Thread wait representation or durable checkpoint contract |
| IMP-U06-AGG-09 U07ResumeEligibility projection | IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT | exact eligibility preconditions and identity are frozen; live U07 remains out of scope |
| IMP-U06-AGG-10 synthetic C03/D04/delivery adapters + synthetic scope authorization | IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT | profile, fail-closed scope and zero-I/O semantics are frozen |
| IMP-U06-AGG-11 upstream real source producers | OUT_OF_SCOPE_FOR_BOUNDED_SLICE | reviewed synthetic authoritative fixtures only; real producer implementation may not be claimed |
| IMP-U06-AGG-12 verifier/oracle/fixture/evidence infrastructure | IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT | RDP-06 + aggregate 12 subcases provide executable verification contract |

Because IMP-03 and IMP-04 are one P01 implementation boundary, this review groups them into one blocking design package.

Therefore the four DESIGN_REQUIRED impact rows produce three blocker packages:

~~~text
IRR-B01
= IMP-03 + IMP-04
= P01 U06 State Storage / Permission / Producer Authorization Physical Design

IRR-B02
= IMP-06
= Consultation WAITING Persistence / Idempotency / Provenance Physical Design

IRR-B03
= IMP-08
= Runtime Wait / Checkpoint / Thread AWAITING_USER Physical Design
~~~

---

# 5. BF-U06-IRR-01 — P01 U06 state-write physical design missing

Status:

~~~text
OPEN / BLOCKING PROFILE-B READY
~~~

The semantic contract is frozen, but implementation still needs one reviewed concrete design resolving all of:

~~~text
A. physical storage shape for:
   /patient_state/f3_gap_assessment
   /patient_state/information_gaps/{gap_id}
   /patient_state/questions/{question_id}
   /patient_state/pending_question

B. creation/materialization rule for absent parent maps

C. proof that implementation does not stringify nested objects
   to bypass StatePatch boundary validation

D. exact U06 physical StatePatch producer
   satisfying lower-case producer contract

E. exact U06 state-write capability_id/version

F. exact FieldPermission mapping:
   only U06-authorized F3/Gap/Question/PendingQuestion paths

G. exact SourceValidation mapping:
   RDP-03 owner-derived state source class

H. synthetic PROFILE-B authorization:
   may use synthetic/non-production policy adapters
   without pretending real C03 binding is state-write authority

I. exact patch adapter:
   stable patch/proposal/idempotency identities
   and current ADD-vs-REPLACE behavior
~~~

The current repository cannot safely infer item B.

Example:

~~~text
/patient_state/questions/{question_id}
~~~

cannot be committed through the current CDP adapter if:

~~~text
patient_state.questions
~~~

does not already exist as a map.

A coding implementation must not choose ad hoc:
- preinitialize maps;
- add whole parent map;
- modify ClinicalCdpStateRepositoryAdapter to materialize intermediate parents;
- add a U06-specific repository adapter;

without a reviewed physical design because these choices affect:
- concurrency;
- ADD/REPLACE semantics;
- field permission scope;
- patch fingerprint;
- shared-runtime behavior.

Required next design:

~~~text
CA-U06-IRR-01
P01 U06 State Storage / Permission / Producer Authorization Physical Design
~~~

---

# 6. BF-U06-IRR-02 — Consultation WAITING persistence design missing

Status:

~~~text
OPEN / BLOCKING PROFILE-B READY
~~~

RDP-03/RDP-04 freeze semantic behavior, but current Consultation persistence does not yet define a physical authoritative mechanism for:

~~~text
ACTIVE -> WAITING_USER
~~~

with:

~~~text
CONSULTATION_WAITING_EFFECT_ID
QUESTION_DELIVERED_WAIT_EFFECT_ID
question_id
delivery_id
canonical payload fingerprint
idempotency identity
row-version transition/result
~~~

Current ConsultationRecord only contains:
- lifecycle_status;
- row_version;
- U01 framing fields.

Exact replay requires more than:

~~~text
lifecycle_status == WAITING_USER
~~~

because replay must prove it is the same:
- parent effect;
- Question;
- delivery.

The aggregate contract explicitly permits:
- reviewed ConsultationRecord provenance fields; or
- a companion Consultation lifecycle effect/idempotency record.

That choice is still unresolved.

It affects:
- U01 shared lifecycle schema;
- transaction boundary;
- optimistic concurrency;
- exact replay;
- crash C6 recovery;
- RDP-06 evidence extraction.

Required next design:

~~~text
CA-U06-IRR-02
Consultation WAITING Effect / Idempotency / Provenance Persistence Design
~~~

---

# 7. BF-U06-IRR-03 — Runtime wait/checkpoint physical design missing

Status:

~~~text
OPEN / BLOCKING PROFILE-B READY
~~~

RDP-04 requires:

~~~text
business delivered state authoritative
+
Consultation WAITING authoritative
+
durable compatible checkpoint
→ Thread AWAITING_USER
→ U07ResumeEligibility
~~~

Current Runtime does not provide that physical surface.

Current ClinicalRunRecord supports only:

~~~text
OPEN
CLOSED
~~~

Current checkpoint.py is explicitly test-only and too small to carry the frozen U06 wait identity.

A concrete design must freeze at least:

~~~text
Runtime Thread / Run wait owner

AWAITING_USER representation

U06WaitCheckpoint persistence shape

checkpoint_id stability/replay

question_id
pending_question_ref
QUESTION_SELECTION_EFFECT_ID
QUESTION_DELIVERY_EFFECT_ID
QUESTION_DELIVERED_WAIT_EFFECT_ID
delivery_id
delivery_confirmation_ref
clinical_state_version
CONSULTATION_WAITING_EFFECT_ID

checkpoint compatibility validation

transition ordering:
business states
→ checkpoint
→ AWAITING_USER

crash C7/C8/C9 recovery

exact replay vs new resume window

U07ResumeEligibility projection owner/port

bounded PROFILE-B persistence:
no production runtime store
~~~

Implementation must not simply overload:

~~~text
ClinicalRunRecord.status = OPEN/CLOSED
~~~

with undocumented meanings.

Required next design:

~~~text
CA-U06-IRR-03
Runtime Wait / Checkpoint / Thread AWAITING_USER Physical Design
~~~

---

# 8. Areas sufficient for direct implementation after later authorization

The following do not require another semantic/architecture design before bounded PROFILE-B implementation, provided IRR-01..03 are closed.

## 8.1 U06 consumer / admission types

PASS_FOR_READINESS.

RDP-01 + A01/A02 freeze:
- modes;
- source authorities;
- normalized binding;
- policy refs;
- currentness;
- Safety;
- replay/admission identities.

Exact Java class naming is an implementation detail.

## 8.2 F3 / D04 owner-decision logic

PASS_FOR_READINESS for PROFILE-B.

The bounded profile may implement only deterministic synthetic policy fixtures.

It may not encode new medical policy.

## 8.3 U06 parent trace

PASS_FOR_READINESS.

The parent/leaf split and typed applicability are already frozen.

A U06-local companion persistence model is allowed.

A shared P05 generalized refactor would require separate authorization but is not required for the bounded target.

## 8.4 Delivery ledger and synthetic transport

PASS_FOR_READINESS.

RDP-04 freezes:
- one active effect;
- stable delivery identity;
- intent-before-send;
- attempts;
- receipts;
- confirmation;
- retry/rebinding;
- C1-C9;
- synthetic zero-network scope.

Exact table/class naming is implementation detail.

## 8.5 Synthetic C03 / D04 adapters

PASS_FOR_READINESS.

They are structural fixture adapters only:
- deterministic;
- no PHI;
- no network/model/tool/knowledge;
- no production fallback;
- no Capability Quality Gate claim.

## 8.6 Verification infrastructure

PASS_FOR_READINESS.

RDP-06 + aggregate baseline provide:

~~~text
106 EV
9 CW
3 HG
10 VG
12 U06-AGG-V
~~~

and exact oracle/fixture/environment/evidence governance.

The verifier does not require a new design before coding.

---

# 9. Direct F1 / upstream producer disposition

For bounded PROFILE-B:

~~~text
direct F1 runtime activation
= OUT_OF_SCOPE / DISABLED

A1 real producer
= OUT_OF_SCOPE

POST_F3 real routing producer
= OUT_OF_SCOPE

ClinicalContinuation real producer
= OUT_OF_SCOPE

F3 reassessment real producer
= OUT_OF_SCOPE
~~~

The bounded implementation may use reviewed synthetic authoritative fixtures where RDP-06 permits them.

It must expose no claim that these producers are implemented.

Therefore missing real producers are not PROFILE-B implementation-readiness blockers.

They remain real-loop blockers.

---

# 10. PROFILE-A real target

PROFILE-A remains:

~~~text
NOT_READY
~~~

Independent of the three PROFILE-B physical-design blockers, current real blockers include:

~~~text
A-01
real governed C03 implementation
+ Capability Quality Gate

A-02
approved real D04 policy

A-03
approved F3 owner / Question policies for target clinical scope

A-04
clinically approved patient-facing Question content/rendering

A-05
real delivery policy/adapter
+ privacy/consent/channel governance

A-06
required real upstream producers for selected end-to-end loop

A-07
production runtime/environment authorization

A-08
real-patient clinical evaluation / release governance
~~~

Synthetic PROFILE-B outputs cannot satisfy any A-01..A-08 blocker.

---

# 11. Verification authority after readiness remediation

Closing IRR-01..03 must not change the RDP-06 contract unless a semantic deviation is discovered.

Any future physical-design package must:
- reference aggregate semantic head;
- identify exact shared-runtime files it proposes to modify;
- enter the future authorized shared-runtime manifest;
- preserve 106/9/3/10/12 verification thresholds.

If a physical design discovers a true semantic conflict:

~~~text
stop
→ controlled aggregate amendment
→ independent refreeze
→ readiness re-evaluation again
~~~

Do not resolve semantic conflicts inside implementation code.

---

# 12. Readiness matrix

| Area | PROFILE-B result | PROFILE-A result |
|---|---|---|
| RDP-01 Admission | READY_CONTRACT | READY_CONTRACT |
| RDP-02 Owner/D04 semantics | READY_CONTRACT for synthetic policy | real policy blocked |
| RDP-03 Mutation/Trace semantics | READY_CONTRACT | READY_CONTRACT |
| RDP-04 Delivery semantics | READY_CONTRACT for synthetic | real delivery blocked |
| RDP-05 Dependencies | READY_CONTRACT for synthetic | real C03/D04 blocked |
| RDP-06 Verification | READY_CONTRACT | real-profile verification not activatable |
| Aggregate compatibility | PASS | PASS semantics only |
| P01 U06 physical write design | **BLOCKING** | BLOCKING |
| Consultation WAITING physical design | **BLOCKING** | BLOCKING |
| Runtime wait/checkpoint physical design | **BLOCKING** | BLOCKING |
| Real upstream producers | OUT_OF_SCOPE | BLOCKING as applicable |
| Clinical content / real delivery | OUT_OF_SCOPE | BLOCKING |
| Implementation Readiness | **NOT_READY** | **NOT_READY** |

---

# 13. Readiness blocker count

For the bounded PROFILE-B implementation target:

~~~text
BF-U06-IRR-01
= OPEN / BLOCKING

BF-U06-IRR-02
= OPEN / BLOCKING

BF-U06-IRR-03
= OPEN / BLOCKING

open PROFILE-B readiness blockers
= 3
~~~

No original RG blocker is reopened.

No aggregate AC item is reopened.

---

# 14. Required next work

Do not redesign U06 business semantics.

The next permitted work is a narrow physical-design remediation package:

~~~text
CA-U06-IRR-01
P01 U06 State Storage / Permission / Producer Authorization Physical Design

CA-U06-IRR-02
Consultation WAITING Effect / Idempotency / Provenance Persistence Design

CA-U06-IRR-03
Runtime Wait / Checkpoint / Thread AWAITING_USER Physical Design
~~~

These may be designed:
- in one controlled package with three independently reviewable sections; or
- as three sequential controlled amendments.

They must remain implementation design only.

After all three pass/refreeze:

~~~text
repeat U06 Implementation Readiness Re-Evaluation
~~~

Only if no blocker remains may:

~~~text
PROFILE-B Implementation Readiness
= READY_FOR_IMPLEMENTATION_AUTHORIZATION_REVIEW
~~~

be considered.

---

# 15. Authorization boundary

This re-evaluation does not authorize:

~~~text
U06 runtime/code implementation
P01 shared-runtime modification
P05 shared-runtime modification
Consultation schema modification
Runtime Thread/checkpoint modification

synthetic C03/D04/delivery implementation

direct F1 activation
real upstream producer wiring
real C03/D04
Question content
real external delivery

merge
production
real-patient traffic
~~~

---

# 16. Re-evaluation verdict

~~~text
U06 Implementation Readiness Re-Evaluation
= COMPLETE

U06 Definition / Business-Semantic Readiness
= READY

U06 Aggregate Contract Compatibility
= READY

PROFILE-B Bounded Synthetic Structural Implementation Readiness
= NOT_READY

BF-U06-IRR-01
= OPEN / BLOCKING

BF-U06-IRR-02
= OPEN / BLOCKING

BF-U06-IRR-03
= OPEN / BLOCKING

PROFILE-A Real Governed Implementation Readiness
= NOT_READY

U06 Implementation Authorization Review
= NOT_PERMITTED_YET

U06 Implementation Authorization
= NOT_GRANTED
~~~

Recommended next action:

~~~text
CA-U06-IRR-01..03
Targeted Physical Design Remediation
~~~
