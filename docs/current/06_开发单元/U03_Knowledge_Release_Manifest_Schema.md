# U03 Knowledge Release Manifest Schema

> 阶段：U03 Clinical Dependency Completion / E — Knowledge Release Manifest  
> 分支：`prep/u03-clinical-dependency-completion`  
> 本文件只冻结知识依赖的治理结构与适用性判定，不提供任何具体医学知识内容，也不构成医学审核、Implementation Authorization、Merge Authorization 或 Production Authorization。

---

## 1. 目的

U03 的真实临床 Risk 能力不得依赖“未版本化的最新知识”或代码中隐式存在的医学常识。

正式运行时若使用知识资产，必须绑定一个可追溯、可审核、可回滚的 Knowledge Release。

必须保持：

```text
Knowledge Candidate
!=
Knowledge Published
!=
Knowledge Active
!=
Knowledge Authorized For This Consultation
```

Knowledge Release 只证明“某一版本的知识内容在某一范围内被正式发布并允许引用”，不拥有 Risk Evidence、Rule Hit、D09 Decision 或 Clinical Truth。

---

## 2. E 层回答的问题

E 层只回答：

1. U03 是否需要独立 Knowledge Release；
2. 哪些知识依赖必须显式版本化；
3. 一个 Knowledge Release 至少需要哪些身份、来源、范围、审核与生命周期字段；
4. Rule Pack / D09 如何引用 Knowledge Release；
5. 知识失效、撤回、过期和回滚如何表达；
6. 哪些情况可以明确标记为 `NOT_REQUIRED`。

E 层不回答：

- 具体医学事实是什么；
- 某症状对应什么风险；
- 某阈值是多少；
- D09 最终如何裁决；
- 哪个指南结论“更正确”。

---

## 3. U03 Knowledge Dependency 分类

每一类潜在知识依赖必须先做 applicability adjudication。

推荐枚举：

```text
REQUIRED
OPTIONAL
NOT_REQUIRED
PROHIBITED_AS_IMPLICIT_DEPENDENCY
TBD_BY_MEDICAL_OWNER
```

含义：

- `REQUIRED`：没有该知识发布版本，就不能形成当前受治理 clinical result；
- `OPTIONAL`：可增强解释/来源追踪，但不是当前 decision 的必要前置；
- `NOT_REQUIRED`：当前 U03 slice 的正式判断不依赖独立 Knowledge Release；
- `PROHIBITED_AS_IMPLICIT_DEPENDENCY`：不得由模型/开发者“凭常识”隐式带入；
- `TBD_BY_MEDICAL_OWNER`：是否需要独立知识发布尚未完成医学裁定。

任何 `REQUIRED` 依赖都必须进入 P06 binding allow-list，并在 candidate / rule / policy / trace 中保留对应 release ref。

---

## 4. Knowledge Release 最小身份结构

```text
knowledge_release_id
knowledge_domain
knowledge_version
release_status
contract_version
```

要求：

- `knowledge_release_id` 稳定标识一次 release；
- `knowledge_version` 必须可重放；
- 内容发生会影响临床判断的变化时必须产生新版本；
- 不能用 mutable alias（例如 `latest`）作为正式临床依赖。

---

## 5. 内容范围与适用范围

至少：

```text
content_scope
population_scope
region_scope
language_scope
channel_scope
clinical_context_scope
exclusions
```

`clinical_context_scope` 用于描述该知识包在 U03 中允许被哪些能力/规则/策略引用。

必须保持：

```text
Knowledge exists
!=
Knowledge applicable to this consultation
```

若 scope 不匹配，Runtime/Resolver 不得静默继续使用该 release。

---

## 6. 来源与 Provenance

至少：

```text
source_reference_ids[]
source_versions[]
source_publication_dates[]
source_retrieved_at[]
curation_method
curation_owner
provenance_refs[]
change_summary
```

注意：

- 本 schema 不定义哪些医学来源“应该被采用”；
- 来源选择与临床有效性必须由医学 Owner 审核；
- 不能把网页抓取时间或数据库更新时间误当成医学内容生效时间。

---

## 7. Review 与 Approval

推荐生命周期：

```text
DRAFT
SOURCE_COLLECTED
CLINICAL_REVIEW
TECHNICAL_REVIEW
EVALUATION
READY_TO_PUBLISH
PUBLISHED
DEPRECATED
WITHDRAWN
EXPIRED
RETIRED
```

至少记录：

```text
review_owner
clinical_review_status
technical_review_status
evaluation_refs[]
approval_refs[]
reviewed_at
published_at
```

正式 Clinical Decision 只能绑定满足当前 P06 / Release Resolver 可用条件的 release。

---

## 8. 生效期与 Currentness

```text
effective_from
effective_until
supersedes_refs[]
conflict_refs[]
rollback_target_ref
withdrawn_at
expiration_reason
```

必须保持：

```text
PUBLISHED != always-active
WITHDRAWN != ACTIVE
EXPIRED != ACTIVE
DEPRECATED != RETIRED
RETIRED != DELETED
```

知识发布状态变化不得改写已经形成的历史 Clinical State；历史 trace 必须仍可重放当时绑定的 release。

---

## 9. 与 Rule Pack 的关系

Rule Pack 可以引用一个或多个 `KnowledgeReleaseRef`，但二者职责不同：

```text
Knowledge Release
= 受治理医学知识内容与来源版本

Rule Pack
= 结构化、可执行、可测试的规则发布
```

必须保持：

```text
Knowledge Statement
!= Rule
Knowledge Release
!= Rule Release
```

若某条规则依赖特定知识版本，则 Rule Release 必须显式记录该 Knowledge Release ref。

---

## 10. 与 D09 的关系

D09 Policy Release 可以：

- 直接引用 Knowledge Release；
- 或只通过 Rule Release 间接引用。

两者必须在 Policy Manifest 中明确，不允许隐式存在。

必须保持：

```text
Knowledge Release
!= D09 Decision
```

D09 的正式 Decision 仍由 deterministic policy owner 形成，并绑定：

```text
clinical_state_version
rule_release_ref(s)
knowledge_release_ref(s) where applicable
capability_binding_ref
policy_release_ref
reason_code
```

---

## 11. U03 Manifest 建议结构

```text
manifest_id
manifest_version
u03_unit_version
status

knowledge_dependencies[]:
  - dependency_id
  - dependency_role
  - applicability
  - required_by_component
  - allowed_release_refs[]
  - required_scope
  - failure_behavior_ref
  - review_owner
  - review_status

active_release_refs[]
rollback_plan_ref
evaluation_refs[]
provenance_refs[]
created_at
approved_at
```

`required_by_component` 可引用：

```text
C02
Rule Pack
D09
EvalSet
```

---

## 12. Failure 语义

如果某个 dependency 被裁定为 `REQUIRED`，但当前没有满足 binding/scope/effective-time 的 Knowledge Release：

```text
required knowledge unavailable
→ governed failure
```

不得变成：

```text
use model common knowledge
use stale release silently
use unreviewed candidate knowledge
use latest external content implicitly
```

具体 failure 到 U03 Risk Assessment 的映射由既有 U03 failure semantics / D09 policy 负责，本文件只定义知识依赖不可静默缺失。

---

## 13. `NOT_REQUIRED` 的合法条件

只有在经过显式 adjudication 后才能使用 `NOT_REQUIRED`。

至少需要记录：

```text
dependency_id
adjudication = NOT_REQUIRED
rationale_ref
review_owner
review_status
scope
adjudicated_at
```

禁止：

```text
没有找到知识资产
→ 自动视为 NOT_REQUIRED
```

也禁止：

```text
Rule Pack 已包含文本说明
→ 自动推断 Knowledge Release 不需要
```

---

## 14. 版本一致性要求

正式 U03 result 至少要能追溯到当次运行实际使用的：

```text
Capability Binding
Rule Release
Knowledge Release(s) where applicable
D09 Policy Release
Clinical State Version
```

如果任一 REQUIRED 版本发生 mismatch，不能把结果继续视为 current VALID result。

---

## 15. E 层当前状态

```text
E Knowledge Release Manifest
= STRUCTURAL_SCHEMA_FROZEN
/ APPLICABILITY_ADJUDICATION_PENDING
/ CLINICAL_CONTENT_PENDING
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ NOT_APPROVED
```

当前仍未完成：

- 哪些 U03 知识依赖属于 REQUIRED / OPTIONAL / NOT_REQUIRED；
- 真实 Knowledge Release 内容；
- source selection 医学审核；
- release publication；
- P06 production binding；
- clinical evaluation。

因此本文件只允许作为后续 E 层医学裁定与发布治理的 schema，不代表 U03 已具有可用医学知识发布。
