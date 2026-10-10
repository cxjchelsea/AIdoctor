# U07 D5-F8 针对性设计审查 v0.1

日期：2026-10-10。受审 #394 HEAD 12936fbf27cd632194cd3f97aa08457c28ced547；base/main 835441492d130de320bf3d49fe2b4be598a2b327。
结论：REVISE_REQUIRED；1 BLOCKER、1 REQUIRED（含两个相关恢复合同缺口）。
本轮同一助手重新读取精确HEAD全文及上轮采用的RDP-02/接入合同约束，非独立人员签署，无新测试执行。未修订原设计、未实施或合并。

## Finding

### DF8-B01 — 最终SQL的全局predicate与P1/P2/P3/P4/P6冲突（BLOCKER）

位置：#394 §4政策及§5：“重新predicate ... Consultation版本/current wait ... wait及claim generation”；“final decision语句同时校验winner空/代次”。

设计同时要求P1同wait已APPLIED同内容优先DUPLICATE，且可在expiry/cancel、Question已结束后成立。合法P1已存在另一ACCEPTED winner，当前Consultation也可能早已不指向该wait。若winner空及current wait相等是最终INSERT共同WHERE条件，P1分支永远被过滤，CASE无法挽救。类似地，可信P3取消/终止、P4当前wait不匹配也可能正是要持久REJECTED的原因；要求它们全部匹配会将合法终态过滤为零行。该问题不是数据库尚未执行导致的证据缺口，而是现有设计文本内部矛盾。

修订要求：
- 明确所有verdict共有的authority安全条件：认证scope、canonical/source完整性、锁域/clock有效、读取版本未变等。
- 将ACCEPTED业务适用条件（current wait合法、无terminal、winner空等）限定于P7；不能成为全部verdict共享的WHERE。
- 对P1定义原已应用winner/decision/claim及同内容同wait证据的predicate；允许当前wait已合法移动，禁止新claim。
- 对P2/P3/P4/P6分别定义可持久decision的可信负向事实，和事实不可认证时的DEFER；不同Consultation错授权仍在入口阻断，不能为跨scope创建decision。
- 最终CASE/过滤表逐项描述P1..P8命中、零行和generation来源；原owner行缺失不能用SQL inner join静默代替政策判断。
- 增加明确oracle：已有claim且current wait移动后的同内容P1；取消后current wait清空；可信current-wait mismatch；并验证仍无新winner/下游effect。

不要求放宽authority验证，只要求安全资格与业务命中条件分离。若实现者本来打算分支predicate，必须写入合同而非依赖推测。

### DF8-R01 — 零行、attempt及verdict-specific一致性恢复不封闭（REQUIRED）

位置：§4 sentinel、§5 DEFER过滤零行、§6结果组合/“claim有winner但decision缺失或反之”。

两个缺口：
1. 首次final INSERT已执行但零行，或锁内发现pending winner而退出，不能沿唯一列出的DEFER+NOT_ATTEMPTED报告为“未尝试”。§6没有穷举这种路径是只读precheck拒绝、尝试后确定回滚，还是需fresh read；“RETRYABLE_FAILURE等”也不足以定义可编码结果。若sentinel创建已执行则更不能自动NOT_ATTEMPTED。
2. decision缺claim并非一律损坏：EXPIRED/REJECTED没有自己的claim，DUPLICATE关联别人的已应用winner；空sentinel也没有decision。§6“或反之”必须明确仅指ACCEPTED-own-claim。非ACCEPTED“无新claim”还须区分不生成winner与是否允许提交空sentinel，避免恢复reader将合法状态判INCONSISTENT。

修订要求：
- 固定attempt的计数边界，并按历史读、只读DEFER、sentinel尝试、零行finalizer、成功、确定回滚、commit unknown列出完整合法结果。
- 明确零行后的外层回滚/提交纪律，不盲重试、不伪造business verdict；fresh winner作为独立HistoricalDecisionRead，不写入失败attempt confirmed字段。
- 固定sentinel创建时点及是否允许无decision持久化；选择rollback清除或合法空sentinel之一并测试。
- 分verdict校验：ACCEPTED↔own winner claim；DUPLICATE↔original已应用winner证据，无own claim；EXPIRED/REJECTED无own claim；合法sentinel与腐化winner分别处理。
- 增加合法/非法组合表和oracle，包括历史拒绝decision无claim正常、合法空sentinel、ACCEPTED缺claim损坏、DUPLICATE引用错误winner拒绝。

## 已成立设计要点

| 方面 | 本轮判断 |
| --- | --- |
| 外层事务与Source verifier复用 | 顶层owned entry、private reader抽取需Source25例回归；不调用嵌套admit/read的方向正确 |
| 共同锁与scope权限 | Consultation优先、synthetic所有相关写owner参与锁序；真实U15 CA未被冒充关闭 |
| 逻辑时间与物理commit | statement before deadline/commit after deadline保持历史ACCEPTED；以后effect另行fence，符合既有V1 |
| decision/claim两语句 | 同物理事务、同共同锁、claimCAS失败全部回滚在设计上可以成立；必须实测，非本轮通过证明 |
| Tier0与权限 | historical-read action与新finalize/effect权限区分；历史ACCEPTED不授新effect |
| UNKNOWN | 无confirmed字段、同identity新事务读、单次ABSENT不盖写未知结果；原则正确 |
| 测试边界 | fixed-clock oracle与真实MySQLclock证据区分；不冒称Oracle/crash/真实owner已验证 |

不把MySQL候选clock表达式或16组未来测试当已执行。具体schema/conditional SQL和权限等价仍是后续prototype门禁。设计审查通过也只能进入隔离prototype范围，不自动授权真实F8实施。

## 裁决与下一步

D5_F8_TARGETED_DESIGN_REVIEW = REVISE_REQUIRED
DF8-B01 = OPEN；DF8-R01 = OPEN
ISOLATED_F8_IMPLEMENTATION_READINESS = NOT_READY
REAL_D5 = NOT_READY
MERGE = NOT_AUTHORIZED / NOT_PERFORMED

下一步只修订 #394 的最终statement分支predicate及结果/恢复矩阵，补独立负例oracle；随后针对新精确HEAD复审。原F8优先级、21字段Source binding及真实owner CA保持不变。无需在修订前接入真实数据库/用户凭据、临床写入或dispatch。
