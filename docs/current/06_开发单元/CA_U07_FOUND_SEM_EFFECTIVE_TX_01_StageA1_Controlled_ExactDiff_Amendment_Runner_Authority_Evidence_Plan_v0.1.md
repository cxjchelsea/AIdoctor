# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Controlled Exact-Diff Amendment + Runner Authority Evidence Plan v0.1

> Date: 2026-10-09
> Base: `main@86e8843197091c8c8172b7e4213537a31bdf0654` (latest reported GitHub repository commit when checked).
> Design authority: [PR #323](https://github.com/cxjchelsea/AIdoctor/pull/323) exact HEAD `c9d495620b74706c57d79e9cdd8528f46e8a5e04`, design blob `18c092d37cb839675426199c18bd9e10a8cddb6a`.
> Targeted independent design re-review: [PR #325](https://github.com/cxjchelsea/AIdoctor/pull/325) exact HEAD `db6de3c4a9ffe33d5048dad81b3b3d570f844617`, blob `2dbbc6b18518b7506c38fcb3d5e9e5a9c6bd2680`: `PASS / CONDITIONAL_DESIGN_ACCEPTANCE` only.
> Governing readiness: [PR #322](https://github.com/cxjchelsea/AIdoctor/pull/322) exact HEAD `9371fd810c86d5cd2dbe79fc5d5336624a8e3795`; original effective-TX scope PR #300/#303.
> **Status: CONTROLLED_AMENDMENT_DESIGN_CANDIDATE / INDEPENDENT_REVIEW_PENDING. This document does not itself amend an approved allowlist or authorize any code, runner provisioning, host inspection, SETUP, CANARY, JVM, Spring, CI, network, DB, merge, PHI or production.**

## 1. Purpose and authority preservation

The question is whether Stage A1's original D1-01/D1-02 two-file candidate can cover independently verifiable negative canaries. PR #325 established the need for **a separate synthetic fixture setup, canary driver, independent Oracle, and evidence schema**. This design creates an **explicit, bounded proposed amendment** to the previously reviewed Stage A1 implementation allowlist; it is **not** an approval of that amendment. A proposed six-file design becomes an authorized six-file implementation allowlist only after independent exact-head acceptance AND a separate scoped owner/implementation authorization.

Distinguish four independent decisions:
1. `SCOPE_DESIGN_ACCEPTANCE`: accept paths and interfaces in principle, no code written;
2. `RUNNER_ATTESTATION_COLLECTION_AUTHORITY`: authorize narrowly safe, non-Spring, no-network inventory of an identified nonproduction runner, no negative canary;
3. `A1_IMPLEMENTATION_AUTHORIZATION`: explicitly permit only accepted file creations on a pinned source tree, no execution;
4. `SETUP_EXECUTION_AUTHORIZATION` then `CANARY_EXECUTION_AUTHORIZATION`: separate permits and independent evidence gates; neither authorizes Spring diagnostic.

No inherited Tier-0 `SOURCE_ONLY` evidence grants effective manager identity, shared physical transaction, or clinical implementation. Stage A2 Oracle/Framework work and Stage B Spring remain excluded.

## 2. Controlled amendment — exact candidate file allowlist

All file paths are relative to repository root, with case-sensitive spelling and operation `ADD ONLY`. Existing files, extra helpers, renames, POM, workflow files, production source, migrations and scripts outside the table are **excluded**.

| ID | Exact future path | Stage A1 purpose | Execution class | Dependency / ownership |
|---|---|---|---|---|
| `D1-01` | `tools/u07_foundation_tx_topology/sandbox/deny_network.sh` | Fail-closed runner-specific preventive OS/VM policy selection and pre-JVM attestation; never assert isolation from exit code alone | **CODE ONLY; execution separately gated** | Security owner; real runner-specific policy proof required before usage |
| `D1-02` | `tools/u07_foundation_tx_topology/sandbox/bootstrap_guard_manifest.json` | Typed enforcement surface, minimum coverage and per-phase attempts provenance; UNKNOWN on gap | **DATA ONLY** | Security + Foundation/U01 owner |
| `D1-01-T` | `tools/u07_foundation_tx_topology/sandbox/test_deny_network_canaries.py` | Synthetic non-JVM A1-N01..10 driver, readback and policy-event correlation | **TEST CODE; NO RUN GRANT** | Requires separately authorized fixture and canary execution |
| `D1-01-F` | `tools/u07_foundation_tx_topology/sandbox/synthetic_fixture_setup.py` | Dedicated trusted fixture SETUP and TEARDOWN controller; no real network/customer secrets | **SETUP CODE; NO RUN GRANT** | Named synthetic setup owner; separate setup execution authorization |
| `D1-01-O` | `tools/u07_foundation_tx_topology/sandbox/canary_expected_oracle.json` | Independently authored/frozen case input, permitted preconditions, expected policy-denial rules, negative controls and failure labels | **DATA ONLY** | Oracle reviewer independent from the implementation author |
| `D1-01-E` | `tools/u07_foundation_tx_topology/sandbox/sandbox_evidence_schema.json` | Strict schema with source/tree/runner/policy/grants/fixture/phase/attempt/effect/teardown/ref binding | **DATA ONLY** | Evidence reviewer independent from the producer |

**Delta to PR #303/#322 design candidates:** `D1-01-T/F/O/E` are **four new proposed entries**. `D1-01/02` keep their prior names and purpose, with no assumed prior implementation grant. No Stage B D1-03/D1-05/D1-06/D1-07 and no Stage A2 D1-04/T/O/M are included. Do not change the CI workflow merely to run these scripts. If the selected runner needs additional code or manifests, return `EXACT_DIFF_AMENDMENT_REQUIRED` instead of silently adding them.

### 2.1 Proposed contract relations and design-time verification

```text
RunnerAuthorityRecord (external signed evidence, not synthesized by code)
    -> D1-01 pre-execution policy/identity decision
    -> D1-02 guard coverage + fail-closed surface specification

Independent Oracle (D1-01-O; frozen before canary observations)
    + exact fixture preparation specification (D1-01-F)
    -> CANARY suite D1-01-T validates observed deny attribution
    -> evidence strictly normalized by D1-01-E
    -> external independent reviewer validates bundle, not producer self-approval
```

All six artifacts must be pinned by commit and blob SHA at implementation review. The `oracle.json` expected fields must not be populated by execution output of `test_deny_network_canaries.py`; the implementation code must not set expected/actual to the same value. No default PASS on missing policy event. If a VM has no NIC but no observable denial, it may produce a **separately reviewed narrower capability-absence claim** and cannot be mislabeled `DENIED_BY_POLICY` without policy proof.

### 2.2 Required JSON schema-level invariants

- `case_id`: exactly A1-N01..A1-N10, uniquely specified. `fixture_hash` plus approved `setup_grant_ref`, `fixture_accessible_without_measured_policy` proof or independently evaluated deny-rule proof.
- `policy_sha256`, `runner_id`, `kernel_build_digest`, `instance_id`, `namespace_or_vm_identity`, `pid_lineage_token`, `uid_token`, `attempt_timestamp_monotonic` and `enforced_rule_id`/VM assurance reference bind every result to the same preventive boundary.
- `expected_outcome` is `DENIED_BY_POLICY` **only** when the prevention-layer event can be attributed independently. `UNREACHABLE_TARGET`, `REFUSED_NO_LISTENER`, `NO_DNS_RESOLVER`, `INVALID_FIXTURE`, `OBSERVABILITY_GAP` and `UNEXPECTED_SUCCESS` are never coerced to passing denial outcomes.
- `stage` is one of `TRUSTED_FIXTURE_SETUP`, `SANDBOX_CANARY`, `TEARDOWN`, `SPRING_DIAGNOSTIC`; `SPRING_DIAGNOSTIC` is **schema-reserved only** here, not executable.
- Every phase has distinct instance/process lineage/counter origin. SETUP-only allowed fixture effects must not be excused during CANARY; allowed TEST harness writes must be scoped to isolated scratch. TEARDOWN evidence must show closure of synthetic listeners/FDs.
- `A1_CANARY_EVIDENCE_ACCEPTED` is possible only after full independent review of policy origin, negative control, trusted fixture preconditions, counts and teardown. The producer can output a candidate record, **not** the final acceptance grant.

## 3. Runner Authority Evidence Plan (not an invented real runner)

### 3.1 Selected target architecture vs actual identity

**Candidate architecture:** ephemeral isolated **nonproduction Linux VM or comparably enforced runner**, with host/guest isolation, no production mounts/secrets, no external egress, no guest-to-host socket exposure, and immutable image. This is not a factual selection of a machine. The existing `.github/workflows/foundation-0-verification.yml` (blob `eb80668db53c9ac497de99a774434ec2b73d62a2`) uses `ubuntu-latest` for normal JDK/Maven testing; this does not prove it supports OS policy enforcement. No change to any workflow is proposed.

**Unknown fields block permission:** `actual_runner_id`, `owner`, `OS/kernel build`, `arch`, `image and runtime digests`, `UID/GID and capabilities`, `VM/namespace/LSM policy`, `network/loopback/IPv6/DNS and AF_UNIX controls`, `mounts/secrets`, `metadata/vsock paths`, `child and inherited FD controls`, `allowed scratch mounts`, `policy evidence and logging`. None has been independently attested, therefore `SA-BF-01` remains OPEN.

### 3.2 Evidence acquisition tiers with explicit safety authority

| Tier | Evidence action | May proceed from this design alone? | Mandatory permissions / outcome |
|---|---|---|---|
| `R0` | Offline inventory of **existing repository** design, POM, workflow and provenance | **Yes, design inspection only** | No live runner, no execution; `REPOSITORY_SOURCE_ONLY` |
| `R1` | Name a proposed synthetic/nonproduction runner and its owner through planning / owner review; fill nontechnical profile without host command execution | **Design planning only** | Real owner acknowledgment still separately required; `RUNNER_NOMINATED_UNVERIFIED` |
| `R2` | Read-only **live machine/guest** inventory of kernel, namespace/capabilities, read-only mounts, image IDs, policy configuration and available audit mechanisms | **NO** | Separate `RUNNER_ATTESTATION_COLLECTION_AUTHORIZATION`, Security + infrastructure owner approval, approved commands/source disclosure/retention, no network probe / privileged changes / synthetic listener |
| `R3` | Test preventive rule viability and synthetic **trusted fixture SETUP** (listener/FD/namespace, no public/corporate endpoints) | **NO** | Separate `SETUP_EXECUTION_AUTHORIZATION`; approved real runner/scope/fixture policy; fail closed |
| `R4` | Measured A1-N01..10 non-Spring `SANDBOX_CANARY` | **NO** | Prior verified A1 implementation and setup, separate `CANARY_EXECUTION_AUTHORIZATION`; external Security/Owner grants and independent oracle |
| `R5` | Spring Context diagnostic | **NO / OUT OF STAGE A1** | Future independent Stage B execution decision, not implied by A1 evidence |

R2 must not use GitHub Actions merely because the repository has a workflow. No real host introspection, network/DNS probe, mounting, namespace creation, shell script, VM startup or policy modification is authorized by this document. Any R2 command list must be predeclared and subject to owner approval; negative canary results are **not** generated by R2.

### 3.3 Runner authority record and expiration rules

Required immutable record structure (placeholders deliberately unresolved):

```yaml
schema: U07StageA1RunnerAuthorityV1
source_baseline: 86e8843197091c8c8172b7e4213537a31bdf0654
runner_id: UNKNOWN
infrastructure_owner: UNASSIGNED
security_reviewer: UNASSIGNED
foundation_u01_owner: UNASSIGNED
environment_class: SYNTHETIC_NONPATIENT_ONLY
os_kernel_build_digest: UNKNOWN
architecture: UNKNOWN
image_digest: UNKNOWN
vm_or_container_runtime_digest: UNKNOWN
uid_caps_namespace_digest: UNKNOWN
network_dns_loopback_af_unix_policy_digest: UNKNOWN
host_socket_mount_metadata_vsock_proof_digest: UNKNOWN
child_process_fd_policy_digest: UNKNOWN
scratch_quota_ro_mount_proof_digest: UNKNOWN
observability_coverage_digest: UNKNOWN
synthetic_fixture_policy_digest: UNKNOWN
attestation_command_allowlist_digest: UNKNOWN
runner_attestation_grant_ref: NOT_GRANTED
setup_execution_grant_ref: NOT_GRANTED
canary_execution_grant_ref: NOT_GRANTED
independent_readback_ref: UNAVAILABLE
valid_until_or_recheck_trigger: UNSET
status: NOT_ATTESTED
```

Runner kernel/image/policy/permissions/owner change or validity expiration immediately invalidates the record. No `UNKNOWN` may be cast to `NONE`, `DENIED`, `ZERO` or `SAFE`.

## 4. Precise executable-gate separation and negative Oracle review

```text
AUTHOR DESIGN + CONDITIONAL RE-REVIEW
             |
             v
EXACT-DIFF AMENDMENT INDEPENDENT REVIEW (future)
             |
             v
REAL RUNNER NOMINATION + SIGNED SECURITY/OWNER APPROVAL (future)
             |
             v
R2 READ-ONLY HOST ATTESTATION COLLECTION GRANT (future)
             |
             v
ACTUAL RUNNER EVIDENCE INDEPENDENT READBACK (future)
             |
             v
SEPARATE A1 IMPLEMENTATION AUTHORIZATION (future)
             |
             v
IMPLEMENT EXACT ACCEPTED FILES / INDEPENDENT CODE REVIEW (future)
             |
             v
R3 TRUSTED FIXTURE SETUP EXECUTION GRANT (future)
             |
             v
R4 NON-SPRING CANARY EXECUTION GRANT (future)
             |
             v
INDEPENDENT NEGATIVE ORACLE + EVIDENCE ACCEPTANCE (future)
```

This ordering is a **conditional proposal**, not a statement that real owner permissions can be obtained by an AI review. If an implementation-only authorization can safely precede live R2 inventory under a chosen owner-reviewed target platform, that is a separately documented authorization alternative—not an implicit waiver of runtime verification. Execution remains blocked until the real policy is attested.

**Negative case safety contract:** A1-N01..10 only synthetic destinations, no real metadata URL, DB/Oracle/MySQL, public network, company hostname, secrets or PHI. Trusted SETUP positive controls must not relax measured CANARY policy; any unobtainable fixture/denial attribution => `INCOMPLETE_EVIDENCE`, never an inferred `ISOLATION_POLICY_PASS`. A no-NIC VM attestation may support only a narrowed no-network-capability claim pending separate review, not a fake firewall-rule hit.

## 5. Required evidence and review gates

| Gate | Evidence | Current decision |
|---|---|---|
| `A1-CA-G01` | Exact main/author PR/review PR/source blob identities | `SOURCE_PINS_RECORDED` |
| `A1-CA-G02` | Six-file candidate exact path/diff inventory, exclusion set and owners | `DESIGN_CANDIDATE` |
| `A1-CA-G03` | Independent acceptance of controlled amendment `D1-01-T/F/O/E` | `PENDING_INDEPENDENT_REVIEW` |
| `A1-CA-G04` | Concrete runner identity and owner/OS/kernel/UID/network/mount/FD policy | `NOT_PROVEN` |
| `A1-CA-G05` | Explicit, safe R2 host attestation collection authority and command policy | `NOT_GRANTED` |
| `A1-CA-G06` | Runner-bound exact policy/hash and guard attempt-coverage evidence | `NOT_PROVEN` |
| `A1-CA-G07` | Independently authored/frozen test Oracle and setup preconditions, no denial vs unreachable confusion | `DESIGN_CONTRACT_ONLY` |
| `A1-CA-G08` | Security and Foundation/U01 owner approval of synthetic setup/runner/retention | `NOT_GRANTED` |
| `A1-CA-G09` | Separate exact-file implementation grant after readiness re-evaluation | `NOT_GRANTED` |
| `A1-CA-G10` | Separate setup/canary execution grants and independent evidence verification | `NOT_GRANTED` |
| `A1-CA-G11` | No scope contamination (A2/B/production/CI/PHI/DB), no automatic merge | `PASS / DESIGN_ONLY` |

### 5.1 Negative-case verdict precedence

For any measured case: `UNEXPECTED_SUCCESS/ISOLATION_POLICY_FAIL` outranks any positive; `INVALID_PROVENANCE` rejects the run; `INVALID_FIXTURE`, `UNREACHABLE_TARGET`, `REFUSED_NO_LISTENER`, `NO_DNS_RESOLVER`, `OBSERVABILITY_GAP` => `INCOMPLETE_EVIDENCE`; `DENIED_BY_POLICY` becomes an individually admitted observation **only after** policy-origin attribution and negative control. Aggregate `ISOLATION_POLICY_PASS` requires all relevant cases independently admitted plus teardown and separate security review; ambiguous or unexecuted cases are never automatically exempted.

### 5.2 Finding traceability

| Finding | This amendment/plan response | Disposition |
|---|---|---|
| `BF-U07-A1-IR-01` | Preserves denial provenance vs unreachable classifier, negative control in D1-01-O/E | `CLOSED_CONDITIONAL_DESIGN` from PR #325; physical `NOT_VERIFIED` |
| `BF-U07-A1-IR-02` | Proposes distinct D1-01-T/F/O/E; freezes SETUP/CANARY/TEARDOWN contracts and file paths | `CLOSED_CONDITIONAL_DESIGN` from PR #325; amendment approval `PENDING` |
| `A1-DESIGN-RF-01` | Six-file exact diff proposed with four named additional files | `AMENDMENT_DESIGN_PROPOSED / NOT_AUTHORIZED` |
| `SA-BF-01` | Runner identity fields, R1/R2 approval/readback plan; no fabricated machine | `OPEN` |
| `SA-BF-02` | Independent Oracle and separately gated setup/canary execution plan; no runnable evidence | `OPEN` |
| `SA-BF-03/04` | Explicitly not part of Stage A1 | `OPEN / A2` |
| `RF-U07-A1-IR-01/02` | Actual Security approval/attempt coverage remain required | `REQUIRED` |

## 6. Immutable excluded scope

No files under `diagnosis-service/src/main/java/**`, no `diagnosis-service/pom.xml` or `application.yml`, no database migrations, no U06/U15/P02 authority changes, no `tools/u07_foundation_tx_topology/oracle/stage_a2_*`, no `.github/workflows/**`, no production secret/patient/PHI, no Spring Context, JDBC, SQL, Redis/Nacos remote integration, no external connectivity, no host privilege escalation, no automatic CI trigger or merge. Future executable actions require their own exact grants; this design document never supplies one.

## 7. Requested next independent review and formal decision

**Next step:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Controlled Exact-Diff Amendment + Runner Authority Evidence Plan Independent Design Review`, pinned to this author's exact HEAD and blob. Review must check all six candidate paths and ownership, whether new paths are necessary yet minimal, safety of R2 inventory collection without ambient host access, any circular authorization dependencies, fixture/control feasibility and independent policy-denial evidence. Reviewer should return `PASS / CONDITIONAL_DESIGN_ACCEPTANCE` or `REVISE_REQUIRED` **for design only**. A PASS does not confer a real-runner/Security grant, accept an executable Oracle, permit code writes, execute setup/canary tests, start Spring or merge.

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_CONTROLLED_EXACT_DIFF_AND_RUNNER_AUTHORITY_EVIDENCE_PLAN
= DESIGN_CANDIDATE / INDEPENDENT_REVIEW_PENDING

BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
AUTHOR_PR323 = c9d495620b74706c57d79e9cdd8528f46e8a5e04
DESIGN_REVIEW_PR325 = db6de3c4a9ffe33d5048dad81b3b3d570f844617
PROPOSED_DIFF = D1-01, D1-02, D1-01-T, D1-01-F, D1-01-O, D1-01-E
ACCEPTED_IMPLEMENTATION_DIFF = NONE
SA-BF-01/02 = OPEN
SA-BF-03/04 = OPEN / STAGE_A2
A1_DESIGN_RF_01 = AMENDMENT_PROPOSED / NOT_AUTHORIZED
RUNNER_IDENTITY = UNKNOWN / NOT_ATTESTED
RUNNER_ATTESTATION_COLLECTION = NOT_AUTHORIZED
A1_IMPLEMENTATION = NOT_AUTHORIZED
TRUSTED_SETUP_EXECUTION = NOT_AUTHORIZED
CANARY_EXECUTION = NOT_AUTHORIZED
STAGE_B_SPRING = NOT_AUTHORIZED
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```
