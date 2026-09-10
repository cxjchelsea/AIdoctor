# AIdoctor 患者端证据与引用 UI 契约

> 文档状态：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-30  
> 适用分支：`agent/enterprise-agent-refactoring-plan`  
> 上游设计：`前端与业务迁移.md`、`临床数据与循证智能扩展方案.md`、`成人呼吸道RAG.md`、`总架构与模块设计.md`

---

## 1. 文档目的

本文定义患者端如何消费和展示经过批准的医学结论、确定性安全依据、来源引用、证据冲突、适用限制和信息不足状态。

该契约解决以下问题：

1. 患者端展示什么，不展示什么；
2. `EvidencePack`、`DeliveryPackage` 与患者 UI DTO 如何映射；
3. 每条患者友好 Claim 如何关联医学来源或确定性安全规则；
4. 证据不足、冲突、过期和人群不匹配如何表达；
5. 页面刷新、医生审核、Knowledge Release 回滚后如何保持版本一致；
6. 前端、Java、Python 和 JSON Schema 如何进行 Contract Test。

该契约是 **Business Delivery Contract**，不是 LangGraph State、内部 EvidencePack 或模型原始输出的直接投影。

```text
ClinicalObservation / Triage / EvidencePack / Clinician Review
                         ↓
                  Delivery Composer
                         ↓
            PatientEvidenceView DTO
                         ↓
                    Patient UI
```

---

## 2. 核心原则

### 2.1 Claim 级证据绑定

关键医学结论不能只在页面末尾集中列出参考资料。每个需要证据支持的患者友好 Claim 必须绑定：

- 一个或多个经 Citation Validation 的医学来源；或
- 一个经过版本治理的确定性 Safety/Triage Rule；或
- 一项明确标记的医生审核决定。

### 2.2 患者友好，不等于展示内部推理

患者端可以展示：

- 当前信息摘要；
- 风险等级和下一步行动；
- 结论所依据的患者已提供信息；
- 来源机构、标题、发布日期和适用限制；
- 证据不足、证据冲突和不确定性；
- 医生是否审核。

患者端不得展示：

- Chain-of-Thought 或内部推理草稿；
- 完整 Prompt；
- Provider 请求、响应和模型供应商名称；
- 未批准的内部候选疾病列表；
- 内部 Policy 表达式、阈值实现细节和安全绕过条件；
- 其他患者、租户或无权限数据；
- 许可不允许展示的全文内容。

### 2.3 安全规则与医学文献分开展示

以下两种依据必须在 UI 中采用不同语义：

```text
Safety Rule Basis
回答：为什么系统建议立即就医、升级或停止普通问诊

Medical Evidence Basis
回答：为什么某条医学说明、可能方向或行动建议有循证支持
```

紧急分诊不得等待 Evidence Service 在线返回。即使医学检索失败，确定性红旗和升级提示仍必须展示。

### 2.4 不向患者展示未经校准的数值置信度

患者端默认不展示模型概率、内部评分或 `confidence=0.73` 一类数值。

只能展示经过产品和临床审核的定性状态：

- `依据较充分`；
- `依据有限`；
- `不同来源存在差异`；
- `目前信息不足`；
- `该证据可能不完全适用于您`。

### 2.5 同一 Delivery 版本一致

患者结论、证据卡、行动建议和安全提示必须来自同一：

- `encounter_id`；
- `delivery_version`；
- `cdp_version`；
- `capability_release_id`；
- `knowledge_release_id`（使用医学知识时）；
- `review_decision_version`（经过医生审核时）。

前端不得将不同版本请求结果自行拼接成一个页面。

---

## 3. 展示对象分层

患者端证据展示分为四类：

| 类型 | 用途 | 是否需要医学 Citation |
|---|---|---|
| `SAFETY_RULE` | 红旗、紧急程度、升级条件 | 否，但必须绑定治理后的 Safety Policy 快照 |
| `MEDICAL_EVIDENCE` | 医学说明、可能方向、检查或观察依据 | 是 |
| `CLINICIAN_DECISION` | 医生确认、修改或补充的结论 | 可选，需显示医生已审核 |
| `PATIENT_FACT` | 患者已提供的信息摘要 | 否，必须关联患者输入或资料来源 |

同一张证据卡只能有一个主要 `basis_type`，但可以包含多个支持来源。

---

## 4. 顶层 API 契约

患者端通过以下接口获取最终或阶段性 Delivery：

```text
GET /api/v2/encounters/{encounter_id}/delivery
```

建议响应：

```json
{
  "encounter_id": "enc_123",
  "delivery_id": "del_123",
  "delivery_version": 3,
  "cdp_version": 12,
  "status": "ready",
  "generated_at": "2026-07-30T12:00:00Z",
  "review_status": "clinician_approved",
  "patient_delivery": {
    "summary": {},
    "risk": {},
    "next_actions": [],
    "evidence_sections": [],
    "limitations": [],
    "safety_notice": {}
  },
  "version_snapshot": {
    "capability_release_id": "adult_respiratory_v1@1.0.0",
    "knowledge_release_id": "respiratory_knowledge@2026.07",
    "review_decision_version": 2
  }
}
```

### 4.1 Delivery 状态

```text
preparing
awaiting_clinician
ready
ready_with_limited_evidence
degraded
superseded
cancelled
```

处理规则：

- `preparing`：显示处理中，不展示旧结论冒充新结果；
- `awaiting_clinician`：显示等待医生审核，允许展示已批准的安全提示；
- `ready`：展示完整患者 Delivery；
- `ready_with_limited_evidence`：展示结果，同时突出证据限制；
- `degraded`：只展示确定性安全信息和可安全交付的内容；
- `superseded`：客户端重新获取最新版本；
- `cancelled`：停止轮询并展示取消说明。

---

## 5. JSON Schema 级 DTO

### 5.1 PatientDeliveryView

```typescript
export interface PatientDeliveryView {
  title: string;
  summary: PatientSummaryBlock;
  risk: PatientRiskBlock;
  nextActions: PatientActionItem[];
  evidenceSections: PatientEvidenceSection[];
  limitations: PatientLimitation[];
  safetyNotice: PatientSafetyNotice;
  followUp?: PatientFollowUpBlock;
}
```

### 5.2 PatientEvidenceSection

```typescript
export interface PatientEvidenceSection {
  sectionId: string;
  title: string;
  description?: string;
  displayOrder: number;
  cards: PatientEvidenceCard[];
}
```

推荐首版 Section：

```text
why_this_matters
why_seek_care
medical_information
uncertainty_and_limits
```

### 5.3 PatientEvidenceCard

```typescript
export type EvidenceBasisType =
  | "SAFETY_RULE"
  | "MEDICAL_EVIDENCE"
  | "CLINICIAN_DECISION"
  | "PATIENT_FACT";

export type PatientEvidenceStatus =
  | "SUPPORTED"
  | "PARTIALLY_SUPPORTED"
  | "CONFLICTING"
  | "INSUFFICIENT_EVIDENCE"
  | "POPULATION_MISMATCH"
  | "STALE_SOURCE"
  | "OUT_OF_SCOPE"
  | "NOT_REQUIRED";

export type PatientCertaintyLabel =
  | "STRONGER_BASIS"
  | "LIMITED_BASIS"
  | "SOURCES_DIFFER"
  | "MORE_INFORMATION_NEEDED"
  | "MAY_NOT_FULLY_APPLY"
  | "CLINICIAN_REVIEWED";

export interface PatientEvidenceCard {
  cardId: string;
  claimId: string;
  basisType: EvidenceBasisType;
  patientFriendlyClaim: string;
  rationaleSummary?: string;
  evidenceStatus: PatientEvidenceStatus;
  certaintyLabel?: PatientCertaintyLabel;
  relatedPatientFactIds: string[];
  sources: PatientSourceSummary[];
  conflictSummary?: PatientConflictSummary;
  applicability?: PatientApplicabilitySummary;
  limitations: PatientLimitation[];
  clinicianReviewed: boolean;
  reviewedAt?: string;
  displayPriority: "CRITICAL" | "HIGH" | "NORMAL" | "LOW";
  expandable: boolean;
}
```

### 5.4 PatientSourceSummary

```typescript
export interface PatientSourceSummary {
  citationId: string;
  sourceId: string;
  title: string;
  organization: string;
  publicationDate?: string;
  sourceVersion?: string;
  sourceType:
    | "GUIDELINE"
    | "SYSTEMATIC_REVIEW"
    | "GOVERNMENT_HEALTH_INFORMATION"
    | "PEER_REVIEWED_STUDY"
    | "OTHER_APPROVED_SOURCE";
  applicablePopulation?: string;
  region?: string;
  patientFriendlyExcerpt?: string;
  accessUrl?: string;
  linkPolicy: "DIRECT" | "LANDING_PAGE" | "NO_EXTERNAL_LINK";
  freshnessStatus: "CURRENT" | "REVIEW_DUE" | "STALE" | "UNKNOWN";
}
```

约束：

- `patientFriendlyExcerpt` 必须来自已验证的 `SourceSpan` 或经批准的释义；
- 不能为了展示而生成来源中不存在的新结论；
- `accessUrl` 只在 License、权限和链接安全检查允许时返回；
- 首屏每张卡最多显示 2 个来源，展开后最多显示 5 个；
- 更多来源只显示“还有 N 个已审核来源”，不得无限渲染。

### 5.5 PatientConflictSummary

```typescript
export interface PatientConflictSummary {
  exists: boolean;
  patientFriendlyMessage: string;
  affectedClaimIds: string[];
  requiresClinicianReview: boolean;
}
```

禁止直接向患者展示未经解释的研究结论冲突列表。必须转换为例如：

> 不同权威来源对这一问题的建议存在差异，当前结果采用更保守的处理方式，并建议由医生结合您的具体情况判断。

### 5.6 PatientApplicabilitySummary

```typescript
export interface PatientApplicabilitySummary {
  populationMatch: "MATCH" | "PARTIAL" | "MISMATCH" | "UNKNOWN";
  regionMatch: "MATCH" | "PARTIAL" | "MISMATCH" | "UNKNOWN";
  patientFriendlyMessage?: string;
}
```

### 5.7 PatientLimitation

```typescript
export interface PatientLimitation {
  code:
    | "MISSING_INFORMATION"
    | "INSUFFICIENT_EVIDENCE"
    | "CONFLICTING_EVIDENCE"
    | "POPULATION_MISMATCH"
    | "REGION_MISMATCH"
    | "STALE_SOURCE"
    | "SOURCE_ACCESS_LIMITED"
    | "CLINICIAN_REVIEW_REQUIRED"
    | "CAPABILITY_SCOPE_LIMIT";
  message: string;
  severity: "INFO" | "WARNING" | "CRITICAL";
  userAction?: string;
}
```

### 5.8 PatientSafetyNotice

```typescript
export interface PatientSafetyNotice {
  urgency: "EMERGENCY" | "URGENT" | "ROUTINE" | "SELF_CARE_WITH_MONITORING";
  title: string;
  message: string;
  triggerSummary: string[];
  immediateActions: string[];
  escalationConditions: string[];
  emergencyDisclaimer: string;
  basisType: "SAFETY_RULE" | "CLINICIAN_DECISION";
}
```

Safety Notice 不依赖 Medical RAG 成功才能生成。

---

## 6. EvidencePack 到患者 DTO 的映射

```text
EvidenceClaim.claim_text
→ 经过 Delivery Composer 转换
→ PatientEvidenceCard.patientFriendlyClaim

EvidenceClaim.source_spans
→ Citation Validator
→ PatientSourceSummary

EvidenceClaim.support_level
+ EvidencePack.status
→ PatientEvidenceStatus

ApplicabilityAssessment
→ PatientApplicabilitySummary

EvidenceConflict
→ PatientConflictSummary

EvidencePack.limitations
→ PatientLimitation[]
```

### 6.1 禁止直接映射

以下字段不得直接原样传给患者端：

- 内部 `confidence` 数值；
- Retrieval Score；
- Reranker Score；
- Embedding 相似度；
- 内部 Reason Code；
- 原始 PICO；
- 原始模型解释；
- 未通过 Citation Validation 的 SourceSpan；
- 完整 Knowledge Release 内部清单。

### 6.2 Delivery Composer 校验

生成患者 DTO 前必须校验：

1. Claim 已批准进入 PatientDelivery；
2. 医学 Claim 存在有效 Citation，或被明确标记为证据不足；
3. Safety Claim 绑定有效 Safety Policy 快照；
4. 来源未撤回，或已显示过期/撤回限制；
5. 来源人群和地区限制已转换为患者友好语言；
6. 页面 Claim 不超出来源支持范围；
7. 不包含内部 Prompt、模型输出和 PHI 泄漏；
8. Delivery、CDP、Knowledge 和 Review 版本一致。

---

## 7. UI 展示规则

### 7.1 默认信息层级

```text
1. 风险等级和立即行动
2. 当前信息摘要
3. 为什么给出这一建议
4. 参考依据
5. 不确定性与限制
6. 随访和恶化升级条件
```

紧急场景不得将 Citation 卡片置于立即行动之前。

### 7.2 卡片状态文案

| 状态 | 推荐患者文案 |
|---|---|
| `SUPPORTED` | 该说明有已审核医学来源支持 |
| `PARTIALLY_SUPPORTED` | 现有资料提供部分支持，仍需结合具体情况 |
| `CONFLICTING` | 不同来源存在差异，当前采用更保守的建议 |
| `INSUFFICIENT_EVIDENCE` | 目前没有足够可靠的信息支持更明确的结论 |
| `POPULATION_MISMATCH` | 现有证据可能不完全适用于您的情况 |
| `STALE_SOURCE` | 相关资料需要更新确认，建议由医生复核 |
| `OUT_OF_SCOPE` | 当前系统不支持对此问题作进一步判断 |

### 7.3 展开交互

患者点击“查看依据”后最多展示：

- 来源标题；
- 发布机构；
- 发布日期或版本；
- 适用人群；
- 一段患者友好的原文摘录或释义；
- 访问来源按钮（许可允许时）；
- 局限说明。

不得展示：

- 整篇版权文档；
- 隐藏 Prompt；
- 原始模型响应；
- 未脱敏日志；
- 内部数据库 ID 以外的敏感信息。

### 7.4 空状态与降级

- 没有医学来源但 Safety Rule 已触发：展示安全依据，不显示伪造 Citation；
- RAG 超时：显示“医学依据暂时不可用”，不阻塞紧急提示；
- 来源冲突：展示冲突摘要，并在需要时显示“等待医生审核”；
- 所有 Citation 被撤回：将 Delivery 标记为 `ready_with_limited_evidence` 或重新审核；
- 客户端缓存版本落后：显示更新提示并重新获取，不继续展示旧证据卡。

---

## 8. 权限、隐私和安全

1. 患者只能读取属于自己且 Consent 有效的 Encounter Delivery；
2. DTO 不返回其他患者、医生内部备注或租户信息；
3. 来源摘录必须经过 License 和内容安全检查；
4. 前端日志不得记录患者完整 Claim、报告原文或 SourceSpan 全文；
5. 浏览器 Local Storage 不保存完整 Delivery；
6. 导出、复制和外链行为必须进入 Compliance Audit；
7. 所有外部链接使用允许域名、HTTPS 和防 Open Redirect 校验；
8. `correlation_id` 可用于客服定位，但不得作为鉴权凭证。

---

## 9. 可访问性与多语言

- 风险等级不能只依赖颜色表达；
- 所有图标必须有文本或 `aria-label`；
- 展开/收起支持键盘操作；
- 来源日期使用用户 Locale，同时保留机器可读 ISO 时间；
- Claim 和 Citation 必须绑定同一语言版本；
- 翻译内容必须保留原始 SourceSpan 和翻译版本；
- 紧急提示应兼容屏幕阅读器并优先朗读。

---

## 10. Contract Versioning

建议 Schema：

```text
contracts/ui/patient-delivery-view/v1.json
contracts/ui/patient-evidence-card/v1.json
contracts/ui/patient-source-summary/v1.json
contracts/ui/patient-safety-notice/v1.json
```

版本规则：

- 新增可选字段：Minor；
- 修改枚举语义、删除字段或改变必填性：Major；
- 文案调整但语义不变：不升级 Schema，可升级内容版本；
- Java DTO、Pydantic Model、TypeScript 类型必须由同一 JSON Schema 生成或进行一致性测试。

---

## 11. 测试要求

### 11.1 Contract Test

必须验证：

- JSON Schema 示例可被 Java、Python 和 TypeScript 同时解析；
- 枚举值和必填字段一致；
- 未知字段兼容策略一致；
- Delivery 版本不一致时前端拒绝拼接；
- Patient DTO 不包含禁止字段。

### 11.2 患者端组件测试

覆盖：

- Supported Citation；
- 证据不足；
- 来源冲突；
- 人群不匹配；
- 来源过期；
- RAG 不可用但 Safety 正常；
- 等待医生审核；
- Delivery 更新和旧版本失效；
- 移动端和键盘操作；
- 长标题、无外链和多来源折叠。

### 11.3 安全测试

覆盖：

- XSS 和恶意来源标题；
- Open Redirect；
- 跨患者访问；
- PHI 日志泄漏；
- License 不允许时仍返回全文；
- 将内部 Prompt 或模型响应误映射到患者 DTO。

---

## 12. 实施阶段绑定

| 阶段 | 交付 |
|---|---|
| A5 | 创建 UI JSON Schema、fixtures 和三语言 Contract Test 设计 |
| B | Business API v2 保留 Delivery Version 和患者权限边界 |
| C | Thread/Resume 状态能够通知 Delivery 正在准备或等待审核 |
| D | EvidencePack、Claim、SourceSpan、Citation Validation 可映射到 DTO |
| E | 实现患者证据卡、来源展开、医生审核后刷新和 Delivery E2E |
| F | Feature Flag、Shadow/Canary、审计、来源撤回与回滚验证 |

A1-A4 只负责盘点现有前端、API、Evidence、Citation 和结果展示资产，不应提前实现该 UI 契约。

---

## 13. 完成门禁

患者端 Evidence/Citation UI 完成必须同时满足：

- [ ] JSON Schema 已建立；
- [ ] Java/Python/TypeScript Contract Test 通过；
- [ ] 每条医学 Claim 有 Citation 或明确证据不足状态；
- [ ] Safety Rule 与医学 Citation 分开展示；
- [ ] 紧急提示不依赖 RAG 在线成功；
- [ ] 患者端不显示内部 Prompt、模型原始响应和私有推理；
- [ ] 来源许可、外链和摘录策略通过安全检查；
- [ ] 证据冲突、人群不匹配和过期状态有患者友好文案；
- [ ] Delivery、CDP、Knowledge 和 Review 版本一致；
- [ ] 页面刷新和旧版本失效 E2E 通过；
- [ ] 医生审核修改后患者端能获取新的 Delivery；
- [ ] Accessibility 和移动端测试通过。

---

## 14. 当前结论

患者端证据展示不是将内部 EvidencePack 原样呈现，而是：

```text
经验证的患者事实
+ 确定性安全依据
+ Claim-Level 医学 Citation
+ 适用性、冲突与局限
+ 医生审核状态
→ 版本一致、患者友好且不暴露内部推理的 Delivery UI
```

该契约作为 `前端与业务迁移.md` 中 Evidence/Citation 展示要求的可开发细化规范。