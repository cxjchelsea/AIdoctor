# U03 CD-07 Runtime Release Binding Policy v0.1

> 对象：CD-07 non-production runtime 对 Gate-C-frozen governed releases 的解析、绑定与激活边界。  
> 状态：`RDP-03_FROZEN / EXPLICIT_NON_PRODUCTION_BINDING_ONLY / NOT_PUBLICATION / NOT_ACTIVATION / NOT_IMPLEMENTATION_AUTHORIZATION`。

## 1. Frozen governed set

CD-07 第一阶段只允许引用当前已经完成 Gate C 的 exact set：

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
```

如需 Gate C regression，可同时引用：

```text
ER-U03-RISK-001@0.1.0-candidate
```

## 2. Binding mode

CD-07 readiness 冻结的唯一允许模式：

```text
EXPLICIT_NON_PRODUCTION_BINDING_ONLY
```

含义：

```text
- exact ref 必须由受控配置/调用上下文明确指定；
- runtime 不得按 latest/current/newest 自动选择临床 release；
- 只允许用于隔离、受控、非生产实现和验证环境；
- 绑定不改变 release 生命周期状态；
- 绑定不代表 publication；
- 绑定不代表 ACTIVE_FOR_RUNTIME_PRODUCTION；
- 绑定不代表 Production Authorization。
```

## 3. Required binding invariants

一次 U03 runtime execution 的 governed refs 必须构成同一冻结 binding chain。实现必须验证：

```text
B1 all required refs present
B2 refs are exact identities, not aliases such as latest
B3 each ref is resolvable through the governed release registry/boundary
B4 rule/coverage/policy/knowledge refs form the approved compatible set
B5 policy_pair_ref matches the frozen pair
B6 refs are bound to the exact execution / trace provenance
B7 Clinical State Version and release set used for decision cannot silently change mid-run
```

## 4. Fail-closed matrix

以下任一情况必须阻断 clinical decision/commit path：

```text
missing required ref
unresolvable ref
release identity mismatch
cross-release incompatibility
policy-pair mismatch
unexpected release substitution
runtime attempt to use an unapproved alias/default
release binding changed after execution started
```

Failure must be represented as release/governance failure, not `NO_MATCH`, low risk, safe, normal, or other clinical conclusion.

## 5. Candidate lifecycle boundary

Current set remains:

```text
CANDIDATE / FROZEN / EVALUATED
NOT_PUBLISHED
NOT_ACTIVE_FOR_PRODUCTION
```

The controlled non-production binding exception exists solely to permit implementation/verification against the exact Gate-C-passed package.

```text
EXPLICIT_NON_PRODUCTION_BINDING
!= publication
!= general runtime activation
!= production activation
```

## 6. Publication / activation ownership

CD-07 implementation must not create an implicit publication or activation mechanism.

Any future production use requires a separate governance decision that defines at minimum:

```text
- release publication authority
- activation authority
- target environment/scope
- effective version pair/set
- rollback/deactivation behavior
- audit evidence
- production authorization linkage
```

Those concerns are outside current CD-07 readiness scope.

## 7. Mutation and cache safety

Implementations may cache resolved non-clinical metadata only if cache behavior cannot cause silent release substitution. A cached resolved release must remain keyed by exact immutable ref; cache miss/failure must not select a different release.

## 8. Gate-C relationship

Gate C proves the exact frozen set was evaluated under its governed evaluation package. Gate C does not authorize arbitrary new release combinations.

If any clinical release/content identity changes, current Gate C evidence cannot simply be inherited; the appropriate governance/evaluation path must be reassessed.

## 9. Verdict

```text
RDP-03 = FROZEN / PASS_FOR_READINESS_REVIEW
Runtime binding mode = EXPLICIT_NON_PRODUCTION_BINDING_ONLY
Release publication = NOT_AUTHORIZED
Production activation = NOT_AUTHORIZED
CD-07 Implementation Authorization = NOT_GRANTED
```
