# U03 D09 Clinical Policy Medical / Technical Review Record v0.1

> 审核对象：`U03_D09_Clinical_Policy_Content_Draft_v0.1.md`  
> Policy Release：`PR-U03-D09-001@0.1.0-draft`  
> Rule Release：`RR-U03-RISK-001@0.2.0-candidate`  
> Knowledge Release：`KR-U03-SOURCE-001@0.1.0-candidate`  
> 修订任务：`U03_D09_Clinical_Policy_Revision_Task_v0.1.md`  
> 审核角色：`U03_MEDICAL_OWNER_REVIEW` + `U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-15`  
> 对照基线：远程 `7b4d59f`（D09 draft + review template）  
> 状态：`REVIEW_COMPLETE / CONTENT_NOT_APPROVED / REVISION_REQUIRED / NOT_FROZEN / NOT_FOR_PRODUCTION`  
> 本记录只审核 D09 deterministic disposition policy，不重审 C predicate/threshold，不审核 U04。

---

## 1. Review Boundary

本轮只允许裁决：

```text
APPROVE
REVISE
REJECT
NEED_MORE_SOURCE
```

必须保持：

```text
Rule Hit != D09 Decision
FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
D09 Decision != U04 Safety Gate Decision
```

---

## 2. Branch Review Matrix

| Policy Branch | Medical | Technical | Review Focus |
|---|---|---|---|
| D09-P-001 Integrity / Release / Currentness Failure | APPROVE | REVISE | fail-closed 成立；但 §8 的整体 D scope mismatch 未进入本 branch 的 precondition / reason_code。 |
| D09-P-010 High-risk Rule Signal Present | APPROVE | APPROVE | 三类 HIGH-class C signal → HIGH_RISK 成立；HIGH + applicable insufficiency 不得降级。 |
| D09-P-020 Applicable Information Insufficient | APPROVE | APPROVE | 无 HIGH 时，实际适用 rule 的 insufficiency → FAILED / INSUFFICIENT_INFORMATION 成立。 |
| D09-P-030 Moderate/High Sepsis Criterion | APPROVE | APPROVE | 仅 `SEPSIS_MODERATE_HIGH` 且无 P0/P1/P2 时 → CAUTION 成立；不得发明其他 CAUTION 来源。 |
| D09-P-040 Fully Evaluated No Signal | APPROVE | REVISE | 医学上不得在未完整评估时输出 NO_HIGH_RISK_SIGNAL；“actually applicable governed rules/families” 尚不可执行。 |
| D09-P-090 No Unique Valid Decision | APPROVE | APPROVE | 无唯一合法分支时 FAILED / UNRESOLVABLE_CONFLICT 成立，禁止 LLM 补结论。 |

```text
Medical APPROVE = 6
Medical REVISE = 0
Technical APPROVE = 4
Technical REVISE = 2
blocking finding = 2
```

---

## 3. Cross-cutting Review Questions

| ID | Question | Medical | Technical |
|---|---|---|---|
| D-RV-01 | `P0 > P1 > P2 > P3 > P4 > P5` 是否合理 | APPROVE | APPROVE |
| D-RV-02 | HIGH-class 三类 C signal → `VALID + HIGH_RISK` 是否成立 | APPROVE | APPROVE |
| D-RV-03 | HIGH + applicable insufficiency 是否保持 HIGH_RISK 且保留 insufficiency trace | APPROVE | APPROVE |
| D-RV-04 | CAUTION + applicable insufficiency 是否应 fail closed，而不是输出 CAUTION | APPROVE | APPROVE |
| D-RV-05 | 专用 family `SCOPE_MISMATCH` 是否只表示 NOT_APPLICABLE，不单独制造整次 FAILED | APPROVE | APPROVE |
| D-RV-06 | 整体 D scope mismatch 是否应形成 FAILED / SCOPE_MISMATCH | APPROVE | REVISE |
| D-RV-07 | `NO_HIGH_RISK_SIGNAL` 是否仅允许在完整适用集合均充分评估且无 signal 时形成 | APPROVE | REVISE |
| D-RV-08 | P0 integrity failure 是否必须压过 clinical signal | APPROVE | APPROVE |
| D-RV-09 | Decision provenance 是否足以保留 matched / insufficient / not-applicable refs | APPROVE | APPROVE |
| D-RV-10 | 是否无 D09→U04 越权、无 C threshold 重算、无新 evidence taxonomy | APPROVE | APPROVE |

---

## 4. Focused Findings

### 4.1 六个 branch

六个 branch 的职责划分成立，不需要增删医学结论类型：

```text
D09-P-001  P0  integrity / release / currentness
         → FAILED / NONE

D09-P-010  P1  HIGH-class C signals
         → VALID / HIGH_RISK

D09-P-020  P2  applicable INPUT_INSUFFICIENT, no HIGH
         → FAILED / INSUFFICIENT_INFORMATION

D09-P-030  P3  only SEPSIS_MODERATE_HIGH
         → VALID / CAUTION

D09-P-040  P4  complete applicable set, all NO_MATCH
         → VALID / NO_HIGH_RISK_SIGNAL

D09-P-090  P5  no unique legal branch
         → FAILED / UNRESOLVABLE_CONFLICT
```

不另增第七个医学 disposition。整体 D scope 失败应并入 P0，而不是新的临床结论。

### 4.2 Precedence `P0 > P1 > P2 > P3 > P4 > P5`

医学与技术均接受该顺序：

```text
P0  输入/release 不可信时，禁止任何临床 disposition
P1  已存在的 HIGH-class 阳性不得被其他缺失信息降级
P2  无 HIGH 时，适用集合未评完必须 fail-closed
P3  CAUTION 只能在无 HIGH、无 applicable insufficiency 时形成
P4  NO_HIGH_RISK_SIGNAL 只能在 coverage-complete 后形成
P5  无唯一合法分支时守底失败
```

这同时关闭 RISK-D-01：P0 先于 P1，integrity/release 失败不会被 HIGH 掩盖。

### 4.3 `SCOPE_MISMATCH` 边界

专用 family 与整包 D scope 必须继续分开：

```text
specialized-family SCOPE_MISMATCH
= NOT_APPLICABLE for that family
!= whole D09 FAILED

suspected_sepsis == FALSE
或 NHS dyspnoea context == FALSE
→ 只排除对应 family
→ 不得把普通非 sepsis / 非 dyspnoea consultation 判整次 FAILED
```

D-RV-05 = APPROVE / APPROVE。

整包 D population/region/channel 不成立时，医学接受：

```text
FAILED / NONE / SCOPE_MISMATCH
```

但当前只写在第 8 节散文中：

```text
D09-P-001.reason_code ∈ {
  STALE_INPUT,
  RELEASE_MISMATCH,
  INVALID_INPUT,
  DEPENDENCY_FAILURE
}
```

`SCOPE_MISMATCH` 不是 P-001 的正式 precondition / reason_code，也容易与 C 的 `RULE_SIGNAL_SCOPE_MISMATCH` 混淆。这是 **BF-D-01**。

v0.2 必须把整包 D scope 失败并入 P0，并使用可与 family-level 信号区分的 reason_code，例如：

```text
OVERALL_POLICY_SCOPE_MISMATCH
```

### 4.4 `INPUT_INSUFFICIENT` 边界

以下映射成立，保持：

```text
applicable INPUT_INSUFFICIENT + no HIGH
→ P2 FAILED / INSUFFICIENT_INFORMATION

HIGH-class signal + applicable INPUT_INSUFFICIENT
→ P1 HIGH_RISK
→ retain insufficient_rule_refs
→ 不得降级

CAUTION-class signal + applicable INPUT_INSUFFICIENT
→ P2 FAILED
→ 不得输出 CAUTION

specialized-family SCOPE_MISMATCH
!= INPUT_INSUFFICIENT
→ 不单独触发 P2
```

基线 5 条（RESP / NEURO×2 / CARD / ALLERGY）在整包 D scope 成立时始终进入适用集合。其中任一条 `INPUT_INSUFFICIENT`（含 UNKNOWN / 未测 / 未问）且无 HIGH 时，整次 D09 必须 P2 失败。这是本 safety-critical slice 的 fail-closed，不是过严误伤。

专用 family 的 UNKNOWN / NOT_ESTABLISHED / provenance 不可验证，必须消费 C 已发出的 `RULE_SIGNAL_INPUT_INSUFFICIENT`，从而阻止 P3/P4。因此：

```text
suspected_sepsis UNKNOWN
或 nhs_dyspnoea context UNKNOWN
→ 不得形成 NO_HIGH_RISK_SIGNAL
```

v0.2 应删掉 §4.2 / §4.3 的“本应可判定”主观判断，只绑定 C 已冻结信号：

```text
C SCOPE_MISMATCH → NOT_APPLICABLE
C INPUT_INSUFFICIENT → INSUFFICIENT_APPLICABLE
```

该措辞修订并入 coverage contract，不单独构成第三条 blocker。

### 4.5 `NO_HIGH_RISK_SIGNAL` 边界

医学接受：

```text
NO_HIGH_RISK_SIGNAL
!= SAFE
!= NORMAL
!= no disease
!= no future deterioration
```

且只有 coverage-complete 且全 `NO_MATCH` 时才能输出。RISK-D-03 成立：P-040 的分母

```text
all actually applicable governed rules/families
required by this policy execution
```

对实现者仍可解释。这是 **BF-D-02**。

v0.2 必须把第 4 节固化为 P-040 / P-020 共同引用的 coverage contract：

```text
ALWAYS_APPLICABLE  when overall D scope holds:
  C-RULE-RESP-001
  C-RULE-NEURO-001
  C-RULE-NEURO-002
  C-RULE-CARD-001
  C-RULE-ALLERGY-001

CONDITIONALLY_APPLICABLE:
  NHS_DYSPNOEA_FAMILY
  NG253_SEPSIS_FAMILY
  APPLICABLE             iff C family/shared-scope result is in-scope MATCHED or NO_MATCH
  NOT_APPLICABLE         iff C emits RULE_SIGNAL_SCOPE_MISMATCH
  INSUFFICIENT_APPLICABLE iff C emits RULE_SIGNAL_INPUT_INSUFFICIENT
```

```text
P-020 denominator
= ALWAYS_APPLICABLE ∪ CONDITIONALLY_APPLICABLE.INSUFFICIENT_APPLICABLE

P-040 denominator
= ALWAYS_APPLICABLE ∪ CONDITIONALLY_APPLICABLE.APPLICABLE

NOT_APPLICABLE families
are excluded from both denominators
```

---

## 5. Explicit Review Risks

```text
RISK-D-01  CLOSED
P0 先于 P1；integrity/release 失败不会被 HIGH 掩盖
```

```text
RISK-D-02  OPEN → BF-D-01
专用 family SCOPE_MISMATCH 与整包 D scope mismatch
语义已区分，但整包路径尚未形式化进 P0
```

```text
RISK-D-03  OPEN → BF-D-02
NO_HIGH_RISK_SIGNAL completeness denominator
仍依赖“actually applicable”散文，必须固化 coverage contract
```

```text
RISK-D-04  CLOSED
当前 C candidate 没有其他 caution-class signal
D 未发明新的 CAUTION 来源
```

非阻塞备注：P-030 应与 P-010 一样保留全部 `matched_rule_refs`；不单独构成 blocker。

---

## 6. Blocking Findings

```text
BF-D-01
整体 D policy scope mismatch
必须并入 D09-P-001 / P0
并使用可与 C RULE_SIGNAL_SCOPE_MISMATCH 区分的 reason_code
不得只停留在第 8 节散文

BF-D-02
P-040 / P-020 必须引用冻结的 coverage contract
5 条 always-on + 2 个 conditional family
APPLICABLE / NOT_APPLICABLE / INSUFFICIENT_APPLICABLE
只允许由 C 已冻结信号判定
```

---

## 7. Review Completion Rule

只有同时满足：

```text
6 / 6 policy branches Medical = APPROVE
6 / 6 policy branches Technical = APPROVE
D-RV-01..10 = APPROVE / APPROVE
blocking finding = 0
```

才可以：

```text
D Content Approval = APPROVED_FOR_POLICY_CONTENT
```

本轮不满足。当前：

```text
D Content Approval = REVISE_REQUIRED
```

即使后续内容审核通过，也仍不自动意味着：

```text
D policy candidate frozen
CD-05 passed
Gate B passed
Gate C passed
Implementation Authorized
Production Authorized
```

---

## 8. Current Status

```text
D Content Draft v0.1 = REVIEWED
Medical Review = COMPLETE
Technical Review = COMPLETE
D Content Approval = REVISE_REQUIRED
blocking findings = BF-D-01, BF-D-02
D Policy Candidate Freeze = NOT_COMPLETE
CD-05 = NOT_PASSED
C/D/E Cross-Consistency = NOT_STARTED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
D Runtime Implementation = NOT_AUTHORIZED
```

下一步只允许按 `U03_D09_Clinical_Policy_Revision_Task_v0.1.md` 修订为 v0.2 后再审。
不冻结 D policy，不开始 D runtime，不宣称 Gate B/C PASS，不开始 CD-07。
