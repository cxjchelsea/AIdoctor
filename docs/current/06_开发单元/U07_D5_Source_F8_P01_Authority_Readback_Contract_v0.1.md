# U07 D5 Source / F8 / P01-G2 权威来源与读回接入合同设计 v0.2

日期：2026-10-10。实现基线：#383 `0d5969df939bf7d079fe3d4ddb35143204781951`。
修订依据：#385 `3529b0dbdc2511f52de8f8d5b375f753cb701eaa`，B01/R01/R02；替代本文件v0.1接入细节，不替代原RDP。
状态：TARGETED_REMEDIATION_PROPOSED / READY_FOR_TARGETED_RE_REVIEW；不是冻结、实现、真实权限或合并决定。

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
- read result = COHERENT / PROVEN_MISMATCH / UNAVAILABLE / INCOHERENT / INVALID_PROVENANCE / DEPENDENCY_BLOCKED。

业务owner状态必须在common guard下重新读回。远端快照、TTL缓存或各自“最新”版本不能证明同一临界区的一致性。缺少共同事务/fence的V1返回DEPENDENCY_BLOCKED，不能用时间接近或重复读取代替。

checkpoint missing只影响P02诊断；若原issuance可信且business wait仍合法，允许既有RDP历史重附着路径。原issuance不存在时禁止U07自签eligibility。

### 3.4 F8DecisionAuthorityPort

`readCommittedDecision(canonicalAnswerId, bindingFingerprint, scope) -> HistoricalDecisionRead`
`finalizeFirstDecision(VerifiedAdmissionRef, WaitEvidenceRef, pinnedPolicy) -> DecisionCommitResult`

先读Tier0原不变decision；已有ACCEPTED但未APPLIED保持ACCEPTED。仅历史读且当前读权限有效时可返回历史信息；返回历史decision不授予新effect。

首次decision在Consultation/U15 common guard中重读owner refs，沿用RDP-02 P1..P8顺序。decision/claim必须同commit；DB最终conditional statement同时取认证statement-current时间并判deadline/generation/winner；`t_f8_linearize >= deadline`为EXPIRED。客户端时间、transaction-start时间、单独时间SELECT后无条件INSERT均不够。

DecisionCommitResult按§10封闭组合分别报告attempt与durability NOT_ATTEMPTED / COMMITTED / UNKNOWN / NOT_COMMITTED、decision ref/contract/policy/input fingerprint、claim generation、owner refs、decision_effective_at与clock authority。外层事务COMMITTED之后才发布business verdict；UNKNOWN保持未知，新事务查相同ID，不盲发第二decision。

F8四种business verdict不增加第五项；owner/clock/ledger不可用用operational DEFER。DUPLICATE只在已APPLIED等价内容+同wait证明成立时判定；pending winner不够。

### 3.5 F3AnswerConsumptionPort 与 P01GovernedCommitPort

`F3.decideConsumption(CommittedF8Ref, ExactWaitKey, OwnerSnapshotRefs) -> OwnerConsumptionDecision`
`P01.prepare(OwnerConsumptionDecision, StableRootRef, AuthorityBinding) -> PrepareResult`
`P01.commitExact(ImmutableProposalRef, CurrentFenceWitness) -> GovernedCommitOutcome`
`P01.readEffect(ExactEffectQuery) -> EffectReadResult`

F3决定Question/Gap是否适用及精确Pending消费；P01依据独立capability/consent/field/source许可执行，不继承U06SyntheticP01Runtime固定producer/permission。Stage C必须在已验证P02恢复结果后才执行。

commitExact的外层提交与durable幂等协议见§8；core返回COMMITTED是暂存值，不能直接发布GovernedCommitOutcome成功。

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
| 本接入合同 | TARGETED_REMEDIATION_PROPOSED，B01/R01/R02待复审 |
| Foundation/JPA/JDBC最小事务桥接 | ISOLATED_TEST_PASS，不是全部guard resources证明 |
| 实际issuer/readback/store/owner许可版本 | NOT_BOUND |
| U06 eligibility issuance CA | 保留CA-U06-U07-ELIG-ISSUANCE-01，未由本轮关闭 |
| U15/F8 shared fence与time authority | 保留CA-U07-RDP02-U15-SHARED-FENCE-01 |
| F3 answer bridge与P01共享commit | 保留CA-U07-RDP03-F3-ANSWER-BRIDGE-01、CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 |
| P02 grant、landing、U02 consumer与dispatch CAs | 保留原RDP前提，本轮未验证或关闭 |
| 真实D5 readiness | NOT_READY；不授权真实payload、临床、dispatch、生产或merge |

下一步针对性设计复审聚焦§8–11的B01外层提交/幂等、R01可信签发/复验、R02封闭结果组合，同时核对原source/F8/P01约束未改变。通过后优先实施一个隔离source/binding+readback adapter切片；采用test issuer与nonclinical store，并保留真实owner未绑定为fail-closed。真实issuer部署/许可/store信息应在真实接入前由owner确认，而不是为了本轮设计要求用户提供凭据。

本次未运行新测试、未修改源码/Shared Contracts/迁移、未替代冻结或条件设计文件；只提供可审阅接入合同，不将设计完成写作真实D5完成。


## 8. B01：Stage C 外层提交、幂等与恢复协议

本节指定下一Stage-C adapter的必要合同，不表示现有机械core或#383已实现它。选定一个guard DB、一个DataSource/transaction manager，owner content mutation、不可变effect receipt、durable idempotency reservation/completion共享同一物理事务。map、独立store、REQUIRES_NEW完成或提前发出的internal event不能充当完成依据。启动必须检查参与资源与profile；不支持共享资源则DEPENDENCY_BLOCKED。

### 8.1 正常路径与发布点

1. adapter拥有最外层事务；已有外层事务时只允许返回内部provisional handle，交最外层拥有者完成发布。不能把“加入事务的方法返回”解释为提交完成。
2. 在共同Consultation/U15及permission epoch guard下重新验证当前许可、F8 winner、P02、F3决定、Clinical baseVersion/exact Pending。锁顺序固定并由所有相关owner共同遵守。
3. 在同事务reserve稳定idempotency identity及fingerprint。state repository adapter执行唯一Question/Gap/Pending patch、条件版本增量，并在同事务写完整effect receipt/immutable value projection；写receipt失败使事务rollback-only。reservation、mutation和receipt不能分拆提交。
4. 机械core返回COMMITTED只暂存。durable IdempotencyPort.complete只写同事务投影，originalResult须绑定同effect receipt，不能另行commit。core内部emit事件只作非权威诊断，接入层不得订阅它发布成功或dispatch。
5. adapter在提交前核对receipt、idempotency状态及事务rollback-only；flush后仍仅暂存。只有最外层transaction manager报告成功提交，才生成durability=COMMITTED的外部结果。commit/flush/rollback异常不得把暂存result向调用方升级；确定回滚为NOT_COMMITTED，不确定为UNKNOWN。
6. 即使提交确认成功但返回响应丢失，后续请求按相同effect/key新事务读回。receipt证明Stage C，不证明整个APPLIED；D/E/F条件保留。

### 8.2 complete失败与状态转换

| 同事务状态 | 允许转换 | 对外意义 |
| --- | --- | --- |
| ABSENT | reserve → RESERVED（仍未提交） | 不代表完成 |
| RESERVED且无receipt | mutation + receipt + complete → COMPLETED | 全部外层提交后才确认 |
| RESERVED且有exact receipt | complete投影失败时可保留RESERVED并提交 | receipt确认effect；completion projection=PENDING，不得重mutation |
| COMPLETED且exact receipt匹配 | 读取原result | 只重放已提交历史 |
| 任意暂存状态，外层rollback | 回到该事务前持久状态 | 无本次完成或版本增量 |
| COMPLETED但无receipt / receipt不匹配 | 隔离为INCONSISTENT | 禁止按completed重放或重apply |

core捕获complete的RuntimeException不是事务成功证明。adapter必须在返回core后检查持久投影及rollback-only：若数据库异常已污染事务，整个事务回滚；不能清除rollback-only。只有事务仍有效、reservation+receipt已完整写入、complete失败限定为尚未写入完成投影的可恢复故障，才允许owner effect提交并保留RESERVED。这是adapter须注入验证的路径；不支持则一律回滚，不声称所有数据库故障都可继续提交。

RESERVED状态必须携带stable effect/proposal/key、fingerprint和owner domain；不是超时即释放的lease。外层已回滚可在新事务且确认原事务结束后看到ABSENT；在途/未知时不得释放后重写。普通core release只可操作本事务未产生receipt的reservation；receipt存在则禁止删除reservation。

### 8.3 新事务恢复顺序

先认证当前读权限；在新事务按exact effect query读取receipt与不可变内容，再读取同域幂等投影，核对scope/proposal/patch/versions/fence和fingerprint。FOUND_MATCH可确认历史Stage C；若RESERVED，唯一允许修复是同effect COMPLETED投影，不调用StateCommitter，不增Clinical version，不重做Question/Gap/Pending。修复需独立reconciliation许可，共同guard、receipt复验及compare-and-set；历史读权限不授予该写许可。

UNKNOWN提交之后单次ABSENT不证明失败。保持UNKNOWN并查询同identity；仅当owner可证明原事务已经终止、查询为认证primary/一致读且无receipt/完成记录，才可归类NOT_COMMITTED；再次尝试仍必须使用原identity、重验当前effect权限和条件。COMPLETED无receipt或fingerprint矛盾为INCONSISTENT，人工/受控reconciliation，不盲重试。

当前latest Clinical state已被后续合法effect覆盖时，按不可变receipt/value projection确认历史；缺exact lineage保持INDETERMINATE。projection修复仅限receipt/幂等派生记录，绝不能补第二次临床patch。

## 9. R01：隔离签发与consumer复验拓扑

下一切片固定PROFILE-B/nonclinical store。受控测试composition root加载受审manifest，构造package-private TestAuthorityIssuer及RecordVerifier；发行factory不暴露给request handler。它们不是实际生产issuer，生产profile禁止注册；真实owner尚未绑定时相关能力UNSUPPORTED_BINDING/DEPENDENCY_BLOCKED。

| 组件 | 写入/输入权限 | 输出及消费规则 |
| --- | --- | --- |
| 可信composition root | 只读受控manifest；request不得替换registry/route/epoch source | pinned manifest digest和已注册issuer/profile |
| TestAuthorityIssuer | 仅测试服务身份可写owner evidence namespace；请求只能提供untrusted refs | 验证synthetic原始source/issuance后提交不可变record，返回opaque ref |
| Admission/binding adapter | 通过配置route读取已提交record；无issuer namespace写权限 | 复验后原子存canonical+21字段binding及source record ref |
| F8/F3/P01 consumer | 每次按ref通过受控reader读取record；不信caller DTO字段 | 校验record type、issuer/version、profile/environment/tenant/actor/action、contract/policy、manifest、完整fingerprint与scope/root/wait/effect |
| Owner snapshot reader | 在共同guard内读取实际owner rows，关联已验证issuance | 返回事务限定witness；持久历史ref不替代当前状态检查 |

SourceResolution.VERIFIED、VerifiedAdmissionRef、OwnerConsumptionDecision及CommittedF8Ref均为传输载体。consumer必须重新读取类型对应的已提交record，核对issuer registry及与canonical binding/proposal的关联；单凭status=VERIFIED、猜中的ID、DTO构造器或相同进程对象均不能通过。不存在、错误record type、其他tenant/effect引用、错误issuer/profile、policy/contract不符分别返回拒绝或完整性错误，零handoff。确实的依赖不可读返回UNAVAILABLE，不能伪装DENIED业务事实。

record至少保存不可变record ID/type、issuer identity/version、environment/profile/scope/actor/action、contract/policy/manifest digest、原始source/issuance refs及版本、typed identity fingerprint、payload/patch digest、issuance epoch和owner commit ref。其digest不是认证：可信来自受控签发路径、namespace写权限、配置绑定的读取route及关联核对。测试只有synthetic字节；不得以测试issuer签发真实owner许可。

permission/revocation状态在guard DB中由有权限的owner更新；新effect事务锁定同scope/action的epoch行并复验当前epoch/许可，撤销操作也必须取得同锁并更新generation。二者序列化：撤销先提交则effect拒绝；effect先取得并提交，则历史effect保留，后续effect拒绝。common U15 generation仍按原合同参与最终条件写。若实际authority远端且不能与guard mutation共享此协议，V1为DEPENDENCY_BLOCKED；远端bool、缓存TTL或提交前precheck不替代它。

witness只在拥有该guard的事务内有效，不能缓存给下个事务。历史record允许在当前读权限下读取；新effect必须复验当前permission epoch，旧record或历史F8 ACCEPTED不授权。下一切片仅证明test issuer/ref-verifier/binding机制；F3/P01/U15实际owner未实现部分继续显式阻断。

## 10. R02：封闭结果与字段约束

每个结果必须标明kind，未知enum/违反字段组合为INVALID_RESULT并fail-closed，不能默认成功。所有read port有readStatus；所有write port有attempt与durability；NOT_ATTEMPTED是durability的独立值，不能偷换为NOT_COMMITTED。candidateQuery是诊断查询键，永远不是committed ref。

### 10.1 读取与准备结果

| Port/kind | 完整合法status集合 | 成功必有字段；失败可有字段 | 禁止字段/后续 |
| --- | --- | --- | --- |
| SourceResolution | VERIFIED / DENIED / UNAVAILABLE / INTEGRITY_CONFLICT / UNSUPPORTED_BINDING | VERIFIED必须verifiedRecordRef+完整§9关联；其余仅reason/query refs | 非VERIFIED无verified evidence；任何状态无F8 verdict |
| BindingReadResult | FOUND / ABSENT / INCONSISTENT / UNAVAILABLE | FOUND必须canonical+完整binding+source record lineage；其余仅diagnostics | 无business verdict；缺表不能FOUND |
| WaitSnapshotResult | COHERENT / PROVEN_MISMATCH / UNAVAILABLE / INCOHERENT / INVALID_PROVENANCE / DEPENDENCY_BLOCKED | 前两者必须可信owner refs+same-guard witness；MISMATCH必须具体事实及版本；其余无完整witness | 不携带business verdict；非前两者不得首次finalize |
| HistoricalDecisionRead | FOUND_MATCH / ABSENT / MISMATCH / UNAVAILABLE / INDETERMINATE | FOUND_MATCH必须不可变decision、适用claim关系、clock/input/policy refs | 无新effect permission；其余无confirmed decision |
| OwnerConsumptionDecision | ISSUED / DENIED / UNAVAILABLE / DEPENDENCY_BLOCKED / INTEGRITY_CONFLICT | ISSUED必须owner record ref及F8/wait/patch-scope关联 | 非ISSUED无proposal授权 |
| PrepareResult | PREPARED / DENIED / UNAVAILABLE / DEPENDENCY_BLOCKED / INTEGRITY_CONFLICT | PREPARED必须immutable proposal/effect/key及digest | 准备不改变state、不携带committed receipt |
| EffectReadResult | FOUND_MATCH / ABSENT / MISMATCH / UNAVAILABLE / INDETERMINATE | FOUND_MATCH必须exact receipt/value projection及versions；其余仅query/diagnostics | ABSENT不证明未知commit失败；latest不能代exact lineage |

COHERENT仅说明输入证据一致，可能最终EXPIRED/REJECTED。PROVEN_MISMATCH是可信业务事实，交F8原precedence评估：更高优先级已applied等价或expiry等仍先判断，不能由admission提前强判REJECTED。INVALID_PROVENANCE、INCOHERENT、不可用或DEPENDENCY_BLOCKED无可信业务输入，operational DEFER。Tier0历史查询独立，不要求当前wait仍匹配；读历史与当前effect权限分离。

### 10.2 外部写入结果的全部组合

下表适用于F8 DecisionCommitResult与P01 GovernedCommitOutcome；P01没有business verdict，F8没有P01 effect receipt。仅列出的组合合法。completionProjection为P01专属 NONE/COMPLETE/PENDING。

| operational | attempt | durability | 必有/可有字段 | 必须为空 |
| --- | --- | --- | --- | --- |
| DEFER / DENIED / INTEGRITY_CONFLICT | NOT_ATTEMPTED | NOT_ATTEMPTED | reason；可有untrusted query refs | confirmed decision/verdict/claim、receipt、completionProjection非NONE |
| RETRYABLE_FAILURE / DENIED / INTEGRITY_CONFLICT | ATTEMPTED | NOT_COMMITTED | 确定未提交证明；可有candidateQuery | confirmed decision/verdict/claim、receipt、completionProjection非NONE |
| RECONCILIATION_REQUIRED | ATTEMPTED | UNKNOWN | candidateQuery（stable identity）；提交异常/诊断 | confirmed decision/verdict/claim、receipt、成功原result；completionProjection非NONE |
| FINALIZED | ATTEMPTED | COMMITTED | F8：decision ref+四verdict之一+policy/input/clock refs；P01：exact receipt+versions+value fingerprint及COMPLETE/PENDING | F8的P01字段；P01的business verdict |
| HISTORICAL_FOUND | NOT_ATTEMPTED | COMMITTED | 新事务FOUND_MATCH证据，原committed refs；P01投影COMPLETE/PENDING | 新mutation/新decision、当前effect permission暗示 |

claim不是四verdict的通用必填字段：新ACCEPTED必须同commit的唯一wait-answer claim；DUPLICATE引用已证明APPLIED的winner/equivalence证据，不生成新winner；EXPIRED/REJECTED不生成新claim；历史结果按其原verdict携带对应关系。任何缺失关联或错误claim组合都是INVALID_RESULT。

NOT_COMMITTED只描述本次attempt，不覆盖同时读到的其他已提交winner；该winner若需返回，作为单独HistoricalDecisionRead，不填入本次confirmed字段。UNKNOWN的后续FOUND_MATCH作为独立读回证据，可报告历史已确认effect，但保留原attempt未知事实；不得改写当时commit观察结果。P01已提交且幂等投影PENDING返回FINALIZED/COMMITTED并明确projection pending，不能以此声称完整APPLIED。

内部provisional handle单独kind=INTERNAL_PROVISIONAL，仅活跃outer transaction可使用；不属于上述对外结果，含候选metadata而无durable success。字段时间空值也受约束：decision_effective_at只在confirmed F8 evidence内；UNKNOWN不得填“现在”冒充已提交时间。

## 11. 增补验收oracle与修订边界

以下与AUTH-01..14合并作为未来验收，未执行。oracle由独立fixture/owner预期定义，数据库内容、版本增量、幂等与receipt分别断言，不能从SUT status反推成功。

| ID / finding | 故障或反例 | 必须验证 |
| --- | --- | --- |
| AUTH-15 / B01 | core暂存COMMITTED后外层主动rollback或JPA flush失败 | state/version/receipt/reservation/completion全部回滚；零外部COMMITTED |
| AUTH-16 / B01 | owner commit响应unknown；一次read ABSENT | UNKNOWN无verdict/receipt；同identity读回，不释放重写；有明确终止证明才NOT_COMMITTED |
| AUTH-17 / B01 | complete故障但事务仍有效，owner effect提交 | 一次patch/版本增量；receipt存在、RESERVED；新事务仅修COMPLETED，同effect |
| AUTH-18 / B01 | complete数据库故障使rollback-only，或repair再故障 | 不清rollback-only；前者全回滚，后者保留receipt/RESERVED，无第二patch |
| AUTH-19 / B01 | commit成功后响应丢失；COMPLETED缺receipt | 前者exact历史重放一次增量；后者INCONSISTENT，不重放成功 |
| AUTH-20 / R01 | 手造VERIFIED DTO、错issuer/profile/tenant/type/ref | consumer按record复验拒绝；零F8/effect；客户端改manifest无效 |
| AUTH-21 / R01 | 换ref到另一wait/proposal/effect；改字节digest | lineage/fingerprint conflict；零临床写 |
| AUTH-22 / R01 | epoch撤销与effect并发，两种先后 | barrier证明同锁序列；撤销先则拒绝，effect先则历史保留且后续拒绝 |
| AUTH-23 / R01 | 生产profile加载test issuer；authority无共同guard | 启动拒绝/DEPENDENCY_BLOCKED，不退回remote bool |
| AUTH-24 / R02 | 每个合法及非法结果字段组合、未知enum | 合法组合可编码/消费；非法fail-closed，UNKNOWN/DEFER零confirmed字段 |
| AUTH-25 / R02 | PROVEN_MISMATCH与expiry/已applied等价同时成立 | 按原F8 precedence决定；admission不造REJECTED |
| AUTH-26 / R02 | 历史ACCEPTED + 当前撤销；P01 projection pending | 可授权历史读；拒新effect；pending不重mutation、不升APPLIED |

| 审查项 | 本轮具体修订 | 仍未证明 |
| --- | --- | --- |
| B01 | §8同事务归属、暂存/发布点、complete故障分类、exact恢复；AUTH-15..19 | 实际StateCommitter durable adapter/Stage-C集成 |
| R01 | §9test-only issuer/record复验、配置权限、epoch共同锁；AUTH-20..23 | 真实owner身份、权限及跨owner共同资源 |
| R02 | §10封闭status/字段组合、历史与unknown分离；AUTH-24..26 | Java DTO/校验器与真实故障执行 |

修订完成只代表REMEDIATION_PROPOSED，不自动关闭#385 findings。下一步在新HEAD定向复审；通过后进入隔离source/binding切片。原RDP、Shared Contracts、源码、migration和owner CAs未改，本轮无新测试执行、无merge；真实D5仍NOT_READY。
