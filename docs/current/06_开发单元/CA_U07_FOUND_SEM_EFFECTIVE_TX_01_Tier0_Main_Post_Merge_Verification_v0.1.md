# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Main Post-Merge Verification v0.1

> Verification date: 2026-10-08
> **Execution authorization:** explicit user approval to merge only clean [PR #313](https://github.com/cxjchelsea/AIdoctor/pull/313) into `main` using **standard merge commit** after exact source/base/diff/check verification. User explicitly accepts documentary independent reviews PR #310 and #314.
> **Actual GitHub PR #313 merge:** `MERGED=true`, `sha=86e8843197091c8c8172b7e4213537a31bdf0654`.
> **Post-merge verification verdict: PASS / COMMIT_TREE_AND_BLOB_INTEGRITY; POST_MERGE_CI_NOT_EVIDENCED.**
> This report is an evidence-only review artifact on its own independent branch. **Do not auto-merge this evidence PR** or any additional PR.

## 1. Pre-merge exact checks

Immediately before merge, independently queried PR #313, literal `main`, GitHub compare, PR review state, check runs, and the four source Git Blobs.

```text
PR = #313 / Ready for review / Draft=false
SOURCE HEAD = ff3539acb9c8bb0b798c716fee97c21578595774
TARGET main HEAD = 6d4fd787600e3a57f01f3e17893e6d98893ac546
TARGET BASE PR SHA = same as main
MERGEABLE = true
MERGED (before) = false
EXACT DIFF = 4 ADD-only files, 623 additions, 0 deletions
AHEAD/BEHIND = 4/0
EXACT SOURCE BLOBS = 4/4 MATCH PR #305 INDEPENDENTLY ACCEPTED CODE
GITHUB CHECKS = u02-java SUCCESS; u03-java SUCCESS; u02-python SUCCESS
MAIN REQUIRED STATUS CHECKS = none configured; branch not protected
FORMAL GITHUB PR REVIEWS = 0
OWNER APPROVAL = explicit acceptance of independent reviews PR #310 and #314
OWNER MERGE EXECUTION APPROVAL = explicit for PR #313 -> main only
```

Every pre-merge hard predicate passed. Formal GitHub approval absence was not hidden: the user's own explicit acceptance of the documentary reviews and instruction to merge was the recorded owner-level approval.

## 2. Actual merge execution (only PR #313)

Invoked GitHub's pull request merge for `pr_number=313` with:
- `merge_method = merge` (**standard two-parent merge commit**, no squash/rebase).
- `expected_head_sha = ff3539acb9c8bb0b798c716fee97c21578595774` (GitHub rejects HEAD drift).
- A merge title/body encoding owner authorization, bounded D0 source/target SHAs, prior evidence reviews, and no Tier-1 authority.

GitHub response:
```text
merged = true
message = "Pull Request successfully merged"
merge_sha = 86e8843197091c8c8172b7e4213537a31bdf0654
```

**No other PR was merged.** No branch was rebased/squashed; no Spring/Java application was started, and no production or database integration was attempted.

## 3. Post-merge source, target and parent proofs

Independently queried the *new* `main` branch and the GitHub merge commit API:

```text
main@after =
86e8843197091c8c8172b7e4213537a31bdf0654

PR #313 merged = true
PR #313 merge_commit_sha =
86e8843197091c8c8172b7e4213537a31bdf0654

Merge commit parents (exactly 2):
parent[0] = 6d4fd787600e3a57f01f3e17893e6d98893ac546
parent[1] = ff3539acb9c8bb0b798c716fee97c21578595774
```

This confirms an actual **standard merge commit** of precisely the reviewed base and source, not a squash, rebase or alternate branch history.

## 4. Post-merge actual tree/file and blob validation

Compared `main@before = 6d4fd787...` and `main@after = 86e88431...` via GitHub and inspected each file at the actual merged commit:

| D0 | Integrated file | Post-merge Git Blob | Matches closed PR #305 and clean PR #313 |
|---|---|---|---|
| D0-01 | `tools/u07_foundation_tx_topology/static_projection.py` | `45fd3b336a7ac8797f19714dca111292aba3840e` | **PASS** |
| D0-02 | `tools/u07_foundation_tx_topology/static_schema.py` | `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` | **PASS** |
| D0-03 | `tools/u07_foundation_tx_topology/test_static_projection.py` | `36731fadf8c9c574b326549084a29d19f2249478` | **PASS** |
| D0-04 | `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` | `98189638619aa78e997a1a588239b36b89617378` | **PASS** |

```text
POST_MERGE_COMPARE = EXACT FOUR ADD-only FILES
NEW FILES = 4
ADDITIONS = 623
DELETIONS = 0
UNAUTHORIZED FILES = 0
POST_MERGE_FILE_BLOB_IDENTITY = 4/4 PASS
STACKED U07 HISTORICAL DOC/CI ANCESTRY = NOT IMPORTED
```

## 5. Post-merge CI evidence boundary

Queried check-runs and legacy commit status **for the actual merge commit SHA** `86e88431...`, rather than substituting pre-merge source check status.

```text
POST_MERGE_CHECK_RUNS = 0 (at verification query time)
POST_MERGE_COMBINED_STATUS = pending
POST_MERGE_LEGACY_STATUS_CONTEXTS = 0
```

**NO new post-merge CI run is evidenced.** This does **not** establish a test failure or mean pre-merge checks failed; it means only commit/tree/file verification is presently complete. Pre-merge source HEAD did have three green GitHub checks, and Tier-0 code/CLI had independent accepted evidence (15 Python tests, six-source actual CLI Oracle byte equality). They cannot be silently relabeled as tests run on the new merge commit. Any policy requiring main-post-merge CI must be checked separately if such runs appear or deliberately executed.

## 6. Formal status and downstream control

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Clean-Main Merge Execution / Post-Merge Verification
= MERGED / COMMIT_TREE_BLOB_INTEGRITY_PASS

MERGED_PR = #313 ONLY
MERGE_METHOD = STANDARD_MERGE_COMMIT
MAIN_NEW_HEAD = 86e8843197091c8c8172b7e4213537a31bdf0654
MERGE_PARENTS = [6d4fd787600e3a57f01f3e17893e6d98893ac546,
                 ff3539acb9c8bb0b798c716fee97c21578595774]
MAIN_DIFF = EXACT_D0_01_TO_04_ADD_ONLY
MAIN_FILE_BLOBS = 4/4 MATCH
POST_MERGE_STRUCTURE_AND_CONTENT_VERIFICATION = PASS
POST_MERGE_CI_RUN = NOT_EVIDENCED / ZERO_CHECK_RUNS

TIER0_IMPLEMENTATION_VERIFICATION = CLOSED / SOURCE_ONLY
TIER0_MAIN_INTEGRATION = COMPLETE / CODE_SCOPE_ONLY
TIER0_INDEPENDENT_SOURCE_ONLY_EVIDENCE = ACCEPTED

TIER1_SPRING_CONTEXT_DB = NOT_AUTHORIZED
BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07_IMPLEMENTATION_READINESS = NOT_READY
U07_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PRODUCTION / PHI / REAL_PATIENT / PROFILE-A = BLOCKED
```

**Next possible step:** separately determine if a dedicated post-merge Tier-0 offline CI validation is necessary. More importantly, resuming Tier-1 or the Foundation audit requires its own previously blocked read-only authority and compatibility remediation; Tier-0 integration alone does not grant those permissions. No additional PR merge authorization follows from this verification.
