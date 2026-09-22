# U05 RDP-06 Authoritative Verifier Target Rebind Authorization Decision v0.1

> Authorization ID: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001`  
> Parent authorization: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001 = AUTHORIZED`  
> Reviewed rebind amendment: PR #208  
> Reviewed semantic rebind head: `0778a57e148bfaf3adc27e86bcaf9fdb8d8a80ae`  
> Current rebind status/provenance head: `1c03b0c28251fc62531c4facd30cf4ecb1942f45`  
> Targeted Independent Re-Review: **PASS** / review_id `5275342418`  
> Status sync review: **PASS** / review_id `5275346436`  
> Proposed exact target: `261ee5525c8260e93db19173ffbde89a8af6810d`  
> Status: **AUTHORIZED / CONSUMED_FOR_EXACT_TARGET_REBIND**

---

# 1. Decision question

Whether to authorize rebinding the already-reviewed RDP-06 authoritative verifier from the historical implementation target:

    1dc49c4097841523a9445dab078bc5a3d1ad1259

to the remediated exact target:

    261ee5525c8260e93db19173ffbde89a8af6810d

with no change to verifier semantics, scope, contract authority, or production/live boundaries.

---

# 2. Preconditions

Confirmed:

    PR #205
    Targeted Remediation Design / Shared Runtime Impact
    = PASS

    PR #206
    Exact-Head Targeted Implementation Re-Review
    = PASS
    review_id = 5275319103

    PR #207
    Accepted engineering smoke
    run_id = 35700739442
    job_id = 106657871560
    conclusion = SUCCESS

    focused U05 tests
    = 30
    failures = 0
    errors = 0
    skipped = 0

    PR #208
    Verifier Exact-Target Rebind Amendment
    = PASS
    review_id = 5275342418

    BF-U05-RDP06-RB-IR-01 = CLOSED
    BF-U05-RDP06-RB-IR-02 = CLOSED

Therefore:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001
    = ELIGIBLE_FOR_EXPLICIT_OWNER_DECISION

but remains:

    NOT_GRANTED

until the repository owner explicitly chooses AUTHORIZE.

---

# 3. Exact authorized-if-approved rebind

If owner chooses AUTHORIZE:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001
    = AUTHORIZED

with exact parent:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001

and exact new target:

    U05_IMPLEMENTATION_SHA
    = 261ee5525c8260e93db19173ffbde89a8af6810d

The prior target remains historical:

    1dc49c4097841523a9445dab078bc5a3d1ad1259
    = HISTORICAL / NOT_RUN / SUPERSEDED_FOR_FUTURE_RDP06_EXECUTION.

---

# 4. Target-only override rule

If authorized, the active target identity rule is:

    this explicit owner-authorized rebind
    >
    parent verifier target field

for:

    U05_IMPLEMENTATION_SHA

only.

No other parent verifier semantic is overridden.

The following remain inherited unchanged from the parent verifier design/authorization:

    verification-only overlay topology
    permitted verifier file inventory
    60 EV identities
    U05-HG-001
    U05-VG-001..006
    D03 pairwise precedence requirement
    expectation oracle
    precedence oracle
    reviewed synthetic fixture manifest
    typed effect-count evidence
    cross-contract provenance equality evidence
    G0..G15 workflow gates
    immutable full-SHA third-party Action pins
    toolchain/workflow provenance
    >=90-day durable artifact
    evidence checksums
    independent evidence-only review
    no self-declared final PASS.

---

# 5. Exact target-delta guard

The owner-authorized rebind is valid only for the exact six-path target delta frozen in PR #208:

    3 U05 production files
    1 U05 focused test file
    2 reviewed PR #205 governance/design files.

Any further target-head change or additional path:

    invalidates this authorization
    -> STOP
    -> exact-head re-review
    -> new target rebind decision.

---

# 6. Permitted next work if authorized

The authorization permits only:

    implement the already-reviewed verification-only overlay

    bind it to:
      261ee5525c8260e93db19173ffbde89a8af6810d

    build:
      u05-verification-expectations.json
      u05-d03-precedence-expectations.json
      u05-verification-fixtures.json
      auth profile
      contract manifest
      evidence builder/validator
      focused verification suites
      exact-head authoritative workflow

    independently review:
      overlay
      expectation oracle
      precedence oracle
      fixture manifest
      workflow

    freeze reviewed digests

    then execute:
      authoritative RDP-06 exact-target CI.

A green workflow may only emit:

    PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW

until independent artifact review passes.

---

# 7. Still prohibited

This rebind authorization does not permit:

    modifying the U05 implementation candidate
    modifying diagnosis-service/src/main/** for verifier needs
    shared-runtime semantic modification
    Shared Contracts modification
    production/default activation
    live U04 -> U05 cutover
    live U06/U08/U10/U11/U14 execution
    production Clinical State mutation
    external delivery
    external model/tool side effects
    real-patient/PHI verification data
    merge of PR #206
    merge of verifier branches
    release activation
    production authorization.

---

# 8. Owner options

Allowed explicit decisions:

    AUTHORIZE
    REVISE
    REJECT

## AUTHORIZE

Sets:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001
    = AUTHORIZED

for exact target:

    261ee5525c8260e93db19173ffbde89a8af6810d

and allows the reviewed verification-only overlay implementation sequence to resume.

## REVISE

No rebind is granted.

The decision package must be revised and independently re-reviewed.

## REJECT

No rebind is granted.

The old parent authorization remains historical for its prior exact target only; authoritative RDP-06 execution against the remediated target remains blocked.

---

# 9. Current state

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001
    = AUTHORIZED_FOR_HISTORICAL_TARGET_ONLY

    historical target
    = 1dc49c4097841523a9445dab078bc5a3d1ad1259
    = NOT_RUN

    proposed rebound target
    = 261ee5525c8260e93db19173ffbde89a8af6810d

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001
    = AUTHORIZED

    RDP-06 authoritative verification
    = NOT_RUN / AUTHORIZED_TO_RESUME_VERIFIER_OVERLAY_SEQUENCE

    U05 Implementation Verification
    = NOT_PASSED


---

# 10. Owner Authorization Record

Repository owner explicitly selected:

    AUTHORIZE

against exact decision-package head:

    d4e6f10bc7527219e88473bce3da97f545f2f0ed

Therefore:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001
    = AUTHORIZED

Exact authorized implementation target:

    261ee5525c8260e93db19173ffbde89a8af6810d

Parent authorization:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001

Authorized continuation:

    implement the already-reviewed verification-only overlay
    -> independent overlay/oracle/fixture/workflow review
    -> authoritative exact-target RDP-06 CI
    -> durable evidence generation
    -> independent evidence-only review
    -> combined closure review.

Still not authorized:

    merge
    production activation
    live downstream execution
    external delivery
    release activation
    real-patient traffic.

Any change to:

    implementation target SHA
    verifier scope
    authority identities
    oracle/fixture digests
    workflow semantics

invalidates this authorization and requires a new review/authorization decision.


---

# 10. Repository Owner Decision

The repository owner explicitly selected:

    AUTHORIZE

against the independently reviewed decision package at:

    d4e6f10bc7527219e88473bce3da97f545f2f0ed

Therefore:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001
    = AUTHORIZED

Exact active implementation target:

    261ee5525c8260e93db19173ffbde89a8af6810d

Parent verifier authorization:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001

Target-only precedence:

    this owner-authorized rebind
    >
    historical parent target field

for U05_IMPLEMENTATION_SHA only.

Authorization consumption boundary:

    verifier overlay implementation
    independent exact-head overlay/oracle/fixture/workflow review
    authoritative exact-target CI
    durable evidence generation
    independent evidence-only review

Still not authorized:

    modification of U05 production implementation
    modification of shared production runtime
    merge
    production activation
    live downstream execution
    external delivery
    real-patient / PHI traffic
    release activation.
