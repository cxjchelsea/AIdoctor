# U07-RDP-01 Third Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-08
> Design PR: [#266](https://github.com/cxjchelsea/AIdoctor/pull/266)
> Exact reviewed head: `4f98e950f6d7dfedd7c046dedfc9db7e52135b04`
> Previous amended head: `b7545adb0ef93f51caac415d4cced3612ece6927`
> Previous review: PR #269
> Authority baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Verdict: **PASS / NO_NEW_DESIGN_BLOCKER — CONDITIONAL DESIGN CLOSURE ONLY**.
> No implementation, merge, release, production, PROFILE-A, PHI or real-patient authorization.

## 1. Evidence and review scope

The commit comparison `b7545adb..4f98e950` is **ahead one, behind zero** and changes exactly one document: `docs/current/06_开发单元/U07_RDP01_Consumer_Inbound_Event_Admission_Contract_v0.1.md`, **123 additions / 65 deletions**. No source code or database migration was executed or changed in the reviewed delta.

Re-read amended sections §4.3.1–4.3.4, §4.4, §5 and §6.2.2 at exact SHA. Cross-checked the previous independent review and the previously verified main Foundation event ledger, entity/repository, Foundation tests, MySQL V2 and Oracle V3 foundation migrations, plus U06 wait/checkpoint and execution/issuance evidence.

The target review question is whether the **RDP-01 physical design** is sufficiently unambiguous and compatible at the documented interface to proceed through later design gates, **not** whether complete runtime compatibility has been demonstrated.

## 2. BF-U07-RDP01-IR-01 — canonical identity alias

**CLOSED / unchanged from earlier review.**

The immutable canonical event ID, incoming alias, global scoped idempotency key and fixed 21-field protected binding remain separate. Incoming alias is excluded from the canonical binding fingerprint, and different event/new key vs same-key replay remain correctly distinguished. No contradiction is introduced by the new inline storage: deterministic logical handles derive from the stable storage key, not the transport alias.

## 3. BF-U07-RDP01-IR-02 — Foundation compatibility and physical atomicity

**PASS / DESIGN CLOSED, WITH REQUIRED DOWNSTREAM GATE.**

### 3.1 One definite authoritative durable representation

The prior design had an unresolved option between external immutable refs and inline bytes. New §4.3.2 explicitly selects:

```text
SINGLE_DB_INLINE_SYNTHETIC_V1
Foundation canonical event row
+ U07 canonical binding bytes
+ synthetic USER_ANSWER payload bytes when applicable
= one database transaction, one committed authoritative representation
```

External object/payload stores and mixed transactional fallback are explicitly excluded. `binding_payload_ref` and `protected_binding_ref` are deterministic *logical handles* into the committed row, not separate durable authorities. Correctly forbids any F8 handoff before the entire outer commit.

### 3.2 Physical schema compatibility

MySQL and Oracle each now have explicit append-only proposed V7 schemas: the U07-owned table has a PK/FK on the canonical event ID, separate protected binding fingerprint and canonical byte/BLOB columns, synthetic-answer BLOB, answer digest, and the same logical consultation/wait index. Original Foundation schema, method signatures, digest semantics and global unique key stay untouched.

Conditional NULL/correct foreign-reference checks are explicitly adapter/database verification requirements. These are not falsely reported as existing migrations or successful tests.

### 3.3 Transaction and crash/retry boundaries

The chosen outer REQUIRED transaction contains Foundation event resolution and inline binding persistence; collision failure/rollback-only triggers full rollback and a genuinely separate REQUIRES_NEW winner-reconciliation transaction. Existing ledger's JPA catch-and-relookup is **not** assumed safe in a rollback-only transaction. Explicit negative effects cover partial insertion, lost commit response, key collision, mismatched binding and unknown DB outcome.

### 3.4 Deferred full-source compatibility check is a genuine **blocking gate**

`GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01` is now explicitly **REQUIRED / NOT_PASSED**, with exact candidate SHA, tracked-file content scan, per-hit inventory including MySQL/Oracle migrations and fixtures, independent evidence-only review and incompatible consumer controlled-amendment route. This is acceptable to **conditionally close design specificity**, not to claim Foundation's repository-wide compatibility has already passed.

The author explicitly states that code search returned `incomplete_results=true`; a non-truncated Git tree and bounded source sample do **not** constitute a whole-repo full-text impact scan. **Audit status must remain NOT_PASSED until executed and independently evidenced.** No positive implementation readiness or authorization can bypass this requirement.

**BF-02 conclusion:** the physical design ambiguity is resolved and an auditable, blocking compatibility gate now covers evidence not yet available. Design closes; physical compatibility evidence does **not** close.

## 4. BF-U07-RDP01-IR-03 — historical U06 eligibility issuance

**CONDITIONAL_DESIGN_RESOLUTION / prior decision preserved; mandatory upstream authority remains missing.**

No new purported U06 issuance implementation is introduced. The explicit `CA-U06-U07-ELIG-ISSUANCE-01` still defines the required owner-controlled durable issuance/query contract and must undergo separate detailed design, independent review, controlled approval, and physical compatibility/evidence before U07 implementation authorization. A hash or missing checkpoint alone cannot authorize a business resume.

This conditional closure must be carried to RDP-05 and aggregate compatibility/readiness; it is **not** an issuance-capability implementation PASS.

## 5. Nonblocking follow-up observations

1. The synthetic row-local answer payload has to remain consistently accessible through the agreed logical-ref adapter consumed by U02; owner-approved consumption contract belongs to RDP-03/04/05. U07 does not interpret it as Clinical Fact.
2. Design DDL is indicative of an executable migration, not an executed migration: MySQL/Oracle column-type, BLOB/CHAR behavior, FK/index, migration ordering and rollback integration tests remain mandatory after suitable authorization.
3. The reference-audit gate must produce a true exhaustive content scan. A GitHub search returning incomplete/no matches cannot pass that gate.
4. The inline-storage design is restricted to approved nonproduction synthetic data, with no real patient ingestion or external side effects.

These are **tracked implementation/readiness prerequisites**, not newly observed contradictory design decisions in the targeted delta.

## 6. Gate decision

```text
U07-RDP-01 Third Targeted Independent Design Re-Review = PASS / NO_NEW_DESIGN_BLOCKER
Reviewed exact head = 4f98e950f6d7dfedd7c046dedfc9db7e52135b04

BF-U07-RDP01-IR-01 = CLOSED
BF-U07-RDP01-IR-02 = DESIGN_CLOSED / PHYSICAL_EVIDENCE_PENDING
BF-U07-RDP01-IR-03 = CONDITIONAL_DESIGN_RESOLUTION / UPSTREAM_GAP_PENDING

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED / NOT_IMPLEMENTED

U07-RDP-01 Design = CONDITIONALLY_ACCEPTED / PENDING_AGGREGATE_COMPATIBILITY
U07-RDP-01 implementation = NOT_AUTHORIZED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / real patient / PHI = BLOCKED
```

**Next permissible design step:** `U07-RDP-02 F8 Business Resume Decision / Precedence Contract` can proceed as a *design candidate* while explicitly carrying both upstream dependencies. Neither the Foundation reference audit nor the U06 issuance controlled amendment is considered complete. If governance requires an explicit conditional design acceptance/closure record before proceeding, issue that record separately and do not silently upgrade RDP-01 to unrestricted FROZEN.

No merge, squash, rebase, source-code implementation or real-world activation is authorized by this review.
