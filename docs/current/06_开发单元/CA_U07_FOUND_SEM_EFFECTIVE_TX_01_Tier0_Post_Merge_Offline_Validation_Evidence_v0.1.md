# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Post-Merge Offline Validation and Evidence Closure v0.1

**Date:** 2026-10-08  
**Scope:** local offline source-only regression on actual merged-main files. No Spring application, JVM, DB, clinical runtime, production, PHI, CI workflow edits, or additional merge.  
**Decision:** `PASS / OFFLINE_REEXECUTION_VERIFIED` with separate `POST_MERGE_GITHUB_CI = NOT_EVIDENCED`.

## 1. Immutable authorization and source provenance

- Owner explicitly authorized one and only one standard merge: clean [PR #313](https://github.com/cxjchelsea/AIdoctor/pull/313) into `main`, accepting the independent evidence reviews [PR #310](https://github.com/cxjchelsea/AIdoctor/pull/310) and [PR #314](https://github.com/cxjchelsea/AIdoctor/pull/314).
- Original `main`: `6d4fd787600e3a57f01f3e17893e6d98893ac546`.
- Clean source HEAD: `ff3539acb9c8bb0b798c716fee97c21578595774`.
- **Actual standard two-parent merge commit / current main**: `86e8843197091c8c8172b7e4213537a31bdf0654`.
- Independent previous post-merge structure verification: [PR #317](https://github.com/cxjchelsea/AIdoctor/pull/317). It confirms four ADD-only files, 623 additions, zero deletions, and no imported stacked U07 history.

## 2. Offline re-execution method

Re-read GitHub: main HEAD equals `86e8843197091c8c8172b7e4213537a31bdf0654`, PR #313 reports merged with the same merge SHA. Against this exact merged commit, all three Python file Git Blobs match the previously accepted implementation:

| Path | Required / actual Git Blob |
|---|---|
| `tools/u07_foundation_tx_topology/static_projection.py` | `45fd3b336a7ac8797f19714dca111292aba3840e` |
| `tools/u07_foundation_tx_topology/static_schema.py` | `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` |
| `tools/u07_foundation_tx_topology/test_static_projection.py` | `36731fadf8c9c574b326549084a29d19f2249478` |

Extracted the already independently attested successful six-source ZIP into a **separate offline directory**, checked Python Git Blob identities again using the Git `blob <length>\0<bytes>` algorithm, and re-ran **the actual producer** using the genuine frozen archive and all six original Java source snapshots.

Commands (isolated filesystem, stdlib only):

```bash
python python/static_projection.py \
  --archive inputs/u07-foundation-exact-head-inventory.zip \
  --snapshots-dir java-snapshots \
  --artifact-dir postmerge-validation

python -m unittest discover -s python -p 'test_*.py' -v
```

A new process ran both commands and captured their stdout/stderr. This is an **offline local validation** of Git Blob-identical merged code, **not a CI run executed on GitHub's merge SHA**.

## 3. New observed execution results

```text
merged_exact_python_git_blobs = 3/3 VERIFIED
trusted_original_scan_archive_sha256 =
  0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089
actual_cli_exit_code = 0
actual_projection_sha256 =
  2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9
actual_result_byte_equal_to_independent_oracle = TRUE
offline_python_unittest_exit_code = 0
offline_python_test_count = 15
verification_result_json_sha256 =
  309a3a4d2365651f5609f079f4fa9d427c836443f8e2d14d675a3e9ec8ab881b
```

This reproduces the previously independently accepted Tier-0 `SOURCE_ONLY` output. It does not establish any live Spring manager, effective transactional advice, EntityManagerFactory, DataSource, JDBC enlistment, COMMIT or physical uniqueness behavior.

## 4. Evidence bundle

Portable local evidence bundle: `U07_Tier0_Main_PostMerge_Offline_Verification_Evidence.zip`.

```text
bundle_sha256 = 4252487fc8d59c1940a51a15f0678c5e01ea3cd4d90b722d6c805bc4f99eaa7f
bundle_bytes = 190475
bundle_members = 22
```

Contains all six original snapshot sources, three Git-Blob-confirmed Python sources, genuine original authority ZIP, prior independently authored Oracle, actual newly re-generated `tier0-source-projection.json`, original reference evidence, new CLI stdout/stderr, new unittest stdout/stderr and `verification-result.json`. This bundle is evidence from the isolated environment, not a committed repository artifact.

## 5. GitHub post-merge CI observation

Queried exact merged `main@86e8843197091c8c8172b7e4213537a31bdf0654`:

```text
GitHub check runs = 0
legacy combined commit status = pending
legacy status contexts = 0
```

**No post-merge GitHub CI result is evidenced.** Do not re-label pre-merge `u02-java`, `u03-java` or `u02-python` checks as tests on the merge commit. This run established the offline Tier-0 source-only regression result; it did **not** install or enable a new GitHub Actions workflow. A future repository CI baseline, if required, needs separate design and explicit authorization.

## 6. Formal status

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Post-Merge Offline Validation
= PASS / VERIFIED_SOURCE_ONLY

MAIN_HEAD = 86e8843197091c8c8172b7e4213537a31bdf0654
MERGED_CANDIDATE_PR = #313
MERGE_METHOD = STANDARD_MERGE_COMMIT
MERGED_D0_FILES = EXACT_4_ADD_ONLY
MERGED_PYTHON_GIT_BLOBS = 3/3 VERIFIED
ACTUAL_OFFLINE_PROJECTION = PASS / ORACLE_BYTE_EQUAL
ACTUAL_OFFLINE_UNIT_TESTS = 15 PASSED
POST_MERGE_GITHUB_CI = NOT_EVIDENCED

TIER0_MAIN_CODE_INTEGRATION = COMPLETE
TIER0_SOURCE_ONLY_VERIFICATION = CLOSED
NEW_CI_WORKFLOW_AUTHORIZATION = NOT_GRANTED
TIER1_SPRING_CONTEXT / TX_MANAGER / JDBC = NOT_AUTHORIZED

BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07_IMPLEMENTATION_READINESS = NOT_READY
U07_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PROFILE-A / PHI / REAL_PATIENT / PRODUCTION = BLOCKED
```

**Next controlled decision:** whether to commission a new *authorized* post-merge GitHub Actions workflow for the bounded Tier-0 offline test, or return to unresolved Foundation audit/Tier-1 authority blockers. Neither is permitted automatically by this evidence record. This evidence PR stays Draft; do not merge without separate authorization.
