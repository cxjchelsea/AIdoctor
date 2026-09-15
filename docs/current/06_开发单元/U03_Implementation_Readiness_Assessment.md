# U03 Implementation Readiness Assessment

> Unit：U03 当前版本风险评估  
> 阶段：Implementation Readiness Assessment  
> 分支：`prep/u03-current-version-risk-assessment`  
> 基线：`main@df1ee8dab58156ec9594d505c06d31622633072b`  
> 本文件只判断 U03 是否具备进入实现的前置条件；Readiness PASS 不等于 Implementation Authorization，更不等于 Merge / Production Authorization。

---

## 1. 冻结输入

U03 仅消费：

```text
current valid Clinical State Version
+
Risk Assessment = NOT_ASSESSED / STALE
+
CLINICAL_STATE_CHANGED or RISK_REEVALUATION_REQUIRED
```

U02 已提供 versioned Clinical State、typed clinical facts/derived assertions 与最小 dependent-state invalidation 语义；U03 不重新设计 U02。

---

## 2. 冻结输出与 Owner

U03 先形成：

```text
Risk Assessment = VALID / FAILED
```

仅当 VALID 时，由唯一确定性 Owner D09 形成：

```text
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
```

Owner 边界：

```text
C02 = Risk Evidence / Candidate Producer
D09 = sole Clinical Risk Disposition Owner
P01/G2 = formal state commit owner
P05 = trace only
U04 = Safety Gate owner（不在 U03 实现范围）
```

必须保持：

```text
Risk FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
NO_DDX != LOW_RISK
C02 Result != Clinical Truth
Risk Evidence != Risk Disposition
Candidate != Decision != Proposal != Commit
Trace != Clinical State
```

---

## 3. U03 实施包冻结

### U03-01 — K05 C02 Risk Evidence Contract

建立 candidate/evidence-only contract，至少覆盖：

```text
clinical_state_version
red_flag_evidence[]
risk_factor_evidence[]
vital_sign_safety_signals[] / equivalent governed evidence
must_not_miss_signals[] / equivalent governed evidence
confidence / uncertainty
limitations
source_refs
capability binding/version refs
knowledge_release_refs
rule_release_refs
provenance
failure semantics
```

不得包含正式 Clinical Risk Disposition 所有权。

### U03-02 — C02 Risk Evidence Engine + Eval

最小实现：

```text
Red Flag Evidence
Risk Factor / Combination Rule Hits
Vital-sign Safety Signals
Must-not-miss Signals
```

并建立 U03-first safety eval：

- red-flag recall 优先；
- 特殊人群；
- 生命体征；
- must-not-miss；
- no-evidence / ambiguous / conflicting input；
- capability failure；
- rule/knowledge unavailable。

旧 Risk Engine 仅可作为 refactor/evidence-level asset，不得保留 final-risk owner 语义。

### U03-03 — Minimal P04 Safety-critical Release Governance

只建设 U03 首个消费者所需的最小不可变版本对象/读取边界：

```text
KnowledgeRelease
RuleRelease / Safety-critical Risk Rule Pack
```

至少带：

```text
release id/version
status/effective scope
evidence/provenance refs
population/region/language/channel constraints where applicable
effective_from / effective_to
```

不建设 full P04 platform、内容管理后台或通用发布系统。

### U03-04 — P06 Risk Release Binding Increment

在 Foundation-1 capability binding/resolver 基础上增量支持 U03 所需：

```text
Capability Binding
+
Knowledge Release binding
+
Risk Rule / Safety Pack binding
+
Scope/Contract Version
```

要求 exact/effective binding，失败不得静默降级为默认低风险。

### U03-05 — Governed C02 Invocation

复用：

```text
BindingReleaseResolver
CapabilityInvocationGuard
```

形成 C02 governed invocation；`enabled != authorized` 继续成立。

### U03-06 — U03 Business Owner Evidence Acceptance Boundary

Business Owner 负责：

- 校验 C02 response 与授权 binding/release/version 一致；
- 判断 evidence 是否达到 D09 可裁决输入边界；
- capability/rule/knowledge failure 显式形成 Risk Assessment FAILED；
- 不让 C02 直接变成正式 Risk。

### U03-07 — D09 Deterministic Clinical Risk Disposition

D09 是唯一 final owner。

最小输出：

```text
VALID + NO_HIGH_RISK_SIGNAL
VALID + CAUTION
VALID + HIGH_RISK
FAILED
```

必须可追溯到：

```text
clinical_state_version
accepted evidence refs
rule release
knowledge release
capability binding/version
reason code
```

### U03-08 — K09 Typed Risk Assessment Proposal + P01 Commit

由 Business Owner / D09 Decision 生成 typed K09 proposal，经现有 P01/G2/StateCommitter 正式提交。

要求：

- proposal base version 明确；
- evaluated clinical_state_version 明确；
- VALID/FAILED 与 disposition 分离；
- FAILED 不携带伪低风险 disposition；
- idempotency；
- same trigger max one formal effect；
- newer state version 不允许旧 Risk 冒充 current。

### U03-09 — P05 Risk Trace Increment

复用 Foundation-1 P05，记录：

```text
U03 / C02 call
binding refs
knowledge/rule release refs
risk evidence result ref
D09 decision ref
proposal ref
commit result/version ref
failure ref
```

Trace 只观察，不成为 Risk State Owner。

### U03-10 — Tests / Eval / Regression / Boundary Guards

至少验证：

1. 高风险输入稳定形成 HIGH_RISK；
2. no red flag 不会产生 SAFE 语义；
3. Risk failure 不会产生 NO_HIGH_RISK_SIGNAL；
4. P04/rule/knowledge failure fail closed；
5. exact Clinical State Version binding；
6. newer facts 使旧 Risk 不再 current；
7. D09 是唯一 final disposition owner；
8. C02 输出不能直接 commit；
9. same trigger/idempotency 无重复正式 effect；
10. U02/Foundation-1/C01-U01/StateCommitter 回归不退化。

### U03-11 — Implementation / Verification Record

实现完成后补：

```text
U03_实施与验证记录.md
U03_Legacy_Retirement_Update.md
```

Readiness 阶段不提前宣称完成。

---

## 4. 明确排除

本 U03 Implementation Authorization 即使后续获得，也不包含：

```text
U04 Safety Gate implementation
U05+ downstream units
full P04 knowledge platform
full P06 release platform
full D05 dependency engine
external /continue cutover
legacy physical deletion
clinical production activation
Production Authorization
```

U03 `FAILED` 只作为 U04 的 safety input；U03 不自行实现 U14 failure routing。

---

## 5. Dependency / Foundation Gate

### 5.1 Mainline dependencies

```text
Foundation-1 = SATISFIED / MERGED
C01-U01 = SATISFIED / MERGED
U02 = SATISFIED / MERGED
P01 mechanical commit foundation = AVAILABLE
P05 capability trace baseline = AVAILABLE
P06 capability binding/resolver baseline = AVAILABLE
```

### 5.2 U03-first consumer gaps

```text
C02-U03 = DEFINED / NOT_IMPLEMENTED
D09 = DEFINED / NOT_IMPLEMENTED
Minimal P04 Risk Knowledge/Rule Release Governance = DEFINED / NOT_IMPLEMENTED
P06 Risk Release Binding Increment = DEFINED / NOT_IMPLEMENTED
P05 Risk Release Trace Increment = DEFINED / NOT_IMPLEMENTED
```

这些是 U03 implementation package 的组成，不是 Readiness blocker。

### 5.3 Foundation-2

```text
Foundation-2 = NOT_REQUIRED
```

Foundation-1 已提供共享治理骨架；C02 与 Safety-critical Risk Rule Pack 的首个消费者均为 U03，应由 U03 consumer-driven 增量建设，而不是先建设脱离消费者的第二个平台阶段。

---

## 6. Legacy Gate

`U03_Legacy_Impact_Assessment.md` 已冻结：

```text
legacy Risk Engine = REFACTOR / EVIDENCE_ONLY
legacy final-risk semantics = BLOCK_NEW_U03_USE
legacy fixed workflow/direct risk branching = LEGACY_ONLY / BLOCK_NEW_USE
CDPManager.updateCDP = BLOCK_NEW_U03_USE for formal Risk
P01/StateCommitter = KEEP + ADAPT
Foundation-1 governance = REUSE + U03 INCREMENT
physical legacy removal = NOT_AUTHORIZED
```

因此 Legacy Impact Assessment 不构成实现阻塞。

---

## 7. Readiness Risks / Required Controls

| 风险 | Readiness 控制 | 状态 |
|---|---|---|
| C02 偷偷成为 final Risk owner | D09 sole-owner + contract boundary test | DEFINED |
| Risk failure 被映射为低风险 | explicit VALID/FAILED + fail-closed tests | DEFINED |
| no red flag 被解释为 SAFE | disposition semantics + U04 ownership separation | DEFINED |
| Rule/Knowledge 漂移 | immutable versioned release + exact binding | DEFINED |
| old Risk 跨 Clinical State Version 继续生效 | evaluated-version binding + currentness test | DEFINED |
| 旧 Risk Engine 规则直接进入正式状态 | evidence-only adapter boundary | DEFINED |
| P04/P06 被扩成平台重构 | U03-consumer minimum scope | CONTROLLED |
| U03 越权实现 U04/U14 | explicit exclusions | CONTROLLED |

暂无阻塞 Implementation Readiness 的未定义项。

---

## 8. Readiness Conclusion

```text
U03 Business Design = READY
U03 Legacy Impact Assessment = COMPLETE
C02-U03 Gap = DEFINED
D09 Scope = DEFINED
P04-U03 Minimum Release Governance Gap = DEFINED
P06 Risk Release Binding Gap = DEFINED
P05 Risk Trace Increment Gap = DEFINED
P01 Risk Commit Reuse Boundary = DEFINED
Foundation-1 Dependency = SATISFIED
U02 Mainline Dependency = SATISFIED
Foundation-2 = NOT_REQUIRED
U03 Implementation Readiness = PASS / IMPLEMENTATION_READY

U03 Implementation Authorization = NOT_GRANTED
U03 Business Implementation = NOT_STARTED
U03 Unit Verification = NOT_STARTED
U03 External Business Wiring = NOT_COMPLETE
U03 E2E Business Loop = NOT_VERIFIED
Clinical Evaluation = NOT_COMPLETE
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
Physical Legacy Removal = NOT_AUTHORIZED
Merge Authorization = NOT_GRANTED
```

下一门禁必须是显式 `Implementation Authorization`。未获得前，不进入 U03 代码实现。
