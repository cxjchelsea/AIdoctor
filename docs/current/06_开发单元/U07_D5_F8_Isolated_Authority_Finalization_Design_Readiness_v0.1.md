# U07 D5-F8 隔离权威读回与决策提交适配详细设计及实现就绪评估 v0.1

日期：2026-10-10。base/main：835441492d130de320bf3d49fe2b4be598a2b327。
状态：DESIGN_PROPOSED / READY_FOR_TARGETED_DESIGN_REVIEW；IMPLEMENTATION_NOT_YET_AUTHORIZED；REAL_D5_NOT_READY。
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

claim：PK consultation/question/parent wait；sentinel空winner，generation=0。ACCEPTED同时绑定winner canonical/decision及generation CAS 0→1、观察owner fence版本。无winner重派操作。非ACCEPTED无新claim；DUPLICATE关联已应用winner，不冒充自己的winner。FK和CHECK只辅助，reader仍交叉校验decision/verdict/claim/payload/source关系。

## 5. 最终语句与时间门禁

MySQL8候选clock表达式UTC_TIMESTAMP(6)，作为待实测mapping，不宣称本文件已认证。deadline只用相同primary DB域、精度及fixture owner签发来源；client Instant、transaction-start或单独SELECT时间均不能授权ACCEPTED。Oracle不在本切片实施范围。

在持有共同guard的事务内，以一个INSERT ... SELECT最终decision语句取statement时间，并重新predicate owner generation、Consultation版本/current wait、permission epoch、wait及claim generation，按P1..P8产生最终verdict或零行。时间与判定来自同语句，同精度deadline比较，等于即EXPIRED。禁止在Java提前选定ACCEPTED再无条件INSERT。时钟mapping/precision未经证明时DEFER，零新decision/claim。

statement可按确定性CASE选择四verdict；DEFER条件必须过滤为零行，不持久化第五business enum。全部优先级条件使用本事务认证的owner行/ref，不接受request布尔结果。执行后读取本事务decision以获取实际verdict/statement time；若ACCEPTED，CAS更新claim到该decision，受同Consultation/claim锁保护。CAS受影响行不为1或claim写失败使外层全部回滚。语句谓词失败不得降级无条件写，须退出/重读同identity。

decision与claim可以是两条SQL，但必须一个物理事务、持锁到commit；final decision语句同时校验winner空/代次。外部COMMITTED仅在outer commit成功后公开，不能以两条SQL之间的暂存decision视作winner。decision-before-claim满足逻辑原子性的前提须由故障测试证明。

最终statement在deadline前、物理commit在deadline后：若commit成功，历史ACCEPTED保持；下游必须另行currentness验证，本切片不执行下游。先finalization再故意延迟commit不能被误标EXPIRED。

## 6. 失败、恢复与结果合同

沿接入合同§10：
- DEFER/DENIED/INTEGRITY_CONFLICT + NOT_ATTEMPTED/NOT_ATTEMPTED，无confirmed字段。
- 确定已回滚：RETRYABLE_FAILURE等 + ATTEMPTED/NOT_COMMITTED，无verdict/decision/claim。
- commit outcome不明：RECONCILIATION_REQUIRED + ATTEMPTED/UNKNOWN，仅stable query及诊断，无confirmed verdict/time。
- FINALIZED + ATTEMPTED/COMMITTED，必须完整decision；仅ACCEPTED带自己的claim，DUPLICATE带已应用等价证据。
- HISTORICAL_FOUND + NOT_ATTEMPTED/COMMITTED，返回原decision，不授新effect许可。

非duplicate异常不能普遍解释成NOT_COMMITTED。1062失败必须结束原事务后fresh exact read，不在rollback-only session继续。新的FOUND_MATCH是独立历史证据，不篡改原UNKNOWN观察；ABSENT一次不证明unknown失败，不换identity。claim有winner但decision缺失或反之为INCONSISTENT，禁止补造历史claim/decision。当前权限不足与source不可用分别报告。

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
