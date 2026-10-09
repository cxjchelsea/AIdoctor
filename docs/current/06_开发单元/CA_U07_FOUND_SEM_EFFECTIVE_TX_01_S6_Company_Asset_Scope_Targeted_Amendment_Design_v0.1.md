# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — S6 Company-Asset Scope Targeted Amendment Design v0.1

> 2026-10-09 · **DESIGN_DRAFT_ONLY / NO_FROZEN_CONTRACT_EDIT / S6_NOT_EFFECTIVE / NO_R2 / NO_MERGE**. Narrowly correct a false personal-hardware assumption; do not redesign U07 or alter actual policy permissions.
>
> Scope authorization: user said "进行下一步" after accepting the proposed **S6 Company-Asset Scope Targeted Amendment Design**. This authorizes a new *design proposal* only, not modifying PR #341/#350 or accepting any company/security signature.

## 1. Exact verified baseline and material correction

| Source | Exact verified pin | Actual role |
|---|---|---|
| `main` | `86e8843197091c8c8172b7e4213537a31bdf0654` | Baseline; no new S6 authority |
| [PR #341](https://github.com/cxjchelsea/AIdoctor/pull/341) | HEAD `e8a2bbb5baa3895e879a5436eaf21d136f7888fe`, blob `780bbff1c1ab13bea9f60a7edbaa96026f1e8429` | Personal-runner supplemental Draft; currently claims personally owned machine |
| [PR #350](https://github.com/cxjchelsea/AIdoctor/pull/350) | HEAD `705b07921722580dd90c497b2c2a072f3d2ac66c`, blob `2e259810931a66fc2dbfb6740602f95361419b13` | S6 binding-record Draft, `DRAFT_ONLY_NOT_EFFECTIVE`, currently specifies `PERSONAL_LOCAL_NONPRODUCTION` |
| [PR #347](https://github.com/cxjchelsea/AIdoctor/pull/347) | HEAD `8147a2e788d58080525f2531f50879a8cba79aac` | Design with personal-asset P2 root procedure |
| [PR #349](https://github.com/cxjchelsea/AIdoctor/pull/349) | HEAD `421959ba3ad0ba3b4a4f63b1ddb5c960870b5b76` | Analytical `PASS_CONDITIONAL_DESIGN_ONLY` for prior personal-hardware assumptions |
| [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) | HEAD `6884c26e93855b97a5f1293fe81c3fa5f524a739`, blob `72b71011331c6c8e36f2a1330d36f5bad1c786ab` | R2 separate authority requirements, still unmerged Draft |
| [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) | HEAD `3f6725f387105798d2fa4ba6cefb1b43a69a0360`, blob `2518bdb4e0a239abf70c84f32b24ccba80c713bd` | Infrastructure/Security/Foundation and exact R2 collector fields, still unmerged Draft |
| [PR #333](https://github.com/cxjchelsea/AIdoctor/pull/333) | HEAD `c51f3f2f3d60613864be03ede4b4fd2b3d85aa30`, blob `550ce59e3f5b4bc07eb4ec8d74f9150c8c35c5ae` | Self-reported Windows Docker Desktop / WSL2 candidate; not independent host attestation |
| Owner asset correction | [PR #350 comment #6075567083](https://github.com/cxjchelsea/AIdoctor/pull/350#issuecomment-6075567083) | The device is **company office hardware**, not personally owned |
| Owner company-use statement | [PR #350 comment #6075594421](https://github.com/cxjchelsea/AIdoctor/pull/350#issuecomment-6075594421) | Company permits personal-project development/tests, **OWNER_REPORTED**, not independently validated company IT authorization |

No source above is an accepted S6 operational grant. Existing company controls, where actually applicable, have precedence. Company permission to perform *general personal development* cannot be read as a blanket approval for specific host/collector commands.

## 2. Corrected profile and restricted semantics

**Proposed new candidate identity**, to avoid silently changing the meaning of an existing personal-asset selector:

```yaml
profile_id: COMPANY_ASSET_PERMITTED_PERSONAL_NONPRODUCTION
governance_model: SOLO_PROJECT_OWNER_WITH_COMPANY_ASSET_CONSTRAINTS
asset_type: COMPANY_OFFICE_COMPUTER
asset_legal_control: COMPANY
asset_use_permission_status: OWNER_REPORTED_ALLOWED_NOT_INDEPENDENTLY_ATTESTED
asset_use_scope: PERSONAL_PROJECT_LOCAL_DEV_TEST_REPORTED
project_owner_identity: cxjchelsea
organization_security_policy_precedence: REQUIRED_ALWAYS
phase: STAGE_A1
purpose: R2_READ_ONLY_PREPARATION_GOVERNANCE_ONLY
data: SYNTHETIC_NONCLINICAL_NO_PHI
effective_policy: false
r2_executable_grant: NOT_GRANTED
```

Do not conflate (a) personal GitHub project ownership, (b) employee access to company asset, (c) company's authorization, and (d) independent Security/Foundation verification. `OWNER_REPORTED_ALLOWED` is positive *user-provided context*, **not** `COMPANY_IT_APPROVED`, `COMPANY_SECURITY_SIGNED`, `HOST_ATTESTED`, or `R2_COMMANDS_APPROVED`.

**Parent/precedence dispatch:** modify only the *prospective design* of P0/P1/P2 mapping, without changing current bindings:

- P0: accepted, actually applicable company/organizational asset-use and information-security rules or other superior controls prevail. A specific disallow/restriction => `BLOCKED`. A specific relevant policy/authorized administrator permission may support permitted use, but must be attributed and scoped.
- P1: effective repository governing rules for the requested exact operation prevail. `main` Tier-0 SOURCE_ONLY is not an R2 permission; PR #326/#328 are still Draft, not active signers or executable consumers.
- P2: Owner may define a *personal project governance decision* on a company device **only within the company's actually permitted-use boundary**. P2 must not claim ownership of employer hardware or waive company security policy. `P2_PERSONAL_OWNED_ASSET_ROOT` is **inapplicable** to this device; a distinct `PROJECT_OWNER_WITH_PERMITTED_COMPANY_ASSET` route is the only candidate.
- UNKNOWN: if device ownership, permitted scope, actual applicable higher-priority constraints, source authenticity or required reviewer role is materially unresolved, `UNKNOWN_BLOCKED`; do not infer `ALLOW` from lack of an explicit prohibition.

For Path A, a separately recorded **manual, evidence-only governance reviewer** may eventually classify the new profile as eligible for *planning only*; it is not a runtime enforcement engine. S6 still requires a separate explicit Owner activation decision, authentic effective record, bounded validity/revocation and actual consumer readback. None exists yet.

## 3. Controlled future exact-diff inventory — design targets only

### 3.1 PR #341 (exact one source file, not modified now)

Target: `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Personal_Nonproduction_R2_Authority_Supplement_v0.1.md`

| Target section / key | Current exact condition | Future minimal change proposal |
|---|---|---|
| §1 introduction, §2 `profile_id` | `PERSONAL_OWNER_LOCAL_NONPRODUCTION`, personally owned Windows Docker/WSL2 | Explicitly distinguish historical personal-owned candidate from new `COMPANY_ASSET_PERMITTED_PERSONAL_NONPRODUCTION`; either conditionally add a new typed variant or designate existing variant NOT_APPLICABLE for company hardware. **Do not silently rename old token** |
| `personal_asset_owner_declaration_ref` | `NOT_GRANTED` | Keep `NOT_GRANTED`; introduce separate `company_asset_usage_permission_evidence_ref` = user claim only and `company_asset_admin_policy_confirmation_ref` = `NOT_OBTAINED`. Never record a false physical owner declaration |
| `asset_candidate` and applicability gates | Personal-owner runner assumptions | Correct type to COMPANY_OFFICE_COMPUTER, classify Docker/WSL2 facts from PR #333 as `UNVERIFIED_SELF_REPORTED`, add source correction refs |
| §3 role/authority statements; §7 or later Tier P model | Self-managed personal owner can potentially substitute personal infrastructure role | New route applies only to project-governance preparation within permitted company use; company actual governing controls retain priority; `security_approver_identity_ref` / Foundation role not forged or auto-waived |
| §4/§5 R2 boundaries | R2 physical collection `NOT_GRANTED` | Preserve all non-grants, exact allowlist and disclosure barriers. Company device has unrelated workloads; no scanning/access to employer services or other containers |

### 3.2 PR #350 (exact one source file, not modified now)

Target: `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_S6_Personal_TierP_Authority_Binding_Record_Draft_v0.1.md`

| Target section / key | Current condition | Future minimal change proposal |
|---|---|---|
| Lead scope statement | `personal, locally owned, nonproduction assets` | `company-owned office hardware permitted for personal project by Owner-reported company usage claim`, pending accepted applicable policy |
| YAML `profile.environment` | `PERSONAL_LOCAL_NONPRODUCTION` | Scoped candidate `COMPANY_ASSET_PERMITTED_PERSONAL_NONPRODUCTION`, **not effective** |
| `principal.personal_project_context` | `USER_DECLARED` | Retain for *project* ownership; add `physical_asset_owner=COMPANY` and `asset_use_permission_evidence_class=OWNER_REPORTED` separately |
| `authority_parent` | `UNKNOWN_BLOCKED` | Keep `UNKNOWN_BLOCKED`; define P0 company policy readback + P2 `PROJECT_OWNER_WITH_COMPANY_ASSET_PERMISSION` candidate; prior private-asset P2 must not succeed |
| `consumer`, `validity`, `downstream` | All non-effective / missing values | **NO PERMISSION CHANGE**; keep `NOT_ISSUED / NOT_SET / UNKNOWN / NOT_GRANTED` and `execution_allowed=false` |
| `source_binding` | Pins obsolete assumption | In future authorized amendment, append immutable owner asset correction and usage statement refs, new version review refs; avoid claiming old PR #349 review covers the changed premise |

### 3.3 PR #326 / PR #328 consumer impact

**No changes in this design task.** PR #326/#328 are unmerged Drafts and do not currently impose executable new check semantics on `main`; however their specified role requirements are *prospective incompatibilities* until a separate exact-HEAD consumer compatibility amendment or a formal finding of nonconsumption.

- PR #326 `R2` collection gate requires separately scoped infrastructure owner + Security approval and exact command collection grant. Its `A1-CA-G05/G08` do not become PASS under an Owner-reported company policy.
- PR #328 `infrastructure_owner_ref`, `security_reviewer`, `foundation_u01_owner`, `r2_collection_grant_ref`, per-command allowlist and output policies remain unassigned/ungranted. The project Owner **is not necessarily the employer asset administrator**, and `OWNER_REPORTED_ALLOWED` cannot be plugged into `security_approver_identity_ref` as an approved signature.
- No amendment shall weaken negative Oracle, setup/canary, PHI/clinical or production gates. R2 remains a *separate later authorization* even if company usage permission is ultimately verified.

### 3.4 PR #347 and PR #349 prior design

They are pinned to the original personal-owned scope. **Mark their earlier acceptance as conditional-on-old-asset-premise for a company device**; do not backdate the old review or claim it covered this new profile. Before effectivity, perform targeted exact-head independent review of revised company-asset binding/parent/consumer mapping. Further design PR proliferation is not required.

## 4. Negative acceptance checks

| Case | Expected evaluation |
|---|---|
| Hardware is company-owned but `PERSONAL_OWNED_LOCAL_NONPRODUCTION` selected | `BLOCKED_ASSET_SCOPE_MISMATCH` |
| User says company permits dev/testing, without independent company evidence | `OWNER_REPORTED_USE_PERMITTED`, **never** `COMPANY_SECURITY_APPROVED`; no executable authority |
| Actual applicable company security policy forbids any proposed R2 probe | `BLOCKED_BY_SUPERIOR_POLICY` |
| Company permitted ordinary IDE/Docker use but not verified R2 host telemetry collection | `S7_R2_NOT_GRANTED` |
| No valid `valid_from/expires_at` or current revocation evidence | `S6_NOT_EFFECTIVE` |
| PR #326/#328 future consumer requires separate human signer that has not been legitimately substituted | `BLOCKED_INCOMPATIBLE_CONSUMER` |
| Personal project Repo Owner is mistaken for company hardware/IT Owner | `BLOCKED_IDENTITY_EQUIVOCATION` |
| Any runtime, network, PHI, R3/R4, CI, merge permission inferred from profile | `BLOCKED_SCOPE_ESCALATION` |

## 5. Controlled action sequence and decision status

1. **Now:** create this *single design document only*. Do not edit PR #341/#350/#326/#328 or any frozen file.
2. Perform **Company-Asset Scope Targeted Independent Design Review** at this exact design PR HEAD/blob. Review the evidence classification, override-precedence logic, and source/consumer diff inventory.
3. Only after targeted acceptance and a **new, explicit exact-file amendment-write authorization**, edit PR #341/#350 in the smallest bounded way, preserving non-grants; independent review new source HEADs. No implicit permission from this design.
4. Collect actual company permitted-use policy/authorized source **when necessary for specific gate effectivity**, plus correct manual governance source check, time/revocation. User confirmation alone must not be relabeled as a company-issued compliance attestation.
5. Distinct Owner S6 activation decision *only after* all governing conditions are proven. Even then: `S7_R2_COLLECTION=NOT_GRANTED` until independently authorized with exact company-host command scope.

```text
S6_COMPANY_ASSET_SCOPE_AMENDMENT_DESIGN = DRAFT_CANDIDATE
CURRENT_HOST_ASSET_TYPE = COMPANY_OFFICE_COMPUTER_OWNER_ASSERTION
COMPANY_GENERAL_PERSONAL_USE = OWNER_REPORTED_PERMITTED
COMPANY_POLICY_INDEPENDENT_ATTESTATION = NOT_OBTAINED
PERSONAL_OWNED_ASSET_PROFILE = INAPPLICABLE_FOR_THIS_HOST
COMPANY_ASSET_PROFILE = PROPOSED_NOT_ACCEPTED
PR341_PR350_SOURCE_AMENDMENTS = NOT_AUTHORIZED_NOT_PERFORMED
PR326_PR328_CONSUMER_AMENDMENTS = NOT_AUTHORIZED_NOT_PERFORMED
S6_EFFECTIVE_PROFILE = FALSE
R2_EXACT_COLLECTION_GRANT = NOT_GRANTED
STAGE_A1_IMPLEMENTATION_R3_SETUP_R4_CANARY = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```
