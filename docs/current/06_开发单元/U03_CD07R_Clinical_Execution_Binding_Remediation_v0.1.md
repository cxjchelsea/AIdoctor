# U03 CD-07R Clinical Execution Binding Remediation v0.1

## 1. Why this remediation exists

A post-merge audit of the CD-07 non-production runtime slice found that PR #91 proved the structural runtime chain but did not prove that the Java runtime executed the Gate-C-frozen clinical semantics.

The previous V8 E2E created C02 and D09 inline inside the test:

```text
synthetic C02 lambda
→ governed runtime chain
→ synthetic anonymous D09
```

Therefore the prior result remains valid as structural integration evidence, but it is insufficient as clinical-execution binding evidence.

This record does not invalidate Gate C. Gate C remains the frozen governed semantic source.

## 2. Frozen semantic source

CD-07R is strictly bound to the already governed Gate-C objects:

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
PF-U03-C-POLICY-001
```

The executable semantic reference is:

```text
tools/u03_gatec_eval/evaluator.py
```

CD-07R may mechanically port these frozen semantics. It may not add thresholds, outcomes, populations, regions, clinical interpretations, or policy branches.

## 3. Remediation scope

CD-07R adds only the missing non-production execution binding:

```text
accepted evidence identity/provenance
+ typed accepted clinical fact values
+ exact Gate-C frozen release tuple
        ↓
U03GateCFrozenRuleEvaluator (concrete C02)
        ↓
U03RiskAssessmentCandidate + frozen rule execution evidence
        ↓
U03GateCFrozenDecisionPort (concrete D09)
        ↓
existing K09 proposal / P01 / StateCommitter / P05 / outbound chain
```

The clinical input values are loaded through `U03GateCClinicalInputPort`. This is deliberately a port: CD-07R does not create or authorize a production Clinical State adapter.

## 4. Runtime truth boundaries

The following remain distinct:

```text
Clinical Truth
!= accepted clinical fact transport
!= C02 rule execution result
!= D09 business decision
!= committed Clinical State
!= trace
```

`U03AcceptedEvidenceBinding` remains identity/provenance only. It is not reinterpreted as containing clinical values.

`U03GateCClinicalInput` carries values required by the frozen rules, but does not itself grant clinical truth or mutation authority.

## 5. Concrete implementation

CD-07R adds:

```text
U03GateCClinicalInput
U03GateCClinicalInputPort
U03GateCRuleResult
U03GateCRuleEvaluation
U03GateCFrozenRuleEvaluator
U03GateCFrozenDecisionPort
```

`U03RiskAssessmentCandidate` gains an optional `gateCEvaluation` payload for the frozen C02 path. Existing historical candidate construction remains compatible.

`U03DecisionService` is corrected so the explicit governed D09 path can return the Gate-C-defined fail-closed results such as:

```text
INSUFFICIENT_INFORMATION
OVERALL_POLICY_SCOPE_MISMATCH
OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
INVALID_INPUT
DEPENDENCY_FAILURE
UNRESOLVABLE_CONFLICT
```

The historical component path still requires a normal VALID disposition.

## 6. Verification requirements

The remediation is not considered verified merely because the code exists.

Required evidence:

```text
1. diagnosis-service compile = PASS
2. focused concrete C02/D09 tests = PASS
3. NON_PRODUCTION_RUNTIME_E2E uses concrete Gate-C C02 and D09 = PASS
4. full diagnosis-service regression = PASS
5. Python Gate-C semantic regression remains 30/30 Golden + 19/19 Safety = PASS
6. production mutation = false
7. real-patient traffic = false
8. U04 execution = false
```

The E2E clinical values may remain synthetic; what must no longer be synthetic is the C02/D09 implementation itself.

## 7. Governance state while this PR is under review

```text
Gate C = PASS / FROZEN
CD-07 structural runtime integration = PASS
CD-07 clinical execution integration = REMEDIATION_IN_PROGRESS
BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY = OPEN
U04 readiness final PASS = NOT_ALLOWED_YET
U04 implementation authorization = NOT_GRANTED
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
Real-patient traffic = NOT_AUTHORIZED
```

Only after this remediation is independently verified and explicitly accepted may the U03 runtime clinical dependency be re-reviewed for closure.

## 8. Non-goals

CD-07R does not authorize or implement:

```text
production clinical input adapters
release publication or production activation
real-patient execution
U04 execution or routing
U14 execution
new pediatric or pregnancy semantics
new regional/localized semantics
new clinical thresholds
new D09 dispositions
```
