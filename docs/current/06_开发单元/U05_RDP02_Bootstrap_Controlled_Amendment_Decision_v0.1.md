# U05 RDP-02 Bootstrap Controlled Amendment Decision v0.1 (Revised)

> Scope: 解决 U05/D03 首轮 Clinical Readiness bootstrap underdetermination 的受控设计修订决策包。
>
> Status: REVISED / READY_FOR_FIFTH_TARGETED_INDEPENDENT_REVIEW / OWNER_SELECTION_NOT_YET_AUTHORIZED
>
> Basis:
>
> - main@6e68fd9fb7cd19e87aadae30f3bb53a2264d1920
> - U05-RDP-05 frozen at fd0e88e21aaab2a2ab67ffd1449dce8e946d7ed5
> - Bootstrap Independent Design Review = REVISE_REQUIRED at PR #127 exact head 42484959ae5d5c434027a74a019a6919ee42aef1
>
> 本文件只修复 Bootstrap Controlled Amendment Decision 的设计完整性。
> 不修改任何 frozen Phase 4/5/6/7/8/9 或 U05-RDP-05 语义，不批准任何 candidate，不授予 implementation / routing / production / real-patient authorization。

---

## 1. Problem statement

当前合法首轮 profile：

    Safety Gate = ALLOW / permitted RESTRICTED
    F1 = PRESENT / FRAMED_IN_SCOPE
    F3 = NOT_YET_APPLICABLE
    F5 = NOT_YET_APPLICABLE
    F6 = NOT_YET_APPLICABLE

在现有 frozen semantics 下：

    F3 NOT_YET_APPLICABLE
    != no active gap
    != READY evidence

因此 D03 无法合法证明：

    CAN_ASK_MORE

或：

    READY_FOR_CLINICAL_ANALYSIS

与此同时：

    Phase 5/6/9 ordinary path
    Safety/U04
    -> U05 Readiness
    -> CAN_ASK_MORE
    -> U06/F3/C03

而 Phase 4 又冻结：

    F2 fact change
    -> F3 Gap
    -> downstream reevaluation
    -> Clinical Readiness reevaluation

因此存在真实 cross-phase bootstrap tension。

该问题定义为：

    U05_BOOTSTRAP_CROSS_PHASE_DESIGN_GAP

它不是：

    patient business outcome
    Capability failure
    D03 fallback condition
    runtime-normal state

---

## 2. Governance classification

本问题必须按：

    CONTROLLED UPSTREAM DESIGN AMENDMENT

处理，而不是：

    ordinary Owner parameter selection

必须保持：

    Clinical Readiness has one G2/U05 resolver
    U05 has no independent AI Capability
    U05 must not invent Gap/DDx/offline evidence
    F3 remains the canonical owner of information-gap / online-question-value semantics unless explicitly amended
    fixed completeness percentage = PROHIBITED
    legacy required-field checklist as truth = PROHIBITED
    null/no-gap -> READY = PROHIBITED
    LLM final readiness decision = PROHIBITED
    Business Owner != executable Unit != Scheduler node
    Clinical Truth != Runtime State != Trace

Owner approval：

    != frozen-contract amendment
    != independent re-review
    != re-freeze
    != implementation authorization

---

# 3. Exact frozen-artifact impact inventory

任何 candidate 被 Owner 选中后，都必须以本矩阵为起点做 exact amendment plan。

Legend:

    YES = candidate 必然修改该 frozen boundary
    CONDITIONAL = 取决于所选 subvariant / exact implementation contract
    NO_EXPECTED = 当前 candidate 不预期修改，但详细设计仍需确认无隐含影响

| Frozen artifact / boundary | A1 U06+C03 pre-readiness F3 | A2 Dedicated non-C03 F3 Unit | B1 F2/U02 + F2_SUFFICIENCY | B2 New positive-sufficiency Owner/Unit |
|---|---|---|---|---|
| Phase 4 F3 owner / Gap lifecycle | YES | YES | CONDITIONAL | CONDITIONAL |
| Phase 4 Readiness input-source semantics | timing change | timing change | semantic expansion | YES, source/owner expansion |
| U04-RDP-04 Downstream Routing Boundary | YES: A1 pre-readiness eligibility | YES: A2 pre-readiness eligibility | YES: B1 pre-readiness eligibility | YES: B2 pre-readiness eligibility |
| Phase 5 Safety→Readiness→F3 business loop | YES | YES | YES | YES |
| Phase 6 U05 S_in / Trigger | YES | YES | YES | YES |
| Phase 6 U06 S_in / Action | YES | NO_EXPECTED | NO_EXPECTED | NO_EXPECTED |
| Phase 7 C03 FIRST_CONSUMER_UNIT | NO_EXPECTED: still U06 | NO | NO | NO |
| Phase 7 C03 capability usage semantics | YES: assessment before question delivery | NO | NO | NO |
| Phase 7 U02/C01 dependency semantics | NO | NO | YES: mode-aware dependency amendment | NO |
| Phase 8 readiness input / decision / K09 contracts | YES | YES | YES: deterministic decision/readiness-input ref; no pre-D03 K09 state mutation | YES |
| Phase 9 Unit Scheduler / transition graph | YES | YES | YES | YES |
| Phase 9 dependency resolution / commit sequence | YES | YES | YES | YES |
| U05-RDP-05 applicability/source/version contract | YES | YES | YES | YES |
| U05-RDP-02 D03 policy | YES | YES | YES | YES |

每次 amendment 必须补充：

    exact artifact path
    exact section / frozen statement
    old text / old invariant
    proposed replacement
    compatibility impact
    required independent re-review
    new frozen exact head

禁止只写：

    "Phase 6 affected"

而不说明具体修改哪一个 Unit boundary。

---

# 3.1 Unified Pre-Readiness Version-Safety Contract

本节统一约束所有发生在：

    current committed U04 Safety Gate
    -> first U05 D03

之间的 pre-readiness effect。

Frozen U04-RDP-04 requires:

    routable Safety Gate
    = current committed U04 Gate
    bound to current Clinical State Version

因此任何 pre-readiness candidate 必须且只能采用以下两类版本模式之一。

## VS-A — SAME_VERSION_NON_STATE_DECISION

适用于：

    positive readiness evidence
    that does not need to become canonical Clinical State before D03

规则：

    current U04 Gate @ Vn
    -> deterministic governed decision/readiness-input @ Vn
    -> NO K09 StateChangeProposal
    -> NO Clinical State Version advance
    -> U05/D03 @ Vn

必须绑定：

    consultation_id
    current Clinical State Version = Vn
    current U04 Gate ref @ Vn
    exact policy/rule/release refs
    source decision ref
    input/evidence refs
    validity/staleness
    replay/idempotency identity

任何后续 Clinical State Version 变化：

    Vn -> Vn+1

都会使旧 decision/readiness input：

    STALE

除非存在显式 frozen current-version revalidation/reference-binding rule。

当前 candidate mapping：

    B1 = VS-A
    B2 = VS-A

## VS-B — STATE_MUTATION_WITH_POST_COMMIT_SAFETY_BARRIER

适用于：

    canonical business state
    that must be committed before D03

Current mapping:

    A1 = canonical F3 Gap -> VS-B
    A2 = canonical F3 Gap -> VS-B

Required sequence:

    current U04 Gate @ Vn
    -> pre-readiness F3 assessment using current facts/framing @ Vn
    -> canonical F3 K09/G2/P01 commit
    -> Clinical State Version advances to Vn+1
    -> prior U04 Gate @ Vn becomes NON_ROUTABLE for U05
    -> enter POST_F3_SAFETY_REVALIDATION_BARRIER
    -> re-establish required current risk/safety dependencies
    -> obtain current committed U04 Safety Gate @ Vk
    -> current-version revalidate/reference-bind canonical F3 readiness input to Vk
    -> U05/D03 only when Gate + all required readiness inputs are current at Vk

The original U04 Gate @ Vn authorizes only:

    the pre-readiness F3 assessment effect

It does NOT authorize:

    U05 routing after the F3 state commit

## VS-B termination invariant

POST_F3_SAFETY_REVALIDATION_BARRIER must not create an infinite loop.

The barrier freezes these rules:

    F3_CANONICAL_EFFECT_ID
    = consultation
      + fact/framing basis identity
      + F3 assessment policy/capability binding
      + source assessment version/event identity

Once the canonical F3 effect for the same fact/framing basis has been applied:

    later U03/U04 risk/safety-only commits
    MUST NOT retrigger another canonical F3 commit
    merely because Clinical State Version advanced.

During the barrier:

    U03/U04 may recompute/revalidate risk/safety derived state as required;

    they must not manufacture new F1/F2 facts/framing;

    the already-committed F3 Gap remains canonical unless one of its true invalidation dependencies changed.

A second F3 canonical assessment is allowed only when:

    F1 framing changed
    or
    F2 patient facts changed/corrected
    or
    an explicitly frozen F3 evidence dependency changed
    or
    the prior F3 assessment itself became invalid/failed under a governed rule.

Version advancement caused only by:

    F3 commit
    U03 risk re-evaluation/revalidation
    U04 Safety Gate re-evaluation/revalidation

is NOT by itself an F3 invalidation reason.

## VS-B current-version F3 binding

RDP-05 already requires all D03 inputs to be current-version compatible and allows explicit current-version revalidation/reference binding for older source decisions/state.

Therefore after Safety barrier completion:

    canonical F3 state may have been committed at Vn+1
    current authoritative state may be Vk

D03 may consume F3 only through a current-version readiness-input envelope that binds:

    clinical_state_version = Vk
    source_state_ref = canonical F3 state ref
    source_decision_ref = F3 assessment/revalidation ref
    current-version revalidation_ref
    current U04 Gate ref @ Vk
    validity = CURRENT

This revalidation:

    DOES NOT duplicate canonical F3 Gap state
    DOES NOT silently rewrite historical F3 provenance
    DOES NOT create another F3 Clinical State mutation unless a true F3 invalidation dependency changed.

## Cross-candidate invariant

For every candidate:

    pre-readiness effect after U04
    -> either VS-A
       or VS-B

No third ambiguous persistence mode is allowed.

Forbidden:

    state mutation after U04
    -> reuse old U04 Gate silently

    old-version readiness input
    -> D03 because "content seems unchanged"

    version advancement alone
    -> retrigger same canonical F3 effect

    current Gate + stale readiness input
    -> D03

# 3.2 U04-RDP-04 Candidate-Specific Pre-Readiness Eligibility Amendment Impact

本节只描述：

    frozen U04-RDP-04 Downstream Routing Boundary

在 bootstrap candidate 被 Owner 选择后的受控 amendment 影响。

它不修改：

    U04 Safety business ownership
    Safety Gate vocabulary
    U04 clinical risk/safety judgment semantics

它只修改：

    committed/current Safety Gate
    -> downstream eligibility projection

## 3.2.1 Frozen boundary being amended

Current frozen U04-RDP-04:

    ALLOW
    -> may make U05 ordinary-path eligibility available

    RESTRICTED
    -> only governed restricted downstream path

    BLOCKED
    -> no ordinary U05 continuation; U11 eligibility may be exposed

    UNAVAILABLE
    -> no ordinary U05 continuation; U14 eligibility may be exposed

Bootstrap candidates require the immediate ordinary path to become:

    current Safety Gate
    -> candidate-specific pre-readiness eligibility when bootstrap effect is still required
    -> candidate pre-readiness effect
    -> eventual U05 eligibility only after bootstrap requirement is satisfied/current

Therefore:

    U04-RDP-04
    = CONTROLLED AMENDMENT REQUIRED
    for A1 / A2 / B1 / B2

## 3.2.2 Ownership invariant

U04 still answers only:

    ALLOW
    RESTRICTED
    BLOCKED
    UNAVAILABLE

U04 does NOT:

    execute U06
    execute U02
    execute a new pre-readiness Unit
    invoke C03
    evaluate F2/F3 sufficiency
    choose A1/A2/B1/B2 at runtime
    decide final Clinical Readiness

The routing projection consumes:

    current committed U04 Gate
    + active BootstrapArchitectureBindingRef
    + current bootstrap-effect status/ref when applicable

and exposes typed eligibility.

Therefore:

    Gate decision
    != downstream execution

    Eligibility projection
    != Unit invocation

Scheduler remains the execution coordinator.

## 3.2.3 Active candidate binding

Only one bootstrap architecture may be active for a governed Consultation/runtime scope:

    BootstrapArchitectureBindingRef
    = A1 / A2 / B1 / B2

Candidate selection is configuration/governance binding, not a U04 decision.

A single U04 result must NOT simultaneously expose:

    A1 eligibility
    + A2 eligibility
    + B1 eligibility
    + B2 eligibility

for the same governed path.

The current active binding determines which candidate-specific eligibility type is legal.

## 3.2.4 Candidate-specific eligibility types

If bootstrap effect is required and not yet valid/current:

    A1:
    PRE_READINESS_A1_F3_C03_ELIGIBLE

    A2:
    PRE_READINESS_A2_F3_DETERMINISTIC_ELIGIBLE

    B1:
    PRE_READINESS_B1_F2_SUFFICIENCY_ELIGIBLE

    B2:
    PRE_READINESS_B2_MINIMUM_SUFFICIENCY_ELIGIBLE

These are routing eligibility projections, not Clinical State and not Clinical Readiness.

They must bind at least:

    consultation_id
    cdp_id
    clinical_state_version
    u04_gate_ref
    gate_value
    bootstrap_architecture_binding_ref
    restricted_context_ref when applicable
    bootstrap_requirement_status
    bootstrap_effect_ref when applicable
    eligibility_type
    validity
    created_at / trace refs
    routing projection policy/version

## 3.2.5 Gate-to-pre-readiness projection

### ALLOW

For a current committed ALLOW Gate:

    if active candidate bootstrap effect is required
    and no valid/current completion/effect ref exists
    -> expose exactly one candidate-specific PRE_READINESS_*_ELIGIBLE

    if active candidate bootstrap requirement is already valid/current
    -> may expose U05_ELIGIBLE

No candidate Unit is executed directly by U04.

### RESTRICTED

For RESTRICTED:

    candidate-specific pre-readiness eligibility
    or eventual U05 eligibility

may be exposed only when:

    the frozen/governed U04 restricted policy
    explicitly permits that downstream action under the restricted context

and the projection must carry:

    restricted_context_ref

If the selected candidate's pre-readiness assessment is not permitted under the restricted context:

    no ordinary bootstrap continuation
    no ordinary U05 continuation

The package does not invent a fallback route.

### BLOCKED

    PRE_READINESS_*_ELIGIBLE = PROHIBITED
    U05_ELIGIBLE = PROHIBITED

Existing governed high-risk escalation / U11 eligibility remains the only applicable downstream family according to frozen business-loop rules.

### UNAVAILABLE

    PRE_READINESS_*_ELIGIBLE = PROHIBITED
    U05_ELIGIBLE = PROHIBITED

Failure-handling / U14 eligibility remains applicable according to frozen failure rules.

## 3.2.6 VS-A eligibility transition

Applies to:

    B1
    B2

One current U04 Gate creates one governed routing authorization:

    routing_authorization_id
    = business event identity
      + current u04_gate_ref
      + clinical_state_version
      + bootstrap_architecture_binding_ref

Sequence:

    current U04 Gate @ Vn
    -> one routing authorization @ Vn
    -> first eligible consequence =
       PRE_READINESS_B1/B2_*_ELIGIBLE
    -> Scheduler executes selected VS-A assessment
    -> deterministic decision/readiness-input @ Vn
    -> no Clinical State Version advance
    -> same routing authorization remains valid only if Gate/version/restricted context remain current
    -> Scheduler may continue to U05 under the same authorization
       after verifying current bootstrap completion/decision ref @ Vn

There is NOT a second independent U04 routing side effect for U05.

Instead:

    U04 authorization
    -> sequenced Scheduler consequences:
       pre-readiness
       -> U05

The same U04 Gate may remain current because VS-A does not mutate Clinical State.

Unit execution and bootstrap decision artifacts still use their own effect/idempotency identities,
but they are descendants of the same routing_authorization_id.

## 3.2.7 VS-B eligibility transition

Applies to:

    A1
    A2

Initial sequence:

    current U04 Gate @ Vn
    -> routing authorization RA-n
    -> first eligible consequence =
       PRE_READINESS_A1/A2_*_ELIGIBLE @ Vn
    -> Scheduler executes selected F3 assessment
    -> canonical F3 commit
    -> Clinical State Version advances
    -> RA-n and every eligibility under Gate @ Vn become STALE/NON_ROUTABLE

Then:

    POST_F3_SAFETY_REVALIDATION_BARRIER
    -> current Risk/Safety re-established
    -> current U04 Gate @ Vk
    -> canonical F3 effect is current-version revalidated/reference-bound @ Vk
    -> new routing authorization RA-k is projected from the new current Gate

RA-k must check:

    active BootstrapArchitectureBindingRef
    + current F3 bootstrap_effect_ref/revalidation_ref
    + F3_CANONICAL_EFFECT_ID already satisfied for the same fact/framing basis

If satisfied/current:

    first eligible consequence under RA-k
    = U05_ELIGIBLE @ Vk

RA-k MUST NOT expose:

    PRE_READINESS_A1/A2_*_ELIGIBLE

merely because a new U04 Gate exists.

A new pre-readiness A1/A2 eligibility may be exposed only if a true F3 invalidation dependency requires a new canonical F3 effect.

Therefore:

    one U04 Gate/result
    -> one routing authorization for that version/path identity

and the post-commit Gate necessarily produces a new authorization because the prior one is stale.

This is the routing-level counterpart of the VS-B no-cycle rule.

## 3.2.8 Eligibility invalidation

Any eligibility is invalid when:

    its u04_gate_ref is no longer current
    or
    clinical_state_version changed
    or
    active BootstrapArchitectureBindingRef changed
    or
    restricted_context_ref became incompatible
    or
    required bootstrap effect/input became STALE/FAILED/UNAVAILABLE
    or
    routing projection policy/version is no longer valid for the bound consultation

Stale eligibility:

    must not invoke downstream Unit
    must not enter U05
    must not be silently rebound

Scheduler must reload authoritative state and re-project eligibility.

## 3.2.9 Idempotency / replay

A routing authorization identity must bind at least:

    business event identity
    + current u04_gate_ref
    + clinical_state_version
    + bootstrap_architecture_binding_ref
    + restricted_context_ref when applicable

The authorization contains/derives the currently eligible consequence:

    PRE_READINESS_*_ELIGIBLE
    or
    U05_ELIGIBLE

based on the bound bootstrap completion/effect state.

Replay of the same U04 projection:

    may attach/return the authoritative routing_authorization_id
    must not create a second routing authorization
    must not duplicate downstream Unit execution

Each actual Unit execution still uses a separate effect identity,
linked to the routing_authorization_id.

Therefore the frozen U04-RDP-04 idempotency invariant is preserved in the proposed amendment:

    one committed U04 result
    -> at most one routing authorization for the same business event / selected path identity

VS-A:

    the same authorization sequences:
    PRE_READINESS consequence
    -> U05 consequence
    without a second U04 routing side effect

VS-B:

    state-version advance invalidates the old authorization;
    a new current U04 Gate creates a new authorization for the new version.

## 3.2.10 Exact candidate impact summary

    A1:
    U04-RDP-04 must expose PRE_READINESS_A1_F3_C03_ELIGIBLE
    before initial U05.
    After canonical F3 commit, old eligibility expires.
    New current Gate + current F3 completion ref -> U05_ELIGIBLE.

    A2:
    U04-RDP-04 must expose PRE_READINESS_A2_F3_DETERMINISTIC_ELIGIBLE.
    Same VS-B expiry/barrier/U05 transition as A1.

    B1:
    U04-RDP-04 must expose PRE_READINESS_B1_F2_SUFFICIENCY_ELIGIBLE.
    VS-A decision does not advance Clinical State Version.
    Same current Gate + current F2_SUFFICIENCY ref -> U05_ELIGIBLE.

    B2:
    U04-RDP-04 must expose PRE_READINESS_B2_MINIMUM_SUFFICIENCY_ELIGIBLE.
    VS-A decision does not advance Clinical State Version.
    Same current Gate + current B2 decision/input ref -> U05_ELIGIBLE.

## 3.2.11 Frozen-artifact amendment boundary

If any candidate is selected, the detailed amendment must include:

    docs/current/06_开发单元/U04_RDP04_Downstream_Routing_Boundary_v0.1.md

and explicitly amend only the downstream eligibility contract required by the selected candidate.

Unless independently justified later:

    U04 Safety Gate owner
    U04 Safety decision vocabulary
    U11/U14 ownership split

must remain unchanged.

The amended U04-RDP-04 must undergo:

    exact diff review
    independent re-review
    re-freeze at exact head

before any bootstrap runtime implementation authorization.

---

# 4. Candidate A — F3 remains the positive sufficiency producer

Candidate A 的共同目标：

    before first D03 evaluation
    -> F3 Owner has a lawful current-version canonical Gap assessment
    -> D03 can consume:
       CAN_ASK_MORE
       or NO_ACTIVE_ONLINE_BLOCKING_GAP

共同不变量：

    one F3 Owner
    one canonical F3 Gap lifecycle
    no standalone sufficiency side-channel
    no U05-owned Gap generation
    no duplicate pre-D03 vs U06/U09 Gap truth
    all canonical Gap mutation -> governed K09/G2/P01 commit

Candidate A 分为两个可执行 subcandidate。

---

## 4.1 Candidate A1 — U06 pre-readiness F3 assessment using C03

### Semantic owner

    F3

不变。

### Execution host

    U06

但 U06 新增一个明确的：

    PRE_READINESS_GAP_ASSESSMENT entry mode

该 mode 不等于普通 Question Delivery mode。

### Scheduler position

Current:

    U04
    -> U05
    -> CAN_ASK_MORE
    -> U06

Proposed A1:

    U04 ALLOW / permitted RESTRICTED @ Vn
    -> U06 PRE_READINESS_GAP_ASSESSMENT
    -> canonical F3 Gap commit -> Vn+1
    -> POST_F3_SAFETY_REVALIDATION_BARRIER
    -> current Risk/Safety re-established
    -> current U04 Gate @ Vk
    -> current-version F3 readiness-input revalidation/ref-binding @ Vk
    -> U05 D03
    -> if CAN_ASK_MORE
       -> U06 QUESTION_SELECTION_DELIVERY

因此 U06 可在同一 consultation 中以不同业务 trigger 被合法进入两次。

### Trigger

    PRE_READINESS_GAP_ASSESSMENT_REQUIRED

只有：

    current U04 admitted Safety context
    + current committed facts
    + no current-version canonical F3 assessment

时可触发。

### Capability use

    C03 = YES

Phase 7：

    FIRST_CONSUMER_UNIT = U06

仍保持，因此不需要把 C03 first consumer 改成 U05。

但必须扩展 C03 在 U06 内的使用时机：

    pre-readiness Gap Detection / Decision Impact
    before question delivery selection

### Input

    current committed Clinical Facts
    current Clinical State Version
    current Safety restriction context
    approved C03 CapabilityBindingRef
    applicable rule/knowledge refs

### Output

必须是 canonical F3 business state，而不是临时 side-channel：

    canonical Information Gap records
    Gap Decision Impact
    F3 readiness input:
      CAN_ASK_MORE
      / NEEDS_OFFLINE_EVIDENCE
      / NO_ACTIVE_ONLINE_BLOCKING_GAP

### State commit

    required

Canonical F3 mutation：

    -> K09 StateChangeProposal
    -> G2/P01 commit
    -> new Clinical State Version
    -> prior U04 Gate becomes non-routable for U05
    -> mandatory POST_F3_SAFETY_REVALIDATION_BARRIER
    -> re-establish current U04 Gate
    -> current-version revalidate/reference-bind F3 readiness input
    -> U05 consumes only current Gate + current-compatible F3 input

A1 version mode：

    VS-B = STATE_MUTATION_WITH_POST_COMMIT_SAFETY_BARRIER

### Replay / idempotency

至少绑定：

    consultation_id
    source Clinical State Version
    assessment trigger/event identity
    C03 CapabilityBindingRef
    F3 assessment policy/version
    effect idempotency key

Same replay：

    must not create duplicate Gap records
    must not duplicate Question candidates
    must attach/return authoritative existing effect

Termination/idempotency：

    same F3_CANONICAL_EFFECT_ID
    + only downstream Risk/Safety version advancement
    -> no second canonical F3 commit

Only true F3 invalidation dependencies may create a new canonical F3 effect.

### Failure owner

Capability/runtime failure：

    -> typed failure contract
    -> U14 eligibility
    -> no fake NO_ACTIVE_ONLINE_BLOCKING_GAP
    -> no D03 execution from missing F3 result

### Main amendment consequences

A1 必改：

    U04-RDP-04
    - ALLOW / permitted RESTRICTED may expose PRE_READINESS_A1_F3_C03_ELIGIBLE
    - U04 never directly executes U06/C03
    - after F3 commit, old Gate/eligibility expires
    - post-barrier current Gate + current F3 completion ref may expose U05_ELIGIBLE

    Phase 5
    - BL-04 trigger/order: F3 assessment can precede first Readiness

    Phase 6
    - U06 gains PRE_READINESS_GAP_ASSESSMENT entry
    - U05 consumes committed F3

    Phase 7
    - C03 first Unit remains U06
    - C03 usage expands to pre-readiness assessment

    Phase 8
    - canonical F3 proposal/commit contract before D03

    Phase 9
    - Scheduler edge U04 -> U06(pre) -> POST_F3_SAFETY_REVALIDATION_BARRIER -> U05
    - U06(pre) != U06(question delivery)
    - barrier must re-establish current U04 Gate before U05
    - same canonical F3 effect must not repeat on Risk/Safety-only version advancement

    RDP-05
    - POST_SAFETY_INITIAL F3 no longer always NOT_YET_APPLICABLE after pre-assessment trigger
    - applicability/version matrix must distinguish pre-assessment pending vs committed

### Canonical F3 truth rule

A1 only allows：

    A-canonical

即：

    pre-D03 C03/F3 assessment
    -> creates/updates canonical governed F3 Gap state

禁止：

    pre-D03 "sufficiency score"
    + later independent canonical Gap truth

---

## 4.2 Candidate A2 — dedicated deterministic/non-C03 pre-readiness F3 Unit

### Semantic owner

    F3

不变。

### Execution host

新增明确 sub-unit / Unit candidate：

    U05-PRE-F3-ASSESSMENT

名称只是设计标识，不构成 Unit 编号冻结。

它位于：

    U04
    -> U05-PRE-F3-ASSESSMENT
    -> canonical F3 commit
    -> POST_F3_SAFETY_REVALIDATION_BARRIER
    -> U05

### Scheduler position

    after committed U04 ALLOW / permitted RESTRICTED
    before first U05 D03

### Trigger

    PRE_READINESS_F3_ASSESSMENT_REQUIRED

### Capability use

    C03 = NO

因此若选择 A2，必须另行定义：

    deterministic governed F3 assessment policy
    exact RuleReleaseRef / policy version
    evidence required to identify canonical Gap states

禁止：

    legacy completeness heuristic
    raw LLM sufficiency judgment

### Input

    current committed Clinical Facts
    current Clinical State Version
    current Safety restriction context
    approved deterministic F3 policy/rule refs

### Output

同 A1：

    canonical governed F3 Gap records
    Gap Decision Impact
    F3 readiness input

### State commit

    required

    K09 -> G2/P01
    -> Clinical State Version advances
    -> prior U04 Gate becomes non-routable for U05
    -> mandatory POST_F3_SAFETY_REVALIDATION_BARRIER
    -> current U04 Gate re-established
    -> current-version F3 readiness-input revalidation/ref-binding
    -> U05

A2 version mode：

    VS-B = STATE_MUTATION_WITH_POST_COMMIT_SAFETY_BARRIER

### Replay / idempotency

与 A1 同等级要求：

    exact input state version
    deterministic policy version
    F3_CANONICAL_EFFECT_ID
    effect idempotency identity
    no duplicate Gap truth

Termination：

    downstream U03/U04 Risk/Safety-only commits
    != F3 invalidation

    same fact/framing basis
    + same canonical F3 effect already applied
    -> barrier may revalidate/rebind F3 input
    -> MUST NOT create another F3 canonical commit

### Failure owner

    deterministic-policy failure / unavailable rule binding
    -> typed failure
    -> U14 eligibility
    -> no D03

### Main amendment consequences

A2 必改：

    U04-RDP-04
    - ALLOW / permitted RESTRICTED may expose PRE_READINESS_A2_F3_DETERMINISTIC_ELIGIBLE
    - U04 never directly executes the pre-F3 Unit
    - after F3 commit, old Gate/eligibility expires
    - post-barrier current Gate + current F3 completion ref may expose U05_ELIGIBLE

    Phase 4
    - F3 activation timing

    Phase 5
    - business-loop order

    Phase 6
    - introduce explicit pre-readiness execution host

    Phase 8
    - F3 deterministic policy / K09 contract

    Phase 9
    - Scheduler edge U04 -> pre-F3 Unit -> canonical F3 commit -> POST_F3_SAFETY_REVALIDATION_BARRIER -> U05
    - barrier re-establishes current U04 Gate and current-compatible F3 input
    - termination/idempotency prevents version-only F3 recommit

    RDP-05
    - applicability matrix

Phase 7 C03：

    no expected amendment

### Canonical F3 truth rule

A2 也只允许：

    A-canonical

Initial bootstrap 场景中不存在可供 A-derived 使用的 prior canonical F3 state。

因此：

    A-derived
    = may be valid only for later reevaluation scenarios
    = NOT a solution to initial bootstrap

---

# 5. Candidate B — positive minimum-analysis input outside F3

Candidate B 不再视为单一 amendment scope。

必须区分：

    B1 = F2/U02 semantic extension + explicit F2_SUFFICIENCY source_domain
    B2 = genuinely new readiness source / owner

两者均不构成批准。

---

## 5.1 Candidate B1 — F2/U02 semantic extension with explicit F2_SUFFICIENCY source

B1 不再声称“无需 source_domain amendment”。

Frozen RDP-05 当前只允许：

    F1
    F2_CLARIFICATION
    F3
    F5
    F6

且：

    F2_CLARIFICATION
    = only NEEDS_CLARIFICATION
    != general F2 readiness source

因此 B1 明确采用：

    new readiness source_domain candidate
    = F2_SUFFICIENCY

同时保持：

    F2_CLARIFICATION
    = unchanged / narrow clarification-only semantics

这意味着 B1 是：

    existing F2 business owner extension
    + explicit new F2-derived readiness source_domain
    + existing U02 execution Unit extension

而不是：

    reuse F2_CLARIFICATION for positive sufficiency

### Semantic owner

Existing:

    F2 = governed patient-fact formation

Proposed controlled extension:

    F2 also owns a distinct positive assertion:

    MINIMUM_ANALYSIS_CONDITION_SATISFIED

but only through:

    source_domain = F2_SUFFICIENCY

强制保持：

    F2_SUFFICIENCY
    != F2_CLARIFICATION

    F2_SUFFICIENCY
    != F3 gap/no-gap decision

    F2_SUFFICIENCY
    != final Clinical Readiness

最终 Clinical Readiness 仍只由 G2/U05 D03 产生。

### Execution host

    U02

但 B1 增加一个明确的 U02 execution mode:

    SUFFICIENCY_ASSESSMENT_ONLY

该 mode：

    does not parse new patient text
    does not call C01
    does not create/update patient facts
    does not modify framing
    only evaluates the governed F2_SUFFICIENCY policy
    against already committed current facts/framing

### Scheduler position

普通事实主链仍先执行：

    U02 FACT_FORMATION
    -> U03
    -> U04

B1 在 U04 已产生 committed/current ALLOW 或 permitted RESTRICTED 后增加：

    U04
    -> U02 SUFFICIENCY_ASSESSMENT_ONLY
    -> publish durable current-version F2 Sufficiency Decision / readiness-input ref
    -> U05 D03

因此不再尝试：

    U02-produced signal @ Vn
    -> blindly reuse at U05 Vn+k

而是：

    post-U04 current state
    -> U02 sufficiency-only assessment
    -> current-version governed decision/readiness-input ref
    -> U05

这同时解决 current-version revalidation hosting 问题，并避免通过额外 Clinical State commit 使当前 U04 Gate 立即 stale。

### Trigger

    F2_SUFFICIENCY_ASSESSMENT_REQUIRED

触发前置：

    current U04 committed Safety Gate = ALLOW
    or permitted RESTRICTED

    + current committed F1 framing
    + current committed patient facts
    + no current-version valid F2_SUFFICIENCY assertion
    + approved F2 sufficiency RuleRelease / PolicyRef

以下情况不得触发 ordinary B1 assessment：

    Safety Gate = BLOCKED
    Safety Gate = UNAVAILABLE
    stale/malformed U04 handoff
    missing required policy binding

### Policy / Capability usage

B1 V1 proposal:

    AI Capability = NONE

    C01 = NOT_CALLED in SUFFICIENCY_ASSESSMENT_ONLY

    F2 sufficiency assessment
    = deterministic governed policy

必须绑定：

    F2SufficiencyPolicyRef
    RuleReleaseRef
    applicable KnowledgeReleaseRef only if the frozen policy explicitly requires it

禁止：

    LLM decides sufficiency
    C01 model output -> direct sufficiency
    completeness percentage
    legacy required/important/optional checklist as truth

### Input contract

    consultation_id
    cdp_id
    current Clinical State Version
    current F1 framing ref
    current committed patient-fact refs
    committed U04 Safety Gate ref
    restricted_context_ref when applicable
    F2SufficiencyPolicyRef
    RuleReleaseRef
    required governance/version refs

### Output contract

Exactly one of:

    A. F2_SUFFICIENCY readiness input:
       business_signal = MINIMUM_ANALYSIS_CONDITION_SATISFIED

    B. no positive sufficiency assertion
       = no F2_SUFFICIENCY readiness input
       = NOT automatically insufficient / CAN_ASK_MORE / READY / NO_RELIABLE_DIRECTION

    C. typed technical/governance failure

B1 不允许输出：

    READY_FOR_CLINICAL_ANALYSIS
    CAN_ASK_MORE
    NO_RELIABLE_DIRECTION
    NEEDS_OFFLINE_EVIDENCE

这些仍属于各自 Owner / D03。

### Decision / persistence boundary

B1 adopts a version-safe non-state decision-ref pattern before D03.

Required sequence:

    current committed Clinical State Version = Vn
    + current committed U04 Safety Gate @ Vn
    -> U02 SUFFICIENCY_ASSESSMENT_ONLY
    -> deterministic F2 Sufficiency Decision @ Vn
    -> durable governed decision/readiness-input record
    -> U05 consumes F2_SUFFICIENCY readiness input @ Vn
    -> D03

The F2 Sufficiency Decision must conform to the Phase-8 Deterministic Decision semantics:

    decision_id
    decision_type = F2_MINIMUM_ANALYSIS_SUFFICIENCY
    consultation_id
    input_clinical_state_version = Vn
    decision
    reason_codes[]
    basis_refs[]
    policy_id / policy_version
    rule_release_refs[]
    knowledge_release_refs[] when applicable
    input_refs[]
    created_at
    validity / staleness

The normalized RDP-05 readiness input must bind:

    source_domain = F2_SUFFICIENCY
    source_owner = F2
    business_signal = MINIMUM_ANALYSIS_CONDITION_SATISFIED
    clinical_state_version = Vn
    source_decision_ref = F2 Sufficiency Decision ref
    source_state_ref = authoritative Clinical State / facts-framing ref @ Vn
    evidence_refs[]
    policy_or_rule_refs[]
    validity = CURRENT

Critical invariant:

    Deterministic Decision != StateChangeProposal

Therefore B1 pre-D03 sufficiency evaluation:

    DOES NOT create K09 StateChangeProposal
    DOES NOT call P01/G2 to mutate Clinical State
    DOES NOT advance Clinical State Version
    DOES NOT stale the current U04 Safety Gate merely by being evaluated

Durability means:

    decision/readiness-input artifact is persisted/auditable/replayable

It does not mean:

    new Clinical State truth
    new Clinical State Version

不得：

    keep only transient controller boolean
    pass an unbound/unaudited decision directly into D03
    project F2_SUFFICIENCY into Clinical State before D03

### Current-version validity and invalidation

B1 不使用“跨 U03/U04 版本兼容继承”作为默认机制。

Instead:

    current committed U04 Safety Gate @ Vn
    + F2 Sufficiency Decision @ Vn
    + F2_SUFFICIENCY readiness input @ Vn
    -> may coexist without changing Clinical State Version
    -> U05 may evaluate D03 against the same current version Vn

This removes the prior loop:

    U04 @ Vn
    -> sufficiency state commit
    -> Vn+1
    -> U04 stale
    -> re-run U04
    -> sufficiency commit again

because there is no pre-D03 sufficiency state commit.

Any later Clinical State Version change to Vn+1 makes the prior decision/input non-current unless explicitly revalidated under a frozen rule.

At minimum, changes to:

    F1 framing
    F2 patient facts
    correction affecting facts/framing
    any upstream state on which the sufficiency policy basis depends

must mark the prior F2 Sufficiency Decision / readiness input:

    STALE

and require:

    normal upstream reevaluation as applicable
    -> current U03
    -> current U04
    -> U02 SUFFICIENCY_ASSESSMENT_ONLY @ new current version
    -> U05

No content-equality shortcut is allowed:

    old decision @ Vn
    + new Clinical State Version Vn+1
    != CURRENT automatically

### Replay / idempotency

B1 assessment identity must bind at least:

    consultation_id
    source/current Clinical State Version
    U04 Safety Gate decision/commit ref
    F2SufficiencyPolicyRef
    RuleReleaseRef
    assessment trigger/event identity
    decision/readiness-input idempotency key

Same replay:

    must not create duplicate authoritative F2 Sufficiency Decision records
    must not create duplicate F2_SUFFICIENCY readiness-input records
    must attach/return the authoritative prior decision/input

If authoritative Clinical State Version changes before decision publication:

    result is stale-before-publish
    -> do not mark CURRENT
    -> scheduler reloads/re-evaluates at the new authoritative version

Because no Clinical State mutation occurs:

    duplicate decision replay
    != duplicate clinical effect

### Failure owner / route

Policy binding missing, deterministic policy failure, durable-decision persistence failure, or stale-before-publish conflict:

    != insufficient
    != no gap
    != READY

must produce typed failure semantics.

Where recoverable/retriable:

    follow governed retry/reload policy

Where business/runtime failure routing is required:

    -> U14 eligibility

No failure may fabricate:

    MINIMUM_ANALYSIS_CONDITION_SATISFIED

No failure may mutate Clinical State merely to record the failure.

### Main amendment consequences

B1 必改：

    U04-RDP-04
    - ALLOW / permitted RESTRICTED may expose PRE_READINESS_B1_F2_SUFFICIENCY_ELIGIBLE
    - U04 never directly executes U02 sufficiency-only mode
    - VS-A preserves the current Gate/version
    - current Gate + current F2_SUFFICIENCY decision/input may expose U05_ELIGIBLE

    Phase 4
    - F2 business semantics expanded
    - add explicit F2_SUFFICIENCY readiness source
    - preserve F2_CLARIFICATION as clarification-only

    Phase 5
    - add post-Safety positive sufficiency assessment before first Readiness

    Phase 6
    - U02 gains SUFFICIENCY_ASSESSMENT_ONLY entry/mode
    - U05 S_in accepts F2_SUFFICIENCY
    - initial path becomes U04 -> U02(sufficiency-only) -> U05

    Phase 7
    - CONTROLLED AMENDMENT REQUIRED
    - U02 dependency semantics become mode-aware:
      FACT_FORMATION -> C01
      SUFFICIENCY_ASSESSMENT_ONLY -> no Clinical AI Capability
    - current unit-level row "U02 | C01" may remain only if explicitly documented as aggregate capability dependency;
      otherwise the matrix/semantic chain must be amended to represent per-mode dependency
    - no C03 change expected
    - no C01 invocation in sufficiency-only mode

    Phase 8
    - add F2_SUFFICIENCY readiness input schema
    - add F2_MINIMUM_ANALYSIS_SUFFICIENCY deterministic decision contract
    - explicitly preserve Deterministic Decision != StateChangeProposal
    - no pre-D03 K09/P01 Clinical State mutation for B1
    - add durable decision/readiness-input provenance, validity and idempotency contract

    Phase 9
    - Scheduler edge U04 -> U02(SUFFICIENCY_ASSESSMENT_ONLY) -> U05
    - distinguish U02 FACT_FORMATION from U02 SUFFICIENCY_ASSESSMENT_ONLY
    - U02 sufficiency-only publishes a durable decision/input artifact, not Clinical State mutation
    - add replay/stale/reload behavior

    RDP-05
    - source_domain adds F2_SUFFICIENCY
    - business_signal adds MINIMUM_ANALYSIS_CONDITION_SATISFIED
    - applicability/version/invalidation matrix updated
    - F2_CLARIFICATION remains narrow and unchanged

    RDP-02
    - D03 policy may consume F2_SUFFICIENCY only after Owner-approved READY policy is separately frozen

### Principal governance boundary

B1 must prove in detailed amendment:

    F2_SUFFICIENCY
    = positive minimum-condition assertion over governed facts/framing

    F3
    = what information is missing, whether online-obtainable,
      and whether it has decision value

Therefore:

    F2_SUFFICIENCY
    must not claim "no important gap exists"

and:

    F3
    must not be bypassed after DDx when new Gap reevaluation is required

If this separation cannot be maintained:

    B1 must be rejected
    rather than creating two competing sufficiency owners.

### B1 decision-package completeness

For Owner-selection comparison, B1 now has:

    semantic owner = F2 extension
    source_domain = F2_SUFFICIENCY
    execution host = U02
    execution mode = SUFFICIENCY_ASSESSMENT_ONLY
    Scheduler = U04 -> U02(sufficiency-only) -> U05
    trigger = F2_SUFFICIENCY_ASSESSMENT_REQUIRED
    Capability = NONE in V1
    policy = deterministic governed policy
    state mutation = NONE before D03
    durable artifact = deterministic decision + normalized readiness-input ref
    idempotency = source-version + U04 Gate ref + policy + trigger/decision identity
    failure owner = typed failure / U14 eligibility
    revalidation = recompute after current U04, not cross-version inheritance
    version safety = current U04 Gate and F2_SUFFICIENCY remain bound to the same Clinical State Version


---

## 5.2 Candidate B2 — new positive-sufficiency Owner + execution Unit

### Semantic owner

新增候选业务 Owner：

    Minimum Analysis Sufficiency Owner

名称只用于设计，不表示已批准新增模块。

### Execution host

新增候选 execution Unit：

    U05-PRE-SUFFICIENCY

位于：

    U04
    -> U05-PRE-SUFFICIENCY
    -> U05

### Scheduler position

    after admitted committed U04 Safety
    before first D03

### Trigger

    MINIMUM_ANALYSIS_SUFFICIENCY_ASSESSMENT_REQUIRED

### Capability use

默认：

    no AI Capability authorized

若后续设计需要 Capability：

    must create separate capability/governance design
    cannot inherit C03 authority implicitly

### Input

    current committed Facts
    current framing
    current Clinical State Version
    Safety restriction context
    approved sufficiency policy/rule refs

### Output

新的 readiness input domain candidate：

    MINIMUM_ANALYSIS_CONDITION_SATISFIED
    or typed non-business failure

不得直接输出：

    READY_FOR_CLINICAL_ANALYSIS

### Decision / persistence boundary

B2 decision-package level now selects the version-safe pattern：

    VS-A = SAME_VERSION_NON_STATE_DECISION

Required sequence：

    current U04 Gate @ Vn
    -> U05-PRE-SUFFICIENCY
    -> deterministic Minimum Analysis Sufficiency Decision @ Vn
    -> durable governed readiness-input ref @ Vn
    -> U05 D03 @ Vn

Before D03：

    NO K09 StateChangeProposal
    NO G2/P01 Clinical State mutation
    NO Clinical State Version advancement

The decision/input must bind：

    consultation_id
    input_clinical_state_version = Vn
    current U04 Gate ref @ Vn
    sufficiency policy/rule refs
    exact input/evidence refs
    source_decision_ref
    validity/staleness
    replay/idempotency identity

Any later Clinical State Version change：

    -> prior B2 decision/input = STALE
    -> recompute/revalidate under a frozen rule before reuse

B2 no longer keeps an ambiguous pre-D03 state-commit branch.

### Replay / idempotency

必须绑定：

    consultation
    current state version
    current U04 Gate ref
    sufficiency policy version
    input refs
    decision idempotency identity

Same replay：

    returns/attaches authoritative prior decision/input
    -> no duplicate readiness-input artifact
    -> no duplicate Clinical State effect because no state mutation occurs

### Failure owner

    typed failure
    -> U14 eligibility where appropriate
    -> no D03 fake result

### Main amendment consequences

B2 必改：

    U04-RDP-04
    - ALLOW / permitted RESTRICTED may expose PRE_READINESS_B2_MINIMUM_SUFFICIENCY_ELIGIBLE
    - U04 never directly executes U05-PRE-SUFFICIENCY
    - VS-A preserves the current Gate/version
    - current Gate + current B2 decision/input may expose U05_ELIGIBLE

    Phase 4
    - readiness input-source / Owner model

    Phase 5
    - business loop

    Phase 6
    - new execution Unit and U05 S_in

    Phase 8
    - new deterministic decision/readiness-input contract
    - explicitly preserve Deterministic Decision != StateChangeProposal
    - no pre-D03 Clinical State mutation in B2

    Phase 9
    - Scheduler edge U04 -> U05-PRE-SUFFICIENCY -> U05
    - same-version U04 Gate + B2 decision/input
    - stale/recompute behavior after any later state-version advance

    RDP-05
    - source_domain / applicability / version contract

B2 不允许复用 F3 的语义名称来规避 source-domain amendment。

---

# 6. Candidate comparison for Owner discussion

本表只呈现结构差异，不构成推荐、排序或批准。

| Dimension | A1 U06+C03 canonical F3 | A2 Dedicated non-C03 F3 Unit | B1 F2/U02 + F2_SUFFICIENCY | B2 New sufficiency Owner/Unit |
|---|---|---|---|---|
| Clinical sufficiency semantic owner | F3 | F3 | F2 extension | new owner |
| Execution host | U06 | new pre-F3 Unit | U02 | new pre-sufficiency Unit |
| Initial Scheduler change | U04→U06(pre)→U05 | U04→new Unit→U05 | U04→U02(sufficiency-only)→U05 | U04→new Unit→U05 |
| U04 pre-readiness eligibility | A1 typed eligibility; post-F3 new Gate required | A2 typed eligibility; post-F3 new Gate required | B1 typed eligibility; same-version transition | B2 typed eligibility; same-version transition |
| Uses C03 | yes | no | no | no by default |
| Produces canonical F3 Gap | yes | yes | no | no |
| New readiness source domain | no | no | yes: F2_SUFFICIENCY while F2_CLARIFICATION stays narrow | yes |
| Phase 5 amendment | yes | yes | yes | yes |
| Phase 9 amendment | yes | yes | yes: mode-aware scheduler + durable decision/input publication | yes |
| Duplicate sufficiency-owner concern | low if canonical F3 only | low if canonical F3 only | must resolve F2 vs F3 boundary | must resolve new owner vs F3 boundary |
| Current-version complexity | canonical F3 commit + mandatory Safety barrier/revalidation | canonical F3 commit + mandatory Safety barrier/revalidation | same-version Gate + non-state decision/input | same-version Gate + non-state decision/input |
| New deterministic clinical rule pack | not necessarily; C03 governed capability | yes | yes for positive sufficiency semantics | yes |
| Direct U05 Gap ownership | prohibited | prohibited | prohibited | prohibited |

---

# 7. Canonical F3 lifecycle rule

本节专门关闭 parallel Gap truth 风险。

Phase 4 canonical F3 lifecycle remains：

    IDENTIFIED
    QUESTIONABLE_ONLINE
    ASKED
    ANSWERED
    USER_UNKNOWN
    UNMEASURED
    OFFLINE_REQUIRED
    WAIVED
    RESOLVED
    INVALIDATED

对 initial bootstrap：

    Candidate A1/A2
    -> must use A-canonical semantics

即：

    pre-D03 F3 assessment
    -> create/update canonical governed F3 Gap state
    -> commit
    -> derive readiness input from canonical F3 state
    -> D03

禁止：

    pre-D03 independent sufficiency result
    + later independent U06/U09 canonical Gap truth

A-derived：

    assessment derived only from already-canonical F3 Gap state

只允许用于：

    later reevaluation where canonical F3 state already exists

它不能解决 initial bootstrap，因此不是 OD-U05-BOOTSTRAP-01 的 initial-path候选。

---

# 8. Separate governance decisions

Bootstrap architecture 与 D03 READY policy 必须完全分离。

## 8.1 Bootstrap architecture decision

只有 targeted re-review PASS 后，才允许：

    OD-U05-BOOTSTRAP-01

Owner 必须选择 exact candidate/subcandidate：

    A1
    A2
    B1
    B2
    REJECT_ALL_AND_REDESIGN

不再允许粗粒度：

    A / B

因为 execution host / Scheduler / contract impact 不同。

Owner selection 只表示：

    detailed amendment design authorized for selected candidate

它不表示：

    frozen artifacts already amended
    design re-review passed
    implementation authorized

## 8.2 D03 positive READY policy decision

独立决策：

    OD-U05-READY-01

其批准对象必须绑定：

    exact post-amendment readiness input model
    exact D03 policy version
    exact accepted positive evidence set

当前 proposal：

    F1 = FRAMED_IN_SCOPE
    + current F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
    + no higher-priority blocker
    + all required current inputs valid
    -> READY_FOR_CLINICAL_ANALYSIS

当前状态：

    NOT_APPROVED

即使 Owner 选择 A1/A2：

    != OD-U05-READY-01 approved

即使 Owner 选择 B1/B2：

    READY policy must be re-specified against that candidate's exact positive input model

因此 Bootstrap Decision 与 READY Decision 可以连续讨论，但不得互相隐式授权。

---

# 9. Required detailed-design fields after Owner selection

任何被选 candidate 的下一版 detailed amendment 必须逐项给出：

    semantic owner
    execution host Unit
    Scheduler predecessor/successor
    U04-RDP-04 candidate-specific eligibility type / projection
    trigger
    admission criteria
    input contract
    output contract
    canonical state affected
    K09/G2/P01 requirement
    Capability invocation and binding
    RuleRelease / KnowledgeRelease requirements
    pre-readiness version mode = VS-A / VS-B
    Clinical State Version binding
    Safety Gate current-version barrier/revalidation when VS-B
    invalidation propagation
    termination/no-cycle proof when VS-B
    replay/idempotency identity
    failure owner / U14 eligibility
    trace/audit refs
    exact upstream frozen artifacts changed
    exact re-review plan
    regression/eval consequences

缺任何一项：

    amendment design != implementation-ready

---

# 10. Required amendment sequence

正确顺序：

    1. Bootstrap package targeted independent re-review
    2. PASS -> package becomes OWNER_SELECTION_READY
    3. Owner selects exact A1/A2/B1/B2 or REJECT_ALL
    4. Produce detailed amendment for selected candidate only
    5. Produce exact frozen-artifact diff inventory
    6. Independent design review of amendment
    7. Explicit authorization to amend affected frozen artifacts
    8. Amend only authorized artifacts
    9. Independent re-review every modified frozen artifact
    10. Re-freeze each modified artifact at exact head
    11. Reconcile RDP-05 / RDP-02 against new baseline
    12. Perform RDP-02 targeted independent re-review
    13. Separately resolve OD-U05-READY-01 against exact post-amendment model
    14. Only then consider BF-U05-RG-02 CLOSED
    15. U05 Implementation Readiness review remains separate

禁止：

    Owner selects candidate -> directly implement runtime

禁止：

    modify frozen artifact -> retain previous PASS/FROZEN status

禁止：

    bootstrap candidate selected -> infer READY policy approval

---

# 11. Review-finding status

Independent / targeted review history current truth:

    BF-U05-BOOTSTRAP-IR-01 = CLOSED
    BF-U05-BOOTSTRAP-IR-02 = CLOSED
    BF-U05-BOOTSTRAP-IR-03 = CLOSED
    BF-U05-BOOTSTRAP-IR-04 = CLOSED
    RQ-U05-BOOTSTRAP-IR-05 = CLOSED

    BF-U05-BOOTSTRAP-TR-01 = CLOSED
    BF-U05-BOOTSTRAP-TR-02 = CLOSED
    BF-U05-BOOTSTRAP-TR-03 = CLOSED
    RQ-U05-BOOTSTRAP-TR-04 = CLOSED
    BF-U05-BOOTSTRAP-TR-05 = CLOSED

Current open review item:

    BF-U05-BOOTSTRAP-TR-06
    = REMEDIATED / FIFTH_TARGETED_REVIEW_PENDING

TR-06 remediation summary:

    U04-RDP-04 Downstream Routing Boundary
    = added to exact frozen-artifact impact inventory for A1/A2/B1/B2

    Routing projection now distinguishes:
    current Gate
    + active BootstrapArchitectureBindingRef
    + current bootstrap effect/completion ref
    -> candidate-specific PRE_READINESS_ELIGIBILITY
       or U05_ELIGIBLE

    ALLOW
    -> selected candidate pre-readiness eligibility when bootstrap effect required

    RESTRICTED
    -> only candidate-specific eligibility explicitly permitted by governed restricted policy

    BLOCKED / UNAVAILABLE
    -> no ordinary/pre-readiness eligibility

    VS-A
    -> same current Gate may transition from pre-readiness eligibility to U05 eligibility

    VS-B
    -> pre-F3 Gate/eligibility expires after canonical F3 commit;
       post-barrier current Gate + current F3 completion ref may expose U05 eligibility
       without retriggering the same canonical F3 effect

    U04 remains Gate owner only and never directly executes downstream Unit/Capability.

No review item in this section grants Owner selection or upstream amendment authorization.

---

# 12. Current disposition

    U05_BOOTSTRAP_CROSS_PHASE_DESIGN_GAP
    = OPEN

    Controlled Amendment Decision Package
    = REVISED / READY_FOR_FIFTH_TARGETED_INDEPENDENT_REVIEW

    Controlled Amendment Decision Package
    != OWNER_SELECTION_READY yet

    OD-U05-BOOTSTRAP-01
    = NOT_READY_FOR_DECISION until targeted review PASS

    OD-U05-READY-01
    = SEPARATE / NOT_APPROVED

    Upstream amendment
    = NOT_AUTHORIZED

    BF-U05-RDP02-IR-02
    = REMEDIATED_WITH_REVISED_CONTROLLED_AMENDMENT_PACKAGE
    = FIFTH_TARGETED_REVIEW_PENDING

    BF-U05-RG-02
    = NOT_CLOSED

---

# 13. Authorization boundary

This document does not authorize:

    selection of A1/A2/B1/B2 before targeted review PASS
    modification of frozen U04-RDP-04 Downstream Routing Boundary
    modification of frozen Phase 4/5/6/7/8/9 semantics
    modification of frozen U05-RDP-05
    pre-D03 F3 execution
    C03 pre-readiness invocation
    new Unit creation
    new readiness input producer
    new F2 sufficiency ownership
    OD-U05-READY-01 approval
    U05 implementation
    U04->U05 live routing
    downstream owner execution
    production Clinical State mutation
    production Clinical Runtime
    release activation
    real-patient traffic
