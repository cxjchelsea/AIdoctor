# U07 D5-O2 Stage B — RACE-05 F-ISOLATED Deadlock Observer Privilege / Redaction / Incident-Binding Detailed Design + Scoped Implementation Readiness v0.1

日期：2026-10-13。**仅设计与就绪审查；不批准实施、不批准合并、不批准生产接线。** 本报告由同一助手执行技术审查，不构成第三方独立安全签署。

## 0. 精确输入与证据基线

- 远程主线精确 SHA：`20a54b4a7478641d8b8d8c12c6213b8e2ea73acc`，无漂移。
- P1-C 原始并发实现 Draft PR #457：`eac72825ca80ad34406a1fbd620eed20c17db3fe`，六个 ADD-only 测试/CI 文件，未合并。前期设计及重试边界报告 Draft PR #460：`23a9da316e48db6c791b79f451bf103e089e272e`。
- 同一实现 HEAD 的真实 Java8/MySQL8 三轮结果：PASS / FAIL(RACE05) / PASS；失败轮次由 MySQL `40001:1213` 回滚 Writer 37，另一 Writer 36 提交完整六表，独立 Reader 38 分别见到 [0,0,0,0,0,0] / [1,1,1,1,1,1]。原始 Artifact #11781323803 / #11782500392 / #11782018387。
- **证据边界：** 已确定死锁受害者回滚；**未**取得当时真实 InnoDB wait-for graph。也未授权自动重试或将原始 RACE05 的“两个单次事务都成功”标准改为“一方死锁也算通过”。
- 原始 `JdbcU06DeliveryStore.find()` 的四表 JOIN 缺少 Attempt/Receipt，错误重放缺口仍为 `CONFIRMED_NOT_SAFE`。本取证设计不得把取证结果等同于六表业务安全或可靠性策略授权。

## 1. 设计目标、不变量与禁止项

F-ISOLATED 仅重现并定位 *同一原始 Store、同一 SERIALIZABLE、两个独立业务 Effect* 的真实 InnoDB 1213 竞争；其主输出是**可验证的物理死锁证据或可信的“不可获得/不可关联”结果**，绝不是业务成功、生产安全或重试效果。

- 仅 Java 8 + disposable MySQL 8.0 专用服务：建议测试 Schema `u07_d5o2_f_deadlock`，本地端口 `33331`，Profile `U07_D5_O2_RACE05_F_ISOLATED_SYNTHETIC`。不得复用 P1-A/P1-B/P1-C 容器、凭据、端口或数据库，也不得用病患/生产数据。
- 两个 Writer 仍为不同物理 JDBC 连接，使用原始 Store 和原始 V2/V3/V6 DDL，独立 SERIALIZABLE 事务；协调 START 只可在开启事务之前，不可在持锁后设置人为屏障。第三个 Reader 只读六表、确认结果/原始回滚证据。
- 独立 Observer 使用单独连接，不参与业务事务，不持有业务锁，不执行表更新、事务重试或数据库行为修复。
- 所有尝试（包括没有死锁、死锁记录缺失、采集异常或 CI 失败）都必须有对应的机器可读证据；失败不能变通过，取证本身不得提高 Store 的权威性。

## 2. Observer 权限合同 — L0 / L1 两级，绝不泛化给应用账户

### L0：首选在线 Performance Schema，只读账户

`stagebf_observer_ps` 用独立随机凭据，仅限一次性本机 MySQL 服务。原则上仅授予以下表级 SELECT（具体需要的每张表必须在目标 MySQL 8.0 小版本上真实负控核验）：

```sql
GRANT SELECT ON performance_schema.data_locks
  TO 'stagebf_observer_ps'@'%';
GRANT SELECT ON performance_schema.data_lock_waits
  TO 'stagebf_observer_ps'@'%';
GRANT SELECT ON performance_schema.threads
  TO 'stagebf_observer_ps'@'%';
```

- 映射 `performance_schema.threads.THREAD_ID` ↔ `PROCESSLIST_ID` ↔ Writer 的 `CONNECTION_ID()`，锁等待关联从 `REQUESTING_THREAD_ID / BLOCKING_THREAD_ID` 和 lock IDs、transaction IDs 读取；只从允许的列中输出归一化数据。若为了查询源 SQL 需要额外的 Performance Schema 表，**先走 Scope Amendment**，本合同不默认授予。
- **负控必须真实执行：** Observer 对六张业务表 `INSERT/UPDATE/DELETE` 均被拒绝；对业务表 SELECT 也不能读取（若无需原始 SQL）；对 `SHOW ENGINE INNODB STATUS` 应无 PROCESS 而失败；`SHOW GRANTS` 不得包含 PROCESS/SUPER/ALL/FILE/RELOAD 或全库 DML。业务 Writer、Reader 的原始最小权限也必须独立检查。
- 按 MySQL 官方文档 `data_lock_waits` 表一般使用普通 `SELECT` 权限访问，并非必须 PROCESS；但实际运行仍需 MySQL 小版本上的权限矩阵实证，不能仅从文档推出账号配置必定有效。

### L1：事件补证，独立提升的诊断账户

`stagebf_observer_status` 必须是与 L0、Writer、Reader **不同的账户**，专用一次性 MySQL 服务，才可考虑 `GRANT PROCESS ON *.*` 以运行 `SHOW ENGINE INNODB STATUS`。该权限是全局诊断权限，**不是真正的表级最小权限**，只能用以下补偿控制接受：没有生产或其他项目连接、实例仅用于 F-ISOLATED、凭据一次性生成、观测完成即销毁服务、不提供远程网络访问、原始输出不进入 CI 日志或 Artifact。

- 优先不启用 L1；若 L0 捕获到完整有向等待边并能够绑定事件，则不读取 STATUS。
- L1 的启用需要独立批准（不能因本次设计 PR 自动授予 PROCESS）；CI 只能在 disposable MySQL8 服务上用临时 root 建立并**单独标明 PROCESS 使用**，不得把 root 凭据交给应用 JVM。
- L1 负控：无业务表读写权限、无 DDL/GRANT OPTION/SUPER/FILE/SHUTDOWN、不得调用修改数据库全局参数的语句；只针对死锁时点取一次受控快照，暂存于容器内权限 0600 的文件或内存，随 job 终止清除。

官方依据：MySQL 8 `SHOW ENGINE INNODB STATUS` 要求 PROCESS（https://docs.oracle.com/cd/E17952_01/mysql-8.0-en/show-engine.html）；`performance_schema.data_lock_waits` 使用 SELECT 权限（https://dev.mysql.com/doc/refman/8.0/en/performance-schema-data-lock-waits-table.html）。

## 3. 采集方案与时序

```text
disposable MySQL8 bootstrap (immutable DDL, test credentials, strict roles)
  → L0/L1 capability preflight + negative privileges
  → two Writer threads READY before TX
  → release START → begin two original SERIALIZABLE Store transactions
          ├─ L0 observer asynchronously samples narrowly scoped data_lock_waits
          │    only during bounded observation window
          ├─ on first real 1213 + both writers termination confirmed:
          │    OPTIONAL L1 reads latest deadlock once
          └─ workers finish / rollback / commit (no automatic retry)
  → independent Reader checks original six-table vector + column digests
  → parse/redact raw evidence inside isolated job
  → fail-closed typed evidence + original XML/raw test facts
  → discard raw status/locks and disposable DB
```

- **有界预算建议（须下轮核准）**：3 秒 READY；15 秒本次竞争整体限时；单事务 10 秒；InnoDB lock wait 5 秒；Observer 采集最多 15 秒，建议轮询间隔约 25–50 ms、最多 100 次（具体数值须审核其一致性，不能同时承诺 15 秒窗口及最多 100 次×50ms 就覆盖整段）；任务退出 5 秒。禁止无限循环和采样线程在测试完成后泄漏。
- 观测线程只能输出对业务请求的**观测事实**，不能通过锁/SQL 手段改变已冻结的原始 Store；观察本身对调度的扰动须标记于证据。失联/超时的 Observer 不得让 Worker 被错误分类为成功。
- STATUS 的 `LATEST DETECTED DEADLOCK` 可能是其他死锁，死锁后的 `data_lock_waits` 也可能为空。两个信息源任何一个缺失，都不能人工构造锁边。

## 4. Incident 归属合同：CONFIRMED vs BOUND vs UNAVAILABLE

### 会话关联链

必须由**实际物理采集**提供：

```text
test actor A/B logical effect digest
  → JDBC writer CONNECTION_ID A/B
  → performance_schema.threads(PROCESSLIST_ID -> THREAD_ID)
  → data_lock_waits REQUESTING/BLOCKING_THREAD_ID
  → data_locks ENGINE_LOCK_ID + ENGINE_TRANSACTION_ID
  → redacted OBJECT_SCHEMA/TABLE_NAME/INDEX_NAME + LOCK_TYPE/MODE/STATUS
```

`SHOW ENGINE INNODB STATUS` 如可用，必须同时对照两个 Writer 线程标识、原始 SQL 签名、对应时间窗口、限定六表和 MySQL server instance；**仅有“最近死锁”文本不构成本轮归属证明**。

### 独立输出字段

- `rollbackVerdict`: `ROLLBACK_CONFIRMED_1213` / `ROLLBACK_NOT_ESTABLISHED` / `UNKNOWN`。
- `liveLockGraphVerdict`: `LIVE_EDGES_BOUND` / `LIVE_EDGES_PARTIAL` / `LIVE_EDGES_UNAVAILABLE`。
- `latestDeadlockVerdict`: `LATEST_BOUND_TO_ACTORS` / `LATEST_UNBOUND` / `LATEST_MISSING` / `L1_NOT_ENABLED`。
- `incidentAttribution`: `FORENSIC_GRAPH_BOUND` 仅在两方身份、真实持有/等待锁关系、对象/索引、原始事件完全绑定时；否则 `FORENSIC_PARTIAL` / `FORENSIC_UNBOUND` / `FORENSIC_UNAVAILABLE` / `FORENSIC_CAPTURE_FAILED`。
- `businessCompletion`：一律不由 Observer 判断，标记 `NOT_AUTHORIZED_TO_ATTEST`。原始 RACE05 原有判定保持原样。
- `sourceReplayGap`：始终 `CONFIRMED_NOT_SAFE`，不得随取证通过改为安全。

**时间说明：** Java `System.nanoTime()` 仅可比较同一 JVM 内事件间隔；MySQL `LATEST` 的墙上时间必须明确带时区和时钟误差窗口，不能用单调纳秒与数据库墙钟直接比较。若无法可靠对时，时间维度应是 `UNBOUND`，不能提升事件归属等级。

## 5. 安全脱敏与 Artifact 合同

**不要先上传原始数据再去脱敏**。Parser 必须采用正向列/枚举提取、拒绝未知列/模式、对结构外内容 fail-closed：

- 只允许经审核的 schema/table/index 标识（限定当前实例的原始六张 U06 表及其经 DDL 枚举的索引）；会话 ID、lock mode、lock type、row-level waiting/blocking 关系，经过类型校验后输出。
- 禁止输出：raw SQL text、SQL literals、原始查询参数、原始死锁转储、MySQL 用户名/密码、凭据、环境变量、`LOCK_DATA` 值、患者资料、未列入 allowlist 的库表/进程信息、任意服务器文件路径。对参数、ID 输出只用固定上下文带盐/运行级哈希摘要，不输出原始值；数据库物理 `CONNECTION_ID` 可作为当次合成测试的关联整数，不跨环境复用。
- Snapshot 大小上限（建议每次规范化记录 64KiB，总 Artifact 1MiB，待下轮冻结）；只包含白名单字段，正向 JSON schema 验证；列裁剪应发生于读取驱动结果之后、持久化/日志之前，原始 `SHOW STATUS` 字符串永不得触达公共 logger/异常消息。
- 负控必须以包含标记口令、人工假手机号/患者名、SQL literal、`LOCK_DATA` 和非授权 Schema 的合成输入检验，确保解析器输出、JUnit XML、CI stdout/stderr、Artifact 都不出现明文。若脱敏失败，整个 CI 应失败，证据 Artifact 不应包含未清洗文件。
- Artifact 仅接受：原始 Surefire XML（若含敏感错误堆栈需受控/脱敏）、`f-isolated-incident.json`、`f-isolated-role-negative.json`、`f-isolated-verification.json`、SHA-256 manifest。需检查 `if: always()` 上传路径**不得通配临时原始转储**。
- 证据必须绑定 exact base/main、候选 PR HEAD、synthetic merge checkout second parent、GitHub run ID + attempt number、完整列出的 DDL Git blob、MySQL version、独立角色身份和实际采集时间。

## 6. 六文件建议范围（仅 Proposed，不是当前授权）

建议新 Draft 实现 PR **只新增**，与 #457 完全隔离的以下 6 个文件（本次仅评审，不冻结实施权限）：

1. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/forensics/StageBFDeadlockForensicsTest.java` — 两 Worker 原始 RACE05 行为 + 独立 Observer、第三 Reader。
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/forensics/StageBFObserverEvidence.java` — JDBC 物理绑定、正向归一化/脱敏输出与 typed verdict。
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/forensics/StageBFTestConfig.java` — 唯一 disposable MySQL8 profile、角色和失败关闭配置。
4. `diagnosis-service/src/test/resources/u07/d5o2/stageb/forensics/manifest.json` — Oracle、权限矩阵、DDL SHA、schema/index allowlist。
5. `tools/u07_d5_o2_tx_topology/stageb/forensics/bootstrap-and-verify.py` — 创建独立 Schema/账户、校验真实 grants、验证红线及证据合同。
6. `.github/workflows/u07-d5-o2-stage-b-forensics.yml` — 独立 Java8/MySQL8 runner，路径仅覆盖新的 forensics 资产。

**严禁编辑**：生产 `JdbcU06DeliveryStore`、Flyway、已有 P1-A/B/C 的测试/工具/CI、`pom.xml`、环境配置、外部消息/F8、患者数据。仅当目标源码已存在可复用的 Java8/JUnit/Spring/MySQL 依赖、无需更改旧文件时，该建议列表才可能进入正式冻结。若还需新增 `redactor`/专用状态机文件或额外依赖，须提交受控设计修订后再实施。

## 7. 评审问题与实施就绪结论

| Gate | 结论 | 下阶段必须取得的证据 |
|---|---|---|
| F-R05-01 双 Writer 与实际 1213 | `INPUT_CONFIRMED` | 不改变原始 Store 和 RACE05 的首轮结果 |
| F-R05-02 L0 表级只读权限 | `DESIGN_ACCEPTED_PHYSICAL_NEGATIVE_PENDING` | 真实 MySQL8 小版本上 SELECT/拒绝写权限矩阵 |
| F-R05-03 L1 全局 PROCESS 最小权限 | `CONDITIONAL_DESIGN_ONLY` | 独立批准 L1 + 纯一次性服务隔离 + 非授权动词拒绝 |
| F-R05-04 取证事件物理关联 | `CONTRACT_DEFINED_EVIDENCE_PENDING` | PROCESSLIST_ID/thread/transaction/locks/INDEX_NAME 与实际 1213 一致 |
| F-R05-05 脱敏安全 | `BLOCKER_UNTESTED` | 恶意合成负控和输出 allowlist fail-closed |
| F-R05-06 精确 CI 与文件范围 | `PROPOSED_6_ADD_ONLY` | 正式审查六文件足够、无现有文件和 Maven 修改 |
| F-R05-07 多轮真实重现 | `NOT_EXECUTED` | 有界所有轮次保留 + JSON/XML 原始证据 |
| R-WHOLE-TX-RETRY | `NOT_IN_THIS_UNIT` | 另案解决 rollback 权威、完整六表安全读取、全局时间预算 |
| 源码四表 JOIN 缺口 | `CONFIRMED_NOT_SAFE` | 单独生产源缺陷设计/授权 |

```text
F_ISOLATED_DETAILED_DESIGN = DOCUMENTED
L0_OBSERVER_POLICY = PROPOSED_MINIMAL_SELECT_ONLY
L1_PROCESS_POLICY = OPTIONAL_SEPARATELY_APPROVED_EPHEMERAL_ONLY
FORENSIC_EVENT_BINDING = FAIL_CLOSED_CONTRACT_DEFINED
RAW_STATUS_ARTIFACT_UPLOAD = FORBIDDEN
PROPOSED_IMPLEMENTATION_SCOPE = SIX_ADDED_FORENSICS_FILES
IMPLEMENTATION_READINESS = NOT_READY
IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
NEW_MYSQL8_FORENSIC_EVIDENCE = NOT_EXECUTED
PR457_FIRST_ATTEMPT_RACE05 = UNCHANGED_NOT_STABLY_PASSING
WHOLE_TX_RETRY = DESIGN_ONLY_NOT_AUTHORIZED
B2_PRODUCTION = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

## 8. 下一门禁

`D5-O2 Stage B — F-ISOLATED Six-File Scope / MySQL8 Observer L0-L1 Privilege & Redaction Adversarial Independent Design Review + Limited Test-Only Implementation Authorization Decision`。

正式实现前必须逐项冻结允许的 Observer 账号与权限、禁止的 SQL/副作用、脱敏负控、时间预算、事件关联门禁和六文件范围；若不能通过独立设计评审，就不得着手取证代码。允许受控失败或无图结果，绝不允许“没看到图却报告具体死锁索引”。设计/实施分离，不授予本文件任何合并或生产执行权。
