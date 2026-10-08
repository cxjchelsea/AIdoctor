# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Independent Implementation / Evidence Review v0.1

> Review date: 2026-10-08
> **Author candidate PR:** [#305](https://github.com/cxjchelsea/AIdoctor/pull/305)
> **Exact reviewed candidate HEAD:** `186502f0a2ce4fbc2f96a54c41799a1959e68f5a`
> **Authorization basis:** [PR #304](https://github.com/cxjchelsea/AIdoctor/pull/304), `fd820541a6a94993c7e698661d7d01c3fd5f74de`
> Source of archival lexical evidence: actual main `6d4fd787600e3a57f01f3e17893e6d98893ac546`, tree `1bfe776f76c986d4e6199a9b820f2cc20773a181`, CI [run 37736999966](https://github.com/cxjchelsea/AIdoctor/actions/runs/37736999966), artifact `11531528615`.
> **VERDICT: REVISE_REQUIRED / TWO IMPLEMENTATION BLOCKERS + TWO REQUIRED EVIDENCE FIXES.**
> Independent review only; no changes to author PR, no Tier-1, Spring boot, JDBC/MySQL/Oracle, patient/PHI/production, or merge.

## 1. Exact-diff and inspected source

GitHub compare against the authorization HEAD confirms precisely **four new files, 453 additions, zero deletions**, and no changes to existing code:
- `tools/u07_foundation_tx_topology/static_schema.py` — blob `d282c765880a8d5c287925b76114dd12109f15cd`.
- `tools/u07_foundation_tx_topology/static_projection.py` — blob `f5fc23621d2c67861a8c5d7fe76006d13123c8b4`.
- `tools/u07_foundation_tx_topology/test_static_projection.py` — blob `c5bbf00c9204dfa00430e1361bc065d5bfe8c9b9`.
- `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` — blob `e42155de32a696602dbb8d2e894e32c5489de5e3`.

**D0-01..04 exact-diff authorization boundary PASS.** The implementation is source-only, uses Python standard libraries, explicitly returns `SOURCE_ONLY`/`UNKNOWN`/`NOT_IMPLEMENTED`, and does not import Spring, Java, JDBC, socket or HTTP client modules. It does **not** claim actual Spring `TransactionInterceptor`/Manager/EntityManager/DataSource/COMMIT evidence.

Read the exact author files via GitHub. The local review environment also contains a **byte-identical copy of `static_schema.py` (verified Git blob SHA)** and an independently downloaded copy of the original CI scan ZIP. Other pre-existing local producer/test copies have **different Git blob SHAs** from author PR #305; their local 9-test PASS is **not accepted as an exact-HEAD execution result**. An exact-author-HEAD test runner log, including the file SHAs, remains required.

## 2. BF-U07-FOUND-T0-IMPL-01 — authentic scan artifact binding is not enforced

**OPEN / BLOCKER / UNTRUSTED_INVENTORY_PROMOTION.**

`static_schema.verify_inventory()` validates a JSON object's self-declared `source_head`, `tree_sha`, number of files, split of text/binary counts and match count. It does not check the **authenticated original producer artifact/JSON expected SHA256** from independently accepted PR #298; `static_projection.project()` merely hashes the caller-supplied ZIP/JSON **after reading** and records those digests as output. Thus a modified inventory can be accepted as authentic evidence if the attacker preserves expected dimensions and a few internally consistent values. A declared Git tree SHA is not proof that every entry is actually in that Git tree.

**Actual independent negative check** using `u07-foundation-exact-head-inventory.zip` from the previous CI run and the exact `static_schema.py` implementation:
```text
ACTUAL_PROVENANCE:
  head=6d4fd787600e3a57f01f3e17893e6d98893ac546
  tree=1bfe776f76c986d4e6199a9b820f2cc20773a181
  files=2112, matches=256
ACTUAL_SCHEMA: accepted 2112
FORGED_UNRELATED_FILE_DIGEST: ACCEPTED   (should reject)
FORGED_TRACKED_FILE_PATH: ACCEPTED       (should reject)
```
The two spoofed examples alter a tracked-file entry not used by any fixed six-source snapshot; the input count and primary header stay unchanged. This is not a claim of a GitHub CI compromise: it demonstrates that the implementation's internal verifier lacks an independent trust anchor.

**Required correction:** accept the **independently frozen original archive SHA-256** `0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089` and JSON SHA-256 `b28d72868833a8c109938025a6077dd6799924e89e31a34572c4d186f5bcf077`, or an equally authoritative signed immutable exact-artifact manifest whose trust root is outside the candidate input; verify **before parsing or emitting provenance**. Provide a negative test with a re-packed internally plausible archive where only an unreferenced file's digest/path changes; require `FAIL_CLOSED` and no output. Source snapshot SHA/Blob checks then remain an independent second layer. Any need for new external metadata files must undergo **exact-diff scope reauthorization**; prefer code/constants within existing D0 paths plus D0-04 manifest.

## 3. BF-U07-FOUND-T0-IMPL-02 — negative edge tests are masked by source hash rejection

**OPEN / BLOCKER / UNPROVEN_SEMANTIC_FAIL_CLOSED.**

`test_missing_or_duplicate_edge_fails_closed()` modifies `CDPManager` source bytes in the fixture and asserts only that `project()` raises `EvidenceError`. Because `project()` calls `verify_source_snapshot` **before** running the edge detector, both missing-call and duplicate-call mutations are already rejected by the unchanged fixture's SHA-256. The test may pass even if the edge detector is removed or broken. The same conflation affects confidence in other checks that change source but never update the declared independent inventory.

**Required correction:** build fully *self-consistent synthetic* fixture inventory+source for each missing/duplicate edge case, update the matching path SHA-256 and Git blob metadata and repack synthetic ZIP, then assert the **specific edge error** not a generic hash mismatch. Negative tests must independently cover both hash-failure and semantic duplicate/missing-edge failure. Also test `_member_tx` declared-annotation parsing separately from source authenticity and verify that an annotation appearing only in comments/string literals is not counted as a declaration (or conservatively returns UNKNOWN).

## 4. Required evidence and robustness findings

### RF-U07-FOUND-T0-IMPL-01 — exact author HEAD test output missing

Previous implementation message reported nine local synthetic tests passed, but no attached CI or independently verified execution of **the exact three source blobs above** exists in PR #305. In this review, the local `static_schema.py` SHA matched GitHub author blob, while the local producer/test SHA differed from GitHub author blobs, so a local 9/9 result is **not** accepted as test verification of PR #305. Re-run exact-author-head tests (using a traceable checkout or independently hash-reconciled source bundle), record Python version, exact file SHAs, test IDs, stdout, no-I/O scope and artifact digests; do not equate a prior in-progress local worktree with the pushed candidate.

### RF-U07-FOUND-T0-IMPL-02 — complete real six-source projection and safe output ownership missing

The D0-04 manifest explicitly records `Tier0 exact-main full six-snapshot projection = NOT_EXECUTED`. A positive actual source projection requires six hash-verified immutable `main` Java snapshots and the independently frozen original CI ZIP, then a real `SOURCE_ONLY` JSON output SHA and independent graph/oracle reconciliation.

`static_projection.main()` refuses existing outputs and some input/producer ancestry, but creates `output.parent` and writes any otherwise-new caller-selected location. It does not positively enforce the decision's requirement of an **isolated caller-specified artifact directory**, nor guard against a symlink chain to the tracked source tree or unapproved filesystem target. A proposed future test should assert an allowed output-root boundary and reject source-tree paths and symlink escapes. If this requires a new config/artifact file, request exact-diff amendment; otherwise correct under D0-01..03.

## 5. Accepted protections and limits retained

- No Tier-1 `ApplicationContext`, Spring, Java or database calls in reviewed static producer.
- Exact-path six-source snapshot requirement and source SHA-256 / computed Git blob checks are useful second-layer protections once original archive is independently authenticated.
- Graph includes source-declared U01→Ledger, U01→CDP→CDPVersion, U01→RuntimeBinding, U01→ConsultationRepository.save and U01→ClinicalRun. The actual main `CDPManager` and `CDPVersionService` source declarations were independently inspected and contain the proposed method call.
- Future U07 beans are `NOT_IMPLEMENTED`; source graph is never declared to be Spring effective transaction topology.
- No unapproved changes to production Java, `application.yml`, Flyway migrations, CI workflow or Maven dependencies.

## 6. Formal review verdict

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Independent Implementation / Evidence Review
= REVISE_REQUIRED

Exact author HEAD = 186502f0a2ce4fbc2f96a54c41799a1959e68f5a
Exact-diff D0-01..04 = PASS / NO UNAUTHORIZED FILES

BF-U07-FOUND-T0-IMPL-01 = OPEN / UNTRUSTED_ORIGINAL_ARCHIVE_BINDING
BF-U07-FOUND-T0-IMPL-02 = OPEN / NEGATIVE_TEST_MASKED_BY_SOURCE_HASH
RF-U07-FOUND-T0-IMPL-01 = REQUIRED / EXACT_HEAD_TEST_PROVENANCE
RF-U07-FOUND-T0-IMPL-02 = REQUIRED / REAL_SIX_SOURCE_AND_OUTPUT_BOUNDARY

Tier0 Implementation Verification Closure = NOT_GRANTED
Tier0 Independent Evidence Acceptance = NOT_PASSED
Tier0 Merge Authorization = NOT_GRANTED
Tier1 Implementation/Context Probe = NOT_AUTHORIZED

SOURCE_LEXICAL_INVENTORY_COMPLETENESS = PASS (prior PR #297/#298)
BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next action:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Targeted Implementation Remediation` on original PR #305, only within D0-01..04, then exact-head tests, true six-Java-source output evidence and a **separate Targeted Independent Implementation / Evidence Re-Review**. No auto-merge or Tier-1 implementation.
