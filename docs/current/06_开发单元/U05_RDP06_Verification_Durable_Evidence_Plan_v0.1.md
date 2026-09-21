# U05 RDP-06 Verification / Durable Evidence Plan v0.1

> Scope: U05 当前 non-production implementation slice 的验证矩阵、expected-result authority、durable evidence schema、CI/exact-head binding、replay/crash/conflict evidence、regression gates 与 independent evidence acceptance criteria。  
> Design basis: U05-RDP-01 / 02 / 03 / 04 / 05 current frozen/refrozen semantics.  
> Immediate upstream design head: U05-RDP-04 status/provenance head `c800f8d8645d416c4a87b0d63d4fa02ec6ce8a97`.  
> Status: **PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW**.  
> Target blocker: `BF-U05-RG-06`.  
> 本文件只定义“未来如何证明 U05 实现正确”，不等于实现已存在，不授予 implementation / merge / production / live downstream / real-patient authorization。

---

# 1. Purpose

U05 readiness package 的前五项合同已经覆盖：

    RDP-01
    Consumer Inbound Contract

    RDP-02
    D03 Policy / Owner Decision Contract

    RDP-03
    State Ownership / K09-P01 Mutation / Trace Contract

    RDP-04
    Downstream Routing / Side-effect Boundary

    RDP-05
    Readiness Input Dependency / Applicability Contract

RDP-06 只回答：

> **未来 U05 代码实现完成后，必须拿出什么可执行验证与 durable evidence，才能证明“实现忠实于 Frozen Contract”，而不是只证明测试代码自己同意自己？**

因此 RDP-06 必须冻结：

    verification authority
    scenario matrix
    expected-result derivation
    POLICY_EXPECTATION_GAP stop rule
    structured observed evidence
    exact-head binding
    CI/static/runtime gates
    replay/crash/conflict verification
    P05 correlation
    non-production hard boundaries
    regression suite
    artifact integrity / retention
    independent evidence review
    implementation closure criteria

---

# 2. Verification principle

必须保持：

    test passed
    != implementation correct

    implementation correct
    != clinically evaluated

    clinically evaluated
    != production authorized

验证目标是：

    Frozen Contract
    -> executable test expectation
    -> observed runtime/governance evidence
    -> expected-vs-observed comparison
    -> independently reviewable durable artifact

禁止：

    implementation behavior
    -> reverse-engineer expected answer
    -> test passes itself

---

# 3. Expected-result authority hierarchy

每一个 U05 verification case 必须绑定：

    expected_authority_refs[]

Expected result 的 authority 优先级：

    1. U05-RDP-02
       D03 status / six readiness values / precedence / POL-005 / POL-011

    2. U05-RDP-01
       admission / rejection / replay / currentness

    3. U05-RDP-03
       readiness effect / K09/P01 / invalidation / replay / trace

    4. U05-RDP-04
       downstream routing / permission / side-effect boundary

    5. U05-RDP-05
       applicability / readiness input currentness

    6. current frozen Phase 5 / 6 / 8 / 9
       only when the RDP contracts explicitly depend on those frozen semantics

测试代码不得自行创造业务 expectation。

---

# 4. POLICY_EXPECTATION_GAP stop rule

如果一个 intended verification profile：

    is legally admitted/current
    + all required input applicability is lawful
    + Frozen Contract cannot uniquely derive the expected D03 / route / commit behavior

则必须产生：

    POLICY_EXPECTATION_GAP

并立即：

    verification verdict = FAIL_CLOSED
    implementation readiness closure = STOP
    do not invent expected result in test code
    do not map gap to INPUT_FAILURE
    do not map gap to NO_RELIABLE_DIRECTION
    do not silently exclude the case

POLICY_EXPECTATION_GAP：

    != runtime D03 status
    != seventh Clinical Readiness value
    != test skip reason

它是：

    design/readiness sentinel

Future implementation verification harness 必须支持：

    policy_expectation_gap_count

并要求正常 acceptance run：

    policy_expectation_gap_count = 0

---

# 5. Verification layers

RDP-06 冻结六层验证。

## L1 Contract / schema

验证：

    enums
    required fields
    conditional fields
    source refs
    version bindings
    typed status vocabularies
    illegal value rejection

## L2 Deterministic unit behavior

验证：

    RDP-01 admission
    D03 precedence
    RDP-03 effect/proposal semantics
    RDP-04 mapping

## L3 State / ledger integration

验证：

    K09/P01 commit
    version conflict
    invalidation
    effect ledger
    route ledger
    idempotency

## L4 Runtime orchestration boundary

验证：

    admission -> D03 -> commit -> route eligibility
    no direct downstream execution
    crash/replay/reconcile
    Scheduler intent boundary

## L5 Structural authorization guards

验证：

    no production activation
    no live downstream target invocation
    no external delivery/tool/model side effect
    no real-patient input source
    no unauthorized Spring/runtime auto-activation
    no bypass of K09/P01 or current Safety

## L6 Regression

验证：

    Foundation
    U01
    U02
    U03
    U04

以及 U05-focused suites 全部通过。

---

# 6. Verification authorization profile

Future exact-head verification run 必须冻结一个 machine-readable：

    U05_VERIFICATION_AUTH_PROFILE

至少表达：

    environment = non-production

    u05_live_external_entry = false
    production_routing = false
    production_mutation = false
    real_patient_traffic = false

    live_u06_execution = false
    live_u08_execution = false
    live_u10_execution = false
    live_u11_execution = false

    external_delivery_side_effects = false

    unauthorized_model_tool_calls = false

允许在 test/in-memory/non-production harness 中真实验证：

    U05 admission
    deterministic D03
    K09/P01-compatible readiness commit semantics
    Runtime/Effect-Ledger route evidence
    typed downstream eligibility

但：

    eligibility
    != live downstream Unit activation

任何 auth-profile hard boundary = true：

    verification verdict = FAIL

---

# 7. Test data boundary

U05 RDP-06 不要求真实患者数据。

Verification fixtures 必须：

    synthetic
    non-identifiable
    contract-driven

测试输入优先使用抽象 readiness semantics：

    F1 applicability/business signal
    F3 applicability/business signal
    F5 applicability/business signal
    F6 applicability/business signal
    Gate/restriction
    version/provenance/currentness

而不是自行发明新的医学病例真值。

禁止：

    real patient PHI in retained evidence
    developer-created new medical policy hidden inside fixture
    expected diagnosis as D03 truth

---

# 8. Canonical verification case identity

每条 case 定义：

    U05_VERIFICATION_CASE_ID

格式建议：

    U05-EV-001 ...

Case identity 一旦进入 frozen RDP-06：

    semantic scenario must remain stable

允许未来新增：

    U05-EV-0xx

但不得复用旧 case_id 表示不同语义。

---

# 9. Required case matrix — RDP-01 admission

## Admission cases

    U05-EV-001
    A1_POST_BARRIER_CURRENT + ALLOW + current F3
    -> ADMITTED

    U05-EV-002
    current active A1 binding + direct POST_SAFETY_INITIAL
    -> REJECTED

    U05-EV-003
    Gate BLOCKED
    -> REJECTED / no D03

    U05-EV-004
    Gate UNAVAILABLE
    -> REJECTED / no D03

    U05-EV-005
    RESTRICTED + U05 evaluation permitted + matching context
    -> ADMITTED

    U05-EV-006
    RESTRICTED + missing/mismatched context
    -> REJECTED

    U05-EV-007
    stale U04 Gate / inbound route
    -> REJECTED

    U05-EV-008
    uncommitted U04 Gate
    -> REJECTED

    U05-EV-009
    continuation route consequence != TO_U05_CLINICAL_READINESS
    -> REJECTED

    U05-EV-010
    consultation/CDP/state-version mismatch or malformed request
    -> REJECTED

    U05-EV-011
    required owner recomputation still pending
    -> REJECTED / PENDING_OWNER_RECOMPUTATION

    U05-EV-012
    lawful route + required RDP-05 input FAILED
    -> ADMITTED
    -> D03 INPUT_FAILURE
    -> proves admission failure != readiness-input failure

    U05-EV-013
    exact admission replay
    -> same admission_id / REATTACHED

    U05-EV-014
    same admission identity + changed canonical payload
    -> REPLAY_CONFLICT

    U05-EV-015
    same Clinical State Version but authoritative input-set identity changed
    -> old request not current / re-admission required

---

# 10. Required case matrix — RDP-02 / D03

## D03 decision cases

    U05-EV-016
    OUT_OF_SCOPE profile
    -> DECIDED / OUT_OF_SCOPE

    U05-EV-017
    blocking offline evidence present
    + lower-priority signals present
    -> DECIDED / NEEDS_OFFLINE_EVIDENCE

    U05-EV-018
    clarification required
    + no higher priority signal
    -> DECIDED / NEEDS_CLARIFICATION

    U05-EV-019
    current actionable F3 gap
    + no higher priority signal
    -> DECIDED / CAN_ASK_MORE

    U05-EV-020
    POL-005 first-entry positive profile
    -> DECIDED / READY_FOR_CLINICAL_ANALYSIS

    U05-EV-021
    POL-011 first-entry-after-current-F6-NOT_NEEDED profile
    -> DECIDED / READY_FOR_CLINICAL_ANALYSIS

    U05-EV-022
    qualified no reliable direction
    + no P2-P6 result
    -> DECIDED / NO_RELIABLE_DIRECTION

    U05-EV-023
    required FAILED/UNAVAILABLE input
    with lawful admission
    -> INPUT_FAILURE / no readiness

    U05-EV-024
    mutually inconsistent authoritative required inputs
    -> INPUT_CONFLICT / no readiness

    U05-EV-025
    profile with multiple lower-priority candidate signals
    -> exactly one readiness according to frozen precedence

    U05-EV-026
    missing artifact cannot infer NOT_YET_APPLICABLE / NOT_NEEDED / READY
    -> fail according to frozen applicability/admission semantics

    U05-EV-027
    POST_DDX or prior-F5-activated profile
    -> POL-011 must not apply

    U05-EV-028
    synthetic meta-case deliberately lacking frozen expectation authority
    -> POLICY_EXPECTATION_GAP detector fires
    -> harness verdict FAIL_CLOSED

U05-EV-028 不是正常 runtime acceptance case。

它验证：

    the verification system itself
    refuses to invent expected business truth

Acceptance run 可以单独以“sentinel self-test”方式证明：

    detector catches injected gap

但正常 governed scenario matrix 必须：

    policy_expectation_gap_count = 0

---

# 11. Required case matrix — RDP-03 mutation / trace

    U05-EV-029
    DECIDED readiness
    -> one readiness effect
    -> one K09 Proposal
    -> COMMITTED

    U05-EV-030
    INPUT_FAILURE
    -> no effect / proposal / commit

    U05-EV-031
    INPUT_CONFLICT
    -> no effect / proposal / commit

    U05-EV-032
    exact readiness effect replay after successful commit
    -> REATTACH/authoritative NO_OP
    -> no second state version only for replay

    U05-EV-033
    same readiness enum + different admitted basis
    -> different effect
    -> new provenance / commit

    U05-EV-034
    state advances before first readiness commit
    -> CONFLICT
    -> no blind baseVersion rewrite

    U05-EV-035
    Proposal attempts unauthorized field mutation
    -> REJECTED
    -> no state effect

    U05-EV-036
    RESTRICTED source context
    -> context/permission evidence preserved through readiness commit

    U05-EV-037
    READINESS_ONLY_COMMIT
    -> version advances
    -> does not automatically invalidate Gate/dependencies

    U05-EV-038
    state-changing upstream dependency change
    -> CLINICAL_READINESS_INVALIDATION_EFFECT
    -> STALE atomically/authoritatively

    U05-EV-039
    non-state authoritative revalidation invalidates accepted input-set identity
    -> old readiness fail-closed
    -> governed invalidation commit before ordinary reuse

    U05-EV-040
    same readiness effect id + different canonical fingerprint
    -> hard replay conflict

    U05-EV-041
    crash after D03 / before Proposal
    -> same effect/proposal identity recovered

    U05-EV-042
    crash after commit / before checkpoint
    -> prior commit reattached
    -> no duplicate readiness effect

    U05-EV-043
    two concurrent writers on same base
    -> at most one distinct effect commit
    -> other conflicts or exact-effect reattach

---

# 12. Required case matrix — RDP-04 routing / side-effect boundary

## Six-value mapping

    U05-EV-044
    NEEDS_CLARIFICATION
    -> ELIGIBLE / TO_U06_QUESTION_PATH

    U05-EV-045
    CAN_ASK_MORE
    -> ELIGIBLE / TO_U06_QUESTION_PATH

    U05-EV-046
    READY_FOR_CLINICAL_ANALYSIS
    -> ELIGIBLE / TO_U08_CLINICAL_ANALYSIS

    U05-EV-047
    NEEDS_OFFLINE_EVIDENCE
    -> ELIGIBLE / TO_U10_OFFLINE_EVIDENCE

    U05-EV-048
    OUT_OF_SCOPE
    -> ELIGIBLE / TO_U11_SAFE_EXIT

    U05-EV-049
    NO_RELIABLE_DIRECTION
    -> ELIGIBLE / TO_U11_SAFE_EXIT

## Permission / failure / replay

    U05-EV-050
    RESTRICTED permits U05 evaluation but denies candidate U08 action
    -> PREEMPTED
    -> no U08 eligibility

    U05-EV-051
    downstream action permission resolution UNAVAILABLE
    -> FAILURE_REQUIRED
    -> no ordinary eligibility

    U05-EV-052
    Safety becomes BLOCKED after readiness commit
    -> PREEMPTED
    -> no ordinary eligibility

    U05-EV-053
    Safety UNAVAILABLE
    -> FAILURE_REQUIRED
    -> no ordinary eligibility
    -> not a U14 final decision

    U05-EV-054
    exact eligible route replay
    -> same route authorization / eligibility
    -> replay_disposition REATTACHED

    U05-EV-055
    same route identity + different canonical fingerprint
    -> route replay conflict

    U05-EV-056
    eligibility becomes stale before Scheduler consume
    -> no target intent

    U05-EV-057
    target execution binding unavailable
    -> no target invocation
    -> failure-governance handoff
    -> no silent alternate clinical route

    U05-EV-058
    same eligibility replay
    -> at most one authoritative Scheduler target intent

    U05-EV-059
    PREEMPTED / FAILURE_REQUIRED / REJECTED_STALE
    -> no ordinary route effect
    -> no ordinary route lifecycle

    U05-EV-060
    U05 route creation/consumption
    -> no Question delivery
    -> no DDx effect
    -> no F6 effect
    -> no Safe Exit delivery
    -> no external side effect

---

# 13. Required case matrix — structural / regression gates

    U05-EV-061
    no direct U05 runtime dependency that invokes live U06/U08/U10/U11 owner execution
    -> structural guard PASS

    U05-EV-062
    no automatic production/Spring activation outside explicitly authorized profile
    -> structural guard PASS

    U05-EV-063
    no unauthorized model/tool/external-service call from U05 deterministic D03/routing slice
    -> structural guard PASS

    U05-EV-064
    no direct Clinical State write bypassing K09/P01
    -> structural guard PASS

    U05-EV-065
    synthetic/non-PHI evidence fixture guard
    -> PASS

    U05-EV-066
    Foundation + U01-U04 regression
    -> PASS within accepted existing authorization-gated skips only

---

# 14. Scenario matrix completeness rule

以上：

    U05-EV-001 .. U05-EV-066

是最低 required governed matrix。

Implementation 可以增加 case，但不得删除 required case。

如果某 case 因 implementation slice 还没有相应 executable surface：

    NOT_IMPLEMENTED

不得：

    SKIP and still PASS readiness implementation verification

除非 Frozen RDP-06 明确把该 case 标为：

    NOT_APPLICABLE

当前 required 66 case 中：

    none is silently optional

U05-EV-028 是 sentinel self-test，可与 normal governed cases 分开计数，但必须执行。

---

# 15. Focused test suites

Future implementation 至少应形成等价的 focused suites：

    U05ConsumerAdmissionTest
    U05D03PolicyTest
    U05ReadinessMutationTest
    U05DownstreamRoutingTest
    U05ReplayRecoveryTest
    U05EvidenceHarnessTest

具体 Java class 名可等价调整，但 evidence manifest 必须映射：

    case_id
    -> executable test id/name
    -> observed evidence record

禁止只有 JUnit PASS 而没有 per-case observed evidence。

---

# 16. Structured case evidence schema

定义：

    U05_CASE_EVIDENCE_V0_1

每条 case 至少包含：

    case_id
    category
    scenario

    implementation_sha

    expected_authority_refs[]
    expected_contract_versions[]

    fixture_id
    fixture_digest

    consultation_id
    cdp_id

    source_clinical_state_version
    current_clinical_state_version

    evaluation_context

    gate_ref
    gate_value
    restricted_context_ref?

    admission_id?
    admission_status?
    admission_reason?
    admission_replay_disposition?

    readiness_input_set_identity?
    readiness_input_refs[]

    d03_decision_id?
    d03_decision_status?
    clinical_readiness?
    d03_policy_rule_ref?

    policy_expectation_gap

    readiness_effect_id?
    readiness_payload_fingerprint?

    proposal_id?
    proposal_base_version?
    commit_status?
    commit_result_ref?
    committed_version?
    audit_ref?

    invalidation_effect_id?

    routing_decision_id?
    routing_status?
    routing_replay_disposition?
    routing_decision_fingerprint?

    downstream_permission_decision_ref?
    downstream_permission_status?

    route_effect_id?
    downstream_consequence?
    target_unit_id?
    downstream_route_authorization_id?
    eligibility_id?

    route_consumption_id?
    scheduler_intent_ref?

    failure_handoff_ref?

    correlation_id
    trace_id

    expected_boundary
    observed_boundary

    expected_result
    observed_result

    expected_side_effect_count
    observed_side_effect_count

    pass

Conditional fields 可为 null，但 schema 必须能区分：

    not applicable
    not produced because boundary stopped
    missing evidence

“missing evidence”不得伪装为 null-is-valid。

---

# 17. Expected evidence authority object

每个 case 必须额外能解析：

    ExpectedAuthority

至少：

    authority_ref
    document_path
    frozen_status
    rule_or_section_id
    semantic_claim
    source_commit_or_blob_identity

Evidence builder 必须验证：

    expected_authority_refs not empty

对于 D03 readiness case：

    expected D03/readiness result
    must trace to RDP-02 frozen rule

对于 admission：

    trace to RDP-01

对于 mutation：

    trace to RDP-03

对于 routing：

    trace to RDP-04

对于 applicability：

    trace to RDP-05

---

# 18. Contract manifest

Durable evidence bundle 必须包含：

    u05-contract-manifest.json

至少记录：

    RDP-01 path + git/blob identity + status
    RDP-02 path + git/blob identity + status
    RDP-03 path + git/blob identity + status
    RDP-04 path + git/blob identity + status
    RDP-05 path + git/blob identity + status
    RDP-06 path + git/blob identity + status

以及被引用的：

    Phase 5
    Phase 6
    Phase 8
    Phase 9

identity。

验证 run 不得只写：

    "latest"

必须绑定 exact contract identities。

---

# 19. Exact-head CI binding

Authoritative U05 verification workflow 必须：

    receive exact implementation_sha

    checkout exact implementation_sha

    assert git rev-parse HEAD == implementation_sha

并记录：

    repository
    PR/branch/ref
    implementation_sha

    workflow run id
    run attempt
    workflow file identity/digest

    contract manifest digest

    auth profile

    environment

只有 exact tested head 才能进入：

    PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW

后续 implementation commit：

    invalidates exact-head verification
    -> rerun required

除非独立证明 tree-equivalent integration，并按项目既有 PMV 规则处理。

---

# 20. CI pipeline minimum gates

Future U05 workflow 至少执行：

    G0 governance/auth profile guard

    G1 exact-head checkout identity

    G2 dependency/shared-contract install

    G3 compile

    G4 static/structural authorization guards

    G5 focused U05 tests

    G6 U05 evidence harness

    G7 diagnosis-service regression suite

    G8 structured evidence build

    G9 evidence schema/completeness validation

    G10 expected-vs-observed gate

    G11 POLICY_EXPECTATION_GAP count gate

    G12 hard-boundary side-effect gate

    G13 checksums/digests

    G14 artifact upload/retention

    G15 final identity/status report

任一失败：

    overall verification = FAIL

---

# 21. Structural authorization guards

CI 必须 fail if current authorized non-production slice introduces unauthorized activation，例如：

    direct live import/invocation of U06/U08/U10/U11 owner services
    direct external delivery sender
    direct production router
    direct model/tool/http client from deterministic U05 policy/routing layer
    direct Clinical State write bypassing K09/P01
    automatic production profile activation
    real-patient connector/source

具体 grep/static-analysis 实现可根据代码结构调整，但 guard intent 不可删除。

---

# 22. Observed evidence must come from real implementation objects

Structured case evidence 不能手工把：

    expected_result

复制到：

    observed_result

Evidence harness 必须从实际执行/returned objects/committed state/ledger 中提取 observed facts，例如：

    U05AdmissionResult
    D03 decision object
    ClinicalReadiness effect
    StateChangeProposal
    CommitResult
    authoritative Clinical State reload
    Runtime/Canonical Effect Ledger route record
    Scheduler target intent spy/fake

测试 fixture 可以 synthetic。

但 observed result 必须来源于真实实现 surface。

---

# 23. Side-effect observation

RDP-06 必须保留：

    observed_side_effect_count

以及分类 evidence，例如：

    state_commit_count
    readiness_effect_count
    route_eligibility_count
    scheduler_target_intent_count

    downstream_unit_invocation_count
    external_delivery_count
    external_tool_model_call_count

当前 non-production readiness verification 要求：

    downstream_unit_invocation_count = 0
    external_delivery_count = 0
    unauthorized_tool_model_call_count = 0

对于允许的 state/ledger effect：

    count must match case expectation

---

# 24. Replay / crash evidence

Case 不能只断言最终值相同。

必须保留：

    first attempt effect identities
    replay attempt identities
    prior commit/ledger refs
    reattached refs
    state version before/after
    side-effect counts

以证明：

    exact replay
    -> no duplicate formal effect

Crash scenarios 必须证明：

    recovery from durable state/effect ledger

而不是：

    test restarted from fresh fixture
    and happened to reach same value

---

# 25. Conflict / concurrency evidence

对于：

    commit conflict
    concurrent readiness writers
    route replay conflict

至少保留：

    competing base versions
    effect/fingerprint identities
    winning commit ref
    rejected/conflicted ref
    final authoritative state
    duplicate effect count

必须证明：

    no last-write-wins clinical overwrite
    no duplicate route intent

---

# 26. P05 correlation requirements

所有 admitted/executed case 至少必须可追踪：

    correlation_id
    trace_id

并能够串联：

    inbound request
    -> admission
    -> D03
    -> readiness effect
    -> proposal
    -> commit
    -> route decision
    -> eligibility
    -> Scheduler intent when applicable

如果某 boundary 提前停止：

    trace must end at that exact boundary

不得伪造后续 refs。

---

# 27. Durable evidence bundle

定义逻辑 artifact：

    U05_NONPROD_VERIFICATION_EVIDENCE_V0_1

至少包含：

    evidence.json
    u05-case-evidence.json
    u05-contract-manifest.json
    workflow-provenance.txt
    auth-profile.json
    regression-summary.json
    structural-guard-summary.json
    SHA256SUMS

以及 focused test reports：

    admission JUnit XML
    D03 JUnit XML
    mutation JUnit XML
    routing JUnit XML
    replay/recovery JUnit XML
    evidence-harness JUnit XML

如果实际 suite 合并，可减少 XML 文件数，但：

    all case_id -> test mapping
    must remain explicit

---

# 28. Evidence.json minimum summary

至少：

    schema
    implementation_sha

    contract_manifest_digest
    workflow identity
    repository/ref/run/attempt

    auth profile

    focused test summary
    evidence harness summary
    regression summary
    structural guard summary

    required_case_count
    executed_case_count
    passed_case_count
    failed_case_count

    policy_expectation_gap_count

    hard_boundary flags

    structured_case_evidence digest

    overall verdict

整体 verdict 只允许：

    PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW
    FAIL

CI 自己不得直接写：

    IMPLEMENTATION_VERIFIED_FINAL

最终接受需要 independent review。

---

# 29. Artifact integrity

必须生成 SHA-256 至少覆盖：

    evidence.json
    u05-case-evidence.json
    u05-contract-manifest.json
    workflow-provenance.txt
    auth-profile.json
    regression-summary.json
    structural-guard-summary.json
    all focused JUnit XML
    evidence-harness XML

Artifact upload：

    if-no-files-found = error
    overwrite = false

workflow report 必须记录：

    artifact_id
    artifact_url/ref
    artifact_digest
    implementation_sha

---

# 30. Retention / durable reference

Raw CI artifact 最低 retention：

    90 days

同时必须在 repository-governed verification status/evidence record 中长期保留：

    exact implementation_sha
    exact workflow run
    artifact id/ref
    artifact digest
    contract manifest digest
    required/passed case counts
    regression summary
    independent review ids
    final verification verdict

不得只保留一个易过期 artifact URL 而没有 digest/status record。

如果未来建立 approved long-lived Evidence Store：

    may additionally retain raw bundle there

但 RDP-06 不依赖一个当前不存在的外部 evidence system。

---

# 31. PHI / evidence minimization

Durable artifact 禁止保存完整真实患者 PHI。

Case evidence 应优先保存：

    synthetic fixture id
    digests
    stable refs
    typed business signals
    status enums
    versions
    trace refs

而不是：

    raw patient narrative
    real name/id/contact
    unrestricted model prompt transcript

---

# 32. Focused acceptance thresholds

Final exact-head run 至少要求：

    all required U05 governed cases executed
    all required cases PASS

    failures = 0
    errors = 0
    unexpected skips = 0

    policy_expectation_gap_count = 0
    in normal governed matrix

    sentinel U05-EV-028
    proves gap detector itself works

    expected_boundary == observed_boundary
    for every case

    expected_result == observed_result
    for every case where exact expected result is contract-defined

    expected_side_effect_count == observed_side_effect_count

    all hard boundaries false / satisfied

    contract manifest complete

    all checksums valid

---

# 33. Skip policy

不允许：

    flaky -> skip
    unsupported -> skip
    missing implementation -> skip

然后仍宣称 PASS。

允许 skip 只有：

    explicitly authorization-gated existing regression test
    with accepted baseline/reference

或者：

    RDP-06 explicitly marks case NOT_APPLICABLE

当前 U05 required 66-case matrix：

    no case is NOT_APPLICABLE

U05 final evidence 必须单独列：

    skipped_tests[]
    skip_reason
    accepted_baseline_ref

---

# 34. Regression acceptance

Diagnosis-service regression 必须：

    no new failure
    no new error

Existing accepted authorization-gated skips：

    may remain only when explicitly identified

若 U05 implementation 导致：

    U01-U04 behavior regression
    shared contract regression
    Foundation regression

则：

    U05 verification FAIL

不得以“U05 focused tests all green”覆盖 regression。

---

# 35. No-current-slice-live-downstream proof

由于当前 RDP-04 Freeze 明确：

    typed eligibility
    != target Unit live execution

Final evidence 必须同时包含：

    structural guard

和：

    runtime observed counter/spy evidence

证明：

    live U06 invocation = 0
    live U08 invocation = 0
    live U10 invocation = 0
    live U11 invocation = 0
    external delivery = 0

仅 grep 不足以单独证明 runtime boundary。

仅 runtime mock 也不足以证明没有结构性自动 activation。

两者都必须有。

---

# 36. Override rejection verification

必须有负向验证证明：

    frontend/model/planner cannot override admission

    frontend/model/planner cannot override D03 readiness

    Scheduler cannot remap consequence

    direct target invocation without current eligibility rejected/not wired

    direct state mutation without K09/P01 rejected/not wired

这些可以通过：

    API/service boundary test
    type/module visibility
    structural guard
    fake adversarial caller

组合证明。

---

# 37. Independent review sequence after implementation

Future U05 implementation 不能以 CI SUCCESS 自行宣布 closure。

必须按顺序：

    1. implementation exact-head review
       -> code/contract/boundary review

    2. authoritative exact-head CI run
       -> SUCCESS
       -> evidence bundle generated

    3. independent evidence-only review
       -> download/inspect exact artifact
       -> checksum/digest verify
       -> per-case evidence completeness
       -> expected authority refs verify
       -> observed provenance verify

    4. combined implementation/evidence review
       -> PASS

    5. explicit implementation closure record

之后才可能进入：

    merge authorization

仍不等于：

    production authorization

---

# 38. Independent evidence-only review minimum checks

Reviewer 至少检查：

    artifact digest matches platform metadata

    SHA256SUMS valid

    implementation_sha matches reviewed exact head

    workflow checkout matches implementation_sha

    contract manifest identities correspond to frozen contracts

    auth profile is non-production

    required case count complete

    case ids unique

    every case has expected_authority_refs

    observed evidence fields are present for applicable boundary

    no expected/observed field copied without provenance

    policy_expectation_gap_count = 0 normal cases

    sentinel gap detector test PASS

    side-effect counts match

    no live downstream execution

    regression PASS

    no unexpected skip

---

# 39. Evidence builder fail-closed rules

Evidence builder must reject artifact if：

    missing required case
    duplicate case id
    unknown case id used as replacement for required case
    missing expected authority
    missing fixture digest
    missing correlation/trace for executed case
    expected/observed mismatch
    missing required Proposal/Commit refs on committed case
    refs present beyond a boundary that should have stopped
    policy expectation gap in normal case
    hard boundary violated
    checksum input missing

Builder itself必须有 tests。

---

# 40. Evidence schema versioning

Schema：

    U05_CASE_EVIDENCE_V0_1
    U05_NONPROD_VERIFICATION_EVIDENCE_V0_1

未来 breaking schema change：

    requires new schema version
    + review

不得 silent reinterpret old artifact。

Historical evidence：

    retains original schema identity

---

# 41. Contract change invalidation

如果 U05 RDP-01/02/03/04/05/06 任何一个 Frozen Contract 发生 semantic amendment：

    prior implementation verification
    may no longer prove current contract compliance

必须：

    impact assessment

若 affected：

    rerun focused verification
    regenerate exact-head evidence
    independent evidence review

Status-only provenance update：

    does not automatically invalidate evidence

但必须证明：

    semantic reviewed content unchanged

---

# 42. Implementation change invalidation

Final verified implementation SHA 之后任何 code change touching：

    U05 implementation
    shared contracts used by U05
    K09/P01 behavior used by U05
    routing/effect ledger behavior
    relevant CI/evidence harness

必须重新评估 evidence validity。

默认：

    code change
    -> exact-head verification stale

除非：

    independently proven tree-equivalent / non-semantic under project PMV rules

---

# 43. Verification result records

Future implementation should produce repository-governed records equivalent to：

    U05_NonProduction_Implementation_Verification_Result_v0.1.md
    U05_NonProduction_Implementation_Verification_Status.md

Result record：

    chronological run/remediation detail

Status record：

    current authoritative verification state

Status record must not confuse historical pending state with current accepted state。

---

# 44. Final implementation verification verdict vocabulary

允许：

    NOT_RUN
    RUNNING
    FAIL
    PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW
    PASS

只有 independent evidence / implementation review 完成后：

    PASS

Implementation existence alone：

    != PASS

---

# 45. RDP-06 design verification itself

本 RDP-06 在 implementation 之前只需证明：

    verification plan complete
    every frozen contract family has executable cases
    expected truth authority is explicit
    durable evidence fields are defined
    failure/stop semantics are explicit
    no implementation authorization is smuggled into design

RDP-06 design PASS：

    closes RG-06 readiness-plan gap

但：

    does not mean future U05 implementation evidence has already passed

---

# 46. Coverage traceability matrix

| Contract | Verification groups |
|---|---|
| RDP-01 | EV-001..015 |
| RDP-02 | EV-016..028 |
| RDP-03 | EV-029..043 |
| RDP-04 | EV-044..060 |
| RDP-05 | EV-001/011/012/015/016..028/033/037..039 |
| Phase/Foundation regressions | EV-061..066 |

Cross-contract replay/currentness：

    EV-013..015
    EV-032..043
    EV-050..060

Safety/RESTRICTED：

    EV-003..006
    EV-036
    EV-050..053

---

# 47. BF-U05-RG-06 disposition

Original blocker：

    BF-U05-RG-06
    = Verification / durable evidence plan missing

本设计提供：

    expected-result authority hierarchy
    POLICY_EXPECTATION_GAP stop rule
    six verification layers
    non-production auth profile
    synthetic/non-PHI fixture boundary
    66 required governed cases
    structured case evidence schema
    contract manifest
    exact-head CI binding
    structural guards
    real observed evidence requirement
    side-effect counters
    replay/crash/conflict evidence
    P05 correlation
    durable artifact bundle
    checksum/digest
    retention/status record
    regression acceptance
    no-live-downstream proof
    independent evidence-only review
    implementation closure criteria

因此当前只能推进到：

    BF-U05-RG-06
    = DESIGN_RESOLVED / INDEPENDENT_REVIEW_PENDING

    U05-RDP-06
    = PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW

只有独立审查 PASS 后才可：

    BF-U05-RG-06 = CLOSED
    U05-RDP-06 = FROZEN / PASS_FOR_READINESS

---

# 48. Aggregate readiness boundary before RDP-06 review

当前：

    BF-U05-RG-01 = CLOSED
    BF-U05-RG-02 = CLOSED
    BF-U05-RG-03 = CLOSED
    BF-U05-RG-04 = CLOSED
    BF-U05-RG-05 = CLOSED
    BF-U05-RG-06 = DESIGN_RESOLVED / REVIEW_PENDING

    U05 Implementation Readiness
    = NOT_READY

    U05 Implementation Authorization Review
    = NOT_PERMITTED_YET

    U05 Implementation Authorization
    = NOT_GRANTED

即使 RDP-06 review PASS 并关闭 RG-06：

    do not automatically declare Implementation Readiness READY

必须再做：

    U05 Implementation Readiness Re-Evaluation
    across RG-01..RG-06
    against exact frozen contract package

---

# 49. Authorization boundary

本文件不授权：

    U05 runtime/code implementation
    D03 live owner execution
    production Clinical State mutation
    Scheduler live downstream invocation
    U06/U08/U10/U11 live execution
    external delivery
    merge to main
    production Clinical Runtime
    release activation
    real-patient traffic
