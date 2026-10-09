# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Isolation Authority + Exact Sandbox Design v0.1

> Design date: 2026-10-09
> Main evidence baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654`
> Governing readiness: [PR #322](https://github.com/cxjchelsea/AIdoctor/pull/322) exact HEAD `9371fd810c86d5cd2dbe79fc5d5336624a8e3795`, report blob `edd4b6d8fa0707343b8f64b9568c67752b17d64b`, `SA-BF-01/02` OPEN.
> Prior design: [PR #319](https://github.com/cxjchelsea/AIdoctor/pull/319) exact HEAD `7f9b056dde656e69bf7a061f0e41ba6cb8b832ca`, design blob `370bd1dd4fac8289a2062c221a781eb599f4a1c5`; independent conditional review [PR #321](https://github.com/cxjchelsea/AIdoctor/pull/321) @ `8ad0a89d072c74e7b31cf0d6f0334e667f802658`.
> Targeted remediation of [PR #324](https://github.com/cxjchelsea/AIdoctor/pull/324) exact HEAD `0b93405d5bba124a0e2464605de3aa2a24c237e2`: `BF-U07-A1-IR-01/02`, `RF-U07-A1-IR-01/02`. The findings are **proposed for design closure only**, pending a new exact-head independent re-review.  
> **Decision: TARGETED_DESIGN_REMEDIATED / RUNNER_NOT_ATTESTED / INDEPENDENT_RE_REVIEW_PENDING.** No authority grant, shell implementation, canary execution, Spring, JDBC, CI change, production or merge.

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
TRUSTED_FIXTURE_SETUP (distinct authority, process, namespace, counters)
    |
    v
FREEZE_FIXTURE_PRECONDITION & POLICY/NEGATIVE-CONTROL ORACLE
    |
    v
CREATE/FREEZE SANDBOX_CANARY INSTANCE (no inherited open listener/FD)
    |
    v
NEGATIVE DENIAL CANARIES + POLICY-ATTRIBUTED ATTEMPTS
    |
    v
TEARDOWN CANARY & SETUP + IMMUTABLE EVIDENCE + INDEPENDENT REVIEW
    |
    v
STAGE_A1_EVIDENCE_ACCEPTANCE_OR_FAIL
    |
    v
FUTURE STAGE_B SEPARATELY AUTHORIZED (NOT HERE)
```

### 4.1 Synthetic canary fixtures + enforceable denial provenance (BF-U07-A1-IR-01)

**Normative distinction:** `connect() returned error` and `zero successful contacts` are **not** proof of a preventive isolation rule. For every test, the accepted result requires policy-origin evidence from the chosen kernel/VM enforcement boundary tied to the exact `policy_sha256`, isolated instance ID, process/UID/cgroup/namespace identity, target fixture ID and event/case correlation. The Oracle must classify these disjoint outcomes: `DENIED_BY_POLICY`, `UNREACHABLE_TARGET`, `REFUSED_NO_LISTENER`, `NO_DNS_RESOLVER`, `INVALID_FIXTURE`, `OBSERVABILITY_GAP`, `UNEXPECTED_SUCCESS`. Only `DENIED_BY_POLICY` with complete enforcement attribution and no prohibited success can PASS a required denial case; do not infer denial from `ENETUNREACH`, `ECONNREFUSED`, `ENOENT`, `EAI_NONAME` or a timeout alone. OS error names are *illustrative only*: accepted kernel-specific errno/audit event mapping must be frozen for the selected OS before testing. Rejected fixture/unknown telemetry is `INCOMPLETE_EVIDENCE`, not a retry under relaxed policy.

**Expected event record for each case:** `case_id, policy_sha256, runner_id, setup_fixture_id, setup_authority_ref, fixture_precondition_sha256, attempt_timestamp_monotonic, canary_instance_id, pid_lineage_token, uid_gid_token, namespace_or_vm_identity_digest, operation_class, target_token, sys_call_or_policy_event_type, errno_or_event_class, enforcement_layer, enforced_rule_id, policy_denial_event_ref, attempted_count, prohibited_success_count, observability_coverage_ref, outcome_code, oracle_sha256`. Run-local tokens must never reveal real host IP, secrets, connection strings, tenant IDs or PHI. `enforced_rule_id` with a valid independent VM/firewall/kernel audit event, or an independently attested deterministic preventative policy proof when specific event telemetry is unsupported, is mandatory; a guessed rule ID does not qualify.

#### 4.1.1 Revised negative Oracle matrix (design-only, NOT_EXECUTED)

| Case | Safe synthetic fixture and precondition | Expected outcome and discriminator |
|---|---|---|
| `A1-N01` | Setup-owned IPv4 fixture service or independently attested egress policy-target mapping; no public or corporate destination | `DENIED_BY_POLICY` with preventive-rule correlation; `UNREACHABLE_TARGET` must FAIL/UNKNOWN |
| `A1-N02` | Analogous isolated IPv6 service/route or independently attested explicit family-deny rule | Policy-origin denial; no-route/unsupported IPv6 is `INVALID_FIXTURE` or `INCOMPLETE_EVIDENCE` |
| `A1-N03` | Setup-owned offline synthetic DNS responder/transport or policy-specific denied DNS socket fixture; no real resolver | Correlated DNS socket policy denial; `NO_DNS_RESOLVER` or name-not-found cannot PASS |
| `A1-N04` | Trusted setup service reachable *absent measured denial*, via controlled separate fixture path/namespace; no fixture listener created under measured CANARY policy | Correlated loopback policy denial; no listener/refusal is `INVALID_FIXTURE` |
| `A1-N05` | Setup-owned AF_UNIX listener path within isolated synthetic fixture, precondition attested and visible in controlled mount namespace with permission checks distinguished | Denial by identified socket/path policy; missing path or no listener cannot PASS |
| `A1-N06` | Synthetic read-only mounted path with setup attestation, scratch-write positive control in SETUP only | Policy/mount-origin denial of out-of-scope write; `ENOENT` invalid; successful unapproved mutation FAIL |
| `A1-N07` | Setup-owned synthetic destination; canary child inherits exact policy and can be traced | Child attempt denied by same preventive layer and linked lineage; child spawn itself only if explicitly allowed for this test |
| `A1-N08` | Harmless capability/namespace policy inspection (no real privilege escalation or host modification) | Explicit denial or separately attested absence of dangerous capability; mere no-op/error unsupported cannot PASS |
| `A1-N09` | Synthetic metadata-shaped fixture/denied route without real cloud metadata contact | Explicit correlated deny; target being absent/unroutable alone cannot PASS |
| `A1-N10` | Trusted setup synthetic FD sentinel with bounded token; no actual sensitive file/socket or privilege | Inheritance rejected/FD absent by specific policy or independently inspected descriptor table; missing test sentinel before setup = invalid |

Where the candidate runner cannot present a fixture *and* independently attribute the denial to its actual prevention layer, designate the corresponding test `NOT_EXECUTABLE_ON_SELECTED_RUNNER` and block the full `ISOLATION_POLICY_PASS` conclusion. A VM with no NIC may produce a valid network-absence **capability attestation** for a narrower claim, but not falsely claim that a policy rule intercepted a connect call. Such substitution requires its own reviewed oracle/expected evidence and must never borrow `DENIED_BY_POLICY` labels.

#### 4.1.2 Noncircular control and independence

An approved trusted **SETUP-only** control may attest that the synthetic listener/address/path exists and is accessible in a separately isolated non-patient *fixture context without the measured denial*, or provide a deterministic OS/kernel rule-evaluation proof tying a synthetic target and namespace to the blocked rule. This control must not use the production network, actual metadata endpoint, external DNS, patient systems, or relax the **measured** canary sandbox. Record independently frozen `fixture_precondition_hash` and `control_scope`; do not construct the expected verdict from the same failed canary event. If precondition/control cannot be safely demonstrated, classify `INVALID_FIXTURE`/UNKNOWN rather than pretending an unreachable address was blocked.

### 4.2 Trusted SETUP → measured CANARY → TEARDOWN authority (BF-U07-A1-IR-02)

**Strict owner and namespace boundaries:**

| Phase | Owner and permitted effects | Forbidden and provenance |
|---|---|---|
| `TRUSTED_FIXTURE_SETUP` | Separately authorized synthetic fixture owner in a dedicated isolated namespace/process; create synthetic listeners, paths, FD sentinel, preconditions and expected-target map in bounded scratch; optional offline fixture-only reachability control | No real network/DB/metadata/PHI or company sockets; setup writes/listeners never counted as canary successes; record allowed setup ops, setup instance ID, fixture manifest SHA, setup counter and sealed cutoff |
| `SANDBOX_CANARY` | Fresh measurement namespace/instance and process lineage; only the pre-approved canary operations; zero allowed prohibited network/write effects | Never creates listeners or reconfigures firewall, never reuses setup counters/UID escalation; must refer to fixture tokens and policy origin; denied attempts counted **here only** |
| `TEARDOWN` | Trusted controller independently destroys canary and fixture processes/FDs/mounts, records cleanup/readback and immutable hashes | No inherited open socket/FD, writable host mount or reused sandbox; uncertain cleanup => `INCOMPLETE_EVIDENCE` and no downstream execution |
| `SPRING_DIAGNOSTIC` **future, OUT_OF_SCOPE** | Only under separate Stage B authorization in completely new denied instance with zero-origin counters | No canary exceptions, listeners, previous instance/FDs, unauthorized startup I/O or Spring invocation under A1 |

**Fixture lifecycle requirements:** trusted SETUP is not automatically approved by A1 shell design or implementation authorization. It requires *separate execution approval*, precise permitted synthetic operations, real Runner threat model and no external target. Setup must finish before the CANARY measurement cutoff. If a fixture must remain alive, it stays in an independently restricted setup namespace with only minimal synthetic communication path visible to canary; reviewer must prove that path cannot route to host/production and that the measured kernel deny is effective on its attempted access. Cross-phase listener/FD visibility and positive controls are attested without granting an exception in the measured canary policy. Any need to open a listener **inside** the measured sandbox violates the default canary design and requires a new exact-head amendment. Close all sentinels at TEARDOWN and prove no inherited FD at any later Stage B boundary.

**Per-phase manifest fields:** `setup_grant_ref, setup_controller_id, setup_isolation_digest, setup_allowed_operations, setup_fixture_manifest_sha256, synthetic_listener_tokens, fd_sentinel_token, setup_write_manifest, setup_counter_origin, setup_finished_at_monotonic, canary_grant_ref, canary_instance_id, canary_policy_sha256, canary_counter_origin_zero, negative_control_provenance_sha256, canary_end_at_monotonic, teardown_fixture_digest, teardown_canary_digest, no_inherited_fd_evidence, independent_reviewer_ref`.

**Design fail-closed:** failed fixture precondition, unsafe setup, missing policy-origin attribution, missing cross-phase identity binding, unmonitored subprocess/FD or teardown ambiguity must never yield `ISOLATION_POLICY_PASS`; output `INVALID_FIXTURE`, `INCOMPLETE_EVIDENCE` or `ISOLATION_POLICY_FAIL` with every failure fact preserved. No actual SETUP/CANARY/TEARDOWN occurs in this design revision.

### 4.3 Phase accounting and verdict truth table



| Phase | Attempts | Successful prohibited effects | Verdict |
|---|---|---|---|
| `SANDBOX_CANARY` (separate sandbox ID, no JVM) | Required attempts **attributed to an effective policy denial**, matching frozen fixture preconditions | **0** | `ISOLATION_POLICY_PASS` only for complete per-case `DENIED_BY_POLICY` (or separately reviewed narrower prevention attestation) with independent audit |
| `SANDBOX_CANARY` | Any required denied operation succeeds | >0 or denial mismatch | `ISOLATION_POLICY_FAIL`; quarantine and stop |
| `SANDBOX_CANARY` | `UNREACHABLE_TARGET`, `REFUSED_NO_LISTENER`, `NO_DNS_RESOLVER`, `INVALID_FIXTURE`, ambiguous denial origin or unknown coverage | UNKNOWN | `INCOMPLETE_EVIDENCE` / NOT_ACCEPTED; cannot be transformed into deny PASS |
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
| Synthetic fixture missing, address unreachable or connection refused without policy attestation | `INVALID_FIXTURE` / `INCOMPLETE_EVIDENCE`; never PASS |
| Preventive rule explicitly correlated with denied case and independently verified control | `DENIED_BY_POLICY` (case-level only, not automatic final PASS) |
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
trusted_setup_grant_ref: NOT_GRANTED
setup_instance_and_policy_digest: UNKNOWN
setup_allowed_operations_manifest_sha256: UNKNOWN
fixture_precondition_and_negative_control_digest: UNKNOWN
setup_end_canary_start_cutoff_monotonic: UNKNOWN
setup_fixture_communication_path_attestation: UNKNOWN
canary_case_enforcement_rule_event_refs: UNKNOWN
canary_denial_vs_unreachable_classification: UNKNOWN
phase_counter_isolation_proof: UNKNOWN
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

**Controlled exact-diff amendment candidate (NEW PATHS, NOT_AUTHORIZED):** the D1-01/D1-02 pair can define an isolation policy and coverage manifest, but cannot by itself deliver reproducible trusted setup, negative-control and independent oracle evidence. The following are **proposed, precisely frozen additional paths**, to be independently reviewed and explicitly added to an accepted exact-diff authorization *before* any code/file creation:

| Candidate ID | Exact future path | Separate role / required authority |
|---|---|---|
| `D1-01-T` | `tools/u07_foundation_tx_topology/sandbox/test_deny_network_canaries.py` | Standalone **non-JVM synthetic** canary driver, phase-local counters and policy-event readback; NO execution authorization |
| `D1-01-F` | `tools/u07_foundation_tx_topology/sandbox/synthetic_fixture_setup.py` | Trusted offline setup/teardown controller, synthetic listener/path/FD sentinel and setup-only control; separate setup execution grant |
| `D1-01-O` | `tools/u07_foundation_tx_topology/sandbox/canary_expected_oracle.json` | Independently pre-frozen A1-N01..10 expected event classes, policy rule mappings, fixture preconditions and denied/unreachable discriminators |
| `D1-01-E` | `tools/u07_foundation_tx_topology/sandbox/sandbox_evidence_schema.json` | Typed source/policy/phase/event/fixture provenance and fail-closed evidence validation fields |

**Exact-diff rule:** these four paths are **only candidates for a future controlled amendment**. The authorized list remains `D1-01/02` only if and when separately granted; this document grants neither pair nor additions. If an independently safe minimal two-file scope is chosen instead, all A1-N01..10 execution and Oracle acceptance remain deferred and no `SA-BF-02` closure may be claimed. If fixtures require any further file or executable, a new exact-diff amendment and review are mandatory, never implicit expansion or helper insertion into unrelated files.

**Independence and ordering:** the Oracle must be frozen and reviewed separately from the code under test; actual runner policy and allowed OS-specific syscall outcomes must be pinned after the real Runner is selected and **before** negative test execution. Stage A1 implementation, trusted fixture SETUP execution, CANARY execution, evidence acceptance, and Stage B each require their own authorization and independent review.

**Denied:** `.github/workflows/**` including a new automatically triggered workflow, `diagnosis-service/pom.xml`, production `src/main/java/**`, application.yml, Flyway/migration files, existing Tier-0 static producer, DB/JPA/CDP/U01 stores, Spring Context tests, PHI/PROFILE-A, network/DB/remote services, secrets, production, any merge, Stage A2 D1-04 files and Stage B D1-03/05/06/07.

## 7. Authority decision checklist (no human signoff yet)

| Gate | Required affirmative evidence | Status at design authoring |
|---|---|---|
| A1-G01 exact source/design identity | immutable main/source/design and PR #322 blocker linkage | `SOURCE_CONFIRMED` |
| A1-G02 concrete runner selection | named isolated runner owner, OS kernel/runtime identity, privileges and network topology | `NOT_READY` |
| A1-G03 actual enforcement feasibility | independent Security review of VM/kernel/namespace preventive deny and teardown under chosen UID | `NOT_PROVEN` |
| A1-G04 pre-JVM guard coverage | source, syscall, native/child-process, socket and filesystem attempt inventory | `DESIGN_ONLY` |
| A1-G05 negative canary Oracle | per-case policy-origin enforcement evidence, denied-vs-unreachable discriminators, separate trusted SETUP preconditions and independent Oracle | `TARGETED_DESIGN_PROPOSED / NOT_EXECUTED` |
| A1-G06 exact future diff | D1-01/02 and separately reviewed candidate D1-01-T/F/O/E paths for actual harness/fixtures/oracle/evidence | `CONTROLLED_AMENDMENT_REQUIRED / NOT_AUTHORIZED` |
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
| `A1-DESIGN-RF-01` | Exact candidate D1-01-T/F/O/E now specified; separate controlled amendment and grants still required | `DESIGN_PROPOSED / AUTHORIZATION_PENDING` |
| `BF-U07-A1-IR-01` | Independently policy-attributed denied events, unreachable-vs-denied discriminator and actual synthetic fixture precondition | `TARGETED_DESIGN_REMEDIATED / PENDING_RE_REVIEW` |
| `BF-U07-A1-IR-02` | Trusted SETUP/CANARY/TEARDOWN boundaries and exact canary harness/oracle candidate paths | `TARGETED_DESIGN_REMEDIATED / PENDING_RE_REVIEW` |
| `SA-BF-03/04` | A2 independent Oracle/Framework dependency and exact-diff approval | `OPEN / OUT_OF_THIS_A1_SCOPE` |

**Next:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Isolation Targeted Independent Design Re-Review` on the NEW exact author HEAD and blob; expect `PASS / CONDITIONAL_ACCEPTANCE` or `REVISE_REQUIRED` on OS/VM enforcement truthfulness, ability to generate a safe negative canary, distinction between selected architecture and approved actual runner, the two-file scope tension and execution authority. Then separately collect real runner authority, review any necessary canary harness exact-file amendment, and decide whether narrowly bounded A1 implementation may be authorized. No Stage B from an A1 design-only PASS.

```text
STAGE_A1_RUNNER_AUTHORITY_AND_SANDBOX_DESIGN = TARGETED_DESIGN_REMEDIATED / INDEPENDENT_RE_REVIEW_PENDING
BF-U07-A1-IR-01 = REMEDIATION_PROPOSED / OPEN_UNTIL_RE_REVIEW
BF-U07-A1-IR-02 = REMEDIATION_PROPOSED / OPEN_UNTIL_RE_REVIEW
D1-01-T/F/O/E = PROPOSED_CONTROLLED_AMENDMENT / NOT_AUTHORIZED
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
