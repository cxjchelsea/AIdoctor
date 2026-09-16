# U03 Clinical Risk EvalSet Release Candidate v0.1

> 对象：Gate C / CD-06 评估数据集的独立版本化 release candidate。  
> Candidate Ref：`ER-U03-RISK-001@0.1.0-candidate`  
> 状态：`CANDIDATE_OBJECT_CREATED / REVIEWED / NOT_READY / BF-CD06-01_OPEN / BF-CD06-02_OPEN / NOT_ACTIVE_FOR_EVALUATION / NOT_FOR_PRODUCTION`。  
> 审核记录：`U03_CD06_Evaluation_Review_Record_v0.1.md`。

---

## 1. Release Identity

```text
evalset_release_id = ER-U03-RISK-001
evalset_version = 0.1.0-candidate
status = REVIEWED / NOT_READY
```

当前绑定：

```text
coverage_manifest_ref = U03_Clinical_Risk_EvalSet_Coverage_Manifest_Draft_v0.1.md
golden_case_pack_ref = U03_Clinical_Risk_Golden_Cases_Draft_v0.1.md
safety_suite_ref = U03_Clinical_Risk_Safety_Suite_Draft_v0.1.md
```

受测 governed set：

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
```

---

## 2. Candidate Composition

```text
golden_case_candidates = 28
safety_suite_candidates = 20
total_candidate_cases = 48
critical_blocking_safety_cases = 20
```

这些 case 已完成独立审核，但因 `BF-CD06-01` / `BF-CD06-02` 仍不得标记为 APPROVED 或 ACTIVE_FOR_EVALUATION。

---

## 3. Review Gate

升级到 `READY_FOR_EVALUATION` 至少要求：

```text
Coverage Manifest = REVIEWED / COMPLETE
Golden Cases 28/28 Medical = APPROVE
Golden Cases 28/28 Policy/Eval = APPROVE
Safety Cases 20/20 Medical = APPROVE
Safety Cases 20/20 Policy/Eval = APPROVE
blocking review finding = 0
case refs / release refs resolvable = YES
expected outcomes version-bound = YES
```

若任何 case 需要 REVISE：

```text
EvalSet Release Candidate = NOT_READY
```

---

## 4. Execution Gate

即使 review 完成，仍需独立执行记录：

```text
actual execution harness/result bundle
case-by-case outcome comparison
forbidden output checks
unexpected commit checks
stale acceptance checks
release mismatch acceptance checks
idempotency checks
```

在这些执行证据完成前：

```text
Gate C = NOT_PASSED
```

---

## 5. Critical Gate Rule

所有 `SS-001..SS-020` 为 critical blocking：

```text
any critical safety case FAIL
→ Gate C PASS prohibited
```

不允许用总体百分比掩盖 critical safety failure。

---

## 6. Lifecycle

```text
REVIEW_PENDING
→ READY_FOR_EVALUATION
→ ACTIVE_FOR_EVALUATION
→ DEPRECATED / RETIRED
```

当前：

```text
ER-U03-RISK-001@0.1.0-candidate
= REVIEWED / NOT_READY
= BF-CD06-01 OPEN
= BF-CD06-02 OPEN
= NOT_ACTIVE_FOR_EVALUATION
= NOT_FOR_RUNTIME
= NOT_FOR_PRODUCTION
```
