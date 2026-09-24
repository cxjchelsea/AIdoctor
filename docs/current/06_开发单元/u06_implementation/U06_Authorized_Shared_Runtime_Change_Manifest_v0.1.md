# U06 Authorized Shared-Runtime Change Manifest — Observed Implementation Diff v0.1

> Authorization: `AUTH-U06-PROFILEB-IMPL-001`  
> Authorization-time manifest: `U06_Authorization_Time_Allowed_Change_Manifest_v0.1.md`  
> Implementation base SHA: `f78bd9192d0603cfa3cc878644088f908dcb2fb8`  
> Reviewed implementation target SHA: `66fa3c078dce5e304e2ada9d6c0df0ff9fbde661`  
> Targeted Exact-Head Implementation Re-Review: **PASS / PR #245 / record 7ddc61b9aae7b0cb14f3a44e8dcc638e4c171da1**  
> Scope: **PROFILE-B / SYNTHETIC_STRUCTURAL_NONPROD only**

## 1. Repository-native diff binding

~~~text
base = f78bd9192d0603cfa3cc878644088f908dcb2fb8
target = 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
merge_base = f78bd9192d0603cfa3cc878644088f908dcb2fb8
ahead_by = 111
behind_by = 0

observed_git_diff_digest
= ecb13791fb5c98a1da21b2b967366092a29c301c2106a9b62a59ebc0308f8f74
~~~

Digest input is the canonical compact JSON tuple:
`{"base":base,"target":target,"files":[sorted changed-file paths]}`.

## 2. Observed changed files

- `.github/workflows/u06-rdp06-authoritative-verification.yml`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRecord.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRepository.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadWaitTransitionService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRecord.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRepository.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRecord.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRepository.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06AdmissionEvidence.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06AdmissionService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ExecutionResult.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06Ids.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06NoProgressRouter.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBApplicationService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBRequest.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticDecisionBundle.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticDecisionEngine.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticDecisionInput.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticPostF3SafetyBarrier.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticRevalidationAuthority.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/U06DeliveryStore.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/U06SyntheticDeliveryRuntime.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/U06SyntheticDeliveryService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/state/U06StateValues.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/state/U06SyntheticP01Runtime.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/trace/JdbcU06GovernedExecutionTraceStore.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/trace/U06GovernedExecutionTraceStore.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/ConsultationWaitEffectRecord.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/ConsultationWaitEffectRepository.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/ConsultationWaitTransitionService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/U06WaitCoordinator.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/U07ResumeEligibilityProjector.java`
- `diagnosis-service/src/main/resources/db/migration-oracle/V6__add_u06_wait_runtime.sql`
- `diagnosis-service/src/main/resources/db/migration/V6__add_u06_wait_runtime.sql`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/U06AuthoritativeContractTest.java`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/U06AuthoritativeNetworkSpyTest.java`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBStructuralTest.java`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/U06Rdp06AuthoritativeObservationTest.java`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/state/U06SyntheticP01TestFactory.java`
- `diagnosis-service/src/test/resources/u06/u06-auth-profile.json`
- `diagnosis-service/src/test/resources/u06/u06-contract-manifest.json`
- `diagnosis-service/src/test/resources/u06/u06-fixture-review-gate.json`
- `diagnosis-service/src/test/resources/u06/u06-oracle-review-gate.json`
- `diagnosis-service/src/test/resources/u06/u06-verification-expectations.json`
- `diagnosis-service/src/test/resources/u06/u06-verification-fixtures.json`
- `docs/current/06_开发单元/u06_implementation/U06_Authoritative_Verification_Failure_Triage_Targeted_Remediation_v0.1.md`
- `docs/current/06_开发单元/u06_implementation/U06_Authorized_Shared_Runtime_Change_Manifest_v0.1.md`
- `docs/current/06_开发单元/u06_implementation/U06_ProfileB_Formal_Implementation_Status_v0.1.md`
- `docs/current/06_开发单元/u06_implementation/U06_RDP06_Authoritative_Verification_Runner_Completion_v0.1.md`
- `docs/current/06_开发单元/u06_implementation/U06_RDP06_Oracle_Fixture_Authority_Freeze_v0.1.md`
- `tools/u06_nonprod_verification/u06-execution-evidence-map.json`
- `tools/u06_nonprod_verification/verify_u06.py`

## 3. Authorization comparison

~~~text
observed_changed_file_count = 54
unexpected_changed_file_count = 0
unreviewed_shared_runtime_change_count = 0
~~~

Unexpected files:

~~~text
[]
~~~

## 4. Protected shared/core check

Observed diff remains inside the authorization-time allowlist and does not authorize production/live activation for PROFILE-A, real PHI, real C03/D04, patient-facing content, external delivery, direct live F1, live U07, production Scheduler routing, release activation, or real-patient traffic.

## 5. Verdict

~~~text
U06_AUTHORIZED_SHARED_RUNTIME_CHANGE_MANIFEST
= PASS

unexpected_changed_files = []
unreviewed_shared_runtime_change_count = 0

IMPLEMENTATION_TARGET
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

This manifest validates authorization scope only.
It does not prove final RDP-06 evidence closure, merge readiness, production readiness, or PROFILE-A.
