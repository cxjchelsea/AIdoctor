# U07 Durable Event / Application-State Transaction — Design + Implementation Readiness v0.1

> 2026-10-09 · 设计与就绪评估，非实现授权 · 基于 `main@86e8843197091c8c8172b7e4213537a31bdf0654` 和尚未合并的 PR #359 `9f32108a104248704ea0e6ab59c4c5d55be552e8`。
>
> **Verdict: DESIGN_DIRECTION_ACCEPTED_FOR_TARGETED_PROTOTYPE; DURABLE_IMPLEMENTATION = NOT_READY; REAL_CLINICAL / PHI / P02-U02 / PRODUCTION = NOT_AUTHORIZED.**
>
> 此文只定义持久化边界及具体实施门槛。既有 PR #353–#359 是合成隔离代码，虽已有 71 项兼容执行测试证据，均 **未合并 main**，不等于真实 U07 生命周期已实现。没有授权 SQL 执行、部署或迁移。

## 1. 设计目标与必须遵守的不变量

在同一数据库事务内一致地记录 **canonical event identity、U07 application state 与纯业务判定记录**；在明确的下游应用阶段，通过持久化应用凭据和幂等 effect key 确认 APPLIED。不能仅凭 event RECEIVED、F8 ACCEPTED、transport retry、Checkpoint 是否可用、调用方布尔标志、或者合成回执而推导真实 APPLIED。

- `canonical_business_event.RECEIVED identity != F8 ACCEPTED != business effect APPLIED != P02 resumed != U02 delivered`。
- `USER_ANSWER / RESUME_REQUEST` 的 Clinical Fact 解释仍归 U02；U07 不直接写 Clinical Truth。
- **Business Resume validity 与 Runtime Resume compatibility 是两个不同判定**；checkpoint 缺失不得反向伪造 F8 REJECTED。
- 一切真实事件必须由经认证的 ingress/resolver 和权威当前状态读取产生证据；客户端不可指定 trusted/source/current/applied。
- 真实状态与事务必须由一个所有者负责；多个表的应用与回执不可分别提交然后谎称原子。
- Oracle 和 MySQL 分别验证；不可把 `SERIALIZABLE` 或唯一键异常捕获等同为已验证跨数据库并发语义。
- `SAME_EVENT_REPLAY` 必须查询既有 authoritative application row，未查明状态则 `RECONCILIATION_REQUIRED`，不能默认 `DUPLICATE`。

## 2. 本次仓库实物盘点（核验过的文件）

| Existing asset on main | 已证实的作用 | 可复用范围及缺口 |
|---|---|---|
| `runtime/foundation/CanonicalBusinessEventLedger.java` | Spring `@Transactional resolveOrCreate`，先查事件 ID/幂等键，持久化规范身份 | **保留并复用** identity owner；不负责 ACCEPTED/APPLIED，且并发唯一键异常发生于当前事务内时的恢复路径需要真实 DB 验证 |
| `CanonicalBusinessEventRecord.java` + `CanonicalBusinessEventRepository.java` | JPA entity, `event_id` PK, `idempotency_key` 唯一，`payload_digest` | 无 question/wait、event application phase、claim lease、decision/commit receipt 字段 |
| `db/migration/V2__create_clinical_runtime_foundation.sql` | MySQL 定义 `canonical_business_event` | 可复用；需新增 U07 表而非复制事件身份表 |
| `db/migration-oracle/V3__create_clinical_runtime_foundation.sql` | Oracle 的同表及约束 | 需同构的专用 U07 migration 和 Oracle 集成验证 |
| `u06/wait/ConsultationWaitTransitionService.java` | `SERIALIZABLE`、锁 consultation、行版本校验、等待 effect 一致提交 | 候选锁顺序与生命周期前置依据；不直接具有 U07 apply 权限 |
| `db/migration[/\u002doracle]/V6__add_u06_wait_runtime.sql` | U06 consultation wait / runtime checkpoint / delivery ledger 等表 | 可由权威仓储验证 WAITING、question binding、checkpoint；但不能假定全部属于同一事务或 F8 owner |
| `u06/wait/U07ResumeEligibilityProjector.java` | 将 parent wait + checkpoint 投影 eligibility hash | 只是 U06 handoff helper，不是生产 U07/源认证 |
| PR #359 (未合并) | Synthetic Ledger→F8→application-state read-only reconciliation | 可作为纯业务单元测试模型，不能直接连生产；目前 caller-constructible provenance 与 volatile maps |

注意：`canonical_business_event` 约束现为 `event_id` PK + `idempotency_key` 唯一。所有归一化映射、同一语义不同 event_id 的处理和 digest 签名定义尚需明确；**不得**简单按内容摘要去重。

## 3. 建议的最小持久化数据契约（proposal, 尚未落库）

沿用现有 `canonical_business_event` 为唯一 canonical identity owner；新增而不是替换：

### 3.1 `u07_event_application`（一个 canonical event 一行）

- `event_id` VARCHAR(128) PRIMARY KEY, FK/reference to `canonical_business_event.event_id`（FK 能否直接加取决于迁移及既有数据调查，未验证之前采用显式应用层一致性检查）。
- `consultation_id`, `question_id`, `parent_wait_effect_id`, `payload_digest`（均非空、与 canonical identity/权威 wait 对齐）。
- `source_event_ref`, `source_version_ref`, `source_state_version`, `business_policy_ref`、`trace_ref`：保留证据身份与版本，不保存未经授权的明文回答／PHI。
- `phase`: `RECEIVED | ACCEPTED | APPLIED | FAILED`；`decision`: `ACCEPTED | DUPLICATE | EXPIRED | REJECTED | NULL`（其中只有适用的 ACCEPTED 可推进应用；`RECONCILIATION_REQUIRED` 为查询/编排结果，不是持久化业务 verdict）。
- `row_version` BIGINT NOT NULL、`owner_token`、`lease_expires_at`（适用时）、`created_at`、`updated_at`、`terminal_at`。
- `receipt_ref`、`effect_id`、`effect_fingerprint`（仅真实权威、匹配 event_id/digest/state version 的应用回执方可写入）。
- Unique(`effect_id`) 允许空值的数据库差异须经双数据库验证；不能依赖不同平台对 NULL uniqueness 的推测。
- `event_id` 唯一键 + `row_version` CAS + wait/question/current state readback 共同防双写；单一主键不提供真实副作用 exactly-once。

### 3.2 `u07_effect_outbox`（仅针对需要异步推进的授权后续动作）

- `effect_id` PK、`event_id`、`target_type`、`payload_ref_or_digest`、`effect_status`, `attempt`, `owner_token`, `lease_expires_at`, `last_error_class`, `created_at`, `updated_at`。
- Outbox 插入与正式应用意图状态变化在**同一事务**内；外部 P02/U02 消费由幂等消费者独立处理，必须持久化消费回执/下游去重。
- 第一阶段只可写 **non-dispatching synthetic outbox** 测试；任何真实 P02/U02 命令都另需专属授权。

**禁止**在数据库中仅凭合成 `receiptRef` 将真实应用 `phase` 置为 `APPLIED`。

## 4. 事务分段与锁序

| Transaction | 同事务写入 / 校验 | 事务外行为 |
|---|---|---|
| TX-A Canonical Admission | 权威 source provenance 校验 → 解析现有 canonical identity → 锁 consultation/current wait/question basis → `u07_event_application` insert-or-match with `RECEIVED` and immutable bound refs；唯一冲突应在新事务或 savepoint 安全查询 winner | **无**真实业务执行 |
| TX-B F8 Decision Commit | 重读权威 currentness/同一 row version → 持久化 exact F8 decision & trace；仅合法 ACCEPTED 推到 `ACCEPTED`；其他结果保留终态证据，不可通过应用分支 | 禁止调用外部 Runtime |
| TX-C Apply Intent / effect | 锁同一 event application + current wait state → 检查 Approved U07 effect 与 P01/G2 authority → 同事务更新被授权的本地状态 + 插入唯一 effect/outbox；只有确定本地提交回执才记录对应 APPLIED 语义 | 对外 P02/U02 只能由可恢复的后续消费者执行，且不能冒充 TX-C 里已完成 |
| TX-D Reconciliation | 只读 current ledger/application/outbox/receiver receipt 并比对 digest、版本、scope、owner；若缺证据给出 `UNKNOWN / REQUIRES_RECONCILIATION`，禁止盲重试 | 后续人工/授权 recovery-owner 分配 |

**锁序拟定**：canonical event row → consultation/current wait row → U07 application row → effect/outbox row。具体跨 JPA/JDBC 的隔离、死锁处理与实例竞争需在实际 DB 测试后冻结；不允许不同路径逆序拿锁。

**关键问题**：现有 `CanonicalBusinessEventLedger.resolveOrCreate()` 捕获 `DataIntegrityViolationException` 后继续查询 winner，并不自动证明该 transaction 在 PostgreSQL/Oracle/MySQL 的任意驱动场景都能继续安全使用。需覆盖 `saveAndFlush` 触发异常、transaction rollback-only、原子重试在新 transaction 读取 winner 的场景；此问题是实施门禁之一。

## 5. 崩溃窗口与预期恢复

| Crash point | Durable truth | 正确恢复行为 |
|---|---|---|
| C0 事件到达、TX-A 前 | 可能没有新记录 | 重做标准 canonical resolution（需相同 idempotency key） |
| C1 TX-A 已提交、TX-B 前 | identity + RECEIVED | 只读检查，按新状态/版本重新评估 F8，不能因存在记录就说 DUPLICATE |
| C2 TX-B ACCEPTED 已提交、TX-C 前 | ACCEPTED, 无 effect receipt | `RECONCILIATION_REQUIRED`；只允许授权 owner 检查恢复 |
| C3 TX-C 提交前崩溃 | 本地事务整体回滚 | 查 authoritative DB 状态判断提交与否，不凭请求端超时猜测 |
| C4 TX-C 提交后、outbox 投递前 | effect/outbox durable | 消费方使用 `effect_id` 幂等处理；不能重复建立新的 clinical effect |
| C5 下游成功但回执落库前 | 外部效果可能已产生，状态未确定 | 按 effect id 向下游查询权威回执／重试策略，不可盲二次下发 |
| C6 回执已保存、响应丢失 | durable receipt | 重放读回同一历史结果，不重新 APPLY |
| C7 checkpoint 缺失/不兼容 | Business verdict may remain ACCEPTED | Runtime failure/repair 单独记录，不改写成 Business REJECTED |

## 6. 代码实施切片与真实验收门槛

**D1 — Schema-only / no runtime activation**：两个 DB 方言的 U07 next-version migration、约束与索引、回滚/重复迁移策略；本地隔离数据库迁移 smoke。禁止修改线上表。

**D2 — Repository contract**：`U07EventApplicationRepository` + `U07EffectOutboxRepository`；typed domain records, immutable event digest, revision CAS, transaction-bound insert-or-read；测试异常后新事务 winner readback。

**D3 — TX-A/TX-B synthetic DB integration**：经固定合成输入、事务/唯一键锁与 rollback，验证 FIRST_SEEN/replay/conflict；区分已收到、已接受和已应用；**不允许**来源字符串自称权威。

**D4 — Recovery read-only**：数据库重启后的 RECEIVED/ACCEPTED/APPLIED/FAILED/UNKNOWN 恢复验证；真实同一 event_id 与不同 payload_digest 的冲突保持可追踪。单机重启 != 多实例稳定；分别检查。

**D5 — Authorized application, later**：P01/G2 + real source readback + accepted F8 + outbox/consumer receipt and target runtime binding，需分别授权，不属于 D1–D4。

**最低必测**：
1. event ID/idempotency key 重放、冲突与不同 ID 同 payload；缺 provenance / wait / question / state version 不可接受；
2. 10+ 并发同事件只留下一个 identity 与一个 application row；锁序与 deadlock retry；
3. 事务 C1–C6 故障注入、重启、事务原子性、rollback-only 后 winner 回读；
4. application row `APPLIED` 必须有匹配的真实 authority-bound effect evidence（D1–D4 的 synthetic fixture 不得通过生产映射）；
5. Oracle / MySQL migration parity，真实 DB isolation semantics、日期/索引/NULL unique difference；
6. full `mvn test` including Spring transaction + integration suites on clean CI；
7. U06 WAITING_USER、Pending Question owner、F8 与 P02 compatibility 分离；禁止 direct Clinical Truth write 与 PHI 泄漏。

## 7. 实施就绪审查（截至本次核验）

| Gate | Evidence | Verdict |
|---|---|---|
| S0 — U07 synthetic business reference | PR #359 七组合成测试历史证据 71/71，非 Maven/JUnit 正式运行；未合并 | AVAILABLE_FOR_DESIGN_ONLY |
| S1 — Existing durable identity foundation | main 的 canonical JPA entity/repo/ledger + MySQL V2 / Oracle V3 schema | PASS_REUSE_INPUT |
| S2 — Current wait / checkpoint provenance | main U06 WAITING and checkpoint schema + U06 services | PARTIAL / needs exact U07 owner locking/readback |
| S3 — U07 application/outbox schema and repository | main 没有已核验的专属实体/持久化状态/真实事务实现 | **BLOCKED** |
| S4 — Atomic transaction & race proof | 未运行 DB contention / rollback-only / crash proof | **BLOCKED** |
| S5 — Evidence trust / issuer / APPLIED receipt | synthetic DTO caller-constructible, no authenticated source | **BLOCKED** |
| S6 — Oracle/MySQL realistic DB test | dual migrations exist for U06; no U07 DB test evidence | **BLOCKED** |
| S7 — Exact-main integration & merge preconditions | #353–#359 draft unmerged, no exact-main merge and official JUnit/Maven evidence | **BLOCKED** |
| S8 — Clinical / real-patient / PHI / production | no delegated authorization | **NOT_AUTHORIZED** |

### Decision

- **Detailed Design = PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW**，不是已冻结契约，也不是批准 SQL 或 runtime 的设计。
- **Readiness for bounded D1 isolated schema prototype = CONDITIONAL**：必须先明确 DB profile / test target、migration numbering 和临床数据隔离，采用新分支 + 非生产数据库，方能真正执行迁移测试。
- **Readiness for D2–D4 implementation = NOT_READY**：需先完成 schema/migration 和基础 contract review。
- **Readiness for D5 true applied/resume/clinical integration = NOT_READY / NOT_AUTHORIZED**。

## 8. 下一步（Code-First，避免重复治理循环）

**优先 D1 单独实施切片**：在新分支只新增 MySQL/Oracle 同构 next-version migration + 最小的 JDBC repository contract tests（无业务连线）；先本地迁移/回滚/唯一键测试，再以真实 diff 和测试报告作为证据。该步骤不需真实患者数据、不连接真实问诊流。

本文件没有修改冻结 Unit Spec/RDP，也不替代它们。涉及 canonical F8 precedence、owner、P01 mutation、historical binding 等冻结语义冲突，必须先核对权威版本并做明确 amendment；不能由合成实验的代码/本文件暗中替换。
