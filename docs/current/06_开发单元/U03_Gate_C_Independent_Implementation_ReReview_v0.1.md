# U03 Gate C Independent Implementation Targeted Re-review v0.1

> 对象：PR #89 HEAD `d51536f` 对 `BF-IR-01` / `BF-IR-02` 的定向再审。  
> 初审：`U03_Gate_C_Independent_Implementation_Review_v0.1.md` = 历史 `REVISE_REQUIRED`，不改写。  
> 修订说明：`U03_Gate_C_Independent_Implementation_Review_Remediation_v0.1.md`。  
> 审核角色：`U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-16`  
> 状态：`TARGETED_REREVIEW_COMPLETE / APPROVE / BLOCKING_FINDING_0 / NOT_GATE_C / NOT_FOR_PRODUCTION`。

```text
Historical Independent Review = REVISE_REQUIRED
Targeted Re-Review = PASS
Independent Review current = PASS
REVIEW_COMPLETE 可用于后续单独启动的 Governed Evaluation
Governed Evaluation Execution = NOT_STARTED
Gate C = NOT_PASSED
```

本再审没有运行正式 Governed Evaluation，也没有把 workflow 标成已执行。

---

## 1. Scope

只审：

```text
BF-IR-01 是否关闭
BF-IR-02 是否关闭
是否引入新的语义 / Runtime 越权
```

不重开已 APPROVE 的隔离、精确 refs、诚实路径 P0–P4、missingness fail-closed。

---

## 2. Mandatory Questions

| ID | Question | Verdict |
|---|---|---|
| RR-01 | `forced_rule_results` 旁路是否已从 evaluator 删除 | APPROVE |
| RR-02 | GC-026 / SS-012 是否标为 `UNPRODUCIBLE_UNDER_SHARED_SCOPE` 且不计入 executable | APPROVE |
| RR-03 | P5 是否只作为 labeled defensive boundary，且 `counts_as_governed_c_execution = false` | APPROVE |
| RR-04 | 诚实非 P0 路径是否仍由 `evaluate_rules()` 产出完整 15 条 | APPROVE |
| RR-05 | GC-012 / 014 / 015 是否为三条独立 P4 coverage 路径 | APPROVE |
| RR-06 | 是否引入 Runtime / commit / U04 / 新医学条件 | APPROVE 无新越权 |

---

## 3. BF-IR-01 Closure

审查机核验：

```text
evaluator.py 含 forced_rule_results = False
executable GC = 30，不含 GC-026
executable SS = 19，不含 SS-012
excluded identities = {GC-026, SS-012}
classification = UNPRODUCIBLE_UNDER_SHARED_SCOPE
executable_counted = False
```

P5 现为 `D09_DEFENSIVE_CONTRACT_BOUNDARY_ONLY`：先跑完整 15 条诚实 C，再构造非法跨组件输入，确认 D09 fail-closed 到 `D09-P-090`。该检查不进入 executable GC/SS 计数。

```text
BF-IR-01 = CLOSED
```

---

## 4. BF-IR-02 Closure

审查机实测三条 fixture 的 C 状态互不相同，且均 `n_rules=15`、`policy=D09-P-040`：

```text
GC-012
  baseline = NO_MATCH
  dyspnoea = SCOPE_MISMATCH / NOT_APPLICABLE
  sepsis   = SCOPE_MISMATCH / NOT_APPLICABLE

GC-014
  baseline = NO_MATCH
  dyspnoea = NO_MATCH / APPLICABLE_EVALUATED
  sepsis   = SCOPE_MISMATCH / NOT_APPLICABLE

GC-015
  baseline = NO_MATCH
  dyspnoea = SCOPE_MISMATCH / NOT_APPLICABLE
  sepsis   = NO_MATCH / APPLICABLE_EVALUATED
```

这关闭“三个 ID、一条路径”。GC-014/015 现按“一条件 family 适用且全 NO_MATCH + 另一 family NOT_APPLICABLE”实例化，比初审 purpose 原文更可区分。接受该绑定。

```text
BF-IR-02 = CLOSED
```

---

## 5. N-IR-01 / No New Scope

SS-014 / SS-016 增加 AST 结构性 side-effect 检查。独立源码核验仍是隔离结论的权威来源；该项加强可接受，不新开 blocker。

未发现新的 Runtime / State Committer / U04 接线，未改冻结阈值，workflow 仍要求显式 `REVIEW_COMPLETE`。

审查机复跑：`python -m unittest -v` = `8 / 8 PASS`。这是 remediation 自洽证据，不是 Gate C execution evidence。

---

## 6. Decision

```text
BF-IR-01 = CLOSED
BF-IR-02 = CLOSED
blocking re-review finding = 0
Targeted Independent Re-Review = PASS

Historical Independent Review record = REVISE_REQUIRED / IMMUTABLE
Independent Implementation Review current = PASS

executable Golden cases = 30
executable Safety cases = 19
excluded identities = GC-026, SS-012

Governed Evaluation Execution = NOT_STARTED
BF-CD06-EXEC-01 = OPEN
Gate C = NOT_PASSED
Merge Authorization = NOT_GRANTED
Runtime / U04 / Production = BLOCKED
```

下一步才允许单独启动正式 Governed Evaluation Execution，固化 result bundle，再做 Gate C Decision Review。  
`Independent Review PASS != Gate C PASS`。
