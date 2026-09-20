# U05 RDP-02 Bootstrap Controlled Amendment Decision v0.1

> Scope: 解决 U05/D03 首轮 Clinical Readiness bootstrap underdetermination 的受控设计修订决策包。
>
> Status: PROPOSED / OWNER_DECISION_REQUIRED / UPSTREAM_AMENDMENT_NOT_AUTHORIZED
>
> Basis:
>
> - main@6e68fd9fb7cd19e87aadae30f3bb53a2264d1920
> - U05-RDP-05 frozen at fd0e88e21aaab2a2ab67ffd1449dce8e946d7ed5
> - U05-RDP-02 Independent Design Review = REVISE_REQUIRED at PR #127 head 1f1b4719a4eada62fda980da24f282cf9c35d650
>
> 本文件不修改任何 frozen Phase 4/6/7/RDP-05 语义，也不授予 implementation / routing / production / real-patient authorization。

---

## 1. Problem statement

当前合法首轮 profile：

    Safety Gate = ALLOW / permitted RESTRICTED
    F1 = PRESENT / FRAMED_IN_SCOPE
    F3 = NOT_YET_APPLICABLE
    F5 = NOT_YET_APPLICABLE
    F6 = NOT_YET_APPLICABLE

在现有 frozen semantics 下：

    F3 NOT_YET_APPLICABLE
    != no active gap
    != READY evidence

因此 D03 无法合法证明：

    CAN_ASK_MORE

或：

    READY_FOR_CLINICAL_ANALYSIS

与此同时：

    ordinary F3/C03 question path
    normally begins at U06

而：

    U06 F3 path
    normally follows U05 = CAN_ASK_MORE

形成 bootstrap underdetermination。

该问题定义为：

    U05_BOOTSTRAP_CROSS_PHASE_DESIGN_GAP

它不是患者业务结果，不是 Capability failure，也不是可由 D03 fallback 解决的问题。

---

## 2. Governance classification

本问题必须按：

    CONTROLLED UPSTREAM DESIGN AMENDMENT

处理，而不是：

    ordinary Owner parameter selection

原因：

- 任一可行解都会改变至少一份已经 frozen 的 readiness-input / unit / capability timing 设计；
- Owner approval 不能直接绕过 frozen contract 的 amendment + independent re-review；
- 在 amendment 完成前，不得授权 incomplete D03 runtime。

必须保持：

    Clinical Readiness has one G2/U05 resolver
    U05 has no independent AI Capability
    U05 must not invent Gap/DDx/offline evidence
    fixed completeness percentage = PROHIBITED
    legacy required-field checklist as truth = PROHIBITED
    null/no-gap -> READY = PROHIBITED
    LLM final readiness decision = PROHIBITED
    Clinical Truth != Runtime State != Trace

---

## 3. Candidate Amendment A — pre-D03 F3 sufficiency assessment

### A.1 Intent

在第一次普通 D03 evaluation 前，允许 F3 Owner 合法形成一个 current-version information-sufficiency / gap-state input，使 D03 能区分：

    high-value online gap exists
    -> CAN_ASK_MORE candidate input

或：

    no active high-value online blocking gap
    -> positive READY-policy evidence candidate

### A.2 Frozen artifacts potentially affected

至少需要受控复审：

    U05-RDP-05
    - POST_SAFETY_INITIAL F3 applicability
    - initial F3 hard-dependency semantics

    Phase 4 module/state ownership design
    - F3 readiness-input activation timing

    Phase 6 U05/U06 unit split
    - whether pre-D03 F3 assessment exists outside ordinary U06 question execution

可能影响：

    Phase 7 capability design

仅当该 pre-D03 F3 assessment 需要调用 C03 时，才会触及：

    C03 FIRST_CONSUMER_UNIT = U06

### A.3 Important sub-boundary

Candidate A 不等于自动允许：

    U05 -> C03

可以存在两种后续设计方向，但本文件不批准任何一种：

    A1. F3 Owner 使用 deterministic / non-C03 governed assessment
        -> Phase 7 C03 first consumer may remain U06

    A2. F3 Owner 需要 C03 before D03
        -> Phase 7 C03 first-consumer timing must be amended

二者都必须单独设计、review、freeze。

### A.4 Risks

- 可能把 U05 前置链路复杂化；
- 可能导致 F3 business owner 与 C03 capability timing 混淆；
- 如果 assessment 规则没有明确 Owner/RuleRelease，容易退化成旧 completeness heuristic；
- 若 pre-D03 与 U06/U09 后续 F3 使用不同语义，会形成双 Gap 真值。

### A.5 Minimum acceptance criteria

若选择 A，必须证明：

    one F3 owner
    one canonical F3 input schema
    same versioning / invalidation semantics
    no duplicate F3 truth
    no U05-owned gap generation
    no ungoverned model sufficiency decision

---

## 4. Candidate Amendment B — explicit non-F3 positive minimum-analysis input

### B.1 Intent

新增一个明确治理的 positive minimum-analysis-condition input producer，使 D03 不依赖首轮 F3 就能正向证明：

    minimum analysis condition satisfied

### B.2 Frozen artifacts potentially affected

至少需要受控复审：

    Phase 4 Clinical Readiness input-source model

当前 frozen model：

    F1 / F3 / F5 / F6
    -> Clinical Readiness Resolver

RDP-05 current source domains：

    F1
    F2_CLARIFICATION
    F3
    F5
    F6

因此新增 non-F3 positive producer 会改变 frozen input model。

还需复审：

    U05-RDP-05 canonical readiness input envelope/source_domain
    Phase 6 U05 S_in

### B.3 Producer ownership must be explicit

不得使用匿名：

    completeness service
    controller heuristic
    frontend score
    D03 internal raw-fact inspection

必须指定一个现有或经批准新增的 business owner。

若考虑 F2 作为 producer，必须明确：

    F2 patient-fact ownership
    != automatic authority to decide minimum-analysis sufficiency

任何 F2 扩权都必须单独批准。

### B.4 Risks

- 新增 readiness input source 可能稀释 F3 对 information-gap/sufficiency 的 Owner 边界；
- 可能出现 F2/F3 双重充分性判断；
- 可能退化成“字段够不够”的旧逻辑；
- 增加新的 cross-module invalidation dependency。

### B.5 Minimum acceptance criteria

若选择 B，必须证明：

    named single business owner
    explicit positive business semantics
    no duplicate sufficiency owner
    current-version provenance
    invalidation rules
    no completeness threshold shortcut
    no raw model output -> readiness shortcut

---

## 5. Candidate comparison

| Dimension | Candidate A: pre-D03 F3 assessment | Candidate B: non-F3 positive input |
|---|---|---|
| 保持 F3 sufficiency Owner | 更容易 | 风险较高 |
| 是否必改 RDP-05 | 是 | 是 |
| 是否可能改 Phase 7 C03 timing | 可能，取决于 A1/A2 | 通常不需要 |
| 是否改 Phase 4 input-source model | 可能只改 timing | 是 |
| 双 Owner 风险 | 中 | 高 |
| 旧 completeness 逻辑回流风险 | 中 | 高 |
| 与现有 F3 Gap 生命周期一致性 | 较高 | 较低 |
| 设计复杂度 | 中 | 中-高 |

此表用于决策，不构成方案批准或排名授权。

---

## 6. Owner decisions required

必须显式回答：

    OD-U05-BOOTSTRAP-01
    Which bootstrap amendment family is authorized for detailed design?
    = A / B / REJECT_BOTH_AND_REDESIGN

如果选择 A：

    OD-U05-BOOTSTRAP-02A
    Can pre-D03 F3 assessment be deterministic/non-C03?
    Or is C03 required before D03?

如果选择 B：

    OD-U05-BOOTSTRAP-02B
    Which named business owner may produce the new positive minimum-analysis input?

无论选择何种方案，还必须批准：

    OD-U05-READY-01
    Is the following V1 post-F3 combination sufficient positive evidence for READY_FOR_CLINICAL_ANALYSIS?

    F1 = FRAMED_IN_SCOPE
    + current F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
    + no higher-priority blocker
    + all required current inputs valid

Decision:

    APPROVE
    / REVISE
    / REJECT

该 READY rule 在 Owner 决策前仅为 proposal。

---

## 7. Required amendment sequence

正确顺序：

    1. Owner selects amendment family
    2. Produce detailed amendment design
    3. Identify exact frozen artifacts affected
    4. Amend only authorized artifacts
    5. Independent re-review affected upstream artifacts
    6. Re-freeze affected contracts
    7. Update RDP-02 against the new frozen baseline
    8. Targeted RDP-02 re-review
    9. Only then consider BF-U05-RG-02 CLOSED

禁止：

    Owner says "A"
    -> directly implement runtime

也禁止：

    amend RDP-05
    -> silently retain old frozen review result

任何被修改的 frozen artifact 都必须重新 review/freeze。

---

## 8. Current disposition

    U05_BOOTSTRAP_CROSS_PHASE_DESIGN_GAP
    = OPEN

    Controlled Amendment Decision Package
    = PROPOSED / OWNER_DECISION_REQUIRED

    Upstream amendment
    = NOT_AUTHORIZED

    BF-U05-RDP02-IR-02
    = REMEDIATION_DESIGNED / OWNER_DECISION_PENDING

    BF-U05-RG-02
    = NOT_CLOSED

---

## 9. Authorization boundary

This document does not authorize:

    modification of frozen Phase 4/6/7 semantics
    modification of frozen U05-RDP-05
    pre-D03 F3 execution
    C03 first-consumer timing change
    new readiness input producer
    U05 implementation
    U04->U05 live routing
    downstream owner execution
    production Clinical State mutation
    production Clinical Runtime
    release activation
    real-patient traffic
