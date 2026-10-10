# U07 D5-P 前置入口与恢复边界设计 + 实现就绪评估 v0.2

日期：2026-10-10。状态：TARGETED_REMEDIATED / READY_FOR_TARGETED_RE_REVIEW。修订来源：PR #376 的 B01、R01–R03；本文件为新版本提案，未获实现授权。
基线：PR #373 HEAD 4a2ea4ebbc2e7359a2c8292895646d89ac113d1b；D4 决策 PR #374 HEAD 0924e08d22260212b202a1d2eb5f432e74b2d53a。均未合并。仅设计，无代码/SQL 执行或合并。

## 1. 与原路线接轨

原 PR #360 的 D5 定义为 Authorized application：真实 source readback、F8、P01/G2、outbox/consumer receipt、target runtime binding。该原定义保持不变。
本阶段新增前置标签 D5-P，不把 synthetic runner 包装成真实 D5，不因 D4 的 bounded PASS 推导真实 APPLIED。

D4 只关闭 disposable MySQL 合成验证范围；旧 D4 路线列出的数据库重启、完整阶段和更广并发没有因此全部完成。跨会话唯一键竞争、Oracle、完整 Flyway 部署仍分开待验证。

| 阶段 | 目标 | 当前判定 |
| --- | --- | --- |
| D5-P1 | 唯一事务 owner，消除原公开 Connection 提交入口 | 设计已具体化，待评审/实现 |
| D5-P2 | 单语句恢复证据 + 不确定提交语义 | 设计已具体化，待评审/实现 |
| D5-P3 | SQL 前完整 synthetic 身份与测试目标约束 | 设计已具体化，待评审/实现 |
| 原 D5 | 经授权的真实应用与回执闭环 | NOT_READY |

## 2. 当前实物与影响范围

U07D3SyntheticTransactionCoordinator 当前 public 方法接收任意 Connection，自己 commit/rollback。
U07D4SyntheticOwnedTransactionRunner 每次新开连接，但仍委托 D3 提交，URL 接受任意查询参数。
U07D4SyntheticRecoveryInspector 在 READ_COMMITTED 下分两条 SELECT，可能跨两个提交快照。
D3 仅检查 event/key synthetic-d3- 前缀；inspector 对 consultation/question/wait 有额外约束。
D2 application/outbox repository 已不提交，继续保持 storage-only。
Foundation CanonicalBusinessEventLedger 是现有 Spring/JPA 身份 owner，其 DataIntegrityViolationException 后同事务读取、flush 时机和事务桥接未验证；D5-P 不接线或修复该生产层。

D5-P 允许的未来变更清单：D3 coordinator、D4 owned runner、D4 inspector；新增 synthetic 输入校验/结果类型；D3/D4 JDBC smokes、forced-exit worker、两份 isolated workflows、必要 architecture tests。
不修改 migrations、Foundation JPA ledger、U06/P01/G2/F8、生产 Spring wiring 或正式临床枚举。不新增 effect/outbox dispatch。

## 3. P1：事务 owner 与调用图

公共边界仅允许 execute(SyntheticInput, now, fault) 和 inspectFresh(SyntheticInput)。均不接受 Connection、DataSource、调用方事务或任意 SQL callback。
runner 通过固定 disposable test target 新开连接；唯一负责开始/commit/rollback/close。
D3 coordinator 改为 package-private transaction body；不再公开可被外部调用的 Connection API，不持有 commit/rollback 权限。仓储也不提交。
coordinator 和 inspector 的 class 与 Connection 方法均 package-private；公共执行/恢复边界只属于 synthetic runner。同 package 为受信任内部实现范围；Java package-private 不是抵抗恶意反射的安全沙箱。任何生产 Spring/controller/正式业务服务禁止引用这些 synthetic 类；不能以此宣称 JVM 内任意代码无法访问数据库。完整可达性约束见第10节。

执行顺序：
1. 校验输入、时间、fault 和 test target；失败不得创建连接或执行 SQL。
2. 新开专属连接，设置 READ_COMMITTED、autoCommit=false。
3. coordinator 执行既有 synthetic body，返回 ACCEPTED / SAME_EVENT_REPLAY / REJECTED_CURRENTNESS，不结束事务。
4. owner 对 ACCEPTED/replay commit；对 currentness rejection rollback。异常按提交阶段分类并清理。
5. close；返回结果中不能泄露连接或凭据。

保持当前 D3 consultation → canonical → wait（首次接受）→ application 锁路径；此顺序只适用于本 isolated synthetic 实现。PR #360 的 canonical-first 是未验证 proposal，不能与本路径混合到生产。真实 D5 与 JPA/U06 的统一锁序仍为独立门禁。

迁移所有直接调用：U07D3SyntheticAdmissionJdbcSmoke、D4RecoveryConcurrencyJdbcSmoke、ForcedProcessExitWorker、owned runner。删除 public 方法而非保留 deprecated bypass。原测试直接 Connection 接口不承诺兼容；测试 fixtures 仍可自建连接，但不得借公共执行 API 传入。

P1 验收：源码/反射证明无 public Connection 执行入口；coordinator/repository 无 commit/rollback；owner terminalization 路径可计数；调用方另一连接的 sentinel 保持未提交；错误/currentness rollback 无 durable 新行。原“同连接预置 sentinel”误提交反例在接口删除后转为不可构造；不得再保留公开 callback 来测试它。

## 4. P2：恢复必须一次读完整证据

选择一个普通 nonlocking SELECT，把请求锚点、canonical 双身份候选、application 合成一个结果集。不要分两次查询，不用 SELECT FOR UPDATE，不调用读表函数，不在读取事务中先写再读。

逻辑形状（设计伪 SQL，绑定参数，不是本轮执行的迁移）：

    request_anchor(request_event_id)
      LEFT JOIN canonical_business_event c
        ON c.event_id = request_event_id OR c.idempotency_key = request_key
      LEFT JOIN u07_event_application a
        ON a.event_id = request_event_id

anchor 始终一行，确保 canonical 缺失而 application 存在的 orphan 也可被看到。不要只以 canonical 为主表，否则 orphan 被误判无记录。
实现使用 MySQL/Oracle 方言适配的参数锚点；MySQL 可 CAST bind AS CHAR(128)，Oracle 对应 VARCHAR2(128)。列显式别名，所有身份、phase/decision/revision/effect 同次读取，最多两个 canonical 候选，超过一个判 split identity conflict，不把重复 join 行当正常结果。
数据库唯一键限制两个候选；仍显式检查行数与 identity，不能按首行挑选赢家。

| 同次读取结果 | 返回证据状态 | 权限含义 |
| --- | --- | --- |
| 无 canonical 与 application | NO_DURABLE_EVENT | 该快照未见记录；不代表不确定提交永远失败 |
| canonical 双候选/字段不匹配 | IDENTITY_CONFLICT | 明确拒绝，不重试写入 |
| 仅 canonical 或仅 application | PARTIAL_OR_INCONSISTENT | 保守输出需审查；无修复权 |
| 两者匹配、RECEIVED/NULL/0/no effect | RECEIVED_ONLY_REQUIRES_REVIEW | 不是 ACCEPTED |
| 两者匹配、ACCEPTED/ACCEPTED/1/no effect | HISTORICAL_ACCEPTED | 仅合成历史接受，不是 APPLIED |
| 其他 phase/decision/revision/effect 组合 | PARTIAL_OR_INCONSISTENT 或已有 identity conflict | 不提升权限 |

读取 SQLException 在 RecoveryResult 中记录 readStatus=UNAVAILABLE 和原始错误，不能转为 NO_DURABLE_EVENT。使用新连接只读事务，结束时 rollback；readOnly 仅是驱动提示，不依靠它授予/拒绝权限。
同次快照成立的适用条件：MySQL InnoDB、普通一致 SELECT、无脏读、无 DDL；Oracle 语句级读一致性。设计采用单语句是根据官方一致性合同作出的选择；Oracle 仍需真库验证，不由文档代替。

参考：
- https://dev.mysql.com/doc/refman/8.0/en/innodb-consistent-read.html
- https://docs.oracle.com/en/database/oracle/oracle-database/19/cncpt/data-concurrency-and-consistency.html

P2 验收：原双 SELECT 可控交错反例；新查询 SQL 计数=1；atomic writer commit 前/后结果只落对应完整快照，不混合拼接；orphan、split key、partial、RECEIVED、坏 revision/effect 等反例。将 writer commit 与 SELECT 发起用 latch 明确排序，禁止用 sleep 或仅多次随机执行宣称覆盖。若要证明 statement 执行中交错，用驱动/DB 可观测同步点另补证据，不冒充普通 latch 已覆盖。

## 5. 提交、响应与清理的固定合同

采用第10节规定的 typed result，替代 acknowledgementKnown；正常故障不再留“返回或抛出任一均可”的实施选项。
事务状态包含 ABORT_UNCONFIRMED，不能将 rollback 失败记成 ROLLED_BACK。响应可用性与清理状态是独立字段。
commit 已进入但异常：COMMIT_OUTCOME_UNKNOWN；rollback 成功也不能将其降级为 ROLLED_BACK。
commit 正常返回：COMMITTED；close失败、响应隐藏不改变已知commit事实。
未知commit后快照无记录仍保持不确定；禁止自动写重试、恢复mutation和APPLIED推导。
具体字段、错误优先级和完整路径矩阵以第10节为准。

## 6. P3：SQL 前输入与目标隔离

集中 SyntheticInputValidator，在 public execute/inspectFresh 和内部 body 入口调用，复用同一规则；业务 body 调用是防内部误用，不授予临床权限。

| 字段 | 约束 |
| --- | --- |
| eventId / idempotencyKey | synthetic-d3- 前缀、非空、trim 后与原值相同、最多128 ASCII 字符 |
| consultationId / questionId / waitEffectId | synthetic- 前缀、同上 |
| digest | 非空、最多128；不得把 digest 当认证签名或跨 event 去重规则 |
| expectedConsultationVersion | >=0；权威值仍从 DB 校验 |
| now / fault | 非NULL；只接受已定义测试 fault |

身份字符集固定为 ASCII 字母数字及 . _ : -，禁止空格/control/unicode 混淆。SQL 一律 PreparedStatement 参数化。前缀只是 fixture scope，不是患者隔离或授权证明。
违规必须在 DriverManager.getConnection 前返回带 INVALID_SYNTHETIC_INPUT 的失败 WriteResult/RecoveryResult；代理/mock 验证连接计数和 SQL 数为0。禁止用无上下文异常替代结果合同。

新 test target 配置只允许固定配对：
- 127.0.0.1:33320 / u07_d3_synthetic。
- 127.0.0.1:33321 / u07_d4_synthetic。
URL 由受控配置生成；用户不能传任意查询参数、socketFactory、host、路径、multi-host 或替代 schema。connector 参数固定为现有 disposable smoke 的明确值。凭据仅内存/env 输入，日志不得输出 password、完整连接属性。
容器仅绑定 loopback、随机凭据、独立数据库、退出销毁；这些约束共同构成隔离。未来加端口/数据库属于 profile 明确变更，不设“任意 localhost 安全”例外。

## 7. 准备实施的最小测试矩阵

| ID | 目标 | 必需证据 |
| --- | --- | --- |
| P1-T1 | 无公开 Connection bypass | 编译/反射/调用点扫描 |
| P1-T2 | 唯一 owner | terminalization 测试，所有 body/repo 无 terminal call |
| P1-T3 | 失败/currentness/异常回滚 | 新连接0条新记录及 sentinel未提交 |
| P2-T1 | 单语句与 orphan/split | SQL计数及完整类别 readback |
| P2-T2 | 快照前/后受控 commit | latch排序，不混合拼接 |
| P2-T3 | commit正常/抛异常/close异常 | JDBC fault injection，status与副作用回读相符；真实网络未知提交仍另测 |
| P3-T1 | 每个业务身份越界 | 参数化反例，连接/SQL次数0 |
| P3-T2 | URL/目标越界 | profile构造反例，连接次数0 |
| REG-1 | 合法历史 replay / 7类无效状态 | 保留 #373 75断言语义 |
| REG-2 | D3 / owner / recovery / JVM exit | 全部既有 smoke，编译Java8 |
| REG-3 | worker 非冲突故障 | 注入真实worker路径并验证进程非零，补强旧 classifier-only 证据 |

不按断言数冻结测试，按行为验证；任何变更必须在新的精确 HEAD CI 核验，旧 #373 CI 不能当新实现 PASS。

## 8. 实现就绪结论与明确阻塞

| 范围 | 判定 | 原因 |
| --- | --- | --- |
| D5-P 详细设计 | READY_FOR_TARGETED_DESIGN_REVIEW | API、事务职责、查询形状、结果合同、profile、调用迁移及验收已明确 |
| D5-P 合成MySQL实现 | CONDITIONAL_READY_AFTER_DESIGN_REVIEW | 当前基线和隔离CI可用；需先接受删除旧public API及新结果合同，完成设计复审 |
| 原 D5 真实业务应用 | NOT_READY | authority/source/F8/P01-G2/receipt/target绑定缺少本轮实际验证；JPA/JDBC事务桥接和统一锁序未验证 |
| Oracle及部署 | NOT_READY | 缺Oracle执行证据，完整迁移/Flyway部署历史仍待解决 |
| main 集成/merge | NOT_AUTHORIZED | 当前stack未合并；需单独 exact-main 集成与合并决定 |
| 临床/PHI/生产 | NOT_AUTHORIZED | 本轮无此范围授权 |

独立人员签核未建立，不把本助手自审当作外部reviewer签名。设计评审不要求真实患者或公司数据库。
本文件没有宣称 RR-04/05/06 已关闭；它们只能在相应实现与证据通过后关闭。

## 9. 下一步执行顺序

先进行 D5-P Targeted Design Review，重点只检查：owner终结事务唯一性、single-query orphan/split覆盖、unknown commit结果不被重读升级、validator在连接前拒绝、旧入口调用迁移不留bypass。
通过后进入 D5-P Synthetic Entry/Recovery Boundary Implementation，按 P1→P2→P3 同一隔离实现PR推进，真实CI后再关闭这三项finding。原 D5 的真实应用另做 readiness 与授权，不把它偷偷合并进前置修复。

## 10. 本轮定向修订的约束合同（B01 / R01 / R02 / R03）

本节细化前文，实施不得保留旧的单布尔、public Connection入口或未指定结果形式。修复finding仍需实现证据才能关闭。

### 10.1 唯一结果形式与事实分离

execute 返回 WriteResult；inspectFresh 返回 RecoveryResult。输入校验、连接/配置、SQL/业务拒绝、commit、rollback、close等预期故障都转换为这两个明确结果，不另抛普通 SQLException/IllegalStateException 让调用方猜状态。捕获范围限这些预期异常；JVM Error不转换成成功/普通失败，由finally尽力清理并继续抛出，不承诺进程可恢复。

WriteResult 字段：
- operationStatus：SUCCEEDED / REJECTED / FAILED。
- transactionStatus：NOT_STARTED / ROLLED_BACK / COMMITTED / COMMIT_OUTCOME_UNKNOWN / ABORT_UNCONFIRMED。
- responseAvailability：AVAILABLE / SUPPRESSED_AFTER_COMMIT。
- cleanupStatus：NOT_REQUIRED / COMPLETE / FAILED；cleanupErrors 有序保存。
- businessOutcome：ACCEPTED / SAME_EVENT_REPLAY / REJECTED_CURRENTNESS / absent。
- primaryFailure：包含原cause的错误对象或absent；禁止记录凭据。
- durableObservation：初次execute固定absent，恢复单独调用，不自动读取或改写write事实。

RecoveryResult 字段：
- operationStatus：SUCCEEDED / FAILED。
- readStatus：NOT_ATTEMPTED / COMPLETE / UNAVAILABLE。
- observation：只有完整SELECT读取及分类完成时存在；状态为第4节证据分类，包含客户端observedAt。
- cleanupStatus / cleanupErrors / primaryFailure，同上。
- 没有businessOutcome，不改变既有WriteResult.transactionStatus，不授予恢复执行权。

AVAILABLE 表示结果可交给本地测试调用者，不是HTTP/网络送达保证。SUPPRESSED_AFTER_COMMIT 是命名测试故障，仅在commit已正常返回时设置；其businessOutcome必须absent。WriteResult中的COMMITTED是故障fixture元数据，不能被业务消费者拿来合成响应。

正常消费者成功条件必须同时满足 operationStatus=SUCCEEDED、responseAvailability=AVAILABLE、cleanupStatus=COMPLETE、transactionStatus=COMMITTED。只看businessOutcome或COMMITTED不得走正常成功通路。
close失败时businessOutcome若先前已知可保留为历史元数据，但operationStatus=FAILED，禁止正常成功投递。已确认currentness rollback才可保留REJECTED_CURRENTNESS。

### 10.2 错误优先级和状态矩阵（B01 / R02）

原validate/body/read/commit/rollback错误为primary，后续rollback/close错误按发生顺序附加；若此前没有primary，首个cleanup错误成为primary，同时仍记录在cleanupErrors中。
只有rollback正常返回且未进入commit才能记录ROLLED_BACK。ABORT_UNCONFIRMED表示事务已开始、未进入commit、终结未确认；不代表可写重试。
连接配置未完成前，禁止任何业务SQL。如果autoCommit=false已成功但其它配置/准备失败，按已开始事务处理；配置顺序固定先isolation，再autoCommit=false。

| 路径 | operation / transaction或read | response / cleanup | business或observation |
| --- | --- | --- | --- |
| 输入/目标违规 | FAILED / NOT_STARTED或NOT_ATTEMPTED | AVAILABLE / NOT_REQUIRED | absent；连接计数0 |
| getConnection失败 | FAILED / NOT_STARTED或UNAVAILABLE | AVAILABLE / NOT_REQUIRED | absent |
| 配置失败且尚未autoCommit=false | FAILED / NOT_STARTED或UNAVAILABLE | AVAILABLE / COMPLETE或FAILED | absent；close执行 |
| body失败、rollback成功 | FAILED / ROLLED_BACK | AVAILABLE / COMPLETE或FAILED | absent；原body错误保留 |
| body失败、rollback失败 | FAILED / ABORT_UNCONFIRMED | AVAILABLE / FAILED | absent；rollback/close错误附加 |
| currentness拒绝、rollback成功 | REJECTED / ROLLED_BACK | AVAILABLE / COMPLETE | REJECTED_CURRENTNESS |
| currentness拒绝、rollback失败 | FAILED / ABORT_UNCONFIRMED | AVAILABLE / FAILED | absent |
| currentness rollback成功但close失败 | FAILED / ROLLED_BACK | AVAILABLE / FAILED | 可保留REJECTED_CURRENTNESS元数据 |
| commit正常、close正常 | SUCCEEDED / COMMITTED | AVAILABLE / COMPLETE | ACCEPTED或SAME_EVENT_REPLAY |
| commit正常、close失败 | FAILED / COMMITTED | AVAILABLE / FAILED | 已知business元数据可保留，不能正常投递 |
| commit正常、隐藏响应、close正常 | SUCCEEDED / COMMITTED | SUPPRESSED_AFTER_COMMIT / COMPLETE | absent |
| commit正常、隐藏响应、close失败 | FAILED / COMMITTED | SUPPRESSED_AFTER_COMMIT / FAILED | absent |
| commit已进入但抛异常 | FAILED / COMMIT_OUTCOME_UNKNOWN | AVAILABLE / COMPLETE或FAILED | absent；cleanup正常也不消除unknown |
| BEFORE_ADMISSION、rollback成功 | FAILED / ROLLED_BACK | AVAILABLE / COMPLETE或FAILED | absent；命名测试故障原因 |
| BEFORE_ADMISSION、rollback失败 | FAILED / ABORT_UNCONFIRMED | AVAILABLE / FAILED | absent |
| 完整SELECT分类成功、清理正常 | SUCCEEDED / COMPLETE | cleanup COMPLETE | observation保留 |
| 完整SELECT分类成功、清理失败 | FAILED / COMPLETE | cleanup FAILED | observation仍保留，不能自动写修复 |
| query/resultset读取/分类未完成 | FAILED / UNAVAILABLE | cleanup COMPLETE或FAILED | observation absent，绝非NO_DURABLE_EVENT |
| read输入校验失败 | FAILED / NOT_ATTEMPTED | cleanup NOT_REQUIRED | observation absent |

读事务cleanupStatus只有rollback与close均正常才COMPLETE；任一失败即FAILED。readStatus不承担回滚事实，不转成写入COMMIT_OUTCOME_UNKNOWN。未来需要资源诊断可看cleanupErrors，不新增业务权限。
observedAt是客户端分类时间，不是DB快照SCN或commit版本。分类成功后不能把close失败吞掉，但也不能把已读证据抹去。
提交未知连接不再执行SQL；只做最佳努力rollback/close并弃用。cleanup失败不得将连接放回任何复用路径，本阶段DriverManager专属连接无连接池。

### 10.3 可达性清单（R01）

| 元素 | 可见性/可调用范围 |
| --- | --- |
| synthetic runner构造 | public，参数只为固定TestTarget枚举和凭据；不接受任意URL/Connection/DataSource/Factory |
| execute / inspectFresh | public typed input/result；无任意callback |
| SyntheticInput / results / TestTarget | public不可变数据类型；不携带Connection/SQL执行能力 |
| D3 coordinator及Connection body | package-private；仅runner调用 |
| D4 inspector及Connection query | package-private；仅runner调用 |
| connection fault/spy instrumentation | src/test专用包装器；生产构造器不接受注入factory，测试通过测试注册JDBC driver对固定生成URL计数/代理 |
| 正式Spring/controller/Foundation/U06业务 | 禁止引用所有synthetic runner/coordinator/inspector，不允许改为“只调用runner” |
| D2 storage-only repositories | 继续无commit/rollback；不冒称其低层Connection接口已全局隐藏 |

public inspection同时检查class、constructor、method、nested types。生产引用检测对非synthetic main路径生效；测试fixture引用按下表例外列出。不对恶意反射或同JVM任意代码作隔离保证。
测试driver代理仅src/test存在：运行时测试先注册interceptor，再委托明确的真实MySQL driver；不能从正式runner公开注入任意执行对象。此机制不写生产注册/配置。

### 10.4 固定测试迁移与例外（R03）

选择保留pre-exit直接fixture写入，不在runner公开新的precommit callback。

| 调用点 | 迁移后调用 | 特殊约束 |
| --- | --- | --- |
| D3SyntheticAdmissionJdbcSmoke | D3_TARGET runner.execute；读回通过inspectFresh或test查询 | 原异常断言改验证typed失败cause/status |
| D4RecoveryConcurrencyJdbcSmoke | D4_TARGET runner.execute/inspectFresh | fixture原始SQL可保留；不向执行入口传Connection |
| D4AdversarialOwnedJdbcSmoke | D4_TARGET runner.execute/inspectFresh | worker按typed primaryFailure分类，未知提交必须FAIL/显式unknown，不算普通conflict |
| D4ReplayStateJdbcSmoke | D4_TARGET runner.execute/inspectFresh | 七类fixture写入保持test-only，坏重放验证typed失败 |
| ForcedProcessExitWorker pre | test-only新连接写canonical+RECEIVED，再Runtime.halt(77) | 显式不走runner；只证明数据库未提交双行在进程退出后消失，不证明runner crash点 |
| ForcedProcessExitWorker post | D4_TARGET runner.execute成功且COMMITTED/cleanup COMPLETE后Runtime.halt(78) | 不保留coordinator Connection调用；证明owner已提交后退出 |
| 新边界fault smoke | src/test driver spy + fixed目标runner | rollback/close/query/commit错误矩阵；无公开callback |
| 独立读回parent shell | 原fresh DB查询 | pre=两行0；post=两行存在且ACCEPTED |

post-exit在owner返回后发生，明确不是commit返回到close之间的退出；原两行持久性覆盖保留，window精度不夸大。

D3 workflow保留33320/u07_d3_synthetic；D4保留33321/u07_d4_synthetic。CLI仍接URL/user/password的旧test程序在进入runner前把URL与两条固定生成字符串逐字匹配成TestTarget，其他URL返回失败，不把任意URL交给runner。
固定URL=jdbc:mysql://127.0.0.1:<paired-port>/<paired-schema>?useSSL=false&allowPublicKeyRetrieval=true；生产不得引用该配置。target本身不接受用户查询参数。
两份workflow javac新增validator、result types、target、边界fault测试和共享classifier；各程序所需源文件必须完整列出。D3不依赖D4运行数据库；类名历史保留不影响target语义。

fault evidence覆盖：
- commit代理在delegate前抛出：写结果UNKNOWN，fresh read无记录仍不自动重试。
- delegate commit正常后代理抛出：写结果UNKNOWN，fresh read历史接受，不改写原UNKNOWN。
- rollback异常与rollback+close异常：ABORT_UNCONFIRMED、primary保持、无正常success。
- 非冲突worker故障：child进程非零，父进程验证，不只测试classifier函数。
这些是JDBC故障模拟，不声称真实网络partition。
失效输入spy记录0连接/0 SQL；真实MySQL仅用于有效输入与故障路径。

### 10.5 单查询分类顺序补充

固定优先级：canonical候选>1或canonical字段不匹配 → IDENTITY_CONFLICT；application身份不匹配 → IDENTITY_CONFLICT；两种都无 → NO_DURABLE_EVENT；仅一类存在 → PARTIAL_OR_INCONSISTENT；两类身份匹配但effect非NULL → PARTIAL_OR_INCONSISTENT；之后按RECEIVED/ACCEPTED严格phase/decision/revision组合分类；其它→PARTIAL_OR_INCONSISTENT。
这使非NULL effect从旧inspector的identity-conflict改为partial分类，需明确记录为合成query结果分类细化并更新对应测试；拒绝权限不变，不改冻结F8/临床合同。

## 11. 修订就绪评估

B01、R01、R02、R03均已提出具体设计补充；本轮不自称独立签核或实施PASS。
D5P_TARGETED_REMEDIATION = DESIGN_COMPLETE
D5P_DESIGN_RE_REVIEW = PENDING
D5P_SYNTHETIC_IMPLEMENTATION = CONDITIONAL_AFTER_RE_REVIEW
RR04 / RR05 / RR06_IMPLEMENTATION_CLOSURE = OPEN
REAL_D5 / ORACLE / PRODUCTION / MERGE = NOT_AUTHORIZED

下一步仅对本修订精确HEAD做targeted re-review，核验状态矩阵完整性、public API封闭、测试driver不泄漏生产及fixture例外边界。通过后再进入同一隔离实现PR；不继续扩展治理范围。
