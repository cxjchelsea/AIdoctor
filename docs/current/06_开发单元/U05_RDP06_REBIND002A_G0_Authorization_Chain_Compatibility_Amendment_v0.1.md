# U05 RDP-06 REBIND-002A G0 Authorization Chain Compatibility Amendment v0.1

> Finding: `BF-U05-RDP06-RB002A-PF-01`  
> Source verifier head: `cc7e058e1feddb0d7a672a1f8da93449ea298e94`  
> Failing non-authoritative run: `35805785929`  
> Failing job: `107006200719`  
> Parent target authorization: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002 = AUTHORIZED`  
> Parent target-binding inventory authorization: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002A = AUTHORIZED`  
> Status: **PROPOSED / INDEPENDENT_REVIEW_PENDING**

## 1. Failure

REBIND-002A target/provenance rebinding correctly moved the verifier to:

    U05_IMPLEMENTATION_SHA
    = 2b7926afd69de9fe2224d8a5b69e91c02c2db495

and immutable target-authorization record:

    commit = ce83f5a2e930db08e2415012a22170df12d66b44
    blob   = b288077ea7e22bb03292cff36641564f0fb6eff6

The preflight successfully verified:

    authorization record commit
    authorization record blob
    REBIND-002 authorization id
    '= AUTHORIZED'
    exact implementation target.

It then failed on the legacy line:

    grep -F 'AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001' "$AUTH_DOC"

because the REBIND-002 record does not restate the root parent verifier authorization.

Instead its reviewed ancestry is:

    Prior authorized rebind:
    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001 = AUTHORIZED

This is the correct target-authority chain introduced by the second rebind.

## 2. Root cause

The verifier G0 immutable-record guard was originally written for the REBIND-001 record shape:

    REBIND-001
    -> root parent verifier authorization

The REBIND-002 record shape is:

    REBIND-002
    -> prior authorized REBIND-001
    -> root parent verifier authorization.

The target/provenance rebinding changed the record path and active authorization id, but did not adapt the ancestry assertion.

Therefore:

    target authorization = valid
    immutable record identity = valid
    workflow G0 ancestry assertion = stale.

This is a verification-governance compatibility defect, not an implementation/clinical defect.

## 3. Exact permitted fix

Modify only:

    .github/workflows/u05-rdp06-authoritative-verification.yml

in exactly two G0 immutable authorization-record checks:

1. engineering-preflight authorization record check;
2. authoritative-verification authorization record check.

Replace only:

    grep -F 'AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001' "$AUTH_DOC"

with:

    grep -F 'AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001' "$AUTH_DOC"

No other workflow line is authorized by this amendment.

## 4. What remains unchanged

The auth profile and evidence schema must continue to carry:

    parent_authorization_id
    = AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001

because the root verifier authorization remains the semantic parent authorization.

The workflow record guard instead validates the immediate target-authorization predecessor:

    REBIND-002
    -> REBIND-001.

Therefore:

    workflow immediate-authorization-chain check
    != evidence root parent_authorization_id field.

Both are valid and serve different provenance roles.

## 5. No semantic verifier change

This amendment changes no:

    U05 implementation target
    authority-core digest
    expectation oracle
    precedence oracle
    fixture
    EV case mapping
    expected result
    effect count
    HG-001
    VG
    evidence schema
    G1..G15 semantics
    production/live boundary.

It changes only the G0 immutable authorization-record ancestry literal in two identical checks.

## 6. Required post-fix validation

After implementation:

    exact workflow-only diff
    -> exactly two one-line replacements
    -> non-authoritative preflight rerun
    -> immutable record check PASS
    -> full preflight compile/static/focused suite PASS
    -> only then exact-head static review.

The existing REBIND-002/002A target and eight-file mechanical rebinding remain unchanged.

## 7. Authorization assessment

The currently authorized REBIND-002A scope froze the workflow change categories to target/provenance fields and explicitly prohibited control-logic drift.

Because this amendment changes which ancestry identity G0 asserts, it is treated as a control-guard semantic amendment rather than silently consumed under REBIND-002A.

Proposed authorization ID:

    AUTH-U05-RDP06-VERIFIER-G0-CHAIN-COMPAT-001

Current:

    NOT_GRANTED

Owner authorization is required after independent review.

## 8. Still prohibited

This amendment does not authorize:

    additional workflow logic
    weakening or deleting immutable authorization checks
    accepting missing authorization ancestry
    target drift
    oracle/fixture changes
    runtime/source changes
    merge
    production activation
    live downstream execution
    external delivery
    real-patient/PHI traffic.
