# U03 Clinical Risk Semantics

> 文档角色：U03 Clinical Input Package — A / Clinical Risk Semantics  
> 当前状态：`STRUCTURAL_SEMANTICS_FROZEN / MEDICAL_CONTENT_PENDING / NOT_APPROVED`  
> 基线：`impl/u03-current-version-risk-assessment@b6913433b72a7156855db048f524efdd5175abd6`  
> 本文件只冻结 U03 风险语义边界、状态语义、Owner、版本/currentness 与失败闭合；不定义任何具体医学阈值、疾病规则、红旗清单或生产风险等级映射。

---

## 1. 目的

U03 的真实临床能力必须先回答“系统里的 Risk 到底是什么意思”，再回答“哪些医学内容属于 Risk”。

本文件负责冻结前者：

```text
Clinical State
↓
Risk Evidence Candidate
↓
Evidence Acceptance
↓
D09 Deterministic Clinical Risk Disposition
↓
Risk Assessment
```

后续 Evidence Catalog、Safety-critical Risk Rule Pack、D09 Policy Table、Risk EvalSet 必须遵守本文语义，不能反向改变这些边界。

---

## 2. 权威边界与禁止事项

已有冻结设计明确要求：

```text
Capability Result != Clinical Truth
Risk Evidence != Clinical Risk Disposition
Candidate != Decision != Proposal != Commit
Risk Capability != Clinical Risk Disposition Owner
Risk Capability != Safety Gate Owner
Runtime State != Clinical State
Trace != Clinical State
```

因此：

- C02 只能产生 Risk Evidence / Candidate；
- D09 是正式 Clinical Risk Disposition 的唯一确定性 Owner；
- P01 / G2 负责正式状态提交；
- U04 才拥有 Safety Gate；
- Trace 只能记录发生了什么，不能成为风险真值；
- 任何模型输出、检索结果、知识库结果、规则命中本身都不能直接等同正式 Risk Assessment。

---

## 3. Risk Evidence 的结构定义

### 3.1 Risk Evidence 是什么

Risk Evidence 是：

> 基于某个明确 `clinical_state_version`，由受治理能力、规则或已发布知识识别出的、可能影响风险裁决的结构化证据对象。

它至少必须能够追溯：

```text
clinical_state_version
source fact / assertion refs
source type
capability binding/version（适用时）
rule release ref（适用时）
knowledge release ref（适用时）
confidence / uncertainty
limitations
provenance
```

Risk Evidence 不是：

- 最终风险等级；
- Safety Gate 结论；
- “患者安全”声明；
- 无版本来源的自然语言判断；
- 未经发布的知识或 Prompt 结论；
- 仅因为没有命中规则而自动生成的“低风险”。

### 3.2 Risk Evidence 的候选分类

当前结构层允许以下 taxonomy：

```text
RED_FLAG
MUST_NOT_MISS
VITAL_SIGN_SAFETY_SIGNAL
RISK_FACTOR
COMBINATION_SIGNAL
```

这些名称只冻结类别边界，不冻结具体医学内容。

最终 category 是否保留、拆分或补充，必须由医学 Owner 审核；在医学审核前不得据此生成 production rule content。

---

## 4. 五类 Evidence 的结构语义

### 4.1 RED_FLAG

结构定义：

> 一类安全关键证据；一旦满足经过审核的正式定义，必须被风险裁决链显式考虑，不能被普通缺失信息、模型置信度或“未发现其他异常”静默覆盖。

当前只冻结：

```text
red flag evidence ≠ final HIGH_RISK disposition
red flag evidence ≠ Safety Gate decision
```

具体哪些症状、体征、组合、时序或阈值构成 Red Flag：

```text
TBD_BY_MEDICAL_OWNER
```

### 4.2 MUST_NOT_MISS

结构定义：

> 一类因潜在后果或安全要求而必须在后续风险/鉴别流程中被显式保留和处理的安全关键信号。

必须保持：

```text
MUST_NOT_MISS evidence
!= confirmed diagnosis
!= final HIGH_RISK disposition
```

它与 RED_FLAG 的包含/交叉/优先级关系：

```text
TBD_BY_MEDICAL_OWNER
```

### 4.3 VITAL_SIGN_SAFETY_SIGNAL

结构定义：

> 来源于受治理生命体征或等价客观测量数据、可影响风险裁决的安全信号。

必须保持：

```text
UNMEASURED != NORMAL
UNKNOWN != NORMAL
missing measurement != reassuring measurement
```

哪些生命体征字段、单位、年龄/人群范围和阈值进入该类别：

```text
TBD_BY_MEDICAL_OWNER
```

### 4.4 RISK_FACTOR

结构定义：

> 可改变风险解释、优先级或后续处理要求，但其存在本身不必然等同 Red Flag 或最终 Risk Disposition 的因素。

必须保持：

```text
risk factor present
!= HIGH_RISK automatically

risk factor absent / unknown
!= NO_HIGH_RISK_SIGNAL automatically
```

具体 Risk Factor Catalog 与人口学/病史/暴露等医学定义：

```text
TBD_BY_MEDICAL_OWNER
```

### 4.5 COMBINATION_SIGNAL

结构定义：

> 只有在多个正式输入或证据满足受治理组合规则时才能形成的派生风险信号。

必须绑定：

```text
input evidence refs
rule_release_ref
clinical_state_version
provenance
```

不得：

```text
模型自由推断组合关系
→ 未经 Rule Release
→ 直接成为正式风险依据
```

具体组合规则：

```text
TBD_BY_MEDICAL_OWNER
```

---

## 5. Clinical Fact value 与 Risk 解释

U03 消费的 Clinical State 继续沿用已冻结 Fact value：

```text
YES
NO
UNKNOWN
UNMEASURED
NOT_ASKED
NOT_APPLICABLE
```

这些值必须保持语义独立。

### 5.1 YES

表示当前 governed Clinical State 中对应事实被正式表示为存在/肯定。

它是否足以形成某类 Risk Evidence，仍由 Evidence Definition / Rule Pack 决定。

### 5.2 NO

表示对应事实被正式表示为否定。

必须保持：

```text
NO != UNKNOWN
NO != UNMEASURED
NO != NOT_ASKED
```

### 5.3 UNKNOWN

表示当前无法确认 YES 或 NO。

强制不变量：

```text
UNKNOWN != NO
UNKNOWN != NEGATIVE
UNKNOWN != NORMAL
UNKNOWN != reassuring evidence
```

UNKNOWN 是否允许某条 D09 policy 继续裁决，由该 policy 的显式 prerequisite 决定；不得全局默认“按否定处理”。

### 5.4 UNMEASURED

表示该项需要测量语义，但当前没有有效测量结果。

强制不变量：

```text
UNMEASURED != NORMAL
UNMEASURED != NO
UNMEASURED != UNKNOWN
```

若某条风险规则要求某个测量值，而当前状态为 UNMEASURED，则只能：

```text
按照正式 policy 处理为信息不足 / caution / failed / downstream gap
```

具体处理不能由开发默认。

### 5.5 NOT_ASKED

表示当前尚未采集该事实。

必须保持：

```text
NOT_ASKED != NO
NOT_ASKED != UNKNOWN
```

它可以驱动后续 Gap / Question，但不能作为 Risk-negative evidence。

### 5.6 NOT_APPLICABLE

表示经正式语义判断，该事实在当前 Subject / Scope 下不适用。

必须有适用性依据；不得把“没问到 / 不知道 / 没测”映射为 NOT_APPLICABLE。

---

## 6. Observation lifecycle 与 Risk 解释

Observation lifecycle 与 Fact value 分离：

```text
EXTRACTED
NORMALIZED
CONFIRMED
UNCERTAIN
CONTRADICTED
INVALIDATED
```

### 6.1 CONFIRMED

可进入 Evidence Acceptance，但仍必须满足对应 Evidence Definition 的 source / version / scope 要求。

### 6.2 UNCERTAIN

不等同于否定。

是否可以进入 D09：

```text
必须由 Evidence Definition / D09 Policy 显式规定
```

不能由工程层统一提升为肯定证据，也不能统一丢弃。

### 6.3 CONTRADICTED

表示存在正式冲突关系。

必须保持：

```text
CONTRADICTED != resolved NO
CONTRADICTED != resolved YES
```

如果关键安全证据处于 CONTRADICTED，而没有受治理的冲突解决结果：

```text
不得静默进入 NO_HIGH_RISK_SIGNAL
```

其最终处理分支由 D09 policy 决定。

### 6.4 INVALIDATED

不得作为 current Risk Assessment 的有效输入。

任何依赖已 INVALIDATED 上游事实形成的旧 Risk Evidence / Risk Assessment 必须变为 non-current / stale / invalidated，具体生命周期沿 D05 与 U03 version binding 执行。

---

## 7. AMBIGUOUS 与 CONFLICTING 语义

### 7.1 AMBIGUOUS

AMBIGUOUS 表示：

> 当前输入可以支持多个互斥或关键不同的临床解释，且尚未被正式消歧。

必须保持：

```text
AMBIGUOUS != NO
AMBIGUOUS != NEGATIVE
AMBIGUOUS != SAFE
```

安全关键 Evidence 若依赖尚未解决的歧义，是否能继续形成 provisional candidate 由 Evidence Catalog 定义；是否允许 D09 形成 VALID disposition 必须由 D09 policy 显式规定。

### 7.2 CONFLICTING

CONFLICTING 表示：

> 两个或多个 current / potentially-current 输入在同一风险语义上存在不兼容证据。

必须保留：

```text
conflicting refs
source/provenance
version
```

禁止按“最后一条输入覆盖”或“模型置信度较高者自动胜出”解决安全关键冲突，除非该机制本身属于已批准 deterministic policy。

---

## 8. Source Type 与证据强度边界

已冻结来源至少包括：

```text
PATIENT_REPORTED
EXTERNAL_MEASUREMENT
OCR_EXTRACTED
MODEL_INFERRED
RULE_DERIVED
CLINICIAN_CONFIRMED
```

必须保持：

```text
MODEL_INFERRED != PATIENT_REPORTED
RULE_DERIVED != EXTERNAL_MEASUREMENT
Patient Fact != Derived Clinical Assertion
```

本文不定义不同 source 的医学证据等级。

以下内容必须由后续 Evidence Catalog / Medical Owner 冻结：

```text
哪些 evidence 接受哪些 source types
是否需要多源确认
哪些 source 只能作为候选而不能独立触发安全关键规则
冲突 source 的 precedence
```

---

## 9. Risk Assessment 正式状态语义

### 9.1 Assessment status

U03 正式状态先区分：

```text
VALID
FAILED
```

并保留上游生命周期：

```text
NOT_ASSESSED
STALE
```

### 9.2 VALID

VALID 只表示：

> 当前 Clinical State Version 上，C02 evidence、required release、Evidence Acceptance 与 D09 deterministic policy 均成功完成，形成一个可提交的正式 Risk Disposition。

VALID 不表示“患者安全”。

### 9.3 FAILED

FAILED 表示：

> 当前风险评估链未能在治理要求下产生可接受的正式风险裁决。

可能来源包括但不限于结构性类别：

```text
capability failure
invalid output
rule release unavailable
knowledge release unavailable
binding mismatch
stale clinical state version
required evidence unavailable / unresolved according to policy
safety-blocking governance failure
```

具体 failure taxonomy 后续可以扩充，但必须保持：

```text
FAILED != NO_HIGH_RISK_SIGNAL
FAILED != LOW_RISK
FAILED != SAFE
```

FAILED 必须被正式记录，并作为后续 U04 Safety Gate 的安全输入。

---

## 10. Clinical Risk Disposition 语义

当且仅当：

```text
Risk Assessment status = VALID
```

D09 才允许产生且只能产生一个冻结 disposition：

```text
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
```

### 10.1 NO_HIGH_RISK_SIGNAL

结构语义：

> 在当前 Clinical State Version、当前受治理 Evidence/Rule/Knowledge Release 与 D09 policy 范围内，没有形成满足更高风险 disposition 条件的正式风险信号。

必须保持：

```text
NO_HIGH_RISK_SIGNAL != SAFE
NO_HIGH_RISK_SIGNAL != no disease
NO_HIGH_RISK_SIGNAL != no need for follow-up
NO_HIGH_RISK_SIGNAL != all unknowns are negative
```

### 10.2 CAUTION

结构语义：

> 当前存在需要提高谨慎程度、进一步澄清/观察/后续处理的正式风险信号，但其具体医学条件和后续动作由受治理 D09/U04 policy 决定。

本文不定义任何具体触发阈值或处置建议。

### 10.3 HIGH_RISK

结构语义：

> 当前存在满足受治理 D09 policy 的高风险条件，需要下游 Safety Gate 以高风险输入处理。

必须保持：

```text
HIGH_RISK disposition
!= diagnosis
!= treatment order
!= final patient-facing instruction by itself
```

### 10.4 不允许 LOW_RISK

当前冻结词表不包含 `LOW_RISK`。

原因：现有 V1 设计明确使用：

```text
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
```

不得自行引入 `LOW_RISK` 并与 `NO_HIGH_RISK_SIGNAL` 混用。

---

## 11. Currentness / Version Semantics

每个 Risk Evidence / Risk Assessment 必须绑定：

```text
consultation_id
clinical_state_version
capability binding/version
rule release ref（适用时）
knowledge release ref（适用时）
contract version
```

强制不变量：

```text
Risk(v3) evaluated from ClinicalState(v3)
!= current Risk for ClinicalState(v4)
```

当上游 Clinical State Version 发生会影响 Risk 的变化：

```text
existing Risk Assessment
→ STALE / non-current
→ must be re-evaluated
```

旧 Risk 不得因为仍存在于数据库中就继续作为当前 U04 输入。

---

## 12. Evidence Acceptance 最低语义

Evidence Acceptance 不是医学裁决，而是进入 D09 前的治理门。

VALID candidate 至少要求：

```text
exact clinical_state_version match
capability binding/version match
rule release match（若使用）
knowledge release match（若使用）
required provenance present
candidate status not structurally invalid
```

以下情况必须 fail closed，而不能“尽量继续”：

```text
stale version
unauthorized capability binding
inactive / mismatched required rule release
inactive / mismatched required knowledge release
structurally invalid candidate
missing required provenance for safety-critical evidence
```

是否因临床证据“不足 / 歧义 / 冲突”进入 FAILED、CAUTION 或其他合法分支，由 D09 Clinical Policy 决定，不由 Evidence Acceptance 擅自定义。

---

## 13. D09 Owner 语义

D09 是唯一正式 Clinical Risk Disposition Owner。

D09 输入只能是：

```text
accepted risk evidence
+
current clinical_state_version
+
active/effective rule release
+
active/effective knowledge release（若需要）
+
policy release
```

D09 输出必须包含：

```text
decision id
assessment status
disposition（VALID 时）
reason code
accepted evidence refs
clinical_state_version
rule / knowledge / capability refs
provenance
```

D09 不拥有：

```text
Safety Gate
Clinical State commit
Runtime routing
patient-facing wording
```

---

## 14. 与 U04 的边界

U03 的终点是正式 Risk Assessment。

```text
U03
→ VALID + NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK
or FAILED
```

U04 才负责回答：

```text
基于正式 Risk Assessment
以及其他 Safety Gate 所需输入
当前是否允许继续后续临床流程
```

因此必须保持：

```text
NO_HIGH_RISK_SIGNAL != Safety Gate PASS
HIGH_RISK != U03 自己决定患者交付动作
FAILED != U03 自己并发跳转 U14
```

---

## 15. 本文件已冻结与未冻结内容

### 15.1 已冻结

```text
Risk Evidence 与 Risk Disposition 分层
五类候选 Evidence taxonomy 的结构语义
Fact value 与 Risk 的非等价关系
UNKNOWN / UNMEASURED / NOT_ASKED / NOT_APPLICABLE 的安全语义
AMBIGUOUS / CONFLICTING 的非否定语义
VALID / FAILED 区分
NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK 冻结词表
NO_HIGH_RISK_SIGNAL != SAFE
FAILED != NO_HIGH_RISK_SIGNAL
currentness/version binding
D09 唯一 Owner
U03 != U04 Safety Gate
```

### 15.2 未冻结，必须由医学 Owner 提供

```text
具体 Red Flag 定义
具体 Must-not-miss 定义
具体生命体征字段与阈值
具体 Risk Factor Catalog
具体 Combination Rules
不同 source type 的医学证据强度
关键歧义/冲突的医学解决规则
哪些证据不足必须 FAILED / CAUTION
D09 具体 branch / priority / precedence
人群/年龄/地区/场景差异
具体 source references / guideline provenance
```

---

## 16. CD-01 当前状态

本文完成后，CD-01 不再是 `MISSING`，但也不能宣称 `APPROVED`。

当前准确状态：

```text
CD-01 Clinical Risk Semantics
= STRUCTURAL_SEMANTICS_FROZEN
/ MEDICAL_CONTENT_PENDING
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ NOT_APPROVED
```

进入 `APPROVED` 至少还需要医学 Owner 审核：

1. Evidence taxonomy 是否充分且无错误边界；
2. Red Flag / Must-not-miss 的关系定义；
3. ambiguous / conflicting / insufficient evidence 的临床处理原则；
4. source type 的可接受性原则；
5. U03 风险词表是否满足目标产品临床范围；
6. scope / population / region 差异是否需要额外语义。

在该审核完成前：

```text
不得进入真实 Rule Pack 内容实现
不得声明 CD-01 APPROVED
不得解除 CD-07 blocker
```
