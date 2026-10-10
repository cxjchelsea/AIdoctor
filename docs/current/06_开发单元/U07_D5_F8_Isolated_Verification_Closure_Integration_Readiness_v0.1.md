# U07 D5 F8 隔离验证收口与集成就绪评估 v0.1

日期：2026-10-10。本轮只形成证据裁决和集成准备合同，不修改实现、不新跑数据库套件、不合并。为同一助手评估，非独立人员批准。

## 1. 决策

- `ISOLATED_IMPLEMENTED_SCOPE_VERIFICATION=BOUNDED_ACCEPTED_WITH_EXPLICIT_EXCLUSIONS`。
- `FULL_CHECKLIST_VERIFICATION=NOT_FULLY_CLOSED`：V11 outbox写拒绝未执行，不能将11组全部标PASS。
- `INTEGRATION_READINESS=READY_FOR_MAIN_BASED_CANDIDATE_PREPARATION_WITH_CONDITIONS`。
- `EXACT_MAIN_CANDIDATE_VERIFICATION=PENDING`；`MERGE_AUTHORIZATION=NOT_GRANTED`；`REAL_D5=NOT_READY`。

关闭的是当前synthetic Source/Canonical→F8实现已执行范围；不是生产owner CA、全部数据库故障模型或真实F8/P01业务交付。F8A-B01/F8A-R01的关闭仅继承#403在精确受审HEAD的有限结论，不扩大。

## 2. 精确基线与证据核验

| 对象 | 本轮核验值 |
| --- | --- |
| 最新main | 835441492d130de320bf3d49fe2b4be598a2b327 |
| 原型#397 / #401 base | 0ccfe62dc6d53aec929f06bad1cba2e5f6fba927，仍未合并 |
| #401实现HEAD | 5edc0ee3599bd8993c9452c418b8f7edb343972d；draft，未合并 |
| #403修复复审HEAD | ff994388a5eb511ae3d6687c01832304e34ab37e；仅一份文档，base为上述实现HEAD |
| F8 Actions | [38032795910](https://github.com/cxjchelsea/AIdoctor/actions/runs/38032795910)，job114157057601；同实现HEAD，success；62 adapter+34原型=96，零failure/error/skip |
| Source Actions | [38032795904](https://github.com/cxjchelsea/AIdoctor/actions/runs/38032795904)，job114157057473；同实现HEAD，success；25，零failure/error/skip |

本轮重新读取远程PR/commit/run及两份job日志，总计121项真实执行证据。没有把文档分支HEAD称为测试代码HEAD。main为实现HEAD祖先，merge-base等于最新main；本轮git diff whitespace与Source golden检查成功。工作区另有先前文档PR的未跟踪副本，未纳入任何candidate inventory，不声称工作区完全干净。

原#402三个失败反例历史保留；当前原断言均通过。未知提交/回滚仍为manager/probe注入；真实网络故障、process crash与Oracle无证据。121个测试不等于121个生产前提通过。

## 3. #400验收矩阵收口

| 组 | 裁决 | 核验依据与限制 |
| --- | --- | --- |
| V01 基线 | BOUNDED_PASS | 原型34、Source25、golden，原套件未删减 |
| V02 接入 | BOUNDED_PASS | adapter source/cipher/binding/canonical timestamp/原APPLIED proof损坏、alias、RESUME happy path；shared target verifier损坏由Source25覆盖，不虚报每种target故障都有独立F8入口例 |
| V03 事务 | PASS_IN_FIXTURE | existing outer、错误manager/DS、unjoined、实际autocommit、真实connection ID mismatch拒绝；mismatch仅joined报告代理 |
| V04 许可 | BOUNDED_PASS | historical/new finalize分离、跨scope、guard前epoch撤销；owner/F8两个锁顺序验证guard串行，不冒充完整生产action撤销认证 |
| V05 政策 | PASS_IN_FIXTURE | P1–P8、NULL/moved/terminal/expiry、pending winner、错scope claim、缺owner；仅独立synthetic issuer事实 |
| V06 时钟 | BOUNDED_PASS | 原型固定equality与真实statement time；adapter实际跨期、session +08:00；固定clock不冒充真实微秒同步认证 |
| V07 原子性 | BOUNDED_PASS | sentinel/decision/CAS前后故障、零行、跨期、local/global rollback-only、实际JPA flush失败；CAS影响0行的专门adapter注入仍未单独执行 |
| V08 并发 | PASS_IN_FIXTURE | 同identity/不同答案、owner先/F8先、一个pending winner；不证明生产writer都采用guard |
| V09 读回 | BOUNDED_PASS | 四verdict、claim/APPLIED/原proof损坏、旧sentinel、UNKNOWN后FOUND/ABSENT/UNAVAILABLE；新增winner关联反例与合法negative历史通过 |
| V10 结果 | BOUNDED_PASS | 结果矩阵、确定rollback、rollback-only、flush异常、commit/rollback unknown；Source实际1062非F8实际1062注入证据 |
| V11 权限/副作用 | PARTIAL_WITH_SCOPE_EXCLUSION | owner/APPLIED/canonical/runtime权限拒绝与行数/bytes不变；没有outbox表写拒绝实测；未接production dispatch/effect/P01，不声称其端到端已验证 |

这里的BOUNDED_PASS承认具体实测集合；不将未执行分支补算为PASS。当前未发现需要撤销两项修复关闭或禁止整理隔离main候选的新实现blocker。未验证项保留登记，不自动降级为生产可用。

## 4. 剩余证据登记

| 项 | 处理与门禁 |
| --- | --- |
| EV-F8-01：V11 outbox写拒绝无fixture证据 | 保持OPEN/NOT_EXECUTED；候选明确排除outbox能力，生产接入前需对应owner及权限测试。不新增临时outbox表来冒充真实owner认证；若未来要求FULL_CHECKLIST_CLOSED，必须补证据或另作显式范围裁决 |
| EV-F8-02：adapter实际CAS零行及1062分支专项注入 | 保持OPEN/NOT_EXECUTED，作为下一轮候选验证增强项；已有CAS前后rollback与Source1062不能替代这些具体分支。候选说明中须保留，不虚报覆盖 |
| EV-F8-03：F8 RESUME missing/corrupt target专项退出 | shared verifier/Source回归已有证据，F8逐类入口专项覆盖仍未执行；候选验证可补零decision和typed结果检查 |
| 真实Foundation/U06/U15/APPLIED/P01 CA、网络/crash/Oracle | 保持OPEN/OUTSIDE_THIS_SLICE；本决策不授权真实接入 |

## 5. main集成差异与依赖

#401相对其prototype base为15文件；相对main实际19文件，1122增/72删。原型base相对main另带6文件，与adapter差异有重叠，不能直接15+6计数。

实际19文件如下（仅inventory，尚未创建main-target代码PR）：

- .github/workflows/u07-d5-f8-sql-clock.yml
- docs/current/06_开发单元/U07_D5_F8_Adapter_Implementation_Evidence_v0.1.md
- docs/current/06_开发单元/U07_D5_F8_SQL_Clock_Prototype_Physical_Verification_v0.1.md
- tools/u07_d5_f8/pom.xml
- tools/u07_d5_f8/src/main/java/com/aidoctor/verification/sourcebinding/F8Adapter.java
- tools/u07_d5_f8/src/main/java/com/aidoctor/verification/sourcebinding/F8HistoricalReader.java
- tools/u07_d5_f8/src/main/java/com/aidoctor/verification/sourcebinding/F8Identity.java
- tools/u07_d5_f8/src/main/java/com/aidoctor/verification/sourcebinding/F8OwnerReader.java
- tools/u07_d5_f8/src/main/java/com/aidoctor/verification/sourcebinding/F8Result.java
- tools/u07_d5_f8/src/main/resources/adapter-finalize.sql
- tools/u07_d5_f8/src/test/java/com/aidoctor/verification/f8/SqlClockTest.java
- tools/u07_d5_f8/src/test/java/com/aidoctor/verification/sourcebinding/F8AdapterTest.java
- tools/u07_d5_f8/src/test/java/com/aidoctor/verification/sourcebinding/F8TestAuthority.java
- tools/u07_d5_f8/src/test/resources/bootstrap-adapter.py
- tools/u07_d5_f8/src/test/resources/finalize.sql
- tools/u07_d5_f8/src/test/resources/schema-adapter-mysql.sql
- tools/u07_d5_f8/src/test/resources/schema-mysql.sql
- tools/u07_d5_source_binding/src/main/java/com/aidoctor/verification/sourcebinding/SourceBindingAdapter.java
- tools/u07_d5_source_binding/src/main/java/com/aidoctor/verification/sourcebinding/SourceBindingVerifier.java

原型旧证据文档记载首轮25项与未完成adapter，是历史事实；adapter实施文档又引用旧111项。main候选须同时携带或明确引用最新#403与本决策，不把旧文档当现行121项收口结论。#394/#396/#398/#399/#400/#402/#403未合入main的设计/审查不靠批量merge带入；选取必要记录或固定精确SHA引用。

Source修改仅工具verifier提取，须继续Source25回归；Foundation实体、codec、原DDL/golden及生产service/migration无差异。新模块通过build-helper选择有限Source/Foundation编译输入，不等于根项目/全服务build已跑。candidate不得增加production Bean/routes、APPLY、effect、dispatch或真实数据。

## 6. 集成准备合同

下一步可以从届时最新main创建隔离candidate，复制精确已审实现与原型必要文件，附精确inventory及证据摘要。当前main尚未变化，代码树可保持19文件已审差异；文档整理的新增差异需单独列明。不要直接合并叠加PR #397/#401，也不要把审查反例旧文件回放覆盖修复后的测试。

候选必须：

1. 固定新main/candidate HEAD，核对与受审实现的Java/SQL/schema/测试内容等价；任何变化重新审查。
2. 在main-target PR的精确HEAD重新执行62+34+25和golden，不复用旧HEAD绿灯作为新候选通过证据；必要时针对EV-F8-02/03增强验证并更新数量。
3. Source workflow仍采用固定独立数据库，F8 workflow仅synthetic schema，确认根build/production隔离与文件范围。
4. 单独做精确main-target差异复审和合并授权评估。当前决定只支持candidate整理，无merge授权。

本轮交付仅一份main-based评估文档，不创建代码candidate、不改变main、不部署。下一步是“从最新main整理隔离F8集成候选并验证”，其后才讨论merge。
