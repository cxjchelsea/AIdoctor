# U07 D3 — Transaction / Safety Independent-Style Review v0.1

**Target**: PR #368, exact `f77693f5e0c6383518d394c0e5ad532e6ad59527`; based on PR #367.  
**Review date**: 2026-10-09. **Review method**: independent critical inspection of source and original JDBC 18-assertion CI, **not** organizationally independent review or production attestation.

## Verdict

- **D3 SYNTHETIC HAPPY PATH = EVIDENCE PASS** (Java 8 / disposable MySQL: 18 assertions).
- **D3 CONCURRENCY + RECOVERY = NOT_READY**, blockers below.
- **CLINICAL F8 / REAL INGEST = NOT_AUTHORIZED**. Test-only coordinator and synthetic namespace must not become production service.
- **MERGE = NOT_AUTHORIZED**.

## Critical findings

| Finding | Severity | Exact behavior / proof gap | Required D4 action |
|---|---|---|---|
| D3-IR-01 | BLOCKER | Consultation currentness is checked **before** reading an existing canonical/app record. A committed accepted event replayed after consultation leaves `WAITING_USER` will return `REJECTED_CURRENTNESS`, not stable replay, despite durable acceptance. Current CI only replays while wait remains unchanged. | Confirm historical identity first under an unambiguous lock order, preserve currentness gate for **new** admission; test replay after state version/phase advances |
| D3-IR-02 | BLOCKER | `SELECT canonical_business_event ... WHERE event_id=? OR idempotency_key=? FOR UPDATE` on an absent row does not serialize all possible conflicting fresh inserts. Unique constraint losers throw SQL; fresh transaction readback/reconciliation policy not implemented. | Multi-connection race tests: same identity, same idempotency alias, different payload. Distinguish UNIQUE race from deadlock/timeouts and retry only under explicit bounded policy |
| D3-IR-03 | BLOCKER | DB connection is caller-provided and required `autoCommit=false`, but the API claims unused transaction without proving isolation or absence of unrelated writes. The coordinator `commit()` can inadvertently commit caller's work; it may run on `READ COMMITTED`/other isolation without validation. | Enforce safe ownership/isolation in a test-scoped transaction factory; test no unrelated writes and connection poisoning / rollback behavior |
| D3-IR-04 | BLOCKER | D3 only tests normal commits and caught exceptions. Process/connection failure before/after commit, ambiguous commit acknowledgement, partial canonical/application evidence and restart reconciliation are unverified. | Fault injection at pre-insert, post-canonical, post-application, post-CAS pre-commit, after commit; fresh-connection readback in each |
| D3-IR-05 | REQUIRED | Actual F8 semantics are deliberately absent: `ACCEPTED` is unconditional after positive currentness, not a clinical rule decision; the synthetic namespace check is only a test guard, not ingress source attestation. | No production connection; keep `SYNTHETIC_TEST_ONLY` and no APPLIED/outbox, design authoritative F8 separately |
| D3-IR-06 | REQUIRED | MySQL-only, non-Flyway V2/V3/V6/V7 synthetic fixture; Oracle dialect, migrations V1–V7 original install, multi-process owner/leases are not verified. | Preserve D1 deployment blockers and split Oracle/production verification into later gates |

## D4 bounded target

D4-A: implement **read-only crash/restart reconciliation** for synthetic canonical+application, fail-closed on identity or digest divergence; no implicit APPLIED. D4-B: isolated MySQL multi-connection test with serialization and rollback/ambiguous ack. D4-C: targeted remediation of D3-IR-01 in a separate PR or commit requiring exact fresh proof, not hidden in review. D4-D: explicitly document unsupported Oracle/full-release/clinical paths.

## Constraints

No clinical/P02/U02 runtime wiring, real database, PHI, side-effect dispatch, published migration alteration, Flyway repair/baseline or merge without express authorization.  
Do not label a synthetic positive ACCEPTED as authoritative F8. Existing 18-assertion CI cannot close crash/concurrency gates.
