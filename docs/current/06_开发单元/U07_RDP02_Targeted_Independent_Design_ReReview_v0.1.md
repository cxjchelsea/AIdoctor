# U07-RDP-02 Targeted Independent Design Re-Review v0.1

> Date: 2026-10-08
> Target: [PR #271](https://github.com/cxjchelsea/AIdoctor/pull/271)
> **Reviewed exact head**: `0346ffe7701bc441fef1c7ad48e1fcd231c5be31`
> Prior design head: `8ce3c2ca140674c2e723b4c40e8a37ed25371216`
> Prior independent review: [PR #272](https://github.com/cxjchelsea/AIdoctor/pull/272)
> Main reference: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Verdict: REVISE_REQUIRED / 1 OPEN, 1 CONDITIONAL_DESIGN_RESOLUTION.** This review is design-only.

## 1. Exact-head evidence / review method

GitHub comparison confirms **one commit**, **one Markdown file** changed (123 additions / 10 deletions), no Runtime or schema change. Read RDP-02 §5, new §§5.1/5.2, §8.1.1/§8.2, expanded F8-T28..34, and original review blocker requirements. Source evidence from main: `ConsultationRepository.findByIdForUpdate` uses `PESSIMISTIC_WRITE`, `ConsultationRecord.rowVersion` has `@Version`; `ConsultationWaitTransitionService.establish` uses `SERIALIZABLE` with Consultation lock. None of those establish U15 sharing the same lock.

## 2. BF-U07-RDP02-IR-01 — First-verdict temporal precedence

**Decision: PARTIALLY_REMEDIATED / BLOCKER OPEN**.

The design now clearly decides `T1` receipt, `T2` authoritative expiry, `T3` first F8 decision => **EXPIRED**, rather than defaulting to REJECTED or using arrival-before-deadline as an implicit right. Cancellation-before-first-verdict => REJECTED. Original durable ACCEPTED remains immutable while later apply needs a fresh fence. Historical duplicate remains zero-effect. F8-T28..32 cover the intended relations.

**Residual contradiction / missing implementable invariant:** §5.1 requires eligibility at the **actual F8 transaction commit point** and says `t_decision` is a database-authoritative **transaction time / commit ordering reference**. But §8.1.1 checks the expiry/deadline *inside* the transaction, **before** persisting and committing. A database transaction can hold the Consultation lock while the deadline is reached solely by the passage of time. No U15 writer need execute to advance `row_version` in that interval. E.g.:

```text
deadline = 10:01:00.000
F8 acquires Consultation lock and checks DB time = 10:00:59.999
F8 persists ACCEPTED and commits at 10:01:00.050
(no U15 write occurred during lock)
```

The proposed protocol could persist **ACCEPTED** even though its own §5.1 rule says first-verdict commit at/after deadline must be **EXPIRED**. Acquiring a shared lock does not fence a *clock threshold* or manufacture actual commit timestamp in advance. This defect is distinct from U15 cross-owner participation.

**Required targeted correction:** select one physically enforceable temporal linearization point with proven clock source (e.g., an authoritative DB time check at a named serialized decision-finalization point and define that as the business decision instant, not the unknowable later commit instant), **or** provide an independently reviewable database-level mechanism that makes first-verdict commit-time deadline eligibility enforceable. Freeze exactly what happens at equality, near-boundary checks, unknown time source, and commit uncertainty. Align `§5.1`, `§5 P2`, `§8.1.1`, `§8.2` and F8-T28..34; add check-before-deadline/commit-after-deadline oracle. Do not claim the existing Consultation lock alone solves temporal passage.

## 3. BF-U07-RDP02-IR-02 — Shared F8/U15 owner fence

**Decision: CONDITIONAL_DESIGN_RESOLUTION / original ambiguity removed; UPSTREAM BLOCKER REMAINS**.

The remediation selects **one** V1 Consultation `PESSIMISTIC_WRITE` lock as the ordering primitive, with Consultation row/version, wait effect, U15 terminal generation, F8 claim generation, deterministic lock ordering, F8DecisionLedger + F8WaitAnswerAuthority atomicity, and rollback-only reconciliation. This addresses the original "F8 lock **or** equivalent CAS" ambiguity.

The design does **not** pretend U15 uses this lock: `CA-U07-RDP02-U15-SHARED-FENCE-01` is correctly registered `REQUIRED / NOT_DESIGNED / NOT_AUTHORIZED`. The legacy main lock examples cover U06, not U15. The F8 design is **NOT_APPLICABLE for real execution** unless U15/F3 business terminal owners share the exact barrier or an independently reviewed controlled amendment supplies equivalent global ordering. A separate DB/transaction domain fails closed.

Therefore IR-02's **RDP-02 choice-of-mechanism design** is provisionally resolved and can be conditionally closed as a registered upstream capability gap, **not as physical concurrency verification**. The CA must be designed/reviewed and actually fulfilled prior to positive U07 Implementation Readiness. If omitted from RDP-05/aggregate gate, reopen this finding.

## 4. Regressions / inherited boundaries

| Control | Result |
|---|---|
| Same canonical replay, one durable decision | PRESERVED |
| ACCEPTED-but-not-APPLIED is not DUPLICATE | PRESERVED |
| Four business verdicts; DEFER only operational | PRESERVED |
| U15 owns expiry/cancellation, F8 does not mutate lifecycle | PRESERVED |
| Original ACCEPTED not rewritten by P02 failure | PRESERVED |
| F3/P01/K09/U02 owner boundaries | PRESERVED |
| RDP-01 Foundation reference audit | REQUIRED / NOT_PASSED |
| U06 Eligibility issuance CA | REQUIRED / NOT_AUTHORIZED |
| U15 shared fence CA | REQUIRED / NOT_AUTHORIZED |
| Runtime implementation/test execution | NOT_DONE / NOT_CLAIMED |

## 5. Formal result and next gate

```text
U07-RDP-02 Targeted Independent Design Re-Review = REVISE_REQUIRED
Exact reviewed head = 0346ffe7701bc441fef1c7ad48e1fcd231c5be31

BF-U07-RDP02-IR-01 = OPEN / TEMPORAL_LINEARIZATION_GAP
BF-U07-RDP02-IR-02 = CONDITIONAL_DESIGN_RESOLUTION / UPSTREAM_U15_FENCE_REQUIRED

CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED

U07-RDP-02 = DESIGN_CANDIDATE / NOT_FROZEN
B-U07-RG-02 = OPEN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient traffic = BLOCKED
```

Next governed step: **U07-RDP-02 Second Targeted Design Remediation**, limited to the first-verdict **time linearization** defect; then an exact-head **Second Targeted Independent Design Re-Review**. No merge, runtime code edits or implementation authorization.
