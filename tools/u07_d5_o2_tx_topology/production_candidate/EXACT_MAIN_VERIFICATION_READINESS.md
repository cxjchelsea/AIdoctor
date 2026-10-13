# D5-O2 — Combined Main-Tree Physical Verification and Legacy Migration Readiness

## Scope and evidence boundary

Pinned integrated main SHA: `8bc9c4c288aa65748daed2b0e5e7012c577883bd`, resulting from standard-merging #476, #477, #475.

The dedicated isolated GitHub Actions workflow **checks out that exact existing main SHA**, even though the workflow is introduced by a separate Draft PR. The exact-sha guard fails if checkout resolves differently. This proves the integrated *main code tree* under disposable Java8/MySQL8; it does NOT mean GitHub registered the check run against the main commit SHA. The workflow execution run belongs to its PR, and verification of later main HEAD requires a new pinned run.

### Physical evidence contract

- Java 8 / Spring Boot / JDBC / MySQL 8 disposable, no patient data or production credentials.
- U06 six-table replay + unknown-commit readback + bounded whole-TX retry.
- Persistent CAS recovery, lease generation and scheduler fence.
- Atomic Recovery Claim + outbox DENIED_NO_DISPATCH.
- Explicit synthetic pre-commit intake with object-reinstantiation crash simulation, not an OS-level process restart.
- Historical Flyway7 MySQL8 evidence: original V1 failure is **expected negative**; candidate V7/V8 from artificially baselined V6 is not proof of real historical upgrade.
- Legacy compatibility diagnosis: original V1 fails, temporary nine-token CLOB->LONGTEXT V1 copy migrates V1..V6 without modifying frozen source.
- Twelve offline owner-checksum and source-claim policy adversarial checks.
- Verify exactly 42 JUnit cases without skip/error/failure and explicit negative migration labels. Exact baseline and required gate signals must appear in run log.

### Production migration remediation readiness decision

**MIGRATION_RELEASE: BLOCKED.** The raw, unmodified V1 still fails on MySQL8. Existing `flyway_schema_history` check-sums and authentic deployment cohort inventories have **not** been obtained. A temporary modified-copy checksum would not match a database that applied original V1. The approved manifest checker returns *ALLOW_REVIEW_ONLY* even on a perfect match, not deploy authority.

For *existing deployments*: obtain independently approved, minimally redacted read-only `flyway_schema_history` evidence and compare it with signed/owner-attested version hashes, actual table definitions, server version, and Flyway configuration. Verify the actual sanitized backup can upgrade without rewriting immutable V1 or blind `repair`. Route diverged histories to separate controlled remediation decisions.

For *fresh MySQL8 installations*: do not baseline at V6 on empty databases; that skips the six foundational migrations. Design a new-install-only compatible package with full expected V1..V6 domain schema and a separate, explicitly authorized migration lineage. Continue testing V7/V8 only in disposable clones.

For *Oracle*: existing Java CAS is MySQL-specific and Oracle migration DDL remains **unexecuted**. Keep blocked until an actual Oracle JDBC migration, dialect adaptation and rollback verification.

### Minimum gates before closing D5-O2

1. All integrated main-tree physical tests succeed on an exact checked-out SHA; independently audit check-run attribution.
2. Genuine historical deployment/checksum evidence, DB-owner sign-off, backup/restore rehearsal and downgrade/forward-only recovery plan.
3. Controlled fresh install baseline plan and existing-database migration compatibility; no historic V1 edits.
4. Real process terminate/restart and multi-JVM lease fencing, with external side effects permanently denied in this stage.
5. Explicit separate owner deployment authorization for production migration and recovery scheduling; external delivery remains forbidden.

**Never infer production readiness from an expected-negative Flyway test being green.**
