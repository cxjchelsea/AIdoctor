# U03 Clinical Dependency Assessment

> Unit：U03 当前版本风险评估  
> 阶段：Clinical Dependency Completion / Assessment  
> 基线：`impl/u03-current-version-risk-assessment@b6913433b72a7156855db048f524efdd5175abd6`  
> 本文件只盘点 U03 工程组件验证通过后仍缺失的临床依赖；不构成新的 Implementation Authorization、Merge Authorization、Clinical Runtime Enablement 或 Production Authorization。

---

## 1. 当前事实

U03 当前已完成的是工程治理链：

```text
Clinical State Version
→ governed C02 invocation boundary
→ candidate / evidence contract
→ evidence acceptance
→ D09 owner boundary
→ typed Risk proposal
→ P01 commit
→ P05 trace
```

当前仍未完成：

```text
real clinically governed C02 Risk Evidence Engine/content
Safety-critical medical Risk Rule Pack content
clinical Risk EvalSet / Safety Suite
production D09 clinical policy release
production release registration / activation
```

因此：

```text
U03 Engineering Governance Path = IMPLEMENTED / COMPONENT_VERIFIED
U03 Clinical Capability = NOT_COMPLETE / NOT_CLINICALLY_EVALUATED
U03 Business Loop = NOT_CLOSED
```

---

## 2. 缺口不是一个问题，而是四层依赖

### 2.1 医学定义层

必须先由医学 Owner 冻结的内容：

1. Risk Evidence Taxonomy：哪些输入属于可接受的风险证据类别；
2. Red Flag / Must-not-miss / Vital-sign Safety Signal / Risk Factor 的定义边界；
3. 特殊人群和适用范围；
4. UNKNOWN / UNMEASURED / CONFLICTING / AMBIGUOUS 的医学语义；
5. 哪些证据不足时必须 FAILED / NEEDS_MORE_EVIDENCE，而不能输出低风险语义；
6. D09 最终 disposition 的医学判定原则与优先级。

工程侧不得自行发明这些临床内容。

### 2.2 规则与知识资产层

必须形成版本化受治理资产：

```text
KnowledgeRelease
SafetyCriticalRiskRulePack
D09ClinicalPolicyRelease
```

每个 release 至少需要：

```text
release_id / version
status
applicable population / region / language / channel
source/evidence provenance
review owner
review status
effective_from / effective_to
supersedes / rollback reference
```

规则内容必须引用医学定义，不得把“代码常量”当作临床知识来源。

### 2.3 工程实现层

在医学定义和 release contract 冻结后，开发才能完成：

```text
C02 Risk Evidence Engine
Rule Pack loader/evaluator
Knowledge lookup adapter
D09 deterministic policy implementation
release binding integration
failure/fallback semantics
```

当前 U03 已提供这些实现的治理接口与不可绕过边界，但没有真实临床内容。

### 2.4 临床评估层

必须建立独立于实现代码的 Risk EvalSet / Safety Suite，至少覆盖：

```text
red-flag positive
must-not-miss positive
special population
vital-sign abnormal / unknown / unmeasured
multiple interacting risk factors
no high-risk evidence
ambiguous input
conflicting evidence
missing required evidence
capability failure
rule release unavailable
knowledge release unavailable
stale clinical-state version
version/release mismatch
```

核心指标不得只看 accuracy；安全优先指标至少需要：

```text
high-risk recall / miss rate
must-not-miss recall
false reassurance rate
FAILED incorrectly mapped to low-risk = 0
UNKNOWN/UNMEASURED incorrectly mapped to negative/normal = 0
release/version mismatch accepted = 0
```

具体阈值必须由后续医学/产品治理批准，本文件不自行设定。

---

## 3. Dependency Completion 工作包

### CD-01 — Clinical Risk Taxonomy & Semantics

Owner：医学 Owner  
输出：风险证据分类、状态词表、UNKNOWN/UNMEASURED/AMBIGUOUS/CONFLICTING 语义、适用范围。

### CD-02 — Safety-critical Evidence Definition

Owner：医学 Owner  
输出：Red Flag、Must-not-miss、Vital-sign Safety Signal、Risk Factor/Combination 的定义和证据要求。

### CD-03 — Safety-critical Rule Pack Specification

Owner：医学 Owner + 产品/治理  
输出：规则结构、优先级、冲突处理、适用范围、source/provenance 要求；规则内容必须可审核、版本化。

### CD-04 — Knowledge / Rule Release Governance Content

Owner：医学 Owner + 工程  
输出：首个可注册的 `KnowledgeRelease` 与 `SafetyCriticalRiskRulePack` 内容包及元数据；复用 U03 已实现的最小 P04/P06 机制。

### CD-05 — D09 Clinical Policy Definition

Owner：医学 Owner  
输出：从 accepted evidence 到以下正式 disposition 的确定性策略：

```text
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
FAILED
```

必须保持 `NO_HIGH_RISK_SIGNAL != SAFE`。

### CD-06 — Risk EvalSet / Safety Suite Specification & Dataset

Owner：医学 Owner + Eval Owner  
输出：golden cases、expected evidence、expected disposition、failure cases、版本绑定信息、评估指标。

### CD-07 — C02 / D09 Clinical Implementation

Owner：工程  
前置：CD-01～CD-06 足以形成可执行 contract。  
输出：真实 C02 Risk Evidence Engine、Rule/Knowledge evaluator、D09 deterministic policy implementation；不得扩大到 U04。

### CD-08 — Clinical Verification & Release Readiness

Owner：医学 Owner + Eval Owner + 工程  
输出：Eval/Safety Suite 结果、失败分析、release candidate decision；只有通过后，U03 才可从 `ENGINEERING_IMPLEMENTED` 提升到更高验证状态。

---

## 4. 依赖顺序

```text
CD-01 Clinical semantics
      ↓
CD-02 Evidence definitions
      ↓
CD-03 Rule specification ─────┐
CD-05 D09 policy             │
      ↓                      │
CD-04 governed releases      │
CD-06 EvalSet/Safety Suite ──┘
      ↓
CD-07 implementation
      ↓
CD-08 clinical verification
      ↓
U03 clinically verifiable
      ↓
then U04 Safety Gate implementation
```

CD-04/CD-06 可以在定义稳定后并行准备；CD-07 不应在医学语义未冻结时先写真实临床规则。

---

## 5. 是否需要新 Foundation

结论：

```text
New Foundation = NOT_REQUIRED AT THIS STAGE
```

理由：

- Foundation-1 已提供 Capability binding / invocation / trace 基线；
- U03 已实现首个消费者所需的最小 P04/P06 release binding；
- 当前真正阻塞是临床内容、policy 与 EvalSet，不是新的通用 Runtime 平台；
- 在没有第二个明确消费者之前，不应把 P04/P06 扩成 full platform。

若后续 U08/U10 等出现共同的知识发布需求，再按消费者驱动原则评估是否提炼共享 Foundation。

---

## 6. 当前结论

```text
U03 dependency gap = CONFIRMED
Engineering scaffold dependency = SATISFIED
Clinical definition dependency = NOT_SATISFIED
Clinical governed content = NOT_SATISFIED
Clinical Eval dependency = NOT_SATISFIED
New Foundation = NOT_REQUIRED
U04 Implementation = SHOULD_NOT_START YET
```

下一步不是直接写临床 Risk Engine，而是先形成并审核 `U03 Clinical Input Package`。