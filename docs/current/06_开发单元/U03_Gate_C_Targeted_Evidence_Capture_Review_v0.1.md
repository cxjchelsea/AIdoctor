# U03 Gate C Targeted Evidence-Capture Review v0.1

> 对象：PR #89 HEAD `3441ad9099fa30ec40c577210f27e07a08a98e3d` 对 `BF-GATEC-EVIDENCE-01` 的 evidence-capture remediation 定向独立审查。  
> 实现 commit：`ca6e2b8760187f70704d96910cbbf09e8169f85a`。  
> 修订说明：`U03_Gate_C_Evidence_Capture_Remediation_v0.1.md`。  
> 前序正式执行：run `35074643060` @ `efd2199` = COMPLETED / SUMMARY_PASS / 无 durable artifact。  
> 审核角色：`U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-16`  
> 状态：`TARGETED_EVIDENCE_CAPTURE_REVIEW_COMPLETE / APPROVE / BLOCKING_FINDING_0 / NOT_REEXECUTED / NOT_GATE_C / NOT_FOR_PRODUCTION`。

```text
Evidence Capture Remediation = IMPLEMENTED
Targeted Evidence-Capture Review = PASS
Governed Evaluation Re-Execution = NOT_STARTED
BF-GATEC-EVIDENCE-01 = OPEN
Gate C = NOT_PASSED
```

本审查没有重新 dispatch workflow，也没有把 artifact / Gate C 标成已完成。

---

## 1. Scope

只审这次 workflow evidence-capture delta，确认：

```text
未改变临床执行语义
artifact 会保存完整 result-bundle.json
provenance 绑定到真实 GitHub run / SHA / event / ref
没有伪造 Gate C 状态
```

不重开：

```text
Independent Implementation Review current = PASS
BF-IR-01 / BF-IR-02
C 15 条 / Coverage / D09 P0–P5 语义
GC-026 / SS-012 排除会计
前序 run 35074643060 的 summary PASS 事实
```

不进入：

```text
Governed Evaluation Re-Execution
Evidence Freeze
Gate C Re-Decision
Runtime / U04 / CD-07 / production / merge
```

审查对象以 GitHub API 上的 PR #89 HEAD `3441ad9` 与实现 commit `ca6e2b8` 为准。本地 `git fetch origin` 因 HTTPS 凭证失败，未改写本地 HEAD；文件内容已按远程 blob 核对。

---

## 2. Mandatory Questions

| ID | Question | Verdict |
|---|---|---|
| ECR-01 | remediation diff 是否只影响 evidence capture | APPROVE |
| ECR-02 | evaluator / cases / clinical semantics 是否完全未变 | APPROVE |
| ECR-03 | bundle 是否在 upload 前绑定 actual run / SHA / event / ref provenance | APPROVE |
| ECR-04 | stale harness governance strings 是否被移除且未伪造 Gate C PASS | APPROVE |
| ECR-05 | upload artifact 是否包含完整 case-level `result-bundle.json` | APPROVE |
| ECR-06 | artifact action 是否 immutable pin，missing file 是否 fail closed | APPROVE |
| ECR-07 | workflow verify 是否仍锁定 30/30、19/19、排除项与 non-case checks | APPROVE |
| ECR-08 | 是否新增 Runtime / commit / U04 / production capability | APPROVE / NONE |

blocking finding = 0

---

## 3. ECR-01 / ECR-02 — 范围与临床语义未变

GitHub compare：

```text
06228a46aa7d1acc663e21672668a1ab2e760546
  → ca6e2b8760187f70704d96910cbbf09e8169f85a
files changed = 1
.github/workflows/u03-gatec-eval.yml
+86 / -1
```

HEAD `3441ad9` 只额外增加 remediation 记录：

```text
docs/current/06_开发单元/U03_Gate_C_Evidence_Capture_Remediation_v0.1.md
```

`tools/u03_gatec_eval` blob SHA 在基线 `06228a46` 与 HEAD `3441ad9` 完全相同：

```text
evaluator.py      = b3dcfa10e27da49a133c7cdc776f80799b401260
cases.py          = a2cd592b502e9d46adbd081c2c23c7487ec5c0fb
run_evaluation.py = 8fc63e9827e84198c20666e40f6b0d48fa411295
test_evaluator.py = fb2ab50181c2c71049977b8acadd643f2af298e1
README.md         = 1d1e345e46da624e6d48cc357d83f0edee801119
```

因此未改 C 阈值 / 谓词、D09 precedence / disposition、fixture、expected outcome、GC-026 / SS-012 accounting，也未改 Runtime wiring。

新步骤都发生在 `python3 run_evaluation.py` 成功之后：

```text
C / D09 / case assertions / result-bundle 生成
→ provenance 绑定与 stale-status 移除
→ evidence verification
→ artifact upload
→ identity 输出
```

```text
ECR-01 = APPROVE
ECR-02 = APPROVE
```

---

## 4. ECR-03 / ECR-04 — provenance 真实，Gate C 未被伪造

评估完成后，workflow 打开已生成的 `result-bundle.json`，写入 GitHub runner 环境变量，而不是静态字符串：

```text
execution_kind        = GITHUB_ACTIONS_WORKFLOW_DISPATCH
run_id                = GITHUB_RUN_ID
run_attempt           = GITHUB_RUN_ATTEMPT
repository            = GITHUB_REPOSITORY
commit_sha            = GITHUB_SHA
ref / ref_name        = GITHUB_REF / GITHUB_REF_NAME
event_name            = GITHUB_EVENT_NAME
workflow / job        = GITHUB_WORKFLOW / GITHUB_JOB
review_confirmation   = REVIEW_CONFIRMATION
gate_c_decision_embedded = false
```

随后 verify 把这些字段回绑到**同一次 job** 的真实环境：

```text
prov.run_id == GITHUB_RUN_ID
prov.commit_sha == GITHUB_SHA
prov.event_name == workflow_dispatch
prov.review_confirmation == REVIEW_COMPLETE
prov.gate_c_decision_embedded is False
```

durable bundle 写入前，从 `summary` 移除：

```text
governed_evaluation_execution_status
gate_c_status
```

并断言这两个键不再存在。没有写入 `Gate C PASS`，也没有把 Gate C 裁决嵌进 bundle。

`gate_c_decision_embedded = false` 明确表示 provenance 不是 Gate C 决定。

```text
ECR-03 = APPROVE
ECR-04 = APPROVE
```

---

## 5. ECR-05 / ECR-06 — 完整 bundle 上传且 fail closed

upload 路径是评估产物本身，不是摘要日志：

```text
tools/u03_gatec_eval/build/u03-gatec-eval/result-bundle.json
```

该文件由 `run_evaluation.py` 写出，包含：

```text
golden_case_results
safety_case_results
excluded_unproducible_cases
shared_scope_invariant_evidence
d09_p5_defensive_contract_boundary
bound_evalset_release_ref
bound_governed_refs
summary
```

post-execution 处理只做两件事，不改 case-level 结果：

```text
summary.pop(stale governance strings)
data += execution_provenance + evidence_capture_status
```

因此 artifact 将保存完整 case-level bundle，外加真实 run provenance。

upload action 钉死到上游 `v7.0.1` 对应 immutable commit：

```text
actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a
tag v7.0.1 object.sha = 043fb46d1a93c77aae656e7c1c64a875d1fc6a0a
```

该 pin 的 `action.yml` 确认输出：

```text
artifact-id
artifact-url
artifact-digest
```

与 workflow 的 identity echo 一致。

fail-closed / 禁止 overwrite：

```text
bundle 缺失 → Path.read_text 失败，job fail，不会 upload
if-no-files-found = error
overwrite = false
artifact name = u03-gatec-governed-evidence-<run_id>-attempt-<run_attempt>
```

同 run 内 `upload-artifact` v7 使用 runner `ACTIONS_RUNTIME_TOKEN`，不依赖把 `permissions.actions` 写成 write。现有 `contents: read` 不构成 blocker。

```text
ECR-05 = APPROVE
ECR-06 = APPROVE
```

---

## 6. ECR-07 / ECR-08 — 会计锁定与隔离保持

upload 前仍锁定：

```text
approved Golden = 31 / executable = 30 / excluded = [GC-026]
golden_pass = 30 / golden_fail = 0
approved Safety = 20 / executable = 19 / excluded = [SS-012]
critical_safety_pass = 19 / critical_safety_fail = 0
failed_non_case_checks = []
gate_c_execution_blocked_by_critical_failure = false
shared_scope_invariant = PASS
P5 defensive boundary = PASS
P5 counts_as_governed_c_execution = false
production_state_mutation_capability = false
network_access_required = false
```

workflow 触发面未扩大：

```text
on = workflow_dispatch only
job if = independent_review_confirmed == REVIEW_COMPLETE
Governance guard 仍做 exact-string test
checkout pin 未改
无 Runtime / StateCommitter / U04 / network / production 接线
```

```text
ECR-07 = APPROVE
ECR-08 = APPROVE / NONE
```

---

## 7. Non-blocking Residuals

这些项不阻止本 targeted review PASS，也不关闭 `BF-GATEC-EVIDENCE-01`。

```text
N-ECR-01
run_evaluation.py 仍写出 stale
governed_evaluation_execution_status / gate_c_status。
workflow 在 durable 化之前剥离它们。
这落在已授权的 “不改 clinical runner” 边界内，可接受。

N-ECR-02
harness 顶层 execution_run_id 仍是 LOCAL-DETERMINISTIC-RUN。
真实 GitHub 身份写在 execution_provenance 与 job echo 中。
不是伪造 Gate C，也不是用本地 run 冒充正式执行。

N-ECR-03
artifact retention-days = 90，不是永久治理档案。
remediation 记录已要求：下次正式 run 后必须下载、按 digest 核验、再冻结进项目证据。

N-ECR-04
verify 继续断言 summary 计数，没有再独立 len(golden_case_results)。
这与前一版 workflow 相同，不是本次 delta 引入的新缺陷。
```

---

## 8. Decision

```text
ECR-01..ECR-08 = APPROVE
blocking evidence-capture finding = 0
Targeted Evidence-Capture Review = PASS

Independent Implementation Review current = PASS
Prior Governed Evaluation Execution = COMPLETED / SUMMARY_PASS
Evidence Capture Remediation = IMPLEMENTED / REVIEWED

BF-GATEC-EVIDENCE-01 = OPEN
Governed Evaluation Re-Execution = NOT_STARTED
Gate C = NOT_PASSED
Gate C Decision = EVIDENCE_REMEDIATION_REQUIRED
Merge Authorization = NOT_GRANTED
Runtime / U04 / Production = BLOCKED
```

本审查允许下一步单独启动：

```text
workflow_dispatch
  ref = impl/u03-gatec-eval-only
  independent_review_confirmed = REVIEW_COMPLETE
```

下一步仍必须：

```text
确认 job success
确认 artifact 已上传
下载完整 result-bundle.json
核验 execution_provenance 与 artifact_digest
冻结 run id / SHA / artifact id / url / digest / case-level evidence
再做 Gate C Re-Decision Review
```

```text
Targeted Evidence-Capture Review PASS
  != Governed Evaluation Re-Execution
  != Evidence Freeze
  != Gate C PASS
```
