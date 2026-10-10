# U07 D4 Post-Remediation Targeted Re-Review + Bounded Verification Closure Decision v0.2

日期：2026-10-10。前序：PR #372 v0.1（未关闭），修复：PR #373。
本文件替代 v0.1 对已修复 RR-01/02 和选择限定范围的 RR-03 的当前结论；历史报告保留。
本轮是同一执行助手重新审查实现与原始证据，不是独立人员/组织签字；不得标记独立 reviewer identity 已获得。如项目需要独立人员签核，该签核仍待完成。这里作出有限技术验证关闭决策，不授予合并。

## 1. 精确审查对象

- PR #373 HEAD：4a2ea4ebbc2e7359a2c8292895646d89ac113d1b。
- base #371：e825a8e6d9a3902856dd118b2f4e147451cbb37c。
- OPEN / DRAFT / UNMERGED，精确 diff 7 文件。
- 复读 coordinator、共享 classifier、两套 concurrency smoke、新 replay-state smoke、workflow、修复说明及继承 inspector/owned runner。
- 本地 HEAD 相同、工作区干净；git diff --check base HEAD PASS。
- CI checkout 为 0076fdefd0a5a053fa8eb6f202644925b5b1b1df；本地 git diff --exit-code HEAD 与该 merge commit 返回 0，源码树一致。本轮没有重新执行数据库测试，接受已完成 CI 原始日志证据。

## 2. 针对性判定

| Finding | 判定 | 依据及边界 |
| --- | --- | --- |
| RR-01 重放阶段 | CLOSED_IN_SYNTHETIC_SCOPE | 身份检查之后仅 ACCEPTED / ACCEPTED / revision 1 / NULL effect 返回正常历史 replay；其他状态明确异常并 rollback。7 类坏状态字段保持不变，合法历史 replay 在会话推进后仍成功。 |
| RR-02 SQL 分类 | CLOSED_FOR_CURRENT_HARNESSES | 两套 worker 共用 SQLState+MySQL vendor code allowlist，仅 23000/1062 或 40001/1213；非冲突异常收集后主线程失败，Throwable 也会传回。 |
| RR-03 竞态覆盖 | CLOSED_BY_EXPLICIT_SCOPE_LIMIT | 接受 SAME_CONSULTATION_SERIALIZED_RACES；没有跨会话插入窗口竞争验证，不宣称更广保证。 |
| RR-04 原公共 Connection 入口 | OPEN | facade 新连接测试不能证明原 API 不提交/回滚调用方其他写入。 |
| RR-05 READ_COMMITTED 双读取 | OPEN | inspector 两次查询不是同一稳定快照，可能保守返回瞬态 partial；禁止自动修复。 |
| RR-06 synthetic 身份约束 | OPEN | 写入 coordinator 的 consultation/question/wait 输入没有完整前缀约束；固定 disposable fixture 范围有效，不推广。 |

没有在本轮选定范围内发现新增关闭阻塞。RR-04/05/06 是显式排除范围的未解决项，不能标成已修复或全局关闭。

RR-02 证据强度说明：错误码反例执行共享 requireExpected 并确认原异常传播，真实 missing-table 错误执行分类校验；worker collector→主线程 AssertionError 路径经源代码审查。没有另行注入实际 worker 的数据库断连并证明整个 child process 非零退出，不声称这一更强的端到端故障注入已经执行。

## 3. 原始执行证据

本轮重新读取下面两份原始 job logs，确认实际输出和 checkout 来源：

- D4：https://github.com/cxjchelsea/AIdoctor/actions/runs/38011892199，job 114093520038，SUCCESS。
- D3：https://github.com/cxjchelsea/AIdoctor/actions/runs/38011892248，job 114093520022，SUCCESS。
- javac --release 8 编译步骤 PASS。
- U07_D4_REPLAY_STATE_SMOKE=PASS assertions=75 invalid_states=7 scope=SYNTHETIC_HISTORICAL_ACCEPTED_ONLY
- U07_D4_SYNTHETIC_RECOVERY=PASS assertions=17 concurrency_replay=1 explicit_db_conflict=0
- U07_D4_ADVERSARIAL_OWNER_SMOKE=PASS assertions=74 races=10 scope=SAME_CONSULTATION_SERIALIZED_RACES
- U07_D4_FORCED_JVM_TERMINATION=PASS pre_rollback=1 post_commit_durable=1
- U07_D3_SYNTHETIC_JDBC_SMOKE=PASS assertions=18

断言数量不是场景完整性或证明强度的替代。Runtime.halt 在已知提交前/后执行，不等于数据库服务崩溃或 commit 执行中网络中断。10 轮两个线程共用会话锁，不等于分布式多实例证明。V2/V3/V6/V7 单独建表不是完整 Flyway 部署成功。

## 4. 有限关闭决定

TARGETED_TECHNICAL_RE_REVIEW = PASS_WITH_EXPLICIT_EXCLUSIONS
D4_BOUNDED_SYNTHETIC_MYSQL_VERIFICATION = CLOSED
RR_01 = CLOSED_IN_SYNTHETIC_SCOPE
RR_02 = CLOSED_FOR_CURRENT_HARNESSES
RR_03 = CLOSED_BY_SAME_CONSULTATION_SCOPE_LIMIT
RR_04 / RR_05 / RR_06 = OPEN
SEPARATE_INDEPENDENT_REVIEWER_SIGNOFF = NOT_ESTABLISHED
D4_FULL_PRODUCTION_CLOSURE = NOT_READY
U07_FULL_UNIT_CLOSURE = NOT_ESTABLISHED
D5_IMPLEMENTATION_OR_ACTIVATION = NOT_AUTHORIZED_BY_THIS_DECISION
CLINICAL_F8 / PHI / PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED

关闭范围：disposable MySQL 8 + 固定 synthetic fixtures，owned runner 新连接入口、合法/不合法历史 replay、同会话两线程串行竞争、只读历史证据检查及本地 JVM 提交前后退出的既有测试。
关闭不包含：原任意 Connection API 全局事务归属、任意时间交错恢复读取、任意业务身份安全、跨会话/跨实例竞争、Oracle、APPLIED/outbox/真实 F8，以及完整部署与临床运行。

## 5. 后续工作边界

下一步建议：U07 D5 前置入口与恢复边界设计 + Implementation Readiness。
先明确 RR-04 的唯一事务 owner 及入口可达性、RR-05 的一致读取/不确定结果合同、RR-06 的 SQL 前全身份校验，并追溯正式 D5 spec 的实现范围与依赖；不得仅凭本关闭文件开始生产接线或真实 F8。

若后续仍只是 synthetic 原型，需明确哪些残留项本次要解决；若要扩大至正式入口，则相应残留项必须先关闭。PR #373 及其 stack 的 merge readiness/authorization 要单独评估，本文件不授权自动合并。
