# U07 D5-O1 隔离读回实现与证据 v0.1

固定基线main750770e81af79b764cc4148d7fa856363c89972a。设计#412 v0.3（1b58f4d4f3fd7ffc4536d698f1fc05852f9031ae），设计复审#415（599fa7fb6dff9c8c01f18459ef1b1dcb8cbd88a0）。只读结构工具，不是生产Owner CA。

## 已实现范围
按13文件清单：独立pom、5 main Java类、2 test类、3 test资源、1 workflow、本证据文档。没有生产Bean、路由、原迁移/F8实现或root build修改，没有JPA/生产源码编译输入。Jackson仅解析固定manifest/fixture资源。
固定NONPRODUCTION_SYNTHETIC与127.0.0.1:33325/u07_owner_readback。lazy资源构造无连接；input/scope/outer检查后才建Spring JDBC RR只读事务；每请求同bound physical connection观察MySQL/engine/shape/隔离，owner查询前preflight无缓存。P0..P10硬关联与版本检查先于STALE；authority恒UNKNOWN，仅MATCH有最小不可修改payload，不读取user_id/problem/subject/答案/CDP JSON。
原V2→V3→V6 bytes受SHA256/Git blob校验，原PK/unique保持，六表InnoDB；没有f8_owner镜像。V6原附带delivery/trace表不向reader授权。root仅准备合成fixture、独立writer仅fixture UPDATE；reader仅六表SELECT。

## 精确代码与真实执行
受测代码HEAD4cbbd46ef38c685a851371740035d3d0c8ac9214；实际PR test merge checkout f515e16ca33b935fe2f2ffa618ee13836d54f84b，parents精确main/head，treeae66e3b3a64ff581723128d33f1c577e50ba73ba与代码HEAD一致。初始f7dbcd9c首轮64项绿灯是历史；随后对齐未知run状态reason并补旧对象scope/invalid输入回归，当前70项。不是合并后main证据。
- [O1 38195647655](https://github.com/cxjchelsea/AIdoctor/actions/runs/38195647655)，job114637555271：WaitChainReaderTest69+SnapshotConcurrencyTest1=70，failure/error/skip均0，BUILD SUCCESS。实际Java8、Spring JDBC、MySQL8 disposable。
- [U02 38195647638](https://github.com/cxjchelsea/AIdoctor/actions/runs/38195647638)：Java114637555382编译/定向/全套434，0failure/error、1skip，BUILD SUCCESS；Python114637555399为11+9+1 passed。
- [U03 38195647710](https://github.com/cxjchelsea/AIdoctor/actions/runs/38195647710)，job114637555580：编译/定向/全套434，0failure/error、1skip，BUILD SUCCESS。
三个run最终completed/success；四job日志读取核验。重复434不计独立增量覆盖；CD08临床显式参数harness skip不算通过。129 F8/Source基线来自main750770e8先前合并后证据，未在本轮重新执行。

## 实测含义与限度
矩阵测试包含缺根/缺关联/状态/版本/refs，旧请求叠加当前损坏、版本回退优先、旧对象scope、scope证据每字段、构造至拒绝零connection/SQL、profile/URL、outer事务、错误manager DS、真实schema额外列/非InnoDB拒绝、实际SELECT-only DML/DDL拒绝及六表读前后不变。
MVCC barrier在首次owner SELECT后放行独立writer改根/thread并COMMIT；旧reader仍MATCH且旧根版本，新readerNO_CURRENT_WAIT。无sleep猜测，不以metadata或同ID代替快照实测。
隔离级别不符用报告值connection proxy故障注入，非真实数据库切换；实际RR和同连接由正常物理路径和MVCC用例提供证据。多行重复PK未做，因为原约束保持。全部合成数据，未访问生产/患者。
本地bootstrap输出/Python语法/Source golden及diff whitespace成功；无本地Maven/MySQL/docker，Java结果只来自Actions。
本记录是代码验证后的仅文档提交；不把文档SHA冒充上述代码SHA。测试代码blob保持不变；若其后自动CI再运行，最终head结果记录PR body，不为重复写结果形成无限提交。

## 状态与下一步
ISOLATED_IMPLEMENTED_AND_PHYSICALLY_VERIFIED_IN_LISTED_SCOPE。待实现与证据定向审查，不声称独立人员批准、验证全部闭合或merge授权。没有合并。
真实scope/action、delivery/eligibility、question/pending/current CDP、APPLIED/Source、writer共同协议及outbox门禁均OPEN；REAL_D5=NOT_READY。
下一步针对#416 reader实现、scope/资源夹具、manifest权限和MVCC证据做定向审查，再决定有限验证收口及集成就绪。

## #417 定向修复与测试复审（2026-10-12）

复审所指旧 HEAD 为 0f365a3737e9b82fbff28b8c3c0acedeab1fb58b。本次受测代码 HEAD 为 4683195864c07c978d7d5d657ec33ab3c1c2c05f（修复提交 e8544988d7b19f618662f8c52aebc94f136b82c1，随后兼容两种 JDBC statement 故障注入路径）。本次仅改资源 preflight、schema manifest、reader 测试；保留原 DDL、所有原13文件范围及生产隔离边界。

- O1-I01：manifest 将有序 primary_key 与普通 unique keys 分开；preflight 从实际索引集合取出 PRIMARY，验证存在且列序精确相等，再独立比较普通 unique 列集合。普通 unique 索引名称不作为冻结角色。六表主键和原唯一约束逐项核对。
- 新增真实 MySQL 主键负向回归：正常 MATCH 后，临时将 clinical_runtime_run PRIMARY(run_id) 替换为同列 UNIQUE，预期 UNAVAILABLE/RESOURCE_PRECONDITION_FAILED、payload null，owner SELECT 计数0；finally 恢复原主键，下一次请求恢复 MATCH。仅限 disposable 故障 fixture，不修改原迁移，不将删约束变为正常 writer 协议。
- O1-R02：新增报告 autoCommit=true 的 connection proxy 回归，断言拒绝且SQL/owner读计数0；新增根读后报告 CONNECTION_ID 改变的 ResultSet proxy 回归，断言拒绝、payload null、owner读只有根1次，不继续子查询。故障注入支持 Statement/PreparedStatement；这是报告值注入，并非真实 autoCommit 切换或连接漂移的认证。

实际 checkout 4e410949fde306dc6ce430a32924b31bda8f2ff4，parents 精确为 main750770e81af79b764cc4148d7fa856363c89972a 与代码HEAD4683195864c07c978d7d5d657ec33ab3c1c2c05f，tree 95af1355bcb36a183ee926a7c2080d1362c511ff 与受测候选一致。

| 本轮执行 | job | 结果 |
| --- | --- | --- |
| [O1 38197094938](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197094938) | 114642072886 | 72 reader + 1 MVCC = 73；0 failure/error/skip，BUILD SUCCESS |
| [U02 38197094425](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197094425) | Java 114642071603 | 434，0 failure/error，1 skip，BUILD SUCCESS |
| 同 U02 | Python 114642071449 | 11+9+1 passed |
| [U03 38197094540](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197094540) | 114642071882 | 434，0 failure/error，1 skip，BUILD SUCCESS |

三个 run 均 completed/success，四 job 完整日志核验，实际 Java8/Spring JDBC/MySQL8。CD08显式参数harness skip不算通过，重复434不算独立增量。129 F8/Source既有基线未重跑。本地diff check、六manifest角色核对、原DDL digest bootstrap通过；本地未执行Java/MySQL测试。

### 修复后定向复审与范围决定

复审按 #417 的两个 finding 验收条件检查代码、真实DDL回归与计数断言，结果 PASS_WITH_BOUNDED_SCOPE；未发现本次修复新增阻断项。O1-I01=CLOSED_BOUNDED；O1-R02=CLOSED_BOUNDED，限所列主键角色拒绝与代理故障拒绝测试。旧 #417 文档是旧HEAD审查记录，不用覆盖历史报告冒充当时已关闭。

这不是独立人员批准、生产拓扑认证、完整物理前提全组合证明或合并授权。其他未列明故障组合未因此关闭。REAL_D5=NOT_READY；生产scope/action、delivery/eligibility、question/pending/current CDP、APPLIED/Source、共同writer协议与outbox继续OPEN。

下一步：隔离验证收口与集成就绪评估，然后如需整理 main-based 候选，再进行精确 main-target 差异复审和合并授权评估。本次仅补证据文档，测试代码blob不变；最终文档HEAD自动CI如重跑，其结果写PR body，避免无限文档提交。
