# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Project Governance Profile Controlled Amendment Design v0.1

> 2026-10-09 · **DESIGN-ONLY, NOT EFFECTIVE; no grant to run R2, SETUP, CANARY, Spring, clinical operations or merge.**
>
> New material input: the repository owner explicitly states, **“这是我个人的项目”**. The project is individually owned/developed, not an enterprise system that necessarily has separately staffed Infrastructure, Security and Foundation departments. This user statement establishes **project governance context**, not proof of operating system isolation or waiver of previously accepted project-internal contracts.
>
> `main@86e8843197091c8c8172b7e4213537a31bdf0654`; [PR #341](https://github.com/cxjchelsea/AIdoctor/pull/341) head `ece0c12b4ab90c2629d315c7c9b81153196f18f5`, blob `c1098d2ac6a120cebd8e503725919486e8fd31e9`; exact-head Draft review [PR #342](https://github.com/cxjchelsea/AIdoctor/pull/342) head `3c3e6ea56412902d4c86cdab53261623cbb6587e`, blob `84ee0e3e45646f8d8aabcab0570038bbb9689a10`.
>
> Original *draft* Stage A1 authorization model [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) head `3f6725f387105798d2fa4ba6cefb1b43a69a0360`, blob `2518bdb4e0a239abf70c84f32b24ccba80c713bd`; runner/evidence plan [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) head `6884c26e93855b97a5f1293fe81c3fa5f524a739`, blob `72b71011331c6c8e36f5bad1c786ab`; earlier source/precedence report [PR #338](https://github.com/cxjchelsea/AIdoctor/pull/338) head `e28d5fc2c823f6204c4cc38e7e341ef5c41accc0`.
>
> **Design disposition:** `PROPOSED_PERSONAL_PROJECT_OWNER_GOVERNANCE`; precise future amendment targets specified below. **Effectiveness:** `NOT_APPROVED / NOT_ACTIVE`. No retroactive signed review or execution grant.

## 1. The actual governance conflict

In the PR #328 `Signed R2 Authorization Profile` draft, the fields `infrastructure_owner_identity_ref`, `security_approver_identity_ref` and `foundation_u01_approver_identity_ref` are simultaneously required. PR #326 assumes distinct owners for Stage A1 R2/SETUP/CANARY artifacts. PR #341's Draft-only personal supplement preserves all three pending fields.

For a **single-maintainer personal repository** with a personally owned nonproduction Docker/WSL2 runner, treating these as three fictitious employee positions would produce artificial paperwork and permanent deadlock. The opposite extreme—writing `APPROVED` three times under the same account and calling this an independent security assessment—is equally invalid.

**Proposed solution:** distinguish **governance decision authority** (can legitimately be exercised by the sole human repo/project owner for synthetic nonclinical work) from **technical acceptance evidence** (must be independently checked against precommitted Oracle and actual runner observables). *Separation of functions is mandatory; separation of organization employees is not necessarily mandatory for personal nonclinical inventory.*

No claim is made that any regulator or independent clinical/production authorization can be waived.

## 2. Proposed scope and role/accountability mapping

| Prior draft role/control | Proposed personal-project role | What it may sign | What it must NOT claim |
|---|---|---|---|
| Infrastructure Owner / machine owner | `PERSONAL_PROJECT_OWNER` + `LOCAL_ASSET_CUSTODIAN`, bound to authenticated `cxjchelsea` repo ownership and a separate local asset attestation | Authoring/asset ownership and future bounded R2 collection consent, **after** the profile is accepted | Hardware/kernel/host isolation verified merely from GitHub admin flag |
| Security Approver for *design governance of a synthetic local R2 inventory* | `PROJECT_OWNER_SECURITY_RISK_ACCEPTOR`, dual-hatted by sole owner **only if exact conflict-free governance amendment authorized** | Approve a **reviewed command/retention safety policy** for read-only nonclinical collection | An independent Security person's signature, verified sandbox policy denial, or automatic acceptance of own unverified evidence |
| Foundation/U01 Owner *for Stage A1 local infrastructure profile* | `PROJECT_OWNER_FOUNDATION_SCOPE_AUTHORIZER`, same owner within strictly limited Stage A1 governance | Make an explicit **scoped profile authoring/adoption decision** with reference to frozen rules | Amend clinical U01 production semantics, original code contracts or broader Foundation sign-offs |
| Technical / design review | An analytical reviewer separate from authoring *function*, with recorded exact HEAD, checklist, diff and evidence limitations | Produce review recommendation only, independently repeat/inspect tests where access allows | Create a fake second human, credentialed Security signature or independent runtime attestation |
| Oracle / negative-case evidence acceptance | A separately authored/frozen expected Oracle and an independent evidence examination (another person if available; otherwise openly recorded `SINGLE_OWNER_REVIEW_WITH_NONINDEPENDENCE_LIMITATION`) | Accept evidence **only** at a threshold permitted by final controlled policy; for claims requiring genuinely independent validation, remain BLOCKED | Mark self-produced Oracle/actual, unreachable network or `Seccomp=2` as `DENIED_BY_POLICY` |
| R2, R3, R4, merge decisions | Explicit single owner **per operation**, never inherited | Only its specific scoped action after prerequisites satisfied | Convert governance-profile adoption into execution, code-write, or merge authorization |

**Important separation:** A single human may hold multiple project-governance responsibilities only when final controlling policy expressly permits it. Evidence should label actual reviewer independence `INDEPENDENT_HUMAN`, `SEPARATE_ANALYTICAL_REVIEW`, or `SINGLE_OWNER_ONLY`. These values are **not equivalent**. Particularly, an AI-assisted technical review is not an independent human signature.

## 3. Guardrail: two assurance tiers, no cross-tier transfer

**Tier P (personal synthetic nonclinical)** may use sole-owner governance for **bounded R2 read-only introspection** when approved. Minimum independent *technical* checks: immutable command manifest, strict output schema, no raw host disclosure, isolated collector and target identity labels, expiry/revocation, all unknowns fail closed. Separate R2 owner consent is still needed.

**Tier H (medical/clinical, patient/PHI, production, deployment, public networks, Spring-backed diagnostic integration, and any high-risk activity)** is **out of scope** and remains held by its existing stricter authorizations. No personal-profile permission automatically flows into Tier H.

**Negative policy-denial canaries** are **not** Tier P R2 inventory. They require separately frozen expected Oracle, trusted synthetic fixture, observed policy-denial causal attribution, negative controls, cleanup, and a separate R4 decision. A solo-review limitation must be explicit; a narrow user-owned exploratory result cannot be promoted into external independent clinical/production assurance.

## 4. Exact controlled amendment plan (candidate-only)

No source files are amended by this design report. Proposed changes **after separate explicit authorization and independent exact-head review**:

| Target (all currently Draft / not authority) | Source blob pinned | Proposed exact change | What stays unchanged |
|---|---|---|---|
| PR #341 `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Personal_Nonproduction_R2_Authority_Supplement_v0.1.md` | `c1098d2ac6a120cebd8e503725919486e8fd31e9` | Add `governance_model: SOLO_OWNER_WITH_FUNCTIONAL_SEPARATION`; project ownership statement; proposal to replace three employee slots **for Tier P only** with one explicit owner `project_owner_authorization_ref` plus separately bounded and reviewed `r2_safety_profile_ref` and reviewer independence classification; keep flags `effective=false` | Draft non-effective state, fixed bounds, all R2/R3/R4 and merge NOT_GRANTED; no silent existing rule override |
| PR #328 `..._StageA1_Runner_Nomination_R2_Attestation_Authorization_Readiness_v0.1.md` | `2518bdb4e0a239abf70c84f32b24ccba80c713bd` | **Optional future** typed dual-profile amendment: `ENTERPRISE_SEPARATE_PRINCIPALS` unchanged; `PERSONAL_SYNTHETIC_NONCLINICAL` can use project owner role map only **if** competent owner approves the new profile, with exact version and signed/scope audit | All existing R2 fixed-argv safety, nondisclosure, telemetry gaps, and prohibition on premature runner attestation |
| PR #326 `..._StageA1_Controlled_ExactDiff_Amendment_Runner_Authority_Evidence_Plan_v0.1.md` | `72b71011331c6c8e36f5bad1c786ab` | **Optional future** role description and Tier P-only approval graph clarification; keep implementation/R3/R4 authority separate | Same six-file allowlist, negative-case Oracle, `DENIED_BY_POLICY` threshold, policy origin and independent evidence constraints |
| Source-precedence PR #338 and design/review PR #339/#340/#342 | Immutable cited heads | Referenced evidence only; no modifications | Historical meaning and prior design-review verdict unchanged |
| Existing `main` Tier-0 static manifest, frozen medical/clinical governance, runtime code, CI | No requested change | **NO_CHANGE** | Full authority and contracts preserved |

**No hidden effective status:** the first minimal future controlled diff should be only **PR #341's one Markdown document**, and be reviewed anew at exact HEAD. Changes to #328/#326, if necessary, need their own scoped authorization: this design is not blanket permission to edit them.

### Candidate field-level patch (illustrative — NOT APPLIED)

```diff
  proposal_status: DRAFT_ONLY_NOT_EFFECTIVE
  profile_id: PERSONAL_OWNER_LOCAL_NONPRODUCTION
+ governance_model: SOLO_OWNER_WITH_FUNCTIONAL_SEPARATION
+ ownership_assertion: SINGLE_MAINTAINER_PERSONAL_PROJECT_USER_DECLARED
+ project_owner_identity: cxjchelsea
+ owner_roles: [LOCAL_ASSET_CUSTODIAN, PROJECT_GOVERNANCE_APPROVER]
+ independent_human_security_signature: NOT_ASSUMED
+ technical_review_independence: SEPARATE_ANALYTICAL_REVIEW_NOT_HUMAN_CREDENTIAL
+ higher_risk_policy: UNCHANGED
+ authority_override_effect: NONE_UNTIL_EXPLICIT_APPROVED_AMENDMENT
+ r2_command_profile_approval: REQUIRED_NOT_GRANTED
+ r2_owner_consent: REQUIRED_NOT_GRANTED
+ evidence_review_limitation: NO_INDEPENDENCE_CLAIM_UNLESS_ACTUALLY_PROVEN
  decision:
    effective_supplement: false
    r2_collection_allowed: false
    setup_allowed: false
    canary_allowed: false
    clinical_production_allowed: false
    merge_allowed: false
```

This is **not** an assertion of `INDEPENDENT_HUMAN_SECURITY_SIGNATURE`. Governing-role reassignment and **actual permission** remain distinct acts.

## 5. Operation-specific authorization graph

```text
[personal project declaration + GitHub owner readback]
  -> [design of solo-owner profile, exact-source review]
  -> [explicit controlled amendment AUTHORIZATION; one diff only]
  -> [exact-head revised PR #341 technical review + no-conflict authority decision]
  -> [separate decision whether personal Tier P supplement may become effective]
  -> [local asset identity + one exact bounded R2 read-only grant]
  -> [single-run non-sensitive R2 metadata collection]
  -> [independent/evidence-limited technical review, truthfully labeled]
  != [Stage A1 six code files authorization]
  != [R3 SETUP permission]
  != [R4 canary or DENIED_BY_POLICY accepted]
  != [clinical/PHI/production/merge]
```

Each edge is **an explicit decision/gate**, not automatic progression. Expired consent, revised code/commands/runner image/kernel/retention or insufficient review => `NOT_READY`. Do not coerce missing command-level test evidence to PASS.

## 6. Acceptance checklist and open issues

| Gate | Condition | Present judgment |
|---|---|---|
| `PPG-01` | User confirms individually owned project | **PASS_USER_DECLARATION** |
| `PPG-02` | Owner account GitHub admin control | **PASS_GITHUB_METADATA** (previous check; not a host identity assertion) |
| `PPG-03` | Solo-owner role model designed without fake employees | **PASS_DESIGN_CANDIDATE** |
| `PPG-04` | Independent technical review accurately classified, never a fake Security sign-off | **PASS_DESIGN_CANDIDATE** |
| `PPG-05` | Frozen effective contracts checked for conflict and amendment is explicitly permitted | **NOT_YET_AUTHORIZED / OPEN** |
| `PPG-06` | Amendment to PR #341 exactly authorized and re-reviewed at new blob | **NOT_GRANTED** |
| `PPG-07` | Signed or explicit actual owner decision to **activate**, separate from authoring | **NOT_GRANTED** |
| `PPG-08` | Bound runner R2 grant and command manifest | **NOT_GRANTED** |
| `PPG-09` | Genuine policy denial / independent target evidence | **NOT_PROVEN** |
| `PPG-10` | No clinical scope, no code, no CI, no merge | **PASS_SCOPE** |

**Decision: `PERSONAL_GOVERNANCE_CONTROLLED_AMENDMENT_DESIGN_READY_FOR_INDEPENDENT_REVIEW`; `PERSONAL_SUPPLEMENT_EFFECTIVENESS_NOT_AUTHORIZED`.**

One concrete risk requiring review: PR #328/#326 and PR #341 were authored in enterprise-signature language. If any of those is incorporated into an effective frozen policy elsewhere, a distinct explicit precedence amendment is required before a solo owner can waive independent human signatures for **Tier P**. This design does not claim independent evidence when no independent human exists.

## 7. No-execution and no-merge conclusion

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_PROJECT_GOVERNANCE_PROFILE_CONTROLLED_AMENDMENT
= DESIGN_ONLY

PROJECT_CLASS = PERSONAL_INDEPENDENT_DEVELOPMENT (USER_DECLARED)
PROJECT_OWNER = cxjchelsea (GITHUB_ACCOUNT_VERIFIED)
SOLO_OWNER_FUNCTIONAL_ROLE_MAPPING = PROPOSED
SECURITY_FOUNDATION_ENTERPRISE_SIGNER_SUBSTITUTION = NOT_EFFECTIVE
PR341_AMENDMENT = NOT_APPLIED
PR341_SUPPLEMENT = DRAFT_NOT_EFFECTIVE
R2_COLLECTION = NOT_GRANTED
A1_CODE_R3_SETUP_R4_CANARIES = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
NEXT = TARGETED_INDEPENDENT_CONTROLLED_AMENDMENT_DESIGN_REVIEW
```
