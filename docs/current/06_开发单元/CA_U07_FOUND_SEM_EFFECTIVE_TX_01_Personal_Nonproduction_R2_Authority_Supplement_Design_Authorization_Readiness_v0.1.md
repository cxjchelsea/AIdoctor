# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Nonproduction R2 Authority Supplement Design + Authorization Readiness v0.1

> 2026-10-09 | Repository: `cxjchelsea/AIdoctor`.
>
> **Classification:** `DESIGN_CANDIDATE + AUTHORIZATION_READINESS_ASSESSMENT`, **NOT** an effective authority supplement, signed approval, frozen amendment, grant, or executable instruction.
>
> Baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654`.
> Source & precedence decision: [PR #338](https://github.com/cxjchelsea/AIdoctor/pull/338), exact HEAD `e28d5fc2c823f6204c4cc38e7e341ef5c41accc0`, blob `caf55e8f99672314a1901c1bf8fb3a528987493b`.
> Owner profile author [PR #335](https://github.com/cxjchelsea/AIdoctor/pull/335) @ `cef1e77948403bdddab1371a2592115ce4b88365`; targeted analytical review [PR #337](https://github.com/cxjchelsea/AIdoctor/pull/337) @ `8eea5bdbd4e1ce70cfd4181196b408fcdb4bafc3`.
> Draft R2 contract [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) @ `3f6725f387105798d2fa4ba6cefb1b43a69a0360`, blob `2518bdb4e0a239abf70c84f32b24ccba80c713bd`; Stage A1 six-file proposal [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) @ `6884c26e93855b97a5f1293fe81c3fa5f524a739`.
>
> **Decision: SUPPLEMENT_DESIGN_READY_FOR_INDEPENDENT_REVIEW; AUTHORIZATION_READINESS = NOT_READY / EXPLICIT_COMPETENT_GOVERNANCE_APPROVAL_REQUIRED.** This file does not itself satisfy the missing approval.

## 1. Specific objective and precedence

Design **one narrow future supplemental authority** for personal, locally managed Docker Desktop/WSL2 with synthetic-only, nonclinical Stage A1 **R2 read-only** machine/isolated-container facts. This is an alternative only to *personal asset ownership and local collection consent*, not to required independent security review, Foundation/U01 governance, approved R2 evidence or enforcement/oracle verification.

Per PR #338, no already effective personal R2 signer exception was identified on the inspected `main` and earlier enterprise governance source. **Controlling effective frozen contracts take precedence** over any personal supplement; a new supplement can be effective only after a competent policy owner explicitly establishes its source-of-truth location, applicability, non-overriding precedence and independent approvals. PR #326/#328 are OPEN Draft designs, not actual permission.

The proposed authoritative file path (if and only if authorized):
`docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Personal_Nonproduction_R2_Authority_Supplement_v0.1.md`.

**This PR intentionally uses a DIFFERENT path for design+readiness**, so an unapproved Draft Markdown cannot masquerade as that effective supplement. No append-only authority record or signed manifest is created here.

## 2. Proposed supplemental contract — exact bounded fields (not issued)

```yaml
schema: U07StageA1PersonalNonproductionR2AuthoritySupplementV1
status: DRAFT_NOT_EFFECTIVE
profile_id: PERSONAL_OWNER_LOCAL_NONPRODUCTION
scope:
  stage: CA-U07-FOUND-SEM-EFFECTIVE-TX-01/STAGE_A1/R2
  operation: READ_ONLY_OFFLINE_BOUND_RUNNER_INSPECTION
  environment: USER_OWNED_PERSONAL_WINDOWS_DOCKER_WSL2
  dataset: SYNTHETIC_ONLY_NO_PHI
  excludes:
    - PRODUCTION_CLINICAL_PATIENT_PHI
    - ORGANIZATIONAL_OR_THIRD_PARTY_RUNNER
    - SPRING_JVM_STAGE_A2_OR_STAGE_B
    - DB_API_PUBLIC_NETWORK_CORPORATE_NETWORK_OR_METADATA_ENDPOINT
    - A1_IMPLEMENTATION_R3_SETUP_R4_CANARY_EXECUTION
    - PRIVILEGED_CONTAINER_DOCKER_SOCKET_HOST_MOUNT_HOST_NETWORK
    - MAIN_MERGE_CI_RELEASE
precedence:
  governing_authority_ref: REQUIRED_EXACT_AUTHORIZED_REF
  competent_governance_authorizer_ref: REQUIRED_SIGNED_OWNER_DECISION
  conflicting_effective_prohibition: FAIL_CLOSED
  enterprise_security_foundation_clinical_policy: UNCHANGED
candidate_asset:
  owner_declared_nonidentifying_alias: REQUIRED_UNASSIGNED
  source_class: PERSONAL_NONPRODUCTION
  owner_identity_or_trusted_declaration_ref: REQUIRED_NOT_PRESENT
  docker_context: desktop-linux
  docker_desktop_version_user_reported: 4.41.2
  docker_engine_version_user_reported: 28.1.1
  kernel_user_reported: 6.18.33.2-microsoft-standard-WSL2
  default_runtime_user_reported: runc
  local_test_image_id_user_reported: sha256:25c5b8011a3425a140bf5fa73be0feabd3c0d5b323eecb19dc02437a368ae075
authorities:
  asset_owner: LOCAL_OWNER_SELF_DECLARATION_PROPOSED
  local_r2_collection_consent: MAY_BE_OWNER_ONLY_IF_GOVERNANCE_AMENDMENT_EFFECTIVE
  command_profile_independent_review: REQUIRED_UNAPPROVED
  evidence_acceptor_independent_review: REQUIRED_UNAPPROVED
  security_signer_waiver: FORBIDDEN_WITHOUT_EXACT_AUTHORIZED_EXCEPTION
  foundation_u01_signer_waiver: FORBIDDEN_WITHOUT_EXACT_AUTHORIZED_EXCEPTION
r2_collection:
  exact_command_allowlist_ref: REQUIRED_FROZEN_REVIEWED
  collector_binary_and_per_argv_digests: REQUIRED
  invocation_principal_and_runner_attempt_ref: REQUIRED
  allowed_fields_and_output_schema_digest: REQUIRED
  raw_host_paths_env_secrets_daemon_logs: FORBIDDEN
  offline_manifest_signature_and_revocation_readback: REQUIRED_WHEN_APPLICABLE
  retention_acl_and_redaction_ref: REQUIRED
  no_raw_stdout_stderr_disclosure_proof: REQUIRED_BEFORE_EXECUTION
validity:
  issued_at_utc: UNSET
  valid_from_utc: UNSET
  expires_at_utc: UNSET
  revocation_record_ref: REQUIRED
  invalidating_drift: [OWNER, ASSET, IMAGE, ENGINE, KERNEL, RUNTIME, POLICY, ARGV, COLLECTOR, ORACLE, OUTPUT_SCHEMA]
  invalid_or_unknown: NOT_READY
approval:
  independent_supplement_design_review_ref: PENDING
  exact_diff_and_source_precedence_review_ref: PENDING
  competent_governance_authorization_ref: NOT_GRANTED
  local_owner_r2_consent_ref: NOT_GRANTED
  actual_r2_collection_grant_ref: NOT_GRANTED
```

No cryptographic identity is inferred from Docker Context, an image digest or self-supplied terminal output. An owner alias and timestamp are distinct from independently signed machine/security attestation.

## 3. Original-signature vs personal-profile authority matrix

| Original Stage A1 R2 **draft** requirement | Personal supplement's permitted treatment | Effective grant condition / fail-closed branch |
|---|---|---|
| `infrastructure_owner_identity_ref`: runner asset control | Developer can be `LOCAL_ENVIRONMENT_OWNER` of their own declared asset | Explicit narrow policy exception + owner declaration + exact asset binding; else NOT_READY |
| Infrastructure consent to scoped R2 inventory | Owner can be `LOCAL_COLLECTION_CONSENT_SIGNER` only for own personal nonclinical machine | Effective approved supplement **and** distinct exact-argv R2 consent; no retroactive grants |
| `security_approver_identity_ref` | **No automatic self-substitution**; preserve credentialed independent authority unless the actual governing authority explicitly authorizes a narrower alternative | Absent independent signer/approved exception => NOT_READY |
| `foundation_u01_approver_identity_ref` | **No automatic self-substitution** for Core/frozen authority | Absent controlling owner/delegation => NOT_READY |
| R2 collector / command-profile author | Fixed-command collector on synthetic-only local machine after distinct permission | Independent approved command-profile review; no raw logs, host enumeration, Docker socket or shells |
| R2 evidence acceptance | Independent acceptance must be distinct from evidence production | If independent role unavailable => NOT_READY |
| R3 setup, R4 negative canaries, A1 code, Stage A2/B, production | **Not covered** | Separate scoped grants and unchanged original gates |

**Roles cannot be conjured by labels**: an AI-authored independent *analytical* review cannot issue an organizational Security/clinical identity or replace a real authorized signer. Even if the personal developer explicitly owns the workstation, ownership is not acceptance of security enforcement proof.

## 4. Candidate R2 fixed-command / output contract

Reference PR #328's fixed `R2-01..05` non-shell argv profiles, bounded output schemas and offline owner statements `R2-06..10` **without treating that PR as effective authority**.

| Group | Candidate read-only evidence | Minimum safeguards |
|---|---|---|
| R2-01 | Kernel OS/architecture | Exact approved `uname` argv; output allowlist, no hostname |
| R2-02 | OS release | Approved `ID` + `VERSION_ID` only; no unfiltered release dump |
| R2-03 | Collector UID/GID | Integers only, no account names/group enumeration |
| R2-04 | Collector process seccomp, no_new_privs, caps | Filtered `/proc/self/status` fields only; collector != future test target |
| R2-05 | Namespace identity | Only bounded inode tokens from proc-self namespaces |
| R2-06..10 | Minimal owner-issued offline signed asset/mount/policy/telemetry attestations | Never live runner API, raw firewall dumps, host paths or token; signature, expiry, revocation and independent readback mandatory where required |

For a future approved local R2 run, collect **one bounded immutable evidence envelope per attempt**, mapping exact controller/engine image IDs, container attempt/process reference, command allowlist digest, collector provenance, typed fields, timing, retention and independent review. Every missing mandatory identity or raw-output containment fact => `INCOMPLETE_EVIDENCE` and refuse authorization. Two earlier ephemeral self-reported Docker commands cannot retroactively become a formal signed R2 evidence bundle.

The mechanism is a *read-only inventory*. No synthetic listener, socket probing, firewall modification, namespace creation, eBPF, auditd change, real network access, canary process or negative-denial policy attempt is authorized by it.

## 5. Authorization readiness checklist (as of exact source)

| ID | Must hold before supplement effective | Observed state | Decision |
|---|---|---|---|
| `PSR2-G01` | Exact governing `main` and design references | Verified `main@86e8843197091c8c8172b7e4213537a31bdf0654`, PR #338/#335/#337/#328 | PASS_SOURCE |
| `PSR2-G02` | No silent override of Security/Foundation/clinical authority | Normative prohibition present above | PASS_DESIGN_ONLY |
| `PSR2-G03` | Applicable competent governance owner identified, authority to accept supplemental contract evidenced | **Not identified/signed** in available source | BLOCKED |
| `PSR2-G04` | Independent exact-source/changed-file design review of the **new** proposal | No independent review of this exact HEAD yet | PENDING |
| `PSR2-G05` | Separate explicit authorization to add effective supplemental authority file | **Not granted** | BLOCKED |
| `PSR2-G06` | Stable owner-controlled runner alias, owner declaration, freshness + provenance | Basic user CMD observation only, no signed owner profile | PARTIAL |
| `PSR2-G07` | Exact approved R2 allowlist, binary/argv digest, no-raw-output controls, retention/ACL | Design refs only, no accepted binding | NOT_READY |
| `PSR2-G08` | Separate actual local R2-only collection grant within valid authority | **Not granted** | BLOCKED |
| `PSR2-G09` | Independently verifiable, runner/collector/target-bound result and original Oracle preserved | Not produced | NOT_READY |
| `PSR2-G10` | No A1 source changes, SETUP/CANARY, Spring, PHI, CI or merging | Scope excluded in this proposal | PASS_SCOPE_ONLY |

**Aggregate decision: `NOT_READY / COMPETENT_GOVERNANCE_APPROVAL_MISSING`.** Ready **for independent design review only**. Even design review PASS cannot on its own satisfy G03/G05/G08.

## 6. Concrete approval decision needed, no fictitious signature

Before any effective contract authoring, obtain a real named decision referencing:
- exact approving person/role and basis of authority over the repository's nonclinical Stage A1 local R2 governance;
- proposed single ADD file path above, its parent authority / default precedence, and reason this cannot override clinical/enterprise controls;
- accepted personal-owner consent substitution **only** for local environment management + scoped R2 read-only operations; distinct Security/Foundation-independent acceptance rules;
- approved independent reviewer(s) where the effective governance policy requires them;
- authorized exact-HEAD content and expiry/revocation of approval; permission to create document **separate from** permission to run R2;
- denial path for absent approver or no acceptable provenance.

If an explicit scope-specific decision is genuinely available, record it as external evidence without assuming the decision exists merely because a user chooses to proceed. If not, stop at `NOT_READY`. There is **no need to repeat a generic corporate Runner discovery**: we have a real local candidate already.

## 7. Smallest future exact-diff inventory and recommended next action

| Artifact | Exact proposed operation | Status |
|---|---|---|
| Present **design and readiness** Markdown only | ADD in new Draft PR based on pinned main | **This request**, no authority effect |
| `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Personal_Nonproduction_R2_Authority_Supplement_v0.1.md` | Future ADD only after authorizer is verified and grants a controlled amendment | `NOT_AUTHORIZED`; do **not** create now |
| PR #326/#328 historical draft documents and PR #335/#337/#338 | Refer by exact HEAD/Blob only, no rewriting | UNCHANGED |
| Existing main Tier-0 contract and frozen enterprise/clinical code | NO CHANGE | PRESERVED |
| Docker host, Stage A1 six executable files, Oracle, CI, Stage B/PHI/clinical/production | NO ACTION | NOT_AUTHORIZED |

**Next executable work in repo:** independent review of this exact **design-only** file and a genuine governing-owner supplemental-contract authorization decision. If authority is unavailable, retain NOT_READY and stop; independently pursued exploratory local Docker tests are not promoted to R2 formal evidence.

## 8. Formal readiness verdict

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_NONPRODUCTION_R2_AUTHORITY_SUPPLEMENT_DESIGN
= READY_FOR_INDEPENDENT_DESIGN_REVIEW

BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
PR338_SOURCE_HEAD = e28d5fc2c823f6204c4cc38e7e341ef5c41accc0
PR338_SOURCE_BLOB = caf55e8f99672314a1901c1bf8fb3a528987493b
PROFILE = PERSONAL_OWNER_LOCAL_NONPRODUCTION
PSR2_G03_COMPETENT_GOVERNANCE_OWNER = MISSING
PSR2_G04_INDEPENDENT_DESIGN_REVIEW = PENDING
PSR2_G05_EFFECTIVE_SUPPLEMENT_ADD_AUTHORIZATION = NOT_GRANTED
PSR2_G08_R2_COLLECTION_GRANT = NOT_GRANTED
AUTHORIZATION_READINESS = NOT_READY
EFFECTIVE_AUTHORITY_SUPPLEMENT = NOT_CREATED
LOCAL_RUNNER = ELIGIBLE_CANDIDATE_NOT_ATTESTED
STAGE_A1_IMPLEMENTATION_OR_CANARY = NOT_AUTHORIZED
FOUNDATION_AUDIT = NOT_PASSED
MERGE = NOT_AUTHORIZED
```
