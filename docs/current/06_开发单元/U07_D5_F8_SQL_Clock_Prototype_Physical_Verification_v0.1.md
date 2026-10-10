# U07 D5 F8 — Isolated SQL/Clock Prototype + Physical Verification v0.1

## 范围与固定输入

基线 main `835441492d130de320bf3d49fe2b4be598a2b327`。设计输入为 PR #394 `a941815cfe325bacb32089c6a106a221b273969e` §9–12，定向复审 PR #396 `4d24c1f3fc7c41c588bda89b7c18f08ac4c06114`。

仅新增独立 Maven 测试模块、MySQL synthetic schema、条件 SQL、CI。无生产 Bean、Foundation/Source 修改、患者数据、真实 owner、APPLIED 写入、Oracle 或 P01 接线。这里是物理结构原型，不是完整 F8 adapter；没有实现21字段 canonical/source认证、历史 reader、外层入口权限或完整 typed result validator。

## 可复核机制

最终 INSERT SELECT 直接读取 owner/claim/original decision/applied issuer 表。公共条件仅约束 synthetic scope、authority_complete、观察版本、原winner关系完整性；current wait清空、terminal、既有winner不被共同 WHERE 排除。应用证据必须具有完整 scope/wait/digest/generation/issuer 关联，不接受请求布尔值。authority_complete是独立fixture的聚合证据存在标记，并非真实owner验证器。

P1等价APPLIED优先于截止/terminal/moved wait；P2到期；P3terminal；P4mismatch；P5pending winner零行；P6不同APPLIED拒绝；P7合法无winner接受；P8缺交付证明零行。固定时钟测试逐项独立期望，不运行一份Java政策实现生成SQL期望。

同一SQL所有时钟位置使用 UTC_TIMESTAMP(6)，并存储 sampled_at / predicate_at。真实MySQL长事务用SLEEP推进至deadline后验证EXPIRED和statement一致；固定CAST时钟单独测试 equality 与一微秒前。真实环境日志记录server version/timezone；不宣称微秒准确度、与外部UTC同步或Oracle等价。

使用Spring DataSourceTransactionManager + SERIALIZABLE，消费者JDBC在同一connection-bound事务中运行。真实Foundation JPA资源映射继续沿用既有Source证据，本模块没有重证JPA。消费者无owner/applied UPDATE/INSERT权限、无decision DELETE/UPDATE。root仅用于独立fixture和owner侧锁测试，不代表生产owner权限模型。

owner和F8均先SELECT guard FOR UPDATE。两种顺序由latch协调，验证后一方在释放前未获得guard；owner先行后F8读到新的version/terminal。F8先行场景只证明锁串行及回滚后owner可更新，不宣称真实APPLIED竞争已验证。独立连接检测应拒绝RESOURCE_MISMATCH；该检测是原型断言，不是生产入口实现。

原子性wrapper：首次DML前标记attempt；新sentinel只用于P7候选；decision后、claim后故障由Spring回滚全部；final SQL零行或sentinel存在且最终EXPIRED时回滚。随后同identity负向重试不建sentinel。完整own claim CAS 0→1；原合法空sentinel可完成。数据库commit/rollback异常传播，不伪造NOT_COMMITTED。真实network commit-unknown、进程crash及完整typed异常映射尚未验证。

原始P7政策探针在事务内断言ACCEPTED后回滚，禁止提交缺claim的ACCEPTED。成功提交仅经过decision+claim原子wrapper。

## 执行证据与判定

CI待执行。本文件将按精确HEAD的真实编译/执行结果更新；不得把测试源码或workflow存在视为PASS。

物理验证通过后也只支持隔离SQL/clock原型范围，完整F8实施就绪仍需canonical/source同事务接入、完整Tier0/verdict-specific历史读回、完整结果validator及异常边界。真实D5仍NOT_READY，真实U06/U15/APPLIED/P01 CA不关闭，不授权合并。
