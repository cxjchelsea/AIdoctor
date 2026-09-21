# U05 RDP-04 Downstream Routing / Side-effect Boundary v0.1

> Scope: authoritative committed Clinical Readiness 在 U05 之后如何形成唯一 typed downstream eligibility / route，并明确 U05、Scheduler、下游 Unit、Delivery/Failure owner 之间的 side-effect 边界。  
> Design basis: U05-RDP-03 `FROZEN / PASS_FOR_READINESS` at PR #172 status/provenance head `f90db2c1e4848370ec79fdd9e6f0a46dc983f129`.  
> Admission basis: U05-RDP-01 `FROZEN / PASS_FOR_READINESS`.  
> Decision basis: U05-RDP-02 current REFROZEN / V1.  
> Input basis: U05-RDP-05 current REFROZEN / V1.  
> Exact frozen semantic baseline before U05 RDP-04: `3bd85f908a1cb09355f6ea1c5ce737638d1c0fdc`.  
> Status: **FROZEN / PASS_FOR_READINESS**.  
> Target blocker: `BF-U05-RG-04`.  
> 本文件不授权 Runtime/code implementation、真实下游 Unit 执行、merge、production、release activation 或 real-patient traffic。

---

# 1. Design objective

当前已经冻结：

    RDP-01
    -> lawful U05 admission

    RDP-02
    -> D03 deterministic Clinical Readiness decision

    RDP-03
    -> authoritative committed Clinical Readiness state/effect

RDP-04 只回答：

> **一个已经 authoritative committed 且当前仍可消费的 Clinical Readiness，怎样被投射为唯一 typed downstream consequence；U05 在哪里停止；谁才有权实际执行下游 Unit？**

必须解决：

    readiness result -> route mapping
    committed/currentness precondition
    Safety / RESTRICTED preemption
    typed eligibility object
    route authorization identity
    exactly-one ordinary consequence
    Scheduler handoff
    downstream Unit side-effect boundary
    downstream binding unavailable
    stale route invalidation
    replay/idempotency
    race/crash recovery
    trace/audit
    non-production authorization boundary

---

# 2. Core ownership split

必须保持：

    Clinical Readiness
    != route eligibility
    != Scheduler intent
    != Unit invocation
    != downstream business effect

职责：

    U05 / D03
    -> owns Clinical Readiness interpretation

    RDP-03 / G2/P01
    -> owns authoritative Clinical Readiness commit

    U05-RDP-04 routing projection
    -> owns typed post-readiness route eligibility only

    Scheduler / Transition Engine
    -> consumes current eligibility and selects execution intent

    target Unit
    -> owns its own admission / action / business effect

    U14
    -> owns capability/runtime failure recovery routing

    Delivery Side-effect Coordinator
    -> owns delivery transport side-effects only

U05-RDP-04 不拥有：

    F3 Question truth
    DDx truth
    F6 Offline Evidence truth
    F7 Delivery truth
    U14 failure business decision
    target Unit capability execution

---

# 3. Formal post-readiness sequence

唯一合法普通顺序：

    D03 DECIDED
    -> RDP-03 readiness effect
    -> K09/G2/P01 commit
    -> authoritative Clinical State reload
    -> RDP-04 currentness / Safety / permission validation
    -> U05DownstreamRoutingDecision
    -> U05DownstreamEligibility
    -> Scheduler / Transition Engine
    -> target Unit admission
    -> target Unit execution

禁止：

    D03 decision
    -> direct target Unit

禁止：

    K09 Proposal
    -> direct target Unit

禁止：

    uncommitted readiness
    -> route

禁止：

    frontend/model/planner
    -> override route

---

# 4. Routable readiness precondition

RDP-04 只消费：

    authoritative committed ClinicalReadinessStateValue

并且必须具有：

    corresponding authoritative commit evidence

允许的 commit basis：

    COMMITTED

或：

    exact-effect authoritative REATTACH / NO_OP
    where prior committed effect is proven authoritative

禁止：

    proposal-only readiness
    D03-only readiness
    stale readiness
    uncommitted readiness
    readiness from failed/rejected/conflicted commit
    fabricated readiness enum

RDP-04 必须绑定：

    readiness_record_ref
    readiness_effect_id
    readiness_commit_evidence_ref
    readiness_clinical_state_version
    source_admission_ref
    source_d03_decision_ref

---

# 5. Effective currentness, not version equality

RDP-03 已冻结：

    version advancement alone
    != dependency invalidation

因此 readiness-only commit 从 Vn 推进到 Vn+1 后：

    source Gate may have been evaluated before readiness commit

但只要其 dependency identity 仍 current：

    Gate remains dependency-valid

RDP-04 不得使用：

    gate_version == readiness_committed_version

作为唯一 currentness 判据。

必须验证：

    readiness state_validity not STALE

    all readiness_dependency_refs current/compatible

    source U04 Gate dependency-valid

    source admission / inbound route provenance has not been invalidated

注意：

    source inbound route authorization/ref
    only proves how U05 was lawfully entered

    it does NOT authorize the downstream target action

    restricted context still current when applicable

    no newer authoritative invalidation / superseding readiness exists

如果任一 currentness 无法证明：

    no ordinary downstream eligibility

    fail closed
    -> recomputation / Safety / failure path according to current authoritative state

---

# 6. Canonical readiness-to-target mapping

Phase 6 已冻结业务映射，RDP-04 只把它 typed 化，不新增临床政策。

映射：

    NEEDS_CLARIFICATION
    -> U06

    CAN_ASK_MORE
    -> U06

    READY_FOR_CLINICAL_ANALYSIS
    -> U08

    NEEDS_OFFLINE_EVIDENCE
    -> U10

    OUT_OF_SCOPE
    -> U11

    NO_RELIABLE_DIRECTION
    -> U11

禁止新增第七个 readiness。

禁止修改上述业务 target。

---

# 7. Typed route consequence vocabulary

定义：

    U05DownstreamConsequence

V1 ordinary consequence 仅允许：

    TO_U06_QUESTION_PATH
    TO_U08_CLINICAL_ANALYSIS
    TO_U10_OFFLINE_EVIDENCE
    TO_U11_SAFE_EXIT

它们是：

    typed execution eligibility

不是：

    Unit execution result
    Clinical Truth
    delivery receipt
    capability result

映射必须是确定性的：

    NEEDS_CLARIFICATION
    -> TO_U06_QUESTION_PATH

    CAN_ASK_MORE
    -> TO_U06_QUESTION_PATH

    READY_FOR_CLINICAL_ANALYSIS
    -> TO_U08_CLINICAL_ANALYSIS

    NEEDS_OFFLINE_EVIDENCE
    -> TO_U10_OFFLINE_EVIDENCE

    OUT_OF_SCOPE
    -> TO_U11_SAFE_EXIT

    NO_RELIABLE_DIRECTION
    -> TO_U11_SAFE_EXIT

一个 current committed readiness：

    -> at most one ordinary U05 downstream consequence

---

# 8. Safety / failure preemption vocabulary

RDP-04 还必须能够表示“不产生 ordinary consequence”。

定义 routing_status：

    ELIGIBLE
    PREEMPTED
    FAILURE_REQUIRED
    REJECTED_STALE

另定义 replay_disposition：

    ORIGINAL
    REATTACHED

其中：

    ELIGIBLE
    -> carries exactly one U05DownstreamConsequence

    PREEMPTED
    -> no ordinary consequence
    -> current Safety path owns continuation

    FAILURE_REQUIRED
    -> no ordinary consequence
    -> typed handoff into existing failure-governance path
    -> not a U14 business decision

    REJECTED_STALE
    -> no ordinary consequence
    -> old route/readiness cannot be consumed

replay_disposition 语义：

    ORIGINAL
    -> newly formed routing evaluation/effect record

    REATTACHED
    -> exact prior routing decision/eligibility record reattached

例如 exact eligible replay：

    routing_status = ELIGIBLE
    replay_disposition = REATTACHED

这些都不是 Clinical Readiness value。

---

# 9. Current Safety Gate check

在 post-readiness route projection 前，必须重新确认：

    current/dependency-valid U04 Gate

Gate 只允许：

    ALLOW
    or action-permitted RESTRICTED

进入 ordinary readiness route projection。

## 9.1 ALLOW

    current Gate = ALLOW
    + readiness current
    -> apply readiness-to-target mapping

## 9.2 RESTRICTED

RESTRICTED 必须针对：

    exact downstream consequence / target action

进行 permission validation。

因此：

    U05 evaluation was permitted under RESTRICTED

不等于：

    U06/U08/U10/U11 automatically permitted

必须检查：

    restricted_context_ref
    + candidate downstream consequence
    + target action

必须形成新的 route-time：

    DOWNSTREAM_ACTION_PERMISSION_DECISION

如果 permission_status = PERMITTED：

    ordinary typed eligibility may be emitted

如果 permission_status = DENIED：

    no ordinary eligibility
    -> PREEMPTED
    -> current Safety policy / safe handling owns next consequence

如果 permission_status = UNAVAILABLE：

    no ordinary eligibility
    -> FAILURE_REQUIRED
    -> existing failure-governance handoff

禁止：

    RESTRICTED
    -> silently ALLOW

## 9.2.1 DOWNSTREAM_ACTION_PERMISSION_DECISION

RDP-04 冻结一个 route-time permission result：

    DownstreamActionPermissionDecision

它不是 Clinical Readiness，不修改上游 admission 历史。

最小字段：

    permission_decision_id

    consultation_id
    cdp_id

    current_u04_gate_ref
    restricted_context_ref

    candidate_downstream_consequence
    target_action
    target_unit_id

    permission_status
    permission_ref?

    permission_policy_id
    permission_policy_version

    evaluated_at
    validity

permission_status 只允许：

    PERMITTED
    DENIED
    UNAVAILABLE

语义：

    PERMITTED
    -> RDP-04 may continue ordinary eligibility projection

    DENIED
    -> PREEMPTED
    -> no ordinary eligibility

    UNAVAILABLE
    -> FAILURE_REQUIRED
    -> no ordinary eligibility

该 decision 的 authority 来自当前 Safety/permission governance，不来自：

    RDP-01 historical U05 admission permission
    D03
    Scheduler preference
    target Unit self-authorization

因此：

    permission to evaluate U05
    != permission to execute downstream target

---

## 9.3 BLOCKED

如果 post-readiness current Safety = BLOCKED：

    no ordinary eligibility

    routing_status = PREEMPTED

RDP-04 不把旧 readiness 转换成：

    OUT_OF_SCOPE
    NO_RELIABLE_DIRECTION

Safety path 根据现有 frozen U04/U11 semantics 处理。

## 9.4 UNAVAILABLE

如果 current Safety = UNAVAILABLE：

    no ordinary eligibility

    routing_status = FAILURE_REQUIRED

    no ordinary target_unit_id
    failure_handoff_ref must identify the existing failure-governance handoff

RDP-04 不把 UNAVAILABLE 解释成：

    NO_RELIABLE_DIRECTION
    OUT_OF_SCOPE

---

# 10. Post-readiness route source object

定义：

    U05DownstreamRoutingDecision

最小字段：

    routing_decision_id
    routing_decision_fingerprint

    consultation_id
    cdp_id

    source_readiness_record_ref
    source_readiness_effect_id
    source_readiness_commit_evidence_ref

    source_readiness_value
    source_readiness_derived_from_version
    source_readiness_committed_version

    current_clinical_state_version_at_routing

    source_u04_gate_ref
    gate_value

    source_inbound_route_ref?

    restricted_context_ref?

    downstream_permission_decision_ref?
    downstream_permission_ref?

    routing_status
    replay_disposition

    candidate_downstream_consequence?
    candidate_target_unit_id?

    downstream_consequence?
    target_unit_id?

    downstream_route_authorization_id?

    failure_handoff_ref?

    routing_policy_id = U05_RDP04
    routing_policy_version

    route_effect_id?

    canonical_event_ref
    business_event_identity
    correlation_id
    trace_id

    created_at
    validity

RDP-04 decision：

    != D03 decision
    != target Unit decision
    != Scheduler execution result

---

# 11. Downstream eligibility object

只有：

    routing_status = ELIGIBLE

才形成：

    U05DownstreamEligibility

最小字段：

    eligibility_id

    routing_decision_ref
    route_effect_id

    consultation_id
    cdp_id

    authoritative_readiness_record_ref
    authoritative_readiness_effect_id
    readiness_commit_evidence_ref

    readiness_value

    current_clinical_state_version

    downstream_consequence
    target_unit_id

    current_u04_gate_ref

    restricted_context_ref?
    downstream_permission_ref?

    canonical_event_ref
    business_event_identity

    correlation_id
    trace_id

    routing_policy_version
    validity
    created_at

Eligibility：

    != Unit invocation
    != Unit admission
    != downstream Clinical State effect

---

# 12. Routing decision identity and eligible route effect identity

所有 RDP-04 evaluation outcome 都必须具有稳定：

    U05_ROUTING_DECISION_ID

该 identity 至少绑定：

    consultation_id
    cdp_id
    authoritative readiness effect id
    current Gate ref
    candidate consequence/target when derivable
    downstream permission decision when applicable
    routing_status
    routing policy version
    routing decision contract version

因此：

    ELIGIBLE
    PREEMPTED
    FAILURE_REQUIRED
    REJECTED_STALE

都可以被 durable/replay 审计，而不需要伪造 ordinary route effect。

## 12.1 Routing decision canonical fingerprint

定义：

    U05_ROUTING_DECISION_CANONICAL_FINGERPRINT

它用于所有 routing outcome 的 replay semantic equality，包括：

    ELIGIBLE
    PREEMPTED
    FAILURE_REQUIRED
    REJECTED_STALE

至少覆盖：

    authoritative readiness record/effect/value

    current Gate ref/value

    candidate downstream consequence/target
    when derivable

    downstream permission decision/status
    when applicable

    routing_status

    failure/preemption/stale reason/ref
    when applicable

    routing policy version

    routing decision contract version

明确排除 attempt-local metadata：

    retry timestamp
    Runtime attempt number
    trace span/attempt metadata
    transport message identity
    checkpoint/run identity

因此：

    same U05_ROUTING_DECISION_ID
    + same U05_ROUTING_DECISION_CANONICAL_FINGERPRINT
    -> exact routing-decision replay candidate

    same U05_ROUTING_DECISION_ID
    + different fingerprint
    -> U05_ROUTE_REPLAY_CONFLICT

只有 ELIGIBLE ordinary route effect 才继续使用：

    U05_ROUTE_CANONICAL_PAYLOAD_FINGERPRINT

---

只有：

    routing_status = ELIGIBLE

才定义：

    U05_DOWNSTREAM_ROUTE_EFFECT_ID

至少绑定：

    consultation_id
    cdp_id

    authoritative readiness effect id

    readiness record ref

    readiness value

    current routing context

    current Gate ref

    downstream consequence
    target unit

    restricted context/permission when applicable

    routing policy version

    route effect contract version

要求：

    same exact governed route projection
    -> same route effect id

以下变化必须产生不同 route effect：

    new readiness effect
    new current Gate basis
    different restricted permission
    different consequence
    different target
    new policy version
    new relevant routing context

---

# 13. Downstream route authorization / eligibility identity

必须区分：

    source inbound route authorization/ref
    != downstream route authorization

source inbound route ref 只作为 readiness provenance/currentness evidence。

只有 routing_status = ELIGIBLE 时，RDP-04 才形成新的：

    U05_DOWNSTREAM_ROUTE_AUTHORIZATION_ID

它至少绑定：

    route_effect_id
    routing_decision_ref
    authoritative readiness effect
    current Gate
    downstream consequence
    target Unit
    downstream action permission when RESTRICTED
    routing policy version

该 authorization：

    authorizes only Scheduler consideration of this exact target intent
    != target Unit business execution
    != capability invocation
    != external side effect

定义 eligibility identity：

    U05_DOWNSTREAM_ELIGIBILITY_ID

语义可由：

    route_effect_id
    + eligibility_contract_version

稳定派生，或 durable ledger assign once。

必须满足：

    same eligible route effect replay
    -> same eligibility identity

禁止：

    retry
    -> random new eligibility id
    -> duplicate downstream execution

---

# 14. Exactly-one ordinary consequence rule

当：

    routing_status = ELIGIBLE

必须：

    exactly one downstream_consequence
    exactly one target_unit_id

禁止：

    CAN_ASK_MORE
    -> U06 + U08

禁止：

    NEEDS_OFFLINE_EVIDENCE
    -> U10 + U11 simultaneously

禁止：

    READY_FOR_CLINICAL_ANALYSIS
    -> U08 + U12

U12 normal delivery preparation 属于 post-analysis routing，不属于 U05 first-entry readiness mapping。

U05-RDP-04 不得把：

    READY_FOR_CLINICAL_ANALYSIS

直接解释成：

    delivery ready
    consultation complete

---

# 15. U06 boundary

当：

    NEEDS_CLARIFICATION
    or CAN_ASK_MORE

RDP-04 只形成：

    TO_U06_QUESTION_PATH

Scheduler 后续才可尝试 U06 admission。

U05 不：

    select actual question
    create Question
    call C03
    set WAITING_USER
    deliver message

U06 自己必须再验证：

    F1 clarification requirement
    or F3 actionable gap
    current Safety/permission
    its own mode-specific prerequisites

如果 U06 无法形成合法 question：

    it must return through its governed no-progress / reevaluation path

而不是 U05 在 RDP-04 阶段提前伪造 question。

---

# 16. U08 boundary

当：

    READY_FOR_CLINICAL_ANALYSIS

RDP-04 只形成：

    TO_U08_CLINICAL_ANALYSIS

并要求当前 Safety/restriction 对：

    U08 clinical analysis

明确允许。

U05 不：

    invoke DDx capability
    form candidates
    form Must-Exclude
    write F5 state

U08 自己执行其 admission / binding / capability / owner logic。

---

# 17. U10 boundary

当：

    NEEDS_OFFLINE_EVIDENCE

RDP-04 只形成：

    TO_U10_OFFLINE_EVIDENCE

它不代表：

    F6 assessment already complete
    examination recommendation already valid
    safe-exit delivery already complete

U10 仍需按其 mode/current context：

    evaluate F6 / offline evidence
    or follow current frozen mode semantics

RDP-04 不调用 C05。

---

# 18. U11 boundary

当：

    OUT_OF_SCOPE
    or NO_RELIABLE_DIRECTION

RDP-04 形成：

    TO_U11_SAFE_EXIT

这只是：

    Safe Exit result assembly / delivery eligibility

不是：

    SAFE_EXIT lifecycle already committed
    message already delivered

U11/F7 才拥有：

    safe-exit package
    delivery validation
    actual delivery path
    Consultation SAFE_EXIT transition

同样，Safety BLOCKED 可能由 current Safety path 进入 U11，但：

    that is U04/Safety-owned eligibility

不是把 BLOCKED 改写成某个 Clinical Readiness。

---

# 19. U14 failure boundary

RDP-04 普通 readiness mapping 不把任何六值直接映射 U14。

只有 runtime/governance failure，例如：

    current Safety UNAVAILABLE
    routing infrastructure failure
    target Unit binding resolution failure
    authorization incompatibility
    route ledger failure

才可能形成：

    FAILURE_REQUIRED

必须保持：

    FAILURE_REQUIRED
    != U14 business decision
    != terminal failure
    != safe exit

它只是：

    typed failure-governance handoff

Scheduler/Runtime 可把该 handoff 交给现有 U14 failure boundary。

U14 仍然唯一决定：

    retry
    repair
    degraded safe exit
    terminal failure

RDP-04 不决定最终 failure outcome。

---

# 20. Scheduler boundary

Scheduler 输入：

    current committed Clinical State
    + current U05DownstreamEligibility
    + current Safety/restriction context
    + current governance binding context

Scheduler 只允许：

    consume eligibility
    validate still-current
    create target execution intent

Scheduler 不允许：

    change readiness value
    remap consequence
    skip Safety permission
    invoke different target because preferred
    convert unavailable target into another clinical route silently

例如：

    TO_U08_CLINICAL_ANALYSIS

Scheduler 不得因为 U08 binding unavailable：

    -> silently invoke U10
    -> silently invoke U11

必须：

    failure/governance handling
    -> U14 according to frozen failure semantics

---

# 21. Target binding availability

Route eligibility 与 target Unit binding availability 分层。

RDP-04 形成 route 时至少要求：

    target_unit_id is one of the frozen identifiers:
      U06
      U08
      U10
      U11

    route consequence -> target Unit mapping
    exactly matches the frozen RDP-04 mapping

但不要求在 U05 内执行目标 Capability binding resolution。

实际 invoke 前：

    Scheduler / target Unit / Binding Resolver

必须验证：

    target Unit enabled for current environment
    required CapabilityBindingRef current
    required Rule/Knowledge release current
    contract/schema compatible
    restricted permission still allows action

失败：

    no target execution
    no alternate clinical route invention
    -> FAILURE_REQUIRED
    -> typed failure-governance handoff
    -> U14 remains owner of retry/repair/degraded-safe-exit/terminal outcome

---

# 21.1 No new Unit Registry dependency

RDP-04 不新增独立 Unit Registry contract。

它只冻结静态受治理 target set：

    U06
    U08
    U10
    U11

以及 readiness-to-target mapping。

真正的 execution availability：

    Unit enabled state
    CapabilityBindingRef
    Rule/Knowledge release
    schema compatibility
    environment compatibility

由现有：

    Scheduler
    Binding Resolver
    target Unit admission

在 invoke 前验证。

因此：

    RDP-04 target validation
    != registry availability validation

---

# 22. Non-production slice boundary

当前 U05 readiness package：

    NON_PRODUCTION_ONLY

因此 RDP-04 V1 当前 slice 允许：

    form typed downstream routing decision
    form typed eligibility
    persist/reconcile route effect evidence in non-production scope

但本设计不自动授权：

    Scheduler live invocation
    target Unit live business execution
    external side effects

未经后续 implementation authorization：

    eligibility
    != execution authorization

---

# 23. Staleness / invalidation

U05DownstreamRoutingDecision / Eligibility becomes non-routable when any bound dependency becomes invalid, including：

    source readiness becomes STALE

    readiness is superseded by new authoritative readiness

    source Gate/restricted context no longer permits target

    current routing policy/version incompatible

    target mapping superseded

    relevant Clinical State dependency changes

    environment authorization revoked

禁止：

    stale route
    -> silently rebind to current state

必须：

    re-evaluate from current authoritative Clinical State
    -> new/current readiness route projection

---

# 24. Readiness-only commit version safety in routing

RDP-03 已冻结：

    readiness-only commit
    -> version advance
    != automatic Gate invalidation

因此：

    readiness derived from Vn
    readiness committed in Vn+1

RDP-04 可在 Vn+1 consume：

    Gate/ref/input dependencies from the producing basis

只要：

    dependency-validity proof passes

不得要求：

    every source dependency version
    == Vn+1

也不得因为：

    current state > readiness committed version

自动判 stale。

必须按 dependency identity 判断。

---

# 25. Post-route state advance

如果 eligibility 形成后、Scheduler invoke 前：

    Clinical State advances

则 Scheduler 必须重新验证：

    eligibility still current
    readiness still current
    Gate/restricted permission still compatible

若无法证明：

    no invocation
    -> route stale
    -> re-evaluate

旧 eligibility 不得只换一个 state version 后继续执行。

---

# 26. Exact route replay

Replay 发生在当前 authoritative readiness/Gate/permission 已重新验证之后。

固定语义：

    derive U05_ROUTING_DECISION_ID

    reconcile Runtime/Canonical Effect Ledger

    if same routing decision identity
       + same canonical routing-decision fingerprint
       -> replay_disposition = REATTACHED

若 routing_status = ELIGIBLE：

    same U05_DOWNSTREAM_ROUTE_EFFECT_ID
    + same canonical route payload fingerprint
    -> reuse same downstream route authorization
    -> reuse same eligibility identity
    -> no duplicate eligibility effect

如果：

    same routing decision/effect identity
    + different canonical fingerprint

则：

    U05_ROUTE_REPLAY_CONFLICT
    -> fail closed

PREEMPTED / FAILURE_REQUIRED / REJECTED_STALE replay：

    reattach same routing decision record
    without fabricating an ordinary route effect

---

# 27. Canonical route payload fingerprint

定义：

    U05_ROUTE_CANONICAL_PAYLOAD_FINGERPRINT

至少覆盖：

    authoritative readiness effect id
    readiness value

    downstream consequence
    target unit

    Gate ref
    downstream route-time restricted context/permission decision

    relevant current routing context

    routing policy version

不包含 attempt-local：

    retry timestamp
    transport message id
    Runtime attempt
    trace span attempt
    checkpoint id

first authoritative created_at wins。

禁止 raw serialized equality 作为 replay semantic equality。

---

# 28. Runtime route ledger ownership and lifecycle

定义 durable non-clinical record：

    U05DownstreamRouteLedgerRecord

Owner：

    Runtime / Canonical Effect Ledger

它不属于：

    Clinical State
    G2 Clinical Readiness Resolver
    D03
    target Unit business state

最小可记录：

    routing_decision_id
    routing_status
    replay_disposition

    readiness_record/effect refs
    Gate/permission refs

    candidate consequence/target

    route_effect_id?
    downstream_route_authorization_id?
    eligibility_id?

    failure_handoff_ref?

    route_lifecycle?
    route_consumption_id?
    scheduler_intent_ref?

    correlation/trace refs

该 ledger record：

    = Runtime/governance durable evidence
    != Clinical Truth
    != Clinical Readiness
    != target Unit effect

只有当：

    routing_status = ELIGIBLE
    and ordinary route effect / eligibility exists

才允许 route_lifecycle 字段存在。

对于：

    PREEMPTED
    FAILURE_REQUIRED
    REJECTED_STALE

必须：

    route_lifecycle = absent

这些 non-route outcomes 只保留：

    routing_decision_id
    routing_status
    replay_disposition
    preemption/failure/stale refs
    trace refs

不得伪造 ordinary route lifecycle。

定义：

    U05RouteLifecycle

允许：

    ELIGIBLE
    CONSUMED
    STALE
    SUPERSEDED
    FAILED

含义：

    ELIGIBLE
    -> can be considered by Scheduler if all dependencies current

    CONSUMED
    -> Scheduler accepted this eligibility into one target execution intent

    STALE
    -> dependencies no longer current

    SUPERSEDED
    -> a newer authoritative route replaced it

    FAILED
    -> route infrastructure/governance failed before lawful consumption

Route lifecycle：

    != Clinical Readiness
    != target Unit lifecycle

---

# 29. At-most-once route consumption identity

定义：

    U05_ROUTE_CONSUMPTION_ID

绑定：

    eligibility_id
    route_effect_id
    target_unit_id
    current execution intent identity
    route consumption contract version

Scheduler 对同一 eligibility：

    may retry transport/runtime handling

但必须：

    at most one authoritative target execution intent

exact same consumption replay：

    reattach

different target / payload under same consumption identity：

    conflict

---

# 30. Eligibility consumed != downstream effect applied

即使：

    route lifecycle = CONSUMED

也只表示：

    Scheduler accepted the eligibility into target execution intent

不表示：

    U06 question delivered
    U08 DDx committed
    U10 F6 effect committed
    U11 delivery completed

因此：

    route consumption
    != target business effect

目标 Unit 仍需自己的：

    admission
    execution
    commit
    side-effect idempotency

---

# 31. Downstream side-effect boundary

U05-RDP-04 明确禁止 U05 直接产生：

    Question delivery
    model/tool invocation
    DDx candidate
    Must-Exclude
    examination suggestion
    external notification
    Consultation WAITING_USER
    Consultation SAFE_EXIT
    Consultation COMPLETED

U05 的最后一个普通 side effect 只允许是：

    durable route eligibility / routing evidence

并且仅限：

    governed non-production routing layer

是否真实 persist/dispatch 取决于后续 implementation authorization。

---

# 32. External side effects

任何外部不可逆 side effect：

    send message
    call external service
    notification
    delivery

都不属于 RDP-04。

这些必须由：

    target Unit
    Delivery Side-effect Coordinator
    governed tool/capability layer

通过独立 idempotency 完成。

禁止：

    route eligibility creation
    -> directly send user message

---

# 33. Currentness / mapping / permission / replay order

固定顺序：

    C0 load authoritative Clinical State

    C1 load authoritative readiness + commit evidence

    C2 verify readiness effective currentness

    C3 verify current/dependency-valid U04 Gate

    C4 deterministically derive candidate consequence/target
       from committed readiness

    C5 if Gate = RESTRICTED:
       resolve DOWNSTREAM_ACTION_PERMISSION_DECISION
       for that exact candidate consequence/target

    C6 determine routing_status:
       ELIGIBLE / PREEMPTED / FAILURE_REQUIRED / REJECTED_STALE

    C7 derive U05_ROUTING_DECISION_ID
       and eligible-only route effect id when applicable

    C8 reconcile Runtime/Canonical Effect Ledger

    C9 create or reattach routing decision / eligibility as applicable

    C10 Scheduler revalidates currentness before consumption

必须保持：

    deterministic readiness mapping
    != downstream execution authorization

以及：

    reattach
    != unconditional invoke

---

# 34. Race semantics

## 34.1 State changes during route projection

若：

    readiness current at C2
    but state/dependency changes before eligibility becomes authoritative

则：

    do not create current eligibility
    -> retry from authoritative state

## 34.2 State changes after eligibility / before Scheduler consume

    Scheduler must revalidate
    -> stale if incompatible
    -> no target invocation

## 34.3 Safety restriction changes

如果：

    RESTRICTED permission revoked/changed

则旧 eligibility：

    STALE / NON_ROUTABLE

不得保留旧 permission。

---

# 35. Crash recovery

## crash after readiness commit / before route decision

恢复：

    reload authoritative readiness
    reconcile route effect
    if none
    -> project route

## crash after route decision / before eligibility

恢复：

    route effect id
    -> reuse same routing decision identity
    -> create/reconcile same eligibility identity

## crash after eligibility / before Scheduler consumes

恢复：

    reattach eligibility
    revalidate currentness
    then Scheduler may consume

## crash after Scheduler consume / before checkpoint

恢复：

    lookup U05_ROUTE_CONSUMPTION_ID
    reconcile target execution intent

不得重复 create second target invocation intent。

---

# 36. Trace / audit chain

定义：

    U05DownstreamRoutingTrace

至少关联：

    unit_id = U05

    consultation_id
    cdp_id

    readiness_record_ref
    readiness_effect_id
    readiness_commit_evidence_ref
    readiness_value

    source_d03_decision_ref
    source_admission_ref

    current_clinical_state_version

    u04_gate_ref
    gate_value

    restricted_context_ref?
    downstream_permission_decision_ref?
    downstream_permission_ref?

    routing_status

    routing_decision_id
    route_effect_id

    downstream_consequence?
    target_unit_id?

    eligibility_id?

    route_lifecycle

    route_consumption_id?
    scheduler_intent_ref?

    routing_policy_version

    canonical_event_ref
    business_event_identity
    correlation_id
    trace_id

    failure_ref?

Trace 默认不复制完整 PHI。

---

# 37. Trace invariants

必须可证明：

    committed readiness
    -> exact route mapping

    one readiness effect
    -> at most one current ordinary consequence

    route effect
    -> at most one current eligibility identity

    exact routing decision replay
    -> same replay disposition / durable record

    exact eligible route replay
    -> no duplicate eligibility

    one eligibility
    -> at most one authoritative Scheduler target intent

    stale eligibility
    -> no target invocation

    RESTRICTED
    -> permission preserved through eligibility and Scheduler intent

    BLOCKED
    -> no ordinary route

    UNAVAILABLE
    -> no ordinary route

    target binding unavailable
    -> no alternate clinical route invention

    route consumption
    != downstream business effect applied

---

# 38. Scenario matrix

## Scenario A — CAN_ASK_MORE

    readiness = CAN_ASK_MORE
    Gate = ALLOW
    readiness current

Result：

    ELIGIBLE
    TO_U06_QUESTION_PATH
    target U06

No Question is created by U05.

## Scenario B — READY_FOR_CLINICAL_ANALYSIS

    readiness = READY_FOR_CLINICAL_ANALYSIS
    Gate = ALLOW

Result：

    ELIGIBLE
    TO_U08_CLINICAL_ANALYSIS

No DDx execution inside U05.

## Scenario C — NEEDS_OFFLINE_EVIDENCE

    readiness = NEEDS_OFFLINE_EVIDENCE
    Gate = ALLOW

Result：

    ELIGIBLE
    TO_U10_OFFLINE_EVIDENCE

No C05 invocation inside U05.

## Scenario D — OUT_OF_SCOPE

    readiness = OUT_OF_SCOPE
    Gate current

Result：

    ELIGIBLE
    TO_U11_SAFE_EXIT

No delivery side-effect inside U05.

## Scenario E — NO_RELIABLE_DIRECTION

    readiness = NO_RELIABLE_DIRECTION

Result：

    ELIGIBLE
    TO_U11_SAFE_EXIT

## Scenario F — RESTRICTED allows U05 but not U08

    readiness = READY_FOR_CLINICAL_ANALYSIS
    Gate = RESTRICTED
    U05 evaluation permission = yes
    U08 action permission = no

Result：

    PREEMPTED
    no TO_U08 eligibility

No permission widening.

## Scenario G — Safety becomes BLOCKED after readiness commit

    prior readiness remains stored
    current Safety = BLOCKED

Result：

    PREEMPTED
    no ordinary route

Readiness is not rewritten to OUT_OF_SCOPE.

## Scenario H — target Unit binding unavailable

    readiness = READY_FOR_CLINICAL_ANALYSIS
    route maps U08
    U08 required binding unavailable before invocation

Result：

    no U08 execution
    FAILURE_REQUIRED
    typed failure-governance handoff
    U14 then owns recovery/outcome

No silent U10/U11 fallback.

## Scenario I — exact route replay

    same readiness effect
    same Gate
    same downstream route-time permission decision
    same consequence/target
    same route fingerprint

Result：

    same route effect
    same eligibility
    REATTACHED

No duplicate target execution intent.

## Scenario J — eligibility stale before consume

    route created
    then current dependency changes

Result：

    Scheduler rejects stale eligibility
    no target invoke
    reevaluate current state

---

# 39. Relationship to RDP-01

RDP-01 owns：

    pre-D03 consumer admission

RDP-04 不可使用 rejected/stale admission 重新构造 readiness。

它通过 committed readiness provenance 间接引用：

    source_admission_ref

用于 audit/currentness。

---

# 40. Relationship to RDP-02

RDP-02 owns：

    Clinical Readiness policy

RDP-04 不能重新解释：

    D03 reason codes
    precedence
    READY criteria

它只读取 authoritative readiness value。

---

# 41. Relationship to RDP-03

RDP-03 owns：

    authoritative readiness mutation
    effect identity
    validity/invalidation
    commit evidence

RDP-04 只消费：

    current authoritative readiness

必须保持：

    D03 DECIDED
    != routable

    readiness COMMITTED/current
    -> may be route source

RDP-04 不写 clinical_readiness。

---

# 42. Relationship to RDP-05

RDP-04 不直接重新解析 RDP-05 input business semantics。

只允许用：

    readiness dependency refs
    currentness evidence

验证 committed readiness 是否仍 current。

禁止：

    route layer re-run D03 from F1/F3/F5/F6 inputs

---

# 43. Relationship to future RDP-06

RDP-06 至少验证：

    six readiness -> exact target mapping

    committed-only routing
    stale/uncommitted readiness blocked

    readiness-only version advancement not auto-stale

    actual dependency invalidation blocks route

    ALLOW path
    permitted RESTRICTED path
    RESTRICTED target permission denied

    BLOCKED preemption
    UNAVAILABLE failure path

    exactly one ordinary consequence

    route effect idempotency
    route replay
    route payload conflict

    eligibility stale before consume

    one eligibility -> at most one Scheduler target intent

    target binding unavailable -> U14/failure
    no alternate route invention

    no direct Unit invocation by U05

    no external side effect in RDP-04

    trace:
    committed readiness
    -> route decision
    -> eligibility
    -> Scheduler consumption
    -> target intent

---

# 44. Prohibited implementations

禁止：

    D03 decision -> direct U06/U08/U10/U11

    uncommitted readiness -> route

    stale readiness -> route

    Scheduler remaps target

    U05 invokes C03/C05/DDx capability

    U05 creates Question

    U05 sends user message

    U05 commits WAITING_USER

    U05 commits SAFE_EXIT

    U05 commits COMPLETED

    RESTRICTED U05 permission -> generic downstream ALLOW

    BLOCKED -> ordinary readiness route

    UNAVAILABLE -> NO_RELIABLE_DIRECTION

    target binding failure -> silent different clinical route

    same readiness enum -> reuse stale route across new effect

    old eligibility -> rebind to new state version

    route replay -> duplicate Scheduler target intent

---

# 45. Independent Review Remediation

Independent Review：

    PR #173
    review_id = 5263559817
    verdict = REVISE_REQUIRED

Findings：

    BF-U05-RDP04-IR-01
    = INBOUND_ROUTE_AUTHORIZATION_REUSED_AS_DOWNSTREAM_AUTHORITY

    BF-U05-RDP04-IR-02
    = DOWNSTREAM_RESTRICTED_PERMISSION_SOURCE_UNDERDEFINED

    BF-U05-RDP04-IR-03
    = UNFROZEN_TARGET_UNIT_REGISTRY_DEPENDENCY

    RQ-U05-RDP04-IR-04
    = FAILURE_REQUIRED_MUST_NOT_BE_U14_FINAL_ROUTE_DECISION

Remediation：

    IR-01
    -> source inbound route ref is provenance only
    -> new U05_DOWNSTREAM_ROUTE_AUTHORIZATION_ID is formed after post-readiness validation

    IR-02
    -> route-time DownstreamActionPermissionDecision added
    -> PERMITTED / DENIED / UNAVAILABLE explicitly defined

    IR-03
    -> removed invented Unit Registry dependency
    -> RDP-04 validates only frozen U06/U08/U10/U11 mapping
    -> execution availability remains Scheduler/Binding Resolver/target Unit responsibility

    IR-04
    -> FAILURE_REQUIRED frozen as typed failure-governance handoff only
    -> U14 remains outcome owner

Current：

    BF-U05-RDP04-IR-01 = CLOSED
    BF-U05-RDP04-IR-02 = CLOSED
    BF-U05-RDP04-IR-03 = CLOSED
    RQ-U05-RDP04-IR-04 = CLOSED

    U05-RDP-04 = REVISED / READY_FOR_TARGETED_INDEPENDENT_REVIEW
    BF-U05-RG-04 = DESIGN_RESOLVED / TARGETED_REVIEW_PENDING

---

# 46. Second Targeted Review Remediation

First Targeted Independent Design Re-Review：

    PR #173
    review_id = 5263574814
    verdict = REVISE_REQUIRED

Findings：

    BF-U05-RDP04-TR-01
    = REATTACHED_MODELED_AS_ROUTING_STATUS

    BF-U05-RDP04-TR-02
    = ROUTE_EFFECT_ID_REQUIRED_FOR_NON_ELIGIBLE_OUTCOMES

    BF-U05-RDP04-TR-03
    = RESTRICTED_PERMISSION_VALIDATION_ORDER_INVALID

    BF-U05-RDP04-TR-04
    = ROUTE_LEDGER_AND_LIFECYCLE_OWNER_UNDEFINED

Remediation：

    TR-01
    -> routing_status separated from replay_disposition
    -> ORIGINAL / REATTACHED no longer compete with ELIGIBLE/PREEMPTED/etc.

    TR-02
    -> U05_ROUTING_DECISION_ID added for every routing outcome
    -> ordinary U05_DOWNSTREAM_ROUTE_EFFECT_ID exists only when ELIGIBLE

    TR-03
    -> order corrected:
       current readiness/Gate
       -> candidate mapping
       -> RESTRICTED target permission
       -> routing status

    TR-04
    -> U05DownstreamRouteLedgerRecord added
    -> owner = Runtime / Canonical Effect Ledger
    -> explicitly not Clinical State / Readiness / target business state

Current：

    BF-U05-RDP04-TR-01 = CLOSED
    BF-U05-RDP04-TR-02 = CLOSED
    BF-U05-RDP04-TR-03 = CLOSED
    BF-U05-RDP04-TR-04 = CLOSED

    U05-RDP-04 = REVISED / READY_FOR_SECOND_TARGETED_INDEPENDENT_REVIEW
    BF-U05-RG-04 = DESIGN_RESOLVED / SECOND_TARGETED_REVIEW_PENDING

---

# 47. Final Narrow Remediation

Second Targeted Independent Design Re-Review：

    PR #173
    review_id = 5263583400
    verdict = REVISE_REQUIRED

Findings：

    BF-U05-RDP04-TR2-01
    = ROUTING_DECISION_CANONICAL_FINGERPRINT_REFERENCED_BUT_UNDEFINED

    BF-U05-RDP04-TR2-02
    = ROUTE_LIFECYCLE_APPLIED_TO_NON_ROUTE_OUTCOMES

Remediation：

    TR2-01
    -> U05_ROUTING_DECISION_CANONICAL_FINGERPRINT formally defined
    -> applies to all routing outcomes
    -> eligible-only route payload fingerprint remains separate

    TR2-02
    -> route_lifecycle made optional/conditional
    -> only ELIGIBLE ordinary route effects may own U05RouteLifecycle
    -> PREEMPTED / FAILURE_REQUIRED / REJECTED_STALE keep decision-level durable evidence only

Current：

    BF-U05-RDP04-TR2-01 = CLOSED
    BF-U05-RDP04-TR2-02 = CLOSED

    U05-RDP-04 = FROZEN / PASS_FOR_READINESS
    BF-U05-RG-04 = CLOSED

---

# 48. BF-U05-RG-04 disposition

Original blocker：

    BF-U05-RG-04
    = U05 downstream routing / side-effect boundary missing

本设计提供：

    committed-readiness routing precondition
    effective currentness
    readiness-to-target mapping
    typed consequence vocabulary
    Safety/failure preemption
    RESTRICTED target permission
    routing decision
    downstream eligibility
    route effect identity
    eligibility identity
    exactly-one route
    Scheduler boundary
    target binding failure semantics
    stale route invalidation
    replay/idempotency
    route consumption identity
    crash/race recovery
    trace/audit
    no-direct-side-effect boundary

Final Targeted Independent Design Re-Review 已 PASS，因此：

    BF-U05-RG-04 = CLOSED
    U05-RDP-04 = FROZEN / PASS_FOR_READINESS

该 closure 仅表示 Downstream Routing / Side-effect Boundary 的 readiness/design 缺口已经关闭，不表示 Scheduler 或任何 downstream Unit 已实现/激活。

---

# 49. Current aggregate readiness boundary

当前：

    BF-U05-RG-01 = CLOSED
    BF-U05-RG-02 = CLOSED
    BF-U05-RG-03 = CLOSED
    BF-U05-RG-04 = CLOSED
    BF-U05-RG-05 = CLOSED
    BF-U05-RG-06 = OPEN / BLOCKING

    U05 Implementation Readiness
    = NOT_READY

    U05 Implementation Authorization Review
    = NOT_PERMITTED_YET

    U05 Implementation Authorization
    = NOT_GRANTED

---

# 50. Authorization boundary

本文件不授权：

    U05 runtime/code implementation
    Scheduler live execution
    U06/U08/U10/U11 live invocation
    capability/tool/model calls
    external delivery side effects
    merge to main
    production Clinical Runtime
    release activation
    real-patient traffic


---

# 51. Final Targeted Review / Freeze Provenance

    PR #173

    Initial Independent Design Review
    = REVISE_REQUIRED
    review_id = 5263559817

    First Targeted Independent Design Re-Review
    = REVISE_REQUIRED
    review_id = 5263574814

    Second Targeted Independent Design Re-Review
    = REVISE_REQUIRED
    review_id = 5263583400

    Final Targeted Independent Design Re-Review
    = PASS
    review_id = 5263589876

    reviewed semantic head
    = 99b69879fccc4a213ad1572dffee32ab8c1d4351

    BF-U05-RDP04-IR-01 = CLOSED
    BF-U05-RDP04-IR-02 = CLOSED
    BF-U05-RDP04-IR-03 = CLOSED
    RQ-U05-RDP04-IR-04 = CLOSED

    BF-U05-RDP04-TR-01 = CLOSED
    BF-U05-RDP04-TR-02 = CLOSED
    BF-U05-RDP04-TR-03 = CLOSED
    BF-U05-RDP04-TR-04 = CLOSED

    BF-U05-RDP04-TR2-01 = CLOSED
    BF-U05-RDP04-TR2-02 = CLOSED

    BF-U05-RG-04 = CLOSED
    U05-RDP-04 = FROZEN / PASS_FOR_READINESS

Current aggregate:

    BF-U05-RG-01 = CLOSED
    BF-U05-RG-02 = CLOSED
    BF-U05-RG-03 = CLOSED
    BF-U05-RG-04 = CLOSED
    BF-U05-RG-05 = CLOSED
    BF-U05-RG-06 = OPEN / BLOCKING

    closed = 5
    open blocking = 1

    U05 Implementation Readiness = NOT_READY
    U05 Implementation Authorization Review = NOT_PERMITTED_YET
    U05 Implementation Authorization = NOT_GRANTED

This provenance update changes status only and does not authorize implementation.
