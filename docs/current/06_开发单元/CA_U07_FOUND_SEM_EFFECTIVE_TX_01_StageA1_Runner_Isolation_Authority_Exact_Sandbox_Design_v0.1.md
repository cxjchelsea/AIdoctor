# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Isolation Authority + Exact Sandbox Design v0.1

> Design date: 2026-10-09
> Main evidence baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654`
> Governing readiness: [PR #322](https://github.com/cxjchelsea/AIdoctor/pull/322) exact HEAD `9371fd810c86d5cd2dbe79fc5d5336624a8e3795`, report blob `edd4b6d8fa0707343b8f64b9568c67752b17d64b`, `SA-BF-01/02` OPEN.
> Prior design: [PR #319](https://github.com/cxjchelsea/AIdoctor/pull/319) exact HEAD `7f9b056dde656e69bf7a061f0e41ba6cb8b832ca`, design blob `370bd1dd4fac8289a2062c221a781eb599f4a1c5`; independent conditional review [PR #321](https://github.com/cxjchelsea/AIdoctor/pull/321) @ `8ad0a89d072c74e7b31cf0d6f0334e667f802658`.
> **Decision: DESIGN_CANDIDATE / RUNNER_NOT_ATTESTED / INDEPENDENT_REVIEW_PENDING.** No authority grant, shell implementation, canary execution, Spring, JDBC, CI change, production or merge.

## 1. Objective and phase boundary

Stage A1 shall define a **specific, enforceable pre-JVM OS isolation design** and independently reviewable evidence/authorization chain for future non-Spring sandbox tests. It shall not attempt to prove actual Spring transaction topology or allow Stage B. The safety properties are **default deny at kernel/sandbox boundary before any JVM or Spring code**, no company/patient resources, and fail-closed when isolation is unsupported. This document responds only to `SA-BF-01 RUNNER_IDENTITY_AND_PREVENTION_NOT_FROZEN` and `SA-BF-02 CANARY_PHASE_EVIDENCE_AND_EXECUTION_AUTHORITY_MISSING` and related `RF-U07-FOUND-PB-IR-01/02`. It does not remediate Stage A2 SA-BF-03/04.

## 2. Exact observed repository evidence versus selected future runner

| Evidence | Observation | Authority |
|---|---|---|
| `.github/workflows/foundation-0-verification.yml` | Blob `eb80668db53c9ac497de99a774434ec2b73d62a2`; normal Java workflow `runs-on: ubuntu-latest`, JDK8 setup and Maven tests; no sandbox isolation test or pre-JVM deny attestation | **CI SOURCE_ONLY**; not a Tier-1 runner grant |
| `diagnosis-service/pom.xml` | Blob `dfa3c6f1cfc274ca796bf1bde8824ed1d9b80bb5`; Spring Boot 2.7.8/JPA/AOP/Flyway, Redis/Nacos and JDBC drivers | Illustrates need to prohibit boot-time I/O; **no JVM allowed in Stage A1** |
| `diagnosis-service/src/main/resources/application.yml` | Blob `7a9d6931da6763f83769a5234ac8aa77459b0be0`; `dev` active | **NOT** synthetic isolation authority |
| Actual isolated nonproduction runner ID, OS/kernel, namespace privileges, Linux security module, network/UID policy | No attested, owner-approved actual runner evidence supplied by current audit | **UNKNOWN / NOT_READY** |

### 2.1 Candidate deployment profile (selected architecture, not an identified machine)

**Candidate:** dedicated, ephemeral, **nonproduction Linux x86_64 or aarch64 sandbox runner** in a separately administered runner pool, with immutable runner image digest and OS/kernel build identity. This is an **implementation candidate**, not a claim the repository has such a host today. Prefer a provisioned disposable VM or tightly controlled runner with **network-disabled guest/isolated execution boundary** plus process, mount and syscall containment. The Security owner must select the concrete host/VM/container implementation and demonstrate controls on that **exact platform** before any code-implementation permission. Standard GitHub-hosted `ubuntu-latest` is **not presumed** to support privileged namespace, eBPF/LSM or host firewall policy modification. No modification to existing workflows.

**Alternative evaluation:** if a hardened VM with no virtual NIC, no host/shared sockets or drives, no credentials and read-only source/dependencies offers an independently attested deny boundary, it may replace a rootless container strategy only by an explicit controlled design revision. No switch to unrestricted host execution after a denial-policy failure.

### 2.2 Required Runner Authority Record (all fields mandatory; current values remain UNKNOWN)

```yaml
schema: U07StageA1RunnerAuthorityV1
runner_id: UNKNOWN
environment_class: SYNTHETIC_NONPATIENT_ONLY
physical_or_vm_host_owner: UNKNOWN
security_owner_grant_ref: NOT_GRANTED
foundation_u01_owner_grant_ref: NOT_GRANTED
runner_os_family: LINUX_CANDIDATE_NOT_ATTESTED
kernel_release_build_digest: UNKNOWN
architecture: UNKNOWN
runner_image_immutable_digest: UNKNOWN
container_or_vm_runtime_and_version: UNKNOWN
uid_gid_mapping_and_capabilities: UNKNOWN
namespaces_or_equivalent_enforcement: UNKNOWN
network_stack_and_egress_denial: UNKNOWN
host_network_or_vsock_exposure: UNKNOWN
loopback_ipv4_ipv6_dns_denial: UNKNOWN
unix_socket_and_host_mount_denial: UNKNOWN
metadata_endpoints_denial: UNKNOWN
child_process_and_exec_policy: UNKNOWN
filesystem_roots_ro_and_scratch_policy: UNKNOWN
secrets_credentials_and_patient_mounts_absent: UNKNOWN
pre_jvm_guard_attachment_and_attempt_coverage: UNKNOWN
canary_target_set_is_synthetic: UNKNOWN
independent_security_reviewer: UNASSIGNED
policy_sha256: UNKNOWN
authority_status: NOT_ATTESTED
```

No placeholder may be interpreted as `false`, `none`, `blocked`, `pass` or proof of zero attempts. Frozen implementation authority must bind SHA256 of actual runner policy, image and owner-reviewed controls to a stable runner ID and an expiration/reevaluation rule.

## 3. Explicit prevention boundary (minimum controls)

| Attack/failure surface | Preventive boundary before canary process/JVM | Required validation |
|---|---|---|
| IPv4/IPv6 outbound and loopback, TCP/UDP, DNS, raw sockets | VM network interface absent or kernel policy default-deny for chosen process/namespace; drop privileges; no host networking | Non-Spring synthetic socket/DNS canaries denied, independent blocked-attempt audit; **not** real internet target |
| Unix-domain sockets and inherited FDs | No mounted host Docker/DB/Redis/Nacos or privileged socket; close-on-exec verified, isolated IPC/FS namespaces, denied socket paths | Synthetic local socket/connect canary, inherited FD sentinel |
| Cloud instance metadata/host/vsock | No network route or hypervisor host-channel; no guest shared services | Offline policy proof and harmless synthetic denial fixture; never probe real metadata URL |
| Read/write mounts and filesystem | Root/source/dependency mounts read-only; isolated bounded scratch; no writable host home/cache/secrets; `no-new-privileges`/seccomp or equivalent | Synthetic write denial outside scratch, scratch quota, mount digest, no external persistence |
| Processes, syscalls, JNI/native, subprocess escape | Least-privileged UID/capability set; deny namespace/privilege escalation, unauthorized exec, ptrace and privileged IO; children inherit policy | Fork/exec/child-network canary, child lineage evidence; any unmonitored path UNKNOWN |
| Environment and credentials | Explicit allowlisted synthetic variables, empty inherited secret mounts/environment, no service tokens | Sanitized presence/absence attestations; do not log secret values |
| JDBC/pool/Flyway/Hibernate/Redis/Nacos and boot hooks | Not executed in Stage A1; later Stage B needs **separate pre-JVM attempt instrumentation** plus continued kernel deny | Coverage manifest must enumerate Java/Spring startup entry points, return `INCOMPLETE_EVIDENCE` for uncovered surfaces |

**Preventive versus observational control:** OS/VM boundary prevents any access, while optional syscall tracing/test-only hooks observe attempts. A log reporting zero successful network calls alone is not sufficient. If kernel enforcement is unavailable, DO NOT substitute application-level monkeypatching or a shell environment variable. If a canary succeeds, fail permanently for that candidate policy and require a new review; never relax isolation to obtain a green status.

## 4. Split-phase execution and negative test Oracle

The Stage A1 design contains a **future** negative harness, not an instruction to run it now:

```text
AUTHORIZE_DESIGN_REVIEW
    |
    v
AUTHORIZE_A1_IMPLEMENTATION (separate decision, exact D1-01/02 only)
    |
    v
INDEPENDENT_REVIEW_IMPLEMENTED_SANDBOX (no JVM)
    |
    v
AUTHORIZE_A1_CANARY_EXECUTION (separate decision)
    |
    v
PREPARE_SANDBOX_CANARY INSTANCE
    |
    v
NEGATIVE DENIAL CANARIES + PER-INSTANCE COUNTERS
    |
    v
TEARDOWN + IMMUTABLE EVIDENCE + INDEPENDENT REVIEW
    |
    v
STAGE_A1_EVIDENCE_ACCEPTANCE_OR_FAIL
    |
    v
FUTURE STAGE_B SEPARATELY AUTHORIZED (NOT HERE)
```

### 4.1 Synthetic canary fixture matrix (design-only, NOT_EXECUTED)

| Case | Harmless target | Expected kernel/sandbox observation |
|---|---|---|
| A1-N01 | Numeric IPv4 synthetic non-routable address/port, no real host | denied connect; zero successful contacts |
| A1-N02 | IPv6 synthetic non-routable destination | denied connect; zero successful contacts |
| A1-N03 | Synthetic DNS query resolved wholly within a disconnected fixture, no real resolver | blocked socket/query attempt under deny policy |
| A1-N04 | Loopback TCP listener only if policy forbids local socket access; fixture created in separate process within isolated sandbox | denied connection; prohibit any successful localhost connection |
| A1-N05 | Local AF_UNIX fixture socket path; do not expose host socket | denied connection; no successful FD exchange |
| A1-N06 | Write attempt outside bounded scratch against synthetic read-only mount | denied; zero unapproved filesystem mutations |
| A1-N07 | Spawn child that attempts same synthetic denied operation | child retains policy; denied; lineage recorded |
| A1-N08 | Privilege/namespace escape or forbidden process property change **only as a harmless capability query/denial probe** | rejected without privileged action |
| A1-N09 | Attempt reach synthetic metadata-shaped endpoint without real metadata service | denied route; no access to real provider metadata |
| A1-N10 | Attempt to pass inherited sensitive socket FD via test sentinel (not a real secret) | FD absent / use rejected |

The proposed canary suite must be reviewed for actual fixture setup feasibility (e.g., not introducing forbidden listeners or legitimate scratch writes that the Oracle would misclassify). Only synthetically bounded, separately permitted fixture initialization operations may be performed by a **trusted setup process outside the measured sandbox**, or under an independently approved setup phase. No real credential, patient data, DB, public network, company hostname or actual cloud metadata endpoint may be a canary target.

### 4.2 Phase accounting and verdict truth table

| Phase | Attempts | Successful prohibited effects | Verdict |
|---|---|---|---|
| `SANDBOX_CANARY` (separate sandbox ID, no JVM) | Expected blocked negative attempts; count >0 with provenance | **0** | `ISOLATION_POLICY_PASS` only with complete required cases and audit |
| `SANDBOX_CANARY` | Any required denied operation succeeds | >0 or denial mismatch | `ISOLATION_POLICY_FAIL`; quarantine and stop |
| `SANDBOX_CANARY` | Any provenance/coverage/fixture uncertainty | UNKNOWN | `INCOMPLETE_EVIDENCE` / NOT_ACCEPTED |
| Future `SPRING_DIAGNOSTIC` (new sandbox ID) | **0 forbidden startup attempts** | **0** | candidate `CONTEXT_CONFIRMED` only after unrelated R2/R3 truth checks |
| Future `SPRING_DIAGNOSTIC` | Any forbidden startup attempt including one blocked | any | `CONTEXT_UNSAFE` |
| Phase-origin contamination, inherited FD or reused counters | UNKNOWN | UNKNOWN | `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE` before JVM, `INCOMPLETE_EVIDENCE` after valid guard attestation |

The **canary phase never writes to Spring diagnostic counters** and its expected blocked attempts are NEVER whitelisted at runtime. Canary teardown precedes a brand-new Spring sandbox; new isolated instance ID, zero-origin counters, no inherited FDs, identical policy hash and new attestation are mandatory. This stage shall not instantiate the future Spring sandbox or boot any JVM.

## 5. Deterministic failure handling and independent evidence

### 5.1 Failure codes by stage

| Condition | Code |
|---|---|
| Missing selected runner/capability/owner/security grant **before** process creation | `A1_NOT_READY` / `NOT_AUTHORIZED` |
| No enforceable pre-JVM preventive deny before potential JVM | `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE` |
| Negative canary forbidden effect succeeded | `ISOLATION_POLICY_FAIL` |
| Canary audit counters, coverage, fixture or process lineage missing | `INCOMPLETE_EVIDENCE` |
| Future Spring startup attempted forbidden access (even blocked) | `CONTEXT_UNSAFE` |
| No independent readback or SHA/provenance mismatch | `INVALID_PROVENANCE` / reject |
| Synthetic canaries passed with policy fully attested | `A1_CANARY_EVIDENCE_ACCEPTED` **only by later independent review**, not by this design |

### 5.2 Evidence bundle manifest v0.1 — proposed schema, no fabricated hashes

```yaml
schema: U07StageA1SandboxEvidenceV1
source_commit_sha: 86e8843197091c8c8172b7e4213537a31bdf0654
source_tree_sha: UNKNOWN
design_author_head_and_blob: REQUIRED
runner_authority_manifest_sha256: UNKNOWN
runner_os_kernel_image_runtime_digest: UNKNOWN
sandbox_policy_sha256: UNKNOWN
candidate_implementation_commit_and_blob_sha: UNKNOWN
security_and_foundation_u01_grants: NOT_GRANTED
canary_execution_grant_ref: NOT_GRANTED
phase: SANDBOX_CANARY
phase_instance_id_and_process_lineage: UNKNOWN
negative_case_oracle_sha256: UNKNOWN
fixture_provenance_sha256: UNKNOWN
pre_execution_policy_attestation_sha256: UNKNOWN
attempt_monitor_coverage_sha256: UNKNOWN
attempted_denied_ops_by_case: UNKNOWN
successful_forbidden_contacts: UNKNOWN
filesystem_mutations_outside_scratch: UNKNOWN
teardown_attestation_sha256: UNKNOWN
independent_evidence_review_ref: NOT_ACCEPTED
evidence_result: NOT_EXECUTED
```

Evidence artifacts must hash individually, be immutable and use event timestamps, policy IDs, case IDs, blocked-attempt reasons, sanitized call-site IDs, reviewer identity, retention policy and exact runner context. No sensitive IP/hostname/URLs, credentials, tenant IDs, SQL or PHI in emitted payloads. An external/independent reviewer must compare actual outputs to pre-frozen Oracle, **not** regenerate expected outputs from the same runner under review.

## 6. Exact future code-diff authorization candidates

**This PR is a single ADD-only Markdown document.** A later **separate** Stage A1 implementation authorization may permit exactly these two prospective file paths:

| ID | Exact path | Future permitted behavior |
|---|---|---|
| `D1-01` | `tools/u07_foundation_tx_topology/sandbox/deny_network.sh` | Target-runner-specific pre-JVM isolation selection/attestation; fail closed when unsupported; no auto execution |
| `D1-02` | `tools/u07_foundation_tx_topology/sandbox/bootstrap_guard_manifest.json` | Typed pre-JVM coverage, allowed synthetic fixtures and phase/evidence schema, no collected secrets |

**Scope tension requiring future explicit resolution:** two files alone might be insufficient to implement the proposed non-Spring synthetic canary harness and standalone Oracle. This design does **not** silently authorize extra scripts/test files: freeze a separate exact-diff controlled amendment with individual paths, owner + security review **before** implementation, or postpone canary implementation/testing entirely. Do not hide test helpers within unrelated preexisting files. The future Stage A1 authorization must explicitly state whether canary harness creation is in scope; it must never imply executing the harness.

**Denied:** `.github/workflows/**` including a new automatically triggered workflow, `diagnosis-service/pom.xml`, production `src/main/java/**`, application.yml, Flyway/migration files, existing Tier-0 static producer, DB/JPA/CDP/U01 stores, Spring Context tests, PHI/PROFILE-A, network/DB/remote services, secrets, production, any merge, Stage A2 D1-04 files and Stage B D1-03/05/06/07.

## 7. Authority decision checklist (no human signoff yet)

| Gate | Required affirmative evidence | Status at design authoring |
|---|---|---|
| A1-G01 exact source/design identity | immutable main/source/design and PR #322 blocker linkage | `SOURCE_CONFIRMED` |
| A1-G02 concrete runner selection | named isolated runner owner, OS kernel/runtime identity, privileges and network topology | `NOT_READY` |
| A1-G03 actual enforcement feasibility | independent Security review of VM/kernel/namespace preventive deny and teardown under chosen UID | `NOT_PROVEN` |
| A1-G04 pre-JVM guard coverage | source, syscall, native/child-process, socket and filesystem attempt inventory | `DESIGN_ONLY` |
| A1-G05 negative canary Oracle | pre-frozen independently reviewed synthetic cases and approved setup actions | `DESIGN_ONLY` |
| A1-G06 exact future diff | D1-01/D1-02 + any additional canary scripts individually included by amendment | `REQUIRES_AMENDMENT_IF_EXPANDED` |
| A1-G07 Security / Foundation U01 grant | named reviewers sign synthetic/nonpatient scope, evidence retention, no secrets | `NOT_GRANTED` |
| A1-G08 implementation authorization | separate, explicit implementation-only decision pinned to accepted exact scope | `NOT_GRANTED` |
| A1-G09 canary execution authorization | separate after implementation independent verification | `NOT_GRANTED` |
| A1-G10 independent design review | review this exact document/HEAD for controls, fixtures, authority and limits | `PENDING` |

No Stage A1 gate may be marked PASS on the basis of a future planned command or an unverified Github-hosted runner. Any new main HEAD requires pin/diff revalidation. Stage A2 and Stage B remain separate.

## 8. Findings, next review and decision

| Finding | Requirement | Current |
|---|---|---|
| `SA-BF-01` | Real runner/platform Security authorization and enforceable deny evidence | `OPEN / SELECTED_ARCHITECTURE_ONLY` |
| `SA-BF-02` | Approved separated canary procedure, future execution grant and physical evidence | `OPEN / DESIGN_ONLY` |
| `SA-RF-01` | Security + Foundation/U01 owner consent, retention/redaction | `REQUIRED` |
| `A1-DESIGN-RF-01` | Resolve possible two-file diff vs independent canary harness/test helper needs by explicit amendment | `REQUIRED_BEFORE_IMPLEMENTATION` |
| `SA-BF-03/04` | A2 independent Oracle/Framework dependency and exact-diff approval | `OPEN / OUT_OF_THIS_A1_SCOPE` |

**Next:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Isolation Authority + Exact Sandbox Independent Design Review` on exact author HEAD and blob; expect `PASS / CONDITIONAL_ACCEPTANCE` or `REVISE_REQUIRED` on OS/VM enforcement truthfulness, ability to generate a safe negative canary, distinction between selected architecture and approved actual runner, the two-file scope tension and execution authority. Then separately collect real runner authority, review any necessary canary harness exact-file amendment, and decide whether narrowly bounded A1 implementation may be authorized. No Stage B from an A1 design-only PASS.

```text
STAGE_A1_RUNNER_AUTHORITY_AND_SANDBOX_DESIGN = DESIGN_CANDIDATE
MAIN_BASELINE = 86e8843197091c8c8172b7e4213537a31bdf0654
SELECTED_RUNNER_ARCHITECTURE = EPHEMERAL_NONPRODUCTION_LINUX_VM_CANDIDATE
ACTUAL_RUNNER_IDENTITY = UNKNOWN / NOT_ATTESTED
PREVENTIVE_OS_BOUNDARY = SPECIFIED_DESIGN / NOT_VALIDATED
SANDBOX_CANARY_ORACLE = DESIGN_ONLY / NOT_EXECUTED
A1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
A1_CANARY_EXECUTION_AUTHORIZATION = NOT_GRANTED
SPRING_DIAGNOSTIC = NOT_AUTHORIZED / NOT_EXECUTED
SA-BF-01 = OPEN
SA-BF-02 = OPEN
SA-BF-03/04 = OPEN / A2_OWNER
TIER1_IMPLEMENTATION_READINESS = NOT_READY
BF-U07-FOUND-TX-INT-01..03 = OPEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```
