# U03 D09 Coverage Contract Freeze Record v0.2

> 对象：`U03_D09_COVERAGE_V0_2`。  
> 状态：`CANDIDATE_FROZEN / BLOCKER-FZ-D-01_CLOSED / NOT_A_D_POLICY_FREEZE / NOT_FOR_PRODUCTION`。  
> 冻结日期：`2026-09-15`  
> 本记录只冻结 D09 coverage denominator contract；不创建 `PR-U03-D09-001@0.2.0-candidate`，不冻结 D policy，不构成 CD-05 / Gate B / Gate C / Implementation Authorization。

---

## 1. Freeze Target

```text
coverage_contract_ref = U03_D09_COVERAGE_V0_2
coverage_contract_file = U03_D09_Coverage_Contract_v0.2.md
freeze_status = CANDIDATE_FROZEN
```

该 contract 绑定：

```text
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
policy_pair_freeze_ref = PF-U03-C-POLICY-001
```

---

## 2. Preconditions

已满足：

```text
D Content Approval = APPROVED_FOR_CONTENT_AND_COVERAGE
BF-D-01 = CLOSED
BF-D-02 = CLOSED
coverage contract Medical Review = APPROVE
coverage contract Technical Review = APPROVE
blocking finding = 0
```

权威审核记录：

```text
U03_D09_Clinical_Policy_ReReview_Record_v0.2.md
```

因此 coverage contract 内容已经具备冻结条件。

---

## 3. Frozen Semantics

本 freeze 锁定以下 denominator / applicability 语义。

### 3.1 Overall policy scope

```text
population_scope = Gate-A / C-candidate source-locked adult scope only
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
```

整体 policy scope 不成立：

```text
→ D09-P-001
→ FAILED
→ OVERALL_POLICY_SCOPE_MISMATCH
```

### 3.2 ALWAYS_APPLICABLE denominator

当 overall D scope 成立时，下列 5 条始终属于 denominator：

```text
C-RULE-RESP-001
C-RULE-NEURO-001
C-RULE-NEURO-002
C-RULE-CARD-001
C-RULE-ALLERGY-001
```

### 3.3 CONDITIONALLY_APPLICABLE families

```text
NHS_DYSPNOEA_FAMILY
  C-RULE-DYSPNOEA-APPEAR-001
  C-RULE-DYSPNOEA-CONFUSION-001

NG253_SEPSIS_FAMILY
  C-RULE-SEPSIS-RR-HIGH-001
  C-RULE-SEPSIS-RR-MODHIGH-001
  C-RULE-SEPSIS-SBP-HIGH-001
  C-RULE-SEPSIS-SBP-MODHIGH-001
  C-RULE-SEPSIS-HR-HIGH-001
  C-RULE-SEPSIS-HR-MODHIGH-001
  C-RULE-SEPSIS-APPEAR-HIGH-001
  C-RULE-SEPSIS-RASH-HIGH-001
```

### 3.4 Coverage state mapping

只允许：

```text
C MATCHED / NO_MATCH
→ APPLICABLE_EVALUATED

C RULE_SIGNAL_INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE
→ counts for P-020
→ blocks P-030 / P-040

C RULE_SIGNAL_SCOPE_MISMATCH
→ NOT_APPLICABLE
→ excluded from P-020 / P-040 denominator
```

必须保持：

```text
OVERALL_POLICY_SCOPE_MISMATCH
!= RULE_SIGNAL_SCOPE_MISMATCH
```

前者是 D-level whole-policy failure，后者只表示 specialized-family non-applicability。

---

## 4. Freeze Effect

冻结后：

```text
U03_D09_COVERAGE_V0_2
= RESOLVABLE
= MEDICAL_APPROVED
= TECHNICAL_APPROVED
= CANDIDATE_FROZEN
```

任何后续 denominator / applicability 修改必须形成新的 coverage contract version，并重新审核；不得原地修改 `U03_D09_COVERAGE_V0_2` 的冻结语义。

---

## 5. What This Freeze Does Not Mean

```text
Coverage Contract Frozen
!= D Policy Candidate Created
!= D Policy Candidate Frozen
!= CD-05 PASS
!= Gate B PASS
!= Gate C PASS
!= D09 runtime enabled
!= CD-07 Implementation Authorization
!= Production Authorization
```

`PR-U03-D09-001@0.2.0-draft` 仍保持 review draft 身份。

---

## 6. Freeze Blocker Status

```text
BLOCKER-FZ-D-01 = CLOSED

BLOCKER-FZ-D-02
= OPEN / D minimum pre-freeze evaluation not built/reviewed

BLOCKER-FZ-D-03
= OPEN / MUST_REMAIN_AFTER_D-02

BLOCKER-FZ-D-04
= OPEN / MUST_REMAIN_LAST
```

下一步只能按 `U03_D09_PreFreeze_Evaluation_Minimum_v0.1.md` 构建并审核 D minimum pre-freeze fixtures。
