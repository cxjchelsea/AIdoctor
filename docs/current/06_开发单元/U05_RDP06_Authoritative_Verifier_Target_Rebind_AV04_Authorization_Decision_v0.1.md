# U05 RDP-06 Authoritative Verifier Target Rebind AV-04 Authorization Decision v0.1

> Authorization ID: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002`  
> Prior authorized rebind: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001 = AUTHORIZED`  
> Reviewed amendment: PR #214  
> Reviewed amendment head: `d71e750b07ee8edd8c951aea888c6f9667e030ee`  
> Independent review: **PASS** / review_id `5285630593`  
> Proposed exact target: `2b7926afd69de9fe2224d8a5b69e91c02c2db495`  
> Status: **OWNER DECISION PENDING**

## 1. Decision question

Whether to authorize the second exact-target rebind of the already-reviewed RDP-06 verifier from:

    261ee5525c8260e93db19173ffbde89a8af6810d

to:

    2b7926afd69de9fe2224d8a5b69e91c02c2db495

after BF-U05-RDP06-AV-04 remediation.

## 2. Preconditions

Confirmed:

    PR #211
    AV-04 remediation design
    = PASS
    review_id = 5276182930

    PR #212
    AV-04 exact-head implementation
    = PASS_FOR_REBIND
    review_id = 5285624416

    PR #213
    Phase A CI MVP
    run_id = 35709800508
    = SUCCESS

    PR #214
    Target Rebind 002 Amendment
    = PASS
    review_id = 5285630593

Therefore:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002
    = ELIGIBLE_FOR_EXPLICIT_OWNER_DECISION

but remains:

    NOT_GRANTED

until the repository owner explicitly selects AUTHORIZE.

## 3. Authorized-if-approved exact target

If authorized:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002
    = AUTHORIZED

Exact target:

    U05_IMPLEMENTATION_SHA
    = 2b7926afd69de9fe2224d8a5b69e91c02c2db495

Target authority precedence:

    REBIND-002
    >
    REBIND-001
    >
    parent verifier target

for implementation-target identity only.

## 4. Exact target delta

The authorized target drift is bounded to:

1. move the synthetic readiness snapshot adapter from U05 production source to U05 test source with identical content;
2. add the reviewed AV-04 remediation design document.

Any additional target drift invalidates the authorization.

## 5. Permitted next work if authorized

Only:

    amend PR #210 verifier target binding
    to exact target 2b7926...

Update only target/provenance binding fields in:

    .github/workflows/u05-rdp06-authoritative-verification.yml
    diagnosis-service/src/test/resources/u05/verification/u05-verification-auth-profile.json
    .github/verification/u05_rdp06/build_contract_manifest.py

plus any mechanically required reviewed provenance record.

Then:

    independent exact-head overlay target-binding re-review
    -> authoritative exact-target RDP-06 CI
    -> durable evidence generation
    -> independent evidence-only review
    -> combined closure review.

## 6. Semantics that must remain unchanged

No change is authorized to:

    60 EV expected outcomes
    28 P0..P7 pairwise precedence records
    special precedence proofs
    fixture business semantics
    RDP-01..06 authority identities
    HG-001 semantics
    VG coverage
    evidence schema
    G0..G15 verification semantics
    production/live prohibition rules.

## 7. Still prohibited

This authorization does not permit:

    U05 production semantic changes
    shared runtime semantic changes
    frozen RDP amendments
    merge
    production activation
    live downstream execution
    external delivery
    model/tool side effects
    real-patient or PHI traffic
    release activation.

## 8. Owner options

    AUTHORIZE
    REVISE
    REJECT

Current:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-002
    = NOT_GRANTED

    RDP-06 authoritative verification
    = BLOCKED_PENDING_OWNER_REBIND_002_DECISION

    U05 Implementation Verification
    = NOT_PASSED
