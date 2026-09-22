# U03 D09 Coverage Contract Candidate v0.2.1

> 对象：BF-CDE-01 targeted scope correction 后的独立 D09 coverage contract candidate identity。  
> Candidate Ref：`U03_D09_COVERAGE_V0_2_1_CANDIDATE`  
> 状态：`CANDIDATE_FROZEN / RESOLVABLE / NOT_FOR_RUNTIME / NOT_FOR_PRODUCTION`  
> 本对象不修改、不覆盖 `U03_D09_COVERAGE_V0_2`。

---

## 1. Candidate Identity

```text
coverage_contract_id = U03_D09_COVERAGE
coverage_contract_version = 0.2.1-candidate
candidate_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
status = CANDIDATE_FROZEN
source_draft_ref = U03_D09_COVERAGE_V0_2_1_DRAFT
candidate_created_from = U03_D09_Coverage_Contract_Revision_Draft_v0.2.1.md
freeze_record_ref = U03_D09_Coverage_Contract_Candidate_Freeze_Record_v0.2.1.md
```

Historical contract remains immutable:

```text
U03_D09_COVERAGE_V0_2 = CANDIDATE_FROZEN / HISTORICAL
```

---

## 2. Bound Dependencies

```text
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_entry_review_ref = U03_CDE_v0.2.1_Scope_Entry_Missingness_Review_Record.md
```

---

## 3. Revision Delta

Internal denominator semantics are unchanged:

```text
ALWAYS_APPLICABLE = 5 baseline rules
CONDITIONALLY_APPLICABLE = NHS_DYSPNOEA_FAMILY + NG253_SEPSIS_FAMILY

C MATCHED / NO_MATCH → APPLICABLE_EVALUATED
C RULE_SIGNAL_INPUT_INSUFFICIENT → INSUFFICIENT_APPLICABLE
C RULE_SIGNAL_SCOPE_MISMATCH → NOT_APPLICABLE
```

Only whole-policy entry is clarified:

```text
pregnancy_or_puerperium = TRUE
→ NOT_APPLICABLE_AT_POLICY_LEVEL
→ denominator NOT_CONSTRUCTED
→ D09-P-001 / OVERALL_POLICY_SCOPE_MISMATCH

pregnancy_or_puerperium ∈ {UNKNOWN, NOT_ASKED, NOT_ESTABLISHED}
→ whole-policy scope NOT_ESTABLISHED
→ denominator NOT_CONSTRUCTED
→ D09-P-001 / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED

pregnancy_or_puerperium = FALSE
→ continue remaining overall-scope validation
```

---

## 4. Evaluation Binding

```text
historical D48 = reusable for unchanged coverage semantics
targeted scope fixtures = 6 / APPROVE / APPROVE
Targeted Eval PASS = YES
BLOCKER-FZ-CDE-021-01 = CLOSED
```

No baseline membership, conditional-family membership, or C-signal-to-coverage mapping changed.

---

## 5. Lifecycle Boundary

```text
U03_D09_COVERAGE_V0_2_1_CANDIDATE
= RESOLVABLE_CANDIDATE
= CANDIDATE_FROZEN
= NOT_FOR_RUNTIME
= NOT_FOR_PRODUCTION
```

Candidate freeze does not mean Gate B PASS, Gate C PASS, runtime authorization, or production authorization.
