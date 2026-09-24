# U06 Auth Profile Authority Consistency Targeted Remediation v0.1

> Trigger: Independent Evidence-Only Re-Review PR #248  
> Findings: `B-U06-EVRR-01..03`  
> Remediation class: **authority / provenance / verifier only**  
> Runtime implementation semantic SHA: **unchanged**

## 1. Frozen implementation identity

~~~text
IMPLEMENTATION_SEMANTIC_PASS_SHA
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

No `src/main` runtime implementation code is modified by this remediation.

## 2. B-U06-EVRR-01 — Auth Profile contract shape

`u06-auth-profile.json` is migrated from the incomplete legacy schema:

~~~text
U06_AUTH_PROFILE_V0_1
~~~

to the frozen RDP-06 authority schema:

~~~text
U06_VERIFICATION_AUTH_PROFILE_V0_1
~~~

The profile now explicitly carries all RDP-06 minimum authority fields, including:

- NON_PRODUCTION environment;
- SYNTHETIC_STRUCTURAL_NONPROD execution profile;
- synthetic-only / zero real-patient traffic;
- PROFILE-A real C03 and real delivery disabled;
- zero external delivery/model/tool/knowledge calls;
- no production state or consultation store;
- no live U07 or U14;
- synthetic delivery scope required;
- exact authorized shared-runtime refs;
- exact authorization-time path allowlist;
- unreviewed shared-runtime change count = 0.

Legacy negative-boundary aliases are retained only for compatibility with already frozen aggregate/static cases.

## 3. B-U06-EVRR-02 — semantic binding

The auth profile now binds:

~~~text
implementation_semantic_pass_sha
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

which exactly equals the refrozen contract-manifest semantic target.

## 4. Authorized shared-runtime refs

The profile uses only identifiers already present in the owner authorization decision:

~~~text
AUTH-U06-PROFILEB-IMPL-001
FROZEN_RDP01_TO_RDP06
AGGREGATE_AC01_TO_AC10
CA_IRR01_TO_IRR03
EXACT_SHARED_RUNTIME_ALLOWLIST_ONLY
~~~

## 5. Authorized path allowlist

The machine-readable path list is copied from the frozen authorization-time allowed-change manifest:

- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/**`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRecord.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRepository.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRecord.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRepository.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRecord.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointRepository.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadWaitTransitionService.java`
- `diagnosis-service/src/main/resources/db/migration/V6__add_u06_wait_runtime.sql`
- `diagnosis-service/src/main/resources/db/migration-oracle/V6__add_u06_wait_runtime.sql`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u06/**`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/u06/**`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u01/U01ConsultationServiceTest.java`
- `diagnosis-service/src/test/resources/u06/**`
- `tools/u06_nonprod_verification/**`
- `.github/workflows/u06-rdp06-authoritative-verification.yml`
- `docs/current/06_开发单元/u06_implementation/**`

No path is broadened beyond the frozen authorization manifest.

## 6. Contract manifest update

Only the exact auth-profile SHA-256 binding changes.

~~~text
new auth_profile_digest
= b1aaea818525f6e6c81089d18a8d2ac38d35478a666173de27903c70e4ab77b4

authority_core_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22
= unchanged
~~~

Because the contract-manifest file bytes change, the existing refrozen Oracle/Fixture gate `reviewed_contract_manifest_blob_sha` values are intentionally **not rewritten in this remediation candidate**.

They must be rebound only after an independent equivalence review of this candidate.

## 7. New Auth Profile independent review gate

A machine-readable gate is added:

~~~text
U06_AUTH_PROFILE_REVIEW_GATE_V0_1
~~~

Candidate state:

~~~text
verdict
= PENDING_INDEPENDENT_REVIEW

reviewed_auth_profile_digest
= b1aaea818525f6e6c81089d18a8d2ac38d35478a666173de27903c70e4ab77b4

reviewed_contract_manifest_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22

reviewed_contract_manifest_blob_sha
= fec6755eab3709472b571f9317c75c8b50b6efd4

reviewed_implementation_semantic_sha
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

The verifier requires this gate to be PASS before authoritative evidence can pass.

Therefore this candidate cannot accidentally promote itself before independent review.

## 8. B-U06-EVRR-03 — verifier fail-closed enforcement

The authoritative verifier now checks:

- exact auth-profile SHA-256;
- exact auth-profile schema;
- authorization ID;
- environment / execution / delivery profiles;
- current semantic SHA against both manifest and CLI implementation SHA;
- every frozen hard-boundary boolean;
- exact authorized shared-runtime refs;
- exact authorization-time path allowlist;
- unreviewed shared-runtime change count = 0;
- independent auth-profile gate PASS;
- auth-profile gate digest/core/blob/semantic binding;
- non-empty independent review identity and review record;
- Oracle and Fixture review gates bind the **current contract-manifest Git blob**, not only the unchanged authority-core digest.

This closes the prior gap where a byte-valid but stale/incomplete auth profile could pass.

## 9. Required next gate

This commit is intentionally a remediation candidate, not accepted authority.

Next required step:

> **U06 Auth Profile / Contract Manifest Targeted Independent Equivalence Review**

That review must:

1. independently confirm the new auth profile is a faithful machine-readable projection of already frozen PROFILE-B authorization;
2. confirm no permission/scope expansion;
3. confirm the only contract-manifest semantic asset change is the auth-profile digest;
4. assign a new immutable auth-profile review ID and review record;
5. authorize provenance-only rebinding of the Oracle/Fixture gate contract-manifest blob SHA.

Only after that review may the review gate become PASS and a fresh authoritative run be used for evidence closure.
