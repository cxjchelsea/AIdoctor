# U06 RDP-06 Oracle / Fixture / Authority Freeze v0.1

> Scope: **PROFILE-B / SYNTHETIC_STRUCTURAL_NONPROD**  
> Implementation semantic PASS head: `1d427fd09957767c938e34883dcbcda85b7fd992`  
> Authority freeze reviewed head: `995528242cc87ea3e9abc5f7787d7edf9e281f66`  
> Formal Implementation Review: **PASS / 5298441567**  
> Authorized Change Manifest Review: **PASS / 5298457832**  
> Status: **FROZEN / ORACLE_FIXTURE_GATES_PASS / AUTHORITATIVE_VERIFICATION_NOT_RUN**

---

# 1. Frozen contract authority core

~~~text
schema
= U06_CONTRACT_AUTHORITY_CORE_V0_1

authority_core_sha256
= 4f2a1373ab5e6cc25a599a80df3aa3ddab05e55959a643eef80d8b3112d664d0
~~~

The core binds:
- U06 Unit Spec;
- RDP-01..06;
- Aggregate Amendment;
- CA-U06-IRR-01..03;
- post-physical readiness;
- Implementation Authorization Review / Decision;
- Authorization-Time Allowed Change Manifest;
- observed authorized-change manifest;
- Shared Contract version 1.0.0.

The authority-core digest is the stable `contract_manifest_digest` used by oracle/fixture review gates.

The contract manifest file itself is:

~~~text
diagnosis-service/src/test/resources/u06/u06-contract-manifest.json

Git blob SHA
= e1e228ea23da384353609ef2e86385dff9ef1e44
~~~

Review-gate verdicts are separate immutable records and do not mutate the authority core.

---

# 2. Frozen expectation oracle

~~~text
path
= diagnosis-service/src/test/resources/u06/u06-verification-expectations.json

schema
= U06_VERIFICATION_EXPECTATIONS_V0_1

SHA-256 exact UTF-8 file text
= ac89a1e902bd82c84e39748d76de1807db434aff3bb6e21d44c03aa564a174d7
~~~

Case identity inventory:

~~~text
U06-EV-001..106 = 106
U06-CW-01..09 = 9
U06-HG-001..003 = 3
U06-VG-001..010 = 10
U06-AGG-V-001..012 = 12
IRR01-V01..V16 = 16
IRR02-V01..V16 = 16
IRR03-V01..V17 = 17

total unique required identities
= 189
~~~

Every case explicitly carries the RDP-06 expectation schema.
Null and zero are distinct:
- `null` = not applicable / exact scalar not constrained by frozen case;
- `0` = explicitly zero.

---

# 3. Independent Oracle Review Gate

~~~text
schema
= U06_ORACLE_REVIEW_GATE_V0_1

initial review
= 5298485951 / REVISE_REQUIRED

targeted re-review
= 5298506940 / PASS

reviewed_oracle_digest
= ac89a1e902bd82c84e39748d76de1807db434aff3bb6e21d44c03aa564a174d7

reviewed_contract_manifest_digest
= 4f2a1373ab5e6cc25a599a80df3aa3ddab05e55959a643eef80d8b3112d664d0
~~~

Gate record:

~~~text
diagnosis-service/src/test/resources/u06/u06-oracle-review-gate.json
~~~

Expectation authority is contract-derived, not SUT-output-derived.

---

# 4. Frozen fixture manifest

~~~text
path
= diagnosis-service/src/test/resources/u06/u06-verification-fixtures.json

schema
= U06_VERIFICATION_FIXTURES_V0_1

SHA-256 exact UTF-8 file text
= 6494a86da0e385c287276100e12af52e57067b5a72a441740cf3ab3ddf411c5a

fixture count
= 189
~~~

Every fixture is:

~~~text
synthetic = true
patient_data_classification = SYNTHETIC_NON_PATIENT
~~~

Machine-readable `scenario_controls` freeze the actual structural stimulus.
Free-text stimulus is descriptive only.

Production/live values in negative cases are inert sentinel tokens used to prove rejection.
They are not real endpoints, stores, credentials, or patient identifiers.

---

# 5. Independent Fixture Review Gate

~~~text
schema
= U06_FIXTURE_REVIEW_GATE_V0_1

initial review
= 5298486127 / REVISE_REQUIRED

targeted re-review
= 5298507129 / PASS

reviewed_fixture_manifest_digest
= 6494a86da0e385c287276100e12af52e57067b5a72a441740cf3ab3ddf411c5a

reviewed_contract_manifest_digest
= 4f2a1373ab5e6cc25a599a80df3aa3ddab05e55959a643eef80d8b3112d664d0
~~~

Gate record:

~~~text
diagnosis-service/src/test/resources/u06/u06-fixture-review-gate.json
~~~

---

# 6. Frozen auth profile

~~~text
path
= diagnosis-service/src/test/resources/u06/u06-auth-profile.json

schema
= U06_AUTH_PROFILE_V0_1

SHA-256 exact UTF-8 file text
= 38f6afb30a55ea8d4cd54dad52509a629e20fbd31639b9b537e1324205a65f7c
~~~

The auth profile requires:
- PROFILE-B only;
- no real PHI;
- no external I/O;
- no production-store writes;
- no real C03/D04;
- no direct F1 activation;
- no live U07;
- no production Scheduler routing;
- deny-by-default network isolation for the authoritative run.

---

# 7. Implementation / evidence identity separation

~~~text
implementation semantic PASS head
= 1d427fd09957767c938e34883dcbcda85b7fd992
~~~

Later commits adding:
- implementation status;
- diff manifest;
- oracle;
- fixtures;
- review gates;
- verifier/evidence tooling

do not silently redefine the implementation semantic PASS head.

The authoritative run must bind separately:

~~~text
implementation_target_sha
verifier_sha
verification_contract_sha
authority_core_digest
oracle_digest
fixture_digest
auth_profile_digest
~~~

---

# 8. Current environment gate status

Current engineering workflow:

~~~text
U06 Profile-B Engineering Verification Candidate
~~~

proves:
- authorization diff guard;
- structural no-live guard;
- compile;
- focused U06 tests.

It does **not** yet prove the RDP-06 mandatory environment isolation contract.

Current workflow does not yet run the authoritative SUT/verifier process inside a separately provable:

~~~text
network egress = DENY_BY_DEFAULT
or equivalent isolated boundary
~~~

with the complete 189-case verifier and evidence bundle.

Therefore:

~~~text
U06_VERIFICATION_ENVIRONMENT_ISOLATION
= NOT_YET_PASSED

RDP06_AUTHORITATIVE_VERIFICATION
= NOT_RUN

IMPLEMENTATION_VERIFICATION_CLOSURE
= NOT_PASSED
~~~

---

# 9. Required next work

The next implementation/evidence work is:

~~~text
1. implement contract-bound U06 verification runner
   consuming reviewed oracle + fixture + gate records;

2. implement exact identity/digest validation;

3. implement all 189 required case executions/assertions;

4. run authoritative verifier in a process/container with
   deny-by-default network boundary
   and no production secrets/endpoints/stores;

5. produce durable evidence bundle + checksums;

6. independent evidence-only review;

7. combined implementation/evidence review;

8. explicit Implementation Verification Closure.
~~~

The verifier may discover implementation gaps.
Those gaps must be remediated in code and re-reviewed; they may not be hidden by changing the oracle to match the implementation.

---

# 10. Boundary

This freeze does not authorize or prove:
- merge;
- production;
- PROFILE-A;
- real C03/D04;
- real Question content;
- external delivery;
- live routing;
- real-patient use.



---

# 11. Byte-Integrity Rebound Review

Diagnostic run:

~~~text
35941402018
= INVALID_EVIDENCE
~~~

The failure was correctly caused by frozen digest metadata not matching exact committed UTF-8 bytes.

No oracle, fixture, or auth-profile semantic content changed from the prior reviewed semantic freeze.

Targeted integrity re-review:

~~~text
review_id = 5298650596
verdict = PASS
reviewed_head = a832018ec9c9623e18330d04c50fecc7b89d7067
~~~

Correct committed-byte SHA-256:

~~~text
oracle
= ac89a1e902bd82c84e39748d76de1807db434aff3bb6e21d44c03aa564a174d7

fixture
= 6494a86da0e385c287276100e12af52e57067b5a72a441740cf3ab3ddf411c5a

auth profile
= 38f6afb30a55ea8d4cd54dad52509a629e20fbd31639b9b537e1324205a65f7c
~~~

Updated contract-manifest Git blob:

~~~text
e1e228ea23da384353609ef2e86385dff9ef1e44
~~~

Authority-core digest remains unchanged.

Current gate state:

~~~text
ORACLE_GATE = PASS / BYTE_INTEGRITY_REBOUND
FIXTURE_GATE = PASS / BYTE_INTEGRITY_REBOUND

AUTHORITATIVE_VERIFICATION
= NOT_PASSED
~~~
