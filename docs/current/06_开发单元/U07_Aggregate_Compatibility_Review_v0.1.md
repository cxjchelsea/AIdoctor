# U07 Aggregate Compatibility Review v0.1

> Date: 2026-10-08
> Aggregate baseline: `705e2f8ac2df3d96aa00646e0bf3ed802d565c6e` (RDP-06 independent re-review PR #288)
> Scope: design-only cross-RDP compatibility, **not** physical implementation readiness, actual test execution or merge.
> **VERDICT: REVISE_REQUIRED / AGGREGATE_NOT_ACCEPTED / TWO REQUIRED CONTROLLED AMENDMENTS.**
> Status: independent review candidate; source RDP documents are unmodified.

## 1. Exact reviewed design input manifest

All six design blobs were **fetched at the exact same aggregate git HEAD**. Independent review acceptance is conditional and does not certify executable physical interfaces.

| Design artifact | Exact blob SHA | Current design posture / review provenance |
|---|---|---|
| `U07_RDP01_Consumer_Inbound_Event_Admission_Contract_v0.1.md` | `8d8f098952bf9fcc14ca35f38de76bfd4668aa39` | CONDITIONALLY_ACCEPTED_DESIGN; Foundation audit / U06 issuance blocked |
| `U07_RDP02_F8_Business_Resume_Decision_Precedence_Contract_v0.1.md` | `2b900dfe73168e7d19a6042bf91178bbdca63691` | CONDITIONALLY_ACCEPTED_DESIGN; F8/U15 shared fence blocked |
| `U07_RDP03_State_Ownership_K09_P01_Idempotent_Apply_Trace_Contract_v0.1.md` | `cb1cc42f14e70ee50d93e524cae63896cad5fddd` | CONDITIONALLY_ACCEPTED_DESIGN; Stage-C owner and Stage-F grant blocked |
| `U07_RDP04_P02_Runtime_Resume_Recovery_Downstream_Handoff_Boundary_v0.1.md` | `1c40386813af6244985fc9bb7517fa95585daece` | CONDITIONALLY_ACCEPTED_DESIGN; P02 grant/parked/rehydration blocked; PR #282 |
| `U07_RDP05_Capability_Dependency_Applicability_Contract_v0.1.md` | `994c1c2ec824d8fd18ae1516c6bc444ef093de66` | CONDITIONALLY_ACCEPTED_DESIGN; owner capability/transaction proof blocked; PR #285 |
| `U07_RDP06_Verification_Durable_Evidence_Contract_v0.1.md` | `aeb3fbc740631300395d73fbb4d95820b324871d` | CONDITIONALLY_ACCEPTED_DESIGN; machine oracle/fixtures/physical evidence not executed; PR #288 |

Baseline source-inspection context is bounded by `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`, not a freshly executed exhaustive exact-main runtime compatibility audit.

## 2. Compatibility findings matrix

| Cross-RDP boundary | Assessment | Reason |
|---|---|---|
| RDP-01 canonical USER_ANSWER vs transport RESUME_REQUEST target-only | **COMPATIBLE / DESIGN** | immutable original answer and same target root; no second F8 verdict or answer bytes on RESUME_REQUEST |
| RDP-01 trust/issued U06 eligibility → RDP-02 F8 | **COMPATIBLE / CONDITIONAL** | no hash-only issuance, original business wait authority distinct from Checkpoint health |
| RDP-02 first-verdict F8 vs U15 terminal/expiry clock | **COMPATIBLE / CONDITIONAL** | statement-current final conditional write + shared Consultation lock; MySQL/Oracle physical proof pending |
| RDP-02 historical ACCEPTED vs later P02/U15 failure | **COMPATIBLE / DESIGN** | original verdict immutable, further effects independently fenced |
| RDP-03 stage A claim → P02 stage B → stage C P01 → D ACTIVE → E APPLIED/Outbox → F dispatch | **COMPATIBLE AT LOGICAL LEVEL** | one stage owner at a time, prior results and same-root replay; physical P02 start grant topology differs (AC-01) |
| RDP-03 Stage C one F3-owner-approved P01 patch vs RDP-05 owner recheck | **COMPATIBLE / CONDITIONAL** | one Clinical version and fresh owner authority before mutation, real owner grants pending |
| RDP-03 E one COMMIT APPLIED+Outbox vs RDP-04 delivery | **COMPATIBLE / CONDITIONAL** | positive E never directly sends; F U15 durable grant before first outbound send |
| P02 parked restored Runtime vs RDP-03 Stage C/D/E/F | **COMPATIBLE / CONDITIONAL** | `RESUMED_VERIFIED` = parked cursor only, no resumed scheduler-node execution |
| RDP-03 U15 dispatch grant vs RDP-05 U02 consumer permission | **COMPATIBLE / NEEDS PHYSICAL PROOF** | grant irrevocable only within U15-approved same-effect rule; independent current U02 permission can still refuse transport |
| RDP-05 pre/post stage role vs RDP-06 oracle | **COMPATIBLE / DESIGN** | first-ever F8/E do not require own receipt in PRE; POST must prove owner result |
| RDP-06 Tier 0 vs Tier 1 and U02 unique handoff | **COMPATIBLE / CONDITIONAL** | Tier 0 only negative authority evidence; Tier 1 required for positive physical readiness; one logical handoff can have >1 physical attempt |
| RDP-01 selected single-transaction binding vs own T17 and RDP-06 VG-006 | **CONFLICT / AC-02** | crash before outer COMMIT must roll back both; T17 still describes quarantined pending instead of ordinary rollback |
| RDP-03 P02 independent transaction vs RDP-04 same-transaction grant+P02 journal | **CONFLICT / AC-01** | authoritative owner-phase atomicity and same-DB participant undecided |
| PROFILE-B vs PROFILE-A, PHI and production | **COMPATIBLE / RESTRICTED** | only synthetic, no patient, no production permissions |

## 3. BF-U07-AGG-01 — P02 owner journal / U15 start-grant transaction topology

**OPEN / CROSS_OWNER_ATOMIC_GRANT_TOPOLOGY.**

**Evidence:**
- RDP-03 §9.1, §10.2 B: P02 is an **external Runtime owner executing an independent transaction**, and the U07 coordinator persists its Stage-B intent in guard DB before calling that owner. No cross-P02 Runtime/Clinical/Consultation/U02 transaction is assumed.
- RDP-04 §6.1: P02 `P02ExecutionStartGrantV1` must be written atomically with its `P02ResumeJournalV1.RESTORE_START_AUTHORIZED` under **one physical guard DB transaction manager** holding Consultation/U15 row first, then original Runtime Thread row and P02 journal.
- RDP-05 §8: assumes P02 restoration work is external but the restore-start grant is inside the shared guard DB; the status/commit authority of the P02 journal and Thread row is not shown as physically co-located.

The design may be physically realizable **if** the P02 owner journal and Thread claim join the same guard-DB transaction for the grant while later inert restoration is separately reconciled. However that is **not yet an identical selected topology across RDP-03/04/05**. A split P02 journal can instead allow a committed grant with no durable owner-phase authority; claiming atomicity across two databases would be incorrect.

**Required controlled amendment `CA-U07-AGG-P02-START-GRANT-TX-01`:**
1. Freeze exact physical ownership/location for Consultation/U15 row, P02 Thread state, grant ledger, P02ResumeJournal and U07 Stage-B request journal, with a single transaction-manager participation diagram.
2. Select one **actual** transaction protocol that is consistent with RDP-03 B and RDP-04 §6.1: either shared owner-grant/Thread-phase transaction + separately owned inert restoration **with proven cross-owner field ownership and lock order**, or an independently approved alternative that gives equivalent durable grant ordering and no duplicate starts. No implicit distributed atomic commit.
3. Freeze grant COMMIT_UNKNOWN, journal missing/partial, U15 terminal-first / grant-first, Thread supersession and replay reconciler readback across owner stores; U07 may only advance B on proven `RESUMED_VERIFIED` parked result, never a grant alone.
4. Propagate selected topology and field-level authority into RDP-03 §9/10, RDP-04 §6 and RDP-05 §8/§6.2, then RDP-06 MySQL/Oracle fault/race oracles. Independent targeted design review must PASS the whole amended set before aggregate acceptance.
5. If actual P02 store cannot join selected shared guard transaction, status stays **NOT_APPLICABLE / NOT_READY** until a separately reviewed replacement; do not assume a bridge is implemented.

**Safety invariant:** no physical first restore without committed U15-authorized *inert* grant, no post-wait business effects from parked restoration, no new P01 or U02 effect before their own stages.

## 4. BF-U07-AGG-02 — canonical ledger/binding crash oracle has incompatible expected outcome

**OPEN / ADMISSION_ATOMICITY_ORACLE_DRIFT.**

**Evidence:**
- RDP-01 §4.3.2–4.3.3 selects `SINGLE_DB_INLINE_SYNTHETIC_V1`: Foundation `canonical_business_event` and U07 binding/answer bytes visible **together in one transaction**; crash after Foundation insert before binding => outer transaction rollback; no durable orphan. A **preexisting corrupted/legacy winner without matching binding** is a different, quarantine-only case.
- RDP-01 §8 correctly echoes rollback after ledger flush, before binding; but its acceptance matrix §11 `U07-RDP01-T17` still says `crash after ledger before binding → quarantined pending + deterministic repair`. It blurs a normal uncommitted crash with a preexisting already-committed orphan.
- RDP-06 `U07-VG-006` freezes `ledger insert commits but side-binding fails → both rollback` — wording “commits” cannot be literally true if both rollback. Actual test should inject *insert/flush* before outer COMMIT, not a previously committed ledger-only row.

**Required controlled amendment `CA-U07-AGG-ADMISSION-T17-ORACLE-01`:**
1. Rewrite RDP-01 T17 as `Foundation row inserted/flushed, binding fails BEFORE shared outer COMMIT` => exactly **zero** durable canonical event and binding, no F8.
2. Add a distinct `LEGACY_ORPHAN_QUARANTINE` case for an **already committed** Foundation winner with missing binding, with no new F8/answer-effect or fabricated binding. Do not claim the ordinary atomic path can yield this by crashing.
3. Amend RDP-06 VG-006 and its Oracle branch to say `Foundation insert/flush succeeds but U07 binding insert fails; outer transaction rolls back both`. Require positive DB row-count readback, failure injection and independent owner evidence in MySQL/Oracle.
4. Update RDP-05 evidence/quality gate and Oracle/Fixture trace IDs; explicit invalidation and re-review of affected design SHA and future expectation-authority digest.

This is a **testable behavioral contradiction**, not just editorial cleanup: without the change, an implementation can pass an incorrect “quarantine pending” expectation after an ordinary crash while violating the chosen atomic admission contract.

## 5. Non-blocking items to carry to physical readiness

- **U15 granted-before-terminal limits**: P02 restore-only grant and U02 dispatch grant are distinct. U15 irrevocability does not override a separately revoked U02 endpoint/consumer permission or clinical safety owner policy. A complete physical grant/permission protocol is still not authorized.
- **RDP-06 60 scenario families vs underlying source oracles**: the independently reviewed machine-readable `U07TraceabilityMatrixV1` and disjoint branch Oracle are **future prerequisites**. The current 60 rows alone are not accepted test coverage of all RDP source cases; this is a physical/evidence gate rather than an automatic aggregate design PASS.
- **P02 historical reconstruction**: current inspected U06 wait checkpoint is metadata-only; original plan/cursor and all prior tool owner-completion receipts remain required. No runtime implementation/rehydration evidence exists in this design report.
- **F8 clock and dual DB**: MySQL and Oracle need verified statement-current conditional inserts, lock and F8/U15 deadline ordering; Java time or transaction-start-frozen expression is invalid.
- **P01 protected field set**: U06 synthetic StateCommitter does not grant U07 a combined F3 Question/Gap/Pending mutation permission. All related controlled amendments remain not authorized.
- **U02**: one committed logical Outbox identity plus zero duplicate consumer admission; at-least-once physical sends allowed only with same ID and authorized grant. A producer ACK is not U02 Clinical Fact proof.
- **Document history**: several author design files still contain pre-independent-review statuses in historical sections. Canonical accepted state for the aggregate comes from exact independent review records, not from retroactively treating old author headings as current physical authorizations.

## 6. Required external audit and existing controlled-amendment blockers

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

CA-U07-AGG-P02-START-GRANT-TX-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-AGG-ADMISSION-T17-ORACLE-01 = REQUIRED / NOT_AUTHORIZED
```

Existing Foundation/owner CAs and two newly identified aggregate controlled amendments are **distinct**. One must not be marked passed merely because the others are designed. No code, database, Oracle/Fixture runner or MySQL/Oracle tests were modified/executed for this review.

## 7. Formal aggregate compatibility decision

```text
U07 Aggregate Compatibility Review = REVISE_REQUIRED / NOT_ACCEPTED
Aggregate HEAD = 705e2f8ac2df3d96aa00646e0bf3ed802d565c6e

BF-U07-AGG-01 = OPEN / P02_START_GRANT_OWNER_TX_TOPOLOGY
BF-U07-AGG-02 = OPEN / ADMISSION_T17_ORACLE_CONTRADICTION

U07-RDP-01..06 = CONDITIONALLY_ACCEPTED_DESIGN (individual only)
U07 Aggregate Compatibility = NOT_ACCEPTED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED

U07TraceabilityMatrixV1 / independent Oracle + Fixture = NOT_CREATED / NOT_REVIEWED
U07 physical runner / MySQL + Oracle proof = NOT_IMPLEMENTED / NOT_EXECUTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

**Next permitted work:** `U07 Aggregate Controlled Amendment — Targeted Cross-RDP Design Remediation` for `CA-U07-AGG-P02-START-GRANT-TX-01` and `CA-U07-AGG-ADMISSION-T17-ORACLE-01`, followed by an exact-head **U07 Targeted Independent Aggregate Compatibility Re-Review**. Only then may the aggregate design be considered accepted; Implementation Readiness and Owner CA authorizations remain separate, later gates.

No merge, Runtime code, schema, PHI, network send, clinical side effect, test execution or implementation authority was performed or granted.
