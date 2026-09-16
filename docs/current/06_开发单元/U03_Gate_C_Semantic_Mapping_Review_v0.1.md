# U03 Gate C Semantic Mapping Review v0.1

> 对象：Implementation Step 1 / 冻结语义 → 可执行规则映射。  
> 审核角色：`U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 对照基线：`c141462` / PR #88 HEAD  
> 状态：`REVIEW_COMPLETE / NO_BLOCKING_SEMANTIC_GAP / IMPLEMENTATION_BINDING_DEFINED / NOT_EXECUTED / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 授权：`AUTH-U03-GATEC-EVAL-IMPL-001` 仅覆盖 evaluation-only isolated evaluator。

本文件把 15 条 C rule、Coverage 5+2、D09 P0–P5 写成实现可直接照抄的映射。实现者不得自行解释医学含义，也不得补写未冻结条件。

---

## 1. Verdict

```text
machine-executable C predicates = 15 / 15
Coverage 5+2 mapping = EXECUTABLE
D09 P0-P5 mapping = EXECUTABLE
blocking Governed Semantic Gap = 0
residual notes = GSG-R1..GSG-R4
Semantic Mapping Review = COMPLETE
Evaluation-only Implementation = AUTHORIZED_TO_START
impl/u03-gatec-eval-only = MAY_BE_CREATED
Governed Evaluation Execution = NOT_STARTED
Gate C = NOT_PASSED
```

没有规则只剩“医学含义、无法落成字段条件”而挡住当前 Gate C EvalSet。  
baseline 5 的 C 级 `SCOPE_MISMATCH` 散文，按 Coverage / D09 分层绑定，不需要开发者猜临床范围。

---

## 2. Frozen Authority

只允许消费：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_C_MISSINGNESS_V0_2
U03_SEPSIS_SHARED_SCOPE_V0_2
U03_C_BF_C_04_Closure_Amendment_v0.2.md
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
ER-U03-RISK-001@0.1.0-candidate
FX-GC-001..FX-GC-031
SS-001..SS-020
```

禁止：

```text
发明新阈值 / 新 evidence / 新 disposition
把 accepted evidence 再解读成自然语言临床判断
由当前 pack 创建 suspected_sepsis 或 NHS dyspnoea context
输出 SAFE / NORMAL / 确诊病名
```

---

## 3. Machine Execution Order

实现必须按这个顺序，禁止按文件顺序或 first-hit 决定：

```text
1. bind exact release refs
2. evaluate whole-policy P0
3. if P0 hit: stop; do not construct denominator; do not run C
4. consume fixture-provided accepted_evidence / measurements / scope_context
5. evaluate 15 C rules independently
6. map C results → Coverage 5+2 states
7. apply D09 P1 > P2 > P3 > P4 > P5
8. compare expected / must_not_output / must_not_commit
```

C 不得输出 D09 vocabulary。D09 不得重算 C 阈值。

---

## 4. Whole-policy P0 Mapping

输入字段：

```text
clinical_state_version
knowledge_release_ref
rule_release_ref
policy_release_ref
coverage_contract_ref
policy_pair_ref
pregnancy_or_puerperium
region_scope
channel_scope
dependency_execution_status
accepted_evidence_binding_valid
```

| Condition | result_status | disposition | reason_code |
|---|---|---|---|
| `clinical_state_version != CURRENT` | FAILED | NONE | `STALE_INPUT` |
| any required release/pair/coverage ref missing, unresolvable, or not exact current set | FAILED | NONE | `RELEASE_MISMATCH` |
| accepted evidence binding invalid | FAILED | NONE | `INVALID_INPUT` |
| dependency execution failure | FAILED | NONE | `DEPENDENCY_FAILURE` |
| `pregnancy_or_puerperium = TRUE` | FAILED | NONE | `OVERALL_POLICY_SCOPE_MISMATCH` |
| `pregnancy_or_puerperium ∈ {UNKNOWN, NOT_ASKED, NOT_ESTABLISHED}` | FAILED | NONE | `OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` |
| `region_scope` 明确不属于 `INTERNATIONAL_REFERENCE_ONLY` | FAILED | NONE | `OVERALL_POLICY_SCOPE_MISMATCH` |
| `channel_scope` 明确不属于 source-supported remote/community | FAILED | NONE | `OVERALL_POLICY_SCOPE_MISMATCH` |

`pregnancy_or_puerperium = FALSE` 后才继续其余 overall-scope 检查，通过后才允许构造 Coverage denominator 并执行 C。

实现禁止把 `age UNKNOWN` 发明成新的 whole-policy P0 reason。当前 0.2.1 只把孕产缺失正式写成 P0。`age` 只进入 sepsis shared scope。见 GSG-R2。

---

## 5. Shared C Result Vocabulary

每条 C rule 只允许：

```text
MATCHED | NO_MATCH | INPUT_INSUFFICIENT | SCOPE_MISMATCH
```

不可判定态集合 `UNRESOLVED`：

```text
UNKNOWN
UNMEASURED
NOT_ASKED
AMBIGUOUS
CONFLICTING
REMOTE_NOT_OBSERVED
INVALID
NOT_ESTABLISHED
```

通式：

```text
scope 未满足且明确 outside
→ SCOPE_MISMATCH + RULE_SIGNAL_SCOPE_MISMATCH

scope / required evidence / required measurement ∈ UNRESOLVED
或 scope provenance independence 不可验证
→ INPUT_INSUFFICIENT + RULE_SIGNAL_INPUT_INSUFFICIENT

scope 满足 AND required inputs 可判定 AND predicate true
→ MATCHED + 本 rule matched_signal

scope 满足 AND required inputs 可判定 AND predicate false
→ NO_MATCH
  NO_MATCH 不是 rule signal，也不是低风险
```

`accepted_evidence(ID)` 只读 B 已接受状态。C 不再解释“像不像中风 / 是不是轻过敏”。

---

## 6. Fifteen C Rule Mappings

### 6.1 Baseline 5 — ALWAYS_APPLICABLE

P0 通过后，这 5 条始终进入 denominator。实现不得因医学散文再把它们标成 family `SCOPE_MISMATCH`。见 GSG-R1。

| Rule | Required field | MATCH | NO_MATCH | INPUT_INSUFFICIENT | C signal if MATCHED | D09 class |
|---|---|---|---|---|---|---|
| `C-RULE-RESP-001` | `accepted_evidence(EV-RF-RESP-001)` | `== PRESENT` | `== ABSENT` | field ∈ UNRESOLVED | `RULE_SIGNAL_CRITICAL_RED_FLAG` | P1 HIGH |
| `C-RULE-NEURO-001` | `accepted_evidence(EV-MNM-NEURO-001)` | `== PRESENT` | `== ABSENT` | field ∈ UNRESOLVED | `RULE_SIGNAL_MUST_NOT_MISS` | P1 HIGH |
| `C-RULE-NEURO-002` | `accepted_evidence(EV-MNM-NEURO-002)` | `== PRESENT` | `== ABSENT` | field ∈ UNRESOLVED | `RULE_SIGNAL_MUST_NOT_MISS` | P1 HIGH |
| `C-RULE-CARD-001` | `accepted_evidence(EV-MNM-CARD-001)` | `== PRESENT` | `== ABSENT` | field ∈ UNRESOLVED | `RULE_SIGNAL_MUST_NOT_MISS` | P1 HIGH |
| `C-RULE-ALLERGY-001` | `accepted_evidence(EV-RF-ALLERGY-001)` | `== PRESENT` | `== ABSENT` | field ∈ UNRESOLVED | `RULE_SIGNAL_CRITICAL_RED_FLAG` | P1 HIGH |

C 规则正文里的“慢性/双侧/非局灶”“轻型过敏”“不等于确诊 ACS”是 B / 禁止输出约束，不是额外 predicate。实现只消费 accepted evidence，并在 HIGH 路径禁止输出 `SAFE` / `NORMAL` / `CONFIRMED_ACS` / `CONFIRMED_MI`。

### 6.2 NHS dyspnoea family

共享 scope 字段：

```text
scope_context.nhs_dyspnoea_emergency_warning_context.status
scope_context.nhs_dyspnoea_emergency_warning_context.context_ref
scope_context.nhs_dyspnoea_emergency_warning_context.provenance_ref
scope_context.nhs_dyspnoea_emergency_warning_context.established_before_rule_pack_execution
```

Scope 先于 predicate：

```text
status == FALSE
→ 两条 rule 都 SCOPE_MISMATCH
→ family NOT_APPLICABLE

status ∈ {UNKNOWN, NOT_ESTABLISHED}
OR context_ref/provenance_ref 缺失
OR established_before_rule_pack_execution != TRUE
OR provenance 来自当前 pack evidence/result
→ 两条 rule 都 INPUT_INSUFFICIENT
→ family INSUFFICIENT_APPLICABLE

status == TRUE AND provenance independence VALID
→ 分别评估 appearance / confusion predicate
```

禁止用 `EV-RF-APPEAR-001` / `EV-RF-NEURO-001` 或本 family 结果创建该 context。

| Rule | Required field | MATCH | NO_MATCH | matched_signal | D09 class |
|---|---|---|---|---|---|
| `C-RULE-DYSPNOEA-APPEAR-001` | `accepted_evidence(EV-RF-APPEAR-001)` | `== PRESENT` | `== ABSENT` | `RULE_SIGNAL_CRITICAL_RED_FLAG` | P1 HIGH |
| `C-RULE-DYSPNOEA-CONFUSION-001` | `accepted_evidence(EV-RF-NEURO-001)` | `== PRESENT` | `== ABSENT` | `RULE_SIGNAL_CRITICAL_RED_FLAG` | P1 HIGH |

### 6.3 NG253 sepsis family

共享 scope：`U03_SEPSIS_SHARED_SCOPE_V0_2`

```text
age
pregnancy_recent_pregnancy
setting
scope_context.suspected_sepsis.status
scope_context.suspected_sepsis.context_ref
scope_context.suspected_sepsis.provenance_ref
scope_context.suspected_sepsis.established_before_rule_pack_execution
```

```text
age < 16
OR pregnancy_recent_pregnancy == TRUE
OR suspected_sepsis == FALSE
OR setting ∉ {SOURCE_SUPPORTED_COMMUNITY, SOURCE_SUPPORTED_CUSTODIAL}
→ 8 条全部 SCOPE_MISMATCH
→ family NOT_APPLICABLE

age UNKNOWN
OR pregnancy_recent_pregnancy UNKNOWN
OR setting UNKNOWN
OR suspected_sepsis ∈ {UNKNOWN, NOT_ESTABLISHED}
OR provenance independence 不可验证
→ 8 条全部 INPUT_INSUFFICIENT
→ family INSUFFICIENT_APPLICABLE

age >= 16
AND pregnancy_recent_pregnancy == FALSE
AND suspected_sepsis == TRUE
AND setting in allowed set
AND provenance independence VALID
→ 分别评估 8 条 predicate
```

禁止用 RR / SBP / HR / `EV-RF-APPEAR-001` / `EV-RF-SEPSIS-001` 或本 family 结果创建 `suspected_sepsis`。

| Rule | Field | MATCH | NO_MATCH | INSUFFICIENT extra | matched_signal | D09 class |
|---|---|---|---|---|---|---|
| `C-RULE-SEPSIS-RR-HIGH-001` | `respiratory_rate_bpm` + `EV-VS-SEPSIS-001` | `RR >= 25` | `RR` 已知且 `< 25` | RR ∈ UNRESOLVED | `RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION` | P1 HIGH |
| `C-RULE-SEPSIS-RR-MODHIGH-001` | same | `21 <= RR <= 24` | `RR` 已知且不在 21..24 | RR ∈ UNRESOLVED | `RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION` | P3 CAUTION |
| `C-RULE-SEPSIS-HR-HIGH-001` | `heart_rate_bpm` + `EV-VS-SEPSIS-003` | `HR > 130` | `HR` 已知且 `<= 130` | HR ∈ UNRESOLVED | `RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION` | P1 HIGH |
| `C-RULE-SEPSIS-HR-MODHIGH-001` | same | `91 <= HR <= 130` | `HR` 已知且不在 91..130 | HR ∈ UNRESOLVED | `RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION` | P3 CAUTION |
| `C-RULE-SEPSIS-APPEAR-HIGH-001` | `accepted_evidence(EV-RF-APPEAR-001)` | `== PRESENT` | `== ABSENT` | field ∈ UNRESOLVED | `RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION` | P1 HIGH |
| `C-RULE-SEPSIS-RASH-HIGH-001` | `accepted_evidence(EV-RF-SEPSIS-001)` | `== PRESENT` | `== ABSENT` | field ∈ UNRESOLVED | `RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION` | P1 HIGH |

`HR == 130`：HIGH = `NO_MATCH`，MODHIGH = `MATCHED`。不得发明孕期 100..130 分层。

#### SBP HIGH 双分支 — 必须按冻结顺序

字段：`systolic_bp_mmHg`、`usual_systolic_bp_mmHg`、provenance、`EV-VS-SEPSIS-002`。

```text
SBP ∈ UNRESOLVED
→ C-RULE-SEPSIS-SBP-HIGH-001 = INPUT_INSUFFICIENT

SBP 已知 AND SBP <= 90
→ HIGH = MATCHED
→ 不再要求 usual SBP

SBP 已知 AND SBP > 90
AND usual SBP known + provenance traceable
AND usual_systolic_bp_mmHg - systolic_bp_mmHg > 40
→ HIGH = MATCHED

SBP 已知 AND SBP > 90
AND usual SBP known + traceable
AND drop <= 40
→ HIGH = NO_MATCH

SBP 已知 AND SBP > 90
AND usual SBP ∈ UNRESOLVED 或无 provenance
→ HIGH relative-drop = INPUT_INSUFFICIENT
→ 不得写成 HIGH NO_MATCH
```

`C-RULE-SEPSIS-SBP-MODHIGH-001`：

```text
91 <= SBP <= 100 → MATCHED / MODERATE_HIGH
SBP 已知且不在 91..100 → NO_MATCH
SBP ∈ UNRESOLVED → INPUT_INSUFFICIENT
```

SBP 91..100 且 usual 未知时：MODHIGH 可 `MATCHED`，HIGH 同时可为 `INPUT_INSUFFICIENT`。两者允许并存；MODHIGH 不得抹掉 HIGH 的 insufficient。

生命体征 `expected_evidence_refs` 执行绑定时：`MEAS-RR/SBP/HR` → `EV-VS-SEPSIS-001/002/003`。

---

## 7. Coverage 5+2 Mapping

| Bucket | Members | C result | coverage_state |
|---|---|---|---|
| ALWAYS_APPLICABLE | RESP / NEURO-001 / NEURO-002 / CARD / ALLERGY | MATCHED / NO_MATCH | `APPLICABLE_EVALUATED` |
| ALWAYS_APPLICABLE | same | INPUT_INSUFFICIENT | `INSUFFICIENT_APPLICABLE` |
| `NHS_DYSPNOEA_FAMILY` | APPEAR / CONFUSION | MATCHED / NO_MATCH | `APPLICABLE_EVALUATED` |
| `NHS_DYSPNOEA_FAMILY` | same | INPUT_INSUFFICIENT | `INSUFFICIENT_APPLICABLE` |
| `NHS_DYSPNOEA_FAMILY` | same | SCOPE_MISMATCH | `NOT_APPLICABLE` |
| `NG253_SEPSIS_FAMILY` | 8 sepsis rules | MATCHED / NO_MATCH | `APPLICABLE_EVALUATED` |
| `NG253_SEPSIS_FAMILY` | same | INPUT_INSUFFICIENT | `INSUFFICIENT_APPLICABLE` |
| `NG253_SEPSIS_FAMILY` | same | SCOPE_MISMATCH | `NOT_APPLICABLE` |

```text
NOT_APPLICABLE 不计入 P2
INSUFFICIENT_APPLICABLE 计入 P2 并阻断 P3/P4
family 内 MATCHED + INPUT_INSUFFICIENT 仍记录 insufficiency；最终按 D09 优先级
OVERALL_POLICY_SCOPE_* 不进入 coverage denominator
```

同一条件 family 若同时出现互斥共享 scope 结果，例如 `SCOPE_MISMATCH + MATCHED`，不得任选一支，进入 P5。见 GC-026。

---

## 8. D09 P1–P5 Mapping

| Branch | When | result / disposition / reason |
|---|---|---|
| P1 `D09-P-010` | no P0 AND 任一条 applicable C 含 CRITICAL_RED_FLAG / MUST_NOT_MISS / SEPSIS_HIGH_RISK_CRITERION | `VALID / HIGH_RISK / HIGH_RISK_RULE_SIGNAL_PRESENT` |
| P2 `D09-P-020` | no P0 AND no P1 AND exists `INSUFFICIENT_APPLICABLE` | `FAILED / NONE / INSUFFICIENT_INFORMATION` |
| P3 `D09-P-030` | no P0/P1/P2 AND applicable C 含 SEPSIS_MODERATE_HIGH_RISK_CRITERION | `VALID / CAUTION / MODERATE_HIGH_RULE_SIGNAL_PRESENT` |
| P4 `D09-P-040` | no P0..P3 AND ALWAYS 全部 APPLICABLE_EVALUATED AND 每个条件 family 为 APPLICABLE_EVALUATED 或 NOT_APPLICABLE AND 无 insufficiency AND 分母内全部 NO_MATCH | `VALID / NO_HIGH_RISK_SIGNAL / COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL` |
| P5 `D09-P-090` | 优先级解析后无唯一合法 branch | `FAILED / NONE / UNRESOLVABLE_CONFLICT` |

Precedence：

```text
HIGH + CAUTION → HIGH_RISK
HIGH + INSUFFICIENT_APPLICABLE → HIGH_RISK；保留 insufficient_rule_refs[]
CAUTION + INSUFFICIENT_APPLICABLE → P2 FAILED
multiple HIGH → HIGH_RISK；保留全部 matched_rule_refs[]
NO_HIGH_RISK_SIGNAL != SAFE != NORMAL
FAILED != NO_HIGH_RISK_SIGNAL
D09 Decision != U04 Safety Gate Decision
```

P0 / P2 / P5 不得 commit clinical risk disposition。任何路径不得 `UNAUTHORIZED_DIRECT_CLINICAL_STATE_COMMIT`。

---

## 9. Governed Semantic Gaps

### Blocking

```text
blocking Governed Semantic Gap = 0
```

当前 Gate C 31 GC + 20 SS 不需要开发者补写未冻结医学条件。

### Residual — 实现必须遵守，不得发明

```text
GSG-R1
baseline 5 的 C 正文仍有“adult Gate-A / chest-pain-like / ABC allergy”散文。
绑定：P0 通过后这 5 条只做 missingness + accepted_evidence predicate。
不得再猜一套 C 级 SCOPE_MISMATCH 临床范围。

GSG-R2
0.2.1 没有把 age UNKNOWN 写成 whole-policy P0。
绑定：age 只进入 sepsis shared scope。
不得发明 OVERALL_POLICY_AGE_NOT_ESTABLISHED。

GSG-R3
C 不实现 B 叙事筛查（慢性双侧麻木、轻过敏等）。
只读 accepted_evidence。

GSG-R4
不实现 suspected_sepsis / NHS dyspnoea context 的上游创建算法。
只消费 fixture 已给出的 status + context_ref + provenance_ref。
```

这些 residual 不阻止 `impl/u03-gatec-eval-only`。它们是实现禁令，不是待补医学定义。

---

## 10. Implementation Binding Checklist

开始 isolated evaluator 前必须钉死：

```text
exact refs only; no latest alias
P0 before C
15 rules independent; no first-hit
SBP HIGH 双分支顺序不可改
family shared scope 一次判定，8 条 / 2 条共用
Coverage 只映射 C 结果，不重算临床
D09 不输出 SAFE/NORMAL
SS-001..SS-020 critical blocking
fixture-only; no production I/O
```

---

## 11. Current Status

```text
AUTH-U03-GATEC-EVAL-IMPL-001 = AUTHORIZED / EVALUATION_ONLY
Semantic Mapping Review = COMPLETE / NO_BLOCKING_GAP
Evaluation-only Implementation = NOT_STARTED / AUTHORIZED_TO_START
Independent Implementation Review = NOT_STARTED
Governed Evaluation Execution = NOT_STARTED
BF-CD06-EXEC-01 = OPEN
Gate C = NOT_PASSED
CD-07 Runtime Implementation Authorization = NOT_GRANTED
```

下一步允许建立 `impl/u03-gatec-eval-only`，实现 isolated evaluator + harness。  
实现后必须先独立 implementation review，再跑 `ER-U03-RISK-001@0.1.0-candidate`。  
本文件不是 Gate C PASS。
