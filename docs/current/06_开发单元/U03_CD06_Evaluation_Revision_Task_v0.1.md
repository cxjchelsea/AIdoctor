# U03 CD-06 Evaluation Revision Task v0.1

> 权威输入：`U03_CD06_Evaluation_Review_Record_v0.1.md`  
> 状态：`REVISION_REQUIRED / CD-06_NOT_REVIEW_READY / NOT_GATE_C`

本任务只关闭 BF-CD06-01 / BF-CD06-02。不改 C/D/E 冻结对象，不开始 evaluation execution。

---

## 1. BF-CD06-01 — 纠正 15-rule 代表性覆盖

Coverage Manifest 必须把阳性 rule 绑到真正 MATCHED 的 case：

```text
C-RULE-SEPSIS-APPEAR-HIGH-001
→ 新的或改写后的 HIGH_RISK golden case
!= GC-014

C-RULE-SEPSIS-RASH-HIGH-001
→ 新的或改写后的 HIGH_RISK golden case
!= GC-015

C-RULE-SEPSIS-HR-HIGH-001
→ 独立 HIGH_RISK golden case
!= GC-016
```

GC-014 / GC-015 继续保留为 specialized-family SCOPE_MISMATCH → P4 例。

GC-016 继续保留为 HIGH + insufficiency；manifest 的 multi-hit HIGH 行不得再只指向它。

---

## 2. BF-CD06-02 — 把 Golden Case 补成可执行 review 对象

每个 GC 至少补齐：

```text
case_id
case_version
clinical_state_fixture_ref
clinical_state_version
input_fact_refs[]
expected_evidence_refs[]
expected_rule_refs[]
expected_policy_ref
expected_result_status
expected_disposition
expected_reason_code
must_not_output[]
must_not_commit[]
source_refs[]
rationale_ref
provenance_refs[]
```

统一绑定现行 0.2.1 集合。没有 fixture 的 purpose table 不得标 `REVIEW_READY`。

---

## 3. 明确不要做的事

```text
不要改 RR-U03-RISK-001@0.2.1-candidate
不要改 PR-U03-D09-001@0.2.1-candidate
不要改 U03_D09_COVERAGE_V0_2_1_CANDIDATE
不要改 KR-U03-SOURCE-001@0.1.0-candidate
不要开始 evaluation execution
不要宣称 Gate C PASS
不要打开儿科 / 孕产临床规则
```

---

## 4. 完成定义

```text
15 条 active C rule 各有可追溯的代表性 Gate C case
APPEAR / RASH / HR HIGH 不再误绑 P4 scope 例
28+ 个 GC 具备 schema 最小字段
再审 E-EVAL-02 / E-EVAL-07 = APPROVE
BF-CD06-01 / BF-CD06-02 = CLOSED
```

之后才能再评估 CD-06 REVIEW_READY。
