# U03 Evidence Catalog — Content Draft v0.2

> 角色：Clinical Input Package B 的修订医学内容草案。  
> 权威输入：`U03_AB_Medical_Owner_Review_Record_v0.1.md`、`U03_AB_Revision_Task_v0.2.md`、第二轮 Medical Owner Review。  
> 状态：`REVISION_DRAFT / SECOND_REVIEW_CHANGES_APPLIED / MEDICAL_OWNER_CONFIRMATION_REQUIRED / NOT_APPROVED / NOT_FOR_PRODUCTION`  
> 范围：成人一般急性症状远程/社区问诊的初始安全证据集；不是完整全病种 Catalog。

## 1. Source Registry

沿用 v0.1 Source Registry；本轮不新增或替换临床来源。

| source_id | 来源 | 主要支持内容 |
|---|---|---|
| SRC-NICE-SEPSIS-NG253 | NICE NG253 Suspected sepsis in people aged 16 or over | 成人疑似脓毒症上下文中的意识、外观与生命体征安全信号 |
| SRC-NICE-NEURO-NG127 | NICE NG127 Suspected neurological conditions | 成人突发局灶神经功能异常、言语/语言异常 |
| SRC-NICE-ANAPHYLAXIS-NG258 | NICE NG258 Anaphylaxis | 快速发生的气道、呼吸或循环严重受损 |
| SRC-NHS-STROKE | NHS Symptoms of a stroke | FAST 与卒中警示症状 |
| SRC-NHS-CHEST | NHS Chest pain / Heart attack | 急性高危胸痛样表现 |
| SRC-NHS-DYSPNOEA | NHS Shortness of breath | 严重呼吸困难、异常肤色、突然意识混乱 |

## 2. 公共字段语义

所有 entry 均必须显式保留：

```text
missingness_handling:
  UNKNOWN != NO
  UNMEASURED != NORMAL
  NOT_ASKED != NO
  AMBIGUOUS != NEGATIVE
  CONFLICTING != NEGATIVE
  REMOTE_NOT_OBSERVED != EXCLUDED
```

其中远程观察相关条目还必须记录 observation/channel limitation；症状缓解相关条目必须保留历史阳性语义。

## 3. Candidate Entries v0.2

### EV-RF-RESP-001 — 严重呼吸困难伴言语受限

- category：`RED_FLAG`
- definition：明显严重呼吸困难，尤其喘憋、窒息样表现或因呼吸困难无法正常说话
- scope：成人一般急性远程/社区问诊候选
- limitation：远程主诉可作为候选；远程未观察到严重表现不得作为充分排除
- missingness：未询问/无法观察不得解释为阴性
- source：`SRC-NHS-DYSPNOEA`
- second-round verdict：`APPROVE`

### EV-RF-NEURO-001 — 新发意识或认知改变

- category：`RED_FLAG` candidate
- definition：新出现的意识、行为或认知异常
- scope：仅限当前来源明确支持的上下文，不得写成“任何急性/专病上下文”或跨病种全局 RED_FLAG
- source-context A：NICE NG253 对应 `age >= 16`、`suspected sepsis`、来源对应 community / custodial 场景
- source-context B：NHS Shortness of breath 所描述的严重呼吸困难急诊警示上下文中的突然意识混乱
- global-promotion：`PROHIBITED_PENDING_ADDITIONAL_GENERAL_ACUTE_SOURCE_AND_REVIEW`
- missingness：未询问或信息不足不得解释为阴性
- source：`SRC-NICE-SEPSIS-NG253`、`SRC-NHS-DYSPNOEA`
- second-round verdict：`REVISE`
- current action：`SOURCE_CONTEXT_SCOPE_TIGHTENED / MEDICAL_OWNER_CONFIRMATION_REQUIRED`

### EV-RF-APPEAR-001 — 异常皮肤/口唇颜色或灰白外观

> 由 v0.1 `EV-RF-CIRC-001` 修订；新 ID 避免继续承载循环机制暗示。

- category：`RED_FLAG`
- definition：皮肤、口唇或舌明显蓝灰，或出现明显灰白/苍白外观
- limitation：肤色、光线、观察部位与远程成像质量均可影响判断
- channel rule：远程阴性观察不得作为充分排除
- missingness：无法观察/图像质量不足 = `UNKNOWN/UNRELIABLE`，不得转阴性
- source：`SRC-NICE-SEPSIS-NG253`、`SRC-NHS-DYSPNOEA`
- second-round verdict：`APPROVE`

### EV-MNM-NEURO-001 — 突发局灶无力或单侧/局灶感觉异常

- category：`MUST_NOT_MISS`
- definition：突然发生的局灶性面部或肢体无力，或突发单侧/局灶感觉异常，需要显式保留急性血管性神经事件方向
- required fields：`onset`、`side`、`focality`、`temporality`
- exclusion boundary：慢性、双侧或非局灶感觉异常不得仅因“麻木”字样自动进入本条
- resolution rule：症状缓解不得自动转为 negative
- source：`SRC-NICE-NEURO-NG127`、`SRC-NHS-STROKE`
- second-round verdict：`APPROVE`

### EV-MNM-NEURO-002 — 突发言语或语言异常

- category：`MUST_NOT_MISS`
- definition：突然发生的言语含糊、表达或理解异常，需要显式保留血管事件方向
- resolution rule：短暂缓解仍保留历史阳性证据
- missingness：未询问不得解释为阴性
- source：`SRC-NICE-NEURO-NG127`、`SRC-NHS-STROKE`
- second-round verdict：`APPROVE`

### EV-MNM-CARD-001 — 急性高危胸痛样表现

- category：`MUST_NOT_MISS`
- definition：突然或持续胸部压迫/紧缩样不适，特别是伴放射不适、出汗、恶心、头晕或呼吸困难时，需要显式考虑急性冠脉事件方向
- naming boundary：Evidence 本名不使用“缺血性”等病因命名
- diagnostic boundary：需考虑急性冠脉事件 != 已确诊
- atypical boundary：非典型或类似消化不良表现不得自动排除
- source：`SRC-NHS-CHEST`
- second-round verdict：`APPROVE`

### EV-RF-ALLERGY-001 — 快速进展的过敏反应伴气道/呼吸/循环受损

- category：`RED_FLAG`
- definition：快速出现、涉及气道、呼吸或循环的严重全身过敏反应特征
- boundary：皮肤/黏膜改变常见但不是绝对必需；非过敏性 ABC 受损不得自动编入本条
- missingness：未观察到皮肤表现不得自动转阴性
- source：`SRC-NICE-ANAPHYLAXIS-NG258`
- second-round verdict：`APPROVE`

### EV-VS-SEPSIS-001 — 成人疑似脓毒症上下文中的异常呼吸频率信号

- category：`VITAL_SIGN_SAFETY_SIGNAL`
- context prerequisite：`suspected sepsis`
- population：`age >= 16`
- setting：来源对应 community / custodial 等场景
- definition：对应来源风险分层中的异常呼吸频率候选信号
- threshold：`NOT_STORED_IN_CATALOG`
- prohibition：不得复制为全局生命体征红旗
- source：`SRC-NICE-SEPSIS-NG253`
- second-round verdict：`APPROVE`

### EV-VS-SEPSIS-002 — 成人疑似脓毒症上下文中的异常收缩压信号

- category：`VITAL_SIGN_SAFETY_SIGNAL`
- context prerequisite：`suspected sepsis`
- population：`age >= 16`
- setting：来源对应 community / custodial 等场景
- definition：对应来源风险分层中的异常收缩压或相对基线变化候选信号
- threshold：`NOT_STORED_IN_CATALOG`
- prohibition：不得复制为全局生命体征红旗
- source：`SRC-NICE-SEPSIS-NG253`
- second-round verdict：`APPROVE`

### EV-VS-SEPSIS-003 — 成人疑似脓毒症上下文中的异常心率信号

- category：`VITAL_SIGN_SAFETY_SIGNAL`
- context prerequisite：`suspected sepsis`
- population：`age >= 16`
- setting：来源对应 community / custodial 等场景
- definition：对应来源风险分层中的异常心率候选信号
- threshold：`NOT_STORED_IN_CATALOG`
- prohibition：不得复制为全局生命体征红旗
- source：`SRC-NICE-SEPSIS-NG253`
- second-round verdict：`APPROVE`

### EV-RF-SEPSIS-001 — 成人疑似脓毒症上下文中的不褪色瘀点/紫癜样皮疹

- category：`RED_FLAG`
- context prerequisite：`suspected sepsis`
- population：`age >= 16`
- definition：疑似脓毒症场景中出现不褪色瘀点或紫癜样皮疹
- limitation：远程图像/描述质量有限，必须记录来源质量
- channel rule：远程未观察到不得作为充分排除
- source：`SRC-NICE-SEPSIS-NG253`
- second-round verdict：`APPROVE`

## 4. v0.1 → v0.2 处置摘要

```text
EV-RF-RESP-001 = retained; missingness hardened
EV-RF-NEURO-001 = source contexts explicitly locked after second review; global promotion still prohibited
EV-RF-CIRC-001 = replaced by mechanism-neutral EV-RF-APPEAR-001
EV-MNM-NEURO-001 = sudden focal/unilateral sensory scope hardened
EV-MNM-NEURO-002 = retained; missingness hardened
EV-MNM-CARD-001 = ischemic naming removed
EV-RF-ALLERGY-001 = retained; non-allergic ABC exclusion hardened
EV-VS-SEPSIS-001/002/003 = suspected-sepsis scope locked; no thresholds in Catalog
EV-RF-SEPSIS-001 = naming aligned to suspected-sepsis scope
```

## 5. 当前明确禁止的提升

Candidate Entry 仍不得直接被解释为：

```text
APPROVED Evidence Definition
Rule Pack Rule
D09 Decision
HIGH_RISK
Safety Gate Decision
Production Release
```

并继续禁止：
- 将专病阈值扩展为一般人群全局阈值；
- 成人来源扩展到儿童或孕产；
- patient-reported 数值自动视为已验证客观测量；
- “未观察到”自动等价于 negative；
- 将 NICE/NHS 直接视为中国生产规则。

## 6. 当前状态

```text
B Evidence Catalog v0.2
= SECOND_REVIEW_COMPLETE
/ ONE_TARGETED_SCOPE_REVISION_APPLIED
/ MEDICAL_OWNER_CONFIRMATION_REQUIRED_FOR_EV-RF-NEURO-001
/ PACKAGE_NOT_APPROVED
/ NOT_FOR_PRODUCTION
```
