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
