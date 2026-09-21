# U05 RDP-03 State Ownership / K09-P01 Mutation / Trace Contract v0.1

> Scope: U05 / D03 Clinical Readiness 在 D03 已完成 deterministic decision 后，如何形成唯一 authoritative Clinical State mutation、K09 Proposal、G2/P01 commit、幂等 effect、失效语义与 P05 Trace/Audit 证据链。
> Design basis: U05-RDP-01 FROZEN / PASS_FOR_READINESS, PR #171 status/provenance head 9cf9f8754cdf145b24ae50442c528b2062b54cf7.
> D03 basis: U05-RDP-02 current REFROZEN / V1.
> Input basis: U05-RDP-05 current REFROZEN / V1.
> Exact frozen semantic baseline before U05 readiness-package additions: 3bd85f908a1cb09355f6ea1c5ce737638d1c0fdc.
> Status: PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW.
> Target blocker: BF-U05-RG-03.
> 本文件不授权 U05 runtime/code implementation、post-D03 downstream execution、merge、production、release activation 或 real-patient traffic。

---

# 1. Design objective

RDP-01 已冻结：

    lawful upstream route
    + current Safety Gate
    + authoritative RDP-05 input snapshot
    -> U05 admission
    -> U05AdmittedInput

RDP-02 已冻结：

    U05AdmittedInput
    -> D03 deterministic policy
    -> exactly one D03 decision object

其中只有：

    decision_status = DECIDED

才允许携带六值 Clinical Readiness：

    OUT_OF_SCOPE
    NEEDS_OFFLINE_EVIDENCE
    NEEDS_CLARIFICATION
    CAN_ASK_MORE
    READY_FOR_CLINICAL_ANALYSIS
    NO_RELIABLE_DIRECTION

本文件只回答：

> 一个已经合法形成的 D03 DECIDED 结果，怎样成为唯一 authoritative Clinical Readiness state？

需要冻结：

    Clinical Readiness authoritative state envelope
    state ownership
    commit eligibility
    K09 Proposal schema
    field/path permission
    readiness effect identity
    proposal idempotency
    G2/P01 validation
    commit-result semantics
    same-value/new-basis handling
    replace/supersede/invalidation
    version-safety
    conflict recovery
    replay/crash recovery
    P05 trace/audit chain
    no-capability semantics

---

# 2. Non-negotiable ownership model

必须保持：

    Clinical Truth
    != Model Output
    != Capability Result
    != Runtime State
    != Trace

Clinical Readiness 的唯一业务解释 Owner：

    G2 / Clinical Readiness Resolver
    executed through U05 / D03

正式 state mutation 的唯一写入边界：

    K09 StateChangeProposal
    -> G2/P01 State Governance
    -> authoritative Clinical State

因此：

    D03 Decision
    != StateChangeProposal

    StateChangeProposal
    != CommitResult

    CommitResult
    != downstream execution authorization

U05 没有独立 AI Capability。

    U05/D03 = deterministic policy path
    CapabilityBindingRef = NOT_APPLICABLE for D03 itself

不得为了满足通用 Trace schema 而伪造假的 U05 clinical capability binding。

---

# 3. Formal mutation sequence

正式链路：

    U05AdmittedInput
        -> D03
        -> D03 Decision
        -> if DECIDED only
        -> U05 Readiness Effect
        -> K09 StateChangeProposal
        -> G2/P01 State Governance
        -> CommitResult
        -> authoritative Clinical State reload
        -> RDP-04 may later evaluate post-D03 route

如果：

    D03 = INPUT_FAILURE
    or D03 = INPUT_CONFLICT

则：

    no Readiness Effect
    no K09 Proposal
    no Clinical Readiness commit
    no ordinary post-D03 route

如果 POLICY_EXPECTATION_GAP 被 design/readiness verification 触发：

    no normal runtime D03 decision
    no Proposal
    no Commit
    Implementation Readiness fails closed

---

# 4. Authoritative Clinical Readiness record

定义 governed object：

    ClinicalReadinessRecord

它是 Clinical State 中 clinical_readiness 的 authoritative structured value。

最小字段：

    readiness_record_id
    clinical_readiness

    consultation_id
    cdp_id

    derived_from_clinical_state_version
    committed_in_clinical_state_version

    evaluation_context

    source_admission_id
    source_d03_decision_ref
    source_d03_decision_status

    source_readiness_input_manifest_ref
    source_readiness_input_set_identity
    source_readiness_input_refs[]

    source_u04_gate_ref
    source_route_authorization_type
    source_route_authorization_ref
    source_restricted_context_ref?

    policy_id = D03
    policy_version
    policy_rule_ref
    rule_release_refs[]
    knowledge_release_refs[]

    record_validity
    invalidation_reason_refs[]

    effect_id
    proposal_ref
    commit_result_ref
    audit_ref

    created_at

readiness_record_id 标识本次 authoritative readiness record。

它不等于：

    D03 decision_id
    proposal_id
    effect_id
    Clinical State Version

record_validity 是治理 metadata，不是第七个 Clinical Readiness business value。

允许：

    CURRENT
    STALE
    SUPERSEDED

含义：

    CURRENT
    = 当前 authoritative state 仍可合法使用该 readiness record

    STALE
    = 其依赖发生合法变化，不能继续用于当前 route

    SUPERSEDED
    = 已被后续 authoritative readiness record 取代

历史 record 必须保留审计，不允许就地篡改历史业务决策。

---

# 5. Commit eligibility

只有以下条件全部满足，才允许生成 readiness effect / K09 Proposal：

    RDP-01 admission_status = ADMITTED

    U05AdmittedInput is still current

    D03 decision_status = DECIDED

    clinical_readiness in frozen six-value vocabulary

    D03 decision binds same:
      consultation_id
      cdp_id
      input_clinical_state_version
      admission_id / admitted snapshot
      readiness_input_set_identity
      current Gate/restricted context

    D03 decision validity = current for commit

    no POLICY_EXPECTATION_GAP

以下均禁止 Proposal：

    INPUT_FAILURE
    INPUT_CONFLICT
    POLICY_EXPECTATION_GAP
    stale admission
    stale D03 decision
    version mismatch
    lost restricted context
    changed readiness-input-set identity
    untrusted policy version

---

# 6. Commit-time currentness revalidation

Proposal creation 前和 P01 commit 前必须再次确认：

    authoritative current Clinical State Version
    = D03 input_clinical_state_version
    = proposal.base_clinical_state_version

且：

    source U04 Gate remains dependency-valid
    source route authorization remains valid for this effect
    source restricted context remains compatible
    source readiness-input-set identity remains authoritative/current

必须保持：

    D03 decided at Vn
    + state became Vn+1 before readiness commit
    -> old D03 Proposal cannot blindly commit

此时：

    CommitResult = CONFLICT
    or proposal rejected before commit
    -> reload authoritative state
    -> old admission becomes stale
    -> new RDP-01 admission required
    -> D03 must be reevaluated on new admitted snapshot

禁止：

    old D03 decision
    + new Clinical State Version
    -> just change baseVersion and retry

---

# 7. U05 Readiness Effect identity

定义：

    CLINICAL_READINESS_EFFECT_ID

最小语义组成：

    consultation_id
    cdp_id

    source_admission_id
    source_d03_decision_ref

    input_clinical_state_version
    evaluation_context

    source_readiness_input_set_identity

    clinical_readiness

    policy_id
    policy_version
    policy_rule_ref

    source_u04_gate_ref
    source_route_authorization_type
    source_route_authorization_ref

    source_restricted_context_ref when applicable

    readiness_effect_contract_version

不冻结具体 hash 算法。

要求：

    same exact governed D03 effect
    -> same effect identity

以下任一变化必须产生不同 effect identity：

    new admission
    new D03 decision
    new input state version
    new readiness-input-set identity
    different readiness value
    different D03 policy version/rule
    different Gate/route basis
    different restricted context

---

# 8. Same value != same effect

必须显式区分：

    same Clinical Readiness enum
    != same governed effect

例如：

    V10:
    D03 -> CAN_ASK_MORE
    based on input-set A

    V12:
    D03 -> CAN_ASK_MORE
    based on input-set B

即使业务值相同，也必须形成新的 authoritative provenance。

因此：

    same readiness value
    + different effect identity
    -> NOT a replay NO_OP merely because enum is same

是否需要新 Clinical State Version，由 new governed effect/provenance 决定，而不是只看 enum 是否变化。

---

# 9. K09 StateChangeProposal contract

定义：

    U05ClinicalReadinessProposal

使用 Phase 8 K09 generic StateChangeProposal，并冻结 U05-specific minimum。

最小字段：

    proposal_id

    consultation_id
    cdp_id

    base_clinical_state_version

    producer = U05
    business_owner = G2_CLINICAL_READINESS_RESOLVER

    source_admission_ref
    source_decision_ref
    source_effect_id

    reason_code

    operations[]

    evidence_refs[]
    source_refs[]

    rule_release_refs[]
    knowledge_release_refs[]

    capability_binding_refs[] = empty / NOT_APPLICABLE for D03 itself

    idempotency_key

    correlation_id
    trace_id

    created_at
    proposal_contract_version

必须：

    source_decision_ref = D03 decision_id
    source_admission_ref = exact admitted U05 snapshot used by D03
    source_effect_id = CLINICAL_READINESS_EFFECT_ID

V1 reason family：

    U05_COMMIT_CLINICAL_READINESS

业务解释仍由：

    clinical_readiness
    D03 reason_codes[]
    policy_rule_ref
    basis_refs[]

承载。

---

# 10. Logical state path and field permission

RDP-03 冻结逻辑 path：

    /patient_state/clinical_readiness

如果最终工程 schema 使用等价 typed field，可在实现时映射，但不得改变 ownership。

U05 Proposal 只允许修改：

    clinical_readiness

以及该 record 内属于其 provenance/validity 的 metadata。

U05 Proposal 禁止修改：

    clinical_risk
    safety_gate
    patient facts
    information_gaps
    questions
    ddx_candidates
    must_exclude
    offline_evidence
    delivery_readiness
    Consultation lifecycle
    Runtime Thread/Run/Checkpoint

P01 必须执行 field permission validation。

---

# 11. Proposal operation semantics

Clinical Readiness 是单一 current system-level derived state。

因此 Proposal 使用：

    TEST / PRECONDITION
    +
    ADD or REPLACE

当 authoritative state 中不存在 active/current readiness record：

    TEST expected current readiness = absent
    ADD /patient_state/clinical_readiness

当存在 current readiness record，且新 D03 effect 需要成为新的 authoritative readiness：

    TEST expected current readiness ref/identity = existing authoritative readiness
    REPLACE /patient_state/clinical_readiness

旧 record：

    remains in version history/audit
    becomes SUPERSEDED in governance interpretation

不得物理覆盖历史证据。

如果 authoritative state/effect ledger 已证明：

    same CLINICAL_READINESS_EFFECT_ID
    + same normalized record payload
    + prior commit authoritative

则不得再生成第二个 state effect。

允许：

    NO_OP / attach prior commit

具体实现可在 Proposal 前 reconciliation 或 P01 idempotency 中完成。

---

# 12. Proposal idempotency

定义：

    U05_READINESS_PROPOSAL_IDEMPOTENCY_KEY
    = CLINICAL_READINESS_EFFECT_ID
      + proposal_contract_version

要求：

    same effect replay
    -> same idempotency key

禁止：

    same business_event_identity only
    -> assume same readiness effect

因为同一个 accepted event 可能合法触发多个 distinct governed effects。

---

# 13. P01/G2 validation checklist

P01 commit 前至少验证：

    proposal contract/schema valid

    producer allowed to propose clinical_readiness

    business_owner = G2 Clinical Readiness Resolver

    source admission exists and is ADMITTED

    source D03 decision exists
    source D03 decision_status = DECIDED

    proposal clinical_readiness
    = source D03 clinical_readiness

    proposal effect_id
    = deterministic expected effect identity

    base Clinical State Version current

    consultation_id/cdp_id match authoritative state

    readiness-input-set identity matches D03/admission snapshot

    Gate/route/restricted refs match source admission/decision

    policy_id/version/rule refs match D03

    operation set touches only allowed readiness path

    typed value schema valid

    idempotency/replay check passes

    evidence/source refs resolve

    authorization/environment permits current non-production mutation

任何校验失败：

    must not commit readiness

---

# 14. CommitResult semantics

沿用 K09/P01 generic：

    COMMITTED
    REJECTED
    CONFLICT
    NO_OP
    FAILED

## COMMITTED

表示 exact readiness effect is now authoritative in new committed Clinical State Version。

必须返回/可关联：

    previous_version
    committed/new_version
    commit_result_ref
    audit_ref
    proposal_ref
    effect_id
    authoritative_readiness_record_ref

只有 COMMITTED 或已证明 exact-effect authoritative 的 NO_OP/reattach，才可被后续 RDP-04 消费。

## NO_OP

只允许：

    same exact effect identity
    already authoritatively committed
    and same normalized record payload

NO_OP 不允许仅因为 new D03 produced same readiness enum 就跳过新的 provenance effect。

## CONFLICT

表示：

    base version
    or expected current readiness/gate/input basis
    no longer matches authoritative state

处理：

    reload
    invalidate old admission for further execution
    new RDP-01 admission
    new D03 evaluation
    new effect/proposal if still applicable

不得 stale proposal blind retry。

## REJECTED

用于：

    field permission
    schema
    source binding
    policy binding
    authorization
    evidence
    operation
    idempotency conflict

等 governance rejection。

    REJECTED
    != Clinical Readiness business result

## FAILED

技术执行失败不得映射成：

    NO_RELIABLE_DIRECTION
    OUT_OF_SCOPE
    NEEDS_OFFLINE_EVIDENCE

---

# 15. Version-safety of readiness commit

这是 RDP-03 的关键规则。

假设：

    D03 evaluated authoritative state Vn

readiness Proposal 以：

    base_version = Vn

合法 commit 后：

    authoritative state = Vn+1

必须保持：

    readiness-only state commit advancing version
    != automatic invalidation
       of the Gate/readiness-input dependencies
       used to produce that readiness

原因：

    version advancement alone
    != dependency invalidation

Readiness record 保存：

    derived_from_clinical_state_version = Vn
    committed_in_clinical_state_version = Vn+1

只要依赖 identity 仍有效：

    record_validity = CURRENT

定义治理 effect classification：

    READINESS_ONLY_COMMIT

它表示：

    the commit changes only authoritative clinical_readiness
    + its provenance metadata

因此它自身不得被解释为：

    new patient fact
    new F3 gap
    new Risk evidence
    new Safety evidence
    new F5 DDx
    new F6 Workup

所以：

    READINESS_ONLY_COMMIT
    -> no mandatory U03/U04 rerun
       solely because version number advanced

如果 commit 同时包含任何其他 effect：

    not allowed by RDP-03

---

# 16. Dependency-validity model

Clinical Readiness record 必须绑定：

    readiness_dependency_refs[]

至少包含其实际依赖的：

    source admission
    source readiness-input refs/applicability evidence
    source U04 Gate
    route authorization/ref
    restricted context when applicable
    D03 policy/rule

并按当前 context 包含 F1/F3/F5/F6 authoritative refs。

Currentness 判断：

    record is CURRENT
    iff all required dependency identities remain valid/current
    under frozen owner/invalidation semantics

禁止：

    current state version > committed version
    -> automatically STALE

同样禁止：

    same state version
    -> automatically CURRENT

因为同版本 non-state-mutating revalidation 也可能更新 authoritative readiness-input-set identity。

---

# 17. Invalidation triggers

RDP-03 不新增医学失效规则，只落实 Phase 4 已冻结依赖。

至少以下变化可使当前 readiness 失效：

    F1 framing/scope changes

    accepted F2 fact changes
    that affect readiness dependencies

    F3 current gap/readiness input changes

    F4/Safety basis changes
    such that current Gate/restriction no longer permits same path

    F5 DDx/Must-Exclude readiness input changes

    F6 Offline Evidence readiness input changes

    current route/evaluation context becomes incompatible

    restricted context changes or is revoked

    D03 policy/rule release changes under an explicitly authorized migration
    for a not-yet-consumed current decision

失效语义：

    existing ClinicalReadinessRecord
    -> STALE

或者在新的 authoritative readiness commit 后：

    old record
    -> SUPERSEDED

必须由：

    G2 State Governance / lawful owner-driven invalidation effect

执行。

U05/D03 不可直接修改历史 record。

---

# 18. Invalidation evidence

任何 readiness invalidation 必须可绑定：

    invalidation_effect_id
    prior_readiness_record_ref
    triggering_authoritative_change_ref
    affected_dependency_ref
    invalidation_reason_code
    source_decision/event ref
    proposal_ref
    commit_result_ref
    audit_ref
    before_version
    after_version

不得：

    Runtime sees mismatch
    -> silently ignore old readiness

而没有可审计 invalidation/currentness evidence。

但 Runtime 可在 routing/admission 时拒绝消费无法证明依赖 current 的 record。

---

# 19. New D03 decision replacing old readiness

当新的合法 U05 admission + D03 DECIDED 成立：

    new readiness effect
    -> K09 Proposal
    -> REPLACE current readiness

旧 readiness：

    SUPERSEDED

新 record 的：

    source_admission_id
    source_d03_decision_ref
    source_readiness_input_set_identity
    derived_from version
    policy version

必须是新的 authoritative basis。

不能只 overwrite enum value 并保留旧 provenance。

---

# 20. Input failure / conflict after prior readiness exists

如果当前 state 中已有旧 readiness，但新的 lawful U05 evaluation 得到：

    D03 INPUT_FAILURE
    or INPUT_CONFLICT

则：

    no new readiness value is committed

但：

    old readiness must not automatically remain routable

若 upstream mutation/currentness semantics 已使旧 readiness dependency invalid：

    old readiness = STALE / non-routable

该 stale 状态必须由此前 accepted mutation/invalidation or currentness evidence 证明。

禁止：

    new D03 failed
    -> silently reuse old readiness

也禁止：

    new D03 failed
    -> overwrite old readiness with fake negative readiness

---

# 21. Restricted-context preservation through commit

如果 source Gate：

    RESTRICTED

则 Clinical Readiness record、effect、proposal、trace 必须保留：

    source_restricted_context_ref

以及能证明 U05 evaluation was permitted 的 permission evidence ref。

Readiness commit：

    does not widen downstream permission

因此：

    READY_FOR_CLINICAL_ANALYSIS committed
    under RESTRICTED U05 permission
    != U08 automatically permitted

post-D03 action permission 属于 RDP-04。

---

# 22. Policy / release binding

D03 是 deterministic policy。

Proposal/record 必须保留：

    policy_id = D03
    policy_version
    policy_rule_ref
    rule_release_refs[]
    knowledge_release_refs[]

如果当前具体 D03 rule 不依赖某类 release：

    empty list is allowed when lawfully NOT_APPLICABLE

不得 invent placeholder release refs。

历史 committed readiness 保留历史 policy/release refs。

新 policy/release 不 rewrite historical readiness record。

---

# 23. No-capability trace semantics

由于 U05/D03 本身 has no Clinical AI Capability：

    capability_binding_ref
    = absent / NOT_APPLICABLE

不能伪造 C03、C05 或 P03 model 作为 D03 producer。

RDP-03 trace 应直接证明：

    admitted owner inputs
    -> deterministic D03 decision
    -> K09 proposal
    -> P01 commit

---

# 24. P05 trace chain

定义逻辑 evidence chain：

    U05ReadinessMutationTrace

最小关联：

    unit_id = U05

    admission_id
    admitted_input_ref

    consultation_id
    cdp_id

    input_clinical_state_version
    evaluation_context

    u04_gate_ref
    route_authorization_type
    route_authorization_ref
    restricted_context_ref?

    readiness_input_manifest_ref
    readiness_input_set_identity
    readiness_input_refs[]

    d03_decision_id
    d03_decision_status
    clinical_readiness
    d03_reason_codes[]
    policy_id
    policy_version
    policy_rule_ref

    readiness_effect_id

    proposal_id
    proposal_base_version
    proposal_idempotency_key
    proposal_operation_summary

    commit_status
    commit_result_ref
    previous_version
    new_version?
    audit_ref
    conflict_detail?
    retryable?

    authoritative_readiness_record_ref?

    canonical_event_ref
    business_event_identity
    correlation_id
    trace_id

    created_at/duration/status

默认不复制完整 PHI payload。

---

# 25. Trace invariants

必须可通过 Trace/Audit 证明：

    accepted admission
    -> exact D03 input set

    exact D03 decision
    -> exact readiness effect

    exact effect
    -> at most one authoritative commit effect

    proposal base version
    -> commit previous version

    COMMITTED
    -> new version contains matching readiness record

    NO_OP
    -> existing authoritative commit has same effect identity/payload

    CONFLICT
    -> no new readiness state from stale proposal

    REJECTED/FAILED
    -> no authoritative readiness effect

    restricted context
    -> preserved from admission through commit

Trace 不可作为 authoritative Clinical Readiness 本身。

---

# 26. Commit/replay scenarios

## Scenario A — first successful readiness commit

    Admission A @ V10
    D03 DECIDED / CAN_ASK_MORE
    Effect E1
    Proposal P1 base V10
    P01 COMMITTED
    -> V11

Authoritative record：

    clinical_readiness = CAN_ASK_MORE
    derived_from = V10
    committed_in = V11
    effect_id = E1
    record_validity = CURRENT

## Scenario B — exact replay after commit

    same Admission A
    same D03 decision
    same Effect E1
    same payload

Result：

    reattach prior authoritative commit
    or NO_OP with prior commit ref
    no V12 only because replay occurred

## Scenario C — same value, new basis

    new Admission B @ V20
    new D03 decision
    same enum = CAN_ASK_MORE
    different input set
    Effect E2

Result：

    new governed effect
    new provenance
    not replay NO_OP

## Scenario D — commit conflict

    D03 decided @ V30
    other lawful state effect commits -> V31
    old readiness proposal base = V30

Result：

    CONFLICT
    reload V31
    new RDP-01 admission
    new D03
    no blind retry

## Scenario E — INPUT_FAILURE

    Admission accepted
    D03 = INPUT_FAILURE

Result：

    no effect
    no proposal
    no commit

## Scenario F — old readiness invalid after user correction

    old readiness R1
    accepted correction changes dependent fact
    -> lawful invalidation
    -> R1 STALE
    -> new route/currentness evaluation

R1 cannot be reused merely because its enum would still look plausible.

## Scenario G — readiness-only commit version safety

    Gate/current inputs valid @ V40
    D03 DECIDED @ V40
    READINESS_ONLY_COMMIT
    -> V41

If no dependency changed：

    source Gate/input dependencies remain dependency-valid

    version number advance alone
    does not force U03/U04 rerun

---

# 27. Crash recovery

## crash after D03 / before Proposal

恢复：

    load admission
    load D03 decision
    check current version/dependencies
    if still valid
    -> deterministically recreate same effect/proposal identity

不得重新随机生成第二个 effect identity。

## crash after Proposal / before Commit response

恢复：

    lookup effect/proposal/idempotency
    reconcile P01/ledger

如果实际已 commit：

    attach authoritative result
    no second commit

## crash after commit / before checkpoint

从：

    authoritative Clinical State
    + effect ledger
    + proposal ref
    + commit ref
    + audit ref

恢复。

不得因为 checkpoint 缺失重复 readiness effect。

---

# 28. Concurrent U05 evaluation

同一 Consultation 不允许两个 authoritative readiness writers。

如果两个 U05 executions 都基于 same base version：

    only one may commit

另一个：

    CONFLICT
    -> reload
    -> re-admit/re-evaluate

如果 exact same effect identity：

    reattach / NO_OP

如果不同 effect：

    must not last-write-wins blindly

---

# 29. Relationship to RDP-01

RDP-03 只接受 U05AdmittedInput。

必须复用：

    admission_id
    accepted Gate ref
    accepted route ref
    accepted readiness-input-set identity
    accepted readiness-input refs
    accepted restricted context

RDP-03 不得：

    fetch a new unadmitted input set
    replace route basis
    repair a rejected admission

---

# 30. Relationship to RDP-02

RDP-02 owns：

    D03 decision semantics
    decision status
    Clinical Readiness vocabulary
    precedence
    policy rules

RDP-03 owns：

    how DECIDED becomes authoritative state

RDP-03 不得：

    recompute D03
    reinterpret reason codes
    upgrade INPUT_FAILURE to business readiness

---

# 31. Relationship to RDP-05

RDP-05 owns readiness input semantics。

RDP-03 只保存 exact accepted readiness-input-set identity and refs used by D03，用于：

    provenance
    invalidation
    replay
    audit

不得复制/重算 applicability truth。

---

# 32. Relationship to future RDP-04

RDP-04 只能消费：

    authoritative committed Clinical Readiness

以及：

    current validity
    current Safety/restriction permission
    commit/effect refs

D03 决策本身在 successful readiness commit 前：

    != routable Clinical Readiness

只有：

    COMMITTED
    or exact-effect authoritative NO_OP/reattach

才可进入 post-D03 routing evaluation。

---

# 33. Relationship to future RDP-06

RDP-06 至少验证：

    DECIDED -> Proposal
    INPUT_FAILURE -> no Proposal
    INPUT_CONFLICT -> no Proposal

    six readiness values only

    proposal only touches clinical_readiness

    proposal source/admission/decision bindings

    same exact effect replay
    same value / different basis != replay

    COMMITTED path
    NO_OP exact-effect path
    CONFLICT re-admission path
    REJECTED no-state path
    FAILED no-state path

    stale admission blocked before commit
    state version race
    concurrent writers

    readiness-only commit does not trigger version chase
    real dependency change does invalidate readiness

    restricted context preserved
    downstream permission not widened

    no fake CapabilityBindingRef

    P05 trace:
    admission -> D03 -> effect -> proposal -> commit -> state version

---

# 34. Prohibited implementations

禁止：

    D03 writes Clinical State directly
    Controller writes clinical_readiness
    Frontend writes clinical_readiness
    model output writes clinical_readiness

    INPUT_FAILURE commits UNKNOWN
    INPUT_CONFLICT commits fallback readiness
    POLICY_EXPECTATION_GAP commits anything

    same readiness enum -> always NO_OP

    old admission + new base version -> retry
    commit conflict -> update baseVersion only

    READINESS_ONLY_COMMIT -> force endless U03/U04 rerun

    version number greater -> automatically mark readiness stale
    same version -> automatically assume readiness current

    restricted U05 permission -> automatically permit U08/U10/U11

    fake CapabilityBindingRef for D03
    checkpoint as readiness truth

---

# 35. BF-U05-RG-03 disposition

Original blocker：

    BF-U05-RG-03
    = Clinical Readiness state mutation / K09-P01 / Trace contract missing

本设计提供：

    authoritative ClinicalReadinessRecord
    commit eligibility
    logical state path
    field permission
    readiness effect identity
    same-value/new-basis rule
    K09 proposal
    idempotency key
    P01 validation
    CommitResult semantics
    replace/supersede
    dependency-based invalidation
    readiness-only version-safety
    conflict/replay/crash recovery
    restricted-context preservation
    no-capability trace
    P05 end-to-end evidence chain

因此当前只能推进到：

    BF-U05-RG-03
    = DESIGN_RESOLVED / INDEPENDENT_REVIEW_PENDING

    U05-RDP-03
    = PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW

只有独立设计审查 PASS 后才可：

    BF-U05-RG-03 = CLOSED
    U05-RDP-03 = FROZEN / PASS_FOR_READINESS

---

# 36. Current aggregate readiness boundary

当前：

    BF-U05-RG-01 = CLOSED
    BF-U05-RG-02 = CLOSED
    BF-U05-RG-03 = DESIGN_RESOLVED / REVIEW_PENDING
    BF-U05-RG-04 = OPEN / BLOCKING
    BF-U05-RG-05 = CLOSED
    BF-U05-RG-06 = OPEN / BLOCKING

    U05 Implementation Readiness = NOT_READY
    U05 Implementation Authorization Review = NOT_PERMITTED_YET
    U05 Implementation Authorization = NOT_GRANTED

---

# 37. Authorization boundary

本文件不授权：

    U05 runtime/code implementation
    D03 live execution
    Clinical Readiness live commit
    RDP-04 downstream routing
    U05->U06/U08/U10/U11 execution
    merge to main
    production Clinical Runtime
    release activation
    real-patient traffic
