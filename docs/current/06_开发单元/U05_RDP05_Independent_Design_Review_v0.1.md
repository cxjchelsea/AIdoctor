# U05 RDP-05 Independent Design Review v0.1

Target artifact:
`U05_RDP05_Readiness_Input_Dependency_Applicability_Contract_v0.1.md`

Reviewed PR: #127  
Reviewed exact head: `7aee3246f601b3a01d8103dc507955e18369b260`

## Verdict

```text
U05 RDP-05 Independent Design Review
= REVISE_REQUIRED

BF-U05-RDP05-IR-01
= INITIAL_F3_APPLICABILITY_STATUS_IS_NOT_UNIQUE
= OPEN / BLOCKING

BF-U05-RDP05-IR-02
= POST_FACT_UPDATE_F3_STALENESS_CAN_BE_ERASED_AS_ABSENCE
= OPEN / BLOCKING

RQ-U05-RDP05-IR-03
= preserve direct BL-01 clarification path explicitly
= OPEN / REQUIRED

U05-RDP-05
= NOT_FROZEN

BF-U05-RG-05
= DESIGN_DIRECTION_ACCEPTED
= NOT_CLOSED_YET
```

## Accepted design direction

The review accepts these boundaries:

```text
U05 does not invoke C03 to manufacture F3 input
missing input != business negative
FAILED / UNAVAILABLE / STALE != READY / NOT_NEEDED
same-version binding is required
frontend/model/agent are not readiness owners
F1/F2 lawful clarification may support NEEDS_CLARIFICATION without an F3 Gap
```

## Required remediation

1. Make `ABSENT_BY_DESIGN` and `NOT_YET_APPLICABLE` mutually exclusive.
2. For initial pre-U06/C03 F3, use the uniquely defined lawful pre-activation state.
3. In `POST_USER_FACT_UPDATE`, preserve prior F3 as `STALE` until current-version reevaluation instead of allowing it to disappear as absence.
4. Explicitly preserve BL-01 entry clarification as a direct U01/U06 boundary; U05 must not become mandatory for every F1 clarification.
5. Keep `admissible input profile != proof of READY`; positive READY conditions remain for RDP-02.

No runtime, clinical-content, routing, production, or implementation authorization is granted.
