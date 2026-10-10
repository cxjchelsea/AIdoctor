# U07 D5-P PR #379 实现与证据复审及有限关闭决定

日期：2026-10-10。受审 PR：https://github.com/cxjchelsea/AIdoctor/pull/379
精确 HEAD：b2fe349103c68035d2cb298de8f0ad40c6d9d1c8。
基线：2897a2a73371227193719820abf598a7a0cfa995（#378）。
本次是同一助手的实现复审，不构成独立人员签署。没有合并或批准真实 D5。

## 决定

| 遗留项 | 决定 | 范围与依据 |
| --- | --- | --- |
| RR04 事务归属 | OPEN — 原归属缺口已修复，清理合同尚未完整满足 | public 入口固定 synthetic target，自建连接；coordinator/inspector package-private，无外部 Connection/DataSource/callback 入口。普通 SQL/runtime rollback/close 故障已建模，但本次发现 fatal rollback 会阻断 close；修复并补测后再关闭该项。 |
| RR05 恢复读取一致性 | CLOSED — synthetic MySQL 单查询范围 | canonical 与 application 由同一 SELECT 的 LEFT JOIN 读取；第二候选行识别 split identity；无记录、孤立 application、RECEIVED、历史 ACCEPTED、部分状态与身份冲突有明确分类。恢复读取不会重写原写入 UNKNOWN/ABORT 状态；read 与 cleanup 分别报告。 |
| RR06 输入身份校验 | CLOSED — synthetic 边界范围 | 五类身份在连接前验证固定前缀、ASCII 字符集合、非空与长度；digest/version/time/fault/target 亦验证。invalid 测试确认零连接；不是完整临床身份认证、租户隔离或真实 D5 授权。 |

D5P_AGGREGATE_VERIFICATION_CLOSURE = OPEN
RR05_RR06_BOUNDED_CLOSURE = CLOSED
RR04_BOUNDED_CLOSURE = OPEN

## 新发现 B01：fatal rollback 中断剩余清理

位置：U07D4SyntheticOwnedTransactionRunner.Session.finish()/rollback()，两个公开入口共享此路径。
finish 先调用 rollback，再调用 close；rollback 仅捕获 SQLException/RuntimeException。若 rollback 抛 Error，finish 提前退出，外层 finally 仅保留/传播 Error，并不尝试 close。

这不是要求 JVM Error 转换为普通结果或保证进程可恢复，而是已接受设计“finally 尽力清理并继续抛出”的遗漏：可执行的 close 尝试被控制流跳过。既有 FATAL 测试只在 body 抛 Error 且 rollback 正常，不覆盖 cleanup 自身的 Error。

复现：用 --release 8 编译未改动的受审 main 源码；反射构造私有 Session、设置 started=true 和 Connection 动态代理。代理 rollback 抛固定 AssertionError，close 仅计数。调用 finish 后得到：
```
U07_379_FATAL_ROLLBACK_REPRO=CONFIRMED rollback_attempts=1 close_attempts=0
```
该探针无需数据库，仅证明控制流；没有替代 MySQL 故障测试或改变受审代码。

最小修复要求：无论 rollback 抛普通异常或 Error，均尽力尝试 close；保留首个 fatal Error，后续清理错误按顺序附加且不得自 suppression；Error 继续传播。补覆盖 write/read cleanup fatal rollback，以及 body fatal + rollback fatal + close 故障组合，验证 close 被尝试、原 fatal 身份保留和次级错误顺序。不扩大生产入口、不改变恢复分类、不引入真实 D5。

## 精确 CI 证据

原始 job 日志本次重新读取。D3/D4 checkout 均为临时 merge a26acb321caa0fe376e31ba3a4d76339940ab7f9；其 tree 与受审 HEAD 已核对一致。以下为实际执行输出，不是脚本 echo 源码。

| 检查 | 证据 |
| --- | --- |
| D3 | run 38014711889 / job 114102317666；architecture PASS；JDBC smoke PASS assertions=18 |
| D4 | run 38014711885 / job 114102317677；architecture PASS public_entry=owned_only production_refs=0 recovery_selects=1 |
| 真正 JVM halt | PASS pre_rollback=1 post_commit_durable=1；pre-halt raw fixture 是设计明确的测试例外，post-halt 已经 runner commit+close |
| 恢复 smoke | PASS assertions=17 |
| adversarial | PASS assertions=74 races=10 scope=SAME_CONSULTATION_SERIALIZED_RACES |
| replay state | PASS assertions=75 invalid_states=7 |
| D5-P boundary | PASS assertions=183 scope=SYNTHETIC_MYSQL_DRIVER_FAULTS_AND_SINGLE_QUERY |
| 非冲突 worker 错误 | PASS nonzero_exit=1 |

D3：https://github.com/cxjchelsea/AIdoctor/actions/runs/38014711889
D4：https://github.com/cxjchelsea/AIdoctor/actions/runs/38014711885

本地本次复核：main --release 8 编译通过；architecture script PASS；fatal rollback 探针复现 B01。未在本地重新运行 MySQL 集成套件。

## 证据边界

并发 interleaving 测试复现旧双 SELECT 混合读取，并核对新单 SELECT 在提交前/后观测；不是对 SELECT 执行中点进行调度，也不是多实例证明。commit-before/after、rollback/close 注入为 driver 代理模型，不等价于真实数据库重启或网络分区。历史 ACCEPTED 不代表 APPLIED/consumer receipt。

本决定不关闭既有跨 consultation 竞争限制，不涵盖 Oracle、全项目 Maven/Spring、Foundation JPA bridge、生产接线、真实数据、F8/source/P01-G2/outbox receipt 或真实 D5。#379 保持 draft/unmerged。下一步仅需定向修复 B01、补故障证据，再复审 RR04；无须重开已关闭的 RR05/RR06，除非新修改触及其行为。
