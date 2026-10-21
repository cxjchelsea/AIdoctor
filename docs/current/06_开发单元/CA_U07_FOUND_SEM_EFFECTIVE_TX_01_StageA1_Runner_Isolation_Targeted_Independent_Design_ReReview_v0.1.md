# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Isolation Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-09
> **Reviewed author:** [PR #323](https://github.com/cxjchelsea/AIdoctor/pull/323) exact HEAD `c9d495620b74706c57d79e9cdd8528f46e8a5e04`, Markdown blob `18c092d37cb839675426199c18bd9e10a8cddb6a`. Base `main@86e8843197091c8c8172b7e4213537a31bdf0654`.
> **Original independent findings:** [PR #324](https://github.com/cxjchelsea/AIdoctor/pull/324) exact HEAD `0b93405d5bba124a0e2464605de3aa2a24c237e2`, report blob `0875364335c9567ffead08f8e6efa5c030ec295c`.
> **Readiness ancestor:** [PR #322](https://github.com/cxjchelsea/AIdoctor/pull/322), Stage A READY_WITH_BLOCKERS / NOT_AUTHORIZED.
> **Decision: PASS / CONDITIONAL_DESIGN_ACCEPTANCE.** This is a targeted evidence-only document review. No physical OS/kernel isolation tested; no implementation, fixture SETUP, canary execution, JVM/Spring/DB/PHI/production or merge is authorized or executed.

## 1. Independent method and exact scope

Fetched exact author HEAD and prior independent report from GitHub, verified the exact design blob and author PR file inventory (one ADD-only Markdown design relative to unchanged main). Independently compared every original closure condition to revised §4.1, §4.2, §4.3, §5 and §6 rather than treating the author's proposed finding dispositions as evidence of closure. No dynamic runner, permission, artifact provenance or enforcement fact is inferred from design text.

## 2. Finding-by-finding independent disposition

| Original finding | Author remediation, exact location | Re-review judgment | Remaining physical authority |
|---|---|---|---|
| `BF-U07-A1-IR-01` | §4.1 defines `DENIED_BY_POLICY` vs `UNREACHABLE_TARGET`, `REFUSED_NO_LISTENER`, `NO_DNS_RESOLVER`, `INVALID_FIXTURE`, `OBSERVABILITY_GAP` and `UNEXPECTED_SUCCESS`; binds policy hash, enforcement layer, rule/event, UID, process/instance/namespace, synthetic fixture precondition and control; A1-N01..10 have per-case rejection criteria. VM no-NIC proof is admitted only under a separately approved narrower claim, not falsely classified as `DENIED_BY_POLICY`. | **CLOSED_CONDITIONAL_DESIGN**: false positives from absent routes, absent listeners and non-existent DNS are now explicitly disallowed; independently bound policy-origin evidence is required for each denial case. | Real runner rule-coverage, reproducible fixtures, policy event-to-attempt attribution and negative controls remain **NOT_EXECUTED/NOT_VERIFIED**. Unsupported policy telemetry => `INCOMPLETE_EVIDENCE`, not pass. |
| `BF-U07-A1-IR-02` | §4.2 freezes `TRUSTED_FIXTURE_SETUP → SANDBOX_CANARY → TEARDOWN`, distinct authority/namespace/instance/counters, setup-only permitted writes/listeners, safe synthetic listener/FD preconditions, phase cutoff, readback and cleanup; §6 freezes exact **candidate** file paths `D1-01-T/F/O/E` for driver, setup controller, independent Oracle and evidence schema, with separate controlled-amendment and execution grants. | **CLOSED_CONDITIONAL_DESIGN**: the fixture provenance and exact-diff omission have design answers, with no accidental permission to create extra files. | No real setup owner/grant, fixture proof, cleanup attestation, or controlled amendment is accepted. New files cannot be implemented merely because listed. |

**Review nuance:** this decision closes the *original design defects*, not the right to mark the ten test cases as `ISOLATION_POLICY_PASS`. For some target OS/VM implementations, observable denial might be unavailable; design correctly requires fail-closed or an independently reviewed narrower prevention-attestation Oracle. Choosing a different prevention oracle or fixture architecture after selecting an actual runner may require further controlled amendment.

## 3. Exact-diff boundary and test feasibility

| Item | File | Current authority |
|---|---|---|
| `D1-01` | `tools/u07_foundation_tx_topology/sandbox/deny_network.sh` | Proposed original path; **NOT_AUTHORIZED** |
| `D1-02` | `tools/u07_foundation_tx_topology/sandbox/bootstrap_guard_manifest.json` | Proposed original path; **NOT_AUTHORIZED** |
| `D1-01-T` | `tools/u07_foundation_tx_topology/sandbox/test_deny_network_canaries.py` | Proposed ADD under future controlled amendment; **NOT_AUTHORIZED** |
| `D1-01-F` | `tools/u07_foundation_tx_topology/sandbox/synthetic_fixture_setup.py` | Proposed ADD under future controlled amendment; **NOT_AUTHORIZED** |
| `D1-01-O` | `tools/u07_foundation_tx_topology/sandbox/canary_expected_oracle.json` | Proposed ADD under future controlled amendment; **NOT_AUTHORIZED** |
| `D1-01-E` | `tools/u07_foundation_tx_topology/sandbox/sandbox_evidence_schema.json` | Proposed ADD under future controlled amendment; **NOT_AUTHORIZED** |

Stage A1 standalone program/test runs remain blocked. No expanded exact-diff acceptance has been granted. A1-G05/06 are **CONDITIONALLY_DESIGNED / NOT_PHYSICALLY_VERIFIED**. A test entrypoint may never bootstrap Spring, invoke clinical/PHI code or make real network/database connections, including as a convenient positive control.

## 4. Positive design elements and remaining external blockers

**Accepted boundaries:** no false-policy-denial from plain errno; no risky external control endpoints; synthetic-only setup, per-phase counters; mandatory teardown before any future fresh Spring instance; kernel/VM prevention before JVM; no assumed GitHub `ubuntu-latest` isolation capability; no inadvertent A2/B grants; explicit fail-closed behavior on observability or test-fixture gaps.

**Still OPEN / outside this design-only acceptance:**
- `SA-BF-01`: an actual named nonproduction runner, OS/kernel/build, VM/container identity, security controls and independently approved Security/Foundation/U01 authority do not exist as verified evidence in this review.
- `SA-BF-02`: no authorized and executed synthetic fixture preparation, A1-N01..10 canary results, counter/provenance attestation or independent acceptance.
- `SA-BF-03/04`: Stage A2 resolved Framework classpath/Oracle and exact-diff amendment remain unaddressed.
- `RF-U07-A1-IR-01/02`: actual runner/owner attestation and guard/child/native/FD attempt coverage remain REQUIRED.
- `A1-DESIGN-RF-01`: now **PATHS_SPECIFIED**, but `CONTROLLED_EXACT_DIFF_AMENDMENT_PENDING`.
- `BF-U07-FOUND-TX-INT-01..03`, `BF-U07-FOUND-SEM-01/02`, Foundation audit closure remain OPEN.

## 5. Recommended next gated work

**Next controlled decision:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Controlled Exact-Diff Amendment + Runner Authority Evidence Plan`. Review the precise six-file candidate scope against PR #303/#322 authorization baseline, determine whether a standalone runner capability attestation may safely be gathered in a separate non-Spring authority gate, freeze explicit owner-grant conditions and approve/reject additions. This is **not** an authorization to execute a canary, initiate a network probe or create implementation files. Then seek an independent exact-head amendment review; only after the named OS runner/security owner grants and implementation-only authorization can A1 code be written. Fixture SETUP and CANARY execution require separate later permission.

## 6. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_RUNNER_ISOLATION_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

AUTHOR_PR = #323
AUTHOR_EXACT_HEAD = c9d495620b74706c57d79e9cdd8528f46e8a5e04
AUTHOR_DESIGN_BLOB = 18c092d37cb839675426199c18bd9e10a8cddb6a
REVIEW_BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654

BF-U07-A1-IR-01 = CLOSED_CONDITIONAL_DESIGN
BF-U07-A1-IR-02 = CLOSED_CONDITIONAL_DESIGN
RF-U07-A1-IR-01/02 = REQUIRED
A1-DESIGN-RF-01 = PATHS_SPECIFIED / AMENDMENT_PENDING
SA-BF-01/02 = OPEN
SA-BF-03/04 = OPEN / STAGE_A2
A1_IMPLEMENTATION_READINESS = NOT_READY
A1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
A1_SETUP_EXECUTION_AUTHORIZATION = NOT_GRANTED
A1_CANARY_EXECUTION_AUTHORIZATION = NOT_GRANTED
STAGE_B_SPRING_CONTEXT = NOT_AUTHORIZED
TIER1_MANAGER_TOPOLOGY = NOT_PROVEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```

This exact-head, separate-branch review is a scoped analytical artifact and does not constitute a distinct human Security/Owner approval, branch protection approval, any executable evidence, or permission to merge.
