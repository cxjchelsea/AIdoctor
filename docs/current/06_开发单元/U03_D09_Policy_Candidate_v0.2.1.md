# U03 D09 Policy Candidate v0.2.1

> 对象：BF-CDE-01 targeted scope correction 后的独立 D09 policy candidate identity。  
> Candidate Ref：`PR-U03-D09-001@0.2.1-candidate`  
> 状态：`CANDIDATE_FROZEN / RESOLVABLE / NOT_PUBLISHED / NOT_FOR_RUNTIME / NOT_FOR_PRODUCTION`  
> 本对象不修改、不覆盖 `PR-U03-D09-001@0.2.0-candidate`。

---

## 1. Candidate Identity

```text
policy_release_id = PR-U03-D09-001
policy_set_id = U03-D09-CLINICAL-RISK-DISPOSITION
policy_version = 0.2.1-candidate
status = CANDIDATE_FROZEN
source_draft_ref = PR-U03-D09-001@0.2.1-draft
candidate_created_from = U03_D09_Policy_Scope_Revision_Draft_v0.2.1.md
freeze_record_ref = U03_D09_Policy_Candidate_Freeze_Record_v0.2.1.md
```

Historical candidate remains immutable:

```text
PR-U03-D09-001@0.2.0-candidate = CANDIDATE_FROZEN / HISTORICAL
```

---

## 2. Bound Dependencies

```text
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_pair_freeze_ref = PF-U03-C-POLICY-001
```

---

## 3. Revision Delta

Only approved delta:

```text
pregnancy / puerperium
= OUTSIDE_CURRENT_U03_WHOLE_POLICY_SLICE
```

Formal P0 handling:

```text
pregnancy_or_puerperium = TRUE
→ FAILED / NONE / OVERALL_POLICY_SCOPE_MISMATCH

pregnancy_or_puerperium ∈ {UNKNOWN, NOT_ASKED, NOT_ESTABLISHED}
→ FAILED / NONE / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED

pregnancy_or_puerperium = FALSE
→ continue remaining overall-scope validation
→ denominator may be constructed only after all scope predicates pass
```

The following remain unchanged:

```text
6 branch identities
P0 > P1 > P2 > P3 > P4 > P5
HIGH_RISK / CAUTION / NO_HIGH_RISK_SIGNAL mappings
FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
D09 Decision != U04 Safety Gate Decision
```

---

## 4. Evaluation Binding

Historical reusable evidence:

```text
D48 = U03_D09_PreFreeze_Evaluation_Fixtures_v0.1.md
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

Scope-entry review:

```text
U03_CDE_v0.2.1_Scope_Entry_Missingness_Review_Record.md
BLOCKER-FZ-CDE-021-01 = CLOSED
```

---

## 5. Scope

```text
population_scope = Gate-A / E source-locked current adult slice only
pregnancy_puerperium = EXCLUDED_FROM_CURRENT_U03_WHOLE_POLICY_SLICE
pediatrics = EXCLUDED
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
china_localized_production_policy = NOT_INCLUDED
```

---

## 6. Lifecycle Boundary

```text
PR-U03-D09-001@0.2.1-candidate
= RESOLVABLE_CANDIDATE
= CANDIDATE_FROZEN
= NOT_PUBLISHED
= NOT_FOR_RUNTIME
= NOT_FOR_PRODUCTION
```

Candidate freeze does not mean CD-05 re-certification, Gate B PASS, Gate C PASS, runtime authorization, or production authorization.
