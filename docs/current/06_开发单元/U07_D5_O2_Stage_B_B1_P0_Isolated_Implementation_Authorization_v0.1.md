# U07 D5-O2 Stage B — B1-P0 Isolated Implementation Authorization Decision v0.1

Date 2026-10-12. Source main `2834714354b02a88ccf73773511af706554cd9fb`. Inputs: Stage B v0.2 Design PR #434, re-review PR #435, and R2-B01/B02 focused normative v0.3 in this same documentation PR.

## Decision

**AUTHORIZE LIMITED B1-P0 TEST-ONLY SPIKE**, subject to an exact-head preflight of the normative v0.3 document and implementation whitelist. This decision is bounded implementation authorization from the user's explicit request for a B1-P0 authorization assessment; it is **not** an independently signed architecture approval, not an authorization to merge this design PR, and not a production gate.

**Precisely authorized:** create a *new Draft implementation PR* based on exact main, at most the eight listed test/fixture/tools/CI files from v0.3. Compile and run the minimal Java8/disposable MySQL8 dual-transaction-manager identity/isolation/leak tests, wrong-manager/nested-call negative controls, plus one real original-SQL `JdbcTemplate.update(String,Object...)` post-write failure with SELECT-only rollback readback. Do not extend P0 scope merely because additional tests are useful; raise a scope amendment if needed.

### Mandatory execution prerequisites (fail closed)

1. Re-read real current remote main and exact v0.3 HEAD before creating implementation branch; if moved, re-review diff and provenance first.
2. Source-only scope is immutable: no `src/main`, Flyway, Oracle, clinical data, production bean registration, original U06 structural safety gate, existing Stage A probe or CI edits. Exactly the eight P0 allowlisted paths, with any extra require explicit review.
3. CI runner must use Java 8 + fresh disposable MySQL8, localhost fixed stageb-specific schema/port/users, original pinned immutable DDL and a read-only separate observer; do not reuse a real database or patient data. No real external delivery, model, tools or F8.
4. Before and after each stage the Spring thread must be clean (transaction inactive, synchronization inactive, DS/EMF resource map empty); each named manager is explicitly identity-bound and effective connection isolation verified. Inner transaction manager invocation must be rejected.
5. A P0 SQL fault hook must execute original SQL through `super.update` **before** throwing; check real transaction rollback from independent SELECT-only account. On compiler or interception mismatch, P0 FAIL and re-review; no mock-only substitute.
6. Case evidence must bind actual HEAD, workflow run, test name, stage, requested/actual isolation, manager/DS connection provenance and physical rollback observation. Missing/skip/error => not PASS.
7. All reports must state `B1_P0_ISOLATED_ONLY`, `PRODUCTION_TOPOLOGY_VERIFIED=false`, `B1_FULL_IMPLEMENTATION_NOT_READY`, `REAL_D5=NOT_READY`.

### Not authorized

- Complete D1 six-case atomicity/replay/concurrency (B1-P1), Stage B complete cross-phase recovery/evidence closure (B1-P2), B2 production Spring wiring, actual `@Transactional` advice, persistent StateCommitter/CDP owner assumption, external delivery, clinical privileges, scheduler, F8 resume, Oracle, patient/PHI, deployments, merging PRs.
- A successful probe alone does not certify the annotation on `final JdbcU06DeliveryStore` nor authorize a production bean.
- A design document in an unmerged Draft PR is a controlled work item, not a live production policy.

## Gate table

| Gate | Decision |
| --- | --- |
| R2-B01/B02 v0.3 normative design | DESIGN_FIXED_FOR_P0 / TARGETED_REVIEW_AND_TEST_PENDING |
| B0 read-only source attestation | READY |
| B1-P0 isolated implementation scope | **AUTHORIZE_LIMITED** |
| B1-P0 actual Java8/MySQL8 verification | NOT_EXECUTED |
| B1-P1 / B1-P2 full implementation | NOT_AUTHORIZED |
| B2 actual production integration | NOT_AUTHORIZED |
| PR / main merge | NOT_AUTHORIZED |
| REAL_D5 | NOT_READY |

**Next operation:** B1-P0 isolated implementation Draft PR, then exact-head real CI, targeted security/implementation review and evidence intake. Never infer that this implementation authorization grants subsequent merge.
