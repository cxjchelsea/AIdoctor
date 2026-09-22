# U05 RDP-06 Authoritative Verification Runner Design v0.1

> Status: DESIGN / INDEPENDENT REVIEW PENDING  
> Target implementation SHA: `1dc49c4097841523a9445dab078bc5a3d1ad1259`  
> Target implementation PR: #201  
> Frozen authority: `U05_RDP06_Verification_Durable_Evidence_Plan_v0.1.md` at target blob `83242bb6fa1736665afda2c9882215c22b84e1c2`  
> Purpose: implement the verification-only infrastructure required to execute RDP-06 against the exact reviewed U05 implementation without modifying U05 production/runtime source.

---

## 1. Decision

Use a **verification overlay** rather than modifying PR #201.

The authoritative workflow will:

1. verify the verifier branch/authorization lineage and exact verifier-file inventory;
2. preserve the verifier-only files in a temporary overlay directory;
3. checkout the exact implementation SHA `1dc49c4097841523a9445dab078bc5a3d1ad1259`;
4. assert `git rev-parse HEAD == target SHA`;
5. copy only approved test/resource/evidence-builder files from the overlay into the target checkout;
6. prove no production/runtime source differs from the target SHA;
7. execute the frozen RDP-06 matrix;
8. generate the durable evidence bundle;
9. upload the bundle with >=90-day retention;
10. stop at `PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW` or `FAIL`.

This preserves:

```
implementation under test
= exact PR #201 head

verification harness
!= production/runtime implementation
```

No merge, production activation, live downstream execution, external delivery, or real-patient traffic is authorized.

---

## 2. Exact frozen contract identities

The runner must bind the exact contract files present at the target implementation SHA.

| Contract | Path | Target blob |
|---|---|---|
| RDP-01 | `docs/current/06_开发单元/U05_RDP01_Consumer_Inbound_Contract_v0.1.md` | `b59430861b153a04773ac7d151b03f22e3d6b296` |
| RDP-02 | `docs/current/06_开发单元/U05_RDP02_D03_Policy_Owner_Decision_Contract_v0.1.md` | `20ca8b707e003e5265f5976999577541bf80543e` |
| RDP-03 | `docs/current/06_开发单元/U05_RDP03_State_Ownership_K09_P01_Mutation_Trace_Contract_v0.1.md` | `f54dfab7352bd796700f5ba9009d5672631f6196` |
| RDP-04 | `docs/current/06_开发单元/U05_RDP04_Downstream_Routing_SideEffect_Boundary_v0.1.md` | `c313d152779da6421d417a9660ee8798f92e8e40` |
| RDP-05 | `docs/current/06_开发单元/U05_RDP05_Readiness_Input_Dependency_Applicability_Contract_v0.1.md` | `e4b5ea930220eaa175e1e1f66184ee56f53eb10a` |
| RDP-06 | `docs/current/06_开发单元/U05_RDP06_Verification_Durable_Evidence_Plan_v0.1.md` | `83242bb6fa1736665afda2c9882215c22b84e1c2` |

The contract manifest builder must compare actual blob identities to these frozen identities and fail closed on any mismatch or ambiguous current-authority status.


### 2.1 Referenced Phase authority identities

RDP-06 Section 18 also requires exact identities for the frozen documents carrying referenced Phase 5 / 6 / 8 / 9 semantics.

| Referenced phase | Canonical document | Target blob |
|---|---|---|
| Phase 5 | `docs/current/05_业务闭环/业务闭环设计_V1.md` | `0920743521d11e14831420a2cb3a8bd3054a6e65` |
| Phase 6 | `docs/current/06_开发单元/可验证开发单元拆分_V1.md` | `b20ba9a9e28ba82db2737e8383c0c85f96f6674f` |
| Phase 8 | `docs/current/08_契约与数据/Contract与数据语义设计.md` | `8f47d80314b5352d1dd5ee41b1ff0800b7e70362` |
| Phase 9 | `docs/current/09_Runtime与技术架构/Runtime与技术架构设计_V1.md` | `20f4d3c7badbc373401c01a9f999d9fe568aa91f` |

The contract manifest must record these identities and the exact section/rule refs actually consumed by an oracle/case. Missing or drifted referenced-phase identity is a hard verification failure.

---

## 3. Verification implementation scope

After explicit verifier authorization, the implementation branch may add **only** these paths:

```
.github/workflows/u05-rdp06-authoritative-verification.yml

.github/verification/u05_rdp06/build_contract_manifest.py
.github/verification/u05_rdp06/build_evidence.py
.github/verification/u05_rdp06/validate_evidence.py

diagnosis-service/src/test/resources/u05/verification/u05-verification-expectations.json
diagnosis-service/src/test/resources/u05/verification/u05-d03-precedence-expectations.json
diagnosis-service/src/test/resources/u05/verification/u05-verification-fixtures.json
diagnosis-service/src/test/resources/u05/verification/u05-verification-auth-profile.json

diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05VerificationSupport.java
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05ConsumerAdmissionVerificationTest.java
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05D03PolicyVerificationTest.java
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05ReadinessMutationVerificationTest.java
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05DownstreamRoutingVerificationTest.java
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05ReplayRecoveryVerificationTest.java
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05EvidenceHarnessVerificationTest.java
```

No `src/main/**`, POM, shared contract, runtime/effects, StateCommitter, Spring configuration, downstream Unit, or production profile change is permitted.

---

## 4. Independent static verification inputs

### 4.1 Expectation oracle

`u05-verification-expectations.json` must contain exactly U05-EV-001..060 and satisfy `U05_VERIFICATION_EXPECTATIONS_V0_1`.

Every EV must bind:

- stable fixture identity/digest;
- expected boundary/result;
- typed expected effect counts;
- expected provenance equalities where applicable;
- non-empty frozen authority refs;
- contract-manifest digest placeholder/input binding.

Expected values must be hand-derived from the frozen contracts. Production U05/D03/router code may not generate them.

### 4.2 Precedence oracle

`u05-d03-precedence-expectations.json` must enumerate every theoretical P0..P7 higher/lower pair.

Every pair must be either:

- `CONSTRUCTIBLE`: stable subcase + fixture semantics + expected higher-priority outcome; or
- `NOT_CONSTRUCTIBLE`: frozen authority refs + explicit rationale.

No implementation-selected skip is allowed.

### 4.3 Fixture manifest

`u05-verification-fixtures.json` must define every fixture as:

```
synthetic = true
contains_real_phi = false
```

Each fixture must have a stable semantic ID, source ref, canonical digest and contract-driven semantic summary.


### 4.4 Canonical digest model — non-circular and reproducible

To remove digest self-reference/cycles, the verifier freezes two contract-manifest layers.

#### Authority core

`u05-contract-manifest.json` contains an `authority_core` object with only:

- exact RDP-01..06 path/blob/current-authority-status identities;
- exact referenced Phase 5/6/8/9 path/blob identities;
- section/rule identities needed by verification;
- target implementation SHA;
- schema identity.

It MUST NOT contain oracle/fixture digests or review IDs.

The RDP-06 field named `contract_manifest_digest` is defined for this verifier as:

```
SHA256(canonical_json(authority_core))
```

Therefore the expectation oracle, precedence oracle and fixture manifest can bind `contract_manifest_digest` without a circular dependency.

#### Final manifest

The same file may additionally contain `reviewed_inputs` with:

- expectation oracle digest/review id;
- precedence oracle digest/review id;
- fixture manifest digest/review id.

The final file itself is covered by `SHA256SUMS`; no field inside the file is required to contain its own full-file digest.

#### JSON canonicalization

For every verification JSON digest:

```
encoding = UTF-8
ensure_ascii = false
keys = lexicographically sorted
separators = (",", ":")
line ending = LF
terminal newline = exactly one LF
```

Equivalent reference serialization:

```python
json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n"
```

#### Fixture digest

Each fixture is represented as:

```
{
  "fixture_id": "...",
  "fixture_digest": "...",
  "fixture_payload": { ... contract-driven semantic input ... }
}
```

`fixture_digest` MUST equal SHA-256 of canonical JSON of `fixture_payload` only. It never hashes the enclosing object or its own digest field.

CI and the evidence builder independently recompute all authority-core/oracle/precedence/fixture digests and fail closed on mismatch.


### 4.5 Static review gate

Before any authoritative run, the exact verifier implementation head must receive an independent review confirming:

- oracle completeness and contract derivation;
- precedence pairwise completeness;
- fixture identity/digests and non-PHI declaration;
- no expected value copied from observed runtime output;
- exact verifier inventory only;
- workflow supply-chain pinning;
- no production/runtime source changes.

The review must record the exact verifier head plus the three reviewed manifest SHA-256 digests.

---

## 5. Executable verification matrix

The verifier must execute all required identities.

### Governed cases

```
U05-EV-001..015  RDP-01 admission/currentness/replay
U05-EV-016..028  RDP-02 D03/precedence/status
U05-EV-029..043  RDP-03 mutation/invalidation/replay/conflict/crash
U05-EV-044..060  RDP-04 routing/permission/replay/side-effect boundary
```

Exactly 60 required EV identities.

### Harness

```
U05-HG-001 POLICY_EXPECTATION_GAP_DETECTOR_SELF_TEST
```

The injected gap must fail closed and must not invent a business result.

### Verification gates

```
U05-VG-001 no direct live U06/U08/U10/U11 owner invocation
U05-VG-002 no automatic production/Spring activation
U05-VG-003 no model/tool/external-service dependency
U05-VG-004 no direct Clinical State write bypassing K09/P01
U05-VG-005 synthetic/non-PHI fixture/evidence guard
U05-VG-006 Foundation + U01-U04 regression gate
```

No required identity may be skipped and still produce PASS.

---

## 6. Real observed evidence rule

Observed fields must be read from actual implementation objects and durable/reloaded state, including as applicable:

- `U05AdmissionResult`;
- `U05AdmittedInput`;
- `U05ClinicalReadinessDecision`;
- readiness proposal/effect;
- `CommitResult` and exact committed-version snapshot read-back;
- `CanonicalEffectLedger` admission/route/eligibility records;
- routing decision/eligibility objects;
- explicit spy/counter records proving zero live downstream/external effects.

The evidence support class may serialize observations, but it may not synthesize observed values from the expectation oracle.

---

## 7. Cross-contract provenance checks

For every admitted D03 case:

```
d03_source_admission_ref
= admission_id

d03_source_readiness_input_set_identity
= admitted_readiness_input_set_identity
= manifest/evidence readiness_input_set_identity
```

For RESTRICTED cases:

```
inbound_restricted_permission_ref
= admission_result_restricted_permission_ref
= admitted_restricted_permission_ref
= d03_restricted_permission_ref
= readiness_source_restricted_permission_ref
```

These values must be independently read from the corresponding runtime objects, not populated from one test constant.

---

## 8. Typed side-effect evidence

Every EV must compare expected and observed maps containing at minimum:

```
state_commit_count
readiness_effect_count
invalidation_effect_count
route_decision_count
route_eligibility_count
scheduler_target_intent_count
downstream_unit_invocation_count
external_delivery_count
external_tool_model_call_count
```

Any non-zero observed count requires a durable/inspectable evidence ref.

For the current non-production slice:

```
downstream_unit_invocation_count = 0
external_delivery_count = 0
external_tool_model_call_count = 0
```

---

## 9. Verification overlay integrity

The authoritative workflow must first checkout the exact verifier head and verify its exact authorized file inventory.

It must then copy the approved verifier-only files to a temporary directory and checkout:

```
U05_IMPLEMENTATION_SHA
= 1dc49c4097841523a9445dab078bc5a3d1ad1259
```

Before applying the test overlay it must assert:

```
git rev-parse HEAD == U05_IMPLEMENTATION_SHA
git status --porcelain == empty
```

After copying verifier tests/resources/scripts, it must assert that no path under:

```
diagnosis-service/src/main/**
contracts/**
```

changed relative to the exact target.

The verifier overlay must never modify the implementation branch or target commit.

---

## 10. Authoritative workflow minimum gates

The workflow must implement RDP-06 G0..G15:

```
G0  governance/auth-profile guard
G1  exact target identity
G1A immutable action pins + toolchain provenance
G2  shared-contract dependency install
G3  compile
G4  structural authorization guards
G5  focused U05 verification suites
G6  evidence harness
G7  full diagnosis-service regression
G8  evidence build
G9  schema/completeness validation
G10 expected-vs-observed comparison
G11 POLICY_EXPECTATION_GAP gate
G12 hard-boundary side-effect gate
G13 SHA-256 checksums
G14 artifact upload >= 90 days
G15 final identity/status report
```

Any failure => overall FAIL.

The workflow may emit only:

```
PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW
FAIL
```

It may not self-declare final U05 verification PASS.

---

## 11. Supply-chain and toolchain provenance

Permissions:

```
contents: read
```

No write permissions.

Required action pins:

```
actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1
actions/setup-java@b6effb05e454b25005698d916606bdc6ffcbf961
actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02
```

No floating tags are permitted.

The artifact must record:

- runner OS/image;
- JDK vendor/version;
- Maven version;
- Python version;
- materially relevant verification-tool versions;
- exact action pins;
- workflow file SHA-256;
- verifier head SHA;
- implementation SHA;
- workflow run/attempt/ref identity.

---

## 12. Durable evidence bundle

Artifact logical schema:

```
U05_NONPROD_VERIFICATION_EVIDENCE_V0_1
```

Required files:

```
evidence.json
u05-case-evidence.json
u05-harness-self-test-evidence.json
u05-verification-gate-evidence.json
u05-d03-precedence-evidence.json
u05-verification-expectations.json
u05-d03-precedence-expectations.json
u05-verification-fixtures.json
u05-contract-manifest.json
workflow-provenance.txt
auth-profile.json
regression-summary.json
structural-guard-summary.json
SHA256SUMS
focused Surefire XML reports
evidence-harness Surefire XML report
```

Upload requirements:

```
retention-days >= 90
if-no-files-found = error
overwrite = false
```

---

## 13. Acceptance thresholds

A successful authoritative run requires all of:

```
60/60 EV PASS
HG-001 PASS
6/6 VG PASS
all CONSTRUCTIBLE precedence subcases executed and PASS
all NOT_CONSTRUCTIBLE pairs explicitly frozen with authority/rationale
normal policy_expectation_gap_count = 0
failures = 0
errors = 0
unexpected skips = 0
all expected/observed boundaries match
all contract-defined expected/observed results match
all typed effect-count maps match key-by-key
all hard-boundary flags satisfied
contract manifest complete
all checksums valid
full regression has no new failure/error
reviewed oracle/fixture digests == CI-consumed digests
```

Missing executable surface is `NOT_IMPLEMENTED -> FAIL`, never skip-to-pass.

---

## 14. Independent evidence-only review

After a green authoritative run, the artifact must be downloaded and independently checked for:

- GitHub artifact/platform identity;
- SHA256SUMS validity;
- exact implementation SHA;
- exact workflow/verifier identity;
- exact contract blob identities;
- reviewed oracle/precedence/fixture digests;
- completeness/uniqueness of 60 EV + HG + 6 VG;
- precedence coverage;
- expected authority refs;
- observed provenance;
- typed effect counts;
- no live downstream/external effects;
- full regression;
- no unexpected skip.

Only after evidence-only review PASS may a combined implementation/evidence review be performed.

Final `PASS` requires both reviews. A green workflow alone remains `PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW`.

---

## 15. Long-term accepted evidence

After final independent acceptance, create repository-governed:

```
U05_Accepted_Verification_Evidence_v0.1.json
```

It must retain the sanitized per-case audit summary required by RDP-06 Section 30 and contain no PHI.

This file is **not** part of the initial verifier implementation scope; it is created only after independent artifact acceptance and combined review, under a separate closure record.

---

## 16. Authorization boundary

This design does not authorize verifier implementation or execution.

Proposed verifier authorization:

```
AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001
```

If later explicitly authorized, it permits only:

```
VERIFICATION_ONLY
PINNED_TARGET_SHA_ONLY
FROZEN_RDP06_ONLY
SYNTHETIC_NON_PHI_ONLY
NO_PRODUCTION_RUNTIME_SOURCE_CHANGE
NO_LIVE_DOWNSTREAM_EXECUTION
NO_EXTERNAL_DELIVERY
NO_REAL_PATIENT_TRAFFIC
NO_MERGE
```

Implementation must be reviewed at its exact verifier head before the authoritative PR-triggered run is accepted.

---

## 17. Required sequence

```
1. independent design review
2. explicit owner verifier authorization decision
3. implement exact verification-only overlay
4. independent overlay/oracle/fixture/workflow review
5. authoritative exact-target workflow run
6. independent evidence-only review
7. combined implementation/evidence review
8. explicit U05 implementation-verification closure record
9. only then consider merge authorization
```

Current after this design commit:

```
U05 implementation target
= 1dc49c4097841523a9445dab078bc5a3d1ad1259

RDP-06 authoritative verification
= NOT_RUN

AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001
= NOT_GRANTED
```

---

## 18. Independent Design Review Remediation

Initial independent design review:

```
PR #203
review_id = 5274680007
verdict = REVISE_REQUIRED
reviewed head = 56b19d28413beffcf970cba44f089c6f83af09d4
```

Findings:

```
BF-U05-RDP06-VR-IR-01
= REFERENCED_PHASE_AUTHORITY_IDENTITIES_NOT_BOUND

BF-U05-RDP06-VR-IR-02
= HASH_CANONICALIZATION_AND_DIGEST_CYCLE_UNDEFINED
```

Remediation:

- IR-01: exact Phase 5 / 6 / 8 / 9 document blob identities are now frozen and must be included in the authority core and case rule refs where applicable.
- IR-02: `contract_manifest_digest` is now explicitly the digest of a non-circular `authority_core`; full manifest metadata is checksum-covered externally. JSON canonicalization and self-excluding fixture-payload digest semantics are frozen.

Current:

```
BF-U05-RDP06-VR-IR-01 = REMEDIATED / TARGETED_REVIEW_PENDING
BF-U05-RDP06-VR-IR-02 = REMEDIATED / TARGETED_REVIEW_PENDING

AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001
= NOT_GRANTED
```
