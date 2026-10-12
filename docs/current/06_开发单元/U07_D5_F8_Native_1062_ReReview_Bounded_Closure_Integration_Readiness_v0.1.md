# U07 D5 F8 原生1062夹具复审、有限验证关闭与集成就绪裁决 v0.1

日期2026-10-12。对象PR #408 HEAD5a5e26670d4252e294a9751a1970057918ffaa38；main702e280f14adacb50d6b72a6cd42cc7d32ff020a。本轮复审实现与证据，不新跑测试、不改#408代码、不执行合并。同一助手复审，非独立人员审批。

## 裁决

- NATIVE_JDBC_FIXTURE_REVIEW=PASS_WITH_BOUNDED_SCOPE；没有新增required finding或实现blocker。
- F8EV-D01=BOUNDED_CLOSED_ON_REVIEWED_PR_HEAD：仓库记录已包含真实代码SHA/checkout/tree/run/job/result，历史PENDING由末节显式替代。
- EV-F8-02=BOUNDED_CLOSED_FOR_LISTED_ISOLATED_FAULTS：CAS零行、probe真实1062传播、adapter原生claim INSERT真实1062及确定回滚均有实测。不是任意statement/数据库厂商/竞争writer故障认证。
- EV-F8-03=BOUNDED_CLOSED_FOR_FIVE_LISTED_RESUME_TARGET_FAULTS，继承#409并确认测试保留。
- ISOLATED_SUPPLEMENT_VERIFICATION=BOUNDED_ACCEPTED_WITH_EXPLICIT_EXCLUSIONS。
- INTEGRATION_READINESS=READY_FOR_EXPLICIT_LIMITED_MERGE_AUTHORIZATION_OF_PR408。
- MERGE_AUTHORIZATION=NOT_GRANTED；FULL_CHECKLIST_VERIFICATION=NOT_FULLY_CLOSED；REAL_D5=NOT_READY。

关闭仅作用于上述受审PR树，不代表已集成main。EV-F8-01 outbox写拒绝仍OPEN/NOT_EXECUTED；外部不遵守guard的writer竞争仍OPEN/OUTSIDE_COMPLIANT_PROTOCOL；生产owner CA/APPLIED/P01、APPLY/effect/dispatch、真实网络/crash及Oracle仍未认证。

## 精确差异与checkout

#408相对main仅2文件、150增/0删：F8AdapterTest新增110行、专项补证文档40行。原main62 adapter项及#408早期新增7项均保留；现在adapter70。没有生产/工具adapter实现、SQL、DDL、权限、workflow或Source模块差异。

最终候选tree ac5340fc1cf89cdb30d7518e72408b9aef2595dd。
最终CI实际checkout e415f0990fcf4a32605517198f5b55e69c18d650，parents依次为main702e280f、HEAD5a5e2667，tree与候选相同。实际PR合并树证据不能称为合并后main证据。最终HEAD仅文档相对代码提交5bd29b16，但最终树仍再次真实执行了104项。

main与受审HEAD远程核验未变；PR draft/open/unmerged。机械可合并不等于执行授权。

## 原生JDBC夹具审查

NativeConflictConfig仅由新增测试显式创建独立Spring context；其中NativeClaimConflictDataSource供EMF/JpaTransactionManager/F8Adapter共同使用，继承fixture固定disposable URL和consumer凭证。adapter原有guard运行，未伪造joined状态、事务绑定、connection ID或autoCommit。

连接proxy将方法委托实际连接。只对原生claim INSERT准备的statement包装；参数setter调用照常委托并记录。armed原子开关仅触发一次。在executeUpdate前以同raw connection准备相同SQL、复用实际绑定参数种入1行；随即调用原PreparedStatement.executeUpdate。InvocationTargetException解包原SQLException，未替换异常、未构造1062或SIGNAL。seed statement try-with-resources关闭，原statement/connection仍由实际调用方关闭，context退出关闭。没有额外独立连接提交seed，零残留证明其跟随owned transaction回滚。

断言种入1行、原statement执行1次、after_sentinel未到达、真实1062/23000、autoCommit=false与正connection ID；结果RETRYABLE_FAILURE/ATTEMPTED/NOT_COMMITTED、DB_FAILURE、queryKey准确、receipt为null；fresh连接查询零claim/decision、fresh read ABSENT、正常adapter重试ACCEPTED。日志实际SQL明确为原生INSERT INTO f8_claim(wait_key,... ) VALUES (...)，排除只由旧probe INSERT SELECT错误冒充本次原生路径。

该fixture为特定SQL的单事务执行边界，不是通用JDBC代理；没有覆盖batch/参数清空/statement复用/每类driver，它们不是此受测调用路径。只对这一原生claim INSERT故障接受证据，不扩张为所有F8决策INSERT或生产竞争路径。遵守同guard的合法调用先串行读历史，正常并发不会靠此夹具证明1062；真实不遵守协议writer如需接入须另设合同与测试。

## 最终HEAD真实执行证据

| Run/job | 结果 |
| --- | --- |
| [F8 38191318748](https://github.com/cxjchelsea/AIdoctor/actions/runs/38191318748) /114624486674 | 70 adapter+34 SQLClock=104；failure/error/skip均0，BUILD SUCCESS；日志含原生SQL真实1062/23000 |
| [U02 38191318722](https://github.com/cxjchelsea/AIdoctor/actions/runs/38191318722) /114624486584 | contracts install、diagnosis编译、定向及全套成功；434，0failure/error、1skip |
| 同U02 /114624486821 | Python11+9+1 passed |
| [U03 38191318727](https://github.com/cxjchelsea/AIdoctor/actions/runs/38191318727) /114624486758 | 编译/定向/全套成功；434，0failure/error、1skip |

本轮重新读取3个run最终completed/success及4份完整job日志。CD08显式参数临床验证skip不算通过；两份434不当新增唯一覆盖。Source25仅继承未改main的合并后run38035233680/job114164180154，非本HEAD新执行；不称129项全新执行。没有全仓聚合构建或生产端到端验证。

## 文档问题关闭与集成合同

专项文档保留发布时PENDING历史段落，但末节明确替代其状态，给出真实首轮104项代码提交、checkout/tree/run/job及未认证范围。最终HEAD再次验证的104项记录在PR body；不混淆文档SHA与代码SHA。F8EV-D01所要求的仓库内真实已执行证据已满足，无需为每次文档记录反复生成测试证据提交。

#408已经是main-based两文件候选，没有另建代码candidate或批量合并历史PR的必要。可以提请用户明确授权“仅将PR #408以standard merge commit合入main，随后进行合并后验证”。本裁决不代替该指令，也不授权合并本评估文档PR。

执行前固定main/head、核对所有check和服务端合并条件；任何实现/base变化须重新核验。合并后确认merge parents/tree与受审对象一致、仅#408进入main，并在实际merge SHA执行合并后F8及必要回归。当前workflow不自动覆盖main push，可沿用未合并验证PR固定checkout的方式，不擅改main workflow或绕过branch规则。原出界门禁保持OPEN。若届时无法执行，须报告实际PARTIAL，不能把本轮PR绿灯称合并后验证。
