# U03 A/B Medical Owner Review Record v0.2

> 角色：A/B Content Draft v0.2 的第二轮 Medical Owner Review 正式记录。  
> 审核对象：`U03_Clinical_Risk_Semantics_Content_Draft_v0.2.md`、`U03_Evidence_Catalog_Content_Draft_v0.2.md`。  
> 状态：`SECOND_REVIEW_RECORD_COMPLETE / TWO_TARGETED_REVISIONS_REQUIRED / PACKAGE_NOT_APPROVED / C_CONTENT_BLOCKED / NOT_FOR_PRODUCTION`

本记录只固化第二轮审核裁决，不把修订后的条目自动提升为批准状态。第二轮指出的两项 scope 修订已经落回 v0.2 正文，但仍需 Medical Owner 对修订结果做确认。

---

## 1. 第二轮总裁决

```text
A v0.2 Review = COMPLETE
A APPROVE = 6
A REVISE = 1
A REJECT = 0
A NEED_MORE_SOURCE = 0

B v0.2 Review = COMPLETE
B APPROVE = 10
B REVISE = 1
B REJECT = 0
B NEED_MORE_SOURCE = 0

Medical Owner Approval = NOT_COMPLETE
C Rule Pack Clinical Content = BLOCKED
CD-07 = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

第二轮确认 v0.1 的主要病因先验与 scope 泛化问题已经闭合；残留问题仅为 A-RS-02A 与 EV-RF-NEURO-001 的默认 scope 仍需锁到来源明确支持的上下文。

---

## 2. A v0.2 逐条裁决

| ID | Verdict | 第二轮结论 |
|---|---|---|
| A-RS-01 | APPROVE | 方向保留；远程未观察不得排除已补齐。 |
| A-RS-02A | REVISE | 拆条与去机制名成立；默认 scope 必须明确锁到来源上下文，禁止解释为任何急症或全局 RED_FLAG。 |
| A-RS-02B | APPROVE | 只描述可观察现象；肤色、光线、远程限制已明确。 |
| A-RS-03 | APPROVE | 感觉异常已收窄到突发单侧/局灶；关键时序/侧别字段明确。 |
| A-RS-04 | APPROVE | 病因命名已移除；急性冠脉事件仅为需考虑方向，不等于确诊。 |
| A-RS-05 | APPROVE | ABC 边界与皮肤非必需语义成立。 |
| A-RS-06 | APPROVE | suspected sepsis、16+、来源场景已锁；阈值和组合关系未进入 A。 |

第二轮后，A-RS-02A 已按本裁决进一步收紧为：
- NICE NG253：`age >= 16`、`suspected sepsis`、来源对应 community / custodial 场景；
- NHS Shortness of breath：该来源所描述的严重呼吸困难急诊警示上下文中的突然意识混乱；
- 上述来源不得合并推导出一般急症或跨病种全局 RED_FLAG。

该修订仍需 Medical Owner 确认后，才能把 A-RS-02A 从 REVISE 转为 APPROVE。

---

## 3. B v0.2 逐条裁决

| ID | Verdict | 第二轮结论 |
|---|---|---|
| EV-RF-RESP-001 | APPROVE | 缺失和远程阴性边界已硬化。 |
| EV-RF-NEURO-001 | REVISE | 全局升格已禁止，但“急性/专病上下文”仍过宽；必须写成来源明确上下文。 |
| EV-RF-APPEAR-001 | APPROVE | 中性 ID/命名成立，限制已字段化。 |
| EV-MNM-NEURO-001 | APPROVE | 麻木过宽问题已闭合。 |
| EV-MNM-NEURO-002 | APPROVE | 短暂缓解仍保留历史阳性。 |
| EV-MNM-CARD-001 | APPROVE | 病因命名已移除。 |
| EV-RF-ALLERGY-001 | APPROVE | 边界清楚。 |
| EV-VS-SEPSIS-001 | APPROVE | scope 已锁；Catalog 不保存阈值。 |
| EV-VS-SEPSIS-002 | APPROVE | 同上。 |
| EV-VS-SEPSIS-003 | APPROVE | 同上。 |
| EV-RF-SEPSIS-001 | APPROVE | 名称与疑似脓毒症上下文已对齐。 |

第二轮后，EV-RF-NEURO-001 已按本裁决进一步收紧为：
- NICE NG253：`age >= 16`、`suspected sepsis`、来源对应 community / custodial 场景；
- NHS Shortness of breath：该来源所描述的严重呼吸困难急诊警示上下文中的突然意识混乱；
- 未补通用来源并重新审核前，禁止升格为跨病种全局 RED_FLAG。

`NEED_MORE_SOURCE` 不再作为当前条目本身的 verdict；它只保留为未来申请全局升格的前置条件。

该修订仍需 Medical Owner 确认后，才能把 EV-RF-NEURO-001 从 REVISE 转为 APPROVE。

---

## 4. 第二轮已闭合问题

```text
病因先验 = CLOSED_FOR_v0.2_DRAFT
A-RS-02 拆条 = CLOSED
局灶/单侧感觉异常收窄 = CLOSED
suspected sepsis scope = CLOSED
16+ population lock = CLOSED
COMBINATION_SIGNAL removed from A = CLOSED
B executable thresholds = STILL_PROHIBITED
UNKNOWN / UNMEASURED / NOT_ASKED / remote-not-observed negative conversion = PROHIBITED
Pediatrics / pregnancy expansion = PROHIBITED
NICE/NHS as China production rule = PROHIBITED
```

---

## 5. 当前残留

```text
A-RS-02A targeted scope revision = APPLIED / MEDICAL_OWNER_CONFIRMATION_PENDING
EV-RF-NEURO-001 targeted scope revision = APPLIED / MEDICAL_OWNER_CONFIRMATION_PENDING
```

除上述两项确认外，第二轮没有新的 REVISE / REJECT / NEED_MORE_SOURCE 项。

---

## 6. Gate

```text
A/B v0.2 Second Review = COMPLETE
A/B Package Approval = NOT_COMPLETE
Gate A = NOT_PASSED
C Rule Pack Clinical Content = BLOCKED
CD-07 = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

下一步只能对 A-RS-02A 与 EV-RF-NEURO-001 的定向 scope 修订做 Medical Owner confirmation。确认前不得开始 C Rule Pack 真实内容。