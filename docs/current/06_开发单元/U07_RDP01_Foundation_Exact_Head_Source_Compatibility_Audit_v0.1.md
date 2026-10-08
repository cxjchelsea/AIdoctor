# GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 — Exact-Head Source / Compatibility Audit v0.1

> Audit date: 2026-10-08
> **Source HEAD:** `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> U07 readiness report: [PR #292](https://github.com/cxjchelsea/AIdoctor/pull/292) @ `315afd7fdf25d1807a8f37ad6ff77ac322134ea7`
> U07 aggregate design source: `c319c67e65a0e620e547d03514cbe45e25020975`; reviewed in [PR #291](https://github.com/cxjchelsea/AIdoctor/pull/291)
> **Gate verdict: NOT_PASSED / FAIL_WITH_REQUIRED_SOURCE_REMEDIATION_AND_EVIDENCE.**
> Audit mode: GitHub exact-head source inspection and schema comparison. No checkout, code compilation, database execution, test run, owner approval, migration or merge was performed.
> **Coverage limitation:** the complete transitive search of all Java/Python/JPA/SQL consumers, environments and generated artifacts **has not been established**. GitHub code search at this ref returned `incomplete_results=true` even for a known literal; do not treat a zero-hit response as exhaustive proof.

## 1. Normative objective and gate distinction

Determine whether the *actual* Foundation canonical event ledger, its clients, Spring transaction boundaries and MySQL/Oracle schemas can safely support U07 RDP-01's selected `SINGLE_DB_INLINE_SYNTHETIC_V1`: original Foundation `canonical_business_event` and an immutable U07 synthetic answer/binding row written in **one physical transaction**, committed atomically, with fail-closed winner reconciliation in a **genuinely new transaction**, stable original canonical identity, no PHI and zero F8 invocation before verified COMMIT.

This is a **source reference and compatibility audit gate**. It is NOT an independent implementation authorization and cannot be flipped PASS just because a design states `@Transactional(REQUIRED)` or contains a proposed V7 migration. Explicit source defects, missing transaction evidence and incomplete consumer enumeration keep this gate **NOT_PASSED**.

## 2. Exact-head inspected source and blob inventory

| Source at `main@6d4fd787...` | Blob SHA | Direct observation |
|---|---|---|
| `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/CanonicalBusinessEventLedger.java` | `3b7cb7191e6b10290b5cbb53d237c26b22b498` | `@Transactional` + `repository.save`, catches uniqueness race and rereads in the same call |
| `.../runtime/foundation/CanonicalBusinessEventRecord.java` | `6a20c29763a151602be46e19fe42edd8f22c09d6` | JPA table `canonical_business_event`, PK event ID, unique global idempotency key |
| `.../runtime/foundation/CanonicalBusinessEventRepository.java` | `d914ffe6c2a7181c7670f8466c806660a9b55a3b` | JPA repository, `findByIdempotencyKey` only |
| `.../runtime/u01/U01ConsultationService.java` | `55948ef6de77693f546e43954754c14b7854d667` | **confirmed existing** `@Transactional start()` client of `eventLedger.resolveOrCreate` |
| `.../runtime/foundation/ClinicalRunCoordinator.java` | inspected directly | existing event repository consumer via `openRun` and original-run lookup; not U07 answer owner |
| `.../runtime/foundation/RuntimeBindingService.java` | `da552124d4aea2faf1d5d24c779be5346d4d0e0d` | adjacent idempotent insert/catch/reread hazard for shared U01 process |
| `.../runtime/u06/wait/U06WaitCoordinator.java` | `43afb65060d4dd301078abc66b4f0500cc513eb6` | checkpoint/thread wait sequence and derived eligibility; not a U07 atomic event-binding writer |
| `.../runtime/foundation/RuntimeWaitCheckpointService.java` | `7e2c06a5f27c5f823c2e6a5507d5a980e16752ce` | checkpoint/Thread storage, no executable post-wait continuation |
| `.../runtime/u06/wait/ConsultationWaitTransitionService.java` | `8c83d85b5b5b4484988644b7c2100a173e7d48cf` | Consultation lock/serializable U06 waiting transition, not F8/U15 shared grant |
| `.../runtime/u01/ConsultationRecord.java` | `a9afe71101f936aa301a5eee7486f9b38da07e94` | versioned Consultation; `ACTIVE`, `WAITING_USER`, no complete terminal U15 grant protocol |
| `.../runtime/u01/ConsultationRepository.java` | `46aec69bf1fe3694dc751449f3ab3ae24def0132` | `@Lock(PESSIMISTIC_WRITE)` Consultation row read |
| `.../runtime/foundation/FoundationRuntimeBaseTest.java` | `3eef25acd7ceeaed80a29e09e866848bbf92f121` | Mockito Foundation transport replay/conflict checks, not physical MySQL/Oracle transaction proof |
| `diagnosis-service/src/main/resources/db/migration/V2__create_clinical_runtime_foundation.sql` | `73029495c5f46e8576e34f8c9a77644fb8c1c4c4` | MySQL Foundation `canonical_business_event` PK and unique idempotency |
| `diagnosis-service/src/main/resources/db/migration-oracle/V3__create_clinical_runtime_foundation.sql` | `78ba84df1ba36b9dd438cad3a4bdb7813bf0bb95` | Oracle Foundation table, corresponding PK/unique |
| `diagnosis-service/src/main/resources/db/migration/V6__add_u06_wait_runtime.sql` | `7ed5a82bfba3b3fed2666bee969a7fe276e67566` | MySQL U06 wait/Thread metadata |
| `diagnosis-service/src/main/resources/db/migration-oracle/V6__add_u06_wait_runtime.sql` | `ff9d97fcb199ff5728669a9386b04e5543c5451a` | Oracle U06 wait/Thread metadata |
| `diagnosis-service/src/main/resources/application.yml` | `7a9d6931da6763f83769a5234ac8aa77459b0be0` | profile `dev` active; no physical transaction manager/co-location evidence in this file |
| `diagnosis-service/pom.xml` | `dfa3c6f1cfc274ca796bf1bde8824ed1d9b80bb5` | Spring Boot 2.7.8 / Spring Data JPA / Flyway 7.15; MySQL and Oracle driver dependencies |

Repository tree inventory on the same exact source HEAD: **2,112 blobs**, **340 diagnosis-service production Java files**, Foundation package and both migration trees inventoried. A GitHub *tree listing* is a filename inventory, not the text-level consumer/transaction scan required for an exhaustive audit.

## 3. Confirmed client and cross-owner compatibility

### 3.1 Existing U01 caller is a mandatory protected consumer

`U01ConsultationService.start()` has `@Transactional` and calls `eventLedger.resolveOrCreate(..., "START_CONSULTATION", ...) ` before it creates CDP, binding, Consultation and original Run. This is a **live source-level integration dependency**. The Foundation event ledger is not an unused U07-only utility.

`ClinicalRunCoordinator.openRun()` references canonical events through `CanonicalBusinessEventRepository.findById` and enforces matching consultation; `ClinicalRunCoordinator.requireOriginalRun()` reads an original event/run relationship. These are known consumers/referents requiring non-regression checks. `RuntimeBindingService.bind()` shares a catch-and-reread pattern in U01 creation.

**Consequences for RDP-01:** U07 cannot change `resolveOrCreate` event ID matching, idempotency keys, `START_CONSULTATION` semantics, run ownership or U01 transaction boundaries without a separately authorized Foundation interface amendment and an explicit U01 regression matrix.

### 3.2 Transaction semantics not proven by annotations

`CanonicalBusinessEventLedger.resolveOrCreate()` is `@Transactional` with default `REQUIRED`, which ordinarily joins an existing Spring-managed transaction when called via its proxy. `U01ConsultationService.start()` is separately `@Transactional`. Neither annotation, by itself, proves a U07 binding INSERT is enlisted in **the exact same actual** DB/EntityManager/transaction manager or proves successful physical COMMIT before publishing to F8.

`CanonicalBusinessEventLedger.resolveOrCreate()` uses `repository.save(created)`, which may defer the SQL INSERT/unique violation until flush/transaction commit. Its `catch (DataIntegrityViolationException)` immediately executes `findById` or `findByIdempotencyKey` on the same surrounding transactional call. A uniqueness error can mark a Hibernate/JPA transaction rollback-only; **the catch/read does not clear rollback-only**. This is a documented **source-level unsafe assumption/hazard**, not proof that every DB/profile actually fails. U07 RDP-01 explicitly requires outer transaction rollback and a separate proxy-invoked `REQUIRES_NEW` winner readback after failed commit.

Likewise `RuntimeBindingService.bind()` catches `DataIntegrityViolationException` and queries a binding winner. U01's multi-write consistency can be broken if a caller treats a rollback-only operation as successful. Real race tests and a no-silent-success protocol are mandatory. Do not implement U07's correct coordinator by wrapping an already invalid inner transaction and returning `ADMITTED_FOR_F8`.

### 3.3 Incomplete exhaustive consumer inventory is an explicit gate failure

The inspected `U01ConsultationService`, `ClinicalRunCoordinator`, `RuntimeBindingService`, U06 Wait and Foundation tests are **confirmed source observations**, not an assertion that these are the only references. GitHub code search returned `incomplete_results=true` with zero items even for the known ledger class, so it cannot close the required all-consumer scan. This audit did **not** prove all 340 production Java files and all relevant test / SQL / Python call paths text-scanned. Until an independent exact-SHA **consumer manifest** with every reference (imports, method calls, repository direct reads, SQL writes, test fixtures, transaction-manager configuration and external adapters) is produced, `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01` must remain NOT_PASSED.

A later exhaustive result finding a new consumer means this compatibility matrix and dependent RDP gates must be updated/re-reviewed. The absence of a caller in the bounded inspection is not negative evidence.

## 4. Physical database compatibility and migration inventory

| Requirement | MySQL actual inspected | Oracle actual inspected | Gate outcome |
|---|---|---|---|
| Canonical event table | `V2` create, event_id PK, unique idempotency_key, payload_digest | `V3` create, corresponding PK and unique | **STRUCTURAL_BASELINE_PRESENT** |
| U07 inline binding row | no `V7__add_u07_canonical_event_binding.sql` in main | no corresponding V7 file in main | **ABSENT / NOT_IMPLEMENTED** |
| Foundation + U07 binding one outer COMMIT | design only, no physical participant/manager proof | design only, no physical participant/manager proof | **NOT_VERIFIED** |
| Binding FK to canonical event | proposed in unmerged RDP-01 design | proposed in unmerged RDP-01 design | **NOT_MIGRATED / NOT_TESTED** |
| Mutable idempotency-key namespace scoping and collision recovery | unique index exists, namespace owner contract not implemented | unique index exists, namespace owner contract not implemented | **NOT_VERIFIED** |
| BLOB/canonical 21-field fingerprint, replay alias and original binding | new design only | new design only | **NOT_VERIFIED** |
| Rollback after Foundation FLUSH / binding failure before outer COMMIT | not executed | not executed | **NOT_VERIFIED** |
| Collision causing rollback-only; new independent transaction readback | no integration proof | no integration proof | **NOT_VERIFIED** |
| Previously committed orphan quarantine without fabricating bytes | design only | design only | **NOT_VERIFIED** |
| No-F8-before-COMMIT and no PHI/leakage in row/trace | design only | design only | **NOT_VERIFIED** |

The MySQL and Oracle migration series have **different historical version numbering** for equivalent Foundation operations (`V2` vs `V3`) due to dialect-specific histories; this alone is not a defect, but U07 V7 migrations must be checked within each dialect's actual Flyway history. SQL names and JPA schema annotations are **not** sufficient to show Oracle and MySQL behave the same on rollback-only, BLOB, `CHAR` fingerprint padding, timestamps, transaction isolation, indexes and foreign-key enforcement.

Inspection of `application.yml` shows `spring.profiles.active: dev` but not a demonstrated end-to-end exact environment configuration; transaction manager, source profile values and deployed DB schemas require further exact-head/source review and authorized physical probes. No MySQL/Oracle session was executed in this audit.

## 5. Formal numbered audit findings and severity

| Finding | Severity | Evidence | Required closure |
|---|---|---|---|
| `BF-U07-FOUND-AUD-01` — uniqueness collision winner readback may occur in rollback-only transaction | **BLOCKER** | Ledger catch `DataIntegrityViolationException` and same-call read; adjacent binding catch; no independent fresh-transaction logic | approved Foundation-safe no-success-on-rollback protocol, exact source review, MySQL/Oracle concurrency evidence |
| `BF-U07-FOUND-AUD-02` — no demonstrated atomic Foundation+U07 binding writer in current main | **BLOCKER** | Foundation `save` only; no U07 V7 table/writer; proposed coordinator unmerged design | controlled physical implementation plan and real same-manager/COMMIT evidence; no F8 before committed readback |
| `BF-U07-FOUND-AUD-03` — exhaustive consumer and transaction participation proof incomplete | **BLOCKER** | known U01 and ClinicalRun clients; code-search incomplete; no approved full call graph | exact-source all-consumer manifest, bean/proxy/tx manager inspection, non-regression audit |
| `BF-U07-FOUND-AUD-04` — dual-dialect schema/replay/rollback proof unavailable | **BLOCKER** | V2/V3 baseline; no V7; only existing Foundation mock tests; real SQL not run | frozen V7 dialect schema + MySQL/Oracle integration tests for T17, alias, collision, rollback and legacy orphan |
| `RF-U07-FOUND-AUD-01` — U06 Wait Coordinator exposes derived eligibility ID without durable issuance | **REQUIRED OWNER DEPENDENCY / NOT A FOUNDATION-FIX BY ITSELF** | `U07ResumeEligibilityProjector` and U06 coordinator | `CA-U06-U07-ELIG-ISSUANCE-01` as separate U06-owned approval/implementation; no hash-only historical admission |
| `RF-U07-FOUND-AUD-02` — U01/CDP/createRun correctness under shared ledger failure | **REQUIRED REGRESSION** | `U01ConsultationService.start()`, `ClinicalRunCoordinator.openRun()`, Foundation mock base test | prove U01 replay/original-Run and rollback invariants unchanged in both dialects |

**Do not classify `BF-U07-FOUND-AUD-01` as proven production data corruption.** The source proves unsafe assumption and missing evidence; DB/JPA failure manifestation remains a physical investigation. Conversely a small happy-path mock test cannot satisfy its closure.

## 6. Required targeted remediation / independent acceptance pack

A later `GATE...Closure Re-Evaluation` must consume:

1. **Full exact-head graph:** source file list SHA/blobs; all references to `CanonicalBusinessEventLedger`, `CanonicalBusinessEventRepository`, `canonical_business_event`, `resolveOrCreate`, indirect U01/U06 consumers, scheduler and tests; code-search completeness proof (not just zero-result GitHub search); owner impact matrix. Include original consumer behavior snapshots.
2. **Transaction participation inventory:** Spring bean creation and proxy boundaries, `@Transactional` call sites, `REQUIRED`/new transaction, rollback-only behavior, JPA `save` versus `saveAndFlush`, flush/commit callback semantics, actual PlatformTransactionManager and DataSource identity, readback after UNKNOWN COMMIT; no self-invoked `REQUIRES_NEW`.
3. **Selected single-DB schema plan:** V7 MySQL/Oracle canonical binding migrations, exact canonical 21-field byte frame, checksum, answer bytes retention/encryption, unique original ID/alias, foreign keys/transaction isolation, any legacy data repair; independent DBA/owner compatibility review required before migration.
4. **No-blind-winner path:** on collision/constraint error, abort and roll back doomed transaction, then query winner plus U07 side-binding in separately authorized independent new transaction. Missing/mismatching binding = QUARANTINE. Same logical key but new answer/scope = CONFLICT. Do not let outer success return on rollback-only.
5. **Physical adversarial tests:** two concurrent writers same key, same-key payload conflict, U01 regression, failure after Foundation INSERT/FLUSH before binding, crash after binding before COMMIT, committed response lost, commit UNKNOWN, legacy orphan, original canonical alias, mixed consultation/scope, MySQL/Oracle FK/BLOB/CHAR and full migration upgrade from actual deployed Flyway states.
6. **Independent evidence:** exact-head source runner/migration hashes, test fixtures/Oracle reviewed separately, owner SQL state before/after, commit/readback logs, attempted F8/P02/P01/U02 zero counts on failure, profile isolation and independent evidence-only review.

**Authorization ordering:** audit/report design may be reviewed now. No Foundation code remediation, V7 migration or physical U07 tests shall be taken as authorized by this audit. A Foundation-impacting change needs an approved controlled amendment and explicit scoped implementation authorization; after that, test and independent review can close physical evidence. If the process distinguishes pre-implementation reference-audit PASS from post-implementation DB evidence, document separate gates rather than silently requiring future code before implementation authorization; current gate remains NOT_PASSED at the inspected head due to incomplete complete source inventory and unsafe collision-path assumptions.

## 7. Gate decision and next permitted step

```text
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01
= NOT_PASSED / FAIL_WITH_REQUIRED_REMEDIATION

Audit source HEAD = 6d4fd787600e3a57f01f3e17893e6d98893ac546
Verified structural Foundation schema = PRESENT
Complete consumer/transaction source proof = INCOMPLETE
U07 atomic inline binding implementation = ABSENT
MySQL/Oracle concurrency/rollback and evidence = NOT_EXECUTED

BF-U07-FOUND-AUD-01 = OPEN
BF-U07-FOUND-AUD-02 = OPEN
BF-U07-FOUND-AUD-03 = OPEN
BF-U07-FOUND-AUD-04 = OPEN

U07 Aggregate Compatibility = CONDITIONALLY_ACCEPTED_DESIGN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED

U07 PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Recommended immediate next step:** `U07 Foundation Reference Audit — Targeted Source Inventory and Transaction Remediation Design`, initially **read-only full exact-head consumer/proxy/DataSource scan**, then independently reviewed narrowly scoped `Foundation Canonical Ledger Collision/Commit-Safety Controlled Amendment` before any change to the existing ledger used by U01. This precedes a truthful audit closure re-evaluation. The separate `CA-U06-U07-ELIG-ISSUANCE-01` design can proceed without assuming the Foundation gate passed.

This report is an evidence-limited, source-backed NOT_PASSED audit; it does not claim that code was tested or that all repository consumers have been enumerated.
