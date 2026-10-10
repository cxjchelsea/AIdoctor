# U07 D5 隔离 F8 Adapter 实施与验证证据 v0.1

日期：2026-10-10。实施 PR：#401（draft），基线：#397 修复后分支 `prototype/u07-d5-f8-sql-clock`。

## 1. 精确范围与结论

基线 SHA：`0ccfe62dc6d53aec929f06bad1cba2e5f6fba927`。代码/测试证据 SHA：`62e88b47740ef2a178952bc96c5661d9c0a7a59e`。本文随后独立文档提交，不改动被验证代码；最终 PR HEAD 的 CI 另在 PR 中记录。

按 #400 清单实现 owned F8 outer transaction、Source/Canonical verifier、独立 owner/action/APPLIED fixture、条件 SQL 最终决策、完整历史读回及故障恢复结果合同。范围为 synthetic、Spring/JPA/JDBC 与 MySQL 8 隔离适配器；不作为真实 owner CA 或生产集成通过结论。

状态：`IMPLEMENTED_FOR_TARGETED_REVIEW`；`REAL_D5=NOT_READY`；未合并。后续门禁为实现与证据定向复审。

## 2. 精确修改 inventory

相对基线，代码及测试 14 文件；本文为第 15 文件。

| 路径（包路径均为 com/aidoctor/verification/sourcebinding） | 作用 |
| --- | --- |
| tools/u07_d5_source_binding/src/main/java/包/SourceBindingAdapter.java | 将原 source/binding/target 校验委托 verifier，原 admission/epoch/alias/1062 协议保留 |
| 同目录 SourceBindingVerifier.java | 同事务校验及不可由调用者构造的 VerifiedCanonical，无事务创建与提交 |
| tools/u07_d5_f8/src/main/java/包/F8Adapter.java | outer 事务、实际资源守卫、读/写许可、finalize、attempt 与 durability 映射 |
| 同目录 F8Identity.java | 从 verified canonical 派生 decision/wait/scope identity；额外内部辅助类，不增加公共入口 |
| 同目录 F8OwnerReader.java | guard/action 与四类独立 issuer owner 记录，scope/provenance/version 校验 |
| 同目录 F8HistoricalReader.java | decision/claim/APPLIED 原 winner 与原 Source proof 全链读回 |
| 同目录 F8Result.java | operational/attempt/durability/reason 合同与组合 validator |
| tools/u07_d5_f8/src/main/resources/adapter-finalize.sql | P1–P8、有版本及许可 epoch 谓词的 INSERT SELECT、单语句时钟 |
| tools/u07_d5_f8/src/test/java/包/F8AdapterTest.java | 实际 Spring/MySQL adapter、故障、恢复、并发、权限测试 |
| 同目录 F8TestAuthority.java | 独立 fixture issuer/writer，实际 owner 变更遵守同 guard |
| tools/u07_d5_f8/src/test/resources/schema-adapter-mysql.sql | 独立 schema、binary identity、FINAL-only 及最小 consumer 权限 |
| 同目录 bootstrap-adapter.py | 固定 Source DDL checksum 与固定 schema 重映射，复用原 Foundation/Source DDL |
| tools/u07_d5_f8/pom.xml | JPA 及有限 Source/Foundation 编译输入 |
| .github/workflows/u07-d5-f8-sql-clock.yml | 保留 34 原型例，增加独立 adapter schema、golden、报告 artifact |

不修改 Foundation 实体、生产 service/migration、U06/U15/P01 owner、Source codec/golden。无生产 bean、路由、clinical APPLY、effect/dispatch/outbox 接入。

## 3. 实现合同与本轮修正

Source 验证只提供事务内事实，不能作为 F8 COMMITTED 证明。F8 使用单一 JpaTransactionManager/DataSource，SERIALIZABLE；检查 existing outer、manager/DS、joined EM、bound connection、autocommit 与实际 CONNECTION_ID。新决策先读当前 historical-read 许可；Tier0 完整历史返回不重算 deadline，新 finalize 另检查 action epoch 与权限。

四类 owner 记录独立验证，不接受 caller authority_complete。issuer 与 consumer 使用同 Consultation guard；consumer 只有 owner SELECT，不能更新 owner/APPLIED/canonical/runtime。claim 保留完整 wait identity 列与唯一约束，并以 raw row 校验防止错误 scope 被 JOIN 隐藏。APPLIED 必须追溯原 ACCEPTED/claim/generation/answer digest 及原 Source/binding/cipher 证据。

sentinel 只用于 P7 候选；最终 SQL 时间跨期后整次 attempt 回滚，下一次 EXPIRED 无 sentinel。持久 DML 前设置 ATTEMPTED，零行仍属于 attempted；仅确认 precommit rollback 为 NOT_COMMITTED。提交阶段发生异常，即使 Spring callback 报告 rollback，仍保守 UNKNOWN、confirmed=null，另开 fresh read 恢复。提交/回滚确认丢失由 manager/probe 注入；未执行真实网络故障或进程 crash。

测试曾发现 rollback-unknown fixture 在数据库回滚前抛错造成锁残留；已改为实际回滚后丢确认。随后发现真实提交后抛错被错误报告 NOT_COMMITTED；已增加 beforeCommit 阶段跟踪，相关回归验证 fresh FOUND_MATCH。autocommit 拒绝用例在实际 guard 读取 true 后恢复 fixture 连接，再执行 rollback；不同物理 JPA 连接用例仅代理 joined 报告，CONNECTION_ID 比较使用真实第二连接。

## 4. 按清单的证据映射

| 组 | 实际验证与边界 |
| --- | --- |
| V01 | 34 原型 + Source25 + golden；原套件未删减 |
| V02 | source/cipher/binding/canonical timestamp/original APPLIED proof 损坏、RESUME target、alias、scope |
| V03 | existing outer、错误 DS/JDBC manager、真实未 joined EM、实际 autocommit、实际不同 connection ID |
| V04 | read/finalize 许可分离、跨 scope、进入 guard 前独立 issuer 撤销 action epoch |
| V05 | applied 同/不同答案、expired/terminal/cleared/moved/current pending、P8、缺 owner、异常 raw claim |
| V06 | 原型固定 equality 与边界、adapter 真实 SQL 时间跨期、实际 session +08:00 |
| V07 | sentinel/decision/CAS/before-commit 故障、零行、跨期回滚；decision 与 claim 原子性 |
| V08 | 同答案、不同答案同 wait、owner/F8 两种锁顺序 |
| V09 | 四 verdict、错误 own/original claim/APPLIED、旧空 sentinel、UNKNOWN 后 FOUND/ABSENT/UNAVAILABLE |
| V10 | operational×attempt×durability 枚举、确定 rollback、注入 commit/rollback unknown、confirmed 字段限制 |
| V11 | consumer owner/APPLIED/canonical/runtime 写入拒绝、canonical bytes/相关行数不变；无 outbox 表/生产 dispatch 接入，未声称实测 outbox 写拒绝 |

`ABSENT`/`UNAVAILABLE` 不消除原 UNKNOWN，也不授权重派 winner。唯一冲突及其它数据库异常走失败 attempt，fresh read 是独立入口；Source25 保留原 1062 回归。F8 同 scope 并发由 guard 串行化，未声称以实际 1062 注入证明所有数据库错误码分支。

## 5. 精确执行结果

代码 HEAD `62e88b47740ef2a178952bc96c5661d9c0a7a59e`：

| workflow/run/job | 实际报告 |
| --- | --- |
| F8 [38031944291](https://github.com/cxjchelsea/AIdoctor/actions/runs/38031944291)，job 114154550734 | F8AdapterTest 52、SqlClockTest 34；共 86，failures/errors/skipped 均 0，BUILD SUCCESS |
| Source [38031944315](https://github.com/cxjchelsea/AIdoctor/actions/runs/38031944315)，job 114154550922 | SourceBindingTest 25，failures/errors/skipped 均 0，BUILD SUCCESS |

两套合计 111 项；golden check 成功。workflow 的最终 conclusion 与文档提交后 HEAD 验证在 PR body 记录。

本机仅执行 golden 与 diff whitespace 检查；Java 8/Spring/MySQL 结果来自上述 GitHub Actions 日志，不使用本机未安装的 Maven/MySQL 声称通过。

## 6. 保留边界

该实现尚需定向实现/证据复审，不能替代独立审查。真实 Foundation audit、U06 issuance、U15 共享锁、APPLIED/P01 owner CA 保持开放；Oracle、真实数据、生产 owner writer 遵守协议、部署、main 集成和 merge 均未验证/执行。fixture 权限与 guard 证据只支持本隔离范围。
