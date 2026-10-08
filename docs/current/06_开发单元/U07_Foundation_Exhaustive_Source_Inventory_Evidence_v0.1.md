# GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 — Exhaustive Exact-Head Source Inventory / Consumer Compatibility Evidence v0.1

> Date: 2026-10-08
> Target actual source: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546` — **not** the new review-branch HEAD.
> Independent design acceptance baseline: [PR #296](https://github.com/cxjchelsea/AIdoctor/pull/296), `b8be89aea705bbdf7490cf02d9801ed96248a12d`
> Earlier failed Foundation audit: [PR #293](https://github.com/cxjchelsea/AIdoctor/pull/293)
> **EVIDENCE COLLECTION CANDIDATE / GATE NOT_PASSED / NOT A COMPLETE REVIEWED CONSUMER MANIFEST.**
> The audit tooling and GitHub Actions workflow are not implementation/runtime code. PROFILE-A, PHI, real patients and production remain prohibited.

## 1. What evidence exists and what is not yet proven

The GitHub REST `git/trees/<exact_sha>?recursive=1` response at the target source was `truncated=false`, exposing **2,634 entries**. Of these, **340 are diagnosis-service production Java files**, and **464 are repository-wide Java files**; a tree listing alone does not enumerate references inside those files. GitHub code search at earlier audit returned `incomplete_results=true` even for known literal `CanonicalBusinessEventLedger` and cannot establish all-consumer absence. A direct Git clone from this assistant's container failed due to unavailable network/DNS resolution. Therefore **no full tracked-file content scan result can be honestly claimed from the interactive environment**.

An auditable, non-mutating full-repository tracked-file lexical scan is now supplied as code:

- `tools/u07_foundation_audit/exhaustive_inventory.py`
- `.github/workflows/u07-foundation-exact-head-inventory.yml`

The workflow checks out the **audit-tool PR HEAD** and separately checks out the **exact immutable source SHA**; it scans source files only under the latter. It must fail if `git rev-parse HEAD` differs, if the tracked tree is dirty, or if a tracked file cannot be read. It enumerates `git ls-files -z`, reads every readable tracked regular file, classifies binary files, regex-scans all text lines for Foundation and transaction references, records `path:line:term:Git blob SHA` for matches, writes original source file SHA-256 and exact HEAD, produces JSON/TSV/summary and per-output SHA-256 digests, and uploads the artifacts to a nonproduction CI job for independent review.

This tool does **not** by itself establish semantic completeness for reflection, generated runtime calls, external database producers, dynamic SQL or all indirect paths. The job running alone is **NOT** `GATE PASS`: an independent reviewer must reconcile those residuals and inspect each candidate's actual transaction manager, caller, owner permissions and impact.

## 2. Known independently observed direct consumers and transaction surfaces at immutable source HEAD

| Concrete inspected source | Confirmed relationship | Compatibility obligation |
|---|---|---|
| `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/U01ConsultationService.java` | `@Transactional start()` calls `CanonicalBusinessEventLedger.resolveOrCreate(...,"START_CONSULTATION",...)` then `CDPManager.createCDP`, `RuntimeBindingService.bind`, Consultation save and `ClinicalRunCoordinator.openRun` | retain U01 original Event/Run and replay semantics; all writes' commit identity must be verified before claiming U01 atomic regression |
| `.../runtime/foundation/CanonicalBusinessEventLedger.java` | `@Transactional resolveOrCreate`; `findById` then `findByIdempotencyKey`, `save` and `catch DataIntegrityViolationException` with same-call winner reread | deferred SQL uniqueness and potential rollback-only; never assume caught exception clears tx or `save` returned COMMIT |
| `.../runtime/foundation/CanonicalBusinessEventRepository.java` | Spring Data JPA `findByIdempotencyKey`, PK event ID | winner lookup and globally unique key are Foundation facts, not U07 per-wait validity |
| `.../runtime/foundation/CanonicalBusinessEventRecord.java` | PK `event_id`, unique `idempotency_key`, event_type + payload_digest + consultation | preserve Foundation payload-digest semantics and U01 `START_CONSULTATION` without mandatory U07 Binding |
| `.../runtime/foundation/RuntimeBindingService.java` | `@Transactional bind()` catches unique-key violation then rereads in same call | adjacent possible rollback-only hazard; separately authorized U01/Foundation remediation if proven |
| `.../runtime/foundation/ClinicalRunCoordinator.java` | `@Transactional openRun()` uses `CanonicalBusinessEventRepository.findById`, `requireOriginalRun` by consultation/event | no replacement of original canonical ID and no fresh Run on U07 alias replay |
| `.../runtime/u06/wait/U06WaitCoordinator.java` | coordinates metadata Wait/Thread transition and returns projected `eligibilityId` | **not** original U06 durable issuance; separate `CA-U06-U07-ELIG-ISSUANCE-01` still blocked |
| MySQL `db/migration/V2__create_clinical_runtime_foundation.sql` | original Foundation Event PK and global unique idempotency | additive V7 only; no historic schema rewrite |
| Oracle `db/migration-oracle/V3__create_clinical_runtime_foundation.sql` | equivalent canonical event PK/unique | physical dual-dialect verification remains missing |
| `src/test/.../foundation/FoundationRuntimeBaseTest.java` | mocked transport replay and event-identity conflicts | not real JPA or DB rollback-only proof |
| `src/test/.../u01/U01ConsultationServiceTest.java` | mocks Ledger and verifies `START_CONSULTATION` and original-run control | guard regression test; real transaction execution still required |

These are **directly inspected, bounded observations**, not yet an independently validated exhaustive manifest; absence of a hit in this table does not establish a negative source fact.

## 3. Required evidence schema and acceptance procedure

When CI has run, require all following for an independent inventory review:

1. `source_head` matches `6d4fd787...` and `tree_sha` matches GitHub's immutable tree; all tracked source path counts, file digests, binary classifications and zero skipped tracked files verified.
2. `foundation-exact-head-inventory.json` lists every scanned tracked file, pattern counts and every matching `path:line:term:blob SHA`; TSV and summary SHA-256 match uploaded evidence metadata. CI run URL, workflow HEAD, runner version and artifact ID must be recorded.
3. Review *every first-order* Foundation consumer (class, repository, event table, `resolveOrCreate`, key/unique), identify direct writer/reader/test/schema/config; then group broad `@Transactional`/DataSource/EntityManager matches by dependency from Foundation rather than claiming all transaction annotations are direct consumers.
4. Independently follow transitive U01, CDP and ClinicalRun chains; inspect custom Spring proxy/bean wiring, `@Transactional` interceptibility, effective Spring profile, `PlatformTransactionManager`, actual `DataSource` identity and JPA flush/commit behavior. List `UNKNOWN` or dynamic adapters and require owner confirmation.
5. Freeze a canonical per-consumer audit matrix: `path:line:source_blob:role:owner:call_boundary:tx_manager:DDL/key/digest_assumptions:behavior_before:behavior_after:independent_evidence_ref:disposition`. Consumer `UNKNOWN` or missing head binding => **AUDIT_INCOMPLETE**, not PASS.
6. Distinguish three gates: `SOURCE_INVENTORY_COMPLETE` (all tracked lexical scan, independently reviewed), `TRANSACTION_COMPATIBILITY_ACCEPTED` (owner/proxy/readback applicability and explicit unsafe assumptions), and `FOUNDATION_PHYSICAL_EVIDENCE` (future authorized MySQL/Oracle transaction tests). This evidence-candidate PR alone does not mark any passed.

### Negative scenarios that must be retained

- U01 `START_CONSULTATION` without U07 Binding remains normal; original Run is unchanged.
- U07 `USER_ANSWER` or `RESUME_REQUEST` original event, scoped key and binding commit together.
- A U07 alias pointing to existing U01 Event ID is typed `IDENTITY_CONFLICT_NON_U07_OWNER`, no U07 repair.
- Under active outer `@Transactional(REQUIRED)`, U07 ingress returns `BLOCKED_AMBIENT_TRANSACTION` before write/F8.
- Foundation INSERT/FLUSH followed by U07 Binding failure before outer COMMIT rolls both back; no fabricated orphan.
- A genuinely preexisting committed U07 row missing binding is quarantined, not inferred from request bytes.
- Unique-key collision/rollback-only does not permit same-context positive success; any independent `REQUIRES_NEW` owner readback happens after failing write scope exits.
- MySQL/Oracle dual-dialect original V2/V3 and U06 V6 physical compatibility must be shown independently after authorization.

## 4. Current evidence status and allowed next gate

```text
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01
= NOT_PASSED / FULL_SCAN_EVIDENCE_PENDING

Repository tracked-source inventory runner = CODE_CANDIDATE / NOT_YET_VERIFIED_RUN
Immutable source target = main@6d4fd787600e3a57f01f3e17893e6d98893ac546
Artifact source/CI result = NOT_YET_OBSERVED
Independent per-consumer compatibility evaluation = NOT_PERFORMED

BF-U07-FOUND-AUD-01 = OPEN / COLLISION_TX_SAFETY_NOT_PHYSICALLY_PROVEN
BF-U07-FOUND-AUD-02 = OPEN / U07_ATOMIC_BINDING_NOT_IMPLEMENTED
BF-U07-FOUND-AUD-03 = OPEN / EXHAUSTIVE_REFERENCE_EVIDENCE_PENDING
BF-U07-FOUND-AUD-04 = OPEN / MYSQL_ORACLE_PHYSICAL_EVIDENCE_PENDING

U07 Foundation targeted design = CONDITIONAL_DESIGN_ACCEPTANCE
U07 Aggregate Compatibility = CONDITIONALLY_ACCEPTED_DESIGN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next action after the workflow emits verified artifacts:** `U07 Foundation Exhaustive Source Inventory — Independent Evidence Review`. If no CI artifacts appear, the scan has **not** run and must not be reported as completed; resolve workflow execution or run the script in a controlled exact-HEAD checkout, then ingest actual raw manifests with integrity hashes. Do not substitute this planned report for observations.

This PR only adds an auditable source-inventory runner, read-only evidence workflow, and a source-backed bounded inventory report. No Foundation/runtime code, U07 migrations, clinic endpoints, patient/PHI traffic, tests or merge were performed by the assistant.
