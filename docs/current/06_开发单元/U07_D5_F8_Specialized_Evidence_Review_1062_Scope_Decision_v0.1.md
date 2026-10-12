# U07 D5 F8 专项补证定向复审与1062范围裁决 v0.1

日期：2026-10-12。对象PR #408，HEAD99f46f89d1ac512d68360062e0f53276f31e6dd5；main702e280f14adacb50d6b72a6cd42cc7d32ff020a。本轮重新读取精确差异、实现、identity、权限schema、run状态及4份job日志；没有新跑套件、修改实现或合并。同一助手复审，不称独立人员批准。

## 裁决

TEST_IMPLEMENTATION_REVIEW=PASS_WITH_BOUNDED_SCOPE；未发现新增实现blocker。
EVIDENCE_RECORD_INTEGRATION=REQUIRED_DOC_UPDATE，F8EV-D01须在集成前修订。
MERGE_AUTHORIZATION=NOT_GRANTED；FULL_CHECKLIST_VERIFICATION=NOT_FULLY_CLOSED；REAL_D5=NOT_READY。

| 证据项 | 本轮裁决 |
| --- | --- |
| EV-F8-02/CAS_ZERO | BOUNDED_CLOSED_ON_REVIEWED_PR_TREE：实际adapter CAS谓词被同事务probe破坏，零匹配、无after_claim、INTEGRITY_CONFLICT/ATTEMPTED/NOT_COMMITTED、零decision/claim、fresh ABSENT、重试ACCEPTED |
| EV-F8-02/REAL_1062_PROPAGATION | BOUNDED_ACCEPTED：真实MySQL唯一约束1062/23000，由probe DML抛入owned transaction，原异常传播、确定回滚及重试正确 |
| EV-F8-02/ADAPTER_OWN_DML_1062 | OPEN/NOT_EXECUTED：不能用probe自己的INSERT替代adapter原生INSERT发生异常的证据 |
| EV-F8-03 | BOUNDED_CLOSED_ON_REVIEWED_PR_TREE，仅五种列明target故障；不等于已集成main或生产target权威认证 |
| EV-F8-01 outbox | OPEN/NOT_EXECUTED，继续范围排除 |

## 差异与证据

PR #408相对当前main仅2文件、61增/0删。原62 adapter测试未删改；新增2个@Test和5次参数化执行。未改生产或工具实现、SQL、DDL、workflow、权限、codec/golden。missing-canonical测试只在root同一Connection暂时关闭FK检查，finally恢复，用于既存损坏fixture；不是放宽consumer权限。

F8 run38041195029/job114181613952：69 adapter+34 SQLClock=103，failure/error/skip均0，BUILD SUCCESS。
U02 run38041195085：job114181614189编译/定向/全套434、0failure/error、1skip；job114181614361 Python11+9+1 passed。
U03 run38041195074/job114181614127：编译/定向/全套434、0failure/error、1skip。
本轮重新读出三个run均completed/success。重复全套不计唯一增量覆盖。CD08显式cases/report临床验证skip不算通过。

实际checkout为PR测试合并02fcbecc4c7338f8cca2ed4a2e6fc1f364fe75dd，parents main702e280f/head99f46f89，tree9690b8483a8b4ee5413725501aee6b19f43f1dc8与candidate相同。Source25继承main702e280f上run38035233680/job114164180154的合并后证据，未在本轮重新执行。只能称“103新执行+25未改基线证据”，不能称128精确HEAD新执行。

五种RESUME target故障均先合法admit，再损坏target：missing canonical→DEFER/INDETERMINATE；missing binding、fingerprint、cipher→INTEGRITY_CONFLICT/INCONSISTENT；wrong scope→DENIED/MISMATCH。所有finalize NOT_ATTEMPTED、无receipt，原target与resume均无decision、无claim。该证据是typed入口与所列F8零写入断言，不扩大为全数据库所有字段不变证明。

## Finding F8EV-D01：现行证据记录滞后

级别REQUIRED_DOCUMENTATION，非实现blocker。#408新增文档仍写“CI尚未执行”“EV02/03保持OPEN/PENDING”及预计128，最终103新执行和有界复审结果只在PR body。PR body有完整证据，但仓库记录作为集成交付仍须更新，避免读者把规划状态当当前结果。

修订必须记录真实run/job、精确被测checkout/tree、Source继承证据、CAS/1062/RESUME拆分状态及REAL_D5/未执行项。文档修订HEAD不可冒称之前测试HEAD；若代码不变，可明确继承原受测代码blob证据，或重新执行后再记状态。

## 剩余1062范围裁决

从代码推断：同一已验证tenant/consultation/actor使用同一scope guard的FOR UPDATE；历史claim/decision读取发生于持guard期间；所有遵守该guard的同身份finalizer在写入前串行，因此通常第二调用看到历史结果或pending winner，而不是竞争INSERT。该判断只限同guard、不可变绑定、正常hash/完整schema、遵守协议writer。scopeKey含actor、waitKey不含actor，不能据此宣称所有跨actor/cross-scope writer均同guard，更不能证明生产writer遵守协议。

保留两个不同验证目标：
1. 原生DML异常处理：值得下一轮补一个测试专用JDBC边界故障fixture，使adapter实际prepare/execute的claim INSERT触发真实约束1062；必须观察该具体statement、vendor/sqlstate、确定rollback、无receipt/残留、fresh read与干净重试。可在同一底层事务执行前种入冲突行，再让原statement真实执行；不能直接返回mock SQLException或使用SIGNAL伪造1062，也不能将这个单事务故障称为生产并发冲突。
2. 不遵守guard的外部writer竞争：继续OPEN/OUTSIDE_COMPLIANT_PROTOCOL，不为得到1062而绕开guard、降低隔离级别或取消锁。若后续真实owner合同允许该writer，须单独设计带barrier和真实独立连接的专项测试；允许实际出现1205/1213等不同错误，不能把其中任意异常算1062通过。

正常同guard并发已有两类测试支持一个winner，不是新增1062实测。当前无需修改adapter生产行为、增加“遇1062即成功”或自动重新接回事务；确定回滚后仍按RETRYABLE_FAILURE及独立fresh read处理。

## 下一步

在隔离#408分支补“adapter原生claim INSERT真实1062”故障测试并更新F8EV-D01证据文档；保留已有7项及原套件，再真实编译执行并定向复审。若fixture无法保持JPA/JDBC同DS、同物理连接及真实事务，宁可维持OPEN，不接受代理绕过guard。任何新head须分别标注代码和证据SHA。

本评估文档独立main-based PR保存，不自动合并#408或其他PR。没有撤销#405已完成的有限集成；没有将真实D5升级为READY。
