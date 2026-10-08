# U07 Implementation Readiness Re-Evaluation v0.1

> Review date: 2026-10-08
> **Verdict: NOT_READY / IMPLEMENTATION_AUTHORIZATION_NOT_GRANTED**
> Aggregate design basis: [PR #291](https://github.com/cxjchelsea/AIdoctor/pull/291), review HEAD `76951d72f5510a9f70ca6936d76ff75a7d3cc195`
> Frozen aggregate author design HEAD: `c319c67e65a0e620e547d03514cbe45e25020975`, [PR #290](https://github.com/cxjchelsea/AIdoctor/pull/290)
> Independently inspected current main HEAD: `6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Review scope: source-backed **pre-implementation** readiness, outstanding owner authorizations, architecture applicability, negative/physical evidence availability and the next governed corrective sequence.
> Review only: no Runtime code, DB migration, U07 tests, permission grant, merge, PHI or production action performed.

## 1. Executive decision / meaning of aggregate PASS

U07 Unit Spec and RDP-01..06 received **conditional design acceptance**, and targeted aggregate compatibility was **conditionally accepted** in PR #291. This resolves the two original *document-level* cross-RDP contradictions:
- P02 B0 U07 immutable request intent; B1 owner-owned Thread claim + `RESTORE_START_AUTHORIZED` journal + U15-approved restore-only grant in **one actual guard-DB transaction**; B2 inert P02 restoration outside transaction to a durable parked barrier.
- RDP-01 T17 / RDP-06 VG-006 normal Foundation INSERT/FLUSH before binding failure requires one-transaction rollback of **both** rows; separate preexisting committed legacy-orphan scenario is quarantine.

Neither conditional design acceptance proves physical transaction co-location, producer/consumer grants, executable P02 restoration, exact U06 eligibility issuance, actual Oracle/Fixture approval, or a completed source compatibility audit. **NOT_READY** is therefore mandatory.

## 2. Exact-main source inspection / verified presence and non-equivalence

Inspected the current `main` GitHub tree recursively and fetched selected core source files. Existing useful assets:
- `runtime/foundation/CanonicalBusinessEventLedger.java`: a real transport-idempotency-oriented Foundation ledger with `@Transactional`, not U07 canonical inline binding, full safe outer transaction/commit-unknown reconciliation or F8 decision authority. Its existing DataIntegrityViolationException catch/re-read path is an explicit JPA rollback-only compatibility audit target.
- `RuntimeThreadStateRecord.java`: existing `ACTIVE / WAIT_CHECKPOINTED / AWAITING_USER`, `@Version`, current Run/Checkpoint/Wait references. **No** parked `RESUMED_READY_BUT_NOT_DISPATCHED`, P02 B1 owner-grant CAS or verified U07 resume transition in this class.
- `RuntimeWaitCheckpointRecord.java`: original wait provenance and delivered Question metadata; **not** a serialized executable checkpoint with frozen plan/cursor/history and all external-effect receipts.
- `runtime/u06/wait/U07ResumeEligibilityProjector.java`: hashes original wait effect/checkpoint `U06Ids.hash("u07elig", ...)` after `threadAwaitingUser`; **not** a durable U06-owned signed issuance, historical transition proof, or independent authorization record.
- Existing U06 Wait/Delivery/Trace stores, `diagnosis-service/src/test/.../U06Rdp06AuthoritativeObservationTest.java`, `tools/u06_nonprod_verification/verify_u06.py`, and `.github/workflows/u06-rdp06-authoritative-verification.yml` are bounded references for reuse, **not U07 implementation or U07 Oracle authorization**.
- MySQL and Oracle `V6__add_u06_wait_runtime.sql` exist. The main tree does **not** contain `V7__add_u07_canonical_event_binding.sql`, any `tools/u07_nonprod_verification`, or a U07-specific runtime Java package/runner in the inspected paths.

Main remains at `6d4fd787...`; PR #290/#291 design/review files are **not merged to main**. This is a documented HEAD comparison, not a claim to have executed application/test binaries or audited all indirect consumers.

### Evidence source table

| Evidence item | Source/ref | Finding |
|---|---|---|
| main HEAD | GitHub branch `main@6d4fd787...` | exact current physical source baseline |
| CanonicalBusinessEventLedger | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/CanonicalBusinessEventLedger.java` | reusable Foundation ledger only; no U07 binding/outer COMMIT proof |
| RuntimeThreadStateRecord | `.../runtime/foundation/RuntimeThreadStateRecord.java` | U06 wait phases only, no P02 B1 or parked restoration |
| RuntimeWaitCheckpointRecord | `.../runtime/foundation/RuntimeWaitCheckpointRecord.java` | metadata Wait checkpoint, not executable restoration proof |
| U07ResumeEligibilityProjector | `.../runtime/u06/wait/U07ResumeEligibilityProjector.java` | derived hash, not durable U06 issuance owner fact |
| U06 runner workflow | `tools/u06_nonprod_verification/verify_u06.py` and `.github/workflows/u06-rdp06-authoritative-verification.yml` | U06-only evidence and authorization domain |
| U07 approved design | aggregate author HEAD `c319c67e...`, independent review `76951d72...` | design compatible conditionally, physical authority not conferred |

## 3. Readiness decision matrix

| Gate | Current evidence | Readiness disposition | Mandatory remediation |
|---|---|---|---|
| G-01 independently reviewed U07 Unit Spec/RDP01..06 aggregate design | PR #291 conditional aggregate design acceptance | **DESIGN_ONLY_PASS** | exact blobs pinned in later authority manifest |
| G-02 Foundation canonical ledger consumer/transaction/migration reference audit | `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01` NOT_PASSED | **BLOCKED** | exact SHA-pinned consumer and JPA outer-transaction audit |
| G-03 authoritative U06 eligibility issuance | hash projector only, `CA-U06-U07-ELIG-ISSUANCE-01` NOT_AUTHORIZED | **BLOCKED** | U06 owner issuance/provenance controlled contract and review |
| G-04 F8/U15 shared terminal winner/clock | `CA-U07-RDP02-U15-SHARED-FENCE-01` NOT_AUTHORIZED | **BLOCKED** | owner-approved shared lock and MySQL/Oracle statement-current final conditional write |
| G-05 P02 B1 owner Thread/journal/grant atomic start topology | no U07 P02 B1/parked source, `CA-U07-AGG-P02-START-GRANT-TX-01` NOT_AUTHORIZED | **BLOCKED** | exact physical ownership/co-location, lock, CAS, COMMIT_UNKNOWN evidence |
| G-06 P02 safe rehydrate/landing | `CA-U07-RDP04-P02-REHYDRATE-OWNER-01`, `...EXECUTION-START-FENCE-01`, `...LANDING-BARRIER-01` NOT_AUTHORIZED | **BLOCKED** | P02 owner-certified complete effect manifest and dispatcher-wide parked stop |
| G-07 F3 Question/Pending and K09/P01 Stage C | `CA-U07-RDP03-F3-ANSWER-BRIDGE-01`, `...P01-U15-SHARED-COMMIT-FENCE-01` NOT_AUTHORIZED | **BLOCKED** | single owner-authorized patch, one Clinical version, U15 fence and readback |
| G-08 Consultation ACTIVE / APPLIED+Outbox + U15 outbound grant | source proof not demonstrated, `CA-U07-RDP03-U15-DISPATCH-GRANT-01` NOT_AUTHORIZED | **BLOCKED** | guarded D, E unique atomic commit, distinct F grant |
| G-09 U02 consumer replay and receipt | `CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01` NOT_AUTHORIZED | **BLOCKED** | U02 owner-approved same handoff ID, idempotent admission/receipt readback |
| G-10 admission T17 atomicity/oracle | `CA-U07-AGG-ADMISSION-T17-ORACLE-01` conditionally design accepted only | **BLOCKED_FOR_PHYSICAL** | correct tests for pre-commit rollback versus preexisting legacy orphan |
| G-11 source/migration/DB dialect evidence | no U07 V7/transaction tested; only U06 V6 and design DDL | **NOT_DEMONSTRATED** | independent MySQL/Oracle transaction/clock/isolation evidence after authorization |
| G-12 Oracle/Fixture/Traceability authority | `U07TraceabilityMatrixV1`, machine oracles/fixtures and review gates not built or reviewed | **NOT_DEMONSTRATED** | reviewed independent source-case-to-branch map and exact-head manifests |
| G-13 Tier-1 U07 runner and mandatory physical/negative-effect evidence | no U07 runner/tests observed, no actual U07 test execution | **NOT_EXECUTED** | execute only after scoped authorizations and isolation |
| G-14 PROFILE-B authorization | contract limits design to synthetic nonprod, physical grant not demonstrated | **NOT_GRANTED / CONDITIONAL** | explicit isolated profile and owner capabilities before any physical U07 invocation |
| G-15 PROFILE-A, PHI, real patients, production | out of scope | **BLOCKED** | no implied extension by U07 design or Tier-1 green |

**Decision logic:** gate G-01 is necessary but insufficient. Any missing G-02..G-10 required owner gate and physical transaction feasibility forces `NOT_READY`; G-11..G-13 cannot be declared PASSED from a design-only review. Synthetic PROFILE-B is **a permitted target scope to seek authorization**, not already an authorized physical implementation.

## 4. Exact unresolved gate inventory and distinct authority meanings

```text
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED

CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-U15-DISPATCH-GRANT-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-REHYDRATE-OWNER-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-EXECUTION-START-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-LANDING-BARRIER-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01 = REQUIRED / NOT_AUTHORIZED

CA-U07-AGG-P02-START-GRANT-TX-01 = DESIGN_ACCEPTED / NOT_AUTHORIZED
CA-U07-AGG-ADMISSION-T17-ORACLE-01 = DESIGN_ACCEPTED / NOT_AUTHORIZED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
```

Total: **one required Foundation audit plus eleven distinct owner/aggregate CAs not authorized**. `DESIGN_ACCEPTED` for an aggregate CA means the textual correction passed independent review, **not** U15/P02/Foundation owner authorization or implemented schema. No review PR may silently change any of these to GRANTED.

## 5. Recommended governed remediation sequence (dependency order, no auto authorization)

### Phase P0 — Canonical Foundation evidence and exact source audit

Execute `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01` as a **separate bounded, SHA-pinned audit and independent review**. Examine all ledger consumers and real transaction/proxy relationships, `REQUIRES_NEW` winner reconciliation, unique-key rollback-only behavior, inline canonical synthetic bytes and FK/index/migration conflicts, other Unit legacy integrations and MySQL/Oracle dialect fidelity. Record exact HEAD, file manifest, negative-test plan and gate PASS/FAIL. **Do not change shared Foundation implementation before its own amendment authorization.**

### Phase P1 — Owner authority design and grants, in two synchronized tracks

**Track identity/business:** U06 authoritative eligibility issuance (not hash projector); Foundation canonical binding/readback; F8/U15 shared consultation lock/deadline winner; secure P06 scope/historical manifest. Each owner must approve its contract/permission separately, with independent reviews and exact CAs.

**Track runtime authority/transaction:** approve precise B0/B1/B2 resource layout across P02 Thread/journal/grant and Consultation/U15; confirm co-located primary database + transaction manager rather than merely similar SQL entity names. Address recovery-owner proof, park-wide dispatcher barrier, start grant and U15 grant-first policy. If physical resource co-location fails, return to a **new controlled amendment** and re-aggregate; do not implement the unverified V1 using independent dual writes.

### Phase P2 — Side-effect owner contracts and consumer capability

Approve F3 semantic Answer/Question/Gap and K09/P01 single Stage-C patch/field grant, Consultation D ACTIVE and journal/Outbox E, distinct U15 F dispatch grant, U02 consumer idempotent admission and query/ACK semantics, U14 failure routing. Require owner-version revocation fencing at effect COMMIT, not cached READY. Confirm current U02 consumer permission can block transport despite a historical U15 grant.

### Phase P3 — Physical implementation readiness and evidence authority design gates

Create and independently review exact-source package, per-RDP contract digest manifest, `U07TraceabilityMatrixV1`, Oracle and Fixture expectations with disjoint owner policy branches, Tier-0 authorization-negative plan, Tier-1 isolated synthetic scope and U02 in-process interception. Freeze V7 schema and MySQL/Oracle test matrix and physical lock protocol. **Machine oracles must be owner-derived, not SUT-output-derived.**

**Distinction:** Designing these artifacts does not itself require executing unapproved business owners. But running Tier-1 physical positive U07 cases is prohibited until all participating owner CAs and the specific synthetic profile are authorized. Tier-0 negative manifest-only checks are allowed within their separately approved non-effect review scope.

### Phase P4 — Reevaluate readiness, then an explicit implementation authorization decision

Once P0–P3 authorization/design prerequisites have independently accepted evidence, rerun **U07 Implementation Readiness Re-Evaluation** at new exact main/owner heads. Only a separate explicit `U07 Implementation Authorization Decision = AUTHORIZE` may allow bounded changes. Build real code, migrations, verification runner and execute Tier-1 only under subsequently authorized scopes; then exact-head implementation/evidence/merge reviews. Do not label full physical runner PASS as a pre-implementation prerequisite when the runner is itself an authorized implementation deliverable. This readiness review asks for **feasible frozen physical design and owner authorizations**, not completed future production code.

## 6. Invalidation rules, explicit prohibitions and interpretation

- A newer design PR head does not change `main` source tree: review artifacts pinned to `76951d72...`, physical code pinned to `6d4fd787...`.
- A later change to any RDP, original U06 producer contract, Foundation ledger, U15 policy or P02 transaction topology invalidates affected snapshot/signatures. Repeat the scoped independent compatibility review as needed.
- A successful U06 oracle/verification runner is evidence of U06 scope only; it cannot be imported as U07 accepted physical evidence.
- `U07ResumeEligibilityProjector` derived hash does not demonstrate durable authoritative U06 eligibility issuance.
- `RuntimeWaitCheckpointRecord` does not demonstrate P02 executable restore context or preexisting completed external effect journal.
- A P02 restore-only grant does not authorize next scheduler/tool/model node, P01 mutation or U02 dispatch; E APPLIED+Outbox does not authorize network send without distinct Stage-F U15 grant.
- No clinical safety, production or PROFILE-A implication follows from PROFILE-B synthetic architecture.

## 7. Final gate decision and next work

```text
U07 Implementation Readiness Re-Evaluation
= NOT_READY / BLOCKED_BY_FOUNDATION_AND_OWNER_PHYSICAL_AUTHORITY

Design:
  U07 Unit Spec / RDP-01..06 = CONDITIONALLY_ACCEPTED_DESIGN
  U07 Aggregate Compatibility = CONDITIONALLY_ACCEPTED_DESIGN
  Original BF-U07-AGG-01,02 = CLOSED_DESIGN_ONLY

Physical and owner authority:
  Foundation Reference Audit = NOT_PASSED
  U06 Eligibility Issuance = NOT_AUTHORIZED
  U15/F8/P01/P02/U02 shared owner amendments = NOT_AUTHORIZED
  P02 B1 shared transaction = NOT_DEMONSTRATED
  RDP-01 V7 inline binding = NOT_IMPLEMENTED
  U07 runner/oracle/fixture physical evidence = NOT_IMPLEMENTED / NOT_EXECUTED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

**Recommended immediate next governed step:** `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 — Exact-Head Source / Compatibility Audit`. In parallel only where independently reviewable, prepare `CA-U06-U07-ELIG-ISSUANCE-01` controlled owner contract design and `CA-U07-AGG-P02-START-GRANT-TX-01` physical co-location feasibility **without editing code**. After the audit/authority design, request a new focused readiness evaluation. Do not seek a blanket AUTHORIZE from this document.

This is a source-and-design assessment, not CI or production evidence. No automated tests were run during this review.
