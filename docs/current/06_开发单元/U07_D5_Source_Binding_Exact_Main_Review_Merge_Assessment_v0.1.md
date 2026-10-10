# U07 D5 Source/Canonical Binding 精确 main-target 差异复审与合并授权评估 v0.1

日期：2026-10-10。受审 PR #392。
main/base：86e8843197091c8c8172b7e4213537a31bdf0654。
candidate HEAD：d215a2fa0b0ef24e380ae1b0d23c49c47b47a868。
candidate tree：de9385d29df548615f8c1bb3a670cbf84072eef3。
本次同一助手复审，不声称独立人员批准；没有新增runtime执行。

## 结论

EXACT_MAIN_TARGET_DIFF_REVIEW = PASS
NEW_BLOCKER / REQUIRED = 0 / 0（限本候选隔离集成范围）
MERGE_AUTHORIZATION_ASSESSMENT = READY_FOR_EXPLICIT_EXECUTION_AUTHORIZATION
MERGE_EXECUTION = NOT_AUTHORIZED / NOT_PERFORMED
REAL_D5 = NOT_READY

评估建议仅接受 #392 指定HEAD的隔离测试模块集成。用户本次要求授权评估，未要求执行合并。此记录不将“建议可授权”写成“已获执行授权”，不自动合并 #387/#389/#390/#391 或任何祖先/审查PR。

## 精确差异复核

- #392仅一个commit，唯一parent是上述main；不是将54文件堆叠链合入main。
- 12新增文件、1017 additions / 0 deletions；workflow一份，独立Maven工具模块及Java/test/fixture八份，历史实现/修复/复审记录三份。
- 全部12文件git blob与 #389修复/#390接受版本逐项相等；不存在只比较摘要说明而未比较源码的问题。
- Foundation源码、生产wiring、Shared Contracts、Flyway、V7、ConsultationWaitTransitionService与桥接工具均未纳入。
- 重新核对issuer、fixture DB权限、pom及workflow：固定synthetic profile、package-private工具类、固定disposable DB、consumer无UPDATE/DELETE、原V2 + fixture schema、仅编译CanonicalBusinessEvent*.java与工具代码。
- 当前测试不把错误行为当验收；SB-B01/SB-R01保持 #390有限关闭。当前评估不重新声称全局无缺陷。
- 历史文档保留原精确SHA及提交时PENDING，是provenance记录；候选实际结果以 #392证据评论为准。未将设计合同全文或未合并祖先隐式依赖纳入运行时。

## 验证及状态

| 门禁 | 候选自身证据 | 结果 |
| --- | --- | --- |
| 独立Python golden / diff whitespace | 本地再次执行reference_vectors.py --check、git diff --check | PASS |
| 实际Source/Binding | run 38020085937，job 114118943715 | 25 tests、0 failures/errors/skips、BUILD SUCCESS |
| U02 | run 38020085957；Java 114118944013、Python 114118943801 | workflow SUCCESS；Java全模块434 tests、0 failures/errors、1 skipped；Python 11/9/1 passed |
| U03 | run 38020085931，job 114118943773 | workflow SUCCESS；Java全模块434 tests、0 failures/errors、1 skipped |
| 实际checkout | 0a50edfaa8f24f00753275f27a1261d8ea0d663c | 合成merge tree与candidate HEAD tree相等 |
| GitHub PR状态 | fresh API读取 | open/draft、merged=false、mergeable=true、mergeable_state=clean；head/base未变 |

Java两次全模块执行不能相加为868个独立测试；434中有1项skip，不写零跳过。
原始Source/Binding日志本轮再次读取；U02/U03候选日志已在 #392创建验证时读取，本轮重新确认对应workflow均SUCCESS。
实际CI：https://github.com/cxjchelsea/AIdoctor/actions/runs/38020085937；
证据评论：https://github.com/cxjchelsea/AIdoctor/pull/392#issuecomment-6093244358。
没有把 #389旧run冒充候选执行，或把本审查文档HEAD声称为测试对象。

GitHub clean/mergeable状态不代替分支保护/组织规则授权。本轮未取得完整管理规则断言；执行时须遵守GitHub实际门禁，禁止bypass或force更新main。草稿状态尚须在授权执行时转为ready。

## 若用户明确授权执行

授权对象应明确为 #392 HEAD d215a2fa0b0ef24e380ae1b0d23c49c47b47a868，且main仍为86e8843197091c8c8172b7e4213537a31bdf0654。建议standard merge commit，禁止顺带合并其他PR。

执行前再次核验精确head/base、12文件范围、CI、merge条件。若head或main变化，暂停既有授权的直接执行并复核新的差异/测试适用性；不将旧授权绑定到未知SHA。满足条件后只取消 #392草稿并通过正常PR merge接口合并，不改写main历史。

合并后核对main merge commit父提交、12文件范围、内容tree与已测候选一致，读取post-merge触发的实际CI；未触发相应工作流则明确区分内容等价复核与新执行，不称“post-merge tests passed”。相关旧PR的归档/关闭另行决定。

## 保留边界

仅隔离synthetic Source/Canonical Binding集成；不导入真实患者数据，不注册生产入口，不证明Oracle runtime、真实网络commit unknown/进程crash、真实KMS/issuer/F3/P01/U15/P02/U02 owner合同，或真实F8/Stage C/APPLIED/outbox/dispatch。所有真实owner CA及全局gate保持原状态，真实D5 NOT_READY。

本审查PR只增加本文件，不改变 #392的12文件候选范围。下一步是用户对具体 #392执行对象给出明确merge授权，再做最终复核、standard merge及post-merge verification。
