# U07 D5 F8 main基线隔离集成候选 v0.1

日期2026-10-10。固定main基线835441492d130de320bf3d49fe2b4be598a2b327。单一main父提交，不合并任何堆叠PR。

从受审#401 HEAD5edc0ee3599bd8993c9452c418b8f7edb343972d原样转移19文件差异，17非文档文件逐blob相同。另带#403复审报告、#404收口评估与本清单，共22文件/5文档。

## 转移inventory

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

新增文档均位于docs/current/06_开发单元：U07_D5_F8_Adapter_Targeted_Remediation_ReReview_v0.1.md（#403 ff994388a5eb511ae3d6687c01832304e34ab37e）；U07_D5_F8_Isolated_Verification_Closure_Integration_Readiness_v0.1.md（#404 380a70bf1d4aaa4679e31f02adc74cad628cabc2）；本清单。

## 验证合同与状态

旧受审HEAD的121项绿灯不代替候选验证。候选新HEAD的adapter62+原型34+Source25将在Spring/Java8/MySQL8 CI重跑，run/job/count/conclusion完成后记录于PR body；本文件不预先声明通过。Source golden与whitespace本机检查。原25/111项旧文档保留历史含义，当前判断以附带#403/#404与候选新HEAD结果为准。

本轮复制原测试而不增添专项例：EV-F8-01 outbox写拒绝未执行；EV-F8-02 adapter CAS零行/实际1062专项注入未执行；EV-F8-03 F8负向target专项入口未执行。全部保持OPEN/NOT_EXECUTED，不把Source1062/target回归重命名成F8专项证明。完整清单未全部关闭。

无生产bean/routes、临床APPLY、effect/dispatch/outbox接线、真实数据、Oracle或部署。Foundation实体/codec/DDL/golden、Source原workflow、生产service/migration不修改。build-helper只编译有限Source/Foundation类，不代表根Maven或全服务build已通过。Source独立固定DB与F8固定33324 disposable schema保持；真实Foundation/U06/U15/APPLIED/P01 CA开放，REAL_D5=NOT_READY。

下一步：候选验证通过后做精确main-target差异复审与合并授权评估。本轮无merge授权/执行。main或HEAD变化后重新核验。没有自动合并#397/#401/#402/#403/#404。
