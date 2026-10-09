# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Nomination + R2 Read-Only Attestation Collection Authorization Readiness v0.1

> Date: 2026-10-09. Repository `cxjchelsea/AIdoctor`.
> Source main exact HEAD: `86e8843197091c8c8172b7e4213537a31bdf0654`; GitHub latest commit rechecked.
> Inputs: [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) exact HEAD `6884c26e93855b97a5f1293fe81c3fa5f524a739` and [PR #327](https://github.com/cxjchelsea/AIdoctor/pull/327) exact HEAD `fe300a512a91cb56dd322b405f2b1642a3bdd03c`, review blob `0c7dce6cd1c7d7d22045dc63bc9f48fc190aa35b`; [PR #323](https://github.com/cxjchelsea/AIdoctor/pull/323) author runner design exact HEAD `c9d495620b74706c57d79e9cdd8528f46e8a5e04`.
> **Decision: RUNNER_NOMINATION_NOT_COMPLETE / R2_AUTHORIZATION_READINESS_NOT_READY / R2_COLLECTION_NOT_AUTHORIZED.** This is source-only authorization readiness and evidence-plan design; no machine selected or inspected.

## 1. Narrow scope and non-assumptions

The sole question is whether a **real**, nonproduction and synthetic-data-only Stage A1 runner has been nominated and whether a bounded, owner-approved **read-only R2 host attestation collection** can be separately authorized. No direct host tools, provisioning, namespace creation, firewall inspection by command, canary, DNS, network, CI dispatch, shell script, Spring/JVM, JDBC, PHI, clinical/production code, PR merge or permission changes were executed by this review.

Actual runner ID, owner identity, host access, kernel capabilities, credentials and security grants are **UNKNOWN**. Existing `.github/workflows/foundation-0-verification.yml` at blob `eb80668db53c9ac497de99a774434ec2b73d62a2` has `ubuntu-latest` ordinary Java CI, **not** independently verified no-network, host-socket, namespace or kernel policy isolation. Never nominate GitHub hosted runner by default or self-approve human grants.

## 2. Runner nomination register (no fabricated entries)

| Required attribute | Evidence field | Current |
|---|---|---|
| Environment/tenant and nonproduction segregation | `environment_class / nonproduction_owner_attestation` | UNKNOWN |
| Concrete runner ID / inventory record / physical or VM host owner | `runner_id / platform_asset_ref / infrastructure_owner_ref` | UNASSIGNED |
| OS, kernel release/build, architecture and image | `os_id, kernel_build_digest, arch, image_digest` | UNKNOWN |
| VM/container runtime, user/group/capabilities, LSM/seccomp | `runtime_digest, uid_caps_digest, lsm_policy_digest` | UNKNOWN |
| Deny policy for IPv4/IPv6/DNS/loopback/AF_UNIX/vsock/metadata | `network_policy_ref, mount_socket_policy_ref` | UNKNOWN |
| Host, secret, patient and production mount/credential absence | `mount_secrets_absence_attestation` | UNKNOWN |
| Names of infrastructure owner, Security reviewer, Foundation/U01 owner | `infra_owner, security_reviewer, foundation_u01_owner` | UNASSIGNED |
| Authoritative permission to do **R2 only** | `r2_collection_grant_ref, signer, scope, expiry` | NOT_GRANTED |
| Approved exact read-only command list and redaction | `r2_command_allowlist_sha256, disclosure_retention_policy` | DESIGN_CANDIDATE / NOT_APPROVED |

The above values must come from the responsible platform/owners or an authorized, independently verifiable machine inventory. A future program, an untrusted chat statement or source-only CI YAML cannot substitute for the live runner's independently read back identity.

## 3. R2 permitted-action design (commands are candidates, DO NOT EXECUTE)

**Design principle:** R2 obtains *existing* facts, no preventive configuration changes and no negative canaries. All commands are conditional on an approved **exact runner/platform**, locally executed by an already authorized owner or an individually authorized collection identity under a reviewed command allowlist. The following describe conceptual probes; they are not instructions to run now.

| Probe ID | Proposed command or read-only interface | Evidence purpose | Sensitive-data constraint |
|---|---|---|---|
| `R2-01` | `uname -s; uname -r; uname -m` | OS/kernel/arch, binding actual image record | Record versions only; no usernames/hostname |
| `R2-02` | `cat /etc/os-release` (only vendor/version keys) | Distribution and release | Filter other keys if environment-specific |
| `R2-03` | `id -u; id -g` | Collector UID/GID (not user identity) | Do not collect group membership or account names |
| `R2-04` | `grep -E '^(Cap(Eff|Bnd)|NoNewPrivs|Seccomp):' /proc/self/status` | Effective/bounding privilege and seccomp mode | Never enumerate process IDs beyond self |
| `R2-05` | `readlink /proc/self/ns/user; readlink /proc/self/ns/mnt; readlink /proc/self/ns/net; readlink /proc/self/ns/pid` | Current namespace inode identity | No host-wide namespace traversal |
| `R2-06` | `findmnt --noheadings --output TARGET,FSTYPE,OPTIONS` with a reviewed mount-target allowlist | Mount isolation and read-only roots | **May reveal sensitive paths**: must be filtered in-process before export; if safe filtering cannot be guaranteed, skip / `INCOMPLETE_EVIDENCE` |
| `R2-07` | Approved API/readback of existing runner image digest, virtualization runtime ID, and assigned runner ID | Bind provenance to platform owner | No privileged daemon, container socket or cloud API access by unapproved collector |
| `R2-08` | Owner-provided **sanitized** policy description/hash + existing enforcement mechanism status | Presence of network/LSM/firewall rules without modifying or testing them | No `iptables`/`nft` raw rule dump unless separately scoped and Security reviewed; no packet probe |
| `R2-09` | Owner-provided existing audit/telemetry coverage manifest for syscall/socket/child/FD attempts | Observability feasibility, not evidence of successful denial | No eBPF load, tracing attachment, auditd configuration, elevated capability |
| `R2-10` | Existing signed runner inventory attestation and independent owner readback | Immutable runner identity and validity | No tokens/secrets/system paths exported |

**Important limits:** Even apparently read-only commands can disclose sensitive host paths, process details or internal configuration. Explicit allowlist approval must include each command's **arguments, allowed output keys, output truncation/redaction, execution UID, runner identifier, expiry, artifact storage and independent readback**. No `sudo`, network access, package installation, privileged `nft/iptables`, `unshare`, `nsenter`, `docker` socket, recursive `/proc`, `lsblk`, `env`, `mount`, `curl`, `ping`, `dig`, policy writes or real metadata probes. A subcommand missing from the exact signed allowlist must fail closed.

## 4. Signed R2 Authorization Profile (design schema only)

```yaml
schema: U07StageA1R2CollectionGrantV1
scope: R2_READ_ONLY_RUNNER_ATTESTATION_ONLY
source_main_sha: 86e8843197091c8c8172b7e4213537a31bdf0654
author_design_pr326_sha: 6884c26e93855b97a5f1293fe81c3fa5f524a739
independent_design_pr327_sha: fe300a512a91cb56dd322b405f2b1642a3bdd03c
runner_id: UNKNOWN
runner_image_kernel_policy_digest: UNKNOWN
infrastructure_owner_identity_ref: UNASSIGNED
security_approver_identity_ref: UNASSIGNED
foundation_u01_approver_identity_ref: UNASSIGNED
collection_principal_and_uid_ref: UNASSIGNED
approved_command_allowlist_sha256: UNKNOWN
allowed_output_fields_and_redaction_sha256: UNKNOWN
no_network_no_privilege_change_attestation: NOT_PROVEN
collection_start_end_or_expiration: UNSET
evidence_storage_retention_and_acl_ref: UNSET
independent_readback_and_reviewer: UNASSIGNED
owner_signatures_or_external_approval_refs: []
status: NOT_GRANTED
```

A valid authorization binds exact runner ID, collection identity, command and artifact schemas, time bounds, retention and named Security/Infrastructure/Foundation U01 owner decision. Any unbound/mismatched field, stale source/run context, missing privilege proof or revoked approval => `R2_NOT_AUTHORIZED`.

## 5. Deterministic evidence and failure matrix

| Check | Passing condition for readiness | Current |
|---|---|---|
| `R2-G01` | Author PR #326 / independent PR #327 / main source exact heads frozen | SOURCE_VERIFIED |
| `R2-G02` | Concrete nonproduction runner/owner nominated and independently verified | BLOCKED |
| `R2-G03` | OS/kernel/VM and collection principal can be read without privilege changes | NOT_PROVEN |
| `R2-G04` | Approved explicit command set bound to runner and output redaction | DESIGN_CANDIDATE, NOT_APPROVED |
| `R2-G05` | Signed Infrastructure + Security + Foundation/U01 R2-only approvals | NOT_GRANTED |
| `R2-G06` | Valid retention/ACL/provenance and independent reviewer/readback | NOT_PROVEN |
| `R2-G07` | No SETUP, CANARY, Spring or DB, no CI trigger, no production scope | PASS / CONTRACT_ONLY |
| `R2-G08` | Exact grant receipt and authorized start/expiry verified **before** collector action | NOT_GRANTED |

**Failure outcomes:** absent runner identity or owner = `R2_RUNNER_NOT_NOMINATED`; unauthorized scope/expiry = `R2_NOT_AUTHORIZED`; no safe output filtering = `R2_DISCLOSURE_BOUNDARY_UNPROVEN`; lacking kernel/policy readback = `R2_INCOMPLETE_EVIDENCE`; command mismatch or host drift = `INVALID_PROVENANCE` / reject. No missing fact may be presented as `false`, `safe` or `passed`.

## 6. Separation from later actions and noncircular ordering

```text
R0 SOURCE INSPECTION (complete for this review)
  -> R1 NAMED RUNNER + INFRA OWNER ACK (NOT DONE)
  -> OWNER/SECURITY REVIEW OF EXACT R2 COMMANDS & OUTPUTS
  -> EXPLICIT R2 COLLECTION AUTHORIZATION (NOT GRANTED)
  -> AUTHORISED READ-ONLY HOST COLLECTION (NOT EXECUTED)
  -> INDEPENDENT RUNNER EVIDENCE READBACK/ATTESTATION (NOT DONE)
  -> A1 IMPLEMENTATION READINESS / SIX-FILE EXACT DIFF + CODE AUTHORIZATION (FUTURE)
  -> R3 TRUSTED SYNTHETIC FIXTURE SETUP GRANT (FUTURE)
  -> R4 NON-SPRING CANARY GRANT (FUTURE)
  -> STAGE B SPRING DIAGNOSTIC (SEPARATE, FUTURE)
```

One can evaluate a platform's likely suitability from existing independent inventories prior to R2, but **no collector command** is executed without R2 permission. R2 results are runner facts, not proof a network attempt is blocked; `SA-BF-01` requires additional preventive enforcement feasibility verification; `SA-BF-02` requires later negative-case evidence. Stage A1 six-file scope `CONDITIONALLY_ACCEPTED` from PR #327 is a design decision only; exact implementation scope, SHA, independent Oracle and grant remain separate.

## 7. Gap register and independent-review target

| Finding | Description | Outcome |
|---|---|---|
| `BF-U07-A1-R2-01` | No actual nominated, approved nonproduction runner/owner record | OPEN / EXTERNAL_DEPENDENCY |
| `BF-U07-A1-R2-02` | No signed R2 read-only command/redaction/time/ACL/security grant | OPEN |
| `RF-U07-A1-CA-IR-01` | Six future code paths exact tree/blob review | REQUIRED / FUTURE |
| `RF-U07-A1-CA-IR-02` | Concrete runner identity + safe command set/owner authority | REQUIRED / ADDRESSED_AS_PLAN, NOT CLOSED |
| `RF-U07-A1-CA-IR-03` | Executable independent Oracle/fixture/attempt coverage | REQUIRED / FUTURE |
| `RF-U07-A1-CA-IR-04` | Separate noncircular gates for R2/code/SETUP/CANARY | DESIGN_PROPOSED / NO_GRANTS |
| `SA-BF-01/02` | Physical isolation and canary evidence | OPEN |
| `SA-BF-03/04` | Stage A2 dependencies/Oracle and amendments | OPEN / NOT IN SCOPE |

**Next review:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Nomination + R2 Read-Only Attestation Authorization Readiness Independent Review`, exact author HEAD/blob. Independent reviewer must examine whether the command set is genuinely read-only and disclosure-safe, whether runner nomination is missing, whether any API command implies ambient privileges, and whether grant ownership is complete. If real runner is not nominated, the correct decision remains `R2_NOT_READY` rather than authorizing collection.

## 8. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_RUNNER_NOMINATION_R2_READ_ONLY_ATTESTATION_AUTHORIZATION_READINESS
= NOT_READY / R2_NOT_AUTHORIZED

BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
AUTHOR_PR326_HEAD = 6884c26e93855b97a5f1293fe81c3fa5f524a739
REVIEW_PR327_HEAD = fe300a512a91cb56dd322b405f2b1642a3bdd03c

R2-G01 = SOURCE_VERIFIED
R2-G02..06 = NOT_PASSED
R2-G07 = PASS_CONTRACT_ONLY
R2-G08 = NOT_GRANTED
BF-U07-A1-R2-01/02 = OPEN
REAL_RUNNER_ID = UNKNOWN / NOT_NOMINATED
R2_APPROVED_COMMAND_SHA = NONE
R2_COLLECTION_GRANT = NOT_GRANTED
R2_HOST_COMMANDS_EXECUTED = NO
A1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
TRUSTED_SETUP_EXECUTION = NOT_GRANTED
SANDBOX_CANARY_EXECUTION = NOT_GRANTED
STAGE_B_SPRING = NOT_AUTHORIZED
SA-BF-01/02 = OPEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```
