# U05 RDP-02 D03 Policy / Owner Decision Contract v0.1

> Scope: U05 / D03 Clinical Readiness deterministic policy, owner boundary, decision semantics, precedence and fail-closed behavior.
> Status: READY-POLICY REFROZEN / V1
> Review basis: main@6e68fd9fb7cd19e87aadae30f3bb53a2264d1920
> Dependency baseline: U05-RDP-05 prior FROZEN / PASS_FOR_READINESS at fd0e88e21aaab2a2ab67ffd1449dce8e946d7ed5; A1 affected scope is now REFROZEN / V1 under AUTH-U05-A1-FROZEN-AMEND-001
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

runtime decision_status proposal 仅允许：

    DECIDED
    INPUT_FAILURE
    INPUT_CONFLICT

POLICY_EXPECTATION_GAP 不属于可授权实现的正常 runtime decision_status。

它只作为：

    design/readiness verification sentinel

用于表示“存在合法 admitted profile，但当前 frozen D03 policy 尚无唯一结果”。

只要该 sentinel 仍可被合法输入触发：

    D03 executable policy = INCOMPLETE
    U05 Implementation Readiness = NOT_READY
    Implementation Authorization = PROHIBITED

只有：

    decision_status = DECIDED

才允许 clinical_readiness 为正式六值之一。

INPUT_FAILURE / INPUT_CONFLICT：

    clinical_readiness = absent
    no K09 readiness proposal
    no Clinical Readiness commit
    no ordinary downstream route

design/readiness sentinel POLICY_EXPECTATION_GAP：

    not a normal runtime decision object
    no production/runtime authorization
    no readiness commit
    no ordinary route

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

技术失败/输入冲突必须走 runtime decision_status，而不是伪造第七个 Clinical Readiness。

规则缺口必须走 design/readiness sentinel，不得伪装成 runtime decision_status。

---

## 5. Pre-D03 admission boundary and post-admission input validation

U05-RDP-01 owns pre-D03 consumer admission.

Required sequence:

    U04 committed/current outbound boundary
    -> U05-RDP-01 consumer admission
    -> admitted U05 input envelope
    -> D03

Pre-D03 rejection includes at least:

    malformed or missing U04 handoff identity
    stale/mismatched U04 Clinical State Version
    uncommitted Safety Gate claim
    Safety Gate = BLOCKED / UNAVAILABLE
    RESTRICTED context not authorized/preserved
    replay/idempotency admission conflict

These cases produce:

    typed RDP-01 admission failure
    no D03 decision_id
    no D03 decision_status
    no readiness decision

Only an admitted envelope may reach D03.

After admission, D03 validates readiness inputs:

    current Clinical State Version = exact match
    RDP-05 applicability profile = valid
    all PRESENT inputs = current and identity-compatible
    no required readiness input = FAILED / UNAVAILABLE / STALE

Post-admission readiness-input failure may produce:

    decision_status = INPUT_FAILURE
    clinical_readiness = absent

Suggested D03 reason family:

    D03_STALE_READINESS_INPUT
    D03_REQUIRED_INPUT_FAILED
    D03_REQUIRED_INPUT_UNAVAILABLE
    D03_INPUT_IDENTITY_MISMATCH
    D03_INPUT_VERSION_MISMATCH

D03_SAFETY_GATE_NOT_ELIGIBLE is removed from the D03 reason family unless a future frozen RDP-01 contract explicitly admits such a state into D03.

Final reason-code naming may be aligned in RDP-03/RDP-06, but these business/governance distinctions must remain separate.

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

    P0 post-admission readiness-input failure
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

### 9.1 Owner-approved V1 READY policy — first clinical-analysis entry only

Owner decision：

```text
OD-U05-READY-01
= APPROVE
```

Decision basis：

```text
PR #145 exact reviewed head
= 9adab885db20e902b9c705d566651acd1a625739
```

The approved rule is context-scoped：

```text
policy_rule = D03-POL-005
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_ONLY
```

Exact positive condition：

```text
F1 = PRESENT / FRAMED_IN_SCOPE
+ F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
+ F5 applicability = NOT_YET_APPLICABLE
+ F6 applicability = NOT_YET_APPLICABLE
+ no qualified blocking offline signal
+ no lawful NEEDS_CLARIFICATION
+ no OUT_OF_SCOPE
+ no required input failure / stale / unavailable
→ DECIDED / READY_FOR_CLINICAL_ANALYSIS
```

The F5/F6 applicability guards are executable scope guards, not explanatory text.

Therefore D03-POL-005 is NOT applicable when：

```text
evaluation context = POST_DDX_REEVALUATION
or F5 is PRESENT
or F6 is PRESENT as an applicable producer result
or F5 = NO_RELIABLE_DIRECTION
or F5 = ANALYSIS_RESULT_AVAILABLE
or F5 = REASSESSMENT_REQUIRED
```

D03-POL-005 cannot override D03-POL-006.

The exact meaning of：

```text
READY_FOR_CLINICAL_ANALYSIS
```

is only：

```text
current information is sufficient to lawfully enter governed U08/F5 clinical analysis
```

It does NOT mean：

```text
diagnosis is known or confirmed
patient is safe
no disease is present
no future information gap can emerge
no later offline evidence will be needed
Must-Exclude is complete
consultation may complete
delivery is allowed
```

Current governance classification：

```text
D03-POL-005
= FROZEN_EXECUTABLE_EXPECTATION
```

### 9.2 Initial bootstrap gap — A1 controlled resolution

Owner 已选择：

    OD-U05-BOOTSTRAP-01 = A1

因此以下 profile：

    Safety Gate = ALLOW / permitted RESTRICTED
    F1 = PRESENT / FRAMED_IN_SCOPE
    F3 = NOT_YET_APPLICABLE
    F5 = NOT_YET_APPLICABLE
    F6 = NOT_YET_APPLICABLE

在 A1 下**不再是一个 admitted D03 profile**。

当 bootstrap F3 尚未完成/current-version revalidated：

    U04 routing / U05 inbound admission
    → U05 ordinary readiness evaluation is not eligible
    → D03 is not invoked
    → no D03 decision_id
    → no D03 decision_status
    → route remains in A1 pre-readiness / Safety barrier / F3 revalidation path.

只有 A1 产生 current-compatible F3 readiness input 后，U05 才可进入正常 D03 precedence。

POLICY_EXPECTATION_GAP 仍保留为一般 design/readiness sentinel，用于其他合法 admitted configuration 中尚无唯一 policy coverage 的情况；它不再承担 A1 initial bootstrap 的正常运行路径。

---

## 10. Initial bootstrap controlled amendment gap

该 gap 的根因不是 D03 precedence，而是首轮 positive sufficiency evidence 没有合法 producer。

Independent Design Review 已确认：

    bootstrap gap
    = CROSS-PHASE DESIGN INCONSISTENCY / AMENDMENT REQUIRED
    != ordinary unresolved Owner parameter

本问题由独立文件管理：

    U05_RDP02_Bootstrap_Controlled_Amendment_Decision_v0.1.md

当前冻结设计同时要求：

    F3 owns information-gap / information-sufficiency semantics
    C03 FIRST_CONSUMER_UNIT = U06
    U05 direct C03 invocation = PROHIBITED
    U06 ordinary F3 path normally follows U05 CAN_ASK_MORE

因此形成 bootstrap tension：

    no F3 yet
    -> D03 cannot prove CAN_ASK_MORE or READY
    -> ordinary U06 F3 path has no lawful D03 trigger

RDP-02 不擅自修改 Phase 4/6/7 或 frozen RDP-05。

Controlled Amendment package 当前列出候选 family：

    Candidate A
    pre-D03 F3 sufficiency assessment

    Candidate B
    explicit non-F3 positive minimum-analysis input

但：

    Owner selection
    != amendment authorization
    != re-freeze
    != implementation authorization

任何选择都必须先明确受影响 frozen artifacts，并完成 amendment + independent re-review + re-freeze。

禁止方案：

    D03 reads arbitrary raw facts and invents its own sufficiency rule
    fixed completeness percentage
    hard-coded required-field checklist from legacy InformationGapIdentifier
    null/no-gap -> READY
    model/LLM decides READY

在 Controlled Amendment Owner Decision、受影响上游 amendment、independent re-review 与 re-freeze 完成前：

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

## 14. Suggested reason / sentinel code families

业务 DECIDED：

    D03_OUT_OF_SCOPE
    D03_BLOCKING_OFFLINE_EVIDENCE_REQUIRED
    D03_CLARIFICATION_REQUIRED
    D03_HIGH_VALUE_ONLINE_GAP_AVAILABLE
    D03_MINIMUM_ANALYSIS_CONDITIONS_SATISFIED  [FROZEN_EXECUTABLE_EXPECTATION]
    D03_NO_RELIABLE_DIRECTION

非业务 runtime decision：

    D03_STALE_READINESS_INPUT
    D03_REQUIRED_INPUT_FAILED
    D03_REQUIRED_INPUT_UNAVAILABLE
    D03_INPUT_IDENTITY_MISMATCH
    D03_INPUT_VERSION_MISMATCH
    D03_MUTUALLY_EXCLUSIVE_INPUT_CONFLICT

design/readiness sentinel：

    D03_INITIAL_READINESS_BOOTSTRAP_UNDERDETERMINED

sentinel code 不是 runtime D03 reason code，也不是临床诊断。

---

## 15. Policy expectation matrix

| Case | Current inputs | Expected D03 status/result |
|---|---|---|
| D03-POL-001 | current F1 OUT_OF_SCOPE | DECIDED / OUT_OF_SCOPE |
| D03-POL-002 | qualified blocking offline signal | DECIDED / NEEDS_OFFLINE_EVIDENCE |
| D03-POL-003 | lawful F1/F2 NEEDS_CLARIFICATION | DECIDED / NEEDS_CLARIFICATION |
| D03-POL-004 | current F3 CAN_ASK_MORE, no higher blocker | DECIDED / CAN_ASK_MORE |
| D03-POL-005 | FIRST_CLINICAL_ANALYSIS_ENTRY_ONLY: F1 FRAMED_IN_SCOPE + current F3 NO_ACTIVE_ONLINE_BLOCKING_GAP + F5 NOT_YET_APPLICABLE + F6 NOT_YET_APPLICABLE + no higher blocker | FROZEN_EXECUTABLE_EXPECTATION: DECIDED / READY_FOR_CLINICAL_ANALYSIS |
| D03-POL-006 | F5 NO_RELIABLE_DIRECTION + current F3 NO_ACTIVE_ONLINE_BLOCKING_GAP + no higher path | DECIDED / NO_RELIABLE_DIRECTION |
| D03-POL-007 | A1 bootstrap incomplete: F1 FRAMED_IN_SCOPE + F3 not yet current | PRE-D03 NON-ENTRY: U05/D03 not invoked; no D03 object/status; route remains A1 pre-readiness/barrier/revalidation |
| D03-POL-008 | stale required F3 | INPUT_FAILURE / no readiness |
| D03-POL-009 | mutually exclusive same-owner signals | INPUT_CONFLICT / no readiness |
| D03-POL-010 | RESTRICTED allowed + valid readiness inputs | same business result as policy, restricted context preserved |

RDP-06 必须区分：

    FROZEN_EXECUTABLE_EXPECTATION
    PROPOSED_OWNER_EXPECTATION
    DESIGN_SENTINEL_CASE

只有已经 Owner-approved + frozen 的 expectation 才能作为实现验收的正式 expected business result。

---

## 16. Relationship to other U05 RDPs

RDP-05：冻结 input source/applicability/version semantics。

RDP-01：应在 U05 consumer admission 中验证 U04 committed Gate 与基础 identity。

RDP-03：只能对 decision_status = DECIDED 的 D03 result 创建 readiness StateChangeProposal。

RDP-04：只能从 committed/current Clinical Readiness 暴露 downstream eligibility。

RDP-06：必须验证 precedence、conflict、replay/idempotency、RESTRICTED preservation，并把 policy expectation gap 作为 design/readiness blocker evidence，而不是正常 runtime case。

---

## 17. BF-U05-RG-02 disposition

已解决或整改完成：

    D03 owner boundary
    formal six-value readiness vocabulary
    post-admission input failure / input conflict separation
    pre-D03 admission ownership returned to RDP-01
    Phase-5 precedence
    qualified offline precedence
    clarification / online-gap mapping
    NO_RELIABLE_DIRECTION guard
    RESTRICTED preservation
    POLICY_EXPECTATION_GAP reclassified as design/readiness sentinel

Owner / controlled amendment status：

    OD-U05-READY-01
    = APPROVE

    D03-POL-005
    = FIRST_CLINICAL_ANALYSIS_ENTRY_ONLY
    = FROZEN_EXECUTABLE_EXPECTATION

    U05_BOOTSTRAP_CROSS_PHASE_DESIGN_GAP
    = A1 selected
    = controlled amendment applied
    = independently reviewed
    = re-frozen

因此当前状态：

    BF-U05-RDP02-IR-01
    = CLOSED_BY_OWNER_APPROVAL_AND_READY_POLICY_REFREEZE

    BF-U05-RDP02-IR-02
    = CLOSED_BY_A1_CONTROLLED_AMENDMENT_REFREEZE

    BF-U05-RDP02-IR-03
    = REMEDIATED / RE_REVIEW_PENDING

    RQ-U05-RDP02-IR-04
    = REMEDIATED / RE_REVIEW_PENDING

    BF-U05-RG-02
    = NOT_CLOSED
    = READY_POLICY_REFROZEN / CLOSURE_EVALUATION_PENDING

    U05-RDP-02 A1 bootstrap scope
    = REFROZEN / V1

    U05-RDP-02 READY-policy affected scope
    = REFROZEN / V1

READY-policy amendment targeted re-review 与显式 re-freeze 已完成；BF-U05-RG-02 仍需单独 closure evaluation，确认不存在其他 RDP-02 blocker 后才能 CLOSED。

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

---

## 19. A1 Controlled Amendment — Pre-D03 Bootstrap Admission

> Authorization: `AUTH-U05-A1-FROZEN-AMEND-001`  
> Reviewed design source: PR #138 exact head `7a62cc6f3b0cd9d803590594394bbed433351fab`  
> Status: **A1 REFROZEN / V1**

### 19.1 A1 bootstrap is pre-D03 routing/admission, not D03 status

When A1 bootstrap F3 is incomplete or not current：

```text
PRE_READINESS_A1_F3_C03_ELIGIBLE
or POST_F3_SAFETY_REVALIDATION_BARRIER
or F3_CURRENT_VERSION_REVALIDATION
```

remains active.

Therefore：

```text
U05 ordinary readiness evaluation
= not eligible

D03
= not invoked

D03 decision object
= absent

D03 decision_status
= absent
```

禁止新增：

```text
NOT_ADMITTED
NOT_REACHED
```

作为 D03 runtime vocabulary。

D03 runtime status 仍仅允许：

```text
DECIDED
INPUT_FAILURE
INPUT_CONFLICT
```

### 19.2 After A1 completion

只有：

```text
current U04 Gate
+ current F3 readiness input
+ other applicable/current RDP-05 inputs
```

通过 U05 inbound admission 后，D03 才正常执行。

### 19.3 D03-POL-005 Owner-approved first-entry policy

A1 bootstrap 解决“谁合法产生 current F3 input”。

Owner decision：

```text
OD-U05-READY-01
= APPROVE
```

在 U05 admission 已通过、且：

```text
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_ONLY
F1 = FRAMED_IN_SCOPE
F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
F5 = NOT_YET_APPLICABLE
F6 = NOT_YET_APPLICABLE
no higher-priority blocker
```

时：

```text
D03-POL-005
→ DECIDED / READY_FOR_CLINICAL_ANALYSIS
```

当前分类：

```text
FROZEN_EXECUTABLE_EXPECTATION
```

Post-DDx / F5-present profile 不得进入本规则。

### 19.4 POLICY_EXPECTATION_GAP boundary

```text
POLICY_EXPECTATION_GAP
```

继续仅作为 design/readiness verification sentinel。

它不是：

```text
patient business result
Capability failure
D03 runtime status
A1 bootstrap runtime outcome
```

### 19.5 Current amendment status

```text
U05-RDP-02 A1 bootstrap affected scope
= REFROZEN / V1

U05-RDP-02 READY-policy affected scope
= REFROZEN / V1

BF-U05-RG-02
= NOT_CLOSED
= READY_POLICY_REFROZEN / CLOSURE_EVALUATION_PENDING

OD-U05-READY-01
= APPROVE

READY-policy re-freeze
= GRANTED / COMPLETE

U05 Implementation Authorization
= NOT_GRANTED
```


---

## 20. OD-U05-READY-01 Narrow Owner-Policy Amendment

> Authorization source: Owner APPROVE recorded against PR #145 exact head `9adab885db20e902b9c705d566651acd1a625739`  
> Amendment ID: `AUTH-U05-READY-RDP02-AMEND-001`  
> Status: **APPLIED / REVIEW_PASS / REFROZEN**

Only the D03-POL-005 positive READY expectation is amended.

No change is made to：

```text
D03 owner
six-value readiness vocabulary
P0-P7 precedence order
D03-POL-001/002/003/004/006/007/008/009/010
A1 bootstrap architecture
RDP-05 input semantics
U04 Safety ownership
U05 implementation authorization
```

Current final classification after PASS + re-freeze：

```text
D03-POL-005
= FROZEN_EXECUTABLE_EXPECTATION
= FIRST_CLINICAL_ANALYSIS_ENTRY_ONLY
```


---

## 21. Post-DDx Routing Boundary Amendment

> Authorization: `AUTH-U05-PDX-FROZEN-AMEND-001`  
> Reviewed design source: PR #153 exact head `a5b8aa6e23e5f54a0c2e1884ed027d7f7b7cbeee`  
> Status: **REFROZEN / V1**

### 21.1 D03 remains unique Clinical Readiness Resolver

Post-DDx routing does not add new D03 values.

Frozen six-value Clinical Readiness remains：

```text
OUT_OF_SCOPE
NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
READY_FOR_CLINICAL_ANALYSIS
NO_RELIABLE_DIRECTION
```

### 21.2 Post-DDx profiles owned outside D03

For：

```text
evaluation_context = POST_DDX_REEVALUATION
F5 = ANALYSIS_RESULT_AVAILABLE
F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
no higher Clinical Readiness blocker
```

D03-POL-005 remains NOT_APPLICABLE.

Instead：

```text
U09 PostDdxRoutingDecision
→ TO_U12_DELIVERY_PREPARATION
```

For：

```text
F5 = REASSESSMENT_REQUIRED
F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
no higher Clinical Readiness blocker
```

the governed consequence is：

```text
U09 PostDdxRoutingDecision
→ TO_U08_REASSESSMENT
```

These are not Clinical Readiness results and do not create D03 decision objects.

### 21.3 When D03 still applies post-DDx

If current post-DDx owner outputs expose any actual Clinical Readiness path：

```text
OUT_OF_SCOPE
blocking NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
NO_RELIABLE_DIRECTION
```

then：

```text
U09
→ TO_U05_CLINICAL_READINESS
→ D03 frozen precedence
```

### 21.4 POLICY_EXPECTATION_GAP boundary

The known CL-01 profiles are no longer D03 expectation gaps once this amendment is independently reviewed/re-frozen, because they are explicitly classified as non-D03 post-DDx routing consequences.

Unknown admitted D03 profiles may still trigger POLICY_EXPECTATION_GAP and remain readiness blockers.

### 21.5 Current status

```text
U05-RDP-02 post-DDx boundary scope
= REFROZEN / V1

BF-U05-RG02-CL-01
= REMEDIATED / REFROZEN / CLOSURE_REEVALUATION_PENDING

BF-U05-RG-02
= NOT_CLOSED
```


---

## 22. Post-Analysis Routing Boundary Extension

> Authorization: `AUTH-U05-PA-FROZEN-AMEND-001`  
> Reviewed design source: PR #155 exact head `7e2d4d4255d51a10f58c63dec4e2ccb53920c33f`  
> Status: **REFROZEN / V1**

### 22.1 D03 scope remains unchanged

D03 继续只拥有 Clinical Readiness。

PostAnalysisRoutingDecision 不新增或重定义任何 D03 value。

### 22.2 POST_OFFLINE_ASSESSMENT non-D03 consequences

以下合法 profile 不要求伪造 D03 result：

```text
F6 = NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ F5 = ANALYSIS_RESULT_AVAILABLE
+ current F3 = PRESENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher Clinical Readiness path
→ TO_U12_DELIVERY_PREPARATION
```

以及：

```text
F6 = NO_BLOCKING_OFFLINE_EVIDENCE_NEED
+ F5 = REASSESSMENT_REQUIRED
+ no higher Clinical Readiness path
→ TO_U08_REASSESSMENT
```

若 current F3 仅为：

```text
ABSENT_BY_DESIGN
```

且当前 consequence 需要 F3 no-gap evidence，则：

```text
TO_F3_CURRENT_VERSION_REVALIDATION
```

不是 D03 business result。

### 22.3 Clinical Readiness paths still go to D03

若 current inputs 形成：

```text
OUT_OF_SCOPE
blocking NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
NO_RELIABLE_DIRECTION
```

则：

```text
TO_U05_CLINICAL_READINESS
→ D03
```

### 22.4 Closure blocker status

```text
BF-U05-RG02-CL-01
= REMEDIATED / REFROZEN / CLOSURE_REEVALUATION_PENDING

BF-U05-RG02-CL-02
= REMEDIATED / REFROZEN / CLOSURE_REEVALUATION_PENDING

BF-U05-RG-02
= NOT_CLOSED
```

### 22.5 Current status

```text
U05-RDP-02 post-analysis boundary scope
= REFROZEN / V1
```


---

## 23. Clinical Continuation Routing Boundary

> Authorization: `AUTH-U05-CCR-FROZEN-AMEND-001`  
> Reviewed design source: PR #157 exact head `4c3c7eb7e9aa9b6f9506871f7d28e033b4a6482e`  
> Status: **REFROZEN / V1**

### 23.1 POST_USER_FACT_UPDATE pre-D03 boundary

正常 upstream mutation 导致的：

```text
F3/F5/F6 STALE / INVALIDATED
```

若绑定 current accepted mutation / invalidation provenance，则统一分类为：

```text
STALE_BY_UPSTREAM_MUTATION
```

并保持：

```text
STALE_BY_UPSTREAM_MUTATION
!= FAILED
!= UNAVAILABLE
```

在 continuation sequencing 尚未完成时，不应被提前送入 D03。

因此：

```text
mutation-stale expected recomputation
→ ClinicalContinuationRoutingDecision
→ not D03 INPUT_FAILURE
```

### 23.2 When D03 becomes eligible

只有 current continuation consequence 为：

```text
TO_U05_CLINICAL_READINESS
```

且 RDP-05 currentness/admission 条件满足后，D03 才执行。

### 23.3 F5 stale is not a new Clinical Readiness value

```text
prior F5 activated
+ current F5 STALE/INVALIDATED by current fact mutation
+ current F3 no-gap
+ no higher path
→ TO_U08_REASSESSMENT
```

该结果：

```text
!= READY_FOR_CLINICAL_ANALYSIS
!= INPUT_FAILURE
!= new Clinical Readiness enum
```

### 23.4 True failure remains failure

```text
FAILED / UNAVAILABLE
```

不得因为 continuation router 存在而自动变成 reassessment。

### 23.5 Blocker status

```text
BF-U05-RG02-CL-03
= REMEDIATED / REFROZEN / CLOSURE_REEVALUATION_PENDING

BF-U05-RG-02
= NOT_CLOSED
```

### 23.6 Current status

```text
U05-RDP-02 continuation boundary
= REFROZEN / V1
```


---

# CL-04 Controlled Amendment — F6 Mutation-Stale Pre-D03 Boundary + D03-POL-011

> Authorization: `AUTH-U05-CL04-FROZEN-AMEND-001`  
> Reviewed design: PR #160 exact head `80cd6d7d154aa3e8de093ef43328e8ee9c2733d3`  
> Owner decision: `OD-U05-READY-02 = APPROVE_OPTION_A`  
> Amendment status: **APPLIED / INDEPENDENT_AMENDMENT_REVIEW_PENDING**  
> Re-freeze status: **NOT_YET_REFROZEN**

## 23. F6 mutation-stale pre-D03 boundary

For:

```text
POST_USER_FACT_UPDATE
+ prior F6 legally activated
+ F6 = STALE_BY_UPSTREAM_MUTATION
+ F6 is required for the current continuation basis
```

freeze:

```text
U05/D03 = NOT ELIGIBLE
no D03 decision object
no Clinical Readiness commit
```

The expected action is owner recomputation/revalidation before D03.

```text
F6 mutation-stale expected recomputation
= PRE-D03 NON-ENTRY
!= INPUT_FAILURE
!= INPUT_CONFLICT
```

If actual reassessment/revalidation execution fails, normal governed failure semantics apply; failure must not be rewritten as `NOT_NEEDED`.

## 24. D03-POL-011 — Owner-approved positive READY subrule

Owner decision:

```text
OD-U05-READY-02
= APPROVE_OPTION_A
```

Add:

```text
D03-POL-011
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_AFTER_CURRENT_F6_NOT_NEEDED
```

Exact positive guards:

```text
first-entry eligibility satisfied

evaluation_context in:
  A1_POST_BARRIER_CURRENT
  POST_USER_FACT_UPDATE
  POST_OFFLINE_ASSESSMENT

F1 = PRESENT / FRAMED_IN_SCOPE

F3 = PRESENT / CURRENT
F3 business signal = NO_ACTIVE_ONLINE_BLOCKING_GAP

authoritative F5 readiness applicability input:
  applicability_status = NOT_YET_APPLICABLE
  = F5 has never lawfully activated in this Consultation path

F6 = PRESENT / CURRENT
F6 Assessment = VALID
F6 business signal = NO_BLOCKING_OFFLINE_EVIDENCE_NEED

no qualified blocking offline signal
no lawful NEEDS_CLARIFICATION
no OUT_OF_SCOPE
no CAN_ASK_MORE
no required input failure / stale / unavailable
all required provenance/currentness/admission guards pass

-> DECIDED / READY_FOR_CLINICAL_ANALYSIS
```

Meaning remains narrow:

```text
READY_FOR_CLINICAL_ANALYSIS
= permission to enter governed U08/F5 first clinical analysis only
```

It does not mean diagnosis, safety, completion, delivery, or proof that no future gap exists.

## 25. Explicit non-applicability

D03-POL-011 is NOT_APPLICABLE when:

```text
POST_DDX_REEVALUATION
any provenance proves prior/current F5 activation
F5 PRESENT / STALE / INVALIDATED / FAILED / UNAVAILABLE
F6 STALE / FAILED / UNAVAILABLE
F6 has current justified blocking offline need
F3 is not current
F3 = CAN_ASK_MORE
OUT_OF_SCOPE
NEEDS_CLARIFICATION
Safety/admission does not permit current first-analysis entry
```

Missing/null F5 artifact alone does not establish `NOT_YET_APPLICABLE`.

## 26. P6 positive READY layer

Preserve the existing P0-P7 precedence structure.

```text
P2 OUT_OF_SCOPE
P3 NEEDS_OFFLINE_EVIDENCE
P4 NEEDS_CLARIFICATION
P5 CAN_ASK_MORE
P6 positive READY layer:
   D03-POL-005
   D03-POL-011
P7 NO_RELIABLE_DIRECTION
```

The two P6 subrules are mutually exclusive:

```text
D03-POL-005
-> F6 applicability = NOT_YET_APPLICABLE

D03-POL-011
-> F6 = PRESENT / CURRENT / VALID
   / NO_BLOCKING_OFFLINE_EVIDENCE_NEED
```

No internal 005-vs-011 ordering may change the result.

Preserve unchanged:

```text
D03-POL-005 exact existing semantics
D03-POL-006 exact existing semantics
six-value Clinical Readiness vocabulary
D03 unique Clinical Readiness ownership
```

This amendment is not yet re-frozen and does not authorize implementation.
