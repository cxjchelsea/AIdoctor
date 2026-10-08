# U07-RDP-05 Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-08
> Target design PR: [#283](https://github.com/cxjchelsea/AIdoctor/pull/283)
> **Exact reviewed HEAD:** `7083cf124845befa752c18acf17498ee3b39f69f`
> Exact design blob: `994c1c2ec824d8fd18ae1516c6bc444ef093de66`
> Previous design HEAD: `38eceebe3ad84dda37a78326e58f8b0576bbfa55`
> Prior independent review: [#284](https://github.com/cxjchelsea/AIdoctor/pull/284)
> **Verdict: PASS / CONDITIONAL_DESIGN_ACCEPTANCE ONLY**.
> Review scope: design contract, not source implementation, tests, authorization, clinical production, or merge.

## 1. Evidence and exact-head verification

Fetched PR #283 and the design file by exact commit. GitHub commit comparison from `38eceebe` to `7083cf12` shows **one Markdown file, 104 insertions / 40 deletions**. Reviewed §§4–8, 10–11, 15–16 and the prior review's three blockers in light of the full frozen RDP-01..04 ownership model. Confirmed **48 unique, consecutively numbered CAP-T01..CAP-T48 design oracles**, not executed automated tests.

Source comparison remains bounded to earlier Foundation/U06/Runtime evidence anchored at `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`. These design conclusions do not assert that latest main contains a physically complete U07 implementation.

## 2. BF-U07-RDP05-IR-01 — preconditions versus effect receipts

**CLOSED / DESIGN.** §4–5 now distinguishes the orthogonal roles:
- `CAPABILITY_PREREQUISITE`: owner adapter/contract, permission, version, profile, transaction feasibility.
- `PRIOR_STAGE_EFFECT_EVIDENCE`: completed prior-stage immutable owner result.
- `CURRENT_STAGE_EXPECTED_OUTPUT`: current effect's **post-commit** result, never the initial effect's precondition.
- `FUTURE_STAGE_DEPENDENCY`: tracked globally without premature stage gate.

`assessPre` and `verifyPost` have different outputs; §6 freezes distinct PRE/POST stage rows for first-time F8, Stage C, D, E and downstream phases. A first-ever F8 evaluation is not required to possess ACCEPTED; Stage D PRE is not required to have ACTIVE; Stage E PRE is not required to have APPLIED+Outbox. POST phase is verified from the corresponding owner receipt/readback. On replay, original effect identity must resolve an existing result instead of creating a second effect. Oracles CAP-T37–39 and CAP-T47 exercise exactly these boundaries at the design level.

**Condition remaining:** the capability evaluator and owner result oracle must be built and physically tested, with exact owner receipt provenance. This is not a design blocker once the pre/post distinction is frozen.

## 3. BF-U07-RDP05-IR-02 — U15 grant, dispatch and U02 consumption

**CLOSED / CONDITIONAL_DESIGN.** §5–6 now has `F_GRANT_PRE`, `F_GRANT_POST`, `F_DISPATCH_PRE`, `U02_ACCEPT_POST` and `U02_FACT_OWNER_LATER`. The matrix requires Stage E APPLIED+PENDING Outbox for grant evaluation; the positive U15 grant only at F_GRANT_POST; original committed grant and verifiable consumer **idempotency capability** before send; a consumer ACK/acceptance receipt only after actual delivery or exact-effect query; Clinical Fact only under later U02 authority.

This excludes requiring an impossible U02 ACK before first send or using an outbound grant as Clinical Fact evidence. CAP-T40–42 and T46 bind negative and revocation scenarios. Same-grant handoff after U15 terminal is explicitly conditional on a *separately approved* U15 irrevocable grant policy, and the consumer replay contract remains unverified.

**Physical blockers retained:** `CA-U07-RDP03-U15-DISPATCH-GRANT-01` and `CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01`, both NOT_AUTHORIZED. Producer Outbox does not prove exactly-once clinical fact formation.

## 4. BF-U07-RDP05-IR-03 — owner-by-owner authority snapshots

**CLOSED / CONDITIONAL_DESIGN.** §5 adds owner-specific `authority_version_or_epoch`, immutable owner-signed snapshot or transaction row version, authority fingerprint, `required_recheck_point`, `recheck_mechanism`, and bounded lease metadata. §6.2 maps RDP-01 admission, F8 statement linearization, P02 restore-start grant, P01 Stage C, Consultation D, Stage E APPLIED+Outbox, U15 Stage F grant, F_DISPATCH_PRE, U02 owner receipt/fact processing to specific owner authority and final recheck.

Three allowable **proposed** enforcement patterns are transactional owner CAS, version-conditional owner API and an independently approved owner signed/leased capability with enforceable revocation. An uncoordinated TTL cache or Consultation/U15 lock alone cannot protect external P06/F3/P01 permission sources. Owner revocation before the linearization must block; revocation after a committed effect must not fabricate rollback or invalidate historical facts. CAP-T43–48 cover authority races and inadequate cache evidence.

**Conditional risk, not an implemented capability:** independent owners must physically demonstrate appropriate revocation/commit serialization. A signed snapshot/lease without enforceable revocation ordering is **not** enough; the design correctly classifies unsupported mechanisms as `BLOCKED_PHYSICAL / NOT_READY`. This is an RDP-05/RDP-06 physical gate and a future controlled owner interface concern, not design acceptance for unsafe time-of-check/time-of-use.

## 5. Preservation of previously accepted boundaries

| Interface | Review conclusion |
|---|---|
| RDP-01 original USER_ANSWER identity and U06 issuance | maintained, Foundation audit / issuance still blocking |
| RDP-02 F8 business validity and U15 shared-fence authority | maintained, current/historical verdicts separate |
| RDP-03 Stage C single F3-authorized P01 commit; D owner ACTIVE; E one-commit APPLIED+outbox | maintained, no fabricated preexisting receipts |
| RDP-03 Stage F U15 dispatch grant | separate from U02 ACK or clinical fact |
| RDP-04 P02 exact restore/rehydrate, U15 restore-only start grant, parked no-execute state | preserved; designs not runtime features |
| P06 historical vs current version references | separately resolved; latest cannot replace historical source |
| P05 Trace | evidence only |
| P03/P04 for U07 F8/Runtime resume legality | NOT_APPLICABLE; U02 later dependency independent |
| PROFILE-B | synthetic, structurally governed nonproduction only |
| PROFILE-A, PHI, real patients, production | BLOCKED |

One remaining implementation caution: §8's single guard-DB topology must be proven physically (including selected MySQL/Oracle statement-time semantics); declaring a transaction-manager ID in an assessment is not a source-level proof.

## 6. Required outstanding gates and CAs

```text
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-U15-DISPATCH-GRANT-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-REHYDRATE-OWNER-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-EXECUTION-START-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-LANDING-BARRIER-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01 = REQUIRED / NOT_AUTHORIZED
```

All remain blocking to their affected runtime stages and to overall U07 Implementation Readiness. A future independent Aggregate Compatibility Review must confirm these are not silently treated as passed by this RDP-05 design acceptance.

## 7. Formal gate decision and next step

```text
U07-RDP-05 Targeted Independent Design Re-Review
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE
Exact reviewed HEAD = 7083cf124845befa752c18acf17498ee3b39f69f

BF-U07-RDP05-IR-01 = CLOSED / DESIGN
BF-U07-RDP05-IR-02 = CLOSED / CONDITIONAL_DESIGN
BF-U07-RDP05-IR-03 = CLOSED / CONDITIONAL_DESIGN

B-U07-RG-05 = DESIGN_REVIEWED / IMPLEMENTATION_BLOCKED
U07-RDP-05 = CONDITIONALLY_ACCEPTED_DESIGN / PENDING_AGGREGATE_COMPATIBILITY

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

**Next governed design step:** `U07-RDP-06 Verification / Evidence Contract` (formal design, not test execution), after which U07 Aggregate Compatibility Review and a distinct Implementation Readiness review may be considered. None of those may infer physical implementation from conditionally accepted RDPs.

No author design changes, Runtime code, schema migrations, tests, PR merge, or production/clinical authorization performed by this independent review.
