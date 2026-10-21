# U07 D5-O2 Stage B — B1-P1-C RACE-05 InnoDB Deadlock Forensics Evidence Design + Caller-Owned Whole-TX Retry Contract Independent Technical Review v0.1

Date: 2026-10-13. Status: **DESIGN REVIEW COMPLETE; NOT IMPLEMENTATION READY; NO RETRY/FORensics EXECUTION AUTHORIZATION.** This is a separately documented **same-assistant technical review**, NOT a review by an independent person or security sign-off. This Draft report does not itself grant implementation, merge, production or database access authority.

## 0. Exact baseline, evidence and intended scope

- Remote `main` verified identical to `20a54b4a7478641d8b8d8c12c6213b8e2ea73acc`. Draft implementation [PR #457](https://github.com/cxjchelsea/AIdoctor/pull/457) exact HEAD `eac72825ca80ad34406a1fbd620eed20c17db3fe` remains unmerged. Previous read-only deadlock/root-cause options review [PR #459](https://github.com/cxjchelsea/AIdoctor/pull/459) exact HEAD `528812b408a431d8a74ca6d2114122cb374c018c`.
- PR #457's existing C-ISOLATED first-attempt `race05` is still frozen to **two independent NEW_SUCCESS returns plus two complete coherent [1,1,1,1,1,1] groups without retry**. Three independent jobs in Actions run [#38329637853](https://github.com/cxjchelsea/AIdoctor/actions/runs/38329637853) at one exact source HEAD gave **PASS_8 / FAIL_RACE05 / PASS_8**, artifacts #11781323803, #11782500392, #11782018387, each checked in preceding rounds. On failed job #115076610917 Writer 37 returned actual `40001:1213`, zero original INSERTs, no normal TX return; Writer 36 returned NEW_SUCCESS, six inserts; independent Reader 38 saw [0,0,0,0,0,0] versus [1,1,1,1,1,1]. First-attempt RACE-05 **fails correctly** in this run. Exact InnoDB wait-for/index graph was **not** captured and must not be fabricated or inferred from overlapping timestamps.
- Original production `JdbcU06DeliveryStore.find()` performs only four-way ledger/authority/intent/confirmation joins, not Attempt or Receipt, and its separate confirmed replay false-positive is **OPEN / CONFIRMED_NOT_SAFE**. Whole-TX retry could amplify an unsafe replay path; it cannot be authorized for a production claimant before independent full-six safety remediation.

**Separation of review objects:** (F) *forensic evidence collection of original first-attempt deadlocks* in a new disposable MySQL 8 environment; (R) *future synthetic caller-owned whole-transaction retry policy*, as a distinctly named extension and separately gated acceptance plane. Do not extend original PR #457 or silently alter its failure verdict.

## 1. F — InnoDB physical deadlock forensics design

**F-01 Capture target and time ordering.** The test records its own worker `CONNECTION_ID()`, source role, transaction begin/first SELECT/first INSERT/error timestamps (monotonic for ordering), real `SQLException` SQLState/vendorCode and test run/attempt/scenario IDs. A separate tightly scoped **test-only observer connection** (never business Writer or Reader) is allowed *only after independent scope/privilege approval* to sample:
- `SHOW ENGINE INNODB STATUS`, specifically a fresh `LATEST DETECTED DEADLOCK` subsection;
- scoped `performance_schema.data_locks`, `performance_schema.data_lock_waits` and applicable `performance_schema.threads` correlations **while a conflict is active**, if available and nonintrusive;
- `SELECT @@version, @@version_comment, @@transaction_isolation, @@innodb_deadlock_detect` and relevant server settings.
Do **not** assume that a post-event `data_lock_waits` query must show the deadlock: the victim may already be rolled back and locks released. Likewise `LATEST DETECTED DEADLOCK` is globally last observed, can be overwritten by another transaction, and does not automatically belong to this test.

**F-02 Attribution requirements.** An accepted `FORENSIC_GRAPH_BOUND` result requires all: the observed 1213 incident; same isolated schema and exact MySQL server instance; a deadlock stanza time/version and transaction MySQL thread ids correlated to **both actual writer CONNECTION_ID values** (or a stronger independently evidenced thread correlation); matching original SQL table/query signatures and relevant index/record/gap locks; consistent timeline for incident capture. Raw `LATEST DETECTED DEADLOCK` alone without matching participant ids is **UNBOUND**, not causal proof. If a record is unavailable, raced away or overwritten, output `FORENSIC_UNAVAILABLE` or `FORENSIC_UNBOUND`, never invent a wait-for edge, index name or cause. Distinguish (a) `TRANSACTION_ROLLBACK_CONFIRMED` from (b) `INNODB_DEADLOCK_CYCLE_BOUND`; (a) can be true while (b) is unknown.

**F-03 Synthetic-data, privileges, and disclosure.** Use a new ephemeral schema/service/profile and port distinct from P1A/B/C. Business writer still SELECT/INSERT only; application reader SELECT only; narrow observer permissions scoped to disposable server, not production credential, and reviewed against which `performance_schema` or `PROCESS` privileges are truly required. CI root may configure scoped observer privileges but must not be passed into Java business code. Mask generated passwords. InnoDB status can include literal SQL, user/session identifiers and other active transactions: **do not upload raw unfiltered status dumps**, logs or PII. Use a parser that extracts and redacts bounded structural fields (correlated thread IDs, sanitized table/index identifiers, lock-mode and waiting/holding edges), with an explicit forbidden-string test against generated secrets; an optional raw dump can remain ephemeral and inaccessible to artifact upload. No patient data, no external delivery.

**F-04 Harness and evidence integrity.** Collect a source-generated `forensics.json` with source main SHA, candidate PR head, synthetic merge checkout, GitHub run/attempt ID, pinned original DDL blob identities, exact reader/writer/observer roles, session IDs, versions, timestamps, original SQL/SQLState/vendor code, observer query availability, parser version, bounded redacted lock-edge evidence and `attributionVerdict`. Original Surefire plus P1C first-attempt case evidence must stay independent; never synthesize successful outcome from a forensic parser. Parser rejects missing/unmatched role/connection IDs, stale deadlock stanza, wrong schema, leaked secrets, incorrect source/head identity or duplicated incident ID. `FORENSIC_UNAVAILABLE` may be an honest **data-collection result**, not proof of root cause or a pass for the graph-bound gate.

**F-05 Reproduction safety.** A finite predeclared number of disposable real Java8/MySQL8 attempts may attempt to capture a 1213; no unbounded polling, lock-held barrier, purposeful production writes or arbitrary `Thread.sleep` changes to manufacture successful behavior. Every attempt, including a run with no deadlock, must be recorded. Any CI timeout, unknown worker state, unexpected role, unscoped capture or evidence leak fails closed. Sampling can perturb scheduling and must be recorded in the evidence, not hidden.

## 2. R — Caller-owned whole transaction retry *contract*, not code

**Authority boundary:** A separate future *synthetic reliability coordinator* outside `JdbcU06DeliveryStore` owns retry admission and orchestration. It receives one immutable `U06DeliveryStore.Command` with stable selection/effect/delivery/idempotency/confirmation identities, and creates a fresh caller-owned, explicit `SERIALIZABLE` Spring transaction for **each full attempt**. It cannot inherit a caller ambient transaction, reuse the terminated JDBC connection/lock context as its execution authority, alter original SQL ordering, mutate request identity, or invoke an external side effect.

The Store retains the raw original behavior; after each attempt, a policy classifier consumes only authenticated source/error and transaction-status evidence. **Do not declare a failed transaction retriable merely because some nested exception contains `SQLState=40001`: verify actual vendor code 1213, transaction manager completed rollback, all resources released, no commit-ack ambiguity and no external action.** If any evidence is missing, block.

### Candidate transition contract

```text
RECEIVED
  -> IN_TRANSACTION(attempt=1)
     -> COMMITTED_RESULT -> FRESH_SIX_TABLE_ATTEST -> COMPLETED | INVALID_GRAPH_BLOCKED
     -> KNOWN_ROLLBACK
          -> if EXACT_1213 + UNWOUND + NO_SIDE_EFFECTS + BUDGET_LEFT
                -> RETRY_ELIGIBLE -> NEW_TRANSACTION(attempt=2)
          -> else RETRY_UNSAFE | STORAGE_CONFLICT | EXHAUSTED
     -> UNKNOWN_COMMIT / CANCELLATION / TX_RESOURCE_LEAK -> HOLD_FOR_AUTH_RECONCILIATION (NO RETRY)
```

- **Strict eligibility candidate:** MySQL InnoDB `1213` with expected SQLState `40001`, demonstrable whole-transaction rollback/termination, clean Spring thread/JDBC resources, no side effect/ACK uncertainty and remaining budget. Err on false negative. Exception wrapper mappings alone do not supply rollback attestation.
- **Forbidden generic admissions:** `1205` lock-wait timeout (statement rollback may differ from whole-TX rollback), `1062` duplicate key, typed `U06_DELIVERY_REPLAY_CONFLICT`, unknown vendor code, generic `40001` without `1213`, `08xxx` connectivity/unknown commit, cancellation/interrupt, worker/task timeout, partially observed state, missing role identity or undetermined commit response. Never “retry until green.”
- **Candidate budget (not frozen):** 2 total whole transactions maximum, a finite overall monotonic deadline and a deterministic bounded backoff/jitter policy. This is a proposal requiring safety review and failure/timeout accounting before implementation; deadline applies across *both* attempts, rollback/fencing, physical attestations and executor shutdown. A deadline or retry exhaustion returns a typed **not completed** state.
- **Success authority:** independent read-only physical full-six predicate for **each** completed Effect, including Attempt and Receipt identity/status; ordered original SQL evidence per attempt, fully matching committed original Command, no orphan/mixed rows; `transactionCallReturned=true` and graph evidence are both required. The known source four-join replay result is **not** sufficient for retry/reconciliation success. Do not claim external exactly-once or real Delivery success.
- **Durable identity/fencing:** repeated logical invocation must preserve effect/idempotency fingerprint and enforce one writer authority, protection from duplicate external execution. Multi-process restart/unknown commit remains out of scope until a separately authorized durable claim/checkpoint contract is proven. If the existing full-six source read is not repaired, only a test-only shadow observer can characterize states, never approve production business replay.

### Distinct result planes

| Plane | Example typed result | Meaning |
|---|---|---|
| Source attempt | `ORIGINAL_THROW_SQL_1213` | First attempt failed/rolled back; RACE-05 source-only verdict remains FAIL |
| Transaction authority | `ROLLBACK_CONFIRMED` / `UNKNOWN_COMMIT` | Whether a safe new transaction could ever be considered |
| Retry policy | `RETRY_ELIGIBLE`, `RETRY_UNSAFE`, `RETRY_EXHAUSTED` | Coordinator controls future attempts; never itself evidence of success |
| Full-six physical state | `COMPLETE`, `PARTIAL`, `EMPTY`, `UNOBSERVED` | Fresh bounded Reader result per Effect |
| Eventual task result | `EVENTUAL_COMPLETION_VERIFIED` or `NOT_COMPLETED` | New reliability extension only; does not change original source test |
| Forensics | `FORENSIC_GRAPH_BOUND` / `UNBOUND` / `UNAVAILABLE` | Index-level incident attribution, never a source/commit success flag |

## 3. Adversarial test obligations before retry can be authorized

**R-01:** A real independent-effect MySQL `1213`, verified victim rollback, then a *separate* bounded new full transaction on same immutable Command; if it returns normally, fresh six-table readback must verify both Effects. Preserve first-attempt red fact and per-attempt log.

**R-02:** Exhaust attempts/overall deadline; verify no success claim, no leaked TX resource/active worker, no extra partial group, no downstream side effect.

**R-03:** `1205`, `1062`, non-1213 `40001`, unknown commit, lost ACK, `08xxx` connection failure, timeout/cancel, wrong logical effect or changed fingerprint, partially populated six-table graph, duplicate caller, and failure to confirm rollback must all **deny** generic retry without interpreting “no row read” as proof of no commit.

**R-04:** A fake/missing `1213` or unattested rollback must not trigger the attempt. A real 1213 with no physically verified completion cannot be marked `EVENTUAL_COMPLETION_VERIFIED`. Record exactly which attempt won, commit/rollback observed, SQL error, all-column graph digest, connection/role/transaction identity, and source SHA.

**R-05:** The original first-attempt RACE-05 remains a separate test and may still FAIL on genuine deadlock. Its pass/fail cannot be rewritten by a post-failure coordinator. Existing P1-A/P1-B and four-join false replay regression must be rerun unchanged when future code is implemented.

## 4. Findings and readiness gates

| ID | Focus | Review finding | Disposition |
|---|---|---|---|
| **F-R05-01** physical graph provenance | Prior 1213 is physically established, actual wait-for graph not captured; global LATEST record might mismatch incident | **FORENSIC_DESIGN_DEFINED / CAPTURE_NOT_AUTHORIZED** |
| **F-R05-02** observer privilege and leakage | Need scoped ephemeral observer, raw-status redaction, test-only domain and proof of masked credentials | **BLOCKER_TO_PHYSICAL_CAPTURE** until exact grants/retention/file allowlist independently reviewed |
| **F-R05-03** reliable incident binding | Need real two-writer IDs and graph index/edge match; missing transient data_locks must not be inferred | **CONTRACT_DEFINED / PHYSICAL_VERIFICATION_PENDING** |
| **R-R05-01** caller-owned boundary | Complete independent TX per attempt, stable Command, no automatic Store modifications | **CONTRACT_DEFINED** |
| **R-R05-02** 1213 admission and rollback proof | Need typed rollback/no-commit observation separate from exception class | **BLOCKER_TO_RETRY_IMPLEMENTATION** |
| **R-R05-03** timeout, unknown commit, idempotency | Need bounded global time budget, durable claim/fencing and safe full-six replay authority | **BLOCKER_TO_RETRY_IMPLEMENTATION** |
| **R-R05-04** known four-join replay defect | Attempt/Receipt absent still yields false `replay=true`; future coordinator cannot treat this as complete | **PRODUCTION_SAFETY_GAP_OPEN** |
| **R-R05-05** fresh, independent success authority | Additional self-contained six-table observer with complete typed physical identity and status is required | **CONTRACT_DEFINED / NOT IMPLEMENTED** |

**Implementation-readiness conclusion:** Neither F nor R has an approved concrete new file/CI inventory, evaluated least-privilege forensic grants, signed independent reviewer or reproducible original InnoDB graph. R additionally lacks a frozen transaction status authority, exact total elapsed budget and safe full-six replay implementation. Therefore *do not implement either under the old PR #457's six-file authorization*. A separately scoped minimal forensics implementation could precede retry policy implementation **only after** narrow new-file and observer-privilege amendment. A synthetic retry prototype requires its own independent authorization and must not be sold as production reliability.

```text
REMOTE_MAIN_EXACT = 20a54b4a7478641d8b8d8c12c6213b8e2ea73acc
P1C_PR457_EXACT_HEAD = eac72825ca80ad34406a1fbd620eed20c17db3fe
ORIGINAL_RACE05_THREE_ATTEMPTS = PASS / FAIL_1213 / PASS
ORIGINAL_RACE05_SOURCE_ONLY_VERDICT = NOT_STABLY_MET
INNODB_PRECISE_DEADLOCK_GRAPH = NOT_CAPTURED
FORENSIC_OBSERVER_GRANTS_AND_FILE_SCOPE = NOT_FROZEN
FORensics_DESIGN = DEFINED; IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
CALLER_OWNED_WHOLE_TX_RETRY_CONTRACT = DESIGN_DEFINED_PENDING_BLOCKERS
RETRY_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
FOUR_JOIN_FULL_SIX_REPLAY_SAFETY = CONFIRMED_NOT_SAFE
PR457_MERGE_AUTHORIZATION = NOT_GRANTED
B2_AND_REAL_D5 = NOT_AUTHORIZED
```

## 5. Next gate

**`D5-O2 Stage B — RACE-05 F-ISOLATED Deadlock Observer Privilege / Redaction / Incident-Binding Detailed Design + Scoped Implementation Readiness Review`**.

Freeze a *new*, separate forensic-only test/CI file list and disposable MySQL instance; evaluate `performance_schema` permissions and sanitization with negative examples; define deterministic correlation and no-graph outcomes and finite runs. Then conduct independent targeted design review. **Only after** a separate explicit authorization may a forensic implementation PR begin. In parallel, maintain R as a documented retry candidate, but defer its implementation until rollback authority, full-six source repair or equivalent authorized safe read, timeout/unknown-commit and idempotency/fencing contracts are closed.

No production code/schema/PHI/real message, existing PR #457 semantics or merges are changed by this review.
