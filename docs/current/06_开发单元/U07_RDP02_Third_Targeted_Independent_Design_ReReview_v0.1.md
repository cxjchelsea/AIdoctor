# U07-RDP-02 Third Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-08
> Design PR: [#271](https://github.com/cxjchelsea/AIdoctor/pull/271)
> **Exact reviewed design HEAD:** `3fe93aa2cacd2eb76e94984ae56a0967ac634ac5`
> Previous design HEAD: `e1fedc43b861c372e15e1f62fadb75a7e73941a5`
> Previous review: PR #274 (REVISE_REQUIRED)
> Integration reference: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Verdict: **PASS / CONDITIONAL DESIGN ACCEPTANCE ONLY**.
> No implementation, merge, PHI, patient, PROFILE-A or production authorization.

## 1. Exact-head comparison and method

GitHub comparison of `e1fedc43..3fe93aa2` confirms **one Markdown design document** changed, **13 lines added and 12 deleted**. The latest reviewed file has blob SHA `2b900dfe73168e7d19a6042bf91178bbdca63691`. Reviewed §§5–5.2, 8.1–8.2, 10–13, prior blockers in PRs #272–#274, and explicit retained dependency register. No runtime/database implementation or tests were run or claimed.

All **41 scenario identifiers F8-T01..F8-T41** are present, unique and in order. The third remediation also corrected the stale “27 scenarios” summary.

## 2. BF-U07-RDP02-IR-01 — Temporal linearization and normative oracles

**CLOSED AT RDP-02 DESIGN LEVEL.**

The selected V1 design retains **one business-effective linearization point**: `t_f8_linearize`, certified primary DB statement-current time obtained at the final, atomic conditional decision-write under the Consultation/U15 shared fence. This is explicitly different from physical COMMIT time; result is visible only on successful COMMIT. Deadline comparison requires a shared certified clock domain and precision; otherwise the operation fails closed as **DEFER without business verdict**.

Third-remediation changes independently checked:

| Oracle | Precisely distinguished premise | Required result |
|---|---|---|
| **F8-T28** | Original ingress T1; U15 expiry committed T2; **first final conditional SQL decision** T3 afterward | EXPIRED |
| **F8-T29** | Original ingress T1; U15 non-expiry cancellation committed T2; first final conditional decision T3 afterward | REJECTED/CANCELLED |
| **F8-T31** | Apparent wall-clock equality but **no certified same-domain statement time / owner commit ordering / timestamp precision** | DEFER / TIME_AUTHORITY_UNAVAILABLE; **not** a verified-equality case |
| **F8-T38** | Certified primary DB statement time exactly equals independently authenticated deadline, same precision and coherent fence | EXPIRED under half-open legal interval |
| **F8-T40** | Statement-current clock, precision or F3/U15 time domain unverified | DEFER / TIME_AUTHORITY_UNAVAILABLE |

This removes the former ambiguity in which physical F8 COMMIT could be incorrectly equated with `t_f8_linearize`. There are **no remaining contradicting test oracle premises among the targeted cases**. Conceptual temporal design no longer requires another round of author revision for this issue.

**Important distinction:** This PASS does **not** prove MySQL/Oracle can execute the demanded one-statement conditional write with the exact timestamp/claim predicates. The design correctly requires dialect-specific RDP-05 compatibility evidence, authoritative-clock verification and integration testing. If either dialect cannot meet the frozen semantics, U07 must remain **NOT_READY** and take a controlled amendment—not silently weaken atomicity.

## 3. BF-U07-RDP02-IR-02 — Shared Consultation/U15 fence

**CONDITIONAL_DESIGN_RESOLUTION / REQUIRED UPSTREAM PHYSICAL CAPABILITY NOT IMPLEMENTED.**

The previously reviewed choice of `clinical_consultation` `PESSIMISTIC_WRITE` and row version as the V1 shared authority, followed by the F8 same-wait claim lock and deterministic ordering, remains unchanged. The design preserves U15 lifecycle ownership and does not falsely claim U15 is physically participating.

`CA-U07-RDP02-U15-SHARED-FENCE-01` remains **REQUIRED / NOT_DESIGNED / NOT_AUTHORIZED**. Its owner-controlled detailed design, independent review, execution/verification and compatibility with the actual Consultation, F3 and U15 transaction domains must precede any positive U07 Implementation Readiness. The proposed F8 V1 design is **NOT_APPLICABLE** to a deployment unable to provide that common fence.

## 4. Regression boundaries and readiness carry-forward

| Control | Decision |
|---|---|
| Exactly four F8 verdicts; DEFER only operational | PRESERVED |
| Canonical retry preserves original immutable verdict | PRESERVED |
| ACCEPTED before APPLIED is not DUPLICATE | PRESERVED |
| Historic DUPLICATE after APPLIED means zero new effects | PRESERVED |
| Later U15 terminal fact blocks downstream effects without rewriting historical ACCEPTED | PRESERVED |
| F8 vs P02 runtime vs U02 clinical truth authority separation | PRESERVED |
| Foundation full-reference audit | REQUIRED / NOT_PASSED |
| U06 Eligibility issuance amendment | REQUIRED / NOT_AUTHORIZED / NOT_IMPLEMENTED |
| U15 shared-fence amendment | REQUIRED / NOT_AUTHORIZED |
| F8DecisionLedger / same-wait winner physical schema and two-dialect tests | DESIGN ONLY / NOT_IMPLEMENTED / NOT_RUN |
| Patient/PHI/PROFILE-A/production | BLOCKED |

## 5. Gate decision

```text
U07-RDP-02 Third Targeted Independent Design Re-Review = PASS / CONDITIONAL_DESIGN_ACCEPTANCE
Reviewed exact HEAD = 3fe93aa2cacd2eb76e94984ae56a0967ac634ac5

BF-U07-RDP02-IR-01 = CLOSED / DESIGN
BF-U07-RDP02-IR-02 = CONDITIONAL_DESIGN_RESOLUTION / U15_UPSTREAM_GAP_OPEN
B-U07-RG-02 = DESIGN_REVIEWED / IMPLEMENTATION_BLOCKED
U07-RDP-02 = CONDITIONALLY_ACCEPTED_DESIGN / PENDING_AGGREGATE_COMPATIBILITY
RDP-03 DESIGN = MAY_PROCEED_WITH_EXPLICIT_DEPENDENCIES

CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED / NOT_IMPLEMENTED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient traffic = BLOCKED
```

**Next governed design step:** U07-RDP-03 State Ownership / K09-P01 Mutation / Idempotent Apply / Trace Contract design may proceed using RDP-02 only as a conditionally accepted input. Preserve RDP-01 and RDP-02 explicit dependency blockers throughout RDP-05/aggregate readiness.

No merge, runtime change, source migration, deployment, standard/squash/rebase merge, executable verification or implementation authorization is performed by this review.
