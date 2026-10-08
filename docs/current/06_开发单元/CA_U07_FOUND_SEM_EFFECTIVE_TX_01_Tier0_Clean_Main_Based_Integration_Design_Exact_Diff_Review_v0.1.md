# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Clean Main-Based Integration Design + Exact-Diff Review v0.1

> Review date: 2026-10-08
> Main baseline exact HEAD: `6d4fd787600e3a57f01f3e17893e6d98893ac546` (verified current at review).
> Existing original implementation [PR #305](https://github.com/cxjchelsea/AIdoctor/pull/305) exact HEAD: `2ca948009281ed6bf5f346d8c382291af96a7934`.
> Original Tier-0 verification closure [PR #311](https://github.com/cxjchelsea/AIdoctor/pull/311) exact HEAD: `1a283bde92c125eceb8f0dbeff72d0409fe33f56`.
> Conditional **non-main** merge decision [PR #312](https://github.com/cxjchelsea/AIdoctor/pull/312) exact HEAD: `9f813d0899ead09ae1a5133509b3afe310adb871`.
> **Clean main-rooted candidate:** [PR #313](https://github.com/cxjchelsea/AIdoctor/pull/313), source branch `candidate/u07-found-sem-tier0-clean-main-four-file-v1`, exact HEAD `ff3539acb9c8bb0b798c716fee97c21578595774`, base `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`.
>
> **DESIGN + EXACT-DIFF REVIEW = PASS / READY_FOR_SEPARATE_EXACT_MAIN_TARGET_MERGE_AUTHORIZATION_DECISION.**
>
> This is an exact **candidate scope/design review**, not a merger, Merge Authorization, production deployment, Tier-1 setup or U07 implementation permission.

## 1. Why a clean main-based candidate is required

The prior PR #312 intentionally did **not** authorize moving original PR #305's stacked branch directly to main. Independently repeated GitHub comparisons:

| Proposed integration | Relative files | Relative commits | Authority |
|---|---:|---:|---|
| Original PR #305 → current authorized decision base `fd820541` | 4 | 16 | Only four D0 file additions allowed within non-main branch |
| Original PR #305 → `main@6d4fd787` | 37 | 71 | **DENIED** as a direct main merge: carries 33 unrelated changes |
| Clean candidate PR #313 → `main@6d4fd787` | **4** | **4** | **PASS / candidate review only** |

The original cumulative 37 files included U07 design/review documents, an existing-source inventory helper, and `.github/workflows/u07-foundation-exact-head-inventory.yml`. This proposal avoids importing those unrelated historical commits. GitHub `mergeable=true` indicates no detected technical conflict, **not** authorization to merge.

## 2. Exact four-file clean branch plan and implemented candidate

Created the clean candidate branch **directly** from the immutable current main HEAD, not from any U07 stacked branch. Recreated exactly the four previously accepted D0-01..04 files using the original source contents, without edits.

| Item | Path | Original PR #305 Git Blob | Clean PR #313 Git Blob | Result |
|---|---|---|---|---|
| D0-01 | `tools/u07_foundation_tx_topology/static_projection.py` | `45fd3b336a7ac8797f19714dca111292aba3840e` | same | EXACT_BLOB_MATCH |
| D0-02 | `tools/u07_foundation_tx_topology/static_schema.py` | `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` | same | EXACT_BLOB_MATCH |
| D0-03 | `tools/u07_foundation_tx_topology/test_static_projection.py` | `36731fadf8c9c574b326549084a29d19f2249478` | same | EXACT_BLOB_MATCH |
| D0-04 | `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` | `98189638619aa78e997a1a588239b36b89617378` | same | EXACT_BLOB_MATCH |

GitHub exact `main..candidate` comparison:
```text
BASE = 6d4fd787600e3a57f01f3e17893e6d98893ac546
CANDIDATE = ff3539acb9c8bb0b798c716fee97c21578595774
AHEAD = 4
BEHIND = 0
CHANGED_FILES = 4
ADDITIONS = 623
DELETIONS = 0
FILE_STATUSES = 4x ADDED
UNAUTHORIZED_FILES = 0
```

All four paths are absent in current main's non-truncated exact tree. No `java/**`, Maven/POM, Spring configuration, Flyway/schema change, infrastructure, CI workflow or unrelated U07 documents changed. **The clean candidate is now present as a Draft PR against main, but remains unmerged.**

## 3. Valid reuse of already-accepted evidence (and its limitations)

Because every file's *full Git Blob* exactly matches the accepted implementation and the static program consumes separately pinned immutable historical-source inputs, previously accepted *content-specific* evidence can be reused for this candidate:

- Original Tier-0 authorization [PR #304](https://github.com/cxjchelsea/AIdoctor/pull/304): D0-01..04 source-only four-file scope.
- Original actual-six-source CLI evidence [PR #309](https://github.com/cxjchelsea/AIdoctor/pull/309): source `main@6d4fd787`, original scan ZIP/JSON SHA pinned, actual CLI exit `0`, actual JSON SHA256 `2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9`, byte-for-byte equal to independent Oracle.
- Independent implementation/evidence review [PR #310](https://github.com/cxjchelsea/AIdoctor/pull/310): **PASS**, including independent actual CLI repeat and **15** passing offline Python tests.
- Formal Tier-0 verification closure [PR #311](https://github.com/cxjchelsea/AIdoctor/pull/311): **AUTHORIZE_TIER0_IMPLEMENTATION_VERIFICATION_CLOSURE / CLOSED_SOURCE_ONLY**.

**This clean-branch exact-diff review does not claim to have run a new independent unit-test job or CLI job on PR #313.** The scope of reused evidence is byte-identical static Python source and the historically pinned original source inventory. A future exact-target main merge authorization may separately require a clean-main candidate CI check or smoke test under local offline test policy.

The source-only producer requires the **external, previously pinned** original inventory ZIP plus six exact Java file snapshots when executed. These files are not automatically present in main, and this PR does not add the old U07 inventory CI workflow. No startup/deployment claims follow from adding the producer code alone.

## 4. Exact main-integration design invariants

1. **Merge target is literal main**, and base must remain pinned to current `main@6d4fd787600e3a57f01f3e17893e6d98893ac546` until final authorization and immediate execution-time recheck.
2. **Source candidate HEAD is literal `ff3539acb9c8bb0b798c716fee97c21578595774`**; changes to source code or branch revision void this candidate review pending fresh SHA/impact checks.
3. Changeset exclusively contains the four ADD-only D0-01..04 paths; the independent source PR #305 blobs must continue to match all candidate file blobs exactly.
4. Do **not** merge original PR #305 to main or retarget that 71-commit ancestry; the new candidate PR #313 is the **only reviewed clean-main proposal**.
5. Keep the original immutable source SHA and accepted scan artifact hash as first-class inputs; no implicit `latest`, alternate source head, regenerated synthetic authority artifact or PII data allowed.
6. Never claim static SOURCE_ONLY evidence proves Spring transaction manager, EMF/DS, JDBC enlistment, physical COMMIT, Foundation audit gate, actual U07 admission or deployment.
7. No Tier-1 Java harness, Spring Context, runtime DB/Redis/Nacos/Oracle, CI activation, migrations or production edits.
8. Do not squash/rebase; if actual merge is later authorized, use **standard merge commit only**. Preserve the complete approval/provenance chain.

## 5. Main-target authorization gates — no automatic merge

| Gate | Mandatory check | Result at this review |
|---|---|---|
| `CM-T0-01` | main exact base HEAD pinned and candidate root derives solely from it | **PASS** |
| `CM-T0-02` | exactly four ADD-only files, 0 deletions or unauthorized edits | **PASS** |
| `CM-T0-03` | all four original closed-implementation blobs equal candidate blobs | **PASS** |
| `CM-T0-04` | prior Tier-0 verification closure and independent evidence accepted for exact code | **PASS** |
| `CM-T0-05` | no UI/production/runtime/CI and no additional stacked history | **PASS** |
| `CM-T0-06` | independent clean-main target-specific Merge Authorization Decision | **NOT_GRANTED / NEXT GATE** |
| `CM-T0-07` | draft→ready, required reviewers/checks, branch protection compliance | **PENDING** |
| `CM-T0-08` | refresh current main HEAD and candidate HEAD, exact diff/mergeability immediately before merge | **PENDING** |
| `CM-T0-09` | explicit owner instruction to execute standard merge commit into main | **NOT_GRANTED** |
| `CM-T0-10` | actual merge execution, post-merge verification and evidence chain | **NOT_EXECUTED** |

**Decision:** `PASS / READY_FOR_SEPARATE_EXACT_MAIN_TARGET_MERGE_AUTHORIZATION_DECISION`. This is not `MAIN_MERGE_AUTHORIZED` and not `MAIN_MERGED`.

## 6. Wider U07 state is unchanged

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Clean Main-Based Integration Design + Exact-Diff Review
= PASS / READY_FOR_MAIN_TARGET_MERGE_AUTHORIZATION_REVIEW

CANDIDATE_PR = #313 / DRAFT / NOT_MERGED
MAIN_BASE_HEAD = 6d4fd787600e3a57f01f3e17893e6d98893ac546
CANDIDATE_HEAD = ff3539acb9c8bb0b798c716fee97c21578595774
CANDIDATE_DIFF = EXACT_FOUR_ADD_ONLY_FILES
CANDIDATE_FILE_BLOB_EQUIVALENCE = 4/4 PASS
EXTRA_STACKED_HISTORY = NONE
MAIN_MERGE_AUTHORIZATION = NOT_GRANTED
MAIN_MERGE_EXECUTION = NOT_EXECUTED

TIER0_IMPLEMENTATION_VERIFICATION = CLOSED / SOURCE_ONLY
TIER0_INDEPENDENT_SOURCE_ONLY_EVIDENCE = ACCEPTED
TIER1_SPRING_CONTEXT_OR_DB = NOT_AUTHORIZED
EFFECTIVE_SPRING_MANAGER_TOPOLOGY = NOT_PROVEN

BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07_IMPLEMENTATION_READINESS = NOT_READY
U07_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PROFILE_A / PHI / REAL_PATIENT / PRODUCTION = BLOCKED
```

**Next controlled step:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Exact Main-Target Merge Authorization Decision` based specifically on candidate **PR #313** and `main`, not on the conditional non-main PR #312 decision. Then require explicit final owner merge execution approval and renewed exact-head checks before any merge. No merge has occurred.
