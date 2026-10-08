# U07 Foundation Exhaustive Exact-Head Source Inventory — Independent Evidence Review v0.1

> Date: 2026-10-08
> Evidence producer: [PR #297](https://github.com/cxjchelsea/AIdoctor/pull/297), **exact audit-tool HEAD** `d3c6d95cc716d291209c669afa9a74f96a0b6ca1`
> **Physical source inspected:** `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Git source tree:** `1bfe776f76c986d4e6199a9b820f2cc20773a181`
> CI execution: [Actions run 37736999966](https://github.com/cxjchelsea/AIdoctor/actions/runs/37736999966) / job `113178711510`
> Evidence artifact: [u07-foundation-exact-head-source-inventory / 11531528615](https://github.com/cxjchelsea/AIdoctor/actions/runs/37736999966/artifacts/11531528615)
> **Verdict: PASS / TRACKED_SOURCE_LEXICAL_INVENTORY_INTEGRITY ONLY; CONSUMER_SEMANTIC_COMPATIBILITY_PENDING.**
> **GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED.**
> No runtime code, migration, physical DB transaction, clinical traffic or merge.

## 1. Scope, execution and independent integrity validation

The prior review PR #293 had only bounded file inspection and an incomplete GitHub code-search response. PR #297 now includes a **real, completed** GitHub Actions scan over a separately checked-out immutable main commit, rather than merely a script or proposed runner:

| Exact evidence | Verified value |
|---|---|
| Workflow | `U07 Foundation Exact-HEAD Source Inventory (evidence only)` |
| Workflow run / event | `37736999966` / `pull_request` |
| Run and sole audit job result | **completed / success** |
| Scan source HEAD | `6d4fd787600e3a57f01f3e17893e6d98893ac546` |
| Scan source Git tree | `1bfe776f76c986d4e6199a9b820f2cc20773a181` |
| Total Git tracked files | **2,112** |
| UTF-8 decoded text-file scans | **2,063** |
| Binary files individually read/classified | **49** |
| Skipped tracked paths | **0** |
| Lexical path/line/term matches | **256** |
| First-order Foundation matching paths | **10** (nine code/schema/test and one historical design document) |
| Broader transaction-reference candidate paths | **51** |
| Git ls-files NUL list SHA256 | `26cc3ec84a2940571a22c3c160c545ca9f5376a165b00516645513a98fdfb3a2` |
| JSON evidence SHA256 | `b28d72868833a8c109938025a6077dd6799924e89e31a34572c4d186f5bcf077` |
| TSV evidence SHA256 | `7ce9b4403683b864ae9a5beeae3d62d797bad313d020340142008e9ce0ec0fdc` |
| Summary Markdown SHA256 | `0d429a1c83120a75b0cf744a642f5280a5dc6fc478b8f2b16c1cc5437b92e0e2` |
| Uploaded ZIP artifact SHA256 | `0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089` |
| Artifact retention | configured 30 days; preserve digest and run ID for audit |

The independent reviewer downloaded the actual ZIP artifact through the GitHub connector and **recomputed SHA256 for all three unzipped evidence files and ZIP**, matching CI log hashes/artifact metadata. Parsed the JSON to confirm `2,063 + 49 = 2,112` unique tracked paths with no missing file records and `256` typed occurrences. This is real executed inventory evidence, unlike the earlier audit draft. It is deliberately **lexical** and does not prove every runtime/reflective/transitive linkage.

## 2. First-order Foundation source impact inventory (all ten match paths)

| Source path, relative to repo root | Evidence type | Required consumer/owner conclusion |
|---|---|---|
| `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/CanonicalBusinessEventLedger.java` | Foundation writer API; lines 20–66 | Preserve `resolveOrCreate` signature and `sameCanonicalInput`; duplicate-key same-transaction recovery risk remains |
| `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/CanonicalBusinessEventRecord.java` | Foundation entity/DDL mapping | PK `event_id`, unique global idempotency key, payload digest semantics unchanged |
| `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/CanonicalBusinessEventRepository.java` | Foundation persistence interface | `findById`/idempotency winner read still requires fresh healthy transaction after failed write |
| `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/ClinicalRunCoordinator.java` | **direct repository consumer** | `openRun` uses canonical Event ID and Consultation; `requireOriginalRun` must not mint a new original on U07 aliases |
| `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/U01ConsultationService.java` | **confirmed direct API caller** | `@Transactional start` creates `START_CONSULTATION` canonical Event, CDP, Binding, Consultation, Run; U07 must not change original event or rollback semantics |
| `diagnosis-service/src/main/resources/db/migration/V2__create_clinical_runtime_foundation.sql` | MySQL original DDL | Preserve historical V2, PK/global unique constraint; U07 V7 additive only |
| `diagnosis-service/src/main/resources/db/migration-oracle/V3__create_clinical_runtime_foundation.sql` | Oracle original DDL | Preserve historical V3; no invented dialect equivalence or migration PASS |
| `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/FoundationRuntimeBaseTest.java` | unit/mock consumer | no proof of rollback-only under real DB |
| `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u01/U01ConsultationServiceTest.java` | U01 test caller | mocked Ledger and same-event replay; production DB concurrency still requires real evidence |
| `docs/current/06_开发单元/U06_Implementation_Authorization_Review_v0.1.md` | historical design reference | documentation mention, not a runtime direct writer/consumer |

All paths above were present in the full lexical result; the first nine are the actual first-order code/schema/test assets. Neither a historical design mention nor a generic shared idempotency key is an independent runtime consumer.

### 2.1 Direct and transitive consumer classification

**Confirmed direct caller:** `U01ConsultationService.start` invokes `CanonicalBusinessEventLedger.resolveOrCreate(... START_CONSULTATION ...)` under `@Transactional`. After that it invokes `CDPManager.createCDP`, `RuntimeBindingService.bind` and `ClinicalRunCoordinator.openRun`. Separate source inspection at the same exact `main` established `CDPManager.createCDP` is also `@Transactional` and creates CDP + an initial version. This is a **transitive owner/transaction graph**, even though `CDPManager` does not itself reference `canonical_business_event` by name.

**Confirmed direct repository reader:** `ClinicalRunCoordinator.openRun` uses `CanonicalBusinessEventRepository.findById(eventId)` and checks Consultation equality. `requireOriginalRun` looks up the historical Run by original consultation/event. These paths must remain unchanged under U07 alias retry.

**Adjacent collision-sensitive service:** `RuntimeBindingService.bind` catches `DataIntegrityViolationException` and rereads in the same call. Its safety is a distinct U01/Foundation impact question; it cannot be changed as an implicit consequence of U07's design.

**Related but separate state owner:** U06 `ConsultationWaitTransitionService` uses Consultation row locking, and `JdbcU06DeliveryStore` has `@Transactional(SERIALIZABLE)` JDBC writes to U06 delivery tables. Neither is demonstrated by the lexical scan to be a direct Foundation `canonical_business_event` writer. U06 issuance is still a separately blocked owner CA.

### 2.2 Broader lexical candidate classes and limits

Across all matches, counted:
```text
ledger_class                  12
ledger_repo                   11
ledger_record                 19
ledger_call                    5
ledger_table                   4
foundation_unique              3
payload_digest                 3
idempotency_key               68
runtime_binding               19
transaction_annotation        50
transaction_template           0
transaction_manager            0
data_source                    0
fresh_tx                       0
direct_jpa                    55
direct_sql                     7
after_commit                   0
```

**Important:** these are exact lexical hit totals, **not** counts of actual active transactions. The 51 broader transaction-candidate paths include CDP, U06 wait/delivery/trace, state governance, other service repositories and tests. They must be examined by causal reachability and Spring bean/transaction-manager configuration, rather than treated as 51 Foundation consumers.

Zero hits for literal `PlatformTransactionManager`, `TransactionTemplate` or `DataSource` in this exact tracked source **does not establish** that the runtime lacks a real transaction manager: Spring Boot/JPA may auto-configure one. It instead means this source inventory cannot prove **which physical manager and EntityManager** owns Foundation and proposed U07 writes, especially under active dev/Oracle and MySQL profiles.

## 3. Compatibility assessment based on actual inventory

| Compatibility assertion | Classification | Evidence / remaining proof |
|---|---|---|
| Foundation has stable canonical event entity and both original dialect DDLs | **SOURCE_CONFIRMED** | matched Java/JPA and V2/V3 SQL |
| U01 is a real existing Foundation API consumer | **SOURCE_CONFIRMED** | `U01ConsultationService.java` call `resolveOrCreate` line 52 |
| ClinicalRun is a direct event-repository consumer | **SOURCE_CONFIRMED** | `ClinicalRunCoordinator.java` repo reference and `openRun` |
| U01 transactions compose with CDP/Binding/Run | **DECLARED_SOURCE_CHAIN / PHYSICAL_INCOMPLETE** | `@Transactional` annotations and method calls inspected, effective Spring transaction manager unknown |
| RuntimeBinding duplicate-key exception/catch can be rollback-only | **POTENTIAL_RISK / NOT PHYSICALLY PROVEN** | source pattern present; no MySQL/Oracle concurrency tests |
| No other textual first-order Foundation caller exists in tracked source | **LEXICAL_INVENTORY_SUPPORTED** | all 2,112 tracked files scanned, nine code/schema/test + one doc first-order paths; dynamic/proxy/generated/external paths remain UNKNOWN |
| U07 can add side binding in one true physical COMMIT | **NOT_IMPLEMENTED / NOT_DEMONSTRATED** | V7 DB migration/U07 coordinator absent from main |
| `REQUIRES_NEW` owner read after rollback is actually independent | **DESIGN_ACCEPTED / NOT_DEMONSTRATED** | proposed code not present; no Spring manager/connection evidence |
| U01 survives Foundation collision/commit-unknown without side effects | **NOT PHYSICALLY VERIFIED** | mock tests only, DB/transaction-manager proof still required |
| U07 original U06 eligibility issuance | **NOT_AUTHORIZED** | hash projection is not durable U06 issuance |
| U07 synthetic test scope versus PHI/production | **DESIGN-ONLY / EXECUTION_BLOCKED** | no positive clinical/production authority |

## 4. Remaining audit blockers and next precise evidence requirements

- `BF-U07-FOUND-AUD-03` **lexical source inventory subgate can be marked COMPLETED_WITH_INTEGRITY_EVIDENCE**, with the immutable run/artifact above. It **cannot be marked a complete semantic transaction/consumer compatibility acceptance** until an independently accepted graph including boot auto-configured transaction manager/DataSource/EntityManager, CDP manager, U01 rollback/commit boundaries, generated/reflection/remote integrations and observed environment binding is supplied. Retain `BF-U07-FOUND-AUD-03 = OPEN / LEXICAL_SUBGATE_COMPLETE, SEMANTIC_OWNER_REVIEW_PENDING`.
- `BF-U07-FOUND-AUD-01` still requires approved collision-safe JPA/DB proof and exact U01 regressions.
- `BF-U07-FOUND-AUD-02` still requires authorized U07 source and physical Foundation+Binding same-transaction COMMIT, not just the design proposal.
- `BF-U07-FOUND-AUD-04` still requires both **MySQL and Oracle** physical rollback/concurrency/commit-UNKNOWN tests plus independently reviewed Oracle/Fixtures.

**Next governed step:** `U07 Foundation Consumer Transaction / Effective Spring Manager Targeted Semantic Compatibility Review`, using the 51 transaction candidates grouped by actual call reachability, plus a nonproduction harness that records the *same exact* transaction manager/DataSource/EntityManager for U01 and prospective U07. This may lead to a separate `CA-U07-FOUNDATION-COLLISION-TX-SAFETY-01` decision; do not grant it by omission.

## 5. Formal evidence-only decision

```text
U07 Foundation Exhaustive Exact-Head Source Inventory CI
= EXECUTED / SUCCESS / ARTIFACT_VERIFIED

Exact main source = 6d4fd787600e3a57f01f3e17893e6d98893ac546
Source tree SHA = 1bfe776f76c986d4e6199a9b820f2cc20773a181
Tracked files = 2112
Text scanned = 2063
Binary inspected/classified = 49
Skipped tracked paths = 0
Typed lexical occurrences = 256
First-order Foundation matching paths = 10
Broad transaction-reference candidate paths = 51
Evidence artifact ID = 11531528615
Evidence archive and component digests = VERIFIED

SOURCE_LEXICAL_INVENTORY_COMPLETENESS = PASS
FOUNDATION_CONSUMER_SEMANTIC_COMPATIBILITY = NOT_PASSED / PENDING
FOUNDATION_PHYSICAL_JPA_MYSQL_ORACLE_EVIDENCE = NOT_EXECUTED

BF-U07-FOUND-AUD-01 = OPEN
BF-U07-FOUND-AUD-02 = OPEN
BF-U07-FOUND-AUD-03 = OPEN / LEXICAL_SUBGATE_COMPLETE
BF-U07-FOUND-AUD-04 = OPEN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

This review records an actual **successful exhaustive tracked-source lexical scan** and **independently checked artifact integrity**, not full operational compatibility or a Foundation gate PASS. No business code or databases were modified and no clinical tests executed.
