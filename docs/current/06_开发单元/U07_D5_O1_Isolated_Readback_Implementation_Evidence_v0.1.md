# U07 D5-O1 隔离读回实现与证据 v0.1

基线main750770e81af79b764cc4148d7fa856363c89972a；设计#412 v0.3（1b58f4d4f3fd7ffc4536d698f1fc05852f9031ae），设计复审#415（599fa7fb6dff9c8c01f18459ef1b1dcb8cbd88a0）。只读结构工具，不是生产Owner CA。

按13文件清单实现：独立pom、5 main Java类、2 test类、3 test资源、1 workflow、本证据文档。没有生产Bean、路由、原迁移/SQL/adapter改动，没有JPA或生产源码编译输入。Jackson仅解析固定schema/scope fixture资源。

固定disposable URL和NONPRODUCTION_SYNTHETIC；lazy资源构造无连接。input/scope/outer检查后才使用Spring JDBC RR read-only事务；每请求同bound connection观察metadata/engine/shape/隔离；P0..P10硬关联及版本检查先于STALE；authority恒UNKNOWN，非MATCH无payload。SELECT只投影结构列，不读取problem_text/user_id/subject/答案/CDP JSON。

原V2→V3→V6 bytes由bootstrap严格SHA256/Git blob校验并加载，现有PK/unique保持，未创建f8_owner镜像。额外delivery/trace表由原V6带入，但reader只有六Owner表SELECT。root合成fixture与独立writer账号同reader隔离。

测试覆盖typed矩阵、旧请求叠加当前损坏、版本回退优先、全部scope字段、构造到拒绝路径零connection/SQL、profile/URL硬拒绝、outer事务、错误manager DS、真实schema/engine破坏、SELECT-only DML/DDL拒绝及六表读前后不变。隔离级别不符测试用返回报告的connection proxy，明确只是故障注入；实际RR及同connection由真实reader路径及MVCC测试提供证据。

MVCC用barrier：第一次普通consultation SELECT后独立writer将根/thread更新并COMMIT；旧reader必须仍MATCH且旧版本，新reader必须NO_CURRENT_WAIT。无sleep猜测，不把metadata preflight当owner snapshot实测。所有数据库均合成数据，未访问真实患者/生产数据库。

本地bootstrap产物/Python语法、原Source golden检查成功；本地无Maven/MySQL/docker，不声明本地Java执行。精确HEAD的真实Java8/Spring/MySQL Actions及实际test merge checkout/run/job/数量会记录于实施PR body；当前CI PENDING，不将129旧基线当新模块实测。

状态ISOLATED_IMPLEMENTED_PENDING_REAL_VERIFICATION。完成后还需实现/证据审查与有限关闭/集成授权。没有合并、没有生产D5授权。真实scope/action、delivery/eligibility、current question/pending/CDP、APPLIED/Source、writer共同协议及outbox门禁OPEN；REAL_D5=NOT_READY。
