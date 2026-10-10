# U07 D4 重放状态与验证测试定向修复 v0.1

日期：2026-10-10。来源：PR #372 对 PR #371 e825a8e6d9a3902856dd118b2f4e147451cbb37c 的复审。

## 改动与合同

- RR-01：D3 SAME_EVENT_REPLAY 限定为身份匹配且 phase=ACCEPTED、decision=ACCEPTED、row_version=1、effect_id=NULL 的合成历史状态。RECEIVED、错误 decision/version/phase、已绑定 effect 返回 U07_D3_REPLAY_STATE_REQUIRES_REVIEW 并 rollback；缺失 application 仍返回 U07_D3_RECONCILIATION_REQUIRED。不自动修复，不重做接受。
- RR-02：两套并发 smoke 共享测试专用 U07D4MysqlConflictClassifier。仅 SQLState=23000/vendor=1062（duplicate key），或 SQLState=40001/vendor=1213（deadlock）作为允许的数据库竞争失败；其他 SQLException 原样传播到 worker failure collector，主线程抛 AssertionError。未新增自动 retry。
- RR-03：选择本轮仅关闭 SAME_CONSULTATION_SERIALIZED_RACES 范围。10 轮使用同一个 consultation 行锁；不声称跨会话唯一键插入竞争、多进程或多实例证明。新增输出明确这一范围。

## 新增回归证据要求

新增 disposable MySQL JDBC smoke：7 类坏状态（RECEIVED、错误 decision、NULL decision、错误 version、APPLIED、已绑定 effect、缺失 application）分别验证明确拒绝、inspector 不提升为接受、拒绝后原行字段不变。合法接受后推进 consultation，再验证成功历史重放及 inspector 一致。

共享 classifier 的成功/失败错误码对、真实 missing-table SQL 错误及原异常传播反例由同一 smoke 检验。D4 workflow 编译所有新源文件并在既有回滚、强制 JVM 退出、10 轮同会话竞态之后执行新增 smoke；既有 D3 workflow 保持回归。

实施完成不等于执行 PASS。必须核验新提交原始 CI 日志及 checkout 来源后接受证据；历史报告 #372 保留原判断，不覆盖历史。新的 bounded closure 仍需后续复审决策。

## 明确保留的未关闭范围

RR-04 原 D3 Connection 公共入口事务归属；RR-05 READ_COMMITTED 双查询恢复快照；RR-06 全身份 synthetic 输入约束。本轮不修改这些合同，不声称它们已解决。

Oracle、数据库服务崩溃、commit 执行中真实网络断连、完整 Flyway/部署历史、真实 F8、APPLIED/outbox、临床与生产启用都未覆盖。没有合并任何 PR，本改动不授予合并或 D5 激活权限。
