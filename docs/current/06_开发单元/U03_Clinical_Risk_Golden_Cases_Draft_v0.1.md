# U03 Clinical Risk Golden Cases Draft v0.1

> 对象：Gate C / CD-06 Clinical Golden Case candidate pack。  
> 状态：`CANDIDATE_CASES_AVAILABLE / MEDICAL_REVIEW_PENDING / POLICY_EVAL_REVIEW_PENDING / NOT_APPROVED / NOT_FOR_PRODUCTION`。  
> 说明：这些 case 由当前已批准 C/D/E policy 机械展开为评估候选；**尚不是 Medical-approved Golden Cases**。

## 1. Common Binding

所有 case 统一绑定：

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
review_status = MEDICAL_REVIEW_PENDING
```

除非 case 明确测试 P0/versioning，否则：

```text
clinical_state_version = CURRENT
release refs = exact / resolvable
whole-policy scope = established
pregnancy_or_puerperium = FALSE
pediatrics = FALSE
region/channel = in-scope
```

## 2. Candidate Cases

| ID | Purpose / controlled input | Expected D09 | Must not |
|---|---|---|---|
| GC-001 | `C-RULE-RESP-001=MATCHED / CRITICAL_RED_FLAG` | `VALID / HIGH_RISK / HIGH_RISK_RULE_SIGNAL_PRESENT` | SAFE/NORMAL |
| GC-002 | `C-RULE-NEURO-001=MATCHED / MUST_NOT_MISS` | `VALID / HIGH_RISK` | diagnostic certainty |
| GC-003 | `C-RULE-NEURO-002=MATCHED / MUST_NOT_MISS` | `VALID / HIGH_RISK` | first-hit mutation |
| GC-004 | `C-RULE-CARD-001=MATCHED / MUST_NOT_MISS` | `VALID / HIGH_RISK` | claim confirmed ACS/MI |
| GC-005 | `C-RULE-ALLERGY-001=MATCHED / CRITICAL_RED_FLAG` | `VALID / HIGH_RISK` | expand to mild allergy |
| GC-006 | dyspnoea context independently established + appearance rule matched | `VALID / HIGH_RISK` | appearance creates context |
| GC-007 | dyspnoea context established + confusion rule matched | `VALID / HIGH_RISK` | globalize confusion red flag |
| GC-008 | sepsis shared scope valid + RR high criterion | `VALID / HIGH_RISK` | RR creates suspected_sepsis |
| GC-009 | sepsis scope valid + RR moderate-high only | `VALID / CAUTION / MODERATE_HIGH_RULE_SIGNAL_PRESENT` | HIGH without high-class signal |
| GC-010 | sepsis scope valid + SBP high criterion | `VALID / HIGH_RISK` | downgrade because usual BP unavailable when absolute high branch matched |
| GC-011 | sepsis scope valid + SBP moderate-high only | `VALID / CAUTION` | convert to NO_HIGH |
| GC-012 | baseline 5 all NO_MATCH; both conditional families NOT_APPLICABLE; no insufficiency | `VALID / NO_HIGH_RISK_SIGNAL / COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL` | SAFE/NORMAL/no disease |
| GC-013 | sepsis scope valid + HR moderate-high only | `VALID / CAUTION` | P4 |
| GC-014 | baseline complete NO_MATCH; sepsis family specialized scope mismatch; dyspnoea not applicable | `VALID / NO_HIGH_RISK_SIGNAL` | whole-policy FAILED solely from family mismatch |
| GC-015 | baseline complete NO_MATCH; dyspnoea family specialized scope mismatch; sepsis not applicable | `VALID / NO_HIGH_RISK_SIGNAL` | whole-policy scope mismatch |
| GC-016 | one HIGH-class match + another applicable INPUT_INSUFFICIENT + optional CAUTION | `VALID / HIGH_RISK`; preserve matched + insufficient refs | P2/P3 downgrade |
| GC-017 | no HIGH; required evidence `UNKNOWN` → applicable insufficiency | `FAILED / NONE / INSUFFICIENT_INFORMATION` | NO_MATCH / NO_HIGH |
| GC-018 | no HIGH; required measurement `UNMEASURED` | `FAILED / NONE / INSUFFICIENT_INFORMATION` | treat normal |
| GC-019 | no HIGH; required remote evidence `REMOTE_NOT_OBSERVED` | `FAILED / NONE / INSUFFICIENT_INFORMATION` | treat excluded |
| GC-020 | `pregnancy_or_puerperium=TRUE` | `FAILED / NONE / OVERALL_POLICY_SCOPE_MISMATCH` | enter C evaluation / denominator |
| GC-021 | `pregnancy_or_puerperium=UNKNOWN` | `FAILED / NONE / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | FALSE / denominator |
| GC-022 | `pregnancy_or_puerperium=NOT_ASKED` | `FAILED / NONE / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | infer non-pregnant |
| GC-023 | `pregnancy_or_puerperium=NOT_ESTABLISHED` | `FAILED / NONE / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | family-level scope mismatch |
| GC-024 | stale `clinical_state_version` + otherwise valid HIGH input | `FAILED / NONE / STALE_INPUT` | HIGH disposition |
| GC-025 | wrong rule/knowledge/policy/coverage release binding | `FAILED / NONE / RELEASE_MISMATCH` | fallback to latest/current alias |
| GC-026 | incompatible conditional-family result state violating shared-scope invariant | `FAILED / NONE / UNRESOLVABLE_CONFLICT` | arbitrary/file-order branch |
| GC-027 | CAUTION-class match + applicable insufficiency, no HIGH | `FAILED / NONE / INSUFFICIENT_INFORMATION` | CAUTION |
| GC-028 | same accepted event replayed with identical idempotency identity | one clinical effect only; replay returns same/duplicate-safe outcome | duplicate Risk Assessment commit |

## 3. Expected Evidence / Rule Reference Requirements

每个 case 在正式 review 版必须继续填写：

```text
expected_evidence_refs[]
expected_rule_refs[]
expected_policy_ref
source_refs[]
rationale_ref
provenance_refs[]
```

当前 mapping 规则：

- GC-001..GC-015：引用相应 frozen C rule + Gate A/B evidence/source provenance；
- GC-016..GC-019、GC-027：引用 frozen missingness / coverage / D precedence；
- GC-020..GC-023：引用 A/E whole-slice scope + 0.2.1 scope-entry review；
- GC-024..GC-026：引用 D P0/P5 governance semantics；
- GC-028：引用 P01/K09 idempotent commit contract 与 U03 engineering path。

## 4. Review Questions

Medical Owner 至少逐 case 判断：

```text
clinical fixture 是否忠实表达受治理医学语义
expected clinical disposition 是否正确
negative assertion 是否足够且没有越权扩大医学结论
```

Policy/Evaluation Owner 至少判断：

```text
binding refs 是否精确
branch / reason code 是否一致
coverage 是否完整
case 是否与实现 owner 独立
```

## 5. Current Status

```text
candidate_case_count = 28
content = AVAILABLE
Medical Review = NOT_STARTED
Policy/Eval Review = NOT_STARTED
Approved Golden Cases = 0
CD-06 = NOT_REVIEW_READY
Gate C = NOT_PASSED
```
