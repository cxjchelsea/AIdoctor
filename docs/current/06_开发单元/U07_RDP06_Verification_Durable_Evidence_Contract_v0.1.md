# U07-RDP-06 Verification / Durable Evidence Contract v0.1

> Development Unit: U07 — User Answer Resume / Idempotent Recovery
> Gap: B-U07-RG-06
> Upstream: independently **CONDITIONALLY_ACCEPTED_DESIGN** RDP-01..05, latest RDP-05 independent review [PR #285](https://github.com/cxjchelsea/AIdoctor/pull/285) @ `15e335759b9cf7acaeca8c7424ff971034c4c1f1`
> Verified source **inspection** baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`; design artifacts may be on unmerged PR branches
> Status: **DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN**
> Scope: design the authoritative expectation oracle, fixture/provenance contract, physical verification layers, runner, failure classification, evidence integrity and independent review. **No executable suite is claimed built, run or passed.**
> Profile: PROFILE-B synthetic structural nonproduction exclusively; PROFILE-A, PHI, clinical patients, live outbound IO and production BLOCKED.

## 1. Verification question and separation of authorities

Prove an authorized implementation of U07 honors original answer identity, F8 business verdict, P02 side-effect-free parked recovery, F3/P01/Consultation owner mutation, U15 terminal/currentness, APPLIED+unique outbox, bounded dispatch grants, U02 consumer idempotency, versioned dependency invalidation and replay-safe failure. Prove that **nothing** executes when prerequisites fail.

The evidence authority chain is:

~~~text
independently reviewed frozen RDP01..06 contracts + owner authorization profile
 → independently reviewed machine-readable expected oracle
 → independently reviewed synthetic fixture manifest
 → source-pinned implementation / database / adapter / test runner
 → SUT execution produces observations WITHOUT reading expected oracle
 → separate comparator verifies each observation against oracle
 → durable exact-head bundle + provenance hashes + external-effect counts
 → independent evidence-only review
 → separate Implementation/Evidence closure and explicit governance gates
~~~

**No self-certification:** a test deriving its expected value from SUT output, comparing a mocked U07 response to itself, or treating P05 Trace as a Clinical/P02/Consultation owner receipt is INVALID_EVIDENCE. An independently reviewed design matrix is **not** an executed oracle. `RDP06_DESIGN_PASS` is not runner complete, test passed, implementation readiness or merge authorization.

## 2. Frozen authority package and provenance

`U07VerificationAuthorityManifestV1` must contain immutable refs/digests for:
- U07 Unit Spec v0.1 and frozen Phase 5/6/7/8/9 owner rules; exact RDP-01 through RDP-05 **accepted source blob SHAs**, plus RDP-06 reviewed design blob SHA.
- Independent review records for each RDP and exact head they reviewed. Acceptance of a conditional design **does not** close Foundation/CA physical gates.
- Source head, full source tree digest, migration manifest for both supported DB dialects, configuration/Registry/scope/profile, deployment artifact digest and test source hash; no inferred equivalence from PR numbers.
- Every owner authorization and CA provenance for U06 eligibility, F8/U15 shared fence, F3 bridge, P01/U15 commit, U15 dispatch grant, P02 rehydrate/start/landing and U02 consumer idempotency; **missing authorization is an explicit blocked gate**, never an implicit synthetic approval.
- Reviewed machine-readable oracle digest and independent Oracle Review Gate, fixture digest and Fixture Review Gate, reviewed contract/provenance manifest digest, Authorization Profile Gate and dedicated runner/evidence protocol version.

**Dependency graph rule:** each gate references the **same** immutable authority core. If design SHA/owner CA/profile/runner/fixture/oracle changes, every dependent hash and gate must be reassessed/reviewed; no silent retargeting of an existing PASS evidence bundle. Historical prior evidence remains identifiable but cannot support a changed head without an explicit accepted equivalence proof.

**Contract-expectation gap:** where frozen owner contracts cannot determine an expected observation (ambiguous U15 terminal policy, unsupported P02 reconstruction source, unapproved owner lease semantics), mark `CONTRACT_EXPECTATION_GAP` **before running the case** and fail the overall authority gate, instead of deriving a convenient answer from implementation or inventing clinical semantics.

## 3. Scope and execution layers

| Layer | Evidence target | Required execution source |
|---|---|---|
| L0 authority/provenance | exact heads, reviewed oracle/fixture/profile/gates | independent manifest validator |
| L1 schema and identity | typed USER_ANSWER, RESUME_REQUEST, original wait binding, IDs/digests, decision/output schema | SUT serializers and actual persistent rows |
| L2 deterministic F8/business rules | priority/precedence, first verdict vs replay, cancellation, expiry, scope | F8 evaluator/decision owner with reviewed test fixtures |
| L3 owner and state effects | F3-approved ONE P01 patch, Question/Gap/Pending consume, D ACTIVE, APPLIED+outbox | actual P01/Consultation DB adapters plus owner readback |
| L4 runtime recovery | exact executable checkpoint vs owner-certified rehydrate, stable continuation, parked barrier | actual P02 Thread/Run/Checkpoint owner and scheduler interception |
| L5 downstream U02 | U15 grant, same-effect dispatch, consumer replay/ACK, no Clinical Fact inference | isolated outbound adapter + independently controlled synthetic U02 consumer |
| L6 fault/race | crash after each durable stage, U15/clock/permission races, dual DB dialect | real database transactions, fault injection, independent resource spies |
| L7 scope/guard | zero real PHI, zero real network/phone/SMS, no live profile, no clinical models | fail-closed environment/network isolation and side-effect tripwires |
| L8 regression | existing U01..U06 and domain code baseline, U07 changes | full supported diagnosis-service regression + static/build gates |

**No fake physical PASS:** contract/unit mocks may prove structural shape in L1/L2 but cannot satisfy L3–L6 where real DB concurrency, authentic owner stores, physical retries and observed external-call counters are required. If a DB implementation is not available for the required dialect, mark the corresponding matrix cells `NOT_EXECUTED / REQUIRED`, never SKIPPED_PASS.

## 4. Verification profile and isolation

Only `PROFILE_B_SYNTHETIC_STRUCTURAL_NONPROD` can be a valid test input. Fixtures use generated nonpatient consultations, Questions and answers; no PHI or external medical profiles. Production credentials/endpoints are impossible to mount in the runner. Deny all real outbound sockets, calls/SMS, production queues, patient CDP and unapproved model endpoints; allow only explicitly cataloged ephemeral DB services and an **isolated synthetic U02 receiver**. Verify denial controls with a deliberate network-attempt tripwire and fail the run if it connects.

Runner records synthetic scope attestation, allowlist hash, route intercept coverage, number of outbound attempts by endpoint/class, actual interposed test consumer count, attempted P01 writes, runtime resumes, scheduler/tool/model invokes, Clinical version advances, Outbox and U02 effects. A zero count without a configured/verified tripwire is insufficient evidence.

## 5. Canonical case/fixture/oracle model

~~~text
U07VerificationCaseV1 {
 case_id, parent_rdp, owner_namespace, covered_invariant_ids[],
 authority_contract_refs[] + immutable_reviewed_blob_shas[],
 fixture_id, scenario_seed, expected_pre_owner_snapshot_ref?,
 injection_point?, required_execution_layer, required_dialects[],
 expected_business_verdict?, expected_owner_phase?, expected_owner_receipts[],
 expected_event_root_identity_relations[],
 expected_sql_commit_count_or_range?,
 expected_effect_counts_by_owner_and_kind[],
 expected_negative_effect_counts_by_kind[],
 expected_u15_generation/order?, expected_readback_fingerprint?,
 expected_replay_or_reconcile_outcome?, expected_failure_class?,
 source_of_expected_authority, expected_oracle_digest
}
U07VerificationFixtureV1 {
 fixture_id, canonical_input_bytes_digest, synthetic_answer_digest,
 consultation_id, question_id, parent_wait_effect_id, original_eligibility_ref,
 thread_id, run_id, checkpoint_id?, frozen_historical_bindings,
 original_state_snapshot_digest, owner_version_vector,
 u15_terminal_generation, wait_deadline_source,
 selected_p02_path, executable_checkpoint_digest?,
 rehydrate_owner_proof_ref?, original_effect_manifest_digest?,
 controlled_fault_schedule?, test_consumer_config_ref?,
 expected_isolation_profile, independent_fixture_review_ref
}
~~~

No text, customer secret or PHI in evidence. Fixture generator records deterministic version/seed and canonical bytes hash; it must not read oracle expected outcomes. Oracles are reviewed from contracts/owners; fixture validity and oracle correctness are **two independent reviews**. If a clinical-policy output requires unreviewed medical thresholds, exclude it as `CONTRACT_EXPECTATION_GAP`, not guessed clinical answer quality.

## 6. Mandatory U07 first-class oracle catalog

Each row is a **design-only requirement**; IDs `U07-VG-001..060` are reserved for future executable cases. Expected assertions must include exact root/owner state and **negative side-effect counters**, not merely response codes.

### RDP-01 — Admission and canonical binding

| ID | Scenario | Required observation |
|---|---|---|
| U07-VG-001 | first valid synthetic USER_ANSWER | canonical row + inline immutable binding one transaction, original payload/digest |
| U07-VG-002 | same event/idempotency retry | one canonical row, identical owner fingerprint, no second decision |
| U07-VG-003 | same protected identity but changed payload/scope | conflict, zero F8/P02/P01/U02 effects |
| U07-VG-004 | RESUME_REQUEST references original accepted answer | same target answer/root, no second content or independent F8 verdict |
| U07-VG-005 | RESUME_REQUEST missing/conflicting target | typed unresolved/conflict; no manufactured USER_ANSWER |
| U07-VG-006 | ledger insert commits but side-binding fails | both rollback; no partially admitted canonical event |
| U07-VG-007 | projected U06 eligibility hash without authoritative issuance | no accepted admission; original issuance must be proven |
| U07-VG-008 | same key two concurrent writers, original exact payload | unique canonical owner ID, collision reconciliation, no duplicate side binding |
| U07-VG-009 | existing original wait delivery not confirmed or parent provenance missing | blocked owner provenance, zero novel effects |
| U07-VG-010 | historical wait is real, checkpoint stale | F8 may evaluate; runtime repair is not business REJECTED |

### RDP-02 — F8 verdict/precedence and time

| ID | Scenario | Required observation |
|---|---|---|
| U07-VG-011 | first legitimate on-time answer with current Question/wait | one F8 ACCEPTED/winner, not yet APPLIED |
| U07-VG-012 | same original ACCEPTED event replay after U15 terminal | original ACCEPTED unchanged; no new effect |
| U07-VG-013 | already APPLIED identical new canonical answer | F8 DUPLICATE, no new Runtime/Clinical effect |
| U07-VG-014 | same wait but different answer after prior APPLIED | REJECTED/conflict, zero new effect |
| U07-VG-015 | new late answer vs certified statement-current deadline | EXPIRED; no first ACCEPTED |
| U07-VG-016 | U15 cancel commits before F8 final conditional statement | REJECTED/owner-terminal with zero effects |
| U07-VG-017 | F8 final statement commits before U15 terminal later | historical ACCEPTED retained; all later effects independently fenced |
| U07-VG-018 | DB transaction begins pre-deadline, final F8 statement after deadline | no false ACCEPTED based on transaction-start time |
| U07-VG-019 | two distinct fresh answers contest same wait | exactly one winner, loser typed disposition |
| U07-VG-020 | time source/currentness unavailable or unverified SQL dialect | DEFER/F8_TIME_AUTHORITY_UNAVAILABLE, zero acceptance/effects |

### RDP-03 — P01/Consultation/APPLIED and effect replay

| ID | Scenario | Required observation |
|---|---|---|
| U07-VG-021 | Stage C owner-authorized Question/Gap/Pending | exactly one P01 commit, one Clinical version, one owner readback |
| U07-VG-022 | C owner commit succeeds but Question projection incomplete | replay same P01 CommitResult; zero second Clinical write |
| U07-VG-023 | C patch only partial in-memory, DB rollback | no durable Question/Pending mutation |
| U07-VG-024 | F3 owner authorization or P01 field grant missing | Stage C blocked, no partial state |
| U07-VG-025 | U15 terminal wins before C conditional commit | zero new Clinical mutation |
| U07-VG-026 | C committed, U15 terminal wins before D | C historical effect preserved; D fails closed |
| U07-VG-027 | D ACTIVE commits under correct owner then caller crashes | same ACTIVE receipt and root on recovery |
| U07-VG-028 | E APPLIED journal insert succeeds but outbox insert fails | both rollback; no APPLIED without durable intent |
| U07-VG-029 | E APPLIED + unique Outbox commits together | one handoff ID and one atomic APPLIED with readback |
| U07-VG-030 | APPLIED journal effect ID reused with different payload | quarantine/reconciliation, no new handoff |
| U07-VG-031 | C, D, E crash window injected after each COMMIT | stage-specific owner truth reconciled with original effect IDs |
| U07-VG-032 | two concurrent workers apply same accepted root | at most one Stage C/D/E owner mutation sequence |

### RDP-04 — P02 and U02 grant/dispatch boundaries

| ID | Scenario | Required observation |
|---|---|---|
| U07-VG-033 | U06 metadata-only Checkpoint without executable image | no COMPATIBLE_CHECKPOINT, no invented runnable continuation |
| U07-VG-034 | valid executable checkpoint + pinned history | one restore-only grant and parked RESUMED_VERIFIED readback |
| U07-VG-035 | full certified deterministic rehydrate proof | stable derived continuation Run and parked-state digest match |
| U07-VG-036 | historical UNKNOWN/PARTIAL_SUCCESS Tool attempt | no rehydrate, no history/tool replay |
| U07-VG-037 | U15 terminal wins before P02 restore-only start grant | no first physical restoration |
| U07-VG-038 | restore-only grant committed then U15 terminates before worker start | same inert grant may park if explicitly approved; no business node/C |
| U07-VG-039 | grant COMMIT UNKNOWN | no first physical restore until authoritative grant readback |
| U07-VG-040 | cursor points at Tool/U02, scheduler callback or retry | global parked landing barrier blocks node/tool/model/U02/Clinical effects |
| U07-VG-041 | P02 owner restore completed but reply/receipt lost | query same Thread/Run owner result; no second restore |
| U07-VG-042 | restore succeeded, C/P01 later fails or U15 changes | F8 historical ACCEPTED intact; no automatic scheduler or U02 |
| U07-VG-043 | E APPLIED+PENDING but no U15 Stage-F grant | zero outbound U02 sends |
| U07-VG-044 | U15 terminal first before F grant | durable BLOCKED_TERMINAL, zero U02 sends |
| U07-VG-045 | F grant first then U15 terminal before transport | only same granted effect may finish under authorized U15 policy |
| U07-VG-046 | U02 consumer ACK lost / retry | same immutable handoff ID and consumer query; no producer exactly-once fact assumption |
| U07-VG-047 | U02 consumer lacks idempotent admission/receipt query | NOT_READY, no outbound dispatch |
| U07-VG-048 | delivery ACK reported but no U02 Clinical Fact receipt | U07 cannot claim Clinical Fact formed or modify U02 truth |

### RDP-05 — Capabilities, applicability and authority

| ID | Scenario | Required observation |
|---|---|---|
| U07-VG-049 | first-ever F8 lacks prior F8 result; Stage E lacks APPLIED result | precondition checks pass if capabilities/earlier effects valid; own receipts verified only POST |
| U07-VG-050 | F grant, actual transport, U02 accept and U02 Fact phases | distinct persisted statuses; grant/ACK/Fact not interchangeable |
| U07-VG-051 | P01 field grant revoked between READY and Stage C commit | owner-fenced commit blocks; zero Clinical version advance |
| U07-VG-052 | F3 policy release revoked or P06 scope altered before relevant commit | fail-closed owner recheck; no cached-ready bypass |
| U07-VG-053 | U02 consumer endpoint/permission changes after grant, before send | no outbound until current consumer authority verified |
| U07-VG-054 | metadata Checkpoint or U06 synthetic P01 adapter labeled as full U07 capability | NOT_DEMONSTRATED / NOT_READY |
| U07-VG-055 | current vs historical Registry binding differs | certified historical resolution or BLOCKED; no silent latest |
| U07-VG-056 | PROFILE-A/PHI or forbidden live endpoint requested | structural authorization barrier BLOCKED, zero actual outbound/PHI |
| U07-VG-057 | owner snapshot cached without CAS/fenced lease, then revoked | no positive READY reusable at commit; no new effect |
| U07-VG-058 | mandatory Foundation audit or one CA remains not authorized | U07 overall readiness NOT_READY despite otherwise successful synthetic cases |
| U07-VG-059 | MySQL vs Oracle time precision/current-statement semantics | both dialected tests prove same owner winner/deadline policy or invalid evidence |
| U07-VG-060 | F8 P02 P01/Consultation journal Outbox P05 identities | exact single-root lineage; each owner receipt independent of Trace |

All 60 rows are **unexecuted design oracles**, not existing runnable test methods. After design acceptance and separate implementation authorization, a **machine-readable case expectation file** must explicitly enumerate every ID, fixture ID, actual source-of-authority, owner outcome, side-effect counts, required dialect and negative assertions. An omitted case blocks authoritative closure.

## 7. Cross-cutting concurrency and crash schedule

Use controlled barriers and deterministic fault injection, not random sleep-only “race tests.” Record worker/transaction identity, lock acquisition order, SQL statement-time and precision, version/epoch, COMMIT/ROLLBACK status, owner receipt, guard and physical network attempt.

| Boundary | Fault/race required | Invariant |
|---|---|---|
| Foundation event+side-binding | exception before second insert or before COMMIT | no canonical-only success row |
| F8 first verdict | U15 cancel/expire before vs after final conditional statement | correct precedence, historical ACCEPTED immutable |
| F8 time | transaction-start before deadline, statement after | no acceptance from transaction-start-stale clock |
| P02 restore grant | U15-first vs grant-first vs unknown COMMIT | no start absent durable grant; grant only inert restoration |
| P02 physical recovery | crash before/after restore, status write, parked owner receipt | original Run/Thread root; no duplicate runnable task |
| Historical rehydrate | incomplete effect manifest, unknown external attempt | zero restored execution and zero history replay |
| Stage C P01 | U15-first, grant revoked, partial journal evidence, rollback | one atomic Question/Gap/Pending commit or none |
| Stage D | C already committed, then U15 terminal | retain C, block D; never invent rollback |
| Stage E | APPLIED write success but Outbox insert failure | both rollback |
| Stage F | E PENDING with terminal-before-grant, grant-before-terminal | no unauthorized send, exact same immutable authorized grant |
| U02 handoff | network reply lost, consumer query failure, worker retry | fixed same ID, no fabricated Clinical Fact |
| Multi-owner versions | F3/P06/P01/U02 authority revoked after assessment | recheck at final action; no stale cached READY |
| Independent evidence | oracle digest tampered, fixture SHA changed, test omission | manifest INVALID_EVIDENCE not PASS |

**Physical lock proof** covers Consultation/U15 first, F8 winner/root, P02 Thread, P01/journal/outbox as applicable, plus other owner-version conditional commits. No open DB transaction spans a real network call. A remote owner with no enforceable revocation/lease protocol fails readiness.

## 8. Dual-dialect database verification (MySQL and Oracle)

Separate ephemeral migration/test environments for MySQL and Oracle. Each required case must list which dialects apply. At minimum the concurrency, deadline and atomicity cases `VG-006,008,015..020,021..032,037..039,043..045,051..053,057,059` require both dialects at authoritative gate; unit mock outcomes do not count.

Evidence per dialect:
1. actual database product/version, driver, isolation level, transaction manager identity, migration checksum, primary DB identity, clock expression and **observed statement-current vs transaction-start** behavior at precision boundary;
2. proven co-location/atomic enlistment for canonical event+binding, P01 guarded Stage C, Consultation/U15 owner lock, Stage E APPLIED+outbox and both distinct U15-granted permission commits;
3. captured SQL/result/row version/readback with synthetic IDs and trace-free authoritative effects;
4. forced rollback after partial insert, concurrent unique collision, U15 cancellation/expiry and owner permission revocation;
5. deterministic lock ordering and timeouts (timeout => UNKNOWN/RECONCILIATION_REQUIRED until owner readback), never silent successful fallback;
6. same frozen semantics across both dialects; where a dialect cannot implement chosen timing/transaction/fencing, status NOT_APPLICABLE / NOT_READY pending controlled design amendment. A MySQL-only pass cannot authorize Oracle.

DB source logs and connection metadata must avoid PHI; data export contains sanitized synthetic refs/digests, not credentials.

## 9. Independent expectation oracle and fixture review gates

`U07OracleReviewGateV1` verifies reviewed contract digests, exact deterministic expected statuses and negative side effects for all 60 cases. The reviewer must be independent of implementation-output-derived expectations. `U07FixtureReviewGateV1` verifies synthetic source, original U06 wait consistency, stable ID/digest bindings, injected schedules, and absence of production data. Gates must include reviewer identity/decision, authority-core digest, reviewed oracle/fixture SHA256, exact source heads and all case IDs.

**Self-tests required for verification harness:**
- **HG-01 contract-expectation gap:** introduce a valid case without reviewed expected outcome => fail closed.
- **HG-02 external-effect detection:** deliberately attempt a blocked external socket/model/Tool/U02 call => must register nonzero attempted effects and force failure.
- **HG-03 fixture scope escape:** inject real-looking PHI/prod endpoint => rejected before any SUT operation.
- **HG-04 oracle-output separation:** test harness must not import/reuse expectation oracle in observation generation.
- **HG-05 evidence tamper:** flip an oracle/fixture/result byte after review => SHA mismatch invalidates run.
- **HG-06 wrong source/ref:** different source head or migration/runner digest => manifest INVALID_EVIDENCE.
- **HG-07 missing case:** remove any required U07-VG case/owner receipt => INCOMPLETE, never PASS.
- **HG-08 same-root false-positive:** inject P05 Trace success but P01 owner receipt absent => cannot pass committed effect.
- **HG-09 authority gate escape:** force a pending CA to appear authorized in a fixture without owner approval => invalid.
- **HG-10 dialect skip:** suppress Oracle DB job while keeping expected both-dialect cases => INCOMPLETE.

## 10. Runner proposal and observation isolation

**Proposed files; not created or executed under RDP-06 design:**

~~~text
tools/u07_nonprod_verification/verify_u07.py
tools/u07_nonprod_verification/build_evidence.py
diagnosis-service/src/test/resources/u07/u07-verification-expectations.json
diagnosis-service/src/test/resources/u07/u07-verification-fixtures.json
diagnosis-service/src/test/resources/u07/u07-contract-manifest.json
diagnosis-service/src/test/resources/u07/u07-oracle-review-gate.json
diagnosis-service/src/test/resources/u07/u07-fixture-review-gate.json
diagnosis-service/src/test/resources/u07/u07-auth-profile-review-gate.json
.github/workflows/u07-rdp06-authoritative-verification.yml
~~~

Pattern may borrow U06's conceptual runner/manifest/guard approach **without** reusing frozen U06 oracle, verification identity, authorization profile or claiming U06 tests prove U07 physical capability.

Runner sequence:
1. resolve exact reviewed authorization profile and authority-core digest; fail if any mandatory owner authorization is missing;
2. independently validate contract manifest, reviewed oracle, fixture and scope gates; verify source/migration/runner exact HEAD and hashes;
3. start isolated ephemeral MySQL/Oracle and synthetic U02, verify live-socket denial tripwires and synthetic scope;
4. invoke actual SUT through specified public or owner adapter boundaries; observer records statuses/rows/owner receipts/attempted side effects **without importing expected oracle**;
5. execute deterministic concurrent workers and fault schedules with exact effect identity and owner readback;
6. compare each result with independently reviewed expected oracle; do not coerce thrown exceptions/UNKNOWN/SKIP to PASS;
7. run focused U07 tests, full diagnosis-service regression, static/build/profile barriers; capture real exit codes;
8. produce canonical durable result bundle and integrity digests; then send **separately** to independent evidence-only reviewer.

A test that directly executes production clinical endpoints, lacks physical transaction environment or uses hidden external access MUST fail the authorization/isolation gate and must not be rerun “best effort.”

## 11. Structured execution/effect evidence

~~~text
U07CaseObservationV1 {
  case_id, fixture_id, sut_source_head, execution_profile,
  dialect, test_suite + test_method, attempt_id + deterministic_seed,
  observed_business_verdict + f8_owner_receipt?,
  original_canonical_event_id + original_wait_ref + root_effect_id,
  observed_p02_resume_phase + owner_checkpoint/readback?,
  observed_p01_commit_id + version_before/after + Clinical_readback?,
  observed_consultation_lifecycle_effect + owner_version?,
  observed_apply_journal_status + applied_effect_id?,
  observed_u02_outbox_identity + grant_id + dispatch_state?,
  observed_u02_consumer_receipt? + U02_fact_owner_receipt?,
  observed_owner_authority_version_vector + final_recheck_results[],
  observed_sql_transactions[] = {physical_db, tx_id, commit_outcome, lock_order, db_clock_ref},
  effect_counts = {p02_restores, p01_commits, clinical_version_advances,
                   consultation_active_commits, outbox_intents, u02_send_attempts,
                   u02_consumer_admissions, U02_fact_commits,
                   tool_calls, model_calls, external_network_attempts},
  negative_effect_probe_result, observed_failure_class, trace_ref,
  test_exit_code, observed_authority_provenance_refs[]
}
~~~

Typed count assertions must report both expected and observed counts at a **defined causal boundary**; zero attempted U02 sends is not equivalent to zero accepted consumer facts if earlier effects existed. Post-commit receipt evidence must be observed from the corresponding owner store, never inferred from scheduler log, response text or Trace. For partial Saga failures, count irreversible prior stages accurately and assert **no additional unauthorized effect**.

## 12. Durable result bundle and fail-closed final verdicts

~~~text
U07VerificationEvidenceBundleV1 {
  bundle_version, authority_core_digest,
  source_git_head, source_tree_digest, contract_manifest_digest,
  accepted_rdp_blob_digests[], reviewed_ca_authorization_digests[],
  oracle_digest + oracle_review_gate_digest,
  fixture_digest + fixture_review_gate_digest,
  authorization_profile_gate_digest, runner_source_digest,
  migration_digests_by_dialect, configuration_digest,
  execution_environment = {isolated_profile, mysql_version,
                           oracle_version, network_tripwire_result, resource_refs},
  case_evidence[] + case_coverage_summary,
  expected_vs_observed_effect_count_summary,
  owner_receipt_provenance_summary, db_concurrency_outcome_summary,
  full_regression_results, test_log_refs + test_log_digests,
  external_side_effect_attestation + interception_results,
  artifact_file_manifest_with_sha256,
  structural_observation_status, physical_authority_status,
  preliminary_verdict, independent_review_ref?,
  created_at, retention_policy_ref
}
~~~

**Verdict vocabulary:**
- `INVALID_EVIDENCE`: authority/head/digest/oracle/fixture/profile/case self-test/PHI isolation mismatch; no valid comparison.
- `INCOMPLETE`: necessary physical dialect/case/receipt/negative spy missing or not executed.
- `FAIL`: reviewed oracle vs actual implementation result differs; real SUT or regression failure with valid evidence.
- `PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW`: all mandatory cases and gates valid and green at exact head, but independent review not done; **not a merge/authorization**.
- `PASS_REVIEWED`: after independent evidence-only review validates exact unchanged manifest, results, receipts and side effect counts; still does not itself authorize merge, PROFILE-A or production.

A valid SUT failure is **FAIL**, not INCOMPLETE. Incomplete coverage is not a passing result. An independent reviewer can reject the preliminary runner finding, and that must invalidate any earlier claim of accepted evidence.

## 13. Source/contract change invalidation, retention and review order

Freeze immutable source head and reviewed contract/fixture/oracle SHA before a physical run. Re-running at a different implementation HEAD or changing a contract/migration/runner must produce a new evidence bundle, not mutate prior evidence. If implementation changes after oracle/fixture review, verify semantic equivalence through a new independently accepted exact-head gate as required. Store artifacts on GitHub Actions with integrity metadata, retention policy, and verifiable downloaded SHA256; never rely on CI “green check” without the complete result bundle.

**Governed sequence:**
1. RDP-06 independent **design** review and any targeted design remediation/re-review.
2. U07 Aggregate Compatibility Review (RDP-01..06 and open CA topology); controlled amendments if incompatibilities remain.
3. U07 Implementation Readiness re-evaluation: `NOT_READY` while Foundation audit/required CAs/physical owner gates remain unapproved.
4. Explicit U07 Implementation Authorization Decision; code only after authorization.
5. Implement frozen physical interfaces and tests under authorized scope, then independently review implementation exact head.
6. Review/freeze machine-readable Oracle and Fixture as independent authority gates.
7. Build and independently verify actual authoritative runner and environment isolation.
8. Execute focused and full suites, independently review durable evidence.
9. Separate Combined Implementation/Evidence Review, Verification Closure Decision and explicit Merge Authorization. No auto-merge and no live activation by inference.

**Important:** Step ordering for Oracle/Fixture freeze vs implementation work must not let the implementation author select success expectations from observed outputs; independent expectation authority precedes using the runner as proof. Any gap returns to controlled design review, not an oracle edit to match SUT.

## 14. Required capability/authorization carry-forward

All remain **hard blockers** to full U07 readiness; RDP-06 design does not waive them:

~~~text
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

P02 runtime executable/checkpoint/parked owner = NOT_IMPLEMENTED / NOT_VERIFIED
U07 F8, ApplyJournal, U02 Outbox and consumer physical proof = NOT_VERIFIED
MySQL/Oracle statement-time/lock/shared-transaction proof = NOT_VERIFIED
PROFILE-A / PHI / real-patient / production = BLOCKED
~~~

Missing capability requires its owner-controlled implementation/CA; a synthetic test double is not a substitute. `RDP06_DESIGN_ACCEPTED` closes only the **verification design gap**, not executable runner readiness, test closure or clinical deployment.

## 15. Independent design review checklist

~~~text
IR-U07-RDP06-01  Are reviewed expectation/oracle, fixture, SUT observation and comparator four distinct authorities?
IR-U07-RDP06-02  Is exact-head contract/RDP/owner-CA/implementation/runner/DB evidence provenance frozen?
IR-U07-RDP06-03  Does missing or ambiguous frozen expected behavior cause CONTRACT_EXPECTATION_GAP fail closed?
IR-U07-RDP06-04  Do all U07-VG-001..060 have expected owner receipts and negative effect counters?
IR-U07-RDP06-05  Do first F8, parked P02 and staged C/D/E preserve pre/post owner evidence separation?
IR-U07-RDP06-06  Is U15 terminal-before/after first verdict, start grant and dispatch grant checked independently?
IR-U07-RDP06-07  Are real MySQL and Oracle statement-time, transaction atomicity and shared guard tested?
IR-U07-RDP06-08  Is full rehydrate proof and no replay of UNKNOWN external effects observed?
IR-U07-RDP06-09  Is the parked dispatcher-wide barrier instrumented for scheduler/callback/tools/models?
IR-U07-RDP06-10  Can producer Outbox/ACK ever masquerade as U02 Clinical Fact receipt?
IR-U07-RDP06-11  Are per-Owner permission/version revocation race tests run at final effect boundaries?
IR-U07-RDP06-12  Are PROFILE-A, PHI, production and real network egress mechanically blocked?
IR-U07-RDP06-13  Does harness reject tampered oracle, fixtures, source/migration hashes and omitted cases?
IR-U07-RDP06-14  Are any skipped dialect/owner integration tests INCOMPLETE rather than PASS?
IR-U07-RDP06-15  Does the complete bundle support independent recomputation of integrity and verdict?
IR-U07-RDP06-16  Are verification design acceptance and implementation/evidence/merge authorization strictly separate?
~~~

## 16. Current design status and next gate

~~~text
U07-RDP-06 = DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN
B-U07-RG-06 = OPEN / DESIGN_REVIEW_REQUIRED

U07-RDP-01..05 = CONDITIONALLY_ACCEPTED_DESIGN
U07 Aggregate Compatibility Review = NOT_PERFORMED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
U07 runner/oracle/fixtures/physical tests = NOT_IMPLEMENTED / NOT_EXECUTED
All inherited Foundation and controlled-amendment gates = BLOCKING
PROFILE-A / PHI / real-patient / production = BLOCKED
~~~

**Next permitted step:** `U07-RDP-06 Independent Design Review` of the exact commit head. Inspect the deterministic completeness of expected oracles, dual-dialect matrix, owner proof validity, negative side-effect tripwires, evidence bundle integrity and physical gate separation. No Merge, code, migrations or test-execution authorization accompanies this contract.
