# U05 RDP-06 Verification / Durable Evidence Plan v0.1

> Scope: U05 当前 non-production implementation slice 的验证矩阵、expected-result authority、durable evidence schema、CI/exact-head binding、replay/crash/conflict evidence、regression gates 与 independent evidence acceptance criteria。  
> Design basis: U05-RDP-01 / 02 / 03 / 04 / 05 current frozen/refrozen semantics.  
> Immediate upstream design head: U05-RDP-04 status/provenance head `c800f8d8645d416c4a87b0d63d4fa02ec6ce8a97`.  
> Status: **REVISED / READY_FOR_FINAL_TARGETED_INDEPENDENT_DESIGN_REVIEW**.  
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
    required non-PRESENT slot lacks authoritative applicability evidence
    -> RDP-01 REJECTED
    -> U05_ADMISSION_APPLICABILITY_EVIDENCE_MISSING
    -> no D03
    -> proves missing artifact cannot infer NOT_YET_APPLICABLE / NOT_NEEDED / READY

    U05-EV-027
    POST_DDX or prior-F5-activated profile
    -> POL-011 must not apply

    U05-EV-028
    required FAILED/UNAVAILABLE input
    + one or more lower-priority business candidate signals
    -> INPUT_FAILURE
    -> no readiness
    -> proves P0 is not bypassed by lower-level business candidates

正常 governed scenario matrix 必须：

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

# 13. Verification namespaces

RDP-06 冻结三类 identity，禁止混用。

## 13.1 Governed runtime / contract cases

    U05-EV-001 .. U05-EV-060

这些 case 必须：

    consume Frozen Contract expectations
    execute implementation/runtime/governance surface
    emit U05_CASE_EVIDENCE_V0_1

Normal EV acceptance：

    policy_expectation_gap_count = 0

## 13.2 Harness self-tests

定义：

    U05-HG-001
    POLICY_EXPECTATION_GAP_DETECTOR_SELF_TEST

它故意向 verification oracle loader 提供：

    a profile without lawful business expected-authority mapping

Expected harness behavior：

    detector = FIRED
    verification path = FAIL_CLOSED
    no business expected result invented

U05-HG-001：

    != normal U05 runtime case
    != D03 runtime status
    != part of normal policy_expectation_gap_count

它的 authority 来自：

    RDP-06 Section 4
    verification-harness rule

而不是伪造一个 RDP-02 business expectation。

## 13.3 Verification / CI gates

定义：

    U05-VG-001
    no direct U05 live U06/U08/U10/U11 owner invocation

    U05-VG-002
    no unauthorized automatic production/Spring activation

    U05-VG-003
    no unauthorized model/tool/external-service call
    from deterministic U05 policy/routing slice

    U05-VG-004
    no direct Clinical State write bypassing K09/P01

    U05-VG-005
    synthetic/non-PHI fixture/evidence guard

    U05-VG-006
    Foundation + U01-U04 regression gate

VG records使用：

    U05_VERIFICATION_GATE_EVIDENCE_V0_1

而不是伪装成一个 Clinical Runtime case。

---

# 14. Scenario / harness / gate completeness rule

Required governed cases：

    U05-EV-001 .. U05-EV-060
    = 60 required EV cases

Required harness self-tests：

    U05-HG-001

Required verification gates：

    U05-VG-001 .. U05-VG-006

Implementation 可以新增 identity，但不得删除 required identity。

如果 required EV/VG/HG 因 implementation slice 尚无 executable surface：

    NOT_IMPLEMENTED
    -> overall verification FAIL

不得：

    SKIP and still PASS

除非未来受控 amendment 明确把某 identity 改为 NOT_APPLICABLE。

当前：

    no required EV/HG/VG identity is silently optional.

---

# 14.1 Independent machine-readable expectation oracle

Future implementation verification 必须包含静态、受审查的：

    u05-verification-expectations.json

定义 schema：

    U05_VERIFICATION_EXPECTATIONS_V0_1

每个 EV 至少包含：

    case_id
    fixture_id
    fixture_semantic_id
    fixture_source_ref
    fixture_digest

    expected_boundary
    expected_admission_status?
    expected_admission_reason?

    expected_d03_status?
    expected_clinical_readiness?
    expected_policy_rule_ref?

    expected_commit_status?
    expected_state_version_delta?

    expected_routing_status?
    expected_downstream_consequence?
    expected_target_unit?

    expected_replay_disposition?

    expected_effect_counts{}

    expected_authority_refs[]
    authority_semantic_claims[]

    contract_manifest_digest

Oracle 必须：

    derived from Frozen Contracts
    independently reviewed
    committed as static verification input
    hashed into durable evidence

禁止：

    production U05 code generates oracle
    D03 under test computes its own expected result
    RDP-04 router under test computes its own expected route
    observed output is copied into expected fields

CI 应在 focused execution 前读取/校验 oracle identity。

若 oracle 与 Frozen Contract 不一致：

    verification FAIL
    do not modify implementation expectation ad hoc.

---

# 14.2 D03 precedence coverage matrix

除 EV-016..028 的 branch cases 外，必须有独立 machine-readable：

    u05-d03-precedence-expectations.json

schema：

    U05_D03_PRECEDENCE_EXPECTATIONS_V0_1

其 subcase identity 建议：

    U05-PM-xxx

最低覆盖标准：

    P0 INPUT_FAILURE
    -> with lawful lower-level business candidate signal(s) present

    P1 INPUT_CONFLICT
    -> with lawful lower-level business candidate signal(s) present
       and no P0 failure

    every legally constructible precedence pair
    Pi > Pj
    where i < j

    must have a stable subcase proving:
      both candidate conditions are present
      -> Pi wins
      -> Pj is not emitted

    This applies to:
      P0 / P1 technical statuses
      when lawful lower business candidates can co-exist

      P2 .. P7 business precedence

    If a theoretical pair is NOT legally constructible
    under RDP-05 / lifecycle / context semantics:

      precedence oracle must mark:
        constructibility = NOT_CONSTRUCTIBLE

      and bind:
        frozen authority refs
        rationale

    NOT_CONSTRUCTIBLE:
      != runtime skip
      != implementation choice

    P7
    -> executable only when no P0-P6 result applies

    POL-005
    -> positive first-entry coverage

    POL-011
    -> positive current-F6-NOT_NEEDED first-entry coverage

    POL-005 / POL-011
    -> mutual-exclusivity proof

    POST_DDX / prior-F5 activation
    -> POL-011 exclusion proof

每个 precedence subcase 必须：

    have stable subcase_id
    bind RDP-02 authority
    declare constructible fixture semantics
    record higher candidate + lower candidate
    record constructibility = CONSTRUCTIBLE / NOT_CONSTRUCTIBLE
    bind constructibility authority refs
    prove emitted result equals higher lawful precedence
    for every CONSTRUCTIBLE pair

禁止为了 coverage 发明违反 RDP-05 applicability 或 frozen lifecycle 的 impossible profile。

如果 reviewer 发现一个 frozen lawful overlap 未被 precedence matrix 覆盖：

    RDP-06 implementation verification incomplete
    -> FAIL

Precedence oracle 不能由 production D03 resolver 生成。

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

    expected_effect_counts{}
    observed_effect_counts{}
    side_effect_evidence_refs{}

    pass

Conditional fields 可为 null，但 schema 必须能区分：

    not applicable
    not produced because boundary stopped
    missing evidence

“missing evidence”不得伪装为 null-is-valid。

---

# 16.1 Typed effect-count evidence

expected_effect_counts / observed_effect_counts 至少包含：

    state_commit_count
    readiness_effect_count
    invalidation_effect_count
    route_decision_count
    route_eligibility_count
    scheduler_target_intent_count
    downstream_unit_invocation_count
    external_delivery_count
    external_tool_model_call_count

可按 case 增加：

    admission_effect_count
    failure_handoff_count
    checkpoint_count

每个 non-zero observed count 必须能通过：

    side_effect_evidence_refs

定位到 commit/effect/ledger/spy record。

Verification builder 必须逐 key 比较：

    expected_effect_counts[key]
    == observed_effect_counts[key]

禁止把多个 effect 类型压成单一总数后宣称幂等通过。

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

# 17.1 Harness / verification-gate evidence schemas

HG-001 使用：

    U05_HARNESS_SELF_TEST_EVIDENCE_V0_1

至少包含：

    harness_case_id = U05-HG-001
    injected_profile_id
    injected_authority_mapping_state = MISSING_BY_TEST_DESIGN
    detector_fired
    fail_closed_observed
    no_business_expectation_invented
    harness_rule_ref = RDP06_POLICY_EXPECTATION_GAP_STOP_RULE
    pass

VG-001..006 使用：

    U05_VERIFICATION_GATE_EVIDENCE_V0_1

每条至少包含：

    gate_id
    gate_type
    verification_method
    source_paths_or_suite_refs[]
    expected_gate_result
    observed_gate_result
    evidence_refs[]
    pass

D03 precedence execution 使用：

    U05_D03_PRECEDENCE_EVIDENCE_V0_1

每条 subcase 至少包含：

    subcase_id
    fixture_semantic_id

    higher_priority_candidate
    lower_priority_candidates[]

    expected_d03_status
    expected_clinical_readiness?
    expected_policy_rule_ref

    expected_authority_refs[]

    observed_d03_status
    observed_clinical_readiness?
    observed_policy_rule_ref?

    correlation_id
    trace_id

    pass

这些 schema 与：

    U05_CASE_EVIDENCE_V0_1

分开，防止 harness/meta gate 被伪装成 Clinical Runtime case。

---

# 17.2 Oracle review gate

在 authoritative implementation evidence run 被接受之前：

    u05-verification-expectations.json
    u05-d03-precedence-expectations.json

必须完成独立 oracle review。

Oracle review 至少确认：

    every EV has expected authority
    every expected business result is derivable from Frozen Contract
    no expected result was generated by SUT
    precedence coverage satisfies Section 14.2
    contract identities/digests match current frozen package

Oracle 通过后记录：

    oracle_review_id
    oracle_digest
    precedence_oracle_review_id
    precedence_oracle_digest

Exact-head CI 必须消费相同 digest。

若 oracle 在 CI 后修改：

    evidence stale
    -> rerun required

---

# 17.3 Independently reviewed fixture manifest

Future implementation verification 必须包含静态：

    u05-verification-fixtures.json

schema：

    U05_VERIFICATION_FIXTURES_V0_1

每个 fixture 至少包含：

    fixture_id
    fixture_semantic_id
    fixture_digest

    synthetic = true
    contains_real_phi = false

    authoritative_semantic_input_summary

    evaluation_context
    relevant Gate / applicability / currentness semantics

    applicable_contract_refs[]

Fixture manifest 必须经过独立 review，并记录：

    fixture_manifest_review_id
    fixture_manifest_digest

Expectation oracle 必须绑定：

    fixture_id
    fixture_digest

或绑定：

    exact reviewed fixture_manifest_digest
    + fixture_id

Exact-head CI 必须在执行前验证：

    actual fixture digest
    == reviewed fixture digest

禁止：

    same expectation oracle
    + modified fixture content
    -> reuse prior oracle review

任何 fixture semantic/content change：

    invalidates fixture review
    invalidates dependent oracle review
    requires authoritative verification rerun

Fixture manifest 不能由 SUT 运行时动态生成。

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

同时记录：

    expectation_oracle_digest
    precedence_oracle_digest
    fixture_manifest_digest

以及对应独立 review ids。

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
    expectation oracle digest
    precedence oracle digest
    fixture manifest digest

    auth profile

    environment

    runner OS/image identity
    JDK vendor/version
    Maven version
    Python version
    materially relevant verification-tool versions
    third_party_action_pins[]

Authoritative workflow 中所有 third-party GitHub Actions 必须：

    pinned by immutable full commit SHA

禁止使用 floating tag 作为 authoritative evidence dependency，例如：

    actions/checkout@v4
    actions/setup-java@v4

可读版本标签可以写在注释/记录中，但实际 uses 引用必须是 full SHA。

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

    G1A immutable action-pin / toolchain provenance guard

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

# 23. Typed side-effect observation

每个 EV 必须保留：

    expected_effect_counts{}
    observed_effect_counts{}
    side_effect_evidence_refs{}

最低 typed keys：

    state_commit_count
    readiness_effect_count
    invalidation_effect_count
    route_decision_count
    route_eligibility_count
    scheduler_target_intent_count

    downstream_unit_invocation_count
    external_delivery_count
    external_tool_model_call_count

当前 non-production readiness verification 要求：

    downstream_unit_invocation_count = 0
    external_delivery_count = 0
    external_tool_model_call_count = 0

对于允许的 state/ledger effect：

    expected count
    == observed count

且任何 non-zero observed count 都必须有：

    side_effect_evidence_refs[key]

指向实际：

    commit
    effect ledger
    route ledger
    Scheduler intent spy/fake
    external side-effect spy

禁止：

    only total side-effect count matches
    while one effect duplicated and another missing

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
    u05-harness-self-test-evidence.json
    u05-verification-gate-evidence.json
    u05-d03-precedence-evidence.json
    u05-verification-expectations.json
    u05-d03-precedence-expectations.json
    u05-verification-fixtures.json
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
    expectation_oracle_digest
    precedence_oracle_digest
    fixture_manifest_digest

    runner_os_image
    jdk_vendor_version
    maven_version
    python_version
    third_party_action_pins[]

    workflow identity
    repository/ref/run/attempt

    auth profile

    focused test summary
    evidence harness summary
    regression summary
    structural guard summary

    required_ev_case_count
    executed_ev_case_count
    passed_ev_case_count
    failed_ev_case_count

    required_harness_self_test_count
    passed_harness_self_test_count

    required_verification_gate_count
    passed_verification_gate_count

    precedence_subcase_count
    passed_precedence_subcase_count

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
    u05-harness-self-test-evidence.json
    u05-verification-gate-evidence.json
    u05-d03-precedence-evidence.json
    u05-verification-expectations.json
    u05-d03-precedence-expectations.json
    u05-verification-fixtures.json
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

# 30. Retention / long-term durable evidence

Raw CI artifact 最低 retention：

    90 days

这只满足 near-term independent evidence review，不足以单独承担长期审计。

Independent evidence review PASS 后，必须另外形成 repository-governed、去 PHI 的 accepted evidence snapshot：

    U05_ACCEPTED_VERIFICATION_EVIDENCE_V0_1

建议文件：

    U05_Accepted_Verification_Evidence_v0.1.json

至少长期保留：

    exact implementation_sha
    exact workflow run
    artifact id/ref
    artifact digest

    workflow identity/digest
    contract manifest digest
    expectation oracle digest
    precedence oracle digest
    fixture manifest digest

    runner/toolchain identity summary
    third-party action pin summary

    every EV case_id
    expected_authority_refs
    expected vs observed boundary/result summary
    key effect/commit/route refs
    typed effect-count summary
    pass status

    HG self-test result
    VG gate results

    regression summary
    hard-boundary summary

    independent evidence review ids
    combined implementation/evidence review id
    final verification verdict

该 accepted snapshot：

    must contain no PHI
    must be repository governed
    must be checksumable/reviewable
    survives raw CI artifact expiry

Raw JUnit / large traces / full bundle：

    may retain 90-day minimum

如果未来建立 approved long-lived Evidence Store：

    raw bundle may additionally or alternatively be preserved there

但替代 repository snapshot 需要单独 governed reference rule；当前 RDP-06 不依赖一个不存在的外部 evidence system。

不得：

    artifact expires
    + only opaque digest remains
    -> still claim full long-term per-case auditability.

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

    all 60 required U05 EV cases executed
    all 60 EV cases PASS

    U05-HG-001 PASS

    all 6 U05-VG gates PASS

    every CONSTRUCTIBLE precedence subcase executed and PASS

    every NOT_CONSTRUCTIBLE precedence pair
    independently reviewed with frozen authority/rationale
    and not counted as runtime skip

    independent expectation-oracle review PASS
    independent precedence-oracle review PASS
    independent fixture-manifest review PASS

    CI-consumed oracle/fixture digests
    = reviewed digests

    immutable action-pin guard PASS
    toolchain provenance complete

    failures = 0
    errors = 0
    unexpected skips = 0

    policy_expectation_gap_count = 0
    in normal governed matrix

    U05-HG-001
    proves POLICY_EXPECTATION_GAP detector itself works

    expected_boundary == observed_boundary
    for every case

    expected_result == observed_result
    for every case where exact expected result is contract-defined

    expected_effect_counts == observed_effect_counts
    key-by-key

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

当前 U05 required identities：

    EV-001..060
    HG-001
    VG-001..006

均无 silent NOT_APPLICABLE

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

    all EV/HG/VG required identities complete

    EV case ids unique
    precedence subcase ids unique

    every EV has expected_authority_refs

    expectation oracle digest matches independently reviewed oracle
    precedence oracle digest matches independently reviewed precedence oracle
    fixture manifest digest matches independently reviewed fixture manifest
    every EV actual fixture digest matches reviewed fixture digest
    CI-consumed oracle/fixture digests match reviewed digests
    all third-party workflow actions are full-SHA pinned
    recorded action pins match executed workflow
    toolchain provenance is complete
    HG/VG/precedence evidence schema complete

    observed evidence fields are present for applicable boundary

    no expected/observed field copied without provenance

    policy_expectation_gap_count = 0 normal cases

    HG-001 gap detector self-test PASS

    typed effect-count maps match

    no live downstream execution

    regression PASS

    no unexpected skip

---

# 39. Evidence builder fail-closed rules

Evidence builder must reject artifact if：

    missing required EV/HG/VG identity
    duplicate EV/subcase identity
    unknown identity used as replacement for required identity
    missing EV expected authority
    oracle digest mismatch
    fixture manifest/digest mismatch
    expected value generated from SUT/runtime output
    precedence constructible pair missing from matrix
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

Expectation oracle / precedence oracle / fixture manifest semantic change：

    invalidates dependent verification evidence
    -> re-review + rerun required

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

| Contract / layer | Verification identities |
|---|---|
| RDP-01 | EV-001..015 |
| RDP-02 | EV-016..028 + U05-PM-* precedence subcases |
| RDP-03 | EV-029..043 |
| RDP-04 | EV-044..060 |
| RDP-05 | EV-001/011/012/015/016..028/033/037..039 |
| Harness correctness | HG-001 |
| Structural / regression | VG-001..006 |

Cross-contract replay/currentness：

    EV-013..015
    EV-032..043
    EV-050..060

Safety/RESTRICTED：

    EV-003..006
    EV-036
    EV-050..053

Oracle / fixture integrity：

    u05-verification-expectations.json
    u05-d03-precedence-expectations.json
    u05-verification-fixtures.json

Long-term evidence：

    U05_ACCEPTED_VERIFICATION_EVIDENCE_V0_1

---

# 47. Independent Review Remediation

Independent Design Review：

    PR #174
    review_id = 5263669789
    verdict = REVISE_REQUIRED

Findings：

    BF-U05-RDP06-IR-01
    = GOVERNED_RUNTIME_CASES_CONFLATED_WITH_HARNESS_META_TESTS_AND_CI_GATES

    BF-U05-RDP06-IR-02
    = TEST_ORACLE_INDEPENDENCE_UNDERDEFINED

    BF-U05-RDP06-IR-03
    = D03_PRECEDENCE_COVERAGE_NOT_SUFFICIENTLY_SPECIFIED

    BF-U05-RDP06-IR-04
    = SIDE_EFFECT_EVIDENCE_CARDINALITY_TOO_COARSE

    BF-U05-RDP06-IR-05
    = RAW_EVIDENCE_EXPIRY_LEAVES_INSUFFICIENT_LONG_TERM_AUDITABILITY

Remediation：

    IR-01
    -> EV / HG / VG namespaces separated
    -> EV-001..060 normal governed cases
    -> HG-001 harness sentinel
    -> VG-001..006 structural/regression gates

    IR-02
    -> static independently reviewed u05-verification-expectations.json added
    -> expected oracle cannot be generated by system under test

    IR-03
    -> separate D03 precedence expectation matrix added
    -> branch/lower-priority overlap + POL-005/POL-011 exclusivity coverage frozen

    IR-04
    -> scalar side-effect count replaced by typed expected/observed effect maps
    -> non-zero counts require evidence refs

    IR-05
    -> repository-retained sanitized accepted evidence snapshot required
    -> raw CI artifact may expire without destroying long-term per-case audit record

Current：

    BF-U05-RDP06-IR-01 = REMEDIATED / TARGETED_REVIEW_PENDING
    BF-U05-RDP06-IR-02 = REMEDIATED / TARGETED_REVIEW_PENDING
    BF-U05-RDP06-IR-03 = REMEDIATED / TARGETED_REVIEW_PENDING
    BF-U05-RDP06-IR-04 = REMEDIATED / TARGETED_REVIEW_PENDING
    BF-U05-RDP06-IR-05 = REMEDIATED / TARGETED_REVIEW_PENDING

    U05-RDP-06 = REVISED / READY_FOR_TARGETED_INDEPENDENT_REVIEW
    BF-U05-RG-06 = DESIGN_RESOLVED / TARGETED_REVIEW_PENDING

---

# 48. Final Targeted Remediation

Targeted Independent Design Re-Review：

    PR #174
    review_id = 5263689991
    verdict = REVISE_REQUIRED

Findings：

    BF-U05-RDP06-TR-01
    = D03_PRECEDENCE_PAIRWISE_COMPLETENESS_UNDERDEFINED

    BF-U05-RDP06-TR-02
    = ORACLE_NOT_BOUND_TO_REVIEWED_FIXTURE_IDENTITY

    BF-U05-RDP06-TR-03
    = CI_SUPPLY_CHAIN_AND_TOOLCHAIN_PROVENANCE_UNDERDEFINED

Remediation：

    TR-01
    -> every legally constructible Pi>Pj precedence pair required
    -> non-constructible pairs require frozen authority/rationale in oracle

    TR-02
    -> independently reviewed u05-verification-fixtures.json added
    -> oracle binds fixture id/digest
    -> actual CI fixture digest must match reviewed digest

    TR-03
    -> authoritative workflow actions require immutable full-SHA pins
    -> runner/JDK/Maven/Python/material toolchain provenance required

Current：

    BF-U05-RDP06-TR-01 = REMEDIATED / FINAL_TARGETED_REVIEW_PENDING
    BF-U05-RDP06-TR-02 = REMEDIATED / FINAL_TARGETED_REVIEW_PENDING
    BF-U05-RDP06-TR-03 = REMEDIATED / FINAL_TARGETED_REVIEW_PENDING

    U05-RDP-06 = REVISED / READY_FOR_FINAL_TARGETED_INDEPENDENT_REVIEW
    BF-U05-RG-06 = DESIGN_RESOLVED / FINAL_TARGETED_REVIEW_PENDING

---

# 49. BF-U05-RG-06 disposition

Original blocker：

    BF-U05-RG-06
    = Verification / durable evidence plan missing

本设计提供：

    expected-result authority hierarchy
    POLICY_EXPECTATION_GAP stop rule
    six verification layers
    non-production auth profile
    synthetic/non-PHI fixture boundary
    60 required governed EV cases
    1 required harness self-test
    6 required verification gates
    independently reviewed expectation oracle
    D03 precedence oracle/coverage
    structured case evidence schema
    contract manifest
    exact-head CI binding
    structural guards
    real observed evidence requirement
    typed side-effect count maps
    replay/crash/conflict evidence
    P05 correlation
    durable artifact bundle
    checksum/digest
    90-day raw retention + repository-retained accepted evidence snapshot
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

# 50. Aggregate readiness boundary before RDP-06 review

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

# 51. Authorization boundary

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
