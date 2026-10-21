# U07 D5 F8 精确 main-target 差异复审与合并授权评估 v0.1

日期：2026-10-10。对象：[PR #405](https://github.com/cxjchelsea/AIdoctor/pull/405)。同一助手复审，非独立人员审批。本轮不改实现、不重跑数据库、不执行合并。

## 决策

- EXACT_MAIN_TARGET_REVIEW=PASS_WITH_BOUNDED_SCOPE；未发现新的实现或集成阻断项。
- MERGE_READINESS=READY_FOR_EXPLICIT_AUTHORIZATION_OF_ISOLATED_CANDIDATE_ONLY。
- MERGE_AUTHORIZATION=NOT_GRANTED；PR仍draft/open/unmerged。GitHub mergeable=true、mergeable_state=clean仅证明机械可合并。
- FULL_CHECKLIST_VERIFICATION=NOT_FULLY_CLOSED；REAL_D5=NOT_READY。

允许提请授权的范围仅为#405隔离synthetic工具、测试、工作流与证据文件。三项专项证据缺口继续OPEN，不能用本评估关闭生产接入、完整清单或真实业务门禁。

## 固定对象与差异

| 对象 | 精确值 |
| --- | --- |
| main/base | 835441492d130de320bf3d49fe2b4be598a2b327 |
| candidate HEAD | da7c7ea71677244410a4db2cf1f1069af5edcdb6 |
| candidate tree | 880e35da4efe5a3518295f6d09c7a3609f2dbf14 |
| CI实际checkout | ebd46cf282f14b97109ef923040f3dabad6fbfc0 |
| CI merge parents | 上述main、candidate HEAD，依次为第一/第二parent |
| 已审实现#401 | 5edc0ee3599bd8993c9452c418b8f7edb343972d |

candidate只有一个parent，即当前main；无叠加PR合并。精确差异22文件、1313增/72删：17个非文档blob逐一与#401相同，5个证据/inventory文档。清单见候选文档U07_D5_F8_Main_Based_Integration_Candidate_v0.1.md。本轮以NUL分隔路径重新核验，避免中文路径quote影响统计；whitespace及Source golden通过，隔离候选工作树干净。

逐项复审Source verifier提取、F8事务完成状态/回滚分类、历史winner关联、owner/action锁、最终SQL时间与条件、DDL权限、build-helper编译输入和workflow隔离。source/checkBinding/target原校验逻辑转移至只读verifier；Source25保持通过。F8修复保留local/global rollback-only、JPA flush失败及winner损坏回归。SQL、schema、Java和测试均无候选整理引入的额外实现变化。

Foundation实体/codec、原生产DDL/migrations、生产service/routes、golden及Source workflow无差异；新F8模块位于tools，非生产Bean，编译只选择工具Source和CanonicalBusinessEvent类。未接APPLY、clinical patch、effect、dispatch/outbox或真实数据。未执行全仓聚合构建，不将diagnosis-service构建等同于所有服务构建。

## 测试证据与checkout更正

此前候选PR把run的head_sha简写为“精确HEAD验证”。严格说四个pull_request run实际使用GitHub测试合并提交；F8/Source日志明确记录checkout ebd46cf，U02/U03亦通过默认PR checkout。该提交tree与candidate tree完全相同，parents精确对应本次main/head，因此可用于本次main-target代码树证据；不能称为已合入main后的验证。

| Run | 结果与限度 |
| --- | --- |
| [F8 38033397298](https://github.com/cxjchelsea/AIdoctor/actions/runs/38033397298) | job114158786034：adapter62+SQLClock34=96，failure/error/skip均0，BUILD SUCCESS |
| [Source 38033397302](https://github.com/cxjchelsea/AIdoctor/actions/runs/38033397302) | job114158786094：25，failure/error/skip均0，BUILD SUCCESS；定向合计121 |
| [U02 38033397327](https://github.com/cxjchelsea/AIdoctor/actions/runs/38033397327) | Java job114158786235：contracts安装、diagnosis-service编译/定向/全回归成功；434，0failure/error、1skip；Python job114158786277：11+9+1 passed |
| [U03 38033397341](https://github.com/cxjchelsea/AIdoctor/actions/runs/38033397341) | job114158786283：编译/定向/全回归成功；全回归434，0failure/error、1skip |

本轮重新读取run最终状态及五份job日志，四run均completed/success。重复全套不计为新增独立覆盖。唯一skip为U03Cd08FrozenClinicalValidationTest，其Assumptions要求u03.cd08.cases及u03.cd08.report显式参数；该临床验证未执行，不能算通过。

## 未关闭项目及授权边界

| 项 | 本轮裁决 |
| --- | --- |
| EV-F8-01 outbox写拒绝 | OPEN/NOT_EXECUTED；无真实outbox fixture，本候选排除该能力 |
| EV-F8-02 F8实际CAS零行/1062专项 | OPEN/NOT_EXECUTED；CAS前后故障、Source1062不能替代专项执行 |
| EV-F8-03 F8 RESUME missing/corrupt target逐类入口 | OPEN/NOT_EXECUTED；共享Source verifier25支持底层校验，不替代F8逐类结果映射证据 |
| 生产owner CA/APPLIED/P01、真实网络/crash、Oracle | OUTSIDE_THIS_SLICE/UNVERIFIED，生产接入前另设门禁 |

这些项目不被本次有限工具集成评估认定为新合并blocker；其未执行状态是授权范围的一部分。若授权目标改为完整清单关闭或真实D5接入，本结论不适用。F8A-B01/F8A-R01仅继承#403已执行范围的关闭。

## 可执行的下一步合同

可以由用户明确授权“仅将PR #405以standard merge commit合入main，随后进行合并后验证”；本轮评估不代替该授权，不批量合并#397/#401或其他文档PR。

执行前重新核对main/head、PR范围、所有check及分支规则；任一代码/base变化须重新评估。合并后记录实际merge SHA/parents/tree并与受审tree比较，确认只合入#405。U02/U03有main push触发；F8/Source仅pull_request触发，不会自动形成合并后数据库证据。须在实际merge SHA上用独立验证PR/分支重跑已有隔离workflow，明确checkout与tree关系，不擅改workflow添加生产行为；若无法执行，合并后验证应报告PARTIAL并保留缺口。本轮未证明后续执行已获授权或已完成。
