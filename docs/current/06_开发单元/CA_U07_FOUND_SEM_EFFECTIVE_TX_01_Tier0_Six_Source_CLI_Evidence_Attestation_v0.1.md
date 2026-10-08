# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Six-Source CLI Evidence Attestation v0.1

> Evidence date: 2026-10-08
> Exact implementation source: [PR #305](https://github.com/cxjchelsea/AIdoctor/pull/305), HEAD `2ca948009281ed6bf5f346d8c382291af96a7934`
> Authorization: [PR #304](https://github.com/cxjchelsea/AIdoctor/pull/304), `fd820541a6a94993c7e698661d7d01c3fd5f74de`; prior independent review [PR #307](https://github.com/cxjchelsea/AIdoctor/pull/307)
> Physical historical source: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`, tree `1bfe776f76c986d4e6199a9b820f2cc20773a181`
> **Attestation verdict: NOT_PASSED / ACTUAL_CLI_ATTEMPT_FAIL_CLOSED_MISSING_SNAPSHOTS.**
> Attestation-only; no modifications to PR #305, Spring/JVM, JDBC/Oracle/MySQL, production, PHI or merge.

## 1. Input provenance independently verified

Exact prior GitHub CI scan archive `/mnt/data/u07-foundation-exact-head-inventory.zip` was found in the offline execution environment, read byte-for-byte, and matches independently frozen evidence from [PR #298](https://github.com/cxjchelsea/AIdoctor/pull/298):

```text
archive SHA256
= 0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089

foundation-exact-head-inventory.json SHA256
= b28d72868833a8c109938025a6077dd6799924e89e31a34572c4d186f5bcf077

Git-tracked files = 2112
lexical matches = 256
```

Prior local offline evidence ZIP `U07_Tier0_ExactHead_Offline_Evidence.zip` includes exactly these GitHub implementation-source blobs:

| Filename | Exact Git blob | Source SHA-256 |
|---|---|---|
| `static_projection.py` | `45fd3b336a7ac8797f19714dca111292aba3840e` | `e79eb7cdbd3d789ac5f86e1388bbe441978b34b4d136e6523b1dc0f96046c997` |
| `static_schema.py` | `cc7eaa06199da8194ea4d19eb7fe53ea1f4b4f87` | `89060290cc98468fd8396e24381d849f7299a60e2b0a0147eeb2289bb62ef971` |
| `test_static_projection.py` | `36731fadf8c9c574b326549084a29d19f2249478` | `f49c14d2933fb89f73ae2c86294cd1f74dd8a1196ebab7984f54f6d2a3ba81c6` |

The local Python stdlib test runner was executed against the offline mirror of those three verified exact blobs:

```text
python -m unittest discover -s /mnt/data/tier0_work/tools/u07_foundation_tx_topology -p 'test_*.py' -q

Ran 15 tests in 0.255s
OK
```

**This is valid exact-blob offline unit-test evidence, not a six-source CLI projection.** There was no Java, Spring or database startup.

## 2. Independent source Oracle — NOT an executed producer result

Prior independent inspection obtained the following main Java Git blobs, but the *complete raw source bytes were not part of the local offline input package*:

| Class | Git blob |
|---|---|
| `U01ConsultationService` | `55948ef6de77693f546e43954754c14b7854d667` |
| `CanonicalBusinessEventLedger` | `3b7cb7191e6b10290b5c5cbb53d237c26b22b498` |
| `CDPManager` | `993b4c5f2d313f7416d142378a67eb8d4f6491fa` |
| `CDPVersionService` | `a4e8407120b20b3229f8402290ace57b9480b79e` |
| `RuntimeBindingService` | `da552124d4aea2faf1d5d24c779be5346d4d0e0d` |
| `ClinicalRunCoordinator` | `a0d991b7e0286e025e93227fa3e83ffec7fea5e1` |

The separately authored `U07_Tier0_ExactMain_Independent_Source_Oracle.json` contains six source-declared edges (U01→Ledger L52, U01→CDPManager L66, CDPManager→CDPVersionService L70, U01→RuntimeBinding L68, U01→ConsultationRepository.save L83, U01→ClinicalRun L85) and SOURCE_ONLY/UNKNOWN dispositions. Its SHA256 is `2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9`.

**Do not promote this Oracle into a successful CLI output.** GitHub source access was read-only; transferring complete Java source snapshots into the isolated filesystem was unavailable in this attestation execution. A Git blob identifier or independently reconstructed expected output is not a substitute for supplying the actual Java bytes to the producer.

## 3. Actual CLI attempt and observed fail-closed result

An isolated local artifact directory and a separate snapshot directory were prepared under `/mnt/data/u07_tier0_sixsource_attestation/`. The actual Tier-0 Python CLI was invoked, pointing to the *genuine* authority ZIP:

```bash
python /mnt/data/tier0_work/tools/u07_foundation_tx_topology/static_projection.py \
  --archive /mnt/data/u07-foundation-exact-head-inventory.zip \
  --snapshots-dir /mnt/data/u07_tier0_sixsource_attestation/snapshots \
  --artifact-dir /mnt/data/u07_tier0_sixsource_attestation/output
```

Observed actual execution:

```text
exit_code = 1
FAIL_CLOSED: [Errno 2] No such file or directory:
  /mnt/data/u07_tier0_sixsource_attestation/snapshots/
  diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/
  U01ConsultationService.java

complete_verified_java_snapshots = 0 / 6
actual_cli_output_count = 0
tier0-source-projection.json = NOT_CREATED
```

This is positive evidence that a missing required source snapshot causes refusal rather than invented evidence. **It is not a successful six-source CLI execution.**

Attestation attempt JSON SHA256:
`582cc004e353304cbec6ae7659c03a8db5b514e8fc7fce2bca6b9212d6cea3ce`

Evidence attempt ZIP SHA256:
`0fc42561e1d3e227716b88fdb735a2d1b8d60dc8da012690eea82c64db9d20e7`

ZIP contents: `attestation-result.json`, `attempt.stdout`, `attempt.stderr`, `readme.txt`. Exact file `/mnt/data/U07_Tier0_SixSource_CLI_Attestation_Attempt.zip`.

## 4. Unsatisfied positive attestation condition

A reviewer may mark `SIX_SOURCE_CLI_ATTESTATION = PASSED` only after all conditions are met:

1. Fetch/export all **six complete Java source files** at immutable `main@6d4fd...` to an isolated snapshot directory mirroring their original relative paths; verify every file SHA256 against the authenticated full tracked-file manifest and Git blob against the repository's exact commit.
2. Reconfirm the three Python producer source Git blobs against the current PR #305 exact HEAD, and run 15 offline tests with an immutable test log and tool version.
3. Run the actual CLI with the genuine original ZIP, a **pre-existing isolated output directory**, and the real snapshots. Capture zero exit status, one generated JSON output, its SHA256, and source-snapshot SHA manifest.
4. Compare producer `tier0-source-projection.json` **byte-for-byte** with independently authored expected Oracle. If mismatch, report `ORACLE_MISMATCH` with exact evidence diff, do not revise Oracle to match observed output automatically.
5. Independently verify artifact/provenance and SOURCE_ONLY/UNKNOWN/NOT_IMPLEMENTED classification; ensure no Spring, DB or network calls.
6. Add no extra files to PR #305 (D0-01..04 only). An independent evidence document in its *own branch* is permitted; merge remains separately authorized.

This attestation collected valid preflight evidence but could not complete item 1; thus items 3 and 4 remain **NOT_EXECUTED**, not `PASS`. Further work to acquire the source bytes is still limited to Tier-0 local read-only inputs, not an authorization to call Runtime or connect to DB.

## 5. Formal evidence verdict

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Six-Source CLI Evidence Attestation
= NOT_PASSED / FAIL_CLOSED_MISSING_REQUIRED_SNAPSHOTS

Original Inventory ZIP/JSON SHA = VERIFIED
Exact Python Source Git Blob SHAs = VERIFIED
Tier0 Python Offline Tests = 15 PASSED
Independent Six-Source Expected Oracle = AVAILABLE
Actual Six-Source Java Snapshot Package = MISSING
Actual Six-Source CLI Exit = 1 / FAIL_CLOSED
Actual Six-Source CLI Output Artifact = NOT_CREATED
Output-vs-Oracle Comparison = NOT_EXECUTED

RF-U07-FOUND-T0-IMPL-01 = EXACT_BLOB_OFFLINE_TEST_EVIDENCE_PRESENT
RF-U07-FOUND-T0-IMPL-02 = OPEN / SIX_SOURCE_CLI_EVIDENCE_MISSING

Tier0 Implementation Verification Closure = NOT_GRANTED
Tier0 Merge Authorization = NOT_GRANTED
Tier1 Spring/Manager/DB = NOT_AUTHORIZED

BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next work:** export/pin six exact Java snapshot bytes into the isolated execution environment and **repeat** this same CLI attestation. Do not conduct implementation closure, independent evidence acceptance, Tier-1 setup or merge without positive evidence.
