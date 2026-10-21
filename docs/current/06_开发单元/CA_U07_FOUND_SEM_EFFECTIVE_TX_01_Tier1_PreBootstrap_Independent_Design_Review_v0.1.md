# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-1 Pre-Bootstrap Readiness Remediation Independent Design Review v0.1

> Review date: 2026-10-08  
> Author design: [PR #319](https://github.com/cxjchelsea/AIdoctor/pull/319) EXACT HEAD `75ab78058802970c9f5b1a08ca84950560586cd8`, design blob `fb7cc239da5f327974e92313ee3064bcd8699376`.  
> Author base `main@86e8843197091c8c8172b7e4213537a31bdf0654`. PR #319 exact diff: **one ADD-only Markdown file / 174 additions / zero deletions**.  
> Normative inherited sources: PR #300 design @ `09736a2764a7ea5ba640fbc82bd87bc403ad0b52`, PR #302 conditional design PASS @ `b1fee57665aa1ed44a5e8859f76a5824224fc423`, PR #303 earlier Tier-1 NOT_READY review @ `00c81cb4f7ddda60d1f846063030cca9a3131f38`; PR #311/317/318 Tier-0 source-only implementation, merge and offline evidence.  
> **Review verdict: REVISE_REQUIRED / DESIGN_NOT_INDEPENDENTLY_ACCEPTED**. This is evidence-only review, not code authorization, Spring start, test execution, production access, Tier-1 grant or merge.

## 1. Independent review scope and limits

Independently read the exact author design and compared its R1, R2, R3, PB-G01..09, TX-R01..12, D1 staged file allowlist and permission requirements to the already accepted baseline. Verified GitHub PR head and changed filename. No test runner, JVM, sandbox, CI or external physical environment executed; no claim of physical enforcement or Spring runtime representation. Reviewer output is kept in a separately rooted, one-file design review PR rather than amending the author branch.

## 2. Finding disposition

| Finding | Severity | Author location | Independent assessment | Required precise amendment |
|---|---|---|---|---|
| BF-U07-FOUND-PB-IR-01 | **BLOCKER** | R1 P2/P3/P5; PB-G02/03; stop policy | P2 intentionally exercises prohibited network/filesystem canaries, but P5 and contract say **any** forbidden attempt is CONTEXT_UNSAFE. Without separated scopes/counters, an intentionally blocked isolation-test attempt contaminates the eventual real probe or can be excused as a canary. | Freeze **distinct subprocess/namespace/phase evidence** `SANDBOX_CANARY` vs `SPRING_DIAGNOSTIC`, dedicated counters and immutable cutoff; canary requires denied outcomes, zero successes and no external real target; reset/teardown and independent reattestation before JVM. Any forbidden attempted effect *during Spring phase* must be CONTEXT_UNSAFE, and phase-origin ambiguity must fail closed; no blanket whitelist for runtime canaries. |
| BF-U07-FOUND-PB-IR-02 | **BLOCKER** | R2-A/B override impact; R2 verdict lattice; PB-G04 | `unchanged by overrides` cannot be physically compared with an untouched target that the contract forbids booting in its original external environment. Comparing only a potentially substituted test-context graph to itself cannot certify representativeness. | Freeze **independent reference-topology basis** (pinned source/config/auto-config conditions, BeanDefinition expectations, profile property origin and resolved dependency/classpath), per relevant manager/advisor/EMF/DS node an unchanged/changed/unresolved proof criterion; admit `CONFIGURED_SAME_MANAGER` only where target effective selection is independently evidenced without a real DB connection, otherwise `REPRESENTATIVENESS_NOT_PROVEN`. Missing original-context evidence cannot be invented. |
| BF-U07-FOUND-PB-IR-03 | **BLOCKER** | R3 §5.2; Stage A2 D1-04; PB-G05 | 12 cases are table-level candidate expectations, but the **independently authored executable Oracle/fixture** and exact test-file allowlist are unfrozen. Stage A2 currently permits D1-04 with unspecified extra test files and unspecified pre-resolved Framework artifact hashes. Exact-diff authorization cannot safely be issued on this scope. | Specify frozen Stage-A2 **test-file paths**, synthetic fixture schemas, independent Oracle provenance and review-before-implementation ordering; pin actual resolved Spring Framework 5.x artifacts via offline dependency/classpath evidence. If new file/POM/dependency required, controlled amendment and independent exact-diff review **before** limited implementation authorization, not as implicit implementation discretion. |
| RF-U07-FOUND-PB-IR-01 | **REQUIRED** | R1 preventive control; D1-01 `deny_network.sh` | A shell script filename is not kernel boundary proof. Design states the distinction, but implementation prerequisites require an explicit OS/runner feasibility decision and negative canary attestation. | Name OS/runner/environment capability, enforceability of user/network/mount namespaces, host socket/credential denial and child-process containment, kernel preventive boundary ownership; negative-run evidence gate before any JVM. |
| RF-U07-FOUND-PB-IR-02 | **REQUIRED** | R2 failure precedence; Stage B | Rich failure ladder exists but observability of attempted `@PostConstruct`, native/JNI effects, and logging is not demonstrated. | Enumerate interception coverage and define `UNKNOWN` for any gap; forbids claiming zero from empty output. Separate Spring safety observations from source-only and physical SQL/rollback obligations. |

**Nonblocking accepted boundaries:** production Java/migrations forbidden; Tier-0 outputs remain SOURCE_ONLY with effective manager/EMF/DS UNKNOWN; F8/U07 atomicity not asserted; explicit Security+U01/Foundation owner permission required; Tier-1 separate authority; no SQL, JDBC connection, PHI/PROFILE-A, production or automatic CI probe.

## 3. R1–R3 detailed acceptance conditions

### R1 — OS prevention and attempt provenance

The successful canary is a **negative security policy test**, not proof of Spring diagnostic safety. Canary attempts are expected and recorded only in a separate isolated process/phase. No counter merging into positive runtime result; Spring-phase zero forbidden-attempt evidence must be measured independently with pre-JVM guard coverage. Any canary success or unauthorized external target is `ISOLATION_POLICY_FAIL`, and a Spring-phase attempt is `CONTEXT_UNSAFE`. If origin or the enforcement boundary is ambiguous, the outcome is `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE`/`INCOMPLETE_EVIDENCE`, never PASS. Running canaries is a **future separately authorized** action, not performed here.

### R2 — Independent target topology reference

The only acceptable strong claim is grounded in an independently validated effective topology comparator for the chosen representative synthetic profile; static evidence alone cannot establish what runtime bean survived conditional auto-configuration. A safe nonrepresentative Spring context may prove the probe mechanism works, but **not** that the untouched app uses the same manager. The reviewer requires explicit property-origin and selected auto-configuration outcome evidence, no circular `test_observation == test_baseline` equivalence. Missing authoritative untouched-context topology means UNKNOWN/REPRESENTATIVENESS_NOT_PROVEN even when no network occurred.

### R3 — Frozen executable Oracle and exact diff

For TX-R01..12, provide a separate author-controlled independent Oracle artifact `case_id / input_fixture_hash / expected_attribute_resolution / expected_manager_identity / expected_proxy_outcome / expected_equivalence_E01..05 / expected_failure_precedence / framework_classpath_digest`. Author fixture and comparator source must not both generate expected outcome from the same implementation under test. Case R04 especially must resolve actual `TransactionManagementConfigurer` and `TransactionInterceptor` contract on pinned Framework 5.x jars. Stage A2 must explicitly list source/test/manifest files, dependency resolution method and absence of network/bootstrap; no open-ended `if tests require new files` clause in an implementation authorization.

## 4. Exact-head verification and evidence mapping

| Evidence | Review result |
|---|---|
| PR #319 author head `75ab78058802970c9f5b1a08ca84950560586cd8` | MATCH |
| Design blob `fb7cc239da5f327974e92313ee3064bcd8699376` | MATCH |
| Target main `86e8843197091c8c8172b7e4213537a31bdf0654` | MATCH |
| PR #319 diff | 1 ADD-only Markdown, no production modification |
| PR #300/#302 contract & conditional design | PRESERVED, no Tier-1 execution authority |
| PR #303 `BF-U07-FOUND-TX-INT-01..03` | REMAIN OPEN |
| Tier-0 static implementation from #311/#317/#318 | ACCEPTED SOURCE_ONLY, no positive effective-manager proof |
| Current review runtime/test/sandbox execution | **NOT_EXECUTED** |

## 5. Ordered remediation and next controlled gate

1. **Author PR #319 targeted amendment:** address BF-PB-IR-01 phase-separated canaries/attempt counters, BF-PB-IR-02 noncircular representativeness target baseline, BF-PB-IR-03 executable Oracle and Stage A2 exact test-file manifest. Explicitly preserve all non-grants.
2. **Targeted independent design re-review at NEW exact PR #319 HEAD:** verify the three blockers with direct line/fixture evidence. No implementation or automatic merge.
3. Only upon `PASS / CONDITIONAL_DESIGN_ACCEPTANCE`, conduct a **Stage A preimplementation readiness and bounded implementation authorization decision**. Source-only feasibility does not imply runnable OS isolation. Any Stage A execution must be separately authorized for synthetic non-Spring canaries, with Security+Foundation/U01 owner signatures. Stage B Spring ApplicationContext remains blocked.
4. After Stage A independent evidence, re-evaluate INT-01/02/03 and the Tier-1 readiness before considering Stage B. BF-SEM-01/02 and BF-AUD-01..04 remain OPEN until separately evidenced.

## 6. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
TIER1 PREBOOT REMEDIATION INDEPENDENT DESIGN REVIEW
= REVISE_REQUIRED / NOT_ACCEPTED

AUTHOR_PR = #319
AUTHOR_EXACT_HEAD = 75ab78058802970c9f5b1a08ca84950560586cd8
AUTHOR_DESIGN_BLOB = fb7cc239da5f327974e92313ee3064bcd8699376
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654

BF-U07-FOUND-PB-IR-01 = OPEN
BF-U07-FOUND-PB-IR-02 = OPEN
BF-U07-FOUND-PB-IR-03 = OPEN
RF-U07-FOUND-PB-IR-01/02 = REQUIRED

BF-U07-FOUND-TX-INT-01..03 = OPEN
TIER1_DESIGN = AMENDMENT_REQUIRED
TIER1_READINESS = NOT_READY
TIER1_AUTHORIZATION = NOT_GRANTED
TIER1_CONTEXT_BOOT = NOT_EXECUTED
SOURCE_ONLY_TIER0 = VERIFIED / MERGED
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
FOUNDATION_GATE = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
PROFILE-A/PHI/PRODUCTION = BLOCKED
```

This is a **separate exact-head independent review record**, not a GitHub branch-protection approval by a different human, merge decision, security clearance or permission grant.
