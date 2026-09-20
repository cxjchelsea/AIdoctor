# U05 RDP-02 Bootstrap Controlled Amendment Decision v0.1 (Revised)

> Scope: 解决 U05/D03 首轮 Clinical Readiness bootstrap underdetermination 的受控设计修订决策包。
>
> Status: REVISED / READY_FOR_TARGETED_INDEPENDENT_REVIEW / OWNER_SELECTION_NOT_YET_AUTHORIZED
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
| Phase 5 Safety→Readiness→F3 business loop | YES | YES | YES | YES |
| Phase 6 U05 S_in / Trigger | YES | YES | YES | YES |
| Phase 6 U06 S_in / Action | YES | NO_EXPECTED | NO_EXPECTED | NO_EXPECTED |
| Phase 7 C03 FIRST_CONSUMER_UNIT | NO_EXPECTED: still U06 | NO | NO | NO |
| Phase 7 C03 capability usage semantics | YES: assessment before question delivery | NO | NO | NO |
| Phase 8 readiness input / K09 contracts | YES | YES | YES | YES |
| Phase 9 Unit Scheduler / transition graph | YES | YES | CONDITIONAL | YES |
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

    U04 ALLOW / permitted RESTRICTED
    -> U06 PRE_READINESS_GAP_ASSESSMENT
    -> canonical F3 Gap commit
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
    -> U05 consumes only committed/current F3 state

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

### Failure owner

Capability/runtime failure：

    -> typed failure contract
    -> U14 eligibility
    -> no fake NO_ACTIVE_ONLINE_BLOCKING_GAP
    -> no D03 execution from missing F3 result

### Main amendment consequences

A1 必改：

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
    - Scheduler edge U04 -> U06(pre) -> U05
    - U06(pre) != U06(question delivery)

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
    K09 -> G2/P01 -> current version

### Replay / idempotency

与 A1 同等级要求：

    exact input state version
    deterministic policy version
    effect idempotency identity
    no duplicate Gap truth

### Failure owner

    deterministic-policy failure / unavailable rule binding
    -> typed failure
    -> U14 eligibility
    -> no D03

### Main amendment consequences

A2 必改：

    Phase 4
    - F3 activation timing

    Phase 5
    - business-loop order

    Phase 6
    - introduce explicit pre-readiness execution host

    Phase 8
    - F3 deterministic policy / K09 contract

    Phase 9
    - new Scheduler node/edge

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
    -> commit current F2_SUFFICIENCY assertion
    -> U05 D03

因此不再尝试：

    U02-produced signal @ Vn
    -> blindly reuse at U05 Vn+k

而是：

    post-U04 current state
    -> U02 sufficiency-only assessment
    -> current-version governed assertion
    -> U05

这同时解决 initial bootstrap 和 U03/U04 后的 current-version revalidation hosting 问题。

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

### State / commit boundary

Positive signal 必须成为 governed current-version derived assertion/readiness input。

Required sequence:

    deterministic F2 sufficiency decision
    -> K09 StateChangeProposal
    -> G2/P01 commit
    -> new current Clinical State Version
    -> reload authoritative state
    -> U05 consumes committed F2_SUFFICIENCY

不得：

    keep only transient controller boolean
    pass uncommitted signal directly into D03

### Current-version validity and invalidation

B1 不使用“跨 U03/U04 版本兼容继承”作为默认机制。

相反：

    every ordinary initial/re-entry path reaching U05 after U04
    must have a current-version F2_SUFFICIENCY assessment

After F2_SUFFICIENCY commit:

    pure downstream U05 decision may consume it

Any later change to:

    F1 framing
    F2 patient facts
    correction affecting facts/framing

must invalidate:

    F2_SUFFICIENCY

and require the normal chain:

    U02 FACT_FORMATION
    -> U03
    -> U04
    -> U02 SUFFICIENCY_ASSESSMENT_ONLY
    -> U05

U03/U04 commits happen before the sufficiency assessment, so B1 no longer depends on preserving an earlier U02 signal across those commits.

### Replay / idempotency

B1 assessment identity must bind at least:

    consultation_id
    source/current Clinical State Version
    U04 Safety Gate decision/commit ref
    F2SufficiencyPolicyRef
    RuleReleaseRef
    assessment trigger/event identity
    effect idempotency key

Same replay:

    must not create duplicate F2_SUFFICIENCY assertions
    must not produce multiple commits for the same source version/policy
    must attach/return the authoritative prior effect

If authoritative Clinical State Version changes before commit:

    stale conflict
    -> no commit
    -> scheduler must reload/re-evaluate

### Failure owner / route

Policy binding missing, deterministic policy failure, commit failure, or stale conflict:

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

### Main amendment consequences

B1 必改：

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
    - no C03 change expected
    - no C01 invocation in sufficiency-only mode
    - capability matrix may require clarification that U02 has a deterministic non-Capability consequence

    Phase 8
    - add F2_SUFFICIENCY readiness input schema
    - add deterministic sufficiency decision/proposal contract
    - add K09/P01 field/source permission and idempotency contract

    Phase 9
    - Scheduler edge U04 -> U02(SUFFICIENCY_ASSESSMENT_ONLY) -> U05
    - distinguish U02 FACT_FORMATION from U02 SUFFICIENCY_ASSESSMENT_ONLY
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
    state = committed governed derived assertion
    idempotency = source-version + policy + trigger/effect identity
    failure owner = typed failure / U14 eligibility
    revalidation = recompute after current U04, not cross-version inheritance


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

### State commit

如果 output 成为 governed derived state：

    K09/G2/P01 contract required

如果仅是 deterministic decision input ref：

    exact durable decision/provenance contract still required

详细 amendment 必须二选一并冻结，不得模糊。

### Replay / idempotency

必须绑定：

    consultation
    current state version
    sufficiency policy version
    input refs
    effect/decision idempotency identity

### Failure owner

    typed failure
    -> U14 eligibility where appropriate
    -> no D03 fake result

### Main amendment consequences

B2 必改：

    Phase 4
    - readiness input-source / Owner model

    Phase 5
    - business loop

    Phase 6
    - new execution Unit and U05 S_in

    Phase 8
    - new input/decision/state contract

    Phase 9
    - Scheduler node/edge/dependency

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
| Uses C03 | yes | no | no | no by default |
| Produces canonical F3 Gap | yes | yes | no | no |
| New readiness source domain | no | no | yes: F2_SUFFICIENCY while F2_CLARIFICATION stays narrow | yes |
| Phase 5 amendment | yes | yes | yes | yes |
| Phase 9 amendment | yes | yes | yes/revalidation | yes |
| Duplicate sufficiency-owner concern | low if canonical F3 only | low if canonical F3 only | must resolve F2 vs F3 boundary | must resolve new owner vs F3 boundary |
| Current-version complexity | canonical commit before D03 | canonical commit before D03 | recompute after current U04, then commit before D03 | committed/decision binding before D03 |
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
    trigger
    admission criteria
    input contract
    output contract
    canonical state affected
    K09/G2/P01 requirement
    Capability invocation and binding
    RuleRelease / KnowledgeRelease requirements
    Clinical State Version binding
    invalidation propagation
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

# 11. Review-finding remediation status

    BF-U05-BOOTSTRAP-IR-01
    = REMEDIATED / TARGETED_REVIEW_PENDING

Reason：

    Phase 4/5/6/7/8/9 + RDP-05/RDP-02 impact matrix added.

    BF-U05-BOOTSTRAP-IR-02
    = REMEDIATED / TARGETED_REVIEW_PENDING

Reason：

    A1/A2/B1/B2 each define semantic owner, execution host, Scheduler position, trigger, contracts, commit/capability/idempotency/failure boundary.

    BF-U05-BOOTSTRAP-IR-03
    = REMEDIATED / TARGETED_REVIEW_PENDING

Reason：

    Candidate B split into B1 existing F2 Owner/U02 extension with explicit F2_SUFFICIENCY source_domain and B2 new-source-domain/new-owner.

    BF-U05-BOOTSTRAP-IR-04
    = REMEDIATED / TARGETED_REVIEW_PENDING

Reason：

    initial Candidate A requires one canonical F3 lifecycle; standalone sufficiency side-channel prohibited.

    RQ-U05-BOOTSTRAP-IR-05
    = REMEDIATED / TARGETED_REVIEW_PENDING

    BF-U05-BOOTSTRAP-TR-01
    = REMEDIATED / TARGETED_REVIEW_PENDING

Reason：

    B1 no longer reuses F2_CLARIFICATION or claims no source-domain change.
    It explicitly adds F2_SUFFICIENCY and keeps F2_CLARIFICATION narrow.

    BF-U05-BOOTSTRAP-TR-02
    = REMEDIATED / TARGETED_REVIEW_PENDING

Reason：

    B1 now defines production/revalidation as U02 SUFFICIENCY_ASSESSMENT_ONLY after current U04,
    with explicit trigger, deterministic policy/no-Capability boundary, K09/P01 commit,
    version/invalidation rules, replay/idempotency identity and typed failure/U14 boundary.

Reason：

    OD-U05-BOOTSTRAP-* and OD-U05-READY-01 are explicitly separate and independently authorized.

---

# 12. Current disposition

    U05_BOOTSTRAP_CROSS_PHASE_DESIGN_GAP
    = OPEN

    Controlled Amendment Decision Package
    = REVISED / READY_FOR_SECOND_TARGETED_INDEPENDENT_REVIEW

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
    = SECOND_TARGETED_REVIEW_PENDING

    BF-U05-RG-02
    = NOT_CLOSED

---

# 13. Authorization boundary

This document does not authorize:

    selection of A1/A2/B1/B2 before targeted review PASS
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
