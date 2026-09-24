# U06 Authorization-Time Allowed Change Manifest v0.1

> Authorization ID: `AUTH-U06-PROFILEB-IMPL-001`  
> Scope: bounded `PROFILE-B / SYNTHETIC_STRUCTURAL_NONPROD` only  
> Authorization Review semantic head: `50db54beefe670b396b9b1d0044196ac49332f34`  
> Decision-package gate semantic head: `c4cdfcb01e023d427d33fff66891397395db2022`  
> Readiness status head: `337f2176b39e1454bc848d6ecdeaaafe5a2012a7`  
> Aggregate semantic head: `d4a709ee20564f123e996a572ac760797a25f593`  
> Physical-design semantic head: `9c0893a2892a8b08a9c03ce3a8f214397d893df4`  
> Post-physical readiness semantic head: `8122e7c11bd069106d5e99c64331981f22cee7c9`  
> Owner decision record head: `PENDING_OWNER_AUTHORIZATION_COMMIT`  
> Implementation base: `FINAL_OWNER_AUTHORIZED_DECISION_RECORD_HEAD`  
> Implementation branch: `impl/u06-profileb-synthetic-structural-v1`

---

# 1. Allowed new production-source root

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/**
~~~

---

# 2. Allowed existing production-source modifications

Exactly:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRepository.java
~~~

Allowed semantics are limited to CA-U06-IRR-02:
- add `WAITING_USER`;
- add nullable `current_wait_effect_id` mapping;
- add locked Consultation lookup for the wait transition.

No unrelated U01 semantic change is allowed.

---

# 3. Allowed exact new shared-runtime files

Exactly:

~~~text
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRepository.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRecord.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRepository.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointService.java

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadWaitTransitionService.java
~~~

Allowed semantics are limited to CA-U06-IRR-03:
- `ACTIVE`;
- `WAIT_CHECKPOINTED`;
- `AWAITING_USER`;
- stable checkpoint;
- exact replay;
- conflict/reconciliation;
- checkpoint reservation;
- transition to `AWAITING_USER`.

---

# 4. Allowed migration files

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

New shared/runtime tables allowed exactly:

~~~text
clinical_consultation_wait_effect
clinical_runtime_thread_state
clinical_runtime_wait_checkpoint
~~~

New U06-owned tables allowed exactly:

~~~text
u06_governed_execution_trace
u06_delivery_authority
u06_delivery_intent
u06_delivery_attempt
u06_delivery_receipt
u06_delivery_confirmation
u06_delivery_ledger
~~~

No other table creation or existing-table ALTER is authorized.

---

# 5. Allowed tests, fixtures, tools, workflow, implementation records

~~~text
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/**

diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/u06/**

diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u01/U01ConsultationServiceTest.java

diagnosis-service/src/test/resources/u06/**

tools/u06_nonprod_verification/**

.github/workflows/u06-rdp06-authoritative-verification.yml

docs/current/06_开发单元/u06_implementation/**
~~~

The implementation-only document directory may contain:
- implementation status/review records;
- verifier/evidence manifests;
- implementation verification closure records;
- sanitized accepted-evidence snapshots.

---

# 6. Explicit forbidden existing files

Read-only:

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

---

# 7. Forbidden semantic-modification roots

~~~text
contracts/releases/**

packages/python_runtime/**

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u03/**

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u04/**

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/**

diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u07/**
~~~

Read-only imports/consumption remain allowed where already governed.

---

# 8. Frozen implementation profile

~~~text
NON_PRODUCTION_ONLY
PROFILE_B_SYNTHETIC_STRUCTURAL_ONLY

ZERO_REAL_PHI
ZERO_REAL_EXTERNAL_IO
ZERO_PRODUCTION_STORE_WRITE

NO_REAL_C03_D04
NO_REAL_PATIENT_QUESTION_CONTENT

NO_DIRECT_F1_RUNTIME_ACTIVATION
NO_LIVE_UPSTREAM_PRODUCER_CUTOVER
NO_LIVE_U07
NO_PRODUCTION_SCHEDULER_ROUTING

EXACT_SHARED_RUNTIME_ALLOWLIST_ONLY
NO_UNREVIEWED_SHARED_RUNTIME_SEMANTIC_CHANGE
~~~

---

# 9. Verification authority

Required exact verification identities:

~~~text
U06-EV-001..106
U06-CW-01..09
U06-HG-001..003
U06-VG-001..010
U06-AGG-V-001..012

IRR01-V01..V16
IRR02-V01..V16
IRR03-V01..V17
~~~

Required:
- 106/106 EV PASS;
- 9/9 CW PASS;
- 3/3 HG PASS;
- 10/10 VG PASS;
- 12/12 AGG-V PASS;
- 49/49 exact IRR physical cases PASS.

---

# 10. Post-implementation diff rule

Observed change set must be computed from:

~~~text
git diff --name-only IMPLEMENTATION_BASE_SHA..IMPLEMENTATION_TARGET_SHA
~~~

and compared against this manifest.

Implementation may not author or broaden its own allowlist after coding begins.

Required:

~~~text
unexpected_changed_files = []
unreviewed_shared_runtime_change_count = 0
~~~

---

# 11. Invalidation

If implementation requires any file, root, migration object, semantic authority, live dependency, or verification weakening outside this manifest:

~~~text
STOP
→ AUTHORIZATION_STALE_OR_INSUFFICIENT
→ impact review / amendment / additional authorization
~~~
