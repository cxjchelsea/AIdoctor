# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Merge Authorization Decision v0.1

> Decision date: 2026-10-08
> Decision input: [PR #311](https://github.com/cxjchelsea/AIdoctor/pull/311) Tier-0 Implementation Verification Closure @ `1a283bde92c125eceb8f0dbeff72d0409fe33f56`.
> Original implementation [PR #305](https://github.com/cxjchelsea/AIdoctor/pull/305) exact HEAD `2ca948009281ed6bf5f346d8c382291af96a7934`; existing PR base `decision/u07-found-sem-effective-tx-tier0-limited-authorization-v1` exact HEAD `fd820541a6a94993c7e698661d7d01c3fd5f74de`.
> Independent evidence [PR #309](https://github.com/cxjchelsea/AIdoctor/pull/309) @ `5619d17b43b3234524743578501881fd863410d2`; independent review [PR #310](https://github.com/cxjchelsea/AIdoctor/pull/310) @ `9dd2020166de800d7a0c8cb4330968d297a058dc`.
> Main `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`.
> **FORMAL DECISION = AUTHORIZE_CONDITIONAL_EXACT_TARGET_BRANCH_ONLY.** Authorization is **limited exclusively** to merging PR #305 exact implementation HEAD into its **existing authorized base branch**, **not into main**. No merge action is executed by this decision.
>
> PR #305 is still **DRAFT**. A later actual merge is blocked pending explicit final owner approval and branch/PR readiness checks. This record is **not** an instruction to merge immediately.

## 1. Why merge target identity is part of the authorization

Exact GitHub compare shows two very different changesets:

| Proposed merge relation | Source/target HEADs | Actual cumulative changed files | Decision |
|---|---|---|---|
| **Authorized candidate:** PR #305 → current PR base `decision/u07-found-sem-effective-tx-tier0-limited-authorization-v1` | source `2ca948009281ed6bf5f346d8c382291af96a7934`; base `fd820541a6a94993c7e698661d7d01c3fd5f74de` | **4 added files**, 0 changes to pre-existing files | **CONDITIONALLY AUTHORIZE / EXACT_HEAD_ONLY** |
| **Explicitly unauthorized:** PR #305 → `main` | source `2ca948009281ed6bf5f346d8c382291af96a7934`; main `6d4fd787600e3a57f01f3e17893e6d98893ac546` | **37 added files**, **71 commits ahead** | **DENY_MAIN_MERGE / OUT_OF_SCOPE** |

The 33 additional `main`-relative files predate PR #305's four-file implementation and include numerous U07 design/review artifacts and `.github/workflows/u07-foundation-exact-head-inventory.yml`. This cumulative ancestry has **not been granted production/main integration authority** by the Tier-0 D0-only decision. GitHub's `mergeable=true` is a technical conflict indicator, **not** proof of an authorized scope.

The existing PR #305 base HEAD and code HEAD have not drifted since closure. All source and review PRs #304/#305/#309/#310/#311 were still unmerged and draft at the time of this decision.

## 2. Exclusive four-file authorized merge scope

The authorized compare `fd820541a6a94993c7e698661d7d01c3fd5f74de..2ca948009281ed6bf5f346d8c382291af96a7934` contains these **ADD-only** paths:

| D0 | Path | Review |
|---|---|---|
| D0-01 | `tools/u07_foundation_tx_topology/static_projection.py` | AUTHORIZED |
| D0-02 | `tools/u07_foundation_tx_topology/static_schema.py` | AUTHORIZED |
| D0-03 | `tools/u07_foundation_tx_topology/test_static_projection.py` | AUTHORIZED |
| D0-04 | `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` | AUTHORIZED |

No Java Runtime, migration, JPA/Flyway, CI, Maven, Spring profiles, scheduler, user data, PHI, external service or Tier-1 files belong in this authorized delta. Any additional diff, different base or later head requires renewed exact-diff approval.

## 3. Implementation verification and evidence closure reused without scope expansion

The independently reviewed evidence for this exact candidate is sufficient for **Tier-0 source-only** integration into its approved branch:

- PR #311 formal Tier-0 Implementation Verification Closure = `AUTHORIZE_TIER0_IMPLEMENTATION_VERIFICATION_CLOSURE / CLOSED_SOURCE_ONLY`.
- Independent PR #310 reviewed and accepted six immutable Java source snapshots, three byte-identical Python producer/test blobs and 15 passing offline tests.
- Actual Python CLI produced `SOURCE_ONLY` JSON; SHA256 `2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9`, byte-for-byte equal to independent frozen Oracle.
- Original scan ZIP and embedded JSON digest authority and fail-closed negative tests accepted; BF-T0-IMPL-01/02 and BF-T0-RE-01 CLOSED, RF-T0-IMPL-01/02 SATISFIED.
- `effective_manager`, `entity_manager_factory`, `datasource` remain **UNKNOWN**; no live `TransactionInterceptor`, DB or physical COMMIT proof.

**No new validation run is claimed** by this merge decision. It consumes already independently accepted, immutable exact-HEAD evidence; the specific merge target delta was independently rechecked via GitHub compare.

## 4. Pre-merge hard gates (required, not silently deemed passed)

| Gate | Constraint | Decision at this time |
|---|---|---|
| `M-T0-01` | Implementation author HEAD exactly `2ca948009281ed6bf5f346d8c382291af96a7934` | PASS / checked |
| `M-T0-02` | PR base branch exactly `decision/u07-found-sem-effective-tx-tier0-limited-authorization-v1`, SHA `fd820541a6a94993c7e698661d7d01c3fd5f74de` | PASS / checked |
| `M-T0-03` | Only the four D0 files present in base-relative GitHub compare | PASS / checked |
| `M-T0-04` | Exact approved closure/evidence heads stable | PASS / checked |
| `M-T0-05` | No unapproved changes to runtime/DB/CI; no false Spring runtime claims | PASS / source review |
| `M-T0-06` | PR converted from DRAFT to ready and required repository checks/reviews satisfied | **PENDING** |
| `M-T0-07` | Fresh exact-head/base/mergeability and actual diff recheck immediately before merge | **PENDING / MUST RECHECK** |
| `M-T0-08` | **Explicit final user-owner authorization to execute** standard merge commit of PR #305 into the existing authorized base | **NOT_GRANTED_BY_THIS_DOCUMENT** |
| `M-T0-09` | Actual merge uses GitHub standard merge commit only; no squash/rebase, no automated downstream merges | **REQUIRED / NOT_EXECUTED** |

Because PR #305 remains Draft and execution authority is separate, the result is **conditional**, not `MERGE_EXECUTED` and not an unrestricted executable approval.

## 5. Do not smuggle parent history into main

The parent authorization branch is a **review/decision integration branch**, not a production mainline release target. Merging #305 there does not land the tool in `main`, and must not be reported as `main` integration.

If the product owner later wants **only the four D0 files on main**, prepare a *new* clean branch rooted in a freshly pinned current `main`, carry over precisely the four independently accepted D0 files (no ancestry of other U07 documentation/CI), recheck source blob equivalence and the new exact four-file compare, and obtain **separate controlled amendment + exact-target Merge Authorization**. Do not silently retarget PR #305 onto main or use its existing 71-commit ancestry for a direct main merge.

## 6. Non-goals and unresolved downstream findings

This Tier-0 conditional merge approval has **no bearing** on:

- Tier-1 isolated Spring context startup, actual transaction manager/EMF/DS resolver, Java/JPA/Spring proxy or JDBC physical behavior (`BF-U07-FOUND-TX-INT-01..03` still OPEN).
- `BF-U07-FOUND-SEM-01/02` (actual Spring transactional consistency and physical uniqueness-collision/commit proof).
- `BF-U07-FOUND-AUD-01..04` and Foundation Reference Audit gate (`NOT_PASSED`).
- The wider U07 implementation; its Readiness = `NOT_READY`, Implementation Authorization = `NOT_GRANTED`.
- Any PHI, real patient, `PROFILE-A`, production or external DB/data/network execution.

No merge, branch retarget, rebase/squash, Spring runtime, CI activation or clinical access was performed.

## 7. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Merge Authorization Decision
= AUTHORIZE_CONDITIONAL_EXACT_TARGET_BRANCH_ONLY

AUTHORITY_SCOPE = D0-01..04 / SOURCE_ONLY
EXACT_IMPLEMENTATION_PR = #305
SOURCE_HEAD = 2ca948009281ed6bf5f346d8c382291af96a7934
AUTHORIZED_TARGET_BRANCH = decision/u07-found-sem-effective-tx-tier0-limited-authorization-v1
AUTHORIZED_TARGET_HEAD = fd820541a6a94993c7e698661d7d01c3fd5f74de
AUTHORIZED_DIFF = 4 ADD_ONLY_FILES
AUTHOR_IMPLEMENTATION_VERIFICATION = CLOSED / SOURCE_ONLY
INDEPENDENT_EVIDENCE = ACCEPTED

PREMERGE_PR_READY = PENDING / DRAFT
FRESH_PREMERGE_EXACT_HEAD_CHECK = PENDING
FINAL_MERGE_EXECUTION_APPROVAL = NOT_GRANTED
MERGE_EXECUTION = NOT_EXECUTED
MERGE_METHOD_IF_LATER_APPROVED = STANDARD_MERGE_COMMIT

DIRECT_MAIN_MERGE_AUTHORIZATION = DENIED / OUT_OF_SCOPE
DIRECT_MAIN_CUMULATIVE_DIFF = 37 FILES / 71 COMMITS
MAIN_INTEGRATION = NOT_EXECUTED

TIER1 = NOT_AUTHORIZED
BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07_IMPLEMENTATION_READINESS = NOT_READY
U07_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PROFILE_A / PHI / REAL_PATIENT / PRODUCTION = BLOCKED
```

**Next controlled action:** complete PR readiness/required reviews, independently recheck `M-T0-06..09` and request an **explicit final merge execution approval** for the exact PR #305 source/target, if integration into its decision branch is wanted. For mainline integration, a separate clean exact-four-file main-based proposal and authorization is mandatory.
