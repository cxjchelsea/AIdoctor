# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A Pre-Bootstrap Implementation Readiness & Exact-Diff Authorization Review v0.1

> Date: 2026-10-09
> **Baseline:** `main@86e8843197091c8c8172b7e4213537a31bdf0654`; verified latest commit in GitHub repository at assessment time.
> Design input: [PR #319](https://github.com/cxjchelsea/AIdoctor/pull/319) exact HEAD `7f9b056dde656e69bf7a061f0e41ba6cb8b832ca`, blob `370bd1dd4fac8289a2062c221a781eb599f4a1c5`.
> Independent conditional design re-review: [PR #321](https://github.com/cxjchelsea/AIdoctor/pull/321) exact HEAD `8ad0a89d072c74e7b31cf0d6f0334e667f802658`, report blob `e8e2acb70869e4c0b8900d73b9f4cb3f0da290b3`.
> Governing prior contract [PR #300](https://github.com/cxjchelsea/AIdoctor/pull/300), integration blocker register [PR #303](https://github.com/cxjchelsea/AIdoctor/pull/303); Tier-0 completion PR #311/#317/#318.
> **Decision: STAGE_A_READY_WITH_BLOCKERS / IMPLEMENTATION_AUTHORIZATION_NOT_GRANTED.** Assessment only; no sandbox, JVM, CI, Spring, MySQL/Oracle, PHI or production execution; no merge.

## 1. Distinct questions and authority

Determine (i) whether bounded Stage A **non-Spring preparation** can be authorized now, (ii) what exact files may eventually be created, and (iii) whether test execution, Tier-1 context boot or any further grant follows. None follows implicitly. Stage A1 = pre-JVM OS sandbox/guard support. Stage A2 = pure, synthetic comparator source/test + independent expected-value Oracle under pinned Spring Framework dependency. Stage B = actual isolated Spring Context topology diagnostic, explicitly excluded.

This decision **does not overrule** independent PR #321: three PB-IR design findings are closed at conditional design level, while `BF-U07-FOUND-TX-INT-01..03` remain OPEN pending runnable evidence. A code-readiness gate must not require full Stage B before narrower Stage A authorization, but it **does** require an exact enforceable Stage A contract.

## 2. Source and provenance recheck

| Evidence | Independent observed disposition |
|---|---|
| GitHub latest `main` | `86e8843197091c8c8172b7e4213537a31bdf0654` at query time |
| PR #319 | HEAD `7f9b056dde656e69bf7a061f0e41ba6cb8b832ca`, 1 design Markdown file, author base same main |
| PR #321 | HEAD `8ad0a89d072c74e7b31cf0d6f0334e667f802658`, conditional design re-review only |
| `diagnosis-service/pom.xml` | Git blob `dfa3c6f1cfc274ca796bf1bde8824ed1d9b80bb5`; Java 8, Spring Boot 2.7.8, Spring AOP/JPA/Flyway 7.15.0, redis/Nacos, MySQL/Oracle dependencies |
| `diagnosis-service/src/main/resources/application.yml` | Git blob `7a9d6931da6763f83769a5234ac8aa77459b0be0`; `dev` active, **not** safe environment authority |
| Actual selected runner OS/kernel and sandbox capability | **NOT_PROVIDED / NOT_ATTESTED** |
| Actual locally resolved Spring Framework jars, classpath digest and independent executable Oracle | **NOT_PROVIDED / NOT_PINNED** |
| Security + Foundation/U01 owner environment/implementation authorization | **NOT_EVIDENCED** |

## 3. Readiness gates

| Gate | Requirement | Decision |
|---|---|---|
| SA-G01 | Exact source/design review hashes and no source drift | **PASS / DESIGN_SOURCE** |
| SA-G02 | Tier-0 SOURCE_ONLY unchanged, no manager/EMF/DS false claim | **PASS / CONTRACT** |
| SA-G03 | A1 specified concrete runner OS/kernel, UID/capabilities, egress/mount/process/socket denial mechanism, security owner | **BLOCKED** |
| SA-G04 | A1 non-Spring canary run/attempt/origin/teardown Oracle and independent pre-JVM guard coverage, explicit execution separation | **SPECIFIED_DESIGN / NOT_VERIFIED** |
| SA-G05 | A2 R0–R9 comparator and 12 synthetic negative cases bounded | **PASS / CONDITIONAL_DESIGN** |
| SA-G06 | A2 fixed independent Oracle inputs/outputs with exact fixture artifact/hash and independent provenance | **BLOCKED / ACTUAL_ORACLE_ABSENT** |
| SA-G07 | A2 locally resolved Spring Framework 5.x source/jar/classpath dependency identity, offline Maven feasible | **BLOCKED / DEPENDENCY_EVIDENCE_ABSENT** |
| SA-G08 | Controlled amendment for new D1-04-T/O/M files, exact runnable test runner and reviewer approval | **BLOCKED / AMENDMENT_NOT_AUTHORIZED** |
| SA-G09 | Security/privacy, Foundation/U01 owner, explicit bounded implementation grant | **PENDING / NOT_GRANTED** |
| SA-G10 | Separate implementation vs execution authority, no Spring/DB/production/PHI/auto CI | **PASS / CONTRACT_ONLY** |

**Stage A1 verdict = NOT_READY_FOR_IMPLEMENTATION_AUTHORIZATION** until chosen runner/sandbox implementation primitives and independent security authority are frozen. A1 design-only offline narrowing may proceed, but no file-creation grant follows.
**Stage A2 verdict = NOT_READY_FOR_IMPLEMENTATION_AUTHORIZATION** until amended exact-file inventory, independently pre-frozen Oracle and resolved offline Framework artifacts exist. A2 design preparation may proceed; implementation remains unauthorized.

## 4. Normative exact future file inventory (six candidate files, not implementation authorization)

| ID | Candidate repository path | Stage | Scope |
|---|---|---|---|
| D1-01 | `tools/u07_foundation_tx_topology/sandbox/deny_network.sh` | A1 | OS-specific deny policy/attestation tool; cannot claim platform support without chosen runner |
| D1-02 | `tools/u07_foundation_tx_topology/sandbox/bootstrap_guard_manifest.json` | A1 | pre-JVM prevention / attempt-detection coverage, fail-closed unknown |
| D1-04 | `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/topology/U07EffectiveManagerResolver.java` | A2 | pure effective Spring method/manager resolver, no business invocation |
| D1-04-T | `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/topology/U07EffectiveManagerResolverTest.java` | A2 | standalone synthetic negative-case testing |
| D1-04-O | `tools/u07_foundation_tx_topology/oracle/stage_a2_cases.json` | A2 | **independently authored** expected outcomes and fixtures; must be frozen before comparator evidence |
| D1-04-M | `tools/u07_foundation_tx_topology/oracle/stage_a2_dependency_manifest.json` | A2 | local Maven resolved classpath/jar SHA proof |

The three `D1-04-T/O/M` files are **not part of the original PR #303 D1-01..07 allowlist** and require an accepted *controlled exact-diff amendment* before implementation. No permission to create, rename or run these files follows from their listing. The future Stage B D1-03/D1-05/D1-06/D1-07 remains entirely blocked.

### Prohibited changes and behavior

Never silently change `diagnosis-service/src/main/java/**`, `diagnosis-service/src/main/resources/application.yml`, production properties, either migration tree, authority-grant schemas, owner records, clinical endpoints, `pom.xml`, pre-existing Tier-0 parser/schema/test, network configuration, or production secrets. No `@SpringBootTest` full context, JPA query, JDBC `getConnection`, SQL, Flyway, remote Nacos/Redis, local/remote sockets, PHI, actual patients, production, external API, scheduled probe or automatic CI trigger. No run or merge from this assessment.

## 5. Stage A1 narrow preauthorization blockers

**SA-BF-01 RUNNER_IDENTITY_AND_PREVENTION_NOT_FROZEN** — Must identify exact selected OS/kernel and runner, container privilege/capability support, network and filesystem restrictions including loopback/IPv4/IPv6/DNS/Unix sockets, child process, host sockets and metadata, no credentials, no host network. The `deny_network.sh` filename is not evidence of enforceability. A named Security reviewer must independently accept the specific implementation threat model before code grant.

**SA-BF-02 CANARY_PHASE_EVIDENCE_AND_EXECUTION_AUTHORITY_MISSING** — Freeze stage-specific `SANDBOX_CANARY` vs `SPRING_DIAGNOSTIC` counters, sandbox instance IDs, teardown/new-instance re-attestation and fail-closed phases. For Stage A1, distinguish authorization to **write shell/manifest** from authorization to **execute non-Spring canaries**. Before any canary test, a separate run approval and safe synthetic endpoint model are necessary. No JVM is allowed in this stage.

## 6. Stage A2 narrow preauthorization blockers

**SA-BF-03 FRAMEWORK_ARTIFACT_AND_ORACLE_NOT_FROZEN** — Boot 2.7.8 POM is insufficient to independently pin actual runtime Spring 5.x transaction classes. Capture an offline-resolved, artifact-hashed dependency/classpath manifest, source provenance and deterministic independent fixture/expected results for TX-R01..12. Prohibit self-referential Oracle derivation and unexpected dependency downloads.

**SA-BF-04 EXACT_DIFF_AMENDMENT_NOT_ACCEPTED** — Need a formal controlled amendment accepting additional test/oracle/manifest paths `D1-04-T/O/M`, precise fixtures, test entrypoint and dependency origin. Treat lack of actual fixture bytes/hash as a readiness gap rather than fabricating them. Test code cannot be added under D1-04-only scope by implication.

**SA-RF-01 OWNER/SECURITY_PERMISSION_REQUIRED** — Security + Foundation/U01 reviewers must approve isolation, synthetic-only environment, immutable artifacts, redaction/retention, and explicit grant scopes. Neither this report nor prior PR #321 is an implementation approval.

## 7. Authorization decision separation

```text
STAGE_A1_DESIGN_SCOPECHECK = PASS_CONDITIONAL
STAGE_A1_IMPLEMENTATION_READINESS = NOT_READY
STAGE_A1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
STAGE_A1_CANARY_EXECUTION_AUTHORIZATION = NOT_GRANTED

STAGE_A2_DESIGN_SCOPECHECK = PASS_CONDITIONAL
STAGE_A2_IMPLEMENTATION_READINESS = NOT_READY
STAGE_A2_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
STAGE_A2_UNIT_TEST_EXECUTION_AUTHORIZATION = NOT_GRANTED

STAGE_B_SPRING_CONTEXT = NOT_AUTHORIZED
TIER1_IMPLEMENTATION = NOT_AUTHORIZED
TIER1_PHYSICAL_DB = NOT_AUTHORIZED
```

The assessment can outline future authorization conditions but cannot unilaterally replace the user's and respective owners' explicit grants. This review records **NO implementation authorization**, not a conditional authorization to start writing code.

## 8. Ordered next work

1. **`CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Isolation Authority + Exact Sandbox Design`**: choose explicit nonproduction runner, OS/kernel primitive, rootless capabilities, no-connect canary fixtures, independent security/owner approval contract, guard coverage and file-specific design; do not run canaries without a separate scoped execution grant.
2. **`CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A2 Controlled Exact-Diff Amendment + Independent Oracle/Classpath Design`**: obtain exact deterministic fixture schema/cases and reviewed expected results, dependency version/hash capture procedure; amend D1-04-T/O/M exact file allowlist before code grant.
3. Independent design re-review of both bounded deliverables. Thereafter **separate** Stage A1 and A2 scoped implementation authorization decisions; reserve actual canary/test execution, Spring Context and physical JDBC for later gates.
4. After real Stage A evidence, reconsider `BF-U07-FOUND-TX-INT-01..03` and `SEM-01`. `SEM-02`, `BF-U07-FOUND-AUD-01..04` and Foundation Gate remain OPEN / NOT_PASSED.

## 9. Final decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A_PREBOOT_IMPLEMENTATION_READINESS_EXACT_DIFF_AUTHORIZATION_REVIEW
= READY_WITH_BLOCKERS / NOT_AUTHORIZED

BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
AUTHOR_PR319 = 7f9b056dde656e69bf7a061f0e41ba6cb8b832ca
INDEPENDENT_DESIGN_REVIEW_PR321 = 8ad0a89d072c74e7b31cf0d6f0334e667f802658
SOURCE_ONLY_TIER0 = VERIFIED / MERGED

SA-BF-01..04 = OPEN
SA-RF-01 = REQUIRED
STAGE_A1_IMPLEMENTATION = NOT_READY / NOT_GRANTED
STAGE_A2_IMPLEMENTATION = NOT_READY / NOT_GRANTED
STAGE_A1_EXECUTION = NOT_GRANTED
STAGE_A2_EXECUTION = NOT_GRANTED
STAGE_B_SPRING_PROBE = NOT_AUTHORIZED
BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
PHI / PROFILE-A / PRODUCTION = BLOCKED
```

Review-only output. It is not approval by a separate human reviewer; it does not modify production code, run a database, change privileges, or authorize a merge.
