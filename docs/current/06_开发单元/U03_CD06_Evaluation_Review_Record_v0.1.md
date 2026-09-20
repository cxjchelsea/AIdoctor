# U03 CD-06 Evaluation Review Record v0.1

> 对象：Gate C / CD-06 evaluation package 的 Medical + Policy/Eval 审核记录。  
> 审核角色：`U03_MEDICAL_OWNER_REVIEW` + `U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-16`  
> 对照基线：`c0aadea`  
> 状态：`REVIEW_COMPLETE / REVISE_REQUIRED / NOT_REVIEW_READY / NOT_GATE_C / NOT_FOR_PRODUCTION`。

---

## 1. Review Inputs

```text
U03_Gate_C_Readiness_Decomposition_v0.1.md
U03_Clinical_Risk_EvalSet_Coverage_Manifest_Draft_v0.1.md
U03_Clinical_Risk_Golden_Cases_Draft_v0.1.md
U03_Clinical_Risk_Safety_Suite_Draft_v0.1.md
U03_Clinical_Risk_EvalSet_Release_Candidate_v0.1.md
```

受测 governed set：

```text
E = KR-U03-SOURCE-001@0.1.0-candidate
C = RR-U03-RISK-001@0.2.1-candidate
Coverage = U03_D09_COVERAGE_V0_2_1_CANDIDATE
D = PR-U03-D09-001@0.2.1-candidate
```

---

## 2. Package Review Matrix

| Object | Medical | Policy/Eval |
|---|---|---|
| Coverage Manifest | APPROVE | REVISE |
| Golden Cases GC-001..GC-028 | APPROVE | REVISE |
| Safety Suite SS-001..SS-020 | APPROVE | APPROVE |
| EvalSet Release Candidate | APPROVE | REVISE |

```text
blocking review finding = 2
CD-06 = NOT_REVIEW_READY
ER-U03-RISK-001@0.1.0-candidate = NOT_READY
Gate C = NOT_PASSED
```

---

## 3. Mandatory Review Questions

### Medical Owner

| ID | Question | Verdict |
|---|---|---|
| M-EVAL-01 | 28 个 golden case 的 expected clinical path 是否忠实于现行批准语义 | APPROVE |
| M-EVAL-02 | expected disposition / FAILED semantics 是否与已批准 D policy 一致 | APPROVE |
| M-EVAL-03 | negative assertions 是否避免错误安全结论 | APPROVE |
| M-EVAL-04 | scope TRUE / FALSE / UNKNOWN family 是否正确 | APPROVE |
| M-EVAL-05 | NO_HIGH_RISK_SIGNAL case 是否没有被误写成 SAFE / NORMAL | APPROVE |
| M-EVAL-06 | Safety Suite 20 条是否均应作为 critical blocking | APPROVE |

### Policy / Evaluation Owner

| ID | Question | Verdict |
|---|---|---|
| E-EVAL-01 | current immutable release refs 是否完整 | APPROVE |
| E-EVAL-02 | 15 C rules 是否有代表性 Gate C coverage | REVISE |
| E-EVAL-03 | D P0-P5 是否全部覆盖 | APPROVE |
| E-EVAL-04 | missingness / scope / conflict / version / release mismatch 是否覆盖 | APPROVE |
| E-EVAL-05 | idempotency / direct-commit prohibition 是否覆盖 | APPROVE |
| E-EVAL-06 | expected result / reason code 是否与 frozen D09 一致 | APPROVE |
| E-EVAL-07 | coverage manifest 是否能逐项追到正确 case id | REVISE |
| E-EVAL-08 | implementation owner 与 expected outcome owner 是否分离 | APPROVE |

---

## 4. Case Review Tables

### Golden Cases

| ID | Medical | Policy/Eval | Note |
|---|---|---|---|
| GC-001 | APPROVE | APPROVE | RESP CRITICAL_RED_FLAG → HIGH_RISK |
| GC-002 | APPROVE | APPROVE | NEURO-001 MUST_NOT_MISS → HIGH_RISK |
| GC-003 | APPROVE | APPROVE | NEURO-002 MUST_NOT_MISS → HIGH_RISK |
| GC-004 | APPROVE | APPROVE | CARD MUST_NOT_MISS → HIGH_RISK；不得写成确诊 ACS |
| GC-005 | APPROVE | APPROVE | ALLERGY CRITICAL_RED_FLAG → HIGH_RISK |
| GC-006 | APPROVE | APPROVE | dyspnoea context 独立成立后 appearance HIGH |
| GC-007 | APPROVE | APPROVE | confusion 不得全局化 |
| GC-008 | APPROVE | APPROVE | RR 不得反建 suspected_sepsis |
| GC-009 | APPROVE | APPROVE | RR MODHIGH only → CAUTION |
| GC-010 | APPROVE | APPROVE | SBP HIGH 不得因 usual 未知降级 |
| GC-011 | APPROVE | APPROVE | SBP MODHIGH only → CAUTION |
| GC-012 | APPROVE | APPROVE | coverage-complete NO_HIGH；不得写成 SAFE |
| GC-013 | APPROVE | APPROVE | HR MODHIGH only → CAUTION |
| GC-014 | APPROVE | APPROVE | 专用 sepsis SCOPE_MISMATCH → P4，不是 APPEAR HIGH |
| GC-015 | APPROVE | APPROVE | 专用 dyspnoea SCOPE_MISMATCH → P4，不是 RASH HIGH |
| GC-016 | APPROVE | APPROVE | HIGH + insufficiency 保持 HIGH |
| GC-017 | APPROVE | APPROVE | UNKNOWN → P2 FAILED |
| GC-018 | APPROVE | APPROVE | UNMEASURED → P2 FAILED |
| GC-019 | APPROVE | APPROVE | REMOTE_NOT_OBSERVED → P2 FAILED |
| GC-020 | APPROVE | APPROVE | pregnancy TRUE → P0 MISMATCH |
| GC-021 | APPROVE | APPROVE | UNKNOWN → P0 NOT_ESTABLISHED |
| GC-022 | APPROVE | APPROVE | NOT_ASKED → P0 NOT_ESTABLISHED |
| GC-023 | APPROVE | APPROVE | NOT_ESTABLISHED → P0；不得写成 family mismatch |
| GC-024 | APPROVE | APPROVE | stale + HIGH 仍 P0 |
| GC-025 | APPROVE | APPROVE | release mismatch；禁 latest |
| GC-026 | APPROVE | APPROVE | P5 UNRESOLVABLE_CONFLICT |
| GC-027 | APPROVE | APPROVE | CAUTION + insufficiency → P2 |
| GC-028 | APPROVE | APPROVE | 幂等回放不得二次 commit |

```text
28 / 28 Medical = APPROVE
28 / 28 purpose-level Policy/Eval = APPROVE
pack-level Policy/Eval = REVISE
```

### Safety Suite

| ID | Medical | Policy/Eval |
|---|---|---|
| SS-001..SS-020 | APPROVE | APPROVE |

```text
20 / 20 Medical = APPROVE
20 / 20 Policy/Eval = APPROVE
criticality = CRITICAL_BLOCKING = APPROVE
```

SS-001..SS-020 作为 critical blocking 成立：任一条 FAIL 不得被通过率掩盖。

---

## 5. Blocking Findings

```text
BF-CD06-01
Coverage Manifest 把
  C-RULE-SEPSIS-APPEAR-HIGH-001 → GC-014
  C-RULE-SEPSIS-RASH-HIGH-001 → GC-015
写错。
GC-014 / GC-015 实际是 specialized-family SCOPE_MISMATCH 的 P4 例，
不是 appearance / rash HIGH 阳性路径。
同时缺少独立的
  HR HIGH
  SEPSIS APPEAR HIGH
  SEPSIS RASH HIGH
代表性 Gate C case。
因此“15 条 C rule 均有代表性覆盖”不成立。

BF-CD06-02
Golden Case pack 仍是 purpose table，尚未按
U03_Risk_EvalSet_Safety_Suite_Schema
与 Gate C decomposition §5 补齐：
  case_version
  clinical_state_fixture_ref
  input_fact_refs[]
  expected_evidence_refs[]
  expected_rule_refs[]
  source_refs[] / rationale_ref / provenance_refs[]
  must_not_commit[]
pack 自己第 3 节也写明这些字段“正式 review 版必须继续填写”。
没有 fixture 的 case 不能进入 CD-06 REVIEW_READY 或 evaluation execution。
```

非阻塞备注：

- GC-016 覆盖 HIGH+insufficiency，不是“两条 HIGH 并存”；manifest 的 multi-hit HIGH 行应改绑或补 case。
- region / channel / language mismatch、AMBIGUOUS / CONFLICTING evidence、DEPENDENCY_FAILURE 尚未各有独立 Gate C case；可在 v0.2 补，不单独构成第三条 blocker。
- Safety Suite 20 条语义成立；执行前仍需与 GC 同一套 fixture binding，但不另开 blocker。

---

## 6. CD-06 Closure Rule

未同时满足 APPROVE / APPROVE 与 blocking finding = 0。

因此：

```text
CD-06 = NOT_REVIEW_READY
ER-U03-RISK-001@0.1.0-candidate = NOT_READY
Evaluation execution = NOT_STARTED
Gate C = NOT_PASSED
```

---

## 7. Current Status

```text
Evaluation Package = REVIEWED
Golden Cases = 28 CANDIDATES / PURPOSE_APPROVED / SCHEMA_INCOMPLETE
Safety Cases = 20 CANDIDATES / APPROVED_FOR_CONTENT
Medical Review = COMPLETE
Policy/Eval Review = COMPLETE / REVISE_REQUIRED
blocking review finding = BF-CD06-01, BF-CD06-02
CD-06 = NOT_REVIEW_READY
EvalSet Release = REVIEW_PENDING / NOT_READY
Gate C = NOT_PASSED
CD-07 = BLOCKED
```

下一步只修订 Manifest 与 Golden Case pack，关闭 BF-CD06-01 / BF-CD06-02 后再审。
不开始 governed evaluation execution，不宣称 Gate C PASS。
