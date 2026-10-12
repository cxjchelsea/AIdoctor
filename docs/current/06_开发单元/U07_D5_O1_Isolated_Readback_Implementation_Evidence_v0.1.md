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
