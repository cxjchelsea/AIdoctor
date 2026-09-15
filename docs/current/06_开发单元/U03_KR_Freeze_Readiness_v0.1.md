# U03 KR Freeze Readiness v0.1

> 对象：`KR-U03-SOURCE-001`  
> 状态：`FREEZE_READINESS_NOT_PASSED / NOT_FROZEN / NOT_PUBLISHED`  
> 本文件只定义 candidate freeze 门禁，不创建 production release。

## 1. Freeze Preconditions

必须全部满足：

```text
KD-U03-01 Applicability = REQUIRED / APPROVED
6-source metadata verification = COMPLETE
locked clinical context = COMPLETE
curation_owner = ASSIGNED
clinical_review_owner = ASSIGNED
technical_review_owner = ASSIGNED
curation sign-off = APPROVED
clinical review = APPROVED
technical review = APPROVED
knowledge_release_id = FROZEN
knowledge_version = FROZEN_CANDIDATE_VERSION
scope = FROZEN
effective_from = DEFINED_FOR_CANDIDATE
effective_until = DEFINED_OR_EXPLICITLY_OPEN_ENDED
supersedes_refs = VALID
rollback_target_ref = VALID_OR_EXPLICITLY_NOT_APPLICABLE
provenance refs = VALIDATED
no new clinical content introduced = CONFIRMED
```

## 2. Candidate Freeze Semantics

通过本 gate 只允许形成：

```text
KR-U03-SOURCE-001@<candidate-version>
= RESOLVABLE_CANDIDATE
```

不等于：

```text
PUBLISHED
ACTIVE_FOR_PRODUCTION
PRODUCTION_AUTHORIZED
C Rule Pack APPROVED
D09 Policy APPROVED
```

## 3. Current Evaluation

当前已满足：

```text
Applicability = PASS
Source metadata verification = PASS_FOR_CURRENT_6_SOURCES
Locked clinical context = PASS
No new source = PASS
No new clinical conclusion = PASS
```

当前未满足：

```text
Owner Assignment = NOT_COMPLETE
Curation Sign-off = NOT_STARTED
Clinical Review = NOT_STARTED
Technical Review = NOT_STARTED
Candidate version freeze = NOT_STARTED
Effective-time freeze = NOT_STARTED
Rollback/supersede finalization = NOT_STARTED
```

## 4. Verdict

```text
KR Freeze Readiness = NOT_PASSED
Primary Blocker = REQUIRED_OWNERS_NOT_ASSIGNED
Secondary Blocker = FORMAL_REVIEW_NOT_COMPLETE
KR Candidate Freeze = BLOCKED
C/D real content = BLOCKED
Gate B = NOT_PASSED
```
