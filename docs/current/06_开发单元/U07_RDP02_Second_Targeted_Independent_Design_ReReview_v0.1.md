# U07-RDP-02 Second Targeted Independent Design Re-Review v0.1

> Date: 2026-10-08
> Design PR: [#271](https://github.com/cxjchelsea/AIdoctor/pull/271)
> **Exact reviewed head**: `e1fedc43b861c372e15e1f62fadb75a7e73941a5`
> Prior remediation head: `0346ffe7701bc441fef1c7ad48e1fcd231c5be31`
> Prior independent review: [PR #273](https://github.com/cxjchelsea/AIdoctor/pull/273)
> Main authority baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Verdict: REVISE_REQUIRED / BF-01 PARTIALLY_REMEDIATED; one remaining exact contract inconsistency; BF-02 conditional design resolution retained.**
> This is a design-only review, not independent executed DB verification, merge or implementation authorization.

## 1. Exact-head and scope evidence

Compared `0346ffe770..e1fedc43`: ahead 1, behind 0; **one Markdown file** changed, **69 additions / 33 deletions**. Re-read RDP-02 §§5, 5.1–5.2, 8.1.1–8.2, 10–13 at exact head, prior blocker definition in PR #273, and the U07 Unit Spec/previous governance boundaries. There were **no Runtime, migration or test file changes** in the reviewed delta; no tests are claimed to have run.

## 2. BF-U07-RDP02-IR-01 — time linearization

**PARTIALLY_REMEDIATED / REMAINS OPEN for one normative-vs-oracle contradiction.**

Positive observations:
- The candidate chooses exactly one **logical first-verdict effective instant**: primary DB statement-current time `t_f8_linearize` at the final conditional decision-write statement under the shared Consultation/U15 fence, **not the later wall-clock COMMIT**. That directly repairs the prior conceptual error of pretending a transaction can know its future COMMIT timestamp.
- The design requires **one atomic condition/write statement**; a separate time SELECT followed by an unconditional INSERT is explicitly forbidden.
- A successful transaction must commit before ACCEPTED becomes observable; an unknown COMMIT is reconciled by unique decision key in a new transaction.
- The contract explicitly distinguishes timely first F8 finalization from later *effect-stage* validity checks. An ACCEPTED before deadline but committed later remains historical ACCEPTED; it cannot cause a late Runtime/P01 apply if owner currentness no longer holds.
- Clock time-source/precision and MySQL/Oracle dialect capability are **NOT_VERIFIED**; fail-closed `DEFER/NOT_READY` is required if a trustworthy statement-current DB time is unavailable. This is properly left for RDP-05 and authorized DB integration tests, not represented as executed evidence.

**Remaining blocker (precise evidence):**
- §5.1 explicitly freezes **half-open** legal window `[opened_at, deadline)`: `t_f8_linearize == deadline` **MUST be EXPIRED** if deadline/clock evidence is verified. The new F8-T38 agrees.
- The *unchanged* F8-T31 still says: **“same instant deadline and first F8 commit without order proof → DEFER, no verdict.”** Its wording conflates *physical COMMIT*, which the new policy says is **not** the decision-effective instant, with the final conditional statement. It can be read as requiring DEFER on a case that §5.1/T38 definitively calls EXPIRED.
- Both cases can legitimately coexist **only** if their premises are differentiated: `T38` has a **certified statement time and deadline in the same time domain**, while `T31` is a case of **unknown clock provenance, quantization/precision or unorderable terminal owner evidence**, for which there is no verifiable `t_f8_linearize` comparison. The design does not expressly make this separation in the oracle, so the normative test set is not deterministic.

**Required limited correction:** amend F8-T31 to specify precisely what owner/time evidence is **unorderable or unavailable** (e.g. `UNVERIFIED_TIME_DOMAIN/TERMINAL_COMMIT_ORDER`), **without** asserting that bare clock equality requires DEFER. Add an explicit acceptance test for `same trusted clock, equality => EXPIRED` (T38 can serve this) and for `unknown clock/owner order => DEFER` (T40 can serve this); remove `first F8 commit` as a surrogate for `t_f8_linearize` in the F8-T28/T29 test premises. Verify the short §11/§13 summaries are consistent with 41 cases and this *second* targeted re-review, with no change to selected temporal policy.

**Other physical questions (not a new blocker at this design gate):** actual one-statement MySQL/Oracle conditional write joining the claim/winner ledger may require dialect-specific implementation/RDP-05 review; treat unsupported dialects as `NOT_READY`, not as implicit proof that SQL is already implemented. A statement-level clock function whose value is tied to transaction start fails the explicit contract.

## 3. BF-U07-RDP02-IR-02 — common F8/U15 concurrency authority

**CONDITIONAL_DESIGN_RESOLUTION / UPSTREAM DEPENDENCY STILL REQUIRED** (unchanged from PR #273).

The V1 design selects Consultation `PESSIMISTIC_WRITE` row/row_version as the shared lock, with F8WaitAnswerAuthority winner/generation, immutable decision ledger and owner terminal epoch. It does **not** claim U15 shares this lock on current main. `CA-U07-RDP02-U15-SHARED-FENCE-01` must receive its own reviewed U15-side contract and physical proof before any positive U07 implementation readiness. Absence of common lock/domain remains fail-closed `NOT_APPLICABLE/NOT_READY`.

## 4. Regression/gate carry-forward

| Contract | Result |
|---|---|
| Exactly four F8 business verdicts, DEFER is operational | PRESERVED |
| Canonical replay after ACCEPTED (before APPLIED) | PRESERVED / no false DUPLICATE |
| Same-wait identical answer already APPLIED | PRESERVED / history-only DUPLICATE |
| U15 expiry/cancel business authority | PRESERVED (shared implementation NOT proven) |
| F8 business verdict separate from P02 Runtime and U02 Clinical Facts | PRESERVED |
| RDP-01 Foundation reference audit | REQUIRED / NOT_PASSED |
| U06 Eligibility issuance CA | REQUIRED / NOT_AUTHORIZED |
| U15 common fence CA | REQUIRED / NOT_AUTHORIZED |
| MySQL/Oracle statement-current time and conditional write | DESIGN REQUIREMENT / TESTS NOT_RUN |
| Production / PROFILE-A / patient / PHI | BLOCKED |

## 5. Gate decision

```text
U07-RDP-02 Second Targeted Independent Design Re-Review = REVISE_REQUIRED
Reviewed exact head = e1fedc43b861c372e15e1f62fadb75a7e73941a5

BF-U07-RDP02-IR-01 = OPEN / CONTRACT_ORACLE_EQUALITY_CONTRADICTION
BF-U07-RDP02-IR-02 = CONDITIONAL_DESIGN_RESOLUTION / UPSTREAM_U15_FENCE_PENDING

CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED

B-U07-RG-02 = OPEN
U07-RDP-02 = DESIGN_CANDIDATE / NOT_FROZEN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

Next permitted step: **U07-RDP-02 Third Targeted Design Remediation**, limited to normalizing F8-T31/T28/T29 and the concise cross-reference summaries without reopening the selected final-statement time policy; followed by **Third Targeted Independent Design Re-Review** at amended exact HEAD.

Do not merge PR #271, grant implementation authorization or modify executable/runtime files on the authority of this document.
