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

---

## 10. R2-D01 remediation — concrete transaction-owner / authority seam (design candidate)

**Entry boundary.** A future `CallerOwnedWholeTxAttemptPort.executeOnce(ImmutableDeliveryCommand, AttemptId, Deadline)` is invoked from a **non-transactional** caller; it must test `TransactionSynchronizationManager.isActualTransactionActive() == false` and no bound transaction resource for the target transaction manager *before attempting*. Reject `AMBIENT_TX_PRESENT`. Do not infer that the existing `JdbcU06DeliveryStore.reconcileConfirmed` annotation creates a new transaction when invoked directly through `new JdbcU06DeliveryStore(jdbc)`: the Spring proxy is absent. The future port must own one injected `DataSourceTransactionManager` and wrap exactly one `TransactionTemplate` configured with SERIALIZABLE and REQUIRES_NEW only **after rejecting an existing outer transaction**. `REQUIRES_NEW` alone is not proof of isolation or source authority.

**Critical return boundary.** Returning from `TransactionTemplate.execute` means transaction-manager commit processing returned, but callback return alone is not commit proof. The port returns a typed outcome only after transaction-manager completion, resource unbinding, and a separate six-table readback. An exception propagating through execute is not by itself an authoritative rollback certificate. For a verified 1213 victim, require (a) exact error chain; (b) transaction manager's afterCompletion status `ROLLED_BACK` for that attempt; (c) no rollback/cleanup exception; (d) connection/resource ownership release; (e) independent readback `ZERO_SIX` scoped to **that exact fresh effect**. If any condition is absent/conflicting: `TX_OUTCOME_UNKNOWN`; do not retry. This conservative definition deliberately rejects safe but unverifiable attempts.

**Rollback-victim limitations.** An independent readback of six zero rows is supporting evidence, not sufficient proof of rollback when an effect may have pre-existed or another writer is concurrent. This coordinator is only admissible for an immutable, previously absent synthetic effect validated *before* first attempt. Any pre-existing identity or concurrent identical-effect writers makes the negative inference invalid and must stop/reconcile.

**Commit recognition.** Only a successful transaction-manager terminal signal followed by independent, consistent six-table readback is `COMMITTED_AND_VERIFIED`. If the commit returned but readback is partial/unavailable => `COMMIT_UNVERIFIED`; no retry. If commit connection lost => `TX_OUTCOME_UNKNOWN`, even when later readback might show rows; an explicit reconciliation authority must handle it.

**Ownership authority:** `AttemptOwner` = caller-approved test-only attempt executor; `TransactionManager` = terminal lifecycle signal producer; `ReadbackVerifier` = independent six-table evidence producer; `RetryCoordinator` = policy consumer only; `L0Observer` = diagnostic consumer (no retry policy vote). Every typed evidence object carries `sourceKind`, `runId`, `attemptId`, `txCompletionStatus`, `connectionId`, `capturedAt`, `authorityVersion`, and `evidenceDigest`. Unknown fields/enums fail closed.

## 11. R2-D02 remediation — precise classifier, budget and transition precedence

Classify exceptions by traversing the bounded cause chain including `SQLException.getNextException()`, with cycle detection. `DEADLOCK_1213` requires observed MySQL `errorCode==1213` AND `SQLState=="40001"`; ambiguous SQL error mixes, translation failures, missing state, conflicting nested codes => `UNKNOWN_ERROR`. `1205`, `1062`, generic `40001`, timeouts and `DataAccessException` text never grant admission. Neither `1213` nor a complete rollback certificate alone guarantees eligibility; all TX-01/02/03/05/06/09 gates must pass.

Order of decision: **(1)** cancellation/deadline; **(2)** ambient transaction/authority conflict; **(3)** unknown commit or uncertain rollback; **(4)** identity/scope/side-effect fence conflict; **(5)** precise error category and confirmed rollback; **(6)** budget; **(7)** new attempt admission. An unknown or unsafe state dominates retry eligibility. Backoff uses monotonic clock, never extends deadline; interruption before/during wait terminalizes to `CANCELLED`. No other worker is allowed to start a retry on the same operation while terminal reconciliation is unresolved.

Candidate data model: `AttemptPhase {ALLOCATED, ACTIVE, CALLBACK_RETURNED, COMMITTING, COMMITTED_KNOWN, ROLLBACK_KNOWN, UNKNOWN_TERMINAL}`, `RetryEligibility {ELIGIBLE_CONFIRMED_ROLLBACK_1213, REJECT_NON_RETRYABLE, REJECT_AMBIENT_TX, REJECT_IDENTITY, REJECT_BUDGET, REJECT_SIDE_EFFECT, QUARANTINE_UNKNOWN}`, and `EvidenceAuthority {TX_MANAGER, READBACK_VERIFIER, CALLER_FENCE, DIAGNOSTIC_ONLY}`. Lifecycle recording is **post-fact**, not an authorization to issue writes.

A retry never begins until prior attempt has terminated and been closed, including its JDBC connection, Spring synchronization and executor resources. Crash before terminal event => `UNKNOWN_TERMINAL`, and crash after commit-before-readback => `COMMIT_UNVERIFIED`; durable reconciliation is outside this prototype. Proposed test-only budget remains **at most 3 total attempts / 15 seconds including readback**, but exact clock source, budget and backoff parameters must receive independent freeze approval; they are not production defaults.

## 12. R2-D03 remediation — six-table independent proof contract

Run fresh reader **after** attempt termination, on a different physical connection (assert distinct JDBC CONNECTION_ID), autocommit-readonly consistent snapshot or explicit read-only REPEATABLE READ transaction. Snapshot boundary begins only after observed terminal attempt status. Read all six tables within one consistent reader transaction; no individual `READ_COMMITTED` snapshots stitched together. Verify exact effect key, delivery identity, authority selection effect, intent question/content/endpoint/idempotency, attempt ID/status, receipt ID/status/evidence type, confirmation evaluation ID/status/fingerprint, ledger canonical fingerprint/status and timestamps against immutable command. Check exact cardinality `[1,1,1,1,1,1]`, reject duplicates, nullable/missing mismatches, unknown statuses, orphan rows and extra same-effect rows. Use full row identities rather than a hash or row counts alone. For *rollback evidence* require `[0,0,0,0,0,0]` under qualified unique-effect precondition; if any nonzero or unconfirmed reader consistency, fail closed.

The readback must never call `JdbcU06DeliveryStore.find()` or trust its `replay=true` because its existing four-way JOIN omits attempt/receipt. A separate six-table readback implementation is mandatory. A first attempt that returns replay on four tables when missing attempt/receipt is `REPLAY_INCOMPLETE`, never eventual completion and never retry-eligible. Reader outage after known commit => `COMMIT_UNVERIFIED`; readback itself shall not emit side effects. A concurrent writer or uncertain snapshot => `INDETERMINATE`, no success.

## 13. R2-D04/05 remediation — side-effect fence and verdict records

`SideEffectFence` is a precondition supplied by a separate test harness that has **no real delivery adapters or external tool credentials**; only synthetic, no-op witness probes are permitted. Record capability discovery and denied-real-tool invocation counters; a missing or weakened fence, downstream callback registration, nonzero side-effect counter, or unknown delivery history forces `REJECT_SIDE_EFFECT`. Do not treat synthetic `u06_delivery_receipt` as proof of SMS/call/tool dispatch. Real outbox/external delivery idempotency, crash-safe fencing and permission models remain unimplemented / not authorized.

`FirstAttemptSourceEvidence` stores `RACE05_FIRST_ATTEMPT_BOTH_NEW_SUCCESS` or `RACE05_FIRST_ATTEMPT_FAILED_1213`, immutable once observed, independently of retries. `EventualCompletionEvidence` has `NOT_RUN | VERIFIED_FULL_SIX | REJECTED | UNKNOWN` and references the complete list of distinct attempts plus readback authority. One successful retry may establish eventual success while first-attempt source verdict stays failed. Never silently rewrite original PR #457 source-only RACE-05 gate.

Proof provenance requires exact main SHA, exact candidate/test SHA, Java8/MySQL8 runtime versions, workflow run/attempt, synthetic schema identity, operationId/effectId digest (not raw patient identifiers), all transaction and reader connection IDs, monotonic timestamps, writer outcome vector and evidence digests. White-listed fields only; preserve logs' no-raw-SQL/no-secret redaction.

## 14. R2-D06 — CI baseline triage and acceptance rule

PR #465 at `0569bfaab6ab402f20059967e45119a7c8ba98d2` ran #38338977269 (U02 Java failed, Python succeeded) and #38338977155 (U03 Java failed). Logs show `StageBP1BPartialLedgerTest` and `StageBP1BReplayConflictTest` `P1B_NOT_DIS...` test environment guards; in one full suite: **473 tests, 0 failures, 38 errors, 1 skipped**. Since the only PR diff was a new markdown file, **causation is not established**, but these CI checks remain failed. To close attribution: run *identical* failing job profile against exact base main SHA `33c0914e` on a correctly configured runner; compare Java version, test selector, P1B env gates, network/db configuration and test results. No claim of pre-existing baseline failure without this run.

A documentation PR may be assessed separately from these failures only by an expressly approved CI exception with exact base evidence and owner decision; absence of a changed Java file does not waive a failing required check.

## 15. Separate exact-main physical runner — design only, no trigger change authorized

**MR-01 Isolation:** New separately reviewed workflow file, not modification of the merged PR-only F-ISOLATED workflow. Event strategy must restrict execution to the exact `main` commit, avoiding arbitrary ref inputs and untrusted fork code; an explicit one-shot pinned commit checkout in a disposable CI environment is preferred. Minimum workflow permission `contents: read`, no write token, no patient data or production credentials. The runner must assert `git rev-parse HEAD == EXPECTED_MAIN_SHA` and `git rev-parse HEAD^2` is **not** used as PR identity. Freeze exact commit before a run.

**MR-02 Adapter incompatibility:** Current workflow sets `U07_F_PR_HEAD_SHA=github.event.pull_request.head.sha` and checks `HEAD^2`; Python `verify()` enforces `HEAD^2==head` and `sha==GITHUB_SHA`; Java `evidence()` binds `candidateHead=U07_F_PR_HEAD_SHA`. Thus a main-only runner cannot simply enable `push` / `workflow_dispatch` without violating existing proof contracts. Propose *separate test-only main provenance adapter* with `sourceMainSha=EXPECTED_MAIN_SHA`, `checkoutSha=EXPECTED_MAIN_SHA`, `runnerEvent=main_exact`, `runId`, `attempt`; version this schema distinctly from PR evidence. No main witness may be mislabelled as a PR HEAD witness.

**MR-03 Physical target:** Temurin Java8, pinned Docker MySQL8 image/digest where feasible, one-use localhost disposable schema, only `performance_schema.data_locks/data_lock_waits/threads` table-level L0 SELECT for observer, no PROCESS and no L1. Reuse original six owner-table DDL by verified blob IDs; no schema migrations or Store edits. Writer/readback roles remain least-privileged, credentials ephemeral and masked; no external network side effects.

**MR-04 Witness:** Execute original Store's two independent synthetic SERIALIZABLE writers, L0 observer, negative role grants, exception/rollback proof, actual six-table rows, redaction and same-epoch edge logic; accept `NO_EDGE_CAPTURED` as diagnostic uncertainty rather than causal proof. Test-only main adapter is not permitted to rewrite original PR RACE-05 first-attempt result.

**MR-05 Negative CI and artifact:** Independently inject command failure and verify `always()` safe admission rejection, upload skipped, no artifact. Three artifact files with *main-specific schema/version*, SHA/CRC independently downloadable, allowlisted fields, console logs sanitized. An artifact from PR-head workflow must never satisfy main-baseline closure.

**MR-06 Safety and scope:** No test code, workflow, manifest, script or privilege changes are authorized by this document. First submit independent detailed design review, then a separate **limited test-only implementation authorization** with frozen explicit file inventory and rollback strategy. Keep main `33c0914e` unchanged pending that process.

**Main baseline pass** requires: separately executed and independently verified evidence of actual checkout main SHA `33c0914e`, Java8/MySQL8 witness, role/redaction negative controls, no unauthorized artifacts, and precise provenance. If the expected main SHA changes, restart the exact-main gate rather than silently rebasing the target.

## 16. R2 remediation traceability and decision

| Finding | This design remediation | Implementation proof still missing |
|---|---|---|
| R2-D01 | Section 10 owner, Spring proxy, transaction terminal signal | Transaction-manager adapter and rollback signal physical tests |
| R2-D02 | Section 11 typed classifier, precedence, resource closure | Crash/cancel/deadline deterministic & physical oracle |
| R2-D03 | Section 12 independent consistent six-table reader | Full field-level reader implementation and fault injection |
| R2-D04 | Section 13 no-real-tool synthetic side-effect fence | Isolated capability/permission runtime enforcement |
| R2-D05 | Section 13 source-vs-eventual evidence contract | Typed schema registry and cross-run compatibility checks |
| R2-D06 | Section 14 CI failure attribution, base-run criteria | Exact-main comparison CI and owner authorization |
| Main runner | Section 15 MR-01..06 design | Separate file inventory, independent review, implementation authorization |

**Updated proposed gate:** `TARGETED_DESIGN_REMEDIATION=SUBMITTED_FOR_RE_REVIEW`; `DESIGN_FREEZE=PENDING_RE_REVIEW`; `EXACT_MAIN_RUNNER_DESIGN=CANDIDATE_ONLY`; `MAIN_PHYSICAL_VERIFICATION=PENDING`; `PRODUCTION_RETRY=NOT_AUTHORIZED`; `PR465_MERGE_AUTHORIZATION=NOT_GRANTED`.
