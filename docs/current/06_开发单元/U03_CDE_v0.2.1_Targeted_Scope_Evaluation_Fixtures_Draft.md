# U03 C / D / E v0.2.1 Targeted Scope Evaluation Fixtures

> 对象：0.2.1 pregnancy/puerperium whole-policy scope delta。  
> 状态：`TARGETED_FIXTURE_REVIEW_COMPLETE / TARGETED_EVAL_PASS / NOT_GATE_C / NOT_FOR_PRODUCTION`  
> 复用依据：`U03_CDE_v0.2.1_Evaluation_Reuse_Assessment.md`。  
> 审核记录：`U03_CDE_v0.2.1_Scope_Entry_Missingness_Review_Record.md`。  
> 本 pack 只验证 0.2.1 新增的 whole-policy scope-entry 语义；历史 C57 / D48 对未变化语义继续作为复用证据，不重复生成。

---

## 1. Bound Drafts

```text
C scope revision = RR-U03-RISK-001@0.2.1-draft
D scope revision = PR-U03-D09-001@0.2.1-draft
Coverage revision = U03_D09_COVERAGE_V0_2_1_DRAFT
Knowledge release = KR-U03-SOURCE-001@0.1.0-candidate
scope-entry decision = U03_CDE_v0.2.1_Scope_Entry_Missingness_Decision_Draft.md / APPROVED
```

Historical reusable evidence:

```text
C57 = U03_C_PreFreeze_Evaluation_Fixtures_v0.2.md
D48 = U03_D09_PreFreeze_Evaluation_Fixtures_v0.1.md
```

---

## 2. Targeted Delta Fixtures

### TGT-CDE-01 — Confirmed pregnancy/puerperium TRUE

```text
pregnancy_or_puerperium = TRUE
other overall-scope inputs = otherwise valid
```

Expected:

```text
whole-policy scope = MISMATCH
C current-slice rule evaluation = NOT_ENTERED
coverage denominator = NOT_CONSTRUCTED
D09 = P0 / FAILED / NONE
reason_code = OVERALL_POLICY_SCOPE_MISMATCH
```

Negative assertions:

```text
must not treat only sepsis family as NOT_APPLICABLE
must not enter baseline 5 denominator
must not output HIGH_RISK / CAUTION / NO_HIGH_RISK_SIGNAL
```

### TGT-CDE-02 — Confirmed FALSE permits downstream evaluation

```text
pregnancy_or_puerperium = FALSE
other overall-scope inputs = satisfied
```

Expected:

```text
whole-policy pregnancy/puerperium gate = SATISFIED
coverage denominator construction = ALLOWED
subsequent behavior = governed by existing C57 / D48 evidence
```

Negative assertion:

```text
FALSE alone does not imply LOW_RISK / SAFE / NO_HIGH_RISK_SIGNAL
```

### TGT-CDE-03 — UNKNOWN fails before denominator

```text
pregnancy_or_puerperium = UNKNOWN
```

Expected:

```text
whole-policy scope = NOT_ESTABLISHED
C current-slice rule evaluation = NOT_ENTERED
coverage denominator = NOT_CONSTRUCTED
D09 = P0 / FAILED / NONE
reason_code = OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
```

Negative assertions:

```text
UNKNOWN != FALSE
must not enter P2 denominator semantics
must not output P3/P4
```

### TGT-CDE-04 — NOT_ASKED fails before denominator

```text
pregnancy_or_puerperium = NOT_ASKED
```

Expected:

```text
whole-policy scope = NOT_ESTABLISHED
coverage denominator = NOT_CONSTRUCTED
D09 = P0 / FAILED / NONE
reason_code = OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
```

Negative assertions:

```text
NOT_ASKED != FALSE
must not enter baseline denominator
```

### TGT-CDE-05 — NOT_ESTABLISHED fails before denominator

```text
pregnancy_or_puerperium = NOT_ESTABLISHED
```

Expected:

```text
whole-policy scope = NOT_ESTABLISHED
coverage denominator = NOT_CONSTRUCTED
D09 = P0 / FAILED / NONE
reason_code = OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
```

Negative assertions:

```text
must not become OVERALL_POLICY_SCOPE_MISMATCH unless TRUE is established
must not become family-level RULE_SIGNAL_SCOPE_MISMATCH
```

### TGT-CDE-06 — P0 scope-entry failure dominates existing HIGH input

```text
pregnancy_or_puerperium = UNKNOWN
plus an otherwise valid historical HIGH-class C result is present
```

Expected:

```text
P0 > P1
D09 = FAILED / NONE / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
```

Negative assertion:

```text
HIGH must not override unestablished whole-policy scope
```

---

## 3. Reuse Assertions

This targeted pack does not re-test unchanged semantics:

```text
15 C rule predicates / thresholds / signal vocabulary
C Missingness Policy
C sepsis shared-scope policy
D 6 branch identities
P0 > P1 > P2 > P3 > P4 > P5
5 baseline + 2 conditional family denominator membership after whole-policy entry succeeds
family-level INPUT_INSUFFICIENT / SCOPE_MISMATCH mapping
```

Those remain supported by historical C57 / D48 review records.

---

## 4. Review Result

```text
fixture_count = 6
TGT-CDE-01..06 Medical = APPROVE
TGT-CDE-01..06 Technical/Eval = APPROVE
blocking finding = 0
Targeted Eval PASS = YES
```

This pack is not Gate C and does not replace the future Clinical EvalSet / Safety Suite.
