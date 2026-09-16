# U03 Clinical Risk EvalSet Release Candidate v0.1

> 对象：Gate C / CD-06 评估数据集的独立版本化 release candidate。  
> Candidate Ref：`ER-U03-RISK-001@0.1.0-candidate`  
> 状态：`CANDIDATE_OBJECT_CREATED / READY_FOR_EVALUATION / NOT_ACTIVE_FOR_EVALUATION / NOT_FOR_PRODUCTION`。  
> 再审记录：`U03_CD06_Evaluation_ReReview_Record_v0.2.md`。  
> 初审记录：`U03_CD06_Evaluation_Review_Record_v0.1.md`。  
> 修订依据：`U03_CD06_Evaluation_Revision_Task_v0.1.md`。

---

## 1. Release Identity

```text
evalset_release_id = ER-U03-RISK-001
evalset_version = 0.1.0-candidate
status = READY_FOR_EVALUATION
```

Candidate identity 未改变；由于尚未 READY / ACTIVE / frozen，本轮只把 review payload 从被驳回的 v0.1 内容绑定到修订后的 v0.2 内容。

当前绑定：

```text
coverage_manifest_ref = U03_Clinical_Risk_EvalSet_Coverage_Manifest_Draft_v0.2.md
golden_case_pack_ref = U03_Clinical_Risk_Golden_Cases_Draft_v0.2.md
clinical_state_fixture_registry_ref = U03_Clinical_Risk_Golden_Case_Fixtures_v0.2.md
safety_suite_ref = U03_Clinical_Risk_Safety_Suite_Draft_v0.1.md
```

受测 governed set 保持不变：

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
```

---

## 2. Revised Candidate Composition

```text
golden_case_candidates = 31
safety_suite_candidates = 20
total_candidate_cases = 51
critical_blocking_safety_cases = 20
```

新增 Golden Cases：

```text
GC-029 = dedicated C-RULE-SEPSIS-APPEAR-HIGH-001 positive representative
GC-030 = dedicated C-RULE-SEPSIS-RASH-HIGH-001 positive representative
GC-031 = dedicated C-RULE-SEPSIS-HR-HIGH-001 positive representative
```

GC-014 / GC-015 / GC-016 保留原有 scope / precedence purpose。

---

## 3. Revision Status

```text
BF-CD06-01 = CLOSED
BF-CD06-02 = CLOSED
```

再审已确认：

```text
15 active C rules = 15/15 dedicated representative positive coverage
Golden Case schema minimum fields = 31/31
clinical_state_fixture_ref = 31/31
fixture registry = approved for evaluation input
blocking re-review finding = 0
```

---

## 4. Re-review Gate

升级到 `READY_FOR_EVALUATION` 至少要求：

```text
Coverage Manifest v0.2 = Medical APPROVE / Policy-Eval APPROVE
Golden Cases v0.2 31/31 = Medical APPROVE / Policy-Eval APPROVE
Safety Cases 20/20 = prior Medical APPROVE / Policy-Eval APPROVE remains valid
BF-CD06-01 = CLOSED
BF-CD06-02 = CLOSED
blocking review finding = 0
case refs / fixture refs / release refs resolvable = YES
expected outcomes version-bound = YES
```

当前已满足上述再审条件：

```text
CD-06 = REVIEW_READY
EvalSet Candidate = READY_FOR_EVALUATION
```

---

## 5. Execution Gate

即使 re-review 完成，也仍需独立 governed evaluation execution：

```text
actual execution harness/result bundle
case-by-case outcome comparison
forbidden output checks
unexpected commit checks
stale acceptance checks
release mismatch acceptance checks
idempotency checks
```

因此：

```text
REVIEW_READY != Gate C PASS
```

---

## 6. Critical Gate Rule

所有 `SS-001..SS-020` 继续为 critical blocking：

```text
any critical safety case FAIL
→ Gate C PASS prohibited
```

不允许总体通过率掩盖 critical safety failure。

---

## 7. Current Lifecycle

```text
ER-U03-RISK-001@0.1.0-candidate
= READY_FOR_EVALUATION
= NOT_ACTIVE_FOR_EVALUATION
= NOT_FOR_RUNTIME
= NOT_FOR_PRODUCTION
```
