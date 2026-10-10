# U07 D5 F8 Adapter 实现与证据定向复审 v0.1

日期：2026-10-10。受审 PR #401 HEAD `298c0d28ccb98037365a9440ebeeefc1cdd12647`；base `0ccfe62dc6d53aec929f06bad1cba2e5f6fba927`。本轮为同一助手重新检查并新增反例验证，非独立人员批准。

## 1. 结论

`TARGETED_IMPLEMENTATION_EVIDENCE_REVIEW=REVISE_REQUIRED`。

原有 111 项通过的证据真实，但不覆盖本轮三个反例。新增一个 blocker、一个 required finding；隔离验证收口与集成就绪评估暂不进入。PR #401 保持 draft/未合并；REAL_D5=NOT_READY，真实 owner CA 均不关闭。

## 2. 精确检查及证据

核验远程 PR HEAD、base、15 文件范围和最终两套 CI，重读 #394 正文 v0.2、#396 设计复审及 #400 清单。检查 Source 校验提取、resource guard、action epoch、四类 owner、P1–P8 SQL、时钟、claim/APPLIED/readback、durability、测试和 grants。

| 对象 | 精确执行结果 |
| --- | --- |
| 原 #401 F8 run [38032095157](https://github.com/cxjchelsea/AIdoctor/actions/runs/38032095157)，job 114154991841 | adapter52 + 原型34=86，0 failure/error/skip，success |
| 原 #401 Source run [38032095099](https://github.com/cxjchelsea/AIdoctor/actions/runs/38032095099)，job 114154991658 | Source25，0 failure/error/skip，success |
| 审查反例 HEAD `8badd0ac13999d2af4badc4964294be6ed698d1c`，基于受审 HEAD | 仅 F8AdapterTest 新增三个 oracle；main Java/SQL/schema 均不改 |
| 反例 F8 run [38032370107](https://github.com/cxjchelsea/AIdoctor/actions/runs/38032370107)，job 114155812746 | adapter55 中3 failure、0 error/skip；原型34全绿；合计89项、3 failure，BUILD FAILURE。三项新反例均失败，原52项adapter未新增失败。 |

本机 golden check 与 diff whitespace check 成功。新增反例在实际 Java8/Spring/MySQL8 workflow 编译执行；没有冒充本机 Maven 执行或真实网络/crash测试。review PR 为审查证据，不是修复或可合并实现候选。

## 3. F8A-B01：rollback-only 正常返回仍发布 COMMITTED（blocker）

位置：F8Adapter.finalizeAnswer 成功返回路径（受审文件第44、62–66行）。beforeCommit/afterCompletion 已跟踪，但 tx.execute 正常返回后直接发布 result，没有核验 completion。

真实反例：JpaTransactionManager 子类仅捕获 Spring 的 DefaultTransactionStatus；在既有 before_commit probe 调用 current.setRollbackOnly()，不改 adapter 正式实现。Spring 对 local rollback-only 执行 rollback，并可正常返回 callback 结果。测试首先断言 decision=0、claim=0，随后要求 NOT_COMMITTED、confirmed=null。

实际：decision/claim 两项零行断言通过；durability 断言失败，预期 NOT_COMMITTED，实际 COMMITTED。实现正常返回的 result 同时携带非空 confirmed（代码路径审查），测试在 durability 首个失败处停止，未声称后续 assertNull 已执行。

这是事务完成语义缺口，不是未知提交的保守退出：本例完成回调已明确 ROLLED_BACK，但仍将事务内候选当作成功收据。违反 #394 §10 与 #400 V07/V10。原异常测试全绿不能证明正常 rollback-only 分支。

必要修复：写成功候选只在明确 STATUS_COMMITTED 后公开；明确 STATUS_ROLLED_BACK 返回 ATTEMPTED/NOT_COMMITTED、confirmed=null；其它完成状态保留 UNKNOWN。不能仅依赖 TransactionTemplate 正常返回；同时保留现有 commit-exception UNKNOWN 保护。增加 local rollback-only、flush/global rollback-only 及已有异常回归，历史只读退出不能误称本次写入。

## 4. F8A-R01：非 DUPLICATE 的 winner_id 未交叉校验（required）

位置：F8HistoricalReader.read（受审第35–44行）及 F8Result.Receipt。read 只在 DUPLICATE 分支校验 winner_id；ACCEPTED/EXPIRED/REJECTED 最终将存储值原样放进 confirmed receipt。

两个真实反例在 root 显式 corruption fixture 下，将合法 ACCEPTED 或无 claim 的 EXPIRED decision 的 winner_id 改为不存在的64字符hash。其它 scope/source/fingerprint/claim关系保持原样；期望 INCONSISTENT，不改历史 verdict、不补造或修复任何记录。

实际：两例均预期 INCONSISTENT，实际 FOUND_MATCH。SQL允许 fixture 修改该字段，reader 未隔离 forged winner。

当前 SQL 的 ACCEPTED 必无另一 winner；negative 的非空 winner 是观察到的原 ACCEPTED winner引用，而不是任意字符串。完整历史读回不能确认该字段损坏的收据。此问题不否定正常 negative 没有 own claim；也不要求 negative 的 null winner 永远对应当前 claim 缺失。

必要修复：ACCEPTED 拒绝不允许的 winner_id；negative 的非空引用验证合法原 winner/claim、scope/wait/source关系，null 引用保持正常历史含义；DUPLICATE 保留完整 APPLIED 原链。构造 validator 与 reader 分别覆盖字段合法性及数据库关联，禁止依赖仅有非空检查；增加 ACCEPTED/EXPIRED/REJECTED 与合法另一 winner 的正负对照。

## 5. 通过的有限检查与证据限制

Source source/checkBinding/target 提取保留原校验体，Source25 与 golden 均绿；owned transaction 的错误 manager/DS、unjoined、实际 autocommit、真实不同 connection ID 被拒绝。权限 read/finalize 分离及 guard 前 epoch 撤销被实际测试；SQL 支持可信 moved/cleared/terminal/expiry 的优先负向分支，P5/P8 不落第五 durable verdict。新 sentinel 跨期与多处异常故障回滚、两类答案并发、owner 两顺序、原 APPLIED Source proof 损坏和最小 owner权限已有证据。

这属于有限通过检查，不抵消两项 finding。实际 connection mismatch 用例代理 joined 报告但使用真实第二连接；commit/rollback lost acknowledgement 是 manager 注入。无 Oracle、真实网络/crash、outbox 表写拒绝或生产 owner 共同锁认证。Source既有1062例不是新增F8实际1062错误码注入证据。

## 6. 下一步

仅修复 F8A-B01/F8A-R01 与对应回归，重新跑 adapter、原型34、Source25、golden，记录新精确 HEAD，再做定向复审。review反例保留失败历史，不把它们改成接受现状的断言。未授权任何 merge，未关闭真实 Foundation/U06/U15/APPLIED/P01 CA。
