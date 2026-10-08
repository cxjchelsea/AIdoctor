# U07 Foundation Consumer Transaction / Effective Spring Manager — Targeted Semantic Compatibility Review v0.1

> Review date: 2026-10-08
> **Actual source HEAD**: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Lexical evidence: [PR #297](https://github.com/cxjchelsea/AIdoctor/pull/297), GitHub Actions [run 37736999966](https://github.com/cxjchelsea/AIdoctor/actions/runs/37736999966), artifact `11531528615`; independent inventory [PR #298](https://github.com/cxjchelsea/AIdoctor/pull/298) @ `a2a505a2d2e7596acd552d8d7d1013ded0283eeb`
> U07 Foundation targeted design independently reviewed in [PR #296](https://github.com/cxjchelsea/AIdoctor/pull/296); selected `U07AdmissionApplicationService` non-transactional ingress + pinned `TransactionTemplate(REQUIRES_NEW)`.
> **Verdict: TARGETED_SEMANTIC_REVIEW_COMPLETE / EFFECTIVE_TRANSACTION_COMPATIBILITY_NOT_PROVEN / REQUIRED_PHYSICAL_AUTHORITY_REMEDIATION.**
> `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED`.
> Source/evidence review only; no business code, schema, database, test execution, merge, owner authorization, PROFILE-A or PHI.

## 1. Scope and evidence discipline

The full immutable Git-tracked lexical inventory was **actually executed** against `main@6d4fd787...`, with CI success, 2,112 tracked paths, 2,063 text files scanned, 49 binaries classified, 0 skipped, 256 occurrences, ten first-order Foundation match paths (nine code/schema/test, one historical doc), and 51 broad transaction-reference paths. These numbers describe a lexical scan, **not** 51 transactional consumers. The original ZIP and constituent SHA-256s were checked in the preceding independent review PR #298.

This review additionally fetched exact-head source contents and independently traced the **confirmed U01→Foundation→CDP→Binding→Consultation→ClinicalRun** call graph plus adjacent U06 JDBC/Wait use. A configured Spring transaction manager or runtime DataSource identity is **not** inferable from repository-wide zero lexical matches for `PlatformTransactionManager`, `TransactionTemplate` or `DataSource`: Spring Boot 2.7.8 and Spring Data JPA may auto-configure those beans. The checked `application.yml` indicates active `dev` profile but does not itself resolve the effective datasource. No running Spring application context, JPA transaction coordinator, MySQL connection or Oracle connection was queried.

## 2. Confirmed semantic consumer graph (exact-head source)

```text
U01ConsultationService.start(command)   [@Transactional; public Spring service]
  ├─ CanonicalBusinessEventLedger.resolveOrCreate(eventId,consultationId,
  │      "START_CONSULTATION",idempotencyKey,payloadDigest)   [@Transactional]
  │     └─ CanonicalBusinessEventRepository [JPA; read eventId/key, save]
  ├─ ConsultationRepository.findById()
  ├─ CDPManager.createCDP(userId,sessionId)   [@Transactional]
  │     ├─ CDPRepository.save(CDP)
  │     └─ CDPVersionService.createInitialVersion(CDP)   [@Transactional]
  │           └─ CDPVersionRepository.save(initial version)
  ├─ RuntimeBindingService.bind(...)   [@Transactional]
  │     └─ RuntimeBindingRepository [JPA; findById/save/duplicate catch-read]
  ├─ ConsultationRepository.save()
  └─ ClinicalRunCoordinator.openRun(...)   [@Transactional]
        ├─ RuntimeBindingService.requireBinding()
        ├─ CanonicalBusinessEventRepository.findById(eventId)
        └─ ClinicalRunRepository.save()
U01 replay:
  U01ConsultationService.start -> ClinicalRunCoordinator.requireOriginalRun()
```

**Explicitly confirmed implementation behavior:**
- U01 `start` uses `START_CONSULTATION`; this Foundation event **does not** require U07 canonical Binding. U07-only `USER_ANSWER/RESUME_REQUEST` orphan rules must not quarantine it.
- Existing U01 service, Foundation Ledger, CDPManager, CDPVersionService, RuntimeBindingService and `ClinicalRunCoordinator.openRun` have declared `@Transactional` annotations. In the common Spring proxy mode, the calls crossing injected Spring beans can participate in one caller `REQUIRED` transaction *if* the bean proxies actually intercept the calls and all repositories use the same effective manager/persistence unit. This is a **conditional semantic expectation**, not a verified physical fact.
- `CDPManager.createCDP` also persists an initial `CDPVersion` via another annotated service. This is a previously under-described U01 **transitive** write and must be included in rollback/commit tests.
- `ClinicalRunCoordinator.openRun` checks canonical event consultation ownership before creating the initial Run. Original `event_id` identity cannot be rewritten by U07 alias handling.
- `RuntimeBindingService.bind` catches a `DataIntegrityViolationException` from `repository.save` and then immediately queries the supposed winner in the same transactional call. Foundation Ledger does the same. SQL INSERT/constraint errors can arise on later flush/COMMIT; after actual Hibernate/JPA constraint failures the transaction may be rollback-only. This is **a source-backed reliability hazard**, not proof of a production incident or universal dialect-specific failure.
- `U06 ConsultationWaitTransitionService.establish` uses a Consultation row `PESSIMISTIC_WRITE` and `@Transactional(isolation=SERIALIZABLE)`. `JdbcU06DeliveryStore.reconcileConfirmed` uses JDBC with `@Transactional(SERIALIZABLE)`; its class in inspected code is `final` and not annotated as a Spring `@Service` or `@Component`. Whether an injected instance is proxied / whether annotation interception occurs **requires bean-wiring proof**. The latter is not an assertion that U06 JDBC must share Foundation transactions; it is a *separate owner* boundary.

### 2.1 Exact source provenance for independent corroboration

| Source file at exact main HEAD | Source blob SHA | Relevant declaration |
|---|---|---|
| `runtime/u01/U01ConsultationService.java` | `55948ef6de77693f546e43954754c14b7854d667` | `@Transactional start`, calls eventLedger, CDP, runtime binding, Consultation, ClinicalRun |
| `runtime/foundation/CanonicalBusinessEventLedger.java` | `3b7cb7191e6b10290b5c5cbb53d237c26b22b498` | `@Transactional resolveOrCreate`, `repository.save` then constraint-catch/read |
| `runtime/foundation/RuntimeBindingService.java` | `da552124d4aea2faf1d5d24c779be5346d4d0e0d` | `@Transactional bind`, `save` then same-transaction catch/read |
| `runtime/foundation/ClinicalRunCoordinator.java` | `a0d991b7e0286e025e93227fa3e83ffec7fea5e1` | `@Transactional openRun`, canonical Event and Run binding |
| `service/cdp/CDPManager.java` | `993b4c5f2d313f7416d142378a67eb8d4f6491fa` | `@Transactional createCDP`, CDP `save`, initial version delegate |
| `service/cdp/CDPVersionService.java` | `a4e8407120b20b3229f8402290ace57b9480b79e` | `@Transactional createInitialVersion`, version `save` |
| `runtime/u06/wait/ConsultationWaitTransitionService.java` | `8c83d85b5b5b4484988644b7c2100a173e7d48cf` | Consultation guard/serializable U06 wait operation |
| `runtime/u06/delivery/JdbcU06DeliveryStore.java` | `6b67b75bb121410a0a78b6cbf188a5369ad5d1fb` | final class, JDBC-based U06-only operations, annotated `@Transactional` |
| `diagnosis-service/pom.xml` | `dfa3c6f1cfc274ca796bf1bde8824ed1d9b80bb5` | Spring Boot 2.7.8, Spring Data JPA, Flyway, MySQL/Oracle JDBC dependencies |
| `diagnosis-service/src/main/resources/application.yml` | `7a9d6931da6763f83769a5234ac8aa77459b0be0` | active dev profile comment; no effective application-context bean map |

Paths for Java rows are relative to `diagnosis-service/src/main/java/com/aidoctor/diagnosis/`, except the two explicit `diagnosis-service/` paths.

## 3. Effective transaction-manager / DataSource evidence matrix

| Required question | Source-only answer | Physical proof needed | Verdict |
|---|---|---|---|
| Does U01 `start()` cross Spring proxies into Ledger/CDP/Binding/Run? | injected Spring-style services, public annotated methods | ApplicationContext bean classes, AOP proxy inspection, invocation path | **PLAUSIBLE / NOT_PROVEN** |
| Is one JPA `PlatformTransactionManager` used by all U01 repositories? | no explicit manager referenced in lexical inventory; Boot/JPA dependencies present | actual bean names/qualifiers, `@Primary`, manager identity, persistence-unit/EntityManagerFactory mapping | **UNKNOWN** |
| Is CDP version insert committed/rolled back with U01? | `createInitialVersion()` is cross-bean annotated call in `createCDP()` | one physical connection/transaction boundary and crash-injection SQL readback | **UNKNOWN** |
| Can Foundation catch/read recover from uniqueness error in active TX? | same-call catch+find observed; `save()` may defer SQL | actual MySQL/Oracle flush/constraint and `rollbackOnly`, `UnexpectedRollbackException` traces | **NOT_ACCEPTED_AS_SAFE** |
| Can U07 use pinned `REQUIRES_NEW` from non-transactional ingress? | accepted U07 design only; no actual class | source/bean-manager wiring, absence of ambient TX, independent callback/commit/readback origin | **NOT_IMPLEMENTED** |
| Can Foundation and U07 Binding share one physical write transaction? | U07 Binding/DAO/V7 absent on main | same DataSource and manager instance, underlying transaction/connection, atomic row count | **NOT_DEMONSTRATED** |
| Does real Oracle dev use same manager as optional MySQL profile? | dev indicated, drivers exist | profile-specific runtime bean and JDBC metadata for each independent suite | **NOT_DEMONSTRATED** |
| Does U06 JDBC `@Transactional` execute through an AOP proxy? | JDBC store final and not directly stereotype-annotated; may be externally wrapped | actual bean factory/wrapper, Spring AOP adapter, JDBC manager selection | **UNKNOWN / NONFOUNDATION_SIDE_BAND** |
| Are zero SourceManager literals proof that none exists? | lexical zeros | never make that inference; inspect Spring Boot autoconfiguration | **NO** |
| Is effective JPA tx manager same as JdbcTemplate transaction manager? | no physical evidence | same DataSource vs different managers, transaction synchronization enlistment | **UNKNOWN; owner boundaries must remain separate** |

### Nonproduction proof contract (future; no execution claimed)

Produce a **diagnostic only** Spring context/transaction topology report pinned to the exact source SHA and a synthetic, credential-redacted PROFILE-B setup. For each critical method (U01 start, Foundation Ledger, CDPManager, CDPVersionService, RuntimeBindingService, ClinicalRunCoordinator and future U07 admission), record:
- Spring proxy class and target type; actual `TransactionInterceptor`/advisor presence, method visibility and interceptibility; injected call graph and bean qualifiers.
- `PlatformTransactionManager` bean identity (logical non-secret stable ID), transaction propagation and `TransactionSynchronizationManager` resource map; EntityManagerFactory and `DataSource` identity, **without exposing credentials, JDBC URLs with secrets or PHI**.
- On both MySQL and Oracle, safe synthetic write/read/failure probes recording the transaction/connection identity, save vs flush vs COMMIT and rollback-only transitions. U01 original event/CDP/version/Binding/Consultation/Run after each fault must be all-or-none according to independently approved semantics.
- U07 new primary key/global idempotency key and side binding must commit together with no F8 before COMMIT, under the separately authorized V7 scope. If V7 not authorized, restrict to **read-only context topology** and U01 safe existing synthetic test profile. Do **not** deploy U07 V7 or execute new clinical write paths merely to satisfy a readiness audit.
- Independent expected outcomes for confirmed new original, preexisting winner/reattach, uniqueness FLUSH failure, COMMIT UNKNOWN, U01 `START_CONSULTATION` without U07 Binding, U07 alias to U01 ID, and original Run invariants.
- All evidence is source/head/profile/owner-bounded and independently reviewed. If Boot auto-configures multiple eligible transaction managers, the U07 design's **pinned selected manager** must be unambiguous or remain NOT_READY.

## 4. Targeted semantic findings and impact decisions

### BF-U07-FOUND-SEM-01 — runtime effective manager / proxy identity unknown

**BLOCKER / OWNER_TOPOLOGY_NOT_PHYSICALLY_PROVEN.** Existing annotations show plausible intended transaction coupling, but there is no runtime bean/proxy/resource/connection evidence confirming the full U01 graph or prospective U07 Foundation+Binding write uses the same transaction manager/physical DB. A source `@Transactional` review cannot close this condition. Do not infer U07 implementation readiness or atomicity from lexical zeros.

**Next required controlled deliverable:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Read-Only Effective Spring Manager/Consumer Topology Evidence`. This is an authorization-bounded **diagnostic contract/evidence design**, not a blanket permission to change Runtime. Define exact HEAD, safe environment, non-secret resource identity and owner reviewer. If code instrumentation/DB access is required, obtain explicit authorization first.

### BF-U07-FOUND-SEM-02 — existing U01 collision/rollback recovery remains unproven

**BLOCKER / SHARED_LEDGER_COLLISION_SAFETY_UNVERIFIED.** Both CanonicalBusinessEventLedger and RuntimeBindingService catch potential uniqueness errors then reread under their transactional method; save may postpone actual SQL. The graph includes CDP and initial version side writes. Production damage is **not** evidenced, but the selected U07 admission must not rely on this path returning valid COMMIT after an exception. A Foundation/U01 owner decision is required as to whether `CA-U07-FOUNDATION-COLLISION-TX-SAFETY-01` is needed. Its authorization is **not** inferred from this review.

**Next required controlled deliverable:** `U07 Foundation Collision / U01 Transitive Rollback Semantic Proof Design` under safe synthetic owner-approved test profiles, with before/after all six resource readbacks and negative exact-head assertions. A real failure must produce `NOT_READY` and a separately reviewed Foundation/U01 remediation—not an ungoverned fix.

### RF-U07-FOUND-SEM-01 — dynamic/external consumer proof residual

**REQUIRED FOLLOW-THROUGH**, not a failed lexical scan. PR #297 established exhaustive **tracked-file lexical** coverage. It cannot prove absence of reflection, external services or dynamically configured writers. Resolve by source/owner API inventory plus transaction-capable integration/topology report; flag every unknown rather than claiming complete runtime call graph.

### RF-U07-FOUND-SEM-02 — U06/U15/P02 physical transactional interfaces

**SEPARATE OWNER DEPENDENCIES.** U06 wait's Consultation lock and JDBC delivery store do not certify U15 first-decision fence or P02 B1 co-location. Do not import U06 source features as authorization for U07 recovery. Shared guard DB B1 remains independently unproven and the existing owner CAs stay blocked.

## 5. Exact closure conditions, separate stages

| Subgate | Status at this review | Evidence needed for positive gate |
|---|---|---|
| `SOURCE_LEXICAL_INVENTORY_COMPLETENESS` | **PASS** (PR #297/298) | previously accepted immutable CI/artifact integrity |
| `FOUNDATION_FIRST_ORDER_CONSUMER_GRAPH` | **SOURCE_CONFIRMED / BOUNDED** | U01 direct + ClinicalRun direct + transitive CDP/version and binding; owner acceptance |
| `EFFECTIVE_SPRING_MANAGER_TOPOLOGY` | **NOT_PROVEN** | independent pinned nonprod context/bean/connection evidence |
| `FOUNDATION_COLLISION_RECONCILIATION_SAFETY` | **NOT_PROVEN** | real DB unique/flush/rollback semantics and U01 all-or-none owner readback |
| `U07_SINGLE_DB_ATOMIC_BINDING` | **NOT_IMPLEMENTED** | authorized physical proof, no optimistic readiness claim |
| `MYSQL_ORACLE_DIALECT_EQUIVALENCE` | **NOT_EXECUTED** | two isolated dialect suites and independent Oracle/Fixture |
| `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01` | **NOT_PASSED** | scoped closure decision after owner/source proofs are independently accepted |

**Phase distinction:** A read-only source audit may establish all direct lexical references before physical U07 code exists. It cannot fairly require *finished U07 code* as evidence for a pre-implementation authorization while simultaneously withholding permission to implement. Split a future `FOUNDATION_PREIMPLEMENTATION_SOURCE_COMPATIBILITY` and a later `FOUNDATION_POSTIMPLEMENTATION_PHYSICAL_ATOMICITY` gate if needed, with both explicitly blocking the corresponding stage rather than silently treating either as already passed.

## 6. Formal review decision

```text
U07 Foundation Consumer Transaction /
Effective Spring Manager Targeted Semantic Compatibility Review
= COMPLETE / SEMANTIC_AND_EFFECTIVE_MANAGER_COMPATIBILITY_NOT_PROVEN

Exact physical source = main@6d4fd787600e3a57f01f3e17893e6d98893ac546
Inventory evidence = PR #297 / #298; CI 37736999966; 2112 tracked; 256 matches

SOURCE_LEXICAL_INVENTORY_COMPLETENESS = PASS
U01 / Foundation / CDP / Binding / ClinicalRun declared graph = SOURCE_CONFIRMED
EFFECTIVE_SPRING_MANAGER_TOPOLOGY = NOT_PROVEN
FOUNDATION_COLLISION_RECONCILIATION_SAFETY = NOT_PROVEN
U07_ATOMIC_BINDING_RUNTIME = NOT_IMPLEMENTED

BF-U07-FOUND-SEM-01 = OPEN
BF-U07-FOUND-SEM-02 = OPEN
RF-U07-FOUND-SEM-01 = REQUIRED
RF-U07-FOUND-SEM-02 = SEPARATE_OWNER_DEPENDENCY

BF-U07-FOUND-AUD-01..04 = OPEN
BF-U07-FOUND-AUD-03 = LEXICAL_SUBGATE_COMPLETE / SEMANTIC_NOT_CLOSED

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 Aggregate Compatibility = CONDITIONALLY_ACCEPTED_DESIGN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next governed work:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Read-Only Effective Spring Manager / Consumer Topology Evidence Contract`, then `U07 Foundation Collision / U01 Transitive Rollback Semantic Proof Design`, each requiring bounded authorization before any physical instrumentation. After independent evidence collection and review, perform `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 — Targeted Closure Re-Evaluation`. No changes were made to business Runtime, database schemas, protected state, clinical endpoints or production.
