# U07 D5-O2 — U02/U03 Java Dedicated-Test Ownership Matrix v0.1

> Design only / exact source main `33c0914e843d7976c7ff2272338377aa4925f443`; PR #465 technical candidate. **Not** authorization to change workflows, skip tests, waive CI, merge or deploy.
> Evidence: U02/U03 Java run `38339922286`/`38339922173`; U02 Java regression 473 tests / 0 failures / 38 errors / 1 skipped. This matrix maps the **11 distinct classes printed in the U02 error summary**, not a claim to have enumerated every class in the entire repository.

## 1. Exact failed-class ownership: static main workflow selector mapping

| No. | Class (all package prefix `com.aidoctor.diagnosis.runtime.u07.d5o2`) | Dedicated runner file | Dedicated `-Dtest` binding | Generic U02/U03 admission |
|---|---|---|---|---|
| A01 | `U07D5O2StageAMultiOwnerTest` | `u07-d5-o2-stage-a.yml` | Explicitly selected | Exclude only after dedicated-run coverage evidence |
| A02 | `U07D5O2StageANegativeTest` | `u07-d5-o2-stage-a.yml` | Explicitly selected | Same |
| A03 | `U07D5O2StageATopologyTest` | `u07-d5-o2-stage-a.yml` | Explicitly selected | Same |
| A04 | `U07D5O2StageATxPhysicalTest` | `u07-d5-o2-stage-a.yml` | Explicitly selected | Same |
| B01 | `stageb.StageBJdbcFaultHookTest` | `u07-d5-o2-stage-b-p0.yml` | Explicitly selected | Same |
| B02 | `stageb.StageBTxOwnerBindingTest` | `u07-d5-o2-stage-b-p0.yml` | Explicitly selected | Same |
| F01 | `stageb.forensics.StageBFDeadlockForensicsTest` | `u07-d5-o2-stage-b-forensics.yml` | Explicitly selected | Same |
| P01 | `stageb.p1.StageBP1AtomicityTest` | `u07-d5-o2-stage-b-p1.yml` | Explicitly selected | Same |
| P02 | `stageb.p1.StageBP1ReplayConflictTest` | `u07-d5-o2-stage-b-p1.yml` | Explicitly selected | Same |
| PB01 | `stageb.p1b.StageBP1BPartialLedgerTest` | `u07-d5-o2-stage-b-p1b.yml` | Explicitly selected | Same |
| PB02 | `stageb.p1b.StageBP1BReplayConflictTest` | `u07-d5-o2-stage-b-p1b.yml` | Explicitly selected | Same |

All dedicated runner paths have `.github/workflows/` prefix. All 11 are present at the exact main SHA and each appears literally in its dedicated runner's `-Dtest` selection. This is static ownership; it does **not** prove each current runner passed at this exact main SHA.

## 2. Why generic U02/U03 CI fails, without inventing base physical results

Main `.github/workflows/u02-verification.yml` and `.github/workflows/u03-verification.yml` both execute generic `mvn -f diagnosis-service/pom.xml test` without an isolated U07 D5-O2 MySQL8 service/profile. Main `StageBP1BTestConfig` requires `GITHUB_ACTIONS=true`, `U07_D5O2_P1B_PROFILE=U07_D5O2_STAGE_B_P1B_SYNTHETIC`, `U07_D5O2_P1B_ISOLATED=true`, exact JDBC URL, and later writer credentials; otherwise raises `P1B_NOT_DISPOSABLE_RUNNER`. This is observed in PR #465 U02/U03 job logs, demonstrating a **static runner selector/guard mismatch**, not an independently run main failure comparison.

`P1B_NOT_DISPOSABLE_RUNNER` is a correct safety guard. Do not weaken it merely to green CI. Some A/B tests may fail due to distinct disposable-runner guards; map their exact conditions individually in the implementation-readiness review. Avoid claiming all 38 Errors are caused by the P1B guard alone.

## 3. Dedicated owner contracts

| Family | Job | Synthetic DB/port | Runner category |
|---|---|---|---|
| Stage A A01–A04 | `u07-d5-o2-stage-a.yml` | MySQL8, `33326` | Real source topology/transaction |
| Stage B B01–B02 | `u07-d5-o2-stage-b-p0.yml` | MySQL8, `33327` | Isolated transaction/fault test |
| Stage B P01–P02 | `u07-d5-o2-stage-b-p1.yml` | MySQL8, `33328` | Synthetic six-row atomicity |
| Stage B PB01–PB02 | `u07-d5-o2-stage-b-p1b.yml` | MySQL8, `33329` | Partial/replay conflict |
| Stage B F01 | `u07-d5-o2-stage-b-forensics.yml` | MySQL8, `33331` | L0 forensic observer |

All these runner workflows are **pull_request-triggered**. A prior PR-head success does not equal a fresh physical verification of exact main `33c0914e`. A missing dedicated check is NOT covered simply because the YAML mentions the class.

## 4. Exact proposed CI remediation (no implementation authorized)

**Option A — preferred narrow selection:** Maintain a versioned manifest of exactly 11 full-class exclusions for generic U02/U03 `mvn test`. The manifest is mirrored into the two generic CI selectors or a separately reviewed Surefire profile, while dedicated runners continue selecting *all* these classes. Do not add `-DskipTests` or exclude the entire `u07` tree.

**Option B — alternative:** Use explicit physical-test categories (JUnit tags or Maven test source-set split). Because tagging edits individual Java test sources, this exceeds minimal CI-only scope and needs a separate impact review and explicit file inventory. Not automatically preferred.

**Exclusion coverage invariant:** `GenericExcludedTestClassNames == DedicatedSelectedTestClassNamesWithinIsolationCategory` for all approved classes. Detect new `*Test.java` files with disposable-runner guards, fail CI if orphaned, and fail when a dedicated workflow no longer executes an excluded class. Compare discovered test count before/after, expected ordinary coverage set and isolation test owner coverage. Do not rely on filename wildcard alone.

**Implementation scope not frozen:** Exact Maven Surefire CLI `-Dtest=...` exclusion parsing and test discovery must be verified in disposable Java8 CI; `-Dtest` may override rather than complement the original standard regression. A safe concrete selector will require its own independent review, and may modify `.github/workflows/u02-verification.yml`, `.github/workflows/u03-verification.yml` and a dedicated test-inventory checker; all such changes need separate authorization.

## 5. Evidence / independent admission checklist

1. Exact source SHA, same-profile U02/U03 comparison on base main, Java8, target CI configuration and test outputs; until then `EXACT_BASE_CI_ATTRIBUTION=PENDING`.
2. Fixed inventory of 11 failed classes and dedicated runner test selectors, with proof from source and job commands.
3. Each mapped class in dedicated runner has a physical test pass on exact source or an explicit pending status; no hypothetical green certification.
4. Ordinary suite regression preservation: no missing non-isolated test class, no broad accidental exclusions, no `skipTests`.
5. No redaction/privilege guard weakening, no Patient/real DB credentials or external delivery.
6. Targeted independent re-review before authorizing any workflow edits.

**Current decisions:** `DEDICATED_TEST_STATIC_OWNERSHIP=ALL_11_MAPPED`; `DEDICATED_EXACT_MAIN_PHYSICAL_COVERAGE=PENDING`; `EXACT_BASE_SAME_PROFILE_REPRODUCTION=PENDING`; `U02_U03_REMEDIATION_DESIGN=READY_FOR_RE_REVIEW`; `U02_U03_REMEDIATION_IMPLEMENTATION=NOT_AUTHORIZED`; `PR465_MERGE_AUTHORIZATION=NOT_GRANTED`.

---

## 6. Exact-class selection adversarial design — SEL-01..16 (DESIGN ONLY)

**Scope:** The main `.github/workflows/u02-verification.yml` and `.github/workflows/u03-verification.yml` both execute full `mvn -f diagnosis-service/pom.xml test`, which picks up disposable-MySQL8 tests whose runtime guards are not satisfied. The exact 11 failing class names and 5 dedicated `-Dtest` owners in §1–3 are the current candidate isolation manifest. Neither the Maven Surefire exclude syntax nor a repaired run has been physically verified yet.

**Candidate least-change selector:** retain normal Maven test discovery and explicitly remove *exactly* the 11 dedicated full-class identities via a versioned exclusion manifest, scoped to the generic U02/U03 CI job only. Never edit the individual isolated-test guards, never set synthetic DB env vars in generic CI, and never introduce broad `**/u07/**` patterns or `-DskipTests`. A Surefire `-Dtest` negative selector like `-Dtest='*,!SomeClass,...'` is ONLY a candidate until its exact Surefire 2.22.2 behavior and package/collision semantics are empirically verified. The Maven `-Dtest` argument may replace normal selection; if it loses ordinary test coverage, reject it and consider a separately authorized explicit plugin/profile configuration. No change to `diagnosis-service/pom.xml` is authorized here.

**Build/evaluation stages:**
- `SEL-P0` enumerate SOURCE HEAD `33c0914e` all test source FQCNs and ordinary Maven/Surefire discovered test class names. Preserve pinned full inventory and counts, not just the 11 prior failures.
- `SEL-P1` parse each five dedicated YAML file's exact `-Dtest` names and compare with the 11 allowlisted exclusions. Require `11/11`, exactly one designated dedicated owner per class, zero orphan or duplicated owner entries.
- `SEL-P2` run baseline *generic* U02 and U03 same-profile test commands on exact SOURCE main to establish what fails without specialized env. Do not claim this is already executed.
- `SEL-P3` test proposed exclusions with Java8 and exact Surefire provider. Compare class/test-case `surefire-reports` XML to full selection. **Invariant:** `DiscoveredOrdinaryBefore == ExecutedOrdinaryAfter`; no ordinary test silently skipped; each excluded class absent from generic executed list and present in the dedicated suite plan.
- `SEL-P4` execute all five dedicated runners against appropriate exact-source evidence, or formally mark `PENDING` for unavailable main-only reruns; a `PASS` is prohibited solely from YAML selector coverage. Dedicated test guards stay authoritative.
- `SEL-P5` run deterministic mutant controls below and independent CI review before any code/CI implementation authorization.

| Negative ID | Adversarial mutation | Required outcome |
|---|---|---|
| SEL-N01 | Exclusion missing one of 11 dedicated classes | fail inventory equality |
| SEL-N02 | Extra exclusion of ordinary unrelated Java test | fail ordinary coverage invariant |
| SEL-N03 | Broad exclude `**/u07/**` or `*Stage*` | fail selector validation |
| SEL-N04 | Case typo, package collision or duplicate simple class name | fail FQCN disambiguation |
| SEL-N05 | Class renamed/moved while manifest unchanged | fail source-to-owner match |
| SEL-N06 | Dedicated YAML drops selected class | fail ownership coverage |
| SEL-N07 | Same class ambiguously selected by two unrelated dedicated runner owners | fail unique owner contract |
| SEL-N08 | `-DskipTests`, `maven.test.skip`, `failIfNoTests=false`, or zero tests from `-Dtest` | fail execution gate |
| SEL-N09 | Surefire provider loads zero JUnit5 tests | fail executed XML count |
| SEL-N10 | New `*Test.java` with disposable env guard but no dedicated owner | fail orphan discovery |
| SEL-N11 | Inject synthetic DB profile into generic U02/U03 to bypass guard | fail runner profile separation |
| SEL-N12 | Dedicated job skipped/failed but generic exclusion treated as covered | fail physical coverage evidence gate |
| SEL-N13 | Test class disappears from ordinary suite due negative-selector override | fail ordinary before/after equality |
| SEL-N14 | Manifest based on candidate PR HEAD rather than frozen main source | fail SHA provenance equality |
| SEL-N15 | Failed physical test misclassified as test skipped or optional | fail required-check invariant |
| SEL-N16 | Newly added main-exact class inadvertently included in generic suite | fail missing category/owner |

**Executable result minimum:** U02 Java focused tests remain explicitly executed; U03 predecessor focused tests remain explicitly executed; generic regression suite emits machine-readable XML for every non-isolated class and retains all their assertions. The negative test matrix must include both discovery and execution path and cannot rely solely on command text inspection. A generic failure caused by isolated-runner guard is a truthful failure of the current CI job, not evidence of a broken business Store nor a waiver.

**CI implementation candidate, separate authorization:** only `.github/workflows/u02-verification.yml`, `.github/workflows/u03-verification.yml` and one immutable manifest/validator new file, if feasible; exact three-file scope is provisional until class exclusion command has been proven. Changes to source tests, POM, existing dedicated workflow, or exclusion by broad regex are escalations requiring renewed scope approval.

**Gate:** `CLASS_TO_OWNER_STATIC_MAPPING=PASS_11_OF_11`; `CI_EXACT_CLASS_SELECTOR_DESIGN=SUBMITTED`; `SUREFIRE_NEGATIVE_SELECTION_PHYSICAL_PROOF=PENDING`; `U02_U03_EXACT_BASE_SAME_PROFILE_RUN=PENDING`; `DEDICATED_PHYSICAL_COVERAGE=PENDING`; `CI_IMPLEMENTATION_AUTHORIZATION=NOT_GRANTED`.
