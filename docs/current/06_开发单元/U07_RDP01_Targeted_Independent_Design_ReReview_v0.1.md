# U07-RDP-01 Targeted Independent Design Re-Review v0.1

> Date: 2026-10-08
> Target: [PR #266](https://github.com/cxjchelsea/AIdoctor/pull/266)
> **Exact remediation head:** `2ee68ca1f15467ea942915c8ae8dcb05637c8a22`
> Prior reviewed head: `0a8f75f54a148ebd17243ebc1db529d9ce4d9d6e`
> Prior independent review: [PR #267](https://github.com/cxjchelsea/AIdoctor/pull/267)
> Main authority/code: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Verdict: REVISE_REQUIRED / 1 CLOSED, 2 REMAIN OPEN**.
> This is an independent targeted **design** re-review, not code execution, implementation authorization, merge authorization, or production approval.

## 1. Exact-head and scope evidence

Comparison `0a8f75f...2ee68ca`: ahead 1, behind 0; **one** changed file `docs/current/06_开发单元/U07_RDP01_Consumer_Inbound_Event_Admission_Contract_v0.1.md`; **167 additions / 31 deletions**; no Runtime or test code change.

Re-read the remediated design at the exact target commit. Cross-checked existing:
- `CanonicalBusinessEventLedger.java` (`@Transactional`, `resolveOrCreate`, conflict recovery);
- `CanonicalBusinessEventRecord.java` (event_id PK, idempotency_key global UNIQUE, payload_digest stored as opaque string);
- MySQL `db/migration/V2__create_clinical_runtime_foundation.sql` and Oracle `db/migration-oracle/V3__create_clinical_runtime_foundation.sql` (both foundation event tables);
- `FoundationRuntimeBaseTest.java` (Foundation digest tests include `sha256:abc`, confirming no universal fixed 64-character format contract);
- `U06WaitCoordinator.java` + `U07ResumeEligibilityProjector.java` (returns eligibilityId generated from parent wait effect / checkpoint after AWAITING_USER; no separately evident durable eligibility issuance record in these two classes);
- Phase 8 K02/K10 and Phase 9 wait/resume invariants, previously reviewed in PR #267.

Observed repository tree at main exact baseline has 2,634 entries and includes both MySQL and Oracle migration branches. These observations are **not** an exhaustive repository-wide code-call-site inventory.

## 2. BF-U07-RDP01-IR-01 — Alias / canonical fingerprint

**Decision: PASS / CLOSED at design level.**

- Original canonical_event_id now remains a stable Foundation event PK, while incoming same-key event ID may be an alias.
- Full binding fingerprint explicitly excludes original/alias `business_event_id`, transport attempt, audit/timestamp churn, idempotency token and Foundation key.
- Exactly 21 protected fields and strict framing are enumerated; `storage_idempotency_key` scoped derivation is explicit and fits 128 characters.
- V1–V8 symbolic identity relations and added T03/T05A/T05B cover same-key reattachment, changed binding conflict and cross-scope isolation.
- Same-answer/new-key cases correctly remain for F8/RDP-02 with exactly-once apply for RDP-03.

**Follow-on evidence:** exact deterministic binary/hash vectors and concurrency database tests still belong to RDP-06 / authorized implementation; closure does not claim they executed.

## 3. BF-U07-RDP01-IR-02 — Foundation compatibility / transaction

**Decision: PARTIALLY_REMEDIATED / BLOCKER REMAINS OPEN.**

Resolved at design-intent level:
- Foundation `payload_digest` remains an immutable **event-payload** digest, not the U07 full business-binding fingerprint. New U07-only binding fingerprint separated.
- One physical relational database + same ACID outer transaction selected. Cross-DB fallback explicitly forbidden.
- Rollback-only/JPA unique-index race is recognized; poisoned transactions must roll back and recovery/recheck occur in a fresh transaction.
- Global scope-derived key is bounded and namespace-specific.

**Unresolved original required closure:**
1. §4.3 explicitly says **repository-wide consumer and migration/test-fixture inventory remains unverified**, while prior review BF-U07-RDP01-IR-02 required *inventory all call sites and migrations/fixtures* before accepting semantic compatibility. Foundation's existing tests use opaque digest values such as `sha256:abc`; this does not contradict a U07-only hex SHA-256, but it means the shared Foundation has not globally frozen a 64-character fingerprint format.
2. The schema/physical deployment and migration plan for `u07_canonical_event_binding` is not separately specified for both MySQL and Oracle migration lineages; unique FK/cross-table atomicity remains a conditional assumption rather than an evidenced compatible migration design.
3. The design admits exact transaction interception / `@Transactional` rollback-safe uniqueness race placement requires follow-up physical review. This may be verified before implementation with a concrete call/transaction boundary and failure table, without demanding runtime tests prematurely.

**Required targeted correction:** append an exact code-consumer/fixture/migration impact inventory, freeze MySQL/Oracle side-binding DDL/index/foreign key/immutability obligations at design level, and show outer transaction / retry-as-new-transaction topology and application of the current Foundation ledger without claiming race integration tests passed. If some inventory cannot be completed, retain explicit dependency blocker for aggregate readiness and do not claim this review finding CLOSED.

## 4. BF-U07-RDP01-IR-03 — Eligibility provenance after checkpoint loss

**Decision: PARTIALLY_REMEDIATED / BLOCKER REMAINS OPEN.**

Resolved:
- New `U07AuthoritativeWaitEvidence` field matrix names authoritative F3, U06, Consultation/P01, U15 and P06 versions.
- Clear positive/negative decision table for confirmed live wait, missing/stale checkpoint, expired/cancelled interaction, absent U06 issuance and unavailable owner.
- Appropriately does not let a plain projected hash or runtime state grant business validity. RDP-04 retains P02 compatibility; owner versions must be rechecked before F8/apply.

**Unresolved physical provenance assumption:** design now makes **durable original U06 eligibility issuance ref with authoritative binding** a mandatory positive input. At the reviewed main baseline, `U06WaitCoordinator.establish` derives the eligibility ID and returns it in an in-memory `Result`; `U07ResumeEligibilityProjector.project` hashes parent effect + checkpoint. The two reviewed classes alone do **not** demonstrate a persisted `original_eligibility_issuance_ref` or a separately queryable issuance authority. The design does not identify a concrete existing durable source, retrieval/query key, or deterministic owner-authorized reconstruction of issuance if no record exists. The historical reattach path is therefore formally fail-closed by design, but its intended **positive** valid missing-checkpoint path is not yet proven physically applicable.

**Required targeted correction:** identify a concrete verifiable U06 durable issuance/effect/trace authority **and its query contract**, OR freeze a deterministic independently auditable derivation from already-persisted U06 wait records and immutable provenance (with verification of actual AWAITING_USER transition and no fabricated issuance), OR register a required upstream U06 controlled amendment before any implementation/readiness gate. Include source-absent and original-issuance-never-created negative cases. Do not weaken no-eligibility=>no ordinary U07 resume or allow currentness based on a hash alone.

## 5. Ownership, security, and gate regression

| Check | Verdict |
|---|---|
| Business F8 validity != P02 resume compatibility | PASS |
| Foundation canonical event identity != F8 decision | PASS |
| F3 Question and U02 Clinical Fact ownership | PASS |
| U15 cancellation and expiration owner | PASS |
| Cross-scope idempotency safety as proposed design | PASS, pending physical verification |
| Original U06 issuance durable source physically established | **NOT_DEMONSTRATED** |
| Repository-wide Foundation compatibility inventory | **NOT_DEMONSTRATED** |
| Synthetic scope and no real PHI/live patient admission | PASS for design scope |
| Runtime code/test modified by remediation | NO |
| Implementation Authorization | NOT_GRANTED |

## 6. Formal decision and next action

```text
U07-RDP-01 Targeted Independent Design Re-Review = REVISE_REQUIRED
Target head = 2ee68ca1f15467ea942915c8ae8dcb05637c8a22
BF-U07-RDP01-IR-01 = PASS / CLOSED
BF-U07-RDP01-IR-02 = PARTIALLY_REMEDIATED / OPEN
BF-U07-RDP01-IR-03 = PARTIALLY_REMEDIATED / OPEN
New independent blocking findings = NONE
U07-RDP-01 = DESIGN_CANDIDATE / NOT_FROZEN
B-U07-RG-01 = OPEN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / real patient / PHI = BLOCKED
```

Next: **U07-RDP-01 Second Targeted Design Remediation (BF-02/03 only)**, then exact-head targeted independent re-review. Do not mark PASS based solely on author-proposed designs that still depend on undocumented physical authority or migration compatibility. No PR merge or source implementation authorized.
