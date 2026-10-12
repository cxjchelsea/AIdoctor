# U07 D5 真实Owner接入差距评估与首个接入切片设计 v0.2

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

schema bootstrap在新disposable schema设置default_storage_engine=InnoDB并按V2→V3→V6加载未改原DDL；manifest记录固定source SHA750770e8全文、三DDL的实际Git blob/digest、六表字段/PK/unique shape。Reader startup preflight使用reader账号验证数据库版本、六表存在、ENGINE=InnoDB、shape与manifest一致；schema/engine/版本不匹配→UNAVAILABLE/RESOURCE_PRECONDITION_FAILED，不返回任何MATCH。metadata查询仅固定schema，不列举其他库。

每次业务读在同一transaction-bound physical connection上检查autoCommit=false、connection.getTransactionIsolation()==TRANSACTION_REPEATABLE_READ和固定catalog；JdbcTemplate必须复用此connection。连接漂移、错误manager/DS、连接或SQL异常→UNAVAILABLE/RESOURCE_PRECONDITION_FAILED或DB_UNAVAILABLE，payload为空。runtime readOnly标志不作为DML权限证明。

所有owner读取用普通parameterized SELECT；无FOR UPDATE、LOCK IN SHARE MODE或其他locking/current read。snapshot在首次clinical_consultation普通一致性读时建立，不声称BEGIN时建立；schema metadata preflight不作为snapshot建立证据。后续binding/thread/run/当前wait effect/请求checkpoint独立读取，不使用INNER JOIN吞掉缺对象。读取当前指向而非只用请求指向；PK对象0/1、多行逻辑必须显式检查。

Reader没有业务DML；GRANT只六张目标表SELECT，不授全schema SELECT/任何INSERT/UPDATE/DELETE、DDL或F8访问。root fixture仅用于加载原DDL/种合成数据/独立writer测试，永不作为reader DS。结果离开txn后立即只属历史观察，不作为F8 finalize缓存、permission或eligibility；六表同一实例保证不是跨服务全局事务认证。

### 3.4 精确关系、分类矩阵与返回值（O1-B01）

StructureStatus={MATCH,ABSENT,STALE,INCONSISTENT,INVALID_INPUT,DENIED,UNAVAILABLE}。authorityStatus恒UNKNOWN；MATCH仅表示该冻结六表结构。Payload仅MATCH包含六表最小结构IDs、consultation/thread rowVersion及checkpoint capturedClinicalVersion；其他status无row payload。Envelope仅status/reason、authority UNKNOWN及固定非敏感schema/source manifest引用，scopeEvidenceKind仅成功授权后TEST_SCOPE，否则NONE；不输出problem_text/user_id、subject refs、答案或CDP JSON。无COMMITTED/APPLIED/F8 verdict/canFinalize字段。

按下表序号执行，第一条匹配即返回；scope未通过前零SQL。允许ID均在TEST_SCOPE批准四元组内。表中的“缺失”指普通snapshot查询的0行；未知枚举永不自动当新业务状态。

| 序号 | 条件 | status / reason |
| --- | --- | --- |
| 0 | profile/URL不批准 | 工厂配置异常CONFIG_REJECTED，零连接/SQL |
| 1 | Request/ID无效 | INVALID_INPUT / INPUT_INVALID |
| 2 | scope DENY/证据错绑定或port不可用 | 按3.2返回DENIED或UNAVAILABLE |
| 3 | 外层事务、物理/隔离/schema前提或DB不可用 | UNAVAILABLE / 对应资源原因 |
| 4 | consultation不存在 | ABSENT / CONSULTATION_ABSENT；不读取其他对象 |
| 5 | consultation rowVersion null/负数或lifecycle不在ACTIVE/WAITING_USER | INCONSISTENT / CONSULTATION_SHAPE_INVALID |
| 6 | consultation ACTIVE且current_wait_effect_id为空 | STALE / NO_CURRENT_WAIT；正常非等待状态，不要求未来checkpoint存在 |
| 7 | ACTIVE却current wait非空，或WAITING_USER但current wait为空 | INCONSISTENT / CONSULTATION_WAIT_SHAPE_INVALID |
| 8 | binding/thread/request run不存在或错关联、重复结果；未知runtime_authority/run/thread枚举 | INCONSISTENT / OWNER_LINK_INVALID或OWNER_ENUM_INVALID |
| 9 | current wait effect不存在或错consultation、effect状态/版本非法 | INCONSISTENT / WAIT_EFFECT_INVALID |
| 10 | thread ACTIVE且全部current_run/checkpoint/wait指针为空 | STALE / THREAD_WAIT_NOT_ESTABLISHED；允许consultation已等待而runtime尚未预留的部分阶段，不读取未来checkpoint |
| 11 | thread ACTIVE残留任一current指针；WAIT_CHECKPOINTED/AWAITING_USER缺任一指针或指向对象不存在 | INCONSISTENT / THREAD_WAIT_SHAPE_INVALID或CURRENT_CHECKPOINT_MISSING |
| 12 | 请求run/checkpoint不是thread当前指针 | STALE / REQUEST_NOT_CURRENT；只在当前checkpoint确实存在且scope关联一致后返回，不隐藏dangling current指针 |
| 13 | 当前/request同一checkpoint关联/状态/version非法；错parent/question/delivery等硬冲突 | INCONSISTENT / CHECKPOINT_LINK_INVALID |
| 14 | consultation.rowVersion小于effect.committedRowVersion | INCONSISTENT / VERSION_REGRESSION |
| 15 | consultation.rowVersion大于effect.committedRowVersion | STALE / CONSULTATION_ADVANCED |
| 16 | thread WAIT_CHECKPOINTED且其余硬关系一致 | STALE / THREAD_WAIT_CHECKPOINTED |
| 17 | 所有MATCH必要条件满足 | MATCH / WAIT_CHAIN_MATCH |
| 18 | 未覆盖/未知情况 | INCONSISTENT / UNSUPPORTED_SHAPE；不得fallback MATCH |

binding必须consultation/cdp等于consultation且thread等于请求thread；runtime_authority固定CLINICAL_RUNTIME_V1；scope_version/capability_set_version/contract_version与fixture manifest精确一致。run.consultation/thread与binding相同，run.status允许OPEN/CLOSED，只核对已知值和关联，不宣称F8可恢复；based_on_clinical_state_version非负且与checkpoint captured version相等仅作为此fixture冻结关系，不读取CDP当前临床版本。

effect必须COMMITTED、expected_prior_lifecycle ACTIVE、committed_lifecycle WAITING_USER；priorRowVersion>=0，committedRowVersion=prior+1（检查overflow）；question/delivery/parent IDs非空。此关系基于当前单saveAndFlush的fixture，不宣称未来所有writer同一版本演进。current effect从consultation指针读取。

checkpoint必须ACTIVE、capturedClinicalVersion>=0，consultation/thread/run与当前binding/thread/run一致；consultation_wait_effect_id=consultation.current_wait_effect_id；question_id/delivery_id=effect对应字段；question_delivered_wait_effect_id=effect.parent_delivered_wait_effect_id=thread.current_wait_effect_id。thread current checkpoint/run与被读当前对象一致。pending_question_ref/selection/delivery/confirmation refs及fingerprint要求非空，但只作为结构字段，不视为question当前事实、实际receipt或digest独立验证。

MATCH另要求consultation WAITING_USER、thread AWAITING_USER、consultation.rowVersion=effect.committedRowVersion。WAIT_CHECKPOINTED只属于thread；checkpoint没有该枚举。请求旧checkpoint允许STALE，不因其不是current便将正常current链改称损坏。实现应读取/核验当前checkpoint和当前run的scope后再区分请求是否current；请求run错scope仍按8失败，避免跨scope请求藏入STALE。

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
1. 矩阵各独立条件及优先级组合：MATCH仍authority UNKNOWN；invalid input、根ABSENT、ACTIVE无wait、ACTIVE残留指针、thread阶段、每类missing硬指针、旧请求、各版本方向/overflow、未知枚举及所有关联错scope。非MATCH全部无row payload。
2. scope任一字段替换/缺失、未知schema/kind/profile/manifest、任意ALLOW企图绕过production/错URL、port异常；通过connection/query计数证实前置拒绝零连接/SQL。authority UNKNOWN为不可修改结果常量。
3. schema six-engine/shape不匹配、错误manager/DS、outer事务/autoCommit/隔离/connection漂移；真实路径与故障代理分开标记。
4. MVCC barrier：reader完成首次consultation普通consistent SELECT后通知writer；独立writer UPDATE同时改变consultation指针/版本和thread指针并COMMIT，writer通知reader继续；本reader其余SELECT仍观察旧完整snapshot，返回旧MATCH；下一新reader观察新链的相应状态。barrier设有timeout/finally释放，不依靠sleep或connection ID单独证明MVCC。
5. 保留PK/unique；跨关联损坏利用现有无FK结构fixture。重复结果只用现有非唯一业务字段查询或纯classifier测试，不删除owner约束制造“重复PK实测”。
6. reader INSERT/UPDATE/DELETE/DDL被真实DB拒绝；读取前后六表关键列的canonical摘要相同，摘要在fixture内部比对不打印敏感数据。静态依赖/SQL清单及账号无F8表权限证明无F8调用能力；不查询不存在f8表冒充行数0。
7. 不做生产profile数据库执行、真实数据、writer共享guard认证、临床current CDP/APPLIED/readmission/effect/outbox。129基线为已有实测，不称新slice已执行。

## 5.1 修订状态与逐项响应

| Finding | 本次修订 | 状态 |
| --- | --- | --- |
| O1-B01 | 增INVALID_INPUT；冻结lifecycle/effect/thread/checkpoint及版本规则、排序矩阵和中间态；修正WAIT_CHECKPOINTED归属 | REMEDIATED_FOR_RE_REVIEW，非已关闭 |
| O1-B02 | immutable TEST_SCOPE字段+精确绑定检查、前置零SQL、authorizer不可信输入禁止、production/未知profile与非精确URL工厂硬拒绝 | REMEDIATED_FOR_RE_REVIEW |
| O1-R01 | MySQL/InnoDB/实际隔离/单connection前提、原DDL顺序与digest、13文件inventory、barrier及约束保留测试 | REMEDIATED_FOR_RE_REVIEW |

DESIGN_V02=TARGETED_REMEDIATION_COMPLETE；IMPLEMENTATION_READINESS=RE_REVIEW_PENDING，未授权实施或合并。没有新测试执行或运行时认证；O-B01..06真实接入门禁均不变。下一步v0.2定向设计复审，之后才决定D5-O1隔离实施。

## 6. 本轮具体源码依据

读取当前HEAD的：ConsultationRecord、ConsultationWaitTransitionService、U06WaitCoordinator、U07ResumeEligibilityProjector、RuntimeThreadWaitTransitionService、RuntimeWaitCheckpointRecord/Service、RuntimeBindingRecord、ClinicalRunRecord、JdbcU06DeliveryStore、ClinicalCdpStateRepositoryAdapter、U06SyntheticP01Runtime、CommitEventEvidencePort、V6迁移，以及历史Tier0 manifest。该列举纠正开头概括数量，以此完整清单为准。所有判定限已读源码，不将未读动态部署配置推为不存在。
