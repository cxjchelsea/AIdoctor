# U05 RDP-06 Authoritative Verifier Target Rebind AV-04 Amendment v0.1

> Amendment type: second exact-target rebind after AV-04 remediation  
> Prior authorized rebind: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001 = AUTHORIZED`  
> Prior authorized target: `261ee5525c8260e93db19173ffbde89a8af6810d`  
> Proposed new target: `2b7926afd69de9fe2224d8a5b69e91c02c2db495`  
> Proposed authorization ID: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002`  
> Status: **PROPOSED / INDEPENDENT_REVIEW_PENDING**

## 1. Trigger

Authoritative RDP-06 run `35709046097` failed before verifier overlay execution because the baseline regression gate detected a production-source dependency from:

    U05SyntheticClinicalReadinessSnapshotAdapter
    -> SyntheticVersionedStateRepository

This was recorded as:

    BF-U05-RDP06-AV-04

The reviewed remediation moved only the concrete synthetic adapter from:

    diagnosis-service/src/main/java/.../runtime/u05/

to:

    diagnosis-service/src/test/java/.../runtime/u05/

with no content change and no change to the U05 snapshot port, commit service, StateCommitter, frozen RDP semantics, routing, D03, or Clinical Readiness.

## 2. Remediation evidence

Design:
- PR #211
- review `5276182930 = PASS`

Implementation:
- PR #212
- exact head `2b7926afd69de9fe2224d8a5b69e91c02c2db495`
- exact-head implementation re-review `5285624416 = PASS_FOR_REBIND`

Full regression:
- PR #213
- Phase A CI MVP run `35709800508 = SUCCESS`
- diagnosis-service java job = SUCCESS
- all workflow jobs = SUCCESS

Therefore:

    BF-U05-RDP06-AV-04
    = CLOSED_AS_IMPLEMENTATION_SURFACE_BLOCKER

## 3. Exact target delta

Relative to prior authorized target:

    261ee5525c8260e93db19173ffbde89a8af6810d

the proposed new target changes exactly:

1. rename only:
   `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/U05SyntheticClinicalReadinessSnapshotAdapter.java`
   ->
   `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/U05SyntheticClinicalReadinessSnapshotAdapter.java`

2. add reviewed design document:
   `docs/current/06_开发单元/U05_RDP06_AV04_Synthetic_Adapter_Targeted_Remediation_v0.1.md`

No shared production runtime source changes.
No frozen RDP changes.

Any additional target-path drift invalidates this amendment.

## 4. Verifier overlay consequence

The existing verifier overlay in PR #210 remains semantically reviewed, but its target binding is stale because these files hard-code the prior target/authorization identity:

- `.github/workflows/u05-rdp06-authoritative-verification.yml`
- `diagnosis-service/src/test/resources/u05/verification/u05-verification-auth-profile.json`
- `.github/verification/u05_rdp06/build_contract_manifest.py`

Therefore the prior static verifier review may be reused only for unchanged verifier semantics.

After owner authorization of REBIND-002, a bounded **overlay target-binding amendment** is required to update only:

    U05_IMPLEMENTATION_SHA
    authorization id / authorization record SHA / record blob
    target constant used by the contract-manifest builder
    corresponding target-binding provenance

The following must remain unchanged unless independently re-reviewed as semantic changes:

    60 EV expectations
    28 P0..P7 pairwise precedence records + special proofs
    fixture semantics
    EV expected results
    HG-001
    VG coverage
    evidence schema
    G0..G15 logic
    no-live/no-production boundaries

## 5. Target authority precedence

If explicitly authorized:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002
    >
    REBIND-001
    >
    parent verifier target field

for implementation-target identity only.

REBIND-001 remains historical for target `261ee552...`.

The verifier must fail closed unless exactly one active latest owner-authorized rebind resolves to:

    2b7926afd69de9fe2224d8a5b69e91c02c2db495

and workflow/auth-profile/manifest-builder all bind the same target.

## 6. Authorization scope

If authorized, REBIND-002 permits only:

1. bounded verifier overlay target-binding amendment;
2. independent exact-head overlay re-review;
3. authoritative exact-target RDP-06 CI;
4. durable evidence generation;
5. independent evidence-only review.

Still prohibited:

- U05 production semantic modification;
- shared runtime semantic modification;
- frozen RDP changes;
- merge;
- production activation;
- live downstream execution;
- external delivery;
- real-patient/PHI traffic;
- release activation.

## 7. Required sequence

    independent rebind review
    -> explicit Owner AUTHORIZE / REVISE / REJECT
    -> overlay target-binding amendment
    -> independent exact-head overlay re-review
    -> authoritative RDP-06 run
    -> durable evidence
    -> independent evidence-only review
    -> combined closure review

Current:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002
    = NOT_GRANTED

    RDP-06 authoritative verification
    = BLOCKED_PENDING_REBIND_002
