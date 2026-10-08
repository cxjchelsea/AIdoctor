# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Independent Design Review v0.1

> Review date: 2026-10-08
> Author candidate: [PR #300](https://github.com/cxjchelsea/AIdoctor/pull/300)
> **Exact reviewed author HEAD:** `dad40fb399225290c4774d4b748b174e9868c2ed`
> **Exact design blob:** `90f332365971ac721c1b862351af6b16b7f0edb0`
> Baseline main: `6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Preceding independent semantic compatibility review: [PR #299](https://github.com/cxjchelsea/AIdoctor/pull/299) exact HEAD `c746b347eba465e3961ddc3465f85e2395d84a2a`
> **VERDICT: REVISE_REQUIRED / TWO DESIGN BLOCKERS**
> Evidence-only independent document; no application bootstrap, implementation, diagnostics execution, database connection, test, owner authorization, or merge.

## 1. Independent review scope and accepted elements

Reviewed CA design §§1–8 at the exact author SHA. It defines exactly eight `TX-G01..08` independent evidence gates and sixteen `TX-T01..16` design-only case IDs, both consecutively numbered. Reviewed against the existing source-backed U01→Foundation→CDP→CDPVersion→RuntimeBinding→Consultation→ClinicalRun graph and the current main source's absence of U07 production beans.

**Accepted design properties:**
- Correct Tier-0 source-only vs Tier-1 context-introspection separation and explicit Tier-2 physical connection/COMMIT evidence exclusion.
- No `DataSource.getConnection`, SQL, Flyway, schema DDL, business-method calls, patient data or production/implicit dev Oracle access permitted.
- Exact SHA/profile/runner/grant provenance, redaction and independent expected Oracle/Fixture requirements.
- Detailed consumer bean/advisor/manager/EMF/DataSource typed topology, with `NOT_IMPLEMENTED` for future U07 beans rather than invented evidence.
- `CONFIGURED_SAME_MANAGER` explicitly **not equivalent** to `PHYSICALLY_SAME_CONNECTION_AND_COMMIT`.
- Correct retention of Foundation Audit `NOT_PASSED`, U07 readiness `NOT_READY`, no PHI/PROFILE-A or implementation authorization.
- `JdbcU06DeliveryStore` possible AOP interceptibility is rightly a **separate U06 owner** diagnostic, not proof of Foundation or U07 atomicity.

However, two executable-contract details are underdetermined and can invalidate Tier-1 evidence even if an implementation superficially follows the document.

## 2. BF-U07-FOUND-SEM-TX-IR-01 — Pre-bootstrap no-connect / no-side-effect containment not enforceable by current specification

**OPEN / BLOCKER / NO_SIDE_EFFECT_AUTHORITY_NOT_PROVABLE.**

§2 requires preventing Spring context startup DB/network activities (Flyway, JPA JDBC metadata resolution, connection pool validation, bean constructors, `@PostConstruct`, application listeners, scheduled tasks), while §5 only instructs the future test harness to *trap* such calls at "supported surfaces". There is no frozen **pre-bootstrap isolation primitive**, no exhaustive instrumentation coverage model, and no stop-before-first-contact authority. Merely having zero collected counters **after** the context starts cannot prove there was no uninstrumented connection attempt during bean construction or Spring auto-configuration. A configuration override suppressing Flyway/JPA may also materially change the bean topology under test.

### Required targeted design remediation

1. Freeze a **specific pre-bootstrap containment topology** for Tier-1: an isolated process/classloader environment with network egress disabled at the OS/test sandbox boundary and no credentials or reachable clinical datasource; install all JDBC/DriverManager/DataSource/pool/Flyway, socket/HTTP, scheduler/async and outbound interceptors **before** the Spring context is created. State the trusted enforcement authority and what each interception layer can/cannot observe. Deny-by-default at boundary; instrumentation must not merely log after contact.
2. Declare the **bootstrap override allowlist** and per-override source→observation impact matrix. If disabling Flyway, Hibernate metadata discovery, scheduling, health checks or changing `DataSource`/EMF substitutes a bean or changes manager selection, mark the affected equality E01–E08 `OBSERVED_WITH_TEST_OVERRIDES / REPRESENTATIVENESS_NOT_PROVEN`, not unqualified `CONTEXT_CONFIRMED`.
3. Specify **failure-before-observation**: attempted forbidden JDBC/network access, unexpected bean initializer, unmonitored source, or startup needing live DB is `CONTEXT_UNSAFE` or `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE`. Do not attempt again with relaxed access or production/dev credentials.
4. Add explicit negative design cases for **connection attempt during early Spring auto-configuration**, uninstrumented JDBC wrapper/direct `DriverManager`, side-effecting `@PostConstruct`, and override-induced transaction-manager replacement; attach independent bypass coverage and nonsecret evidence.
5. Define test-only diagnostics prohibition on publishing or storing secret/environment internals; emergency cleanup and artifact invalidation if exposure occurs.

**Acceptance:** independent reviewer can determine *before the first Spring bean initialization* which isolation mechanism blocks all forbidden external activity and which results must be downgraded because the isolation altered the observed transaction topology.

## 3. BF-U07-FOUND-SEM-TX-IR-02 — Effective manager resolution lacks a deterministic source-of-authority algorithm

**OPEN / BLOCKER / EFFECTIVE_MANAGER_RESOLUTION_AMBIGUITY.**

§3/§4 request that each bean's "effective manager" and shared EMF/DataSource identity be observed, while §4.1 uses direct manager equality E01–E05. But the contract does not specify how to resolve the **transaction advisor's actual selected `PlatformTransactionManager`** at each method/call edge: method/class `@Transactional(transactionManager/value)`, inherited metadata, `TransactionManagementConfigurer`, transaction interceptor manager qualifier/default, named BeanFactory resolution, `@Primary`, multiple eligible managers, JDK/CGLIB proxy method differences, and AOP self-call bypass. Listing registered manager beans or `Advised` advisor types does **not** by itself establish which manager a transactional invocation would select.

The equality formulation also risks conflating **same manager bean identity** with **same underlying DataSource and persistence unit**, and fails to distinguish *configured method transaction-manager resolution* from *runtime selection/enlistment* which Tier-1 cannot execute without forbidden business calls.

### Required targeted design remediation

1. Freeze a **purely introspective manager-resolution algorithm** with an explicit precedence table for exact proxied method+target method `TransactionAttributeSource`, transaction qualifier/value, named manager bean, applicable default configured via `TransactionManagementConfigurer` or `TransactionInterceptor`, unique candidate/`@Primary` where Spring actually uses it; do **not** assume generic `@Primary` always determines the interceptor choice.
2. Produce per-edge identity chain `declared_method → proxy_method → resolved_attribute → chosen_interceptor → resolution_rule → chosen_manager_bean → EMF/persistence_unit → DataSource/proxy_unwrap`; require independent cross-check with actual BeanFactory bean definitions and qualifiers, not inferred names.
3. Treat ambiguous manager resolution, proxy advisor order, JDK interface annotation mismatch, self-invocation or final/uninterceptable methods as `UNKNOWN` or `PROXY_NOT_EFFECTIVE`, and block any E01–E05 **PASS** where resolution is not deterministic.
4. Specify comparator equivalence tiers: `SAME_MANAGER_BEAN`, `SAME_EMF_BEAN`, `SAME_DATASOURCE_BEAN`, `SAME_DATASOURCE_TARGET_AFTER_KNOWN_PROXY_CHAIN`, then `ACTUAL_PHYSICAL_TX` explicitly **OUT_OF_SCOPE**. A matching proxy bean/hash is not proof of same underlying connection, and stable salted tokens require consistent within-artifact derivation for actual identity comparisons.
5. Add independent negative cases for two managers with explicit `@Transactional("...")`, no named manager with multiple candidates, JDK proxy interface annotation mismatch and `TransactionManagementConfigurer` precedence, plus `@Primary` versus explicit qualifier disagreements.

**Acceptance:** a deterministic Tier-1 comparator can return `CONFIGURED_SAME_MANAGER` only when the actual Spring *configuration* resolves the same manager and compatible EMF/DataSource resources for the identified proxied invocations. This **still must not** claim physical transaction enlistment.

## 4. Nonblocking follow-ups and evidence boundary

- Retain `TX-G01..08` and `TX-T01..16`; new negative cases shall be additive or explicitly trace the originals, with stable IDs.
- `U07EffectiveTxEvidenceBundleV1` must bind tool version, profile, isolation policy, override manifest, source SHA and independent Oracle; no reference to a future U07 `BindingRepository` should be reported as currently present.
- `BF-U07-FOUND-SEM-02` (shared Ledger/RuntimeBinding duplicate-key and U01 rollback safety) is **independent**, still OPEN. Tier-1 can only show configured manager topology, never a healthy collision path.
- Source lexical scan completeness in PR #297/#298 remains PASS and should not be reopened solely because a semantic manager question remains.
- Any future runtime boot/introspection, even without intended SQL, requires an explicit, separate security/environment/owner authority decision. This design review issues none.

## 5. Exact-head independent design review decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
INDEPENDENT DESIGN REVIEW
= REVISE_REQUIRED

Exact reviewed author HEAD
= dad40fb399225290c4774d4b748b174e9868c2ed

BF-U07-FOUND-SEM-TX-IR-01
= OPEN / PRE_BOOTSTRAP_NO_SIDE_EFFECT_CONTAINMENT
BF-U07-FOUND-SEM-TX-IR-02
= OPEN / DETERMINISTIC_EFFECTIVE_MANAGER_RESOLUTION

TX-G01..08 and TX-T01..16 = DESIGN_CASES_ONLY / NOT_RUN
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
= DESIGN_REVISE_REQUIRED / NOT_AUTHORIZED / NOT_FROZEN

BF-U07-FOUND-SEM-01 = OPEN
BF-U07-FOUND-SEM-02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
SOURCE_LEXICAL_INVENTORY_COMPLETENESS = PASS
EFFECTIVE_SPRING_MANAGER_TOPOLOGY = NOT_PROVEN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next action:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Targeted Design Remediation` on the original author PR #300, specifically solving IR-01/02 without granting a probe or changing Runtime. Then request a **Targeted Independent Design Re-Review** at the amended exact HEAD.

This document is a **review-only opinion on a specified design artifact**. No implementation authorization, real context introspection, MySQL/Oracle access, executable test completion or merge is implied.
