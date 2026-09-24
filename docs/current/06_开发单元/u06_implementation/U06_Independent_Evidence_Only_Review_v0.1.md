# U06 Independent Evidence-Only Review v0.1

**Review verdict:** REVISE_REQUIRED  
**Review type:** Independent evidence-only review  
**Implementation semantics are not re-reviewed in this document.**  
**Reviewed authoritative run:** `35960173841`  
**Reviewed authoritative job:** `107507048613`  
**Reviewed artifact ID:** `10792201107`  
**Reviewed artifact name:** `u06-rdp06-authoritative-25b38940dec18ce08e7dd5eb0229edfea723bac9`  
**GitHub artifact SHA-256:** `ad96f821a00817d2a8b8c6a892015f062c73fbc9479706206051a3929897e6b8`  
**Implementation semantic SHA:** `66fa3c078dce5e304e2ada9d6c0df0ff9fbde661`  
**Re-freeze/provenance head:** `5c7f2c06995430d1bf169fc2c2e7c6addfbd4636`  
**Verifier checkout SHA:** `25b38940dec18ce08e7dd5eb0229edfea723bac9`  
**Authority core digest:** `038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22`

---

## 1. Review boundary

This review examines only whether the post-re-freeze authoritative evidence can be independently accepted as durable closure evidence under frozen U06-RDP-06.

It does not:

- re-review U06 business/runtime implementation semantics;
- re-open the targeted exact-head implementation PASS at `66fa3c0...`;
- grant Implementation Verification Closure;
- grant Merge Authorization;
- authorize PROFILE-A, production, live routing, external delivery, or real-patient traffic.

The evidence review independently checks:

1. artifact outer integrity;
2. internal checksum integrity;
3. exact implementation/verifier/authority identity binding;
4. Oracle/Fixture review-gate provenance;
5. all 189 required case identities;
6. 115 fixture-driven runtime observations;
7. environment isolation / zero external effect evidence;
8. regression evidence;
9. required durable bundle contents and retention.

---

## 2. Positive evidence findings

### 2.1 GitHub artifact outer digest is exact

The downloaded ZIP was independently hashed.

~~~text
computed ZIP SHA-256
= ad96f821a00817d2a8b8c6a892015f062c73fbc9479706206051a3929897e6b8

GitHub artifact metadata SHA-256
= ad96f821a00817d2a8b8c6a892015f062c73fbc9479706206051a3929897e6b8
~~~

Result:

~~~text
OUTER_ARTIFACT_INTEGRITY
= PASS
~~~

### 2.2 Artifact inventory is readable and non-corrupt

The ZIP contains exactly these seven retained files:

~~~text
u06-rdp06-evidence/SHA256SUMS
u06-rdp06-evidence/environment-input.json
u06-rdp06-evidence/u06-case-evidence.json
u06-rdp06-evidence/u06-environment-evidence.json
u06-rdp06-evidence/u06-verification-summary.json
u06-rdp06-observations/external-call-spy.json
u06-rdp06-observations/u06-sut-observations.json
~~~

All files are readable.

### 2.3 The checksums that are present are correct

The four entries present in `SHA256SUMS` independently recompute exactly:

~~~text
u06-case-evidence.json
= 595b830178db8770744079823736c878c28db62e10166efc97df6c86df690a78

u06-verification-summary.json
= c9f449eff694eb1ae66bb3fb3e536e80ad6b1a9c01098546a02b018492f5b4a0

u06-environment-evidence.json
= bddeeb8bb7c82412d8fb2a879f4df6abfebf2d7af9097825cd87c702b752638d

u06-sut-observations.json
= e60bd0391181303f473b8ab45cac9baa20625be110f56811d695c6b575c054d9
~~~

The issue is completeness of checksum coverage, not a mismatch in these four hashes.

### 2.4 Exact implementation identity binding passes

The artifact summary and case-evidence header both bind:

~~~text
implementation_sha
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

The refrozen contract manifest independently binds the same exact semantic SHA.

~~~text
IMPLEMENTATION_IDENTITY_BINDING
= PASS
~~~

### 2.5 Verifier SHA is explainable and exact

The artifact records:

~~~text
verifier_sha
= 25b38940dec18ce08e7dd5eb0229edfea723bac9
~~~

This is the GitHub pull-request merge-ref commit for the run.

Its parents are:

~~~text
parent 1
= f78bd9192d0603cfa3cc878644088f908dcb2fb8
  authorization/base branch

parent 2
= 5c7f2c06995430d1bf169fc2c2e7c6addfbd4636
  re-freeze/provenance branch head
~~~

Its tree is the same tree as the re-freeze head:

~~~text
tree
= b3449f2b1c6c5a1d4fa2b4d47facd20573356101
~~~

Therefore the difference between the run metadata branch head and `verifier_sha` is normal GitHub pull-request checkout behavior, not an unexplained verifier substitution.

~~~text
VERIFIER_IDENTITY_BINDING
= PASS
~~~

### 2.6 Refrozen authority digests independently match repository bytes

At the re-freeze head:

~~~text
authority_core_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22

oracle SHA-256
= 9a7ae0b5286bd033e74e28167af523a53d57fccd7694d16e7c76f3da281f234f

fixture SHA-256
= df87c9fb7e30fc472d96f840f25fafac987d8a256cbcda2c872e89e2e8a63cd0

auth profile SHA-256
= 38f6afb30a55ea8d4cd54dad52509a629e20fbd31639b9b537e1324205a65f7c
~~~

These values exactly equal the evidence summary and contract-manifest values.

### 2.7 All 189 required evidence identities are present exactly once

Independent inventory:

~~~text
U06-EV-*     = 106
U06-CW-*     = 9
U06-HG-*     = 3
U06-VG-*     = 10
U06-AGG-V-*  = 12
IRR01-V*     = 16
IRR02-V*     = 16
IRR03-V*     = 17

TOTAL
= 189

unique
= 189

duplicates
= 0
~~~

Case statuses:

~~~text
PASS
= 189

FAIL
= 0

NOT_EXECUTED
= 0
~~~

No PASS case contains a non-empty mismatch list.

Every case has at least one evidence reference.

### 2.8 Runtime observation identity coverage is exact

The evidence reports:

~~~text
runtime_observation_count
= 115

unique runtime observation case IDs
= 115
~~~

The 115 observation IDs exactly equal the 115 case-evidence entries of type:

~~~text
FIXTURE_DRIVEN_SUT_OBSERVATION
~~~

There are:

~~~text
missing runtime observation IDs
= 0

extra runtime observation IDs
= 0

duplicate runtime observation IDs
= 0
~~~

The observation bundle explicitly records:

~~~text
fixture_driven = true
oracle_used_to_generate_observation = false
~~~

### 2.9 External-effect observations are clean

Across all 115 runtime observations:

~~~text
observed_external_transport_count
= 0 for all 115
~~~

The external call spy reports:

~~~text
enabled
= true

observed_count
= 0

policy
= BLOCK_AND_COUNT_JAVA_SOCKET_CONNECT
~~~

The environment evidence reports:

~~~text
network_egress_policy
= DOCKER_NETWORK_NONE

allowed_endpoints
= []

production_secret_count
= 0

real_recipient_endpoint_count
= 0

real_phi_count
= 0

production_store_write_count
= 0

real_external_delivery_count
= 0

real_model_call_count
= 0

real_tool_call_count
= 0

real_knowledge_call_count
= 0

unreviewed_shared_runtime_change_count
= 0
~~~

### 2.10 Full regression evidence is clean

Environment evidence:

~~~text
regression_test_count
= 434

regression_failures
= 0

regression_errors
= 0

regression_skips
= 1

unexpected_skips
= 0
~~~

The only skip is the explicitly enumerated pre-existing U03 frozen-clinical-validation skip.

### 2.11 Authoritative verifier integrity checks all report PASS

The summary contains 17 integrity checks and all 17 are true, including:

- oracle SHA-256;
- fixture SHA-256;
- auth-profile SHA-256;
- Oracle gate verdict/digest/core binding;
- Fixture gate verdict/digest/core binding;
- Oracle core binding;
- Fixture core binding;
- exact implementation semantic SHA;
- required identity count;
- unique identity count;
- zero missing fixtures;
- runtime observation count;
- runtime observation identity count.

This establishes that the verifier ran deterministically against the refrozen identities.

---

# 3. Blocking finding B-U06-EVR-01 — Oracle / Fixture review-gate identity was rewritten without a new independent review

## 3.1 Frozen rule

U06-RDP-06 requires the Oracle and Fixture authority to be independently reviewed and bound to exact digests.

The contract-manifest semantics explicitly treat Oracle/Fixture review records as external immutable gate records bound to:

- exact asset digest;
- exact authority-core digest.

Independent evidence acceptance also requires:

~~~text
oracle review digest is exact
and independently accepted
~~~

## 3.2 Historical gate identity before re-freeze

At implementation head `66fa3c0...`, review ID `5298650596` was bound to:

~~~text
Oracle:
review_id
= 5298650596

reviewed_oracle_digest
= ac89a1e902bd82c84e39748d76de1807db434aff3bb6e21d44c03aa564a174d7

reviewed_contract_manifest_digest
= 4f2a1373ab5e6cc25a599a80df3aa3ddab05e55959a643eef80d8b3112d664d0

Fixture:
review_id
= 5298650596

reviewed_fixture_manifest_digest
= 6494a86da0e385c287276100e12af52e57067b5a72a441740cf3ab3ddf411c5a

reviewed_contract_manifest_digest
= 4f2a1373ab5e6cc25a599a80df3aa3ddab05e55959a643eef80d8b3112d664d0
~~~

## 3.3 Gate identity after re-freeze

At `5c7f2c0...`, the **same review ID** was changed in place to claim review of:

~~~text
Oracle:
review_id
= 5298650596

reviewed_oracle_digest
= 9a7ae0b5286bd033e74e28167af523a53d57fccd7694d16e7c76f3da281f234f

reviewed_contract_manifest_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22

Fixture:
review_id
= 5298650596

reviewed_fixture_manifest_digest
= df87c9fb7e30fc472d96f840f25fafac987d8a256cbcda2c872e89e2e8a63cd0

reviewed_contract_manifest_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22
~~~

No new independent Oracle-review ID or Fixture-review ID was created.

The added field:

~~~text
review_binding_revalidated_by
= U06_TARGETED_EXACT_HEAD_IMPLEMENTATION_REVIEW_7ddc61...
~~~

does not close this gap because that record is an **implementation review**, created before the refrozen Oracle/Fixture bytes existed. It is not an independent review of those exact new bytes.

## 3.4 Semantic-equivalence check

This evidence review independently confirmed an important positive fact:

~~~text
new Oracle with new authority-core digest replaced back to prior authority-core digest
= byte-for-byte equal to old Oracle

new Fixture with new authority-core digest replaced back to prior authority-core digest
= byte-for-byte equal to old Fixture
~~~

Therefore:

~~~text
business expectation semantic drift
= NOT_FOUND

fixture scenario semantic drift
= NOT_FOUND
~~~

The defect is review provenance, not content semantics.

## 3.5 Why it still blocks acceptance of this run

The authoritative run consumed gate files that already claimed PASS for the new exact asset digests.

But those exact new bytes had not yet received a new independent Oracle/Fixture review.

That violates the required order:

~~~text
build/review exact Oracle + Fixture
→ authoritative run
→ independent evidence review
~~~

A post-hoc content-equivalence observation in this evidence review cannot make the historical pre-run gate record immutable or create a missing pre-run independent review identity.

### Required remediation

1. create a new targeted **Oracle provenance-only independent re-review** for:
   - Oracle digest `9a7ae0...`;
   - authority core `038c074...`;
2. create a new targeted **Fixture provenance-only independent re-review** for:
   - Fixture digest `df87c9...`;
   - authority core `038c074...`;
3. write new gate records or append new immutable gate records with **new review IDs** rather than rewriting review `5298650596`;
4. confirm the only content delta from the previously reviewed assets is the authority-core provenance substitution;
5. run a **fresh authoritative verification after those new independent gate records exist**.

**Disposition:** OPEN / BLOCKING.

---

# 4. Blocking finding B-U06-EVR-02 — retained evidence files are not fully covered by internal checksums

## 4.1 Frozen rule

U06-RDP-06 Artifact Integrity requires:

~~~text
Artifact manifest must hash every retained evidence file.
Independent reviewer must be able to recompute hashes.
~~~

Evidence-builder fail-closed rules also require failure on any evidence-file hash mismatch.

## 4.2 Current artifact

Excluding `SHA256SUMS` itself, the artifact retains six evidence files:

~~~text
1. environment-input.json
2. u06-case-evidence.json
3. u06-environment-evidence.json
4. u06-verification-summary.json
5. external-call-spy.json
6. u06-sut-observations.json
~~~

But `SHA256SUMS` covers only four:

~~~text
u06-case-evidence.json
u06-verification-summary.json
u06-environment-evidence.json
u06-sut-observations.json
~~~

Missing checksum coverage:

~~~text
environment-input.json
external-call-spy.json
~~~

The external-call spy is a critical raw hard-boundary evidence file, so this omission cannot be treated as cosmetic.

For reference, independently computed omitted hashes are:

~~~text
environment-input.json
= bddeeb8bb7c82412d8fb2a879f4df6abfebf2d7af9097825cd87c702b752638d

external-call-spy.json
= b0c33d7c3f96d48d0fa99c1e3812be74ac267b1472be75ed03d859923b1a5d05
~~~

`environment-input.json` is byte-identical to `u06-environment-evidence.json`, but because it is retained in the artifact it still falls under the “every retained evidence file” integrity requirement.

### Required remediation

Either:

- retain both files and include both in artifact-manifest / SHA256SUMS coverage;

or:

- remove a redundant retained file where appropriate, while still retaining and hashing the raw external-call-spy evidence.

**Disposition:** OPEN / BLOCKING.

---

# 5. Blocking finding B-U06-EVR-03 — required durable provenance source artifacts are absent from the evidence bundle

## 5.1 Frozen rule

U06-RDP-06 explicitly requires that the authorized shared-runtime change manifest:

~~~text
must itself be included in the evidence bundle
and checksum coverage
~~~

The durable evidence bundle also requires semantic contents for:

- contract/authority manifest;
- verification Oracle;
- fixture manifest;
- verification auth profile;
- Oracle review gate;
- environment isolation;
- authorized shared-runtime change manifest;
- regression summary;
- artifact manifest;
- SHA256SUMS.

Exact filenames may vary, but semantic contents are required.

## 5.2 Current artifact

The current ZIP does **not** retain:

- `u06-contract-manifest.json`;
- refrozen verification Oracle;
- refrozen Fixture manifest;
- auth profile;
- Oracle review gate;
- Fixture review gate;
- `U06_Authorized_Shared_Runtime_Change_Manifest_v0.1.md`;
- a complete artifact manifest;
- exact workflow/run/job/toolchain metadata snapshot.

Instead, the summary carries digests and the environment input carries:

~~~text
authorized_change_manifest_review = PASS
unreviewed_shared_runtime_change_count = 0
~~~

Those scalar claims are useful, but they are not equivalent to retaining the required reviewed manifest itself.

This evidence review was able to reconstruct the missing provenance only by separately querying the repository. The frozen durable-bundle requirement is stricter: future independent review must not depend on mutable/external lookup for required retained authority material.

### Required remediation

The evidence builder/upload step must retain, at minimum:

1. exact refrozen contract manifest;
2. exact refrozen Oracle;
3. exact refrozen Fixture manifest;
4. auth profile;
5. new immutable Oracle and Fixture review-gate records from B-U06-EVR-01;
6. observed authorized shared-runtime change manifest;
7. environment evidence;
8. runtime observations;
9. raw external-call-spy evidence;
10. case evidence;
11. verification summary;
12. artifact manifest describing exact run/verifier/toolchain identities;
13. SHA256SUMS covering every retained evidence file.

**Disposition:** OPEN / BLOCKING.

---

# 6. Required finding RF-U06-EVR-01 — workflow / artifact / toolchain metadata must be durably bound

The frozen authoritative evidence identity requires binding of:

~~~text
workflow_run_id
job_id
artifact_id/name
artifact_sha256
toolchain
OS/arch
JDK
~~~

The current independent review can recover:

~~~text
workflow_run_id
= 35960173841

job_id
= 107507048613

artifact_id
= 10792201107

artifact_name
= u06-rdp06-authoritative-25b38940dec18ce08e7dd5eb0229edfea723bac9

artifact_sha256
= ad96f821a00817d2a8b8c6a892015f062c73fbc9479706206051a3929897e6b8
~~~

and can infer major toolchain configuration from the workflow:

~~~text
GitHub Actions runner
Maven container = 3.9.9-eclipse-temurin-8
Java setup = Temurin 8
~~~

But the bundle does not retain a self-contained artifact/run manifest with these fields, nor an exact OS/arch / JDK runtime snapshot.

Required remediation should add an artifact/run manifest or accepted compact evidence snapshot containing these identities.

**Disposition:** REQUIRED / MUST_CLOSE_WITH_BUNDLE_REMEDIATION.

---

# 7. Required finding RF-U06-EVR-02 — retention is only 14 days

Artifact metadata currently states:

~~~text
created_at
= 2026-09-24

expires_at
= 2026-10-08
~~~

The workflow explicitly uses:

~~~text
retention-days: 14
~~~

Frozen RDP-06 retention policy requires raw CI evidence retention of at least 90 days **when the platform permits**, plus a repository-retained accepted compact evidence snapshot after independent acceptance.

This review does not have repository administration evidence proving that 90-day retention is unavailable.

Therefore the next evidence-builder remediation should:

- use 90 days if repository/platform policy permits; or
- document the platform-enforced maximum if lower;
- repository-retain the accepted compact evidence snapshot after a future PASS evidence review.

**Disposition:** REQUIRED.

---

# 8. Independent review verdict

The behavioral evidence is strong:

~~~text
outer artifact integrity = PASS
implementation binding = PASS
verifier merge-ref identity = PASS
authority digest matching = PASS
189 / 189 = PASS
115 / 115 runtime observations = PRESENT / UNIQUE
hard boundary violations = []
external socket attempts = 0
full regression = PASS
unexpected skips = 0
~~~

However durable evidence governance is not yet acceptable:

~~~text
B-U06-EVR-01
Oracle / Fixture exact-byte independent review provenance
= OPEN / BLOCKING

B-U06-EVR-02
complete checksum coverage
= OPEN / BLOCKING

B-U06-EVR-03
required durable provenance artifacts in bundle
= OPEN / BLOCKING

RF-U06-EVR-01
run/artifact/toolchain metadata durability
= OPEN / REQUIRED

RF-U06-EVR-02
retention policy
= OPEN / REQUIRED
~~~

Therefore:

~~~text
U06 Independent Evidence-Only Review
= REVISE_REQUIRED

Fresh Post-Re-Freeze Authoritative Run
= BEHAVIORALLY_PASSING
  BUT EVIDENCE_NOT_ACCEPTED_FOR_CLOSURE

Independent Evidence Acceptance
= NOT_PASSED

Combined Implementation / Evidence Review
= NOT_PERMITTED YET

Implementation Verification Closure
= NOT_PASSED

Merge Authorization
= NOT_GRANTED
~~~

---

# 9. Next permitted step

The next permitted work is:

> **U06 Evidence Authority + Durable Bundle Targeted Remediation**

It must:

1. perform new immutable provenance-only independent Oracle/Fixture reviews for the refrozen exact bytes;
2. update evidence-builder packaging so required provenance assets are retained;
3. hash every retained evidence file;
4. add exact workflow/run/job/artifact/toolchain metadata binding;
5. fix/document retention;
6. execute a new authoritative run against unchanged implementation semantic SHA `66fa3c0...`.

Because these are evidence/verifier/provenance changes only, the implementation semantic head remains `66fa3c0...` unless runtime implementation code changes.

A fresh run is mandatory after the evidence/verifier changes; the current artifact `10792201107` must not be promoted to closure evidence.
