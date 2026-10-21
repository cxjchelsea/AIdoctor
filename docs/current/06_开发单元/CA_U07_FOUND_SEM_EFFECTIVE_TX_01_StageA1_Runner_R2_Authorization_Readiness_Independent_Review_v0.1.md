# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Nomination + R2 Read-Only Attestation Authorization Readiness Independent Review v0.1

> Review date: 2026-10-09
> Author input [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328): exact HEAD `fc8dd189b61e9c943bbdcce716abcc02068f21f9`, blob `ec5b40f335e164e98944f0a698f570919ee6fa26`.
> Source `main@86e8843197091c8c8172b7e4213537a31bdf0654`, verified; linked design and prior re-review [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) @ `6884c26e93855b97a5f1293fe81c3fa5f524a739` and [PR #327](https://github.com/cxjchelsea/AIdoctor/pull/327) @ `fe300a512a91cb56dd322b405f2b1642a3bdd03c`.
> **Verdict: PASS / CORRECT_NOT_READY_AUTHORIZATION_DECISION; READ_ONLY_COLLECTION_COMMAND_PROFILE = REVISE_BEFORE_AUTHORIZATION.** Independent assessment of design and source evidence only. No Runner selected/inspected, no commands executed, no security signoff, no grants, no implementation or merge.

## 1. Source, baseline and scope verification

Fetched exact GitHub PR #328 HEAD, document blob and full text. Verified exactly one ADD-only Markdown file relative to main; no production Java, POM, CI, tool executable, configuration, patient data or DB change. The repository's `.github/workflows/foundation-0-verification.yml` (blob `eb80668db53c9ac497de99a774434ec2b73d62a2`) refers to `ubuntu-latest` ordinary Java CI. The author correctly does **not** call it an approved nonproduction isolation runner.

This review asks two separately answerable questions:
- **Decision integrity:** Is the current `R2_NOT_READY / NOT_AUTHORIZED` determination justified? **YES**.
- **Collection-profile authorization readiness:** Is the proposed R2 command/output plan final, independently approved and safe enough for execution? **NO**.

Passing a negative readiness decision is **not** passing an execution readiness gate.

## 2. Independent assessment of the R2 probes

| Probe | Verdict | Independent rationale / required control |
|---|---|---|
| `R2-01` `uname` trio | ACCEPTABLE_CONCEPT / NOT_AUTHORIZED | OS/kernel/arch usually low-risk; three commands must be separately pinned or exact combined invocation hashed with bounded outputs; no implicit shell expansion |
| `R2-02` `/etc/os-release` | ACCEPTABLE_WITH_FILTER | Allow specifically `ID`, `VERSION_ID`, optionally `PRETTY_NAME` with length bounds; reject uncontrolled raw file export |
| `R2-03` `id -u/-g` | ACCEPTABLE_WITH_CONTEXT | UID/GID of collection identity only, no login/person names; verify no `sudo` and fixed collector context |
| `R2-04` `/proc/self/status` | ACCEPTABLE_WITH_FILTER | Fixed anchored keys for `CapEff`, `CapBnd`, `NoNewPrivs`, `Seccomp`; values are **collector-process** facts only, not automatic evidence of target JVM or runner's every process |
| `R2-05` `readlink /proc/self/ns/*` | ACCEPTABLE_WITH_FILTER | Namespace inode readback for collection process only; cannot prove isolation enforcement or guest-to-host absence |
| `R2-06` `findmnt ... TARGET,FSTYPE,OPTIONS` | **REVISION_REQUIRED_BEFORE_R2** | Unrestricted command collects sensitive mount paths before proposed export filtering; raw logs, failed command traces and output streams can leak paths. Must supply exact target-scoped read-only query or prior independently reviewed mount summary / source-side sanitizer; prevent raw stdout/stderr capture, bound bytes, exclude host and patient paths, demonstrate no secret contents/paths. If safe collection cannot be independently proven, OMIT and return `R2_INCOMPLETE_EVIDENCE` |
| `R2-07` approved runner image/runtime API | **REVISION_REQUIRED_BEFORE_R2** | “Approved API” lacks exact endpoint/data provenance and transport restrictions. A control-plane API could require network/credentials and conflict with “R2 no network” scope. Prefer preexisting signed offline inventory artifact handed over by owner; if network API is necessary, it requires a separately scoped authorization and must not be silently considered R2-local |
| `R2-08` policy descriptor/hash | ACCEPTABLE_WITH_OWNER_ATTESTATION | Owner-provided sanitized signed manifest only; no raw host policy dump, firewall queries or `nft/iptables` command without separate review; hash alone does not attest rule correctness |
| `R2-09` audit coverage manifest | ACCEPTABLE_WITH_OWNER_ATTESTATION | Existing signed owner inventory only; no eBPF/LSM/audit installation or attachment, no ambient process inspection |
| `R2-10` signed runner inventory | ACCEPTABLE_WITH_INDEPENDENT_READBACK | Must specify inventory signer, asset binding, issue/expiry/revocation, chain of custody; cannot use self-attested ephemeral runner metadata as sole independent check |

**Potential general risk:** “read-only” does not imply “no observable effects”; process creation, logging and privileged inventory reads can expose infrastructure details. This report does not infer permission to run any command or contact any API. `R2-04/05` observations are valid only for the actual attestation collector, not evidence that a future Spring JVM necessarily shares the same policy. Exact collector-to-runtime isolation equality requires later independent proof.

## 3. Findings and disposition

| Finding | Severity | Required follow-through | State |
|---|---|---|---|
| `BF-U07-A1-R2-01` | EXTERNAL BLOCKER | Nominate a real nonproduction runner/asset identity and infrastructure owner; provide independently verifiable signed provenance | **OPEN** |
| `BF-U07-A1-R2-02` | AUTHORIZATION BLOCKER | Obtain named Infrastructure, Security and Foundation/U01 approval bound to exact runner/collector/command/output/retention/time scope | **OPEN** |
| `RF-U07-A1-R2-IR-01` | REQUIRED BEFORE COMMAND-PROFILE APPROVAL | Replace unrestricted `findmnt` with explicit safe source-side target-scoped output or offline signed minimal owner artifact; fail closed if not feasible | **REQUIRED** |
| `RF-U07-A1-R2-IR-02` | REQUIRED BEFORE COMMAND-PROFILE APPROVAL | Bind `R2-07` to explicit offline local signed artifact/approved transport; prohibit hidden network API/credentials under R2 no-network promise | **REQUIRED** |
| `RF-U07-A1-R2-IR-03` | REQUIRED BEFORE R2 | Per command freeze exact argv (avoid shell ambiguities), privileges, bounded stdout/stderr/log paths, sanitizer, expected keys, command/file/version digests, run principal and fail-closed behavior; ensure collection process vs target JVM claim distinction | **REQUIRED** |
| `RF-U07-A1-R2-IR-04` | REQUIRED BEFORE R2 | Freeze independent provenance: owner inventory signer, asset identity, approval validity, retention/ACL, policy hashes and separate reviewer; do not elevate signed documentation to physical isolation proof | **REQUIRED** |

No further **design blocker** is necessary to affirm the author's conservative **NOT_READY** decision. The command profile itself must be revised before any eventual positive R2 collection approval.

## 4. Gate decision

| Gate | Independent result |
|---|---|
| `R2-G01` source and PR provenance | PASS / EXACT_SOURCE |
| `R2-G02` identified and approved real Runner/owner | FAIL / `R2_RUNNER_NOT_NOMINATED` |
| `R2-G03` no-privilege-change command feasibility on selected machine | NOT_PROVEN |
| `R2-G04` fully bounded command/output allowlist | REVISE_REQUIRED / NOT_APPROVED |
| `R2-G05` signed Infrastructure/Security/Foundation-U01 authorization | NOT_GRANTED |
| `R2-G06` signed provenance and independent readback/retention | NOT_PROVEN |
| `R2-G07` scope prohibition of CANARY, SETUP, Spring/DB/CI/production | PASS / CONTRACT_ONLY |
| `R2-G08` specific collection grant | NOT_GRANTED |

The pending amendment design PR #326 and its review PR #327 do not themselves approve the six prospective Stage A1 implementation files. `SA-BF-01/02` and `BF-U07-FOUND-TX-INT-01..03` remain OPEN; `SA-BF-03/04` remain separate Stage A2 blockers; no Foundation Audit PASS.

## 5. Recommended bounded next work

1. **Targeted author remediation to PR #328:** freeze a redaction-safe `R2-06` source-scoped mount query **without exposing raw mount data**, and an offline provenance-controlled R2-07 runner asset readback. Include exact permitted output schema, invocation/privilege/retention and source-of-truth boundaries for R2-01..10, while preserving `NOT_READY`.
2. **New targeted independent design re-review** at PR #328 new exact HEAD; may accept command profile as **DESIGN_ONLY**, never grant R2 collection.
3. **External Runner nomination + responsible Owner approval**: create a named candidate asset/owner record and obtain Security/Infrastructure/Foundation-U01 confirmation. This cannot be manufactured by GitHub source review; if no runner exists, remain `R2_NOT_READY`.
4. Only then consider **R2 Read-Only Collection Authorization Decision** with strict, real exact-host/identity/scope evidence. A positive decision, if possible, is separate from actual execution and from Stage A1 code implementation. No Spring/JDBC/PHI/production or merge authorization.

## 6. Formal outcome

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_R2_NOMINATION_ATTESTATION_AUTHORIZATION_READINESS_INDEPENDENT_REVIEW
= PASS / CORRECT_NOT_READY_AUTHORIZATION_DECISION

R2_COMMAND_PROFILE = REVISE_BEFORE_AUTHORIZATION
PR328_AUTHOR_HEAD = fc8dd189b61e9c943bbdcce716abcc02068f21f9
PR328_AUTHOR_BLOB = ec5b40f335e164e98944f0a698f570919ee6fa26
SOURCE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654

BF-U07-A1-R2-01/02 = OPEN
RF-U07-A1-R2-IR-01..04 = REQUIRED
R2_COLLECTION_READINESS = NOT_READY
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
RUNNER_ID = UNKNOWN / NOT_NOMINATED
R2_COMMANDS_EXECUTED = NO
STAGE_A1_IMPLEMENTATION = NOT_AUTHORIZED
TRUSTED_SETUP = NOT_AUTHORIZED
SANDBOX_CANARY = NOT_AUTHORIZED
STAGE_B_SPRING = NOT_AUTHORIZED
SA-BF-01/02 = OPEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```

This is a separate AI-authored independent analytical review artifact, not independent human Security/Infrastructure/Owner approval. No code or runtime was inspected or executed, no host introspection, no merge.
