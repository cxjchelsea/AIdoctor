# U07 Foundation Targeted Source Inventory / Transaction Remediation — Independent Design Review v0.1

> Review date: 2026-10-08
> Target design: [PR #294](https://github.com/cxjchelsea/AIdoctor/pull/294)
> **Exact reviewed design HEAD:** `f7eb2a2662596aff009874fff62f7cb74e5586fe`
> Design blob: `da84af2615dd9201fc85fd0c3b287d27a5f1fc40`
> Exact inspected current source: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Earlier failed source audit: [PR #293](https://github.com/cxjchelsea/AIdoctor/pull/293)
> **VERDICT: REVISE_REQUIRED / TWO DESIGN BLOCKERS.**
> Review-only artifact; no runtime/test/migration changes or implementation authorization.

## 1. Evidence and scope

Fetched PR #294 at its exact HEAD and independently inspected the proposed §§1–8. PR #294 adds only one Markdown document (253 lines). Cross-checked its proposed transaction protocol against the actual `CanonicalBusinessEventLedger`, `CanonicalBusinessEventRecord/Repository`, U01 `U01ConsultationService.start` / `ClinicalRunCoordinator`, and the selected RDP-01 `SINGLE_DB_INLINE_SYNTHETIC_V1` contract. This review assesses **sufficiency of a design**, not whether repository-wide code text was exhaustively scanned or actual MySQL/Oracle transactions executed.

### Accepted elements

- Preserves shared Foundation method, `payload_digest` semantics and global key uniqueness rather than silently rewriting U01.
- Treats a caught `DataIntegrityViolationException` in a Hibernate/JPA transaction as potentially **rollback-only**, including deferred `save`/FLUSH/COMMIT failures; does not equate returning from `repository.save` with successful COMMIT.
- Requires independent winner readback after failed write scope, immutable original canonical event ID, exact U07 binding/scope match and zero F8 effects before authoritative success.
- Keeps RDP-01 pre-COMMIT Foundation INSERT/FLUSH → binding failure (both roll back) separate from **genuinely preexisting committed** U07 orphan quarantine.
- Proposes additive MySQL/Oracle V7 schema, independently reviewed synthetic fixtures/expected Oracle, 16 future `FOUND-T01..16` cases, and U01 non-regression.
- Explicitly admits GitHub code search was incomplete and full tracked-source `git grep` plus owner-reviewed consumer manifest are **not yet executed**. This is correct audit honesty and must remain a gate.

Two missing normative protections prevent approving the transactional design as written.

## 2. BF-U07-FOUND-DIR-01 — physical outermost transaction authority is not uniquely selected

**OPEN / AMBIENT_TRANSACTION_AND_PREMATURE_COMMIT_AUTHORITY.**

The proposed §3 says:

```text
U07AdmissionTransactionCoordinator [Spring transaction proxy / TransactionTemplate]
   -> canonicalLedger.resolveOrCreate(...)  // joins the SAME actual tx
   -> side binding insert
   -> COMMIT
 -> independent readback
 -> ADMITTED_FOR_F8
```

§3.1 allows either `TransactionTemplate` **or** a proxied public `@Transactional` worker; it says “only after physical outer COMMIT” but does not freeze **who owns the outermost transaction** and how that assertion is enforced when called by an existing `@Transactional(REQUIRED)` caller. A default `TransactionTemplate(REQUIRED)` or a public `@Transactional(REQUIRED)` method can join an enclosing transaction; successful return from the callback/method does **not** mean the ambient outer transaction committed. An independent readback in a separate `REQUIRES_NEW` transaction cannot see an uncommitted new canonical+binding row; conversely an already existing matching row could be read back while the enclosing U07 transaction is still pending, allowing premature **new-admission** authorization unless origin and commit identity are distinguished.

**Required targeted remediation:**
1. Select an **exclusive physically controlling** U07 transaction entry contract, e.g. an ingress application-service boundary that **rejects ambient caller transactions** and invokes a known Spring `PlatformTransactionManager` via a distinct `TransactionTemplate(PROPAGATION_REQUIRES_NEW)` *outside* the caller's success scope; alternatively choose a provably equivalent owner-issued `afterCommit` message/outbox design with explicit ordering, not a list of options. State why the selected pattern is compatible with U01's `START_CONSULTATION` `REQUIRED` chain without changing it.
2. Freeze behavior if a caller already holds a transaction: reject it, or formally define how the U07 transaction is suspended and whether new effects are lawful with an uncommitted caller context. No inner callback/return may publish F8, HTTP success or result labeled `ADMITTED_FOR_F8` based only on an ambient transaction joining.
3. Distinguish `NEW_COMMITTED_ORIGINAL_EVENT_AND_BINDING` from `REATTACHED_TO_PREEXISTING_COMMITTED_WINNER` with a verified fresh-source commit/readback provenance; no preexisting other event can validate an uncommitted *new* event.
4. If the selected mechanism cannot prove physical COMMIT and origin after a callback returns (or an asynchronous transport ACK is lost), return `RECONCILIATION_REQUIRED`, not success. Specify `UnexpectedRollbackException`, commit UNKNOWN and post-commit callback failure precedence.

**Re-review acceptance:** one normative, self-consistent exact physical transaction-owning flow for U07 with a full call/commit timeline, negative test for `ambient REQUIRED` caller, positive own-COMMIT readback and zero F8 before physical COMMIT. No actual DB proof is claimed at design review.

## 3. BF-U07-FOUND-DIR-02 — committed-orphan classification must be event-type and owner scoped

**OPEN / CROSS_CONSUMER_ORPHAN_FALSE_POSITIVE.**

§3, §3.1–3.2 and §4 say an authoritative **Foundation event** with no `U07CanonicalEventBinding` is quarantined as a committed orphan. But the shared Foundation ledger is already used by `U01ConsultationService.start()` for **`START_CONSULTATION`**. That legitimate U01 event has no U07 binding by design. A generic “Foundation winner with missing U07 side binding ⇒ orphan” can therefore quarantine valid U01 records or misclassify a user-supplied alias/event-ID collision that points at a non-U07 event.

The write protocol correctly says keep U01 semantics unchanged, but the readback classification is too broad. It must be explicitly limited to **the exact authorized U07 event-type namespace and original U07-owned key**, with independent Foundation `event_type`, consultation, payload digest, scope and original ID checks.

**Required targeted remediation:**
1. Freeze the authoritative `U07_APPLICABLE_EVENT_TYPES = {USER_ANSWER, RESUME_REQUEST}` and scoped U07 storage-key prefix/provenance; only an **already committed U07 event** with a missing mandatory U07 binding can be quarantined as a U07 legacy orphan. A `START_CONSULTATION` or other non-U07 event lacking this binding is normal, not a U07 orphan.
2. Freeze direct conflict precedence: if incoming U07 `event_id` points to committed U01/non-U07 Foundation event, classify `IDENTITY_CONFLICT` and perform **no U07 repair**, regardless of any matching key lookup or alias claims; do not create U07 binding for the U01 event.
3. Add explicit design test cases: legitimate U01 event without U07 binding is unaffected; U07 same-key alias to original U07 row succeeds; alias/event-ID collision with U01 fails identity conflict; *preexisting committed* U07 event without binding is quarantined. Check readback owner identity for each.
4. Update the V7 schema/readback applicability and `FOUND-T10/11/12` expectations so that “binding missing” is checked **after** Foundation `event_type`/owner/scoped storage key checks, not before.

**Re-review acceptance:** typed, mutually exclusive applicability and outcome table matching U01 and U07 original event semantics, with no generic all-Foundation orphan rule.

## 4. Carried audit/evidence deficits — not silently recast as design blockers

- `BF-U07-FOUND-AUD-03`: current scanned source set remains **bounded**. The proposed `git ls-files` / `git grep` manifest with full file/line/blob/role reconciliation is an acceptable *audit plan*; **execution and independent validation remain required before the Foundation gate can pass**. The suggested `':!docs/**'` exclusion must be documented as a scope exclusion; normative contract dependencies in docs are still cross-checked through the design review.
- `BF-U07-FOUND-AUD-01`: source contains a potential rollback-only collision hazard, but production corruption is **not demonstrated**; MySQL and Oracle must verify exception timing and fresh-transaction winner behavior.
- `BF-U07-FOUND-AUD-02`: U07 V7 binding/coordinator is not implemented or authorized, and physical co-location is unproven.
- `BF-U07-FOUND-AUD-04`: `FOUND-T01..16` are consecutively numbered **design requirements**, not executable or green tests. Both dialects, transaction-manager identity and isolation need independent evidence.
- `CA-U07-FOUNDATION-COLLISION-TX-SAFETY-01` remains only a **conditional proposal** subject to owner/U01 impact and explicit authorization. Even after design remediation, no one may unilaterally modify the shared Ledger.

## 5. Formal gate and next action

```text
U07 Foundation Targeted Source Inventory /
Transaction Remediation Independent Design Review
= REVISE_REQUIRED

Exact reviewed HEAD = f7eb2a2662596aff009874fff62f7cb74e5586fe
BF-U07-FOUND-DIR-01 = OPEN / OUTERMOST_COMMIT_AUTHORITY
BF-U07-FOUND-DIR-02 = OPEN / U07_EVENT_TYPE_ORPHAN_APPLICABILITY

BF-U07-FOUND-AUD-01..04 = OPEN / NOT_CLOSED
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
CA-U07-FOUNDATION-COLLISION-TX-SAFETY-01 = PROPOSED_IF_NEEDED / NOT_AUTHORIZED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED

U07 Aggregate Compatibility = CONDITIONALLY_ACCEPTED_DESIGN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next permitted step:** `U07 Foundation Targeted Design Remediation` on original design PR #294 for these two findings, followed by a separate **Targeted Independent Design Re-Review** pinned to the amended exact HEAD. Then execute full exact-head source inventory under an approved audit procedure and independently evaluate Foundation audit closure. No code, tests, migrations, owner grant or merge in this review.
