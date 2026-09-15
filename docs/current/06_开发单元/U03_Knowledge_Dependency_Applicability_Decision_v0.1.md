# U03 Knowledge Dependency Applicability Decision v0.1

> 阶段：U03 Clinical Dependency Completion / E applicability adjudication  
> 状态：`DRAFT_FOR_GOVERNANCE_MEDICAL_REVIEW / NOT_APPROVED / NOT_FOR_PRODUCTION`  
> 依据：Gate A 已通过；`U03_Knowledge_Release_Manifest_Schema.md`；`U03_Gate_B_Readiness_Decomposition.md`；当前 `U03ReleaseBinding` 工程合同。  
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

## 2. 当前工程约束

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

这只是技术兼容性事实，不代表任意医学知识内容已经被批准。

---

## 3. Knowledge Dependency 分类原则

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

## 4. 初始依赖裁定

### KD-U03-01 — Source-grounded Clinical Knowledge Release

角色：承载当前 U03 A/B/C/D 所引用的受治理医学来源集合与其可重放版本身份，包括来源 identity/version/publication metadata、适用 scope、provenance 和 review status。

初始裁定：

```text
applicability = REQUIRED
```

理由：

1. 当前工程 binding 强制 knowledge release identity/version；
2. C/D 的正式 release 需要可追溯到其依据的医学知识版本；
3. 不应把“来源 URL/名称散落在 Rule entries 中”当作完整 Knowledge Release 治理；
4. 历史结果需要能够重放当时实际绑定的来源集合与 scope。

边界：

```text
Knowledge Release
!= Rule Pack
!= Evidence Catalog
!= D09 Policy
```

本项不决定“采用哪个具体指南版本”；具体 source selection / localization 仍需 Medical Owner review。

### KD-U03-02 — Executable Threshold / Combination Logic

角色：生命体征阈值、布尔组合、precedence、rule predicates 等可执行逻辑。

初始裁定：

```text
independent_knowledge_release_applicability = NOT_REQUIRED
owner = C Rule Pack
```

理由：这些内容一旦成为正式 executable clinical logic，应进入版本化 Rule Release，而不是同时维护一份可执行 Knowledge Release 副本。

但其 source/provenance 必须引用 KD-U03-01 的 Knowledge Release。

### KD-U03-03 — Final Risk Disposition Mapping

角色：rule/evidence 到 `NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK / FAILED` 的确定性 mapping、priority、precedence 与 conflict behavior。

初始裁定：

```text
independent_knowledge_release_applicability = NOT_REQUIRED
owner = D09 Policy Release
```

理由：这是 Business/Clinical Policy，不应被隐藏成知识文本。

其医学依据仍可引用 KD-U03-01。

### KD-U03-04 — Runtime Free-form External Retrieval

角色：运行时临时从 Web、RAG、LLM common knowledge、未版本化数据库或最新指南页面获取新的临床知识参与 formal Risk Decision。

初始裁定：

```text
applicability = PROHIBITED_AS_IMPLICIT_DEPENDENCY
```

禁止：

```text
model common knowledge
latest web content
mutable RAG corpus
unreviewed source text
```

直接影响 U03 formal Risk result。

若未来需要此能力，必须另建明确的 governed Knowledge Release / retrieval contract，不属于当前 slice。

### KD-U03-05 — Explanation-only Knowledge

角色：不改变 formal evidence/rule/policy result，仅用于用户可读解释、来源展示或内部 review 辅助。

初始裁定：

```text
applicability = OPTIONAL
```

硬边界：

```text
Explanation knowledge
不得改变 formal Risk Decision
```

若 explanation 反向影响 C/D，就不再是 OPTIONAL，而必须重新进入 governed dependency review。

---

## 5. 当前最小 Knowledge Release 范围建议

若 KD-U03-01 最终批准为 REQUIRED，当前 U03 最小 Knowledge Release 不应复制所有规则文本，而应最小承载：

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

初始内容来源只能来自已经进入当前 A/B 审核链的受治理 source registry；不得在本步骤增加新的医学结论。

---

## 6. 与 C 的 binding 关系

如果本 adjudication 通过：

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

导致双重 truth source。

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

## 8. Medical / Governance Review Questions

本 v0.1 只需要 reviewer 回答：

1. 是否同意 KD-U03-01 在当前 U03 slice 中为 `REQUIRED`？
2. 是否同意 executable threshold/combination truth 只属于 C，而不复制进 E？
3. 是否同意 final disposition mapping 只属于 D，而不复制进 E？
4. 是否同意 runtime free-form external clinical knowledge 对 formal Risk Decision 为 `PROHIBITED_AS_IMPLICIT_DEPENDENCY`？
5. 是否同意 explanation-only knowledge 可为 `OPTIONAL`，但不得反向影响 formal decision？
6. 当前最小 Knowledge Release 是否足以支撑 source/version/scope/provenance replay？

允许 verdict：

```text
APPROVE
REVISE
REJECT
```

---

## 9. 当前状态

```text
E Structural Schema = FROZEN
E Applicability Decision Draft v0.1 = AVAILABLE
E Applicability Approval = NOT_COMPLETE
KD-U03-01 = PROPOSED_REQUIRED
KD-U03-02 = PROPOSED_NOT_REQUIRED_AS_INDEPENDENT_KNOWLEDGE
KD-U03-03 = PROPOSED_NOT_REQUIRED_AS_INDEPENDENT_KNOWLEDGE
KD-U03-04 = PROPOSED_PROHIBITED_AS_IMPLICIT_DEPENDENCY
KD-U03-05 = PROPOSED_OPTIONAL

CD-04 = NOT_PASSED
Gate B = NOT_PASSED
C real clinical rule content = NOT_STARTED
D real clinical policy content = NOT_STARTED
```

下一步：对本 applicability decision 做 Medical/Governance review。批准前不得开始 C 的真实医学规则内容。
