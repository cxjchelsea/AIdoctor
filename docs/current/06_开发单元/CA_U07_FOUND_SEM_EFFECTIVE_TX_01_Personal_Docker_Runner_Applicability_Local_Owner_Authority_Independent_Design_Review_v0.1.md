# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Docker Runner Applicability + Local Owner Authority Adaptation Independent Design Review v0.1

> Date: 2026-10-09
> Author evidence: [PR #333](https://github.com/cxjchelsea/AIdoctor/pull/333) exact HEAD `c51f3f2f3d60613864be03ede4b4fd2b3d85aa30`, document blob `550ce59e3f5b4bc07eb4ec8d74f9150c8c35c5ae`.
> Current source baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654`, confirmed by GitHub readback.
> Related: [PR #332](https://github.com/cxjchelsea/AIdoctor/pull/332) @ `df75db36a8d0f796321f1de86b9b074bf7a73d2f`; [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) @ `3f6725f387105798d2fa4ba6cefb1b43a69a0360`; [PR #330](https://github.com/cxjchelsea/AIdoctor/pull/330) @ `e1ebaa527f95b41ec1611b2bc300872d45762882`.
> **Independent decision: PASS / CONDITIONAL_DESIGN_ACCEPTANCE for personal Docker as a LIMITED, NONPRODUCTION CANDIDATE; CONTROLLED_OWNER_AUTHORITY_AMENDMENT_REQUIRED. NOT a security approval, effective R2 grant, Stage A1 isolation PASS or implementation authorization.**

## 1. Source and evidence truth boundary

Independently fetched PR #333 exact HEAD/blob. It contains only one ADD-only evidence Markdown file with user-supplied CMD transcripts, not authenticated host artefacts or a retained executing container. Two ephemeral `python:3.12-slim` runs share proposed command-line flags, but are separate processes/containers and **do not** establish one immutable runner identity or independent rule-coverage evidence. PR #333 faithfully labels observations as self-reported.

| Fact | Admissible claim | Claim that remains forbidden |
|---|---|---|
| Windows Docker Desktop 4.41.2, Engine 28.1.1, Linux WSL2 6.18.33.2, cgroup v2, `desktop-linux`, `runc` | Realistic local Linux-container **candidate** exists | A separate hardened Linux VM or approved host identity is proven |
| Local `python:3.12-slim` image ID `sha256:25c5b8011a3425a140bf5fa73be0feabd3c0d5b323eecb19dc02437a368ae075` | Content identity **reported** by Docker image inspect | Remote signature, provenance, unchanged tag or no malicious content is established |
| Engine `seccomp=unconfined`; first explicitly flagged sample `Seccomp=2`, 2 filters, no_new_privs=1, CapEff/CapBnd zero | At least the sample Python process reported an active filter and bounded privileges | Exact rule corpus or prevention of all Stage A1 behaviors is proven |
| Second sample UID/GID 65534, interfaces `lo` only, readonly root | That particular process sees these constraints | Loopback or AF_UNIX denied; no host exposure/inherited FD; no external-network denial rule attributable |
| Other business and DB images present on Docker Desktop | Host must be treated as **shared-risk developer workstation** | These workloads/volumes/mounts are safe to involve in tests |

**Technical limitation:** WSL2 Docker Desktop uses a Linux virtualization backend, but a Docker container is not an independently administered whole-VM security boundary. Container runtime flags are configuration; they are not independent proof of denied syscalls, host-socket/FD prevention, LSM policy enforcement or auditable negative-canary origin. Nonproduction safety requires synthetic assets only and no exposure of the existing backend/data volumes.

## 2. Precise applicability decision for Stage A1

| Requirement surface | Local Docker applicability | Preconditions / outcome |
|---|---|---|
| Nonclinical R0 static design and Docker source/version inventory | **YES / SOURCE_AND_USER_OBSERVATION_ONLY** | Can record exact user output; not independently signed Runner facts |
| Basic R2-style local introspection: UID/caps/seccomp/namespace/image | **CANDIDATE, NOT_AUTHORIZED** | Approval after owner-profile amendment; exact command, nonprivileged collector, output redaction and stable runner identity |
| Network namespace and absence of non-loopback interface | **CANDIDATE / OBSERVED CONFIGURATION** | Does not prove loopback denial, IPv6/DNS/metadata denial or their policy attribution |
| Readonly root, nonroot, no-new-privs, caps | **CANDIDATE / TWO SAMPLE OBSERVATIONS** | Need reproducible equivalent process context and independent verify; no raw host mounts |
| Synthetic loopback/Unix socket and child/FD tests | **DESIGN_FEASIBLE / UNAUTHORIZED** | Separate isolated SETUP, R4 authorizations, negative controls, event provenance, no host sharing |
| Enforced denial of external IPv4/IPv6, DNS/metadata, host sockets and vsock | **NOT_PROVEN / MAY REQUIRE MORE THAN DOCKER** | Missing independently observed **preventive rule** and audit trail; fail closed if just unreachable or no listener |
| Full A1-N01..10 Oracle acceptance | **NOT_AUTHORIZED / INCOMPLETE** | Preserve exact original case IDs/semantics from frozen Oracle; do not reinterpret from a generic Linux capability checklist |
| Stage A2 offline Spring / Stage B and clinical integration | **OUT_OF_SCOPE** | No inherited JVM, CI, DB, real patient or production authority |

**Important:** `--network none` is expected to leave loopback, and `Seccomp=2` says only that some filter exists. A candidate that lacks audit attribution must report `INCOMPLETE_EVIDENCE`, not `DENIED_BY_POLICY` or `ISOLATION_POLICY_PASS`. The no-NIC condition may support a narrower claim if independently verified, but cannot satisfy evidence specifically demanding a preventive policy event.

## 3. Personal-owner authority adaptation: independent review of the proposal

**Design verdict: ACCEPTABLE AS A PROPOSED CONTROLLED AMENDMENT, NOT YET EFFECTIVE.** The owner of a personal dev workstation can legitimately nominate and manage their own machine for nonclinical synthetic tests; this removes the need to pretend a corporate infrastructure manager must provision their personal PC. However, the currently frozen R2 authorization profile still expects named Infrastructure, Security and Foundation/U01 approvals. **Do not silently reinterpret the developer as holding all three signed roles**. The profile must be amended through an explicitly approved governance decision before any alternative local R2 authorization is valid.

Proposed distinct rights for `PERSONAL_OWNER_LOCAL_NONPRODUCTION`:

| Authority | Personal adaptation | Cannot do |
|---|---|---|
| Environment/asset owner | Developer records local machine alias, `desktop-linux` engine and image, signs scope/risks and expiry | Attest to independent prevention proof or access other existing workloads |
| Nonclinical R2 inspection authorizer | Developer may approve **their own** narrowly fixed local read-only inventory **only after** an accepted amendment and reviewed command/redaction envelope | Waive excluded command/host privilege/PHI constraints or retroactively grant past runs |
| Independent technical reviewer | Separate evidence-only review of exact-head scripts, logs, actual source provenance and claimed effects | Invent signature keys or turn user statement into independent host verification |
| Clinical/production/security organizational approver | Remains whichever external authority is legally/organizationally responsible if later required | Automatically inherit local-dev consent for clinical, PHI, production, regulatory use |

**Separation of role vs person:** For personal synthetic experimentation the same person may be platform owner and propose local consent; *independent* acceptance must still not be self-signed by a test producer. If independent human review is required by a frozen gate, an AI-authored analysis is not equivalent to a human organizational Security signature. Governance amendment must specify which approvals can be substituted, which cannot, and its precise stage-only applicability.

## 4. Security restrictions and residual-risk register

- Personal host **already has other healthcare/backend/postgres/redis images**. Container image presence alone does not mean their data are mounted or running; nevertheless never issue commands that change or enumerate live volumes/other containers as part of A1. Do not add Docker socket, bind mounts, Windows paths, user credentials or existing networks.
- For a future approved fixture container: `--pull never`, exact `--image` ID pin, `--network none`, `--cap-drop ALL`, `--security-opt seccomp=builtin`, `--security-opt no-new-privileges=true`, nonroot UID, readonly root, strict scratch only, resource limits, no privileged mode. These are **proposed controls**, not a tested complete sandbox policy.
- Bind actual `docker context`, Docker engine/runtime/kernel and image SHA plus the short-lived container identity/creation time to each evidence artifact. Docker Desktop update, kernel/image/host setting drift invalidates old R2 records.
- Negative tests for sockets, process inheritance and policy denial require separate trusted synthetic positive fixture, independent frozen expected Oracle, phase-bound event logging, negative control and teardown. Never probe real metadata IP, external/corporate services or host bridge addresses.
- Do not upgrade to full `RUNNER_ATTESTED` merely because developer reports command output; obtain separately reviewed provenance/capture and the actual preventive-rule observability.
- If Docker cannot provide no-host-FD, AF_UNIX/vsock, policy-origin audit, or controlled counter evidence, **escalate the specific missing test boundary to a dedicated Linux VM** rather than weakening that test's acceptance criteria. This is a conditional future decision, not an instruction to install a VM now.

## 5. Targeted findings and gates

| Item | Review state | Owner action |
|---|---|---|
| `PDR-A1-G01` exact PR #333 HEAD/blob and main pinned | PASS_SOURCE |
| `PDR-A1-G02` local Docker **candidate** applicability | PASS_CONDITIONAL_DESIGN |
| `PDR-A1-G03` accurate untrusted/user-provided evidence classification | PASS_DESIGN |
| `PDR-A1-G04` distinct owner/grant/reviewer authority design | PASS_CONDITIONAL / GOVERNANCE_AMENDMENT_REQUIRED |
| `PDR-A1-G05` exact local R2 executable command, collector and redaction security | NOT_READY |
| `PDR-A1-G06` actual preventive OS/VM/LSM denial + independent evidence | NOT_PROVEN |
| `PDR-A1-G07` Stage A1 six-file implementation, synthetic SETUP/CANARY | NOT_AUTHORIZED |
| `PDR-A1-G08` Spring/PHI/DB/CI/production exclusion | PASS_SCOPE_ONLY |
| `RF-U07-A1-PDR-01` | REQUIRED | New **controlled governance amendment** for personal-owner profile, applicability/exclusion matrix, effective validity, approval and expiry |
| `RF-U07-A1-PDR-02` | REQUIRED | Concrete local asset alias/owner declaration plus exact **non-invasive R2 only** command/profile with independent verification/capture requirements |
| `RF-U07-A1-PDR-03` | REQUIRED | Preserve Stage A1 negative-denial Oracle and independent effect attribution; document a Docker capability insufficiency -> Linux VM escalation rule |

Unresolved original `BF-U07-A1-R2-01` (real asset/owner) becomes `PARTIAL_PERSONAL_CANDIDATE / OPEN`, not CLOSED; `BF-U07-A1-R2-02` (actual grant) remains OPEN. `SA-BF-01/02` remain OPEN. PR #333 and this review do not amend frozen authority documents or merge into main.

## 6. Recommended exact next implementation path (non-executable)

1. **Controlled Personal Owner Governance Amendment Design**: specify alternative approval roles only for developer-owned *synthetic, nonclinical* Stage A1; preserve independent implementation/evidence evaluation. Review the exact frozen authority locations and prepare minimal diff inventory; **not** a blanket clinical security exception.
2. **Independent amendment design review** on frozen exact HEAD. Once explicitly accepted and owner-approved, record a personal runner candidate identity, reproducible **read-only** inventory command allowlist, bounded data capture/retention, drift invalidation and explicit local R2 grant.
3. **Authorized R2 readback** for named Docker engine and isolated test container. Only after policy/host-bound evidence, conduct exact six-file Stage A1 implementation readiness review and a **separate** authorization.
4. Later, frozen synthetic fixture setup and measured A1 non-Spring negative tests under separate grants and an independent Oracle. Require a dedicated VM only where Docker is demonstrably insufficient.

This is a **plan**, not approval or actual execution of any step.

## 7. Formal verdict

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_DOCKER_RUNNER_APPLICABILITY_LOCAL_OWNER_AUTHORITY_ADAPTATION_INDEPENDENT_DESIGN_REVIEW
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

AUTHOR_PR333_HEAD = c51f3f2f3d60613864be03ede4b4fd2b3d85aa30
AUTHOR_PR333_BLOB = 550ce59e3f5b4bc07eb4ec8d74f9150c8c35c5ae
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654

PERSONAL_DOCKER_RUNNER = ELIGIBLE_CANDIDATE / NOT_ATTESTED
SELF_REPORTED_LOCAL_EVIDENCE = ACCEPTED_AS_LIMITED_FEASIBILITY_INPUT
PERSONAL_OWNER_AUTHORITY_PROFILE = PROPOSED / NOT_FROZEN
GOVERNANCE_AMENDMENT = REQUIRED / NOT_AUTHORIZED
RF-U07-A1-PDR-01..03 = REQUIRED
BF-U07-A1-R2-01 = PARTIAL_PERSONAL_CANDIDATE / OPEN
BF-U07-A1-R2-02 = OPEN
SA-BF-01/02 = OPEN
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
R2_HOST_COMMANDS_EXECUTED_BY_REVIEW = NO
A1_SIX_FILE_IMPLEMENTATION = NOT_AUTHORIZED
SETUP_AND_CANARY_EXECUTION = NOT_AUTHORIZED
STAGE_B_SPRING = NOT_AUTHORIZED
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```

Independently authored analytical review artifact; not a human security or organizational authorization. No source, CI, clinical code or host changes and no PR merge.
