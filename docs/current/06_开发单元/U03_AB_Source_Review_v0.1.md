# U03 A/B Source Review v0.1

> 角色：对 Clinical Input Package A/B 第一版真实医学内容做来源一致性审查。
> 状态：`SOURCE_REVIEW_COMPLETE / MEDICAL_OWNER_REVIEW_PENDING / NOT_APPROVED`
> 注意：本文件不是医学审批，也不构成 production release。

## 1. Review 方法

本轮只回答三个问题：
1. 候选内容是否被列出的权威来源直接或合理支持；
2. 是否保持了来源的人群/场景限制；
3. 是否误把 Evidence 提升成 Rule / D09 Decision / Safety Gate。

## 2. A / Clinical Risk Semantics review

| Candidate | Review | 结论 |
|---|---|---|
| A-RS-01 严重呼吸受损 | PASS_FOR_MEDICAL_REVIEW | NHS 明确把 severe difficulty breathing、gasping/choking、不能正常说话列为立即急诊信号；草案没有把它直接等同诊断或 D09 结论 |
| A-RS-02 新发意识/异常肤色 | PASS_WITH_SCOPE_NOTE | NHS 与 NICE sepsis 均支持；其中 NICE sepsis 部分只能用于疑似感染上下文，草案已保留这一限制 |
| A-RS-03 急性局灶神经缺损 | PASS_FOR_MEDICAL_REVIEW | NICE NG127 与 NHS Stroke 均支持突发无力/麻木、言语异常需要立即按血管事件处理 |
| A-RS-04 急性缺血性胸痛样表现 | PASS_FOR_MEDICAL_REVIEW | NHS Chest pain/Heart attack 支持压迫/紧缩、放射及伴随症状；草案保持“需排除”而非“已确诊” |
| A-RS-05 快速过敏反应伴 ABC 受损 | PASS_FOR_MEDICAL_REVIEW | NICE NG258 明确支持快速发生且涉及 airway/breathing/circulation 的严重系统性反应；草案未要求皮肤表现必须存在 |
| A-RS-06 疑似感染背景下成人脓毒症生理信号 | PASS_WITH_STRICT_SCOPE | NICE NG253 支持；只能在疑似感染、16岁及以上、对应场景使用，不能变成全局生命体征规则 |

A 总结：

```text
6/6 = SOURCE_SUPPORTED_FOR_MEDICAL_REVIEW
0 = APPROVED
```

## 3. B / Evidence Catalog review

| evidence_id | Review | 主要审核意见 |
|---|---|---|
| EV-RF-RESP-001 | PASS_FOR_MEDICAL_REVIEW | 来源直接支持；需医学 Owner 决定是否拆分“不能说话”和“gasping/choking” |
| EV-RF-NEURO-001 | PASS_WITH_SCOPE_NOTE | 作为 sepsis signal 时需感染上下文；作为通用 acute mental-status red flag 需要额外跨病种来源后再扩大 scope |
| EV-RF-CIRC-001 | PASS_WITH_LIMITATION | 来源支持，但远程观察受肤色/光线影响；negative observation 不能作为可靠排除依据 |
| EV-MNM-NEURO-001 | PASS_FOR_MEDICAL_REVIEW | NICE/NHS 直接支持突发局灶无力/麻木与卒中/TIA 关联 |
| EV-MNM-NEURO-002 | PASS_FOR_MEDICAL_REVIEW | NICE/NHS 直接支持突发言语/语言异常；症状缓解仍需保留历史阳性 |
| EV-MNM-CARD-001 | PASS_FOR_MEDICAL_REVIEW | NHS 直接支持胸痛模式及伴随症状；不能提升为确诊 ACS/MI |
| EV-RF-ALLERGY-001 | PASS_FOR_MEDICAL_REVIEW | NICE NG258 支持快速 ABC compromise；诊断阈值仍需医学 Owner 确认 |
| EV-VS-SEPSIS-001 | PASS_WITH_STRICT_SCOPE | 具体阈值应进入 C Rule Pack；B 只登记“疑似感染背景下异常呼吸频率信号” |
| EV-VS-SEPSIS-002 | PASS_WITH_STRICT_SCOPE | 同上；相对基线血压下降需要可靠 baseline |
| EV-VS-SEPSIS-003 | PASS_WITH_STRICT_SCOPE | 同上；仅疑似感染上下文使用 |
| EV-RF-SEPSIS-001 | PASS_WITH_REMOTE_LIMITATION | NICE 支持；远程皮疹判断质量需要明确 limitation |

B 总结：

```text
11/11 = SOURCE_SUPPORTED_FOR_MEDICAL_REVIEW
0 = APPROVED
3 vital-sign entries require exact thresholds to remain in C Rule Pack rather than B Catalog
```

## 4. 发现的边界问题

### R1 — 通用 altered mental state scope 仍不足

当前跨病种的“新发意识改变”有 NHS emergency support，但 B 中如果要把它升级成全局 RED_FLAG，建议再补一个更直接的通用急性病/急诊来源，而不是主要依赖 sepsis guideline。

状态：`SOURCE_EXPANSION_RECOMMENDED`

### R2 — 成人来源不能覆盖儿科和妊娠

当前 v0.1 明确没有覆盖儿科、妊娠/近期妊娠。后续必须独立建 source pack，不得复用成人阈值。

状态：`CORRECTLY_EXCLUDED`

### R3 — Evidence Catalog 不应承载可执行阈值

具体生命体征阈值属于 C / Rule Pack；B 只定义证据概念、所需输入与适用上下文。

状态：`BOUNDARY_CONFIRMED`

### R4 — 地区本地化尚未完成

NICE/NHS 来源适合做初始临床安全草案，但最终若系统面向中国大陆或其他地区，需医学 Owner 决定是否补充/替换为当地指南、急救流程或共识。

状态：`LOCALIZATION_PENDING`

## 5. 当前审核结论

```text
A Source Review = PASS_FOR_MEDICAL_OWNER_REVIEW
B Source Review = PASS_FOR_MEDICAL_OWNER_REVIEW
Medical Owner Approval = NOT_STARTED / NOT_AVAILABLE
Production Eligibility = NO
```

下一步不应直接开发 C02/D09，而应：
1. 对 A/B 做医学 Owner 审核；
2. 补齐 R1 的跨病种意识改变来源（可选但建议）；
3. 确定目标地区与人群 scope；
4. 仅将医学审核通过的 B entries 送入 C Rule Pack 内容设计。
