# U07-RDP-03 Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-08
> Target design PR: [#276](https://github.com/cxjchelsea/AIdoctor/pull/276)
> **Reviewed exact HEAD:** `fc961a778fb1099c2e6c37540fa47f374182b67b`
> Previous head: `253032f718d85a0bbbb073ccbaee0d04adf961e7`
> Previous independent review: [PR #277](https://github.com/cxjchelsea/AIdoctor/pull/277)
> Status: **REVISE_REQUIRED / 1 CONDITIONAL RESOLUTION / 1 DESIGN RESOLUTION WITH NEW BLOCKER / 1 OPEN DESIGN BLOCKER**.
> Design-only; not runtime evidence, merge or authorization.

## 1. Exact-head evidence

GitHub exact-head comparison confirms **one Markdown file**, **148 additions and 67 deletions** since the reviewed original. Reviewed §§6–10, §12, §§13–15 and original PR #277 blockers; 38 unique sequential `U07-A03-01..38` design oracles. Source evidence still only demonstrates existing Consultation row lock/row version, U06 synthetic StateCommitter pattern and generic effect ledger; it does not demonstrate a cross-P01/U15 transactional resource or U02 consumer idempotency. No executable tests were run.

## 2. BF-U07-RDP03-IR-01 — chosen topology

**CONDITIONAL DESIGN RESOLUTION / PHYSICAL CAPABILITY BLOCKED.**

The author selected exactly one `SINGLE_GUARD_DB_STAGED_SAGA_V1` instead of previous “single transaction or unspecified saga.” §9.1 binds A/F8 claim, B/P02 request/recovery, C single F3-owner-approved P01 mutation, D Consultation transition, E atomic journal/outbox and F independently idempotent U02 transport. All clinical mutating stages require same primary DB/transaction manager and shared Consultation/U15 guard; if not physically possible, this V1 is explicitly `NOT_APPLICABLE / NOT_READY`. This satisfies the original **choice-of-topology** concern **conditionally**; production implementation or dialect feasibility are not proven.

**Remaining contract mismatch tied to this topology — BF-U07-RDP03-TR-01 (NEW BLOCKER).** §9.1 and §10.2 explicitly freeze stage C as **one atomic P01 StatePatch** updating F3 Question/Gap and Pending Question. But §6 still defines two durable-looking sequential statuses:
```text
F3_OWNER_EFFECT_COMMITTED
  → PENDING_CONSUMED_COMMITTED
```
If both are durable independently, they violate the selected atomic stage C guarantee; if they are only logical sub-evidence within one atomic commit, that distinction is missing. This can lead recovery/replay to assume Question committed but Pending still pending, then create a second P01 write and Clinical Version advance.

**Required remediation:** replace both with one terminal stage `F3_P01_ANSWER_AND_PENDING_COMMITTED` (or explicitly declare two non-durable subfields derived from **the same** CommitResult and one Clinical version), freeze single-commit `CommitResult` provenance identity and readback, update §6 and the crash case `U07-A03-16`, and define an oracle where only one subfield appears in the journal due to a delayed receipt but the underlying P01 commit was atomic. Reconcile from one authoritative P01 receipt, never issue another patch.

## 3. BF-U07-RDP03-IR-02 — APPLIED/outbox durability

**DESIGN RESOLVED / CONDITIONAL**.

§10.2 stage E now specifies **one physical same-DB transaction** that commits `APPLIED_WITH_DURABLE_U02_INTENT` and a uniquely keyed `U07U02HandoffOutboxV1` PENDING row. No APPLIED outside a matching durable outbox; failure on either side rolls back both. Uncertain COMMIT requires independent journal+outbox readback by stable ID; remote U02 acceptance/retry must be separately verified. Cases U07-A03-20/21/33–36 cover rollback/ACK-loss conceptually. The former either/or of atomicity is removed.

**Residual safety ambiguity — BF-U07-RDP03-TR-02 (NEW BLOCKER).** §10.3 says “if D commits then U15 terminalizes, E/F must not claim normal business delivery without separately reviewed terminal currentness”, and §10.5 says no new state mutation after U15. But §10.2 F says the worker may claim PENDING and deliver to U02 based on outbox CAS and consumer idempotency, without an explicit **at-dispatch U15 authorization/fence**. Consider: E atomically commits APPLIED + outbox PENDING; U15 cancels the Consultation before F dispatch; worker later sends the answer to U02. The current F protocol admits delivery merely because outbox exists, contradicting §10.3's owner currentness requirement.

**Required remediation:** freeze a precise F dispatch policy for post-E U15 changes: either (a) durable snapshot authority that explicitly grants dispatch after APPLIED despite later terminalization, subject to U15 policy, **or** (b) current U15/Consultation fence check before outbox CLAIMED/dispatch, with `BLOCKED_TERMINAL/RECONCILE_REQUIRED` durable outbox status and no transmission. Choose exactly one policy, identify lock/authorization owner and atomic claim rule (note network send cannot be in a DB transaction), define race between claim and send, and add positive/negative U02-not-delivered or authorized-delivered oracles. Preserve U02's independent clinical truth ownership.

## 4. BF-U07-RDP03-IR-03 — terminal fence for C/D/E

**CONDITIONAL DESIGN RESOLUTION / UPSTREAM PHYSICAL BLOCKER OPEN.**

§10.3 has concrete Consultation `PESSIMISTIC_WRITE`, row_version and monotonic U15 generation, with P01 stage C actually inside same physical transaction, D owner transition, and E APPLIED/outbox; statement-current certified DB time prevents deadline passage without an U15 writer. U15 first blocks later C/D/E; partially committed C/D facts remain authoritative and are routed for owner reconciliation. This is a defensible **conditional design**, but main does not prove U15/P01/Consultation colocation, required common lock use, MySQL/Oracle atomic predicate implementation or current U15 owner implementation.

The explicit `CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01` remains REQUIRED / NOT_AUTHORIZED / NOT_IMPLEMENTED. Upstream `CA-U07-RDP02-U15-SHARED-FENCE-01` also remains pending.

## 5. Other findings and register consistency

**Required, non-blocking report hygiene:** §12 registers `CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01`, but §15's formal summary omits it. Add it explicitly at `REQUIRED / NOT_AUTHORIZED`; do not imply RDP-03 closes the dependency. §6 and §10 staging terminology must be made identical.

Preserved logical boundaries: F3 owner of Question/Gap, P01/G2 authority, F8 immutable verdict, P02 runtime, stable root/same-wait effect ID, owner readback/trace, PHI/PROFILE-A blocked. No new F3 owner transition implementation may be invented here.

## 6. Final exact-head gate

```text
U07-RDP-03 Targeted Independent Design Re-Review = REVISE_REQUIRED
Exact head = fc961a778fb1099c2e6c37540fa47f374182b67b

BF-U07-RDP03-IR-01 = CONDITIONAL_DESIGN_RESOLUTION / CROSS_RESOURCE_PHYSICAL_PROOF_PENDING
BF-U07-RDP03-IR-02 = CONDITIONAL_DESIGN_RESOLUTION / NEW_F_DISPATCH_GAP
BF-U07-RDP03-IR-03 = CONDITIONAL_DESIGN_RESOLUTION / U15_P01_PHYSICAL_FENCE_PENDING

BF-U07-RDP03-TR-01 = OPEN / ATOMIC_C_STAGE_STATE_MACHINE_MISMATCH
BF-U07-RDP03-TR-02 = OPEN / POST_APPLIED_U15_DISPATCH_AUTHORITY

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 = REQUIRED / NOT_AUTHORIZED

B-U07-RG-03 = OPEN
U07-RDP-03 = NOT_FROZEN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real patient = BLOCKED
```

Next: **U07-RDP-03 Second Targeted Design Remediation**, limited to the atomic stage-C status/replay semantics and post-APPLIED U15-governed outbox dispatch rule, plus §15 register consistency. Then exact-head Second Targeted Independent Design Re-Review.

No implementation, tests, merge, clinical state mutation, PHI/patient data or production operation performed.
