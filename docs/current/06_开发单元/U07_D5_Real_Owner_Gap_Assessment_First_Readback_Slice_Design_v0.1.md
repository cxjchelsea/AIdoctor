# U07 D5 真实Owner接入差距评估与首个接入切片设计 v0.3

日期2026-10-12；固定main750770e81af79b764cc4148d7fa856363c89972a。本交付仅设计，不改实现/DDL/生产配置、不运行真实业务数据库、不合并。SOURCE_INSPECTION_COMPLETE；FIRST_SLICE_DESIGN=TARGETED_REMEDIATION_COMPLETE_RE_REVIEW_PENDING；REAL_D5=NOT_READY。

## 1. 已有基线与结论

#405/#408已合main。#411合并后104 F8+25 Source=129项通过，仅证明disposable synthetic Source/F8与故障范围。真实接入仍需要owner权威、实际Spring事务拓扑、action权限和readback合同，不能把tools/f8_owner复制成生产owner。

本轮读取main完整Git路径树及文末列明的具体源码/DDL/既有manifest。以下是可见源码判断，不是全仓语义扫描或实际Spring proxy/bean/manager/数据库认证。旧Tier0 manifest描述旧HEAD的历史门禁，不能直接当当前main的运行时结论。类或方法有@Transactional不证明其当前被代理调用或各模块处于同一事务。

建议首个切片D5-O1：现有持久化等待链路的只读结构核验原型。读取实际owner表形状，在独立MySQL/disposable实例填充合成数据；不接真实患者数据库、不创建F8最终decision/claim、不把结构一致性升级为真实eligibility。首个完整真实F8写接入目前没有依赖全部满足的切片。

## 2. Owner来源与差距矩阵

| F8事实 | main可见候选来源 | 已有能力 | 真实接入缺口 |
| --- | --- | --- | --- |
| Consultation生命周期/current wait | runtime/u01/ConsultationRecord、ConsultationRepository；u06/wait/ConsultationWaitTransitionService；clinical_consultation | ACTIVE/WAITING_USER、current_wait_effect_id、@Version；establish用findByIdForUpdate及saveAndFlush，写wait effect | 没有本轮认证的终态/撤销/截止期限owner合同；rowVersion不是临床version；tenant/actor不能由user_id直接推出 |
| Wait effect | ConsultationWaitEffectRecord/Repository；clinical_consultation_wait_effect | consultation/question/delivery/parent关联、prior/committed版本、唯一parent与idempotency键 | 与thread/checkpoint/permission writer共享锁/事务的实际证据未知；effect_status字段不是独立投递权威 |
| Runtime thread/checkpoint | RuntimeThreadStateRecord、RuntimeWaitCheckpointRecord及各Service/Repository；V6表 | current run/checkpoint/wait、checkpoint clinical_state_version及pending refs；thread锁、checkpoint预留、进入AWAITING_USER | U06WaitCoordinator.establish没有自己的事务注解，逐调用服务；实际代理/外层事务未知；部分阶段提交需读回分类，不能只看checkpoint ACTIVE |
| Question/pending | U06SyntheticP01Runtime路径patient_state/questions、pending_question；checkpoint question_id/pending_question_ref | synthetic P01授权有限JSON路径，backend可注入 | checkpoint中的ref不证明question/pending当前事实；未从本轮读取中建立独立实体writer/可认证字段及权限；不能宣称全仓没有实现 |
| Issuance/delivery确认 | JdbcU06DeliveryStore、V6 u06_delivery_* | authority/intent/attempt/receipt/confirmation/ledger持久记录 | reconcileConfirmed写SYNTHETIC_ACCEPTED、SYNTHETIC_CONFIRMED、evidence_type=SYNTHETIC；CONFIRMED字符串不是实际渠道确认/有效投递权威 |
| Resume eligibility | U07ResumeEligibilityProjector.project | 从parent/checkpoint及常量生成ID，检查caller bool | consultation/thread/run参数不参与校验，bool由caller传入；没有独立持久authority/issuer/policy/epoch/deadline读回；hash ID不是权威证明 |
| 当前clinical version及P01 APPLIED | ClinicalCdpStateRepositoryAdapter、StateRepositoryPort；CDPVersionService | readCurrentVersion；attemptAtomicCommit锁CDP、expectedVersion、有限字段patch、version+1 | adapter方法返回committed不代表独立F8/APPLIED durable receipt；U06SyntheticP01Runtime Idem为内存Map，Events为空实现；CommitEventEvidencePort明确non-authoritative，不能当APPLIED |
| Source/Canonical | CanonicalBusinessEventLedger/Record/Repository、tools SourceBindingVerifier/TestAuthority | Foundation canonical持久结构；隔离工具全链binding/cipher校验 | tools issuer/action/profile为synthetic；真实来源证明、租户/actor许可、cipher/keyRef、canonical immutable binding contract尚未建立 |
| action权限/guard/outbox | tools f8_action/f8_guard只属fixture | 隔离epoch/read-finalize分离、共同guard | 实际permission owner及撤销writer协议未认证；outbox不是此slice能力，EV01仍OPEN |

根路径前缀：diagnosis-service/src/main/java/com/aidoctor/diagnosis/；表依据src/main/resources/db/migration/V2/V3/V6。没有将runtime Status与clinical truth混为同一owner。

## 3. 首个切片D5-O1合同

### 3.1 目标与可实施边界

在tools/u07_d5_owner_readback新增独立Java8/Spring/MySQL验证模块（具体文件清单待设计审查通过后实施）。通过独立fixture直接使用现有迁移定义的以下六张表：
clinical_consultation、clinical_consultation_wait_effect、clinical_runtime_binding、clinical_runtime_run、clinical_runtime_thread_state、clinical_runtime_wait_checkpoint。

不创建f8_owner镜像，不借synthetic状态替换真实表列；不调用生产writer/service建立状态。根管理员仅disposable fixture准备数据，reader账号SELECT-only；迁移脚本执行与数据准备不是reader能力。未来可选生产owner adapter接入必须再次认证，当前无需先改生产所有writer。

独立模块只编译新reader/value/test类；以JDBC读取现有表结构，不引入生产Spring Bean/controller或临床实体包扫描，不接CDP JSON/真实投递或F8 finalize。读取表结构是向实际owner接入推进，仍不等于真实业务数据接入。

### 3.2 输入、TEST_SCOPE和硬禁用规则（O1-B02）

工具profile唯一常量NONPRODUCTION_SYNTHETIC；数据库URL唯一常量jdbc:mysql://127.0.0.1:33325/u07_owner_readback?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC。任何production/未知/空profile、非精确URL或混入额外连接参数均CONFIG_REJECTED；资源工厂在创建连接前抛配置异常。不得仅依赖authorizer返回值阻断production；fixture authorizer ALLOW也不可绕过。reader无生产路由/Bean和任意URL入口。

Request是immutable四元组(consultationId,threadId,runId,checkpointId)。ID必须非空、不带前后空白、长度1..128，禁止NUL/control characters；不静默trim、不拼接到SQL。invalid input返回INVALID_INPUT/INPUT_INVALID，payload为空；所有字符串按Java精确相等校验，不沿用synthetic-prefix codec限制真实ID形状。

ScopeAuthorizationPort仅由工具构造器注入可信内部fixture对象，不接受请求携带port或attestation。evaluate(request)返回DENY、UNAVAILABLE或ALLOW+immutable TestScopeAttestation：
(schemaVersion=O1_TEST_SCOPE_V1,evidenceKind=TEST_SCOPE,profile,fixtureManifestRef,schemaManifestRef,consultationId,threadId,runId,checkpointId,testTenantId,testActorId)。
字段全部非空且manifest/schema引用与工厂冻结值完全相同，四元组与Request逐字段相等；tenant/actor只属合成预先批准映射，不从user_id推导。attestation没有真实权限签名/epoch承诺，不称生产CA。

reader在任何SQL/连接创建前做input和scope检查；ALLOW缺证据、未知schema/profile/kind、错manifest、错任一ID或缺tenant/actor→DENIED/SCOPE_BINDING_INVALID；DENY→DENIED/SCOPE_DENIED；UNAVAILABLE、null/未知decision或port异常→UNAVAILABLE/SCOPE_UNAVAILABLE。所有返回payload为空且不透露对象存在性。fixture port只使用预先批准四元组白名单，不先读数据库再生成ALLOW；根ABSENT用白名单内尚未种入的ID测试。请求不含actor/tenant，只有可信fixture映射能绑定它们。

### 3.3 单快照、实际前提与查询计划（O1-R01）

固定MySQL8.0、Java8、SpringBoot2.7.8/spring-jdbc、mysql-connector-j8.0.32；不引入JPA/生产package扫描。工具资源工厂固定reader凭证及JDBC DataSourceTransactionManager；reader拥有TransactionTemplate(readOnly=true,isolation=REPEATABLE_READ)并拒绝外层事务，返回UNAVAILABLE/OUTER_TRANSACTION_PRESENT。

schema bootstrap在新disposable schema设置default_storage_engine=InnoDB并按V2→V3→V6加载未改原DDL；manifest记录固定source SHA750770e8全文、三DDL的实际Git blob/digest、六表字段/PK/unique shape。资源构造器只做纯内存profile/URL/manifest校验并创建lazy DataSource/manager，不调用getConnection、不执行startup preflight、health check或后台预热。没有自动启动SQL。

每次read调用在input、scope及outer事务检查全部通过后，才由TransactionTemplate获取reader连接、建立read-only RR事务。在此请求的同一bound connection内执行非缓存metadata preflight：检查MySQL8.0、固定catalog、六表存在/ENGINE=InnoDB/字段PKunique shape与冻结manifest一致。schema/engine/版本不匹配→UNAVAILABLE/RESOURCE_PRECONDITION_FAILED；metadata异常→UNAVAILABLE/DB_UNAVAILABLE，payload为空。metadata仅查固定schema，不列举其他库，不建立owner snapshot证明。不跨请求缓存positive preflight，每个获授权请求重新观察前提。

每次业务读在同一transaction-bound physical connection上检查autoCommit=false、connection.getTransactionIsolation()==TRANSACTION_REPEATABLE_READ和固定catalog；JdbcTemplate必须复用此connection。连接漂移、错误manager/DS、连接或SQL异常→UNAVAILABLE/RESOURCE_PRECONDITION_FAILED或DB_UNAVAILABLE，payload为空。runtime readOnly标志不作为DML权限证明。

所有owner读取用普通parameterized SELECT；无FOR UPDATE、LOCK IN SHARE MODE或其他locking/current read。snapshot在首次clinical_consultation普通一致性读时建立，不声称BEGIN时建立；schema metadata preflight不作为snapshot建立证据。后续binding/thread/run/当前wait effect/请求checkpoint独立读取，不使用INNER JOIN吞掉缺对象。读取当前指向而非只用请求指向；PK对象0/1、多行逻辑必须显式检查。

Reader没有业务DML；GRANT只六张目标表SELECT，不授全schema SELECT/任何INSERT/UPDATE/DELETE、DDL或F8访问。root fixture仅用于加载原DDL/种合成数据/独立writer测试，永不作为reader DS。结果离开txn后立即只属历史观察，不作为F8 finalize缓存、permission或eligibility；六表同一实例保证不是跨服务全局事务认证。

### 3.4 精确关系、分类矩阵与返回值（O1-B01）

StructureStatus={MATCH,ABSENT,STALE,INCONSISTENT,INVALID_INPUT,DENIED,UNAVAILABLE}。authorityStatus恒UNKNOWN；MATCH仅表示该冻结六表结构。Payload仅MATCH包含六表最小结构IDs、consultation/thread rowVersion及checkpoint capturedClinicalVersion；其他status无row payload。Envelope仅status/reason、authority UNKNOWN及固定非敏感schema/source manifest引用，scopeEvidenceKind仅成功授权后TEST_SCOPE，否则NONE；不输出problem_text/user_id、subject refs、答案或CDP JSON。无COMMITTED/APPLIED/F8 verdict/canFinalize字段。

分类只有下面的phase流程一个真源，按P0→P10执行，每阶段内部按列明顺序。v0.2的0..18首命中表废止。硬错误只对该阶段已经读取、存在或被当前硬指针要求存在的对象判定；不将正常尚未建立的未来checkpoint视为损坏。所有阶段普通SELECT属于同一owner snapshot。

| Phase | 查询/验证及唯一出口 |
| --- | --- |
| P0 纯内存配置 | profile/URL/manifest非法→CONFIG_REJECTED配置异常；构造lazy资源对象，不开连接/SQL |
| P1 请求与scope | invalid Request/ID→INVALID_INPUT/INPUT_INVALID；随后按3.2检查TEST_SCOPE；DENIED或scope UNAVAILABLE零connection/SQL |
| P2 外层/资源 | 先拒绝外层事务→UNAVAILABLE/OUTER_TRANSACTION_PRESENT；通过后才创建事务/连接，检查物理connection、autoCommit=false、有效RR、catalog及每请求metadata preflight；失败无payload |
| P3 首个owner读 | SELECT consultation建立snapshot；根不存在→ABSENT/CONSULTATION_ABSENT，不再查子对象 |
| P4 根shape | rowVersion null/负数或未知lifecycle→INCONSISTENT/CONSULTATION_SHAPE_INVALID；ACTIVE且current wait非空或WAITING_USER且为空→INCONSISTENT/CONSULTATION_WAIT_SHAPE_INVALID；只有ACTIVE且无wait才能STALE/NO_CURRENT_WAIT，明确不宣称子链完整 |
| P5 基础绑定 | 独立读取binding/thread/request run；按对象顺序分别检查存在唯一性→ID/scope关联→非负version及已知enum；失败INCONSISTENT/OWNER_LINK_INVALID或OWNER_ENUM_INVALID。尚未返回任何“旧请求”STALE |
| P6 当前effect及版本硬检查 | 用consultation当前wait指针读effect；存在/consultation关联、COMMITTED、prior ACTIVE/committed WAITING_USER、prior>=0且committed=prior+1防overflow、question/delivery/parent必填逐项检查；失败INCONSISTENT/WAIT_EFFECT_INVALID。随后consultation.rowVersion<effect.committedRowVersion→INCONSISTENT/VERSION_REGRESSION。这些检查先于所有后续STALE |
| P7 thread指针shape | ACTIVE且残留任一current run/checkpoint/wait→INCONSISTENT/THREAD_WAIT_SHAPE_INVALID；ACTIVE且三者皆空→STALE/THREAD_WAIT_NOT_ESTABLISHED（P6已排除effect/版本硬错误，不读取未来checkpoint）。WAIT_CHECKPOINTED/AWAITING_USER缺任一指针→INCONSISTENT/THREAD_WAIT_SHAPE_INVALID |
| P8 当前链硬完整性 | 用thread当前指针读取current run/current checkpoint（若等于请求ID复用已读对象）；先存在/唯一性，再scope/已知enum，再parent/question/delivery/version/ref关系。任何失败INCONSISTENT/CURRENT_OWNER_MISSING或CHECKPOINT_LINK_INVALID。不因请求旧或consultation较新跳过该阶段 |
| P9 请求/当前及STALE分类 | P8全部通过后处理请求旧checkpoint：若不同于current则独立读请求checkpoint，存在时必须consultation/thread/run与请求身份关联，否则INCONSISTENT/REQUEST_CHECKPOINT_SCOPE_INVALID；旧非当前checkpoint不存在不构成当前硬指针损坏。随后按顺序：请求run或checkpoint不等当前→STALE/REQUEST_NOT_CURRENT；consultation.rowVersion>effect.committedRowVersion→STALE/CONSULTATION_ADVANCED；thread WAIT_CHECKPOINTED→STALE/THREAD_WAIT_CHECKPOINTED |
| P10 最终MATCH | 其余MATCH必要状态全部满足→MATCH/WAIT_CHAIN_MATCH；未知/未覆盖shape→INCONSISTENT/UNSUPPORTED_SHAPE，不fallback MATCH |

P5 binding必须consultation/cdp等于根且thread等于请求thread；runtime_authority固定CLINICAL_RUNTIME_V1；scope_version/capability_set_version/contract_version与fixture manifest精确一致。thread.consultation、请求run.consultation/thread均匹配binding，thread.rowVersion非负；请求run.status允许OPEN/CLOSED、based_on_clinical_state_version>=0。旧请求run的历史version不拿来与当前checkpoint比较；请求run缺失或错scope仍按P5硬失败，不藏入STALE。

P6 effect版本是consultation rowVersion的单saveAndFlush fixture关系，不是clinical version。P8 current run必须consultation/thread匹配根/binding、status为OPEN/CLOSED、based_on_clinical_state_version非负；其version=当前checkpoint.capturedClinicalVersion，只对当前run/checkpoint匹配比较，不读取CDP当前状态。current checkpoint必须ACTIVE、capturedClinicalVersion>=0；consultation/thread/run与当前对象相同；consultation_wait_effect_id=根当前wait；question/delivery等于effect；question_delivered_wait_effect_id=effect.parent_delivered_wait_effect_id=thread.current_wait_effect_id。pending/selection/delivery/confirmation refs及fingerprint非空但不认证独立业务权威。

P8在同一个scope内多个硬错误时按current run→current checkpoint存在/shape→scope关联→parent→question→delivery→captured version→其他必填ref顺序返回确定reason。P6的effect硬错误优先于VERSION_REGRESSION，后者优先于所有P7/P9阶段STALE；前置资源/授权失败只返回其本身原因，不读取后续对象。中间态“不读未来对象”不被称为完整链验证。

MATCH另要求consultation WAITING_USER、thread AWAITING_USER、effect合法、current/request run/checkpoint完全同一、根rowVersion=effect.committedRowVersion。WAIT_CHECKPOINTED只属thread；checkpoint仅接受ACTIVE。run已知OPEN/CLOSED状态在本slice不产生F8可恢复语义。authority始终UNKNOWN，非MATCH payload为空。

### 3.5 资源生命周期与计数验收（O1-R01-R2）

构造前后、invalid input、scope DENIED/null/异常、outer事务拒绝，均计数getConnection/SQL为0（从资源对象构造前开始计数，禁止把startup I/O从统计中剔除）。scope允许请求在P2第一次开启连接；每请求metadata检查与owner读都计入该请求SQL记录。未批准profile/URL在P0配置异常，即使fixture port愿意ALLOW也不会触及port/DB。

preflight无自动startup命令、无缓存positive结果、无另一个preflight connection。完整授权请求的MySQL/engine/schema检查发生在事务内的metadata阶段，首个owner一致性读仍为P3 consultation SELECT。查询日志不得打印payload或未经授权的ID；测试spy只记录statement类别/connection计数和fixture correlation。DB preflight失败不返回旧snapshot，资源close交由TransactionTemplate，异常统一typed UNAVAILABLE。

## 4. 阻断项与后续真实写接入门禁

| ID | 阻断对象 | 关闭条件 |
| --- | --- | --- |
| O-B01 scope/action authority | 真实reader/F8生产入口 | 完整tenant/consultation/actor委托/epoch绑定、撤销writer及独立permission读回 |
| O-B02 delivery/eligibility authority | 真实F8新finalize | owner定义实际确认/issuer/policy/provenance/deadline，禁止synthetic receipt/hash替代 |
| O-B03 question/pending/current clinical version | 真实F8 P1-P8 | 字段owner、当前状态和版本读取及一致snapshot，明确CDP与consultation版本不同 |
| O-B04 有效事务/共同writer协议 | 真实atomic decision/claim | Spring实际proxy/manager/EMF/DS/connection认证；所有写方采用同guard或经批准等价协议；分阶段历史可读回 |
| O-B05 durable APPLIED和Source binding | 真实duplicate/resume/application | effect→canonical→patch结果→committed version→权威receipt原链、来源和cipher合同 |
| O-B06 outbox side-effect authority | 真实effect/dispatch接入 | 接入对应owner并补权限拒绝、零side-effect及未知提交测试；当前继续排除 |

O-B01/02/03/04/05不妨碍明确label的D5-O1隔离结构reader设计，但阻断生产业务授权。设计阶段未关闭任一真实门禁。

## 5. 冻结实施清单与可执行验收（O1-R01）

仅以下拟新增文件，设计审查后才能实施；不改生产源码/原DDL/root build：
- tools/u07_d5_owner_readback/pom.xml：独立最小spring-jdbc/tx、MySQL driver、JUnit5依赖，无build-helper生产包输入。
- src/main/java/com/aidoctor/verification/ownerreadback/{OwnerReadbackRequest,WaitChainObservation,ScopeAuthorizationPort,OwnerReadbackResources,WaitChainReader}.java（5类）。
- src/test/java/com/aidoctor/verification/ownerreadback/{WaitChainReaderTest,SnapshotConcurrencyTest}.java（2类）。
- src/test/resources/{bootstrap-owner-schema.py,scope-fixture.json,schema-manifest.json}（3文件）。
- .github/workflows/u07-d5-owner-readback.yml；一份新实施证据文档。
以上共13个拟新增文件；花括号目录均位于tools/u07_d5_owner_readback。Reader不依赖F8/Source工具package。

Bootstrap固定源manifest原DDL内容digest/Git blob，实际加载V2、V3、V6原始bytes；V6会创建额外u06_delivery/trace表，但reader不获其权限。workflow MySQL8.0容器端口33325，新schema u07_owner_readback，合成fixture，禁止任意外部URL。每次报告实际engine、isolation、autocommit、受测checkout/sourceDDL SHA与manifest，不把配置值当观测值。数据库账号bootstrap root、reader SELECT-only、writer仅fixture所需UPDATE分离。

测试分组与最小判定：
1. P0..P10各独立条件及优先级组合：MATCH仍authority UNKNOWN；invalid input、根ABSENT、ACTIVE无wait、ACTIVE残留指针、thread阶段、每类missing硬指针、旧请求、各版本方向/overflow、未知枚举及所有关联错scope。非MATCH全部无row payload。
2. scope任一字段替换/缺失、未知schema/kind/profile/manifest、任意ALLOW企图绕过production/错URL、port异常；通过connection/query计数证实前置拒绝零连接/SQL。authority UNKNOWN为不可修改结果常量。
3. schema six-engine/shape不匹配、错误manager/DS、outer事务/autoCommit/隔离/connection漂移；真实路径与故障代理分开标记。
4. MVCC barrier：reader完成首次consultation普通consistent SELECT后通知writer；独立writer UPDATE同时改变consultation指针/版本和thread指针并COMMIT，writer通知reader继续；本reader其余SELECT仍观察旧完整snapshot，返回旧MATCH；下一新reader观察新链的相应状态。barrier设有timeout/finally释放，不依靠sleep或connection ID单独证明MVCC。
5. 保留PK/unique；跨关联损坏利用现有无FK结构fixture。重复结果只用现有非唯一业务字段查询或纯classifier测试，不删除owner约束制造“重复PK实测”。
6. reader INSERT/UPDATE/DELETE/DDL被真实DB拒绝；读取前后六表关键列的canonical摘要相同，摘要在fixture内部比对不打印敏感数据。静态依赖/SQL清单及账号无F8表权限证明无F8调用能力；不查询不存在f8表冒充行数0。
7. 不做生产profile数据库执行、真实数据、writer共享guard认证、临床current CDP/APPLIED/readmission/effect/outbox。129基线为已有实测，不称新slice已执行。

## 5.1 修订状态与逐项响应（v0.3）

| Finding | 本次修订 | 状态 |
| --- | --- | --- |
| O1-B01 | 增INVALID_INPUT；冻结lifecycle/effect/thread/checkpoint及版本规则、排序矩阵和中间态；修正WAIT_CHECKPOINTED归属 | REMEDIATED_FOR_RE_REVIEW，非已关闭 |
| O1-B02 | immutable TEST_SCOPE字段+精确绑定检查、前置零SQL、authorizer不可信输入禁止、production/未知profile与非精确URL工厂硬拒绝 | REMEDIATED_FOR_RE_REVIEW |
| O1-R01 | MySQL/InnoDB/实际隔离/单connection前提、原DDL顺序与digest、13文件inventory、barrier及约束保留测试 | REMEDIATED_FOR_RE_REVIEW |

DESIGN_V03=TARGETED_REMEDIATION_COMPLETE；IMPLEMENTATION_READINESS=RE_REVIEW_PENDING，未授权实施或合并。没有新测试执行或运行时认证；O-B01..06真实接入门禁均不变。下一步v0.3两项定向设计复审，之后才决定D5-O1隔离实施。

## 5.2 v0.3 两项修订响应及组合验收

#414（56109e9825cc567033cba0414e0b860a794449ad）：
- O1-B01-R2=REMEDIATED_FOR_RE_REVIEW：废除旧首命中序号表，冻结P0..P10单真源；current effect/版本回退、current run/checkpoint完整性先于后续STALE。
- O1-R01-R2=REMEDIATED_FOR_RE_REVIEW：取消自动startup I/O，lazy资源＋授权/outer检查后同请求事务内非缓存preflight；统计覆盖构造全生命周期。

冻结反例验收（仅设计oracle，尚未运行）：
| 组合fixture | 预期 |
| --- | --- |
| 旧请求+当前checkpoint错parent | INCONSISTENT/CHECKPOINT_LINK_INVALID，不得STALE |
| 旧请求+当前错question或delivery | INCONSISTENT/CHECKPOINT_LINK_INVALID |
| 旧请求+根rowVersion低于effect committed | INCONSISTENT/VERSION_REGRESSION |
| thread ACTIVE三指针空+根版本回退 | INCONSISTENT/VERSION_REGRESSION，先于THREAD_WAIT_NOT_ESTABLISHED |
| 当前链完全合法+同scope旧run/checkpoint | STALE/REQUEST_NOT_CURRENT |
| 当前checkpoint缺失+旧请求 | INCONSISTENT/CURRENT_OWNER_MISSING |
| 当前parent损坏+根rowVersion较新 | INCONSISTENT/CHECKPOINT_LINK_INVALID，先于CONSULTATION_ADVANCED |
| 完整当前链WAIT_CHECKPOINTED+旧请求 | STALE/REQUEST_NOT_CURRENT（P9固定先后） |
| ACTIVE无wait且未来checkpoint不存在 | STALE/NO_CURRENT_WAIT，仅根状态观察 |
| 构造lazy资源后invalid/scope拒绝/outer拒绝 | 无连接/SQL，无row payload |
| 同resources连续两个获授权read，中间engine/shape前提损坏 | 第二次重新preflight→UNAVAILABLE，无缓存MATCH |
| 获授权首次preflight失败 | UNAVAILABLE，owner SELECT计数0 |

13文件范围、固定DDL/source、SELECT-only、production硬拒绝和六类真实owner门禁保持不变。本轮未实施/执行测试/合并，待针对两项复审，不自动升级READY。

## 6. 本轮具体源码依据

读取当前HEAD的：ConsultationRecord、ConsultationWaitTransitionService、U06WaitCoordinator、U07ResumeEligibilityProjector、RuntimeThreadWaitTransitionService、RuntimeWaitCheckpointRecord/Service、RuntimeBindingRecord、ClinicalRunRecord、JdbcU06DeliveryStore、ClinicalCdpStateRepositoryAdapter、U06SyntheticP01Runtime、CommitEventEvidencePort、V6迁移，以及历史Tier0 manifest。该列举纠正开头概括数量，以此完整清单为准。所有判定限已读源码，不将未读动态部署配置推为不存在。
