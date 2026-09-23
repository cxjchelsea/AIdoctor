# U05 RDP-06 REBIND-002 Target-Binding Inventory Amendment v0.1

> Parent authorization: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002 = AUTHORIZED`  
> Parent authorization record: `ce83f5a2e930db08e2415012a22170df12d66b44`  
> Authorized exact implementation target: `2b7926afd69de9fe2224d8a5b69e91c02c2db495`  
> Verifier overlay under amendment: PR #210 / head `138fe7bdd59921255c4b51717d2b5fca4f81ca71`  
> Status: **PROPOSED / INDEPENDENT_REVIEW_PENDING**

## 1. Trigger

REBIND-002 correctly authorized the new implementation target after AV-04 remediation.

Its reviewed decision package identified three verifier files as carrying stale target/provenance bindings:

1. `.github/workflows/u05-rdp06-authoritative-verification.yml`
2. `diagnosis-service/src/test/resources/u05/verification/u05-verification-auth-profile.json`
3. `.github/verification/u05_rdp06/build_contract_manifest.py`

Before consuming that authorization, an exact overlay scan of all 15 reviewed PR #210 verifier paths found the prior target

    261ee5525c8260e93db19173ffbde89a8af6810d

in three additional verifier files:

4. `.github/verification/u05_rdp06/build_evidence.py`
5. `.github/verification/u05_rdp06/validate_evidence.py`
6. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05VerificationSupport.java`

Therefore the parent authorization is correct as to the target, but its permitted target-binding file inventory is incomplete.

No file may be modified outside the exact amended inventory until this amendment is independently reviewed and explicitly owner-authorized.

## 2. Nature of the three additional files

The additional occurrences are verification provenance constants, not business/clinical expected-result logic.

### build_evidence.py

The stale `TARGET` is written into top-level durable evidence:

    evidence.json.implementation_sha

Updating it is required so the artifact truthfully identifies the SUT.

### validate_evidence.py

The stale `TARGET` is used to verify:

    evidence implementation_sha
    contract authority_core implementation_sha
    per-case implementation_sha

The file also freezes:

    AUTHORITY_DIGEST

which changes mechanically because `authority_core` contains the exact implementation SHA.

### U05VerificationSupport.java

The stale:

    IMPLEMENTATION_SHA
    CONTRACT_MANIFEST_DIGEST

are emitted/checked by the verification-only observation/evidence support.

Changing these constants does not modify EV execution semantics.

## 3. New authority-core digest

Frozen canonicalization remains unchanged:

    UTF-8
    ensure_ascii = false
    sort_keys = true
    compact separators
    LF
    exactly one trailing LF

Authority core remains:

    schema = U05_CONTRACT_AUTHORITY_CORE_V0_1
    implementation_sha = exact target
    same RDP-01..06 blob identities
    same Phase 5/6/8/9 blob identities

For target:

    2b7926afd69de9fe2224d8a5b69e91c02c2db495

the mechanically recomputed authority-core digest is:

    570ca5303f601df2caf672bfb7c9e61fe2c5a5add86e288cf782c8e6030712e5

This digest must replace the prior:

    d5d272963066dccb14a6c0f124cfd5128bbacd363e84e96279800dd3b6e08ef7

only where it represents the target-bound authority core.

## 4. Exact amended verifier file inventory

If explicitly authorized, REBIND-002 target-binding consumption may change exactly these six existing verifier files:

1. `.github/workflows/u05-rdp06-authoritative-verification.yml`
2. `diagnosis-service/src/test/resources/u05/verification/u05-verification-auth-profile.json`
3. `.github/verification/u05_rdp06/build_contract_manifest.py`
4. `.github/verification/u05_rdp06/build_evidence.py`
5. `.github/verification/u05_rdp06/validate_evidence.py`
6. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/verification/U05VerificationSupport.java`

No seventh verifier path is authorized by this amendment.

## 5. Exact permitted substitutions

The bounded amendment may perform only the following categories of changes.

### Target SHA

Replace:

    261ee5525c8260e93db19173ffbde89a8af6810d

with:

    2b7926afd69de9fe2224d8a5b69e91c02c2db495

where the value denotes the implementation target.

### Rebind authorization identity

Replace active authorization identity:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001

with:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002

where the value denotes the active exact-target authorization.

Parent verifier authorization remains:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001

unchanged.

### Authorization record provenance

Replace REBIND-001 record provenance with the immutable REBIND-002 owner authorization record:

    authorization record commit
    = ce83f5a2e930db08e2415012a22170df12d66b44

    authorization record blob
    = b288077ea7e22bb03292cff36641564f0fb6eff6

    authorization record path
    = docs/current/06_开发单元/U05_RDP06_Authoritative_Verifier_Target_Rebind_AV04_Authorization_Decision_v0.1.md

### Authority-core digest

Replace target-bound authority-core digest with:

    570ca5303f601df2caf672bfb7c9e61fe2c5a5add86e288cf782c8e6030712e5

where the constant semantically represents the canonical authority core.

## 6. Static oracle consequence

The static expectation and precedence files currently embed:

    contract_manifest_digest
    = d5d272963066dccb14a6c0f124cfd5128bbacd363e84e96279800dd3b6e08ef7

Because that field is explicitly defined as the digest of the target-bound `authority_core`, the target drift makes those fields stale even though every EV expected outcome and precedence rule remains unchanged.

Therefore exact-target rebinding also mechanically requires changing only the `contract_manifest_digest` field in:

7. `diagnosis-service/src/test/resources/u05/verification/u05-verification-expectations.json`
8. `diagnosis-service/src/test/resources/u05/verification/u05-d03-precedence-expectations.json`

from the old digest to:

    570ca5303f601df2caf672bfb7c9e61fe2c5a5add86e288cf782c8e6030712e5

No case ID, expected result, expected effect count, authority ref, precedence result, fixture identity, or business semantic may change.

Because these two static files change bytes, their SHA-256 digests must be recomputed and independently re-reviewed.

The fixture file:

    u05-verification-fixtures.json

contains no target-bound authority-core digest and must remain byte-identical unless a separate finding proves otherwise.

## 7. Final amended file inventory

The exact bounded target-binding amendment therefore covers eight files:

    6 verifier implementation/provenance files
    + 2 static oracle files with digest-only substitutions

No other PR #210 path may change.

## 8. Semantic immutability guard

The amendment must prove by structured comparison that:

### expectations

For every one of 60 EV records, all fields except:

    contract_manifest_digest

are byte-equivalent after canonical JSON normalization.

### precedence

For every precedence record and special proof, all fields except:

    contract_manifest_digest

are byte-equivalent after canonical JSON normalization.

### fixtures

Entire canonical JSON must be byte-equivalent.

### Java/Python/workflow

Diff must be limited to:

    exact target constant
    target-bound authority digest
    active rebind authorization ID
    authorization record commit/blob/path

No branch logic, case mapping, expected outcome, effect-count logic, evidence schema, G0..G15 gate semantics, or hard-boundary semantics may change.

## 9. Static review consequence

The previously reviewed PR #210 static input digests:

    expectation SHA-256
    = d955969ece67c84810eb4e15f66ecc3341fe7afcd80d9747f7fe7e9ebb5da6c2

    precedence SHA-256
    = 978d6a91cbec92280588ab4d7d0316445cfe5c1e0c2dad94a96dbbfa287b1040

will become historical after the digest-only substitutions.

Therefore a new exact-head static review must freeze:

    new verifier head
    new expectation SHA-256
    new precedence SHA-256
    unchanged fixture SHA-256

before authoritative CI.

The prior static review remains historical and may not be reused as though it reviewed the new bytes.

## 10. Authorization requirement

The existing REBIND-002 owner decision did not enumerate all eight required changed paths.

Therefore this amendment does not self-authorize expansion to the eight-file target-binding inventory.

Proposed authorization ID:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002A

Owner authorization is required after independent review.

Until then:

    REBIND-002 target identity
    = AUTHORIZED

but:

    PR #210 eight-file target-binding amendment
    = NOT_AUTHORIZED

and:

    RDP-06 authoritative execution against 2b7926...
    = BLOCKED

## 11. Still prohibited

This amendment never permits:

    changes to EV expected business outcomes
    changes to P0..P7 precedence semantics
    fixture semantic changes
    new verifier paths
    src/main production changes
    shared runtime changes
    frozen RDP changes
    merge
    production activation
    live downstream execution
    external delivery
    PHI / real-patient traffic
    release activation.
