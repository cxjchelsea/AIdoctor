# U07 Foundation Reference Audit — Targeted Source Inventory and Transaction Remediation Design v0.1

> Date: 2026-10-08
> Design source HEAD: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Prior failed source audit: [PR #293](https://github.com/cxjchelsea/AIdoctor/pull/293) @ `680c69cbca97db468db8f0371248b0199173a4af`
> U07 aggregate author design: `c319c67e65a0e620e547d03514cbe45e25020975`; independent review [PR #291](https://github.com/cxjchelsea/AIdoctor/pull/291)
> **STATUS = TARGETED_REMEDIATION_CANDIDATE / READY_FOR_TARGETED_INDEPENDENT_RE_REVIEW / NOT_FROZEN**
> Review triggering change: [PR #295](https://github.com/cxjchelsea/AIdoctor/pull/295), exact prior design HEAD `f7eb2a2662596aff009874fff62f7cb74e5586fe`, verdict REVISE_REQUIRED (DIR-01 and DIR-02).
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

**Selected single U07 transaction owner: `U07AdmissionApplicationService` as a non-transactional trusted ingress bean.** It explicitly rejects any caller with `TransactionSynchronizationManager.isActualTransactionActive() == true` before doing *any* U07 canonical write. Do not suspend, join or nest an ambient U01/other owner's transaction as a supported V1 entrance. A separate injected `PlatformTransactionManager` **whose exact identity and DataSource equal the Foundation canonical Ledger + U07 Binding repositories** controls an explicit `TransactionTemplate(PROPAGATION_REQUIRES_NEW)`; there are no interchangeable REQUIRED/proxied alternatives. Physical identity and Spring wiring must be independently proven before implementation authorization.

Selected lifecycle (**design only — new components do not exist yet**):

~~~text
Trusted U07 ingress (not @Transactional)
  -> reject ambient actual transaction; no F8/DB write/HTTP success
  -> verify PROFILE-B principal, U07 type, global key namespace and payload
  -> U07AdmissionApplicationService (non-transactional)
       -> U07AdmissionWriteTemplate(PROPAGATION_REQUIRES_NEW, PINNED_TX_MANAGER)
            -> assert same DataSource/EntityManager/transaction manager for BOTH tables
            -> CanonicalBusinessEventLedger.resolveOrCreate() [REQUIRED joins this write tx]
            -> U07BindingRepository.insertOrCompare() [same write tx]
            -> flush canonical + binding (still NOT COMMITTED)
            -> return internal attempt result (NEVER ADMITTED_FOR_F8)
       -> TransactionTemplate.execute() returns only after OWNED COMMIT completed
       -> U07OwnerCommittedReadbackService (distinct proxied REQUIRES_NEW read)
            -> query exact original event identity and committed U07 binding
            -> compare owner scope/type/key/fingerprints and postcommit origin
       -> NEW_COMMITTED_ORIGINAL_EVENT_AND_BINDING
          OR REATTACHED_TO_PREEXISTING_COMMITTED_WINNER
          OR typed failure/reconciliation
  -> only now ADMITTED_FOR_F8 for NEW_COMMITTED result
  -> reattachment returns original-root replay status; NEVER a new F8 decision
~~~

**Any** `DataIntegrityViolationException`, deferred FLUSH/COMMIT failure, `UnexpectedRollbackException`, rollback-only flag or inability to attest physical COMMIT invalidates the write attempt as success. The outer application service catches **only after** the template's write transaction has completed/failed and calls a separate proxied independent read transaction. There is no winner query within a poisoned transactional persistence context. The existing shared Ledger catch may still occur internally; this protocol neither assumes it is safe nor claims to fix U01. `REQUIRES_NEW` is explicit, and the ingress **rejects ambient transactions** rather than depending on suspended caller work. The readback must be after `execute()` completion; external callbacks may not issue F8 within the transaction. Return to caller after COMMIT but before successful readback => `RECONCILIATION_REQUIRED`, not success.

**Original-vs-replay provenance:** `NEW_COMMITTED_ORIGINAL_EVENT_AND_BINDING` requires a first-insert marker/canonical owner receipt **from this exact successfully committed transaction**, plus a new-transaction readback of the same canonical ID and binding. `REATTACHED_TO_PREEXISTING_COMMITTED_WINNER` requires previously committed winner identity/binding with exact type and namespace; no new transaction-local insert marker may be used to classify it as NEW. A late postcommit callback error or lost HTTP ACK does not rewind the DB fact; query the same original key and identity to return a replay-safe confirmed result, or `RECONCILIATION_REQUIRED` if the read cannot establish it. An unrelated historical row with matching answer content never proves this write committed.

### 3.1 Transactional requirements and no-success-on-rollback guarantee

- Use **one pinned** actual `PlatformTransactionManager`, `DataSource` and JPA EntityManager binding for Foundation row and U07 side binding; the non-transactional U07 ingress **rejects** `TransactionSynchronizationManager.isActualTransactionActive()` before doing work. The sole selected V1 write controller is explicit `TransactionTemplate(PROPAGATION_REQUIRES_NEW)`, not an arbitrary `@Transactional(REQUIRED)` worker. Inspect real Spring proxy/bean wiring and physical manager/data source equality before implementation.
- **No alternatives:** a non-transactional `U07AdmissionApplicationService` controls and awaits its own `TransactionTemplate.execute()` physical COMMIT; the callback may return only an internal attempt record, never `ADMITTED_FOR_F8`. A separate proxied `REQUIRES_NEW` readback then proves committed owner state before first F8. No F8, HTTP success or downstream side effect may be emitted from an inner callback, `@Transactional(REQUIRED)` participant, `TransactionSynchronization.afterCommit` callback or unverified async notification.
- `repository.save` can defer SQL error until FLUSH/COMMIT. Explicit bounded FLUSH can expose errors earlier but **does not** authorize positive result before COMMIT. If transaction is rollback-only or throws `UnexpectedRollbackException`, never return `ADMITTED_FOR_F8`.
- Collision is **not** automatically a lost race: incompatible payload/scope/Question/actor must return `IDENTITY_CONFLICT`. Winner lookup compares Foundation `consultation_id, event_type, key, payload_digest` and complete U07 binding fingerprint, not merely global key.
- For `same key + different event ID`, original Foundation `event_id` is the winner; later input ID is an **alias**, not another canonical event. `eventId` PK conflicting with a different key must be classified as identity conflict, **not** recovered by blindly selecting a key winner.
- After a uniqueness exception within the existing Ledger, a same-call catch/re-read may execute inside a rollback-only transaction. U07 may invoke that method within a transaction only if outer completion is monitored and failed commit results in an independent, isolated recovery. If source-level or dialect testing shows the shared catch masks a failure, prepare a **separate controlled Foundation amendment**, not an unreviewed U07 workaround that ignores rollback.
- Winner readback must be invoked by **a separate Spring proxy / independent `REQUIRES_NEW` transaction** after the owned template exits. Ambient caller transactions are **rejected before U07 writes**, not suspended as a supported V1 path. Attempting U07 ingress from `U01ConsultationService.start()` or another `@Transactional(REQUIRED)` method must yield `BLOCKED_AMBIENT_TRANSACTION` and zero U07 writes/F8. U01's ordinary ledger usage remains completely unchanged.
- A competing writer may still be uncommitted when lookup runs. `NO_WINNER` after a race does **not** mean safe permission to create a different canonical identity; reconcile with bounded same-key retry and eventual owner-confirmed terminal response.
- A committed original Foundation row with missing U07 binding is **quarantined**; do not manufacture binding bytes from a retry even if the caller supplies the same digest.
- For `UNKNOWN COMMIT`, query both tables under a new authoritative transaction by the **same original scoped key/event ID** before taking any action. An upstream HTTP acknowledgement lost after confirmed commit returns the original identity, not a second entry.
- All exceptions and rollback observations must produce bounded non-PHI structured error codes and zero F8/P02/P01/U02 physical attempts until an authoritative identity result.

### 3.2 Decision table

| Original write outcome | After failed/uncertain scope exits | Permitted caller result | Prohibited |
|---|---|---|---|
| **this attempt** successfully performs first insert + owned COMMIT + both owner rows exact | distinct postcommit new-transaction readback and committed insert origin | `NEW_COMMITTED_ORIGINAL_EVENT_AND_BINDING` → `ADMITTED_FOR_F8` | success based on `save()`/callback return, or a different historical row |
| duplicate key, failed FLUSH/rollback-only, or preexisting committed winner | after failed template exits, independent original winner + full U07 binding/type/key equal | `REATTACHED_TO_PREEXISTING_COMMITTED_WINNER` (original F8 verdict only) | reuse poisoned EntityManager or create a new F8 decision |
| duplicate key, different U07 binding | verified mismatch | `IDENTITY_CONFLICT` | new key to hide collision |
| exception after Foundation flush before binding | both rows absent on authoritative readback | `FAILED_ROLLED_BACK`; bounded original-identity retry under approved policy | quarantine a nonexistent ghost row |
| exception after binding flush but before COMMIT | both absent if rollback | `FAILED_ROLLED_BACK` | positive event acknowledgement |
| unknown COMMIT / no coherent winner yet | bounded authoritative readback | `RECONCILIATION_REQUIRED` | speculative fresh event or F8 |
| previously committed Foundation winner, binding missing | owner confirms committed legacy orphan | `QUARANTINED_LEGACY_ORPHAN` | fabricate old answer bytes |
| same event ID already bound to another global key | original row conflict | `IDENTITY_CONFLICT` | reassign event ID |

### 3.2.1 Commit timeline, authority and ambient call disposition

| Step | Physical transaction state | Permitted result |
|---|---|---|
| T0 trusted U07 entry, check ambient active Spring transaction | **must be NONE** | `BLOCKED_AMBIENT_TRANSACTION` if any, with no U07 write |
| T1 start selected pinned `REQUIRES_NEW` template | actual U07-owned write tx ACTIVE | internal attempt only, no F8 |
| T2 Foundation insert/lookup and side-binding insert/compare; flush | same write tx ACTIVE (can become rollback-only) | internal attempt only, never business success |
| T3 template exits, physical owned COMMIT returns normally | U07 write tx COMMITTED | may proceed to independent readback |
| T3-F commit exception, unexpected rollback or COMMIT UNKNOWN | failed/unknown write tx has exited | classify `RECONCILIATION_REQUIRED`, or reconcile original winner without new event |
| T4 independent proxied `REQUIRES_NEW` owner read | separate committed read snapshot | verify event type + key + scope + original ID + binding digest |
| T5 origin receipt proves this first insert committed AND T4 exact | no active U07 write tx | `NEW_COMMITTED_ORIGINAL_EVENT_AND_BINDING` → first F8 candidate |
| T5-R winner predates this attempt, T4 exact | no active U07 write tx | `REATTACHED_TO_PREEXISTING_COMMITTED_WINNER`, reuse original verdict |
| T5-U T4 absent/incoherent, callback/ACK failure, unknown COMMIT | no positive owner proof | `RECONCILIATION_REQUIRED`, zero novel F8/P02/P01/U02 |

A Spring manager configuration that cannot guarantee the T3 transaction is the **physical outermost U07 write transaction** (including wrong manager or caller interception bypass) fails capability/readiness; no optimistic fallback. T3-F may find an exact winner committed by another worker; this is still **reattachment**, not proof this attempt committed. A postcommit readback failure must not mark a known successful DB COMMIT as ROLLED_BACK; the operational result remains UNKNOWN pending reconciliation.

### 3.3 Separation of U07-only addition versus shared Foundation amendment

**Phase A / U07-specific only:** new additive binding schema, U07 admission coordinator and entirely new readback service under separately authorized implementation scope. **No** modification to shared `CanonicalBusinessEventLedger.resolveOrCreate` signature, `payload_digest` field, global unique index, or U01 flow.

**Phase B / conditional shared hardening:** if exhaustive source inventory or actual JPA race tests demonstrate that `CanonicalBusinessEventLedger` or `RuntimeBindingService` can return a false-positive success or a poisoned transaction to existing callers, formally propose `CA-U07-FOUNDATION-COLLISION-TX-SAFETY-01` and obtain Foundation + U01 owner authorization and independent impact review **before** editing those services. Do not automatically refactor U01 from a U07 review. Distinguish the project-wide defect remediation from U07-specific admission design and give it its own independent tests.

### 3.4 Committed-orphan owner applicability (BF-U07-FOUND-DIR-02)

**Frozen event-type/namespace authority:** `U07_APPLICABLE_EVENT_TYPES = {USER_ANSWER, RESUME_REQUEST}`. A U07 storage key must be generated using RDP-01 §4.3's exact trusted, globally scoped `u07-` + SHA-256 key format (key prefix **alone** is not proof of authorization). Only an **already durably committed Foundation event** whose `event_type` is one of these U07 types, whose original event ID, consultation and `idempotency_key` match the trusted U07 owner scope and canonical payload digest, and whose expected immutable U07 side-binding is absent, qualifies as `QUARANTINED_LEGACY_U07_ORPHAN`.

Order of authoritative owner checks **before any missing-binding classification**:
1. Read Foundation row by incoming event ID. If present but `event_type` is `START_CONSULTATION` or any non-U07 type, reject this U07 request as `IDENTITY_CONFLICT_NON_U07_OWNER`; **never** insert/repair U07 binding on that row.
2. Resolve original Foundation winner by verified scoped U07 storage key, compare event ID precedence and original-vs-alias relations. Any incoming ID already bound to a *different* existing Foundation row is `IDENTITY_CONFLICT` even if key lookup finds a legitimate U07 winner.
3. Verify Foundation `event_type` is U07-applicable, canonical `consultation_id`, original globally scoped key, trusted actor/profile/tenant owner provenance and `payload_digest`; any mismatch => `IDENTITY_CONFLICT` or `BLOCKED_AUTHORIZATION` without mutation.
4. Only now query exact committed U07 binding; if present and complete matching fingerprint, return replay or first-commit confirmation according to §3.2.1. If missing after a **previously committed U07 winner** was established, quarantine; if this transaction rolled back and no winner exists, `FAILED_ROLLED_BACK` / bounded reconciliation, **never** orphan.
5. A legitimate U01 `START_CONSULTATION` Foundation record lacking U07 binding is **NORMAL_FOR_U01** for U01 reads; the same record requested through U07 is a typed cross-owner identity conflict. No U07 orphan alert or repair.

| Committed Foundation row | Requested context | U07 Binding | Required outcome |
|---|---|---|---|
| `START_CONSULTATION` (U01) | U01 original/read | ABSENT | `NORMAL_FOR_U01`, unchanged original Event/Run |
| `START_CONSULTATION` (U01) | incoming U07 ID/alias references U01 ID | ABSENT | `IDENTITY_CONFLICT_NON_U07_OWNER`; no U07 row/F8 |
| `USER_ANSWER` or `RESUME_REQUEST`, scoped key valid | incoming U07 original or alias matches | PRESENT and exact | `REATTACHED_TO_PREEXISTING_COMMITTED_WINNER` unless this attempt has proven own first COMMIT |
| U07 event, scoped key valid, previously committed owner winner | incoming U07 same original ID/key | ABSENT | `QUARANTINED_LEGACY_U07_ORPHAN`, no new answer bytes or F8 |
| U07 event, incoming ID points to another Foundation PK | U07 alias/key claims different original | any | `IDENTITY_CONFLICT`, no repair |
| U07 event, tenant/consultation/payload/actor ownership fails | incoming U07 | any | `IDENTITY_CONFLICT` / `BLOCKED_AUTHORIZATION`, no replay |
| no committed winner after pre-COMMIT failure | incoming U07 retry | ABSENT | `FAILED_ROLLED_BACK` or `RECONCILIATION_REQUIRED`, never quarantine |

**U07 legacy-orphan classification is not a global Foundation invariant.** `START_CONSULTATION` and other lawful non-U07 event types have no U07 Binding requirement. The database FK enforces that a Binding cannot point to a nonexistent Foundation row, but **does not** imply that every Foundation row must have a Binding. Owner-scoped positive U07 binding/readback logic is a separate application invariant.

## 4. BF-02 atomic Foundation/U07 binding V7 design

Logical immutable table `u07_canonical_event_binding`, schema/migration **candidate** for two dialects:

| Column / constraint | Requirement |
|---|---|
| `canonical_event_id` | PK **and FK** to `canonical_business_event.event_id`; original winner identity only; FK is one-directional, **not** a requirement that U01/non-U07 Foundation records possess bindings |
| `storage_idempotency_key` | global U07-scoped key; compared against original Foundation winner |
| `contract_version`, `binding_schema_version` | frozen RDP-01 formats, not inferred from caller version |
| `event_type`, `consultation_id` | strictly `USER_ANSWER` or `RESUME_REQUEST` for U07; exact committed Foundation event_type/consultation + trusted authenticated scope; no U01 `START_CONSULTATION` U07 binding |
| `binding_fingerprint` | 64-char versioned SHA-256 over RDP-01's **21 exact protected fields** |
| `binding_bytes` and `synthetic_answer_bytes` | in-row immutable bytes, PROFILE-B synthetic only, typed NULL answer on RESUME_REQUEST |
| `answer_payload_digest`, `original_event_ref`, `target_answer_event_id` | original trusted payload and target contract; no invented new answer on RESUME_REQUEST |
| original U06 wait / Question/Thread/Run/version/actor/scope/authority refs | exact provenance, immutable, no client self-authority |
| `received_at`, `occurred_at`, minimal audit ref | original persisted timestamps; no alias overwrite |

Write order under the **sole selected physically controlled U07 write transaction** (§3.2.1): reject ambient transaction → validate trusted U07 type/scope → derive canonical bytes/digests/key → pinned `TransactionTemplate(REQUIRES_NEW)` writes Foundation and binding → FLUSH → physically owned COMMIT → separate authoritative readback → owner/type/origin classification → only new proven commit can enter first F8. No U07 Binding is required for existing `START_CONSULTATION`/non-U07 events. No separate `CANONICAL_BINDING_PENDING` success state, out-of-DB answer store or cross-database best-effort fallback.

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
| `FOUND-T10` | independently seeded **preexisting committed U07 USER_ANSWER/RESUME_REQUEST** under valid U07 scope/key without binding | `QUARANTINED_LEGACY_U07_ORPHAN`, no inferred answer bytes/new F8 |
| `FOUND-T11` | same trusted U07 scoped key with alias Event ID and same protected binding | original U07 winning canonical ID, replay original verdict, no second event; never overwrite `occurred_at` |
| `FOUND-T12` | incoming U07 alias ID collides with another canonical PK (including U01 `START_CONSULTATION`) | `IDENTITY_CONFLICT` or `IDENTITY_CONFLICT_NON_U07_OWNER`, never key-only reattachment or U07 repair on the U01 record |
| `FOUND-T13` | same key, changed Question/wait/tenant/actor or answer digest | fail-closed identity/authorization conflict |
| `FOUND-T14` | MySQL vs Oracle V7 from existing deployed V6 | migrations, FK/unique, bytes, rollback-only, isolation evidence on each dialect |
| `FOUND-T15` | missing trusted U06 issuance despite projected hash | RDP-01 positive admission blocked; zero F8 |
| `FOUND-T16` | one consumer still has incompatible Foundation digest/key expectation | audit gate fails; owner amendment required |
| `FOUND-T17` | legitimate committed U01 `START_CONSULTATION` with no U07 binding is read by U01 | `NORMAL_FOR_U01`, original event/Run unchanged; no U07 orphan alert |
| `FOUND-T18` | U07 event ID matches existing U01 event, while a matching U07 key alias might also exist | non-U07 event-ID precedence → `IDENTITY_CONFLICT_NON_U07_OWNER`, zero new U07 binding/F8 |
| `FOUND-T19` | U07 entry is called under ambient `@Transactional(REQUIRED)` (including U01-style caller) | `BLOCKED_AMBIENT_TRANSACTION`, zero U07 writes, F8 or success |
| `FOUND-T20` | callback returns but owned transaction COMMIT delayed/throws `UnexpectedRollbackException` | no ADMITTED/F8 before physical outer COMMIT; independent readback only after write scope exit |
| `FOUND-T21` | prior committed U07 winner exists while new unrelated attempt fails COMMIT | reattach only if original owner key/scope/type/fingerprint match; never classify new attempt as committed from unrelated row |
| `FOUND-T22` | commit succeeded but postcommit readback or acknowledgement fails | reconcile same original identity; no premature claimed ROLLED_BACK, no speculative fresh event/F8 |

Each case must bind **independent expected Oracle, synthetic fixture, exact source HEAD, actual transaction manager/DB identity, statement isolation, before/after SQL readback and attempted F8/P02/P01/U02 effect counters**. JUnit+Mockito shape alone is not authoritative evidence of real SQL rollback. Existing U06 verification green cannot establish U07 or Foundation transaction safety.

### 6.1 Additional mandatory design-oracle partition

`FOUND-T01..22` are **unexecuted design requirements**; all `FOUND-T17..22` supplement, not replace, the initial 16. Independent Oracle and Fixture must bind an actual U01 owner event for T17, separate U07 origin/key policy for T18, a genuinely active ambient transaction for T19, a test-controlled commit boundary for T20, independently committed historical winner for T21 and a postcommit failure injector for T22. Only T10 contains a verified preexisting committed *U07-owned* missing-binding orphan. Real MySQL and Oracle negative-effect owner readbacks are future requirements.

## 7. Controlled amendment dependency and re-review gates

```text
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED

BF-U07-FOUND-DIR-01 = REMEDIATED_FOR_RE_REVIEW / NOT_CLOSED
BF-U07-FOUND-DIR-02 = REMEDIATED_FOR_RE_REVIEW / NOT_CLOSED

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

**Next step:** `U07 Foundation Targeted Independent Design Re-Review` at the exact amended HEAD. Then execute the full source inventory under a separately accepted audit procedure and evaluate whether shared Foundation owner changes require a dedicated controlled amendment. No tests were run and no source file was modified for this design.
