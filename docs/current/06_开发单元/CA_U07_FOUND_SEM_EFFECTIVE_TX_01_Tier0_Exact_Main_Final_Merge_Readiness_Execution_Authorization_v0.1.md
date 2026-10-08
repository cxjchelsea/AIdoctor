# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Exact-Main Final Merge Readiness + Execution Authorization Decision v0.1

> Date: 2026-10-08. Decision stage: **final readiness assessment**, **NOT merge execution**.
> Target: **clean source-only** [PR #313](https://github.com/cxjchelsea/AIdoctor/pull/313) → `main`.
> Candidate source exact HEAD: `ff3539acb9c8bb0b798c716fee97c21578595774`.
> Main target exact HEAD: `6d4fd787600e3a57f01f3e17893e6d98893ac546`.
> Prior exact-target conditional authorization [PR #315](https://github.com/cxjchelsea/AIdoctor/pull/315): `cb825601e4180f421905a9180aad927a8990c4a6`.
> Previous clean main exact-diff independent design review [PR #314](https://github.com/cxjchelsea/AIdoctor/pull/314): `74955bae18b75e51ec5fb64b28c101461aab9ddf`.
>
> **DECISION = FINAL_READINESS_PASS_WITH_OWNER_EXECUTION_AND_FORMAL_REVIEW_PENDING / MERGE_EXECUTION_NOT_GRANTED.**
> PR #313 is now READY FOR REVIEW (not Draft). This document authorizes no GitHub merge action; explicit user-owner execution approval and pre-merge recheck remain required.

## 1. Exact target and diff re-verification

At this assessment, GitHub confirmed:
- `main` remains at `6d4fd787600e3a57f01f3e17893e6d98893ac546`.
- PR #313 source remains `ff3539acb9c8bb0b798c716fee97c21578595774`; target is literal `main` at the pinned base SHA.
- `mergeable=true`; no detected GitHub merge conflict.
- `main` branch protection reports disabled and required-status-check enforcement off. **This does not waive project policy**.
- GitHub compare `main..candidate`: **4 commits ahead / 0 behind; precisely four new files, 623 additions / 0 deletions; no out-of-scope files**.
- Four candidate Git blobs are **identical** to the already independently verified original PR #305 implementation at `2ca948009281ed6bf5f346d8c382291af96a7934`:

| Allowed file | Exact source/candidate Git Blob |
|---|---|
| `tools/u07_foundation_tx_topology/static_projection.py` | `45fd3b336a7ac8797f19714dca111292aba3840e` |
| `tools/u07_foundation_tx_topology/static_schema.py` | `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` |
| `tools/u07_foundation_tx_topology/test_static_projection.py` | `36731fadf8c9c574b326549084a29d19f2249478` |
| `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` | `98189638619aa78e997a1a588239b36b89617378` |

**NOT PERMITTED**: original stacked PR #305 → main (37 files / 71 commits), or any new added design, CI, clinical, Spring, database, or infrastructure file. Main-target scope only covers these four D0 files.

## 2. Checks, review and Draft readiness

Candidate PR #313 source exact HEAD has three GitHub check runs, all completed **SUCCESS**:

- `u02-java` = SUCCESS.
- `u03-java` = SUCCESS.
- `u02-python` = SUCCESS.

The legacy combined commit status is `pending` with zero status contexts; there are **no required status checks configured**, so this aggregate value is not proof of a failed required gate. It does **not** replace explicit Tier-0 verification.

**Readiness action actually performed:** PR #313 was transitioned via GitHub from `Draft=true` to **`Draft=false` / Ready for review**. Re-read of PR #313 confirmed source/target exact SHAs unchanged, `mergeable=true`, `merged=false`.

At completion, `list_pull_request_reviews(PR #313)` returned **zero formal GitHub PR reviews**. The documentary independent design review in PR #314, independent producer/evidence re-review PR #310 and Tier-0 closure PR #311 are accepted evidence but are **not** represented as a formal GitHub approval of PR #313. Since branch protection is disabled, GitHub might permit merging technically, but **project governance requires final owner review/acceptance**; this decision does not falsely claim a formal review occurred.

## 3. Previously accepted Tier-0 verification

Content-specific evidence is unchanged and remains reusable because all four Git blobs match:
- Full six original pinned `main@6d4fd787...` Java source snapshots, source SHA256 and Git blob verified.
- Original evidence ZIP and embedded JSON hashes verified independently.
- Actual offline six-source producer CLI executed successfully, generated `SOURCE_ONLY` JSON SHA256 `2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9`, **byte-for-byte equal** to frozen independent Oracle.
- Exact code offline unittest 15/15 PASS; independent evidence/implementation re-review PR #310 PASS.
- Tier-0 Verification Closure PR #311 = `CLOSED / SOURCE_ONLY`.

**No new code or source-evidence test run is claimed by this final merge readiness assessment**. The three U02/U03 check runs cannot be relabeled as a dedicated Tier-0 CI run.

## 4. Final exact-main merge gates

| Gate | Required condition | State |
|---|---|---|
| `FM-T0-01` | Source PR #313 exact HEAD `ff3539ac...`, main exact `6d4fd787...` | PASS / rechecked |
| `FM-T0-02` | Four ADD-only D0 files, no hidden prior U07 ancestry | PASS |
| `FM-T0-03` | Four exact candidate blobs identical to closed original implementation | PASS |
| `FM-T0-04` | Independent closure / exact-main scope review / conditional authorization preserved | PASS |
| `FM-T0-05` | GitHub mergeable and observed checks green | PASS / 3 completed checks |
| `FM-T0-06` | PR no longer Draft | **PASS / performed** |
| `FM-T0-07` | Formal PR reviewer approval, or explicit user-owner acceptance of documentary review with policy-compatible exception | **PENDING / 0 formal PR reviews** |
| `FM-T0-08` | Explicit user-owner instruction to actually merge **PR #313 into main**, standard merge commit only | **NOT_GRANTED** |
| `FM-T0-09` | Fresh source, main, exact four-file diff, CI and mergeability verification immediately before execution | **PENDING / MANDATORY AT EXECUTION** |
| `FM-T0-10` | Actual standard merge commit and post-merge verification (resulting main SHA, 4 files, checks, provenance) | **NOT_EXECUTED** |

Only `FM-T0-07..09` remain preparatory/authorization gates prior to execution; `FM-T0-10` is the later execution/verification gate. No one may construe the original conditional decision or this readiness assessment as authorizing a merge without the owner's explicit request.

## 5. Explicit scope exclusions

The four-file candidate merely makes available an **offline SOURCE_ONLY static producer**, which still requires an external trusted scan ZIP and six exact historical Java source snapshots when run. It contains no Spring Context runtime, no transaction manager truth evidence, no JDBC enlistment/commit/uniqueness proof, and no U07 application implementation.

Unresolved broader issues remain:
```text
BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 IMPLEMENTATION READINESS = NOT_READY
U07 IMPLEMENTATION AUTHORIZATION = NOT_GRANTED

TIER1_SPRING_CONTEXT_DB_PROBE = NOT_AUTHORIZED
PROFILE-A / PHI / REAL_PATIENT / PRODUCTION = BLOCKED
```

## 6. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Exact-Main Final Merge Readiness + Execution Authorization
= READINESS_PASS_WITH_CONDITIONS / EXECUTION_AUTHORIZATION_PENDING

TARGET_PR = #313
SOURCE_HEAD = ff3539acb9c8bb0b798c716fee97c21578595774
TARGET_BRANCH = main
TARGET_HEAD = 6d4fd787600e3a57f01f3e17893e6d98893ac546
EXACT_4_FILE_DIFF = PASS
BLOB_EQUIVALENCE = 4/4 PASS
CHECK_RUNS = 3 SUCCESS
GITHUB_MERGEABLE = TRUE

PR_DRAFT = FALSE / READY_FOR_REVIEW
FORMAL_PR_REVIEW_APPROVAL = MISSING / OWNER_ACCEPTANCE_PENDING
MERGE_EXECUTION_OWNER_AUTHORIZATION = NOT_GRANTED
FRESH_EXECUTION_TIME_RECHECK = REQUIRED
MERGE_EXECUTION = NOT_EXECUTED
MERGE_METHOD_IF_AUTHORIZED = STANDARD_MERGE_COMMIT_ONLY

TIER0_IMPLEMENTATION_VERIFICATION = CLOSED / SOURCE_ONLY
TIER1 = NOT_AUTHORIZED
FOUNDATION_AUDIT_GATE = NOT_PASSED
U07_IMPLEMENTATION_READINESS = NOT_READY
U07_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PRODUCTION / PHI = BLOCKED
```

**Next action:** request a formal GitHub PR #313 review approval or explicitly obtain the user's acceptance of the already documented independent reviews, **and** the user's explicit authorization to execute exactly `PR #313 → main` using a standard merge commit. Immediately before merge, refresh all exact-head/diff/check predicates. If any predicate changes, fail closed and issue a re-evaluation. After merge, run a separate post-merge verification. Do not merge PR #305 or any prior stacked U07 branch.
