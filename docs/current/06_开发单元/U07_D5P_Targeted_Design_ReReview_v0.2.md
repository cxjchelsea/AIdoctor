# U07 D5-P Targeted Design Re-Review v0.2

日期：2026-10-10。被审PR #377 HEAD d247494797235fc1abe1eab6df18eb230c492903，base #376 f9ac475b7553d017dda6e67d2b6cdf4ea36ad8a6。
精确diff：1个设计文件，内容版本v0.2（路径保留v0.1以维护历史）。OPEN/DRAFT/unmerged。
本轮读回完整修订，按原PR #376的B01、R01–R03逐项重审，未运行代码/数据库。为同一助手技术复审，不是独立人员/组织签名。

## 1. 复审决定

TARGETED_DESIGN_RE_REVIEW = PASS_IN_BOUNDED_SYNTHETIC_SCOPE
DESIGN_FINDINGS_B01_R01_R02_R03 = CLOSED_AT_DESIGN_LEVEL
D5P_SYNTHETIC_MYSQL_IMPLEMENTATION_READINESS = READY
RR04_RR05_RR06_IMPLEMENTATION_CLOSURE = OPEN
IMPLEMENTATION_TEST_EVIDENCE = NOT_YET_AVAILABLE
SEPARATE_INDEPENDENT_REVIEWER_SIGNOFF = NOT_ESTABLISHED
REAL_D5 / CLINICAL / PHI / PRODUCTION / MERGE = NOT_AUTHORIZED

设计READY不是已实现PASS，也不自动合并任何stack。允许作为下一项合成实现的具体输入；本次用户要求为复审，故本轮不开始实现。

## 2. 原finding逐项判定

| Finding | 设计判定 | 收口证据 | 实现验收 |
| --- | --- | --- | --- |
| B01 rollback失败无准确状态 | CLOSED_DESIGN | §10.1/10.2新增ABORT_UNCONFIRMED；rollback正常才ROLLED_BACK；commit进入异常始终UNKNOWN，cleanup不消除未知 | rollback失败、rollback+close失败、config失败和错误优先级代理测试 |
| R01 inspector/production可达性 | CLOSED_DESIGN | §3/10.3同时内部化coordinator和inspector Connection入口；正式业务禁止synthetic引用，公共runner仅固定target | public API/构造器/nested types及正式引用扫描；0公开callback/factory |
| R02响应/清理/读取混合 | CLOSED_DESIGN | §5/10.1/10.2固定WriteResult/RecoveryResult；response、cleanup、transaction/read分离；read成功但cleanup失败保留观察，未知commit无普通business成功 | 复合失败矩阵；消费者成功条件；observation存在条件及primary/suppressed |
| R03退出fixture与调用迁移 | CLOSED_DESIGN | §10.4明确pre直接test-only双行fixture、post通过owner返回后halt；完整调用迁移、固定profile、src/test driver代理 | pre/post fresh读取、全部旧smoke迁移、代理隔离与新worker故障进程失败 |

这些CLOSED仅指设计缺口已具备明确实施合同，不表示RR04/05/06实际修复完成。

## 3. 交叉一致性检查

1. 无证据升级：commit异常后的UNKNOWN保留；fresh NO_DURABLE_EVENT不触发写重试；历史ACCEPTED观察不创造APPLIED/F8/恢复权限。
2. 单语句查询：请求anchor保留orphan，canonical双候选拒绝split；同次读取所有字段。§10.5固定身份、存在性、effect及phase分类优先级。
3. 新分类差异：effect非NULL从旧identity conflict改为partial，修订已明确声明；只是合成查询分类调整，fail-closed权限不变，测试须更新。
4. 事务终结：body/repository不提交，runner唯一终结产品synthetic执行事务；测试fixture自建事务为列举例外，不夸称所有test代码只能runner开连接。
5. 结果安全：operationStatus=FAILED或SUPPRESSED或cleanup失败不得正常成功投递，即使COMMITTED/businessOutcome元数据可保留。
6. 输入/目标：public执行恢复连接前校验；固定target配对及URL参数生成，不接受任意URL/Connection/DataSource/callback。前缀不冒充认证。
7. Oracle与真实D5仍排除；原D5定义不被前置修复替代。数据库文档仅支持设计，不当作实测证明。

未发现新的bounded implementation blocker。

## 4. 实现时必须遵守的细节（非新设计阻塞）

- 结果对象不可变；cleanupErrors防御性复制。primaryFailure保留原cause但不得序列化/日志输出凭据或完整连接配置。
- runner构造不得建立连接；target/凭据不合法时仍由execute/inspectFresh按固定typed失败合同拒绝。不要因测试方便新增public factory或可执行参数。
- 设置isolation或autoCommit抛异常后禁止business SQL；若连接状态无法确认，清理保守标失败，不能猜成功rollback。
- test JDBC driver注册/真实driver加载顺序必须可控，测试先确认代理实际截获连接，再接受0连接/故障计数；执行后注销代理，避免其他测试串扰。
- split候选必须完整枚举，不能读到第一条匹配就提前成功。查询计数指业务证据SELECT，不包括连接管理SQL。
- §10.4旧异常断言迁移至typed结果，worker不能只按SQL错误码计数而忽略transaction UNKNOWN/cleanup FAILED。
- 75/17/74/18是旧源码证据；新实现全部测试需在新精确HEAD执行，不拿旧数量冒充新PASS。
- prefixture仍只证明uncommitted双行在JVM退出后消失；post在owner返回后执行，不声称commit-close窗口或DB重启。
- 查询执行中并发snapshot交错没有新证据前不声称已验证；先完成规定的commit前/后可控顺序和单SELECT结构证据。

## 5. 下一步实施范围

D5-P Synthetic Entry / Recovery Boundary Implementation：
- P1：移除public Connection执行/读取入口；runner统一事务终结和typed结果。
- P2：anchored单SELECT及分类；明确UNKNOWN/cleanup/read结果。
- P3：连接前全身份验证、固定test target；迁移全部调用及src/test fault工具。
- 真实CI：Java8编译、D3/D4 disposable MySQL smoke、原7坏状态/合法历史replay、JVM halt、可控查询/故障矩阵、worker非冲突失败与架构约束。

可以在一个隔离实现PR中顺序完成上述内容，不再添加无关设计门禁。新实现接受测试后再做实现复审及RR04/05/06关闭决定。
不得修改Foundation JPA事务、U06/P01/G2/真实F8、migrations或生产Spring wiring；不得dispatch/outbox apply或接真实患者。合并另行评估。
