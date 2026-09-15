# U03 A/B Revision Task v0.2

> 角色：A/B Content Draft v0.2 的分类修订任务单。  
> 权威输入：`U03_AB_Medical_Owner_Review_Record_v0.1.md`。  
> 状态：`REVISION_TASK_OPEN / C_CONTENT_NOT_AUTHORIZED / NOT_FOR_PRODUCTION`  
> 本文件只规定 v0.2 修订范围与验收，不批准任何临床内容，不授权进入 C/D/E/F 真实内容。

修订对象：

```text
U03_Clinical_Risk_Semantics_Content_Draft_v0.1.md
→ U03_Clinical_Risk_Semantics_Content_Draft_v0.2.md

U03_Evidence_Catalog_Content_Draft_v0.1.md
→ U03_Evidence_Catalog_Content_Draft_v0.2.md
```

本轮只处理 A/B。不得起草 Rule Pack、D09 Policy、Knowledge Release 临床适用性裁决或 EvalSet 金标准病例。

---

## 提示词 1（病因先验中性化）

统一处理 Evidence / Semantics 名称层把临床表现写成病理机制的问题。审查范围覆盖所有仍含缺血、低灌注、缺氧、循环衰竭等机制命名的候选；目标是沉淀“现象层命名、诊断层后置”的公共命名规则，而不是逐条改成另一个诊断名。

适用条目：
- A-RS-02：去掉“中枢低灌注/严重缺氧”；拆成独立的意识/认知改变语义与异常肤色语义。
- A-RS-04、EV-MNM-CARD-001：去掉“缺血性”；改为中性高危胸痛样表现，并保留“需考虑急性冠脉事件 ≠ 已确诊”。
- EV-RF-CIRC-001：去掉循环机制暗示；`CIRC` 编码不得继续承担病因含义，名称改为异常皮肤 / 口唇颜色或灰白外观。

验收：v0.2 名称与 ID 注释中不再把机制诊断当作 Evidence 本名。

---

## 提示词 2（专病 scope 收窄与来源忠实）

统一处理把专病指南阈值或场景写成泛感染、全局生命体征或跨人群规则的问题。审查范围覆盖脓毒症相关语义、生命体征信号和皮疹条目；目标是沉淀“来源上下文锁定、阈值不进 Catalog”的公共机制。

适用条目：
- A-RS-06：收窄为 16 岁及以上、疑似脓毒症、community / custodial 等来源对应非急性场景；删除“疑似或确认感染”“社区/非急性”等更宽表述；妊娠 / 近期妊娠继续排除；删除 A 层对 `COMBINATION_SIGNAL` 的预分类。
- EV-VS-SEPSIS-001 / 002 / 003：名称与定义一律改为疑似脓毒症上下文；人口锁 16+；不写具体数值；不复制为全局生命体征红旗。
- EV-RF-SEPSIS-001：名称与定义对齐为疑似脓毒症场景；保留远程图像质量限制。

验收：v0.2 不再出现可被读成“任何疑似感染均可套用 NG253”的表述；B 仍不承载可执行阈值。

---

## 提示词 3（条目拆分与必填字段硬化）

统一处理把不同测量误差、不同适用上下文的表现捆在一条，以及关键限制只写在备注里的问题。目标是沉淀拆条规则和必填字段，而不是继续用自由文本提醒。

适用条目：
- A-RS-02：必须拆条，不得只改名。
- A-RS-03、EV-MNM-NEURO-001：将“麻木”收窄为突发单侧 / 局灶感觉异常；onset / side / temporality 必填；症状缓解不得自动转阴性。
- EV-RF-CIRC-001：肤色、光线、远程成像质量限制写入 `limitation` / `missingness_handling` 等硬字段；远程阴性观察不得作为充分排除。

验收：拆后的每条语义只对应一类可观察现象；关键限制可在 schema 字段中被机器和审核同时看到。

---

## 提示词 4（全局红旗与专病红旗分离）

统一处理专病来源被升格为跨病种全局红旗的问题。审查范围覆盖新发意识 / 认知改变；目标是沉淀“先锁 scope，再谈升格”的来源治理，而不是先写成全局再补出处。

适用条目：
- EV-RF-NEURO-001：本轮默认收窄为疑似脓毒症 / 急诊相关上下文候选，并保持 `NEED_MORE_SOURCE`；若主张全局 RED_FLAG，必须先补通用急性病 / 急诊权威来源，不得只靠 NG253 或 NHS 呼吸困难页完成泛化。
- A 若保留对应意识改变语义，必须与 B 的 scope 声明一致，不得一边收窄一边在 A 写成全局。

验收：v0.2 对意识改变给出明确 scope；未补来源前不得标记为全局可批准红旗。

---

## 提示词 5（UNKNOWN / 未测 / 未问不得转阴性）

统一处理“没问、没测、未知、矛盾、远程未看到”被读成安全的问题。审查范围覆盖全部 A 语义与 B 条目，包括本轮 APPROVE 条目；目标是沉淀每条 entry 的缺失与不确定性硬字段，而不是只在总则里写原则。

公共要求：
- `UNKNOWN != NO != NEGATIVE != NORMAL`
- `UNMEASURED` / `NOT_ASKED` / `AMBIGUOUS` / `CONFLICTING` 不得默认转阴性
- 远程未观察到 ≠ 已排除
- 症状已缓解 ≠ 当前安全，尤其是短暂神经功能缺损

APPROVE 条目本轮不做临床改写，但须补齐上述硬字段：A-RS-01、A-RS-05、EV-RF-RESP-001、EV-MNM-NEURO-002、EV-RF-ALLERGY-001、EV-RF-SEPSIS-001。

验收：每条 v0.2 entry 都能回答“未知时不能干什么”，而不是只回答“阳性时是什么”。

---

## 提示词 6（状态文件与内容稿对齐）

统一处理状态文件超前于正文、审稿意见被当成批准的问题。目标是沉淀“内容稿、审核记录、状态源”三者一致，而不是继续用状态文件宣布修订完成。

必须同步：
- A/B 内容稿版本升到 v0.2 后，状态源改为新草稿与本任务单的完成记录；
- 不得把 Recommendation v0.1 的 3/7 APPROVE 计数继续当作当前结论；
- 不得把 Source Review 的 `PASS_FOR_MEDICAL_REVIEW` 写成语义批准；
- 不得在修订完成前把 Revision Pass 标为 COMPLETE。

验收：任何 readiness / governance / revision status 都不再声称 v0.1 正文已经完成病因中性化或 scope 收窄。

---

## 提示词 7（修订交付与禁止越权）

统一处理修订过程中把 B 写成 Rule、把 NICE/NHS 写成中国生产规则、或提前进入 C 的问题。目标是守住 A/B 修订闭环，确保本轮交付仍是可再审的内容草案。

允许：
- 产出 A/B Content Draft v0.2；
- 为 REVISE / NEED_MORE_SOURCE 条目写清修订说明；
- 为 APPROVE 条目做硬字段补齐与最小名称对齐；
- 更新 revision / readiness / governance 状态为“v0.2 修订中或修订完成待再审”。

禁止：
- 起草 C Rule Pack 真实规则、阈值分层或组合规则；
- 起草 D09 生产 policy、E 适用性裁决、F 金标准病例；
- 把任何条目标为 `APPROVED`、`PRODUCTION_ELIGIBLE` 或允许 Runtime 消费；
- 把成人条目扩展到儿童或孕产；
- 把 NICE / NHS 写成中国最终生产规则；本地化只允许记为后续门禁，不在本轮假装完成。

---

## 条目处置总表

| ID | 本轮 Verdict | v0.2 动作 |
|---|---|---|
| A-RS-01 | APPROVE | 原文方向保留；补齐 UNKNOWN / 远程阴性硬字段。 |
| A-RS-02 | REVISE | 拆条 + 去机制命名。 |
| A-RS-03 | REVISE | 收窄麻木；硬化 onset / side。 |
| A-RS-04 | REVISE | 去“缺血性”。 |
| A-RS-05 | APPROVE | 原文方向保留；补齐硬字段。 |
| A-RS-06 | REVISE | 锁 suspected sepsis scope；组合关系移出 A。 |
| EV-RF-RESP-001 | APPROVE | 保留；补齐硬字段。 |
| EV-RF-NEURO-001 | NEED_MORE_SOURCE | 先收窄 scope，或补通用来源后再申请全局。 |
| EV-RF-CIRC-001 | REVISE | 中性命名 + 限制字段硬化。 |
| EV-MNM-NEURO-001 | REVISE | 收窄麻木；onset / side 必填。 |
| EV-MNM-NEURO-002 | APPROVE | 保留；补齐硬字段。 |
| EV-MNM-CARD-001 | REVISE | 去“缺血性”。 |
| EV-RF-ALLERGY-001 | APPROVE | 保留；禁止扩到非过敏性 ABC 受损。 |
| EV-VS-SEPSIS-001 | REVISE | 锁疑似脓毒症；不写数值。 |
| EV-VS-SEPSIS-002 | REVISE | 同上。 |
| EV-VS-SEPSIS-003 | REVISE | 同上。 |
| EV-RF-SEPSIS-001 | APPROVE | 名称与定义对齐为疑似脓毒症；保留远程限制。 |

---

## 完成定义

v0.2 修订完成的充分条件：

```text
A/B Content Draft v0.2 已产出
REVISE / NEED_MORE_SOURCE 条目均有对应修订
APPROVE 条目未被改写成新的病因名或更宽 scope
B 仍无具体生命体征阈值
C/D/E/F 无新增临床内容
Medical Owner Approval 仍标记为 NOT_COMPLETE
```

完成后只进入“v0.2 再审”，不进入 C。
