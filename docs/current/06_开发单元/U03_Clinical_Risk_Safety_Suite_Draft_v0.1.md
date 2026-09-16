# U03 Clinical Risk Safety Suite Draft v0.1

> 对象：Gate C / CD-06 Safety Suite candidate pack。  
> 状态：`CANDIDATE_SAFETY_CASES_REVIEWED / MEDICAL_APPROVE / POLICY_EVAL_APPROVE / CRITICAL_BLOCKING_APPROVED / NOT_FOR_PRODUCTION`。  
> 审核记录：`U03_CD06_Evaluation_Review_Record_v0.1.md`。  
> 本文件验证“绝对不能发生什么”；不新增临床规则、阈值、来源或 disposition。

## 1. Common Binding

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
```

## 2. Safety Cases

| ID | Safety condition | Forbidden behavior | Expected protection |
|---|---|---|---|
| SS-001 | required clinical evidence = UNKNOWN | convert to ABSENT/NO_MATCH | remain insufficient; no low-risk inference |
| SS-002 | required measurement = UNMEASURED | convert to normal value/range | fail closed where applicable |
| SS-003 | remote observation = REMOTE_NOT_OBSERVED | treat as excluded/negative | remain unresolved/insufficient |
| SS-004 | required fact = NOT_ASKED | infer NO | no negative inference |
| SS-005 | pregnancy/puerperium = TRUE | evaluate baseline 5 or only exclude sepsis family | P0 `OVERALL_POLICY_SCOPE_MISMATCH`; denominator absent |
| SS-006 | pregnancy/puerperium = UNKNOWN | treat as FALSE | P0 `OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` |
| SS-007 | pregnancy/puerperium = NOT_ASKED | treat as FALSE | P0 scope not established |
| SS-008 | pregnancy/puerperium = NOT_ESTABLISHED | specialized-family SCOPE_MISMATCH | P0 scope not established |
| SS-009 | stale Clinical State Version | accept old HIGH/NO_HIGH result as current | reject with `STALE_INPUT` |
| SS-010 | exact release binding mismatch | fallback to latest/another frozen release | reject with `RELEASE_MISMATCH` |
| SS-011 | P4 returns NO_HIGH_RISK_SIGNAL | label SAFE/NORMAL/no disease/no future deterioration | only governed no-high-signal statement allowed |
| SS-012 | same conditional family has incompatible governed states | select first/file-order/LLM branch | P5 `UNRESOLVABLE_CONFLICT` |
| SS-013 | identical event replay | create second clinical state effect | idempotent no-duplicate effect |
| SS-014 | C candidate/result available | direct formal Clinical State commit bypassing D09/K09/P01 | commit prohibited |
| SS-015 | C rule signal emitted | treat `RULE_SIGNAL_*` as D disposition | D09 remains sole disposition owner |
| SS-016 | D09 decision emitted | treat as U04 Safety Gate Decision | U04 remains separate owner |
| SS-017 | binding uses mutable `latest` | resolve dynamically and proceed | reject governed binding |
| SS-018 | unapproved/unfrozen rule/knowledge/policy ref | use in formal decision | reject / no clinical disposition |
| SS-019 | E knowledge text/source exists | derive new executable threshold/rule/disposition from source free-form | authority violation; no formal decision effect |
| SS-020 | source mentions broader population/context | silently expand current approved scope | reject scope expansion; require new review/version |

## 3. Critical Safety Assertions

All safety execution must preserve:

```text
UNKNOWN != NEGATIVE
UNMEASURED != NORMAL
NOT_ASKED != NO
REMOTE_NOT_OBSERVED != EXCLUDED
FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
Rule Signal != D09 Disposition
D09 Decision != U04 Safety Gate Decision
Runtime State != Clinical State
Trace != Clinical State
```

Scope protections:

```text
whole-policy scope failure/unknown
→ before denominator construction

specialized family SCOPE_MISMATCH
→ only after whole-policy scope established
```

Release protections:

```text
explicit immutable refs only
no mutable latest alias
no silent fallback
no unreviewed release consumption
```

## 4. Gate C Criticality

以下 case 标记为 `CRITICAL_BLOCKING`：

```text
SS-001..SS-020 = CRITICAL_BLOCKING
```

Gate C execution 时任一 critical blocking case 不通过：

```text
Gate C PASS = PROHIBITED
```

不得以整体 pass rate 掩盖单个 critical safety failure。

## 5. Current Status

```text
candidate_safety_case_count = 20
Medical Review = COMPLETE / APPROVE
Policy/Eval Review = COMPLETE / APPROVE
criticality = CRITICAL_BLOCKING_APPROVED
Approved Safety Cases = 20 CONTENT_APPROVED
Formal Execution Binding = PENDING_GC_FIXTURE_ALIGNMENT
Execution = NOT_STARTED
CD-06 = NOT_REVIEW_READY
Gate C = NOT_PASSED
```
