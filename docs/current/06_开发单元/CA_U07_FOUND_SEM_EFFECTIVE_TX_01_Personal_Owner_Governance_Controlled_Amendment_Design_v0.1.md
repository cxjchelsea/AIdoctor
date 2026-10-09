# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Owner Governance Controlled Amendment Design v0.1

> Date: 2026-10-09. Repository `cxjchelsea/AIdoctor`.
> Base: `main@86e8843197091c8c8172b7e4213537a31bdf0654`.
> Author local baseline: [PR #333](https://github.com/cxjchelsea/AIdoctor/pull/333) exact HEAD `c51f3f2f3d60613864be03ede4b4fd2b3d85aa30`, evidence blob `550ce59e3f5b4bc07eb4ec8d74f9150c8c35c5ae`.
> Independent applicability review: [PR #334](https://github.com/cxjchelsea/AIdoctor/pull/334) exact HEAD `face63327e172405cc84b5ae8cde57df9b6da308`, review blob `4ecf1287ca7fb730b0a9862da5c3928c5f892f10`, `PASS / CONDITIONAL_DESIGN_ACCEPTANCE`.
> Current R2 command-profile design: [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) @ `3f6725f387105798d2fa4ba6cefb1b43a69a0360` and [PR #330](https://github.com/cxjchelsea/AIdoctor/pull/330) @ `e1ebaa527f95b41ec1611b2bc300872d45762882`; scope design [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) @ `6884c26e93855b97a5f1293fe81c3fa5f524a739`.
> **Status: CONTROLLED_AMENDMENT_DESIGN_CANDIDATE / INDEPENDENT_DESIGN_REVIEW_PENDING.** This Markdown is the only present change; no frozen authority record has been amended, no host access/SETUP/CANARY, no owner signature, implementation or merge.

## 1. Decision to be designed and strict scope

The question is whether **one developer's personal Windows + Docker Desktop WSL2 environment**, running **synthetic-only nonclinical Stage A1 prebootstrap tests**, may replace a nonexistent *corporate infrastructure operator* as its local environment owner **without** treating that substitution as a Security audit pass.

The answer proposed is **YES for personal environment nomination and an explicitly scoped owner-issued R2 read-only collection permit, subject to a separately authorized controlled governance amendment**. The developer may assume the `LOCAL_ENVIRONMENT_OWNER` and `LOCAL_COLLECTION_CONSENT_SIGNER` roles for their own machine. This **does not** automatically satisfy the existing organizational `Security` / `Foundation-U01` signature contracts, which must be explicitly and narrowly adapted for the chosen personal-development profile. It **does not** let the evidence producer sign their own independent evidence acceptance.

The profile is **not applicable** to production, clinical execution, PHI/real patients, corporate hardware/accounts, externally facing testing, Spring/JVM Stage B, U03/U06 clinical runtime activation, real DB/network integrations, or a different asset. If ownership/asset classification is ambiguous, select the existing enterprise profile or return `NOT_READY`.

## 2. Local candidate facts; no invented attestation

```yaml
profile_candidate: PERSONAL_OWNER_LOCAL_NONPRODUCTION
machine_asset_alias: UNASSIGNED_USER_DECLARATION_REQUIRED
owner_identity_signature_ref: NOT_PROVIDED
environment_class: PERSONAL_NONPRODUCTION_SYNTHETIC_ONLY
docker_context: desktop-linux
docker_desktop_reported: 4.41.2
docker_engine_reported: 28.1.1
linux_kernel_reported: 6.18.33.2-microsoft-standard-WSL2
runtime_reported: runc
cgroup_version_reported: 2
image_id: sha256:25c5b8011a3425a140bf5fa73be0feabd3c0d5b323eecb19dc02437a368ae075
self_reported_test_process:
  effective_and_bounding_capabilities: "0000000000000000"
  no_new_privs: 1
  seccomp_mode: 2
  seccomp_filters: 2
self_reported_separate_test_process:
  uid: 65534
  gid: 65534
  interfaces: ["lo"]
  root_read_only: true
asset_attestation: NOT_INDEPENDENTLY_VERIFIED
r2_collection_grant: NOT_GRANTED
```

These are user-supplied outputs from **two different ephemeral containers**. They demonstrate initial feasibility only. They are not one attested Runner process, do not prove policy-origin denial, and cannot close `SA-BF-01/02`.

## 3. Roles and authority matrix (normative proposal)

| Role | Allowed personal-profile assignment | May approve | May not assert |
|---|---|---|---|
| `LOCAL_ENVIRONMENT_OWNER` | Developer/owner of this personal workstation | Register personal asset alias, synthetic-only intent, resource scope, expiry and stop conditions | Independence of the security verification |
| `LOCAL_COLLECTION_CONSENT_SIGNER` | Same individual **for this personal asset only**, with explicit timestamped declaration | R2 **read-only, exact-command** permission *after governance profile effective and command readback accepted* | SETUP/CANARY, arbitrary host operations, clinical access, retroactive authority |
| `R2_COMMAND_PROFILE_REVIEWER` | Reviewer different from command-profile author **as review function** | Evaluate exact binary/argv/output/no-raw-log contract, reject unsafe commands | Sign for a real organizational Security principal; infer tools were executed |
| `EVIDENCE_PRODUCER` | User/approved local collector | Record bounded signed/logged R2 facts | Issue independent PASS or modify frozen Oracle |
| `INDEPENDENT_EVIDENCE_ACCEPTOR` | Independent authorized reviewer (human when required by controlling frozen policy) | Inspect provenance and grant compliance, accept/reject observations | Convert user-observed flags into prevention proof without origin evidence |
| `CLINICAL_SECURITY_AUTHORITY` | Unchanged external authority | Only the separate clinical/production authorization process | Become the personal developer by assumption |
| `FOUNDATION_U01_OWNER` | Existing role preserved for boundary-sensitive frozen contract decisions | Approve actual Core/clinical contractual changes if required | Treat local R2 consent as Foundation deployment authority |

**Separation of decisions**: physical ownership can be the same person as the local collection-consent signer. It is never the same *evidence* as independent denial attribution. If an existing frozen policy requires a distinct person/security signature for a specific R2 operation, that requirement remains in force until **the exact policy paragraph** is explicitly amended and separately approved. AI-generated design review does not purport to supply human institutional credentials.

## 4. Formal local-only profile and decision semantics

```yaml
schema: U07StageA1PersonalOwnerAuthorityProfileV1
profile_id: PERSONAL_OWNER_LOCAL_NONPRODUCTION
status: DESIGN_CANDIDATE_NOT_EFFECTIVE
source_main_sha: 86e8843197091c8c8172b7e4213537a31bdf0654
scope:
  stage: STAGE_A1_PREBOOTSTRAP
  purpose: LOCAL_NONCLINICAL_SYNTHETIC_ONLY
  allowed_r2: READ_ONLY_FIXED_COMMAND_INVENTORY_AFTER_EXPLICIT_GRANT
  excludes:
    - REAL_PATIENT_OR_PHI
    - CORPORATE_OR_PRODUCTION_RUNNER
    - JAVA_OR_SPRING_STAGE_B
    - DATABASE_OR_REAL_NETWORK_OR_HOST_BRIDGE
    - CONTAINER_PRIVILEGED_OR_DOCKER_SOCKET_OR_HOST_BIND_MOUNTS
    - SETUP_OR_CANARY_WITHOUT_SEPARATE_GRANT
asset:
  owner_declared_asset_alias: UNASSIGNED
  owner_local_signature_or_approval_ref: NOT_GRANTED
  docker_context: desktop-linux
  docker_engine_identity: USER_REPORTED_NOT_REATTESTED
  container_and_image_binding: PENDING
approval:
  governance_controlled_amendment_decision_ref: NOT_AUTHORIZED
  r2_command_profile_independent_review_ref: DESIGN_ONLY_PR330
  r2_exact_argv_digest: UNKNOWN
  collection_principal: UNKNOWN
  raw_output_and_logging_control_attestation: NOT_VERIFIED
  output_schema_digest_and_retention_acl: UNKNOWN
  local_owner_r2_grant_ref: NOT_GRANTED
  independent_evidence_acceptance_ref: NONE
validity:
  valid_from: UNSET
  expires_at: UNSET
  revocation_on_docker_kernel_image_policy_owner_drift: REQUIRED
decision: NOT_READY
```

Authority transitions are explicit and fail closed:

```text
PROFILE_PROPOSED
  -> INDEPENDENT_AMENDMENT_DESIGN_REVIEW
  -> FROZEN_CONTRACT_IMPACT_AND_EXACT_DIFF_REVIEW
  -> EXPLICIT_GOVERNANCE_AMENDMENT_AUTHORIZATION
  -> AMENDMENT_IMPLEMENTATION_AND_INDEPENDENT_VERIFICATION
  -> EFFECTIVE_PERSONAL_PROFILE (only exact approved scope)
  -> NAMED_LOCAL_ASSET_AND_OWNER_DECLARATION
  -> RUNNER_BOUND_R2_COMMAND_PROFILE_AND_DISCLOSURE_REVIEW
  -> SEPARATE LOCAL_OWNER_R2_COLLECTION_GRANT
  -> R2_READ_ONLY_COLLECTION (separately executed; not by this document)
  -> INDEPENDENT_EVIDENCE_READBACK / ACCEPTANCE
```

No arrow implies automatic promotion. Non-Spring setup, negative-canary and Stage B require separate authorizations and frozen independent Oracles.

## 5. R2 exact safe-collection adaptation (RF-U07-A1-PDR-02)

The old `R2-01..10` enumeration remains normative as a **design candidate**, with PR #328/#330 constraints. In the personal environment:
- Prefer a **non-network**, nonprivileged, user-controlled local command collector with fixed per-command argv/binary hash, not an unrestricted shell string.
- R2-01..05 may only read a carefully bounded set of UID/GID, kernel, seccomp, namespace and image facts after exact grant; do not export raw `/proc`, mount paths, environment, tokens, live container metadata or unrestricted logs. The reading process's own data are not sufficient to attest target canary process inheritance.
- R2-06/07/08/09/10 remain **offline owner evidence** when feasible. For a personal owner without organizational PKI, an explicit local provenance alternative (dated signed declaration plus independently verified hash and machine/engine readback) must be accepted through the controlled amendment **before** it can replace enterprise signed asset manifests. The mere existence of a SHA-256 or user signature cannot prove preventive policy; `NOT_PROVEN` remains mandatory when source integrity/independence is insufficient.
- Each output binds exact runner/container/image ID, Docker Desktop/engine/kernel, owner declaration, grant reference, command profile and attempt time. Stop and re-attest on version or policy drift.
- For the shared Windows host containing other business and DB images, **no host Docker socket**, container listing/volume enumeration for unrelated workloads, Windows bind mounts, privileges, host network or external probes as part of R2.

**No automatic R2 grant is issued** here. A normal local `docker info` or `docker image inspect` transcript already provided is classified as past exploratory user evidence; this proposal does **not** retroactively authorize or reclassify it as a formally granted R2 run.

## 6. Stage A1 negative-proof and VM escalation contract (RF-U07-A1-PDR-03)

- Keep all original `A1-N01..A1-N10` case IDs, setup and Oracle semantics unchanged. No renaming cases or replacing a policy-origin requirement with an assertion that `--network none` removed external interfaces.
- Passing denial requires independently attributable actual enforcement origin with a valid synthetic target and owner-approved negative control. `UNREACHABLE_TARGET`, `NO_DNS_RESOLVER`, `REFUSED_NO_LISTENER`, `INVALID_FIXTURE`, `OBSERVABILITY_GAP`, and absent policy event **cannot** become `DENIED_BY_POLICY`.
- Docker Desktop's WSL2 virtualized daemon is acceptable to *attempt* approved nonclinical sandbox controls. If loopback, AF_UNIX, host/guest FD or metadata/vsock boundaries cannot be independently shown for an applicable case, mark it `INCOMPLETE_EVIDENCE`.
- Escalate **only the unmet and safety-relevant acceptance criterion** to a separately controlled dedicated Linux VM. Do not install/use that VM or relax test policy automatically. If Docker cannot meet a mandatory case, Stage A1 remains blocked pending alternative evidence.

## 7. Proposed controlled amendment exact-diff inventory

**This PR adds only the present Markdown design document.** The table below is a **future controlled amendment impact inventory**, not an executed edit nor an accepted six-file code allowlist. Target sources known by exact prior PR identities are given; frozen/main effective target existence and blob SHAs must be verified at the later amendment readiness gate. Do not invent unknown base paths or amend all historical drafts.

| Target candidate / origin | Proposed operation after owner authorization | Exact intended change | Present status |
|---|---|---|---|
| `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Runner_Nomination_R2_Attestation_Authorization_Readiness_v0.1.md` — PR #328 | `UPDATE_IN_SCOPED_AMENDMENT_ONLY` | Add local personal authority profile as **alternative** to enterprise R2 signer set, reference its separate grant; preserve enterprise policy as default | Source exists on PR #328, **not part of main**; do not update until target scope/merge status rechecked |
| `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Controlled_ExactDiff_Amendment_Runner_Authority_Evidence_Plan_v0.1.md` — PR #326 | `UPDATE_IN_SCOPED_AMENDMENT_ONLY` | Introduce personal-runner alternative validity and explicit no-Spring/no-PHI limits for R1/R2; do not change six tool-file names or execution grants | Candidate PR only |
| `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Personal_Docker_WSL2_Runner_Baseline_Evidence_v0.1.md` — PR #333 | `PRESERVE_EVIDENCE_UNCHANGED` | Link evidence as immutable user-observed source, not authoritative grant | No content rewrite |
| `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Personal_Docker_Runner_Applicability_Local_Owner_Authority_Independent_Design_Review_v0.1.md` — PR #334 | `PRESERVE_INDEPENDENT_REVIEW_UNCHANGED` | Read-only prerequisite and findings | No content rewrite |
| Effective **frozen Runner/Stage A1 authority contract**, if elsewhere in the governed repository | `BLOCKED_UNTIL_EXACT_PATH_BLOB_IDENTIFIED` | A single narrow `PERSONAL_OWNER_LOCAL_NONPRODUCTION` clause with role map, grant type, scope, expiry, revocation and unchanged enterprise fallback; no broad role substitution | **FROZEN_TARGET_DISCOVERY_REQUIRED**; never fabricate path or bypass it |
| Controlled amendment manifest / signed decision record | `CREATE_ONLY_AFTER_EXPLICIT_AUTHORIZATION` | Exact authoritative artifact paths and version-bound approval refs; fail closed if not approved | `NOT_AUTHORIZED` |

**Forbidden exact diff**: `diagnosis-service/src/main/java/**`, Spring/JVM/POM, production env, patients/PHI, migrations, database, networking, permissions, CI workflows, Stage A2/Stage B files, original Stage A1 synthetic test driver and Oracle, or any other frozen U07 scope without separate permission. No squash/rebase/merge of unrelated PRs.

### Amendment authority invariants

```text
enterprise_profile_required_signatures remain unchanged unless exact-field scoped amendment authorized
personal_profile_active only when environment_class=PERSONAL_NONPRODUCTION_SYNTHETIC_ONLY
personal_local_consent never satisfies INDEPENDENT_EVIDENCE_ACCEPTANCE
signed_profile + grant bind runner/owner/commands/asset/image/engine/expiry
unknown/expired/mismatch/revocation => fail closed
clinical/production authority is never inherited
R2 grant != Stage A1 implementation grant != SETUP grant != CANARY grant != Stage B grant
```

## 8. Review gates and open findings

| Gate | This design's state |
|---|---|
| `PGA-G01` original PR #333/#334 source heads/blobs and main baseline | SOURCE_VERIFIED |
| `PGA-G02` personal role matrix and scope limitation | DESIGN_CANDIDATE |
| `PGA-G03` separation of owner permission vs independent evidence | DESIGN_CANDIDATE |
| `PGA-G04` existing enterprise profile and clinical exclusions preserved | DESIGN_CANDIDATE |
| `PGA-G05` original frozen contract exact path/blob and owner authority | BLOCKED / EXACT_FROZEN_TARGET_NOT_VERIFIED |
| `PGA-G06` independent governance amendment design review | PENDING |
| `PGA-G07` actual owner consent for local R2 and exact read-only commands | NOT_GRANTED |
| `PGA-G08` actual Docker Runner preventive isolation denial evidence | NOT_PROVEN |
| `PGA-G09` executable code or Stage A1 fixture/canary permission | NOT_AUTHORIZED |

| Finding | Disposition |
|---|---|
| `RF-U07-A1-PDR-01` | Personal owner governance amendment design **PROPOSED**, awaiting review/authority |
| `RF-U07-A1-PDR-02` | Owner alias and local read-only evidence contract designed; executable commands and owner grant still absent |
| `RF-U07-A1-PDR-03` | Negative Oracle invariants and Docker-to-VM escalation rule designed |
| `RF-U07-A1-PGA-01` | Identify actual frozen authoritative policy paths/blob SHAs and reject ambiguous transitive overrides |
| `RF-U07-A1-PGA-02` | Independently assess which named signer is genuinely substitutable in personal nonclinical scope; do not treat AI review as an organization signing authority |
| `BF-U07-A1-R2-01/02` | OPEN — candidate only, no effective owner/collection grant |
| `SA-BF-01/02` | OPEN — no canary/prevention evidence |

## 9. Next decision

**Next required review:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Owner Governance Controlled Amendment Independent Design Review`, pinned to this exact document's author HEAD/blob and PR #334 review. It must judge scope nonexpansion, signer substitution, evidence independence, expiration/revocation and the exact frozen-target discovery blocker. A design `PASS` is not itself a constitutional/owner grant or implementation authorization. After that review, locate exact authoritative frozen approval targets before implementing any amendment.

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_OWNER_GOVERNANCE_CONTROLLED_AMENDMENT_DESIGN
= DESIGN_CANDIDATE / INDEPENDENT_REVIEW_PENDING
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
PR333_EVIDENCE_HEAD = c51f3f2f3d60613864be03ede4b4fd2b3d85aa30
PR334_REVIEW_HEAD = face63327e172405cc84b5ae8cde57df9b6da308
PROFILE = PERSONAL_OWNER_LOCAL_NONPRODUCTION
FROZEN_PROFILE_STATUS = NOT_EFFECTIVE
PGA-G05 = FROZEN_TARGET_DISCOVERY_REQUIRED
PGA-G06 = REVIEW_PENDING
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
A1_CODE_IMPLEMENTATION = NOT_AUTHORIZED
TRUSTED_SETUP_AND_CANARY = NOT_AUTHORIZED
SA-BF-01/02 = OPEN
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
```
