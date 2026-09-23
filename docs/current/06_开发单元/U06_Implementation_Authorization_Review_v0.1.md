# U06 Implementation Authorization Review v0.1

> Review type: **bounded PROFILE-B non-production implementation authorization eligibility review**  
> Proposed Authorization ID: **AUTH-U06-PROFILEB-IMPL-001**  
> Readiness basis: **U06 Post-Physical-Design Implementation Readiness Re-Evaluation / PR #240**  
> Reviewed readiness semantic head: **8122e7c11bd069106d5e99c64331981f22cee7c9**  
> Current readiness status head: **337f2176b39e1454bc848d6ecdeaaafe5a2012a7**  
> Status: **DRAFT / READY_FOR_INDEPENDENT_AUTHORIZATION_REVIEW**  
> This document does not itself grant implementation authorization.

---

# 1. Review question

Whether the current U06 package is sufficiently ready and sufficiently bounded for an explicit repository-owner decision authorizing implementation of:

~~~text
PROFILE-B
SYNTHETIC_STRUCTURAL_NONPROD
~~~

only.

This review does not ask whether:
- PROFILE-A is ready;
- real patient-facing U06 may run;
- production routing may activate;
- merge may occur.

---

# 2. Preconditions

Confirmed:

~~~text
U06 Business-Semantic Readiness
= READY

U06 Aggregate Contract Compatibility
= READY

U06 PROFILE-B Physical Design Readiness
= READY

PROFILE-B Implementation Readiness
= READY_FOR_IMPLEMENTATION_AUTHORIZATION_REVIEW

BF-U06-RG-01..06
= AGGREGATE_COMPATIBILITY_CLOSED

AC-U06-01..10
= CLOSED

BF-U06-IRR-01
= CLOSED_FOR_PROFILE_B_IMPLEMENTATION_READINESS

BF-U06-IRR-02
= CLOSED_FOR_PROFILE_B_IMPLEMENTATION_READINESS

BF-U06-IRR-03
= CLOSED_FOR_PROFILE_B_IMPLEMENTATION_READINESS

BF-U06-POSTIRR-IR-01
= CLOSED
~~~

PROFILE-A remains NOT_READY.

Therefore:

~~~text
AUTH-U06-PROFILEB-IMPL-001
= ELIGIBLE_FOR_AUTHORIZATION_REVIEW

but

U06 Implementation Authorization
= NOT_GRANTED
~~~

until an explicit owner decision is recorded.

---

# 3. Exact proposed authorization shape

If later explicitly authorized:

~~~text
AUTH-U06-PROFILEB-IMPL-001
= AUTHORIZED
/ NON_PRODUCTION_ONLY
/ PROFILE_B_SYNTHETIC_STRUCTURAL_ONLY
/ FROZEN_RDP01_TO_RDP06
/ AGGREGATE_AC01_TO_AC10
/ CA_IRR01_TO_IRR03
/ ZERO_REAL_PHI
/ ZERO_REAL_EXTERNAL_IO
/ ZERO_PRODUCTION_STORE_WRITE
/ NO_REAL_C03_D04
/ NO_REAL_PATIENT_QUESTION_CONTENT
/ NO_DIRECT_F1_RUNTIME_ACTIVATION
/ NO_LIVE_UPSTREAM_PRODUCER_CUTOVER
/ NO_LIVE_U07
/ NO_PRODUCTION_SCHEDULER_ROUTING
/ EXACT_SHARED_RUNTIME_ALLOWLIST_ONLY
/ NO_UNREVIEWED_SHARED_RUNTIME_SEMANTIC_CHANGE
~~~

No broader interpretation is permitted.

---

# 4. Reviewed authorization basis

The authorization package binds these authorities:

~~~text
U06-RDP-01 PASS
U06-RDP-02 PASS
U06-RDP-03 PASS
U06-RDP-04 PASS
U06-RDP-05 PASS
U06-RDP-06 PASS

U06 Aggregate Compatibility Amendment
semantic PASS head
= d4a709ee20564f123e996a572ac760797a25f593

aggregate status head
= eb8c52a4e2da1feab1297d820fef0f033ac27698

CA-U06-IRR-01..03
semantic PASS head
= 9c0893a2892a8b08a9c03ce3a8f214397d893df4

physical-design status head
= 243b4ce6d554f6af0efb3b68ca55331e719cf42a

post-physical readiness semantic PASS head
= 8122e7c11bd069106d5e99c64331981f22cee7c9
~~~

If any semantic authority changes materially after owner authorization:

~~~text
AUTHORIZATION_STALE
→ STOP
→ impact review / re-authorization
~~~

---

# 5. Authorization lineage

The reviewed governance/readiness base is:

~~~text
review/u06-post-physical-readiness-reevaluation-v1

readiness status head
= 337f2176b39e1454bc848d6ecdeaaafe5a2012a7
~~~

Implementation must not branch directly from this pre-owner-decision head.

If owner later chooses AUTHORIZE:

~~~text
1. the owner decision must be recorded in a dedicated decision package;

2. that decision-record commit must descend from:
   337f2176b39e1454bc848d6ecdeaaafe5a2012a7
   plus the independently PASSed authorization review lineage;

3. the decision record must contain:
   AUTH-U06-PROFILEB-IMPL-001 = AUTHORIZED
   exact authorization shape
   exact allowed path manifest
   reviewed authorization provenance;

4. the actual implementation branch point is:
   final owner-authorized decision-record head.
~~~

Recommended implementation branch:

~~~text
impl/u06-profileb-synthetic-structural-v1
~~~

Implementation from a head that does not contain the owner authorization record:

~~~text
AUTHORIZATION_LINEAGE_CHECK = FAIL
~~~

---

# 6. Allowed U06-owned production-source surface

New code may be added only under:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/**
~~~

Allowed U06-owned responsibilities include:
- RDP-01 admission;
- normalized dependency/policy views;
- synthetic C03 structural adapter;
- synthetic D04 policy adapter;
- F3 owner decision;
- Question selection decision;
- K09 proposal formation;
- U06 StatePatch adapter/factory/codec;
- U06 synthetic P01 execution context;
- U06 parent governance trace companion;
- delivery authority/intent/attempt/receipt/confirmation/ledger;
- synthetic delivery adapter;
- SyntheticDeliveryScopeAuthorization;
- Consultation WAITING coordinator/commands/views;
- Runtime wait coordinator;
- U07ResumeEligibility projection only;
- U06 typed outcomes/failure handoff;
- PROFILE-B-only wiring/guards.

U06 code may consume existing shared interfaces but may not redefine their semantics.

---

# 7. Exact shared production-source allowlist

Outside runtime/u06/**, production-source changes are authorized only to these exact existing files:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRepository.java
~~~

and only for the CA-U06-IRR-02 physical changes:
- WAITING_USER lifecycle constant;
- nullable current_wait_effect_id mapping;
- locked Consultation lookup needed by wait transition.

No unrelated U01 semantic change is allowed.

---

# 8. Exact new shared-runtime file allowlist

Only these new shared-runtime files may be added outside runtime/u06/**:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRepository.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRepository.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointService.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadWaitTransitionService.java
~~~

Their semantics are limited to CA-U06-IRR-03:
- ACTIVE;
- WAIT_CHECKPOINTED;
- AWAITING_USER;
- stable checkpoint;
- exact replay;
- conflict/reconciliation.

No modification is authorized to existing:

~~~text
RuntimeBindingRecord
RuntimeBindingService
ClinicalRunRecord
ClinicalRunCoordinator
CanonicalBusinessEventLedger
CanonicalBusinessEventRecord
~~~

unless a new authorization is obtained.

---

# 9. Exact shared runtime explicitly forbidden

The following production/shared files are read-only under this authorization:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/StateCommitter.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/StatePatchBoundaryValidator.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/adapters/ClinicalCdpStateRepositoryAdapter.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/CapabilityCallTraceRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/CapabilityTraceService.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/CapabilityBindingRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/CapabilityBindingRegistry.java
~~~

Also forbidden:
- contracts/releases/** semantic modification;
- packages/python_runtime/checkpoint.py modification for U06 authority;
- U03/U04/U05 production runtime source modifications;
- U07 production runtime implementation;
- Scheduler core semantic modification.

If implementation discovers one is required:

~~~text
STOP
→ shared-runtime impact review
→ controlled amendment
→ explicit additional authorization
~~~

---

# 10. Exact migration authorization

Authorized repository additions:

~~~text
diagnosis-service/src/main/resources/db/migration/V6__add_u06_wait_runtime.sql

diagnosis-service/src/main/resources/db/migration-oracle/V6__add_u06_wait_runtime.sql
~~~

The two migrations may only implement reviewed U06 physical persistence for:

~~~text
clinical_consultation.current_wait_effect_id

clinical_consultation_wait_effect

clinical_runtime_thread_state

clinical_runtime_wait_checkpoint

U06-local delivery / trace / idempotency persistence
required by RDP-04 and RDP-03
~~~

Allowed U06-local persistence may include tables whose ownership is explicitly U06 and whose rows are:
- delivery governance/evidence;
- U06 parent trace;
- U06 idempotency/replay evidence.

The migrations must not:
- alter unrelated clinical tables;
- backfill invented WAITING_USER states;
- write business truth;
- alter existing Clinical State schema semantics;
- activate production behavior.

Migration files may exist in the implementation candidate, but executing them against production is not authorized.

---

# 11. Test surface

Allowed test code:

~~~text
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/**
~~~

Additionally, only when necessary to prove backward compatibility of the two explicitly modified shared files, limited changes may be made to:

~~~text
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u01/U01ConsultationServiceTest.java
~~~

New U06-specific tests may also be added under:

~~~text
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/u06/**
~~~

No unrelated existing test expectation may be weakened to make U06 pass.

---

# 12. Verification tooling / fixture surface

Allowed:

~~~text
tools/u06_nonprod_verification/**

diagnosis-service/src/test/resources/u06/**

.github/workflows/u06-rdp06-authoritative-verification.yml

docs/current/06_开发单元/U06_*Implementation*
docs/current/06_开发单元/U06_*Verification*
docs/current/06_开发单元/U06_*Evidence*
~~~

Verification tooling may produce:
- static oracle;
- synthetic fixture manifest;
- authority manifest;
- evidence builder;
- crash/concurrency harness;
- checksum/artifact manifest.

It may not generate expected results from observed SUT output.

---

# 13. Synthetic-only activation boundary

Authorized implementation may be activated only through:
- explicit construction in tests;
- isolated non-production integration harness;
- an explicit u06-nonprod / equivalent non-production profile that is inactive by default.

Forbidden:

~~~text
default production auto-activation
production endpoint cutover
real patient request routing
production Scheduler route
real recipient endpoint
production credentials
production Clinical State repository
production Consultation/Runtime database during authoritative verification
~~~

If Spring registration is used, structural tests must prove production/default profile does not activate PROFILE-B U06.

---

# 14. P01 authorization boundary

PROFILE-B may use:

~~~text
SyntheticVersionedStateRepository
~~~

through the exact reviewed:

~~~text
U06SyntheticP01ExecutionContext
~~~

with:

~~~text
producer = u06-runtime
capability = u06-state-writer@1.0.0
source = RULE_DERIVED
~~~

Required:

~~~text
state_read_store_ref
=
state_commit_store_ref
=
admitted synthetic_state_store_ref
~~~

Forbidden:
- ClinicalCdpStateRepositoryAdapter writes;
- production CDPRepository writes;
- direct CDPManager.updateCDP;
- dependency binding used as P01 capability identity;
- automatic parent-map materialization in shared CDP adapter.

---

# 15. Consultation WAITING authorization boundary

Authorized:
- current_wait_effect_id field;
- WAITING_USER lifecycle value;
- companion wait-effect ledger;
- locked transition;
- post-lock replay recheck;
- JPA committed row-version evidence;
- exact replay/conflict handling.

Not authorized:
- U07 resume transition;
- WAITING_USER → ACTIVE;
- cancellation/expiry semantics;
- production/live wait establishment.

---

# 16. Runtime wait authorization boundary

Authorized:
- RuntimeThreadStateRecord;
- RuntimeWaitCheckpointRecord;
- ACTIVE;
- WAIT_CHECKPOINTED;
- AWAITING_USER;
- checkpoint reservation;
- exact replay;
- C7/C8/C9 recovery;
- U07ResumeEligibility projection.

Not authorized:
- live U07 execution;
- USER_ANSWER processing;
- Runtime resume;
- U02 re-entry;
- changing ClinicalRunRecord OPEN/CLOSED semantics;
- using Python test checkpoint as U06 authority.

---

# 17. Delivery authorization boundary

Authorized PROFILE-B delivery is:

~~~text
SYNTHETIC_NONLIVE_DURABLE_DELIVERY
~~~

with:
- durable intent;
- stable delivery effect/delivery_id/idempotency;
- synthetic attempts/receipts;
- confirmation resolver;
- one-active-effect authority;
- rebinding rules;
- delivery ledger;
- zero-network synthetic adapter;
- SyntheticDeliveryScopeAuthorization;
- C1-C9 recovery.

Required:

~~~text
external_side_effect = false
delivery_evidence_type = SYNTHETIC
~~~

Forbidden:
- SMS;
- email;
- push;
- voice;
- websocket to real user;
- external provider calls;
- real recipient identifiers.

---

# 18. Real dependency/content boundary

Still prohibited:
- real C03 invocation;
- real D04 clinical policy activation;
- real model/tool/knowledge calls;
- real patient-facing Question content;
- claiming adult_respiratory_v1 is an active real C03;
- replacing missing real dependency with synthetic fallback in a real profile.

Synthetic adapters are permitted only because execution_profile is PROFILE-B.

---

# 19. Upstream and downstream boundary

Allowed:
- synthetic authoritative source fixtures;
- explicit internal PROFILE-B invocation.

Still prohibited:
- direct F1 live/runtime activation;
- real A1_PRE_READINESS producer cutover;
- real POST_F3_SAFETY_BARRIER producer cutover;
- real CLINICAL_CONTINUATION routing;
- real F3 reassessment producer;
- production Scheduler route;
- live U07;
- live alternate clinical route on delivery failure.

Missing real producers remain out of bounded scope and must not be claimed implemented.

---

# 20. Exact verification obligations

Implementation candidate is not complete because code compiles.

Future authoritative verification must satisfy at one exact implementation target:

~~~text
106 / 106 U06-EV PASS
9 / 9 U06-CW PASS
3 / 3 U06-HG PASS
10 / 10 U06-VG PASS
12 / 12 U06-AGG-V PASS

IRR01-V01..IRR01-V16
= 16 / 16 PASS

IRR02-V01..IRR02-V16
= 16 / 16 PASS

IRR03-V01..IRR03-V17
= 17 / 17 PASS

total exact physical cases
= 49 / 49 PASS
~~~

U06-PHY-V is only a manifest/group label.

Every physical evidence row must use the exact IRRxx-Vyy identity and bind its independently reviewed CA semantic digest.

---

# 21. Hard verification totals

Required:

~~~text
contract_expectation_gap_count = 0

real_phi_count = 0
real_external_delivery_count = 0
real_model_call_count = 0
real_tool_call_count = 0
real_knowledge_call_count = 0

production_store_write_count = 0
unreviewed_shared_runtime_change_count = 0
external_call_spy_observed_count = 0

oracle review = PASS
fixture review = PASS
environment isolation = PASS
shared-runtime change manifest = PASS

regression failures = 0
regression errors = 0
unexpected skips = 0

artifact integrity = PASS
independent evidence review = PASS
~~~

Network egress must be denied-by-default or equivalently isolated.

---

# 22. Authorized shared-runtime change manifest

Before authoritative CI, the implementation candidate must produce:

~~~text
U06_AUTHORIZED_SHARED_RUNTIME_CHANGE_MANIFEST
~~~

bound to:
- exact implementation base SHA;
- exact target SHA;
- this authorization decision;
- exact allowed files/path patterns.

Observed files must be computed from Git diff.

Unexpected shared-runtime files:

~~~text
= []
~~~

is mandatory.

Implementation may not self-report the diff.

---

# 23. Required regression scope

At minimum:
- compile/build;
- Foundation runtime regression;
- U01;
- U02;
- U03;
- U04;
- U05;
- StateCommitter/state boundary tests;
- migration/schema tests where applicable;
- repository-wide applicable diagnosis-service tests.

Existing expected skips must be enumerated.

No unrelated failing test may be waived by this authorization.

---

# 24. Post-implementation governance order

If later authorized, after implementation candidate exists:

~~~text
1. exact-head implementation/code-boundary review

2. shared-runtime change-manifest review

3. independent expectation-oracle review

4. independent fixture-manifest review

5. freeze reviewed oracle/fixture/authority digests

6. authoritative exact-head CI verification
   under isolated PROFILE-B environment

7. independent evidence-only review

8. combined implementation/evidence review

9. explicit U06 implementation verification closure
~~~

Only after verified closure may a separate Merge Authorization Review begin.

---

# 25. Merge boundary

This authorization does not authorize merge.

After verified implementation closure:

~~~text
Merge Authorization Review
→ explicit repository-owner merge authorization
→ STANDARD MERGE COMMIT ONLY
→ post-merge verification
~~~

Forbidden:
- auto merge;
- squash;
- rebase.

---

# 26. Authorization invalidation conditions

Authorization becomes stale if:
- any RDP-01..06 semantic contract changes;
- AC-U06-01..10 changes;
- CA-U06-IRR-01..03 semantic design changes;
- a new readiness blocker is found;
- implementation requires a forbidden/shared file;
- implementation requires changing StateCommitter/CDP adapter/P05 semantics;
- implementation requires real C03/D04/content/delivery;
- implementation requires live upstream/downstream routing;
- verification expectations require semantic alteration.

Then:

~~~text
STOP
→ governance impact review
→ amendment / re-authorization
~~~

---

# 27. Authorization review decision

Based on current readiness and the bounded exact scope:

~~~text
U06 Implementation Authorization Review
= PROPOSED_PASS

AUTH-U06-PROFILEB-IMPL-001
= PROPOSED_ELIGIBLE_FOR_EXPLICIT_REPOSITORY_OWNER_DECISION

U06 Implementation Authorization
= NOT_GRANTED
~~~

Owner decision remains a later explicit action.

---

# 28. Draft verdict

~~~text
U06 Implementation Authorization Review
= DRAFT / READY_FOR_INDEPENDENT_AUTHORIZATION_REVIEW

AUTH-U06-PROFILEB-IMPL-001
= NOT_DECIDED

Implementation
= NOT_AUTHORIZED
~~~
