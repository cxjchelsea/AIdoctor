# U03 C Rule Release Candidate v0.2.1

> 对象：BF-CDE-01 targeted scope correction 后的独立 C Rule Release candidate identity。  
> Candidate Ref：`RR-U03-RISK-001@0.2.1-candidate`  
> 状态：`CANDIDATE_OBJECT_CREATED / FREEZE_PENDING / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
> 本对象不修改、不覆盖 `RR-U03-RISK-001@0.2.0-candidate`。

---

## 1. Candidate Identity

```text
rule_release_id = RR-U03-RISK-001
rule_set_id = U03-SAFETY-CRITICAL-RISK
release_version = 0.2.1-candidate
status = CANDIDATE_PENDING_FREEZE
source_draft_ref = RR-U03-RISK-001@0.2.1-draft
candidate_created_from = U03_C_Rule_Release_Scope_Revision_Draft_v0.2.1.md
```

Historical candidate remains immutable:

```text
RR-U03-RISK-001@0.2.0-candidate = CANDIDATE_FROZEN / HISTORICAL
```

---

## 2. Revision Delta

Only approved delta:

```text
pregnancy / puerperium
= EXCLUDED_FROM_CURRENT_U03_WHOLE_SLICE
```

The following are unchanged from 0.2.0:

```text
15 active rule identities
all reviewed predicates / thresholds
6 RULE_SIGNAL_* vocabulary entries
U03_C_MISSINGNESS_V0_2
U03_SEPSIS_SHARED_SCOPE_V0_2
PF-U03-C-POLICY-001
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
```

No rule predicate, threshold, evidence taxonomy, source, or signal mapping changed.

---

## 3. Evaluation Binding

Historical reusable evidence:

```text
C57 = U03_C_PreFreeze_Evaluation_Fixtures_v0.2.md
reuse = APPROVED_FOR_UNCHANGED_SEMANTICS
```

Targeted delta evidence:

```text
U03_CDE_v0.2.1_Targeted_Scope_Evaluation_Fixtures_Draft.md
fixture_count = 6
Medical = APPROVE
Technical/Eval = APPROVE
Targeted Eval PASS = YES
```

Scope-entry missingness decision:

```text
UNKNOWN / NOT_ASKED / NOT_ESTABLISHED
→ whole-policy scope NOT_ESTABLISHED
→ current-slice C rule evaluation NOT_ENTERED
```

---

## 4. Scope

```text
population_scope = Gate-A-approved adult / source-locked scope only
pregnancy_puerperium = EXCLUDED_FROM_CURRENT_U03_WHOLE_SLICE
pediatrics = EXCLUDED
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
china_localized_production_pathway = NOT_INCLUDED
```

NG253 sepsis shared-scope remains an additional family-level constraint, not the owner of whole-slice pregnancy/puerperium exclusion.

---

## 5. Lifecycle Boundary

```text
RR-U03-RISK-001@0.2.1-candidate
= CREATED / RESOLVABLE
= NOT_FROZEN
= NOT_PUBLISHED
= NOT_FOR_RUNTIME
= NOT_FOR_PRODUCTION
```

Candidate creation does not mean CD-03 re-certification, Gate B PASS, Gate C PASS, runtime authorization, or production authorization.
