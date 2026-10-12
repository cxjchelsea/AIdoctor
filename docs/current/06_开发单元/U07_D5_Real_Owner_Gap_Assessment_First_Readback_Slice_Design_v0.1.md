# U07 D5 真实Owner接入差距评估与首个接入切片设计 v0.1

日期2026-10-12；固定main750770e81af79b764cc4148d7fa856363c89972a。本交付仅设计，不改实现/DDL/生产配置、不运行真实业务数据库、不合并。SOURCE_INSPECTION_COMPLETE；FIRST_SLICE_DESIGN=READY_FOR_TARGETED_DESIGN_REVIEW；REAL_D5=NOT_READY。

## 1. 已有基线与结论

#405/#408已合main。#411合并后104 F8+25 Source=129项通过，仅证明disposable synthetic Source/F8与故障范围。真实接入仍需要owner权威、实际Spring事务拓扑、action权限和readback合同，不能把tools/f8_owner复制成生产owner。

本轮读取main完整Git路径树及13份具体源码/DDL/既有manifest。以下是可见源码判断，不是全仓语义扫描或实际Spring proxy/bean/manager/数据库认证。旧Tier0 manifest描述旧HEAD的历史门禁，不能直接当当前main的运行时结论。类或方法有@Transactional不证明其当前被代理调用或各模块处于同一事务。

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

### 3.2 输入与权限

请求输入：consultationId、threadId、runId、checkpointId；actor/tenant来自可信ScopeAuthorizationPort，不接受请求自行声称授权。ScopeAuthorizationPort.evaluate(request)返回ALLOW/DENY/UNAVAILABLE及与这些ID绑定的scope attestation；UNKNOWN默认拒绝。
当前真实tenant→consultation、actor委托关系及许可epoch不在六表中，禁止把clinical_consultation.user_id直接当actor或tenant。D5-O1仅允许NONPRODUCTION_SYNTHETIC fixture授权port，独立label标识TEST_SCOPE；production profile无可用authorizer时返回UNAVAILABLE，不暴露任何row数据。这一测试策略不声称真实scope owner已完成。

输入ID非空/长度及枚举验证；不复用BindingCodec的synthetic前缀作为真实ID合同。输出不含problem_text、subject reference、答案或CDP JSON。日志仅reason code及非敏感fixture correlation ID。

### 3.3 事务与一致性

Reader拥有一个read-only事务入口，拒绝既存外层事务；使用固定单DataSource/JDBC transaction manager，REPEATABLE_READ的同一snapshot。必要的全部查询在同一物理connection内完成；不使用FOR UPDATE/LOCK IN SHARE MODE，不锁生产writer。read-only标志不是权限防护，以SELECT-only账号阻止DML。

不能把该snapshot跨服务当全局原子状态；六表当前处于同一disposable MySQL schema，仅覆盖此拓扑。无法建立同connection/snapshot→UNAVAILABLE。查询返回后snapshot已结束，结果不是未来finalize授权，不能缓存为可复用eligibility。时钟不来自caller，不补造deadline：缺实际deadline时始终authority UNKNOWN。

从可获授权的consultation读取及请求IDs单独读取其他表，再做显式一致性检查；不能全部INNER JOIN后将损坏关联隐藏为正常ABSENT。多行/冲突独立归类INCONSISTENT；SQL必须parameterized，拒绝拼接请求字段为表名。

### 3.4 核验关系和结果

1. consultation存在；binding.consultation/cdp与consultation一致，binding.thread与请求一致，runtime_authority受允许fixture manifest限定。
2. thread.consultation与binding一致；run.consultation/thread与binding一致；仅检查run存在与关联，不擅定OPEN/CLOSED状态的F8可恢复语义。
3. consultation.current_wait_effect_id等于waitEffect.wait_effect_id；waitEffect.consultation匹配，committed_lifecycle=WAITING_USER且effect_status在冻结fixture允许集合；需由读取Record定义确定确切枚举，不能靠表名推断。
4. thread current_run/checkpoint/wait分别与run/checkpoint/parent delivered wait effect一致；thread必须AWAITING_USER；checkpoint为ACTIVE。
5. checkpoint.consultation/thread/run匹配；checkpoint.consultation_wait_effect_id与consultation指针相同；question和delivery与waitEffect匹配；parent wait与waitEffect.parent_delivered_wait_effect_id匹配。
6. consultation.row_version与waitEffect.committed_row_version：相等可支持一致fixture；较新版本或current pointer变化不得默认合法，返回STALE/INCONSISTENT，并要求后续owner合同决定。checkpoint clinical_state_version仅标为CAPTURED_VERSION，不声称CDP当前version。

输出WaitChainObservation：
structureStatus={MATCH,ABSENT,STALE,INCONSISTENT,DENIED,UNAVAILABLE}；
authorityStatus恒UNKNOWN；
scopeEvidenceKind=TEST_SCOPE或NONE；
sourceHead/schemaManifestRef、各对象ID/rowVersion、capturedClinicalVersion和reasonCode。
没有ACCEPTED/DUPLICATE/EXPIRED或COMMITTED/APPLIED业务receipt；没有canFinalize=true字段。MATCH只指所列等待链结构，action/eligibility/delivery/APPLIED/当前clinical version仍UNKNOWN。

ABSENT仅授权scope中根consultation没有记录；consultation存在而关联缺失→INCONSISTENT，部分过渡如checkpoint WAIT_CHECKPOINTED→STALE，DB/权限port不可用→UNAVAILABLE。DENIED不携带对象snapshot。结论顺序：输入→scope→资源→存在性/唯一性→关联→阶段状态。

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

## 5. 验收与实施就绪条件

必须先定向设计审查，核对结果类型、scope stub限制、实际effect枚举、迁移加载顺序与账号权限。通过后实施清单：
- reader与immutable observation/typed reasons、test-only ScopeAuthorizationPort；
- disposable现有DDL加载manifest及SELECT-only权限fixture，不改原迁移；
- 单snapshot真实MySQL测试与同connection观测；
- 验证workflow明确仅独立schema，不允许任意外部DB URL。

测试至少：合法MATCH但authority UNKNOWN；根不存在；每个关联分别缺失；跨consultation/thread/run/CDP和错parent/question/delivery；WAIT_CHECKPOINTED中间态；current wait已移动；rowVersion较新；重复关联/无约束模拟损坏；拒绝scope/授权port unavailable/外层事务/错误DS；两个独立connection并发修改验证单snapshot一致性（通过barrier不用时间猜测）；SELECT-only DML拒绝及reader前后六表行数/关键bytes不变；MATCH永不产生F8 decision/claim、eligible flag或APPLIED receipt。继承129基线而不误称本slice已执行。

REAL_OWNER_SCHEMA_READBACK_DESIGN_READY_FOR_REVIEW；D5-O1实施READY仅在设计review后判定。当前没有新测试结果、实现授权、合并授权或真实数据访问。下一步：D5-O1定向设计审查，之后隔离现有Owner表读回实现与验证。

## 6. 本轮具体源码依据

读取当前HEAD的：ConsultationRecord、ConsultationWaitTransitionService、U06WaitCoordinator、U07ResumeEligibilityProjector、RuntimeThreadWaitTransitionService、RuntimeWaitCheckpointRecord/Service、RuntimeBindingRecord、ClinicalRunRecord、JdbcU06DeliveryStore、ClinicalCdpStateRepositoryAdapter、U06SyntheticP01Runtime、CommitEventEvidencePort、V6迁移，以及历史Tier0 manifest。该列举纠正开头概括数量，以此完整清单为准。所有判定限已读源码，不将未读动态部署配置推为不存在。
