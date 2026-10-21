# U07-RDP-05 Independent Design Review v0.1

> Review date: 2026-10-08
> Design PR: [#283](https://github.com/cxjchelsea/AIdoctor/pull/283)
> **Exact reviewed design HEAD:** `38eceebe3ad84dda37a78326e58f8b0576bbfa55`
> **Exact file blob:** `be9852c3087bdf9be0d7f86702346a016eb73114`
> Review base: `b536221bcb66cbce43e559093431cb07631d78f3` (RDP-04 conditionally accepted design review)
> Source baseline inspected: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Verdict: REVISE_REQUIRED — THREE DESIGN CONTRACT BLOCKERS.**
> Design review only. No implementation authorization, code changes, tests, merge, runtime/PHI or production.

## 1. Scope and evidence

Independently fetched PR #283 and the whole design document (§§1–16), reviewed the U07 Unit Spec dependency row and RDP-04 independent design acceptance, and compared source-level reuse inventory with previously inspected Foundation event ledger, RuntimeBindingRecord, Runtime Thread/Checkpoint/Wait services, U06 SyntheticP01 and U06 Trace/Delivery. The document contains **36 unique, ordered, UNEXECUTED design oracles CAP-T01..CAP-T36**. Existing source examples are **bounded** and never prove a complete U07 runtime implementation or physical MySQL/Oracle transaction co-location.

### Accepted design aspects
- Platform row `P01 + P02 + P05 + P06`, with **no mandatory P03/P04 clinical AI** for U07 lawful-resume decision.
- F8 business verdict, P02 runtime, F3 semantic Question owner, K09/P01 commit, Consultation lifecycle, U15 terminal truth, U02 Clinical Facts, P05 Trace remain separate authorities.
- Metadata-only U06 checkpoint and projected U07 eligibility hash are **not** evidence of executable P02 restoration or original issuance.
- Stage C single F3-governed P01 patch, Stage E atomic APPLIED+Outbox, P02 restore-only U15 start grant, parked landing barrier, Stage F distinct U15 dispatch grant and U02 consumer idempotency are appropriately carried as physical dependencies.
- Source-verified reusable primitives, U06-scoped implementations, not-demonstrated U07 capabilities and controlled amendments are not mislabeled as ready.
- PROFILE-B synthetic only; PROFILE-A/PHI/patient/production expressly blocked.
- Foundation Reference Audit and all nine named CAs remain REQUIRED/NOT_AUTHORIZED or NOT_PASSED.

## 2. BF-U07-RDP05-IR-01 — capability readiness requires post-effect owner receipts

**BLOCKER / PRECONDITION_VS_RESULT_AUTHORITY_CONFLATION.**

§5 Requiredness says a stage is READY only if **every** required item has 'owner receipt' along with capability version, executable adapter and producer permission, and §10 uses 'owner receipt' as a general capability quality gate. But the applicability matrix (§6) marks `F8 committed ACCEPTED` R for F8, `Consultation ACTIVE owner effect` R for Stage C/D and `APPLIED+Outbox same guard-DB COMMIT` R for Stage E. These **effect outputs cannot be prerequisites to their own first execution**. A valid F8 candidate must be evaluated without a prior ACCEPTED receipt; Stage D can start without a prior ACTIVE receipt, and E cannot require a prior APPLIED receipt before committing APPLIED.

**Required author remediation:**
1. Freeze distinct `CAPABILITY_PREREQUISITE`, `PRIOR_STAGE_EFFECT_EVIDENCE`, `CURRENT_STAGE_EXPECTED_OUTPUT` (and `FUTURE_STAGE_DEPENDENCY`) dependency roles. A prereq requires installed/verifiable owner adapter, permissions, profile, transaction model and contract; prior-stage evidence requires positive receipt; the current stage's **output receipt is checked after its authoritative commit**, not as admission.
2. Split matrix rows or provide per-stage phase columns (e.g. `F8_EVALUATION_PRE` vs `F8_RESULT_POST`; `STAGE_C_PRE`, `C_COMMIT_POST`, `D_PRE`, `D_COMMIT_POST`, `E_PRE`, `E_COMMIT_POST`), so nobody interprets R as needing a future committed effect.
3. Define pre/post stage resolver outputs, monotonic effect evidence transition, and replay readback; add tests where first-ever F8 decision/first-ever Stage E can qualify without fabricated prior receipts, but post-commit cannot advance without them.

## 3. BF-U07-RDP05-IR-02 — Stage F grant and U02 consumer acceptance conflated

**BLOCKER / DISPATCH_GRANT_VS_CONSUMER_PHASE_COLLAPSE.**

§5 `requested_stage` correctly has `U02_DISPATCH_GRANT` and `U02_CONSUMER_ACCEPT` separately; §6 compresses them into one 'Stage F/U02' column and marks U15 grant as G while U02 consumer acceptance/receipt lookup as R. This collapses:
- capability for idempotent **U02 admission/receipt query** (required *before* any physical send),
- owner-issued **U15 dispatch grant** (produced by Stage F under shared lock),
- actual **U02 receipt** (not obtainable until delivery or exact replay),
- subsequent U02 clinical fact interpretation (separate owner and authorization).

As written, an implementation could require an impossible consumer ACK before grant, or treat a U15 grant as evidence that U02 admitted/committed clinical facts.

**Required author remediation:**
1. Distinguish `F_GRANT_PRE`, `F_GRANT_POST`, `F_DISPATCH_PRE`, `U02_ACCEPT_POST`, `U02_FACT_OWNER_LATER`. Frozen PENDING Outbox + U15 policy/guard and an independently proven U02 **idempotency capability contract** are prerequisites; grant and consumer receipt are distinct post-effect outputs.
2. Positive grant before network send must not be equated with consumer ACK or Clinical Fact. Require stable handoff ID, consumer authority/provenance, readback/query-by-id, exact replay handling, and explicit fail-closed for unsupported consumer.
3. Add negative oracle for APPLIED+PENDING without grant, grant present without ACK, and ACK lost after U15 terminal with only same-grant replay.

## 4. BF-U07-RDP05-IR-03 — positive assessment snapshot lacks per-owner commit-time invalidation

**BLOCKER / SNAPSHOT_CURRENTNESS_TOCTOU.**

§5 correctly says a cached `READY_FOR_STAGE` cannot outlive its bounded U15 generation. However `U07DependencyAssessmentV1` only explicitly materializes a `current_u15_terminal_generation` and generic `owner_snapshot_epoch` plus optional dependency version vectors. Its consumer protocol does **not freeze the precise owner-specific versions and commit-time revalidation** for all mutable prerequisites (F3 policy/Question, P01 field grant, P06 binding/Scope, P02 ownership, U15 policy and U02 consumer permission). The selected shared-lock topology (§8) solves some Consultation/U15 races but not arbitrary Registry/grant updates between capability assessment and downstream commit.

Example: `READY_FOR_STAGE` is emitted for Stage C, then the P01 producer field grant is revoked or F3 policy release is superseded while U15 generation remains constant. Reusing READY without P01 owner permission re-evaluation bypasses authority. A single generic snapshot epoch does not prove all owners co-transactional.

**Required author remediation:**
1. Freeze a dependency-level immutable `authority_version_or_epoch` / signed snapshot ref **per owner**, plus `required_recheck_point` mapping to RDP-01 admission, F8 linearization, P02 start-grant commit, P01 Stage C transaction, D ACTIVE, E APPLIED+Outbox, Stage F grant and U02 consumer admission.
2. Define whether each authority can be verified **under the same DB transaction**, by a version-fenced owner commit, or by a provider-signed bounded lease; never assert Consultation row lock automatically fences independent P06/F3/permission stores.
3. On changed/revoked/missing authority: `BLOCKED_AUTHORITY` or `RECONCILIATION_REQUIRED`, no novel effects; no optimistic reuse of `READY_FOR_STAGE`. Add racing revocation and stale binding oracles and demand RDP-05/RDP-06 physical evidence.

## 5. Requiredness and scope observations (non-blocking)

- §8's `SINGLE_GUARD_DB_STAGED_SAGA_V1` is a **selected design prerequisite**, not evidence that Foundation, Consultation, P01, P02 grant, U15 and Outbox actually share a DB/transaction manager. If co-location fails, V1 NOT_APPLICABLE and independent controlled redesign required.
- Current vs historical P06 bindings and certified statement-time DB clock checks are correctly fail-closed; require MySQL/Oracle real test evidence later.
- A pending controlled amendment should be represented as BLOCKED for the affected stage. When a later stage remains inapplicable, it may be NOT_YET_APPLICABLE for *this stage* while still blocking **overall U07 implementation readiness**; the author should preserve this distinction.
- P03/P04 non-applicability for F8/P02 remains consistent with Unit Spec. Do not infer non-applicability to U02 clinical reasoning.
- The code inventory uses verified snippets, not an exhaustive current-head physical audit. RDP-05 independent PASS alone could not authorize U07 implementation.

## 6. Review outcome and exact gate

~~~text
U07-RDP-05 Independent Design Review = REVISE_REQUIRED
Exact reviewed HEAD = 38eceebe3ad84dda37a78326e58f8b0576bbfa55

BF-U07-RDP05-IR-01 = OPEN / PRECONDITION_VS_RESULT_AUTHORITY
BF-U07-RDP05-IR-02 = OPEN / STAGE_F_U02_PHASE_COLLAPSE
BF-U07-RDP05-IR-03 = OPEN / OWNER_SNAPSHOT_REVALIDATION

B-U07-RG-05 = OPEN / NOT_CLOSED
U07-RDP-05 = DESIGN_CANDIDATE / NOT_FROZEN

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

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
~~~

**Next step:** `U07-RDP-05 Targeted Design Remediation` on design PR #283, limited to (a) pre/post-effect role and matrix, (b) F grant/transport/U02 consumer phases, (c) per-owner versioned revocation at exact commit boundaries. Then exact-head Targeted Independent Design Re-Review.

No runtime source, tests, schema, merge, PHI, patient flows or production changes accompanied this review.
