# U07 D5-P 前置入口与恢复边界设计 + 实现就绪评估 v0.1

日期：2026-10-10。状态：PROPOSED / READY_FOR_TARGETED_DESIGN_REVIEW。
基线：PR #373 HEAD 4a2ea4ebbc2e7359a2c8292895646d89ac113d1b；D4 决策 PR #374 HEAD 0924e08d22260212b202a1d2eb5f432e74b2d53a。均未合并。仅设计，无代码/SQL 执行或合并。

## 1. 与原路线接轨

原 PR #360 的 D5 定义为 Authorized application：真实 source readback、F8、P01/G2、outbox/consumer receipt、target runtime binding。该原定义保持不变。
本阶段新增前置标签 D5-P，不把 synthetic runner 包装成真实 D5，不因 D4 的 bounded PASS 推导真实 APPLIED。

D4 只关闭 disposable MySQL 合成验证范围；旧 D4 路线列出的数据库重启、完整阶段和更广并发没有因此全部完成。跨会话唯一键竞争、Oracle、完整 Flyway 部署仍分开待验证。

| 阶段 | 目标 | 当前判定 |
| --- | --- | --- |
| D5-P1 | 唯一事务 owner，消除原公开 Connection 提交入口 | 设计已具体化，待评审/实现 |
| D5-P2 | 单语句恢复证据 + 不确定提交语义 | 设计已具体化，待评审/实现 |
| D5-P3 | SQL 前完整 synthetic 身份与测试目标约束 | 设计已具体化，待评审/实现 |
| 原 D5 | 经授权的真实应用与回执闭环 | NOT_READY |

## 2. 当前实物与影响范围

U07D3SyntheticTransactionCoordinator 当前 public 方法接收任意 Connection，自己 commit/rollback。
U07D4SyntheticOwnedTransactionRunner 每次新开连接，但仍委托 D3 提交，URL 接受任意查询参数。
U07D4SyntheticRecoveryInspector 在 READ_COMMITTED 下分两条 SELECT，可能跨两个提交快照。
D3 仅检查 event/key synthetic-d3- 前缀；inspector 对 consultation/question/wait 有额外约束。
D2 application/outbox repository 已不提交，继续保持 storage-only。
Foundation CanonicalBusinessEventLedger 是现有 Spring/JPA 身份 owner，其 DataIntegrityViolationException 后同事务读取、flush 时机和事务桥接未验证；D5-P 不接线或修复该生产层。

D5-P 允许的未来变更清单：D3 coordinator、D4 owned runner、D4 inspector；新增 synthetic 输入校验/结果类型；D3/D4 JDBC smokes、forced-exit worker、两份 isolated workflows、必要 architecture tests。
不修改 migrations、Foundation JPA ledger、U06/P01/G2/F8、生产 Spring wiring 或正式临床枚举。不新增 effect/outbox dispatch。

## 3. P1：事务 owner 与调用图

公共边界仅允许 execute(SyntheticInput, now, fault) 和 inspectFresh(SyntheticInput)。均不接受 Connection、DataSource、调用方事务或任意 SQL callback。
runner 通过固定 disposable test target 新开连接；唯一负责开始/commit/rollback/close。
D3 coordinator 改为 package-private transaction body；不再公开可被外部调用的 Connection API，不持有 commit/rollback 权限。仓储也不提交。
同 package 为受信任内部实现范围；Java package-private 不是抵抗恶意反射的安全沙箱。架构测试限制生产入口只能调用 runner，不能以此宣称 JVM 内任意代码无法访问数据库。

执行顺序：
1. 校验输入、时间、fault 和 test target；失败不得创建连接或执行 SQL。
2. 新开专属连接，设置 READ_COMMITTED、autoCommit=false。
3. coordinator 执行既有 synthetic body，返回 ACCEPTED / SAME_EVENT_REPLAY / REJECTED_CURRENTNESS，不结束事务。
4. owner 对 ACCEPTED/replay commit；对 currentness rejection rollback。异常按提交阶段分类并清理。
5. close；返回结果中不能泄露连接或凭据。

保持当前 D3 consultation → canonical → wait（首次接受）→ application 锁路径；此顺序只适用于本 isolated synthetic 实现。PR #360 的 canonical-first 是未验证 proposal，不能与本路径混合到生产。真实 D5 与 JPA/U06 的统一锁序仍为独立门禁。

迁移所有直接调用：U07D3SyntheticAdmissionJdbcSmoke、D4RecoveryConcurrencyJdbcSmoke、ForcedProcessExitWorker、owned runner。删除 public 方法而非保留 deprecated bypass。原测试直接 Connection 接口不承诺兼容；测试 fixtures 仍可自建连接，但不得借公共执行 API 传入。

P1 验收：源码/反射证明无 public Connection 执行入口；coordinator/repository 无 commit/rollback；owner terminalization 路径可计数；调用方另一连接的 sentinel 保持未提交；错误/currentness rollback 无 durable 新行。原“同连接预置 sentinel”误提交反例在接口删除后转为不可构造；不得再保留公开 callback 来测试它。

## 4. P2：恢复必须一次读完整证据

选择一个普通 nonlocking SELECT，把请求锚点、canonical 双身份候选、application 合成一个结果集。不要分两次查询，不用 SELECT FOR UPDATE，不调用读表函数，不在读取事务中先写再读。

逻辑形状（设计伪 SQL，绑定参数，不是本轮执行的迁移）：

    request_anchor(request_event_id)
      LEFT JOIN canonical_business_event c
        ON c.event_id = request_event_id OR c.idempotency_key = request_key
      LEFT JOIN u07_event_application a
        ON a.event_id = request_event_id

anchor 始终一行，确保 canonical 缺失而 application 存在的 orphan 也可被看到。不要只以 canonical 为主表，否则 orphan 被误判无记录。
实现使用 MySQL/Oracle 方言适配的参数锚点；MySQL 可 CAST bind AS CHAR(128)，Oracle 对应 VARCHAR2(128)。列显式别名，所有身份、phase/decision/revision/effect 同次读取，最多两个 canonical 候选，超过一个判 split identity conflict，不把重复 join 行当正常结果。
数据库唯一键限制两个候选；仍显式检查行数与 identity，不能按首行挑选赢家。

| 同次读取结果 | 返回证据状态 | 权限含义 |
| --- | --- | --- |
| 无 canonical 与 application | NO_DURABLE_EVENT | 该快照未见记录；不代表不确定提交永远失败 |
| canonical 双候选/字段不匹配 | IDENTITY_CONFLICT | 明确拒绝，不重试写入 |
| 仅 canonical 或仅 application | PARTIAL_OR_INCONSISTENT | 保守输出需审查；无修复权 |
| 两者匹配、RECEIVED/NULL/0/no effect | RECEIVED_ONLY_REQUIRES_REVIEW | 不是 ACCEPTED |
| 两者匹配、ACCEPTED/ACCEPTED/1/no effect | HISTORICAL_ACCEPTED | 仅合成历史接受，不是 APPLIED |
| 其他 phase/decision/revision/effect 组合 | PARTIAL_OR_INCONSISTENT 或已有 identity conflict | 不提升权限 |

读取 SQLException 返回/抛出独立 READ_UNAVAILABLE，不能转为 NO_DURABLE_EVENT。使用新连接只读事务，结束时 rollback；readOnly 仅是驱动提示，不依靠它授予/拒绝权限。
同次快照成立的适用条件：MySQL InnoDB、普通一致 SELECT、无脏读、无 DDL；Oracle 语句级读一致性。设计采用单语句是根据官方一致性合同作出的选择；Oracle 仍需真库验证，不由文档代替。

参考：
- https://dev.mysql.com/doc/refman/8.0/en/innodb-consistent-read.html
- https://docs.oracle.com/en/database/oracle/oracle-database/19/cncpt/data-concurrency-and-consistency.html

P2 验收：原双 SELECT 可控交错反例；新查询 SQL 计数=1；atomic writer commit 前/后结果只落对应完整快照，不混合拼接；orphan、split key、partial、RECEIVED、坏 revision/effect 等反例。将 writer commit 与 SELECT 发起用 latch 明确排序，禁止用 sleep 或仅多次随机执行宣称覆盖。若要证明 statement 执行中交错，用驱动/DB 可观测同步点另补证据，不冒充普通 latch 已覆盖。

## 5. 提交与响应的不同事实

不用现有 acknowledgementKnown 单布尔承载全部含义。设计结果至少分三项：
- transactionStatus：NOT_STARTED / ROLLED_BACK / COMMITTED / COMMIT_OUTCOME_UNKNOWN。
- responseStatus：AVAILABLE / SUPPRESSED_AFTER_COMMIT / CLEANUP_FAILED。
- durableObservation：上述恢复状态，带 observedAt；可以 absent（尚未读取），不是 write outcome 的替代。

提交前业务或 SQL 失败：owner 尝试 rollback，记录原错误及 suppressed cleanup errors；没有进入 commit 且没有自行提交的 DML/DDL，不宣称业务已接受。
commit 已进入但抛异常：COMMIT_OUTCOME_UNKNOWN；rollback 即使成功也不能证明 commit 未成功。弃用该连接，允许独立只读恢复，不自动重发。
commit 正常返回：COMMITTED；之后响应被注入隐藏或 close 失败不会把已知 COMMITTED 改成 ROLLED_BACK。调用方可能仍拿不到响应，故保留 responseStatus 区分。
BEFORE_ADMISSION 是测试故障，不自动等同真实进程退出。AFTER_COMMIT_ACK_LOSS 保留名称兼容但明确仅隐藏已知提交结果。

未知提交后读到 NO_DURABLE_EVENT：仍不确定，不自动首次插入。读到 HISTORICAL_ACCEPTED：仅确认该 event 当前历史接受证据，不创造 APPLIED 或恢复 mutation 权限。本阶段无自动 retry、replay dispatch 或 receipt 合成。
若采用 exception 替代 result，也必须携带上述事实，禁止单 SQLException 隐去 commit 已知状态。

## 6. P3：SQL 前输入与目标隔离

集中 SyntheticInputValidator，在 public execute/inspectFresh 和内部 body 入口调用，复用同一规则；业务 body 调用是防内部误用，不授予临床权限。

| 字段 | 约束 |
| --- | --- |
| eventId / idempotencyKey | synthetic-d3- 前缀、非空、trim 后与原值相同、最多128 ASCII 字符 |
| consultationId / questionId / waitEffectId | synthetic- 前缀、同上 |
| digest | 非空、最多128；不得把 digest 当认证签名或跨 event 去重规则 |
| expectedConsultationVersion | >=0；权威值仍从 DB 校验 |
| now / fault | 非NULL；只接受已定义测试 fault |

身份字符集固定为 ASCII 字母数字及 . _ : -，禁止空格/control/unicode 混淆。SQL 一律 PreparedStatement 参数化。前缀只是 fixture scope，不是患者隔离或授权证明。
违规必须在 DriverManager.getConnection 前抛 INVALID_SYNTHETIC_INPUT；代理/mock 验证连接计数和 SQL 数为0。

新 test target 配置只允许固定配对：
- 127.0.0.1:33320 / u07_d3_synthetic。
- 127.0.0.1:33321 / u07_d4_synthetic。
URL 由受控配置生成；用户不能传任意查询参数、socketFactory、host、路径、multi-host 或替代 schema。connector 参数固定为现有 disposable smoke 的明确值。凭据仅内存/env 输入，日志不得输出 password、完整连接属性。
容器仅绑定 loopback、随机凭据、独立数据库、退出销毁；这些约束共同构成隔离。未来加端口/数据库属于 profile 明确变更，不设“任意 localhost 安全”例外。

## 7. 准备实施的最小测试矩阵

| ID | 目标 | 必需证据 |
| --- | --- | --- |
| P1-T1 | 无公开 Connection bypass | 编译/反射/调用点扫描 |
| P1-T2 | 唯一 owner | terminalization 测试，所有 body/repo 无 terminal call |
| P1-T3 | 失败/currentness/异常回滚 | 新连接0条新记录及 sentinel未提交 |
| P2-T1 | 单语句与 orphan/split | SQL计数及完整类别 readback |
| P2-T2 | 快照前/后受控 commit | latch排序，不混合拼接 |
| P2-T3 | commit正常/抛异常/close异常 | JDBC fault injection，status与副作用回读相符；真实网络未知提交仍另测 |
| P3-T1 | 每个业务身份越界 | 参数化反例，连接/SQL次数0 |
| P3-T2 | URL/目标越界 | profile构造反例，连接次数0 |
| REG-1 | 合法历史 replay / 7类无效状态 | 保留 #373 75断言语义 |
| REG-2 | D3 / owner / recovery / JVM exit | 全部既有 smoke，编译Java8 |
| REG-3 | worker 非冲突故障 | 注入真实worker路径并验证进程非零，补强旧 classifier-only 证据 |

不按断言数冻结测试，按行为验证；任何变更必须在新的精确 HEAD CI 核验，旧 #373 CI 不能当新实现 PASS。

## 8. 实现就绪结论与明确阻塞

| 范围 | 判定 | 原因 |
| --- | --- | --- |
| D5-P 详细设计 | READY_FOR_TARGETED_DESIGN_REVIEW | API、事务职责、查询形状、结果合同、profile、调用迁移及验收已明确 |
| D5-P 合成MySQL实现 | CONDITIONAL_READY_AFTER_DESIGN_REVIEW | 当前基线和隔离CI可用；需先接受删除旧public API及新结果合同，完成设计复审 |
| 原 D5 真实业务应用 | NOT_READY | authority/source/F8/P01-G2/receipt/target绑定缺少本轮实际验证；JPA/JDBC事务桥接和统一锁序未验证 |
| Oracle及部署 | NOT_READY | 缺Oracle执行证据，完整迁移/Flyway部署历史仍待解决 |
| main 集成/merge | NOT_AUTHORIZED | 当前stack未合并；需单独 exact-main 集成与合并决定 |
| 临床/PHI/生产 | NOT_AUTHORIZED | 本轮无此范围授权 |

独立人员签核未建立，不把本助手自审当作外部reviewer签名。设计评审不要求真实患者或公司数据库。
本文件没有宣称 RR-04/05/06 已关闭；它们只能在相应实现与证据通过后关闭。

## 9. 下一步执行顺序

先进行 D5-P Targeted Design Review，重点只检查：owner终结事务唯一性、single-query orphan/split覆盖、unknown commit结果不被重读升级、validator在连接前拒绝、旧入口调用迁移不留bypass。
通过后进入 D5-P Synthetic Entry/Recovery Boundary Implementation，按 P1→P2→P3 同一隔离实现PR推进，真实CI后再关闭这三项finding。原 D5 的真实应用另做 readiness 与授权，不把它偷偷合并进前置修复。
