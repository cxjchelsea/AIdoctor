# U07 D5-P Targeted Design Review v0.1

日期：2026-10-10。对象：PR #375 exact HEAD 7151c95ccea201fad8a606a044552270fa11cae4，base #374 0924e08d22260212b202a1d2eb5f432e74b2d53a，单一设计文件，OPEN/DRAFT/unmerged。
本轮复读完整设计、当前 D3 coordinator / D4 runner / inspector 和 forced-exit worker。为同一助手批判性复审，不是独立人员签核；无实现、数据库执行、合并。

## 1. 结论

TARGETED_DESIGN_REVIEW = REQUIRES_TARGETED_REMEDIATION
D5P_SYNTHETIC_IMPLEMENTATION_READINESS = NOT_READY_UNTIL_FINDINGS_RESOLVED
BLOCKERS = 1
REQUIRED_FINDINGS = 3
REAL_D5 / CLINICAL / PRODUCTION / MERGE = NOT_AUTHORIZED

保留正确方向：原 D5 与 D5-P 范围分离；runner 独占提交职责；单语句 anchored recovery 覆盖 orphan/split；unknown commit 不因 rollback/负读回被认定失败；连接前全身份校验；固定 disposable target。
本轮不要求跨会话压力测试或 Oracle 执行才能修订设计，不扩大实现范围。

## 2. D5P-DR-B01：回滚失败没有可表达的结果（BLOCKER）

对象：第3节 owner 步骤4–5、第5节 transactionStatus。

状态只有 NOT_STARTED / ROLLED_BACK / COMMITTED / COMMIT_OUTCOME_UNKNOWN。路径：
getConnection 成功 → autoCommit=false → body 发生 SQL/业务异常 → rollback 抛异常 → close 也可能失败。没有进入 commit，不能冒充 COMMIT_OUTCOME_UNKNOWN；rollback 未确认成功，又不能标记 ROLLED_BACK；已经开始事务不能标记 NOT_STARTED。现有“尝试rollback并记录suppressed”不足以定义返回/抛出结果。

必须修订：
- 分开 commitAttempt / commitKnown / rollbackKnown / cleanup 事实，或添加明确的 ABORT_UNCONFIRMED 状态；只在 rollback 正常返回后记录 ROLLED_BACK。
- 定义连接获取/配置失败、开始前错误、body错误、rollback错误、commit错误和close错误各路径。
- 未知 abort 的连接不得再执行 SQL；关闭/丢弃失败单列。禁止自动写重试。
- 原错误为 primary，rollback/close 为 secondary，不能覆盖业务异常。
- 新状态不得推导已接受、已应用或真实授权。

验收：明确 transition/outcome 表；JDBC fault injection 覆盖 rollback-only failure、rollback+close failure、config failure；异常或返回结构保留事实，不错误降级成 ROLLED_BACK。

## 3. D5P-DR-R01：读取器公开接口及生产可达性未收口（REQUIRED）

第3节说公共边界仅 execute/inspectFresh，但只明确将 D3 coordinator 改 package-private。当前 U07D4SyntheticRecoveryInspector 是 public class + public inspect(Connection,... )；若原入口仍可调用，调用方可传入带自己未提交写入的连接，破坏“新连接只读快照”的前提。

还存在一句“架构测试限制生产入口只能调用 runner”，与其它章节禁止生产 synthetic 接线冲突。

必须明确：
- inspector class/Connection method 都内部化，外部只能 inspectFresh；内部无写入、无事务终结。
- production Spring/controller/非synthetic正式服务不得引用任何 synthetic runner/coordinator/inspector；不是允许生产改为只调用 runner。
- 对 public API、构造器、nested types、callback/test seam 制定可达性清单；不得暴露任意连接工厂、DataSource或SQL callback。
- 测试连接spy/fault seam只在受信任test scope，公开产品API不承载该能力。

验收：反射/public API扫描覆盖读取与写入两侧；生产引用检测；测试专用例外清单。

## 4. D5P-DR-R02：响应、清理与恢复错误仍混合（REQUIRED）

第5节 responseStatus 把 AVAILABLE / SUPPRESSED_AFTER_COMMIT / CLEANUP_FAILED 做互斥选项。但已知 commit 成功后隐藏响应再close失败可以同时发生。inspectFresh 的 readUnavailable、rollback/close失败、读到有效证据后清理失败也没有固定优先级。

必须选定一个具体合同，不留“返回/抛出均可”而不定义：
- transaction outcome、response availability、cleanup outcome、durable observation 分开。
- observation 有效仅表示该次SELECT完成且分类完成；读失败不是 absent或NO_DURABLE_EVENT；分类成功后close失败不得抹掉已读证据或虚构业务结果。
- 定义执行/恢复分别返回何种typed result或带完整上下文exception；是否对invoke error继续throw，调用者如何取事实必须唯一。
- 状态与当前Outcome绑定规则：COMMITTED才可携带已提交 ACCEPTED/replay；ROLLBACK currentness 可携带 rejection；UNKNOWN不可返回普通成功。
- observedAt是客户端观测时间，不是数据库SCN/提交版本，也不保证更新后的最新状态。

验收：commit-success+suppression+close-failure、read-success+cleanup-failure、read-failure+cleanup-failure结果表及测试项。

## 5. D5P-DR-R03：强制退出与测试调用迁移缺具体方案（REQUIRED）

列了 ForcedProcessExitWorker 迁移，但当前pre模式直接Connection写两表再halt，post模式直接调用D3持有提交入口。只把post改成runner不保证保留“提交前已写两行但未提交时 abrupt exit”的同等证据。public API又禁止任意callback，不能临时暴露connection来打断。

必须明确：
- pre crash 用测试fixture worker直接构造未提交双行事务（显式test-only例外），或在runner加入命名且固定的test fault点；两种只选一种，不能公开任意callback。
- post crash通过owner成功commit之后halt，parent进程fresh read验证两行。
- fixture例外不计入“所有代码只有runner可创建连接”的全局断言；准确区分产品执行入口与故障fixture。
- D3/D4 workflow的固定target配对、旧args适配、invalid入口0连接测试spy方案和新文件编译清单明确列出。

验收：具体调用迁移表；保持原pre/post故障覆盖，明确哪些测试不走公共runner，不夸大唯一owner证据。

## 6. 已接受的设计点与非阻塞备注

- 单语句 anchored LEFT JOIN 形状能保留无canonical的application行，两个canonical候选可判split；plain SELECT保持同次读取所有字段。实际SQL仍需方言编译和数据库测试。
- 分类顺序建议固定：canonical split/identity mismatch → application identity mismatch → presence partial → phase/decision/revision/effect。特别是orphan身份不匹配与非NULL effect要明确类别。现规则都fail-closed，不新增本轮阻塞，但实施前应在结果表中固定。
- synthetic前缀/ASCII校验不是认证；固定容器profile是测试隔离条件。不能在真实D5复用该校验作为医学授权。
- commit-entered错误保留unknown；fresh NO_DURABLE_EVENT不得触发自动写入。此方向正确。
- API删改不影响生产代码的前提仍需调用扫描，不凭命名推断。
- Oracle、Foundation JPA事务桥接、真实F8/APPLIED、跨会话唯一竞争继续排除。

## 7. 下一步

仅修订 PR #375 设计中的 B01、R01、R02、R03，生成明确状态矩阵、公共/内部API清单和test migration表；随后 targeted re-review。无需重新设计整套架构，无需接真实数据库。
修订通过前不开始D5-P实现；本报告不授予任何合并或真实D5实施权限。
