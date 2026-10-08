# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Implementation Verification Closure Decision v0.1

> Decision date: 2026-10-08
> Decision scope: **Tier-0 static SOURCE_ONLY evidence producer implementation verification only**.
> Limited original implementation authorization [PR #304](https://github.com/cxjchelsea/AIdoctor/pull/304) exact HEAD `fd820541a6a94993c7e698661d7d01c3fd5f74de`.
> Exact implementation [PR #305](https://github.com/cxjchelsea/AIdoctor/pull/305) HEAD `2ca948009281ed6bf5f346d8c382291af96a7934`.
> Six-source positive attestation [PR #309](https://github.com/cxjchelsea/AIdoctor/pull/309) HEAD `5619d17b43b3234524743578501881fd863410d2`; evidence document blob `9065878905a7a80f7682b94ff8797079d66cd059`.
> Independent targeted implementation/evidence re-review [PR #310](https://github.com/cxjchelsea/AIdoctor/pull/310) exact HEAD `9dd2020166de800d7a0c8cb4330968d297a058dc`, independent review blob `b5d219d861c399f7a6471930bc501c14c8a3e108`; verdict `PASS / INDEPENDENT_SOURCE_ONLY_EVIDENCE_ACCEPTED`.
> Historical exact source `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`, tree `1bfe776f76c986d4e6199a9b820f2cc20773a181`.
>
> **FORMAL DECISION = AUTHORIZE_TIER0_IMPLEMENTATION_VERIFICATION_CLOSURE / CLOSED_SOURCE_ONLY.**
>
> **This is NOT Merge Authorization, Tier-1 authority, U07 Implementation Authorization, runtime topology proof, or production activation.**

## 1. Closure criteria and independent evidence acceptance

| Gate | Accepted evidence | Formal disposition |
|---|---|---|
| `CL-T0-01` immutable authorization lineage | PR #304 → PR #305 exact HEAD, no drift | PASS |
| `CL-T0-02` exclusive D0-01..04 diff | GitHub compare between `fd820541` and `2ca94800`: precisely **4 additions**, no edits to pre-existing files | PASS |
| `CL-T0-03` independent raw source identity | six immutable Java snapshots independently SHA256 / Git Blob verified against accepted historical tracked inventory | PASS |
| `CL-T0-04` authorized original input | ZIP SHA256 `0ab0682aa9d14f08573a53d47c4ad54c0cdba2955747b749431901b82e694089`; JSON SHA256 `b28d72868833a8c109938025a6077dd6799924e89e31a34572c4d186f5bcf077` | PASS |
| `CL-T0-05` real producer execution | fresh independent offline CLI: exit `0`, actual generated JSON SHA256 `2894806f159c18a9c24918e058473c6581ac7c9d7719fe2718d08f5aac765ea9` | PASS |
| `CL-T0-06` independent Oracle | actual generated JSON **byte identical** to pre-frozen independent expected SOURCE_ONLY Oracle | PASS |
| `CL-T0-07` synthetic fail-closed testing | exact Python Git Blobs `45fd3b3`/`cc7eaa0`/`36731fa`; fresh 15/15 offline Python stdlib tests PASS | PASS |
| `CL-T0-08` separation from runtime and elevated authority | nine nodes, six source-declared edges; `effective_manager`, EMF and DataSource `UNKNOWN`; no Spring/DB or PHI execution | PASS / IN_SCOPE |
| `CL-T0-09` independent review | PR #310 = PASS; original BF closure and RF satisfaction separately recorded | PASS |
| `CL-T0-10` merge / production authority separation | no merge, activation or downstream permission executed or inferred | PASS / CONTROLLED |

The source-only evidence bundle is recorded by PR #309 with archive SHA256 `39b76870032a3b62eb25e2d026eec9cf934df9042b11f4f93c434d38108c42cf`. PR #308's earlier `FAIL_CLOSED_MISSING_SNAPSHOTS` remains a true historical result; this closure relies on a **new successful** input-backed execution in PR #309, not alteration of historical evidence.

## 2. Formal resolution of tracked Tier-0 findings

| Identifier | Before acceptance | Verification-closure disposition |
|---|---|---|
| `BF-U07-FOUND-T0-IMPL-01` trust root missing | accepted fixed SHA256 ZIP+JSON before parsing | **CLOSED** |
| `BF-U07-FOUND-T0-IMPL-02` semantic negative tests masked by hash mismatch | tests regenerate coherent source SHA256 and Git Blob, assert semantic edge failure | **CLOSED** |
| `BF-U07-FOUND-T0-RE-01` commented/string `@Transactional` false positive | Java lexical masking and adversarial test coverage independently verified | **CLOSED** |
| `RF-U07-FOUND-T0-IMPL-01` exact implementation test evidence | three Python Git Blob hashes verified and independent 15/15 | **SATISFIED / ACCEPTED** |
| `RF-U07-FOUND-T0-IMPL-02` complete six-source CLI / Oracle evidence | six sources present, original archive proven, actual CLI 0 and byte-equal Oracle | **SATISFIED / ACCEPTED** |

**No Tier-0 implementation blocker remains open in the accepted scope.** This must not be broadened into a claim about completeness of all dynamic Java consumers, actual Spring wiring, or database atomicity.

## 3. Precisely frozen closure baseline

```text
CLOSURE_UNIT = CA-U07-FOUND-SEM-EFFECTIVE-TX-01 / TIER0_ONLY
AUTHORITY_HEAD = fd820541a6a94993c7e698661d7d01c3fd5f74de
IMPLEMENTATION_PR = #305
IMPLEMENTATION_HEAD = 2ca948009281ed6bf5f346d8c382291af96a7934
EVIDENCE_PR = #309
EVIDENCE_HEAD = 5619d17b43b3234524743578501881fd863410d2
INDEPENDENT_REVIEW_PR = #310
INDEPENDENT_REVIEW_HEAD = 9dd2020166de800d7a0c8cb4330968d297a058dc
SOURCE_MAIN_HEAD = 6d4fd787600e3a57f01f3e17893e6d98893ac546
SOURCE_MAIN_TREE = 1bfe776f76c986d4e6199a9b820f2cc20773a181

ALLOWED_CHANGED_FILES =
  tools/u07_foundation_tx_topology/static_projection.py
  tools/u07_foundation_tx_topology/static_schema.py
  tools/u07_foundation_tx_topology/test_static_projection.py
  docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md

PRODUCER_RESULT = SOURCE_ONLY
EFFECTIVE_SPRING_MANAGER = UNKNOWN
ENTITY_MANAGER_FACTORY = UNKNOWN
DATASOURCE = UNKNOWN
JDBC_PHYSICAL_TRANSACTION = NOT_PROVEN
```

A new commit on the author implementation branch or a replacement of the attestation evidence does **not** inherit the closure automatically: require exact-head impact review and re-evaluation of relevant closure evidence. This decision may be used as an input for the **separate Tier-0 Merge Authorization Decision**, but does not issue that authorization.

## 4. Closure limitations and unresolved downstream gates

These blockers remain **OPEN** and are not part of this limited closure:
- `BF-U07-FOUND-TX-INT-01..03`: sandbox containment, safe representative Spring Context and effective transaction-manager resolution for Tier-1.
- `BF-U07-FOUND-SEM-01`: actual Spring transaction-manager / proxy / EMF / DataSource equivalence not established.
- `BF-U07-FOUND-SEM-02`: physical uniqueness-collision/commit behavior not established.
- `BF-U07-FOUND-AUD-01..04`: outstanding Foundation reference audit work.

The full `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01` remains `NOT_PASSED`. U07 Implementation Readiness remains `NOT_READY`; U07 Implementation Authorization remains `NOT_GRANTED`; all clinical/PHI/profile-A/production paths stay **BLOCKED**.

The closure explicitly does **not** permit any work on `diagnosis-service/src/main/java/**`, database migrations, Maven/POM, Spring profiles, deployed CI, or Tier-1 harness D1-01..07. No Java or Spring process, actual MySQL/Oracle/Redis/Nacos connections, JDBC transaction or real patient data is covered by this validation.

## 5. Formal Implementation Verification Closure Decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Implementation Verification Closure Decision
= AUTHORIZE_TIER0_IMPLEMENTATION_VERIFICATION_CLOSURE

TIER0_CODE_SCOPE = EXACT_D0_01_TO_04_ONLY
TIER0_IMPLEMENTATION_VERIFICATION = CLOSED / SOURCE_ONLY
TIER0_INDEPENDENT_IMPLEMENTATION_EVIDENCE = ACCEPTED
TIER0_SIX_SOURCE_CLI_EVIDENCE = PASS
TIER0_EXACT_HEAD_OFFLINE_TESTS = 15 PASSED

BF-U07-FOUND-T0-IMPL-01 = CLOSED
BF-U07-FOUND-T0-IMPL-02 = CLOSED
BF-U07-FOUND-T0-RE-01 = CLOSED
RF-U07-FOUND-T0-IMPL-01 = SATISFIED / ACCEPTED
RF-U07-FOUND-T0-IMPL-02 = SATISFIED / ACCEPTED

TIER0_MERGE_AUTHORIZATION = NOT_GRANTED
TIER0_MERGE = NOT_EXECUTED
TIER1_SPRING_CONTEXT / TX_MANAGER / DATABASE = NOT_AUTHORIZED

BF-U07-FOUND-TX-INT-01..03 = OPEN
BF-U07-FOUND-SEM-01/02 = OPEN
BF-U07-FOUND-AUD-01..04 = OPEN
EFFECTIVE_SPRING_MANAGER_TOPOLOGY = NOT_PROVEN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED

U07_IMPLEMENTATION_READINESS = NOT_READY
U07_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PROFILE_A / PHI / PATIENT / PRODUCTION = BLOCKED
```

**Authorized next *decision*, not next execution:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Merge Authorization Decision`. Before any merge, independently verify exact HEAD stability, target/base tree compatibility, the unchanged four-file diff and the final merge authorization explicitly. Even a permitted future merge of Tier-0 source-only tooling does not authorize Tier-1, Foundation audit closure, U07 implementation, PHI access or production.
