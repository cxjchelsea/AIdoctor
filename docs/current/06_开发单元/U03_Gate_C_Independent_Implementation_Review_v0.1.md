# U03 Gate C Independent Implementation Review v0.1

> 对象：PR #89 / `impl/u03-gatec-eval-only` / `tools/u03_gatec_eval`  
> 对照 HEAD：`3e150b8`  
> 审核角色：`U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-16`  
> 状态：`REVIEW_COMPLETE / REVISE_REQUIRED / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 授权范围：`AUTH-U03-GATEC-EVAL-IMPL-001` evaluation-only。

```text
Independent Review PASS != Gate C PASS
local 31/31 + 20/20 != Independent Review PASS
Independent Review PASS != governed evaluation evidence frozen
```

---

## 1. Review Inputs

```text
PR #89 DRAFT
tools/u03_gatec_eval/evaluator.py
tools/u03_gatec_eval/cases.py
tools/u03_gatec_eval/run_evaluation.py
tools/u03_gatec_eval/test_evaluator.py
tools/u03_gatec_eval/README.md
.github/workflows/u03-gatec-eval.yml
U03_Gate_C_Semantic_Mapping_Review_v0.1.md
ER-U03-RISK-001@0.1.0-candidate / GC-001..031 / SS-001..020
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
```

独立复跑（审查机，非作者宣称）：

```text
python -m unittest -v
= 5 / 5 PASS
```

该复跑只证明 harness 自洽，不构成 Independent Review PASS，也不构成 Gate C PASS。

---

## 2. Mandatory Questions

| ID | Question | Verdict |
|---|---|---|
| IR-01 | 实现是否隔离在 `tools/u03_gatec_eval`，无 Runtime / State Committer / U04 接线 | APPROVE |
| IR-02 | release refs 是否精确绑定现行 0.2.1 集合，并拒绝 `latest` | APPROVE |
| IR-03 | 非 P0、非 stub 路径是否真正执行全部 15 条 C rule | APPROVE |
| IR-04 | D09 P0–P5 优先级在诚实 C 结果上是否正确 | APPROVE |
| IR-05 | UNKNOWN / UNMEASURED / NOT_ASKED / REMOTE_NOT_OBSERVED 是否 fail-closed | APPROVE |
| IR-06 | Golden Case / Safety Suite 是否忠实实例化已批准 fixture 语义 | REVISE |
| IR-07 | fixture / evaluator 是否存在“自己验证自己” | REVISE |
| IR-08 | 正式受治理 evaluation execution 是否已固化 | NOT_STARTED |

---

## 3. What is sound

隔离：只依赖标准库；无 `diagnosis-service`、无网络、无 DB、无 Clinical State commit、无 U04/U14。`commit_attempted` / `u04_decision` 在代码路径上不可变为有效副作用。CI 需显式 `REVIEW_COMPLETE` 才跑 governed execution，当前未开。

精确绑定：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
PF-U03-C-POLICY-001
```

`latest` 与错误 version 进入 `RELEASE_MISMATCH`。

诚实路径上的 C/D 语义：

```text
GC-001 实际执行 15 条 rule，P1 HIGH
RR>=25 / 21..24 / SBP<=90 / 91..100 / HR>130 / 91..130 与冻结阈值一致
SBP<=90 不因 usual UNKNOWN 降级
HIGH + applicable insufficiency → P1
CAUTION + applicable insufficiency → P2
pregnancy TRUE / UNKNOWN family → P0，且不进入 C
stale / release mismatch → P0
NO_HIGH_RISK_SIGNAL 未膨胀为 SAFE/NORMAL
```

这些路径可以进入后续修复后的正式 execution，不需要改冻结 C/D/E。

---

## 4. Blocking Finding

```text
BF-IR-01
GC-026 / SS-012 通过 fixture.forced_rule_results
直接注入 C 执行结果，绕过 evaluate_rules()。
审查机实测：
  GC-026 n_rules = 2
  forced = True
P5 UNRESOLVABLE_CONFLICT 因此不是“冻结 C 在受控输入上跑出来的结果”，
而是 D09 冲突分支的自注入自检。
这违反 Independent Review 对
  fixture 不得与 evaluator 共用旁路导致自己验证自己
的要求。
```

关闭条件：删除 `forced_rule_results` 旁路。GC-026 / SS-012 必须由真实 C 执行状态进入 P5；若共享 scope 下无法自然产生 `SCOPE_MISMATCH + MATCHED`，应把该 case 标成 `UNPRODUCIBLE_UNDER_SHARED_SCOPE` 并移出可执行 Golden/Safety 计数，而不是 stub C。

---

## 5. Important Findings

```text
BF-IR-02
GC-012 / GC-014 / GC-015 使用同一 p4_fixture()。
审查机实测 fixture 全等。
已批准语义要求：
  GC-012 = 两条件 family 均 NOT_APPLICABLE 的 coverage-complete P4
  GC-014 = sepsis specialized SCOPE_MISMATCH；dyspnoea NOT_APPLICABLE
  GC-015 = dyspnoea specialized SCOPE_MISMATCH；sepsis NOT_APPLICABLE
当前 3 个 ID 只验证了 1 条 P4 路径。正式 execution 不得把 3 次同一 fixture 算成 3 条独立覆盖。
```

```text
N-IR-01
SS-014 / SS-016 只断言 commit_attempted==False / u04_decision is None。
这两项是 dataclass 默认值，测试不能单独证明无副作用。
副作用结论来自本审查对源码与依赖面的静态核验，而不是这两条 case。
```

```text
N-IR-02
test_all_15_frozen_rule_ids_are_implemented 只检查 ALL_RULES 列表长度，
不检查 evaluate() 是否返回 15 条结果。
本审查另行确认 GC-001 / GC-012 返回 15 条。
```

```text
N-IR-03
scope context 被编码为 suspected_sepsis / dyspnoea_context 字符串，
未消费 context_ref / provenance_ref。
当前实现不会用 appearance/RR 反建 context，这点成立。
正式 provenance 对象仍是 residual，不单独构成第三条 blocker。
```

---

## 6. Decision

```text
Isolation = APPROVE
Exact release binding = APPROVE
Honest-path C/D precedence = APPROVE
Missingness fail-closed = APPROVE
GC/SS fidelity = REVISE
Self-validation control = REVISE

Independent Implementation Review = REVISE_REQUIRED
blocking finding = BF-IR-01
important finding = BF-IR-02
BF-CD06-EXEC-01 = OPEN
Governed Evaluation Execution = NOT_STARTED
Gate C = NOT_PASSED
Merge Authorization = NOT_GRANTED
Runtime / U04 / Production = BLOCKED
```

下一步只修复 BF-IR-01 / BF-IR-02，再做 targeted re-review。  
在 Independent Review PASS 之前，不得把 workflow 标成 `REVIEW_COMPLETE`，不得固化正式 Gate C execution evidence，不得宣布 Gate C PASS。
