# U07 D5 F8 — Isolated SQL/Clock Prototype + Physical Verification v0.1

## 范围与固定输入

基线 main `835441492d130de320bf3d49fe2b4be598a2b327`。设计输入为 PR #394 `a941815cfe325bacb32089c6a106a221b273969e` §9–12，定向复审 PR #396 `4d24c1f3fc7c41c588bda89b7c18f08ac4c06114`。

仅新增独立 Maven 测试模块、MySQL synthetic schema、条件 SQL、CI。无生产 Bean、Foundation/Source 修改、患者数据、真实 owner、APPLIED 写入、Oracle 或 P01 接线。这里是物理结构原型，不是完整 F8 adapter；没有实现21字段 canonical/source认证、历史 reader、外层入口权限或完整 typed result validator。

## 可复核机制

最终 INSERT SELECT 直接读取 owner/claim/original decision/applied issuer 表。公共条件仅约束 synthetic scope、authority_complete、观察版本、原winner关系完整性；current wait清空、terminal、既有winner不被共同 WHERE 排除。应用证据必须具有完整 scope/wait/digest/generation/issuer 关联，不接受请求布尔值。authority_complete是独立fixture的聚合证据存在标记，并非真实owner验证器。

P1等价APPLIED优先于截止/terminal/moved wait；P2到期；P3terminal；P4mismatch；P5pending winner零行；P6不同APPLIED拒绝；P7合法无winner接受；P8缺交付证明零行。固定时钟测试逐项独立期望，不运行一份Java政策实现生成SQL期望。

同一SQL所有时钟位置使用 UTC_TIMESTAMP(6)，并存储 sampled_at / predicate_at。真实MySQL长事务用SLEEP推进至deadline后验证EXPIRED和statement一致；固定CAST时钟单独测试 equality 与一微秒前。真实环境日志记录server version/timezone；不宣称微秒准确度、与外部UTC同步或Oracle等价。

使用Spring DataSourceTransactionManager + SERIALIZABLE，消费者JDBC在同一connection-bound事务中运行。真实Foundation JPA资源映射继续沿用既有Source证据，本模块没有重证JPA。消费者无owner/applied UPDATE/INSERT权限、无decision DELETE/UPDATE。仅guard.lock_token获得UPDATE以满足FOR UPDATE权限；guard.version仍不可写。root仅用于独立fixture和owner侧锁测试，不代表生产owner权限模型。

owner和F8均先SELECT guard FOR UPDATE。两种顺序由latch协调，验证后一方在释放前未获得guard；owner先行后F8读到新的version/terminal。F8先行场景只证明锁串行及回滚后owner可更新，不宣称真实APPLIED竞争已验证。独立连接检测应拒绝RESOURCE_MISMATCH；该检测是原型断言，不是生产入口实现。

原子性wrapper：首次DML前标记attempt；新sentinel只用于P7候选；decision后、claim后故障由Spring回滚全部；final SQL零行或sentinel存在且最终EXPIRED时回滚。随后同identity负向重试不建sentinel。完整own claim CAS 0→1；原合法空sentinel可完成。数据库commit/rollback异常传播，不伪造NOT_COMMITTED。真实network commit-unknown、进程crash及完整typed异常映射尚未验证。

原始P7政策探针在事务内断言ACCEPTED后回滚，禁止提交缺claim的ACCEPTED。成功提交仅经过decision+claim原子wrapper。

## 执行证据与判定

代码测试 HEAD `da39a33d949efdc17f400809a0e42d1c50a22333`；[run 38029560782](https://github.com/cxjchelsea/AIdoctor/actions/runs/38029560782)，job `114147443910`，Java 8真实编译 + MySQL 8.0.46：**25 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS**。session/global timezone均SYSTEM，clock使用UTC_TIMESTAMP(6)，日志样本 `2026-10-10T06:03:23.924851`。这不是主库UTC同步认证。

| 实际执行组 | 数量 | 证据范围 |
| --- | ---: | --- |
| P1–P8独立矩阵 | 8 | moved/cleared wait + P1；带pending winner的P2；P5/P8零行 |
| 缺authority、version改变、损坏APPLIED/claim | 4 | fail closed / 零行 |
| 固定equality/微秒前、真实长事务clock | 2 | fixture边界与真实语句采样分开 |
| connection guard、旧空sentinel、只读pending | 3 | 原型资源检查与合法fixture |
| 成功原子提交、两点故障、零行回滚、跨期再试 | 5 | decision/claim/sentinel原子性 |
| 权限拒绝、共同锁两种顺序 | 3 | owner/APPLIED/guard-version拒绝及串行 |

首轮 `f2799f0f...`（run38027620773）实际失败：FOR UPDATE缺权限。修订只授权guard.lock_token UPDATE，guard.version与owner/APPLIED仍拒绝修改；第二轮 `7f66ee9d...`（run38029471136）24通过、1个DATETIME映射ClassCast测试错误。最终改显式Timestamp读取后25通过。失败证据保留，不隐去。

本证据修订仅改文档，测试代码与上述HEAD一致。Source/Binding源码及workflow未变，独立reference_vectors.py --check通过；未冒称本轮重新执行Source25例。UTC_TIMESTAMP同语句一致性也核对[MySQL 8.0官方时钟说明](https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html)，但实测才是这里的执行证据。

判定：**BOUNDED_PROTOTYPE_VERIFIED / READY_FOR_TARGETED_IMPLEMENTATION_AND_EVIDENCE_REVIEW**。不是完整物理等价或实施就绪关闭：当前只证明上述聚合synthetic owner模型。完整question/pending/issuance/current action、canonical/source绑定、全结果矩阵及verdict读回尚未接入。

物理验证通过后也只支持隔离SQL/clock原型范围，完整F8实施就绪仍需canonical/source同事务接入、完整Tier0/verdict-specific历史读回、完整结果validator及异常边界。真实D5仍NOT_READY，真实U06/U15/APPLIED/P01 CA不关闭，不授权合并。
