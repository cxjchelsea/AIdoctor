# U03 KR Governance Owner Assignment v0.1

> 对象：`KR-U03-SOURCE-001` 的治理角色分配。  
> 状态：`ASSIGNMENT_COMPLETE / REVIEW_CHANNEL_BOUND / CANDIDATE_FROZEN`  
> 本文件指定本 slice 可追责的职能角色，不虚构自然人，也不构成 production approval。

## 1. Required Owners

```text
curation_owner = U03_CLINICAL_INPUT_PACKAGE_MAINTAINER
clinical_review_owner = U03_MEDICAL_OWNER_REVIEW
technical_review_owner = U03_TECHNICAL_GOVERNANCE_REVIEW
assigned_at = 2026-09-15
assignment_ref = KR_OWNER_ASSIGNMENT_2026-09-15
```

绑定说明：

```text
curation_owner
= PR #88 / U03 Clinical Input Package 文档维护职责
= 负责维护已审 6-source registry 与 KR draft 文本

clinical_review_owner
= U03 Clinical Dependency Completion 的 Medical Owner Review 通道
= 对本 KR 出具临床审查结论

technical_review_owner
= U03 KR binding / replay / resolver 兼容性审查通道
= 对本 KR 出具技术治理审查结论
```

同一审查通道可分别履行临床与技术职责，但结论必须分栏记录，不得互相替代。组织后续若指定不同自然人，可覆盖本分配而不自动改写已记录的审查问题清单。

## 2. Role Boundaries

### curation_owner
负责：
- 维护当前 6-source registry 的 identity / URL / metadata / provenance；
- 保证不新增未经过 A/B 审核链的医学来源；
- 维护变更摘要、supersede / rollback 信息；
- 不拥有具体 clinical rule / D09 disposition。

### clinical_review_owner
负责：
- 确认每条 source 的 locked clinical context 与 Gate A source-lock 一致；
- 确认 source_type、scope 与使用边界不会扩大医学含义；
- 确认 KR 不引入新临床结论；
- 对 KR content review 给出 APPROVE / REVISE / REJECT。

### technical_review_owner
负责：
- 确认 release identity/version/scope/effective-time 字段可被工程 resolver/binding 表达；
- 校验 canonical URL / metadata 结构、provenance refs、version replay 与 rollback 结构；
- 确认 `KR-U03-SOURCE-001@0.1.0-draft` 不会被误用作 production binding；
- 不裁决医学内容。

## 3. Independence / Conflict Rule

clinical review 与 technical review 的职责必须可区分。本轮由同一审查通道分栏签署，不合并成一个“综合通过”。任一方未 APPROVE，不得进入 freeze。

## 4. Completion Gate

```text
curation_owner != PENDING_ASSIGNMENT
clinical_review_owner != PENDING_ASSIGNMENT
technical_review_owner != PENDING_ASSIGNMENT
```

当前：

```text
KR Owner Assignment = COMPLETE
```

本分配只解除“无人签字”的 blocker，不自动使 KR 成为 FROZEN 或允许开始 C。

## 5. Current Status

```text
Owner Assignment = COMPLETE
Formal Review = COMPLETE / APPROVE
KR Freeze = COMPLETE_FOR_CANDIDATE
C drafting = UNBLOCKED
D drafting = BLOCKED_UNTIL_C_VOCABULARY
```
