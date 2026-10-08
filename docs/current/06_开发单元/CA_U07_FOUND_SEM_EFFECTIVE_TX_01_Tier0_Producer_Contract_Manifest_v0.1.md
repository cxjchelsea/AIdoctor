# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Producer Contract Manifest v0.1

> Exact authorized decision: [PR #304](https://github.com/cxjchelsea/AIdoctor/pull/304), `fd820541a6a94993c7e698661d7d01c3fd5f74de`.
> Baseline scanned source: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`, tree `1bfe776f76c986d4e6199a9b820f2cc20773a181`.
> Producer: **TARGETED_REMEDIATION_COMPLETE / READY_FOR_TARGETED_INDEPENDENT_IMPLEMENTATION_AND_EVIDENCE_RE_REVIEW**; this is not independent closure or merge authorization.
> Findings source: [PR #306](https://github.com/cxjchelsea/AIdoctor/pull/306), BF-U07-FOUND-T0-IMPL-01/02, RF-U07-FOUND-T0-IMPL-01/02.

## 1. Exact source and evidence provenance

Consumes the pre-existing exact-head read-only full tracked-source scan from PR #297 / #298: GitHub Actions `37736999966`, artifact `11531528615`, `u07-foundation-exact-head-source-inventory`. The three files in that ZIP are `foundation-exact-head-inventory.json`, `foundation-matches.tsv`, `foundation-summary.md`. Producer checks the input archive SHA-256 and JSON SHA-256 for output traceability, validates inventory `source_head`, `tree_sha`, 2,112 tracked paths, 2,063 text and 49 binary classifications, zero skipped, 256 matches, unique paths and all match-to-source SHA256 / Git blob structure.

**Independent trust anchor (checked before parsing):** archive SHA-256 `0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089` and archive-contained JSON SHA-256 `b28d72868833a8c109938025a6077dd6799924e89e31a34572c4d186f5bcf077` are frozen from PR #298 and validated before accepting caller-controlled metadata. Repacked archives or modified unrelated entries fail closed even when self-reported HEAD/tree/counts remain correct.

The original CI artifact is the immutable source of file inventory **only**. It does not contain complete Java source bytes. Therefore the producer additionally **requires caller-supplied snapshots of six exact-main Java sources**, at the same relative paths shown below, and independently validates content SHA-256 from the inventory and Git blob SHA when indexed. It does not fetch or synthesize source text or silently substitute `latest` branch.

- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/U01ConsultationService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/CanonicalBusinessEventLedger.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/service/cdp/CDPManager.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/service/cdp/CDPVersionService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeBindingService.java`
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/ClinicalRunCoordinator.java`

## 2. Authorized implementation inventory

| ID | Only authorized path | Purpose |
|---|---|---|
| D0-01 | `tools/u07_foundation_tx_topology/static_projection.py` | Offline ZIP/snapshot parser, declared source-level graph, fail closed on unavailable or ambiguous exact source |
| D0-02 | `tools/u07_foundation_tx_topology/static_schema.py` | Typed `U07EffectiveSpringTxTopologyV1-TIER0` SOURCE_ONLY contract, SHA/determinism, explicit UNKNOWN |
| D0-03 | `tools/u07_foundation_tx_topology/test_static_projection.py` | stdlib synthetic positive and negative tests; no business application or external system |
| D0-04 | This Markdown manifest | Preserve exact authority, input/output, Oracle, evidence and limits |

No changes to production Java, migrations, POM, Spring config, previous Foundation scan program or CI workflows. No Tier-1 code, Java/Spring runtime execution, database/network probes, PHI, real patients, PROFILE-A or production.

## 3. CLI and response contract

Offline invocation **after manually providing the authorized local inputs**:

```bash
python tools/u07_foundation_tx_topology/static_projection.py \
  --archive /isolated-evidence/u07-foundation-exact-head-inventory.zip \
  --snapshots-dir /isolated-source-snapshots \
  --artifact-dir /isolated-evidence-out
```

The caller-provided snapshot directory mirrors the six `diagnosis-service/src/main/java/...` paths. The program does not clone, run Git, open sockets or connect to a database. `--output` is a **pre-existing isolated artifact directory**, separate from snapshots, the producer directory and the input archive's directory. The fixed output is `tier0-source-projection.json` inside this directory; existing output is not overwritten and symlinked boundaries are rejected. Producer prints only the `SOURCE_ONLY` projection's SHA-256.

Output: canonical sorted UTF-8 JSON with `schema`, `status=SOURCE_ONLY`, `source_head`, `source_tree`, `input_archive_sha256`, `input_inventory_sha256`, verified `sources` list (`source_path`, SHA-256, Git blob), declared `nodes`, declared `edges` with paths/line numbers, explicit `effective_manager=UNKNOWN`, `entity_manager_factory=UNKNOWN`, `datasource=UNKNOWN`, `spring_context_executed=false`, `database_access=false`, and `limitations`. Proposed U07 admission/binding nodes = `NOT_IMPLEMENTED`. No positive configured-manager, proxy identity, transaction owner, JDBC enlistment, actual COMMIT or U07 atomicity statements.

Graph includes source-declared paths:

```text
U01ConsultationService → CanonicalBusinessEventLedger
U01ConsultationService → CDPManager → CDPVersionService
U01ConsultationService → RuntimeBindingService
U01ConsultationService → ConsultationRepository.save
U01ConsultationService → ClinicalRunCoordinator
```

Each Java source is inspected for its known method's declared `@Transactional` annotation only; this does **not** mean that advice is applied. If the exact call expression is absent, repeated, or altered, producer returns `FAIL_CLOSED` rather than fabricating a relation. Dynamic/reflection/external consumer coverage remains `UNKNOWN` even after lexical inventory completeness passes.

## 4. Independent Oracle / fixture and negative proof requirements

A **separately reviewed exact-source snapshot package** is needed for accepting output against actual main; its source bytes cannot be inferred from lexical matches. Synthetic unit fixtures in D0-03 are not independent truth for actual Spring behavior. Required independent Oracle cases: U01 Event and Run original identity; transitive CDPVersion; U01 Consultation save; absent future U07 beans; source SHA/tree mismatch; duplicate/missing source path, source bytes/blob mismatch, missing/duplicate edge, malicious positive-manager field, deterministic output, no external I/O, redaction and output overwrite refusal.

## 5. Validation, status and governance

**Targeted remediation validation (local mirrored code, not yet independently SHA-reconciled exact GitHub HEAD):** `python -m unittest -v test_static_projection` = **11 passed**. The two new forged-input tests use the unfalsified frozen digest; the missing/duplicate-edge cases repack a self-consistent synthetic inventory with updated source SHA-256/Git blob and assert the *semantic* edge error rather than a generic hash failure. The real PR #297 archive SHA-256, extracted JSON SHA-256 and 2,112-entry inventory schema also validate locally. Python syntax validation succeeded. These local results are **not** asserted as an exact-GitHub-HEAD CI test run, and do not independently close RF-U07-FOUND-T0-IMPL-01. These results **do not** verify an actual six-snapshot output until those independent raw sources are supplied and checked. Any missing source snapshot must remain a hard failure. No CI workflow is created in this implementation scope.

```text
Tier0 candidate = TARGETED_REMEDIATED_FOR_INDEPENDENT_RE_REVIEW
BF-U07-FOUND-T0-IMPL-01 = REMEDIATED_FOR_RE_REVIEW / NOT_CLOSED
BF-U07-FOUND-T0-IMPL-02 = REMEDIATED_FOR_RE_REVIEW / NOT_CLOSED
RF-U07-FOUND-T0-IMPL-01 = OPEN / EXACT_GITHUB_HEAD_TEST_EVIDENCE_PENDING
RF-U07-FOUND-T0-IMPL-02 = OPEN / REAL_SIX_SOURCE_PROJECTION_PENDING
Tier0 evidence schema = SOURCE_ONLY
Tier0 exact-main full six-snapshot projection = NOT_EXECUTED / INPUT_SNAPSHOTS_NOT_PACKAGED
Tier0 independent implementation review = PENDING
Tier0 implementation closure / merge authorization = NOT_GRANTED
Tier1 Spring context / manager runtime evidence = NOT_AUTHORIZED / NOT_EXECUTED
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI = BLOCKED
```
