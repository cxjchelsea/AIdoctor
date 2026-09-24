# U06 Targeted Exact-Head Implementation Re-Review After RR Remediation v0.1

**Review verdict:** PASS  
**Review type:** Targeted independent exact-head implementation re-review  
**Reviewed implementation head:** `66fa3c078dce5e304e2ada9d6c0df0ff9fbde661`  
**Remediation base:** `0f49b29e5bfed22ccbd5df5b934e76b131c845f9`  
**Prior re-review:** `U06_New_Exact_Head_Implementation_ReReview_v0.1.md` = REVISE_REQUIRED  
**Candidate verification run:** GitHub Actions `35958776125` = SUCCESS

## 1. Scope

This review is limited to closure of:

- `B-U06-RR-01` — Replay Currentness / Reattachment Ordering;
- `B-U06-RR-02` — Implicit Positive Admission Evidence Fallback.

It also checks that the targeted remediation did not alter the frozen U06 Oracle/fixture semantics, did not expand PROFILE-B scope, and did not activate any production/live boundary.

The remediation diff from `0f49b29...` to `66fa3c0...` contains four files only:

- `U06AdmissionService.java`;
- `U06ProfileBRequest.java`;
- `U06ProfileBStructuralTest.java`;
- `U06AuthoritativeNetworkSpyTest.java`.

No frozen Oracle, fixture, contract manifest, production scheduler, real C03/D04, live U07 or external delivery implementation was modified.

## 2. B-U06-RR-01 closure review

### 2.1 Required invariant

Frozen RDP-01 requires:

```text
same admission identity + different fingerprint
→ REPLAY_CONFLICT

exact same replay
→ current authority/currentness must still be revalidated
→ only then may prior admission be REATTACHED
```

Replay may not bypass source currentness, Gate/Safety, restricted permission, dependency applicability or binding/policy validity.

### 2.2 Remediated implementation

At `66fa3c0...`, admission now separates **conflict detection** from **reattachment authorization**:

```java
String existing=admittedFingerprints.get(id);

if(existing!=null&&!existing.equals(fp))
    throw new IllegalStateException("U06_ADMISSION_REPLAY_CONFLICT");

String staticRejection=validateStatic(r);
if(staticRejection!=null)
    return Admission.rejected(...);

if(actualCurrentStateVersion!=r.getAuthoritativeClinicalStateVersion()
        &&!exactAuthoritativeReplayEvidence)
    return Admission.rejected(...REJECTED_STALE_STATE...);

if(existing!=null)
    return Admission.admitted(...,true);
```

This preserves both frozen requirements:

1. same protected identity + changed canonical fingerprint still fails as replay conflict;
2. same identity + same fingerprint does **not** reattach until current authority/currentness has passed revalidation.

### 2.3 Targeted regression coverage

The new structural test `admissionReplayRevalidatesCurrentAuthorityBeforeReattach` proves a prior admitted identity does not remain current merely because it is in the admission ledger.

It covers:

- source no longer current;
- source superseded;
- Gate stale;
- restricted permission denied;
- restricted permission unavailable;
- dependency expired;
- still-current exact replay remains REATTACHED.

The focused U06 structural test job passed.

### 2.4 Disposition

```text
B-U06-RR-01
= CLOSED / PASS
```

## 3. B-U06-RR-02 closure review

### 3.1 Required invariant

The U06 admission boundary must fail closed when authoritative admission evidence is absent.

Synthetic verification infrastructure may create positive synthetic evidence explicitly, but production-source request construction must not silently invent:

- source currentness;
- Gate ALLOW;
- permission authority;
- dependency applicability;
- synthetic marker validity.

### 3.2 Remediated implementation

The compatibility constructor of `U06ProfileBRequest` no longer calls:

```text
U06AdmissionEvidence.syntheticCurrentAllow()
```

It now leaves admission evidence absent:

```java
this.admissionEvidence=null;
```

`U06AdmissionService.validateStatic()` already fails closed on absent evidence:

```java
if(evidence==null)return REJECTED_SOURCE_AUTHORITY;
```

Executable verification/test requests that require positive synthetic authority now supply `U06AdmissionEvidence.syntheticCurrentAllow()` explicitly.

This retains source compatibility while removing the implicit positive-authority fallback.

### 3.3 Targeted regression coverage

The new test `requestWithoutExplicitAdmissionEvidenceFailsClosed` proves:

```text
request created without explicit admission evidence
→ admissionEvidence == null
→ NOT_ADMITTED
→ REJECTED_SOURCE_AUTHORITY
```

The authoritative network-spy request was also changed to explicit test fixture evidence rather than relying on the compatibility constructor.

### 3.4 Disposition

```text
B-U06-RR-02
= CLOSED / PASS
```

## 4. Regression / verification evidence

GitHub Actions run `35958776125` completed successfully at the reviewed implementation head.

```text
u06-engineering
= SUCCESS

u06-rdp06-authoritative
= SUCCESS

authoritative verdict
= PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW

PASS = 189
FAIL = 0
NOT_EXECUTED = 0

runtime_observation_count = 115
full_regression_pass = true
external_call_spy_enabled = true
external_call_spy_observed_count = 0
failed_case_ids = []
missing_case_ids = []
```

Evidence artifact:

- artifact id: `10791517514`
- artifact digest: `sha256:37dea31afa55b8495bac69e7fb575291c3af2d2336d30500dc9a05971208bbb8`

The preceding candidate run `35958558946` failed only because the first remediation ordering caused frozen `U06-EV-016` replay conflict to be converted into stale rejection. That was corrected by commit `66fa3c0...`: conflict detection remains early, while reattachment remains gated behind current authority validation.

## 5. Oracle / authority integrity

The targeted remediation did not modify:

- `u06-verification-expectations.json`;
- `u06-verification-fixtures.json`;
- Oracle/fixture review-gate records;
- frozen RDP-01..06 documents;
- aggregate semantic authority;
- physical-design semantic authority.

Therefore the green result was not obtained by weakening reviewed expected semantics.

## 6. Scope / authorization review

No scope expansion was found.

Still prohibited / disabled:

- PROFILE-A;
- real PHI;
- production clinical state writes;
- real C03/D04;
- real patient-facing content;
- external delivery;
- direct live F1 activation;
- live U07;
- production Scheduler routing;
- release activation;
- real-patient traffic.

## 7. Remaining required follow-up

The prior review recorded:

```text
RF-U06-RR-01
Exact-Head Provenance Refresh
```

This remains required, but it is now a **re-freeze task**, not an open implementation defect.

Current contract manifest / workflow provenance still references the prior implementation semantic PASS SHA `1d427fd...`.

The correct next step is to establish a new semantic freeze at the reviewed implementation head and then refresh the exact-head provenance bindings before running the authoritative verification that will be used for final closure evidence.

The successful run `35958776125` therefore remains candidate/pre-re-freeze evidence and must not be mislabeled as final closure evidence.

## 8. Verdict

```text
U06 Targeted Exact-Head Implementation Re-Review
= PASS

REVIEWED_IMPLEMENTATION_HEAD
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661

B-U06-RR-01
= CLOSED / PASS

B-U06-RR-02
= CLOSED / PASS

RF-U06-RR-01
= OPEN AS RE-FREEZE FOLLOW-UP

Implementation Semantic Re-Freeze
= PERMITTED NEXT

Fresh Post-Re-Freeze Authoritative RDP-06 Verification
= NOT YET EXECUTED

Independent Evidence-Only Review
= NOT STARTED

Combined Implementation / Evidence Review
= NOT STARTED

Implementation Verification Closure
= NOT_PASSED

Merge Authorization
= NOT_GRANTED
```

## 9. Next permitted step

> **U06 Implementation Semantic Re-Freeze at exact reviewed implementation head `66fa3c078dce5e304e2ada9d6c0df0ff9fbde661`, including RF-U06-RR-01 exact-head provenance refresh.**

Only after that re-freeze should the fresh authoritative RDP-06 verification run be treated as the candidate for final evidence review and closure.
