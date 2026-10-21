# U07 D5 — Isolated F8 Adapter Implementation Readiness + Checklist v0.1

## 1. 决策与固定输入

结论：**READY_FOR_BOUNDED_ISOLATED_IMPLEMENTATION**。表示已有足够设计及原型证据，能够开始下面限定范围的实现；不表示adapter已经通过验证、真实owner等价、真实D5就绪或允许合并。

固定main `835441492d130de320bf3d49fe2b4be598a2b327`；Source/Binding已在main。#394修订设计 `a941815cfe325bacb32089c6a106a221b273969e`；#397修订原型 `0ccfe62dc6d53aec929f06bad1cba2e5f6fba927`；#399复审 `81c3c199b7d15f860bce800c8c9057495754b3f0`。这些后续PR尚未合并，不能称main已含F8。

#397真实Java8/MySQL8.0.46证据：run38030165467/job114149234025，34例零failure/error/skip；仅证明聚合synthetic事实模型、SQL时钟/guard/原子性及两项修订。未重跑Source25例。本轮仅评估和实施清单，无新代码或测试执行。同一助手评估，不称独立审查。

## 2. 已有条件与必须实现的缺口

| 领域 | 可复用输入 | 下一实现的验收条件 |
| --- | --- | --- |
| Source/canonical | 21字段codec、Foundation JPA ledger、original source/binding校验 | 同F8外层事务只读验证；不直接调用admit/read；25例不退化 |
| SQL政策 | 已修复P1–P8原型和34例 | 从真实synthetic owner行读取事实；不接收Ei/authority_complete caller boolean |
| 时间 | UTC_TIMESTAMP(6)语句采样与固定boundary区分 | 最终政策语句内同clock；SQL与存储列一致；deadline equality明确 |
| 共同事务 | Source真实JpaTransactionManager配置，原型JDBC guard | 新adapter中JPA/JDBC/clock/owner同connection；外层已有事务及错resource拒绝 |
| 安全入口 | PROFILE-B及scope基础 | historical-read与new-finalize分离许可；锁内版本/许可再检查 |
| 恢复 | #394 §11合同 | verdict-specific完整reader；未知commit不因ABSENT升格失败 |
| 结果 | #394 §10矩阵 | 全部合法/非法组合validator，不再以string模拟返回结果 |
| owner/APPLIED | 仅聚合fixture/root证据 | 独立fixture issuer、consumer SELECT-only、共享guard的owner写路径 |

## 3. 基线与文件清单

下一实现单独分支/草稿PR，以#397固定HEAD为父提交；它只在main上新增6个原型文件，不继承历史54文件栈或#398/#399文档分支。保留34例及修订SQL作为回归输入。合并前另整理最新main目标及精确差异，不能现在自动合并#397。

F8工具在 `tools/u07_d5_f8` 扩展。为访问package-private Source类，新F8代码使用 `com.aidoctor.verification.sourcebinding` 包；目录按包名布置，仅工具模块编译，没有生产扫描/Bean/HTTP入口。现有SqlClockTest保留原包。

| 文件/目录 | 修改意图 |
| --- | --- |
| Source模块 `SourceBindingAdapter.java` | 最小委托重构，公开owned-entry行为与结果语义不变 |
| Source模块新增 `SourceBindingVerifier.java` | 提取source/checkBinding/target共同只读验证，不开/提交事务；immutable VerifiedCanonical |
| F8模块pom.xml | build-helper引用Source源码及Foundation CanonicalBusinessEvent*.java；精确compiler includes，不复制codec/ledger/验证算法 |
| F8 main包 `F8Adapter.java` | owned outer transaction、Tier0/new finalize、attempt/commit receipt及异常映射 |
| F8 main包 `F8Result.java` | typed operational/attempt/durability/reason和构造validator |
| F8 main包 `F8HistoricalReader.java` | 同事务完整readback；独立fresh read/reconciliation入口 |
| F8 main包 `F8OwnerReader.java` | 专用fixture owner记录验证、确定锁次序及version映射 |
| F8 resources `adapter-finalize.sql` | 原型政策移植至完整fixture事实、scope与版本谓词，非无条件INSERT |
| F8 test resources `schema-adapter-mysql.sql` | 独立schema、复合claim身份、FINAL decision约束、权限，无Flyway生产migration |
| F8 tests `F8AdapterTest.java`、`F8TestAuthority.java` | 独立issuer/consumer、JPA共同资源、故障/恢复/并发oracles |
| F8 workflow | 保留34原型例；新增adapter真实MySQL及Source25回归、golden、报告artifact |
| 开发单元新实施证据文档 | 记录精确HEAD、文件差异、实际结果及未验证边界 |

此表为预期修改面，实际提交须给出精确inventory，超出范围重新审查。Foundation实体/生产service、迁移脚本、P01/U15/U06 owner代码不修改。

## 4. Source verifier与事务归属合同

Source验证不得直接调用SourceBindingAdapter.admit/read，因为ownedEntry拒绝active outer；不得改REQUIRES_NEW绕开，也不得把Source结果COMMITTED用作F8提交证明。

提取verifier保持Source/Binding各项字节、digest、cipher、original issuer、scope/target关联校验；Source adapter委托后保持既有admission epoch、alias timestamp、late winner、1062 fresh-read行为。VerifiedCanonical只表示当前事务中校验事实，不携带F8 committed ID/结果。用于F8的对象不可由caller自造；identity由已验证canonical确定。历史read不依赖当前SYNTHETIC_ADMISSION grant，但始终要求当前historical-read授权；new finalize另要求F8_FINALIZE action/epoch。原SOURCE issuance不能代替新action许可。

一个DataSource、EntityManagerFactory及JpaTransactionManager。F8外层SERIALIZABLE；入口检测已有事务；内部verifier要求active/joined/bound/non-autocommit并验证JPA与JDBC connection ID。检测放在adapter实际路径中，不能只写测试helper。提前验证无副作用，锁内重新读proof/version；任何成功只在TransactionTemplate正常返回后发布。

## 5. 独立fixture owner与数据库合同

新增独立issuance/delivery、Question/Gap、Pending、Consultation/current-wait/terminal/deadline、action epoch及APPLIED evidence行。每组保存synthetic issuer/policy/manifest、scope、version、ref关系；APPLIED必须引用原ACCEPTED decision+claim+generation+digest，consumer不能写APPLIED。事实缺失/未知为DEFER，可信mismatch/terminal为负向事实。聚合authority_complete不作为adapter最终真值来源。

consumer SELECT canonical/binding/source/owner，INSERT FINAL decision，INSERT/CAS claim；guard只给独立lock_token必要权限。不能给owner/version写权限为FOR UPDATE兜底。若现有SELECT-only owner锁不满足MySQL权限，使用共享guard协议及受版本谓词保护的owner读；不得悄悄扩权。issuer执行实际synthetic owner变更同样先取得guard。

锁次序：Consultation guard → action/scope permission → deterministic owner rows → exact wait claim → exact decision。所有fixture owner writer同序；claim缺行由guard覆盖。生产writer加入协议仍未证明。

claim主键完整(env/profile/tenant/consultation/question/parent_wait)，decision按canonical USER_ANSWER+F8 contract稳定hash，UNIQUE canonical+contract。实际raw claim先按完整身份识别，关系scope/structure不得JOIN隐藏成ABSENT。所有精确字符串/digest采用binary comparison或ascii_bin，不能继承原型默认collation作为身份等价证明。

复用Foundation和Source的DDL，不复制另写canonical/binding字段。现有Source grants含固定schema名，adapter bootstrap只允许显式固定目标schema重映射并记录输入checksum，不能传任意数据库。Source原suite继续在原固定URL/schema运行；adapter单独URL/schema，两者独立。

## 6. 行为与退出清单

- USER_ANSWER先解析已验证canonical；RESUME_REQUEST只解析已验证target USER_ANSWER，target缺失/错scope不新建decision，不以resume canonical作为F8 identity。
- Tier0完整historical decision读回，不重新按新deadline裁决；不返回当前effect grant。没有decision才进入当前finalize许可和owner政策。
- P1..P8沿用#394修订次序；current-wait cleared/moved及既有winner不是公共合法性过滤条件。SQL NULL明确二值化，证据不确定不能默认为false跳到低分支。
- 仅P7候选创建provisional sentinel；最终P2导致整个attempt回滚；后续同identity负向重试不创建sentinel。决策+own claim CAS同commit，不发布precommit成功。
- 第一次持久DML调用前ATTEMPTED；零行仍ATTEMPTED。确认rollback才NOT_COMMITTED；只读DEFER保持NOT_ATTEMPTED；提交/回滚异常未知保持UNKNOWN，confirmed fields为空。
- 1062结束failed transaction后fresh reader。失败attempt结果与fresh historical read分开；ABSENT不证明UNKNOWN失败，也不自动重派winner。
- ACCEPTED需完整own claim；DUPLICATE需原APPLIED winner链，无own claim正常；EXPIRED/REJECTED无own claim正常；被claim引用的非ACCEPTED、半填/非法generation隔离不repair。

## 7. 必须执行的验收矩阵

| 组 | 最小真实执行要求 |
| --- | --- |
| V01 基线 | 原34原型例+Source25例+独立golden全通过；不是宣称本轮已执行 |
| V02 接入 | canonical/source/cipher/original-proof/target每类损坏fail closed；alias身份稳定 |
| V03 事务 | existing outer、错误manager/DS、JPA未joined、autocommit、connection不等真实adapter拒绝 |
| V04 许可 | historical允许/new finalize撤销分开；锁内epoch撤销；跨scope无decision |
| V05 政策 | 完整owner P1–P8；NULL/moved/matching pending、错scope相关claim及缺高优先证据 |
| V06 时钟 | 真实语句time推进、固定equality、session timezone变化；fixture证据与真实clock分别报告 |
| V07 原子性 | sentinel/decision/CAS前后故障、零行、新sentinel跨期、无空sentinel提交 |
| V08 并发 | 同答案同identity、不同答案同wait、owner/F8两个顺序；一个winner，未应用accepted不duplicate |
| V09 读回 | 四verdict正常与各损坏组合、合法旧sentinel、UNKNOWN fresh read FOUND/ABSENT/UNAVAILABLE |
| V10 结果 | §10每类退出与非法字段组合；数据库异常、确定rollback、注入commit/rollback未知区分 |
| V11 权限/副作用 | consumer写owner/APPLIED/clinical/outbox拒绝；零临床/P01/effect/dispatch修改 |

注入commit-unknown/rollback-unknown仅证明代码映射，不能声称真实网络故障、进程crash、Oracle或生产owner认证。测试数量由实际case决定，不能把11组当11项完成证明。任何fail/未执行必须进入证据缺口。

## 8. 交付门禁与范围

下一步可直接按清单做隔离adapter实现；遇到不可保持Source行为、无法证明owner/action合同或共同事务时报告具体blocker，不用聚合布尔/扩权限掩盖。完成后先定向实现/证据复审，再有限关闭新增隔离验证问题及评估集成；不自动合并。

当前adapter代码及上述矩阵尚未完成，因此 **ISOLATED_ADAPTER_VERIFICATION=PENDING；REAL_D5=NOT_READY**。真实Foundation audit/U06 issuance/U15共享锁/APPLIED/P01 owner CA保持开放。该实施范围不包括生产路由、真实数据、APPLY clinical patch、Oracle、部署或merge。
