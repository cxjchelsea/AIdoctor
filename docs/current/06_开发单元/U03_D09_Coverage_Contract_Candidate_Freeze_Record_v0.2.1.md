# U03 D09 Coverage Contract Candidate Freeze Record v0.2.1

> 对象：`U03_D09_COVERAGE_V0_2_1_CANDIDATE`。  
> 状态：`CANDIDATE_FREEZE_COMPLETE / NOT_FOR_RUNTIME / NOT_FOR_PRODUCTION`  
> 本记录仅冻结 0.2.1 targeted coverage candidate；不构成 Gate B / Gate C / Implementation Authorization。

---

## 1. Freeze Target

```text
candidate_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
source_draft_ref = U03_D09_COVERAGE_V0_2_1_DRAFT
historical_ref = U03_D09_COVERAGE_V0_2 / CANDIDATE_FROZEN / IMMUTABLE
```

---

## 2. Freeze Preconditions

```text
Targeted Freeze Readiness = READY_FOR_TARGETED_FREEZE
Coverage 0.2.1 Medical = APPROVE
Coverage 0.2.1 Technical = APPROVE
BF-CDE-01 = CLOSED_FOR_CONTENT
BLOCKER-FZ-CDE-021-01 = CLOSED
Targeted Eval PASS = YES
fixture_count = 6
historical D48 reuse = APPROVED_FOR_UNCHANGED_SEMANTICS
```

Bound dependencies:

```text
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_entry_review_ref = U03_CDE_v0.2.1_Scope_Entry_Missingness_Review_Record.md
```

---

## 3. Freeze Decision

```text
U03_D09_COVERAGE_V0_2_1_CANDIDATE
= RESOLVABLE_CANDIDATE
= CANDIDATE_FROZEN
```

Frozen delta:

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

Unchanged and retained:

```text
ALWAYS_APPLICABLE = 5 baseline rules
CONDITIONALLY_APPLICABLE = 2 families
INPUT_INSUFFICIENT → INSUFFICIENT_APPLICABLE
SCOPE_MISMATCH → NOT_APPLICABLE
```

Any later change requires a new coverage contract candidate version; 0.2.1 must not be mutated in place.

---

## 4. Boundary

```text
CANDIDATE_FROZEN
!= Gate B PASS
!= Gate C PASS
!= runtime active
!= production active
```
