# U03 Gate C Evidence Capture Remediation v0.1

> 对象：`BF-GATEC-EVIDENCE-01 = DURABLE_CASE_LEVEL_EXECUTION_BUNDLE_NOT_FROZEN` 的 bounded remediation。  
> 基线：PR #89 / previous HEAD `06228a46aa7d1acc663e21672668a1ab2e760546`。  
> 实现 commit：`ca6e2b8760187f70704d96910cbbf09e8169f85a`。  
> 状态：`IMPLEMENTED / PENDING_TARGETED_EVIDENCE_CAPTURE_REVIEW / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 本修复只改变 evaluation evidence capture；不修改 C/D 临床语义、fixture、expected outcome、阈值、precedence、executable accounting 或 Runtime wiring。

---

## 1. Trigger

正式 Governed Evaluation run `35074643060` 已真实完成并 summary PASS，但该 run 未上传 artifact。

已冻结事实：

```text
run_id = 35074643060
event = workflow_dispatch
executed_sha = efd2199ff672077a27170ebcb2e9d494872b8c05
job = governed-offline-evaluation
conclusion = success

executable Golden = 30 / 30 PASS
executable Safety = 19 / 19 PASS
critical_safety_fail = 0
failed_non_case_checks = []
```

但完整 `result-bundle.json` 仅存在于 ephemeral runner，未形成 durable case-level evidence，因此：

```text
BF-GATEC-EVIDENCE-01 = OPEN
Gate C = NOT_PASSED
```

---

## 2. Authorized Remediation Boundary

本轮只允许：

```text
persist exact result-bundle.json as GitHub Actions artifact
bind immutable workflow execution provenance into durable bundle
remove/externalize stale harness-owned governance status strings
verify evidence identity before upload
record artifact id / url / digest / execution SHA / run id
```

本轮禁止：

```text
modify evaluator.py
modify cases.py
modify run_evaluation.py clinical execution semantics
change any C rule threshold/predicate
change D09 precedence or disposition semantics
change Golden/Safety expected outcomes
change GC-026 / SS-012 accounting
add Runtime / StateCommitter / U04 wiring
claim Gate C PASS
```

---

## 3. Implemented Delta

Compare:

```text
base = 06228a46aa7d1acc663e21672668a1ab2e760546
head = ca6e2b8760187f70704d96910cbbf09e8169f85a
files changed = 1
```

Only changed file:

```text
.github/workflows/u03-gatec-eval.yml
```

No evaluator / case / fixture / clinical-content code changed.

### 3.1 Post-execution provenance binding

After `python3 run_evaluation.py` succeeds, workflow now opens the generated bundle and binds:

```text
execution_kind = GITHUB_ACTIONS_WORKFLOW_DISPATCH
run_id = GITHUB_RUN_ID
run_attempt = GITHUB_RUN_ATTEMPT
repository = GITHUB_REPOSITORY
commit_sha = GITHUB_SHA
ref = GITHUB_REF
ref_name = GITHUB_REF_NAME
event_name = GITHUB_EVENT_NAME
workflow = GITHUB_WORKFLOW
job = GITHUB_JOB
review_confirmation = REVIEW_COMPLETE
gate_c_decision_embedded = false
```

This metadata is execution provenance only; it does not encode Gate C PASS/FAIL.

### 3.2 Stale governance strings externalized

Before evidence becomes durable, workflow removes from `summary`:

```text
governed_evaluation_execution_status
gate_c_status
```

Reason:

```text
Harness executes evaluation semantics.
Governance review/decision records own execution/Gate status.
Static harness strings must not override observed workflow execution facts.
```

No replacement `Gate C PASS` field is introduced.

### 3.3 Evidence verification

Before artifact upload, workflow re-verifies:

```text
approved Golden = 31
executable Golden = 30
excluded Golden = [GC-026]
golden_pass = 30
golden_fail = 0

approved Safety = 20
executable Safety = 19
excluded Safety = [SS-012]
critical_safety_pass = 19
critical_safety_fail = 0
failed_non_case_checks = []

shared_scope_invariant = PASS
P5 defensive boundary = PASS
P5 counts_as_governed_c_execution = false
production_state_mutation_capability = false
network_access_required = false
```

It also verifies provenance is bound to the actual `GITHUB_RUN_ID`, `GITHUB_SHA`, `workflow_dispatch`, and exact `REVIEW_COMPLETE` input.

### 3.4 Durable artifact upload

Workflow now uploads exactly:

```text
tools/u03_gatec_eval/build/u03-gatec-eval/result-bundle.json
```

Artifact identity pattern:

```text
u03-gatec-governed-evidence-<run_id>-attempt-<run_attempt>
```

Upload action is pinned to immutable commit:

```text
actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a
# upstream release tag v7.0.1
```

Upload settings:

```text
if-no-files-found = error
retention-days = 90
overwrite = false
```

The workflow then emits:

```text
artifact_id
artifact_url
artifact_digest
execution_sha
execution_run_id
```

Artifact retention is not treated as the final permanent governance archive. After the governed rerun, the artifact must be fetched, cryptographically identified by the emitted digest, and frozen into the project evidence record before Gate C re-decision.

---

## 4. Why Clinical Semantics Are Unchanged

This remediation occurs strictly after `run_evaluation.py` has completed.

```text
C evaluation
→ D09 evaluation
→ case assertions
→ result-bundle generation
→ [NEW] provenance binding / stale-status removal
→ [NEW] evidence verification
→ [NEW] artifact upload
```

Therefore the new steps cannot alter:

```text
C rule execution results
D09 decision results
fixture inputs
expected outcomes
P0-P5 precedence
30/19 executable accounting
GC-026 / SS-012 exclusion semantics
```

---

## 5. Targeted Evidence-Capture Review Questions

Independent reviewer must answer at minimum:

| ID | Question | Required |
|---|---|---|
| ECR-01 | remediation diff 是否只影响 evidence capture | APPROVE |
| ECR-02 | evaluator/cases/clinical semantics 是否完全未变 | APPROVE |
| ECR-03 | bundle 是否在 upload 前绑定 actual run/SHA/event/ref provenance | APPROVE |
| ECR-04 | stale harness governance strings 是否被移除且未伪造 Gate C PASS | APPROVE |
| ECR-05 | upload artifact 是否包含完整 case-level `result-bundle.json` | APPROVE |
| ECR-06 | artifact action 是否 immutable pin，missing file 是否 fail closed | APPROVE |
| ECR-07 | workflow verify 是否仍锁定 30/30、19/19、排除项与 non-case checks | APPROVE |
| ECR-08 | 是否新增 Runtime / commit / U04 / production capability | APPROVE / NONE |

---

## 6. Current Decision

```text
BF-GATEC-EVIDENCE-01 = OPEN
Evidence Capture Remediation = IMPLEMENTED
Targeted Evidence-Capture Review = NOT_STARTED
Governed Evaluation Re-Execution = NOT_STARTED
Gate C = NOT_PASSED
Merge Authorization = NOT_GRANTED
Runtime / U04 / Production = BLOCKED
```

---

## 7. Next Allowed Sequence

```text
targeted independent evidence-capture review
↓
if PASS: workflow_dispatch with REVIEW_COMPLETE
↓
verify workflow/job success
↓
verify uploaded artifact exists
↓
download artifact and inspect complete result-bundle.json
↓
freeze run id + SHA + artifact id/url/digest + complete case-level evidence identity
↓
close or retain BF-GATEC-EVIDENCE-01 based on evidence
↓
Gate C Re-Decision Review
```

本 remediation implementation record 不是 Independent Review，也不是 Gate C PASS。
