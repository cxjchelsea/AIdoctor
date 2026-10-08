# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Tier-0 Exact-Diff Limited Implementation Authorization Decision v0.1

> Decision date: 2026-10-08
> **Exact input review PR #303:** `00c81cb4f7ddda60d1f846063030cca9a3131f38`, `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Producer_Diagnostic_Harness_Integration_Readiness_Exact_Diff_Authorization_Review_v0.1.md`, blob `372d3736c98df1e2a1976bcb7db9f7e8066d50c3`.
> **Previously accepted design:** author [PR #300](https://github.com/cxjchelsea/AIdoctor/pull/300) @ `09736a2764a7ea5ba640fbc82bd87bc403ad0b52`; independent [PR #302](https://github.com/cxjchelsea/AIdoctor/pull/302) @ `b1fee57665aa1ed44a5e8859f76a5824224fc423`.
> Exact **audited runtime source**: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`.
> **Decision = AUTHORIZE_LIMITED_TIER0_IMPLEMENTATION_ONLY.** This record is a **bounded design/governance authorization decision** for preparing a subsequent diagnostic-only candidate PR; it is **not an approval to merge, activate Tier-1, access a database, change business Runtime or start Spring**.
> Source-readability and file-existence check: all four D0 paths **absent** at the exact decision base tree, so the planned diff is four *new* files only.

## 1. Authorization basis and precise separation of responsibility

The completed Tier-0 tracked-source lexical inventory [PR #297](https://github.com/cxjchelsea/AIdoctor/pull/297)/[#298](https://github.com/cxjchelsea/AIdoctor/pull/298) (CI run `37736999966`, artifact `11531528615`, immutable main 2,112 tracked files, 256 matches) supplies already reviewed **source-input provenance**, not a finished effective Spring manager topology. Independent CA design re-review [PR #302](https://github.com/cxjchelsea/AIdoctor/pull/302) accepted the Tier-0/Tier-1 separation and safe-start/manager-resolution *design*, while PR #303 found that a small **static-only** Tier-0 evidence producer can be implemented without the currently unproven Tier-1 sandbox.

This decision grants **permission to author and test a candidate Tier-0 source parser and static evidence schema only**, under the exact limits below. It **does not** authorize deployment, database-backed tests or Spring context bootstrap. Any additional security/environment grant required by local policy must be recorded before executing the candidate beyond offline synthetic tests; the decision cannot substitute for a real external owner approval.

## 2. Exact authorized diff — exclusive allowlist

| ID | Exact allowed path | Positive implementation scope | Prohibited result |
|---|---|---|---|
| `D0-01` | `tools/u07_foundation_tx_topology/static_projection.py` | parse immutable prior Foundation full-source JSON inventory and independently retrieved exact-source text/annotation snapshots, construct U01→Foundation→CDP/Version→RuntimeBinding→Consultation→ClinicalRun **source-declared candidate graph**; source SHA, path/line/blob and digest fail-closed | Spring import/start, framework bean introspection, DB/network, fabricated manager identity or complete dynamic call graph |
| `D0-02` | `tools/u07_foundation_tx_topology/static_schema.py` | strict typed Tier-0 `U07EffectiveSpringTxTopologyV1` subset: `SOURCE_ONLY/UNKNOWN/NOT_IMPLEMENTED` dispositions, immutable provenance, safe redaction and deterministic serialization/digests | `CONFIGURED_SAME_MANAGER`, `CONTEXT_CONFIRMED`, `PHYSICALLY_SAME_CONNECTION_AND_COMMIT` or similar runtime-positive conclusions |
| `D0-03` | `tools/u07_foundation_tx_topology/test_static_projection.py` | Python stdlib synthetic unit tests of expected source graph, accurate transitive CDPVersion, missing node, mismatched HEAD, tampered hash, explicit unknowns, rejected positive-manager claim, determinism, no-I/O behavior | initializing Java/Spring, external integration tests, PHI fixtures, online package installs |
| `D0-04` | `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md` | contract-versioned input/output schema, source/artifact references, expected independent oracle, exact entrypoint, SHA binding, results/limitations, changed-files inventory and review/evidence release gate | claiming any physical Spring manager topology or active clinical environment authorization |

**Nothing else is within this authorization.** No new CI workflow is permitted in the first implementation diff, no updates to `tools/u07_foundation_audit/exhaustive_inventory.py`, no Maven/POM/library/profiles/config amendments, and no edits to existing Foundation or U01 code.

## 3. Mandatory invariants

1. **Source identity:** outputs must reject mismatched immutable `source_head` and `git_tree_sha` and require a valid input evidence manifest/artifact digest. The previous actual-source baseline is `6d4fd787600e3a57f01f3e17893e6d98893ac546`; an updated main requires fresh independent source delta and explicit rebaseline, not `latest`.
2. **Input completeness and authenticity:** consume the actual exhaustive lexical inventory, including known direct Foundation paths, as a reference; add explicit exact-head Java source snapshots or line/annotation captures only when locally supplied with verifying SHA/blob. Do not convert lexical `no match` into proof that reflective/dynamic/external consumers cannot exist.
3. **No runtime manager assertion:** every edge is `DECLARED_SOURCE_EDGE` or `UNKNOWN`. Static method annotations may establish `DECLARED_TRANSACTIONAL` but never bean/proxy interception, selected `PlatformTransactionManager`, actual EMF or DataSource, JDBC enlistment, rollback behavior or U07 admission atomicity.
4. **No network/DB/Spring access:** only local, read-only Git metadata and file bytes plus deterministic pure transformations. No `java`/`mvn` invocation, Spring startup, JDBC calls, sockets, Python network libraries, external APIs, downloads, cloud credentials or production service calls.
5. **No PHI or secrets:** only source metadata/synthetic fixtures and redacted identifiers. Reject or scrub credential-like inputs, absolute host paths, patient/tenant IDs, JDBC URLs and real data. Do not serialize source payload examples from production.
6. **No future component fiction:** current-main U07 admission/binding beans are `NOT_IMPLEMENTED`; never manufacture their instance identities or present Tier-1 observations as if recorded.
7. **No side effects:** deterministic outputs, optional write only to caller-specified isolated *artifact directory* after exact-source validation; fail-closed if outside allowed output. No source-tree modifications. No network CI trigger.
8. **Trace and reproducibility:** deterministic JSON and hash manifest, exact `path:line:blob`, per-evidence role/unknown classification, program version/command, no universal `PASS` based on presence of file alone.
9. **Compatibility:** retain U01 `START_CONSULTATION` as a Foundation consumer without mandatory U07 Binding; explicit CDPVersion and original ClinicalRun edges; runtime collision and rollback-only hazards stay open, not “solved” by static inference.
10. **Authorization scope:** this decision authorizes **candidate source-only implementation**; it does not grant merge or acceptance, and cannot be reused to authorize Tier-1/prod/PHI.

## 4. Authorized validation within Tier-0

The implementer may run **offline Python stdlib unit tests on synthetic fixtures** and static schema validation and may lint/syntax-check the proposed Python files. Evidence shall include:

```text
Tier0ValidationEvidenceV1
- frozen source_head, tree_sha, input evidence JSON + ZIP SHA256
- exact D0-01..04 diff/file hashes
- test runner/version; test names and pass/fail/skip counts
- positive synthetic source edge cases
- negative source SHA/manifest tamper, missing path, duplicate edge,
  transitive CDPVersion omission, unknown/dynamic consumer handling,
  fake manager identity injection, attempted forbidden I/O,
  deterministic serialization/hash and redaction
- produced static projection SHA256
- limitations and independent review gate
```

Allowed local tests **do not** exercise real Spring, JPA, a connected DB or the clinical U01 production path. Exact-source static inventory facts may be processed as input; **tests must not fabricate live context state**. Do not run previously unrelated broad Maven pipelines as proof of this scope (they are neither necessary nor sufficient).

## 5. Implementation workflow and independent gates

```text
This limited decision: AUTHORIZE_LIMITED_TIER0_IMPLEMENTATION_ONLY
  ↓
Separate candidate branch, parent = THIS decision exact HEAD
  ↓
Only add D0-01..04 and offline synthetic Tier-0 tests
  ↓
Confirm exact diff matches exclusive path allowlist
  ↓
Validate local stdlib tests and SHA/source manifest
  ↓
Independent Tier-0 Producer/Contract/No-I/O Implementation Review
  ↓
Explicit Implementation Verification Closure / evidence acceptance
  ↓
Explicit separate Merge Authorization Decision
```

**No automatic merge**, rebase, squash or code activation. If an implementer discovers a need for another file, framework dependency, CI workflow, Java test, database connection or unsafe input, **STOP and request a controlled exact-diff amendment** rather than broadening this decision.

### Required independent implementation review checklist

| Gate | Expected positive evidence |
|---|---|
| `T0-AUTH-G01` | implementation branch derives from this decision exact head; source SHA and tool manifest provenance pinned |
| `T0-AUTH-G02` | only D0-01..04 added; no existing/runtime file modified |
| `T0-AUTH-G03` | output schema strictly SOURCE_ONLY and UNKNOWN; no configured/physical manager false positives |
| `T0-AUTH-G04` | U01 and CDPVersion/ClinicalRun declared graph correct against independently sourced exact-head Java |
| `T0-AUTH-G05` | invalid/missing/modified source, duplicated/ambiguous edge and false-liveness claims fail closed |
| `T0-AUTH-G06` | no network/DB/Java/Spring/runtime activity; sandboxed synthetic negative tests |
| `T0-AUTH-G07` | evidence JSON/manifest deterministic, hashes verifiable and no secrets/PHI |
| `T0-AUTH-G08` | independent exact-diff/fixture/Oracle review and explicit later merge authorization |

A **design authorization** is not an implementation test success. `T0-AUTH-G01..08` are future gates, **not executed by this decision**.

## 6. Explicitly denied or deferred work

```text
Tier-1 Spring Context / sandbox D1-01..07 = NOT_AUTHORIZED
Boot/AOP runtime proxy introspection = NOT_AUTHORIZED
PlatformTransactionManager runtime selection proof = NOT_AUTHORIZED
DataSource.getConnection, JDBC/SQL, Flyway/MySQL/Oracle = NOT_AUTHORIZED
Production code, U01/Foundation shared Ledger amendment = NOT_AUTHORIZED
U06 eligibility / U15 / P02 / U07 physical owner action = NOT_AUTHORIZED
External real patient, PHI, PROFILE-A, production = BLOCKED
New CI, Maven/POM changes, config/profile changes = NOT_AUTHORIZED
Merge/rebase/squash = NOT_AUTHORIZED
```

Tier-1 integration findings `BF-U07-FOUND-TX-INT-01..03` remain **OPEN**; read-only Context isolation, Spring manager selection and representation have not been physically verified. `BF-U07-FOUND-SEM-01/02` remain OPEN. `BF-U07-FOUND-AUD-01..04` remain OPEN, notwithstanding the already PASS lexical inventory subgate.

## 7. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Tier-0 Exact-Diff Limited Implementation Authorization Decision
= AUTHORIZE_LIMITED_TIER0_IMPLEMENTATION_ONLY

Authorization evidence head = 00c81cb4f7ddda60d1f846063030cca9a3131f38
Allowed diff = ADD D0-01..04 ONLY
Authorized activity = static Python source/evidence producer + synthetic local tests
Tier0 Implementation = NOT_YET_EXECUTED
Tier0 Independent Implementation Review = NOT_STARTED
Tier0 Merge Authorization = NOT_GRANTED

Tier1 Implementation/Probe = NOT_AUTHORIZED
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

**Next step:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Tier-0 Bounded Static Producer Implementation`, adding only the four enumerated files to a new candidate Draft PR from this decision HEAD, then independent implementation and evidence review before any merge. This decision does not execute or approve Tier-1 probes.
