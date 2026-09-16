# U03 A/B Medical Review Recommendation v0.1

> 角色：对 `U03_Clinical_Risk_Semantics_Content_Draft_v0.1.md` 与 `U03_Evidence_Catalog_Content_Draft_v0.1.md` 进行独立、来源约束下的医学内容审查。
> 状态：`REVIEW_COMPLETE / RECOMMENDATIONS_ONLY / MEDICAL_OWNER_APPROVAL_REQUIRED / NOT_APPROVED / NOT_FOR_PRODUCTION`
> 说明：本文件给出审查建议，不替代具备授权的医学 Owner 最终签署。

## 1. Review 规则

每个候选只允许以下结论：

```text
APPROVE
REVISE
REJECT
NEED_MORE_SOURCE
```

判定重点：
1. 来源是否直接支持；
2. 是否超出来源人群/场景；
3. 是否夹带病因推断或诊断结论；
4. 是否把 Evidence 提升为 Rule / D09 / Safety Gate；
5. 是否存在远程观察或来源质量限制。

本轮 `APPROVE` 的含义仅为：

> 推荐医学 Owner 按当前定义方向批准。

不代表实际 `APPROVED` 或可用于生产。

---

## 2. A / Clinical Risk Semantics Review

| Candidate | Verdict | 主要理由 | 建议动作 |
|---|---|---|---|
| A-RS-01 急性严重呼吸受损 | APPROVE | NHS 直接支持 severe difficulty breathing、gasping/choking、不能正常说话作为紧急警示；当前定义没有越权到诊断或最终 Risk Disposition | 保留；医学 Owner 决定后续是否拆分主观严重气短与无法正常说话 |
| A-RS-02 中枢低灌注/严重缺氧的外观或认知异常 | REVISE | 来源支持“突然意识混乱/新发意识改变”和“苍白/蓝灰/灰白外观”为高安全影响表现，但不支持 Evidence 层直接将两类表现统一归因为“中枢低灌注/严重缺氧” | 去掉病理机制命名；拆成“新发意识/认知改变”和“发绀/灰白/苍白外观”两个独立语义候选 |
| A-RS-03 急性局灶神经功能缺损 | APPROVE | NICE 明确支持 sudden-onset weakness、sudden-onset speech/language disturbance；单侧突发感觉异常也按卒中/TIA 路径评估。定义保留了 sudden/onset 和未确诊边界 | 保留；将“麻木”明确限定为突发局灶/单侧感觉异常，避免把慢性或非局灶麻木泛化 |
| A-RS-04 急性缺血性胸痛样表现 | REVISE | NHS 支持压迫/紧缩/挤压样胸痛、放射和伴随呼吸困难、恶心、出汗等，但 Evidence 名称“缺血性”提前嵌入病因判断 | 改名为“急性高危胸痛样表现”或等价中性名称；定义继续保留“需显式考虑急性冠脉事件，不等于确诊” |
| A-RS-05 快速进展的过敏反应伴气道/呼吸/循环受损 | APPROVE | NICE NG258 直接支持快速发生、危及生命并涉及 airway/breathing/circulation 的表现；皮肤/黏膜改变为常见而非绝对必要 | 保留 |
| A-RS-06 疑似感染背景下的成人脓毒症高风险生理信号 | REVISE | NICE NG253 直接支持，但必须严格绑定 suspected sepsis、16+、community/custodial 等来源上下文；当前“疑似或确认感染”与“社区/非急性”表述比来源更宽 | 收窄到“16岁及以上、疑似脓毒症、社区/羁押等对应非急性场景”；妊娠/近期妊娠继续排除出 v0.1；具体数值仍只进入 C Rule Pack |

A Review Summary：

```text
APPROVE = 3
REVISE = 3
REJECT = 0
NEED_MORE_SOURCE = 0
```

结论：A 可进入 `MEDICAL_OWNER_REVIEW_READY_AFTER_REVISIONS`，但尚未达到审批完成。

---

## 3. B / Evidence Catalog Review

| evidence_id | Verdict | 主要理由 | 建议动作 |
|---|---|---|---|
| EV-RF-RESP-001 | APPROVE | NHS 直接支持，定义中性且未升级为诊断 | 保留 |
| EV-RF-NEURO-001 | NEED_MORE_SOURCE | 在疑似脓毒症场景有 NICE 强来源，在 NHS 严重呼吸困难页面也有 emergency confusion，但若作为“全局 RED_FLAG”使用，当前跨病种来源仍不足 | 先限定为 sepsis-context / emergency-context candidate；若希望全局使用，补通用急性意识改变权威来源 |
| EV-RF-CIRC-001 | REVISE | 来源支持苍白/蓝灰/灰白等危险外观，但当前把多个外观统一为 circulation 类名称，且远程观察受肤色/光线影响 | 改成更中性的“异常皮肤/口唇颜色或灰白外观”；保留远程阴性不可排除的限制 |
| EV-MNM-NEURO-001 | REVISE | sudden weakness 有强支持；突发单侧 numbness 有 NICE 支持，但“面部/肢体无力或麻木”范围仍略宽 | 明确为“突发局灶性面/肢无力，或突发单侧感觉异常”；onset/side 必填 |
| EV-MNM-NEURO-002 | APPROVE | NICE 对 sudden-onset speech or language disturbance 直接支持 | 保留 |
| EV-MNM-CARD-001 | REVISE | 来源支持高危胸痛模式，但名称“缺血性”仍暗含病因 | 改为“急性高危胸痛样表现”；不变更 MUST_NOT_MISS 属性 |
| EV-RF-ALLERGY-001 | APPROVE | NICE NG258 直接支持，且未要求皮肤表现必须存在 | 保留 |
| EV-VS-SEPSIS-001 | APPROVE | 作为“疑似脓毒症上下文的呼吸频率异常证据概念”合理；具体阈值已明确留在 C | 保留严格 scope |
| EV-VS-SEPSIS-002 | APPROVE | 同上；相对平时下降的规则需要可靠 baseline，但这属于 Rule 层 | 保留严格 scope |
| EV-VS-SEPSIS-003 | APPROVE | 同上 | 保留严格 scope |
| EV-RF-SEPSIS-001 | APPROVE | NICE NG253 对 non-blanching petechial/purpuric rash 有直接支持，且远程限制已声明 | 保留严格 scope 与图像/描述质量限制 |

B Review Summary：

```text
APPROVE = 7
REVISE = 3
REJECT = 0
NEED_MORE_SOURCE = 1
```

结论：B 已有一批可以送医学 Owner 逐条签署的候选，但 v0.1 不应整体批准。

---

## 4. Review Findings

### MR-01 — Evidence 名称不得夹带病理机制

以下命名需要修正：

```text
中枢低灌注/严重缺氧的外观或认知异常
急性缺血性胸痛样表现
```

原因：来源支持的是临床表现与紧急性，不足以在 Evidence 层直接确认病理机制。

### MR-02 — 全局 RED_FLAG 与专病 RED_FLAG 必须分开

`EV-RF-NEURO-001` 当前最强来源之一是 suspected sepsis。若要作为全局急性意识改变证据，应补通用来源或明确 scope；不能依靠专病指南完成全局泛化。

### MR-03 — 专病生命体征阈值边界正确，但 scope 必须更精确

NG253 条目只能按对应 suspected sepsis 人群与场景绑定。Evidence Catalog 只保留概念；数值阈值、moderate/high 分层、组合关系留在 C Rule Pack。

### MR-04 — Patient-facing NHS 页面可支持第一轮安全候选，但不应单独承担最终生产规则

NHS patient guidance 适合支撑“哪些表现需要紧急注意”的候选语义；真正进入 production Rule Pack 前，优先补充 professional guideline / local clinical pathway / medical-owner adjudication。

### MR-05 — Localization 仍是必需门禁

若产品目标地区不是英国，NICE/NHS 可以作为国际初始 source pack，但不能直接等价为目标地区正式 clinical release。需要后续中国大陆或实际部署地区的指南/路径本地化审查。

---

## 5. 建议的修订后状态

A：

```text
3 candidates = RECOMMENDED_FOR_MEDICAL_OWNER_APPROVAL
3 candidates = REVISION_REQUIRED
A overall = MEDICAL_OWNER_REVIEW_READY_AFTER_REVISIONS / NOT_APPROVED
```

B：

```text
7 entries = RECOMMENDED_FOR_MEDICAL_OWNER_APPROVAL
3 entries = REVISION_REQUIRED
1 entry = NEED_MORE_SOURCE
B overall = PARTIALLY_REVIEW_READY / NOT_APPROVED
```

只有医学 Owner 对具体条目完成批准后，批准条目才允许进入 C Rule Pack 内容设计。

---

## 6. Gate 结论

```text
A Medical Review Recommendation = COMPLETE
B Medical Review Recommendation = COMPLETE
Medical Owner Approval = NOT_COMPLETE
A/B Production Eligibility = NO
C Rule Pack Clinical Content = NOT_AUTHORIZED_FROM_UNAPPROVED_ENTRIES
CD-07 Implementation Readiness = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```
