# U07 D5-O2 Stage B — Post-#463 Baseline & Whole-TX Retry Coordinator Detailed Safety Contract v0.1

> Status: DESIGN CANDIDATE / REVIEW REQUIRED. **Not** implementation authorization, test authority, production readiness, or merge authorization.
> Baseline pinned: main `33c0914e843d7976c7ff2272338377aa4925f443` (PR #463 standard merge). Source PR #457 `eac72825ca80ad34406a1fbd620eed20c17db3fe` remains draft/unmerged. Review against the current exact HEAD again before any implementation.
> Scope: U07 D5-O2 transaction boundary, RACE-05 recovery admission, independent physical evidence. No patient data, external delivery, L1 PROCESS, production wiring, schema/SQL/isolation changes, or retry implementation.

## 1. Evidence provenance and what has *not* closed

- PR #463's six F-ISOLATED test-only files are merged into main. The three PR-head Java8/MySQL8 workflows passed, but these are **pre-merge PR SHA evidence**, not a distinct physical run on the post-merge main SHA. Git topology confirms main `33c0914e` parents `20a54b4a` and `bfaa33f7`.
- Post-main physical verification is **PENDING**. The merged `.github/workflows/u07-d5-o2-stage-b-forensics.yml` triggers only on `pull_request`, not `push` or `workflow_dispatch`. No completed Java8/MySQL8 workflow bound to `33c0914e` has been established.
- The original RACE-05 source-only success invariant is BOTH independent Effects must return FIRST-ATTEMPT `NEW_SUCCESS`, with complete six-table rows for each. A real `40001/1213` failure means this source-only invariant **fails**; it must not be reclassified as a pass.
- Prior real MySQL8 evidence: in a failed race, the 1213 victim rolled back with `[0,0,0,0,0,0]`; the other transaction committed with `[1,1,1,1,1,1]`. Exact lock-index cycle was **not captured**. F-ISOLATED `NO_EDGE_CAPTURED` is a legitimate result, not positive index attribution.
- Original `JdbcU06DeliveryStore.find(effectId)` joins only ledger, authority, intent, confirmation: it does **not** establish completeness of attempt + receipt. Existing four-table replay is `CONFIRMED_NOT_SAFE` and is a separate unresolved production-safety defect.

## 2. Goal and responsibility boundary

A hypothetical `WholeTxRetryCoordinator` is **caller-owned** and wraps one complete invocation from transaction begin to commit/rollback acknowledgement; it does not alter `JdbcU06DeliveryStore`, its SQL, the isolation level, DDL, or original first-attempt semantics.

```text
Caller / Workflow execution authority
   └─ WholeTxRetryCoordinator [future; currently not implemented]
      ├─ immutable request identity + source-policy snapshot
      ├─ attempt #1: NEW physical transaction -> Original Store -> completion acknowledgement
      ├─ classify known rollback vs unknown completion -> fail closed if ambiguous
      ├─ only on verified-rollback 1213: bounded backoff -> NEW physical transaction
      ├─ independent six-table Reader + identity verification
      └─ typed eventual-result distinct from source first-attempt evidence
```

No in-transaction external delivery; no implicit fallback to four-table `replay=true`; no retry from a transactional method that joined an existing transaction or where the outer caller still owns commit.

## 3. Required typed contracts (candidate, not implementation)

```text
RetryRequest {
  operationId, selectionEffectId, deliveryEffectId, deliveryId,
  confirmationEvaluationId, questionId, contentFingerprint,
  confirmationFingerprint, endpointRef, idempotencyKey,
  createdAt, immutableCommandDigest, policyVersion,
  invocationDeadline, callerAuthoritySnapshot
}
AttemptEvidence {
  attemptOrdinal, invocationId, transactionIdentity, connectionIdentity,
  begunAt, finishedAt, transactionOwner, isolationLevel,
  sourceOutcome, sqlState, vendorError, exceptionClassification,
  commitAuthority, rollbackAuthority, observedSideEffectFence,
  outerTransactionJoined, closedResourceProof
}
SixTableReadback {
  readerTransactionIdentity, readerConnectionIdentity, consistentSnapshot,
  effectScopedRowCounts[6], completeFieldIdentityPerTable,
  observedDeliveryEffectId, commandDigestMatch, readbackStatus
}
RetryDecision {
  eligible, reasonCode, remainingAttempts, remainingDeadline,
  nextAction in {RETRY_NEW_TX, COMPLETE_VERIFIED, STOP_TYPED_FAILURE, QUARANTINE_UNKNOWN},
  evidenceReferences
}
RetryOutcome {
  firstAttemptSourceVerdict, eventualVerdict, attempts,
  sixTableReadback, completionAuthority, terminalReason
}
```

Schemas must be frozen separately with exact field types, nullability and wire-compatibility review; the above names alone are not an executable contract.

## 4. Safety invariants and gates

- **TX-01 Ownership.** Coordinator must start a genuine independent transaction per attempt and observe its terminal outcome; no nested/joined caller transaction and no transaction-scoped work escapes into next attempt.
- **TX-02 Rollback proof.** Only a proven, completed whole-transaction rollback after MySQL 1213 / SQLSTATE 40001 can enter the retry-eligible class. SQL exception alone is not rollback/commit authority. Absent authoritative rollback acknowledgement => UNKNOWN.
- **TX-03 No unknown replay.** UNKNOWN_COMMIT, network disconnect around commit, timeout, cancellation, interrupted rollback, lost connection, missing attempt marker, contradictory readback => stop/quarantine and reconcile; **never blind retry**.
- **TX-04 Error precision.** Error 1205, 1062, generic 40001 (without verified MySQL 1213 deadlock and rollback), ordinary user/business exceptions, and observer error do not get auto-retried.
- **TX-05 Immutable identity.** Same Effect, selection, delivery, idempotency key, content and confirmation fingerprints, operation identity, and caller authorization snapshot across attempts. Mutation => stop.
- **TX-06 Budget.** Candidate bounds for test planning: max 2 retries (3 attempts total), total budget 15 seconds including backoff/readback, monotonic clock, deadline checked before attempts. These are proposed test defaults **not frozen production settings**. Never retry indefinitely.
- **TX-07 Six-table authority.** Success requires fresh independent Reader proof of exactly one internally consistent record in each of authority/intent/attempt/receipt/confirmation/ledger for the same effect and immutable command. Four-table `find()` replay result is not completion proof.
- **TX-08 Replay gap.** If any table missing or identities conflict, outcome is `INCOMPLETE_OR_CONFLICT`, not success or automatic retry. Existing false replay must be remediated in an independently authorized production scope before production use.
- **TX-09 External side effects.** No real notification/delivery, downstream tool call, or irreversible action in retryable transaction. A durable outbox/fencing design is a separate approval gate.
- **TX-10 Failure semantics.** Preserve FIRST_ATTEMPT_RACE05_FAILED where original first invocation saw 1213. EVENTUAL_COMPLETION_VERIFIED is a separate future test verdict; never rewrite the original source evidence.
- **TX-11 Trust boundary.** Performance Schema L0 observer edges are diagnostic only; they confer neither retry eligibility nor completion authority. L1 PROCESS is not authorized.
- **TX-12 Evidence & privacy.** Explicit allowlisted typed evidence, exact SHA/runner/transaction/reader provenance; no raw SQL, credentials, patient data, LOCK_DATA or unredacted stack traces in logs/artifacts.

## 5. Retry state transition contract

```text
CREATED -> ATTEMPT_IN_PROGRESS
ATTEMPT_IN_PROGRESS -> TX_COMMITTED_KNOWN | TX_ROLLED_BACK_KNOWN | TX_OUTCOME_UNKNOWN
TX_COMMITTED_KNOWN -> FRESH_SIX_TABLE_READBACK -> VERIFIED_COMPLETE | INCOMPLETE_OR_CONFLICT
TX_ROLLED_BACK_KNOWN + precisely classed 1213 + unchanged input + budget + side-effect fence
    -> RETRY_WAIT -> NEW_TRANSACTION -> ATTEMPT_IN_PROGRESS
TX_ROLLED_BACK_KNOWN + non-eligible / exhausted -> STOP_TYPED_FAILURE
TX_OUTCOME_UNKNOWN -> QUARANTINE_UNKNOWN (no automatic retry)
VERIFIED_COMPLETE -> COMPLETE_VERIFIED
INCOMPLETE_OR_CONFLICT -> STOP_AND_RECONCILE (no automatic retry)
```

No transition from unknown/incomplete/error to success without fresh authoritative evidence. Any failure to acquire a new transaction or separate Reader also fails closed.

## 6. Precise negative and positive verification matrix

| Case | Input / physical condition | Required outcome |
|---|---|---|
| RT-01 | 1213, proven rollback, eligible budget | Exactly one new transaction attempted |
| RT-02 | 1213 but rollback not authoritative | QUARANTINE_UNKNOWN; no retry |
| RT-03 | SQLSTATE 40001 not MySQL 1213 | STOP; no retry |
| RT-04 | MySQL 1205 / 1062 | STOP; no retry |
| RT-05 | connection disappears while COMMIT in-flight | QUARANTINE_UNKNOWN; no retry |
| RT-06 | transaction already joined outside caller | REJECT_OWNER; no retry |
| RT-07 | cancellation/deadline between rollback and new attempt | STOP; no next attempt |
| RT-08 | attempts/deadline budget exhausted | STOP; finite attempt count |
| RT-09 | any immutable command/authorization field changes | REJECT_IDENTITY |
| RT-10 | Store reports replay=true but attempt/receipt missing | INCOMPLETE_OR_CONFLICT; no success |
| RT-11 | duplicate six-table row or cross-effect IDs | INCOMPLETE_OR_CONFLICT |
| RT-12 | independent Reader unavailable | NOT_VERIFIED; no success |
| RT-13 | known commit + six-table exact fresh readback | COMPLETE_VERIFIED |
| RT-14 | 1213 victim rolls back; other unrelated effect commits | source first-attempt RACE-05 remains FAIL |
| RT-15 | physically distinct retry attempt after known rollback eventually commits | EVENTUAL_COMPLETION_VERIFIED only when RT-13 holds |
| RT-16 | side effect emitted in retryable region | BLOCK; fail closed |
| RT-17 | observer lacks/grants extra privilege | observer result rejected; no control-plane effect |
| RT-18 | log/artifact injected SQL/secret/patient marker | reject upload; no unsafe disclosure |
| RT-19 | retry tries reusing JDBC connection/transaction after rollback | BLOCK |
| RT-20 | crash between terminal TX and readback | recovery must reconcile; no blind retry |
| RT-21 | original nonempty lock edge with no same-epoch reverse edge | PARTIAL_EDGE_ONLY; no cause proof |
| RT-22 | deterministic same-epoch two-writer opposing lock edges with real 1213 | may attest bounded graph only, never grant retry alone |

Every physical test must record Java 8 / MySQL 8 versions, clean schema, exact main SHA, test SHA, independent connection IDs, repeat number, exact outcome, and independently validated synthetic artifacts.

## 7. Post-#463 main physical baseline gate (separate from retry)

`BASELINE-01` Exact main checkout `33c0914e...`; assert `git rev-parse HEAD` equals bound expected commit (not PR merge ref). Trigger via a separately reviewed main-enabled one-shot CI runner or explicitly authorized isolated runner. **Existing PR-only workflow cannot certify this**.

`BASELINE-02` Java 8 compile + F-ISOLATED real disposable MySQL 8 L0 observer test; record precise workflow/job run, versions, privileges, no L1.

`BASELINE-03` Verify 12 original adversarial mutants, deterministic graph/failed upload negatives, actual Actions fail-injection skipped artifact, safe upload only on validated success.

`BASELINE-04` Independent post-run ZIP/CRC/SHA256, console SQL redaction, six-table victim vs winner consistency, SHA/run/reader identity, no false graph promotion.

`BASELINE-05` Distinguish `POST_MERGE_SOURCE_INTEGRITY=PASS` (already achieved) from `POST_MERGE_PHYSICAL_BASELINE=PASS` (not achieved at this document's creation). Do not substitute PR-head prior evidence for exact-main execution.

## 8. Blockers and authorization decisions

- **B-01** Main-only physical verification not executed. A controlled CI runner / trigger amendment requires separate review and authorization.
- **B-02** Four-table replay safety gap confirmed in original Store. No production retry or eventual-completion acceptance may rely on it.
- **B-03** Transaction ownership and authoritative rollback/commit proof are not yet specified as concrete integration-point contracts. Design a precise adapter seam, not an internal Store retry.
- **B-04** Crash/unknown-commit reconciliation, fresh six-table consistency and side-effect fence have no authorized physical implementation.
- **B-05** RACE-05 source-only original failing attempt is historical evidence, not a passing invariant. Exact index-level deadlock cause remains unproven; it is useful diagnostic follow-up, **not** proof required for narrowly scoped test-only retry design.

**Decision:** `MAIN_PHYSICAL_BASELINE=PENDING`; `WHOLE_TX_RETRY_DESIGN_CANDIDATE=READY_FOR_INDEPENDENT_REVIEW`; `DESIGN_FREEZE=NOT_GRANTED`; `IMPLEMENTATION_AUTHORIZATION=NOT_GRANTED`; `PRODUCTION_RETRY=NOT_AUTHORIZED`; `B2/REAL_D5=NOT_READY`.

## 9. Proposed next gates

1. Independent exact-HEAD review of this design, especially rollback authority, transaction owner, retry budget and four-table replay.
2. Controlled main-only physical runner design, exact-main test-only authorization, and physical evidence.
3. Revise typed contracts from reviewer findings; independently re-review and formally freeze once blockers close.
4. Only after separate explicit authorization may a **test-only**, disposable-DB coordinator prototype be contemplated. Production Store fixes, SQL/isolation changes, external delivery and B2 remain out of scope.
