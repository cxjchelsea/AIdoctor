# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Isolation Authority + Exact Sandbox Independent Design Review v0.1

> Review date: 2026-10-09. Author [PR #323](https://github.com/cxjchelsea/AIdoctor/pull/323), exact HEAD `b8ff61827a1324140bf08259f4ea215b886fbc21`, exact design blob `1874f43f450f38fa7bdab25ea01220ef50ebd77f`; `main@86e8843197091c8c8172b7e4213537a31bdf0654`.
> Governing Stage A readiness: [PR #322](https://github.com/cxjchelsea/AIdoctor/pull/322) exact head `9371fd810c86d5cd2dbe79fc5d5336624a8e3795`; design dependencies PR #319 and independent conditional design PR #321.
> **Independent design verdict = REVISE_REQUIRED / TWO_DESIGN_BLOCKERS.** This review is restricted to design and source-evidence inspection. No sandbox code, OS canary, Spring/JVM, JDBC, secrets, PHI, CI execution, implementation authorization or merge.

## 1. Exact-scope and epistemic check

Independently fetched GitHub PR #323 metadata, full exact-head Markdown, changed filenames and repository latest main. Verified one ADD-only `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Runner_Isolation_Authority_Exact_Sandbox_Design_v0.1.md` and exact blob `1874f43f450f38fa7bdab25ea01220ef50ebd77f`. Existing `foundation-0-verification.yml` uses `ubuntu-latest` but does not demonstrate safe kernel namespace/network/socket isolation for this proposed probe. PR #323 correctly labels the ephemeral nonproduction Linux VM/runner as an **architecture candidate**, not an actual attested machine. This review does not certify the availability of any Runner.

Scope:
- SA-BF-01: selected OS, VM/hypervisor/container kernel preventive boundary and owner authority
- SA-BF-02: non-Spring negative canary truth, phase independence, event/counter provenance and execution grant
- A1-G01..10, synthetic cases A1-N01..10, future diff D1-01/02 and A1-DESIGN-RF-01
- No physical verification; the original SA-BF-01/02 remain OPEN regardless of design verdict.

## 2. Findings (severity and exact remediation)

| Finding | Severity | Location in author PR #323 | Independent issue | Required design fix |
|---|---|---|---|---|
| `BF-U07-A1-IR-01` | **BLOCKER** | §4.1 A1-N01, N02, N03, N09; §4.2 `ISOLATION_POLICY_PASS` | A numeric nonroutable/unreachable IP, IPv6 target with no route, fictional DNS name or synthetic metadata-shaped endpoint can fail for reasons **unrelated to enforcement**. A failed connection alone does not prove the VM/kernel denial policy applied. `A1-N03` even describes an offline fixture with no real resolver, which may produce a negative without any attempted forbidden socket. The Oracle currently lacks a mandatory policy-enforcement origin and positive-control discrimination. | Freeze per-case `attempt_observed`, `denying_enforcement_layer`, `expected_errno_or_event_class`, policy/namespace/instance/UID binding, kernel/VM audit proof, `unreachable_without_policy` negative discriminator, and preflight synthetic endpoint accessibility **outside** measured sandbox only where safe. Distinguish `DENIED_BY_POLICY` from `UNREACHABLE_TARGET`, `REFUSED_NO_LISTENER`, `NO_DNS_RESOLVER`, `INVALID_FIXTURE`; ONLY the former plus zero successful prohibited effects and independent counter coverage may pass a denial assertion. Canaries shall not access any production/public DB, real metadata service or company network. If prevention-layer attribution cannot be proven by the selected runner, `INCOMPLETE_EVIDENCE` / NOT_READY, not PASS. |
| `BF-U07-A1-IR-02` | **BLOCKER** | §4.1 A1-N04, N05, N07, N10 and trusted-fixture setup caveat; §6 D1-01/02 only | The loopback/AF_UNIX canaries require a meaningful listener/FD fixture, yet the design forbids local network/sockets. It allows trusted setup outside the measured sandbox or an approved setup phase but does not freeze which mechanism, whether setup itself is authorized, how the listener/FD stays accessible without weakening sandbox denial, and how synthetic setup write/socket attempts are excluded from measured forbidden attempt counters. The two-file D1 list has no frozen driver/fixture/Oracle artifacts, so `A1-G05/06` could not be independently reproduced. | Freeze **separate trusted SETUP / measured CANARY / TEARDOWN** lifecycle, precise setup owner/permit/fixture namespaces, immutable fixture material/expected accessible-without-policy precondition, inherited-FD sentinel ownership, phase-local attribution, deterministic cleanup; distinguish expected fixture initialization from forbidden canary effects, never grant canary exception in diagnostic phase. Include exact future helper/Oracle file paths (or explicitly reduce supported canaries to two-file implementable scope) and controlled amendment **before** A1 implementation permission. |
| `RF-U07-A1-IR-01` | REQUIRED | §2.1–2.2 runner candidate/authority record | An architecture candidate is not a named machine with real kernel/UID, policy, LSM, socket denial and security owner attestations. Correctly UNKNOWN today. | Before implementation/physical execution gate, select actual Runner and freeze OS/kernel/image, UID/mount/net namespace, policy digest, owner signoff and validated capabilities. This is an **external prerequisite**, not grounds to invent a runner or mark SA-BF-01 passed. |
| `RF-U07-A1-IR-02` | REQUIRED | §5 attempted-effect manifest and §4.2 future Spring phase | Counter/denial proof must cover native/shell children, inherited descriptors, unauthorized scratch writes and syscall attempt tracing; a zero-success-only report is insufficient. | Formalize guard coverage and negative control; absent observability => `INCOMPLETE_EVIDENCE` even when prevention remains enforced. No Spring Context testing in A1. |

**Verified and accepted in design:** `SANDBOX_CANARY` vs `SPRING_DIAGNOSTIC` run separation and new instance/counter identity; default-deny-before-JVM concept; no clinical/production/PHI access; no assumptions about GitHub-hosted `ubuntu-latest`; separate implementation and execution decisions; denylist production Java/POM/migrations/CI; negative canary cases explicitly described as NOT_EXECUTED; no fabricated hashes.

## 3. Gate-by-gate review

| Gate | Author intent | Independent finding |
|---|---|---|
| A1-G01 source/design identity | Exact main/PR provenance | **PASS / SOURCE_ONLY** |
| A1-G02 chosen actual runner | Candidate Linux VM, actual unknown | **NOT_READY** (external) |
| A1-G03 preventive kernel/VM enforcement | Correctly requires actual Security proof | **CONDITIONAL_DESIGN** |
| A1-G04 coverage | Lists broad attack surfaces | **DESIGN_ONLY / RF-A1-IR-02** |
| A1-G05 independently frozen canary Oracle | 10 cases, insufficient denial attribution/setup control | **BLOCKED / BF-A1-IR-01/02** |
| A1-G06 exact future diff | D1-01/02 plus acknowledged possible helper expansion | **BLOCKED / BF-A1-IR-02** |
| A1-G07 named owner/Security approval | Missing, correctly not claimed | **NOT_GRANTED** |
| A1-G08 implementation authorization | Separate gate | **NOT_GRANTED** |
| A1-G09 canary execution authorization | Separate gate | **NOT_GRANTED** |
| A1-G10 independent design acceptance | This review | **REVISE_REQUIRED** |

Review insists on a truly counterfactual negative oracle: a target's absence/non-routability must not be confused with the firewall, seccomp or VM policy enforcing a denial. A positive-control fixture cannot require live external destinations or relaxation of the measured policy.

## 4. Required targeted remediation and exact-head re-review

1. On **author PR #323**, amend A1-N01..10 with prevention-layer provenance, diagnostic classification of denied vs unreachable, synthetic offline positive-control or policy proof that differentiates mechanisms. Freeze per-case expected evidence and failure precedence.
2. Define explicit fixture provisioning phase, permitted setup operations, separate namespace/process and counter ownership, listener/FD lifecycle, cleanup/teardown, and zero contamination of measured `SANDBOX_CANARY`/future `SPRING_DIAGNOSTIC`.
3. Close `A1-DESIGN-RF-01` at the **design scope** by a precise additive file inventory for canary driver, independent Oracle, fixture/policy manifest as required (or remove unsupported case requirements). New paths are candidate controlled amendments, **not authorized code**.
4. Perform `Stage A1 Runner Isolation Authority + Exact Sandbox Targeted Independent Design Re-Review` on the new author exact HEAD/Blob. Re-check original blocked findings; no authorization via re-review.
5. Only afterward request separate concrete Runner/security authority and bounded implementation readiness/authorization. Stage A2, Stage B, JDBC, PHI and production remain blocked.

## 5. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_RUNNER_ISOLATION_AUTHORITY_EXACT_SANDBOX_INDEPENDENT_DESIGN_REVIEW
= REVISE_REQUIRED / DESIGN_NOT_ACCEPTED

AUTHOR_PR = #323
AUTHOR_HEAD = b8ff61827a1324140bf08259f4ea215b886fbc21
AUTHOR_BLOB = 1874f43f450f38fa7bdab25ea01220ef50ebd77f
MAIN_BASE = 86e8843197091c8c8172b7e4213537a31bdf0654

BF-U07-A1-IR-01 = OPEN
BF-U07-A1-IR-02 = OPEN
RF-U07-A1-IR-01/02 = REQUIRED
SA-BF-01/02 = OPEN
SA-BF-03/04 = OPEN / STAGE_A2
A1_DESIGN = REVISE_REQUIRED
A1_IMPLEMENTATION_READINESS = NOT_READY
A1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
A1_CANARY_EXECUTION_AUTHORIZATION = NOT_GRANTED
SPRING_DIAGNOSTIC = NOT_AUTHORIZED
TIER1_TOPOLOGY = NOT_PROVEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```

This is a separate exact-head analytical review recorded as a Draft PR, **not** approval by an independent human security reviewer, runtime evidence, implemented sandbox, external permission or Merge Authorization. Do not auto-merge this or the author PR.
