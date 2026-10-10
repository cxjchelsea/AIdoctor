# U07 D5 Authorized Application — 接入边界与实现就绪评估 v0.1

日期：2026-10-10。评估基线为 #381 HEAD `2585d3aa232f8eff2f169a0f7095d9051406140e`；不是 main 集成结果。
本轮只增加本文件，不修改冻结合同、生产接线、迁移或临床状态。

## 1. 决定及依据

REAL_D5_IMPLEMENTATION_READINESS = NOT_READY
NEXT_ISOLATED_TRANSACTION_BRIDGE_DESIGN = READY_TO_START
REAL_SOURCE / P01_G2_APPLY / P02_U02_DISPATCH / CLINICAL / PRODUCTION / MERGE = NOT_AUTHORIZED

D5-P RR04/05/06 已在 #379+#381 分支的合成 MySQL 范围有限关闭；#381 评论 6092498978 记录精确提交、原始 D3/D4 CI、代码树一致性及同一助手复核声明。五项 fatal 清理场景、204 项 boundary assertions 并不证明真实应用、Spring/JPA 事务或真实 consumer receipt。

原始定义取自 #360 `20bd356f80b9ce6d80acdb6644f7f1ac4b75268a` 的
`U07_Durable_Event_Application_Transaction_Design_and_Readiness_v0.1.md`：D5 = P01/G2 + real source readback + accepted F8 + outbox/consumer receipt + target runtime binding。该文件在本分支未发现，本次从 #360 文件 diff 完整读取；其锁序只是 proposal，不自动成为已验证合同。

## 2. 当前实物与缺口

以下结论来自基线源码，不把“有接口/有字段”等同于可用真实能力。

| D5 依赖 | 已读取实物 | 结论及需要补齐的证据 |
| --- | --- | --- |
| Canonical identity | runtime/foundation/CanonicalBusinessEventLedger.java；resolveOrCreate 使用 @Transactional、repository.save，并捕获唯一约束异常后同事务读取 | owner 可复用；save 的实际 flush/commit 异常窗口、rollback-only、新事务 winner readback 与 U07 一致提交未实测 |
| current wait / question | u06/wait/ConsultationWaitTransitionService.java；锁 consultation、saveAndFlush、SERIALIZABLE | 有候选状态 owner；代理实际生效与 U07 同事务/一致锁序仍待证明 |
| source | state/committer/ports/SourceValidationPort.java；U06SyntheticP01Runtime 内部 Source 仅比较 RULE_DERIVED | 是来源类型授权接口，不是事件来源真实性或版本 readback；不能作为真实 issuer 认证 |
| F8 decision | D3 coordinator 持久化合成 ACCEPTED，历史 replay 校验明确 | 本分支 runtime/u07 不提供真实 authority-bound F8 调用链；须绑定权威合同版本及 decision/trace，不用 D3 ACCEPTED 代替 |
| P01/G2 | StateCommitter + U06SyntheticP01Runtime 注入 Backend；该 runtime 内部 Idem 为 map，source/cap/consent 为固定策略 | 是复用线索；未证明真实 U07 effect 获授权、被授权路径集合或数据库内原子应用 |
| effect/outbox | U07EventApplicationRepository.bindSyntheticEffect；U07EffectOutboxRepository.insertSyntheticPending | 明确不标 APPLIED、仅 SYNTHETIC_TEST_ONLY、不派发；没有真实 consumer 协议或权威回执闭环 |
| persistence | MySQL V7 包含 source refs、receipt_ref、effect_id、effect_fingerprint、owner/lease 等字段 | 字段存在不证明身份可信、lease fencing 或 receipt 有效；本轮不改 V7 |
| target binding | U07SyntheticTestTarget 固定 disposable MySQL D3/D4 URL | 是测试隔离，不是 runtime/consumer identity、部署 binding 或真实 endpoint |
| deployment / DB parity | D3/D4 isolated Java/JDBC CI 已验证 | 非完整 diagnosis-service Maven/Spring 集成、真实迁移发布历史或 Oracle 实测 |

证据链必须保持：identity RECEIVED、F8 ACCEPTED、本地业务状态已提交、外部命令已消费、P02 resumed、U02 delivered 是不同事实。checkpoint 缺失不得改写历史 business ACCEPTED；仅有 outbox 行不得宣称外部完成。

## 3. D5 接入边界 proposal

本节用于下一段详细设计，不冻结新 API、不授予 effect 写入权限。

| 边界 | 可信输入的产生方 | 最低匹配字段与拒绝规则 |
| --- | --- | --- |
| ingress/source | 经认证 ingress + source resolver；请求只携带待解析引用 | issuer/scope/event/type/digest/source-version；缺失、错域、撤销或版本不符拒绝；trusted/current/applied 不接受客户端指定 |
| F8 | 正式业务 decision owner，合同版本从权威工件确定 | canonical event、consultation/question/wait、current state version、policy ref、decision ref/trace；旧 ACCEPTED 不自动授权当前 APPLY |
| P01/G2 | 被批准的状态 mutation owner | event/effect/fingerprint、approved paths、baseVersion、authority ref；禁止 U07 直接解释 Clinical Fact；CAS 失败重新 reconciliation |
| local apply receipt | 同一事务的真实状态 owner | effect 与 event/digest/scope、提交后版本、receipt ref；只有本地提交证据满足正式 APPLIED 定义才可推进该状态 |
| downstream receipt | 指定 consumer 的持久化幂等记录与可查询回执 | effect/target/binding/fingerprint/receipt identity；发送成功或 caller 回执字符串不等于消费成功 |
| runtime compatibility | 指定 runtime owner | runtime/checkpoint/version/target binding；失败独立于 F8 business verdict |

SourceValidationPort 单个字符串输入不足以承担上述 ingress provenance；不要简单扩大字符串前缀校验或把它命名为真实来源认证。先确定真实 source resolver 与冻结 provenance 合同，再决定适配接口。

APPLIED 的本地状态范围与外部完成范围必须从权威 Unit Spec/RDP 精确读回；本 proposal 不新增 phase、不暗中修改状态定义。外部 effect 的结果未知时禁止创建新 effect 重发；按稳定 effect identity 查询权威 receipt。

## 4. 下一段最小可实施工作：事务桥接详细设计 + 隔离实测

优先解决一个可客观验证的断点：Foundation JPA 与 U07 JDBC 是否由同一事务 owner 管理。它不需要患者数据、真实 ingress 或外部 consumer。

建议以现有 Spring/JPA Foundation 为候选 owner，使用同一 DataSource 下的事务绑定 JDBC 访问；最终方式须由详细设计和真实 Spring 集成证据决定。不能把 D5-P DriverManager runner 嵌进 @Transactional 后称为同事务，也不能复制第二个 canonical owner。

先定义测试专用配置：disposable MySQL、最小 Spring bean set、隔离凭据、无应用启动/定时任务/真实 endpoint；测试 harness 是唯一接线入口。必须确认 transaction manager、EntityManager、JDBC Connection 绑定及真实 proxy 生效；同 URL 不是同事务的证据。

| 验证用例 | 可接受证据 |
| --- | --- |
| JPA insert + JDBC application insert 后抛错 | 两表均无 durable 写入；无独立 JDBC commit |
| JDBC 先写 + JPA flush 失败 | 两表均回滚，异常不被正常结果掩盖 |
| 成功 commit / response 丢失 | 新连接读取两表匹配；原 UNKNOWN 保持 UNKNOWN，观测另存 |
| 相同 canonical unique key 并发 | 一个 winner；失败事务退出后在新事务读取，不能在 rollback-only transaction 宣称成功 |
| 两条不同 consultation 路径争用同 canonical key | 覆盖既有 same-consultation 测试以外的竞争，区分允许冲突与未知 SQL 错误 |
| U06 与候选 U07 锁序 | 枚举所有路径，检查逆序；真实 DB 运行带超时的竞争用例，死锁/重试策略不能推测 |
| configuration fail / no proxy / wrong datasource | fail-closed，禁止伪造事务原子性 PASS |
| 干净 CI build | diagnosis-service 实际 Maven/Spring test；若全模块受既有依赖阻断，先记录精确阻断，再决定最小 harness，不以 javac smoke 代替 |

#360 的 canonical-first proposal 与 D3 consultation-first 实现不同。下一段先设计实际 JPA/U06/U07 统一锁序，并以测试冻结；本评估不选择未经验证的某一种作为生产规则。唯一键错误恢复不得在已经失败的事务里继续接受新业务写入。

## 5. 后续顺序与进入条件

1. 事务桥接详细设计并落地最小隔离 Spring/MySQL 测试；仅修复实测暴露的事务缺口，保持真实 runtime 入口禁用。
2. 明确 source/F8/P01-G2 authority 的实际 owner、版本、readback API、scope 与 target identity。读取权威 Unit Spec/RDP 后形成一份具体接入合同；缺失的环境/owner 信息到此才需用户补充。
3. 设计并验证本地 apply intent / receipt 同事务及稳定 effect identity。外部 consumer dispatch 与收据协议另列范围，不与本地 commit 混称原子。
4. 真实 D5 实施前完成目标环境绑定与明确授权；临床、PHI、真实 P02/U02、Oracle 和部署均按实际范围单独验证。

本轮没有执行数据库迁移、Maven/Spring 或 Oracle 测试，也没有合并任何 PR。该评估可开始第 1 步详细设计；真实 D5 仍 NOT_READY。无需再重复审查已有限关闭的三项，除非下一段改动触及它们的行为。
