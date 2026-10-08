# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Targeted Independent Implementation / Evidence Re-Review v0.1

> Date: 2026-10-08. Independent evidence/design review only.
> **Author PR #305 exact HEAD:** `3d6358e4b153cc8c4414b1b69f8a654bf91b3d28`
> **Limited implementation authority PR #304:** `fd820541a6a94993c7e698661d7d01c3fd5f74de`
> **Original independent implementation review PR #306:** `df76e3da23f83ab60c385ab8b6fa990e91df4867`
> **Exact original audited main:** `6d4fd787600e3a57f01f3e17893e6d98893ac546`, tree `1bfe776f76c986d4e6199a9b820f2cc20773a181`
> **VERDICT: TARGETED_FINDINGS_REMEDIATED_CONDITIONAL / IMPLEMENTATION_EVIDENCE_NOT_ACCEPTED / REVISE_REQUIRED_FOR_CLOSURE.**
> No edits to implementation PR; no Java/Spring boot, JDBC, database access, production changes, PHI or merge.

## 1. Exact diff and baseline comparison

Compared PR #305 exact author head to PR #304 authorization head via GitHub: precisely four **ADD** files, 0 pre-existing changes:
| D0 | File | Exact author Git blob |
|---|---|---|
| D0-01 | `tools/u07_foundation_tx_topology/static_projection.py` | `42960b789dbefe9e5ceb2a6240d6b8402b473250` |
| D0-02 | `tools/u07_foundation_tx_topology/static_schema.py` | `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` |
| D0-03 | `tools/u07_foundation_tx_topology/test_static_projection.py` | `d456bfc7b9d40b8b54270ad82c774565c05ba757` |
| D0-04 | `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` | `2e8b997610365a808cc9052bb568b75fbe2c7e52` |

**Authorization scope = PASS.** No new CI, production Java, Spring profile, Maven/POM, database migrations or Tier-1 probe. The implementation does not claim effective `PlatformTransactionManager` or physical COMMIT.

## 2. Original targeted blocker dispositions

### BF-U07-FOUND-T0-IMPL-01 — unauthenticated source inventory

**REMEDIATED_FOR_RE_REVIEW / CLOSED_CONDITIONAL_CODE**, not yet independent exact-HEAD execution closure.

`project()` now checks `digest(archive)` against **independently accepted frozen ZIP SHA256** `0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089` **before opening the ZIP**, and `digest(inventory_raw)` against **independently accepted JSON SHA256** `b28d72868833a8c109938025a6077dd6799924e89e31a34572c4d186f5bcf077` **before JSON parsing**.

The genuine locally preserved GitHub CI artifact independently hashes to these expected values. Added negative cases reject modified unrelated tracked-file digest or replaced path under immutable archive trust-anchor checks; earlier self-reported head/count-only weakness is structurally closed. The consumer still validates exact source HEAD/tree, 2,112 tracked paths, 256 matches and six exact source SHA/blob snapshots after trust anchor acceptance.

**Residual limitation:** a previously known authority digest proves byte equivalence of the particular artifact, not authorization of a newer source head or absence of dynamic/external consumers.

### BF-U07-FOUND-T0-IMPL-02 — hash rejection masking semantic-edge tests

**REMEDIATED_FOR_RE_REVIEW / CLOSED_CONDITIONAL_CODE**, subject to exact-author-run provenance.

The updated `test_missing_or_duplicate_edge_fails_closed` regenerates a synthetic *internally consistent* file inventory, updates `sha256`, Git blob SHA and `bytes` for the changed CDPManager source and repacks a synthetic archive. Its `run_project()` helper temporarily substitutes synthetic expected archive/JSON hashes *only in the test environment*, then asserts `missing or duplicate direct edge` specifically. Test mutation also uses the correct NUL Git blob separator; it no longer passes merely at the preceding hash check.

Two new tests call `project()` without changing the frozen trust-anchor constants to verify failure on repacked/mutated archive. The symlinked artifact-dir negative case is also added.

**Residual limitation:** synthetic fixture tests prove exact parser behavior only for their cases; there is no syntax-aware validation that declared annotations or call sites are outside comments/strings. See new finding below.

## 3. Independent execution evidence and exact-HEAD limitations

The local review container has the original CI ZIP plus a pre-existing mirror of `tools/u07_foundation_tx_topology`, used only for **supporting, nonauthoritative** behavior checks. In that mirror, `python -m unittest discover -s tools/u07_foundation_tx_topology -p 'test_*.py' -q` returned `Ran 12 tests ... OK`. Independently hashed original ZIP and JSON match the frozen trust roots.

**Important provenance restriction:** the local three-file mirror does **not** match all three current PR #305 Git Blob SHAs (`static_projection.py` and `static_schema.py` differ; the test blob matches). Therefore the 12/12 local result is **not an exact-GitHub-HEAD execution claim**. The previous `RF-U07-FOUND-T0-IMPL-01` remains OPEN until execution on a byte-verified full PR #305 checkout or independently hash-reconciled four-file bundle, with attached runner/version/test-name/digest evidence.

Independently fetched all six actual Java sources from `main@6d4fd...`; GitHub returned these blobs:
| Source | Verified Git blob |
|---|---|
| `runtime/u01/U01ConsultationService.java` | `55948ef6de77693f546e43954754c14b7854d667` |
| `runtime/foundation/CanonicalBusinessEventLedger.java` | `3b7cb7191e6b10290b5c5cbb53d237c26b22b498` |
| `service/cdp/CDPManager.java` | `993b4c5f2d313f7416d142378a67eb8d4f6491fa` |
| `service/cdp/CDPVersionService.java` | `a4e8407120b20b3229f8402290ace57b9480b79e` |
| `runtime/foundation/RuntimeBindingService.java` | `da552124d4aea2faf1d5d24c779be5346d4d0e0d` |
| `runtime/foundation/ClinicalRunCoordinator.java` | `a0d991b7e0286e025e93227fa3e83ffec7fea5e1` |

Source-declared calls were visibly confirmed (U01 lines 52/66/68/83/85 and CDPManager line 70). **However no complete, reproducible CLI execution over all six exact local source snapshots has been attested**, and no resulting JSON SHA/independent evidence bundle exists. `RF-U07-FOUND-T0-IMPL-02` remains OPEN. Fetching GitHub content for manual inspection is not equivalent to producing a verified executable source snapshot bundle.

## 4. New review finding: annotation classification false-positive in comments and string literals

**BF-U07-FOUND-T0-RE-01 = OPEN / NEW SEMANTIC_CORRECTNESS_FINDING.**

The exact author `static_projection._member_tx` still uses a multiline regex over *raw Java source text*:

```python
rx = r"@Transactional(?:\([^)]*\))?\s+public\s+[\w<>?,\[\]. ]+\s+" + re.escape(method) + r"\s*\("
return re.search(rx, text, re.MULTILINE) is not None
```

The preceding test mirror has this same algorithm. A targeted independent negative run showed both inputs accepted as declared transactional:
```text
_member_tx("/*\n@Transactional\npublic Object start(String a) {}\n*/", "start") == True
_member_tx("String s = \"@Transactional\npublic Object start(\";", "start") == True
```

This does **not** demonstrate that the exact current six Java files are misclassified: their genuine annotated methods are present in fetched source. It *does* show the generic producer may misinterpret comments or strings as actual annotations in future or tampered/source-rebased inputs, yielding a false `declared_transactional=true` under a trusted snapshot. This violates the Tier-0 no-false-declared-evidence intent and is not addressed by hashes when the source genuinely contains such text. Require a token/comment/string-aware Java lexer (stdlib) or conservative `UNKNOWN` when syntactic certainty is unavailable, plus explicit adversarial tests for block comment, line comment, Java string, `@Transactional` method mismatch, interface/default methods and annotations with parameters. Exact-diff D0-01..03 only; no Java/runtime parser dependency without amendment.

## 5. Independent status of gate controls

| Gate | Review |
|---|---|
| `T0-AUTH-G01` exact decision/HEAD lineage | PASS / SOURCE_PROVEN |
| `T0-AUTH-G02` only four added files | PASS |
| `T0-AUTH-G03` SOURCE_ONLY output without runtime Manager claim | PASS / CODE_REVIEW |
| `T0-AUTH-G04` full actual six-file projection | PENDING / REAL_RUN_NOT_ATTESTED |
| `T0-AUTH-G05` fail-closed integrity and semantic correctness | PARTIAL: original two blockers conditionally fixed; new annotation false positive |
| `T0-AUTH-G06` no network/DB/runtime activity | SOURCE_CODE_PASS; exact-head runtime test evidence pending |
| `T0-AUTH-G07` evidence hashes/determinism/redaction | PARTIAL / archive anchors accepted; full output not produced |
| `T0-AUTH-G08` independent validation / closure and merge authority | NOT_PASSED |

**No closure is granted.** Neither this targeted re-review nor prior local tests support the claim that Tier-0 implementation is fully verified.

## 6. Formal re-review decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Targeted Independent Implementation / Evidence Re-Review
= REVISE_REQUIRED_FOR_CLOSURE

Exact author HEAD = 3d6358e4b153cc8c4414b1b69f8a654bf91b3d28
Exact-Diff authorization D0-01..04 = PASS

BF-U07-FOUND-T0-IMPL-01 = CLOSED_CONDITIONAL_CODE / EVIDENCE_PENDING
BF-U07-FOUND-T0-IMPL-02 = CLOSED_CONDITIONAL_CODE / EVIDENCE_PENDING
BF-U07-FOUND-T0-RE-01 = OPEN / COMMENT_STRING_ANNOTATION_FALSE_POSITIVE

RF-U07-FOUND-T0-IMPL-01 = OPEN / EXACT_HEAD_12_TEST_PROVENANCE
RF-U07-FOUND-T0-IMPL-02 = OPEN / SIX_REAL_SOURCE_CLI_PROJECTION_AND_ARTIFACT

Tier0 Implementation Verification Closure = NOT_GRANTED
Tier0 Merge Authorization = NOT_GRANTED
Tier1 Spring Context/DB implementation = NOT_AUTHORIZED

BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next recommended bounded work:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Second Targeted Implementation Remediation + Exact-Head Evidence Completion` within D0-01..04. Add comment/string-sensitive conservative annotation parsing and negative tests, then produce byte-exact-head executable test evidence and the six-snapshot SOURCE_ONLY output. Follow with a new targeted independent evidence re-review; do not merge or enable Tier-1 automatically.
