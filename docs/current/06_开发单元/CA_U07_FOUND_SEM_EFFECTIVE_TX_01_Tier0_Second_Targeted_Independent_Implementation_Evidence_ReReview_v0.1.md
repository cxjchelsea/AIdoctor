# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Second Targeted Independent Implementation / Evidence Re-Review v0.1

> Review date: 2026-10-08
> **Implementation PR #305 exact HEAD:** `2ca948009281ed6bf5f346d8c382291af96a7934`.
> **Evidence PR #309 exact HEAD:** `5619d17b43b3234524743578501881fd863410d2`; evidence is based on implementation HEAD above.
> **Limited authorization PR #304 HEAD:** `fd820541a6a94993c7e698661d7d01c3fd5f74de`.
> Historical immutable source `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`, tree `1bfe776f76c986d4e6199a9b820f2cc20773a181`.
> Previous findings: independent review [PR #306](https://github.com/cxjchelsea/AIdoctor/pull/306), first targeted re-review [PR #307](https://github.com/cxjchelsea/AIdoctor/pull/307).
> **VERDICT: PASS / TIER0_INDEPENDENT_IMPLEMENTATION_AND_SOURCE_ONLY_EVIDENCE_ACCEPTED.**
> This is **not** Implementation Verification Closure, Merge Authorization, Tier-1 authorization, Spring runtime or physical JDBC/COMMIT verification.

## 1. Independent exact-diff confirmation

Retrieved PR #305 at exact author HEAD and compared to PR #304's authorization HEAD via GitHub. Precisely four added files only:

- `tools/u07_foundation_tx_topology/static_projection.py` — Git Blob `45fd3b336a7ac8797f19714dca111292aba3840e`.
- `tools/u07_foundation_tx_topology/static_schema.py` — Git Blob `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87`.
- `tools/u07_foundation_tx_topology/test_static_projection.py` — Git Blob `36731fadf8c9c574b326549084a29d19f2249478`.
- `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` — contract manifest.

**Exact diff = PASS.** No Java business/runtime code, Maven, `application.yml`, Flyway migration, CI workflows or Tier-1 probe change. Candidate remains Draft / unmerged.

## 2. Independent evidence bundle audit

Audited the *actual byte bundle* `U07_Tier0_SixSource_CLI_Successful_Evidence.zip`, rather than trusting its textual claims alone.

```text
Bundle SHA256 = 39b76870032a3b62eb25e2d026eec9cf934df9042b11f4f93c434d38108c42cf
Bundle members = 14
Per-file manifest records = 13
Per-file byte length + SHA256 = 13/13 PASS
Required Git blob SHA = ALL MATCHED
Six Java source digests match historical inventory = 6/6 PASS

Original immutable CI archive SHA256 =
0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089

Embedded original inventory JSON SHA256 =
b28d72868833a8c109938025a6077dd6799924e89e31a34572c4d186f5bcf077
```

All six actual main Java snapshot bytes are present; no empty placeholders. They match the original CI inventory independently of the snapshot bundle's own manifest. The ZIP includes three exact implementation Python files, authoritative original source inventory ZIP, actual production-of-evidence JSON, a previously frozen Oracle, and complete offline test log. No patient or clinical case record is required for these checks.

## 3. Independent *new* execution — NOT a replayed log

Unpacked the reviewed evidence ZIP into a new disposable isolated directory and independently invoked the bundled **actual** producer code using its complete original inputs:

```bash
python python/static_projection.py \
  --archive inputs/u07-foundation-exact-head-inventory.zip \
  --snapshots-dir java-snapshots \
  --artifact-dir isolated-result
```

Observed second independent execution:

```text
CLI exit code = 0
STDERR = (empty)
STDOUT = SOURCE_ONLY projection_sha256=2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9
Actual independent result SHA256 = 2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9
Frozen independent expected Oracle SHA256 = 2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9
Actual independent CLI result bytes == frozen Oracle bytes = TRUE
```

Separately reran:

```text
python -m unittest discover -s python -p 'test_*.py' -q
Ran 15 tests in 0.261s
OK
```

Independent review environment's test runner was Python stdlib on local source/evidence inputs. No JVM, Spring `ApplicationContext`, JDBC, MySQL/Oracle, production service, remote system or PHI required. Distinguish the six Java **source files** from the forbidden execution of a Java application.

## 4. Independent result semantics and previous findings

The newly generated result was independently parsed:
- `status=SOURCE_ONLY`;
- **nine nodes**, **six source-declared edges**;
- `effective_manager=UNKNOWN`, `entity_manager_factory=UNKNOWN`, `datasource=UNKNOWN`;
- future U07 Admission/Binding beans are `NOT_IMPLEMENTED`;
- `spring_context_executed=false`, `database_access=false`.

**BF-U07-FOUND-T0-IMPL-01 = CLOSED.** ZIP and embedded JSON bind to independent frozen accepted SHA-256 before parse; independent archive integrity checks passed.

**BF-U07-FOUND-T0-IMPL-02 = CLOSED.** Semantic missing/duplicate-edge tests now repack *internally coherent* source digest and Git blob; their assertion is on the semantic error, not an earlier mismatched hash; this test was re-executed among 15 passes.

**BF-U07-FOUND-T0-RE-01 = CLOSED.** The source parser's Java comments, line comments, string literals, char literals and text blocks are masked before method-annotation or edge matching, and malformed cases fail closed. The targeted negative cases executed successfully in this independent run. It remains a bounded static lexer, not a complete Java compiler/AST.

**RF-U07-FOUND-T0-IMPL-01 = SATISFIED / INDEPENDENT_EVIDENCE_ACCEPTED.** All three Python sources had byte-verified Git Blobs and were tested independently.

**RF-U07-FOUND-T0-IMPL-02 = SATISFIED / INDEPENDENT_EVIDENCE_ACCEPTED.** All six complete Java snapshots verified and independent actual CLI result byte-equals the frozen Oracle.

## 5. Exact review gates

| Gate | Decision |
|---|---|
| `T0-AUTH-G01` exact authority / immutable source provenance | PASS |
| `T0-AUTH-G02` D0-01..04 only, no runtime or CI diff | PASS |
| `T0-AUTH-G03` source-only, UNKNOWN manager, no positive runtime attribution | PASS |
| `T0-AUTH-G04` original six-source graph and no invented U07 beans | PASS / SIX_SOURCE_EVIDENCE |
| `T0-AUTH-G05` hash tamper, negative call-edge, comment/string false positive | PASS / CONTROLLED_TEST_CASES |
| `T0-AUTH-G06` no Spring/DB process, source-only offline execution | PASS / IN_SCOPE |
| `T0-AUTH-G07` deterministic evidence, original immutable digests, Oracle byte equality | PASS |
| `T0-AUTH-G08` independent exact-head implementation/evidence review | PASS / **MERGE_AUTHORIZATION_SEPARATE** |

No evidence is interpreted as a physical transaction test or clinical rollout approval. Historical PR #308 missing-snapshot failure remains a valid earlier fail-closed attempt; PR #309 separately proves successful input-backed execution.

## 6. Formal independent re-review verdict

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Second Targeted Independent Implementation / Evidence Re-Review
= PASS / INDEPENDENT_SOURCE_ONLY_EVIDENCE_ACCEPTED

Exact Implementation HEAD = 2ca948009281ed6bf5f346d8c382291af96a7934
Exact Evidence PR #309 HEAD = 5619d17b43b3234524743578501881fd863410d2
Authorization Exact Diff D0-01..04 = PASS

BF-U07-FOUND-T0-IMPL-01 = CLOSED
BF-U07-FOUND-T0-IMPL-02 = CLOSED
BF-U07-FOUND-T0-RE-01 = CLOSED
RF-U07-FOUND-T0-IMPL-01 = SATISFIED / ACCEPTED
RF-U07-FOUND-T0-IMPL-02 = SATISFIED / ACCEPTED

SIX_JAVA_SNAPSHOTS = 6/6 VERIFIED
EXACT_PYTHON_BLOBS = 3/3 VERIFIED
INDEPENDENT_CLI_EXIT = 0
INDEPENDENT_CLI_ORACLE = BYTE_EQUAL
INDEPENDENT_OFFLINE_UNIT_TESTS = 15 PASSED

Tier0 Implementation Verification Closure = NOT_YET_DECIDED
Tier0 Merge Authorization = NOT_GRANTED
Tier1 Spring/Manager/DB Probe = NOT_AUTHORIZED

BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
SOURCE_LEXICAL_INVENTORY_COMPLETENESS = PASS
EFFECTIVE_SPRING_MANAGER_TOPOLOGY = NOT_PROVEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next gate:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Implementation Verification Closure Decision` at exact author and evidence heads. Only that separate decision may close Tier-0 verification. A subsequent explicitly authorized *Merge Authorization Decision* remains required; no merge, Tier-1, clinical or production activation follows automatically.
