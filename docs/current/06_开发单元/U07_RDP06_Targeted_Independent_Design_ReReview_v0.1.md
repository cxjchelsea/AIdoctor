# U07-RDP-06 Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-08
> Design PR: [#286](https://github.com/cxjchelsea/AIdoctor/pull/286)
> **Exact reviewed design HEAD:** `68acbf0da4f797e157b997d96da65352976232f2`
> Exact reviewed design blob: `aeb3fbc740631300395d73fbb4d95820b324871d`
> Prior design HEAD: `c7abe477cf69dde27641b6be0099df3423959f90`
> Prior independent review: [#287](https://github.com/cxjchelsea/AIdoctor/pull/287)
> **Verdict: PASS / CONDITIONAL_DESIGN_ACCEPTANCE ONLY.**
> No runnable verification, implementation authorization, runtime code, migration, PR merge, clinical traffic or production enablement.

## 1. Exact-head evidence and review method

Independently fetched the PR #286 head and complete contract including §§5.1, 5.2, 9.1 and 11.1; compared the previous design HEAD with the amended HEAD. The exact diff is **one Markdown file with 135 insertions and 16 deletions**. The original `U07-VG-001..060` catalog contains **60 unique consecutively numbered scenario families**. They are not implemented test cases or independently accepted machine-readable oracles.

Reviewed each of three findings in [PR #287](https://github.com/cxjchelsea/AIdoctor/pull/287), checked RDP-01 original `U07-RDP01-T*` (including suffixed entries), RDP-02 `F8-T01..41`, RDP-03 `U07-A03-01..45`, RDP-04 `P02-T01..44`, RDP-05 `CAP-T01..48`, and U07 Unit Spec §26 as **source case domains**. A high-level traceability coverage design is present; the final independent machine-readable mapping/coverage result is **not** supplied and is not falsely accepted as passed.

## 2. BF-U07-RDP06-IR-01 — authoritative deterministic Oracle and traceability

**CLOSED / CONDITIONAL_DESIGN.**

§5.1 freezes `U07ExpectedObservationV1` requiring exact fixture/branch/policy version, one exact expected status/phase, required and forbidden owner receipts, identity relations and causal-window side effect assertions. Conditional outcomes are broken into named reviewed fixture branches:
- `VG-016/CANCEL_FIRST_CONFIRMED` => `REJECTED` with U15 owner evidence;
- `VG-020/CLOCK_UNCERTIFIED` => operational `DEFER` and exact `F8_TIME_AUTHORITY_UNAVAILABLE`, no first durable verdict;
- `VG-035/PROOF_FULLY_CERTIFIED` => exactly bounded owner-certified deterministic reconstruction and a parked result;
- `VG-038/RESTORE_GRANT_POLICY_APPROVED` and `VG-045/DISPATCH_GRANT_POLICY_APPROVED` => corresponding **separately authorized** owner policies bound to grant-first race;
- `VG-050/F_GRANTED_DISPATCH_ACKED_NO_FACT` => separate intent/grant/U02 admission/fact proof.
Absent or unapproved owner policy cannot generate a positive expected result; `CONTRACT_EXPECTATION_GAP` applies and Tier-1 physical work is blocked.

§5.2 specifies a machine-validated `U07TraceabilityMatrixV1` with source case identities for RDP-01..05, Unit Spec §26, gaps RG-01..06 and reviewed branch IDs. Original suffixed T05A/B, T12A/B, etc. cannot be silently dropped. A many-to-one U07-VG mapping requires independently reviewed equivalence. Missing or unjustified exclusions fail closed.

**Conditional nature:** the actual full matrix and branch Oracle JSON **are not yet created, independently reviewed or proven complete**. A 60-row catalog is not a machine-oracle PASS. This is acceptable for a *design* gate only because it precisely defines the mandatory future closure and rejection conditions.

## 3. BF-U07-RDP06-IR-02 — pre-authorization negative tests versus governed physical verification

**CLOSED / DESIGN.**

§9.1 freezes distinct:
- `TIER-0 AUTHORITY_NEGATIVE_PRECHECK`: trusted manifest/authorization evaluator only; no physical U07 SUT, P02, P01, DB mutation, U02 sender, model/tool or clinical owner invoked. Legitimately missing audit/CA is expected to yield blocked/not-ready and may support specifically scoped negative-gate evidence.
- `TIER-1 GOVERNED_PROFILE_B_PHYSICAL`: actual isolated SUT, database and synthetic consumer, **only after** all participating owner authorizations, Foundation audit, profile permissions and exact source/oracle/fixture/runner gates are independently accepted.

The runner §10 now executes Tier-0 first and marks Tier-1 `NOT_AUTHORIZED` when applicable, without claiming overall PASS. Tier-0 evidence is typed `AUTHORITY_NEGATIVE_ONLY` and cannot be promoted to physical proof. This resolves the previous circular runner-start versus deliberately unapproved CA test (`VG-058`, `HG-09`).

Tier-1 remains fully gated; no synthetic fixture may forge an approval or turn an absent CA into granted authority.

## 4. BF-U07-RDP06-IR-03 — U02 exactly-one logical handoff, attempts and isolation

**CLOSED / CONDITIONAL_DESIGN.**

§11.1 freezes `U07U02EffectCountersV1` with separate original-root/immutable handoff identity and:
`logical_u02_handoff_intents_unique`, `dispatch_grants_unique`, `physical_send_attempts`, `synthetic_consumer_receive_attempts`, `consumer_admissions_unique`, `consumer_idempotent_replays`, `u02_clinical_fact_commits`, plus unauthorized external connection attempts and actual connections.

A successful ACK-free single synthetic delivery can count one logical intent, one grant, one physical send, one unique consumer admission and **zero U02 Clinical Fact commits**. A deterministic lost-ACK/retry fixture may have **two physical attempts but still one unique handoff and one admission**. Sender physical attempts, consumer receive attempts and unique admission counts must be asserted separately against independent owner receipts; producer ACK is not Clinical Fact.

The selected V1 sender is an **in-process synthetic U02 receiver** with pre-send interception, independent consumer admission/receipt evidence and global unapproved-socket tripwires. `actual_unauthorized_external_connections = 0` differs from `physical_send_attempts = 0`. Missing an interception surface invalidates evidence rather than silently producing a zero.

**Conditional requirement:** real consumer replay/receipt capabilities, sender interception coverage, and owner-approved post-U15-terminal grant-first dispatch are not implemented or independently demonstrated. Their CAs remain blocked.

## 5. Cross-RDP and authorization preservation

- Frozen U07 Admission/F8/P01/Consultation/P02/Outbox/U02 owner boundaries and immutable historical F8 verdict remain separate.
- U06 wait metadata is not a runnable checkpoint or owner-certified rehydration proof.
- P02 restore-only U15 grant is not RDP-03 outbound dispatch grant.
- RDP-05 stage PRE capability and POST owner effect receipts remain separate; no readiness cache bypasses final owner authority.
- Verification Oracle, Fixture, SUT observation and independent comparator are distinct authority domains.
- MySQL and Oracle actual statement-current semantics, lock ordering, same-guard DB topology, owner revocation races and negative external-effect traces require later source-pinned physical tests.
- PROFILE-B synthetic only; PROFILE-A/PHI/real patient/production blocked.

## 6. Outstanding hard gate inventory

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

U07TraceabilityMatrixV1 = DESIGN_DEFINED / NOT_CREATED / NOT_REVIEWED
Machine-readable Oracle/Fixture independent reviews = NOT_PERFORMED
U07 authoritative runner/physical test execution = NOT_IMPLEMENTED / NOT_EXECUTED
MySQL/Oracle and cross-owner transaction proof = NOT_VERIFIED
```

Do not read this RDP-06 design acceptance as a grant to implement/test live U07. Full positive verification `PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW` remains impossible while Tier-1 authorization and physical evidence are absent.

## 7. Formal gate decision and next authorized design step

```text
U07-RDP-06 Targeted Independent Design Re-Review
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

Exact reviewed HEAD = 68acbf0da4f797e157b997d96da65352976232f2

BF-U07-RDP06-IR-01 = CLOSED / CONDITIONAL_DESIGN
BF-U07-RDP06-IR-02 = CLOSED / DESIGN
BF-U07-RDP06-IR-03 = CLOSED / CONDITIONAL_DESIGN

B-U07-RG-06 = DESIGN_REVIEWED / IMPLEMENTATION_BLOCKED
U07-RDP-06 = CONDITIONALLY_ACCEPTED_DESIGN / PENDING_AGGREGATE_COMPATIBILITY
U07-RDP-01..05 = CONDITIONALLY_ACCEPTED_DESIGN

U07 Aggregate Compatibility Review = NOT_PERFORMED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

**Next design governance step:** `U07 Aggregate Compatibility Review` of the six independently reviewed RDP contracts, their current exact reviewed blobs, mutually consistent owner/effect/authorization semantics, and the open Foundation/CA/physical blockers. Subsequent actual physical development, Oracle/Fixture construction, evidence execution or merge requires its own authorized gates.

No source/runtime modification, executable verification, schema migration, design PR modification, merge, production or clinical action occurred during this independent review.
