# U03 C Rule Pack Formal Review Record v0.1

> 审核对象：`U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.1.md`  
> Rule Release：`RR-U03-RISK-001@0.1.0-draft`  
> Knowledge Release：`KR-U03-SOURCE-001@0.1.0-candidate`  
> 状态：`REVIEW_NOT_STARTED / MEDICAL_OWNER_REVIEW_REQUIRED / TECHNICAL_REVIEW_REQUIRED / NOT_APPROVED / NOT_FROZEN`  
> 本记录只用于 C 内容审核；不授权 D09、runtime 或 production。

## 1. Review Preconditions

```text
Gate A = PASS
B approved evidence refs = AVAILABLE
KR-U03-SOURCE-001@0.1.0-candidate = RESOLVABLE_FOR_C_DRAFTING
C Structural Schema = FROZEN
C Content Draft v0.1 = AVAILABLE
```

## 2. Rule Inventory

| Rule ID | Evidence Ref | Source Ref | Medical Verdict | Technical Verdict |
|---|---|---|---|---|
| C-RULE-RESP-001 | EV-RF-RESP-001 | SRC-NHS-DYSPNOEA | PENDING | PENDING |
| C-RULE-NEURO-001 | EV-MNM-NEURO-001 | SRC-NICE-NEURO-NG127 + SRC-NHS-STROKE | PENDING | PENDING |
| C-RULE-NEURO-002 | EV-MNM-NEURO-002 | SRC-NICE-NEURO-NG127 + SRC-NHS-STROKE | PENDING | PENDING |
| C-RULE-CARD-001 | EV-MNM-CARD-001 | SRC-NHS-CHEST | PENDING | PENDING |
| C-RULE-ALLERGY-001 | EV-RF-ALLERGY-001 | SRC-NICE-ANAPHYLAXIS-NG258 | PENDING | PENDING |
| C-RULE-DYSPNOEA-APPEAR-001 | EV-RF-APPEAR-001 | SRC-NHS-DYSPNOEA | PENDING | PENDING |
| C-RULE-DYSPNOEA-CONFUSION-001 | EV-RF-NEURO-001 | SRC-NHS-DYSPNOEA | PENDING | PENDING |
| C-RULE-SEPSIS-RR-HIGH-001 | EV-VS-SEPSIS-001 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |
| C-RULE-SEPSIS-RR-MODHIGH-001 | EV-VS-SEPSIS-001 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |
| C-RULE-SEPSIS-SBP-HIGH-001 | EV-VS-SEPSIS-002 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |
| C-RULE-SEPSIS-SBP-MODHIGH-001 | EV-VS-SEPSIS-002 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |
| C-RULE-SEPSIS-HR-HIGH-001 | EV-VS-SEPSIS-003 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |
| C-RULE-SEPSIS-HR-MODHIGH-001 | EV-VS-SEPSIS-003 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |
| C-RULE-SEPSIS-MENTAL-HIGH-001 | EV-RF-NEURO-001 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |
| C-RULE-SEPSIS-APPEAR-HIGH-001 | EV-RF-APPEAR-001 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |
| C-RULE-SEPSIS-RASH-HIGH-001 | EV-RF-SEPSIS-001 | SRC-NICE-SEPSIS-NG253 | PENDING | PENDING |

## 3. Medical Review Questions

每条 rule 至少确认：

```text
1. required_evidence_ref 是否属于 Gate A 已批准 B entry
2. source_ref / KR ref 是否正确
3. predicate / threshold 是否忠实于 source
4. population / setting / clinical-context scope 是否未扩大
5. missing / unknown / ambiguous 是否不会被当成 negative
6. rule signal 是否只表达 rule-level signal
7. 是否存在病因先验、重复 truth source 或不应由 C 拥有的 disposition
8. 是否需要 APPROVE / REVISE / REJECT / NEED_MORE_SOURCE
```

特别检查：

- NG253 数值规则必须锁在 `age >= 16 + suspected sepsis + source-supported community/custodial context`；
- pregnancy/recent-pregnancy 不属于本 slice；
- NHS dyspnoea 的 appearance/confusion 不能被提升成任意急症的全局 rule；
- NHS patient-information 来源不得被解释成中国生产规则；
- C 不得新增 B 中不存在的 evidence taxonomy。

## 4. Technical Review Questions

每条 rule 至少确认：

```text
1. rule_id / release ref 唯一且稳定
2. predicate 可结构化执行，不依赖自由文本 Prompt
3. required / optional evidence refs 可解析
4. missingness / uncertainty execution state 可表达
5. scope / version / currentness 可绑定
6. KR-U03-SOURCE-001@0.1.0-candidate ref 可解析
7. rule output 不直接映射 D09 disposition
8. no first-hit-wins / no file-order priority
```

## 5. Threshold-specific Review

当前 NG253 候选阈值：

```text
respiratory rate high: >= 25 / min
respiratory rate moderate-high: 21..24 / min
systolic BP high: <= 90 mmHg OR > 40 mmHg below usual
systolic BP moderate-high: 91..100 mmHg
heart rate high: > 130 / min
heart rate moderate-high: 91..130 / min
```

这些值必须逐条由 Medical Owner 对 `SRC-NICE-SEPSIS-NG253` 做审核确认。当前写入 draft 不等于医学批准。

## 6. Review Outcome

当前：

```text
Medical Owner Review = NOT_STARTED
Technical Review = NOT_STARTED
Rule Inventory Approval = NOT_COMPLETE
Rule Signal Vocabulary Approval = NOT_COMPLETE
Initial Rule Release Freeze = NOT_COMPLETE
CD-03 = NOT_PASSED
D drafting = BLOCKED
```

只有 Medical Owner 与 Technical Review 均完成，且所有 blocking REVISE / REJECT / NEED_MORE_SOURCE 已处理后，才能重新评估：

```text
RR-U03-RISK-001@<candidate-version>
= FROZEN_CANDIDATE ?
```

在此之前不得开始真实 D09 branch，不得将本 rule release 用于 runtime/production binding。
