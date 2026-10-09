# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — S6-B01/B02 Targeted Profile Authority Binding Design v0.1

> Date: 2026-10-09. **DESIGN_CANDIDATE_ONLY / NOT_EFFECTIVE / NO_R2_GRANT / NO_MERGE.**
>
> This document addresses **only** S6-B01 (actual profile consumer/typed dispatch compatibility) and S6-B02 (immutable effective adoption record, expiry and revocation). It does not reopen the accepted conceptual personal-governance design or change its existing Drafts.
>
> Exact checked baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654`. Source supplement [PR #341](https://github.com/cxjchelsea/AIdoctor/pull/341) exact HEAD `e8a2bbb5baa3895e879a5436eaf21d136f7888fe`, blob `780bbff1c1ab13bea9f60a7edbaa96026f1e8429`; Draft review [PR #346](https://github.com/cxjchelsea/AIdoctor/pull/346) @ `b6bd5f7336b0fe716e7b46d8bb273adaf2115380`. S5 Owner decision recorded on PR #341, comment `6075193477`: **conditional owner adoption intent**, not S6 activation. S6 gap assessment comment `6075213698`: **S6 NOT_EFFECTIVE**.
>
> Related [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) @ `6884c26e93855b97a5f1293fe81c3fa5f524a739` and [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) @ `3f6725f387105798d2fa4ba6cefb1b43a69a0360` are **unmerged Draft design contracts**; they cannot be promoted into current live `main` consumers or waived by assumption. Existing `main` Tier-0 SOURCE_ONLY static manifest `98189638619aa78e997a1a588239b36b89617378` is not an R2 grant.

## 1. S6-B01 — Identify the actual consumer and define strict dispatch

### 1.1 Consumer inventory / no fictional runtime

| Candidate consumer | Verified present role | Proposed S6 effect | Decision |
|---|---|---|---|
| `main` Tier-0 static producer contract | SOURCE_ONLY Tier-0 projection, no R2 authorizer | **NO_CHANGE**; not the personal Tier P consumer | `NOT_AN_R2_CONSUMER` |
| PR #341 personal R2 supplement | Design-only markdown, not a parser | Input to **future** governance decision and source-binding record | `DRAFT_NOT_EFFECTIVE` |
| PR #328 R2 collector authorization profile | Draft table/typed command plan with existing three signers | Either separately amend this exact consumer's schema *before it becomes accepted*, or explicitly keep it not-applicable in favor of another reviewed R2 authority profile | `DRAFT_CONSUMER_NOT_ACTIVE` |
| PR #326 A1 runner execution approval graph | Draft architecture for R2 vs implementation/R3/R4 | R2 personal exemption must never flow into R3/R4; if a future consumer calls original signer checks, typed dispatch needs separate authorized reconciliation | `DRAFT_NOT_ACTIVE` |
| **Proposed** `TierPProfileAuthorityResolver` (logical policy validator, not a program being added) | **No existing source/implementation verified** | Must be the **single logically authoritative** consumer at S6; validates accepted binding before any future R2 grant can be checked | `NOT_IMPLEMENTED / NOT_AUTHORIZED` |

**Design decision:** do not build an unrequested new platform or claim that the resolver already exists. First select one of two explicit paths at targeted independent review:

- **Path A — governance-only S6**: manually reviewed, immutable **authority-binding record** is the *sole* authority consumer; S6 can at most mean a personal policy exists for preparing a future R2-specific permission review. It **does not** enable running any R2 commands, and PR #326/#328 remain unactionable Drafts. A future R2 execution authorization must independently validate the binding.
- **Path B — active typed consumer**: before S6 can govern a real executable R2 collector, separately authorize an exact-file controlled amendment to PR #328's R2 schema and its actual collector consumer, pin every parser/probe contract and verify rejected negative cases. **No code changes approved here.**

**Preferred for minimal personal project:** Path A **only with narrow authority effect** `S6_PERSONAL_GOVERNANCE_ONLY`, not an executable `R2_ALLOWED`. It lets a sole owner make a valid project-level decision without pretending a staged runtime exists. Path B remains mandatory if any system later consumes S6 to allow a physical R2 operation.

### 1.2 Dispatch must deny by default

Conceptual, side-effect-free evaluation function:

```text
resolve(profile_request, binding_record, frozen_policy_set, consumer_manifest):
  if binding_record.state != EFFECTIVE or signature/owner/expiry/revocation invalid: BLOCKED
  if exact source HEAD/blob, owner or parent-policy lineage does not match: BLOCKED
  if applicable higher-priority frozen policy prohibits the exemption: BLOCKED
  if requested operation != STAGE_A1_R2_READ_ONLY_PREPARATION: BLOCKED
  if environment != PERSONAL_OWNED_LOCAL_NONPRODUCTION: BLOCKED
  if data != SYNTHETIC_NONCLINICAL_NO_PHI: BLOCKED
  if consumer.mode == GOVERNANCE_ONLY: return PERSONAL_GOVERNANCE_APPLICABLE_NO_EXECUTION
  if consumer.mode == R2_EXECUTOR and no separately authorized typed consumer: BLOCKED
  if any mandatory principal/asset/command evidence is absent: BLOCKED
  return PERSONAL_PROFILE_REVIEW_ELIGIBLE_ONLY
```

The return value **never** grants command execution. A later independently scoped `R2_EXACT_GRANT` is a different typed decision requiring runner, collector UID/instance, command hashes/argv, schemas, output redaction, retention, timeframe and provenance. Original PR #328 `security_approver_identity_ref` and `foundation_u01_approver_identity_ref` remain `NOT_GRANTED`; do not rename one person's signature as multiple independent reviewers. The hypothetical `NOT_APPLICABLE_BY_APPROVED_TIER_P_EXCEPTION` classification is legal only if the accepted parent policy explicitly authorizes a narrow substitution.

## 2. S6-B02 — Immutable authority-binding record (candidate schema only)

The **binding record is a separately versioned adoption record**, **not** an amendment to PR #341 itself. The S5 owner comment can be an input but is not its own accepted parent; the record cannot self-reference for authorization.

```yaml
schema_id: U07PersonalTierPAuthorityBindingV1
record_status: DRAFT_NOT_EFFECTIVE
decision_id: CA-U07-FOUND-SEM-EFFECTIVE-TX-01-S6-PERSONAL-TIERP
scope:
  repository: cxjchelsea/AIdoctor
  profile: PERSONAL_SYNTHETIC_NONCLINICAL
  allowed_phase: STAGE_A1
  allowed_operation: R2_READ_ONLY_PREPARATION_ONLY
  environment: PERSONAL_OWNED_LOCAL_NONPRODUCTION
  data_classification: SYNTHETIC_NONCLINICAL_NO_PHI
  explicitly_excluded: [R2_EXECUTION, A1_IMPLEMENTATION, R3_SETUP, R4_CANARY, CLINICAL, PHI, PRODUCTION, SPRING_JVM, CI, NETWORK, MERGE]
principal:
  github_owner_login: cxjchelsea
  owner_authenticated_ref: REQUIRED
governing_authority:
  accepted_parent_policy_ref: UNRESOLVED_REQUIRED
  parent_applicability_review_ref: NOT_ACCEPTED
  no_conflict_attestation_ref: NOT_ACCEPTED
  independent_human_signature: NOT_CLAIMED
references:
  s5_explicit_owner_adoption_comment: PR341_ISSUECOMMENT_6075193477
  supplement_head: e8a2bbb5baa3895e879a5436eaf21d136f7888fe
  supplement_blob: 780bbff1c1ab13bea9f60a7edbaa96026f1e8429
  source_main_head: 86e8843197091c8c8172b7e4213537a31bdf0654
  post_amendment_review_head: b6bd5f7336b0fe716e7b46d8bb273adaf2115380
  consumer_mode: GOVERNANCE_ONLY_PROPOSED
  consumer_contract_head_and_blob: NOT_ESTABLISHED
  consumer_compatibility_review_ref: NOT_ACCEPTED
effectivity:
  effective_decision_ref: NOT_GRANTED
  valid_from_utc: NOT_SET
  expires_at_utc: NOT_SET
  revocation_record_ref: NOT_SET
  latest_revocation_check_ref: NOT_PERFORMED
  source_drift_revalidation_ref: NOT_PERFORMED
  evaluated_status: NOT_EFFECTIVE
downstream:
  r2_exact_grant_ref: NOT_GRANTED
  stage_a1_implementation_ref: NOT_AUTHORIZED
  r3_setup_ref: NOT_AUTHORIZED
  r4_canary_ref: NOT_AUTHORIZED
  clinical_production_ref: NOT_AUTHORIZED
  merge_ref: NOT_AUTHORIZED
```

**Record immutability and provenance:**
- Pin project/repo identity, authenticated Owner decision and accepted *external-to-this-record* governing-parent source, frozen no-conflict review, exact file blob and commit SHA, and consumer mode.
- Effective record must carry real `valid_from_utc` and `expires_at_utc` with `valid_from < expires_at`, both explicit and timezone-aware, plus owner-issued revocation mechanism and observable readback. Do **not** invent dates or cryptographic signatures as part of a design.
- A commit/blob hash proves source content identity, not signatory authority. Do not call a Markdown comment a signed human-independent security approval.
- On source/parent/consumer/owner identity drift, expiry, revocation or missing binding, set `NOT_EFFECTIVE`; do not silently recalculate approval from the latest default branch.
- An active S6 policy must be recorded as a versioned effective decision **after** independent exact-head review and separate explicit Owner activation permission. This Draft can never turn itself on.

### 2.1 Noncircular authority ordering

```text
Already established:
  S4 PR341 analytical review PASS_DRAFT_SCOPE_ONLY
  S5 explicit owner adoption ACCEPTED_CONDITIONALLY
Required future actions:
  (A) resolve competent parent/precedence applicability for Tier P
  (B) select and review Path A (governance-only) or Path B (active typed consumer)
  (C) separately authorize exact binding-record authoring
  (D) independent exact-HEAD and schema compatibility review
  (E) separately request explicit S6 activation; bind real UTC validity and revocation
  (F) independently verify actual effective record and its consumer readback
  (G) separate S7 owner/runner/command-specific R2 authorization, if ever requested
```

Without (A)–(F), `S6=NOT_EFFECTIVE`. Even after (F), S7 is `NOT_GRANTED` unless independently granted. If Path A is used, S6 means only adoption of a **governance decision** for personal Tier P preparations, not a tested executable security boundary.

## 3. Exact future diff inventory / no automatic scope expansion

| Future artifact | Proposed action | Prerequisite |
|---|---|---|
| **This Markdown Design** | ADD one file, Draft-only | User instruction to proceed with targeted S6-B01/B02 design |
| New `S6 Personal Tier P Authority Binding` record | **Separate single-file controlled ADD candidate** at a future exact reviewed path | Targeted independent S6 design review and explicit authoring authorization |
| PR #341 supplement | **NO_CHANGE** now | Separate explicit authorized amendment if necessary; not implicit |
| PR #328 or PR #326 Draft R2 consumer | **NO_CHANGE** now | Only amend through separately authorized exact diff if selected Path B demands it |
| Any runtime code, CLI, CI, Docker config, clinical Java, database, external network, real host assets | **NO_CHANGE / NO_EXECUTION** | Must have new separate scoped authorization; not covered |
| Existing `main` Tier-0 static manifest or historical clinical/engineering frozen authority | **NO_CHANGE** | Cannot be weakened by Tier P design |
| Any merge or production deployment | **NOT_AUTHORIZED** | Separate owner exact-main merge authorization; if ever, standard merge commit |

## 4. Independent design review criteria, unresolved facts and decision

| Gate | Review requirement | Current status |
|---|---|---|
| `S6-B01-D1` | Consumer existence/truthful status and Path A/B choice | `DESIGNED / PATH_A_RECOMMENDED` |
| `S6-B01-D2` | Typed profile dispatch, negative cases, nontransitive execution | `DESIGN_COMPLETE` |
| `S6-B02-D1` | Immutable binding schema with source,parent,issuer,consumer and refs | `DESIGN_COMPLETE` |
| `S6-B02-D2` | Effective dates, expiry, revocation, SHA drift and no-self-parent | `DESIGN_COMPLETE` |
| `S6-B01-BLOCKER` | Effective parent applicability + actual consumer/decision point | `UNRESOLVED / S6_NOT_EFFECTIVE` |
| `S6-B02-BLOCKER` | Real issued binding record, valid S6 activation/expiry/revocation | `NOT_ISSUED / S6_NOT_EFFECTIVE` |
| `S7` | Exact runner and per-probe R2 grant | `NOT_GRANTED` |

**Requested next step:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — S6-B01/B02 Targeted Independent Design Review`. Review must challenge the proposed Path A governance-only boundary (is it meaningful without a software consumer?), ambiguity in parental frozen governance authority, and whether the field schema can accidentally imply executable R2 or bypass independent signers. A reviewer may recommend `PASS_DESIGN_ONLY` or `REVISE_REQUIRED`; cannot issue S6 activation.

```text
S6_B01_B02_TARGETED_AUTHORITY_BINDING_DESIGN = COMPLETE_CANDIDATE
S5_OWNER_ADOPTION = ACCEPTED_CONDITIONALLY
S6_EFFECTIVE_PERSONAL_PROFILE = FALSE
B01_REAL_ACTIVE_CONSUMER = NOT_ESTABLISHED
B02_ACCEPTED_AUTHORITY_BINDING = NOT_ISSUED
R2_EXACT_RUNNER_COLLECTION_GRANT = NOT_GRANTED
STAGE_A1_IMPLEMENTATION_R3_SETUP_R4_CANARY = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```
