# U05 RDP-06 REBIND-002A Target-Binding Authorization Decision v0.1

> Authorization ID: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002A`  
> Parent target authorization: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002 = AUTHORIZED`  
> Reviewed amendment: PR #216  
> Reviewed amendment head: `3a518619de51c4378f787165443b2a4a0ca70b1c`  
> Independent review: **PASS** / review_id `5285738351`  
> Exact implementation target: `2b7926afd69de9fe2224d8a5b69e91c02c2db495`  
> Status: **AUTHORIZED / CONSUMED_FOR_TARGET_BINDING_AMENDMENT**

## 1. Decision question

Whether to authorize the exact eight-file mechanical target/provenance amendment required to consume the already-authorized REBIND-002 target in PR #210.

This decision does not change the target authorized by REBIND-002.

It changes only the permitted verifier target-binding file inventory after exact overlay inspection found that eight files, not three, carry target-bound provenance.

## 2. Exact authorized-if-approved file inventory

If owner selects AUTHORIZE, changes are permitted only in:

1. `.github/workflows/u05-rdp06-authoritative-verification.yml`
2. `diagnosis-service/src/test/resources/u05/verification/u05-verification-auth-profile.json`
3. `.github/verification/u05_rdp06/build_contract_manifest.py`
4. `.github/verification/u05_rdp06/build_evidence.py`
5. `.github/verification/u05_rdp06/validate_evidence.py`
6. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05VerificationSupport.java`
7. `diagnosis-service/src/test/resources/u05/verification/u05-verification-expectations.json`
8. `diagnosis-service/src/test/resources/u05/verification/u05-d03-precedence-expectations.json`

No ninth verifier path is authorized.

## 3. Exact permitted semantic delta

Only:

    implementation target SHA
    active rebind authorization ID
    authorization record commit/blob/path
    target-bound authority-core digest
    contract_manifest_digest in static expectation/precedence records
    static-review input digests mechanically resulting from those byte changes

may change.

Frozen exact target:

    2b7926afd69de9fe2224d8a5b69e91c02c2db495

Active target authorization:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002

Immutable owner authorization record:

    commit
    = ce83f5a2e930db08e2415012a22170df12d66b44

    blob
    = b288077ea7e22bb03292cff36641564f0fb6eff6

    path
    = docs/current/06_开发单元/U05_RDP06_Authoritative_Verifier_Target_Rebind_AV04_Authorization_Decision_v0.1.md

New authority-core digest:

    570ca5303f601df2caf672bfb7c9e61fe2c5a5add86e288cf782c8e6030712e5

## 4. Mandatory semantic invariants

The amendment must prove:

    all 60 EV case IDs unchanged
    all 60 EV expected results unchanged
    all expected effect-count maps unchanged
    all expected authority refs unchanged
    all 28 pairwise precedence semantics unchanged
    all special precedence proofs unchanged
    fixture canonical JSON byte-identical
    HG-001 semantics unchanged
    VG semantics unchanged
    G0..G15 control semantics unchanged
    hard production/live boundaries unchanged.

No business or clinical expected-result re-derivation is authorized.

## 5. Static review consequence

Because expectation and precedence JSON bytes change through digest-only substitution:

    old expectation SHA-256
    = d955969ece67c84810eb4e15f66ecc3341fe7afcd80d9747f7fe7e9ebb5da6c2

    old precedence SHA-256
    = 978d6a91cbec92280588ab4d7d0316445cfe5c1e0c2dad94a96dbbfa287b1040

become historical.

After the eight-file amendment:

    recompute expectation SHA-256
    recompute precedence SHA-256
    prove fixture SHA-256 unchanged
    independently review exact verifier head and the three static input digests

before authoritative CI.

The previous PR #210 static review may support unchanged semantics but may not be cited as reviewing the new bytes.

## 6. Required post-authorization sequence

If owner selects AUTHORIZE:

    1. amend exactly the eight reviewed PR #210 files
    2. structured semantic-diff guard
    3. recompute static input digests
    4. independent exact-head overlay/static review
    5. freeze reviewed exact head and digests
    6. authoritative RDP-06 exact-target CI
    7. durable artifact generation
    8. independent evidence-only review
    9. combined implementation/evidence closure review

## 7. Still prohibited

This authorization does not permit:

    any U05 production source change
    any shared runtime change
    any new verifier file
    any EV expected-outcome change
    any precedence semantic change
    any fixture semantic change
    frozen RDP changes
    merge
    production activation
    live downstream execution
    external delivery
    model/tool external side effects
    PHI / real-patient traffic
    release activation.

## 8. Owner options

    AUTHORIZE
    REVISE
    REJECT

Current:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002
    = AUTHORIZED

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002A
    = AUTHORIZED

    RDP-06 authoritative verification
    = AUTHORIZED_TO_RESUME_AFTER_EXACT_HEAD_OVERLAY_REVIEW

    U05 Implementation Verification
    = NOT_PASSED


## 9. Repository Owner Decision

The repository owner explicitly selected:

    AUTHORIZE

against exact reviewed decision-package head:

    e01c95072cdd8d494df58f322f88620bf42c54a5

Therefore:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002A
    = AUTHORIZED

Exact implementation target remains:

    2b7926afd69de9fe2224d8a5b69e91c02c2db495

Authorized verifier amendment:

    exactly eight reviewed verifier paths
    target/provenance/digest binding only

Required continuation:

    amend exactly eight paths
    -> structured semantic diff guard
    -> recompute static input digests
    -> independent exact-head overlay/static review
    -> authoritative exact-target RDP-06 CI
    -> durable evidence
    -> independent evidence-only review.

Still not authorized:

    any ninth verifier path
    any EV expected-outcome change
    any precedence semantic change
    any fixture semantic change
    U05 production source modification
    shared runtime modification
    merge
    production activation
    live downstream execution
    external delivery
    PHI / real-patient traffic
    release activation.
