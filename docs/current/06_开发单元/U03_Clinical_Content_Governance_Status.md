# U03 Clinical Content Governance Status

> 角色：U03 clinical content / gate 状态治理索引。  
> 本文件只声明文档成熟度与权威边界，不包含临床规则、阈值或医学决策。  
> 当前状态已同步至 Gate C PASS 后事实；不因此授权 CD-07、U04、runtime、merge 或 production。

## 1. 总体原则

`docs/current` 中的结构/治理资料可作为当前项目参考，但 Draft / Review Material 不会因此自动升级为生产 Clinical Truth。

```text
STRUCTURAL / GOVERNANCE AUTHORITY
!=
CLINICAL CONTENT APPROVAL
!=
RUNTIME AUTHORIZATION
!=
PRODUCTION AUTHORIZATION
```

## 2. 当前 Governed Content Set

```text
A/B source-locked semantics = FROZEN / Gate A PASS

E = KR-U03-SOURCE-001@0.1.0-candidate
  / CANDIDATE_FROZEN / RESOLVABLE / REVIEWED / NOT_PUBLISHED

C = RR-U03-RISK-001@0.2.1-candidate
  / CANDIDATE_FROZEN / CD-03_APPROVED_FOR_GATE_B

Coverage = U03_D09_COVERAGE_V0_2_1_CANDIDATE
  / CANDIDATE_FROZEN / RESOLVABLE

D = PR-U03-D09-001@0.2.1-candidate
  / CANDIDATE_FROZEN / CD-05_APPROVED_FOR_GATE_B

C/D/E Cross-Consistency v0.2.1 = PASS
BF-CDE-01 = CLOSED
```

Current binding chain:

```text
KR-U03-SOURCE-001@0.1.0-candidate
→ RR-U03-RISK-001@0.2.1-candidate
→ U03_D09_COVERAGE_V0_2_1_CANDIDATE
→ PR-U03-D09-001@0.2.1-candidate
```

历史 `0.2.0` C/D/Coverage candidates 继续冻结并保持 immutable，不属于当前 Gate B set。

## 3. 当前 Gate 状态

```text
Gate A = PASS
Gate B = PASS / GOVERNED_CONTENT_READY
Gate C = PASS

CD-03 = APPROVED_FOR_GATE_B
CD-04 = INITIAL_RELEASE_GOVERNANCE_READY
CD-05 = APPROVED_FOR_GATE_B
CD-06 = COMPLETE_FOR_CURRENT_GATE_C_PACKAGE

F Clinical Eval Content = APPROVED_FOR_EVALUATION
Governed Evaluation Re-Execution = COMPLETED / PASS
Governed Evaluation Evidence = FROZEN / VERIFIED

BF-CD06-01 = CLOSED
BF-CD06-02 = CLOSED
BF-CD06-EXEC-01 = CLOSED
BF-GATEC-EVIDENCE-01 = CLOSED
```

Gate C 冻结证据：

```text
run_id = 35077669669
executed_sha = 66a10209b9e98d49d49eae1f472d15110bddc4df
artifact_id = 10439131250
artifact_digest = sha256:c0649153d40610e685bc80940c25b75a2616698e439e913d6da5d0ebaf56e4f3
Golden executable = 30 / 30 PASS
Safety executable = 19 / 19 PASS
excluded = GC-026, SS-012 / UNPRODUCIBLE_UNDER_SHARED_SCOPE
failed_golden = []
failed_critical_safety = []
failed_non_case_checks = []
```

Gate C PASS 只说明当前 frozen evaluation package 已完成治理评估并通过。

它不表示：

```text
current candidates = PUBLISHED
current candidates = ACTIVE_FOR_RUNTIME
current candidates = ACTIVE_FOR_PRODUCTION
CD-07 Implementation Authorization
U04 Implementation Authorization
Runtime Active
Production Authorized
Merge Authorized
```

## 4. Post-Gate-C 当前边界

```text
CD-07 Implementation Readiness = NOT_READY
CD-07 Implementation Authorization = NOT_GRANTED
Runtime Implementation Authorization = NOT_GRANTED

U04 Implementation Readiness = NOT_READY
U04 blocker = BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
U04 Implementation Authorization = NOT_GRANTED

PR #89 Merge Authorization Review = IN_PROGRESS / SEPARATE_GOVERNANCE
Merge Authorization = NOT_GRANTED

Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

当前 governed set 继续保持：

```text
candidate / frozen / evaluated
!= published
!= active for runtime
!= active for production
```

## 5. 当前剩余 runtime clinical dependency

Gate C 已闭环，但 runtime clinical dependency 仍缺：

```text
Runtime Implementation Authorization
C02 runtime wiring
D09 runtime wiring
production Clinical State commit authorization/path
candidate publication / activation
real patient traffic / production data plane
U04 / U14 routing
external clinical API / business wiring
U03 runtime E2E
pediatrics / pregnancy-puerperium / China production localization
```

因此 U03 clinical dependency 不能被描述为 runtime complete。

## 6. 当前允许的下一步

必须分开开单：

```text
A. CD-07 Implementation Readiness / Authorization
B. U03→U04 input contract + U04 readiness
C. PR #89 Merge Authorization Review
D. release publication / activation governance（未来如需）
```

任何一项都不能从 Gate C PASS 自动继承授权。

## 7. 当前禁止事项

- 不把 current frozen candidates 当作 PUBLISHED / ACTIVE_FOR_RUNTIME / ACTIVE_FOR_PRODUCTION；
- 不把 Gate C PASS 解释为 CD-07 Implementation Authorization；
- 不把 Gate C PASS 解释为 U04 Implementation Authorization；
- 不开始未经授权的 C02/D09 runtime；
- 不打开真实患者流量或 production Clinical State mutation；
- 不打开中国生产本地化；
- 不打开儿科或孕产 source pack；
- 不把 NICE/NHS 直接视为中国最终生产规则；
- 不把 Gate C PASS 解释为 Merge Authorization；
- 不宣称 Clinical Runtime Production Enabled；
- 不宣称 Production Authorization。
