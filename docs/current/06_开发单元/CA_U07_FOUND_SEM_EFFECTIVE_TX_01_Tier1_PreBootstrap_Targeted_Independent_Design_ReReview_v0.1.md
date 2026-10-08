# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-1 Pre-Bootstrap Targeted Independent Design Re-Review v0.1

> Review: 2026-10-08. Design author [PR #319](https://github.com/cxjchelsea/AIdoctor/pull/319), exact HEAD `7f9b056dde656e69bf7a061f0e41ba6cb8b832ca`, blob `370bd1dd4fac8289a2062c221a781eb599f4a1c5`. Prior independent [PR #320](https://github.com/cxjchelsea/AIdoctor/pull/320) @ `a8407ee46de90d0906cf1268a71b1b5b410d8089`, review blob `c735cae962455622416eabbac3f96deec14c47cf`. Main anchor: `86e8843197091c8c8172b7e4213537a31bdf0654`.  
> **Verdict: PASS / CONDITIONAL_DESIGN_ACCEPTANCE only.** Neither a GitHub human review approval nor implementation/execution/merge authority. No Tier-1 code/tests, Spring, JDBC, PHI or production were touched.

## 1. Independence and exact-head review

Reviewer independently fetched PR #319's revised HEAD and the previous PR #320 formal findings, and checked the complete revised Markdown, not an author-provided synopsis. GitHub PR #319 targets unchanged main and contains **exactly one Markdown ADD-only file**; reviewed file blob SHA above. The scope of this re-review is whether the **design text** resolves three PB-IR blockers and preserves no-grant boundaries, not execution feasibility or Oracle correctness. Prior Tier-0 source-only acceptance PRs #310/#311/#317/#318 remain valid only in their bounded scope.

## 2. Exact targeted finding disposition

| Finding | Original deficiency | New exact design evidence (PR #319) | Verdict and restrictions |
|---|---|---|---|
| `BF-U07-FOUND-PB-IR-01` | Deliberately denied canary attempts were conflated with forbidden Spring diagnostic attempts | §3 P2, P4, P5; §3.1 frozen phase-local Oracle; `SANDBOX_CANARY` independent child/sandbox, immutable teardown, fresh `SPRING_DIAGNOSTIC` attested sandbox and zero-origin counter, no inherited FDs/privileges; forbidden Spring attempts stay `CONTEXT_UNSAFE` | **CLOSED_DESIGN**. No canary policy or sandbox has been run; `BF-U07-FOUND-TX-INT-01` remains OPEN. |
| `BF-U07-FOUND-PB-IR-02` | Test context could self-certify unchanged effective production topology | §4.2 independent pre-observation `target_reference_manifest`, pinned exact config/property origins/dependency/autoconfiguration condition candidates, separate effective selection reference if admissible, noncircular comparison and UNKNOWN downgrade; test substitutions cannot pass E01–E05 | **CLOSED_CONDITIONAL_DESIGN**. The independently trusted effective reference may be unavailable; then `REPRESENTATIVENESS_NOT_PROVEN` is the *required*, acceptable outcome. `BF-U07-FOUND-TX-INT-02` remains OPEN. |
| `BF-U07-FOUND-PB-IR-03` | Stage A2 omitted frozen tests/Oracle, open-ended file expansion and actual Framework classpath | §5.2 adds exact proposed D1-04-T, D1-04-O, D1-04-M paths; synthetic fixture field schema; independent Oracle SHA/review-before-implementation; preverified offline Spring Framework jar/classpath hashes; §6 forbids extra tests/POM/network without a separate controlled amendment | **CLOSED_CONDITIONAL_DESIGN**. This freezes *proposed scope*, not the actual executable Oracle fixture or resolved dependency manifest; exact files must be separately designed/authorized and verified before Stage A2 implementation. `BF-U07-FOUND-TX-INT-03` remains OPEN. |

## 3. Required follow-through and guardrail checks

- `RF-U07-FOUND-PB-IR-01` **REQUIRED / NOT_EXECUTED**: selected OS and CI or local runner, kernel boundary enforcement, namespace/capability/child-process/Unix-socket/fs restrictions, Security owner and actual non-Spring canary negative evidence. The revised §3.1 states these as prerequisites, not as established facts. Any canary success or missing independent origin tracing blocks JVM startup.
- `RF-U07-FOUND-PB-IR-02` **REQUIRED / NOT_EXECUTED**: attempts from JPA metadata, Flyway, JDBC, network, native/JNI, child process, lifecycle and async surfaces must have coverage assessment. Any unobserved surface is UNKNOWN or INCOMPLETE_EVIDENCE, not clean zeros.
- `RF-U07-FOUND-TX-INT-01..03` remain REQUIRED as in PR #303. Security + Foundation/U01 owner grants are absent; this review does not confer them.
- `BF-U07-FOUND-TX-INT-01/02/03` remain **OPEN / IMPLEMENTATION_READINESS_NOT_PROVEN**. PB design closure is not INT implementation/physical closure.
- The Tier-0 `SOURCE_ONLY` result cannot be converted into `CONFIGURED_SAME_MANAGER` without independent effective runtime evidence. Spring manager object identity is not proof of physical JDBC enlistment or COMMIT.
- Any Stage A2 files beyond D1-04 are proposed controlled amendments to PR #303's D1-01..07 allowlist. They are not automatically approved merely because the design names them.
- Stage B Spring ApplicationContext and any SQL/MySQL/Oracle transaction diagnostics remain NOT_AUTHORIZED and NOT_EXECUTED.

## 4. Nonblocking caution: failure codes and authorization ordering

The revised text sometimes lists `INCOMPLETE_EVIDENCE` and `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE` as alternatives for missing phase origin. At actual runner contract design, freeze a deterministic per-stage mapping: no usable guard/sandbox attestation before JVM => NOT_EXECUTABLE; post-attestation observability gap => INCOMPLETE_EVIDENCE. This is **required Stage-A implementation detail**, not a new blocker for the present design re-review because both choices prevent a positive safety verdict.

A real independent reviewer must still approve the frozen fixture/Oracle, exact artifacts and chosen runner security assumptions before any executable gate. Review conditions do not create a grant. Do not circularly require an actual Tier-1 Spring execution to decide whether narrowly bounded Stage-A non-Spring preparation may be implemented; Stage B is a separate later authority.

## 5. Next decision / evidence conditions

**Next allowed gate:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A Pre-Bootstrap Implementation Readiness & Exact-Diff Authorization Review`. Recheck unchanged current main SHA and PR #319 exact HEAD, then separately review:
1. Stage A1 enforceable selected runner/OS sandbox and guard source/coverage, independent Security and Foundation/U01 owner permission plan, an offline non-Spring negative canary procedure with immutable expected/actual evidence.
2. Stage A2 controlled amended exact file list D1-04 + D1-04-T/O/M, independent synthetic Oracle and offline Framework dependency/artifact provenance.
3. Hard prohibition on actual Spring Context bootstrap/SQL/JDBC, production Java changes, migrations, PHI, deployed/automatic CI and broader permission.
4. Explicit decision whether only Stage A **implementation** (not execution) is possible; no implicit Stage B or full Tier-1 authorization.

## 6. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
TIER-1 PREBOOT TARGETED INDEPENDENT DESIGN RE-REVIEW
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

AUTHOR_PR = #319
AUTHOR_HEAD = 7f9b056dde656e69bf7a061f0e41ba6cb8b832ca
AUTHOR_BLOB = 370bd1dd4fac8289a2062c221a781eb599f4a1c5
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654

BF-U07-FOUND-PB-IR-01 = CLOSED_DESIGN
BF-U07-FOUND-PB-IR-02 = CLOSED_CONDITIONAL_DESIGN
BF-U07-FOUND-PB-IR-03 = CLOSED_CONDITIONAL_DESIGN

RF-U07-FOUND-PB-IR-01/02 = REQUIRED
BF-U07-FOUND-TX-INT-01/02/03 = OPEN
RF-U07-FOUND-TX-INT-01..03 = REQUIRED
STAGE_A_READINESS = NOT_YET_AUTHORIZED
TIER1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
SPRING_CONTEXT_EXECUTION = NOT_EXECUTED
SEM-01 / SEM-02 = OPEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
PROFILE-A/PHI/PRODUCTION = BLOCKED
```
