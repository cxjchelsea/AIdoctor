# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Personal Nonproduction R2 Authority Supplement v0.1 (DRAFT ONLY)

> **NON-EFFECTIVE CANDIDATE. NOT AN AUTHORIZATION.**
>
> Date: 2026-10-09. Scope: single Markdown Draft only. Explicit user authorization, verbatim: **“AUTHORIZE DRAFT ONLY：授权基于已审查的 PR #339/#340，创建个人非生产 R2 补充契约 Draft 文件。保留所有独立安全审批要求，不授予 R2 执行或合并权限。”**
>
> Repository: `cxjchelsea/AIdoctor`; base `main@86e8843197091c8c8172b7e4213537a31bdf0654`.
> Source design: [PR #339](https://github.com/cxjchelsea/AIdoctor/pull/339) exact HEAD `18ea53c653f3d805734ad7cdcdf814f7a9570c53`, blob `ded8d1234684e2bacbd7daf00b26a8c598692641`.
> Analytical independent design review: [PR #340](https://github.com/cxjchelsea/AIdoctor/pull/340) exact HEAD `016cdc1f307c71e8ee6a292a3b7d5240c76245d7`, blob `f187618df436a2eb765a9617cd7a239f2c1a988e`, verdict `PASS / DESIGN_ONLY`, **authorization `NOT_READY`**.
> Earlier source-precedence decision: [PR #338](https://github.com/cxjchelsea/AIdoctor/pull/338) @ `e28d5fc2c823f6204c4cc38e7e341ef5c41accc0`: no currently effective personal R2 signer exception established.
>
> **The user's authorization applies ONLY to authoring this Draft candidate; it does not approve an effective supplement, override a frozen contract, issue any R2 read-only collection grant, authorize Stage A1 test implementation/setup/canary, or approve merging this or any other PR.** A GitHub Draft PR is a versioned proposal, not a signed authority.

## 1. Precise purpose and applicability

Define a proposed profile `PERSONAL_OWNER_LOCAL_NONPRODUCTION` for developer-controlled **personal** Windows Docker Desktop / WSL2 environment. The sole future possible operation is **separately authorized, bounded, read-only R2 inventory collection** using synthetic-only nonclinical assets. It is **not** itself a command permission.

Absolutely excluded: production and corporate machines; patient/clinical/PHI or real healthcare workloads; live databases, service APIs or external/public/corporate/metadata networks; host Docker socket mounts, Windows/host bind mounts, `--privileged`, host network, daemon policy changes; Stage A1 six-file implementation; R3 trusted fixture setup; R4 A1-N01..A1-N10 negative canaries; Stage A2/Stage B Spring/JVM work; CI workflows, releases and merges.

This supplement would be valid **only** after the existing applicable controlling governance authority specifically authorizes its creation and effective status. Until then all gates remain `NOT_READY`.

## 2. Proposed normative schema — strictly inactive

```yaml
schema: U07StageA1PersonalNonproductionR2AuthoritySupplementV1
proposal_status: DRAFT_ONLY_NOT_EFFECTIVE
profile_id: PERSONAL_OWNER_LOCAL_NONPRODUCTION
source_main_sha: 86e8843197091c8c8172b7e4213537a31bdf0654
design_refs:
  source_pr339_head: 18ea53c653f3d805734ad7cdcdf814f7a9570c53
  review_pr340_head: 016cdc1f307c71e8ee6a292a3b7d5240c76245d7
scope:
  stage: STAGE_A1_PREBOOTSTRAP
  phase: R2_ONLY
  operation: FUTURE_APPROVED_READ_ONLY_INVENTORY
  data_classification: SYNTHETIC_NONCLINICAL_NO_PHI
  environment: PERSONAL_WINDOWS_DOCKER_DESKTOP_WSL2
  prohibited: [CLINICAL, PRODUCTION, PHI, SPRING, DATABASE, PUBLIC_NETWORK, CORPORATE_NETWORK, METADATA_ENDPOINT, HOST_SOCKET, HOST_MOUNT, PRIVILEGED, HOST_NETWORK, POLICY_MODIFICATION, R3_SETUP, R4_CANARY, A1_IMPLEMENTATION, CI, MERGE]
authority:
  governing_parent_effective_ref: UNRESOLVED_REQUIRED
  competent_authorizer_identity_and_power_ref: UNRESOLVED_REQUIRED
  effective_supplement_authorization_ref: NOT_GRANTED
  effective_at_utc: UNSET
  personal_asset_owner_declaration_ref: NOT_GRANTED
  independent_security_approval_ref: NOT_GRANTED
  foundation_u01_approval_ref: NOT_GRANTED
  independent_command_profile_review_ref: NOT_GRANTED
  independent_evidence_acceptance_ref: NOT_GRANTED
  owner_r2_collection_consent_ref: NOT_GRANTED
  specific_r2_collection_grant_ref: NOT_GRANTED
asset_candidate:
  owner_alias: NOT_ASSIGNED
  owner_statement: USER_LOCAL_OBSERVATIONS_ONLY
  docker_context_reported: desktop-linux
  docker_desktop_reported: 4.41.2
  docker_engine_reported: 28.1.1
  kernel_reported: 6.18.33.2-microsoft-standard-WSL2
  oci_runtime_reported: runc
  test_image_id_reported: sha256:25c5b8011a3425a140bf5fa73be0feabd3c0d5b323eecb19dc02437a368ae075
  runner_asset_identity_attested: false
r2_profile:
  reviewed_exact_command_allowlist_sha256: UNSET
  binary_and_argv_digests: UNSET
  collector_principal_and_attempt_id: UNSET
  target_container_and_process_binding: UNSET
  bounded_output_schema_sha256: UNSET
  redaction_and_no_raw_stdout_stderr_controls_ref: UNSET
  owner_offline_manifest_signature_revocation_ref: UNSET
  retention_acl_ref: UNSET
  actual_collection_evidence_ref: NONE
validity:
  issued_at_utc: UNSET
  valid_from_utc: UNSET
  expires_at_utc: UNSET
  revocation_ref: REQUIRED_NOT_PROVIDED
  drift_revokes: [ASSET, OWNER, ENGINE, KERNEL, IMAGE, RUNTIME, POLICY, COMMAND, COLLECTOR, SCHEMA]
  conflicting_or_missing_authority: NOT_READY
decision:
  effective_supplement: false
  r2_collection_allowed: false
  setup_allowed: false
  canary_allowed: false
  clinical_production_allowed: false
  merge_allowed: false
```

All `UNSET`, `NOT_GRANTED` and `UNRESOLVED_REQUIRED` tokens are intentional blockers. **None** may be replaced with an assertion from the author of this document alone.

## 3. Hierarchy and signer separation (non-overriding)

1. Any actually applicable accepted **frozen** governance policy, organizational security or Foundation/U01 approval rule has precedence over this proposed file. This document is currently not an effective policy and cannot weaken any prior restriction.
2. Developer-as-personal-owner may make an **asset ownership and inspection consent declaration** solely for their own personal machine, subject to a future approved governance exception. This does **not** mean they are automatically a Security signatory.
3. An independent Security approval and Foundation/U01 authority required by controlling contracts **remain required** unless the **competent controlling governance authority** specifically and validly approves a narrow substitute. Neither owner declaration nor AI-authored independent design analysis is that approval.
4. Exact-head technical/command review and **independent evidence acceptance** remain separate from evidence production. When a genuinely required reviewer is missing, output `APPROVER_UNAVAILABLE / NOT_READY`, never self-sign or fabricate a principal.
5. No Stage A1 R2 consent inherits A1 implementation, R3 setup, R4 canaries, JVM/Spring, clinical/PHI/production, CI or PR merge authority.

## 4. Proposed R2 collection boundary (future only)

Reference [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328)'s fixed read-only `R2-01..05` argv and bounded typed output design plus `R2-06..10` offline owner-issued manifests, all **still conditional and unapproved**.

| Scope | Example allowed type **after separate exact grant** | Conditions and limits |
|---|---|---|
| R2-01/02 | Limited OS/kernel/release identifiers | Version, architecture, allowlisted ID fields; no hostname/raw disclosure |
| R2-03 | Collector UID/GID integers | No group memberships or usernames; collector does not prove target security |
| R2-04 | Only collector's seccomp/no-new-privileges/CapEff/CapBnd | Never full process enumeration or inference about unrelated workloads |
| R2-05 | Current collector's namespace inode tokens | Fixed allowlisted fields, no host namespace traversal |
| R2-06..10 | Preexisting limited signed owner asset/mount/policy/audit manifests | No live host API scans or raw firewall tables/logs; independently validate issuer, scope, expiration and revocation |

Before any real future collection: pin target/collector identities, exact reviewed non-shell argv and binary hash, timed single-attempt immutable source, Docker context/engine/kernel/runtime/image, approved output parser, no raw stdout/stderr disclosure, log retention/ACL and source-binding to each evidence claim. Missing origin / verification / safety containment => `INCOMPLETE_EVIDENCE` or `NOT_READY`.

Earlier user-supplied terminal outputs from two short-lived containers are **exploratory** observations only. `Seccomp=2`, zero effective/bounding caps, nonroot user, isolated network interface view and readonly root **are not proof** of preventive policy-denial attribution or A1-N01..N10 acceptance.

## 5. Effectiveness and expiration gates — all pending

| Gate | Required real condition | Current result |
|---|---|---|
| `PSR2-G03` | Competent governing approver identity, scope and policy authority independently established | `BLOCKED` — repository admin alone is not a Security/Foundation signature |
| `PSR2-G04` | Analytical independent review of the **design** | `PASS_DESIGN_ONLY` via PR #340; *this newly authored candidate file still needs exact-head technical review* |
| `PSR2-G05` | Explicit distinct authorization to **make supplement effective** under controlling policy | `NOT_GRANTED` |
| `PSR2-G06` | Named owner-controlled personal asset declaration and binding | `PARTIAL`, no attested asset |
| `PSR2-G07` | Independently reviewed fixed argv / output / offline manifests / retention | `NOT_READY` |
| `PSR2-G08` | Separately granted, runner-bound read-only R2 collection | `NOT_GRANTED` |
| `PSR2-G09` | Genuine independent signed/readback evidence | `NOT_PRODUCED` |
| `SA-BF-01/02` | Actual preventive policy and independent non-Spring synthetic canary evidence | `OPEN` |

A valid future approval must name the authorized signer, original policy basis, this document's exact audited HEAD/blob, the one narrowly allowed authority change, issuer/recipient, retention, expiry/revocation and explicit separate downstream grants. `main` merge, if ever considered, requires a **separate explicit user merge authorization**, exact diff/CI review and standard merge commit. This document neither requests nor grants merge.

## 6. Explicit provenance and final decision

The source `main` and PR #339/#340 IDs are verified prior version pins, not approvals. The user granted **DRAFT AUTHORING ONLY**, not R2 nor formal activation. This authoring operation adds only this one Markdown file to a Draft branch; no host commands or GitHub workflows may be triggered intentionally by it.

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_NONPRODUCTION_R2_AUTHORITY_SUPPLEMENT_DRAFT
= AUTHORED_UNDER_EXPLICIT_DRAFT_ONLY_USER_PERMISSION

SUPPLEMENT_DOCUMENT_STATUS = DRAFT_NOT_EFFECTIVE
FROZEN_AUTHORITY_CHANGED = NO
INDEPENDENT_SECURITY_AND_FOUNDATION_APPROVALS = PRESERVED / NOT_GRANTED
OWNER_APPROVAL_FOR_ACTIVATION = NOT_GRANTED
R2_READ_ONLY_COLLECTION_GRANT = NOT_GRANTED
STAGE_A1_CODE_IMPLEMENTATION = NOT_AUTHORIZED
R3_TRUSTED_SETUP = NOT_AUTHORIZED
R4_NEGATIVE_CANARIES = NOT_AUTHORIZED
STAGE_B_SPRING_AND_CLINICAL_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```


---

## 7. Scoped controlled amendment — personal project governance candidate (WRITE-ONLY, 2026-10-09)

**Precedence:** this §7 clarifies the proposed future individual-project role model and any ambiguity in §§1–6. It does **not** change their original `NOT_GRANTED` and `false` values. The entire document remains `DRAFT_ONLY_NOT_EFFECTIVE`.

**Authorized write source:** The sole repository owner expressly supplied `AUTHORIZE AMENDMENT WRITE ONLY`, limited to editing this single existing PR #341 Markdown Draft based on [PR #343](https://github.com/cxjchelsea/AIdoctor/pull/343) @ `e6994add9c612e2f7d010f6e9b66bb6f50bde45d`, blob `56fcd137d1ac8761c68dfc0fb38cf161f85597d9`, and [PR #345](https://github.com/cxjchelsea/AIdoctor/pull/345) @ `7708b2feaa2599712da5c8103b4b039a4dfa2889` (conditional design acceptance). Original PR #341 parent HEAD `ece0c12b4ab90c2629d315c7c9b81153196f18f5`, blob `c1098d2ac6a120cebd8e503725919486e8fd31e9`; main `86e8843197091c8c8172b7e4213537a31bdf0654`. Approval is **document-edit only**, not rule adoption, activation, real host inspection, code, tests or merge.

### 7.1 Personal project profile and exact scope

```yaml
# Proposal fields ONLY — never an executable grant
proposal_status: DRAFT_ONLY_NOT_EFFECTIVE
governance_model: SOLO_OWNER_WITH_FUNCTIONAL_SEPARATION
project_classification: SINGLE_MAINTAINER_PERSONAL_PROJECT_USER_DECLARED
profile_selector: PERSONAL_SYNTHETIC_NONCLINICAL
project_owner_identity: cxjchelsea
project_owner_identity_basis: GITHUB_REPOSITORY_ADMIN_NOT_HOST_ATTESTATION
project_owner_roles_proposed:
  - LOCAL_ASSET_CUSTODIAN
  - PROJECT_GOVERNANCE_SCOPE_AUTHORIZER
  - PROJECT_R2_SAFETY_RISK_ACCEPTOR
original_independent_security_approval_ref: NOT_GRANTED
original_foundation_u01_approval_ref: NOT_GRANTED
independent_human_security_signature: NOT_OBTAINED
governing_parent_effective_ref: UNRESOLVED_REQUIRED
personal_tier_p_exception_ref: NOT_GRANTED
profile_dispatch_effect: NO_EFFECT_UNTIL_APPROVED_PARENT_AND_CONSUMER_COMPATIBILITY
project_owner_authorization_ref_for_profile_adoption: NOT_GRANTED
project_owner_r2_risk_acceptance_ref: NOT_GRANTED
project_owner_stage_a1_governance_ref: NOT_GRANTED
local_asset_custody_ref: NOT_ATTESTED
r2_safety_profile_ref: UNREVIEWED_NOT_GRANTED
r2_exact_runner_command_grant_ref: NOT_GRANTED
reviewer_independence: AI_ANALYTICAL_REVIEW
evidence_reproducibility: SOURCE_ONLY
evidence_claim_class: NOT_APPLICABLE_TO_DRAFT_POLICY
actual_host_inspection_executed: false
```

This additional profile applies **only when later adopted** for a user's personally controlled, nonproduction Docker/WSL2 runner handling synthetic, nonclinical Stage A1 **R2 read-only** inventory. It excludes independent claims of protected sandbox enforcement, R3 trusted setup, R4 A1-N01..10 negative canaries, Stage A1 implementation, Spring/JVM, PHI, patients, clinical/production, CI, public/corporate networking and merge. All exclusions from §1 continue to apply.

### 7.2 Original signers: explicit profile dispatch, no forged waiver

**Selection must be positive-only.** The personal branch is eligible only if authenticated competent project-owner governance explicitly accepts this exact amended profile, effective parent/frozen-source precedence is compatible, the profile's immutable version and expiry are valid, the personal runner's custody is established, operation is `STAGE_A1_R2_READ_ONLY`, the data is synthetic and there is no higher-priority prohibitory rule. If ANY condition is unknown, conflicted, expired, or absent, return `BLOCKED` / `PROFILE_NO_EFFECT`.

| Original draft contract field | Enterprise/default branch | Proposed personal Tier P branch, only after *valid accepted exception* | Current state |
|---|---|---|---|
| PR #328 `infrastructure_owner_identity_ref` | Independent Infrastructure role | `project_owner_identity_ref` and separate `local_asset_custody_ref`; no inferred host/kernel proof | Original UNASSIGNED; personal custody NOT_ATTESTED |
| PR #328 `security_approver_identity_ref` | Actual independent Security signoff | Original value remains absent, NEVER replaced by a forged Security identity. An adopted narrow `personal_tier_p_exception_ref` MAY classify the enterprise signer requirement `NOT_APPLICABLE_BY_APPROVED_TIER_P_EXCEPTION` **for R2 nonclinical governance only**, backed by exact parent and `project_owner_r2_risk_acceptance_ref` | NOT_GRANTED; exception NOT_GRANTED |
| PR #328 `foundation_u01_approver_identity_ref` | Independent Foundation/U01 approval | Same conditional nonapplicability only with accepted `project_owner_stage_a1_governance_ref`; cannot waive U01 clinical/runtime restrictions | NOT_GRANTED; exception NOT_GRANTED |
| PR #328 `r2_collection_grant_ref` and per-argv allowlist | Explicit runner-bound and time-limited grant | Remains **separately** required as `r2_exact_runner_command_grant_ref`, exact collector binary/argv/output/retention/expiry; no inheritance | NOT_GRANTED |
| PR #341 `independent_security_approval_ref` and `foundation_u01_approval_ref` | Preserve original absent signatory fields | **Keep both `NOT_GRANTED`** in historical schema; add personal exception/dispatch alongside them, not in their place | NOT_GRANTED |
| PR #326 original R2 signer and R3/R4 gates | Original approval and negative Oracle | Any new R2-only owner-role interpretation requires separate controlled change if consumed; **no modification to R3/R4, six-file implementation or Oracle** | ORIGINAL GATES UNCHANGED |

No document may turn a literal `NOT_GRANTED` from an original signatory slot into `GRANTED` merely because this project has one maintainer. A later personal exception must have an authentic, separate, conflict-free authority record. A draft profile is **not its own parent authority**. Updating this Markdown alone does not modify the consumer fields of PR #328 / #326 or any runtime parser. If they remain incompatible, disposition is `PROFILE_NO_EFFECT / CONSUMER_INCOMPATIBLE`, not silent use of the new rule.

### 7.3 Orthogonal technical review and evidence classes

```yaml
reviewer_independence_enum:
  - INDEPENDENT_HUMAN
  - AI_ANALYTICAL_REVIEW
  - SOLO_OWNER_REVIEW
  - NONE
evidence_reproducibility_enum:
  - REPRODUCIBLE_BY_SEPARATE_VERIFIER
  - SOURCE_ONLY
  - UNVERIFIED
evidence_claim_class_enum:
  - COLLECTOR_LOCAL_FACT
  - ASSET_OWNER_ASSERTION
  - VERIFIED_POLICY_EVENT
  - CLINICAL_PRODUCTION_ASSURANCE
```

A design/source review by an assistant is `AI_ANALYTICAL_REVIEW`, **never `INDEPENDENT_HUMAN`**. Reproducible evidence is not the same thing as a separately credentialed Security signature; a claimed frozen human-independent gate remains unmet if no genuine human reviewer exists and no valid applicable exception has been approved. For future R2 collection, `/proc/self`, `id` and `uname` at most establish `COLLECTOR_LOCAL_FACT` under valid provenance; an owner manifest is `ASSET_OWNER_ASSERTION`, not independent host attestation. Neither fact proves `VERIFIED_POLICY_EVENT`, a target process's sandbox isolation, `DENIED_BY_POLICY` or clinical/production assurance. Negative Oracle, trusted synthetic fixture, actual policy event attribution, control-case observations and teardown remain distinct later-stage obligations.

Unknown provenance, reviewer classification or raw-output disclosure => `INCOMPLETE_EVIDENCE`. Never generate an expected Oracle from actual measured outputs.

### 7.4 Noncircular transition / grant matrix

| State transition | Required real source | Authority effect |
|---|---|---|
| S0 proposed → S1 design reviewed | PR #343 remediation and PR #345 exact design review | Design recommendation only |
| S1 → S2 **amendment write authorized** | User's explicit `AUTHORIZE AMENDMENT WRITE ONLY`; compare exact old PR #341 HEAD/blob and file path | Grants THIS ONE Draft Markdown edit only |
| S2 → S3 draft updated | Compare-and-swap original content blob, readback exact new PR HEAD/blob; no other changes | Modified proposal remains inactive |
| S3 → S4 new-head review | Separate exact-head analytical design review of revised PR #341 | No policy activation |
| S4 → S5 profile adoption decision | New, separately explicit and version-bound Owner instruction with accepted effective parent, priority compatibility, expiry/revocation | Decision record only, not R2 |
| S5 → S6 effective Tier P dispatch | Verify authenticated adopter, parent/consumer compatibility, actual accepted narrow signer exception and validity | Governance classification only; no commands |
| S6 → S7 R2 grant | Separate owner asset declaration, approved fixed command/hash/output restrictions/retention and limited time | Read-only R2 on one exact runner only |
| S7 → S8 R2 collection | Real authorized per-command single-run evidence envelope | Candidate facts, not automatically accepted |
| S8 → S9 R2 evidence review | Provenance check and truthfully labeled independent/solo reviewer | Accept only the permissible factual claim class |

At every authorizing transition recheck exact `main` HEAD, affected branch/file HEAD/blob, scope, authenticated issuer, parent authority, revocation, expiry, reader integrity and higher-priority conflicts. Mismatch fails closed. No part of S0–S9 confers R3, R4, Stage A1 implementation, clinical/PHI, deployment, CI or merge permission.

**This authorized action stops at S3 (Draft updated).** Even S4 has not yet been performed for the amended file. States S5–S9 are `NOT_STARTED / NOT_AUTHORIZED`.

### 7.5 Post-amendment status (must not be interpreted as activation)

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_PROJECT_GOVERNANCE_ONE_FILE_CONTROLLED_AMENDMENT
= DRAFT_WRITE_ONLY

PROFILE_MODEL = SOLO_OWNER_WITH_FUNCTIONAL_SEPARATION
PROFILE_DESIGN_REVIEW = CONDITIONAL_ACCEPTANCE_PR345
PERSONAL_PROFILE_ACTIVATION = NOT_GRANTED
ORIGINAL_SECURITY_FOUNDATION_SIGNERS = NOT_GRANTED
INDEPENDENT_HUMAN_SECURITY_APPROVAL = NOT_OBTAINED
GOVERNING_PARENT_COMPATIBILITY = UNRESOLVED
DOWNSTREAM_R2_CONSUMER_COMPATIBILITY = UNPROVEN
R2_RUNNER_IDENTITY = NOT_ATTESTED
R2_COLLECTION_GRANT = NOT_GRANTED
R2_COLLECTION_EXECUTION = NOT_AUTHORIZED
STAGE_A1_CODE = NOT_AUTHORIZED
R3_SETUP = NOT_AUTHORIZED
R4_CANARY = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
NEXT = EXACT_HEAD_POST_AMENDMENT_INDEPENDENT_DRAFT_REVIEW
```
