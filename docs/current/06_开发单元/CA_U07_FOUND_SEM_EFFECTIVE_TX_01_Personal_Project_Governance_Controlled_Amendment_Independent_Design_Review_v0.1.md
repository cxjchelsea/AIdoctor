# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Project Governance Controlled Amendment Independent Design Review v0.1

> Date: 2026-10-09. **Independent analytical design review** (not a different human reviewer, official Security approval, signed Foundation/U01 decision, actual runner attestation, execution grant, or merge approval).
>
> Review object: [PR #343](https://github.com/cxjchelsea/AIdoctor/pull/343) exact HEAD `95a4e999623807a6c965b3fbdabefc7ee6f99867`, file blob `0290f674f97a57a4cc9c0cc57fa258f680fb8c1c`.
> Baseline `main@86e8843197091c8c8172b7e4213537a31bdf0654`, one Markdown ADD only:
> `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Personal_Project_Governance_Profile_Controlled_Amendment_Design_v0.1.md`.
> Cross-check: [PR #341](https://github.com/cxjchelsea/AIdoctor/pull/341) @ `ece0c12b4ab90c2629d315c7c9b81153196f18f5`, blob `c1098d2ac6a120cebd8e503725919486e8fd31e9`; [PR #342](https://github.com/cxjchelsea/AIdoctor/pull/342) @ `3c3e6ea56412902d4c86cdab53261623cbb6587e`; original Stage A1 runner/signature drafts [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) @ `3f6725f387105798d2fa4ba6cefb1b43a69a0360`, blob `2518bdb4e0a239abf70c84f32b24ccba80c713bd` and [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) @ `6884c26e93855b97a5f1293fe81c3fa5f524a739`, blob `72b71011331c6c8e36f5bad1c786ab`.
>
> **VERDICT = REVISE_REQUIRED (targeted) before authorizing controlled amendment to PR #341.**
> Concept `SOLO_OWNER_WITH_FUNCTIONAL_SEPARATION` is appropriate for a personal-only synthetic nonclinical project, and does not by itself undermine technical isolation. **Three exact design findings remain**: role-reassignment precedence, definition of independent evidence vs role separation, and a noncircular minimum authorization/effectiveness transition.

## 1. Verified repository facts and methodology

Independently fetched PR #343 HEAD/base, its unique changed filename, and the entire design file by pinned commit + returned Git blob. Main still equals `86e8843197091c8c8172b7e4213537a31bdf0654`. Read PR #328's relevant R2 command/signature contract and PR #326's six-file R0–R4 separation against the author report's targeted proposed diff; no altered original frozen document, runtime code, workflow, Docker sandbox, or PR #341 was found.

The user confirmed that this is their personal project; GitHub account `cxjchelsea` was previously verified as repository owner with administrator access. That establishes a plausible `PERSONAL_PROJECT_OWNER` authority for personal nonclinical project decisions—not a guarantee of independence, host ownership attestation, or preexisting frozen policy exception.

## 2. Review gate matrix

| Gate | Test | Result | Evidence |
|---|---|---|---|
| `PPG-IR-01` | Exact HEAD/blob, diff = one design Markdown, base unchanged | **PASS** | GitHub changed-file list + raw file readback |
| `PPG-IR-02` | Personal ownership and separation of governance versus technical evidence | **PASS_CONCEPT** | §§1–3 avoid inventing an employee roster or second human signer |
| `PPG-IR-03` | Tier P scope limited to synthetic personal read-only R2, Tier H not inherited | **PASS_CONDITIONAL** | §3 and field patch; clinical/PHI/Spring/production explicitly excluded |
| `PPG-IR-04` | R2 vs code implementation vs R3 SETUP/R4 CANARY vs merge separate | **PASS** | §2/§5 grant graph noninheritance |
| `PPG-IR-05` | Exact prior signer fields and frozen precedence conflict resolved in actionable diff | **REVISE_REQUIRED** | Proposed PR #341 diff adds owner roles but does not clearly disable/retain existing required signer fields for Tier P and address how optional PR #328 contracts interact |
| `PPG-IR-06` | Reviewer independence semantics and proof threshold unambiguous | **REVISE_REQUIRED** | §2 mixes “independent technical checks” with `SINGLE_OWNER_REVIEW_WITH_NONINDEPENDENCE_LIMITATION`; lack a fixed rule for which evidence may be accepted in single-owner mode |
| `PPG-IR-07` | Authority-change transition cannot self-approve or bypass current frozen constraints | **REVISE_REQUIRED** | §5 proposes reviewed owner-approved profile but does not state precise effectivity/authority-record acceptance gates or no-conflict failure handling as a complete state machine |
| `PPG-IR-08` | No actual R2/canary/clinical/merge grants or operator action | **PASS** | `PR341_AMENDMENT=NOT_APPLIED`, `R2_COLLECTION=NOT_GRANTED`, no runtime commands |

## 3. Findings requiring targeted design remediation

### RF-U07-PPG-IR-01 — Signer substitution semantics / authority precedence (BLOCKER for executable amendment)

PR #328 design has `infrastructure_owner_identity_ref`, `security_approver_identity_ref`, `foundation_u01_approver_identity_ref`. PR #341 currently holds independent Security/Foundation fields `NOT_GRANTED`. The proposed patch in PR #343 **adds** governance_model/roles without explicitly describing which of these original fields remains a gate, becomes `NOT_APPLICABLE_BY_APPROVED_PERSONAL_EXCEPTION`, or must refer to a distinct authority.

A downstream consumer could either remain permanently `NOT_READY` (all three still required) or dangerously ignore failed `NOT_GRANTED` values (unchecked override). Neither is acceptable.

**Required remediation:** provide a field-by-field effective-class matrix for `ENTERPRISE_SEPARATE_PRINCIPALS` versus `PERSONAL_SYNTHETIC_NONCLINICAL` including: source/field, profile dispatch rule, literal `NOT_APPLICABLE` reason and signed governing exception ref, clear Security/Foundation retention for any other scope, and conflict→`BLOCKED`. Explicitly state that altering PR #341 alone cannot change a downstream implementation that consumes PR #328's original schema; if both must change, independently authorize the second exact diff or keep `NO_EFFECT`.

### RF-U07-PPG-IR-02 — Evidence independence classification / acceptance threshold (REQUIRED)

The author correctly avoids pretending AI review is independent human approval, but lacks a deterministic distinction between **independently verifiable evidence** and **an independent human reviewer**. For a solo project, one can perform reproducible isolated technical testing and transparent self-review, but may not claim independent person review. The exact `DENIED_BY_POLICY` outcome must still require source-pinned preventive mechanism, observable enforcement event, non-self-authored expected Oracle and negative controls where applicable.

**Required remediation:** define separate orthogonal dimensions `reviewer_independence` (`INDEPENDENT_HUMAN`, `AI_ANALYTICAL_REVIEW`, `SOLO_OWNER_REVIEW`) and `evidence_reproducibility` (`REPRODUCIBLE_BY_OTHER_RUNNER`, `SOURCE_ONLY`, `UNVERIFIED`), with exact gate consequences. Stage A1 R2 collector introspection can at most be `COLLECTOR_LOCAL_FACT`; it cannot alone PASS prevention/canary Gate. Any external independent clinical/security signoff absent must remain `NOT_OBTAINED`; record exactly which claims are inadmissible without it.

### RF-U07-PPG-IR-03 — Noncircular effective amendment / separate grants (REQUIRED)

The current approval graph is helpful but `competent authorizer`, `effective governance`, `acceptance` and later R2 grants are not sufficiently typed. The personal project owner's explicit control over project policy can be a **new narrowly scoped governance rule** subject to an explicit controlled amendment, but a draft reviewing itself is not its own parent authority.

**Required remediation:** enumerate immutable gate outputs: `PROFILE_DESIGN_REVIEW`, `OWNER_CONTROLLED_AMENDMENT_AUTHORIZATION`, `EXACT_DIFF_PR341_DRAFT_UPDATE`, `POST_UPDATE_INDEPENDENT_ANALYTICAL_REVIEW`, `EXPLICIT_PROFILE_ADOPTION_DECISION`, `LOCAL_RUNNER_ASSET_CONSENT`, `R2_EXACT_COMMAND_GRANT`; specify which can be issued by a personal owner versus which evidence cannot be self-certified; require `main` conflict/head drift check at every authorizing step; establish activation as an explicit reversible, time-bounded, repo-traceable record **without implying merge**. Do not treat this independent review PR as any of those grants.

## 4. Status and exact next action

**Design strengths accepted:** personal-project classification, reason to avoid fake enterprise employee roles, functional separation, clinical/PHI exclusion, R2/SETUP/CANARY separation, negative Oracle's unchanged strict standard, and the **one-file future PR #341** amendment scope.

**Not authorized:** actual role-substitution amendment, modifying PR #341, R2 command collection, negative canary, Spring/PHI/clinical/production, merge. No permission to silently edit PR #326/#328.

**Next recommended action:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Project Governance Targeted Design Remediation` on **existing PR #343 author branch**, resolve RF-01..03 via a narrow appendix and exact field matrix; then one targeted independent re-review. Do not open another broad personal-owner design PR, and do not grant runtime permissions via this review.

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_PROJECT_GOVERNANCE_CONTROLLED_AMENDMENT_INDEPENDENT_DESIGN_REVIEW
= REVISE_REQUIRED

REVIEWED_PR343_HEAD = 95a4e999623807a6c965b3fbdabefc7ee6f99867
REVIEWED_PR343_BLOB = 0290f674f97a57a4cc9c0cc57fa258f680fb8c1c
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
RF_U07_PPG_IR_01 = BLOCKER_OPEN_SIGNER_FIELD_PRECEDENCE
RF_U07_PPG_IR_02 = REQUIRED_EVIDENCE_INDEPENDENCE_TAXONOMY
RF_U07_PPG_IR_03 = REQUIRED_NONCIRCULAR_ADOPTION_GATES
SOLO_OWNER_PROFILE = PROPOSED_NOT_EFFECTIVE
PR341_AMENDMENT = NOT_AUTHORIZED
R2_COLLECTION = NOT_GRANTED
R3_SETUP_R4_CANARIES = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```
