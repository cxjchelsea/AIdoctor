# U03 Clinical Dependency Readiness

> 阶段：U03 Clinical Dependency Completion / Readiness  
> 基线：`main@765fb9ca1178c47a6ecfc660bd650edb5bffaf8b`（包含 PR #86 + #87）  
> 本文件判断是否可以进入真实 C02/D09 临床依赖实现；不构成 Implementation Authorization。

## 1. 已满足工程前置

```text
U03 Engineering Governance Path = IMPLEMENTED / COMPONENT_VERIFIED
Foundation-1 capability authorization = AVAILABLE
minimal P04/P06 release binding = AVAILABLE
candidate-only C02 boundary = AVAILABLE
evidence acceptance boundary = AVAILABLE
D09 owner boundary = AVAILABLE
typed K09 proposal + P01 commit = AVAILABLE
P05 release-aware trace = AVAILABLE
failure semantics = AVAILABLE
New Foundation = NOT_REQUIRED
```

## 2. Clinical Input Package 当前进度

```text
A Clinical Risk Semantics
= STRUCTURAL_SEMANTICS_FROZEN
/ CONTENT_DRAFT_v0.2_AVAILABLE
/ MEDICAL_OWNER_REVIEW_COMPLETE
/ A_APPROVE_7_REVISE_0
/ SOURCE_LOCKED_SEMANTICS_FROZEN

B Evidence Catalog
= STRUCTURAL_SCHEMA_FROZEN
/ CONTENT_DRAFT_v0.2_AVAILABLE
/ MEDICAL_OWNER_REVIEW_COMPLETE
/ B_APPROVE_11_REVISE_0
/ SOURCE_LOCKED_SEMANTICS_FROZEN

C Safety-critical Risk Rule Pack
= RR-U03-RISK-001@0.2.0-candidate_CANDIDATE_FROZEN
/ CD-03_PASSED_FOR_INITIAL_CANDIDATE
/ BF-CDE-01_TARGETED_SCOPE_REVISION_DRAFT_v0.2.1_AVAILABLE

D D09 Clinical Policy Table
= PR-U03-D09-001@0.2.0-candidate_CANDIDATE_FROZEN
/ U03_D09_COVERAGE_V0_2_CANDIDATE_FROZEN
/ CD-05_PASSED_FOR_INITIAL_CANDIDATE
/ BF-CDE-01_POLICY_SCOPE_REVISION_DRAFT_v0.2.1_AVAILABLE
/ BF-CDE-01_COVERAGE_REVISION_DRAFT_v0.2.1_AVAILABLE

E Knowledge Release Manifest
= KR-U03-SOURCE-001@0.1.0-candidate
/ CANDIDATE_FROZEN
/ NOT_PUBLISHED
/ CURRENT_SCOPE_AUTHORITY_RETAINED

F Risk EvalSet / Safety Suite
= STRUCTURAL_SCHEMA_FROZEN / GOLDEN_CASE_CONTENT_NOT_STARTED / NOT_REVIEW_READY
```

## 3. Gate 状态

### Gate A — v0.2 Source-locked Clinical Semantics

```text
Gate A = PASS
A APPROVE = 7 / REVISE = 0
B APPROVE = 11 / REVISE = 0
```

### Gate B — Governed Content Ready

当前：

```text
Gate B = NOT_PASSED
reason = BLOCKED_BY_CDE_SCOPE_INCONSISTENCY
```

Cross-consistency review：

```text
U03_CDE_Cross_Consistency_Review_v0.1.md
PASS = 11
REVISE = 1
blocking finding = 1
BF-CDE-01 = OPEN
```

阻塞项：

```text
A/E authority:
  pregnancy / puerperium = whole-slice exclusion

C/D frozen 0.2.0 metadata:
  explicitly expresses only sepsis-specific exclusion
```

Gate B 决策：

```text
U03_Gate_B_Decision_v0.1.md
= NOT_PASSED / BLOCKED_BY_CDE_SCOPE_INCONSISTENCY
```

BF-CDE-01 修订任务：

```text
U03_CDE_Cross_Consistency_Revision_Task_v0.1.md
```

当前已准备 targeted correction drafts：

```text
C scope revision
= U03_C_Rule_Release_Scope_Revision_Draft_v0.2.1.md
= RR-U03-RISK-001@0.2.1-draft

D scope revision
= U03_D09_Policy_Scope_Revision_Draft_v0.2.1.md
= PR-U03-D09-001@0.2.1-draft

Coverage revision
= U03_D09_Coverage_Contract_Revision_Draft_v0.2.1.md
= U03_D09_COVERAGE_V0_2_1_DRAFT
```

三个修订只做 scope alignment：

```text
pregnancy / puerperium
= OUTSIDE_CURRENT_U03_WHOLE_POLICY_SLICE
```

保持不变：

```text
C 15 active rule predicates / thresholds / signal vocabulary
D 6 branches / precedence / disposition mappings
D denominator membership
E source registry / source metadata
```

Targeted review record 已建立：

```text
U03_CDE_Scope_Alignment_Targeted_Review_Record_v0.2.1.md
Medical Review = NOT_STARTED
Technical Review = NOT_STARTED
BF-CDE-01 = OPEN
```

旧冻结对象保持 immutable：

```text
RR-U03-RISK-001@0.2.0-candidate
PR-U03-D09-001@0.2.0-candidate
U03_D09_COVERAGE_V0_2
KR-U03-SOURCE-001@0.1.0-candidate
```

### Gate C — Independent Evaluation Ready

```text
Gate C = NOT_PASSED
CD-06 = NOT_REVIEW_READY
Clinical Golden Cases = NOT_STARTED
```

C/D pre-freeze evaluation 均只服务历史 candidate freeze，不自动认证新 0.2.1 scope revisions，也不等于 Gate C PASS。

### Gate D — Authorization

```text
CD-07 Implementation Readiness = BLOCKED
Implementation Authorization = NOT_GRANTED
```

---

## 4. Current Governed Release References

```text
E Knowledge Release
= KR-U03-SOURCE-001@0.1.0-candidate
/ CANDIDATE_FROZEN

C Historical Candidate
= RR-U03-RISK-001@0.2.0-candidate
/ CANDIDATE_FROZEN

D Historical Candidate
= PR-U03-D09-001@0.2.0-candidate
/ CANDIDATE_FROZEN

D Historical Coverage
= U03_D09_COVERAGE_V0_2
/ CANDIDATE_FROZEN

Targeted Revision Drafts
= C 0.2.1 scope draft
= D 0.2.1 scope draft
= Coverage 0.2.1 scope draft
/ ALL REVIEW_REQUIRED / NOT_FROZEN
```

## 5. 当前 Readiness 判定

```text
Engineering Prerequisites = PASS
Gate A = PASS
CD-03 = PASSED_FOR_INITIAL_CANDIDATE (historical 0.2.0 candidate)
CD-04 = CANDIDATE_READY / NOT_PRODUCTION
CD-05 = PASSED_FOR_INITIAL_CANDIDATE (historical 0.2.0 candidate)

C/D/E Cross-Consistency = REVISE_REQUIRED
BF-CDE-01 = OPEN
Targeted Scope Revision Drafts = AVAILABLE
Targeted Medical Review = NOT_STARTED
Targeted Technical Review = NOT_STARTED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

## 6. 当前唯一下一步

```text
review U03_CDE_Scope_Alignment_Targeted_Review_Record_v0.2.1.md
↓
if Medical + Technical APPROVE
→ BF-CDE-01 content alignment approved
↓
assess whether historical pre-freeze eval can be reused or requires targeted scope fixtures
↓
create independent new candidate identities
↓
freeze affected new candidates
↓
C/D/E cross-consistency re-review
↓
only if PASS + blocking finding = 0
→ reconsider Gate B
```

## 7. 当前禁止事项

- 不原地修改历史 frozen C/D/coverage candidates；
- 不直接把 0.2.1 revision drafts 标记 candidate/frozen；
- 不自动继承历史 C 57 / D 48 fixture PASS 到新 scope versions；
- 不把当前修订解释为 Gate B PASS；
- 不开始 CD-07 / runtime / U04；
- 不打开儿科或孕产临床规则；当前修订只是在现有 authority 下明确“孕产不属于当前 slice”。
