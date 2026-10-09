# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 R2 Command Profile Targeted Independent Re-Review v0.1

> 2026-10-09. Exact author [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) HEAD `3f6725f387105798d2fa4ba6cefb1b43a69a0360` / document blob `2518bdb4e0a239abf70c84f32b24ccba80c713bd`. Source `main@86e8843197091c8c8172b7e4213537a31bdf0654`.
> Original [PR #329](https://github.com/cxjchelsea/AIdoctor/pull/329) exact HEAD `c7a2772fb5d2668a78354147d79158744558bf16` / review blob `00689a2e5eb5b1e2bc63fa9b0470006b0da7cab0`.
> **Decision: PASS / CONDITIONAL_DESIGN_ACCEPTANCE on the command-profile targeted design corrections only. R2_AUTHORIZATION_READINESS = NOT_READY; R2_COLLECTION_AUTHORIZATION = NOT_GRANTED.** No runner inspection, command, code execution, Spring, network, CI, database, PHI, production or merge.

## 1. Source and independent-method check

Fetched author PR #328 exact HEAD, full document blob and changed-path inventory, together with the unchanged latest repository main. Exactly one ADD-only Markdown design file remains in PR #328. Inspected the amended §3 probes, new §§3.1–3.3, signed-grant fields, §5 gates, §7 finding inventory and §8 decision. Compared required original findings with actual revised normative assertions; do not treat author self-declared remediation as approval.

## 2. Four original finding dispositions

| Finding | Exact-head remedial evidence | Independent result | Reserved real-runner proof |
|---|---|---|---|
| `RF-U07-A1-R2-IR-01` | §3 R2-06 and §3.1 replace unrestricted `findmnt` with **offline owner-signed** bounded tri-state mount claims; no R2-06 host command; raw paths/volumes forbidden; missing owner artifact -> incomplete | **CLOSED_CONDITIONAL_DESIGN** | Validate actual signatory authority, artifact provenance, schema/ACL; do not claim actual mount posture verified |
| `RF-U07-A1-R2-IR-02` | R2-07 becomes preexisting **offline signed asset manifest**; no cloud/control-plane API, HTTP(S), metadata, tokens, DNS or host daemon calls; absent/stale manifest -> incomplete | **CLOSED_CONDITIONAL_DESIGN** | Must independently read back nominated asset identity and verify issuer/validity; no machine nominated |
| `RF-U07-A1-R2-IR-03` | §3.1 fixes R2-01/03/05 argv arrays, no shell, no ambient privilege; typed length-limited outputs, raw stdout/stderr/log denial, collector-vs-future-JVM identity split; §3.3 adds required per-probe argv/binary/schema/collector binding | **CLOSED_CONDITIONAL_DESIGN** | Concrete collector executable and capture architecture NOT_IMPLEMENTED/NOT_REVIEWED; R2-02/04 still include **conditional** parser/readback candidates, not executable approval |
| `RF-U07-A1-R2-IR-04` | §3.2 offline `RunnerOfflineAttestationV1` freezes issuer authority, asset ID, SHA256, signature/key, issue/expiry/revocation, transfer ACL/retention and independent registry readback; §4 requires separate grant | **CLOSED_CONDITIONAL_DESIGN** | No signed real records, validated public keys or owner consent today; model-created text not human authority |

**Verdict scope:** these findings were defects in the *design of the R2 collection profile*. Each has a sufficiently fail-closed **proposed** solution. None establishes that a real host or collector is safe or permitted. An unconditional `R2-G04 PASS` would be false; retain `DESIGN_CONDITIONALLY_ACCEPTED / AUTHORITY_NOT_APPROVED`.

## 3. Adversarial check: candidate command constraints

- **R2-01/R2-03/R2-05:** fixed argv proposals are intelligible and bounded but still unexecuted. Multiple commands previously shown with semicolons in the earlier summary table are **descriptive only**, overridden by mandatory no-shell argv policy. Before any execution, owner-approved real binary hashes and command hashes must disambiguate that notation.
- **R2-02:** `cat /etc/os-release` by itself outputs raw bytes; §3.1 correctly says skip unless locally confined to a separately reviewed filtering mechanism, or prefer owner-signed OS inventory. Before authorization there must be a concrete no-raw-output, no-raw-logging collector design; otherwise reject the command.
- **R2-04:** `/proc/self/status` restricted keys refer only to the collection process. Owner cannot infer future JVM or Spring privilege from it. The real limited reader/parser remains to be specified/verified.
- **R2-06/R2-07:** no live invocation under the revised design; independently signed offline attestations may provide evidence without exposing host mount paths or introducing hidden API credentials. Real acceptance depends on separately verified artifact authority and freshness.
- **R2-08/09/10:** signed owner evidence only; a policy hash is not denial proof, and audit-instrumentation “available” does not imply attached/complete coverage.

**No design blocker introduced in this targeted scope:** the author explicitly forbids running unsafe raw-output commands when secure filtering cannot be demonstrated. A later implementation/authorization review MUST demand non-logging guarantees of the concrete runner/collector and independently validated command/asset/issuer context.

## 4. Gate status remains independent

| Gate | Targeted re-review outcome |
|---|---|
| `R2-G01` exact source identity | PASS_SOURCE_ONLY |
| `R2-G02` real runner + owner | BLOCKED / NOT_NOMINATED |
| `R2-G03` no-privilege-change feasibility on real target | NOT_PROVEN |
| `R2-G04` command profile | **CONDITIONALLY_ACCEPTED_DESIGN / NOT_APPROVED_FOR_EXECUTION** |
| `R2-G05` Infrastructure, Security, Foundation/U01 signatures | NOT_GRANTED |
| `R2-G06` retention, actual signature and independent evidence review | NOT_PROVEN |
| `R2-G07` non-Spring/DB/CI/PHI/no execution boundary | PASS_CONTRACT_ONLY |
| `R2-G08` specific R2 execution grant | NOT_GRANTED |

`BF-U07-A1-R2-01` remains OPEN for missing actual nominated nonproduction runner/owner; `BF-U07-A1-R2-02` remains OPEN for missing signed R2-only grants. `SA-BF-01/02` remain OPEN, and `SA-BF-03/04` remain OPEN/A2. Six-file Stage A1 implementation paths remain only conditionally accepted in design by PR #327, without authorization to write them.

## 5. Next actual readiness boundary

**Next decision recommended:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Real Runner Nomination + Owner Evidence Intake / R2 Authorization Prerequisite Review`.

Before any R2 authorization, obtain from the **actual** infrastructure/Security/Foundation-U01 authorities:
1. Real synthetic-only nonproduction asset ID, owner and environment segregation record.
2. Existing signed offline runner image/runtime and mount/policy/coverage manifest, or an explicitly approved alternative; independently verify signer, asset match, issue/expiry/revocation.
3. Exact per-command binary hashes and argv, collection UID, safe raw-output capture/log policy, no-network assertion, retention/ACL/authorized viewer and collection expiration.
4. Separate human scope-specific approval and explicit `RUNNER_ATTESTATION_COLLECTION_AUTHORIZATION`, after independent grant readiness review.

If the machine has not been nominated, return `R2_NOT_READY`; do not use `ubuntu-latest` by assumption, speculate access to a VM or repeat design-only reviews as if they produced real evidence.

## 6. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_R2_COMMAND_PROFILE_TARGETED_INDEPENDENT_RE_REVIEW
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

AUTHOR_PR328_HEAD = 3f6725f387105798d2fa4ba6cefb1b43a69a0360
AUTHOR_PR328_BLOB = 2518bdb4e0a239abf70c84f32b24ccba80c713bd
PR329_ORIGINAL_REVIEW_HEAD = c7a2772fb5d2668a78354147d79158744558bf16
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654

RF-U07-A1-R2-IR-01 = CLOSED_CONDITIONAL_DESIGN
RF-U07-A1-R2-IR-02 = CLOSED_CONDITIONAL_DESIGN
RF-U07-A1-R2-IR-03 = CLOSED_CONDITIONAL_DESIGN
RF-U07-A1-R2-IR-04 = CLOSED_CONDITIONAL_DESIGN

R2_COMMAND_PROFILE_DESIGN = CONDITIONALLY_ACCEPTED
R2_REAL_RUNNER = UNKNOWN / NOT_NOMINATED
BF-U07-A1-R2-01/02 = OPEN
R2_AUTHORIZATION_READINESS = NOT_READY
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
R2_HOST_COMMANDS_EXECUTED = NO

A1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
A1_SETUP_EXECUTION_AUTHORIZATION = NOT_GRANTED
A1_CANARY_EXECUTION_AUTHORIZATION = NOT_GRANTED
STAGE_B_SPRING = NOT_AUTHORIZED
SA-BF-01/02 = OPEN
SA-BF-03/04 = OPEN / STAGE_A2
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```

This independently authored analytical review is **not** an institutional/human security endorsement or approval for privileged or nonprivileged host access. No PR merge.
