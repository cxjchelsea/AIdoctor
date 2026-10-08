# U07 Foundation Reference Audit — Targeted Source Inventory and Transaction Remediation Design v0.1

> Date: 2026-10-08
> Design source HEAD: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Prior failed source audit: [PR #293](https://github.com/cxjchelsea/AIdoctor/pull/293) @ `680c69cbca97db468db8f0371248b0199173a4af`
> U07 aggregate author design: `c319c67e65a0e620e547d03514cbe45e25020975`; independent review [PR #291](https://github.com/cxjchelsea/AIdoctor/pull/291)
> **STATUS = DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN**
> **GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED**
> This document is a **bounded exact-head source inventory plus a proposed conditional remediation design**, not a complete all-repository full-text audit and not a code change or owner authorization.

## 1. Mandate, acceptance criteria, non-goals

Resolve `BF-U07-FOUND-AUD-01..04` from PR #293 without exposing U01 to unexpected Foundation semantic changes. A successful independent **design review** may close uncertainty about the selected remediation protocol but **must not close** the actual Foundation source audit until an exhaustively verifiable consumer scan and authorized physical database evidence exist.

V1 remains `SINGLE_DB_INLINE_SYNTHETIC_V1`: existing Foundation canonical event and new U07 protected binding + synthetic answer bytes in **one real ACID transaction**. Preserve Foundation event identity, method signature, global event `payload_digest` semantic, and U01 `START_CONSULTATION` behavior. `RESUME_REQUEST` has no fresh answer bytes. No PROFILE-A, PHI, real patient, production, external object store or real downstream send.

Distinguish four independent obligations:

1. **Reference completeness:** discover and classify every consumer or persistence/schema writer of the shared Foundation event identity.
2. **Legacy compatibility:** unchanged U01, ClinicalRun and any other callers' input/output contract and rollback behavior.
3. **New U07 atomic admission:** committed canonical event **and** matching immutable binding, never one without the other.
4. **Physical test/evidence:** MySQL and Oracle, true JPA/proxy/commit, concurrency, crash injection and post-commit owner readback.

## 2. Inspected source inventory (exact existing main)

The GitHub non-truncated tree contains **340 diagnosis-service production Java files**. A tree inventory does not imply their entire contents were searched. Directly content-inspected artifacts and conclusions:

| Exact source file | Source observation | U07 impact |
|---|---|---|
| `runtime/foundation/CanonicalBusinessEventLedger.java` | `@Service @Transactional resolveOrCreate(eventId, consultationId, eventType, idempotencyKey, payloadDigest)`; by event ID, then global idempotency key; `repository.save`, `catch(DataIntegrityViolationException)` and same-call `findBy...` | **Keep public contract; no assumption that catch/read can commit after a uniqueness error** |
| `runtime/foundation/CanonicalBusinessEventRecord.java` | event ID PK, globally unique `idempotency_key`, `payload_digest` VARCHAR(128) and immutable event fields | Additive U07 binding row; no rewrite of Foundation digest semantics |
| `runtime/foundation/CanonicalBusinessEventRepository.java` | `JpaRepository` with `findByIdempotencyKey` | read-after-error must be on fresh safe transaction, not doomed EntityManager |
| `runtime/u01/U01ConsultationService.java` | **confirmed existing direct caller**: `@Transactional start()` invokes ledger with `START_CONSULTATION`, then CDP, RuntimeBinding, Consultation and ClinicalRun | protect U01 outer-transaction semantics, original Run and replay; U07 may not silently change shared API |
| `runtime/foundation/ClinicalRunCoordinator.java` | `@Transactional openRun()` reads canonical event via repository; `requireOriginalRun()` reads run by consultation/event | never rewrite canonical event ID on retry or create second original run |
| `runtime/foundation/RuntimeBindingService.java` | `@Transactional bind()` also catches `DataIntegrityViolationException` and re-reads in the same call | distinct U01 adjacent collision/rollback-risk surface; investigate, no unapproved rewrite |
| `runtime/u01/U01NaturalLanguageStartService.java` | defines U01 natural-language ingress; inspected file has no direct Foundation Ledger textual reference | not an exhaustive *indirect* non-consumer proof; downstream `U01ConsultationService` still matters |
| `runtime/u05/U05AdmissionService.java`, `runtime/u06/U06AdmissionService.java` | inspected these specific files; no direct canonical ledger class reference found in bounded scan | no negative inference about indirect, reflective or other-owner call paths |
| `runtime/u06/wait/U06WaitCoordinator.java` | Wait Coordinator sequentially reserves metadata Checkpoint, marks Thread awaiting, returns projected `eligibilityId` | no U06 durable issuance or U07 canonical writer; separate owner CA still blocked |
| `runtime/u06/wait/ConsultationWaitTransitionService.java` | `@Transactional(SERIALIZABLE)`, Consultation `PESSIMISTIC_WRITE` wait mutation | analogous lock evidence, **not** proof of U15/F8 or U07 binding transaction |
| `runtime/foundation/RuntimeWaitCheckpointService.java` and `RuntimeThreadWaitTransitionService.java` | separate U06 Thread/Checkpoint methods and transactions | metadata-only, cannot be treated as U07 executable continuation |
| `runtime/u01/ConsultationRepository.java` | shared Consultation `PESSIMISTIC_WRITE` method | verified primitive, not U15 all-owner policy authorization |
| `src/test/.../foundation/FoundationRuntimeBaseTest.java` | Mockito Ledger same-event replay and mismatched-digest rejection; fixture uses values like `sha256:abc` | no MySQL/Oracle rollback test; no global regex SHA256 enforcement allowed |
| `src/test/.../u01/U01ConsultationServiceTest.java` | confirmed mocked Ledger `resolveOrCreate(... START_CONSULTATION ...)`, original Run and replay assertions | mandated pre/post regression; Mock pass is not physical transaction evidence |
| `src/test/.../u01/U01NaturalLanguageStartServiceTest.java` | inspected, no direct Foundation Ledger reference found | indirect call graph not closed |
| `db/migration/V2__create_clinical_runtime_foundation.sql` | MySQL Foundation event PK, global idempotency unique and consultation index | preserve historical V2 migration; proposed new binding as additive V7 |
| `db/migration-oracle/V3__create_clinical_runtime_foundation.sql` | Oracle equivalent event table and unique index/constraint | preserve historical V3; separately add V7 for Oracle |
| `db/migration/V6__add_u06_wait_runtime.sql` and `migration-oracle/V6__add_u06_wait_runtime.sql` | U06 Wait/Thread metadata; no U07 binding row | no physical U07 original answer binding |
| `diagnosis-service/pom.xml` | Spring Boot 2.7.8, Data JPA, Flyway 7.15, MySQL and Oracle JDBC dependencies | versions are **design probes**, not proof of active transaction manager or deployed dialect |
| `diagnosis-service/src/main/resources/application.yml` | `spring.profiles.active=dev`; no DataSource/transaction-manager details in this file | effective environment/profiles, DataSource ownership and auto-config need explicit audit |

**Known consumer graph from directly inspected source:**

~~~text
U01ConsultationService.start() [@Transactional]
  -> CanonicalBusinessEventLedger.resolveOrCreate() [@Transactional / REQUIRED]
       -> CanonicalBusinessEventRepository {findById, findByIdempotencyKey, save}
  -> CDPManager.createCDP() [transaction participation not yet audited]
  -> RuntimeBindingService.bind() [@Transactional / REQUIRED, duplicate-key catch-read]
  -> ConsultationRepository.save()
  -> ClinicalRunCoordinator.openRun() [@Transactional / REQUIRED]
       -> CanonicalBusinessEventRepository.findById()
       -> ClinicalRunRepository.save()
  U01 replay -> ClinicalRunCoordinator.requireOriginalRun()
~~~

No claim is made that this is the **complete** graph. A GitHub Code Search query at the audit HEAD returned `incomplete_results=true` and cannot prove zero unknown references. This design will not manufacture line/commit evidence for uninspected consumers.

### 2.1 Mandatory reproducible full-source inventory (to close BF-03)

At an authorized exact source tree checkout, run a **content-level** inventory over **all tracked files**, not GitHub search snippets. Suggested command, run from the repository root in a controlled environment:

~~~bash
git rev-parse HEAD
git status --porcelain
git ls-files -z > tracked-paths.nul
git grep -nI -E 'CanonicalBusinessEventLedger|CanonicalBusinessEventRepository|CanonicalBusinessEventRecord|resolveOrCreate[[:space:]]*\(|canonical_business_event|uk_canonical_event_idempotency|payload_digest|idempotency_key|RuntimeBindingService|@Transactional|TransactionTemplate|PlatformTransactionManager|DataSource' -- ':!docs/**' > foundation-references.txt
echo "grep-exit=$?"
git ls-files | wc -l
sha256sum tracked-paths.nul foundation-references.txt
~~~

The executor must record **exact Git tree SHA, scanned path count/bytes, excluded binary/generated paths, command version, grep exit status, errors and both file digests**. `git grep` exit 1 for no matches is a normal no-match status, not an authorization PASS; nontrivial tool failure must invalidate evidence. Include Java production+tests, SQL, XML, Maven resources, test fixtures, scripts, CI, configuration and any direct SQL or reflective consumers. Use targeted secondary scans for `EntityManager`, `JpaRepository`, `nativeQuery`, `JdbcTemplate`, `CREATE/ALTER TABLE`, `REQUIRES_NEW`, `AFTER_COMMIT` and cross-module endpoint calls.

Every hit must have `path:line:blob_sha:consumer_role:transaction_owner:key_scope_assumption:payload_digest_assumption:impact:approval`. Classify `READ`, `WRITE`, `TRANSITIVE`, `TEST`, `SCHEMA`, `CONFIG`, `INDIRECT_RUNTIME`; mark unresolved dynamic loading as `UNKNOWN`, not `NON_CONSUMER`. A **separate independent reviewer** must reconcile all matches and owner approvals. Do not treat this document's bounded observations as the executed manifest.

## 3. BF-01 targeted collision-safe transaction protocol

**Selected additive U07-specific integration, without immediately rewriting the shared Foundation Ledger**. The existing Ledger method signature and U01 caller remain unchanged in first remediation wave. A documented unsafe collision pattern in a shared service is a risk requiring physical investigation; U07 shall never rely on same-transaction catch+read returning commit-success.

Proposed components (not present in main, not authorized):

~~~text
U07IngressAuthorityGate [trusted PROFILE-B/scope/payload]
  -> U07CanonicalFrame/StorageKey [RDP-01 exact 21-field binding fingerprint]
  -> U07AdmissionTransactionCoordinator [Spring transaction proxy / TransactionTemplate]
       -> canonicalLedger.resolveOrCreate(...)  // joins the SAME actual tx
       -> U07CanonicalEventBindingRepository.insertOrCompare(...)
       -> U07 synthetic inline bytes insert (same U07 row)
       -> COMMIT physical outer transaction
  -> U07AdmissionCommitReadback [fresh owner snapshot only AFTER confirmed COMMIT]
  -> ADMITTED_FOR_F8  // only after canonical row+binding match owner-authoritatively

On UNIQUE CONFLICT or UNKNOWN COMMIT:
  no within-transaction positive recovery
  -> leave active write transaction and ensure rollback/commit resolution
  -> OwnerWinnerReadbackService [new proxy-invoked REQUIRES_NEW read-only tx]
       -> original event ID / global namespaced key winner
       -> immutable U07 binding, complete owner fingerprint, original bytes digest
       -> scope/tenant/event-type + historical alias compatibility
  -> EXACT_MATCH => REATTACHED_TO_CANONICAL, no fresh F8 or new event effect
  -> NO_WINNER + concurrent commit still unresolved => RECONCILIATION_REQUIRED
  -> ORPHAN winner-without-binding => QUARANTINE / block F8
  -> MISMATCH => IDENTITY_CONFLICT
~~~

### 3.1 Transactional requirements and no-success-on-rollback guarantee

- Use **one** actual `PlatformTransactionManager`, `DataSource` and JPA EntityManager binding for the Foundation row and U07 side binding. `@Transactional(REQUIRED)` is only a Spring declaration, never proof of physical participation.
- U07's outer `TransactionTemplate` or a proxied dedicated public `@Transactional` worker must return success **only after its callback/outer transaction has physically completed COMMIT**, followed by authoritative owner readback. Do not emit F8 from an inner service before commit, or use self-invocation to request a new transaction.
- `repository.save` can defer SQL error until FLUSH/COMMIT. Explicit bounded FLUSH can expose errors earlier but **does not** authorize positive result before COMMIT. If transaction is rollback-only or throws `UnexpectedRollbackException`, never return `ADMITTED_FOR_F8`.
- Collision is **not** automatically a lost race: incompatible payload/scope/Question/actor must return `IDENTITY_CONFLICT`. Winner lookup compares Foundation `consultation_id, event_type, key, payload_digest` and complete U07 binding fingerprint, not merely global key.
- For `same key + different event ID`, original Foundation `event_id` is the winner; later input ID is an **alias**, not another canonical event. `eventId` PK conflicting with a different key must be classified as identity conflict, **not** recovered by blindly selecting a key winner.
- After a uniqueness exception within the existing Ledger, a same-call catch/re-read may execute inside a rollback-only transaction. U07 may invoke that method within a transaction only if outer completion is monitored and failed commit results in an independent, isolated recovery. If source-level or dialect testing shows the shared catch masks a failure, prepare a **separate controlled Foundation amendment**, not an unreviewed U07 workaround that ignores rollback.
- Winner readback must be invoked by **a separate Spring proxy / independent transaction**, after the failed write scope exits, and it must not be nested in a rollback-only caller. If an enclosing caller transaction still exists, the system must suspend it safely; source/proxy/transaction-manager behavior needs independent test proof.
- A competing writer may still be uncommitted when lookup runs. `NO_WINNER` after a race does **not** mean safe permission to create a different canonical identity; reconcile with bounded same-key retry and eventual owner-confirmed terminal response.
- A committed original Foundation row with missing U07 binding is **quarantined**; do not manufacture binding bytes from a retry even if the caller supplies the same digest.
- For `UNKNOWN COMMIT`, query both tables under a new authoritative transaction by the **same original scoped key/event ID** before taking any action. An upstream HTTP acknowledgement lost after confirmed commit returns the original identity, not a second entry.
- All exceptions and rollback observations must produce bounded non-PHI structured error codes and zero F8/P02/P01/U02 physical attempts until an authoritative identity result.

### 3.2 Decision table

| Original write outcome | After failed/uncertain scope exits | Permitted caller result | Prohibited |
|---|---|---|---|
| successful actual COMMIT + both rows same immutable binding | new-transaction positive readback | `ADMITTED_FOR_F8` or exact `REATTACHED` | success based on `save()` return |
| duplicate key, failed flush/rollback-only | independent original winner + immutable binding equal | `REATTACHED_TO_CANONICAL` | reuse poisoned EntityManager |
| duplicate key, different U07 binding | verified mismatch | `IDENTITY_CONFLICT` | new key to hide collision |
| exception after Foundation flush before binding | both rows absent on authoritative readback | `FAILED_ROLLED_BACK`; bounded original-identity retry under approved policy | quarantine a nonexistent ghost row |
| exception after binding flush but before COMMIT | both absent if rollback | `FAILED_ROLLED_BACK` | positive event acknowledgement |
| unknown COMMIT / no coherent winner yet | bounded authoritative readback | `RECONCILIATION_REQUIRED` | speculative fresh event or F8 |
| previously committed Foundation winner, binding missing | owner confirms committed legacy orphan | `QUARANTINED_LEGACY_ORPHAN` | fabricate old answer bytes |
| same event ID already bound to another global key | original row conflict | `IDENTITY_CONFLICT` | reassign event ID |

### 3.3 Separation of U07-only addition versus shared Foundation amendment

**Phase A / U07-specific only:** new additive binding schema, U07 admission coordinator and entirely new readback service under separately authorized implementation scope. **No** modification to shared `CanonicalBusinessEventLedger.resolveOrCreate` signature, `payload_digest` field, global unique index, or U01 flow.

**Phase B / conditional shared hardening:** if exhaustive source inventory or actual JPA race tests demonstrate that `CanonicalBusinessEventLedger` or `RuntimeBindingService` can return a false-positive success or a poisoned transaction to existing callers, formally propose `CA-U07-FOUNDATION-COLLISION-TX-SAFETY-01` and obtain Foundation + U01 owner authorization and independent impact review **before** editing those services. Do not automatically refactor U01 from a U07 review. Distinguish the project-wide defect remediation from U07-specific admission design and give it its own independent tests.

## 4. BF-02 atomic Foundation/U07 binding V7 design

Logical immutable table `u07_canonical_event_binding`, schema/migration **candidate** for two dialects:

| Column / constraint | Requirement |
|---|---|
| `canonical_event_id` | PK **and FK** to `canonical_business_event.event_id`; original winner identity only |
| `storage_idempotency_key` | global U07-scoped key; compared against original Foundation winner |
| `contract_version`, `binding_schema_version` | frozen RDP-01 formats, not inferred from caller version |
| `event_type`, `consultation_id` | exact Foundation record and authenticated scope |
| `binding_fingerprint` | 64-char versioned SHA-256 over RDP-01's **21 exact protected fields** |
| `binding_bytes` and `synthetic_answer_bytes` | in-row immutable bytes, PROFILE-B synthetic only, typed NULL answer on RESUME_REQUEST |
| `answer_payload_digest`, `original_event_ref`, `target_answer_event_id` | original trusted payload and target contract; no invented new answer on RESUME_REQUEST |
| original U06 wait / Question/Thread/Run/version/actor/scope/authority refs | exact provenance, immutable, no client self-authority |
| `received_at`, `occurred_at`, minimal audit ref | original persisted timestamps; no alias overwrite |

Write order under **one physical outer transaction**: validate scope → derive canonical bytes/digests/key → Foundation insert or exact existing winner → U07 binding insert/compare → flush → COMMIT → fresh readback → only then F8 handoff. No separate `CANONICAL_BINDING_PENDING` success state, out-of-DB answer store or cross-database best-effort fallback.

**MySQL/Oracle**: independent V7 migrations matching each dialect's real Flyway migration history (Foundation is MySQL V2 and Oracle V3), FK/index naming, transactionally safe rollback semantics and binary handling. No history rewrite of existing V2/V3 or U06 V6. Schema-level checks must include idempotency namespace, original ID alias, byte equality and U01 replay. An Oracle schema name that appears equivalent is not a physical migration proof.

**Important subtlety:** Under existing Foundation `resolveOrCreate`, lookup by existing ID precedes lookup by idempotency key. The U07 coordinator must detect an incoming alias ID that is already bound to another original row and fail closed before taking a key winner as valid. Preserve **the original winning Foundation event ID** rather than changing it on an alias retry.

## 5. BF-03 exact consumer compatibility matrix and remaining audit gate

| Confirmed path | Risk from shared Ledger change | Required proof |
|---|---|---|
| U01 START_CONSULTATION new | outer tx may become rollback-only after key collision; must not leave CDP/binding/run partial | actual DB commit atomicity and old U01 result status |
| U01 same-event replay / same-key alias | new coordinator must not change event identity semantics; U01 stays unchanged | U01Test + MySQL/Oracle duplicate/commit-lost tests |
| U01 conflict changed payload | preserve `sameCanonicalInput` semantics and typed failure | before/after non-PHI error outcome |
| ClinicalRunCoordinator.openRun | original canonical event ID and same consultation must remain | no duplicate Run / cross-consultation acceptance |
| RuntimeBindingService.bind | adjacent catch/read rollback-only hazard | scoped owner review for U01, not automatic U07 mutation |
| U06 wait/eligibility | U07 may read wait facts only, not mint issuance | `CA-U06-U07-ELIG-ISSUANCE-01` separately |
| Proposed U07 coordinator | single event+binding outer COMMIT, alias reconciliation | new source/migration/runner after approval |
| Unknown consumers from unexecuted exhaustive scan | **UNKNOWN** | full `git grep` source manifest + independent consumer mapping |

**Compatibility condition:** do not declare `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01=PASS` merely on this table. Finish the reproducible full-source scan (§2.1) and independently accept every discovered consumer's old/new behavior, including direct SQL/config/test scripts. If the real code reveals a new invocation or changes source SHA, update audit and design before a gate decision.

## 6. BF-04 executable verification plan and authority boundaries

All these are **future required tests**, NOT executed or declared green:

| Oracle | Scenario | Exact negative/positive evidence |
|---|---|---|
| `FOUND-T01` | U01 first START_CONSULTATION | one canonical event + Consultation + original Run; no U07 binding required |
| `FOUND-T02` | U01 same-event/idempotency replay | original Event and Run IDs, no second CDP/Run |
| `FOUND-T03` | U01 same ID/key changed payload | identity conflict, no new rows |
| `FOUND-T04` | two concurrent original Event writes same key | at most one committed winner, loser rolls back/reconciles; no false-positive transaction success |
| `FOUND-T05` | force save/FLUSH uniqueness exception after caller began outer transaction | demonstrate actual Spring rollback-only and safe separate readback; no hidden successful outer commit |
| `FOUND-T06` | U07 Foundation FLUSH then binding INSERT failure | after rollback fresh transaction sees **zero new rows** in both tables, no F8 |
| `FOUND-T07` | U07 binding FLUSH then injected crash before COMMIT | same all-or-none rollback |
| `FOUND-T08` | U07 COMMIT success but HTTP response lost | original canonical+binding owner-readback, one F8 first decision at most |
| `FOUND-T09` | U07 COMMIT_UNKNOWN | query both rows with same global key and scope; defer if incoherent; zero unauthorized effects |
| `FOUND-T10` | preexisting committed Foundation row without U07 binding | quarantine, no inferred bytes/second effect |
| `FOUND-T11` | same key with alias Event ID and same protected binding | return original winning canonical ID; never overwrite original `occurred_at` |
| `FOUND-T12` | alias ID collides with another canonical PK | identity conflict, not key-only reattachment |
| `FOUND-T13` | same key, changed Question/wait/tenant/actor or answer digest | fail-closed identity/authorization conflict |
| `FOUND-T14` | MySQL vs Oracle V7 from existing deployed V6 | migrations, FK/unique, bytes, rollback-only, isolation evidence on each dialect |
| `FOUND-T15` | missing trusted U06 issuance despite projected hash | RDP-01 positive admission blocked; zero F8 |
| `FOUND-T16` | one consumer still has incompatible Foundation digest/key expectation | audit gate fails; owner amendment required |

Each case must bind **independent expected Oracle, synthetic fixture, exact source HEAD, actual transaction manager/DB identity, statement isolation, before/after SQL readback and attempted F8/P02/P01/U02 effect counters**. JUnit+Mockito shape alone is not authoritative evidence of real SQL rollback. Existing U06 verification green cannot establish U07 or Foundation transaction safety.

## 7. Controlled amendment dependency and re-review gates

```text
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED

BF-U07-FOUND-AUD-01 = DESIGN_REMEDIATION_PROPOSED / NOT_CLOSED
BF-U07-FOUND-AUD-02 = DESIGN_REMEDIATION_PROPOSED / NOT_CLOSED
BF-U07-FOUND-AUD-03 = INVENTORY_PARTIAL / NOT_CLOSED
BF-U07-FOUND-AUD-04 = TEST_PLAN_ONLY / NOT_CLOSED

CA-U07-FOUNDATION-COLLISION-TX-SAFETY-01
  = PROPOSED_IF_SHARED_LEDGER_CHANGE_REQUIRED / NOT_AUTHORIZED

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01
  → exact consumer inventory independent review
  → transaction remediation design independent review
  → owner-specific controlled amendment decision if shared Foundation changes needed
  → separately authorized physical implementation and MySQL/Oracle tests
  → independent exact-head evidence review
  → Foundation audit closure re-evaluation

CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-AGG-ADMISSION-T17-ORACLE-01 = DESIGN_ACCEPTED / NOT_AUTHORIZED
U07 Aggregate Compatibility = CONDITIONALLY_ACCEPTED_DESIGN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

## 8. Independent design review questions

- `IR-FOUND-01`: Is the source evidence differentiated from proposed new components and full inventory never falsely asserted?
- `IR-FOUND-02`: Is U01 `@Transactional start()` and original Run identity preserved, with other consumers not preemptively excluded?
- `IR-FOUND-03`: Does collision/flush/commit UNKNOWN correctly leave a potentially poisoned transaction **before** new-transaction readback?
- `IR-FOUND-04`: Does one real transaction own BOTH canonical and U07 binding writes, and is F8 barred until physical commit?
- `IR-FOUND-05`: Is original key winner/alias ID precedence exact and safe against cross-tenant or changed binding?
- `IR-FOUND-06`: Does a pre-commit crash produce zero rows, whereas a preexisting committed orphan is quarantined, never inferred?
- `IR-FOUND-07`: Are MySQL/Oracle migrations additive and existing Foundation V2/V3 plus U06 V6 retained?
- `IR-FOUND-08`: Is the complete `git grep` consumer manifest an explicit, independently reviewed **future blocking artifact**?
- `IR-FOUND-09`: Is optional shared Foundation hardening separately authorized and does no U07-only design grant permission to edit U01?
- `IR-FOUND-10`: Does no design-level acceptance become a physical audit PASS or U07 implementation permission?

**Next step:** `U07 Foundation Targeted Source Inventory / Transaction Remediation Independent Design Review` at the exact amended HEAD. Then execute the full source inventory under a separately accepted audit procedure and evaluate whether shared Foundation owner changes require a dedicated controlled amendment. No tests were run and no source file was modified for this design.
