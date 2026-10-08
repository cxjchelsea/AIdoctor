# U07-RDP-02 Independent Design Review v0.1

> Date: 2026-10-08
> Target design PR: [#271](https://github.com/cxjchelsea/AIdoctor/pull/271)
> **Exact reviewed head:** `8ce3c2ca140674c2e723b4c40e8a37ed25371216`
> Parent conditional RDP-01 review: PR #270 @ `d43ac7fa1b6d65e8db27a5a316ee650f8137e4e2`
> Runtime integration baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Verdict: REVISE_REQUIRED / 2 BLOCKERS; design-only review.**

## 1. Evidence / scope

Examined the full `docs/current/06_开发单元/U07_RDP02_F8_Business_Resume_Decision_Precedence_Contract_v0.1.md` at the exact reviewed commit, U07 Unit Spec §9–12/§16–20, RDP-01 conditional design/third independent review, and Phase 9 frozen business-vs-runtime separation. PR #271 changes **one design document**; no F8 executable implementation is asserted or verified.

Review verifies deterministic precedence, event identity replay, historical duplicate, expiry/cancel races, durable decision/winner authority, and downstream P01/P02/F3/U02 owner boundaries.

## 2. Accepted design properties

- F8 has exactly four mutually exclusive **business verdicts**: `ACCEPTED / DUPLICATE / EXPIRED / REJECTED`. `DEFER` is explicitly *not* a fifth verdict.
- Same canonical event/alias replay first retrieves the original immutable decision. `ACCEPTED` before `APPLIED` is **not** recast to DUPLICATE; after APPLIED no second effect/handoff is permitted.
- A new independently canonical answer with a proved identical already-APPLIED answer on the **same Question and parent wait** can be DUPLICATE; a different answer is REJECTED/competing.
- RDP-01 rejects malformed/unauthorized/identity-conflicting ingress; F8 cannot reinterpret it as a business REJECTED.
- F8 ACCEPTED is separate from P02 checkpoint repair, Runtime result and U02 clinical fact creation. U15 cancellation/expiry cannot be overwritten by Runtime.
- Decision ledger vs Foundation canonical event ledger are separate and compatible at the *logical design* level. 27 scenario oracles are a **plan**, not test evidence.
- RDP-01 Foundation full-source audit and U06 Eligibility issuance controlled amendment remain blocking implementation dependencies.

## 3. BF-U07-RDP02-IR-01 — Expiry / cancellation precedence lacks a unique temporal decision model

**Severity: BLOCKER / SEMANTIC_PRECEDENCE_AMBIGUITY.**

§5 P2 only decides EXPIRED where authoritative expiry occurred **before trusted first ingress**. §5 P7 requires the Consultation to be *currently* WAITING_USER and Question current. Consider:
1. Wait is legal and unexpired at trusted receipt `T1`.
2. No F8 verdict commits yet; U15 expires or cancels the wait at `T2`.
3. F8 obtains current owner snapshot and evaluates at `T3 > T2`.

Under the current P2 wording, expiry did not occur **before** ingress, so P2 does not match; P3 (cancel) or P4 (no longer WAITING_USER) may produce **REJECTED**, although U15 has an authoritative expiry, or the design might regard ingress-before-expiry as a lawful ACCEPTED pending effect. The frozen table therefore does not give one stable result for the T1/T2/T3 race. It also conflates **eligibility at first trusted receipt**, **authority at first F8 decision commit**, and **permission to apply after decision**.

**Required remediation:**
- Define an exact event-time/owner-commit ordering function using server receipt, authoritative effective expiry/deadline, Consultation/U15 commit version and any same-wait fencing; explicitly decide `T1 < T2 < T3` expiry and cancel outcomes. Distinguish timestamps from causal commit order; unorderable cases must DEFER.
- Freeze distinct logic for **first F8 verdict** and **post-ACCEPTED downstream apply barrier**. Define whether a pre-expiry arrival with expiry before first decision is EXPIRED, ACCEPTED-but-not-actionable, or governed deferred reconciliation, and ensure the ordering is consistent with the frozen late-answer policy and no unlawful effect.
- Add counterexamples to the precedence table and RDP-06 scenarios: receipt-before-expiry but F8-after-expiry, cancellation between receipt and F8, event-versus-expiry same instant with differing commit order, prior ACCEPTED before subsequent U15 expiry, and already APPLIED historical duplicate after U15.
- Keep U15 authoritative, and never infer effective deadline merely from untrusted `occurred_at`.

**Review evidence:** §5 “P2”, “P3”, “P7”, “Expiry clock rule”; §6 expiry/cancel matrix; `F8-T09/T16/T25/T26`.

## 4. BF-U07-RDP02-IR-02 — F8 winner claim / U15 fencing physical owner boundary unresolved

**Severity: BLOCKER / CONCURRENCY_AND_TRANSACTION_AUTHORITY.**

§8.1 proposes an `F8WaitAnswerAuthority` keyed by wait and a distinct `F8DecisionLedgerV1`, and requires their atomic commit. §8.2 says F8 must re-read U15 and Consultation before making a winner claim, but it has not yet frozen the exact physical **common serialization or epoch/fence authority** that prevents a U15 terminalization concurrent with the F8 claim. A lock on only the new F8 table would not prevent the U15 state owner from terminalizing Consultation/Question in a different transaction. “Row must be serialized/locked ... **or** equivalent CAS” leaves implementation options rather than a chosen enforceable design.

**Required remediation:**
- Freeze **one** non-invasive authority bridge for synthetic V1: the owner of the shared wait/Consultation version and whether it is locked/read in the same database transaction as the F8 decision claim, with explicit lock ordering. Identify exactly how U15 terminalization participates (same row/CAS generation, or an independently approved cross-owner barrier).
- Give an explicit `consultation_version`, `current_wait_effect_id`, `u15_terminal_version` and `f8_claim_generation` fenced decision context; prohibit F8 ACCEPTED if the shared terminal barrier advances before claim commit. If existing U15 integration is not ready, label the missing owner bridge as **a blocking upstream physical dependency**, not a presumed runtime capability.
- Clarify what happens if the owner state resides in a separate data source: **NOT_APPLICABLE / NOT_READY** until a reviewed controlled amendment; do not claim “same relational DB” without provider evidence.
- Define recovery for claim/decision commit success, crash before response, and claim held by an `ACCEPTED` event whose downstream P02 never completes. No accidental second winner; no F8 unilateral release after Timeout.
- Add deterministic race/fencing oracle cases, including (a) U15 wins first, (b) F8 wins first then U15, (c) both read the same old version, and (d) DB transaction rollback-only after unique conflict.

**Review evidence:** §8.1 “F8WaitAnswerAuthority” and “or ... equivalent CAS”; §8.2 two-phase protocol; §5 P5/P7 and crash scenarios.

## 5. Finding disposition against review questions

| Review question | Result |
|---|---|
| IR-01 historical duplicate before expiry | PASS with historical-only zero-new-effect boundary; first-verdict expiry ordering has BF-01 |
| IR-02 duplicate equivalence | PASS as deterministic same-wait content digest **design candidate**; exact digest canonicalization/test vectors belong to RDP-06 |
| IR-03 decision + winner atomicity | **BLOCKED** by BF-02 |
| IR-04 operational DEFER | PASS, not a fifth business verdict |
| IR-05 trusted timestamp / expiry | **BLOCKED** by BF-01 |
| IR-06 RESUME_REQUEST reattach | PASS, no new answer verdict |
| IR-07 upstream prerequisites | PASS at design/gate registration level; not implementation proof |

## 6. Final gate statement

```text
U07-RDP-02 Independent Design Review = REVISE_REQUIRED
Reviewed exact head = 8ce3c2ca140674c2e723b4c40e8a37ed25371216
BF-U07-RDP02-IR-01 = OPEN / TEMPORAL_PRECEDENCE
BF-U07-RDP02-IR-02 = OPEN / F8_U15_FENCING_AUTHORITY
B-U07-RG-02 = OPEN / NOT_CLOSED
U07-RDP-02 = DESIGN_CANDIDATE / NOT_FROZEN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / real patient / PHI = BLOCKED
```

Next: **U07-RDP-02 Targeted Design Remediation** on the original design PR #271, limited to BF-01 and BF-02; then **U07-RDP-02 Targeted Independent Design Re-Review** on the amended exact HEAD.

No source code, migration, merge, squash, rebase, production activation or implementation authorization is carried out by this independent review.
