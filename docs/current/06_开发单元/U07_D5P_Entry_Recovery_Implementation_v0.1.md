# U07 D5-P Entry / Recovery Boundary Implementation v0.1

2026-10-10。基于 #378 `2897a2a73371227193719820abf598a7a0cfa995`，设计 #377 v0.2。

本实施仅限固定 disposable MySQL synthetic fixtures，无生产Spring注册、Foundation JPA/U06/F8修改、迁移修改、真实effect/outbox/APPLIED或真实患者操作。所有既有stack仍未合并。

## 实施结果

- D3 coordinator和D4 inspector class/Connection方法内部化，public runner只接受固定TestTarget及typed输入，不接受Connection/DataSource/callback/任意URL。
- coordinator不再commit/rollback。runner唯一管理产品合成执行事务；pre-halt测试直接双行fixture是明确例外。
- WriteResult/RecoveryResult固定返回合同；事务、响应、清理和读取事实分离。commit entered异常永远UNKNOWN；rollback失败ABORT_UNCONFIRMED；read失败UNAVAILABLE而不是NO_DURABLE_EVENT。成功证据不会覆盖旧UNKNOWN。
- 存在应用effect时返回PARTIAL_OR_INCONSISTENT，按设计细化旧分类；仍fail-closed。
- 恢复使用anchored single SELECT，canonical双身份与application同次读取；orphan/split完整处理。
- 输入validator在execute/inspectFresh连接前校验全部身份；配置固定D3/D4目标。timestamp执行前防御复制。
- 所有旧D3/D4调用迁移。测试adapter只有确定终结且cleanup完整时抛出供worker分类的原异常；UNKNOWN/cleanup失败绝不算普通冲突。
- 结果errors列表防御复制且不可修改；Throwable保留本地cause供诊断，不日志/序列化凭据。

## 验证与适用范围

本地已运行Java编译器模块 `java com.sun.tools.javac.Main --release 8` 编译全部U07主/测试源，以及boundary architecture脚本和git diff --check。这里编译器在JDK模块可用，即使PATH没有javac可执行文件。

数据库测试通过隔离CI执行，实施提交时仍为PENDING，必须以原始新HEAD job日志为证据：D3回归、D4历史replay/七类无效状态、17条恢复行为、10轮同会话竞争、JVM pre/post halt、新boundary faults和非冲突worker非零退出。

新增src/test注册driver代理：委托真实MySQL driver，针对body/commit/rollback/query/close注入异常并计数；主线程+两次固定快照读取复现旧READ_COMMITTED双SELECT跨commit混合情况。新查询commit前/后各一条证据SELECT，未声称statement执行中断点、真实网络partition、数据库服务器重启或跨实例证明。

代理测试delegate commit前/后抛异常是模型化故障；fresh read显示无记录/历史接受，但原WriteResult保持UNKNOWN。rollback/close故障保持primary及ordered secondary。真正fatal Error通过finally清理后继续抛出。driver测试完成注销并恢复原driver。

## 下一步

先接受新精确HEAD CI证据，再进行实现复审及RR04/05/06有限关闭判断。设计READY不是代码PASS，代码测试PASS不是授权真实D5或合并。

Oracle、生产JPA/JDBC桥接、真实D5 authority/source/F8/P01-G2/receipt/target绑定及完整Flyway部署仍未就绪。
