# D5-O2 — Historical Flyway V1 compatibility and release decision

## Observed, not assumed
- Frozen MySQL V1 fails on disposable real MySQL 8 under Flyway 7.
- In PR #476, only a **temporary copy** replacing nine `CLOB` type tokens with `LONGTEXT` permits V1–V6 migration and validation; **do not alter already-applied V1**.
- Candidate V7/V8 from #473/#474 are **outside** automatically scanned Flyway locations. Their isolated tests do not establish real historical deployment compatibility.
- PR #475 was merged on 2026-10-13; its merge does **not** authorize production recovery or external sending. Production recovery/sending remain disabled pending independent physical and deployment authority.

## Two mutually exclusive release paths

### A. Existing database with Flyway history
1. Database owner exports `flyway_schema_history` **read-only** using a credential allowed to SELECT only. Restrict and redact server details and identifiers; do not check raw production extracts into Git.
2. Independently obtain database-owner-approved expected V1–V6 checksum manifest from authenticated change/deployment records. Do **not** derive trusted checksums from the current candidate code or invent missing ones.
3. Run `verify_flyway_history.py` on the exported JSON and approved manifest in the authorized environment. A BLOCK result is terminal; `ALLOW_REVIEW_ONLY` is **not permission to migrate**. Its `source` value is a declaration, not cryptographic provenance; verify the export chain separately.
4. Compare actual table/column/index shape and Flyway 7 settings against owner records; take tested backup/restore evidence and rehearsed cutover. If checksum/history diverges, **no automatic repair, baseline, out-of-order migration, or rewriting applied V1**.
5. After independent DBA review, test a staged V7/V8 rehearsal on an actual sanitized backup with exact prior schema history, then request a separate deployment authorization.

### B. Fresh database with no Flyway history
1. The unmodified V1 is currently incompatible with the tested MySQL 8 install path.
2. Do **not** reinterpret an empty history as permission to `baselineVersion=6`: this would skip required V1–V6 domain tables.
3. Design and independently validate a separately versioned **new-install-only** MySQL 8 baseline or controlled installation package, preserving the immutable historical migrations and ownership of its Flyway history. Keep it outside the existing default locations until authorized.
4. Validate fresh-install full schema, upgrades, rollback plan and restrictions before authorization.

## Database/platform matrix

| Environment | Proven evidence | Release posture |
|---|---|---|
| MySQL 8, original V1 | FAIL from fresh schema in real MySQL8/Flyway7 | BLOCK |
| MySQL 8, temporary V1 compatibility copy | V1–V6 PASS on disposable test database | diagnostic only |
| MySQL 8, synthetic V6 baseline + V7/V8 | candidate DDL PASS on disposable test database | not existing-history upgrade proof |
| MySQL 5.7 legacy database | no authorized schema-history snapshot | BLOCK |
| Oracle | structural candidate DDL, MySQL-specific CAS Java SQL | BLOCK |

## Explicit non-goals
This preflight cannot confirm an exported artifact came from the expected server, that a manifest is genuinely signed by the database owner, or that production schema matches history. It cannot apply migrations, issue `Flyway.repair`, authorize V7/V8, schedule recovery tasks, send to patients, or lift `DENIED_NO_DISPATCH`. Authentication, backups, DBA change control and production authorization are external hard requirements.

## Required closeout artifacts
- Authenticated inventory of all deployment cohorts (empty / history V1–V6 / divergent / Oracle)
- DBA read-only history proof + expected checksums, provenance and drift diff
- Independent compatibility disposition for fresh installs and existing installs
- Tested deploy/rollback/restore and schema permissions
- Main-HEAD physical CI evidence
- Separate written approval for production migration, recovery scheduling and any external effects

## U07 Workpack A — current evidence and explicit production blockers (2026-10-14)

- Merged PRs #487–#491 establish **disposable MySQL 8** CDP persistence, exact U05 readback and isolated U04/U05 Spring-host service E2E. These tests use synthetic scopes, policies and A1/F3 fixtures; they are **not** full-stack production authority evidence.
- Opt-in real-JPA Safety Gate readback and U04→A1→U05 bridge assembly only consume an **independently supplied** A1 authority port. Absence of independently deployed and verified F3 commitment, post-F3 safety barrier, revalidation and route authorization remains a **production BLOCK**.
- Historical Flyway preflight only assesses exported owner-approved lineage and never changes schemas. No authenticated cohort-by-cohort V1–V6 database-owner history/checksum or physical schema drift evidence was provided during this PR; no migration authorization.
- Oracle physical migrations and dialect-specific recovery/CAS remain **BLOCK**. A MySQL 8 disposable database is not evidence of Oracle runtime compatibility.
- **Closure decision: NOT READY FOR PRODUCTION.** Next necessary evidence is independently attested actual deployment DB and A1 issuer/authority integration, then complete production-host rehearsal and explicit authorization; no synthetic fixture may substitute for owner evidence.
