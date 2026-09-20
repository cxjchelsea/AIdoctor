# U05 A1 Detailed Controlled Amendment Independent Design Review v0.1

Target PR: #138
Reviewed exact head: 0bacd8e99edafad1e58d358dfcf65013c9eedf1c

## Verdict

    U05 A1 Detailed Controlled Amendment
    Independent Design Review
    = REVISE_REQUIRED

    BF-U05-A1-IR-01
    = BARRIER_EXIT_CURRENT_RISK_REQUIREMENT_IS_OVERSTRICT_AND_CAN_SELF_INVALIDATE
    = OPEN / BLOCKING

    BF-U05-A1-IR-02
    = F3_CURRENT_VERSION_REVALIDATION_OWNER_AND_EXECUTION_CONTRACT_UNDEFINED
    = OPEN / BLOCKING

    BF-U05-A1-IR-03
    = RDP02_AMENDMENT_MUST_NOT_INVENT_D03_ADMISSION_STATUS
    = OPEN / BLOCKING

    RQ-U05-A1-IR-04
    = PRE_READINESS_C03_QUESTION_CANDIDATE_LIFETIME_UNDERSPECIFIED
    = OPEN / REQUIRED

    A1 Detailed Amendment
    = NOT_READY_FOR_FROZEN_AMENDMENT_AUTHORIZATION

    Frozen artifact amendment = NOT_AUTHORIZED
    Runtime implementation = NOT_AUTHORIZED
    OD-U05-READY-01 = SEPARATE / NOT_APPROVED

## Accepted portions

- A1 selected architecture is preserved.
- U06 PRE_READINESS mode is separated from Question Delivery.
- PRE_READINESS cannot enter WAITING_USER.
- C03 remains Capability, not Clinical Truth Owner.
- F3 remains Gap semantic Owner.
- U05/D03 remains unique Clinical Readiness Resolver.
- U04 old Gate/authorization becomes stale after F3 state commit.
- Exact artifact inventory includes Phase 4 plus the eight minimum requested artifacts.

## Required remediation

1. Replace barrier exit's literal 'current Risk + current Gate + current F3 on one final version' with dependency-validity semantics that do not create U03/U04 self-looping.
2. Define F3 current-version revalidation as an explicit F3-owned governed decision/consequence, including execution host, trigger, contract, idempotency, failure and binding-version rules.
3. Rewrite RDP-02 amendment so A1 bootstrap incomplete means U05/D03 is not invoked and no D03 decision object/status exists; do not add NOT_ADMITTED/NOT_REACHED to D03 vocabulary.
4. Freeze a deterministic V1 rule for pre-readiness C03 question-candidate lifetime/reuse.

Next step:

    remediate findings
    -> A1 Targeted Independent Design Re-Review