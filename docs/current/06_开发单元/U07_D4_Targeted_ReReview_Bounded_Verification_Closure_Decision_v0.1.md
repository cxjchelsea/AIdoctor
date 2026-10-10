# U07 D4 Targeted Independent Re-Review + Bounded Verification Closure Decision v0.1

日期：2026-10-10。审查性质：本轮对已提交实现和原始 CI 日志的重新批判性审查；非另一名人类或独立组织的签字。仅文档，不修改实现，不合并。

## 1. 精确对象与证据来源

- 仓库：cxjchelsea/AIdoctor。
- 被审 PR：#371，OPEN / DRAFT / UNMERGED。
- HEAD：e825a8e6d9a3902856dd118b2f4e147451cbb37c。
- base：PR #370，827b35adc63aa3de674d05a2df66e808093c94b7。
- 相对 base 的四文件：owned runner、adversarial smoke、forced-exit worker、D4 workflow。
- 另审继承的 D3 coordinator、D4 inspector、旧 recovery smoke 和 application repository。
- CI 实际 checkout 为 PR 临时 merge commit 6b4deca2eaa7583c8f405b9d9e83e6c9dea58759，不是直接 checkout HEAD。已 fetch 此提交，并执行 git diff --exit-code HEAD FETCH_HEAD，返回 0，确认源码树一致；因此接受为该 HEAD 的源码等价证据，不冒充直接 HEAD 运行。
- D4 run：https://github.com/cxjchelsea/AIdoctor/actions/runs/37910005738；job 113752616628，SUCCESS。
- D3 run：https://github.com/cxjchelsea/AIdoctor/actions/runs/37910005856；job 113752617183，SUCCESS。
- 直接读取原始 job logs，确认下列实际输出，不仅依赖 PR 评论：
  - U07_D4_FORCED_JVM_TERMINATION=PASS pre_rollback=1 post_commit_durable=1
  - U07_D4_SYNTHETIC_RECOVERY=PASS assertions=17 concurrency_replay=1 explicit_db_conflict=0
  - U07_D4_ADVERSARIAL_OWNER_SMOKE=PASS assertions=74 races=10 (5 duplicate identity + 5 same-key conflict)
  - U07_D3_SYNTHETIC_JDBC_SMOKE=PASS assertions=18
- 本轮本地未重新执行 Java/MySQL：当前环境未发现 javac 或 docker。这里接受既有 CI 执行证据，不声称本轮重跑。

## 2. 可以接受的有限证据

| 项 | 判定 | 精确含义 |
| --- | --- | --- |
| owned runner 新连接归属 | PASS_OBSERVED | 每次创建自己的连接，未提交另一连接的待提交写入；不等于消除了原公开入口 |
| 同会话重复身份/同键竞争 | PASS_OBSERVED | 10 轮两线程测试中，每轮一个首次接受、一个 canonical/application |
| 提交前 abrupt JVM exit | PASS_OBSERVED | Runtime.halt 后新连接未读到两条未提交记录 |
| 提交后 abrupt JVM exit | PASS_OBSERVED | Runtime.halt 后读到 canonical 和 ACCEPTED application |
| 已知提交后隐藏响应再读取 | PASS_OBSERVED | 注入故障在成功 commit 后发生；不是真实 commit 成败不明 |
| 历史接受后会话推进再重放 | PASS_OBSERVED | 当前 fixture 在会话仍存在时保持历史重放 |

上述 PASS 限定于 disposable MySQL 8 + 原 V2/V3/V6/V7 建表 + synthetic fixtures；不是完整 Flyway 迁移成功，不是 Oracle，不是真实 F8。

## 3. 发现与关闭要求

### D4-RR-01：重放没有验证持久化应用阶段 — REQUIRED / 阻止本轮关闭

位置：U07D3SyntheticTransactionCoordinator.admitAndDecideSynthetic，canonicalEventId != null 分支。

现实现检查 application 存在及身份匹配，即 commit 并返回 SAME_EVENT_REPLAY；没有检查 phase、decision、revision、effectId。静态可复现路径：写入身份匹配 canonical + RECEIVED application 后再次调用，返回 SAME_EVENT_REPLAY；但 D4 inspector 对相同记录返回 RECEIVED_ONLY_REQUIRES_REVIEW。另一例：ACCEPTED + 非 ACCEPTED decision 或错误 revision，coordinator 仍返回 replay，inspector 返回 PARTIAL_OR_INCONSISTENT。

SAME_EVENT_REPLAY 本身不等于 ACCEPTED，也未触发真实业务副作用；问题是正常重放结果没有区分未完成/不一致状态，恢复合同相互矛盾。不得把 replay 当作历史已接受保证。

修复验收：明确 replay 的状态合同；只有本合成范围认可的 ACCEPTED/decision/revision/effect 组合可返回成功历史重放；RECEIVED、缺失、错误阶段/decision/revision、已绑定 effect 等明确输出需审查或拒绝。新增数据库回归证据，并证明会话推进后的合法历史 replay 不退化。若采用更宽的 replay 语义，必须返回独立的持久化状态，且冻结消费规则禁止提升为接受。

### D4-RR-02：旧竞态 smoke 的 SQL 异常归类过宽 — REQUIRED / 阻止本轮关闭

位置：U07D4RecoveryConcurrencyJdbcSmoke 的 worker catch(SQLException)。

任何 SQLException 都增加 dbConflictCount。只要另一 worker 接受、记录计数正确，连接或权限等非竞争异常也可能满足总数断言并 PASS。当前日志 explicit_db_conflict=0，说明这一轮没有靠异常掩盖通过；不否定已观察结果，但测试未来不可靠。

修复验收：统一新 adversarial smoke 的严格异常传播方式；进一步按 SQLState + vendor error code 明确唯一键/死锁类别，不将全部 23000 完整性错误等同于唯一键竞争。其他异常交给主线程失败。加入非冲突异常导致 FAIL 的证据。

### D4-RR-03：所有竞态都被同一个 consultation 行锁串行化 — REQUIRED / 覆盖缺口

两种 5 轮测试均使用 synthetic-consult-d3。D3 第一条 FOR UPDATE 锁会话，另一个事务不能同时穿过它，主要验证会话锁串行化后的 replay/conflict；不证明两个独立会话都完成身份空读后的唯一插入竞争。

关闭范围必须写为 SAME_CONSULTATION_SERIALIZED_RACES，而不是通用 unique-race / 多实例保证。若要关闭跨会话唯一竞争，新增两个独立合法 consultation/wait fixtures，共用 idempotency key，通过可控屏障触发空读到插入窗口，验证失败事务完全回滚、新连接 readback、单一 canonical/application 和有界完成。大量随机重复不能代替该 schedule。

### D4-RR-04：原 D3 公共入口事务归属未强制 — OPEN / 后续入口门禁

owned facade 的安全路径成立，但 public Connection 入口仍只验证 autoCommit=false，不能识别调用方此前待提交的其他写入。合法 synthetic 接受/replay 会 commit 整个传入连接，拒绝/异常会 rollback 整个连接。

本轮不得宣称 D3 原 blocker 已全面修复。后续正式入口必须限制访问或使用可验证的事务 owner/capability；用同一连接预置 sentinel 写入验证，不能只用另一连接测试替代。禁止 production 调用 synthetic 类；该 finding 阻止原公开入口全局关闭及生产化，不否定 owned-path fixture。

### D4-RR-05：恢复读取不是单一一致快照 — OPEN / 高级恢复门禁

inspectFresh 使用 READ_COMMITTED，inspector 分两条 SELECT 读 canonical 和 application。若第一条查询在另一个事务 commit 前、第二条在 commit 后，可产生暂时 PARTIAL_OR_INCONSISTENT。当前返回 fail-closed，不自动修复，是保守结果；不能声称所有原子提交下读取分类总是稳定一致。

后续选择单一 JOIN 查询或数据库适配的一致快照，或明确瞬态不确定读取合同及有限重读；提供受控交错证据。不得自动把 partial 当成数据库损坏或启动修复。

### D4-RR-06：合成写入入口的身份约束不完整 — OPEN / synthetic guard 完整性

D3 仅约束 eventId/idempotencyKey 的 synthetic-d3- 前缀；consultation/question/wait 没有 synthetic- 强制检查。owned runner 限制数据库 URL，但不会修复此输入合同，而 inspector 对这三项已有 synthetic- 校验。两入口约束不一致。

后续在任何 SQL/行锁前拒绝非 synthetic 的所有业务身份，并补反例；URL 查询参数也不应被视为完整目标隔离机制。当前固定 disposable fixture 无真实患者数据，未观察到越界操作。

## 4. Bounded Verification Closure Decision

TARGETED_RE_REVIEW = COMPLETED_WITH_REQUIRED_FINDINGS
EXISTING_MYSQL_CI_EVIDENCE = ACCEPTED_WITH_EXACT_TREE_PROVENANCE
OWNED_PATH_AND_FORCED_JVM_FIXTURES = PASS_OBSERVED
D4_BOUNDED_VERIFICATION_CLOSURE = NOT_CLOSED_REQUIRED_REMEDIATION
D3_ORIGINAL_PUBLIC_TX_OWNER_CLOSURE = NOT_CLOSED
D4_FULL_PRODUCTION_CLOSURE = NOT_READY
U07_FULL_UNIT_CLOSURE = NOT_ESTABLISHED_BY_THIS_REVIEW
D5_IMPLEMENTATION_OR_ACTIVATION = NOT_AUTHORIZED_BY_THIS_REVIEW
CLINICAL_F8 / PHI / PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED

不撤销已执行的 17 / 74 / 18 条断言和 forced-exit 证据；不扩大这些证据的覆盖范围。数据库服务崩溃、commit 正在执行时网络断开、跨进程/多实例锁与租约、Oracle、APPLIED/outbox/真实 F8 全在本轮之外。原 V1 CLOB 与部署 Flyway 历史问题保持独立未解决状态。

## 5. 下一步

执行 U07 D4 Targeted Replay-State + Verification-Harness Remediation：
1. 收口 RR-01 与 RR-02，新增针对性的失败与合法历史重放证据。
2. 对 RR-03 明确选择仅同会话范围，或增加跨会话受控唯一键竞争。
3. RR-04/05/06 保留显式未关闭状态；若随后扩大入口/恢复范围，先解决相应 finding。
4. 在新精确 HEAD 重跑相关 CI，然后重新进行 bounded closure 决策。
5. 关闭判断、实现授权、合并授权分开；本文件不授权任何 PR 合并。

本报告是可审阅结果，不要求开展生产连接、真实患者操作或完整项目重新实现。
