# U07 D5-O1 隔离验证收口与集成就绪评估 v0.1

日期：2026-10-12。结论：ISOLATED_VERIFICATION=CLOSED_BOUNDED；ISOLATED_INTEGRATION_CANDIDATE_READINESS=READY_WITH_BOUNDED_SCOPE。允许下一步整理与验证隔离 main-based 候选，不等于合并授权。REAL_D5=NOT_READY；完整合同验收 NOT_FULLY_CLOSED。

## 精确对象与证据绑定

本决定只适用于 #416 HEAD ca9f0530681b4678ce74fc5802260bd044207c57，候选 tree 2b514fe83390d2c271c971ad545a13b522058699，固定 main750770e81af79b764cc4148d7fa856363c89972a。远程再次核验 #416 为 draft/open/unmerged，main未变。

实际 Actions checkout79c7547190281eb943c29a94bb763e2fe3371bcd，parents 精确为上述 main/head，tree 与候选一致。这个 GitHub PR test merge 是临时受测树，未写入 main；不能作合并后证据。

读取 #416 当前完整13文件差异列表、HEAD证据文档、三个最终run状态、四job日志，承接 #417旧HEAD审查及 #416修复后定向复审。本轮没有再修改实现或运行新测试，不把重复读取历史日志称新执行。执行环境当前不可用，本评估通过远程仓库证据完成；未声称本地检查或运行。

## 最终 HEAD 实测

| 范围 | run / job | 结果 |
| --- | --- | --- |
| 真实Java8/Spring JDBC/MySQL8 O1 | [38197291945](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197291945) / 114642690958 | 72 reader + 1 MVCC =73；0failure/error/skip；BUILD SUCCESS |
| U02 Java | [38197291963](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197291963) / 114642691098 | 全套434；0failure/error，1skip；BUILD SUCCESS |
| U02 Python | 同run / 114642691235 | 11+9+1 passed |
| U03 Java | [38197291959](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197291959) / 114642691134 | 全套434；0failure/error，1skip；BUILD SUCCESS |

三run completed/success，四日志均绑定79c7547。重复434不是新增独立覆盖；CD08临床参数harness明确skip，不算通过。原main的104 F8+25 Source=129属于此前合并后基线，未在O1本轮重跑，不能相加宣称本轮202新验证。

## 有限关闭矩阵

| 对象 | 已有证据与关闭范围 | 决定 |
| --- | --- | --- |
| O1-B01 / B01-R2 | P0..P10结构分类、当前损坏先于旧请求STALE、版本回退/overflow及已列状态关系；authority恒UNKNOWN，非MATCH payload为空 | CLOSED_BOUNDED |
| O1-B02 | 固定合成profile/URL、可信内部四元组TEST_SCOPE、input/scope/outer拒绝零I/O；不认证真实actor/action权限 | CLOSED_BOUNDED |
| O1-R01-R2 | lazy构造与前置拒绝、同请求非缓存preflight；真实schema/engine损坏拒绝 | CLOSED_BOUNDED |
| O1-I01（#417） | PRIMARY角色及列序独立校验；真实MySQL PK→同列UNIQUE拒绝，owner查询0，finally恢复后MATCH | CLOSED_BOUNDED |
| O1-R02（#417） | autoCommit异常报告代理：SQL/owner读0；根读后连接标识漂移代理：owner读仅根1次；无后续子读 | CLOSED_BOUNDED |
| SELECT-only / 无写副作用 | 实际账号DML/DDL拒绝，六表读前后不变；不读取患者内容，不接F8 finalize | CLOSED_BOUNDED |
| 单实例RR snapshot | 根普通SELECT后独立writer改根/thread并提交，旧reader保留旧MATCH、新reader看到NO_CURRENT_WAIT | CLOSED_BOUNDED |
| 所有物理故障/设计组合 | 未执行组合、连接中断/资源失败全矩阵、所有输入边界或schema变形不得由73项推定 | NOT_FULLY_CLOSED |

“有限关闭”只接受上述列明实现与证据；不宣称完整设计清单每个条件已穷举。autoCommit/连接漂移/错误隔离报告代理不等于真实数据库发生该故障。MySQL主键替换是在独立故障fixture临时进行，不授权删除生产约束。正常物理事务和MVCC证据与代理负向证据分开。

## 集成候选就绪

冻结切片为独立tools模块：一个pom、5 main类、2 test类、3资源、1 workflow、1证据文档，共13新增文件。远程差异核验全部added，范围未扩张。没有生产Bean/路由、生产包扫描、原DDL、root build、F8/Source实现修改。配置唯一NONPRODUCTION_SYNTHETIC与固定disposable URL；原V2→V3→V6源bytes仍由bootstrap digest/Git blob校验；reader只授六表SELECT。

本轮未发现阻断该冻结隔离切片整理候选的问题。候选可承接 #416精确实现和证据；无需把 #412/#415/#417或本决策整条审查分支合并进代码候选。评估文档独立保存，不能为了“证据齐全”引入不在13文件范围的生产或历史文档变化。

下一步候选整理验收：
1. 从最新main确认SHA；如仍750770e8，可保留已验证13文件树或创建内容等价main-based隔离候选，避免重复提交/改变业务行为。
2. 对候选重新列出精确13文件差异与blob/tree绑定；若main改变或内容改变，重新做相应差异审查与验证。
3. 候选精确HEAD完成O1真实数据库、既有受影响回归；旧F8/Source129若作为集成新证据则必须实际重跑，不能继承计为新执行。
4. 之后进行精确main-target差异复审和合并授权评估。此次没有授权standard merge或任何main写入。
5. 真正获授权合并后，另做actual main checkout的合并后验证。PR临时merge测试不得替代。

## 保持开放的真实业务门禁

生产tenant/actor/action CA、撤销epoch、delivery/eligibility issuer、question/pending/current CDP、durable APPLIED与Source/Canonical、生产Spring proxy/manager/EMF/DS拓扑、全部writer共同guard协议、outbox/network/crash/Oracle认证继续OPEN。现有单实例六表结构MATCH不承诺未来状态、不授权F8 finalize，不代表完整U07/D5关闭。

本次为同一助手的证据收口评估，不是独立人员批准。结论只允许推进隔离候选阶段；REAL_D5=NOT_READY，MERGE_AUTHORIZATION=NOT_GRANTED，main未变。
