# U03 Knowledge Dependency Applicability Decision v0.1

> 阶段：U03 Clinical Dependency Completion / E applicability adjudication  
> 状态：`APPROVED_FOR_ROLE_APPLICABILITY / NOT_A_KNOWLEDGE_RELEASE / NOT_FOR_PRODUCTION`  
> 依据：Gate A 已通过；`U03_Knowledge_Release_Manifest_Schema.md`；`U03_Gate_B_Readiness_Decomposition.md`；当前 `U03ReleaseBinding` 工程合同；Medical/Governance review。  
> 本文件只裁定“知识依赖的角色与是否需要独立 release”，不提供具体医学知识、阈值、规则或 D09 disposition。

---

## 1. 裁定目标

E 层必须回答：

```text
U03 当前 slice
到底依赖哪些独立 Knowledge Release？
哪些内容应属于 C Rule Pack 而不是 Knowledge Release？
哪些知识不得由模型或开发者隐式带入？
```

必须避免两个相反错误：

```text
错误 A：所有医学内容都塞进 Knowledge Release
错误 B：所有知识都藏进 Rule Pack / Prompt / 模型常识
```

---

## 2. 当前工程约束与临床约束

当前 `U03ReleaseBinding` 强制存在：

```text
ruleReleaseId
ruleReleaseVersion
knowledgeReleaseId
knowledgeReleaseVersion
```

且 knowledge release identity/version 为 required non-empty string。

因此，在不修改工程合同的前提下：

```text
正式 U03 governed invocation
→ 必须能够解析一个显式 Knowledge Release ref
```

但 KD-U03-01 的 REQUIRED 不仅来自当前 Java 合同，也来自本 slice 的临床治理要求：正式 U03 结果必须可重放其实际使用的来源 identity/version/scope/provenance。

```text
即使未来工程合同支持空 knowledge ref
KD-U03-01 在本 slice 仍为 REQUIRED
```

工程约束只说明“现在不能留空”；临床治理约束说明“即使技术上能留空，本 slice 也不应留空”。

---

## 3. Knowledge Dependency 分类

本 adjudication 使用：

```text
REQUIRED
OPTIONAL
NOT_REQUIRED
PROHIBITED_AS_IMPLICIT_DEPENDENCY
```

其中：

- `REQUIRED`：没有正式 release ref，当前受治理结果不能合法形成；
- `OPTIONAL`：可用于解释/追溯，但不影响当前 formal decision；
- `NOT_REQUIRED`：当前 slice 不消费该类独立知识资产；
- `PROHIBITED_AS_IMPLICIT_DEPENDENCY`：不得由模型、Prompt、开发者常识或 mutable external content 隐式注入。

---

## 4. 最终依赖裁定

### KD-U03-01 — Source-grounded Clinical Knowledge Release

角色：承载当前 U03 A/B/C/D 所引用的受治理医学来源集合与其可重放版本身份，包括来源 identity/version/publication metadata、适用 scope、provenance 和 review status。

最终裁定：

```text
applicability = REQUIRED
review = APPROVED
```

理由：

1. 本 slice 必须能够重放其使用的医学来源 identity、版本、发表/取用日期、scope 与 provenance；
2. 当前工程 binding 也强制 knowledge release identity/version；
3. C/D 的正式 release 需要可追溯到其依据的医学来源版本；
4. 不应把散落的 URL/来源名称等同于完整 Knowledge Release 治理。

边界：

```text
Knowledge Release
!= Rule Pack
!= Evidence Catalog
!= D09 Policy
```

### KD-U03-02 — Executable Threshold / Combination Logic

角色：生命体征阈值、布尔组合、precedence、rule predicates 等可执行逻辑。

最终裁定：

```text
independent_knowledge_release_applicability = NOT_REQUIRED
owner = C Rule Pack
review = APPROVED
```

这些内容只能有一份可执行真值，必须属于版本化 Rule Release。E 不复制阈值或组合逻辑；C 必须引用 KD-U03-01 的来源 release，不得自行发明来源。

### KD-U03-03 — Final Risk Disposition Mapping

角色：rule/evidence 到 `NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK / FAILED` 的确定性 mapping、priority、precedence 与 conflict behavior。

最终裁定：

```text
independent_knowledge_release_applicability = NOT_REQUIRED
owner = D09 Policy Release
review = APPROVED
```

这是确定性 Clinical/Business Policy，不是供运行时解释的知识文本。D09 可引用 KD-U03-01 作为 provenance，但 disposition truth 只存在于 D09 Policy Release。

### KD-U03-04 — Runtime Free-form External Retrieval

角色：运行时临时从 Web、RAG、LLM common knowledge、未版本化数据库或最新指南页面获取新的临床知识参与 formal Risk Decision。

最终裁定：

```text
applicability = PROHIBITED_AS_IMPLICIT_DEPENDENCY
review = APPROVED
```

禁止以下内容直接影响 U03 formal Risk result：

```text
model common knowledge
latest web content
mutable RAG corpus
unreviewed source text
```

若未来需要 retrieval，必须另建受治理 Knowledge Release / retrieval contract，并重新进入依赖与审核流程。

### KD-U03-05 — Explanation-only Knowledge

角色：不改变 formal evidence/rule/policy result，仅用于用户可读解释、来源展示或内部 review 辅助。

最终裁定：

```text
applicability = OPTIONAL
review = APPROVED
```

硬边界：

```text
Explanation knowledge
不得改变 formal Risk Decision
```

对外展示的来源应优先引用 KD-U03-01。若 explanation 反向影响 C/D，则不再属于 OPTIONAL，必须重新进入 governed dependency review。

---

## 5. 当前最小 Knowledge Release 范围

KD-U03-01 已批准为 REQUIRED。当前 U03 最小 Knowledge Release 只承载来源发布信息，不复制规则文本，至少包含：

```text
knowledge_release_id
knowledge_version
release_status

source_reference_ids[]
source_versions[]
source_publication_dates[]
source_retrieved_at[]

population_scope
region_scope
language_scope
channel_scope
clinical_context_scope
exclusions

curation_method
curation_owner
clinical_review_status
technical_review_status
provenance_refs[]
change_summary

effective_from
effective_until
supersedes_refs[]
rollback_target_ref
```

初始内容只能绑定已经进入当前 A/B 审核链的 source registry；本步骤不得新增指南、来源或新的临床结论。

---

## 6. 与 C 的 binding 关系

```text
C Rule Release
→ references B evidence definitions
→ references KD-U03-01 Knowledge Release for source/provenance
→ contains executable predicate/threshold/combination logic
```

必须避免：

```text
Knowledge Release 存一套阈值
+
Rule Pack 再存另一套阈值
```

---

## 7. 与 D 的 binding 关系

```text
D09 Policy Release
→ references C Rule Release
→ may reference KD-U03-01 directly for policy provenance
→ owns deterministic disposition mapping
```

D09 不允许从 Knowledge Release 自由解释出新的 branch。

---

## 8. Review 结果

```text
KD-U03-01 = APPROVE / REQUIRED
KD-U03-02 = APPROVE / NOT_REQUIRED_AS_INDEPENDENT_KR / OWNER_C
KD-U03-03 = APPROVE / NOT_REQUIRED_AS_INDEPENDENT_KR / OWNER_D09
KD-U03-04 = APPROVE / PROHIBITED_AS_IMPLICIT_DEPENDENCY
KD-U03-05 = APPROVE / OPTIONAL

E Applicability Decision v0.1 = APPROVED
E Applicability Approval = COMPLETE_FOR_ROLE_APPLICABILITY
```

这里的 COMPLETE 只适用于上述 5 类 dependency role 的适用性裁定，不表示真实 Knowledge Release 已完成或发布。

---

## 9. 当前状态

```text
E Structural Schema = FROZEN
E Applicability Decision v0.1 = APPROVED
E Applicability Approval = COMPLETE_FOR_ROLE_APPLICABILITY
KD-U03-01 Knowledge Release Content = NOT_STARTED
Knowledge Release Publication = NOT_COMPLETE

CD-04 = NOT_PASSED
Gate B = NOT_PASSED
C real clinical rule content = BLOCKED_UNTIL_RESOLVABLE_KR_OBJECT
D real clinical policy content = BLOCKED
```

下一步：按第 5 节起草最小 KD-U03-01 Knowledge Release 内容对象，只绑定已审核 A/B source registry。不得开始 C 的真实阈值/组合规则或 D09 branch。