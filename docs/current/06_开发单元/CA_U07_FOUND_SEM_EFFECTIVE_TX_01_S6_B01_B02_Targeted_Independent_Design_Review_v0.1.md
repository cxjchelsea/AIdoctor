# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — S6-B01/B02 Targeted Independent Design Review v0.1

> 2026-10-09 · **Independent analytical design review**, not a second authenticated human Security/Foundation approver, effective adoption record, runtime test, R2 grant or merge authorization.
>
> Exact target: [PR #347](https://github.com/cxjchelsea/AIdoctor/pull/347) HEAD `28b6d6cf1094eafb302ff0101c6bceec0751372c`, unique design file Blob `f67bf39bc1da1855671cf3240a759028a731c4c9`. Reviewed against `main@86e8843197091c8c8172b7e4213537a31bdf0654`. PR #347 is `OPEN/DRAFT/UNMERGED` and changes one new Markdown design file only.
>
> Decision: **REVISE_REQUIRED — two targeted design blockers and one required specification improvement**. No authorization to create an effective binding record or modify other PRs.

## 1. Review scope and evidence

Independently fetched PR #347 metadata, complete changed-file path list, full source Markdown from pinned HEAD, and confirmed exact current HEADs of PR #341 (`e8a2bbb5baa3895e879a5436eaf21d136f7888fe`), PR #346 (`b6bd5f7336b0fe716e7b46d8bb273adaf2115380`), PR #326 (`6884c26e93855b97a5f1293fe81c3fa5f524a739`) and PR #328 (`3f6725f387105798d2fa4ba6cefb1b43a69a0360`). PR #326/#328 are still unmerged Drafts, not presently accepted live R2 policy consumers. The prior owner S5 approval remains **conditional adoption intent**, not S6 activation.

The review tests the *internal design logic* of a personal synthetic Stage A1 R2-preparation governance rule, actual consumer identification, time bounded authority, and noninheritance. It does not validate any real host, Docker collector, live authority registry, or external organizational signatory.

## 2. Gate matrix

| Gate | Criterion | Result | Notes |
|---|---|---|---|
| `S6-IR-01` | Exact HEAD/blob and change scope | **PASS** | One Markdown addition, main unchanged |
| `S6-IR-02` | R2 preparation is separated from actual execution and higher-risk stages | **PASS** | No inheritance into R2 command execution, R3, R4, implementation or clinical |
| `S6-IR-03` | Actual current consumer identified rather than invented | **PASS_FACTUAL / REVISE_DESIGN** | It correctly says no active typed consumer is verified, but Path A improperly treats a static record as its *own sole consumer* |
| `S6-IR-04` | Frozen parent and original signers cannot be self-overridden | **PASS_PRINCIPLE / REVISE_DESIGN** | Explicit fail-closed rules exist, but competent root-of-authority selection and conflict-resolution inputs are not specified concretely |
| `S6-IR-05` | Source, owner, parent, exact HEAD/blob, review and operation bound in schema | **PASS_DESIGN** | Proposal fields are typed and mostly fail closed |
| `S6-IR-06` | Activation expiry, revocation and revalidation have deterministic verification semantics | **REVISE_REQUIRED** | Has placeholders, but no actual verifier identity/readback protocol, stale revocation response rule, or atomic activation check contract |
| `S6-IR-07` | No S6 activation or R2 grant follows from review | **PASS** | `record_status=DRAFT_NOT_EFFECTIVE`, and all downstream grants remain unavailable |

## 3. Targeted findings

### RF-U07-S6-IR-01 — Governance-only Path A lacks an actual independent verifier/consumer (BLOCKER)

PR #347 §1.1 calls an “immutable authority-binding record” the *sole authority consumer* for Path A, while also declaring `TierPProfileAuthorityResolver` nonexistent. A stored record **cannot consume, parse or verify itself**, nor can a reviewing party infer safety from its existence. As written `S6_PERSONAL_GOVERNANCE_ONLY` risks becoming a self-asserted label with no observable verification point.

**Required correction:**
- Specify Path A's concrete *human-governed* readback process: who performs the verification, exact immutable inputs/links, independent comparison of Owner-authored acceptance to accepted parent governance and effective record, and what authoritative PASS/BLOCKED outcome is recorded. It may be manual and need not require code.
- Separate **authority record (data)**, **review/verification procedure (consumer)** and **decision issuer (Owner)**. None may use its own output as its parent trust anchor.
- Rename outcome to `GOVERNANCE_PROFILE_REVIEW_ELIGIBLE_NO_EXECUTION` or equivalent if no live enforcement exists; reserve `EFFECTIVE_FOR_EXECUTION` for a separately audited runtime consumer. Do not imply an operational enforcement control exists.
- State exactly where a future R2 examiner must look up the S6 decision and verify its validity; absent a real check, **S7 remains blocked**.

### RF-U07-S6-IR-02 — Root parent authority / frozen precedence is underdetermined (BLOCKER)

`accepted_parent_policy_ref: UNRESOLVED_REQUIRED` is correctly marked blocking, but the design does not enumerate any admissible starting authority for this **personal repository** or specify how to resolve its absence without circularity. This can recreate an infinite sequence of self-proposed documents while pretending the problem is technical.

**Required correction:**
- Define explicit tiers: (1) truly applicable externally/organizationally controlled frozen policy, if found, has mandatory precedence and cannot be waived by the project Owner; (2) actual merged/reviewed repository internal rules applicable to the exact Stage A1 operation; (3) for a personal nonclinical project with no conflicting higher authority, a separately explicit authenticated **Owner policy-definition/adoption decision** may be the root governance authority, recorded as such (NOT a second human Security signature); (4) design-only PR #326/#328 remain non-effective but their future active consumers must be reconciled.
- Define an evidence-backed `parent_resolution` algorithm and finite stopping criteria: `EXTERNAL_REQUIRED` / `REPOSITORY_RULE_MATCH` / `PERSONAL_OWNER_ROOT` / `UNKNOWN_BLOCKED`, with exact readback/provenance, negative-finding scope and conflict status.
- Prevent treating “code search returned no files” or a generic Owner declaration as proof of exhaustive absence of all frozen authority. Any unresolved actually applicable prohibition keeps S6 `NOT_EFFECTIVE`.

### RF-U07-S6-IR-03 — Effective-time and revocation verification contract underspecified (REQUIRED)

PR #347 §2 provides excellent **fields** (`valid_from_utc`, `expires_at_utc`, `revocation_record_ref`, `latest_revocation_check_ref`) but does not specify the **acceptance operation** needed to safely transition from a Draft to an actually applied governance decision.

**Required correction:**
- Record creation time, exact issuer/authority ref, immutable record digest, effective interval `[valid_from_utc,expires_at_utc)`, maximum freshness of revocation readback, clock/source for comparing UTC timestamps, explicit `REVOKED` / `NOT_REVOKED` / `UNKNOWN` rules, plus status precedence.
- Every decision consumer must verify immutable source binding + effective parent + no-conflict proof + valid time window + fresh nonrevocation **together** at decision time; no caching across a source change or expiry.
- Any missing/expired/stale/unsigned-or-untrusted provenance is `NOT_EFFECTIVE`, not a deferred warning or truthy default.
- Do not claim a cryptographic human signature unless a real signature/identity verification scheme and keys actually exist; GitHub permalink/author identity is provenance, not independent clinical-security authentication.

## 4. Accepted design strengths and strict decision

Accepted as correct: limited personal/nonproduction/synthetic scope, Path A vs Path B distinction, PR #326/#328 Draft classification, source-only Tier-0 isolation, separate R2 grant requirement, no inference from AI analysis to independent human Security approval, explicit prevention of R3/R4/clinical/production/merge authority.

Three findings concern **only S6 design closure**, not runtime failure: `RF-U07-S6-IR-01` and `02` are blockers; `03` is required to claim the record can be safely applied. Do **not** conclude current S6 policy is authorized or active.

**Exact next action:** `S6-B01/B02 Targeted Design Remediation` on the **existing PR #347 design file only**, adding (a) genuine Path A authority consumer/readback and no-execution output, (b) finite competent parent selection for a solo personal project, and (c) explicit expiry/revocation decision-time acceptance contract. Then one exact-head targeted analytical re-review; no new broad architecture plan needed.

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
S6_B01_B02_TARGETED_INDEPENDENT_DESIGN_REVIEW
= REVISE_REQUIRED

REVIEWED_PR347_HEAD = 28b6d6cf1094eafb302ff0101c6bceec0751372c
REVIEWED_PR347_BLOB = f67bf39bc1da1855671cf3240a759028a731c4c9
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
RF_U07_S6_IR_01 = BLOCKER_PATH_A_VERIFIER_MISSING
RF_U07_S6_IR_02 = BLOCKER_PARENT_ROOT_SELECTION_UNRESOLVED
RF_U07_S6_IR_03 = REQUIRED_REVOCATION_TIME_ACCEPTANCE_SEMANTICS
S5_OWNER_ADOPTION = ACCEPTED_CONDITIONALLY
S6_EFFECTIVE_PROFILE = FALSE
S7_R2_COLLECTION = NOT_GRANTED
STAGE_A1_IMPLEMENTATION_R3_SETUP_R4_CANARY = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```
