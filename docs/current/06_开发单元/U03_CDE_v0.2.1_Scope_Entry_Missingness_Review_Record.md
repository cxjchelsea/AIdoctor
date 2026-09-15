# U03 C / D / E v0.2.1 Scope-entry Missingness Review Record

> 对象：`U03_CDE_v0.2.1_Scope_Entry_Missingness_Decision_Draft.md` + targeted scope fixtures。  
> 状态：`TECHNICAL_REVIEW_COMPLETE / MEDICAL_REVIEW_PENDING / BLOCKER_OPEN / NOT_FOR_PRODUCTION`  
> 本记录只审 whole-policy pregnancy/puerperium entry missingness；不重审 C57 / D48 的未变化语义。

---

## 1. Decision Review

| ID | Question | Medical | Technical |
|---|---|---|---|
| MISSING-SCOPE-R1 | UNKNOWN / NOT_ASKED / NOT_ESTABLISHED 是否在 whole-policy entry fail closed | PENDING | APPROVE |
| MISSING-SCOPE-R2 | 是否保持 P0，而不是 P2 | PENDING | APPROVE |
| MISSING-SCOPE-R3 | reason_code 是否采用 `OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | PENDING | APPROVE |
| MISSING-SCOPE-R4 | 是否与 TRUE → `OVERALL_POLICY_SCOPE_MISMATCH` 明确区分 | PENDING | APPROVE |

Technical rationale:

```text
P2 requires overall policy scope already established.
Scope-entry missingness occurs before denominator construction.
Therefore it belongs to P0 fail-closed handling.
```

---

## 2. Targeted Fixture Review

| Fixture | Medical | Technical/Eval | Focus |
|---|---|---|---|
| TGT-CDE-01 | PENDING | APPROVE | TRUE → P0 scope mismatch; no denominator |
| TGT-CDE-02 | PENDING | APPROVE | FALSE permits downstream governed evaluation only |
| TGT-CDE-03 | PENDING | APPROVE | UNKNOWN → P0 scope not established |
| TGT-CDE-04 | PENDING | APPROVE | NOT_ASKED → P0 scope not established |
| TGT-CDE-05 | PENDING | APPROVE | NOT_ESTABLISHED → P0 scope not established |
| TGT-CDE-06 | PENDING | APPROVE | P0 scope-entry failure > historical HIGH input |

Technical/Eval review finds no need to rebuild C57 / D48 because the revision does not change rule predicates, thresholds, signal vocabulary, D branch identities, precedence, or denominator membership after whole-policy entry succeeds.

---

## 3. Proposed Formal State Table

```text
TRUE
→ P0 / FAILED / NONE / OVERALL_POLICY_SCOPE_MISMATCH

FALSE
→ continue overall-scope validation
→ denominator may be constructed only if all scope predicates pass

UNKNOWN
NOT_ASKED
NOT_ESTABLISHED
→ P0 / FAILED / NONE / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
→ denominator NOT_CONSTRUCTED
```

---

## 4. Closure Rule

`BLOCKER-FZ-CDE-021-01` may close only when:

```text
MISSING-SCOPE-R1..R4 Medical = APPROVE
MISSING-SCOPE-R1..R4 Technical = APPROVE
TGT-CDE-01..06 Medical = APPROVE
TGT-CDE-01..06 Technical/Eval = APPROVE
blocking finding = 0
```

Current:

```text
Technical Review = COMPLETE / APPROVE
Medical Review = PENDING
blocking finding = PENDING_MEDICAL_DECISION
BLOCKER-FZ-CDE-021-01 = OPEN
Targeted Eval PASS = NO
new 0.2.1 candidates = NOT_CREATED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
```
