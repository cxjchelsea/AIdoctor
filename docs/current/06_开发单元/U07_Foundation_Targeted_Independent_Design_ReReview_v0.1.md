# U07 Foundation Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-08
> Original author design PR [#294](https://github.com/cxjchelsea/AIdoctor/pull/294)
> Prior independent design review PR [#295](https://github.com/cxjchelsea/AIdoctor/pull/295), verdict REVISE_REQUIRED
> **Exact reviewed amended HEAD:** `e652645d3d98b8f2e302b13e43ba55fcc3e478c1`
> **Exact design blob:** `458c939b1c0e92c1a858c6edf182ae8dda0b3fdb`
> Physical source comparison baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **VERDICT: PASS / CONDITIONAL_DESIGN_ACCEPTANCE ONLY.**
> Independent review artifact only; no executable code, DB schema migration, tests, clinical activity, authorization or merge.

## 1. Independent exact-head scope and diff

Independently fetched the amended PR #294 exact HEAD and its complete source-inventory/transaction design, then compared it to the previous reviewed design SHA `f7eb2a2662596aff009874fff62f7cb74e5586fe`. The diff changes **one Markdown design file, +92/-35 lines**. The `FOUND-T01..22` numbered scenario requirements remain unique and consecutive; none is an implemented or executed test.

Review specifically focuses on the two findings in PR #295:
- `BF-U07-FOUND-DIR-01`: no unique physical *outermost* commit authority; `@Transactional(REQUIRED)` or default `TransactionTemplate` could join an ambient caller transaction and generate premature F8 success.
- `BF-U07-FOUND-DIR-02`: overly broad “committed Foundation Event without U07 binding => orphan” might incorrectly quarantine ordinary U01 `START_CONSULTATION` events.

The assessment is against accepted RDP-01 `SINGLE_DB_INLINE_SYNTHETIC_V1`, current Foundation/U01 source facts already separately audited in PR #293, and the later U07 aggregate design. It **does not claim all repository consumers were exhaustively scanned**.

## 2. BF-U07-FOUND-DIR-01 — outermost commit authority

**CLOSED / CONDITIONAL_DESIGN.**

The amended §3 explicitly selects **one**, not optional, write protocol:
- A trusted non-transactional `U07AdmissionApplicationService` is the only allowed U07 admission entry; `TransactionSynchronizationManager.isActualTransactionActive() == true` at ingress => `BLOCKED_AMBIENT_TRANSACTION` **before any U07 writes**. It does not join/suspend U01's `@Transactional(REQUIRED)` chain.
- It invokes a **pinned** `PlatformTransactionManager` via explicit `TransactionTemplate(PROPAGATION_REQUIRES_NEW)`, whose real `DataSource` and EntityManager must be the same for Foundation canonical and U07 Binding rows. The Foundation `@Transactional(REQUIRED)` Ledger joins this **owned write transaction**.
- The callback returns **only an internal attempt result**, never `ADMITTED_FOR_F8`. Returning from `TransactionTemplate.execute()` must represent a completed physical owned COMMIT; then a **separate proxied `REQUIRES_NEW` read** verifies event identity, binding, owner scope and the exact commit provenance.
- §3.2.1 freezes T0–T5/T3-F/T5-R/T5-U timelines and distinguishes `NEW_COMMITTED_ORIGINAL_EVENT_AND_BINDING` from `REATTACHED_TO_PREEXISTING_COMMITTED_WINNER`. Only the former, with the first-committed origin proof, can initiate **first F8**. Reattachment reuses prior business decision, not a second F8.
- Rollback-only, uniqueness exceptions, deferred FLUSH/COMMIT failure and `UnexpectedRollbackException` never permit positive commit assertions. `UNKNOWN COMMIT` or failed post-commit readback => `RECONCILIATION_REQUIRED` unless a trusted later original-root reconciliation proves a specific committed winner.

**Design-only dependencies (not waived):** prove selected manager/EntityManager/transaction proxy configuration in physical source; implement an authoritative first-insert/commit-origin witness and correct after-failure readback; test true ambient transaction refusal, concurrent winner, losing attempt, rollback-only, COMMIT_UNKNOWN and postcommit failure in both dialects. A synthetic first-insert marker by itself is not an owner-verified COMMIT fact. The design explicitly marks source-level manager identity and tests as future gates, so this conditional finding may close without claiming those tests passed.

## 3. BF-U07-FOUND-DIR-02 — owner-scoped orphan classification

**CLOSED / DESIGN.**

The amended §3.4 explicitly freezes:
```text
U07_APPLICABLE_EVENT_TYPES = {USER_ANSWER, RESUME_REQUEST}
```
plus the RDP-01 trusted globally scoped `u07-` storage idempotency key and independent owner/event provenance. It now mandates checking existing `event_id` first, rejecting a U07 request whose ID points to U01 `START_CONSULTATION` with `IDENTITY_CONFLICT_NON_U07_OWNER`, then checking U07 key/original-vs-alias mapping, consultation, scope and Foundation payload digest **before** checking for U07 Binding. The key prefix alone is insufficient as authorization proof.

An ordinary U01 `START_CONSULTATION` row lacking U07 Binding = `NORMAL_FOR_U01`. Only a **previously durably committed, verified U07-owned** Foundation row with an absent mandatory U07 Binding qualifies for `QUARANTINED_LEGACY_U07_ORPHAN`. An uncommitted first U07 transaction which rolls back cannot produce a committed orphan. The candidate V7 foreign key is one-directional and does not require every Foundation row to have a U07 Binding.

The revised §6 includes explicit, still-unexecuted:
- `FOUND-T10` committed pre-seeded U07-only orphan;
- `FOUND-T11` valid U07 key alias to original U07 winner;
- `FOUND-T12` Event ID conflict including U01;
- `FOUND-T17` valid U01 `START_CONSULTATION` without Binding stays normal;
- `FOUND-T18` U07 alias pointing at U01 event ID is a non-U07 owner conflict.

This cures the originally documented scope ambiguity without redefining Foundation's global event or U01's Run semantics.

## 4. Preserved architecture and open audit requirements

The following are **not** closed by a targeted design-only re-review:

| Outstanding item | State | Evidence needed |
|---|---|---|
| `BF-U07-FOUND-AUD-01` | OPEN | real Spring/Hibernate rollback-only/collision behavior and protected U01 impact; independently reviewed safety remediation as needed |
| `BF-U07-FOUND-AUD-02` | OPEN | authorized U07 atomic Foundation+Binding source, physical outer COMMIT, matching owner readback |
| `BF-U07-FOUND-AUD-03` | OPEN | reproducible all-tracked-source `git grep`/manifest, full consumer/transaction configuration mapping and independent completeness review |
| `BF-U07-FOUND-AUD-04` | OPEN | MySQL/Oracle V7 migrations and genuine concurrency/rollback/commit UNKNOWN/legacy-orphan test evidence |
| `CA-U07-FOUNDATION-COLLISION-TX-SAFETY-01` | PROPOSED_IF_NEEDED / NOT_AUTHORIZED | separate Foundation/U01 owner-controlled amendment before changing shared Ledger or RuntimeBindingService |
| `CA-U06-U07-ELIG-ISSUANCE-01` | REQUIRED / NOT_AUTHORIZED | U06 durable original eligibility issuance and historical owner provenance |
| U07 physical U01/U06 consumer compatibility | NOT_VERIFIED | old U01 START_CONSULTATION/replay, direct Run reads, exact source manager and release/profile checks |

The `FOUND-T01..22` catalog is a **test design only**. Its length and existence are not evidence that U01 regression tests or DB dialect tests ran. The source audit PR #293 remains accurate and unclosed; the targeted design review does not retroactively turn GitHub's incomplete code search into a complete audit.

## 5. Formal targeted independent review decision

```text
U07 Foundation Targeted Independent Design Re-Review
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

Exact reviewed design HEAD
= e652645d3d98b8f2e302b13e43ba55fcc3e478c1

BF-U07-FOUND-DIR-01
= CLOSED / CONDITIONAL_DESIGN
BF-U07-FOUND-DIR-02
= CLOSED / DESIGN

BF-U07-FOUND-AUD-01..04 = OPEN / NOT_CLOSED

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01
= NOT_PASSED

U07 Aggregate Compatibility
= CONDITIONALLY_ACCEPTED_DESIGN
U07 Implementation Readiness
= NOT_READY
U07 Implementation Authorization
= NOT_GRANTED

Production / PROFILE-A / PHI / real-patient
= BLOCKED
```

**Recommended next step:** `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 — Exhaustive Exact-Head Source Inventory / Consumer Compatibility Evidence`. Carry out the tracked-source scan and the independent consumer/proxy/DataSource analysis, with concrete files/line numbers and completeness evidence, before reopening Foundation audit gate closure. Any future shared Foundation code hardening remains separately authorized. This review makes **no** runtime changes and issues **no** implementation or merge approval.
