# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Exact Main-Target Merge Authorization Decision v0.1

> Decision date: 2026-10-08
> **Decision: AUTHORIZE_CONDITIONAL_EXACT_MAIN_TARGET_ONLY / MERGE_EXECUTION_NOT_AUTHORIZED_YET.**
> Reviewed **clean** main-based candidate [PR #313](https://github.com/cxjchelsea/AIdoctor/pull/313), exact source HEAD `ff3539acb9c8bb0b798c716fee97c21578595774`, literal target `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`.
> Independent design + exact-diff review [PR #314](https://github.com/cxjchelsea/AIdoctor/pull/314) HEAD `74955bae18b75e51ec5fb64b28c101461aab9ddf` = PASS.
> Original implementation [PR #305](https://github.com/cxjchelsea/AIdoctor/pull/305) exact HEAD `2ca948009281ed6bf5f346d8c382291af96a7934`; original six-source evidence [PR #309](https://github.com/cxjchelsea/AIdoctor/pull/309); independent evidence acceptance [PR #310](https://github.com/cxjchelsea/AIdoctor/pull/310); Tier-0 verification closure [PR #311](https://github.com/cxjchelsea/AIdoctor/pull/311).
>
> This authorization is limited to proposing the **four unchanged source-only D0 files** for main integration. It is **not** a direction or authority to execute a GitHub merge immediately. The original stacked [PR #305](https://github.com/cxjchelsea/AIdoctor/pull/305) is **explicitly not authorized** for direct merging to main.

## 1. Exact main/source identity and main-only diff

Queried live GitHub branch/PR state at decision time:
- `main` = `6d4fd787600e3a57f01f3e17893e6d98893ac546`; branch protected flag **false**, required status-check enforcement **off**. This technical lack of protection **does not waive** this project's independent authorization discipline.
- PR #313 target is literal `main` at the above SHA; source exact `ff3539acb9c8bb0b798c716fee97c21578595774`, `mergeable=true`, `draft=true`, `merged=false`.
- Relative `main..candidate` = **four commits ahead, zero behind**, **exactly four ADDED files**, **623 additions, zero deletions**, with **zero** unapproved paths.
- Every new Git Blob exactly equals already verified PR #305 original implementation:

| D0 | Path | Accepted source / main-based candidate exact Git Blob |
|---|---|---|
| D0-01 | `tools/u07_foundation_tx_topology/static_projection.py` | `45fd3b336a7ac8797f19714dca111292aba3840e` |
| D0-02 | `tools/u07_foundation_tx_topology/static_schema.py` | `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` |
| D0-03 | `tools/u07_foundation_tx_topology/test_static_projection.py` | `36731fadf8c9c574b326549084a29d19f2249478` |
| D0-04 | `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` | `98189638619aa78e997a1a588239b36b89617378` |

This is a **different and narrower** proposal than directly merging stacked PR #305 to main (37 files, 71 commits). No legacy U07 draft documentation, scan CI workflows, Spring configs, runtime code or Java files are introduced.

## 2. Candidate current checks and review state (authoritative observation)

Read the actual check-run status for candidate source HEAD from GitHub:

| Check-run | Status | Conclusion |
|---|---|---|
| `u02-java` | COMPLETED | SUCCESS |
| `u03-java` | COMPLETED | SUCCESS |
| `u02-python` | COMPLETED | SUCCESS |

Total check runs = 3, all SUCCESS. Combined legacy commit status reported `pending` with **zero** status contexts; do not misinterpret this as a failing required check. Branch protection is **disabled** with **no configured required contexts**.

At decision time GitHub returned `reviews=[]` for PR #313. The PR is still **Draft**. No explicit independently submitted GitHub PR approval is claimed; prior documentary review PR #314 is an accepted exact-diff/design input, **not** a GitHub approval of PR #313 itself.

Because these process gates remain unresolved, the decision is intentionally **CONDITIONAL**, despite the exact diff and three check runs passing.

## 3. Prior accepted content evidence and what it does not prove

- Tier-0 implementation verification **CLOSED / SOURCE_ONLY** in PR #311 at its pinned implementation/evidence heads.
- Prior six complete Java snapshots and three Python source blobs verified; full offline CLI ran successfully and generated JSON SHA256 `2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9`, **byte identical** to independently frozen Oracle.
- Independent PR #310 independently reproduced actual CLI result and passed all 15 offline Python unit tests.
- Since PR #313 contains byte-identical D0 files, these are valid **source-content evidence inputs**. This decision does **not** assert that a fresh six-source CLI or 15-test job was run during this specific merge authorization review, nor does a check-run for U02/U03 substitute for a specific Tier-0 test.

Crucially, source-level `@Transactional` declaration evidence is **not** proof of actual Spring proxy resolution, `PlatformTransactionManager`, effective EMF/DS, physical transaction, UNIQUE collision or COMMIT. Future U07 services remain NOT_IMPLEMENTED.

## 4. Explicit main-target merge gates

| Gate | Requirement | Status |
|---|---|---|
| `MM-T0-01` | source exact `ff3539acb9c8bb0b798c716fee97c21578595774` and target exact `main@6d4fd787600e3a57f01f3e17893e6d98893ac546` | **PASS / NOW** |
| `MM-T0-02` | exclusive D0-01..04 ADD-only diff, four original source blobs byte-identical | **PASS** |
| `MM-T0-03` | independent Tier-0 implementation verification closure and exact-main design review | **PASS** |
| `MM-T0-04` | mergeability and absence of history/other-file contamination | **PASS / GitHub mergeable=true** |
| `MM-T0-05` | candidate completed checks | **PASS / three green checks**, no configured required checks |
| `MM-T0-06` | Draft converted to ready; required code-review/owner readiness confirmed | **PENDING / DRAFT / zero PR reviews** |
| `MM-T0-07` | final independent pre-merge exact SHA/base/diff/check rerun at execution time | **PENDING / MUST RECHECK** |
| `MM-T0-08` | explicit user-owner authorization to **execute** main merge of precisely PR #313 (separate action) | **NOT_GRANTED** |
| `MM-T0-09` | if finally approved, GitHub **standard merge commit** only; never squash/rebase; preserve closure references | **REQUIRED / NOT_EXECUTED** |
| `MM-T0-10` | post-merge main HEAD and exact tree/file/evidence baseline verification | **NOT_EXECUTED** |

**Main-target authorization judgment:** `AUTHORIZE_CONDITIONAL_EXACT_MAIN_TARGET_ONLY` for PR #313, effective only for the frozen source and target HEAD. It is **not yet authorized for merge execution**; any HEAD drift, unexpected change/check failure, or new dependent file voids this decision and requires re-evaluation.

## 5. Restricted scope and baseline after eventual integration

The four-file D0 Tier-0 producer is an offline, source-only diagnostic artifact. Its original evidence ZIP and six pinned Java snapshots are **external explicit offline inputs**, not a bundled live application integration. The clean PR #313 intentionally does not bring in the old Foundation inventory CI workflow.

This conditional authorization does **not** change:
```text
BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED

TIER1_ISOLATED_SPRING_CONTEXT = NOT_AUTHORIZED
EFFECTIVE_TX_MANAGER / EMF / DATASOURCE = UNKNOWN
PHYSICAL_UNIQUENESS / COMMIT = NOT_PROVEN

U07_IMPLEMENTATION_READINESS = NOT_READY
U07_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PROFILE_A / PHI / REAL_PATIENT / PRODUCTION = BLOCKED
```

## 6. Formal Merge Authorization Decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Exact Main-Target Merge Authorization Decision
= AUTHORIZE_CONDITIONAL_EXACT_MAIN_TARGET_ONLY

TARGET_PR = #313
TARGET_BRANCH = main
TARGET_BASE_HEAD = 6d4fd787600e3a57f01f3e17893e6d98893ac546
SOURCE_HEAD = ff3539acb9c8bb0b798c716fee97c21578595774
AUTHORIZED_DIFF = FOUR_ADD_ONLY_D0_FILES
SOURCE_BLOB_MATCH = 4/4 PASS
TECHNICAL_MERGEABILITY = TRUE
GITHUB_CHECK_RUNS = 3 SUCCESS
BRANCH_PROTECTION = DISABLED

PR_DRAFT = TRUE
FORMAL_PR_APPROVALS = 0
OWNER_MERGE_EXECUTION_AUTHORIZATION = NOT_GRANTED
MERGE_EXECUTION = NOT_EXECUTED
MAIN_INTEGRATION = NOT_COMPLETE

TIER0_IMPLEMENTATION_VERIFICATION = CLOSED / SOURCE_ONLY
TIER0_INDEPENDENT_EVIDENCE = ACCEPTED
TIER1 = NOT_AUTHORIZED
FOUNDATION_AUDIT_GATE = NOT_PASSED
U07_IMPLEMENTATION_READINESS = NOT_READY
U07_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PRODUCTION_AND_PHI = BLOCKED
```

**Next controlled step:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Exact-Main Final Merge Readiness + Execution Authorization`. First ensure PR #313 is ready and required reviews are met, recheck source/target exact HEADs and four-file compare, then obtain explicit final user-owner approval to execute a standard merge commit. Only after that approval may the merge action occur, with a separate post-merge verification. Do not automatically merge the current Draft.
