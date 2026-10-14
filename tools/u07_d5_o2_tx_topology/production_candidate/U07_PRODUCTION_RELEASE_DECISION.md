# U07 Production Deployment Evidence & Release Gate Closure

**Review baseline:** `b927b1c8a7019884129013be85b37afd404f18cd` (post-PR #501 main).
**Decision:** **BLOCK** production migration, clinical authority activation and any real-patient delivery.
**Meaning:** offline release-gate evidence intake is implemented; production release is *not* approved.

## Verified from repository / CI (not from a deployed environment)

| Gate | Evidence | Decision |
|---|---|---|
| Historical Flyway protection | Default MySQL/Oracle V1–V6 unchanged; V7–V9 only separately staged candidates; existing preflight reports V1 MySQL CLOB compatibility and no deployed `flyway_schema_history` | PASS for preservation; BLOCK for rollout |
| Physical schema rehearsal | PR #501 MySQL8 disposable database ran V9 and 19/19 integration tests; no historical production clone | PASS nonproduction; BLOCK production |
| Oracle | V9 candidate column/table structural comparison only | BLOCK physical Oracle deployment |
| Host controls | Spring host config disabled by default, `prod` profile excluded, no permissive trust services created | PASS nonproduction safety |
| Durable delivery / WAIT | Restart/replay tests use disposable CDP and injected authority; real durable waiting host / authenticated recipient evidence absent | BLOCK |
| F3 Owner / Barrier / Revalidation / A1 | Immutable evidence and independent verification interfaces tested; production-signed issuers, revocation and route authorization evidence absent | BLOCK |
| Deployment permissions, operations and clinical signoff | No authenticated environment inventory, DBA authorization, staged rollback or independent clinical safety signoff in repo | BLOCK |

## Current missing external artifacts (intake requirements)

1. Exact deployed platform, environment, database version and tenant ownership with authenticated provenance.
2. **Read-only** `flyway_schema_history` export and V1–V6 checksums + live schema inventory, reviewed by DBA (no repair/migrate in evidence collection).
3. Release sequence V7/V8/V9, least-privilege grants and production-like rollback / crash recovery rehearsal.
4. Real durable Consultation/WAIT/Checkpoint and recipient-confirmation proof under repeated, concurrent and restart operations.
5. Independently signed clinical Owner/Safety/Revalidation/Route authority, validity and revocation evidence; trace and incident provenance.
6. DBA, Security/SRE and clinical safety approvals; exact deployment target, health checks and rollback ownership.
7. If Oracle is in scope, physical Oracle DB verification rather than structural-parity SQL.

## Executable and auditable intake

Run offline:

```bash
python3 -m unittest discover -s tools/u07_d5_o2_tx_topology/production_candidate -p 'test_u07_release_gate.py' -v
python3 tools/u07_d5_o2_tx_topology/production_candidate/u07_release_gate.py \
  --manifest /secure/read-only/u07-evidence.json \
  --evidence-root /secure/read-only/u07-evidence \
  --out /tmp/u07-release-gate-result.json
```

Manifest contains `source_main_sha`, `platform` (`mysql8`, `oracle`, or `dual`), and an `evidence` map, whose keys match the gate's `REQUIRED` names. Each entry holds a relative file `path` and `sha256` digest; avoid including patient records or credentials. Missing or altered submissions return nonzero status.

**The script is intentionally incapable of returning RELEASE_AUTHORIZED.** Hash matching demonstrates only submitted-file integrity; identity, electronic signatures, environment authenticity and clinical authorization require independent validation. Do not promote candidates or enable live transports based on this file.

## Closure decision

- **Repository and isolated integration deliverables:** completed for this scope, subject to merge/CI on the exact new HEAD.
- **External production evidence intake:** missing; no deployed database was accessed and no authenticated environment approval was received.
- **Production release gate:** **BLOCK / NOT AUTHORIZED**.
- **Next action:** supply authorized read-only deployment evidence through approved channels; independently re-review baseline SHA, migration compatibility, host trust and clinical signoffs. No direct production migration, Flyway repair or dispatch is authorized here.
