# U07 D5-O2 Stage B — B1-P1-C RACE-05 SERIALIZABLE Independent-Effect Deadlock Root-Cause / Reliability Policy Design Review v0.1

DATE: 2026-10-13. STATUS: **ROOT-CAUSE CLASS ESTABLISHED, PRECISE LOCK GRAPH UNOBSERVED; RELIABILITY POLICY DESIGN CANDIDATE — NOT AUTHORIZED FOR IMPLEMENTATION.**

This is a **same-assistant source-and-original-artifact technical review**, not an independent human sign-off. It is a review-only artifact and **does not edit** application code, tests, SQL, schema, configuration, or CI; **does not merge** PRs; **does not authorize** automatic retries, runtime wiring, real downstream delivery, F8, B2, production traffic, or REAL_D5.

## 1. Frozen identity / original physical evidence

- Main remains \`20a54b4a7478641d8b8d8c12c6213b8e2ea73acc\`. Implementation Draft PR [#457](https://github.com/cxjchelsea/AIdoctor/pull/457), exact HEAD \`eac72825ca80ad34406a1fbd620eed20c17db3fe\`, still open/unmerged. Test implementation remains **six ADD-only C-ISOLATED files**, unchanged original \`JdbcU06DeliveryStore\` and all migration/P1A/P1B assets.
- [GitHub Actions run #38329637853](https://github.com/cxjchelsea/AIdoctor/actions/runs/38329637853) contains **three separately executed attempts, at that SAME exact HEAD** (new MySQL8 service for each job). Raw original ZIPs were independently downloaded and CRC/SHA-256/individual \`race05.json\` inspected; no subsequent passing run masks an earlier failing run.
- Attempt #1 job \`115076198699\`: eight Surefire cases PASS; original Artifact **#11781323803**, SHA-256 \`eb0e5876e7075cd12fd8b12fc3b598542e3bdcb2a2d427bdca04cdbe82fce386\`. RACE-05 Actor A and B original \`NEW_SUCCESS\`, 6 successful INSERTs each, explicit transaction call returned for both; each separate Effect has final six-vector \`[1,1,1,1,1,1]\`.
- Attempt #2 job \`115076610917\`: seven cases PASS / **RACE-05 FAIL**; Artifact **#11782500392**, SHA-256 \`795121bfd1f5aea93b44fd78db92b97f90d077dd9c56f6e6c73044c72b234aa1\`. **Actual failure evidence preserved before any full-graph assertion:** Actor A physical JDBC connection 37, original \`40001:1213\` \`ER_LOCK_DEADLOCK\`, \`ORIGINAL_THROW_SQL_1213\`, 0 successful INSERTs and no normally returned TX call; Actor B JDBC connection 36, \`ORIGINAL_NEW_SUCCESS\`, 6 successful inserts and normally returned TX call. Both Spring-thread resources cleared. After both terminal, independent SELECT-only Reader connection 38 read A vector \`[0,0,0,0,0,0]\`, B vector \`[1,1,1,1,1,1]\`. Test-origin evidence labels this \`DIAGNOSTIC_ONLY_NOT_ACCEPTED\`; original JUnit result remains FAIL. Actor transaction intervals overlap; this is not sufficient alone to reconstruct lock wait-for edges.
- Attempt #3 job \`115077147960\`: eight cases PASS; Artifact **#11782018387**, SHA-256 \`8ee16ff94725f30e609b5c2c7a46482f8361bf1e17bc06f5efb4954fcf032da0\`. RACE-05 both normal \`NEW_SUCCESS\`, 6 inserts each, both final vectors full.
- **Observed result: PASS / FAIL / PASS.** No \`UNKNOWN_COMMIT\`, no six-table partial-commit group in the failed attempt. The negative fact is one independently identified transaction rolled back as a MySQL deadlock victim while the unrelated Effect succeeded. This is not proof that another previously committed effect was erased.

## 2. Root-cause boundaries — what can actually be claimed

**RC-01 CONFIRMED:** \`SERIALIZABLE\` + two concurrent initial \`find()\` calls and six-table insertion workload can reach a real InnoDB deadlock path even when the input identity keys do not collide. Original \`JdbcU06DeliveryStore.find()\` SELECTs ledger joined to authority, intent and confirmation, and uses \`WHERE l.delivery_effect_id=?\`. No extra SELECT lock hint; each worker wraps the unchanged final Store manually using an explicit Spring \`DataSourceTransactionManager\` \`TransactionTemplate\` with \`SERIALIZABLE\`, not an established effective \`@Transactional\` proxy. Both writers are distinct physical JDBC connections; one MySQL error reports SQLState **40001**, error **1213**, and InnoDB automatic transaction victim rollback.

**RC-02 HIGH-PLAUSIBILITY HYPOTHESIS, NOT PROVEN INDEX-LEVEL ROOT CAUSE:** MySQL8 InnoDB \`SERIALIZABLE\` locking reads can acquire shared next-key/index-range locks. Under concurrent absent-key searches, an ensuing INSERT can contend with another transaction's held gap/index locks, producing a deadlock even if application-level keys are different. MySQL's documented semantics support this as a plausible mechanism, **not proof of which indexed range/lock edge actually formed in run #38329637853**. The current Artifact has no \`SHOW ENGINE INNODB STATUS\` deadlock stanza or \`performance_schema.data_locks/data_lock_waits\` pair captured at the failure. Do **not** state definitive culprit index, lock order, wait-for cycle, or that the deadlock arose solely on \`u06_delivery_ledger\`. Other statement/index lock edges remain possible.

**RC-03 NOT ESTABLISHED:** (a) that lowering transaction isolation would be safe; (b) that new indexes/SELECT hints would eliminate deadlocks; (c) that a production Retry automatically resolves a business operation; (d) that the four-join Store is safe for six-table replay. Existing B1-P1-B confirmed a **separate source defect**: original \`find()\` may return \`replay=true\` with Attempt or Receipt missing. This defect stays \`CONFIRMED_NOT_SAFE\`.

**Documentation alignment:** MySQL 8.0's official manual states \`SERIALIZABLE\` SELECTs use shared next-key locking, automatically rolls back a detected deadlock victim, and advises applications to reissue the entire rolled-back transaction rather than an isolated SQL statement. See \`https://dev.mysql.com/doc/refman/8.0/en/innodb-locks-set.html\`, \`https://dev.mysql.com/doc/refman/8.0/en/innodb-deadlock-detection.html\` and \`https://dev.mysql.com/doc/refman/8.0/en/innodb-deadlocks-handling.html\`. These describe database semantics, **not case-specific deadlock forensic proof**.

## 3. Separate source characterization from reliability semantics

| Plane | Existing source-only B1-P1-C gate (DO NOT CHANGE IN PR #457) | Potential separately authorized reliability gate |
| --- | --- | --- |
| Store execution | One original \`reconcileConfirmed(Command)\` inside one caller-owned TX, no retry | May invoke **a new whole transaction** after a proven rollback, only with same frozen command and separately reviewed retry owner |
| RACE-05 two independent Effects | Both must return \`NEW_SUCCESS\` and both separate complete 6/6 graph. \`1213\` on either is **FAIL/DIAGNOSTIC**, even if other wins | Two Effects may eventually each commit coherent 6/6 **within a fixed bounded budget** after a *provably retriable deadlock*; attempts/results and retry evidence remain visible. A first-attempt \`1213\` is never silently upgraded to source-pass |
| Original SQL deadlock | Real \`40001:1213\` and third-reader vector are evidence | Proposed \`RETRIABLE_TX_ABORT\` state **only** if 1213 physical rollback is established and no commit ambiguity; no assumption that every SQLState 40001 is safely retryable |
| Original 1205 lock timeout | Error evidence / not success | **Not automatically eligible**: MySQL 1205 lock wait timeout does not universally imply whole-transaction rollback; require explicit known rollback/transaction unwind then fresh-state check before any new TX |
| 1062 duplicate | Conflicting or colliding key fact; no replay automatically | **No generic retry**; validate complete existing six-table group and original command identity separately. Invalid partial group => BLOCKED, not success |
| Unknown commit / transport disconnect | NOT_OBSERVED / fail closed | **NEVER blindly retry** until a separately authorized authoritative reconciliation distinguishes COMMITTED/NOT_COMMITTED/UNKNOWN, protected by fencing/idempotency |
| Side effects | No real outbound delivery, PHI or F8 | None authorized. Reliability prototype remains synthetic; **no duplicate external delivery allowed** by inference from database rollback |
| Success semantics | Original source's first-attempt behavior only | New, separately named \`EVENTUAL_RACE_COMPLETION\` test class/contract if later authorized; must retain raw first-attempt result and attribution |

**Hard boundary:** RACE-05 failing on a real 1213 is meaningful characterization evidence. It must remain red under today's frozen source-only contract. “MySQL deadlocks are normal” is not a reason to overwrite the observed failure or change the assertion inside current PR #457.

## 4. Candidate architecture options and recommendation

**Option A — Do not implement retry; return typed \`RETRIABLE_TX_ABORT\` to a caller.** Lowest behavioral change and accurately exposes the deadlock victim; callers require explicit handling or the operation is not guaranteed to complete. Appropriate as a first controlled source characterization and an explicit failure contract, not full production reliability.

**Option B — A bounded, caller-owned whole-transaction retry coordinator,** *separate from unchanged \`JdbcU06DeliveryStore\` and original P1-C tests*, is **the recommended next design candidate only**. The coordinator takes the immutable Command and stable logical effect/idempotency identity, chooses at most a small reviewed number of attempts (candidate **max 2 total TX attempts**, not frozen), fully unwinds and releases the first transaction, then begins another independent SERIALIZABLE transaction. Require a deterministic bounded elapsed budget and recorded backoff/jitter policy with no uncontrolled sleeping; distinguish deadlock-victim rollback from commit ambiguity. Preserve attempt-level raw failures; a later commit may satisfy eventual completion only after fresh independent six-table readback for **both** Effects. Exhaustion is an explicit typed failure, not a success. **Do not implement within PR #457 or grant retry permission by this document.** The policy must avoid live external delivery, must not rely on the known unsafe 4-join replay for reconciliation, and must not hold locks across the retry boundary.

**Option C — Change source locking/isolation or database schema.** Potential for lock-contention reduction but broadens production semantics and repeatability risk (weaker isolation could change correctness guarantees). **Not approved** without physical deadlock graph, independent transaction/safety design, schema review and explicit production authorization; do not assume switching to \`READ COMMITTED\` is safe.

**Recommended staging:** First a **read-only exact-artifact and additional deadlock-forensics design** to capture \`SHOW ENGINE INNODB STATUS\` / scoped InnoDB lock evidence on isolated MySQL8 for cause attribution (review security of potential raw SQL identifiers). In parallel design Option B's retry ownership/invariants but **do not code it** until the four-table replay safety precondition and commit authority boundaries are handled. Production deadlock response must be independently planned with the known replay flaw included in the threat model.

## 5. Proposed reliability contract (design constraints, not executable authorization)

- **Scope:** new standalone synthetic reliability-extension unit, not edits to current PR #457 source-characterization tests or existing production code. Any proposed file inventory and CI must be explicitly frozen in a later review.
- **Immutable input across attempts:** same Effect / selection / delivery / idempotency / confirmation IDs and immutable fingerprint; never mint a new business identity merely because an initial TX deadlocked.
- **Retry eligibility:** verified deadlock-victim SQL \`40001:1213\`, transaction manager unwind successfully completed, no active bound resource, no commit ack ambiguity, no external side effects, no cancelled/unknown operation. Unknown exception, 1205 without known rollback, 1062, unsupported SQLState, TX still active or canceled task => **DO_NOT_RETRY**.
- **One full transaction per attempt:** no SQL-level retry inside the six INSERT chain and no nested unbounded TxTemplate recursion; request-specific attempt number and fresh transaction boundary. Candidate \`attemptLimit=2\` and bounded global deadline must be finalized by a separate review. Rejected/harness-timeout/unknown state always outranks nominal retryability.
- **Verification:** independent Reader must attest all six rows for both successful Effects, correct identity/status including Attempt/Receipt; one winning commit is not equivalent to two independent Effects completed. Replay comparison cannot trust the current four-way JOIN alone. Successful store return and record graph coherence must both hold; never infer the first attempt succeeded from subsequent state.
- **Exhaustion:** explicit \`RETRY_EXHAUSTED\`, \`RETRY_UNSAFE\`, \`UNKNOWN_COMMIT\` or \`STORAGE_CONTENTION\` states, with evidence. No claim of successful delivery, F8 resume, or guaranteed exactly-once across external side effects.
- **Test plan:** (i) preserve original unmodified RACE-05 as a first-attempt source characterization, including its possibility of FAIL; (ii) separately test a bounded retry policy with a physically reproduced original 1213 and fresh readback; (iii) fault-inject unknown commit and 1205/1062 negative admission without fabricated physical completion; (iv) original P1-A/B CI regression and full-six false-replay negatives. Green positive retry runs are not evidence the existing Store source is safe.

## 6. Review findings and formal decision

| Finding | R1 verdict | Required next gate |
| --- | --- | --- |
| **DL-R05-01** noncolliding Effects can deadlock at SERIALIZABLE | **CONFIRMED_REAL_1213**, two pass/one fail | Preserve reproducible test-origin negative observation, no relaxation |
| **DL-R05-02** exact lock edges not in artifact | **FORENSIC_GAP_OPEN** | Design scoped isolated InnoDB deadlock/lock-wait capture; keep secrets/raw IDs out of artifacts |
| **DL-R05-03** first-attempt vs eventual completion confused | **CONTRACT_SPLIT_REQUIRED** | New separately approved reliability gate; existing PR #457 remains source characterization |
| **DL-R05-04** transaction-level retry ownership/eligibility | **OPTION_B_DESIGN_CANDIDATE**, implementation not authorized | Independent design review of owner, whole-TX unwind, time budget, negative admission |
| **DL-R05-05** original four-join replay safety | **CONFIRMED_NOT_SAFE**, independent blocker for business replay authority | Separate source-read repair and six-table re-review before production reliability claim |
| **DL-R05-06** production/external delivery scope | **DENIED** | No authorization to change Store, schema, scheduler, F8 or messaging |

\`\`\`text
BASE_MAIN = 20a54b4a7478641d8b8d8c12c6213b8e2ea73acc
PR457_EXACT_HEAD = eac72825ca80ad34406a1fbd620eed20c17db3fe
RACE05_THREE_ATTEMPTS = PASS_8 / FAIL_7_PLUS_RACE05 / PASS_8
RACE05_SQL_40001_1213_VICTIM_ROLLBACK = CONFIRMED
RACE05_PRECISE_INNODB_WAIT_FOR_GRAPH = NOT_CAPTURED
RACE05_CURRENT_SOURCE_ONLY_DOUBLE_SUCCESS = NOT_STABLY_MET
RACE05_ORIGINAL_TEST_VERDICT = UNCHANGED
DL_R05_RELIABILITY_POLICY_DESIGN = OPTIONS_REVIEWED; OPTION_B_RECOMMENDED_CANDIDATE
DL_R05_RETRY_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
PR457_IMPLEMENTATION_READINESS = NOT_READY
PR457_MERGE_AUTHORIZATION = NOT_GRANTED
PRODUCTION_FOUR_JOIN_REPLAY_SAFETY = CONFIRMED_NOT_SAFE
B2_PRODUCTION_WIRING = NOT_AUTHORIZED
REAL_D5 = NOT_READY
\`\`\`

**Next recommended gate:** \`D5-O2 Stage B — B1-P1-C RACE-05 InnoDB Deadlock Forensics Evidence Design + Caller-Owned Whole-TX Retry Contract Independent Review\`. Keep it **design/read-only first**; if new physical forensic instrumentation or retry code is desired, freeze exact new file/scope, obtain separate implementation authorization, and never silently rewrite PR #457's RACE-05 success condition. No PR merge is authorized.
