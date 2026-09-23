# U06 Implementation Authorization Decision v0.1

> Decision ID: **AUTH-U06-PROFILEB-IMPL-001**  
> Decision status: **READY_FOR_OWNER_DECISION / NOT_DECIDED**  
> Authorization Review: **PASS / PR #241**  
> Reviewed authorization semantic head: **50db54beefe670b396b9b1d0044196ac49332f34**  
> Authorization Review status head: **60401646a35819e80473aff3eddc6e82ee767275**  
> Scope: **explicit repository-owner decision only**  
> This package does not itself grant implementation authorization.

---

# 1. Owner decision question

Whether to authorize implementation of the bounded U06 PROFILE-B slice:

~~~text
PROFILE-B
= SYNTHETIC_STRUCTURAL_NONPROD
~~~

under:

~~~text
AUTH-U06-PROFILEB-IMPL-001
~~~

This is not:
- production authorization;
- merge authorization;
- PROFILE-A authorization;
- live routing authorization;
- real-patient authorization.

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

U06 Implementation Authorization Review
= PASS

AUTH-U06-PROFILEB-IMPL-001
= ELIGIBLE_FOR_EXPLICIT_REPOSITORY_OWNER_DECISION
~~~

Open PROFILE-B pre-coding readiness blockers:

~~~text
0
~~~

PROFILE-A remains:

~~~text
NOT_READY
~~~

---

# 3. Exact authorization shape if approved

If owner chooses AUTHORIZE:

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

# 4. Reviewed semantic/governance basis

Bound authority:

~~~text
U06-RDP-01 = PASS
U06-RDP-02 = PASS
U06-RDP-03 = PASS
U06-RDP-04 = PASS
U06-RDP-05 = PASS
U06-RDP-06 = PASS

Aggregate Compatibility
semantic PASS head
= d4a709ee20564f123e996a572ac760797a25f593

CA-U06-IRR-01..03
semantic PASS head
= 9c0893a2892a8b08a9c03ce3a8f214397d893df4

Post-physical Readiness
semantic PASS head
= 8122e7c11bd069106d5e99c64331981f22cee7c9

Implementation Authorization Review
semantic PASS head
= 50db54beefe670b396b9b1d0044196ac49332f34
~~~

Status/provenance heads do not replace these semantic PASS heads.

---

# 5. Actual implementation lineage

Current reviewed decision-package base:

~~~text
decision/u06-profileb-implementation-authorization-v1
descends from
60401646a35819e80473aff3eddc6e82ee767275
~~~

If owner chooses AUTHORIZE:

~~~text
1. this decision document must be updated to record:
   AUTH-U06-PROFILEB-IMPL-001 = AUTHORIZED;

2. the owner-authorized decision-record commit must descend from
   the independently reviewed decision-package head;

3. the final owner-authorized decision-record commit becomes:
   IMPLEMENTATION_BASE_SHA;

4. implementation branch must descend from that commit.
~~~

Authorized implementation branch name:

~~~text
impl/u06-profileb-synthetic-structural-v1
~~~

Implementation from any earlier pre-owner-decision head:

~~~text
AUTHORIZATION_LINEAGE_CHECK
= FAIL
~~~

---

# 6. Authorization-Time Allowed Change Manifest

The owner decision binds:

~~~text
U06_AUTHORIZATION_ALLOWED_CHANGE_MANIFEST_V0_1
~~~

to:
- AUTH-U06-PROFILEB-IMPL-001;
- reviewed authorization semantic head;
- owner decision record;
- final implementation base SHA.

## 6.1 Allowed new U06 production-source root

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/**
~~~

## 6.2 Allowed existing production-source modifications

Exactly:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRepository.java
~~~

Only CA-U06-IRR-02 changes are allowed.

## 6.3 Allowed exact new shared-runtime files

Exactly:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRepository.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRepository.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointService.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadWaitTransitionService.java
~~~

## 6.4 Allowed migrations

Exactly:

~~~text
diagnosis-service/src/main/resources/db/migration/V6__add_u06_wait_runtime.sql

diagnosis-service/src/main/resources/db/migration-oracle/V6__add_u06_wait_runtime.sql
~~~

Existing-table ALTER allowed exactly:

~~~text
clinical_consultation
→ ADD nullable current_wait_effect_id only
~~~

New shared/runtime tables exactly:

~~~text
clinical_consultation_wait_effect
clinical_runtime_thread_state
clinical_runtime_wait_checkpoint
~~~

New U06-owned tables exactly:

~~~text
u06_governed_execution_trace
u06_delivery_authority
u06_delivery_intent
u06_delivery_attempt
u06_delivery_receipt
u06_delivery_confirmation
u06_delivery_ledger
~~~

No other table/ALTER is authorized.

## 6.5 Allowed test/tool/workflow surfaces

~~~text
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/**

diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/u06/**

diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u01/U01ConsultationServiceTest.java

diagnosis-service/src/test/resources/u06/**

tools/u06_nonprod_verification/**

.github/workflows/u06-rdp06-authoritative-verification.yml

docs/current/06_开发单元/u06_implementation/**
~~~

No existing frozen U06 authority document may be modified during implementation.

---

# 7. Explicit forbidden shared/core files

Read-only under this authorization:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/StateCommitter.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/StatePatchBoundaryValidator.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/adapters/ClinicalCdpStateRepositoryAdapter.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/CapabilityCallTraceRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/CapabilityTraceService.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/CapabilityBindingRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/CapabilityBindingRegistry.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeBindingRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeBindingService.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/ClinicalRunRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/ClinicalRunCoordinator.java
~~~

Forbidden roots for semantic modification:

~~~text
contracts/releases/**
packages/python_runtime/**
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u03/**
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u04/**
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/**
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u07/**
~~~

If implementation needs one of these:

~~~text
STOP
→ authorization invalid for requested change
→ impact review / amendment / additional authorization
~~~

---

# 8. Authorized U06 implementation scope

If approved, implementation may include:
- RDP-01 consumer/admission;
- normalized real/synthetic dependency identity handling;
- synthetic source fixtures;
- synthetic C03 structural result adapter;
- synthetic F3 owner policy;
- synthetic D04 policy;
- deterministic Question selection;
- K09 proposal formation;
- U06 parent trace;
- PROFILE-B StatePatch factory/codec/guard;
- SyntheticVersionedStateRepository-based U06 P01 context;
- Question/Gap/PendingQuestion state lifecycle;
- delivery authority/intent/attempt/receipt/confirmation/ledger;
- zero-network synthetic delivery;
- Consultation WAITING transaction;
- Runtime checkpoint reservation;
- WAIT_CHECKPOINTED;
- AWAITING_USER;
- U07ResumeEligibility projection;
- typed U06 delivery outcomes;
- failure handoffs;
- verifier/oracle/fixture/evidence infrastructure.

No medical policy/content may be invented.

---

# 9. P01 boundary

Authorized PROFILE-B P01 identity:

~~~text
producer
= u06-runtime

capability
= u06-state-writer@1.0.0

source
= RULE_DERIVED
~~~

Repository:

~~~text
SyntheticVersionedStateRepository
~~~

Required:

~~~text
state_read_store_ref
=
state_commit_store_ref
=
admitted synthetic_state_store_ref
~~~

Production ClinicalCdpStateRepositoryAdapter write count must remain zero.

---

# 10. Consultation boundary

Authorized:
- ACTIVE → WAITING_USER;
- current_wait_effect_id;
- companion immutable wait-effect ledger;
- PESSIMISTIC_WRITE;
- post-lock replay recheck;
- exact idempotency/conflict handling.

Not authorized:
- resume;
- WAITING_USER → ACTIVE;
- U15 cancellation/expiry implementation;
- production wait transition.

---

# 11. Runtime boundary

Authorized:
- RuntimeThreadStateRecord;
- RuntimeWaitCheckpointRecord;
- ACTIVE;
- WAIT_CHECKPOINTED;
- AWAITING_USER;
- checkpoint reservation;
- exact replay/recovery;
- U07ResumeEligibility projection.

Not authorized:
- USER_ANSWER;
- Runtime resume;
- U02 re-entry;
- live U07;
- changing ClinicalRunRecord semantics.

---

# 12. Delivery boundary

Authorized delivery profile:

~~~text
SYNTHETIC_NONLIVE_DURABLE_DELIVERY
~~~

Required:
- zero external network;
- synthetic receipt;
- synthetic confirmation;
- one-active-effect authority;
- no blind resend;
- C1-C9 recovery;
- SyntheticDeliveryScopeAuthorization;
- real recipient forbidden.

Synthetic confirmation proves structural mechanics only.

---

# 13. Upstream / downstream boundary

Still disabled/out of bounded scope:
- direct F1 runtime activation;
- real A1 producer;
- real POST_F3 routing producer;
- real ClinicalContinuation producer;
- real F3 reassessment producer;
- production Scheduler;
- live U07;
- live alternate clinical routing.

Reviewed synthetic authoritative fixtures may exercise U06 consumer behavior but do not implement those producers.

---

# 14. PROFILE-A exclusion

This authorization does not apply to:

~~~text
PROFILE-A
REAL_GOVERNED / PATIENT-FACING
~~~

Still blocked:
- real C03 + Quality Gate;
- real D04;
- real F3/Question policy releases;
- approved patient-facing content;
- real delivery/privacy/consent;
- real upstream producers;
- real CDP U06 storage/materialization;
- production runtime;
- clinical evaluation/release.

---

# 15. Mandatory verification authority

Implementation verification must satisfy exactly:

~~~text
106 / 106 U06-EV PASS
9 / 9 U06-CW PASS
3 / 3 U06-HG PASS
10 / 10 U06-VG PASS
12 / 12 U06-AGG-V PASS

IRR01-V01..V16
= 16 / 16 PASS

IRR02-V01..V16
= 16 / 16 PASS

IRR03-V01..V17
= 17 / 17 PASS
~~~

Physical case count:

~~~text
49 unique exact IRR case IDs
~~~

No anonymous replacement namespace is accepted.

---

# 16. Hard evidence boundary

Required zeros:

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
~~~

Required PASS:
- oracle review;
- fixture review;
- network/environment isolation;
- shared-runtime diff manifest;
- regression;
- artifact integrity;
- independent evidence review.

---

# 17. Post-implementation review order

Mandatory:

~~~text
1. exact-head implementation/code-boundary review

2. authorization-time vs observed Git-diff manifest review

3. independent expectation-oracle review

4. independent fixture-manifest review

5. freeze reviewed oracle/fixture/authority digests

6. authoritative isolated exact-head verification

7. independent evidence-only review

8. combined implementation/evidence review

9. explicit implementation verification closure
~~~

No step automatically authorizes merge.

---

# 18. Merge boundary

Implementation authorization does not authorize merge.

Future merge requires:

~~~text
Merge Authorization Review
→ explicit owner merge authorization
→ STANDARD MERGE COMMIT ONLY
→ post-merge verification
~~~

Forbidden:
- automatic merge;
- squash;
- rebase.

---

# 19. Authorization invalidation

If implementation requires:
- a semantic RDP/aggregate/CA change;
- a non-allowlisted shared file;
- another migration object;
- StateCommitter/CDP adapter/P05 semantic change;
- real dependency/content/delivery;
- live upstream/downstream route;
- weakened verification expectation;

then:

~~~text
STOP
→ AUTHORIZATION_STALE_OR_INSUFFICIENT
→ governance review
~~~

---

# 20. Owner choices

Repository owner may choose exactly one:

~~~text
AUTHORIZE
REVISE
REJECT
~~~

## AUTHORIZE

Sets:

~~~text
AUTH-U06-PROFILEB-IMPL-001
= AUTHORIZED
~~~

with the exact bounded shape in this decision package.

Only after the owner-authorized decision-record commit exists may:

~~~text
impl/u06-profileb-synthetic-structural-v1
~~~

be created from that commit.

## REVISE

No implementation begins.

The decision/review package must be revised and independently re-reviewed.

## REJECT

No implementation begins.

PROFILE-B remains implementation-ready but unauthorized.

---

# 21. Current decision state

Until explicit owner choice:

~~~text
AUTH-U06-PROFILEB-IMPL-001
= NOT_DECIDED

U06 Implementation Authorization
= NOT_GRANTED

Implementation
= NOT_AUTHORIZED
~~~

---

# 22. Decision-package gate

Independent Gate Review:

~~~text
PR #242
review_id = 5288639260
verdict = PASS
reviewed_head = c4cdfcb01e023d427d33fff66891397395db2022
~~~

Gate proved:
- exact authorization basis;
- allowed-change manifest;
- forbidden surfaces;
- migration object allowlist;
- owner-lineage rule;
- verification thresholds;
- PROFILE-B/PROFILE-A separation;
- no merge/live/production leakage;
- owner agency preserved.

Current:

~~~text
U06 Implementation Authorization Decision Package
= PASS / READY_FOR_OWNER_DECISION

AUTH-U06-PROFILEB-IMPL-001
= NOT_DECIDED

U06 Implementation Authorization
= NOT_GRANTED

Implementation
= NOT_AUTHORIZED
~~~

Owner choices:

~~~text
AUTHORIZE
REVISE
REJECT
~~~

This status synchronization does not itself authorize implementation.
