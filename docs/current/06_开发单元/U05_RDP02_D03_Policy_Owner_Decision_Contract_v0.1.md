# U05 RDP-02 D03 Policy / Owner Decision Contract v0.1

> Scope: U05 / D03 Clinical Readiness deterministic policy, owner boundary, decision semantics, precedence and fail-closed behavior.
> Status: PROPOSED / READY_FOR_INDEPENDENT_REVIEW_WITH_POLICY_GAP
> Review basis: main@6e68fd9fb7cd19e87aadae30f3bb53a2264d1920
> Dependency: U05-RDP-05 = FROZEN / PASS_FOR_READINESS at fd0e88e21aaab2a2ab67ffd1449dce8e946d7ed5
> 本文件只处理 BF-U05-RG-02；不授予 U05 implementation、owner execution、routing、production 或 real-patient authorization。

---

## 1. Frozen upstream constraints

D03 是 U05 的唯一 Clinical Readiness deterministic decision。

    readiness inputs
    -> D03
    -> exactly one Clinical Readiness when legally decidable

U05 没有独立 AI Capability。

    model / agent / frontend
    != D03 owner

必须保持：

    Capability Result != Deterministic Decision
    Deterministic Decision != StateChangeProposal
    StateChangeProposal != CommitResult

正式 Clinical Readiness 只能在后续 RDP-03 定义的 K09/G2/P01 路径中提交。

Safety Gate 先于普通 D03：

    BLOCKED / UNAVAILABLE
    -> ordinary U05 D03 prohibited

ALLOW 或被当前动作明确允许的 RESTRICTED 才可进入普通 D03 evaluation。

---

## 2. D03 business owner

    business/system-level owner
    = G2 Clinical Readiness Resolver

    development unit
    = U05

    deterministic policy
    = D03

F1/F2 clarification、F3、F5、F6 只提供 RDP-05 已冻结 readiness input；不得直接写最终 Clinical Readiness。

U05/D03 不重新解释原始患者文本、不生成 Gap、不做 DDx、不判断 Must-Exclude、不生成 Offline Evidence Need。

---

## 3. D03 decision envelope

D03 使用 Phase 8 generic Deterministic Decision Contract，并增加 U05 所需的最小 status 语义。

最小字段：

    decision_id
    decision_type = D03_CLINICAL_READINESS
    consultation_id
    cdp_id
    input_clinical_state_version

    evaluation_context
    decision_status
    clinical_readiness
    reason_codes[]
    basis_refs[]
    input_refs[]

    policy_id = D03
    policy_version
    rule_release_refs[]
    knowledge_release_refs[]

    safety_gate_ref
    restricted_context_ref
    created_at
    validity / staleness

decision_status 冻结为：

    DECIDED
    INPUT_FAILURE
    INPUT_CONFLICT
    POLICY_EXPECTATION_GAP

只有：

    decision_status = DECIDED

才允许 clinical_readiness 为正式六值之一。

其他 status：

    clinical_readiness = absent
    no K09 readiness proposal
    no Clinical Readiness commit
    no ordinary downstream route

---

## 4. Formal Clinical Readiness vocabulary

当 decision_status = DECIDED 时，只允许：

    OUT_OF_SCOPE
    NEEDS_OFFLINE_EVIDENCE
    NEEDS_CLARIFICATION
    CAN_ASK_MORE
    READY_FOR_CLINICAL_ANALYSIS
    NO_RELIABLE_DIRECTION

不得扩展：

    UNKNOWN
    DEFAULT_READY
    PARTIAL_READY
    LOW_RISK_READY
    FALLBACK_READY

技术失败和规则缺口必须走 decision_status，而不是伪造第七个 Clinical Readiness。

---

## 5. Admission before policy evaluation

D03 evaluation 前必须满足：

    U04 consumer admission = accepted
    committed/current Safety Gate = ALLOW or permitted RESTRICTED
    current Clinical State Version = exact match
    RDP-05 applicability profile = valid
    all PRESENT inputs = current and identity-compatible
    no required input = FAILED / UNAVAILABLE / STALE

否则：

    decision_status = INPUT_FAILURE
    no readiness decision

建议 reason family：

    D03_STALE_READINESS_INPUT
    D03_REQUIRED_INPUT_FAILED
    D03_REQUIRED_INPUT_UNAVAILABLE
    D03_INPUT_IDENTITY_MISMATCH
    D03_INPUT_VERSION_MISMATCH
    D03_SAFETY_GATE_NOT_ELIGIBLE

最终 reason code 命名可在 RDP-03/RDP-06 一致化，但业务区别不得合并。

---

## 6. Input conflict rules

同一 current Clinical State Version 内，如果出现互斥输入，不允许靠 precedence 随便挑一个。

至少包括：

    F1 OUT_OF_SCOPE + F1 FRAMED_IN_SCOPE
    same owner emits mutually exclusive signals
    same readiness_input_id maps to conflicting business signals
    same source decision ref appears with incompatible validity

此时：

    decision_status = INPUT_CONFLICT
    no Clinical Readiness
    no commit

reason：

    D03_MUTUALLY_EXCLUSIVE_INPUT_CONFLICT

注意：

    cross-domain signals with legitimate business precedence
    != input conflict

例如：

    F3 CAN_ASK_MORE
    + F6 qualified blocking NEEDS_OFFLINE_EVIDENCE

可以由已冻结业务 precedence 解析，不自动视为数据冲突。

---

## 7. Qualified business signals

D03 只消费业务 Owner 已经解释完成的 signal，不从 evidence 原文重新推断。

### 7.1 OUT_OF_SCOPE

必须来自 current F1 scope/framing owner 的正式 OUT_OF_SCOPE input。

    extracted semantic hint
    != OUT_OF_SCOPE final input

### 7.2 NEEDS_OFFLINE_EVIDENCE

只有“阻断当前正常结论或 Must-Exclude 处置”的 offline evidence signal 才参与该 readiness outcome。

允许来源：

    F3 qualified OFFLINE_REQUIRED
    F5 Must-Exclude / DDx qualified offline blocker
    F6 VALID + JUSTIFIED blocking offline evidence need

非阻断性 offline information：

    != NEEDS_OFFLINE_EVIDENCE readiness

### 7.3 NEEDS_CLARIFICATION

允许来源：

    F1 lawful clarification input
    F2_CLARIFICATION lawful clarification input

只表示对象/问题/关键表达需要可靠澄清。

### 7.4 CAN_ASK_MORE

必须来自 current F3 owner：

    PRESENT / CAN_ASK_MORE

并已经代表：

    online obtainable
    + actual expected decision value

D03 不重新计算 question value。

### 7.5 NO_RELIABLE_DIRECTION

必须至少有 current F5：

    PRESENT / NO_RELIABLE_DIRECTION

并同时有足够的 current producer evidence 证明不存在更高优先级合法 online/offline path。

最小 V1 组合：

    F5 = NO_RELIABLE_DIRECTION
    + F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
    + no qualified blocking offline signal
    + no clarification / out-of-scope signal

缺少 current F3 no-online-gap 证明时，不得直接输出 NO_RELIABLE_DIRECTION。

---

## 8. Frozen business precedence

先处理 P0/P1 technical/governance conditions：

    P0 admission/input failure
    -> INPUT_FAILURE

    P1 mutually exclusive input conflict
    -> INPUT_CONFLICT

只有通过 P0/P1 后，才按 Phase 5 业务 precedence：

    P2 OUT_OF_SCOPE
    -> OUT_OF_SCOPE

    P3 qualified blocking NEEDS_OFFLINE_EVIDENCE
    -> NEEDS_OFFLINE_EVIDENCE

    P4 lawful NEEDS_CLARIFICATION
    -> NEEDS_CLARIFICATION

    P5 F3 CAN_ASK_MORE
    -> CAN_ASK_MORE

    P6 positive READY condition
    -> READY_FOR_CLINICAL_ANALYSIS

    P7 qualified NO_RELIABLE_DIRECTION
    -> NO_RELIABLE_DIRECTION

这不是简单严重度排序。

特别是：

    blocking offline need
    > CAN_ASK_MORE

仅在该 offline need 已由业务 Owner 明确为 blocking 时成立。

如果 offline gap 不阻断当前业务目标，而 F3 仍有高价值 online Gap：

    CAN_ASK_MORE remains eligible

---

## 9. Positive READY condition

RDP-05 已冻结：

    admissible input profile
    != proof of READY

以及：

    F3 NOT_YET_APPLICABLE
    != no gap
    != READY evidence

因此 RDP-02 不允许：

    no higher-priority signal
    -> automatically READY

### 9.1 Legally supportable READY rule after F3 is active

当前冻结语义能够支持的最小正向组合是：

    F1 = PRESENT / FRAMED_IN_SCOPE
    + F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
    + no qualified blocking offline signal
    + no NEEDS_CLARIFICATION
    + no OUT_OF_SCOPE
    + no required input failure/stale/unavailable

在进入 DDx 前：

    F5 = NOT_YET_APPLICABLE
    F6 = NOT_YET_APPLICABLE

可以是合法状态。

该组合的业务含义仅是：

    current scope/framing is established
    + F3 owner has positively established no current high-value online blocking Gap
    + no higher-priority blocker exists

因此 D03 可形成：

    READY_FOR_CLINICAL_ANALYSIS

它不表示：

    diagnosis is known
    no disease
    patient is safe
    no future Gap can emerge after DDx

### 9.2 Initial bootstrap gap remains unresolved

当前首轮合法 profile 可能是：

    Safety Gate = ALLOW / permitted RESTRICTED
    F1 = PRESENT / FRAMED_IN_SCOPE
    F3 = NOT_YET_APPLICABLE
    F5 = NOT_YET_APPLICABLE
    F6 = NOT_YET_APPLICABLE

现有冻结规则无法证明：

    CAN_ASK_MORE
    or READY_FOR_CLINICAL_ANALYSIS

也不能合法推出：

    NO_RELIABLE_DIRECTION
    NEEDS_OFFLINE_EVIDENCE

因此必须：

    decision_status = POLICY_EXPECTATION_GAP
    clinical_readiness = absent
    no commit
    no ordinary route

reason：

    D03_INITIAL_READINESS_BOOTSTRAP_UNDERDETERMINED

这不是患者业务结果，也不是 Capability failure。

---

## 10. Initial bootstrap Owner Decision Gap

该 gap 的根因不是 D03 precedence，而是首轮 positive sufficiency evidence 没有合法 producer。

当前冻结设计同时要求：

    F3 owns information-gap / information-sufficiency semantics
    C03 FIRST_CONSUMER_UNIT = U06
    U05 direct C03 invocation = PROHIBITED
    U06 ordinary F3 path normally follows U05 CAN_ASK_MORE

因此形成 bootstrap tension：

    no F3 yet
    -> D03 cannot prove CAN_ASK_MORE or READY
    -> ordinary U06 F3 path has no lawful D03 trigger

RDP-02 不擅自修改 Phase 6/7。

需要后续 Owner 决策在以下方向中冻结一种合法方案：

    OPTION A
    define a lawful pre-D03 F3 information-sufficiency assessment boundary
    while keeping U05 from invoking C03 directly

    OPTION B
    define an explicitly governed non-F3 positive minimum-analysis-condition input
    with a named business owner and source contract

禁止方案：

    D03 reads arbitrary raw facts and invents its own sufficiency rule
    fixed completeness percentage
    hard-coded required-field checklist from legacy InformationGapIdentifier
    null/no-gap -> READY
    model/LLM decides READY

在 Owner 决策冻结前：

    BF-U05-RG-02 cannot close

---

## 11. NO_RELIABLE_DIRECTION guard

NO_RELIABLE_DIRECTION 只能是合法业务 negative，不是 fallback。

必须同时满足：

    F5 assessment completed normally
    F5 = NO_RELIABLE_DIRECTION
    current F3 = no active high-value online blocking Gap
    no blocking offline evidence path
    no clarification path
    no out-of-scope path
    no required input failure

禁止：

    F5 not run -> NO_RELIABLE_DIRECTION
    F5 FAILED -> NO_RELIABLE_DIRECTION
    input missing -> NO_RELIABLE_DIRECTION

---

## 12. RESTRICTED context

当 Safety Gate = RESTRICTED 且当前动作明确允许进入 U05：

    restricted context must be preserved in D03 basis_refs / restricted_context_ref

D03 不得：

    RESTRICTED -> ALLOW
    RESTRICTED -> SAFE

RDP-02 本身不决定后续 U08/U10/U11 是否允许具体动作；它只确保任何 readiness decision 不擦除 RESTRICTED context。

---

## 13. Determinism and uniqueness

同一组：

    policy_version
    + current Clinical State Version
    + exact accepted readiness input refs
    + exact Safety Gate ref

必须得到相同：

    decision_status
    decision when DECIDED
    reason_codes

禁止：

    random/model-dependent precedence
    frontend-specific readiness
    two simultaneous formal readiness values

---

## 14. Suggested reason-code families

业务 DECIDED：

    D03_OUT_OF_SCOPE
    D03_BLOCKING_OFFLINE_EVIDENCE_REQUIRED
    D03_CLARIFICATION_REQUIRED
    D03_HIGH_VALUE_ONLINE_GAP_AVAILABLE
    D03_MINIMUM_ANALYSIS_CONDITIONS_SATISFIED
    D03_NO_RELIABLE_DIRECTION

非业务 decision：

    D03_STALE_READINESS_INPUT
    D03_REQUIRED_INPUT_FAILED
    D03_REQUIRED_INPUT_UNAVAILABLE
    D03_INPUT_IDENTITY_MISMATCH
    D03_INPUT_VERSION_MISMATCH
    D03_MUTUALLY_EXCLUSIVE_INPUT_CONFLICT
    D03_INITIAL_READINESS_BOOTSTRAP_UNDERDETERMINED

reason code 是治理/技术标识，不是临床诊断。

---

## 15. Executable expectation matrix

| Case | Current inputs | Expected D03 status/result |
|---|---|---|
| D03-POL-001 | current F1 OUT_OF_SCOPE | DECIDED / OUT_OF_SCOPE |
| D03-POL-002 | qualified blocking offline signal | DECIDED / NEEDS_OFFLINE_EVIDENCE |
| D03-POL-003 | lawful F1/F2 NEEDS_CLARIFICATION | DECIDED / NEEDS_CLARIFICATION |
| D03-POL-004 | current F3 CAN_ASK_MORE, no higher blocker | DECIDED / CAN_ASK_MORE |
| D03-POL-005 | F1 FRAMED_IN_SCOPE + current F3 NO_ACTIVE_ONLINE_BLOCKING_GAP + no higher blocker | DECIDED / READY_FOR_CLINICAL_ANALYSIS |
| D03-POL-006 | F5 NO_RELIABLE_DIRECTION + current F3 NO_ACTIVE_ONLINE_BLOCKING_GAP + no higher path | DECIDED / NO_RELIABLE_DIRECTION |
| D03-POL-007 | initial F1 FRAMED_IN_SCOPE + F3/F5/F6 NOT_YET_APPLICABLE | POLICY_EXPECTATION_GAP / no readiness |
| D03-POL-008 | stale required F3 | INPUT_FAILURE / no readiness |
| D03-POL-009 | mutually exclusive same-owner signals | INPUT_CONFLICT / no readiness |
| D03-POL-010 | RESTRICTED allowed + valid readiness inputs | same business result as policy, restricted context preserved |

RDP-06 必须把这些场景纳入 durable verification。

---

## 16. Relationship to other U05 RDPs

RDP-05：冻结 input source/applicability/version semantics。

RDP-01：应在 U05 consumer admission 中验证 U04 committed Gate 与基础 identity。

RDP-03：只能对 decision_status = DECIDED 的 D03 result 创建 readiness StateChangeProposal。

RDP-04：只能从 committed/current Clinical Readiness 暴露 downstream eligibility。

RDP-06：必须验证 precedence、conflict、policy expectation gap、replay/idempotency、RESTRICTED preservation。

---

## 17. BF-U05-RG-02 disposition

已解决：

    D03 owner boundary
    deterministic decision envelope
    formal readiness vocabulary
    input failure / conflict separation
    Phase-5 precedence
    qualified offline precedence
    clarification / online-gap mapping
    post-F3 positive READY rule
    NO_RELIABLE_DIRECTION guard
    RESTRICTED preservation
    fail-closed POLICY_EXPECTATION_GAP semantics

尚未解决：

    initial readiness bootstrap positive evidence producer

因此当前状态：

    BF-U05-RG-02
    = PARTIALLY_DESIGN_RESOLVED
    = BLOCKED_BY_INITIAL_READINESS_BOOTSTRAP_OWNER_DECISION

    U05-RDP-02
    = PROPOSED / READY_FOR_INDEPENDENT_REVIEW_WITH_POLICY_GAP

    U05-RDP-02
    != FROZEN

只有 bootstrap Owner Decision 被明确冻结并通过独立 review 后，BF-U05-RG-02 才能 CLOSED。

---

## 18. Authorization boundary

This document does not authorize:

    U05 implementation
    D03 owner execution
    U04->U05 live routing
    C03 invocation from U05
    pre-D03 F3 execution
    U05 downstream owner execution
    production Clinical State mutation
    production Clinical Runtime
    release activation
    real-patient traffic