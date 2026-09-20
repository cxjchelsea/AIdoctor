# U03 Clinical Risk Semantics — Content Draft v0.1

> 角色：Clinical Input Package A 的真实医学内容草案。
> 状态：`SOURCE_GROUNDED_DRAFT / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED / NOT_FOR_PRODUCTION`
> 说明：本文件不是新的工程实现授权，也不是生产规则发布。内容只作为医学 Owner 逐条审核的候选语义。

## 1. v0.1 适用范围

本版先覆盖成人一般急性症状远程/社区问诊中的高安全影响信号。来源优先使用 NICE 与 NHS 的公开临床/患者安全资料。

暂不宣称覆盖：
- 儿科；
- 妊娠/产褥期；
- 精神科急症；
- 创伤全量分诊；
- 中毒全量分诊；
- 专病全部红旗；
- 所有国家/地区急救流程。

其中 NICE 疑似脓毒症的具体阈值候选仅适用于其原始指南定义的人群与场景，不得扩展为全局生命体征阈值。

## 2. Source-grounded 临床语义候选

### A-RS-01 急性严重呼吸受损

候选定义：存在明显严重呼吸困难，尤其无法完整说话、喘憋/窒息样表现，属于必须被 U03 显式保留的安全关键信号。

来源依据：NHS Shortness of breath 将 severe difficulty breathing（如 gasping/choking/not able to get words out）列为立即急诊处理信号。

结构建议：
- category：`RED_FLAG`
- source：可接受患者/照护者报告；若有客观测量，可作为附加证据
- 不等价于：确诊某疾病、D09 最终 HIGH_RISK、U04 Safety Gate

医学审核点：
- 是否需要把“不能完整说话”与“主观严重气短”拆成两条 evidence；
- 儿科呼吸窘迫征象是否另建独立 catalog。

### A-RS-02 中枢低灌注/严重缺氧的外观或认知异常

候选定义：新发意识/认知改变、明显发绀或灰白/苍白外观，应作为高安全影响证据候选；不能因为其他症状轻微而被覆盖。

来源依据：
- NHS Shortness of breath：突然意识混乱、皮肤/口唇苍白蓝灰属于立即急诊信号；
- NICE 成人疑似脓毒症：new altered mental state、mottled/ashen appearance、cyanosis 属于风险分层的重要高风险信号。

结构建议：
- category：`RED_FLAG` 或 `MUST_NOT_MISS`，最终分类由医学 Owner 冻结
- 对皮肤颜色描述需保留肤色差异与观察部位限制

### A-RS-03 急性局灶神经功能缺损

候选定义：突然发生的面部/肢体无力或麻木、突发言语/语言障碍，应进入必须排除血管事件的安全关键信号集合。

来源依据：
- NICE Suspected neurological conditions：sudden-onset limb weakness 可由 stroke/TIA 引起；sudden-onset speech or language disturbance 应立即按血管事件评估；
- NHS Stroke symptoms：FAST 面部无力、单侧上肢无力/麻木、言语异常为主要卒中症状，症状即使短暂缓解仍需立即求医。

结构建议：
- category：`MUST_NOT_MISS` +/or `RED_FLAG`
- onset/temporality 为必需字段
- 症状已经缓解不得自动转为 negative

### A-RS-04 急性缺血性胸痛样表现

候选定义：突然或持续的胸部压迫/紧缩不适，尤其伴向上肢、颈、下颌、背部或上腹放射，或伴出汗、恶心、头晕、呼吸困难时，应作为需要紧急排除心肌缺血事件的安全证据候选。

来源依据：NHS Chest pain / Heart attack 页面将上述表现列为需要立即急救评估的典型警示症状。

结构建议：
- category：`MUST_NOT_MISS`
- 不应将“非典型”或“类似消化不良”表现自动排除
- 不等价于已确诊心肌梗死

### A-RS-05 快速进展的过敏反应伴气道/呼吸/循环受损

候选定义：快速发生、涉及气道、呼吸或循环的严重全身过敏反应特征，应作为安全关键证据；皮肤/黏膜表现常见但不应被设计为绝对必需条件。

来源依据：NICE NG258（2026）将 anaphylaxis 描述为快速发生、可危及生命并涉及 airway/breathing/circulation 的系统性超敏反应，且多数病例伴皮肤/黏膜改变。

结构建议：
- category：`RED_FLAG`
- 时间起始与可能诱因应保留为 provenance/clinical context

### A-RS-06 疑似感染背景下的成人脓毒症高风险生理信号

候选定义：只有在“疑似或确认感染”的上下文中，才允许应用 NICE 成人社区/非急性场景的脓毒症风险阈值；这些阈值不得成为全局生命体征红旗。

来源依据：NICE NG253（2025/2026 页面）对 16 岁及以上社区/羁押场景疑似脓毒症给出高风险标准，包括新发意识改变、呼吸频率 ≥25/min、收缩压 ≤90 mmHg 或较平时下降 >40 mmHg、心率 >130/min，以及花斑/灰白、发绀、非褪色性瘀点/紫癜样皮疹等。

结构建议：
- category：`VITAL_SIGN_SAFETY_SIGNAL` + `COMBINATION_SIGNAL`
- 必须有 infection-context prerequisite
- population scope：`age >= 16`；妊娠/近期妊娠必须按指南独立处理，当前 v0.1 不覆盖
- 这些阈值只可绑定对应 Rule Release，不可作为 Evidence Catalog 的全局常量

## 3. 对现有结构语义的医学内容补充

1. `RED_FLAG` 应优先表示“需要在安全裁决链中显式处理、不可被一般信息缺失或低置信度静默覆盖的临床警示证据”，而不是“必然 HIGH_RISK”。
2. `MUST_NOT_MISS` 更适合表示“潜在严重诊断方向/事件必须被显式保留并排除”，例如急性血管事件或急性冠脉事件候选；它可与 RED_FLAG 重叠，但两者不应在 v0.1 强行定义为包含关系。
3. `VITAL_SIGN_SAFETY_SIGNAL` 的阈值必须绑定具体人群、场景和来源指南；禁止全局复制专病阈值。
4. 任何症状“已经缓解”不自动意味着安全，尤其是卒中/TIA 类短暂神经症状。
5. 皮肤颜色、主观症状和远程观察存在测量与肤色偏差，Evidence Definition 必须保留 limitation，而不能当成绝对阴性依据。

## 4. 医学审核状态

| Candidate | Source-grounded | Medical review | Production use |
|---|---|---|---|
| A-RS-01 | YES | REQUIRED | NO |
| A-RS-02 | YES | REQUIRED | NO |
| A-RS-03 | YES | REQUIRED | NO |
| A-RS-04 | YES | REQUIRED | NO |
| A-RS-05 | YES | REQUIRED | NO |
| A-RS-06 | YES | REQUIRED | NO |

## 5. 当前结论

```text
A Clinical Risk Semantics
= STRUCTURAL_SEMANTICS_FROZEN
/ SOURCE_GROUNDED_CONTENT_DRAFT_v0.1_AVAILABLE
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ NOT_APPROVED
/ NOT_FOR_PRODUCTION
```
