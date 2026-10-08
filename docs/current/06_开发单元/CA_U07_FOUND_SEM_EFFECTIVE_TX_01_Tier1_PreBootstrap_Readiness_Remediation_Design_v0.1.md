# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-1 Pre-Bootstrap Readiness Remediation Design v0.1

> Date: 2026-10-08  
> Design baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654` (Tier-0 four-file-only merge PR #313)  
> Design authority: PR #300 @ `09736a2764a7ea5ba640fbc82bd87bc403ad0b52`; independent conditional design acceptance PR #302; integration readiness PR #303 @ `00c81cb4f7ddda60d1f846063030cca9a3131f38`.  
> Existing Tier-0: PR #311 closure, PR #317 merged-main verification, PR #318 bounded offline verification.  
> Targeted amendment: PR #320 independent review @ `a8407ee46de90d0906cf1268a71b1b5b410d8089`: BF-U07-FOUND-PB-IR-01..03 and RF-U07-FOUND-PB-IR-01/02. This file is the author remediation candidate at a **new exact HEAD**; all review findings stay OPEN pending targeted independent re-review.  
> **Status: TARGETED_DESIGN_REMEDIATED / INDEPENDENT_RE_REVIEW_REQUIRED.** No Tier-1 implementation, execution, environment grant, production access, JDBC, migration, or merge authorization.

## 1. Problem and authority boundaries

This remediation targets **BF-U07-FOUND-TX-INT-01, -02, -03** and the required owner/evidence finding **RF-U07-FOUND-TX-INT-01**. The objective is an independently reviewable implementation readiness path for *read-only topology evidence*, not physical transaction safety. The preexisting immutable SOURCE_ONLY producer must remain intact and cannot be promoted into effective transaction-manager evidence.

- IN SCOPE at this design stage: OS/pre-JVM isolation specification and no-I/O attempt attestation; context representativeness tests and downgrade rules; accurate Spring 5.x effective manager resolver comparator and frozen negative Oracles; exact-diff/provenance and authorization sequence.
- OUT OF SCOPE: actual ApplicationContext startup, network/JDBC `getConnection()`, physical database connections, SQL, Flyway, migrations, write/commit/rollback tests, U07 Binding implementation, PHI, PROFILE-A, real patient systems, U06/U15/P02 grant, source/CI mutation or merge.
- Evidence distinction: preflight safety is not manager identity; configured manager equality is not physical connection enlistment; a safe overridden test context is not automatically representative of the untouched target. No downstream gate receives implicit authorization.

## 2. Exact source inventory and rebase condition

| Item | Source SHA / fact | Required action |
|---|---|---|
| Target main | `86e8843197091c8c8172b7e4213537a31bdf0654` | Freeze for design. Any subsequent drift needs tree/path/blob-diff and source-evidence revalidation. |
| `diagnosis-service/pom.xml` | `dfa3c6f1cfc274ca796bf1bde8824ed1d9b80bb5` | Java 8 / Spring Boot 2.7.8, JPA/AOP, Flyway 7.15, Redis, Nacos, MySQL/Oracle dependency graph. Pin resolved dependency tree at future gate; do not infer exact Spring Framework patch release from Boot version alone. |
| `diagnosis-service/src/main/resources/application.yml` | `7a9d6931da6763f83769a5234ac8aa77459b0be0` | `dev` default is NOT a safe runtime permission. No fallback. |
| Tier-0 `static_projection.py` / `static_schema.py` | `45fd3b336a7ac8797f19714dca111292aba3840e` / `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` | Preserve SOURCE_ONLY and UNKNOWN for actual manager/EMF/DS. |
| Existing U01→Foundation→CDP/Version→Binding→Consultation→Run | PR #299, exact historic six Java source blobs retained at current main | No business invocation during read-only probe. |
| U06 JDBC and future U07 | separate owner / future absent sources | Neither is silently promoted to proven U07 physical co-location. |

## 3. Remediation R1 — INT-01 Pre-JVM containment

**Architecture:** independent caller/orchestrator outside JVM prepares a disposable rootless or equivalent least-privileged sandbox; security boundary enforced by kernel/container permissions **before process creation**. No host networking, privileged container, Docker socket, host PID/IPC namespace, secrets, home/cache credentials, metadata access, real DB sockets or patient mounts. Default-deny IPv4, IPv6, DNS, loopback, Unix-domain sockets connecting outside sandbox, and external egress; filesystem source/dependencies read-only, isolated bounded writable tmp, no access to host DB sockets. Deny/allow policy must be specified and proven for the selected runner, not inferred from an in-process Java interceptor or a shell command alone.

**Lifecycle phases (each persists evidence before proceeding):**
- P0 authority: pinned HEAD/tree, immutable runner image and dependency digests, synthetic profile manifest, human Security + Foundation/U01 permission evidence, sandbox capability attestation.
- P1 preventive boundary: establish namespaces/egress/IPC/filesystem/credential denial and verify policy under the intended OS, runner and identity **before JVM**.
- P2 *non-Spring* adversarial canaries in a **separate SANDBOX_CANARY child process and disposable sandbox instance**: deliberately attempt DNS, IPv4/IPv6, loopback, Unix socket to synthetic/inaccessible targets, metadata-shaped synthetic endpoint, disallowed filesystem writes and prohibited child-process routes. No real DB/production IP, hostname or socket. Expected denial/recorded attempts and zero successful contacts in the canary instance; this is **a negative isolation test, not a Spring diagnostic run**.
- P3 attempt-coverage: preinstalled pre-JVM instrumentation/guard coverage manifest for Java socket/HTTP, DriverManager, connection pools/DataSource, JDBC, Hibernate metadata, Flyway, Redis/Nacos, lifecycle/@PostConstruct, scheduling/event/async, file mutation and spawned children. Coverage of native/JNI/reflection and uninstrumented pathways = explicit UNKNOWN. Instrumentation **cannot substitute** for OS prevention.
- P4 attest and separate phase: terminate/destroy the **SANDBOX_CANARY** process, writable scratch, and namespace; create a **fresh independently attested SPRING_DIAGNOSTIC** sandbox with the same pinned deny-policy hash, no inherited process/file descriptor, no allowlist exception for canaries, independent monitoring counters beginning at zero, fresh sandbox instance ID, monotonic sealed phase-switch timestamp and process lineage. The immutable canary result and diagnostic result are stored separately. If any prerequisite, teardown, counter reset, independent attestation or phase-origin attribution cannot be demonstrated: `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE`; DO NOT create JVM.
- P5 only after **separate implementation/execution authorization** may the probe run in the **SPRING_DIAGNOSTIC** instance. Every forbidden attempted effect attributable to this instance/startup is `CONTEXT_UNSAFE`, including blocked attempts. Unattributable mixed-phase attempts or absent counters are `INCOMPLETE_EVIDENCE` / NOT_EXECUTABLE, never zero. Canary attempts are **not** whitelisted during Spring startup and **cannot** be subtracted from Spring counters. No retry using relaxed network, company dev profile, secrets, actual Oracle or unreviewed overrides.
- P6 destroy isolated process and writable scratch; retain only redacted audit bundle.

**Evidence:** `sandbox_policy_sha256`, `pre_jvm_attestation`, `canary_matrix`, `attempt_coverage_manifest`, `blocked_attempts`, `successful_contacts`, `unobserved_surfaces[]`, `scratch_write_manifest`, `teardown_attestation`, `owner_grant_refs[]`. Additional required origin proof: `phase=SANDBOX_CANARY|SPRING_DIAGNOSTIC`, `instance_id`, `sandbox_policy_digest`, `process_lineage_digest`, `monotonic_phase_boundary`, `canary_teardown_digest`, `diagnostic_fresh_attestation`, `diagnostic_counter_origin=ZERO`. Missing monitor coverage is `INCOMPLETE_EVIDENCE`, not zero.

### 3.1 Frozen phase-local negative Oracle (PB-IR-01)

| Phase | Expected attempts | Successful forbidden effects | Disposition |
|---|---|---|---|
| `SANDBOX_CANARY` separate instance | Deliberately denied canary attempts, provenance attached | **ZERO** | `ISOLATION_POLICY_PASS` only when every required denied attempt is evidenced; any success = `ISOLATION_POLICY_FAIL` |
| `SPRING_DIAGNOSTIC` fresh instance | **ZERO** forbidden attempts from JVM or children | **ZERO** | Candidate for `CONTEXT_CONFIRMED` only when independent representativeness and resolver checks also pass; any forbidden attempt = `CONTEXT_UNSAFE` |
| Mixed/ambiguous origin, inherited counters/FD or broken teardown | UNKNOWN | UNKNOWN | `INCOMPLETE_EVIDENCE` / `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE`, never PASS |

**Runner feasibility prerequisite (RF-PB-IR-01):** before Stage A1 authorization identify exact runner OS/kernel, rootless UID mapping, user/network/mount/PID namespace permissions, firewall/egress enforcement or equivalent host policy, container runtime features, Unix socket mount restrictions, DNS/metadata prevention, child-process containment, read-only cache and sandbox cleanup. Require Security ownership and executable non-Spring negative canaries on the **chosen runner**, not an assumed GitHub-hosted CI capability. If a shell script cannot enforce these controls under the runner identity, fail closed or propose a separately reviewed different runner; never elevate privileges silently.

**Acceptance for design:** Security independent reviewer agrees enforcement primitives are implementable on selected OS and runner; canary negative cases and eventual pre-JVM evidence are specified as exact Oracle outputs. **Acceptance for implementation/execution must be a later gate**, requiring runnable non-Spring sandbox demonstration and the above signed evidence.

## 4. Remediation R2 — INT-02 Representative offline Spring context

### 4.1 Two-phase inventory, no unsafe chicken-and-egg

- **R2-A offline no-JVM:** parse pinned application config, dependency graph, auto-configuration candidates, `@Configuration`/`@Bean` and relevant annotated sources. Identify each initialization route: Flyway, Hibernate metadata, pool, Redis, Nacos discovery, health, Web/Actuator, @PostConstruct, listener, scheduled/async, filesystem and outbound clients. This generates a candidate dependency/effect graph, NEVER a context-confirmed claim.
- **R2-B sandbox-first candidate bootstrap:** only after INT-01 boundary and separate execution authority, create an isolated test-only context with minimal reviewed overrides. Guards remain active before bean initialization. If bootstrap cannot safely complete, fail closed instead of forcing it.

### 4.2 Independent target-topology reference (PB-IR-02)

A test context cannot self-certify `configured same manager` merely because its own BeanDefinitions agree with its own observation. Before any Spring diagnostic startup, an **independent provenance reviewer** freezes a `target_reference_manifest` for the **specified nonpatient target profile** from: (a) exact source and annotated bean/configuration declarations; (b) full, immutable property-source origin and precedence records, including any unresolved external config source as UNKNOWN; (c) Maven resolved dependency/classpath and Spring auto-configuration condition inputs; (d) independently admissible prior effective Bean/advisor/manager/EMF/DS observations **if such a no-I/O authority record already exists**. Unknown actual profile values, external Nacos properties or condition outcomes remain UNKNOWN; do not fetch them through network, assume dev settings or invent BeanFactory outcomes.

For each owner edge, freeze `target_method; property_origin_digest; bean_definition_candidate_digest; effective_condition_expected_or_UNKNOWN; advisor_expected_or_UNKNOWN; interceptor_default_expected_or_UNKNOWN; effective_manager_expected_or_UNKNOWN; emf_expected_or_UNKNOWN; datasource_expected_or_UNKNOWN; evidence_authority`. Separately record the observed test-context graph and all test-only overrides. A comparison succeeds only where both sides have **independent, same-profile, same-version, noncircular evidence of effective selection** (not merely candidate source declarations) and every relevant method/proxy/advisor/manager/EMF/DS condition is unchanged. `UNKNOWN` on either side or substituted resolver-affecting beans forces `REPRESENTATIVENESS_NOT_PROVEN`; `SOURCE_ONLY` remains the proper claim if there is no such independently admissible reference.

The reference manifesto and comparator Oracle are versioned **before** probe observations and their SHA-256 signatures included in the bundle. Reviewer checks that the independent target baseline was not derived from the output it is asked to validate. A safe context that uses a no-connect DataSource/EMF substitute may prove instrumentation behavior **only**: it cannot pass E01–E05 for the untouched target. `CONFIGURED_SAME_MANAGER` is possible only if an independently justified effective-manager reference exists without forbidden contact; if it does not, a truthful negative result is expected.

### 4.3 Override impact contract

For **every** proposed override record:
`property_or_bean; original_value_or_unknown; test_value; reason; source_condition; changed_bean_definitions; advisor_delta; transaction_interceptor_delta; manager_delta; emf_delta; datasource_delta; affected_edges; observed_evidence; independent_reviewer`.

Examples **are candidates, not approved modifications**: `spring.flyway.enabled=false`, `spring.jpa.hibernate.ddl-auto=none`, scheduling/runner/event suppressors. Never assume those properties prevent initial database metadata access. Replacing a DataSource/EMF/manager, disabling relevant autoconfiguration, or altering selected transaction advisor requires `REPRESENTATIVENESS_NOT_PROVEN` for untouched-context E01–E05. A test-only no-connect stub can demonstrate test-harness mechanics but cannot prove the target configuration's effective manager.

### 4.4 Verdict lattice

1. `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE`: preventive isolation or safe startup cannot be established; do not start/continue.
2. `CONTEXT_UNSAFE`: any attempted forbidden resource access, including blocked attempt; abort and quarantine evidence.
3. `INCOMPLETE_EVIDENCE`: unobserved attempt surface or missing provenance/override comparison.
4. `OBSERVED_WITH_TEST_OVERRIDES / REPRESENTATIVENESS_NOT_PROVEN`: safe context, but manager/advisor/EMF/DS equivalence to target cannot be independently shown.
5. `SOURCE_ONLY`: available static metadata without representative context.
6. `CONTEXT_CONFIRMED` / at most `CONFIGURED_SAME_MANAGER`: only when no forbidden attempts, exact effective bean/advisor/manager/EMF/DS selections are observed and unchanged by overrides, independent comparator/owner accepted. This is **never** proof of actual JDBC connection sharing or COMMIT.

Unsafe/privacy/provenance failure overrides positive states. Unknown may not be coerced into a positive result. A fully representative context might be impossible within read-only scope; that is an acceptable and informative negative outcome, **not** a reason to relax the guard.

## 5. Remediation R3 — INT-03 Effective Spring manager resolver

### 5.1 Comparator strategy

Pin Maven resolved Spring Framework 5.x component jar/source hashes with the actual test classpath, not just Spring Boot version. Comparator must independently reflect actual `TransactionInterceptor`, `TransactionAttributeSource`, `BeanFactory` and `TransactionManagementConfigurer` behavior for *the exact target method on the actual Spring proxy* without invoking business service methods.

- R0 verify method/source blob.
- R1 identify actual injected bean definition and caller→callee reference.
- R2 identify JDK/CGLIB proxy and whether method call crosses it; self-call, final/non-public, bypass => `PROXY_NOT_EFFECTIVE` or UNKNOWN.
- R3 collect matching advisors and execution order, actual `TransactionInterceptor` candidate.
- R4 query effective `TransactionAttributeSource(method,targetClass)`; include interface/target/inheritance and bridge methods.
- R5 extract effective transactionManager/value qualifier from actual attributes.
- R6 resolve qualified manager against actual interceptor/BeanFactory; do not assume `@Primary` supersedes explicit qualifier.
- R7 resolve unqualified manager via actual interceptor default/configurer/fallback; ambiguities => UNKNOWN.
- R8 inspect manager object identity and configured EMF, persistence unit and DataSource bean/proxy chain, using run-local pseudonyms; no connection allocation.
- R9 compare E01–E05 independently (`SAME_MANAGER_BEAN`, `SAME_EMF_BEAN`, `SAME_PERSISTENCE_UNIT`, `SAME_DATASOURCE_BEAN`, `SAME_UNWRAPPED_DATASOURCE_TARGET`); PASS/FAIL/UNKNOWN/OVERRIDDEN for each. **Do not infer physical COMMIT/connection.**

A static comparator/pure fixture exercise can be separately scoped before Spring bootstrap, but results remain `COMPARATOR_UNIT_ONLY`.

### 5.2 Stage A2 exact comparator fixture and authority freeze (PB-IR-03)

**Proposed future Stage A2 allowlist (all NOT_AUTHORIZED):**
- `D1-04 diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/topology/U07EffectiveManagerResolver.java` — implementation under test only.
- `D1-04-T diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/topology/U07EffectiveManagerResolverTest.java` — separate synthetic test driver and assertions.
- `D1-04-O tools/u07_foundation_tx_topology/oracle/stage_a2_cases.json` — *independent* synthetic expected-value manifest authored, reviewed and SHA-pinned **before** implementation/tests.
- `D1-04-M tools/u07_foundation_tx_topology/oracle/stage_a2_dependency_manifest.json` — exact dependency-tree and local artifact SHA256/coordinates, offline classpath provenance and fixture-schema version.

Those extra paths are a **controlled amendment candidate** to PR #303's earlier D1-01..07 list; **not** automatically authorized by this document. Separate exact-diff design acceptance must precede any bounded Stage A2 implementation authorization. POM changes, additional helper files, CI workflow execution and network dependency fetching are explicitly prohibited absent a new amendment.

Frozen fixture record (JSON schema proposal): `case_id; fixture_id; fixture_sha256; source_ref; target_method; proxy_kind; caller_edge_mode; advisor_order; transaction_attribute; manager_candidates; method_qualifier; configured_default; configurer_binding; emf_ds_map; property_origin_digest; expected_effective_attr; expected_selected_manager_or_UNKNOWN; expected_proxy_outcome; expected_E01_E05; expected_failure_code; framework_classpath_sha256`. All fields must be synthetic/non-PHI, and `UNKNOWN` must be representable per expected field.

**Independent Oracle authority:** author/reviewer of `stage_a2_cases.json` must be distinct from comparator implementation source; expected results cannot be generated by the implementation or from observed test output. Before coding, an independent reviewer verifies that Spring 5.x *resolved artifacts* are pinned by actual local Maven dependency tree/classpath and jar SHA256; fixed Boot 2.7.8 alone is insufficient. Oracle truth is evaluated against actual pinned Spring transaction interceptor/source semantics and mixed-manager negative conditions. Stage A2 tests may execute only under later explicit **synthetic comparator unit** authorization without Spring ApplicationContext boot, business method invocation or JDBC, and with offline preverified dependencies. If those boundaries cannot be met, return `A2_NOT_READY`.

### 5.3 Independently frozen Oracle candidates (all NOT_EXECUTED)

| Case | Synthetic configuration | Required output |
|---|---|---|
| TX-R01 | explicit manager B; manager A marked Primary | chooses actual B if proxy/interceptor applies |
| TX-R02 | unqualified, deterministic interceptor default | resolves the actual default only |
| TX-R03 | unqualified and multiple unresolved managers | UNKNOWN; no heuristic selection |
| TX-R04 | TransactionManagementConfigurer changes default | apply real framework resolution |
| TX-R05 | JDK proxy / interface vs target annotation | selected advisor+effective attribute or PROXY_NOT_EFFECTIVE |
| TX-R06 | self-invocation bypasses proxy | PROXY_NOT_EFFECTIVE |
| TX-R07 | final or non-intercepted target method | PROXY_NOT_EFFECTIVE / UNKNOWN |
| TX-R08 | distinct manager beans share DataSource | SAME_DATASOURCE may PASS; SAME_MANAGER must FAIL |
| TX-R09 | same manager but ambiguous EMF/DS target | manager evidence separate; EMF/DS UNKNOWN |
| TX-R10 | multiple advisors/interceptors, nondeterministic selection | UNKNOWN |
| TX-R11 | test override swaps transaction manager | REPRESENTATIVENESS_NOT_PROVEN |
| TX-R12 | no source/bean identity hash | INVALID_PROVENANCE, no positive verdict |

Independent Oracle author must produce immutable expected cases prior to collecting any observations; compare against pinned actual Framework behavior (not a hand-authored approximation alone).

## 6. Controlled exact-diff plan — **candidates only**

This document introduces **one design Markdown file only**, on an isolated branch. No implementation diff or permission changes.

Future candidate staged authorization:

| Stage | Existing planned ID/path | Scope and restrictions |
|---|---|---|
| Stage A1 | `D1-01 tools/u07_foundation_tx_topology/sandbox/deny_network.sh` | Non-JVM sandbox policy/attestation only; OS/runner-specific enforcement with independent Security review; no Spring startup |
| Stage A1 | `D1-02 tools/u07_foundation_tx_topology/sandbox/bootstrap_guard_manifest.json` | Typed attempted-effect surfaces and source/config hashes; missing coverage fail-closed |
| Stage A2 | `D1-04`, proposed additions `D1-04-T`, `D1-04-O`, `D1-04-M` exactly as §5.2 | Synthetic comparator + independently pre-frozen Oracle/test driver/dependency manifest only. **New files require separate controlled exact-diff amendment and authorization before creation.** No context/JDBC/DB, and no unbounded helper files. |
| Stage B | `D1-03 .../U07EffectiveManagerTopologyProbeTest.java` | Real-context observation only if isolation + representativeness and owner permissions pass |
| Stage B | `D1-05 .../U07TopologyRedactionOracleTest.java` | Redaction, proxy resolution and failure-precedence checks |
| Stage B | `D1-06 tools/u07_foundation_tx_topology/evidence/verify_bundle.py` | Signed evidence verification; no secrets |
| Stage B | `D1-07 .github/workflows/u07-effective-tx-tier1-probe.yml` | Manual-only workflow; never automatic trigger; must prove runner sandbox can enforce preventive isolation |

**Caution:** Stage A2 may need Spring dependency jars just to unit-test selection logic; if any context bootstrap or dependency resolution accesses the network, this is outside its proposed authorization. Require local preverified deps, independent test scope and exact-diff amendment rather than expanding implicitly.

**Immutable denylist for this CA:** production `diagnosis-service/src/main/java/**`, `diagnosis-service/src/main/resources/application.yml`, MySQL/Oracle migrations, production configs, patient/PHI records, U06/U15/P02 grants, U01/CDP/Run/Binding mutators and unreviewed POM or automatic CI workflows. No new Maven dependency/file beyond approved exact allowlist. No authorization to modify any of D1-01..07 follows from this design.

## 7. Design verification matrix and explicit stop policy

| Gate | Independent evidence required | Fail closed |
|---|---|---|
| PB-G01 baseline | HEAD/tree/POM/Tier0 hashes; diff = one design file | SHA drift/unreviewed extra diff |
| PB-G02 isolation | selected OS/runner, concrete kernel enforcement/UID capabilities, **separate canary and diagnostic instances**, per-phase counter/provenance + non-Spring negative canary Oracle | denied canary attempt counted as Spring safety proof, origin ambiguous, or preventive denial not demonstrable |
| PB-G03 attempted effects | full interception/coverage mapping incl. pre-bean paths and child processes | any unobserved surface -> INCOMPLETE_EVIDENCE |
| PB-G04 no-side-effect context | independent **pre-observation** target-reference manifest/property origins/auto-config conditions, startup inventory, per-override impact | circular test-baseline comparison, unknown target or changed topology -> REPRESENTATIVENESS_NOT_PROVEN |
| PB-G05 resolver fidelity | R0–R9; independently SHA-frozen TX-R01..12 expected Oracle, exact synthetic fixture/test/manifest paths, offline pinned Spring Framework 5.x jar/classpath proof | unknown classpath/extra files/ambiguous method-manager proxy -> UNKNOWN or A2_NOT_READY |
| PB-G06 evidence/data | synthetic-only, no PHI/secrets, redaction, access/log retention | UNKNOWN PRIVACY -> BLOCKED |
| PB-G07 authority | Security + Foundation/U01 owner grant, later separate exact-diff implementation/execution decision | absent grant -> NOT_AUTHORIZED |
| PB-G08 independent review | no self-certification, accepted finding-by-finding evidence & exact head | no independent reviewer -> PENDING |
| PB-G09 prohibited behavior | no Spring/JDBC/test runtime, migration, production or merge at design stage | any execution -> scope violation |

**Negative case precedence:** `CONTEXT_UNSAFE` > `INVALID_PROVENANCE` > `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE` > `RESOURCE_MISMATCH/PROXY_NOT_EFFECTIVE` > `INCOMPLETE_EVIDENCE` > `REPRESENTATIVENESS_NOT_PROVEN` > `SOURCE_ONLY` > `CONTEXT_CONFIRMED`. The precedence is only for a single observation under the relevant mode, and cannot hide multiple blockers; preserve all failure facts independently.

## 8. Readiness dependencies and next decisions

| Finding | Design remediation | Current closure state |
|---|---|---|
| BF-U07-FOUND-TX-INT-01 | R1 kernel isolation + non-Spring canaries + guard coverage + owner evidence | DESIGN_PROPOSED; execution NOT_PROVEN |
| BF-U07-FOUND-TX-INT-02 | R2 inventory + per-override impact + downgrade lattice | DESIGN_PROPOSED; representative context NOT_PROVEN |
| BF-U07-FOUND-TX-INT-03 | R3 pinned Spring method/interceptor resolver + 12 independent Oracle cases | DESIGN_PROPOSED; implementation/comparator NOT_EXECUTED |
| RF-U07-FOUND-TX-INT-01 | explicit Security + Foundation/U01 authorizations, profile/privacy/retention | REQUIRED / NOT_GRANTED |
| RF-U07-FOUND-TX-INT-02/03 | Tier0/source-only boundary and exact-diff denylist unchanged | REQUIRED / CONTINUED |

Next **permitted review type**, not implementation: `Tier-1 Pre-Bootstrap Targeted Independent Design Re-Review` anchored to this **new** exact author HEAD/blob. Review must decide `PASS / REVISE_REQUIRED` on R1–R3, the OS canary model, representativeness versus override truth, independence of TX-R Oracle, and staged exact-diff allowlist. If accepted, separately seek narrowly bounded Stage A design/readiness and implementation authorization; Stage B and actual Tier-1 Spring context remain blocked until independently proven prerequisites.

## 9. Decision and non-grants

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01 PREBOOT REMEDIATION DESIGN
= TARGETED_DESIGN_REMEDIATED / INDEPENDENT_RE_REVIEW_PENDING
BF-U07-FOUND-PB-IR-01..03 = REMEDIATION_PROPOSED / NOT_CLOSED

BASELINE = 86e8843197091c8c8172b7e4213537a31bdf0654
TIER0 = VERIFIED / MERGED / SOURCE_ONLY
INT-01 = DESIGN_PROPOSED / BLOCKER_OPEN
INT-02 = DESIGN_PROPOSED / BLOCKER_OPEN
INT-03 = DESIGN_PROPOSED / BLOCKER_OPEN
RF-INT-01 = REQUIRED / GRANT_NOT_PROVEN
TIER1_IMPLEMENTATION_READINESS = NOT_READY
TIER1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
TIER1_CONTEXT_EXECUTION = NOT_EXECUTED
SEM-01 / SEM-02 = OPEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```
