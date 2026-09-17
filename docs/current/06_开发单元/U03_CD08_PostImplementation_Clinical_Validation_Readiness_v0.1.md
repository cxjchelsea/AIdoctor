# U03 CD-08 Post-Implementation Clinical Validation Readiness v0.1

> 阶段：U03 / CD-08 Post-Implementation Clinical Validation  
> 目的：在 CD-07 真实 non-production runtime 实现完成后，判断是否具备对“真实 runtime 行为”开展受控临床验证的条件。  
> 本文件只做 readiness 判定，不构成 CD-08 执行授权、U04 授权、production 授权或真实患者流量授权。

## 1. CD-08 在 U03 治理链中的位置

U03 clinical dependency 链按既定编号解释为：

```text
CD-01  Clinical Risk Semantics
CD-02  Evidence Catalog
CD-03  Safety-critical Risk Rule Pack
CD-04  Knowledge Release
CD-05  D09 Clinical Policy
CD-06  EvalSet / Safety Suite
Gate A / B / C
Gate D = CD-07 前的 Implementation Readiness / Authorization
CD-07 真实 C02 / D09 non-production runtime implementation
CD-08 post-implementation clinical validation
```

因此：

```text
Gate C PASS
!= CD-08 PASS

CD-07 runtime verification PASS
!= CD-08 clinical validation PASS

CD-08 PASS
!= production authorization
```

CD-08 的验证对象不是 evaluation-only harness，也不是单纯代码接线，而是已经完成 CD-07 后的真实受控 runtime 链。

## 2. 当前可作为 CD-08 输入的已冻结事实

### 2.1 Gate C governed clinical package

```text
Gate A = PASS
Gate B = PASS / GOVERNED_CONTENT_READY
Gate C = PASS
Governed Evaluation Evidence = FROZEN / VERIFIED

Golden approved identities = 31
Golden executable = 30 / 30 PASS
Critical Safety approved identities = 20
Critical Safety executable = 19 / 19 PASS
Excluded = GC-026, SS-012 / UNPRODUCIBLE_UNDER_SHARED_SCOPE
```

Gate-C formal execution identity：

```text
run_id = 35077669669
executed_sha = 66a10209b9e98d49d49eae1f472d15110bddc4df
artifact_id = 10439131250
artifact_digest = sha256:c0649153d40610e685bc80940c25b75a2616698e439e913d6da5d0ebaf56e4f3
```

### 2.2 Exact governed release set

CD-08 必须使用与 CD-07 / Gate-C 冻结一致的 exact refs：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
PF-U03-C-POLICY-001
```

禁止 `latest/current/newest` alias、静默替换或跨版本混用。

### 2.3 CD-07 implementation identity

```text
reviewed implementation HEAD = d14bf447e252fe6abd9f5fe8ad7604a259e04c03
verification run = 35187288619
artifact = 10482628227
merge commit = 22622a86c5d2dfcfca5bdc379e5379e171ac9aab
PMV = PASS
```

CD-07 已证明：

```text
S1-S14 = VERIFIED within authorized non-production scope
NON_PRODUCTION_RUNTIME_E2E = PASS
V1-V8 = PASS
N1-N13 = PASS
```

其 verification plan 已明确声明：

```text
runtime E2E PASS != clinical evaluation PASS
```

CD-08 正是该缺失的 post-implementation clinical validation 层。

## 3. CD-08 验证对象

CD-08 必须驱动真实 CD-07 runtime path，而不是复用 Gate-C evaluator 直接给出结果：

```text
Frozen clinical case / fixture
        ↓
exact Clinical State Version + Run/Thread/Event context
        ↓
Foundation-1 CapabilityInvocationGuard
        ↓
C02 evidence-aware runtime execution
        ↓
CD-07 evidence acceptance
        ↓
D09 governed owner execution
        ↓
K09 typed proposal
        ↓
P01 admission
        ↓
StateCommitter
        ↓
P05 trace / post-commit finalization
        ↓
S14 U03 outbound producer
        ↓
CD-08 clinical validation comparison
```

CD-08 不执行 U04 owner、不生成 U04 safety truth、不激活 U03→U04 routing。

## 4. Clinical Truth authority boundary

CD-08 不允许开发者、测试代码或模型自行创造新的临床预期。

允许作为临床判定 authority 的内容仅包括：

```text
- 已完成 Medical Owner Review 的 A/B 内容
- 已冻结的 C/D/E governed candidate set
- CD-06 / Gate-C 已冻结并批准的 Golden / Safety case expected semantics
- 已冻结 shared-scope / missingness / coverage / policy-pair decisions
```

如真实 runtime 暴露出此前 fixture 无法表达的新医学问题：

```text
DO NOT infer a new expected clinical answer
→ record as CLINICAL_EXPECTATION_GAP
→ return to Medical Owner / governed content review
```

## 5. CD-08 minimum validation dimensions

### CV-01 Clinical semantic preservation

真实 runtime 的临床结果必须与同一 frozen case 的 governed expected semantics 一致。

重点比较：

```text
risk/disposition semantics
matched / no-match distinction
insufficient-input semantics
scope semantics
failure vs clinical-negative distinction
```

### CV-02 Evidence/provenance preservation

必须证明临床结论使用的 evidence/provenance 与冻结 clinical package 的约束一致，不能出现：

```text
missing evidence treated as negative
unsupported evidence silently accepted
wrong source provenance
cross-state-version evidence mixing
```

### CV-03 Safety-critical behavior

所有可执行 Critical Safety case 必须通过真实 runtime path。

任何 critical safety mismatch 均为 blocking failure；不得通过平均分、总体准确率或多数通过掩盖。

### CV-04 Missingness / unknown semantics

必须保持既定不变量，包括但不限于：

```text
UNKNOWN != NO
UNMEASURED != NORMAL
FAILED != NO_MATCH
INPUT_INSUFFICIENT != LOW_RISK
```

### CV-05 Release-bound behavior

必须证明 clinical result 来自 exact frozen release set，并且 release/provenance mismatch 会 fail closed，而不是生成临床结果。

### CV-06 Canonical-state outcome correctness

验证对象必须覆盖真实 committed Clinical State outcome，而不只比较 C02/D09 中间返回值。

```text
Capability Result != Clinical Truth
Decision != Proposal != Commit
Trace != Clinical State
```

最终 clinical validation 必须明确指出 comparison target 是：

```text
committed governed U03 clinical state / governed disposition semantics
```

而不是 free-form explanation 或 trace 文本。

### CV-07 Outbound semantic integrity

S14 handoff 只能携带 U03 已建立的 typed/provenance-bound 信息；不得出现：

```text
safe=true
normal=true
u04_passed=true
continue_without_safety_gate=true
```

除非未来 U04 独立治理正式定义。

### CV-08 Failure / reconciliation behavior

以下技术失败不得被临床解释为低风险或安全：

```text
C02 failure
D09 failure
release resolution failure
stale Clinical State Version
commit conflict
trace persistence failure
RECONCILIATION_REQUIRED
```

## 6. Required test population

第一轮 CD-08 不新增医学 case，优先复用 exact frozen Gate-C population：

```text
Golden executable = 30
Critical Safety executable = 19
```

`GC-026` 与 `SS-012` 保持原 governed exclusion，不得为了追求 100% executable coverage 自行补造临床输入。

如 execution adapter 需要将 frozen case 映射为 runtime input，可以新增技术 mapping fixture，但 mapping 不得改变 case 的医学语义、expected outcome 或 scope。

## 7. Required negative / integrity controls

除临床正向 case 外，CD-08 至少继续证明：

```text
C8-N1 wrong release ref cannot produce accepted clinical result
C8-N2 stale Clinical State Version cannot produce accepted clinical result
C8-N3 malformed / missing evidence cannot collapse to low risk
C8-N4 dependency failure cannot collapse to clinical negative
C8-N5 unauthorized U04 execution remains blocked
C8-N6 production environment / real-patient mode remains blocked
C8-N7 post-commit trace failure is reconciliation state, not clinical reinterpretation
C8-N8 free-form output cannot substitute for committed typed clinical state
```

这些属于 integrity controls，不是新增医学内容。

## 8. Pass / fail rule

CD-08 PASS 至少要求：

```text
1. exact CD-07 implementation identity frozen
2. exact Gate-C governed package identity frozen
3. exact release refs frozen
4. all executable Golden cases pass through REAL_CD07_RUNTIME_PATH
5. all executable Critical Safety cases pass through REAL_CD07_RUNTIME_PATH
6. no blocking clinical semantic mismatch
7. all mandatory integrity controls pass
8. no unauthorized U04 execution/routing
9. no production state mutation / real-patient traffic
10. durable evidence package frozen
11. independent clinical/governance review = PASS
```

任何 Critical Safety mismatch：

```text
CD-08 = FAIL / BLOCKED
```

不得通过调整阈值、修改 expected answer、删除 case 或降低验收标准自行关闭。

## 9. Required evidence package

建议冻结至少：

```text
C8-E1 exact CD-07 implementation/merge SHA
C8-E2 exact CD-08 harness/mapping SHA
C8-E3 exact Gate-C fixture/package identity
C8-E4 exact governed release refs
C8-E5 runtime environment identity
C8-E6 case-by-case Golden result
C8-E7 case-by-case Critical Safety result
C8-E8 committed-state comparison evidence
C8-E9 evidence/provenance/release correlation
C8-E10 integrity-control results
C8-E11 exclusions and residual risks
C8-E12 explicit no-U04/no-production/no-real-patient attestation + machine-verifiable controls where possible
```

## 10. Readiness assessment

当前 prerequisite 核查：

```text
Gate A = PASS
Gate B = PASS
Gate C = PASS
Governed clinical package = FROZEN / VERIFIED
CD-07 Implementation Authorization = GRANTED / NON_PRODUCTION_ONLY
CD-07 implementation = IMPLEMENTED / VERIFIED / INDEPENDENTLY_REVIEWED / MERGED / PMV_PASS
Real non-production runtime path = AVAILABLE
Exact governed release set = AVAILABLE
Frozen Golden/Safety population = AVAILABLE
Production access = NOT_REQUIRED
Real patient traffic = NOT_REQUIRED
U04 execution = NOT_REQUIRED / NOT_AUTHORIZED
```

因此：

```text
CD-08 Validation Readiness = PASS / READY_FOR_EXECUTION_AUTHORIZATION_REVIEW
CD-08 Execution Authorization = NOT_GRANTED_BY_THIS_DOCUMENT
CD-08 Execution = NOT_STARTED
CD-08 Clinical Validation = NOT_PASSED
U03 Clinical Dependency Closure = NOT_COMPLETE
U04 Readiness Re-review = BLOCKED_PENDING_CD08
U04 Implementation Authorization = NOT_GRANTED
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

## 11. Required next sequence

```text
CD-08 Readiness PASS
→ independent CD-08 Execution Authorization Review
→ explicit CD-08 non-production clinical-validation execution authorization
→ implement only the technical runtime-validation harness/mapping if needed
→ execute frozen cases through REAL_CD07_RUNTIME_PATH
→ freeze durable evidence
→ independent clinical/governance review
→ CD-08 PASS / FAIL decision
→ if PASS: U03 Clinical Dependency Closure Review
→ only after U03 closure: U04 Readiness Re-review
```

禁止跳过 CD-08 直接把 CD-07 runtime verification 当作 U03 clinical closure。
