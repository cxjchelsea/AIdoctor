# U07-RDP-01 Independent Design Review v0.1

> Review date: 2026-10-08
> Reviewed PR: [#266](https://github.com/cxjchelsea/AIdoctor/pull/266)
> **Exact reviewed head:** `0a8f75f54a148ebd17243ebc1db529d9ce4d9d6e`
> Parent review head: `0728bbcf50e8a8103697f5b1e88f845d6a1caa29`
> Runtime authority baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Scope: documentation-only independent design review.
> **Verdict: REVISE_REQUIRED / 3 BLOCKERS.**

## 1. Source and boundary review

Sources inspected:
1. `U07_RDP01_Consumer_Inbound_Event_Admission_Contract_v0.1.md` at exact reviewed head.
2. `U07_Unit_Spec_v0.1.md` and prior Unit Spec independent review.
3. `docs/current/08_契约与数据/Contract与数据语义设计.md`, K01/K02/K10.
4. `docs/current/09_Runtime与技术架构/Runtime与技术架构设计_V1.md`, especially RUNTIME-INV-06..12 and §10.
5. `CanonicalBusinessEventLedger.java`, `CanonicalBusinessEventRecord.java`, `CanonicalBusinessEventRepository.java`.
6. `U07ResumeEligibilityProjector.java`, `RuntimeWaitCheckpointRecord.java`, `RuntimeThreadStateRecord.java`.
7. `U06_RDP04_Delivery_Downstream_SideEffect_Boundary_v0.1.md`.

The proposal correctly separates canonical admission from F8 business validity and P02 resume compatibility; keeps F3/U02 authority; prohibits production/live side effects; and includes a useful 20-case design test inventory.

The following are **design blockers**, not confirmed implementation defects.

## 2. BF-U07-RDP01-IR-01 — Alias event ID conflicts with full immutable binding fingerprint

**Severity:** BLOCKER / CONTRACT_CONTRADICTION.

RDP-01 §5 replay matrix / T03 requires **new business_event_id + same idempotency_key + same business binding** to reattach to the same canonical event. The existing Foundation `CanonicalBusinessEventLedger.resolveOrCreate` explicitly returns the winner found by `idempotency_key` even when the passed `eventId` differs, provided its `sameCanonicalInput` agrees.

But RDP-01 §§4.2/5 describes the immutable `U07CanonicalEventBinding` as containing `business_event_id` and says `payload_digest = canonical_binding_fingerprint` over the complete protected binding. It never expressly excludes the incoming alias `business_event_id` from fingerprint and side-binding equality. If included, the same-key new-ID replay changes the fingerprint and fails `sameCanonicalInput`, contradicting T03; if silently excluded, the side-binding's event ID may still be compared against the new request and falsely conflict.

**Required remediation:**
- Freeze the distinction between **original canonical_event_id**, **incoming transport/request alias ID**, and **immutable business payload binding**.
- Enumerate *exactly* every fingerprint input (and excluded retry/alias fields); specify original business_event_id vs alias identity semantics and when an alias is durable.
- Ensure `payload_digest` to Foundation is stable for a permitted same-key alias; require comparison against immutable original canonical binding, not request alias.
- Add two inverse tests: same key + new alias + same protected binding => one winner; same key + new alias + changed Question/scope/answer => conflict.

**Closure:** deterministic byte-level identity table, side-record field rules and a PASS independent test-vector review at design level.

## 3. BF-U07-RDP01-IR-02 — Foundation digest and key compatibility not decided

**Severity:** BLOCKER / UNRESOLVED_PHYSICAL_CONTRACT.

The design notes that the existing table has `payload_digest` and globally unique `idempotency_key` and proposes to reinterpret `payload_digest` as full protected-binding fingerprint **subject to future consumer compatibility checking** (§4.2, §7). That means RDP-01 does not yet select a safe interoperable storage design. Existing Foundation `sameCanonicalInput` compares digest verbatim, and a global idempotency unique index means local key namespacing must be deterministic across tenants/consultations rather than assumed.

**Required remediation:**
- Decide and document whether existing `payload_digest` keeps its original meaning; inventory all call sites of `resolveOrCreate` and migrations/fixtures that assume the digest meaning. If replacement would change existing consumers, design a *versioned U07-owned binding digest* rather than mutating Foundation semantics.
- Specify the exact global storage `idempotency_key` derivation or collision-resistant namespace and collision/fail-closed rules across consultation/tenant/profile, respecting the current 128-char column.
- Freeze transactional persistence and error semantics for canonical + side-binding: single DB transaction with uniqueness proof, or a reviewed pending/quarantine journal with non-success response and deterministic repair. Choose one authorized V1 option rather than listing alternatives with unresolved authority.
- Document how the existing `@Transactional` method and JPA constraint violation paths interact with side-binding commit/race, without claiming runtime tests have passed.

**Closure:** an exact physical schema/transaction decision, cross-consumer compatibility inventory and reviewed concurrency/crash design, with subsequent database integration tests deferred to implementation.

## 4. BF-U07-RDP01-IR-03 — Historical eligibility / missing-checkpoint authority is insufficiently specified

**Severity:** BLOCKER / OWNER_AUTHORITY_GAP.

RDP-01 §6 introduces `VERIFIED_HISTORICAL_REATTACHABLE` and `RUNTIME_RECONCILIATION_REQUIRED`, but does not freeze which **authoritative durable facts** prove that an old waiting interaction is historically valid when U06's projected eligibility was tied to `parent_wait_effect_id + checkpoint_id` and runtime evidence is missing or stale. The proposed `U07AdmissionSnapshotResolver` can otherwise turn a bare hashed eligibility token or an old checkpoint record into a current admission.

The Unit Spec explicitly says missing/stale checkpoint alone must not fabricate a Business REJECTED. The repair branch is valid in principle, but requires a positive business owner provenance chain rather than merely permissive fallback.

**Required remediation:**
- Enumerate authoritative owner/source/versions used to establish current or historically reattachable U06 wait (Question delivery confirmation; F3 Question state; Pending Question; Consultation WAITING_USER; parent effect; cancellation/supersession/expiry; clinical-version provenance).
- Separate `business wait established` from `P02 checkpoint resumable`, and distinguish authoritative state unavailable from authoritative state invalid.
- Define the conditional handoff when eligibility was never issued because Thread failed to enter `AWAITING_USER`: no fabricated `VERIFIED_HISTORICAL_REATTACHABLE`; either approved reconciliation of the same wait by its owners or fail-closed deferral.
- Specify snapshot consistency, version/fencing / recheck at F8 and apply, including U15 concurrent expiry. Do not invent owner authority to mutate U06 wait.
- Add positive and negative admission examples for confirmed business wait + missing checkpoint, unconfirmed delivery, revoked wait and missing owner evidence.

**Closure:** owner-provenance matrix and exact decision/handoff table accepted by independent review, followed by RDP-04 P02 compatibility design.

## 5. Finding disposition for original IR-01..06

| Original review question | Decision |
|---|---|
| IR-01 RESUME_REQUEST reference-only policy vs Phase 8/9 | CONDITIONALLY ACCEPTABLE: not explicitly prohibited by frozen K02; label as a U07-specific policy and require a concrete compatible mapping; does not authorize skipping F8 for first processing |
| IR-02 payload_digest / binding compatibility | BLOCKED by BF-U07-RDP01-IR-01/02 |
| IR-03 new event ID + same answer | BLOCKED until stable alias identity and RDP-02/RDP-03 one-winner handoff are explicit |
| IR-04 eligibility recheck | BLOCKED by BF-U07-RDP01-IR-03 |
| IR-05 ledger/binding transaction | BLOCKED by BF-U07-RDP01-IR-02 |
| IR-06 profile/PHI/trace | PASS for design scope; implementation security evidence still required later |

## 6. Non-blocking observations

- Event types `USER_ANSWER` and `RESUME_REQUEST` are consistent with Phase 8 K02; however, resume request reference-only policy remains a proposed U07 refinement until reviewed mapping is frozen.
- RDP-01 should not decide semantic equivalence, DUPLICATE precedence or whether a late event can be ACCEPTED; these remain F8 RDP-02.
- The 20 scenario entries form **a design plan**, not actual test execution.
- RDP-04 must later own checkpoint repair; RDP-03 must later own APPLIED/Question/Consultation/U02 exactly-once effects.

## 7. Verdict and next gate

```text
U07-RDP-01 Independent Design Review = REVISE_REQUIRED
Reviewed exact head = 0a8f75f54a148ebd17243ebc1db529d9ce4d9d6e
BF-U07-RDP01-IR-01 = OPEN
BF-U07-RDP01-IR-02 = OPEN
BF-U07-RDP01-IR-03 = OPEN
U07-RDP-01 Contract = DESIGN_CANDIDATE / NOT_FROZEN
B-U07-RG-01 = OPEN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / real patient / PHI = BLOCKED
```

Next governed action: **U07-RDP-01 Targeted Design Remediation**, then **U07-RDP-01 Targeted Independent Design Re-Review** on the amended exact head. No merge, implementation or production authorization follows from this independent review.
