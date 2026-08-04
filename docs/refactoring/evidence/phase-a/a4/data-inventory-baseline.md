# Phase A4 Data Inventory baseline

## Scope and status

- Execution date: `2026-08-03` (Asia/Shanghai).
- Repository: `cxjchelsea/AIdoctor`.
- Base and initial Head: `f67fe427caa0f398f40fe85b7f0e452d7c0bc0b1`.
- Base branch: `agent/enterprise-agent-refactoring-plan`.
- Work branch: `agent/phase-a4-data-inventory`.
- Overall status: `PARTIALLY_VALIDATED`.
- Scope: repository data stores, schemas, access paths, classification, lifecycle and drift. No schema, entity, repository, key, CDP, business code, dependency, migration or runtime data was changed.

The task named `企业级临床Agent目标架构.md` and `企业级临床Agent分阶段实施路线.md`, but those paths do not exist at this Base. The canonical README instead links `总架构与模块设计.md` and `可执行实施路线.md`; those files and `企业级临床agent重构主方案.md` were read as the current equivalents. The missing names are recorded as documentation drift, not treated as present.

## Evidence levels

Only the project-approved levels are used:

| Level | A4 meaning |
|---|---|
| `DOCUMENTED` | A statement or target exists only in documentation. |
| `CODE_CONFIRMED` | Source, configuration, migration or tracked-path evidence was located. It does not prove a live store or valid data. |
| `DATA_VERIFIED` | Limited structural inspection of named tracked repository data files succeeded. It does not prove clinical correctness, production data, runtime ingestion or release approval. |
| `BLOCKED` | A concrete missing service, client, safe environment or configuration prevents deeper verification. |
| `UNKNOWN` | No direct evidence supports a stronger conclusion. |
| `PARTIALLY_VALIDATED` | Static inventory and limited tracked-file parsing succeeded while live stores and production data remain unverified. |

A4 does not add `BUILD_VERIFIED`, `RUNTIME_VERIFIED` or `TEST_VERIFIED` claims. It does not upgrade any A1-A3 build, runtime or test conclusion.

## Method and safety boundary

The inventory recursively covered tracked Java, Python, TypeScript, SQL, YAML, JSON, CSV, Compose, Docker, monitoring, knowledge-source and historical research assets. It inherited A1 Java state-write evidence, A2 Python CDP/storage evidence and A3 frontend/Compose evidence.

Representative commands:

```text
git fetch --prune origin
git status --short
git worktree list --porcelain
git ls-files
git grep -n/-l for Entity, Repository, SQL, Redis, Neo4j, Milvus, upload, file and log access
PowerShell Import-Csv / ConvertFrom-Json / Get-FileHash
python -c json.loads(...) for the large HPO JSON after PowerShell parser limits
git diff --check
```

No MySQL, Oracle, Redis, Neo4j, Milvus, model provider or production endpoint was contacted. No container was started, no migration was executed, no database was generated, and no existing data was read or changed outside tracked repository files.

## Inventory summary

| Inventory | Rows | Summary |
|---|---:|---|
| [data-store-inventory.csv](./data-store-inventory.csv) | 19 | Two relational representations, Redis plus memory fallback, Neo4j, blocked Milvus, files, browser token storage and monitoring stores. |
| [data-entity-schema-inventory.csv](./data-entity-schema-inventory.csv) | 19 | Thirteen Java entity representations covering 11 distinct table names plus Redis, Neo4j, Milvus, book/file and browser schemas. |
| [data-access-path-inventory.csv](./data-access-path-inventory.csv) | 45 | 21 rows with reads, 28 with writes/proposals and 5 with deletes; a row may contain more than one operation. |
| [data-lifecycle-governance-inventory.csv](./data-lifecycle-governance-inventory.csv) | 20 | Retention, deletion, withdrawal, backup, restore, lineage and release evidence or explicit gaps. |
| [data-schema-drift-inventory.csv](./data-schema-drift-inventory.csv) | 16 | Dialect/profile, migration, entity, CDP, identity, Trace, Redis, graph, vector, file and monitoring drift. |
| [data-verified-asset-manifest.csv](./data-verified-asset-manifest.csv) | 13 | Exact tracked files with byte size, parse result, structural shape and SHA256; no group-level validation. |

## Data stores

### Relational data

- Eleven distinct JPA table names were identified: `cdp`, `cdp_version`, `diagnosis_record`, `examination_plan`, `examination_record`, `follow_up_plan`, `health_state_assessment_record`, `wellness_screening_record`, `agent_state`, `audit_trail` and `execution_trace`.
- The repository contains 13 entity classes because `examination_plan` and `examination_record` are duplicated across diagnosis and examination services.
- The clinical logical store contains 12 Entity representations across 10 distinct table names. `execution_trace` is the thirteenth Entity representation and eleventh distinct table name, inventoried as a separate logical store.
- Oracle migrations define diagnosis and Trace tables; a separate MySQL-style migration defines AgentState/AuditTrail. The visible MySQL examination profile enables `hibernate.ddl-auto=update`.
- No foreign keys or unique business constraints were located. Indexes exist for common patient/session/status/time access, but duplicate entities and migrations disagree.
- Database URLs/profiles are inconsistent or incomplete. Live schemas and rows were not inspected; relational evidence is `CODE_CONFIRMED`, not `DATA_VERIFIED`.

### Redis

- The only confirmed business Redis access is dialog context: `dialog:context:{cdp_id}` with JSON values, database 0 and `SETEX` TTL 1800 seconds.
- Redis read/write failure silently switches to an in-process dictionary with no TTL or persistence. This changes authority, retention and recovery semantics.
- Diagnosis and clinical parsing expose Redis settings without a confirmed active client path. Root Compose persists Redis to `redis_data` and exposes it without authentication.

### Neo4j and knowledge graph

- Diagnosis code reads `Symptom` to `Disease` paths by CUI/name and returns empty results on query failure.
- Knowledge management defines 18 expected node labels, 21 relationship types, 17 uniqueness constraints and 9 property indexes.
- The schema endpoint can create constraints/indexes, but the four graph importers and validator are TODO stubs that report success with zero imported rows.
- No graph connection, node, relationship, constraint or index data was inspected. Patient/medical graph isolation, provenance, version, deletion, withdrawal and release state remain `BLOCKED`/`UNKNOWN`.

### Milvus/vector

- Root Compose passes `MILVUS_HOST=milvus` and port `19530` to diagnosis engine.
- Neither Compose file defines a Milvus service, and no `pymilvus` or other active client was found.
- Collection, dimension, index, metric, embedding model, metadata, source/version and patient/tenant namespace are all unknown. The store is `BLOCKED`, not `DATA_VERIFIED`.

### Files, logs and monitoring

- Examination uploads are written under `uploads/reports/{userId}` and the path is stored in the relational record. No delete, retention or authorization path was located; file and DB writes are not atomic.
- OCR reads uploaded bytes in memory and returns raw/structured data. Input validation and downstream retention are not established.
- Python/common logging uses rotating `logs/app.log` files (10 MB plus five backups); no tracked runtime log was present. Promtail mounts 12 service log directories.
- Prometheus declares 30-day retention. Loki uses filesystem storage with retention processing enabled but no explicit retention period. Grafana, Prometheus, Loki and Promtail use named volumes.
- Promtail writes positions to `/tmp/positions.yaml`, while Compose persists `/var/lib/promtail`; the volume does not cover the configured cursor path.
- Tracked medical source assets include 35 files under the source-data directory (309,765,464 bytes) and 30 derived files (202,532,716 bytes), plus other large medical documents. Runtime use, licensing and release governance remain unverified.

## Entity, schema and contract findings

- CDP is a large mixed PHI-capable aggregate with 11 JSON/CLOB-like payloads and an integer version. Version snapshots serialize the full CDP and may contain PHI in a real runtime.
- Caller-provided expected version is absent. `CDPManager` relies on a pessimistic lock and server-side increment; Python readers/direct writers do not enforce the reference version carried by some ToolContext variants.
- `examination_record` differs across services: only the examination-service representation has `ocr_status`, while the Oracle migration lacks it.
- CDP uses string patient IDs; follow-up, wellness and health-state records use numeric patient IDs. No foreign keys enforce linkage.
- Technical Trace appears in a relational table, a CDP CLOB summary and frontend DTO/stream variants, creating ownership and retention drift.
- The book registry is both a tracked knowledge file and mutable runtime status store.

## Access, concurrency and consistency

- Confirmed CDP writers include Manager/controller endpoints, start/continue orchestration, five fixed Workflow steps, Trace callback, replay/rollback, dialog direct POST, explanation direct POST and eight suggested-write producers.
- Eight Python services have near-duplicate CDP HTTP readers that return an empty dictionary on failure.
- Broad Map/dict payloads, missing caller expected-version and multiple direct/proposed writers create stale-write and unmapped-field risk.
- Fixed Workflow failures can continue with empty/default values and can mark status completed after partial downstream failure, as inherited from A1 evidence.
- File upload and relational persistence have no atomic boundary. Redis and memory fallback are not reconciled. Neo4j import success does not prove data was written.
- Source-level authentication/authorization is unknown for CDP, history, Trace, schema and book administration paths. Frontend-admin clients attach no authentication; the patient client optionally reads a bearer token from `localStorage`.

## Data classification

- `PHI-capable`/`PHI candidate`: CDP, diagnosis records, examination reports/OCR, dialog context, version snapshots, audit and Trace input/output structures may contain PHI in a real runtime. This classification does not assert that tracked repository files contain real patient data.
- `PII`: patient/user/family/session identifiers; no high-confidence real identity number, phone or patient identity fixture was found in tracked text.
- `AUTHENTICATION`/`CREDENTIAL`: browser token storage and development credential configuration. Environment variable names are classified as `SECRET_REFERENCE`, not as secrets.
- `AUDIT_TRACE`: AuditTrail, execution Trace, CDP trace summary, application logs and monitoring streams.
- `MEDICAL_KNOWLEDGE`/`SAFETY_RULE`: normalization/rule files, HPO/ICPC/ICD/book extraction assets, Neo4j schema and DRKnows research files.
- The repository has no demonstrated tenant boundary, consent record, legal hold, anonymization or patient-data withdrawal mechanism.

## Tracked-data structural verification

The following checks are file-structure evidence only:

- Six normalization CSVs parsed with 8-10 data rows each; the ambiguity rules JSON parsed.
- The HPO Chinese mapping CSV parsed with 18,988 rows.
- The ICPC CSV parsed with 726 rows and 18 columns; its JSON counterpart parsed.
- The large HPO JSON parsed as a five-key top-level object with the standard JSON parser.
- A representative extracted book outline JSON parsed.
- DRKnows relations parsed with 107 rows, but at least one header is missing and PowerShell assigned a placeholder column name.

Exactly 13 named files receive limited per-file `DATA_VERIFIED`; their full paths, tracked status, byte sizes, parser results, structures and SHA256 values are recorded in [data-verified-asset-manifest.csv](./data-verified-asset-manifest.csv). Every other tracked file and every directory or asset group remains `CODE_CONFIRMED` at most.

The counts of 35 tracked source-data files and 30 tracked derived files establish path presence and aggregate size only. They do not mean that every file in either group was parsed, hashed or individually `DATA_VERIFIED`.

The 13 per-file checks validate only file existence, Git tracked status, byte size, format parsing, structural shape or row/column counts, and SHA256. They do not validate completeness, clinical accuracy, knowledge validity, provenance, licensing, production data, runtime ingestion, graph/vector contents, release approval or patient outcomes.

## Secret and patient-data scan

- High-confidence private key, AWS key, GitHub token, JWT, Chinese identity number and mainland mobile patterns: zero files.
- Four OpenAI-style lexical matches are the same hyphenated bibliography phrase already reviewed in A3, not credentials.
- Email matches are confined to medical publication/book metadata, not patient records.
- Hard-coded `password` values in Compose and default settings are development defaults and are recorded as security risks; they are not asserted to be production secrets.
- No actual secret or real patient record was found. Static scanning is not a complete security or privacy audit.

## Lifecycle and governance gaps

- Retention is unknown for clinical relational data, snapshots, Redis persisted content, uploads and most knowledge files.
- Deletion is partial: CDP hard delete and Redis key delete exist, but cascade completeness, audit, consent withdrawal and clinical record policy are absent.
- Backup/restore is represented mainly by named volumes or Git history; no restore procedure or drill is verified.
- Knowledge provenance, license, source checksum, extraction tool/prompt/model version, clinical reviewer, release binding and withdrawal are incomplete.
- Trace/log PHI filtering and access controls are unknown.
- No production data lineage, migration reconciliation or schema drift report is stored at runtime.

## Risks

### BLOCKED_FOR_LIVE_DATA_VERIFICATION

- No safe, identified non-production live datastore was available. Consequently, live database rows, current Redis keys/TTLs, Neo4j contents and other runtime data cannot receive `DATA_VERIFIED`.
- This blocks live-data verification only; it is not a merge blocker for this static A4 Evidence baseline.

### CURRENT_CAPABILITY_GAPS

- Milvus has environment references only: no Compose service, client, collection, index, dimension, metric or namespace implementation was found.
- This is `NOT_IMPLEMENTED` as a descriptive current capability gap, not a formal Evidence Level and not an A4 static-baseline merge blocker. Inventory fields continue to use only the project-approved `CODE_CONFIRMED`, `UNKNOWN` or `BLOCKED` values according to field semantics.

### HIGH

- CDP has multiple direct/proposed writers without caller expected-version, idempotency or a single commit boundary.
- PHI-capable data may be duplicated across CDP, snapshots, AuditTrail, execution Trace, logs, uploads and frontend raw result paths in a real runtime without proven authorization/retention controls.
- Relational profile/migration/entity drift can create environment-specific schemas, especially `examination_record.ocr_status` and Oracle/MySQL JSON/CLOB differences.
- Patient and medical graph/vector isolation is not demonstrated.

### MEDIUM

- Redis failure silently changes to unbounded non-durable memory state.
- Knowledge schema/import APIs can report success while importer stubs write zero rows.
- Book status rewrites a tracked JSON source file without version or audit control.
- Promtail positions are configured outside the persisted volume.

### LOW

- The two task-specified architecture/roadmap filenames have drifted from canonical README navigation.
- DRKnows relation CSV has an unnamed header.

## A5 Shared Contracts inputs

A5 must consume, without implementing them in A4:

1. canonical patient, user, family, session and CDP identifiers;
2. Encounter/CDP version and `expected_version` conflict semantics;
3. `StatePatch`/`CommitResult` with idempotency, writer identity and audit linkage;
4. canonical CDP field names, aliases, JSON types and unmapped-field preservation;
5. examination report metadata, upload/OCR state and source-artifact identity;
6. Trace/Audit/ClinicalDecision separation and PHI-safe projections;
7. knowledge source/version/release/withdrawal references;
8. vector collection metadata and patient/medical namespace boundary;
9. retention/delete/withdrawal status representations;
10. patient-safe delivery allowlist rather than raw CDP/result payloads.

## Remaining UNKNOWN and unverified scope

- Live database tables, indexes, constraints, row counts, null/invalid JSON profiles and version distributions.
- Redis keys/TTLs currently stored, Neo4j nodes/relations/properties, Milvus collections/indexes and monitoring data.
- Production credentials, topology, tenant/consent boundaries, backups, restores, deletion jobs and legal retention.
- Clinical correctness, provenance, licensing and release approval of rule/knowledge files.
- End-to-end data writes, transaction behavior and authorization enforcement.

## Follow-up boundaries

- A5 owns contracts only; it must not silently repair data or introduce a State Committer beyond its approved scope.
- Schema migrations, data reconciliation, Redis/graph/vector decisions, PHI governance, backup/restore and live data profiling require separately authorized, isolated tasks.
- A4 did not start A5, repair A1-A3 findings, modify business code, or access production systems.
