# U05 RDP-06 Authoritative Verifier Authorization Decision v0.1

> Status: OWNER AUTHORIZED  
> Authorization ID: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001`  
> Reviewed design head: `583bfb8bf5ee411e7b95a2a2a779d758bf2ec5ab`  
> Design review: PR #203 / review `5274687058` / PASS  
> Target U05 implementation SHA: `1dc49c4097841523a9445dab078bc5a3d1ad1259`

## 1. Decision subject

Authorize a verification-only implementation and execution path for the already-frozen U05 RDP-06 Verification / Durable Evidence Plan.

This authorization, if explicitly granted by the repository owner, permits only the exact verifier scope reviewed in PR #203.

## 2. Permitted scope

```
VERIFICATION_ONLY
PINNED_TARGET_SHA_ONLY
FROZEN_RDP06_ONLY
SYNTHETIC_NON_PHI_ONLY
NO_PRODUCTION_RUNTIME_SOURCE_CHANGE
NO_SHARED_RUNTIME_SEMANTIC_CHANGE
NO_LIVE_UPSTREAM_CUTOVER
NO_LIVE_DOWNSTREAM_EXECUTION
NO_EXTERNAL_DELIVERY
NO_REAL_PATIENT_TRAFFIC
NO_MERGE
```

Permitted verifier implementation paths are exactly those frozen in the reviewed runner design:

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

## 3. Exact target and authority binding

The verifier must test exactly:

```
U05_IMPLEMENTATION_SHA
= 1dc49c4097841523a9445dab078bc5a3d1ad1259
```

It must bind the exact RDP-01..06 and referenced Phase 5/6/8/9 identities frozen by the reviewed design.

Any target drift, verifier-scope drift, authority identity drift, oracle/fixture digest drift, or workflow drift invalidates this authorization.

## 4. Mandatory sequence

If authorized:

```
1. implement verification-only overlay
2. independent exact-head overlay/oracle/fixture/workflow review
3. authoritative PR-triggered exact-target run
4. artifact generation
5. independent evidence-only review
6. combined implementation/evidence review
7. explicit U05 implementation-verification closure record
```

A green workflow may only produce:

```
PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW
```

It may not self-declare final U05 verification PASS.

## 5. Still prohibited

This authorization does not permit:

- modification of `diagnosis-service/src/main/**`;
- Shared Contracts changes;
- StateCommitter or Runtime Effect Ledger semantic changes;
- Spring/default production activation;
- live U06/U08/U10/U11/U14 execution;
- production Clinical State mutation;
- external/model/tool delivery side effects;
- real-patient or PHI verification data;
- merge of PR #201 or verifier branches;
- production authorization or release activation.

## 6. Owner decision

Current:

```
AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001
= NOT_GRANTED
```

Allowed explicit owner decisions:

```
AUTHORIZE
REVISE
REJECT
```

Repository owner explicitly selected `AUTHORIZE` against this reviewed package. The authorization is therefore granted for the exact bounded verifier scope above. Any scope/target/design drift requires a new review/authorization decision.
