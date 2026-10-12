# U07 D5-O2 Stage B — Targeted Design Re-Review v0.1-R1

Date: 2026-10-12. Exact source main `2834714354b02a88ccf73773511af706554cd9fb`, design PR #432 HEAD `7b613d042da432ca08f6be95b98f5a656847c78e`. Scope: design review of **actual U06 Spring transaction wiring/effective boundaries**. This review was performed by the same assistant, **not independently signed by a separate reviewer**. No Java, MySQL, Oracle or production runtime executed. No implementation or merge authority.

## 1. Source and candidate verification

- main unchanged; #432 contains **exactly one added design document**, remains Draft.
- Source confirms `U06ProfileBApplicationService.execute()` starts trace, executes mode1/2/3, completes trace, with catch-and-rethrow terminalization. Mode2 commits selection state, obtains **synthetic** delivery confirmation, commits delivered/pending state, catches consultation-wait exceptions to return `RECONCILIATION_REQUIRED`, then catches runtime-wait exceptions to return the same class of partial outcome.
- `U06WaitCoordinator.establish()` calls `RuntimeWaitCheckpointService.initialize → reserve → RuntimeThreadWaitTransitionService.enterAwaitingUser → U07ResumeEligibilityProjector.project`. `initialize` is not `@Transactional`; `reserve` and `enterAwaitingUser` are annotated but their containing concrete services are final, without observed live Spring-managed proxy. A shared `TransactionTemplate` in Stage A tested candidate behavior, not production advice.
- `JdbcU06DeliveryStore.reconcileConfirmed()` inserts six synthetic delivery tables under an annotation on a final class implementing `U06DeliveryStore`. Interface-based JDK proxy is possible, not established. `U06SyntheticDeliveryService.ScopeAuthorization` explicitly denies real recipients/external side effects/production stores.
- `U06SyntheticP01Runtime.createInjected()` chooses a `Backend.repositoryPort()`; it does not itself assert the physical DataSource/TxManager for `StateCommitter.commit()`. `ClinicalCdpStateRepositoryAdapter` is a plausible Spring adapter but its actual connection to this Backend has not been established.
- Existing `.github/workflows/u06-rdp06-authoritative-verification.yml` has synthetic-only controls and rejects U06 component registration. Stage A #426 evidence proves test-only Java8/MySQL8 JPA local atomicity (10 tests) on prior exact head; inheritance is limited to fixture mechanics and basic MySQL facts.

## 2. Architecture verdict

**B0 (one end-to-end execute transaction) = REJECT**; mode2 has deliberately observable, separately committed stages and cross-resource/side-effect boundaries.

**B1 (explicit short-lived TxPort + well-bound JpaTransactionManager or correct JDBC manager) = DIRECTION ACCEPTED, WITH BLOCKERS** for **nonproduction local-transaction prototype**. An outer `TransactionTemplate` wrapping manually constructed final classes proves **effective caller-managed transaction** and can be an acceptable transaction ownership choice; it **does not activate nor attest the original `@Transactional` annotation**, and cannot by itself close the “actual production Spring wiring” requirement. The later production-activation decision is a separate controlled gate.

**B2 (remove final / inject broad @Service) = REJECT AS UNCONTROLLED QUICK FIX**. A deliberately designed façade/port remains possible under separate review, not as silent CI guard removal.

## 3. Findings and dispositions

| ID | Severity | Finding | Required targeted remediation / acceptance |
| --- | --- | --- | --- |
| SB-R1-01 | BLOCKER | **Stage B's stated actual-wiring goal is conflated with test-only effective boundary.** A Spring-free manual final-class object graph and test-only Template do not establish production U06 Bean wiring, entrypoint, proxy advice, or profile authority. | Split distinct outcomes: `B0_STATIC_WIRING_ATTESTATION`, `B1_ISOLATED_EFFECTIVE_TX_PROTOTYPE`, and `B2_ACTUAL_SPRING_PROFILE_INTEGRATION`. Explicitly require original target-app Bean factory + actual invocation-path/advice evidence for B2. If B2 remains unauthorized, Stage B must never be labelled actual production wiring verified. |
| SB-R1-02 | BLOCKER | **TxPort identity/proxy/propagation/isolation contract incomplete.** `JpaTransactionManager` chosen by type alone may mismatch the actual JPA/SQL resource; TX-D1 is “JPA or JDBC or JDK proxy” not frozen. `@Transactional(SERIALIZABLE)` on original final classes may not execute, and `TransactionTemplate` defaults could silently run at a weaker isolation level. | Define a typed `TxOwnerBinding` per stage with explicitly named tx-manager, JPA EMF, DataSource, JPA/JDBC participants, propagation (REQUIRED/REQUIRES_NEW as justified), effective isolation (SERIALIZABLE where original uses it), rollback rules and proof method; fail closed on missing or mixed manager. Choose one candidate TX-D1 model at B1, with its own tests. |
| SB-R1-03 | BLOCKER | **Original partial-commit/failure mapping isn't frozen.** Real `U06ProfileBApplicationService.mode2` catches consultation/runtime exceptions into `RECONCILIATION_REQUIRED`; `U06WaitCoordinator` may leave initialization, reservation or awaiting committed depending on actual ownership. Generic “otherwise RECONCILIATION_REQUIRED / FAIL_CLOSED” lacks precise matrix. | Freeze each cut S1, D1, S2, C1, I1, R1, A1 and T1/T2: after each successful commit/fault/unknown outcome, query authoritative tables via independent connection, specify allowed return status, retry/replay identity, terminal trace behavior and forbidden F8 eligibility. Explicitly separate `UNKNOWN_COMMIT` from `ROLLED_BACK`; no blanket transaction rollback or blind replay. |
| SB-R1-04 | BLOCKER | **StateCommitter cross-resource ownership not established.** A nonfinal `ClinicalCdpStateRepositoryAdapter` with `@Component/@Transactional` is not proof that injected U06 backend actually consumes that adapter, nor that CDP + wait/trace use the same transaction manager or database. | Trace `U06SyntheticP01Runtime.Backend` instantiation to concrete `StateRepositoryPort`, DataSource/EMF, actual transaction owner. If nonprod fixtures differ from target configuration, report two planes separately and prohibit cross-boundary atomicity claims. |
| SB-R1-05 | REQUIRED | **Design under-specifies TX-D1 synthetic six-table atomicity and replay**. Annotation on final interface implementation does not establish advice; blank choice between JDK proxy and Template is not executable contract. | Select *explicit, single-manager* candidate TxPort for B1, define six-table SQL write order, inject failure after writes 1..6, verify rollback and idempotent replay, distinguish synthetic receipt from external delivered. JDK-proxy path may be a separate negative/control experiment, not equivalent proof. |
| SB-R1-06 | REQUIRED | **B0/B1 test manifests and negative controls are not executable yet**. Stage A CI cannot be relabelled as B1 proof; existing Stage A artifact is scoped to its old HEAD. | Define exact file inventory/test fixture, controlled profile, expected test identities, manifest and DDL source SHA, physical connection and independent SELECT-only readback, evidence plane, never pretend runner/environment strings constitute indisputable physical isolation. |
| SB-R1-07 | REQUIRED | **Profile authority / legacy structural guard tension**. Existing workflow rejects U06 annotations/bean registration and real side effects; introducing code in a different package can still be scanned by `DiagnosisServiceApplication`. | B0/B1 must be test-only/non-scannable and use a minimal explicit context; keep `src/main`, original U06 and legacy workflow untouched. Production changes require separate Controlled Amendment and negative profile-registration tests. |
| SB-R1-08 | REQUIRED | **Success projection vs durable authority**. `U07ResumeEligibilityProjector` returns a deterministic hash after an asserted boolean, not physical resume authorization; `trace.complete` can fail separately. | `WAIT_ESTABLISHED` requires bound and independently checked C1/R1/A1 persistent data and governing authorization; projection/hash/trace alone must never authorize F8 APPLIED, scheduling or external effects. Add trace-failure and projection-failure scenarios. |

## 4. Required design delta (normative)

**Freeze B1 phase ownership as an auditable matrix**:

`S1/S2=StateCommitter backend explicitly external to local TxPort unless real wiring proves otherwise`;
`D1=JDBC_LOCAL_TEMPLATE_CANDIDATE` with **exact same JDBC DataSource** for six write statements, explicit isolation and no external calls;
`C1=JPA_LOCAL_TEMPLATE`, consultation + effect;
`I1=JPA_LOCAL_TEMPLATE`, thread creation idempotent and own commit;
`R1=JPA_LOCAL_TEMPLATE`, checkpoint + thread;
`A1=JPA_LOCAL_TEMPLATE`, thread awaiting-user + checkpoint validation;
`T1/T2=TRACE_LOCAL_TX` only if trace store and TxManager are positively bound; otherwise `UNKNOWN` and no silent “success” assumption.

These are *test-only candidate assignments*, not confirmations that U06 production executes this sequence transactionally. For every stage, record `sourceProvenance` (original vs wrapper), `managerBeanId`, `emfId`, `dataSourceId`, `connectionIdentity`, `propagation`, `requestedIsolation`, `effectiveIsolation`, `commitObservation`, `replayKey`, `reconciliationState`. Explicit expected status for `WAIT_CHECKPOINTED` with A1 failure = NOT_READY for resume. Same physical database is neither necessary nor sufficient to declare two stages globally atomic.

Acceptance for B1 isolated prototype: controlled Java8/MySQL8 source-bound CI; independently read back durable rows before/after every failure; explicit six-table D1 rollback; 2-owner C1/R1; I1 commit then R1 failure; R1 commit then A1 failure; trace terminalization failure after complete phase; wrong manager / wrong DS / wrong proxy control; conflicting/replay keys; actual proxy/advice evidence kept separate and UNKNOWN if absent. Negative-control failure must fail closed.

## 5. Formal readiness decision

- `STAGE_B_V01_DESIGN=REVIEWED_WITH_BLOCKERS`
- `B1_SHORT_TRANSACTION_DIRECTION=ACCEPTED_CONDITIONALLY`
- `B0_SOURCE_ATTESTATION=READY_FOR_DESIGN_REMEDIATION_AND_READ_ONLY_SOURCE_CHECK`
- `STAGE_B_B1_IMPLEMENTATION_READINESS=NOT_READY`
- `STAGE_B_ACTUAL_U06_WIRING=UNVERIFIED`
- `PRODUCTION_ACTIVATION=NOT_AUTHORIZED`
- `REAL_D5=NOT_READY`

**Recommended next step:** `D5-O2 Stage B — Targeted Design Remediation (SB-R1-01..08)`, create a precise normative v0.2 amendment for TxOwnerBinding/phase-fault matrix/manifest/profile separation. Re-review v0.2 before implementing any test-only transaction ports. No independent approval, production authority or merge decision granted by this review.
