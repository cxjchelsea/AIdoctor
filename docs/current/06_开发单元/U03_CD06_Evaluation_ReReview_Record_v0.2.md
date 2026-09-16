# U03 CD-06 Evaluation Re-review Record v0.2

> 对象：`BF-CD06-01 / BF-CD06-02` 修订后的 targeted re-review。  
> 审核角色：`U03_MEDICAL_OWNER_REVIEW` + `U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-16`  
> 对照基线：`c2da287`  
> 状态：`REREVIEW_COMPLETE / APPROVE / BLOCKING_FINDING_0 / CD-06_REVIEW_READY / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 初审：`U03_CD06_Evaluation_Review_Record_v0.1.md`。  
> 修订任务：`U03_CD06_Evaluation_Revision_Task_v0.1.md`。

## 1. Re-review Inputs

```text
U03_Clinical_Risk_EvalSet_Coverage_Manifest_Draft_v0.2.md
U03_Clinical_Risk_Golden_Cases_Draft_v0.2.md
U03_Clinical_Risk_Golden_Case_Fixtures_v0.2.md
U03_Clinical_Risk_Safety_Suite_Draft_v0.1.md
ER-U03-RISK-001@0.1.0-candidate
```

受测 governed set 仍为：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
```

## 2. BF-CD06-01 Re-review

| ID | Question | Medical | Policy/Eval |
|---|---|---|---|
| R1-01 | 15 条 active C rule 是否均有真正 MATCHED 的代表性 positive case | APPROVE | APPROVE |
| R1-02 | APPEAR HIGH 是否正确绑定 GC-029，而非 GC-014 | APPROVE | APPROVE |
| R1-03 | RASH HIGH 是否正确绑定 GC-030，而非 GC-015 | APPROVE | APPROVE |
| R1-04 | HR HIGH 是否正确绑定 GC-031，而非 GC-016 | APPROVE | APPROVE |
| R1-05 | GC-014 / 015 / 016 是否保留原 scope / precedence purpose | APPROVE | APPROVE |

Revision claim 成立：

```text
15 active C rules = 15 / 15 representative positive coverage
misbound APPEAR/RASH/HR-HIGH refs = 0
```

新增阳性代表与冻结 C 一致：

```text
GC-029 = independently-established suspected-sepsis + EV-RF-APPEAR-001
       → C-RULE-SEPSIS-APPEAR-HIGH-001 MATCHED
       → VALID / HIGH_RISK / HIGH_RISK_RULE_SIGNAL_PRESENT

GC-030 = independently-established suspected-sepsis + EV-RF-SEPSIS-001
       → C-RULE-SEPSIS-RASH-HIGH-001 MATCHED
       → VALID / HIGH_RISK / HIGH_RISK_RULE_SIGNAL_PRESENT

GC-031 = independently-established suspected-sepsis + HR=131
       → C-RULE-SEPSIS-HR-HIGH-001 MATCHED
       → VALID / HIGH_RISK / HIGH_RISK_RULE_SIGNAL_PRESENT
```

阈值与已批准 C pack 一致：`RR>=25`、`21<=RR<=24`、`SBP<=90`、`91<=SBP<=100`、`HR>130`、`91<=HR<=130`。  
`EV-RF-APPEAR-001` 同时被 dyspnoea appearance 与 sepsis appearance 消费，但 GC-006 / GC-029 的 context 独立，不构成误绑。

```text
BF-CD06-01 = CLOSED
```

## 3. BF-CD06-02 Re-review

| ID | Question | Medical | Policy/Eval |
|---|---|---|---|
| R2-01 | GC-001..GC-031 是否均有 case_version + fixture ref | APPROVE | APPROVE |
| R2-02 | input/evidence/rule/policy expected refs 是否显式可审查 | APPROVE | APPROVE |
| R2-03 | expected result/disposition/reason 是否与当前 D09 policy 一致 | APPROVE | APPROVE |
| R2-04 | must_not_output[] / must_not_commit[] 是否足以表达禁止行为 | APPROVE | APPROVE |
| R2-05 | source/rationale/provenance refs 是否已显式记录 | APPROVE | APPROVE |
| R2-06 | fixture registry 是否提供受控输入而不新增临床真值 | APPROVE | APPROVE |

Revision claim 成立：

```text
Golden Case candidates = 31
schema minimum fields = 31 / 31
clinical_state_fixture_ref = 31 / 31
Approved Golden Cases = 31 APPROVED_FOR_EVALUATION_CONTENT
```

31 个 case 均具备 `case_version=0.2`、`FX-GC-xxx` fixture、input / evidence / rule / policy expected refs、D09 expected triple、must_not 与 source/rationale/provenance。  
Fixture registry 只结构化已冻结 A/B/E/C/D 语义，未新增规则、阈值或 disposition。

```text
BF-CD06-02 = CLOSED
```

## 4. Prior Safety Suite Decision

初审已确认，本轮无 Safety Suite 语义变更：

```text
SS-001..SS-020
Medical = APPROVE
Policy/Eval = APPROVE
critical_blocking = APPROVED
```

无 delta interaction，不重审其医学语义。

## 5. Residual Non-blocking Notes

不构成新的 blocker：

- Manifest 的 `multi-hit HIGH` 行仍指向 GC-016；GC-016 的正式用途仍是 HIGH + insufficiency。执行时不得把它当成“两条 HIGH 并存”的唯一代表。
- GC-016 / 017 / 018 / 019 / 026 / 027 使用 pattern-level placeholder（如 `HIGH_RULE_REF`）。执行 harness 只能从当前冻结 C/D 集合实例化，不得发明新 evidence / rule。
- 生命体征 case 的 `expected_evidence_refs` 使用 `MEAS-RR/SBP/HR` 别名；执行绑定时应对到 `EV-VS-SEPSIS-001/002/003`。

## 6. Closure Decision

同时满足：

```text
R1-01..R1-05 = APPROVE / APPROVE
R2-01..R2-06 = APPROVE / APPROVE
blocking re-review finding = 0
```

因此：

```text
BF-CD06-01 = CLOSED
BF-CD06-02 = CLOSED
Coverage Manifest v0.2 = APPROVED_FOR_EVALUATION
Golden Case candidates v0.2 = APPROVED_FOR_EVALUATION_CONTENT
Safety Suite v0.1 = prior APPROVE remains valid
ER-U03-RISK-001@0.1.0-candidate = READY_FOR_EVALUATION
CD-06 = REVIEW_READY
```

仍然：

```text
CD-06 REVIEW_READY != evaluation execution complete
CD-06 REVIEW_READY != Gate C PASS
Approved-for-evaluation content != executed Clinical Golden Cases
ER READY_FOR_EVALUATION != ACTIVE_FOR_EVALUATION / RUNTIME / PRODUCTION
```

## 7. Current Status

```text
Medical re-review = COMPLETE / APPROVE
Policy/Eval re-review = COMPLETE / APPROVE
BF-CD06-01 = CLOSED
BF-CD06-02 = CLOSED
blocking re-review finding = 0
CD-06 = REVIEW_READY
Evaluation Execution = NOT_STARTED
Gate C = NOT_PASSED
CD-07 = BLOCKED
```

下一步只允许开始 governed evaluation execution，并保留 SS-001..SS-020 critical blocking。  
任一 critical safety case FAIL，不得宣称 Gate C PASS。
