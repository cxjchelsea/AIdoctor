# U07 D5-O2 Stage B — R2-B01/B02 Targeted Normative Design Fix v0.3

Date: 2026-10-12. Source baseline `main=2834714354b02a88ccf73773511af706554cd9fb`, design PR #434 `c9a53a4c48348724930ca93b3b1f17c0d02f2e32`, review PR #435 `c954a0cb334c0cbf704223ea928c69d6b0642dbc`. This *supersedes only the R2-B01/B02 provisions* in the v0.2 design. Explicitly scoped to Stage B **B1-P0 test-only spike**. No production code, migration, Oracle, actual U06 Bean activation, patient records, outbound delivery or F8 authorization.

## 1. R2-B01 — two transaction managers, same test DataSource

**Frozen P0 choice:** `PROPAGATION_REQUIRED` + `STRICT_NO_AMBIENT_OR_BOUND_RESOURCES`. No `REQUIRES_NEW` in the P0 prototype. Each invocation **must** originate from a clean thread, not join a caller transaction; after the stage it must return the thread to that clean state. This removes the misleading suggestion that REQUIRES_NEW may suspend an unobserved transaction.

- One explicit `stageBDataSource` = disposable local MySQL 8 Hikari pool. It is the **same instance** injected into the explicitly named `stageBJdbcTemplate`, `stageBJdbcTransactionManager` (DataSourceTransactionManager), and `stageBEntityManagerFactory`/`stageBJpaTransactionManager`.
- `@EnableJpaRepositories` must explicitly bind exactly the four Stage A permitted repositories to `entityManagerFactoryRef="stageBEntityManagerFactory"` and `transactionManagerRef="stageBJpaTransactionManager"`. Use the same four Stage A managed entity class whitelist. Do not import `DiagnosisServiceApplication` or auto-scan production application beans.
- The **caller wrapper** chooses the manager by stage: D1 uses only `stageBJdbcTransactionManager`; C1 uses only `stageBJpaTransactionManager`. P0 does not attempt end-to-end execution, nor StateCommitter Backend.
- Each local `TransactionTemplate` uses `PROPAGATION_REQUIRED`, explicit `ISOLATION_SERIALIZABLE`, and explicit timeout (candidate 10 seconds, bounded by CI test). No metadata annotation on a final original class is credited as effective.
- Before *each stage* `TransactionSynchronizationManager.isActualTransactionActive()==false`, `isSynchronizationActive()==false`, no `getResource(stageBDataSource)`, no `getResource(stageBEntityManagerFactory)`, and no unexpected entries in `getResourceMap()`. The expected preflight resource map is **empty** (P0 only); any residual resource => `STAGE_B_AMBIENT_RESOURCE_DENIED` **before opening a transaction**.
- Inside a stage verify actual transaction active, expected resource bindings, **no second local manager invocation**, expected manager/EMF/DS object identity, `Connection.getTransactionIsolation()==Connection.TRANSACTION_SERIALIZABLE` on the **bound writer connection**. `SELECT CONNECTION_ID()` on that actual writer connection; use a different connection from a SELECT-only MySQL user for independent readback. `@@transaction_isolation` alone is insufficient, and requesting isolation is not evidence of effective isolation.
- After both success *and* exceptions, the wrapper `finally` verifies synchronization inactive, no DataSource or EMF resource bound, empty resource map; if failed, mark `STAGE_B_RESOURCE_LEAK` and fail test. Do not hide an original exception; preserve it as cause/suppressed information.
- In active JDBC transaction an attempted nested JPA stage must be rejected **before** entering the JPA `TransactionTemplate`; vice versa JPA→JDBC must be rejected. Negative tests deliberately provoke these paths. A guard that checks only `isActualTransactionActive()` is insufficient if stale resources/synchronization remain.
- For ordered sequential tests, first run D1→C1 and then C1→D1; assert no leftover Spring holders, distinct transactional connection lifetimes and independent post-commit SELECT-only readback. Connection IDs are observed physical facts, **not globally guaranteed unique across unrelated runs**; never infer long-term identities from a single ID.
- If Spring JPA/SQL resources cannot be bound to this exact DS/EMF in the disposable runner, the P0 spike must fail closed with `OWNER_BINDING_UNKNOWN` / `NOT_VERIFIED`; do not silently substitute a primary manager. No proof that sharing a DataSource makes different stages one atomic transaction.

**Safety precedence:** no production code or existing Stage A fixture modifications. Separate Stage B test configuration on test classpath only. A named resource binding is verified by runtime identity and independent observation, not name equality or annotations. Static credentials and `GITHUB_ACTIONS` labels alone do not establish physical disposable isolation; check schema, fixed localhost port, user privileges, MySQL version, InnoDB, table allowlist and DDL blobs.

## 2. R2-B02 — original six SQL writes and precise post-write fault injection

**Frozen proof mechanism:** a test-only subclass of `org.springframework.jdbc.core.JdbcTemplate`, e.g. `StageBPostWriteFaultJdbcTemplate`, constructed with the **same** `stageBDataSource`. Override the exact Java 8-compatible `update(String sql, Object... args)` signature used by `JdbcU06DeliveryStore.reconcileConfirmed()`. The original Store remains unchanged. Interception algorithm:

```java
@Override
public int update(String sql, Object... args) {
    int changed = super.update(sql, args); // real MySQL INSERT with Spring-bound TX connection
    if (isOneOfSixExactDeliveryInsertStatements(sql)) {
        int current = ++deliveryInsertCount;
        if (current == targetAfterWriteOrdinal) {
            throw new StageBInjectedAfterWriteFailure(current); // unchecked, rollback
        }
    }
    return changed;
}
```

This code is an implementation *sketch*, not a compiled Java file. `JdbcTemplate` is subclassable in the Spring version currently used, and the production Store's six literal calls use `update(String,Object...)`; the **P0 gate still requires real Java 8 compilation and a real MySQL end-to-end hook test**, not just source reasoning.

Controls:
- Check exact **ordered SQL prefixes/table allowlist**, `INSERT INTO u06_delivery_authority`, `u06_delivery_intent`, `u06_delivery_attempt`, `u06_delivery_receipt`, `u06_delivery_confirmation`, `u06_delivery_ledger`; case/whitespace normalized only as needed, reject any unrecognized delivery statement. Do not use a broad substring match that can count SELECT, readback or unrelated INSERT.
- Count only after `super.update()` completes successfully; before and after callback assert actual transaction active and the JDBC writer DataSource identity. If `super.update` fails, **do not count** it as a successful post-write injection.
- The exception must propagate out of `TransactionTemplate.execute()` with no catch-and-commit. Only the target `N=1..6` may inject; `N=0`, no target or SELECT readback are never faulted. Reset fault state between tests; no shared mutable static test state.
- Each selected row uses unique synthetic effect and idempotency key; the 6 table records are verified before/after by an independent `stageb_reader` account with SELECT grants only. After *N=6* the 6th write also must rollback, not remain committed. Assert an existing committed unrelated effect remains unchanged.
- **P0 limited evidence:** proof of the interceptor compiling under Java 8 and physically injecting after **one known SQL write** into the disposable MySQL/transaction, with rollback observation. P0 may execute a no-fault six-table happy path as an optional diagnostic but must **not claim D1 six-case coverage**. `N=1..6` full rollback, complete replay, partial-legder negative and race tests are **P1**, subject to new authorization.
- If subclass override does not intercept real calls (wrong overload, Spring delegation or supplier mismatch), test must fail and `R2-B02=NOT_VERIFIED`, triggering targeted re-design of a **test-only JDBC-level proxy**. Do not change production Store to make instrumentation easier.
- `JdbcU06DeliveryStore.reconcileConfirmed()` is final, with six `jdbc.update` calls. Avoid crediting its `@Transactional` annotation as the source of a transaction: only the caller's explicit DataSourceTransactionManager boundary is in scope.

## 3. P0 minimal test and evidence contract

Permitted new files **only**:
1. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/StageBTestConfig.java`
2. `.../stageb/StageBTxOwnerBindingTest.java`
3. `.../stageb/StageBPostWriteFaultJdbcTemplate.java`
4. `.../stageb/StageBJdbcFaultHookTest.java`
5. `diagnosis-service/src/test/resources/u07/d5o2/stageb/p0-manifest.json`
6. `tools/u07_d5_o2_tx_topology/stageb/bootstrap-p0-schema.py`
7. `tools/u07_d5_o2_tx_topology/stageb/verify-p0-evidence.py`
8. `.github/workflows/u07-d5-o2-stage-b-p0.yml`

Specific evidence cases: `P0-OWNER-01` manager→DS and JPA EMF reference identity; `P0-OWNER-02` JDBC active-bound resource and actual isolation; `P0-OWNER-03` JPA active-bound resource and actual isolation; `P0-OWNER-04` sequential D1→C1 / C1→D1 cleanup; `P0-OWNER-05` nested manager negative in both directions; `P0-OWNER-06` wrong manager/unbound DS/reused holder negative; `P0-HOOK-01` Java8-compiled precise `JdbcTemplate.update` intercept; `P0-HOOK-02` one real post-write fault plus all attempted rows rolled back via independent SELECT-only reader.

Runner: GitHub Actions, Java 8, disposable MySQL8, baseline immutable DDL blob checks, fixed distinct Stage B schema/port/test credentials and no outbound real application calls. Use exact HEAD SHA/CI Run ID, `mvn test-compile`, Surefire exact method names, machine-checked P0-case evidence, fail on missing/skipped/failing cases. Artifact declares `TEST_ONLY`, `SOURCE_PROVENANCE=CALLER_OWNED_TEMPLATE`, `PRODUCTION_TOPOLOGY_VERIFIED=false`, `REAL_D5_READY=false`. Do not leak secrets/PHI or claim that an Actions pass proves legitimate production authority.

**P0 is deliberately one bounded spike**, not activation of full B1 or U06 runtime. If the above eight-file whitelist needs to grow (for Java fixtures / SQL), stop and request amended review; do not silently import production beans or change old U06/Stage A CI.

## 4. Disposition

- `R2-B01=TARGETED_NORMATIVE_DESIGN_FIXED; PHYSICAL_EVIDENCE_PENDING`
- `R2-B02=TARGETED_NORMATIVE_DESIGN_FIXED; JAVA8_AND_MYSQL8_HOOK_EVIDENCE_PENDING`
- `B1_P0_SCOPE=FROZEN_AS_TEST_ONLY_SPIKE`
- `R2-B03/R2-B04=NOT_CLOSED; P1/P2_REMAIN_BLOCKED`
- `B2_ACTUAL_U06_PRODUCTION_WIRING=UNVERIFIED_AND_UNAUTHORIZED`
- `REAL_D5=NOT_READY`

This is *targeted design remediation*, not a third-party independent design sign-off and not physical evidence.
