# U07 D5-F8 隔离权威读回与决策提交适配详细设计及实现就绪评估 v0.2

日期：2026-10-10。base/main：835441492d130de320bf3d49fe2b4be598a2b327。
修订依据：#395 142e47534508b024b878a03d92788cde203a8497，DF8-B01 / DF8-R01。文件名保留v0.1；本修订不自行关闭finding。
状态：TARGETED_REMEDIATION_PROPOSED / READY_FOR_TARGETED_RE_REVIEW；IMPLEMENTATION_NOT_YET_AUTHORIZED；REAL_D5_NOT_READY。
本文件不修改冻结合同、生产源码、Shared Contracts或迁移，不执行合并。

## 1. 输入基线及目标

- 已合入 #392：Source/Canonical Binding，25例隔离Spring/JPA/JDBC/MySQL测试；当前main含该工具模块。只证明canonical/binding来源与原子性，不证明F8。
- RDP-02 #271：3fe93aa2cacd2eb76e94984ae56a0967ac634ac5，U07_RDP02_F8_Business_Resume_Decision_Precedence_Contract_v0.1.md；已读取Tier0、P1..P8、§5.1/8、时间/共同fence及F8-T01..41。其条件设计及上游CA保持，不自称正式冻结。
- 接入合同 #384：37aa52c3af0e83da2cd952af313caa1be2645d73，U07_D5_Source_F8_P01_Authority_Readback_Contract_v0.1.md（正文v0.2）；#386复审139fdce703e6339e082e33ebbc791b5aac9b0ae1只接受隔离Source/Binding实施，未授权本F8切片。
- main源码检索未发现完整F8 decision/claim实现。既有Foundation event ledger不得充当F8政策owner。

目标：在独立nonclinical MySQL fixture内，从完整已提交canonical USER_ANSWER读回，通过synthetic owner共同guard，提交唯一不可变F8 decision及ACCEPTED claim，提交后才公开结果。无LLM、真实用户、P02调用、P01临床patch、APPLIED写入或dispatch。

## 2. 隔离拓扑与资源归属

建议新增独立tools/u07_d5_f8模块与专属workflow，固定PROFILE-B和新disposable DB/端口，具体名称由实现清单确定；不扩展SourceBindingAdapter.admit使其加入已有事务。F8拥有自己的最外层TransactionTemplate，已有事务入口拒绝，不能把admission成功对象当F8权威witness。

共享代码策略：Maven选定编译main现有Source/Binding verifier/codec工具源码与Foundation实体/仓储，不复制第二份codec。若现有private reader不能在F8事务使用，应做package-private只读verifier最小抽取，由Source25例共同回归证明行为等价；不得调用会另开顶层事务的admit/read方法。该抽取属于后续显式exact-diff清单，不能隐蔽修改生产代码。

可信composition root绑定DB/DataSource/JpaTransactionManager、issuer registry、policy/manifest、equivalence policy及clock mapping；request仅有canonical/transport/source/target引用，scope来自测试入口。客户端布尔trusted/applied或手造DTO不得签发authority。

| fixture资源 | 写owner | F8权限/验证 |
| --- | --- | --- |
| canonical/binding/source | 既有synthetic admission/issuer | SELECT；验证Foundation/frame/proof/bytes完整链 |
| clinical_consultation guard row | synthetic协调fixture，使用原V2结构 | SELECT FOR UPDATE；F8不修改lifecycle/current wait |
| wait evidence、Question/Pending、delivery/eligibility记录 | 独立synthetic owner fixture | SELECT；完整immutable record与可变current pointer关联 |
| terminal/deadline/generation | synthetic U15 fixture | SELECT；owner更新须先同Consultation锁 |
| action permission epoch | synthetic authority fixture | SELECT+共享锁；撤销按同锁序排他更新 |
| decision ledger | F8 consumer | SELECT/INSERT，不UPDATE/DELETE final |
| wait claim | F8 consumer | SELECT/INSERT及严格CAS更新；禁止清空/重派winner |
| already-applied evidence | 独立测试owner预置不可变record | SELECT；仅模拟先前已应用事实，不执行APPLIED流程 |

issuer不能写decision或claim；F8不能写owner/apply证据。root只用于建库及显式corruption/fault注入。所有名字均synthetic，不把测试身份映射为真实U06/F3/P01/U15许可。

## 3. 读回与锁序

一个DataSource、一个外层事务；JPA/JDBC connection ID相同、active/joined/bound、autoCommit=false。首次finalization候选使用SERIALIZABLE，独立reader必须在此事务重新读取；不套用Source模块READ_COMMITTED为F8默认值。

固定锁序：Consultation行 → scope/action permission epoch → 按固定owner类型/ID顺序的wait/current/terminal行 → exact wait claim行 → exact decision行。synthetic owner对影响该wait的任何可变状态写入也必须先Consultation，再采用相同后续顺序；不同锁域/无法遵守时DEPENDENCY_BLOCKED。该序列是待审的本fixture细化，不宣称真实owner已批准。

Tier0：认证当前历史读权限，验证binding及原decision/claim完整性；FOUND_MATCH返回不可变原verdict，不要求当前wait仍WAITING_USER，不以effect许可撤销否定历史。历史read action与首次finalize action分开配置及epoch；返回不携带当前effect grant。
首次：取共同锁后重读source/binding、U06原issuance/confirmed delivery、F3Question及Gap/P01Pending、Consultation当前wait/版本、U15generation/deadline、permission和先前decision/claim/applied证据。检查record type、issuer/version、manifest、scope/actor、exact wait、证据版本/commit lineage。哈希只验证字节，不替代namespace权限。

读回状态：COHERENT / PROVEN_MISMATCH / UNAVAILABLE / INCOHERENT / INVALID_PROVENANCE / DEPENDENCY_BLOCKED。只有前两者可进入政策；缺证据不是业务REJECTED。可信mismatch也必须服从P1..P8，不能提前覆盖已应用等价或expiry。checkpoint缺失只作诊断；缺原issuance则阻断。

RESUME_REQUEST必须完整验证既有USER_ANSWER target，后续decision identity取target，不给resume ID独立decision；无原decision可以继续同target评估，无新answer。

## 4. 不变政策和存储形状

Tier0同canonical已提交decision优先。新canonical严格：
P1 同exact wait且已APPLIED同内容 → DUPLICATE；
P2 可信expiry或最终statement time>=deadline → EXPIRED；
P3 非expiry cancel/terminal/supersede → REJECTED；
P4 可信binding/delivery mismatch → REJECTED；
P5 另一pending ACCEPTED winner → DEFER；
P6 同wait已APPLIED不同内容 → REJECTED；
P7 全部合法且无winner → ACCEPTED；
P8 无正向证明 → DEFER。
P1用已认证answer content digest+exact consultation/question/parent wait+equivalence policy，不用binding fingerprint或LLM相似度。new Question同digest不全局去重。

fixture decision：PK stable framed hash(canonical answer ID,decision contract version)，UNIQUE canonical answer ID；包含scope、完整wait、binding及input fingerprints、policy/manifest、verdict/reason、original duplicate/applied refs、owner版本/ref、terminal generation、claim generation、decision_effective_at/clock/precision、trace。input fingerprint含所观察的政策输入及版本，与RDP01 binding hash分离。采用FINAL-only durable行；VALIDATING为事务内候选状态，不新增durable in-flight lease，不以timeout释放claim。

claim：PK consultation/question/parent wait；sentinel空winner，generation=0，仅本次事务内创建；只有ACCEPTED路径可将其与own decision/claim一起提交，其他路径必须回滚新建sentinel。ACCEPTED同时绑定winner canonical/decision及generation CAS 0→1、观察owner fence版本。无winner重派操作。非ACCEPTED无自己的winner claim；可读取既有其他winner，但不提交本次新建sentinel；DUPLICATE关联已应用winner，不冒充自己的winner。FK和CHECK只辅助，reader仍交叉校验decision/verdict/claim/payload/source关系。

## 5. 最终语句与时间门禁

MySQL8候选clock表达式UTC_TIMESTAMP(6)，作为待实测mapping，不宣称本文件已认证。deadline只用相同primary DB域、精度及fixture owner签发来源；client Instant、transaction-start或单独SELECT时间均不能授权ACCEPTED。Oracle不在本切片实施范围。

在持有共同guard的事务内，以一个INSERT ... SELECT最终decision语句取statement时间，并重新核对认证owner记录的观察版本、scope及permission epoch，按§9的分支条件执行P1..P8，产生最终verdict或零行。current-wait匹配和winner为空只适用于P7，不能作为全局WHERE。时间与判定来自同语句，同精度deadline比较，等于即EXPIRED。禁止在Java提前选定ACCEPTED再无条件INSERT。时钟mapping/precision未经证明时DEFER，零新decision/claim。

statement可按确定性CASE选择四verdict；DEFER条件必须过滤为零行，不持久化第五business enum。全部优先级条件使用本事务认证的owner行/ref，不接受request布尔结果。执行后读取本事务decision以获取实际verdict/statement time；若ACCEPTED，CAS更新claim到该decision，受同Consultation/claim锁保护。CAS受影响行不为1或claim写失败使外层全部回滚。语句谓词失败不得降级无条件写，须退出/重读同identity。

decision与claim可以是两条SQL，但必须一个物理事务、持锁到commit；final decision语句按verdict核验适用winner/代次：P7要求无winner，P1要求完整原applied winner；其余分支不要求winner为空。外部COMMITTED仅在outer commit成功后公开，不能以两条SQL之间的暂存decision视作winner。decision-before-claim满足逻辑原子性的前提须由故障测试证明。

最终statement在deadline前、物理commit在deadline后：若commit成功，历史ACCEPTED保持；下游必须另行currentness验证，本切片不执行下游。先finalization再故意延迟commit不能被误标EXPIRED。

## 6. 失败、恢复与结果合同

沿接入合同§10：
- DEFER/DENIED/INTEGRITY_CONFLICT + NOT_ATTEMPTED/NOT_ATTEMPTED，无confirmed字段。
- 确定已回滚：仅§10列出的operational + ATTEMPTED/NOT_COMMITTED，无verdict/decision/claim；零行统一RETRYABLE_FAILURE并附typed defer原因。
- commit outcome不明：RECONCILIATION_REQUIRED + ATTEMPTED/UNKNOWN，仅stable query及诊断，无confirmed verdict/time。
- FINALIZED + ATTEMPTED/COMMITTED，必须完整decision；仅ACCEPTED带自己的claim，DUPLICATE带已应用等价证据。
- HISTORICAL_FOUND + NOT_ATTEMPTED/COMMITTED，返回原decision，不授新effect许可。

非duplicate异常不能普遍解释成NOT_COMMITTED。1062失败必须结束原事务后fresh exact read，不在rollback-only session继续。新的FOUND_MATCH是独立历史证据，不篡改原UNKNOWN观察；ABSENT一次不证明unknown失败，不换identity。只有ACCEPTED与其own winner claim之间缺失/矛盾才按§11判INCONSISTENT；DUPLICATE及其他verdict分别校验，不要求自己的claim。禁止补造历史claim/decision。当前权限不足与source不可用分别报告。

本切片不采用durable VALIDATING claim；线程在提交前终止应回滚；若真实process kill未执行只报告故障注入，不能称crash验证。已有ACCEPTED永不因P02失败、取消或timeout改写。拒绝没有owner授权的winner release。

## 7. 验收清单（未来执行，当前零新增测试）

| ID | oracle与关键断言 | RDP对应 |
| --- | --- | --- |
| DF8-01 | profile/假DTO/错issuer/跨tenant/ref/type拒绝，零decision/claim | T13/15/21/27 |
| DF8-02 | binding/target腐化、缺source/answer、orphan阻断，RESUME无独立decision | T04/20/21 |
| DF8-03 | 合法ACCEPTED、同canonical/alias Tier0重放、零第二winner | T01..04 |
| DF8-04 | 已APPLIED同内容且expiry/cancel同时成立仍P1；不同Question不duplicate | T05/08/24/32 |
| DF8-05 | 已APPLIED不同内容P6、pending winner P5、不同scope不可泄露历史 | T06/07 |
| DF8-06 | expiry优先cancel/mismatch；cancel早于P7；无证据DEFER无第五verdict | T09/11/12/27 |
| DF8-07 | statement前/等于/后deadline及未写U15时自然跨期 | T26/28/37/38 |
| DF8-08 | 禁单独时间SELECT+INSERT；长事务clock推进/精度与域错误fail closed | T31/36/39/40 |
| DF8-09 | statement早于deadline、commit晚于deadline仍历史ACCEPTED | T35 |
| DF8-10 | U15-first/F8-first barrier、旧读者重读、owner不共fence阻断 | T16/29/30/33/34 |
| DF8-11 | 两个不同答案并发至多一个ACCEPTED；同canonical竞态fresh read | T23/41 |
| DF8-12 | decision插入后/claimCAS后/body/flush/rollback-only故障全回滚 | T17 |
| DF8-13 | commit后丢响应原decision重附着；UNKNOWN与ABSENT分离 | T18/22/41 |
| DF8-14 | 历史ACCEPTED撤销effect仍可授权read；missing checkpoint不改verdict | T14/25 |
| DF8-15 | 原Source25例回归、独立编码golden、全部非法result组合 | 接入AUTH03/04/24 |
| DF8-16 | 所有用例零Clinical patch/P02/U02/outbox/dispatch；无F8修改APPLIED | 全部 |

精确deadline equality可由条件finalizer在TEST-only固定clock表达式的独立fixture oracle验证；不能将此称真实DBclock认证。另用真实表达式测试长事务推进及实际跨期，记录DB时区/precision/primary来源。若实际表达式无法稳定构造equality，不虚报真实equality覆盖，须保留fixture与真实clock证据区分。
applied fixture需验证独立issuer证据完整链，而非简单applied=true；本fixture结果不关闭真实APPLIED owner CA。

## 8. 就绪评估与下一步

| 前提 | 当前状态/门禁 |
| --- | --- |
| Source/Binding基线 | 已合入main，隔离证据可复用；如抽取verifier须回归 |
| F8详细方案 | PROPOSED，待针对性设计审查；原precedence不变 |
| synthetic owner共同锁等价 | 本方案待审；真实U15 CA不关闭 |
| 最终conditional SQL/clock | 待隔离prototype及真实MySQL证明，不宣称已就绪 |
| decision/claim原子性及权限 | 待schema/exact-diff审查与故障执行 |
| 实施范围/许可 | READY_FOR_DESIGN_REVIEW，不是立即实施授权 |
| 真实D5 | NOT_READY；真实owner、Foundation audit、U06 issuance及U15/P01等CA保留 |

下一步针对性设计审查重点：Source reader事务归属、共同锁与owner写权限、P1..P8最终SQL完整性、statement clock、decision/claim两语句原子性、Tier0与新permission分离、UNKNOWN恢复。通过后才能形成独立SQL/clock prototype或隔离实现授权清单；不得凭本设计跳到真实F8/P01或合并。

本轮只交付一份设计文档；没有新增Java/schema/workflow、运行新测试或合并任何PR。


## 9. DF8-B01：公共authority条件与分支predicate矩阵

安全公共条件C：已认证当前finalize action与scope；完整canonical USER_ANSWER/source/binding；共同guard/resource/clock mapping有效；statement引用的owner记录、指针和观察版本未被改变；exact decision不存在。版本相等仅表示“观察未变”，不等于其业务状态必须合法。未知/缺失关键高优先级证据不能按false继续低优先级。

U06原issuance与历史wait身份必须有可信记录。当前owner状态与该历史wait不同可以是负向事实，不能强制inner join当前wait相等后使其消失。缺当前row只有owner合同明确认证其“已删除/终结”且有tombstone/version证据才可作负向事实；普通缺行、未签发、不可读、拼错ref均DEFER。跨tenant/actor未经授权仍入口DENIED，不能写REJECTED记录泄露他人scope。

定义Ei为对应已认证事实：E1已应用等价，E2statement-time expiry/owner expiry，E3非expiry terminal，E4可信身份/当前wait mismatch，E5其他pending winner，E6已应用不等价，E7合法当前wait且无winner。按顺序只在前面所需事实可信且均未命中时进入下一项。E2使用本最终语句同一个clock值，不能预先以JVM计算。认证后观察版本/refs输入SQL，不接受caller给的Ei布尔值。

| 分支 | 最终命中条件（除C外） | current wait / winner条件 | 落库及claim |
| --- | --- | --- | --- |
| P1 | E1；原ACCEPTED decision+claim+独立applied证据、同内容digest/政策、exact历史wait关联均匹配 | 允许current wait合法移动/清空及已terminal；必须原applied winner存在并完整 | DUPLICATE，原winner/applied refs；无own claim |
| P2 | 已排除E1；可信E2，包含t>=deadline | 不要求current wait仍匹配或winner为空；记录观察到的owner版本与可选winner | EXPIRED，无own claim |
| P3 | 已排除E1/E2；可信E3 | 可已清空current wait；不要求winner空 | REJECTED/terminal，无own claim |
| P4 | 已排除前项；可信E4 | current wait不匹配本身是命中事实；仍须同认证scope及历史wait来源 | REJECTED/mismatch，无own claim |
| P5 | 前项已排除；可信E5 | 另一ACCEPTED-own-claim完整，但未证明APPLIED | 零decision，typed competing-winner DEFER |
| P6 | 前项已排除；可信E6 | applied winner完整且内容不同；不要求winner空 | REJECTED/conflict，无own claim |
| P7 | 前项已排除；E7及delivery/issuance/Question/Pending/Consultation WAITING_USER全部合法，t<deadline、无terminal、许可有效 | 仅此分支要求current wait匹配、claim缺失或合法空sentinel，generation=0 | ACCEPTED，own claim CAS 0→1，同外层commit |
| P8 | 正向证明不足或任一必要precedence事实不确定 | 不补造缺owner证据 | 零decision，typed blocked-evidence DEFER |

最终语句按此有序CASE+过滤选择四verdict；SQL join必须保留可信负向事实，不以所有owner值“合法”为共同WHERE。非法provenance不进入CASE。duplicate/applied/terminal事实来源的generation分别保存，不把别人的claim generation当自己的own claim。

predicate版本不符或C不成立时零行；不能将零行猜成P2/P4，不能降级无条件INSERT。应按§10结束当前attempt，再重新读同identity。若锁内已出现本canonical committed decision，回到Tier0的完整历史校验，不执行第二INSERT。

## 10. DF8-R01：attempt边界、sentinel及全部退出路径

attempt边界固定为本次首次调用任何F8持久DML（sentinel INSERT、decision final INSERT或claim CAS），在调用前即置ATTEMPTED。只读事务、SELECT FOR UPDATE、已有行锁不会令attempt变ATTEMPTED。入参/权限/证据拒绝或只读pending-winner DEFER均可NOT_ATTEMPTED；执行过DML即使影响0行或失败也不能改回。

策略：先锁Consultation及owner，再读取已存在claim；Consultation共同锁覆盖claim缺行的创建竞争。pending winner只读退出，不先创建sentinel。对可进入finalizer的候选先做只读预判：确实需要P7时才暂存sentinel。最终statement可能因时间推进变为P2；若本事务已新建sentinel，必须回滚整个事务，随后以原identity重新评估负向verdict，且新的负向分支不创建sentinel。禁止提交空sentinel或在失败事务内删sentinel继续。原已有合法空sentinel只读可识别，不能被视为已拥有winner。

最终SQL执行已零行统一回滚，包括sentinel。回滚成功后输出RETRYABLE_FAILURE/ATTEMPTED/NOT_COMMITTED，reason为COMPETING_WINNER_PENDING、BLOCKED_EVIDENCE、PREDICATE_CHANGED或SENTINEL_ROLLBACK_REQUIRED等typed诊断；不是新增business enum。仅当回滚已确认且原attempt结束才可另开评估事务；本次返回原attempt结果，禁止同一次返回将其伪装NOT_ATTEMPTED。下一次请求/受控reconciliation仍用相同canonical/decision ID。

| 路径 | operational / attempt / durability | confirmed字段 |
| --- | --- | --- |
| 仅历史read FOUND_MATCH，无DML | HISTORICAL_FOUND / NOT_ATTEMPTED / COMMITTED | 原完整decision/verdict及适用关联 |
| 入参、scope、许可拒绝，无DML | DENIED或INTEGRITY_CONFLICT / NOT_ATTEMPTED / NOT_ATTEMPTED | 空 |
| owner/clock不可用、只读pending winner或缺证据 | DEFER / NOT_ATTEMPTED / NOT_ATTEMPTED | 空 |
| 任意DML后零行/策略退出，确定回滚 | RETRYABLE_FAILURE / ATTEMPTED / NOT_COMMITTED | 空，可stable query+typed reason |
| 任意DML后确认DENIED/完整性损坏，确定回滚 | DENIED或INTEGRITY_CONFLICT / ATTEMPTED / NOT_COMMITTED | 空 |
| DML后数据库异常，确定回滚 | RETRYABLE_FAILURE / ATTEMPTED / NOT_COMMITTED | 空 |
| DML后commit/rollback结果不明 | RECONCILIATION_REQUIRED / ATTEMPTED / UNKNOWN | 空，仅stable query及diagnostics |
| 最终decision及所需own claim成功，outer commit确认 | FINALIZED / ATTEMPTED / COMMITTED | 完整decision/verdict，关联按§11 |
| DML竞争失败后fresh read winner | 原attempt结果与独立HistoricalDecisionRead分开 | 不将winner写入失败attempt字段 |

只读历史reader的MISMATCH/INCONSISTENT/UNAVAILABLE/ABSENT/INDETERMINATE属于HistoricalDecisionRead，不伪装写结果成功。若尚未尝试F8 DML的只读事务出异常，返回DEFER/NOT_ATTEMPTED/NOT_ATTEMPTED诊断；不得据此修改任何既有UNKNOWN事实。1062不自动证明新canonical成功；先确定失败事务结束，再fresh reader核对decision/claim。

FINALIZED提交时只允许：非ACCEPTED没有本次new sentinel；ACCEPTED的本次sentinel已转完整winner。roll back失败/unknown须保持UNKNOWN，不以“预计回滚”报告NOT_COMMITTED。上述退出分支必须成为独立result validator用例。

## 11. DF8-R01：按verdict读回与损坏隔离

| 持久情况 | 读回裁决 |
| --- | --- |
| ACCEPTED + 同scope/exact wait、own canonical/decision、generation及fence匹配的claim | FOUND_MATCH；无当前effect grant |
| ACCEPTED缺own claim；own winner缺decision；关系/代次错误 | INCONSISTENT，不repair或重派winner |
| DUPLICATE + 原已应用winner的ACCEPTED decision/claim/applied issuer证据完整，equivalence匹配 | FOUND_MATCH；没有自己的claim是正常状态 |
| DUPLICATE缺原winner/证据、错scope/wait/digest，或claim指向该DUPLICATE作为winner | INCONSISTENT/MISMATCH，不确认duplicate |
| EXPIRED/REJECTED + 自身decision完整，没有own winner claim | FOUND_MATCH；可以观察另一答案的合法claim，不要求其为空 |
| EXPIRED/REJECTED被claim作为winner引用 | INCONSISTENT |
| 合法空sentinel：winner canonical/decision均NULL、generation=0，scope/wait有效 | 不是winner，也不是历史decision；query本canonical无decision则ABSENT，可按当前许可重新评估 |
| sentinel半填、空winner非0 generation、非法scope/wait | INCONSISTENT，不归为ABSENT |
| 完全缺claim且无decision | ABSENT，不证明原UNKNOWN失败 |

新实现不会正常提交空sentinel；合法旧空sentinel作为受控fixture兼容情况可识别，不因该兼容能力许可F8补造历史owner证据。判定只检查与本canonical或引用winner相关的claim，不将另一事件的claim误认own claim。

## 12. 修订增补oracle与交付边界

| ID | 独立预期 |
| --- | --- |
| DF8-R17 | 已APPLIED同内容+既有winner+current wait移动/terminal：P1 DUPLICATE，仅新增decision，零新claim |
| DF8-R18 | cancel后current wait清空：可信P3 REJECTED；可信current-wait mismatch：P4 REJECTED；缺authority则DEFER |
| DF8-R19 | finalizer零行、已创建sentinel、clock推进P7→P2：确认回滚，ATTEMPTED/NOT_COMMITTED；后续原identity负向decision无sentinel |
| DF8-R20 | 只读pending winner：NOT_ATTEMPTED；DML后退出：ATTEMPTED；UNKNOWN不得伪装确定回滚 |
| DF8-R21 | 历史EXPIRED/REJECTED无claim、DUPLICATE无own claim：正常；ACCEPTED缺claim/错duplicate winner：隔离 |
| DF8-R22 | 合法空sentinel可读无winner；半填/错误generation隔离；所有非ACCEPTED路径不提交新sentinel |

原16组+增补6组均为未来oracle，本轮未执行。§9–11优先于原文本中简写的最终predicate、attempt及claim检查；这些简写已同步修订。原P1..P8优先级、clock与outer commit边界、Source21字段及真实owner CA不改变。

DF8-B01 / DF8-R01 = REMEDIATION_PROPOSED，待新HEAD定向复审；不是自动关闭或实施授权。下一步只做修订后设计复审，再决定隔离SQL/clock prototype范围。
