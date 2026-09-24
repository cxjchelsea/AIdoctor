# U06 New Exact-Head Implementation Re-Review v0.1

**Review verdict:** REVISE_REQUIRED  
**Review type:** Independent exact-head implementation re-review  
**Reviewed implementation semantic/code head:** `fe525e6de13dcb2e6f51a09b4acd84a068778d84`  
**Implementation branch head at review start:** `0f49b29e5bfed22ccbd5df5b934e76b131c845f9`  
**Difference `fe525...0f49`:** one documentation-only triage/status record; no implementation change  
**Prior semantic PASS head:** `1d427fd09957767c938e34883dcbcda85b7fd992`  
**Post-remediation CI evidence:** GitHub Actions run `35952168656` = SUCCESS

## 1. Review scope

This re-review examines the exact U06 PROFILE-B bounded synthetic structural implementation after authoritative-verification remediation.

The review checks:

1. whether the remediation matches the frozen U06 RDP / aggregate / physical-design semantics;
2. whether any verifier or observation change weakened the frozen oracle;
3. whether implementation changes remain inside the authorized PROFILE-B non-production scope;
4. whether replay/currentness, state mutation, delivery/wait, trace and recovery invariants remain fail-closed;
5. whether the remediated code is eligible for implementation semantic re-freeze.

This review does **not** grant merge, production, PROFILE-A, real C03/D04, external delivery, live U07, production Scheduler routing, release activation, or real-patient authorization.

## 2. Positive findings

The following review findings are accepted:

- the remediation did not modify the frozen oracle or fixture semantic authority;
- the `NO_SELECTION_OR_FAILED` verifier comparison interprets an already frozen disjunctive expectation rather than weakening it;
- P01 synthetic repository construction was removed from production U06 source and moved behind the injected backend/test factory boundary;
- the updated IRR01 static checks now verify that physical boundary instead of requiring a production source to directly construct `SyntheticVersionedStateRepository`;
- replay/conflict observation counters were corrected to measure second-attempt mutation/version deltas rather than cumulative values;
- missing identity evidence for trace, delivery and U07 replay was changed from asserted prose to observed SUT-derived evidence;
- the targeted runtime changes for F3 dependency failure, USER_UNKNOWN exact re-ask suppression and pending-question conflict are directionally consistent with the frozen cases;
- post-remediation run `35952168656` completed with engineering success and 189 / 189 verifier cases PASS, full regression PASS, isolation PASS and zero observed external socket calls;
- no Oracle/fixture digest was changed to manufacture the green result.

These findings are not sufficient for PASS because two fail-closed admission blockers remain.

## 3. Blocking finding B-U06-RR-01 — replay reattaches before currentness / authority revalidation

### 3.1 Frozen authority

U06-RDP-01 freezes exact replay as:

```text
reload authoritative state
→ revalidate source currentness
→ reconcile admission ledger
→ attach prior U06AdmittedInput
→ replay_disposition = REATTACHED
```

It explicitly states that replay may not skip:

- current Safety / Gate;
- source validity/currentness;
- permission validity;
- binding applicability.

It also freezes the admission ordering so replay/admission identity reconciliation occurs only after the authoritative identity/currentness/Safety/policy checks.

The acceptance case is:

```text
same admission identity
+ same fingerprint
+ still-current source
→ REATTACHED
```

A prior admission whose source is stale must not be reattached as current.

### 3.2 Current implementation

At reviewed head `fe525e6...`, `U06AdmissionService.admit(...)` executes:

```java
String existing=admittedFingerprints.get(id);
if(existing!=null) {
    if(!existing.equals(fp))throw new IllegalStateException("U06_ADMISSION_REPLAY_CONFLICT");
    return Admission.admitted(id,fp,r,true);
}

String staticRejection=validateStatic(r);
...
```

Therefore an existing same-id/same-fingerprint request is returned as replay **before** `validateStatic(r)` checks:

- source authority currentness;
- source supersession;
- Gate state;
- restricted permission;
- route consequence;
- consultation/CDP match;
- dependency applicability;
- binding/policy requirements.

### 3.3 Impact

This permits the following invalid sequence:

```text
first request: source CURRENT / Gate ALLOW / dependency current
→ ADMITTED

same admission identity + same canonical fingerprint
but current admission evidence now says:
  source stale / superseded
  or Gate stale
  or permission revoked/unavailable
  or dependency expired
→ current code returns REATTACHED before revalidation
```

This directly violates RDP-01 replay/currentness semantics.

The existing U06-EV-015 and U06-EV-017 tests do not cover the cross-event transition:

```text
prior admitted
→ authority becomes non-current
→ same admission identity replay
```

They test exact replay and superseded source independently, so the 189/189 green run does not disprove this blocker.

### 3.4 Required remediation

The admission path must be changed so that:

1. authoritative/current admission evidence is revalidated before replay reattachment;
2. same-id/different-fingerprint still fails closed as `U06_ADMISSION_REPLAY_CONFLICT`;
3. same-id/same-fingerprint can become `REATTACHED` only when the current source/Gate/permission/dependency basis remains lawful;
4. stale/superseded/revoked/expired replay returns the frozen rejection/failure disposition and causes zero downstream effects;
5. replay should attach the prior normalized admitted input/accepted authority basis rather than silently treating a new raw request as the prior admission.

Required regression coverage must include at least:

- admitted → source stale;
- admitted → source superseded;
- admitted → Gate stale;
- admitted → restricted permission denied/unavailable;
- admitted → dependency expired/unavailable;
- exact still-current replay remains REATTACHED;
- all rejected replay paths show zero C03/D04/state/delivery/wait side effects.

**Disposition:** BLOCKING.

## 4. Blocking finding B-U06-RR-02 — implicit positive admission evidence fallback is not fail-closed

### 4.1 Frozen authority

RDP-01 requires the request/admission boundary to carry authoritative refs/evidence and requires U06 to independently establish that those refs remain current/lawful.

It explicitly rejects the reasoning pattern:

```text
caller says state is current
→ therefore currentness is trusted
```

and states that synthetic fixtures may be used to verify consumer/currentness/fail-closed behavior, but synthetic fixture status is not itself activation or authority.

Admission ordering is explicitly fail-closed.

### 4.2 Current implementation

`U06ProfileBRequest` retains a public constructor that does not receive `U06AdmissionEvidence`.

That constructor automatically executes:

```java
this.admissionEvidence=U06AdmissionEvidence.syntheticCurrentAllow();
```

Therefore a caller that omits authoritative admission evidence does not fail closed. The request object manufactures a fully positive synthetic authority state:

- source present/current;
- valid route;
- Gate ALLOW;
- permission not required;
- consultation/CDP match;
- active synthetic dependency;
- not superseded;
- synthetic marker valid.

This also means the `U06AdmissionService.validateStatic()` null-evidence rejection cannot protect callers using this constructor.

### 4.3 Impact

The production-source API currently has this behavior:

```text
no explicit admission evidence supplied
→ syntheticCurrentAllow() injected automatically
→ admission checks see positive evidence
```

That is the inverse of the frozen fail-closed contract.

The fact that PROFILE-B is non-production and current live wiring is absent limits operational exposure, but it does not make the implementation semantically correct for the reviewed bounded slice.

### 4.4 Required remediation

One of the following equivalent fail-closed forms is required:

- remove the implicit-evidence constructor from production U06 source; or
- require explicit `U06AdmissionEvidence` in all production-source constructors and keep positive synthetic fixture construction exclusively in test/verification factories; or
- make absence of externally supplied evidence unrepresentable except through a test-only adapter.

Required tests:

- request construction without explicit admission evidence cannot yield an admissible request;
- synthetic positive evidence is created only by explicit test/verification fixture infrastructure;
- negative/currentness fixture cases remain independently controllable.

**Disposition:** BLOCKING.

## 5. Required follow-up finding RF-U06-RR-01 — exact-head provenance must be refreshed only after blockers close

The post-remediation verifier run is useful evidence, but it is not yet final exact-head closure evidence.

At `fe525e6...`:

- `u06-contract-manifest.json` still records `implementation_semantic_pass_sha = 1d427fd...`;
- the workflow/verifier invocation still binds the pre-remediation semantic SHA;
- the observed shared-runtime change manifest still names an older reviewed implementation target.

This is expected before semantic re-freeze, but it means run `35952168656` must remain classified as **pre-re-review remediation evidence**.

After the blocking fixes pass a targeted re-review, the re-freeze step must bind the new exact implementation semantic SHA and regenerate/refresh the exact-head provenance manifests before the fresh authoritative verification used for closure.

**Disposition:** REQUIRED AFTER BLOCKER CLOSURE / NOT A SEPARATE IMPLEMENTATION BLOCKER.

## 6. Review of authorization boundary

No evidence was found that the reviewed changes authorize or activate:

- PROFILE-A;
- real PHI;
- real C03/D04;
- real patient-facing content;
- external delivery;
- production data-store writes;
- direct F1 live activation;
- live U07;
- production Scheduler routing.

The current structural no-live guard and the successful isolated network-spy run support this boundary.

The blockers above concern correctness of the bounded PROFILE-B admission implementation, not scope escape.

## 7. Verdict

```text
U06 New Exact-Head Implementation Re-Review
= REVISE_REQUIRED

Reviewed implementation semantic/code head
= fe525e6de13dcb2e6f51a09b4acd84a068778d84

B-U06-RR-01 Replay Currentness / Reattachment Ordering
= OPEN / BLOCKING

B-U06-RR-02 Implicit Positive Admission Evidence Fallback
= OPEN / BLOCKING

RF-U06-RR-01 Exact-Head Provenance Refresh
= REQUIRED AFTER BLOCKER CLOSURE

Implementation Semantic Re-Freeze
= NOT_PERMITTED

Fresh Closure Authoritative Verification
= NOT_PERMITTED YET

Implementation Verification Closure
= NOT_PASSED

Merge Authorization
= NOT_GRANTED
```

## 8. Next permitted work

The next permitted step is:

> **U06 Exact-Head Re-Review Targeted Remediation for B-U06-RR-01 and B-U06-RR-02**

After those two blockers are fixed:

1. run targeted admission/replay regression;
2. run full engineering + authoritative candidate verification;
3. perform a targeted exact-head implementation re-review;
4. only if that re-review is PASS, establish the new implementation semantic re-freeze;
5. then execute the fresh post-re-freeze authoritative RDP-06 verification and downstream evidence reviews.

No merge or production authorization is granted by this review.
