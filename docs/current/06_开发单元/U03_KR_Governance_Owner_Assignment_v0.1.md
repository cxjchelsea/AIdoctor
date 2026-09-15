# U03 KR Governance Owner Assignment v0.1

> 对象：`KR-U03-SOURCE-001` 的治理角色分配。  
> 状态：`ASSIGNMENT_REQUIRED / NOT_COMPLETE / BLOCKS_REVIEW_READY`  
> 本文件不指定虚构个人，仅定义角色、责任与完成条件。

## 1. Required Owners

```text
curation_owner = PENDING_ASSIGNMENT
clinical_review_owner = PENDING_ASSIGNMENT
technical_review_owner = PENDING_ASSIGNMENT
```

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

至少要求 clinical review 与 technical review 的职责可区分；同一人是否可兼任由组织治理决定，但任何 reviewer 都不能把自己未获授权的医学或工程判断扩展到另一职责域。

## 4. Completion Gate

只有以下条件全部满足：

```text
curation_owner != PENDING_ASSIGNMENT
clinical_review_owner != PENDING_ASSIGNMENT
technical_review_owner != PENDING_ASSIGNMENT
```

才允许：

```text
KR Owner Assignment = COMPLETE
```

在此之前：

```text
KR REVIEW_READY = NO
KR Freeze = BLOCKED
C real clinical content = BLOCKED
D real clinical content = BLOCKED
```

## 5. Current Status

```text
Owner Assignment = NOT_COMPLETE
Blocking Reason = REQUIRED_OWNERS_NOT_ASSIGNED
```
