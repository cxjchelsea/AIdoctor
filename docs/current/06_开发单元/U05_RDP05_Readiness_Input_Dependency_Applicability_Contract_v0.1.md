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

当前 evaluation context 合法地不要求该 input 存在。

例如：首次 Safety 后尚未进入 DDx，则 F5 input 可以 ABSENT_BY_DESIGN。

### NOT_YET_APPLICABLE

该业务域将在后续合法阶段才成为当前 readiness 求值的输入来源。

例如：尚未执行 U10/F6 assessment 时，F6 可以 NOT_YET_APPLICABLE。

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
| POST_SAFETY_INITIAL | PRESENT expected | ABSENT_BY_DESIGN or PRESENT if already available | NOT_YET_APPLICABLE | NOT_YET_APPLICABLE |
| POST_USER_FACT_UPDATE | PRESENT expected | ABSENT_BY_DESIGN or PRESENT if current-version F3 input exists | NOT_YET_APPLICABLE or STALE until recalculated | NOT_YET_APPLICABLE or STALE |
| POST_DDX_REEVALUATION | PRESENT/current | PRESENT expected after F3/U09 re-evaluation | PRESENT expected | NOT_YET_APPLICABLE or PRESENT |
| POST_OFFLINE_ASSESSMENT | PRESENT/current | PRESENT or ABSENT_BY_DESIGN | PRESENT/current if applicable | PRESENT expected |

矩阵中的 expected 表示：若该 source 在该 context 按 frozen flow 应当已有结果，但实际为 FAILED/UNAVAILABLE/STALE，则 U05 不得把缺失解释成 business negative。

---

## 7. Resolution of the initial F3 sequencing problem

### 7.1 Decision

首轮 U05 不把 F3 input 设为强制前置条件。

    POST_SAFETY_INITIAL
    → F3 may be ABSENT_BY_DESIGN

因此不存在：

    U05 must wait for U06/C03
    before U05 can run

U05 也不得为了填补 F3 缺席而直接调用 C03。

### 7.2 Why this does not manufacture readiness

F3 = ABSENT_BY_DESIGN 只表示当前 evaluation context 没有一个已形成、可参与 D03 的 F3 input。

它不能推出：

    CAN_ASK_MORE = false
    no important information gap
    READY_FOR_CLINICAL_ANALYSIS
    NO_RELIABLE_DIRECTION

因此 D03/RDP-02 必须定义 `READY_FOR_CLINICAL_ANALYSIS` 的正向成立条件。

如果 RDP-02 无法在合法的初始 input profile 下形成唯一结果而不发明医学语义，则必须把该问题作为 RDP-02 expectation gap 处理；不得回头把 F3 缺席偷偷解释为 READY。

### 7.3 How U06 remains legal

U06 可由两类合法来源进入：

1. F1/F2 的 NEEDS_CLARIFICATION；
2. 已存在 F3 readiness input 且 D03 = CAN_ASK_MORE。

对于 1：不要求先存在 F3 Gap。

对于 2：F3 input 必须已经由 F3 Owner 的合法路径形成，U05 只消费，不生成。

RDP-05 不改变 Phase 7 中 C03 的 Capability 边界，也不把 C03 的 FIRST_CONSUMER_UNIT 改成 U05。

### 7.4 Later F3 production

F3 input 可在后续合法路径形成或更新，例如：

    U06 clarification/question lifecycle consequences
    U09 post-DDx Gap re-evaluation
    correction / new fact invalidation followed by F3 re-evaluation

具体 C03 调用与 Gap 生成机制属于 U06/U09 readiness/implementation design，不由 U05-RDP-05 实现。

---

## 8. Version and identity binding

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

### Example A — initial clarification

    Safety Gate = ALLOW
    F1 = PRESENT / NEEDS_CLARIFICATION
    F3 = ABSENT_BY_DESIGN
    F5 = NOT_YET_APPLICABLE
    F6 = NOT_YET_APPLICABLE

RDP-05 conclusion:

    input profile is admissible
    D03 may evaluate NEEDS_CLARIFICATION according to RDP-02

### Example B — initial no F3

    Safety Gate = ALLOW
    F1 = PRESENT / FRAMED_IN_SCOPE
    F3 = ABSENT_BY_DESIGN
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

Design conclusion:

    initial F3 hard dependency = REJECTED
    U05 direct C03 invocation = PROHIBITED
    initial F3 ABSENT_BY_DESIGN = ALLOWED
    absent F3 -> READY inference = PROHIBITED
    F1/F2 clarification can route U06 without F3 Gap
    later F3 input is consumed only when already produced by its lawful owner path

Therefore the design issue identified by BF-U05-RG-05 has a concrete resolution.

Current status before independent review:

    BF-U05-RG-05
    = DESIGN_RESOLVED / REVIEW_PENDING

    U05-RDP-05
    = PROPOSED / READY_FOR_INDEPENDENT_REVIEW

Only after independent review may it become:

    BF-U05-RG-05 = CLOSED
    U05-RDP-05 = FROZEN / PASS_FOR_READINESS

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