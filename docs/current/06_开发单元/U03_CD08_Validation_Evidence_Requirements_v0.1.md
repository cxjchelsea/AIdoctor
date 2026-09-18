# U03 CD-08 Validation Evidence Requirements v0.1

> 对象：CD-08 formal post-implementation clinical validation 的证据冻结要求。  
> 状态：`EVIDENCE_REQUIREMENTS_FROZEN_FOR_READINESS / NOT_EXECUTION_AUTHORIZATION`。

## 1. Evidence principle

CD-08 必须能够证明“哪个真实 runtime、用哪套临床 release、在哪个 Clinical State / Run 身份下，对哪个 governed case，产生了什么临床结果”。

```text
summary PASS
!= case-level evidence
CI success
!= clinical validation PASS
runtime verification artifact
!= CD-08 clinical evidence package
```

## 2. Required identity evidence

正式 evidence package 至少冻结：

```text
E8-01 exact CD-07 implementation/runtime SHA under validation
E8-02 exact CD-08 validation code/workflow SHA
E8-03 run_id / run_attempt / environment identity
E8-04 EvalSet release ref
E8-05 exact Knowledge / Rule / Coverage / Policy / Policy-Pair refs
E8-06 case inventory and executable/excluded accounting
E8-07 source Clinical State Version per case
E8-08 Thread / Run / Event identities per case
```

## 3. Required case-level clinical evidence

每个 executable Golden / Safety case 必须至少有：

```text
case identity/version
fixture/input reference
expected governed clinical outcome
actual C02 governed result
accepted evidence/provenance refs
actual D09 governed decision
K09 proposal identity/content summary
P01/StateCommitter outcome where applicable
committed Clinical State Version where applicable
P05 trace/finalization identity
actual U03 outbound producer status where applicable
forbidden-output assertion result
clinical comparison verdict
errors/findings
```

## 4. Required aggregate evidence

```text
approved Golden identities = 31
executable Golden expected = 30
excluded Golden = GC-026

approved critical Safety identities = 20
executable critical Safety expected = 19
excluded Safety = SS-012
```

Formal bundle must report counts from the executed case records rather than hand-entered summary values.

## 5. Runtime-path proof

Evidence must demonstrate that case execution used the actual CD-07 governed runtime path and not only `tools/u03_gatec_eval` evaluation-only logic.

At minimum capture machine-verifiable proof of:

```text
C02 runtime invocation
D09 runtime owner execution
K09/P01 boundary traversal where applicable
StateCommitter outcome where applicable
P05 trace/finalization
exact release binding
explicit non-production environment
```

## 6. Safety/failure evidence

Must preserve and verify:

```text
FAILED != NO_MATCH
technical failure != low risk
stale version != negative clinical result
missing evidence != safe
release mismatch != clinical no-risk
trace failure != Clinical Truth
```

If a commit succeeds but trace persistence fails, the existing CD-07 reconciliation behavior must remain observable and no normal downstream handoff may be falsely asserted.

## 7. Evidence integrity

Formal package must include:

```text
- machine-readable case bundle
- human-readable summary
- hashes for evidence files
- artifact/run identity
- exact source/test commits
- exact governed release refs
- known exclusions/residual risks
```

GitHub Actions artifact may be a retained copy but must not be the sole durable evidence location if retention is finite. A persistent repository/PR review record or equivalent durable evidence index is required.

## 8. Review authority

Evidence generation may be automated. Clinical acceptance may not be inferred from automation alone.

Required separation:

```text
machine-verified execution facts
!= clinical interpretation
!= independent clinical/governance approval
```

A formal CD-08 PASS requires an independent review of the frozen evidence. Any finding that could imply a change to clinical semantics requires Medical/Clinical Owner adjudication.

## 9. Exclusions

This evidence package must not contain or imply authorization for:

```text
production activation
real-patient traffic
production Clinical State mutation
release publication
U04 owner execution/routing
new clinical semantics
```

## 10. Verdict

```text
CD-08 Evidence Requirements = DEFINED / FROZEN_FOR_READINESS
CD-08 Evidence Execution = NOT_STARTED
CD-08 Formal Clinical Review = NOT_STARTED
CD-08 Execution Authorization = NOT_GRANTED
```
