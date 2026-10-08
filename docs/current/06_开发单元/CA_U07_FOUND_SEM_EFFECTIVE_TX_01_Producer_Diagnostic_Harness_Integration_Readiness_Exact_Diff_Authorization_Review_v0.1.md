# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Producer / Diagnostic Harness Integration Readiness + Exact-Diff Authorization Review v0.1

> Review date: 2026-10-08
> Design source: [PR #300](https://github.com/cxjchelsea/AIdoctor/pull/300), exact author HEAD `09736a2764a7ea5ba640fbc82bd87bc403ad0b52`; design blob `3d4b1ece54858afbe8795bdbc079f0b37d62641a`
> Independent design re-review: [PR #302](https://github.com/cxjchelsea/AIdoctor/pull/302), exact HEAD `b1fee57665aa1ed44a5e8859f76a5824224fc423`, `PASS / CONDITIONAL_DESIGN_ACCEPTANCE`
> Actual runtime source: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Decision: READY_WITH_FIXES for bounded Tier-0 producer design only; TIER-1 NOT_READY / EXACT_DIFF_NOT_AUTHORIZED.**
> No code, live ApplicationContext, database access, physical test, permission, or merge is authorized or performed by this document.

## 1. Review question and exact scope

Can the already **conditionally accepted** read-only Tier-0/Tier-1 topology contract be safely integrated into current AIdoctor's actual source, Maven test environment and CI **without introducing business logic, clinical/patient data access, incidental network/DB side effects or false effective-manager authority**? If so, what is the *smallest exact future implementation diff*, what risks remain, and what separate authorization is needed?

This document is a **pre-implementation readiness and exact-diff authorization review**, not a command to begin implementation. “Read-only” describes a future contract and does not certify that a Java/Spring bootstrap is safe. No approval for changing `CanonicalBusinessEventLedger`, `U01ConsultationService`, migrations, shared auth grants, PHI or production is implied.

## 2. Observed repository integration facts (exact main HEAD)

| Source / existing integration path | Exact blob SHA | Observed fact and impact |
|---|---|---|
| `diagnosis-service/pom.xml` | `dfa3c6f1cfc274ca796bf1bde8824ed1d9b80bb5` | Java 8, Spring Boot **2.7.8**, Spring Data JPA, Spring AOP, JDBC dependencies for MySQL/Oracle, Flyway **7.15.0**, Redis, Nacos discovery, OpenFeign, Web/Actuator and JUnit/Spring Boot test support. Any safe bootstrap must account for autoconfiguration/network/metadata effects; no H2 or sandbox support may be assumed. |
| `diagnosis-service/src/main/resources/application.yml` | `7a9d6931da6763f83769a5234ac8aa77459b0be0` | Contains `spring.profiles.active: dev` and comments describing dev Oracle/company deployment, MySQL local option. Neither profile is an approved offline synthetic environment. |
| `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/FoundationRuntimeBaseTest.java` | `3eef25acd7ceeaed80a29e09e866848bbf92f121` | Unit tests instantiate mocked repositories directly; no started Spring context, real JPA Manager/EMF/DataSource identity or rollback-only test evidence. |
| `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u03/U03NonProductionExecutionContextTest.java` | `9e2e3fd6dff5508944f6c894dd3ee8951868915e` | Tests explicit nonproduction command/release identity but are not a replacement for OS-level offline containment. |
| `.github/workflows/foundation-0-verification.yml` | `eb80668db53c9ac497de99a774434ec2b73d62a2` | Uses JDK 8, Maven compile, focused mocked Foundation/U01 tests and full diagnosis-service regression; **not** a Tier-1 isolated Spring topology probe. |
| `runtime/u01/U01ConsultationService.java` | `55948ef6de77693f546e43954754c14b7854d667` | `@Transactional start()` calls Ledger, `CDPManager.createCDP`, RuntimeBinding, Consultation save and ClinicalRun; never invoke from diagnostic probe. |
| `runtime/foundation/CanonicalBusinessEventLedger.java` | `3b7cb7191e6b10290b5c5cbb53d237c26b22b498` | Shared `@Transactional resolveOrCreate`, `save` and same-scope duplicate-key catch/read; separate collision proof required. |
| `service/cdp/CDPManager.java` | `993b4c5f2d313f7416d142378a67eb8d4f6491fa` | `@Transactional createCDP()`, transitive CDPVersion creation; baseline U01 consumer graph extends beyond direct Foundation imports. |
| `service/cdp/CDPVersionService.java` | `a4e8407120b20b3229f8402290ace57b9480b79e` | `@Transactional createInitialVersion()`; actual Spring proxy/manager not evidenced. |
| `runtime/foundation/RuntimeBindingService.java` | `da552124d4aea2faf1d5d24c779be5346d4d0e0d` | `@Transactional bind()`, uniqueness catch/re-read; no new shared service changes in this CA. |
| `runtime/foundation/ClinicalRunCoordinator.java` | `a0d991b7e0286e025e93227fa3e83ffec7fea5e1` | `@Transactional openRun()`, direct Canonical Event repository access; no synthetic Run should be minted by topology probe. |
| `runtime/u06/delivery/JdbcU06DeliveryStore.java` | `6b67b75bb121410a0a78b6cbf188a5369ad5d1fb` | JDBC + `SERIALIZABLE` declared in a `final` non-stereotyped class; possible injection/proxy semantics require separate U06 owner checks, not Foundation proof. |

**Previous executed lexical evidence:** [Actions run 37736999966](https://github.com/cxjchelsea/AIdoctor/actions/runs/37736999966) on exact main scanned 2,112 Git-tracked files / 256 matches (independent [PR #298](https://github.com/cxjchelsea/AIdoctor/pull/298)). This proves tracked-source lexical coverage only, **not** Spring bean topology.

## 3. Readiness gates and severity

| Check | Result | Explanation |
|---|---|---|
| `RD-01` exact code/design/evidence heads pinned | **PASS / SOURCE_LEVEL** | Full refs above and previous independent design acceptance |
| `RD-02` Tier-0 no-context source producer feasibility | **READY_WITH_FIXES** | Existing Python/Git metadata and Maven project suffice; proposed new producer+pure comparator, no Spring boot |
| `RD-03` Tier-1 pre-JVM OS sandbox isolation provision | **NOT_READY / BLOCKER** | No audited OS denial policy, permissions, isolation-runner/orchestrator or complete prebootstrap interception evidence in baseline |
| `RD-04` Boot/autoconfiguration side-effect-free representative context | **NOT_READY / BLOCKER** | Default dev, Flyway/JPA JDBC metadata, Redis/Nacos etc potentially initiate I/O; suppressing may change manager/EMF/DS topology |
| `RD-05` resolver fidelity against actual `TransactionInterceptor` / BeanFactory | **NOT_READY / BLOCKER** | R0–R9 specified but no pure/verified implementation, pinned framework-source comparator, exact qualifiers/advices/manager fallback Oracle |
| `RD-06` actual safe evidence/redaction/independent producer grants | **NOT_READY / REQUIRED** | Security+Foundation/U01 approval, signed environment grant, no-side-effect coverage/positive and negative expected cases absent |
| `RD-07` Tier-2 JDBC physical atomicity | **OUT_OF_SCOPE** | Cannot be inferred from Tier-1; must be separately authorized |
| `RD-08` business runtime/clinical blast radius | **ACCEPTABLE ONLY IF DIAGNOSTIC DIFF STRICTLY BOUNDED** | No changes to production business Java, DB migrations, deployed CI/profiles or owner contracts |

**Readiness decision:** `TIER-0_PREIMPLEMENTATION_SCOPE=READY_WITH_FIXES`; `TIER-1_CONTEXT_BOOTSTRAP=NOT_READY`; **no implementation authorized**, including Tier-0, until a named explicit bounded decision is issued. This distinguishes possible design feasibility from execution permission.

## 4. Exact proposed future diff inventory and prohibition list

This is a **normative review of proposed paths**, **not files created by this PR**. Existence/paths must be rechecked at the eventual authorized exact source HEAD.

### 4.1 Tier-0 standalone proposed minimal diff (authorization candidate, read-only)

| ID | Candidate path | Planned addition | Constraints |
|---|---|---|---|
| `D0-01` | `tools/u07_foundation_tx_topology/static_projection.py` | consume already independently accepted full tracked-source manifest and exact main SHA; extract U01→Foundation→CDP→Binding→Consultation→Run declared annotation/direct-edge candidates; record unknown | no Spring imports/runtime start, no network, no DB credentials, no mutation |
| `D0-02` | `tools/u07_foundation_tx_topology/static_schema.py` | strict `U07EffectiveSpringTxTopologyV1` source-only model, typed unknown/absent/proposed, SHA binding, path/line/blob digest verification | never emit `CONFIGURED_SAME_MANAGER` from static sources |
| `D0-03` | `tools/u07_foundation_tx_topology/test_static_projection.py` | synthetic deterministic unit cases for exact-HEAD/fixture mismatch, omission, transitive CDP version/Run, fail-closed UNKNOWN | Python stdlib preferred, isolated no patient data |
| `D0-04` | `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` | manifest of precise source inputs, expected output digest, redact policy and independent review acceptance | design/evidence-only |

**Optional future CI**: `.github/workflows/u07-effective-tx-tier0-source-projection.yml` only after an independently reviewed workflow-level no-network/no-secrets and exact-SHA execution contract. Existing `foundation-0-verification.yml` must remain unchanged in the first Tier-0 candidate. Avoid automatic branch-triggered probe executions.

### 4.2 Tier-1 proposed bounded diff — all **BLOCKED / NOT_AUTHORIZED**

| ID | Candidate path | Future purpose | Blocking prerequisite |
|---|---|---|---|
| `D1-01` | `tools/u07_foundation_tx_topology/sandbox/deny_network.sh` | OS prebootstrap container sandbox/permission/egress policy attestation and teardown | Security-reviewed actual isolation semantics, loopback/Unix/IPv4/IPv6/DNS+local write attack surfaces |
| `D1-02` | `tools/u07_foundation_tx_topology/sandbox/bootstrap_guard_manifest.json` | enumerate JDBC/JPA/Hibernate/DriverManager/HTTP/Redis/Nacos/Flyway/`@PostConstruct` attempted-effect detection coverage | owner-approved exhaustive coverage and redaction policy |
| `D1-03` | `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/topology/U07EffectiveManagerTopologyProbeTest.java` | bounded bean definition/AOP advisor introspection; **no business-method invocation** | explicit offline Tier-1 execution grant and representative context; no `@SpringBootTest` by default |
| `D1-04` | `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/topology/U07EffectiveManagerResolver.java` | R0–R9 exact actual transaction-interceptor/method-manager resolver with proof tokens | Spring Framework 5.x accurate comparator independently reviewed, no reflective business execution |
| `D1-05` | `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/topology/U07TopologyRedactionOracleTest.java` | test actual redaction, method advisor identity, manager/EMF/DS equivalence levels and failure precedence | independent Oracle/fixtures and safe no-I/O harness |
| `D1-06` | `tools/u07_foundation_tx_topology/evidence/verify_bundle.py` | artifact manifest, hashes, blocked-attempt counters, source/runner/profile/override reconciliation | physically emitted Tier-1 artifact and independent provenance |
| `D1-07` | `.github/workflows/u07-effective-tx-tier1-probe.yml` | **manual, never auto-triggered** isolated diagnostic workflow, exact ref, no credentials, no runtime side effects | independently verified sandbox runnable on selected runner; Security+Foundation/U01 owner grant and exact-diff authorization |

**Explicit denylist for this CA:** `diagnosis-service/src/main/java/**` (all production business/runtime), `diagnosis-service/src/main/resources/application.yml`, `db/migration/**`, `db/migration-oracle/**`, all production deployment configs, U01/CDP/Run/Binding authoritative stores, external HTTP/SMS/PHI connectors, P02/U15/U06 grants and any permission widening. Do not add an `application-dev` override or real DB credentials merely to make Tier-1 startup possible. A future Maven POM modification to provide a test dependency is **NOT** in the approved diff; require independent scope amendment.

## 5. Producer / Orchestrator wiring and evidence authority

```text
Independent design acceptance [already: PR #302]
  ↓
Tier-0 bounded exact-diff authorization decision [NOT_GRANTED]
  ↓
Tier-0 isolated static producer [NOT_IMPLEMENTED]
  ↓
Independent static comparator / artifact review [NOT_EXECUTED]
  ↓
Independent Security + Foundation/U01 owner offline sandbox readiness
  ↓
Tier-1 separately scoped exact-diff implementation authorization [NOT_GRANTED]
  ↓
OS isolation tested BEFORE JVM (negative bypass/sandbox attestation)
  ↓
Tier-1 sanitized Spring introspection only if representative bootstrap feasible
  ↓
Independent evidence-only readback / disposition
  ↓
BF-SEM-01 reassessment; BF-SEM-02 collision and real DB separate
```

The proposed Tier-0 producer **must reuse** the existing [PR #297](https://github.com/cxjchelsea/AIdoctor/pull/297) immutable lexical inventory rather than pretending to redo a new full clone. Any new source HEAD requires a new source manifest plus independent compatibility delta. A Tier-1 runner must not consume unrestricted source/tenant env variables as authority. The **independent Oracle** must be authored separately from observed metadata and bind exact input/output contracts, including method qualifier resolution and false-claim rejection.

**Hard stop conditions:** missing explicit authorization, unverified OS sandbox, forbidden *attempt* (including blocked external call), bootstrap fallback requiring Oracle/dev/credentials, altered manager/EMF/DS/advisor topology, uncaptured reflection/dynamic consumer, missing artifact digest => `NOT_EXECUTABLE`, `CONTEXT_UNSAFE`, `REPRESENTATIVENESS_NOT_PROVEN` or `INCOMPLETE_EVIDENCE` as applicable; never auto-escalate to actual DB access.

## 6. Exact-diff review findings / blockers

| Finding | Severity | Closure before Tier-1 authorization |
|---|---|---|
| `BF-U07-FOUND-TX-INT-01` | BLOCKER | demonstrate OS pre-JVM isolation executable in the actual intended runner and guard attempt coverage *without* any external or local effects |
| `BF-U07-FOUND-TX-INT-02` | BLOCKER | prove a representative offline Spring context is bootstrappable without Flyway/JPA/Redis/Nacos/network access and without invalidating E01–E05 manager/EMF/DS comparisons; otherwise record SOURCE_ONLY and disallow Tier-1 |
| `BF-U07-FOUND-TX-INT-03` | BLOCKER | independently validate Spring transaction interceptor/qualifier/default/BeanFactory resolution comparator against actual Framework 5.x dependency and mixed-manager negative cases |
| `RF-U07-FOUND-TX-INT-01` | REQUIRED | require Security + Foundation/U01 synthetic isolation, privacy/no-PHI and consumer-owner grants, exact source/profile and expected-case provenance |
| `RF-U07-FOUND-TX-INT-02` | REQUIRED | ensure Tier-0 code is read-only, does not call business services, and cannot accidentally produce Tier-1 positive verdict |
| `RF-U07-FOUND-TX-INT-03` | REQUIRED | maintain exact diff path allowlist; reject extra Maven dependency, production files or CI auto-run triggers without new review |

**Important sequencing resolution:** a narrow **Tier-0 source-only implementer** can be scoped and separately authorized before Tier-1 feasibility is complete, because Tier-0 does not need Spring or a database. Such authorization must be explicitly limited to D0-01..04 and cannot silently authorize Tier-1 files D1-01..07 or any ApplicationContext execution. No authorization has been issued here.

## 7. Formal readiness and authorization review decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Producer / Diagnostic Harness Integration Readiness
= READY_WITH_FIXES / BOUNDED_TIER0_ONLY

EXACT_DIFF_TIER0 = D0-01..04 / CANDIDATE_FOR_SEPARATE_AUTHORIZATION
EXACT_DIFF_TIER1 = D1-01..07 / NOT_READY / NOT_AUTHORIZED

BF-U07-FOUND-TX-INT-01 = OPEN
BF-U07-FOUND-TX-INT-02 = OPEN
BF-U07-FOUND-TX-INT-03 = OPEN
RF-U07-FOUND-TX-INT-01..03 = REQUIRED

Tier0 source evidence producer = NOT_IMPLEMENTED / NOT_AUTHORIZED
Tier1 offline context probe = NOT_IMPLEMENTED / NOT_AUTHORIZED
Spring manager topology = NOT_PROVEN
Physical JDBC/MySQL/Oracle = NOT_EXECUTED

BF-U07-FOUND-SEM-01 = OPEN
BF-U07-FOUND-SEM-02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
SOURCE_LEXICAL_INVENTORY_COMPLETENESS = PASS

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next authorized-to-discuss decisions:**
1. `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Exact-Diff Limited Implementation Authorization Decision` — explicit approve/reject D0-01..04 under static, offline, no-PHI scope.
2. Independently `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-1 Sandbox Feasibility / Isolation Authority Design` resolving BF-INT-01..03 **before** requesting Tier-1 authorization.

This review does not implement files from §4, does not execute any probe and does not request or make a user approval automatically.
