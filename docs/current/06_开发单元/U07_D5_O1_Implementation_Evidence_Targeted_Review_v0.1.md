# U07 D5-O1 PR #416 实现与证据定向审查 v0.1

日期：2026-10-12。结论：REVISE_BEFORE_BOUNDED_CLOSURE；ISOLATED_INTEGRATION_READINESS=NOT_READY。本轮为同一助手的定向复审，不是独立人员批准；未修改 #416 实现、未合并。

## 精确对象与范围

- main：750770e81af79b764cc4148d7fa856363c89972a。
- #416 HEAD：0f365a3737e9b82fbff28b8c3c0acedeab1fb58b，draft、未合并。
- 当前 CI checkout：7fa7e82acfba5ebc17f958cabcb4aee1ad23d93b；parents 为上述 main/head，tree 为 1a0a98bb84fa40b68a962028d1a614a8e4f3d667，与候选一致。
- 合同：#412 v0.3（1b58f4d4f3fd7ffc4536d698f1fc05852f9031ae）及 #415 设计复审。审查 13 新增文件、main-target 范围、四个当前 HEAD job 日志及冻结设计；未认证部署中的生产 Spring 拓扑。
- tools-only 六 owner 表结构读回，固定 disposable synthetic MySQL。无生产源码/原迁移/root build/F8/Source 变更。

## Findings

### O1-I01 — 中优先级、阻断物理前提有限关闭：PRIMARY 与 UNIQUE 被混同

位置：OwnerReadbackResources.preflight、schema-manifest.json。STATISTICS 查询取得 INDEX_NAME 后，最终比较仅保留各 unique index 的列列表；manifest 的 unique 数组同样把 PRIMARY 放入普通唯一索引集合。索引名称/主键角色在比较前丢失。

反例：clinical_runtime_run 原 PRIMARY(run_id) 改为同列 UNIQUE fixture_pk_substitute(run_id)，保持列类型、NOT NULL、InnoDB、数据及其他索引不变。observed 列列表仍是 [[run_id]]，与 wanted 相等，preflight 不会因主键缺失拒绝。完整合法 fixture 因而仍可能 MATCH，违反 v0.3 §3.3 每请求检查冻结 PK/unique shape 的要求。此问题不证明现有正常 CI schema 错误，也不升级为生产权限漏洞。

本轮用 Python 对实际 manifest 和实现的降维比较构造了静态等价反例；没有执行这项真实 MySQL ALTER，不能把它报告为物理实测。

修复：manifest 独立记录有序 primary_key 与普通 unique keys；preflight 明确验证 PRIMARY 恰好存在且列序精确匹配，并独立比较普通唯一约束。无需要求所有普通唯一索引名称恒定。保留原 DDL。

回归：只在额外 disposable 故障测试中临时替换 PK 为同列 UNIQUE，预期 UNAVAILABLE/RESOURCE_PRECONDITION_FAILED、payload null、owner SELECT 为零；finally 恢复原 PK，后续读恢复 MATCH。该负向 schema 测试不得被描述为“允许生产删约束”或“重复 PK 正常实测”。

### O1-R02 — 中优先级、阻断验证全闭合：物理拒绝分支缺少定向回归

v0.3 §5 第3组要求 autoCommit/connection 漂移故障。现有测试覆盖报告隔离值错误、manager 与 DS 不匹配、外层事务、真实 engine/schema 变化，但未注入 autoCommit=true，也未触发 row() 的 CONNECTION_ID 漂移拒绝。正常 RR/MVCC 通过不能代替这些 fail-closed 分支验证。

补齐两项独立故障回归：在请求事务内报告 autoCommit=true；在 preflight/首次 owner 读后的后续业务读报告 connection ID 改变。均预期 UNAVAILABLE/RESOURCE_PRECONDITION_FAILED、payload null，断言不继续读取后续 owner。明确标注代理故障注入；正常真实数据库路径与之分开。不要以注入报告值声称真实连接发生漂移。

## 已核验当前 HEAD 证据

| 运行 | job | 核验结果 |
| --- | --- | --- |
| [O1 38195820126](https://github.com/cxjchelsea/AIdoctor/actions/runs/38195820126) | 114638095281 | Java8/Spring JDBC/MySQL8：69 reader + 1 MVCC = 70；0 failure/error/skip，BUILD SUCCESS |
| [U02 38195820121](https://github.com/cxjchelsea/AIdoctor/actions/runs/38195820121) | Java 114638095308 | 全套434；0 failure/error、1 skip，BUILD SUCCESS |
| 同 U02 | Python 114638095592 | 11+9+1 passed |
| [U03 38195820221](https://github.com/cxjchelsea/AIdoctor/actions/runs/38195820221) | 114638095517 | 全套434；0 failure/error、1 skip，BUILD SUCCESS |

三个 run completed/success；四 job 日志均核验当前 checkout。重复434不算独立增量覆盖；CD08临床参数 harness 的 skip 不算通过。129 F8/Source 是未变 main 的已有证据，本轮没有重新执行。上述绿灯不包含新发现的 PK 替换、autoCommit 或 connection 漂移反例。

scope/input/outer 前置检查零连接 SQL、TEST_SCOPE 与四元组绑定、固定 profile/URL、authority 恒 UNKNOWN、非 MATCH 无 payload、MATCH 最小不可修改结构结果、旧请求不能遮蔽当前损坏及版本回退，已由实现和列明测试支持。权限测试使用实际 reader 账号拒绝 DML/DDL；MVCC barrier 在根读后让独立 writer 提交，旧 reader MATCH、新 reader NO_CURRENT_WAIT。独立 writer 实测只认证 disposable 单实例快照，不认证生产共同 writer 协议。

## 有限关闭与就绪决定

| 对象 | 本轮决定 |
| --- | --- |
| O1-B01 / O1-B01-R2 分类与优先级 | 在列明工具分类/组合测试范围内 CLOSED_BOUNDED；不宣称完整生产状态枚举 |
| O1-B02 TEST_SCOPE / profile / URL 边界 | 在可信内部合成 fixture 范围内 CLOSED_BOUNDED；不关闭真实租户/actor/action CA |
| O1-R01-R2 lazy 构造与前置零 I/O | CLOSED_BOUNDED，限已测拒绝路径 |
| O1-R01 全部物理前提与验收 | OPEN：O1-I01 与 O1-R02 阻断 |
| 验证全面闭合 | NOT_FULLY_CLOSED |
| 隔离集成就绪 | NOT_READY，修复并复审后再评估 |
| REAL_D5 | NOT_READY |

生产 scope/action、delivery/eligibility、question/pending/current CDP、durable APPLIED/Source、writer 共同协议、outbox 均保持 OPEN。没有合并授权，不将本次工具测试转为真实 F8 finalize 权限。

下一步：仅在 #416 定向修复 O1-I01、补齐 O1-R02 和 PK 负向回归，重新运行真实 Spring/JDBC/MySQL 及既有受影响回归，再进行实现与证据定向复审。
