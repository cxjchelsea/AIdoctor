# U06 Authorized Shared-Runtime Change Manifest — Observed Implementation Diff v0.1

> Authorization: `AUTH-U06-PROFILEB-IMPL-001`  
> Authorization-time manifest: `U06_Authorization_Time_Allowed_Change_Manifest_v0.1.md`  
> Implementation base SHA: `f78bd9192d0603cfa3cc878644088f908dcb2fb8`  
> Reviewed implementation target SHA: `ef532c46cb29afbe12e7d498e197603dd6460cfc`  
> Formal Implementation Review: **PASS / review 5298441567**  
> Scope: **PROFILE-B / SYNTHETIC_STRUCTURAL_NONPROD only**

## 1. Repository-native diff binding

~~~text
base = f78bd9192d0603cfa3cc878644088f908dcb2fb8
target = ef532c46cb29afbe12e7d498e197603dd6460cfc
merge_base = f78bd9192d0603cfa3cc878644088f908dcb2fb8
ahead_by = 39
behind_by = 0

observed_git_diff_digest
= SHA256_NOT_AVAILABLE_IN_REVIEW_RUNTIME
~~~

Digest input is the canonical JSON tuple:
`{base,target,sorted changed-file paths}`.

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
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06AdmissionService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ExecutionResult.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06Ids.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBApplicationService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBRequest.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticDecisionBundle.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticDecisionEngine.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticDecisionInput.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticPostF3SafetyBarrier.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticRevalidationAuthority.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/U06DeliveryStore.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/U06SyntheticDeliveryService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/state/U06StateValues.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/state/U06SyntheticP01Runtime.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/trace/JdbcU06GovernedExecutionTraceStore.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/trace/U06GovernedExecutionTraceStore.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/ConsultationWaitEffectRecord.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/ConsultationWaitEffectRepository.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/ConsultationWaitTransitionService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/U06WaitCoordinator.java`
- `diagnosis-service/src/main/resources/db/migration-oracle/V6__add_u06_wait_runtime.sql`
- `diagnosis-service/src/main/resources/db/migration/V6__add_u06_wait_runtime.sql`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBStructuralTest.java`
- `docs/current/06_开发单元/u06_implementation/U06_ProfileB_Formal_Implementation_Status_v0.1.md`

## 3. Authorization comparison

~~~text
observed_changed_file_count = 34
unexpected_changed_file_count = 0
unreviewed_shared_runtime_change_count = 0
~~~

Unexpected files:

~~~text
[]
~~~

## 4. Protected shared/core check

Observed diff does not modify:
- StateCommitter;
- StatePatchBoundaryValidator;
- ClinicalCdpStateRepositoryAdapter;
- CapabilityCallTraceRecord / CapabilityTraceService;
- CapabilityBindingRecord / CapabilityBindingRegistry;
- RuntimeBindingRecord / RuntimeBindingService;
- ClinicalRunRecord / ClinicalRunCoordinator;
- contracts/releases/**;
- packages/python_runtime/**;
- U03/U04/U05/U07 production runtime.

## 5. Verdict

~~~text
U06_AUTHORIZED_SHARED_RUNTIME_CHANGE_MANIFEST
= PASS

unexpected_changed_files = []
unreviewed_shared_runtime_change_count = 0

IMPLEMENTATION_TARGET
= ef532c46cb29afbe12e7d498e197603dd6460cfc
~~~

This manifest validates authorization scope only.
It does not prove RDP-06 behavioral verification, evidence closure, merge readiness, production readiness, or PROFILE-A.
