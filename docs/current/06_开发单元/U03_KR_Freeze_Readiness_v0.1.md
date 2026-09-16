# U03 KR Freeze Readiness v0.1

> 对象：`KR-U03-SOURCE-001`  
> 状态：`FREEZE_READINESS_PASSED / CANDIDATE_FROZEN / NOT_PUBLISHED`  
> 冻结日期：`2026-09-15`。  
> 本文件只确认 candidate freeze，不创建 production release。

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

本 gate 现已形成：

```text
KR-U03-SOURCE-001@0.1.0-candidate
= RESOLVABLE_CANDIDATE
frozen_at = 2026-09-15
effective_from = 2026-09-15
effective_until = OPEN_ENDED
```

不等于：

```text
PUBLISHED
ACTIVE_FOR_PRODUCTION
PRODUCTION_AUTHORIZED
C Rule Pack APPROVED
D09 Policy APPROVED
Gate B = PASS
```

## 3. Current Evaluation

```text
Applicability = PASS
Source metadata verification = PASS_FOR_CURRENT_6_SOURCES
Locked clinical context = PASS
Owner Assignment = COMPLETE
Curation Sign-off = APPROVE
Clinical Review = APPROVE
Technical Review = APPROVE
No new source = PASS
No new clinical conclusion = PASS
Provenance refs = VALIDATED
Candidate version freeze = COMPLETE
Effective-time freeze = COMPLETE
Rollback/supersede finalization = COMPLETE
```

```text
knowledge_release_id = KR-U03-SOURCE-001
knowledge_version = 0.1.0-candidate
supersedes_refs = []
rollback_target_ref = NOT_APPLICABLE
```

`KR-U03-SOURCE-001@0.1.0-draft` 仅为被替代的工作草稿，不再作为可引用版本。

## 4. Verdict

```text
KR Freeze Readiness = PASSED
KR Candidate Freeze = COMPLETE
KR Publication = NO
C drafting = UNBLOCKED
D drafting = BLOCKED_UNTIL_C_VOCABULARY
Gate B = NOT_PASSED
```

C 若开始起草，必须引用 `KR-U03-SOURCE-001@0.1.0-candidate`。不得把本 candidate 当作生产 binding。
