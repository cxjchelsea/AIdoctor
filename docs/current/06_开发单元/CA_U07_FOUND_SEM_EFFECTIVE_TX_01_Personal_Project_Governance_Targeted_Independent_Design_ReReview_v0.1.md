# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Project Governance Targeted Independent Design Re-Review v0.1

> 2026-10-09. **Independent analytical review, not a second human Security or Foundation approval.** 
> Review target: [PR #343](https://github.com/cxjchelsea/AIdoctor/pull/343), exact author HEAD `e6994add9c612e2f7d010f6e9b66bb6f50bde45d`, source Blob `56fcd137d1ac8761c68dfc0fb38cf161f85597d9`.
> Baseline `main@86e8843197091c8c8172b7e4213537a31bdf0654`. The PR's only changed path is `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Personal_Project_Governance_Profile_Controlled_Amendment_Design_v0.1.md`.
> Original independent review [PR #344](https://github.com/cxjchelsea/AIdoctor/pull/344) @ `5b6d6f8fea4a39b83b8fee52ee32842368493ab4` identifies RF-U07-PPG-IR-01..03.
> Candidate supplement [PR #341](https://github.com/cxjchelsea/AIdoctor/pull/341) @ `ece0c12b4ab90c2629d315c7c9b81153196f18f5` remains unchanged.
>
> **VERDICT: PASS / CONDITIONAL_DESIGN_ACCEPTANCE FOR THREE TARGETED FINDINGS. NOT AN AMENDMENT WRITE GRANT, NOT AN EFFECTIVE GOVERNANCE RULE, NOT AN R2 COLLECTION PERMISSION.**

## 1. Exact-head review method

Fetched GitHub latest main commit, PR #343, PR #344, PR #341 metadata, the full author source at its immutable exact HEAD, and the complete PR changed-filename list. Confirmed:
- author PR #343 exact HEAD `e6994add9c612e2f7d010f6e9b66bb6f50bde45d`, source Blob `56fcd137d1ac8761c68dfc0fb38cf161f85597d9`, one design Markdown file only, Draft and unmerged;
- base is `86e8843197091c8c8172b7e4213537a31bdf0654` and matches current main;
- PR #344's review target was prior PR #343 HEAD `95a4e999623807a6c965b3fbdabefc7ee6f99867`, blob `0290f674f97a57a4cc9c0cc57fa258f680fb8c1c`; target fixes now live in appended §8;
- PR #341 head is still `ece0c12b4ab90c2629d315c7c9b81153196f18f5`.

Reviewed §8.1–§8.4 against the three explicit findings, and checked that original restrictions on clinical/PHI, R2, setup, canary and merge remain. These are static source/diff checks, **not host tests**.

## 2. Targeted independent finding disposition

| Finding | Original issue | Revised design evidence | Independent disposition |
|---|---|---|---|
| `RF-U07-PPG-IR-01` | Owner roles proposed without exact effect on Infrastructure/Security/Foundation/U01 signer fields, risking permanent deadlock or unsafe bypass | §8.1 positive-only dispatch for Tier P; exact PR #328/PR #341/PR #326 field matrix; `NOT_APPLICABLE_BY_APPROVED_TIER_P_EXCEPTION` only when authentic approved exception ref exists; original NOT_GRANTED retained, conflicting superior rule → BLOCKED; downstream consumers explicitly require separate amendment | **CLOSED_AT_DESIGN_LEVEL / CONDITIONAL**, not actually authorized field substitution |
| `RF-U07-PPG-IR-02` | Independent human review conflated with AI analytical or solo owner review; technical reproducibility lacked separate status | §8.2 introduces `reviewer_independence`, `evidence_reproducibility`, `evidence_claim_class`; clear collector-only R2 fact ceiling; no `DENIED_BY_POLICY` from seccomp or absence of connectivity; independent-human gates cannot be satisfied by AI | **CLOSED_AT_DESIGN_LEVEL / CONDITIONAL**, no actual security attestation or external signature |
| `RF-U07-PPG-IR-03` | Proposed governance profile could become its own activation basis; missing version-bound distinct grants | §8.3 S0–S9 state transitions distinguish explicit amendment **write**, exact PR #341 Draft CAS, new-head review, separately issued owner adoption, parent/consumer compatibility, separate exact R2 grant and evidence acceptance | **CLOSED_AT_DESIGN_LEVEL / CONDITIONAL**, S2..S9 have not occurred |

The design makes its own non-effectiveness explicit. The conditional wording matters: a fully compliant implementation requires authentic accepted parent authority, checkable source precedence, and possibly separately controlled changes to the PR #328 / PR #326 consumers. A design-only reviewer cannot establish those external governance facts or silently waive an actual controlling policy.

## 3. No new design blocker for narrowly scoped next step

**PASS specifically for a possible future request** to authorize updating one Draft Markdown file in PR #341 to propose the Tier P role map without activating it.

The only permitted planned change, after **separate explicit Owner authorization**, would be to the existing:
`docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Personal_Nonproduction_R2_Authority_Supplement_v0.1.md`
at PR #341's then-exact HEAD/blob, adding positive-only role dispatch, separate original-signer fields, reviewer/evidence classification, and an explicit noncircular authorization lifecycle while keeping all `effective_supplement=false`, `r2_collection_allowed=false`, `setup_allowed=false`, `canary_allowed=false`, `clinical_production_allowed=false`, `merge_allowed=false`.

Any proposed edits to PR #328, PR #326, implementation code, host scripts, CI, Oracle, medical/PHI/production or main are **outside** that future narrow one-file authority.

## 4. Strict limitations / decisions not taken

- GitHub owner/admin account `cxjchelsea` and user statement that the repository is a personal project support the **design** premise only. They do not grant independent human Security/Foundation credentials or produce runner/OS enforcement evidence.
- This analytical independent review does **not** constitute a real second independent human. If the controlling gate requires one, this does not satisfy it unless a competent authority validly adopts a narrow exception.
- PR #343, PR #341 and PR #344 remain Draft, unchanged by this review.
- No supplementary policy has become active; no proposal can be self-referential as the accepted governing parent.
- No R2 command, local Docker command, R3 SETUP, R4 negative canary, Stage A1 code write or PR merge was authorized or executed by this review.

**Next material decision:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — PR #341 Personal Project Governance One-File Controlled Amendment Write Authorization Decision`. This is a scoped document-edit permission, **not** a rule-activation or execution grant. Obtain explicit owner instruction before making that edit; verify exact HEAD and no conflict immediately before changing the author branch.

## 5. Final result

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_PROJECT_GOVERNANCE_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

AUTHOR_PR343_HEAD = e6994add9c612e2f7d010f6e9b66bb6f50bde45d
AUTHOR_PR343_BLOB = 56fcd137d1ac8761c68dfc0fb38cf161f85597d9
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
RF_U07_PPG_IR_01 = CLOSED_AT_DESIGN_LEVEL
RF_U07_PPG_IR_02 = CLOSED_AT_DESIGN_LEVEL
RF_U07_PPG_IR_03 = CLOSED_AT_DESIGN_LEVEL
PERSONAL_PROJECT_GOVERNANCE_PROFILE = PROPOSED_NOT_EFFECTIVE
PR341_CONTROLLED_AMENDMENT_WRITE = NOT_AUTHORIZED
PR341_HEAD = ece0c12b4ab90c2629d315c7c9b81153196f18f5
R2_COLLECTION = NOT_GRANTED
STAGE_A1_IMPLEMENTATION_R3_SETUP_R4_CANARIES = NOT_AUTHORIZED
CLINICAL_PHI_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```
