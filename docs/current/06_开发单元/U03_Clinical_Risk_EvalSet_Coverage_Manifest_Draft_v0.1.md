# U03 Clinical Risk EvalSet Coverage Manifest Draft v0.1

> 对象：Gate C / CD-06 的 Coverage Manifest。  
> 状态：`DRAFT / COVERAGE_DEFINED / CASE_BINDING_PENDING_REVIEW / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 绑定：E `0.1.0-candidate` + C `0.2.1-candidate` + Coverage `V0_2_1_CANDIDATE` + D `0.2.1-candidate`。

---

## 1. Current Evaluation Binding

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
```

---

## 2. Coverage Matrix

| Coverage dimension | Required | Candidate case refs |
|---|---|---|
| evidence category | YES | GC-001..GC-010, SS-001..SS-004 |
| all 15 active C rules | YES | GC-001..GC-015 |
| D P0 | YES | GC-020..GC-025, SS-005..SS-010 |
| D P1 HIGH | YES | GC-001..GC-008, GC-016 |
| D P2 insufficiency | YES | GC-017..GC-019, SS-001..SS-004 |
| D P3 CAUTION | YES | GC-009..GC-011 |
| D P4 NO_HIGH_RISK_SIGNAL | YES | GC-012..GC-015, SS-011 |
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
| denominator complete / all NO_MATCH | YES | GC-012..GC-015 |
| idempotent replay | YES | GC-028, SS-013 |
| duplicate clinical effect prevention | YES | GC-028, SS-013 |
| direct candidate commit prohibition | YES | SS-014 |
| C signal != D disposition | YES | SS-015 |
| D decision != U04 decision | YES | SS-016 |
| mutable latest alias prohibited | YES | SS-017 |
| unapproved release prohibited | YES | SS-018 |
| knowledge authority boundary | YES | SS-019 |
| scope expansion prohibition | YES | SS-020 |

---

## 3. Rule-family Coverage

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
RR HIGH → GC-008
RR MODHIGH → GC-009
SBP HIGH → GC-010
SBP MODHIGH → GC-011
HR HIGH → GC-016
HR MODHIGH → GC-013
APPEAR HIGH → GC-014
RASH HIGH → GC-015
```

Boundary values remain inherited from approved C57 evidence; Gate C cases verify representative clinical-policy behavior rather than duplicating every pre-freeze threshold fixture.

---

## 4. Policy Branch Coverage

```text
P0 = GC-020..GC-025
P1 = GC-001..GC-008, GC-010, GC-016
P2 = GC-017..GC-019, GC-027
P3 = GC-009, GC-011, GC-013
P4 = GC-012, GC-014, GC-015
P5 = GC-026
```

---

## 5. Negative Assertion Coverage

Mandatory safety invariants:

```text
UNKNOWN != NEGATIVE → SS-001
UNMEASURED != NORMAL → SS-002
REMOTE_NOT_OBSERVED != EXCLUDED → SS-003
NOT_ASKED != NO → SS-004
scope TRUE != family-only mismatch → SS-005
scope missing != FALSE → SS-006..SS-008
stale != current → SS-009
release mismatch != accepted → SS-010
NO_HIGH_RISK_SIGNAL != SAFE → SS-011
conflict != arbitrary branch → SS-012
replay != duplicate clinical effect → SS-013
candidate != direct commit → SS-014
Rule Signal != D disposition → SS-015
D09 Decision != U04 decision → SS-016
latest alias prohibited → SS-017
unapproved release prohibited → SS-018
E != executable-rule owner → SS-019
scope expansion prohibited → SS-020
```

---

## 6. Coverage Verdict Before Review

```text
required dimensions = DECLARED
case mapping = COMPLETE_FOR_DRAFT
Medical validation = PENDING
Policy/Eval validation = PENDING
Execution evidence = NOT_STARTED
```

该 manifest 只有在 case pack 审核后才能升级为 `REVIEWED`；当前不构成 CD-06 REVIEW_READY。
