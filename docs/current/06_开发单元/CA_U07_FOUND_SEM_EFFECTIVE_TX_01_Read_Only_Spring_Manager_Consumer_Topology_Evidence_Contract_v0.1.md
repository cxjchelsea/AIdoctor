# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Read-Only Effective Spring Manager / Consumer Topology Evidence Contract v0.1

> Design date: 2026-10-08
> Initiating independent review: [PR #299](https://github.com/cxjchelsea/AIdoctor/pull/299) exact HEAD `c746b347eba465e3961ddc3465f85e2395d84a2a`
> Inspected physical source baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Prior full tracked-source inventory: [PR #297](https://github.com/cxjchelsea/AIdoctor/pull/297), independent evidence [PR #298](https://github.com/cxjchelsea/AIdoctor/pull/298), CI run `37736999966`, artifact `11531528615`
> **CA STATUS: CONTROLLED_DESIGN_CANDIDATE / NOT_AUTHORIZED / NOT_FROZEN / PENDING_INDEPENDENT_DESIGN_REVIEW**
> **BF-U07-FOUND-SEM-01: OPEN / PENDING_EVIDENCE.**
> **GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01: NOT_PASSED.**
> This document is only a safe diagnostic contract. It neither executes diagnostics nor authorizes changes, production introspection, U07 persistence, MySQL/Oracle writes, clinical activity or PHI access.

## 1. Purpose and trust boundary

Resolve the exact question **which live Spring AOP proxy and transaction manager / EntityManagerFactory / DataSource resources actually govern the existing Foundation and U01 consumer chain**; independently establish whether the proposed U07 `TransactionTemplate(PROPAGATION_REQUIRES_NEW)` could target those exact resources in a future, separately authorized physical implementation.

This is a **read-only** ApplicationContext and configuration-topology evidence exercise. It cannot establish rollback-only behavior after a forced duplicate key, database atomicity after a write, transaction isolation performance, remote consumer absence, an authorized P02 grant, or complete U07 implementation readiness. Those are **separate evidence authorities**. No test should call `U01ConsultationService.start`, `CDPManager.createCDP`, any `save/flush`, or other write method to satisfy this contract.

### 1.1 Baseline integrity and profile admission

```text
S-0 source commit + tree + artifact SHA pinned and verified
  ↓
S-1 explicit synthetic nonproduction environment and execution grant verified
  ↓
S-2 exact ApplicationContext profile and bean definitions inventoried
  ↓
S-3 target consumer beans and AOP advisors inspected without invoking business methods
  ↓
S-4 transaction manager / EMF / datasource non-secret topology inspected
  ↓
S-5 compare expected graph versus observed graph and mark unknowns
  ↓
S-6 evidence bundle independently reviewed
```

Allowed source target = immutable `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`; any newer head needs separate source manifest/diff review, not silent substitution. Captured artifact shall bind the exact source SHA and Git tree, CI/workflow SHA and job ID, runtime dependency lock/POM digest, selected Spring profiles, Spring Boot/JVM versions and oracle/fixture digests.

**Admitted physical environment:** explicitly isolated **PROFILE-B synthetic/nonpatient** build/run with no real patient databases, PHI, production network, clinical owner, real outbound email/SMS, model/tool execution or real U02 recipient. The repository default `dev` profile comment is **not** itself safe environment authorization; no implicit access to a company Oracle instance. If a safe Context cannot be started without a database connection or initializing side effects, stop with `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE` and request a separate explicit environment/test authorization; do not disable guards or fake bean truth.

## 2. Probe modes and allowlist

Two modes are **distinct**. A result from one cannot be silently promoted to the other:

| Mode | Authority | Allowed | Prohibited | Verdict |
|---|---|---|---|---|
| `TIER-0_STATIC_SOURCE_PROJECTION` | exact tracked source + dependency/annotation/config lookup | parse annotations, source methods, selected bean candidates, Spring AutoConfiguration conditions and configurations | actually running app, touching DB, claiming active bean/proxy identity | `SOURCE_ONLY` |
| `TIER-1_READ_ONLY_CONTEXT_PROBE` | separately authorized isolated synthetic ApplicationContext | bean registry/introspection, effective profiles, Spring transaction advisors, safe manager/EMF/DataSource identity **without connection allocation**, redacted mapping | SQL/DB connect, invoke service work, introspect secrets, `getConnection`, EntityManager query, writes, startup jobs | `CONTEXT_CONFIRMED` only where observed |

**Additional `TIER-2_CONNECTION_ATTESTATION` is OUT OF SCOPE.** For proof of actual JDBC connection enlistment/commit identity or MySQL/Oracle driver metadata, prepare a **separate controlled physical-evidence authorization**; a `DataSource` bean object alone does not prove the actual database bound to a transaction. `DataSource.getConnection()` is disallowed in TIER-1, including indirect health checks.

If the application bootstrap ordinarily runs Flyway, JPA schema DDL, auto-seeding, scheduled tasks, event listeners with outbound effects, JDBC validation or Hibernate database metadata fetch, the test harness must suppress those **with an independently reviewed test-only bootstrap configuration** and explicitly prove no such initialization occurred. It must not alter application production profile code or silently replace production wiring with fake beans and then claim the topology is representative. Record each bootstrap override and its implications as `OBSERVED_WITH_TEST_OVERRIDES`; unexpected access/side effects are a hard failure.

## 3. Exact consumer and Spring advisor inventory

Target calls (from source evidence; do not infer all are in the same live physical transaction):

| Node | Declared source fact | Required runtime observation |
|---|---|---|
| `U01ConsultationService.start` | public `@Transactional`; root U01 consumer | Bean type, proxy type, `TransactionInterceptor`, method advisor match, selected manager |
| `CanonicalBusinessEventLedger.resolveOrCreate` | public `@Transactional` REQUIRED; JPA Foundation | proxy/advice, transaction attr, target manager and EMF |
| `CDPManager.createCDP` | public `@Transactional` | proxy/advice, CDPRepository/EMF and participating manager |
| `CDPVersionService.createInitialVersion` | public `@Transactional` | advisor/propagation, initial-version repository/EMF |
| `RuntimeBindingService.bind` | public `@Transactional`; catch+read uniqueness | advisor/manager; cannot infer catch is rollback-safe |
| `ClinicalRunCoordinator.openRun` | public `@Transactional` | proxy/manager, original Event and Run repositories |
| `ConsultationWaitTransitionService.establish` | U06 `SERIALIZABLE`, Consultation row lock | distinct U06 owner graph/manager relationship, without invoking write |
| `JdbcU06DeliveryStore.reconcileConfirmed` | U06 JDBC `@Transactional`, `final` class | actual registered bean? proxy type? interceptibility? JDBC manager; mark absent/unproxied as `NOT_PROVEN` |
| `U07AdmissionApplicationService` | proposed in design, **absent** on inspected main | `NOT_IMPLEMENTED` (do not create a fake production bean to claim proof) |
| `U07CanonicalEventBindingRepository` | proposed V7, **absent** on main | `NOT_IMPLEMENTED`; future manager/source contract only |

For each discovered target bean, gather **non-secret**: declared bean name hash; bean definition resource/class (source safe identifier); actual class, proxy kind (`JDK_PROXY`, `CGLIB_PROXY`, `NONE`), Spring `Advised` advisors list and `TransactionAttributeSource` method attributes; bean registration and injected dependency edges, with unresolved `@Lazy`, scoped proxies or custom interceptors explicitly recorded. `@Transactional` annotation alone is insufficient: if an actual call is self-invoked, does not cross the proxy, method is final/nonpublic, or class/proxy cannot be advised, classify `PROXY_NOT_EFFECTIVE` or `UNKNOWN`.

**Boundary requirement:** map both direct Foundation references and U01 transitive dependencies, not every unconnected repository carrying an `@Transactional` annotation. The source scan's 51 broader candidate files are triage candidates, **not** 51 confirmed Foundation consumers. Additional dynamic/external readers remain `UNKNOWN` until owner certified, never `ABSENT` solely because lexical scan lacks a string.

## 4. Effective manager / JPA / DataSource topology

Freeze a typed topology record per owner path:

```text
U07EffectiveSpringTxTopologyV1:
  evidence_id
  source_head, git_tree_sha, dependency_digest
  execution_profile_id, active_profiles, context_fingerprint, override_manifest
  environment_grant_ref, explicit_read_only_scope_ref
  observed_at, observer_version, artifact_sha256
  beans[]:
    logical_target, hashed_bean_id, actual_type, proxy_type
    advisor_matches[], method_transaction_attributes
    effective_manager_resolution, manager_bean_token
    entity_manager_factory_token, persistence_unit_token
    datasource_bean_token, datasource_proxy_chain[]
    declared_vs_observed, unknown_reason
  edges[]:
    caller, callee, invocation_mode(PROXY|DIRECT|SELF|UNKNOWN)
    propagation, manager_resolution, potential_ambient_status
  manager_entities[]:
    manager_bean_token, manager_class, emf_token?, datasource_token?
    primary_qualifier_state, resolution_trace
  constraints[]:
    required_equivalence, observation_status(PASS|FAIL|UNKNOWN)
    evidence_ref, counterexample?
  access_attestation:
    get_connection_count, sql_attempt_count, mutation_count
    flyway_attempt_count, network_connection_attempt_count
    forbidden_activity_detected
  final_verdict:
    CONTEXT_CONFIRMED | SOURCE_ONLY | PROXY_NOT_EFFECTIVE |
    RESOURCE_MISMATCH | CONTEXT_UNSAFE | INCOMPLETE_EVIDENCE
```

**Tokenization:** logical tokens are generated by deterministic salted pseudonyms scoped to this evidence run. Do **not** collect or print JDBC URL, host, port, credentials, secrets, tenant identifiers, CDP content, SQL bind variables, table samples, stack traces with secrets, or actual database names. `DataSource` identity means **Java bean/resource identity and known proxy chain**, not a discovered production database URL. Ambiguous/dynamic managers are `UNKNOWN`; a `@Primary` selection must be proven by bean resolution, not guessed.

### 4.1 Required equalities (design obligations)

```text
E01:
  U01.start effective manager == Foundation.resolveOrCreate manager
E02:
  U01 manager == CDPManager == CDPVersionService manager
E03:
  U01 manager == RuntimeBindingService == ConsultationRepository manager
E04:
  U01 manager == ClinicalRunCoordinator == CanonicalEventRepository manager
E05:
  all U01 linked JPA repositories resolve to the corresponding same EMF
E06:
  exact matching manager/DataSource for future Foundation + U07 Binding
  = DESIGN_OBLIGATION_ONLY until U07 repository exists
E07:
  U07 nontransactional ingress + REQUIRES_NEW owned commit + independent read
  = NOT_IMPLEMENTED, never CONTEXT_CONFIRMED in current main
E08:
  U06 JDBC store/proxy classification = observed separate owner fact, not E01–E07
```

Equivalent *observed* bean names are not equivalent physical commit participation. TIER-1 topology observation may give `CONFIGURED_SAME_MANAGER`, but **not** `PHYSICALLY_SAME_CONNECTION_AND_COMMIT`; actual runtime enlistment and exception behavior need a later independently authorized physical test. If mismatched JPA managers/EMFs exist, mark `RESOURCE_MISMATCH` and block U07 readiness rather than introduce an implicit distributed transaction.

## 5. Read-only no-side-effect enforcement

Required independent protective instrumentation at the *test harness*, not production Runtime:

- Record no API request, no scheduled task, no async execution, no event listener writing state, and no outbound client attempt during context inspection.
- Ban calls to business service methods, repositories, `JpaRepository.save`, `EntityManager.flush/query`, `JdbcTemplate`, `DataSource.getConnection`, `DriverManager.getConnection`, Flyway migrate/repair, schema validation that accesses a live DB, and all F8/P01/P02/U02 entrypoints.
- Trap database/network access attempts at supported, test-only instrumentation surfaces. If interception coverage cannot prove `zero connection attempts`, downgrade to `INCOMPLETE_EVIDENCE` rather than assuming zero from empty logs.
- Inspect bean definitions/advisors via Spring introspection interfaces **without calling intercepted business methods** or forcing unsafe lazy bean initialization. If forced initialization would connect, stop.
- Avoid full ApplicationContext bootstrap if the service cannot start safely in complete offline isolation. Static TIER-0 always remains usable, but is **not equivalent** to a live effective manager report.
- Produce `no_side_effect_attestation` with counters (all zero), instrumentation coverage declaration and independent reviewer identity/authorization. Unexpected access => `CONTEXT_UNSAFE`, invalidate TIER-1 and fail closed.

## 6. Required evidence bundle and independent review gates

```text
U07EffectiveTxEvidenceBundleV1:
  manifest.json
  static_source_projection.json
  profile_and_override_manifest.json
  context_bean_inventory.json
  advisor_method_resolution.json
  manager_emf_datasource_topology.json
  consumer_edge_matrix.json
  side_effect_intercept_attestation.json
  raw_result_digest_manifest.json
  independent_comparator_result.json
```

Bundle must be immutable, individually SHA-256 hashed, tied to the exact source/runner/profile/owner grant and uploaded under a non-PHI retention policy. Raw fixture/Oracle source must have **independent provenance** from the instrumentation results. An owner or reviewer must sign off the scope of removed/redacted data, not merely rely on a text grep for credentials.

| Gate | Question | Failure condition |
|---|---|---|
| `TX-G01` | Exact source, profile, runner and artifact digest match? | any mismatch or missing immutable reference |
| `TX-G02` | Safe synthetic read-only environment explicitly granted? | absent owner/environment grant, dev/production ambiguity |
| `TX-G03` | Every relevant U01/Foundation consumer bean/edge present and classified? | missing target or unreviewed dynamic edge |
| `TX-G04` | Advisor/proxy and `TransactionAttributeSource` observations cover actual methods? | no effective advisor, self-invocation, final/non-proxied, UNKNOWN |
| `TX-G05` | Manager/EMF/DataSource bean equivalence positively observed? | mismatch, guessing from annotations, missing profile binding |
| `TX-G06` | No side effects or attempts, instrumentation coverage proven? | any DB/network attempts, uninstrumented surfaces, secrets/PHI |
| `TX-G07` | U07 *absent* physical sources and required future co-location correctly labelled? | marking future U07 beans/atomicity as observed |
| `TX-G08` | Independent comparator and human owner review accepted? | derived expected result from observed result, or skipped unknowns |

**Failure precedence:** `CONTEXT_UNSAFE` / privacy or forbidden effect > `INVALID_PROVENANCE` > `RESOURCE_MISMATCH/PROXY_NOT_EFFECTIVE` > `INCOMPLETE_EVIDENCE` > `SOURCE_ONLY` > `CONTEXT_CONFIRMED`. No failure code may be converted to a positive manager compatibility verdict by a retry or fallback.

## 7. Design-case matrix (not executed)

| Case | Input / controlled branch | Expected result |
|---|---|---|
| `TX-T01` | pinned correct HEAD and fully approved offline synthetic profile | immutable source/owner binding |
| `TX-T02` | source or manifest HEAD mismatch | INVALID_PROVENANCE; no probe |
| `TX-T03` | `dev` default points to non-isolated Oracle | CONTEXT_UNSAFE; no connection |
| `TX-T04` | U01 full proxy/manager/EMF graph resolves to one owner | CONFIGURED_SAME_MANAGER, **not** physical COMMIT proof |
| `TX-T05` | U01 service lacks actual transaction advisor | PROXY_NOT_EFFECTIVE |
| `TX-T06` | CDPVersion service or Binding branch resolves to different manager | RESOURCE_MISMATCH |
| `TX-T07` | Multiple manager beans with ambiguous qualifier or primary | INCOMPLETE_EVIDENCE |
| `TX-T08` | U06 JDBC store final/unproxied or absent | isolated U06 owner limitation; no false Foundation claim |
| `TX-T09` | proposed U07 admission class/repository absent in main | NOT_IMPLEMENTED; E06/E07 DESIGN_ONLY |
| `TX-T10` | bean initialization attempts SQL validation/Flyway migration | CONTEXT_UNSAFE, zero executed DB calls required |
| `TX-T11` | side-effect interception missing one network/JDBC surface | INCOMPLETE_EVIDENCE, never zero by default |
| `TX-T12` | unsupported proxy/reflection/dynamic consumer cannot be mapped | INCOMPLETE_EVIDENCE and owner review required |
| `TX-T13` | no DB access performed, manager beans mapped | only CONTEXT_CONFIRMED topology, not SQL/transaction atomicity |
| `TX-T14` | prohibited secret JDBC URL / credential / PHI appears in candidate output | INVALID_EVIDENCE; redact and re-review |
| `TX-T15` | valid topology but collision-after-FLUSH semantics untested | BF-SEM-02 remains OPEN; no Foundation gate PASS |
| `TX-T16` | context fails safe startup due to unavoidable DB health check | SOURCE_ONLY / NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE |

All cases require an independently reviewed expected outcome before any execution. Test cases do not grant rights to run the ApplicationContext or connect to an actual database.

## 8. Owner authority and closure decision

| Authority | Can decide | Cannot decide |
|---|---|---|
| Foundation/U01 owner | approve scope of bean topology inspection and consumer equivalence expectations | U07 physical grant, U06 issuance, P02/U15 state owner change |
| Security/privacy reviewer | approve local synthetic isolation, instrumentation/redaction and retention | mark transactional safety proven |
| U07 author | propose diagnostic harness and target topology | self-approve manager physical equivalence |
| Independent design reviewer | accept/reject this **contract** | infer CI execution from documentation |
| Independent evidence reviewer | accept actual exact-head, sanitized context topology results | infer connection atomicity or unique-key recovery without tests |
| U06/U15/P02 owners | authorize their separate state/fence/transaction interfaces | conflate own CAs with Foundation audit grant |

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
= DESIGN_CANDIDATE / NOT_AUTHORIZED / PENDING_INDEPENDENT_DESIGN_REVIEW

BF-U07-FOUND-SEM-01 = OPEN / CONTEXT_EVIDENCE_NOT_COLLECTED
BF-U07-FOUND-SEM-02 = OPEN / COLLISION_SEMANTIC_PROOF_SEPARATE
BF-U07-FOUND-AUD-01..04 = OPEN
BF-U07-FOUND-AUD-03 = LEXICAL_SUBGATE_COMPLETE / SEMANTIC_NOT_CLOSED

SOURCE_LEXICAL_INVENTORY_COMPLETENESS = PASS
EFFECTIVE_SPRING_MANAGER_TOPOLOGY = NOT_PROVEN
U07_PHYSICAL_ATOMIC_BINDING = NOT_IMPLEMENTED
MYSQL_ORACLE_COMMIT_AND_COLLISION_PROOF = NOT_EXECUTED

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next gate:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Independent Design Review` on the exact author design HEAD. After design acceptance, obtain **explicit separately scoped permission** before creating/running any test-only probe or physically bootstrapping ApplicationContext, then perform independently reviewed evidence collection. In parallel design `U07 Foundation Collision / U01 Transitive Rollback Semantic Proof` as a distinct owner-controlled investigation; it does not follow automatically from a read-only topology observation.

No application code or database interaction was performed or authorized by this design.
