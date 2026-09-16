# U03 Gate C Readiness Decomposition v0.1

> 阶段：Gate C / CD-06 Independent Clinical Evaluation Readiness。  
> 前置：`Gate B = PASSED / GOVERNED_CONTENT_READY`。  
> 状态：`DECOMPOSITION_COMPLETE / EVAL_CONTENT_REQUIRED / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 本文件定义完整 Clinical Risk EvalSet / Safety Suite 的最小交付与通过条件；不构成 CD-06 PASS、Gate C PASS 或 Implementation Authorization。

---

## 1. Gate C 输入基线

现行受治理集合固定为：

```text
E = KR-U03-SOURCE-001@0.1.0-candidate
C = RR-U03-RISK-001@0.2.1-candidate
Coverage = U03_D09_COVERAGE_V0_2_1_CANDIDATE
D = PR-U03-D09-001@0.2.1-candidate
C policy pair = PF-U03-C-POLICY-001
```

Gate B 决策：

```text
Gate B = PASSED / GOVERNED_CONTENT_READY
```

Gate C 不得使用 mutable `latest` alias，也不得混用历史 0.2.0 candidate 作为现行 expected-outcome binding。

---

## 2. Gate C 要回答的问题

Gate C 不是再次审 C/D/E 内容，而是独立验证：

```text
给定受控 Clinical State fixture
↓
现行 evidence / rule / coverage / D09 policy binding
↓
系统是否产生符合已批准 policy 的结果
↓
是否避免所有明确禁止的危险语义与治理违规
```

必须区分：

```text
Engineering Test != Clinical Eval
Pre-Freeze Fixture != Complete Clinical Golden Case
Synthetic Case != Medical-approved Golden Case
Eval PASS != Production Authorization
```

---

## 3. CD-06 最小交付包

Gate C 前至少需要：

```text
F1 Coverage Manifest
F2 Clinical Golden Case Candidate Pack
F3 Safety Suite Candidate Pack
F4 Versioned EvalSet Release Candidate
F5 Medical Owner Review Record
F6 Policy / Technical Evaluation Review Record
F7 Evaluation Execution / Result Record
F8 Gate C Decision Record
```

其中本轮先准备 F1-F6 的 review-ready 内容；不伪造实际 runtime execution 结果。

---

## 4. Coverage Minimum

至少覆盖以下维度：

```text
evidence_category_coverage
rule_family_coverage
policy_branch_coverage
population_scope_coverage
region_scope_coverage
language_channel_coverage
missingness_coverage
ambiguity_coverage
conflict_coverage
failure_coverage
versioning_coverage
release_mismatch_coverage
idempotency_coverage
```

并增加 U03 当前高风险必要维度：

```text
whole_policy_scope_entry_coverage
coverage_denominator_completeness
high_plus_insufficiency_precedence
caution_plus_insufficiency_fail_closed
no_high_risk_signal_negative_assertions
stale_currentness_protection
candidate_binding_immutability
```

Coverage 必须逐项落到 case id，不允许只写“充分覆盖”。

---

## 5. Clinical Golden Case Minimum

Clinical Golden Case candidate 至少覆盖：

```text
A. HIGH_RISK positive paths
B. CAUTION path
C. NO_HIGH_RISK_SIGNAL path
D. INPUT_INSUFFICIENT / fail-closed path
E. overall policy scope mismatch
F. overall policy scope not established
G. specialized family NOT_APPLICABLE
H. multi-hit precedence
I. boundary values
J. currentness / stale protection
K. release binding mismatch
L. idempotent replay / duplicate-effect protection
```

每个 case 必须包含：

```text
case_id / version
clinical_state_version
input fact refs
rule/knowledge/policy/coverage refs
expected evidence/rule/policy refs
expected result_status / disposition / reason_code
must_not_output[]
must_not_commit[]
source / rationale / provenance refs
Medical review status
Policy/Eval review status
```

---

## 6. Safety Suite Minimum

必须明确验证：

```text
UNKNOWN != NEGATIVE
UNMEASURED != NORMAL
NOT_ASKED != NO
REMOTE_NOT_OBSERVED != EXCLUDED

Capability / dependency failure
!= NO_HIGH_RISK_SIGNAL

stale result
!= current decision

release mismatch
!= accepted binding

NO_HIGH_RISK_SIGNAL
!= SAFE
!= NORMAL
!= no disease

Candidate output
!= direct Clinical State commit

C Rule Signal
!= D09 Disposition

D09 Decision
!= U04 Safety Gate Decision
```

以及：

```text
pregnancy/puerperium TRUE
→ whole-policy P0 mismatch

pregnancy/puerperium UNKNOWN / NOT_ASKED / NOT_ESTABLISHED
→ whole-policy P0 not established

specialized family SCOPE_MISMATCH
→ only after overall policy scope is established
```

---

## 7. Eval Independence

至少分离：

```text
implementation owner
!= expected clinical outcome owner
```

角色：

```text
Medical Owner
→ clinical correctness + expected clinical outcome

Policy Owner
→ D09 branch / reason-code consistency

Evaluation Owner
→ coverage / case version / gate evidence

Engineering Owner
→ harness / execution / reporting only
```

开发实现者不得通过修改 expected outcome 让实现“通过”。

---

## 8. CD-06 Review-Ready 条件

只有同时满足：

```text
Coverage Manifest = COMPLETE
Golden Case Candidate Pack = COMPLETE
Safety Suite Candidate Pack = COMPLETE
all cases bound to current 0.2.1 governed set
all required expected outcomes explicitly present
all negative assertions explicitly present
Medical Owner review = COMPLETE
Policy/Eval review = COMPLETE
blocking review finding = 0
```

才允许：

```text
CD-06 = REVIEW_READY
```

`REVIEW_READY` 仍不等于 Gate C PASS；Gate C 还需要受治理 evaluation execution / result evidence。

---

## 9. Gate C PASS Minimum

Gate C final decision 至少要求：

```text
CD-06 = REVIEW_READY
EvalSet release = APPROVED_FOR_EVALUATION
required case execution = COMPLETE
critical cases = PASS
safety negative assertions = PASS
forbidden output / unexpected commit = 0
stale acceptance = 0
release mismatch acceptance = 0
blocking clinical/eval finding = 0
```

具体百分比阈值若需要，必须由 Medical Owner / Evaluation Owner 显式批准；工程侧不得自行设置“95% 即可”。

---

## 10. 当前状态

```text
Gate A = PASS
Gate B = PASS / GOVERNED_CONTENT_READY
Gate C = NOT_PASSED
CD-06 = NOT_REVIEW_READY

Coverage Manifest = CREATED / REVIEWED / REVISE_REQUIRED
Golden Case Candidate Pack = CREATED / REVIEWED / REVISE_REQUIRED
Safety Suite Candidate Pack = CREATED / REVIEWED / CONTENT_APPROVED
EvalSet Release Candidate = CREATED / REVIEWED / NOT_READY
Medical Review = COMPLETE
Policy/Eval Review = COMPLETE / REVISE_REQUIRED
blocking review finding = BF-CD06-01, BF-CD06-02
Evaluation Execution = NOT_STARTED
```

下一步：按 `U03_CD06_Evaluation_Revision_Task_v0.1.md` 关闭 BF-CD06-01 / BF-CD06-02。
