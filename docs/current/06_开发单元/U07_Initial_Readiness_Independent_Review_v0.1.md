# U07 Initial Readiness Independent Review v0.1

> Date: 2026-10-08
> Target: PR #263 — `U07_Initial_Implementation_Readiness_Gap_Review_v0.1.md`
> Exact reviewed head: `1722d7bcbece9909a64e5b1fac544ca433c41ca8`
> Reviewed base: `review/u07-unit-spec-independent-design-v1@9fb8726421ffdb4ed8784560150afcb716f0b4f9`
> Integration reference: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Review scope: independent documentation/readiness evidence review only, no implementation.
> Verdict: **REVISE_REQUIRED / 2 BLOCKERS**.

## 1. Scope, traceability and findings

PR #263 introduces one documentation-only gap-review file; no runtime code is changed. It maintains the frozen distinction between F8 business validity and P02 checkpoint compatibility; F3 lifecycle ownership; U02 fact interpretation and Clinical Truth ownership; and lack of U07 implementation or production authorization.

Evidence directly checked:
- [PR #263](https://github.com/cxjchelsea/AIdoctor/pull/263): exact diff, head and base.
- [PR #262](https://github.com/cxjchelsea/AIdoctor/pull/262): independent Unit Spec design review, PASS / NO_BLOCKER with NB-U07-01..03 deferred for RDPs.
- [PR #261](https://github.com/cxjchelsea/AIdoctor/pull/261): Unit Spec U07.
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/CanonicalBusinessEventLedger.java`: identity/idempotency, not F8 business verdict.
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointService.java`: wait checkpoint reservation, not full P02 resume.
- `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/U07ResumeEligibilityProjector.java`: eligibility projection, not authoritative subsequent business validity.

### IR-U07-01 — RDP-06 readiness acceptance condition improperly requires post-implementation test evidence

**Severity:** BLOCKER / REQUIRED FIX.

The section `B-U07-RG-06` currently defines acceptance as all required cases passing on a **pinned implementation SHA** with durable executable artifacts. But RDP-06 is one of the prerequisite **physical design contracts before implementation authorization**; requiring that completed implementation evidence in this gate would deadlock its own prerequisite.

**Required correction:** Two distinct gates:
- `RDP-06 CONTRACT DESIGN CLOSURE`: reviewed/verifiable oracle and fixture schema, test-plan case IDs and expectations, authoritative runner **design**, contract/authority manifests, profile and failure-mode constraints; implementation-dependent runner functionality may remain `IMPLEMENTATION_REQUIRED` and is recorded in the allowed implementation scope.
- `POST-IMPLEMENTATION VERIFICATION CLOSURE`: executable runner and fixture implementation, actual all-required-case run on exact implementation SHA, durable bundle digests, independent evidence review, and explicit merge authorization steps.

No relaxation of later test/evidence thresholds is allowed. Do not label runner execution PASS before implementation.

### IR-U07-02 — Blanket P0 labels conflate design blocker vs capability availability vs post-implementation proof

**Severity:** BLOCKER / REQUIRED FIX.

The document labels all six areas `P0 readiness design blockers`, while the issue rows mix (a) intentionally deferred design decisions; (b) existing-platform capability availability that was not exhaustively checked; and (c) future implementation/verification obligations. A severity designation is not a substitute for proof of a missing capability. It risks wrong authorization decisions and unnecessary shared-Runtime redesign.

**Required correction:** For each B-U07-RG-01..06 add:
1. **Kind:** `CONTRACT_DESIGN_REQUIRED`, `PHYSICAL_DESIGN_REQUIRED`, `RUNTIME_CAPABILITY_VERIFICATION_REQUIRED`, `IMPLEMENTATION_REQUIRED_LATER`, or combinations.
2. **Evidence strength:** `DIRECTLY_DEFERRED_IN_UNIT_SPEC`, `DIRECTLY_OBSERVED_IN_CODE`, or `NOT_YET_VERIFIED`.
3. **Closure gate:** precise artifact + independent reviewed condition to close design/readiness gaps; separate later implementation tests.
4. **Runtime status:** `PRESENT_REUSABLE`, `PRESENT_PARTIAL`, `NOT_ASSESSED`, `CONFIRMED_MISSING` only after a documented negative exhaustive check.

For the initial readiness gate it is valid to state `NOT_READY` because RDP physical contracts are not frozen; do **not** claim all six Runtime components are confirmed absent.

## 2. Cross-domain pass conditions

| Check | Decision | Why |
|---|---|---|
| Exact-head and upstream U06/main anchoring | PASS | Explicit source/base commit chain |
| Unit Spec independent review authority | PASS | PR #262 PASS is correctly scoped to semantic design |
| RDP-01 canonical event, replay and eligibility | PASS_WITH_RDP_DESIGN_REQUIRED | Correctly preserves Foundation ledger and U06 eligibility boundary |
| RDP-02 F8 authority / precedence | PASS_WITH_RDP_DESIGN_REQUIRED | Business decision separated from runtime status |
| RDP-03 K09/P01, F3, APPLIED, ACTIVE | PASS_WITH_RDP_DESIGN_REQUIRED | Owner boundaries protected; choreography intentionally open |
| RDP-04 P02, repair, durable U02 handoff | PASS_WITH_RDP_DESIGN_REQUIRED | Missing/stale checkpoint does not fabricate business rejection |
| RDP-05 platform dependency applicability | PASS_WITH_CLASSIFICATION_FIX | Requires evidence-based present/partial/unassessed classification |
| RDP-06 verification contract | REVISE_REQUIRED | Design-vs-post-implementation gate circularity |
| Six-blocker severity/evidence taxonomy | REVISE_REQUIRED | Blanket P0 masks proof vs design vs later execution |
| Implementation / production authorization leakage | PASS | NOT_GRANTED and PROFILE-A/live still blocked |

## 3. Remediation and re-review matrix

| Finding | Target | Closure requirement |
|---|---|---|
| IR-U07-01 | Section B-U07-RG-06 and gate/decision sequence | Distinguish RDP-06 design acceptance from post-implementation verified evidence; no implementation required to close design gate |
| IR-U07-02 | Blocker register B-U07-RG-01..06 | Classify each blocker by kind, evidence strength, platform status, design closure and later implementation obligations; avoid unverified absence claims |

Re-review must compare the remediated exact-head diff against these two findings and preserve all frozen invariants; if scope expands beyond the two items, run broader review.

## 4. Verdict

```text
U07 Initial Readiness Independent Review = REVISE_REQUIRED
Independent Review findings = 2 BLOCKERS (IR-U07-01/02)
PR #263 Readiness artifact = NOT ACCEPTED AS FINAL / PENDING REMEDIATION
U07 Implementation Readiness = NOT_READY
U07-RDP-01..06 Design Work = REQUIRED
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / live resume / real patient = BLOCKED
```

**Next governed operation:** targeted remediation on the original readiness review branch, followed by `U07 Initial Readiness Targeted Independent Re-Review`. This review does not authorize merging PR #263/#262/#261/#260, starting U07 implementation, production activation, or live patient processing.
