# U03 Clinical Risk EvalSet Coverage Manifest Draft v0.2

> 对象：Gate C / CD-06 revised Coverage Manifest。  
> 状态：`REVIEWED / APPROVED_FOR_EVALUATION / BF-CD06-01_CLOSED / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 再审记录：`U03_CD06_Evaluation_ReReview_Record_v0.2.md`。  
> 依据：`U03_CD06_Evaluation_Revision_Task_v0.1.md`。  
> 绑定：E `0.1.0-candidate` + C `0.2.1-candidate` + Coverage `V0_2_1_CANDIDATE` + D `0.2.1-candidate`。

## 1. Current Evaluation Binding

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
golden_case_pack_ref = U03_Clinical_Risk_Golden_Cases_Draft_v0.2.md
fixture_registry_ref = U03_Clinical_Risk_Golden_Case_Fixtures_v0.2.md
safety_suite_ref = U03_Clinical_Risk_Safety_Suite_Draft_v0.1.md
```

## 2. Coverage Matrix

| Coverage dimension | Required | Candidate case refs |
|---|---|---|
| evidence category | YES | GC-001..GC-011, GC-029..GC-031, SS-001..SS-004 |
| all 15 active C rules | YES | dedicated map in §3; all 15 now have positive representative |
| D P0 | YES | GC-020..GC-025, SS-005..SS-010 |
| D P1 HIGH | YES | GC-001..GC-008, GC-010, GC-016, GC-029..GC-031 |
| D P2 insufficiency | YES | GC-017..GC-019, GC-027, SS-001..SS-004 |
| D P3 CAUTION | YES | GC-009, GC-011, GC-013 |
| D P4 NO_HIGH_RISK_SIGNAL | YES | GC-012, GC-014, GC-015, SS-011 |
| D P5 conflict | YES | GC-026, SS-012 |
| whole-policy scope TRUE | YES | GC-020, SS-005 |
| whole-policy scope UNKNOWN | YES | GC-021, SS-006 |
| whole-policy scope NOT_ASKED | YES | GC-022, SS-007 |
| whole-policy scope NOT_ESTABLISHED | YES | GC-023, SS-008 |
| specialized family scope mismatch | YES | GC-014, GC-015 |
| missingness UNKNOWN | YES | GC-017, SS-001 |
| UNMEASURED | YES | GC-018, SS-002 |
| REMOTE_NOT_OBSERVED | YES | GC-019, SS-003 |
| conflict | YES | GC-026, SS-012 |
| stale currentness | YES | GC-024, SS-009 |
| release mismatch | YES | GC-025, SS-010 |
| multi-hit HIGH | YES | GC-016 |
| HIGH + insufficiency | YES | GC-016 |
| CAUTION + insufficiency | YES | GC-027 |
| denominator complete / all NO_MATCH | YES | GC-012, GC-014, GC-015 |
| idempotent replay | YES | GC-028, SS-013 |
| duplicate clinical effect prevention | YES | GC-028, SS-013 |
| direct candidate commit prohibition | YES | SS-014 |
| C signal != D disposition | YES | SS-015 |
| D decision != U04 decision | YES | SS-016 |
| mutable latest alias prohibited | YES | SS-017 |
| unapproved release prohibited | YES | SS-018 |
| knowledge authority boundary | YES | SS-019 |
| scope expansion prohibition | YES | SS-020 |

## 3. Corrected 15-rule Representative Coverage

### Baseline 5

```text
C-RULE-RESP-001 → GC-001
C-RULE-NEURO-001 → GC-002
C-RULE-NEURO-002 → GC-003
C-RULE-CARD-001 → GC-004
C-RULE-ALLERGY-001 → GC-005
```

### NHS dyspnoea family

```text
C-RULE-DYSPNOEA-APPEAR-001 → GC-006
C-RULE-DYSPNOEA-CONFUSION-001 → GC-007
```

### NG253 sepsis family

```text
C-RULE-SEPSIS-RR-HIGH-001       → GC-008
C-RULE-SEPSIS-RR-MODHIGH-001    → GC-009
C-RULE-SEPSIS-SBP-HIGH-001      → GC-010
C-RULE-SEPSIS-SBP-MODHIGH-001   → GC-011
C-RULE-SEPSIS-HR-HIGH-001       → GC-031
C-RULE-SEPSIS-HR-MODHIGH-001    → GC-013
C-RULE-SEPSIS-APPEAR-HIGH-001   → GC-029
C-RULE-SEPSIS-RASH-HIGH-001     → GC-030
```

因此：

```text
active C rule count = 15
representative positive case count = 15 / 15
misbound APPEAR/RASH/HR-HIGH refs = 0
```

GC-014 / GC-015 保留为 specialized-family `SCOPE_MISMATCH → P4`；GC-016 保留为 `HIGH + insufficiency` precedence。它们不再承担 dedicated positive representative 角色。

Boundary threshold exhaustive coverage 继续引用已审核 C57；本 Gate C manifest 负责 representative end-to-end policy behavior，不重复复制全部 pre-freeze threshold fixtures。

## 4. Policy Branch Coverage

```text
P0 = GC-020..GC-025
P1 = GC-001..GC-008, GC-010, GC-016, GC-029..GC-031
P2 = GC-017..GC-019, GC-027
P3 = GC-009, GC-011, GC-013
P4 = GC-012, GC-014, GC-015
P5 = GC-026
```

## 5. Schema / Fixture Coverage

```text
golden_case_count = 31
schema_minimum_fields_present = 31 / 31
clinical_state_fixture_refs_present = 31 / 31
fixture_registry = U03_Clinical_Risk_Golden_Case_Fixtures_v0.2.md
```

Required per-case fields now include:

```text
case_version
clinical_state_fixture_ref
clinical_state_version
input_fact_refs[]
expected_evidence_refs[]
expected_rule_refs[]
expected_policy_ref
expected_result_status
expected_disposition
expected_reason_code
must_not_output[]
must_not_commit[]
source_refs[]
rationale_ref
provenance_refs[]
```

## 6. Negative Assertion Coverage

Safety Suite v0.1 remains content-approved and critical-blocking. Mandatory invariants remain mapped to SS-001..SS-020; no Safety Suite semantic change is introduced by this revision.

## 7. Current Verdict

```text
required dimensions = DECLARED
15 active rule mapping = APPROVED / 15_OF_15
Golden Case schema coverage = 31_OF_31
Medical re-review = COMPLETE / APPROVE
Policy/Eval re-review = COMPLETE / APPROVE
BF-CD06-01 = CLOSED
BF-CD06-02 = CLOSED
Execution evidence = NOT_STARTED
CD-06 = REVIEW_READY
Gate C = NOT_PASSED
```
