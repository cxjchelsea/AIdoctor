# U03 Evidence Catalog — Content Draft v0.1

> 角色：Clinical Input Package B 的真实医学内容草案。
> 状态：`SOURCE_GROUNDED_DRAFT / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED / NOT_FOR_PRODUCTION`
> 范围：成人一般急性症状远程/社区问诊的初始安全证据集；不是完整全病种 Catalog。

## 1. Source Registry

| source_id | 来源 | 主要支持内容 |
|---|---|---|
| SRC-NICE-SEPSIS-NG253 | NICE NG253 Suspected sepsis in people aged 16 or over | 疑似感染背景下成人意识、呼吸、循环、皮肤和生命体征风险信号 |
| SRC-NICE-NEURO-NG127 | NICE NG127 Suspected neurological conditions | 成人突发局灶神经功能异常、言语/语言异常 |
| SRC-NICE-ANAPHYLAXIS-NG258 | NICE NG258 Anaphylaxis | 快速发生的气道、呼吸或循环严重受损 |
| SRC-NHS-STROKE | NHS Symptoms of a stroke | FAST 与卒中警示症状 |
| SRC-NHS-CHEST | NHS Chest pain / Heart attack | 急性缺血性胸痛样表现 |
| SRC-NHS-DYSPNOEA | NHS Shortness of breath | 严重呼吸困难、异常肤色、突然意识混乱 |

## 2. Candidate Entries

| evidence_id | 候选名称 | category | source-grounded clinical definition | 适用/限制 | 状态 |
|---|---|---|---|---|---|
| EV-RF-RESP-001 | 严重呼吸困难伴言语受限 | RED_FLAG | 明显严重呼吸困难，尤其喘憋、窒息样表现或因呼吸困难无法正常说话 | 远程主诉可作为候选；不能据远程阴性观察排除 | MEDICAL_REVIEW_REQUIRED |
| EV-RF-NEURO-001 | 新发意识或认知改变 | RED_FLAG | 新出现的意识/行为/认知异常，可提示急性严重状态 | 在脓毒症来源中适用于疑似感染背景；泛化用途需单独批准 | MEDICAL_REVIEW_REQUIRED |
| EV-RF-CIRC-001 | 发绀或明显灰白/苍白外观 | RED_FLAG | 皮肤、口唇或舌明显蓝灰，或明显灰白/苍白外观，属于急性安全警示候选 | 受肤色、光线和远程成像质量影响，阴性观察不具充分排除力 | MEDICAL_REVIEW_REQUIRED |
| EV-MNM-NEURO-001 | 突发面部/肢体无力或麻木 | MUST_NOT_MISS | 突然发生的面部或肢体局灶性无力/麻木，需要显式考虑急性血管性神经事件 | onset/temporality 为关键字段；症状缓解不自动转阴性 | MEDICAL_REVIEW_REQUIRED |
| EV-MNM-NEURO-002 | 突发言语或语言异常 | MUST_NOT_MISS | 突然发生的言语含糊、表达/理解异常，需要显式考虑血管事件 | 短暂缓解仍保留历史阳性证据 | MEDICAL_REVIEW_REQUIRED |
| EV-MNM-CARD-001 | 急性缺血性胸痛样表现 | MUST_NOT_MISS | 突然或持续胸部压迫/紧缩样不适，特别是伴放射痛、出汗、恶心、头晕或呼吸困难时，应显式考虑急性冠脉事件 | 不等价于确诊心肌梗死；非典型表现不能自动排除 | MEDICAL_REVIEW_REQUIRED |
| EV-RF-ALLERGY-001 | 快速进展的过敏反应伴气道/呼吸/循环受损 | RED_FLAG | 快速出现、涉及气道、呼吸或循环的严重全身过敏反应特征 | 皮肤/黏膜改变常见但不应被当作绝对必需条件 | MEDICAL_REVIEW_REQUIRED |
| EV-VS-SEPSIS-001 | 疑似感染背景下异常呼吸频率信号 | VITAL_SIGN_SAFETY_SIGNAL | NICE 成人社区疑似脓毒症风险分层中的呼吸频率高风险/中高风险区间候选 | 仅限疑似感染、16岁及以上、来源场景；具体阈值属于后续 Rule Pack | MEDICAL_REVIEW_REQUIRED |
| EV-VS-SEPSIS-002 | 疑似感染背景下异常收缩压信号 | VITAL_SIGN_SAFETY_SIGNAL | NICE 成人社区疑似脓毒症风险分层中的低收缩压/较平时显著下降候选 | 仅限疑似感染、16岁及以上；妊娠/近期妊娠不在本 v0.1 | MEDICAL_REVIEW_REQUIRED |
| EV-VS-SEPSIS-003 | 疑似感染背景下明显心动过速信号 | VITAL_SIGN_SAFETY_SIGNAL | NICE 成人社区疑似脓毒症风险分层中的明显心动过速候选 | 仅限疑似感染、16岁及以上；具体阈值进入 Rule Pack | MEDICAL_REVIEW_REQUIRED |
| EV-RF-SEPSIS-001 | 疑似感染背景下不褪色瘀点/紫癜样皮疹 | RED_FLAG | 疑似脓毒症场景中出现不褪色瘀点或紫癜样皮疹，是需显式处理的风险信号 | 远程皮疹识别有限；需记录图像/描述来源质量 | MEDICAL_REVIEW_REQUIRED |

## 3. Source-to-entry mapping

- EV-RF-RESP-001：SRC-NHS-DYSPNOEA
- EV-RF-NEURO-001：SRC-NICE-SEPSIS-NG253、SRC-NHS-DYSPNOEA
- EV-RF-CIRC-001：SRC-NICE-SEPSIS-NG253、SRC-NHS-DYSPNOEA
- EV-MNM-NEURO-001 / 002：SRC-NICE-NEURO-NG127、SRC-NHS-STROKE
- EV-MNM-CARD-001：SRC-NHS-CHEST
- EV-RF-ALLERGY-001：SRC-NICE-ANAPHYLAXIS-NG258
- EV-VS-SEPSIS-001 / 002 / 003、EV-RF-SEPSIS-001：SRC-NICE-SEPSIS-NG253

## 4. 当前明确禁止的提升

本文件中的 Candidate Entry 不得直接被解释为：

```text
APPROVED Evidence Definition
Rule Pack Rule
D09 Decision
HIGH_RISK
Safety Gate Decision
Production Release
```

尤其：
- NICE 疑似脓毒症的专病阈值不得扩展为一般人群的全局生命体征阈值；
- 成人来源不得扩展到儿童；
- 妊娠/近期妊娠需要独立来源和规则；
- patient-reported 数值不得自动当作已验证客观测量；
- “未观察到”不得自动等价于 negative。

## 5. 医学 Owner 审核清单

每条 entry 至少需要确认：
1. clinical definition 是否忠实且适合本系统；
2. RED_FLAG / MUST_NOT_MISS / VITAL_SIGN_SAFETY_SIGNAL 分类是否合理；
3. 是否应拆分或合并；
4. population / region / channel scope 是否需要进一步收窄；
5. 可接受 source types；
6. ambiguity / conflict / missingness 处理；
7. 是否允许进入下一阶段 Rule Pack；
8. 是否需要额外权威来源；
9. 对应 EvalSet 正向与负向案例要求。

## 6. 当前状态

```text
B Evidence Catalog
= STRUCTURAL_SCHEMA_FROZEN
/ SOURCE_GROUNDED_CONTENT_DRAFT_v0.1_AVAILABLE
/ 11 CANDIDATE ENTRIES
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ NOT_APPROVED
/ NOT_FOR_PRODUCTION
```
