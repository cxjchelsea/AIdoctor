# U03 D09 Clinical Policy Re-Review Record v0.2

> 对象：`PR-U03-D09-001@0.2.0-draft` + `U03_D09_COVERAGE_V0_2`。  
> 目标：只再审 v0.1 的 BF-D-01 / BF-D-02 与修订后的执行一致性。  
> 状态：`RE_REVIEW_REQUIRED / NOT_APPROVED / NOT_FROZEN / D_RUNTIME_BLOCKED`。  
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
| D2-R1 | P-001 是否显式包含 overall population/region/channel scope mismatch | PENDING | PENDING |
| D2-R2 | reason code 是否使用 `OVERALL_POLICY_SCOPE_MISMATCH` | PENDING | PENDING |
| D2-R3 | 是否与 C `RULE_SIGNAL_SCOPE_MISMATCH` 明确分层 | PENDING | PENDING |
| D2-R4 | 是否未新增第七个医学 disposition branch | PENDING | PENDING |

BF-D-01 closure rule：

```text
D2-R1..R4 = APPROVE / APPROVE
→ BF-D-01 = CLOSED
```

---

## 3. BF-D-02 Coverage Contract Re-review

| ID | Question | Medical | Technical |
|---|---|---|---|
| D2-C1 | baseline 5 条是否被固定为 overall D scope 内 ALWAYS_APPLICABLE | PENDING | PENDING |
| D2-C2 | dyspnoea 2 条是否只作为 conditionally applicable family | PENDING | PENDING |
| D2-C3 | sepsis 8 条是否只作为 conditionally applicable family | PENDING | PENDING |
| D2-C4 | C `RULE_SIGNAL_SCOPE_MISMATCH` 是否映射为 NOT_APPLICABLE 并排除 denominator | PENDING | PENDING |
| D2-C5 | C `RULE_SIGNAL_INPUT_INSUFFICIENT` 是否映射为 INSUFFICIENT_APPLICABLE、进入 P2 并阻断 P3/P4 | PENDING | PENDING |
| D2-C6 | P-020 是否显式引用 `U03_D09_COVERAGE_V0_2` | PENDING | PENDING |
| D2-C7 | P-040 是否显式引用 `U03_D09_COVERAGE_V0_2` 并以 coverage-complete denominator 为前置 | PENDING | PENDING |
| D2-C8 | suspected_sepsis/dyspnoea context UNKNOWN 是否明确不能得到 NO_HIGH_RISK_SIGNAL | PENDING | PENDING |

BF-D-02 closure rule：

```text
D2-C1..C8 = APPROVE / APPROVE
→ BF-D-02 = CLOSED
```

---

## 4. Branch Consistency Recheck

不重新讨论 v0.1 已通过的医学方向，只确认修订没有破坏它们：

| Branch | Expected responsibility | Medical | Technical |
|---|---|---|---|
| D09-P-001 | P0 FAILED / integrity + overall policy scope | PENDING | PENDING |
| D09-P-010 | P1 VALID / HIGH_RISK | PENDING | PENDING |
| D09-P-020 | P2 FAILED / INSUFFICIENT_INFORMATION via coverage contract | PENDING | PENDING |
| D09-P-030 | P3 VALID / CAUTION, no P2 | PENDING | PENDING |
| D09-P-040 | P4 VALID / NO_HIGH_RISK_SIGNAL only coverage-complete | PENDING | PENDING |
| D09-P-090 | P5 FAILED / UNRESOLVABLE_CONFLICT | PENDING | PENDING |

必须保持：

```text
P0 > P1 > P2 > P3 > P4 > P5
```

---

## 5. Cross-cutting Checks

| ID | Question | Medical | Technical |
|---|---|---|---|
| D2-X1 | HIGH + insufficiency 是否仍保持 HIGH_RISK 并保留 insufficient refs | PENDING | PENDING |
| D2-X2 | CAUTION + insufficiency 是否仍 fail closed | PENDING | PENDING |
| D2-X3 | `NO_HIGH_RISK_SIGNAL != SAFE/NORMAL/no disease` 是否保持 | PENDING | PENDING |
| D2-X4 | 是否未重新解释 C 阈值/evidence 或新增来源 | PENDING | PENDING |
| D2-X5 | 是否未定义 U04 Safety Gate / runtime implementation | PENDING | PENDING |
| D2-X6 | coverage contract 是否仍为 review object，未被自动冻结 | PENDING | PENDING |

---

## 6. Approval Rule

只有同时满足：

```text
BF-D-01 = CLOSED
BF-D-02 = CLOSED
6 / 6 branch Medical = APPROVE
6 / 6 branch Technical = APPROVE
D2-X1..X6 = APPROVE / APPROVE
coverage contract Medical = APPROVE
coverage contract Technical = APPROVE
```

才能进一步判断：

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
PR-U03-D09-001@0.2.0-draft = AVAILABLE
U03_D09_COVERAGE_V0_2 = REVIEW_DRAFT / NOT_FROZEN
BF-D-01 Revision = APPLIED
BF-D-02 Revision = APPLIED
Medical Re-review = NOT_STARTED
Technical Re-review = NOT_STARTED
D Content Approval = NOT_COMPLETE
D Candidate Freeze = NOT_COMPLETE
CD-05 = NOT_PASSED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
```
