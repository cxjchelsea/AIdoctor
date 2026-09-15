# U03 Clinical Risk Semantics — Content Draft v0.2

> 角色：Clinical Input Package A 的修订医学内容草案。  
> 权威输入：`U03_AB_Medical_Owner_Review_Record_v0.1.md`、`U03_AB_Revision_Task_v0.2.md`、第二轮 Medical Owner Review。  
> 状态：`REVISION_DRAFT / SECOND_REVIEW_CHANGES_APPLIED / MEDICAL_OWNER_CONFIRMATION_REQUIRED / NOT_APPROVED / NOT_FOR_PRODUCTION`  
> 说明：本版只落实 Medical Owner 对 v0.1 及第二轮 v0.2 review 的修订要求；不构成整包医学批准、Rule Pack 授权或生产发布。

## 1. v0.2 适用范围

本版仍只覆盖成人一般急性症状远程/社区问诊中的高安全影响候选信号。

暂不覆盖：
- 儿科；
- 妊娠/产褥期；
- 精神科急症；
- 创伤全量分诊；
- 中毒全量分诊；
- 专病全部红旗；
- 所有国家/地区急救流程。

NICE 疑似脓毒症相关内容仅可在其来源支持的人群与场景中作为候选语义使用，不得泛化为全局生命体征标准。

## 2. 公共不确定性语义

所有条目均遵守：

```text
UNKNOWN != NO
UNMEASURED != NORMAL
NOT_ASKED != NO
AMBIGUOUS != NEGATIVE
CONFLICTING != NEGATIVE
REMOTE_NOT_OBSERVED != EXCLUDED
SYMPTOM_RESOLVED != CURRENTLY_SAFE
```

任何未问、未测、无法观察、信息矛盾或远程未发现的情况，不得自动转成阴性或安全结论。

## 3. 修订后的 Clinical Risk Semantics 候选

### A-RS-01 急性严重呼吸受损

候选定义：存在明显严重呼吸困难，尤其喘憋、窒息样表现或因呼吸困难无法正常说话，属于必须在 U03 中显式保留的安全关键信号。

来源依据：沿用 v0.1 的 NHS Shortness of breath 来源。

结构建议：
- category：`RED_FLAG`
- 允许患者/照护者报告；客观测量仅作为附加证据
- 远程未观察到严重表现不得作为充分排除依据
- 不等价于确诊某疾病、D09 最终 HIGH_RISK 或 U04 Safety Gate

### A-RS-02A 新发意识或认知异常

候选定义：新出现的意识、行为或认知异常可作为高安全影响候选信号，但本版默认 scope 仅限于当前来源明确支持的上下文，不得表述为一般急症或跨病种全局红旗。

来源依据：沿用 v0.1 中 NHS Shortness of breath 与 NICE 成人疑似脓毒症来源。

结构建议：
- category：候选 `RED_FLAG`
- NICE NG253 对应使用范围：`age >= 16`、`suspected sepsis`、来源对应 community / custodial 场景
- NHS Shortness of breath 对应使用范围：该来源所描述的严重呼吸困难急诊警示上下文中的突然意识混乱
- 上述两个来源上下文不得合并解释为“任何急性疾病的新发意识改变均为全局 RED_FLAG”
- 若未来申请跨病种全局 RED_FLAG，必须另补通用急性病/急诊权威来源并重新审核
- UNKNOWN / 未询问不得解释为阴性

### A-RS-02B 异常皮肤/口唇颜色或灰白外观

候选定义：皮肤、口唇或舌出现明显蓝灰，或出现明显灰白/苍白外观，可作为高安全影响候选信号。

来源依据：沿用 v0.1 中 NHS Shortness of breath 与 NICE 成人疑似脓毒症来源。

结构建议：
- category：候选 `RED_FLAG`
- 仅描述可观察现象，不以“低灌注”“缺氧”“循环衰竭”等病理机制命名
- 必须保留肤色、光线、观察部位和远程成像质量限制
- 远程阴性观察不得作为充分排除依据

### A-RS-03 急性局灶神经功能异常

候选定义：突然发生的局灶性面部或肢体无力、突发单侧/局灶感觉异常，以及突发言语/语言异常，应进入必须显式保留的安全关键信号集合。

来源依据：沿用 v0.1 的 NICE NG127 与 NHS Stroke sources。

结构建议：
- category：`MUST_NOT_MISS` +/or `RED_FLAG`
- onset / side / focality / temporality 为关键字段
- 慢性、双侧或非局灶感觉异常不得仅因“麻木”字样自动进入本候选
- 症状已经缓解不得自动转为 negative

### A-RS-04 急性高危胸痛样表现

候选定义：突然或持续的胸部压迫/紧缩样不适，特别是伴放射不适、出汗、恶心、头晕或呼吸困难时，应作为需要显式考虑急性冠脉事件的安全证据候选。

来源依据：沿用 v0.1 的 NHS Chest pain / Heart attack sources。

结构建议：
- category：`MUST_NOT_MISS`
- 名称与定义保持现象层，不使用“缺血性”作为 Evidence/Semantics 本名
- “需考虑急性冠脉事件”不等价于已确诊
- 非典型或类似消化不良的表现不得自动排除

### A-RS-05 快速进展的过敏反应伴气道/呼吸/循环受损

候选定义：快速发生、涉及气道、呼吸或循环的严重全身过敏反应特征，应作为安全关键候选；皮肤/黏膜表现常见但不是绝对必需条件。

来源依据：沿用 v0.1 的 NICE NG258。

结构建议：
- category：`RED_FLAG`
- 时间起始与可能诱因作为 provenance / clinical context
- 不得把非过敏性 ABC 受损自动纳入本候选
- 未观察到皮肤表现不得自动转为阴性

### A-RS-06 成人疑似脓毒症场景中的生理安全信号

候选定义：仅在来源支持的成人疑似脓毒症上下文中，才允许使用对应生理安全信号；本层不保存可执行阈值，也不把这些信号复制为全局生命体征红旗。

来源依据：沿用 v0.1 的 NICE NG253。

结构建议：
- category：`VITAL_SIGN_SAFETY_SIGNAL`
- population scope：`age >= 16`
- context prerequisite：`suspected sepsis`
- 场景锁定为来源对应的 community / custodial 等非急性医院场景
- 妊娠/近期妊娠继续排除在本版之外
- `COMBINATION_SIGNAL` 不在 A 层预分类；组合关系留给后续 Rule Pack
- 任何具体阈值均不写入本文件

## 4. v0.1 → v0.2 修订摘要

```text
A-RS-01 = direction retained; missingness/remote-negative semantics hardened
A-RS-02 = split into A-RS-02A + A-RS-02B; mechanism naming removed
A-RS-02A = second-review scope tightened to explicit source contexts
A-RS-03 = sensory abnormality narrowed to sudden focal/unilateral scope
A-RS-04 = ischemic causal naming removed
A-RS-05 = direction retained; missingness boundary hardened
A-RS-06 = suspected-sepsis/source scope locked; COMBINATION_SIGNAL removed from A
```

## 5. 再审状态

| Candidate | second-round verdict | current action | Medical confirmation | Production use |
|---|---|---|---|---|
| A-RS-01 | APPROVE | retained | COMPLETE_FOR_THIS_ROUND | NO |
| A-RS-02A | REVISE | source-context scope tightened | REQUIRED | NO |
| A-RS-02B | APPROVE | retained | COMPLETE_FOR_THIS_ROUND | NO |
| A-RS-03 | APPROVE | retained | COMPLETE_FOR_THIS_ROUND | NO |
| A-RS-04 | APPROVE | retained | COMPLETE_FOR_THIS_ROUND | NO |
| A-RS-05 | APPROVE | retained | COMPLETE_FOR_THIS_ROUND | NO |
| A-RS-06 | APPROVE | retained | COMPLETE_FOR_THIS_ROUND | NO |

## 6. 当前结论

```text
A Clinical Risk Semantics v0.2
= SECOND_REVIEW_COMPLETE
/ ONE_TARGETED_SCOPE_REVISION_APPLIED
/ MEDICAL_OWNER_CONFIRMATION_REQUIRED_FOR_A-RS-02A
/ PACKAGE_NOT_APPROVED
/ NOT_FOR_PRODUCTION
```
