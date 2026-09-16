# U03 CD-05 D09 Initial Candidate Re-certification v0.2.1

> 对象：`PR-U03-D09-001@0.2.1-candidate`。  
> 状态：`DECISION_COMPLETE / PASSED_FOR_INITIAL_CANDIDATE / TARGETED_RECERTIFICATION / NOT_GATE_B / NOT_GATE_C / NOT_FOR_PRODUCTION`  
> 本记录只重新判定 CD-05 对 0.2.1 targeted candidate 是否成立。

---

## 1. Inputs

```text
policy_candidate_ref = PR-U03-D09-001@0.2.1-candidate
policy_candidate_state = CANDIDATE_FROZEN
policy_freeze_record_ref = U03_D09_Policy_Candidate_Freeze_Record_v0.2.1.md

rule_release_ref = RR-U03-RISK-001@0.2.1-candidate / CANDIDATE_FROZEN
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE / CANDIDATE_FROZEN
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate / CANDIDATE_FROZEN
policy_pair_ref = PF-U03-C-POLICY-001 / CANDIDATE_FROZEN

D48 historical eval reuse = APPROVED_FOR_UNCHANGED_SEMANTICS
targeted delta fixtures = 6 / APPROVE / APPROVE / PASS
scope-entry missingness review = APPROVE / APPROVE
```

---

## 2. Delta Boundary

Only approved delta from 0.2.0:

```text
pregnancy / puerperium
= OUTSIDE_CURRENT_U03_WHOLE_POLICY_SLICE
```

Formal P0 handling:

```text
TRUE
→ FAILED / NONE / OVERALL_POLICY_SCOPE_MISMATCH

UNKNOWN / NOT_ASKED / NOT_ESTABLISHED
→ FAILED / NONE / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED

FALSE
→ continue remaining overall-scope validation
```

Unchanged:

```text
6 branch identities
P0 > P1 > P2 > P3 > P4 > P5
HIGH_RISK / CAUTION / NO_HIGH_RISK_SIGNAL mappings
5 baseline + 2 conditional family denominator structure
after whole-policy scope entry succeeds
```

---

## 3. Decision

All CD-05 initial-candidate conditions remain satisfied for the new targeted candidate.

```text
CD-05
= PASSED_FOR_INITIAL_CANDIDATE
= candidate_ref PR-U03-D09-001@0.2.1-candidate
```

Historical 0.2.0 decision remains valid only for the historical candidate and is not overwritten.

---

## 4. Boundary

```text
CD-05 PASS
!= Gate B PASS
!= Gate C PASS
!= runtime authorization
!= production authorization
```

Next required step remains:

```text
C / D / E cross-consistency re-review
```
