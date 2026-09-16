# U03 PR #89 Merge Authorization Review v0.1

> 对象：PR #89 `impl/u03-gatec-eval-only` 当前 HEAD `c01cad61f9aa2465dc1b151d9ed7c810b341e0db` 的独立 Merge Authorization 审查。  
> 基线：`prep/u03-clinical-dependency-completion`。  
> 输入：`U03_Post_Gate_C_Governance_Decision_v0.1.md`；Gate C 已 PASS 的冻结 evidence / re-decision。  
> 审核日期：2026-09-16。  
> 状态：`MERGE_AUTHORIZATION_REVIEW_COMPLETE / REVISE_REQUIRED / NOT_MERGE_AUTHORIZED`。

```text
Gate C = PASS / NOT_REOPENED
PR #89 Merge Authorization Review = REVISE_REQUIRED
Merge Authorization = NOT_GRANTED
PR #89 = KEEP_DRAFT
```

本审查只判断 PR #89 是否适合合入其当前目标基线，不重新打开 Gate A/B/C，不评估 CD-07 或 U04 readiness。

---

## 1. Mandatory Merge Questions

| ID | Question | Verdict |
|---|---|---|
| M1 | 合入物是否仍仅覆盖 `AUTH-U03-GATEC-EVAL-IMPL-001` evaluation-only 范围 | APPROVE |
| M2 | 是否无 C02/D09 runtime、U04、production commit、network/real-patient 接线 | APPROVE |
| M3 | Gate C PASS 是否未把 candidate 写成 ACTIVE_FOR_RUNTIME / ACTIVE_FOR_PRODUCTION | APPROVE |
| M4 | eval-only harness 是否可在 CD-07 之前进入工程基线 | APPROVE |
| M5 | 当前权威 status/readiness 文档是否已与 Gate C PASS 同步 | **REVISE_REQUIRED** |
| M6 | 是否仍要求 Repository Owner 显式 Merge Authorization | APPROVE / STILL_REQUIRED |

```text
blocking finding = BF-MAR-01
```

---

## 2. M1–M4 Positive Findings

PR #89 变更仍限定于：

```text
isolated tools/u03_gatec_eval harness
manual workflow_dispatch evaluation workflow
independent review / remediation / evidence / Gate C governance records
```

未发现把评估 harness 接入 runtime classpath、StateCommitter、U04/U14、real patient traffic 或 production activation。

Gate C Re-Decision / Post-Gate-C 文件也继续保持：

```text
candidate != published
Gate C PASS != runtime authorization
Gate C PASS != production authorization
CD-07 = NOT_READY
U04 = NOT_READY
```

因此：

```text
M1 = APPROVE
M2 = APPROVE
M3 = APPROVE
M4 = APPROVE
```

允许 eval-only harness 在 CD-07 之前进入工程基线的理由，是其已完成独立实现审查、正式 governed evaluation、durable evidence freeze，并保持与 runtime/production 物理隔离。该接受不改变 clinical release candidate 状态。

---

## 3. BF-MAR-01 — Authoritative Status Documents Are Stale

当前 PR HEAD 上至少两份 `docs/current` 权威状态文件仍与已冻结事实冲突。

### 3.1 `U03_Clinical_Dependency_Readiness.md`

仍声明：

```text
Gate C = NOT_PASSED
BF-CD06-EXEC-01 = OPEN
Evaluation Execution = BLOCKED_BEFORE_START
AUTH-U03-GATEC-EVAL-IMPL-001 = NOT_AUTHORIZED
CD-07 Runtime Implementation Readiness = BLOCKED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

其中前四项已经被后续正式治理事实取代：

```text
AUTH-U03-GATEC-EVAL-IMPL-001 = GRANTED / EXECUTED_WITHIN_EVALUATION_SCOPE
BF-CD06-EXEC-01 = CLOSED_BY_EVALUATION_HARNESS_PATH
Governed Evaluation = COMPLETED / PASS
Gate C = PASS
```

但这不意味着 CD-07 READY。新的正确后续状态应保持：

```text
CD-07 Implementation Readiness = NOT_READY
CD-07 Implementation Authorization = NOT_GRANTED
```

### 3.2 `U03_Clinical_Content_Governance_Status.md`

仍声明：

```text
Gate C = NOT_PASSED
Evaluation Execution = NOT_STARTED
CD-07 Implementation Readiness = BLOCKED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

应同步到当前事实：

```text
Gate C = PASS
Governed Evaluation Evidence = FROZEN / VERIFIED
CD-07 Implementation Readiness = NOT_READY
U04 Implementation Readiness = NOT_READY
U04 blocker = BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
```

且必须继续保留：

```text
candidate releases = NOT_PUBLISHED / NOT_ACTIVE_FOR_RUNTIME
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

### 3.3 Why Blocking

这是 status consistency blocker，不是 clinical blocker：

```text
Gate C PASS remains valid
BF-GATEC-EVIDENCE-01 remains CLOSED
clinical results are not reopened
```

但 `docs/current` 的权威状态不能在同一合入基线中同时给出互相冲突的当前结论。因此：

```text
BF-MAR-01
= STALE_AUTHORITATIVE_STATUS_DOCUMENTS
= BLOCKING_FOR_MERGE_AUTHORIZATION
```

---

## 4. Required Remediation

只允许纯 status sync，不允许借此扩大实现范围。

必须更新：

```text
docs/current/06_开发单元/U03_Clinical_Dependency_Readiness.md
docs/current/06_开发单元/U03_Clinical_Content_Governance_Status.md
```

同步要求：

```text
Gate C = PASS
Governed Evaluation = COMPLETED / PASS
Evidence = FROZEN / VERIFIED
BF-CD06-EXEC-01 = CLOSED
BF-GATEC-EVIDENCE-01 = CLOSED
Evaluation-only authorization/history = accurately recorded

CD-07 = NOT_READY / NOT_AUTHORIZED
U04 = NOT_READY / BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
candidate releases remain NOT_PUBLISHED / NOT_ACTIVE
Runtime / Production remain BLOCKED
```

禁止在 status sync 中：

```text
新增 clinical rule / threshold / fixture / expected outcome
修改 evaluator / cases / workflow execution semantics
把 candidate 标成 runtime/production active
授权 CD-07 / U04 / merge / production
```

完成后只需 targeted merge-authorization re-review，重点关闭 BF-MAR-01；无需重跑 Gate C evaluation。

---

## 5. M6 / Explicit Authorization Boundary

即使 BF-MAR-01 关闭：

```text
Targeted Merge Re-Review PASS
!= Merge Authorization
```

仍必须由 Repository Owner 给出显式：

```text
Merge Authorization = GRANTED
```

之后才允许 standard merge commit。继续禁止 auto-merge / squash / rebase。

---

## 6. Decision

```text
M1 = APPROVE
M2 = APPROVE
M3 = APPROVE
M4 = APPROVE
M5 = REVISE_REQUIRED
M6 = APPROVE / EXPLICIT AUTHORIZATION STILL REQUIRED

BF-MAR-01 = OPEN
PR #89 Merge Authorization Review = REVISE_REQUIRED
Merge Authorization = NOT_GRANTED
PR #89 = KEEP_DRAFT

Gate C = PASS / NOT_REOPENED
CD-07 = NOT_READY
U04 = NOT_READY
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

Next allowed step:

```text
status-only sync of the two stale authoritative documents
→ targeted Merge Authorization Re-Review
→ if PASS, request explicit Repository Owner Merge Authorization
```
