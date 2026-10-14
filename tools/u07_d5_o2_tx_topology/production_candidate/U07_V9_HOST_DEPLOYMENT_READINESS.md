# U07 deployment readiness — V9 clinical receipts and host wiring

**Decision at this baseline: BLOCK production activation and production Flyway migration.**
This is an executable non-production MySQL 8 integration candidate, not a deployment approval.

## 1. Existing immutable baseline

- Default MySQL/Oracle Flyway scans remain at V1–V6. The MySQL V1 legacy CLOB
  compatibility blocker and unknown deployed `flyway_schema_history` have not
  been waived. Isolated V7/V8 recovery/outbox candidates remain external.
- The candidate V9 files in `mysql8/` and `oracle/` are **not** auto-scanned.
  Never copy/rename them into live `db/migration` without a separately approved
  historical checksum, release ordering and rollback plan.
- The MySQL8 CI creates a disposable schema, explicitly applies V9, and runs
  CDP / delivery / WAIT / revalidation and A1 readback tests. Oracle has
  structural-parity SQL only; no Oracle physical execution evidence is claimed.

## 2. Opt-in host dependencies

`U07DurableClinicalHostIntegrationConfig` requires
`aidoctor.u07.host-integration.nonproduction-enabled=true` and a non-production
Spring profile. It **only** provides assembly of existing components when the
host supplies all required beans:

- `CDPRepository`, `JdbcTemplate`, `TransactionTemplate` and the governed
  `StateCommitter` with real permission, consent, source, idempotency and audit
  ports.
- `U07NonProductionDeliveryWaitRecovery.DeliveryVerifier` with independent,
  authenticated *already-confirmed* delivery receipt verification. No outbound
  patient sender may be created by this configuration.
- Durable `ConsultationWaitTransitionService` and `U06WaitCoordinator`,
  backed by approved consultation, effect, thread and checkpoint repositories.
  These repositories must support exact replay and crash recovery.
- `U07TrustedF3RevalidationWriter.IndependentAuthority` with independently
  authenticated F3 Owner, post-F3 barrier and revalidation/routing evidence.
  It is not automatically derived from the U06 Mode-3 readback result.
- The existing `U07PersistedF3OwnerBarrierRevalidationEvidence.TrustedEvidenceVerifier`
  remains a *separate* A1 trust check, including on every authority read.

No such trust/transport/real clinical issuer is supplied by this candidate.

## 3. Release blockers / evidence intake

Production deployment remains **BLOCK** until all of these are documented:

1. Authenticated deployed MySQL/Oracle version, actual historical
   `flyway_schema_history`, V1–V6 checksum and physical table comparison.
2. DBA-approved migration release sequence including V7/V8/V9 compatibility,
   privileges and rollback rehearsal on a representative clone.
3. Physical Oracle verification if Oracle deployment is in scope.
4. Non-fake delivery confirmation and durable WAIT repositories exercised
   under power-loss, duplicate messages, concurrent requests and restart.
5. Signed, independently verifiable Owner/Barrier/Revalidation/Route authority;
   identity binding, key rotation, revocation and trace-to-incident evidence.
6. Approved deployment/environment configuration, explicit rollback and
   live observability; no real patients until clinical safety sign-off.

**Nonproduction E2E success is not a production issuer, migration approval, or
authorization to dispatch side effects.**
