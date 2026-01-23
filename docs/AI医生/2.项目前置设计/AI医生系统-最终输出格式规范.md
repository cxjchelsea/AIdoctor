# AI医生系统 - 最终输出格式规范

---

## 一、概述

AI医生系统作为医生角色，最终输出结果根据工作态分为两种格式：

1. **健康管理态（Wellness Mode）**：面向无症状或不确定健康状态的人群，输出统一结果页格式
2. **临床诊疗态（Clinical Mode）**：面向疑似/明确有病的患者，输出终点结论包格式（四要素）

两种格式在工作态内统一，符合各自业务场景，便于前端统一展示。

---

## 二、健康管理态（Wellness Mode）最终输出格式

### 2.1 顶层字段说明

| 字段路径 | 数据类型 | 必填 | 说明 | 备注 |
|---------|---------|------|------|------|
| `unifiedResult`<br/>（统一结果页） | Object | 是 | 统一结果页核心内容 | 所有分支收敛为同一种结果展示结构 |
| `unifiedResult.summary`<br/>（一句话总结） | String | 是 | 一句话总结 | 总结本次咨询的核心建议 |
| `unifiedResult.recommendedActions`<br/>（建议事项清单） | Array\<String\> | 是 | 现在建议做的事（清单） | 明确建议用户执行的事项 |
| `unifiedResult.notRecommendedActions`<br/>（不建议事项清单） | Array\<String\> | 是 | 暂时不建议做的事（清单） | 明确不建议的事项及理由 |
| `unifiedResult.nextReviewTime`<br/>（下次复查时间） | String | 是 | 下一次复查/更新时间点 | 明确的随访时间 |
| `unifiedResult.exitConditions`<br/>（退出条件） | Array\<String\> | 是 | 退出A路径条件提示 | 出现不适/危险信号→线下评估 |
| `wellnessPlan`<br/>（健康管理计划） | Object | 否 | 健康管理计划 | 包含风险管理、生活方式建议、随访计划等 |
| `wellnessPlan.riskManagement`<br/>（风险识别与管理） | Array\<Object\> | 否 | 风险识别与管理建议 | 详见2.2.1节 |
| `wellnessPlan.lifestyleAdvice`<br/>（生活方式建议） | Array\<String\> | 否 | 生活方式建议 | 建议列表 |
| `wellnessPlan.followUpPlan`<br/>（随访计划） | Array\<Object\> | 否 | 随访计划 | 详见2.2.2节 |
| `wellnessPlan.reassurance`<br/>（安抚与解释） | String | 否 | 安抚与解释文本 | 安抚性解释 |
| `wellnessPlan.upgradeConditions`<br/>（升级条件） | Array\<String\> | 否 | 升级到临床诊疗态的条件 | 触发条件列表 |
| `wellnessPlan.wellnessScreeningPath`<br/>（健康筛查路径） | Object | 否 | 健康筛查路径执行结果 | A路径（A1-A5）的执行结果 |
| `wellnessPlan.wellnessScreeningPath.demandType`<br/>（需求类型） | Integer | 是 | 需求类型 | 1-筛查建议, 2-健康目标管理, 3-计划性健康需求 |
| `followUpSchedule`<br/>（随访计划详情） | Object | 否 | 随访计划详情 | 详见2.2.3节 |

### 2.2 嵌套结构说明

#### 2.2.1 风险识别与管理（riskManagement）

| 字段名称 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `riskType`<br/>（风险类型） | String | 是 | 风险类型 | "心血管风险" |
| `riskLevel`<br/>（风险等级） | String | 是 | 风险等级（L1-L4） | "L1" / "L2" / "L3" / "L4" |
| `managementAdvice`<br/>（管理建议） | String | 是 | 管理建议 | "建议控制血压，定期监测" |

#### 2.2.2 随访计划（followUpPlan）

| 字段名称 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `followUpType`<br/>（随访类型） | String | 是 | 随访类型 | "筛查建议" / "目标管理" / "计划性需求" |
| `timing`<br/>（随访时间） | String | 是 | 随访时间 | "3个月后" |
| `purpose`<br/>（随访目的） | String | 是 | 随访目的 | "复查血压，评估控制效果" |

#### 2.2.3 随访计划详情（followUpSchedule）

| 字段路径 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `followUpType`<br/>（随访类型） | String | 是 | 随访类型 | "筛查建议" |
| `followUpRules.defaultTime`<br/>（默认随访时间） | String | 是 | 默认随访时间 | "3个月后" |
| `followUpRules.reviewTrigger`<br/>（复评触发条件） | String | 是 | 复评触发条件 | "出现异常指标时" |
| `followUpRules.reminderTemplates`<br/>（随访提醒话术） | Array\<String\> | 是 | 随访提醒话术 | ["提醒您进行血压复查"] |

### 2.3 JSON Schema 完整结构

```json
{
  "type": "object",
  "required": ["unifiedResult"],
  "properties": {
    "unifiedResult": {
      "type": "object",
      "description": "统一结果页核心内容（所有分支收敛为同一种结果展示结构）",
      "required": ["summary", "recommendedActions", "notRecommendedActions", "nextReviewTime", "exitConditions"],
      "properties": {
        "summary": {
          "type": "string",
          "description": "一句话总结本次咨询的核心建议"
        },
        "recommendedActions": {
          "type": "array",
          "description": "现在建议做的事（清单）",
          "items": {
            "type": "string",
            "description": "建议事项"
          }
        },
        "notRecommendedActions": {
          "type": "array",
          "description": "暂时不建议做的事（清单）",
          "items": {
            "type": "string",
            "description": "不建议的事项及理由"
          }
        },
        "nextReviewTime": {
          "type": "string",
          "description": "下一次复查/更新时间点"
        },
        "exitConditions": {
          "type": "array",
          "description": "退出A路径条件提示（出现不适/危险信号→线下评估）",
          "items": {
            "type": "string",
            "description": "退出条件描述"
          }
        }
      }
    },
    "wellnessPlan": {
      "type": "object",
      "description": "健康管理计划（包含风险管理、生活方式建议、随访计划等）",
      "properties": {
        "riskManagement": {
          "type": "array",
          "description": "风险识别与管理建议",
          "items": {
            "type": "object",
            "required": ["riskType", "riskLevel", "managementAdvice"],
            "properties": {
              "riskType": {
                "type": "string",
                "description": "风险类型（如：心血管风险、代谢风险等）"
              },
              "riskLevel": {
                "type": "string",
                "description": "风险等级（L1-L4）",
                "enum": ["L1", "L2", "L3", "L4"]
              },
              "managementAdvice": {
                "type": "string",
                "description": "管理建议"
              }
            }
          }
        },
        "lifestyleAdvice": {
          "type": "array",
          "description": "生活方式建议",
          "items": {
            "type": "string",
            "description": "生活方式建议项"
          }
        },
        "followUpPlan": {
          "type": "array",
          "description": "随访计划",
          "items": {
            "type": "object",
            "required": ["followUpType", "timing", "purpose"],
            "properties": {
              "followUpType": {
                "type": "string",
                "description": "随访类型（筛查建议/目标管理/计划性需求）"
              },
              "timing": {
                "type": "string",
                "description": "随访时间（如：3个月后）"
              },
              "purpose": {
                "type": "string",
                "description": "随访目的"
              }
            }
          }
        },
        "reassurance": {
          "type": "string",
          "description": "安抚与解释文本"
        },
        "upgradeConditions": {
          "type": "array",
          "description": "升级到临床诊疗态的条件",
          "items": {
            "type": "string",
            "description": "触发条件（如：如出现持续胸痛）"
          }
        },
        "wellnessScreeningPath": {
          "type": "object",
          "description": "健康筛查路径执行结果（A路径A1-A5的执行结果）",
          "required": ["demandType"],
          "properties": {
            "demandType": {
              "type": "integer",
              "description": "需求类型：1-筛查建议, 2-健康目标管理, 3-计划性健康需求",
              "enum": [1, 2, 3, 4]
            }
          }
        }
      }
    },
    "followUpSchedule": {
      "type": "object",
      "description": "随访计划详情",
      "properties": {
        "followUpType": {
          "type": "string",
          "description": "随访类型"
        },
        "followUpRules": {
          "type": "object",
          "description": "随访规则",
          "properties": {
            "defaultTime": {
              "type": "string",
              "description": "默认随访时间"
            },
            "reviewTrigger": {
              "type": "string",
              "description": "复评触发条件"
            },
            "reminderTemplates": {
              "type": "array",
              "description": "随访提醒话术",
              "items": {
                "type": "string",
                "description": "提醒话术模板"
              }
            }
          }
        }
      }
    }
  }
}
```

### 2.4 实际示例

```json
{
  "unifiedResult": {
    "summary": "根据您的健康档案和年龄，建议进行常规体检筛查，重点关注心血管和代谢指标",
    "recommendedActions": [
      "建议每年进行一次全面体检",
      "建议每3个月监测一次血压",
      "建议进行血脂、血糖检查",
      "建议保持规律运动，每周至少150分钟中等强度运动"
    ],
    "notRecommendedActions": [
      "目前不建议进行CT检查（无相关指征）",
      "不建议过度筛查（避免不必要的医疗资源浪费）"
    ],
    "nextReviewTime": "3个月后复查血压，6个月后全面体检",
    "exitConditions": [
      "如出现持续胸痛、呼吸困难等症状，请立即就医",
      "如血压持续升高（>140/90），请及时就医"
    ]
  },
  "wellnessPlan": {
    "riskManagement": [
      {
        "riskType": "心血管风险",
        "riskLevel": "L3",
        "managementAdvice": "建议控制血压，定期监测，保持健康生活方式"
      }
    ],
    "lifestyleAdvice": [
      "建议规律运动，每周至少150分钟中等强度运动",
      "建议低盐饮食，每日盐摄入量<6g",
      "建议戒烟限酒，保持充足睡眠"
    ],
    "followUpPlan": [
      {
        "followUpType": "筛查建议",
        "timing": "3个月后",
        "purpose": "复查血压，评估控制效果"
      }
    ],
    "reassurance": "您目前的健康状况良好，通过定期监测和健康管理，可以有效控制风险",
    "upgradeConditions": [
      "如出现持续胸痛",
      "如症状加重",
      "如识别到风险信号（红旗信号）"
    ],
    "wellnessScreeningPath": {
      "demandType": 1,
      "demandTypeName": "筛查建议"
    }
  },
  "followUpSchedule": {
    "followUpType": "筛查建议",
    "followUpRules": {
      "defaultTime": "3个月后",
      "reviewTrigger": "出现异常指标时",
      "reminderTemplates": [
        "提醒您进行血压复查",
        "提醒您进行年度体检"
      ]
    }
  }
}
```

---

## 三、临床诊疗态（Clinical Mode）最终输出格式

### 3.1 顶层字段说明

| 字段路径 | 数据类型 | 必填 | 说明 | 备注 |
|---------|---------|------|------|------|
| `diagnosisId`<br/>（诊断ID） | String | 是 | 诊断ID | 唯一标识本次诊断 |
| `status`<br/>（诊断状态） | Enum | 是 | 诊断状态 | "completed" / "collecting" / "questioning" / "analyzing" |
| `conclusion`<br/>（诊断结论） | Object | 是 | 诊断结论（三层分层） | 终点结论包要素1，详见3.2.1节 |
| `exclusionStatus`<br/>（排除项状态） | Object | 是 | 必须排除项状态 | 终点结论包要素2，详见3.2.2节 |
| `keyEvidence`<br/>（关键依据） | Object | 是 | 关键依据 | 终点结论包要素3，详见3.2.3节 |
| `actionAndFollowUp`<br/>（行动与随访） | Object | 是 | 行动与随访 | 终点结论包要素4，详见3.2.4节 |
| `examinationSuggestions`<br/>（检查建议） | Object | 是 | 检查建议 | 详见3.2.5节 |
| `medicalAdvice`<br/>（就医建议） | Object | 是 | 就医建议 | 详见3.2.6节 |
| `createdAt`<br/>（创建时间） | String | 是 | 创建时间 | ISO 8601格式 |
| `completedAt`<br/>（完成时间） | String | 否 | 完成时间 | ISO 8601格式 |

### 3.2 嵌套结构说明

#### 3.2.1 诊断结论（conclusion）- 三层分层结构

| 字段路径 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `type`<br/>（结论类型） | Enum | 是 | 可确证/不可确证 | "confirmable" / "unconfirmable" |
| `primaryHypothesis`<br/>（首要假设） | Object | 是 | 首要假设（1个） | 最可能的诊断，详见3.3.1节 |
| `alternativeDiagnoses`<br/>（主要备选诊断） | Array\<Object\> | 是 | 主要备选诊断（1-2个） | 其他可能的诊断，详见3.3.1节 |
| `mustExcludeDiagnosis`<br/>（必须排除的高危诊断） | Object | 否 | 必须排除的高危诊断（0-1个） | 高危诊断，详见3.3.1节 |

#### 3.2.2 必须排除项状态（exclusionStatus）

| 字段路径 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `status`<br/>（排除状态） | Enum | 是 | 排除状态 | "excluded" / "not_excluded" / "need_offline_exclude" |
| `reason`<br/>（排除理由） | String | 是 | 排除理由 | "已通过心电图排除" / "需要线下检查才能排除" |

#### 3.2.3 关键依据（keyEvidence）

| 字段路径 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `positiveEvidence`<br/>（阳性证据） | Array\<String\> | 是 | 阳性证据（支持最可能方向，至少三条） | ["胸痛伴活动后加重", "有高血压史", "心电图显示ST段压低"] |
| `negativeEvidence`<br/>（阴性证据） | Array\<String\> | 是 | 关键阴性证据（排除其他方向） | ["无发热", "无咳嗽"] |

#### 3.2.4 行动与随访（actionAndFollowUp）

| 字段路径 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `immediateAction`<br/>（立即行动） | String | 是 | 立即行动建议 | "建议尽快到心内科就诊，进行心电图和心脏彩超检查" |
| `reviewTimeWindow`<br/>（复评时间窗） | String | 否 | 复评时间窗 | "3天后复评" |
| `upgradeTriggerConditions`<br/>（升级触发条件） | Array\<String\> | 是 | 升级触发条件 | ["如出现持续胸痛", "如出现大汗、恶心等症状"] |

#### 3.2.5 检查建议（examinationSuggestions）

| 字段路径 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `priorityExaminations`<br/>（优先检查） | Array\<Object\> | 是 | 优先检查（高优先级） | 详见3.3.2节 |
| `optionalExaminations`<br/>（可选检查） | Array\<Object\> | 是 | 可选检查（中低优先级） | 详见3.3.2节 |
| `explanation`<br/>（检查说明） | String | 否 | 检查建议说明 | "优先进行心电图检查以排除急性心肌梗死" |

#### 3.2.6 就医建议（medicalAdvice）

| 字段路径 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `department`<br/>（建议科室） | String | 是 | 建议科室 | "心内科" |
| `timing`<br/>（就医时机） | String | 是 | 就医时机 | "建议尽快就诊" / "建议1周内就诊" |
| `preparation.documents`<br/>（准备文档） | Array\<String\> | 否 | 就医准备文档 | ["既往检查报告", "用药清单"] |
| `preparation.questions`<br/>（建议问题） | Array\<String\> | 否 | 建议询问医生的问题 | ["是否需要进一步检查？", "治疗方案是什么？"] |
| `sbarSummary`<br/>（就医摘要） | String | 否 | 就医摘要（SBAR格式） | "患者主诉胸痛3天，活动后加重，有高血压史..." |

### 3.3 深层嵌套结构说明

#### 3.3.1 疾病可能性（DiseasePossibility）

| 字段名称 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `disease`<br/>（疾病名称） | String | 是 | 疾病名称 | "心绞痛" |
| `confidence`<br/>（可能性分数） | Number | 是 | 可能性分数（0-1） | 0.75 |
| `supportingEvidence`<br/>（支持证据） | Array\<String\> | 是 | 支持证据 | ["胸痛", "活动后加重"] |
| `opposingEvidence`<br/>（反对证据） | Array\<String\> | 是 | 反对证据 | ["无发热"] |
| `missingInfo`<br/>（缺失信息） | Array\<String\> | 否 | 缺失信息 | ["需要心电图结果"] |
| `inclusionBasis`<br/>（入选依据） | Array\<String\> | 否 | 入选依据 | ["问题清单中的线索"] |

#### 3.3.2 检查项（ExaminationItem）

| 字段名称 | 数据类型 | 必填 | 说明 | 示例 |
|---------|---------|------|------|------|
| `name`<br/>（检查名称） | String | 是 | 检查名称 | "心电图" |
| `purpose`<br/>（检查目的） | String | 是 | 检查目的 | "评估心脏功能，排除急性心肌梗死" |
| `priority`<br/>（优先级） | Enum | 是 | 优先级 | "high" / "medium" / "low" |
| `reason`<br/>（推荐理由） | String | 是 | 推荐理由 | "排除急性心肌梗死" |

### 3.4 JSON Schema 完整结构

```json
{
  "type": "object",
  "description": "临床诊疗态最终输出格式（终点结论包四要素）",
  "required": ["diagnosisId", "status", "conclusion", "exclusionStatus", "keyEvidence", "actionAndFollowUp", "examinationSuggestions", "medicalAdvice", "createdAt"],
  "properties": {
    "diagnosisId": {
      "type": "string",
      "description": "诊断ID（唯一标识本次诊断）"
    },
    "status": {
      "type": "string",
      "description": "诊断状态：completed-已完成, collecting-收集中, questioning-问诊中, analyzing-分析中, cancelled-已取消",
      "enum": ["completed", "collecting", "questioning", "analyzing", "cancelled"]
    },
    "conclusion": {
      "type": "object",
      "description": "诊断结论（三层分层结构）- 终点结论包要素1",
      "required": ["type", "primaryHypothesis", "alternativeDiagnoses"],
      "properties": {
        "type": {
          "type": "string",
          "description": "可确证/不可确证：confirmable-可确证, unconfirmable-不可确证",
          "enum": ["confirmable", "unconfirmable"]
        },
        "primaryHypothesis": {
          "$ref": "#/definitions/DiseasePossibility",
          "description": "首要假设（1个）- 最可能的诊断"
        },
        "alternativeDiagnoses": {
          "type": "array",
          "description": "主要备选诊断（1-2个）- 其他可能的诊断",
          "items": {
            "$ref": "#/definitions/DiseasePossibility"
          },
          "minItems": 1,
          "maxItems": 2
        },
        "mustExcludeDiagnosis": {
          "$ref": "#/definitions/DiseasePossibility",
          "description": "必须排除的高危诊断（0-1个）- 高危诊断，即使概率不高也必须排除"
        }
      }
    },
    "exclusionStatus": {
      "type": "object",
      "description": "必须排除项状态 - 终点结论包要素2",
      "required": ["status", "reason"],
      "properties": {
        "status": {
          "type": "string",
          "description": "排除状态：excluded-已排除, not_excluded-未排除, need_offline_exclude-需线下排除",
          "enum": ["excluded", "not_excluded", "need_offline_exclude"]
        },
        "reason": {
          "type": "string",
          "description": "排除理由（如：已通过心电图排除 / 需要线下检查才能排除）"
        }
      }
    },
    "keyEvidence": {
      "type": "object",
      "description": "关键依据 - 终点结论包要素3",
      "required": ["positiveEvidence", "negativeEvidence"],
      "properties": {
        "positiveEvidence": {
          "type": "array",
          "description": "阳性证据（支持最可能方向，至少三条）",
          "items": {
            "type": "string",
            "description": "支持证据描述"
          },
          "minItems": 3
        },
        "negativeEvidence": {
          "type": "array",
          "description": "关键阴性证据（排除其他方向）",
          "items": {
            "type": "string",
            "description": "排除证据描述"
          }
        }
      }
    },
    "actionAndFollowUp": {
      "type": "object",
      "description": "行动与随访 - 终点结论包要素4",
      "required": ["immediateAction", "upgradeTriggerConditions"],
      "properties": {
        "immediateAction": {
          "type": "string",
          "description": "立即行动建议（如：建议尽快到心内科就诊，进行心电图和心脏彩超检查）"
        },
        "reviewTimeWindow": {
          "type": "string",
          "description": "复评时间窗（如：3天后复评）"
        },
        "upgradeTriggerConditions": {
          "type": "array",
          "description": "升级触发条件（如：如出现持续胸痛、如出现大汗、恶心等症状）",
          "items": {
            "type": "string",
            "description": "触发条件描述"
          }
        }
      }
    },
    "examinationSuggestions": {
      "type": "object",
      "description": "检查建议",
      "required": ["priorityExaminations", "optionalExaminations"],
      "properties": {
        "priorityExaminations": {
          "type": "array",
          "description": "优先检查（高优先级）",
          "items": {
            "$ref": "#/definitions/ExaminationItem"
          }
        },
        "optionalExaminations": {
          "type": "array",
          "description": "可选检查（中低优先级）",
          "items": {
            "$ref": "#/definitions/ExaminationItem"
          }
        },
        "explanation": {
          "type": "string",
          "description": "检查建议说明（如：优先进行心电图检查以排除急性心肌梗死）"
        }
      }
    },
    "medicalAdvice": {
      "type": "object",
      "description": "就医建议",
      "required": ["department", "timing"],
      "properties": {
        "department": {
          "type": "string",
          "description": "建议科室（如：心内科）"
        },
        "timing": {
          "type": "string",
          "description": "就医时机（如：建议尽快就诊 / 建议1周内就诊）"
        },
        "preparation": {
          "type": "object",
          "description": "就医准备",
          "properties": {
            "documents": {
              "type": "array",
              "description": "就医准备文档（如：既往检查报告、用药清单）",
              "items": {
                "type": "string",
                "description": "文档名称"
              }
            },
            "questions": {
              "type": "array",
              "description": "建议询问医生的问题（如：是否需要进一步检查？治疗方案是什么？）",
              "items": {
                "type": "string",
                "description": "问题内容"
              }
            }
          }
        },
        "sbarSummary": {
          "type": "string",
          "description": "就医摘要（SBAR格式：Situation-情况, Background-背景, Assessment-评估, Recommendation-建议）"
        }
      }
    },
    "createdAt": {
      "type": "string",
      "format": "date-time",
      "description": "创建时间（ISO 8601格式）"
    },
    "completedAt": {
      "type": "string",
      "format": "date-time",
      "description": "完成时间（ISO 8601格式，可选）"
    }
  },
  "definitions": {
    "DiseasePossibility": {
      "type": "object",
      "description": "疾病可能性（用于三层分层结构）",
      "required": ["disease", "confidence", "supportingEvidence", "opposingEvidence"],
      "properties": {
        "disease": {
          "type": "string",
          "description": "疾病名称（如：心绞痛）"
        },
        "confidence": {
          "type": "number",
          "description": "可能性分数（0-1，如：0.75表示75%可能性）",
          "minimum": 0,
          "maximum": 1
        },
        "supportingEvidence": {
          "type": "array",
          "description": "支持证据（如：胸痛、活动后加重）",
          "items": {
            "type": "string",
            "description": "支持证据描述"
          }
        },
        "opposingEvidence": {
          "type": "array",
          "description": "反对证据（如：无发热）",
          "items": {
            "type": "string",
            "description": "反对证据描述"
          }
        },
        "missingInfo": {
          "type": "array",
          "description": "缺失信息（如：需要心电图结果）",
          "items": {
            "type": "string",
            "description": "缺失信息描述"
          }
        },
        "inclusionBasis": {
          "type": "array",
          "description": "入选依据（问题清单中的线索）",
          "items": {
            "type": "string",
            "description": "入选依据描述"
          }
        }
      }
    },
    "ExaminationItem": {
      "type": "object",
      "description": "检查项",
      "required": ["name", "purpose", "priority", "reason"],
      "properties": {
        "name": {
          "type": "string",
          "description": "检查名称（如：心电图）"
        },
        "purpose": {
          "type": "string",
          "description": "检查目的（如：评估心脏功能，排除急性心肌梗死）"
        },
        "priority": {
          "type": "string",
          "description": "优先级：high-高, medium-中, low-低",
          "enum": ["high", "medium", "low"]
        },
        "reason": {
          "type": "string",
          "description": "推荐理由（如：排除急性心肌梗死）"
        }
      }
    }
  }
}
```

### 3.5 实际示例

```json
{
  "diagnosisId": "diag_123456",
  "status": "completed",
  "conclusion": {
    "type": "unconfirmable",
    "primaryHypothesis": {
      "disease": "心绞痛",
      "confidence": 0.75,
      "supportingEvidence": [
        "胸痛伴活动后加重",
        "有高血压史",
        "气短"
      ],
      "opposingEvidence": [
        "无发热",
        "无咳嗽"
      ],
      "missingInfo": [
        "需要心电图结果",
        "需要心肌酶检查结果"
      ],
      "inclusionBasis": [
        "主诉胸痛",
        "活动后症状加重"
      ]
    },
    "alternativeDiagnoses": [
      {
        "disease": "焦虑症",
        "confidence": 0.55,
        "supportingEvidence": [
          "阵发性胸痛",
          "症状与情绪相关"
        ],
        "opposingEvidence": [
          "无焦虑病史",
          "症状与活动相关"
        ],
        "missingInfo": [],
        "inclusionBasis": [
          "阵发性症状"
        ]
      }
    ],
    "mustExcludeDiagnosis": {
      "disease": "急性心肌梗死",
      "confidence": 0.30,
      "supportingEvidence": [],
      "opposingEvidence": [],
      "missingInfo": [
        "需要心电图排除ST段抬高",
        "需要心肌酶检查排除心肌坏死"
      ],
      "inclusionBasis": [
        "高危诊断，必须排除"
      ]
    }
  },
  "exclusionStatus": {
    "status": "need_offline_exclude",
    "reason": "需要心电图和心肌酶检查才能排除急性心肌梗死"
  },
  "keyEvidence": {
    "positiveEvidence": [
      "胸痛伴活动后加重（强证据）",
      "有高血压史（中证据）",
      "症状持续时间3天（中证据）"
    ],
    "negativeEvidence": [
      "无发热（排除感染性疾病）",
      "无咳嗽（排除呼吸系统疾病）"
    ]
  },
  "actionAndFollowUp": {
    "immediateAction": "建议尽快到心内科就诊，进行心电图和心脏彩超检查，以明确诊断并排除急性心肌梗死",
    "reviewTimeWindow": "3天后复评，如症状持续或加重，需提前复评",
    "upgradeTriggerConditions": [
      "如出现持续胸痛（>30分钟）",
      "如出现大汗、恶心、呕吐等症状",
      "如症状加重或出现新症状"
    ]
  },
  "examinationSuggestions": {
    "priorityExaminations": [
      {
        "name": "心电图",
        "purpose": "评估心脏功能，排除急性心肌梗死",
        "priority": "high",
        "reason": "排除急性心肌梗死，评估心脏电活动"
      },
      {
        "name": "心肌酶检查",
        "purpose": "排除心肌坏死",
        "priority": "high",
        "reason": "排除急性心肌梗死"
      }
    ],
    "optionalExaminations": [
      {
        "name": "心脏彩超",
        "purpose": "评估心脏结构和功能",
        "priority": "medium",
        "reason": "进一步评估心脏功能"
      }
    ],
    "explanation": "优先进行心电图和心肌酶检查以排除急性心肌梗死，这是必须排除的高危诊断"
  },
  "medicalAdvice": {
    "department": "心内科",
    "timing": "建议尽快就诊（24小时内）",
    "preparation": {
      "documents": [
        "既往检查报告（如有）",
        "用药清单",
        "身份证、医保卡"
      ],
      "questions": [
        "是否需要进一步检查？",
        "治疗方案是什么？",
        "需要注意什么？"
      ]
    },
    "sbarSummary": "患者主诉胸痛3天，活动后加重，伴有气短。既往有高血压史。目前考虑心绞痛可能性大，但需要排除急性心肌梗死。建议尽快到心内科就诊，进行心电图和心肌酶检查。"
  },
  "createdAt": "2024-01-01T10:00:00Z",
  "completedAt": "2024-01-01T10:05:00Z"
}
```

---

## 四、两种格式对比总结

| 对比维度 | 健康管理态（Wellness Mode） | 临床诊疗态（Clinical Mode） |
|---------|---------------------------|---------------------------|
| **适用人群** | 无症状或不确定健康状态的人群 | 疑似/明确有病的患者 |
| **核心输出** | 统一结果页（5要素） | 终点结论包（4要素） |
| **主要结构** | `unifiedResult` + `wellnessPlan` | `conclusion` + `exclusionStatus` + `keyEvidence` + `actionAndFollowUp` |
| **重点内容** | 建议清单、随访计划、风险识别 | 诊断结论、证据链、行动建议 |
| **复杂度** | 相对简单，以建议和计划为主 | 相对复杂，包含三层分层诊断结构 |
| **可确证性** | 不涉及诊断确证 | 可确证/不可确证标识 |
