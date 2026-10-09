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


---

## 5. PR #348 Targeted Remediation: RF-U07-S6-IR-01..03 (2026-10-09)

This appendix is the targeted author-side correction to [PR #348](https://github.com/cxjchelsea/AIdoctor/pull/348), independent analytical review HEAD `4d5c13bd945d93517176ae032fd2013962ea53b7`, which reviewed previous PR #347 HEAD `28b6d6cf1094eafb302ff0101c6bceec0751372c`, Blob `f67bf39bc1da1855671cf3240a759028a731c4c9`. In case of ambiguity this §5 **tightens/supersedes**, but does not execute, §§1–4. Source facts and limitations remain unchanged; this author edit **does not create** a governance binding or make S6 effective.

### 5.1 RF-U07-S6-IR-01 — A genuine manual consumer, distinct from data and issuer

**Three distinct entities** are required even for lightweight personal governance:

| Role | Concrete locus / identity | Operation | Explicit limitation |
|---|---|---|---|
| **Authority record** (data) | A future single immutable `U07PersonalTierPAuthorityBindingV1` record, pinned Git commit/blob, never this design file | Claims source identity, proposed scope, parent and validity; is subject to validation | Cannot read itself, authenticate itself, issue a new grant or satisfy independent review |
| **Decision issuer** (human owner) | Authenticated repository owner `cxjchelsea`, with separate explicit scoped activation decision bound to a reviewed binding record | Chooses governance policy within genuine own-project competence, issues revocation | Is not a separate human independent Security approver; cannot waive a higher actual authority |
| **Manual validation consumer** (documented procedure, not new runtime software) | An independent *analytical check step* using GitHub pinned readback and a recorded evidence/decision checklist; performed by owner with an AI analytical cross-check, explicitly classified `SOLO_OWNER_REVIEW + AI_ANALYTICAL_REVIEW` | Reads issuer event + immutable record + frozen parent applicability evidence, and independently checks source SHA, conflict matrix, UTC validity, revocation and operation scope. Emits `GOVERNANCE_PROFILE_REVIEW_ELIGIBLE_NO_EXECUTION` or `BLOCKED`. | Not an independent human, not a trusted automatic enforcement point; without verifiable inputs, emits `BLOCKED` |

**Manual consumer inputs and execution order (future only):**

1. Fetch actual `main` exact HEAD, PR #341 exact HEAD/Blob and the proposed binding record by **immutable ref**, not a title, branch name or copied field; verify each content digest.
2. Read independent Owner S5 acceptance event `PR341#issuecomment-6075193477`; read a **separate** explicit S6-activation event if later given. Reject absent, self-referential, unsigned/unattributed or scope-mismatched events.
3. Resolve policy parent through §5.2 using **independently obtained** applicable accepted policy references and negative-search scope statements; fail on an unknown possibly applicable superior control.
4. Fetch the exact reviewed consumer mode, current owner identity, frozen conflict/compatibility findings and immutable reviewer provenance. Do not upgrade AI review to `INDEPENDENT_HUMAN`.
5. Obtain trusted current UTC and fresh revocation-state readback per §5.3. Require one atomic *decision snapshot* matching immutable pins; reject stale/mismatched inputs.
6. Write a separate review-result artifact/comment with `checked_at_utc`, checked main/profile/binding refs, parent-resolution branch, expiration/revocation readback IDs, decision reason code and `execution_allowed=false`.
7. If a later separate **S7** grant is requested, its own human authorizer/reviewer MUST explicitly **re-perform** steps 1–6 against latest applicable state. A stale S6 review result cannot serve as a reusable command authorization.

**Path A's actual meaning:** `S6_PERSONAL_GOVERNANCE_ONLY` means an accountable Owner policy was reviewed and may be applied to **governance preparation decisions** under its scope. It is not machine-enforced. Rename positive output `GOVERNANCE_PROFILE_REVIEW_ELIGIBLE_NO_EXECUTION`, never `R2_ALLOWED`, `COLLECTOR_ATTESTED`, `DENIED_BY_POLICY`, or `EFFECTIVE_FOR_EXECUTION`. If no one performed and recorded a complete validation readback, there is no positive consumer result: `S6=NOT_EFFECTIVE`.

### 5.2 RF-U07-S6-IR-02 — Finite parent-resolution algorithm (no infinite approval chain)

Personal project governance **may** have an authorized Owner root for a *local synthetic nonclinical project policy*, but not a power to rewrite a truly superior external or previously accepted frozen security/clinical control.

| Precedence level | Acceptable grounded source | Decision outcome | Stop/fallback |
|---|---|---|---|
| P0: actually applicable higher legal, third-party asset, clinical, organizational security or contract rule | A verified binding source *and* proof its scope covers the requested operation/assets | `EXTERNAL_REQUIRED` — obey required approval, stop personal override | Missing mandatory external approval => `BLOCKED` |
| P1: effective previously accepted repository policy applying to this exact personal R2 preparation | Immutable merged/frozen record, acceptance reference, and explicit operation scope | `REPOSITORY_RULE_MATCH`; apply its existing restrictions, allow exception only under its stated amendment procedure | If conflict or no accepted exception => `BLOCKED` |
| P2: Owner-defined policy root for a personally controlled, synthetic, nonclinical project | Authenticated `cxjchelsea` Owner assertion **plus** a separately explicit scope-specific S6 activation decision referencing exact reviewed binding/PR341; P0/P1 search evidence records scope and no identified conflict | `PERSONAL_OWNER_ROOT`; policy root is the explicit Owner project-governance decision, **not the candidate Draft binding record or AI recommendation** | If property/owner/clinical/PHI scope or precedence remains uncertain => `UNKNOWN_BLOCKED` |
| Unknown | Missing applicable rule inventory, inconclusive frozen-source scope, unresolved actually applicable prohibition, unverifiable owner/source | `UNKNOWN_BLOCKED` | Terminate immediately; do not loop indefinitely or invent a second signature |

**Deterministic readback algorithm:**

```text
resolve_parent(request, owner, inspected_sources):
  assert request == PERSONAL / NONPRODUCTION / SYNTHETIC / STAGE_A1 / R2_PREPARATION
  if verified applicable P0 control exists: return EXTERNAL_REQUIRED
  if P0 applicability is materially unresolved: return UNKNOWN_BLOCKED
  if verified effective P1 rule applies: return REPOSITORY_RULE_MATCH
  if P1 applicability materially unresolved: return UNKNOWN_BLOCKED
  if repo owner verified and owner controls named personal asset,
     no identified applicable superior conflict within declared reviewed coverage,
     and separately explicit exact-binding S6 owner policy-root decision is verified:
       return PERSONAL_OWNER_ROOT
  return UNKNOWN_BLOCKED
```

**Bounded source evidence:** pin `main` HEAD, relevant frozen historical source refs/Blobs (if applicable), PR #326/#328 as `DRAFT_NON_EFFECTIVE`, exact source inventory coverage and the precise limitation `INSPECTED_SOURCES_ONLY`. Negative code-search results do not establish global nonexistence, and neither an unverified owner statement nor a self-signed Draft may override a **known applicable** superior prohibition. However, lack of a fictional enterprise Security department is not itself evidence of a superior source: if adequate scoped source/asset inquiry finds none applicable, a **separate explicit Owner-root S6 governance decision** may be a legitimate P2 root for this restricted personal project. If such an inquiry cannot be recorded, **STOP / S6_NOT_EFFECTIVE**. No recursive chain of unbounded extra approval documents is permitted.

The prior S5 event confirms *choice of candidate direction only*. It cannot simultaneously be the S6 effective-policy decision, and an AI analytical review cannot be an independent human credential.

### 5.3 RF-U07-S6-IR-03 — Deterministic decision-time validity and revocation

Add the following **required future** acceptance contract (all NOT_SET/NOT_ISSUED today):

```yaml
authority_binding_v1_required_fields:
  immutable_record_sha256: REQUIRED_FROM_ACTUAL_BYTES
  record_git_blob_sha: REQUIRED_FROM_GITHUB_READBACK
  record_created_at_utc: REQUIRED
  issuer_identity_ref: REQUIRED_AUTHENTICATED_OWNER
  parent_resolution_outcome: [EXTERNAL_REQUIRED, REPOSITORY_RULE_MATCH, PERSONAL_OWNER_ROOT, UNKNOWN_BLOCKED]
  accepted_parent_or_owner_root_decision_ref: REQUIRED_SEPARATE_NONSELF_SOURCE
  applicable_frozen_scope_inventory_ref: REQUIRED
  consumer_mode: GOVERNANCE_ONLY
  consumer_manual_validation_procedure_version: REQUIRED
  valid_from_utc: REQUIRED_EXPLICIT
  expires_at_utc: REQUIRED_EXPLICIT
  latest_revocation_check_at_utc: REQUIRED_FRESH
  latest_revocation_check_ref: REQUIRED_IMMUTABLE
  revocation_status: [NOT_REVOKED, REVOKED, UNKNOWN]
  revoked_at_utc: OPTIONAL_IF_REVOKED
  source_reviewed_pr341_head_and_blob: REQUIRED
  owner_s5_decision_ref: REQUIRED
  owner_s6_activation_ref: REQUIRED_SEPARATE
  decision_checked_at_utc: REQUIRED_TRUSTED_CLOCK
  review_result: [GOVERNANCE_PROFILE_REVIEW_ELIGIBLE_NO_EXECUTION, BLOCKED]
  execution_allowed: false
```

**Acceptance predicate at each manual decision readback**, with all checks evaluated on one pinned source snapshot:

```text
GOOD =
  binding_digest_and_git_blob_match_actual_bytes
  AND issuer_identity_is_authenticated
  AND parent_resolution_outcome in {REPOSITORY_RULE_MATCH, PERSONAL_OWNER_ROOT}
  AND no_applicable_higher_priority_conflict
  AND independently_checked_source_head_blob_and_consumer_version_match
  AND separately_issued_s6_owner_activation_ref_is_valid
  AND valid_from_utc <= trusted_now_utc < expires_at_utc
  AND created_at_utc <= trusted_now_utc
  AND revocation_status == NOT_REVOKED
  AND revocation_readback_age <= approved_max_freshness
  AND no_policy_asset_owner_or_source_drift
  AND requested_operation == STAGE_A1_R2_READ_ONLY_PREPARATION
  AND no_R2_execution_requested
ELSE BLOCKED / S6_NOT_EFFECTIVE
```

**Clock and freshness:** `trusted_now_utc` must be an explicitly identified and independently read back reliable UTC source for the decision; no authored or guessed clock is evidence. `approved_max_freshness` must be specified and accepted in a later policy before any use; **until a numeric bounded interval and an accessible canonical revocation source are approved, freshness check fails**. UTC validity is half-open `[valid_from_utc, expires_at_utc)`. `valid_from_utc >= expires_at_utc` => `BLOCKED_INVALID_INTERVAL`.

**Revocation dominance:** `REVOKED`, `UNKNOWN`, missing revocation endpoint/evidence, stale readback, time source unavailable, expired profile, SHA/version drift or source disagreement immediately fails closed. No cached `NOT_REVOKED` state may survive a source/owner/runner-policy change or expiry. An explicit revoke event must cite the immutable binding record and authenticated issuer; record historic amendments append-only (do not rewrite prior review evidence).

**Evidence / status hierarchy:** `BLOCKED` from P0/P1 conflict, revoked/expired or source mismatch has precedence over any manual positive approval. A GitHub SHA shows source integrity, **not** that a legally competent Security professional signed. S6 records may authorize **manual governance applicability only**, even if accepted. They NEVER set an R2 execution flag or satisfy `INDEPENDENT_HUMAN` credentials.

### 5.4 Finding disposition and scope freeze

| PR #348 finding | Remediation now delivered | Still prohibited |
|---|---|---|
| `RF-U07-S6-IR-01` | **DESIGN_REMEDIATED**: distinct data/issuer/manual validator, pinned six-step readback, separate evidence output; governance-only positive never executable | No approved S6 binding or runtime consumer |
| `RF-U07-S6-IR-02` | **DESIGN_REMEDIATED**: finite P0/P1/P2/UNKNOWN source precedence resolution and owner-root qualification, no recursive self-parent | No actual P2 scope inquiry/parent readback/activation grant |
| `RF-U07-S6-IR-03` | **DESIGN_REMEDIATED**: half-open UTC validity, canonical revocation freshness, atomic decision snapshot, deny-on-unknown precedence | No issued effective record, approved freshness interval or revocation endpoint |

**Next permitted task:** exact-HEAD `S6-B01/B02 Targeted Independent Design Re-Review` of PR #347 *after this amendment*, against RF-U07-S6-IR-01..03; even `PASS_DESIGN_ONLY` is not an S6 effective grant. Any future binding-record creation requires **another explicit scoped owner authoring authorization**, followed by independent review and separate activation decision.

```text
S6_B01_B02_TARGETED_DESIGN_REMEDIATION = DELIVERED_RE_REVIEW_PENDING
RF_U07_S6_IR_01 = DESIGN_REMEDIATED
RF_U07_S6_IR_02 = DESIGN_REMEDIATED
RF_U07_S6_IR_03 = DESIGN_REMEDIATED
S5_OWNER_ADOPTION = ACCEPTED_CONDITIONALLY
S6_EFFECTIVE_PROFILE = FALSE
S6_ACTIVATION_DECISION = NOT_GRANTED
S7_R2_COLLECTION = NOT_GRANTED
STAGE_A1_IMPLEMENTATION_R3_SETUP_R4_CANARY = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```
