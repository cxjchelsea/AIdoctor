# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — S6 Personal Tier P Authority Binding Record Draft v0.1

> Date: 2026-10-09. **OWNER-AUTHORIZED DRAFT AUTHORING ONLY. NON-EFFECTIVE.** This is a prospective S6 governance binding **record candidate**, not a signed effective policy, active security mechanism, command grant or test result.
>
> **Explicit authorization from project Owner:** `AUTHORIZE BINDING DRAFT ONLY` — create **only one S6 Personal Project Governance Authority Binding Record Draft** based on the reviewed PR #347/#349; leave all missing parent, activation, validity, revocation and consumer checks ungranted; no S6 activation, R2, Stage A1 implementation, tests or PR merge.
>
> **Scope:** personal, locally owned, nonproduction assets, synthetic nonclinical data; **Stage A1 R2 read-only *preparation* only**, excluding physical readback/collection until a separate S7 operation-specific grant.

## 1. Evidence and source lineage — immutable inputs, not effective approvals

| Source / function | Exact verified identifier | Bound meaning |
|---|---|---|
| Baseline `main` | `86e8843197091c8c8172b7e4213537a31bdf0654` | Source tree at drafting, not an R2 authority |
| [PR #347](https://github.com/cxjchelsea/AIdoctor/pull/347) S6-B01/B02 design | HEAD `8147a2e788d58080525f2531f50879a8cba79aac`; Blob `126edd7aaa6e5000f17dc6a933639d3bcbca7afc` | Design candidate with exact-root/manual verification/expiry semantics; **not effective** |
| [PR #349](https://github.com/cxjchelsea/AIdoctor/pull/349) targeted review | HEAD `421959ba3ad0ba3b4a4f63b1ddb5c960870b5b76` | Independent **AI analytical** review `PASS / CONDITIONAL_DESIGN_ACCEPTANCE` only |
| [PR #341](https://github.com/cxjchelsea/AIdoctor/pull/341) personal supplemental draft | HEAD `e8a2bbb5baa3895e879a5436eaf21d136f7888fe`; Blob `780bbff1c1ab13bea9f60a7edbaa96026f1e8429` | Proposed profile document; **NOT_EFFECTIVE** |
| [PR #346](https://github.com/cxjchelsea/AIdoctor/pull/346) post-amendment review | HEAD `b6bd5f7336b0fe716e7b46d8bb273adaf2115380` | `PASS / DRAFT_SCOPE_ONLY`; no operational Security signature |
| Owner S5 adoption intent | PR #341 issue comment `6075193477` | Conditional Owner acceptance, NOT the separate S6 activation event |
| S6 compatibility assessment | PR #341 issue comment `6075213698` | S6-B01/B02 identified; no profile activation |
| [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) A1 runner candidate | HEAD `6884c26e93855b97a5f1293fe81c3fa5f524a739` | `UNMERGED_DRAFT`, no live execution consumer |
| [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) R2 signer/command candidate | HEAD `3f6725f387105798d2fa4ba6cefb1b43a69a0360` | `UNMERGED_DRAFT`; original signer expectations NOT silently replaced |

These HEADs are source version pins; they **do not** attest the user's OS/host/runner, establish command authorization, or prove independence of a second human approver. If any source HEAD changes, future readback must re-evaluate the decision's applicability; this document must not assert positive compatibility without that verification.

## 2. Candidate record — all missing real values remain explicitly missing

```yaml
schema_id: U07PersonalTierPAuthorityBindingV1
record_kind: S6_PERSONAL_PROJECT_GOVERNANCE_BINDING
record_status: DRAFT_ONLY_NOT_EFFECTIVE
record_is_authoritative: false
immutable_record_sha256: TO_BE_COMPUTED_AFTER_FILE_CREATION_NOT_SELF_EMBEDDED
immutable_record_git_blob_sha: TO_BE_VERIFIED_FROM_GITHUB_AFTER_FILE_CREATION
record_created_at_utc: NOT_EVIDENCED_IN_SOURCE
record_source_provenance: GITHUB_PR_DRAFT_READBACK_PENDING
decision:
  id: CA-U07-FOUND-SEM-EFFECTIVE-TX-01-S6-PERSONAL-TIERP
  authorized_action: DRAFT_AUTHORING_ONLY
  owner_adoption_intent: S5_ACCEPTED_CONDITIONALLY
  s6_activation_decision_ref: NOT_GRANTED
  s6_status: NOT_EFFECTIVE
  execution_allowed: false
principal:
  github_repository_owner_login: cxjchelsea
  personal_project_context: USER_DECLARED
  owner_s5_adoption_ref: PR341_ISSUECOMMENT_6075193477
  owner_s6_identity_authentication_ref: NOT_VERIFIED_FOR_ACTIVATION
profile:
  governance_model: SOLO_OWNER_WITH_FUNCTIONAL_SEPARATION
  profile_id: PERSONAL_SYNTHETIC_NONCLINICAL
  environment: PERSONAL_LOCAL_NONPRODUCTION
  allowed_phase: STAGE_A1
  allowed_operation: R2_READ_ONLY_PREPARATION_ONLY
  data_classification: SYNTHETIC_NONCLINICAL_NO_PHI
  prohibited:
    - R2_PHYSICAL_COLLECTION
    - A1_IMPLEMENTATION
    - R3_SETUP
    - R4_CANARY
    - CLINICAL_PHI_PRODUCTION
    - SPRING_JVM_DATABASE
    - LIVE_NETWORK_HOST_POLICY_CHANGE
    - CI_EXECUTION
    - MERGE
source_binding:
  main_commit_sha: 86e8843197091c8c8172b7e4213537a31bdf0654
  pr341_supplement_head: e8a2bbb5baa3895e879a5436eaf21d136f7888fe
  pr341_supplement_blob: 780bbff1c1ab13bea9f60a7edbaa96026f1e8429
  pr347_design_head: 8147a2e788d58080525f2531f50879a8cba79aac
  pr347_design_blob: 126edd7aaa6e5000f17dc6a933639d3bcbca7afc
  pr349_analytical_review_head: 421959ba3ad0ba3b4a4f63b1ddb5c960870b5b76
  pr346_draft_review_head: b6bd5f7336b0fe716e7b46d8bb273adaf2115380
  binding_record_accepted_head_blob_ref: NOT_ISSUED
authority_parent:
  selection_class: UNKNOWN_BLOCKED
  valid_root_policy_or_owner_s6_activation_ref: NOT_ESTABLISHED
  scoped_applicability_inventory_ref: NOT_COMPLETE
  applicable_external_rule_resolution: NOT_FULLY_ESTABLISHED
  merged_frozen_repository_rule_resolution: NOT_FULLY_ESTABLISHED
  no_higher_priority_conflict_evidence_ref: NOT_ACCEPTED
  approved_narrow_security_signer_exception_ref: NOT_GRANTED
  independent_human_security_signature: NOT_OBTAINED
consumer:
  selected_mode: GOVERNANCE_ONLY_CANDIDATE
  manual_validation_procedure: PR347_SECTION_5_1_CANDIDATE
  manual_validation_report_ref: NOT_ISSUED
  consumer_compatibility_review_ref: NOT_ACCEPTED
  actual_typed_r2_authorization_consumer: NOT_IMPLEMENTED_NOT_AUTHORIZED
  result: BLOCKED
  execution_allowed: false
validity:
  valid_from_utc: NOT_SET
  expires_at_utc: NOT_SET
  valid_interval: HALF_OPEN_START_INCLUSIVE_END_EXCLUSIVE
  trusted_current_utc_readback_ref: NOT_PERFORMED
  max_revocation_readback_freshness_seconds: NOT_APPROVED
  canonical_revocation_source_ref: NOT_CONFIGURED
  latest_revocation_check_at_utc: NOT_PERFORMED
  latest_revocation_check_ref: NOT_PERFORMED
  revocation_status: UNKNOWN
  revoked_at_utc: NOT_APPLICABLE_NO_EFFECTIVE_RECORD
  drift_revalidation_ref: NOT_PERFORMED
review:
  independence_class: AI_ANALYTICAL_REVIEW_DESIGN_ONLY
  separate_independent_human_signer: NOT_OBTAINED
  binding_exact_head_independent_review_ref: NOT_GRANTED
  binding_authoritative_effectiveness_review_ref: NOT_GRANTED
downstream:
  r2_exact_runner_identity_ref: NOT_ATTESTED
  r2_exact_binaries_argv_allowlist_ref: NOT_APPROVED
  r2_owner_consent_ref: NOT_GRANTED
  r2_physical_collection_grant_ref: NOT_GRANTED
  stage_a1_implementation_grant_ref: NOT_AUTHORIZED
  r3_setup_grant_ref: NOT_AUTHORIZED
  r4_negative_canary_grant_ref: NOT_AUTHORIZED
  clinical_phi_production_grant_ref: NOT_AUTHORIZED
  merge_grant_ref: NOT_AUTHORIZED
```

## 3. Candidate manual consumer and parent-source precedence

**Document/record, decision Owner, and verifier are distinct functional roles**, not a claim of separate people. At this moment no manual effective-profile readback has been performed. The future manual consumer must fetch pinned immutable evidence, authenticate the separate Owner S6 decision, identify applicable higher-priority rules, validate the accepted record, UTC interval, revocation freshness and consumer scope, then issue a separate decision report with `execution_allowed=false`.

| Priority class | Matching rule (future source verification) | Candidate record state |
|---|---|---|
| P0 `EXTERNAL_REQUIRED` | Verified actually applicable external asset, contractual, safety/clinical/organizational requirement | No waiver; applicable superior rule MUST be obeyed |
| P1 `REPOSITORY_RULE_MATCH` | Actually effective reviewed/merged frozen project governance relevant to this operation | Existing rules take precedence; no silent exception |
| P2 `PERSONAL_OWNER_ROOT` | Authenticated Owner controls personal asset, scoped P0/P1 applicability inquiry recorded, no identified conflicting superior rule, **separate S6 activation decision** | `CANDIDATE_ONLY` — decision **not yet issued** |
| `UNKNOWN_BLOCKED` | Incomplete evidence or unresolved material applicability | **CURRENT RESULT — BLOCKED** |

No search-result absence can be treated as an exhaustive global absence proof. PR #326/#328 are Drafts and their future software consumers remain unimplemented. This record is not its own `accepted_parent_policy_ref`, not a substitute for an actually required independent human Security reviewer, and not an enforcement engine.

## 4. Future acceptance predicate, NOT evaluated/fulfilled

The intended manual check must bind all necessary inputs in a single versioned decision snapshot:

```text
IF record is separately issued and unambiguously authenticated
AND actual record SHA256 / Git blob match independent readback
AND exact main/supplement/design/review/consumer refs are compatible
AND effective P0/P1 precedence inquiry and P2 root authority are accepted
AND separate explicit Owner S6 activation ref is verified
AND trusted UTC >= valid_from_utc AND trusted UTC < expires_at_utc
AND validity interval is well-formed
AND canonical revocation source responds NOT_REVOKED
AND revocation readback freshness <= separately approved numeric maximum
AND current owner/asset/source versions and scope still match
AND operation == STAGE_A1_R2_READ_ONLY_PREPARATION
AND no R2 execution is requested
THEN output GOVERNANCE_PROFILE_REVIEW_ELIGIBLE_NO_EXECUTION
ELSE output BLOCKED / S6_NOT_EFFECTIVE
```

**Currently every positive branch requiring an actual issued authority record, accepted parent, independent record readback, S6 activation, valid UTC times and canonical revocation check is unsatisfied. `UNKNOWN`, expiry, missing fields, mismatched blobs, stale checks, an actual conflicting frozen authority or any attempted R2 execution => `BLOCKED`.**

No circular digest: the future SHA256 and Git blob of *this* file must be computed externally after creation, attached to a separate immutable reviewer/decision record and never embedded as self-validating content. The `TO_BE_COMPUTED` fields are informational markers, not a trust anchor.

## 5. Remaining gates and exact next decision

| Gate | Real requirement | Current |
|---|---|---|
| `S6-DRAFT-01` | Owner authorizes only one binding Draft | **AUTHORIZED_FOR_AUTHORING_ONLY** |
| `S6-DRAFT-02` | Exact independent review of this newly created record's immutable Git Blob | `PENDING` |
| `S6-AUTH-01` | Identify real P0/P1 scope and justify authenticated P2 root, if eligible | `NOT_ACCEPTED` |
| `S6-AUTH-02` | Approved consumer compatibility / manual readback protocol | `NOT_ACCEPTED` |
| `S6-AUTH-03` | Owner separate S6 effectivity decision, fixed valid interval/revocation source | `NOT_GRANTED` |
| `S6-AUTH-04` | Fresh revocation/UTC readback and exact SHA/identity validation | `NOT_PERFORMED` |
| `S7-R2` | Separate identified runner, approved fixed read-only argv, user consent and per-command grant | `NOT_GRANTED` |
| `A1/R3/R4/CLINICAL/MERGE` | Independent separately accepted scope and user permission | `NOT_AUTHORIZED` |

**Next:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — S6 Authority Binding Record Exact-HEAD Independent Draft Review`. Its `PASS_DRAFT_ONLY` cannot itself set `S6_EFFECTIVE` or `S7_R2_GRANTED`.

```text
S6_AUTHORITY_BINDING_RECORD = CREATED_AS_DRAFT_CANDIDATE_ONLY
S5_OWNER_ADOPTION = ACCEPTED_CONDITIONALLY
S6_EFFECTIVE_PROFILE = FALSE
S6_ACTIVATION = NOT_GRANTED
R2_READ_ONLY_COLLECTION = NOT_GRANTED
STAGE_A1_IMPLEMENTATION = NOT_AUTHORIZED
R3_SETUP_R4_CANARIES = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```
