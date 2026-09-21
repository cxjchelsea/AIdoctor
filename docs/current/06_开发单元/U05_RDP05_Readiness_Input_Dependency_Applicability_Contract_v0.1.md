# U05 RDP-05 Readiness Input Dependency / Applicability Contract v0.1

> Scope: U05 Clinical Readiness 输入来源、适用性、版本一致性与阶段依赖边界。
>
> Status: PROPOSED / READY_FOR_INDEPENDENT_REVIEW
>
> Review basis: main@6e68fd9fb7cd19e87aadae30f3bb53a2264d1920
>
> 本文件只解决 BF-U05-RG-05 的设计问题；不授予 U05 implementation、U04→U05 live routing、U05 downstream owner execution、production 或 real-patient authorization。

---

## 1. Design objective

冻结以下问题：

1. U05 允许消费哪些 readiness input；
2. 每类 input 的业务 Owner / producer 是谁；
3. PRESENT、ABSENT_BY_DESIGN、NOT_YET_APPLICABLE、STALE、FAILED、UNAVAILABLE 如何区分；
4. 首轮进入 U05 时 F3/F5/F6 尚不存在是否构成 failure；
5. 如何避免 F3 → U05 → U06 → F3 的循环依赖；
6. 多个 input 如何绑定同一当前 Clinical State Version；
7. 哪些情况必须停止而不能由 U05 猜测 readiness。

本文件不冻结 D03 最终业务优先算法的全部细节；D03 decision contract 属于 U05-RDP-02。

---

## 2. Core ownership rule

Clinical Readiness 的唯一系统级 Resolver 仍是 G2/U05 的 D03。

    F1/F2 clarification semantics
    F3 Gap semantics
    F5 DDx / Must-Exclude semantics
    F6 Offline Evidence semantics
           ↓
    readiness input records
           ↓
    U05 / D03
           ↓
    exactly one governed Clinical Readiness

强制保持：

    readiness input != Clinical Readiness
    capability result != readiness input unless interpreted by its business Owner
    missing input != negative business conclusion
    Runtime state != readiness input
    frontend/model/agent != readiness Owner

U05 不调用模型去补齐缺失 input，也不自行生成 F3/F5/F6 业务结论。

---

## 3. Canonical readiness input envelope

所有可参与 D03 的 input 必须规范化成统一 envelope。最小语义：

    readiness_input_id
    source_domain
    source_owner
    input_kind
    applicability_status
    business_signal
    consultation_id
    cdp_id
    clinical_state_version
    source_decision_ref
    source_state_ref
    evidence_refs[]
    policy_or_rule_refs[]
    produced_at
    validity

source_domain 允许：

    F1
    F2_CLARIFICATION
    F3
    F5
    F6

`F2_CLARIFICATION` 只承接 Phase 6 已允许的“F2 发现当前事实无法可靠形成、需要澄清”的输入，不把 F2 扩展成 Clinical Readiness Owner。

input_kind 至少区分：

    SCOPE_OR_FRAMING
    CLARIFICATION_REQUIREMENT
    ONLINE_INFORMATION_GAP
    DDX_OR_MUST_EXCLUDE
    OFFLINE_EVIDENCE

RDP-05 不冻结 Java 类名、数据库表名或 REST schema。

---

## 4. Applicability status vocabulary

每个 source_domain 对当前 evaluation context 必须明确一个 applicability status：

    PRESENT
    ABSENT_BY_DESIGN
    NOT_YET_APPLICABLE
    STALE
    FAILED
    UNAVAILABLE

语义：

### PRESENT

该业务域已经完成当前版本所需的业务判断，并提供可参与 D03 的 input。

### ABSENT_BY_DESIGN

该 source domain 已经在当前 Consultation 路径中合法适用/激活，但当前 evaluation context 按冻结流程不要求它产出一个 readiness input artifact。

它表示“已适用但本次按设计无需产出”，不能用于表示“这个业务域还没进入合法阶段”。

### NOT_YET_APPLICABLE

该 source domain / stage 在当前 Consultation 路径中尚未被合法激活，因此当前还不是本次 readiness 求值应要求的输入来源。

例如：

    首次 post-safety U05 求值、尚未进入 U06/C03/F3 路径
    → F3 = NOT_YET_APPLICABLE

    尚未进入 DDx
    → F5 = NOT_YET_APPLICABLE

    尚未进入 U10/F6 assessment
    → F6 = NOT_YET_APPLICABLE

两者必须互斥：

    domain not legally activated yet
    → NOT_YET_APPLICABLE

    domain already applicable/activated
    + this evaluation context lawfully requires no input artifact
    → ABSENT_BY_DESIGN

### STALE

存在旧 input，但它绑定旧 Clinical State Version 或已被上游事实变化失效。

STALE 不得参与 D03。

### FAILED

业务域本应为当前阶段产生 input，但其合法计算/解释失败。

FAILED 是 failure semantics，不得映射成业务 negative。

### UNAVAILABLE

当前阶段所需的业务输入生产者或其必要依赖不可用。

UNAVAILABLE 不得映射成 NONE / NOT_NEEDED / READY。

全局不变量：

    ABSENT_BY_DESIGN
    != NOT_YET_APPLICABLE
    != STALE
    != FAILED
    != UNAVAILABLE
    != NOT_NEEDED
    != NO_RELIABLE_DIRECTION
    != READY_FOR_CLINICAL_ANALYSIS

---

## 5. Business signal vocabulary by source

RDP-05 只冻结输入语义类别，不新增医学规则。

### 5.1 F1 / F2 clarification

允许向 U05 提供：

    OUT_OF_SCOPE
    NEEDS_CLARIFICATION
    FRAMED_IN_SCOPE

`FRAMED_IN_SCOPE` 只表示对象/问题/Scope 已合法建立，不代表已经达到临床分析充分性。

F2_CLARIFICATION 只允许提供：

    NEEDS_CLARIFICATION

以及与该澄清要求对应的 source refs/reason refs。

### 5.2 F3

允许向 U05 提供与当前已形成 Gap 状态一致的：

    CAN_ASK_MORE
    NEEDS_OFFLINE_EVIDENCE
    NO_ACTIVE_ONLINE_BLOCKING_GAP

`NO_ACTIVE_ONLINE_BLOCKING_GAP` 只表示 F3 当前没有仍有实际决策价值的线上阻断 Gap。

它：

    != READY_FOR_CLINICAL_ANALYSIS
    != NO_RELIABLE_DIRECTION

### 5.3 F5

允许向 U05 提供：

    ANALYSIS_RESULT_AVAILABLE
    NO_RELIABLE_DIRECTION
    NEEDS_OFFLINE_EVIDENCE
    REASSESSMENT_REQUIRED

具体 DDx / Must-Exclude 临床内容仍由 F5/G1/G6 的治理语义决定。

### 5.4 F6

允许向 U05 提供：

    NEEDS_OFFLINE_EVIDENCE
    NO_BLOCKING_OFFLINE_EVIDENCE_NEED

`NO_BLOCKING_OFFLINE_EVIDENCE_NEED` 只表示 F6 当前没有阻断性的线下证据需求，不代表 Consultation 可完成。

---

## 6. Evaluation context / applicability matrix

U05 不依赖固定 step number，但 RDP-05 冻结四种最小 evaluation context，用于解释 input applicability。

| Evaluation context | F1/F2 | F3 | F5 | F6 |
|---|---|---|---|---|
| POST_SAFETY_INITIAL (non-A1 baseline) | PRESENT expected | NOT_YET_APPLICABLE | NOT_YET_APPLICABLE | NOT_YET_APPLICABLE |
| A1_POST_SAFETY_BOOTSTRAP_REQUIRED | PRESENT/current | NOT_YET_APPLICABLE for U05 admission; U05 ordinary evaluation not eligible; route to A1 pre-readiness | NOT_YET_APPLICABLE | NOT_YET_APPLICABLE |
| A1_PRE_READINESS_IN_PROGRESS | PRESENT/current | NOT_YET_APPLICABLE / producer pending; U05 admission prohibited | NOT_YET_APPLICABLE | NOT_YET_APPLICABLE |
| A1_F3_COMMITTED_BARRIER_PENDING | PRESENT/current | canonical F3 source exists but readiness input is STALE / NOT_CURRENT_FOR_D03 until revalidation | NOT_YET_APPLICABLE or STALE | NOT_YET_APPLICABLE or STALE |
| A1_POST_BARRIER_CURRENT | PRESENT/current | PRESENT / CURRENT after F3 owner current-version revalidation | NOT_YET_APPLICABLE or PRESENT as otherwise governed | NOT_YET_APPLICABLE or PRESENT as otherwise governed |
| POST_USER_FACT_UPDATE | PRESENT expected | NOT_YET_APPLICABLE if F3 has never been lawfully activated; STALE if a prior-version F3 input existed and current-version reevaluation is pending; PRESENT only if current-version F3 input exists | NOT_YET_APPLICABLE or STALE until recalculated | NOT_YET_APPLICABLE or STALE |
| POST_DDX_REEVALUATION | PRESENT/current | PRESENT expected after F3/U09 re-evaluation | PRESENT expected | NOT_YET_APPLICABLE or PRESENT |
| POST_OFFLINE_ASSESSMENT | PRESENT/current | PRESENT if current-version F3 input is required/available; ABSENT_BY_DESIGN only if F3 has already been lawfully activated but this context requires no F3 input artifact | PRESENT/current if applicable | PRESENT expected |

矩阵中的 expected 表示：若该 source 在该 context 按 frozen flow 应当已有结果，但实际为 FAILED/UNAVAILABLE/STALE，则 U05 不得把缺失解释成 business negative。

---

## 7. Resolution of the initial F3 sequencing problem

### 7.1 Decision — A1 controlled amendment

原 `POST_SAFETY_INITIAL → F3 = NOT_YET_APPLICABLE` 仍保留为 non-A1 baseline。

当：

    BootstrapArchitectureBindingRef = A1

且 bootstrap F3 尚未 current 时：

    ordinary U05 readiness evaluation
    = NOT ELIGIBLE

    D03
    = NOT INVOKED

    no D03 decision_id
    no D03 decision_status

Routing instead exposes:

    PRE_READINESS_A1_F3_C03_ELIGIBLE
    → U06 PRE_READINESS_GAP_ASSESSMENT
    → canonical F3
    → Safety barrier
    → F3 current-version revalidation
    → only then ordinary U05 admission may proceed.

因此，A1 下“U05 不等待 U06/C03”不再成立为普遍规则。

精确限定为：

    Outside an explicitly governed A1 bootstrap path,
    U05 does not invent a dependency on U06/C03.

    Under A1,
    U05 still does NOT call C03 directly;
    the pre-readiness dependency is satisfied through U04 routing + U06/F3 Owner path.

### 7.2 Why this does not manufacture readiness

F3 = NOT_YET_APPLICABLE 只表示当前 Consultation 路径尚未合法激活 F3/C03 作为 readiness input producer。

它不能推出：

    CAN_ASK_MORE = false
    no important information gap
    READY_FOR_CLINICAL_ANALYSIS
    NO_RELIABLE_DIRECTION

因此 D03/RDP-02 必须定义 `READY_FOR_CLINICAL_ANALYSIS` 的正向成立条件。

如果 RDP-02 无法在合法的初始 input profile 下形成唯一结果而不发明医学语义，则必须把该问题作为 RDP-02 expectation gap 处理；不得回头把 F3 缺席偷偷解释为 READY。

### 7.3 How U06 remains legal

必须区分两类合法澄清/提问路径，RDP-05 不得把它们合并成“所有问题都必须先经过 U05”。

A. BL-01 entry clarification

    U01/F1 detects lawful entry clarification need
    → U06
    → minimum necessary clarification question

该路径发生在普通 post-safety U05 evaluation 之前时，继续保留 Phase 6 已冻结的 U01→U06 边界；不强制先经过 U05，也不要求先存在 F3 Gap。

B. Post-safety U05 readiness path

    U05 evaluation is already lawfully triggered
    + current F1/F2 clarification input exists
    → D03 may resolve NEEDS_CLARIFICATION
    → U06

或：

    current F3 readiness input exists
    + D03 = CAN_ASK_MORE
    → U06

对于 CAN_ASK_MORE：F3 input 必须已经由 F3 Owner 的合法路径形成，U05 只消费，不生成。

因此：

    U01/F1 entry clarification
    != mandatory U05 route

    U05 NEEDS_CLARIFICATION
    = valid only when U05 evaluation itself is already legitimately in progress

RDP-05 不改变 Phase 7 中 C03 的 Capability 边界，也不把 C03 的 FIRST_CONSUMER_UNIT 改成 U05。

### 7.4 Later F3 production

F3 input 可在后续合法路径形成或更新，例如：

    U06 clarification/question lifecycle consequences
    U09 post-DDx Gap re-evaluation
    correction / new fact invalidation followed by F3 re-evaluation

具体 C03 调用与 Gap 生成机制属于 U06/U09 readiness/implementation design，不由 U05-RDP-05 实现。

---

## 8. Version and identity binding

新事实形成后的 F3 失效语义必须显式保留：

    previous F3 input exists at Clinical State Version N
    + user/new fact creates Clinical State Version N+1
    + current-version F3 reevaluation has not completed
    → previous F3 input = STALE
    → D03 ordinary resolution prohibited

只有当 F3 从未在当前 Consultation 路径中被合法激活时，才可以是 NOT_YET_APPLICABLE。

已经存在过的 prior-version F3 input 不得在新版本中无痕降成 ABSENT_BY_DESIGN / NOT_YET_APPLICABLE。

参与同一次 D03 求值的 PRESENT input 必须满足：

    same consultation_id
    same cdp_id
    same current Clinical State Version
    validity = CURRENT

如果 input 来自长期稳定的上游语义，但其 source decision 产生于旧 version，则必须经过明确的 current-version revalidation/reference binding，不能仅凭“内容没变”自动复用。

禁止：

    stale input + current input mixed resolution
    old F3 gap + new fact version
    old F5 DDx + corrected current fact
    old F6 assessment + superseding evidence

版本冲突：

    → U05 admission/evaluation failure
    → no Clinical Readiness commit

不得自动降级成 NO_RELIABLE_DIRECTION。

---

## 9. Requiredness rule

input 是否 required 由 evaluation context + frozen business flow 决定，不由 source 自己声明。

    source says NOT_NEEDED
    != U05 accepts absence

例如：

- POST_SAFETY_INITIAL：F5/F6 合法 not-yet-applicable；
- POST_DDX_REEVALUATION：若流程声明 F5/F3 应已完成，而实际 FAILED/STALE，则不能跳过；
- POST_OFFLINE_ASSESSMENT：F6 应有当前 assessment；缺失不是自动 NOT_NEEDED。

---

## 10. Failure handling boundary

U05 只接受业务 input 或其 applicability metadata。

当 required source 为 FAILED / UNAVAILABLE / STALE：

    D03 ordinary business resolution = PROHIBITED
    Clinical Readiness commit = PROHIBITED

具体 failure routing 仍属于 U14，U05-RDP-05 只产出 typed failure reason/eligibility，不执行 U14。

推荐 reason family：

    READINESS_INPUT_STALE
    READINESS_INPUT_REQUIRED_BUT_FAILED
    READINESS_INPUT_REQUIRED_BUT_UNAVAILABLE
    READINESS_INPUT_IDENTITY_MISMATCH
    READINESS_INPUT_VERSION_MISMATCH

最终 reason-code 枚举由 RDP-02/RDP-03 一致化。

---

## 11. Prohibited interpretations

以下实现一律不允许：

    null F3 -> READY
    [] Gap -> READY
    F5 not run -> NO_RELIABLE_DIRECTION
    F6 not run -> NOT_NEEDED
    capability failure -> no gap
    stale input -> current negative
    frontend completeness score -> Clinical Readiness
    LLM decides final readiness
    U05 invokes C03 to invent missing input

旧 completeness threshold / fixed required-field heuristic 不能成为 D03 真值来源。

---

## 12. Contract examples

### Example A — post-safety clarification

    Safety Gate = ALLOW
    U05 evaluation = lawfully triggered
    F1/F2 = PRESENT / NEEDS_CLARIFICATION
    F3 = NOT_YET_APPLICABLE
    F5 = NOT_YET_APPLICABLE
    F6 = NOT_YET_APPLICABLE

RDP-05 conclusion:

    input profile is admissible
    D03 may evaluate NEEDS_CLARIFICATION according to RDP-02

### Example B — initial no F3

    Safety Gate = ALLOW
    F1 = PRESENT / FRAMED_IN_SCOPE
    F3 = NOT_YET_APPLICABLE
    F5 = NOT_YET_APPLICABLE
    F6 = NOT_YET_APPLICABLE

RDP-05 conclusion:

    input profile is admissible
    F3 absence is not failure
    F3 absence is not evidence for READY
    final D03 result depends on positive conditions frozen in RDP-02

### Example C — post-DDx online gap

    F1 = PRESENT/current
    F3 = PRESENT / CAN_ASK_MORE
    F5 = PRESENT / ANALYSIS_RESULT_AVAILABLE
    F6 = NOT_YET_APPLICABLE

RDP-05 conclusion:

    current-version inputs may participate in D03

### Example D — post-DDx stale F3

    current Clinical State Version = 12
    F3 input version = 11 / STALE
    F5 input version = 12

RDP-05 conclusion:

    D03 resolution prohibited
    no readiness commit
    typed stale-input failure

### Example E — BL-01 direct entry clarification

    U01/F1 = lawful entry clarification required
    ordinary post-safety U05 evaluation = not yet entered

RDP-05 conclusion:

    existing U01 -> U06 clarification boundary remains valid
    U05 is not inserted merely to relay the clarification
    F3 is not required

---

## 13. Relationship to other U05 RDPs

RDP-05 provides the input contract for:

    RDP-01 U05 consumer admission
    RDP-02 D03 policy / owner decision
    RDP-03 K09/P01 mutation + trace
    RDP-04 downstream routing
    RDP-06 verification evidence

RDP-02 must not redefine applicability status or manufacture input semantics inconsistent with this contract.

RDP-03 must bind committed Clinical Readiness to the exact input refs accepted under this contract.

RDP-06 must test all absence/stale/failure/version-mismatch cases.

---

## 14. BF-U05-RG-05 disposition

Design conclusion after A1 controlled amendment:

    non-A1 initial F3 hard dependency = REJECTED baseline retained
    A1 bootstrap F3 before ordinary U05 = REQUIRED
    U05 direct C03 invocation = PROHIBITED
    A1 C03 invocation occurs only through U06/F3 Owner path
    A1 pre-barrier/current-version-unvalidated F3 = NOT_CURRENT_FOR_D03
    ABSENT_BY_DESIGN and NOT_YET_APPLICABLE = MUTUALLY_EXCLUSIVE
    prior-version F3 after new fact = STALE until reevaluated
    absent/not-yet-applicable F3 -> READY inference = PROHIBITED
    BL-01 U01/F1 entry clarification -> U06 remains valid without mandatory U05
    post-safety U05 NEEDS_CLARIFICATION may consume lawful F1/F2 clarification input
    later F3 input is consumed only when already produced by its lawful owner path

Therefore the design issue identified by BF-U05-RG-05 has a concrete resolution.

Current status after independent-review remediation, before targeted re-review:

    BF-U05-RDP05-IR-01
    = REMEDIATED / RE_REVIEW_PENDING

    BF-U05-RDP05-IR-02
    = REMEDIATED / RE_REVIEW_PENDING

    RQ-U05-RDP05-IR-03
    = REMEDIATED / RE_REVIEW_PENDING

    BF-U05-RG-05
    = DESIGN_RESOLVED / TARGETED_REVIEW_PENDING

    U05-RDP-05
    = REVISED / READY_FOR_TARGETED_INDEPENDENT_REVIEW

Historical prior-baseline closure was:

    BF-U05-RG-05 = CLOSED
    U05-RDP-05 = FROZEN / PASS_FOR_READINESS

After A1 controlled amendment:

    BF-U05-RG-05
    = PRIOR_BASELINE_CLOSED / A1_AMENDMENT_REVIEW_PENDING

    U05-RDP-05 A1 affected scope
    = REFROZEN / V1

A1 amended contract has passed independent review and is now re-frozen at the governed exact head.

---

## 15. Authorization boundary

This document does not authorize:

    U05 implementation
    D03 owner execution
    U04->U05 live routing
    C03 invocation from U05
    U05->U06/U08/U10/U11 execution
    production mutation
    production Clinical Runtime
    release activation
    real-patient traffic

---

## 16. A1 Controlled Amendment — F3 Applicability / Version Binding

> Authorization: `AUTH-U05-A1-FROZEN-AMEND-001`  
> Reviewed design source: PR #138 exact head `7a62cc6f3b0cd9d803590594394bbed433351fab`  
> Status: **A1 REFROZEN / V1**

### 16.1 A1 F3 producer path

A1 F3 readiness input may only originate from：

```text
U06 PRE_READINESS_GAP_ASSESSMENT
→ C03
→ F3 Owner interpretation
→ canonical F3 commit
→ U06 F3_CURRENT_VERSION_REVALIDATION
→ normalized readiness input
```

U05 itself still does not call C03 or manufacture F3 semantics.

### 16.2 A1 current-version revalidation

If canonical F3 source state was created at an older Clinical State Version, D03 may consume it only after an explicit F3-owned current-version revalidation.

Revalidation must prove：

```text
F3 dependency fingerprint unchanged
+ historical semantic binding remains compatible
+ current U04 Gate is valid/routable
```

Result：

```text
REVALIDATED_CURRENT
→ F3 readiness input applicability_status = PRESENT
→ validity = CURRENT

REASSESSMENT_REQUIRED
→ no current F3 input
→ no U05/D03
→ fresh A1 pre-readiness assessment

FAILED
→ no current F3 input
→ no U05/D03
→ governed failure handling
```

### 16.3 Frozen envelope vocabulary retained

A1 F3 input uses：

```text
source_domain = F3
input_kind = ONLINE_INFORMATION_GAP
```

and the frozen business signals：

```text
CAN_ASK_MORE
NEEDS_OFFLINE_EVIDENCE
NO_ACTIVE_ONLINE_BLOCKING_GAP
```

```text
NO_ACTIVE_ONLINE_BLOCKING_GAP
!= READY_FOR_CLINICAL_ANALYSIS
```

### 16.4 Same-current-version rule

All PRESENT inputs entering D03 must still bind：

```text
same consultation_id
same cdp_id
same current Clinical State Version
validity = CURRENT
```

Canonical F3 historical source state may be older only when the current input envelope carries explicit：

```text
current_version_revalidation_ref
source_state_ref
source_decision_ref
current U04 Gate ref
```

No content-equality shortcut.

### 16.5 Current amendment status

```text
U05-RDP-05 A1 affected scope
= REFROZEN / V1

Prior RDP-05 freeze
= retained only for unaffected/non-A1 baseline semantics

Implementation Authorization
= NOT_GRANTED
```


---

## 17. Post-DDx Routing Consumption Amendment

> Authorization: `AUTH-U05-PDX-FROZEN-AMEND-001`  
> Reviewed design source: PR #153 exact head `a5b8aa6e23e5f54a0c2e1884ed027d7f7b7cbeee`  
> Status: **REFROZEN / V1**

### 17.1 POST_DDX_REEVALUATION consumption

RDP-05 继续冻结：

```text
POST_DDX_REEVALUATION
F3 = PRESENT expected
F5 = PRESENT expected
F6 = NOT_YET_APPLICABLE or PRESENT
```

但 post-DDx F5 signals 不再被假设全部必须进入 D03。

### 17.2 Signal consumers

```text
F5 = ANALYSIS_RESULT_AVAILABLE
F5 = REASSESSMENT_REQUIRED
```

可作为：

```text
U09 PostDdxRoutingDecision
```

的 current governed inputs。

在不存在更高优先级 Clinical Readiness path 时，exact consequence 冻结为：

```text
F5 = ANALYSIS_RESULT_AVAILABLE
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no blocking F6 need
→ TO_U12_DELIVERY_PREPARATION

F5 = REASSESSMENT_REQUIRED
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher Safety/acquisition/offline/scope path
→ TO_U08_REASSESSMENT
```

若存在：

```text
OUT_OF_SCOPE
blocking NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
F3 CAN_ASK_MORE
F5 NO_RELIABLE_DIRECTION
```

则必须优先：

```text
TO_U05_CLINICAL_READINESS
```

由 D03 按 frozen precedence 形成唯一 Clinical Readiness。

而：

```text
OUT_OF_SCOPE
blocking NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
F3 CAN_ASK_MORE
F5 NO_RELIABLE_DIRECTION
```

仍要求：

```text
TO_U05_CLINICAL_READINESS
→ D03
```

### 17.3 No semantic shortcut

禁止：

```text
ANALYSIS_RESULT_AVAILABLE
→ READY_FOR_CLINICAL_ANALYSIS

ANALYSIS_RESULT_AVAILABLE
→ Delivery Readiness READY

REASSESSMENT_REQUIRED
→ automatic U08 without higher-path checks
```

### 17.4 Currentness

PostDdxRoutingDecision consumed inputs must satisfy：

```text
same consultation_id
same cdp_id
current/compatible Clinical State Version
validity = CURRENT
```

Stale/failed/conflicting expected inputs cannot be interpreted as normal routing negatives.

### 17.5 Current status

```text
U05-RDP-05 post-DDx consumption scope
= REFROZEN / V1

BF-U05-RG02-CL-01
= REMEDIATED / REFROZEN / CLOSURE_REEVALUATION_PENDING
```


---

## 18. Post-Analysis Routing Consumption Extension

> Authorization: `AUTH-U05-PA-FROZEN-AMEND-001`  
> Reviewed design source: PR #155 exact head `7e2d4d4255d51a10f58c63dec4e2ccb53920c33f`  
> Status: **AMENDED / INDEPENDENT_REVIEW_PENDING**

### 18.1 Generalized consumer

Section 17 的 post-DDx consumer 被 generalized：

```text
PostAnalysisRoutingDecision
```

支持：

```text
POST_DDX_REEVALUATION
POST_OFFLINE_ASSESSMENT
```

### 18.2 POST_OFFLINE_ASSESSMENT F6 semantics

```text
F6 = NEEDS_OFFLINE_EVIDENCE
→ Clinical Readiness path / Safe Exit semantics

F6 = NO_BLOCKING_OFFLINE_EVIDENCE_NEED
→ removes only the blocking-offline-evidence condition
→ does NOT imply READY / delivery / completion
```

### 18.3 F3 ABSENT_BY_DESIGN

RDP-05 保持：

```text
ABSENT_BY_DESIGN
!= NO_ACTIVE_ONLINE_BLOCKING_GAP
```

若 POST_OFFLINE 当前 routing 需要 materialized F3 evidence，而：

```text
F3 = ABSENT_BY_DESIGN
```

则：

```text
TO_F3_CURRENT_VERSION_REVALIDATION
→ U06 MODE-3
```

只有：

```text
F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
validity = CURRENT
```

才可作为 delivery-preparation no-gap guard。

### 18.4 Exact post-offline consequences

不存在更高 Clinical Readiness path 时：

```text
F6 = NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ F5 = ANALYSIS_RESULT_AVAILABLE
+ F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
→ TO_U12_DELIVERY_PREPARATION

F6 = NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ F5 = REASSESSMENT_REQUIRED
+ no higher path
→ TO_U08_REASSESSMENT
```

### 18.5 Identity/currentness

PostAnalysisRoutingDecision consumed inputs 必须满足：

```text
same consultation_id
same cdp_id
current/compatible Clinical State Version
validity = CURRENT
evaluation_context included in routing identity
```

### 18.6 Current status

```text
U05-RDP-05 post-analysis consumption scope
= AMENDED / INDEPENDENT_REVIEW_PENDING

BF-U05-RG02-CL-02
= REMEDIATED_BY_DESIGN / REVIEW_PENDING
```
