# U07 Aggregate Controlled Amendment — Targeted Cross-RDP Design Remediation v0.1

> Date: 2026-10-08
> Author-side design amendment; NOT an authorization decision
> Input independent Aggregate Compatibility Review: [PR #289](https://github.com/cxjchelsea/AIdoctor/pull/289), exact review HEAD `5cbbc1996ca22b145be4db2f99b891a7a8b991bb`
> Selected aggregate baseline: RDP-01..06 all conditionally design accepted, aggregate **REVISE_REQUIRED**
> **Status: REMEDIATED_FOR_TARGETED_INDEPENDENT_AGGREGATE_RE_REVIEW / NOT_FROZEN / NOT_AUTHORIZED**
> Implementation, migrations, tests, PHI, live patient flows, merge and production = NOT_AUTHORIZED.

## 1. Authoritative scope of this controlled amendment

The amendment responds only to these two cross-RDP blockers:

| Blocker | Amendment | Resolved in the author-side candidate | Formal closure |
|---|---|---|---|
| `BF-U07-AGG-01` | `CA-U07-AGG-P02-START-GRANT-TX-01` | Explicit B0/B1/B2 owner transaction and replay protocol | NOT_CLOSED until exact-head independent aggregate re-review |
| `BF-U07-AGG-02` | `CA-U07-AGG-ADMISSION-T17-ORACLE-01` | Separate ordinary pre-commit rollback from legacy committed orphan | NOT_CLOSED until exact-head independent aggregate re-review |

No upstream U06, U15, F3, P01, P02 or U02 producer authorization is granted by this document. Those owners must separately approve new interfaces and permissions. Source-level feasibility, SQL dialect behavior and test evidence are **not verified**.

## 2. Selected end-to-end topology and invariant

One chosen topology only: `SINGLE_GUARD_DB_STAGED_SAGA_V1 / P02_B0_B1_B2_ATOMIC_START_V1`.

```text
RDP-01: Foundation + canonical binding in ONE physical DB transaction
  -> RDP-02: F8 first business verdict + winning claim under U15 guard
  -> RDP-03 Stage A: one root/wait apply claim (guard DB)
  -> RDP-03 B0: U07 commits immutable P02 request intent (guard DB)
  -> P02 B1:
       lock Consultation/U15 FIRST, then P02-owned Thread SECOND
       validate same root/wait, F8 winner, U15 deadline/generation,
           owner versions, checkpoint/proof and original binding
       single physical guard-DB COMMIT:
         P02 Thread RESTORE_CLAIMED owner mutation
         P02ResumeJournal RESTORE_START_AUTHORIZED
         P02ExecutionStartGrant AUTHORIZED_RESTORE_ONLY
  -> P02 B2:
       first re-read committed B1 owner triad;
       perform inert runtime restoration/re-hydration OUTSIDE B1 DB transaction;
       persist/read back RESUMED_READY_BUT_NOT_DISPATCHED and P02 owner result
  -> RDP-03 C: F3-approved ONE P01 Question/Gap/Pending patch
  -> RDP-03 D: Consultation ACTIVE under U15 guard
  -> RDP-03 E: APPLIED + unique U02 Outbox in ONE COMMIT
  -> RDP-03 F: U15-approved durable *distinct* Outbox dispatch grant
  -> synthetic U02 consumer, same handoff effect ID; no Clinical Fact inference
```

**No DB transaction spans physical P02 work or an external network send.** B1 is a short atomic **authorization and owner phase transition transaction**, not a claim of atomicity over the entire Saga. Although Thread, journal and grant are physically in the same guard DB for B1, the **owner and write permission remain P02**, not U07. U15 owns policy and terminal generation; P02 cannot self-authorize the U15 policy. U07 B0 is coordinator intent, not permission to execute. B2 P02 result is separate from B1 grant and does not itself authorize Stage C/D/E/F.

**Mandatory fail-closed condition:** if P02 Thread, owner journal or start grant cannot join **exactly the same real primary DB/transaction manager** as the U15 Consultation row, selected V1 = `NOT_APPLICABLE / NOT_READY` until separately governed replacement. No dual-write approximation, stale U15 snapshot, optimistic grant, or independent physical P02 execution bypass.

## 3. Required resource-level compatibility proofs

| Resource | Owner | Required frozen authority/evidence |
|---|---|---|
| Foundation canonical_business_event and U07 binding bytes | Foundation / U07 ingress | one outer same-DB commit; FK, unique identity, rollback and scoped readback |
| Consultation/U15 row and monotonic terminal generation | Consultation/U15 | same guard DB, consultation lock FIRST in all participating mutations |
| U07 B0 Stage-B intent | U07 coordinator | prior durable commit, original root/wait and stable P02 request identity |
| Runtime Thread claim | P02 | B1 owner CAS against original Run/Wait/Thread version, lock SECOND |
| P02ResumeJournal RESTORE_START_AUTHORIZED | P02 | one B1 commit with Thread claim and U15-approved start grant |
| P02ExecutionStartGrant AUTHORIZED_RESTORE_ONLY | P02 adapter + U15 authorization | same B1 commit; exact root/request/fence/policy/digest |
| P02 physical restore and parked owner result | P02 | after B1, no business nodes/tool/model/Clinical/U02 effects; readback |
| Stage C through F | F3/P01, Consultation, U07, U15 | distinct owner grants and fence/currentness; cannot inherit B1 authority |

**Lock order** for B1 is Consultation/U15 → P02 Thread → checkpoint/owner journal/grant keys. A future implementation must prove there is no reverse lock order from U06 wait entry, U15 terminalization, RDP-03 C/E, recovery or multiple workers. A similar row name across separate data stores is not co-location proof.

## 4. B0/B1/B2 failure authority matrix

| Race or crash window | Required result | Forbidden |
|---|---|---|
| B0 intent absent, B1 invoked | reject original authority; zero Thread mutation, grant or B2 | minting a new root by P02 |
| U15 terminal commits before B1 | no B1 grant/restore; retain historical F8 ACCEPTED | optimistic first restore |
| B1 Thread claimed, owner journal insert fails before COMMIT | both rolled back, no grant | committed partial Thread |
| B1 Thread/journal staged, grant insert fails before COMMIT | all rolled back | standalone AUTHORIZED Thread |
| B1 COMMIT outcome UNKNOWN | re-read matching Thread + journal + grant in new owner transaction | physical start before proven B1 |
| B1 committed, U15 later terminalizes, B2 not yet started | only same finite inert B2 may complete **if U15 owner approved this grant-first rule** | business continuation or C/D/E/F by old authority |
| B1 committed, B2 owner result UNKNOWN | reconcile owner state with same root, Run, grant and parked digest; no unclassified replay | fresh Run or duplicate side effect |
| B2 parked but Stage C not approved/current | remain parked; no scheduler/model/tool/U02 | treating RESUMED_VERIFIED as runnable |
| P02 records in separate DB from guard | NOT_APPLICABLE/NOT_READY | implicit distributed atomic transaction |

The normal F8 verdict, U15 terminal generation, original RDP-01 answer binding and P02 owner result retain their independent immutable sources; P05 Trace never substitutes for a durable owner receipt.

## 5. Canonical admission: two mutually exclusive T17 cases

`SINGLE_DB_INLINE_SYNTHETIC_V1` stays the only accepted RDP-01 storage design.

1. `U07-RDP01-T17 = ATOMIC_PRE_COMMIT_ROLLBACK`: Foundation row **inserted/flushed** then binding insert fails or process crashes **before outer COMMIT**. Neither row is durable, even if ORM flush returned success. A fresh owner query after rollback finds zero rows; no F8 or side effect.
2. `U07-RDP01-T17-LEGACY-ORPHAN = LEGACY_ORPHAN_QUARANTINE`: Foundation winner was **already durably committed** by a prior legacy/corrupt state, but no binding exists. Quarantine original winner under owner audit, do not fabricate binding from retry payload; no F8/P02/P01/U02.

`U07-VG-006` must inject (1) and require physical MySQL/Oracle rollback readback. `U07-VG-006-LEGACY-ORPHAN` must **preseed** a committed orphan explicitly and verify (2), not induce the orphan through a valid V1 transaction. Two cases have different initial DB states; expectation Oracle must not accept either result interchangeably.

## 6. Exact frozen-artifact amendment inventory

| Artifact | Targeted change | Non-change |
|---|---|---|
| RDP-01 | replace contradictory T17 expected result, add separate legacy committed orphan and CA scope note | no answer schema/fingerprint/identity policy change |
| RDP-03 | specify P02 B0/B1/B2, resource owner map, shared grant transaction vs independent physical work | Stage C/D/E/F owners and atomicities retained |
| RDP-04 | atomic B1 owner Thread+journal+grant, owner readback and inert B2 execution | no weakening parked dispatcher/rehydration proof or U15 grant-first policy |
| RDP-05 | declare mandatory physical co-location, per-owner version/phase and atomic versus historical evidence distinction | no reclassification of existing CAs as authorized |
| RDP-06 | correct VG-006 semantics, add LEGACY-ORPHAN and ten aggregate B1 crash/race design oracles, update dual-dialect requirement | base VG-001..060 remains intact; no runnable tests claimed |
| This file | selected protocol, approval authority, review/closure checklist | no authorization or implementation |

RDP-02 requires **no amendment**: F8 time/terminal/business verdict rules stay unchanged. RDP-06's new `U07-VG-AGG-01..10` and `U07-VG-006-LEGACY-ORPHAN` are additional **unexecuted** design cases; their future machine-readable Oracle/Fixture/Traceability entries need independent review.

## 7. Dependency and gate retention

```text
BF-U07-AGG-01 = REMEDIATED_FOR_RE_REVIEW / NOT_CLOSED
BF-U07-AGG-02 = REMEDIATED_FOR_RE_REVIEW / NOT_CLOSED

CA-U07-AGG-P02-START-GRANT-TX-01
  = DESIGN_REMEDIATED / NOT_AUTHORIZED / PENDING_INDEPENDENT_RE_REVIEW
CA-U07-AGG-ADMISSION-T17-ORACLE-01
  = DESIGN_REMEDIATED / NOT_AUTHORIZED / PENDING_INDEPENDENT_RE_REVIEW

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

U07 Aggregate Compatibility = REMEDIATION_CANDIDATE / NOT_ACCEPTED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

## 8. Exact-head targeted independent aggregate re-review checklist

- IR-AGG-01: Do RDP-03/04/05 **all select the identical** B0/B1/B2 topology and owner/transaction split?
- IR-AGG-02: Is B1 really one shared owner Thread+journal+start-grant COMMIT under Consultation/U15 lock, with no external Runtime effect inside it?
- IR-AGG-03: Is P02 the only Runtime owner despite guard DB co-location and is B0 incapable of granting start?
- IR-AGG-04: Are U15 terminal-first, grant-first, COMMIT_UNKNOWN, journal/Thread partial failure and B2 parked readback fail-closed?
- IR-AGG-05: Does every pre-commit admission crash roll back **both** canonical and binding rows without a ghost committed winner?
- IR-AGG-06: Is a genuinely preexisting committed orphan segregated as a different fixture/Oracle outcome?
- IR-AGG-07: Does RDP-06 add B0/B1/B2 cross-RDP cases, keep 60 base IDs, and require dual-dialect owner evidence?
- IR-AGG-08: Are all historic U06, Foundation, U15, P01, P02, U02 CAs and patient/PHI restrictions retained without accidental approval?
- IR-AGG-09: Does aggregate acceptance remain only design compatibility and **not** Implementation Readiness?

**Next permitted action:** `U07 Targeted Independent Aggregate Compatibility Re-Review` at the exact amended HEAD. This author amendment cannot self-close either Blocker. If a physical co-location assumption is false, review should return NOT_READY and request an independently designed alternative, not an optimistic compatibility PASS.
