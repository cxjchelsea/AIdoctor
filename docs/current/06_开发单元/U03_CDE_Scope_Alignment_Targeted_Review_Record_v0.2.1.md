# U03 C / D / E Scope Alignment Targeted Review Record v0.2.1

> 对象：BF-CDE-01 的 targeted correction drafts。  
> 状态：`REVIEW_NOT_STARTED / BF-CDE-01_OPEN / GATE_B_BLOCKED / NOT_FOR_PRODUCTION`  
> 本记录只再审 pregnancy / puerperium whole-slice scope alignment；不重审 15 条 C rule、6 个 D branch 或 E source registry。

---

## 1. Review Inputs

```text
U03_C_Rule_Release_Scope_Revision_Draft_v0.2.1.md
U03_D09_Policy_Scope_Revision_Draft_v0.2.1.md
U03_D09_Coverage_Contract_Revision_Draft_v0.2.1.md
U03_CDE_Cross_Consistency_Review_v0.1.md
U03_CDE_Cross_Consistency_Revision_Task_v0.1.md
A v0.2 source-locked scope
KR-U03-SOURCE-001@0.1.0-candidate
```

---

## 2. C Scope Review

| ID | Question | Medical | Technical |
|---|---|---|---|
| C-SCOPE-R1 | C 0.2.1 是否忠实继承 A/E whole-slice pregnancy/puerperium exclusion | PENDING | PENDING |
| C-SCOPE-R2 | NG253 sepsis shared-scope 是否仍只是额外 family-level constraint | PENDING | PENDING |
| C-SCOPE-R3 | 15 条 rule predicate/threshold/signal 是否完全未改 | PENDING | PENDING |

---

## 3. D Scope Review

| ID | Question | Medical | Technical |
|---|---|---|---|
| D-SCOPE-R1 | D 0.2.1 是否把 pregnancy/puerperium 明确写成 whole-policy exclusion | PENDING | PENDING |
| D-SCOPE-R2 | whole-policy pregnancy/puerperium 是否进入 P0 `OVERALL_POLICY_SCOPE_MISMATCH`，而非 family `NOT_APPLICABLE` | PENDING | PENDING |
| D-SCOPE-R3 | 6 branch / precedence / disposition mapping 是否完全未改 | PENDING | PENDING |

---

## 4. Coverage Review

| ID | Question | Medical | Technical |
|---|---|---|---|
| COV-SCOPE-R1 | denominator 是否只在 whole-policy scope satisfied 后构建 | PENDING | PENDING |
| COV-SCOPE-R2 | pregnancy/puerperium scope failure 是否阻止 baseline 5 条进入 denominator | PENDING | PENDING |
| COV-SCOPE-R3 | denominator membership / signal mapping 是否完全未改 | PENDING | PENDING |

---

## 5. Cross-cutting Checks

| ID | Question | Medical | Technical |
|---|---|---|---|
| CDE-SCOPE-X1 | A / E / C / D / coverage 对 pregnancy/puerperium 是否形成唯一一致解释 | PENDING | PENDING |
| CDE-SCOPE-X2 | 是否未新增来源、阈值、evidence 或 disposition | PENDING | PENDING |
| CDE-SCOPE-X3 | 是否保持 frozen 0.2.0 historical candidates immutable | PENDING | PENDING |
| CDE-SCOPE-X4 | 是否仍明确 `NO_HIGH_RISK_SIGNAL != SAFE`、D != U04 | PENDING | PENDING |

---

## 6. Approval Rule

只有同时满足：

```text
C-SCOPE-R1..R3 = APPROVE / APPROVE
D-SCOPE-R1..R3 = APPROVE / APPROVE
COV-SCOPE-R1..R3 = APPROVE / APPROVE
CDE-SCOPE-X1..X4 = APPROVE / APPROVE
blocking finding = 0
```

才允许：

```text
BF-CDE-01 = CLOSED_FOR_CONTENT
```

之后仍需新 candidate identity / freeze 以及 C/D/E cross-consistency re-review，不能直接 Gate B PASS。

---

## 7. Current Status

```text
Targeted Revision Drafts = AVAILABLE
Medical Review = NOT_STARTED
Technical Review = NOT_STARTED
BF-CDE-01 = OPEN
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
```
