# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Six-Source CLI Successful Evidence Attestation v0.2

> Evidence date: 2026-10-08
> Exact author [PR #305](https://github.com/cxjchelsea/AIdoctor/pull/305) HEAD: `2ca948009281ed6bf5f346d8c382291af96a7934` (Draft, unmerged).
> Limited implementation authorization [PR #304](https://github.com/cxjchelsea/AIdoctor/pull/304): `fd820541a6a94993c7e698661d7d01c3fd5f74de`.
> Independently authored historical source Oracle: `U07_Tier0_ExactMain_Independent_Source_Oracle.json`, frozen SHA-256 `2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9`.
> Historical audited source HEAD: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`; tree `1bfe776f76c986d4e6199a9b820f2cc20773a181`.
> Previous failed input attestation: [PR #308](https://github.com/cxjchelsea/AIdoctor/pull/308) (missing six complete snapshots; **not** retroactively relabeled).
> **Verdict: SIX_SOURCE_CLI_ATTESTATION = PASS / SOURCE_ONLY.**
> Evidence collection and exact-head offline tests only; **NOT** Implementation Verification Closure, independent evidence acceptance, Merge Authorization or Tier-1 permission.

## 1. Previously blocked evidence condition resolved

Unlike the prior [PR #308](https://github.com/cxjchelsea/AIdoctor/pull/308) attempt, this run supplied all **six complete original Java file bytes** to the actual producer CLI. Files were reconstructed via read-only GitHub exact-commit file content export into a local isolated snapshot directory, without changing source code. Every file was verified independently against both (a) SHA-256 in the **genuine prior immutable inventory**, and (b) Git Blob SHA-1 from the exact main GitHub repository.

| Exact main Java snapshot | Byte count | Observed verified Git blob |
|---|---:|---|
| `runtime/u01/U01ConsultationService.java` | 6,976 | `55948ef6de77693f546e43954754c14b7854d667` |
| `runtime/foundation/CanonicalBusinessEventLedger.java` | 3,083 | `3b7cb7191e6b10290b5c5cbb53d237c26b22b498` |
| `service/cdp/CDPManager.java` | 9,296 | `993b4c5f2d313f7416d142378a67eb8d4f6491fa` |
| `service/cdp/CDPVersionService.java` | 2,481 | `a4e8407120b20b3229f8402290ace57b9480b79e` |
| `runtime/foundation/RuntimeBindingService.java` | 2,923 | `da552124d4aea2faf1d5d24c779be5346d4d0e0d` |
| `runtime/foundation/ClinicalRunCoordinator.java` | 3,046 | `a0d991b7e0286e025e93227fa3e83ffec7fea5e1` |

These are immutable **source text** checks. They establish neither live Spring Bean wiring nor any actual DB transaction.

## 2. Exact-author Python and original scan provenance

All three local Python sources were hashed as Git Blobs **before CLI execution** and matched exact PR #305 author GitHub blobs:

| Exact Python file | Git blob |
|---|---|
| `static_projection.py` | `45fd3b336a7ac8797f19714dca111292aba3840e` |
| `static_schema.py` | `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` |
| `test_static_projection.py` | `36731fadf8c9c574b326549084a29d19f2249478` |

Read-only input scan evidence:

```text
Archived CI run = 37736999966
Archived GitHub artifact ID = 11531528615
Source HEAD = 6d4fd787600e3a57f01f3e17893e6d98893ac546
Original archived ZIP SHA256 =
  0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089
Embedded JSON inventory SHA256 =
  b28d72868833a8c109938025a6077dd6799924e89e31a34572c4d186f5bcf077
Tracked inventory files = 2112
Lexical matches = 256
```

The input trust anchor was checked independently. No `latest` source substitution, re-packed pretend authoritative archive, mock Java input, developer `dev` profile or external DB was used.

## 3. Successful **actual producer CLI** execution

Invocation of byte-verified PR #305 `static_projection.py`:

```bash
python /mnt/data/tier0_work/tools/u07_foundation_tx_topology/static_projection.py \
  --archive /mnt/data/u07-foundation-exact-head-inventory.zip \
  --snapshots-dir /mnt/data/u07_tier0_sixsource_attestation/snapshots \
  --artifact-dir /mnt/data/u07_tier0_sixsource_attestation/output
```

Observed positive execution:

```text
CLI_EXIT = 0
STDERR = (empty)
STDOUT = SOURCE_ONLY projection_sha256=2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9
OUTPUT_FILES = ["tier0-source-projection.json"]
OUTPUT_SHA256 = 2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9
INDEPENDENT_ORACLE_SHA256 = 2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9
BYTE_EQUAL = True
```

The program produced a genuine `U07EffectiveSpringTxTopologyV1-TIER0` JSON. Independently checked JSON invariants:

- `status = SOURCE_ONLY`, `spring_context_executed = false`, `database_access = false`.
- **6** expected source-declared edges and **9** reported nodes (six target services + `ConsultationRepository` + two proposed/absent U07 nodes).
- `effective_manager`, `entity_manager_factory`, `datasource` all `UNKNOWN`.
- Proposed `U07AdmissionApplicationService` and `U07CanonicalEventBindingRepository` classified `NOT_IMPLEMENTED`, not fabricated live Beans.
- Actual produced JSON **byte-for-byte equals the frozen independent Oracle**, not simply a similar-looking graph. The pre-existing Oracle was not changed in response to actual output.

## 4. Exact offline unit-test execution

Exact-source Python `unittest` re-run after successful CLI:

```text
python -m unittest discover \
  -s /mnt/data/tier0_work/tools/u07_foundation_tx_topology \
  -p 'test_*.py' -v

Ran 15 tests in 0.255s
OK
```

All 15 synthetic-only tests PASS; zero failures. These cover original trusted ZIP tamper, coherent synthetic missing/duplicate edge semantics, no fake configured manager proof, deterministic JSON, output path/symlink rejection, Java comment/string/text-block annotation and lexical incomplete-input fail-closed behavior. The run did not invoke Spring/Java/JDBC, connect to any network service, or use patient data.

## 5. Portable independent verification artifact

A local evidence bundle was created **outside** the implementation PR; PR #305 remains exactly D0-01..04, no CI workflow or Runtime additions.

```text
Local evidence:
  /mnt/data/U07_Tier0_SixSource_CLI_Successful_Evidence.zip

ZIP SHA256:
  39b76870032a3b62eb25e2d026eec9cf934df9042b11f4f93c434d38108c42cf

Internal successful-attestation-manifest.json SHA256:
  99369364406f4ec209e1e5381ffd9b4ba412047a229785277c248bd224a277da

ZIP member count = 14
Bytes = 166603
```

Bundle contains: all six independently hash-verified Java snapshots in their exact relative locations, three exact implementation Python files, the unchanged authoritative original ZIP, actual CLI `tier0-source-projection.json`, previously frozen independent Oracle, a 15-test exact-blob unittest log, and a per-file SHA256/Git Blob manifest. Artifact is non-PHI and source/evidence-only; **no Java application execution** is implied by inclusion of Java source text.

## 6. Evidence acceptance and remaining authority boundaries

| Gate/evidence | Status after actual execution |
|---|---|
| Original scan ZIP and JSON trust roots | VERIFIED |
| Exact author Git Blob identity for three Python files | VERIFIED |
| Six actual main Java source bytes and independent hash/blob comparison | **6/6 VERIFIED** |
| Actual six-source Python CLI execution | **PASS / EXIT 0** |
| Genuine produced JSON output | **CREATED / SHA FROZEN** |
| Actual JSON vs independent Oracle | **BYTE_EQUAL / PASS** |
| Offline exact-code unit tests | **15 PASSED** |
| `RF-U07-FOUND-T0-IMPL-01` | EXECUTION_EVIDENCE_COLLECTED / PENDING_INDEPENDENT_ACCEPTANCE |
| `RF-U07-FOUND-T0-IMPL-02` | POSITIVE_CLI_EVIDENCE_COLLECTED / PENDING_INDEPENDENT_ACCEPTANCE |
| Tier-0 Independent Implementation / Evidence Re-Review | **NOT_YET_PASSED** |
| Tier-0 Implementation Verification Closure | **NOT_GRANTED** |
| Tier-0 Merge Authorization | **NOT_GRANTED** |
| Tier-1 Spring/AOP/transaction manager/DB probe | **NOT_AUTHORIZED** |

The previous evidence attempt (PR #308, `FAIL_CLOSED_MISSING_SNAPSHOTS`) remains factually accurate for its own earlier execution and is **superseded for the positive evidence objective** by this new success report. It must **not** be rewritten to say the failed run passed.

## 7. Formal result

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Six-Source CLI Evidence Attestation
= PASS / SOURCE_ONLY_EVIDENCE_COMPLETE

Exact implementation PR #305 HEAD
= 2ca948009281ed6bf5f346d8c382291af96a7934

SIX_JAVA_SOURCE_BLOBS = 6/6 VERIFIED
PYTHON_SOURCE_BLOBS = 3/3 VERIFIED
FROZEN_SCAN_ARCHIVE_AND_JSON = VERIFIED
ACTUAL_CLI_EXIT_CODE = 0
ACTUAL_CLI_OUTPUT_SHA256
= 2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9
INDEPENDENT_ORACLE_BYTE_EQUAL = TRUE
EXACT_HEAD_OFFLINE_UNIT_TESTS = 15 PASSED

RF-U07-FOUND-T0-IMPL-01 = EVIDENCE_COLLECTED / INDEPENDENT_REVIEW_PENDING
RF-U07-FOUND-T0-IMPL-02 = EVIDENCE_COLLECTED / INDEPENDENT_REVIEW_PENDING

TIER0_IMPLEMENTATION_VERIFICATION_CLOSURE = NOT_GRANTED
TIER0_MERGE_AUTHORIZATION = NOT_GRANTED

TIER1_SPRING_CONTEXT_DB = NOT_AUTHORIZED
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next gate:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Second Targeted Independent Implementation / Evidence Re-Review`, reviewing this **successful source-byte-backed execution** and author PR #305 exact HEAD. Only after independent acceptance may the owner request a separate Tier-0 Implementation Verification Closure decision. Do **not** merge or activate Tier-1 automatically.
