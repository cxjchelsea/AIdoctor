# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Docker Desktop Candidate Runner Baseline Evidence v0.1

> Date recorded: 2026-10-09.
> Source: user-supplied **local Windows CMD transcript** in the current project conversation, not a verified/signed independent host attestation.
> Repository baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654`; preceding evidence-intake readiness [PR #332](https://github.com/cxjchelsea/AIdoctor/pull/332) @ `df75db36a8d0f796321f1de86b9b074bf7a73d2f`.
> **Status: R1_PERSONAL_RUNNER_CANDIDATE_IDENTIFIED / LIMITED_LOCAL_SELF_REPORTED_OBSERVATIONS. No R2 collection grant, Stage A1 implementation or canary authorization. No physical isolation PASS.**
> **Scope: evidence-only Draft PR / single Markdown file; no Docker daemon configuration, scripts, test harness, Java, POM, GitHub workflow, secrets, actual host verification or merge.**

## 1. Candidate declaration

Candidate: an owner-controlled Windows development computer with Docker Desktop, Linux containers on the WSL2 backend, Docker context `desktop-linux`. User indicates this is their own computer. Infrastructure owner role is proposed to be the developer **for the personal-development context only**, subject to explicit signed local governance selection; no separate Security/independent physical attestations have been obtained.

This is **not** an independently identified/reviewed physical asset: no trusted machine/VM ID, signed asset registration or host metadata has been captured. Do not promote Docker context or local image ID into a cryptographically authenticated runner identity. Local Windows host is also used for other application images; tests must never inspect, mount, connect to or change those workloads or their persistent volumes.

## 2. Self-reported observations and commands

| Observation | User transcript | Appropriate inference |
|---|---|---|
| WSL status | default distro `docker-desktop`, default version 2; `docker-desktop` Running 2; `Ubuntu-22.04` Stopped 2 | WSL2 available; Ubuntu 22.04 is not selected runner |
| Docker Desktop and Engine | Desktop `4.41.2 (191736)`, client/engine `28.1.1`, server `linux/amd64` | Local Linux-container backend responds |
| Engine security metadata | `["name=seccomp,profile=unconfined","name=cgroupns"]` | Daemon-level profile reported unconfined; **does not override** observed explicitly configured per-container seccomp |
| Kernel | `6.18.33.2-microsoft-standard-WSL2` | WSL2 Linux kernel release as reported by daemon |
| Cgroup | `2` | Engine cgroup v2 reported |
| Operating system | `Docker Desktop` | Engine OS label |
| Local image tag | `python:3.12-slim` | Local candidate nonclinical utility image; mutable tag |
| Local image ID | `sha256:25c5b8011a3425a140bf5fa73be0feabd3c0d5b323eecb19dc02437a368ae075` | Locally observed image content identity, **not registry signature/immutable pull digest or independently verified provenance** |
| Docker context | `desktop-linux` | Active Docker CLI context, **not secure host asset identity** |
| Default OCI runtime | `runc` | Daemon reports default runtime runc |
| Constrained sample process | `CapEff=0000000000000000`, `CapBnd=0000000000000000`, `NoNewPrivs=1`, `Seccomp=2`, `Seccomp_filters=2` | User-reported facts from **sample Python process only**, not proof of every future process or policy denial |
| Second constrained sample | `UID=65534`, `GID=65534`, `Interfaces=[(1, 'lo')]`, `RootReadonly=True` | User-reported user/namespace/mount view of **second ephemeral container**, not necessarily same instance as first sample |

Two **different one-shot** `docker run --rm` invocations were used. Both set `--pull never --network none --cap-drop ALL --security-opt seccomp=builtin --security-opt no-new-privileges=true --read-only --user 65534:65534 --pids-limit 64 --memory 128m --cpus 1` and ran a short `python:3.12-slim` introspection command; the first read selected `/proc/self/status` keys, the second read UID/GID, local interfaces and root `statvfs`. Neither container was kept for post-run inspection. No host command was executed by the GitHub assistant; outputs are user supplied. Absence of an external interface is **not** observed proof that loopback, local sockets, vsock, inherited FDs, metadata or outbound access are independently denied by a preventive policy.

## 3. Evidence classification and safety limitations

```text
EVIDENCE_SOURCE = USER_SUPPLIED_LOCAL_CMD_TRANSCRIPT
EVIDENCE_ATTESTATION = UNVERIFIED_SELF_REPORTED
PERSONAL_DOCKER_CANDIDATE = IDENTIFIED_FOR_PLANNING
RUNNER_ASSET_ID = NOT_INDEPENDENTLY_ATTESTED
IMAGE_ID = OBSERVED_LOCAL_SHA256
DOCKER_CONTEXT = desktop-linux
OCI_DEFAULT_RUNTIME = runc
SECCOMP_FILTER_MODE = OBSERVED_ON_ONE_EPHEMERAL_PROCESS
CAPABILITIES_ZERO = OBSERVED_ON_ONE_EPHEMERAL_PROCESS
NONROOT_AND_READONLY_ROOT_AND_LOOPBACK_ONLY = OBSERVED_ON_ANOTHER_EPHEMERAL_PROCESS
POLICY_DENIAL_ATTRIBUTION = NOT_PROVEN
A1_NEGATIVE_CANARIES_N01_N10 = NOT_AUTHORIZED / NOT_EXECUTED
```

A `Seccomp: 2` report means filter mode is active for the reporting process; **not** that the exact rule set is correct, signature verified or denies the relevant operation. `Seccomp_filters: 2` is not a rule inventory. `--network none` yields an isolated network namespace, not necessarily denied loopback or the external preventive-rule audit event required for `DENIED_BY_POLICY`. `--read-only` does not prove all mounts, inherited file descriptors, host OS or external volumes are inaccessible. The two samples were not one durable runner; source/collector and future sandbox identity must be bound anew for physical evidence.

Treat this as **positive feasibility evidence**, not closure of `SA-BF-01/02` or `BF-U07-A1-R2-01/02`. This change records new tangible environment facts without pretending the previously signed-infrastructure approval model has been fulfilled.

## 4. Personal-runner governance adaptation (design proposal, not self-granted authority)

For a developer-owned nonproduction computer, propose an explicit `PERSONAL_OWNER_LOCAL_NONPRODUCTION` governance profile as a **controlled future amendment**:
1. Named self-owned asset alias plus user-signed local declaration of machine and data scope, no secrets or real PHI; risk acceptance and expiration.
2. Separate accountability for the owner decision vs independent evidence review, with recorded reviewer role and minimum auditable provenance. A single developer can manage the local machine, but cannot self-certify an independent implementation/security review.
3. Read-only command collection grant and exact runner-bound argv/output retention policy distinct from any implementation, setup or canary execution grant.
4. Separate high-risk clinical/production/multi-user system paths: **no automatic relaxation** from a personal developer environment. No host-wide network policy modifications, Docker socket mounts, privileged containers or clinical services.

Changing the existing `Infrastructure + Security + Foundation/U01` required authority for this personal profile requires a **separately reviewed and explicitly authorized governance amendment**. This evidence note does not alter any frozen contract and is not itself such an amendment.

## 5. Precise next gates

| Gate | Current status | Evidence still required |
|---|---|---|
| Candidate image/context/runtime self-reported baseline | `RECORDED_FROM_USER` | Independent readback and exact temporal/asset binding |
| Basic constrained container feasibility | `USER_OBSERVED` | Validated process-to-policy provenance on an actual nominated runner |
| Local Owner authority profile | `PROPOSAL_ONLY` | Explicit approval of personal-use authority alternative |
| `BF-U07-A1-R2-01` real runner + owner | `PARTIAL_CANDIDATE_IDENTIFIED / OPEN` | Stable runner asset identity and accepted owner attestation |
| `BF-U07-A1-R2-02` collection authority | `OPEN` | Scoped approved R2 read-only command/evidence grant |
| `SA-BF-01/02` | `OPEN` | Preventive isolation and independently attributed synthetic canary evidence |
| A1 exact six-file implementation | `NOT_AUTHORIZED` | Fresh independent readiness and separate implementation authorization |
| R3/R4 setup/canary | `NOT_AUTHORIZED` | Separate specific execution gates |
| Stage B and clinical production | `NOT_AUTHORIZED` | Separate decisions |

**Next recommended work:** an independent **Personal Docker Runner Applicability + Local Owner Authority Adaptation Design Review** explicitly resolves whether to amend the enterprise-oriented runner approval contract, and defines the minimum additional trusted local evidence required for a real R2 grant. Do **not** run negative canaries or claim `RUNNER_ATTESTED` until that separate decision.

## 6. Formal snapshot

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_DOCKER_WSL2_RUNNER_BASELINE
= SELF_REPORTED_LOCAL_FACTS_RECORDED / INDEPENDENT_ATTESTATION_PENDING

SOURCE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
PR332_R2_READINESS_HEAD = df75db36a8d0f796321f1de86b9b074bf7a73d2f
RUNNER_CANDIDATE = WINDOWS_DOCKER_DESKTOP_WSL2
IMAGE_ID = sha256:25c5b8011a3425a140bf5fa73be0feabd3c0d5b323eecb19dc02437a368ae075
CONTEXT = desktop-linux
OCI_DEFAULT_RUNTIME = runc
ENGINE_SECCOMP_REPORT = unconfined
SAMPLE_PROCESS_SECCOMP_FILTER = OBSERVED
SAMPLE_PROCESS_CAPABILITIES = ZERO_OBSERVED
SAMPLE_PROCESS_NO_NEW_PRIVS = OBSERVED
SAMPLE_PROCESS_UID_GID = 65534:65534_OBSERVED
SAMPLE_PROCESS_INTERFACES = LOOPBACK_ONLY_OBSERVED
SAMPLE_PROCESS_ROOT_READONLY = OBSERVED
RUNNER_OWNER_SIGNED_ATTESTATION = NOT_GRANTED
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
STAGE_A1_ISOLATION_PROVEN = NO
A1_CANARY_RUN = NOT_EXECUTED
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```
