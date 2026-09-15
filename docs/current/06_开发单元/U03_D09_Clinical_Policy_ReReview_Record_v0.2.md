# U03 D09 Clinical Policy Re-Review Record v0.2

> 对象：`PR-U03-D09-001@0.2.0-draft` + `U03_D09_COVERAGE_V0_2`。  
> 目标：只再审 v0.1 的 BF-D-01 / BF-D-02 与修订后的执行一致性。  
> 审核角色：`U03_MEDICAL_OWNER_REVIEW` + `U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-15`  
> 对照基线：`276c915`  
> 状态：`RE_REVIEW_COMPLETE / BF-D-01_CLOSED / BF-D-02_CLOSED / APPROVED_FOR_CONTENT_AND_COVERAGE / NOT_FROZEN / D_RUNTIME_BLOCKED`。  
> 不重新审核 C candidate，不开始 Gate C/CD-07/U04。

---

## 1. Review Inputs

```text
U03_D09_Clinical_Policy_Content_Draft_v0.2.md
U03_D09_Coverage_Contract_v0.2.md
U03_D09_Clinical_Policy_Review_Record_v0.1.md
U03_D09_Clinical_Policy_Revision_Task_v0.1.md
RR-U03-RISK-001@0.2.0-candidate
KR-U03-SOURCE-001@0.1.0-candidate
```

---

## 2. BF-D-01 Re-review

| ID | Question | Medical | Technical |
|---|---|---|---|
| D2-R1 | P-001 是否显式包含 overall population/region/channel scope mismatch | APPROVE | APPROVE |
| D2-R2 | reason code 是否使用 `OVERALL_POLICY_SCOPE_MISMATCH` | APPROVE | APPROVE |
| D2-R3 | 是否与 C `RULE_SIGNAL_SCOPE_MISMATCH` 明确分层 | APPROVE | APPROVE |
| D2-R4 | 是否未新增第七个医学 disposition branch | APPROVE | APPROVE |

```text
D09-P-001 preconditions now include
population_scope / region_scope / channel_scope mismatch

reason_code now includes
OVERALL_POLICY_SCOPE_MISMATCH

OVERALL_POLICY_SCOPE_MISMATCH
= D-level P0 failure
!= C RULE_SIGNAL_SCOPE_MISMATCH
!= specialized-family NOT_APPLICABLE
```

仍是原 6 个 branch，没有新的临床结论类型。

```text
BF-D-01 = CLOSED
```

---

## 3. BF-D-02 Coverage Contract Re-review

| ID | Question | Medical | Technical |
|---|---|---|---|
| D2-C1 | baseline 5 条是否被固定为 overall D scope 内 ALWAYS_APPLICABLE | APPROVE | APPROVE |
| D2-C2 | dyspnoea 2 条是否只作为 conditionally applicable family | APPROVE | APPROVE |
| D2-C3 | sepsis 8 条是否只作为 conditionally applicable family | APPROVE | APPROVE |
| D2-C4 | C `RULE_SIGNAL_SCOPE_MISMATCH` 是否映射为 NOT_APPLICABLE 并排除 denominator | APPROVE | APPROVE |
| D2-C5 | C `RULE_SIGNAL_INPUT_INSUFFICIENT` 是否映射为 INSUFFICIENT_APPLICABLE、进入 P2 并阻断 P3/P4 | APPROVE | APPROVE |
| D2-C6 | P-020 是否显式引用 `U03_D09_COVERAGE_V0_2` | APPROVE | APPROVE |
| D2-C7 | P-040 是否显式引用 `U03_D09_COVERAGE_V0_2` 并以 coverage-complete denominator 为前置 | APPROVE | APPROVE |
| D2-C8 | suspected_sepsis/dyspnoea context UNKNOWN 是否明确不能得到 NO_HIGH_RISK_SIGNAL | APPROVE | APPROVE |

P-020 / P-040 已不再使用“actually applicable governed rules/families”散文，而是引用：

```text
coverage_contract_ref = U03_D09_COVERAGE_V0_2
```

分母现已可执行：

```text
ALWAYS_APPLICABLE
= RESP / NEURO-001 / NEURO-002 / CARD / ALLERGY

CONDITIONALLY_APPLICABLE
= NHS_DYSPNOEA_FAMILY
= NG253_SEPSIS_FAMILY

C RULE_SIGNAL_SCOPE_MISMATCH
→ NOT_APPLICABLE
→ excluded from P-020 / P-040

C RULE_SIGNAL_INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE
→ counts for P-020
→ blocks P-030 / P-040
```

`suspected_sepsis` 或 NHS dyspnoea context 为 UNKNOWN / NOT_ESTABLISHED 时，只能经 C `INPUT_INSUFFICIENT` 进入 P2，不能形成 `NO_HIGH_RISK_SIGNAL`。

非阻塞备注：同一 conditional family 内若同时出现 `SCOPE_MISMATCH` 与 `MATCHED` / `INPUT_INSUFFICIENT`，视为 C shared-scope invariant 破坏，由 P-090 fail-closed；D 不另建第二套 family 聚合规则。

```text
BF-D-02 = CLOSED
coverage contract Medical = APPROVE
coverage contract Technical = APPROVE
coverage contract Frozen = NO
```

---

## 4. Branch Consistency Recheck

不重新讨论 v0.1 已通过的医学方向，只确认修订没有破坏它们：

| Branch | Expected responsibility | Medical | Technical |
|---|---|---|---|
| D09-P-001 | P0 FAILED / integrity + overall policy scope | APPROVE | APPROVE |
| D09-P-010 | P1 VALID / HIGH_RISK | APPROVE | APPROVE |
| D09-P-020 | P2 FAILED / INSUFFICIENT_INFORMATION via coverage contract | APPROVE | APPROVE |
| D09-P-030 | P3 VALID / CAUTION, no P2 | APPROVE | APPROVE |
| D09-P-040 | P4 VALID / NO_HIGH_RISK_SIGNAL only coverage-complete | APPROVE | APPROVE |
| D09-P-090 | P5 FAILED / UNRESOLVABLE_CONFLICT | APPROVE | APPROVE |

```text
P0 > P1 > P2 > P3 > P4 > P5
= UNCHANGED / APPROVE
```

P-030 已与 P-010 一样保留全部 `matched_rule_refs[]`。

---

## 5. Cross-cutting Checks

| ID | Question | Medical | Technical |
|---|---|---|---|
| D2-X1 | HIGH + insufficiency 是否仍保持 HIGH_RISK 并保留 insufficient refs | APPROVE | APPROVE |
| D2-X2 | CAUTION + insufficiency 是否仍 fail closed | APPROVE | APPROVE |
| D2-X3 | `NO_HIGH_RISK_SIGNAL != SAFE/NORMAL/no disease` 是否保持 | APPROVE | APPROVE |
| D2-X4 | 是否未重新解释 C 阈值/evidence 或新增来源 | APPROVE | APPROVE |
| D2-X5 | 是否未定义 U04 Safety Gate / runtime implementation | APPROVE | APPROVE |
| D2-X6 | coverage contract 是否仍为 review object，未被自动冻结 | APPROVE | APPROVE |

---

## 6. Approval Rule

本轮同时满足：

```text
BF-D-01 = CLOSED
BF-D-02 = CLOSED
6 / 6 branch Medical = APPROVE
6 / 6 branch Technical = APPROVE
D2-X1..X6 = APPROVE / APPROVE
coverage contract Medical = APPROVE
coverage contract Technical = APPROVE
blocking finding = 0
```

因此：

```text
D Content Approval = APPROVED_FOR_CONTENT_AND_COVERAGE
```

这仍不自动等于：

```text
coverage contract frozen
D policy candidate frozen
CD-05 PASS
Gate B PASS
Gate C PASS
Implementation Authorization
```

---

## 7. Current Status

```text
PR-U03-D09-001@0.2.0-draft = REVIEWED
U03_D09_COVERAGE_V0_2 = APPROVED_FOR_CONTENT / NOT_FROZEN
BF-D-01 = CLOSED
BF-D-02 = CLOSED
Medical Re-review = COMPLETE
Technical Re-review = COMPLETE
D Content Approval = APPROVED_FOR_CONTENT_AND_COVERAGE
D Candidate Freeze = NOT_COMPLETE
CD-05 = NOT_PASSED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
```

下一步只允许单独评估 coverage-contract freeze / D policy candidate freeze / CD-05 readiness。
不开始 D runtime、不宣称 Gate B/C PASS、不开始 CD-07。
