# U03 Clinical Risk Golden Case Fixtures v0.2

> 角色：Gate C / CD-06 Golden Case 的受控 Clinical State fixture registry。  
> 状态：`REVIEWED / APPROVED_FOR_EVALUATION_INPUT / NOT_EXECUTED / NOT_FOR_PRODUCTION`。  
> 再审记录：`U03_CD06_Evaluation_ReReview_Record_v0.2.md`。  
> 依据：`U03_CD06_Evaluation_Revision_Task_v0.1.md`。  
> 本文件只把已批准 Gate A/B/Candidate policy 语义结构化为可执行评估输入；不新增医学来源、规则、阈值或 disposition。

## 1. Common Binding

所有 fixture 默认绑定：

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
pediatrics = FALSE
pregnancy_or_puerperium = FALSE
clinical_state_version = CURRENT unless explicitly overridden
```

除 scope / stale / release-mismatch 专项 fixture 外，overall policy scope 均已建立。

## 2. Fixture Registry

| Fixture | Controlled clinical-state facts / governed result inputs |
|---|---|
| FX-GC-001 | `EV-RF-RESP-001=PRESENT`; C-RULE-RESP-001 expected MATCHED |
| FX-GC-002 | sudden focal/unilateral neuro context; `EV-MNM-NEURO-001=PRESENT`; C-RULE-NEURO-001 MATCHED |
| FX-GC-003 | sudden speech/language context; `EV-MNM-NEURO-002=PRESENT`; C-RULE-NEURO-002 MATCHED |
| FX-GC-004 | approved chest-pain-like scope; `EV-MNM-CARD-001=PRESENT`; C-RULE-CARD-001 MATCHED |
| FX-GC-005 | rapid systemic allergic ABC scope; `EV-RF-ALLERGY-001=PRESENT`; C-RULE-ALLERGY-001 MATCHED |
| FX-GC-006 | independently-established NHS dyspnoea emergency context + `EV-RF-APPEAR-001=PRESENT`; C-RULE-DYSPNOEA-APPEAR-001 MATCHED |
| FX-GC-007 | independently-established NHS dyspnoea emergency context + `EV-RF-NEURO-001=PRESENT`; C-RULE-DYSPNOEA-CONFUSION-001 MATCHED |
| FX-GC-008 | independently-established suspected-sepsis context + shared scope valid + `RR=25`; C-RULE-SEPSIS-RR-HIGH-001 MATCHED |
| FX-GC-009 | independently-established suspected-sepsis context + shared scope valid + `RR=21`; only RR MODHIGH positive |
| FX-GC-010 | independently-established suspected-sepsis context + shared scope valid + `SBP=90`; usual BP UNKNOWN; absolute SBP HIGH MATCHED |
| FX-GC-011 | independently-established suspected-sepsis context + shared scope valid + `SBP=95`; SBP MODHIGH MATCHED; no high-class signal |
| FX-GC-012 | baseline 5 all resolvable NO_MATCH; dyspnoea + sepsis families governed NOT_APPLICABLE; no insufficiency |
| FX-GC-013 | independently-established suspected-sepsis context + shared scope valid + `HR=100`; HR MODHIGH MATCHED only |
| FX-GC-014 | baseline 5 complete NO_MATCH; sepsis family specialized scope mismatch; dyspnoea NOT_APPLICABLE |
| FX-GC-015 | baseline 5 complete NO_MATCH; dyspnoea family specialized scope mismatch; sepsis NOT_APPLICABLE |
| FX-GC-016 | one applicable HIGH-class matched result + distinct applicable `INPUT_INSUFFICIENT`; optional MODHIGH match |
| FX-GC-017 | no HIGH; required governed evidence state `UNKNOWN` → applicable insufficiency |
| FX-GC-018 | no HIGH; required governed measurement state `UNMEASURED` → applicable insufficiency |
| FX-GC-019 | no HIGH; required remote evidence `REMOTE_NOT_OBSERVED` → applicable insufficiency |
| FX-GC-020 | `pregnancy_or_puerperium=TRUE`; remaining scope fields otherwise valid |
| FX-GC-021 | `pregnancy_or_puerperium=UNKNOWN` |
| FX-GC-022 | `pregnancy_or_puerperium=NOT_ASKED` |
| FX-GC-023 | `pregnancy_or_puerperium=NOT_ESTABLISHED` |
| FX-GC-024 | stale clinical-state version + otherwise valid C-RULE-RESP-001 HIGH input |
| FX-GC-025 | one governed binding ref deliberately differs from current immutable set; no mutable/latest fallback allowed |
| FX-GC-026 | same conditional family contains incompatible governed execution states violating shared-scope invariant, e.g. `SCOPE_MISMATCH + MATCHED` |
| FX-GC-027 | one MODHIGH/CAUTION-class match + distinct `INSUFFICIENT_APPLICABLE`; no HIGH |
| FX-GC-028 | first execution: C-RULE-RESP-001 HIGH input with idempotency identity `IDEMP-GC-028`; second execution replays identical accepted event + same identity |
| FX-GC-029 | independently-established suspected-sepsis context + shared scope valid + `EV-RF-APPEAR-001=PRESENT`; C-RULE-SEPSIS-APPEAR-HIGH-001 MATCHED |
| FX-GC-030 | independently-established suspected-sepsis context + shared scope valid + `EV-RF-SEPSIS-001=PRESENT`; C-RULE-SEPSIS-RASH-HIGH-001 MATCHED |
| FX-GC-031 | independently-established suspected-sepsis context + shared scope valid + `HR=131`; C-RULE-SEPSIS-HR-HIGH-001 MATCHED |

## 3. Fixture Governance

每个 fixture 的 context / evidence provenance 必须来自当前 frozen A/B/E/C/D authority。特别保持：

```text
current C rule result cannot create its own required scope context
UNKNOWN != FALSE
UNMEASURED != NORMAL
REMOTE_NOT_OBSERVED != EXCLUDED
whole-policy scope failure occurs before denominator construction
```

Fixture 本身不是新的 Clinical Truth，也不得被 runtime 作为生产病例库消费。
