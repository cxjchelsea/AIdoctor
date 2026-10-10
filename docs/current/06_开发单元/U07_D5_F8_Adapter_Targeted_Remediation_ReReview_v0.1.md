# U07 D5 F8 Adapter 定向修复、回归与复审 v0.1

日期：2026-10-10。原受审 #401 HEAD `298c0d28ccb98037365a9440ebeeefc1cdd12647`；问题来源 #402 审查 HEAD `78f13972f366c4eb045fabe4cd8b1196e79a198b`，反例执行 HEAD `8badd0ac13999d2af4badc4964294be6ed698d1c`。

修复及本次复审精确 #401 HEAD：`5edc0ee3599bd8993c9452c418b8f7edb343972d`，base仍为 `0ccfe62dc6d53aec929f06bad1cba2e5f6fba927`。同一助手实施并重新审查，不冒充独立人员批准；没有执行 merge。

## 1. 结论

`TARGETED_REMEDIATION_RE_REVIEW=PASS_WITH_BOUNDED_SCOPE`。

F8A-B01 / F8A-R01：`CLOSED_ON_REVIEWED_HEAD_FOR_ISOLATED_IMPLEMENTATION`。这是两项具体实现缺口的关闭，不是整体隔离验证收口、main集成或真实D5认证。可进入隔离验证收口与集成就绪评估；REAL_D5=NOT_READY，真实 Foundation/U06/U15/APPLIED/P01 CA 保持开放。

## 2. 精确修复面

相对原 #401 HEAD，仅4文件，37增/5删：F8Adapter.java、F8HistoricalReader.java、F8Result.java、F8AdapterTest.java（均在 tools/u07_d5_f8 下）。无 SQL/schema、Source/codec/golden、Foundation实体、生产服务或迁移改动。本报告在独立审查分支新增，不混入 #401 被测试代码。

| finding | 修复与复审依据 | 裁决 |
| --- | --- | --- |
| F8A-B01 blocker | tx.execute 正常返回仍须 afterCompletion=COMMITTED 才发布候选；明确 rollback 返回 RETRYABLE_FAILURE/ATTEMPTED/NOT_COMMITTED；其它完成状态保持 UNKNOWN。捕获正常候选返回前 rollback-only 状态，区分已请求且已确认回滚与提交确认丢失。无DML异常退出保持 DEFER/NOT_ATTEMPTED，无 confirmed。 | CLOSED_IN_ISOLATED_SCOPE |
| F8A-R01 required | 当前与被引用的原 ACCEPTED 均要求 winner_id=null，原 winner clock字段一致；negative 非空引用须匹配已完整验证的原 winner claim。null negative 引用不被当前后来出现的合法 winner 否定。DUPLICATE继续检查原 Source/canonical/binding/cipher/APPLIED完整链；Receipt拒绝ACCEPTED带winner、空串或自引用。 | CLOSED_IN_ISOLATED_SCOPE |

复审重新检查正常返回、异常与 historical 三类路径，不以测试数量取代语义审查。commitStarted 保守 UNKNOWN 规则保留，不能重新信任提交异常后可能误报的 rollback callback；rollbackOnly 为当前回调在返回前观察的实际 TransactionStatus，不接受caller布尔authority。

## 3. 实际回归

三个原审查反例完整保留验收预期，未修改为接受错误行为的断言。新增10个实际 adapter测试实例（包括参数化），原52保持；总62。

| 测试 | 真实断言 |
| --- | --- |
| reviewLocalRollbackOnlyMustNotPublishCommittedReceipt | 捕获真实 Spring status，mark local rollback-only；decision/claim=0、NOT_COMMITTED、confirmed=null |
| globalRollbackOnlyNeverPublishesReceipt | 从实际 EntityManagerHolder 标 JPA transaction rollback-only；NOT_COMMITTED、confirmed=null、零decision/claim |
| flushFailureRollsBackBeforeReceiptPublication | 测试probe持久化canonical实体并显式flush，consumer INSERT权限拒绝导致实际JPA flush异常；整个F8 attempt回滚，无成功收据 |
| reviewAcceptedMustRejectForgedWinnerReference | 原ACCEPTED的伪造缺失winner被隔离INCONSISTENT |
| reviewNegativeMustRejectUnrelatedWinnerReference | 无claim EXPIRED的伪造winner被隔离INCONSISTENT |
| negativeHistoricalWinnerReferenceIsValidated ×2 | EXPIRED/REJECTED合法另一winner可FOUND；篡改引用后read INCONSISTENT、finalize INTEGRITY_CONFLICT |
| negativeNullWinnerRemainsValidAfterLaterWinner | 先提交无winner EXPIRED，后来另一答案ACCEPTED；原negative历史仍FOUND/null引用 |
| receiptValidatorRejectsInvalidWinnerShapes | accepted带winner、duplicate自引用、negative空串/自引用拒绝，合法negative null允许 |
| duplicateCannotHideCorruptOriginalAcceptedWinnerReference | 合法DUPLICATE原链先通过；原ACCEPTED被篡改winner引用后read/finalize隔离 |

flush异常是隔离fixture故障注入，不在正常adapter增加canonical写入权限/逻辑；consumer原权限保持。root只用于明确corruption fixture，不参与正常consumer决策。

## 4. 精确执行证据

| workflow | HEAD及报告 |
| --- | --- |
| [F8 run38032795910](https://github.com/cxjchelsea/AIdoctor/actions/runs/38032795910)，job114157057601 | HEAD 5edc0ee3599bd8993c9452c418b8f7edb343972d；adapter62 + 原型34 = 96，0 failures/errors/skips，BUILD SUCCESS |
| [Source run38032795904](https://github.com/cxjchelsea/AIdoctor/actions/runs/38032795904)，job114157057473 | 同HEAD；Source25，0 failures/errors/skips，BUILD SUCCESS |

两套合计121项。原 commit/rollback unknown、postcommit丢响应、write fault、actual clock跨期、原型34、Source25与alias/RESUME/1062回归均保留通过；Source golden及diff whitespace本机检查通过。Java8/Spring/MySQL结果来自Actions日志，不声称本机Maven执行。原#402三个失败反例的89项/3failure历史保留，旧#401的111项证据也保留，不能追溯改为全覆盖。

## 5. 关闭边界与下一步

本轮没有新发现阻断这两项修复的blocker/required。并非全系统独立评审或正式merge授权；没有重新冻结RDP、修改owner CA或部署。真实网络确认丢失、进程crash、Oracle、outbox写拒绝及生产owner共同guard仍未验证。

下一步：隔离验证收口与集成就绪评估，检查整体清单的残余证据、堆叠基线依赖、main目标精确差异及生产隔离边界，再决定是否整理main集成候选。不得自动merge #397/#401/#402或本审查PR。
