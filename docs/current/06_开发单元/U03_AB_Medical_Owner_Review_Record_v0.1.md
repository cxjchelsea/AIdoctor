# U03 A/B Medical Owner Review Record v0.1

> 角色：对 A/B v0.1 真实医学内容草案的本轮 Medical Owner Review 记录。  
> 审核对象：`U03_Clinical_Risk_Semantics_Content_Draft_v0.1.md`、`U03_Evidence_Catalog_Content_Draft_v0.1.md`。  
> 对照材料：`U03_AB_Medical_Review_Recommendation_v0.1.md`、`U03_AB_Source_Review_v0.1.md`、`U03_AB_Revision_Status_v0.2.md`。  
> 状态：`REVIEW_RECORD_COMPLETE / PACKAGE_NOT_APPROVED / NOT_FOR_PRODUCTION / C_CONTENT_NOT_AUTHORIZED`

本文件是当前 A/B v0.1 的正式审核记录，取代 `U03_AB_Medical_Review_Recommendation_v0.1.md` 作为条目计数与下一轮修订依据。它不替代后续对 v0.2 修订稿的再审，也不构成整包医学批准或生产授权。

---

## 1. 审核规则

每条候选只允许：

```text
APPROVE
REVISE
REJECT
NEED_MORE_SOURCE
```

本轮 `APPROVE` 的含义：

```text
本轮接受当前定义方向
!= 整包 Medical Owner Approval
!= Production Eligibility
!= 可进入 C Rule Pack
```

判定重点：
1. 该类表现是否可作为高安全影响证据，而不是最终 HIGH_RISK；
2. 名称是否夹带病因推断；
3. 适用范围是否宽于来源；
4. 成人、儿童、孕产妇是否被混用；
5. 是否存在“没有发现 = 安全”的隐含语义；
6. B 是否越权决定风险等级或写入可执行阈值。

C/D/E/F 本轮只确认仍为 schema，不做临床裁决。

---

## 2. 整包结论

```text
A Review = COMPLETE
A APPROVE = 2
A REVISE = 4
A REJECT = 0
A NEED_MORE_SOURCE = 0

B Review = COMPLETE
B APPROVE = 4
B REVISE = 6
B REJECT = 0
B NEED_MORE_SOURCE = 1

A/B Package Approval = NOT_COMPLETE
A/B Production Eligibility = NO
C Rule Pack Clinical Content = BLOCKED
CD-07 Implementation Readiness = BLOCKED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

总裁决：A/B v0.1 不能整包批准。下一动作是按修订任务单产出 A/B Content Draft v0.2，而不是进入 C。

---

## 3. A / Clinical Risk Semantics

| Candidate | 当前名称 | Verdict | 原因 |
|---|---|---|---|
| A-RS-01 | 急性严重呼吸受损 | APPROVE | NHS 支持严重呼吸困难 / gasping / 无法说话；名称中性，未升格为诊断或 HIGH_RISK。是否拆分可留到后续，不阻塞本条。 |
| A-RS-02 | 中枢低灌注/严重缺氧的外观或认知异常 | REVISE | 意识改变与肤色异常均有来源，但不能捆成一条，更不能在名称中写入病理机制。须拆条并去掉“低灌注/缺氧”。 |
| A-RS-03 | 急性局灶神经功能缺损 | REVISE | 突发局灶无力、言语障碍方向成立；“麻木”未锁死突发 + 局灶 + 单侧，会吸入慢性或非局灶感觉异常。 |
| A-RS-04 | 急性缺血性胸痛样表现 | REVISE | NHS 支持压迫 / 紧缩 / 放射模式，但“缺血性”是病因先验。须改中性名，并保持“需考虑 ACS ≠ 已确诊”。 |
| A-RS-05 | 快速进展的过敏反应伴气道/呼吸/循环受损 | APPROVE | NG258 支持快速 ABC 受累；皮肤不是必需。名称略偏综合征，但与来源构成本身一致。 |
| A-RS-06 | 疑似感染背景下的成人脓毒症高风险生理信号 | REVISE | 阈值留给 C 正确；“疑似或确认感染”“社区/非急性”宽于 NG253 的 suspected sepsis + community/custodial。妊娠排除正确。`COMBINATION_SIGNAL` 不应在 A 预写成分类。 |

---

## 4. B / Evidence Catalog

| evidence_id | 当前名称 | Category | Verdict | 原因 |
|---|---|---|---|---|
| EV-RF-RESP-001 | 严重呼吸困难伴言语受限 | RED_FLAG | APPROVE | 定义中性；远程阴性不可排除已写明。不必现在拆条。 |
| EV-RF-NEURO-001 | 新发意识或认知改变 | RED_FLAG | NEED_MORE_SOURCE | 作全局红旗时，最强来源仍是脓毒症与呼吸困难页。全局使用须补通用急性病来源；否则收窄为感染 / 急诊上下文。 |
| EV-RF-CIRC-001 | 发绀或明显灰白/苍白外观 | RED_FLAG | REVISE | 现象层成立，但 CIRC 编码仍暗示循环机制；远程 / 肤色限制须硬化为字段，不能只写在备注。 |
| EV-MNM-NEURO-001 | 突发面部/肢体无力或麻木 | MUST_NOT_MISS | REVISE | 无力可保留；麻木须改为突发单侧 / 局灶感觉异常，onset/side 必填。 |
| EV-MNM-NEURO-002 | 突发言语或语言异常 | MUST_NOT_MISS | APPROVE | NG127 / NHS FAST 直接支持；短暂缓解仍保留历史阳性。 |
| EV-MNM-CARD-001 | 急性缺血性胸痛样表现 | MUST_NOT_MISS | REVISE | 模式支持 MUST_NOT_MISS；名称必须去掉“缺血性”。生产前还需专业指南 / 中国路径。 |
| EV-RF-ALLERGY-001 | 快速进展的过敏反应伴气道/呼吸/循环受损 | RED_FLAG | APPROVE | 皮肤非必需正确。不得把非过敏性 ABC 受损编入本条。 |
| EV-VS-SEPSIS-001 | 疑似感染背景下异常呼吸频率信号 | VITAL_SIGN_SAFETY_SIGNAL | REVISE | 概念留 B、数值留 C 正确；上下文须锁“疑似脓毒症”，不得写成泛感染。 |
| EV-VS-SEPSIS-002 | 疑似感染背景下异常收缩压信号 | VITAL_SIGN_SAFETY_SIGNAL | REVISE | 同上。相对基线下降的可用性属于 C，B 不得写死数值。 |
| EV-VS-SEPSIS-003 | 疑似感染背景下明显心动过速信号 | VITAL_SIGN_SAFETY_SIGNAL | REVISE | 同上。禁止复制为全局生命体征红旗。 |
| EV-RF-SEPSIS-001 | 疑似感染背景下不褪色瘀点/紫癜样皮疹 | RED_FLAG | APPROVE | NG253 直接支持，定义已锁脓毒症场景。名称仍写“疑似感染”，修订时对齐名称，不构成本轮否决。 |

没有条目被 REJECT。没有条目应在本轮写成 C Rule Pack。三条 VS 若写入 ≥25、≤90、>130 等可执行阈值，即构成 Evidence 越权。

---

## 5. 对上一轮 Recommendation 的处置

`U03_AB_Medical_Review_Recommendation_v0.1.md` 只作为审稿意见，不作为本轮最终计数。

继续成立：
- 病因先验必须从 Evidence / Semantics 名称中移除；
- 全局红旗与专病红旗必须分开；
- B 不承载可执行阈值；
- NICE / NHS 不能直接当作中国生产规则；
- A-RS-01、A-RS-05、EV-RF-RESP-001、EV-MNM-NEURO-002、EV-RF-ALLERGY-001、EV-RF-SEPSIS-001 的批准方向。

本轮加严：

| 对象 | Recommendation | 本记录 | 加严原因 |
|---|---|---|---|
| A-RS-03 | APPROVE | REVISE | 麻木过宽已在上一轮备注中出现，但定义正文未收窄，本轮卡住。 |
| EV-VS-SEPSIS-001 / 002 / 003 | APPROVE | REVISE | 审稿理由写“疑似脓毒症”，正文写“疑似感染”。疑似感染 ≠ 疑似脓毒症。 |

上一轮未充分记录的问题：
1. `U03_AB_Revision_Status_v0.2.md` 声称 revision complete，但 v0.1 正文仍保留病因命名与过宽 scope；
2. A-RS-02 必须拆条，不是只改名；
3. Source Review 比 Recommendation 更松，来源通过不等于语义通过；
4. A 稿对 UNKNOWN ≠ NO 的硬化弱于 B；
5. `COMBINATION_SIGNAL` 未进入 B 是正确边界，A-RS-06 不应预写该分类。

---

## 6. 过程与来源发现

### 6.1 修订状态与正文不一致

`U03_AB_Revision_Status_v0.2.md` 此前声称 A/B revision pass complete。该声称不被本轮接受：v0.1 内容稿仍包含“中枢低灌注/严重缺氧”“急性缺血性胸痛样表现”，以及“疑似或确认感染 / 疑似感染”等过宽表述。

```text
Previous Revision Status Claim = SUPERSEDED
Content Draft Alignment = NOT_REFLECTED
Medical Owner Approval = NOT_COMPLETE
```

### 6.2 来源审查的使用方式

`U03_AB_Source_Review_v0.1.md` 仍可作为“是否有出处”的记录，不作为本轮语义批准。

来源层成立：
- RESP / 过敏 / 言语异常 / 脓毒症皮疹有直接支持；
- 儿科、孕产被正确排除；
- 生命体征数值属于 C；
- 中国本地化仍是后续 release 门禁。

来源层仍须收紧：
- NICE NG253 不得从 suspected sepsis 扩大为泛感染或全局生命体征；
- NHS 患者页可支撑第一轮安全候选，不能单独做生产规则；
- 卒中页的麻木不得泛化为任何面部 / 肢体麻木；
- 专病意识改变来源不得自动升为全局 RED_FLAG。

面向中国的生产释放前，胸痛 / ACS、卒中 / TIA、成人脓毒症、过敏急救路径必须补中国指南或本地路径；儿科与孕产必须另建 source pack。

---

## 7. 五类风险检查

| 风险 | 本轮结论 |
|---|---|
| 病因先验 | 仍存在于 A-RS-02、A-RS-04、EV-MNM-CARD-001。 |
| scope 泛化 | 仍存在于 A-RS-06 与三条 VS；EV-RF-NEURO-001 有全局化倾向。 |
| UNKNOWN 被当 NO | 原则已写，尚未成为每条 entry 的硬字段。 |
| Evidence 越权 | 未直接写 HIGH_RISK；名称层仍有诊断暗示。数值阈值尚未写入 B，必须继续禁止。 |
| 地区 / 人群越界 | 成人范围声明正确；NICE / NHS 尚未完成本地化，不能作为中国生产规则。 |

---

## 8. 结构文件确认

以下文件本轮只确认结构合理，不做临床裁决：

```text
U03_Safety_Critical_Risk_Rule_Pack_Schema.md
U03_D09_Clinical_Policy_Table_Schema.md
U03_Knowledge_Release_Manifest_Schema.md
U03_Risk_EvalSet_Safety_Suite_Schema.md
U03_Evidence_Catalog_Schema.md
U03_Clinical_Risk_Semantics.md
U03_Clinical_Input_Package_Spec.md
U03_Clinical_Dependency_Assessment.md
```

结构边界保持有效：

```text
Evidence Definition != Rule
Rule Hit != D09 Disposition
HIGH_RISK != Safety Gate
UNKNOWN / UNMEASURED / NOT_ASKED != NO
NO_HIGH_RISK_SIGNAL != SAFE
```

---

## 9. Gate 与下一步

```text
A/B v0.1 Medical Owner Review Record = COMPLETE
A/B v0.2 Revision = REQUIRED
Medical Owner Approval = NOT_COMPLETE
C / D / E / F Clinical Content = NOT_AUTHORIZED
CD-07 = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

下一步只允许：

```text
按 U03_AB_Revision_Task_v0.2.md 修订 A/B
↓
产出 Content Draft v0.2
↓
对修订条目再审
↓
仅被批准的 B entries 才可进入 C
```

禁止：把本记录的 APPROVE 条目直接提升为 Rule Pack、D09 Policy、Safety Gate 或生产释放。
