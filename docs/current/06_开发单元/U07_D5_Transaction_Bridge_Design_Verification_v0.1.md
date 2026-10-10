# U07 D5 事务桥接详细设计与隔离验证 v0.1

日期：2026-10-10。父基线 #382 `2c58da39bc35efae4c2e06e2e74042f8f2450e4c`。
本轮交付测试专用桥接配置与真实 Spring/MySQL 验证，不建立真实 U07 application service。

## 事务合同

- 候选 owner：单个 Spring `JpaTransactionManager`，绑定一个 EntityManagerFactory 与同一 DataSource。Spring `TransactionTemplate` 是 harness 的唯一提交/回滚 owner。
- Foundation 原 `CanonicalBusinessEventLedger` 保留 canonical 身份职责；JPA entity/repository 与 U07EventApplicationRepository 均直接编译仓库源码。
- JDBC 经 `DataSourceUtils.getConnection/releaseConnection` 获取事务绑定连接，不调用 commit/rollback/close，不使用 D5-P DriverManager runner。JPA 与 JDBC `SELECT CONNECTION_ID()` 相等、autoCommit=false、resource-bound 与真实 AOP proxy 是必要运行证据。
- harness admission 在任何写入前验证 actual transaction / DataSource resource / EntityManager joined。无 transaction 或 wrong DataSource 均 fail-closed。
- 测试外层 isolation 明确 READ_COMMITTED、timeout=12s。U06 establish 加入外层已有事务；其 SERIALIZABLE 注解不将已有事务升级。本测试不证明真实 U06 standalone SERIALIZABLE 入口已部署，也不冻结生产 isolation。
- 测试候选顺序 consultation lock → canonical resolve/write → application；U06 establish 同样先锁 consultation。#360 canonical-first proposal 不在本轮冻结，不能声称所有生产路径统一；只验证这两个 harness 路径的竞争。
- JPA unique constraint/flush 错误后的 transaction 必须退出；不在 rollback-only session 中读 winner并继续写。winner readback 是失败事务退出后的新 TransactionTemplate 事务。
- 历史 same-key winner 不自动授权另一 consultation：新事务读 winner 后仍需 canonical scope匹配；错 scope由现有 ledger拒绝。
- 仅 RECEIVED rows；不调用 ACCEPTED/APPLIED、outbox insert、consumer、真实 source/F8/P01/G2 或 runtime。

## 具体配置与最小修复

`tools/u07_d5_tx_bridge/pom.xml` 与 diagnosis-service 同用 Spring Boot 2.7.8，编译选定的真实 Foundation/U01/U06 wait/U07 repository 源码。最小 AnnotationConfigApplicationContext 显式注册 JPA、仓储和两个 service，禁止 Boot application scan；Hibernate ddl=none，schema 来自原 V2/V3/V6/V7。

唯一允许 URL：127.0.0.1:33322/u07_d5_bridge 固定参数。工作流新建 disposable MySQL 8，不接用户数据库。测试没有真实医学内容或外部 endpoint。

实物发现：ConsultationWaitTransitionService 是 final 且没有 interface，使用 @Transactional 时无法建立所需 class proxy。本轮只去掉该类的 final；不添加 @Service、不新增生产 wiring、不改业务方法。测试实际 assert两个 service为 AOP proxy。

## 验证矩阵

| 测试 | 断言 |
| --- | --- |
| 实际代理/物理连接绑定 | ledger/wait service AOP proxy；JPA/JDBC CONNECTION_ID 相等；active/joined/bound |
| JPA→JDBC→body异常 | canonical/application 两表均回滚 |
| JDBC→JPA flush唯一冲突 | application与失败canonical均不落库；既有winner保留；新事务读winner |
| commit后响应丢失 | fresh JDBC两表匹配且phase=RECEIVED；不推导APPLIED |
| 无事务/错DataSource | guard在写入前拒绝，零event/application |
| flush错误/rollback-only | JPA异常不可被正常commit掩盖；失败事务退出后新事务读winner |
| 跨consultation同key真实竞争 | barrier使两份不同consultation事务先完成ledger resolve，再并发flush；一个winner一个精确1062/23000 loser；失败application回滚；错scope拒绝 |
| U06 wait 与桥接竞争 | 实际wait proxy持有consultation锁；另一事务阻塞；释放后两者完成；线程错误/超时不掩盖 |

两个竞争用例为确定调度的最小覆盖，不是多实例、所有锁序或长时压力证明。response-loss为提交返回后模拟；不等于真实commit网络分区。五项既有fatal cleanup证据仍以#381为准。

## 执行与证据范围

本地没有 mvn/docker；本地只检查XML/YAML与diff。本轮工作流执行：
1. shared-contracts install + diagnosis-service全模块mvn test，记录原始日志；失败保留为独立限制，不因最小harness成功改写成PASS。
2. 最小harness真实mvn test，对真实Spring/JPA/MySQL运行8项JUnit测试，任一失败使job失败。
3. 保存Surefire reports与全模块日志；只有Maven test成功后打印PASS marker。

提交时 CI 待执行；最终结论必须引用该PR精确HEAD、CI原始日志及checkout tree一致性。后续验证评论可补充实测结果，不将初始pending文字当成结果。

本工作不修改已有migration、不直接修复Foundation ledger的flush/重试语义，也不产生生产事务桥接入口。观察到的Foundation异常行为决定下一段的最小修复；无须提前重写生产层。

REAL_D5_IMPLEMENTATION_READINESS = NOT_READY
PRODUCTION_BRIDGE_WIRING / REAL_AUTHORITY / APPLY / DISPATCH / MERGE = NOT_AUTHORIZED
ISOLATED_BRIDGE_VERIFICATION = PENDING_EXACT_HEAD_CI
