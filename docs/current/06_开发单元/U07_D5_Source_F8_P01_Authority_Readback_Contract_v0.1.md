# U07 D5 Source / F8 / P01-G2 权威来源与读回接入合同设计 v0.1

日期：2026-10-10。实现基线：#383 `0d5969df939bf7d079fe3d4ddb35143204781951`。
状态：DESIGN_PROPOSAL / READY_FOR_TARGETED_DESIGN_REVIEW；不是冻结、实现、真实权限或合并决定。

## 1. 权威输入与适用范围

| 来源 | 精确版本及读取范围 | 采用的约束 |
| --- | --- | --- |
| RDP-01 | #290 c319c67e65a0e620e547d03514cbe45e25020975，U07_RDP01_Consumer_Inbound_Event_Admission_Contract_v0.1.md，全文 | trusted ingress、21字段binding frame、single-DB inline synthetic binding、原始eligibility issuance、alias与RESUME_REQUEST |
| RDP-02 | #271 3fe93aa2cacd2eb76e94984ae56a0967ac634ac5，U07_RDP02_F8_Business_Resume_Decision_Precedence_Contract_v0.1.md，全文 | Tier0/Tier1 precedence、statement-time finalization、共同U15 fence、历史ACCEPTED与当前effect权限分离 |
| RDP-03 | #290 c319c67e65a0e620e547d03514cbe45e25020975，U07_RDP03_State_Ownership_K09_P01_Idempotent_Apply_Trace_Contract_v0.1.md，全文 | SINGLE_GUARD_DB_STAGED_SAGA_V1、单patch Stage C、APPLIED+outbox同commit、Stage F独立dispatch grant |
| Aggregate / readiness | #291/#292文件diff | 以上是CONDITIONALLY_ACCEPTED_DESIGN；不是全部实现或无条件冻结，owner CAs仍是前提 |
| 当前代码 | #383 Foundation ledger、StateCommitter及SourceValidation/StateRepository/FieldPermission/CommitEventEvidence ports；U06 synthetic runtime；U07 repos | 来源字符串不是provenance；mechanical COMMITTED不是临床内容读回；internal evidence sink非权威 |
| 实测 | #383评论6092671481；run38016524441 | 隔离桥接8/8、全模块434总数/1 skipped；不证明canonical binding、F8 finalization或临床P01 |

不导入整个未合并设计链，也不修改它们。本文件为上述条件设计的接入层细化，未重新决定F8 precedence或APPLIED语义。真实D5仍NOT_READY。首个可实现范围建议仍是PROFILE-B隔离、无真实payload与副作用；真实source owner部署身份需要另行确认。

## 2. Owner与信任边界

| 事实 | 唯一语义owner | 接入层允许做什么 |
| --- | --- | --- |
| actor/environment/tenant/action授权 | 经认证ingress + scope/permission resolver | 接收server-side auth context、独立查询当前许可；禁止使用客户端trusted/current/applied字段 |
| canonical event身份 | Foundation ledger | resolve/replay；不产生F8 verdict |
| canonical binding与合成payload | RDP-01 U07 binding owner | 同DB事务存储binding、校验字节/digest；不得用application row替代 |
| delivery/wait/eligibility issuance | U06 | 读owner已提交证据；hash投影或checkpoint存在不是issuance |
| Question/Gap状态与消费决定 | F3 | 读Question版本与owner-issued消费决定；U07不得自己标ANSWER_RECEIVED/ANSWERED |
| Clinical State/Pending及mutation执行 | P01治理边界，G2相关授权/owner合同 | 一个被授权K09 patch、持久提交凭据及同effect读回；不等于直接调用机械committer已有临床权限 |
| expiry/cancel/supersede | U15及指定生命周期owner | 当前generation、deadline和共同guard参与证明；不由U07编造终态 |
| business verdict | F8/U07 | 不变decision、claim与时间证据；不判断医学正确性 |
| restore/parked result | P02 | 只读其权威结果；checkpoint兼容不决定F8 |
| audit/trace | P05 | 记录引用；审计、日志或internal event不能充当commit receipt |

实际服务身份、issuer registry、authority policy/revocation store、P01临床存储adapter和U15共同fence尚未指定。本设计列出逻辑owner而不捏造其部署地址或权限。参数中带owner名称的DTO仍不是可信证据。

## 3. 接入层逻辑接口 proposal

这些接口名与DTO只是设计，未注册Shared Contracts、未创建Java类或Spring bean；public construction不能签发权威。

### 3.1 SourceAuthorityResolver

`resolve(SourceReference request, AuthenticatedIngressContext serverContext, AuthorityBinding pinned) -> SourceResolution`

request仅含原始event/key/type及source/payload/target引用。serverContext必须来自已认证adapter，request不能覆盖其principal、tenant、environment/profile、actor/action或policy refs。

SourceResolution：
- status = VERIFIED / DENIED / UNAVAILABLE / INTEGRITY_CONFLICT / UNSUPPORTED_BINDING；
- VERIFIED携带issuer identity/version、server-derived scope/actor binding、granted action、policy/permission epoch、source event/version/ref、trusted payload digest、schema/hash/canonicalization版本、审计引用；
- 原始source Clinical State版本是历史绑定，当前临床版本通过owner reader单独读回，不能把客户端expected version当authority；
- USER_ANSWER仅按RDP-01获授权的PROFILE-B inline bytes合同处理；digest独立从允许读取的字节计算；设计不授权真实患者payload读取；
- RESUME_REQUEST只解析既有canonical USER_ANSWER target，不产生新answer、替换payload或独立F8评估；
- issuer/scope/action不符为DENIED；读服务不可用为UNAVAILABLE；两者均无F8/effect，但不统一伪造业务REJECTED。

### 3.2 CanonicalBindingReader

`loadByCanonicalOrScopedKey(EventReference, AuthorityBinding) -> BindingReadResult`

读原Foundation winner及u07_canonical_event_binding，在一个已验证DB事务/一致快照内核对完整binding bytes、payload bytes摘要与logical handles。返回FOUND / ABSENT / INCONSISTENT / UNAVAILABLE。只读对齐不修复缺记录。

严格沿用RDP-01 §5.1的21字段顺序、typed NULL、Unicode/字段parser和独立answer content digest；本合同不新增fingerprint字段，不把alias event ID纳入原fingerprint。alias保留trace、不覆盖原event/occurred_at；同key换payload为conflict。新ID同payload不是全局duplicate。

### 3.3 AuthoritativeWaitSnapshotReader

`read(ExactWaitKey, CanonicalBindingRef, AuthorityBinding) -> WaitSnapshotResult`

ExactWaitKey = consultation/question/pending/parent_delivered_wait_effect/eligibility。
成功快照包含：
- Consultation lifecycle、row_version、current_wait_effect；
- F3 Question ID/status/version、applicable Gap refs、F3 policy version；
- P01 CDP/Clinical version及exact Pending pointer/value/ref；
- U06 delivery confirmation、parent/child wait commit refs、原始eligibility issuance及thread/run/checkpoint origin；
- U15 terminal/deadline owner ref、monotonic generation、clock domain/precision；
- 当前owner store/transaction-domain refs及证据版本；historical governance binding refs；
- read result = COHERENT / PROVEN_MISMATCH / UNAVAILABLE / INCOHERENT / INVALID_PROVENANCE。

业务owner状态必须在common guard下重新读回。远端快照、TTL缓存或各自“最新”版本不能证明同一临界区的一致性。缺少共同事务/fence的V1返回DEPENDENCY_BLOCKED，不能用时间接近或重复读取代替。

checkpoint missing只影响P02诊断；若原issuance可信且business wait仍合法，允许既有RDP历史重附着路径。原issuance不存在时禁止U07自签eligibility。

### 3.4 F8DecisionAuthorityPort

`readCommittedDecision(canonicalAnswerId, bindingFingerprint, scope) -> HistoricalDecisionRead`
`finalizeFirstDecision(VerifiedAdmissionRef, WaitEvidenceRef, pinnedPolicy) -> DecisionCommitResult`

先读Tier0原不变decision；已有ACCEPTED但未APPLIED保持ACCEPTED。仅历史读且当前读权限有效时可返回历史信息；返回历史decision不授予新effect。

首次decision在Consultation/U15 common guard中重读owner refs，沿用RDP-02 P1..P8顺序。decision/claim必须同commit；DB最终conditional statement同时取认证statement-current时间并判deadline/generation/winner；`t_f8_linearize >= deadline`为EXPIRED。客户端时间、transaction-start时间、单独时间SELECT后无条件INSERT均不够。

DecisionCommitResult分别报告durability COMMITTED / UNKNOWN / NOT_COMMITTED、decision ref/contract/policy/input fingerprint、claim generation、owner refs、decision_effective_at与clock authority。COMMITTED之后才发布business verdict；UNKNOWN保持未知，新事务查相同ID，不盲发第二decision。

F8四种business verdict不增加第五项；owner/clock/ledger不可用用operational DEFER。DUPLICATE只在已APPLIED等价内容+同wait证明成立时判定；pending winner不够。

### 3.5 F3AnswerConsumptionPort 与 P01GovernedCommitPort

`F3.decideConsumption(CommittedF8Ref, ExactWaitKey, OwnerSnapshotRefs) -> OwnerConsumptionDecision`
`P01.prepare(OwnerConsumptionDecision, StableRootRef, AuthorityBinding) -> ImmutableProposalRef`
`P01.commitExact(ImmutableProposalRef, CurrentFenceWitness) -> GovernedCommitOutcome`
`P01.readEffect(ExactEffectQuery) -> EffectReadResult`

F3决定Question/Gap是否适用及精确Pending消费；P01依据独立capability/consent/field/source许可执行，不继承U06SyntheticP01Runtime固定producer/permission。Stage C必须在已验证P02恢复结果后才执行。

一个稳定proposal/effect/idempotency identity覆盖一个patch：Question + applicable Gap + Pending consume。同一次Clinical version advance、同一CommitResult和同一authority readback；部分projection丢失只能读取同effect，不能再补第二patch。

当前StateCommitter拒绝non-null expectedCurrentValue；V1接入采用owner transaction内exact Pending/Question读回、Clinical baseVersion检查、共同guard及最终owner条件写，以维护CAS语义，发送给机械committer的expectedCurrentValue保持null。只有实际临床adapter能证明原子内容应用及条件写才可用；不得称单个版本递增提供全部CAS。若无法实现，进入明确合同amendment，禁止绕过机械core写CDP。

## 4. authority binding与读回匹配规则

每个部署/测试profile要提供受审AuthorityBinding manifest：owner service ID/version、contract/policy版本或digest、environment/profile/tenant scope、issuer registry与permission epoch来源、state store/guard DB/transaction manager ID、readback route identity、hash/frame/time authority版本和批准的field-path集合。

manifest来自可信配置和owner证据，不接受request指定“最新版本”；启动/调用缺失、版本变化或撤销时使相关能力不可用。摘要只证明字节一致，不代替issuer认证、数据库权限、原始issuance或fence参与。历史引用保存不变；当前授权/撤销在每个新effect临界区重新验证。

| 读回对象 | 必须匹配 | 不能替代的东西 |
| --- | --- | --- |
| source/binding | event/type、scope/actor、21-field frame、payload digest/schema、原source版本 | 客户端trusted flag、单字符串RULE_DERIVED |
| F8 | canonical answer、binding fingerprint、exact wait、policy、decision ID/input fingerprint、claim generation、committed refs | D3 ACCEPTED或任意ledger receipt字符串 |
| P02 parked result | stable root/request、run/thread/wait、历史binding、owner grant/result | checkpoint存在、restore intent或resume bool |
| P01 effect | root/effect/proposal/idempotency、CDP/scope、base→after版本、authorized path set、patch/value fingerprint、F3决定、F8 winner、U15 fence、committed receipt | mechanical COMMITTED、audit/trace或internal event |
| 临床状态读回 | 同一P01 effect的Question/Gap/Pending实际内容、owner版本与after_c | 当前latest state不带effect lineage |
| owner许可 | 相同issuer/owner/environment/action、contract/policy、当前permission epoch | 曾经READY、历史同意、DTO token字段 |

readEffect返回 FOUND_MATCH / ABSENT / MISMATCH / UNAVAILABLE / INDETERMINATE；只有FOUND_MATCH的真实owner commit与内容证据可确认Stage C。无记录不证明未知提交失败；MISMATCH隔离并reconciliation，禁止APPLIED。

之后更高版本临床状态不能简单与after_c等值比较：先读exact effect的不可变commit/value projection，再读当前state用于后续许可；较新state但缺historical lineage仍UNKNOWN。历史commit已成立但被后续合法状态覆盖，不自动称历史commit失败，也不允许重新apply。

## 5. 当前持久化差距与变更纪律

#383桥接写的是canonical + u07_event_application，而RDP-01要求canonical + u07_canonical_event_binding。前者测试可复用事务机制，但不能宣称满足binding原子性。

当前MySQL/Oracle V7已被schema prototype使用；RDP-01设计中的V7 binding示例不是可直接新增的迁移名。后续基于部署/Flyway历史选择新的next-version migration；不改已发布V7、不在同一version再添文件。本轮不确定新version、不执行迁移。

F8DecisionLedger/WaitAnswerAuthority、durable issuance、P01 owner effect readback、U15 generation与Stage-C evidence尚无本轮完整实现证明。必须分别设计表/adapter与owner权限；不复用source refs、receipt_ref或application row_version字段冒充全部语义。

RDP-03的APPLIED依赖P02、单patch Stage C、Consultation Stage D和同commit唯一U02 outbox Stage E。P01 readback成功只确认Stage C。真实consumer ACK是后续进度；Stage F发送还需独立U15 grant。此合同不把Stage C称为APPLIED，也不重新设计P02/U02。

## 6. 验收合同（未来测试，未执行）

| ID | 情况 | 必须结果 |
| --- | --- | --- |
| AUTH-01 | 客户端伪造actor/tenant/trusted/applied | 来源拒绝，零F8/effect |
| AUTH-02 | source timeout vs DENIED | operational UNAVAILABLE与DENIED分别保留，零业务假verdict |
| AUTH-03 | 同key alias / payload更换 / RESUME_REQUEST无target | 同binding重附着；更换conflict；无target unresolved，无新answer |
| AUTH-04 | canonical或binding单表缺失 / 字节digest不符 | INCONSISTENT，零handoff；事务失败两表回滚 |
| AUTH-05 | 原issuance缺失 / checkpoint缺失 | 前者阻断；后者按可信历史路径交P02诊断，不改F8 |
| AUTH-06 | U15未共享fence / owner snapshot混代 | DEPENDENCY_BLOCKED/DEFER；零ACCEPTED或新patch |
| AUTH-07 | deadline相等 / transaction-start clock / commit响应未知 | certified equality EXPIRED；坏clock DEFER；UNKNOWN新事务查同decision |
| AUTH-08 | 历史ACCEPTED未APPLIED / 后来cancel | 历史保留；当前effect禁止；pending winner非DUPLICATE |
| AUTH-09 | P01机械COMMITTED无内容receipt / mismatch / 更高当前版本 | 不确认Stage C；exact effect lineage读回；禁止重apply |
| AUTH-10 | Question/Gap projection存在、Pending projection丢失 | 同effect读回并修projection；零第二patch/版本增量 |
| AUTH-11 | effect换scope/producer/proposal/path / 当前permission revoked | conflict或denied；历史记录不授新权限 |
| AUTH-12 | 不允许clinical-observations/Facts路径 / expectedCurrentValue非null | 被权限/core拒绝；禁止绕过；无Clinical Truth写入 |
| AUTH-13 | Stage C提交未知 / rollback-only / 任意readback不可用 | 新事务exact effect reconciliation；不盲重试、不APPLIED |
| AUTH-14 | Stage C已证实但P02/D/E缺证据 | 仅C确认；不APPLIED、不dispatch |

实现oracle由owner合同独立生成，不能用SUT输出推导期望；分别验证MySQL/Oracle与实际adapter。以上测试不是已有CI PASS，#383仅提供桥接基础。

## 7. 进入条件与下一步

| 条件 | 本轮状态 |
| --- | --- |
| 本接入合同 | PROPOSED，待针对性设计审查 |
| Foundation/JPA/JDBC最小事务桥接 | ISOLATED_TEST_PASS，不是全部guard resources证明 |
| 实际issuer/readback/store/owner许可版本 | NOT_BOUND |
| U06 eligibility issuance CA | 保留CA-U06-U07-ELIG-ISSUANCE-01，未由本轮关闭 |
| U15/F8 shared fence与time authority | 保留CA-U07-RDP02-U15-SHARED-FENCE-01 |
| F3 answer bridge与P01共享commit | 保留CA-U07-RDP03-F3-ANSWER-BRIDGE-01、CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 |
| P02 grant、landing、U02 consumer与dispatch CAs | 保留原RDP前提，本轮未验证或关闭 |
| 真实D5 readiness | NOT_READY；不授权真实payload、临床、dispatch、生产或merge |

下一步针对性设计审查只聚焦：source身份/21字段binding、F8时间与共同fence、P01单patch/CAS与exact effect读回。通过后优先实施一个隔离source/binding+readback adapter切片；采用test issuer与nonclinical store，并保留真实owner未绑定为fail-closed。真实issuer部署/许可/store信息应在真实接入前由owner确认，而不是为了本轮设计要求用户提供凭据。

本次未运行新测试、未修改源码/Shared Contracts/迁移、未替代冻结或条件设计文件；只提供可审阅接入合同，不将设计完成写作真实D5完成。
