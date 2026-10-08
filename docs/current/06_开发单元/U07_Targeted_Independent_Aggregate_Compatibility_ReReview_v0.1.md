# U07 Targeted Independent Aggregate Compatibility Re-Review v0.1

> Review date: 2026-10-08
> Target author design amendment: [PR #290](https://github.com/cxjchelsea/AIdoctor/pull/290)
> **Exact reviewed amendment HEAD:** `c319c67e65a0e620e547d03514cbe45e25020975`
> Original Aggregate Review: [PR #289](https://github.com/cxjchelsea/AIdoctor/pull/289), `REVISE_REQUIRED`
> **Independent verdict: PASS / CONDITIONAL_AGGREGATE_DESIGN_ACCEPTANCE ONLY.**
> This is a review-only record. No original design files were edited; no owner CA was authorized, no Runtime/DB tests were run and no merge or production work was performed.

## 1. Exact-head artifact and integrity check

Fetched PR #290 and compared `5cbbc1996ca22b145be4db2f99b891a7a8b991bb..c319c67e65a0e620e547d03514cbe45e25020975`. Exactly **six Markdown files** changed: one aggregate amendment inventory and five amended RDP designs; **250 additions / 13 deletions**. All below blobs were fetched by the same exact aggregate HEAD.

| Artifact | Exact reviewed blob SHA |
|---|---|
| `U07_Aggregate_Controlled_Amendment_Targeted_Cross_RDP_Design_Remediation_v0.1.md` | `7539e1028e1d8fed30e5668fec8e154fab22fb62` |
| `U07_RDP01_Consumer_Inbound_Event_Admission_Contract_v0.1.md` | `3bc529b39c433a0fdb6d6aab812a98a7882fa310` |
| `U07_RDP03_State_Ownership_K09_P01_Idempotent_Apply_Trace_Contract_v0.1.md` | `7bcece4bd2eeb0e1355919b36051a1487a6ac74e` |
| `U07_RDP04_P02_Runtime_Resume_Recovery_Downstream_Handoff_Boundary_v0.1.md` | `be5c320c5f39c1644f9d775d8a4510effec6de03` |
| `U07_RDP05_Capability_Dependency_Applicability_Contract_v0.1.md` | `0b21ab70fb14e940fc84628ef32def4f4f5a8548` |
| `U07_RDP06_Verification_Durable_Evidence_Contract_v0.1.md` | `2a26fee6847209b81e8ebe0b5c46f16bf38b665b` |

RDP-02 remains unchanged by this amendment. The base RDP-06 `U07-VG-001..060` catalog still has 60 unique, sequential case IDs. The amendment adds `U07-VG-AGG-01..10` (ten ordered design cases) plus `U07-VG-006-LEGACY-ORPHAN`, none executable or executed.

## 2. BF-U07-AGG-01 — shared P02 start grant versus independent Runtime owner

**CLOSED / CONDITIONAL_DESIGN.**

The previously conflicting documents now freeze a single `SINGLE_GUARD_DB_STAGED_SAGA_V1 / P02_B0_B1_B2_ATOMIC_START_V1` topology:

1. **B0** — U07 coordinator commits an immutable same-root Stage-B request **intent** in guard DB. It does not modify P02 owner state or grant restoration permission.
2. **B1** — P02 owner executes **one short physical guard-DB transaction**: Consultation/U15 lock FIRST, P02-owned Runtime Thread row SECOND, then P02 grant-phase journal and grant ledger in deterministic order. Commit **all three owner facts atomically**: Thread claim, `P02ResumeJournalV1.RESTORE_START_AUTHORIZED`, and `P02ExecutionStartGrantV1.AUTHORIZED_RESTORE_ONLY`. Although physically co-located, the Thread and journal remain **P02-owned**, and the grant needs separately approved U15 policy.
3. **B2** — after committed B1 evidence is independently read back, P02 runs **only bounded inert restoration outside the B1 transaction**, durably landing at `RESUMED_READY_BUT_NOT_DISPATCHED`. U07 Stage B may advance on `RESUMED_VERIFIED` or exact replay `ALREADY_RESUMED` only with original owner readback. B1 grant alone is not business effect or parked-success evidence.

**Owner/race compatibility:** B0 absent ⇒ no B1; U15 terminal-first ⇒ zero B1 start grant; B1 grant-first ⇒ same finite inert B2 may finish **only under U15-approved policy**, but no C/D/E/F by old authority; COMMIT_UNKNOWN ⇒ re-read complete P02 Thread/journal/grant triad before physical start; partial write before COMMIT ⇒ full B1 rollback; B2 recovery never invents a fresh original Run, duplicate tool call or business node. RDP-03 §9/§10, RDP-04 §6, RDP-05 §6/§8 and aggregate inventory agree on that split.

**Important independent-review condition:** The design requires P02 Thread, P02 journal and grant to be in **exactly the same real database and transaction manager** as Consultation/U15 for B1. No inspected runtime code or DB-enlistment proof establishes that. If actual P02 storage remains separate, V1 is **NOT_APPLICABLE / NOT_READY** until a separate, approved controlled design. This conditional design acceptance MUST NOT be interpreted as a physical implementation pass or approval of `CA-U07-AGG-P02-START-GRANT-TX-01`.

## 3. BF-U07-AGG-02 — admission crash versus pre-existing orphan

**CLOSED / DESIGN.**

RDP-01 `U07-RDP01-T17` now explicitly tests Foundation canonical-event INSERT/FLUSH followed by binding INSERT failure or crash **before the same outer COMMIT**. Exact expected outcome: **both authoritative tables contain zero new durable rows**, and F8/P02/P01/U02 produce zero effects.

RDP-01 now separately enumerates `T17-LEGACY-ORPHAN`, and RDP-06 maps it to `U07-VG-006-LEGACY-ORPHAN`: only a **pre-seeded previously committed** Foundation winner with an absent binding is quarantined. It must not be manufactured from retry inputs or explained as the result of a normal valid V1 pre-COMMIT crash.

RDP-06 `U07-VG-006` now correctly uses INSERT/FLUSH rather than “ledger commits then both rollback.” Aggregate oracle `U07-VG-AGG-09/10` requires physical readback and distinct fixtures in **MySQL and Oracle**. This is a consistent, testable split, subject to eventual authorized implementation and independent fixture/oracle review.

## 4. Cross-contract preservation checks

| Boundary | Review disposition |
|---|---|
| RDP-01 canonical answer and RESUME_REQUEST target identity | unchanged: no new answer or independent F8 verdict |
| RDP-02 first immutable F8 decision and terminal fence | unchanged: F8 prior ACCEPTED remains historical truth |
| RDP-03 B0/B1/B2 and Stage C/D/E/F | coherent selected topology, one Stage-C P01 commit, D Consultation ACTIVE, E APPLIED+Outbox atomic, F distinct U15 dispatch grant |
| RDP-04 P02 compatibility and parked dispatcher-wide deny | preserved; grant does not run business nodes |
| RDP-05 physical capabilities and per-owner authority version | explicit B1 co-location and commit-time owner checks, still NOT_VERIFIED |
| RDP-06 Oracle/Fixture/Traceability and test tiers | 60 base scenarios + ten aggregate cases + legacy orphan, independent Tier-0/Tier-1 gates unchanged |
| U02 | one logical Outbox/consumer idempotent admission; separate physical sends and later Clinical Fact owner |
| PROFILE-B vs PROFILE-A | synthetic structural only; PHI, real patient, real outbound and production BLOCKED |

**Nonblocking follow-through:** RDP-06 §9 introductory wording still refers to the “60 cases” as the base suite, whereas §8.1 newly requires all aggregate extensions. Before machine Oracle freeze, manifest coverage must explicitly include **all base cases and additional `AGG-01..10` plus `VG-006-LEGACY-ORPHAN`**, and the unit-spec/original-oracle traceability must be independently completed. This does not reopen the two cross-RDP design blockers because §8.1 already makes those additions mandatory and fail-closed.

## 5. Physical authorization dependencies unchanged

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

U07TraceabilityMatrixV1 / independently reviewed Oracle and Fixture = NOT_COMPLETED
U07 physical P02 start-grant shared-DB semantics = NOT_IMPLEMENTED / NOT_VERIFIED
U07 dual-dialect MySQL/Oracle fault tests = NOT_EXECUTED
U07 PROFILE-A / PHI / real patient / production = BLOCKED
```

Design acceptance of these two Aggregate CAs does not authorize their physical changes, permissions, schema migrations or real patient/clinical activation.

## 6. Formal targeted independent aggregate re-review decision

```text
U07 Targeted Independent Aggregate Compatibility Re-Review
= PASS / CONDITIONAL_AGGREGATE_DESIGN_ACCEPTANCE

Exact reviewed HEAD = c319c67e65a0e620e547d03514cbe45e25020975

BF-U07-AGG-01 = CLOSED / CONDITIONAL_DESIGN
BF-U07-AGG-02 = CLOSED / DESIGN

U07-RDP-01..06 = CONDITIONALLY_ACCEPTED_DESIGN
U07 Aggregate Compatibility = CONDITIONALLY_ACCEPTED_DESIGN

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Physical source, CA authorization, MySQL/Oracle and evidence gates = BLOCKING
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next permitted gate:** `U07 Implementation Readiness Re-Evaluation` at a separately audited source/CA/owner exact head, with strict distinction between acceptable *design* and unavailable *physical implementation*. A readiness review may and should conclude NOT_READY until Foundation audit, affected owner permissions, physical transactional resources and verification authority are established; it must not infer source implementation from design acceptance. If the project first chooses to design/remediate upstream CAs, those remain separately governed actions.

No author design modification, source/test/migration changes, executable test runs, merge or production work in this independent review.
