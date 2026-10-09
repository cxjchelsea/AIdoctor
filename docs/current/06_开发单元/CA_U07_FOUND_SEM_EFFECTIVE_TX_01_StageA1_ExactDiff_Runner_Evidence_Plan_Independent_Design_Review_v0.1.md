# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Controlled Exact-Diff Amendment + Runner Authority Evidence Plan Independent Design Review v0.1

> Date: 2026-10-09
> Author: [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326), exact HEAD `6884c26e93855b97a5f1293fe81c3fa5f524a739`, file blob `72b71011331c6c8e36f2a1330d36f5bad1c786ab`.
> Base `main@86e8843197091c8c8172b7e4213537a31bdf0654`. Prior reviewed basis [PR #323](https://github.com/cxjchelsea/AIdoctor/pull/323) @ `c9d495620b74706c57d79e9cdd8528f46e8a5e04`, [PR #325](https://github.com/cxjchelsea/AIdoctor/pull/325) @ `db6de3c4a9ffe33d5048dad81b3b3d570f844617`, Stage A Readiness [PR #322](https://github.com/cxjchelsea/AIdoctor/pull/322) @ `9371fd810c86d5cd2dbe79fc5d5336624a8e3795`.
> **Verdict: PASS / CONDITIONAL_DESIGN_ACCEPTANCE (DESIGN-ONLY).** No human Security/Owner approval, implementation grant, executable Oracle, verified host or run evidence. No runner introspection, shell, network, SETUP, CANARY, Spring, JDBC, CI, PHI, production or merge.

## 1. Independent source, identity and scope check

Independently fetched the GitHub PR #326 exact HEAD, single ADD-only Markdown path `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Controlled_ExactDiff_Amendment_Runner_Authority_Evidence_Plan_v0.1.md`, exact blob above, and repository main `86e8843197091c8c8172b7e4213537a31bdf0654` at review time. Inspected all sections, including the candidate exact-file list, JSON invariants, phased evidence plan, owner gates and exclusions. The new document **proposes** a future implementation allowlist; the only actual PR #326 diff is this Markdown file. Do not confuse an accepted design inventory with an accepted code diff.

## 2. Independent six-file amendment review

| ID | Exact proposed path | Design need and scope | Design decision |
|---|---|---|---|
| D1-01 | `tools/u07_foundation_tx_topology/sandbox/deny_network.sh` | Preventive OS/VM policy and fail-closed pre-JVM attestation | ACCEPTABLE_CANDIDATE |
| D1-02 | `tools/u07_foundation_tx_topology/sandbox/bootstrap_guard_manifest.json` | Typed guard surfaces/phase-provenance/coverage | ACCEPTABLE_CANDIDATE |
| D1-01-T | `tools/u07_foundation_tx_topology/sandbox/test_deny_network_canaries.py` | Isolated non-JVM A1-N01..10 canary driver and observed readback | ACCEPTABLE_ADD_CANDIDATE |
| D1-01-F | `tools/u07_foundation_tx_topology/sandbox/synthetic_fixture_setup.py` | Explicit trusted setup/teardown ownership; never measured as canary effect | ACCEPTABLE_ADD_CANDIDATE |
| D1-01-O | `tools/u07_foundation_tx_topology/sandbox/canary_expected_oracle.json` | Independently frozen synthetic expected results and negative-control semantics | ACCEPTABLE_ADD_CANDIDATE |
| D1-01-E | `tools/u07_foundation_tx_topology/sandbox/sandbox_evidence_schema.json` | Strict evidence/provenance/phase/failure contract | ACCEPTABLE_ADD_CANDIDATE |

**Reasoned scope verdict:** adding a separate fixture controller, test driver, independent Oracle and evidence schema is proportionate to the proven PR #324/#325 separation/attribution gaps. The proposed set is **bounded and plausibly minimal for the declared full synthetic canary suite**; it is not proof that platform-specific runner integration will need no further files. If new helper, dependency, test runner, GitHub workflow or fixture artifact is needed, that is an amendment and cannot be smuggled into another file. `ADD ONLY` remains a proposal until path nonexistence, target HEAD/tree and all new blob contents are verified during a later exact-diff implementation gate.

## 3. Runner evidence tiers and authorization sequence

| Tier | Evidence boundary | Review |
|---|---|---|
| R0 | Repository offline/static source inventory only | PASS_DESIGN; this is not physical runner inspection |
| R1 | Candidate nonproduction runner nomination / owner assignment without host access | PASS_DESIGN; `RUNNER_NOMINATED_UNVERIFIED` only |
| R2 | Live **read-only** runner policy/kernel/UID/mount capability inventory | CONDITIONAL: **separate human infrastructure/Security permission**, exact audited command allowlist, safe disclosure and retention required BEFORE ANY host tool call |
| R3 | Synthetic trusted fixture preparation and teardown | CONDITIONAL: only after separate scoped setup execution permission |
| R4 | Measured non-Spring denial canary execution | CONDITIONAL: only after independent implemented-sandbox verification, frozen independent Oracle and explicit canary execution authority |
| R5 | Spring Context diagnostics | OUT_OF_SCOPE / NOT_AUTHORIZED |

**Authorization ordering review:** the author acknowledges that an implementation-only grant may be logically independent of full physical R2 canary evidence, but does not assume a waiver. This is acceptable at design stage. Before authorization, explicitly choose the permitted order and require a named platform/owner; no circular demand for completed Stage B to write a non-Spring script. A proposed Runner record containing `UNKNOWN` fields is not attestation and cannot satisfy SA-BF-01.

## 4. Independent Oracle / safety semantics

The design preserves the previously accepted core invariant: `DENIED_BY_POLICY` requires independently attributable OS/VM preventive rule evidence; `UNREACHABLE_TARGET`, `REFUSED_NO_LISTENER`, `NO_DNS_RESOLVER`, `INVALID_FIXTURE` and `OBSERVABILITY_GAP` never establish isolation. A no-NIC VM can support an independently reviewed **narrower capability-absence** claim, never a fabricated firewall-policy hit. The stage-local SETUP/CANARY/TEARDOWN counter boundaries cannot transfer canary expected attempts to a future Spring diagnostic.

**Required future freeze:** the **actual** case fixture manifest, concrete enforcement mechanism/event-to-rule mapping, independently produced expected output bytes and SHA256, exact runner command allowlist and independent readback must be frozen before code/test execution. This document does not contain these physical artifacts, so no test result or A1 blocker is closed by design acceptance.

## 5. Required findings, not present design blockers

| Finding | Severity | Required follow-through before granting dependent action |
|---|---|---|
| `RF-U07-A1-CA-IR-01` | REQUIRED | Verify six prospective paths' nonexistence against the exact target tree; freeze final file operations/blobs, implementation-only authority and any changed helper/fixture/dependency scope with a new exact-diff review. This design alone is not an authorization to create files. |
| `RF-U07-A1-CA-IR-02` | REQUIRED | Name the real nonproduction runner and owner, Security reviewer, OS/kernel/image/runtime/policy hashes; before R2 host inspection freeze approved **read-only commands**, actual least-privilege read path, disclosure and evidence retention, and explicit collection authorization. |
| `RF-U07-A1-CA-IR-03` | REQUIRED | Freeze independent Oracle and synthetic fixture policy/control on the chosen platform, audit coverage for native/child/FD/socket effects, deterministic `DENIED_BY_POLICY` proof and teardown policy before any SETUP/CANARY run. |
| `RF-U07-A1-CA-IR-04` | REQUIRED | Record a noncircular gate order for runner attestation vs **implementation-only** permission, with explicit independent decisions for R2, writing code, trusted SETUP, CANARY, later Stage B; never infer permission from a design-approved file list. |

These are future evidence/authority requirements rather than new objections to the proposed document design. Their absence **continues to block actual implementation readiness and all execution**.

## 6. Design gate results

| Gate | Result |
|---|---|
| A1-CA-G01 source & author identity | PASS |
| A1-CA-G02 six candidate path inventory and role separation | PASS_DESIGN |
| A1-CA-G03 independent design review of four additive paths | PASS_CONDITIONAL_DESIGN — not an executable amendment authorization |
| A1-CA-G04 concrete host, kernel, UID/policy | NOT_PROVEN |
| A1-CA-G05 read-only R2 collection permission | NOT_GRANTED |
| A1-CA-G06 runner policy/coverage evidence | NOT_PROVEN |
| A1-CA-G07 independent runnable Oracle and fixture evidence | DESIGN_CONTRACT_ONLY |
| A1-CA-G08 Security/Foundation/U01 owner approval | NOT_GRANTED |
| A1-CA-G09 specific file implementation grant | NOT_GRANTED |
| A1-CA-G10 setup/canary grants | NOT_GRANTED |
| A1-CA-G11 scope denylist | PASS_DESIGN |

`BF-U07-A1-IR-01/02` remain `CLOSED_CONDITIONAL_DESIGN` as recorded by PR #325. `A1-DESIGN-RF-01` is `SIX_PATH_SCOPE_CONDITIONALLY_ACCEPTED / IMPLEMENTATION_AMENDMENT_NOT_AUTHORIZED`. `SA-BF-01/02` remain OPEN; `SA-BF-03/04` remain OPEN in the separate Stage A2 workstream. `BF-U07-FOUND-TX-INT-01..03` remain OPEN; Foundation Audit remains NOT_PASSED.

## 7. Next decision and prohibitions

**Next:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Nomination + R2 Read-Only Attestation Collection Authorization Readiness`. Obtain a concrete nonproduction runner/owner proposal and an exact, safe read-only command/evidence plan, then independently decide whether R2 collection **can be authorized**. Do not proactively execute host commands or provision a runner as a consequence of this design PASS. If there is no nominated/accessible runner, return `R2_NOT_READY` and keep implementation blocked; do not turn the repository's `ubuntu-latest` CI into a substitute.

No source/CI/medical changes, environment/privilege grant, canary execution, Spring/JDBC/SQL, real network/metadata, PHI, production or PR merge; all require separate explicit approvals.

## 8. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_CONTROLLED_EXACT_DIFF_AND_RUNNER_AUTHORITY_EVIDENCE_PLAN_INDEPENDENT_DESIGN_REVIEW
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

AUTHOR_PR = #326
AUTHOR_EXACT_HEAD = 6884c26e93855b97a5f1293fe81c3fa5f524a739
AUTHOR_BLOB = 72b71011331c6c8e36f2a1330d36f5bad1c786ab
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654

SIX_FILE_DESIGN_SCOPE = CONDITIONALLY_ACCEPTED
IMPLEMENTATION_ALLOWLIST_AUTHORIZATION = NONE
RF-U07-A1-CA-IR-01..04 = REQUIRED
BF-U07-A1-IR-01/02 = CLOSED_CONDITIONAL_DESIGN
SA-BF-01/02 = OPEN
SA-BF-03/04 = OPEN / STAGE_A2
RUNNER_IDENTITY = UNKNOWN / NOT_ATTESTED
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
A1_IMPLEMENTATION_READINESS = NOT_READY
A1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
SETUP_EXECUTION_AUTHORIZATION = NOT_GRANTED
CANARY_EXECUTION_AUTHORIZATION = NOT_GRANTED
STAGE_B_SPRING = NOT_AUTHORIZED
BF-U07-FOUND-TX-INT-01..03 = OPEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```

This independently authored report is an AI-generated design review artifact, **not** a distinct human Security/Infrastructure/Owner GitHub review approval.
