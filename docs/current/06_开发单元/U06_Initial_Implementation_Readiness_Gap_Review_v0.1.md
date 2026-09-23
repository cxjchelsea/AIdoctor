# U06 Initial Implementation Readiness / Gap Review v0.1

> Review target: **U06 — F3 Gap评估 / 版本重验证 / 关键问题选择与进入 WAITING_USER**  
> Unit Spec: `U06_Unit_Spec_v0.1.md`  
> Reviewed Unit-Spec status head: `ae332b4a8fc5e179e352a68e8c4762117340c6c5`  
> Runtime repository basis: `main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8`  
> Scope: **U06 V1 / NON_PRODUCTION_READINESS_REVIEW_ONLY**  
> 本文件只判定实施 readiness，不授予 code implementation、merge、production、live delivery、release activation 或 real-patient authorization。

---

# 1. Verdict

```
U06 Unit Definition
= PASS / SUFFICIENT_FOR_INITIAL_IMPLEMENTATION_READINESS_GAP_REVIEW

U06 Business-Semantic Baseline
= AVAILABLE / A1 REFROZEN V1

U06 Implementation Readiness
= NOT_READY

Open blocking readiness findings
= 6

U06 Implementation Authorization Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
```

当前 blocker 主要属于：

```
mode-specific admission / upstream execution contract
F3/D04 executable owner-policy contract
state mutation / Question lifecycle / trace contract
delivery / wait / side-effect contract
C03/P06/release/quality dependency readiness
verification / durable evidence design
```

不是要求工程人员重新发明临床真值。

---

# 2. Frozen U06 role

U06 当前只允许三个 governed mode：

```
MODE-1 PRE_READINESS_GAP_ASSESSMENT
MODE-2 QUESTION_SELECTION_DELIVERY
MODE-3 F3_CURRENT_VERSION_REVALIDATION
```

必须保持：

```
C03 Result
!= canonical F3 truth
!= Question truth
!= WAITING_USER

Question SELECTED
!= DELIVERED_TO_USER

DELIVERED_TO_USER
→ only then may Consultation WAITING_USER be committed
```

MODE-specific mutation boundary already reviewed:

```
MODE-1
→ canonical F3 proposal/commit allowed

MODE-2
→ governed Question lifecycle + delivery/wait boundary

MODE-3
→ NO StateChangeProposal
→ NO canonical F3 mutation
→ NO Clinical State Version advance
```

---

# 3. Available prerequisites

## 3.1 Upstream U05

U05 is now:

```
IMPLEMENTED
VERIFIED
RDP-06 PASS
MERGED TO main
REPOSITORY INTEGRATION COMPLETE
```

For ordinary question routing, U05 has a typed consequence:

```
NEEDS_CLARIFICATION / CAN_ASK_MORE
→ TO_U06_QUESTION_PATH
→ target_unit_id = U06
```

Current U05 does not invoke U06.

This is the correct boundary.

## 3.2 P01 / G2 StateCommitter

Current main contains the governed mechanical P01 foundation:

```
StateCommitter
StatePatch
CommitResult
idempotency
version conflict
field permission
source validation
audit
atomic repository boundary
```

It is reusable foundation.

But:

```
generic StateCommitter exists
!= U06 F3/Question/WAITING mutation contract exists
```

## 3.3 Canonical Effect Ledger

Current main contains:

```
CanonicalEffectLedger
CanonicalEffectLedgerDecision
CanonicalEffectLedgerRecord
NonProductionFileCanonicalEffectLedger
```

This may support U06 stable effect/replay evidence.

It does not by itself define:
- F3 effect identity acceptance;
- Question effect identity;
- delivery idempotency;
- WAITING transition identity.

## 3.4 P05 Trace baseline

Current main contains Foundation-1 capability call trace:

```
CapabilityTraceService
CapabilityCallTraceRecord
```

It can correlate:

```
consultation/thread/run/event/unit
capability/binding
capability result
decision
proposal
commit
state version before/after
failure reason
```

The current minimal record has single:

```
rule_release_ref
knowledge_release_ref
```

and no first-class PromptReleaseRef / ModelRouteRef fields.

Therefore it is reusable baseline, not a complete U06 trace contract.

## 3.5 P06 minimal Capability Binding

Current main contains:

```
CapabilityBindingRecord
CapabilityBindingRegistry
BindingReleaseResolver
CapabilityInvocationGuard
```

Implemented binding checks include:

```
capability id/version
capability set version
scope version
contract version
population
region
language
channel
ACTIVE/DISABLED/EXPIRED
effective window
```

This is reusable.

However Foundation-1 explicitly deferred:
- full KnowledgeRelease registry;
- full RuleRelease registry;
- complete P06 package lifecycle;
- C02-C06 pre-registration.

The current CapabilityBindingRecord also does not physically bind the Phase-8 full allowlist/ref set such as:
- allowed PromptReleaseRefs;
- allowed ModelRouteRefs;
- allowed KnowledgeReleaseRefs;
- allowed RuleReleaseRefs;
- allowed Tool/Skill refs.

Therefore P06 is **minimal foundation**, not a proven U06/C03-ready binding surface.

---

# 4. Implementation archaeology

## 4.1 No U06 runtime implementation

On current main:

```
diagnosis-service/src/main/**/runtime/u06
= ABSENT

U06-specific runtime classes
= ABSENT
```

## 4.2 No C03 runtime implementation

On current main:

```
src/main path containing C03
= ABSENT
```

Legacy/question assets exist, but are not a governed C03 implementation.

Examples:

```
AdaptiveQuestioningService
→ TODO
→ hard-coded “您这个症状出现多久了？”

InformationGapIdentifier
→ required / important / optional fixed heuristics

dialog-service AdaptiveQuestioningStrategy
→ fixed field priority
→ completeness >= 0.7 stopping
→ last-3-message keyword duplicate detection
```

These violate or under-specify current frozen semantics such as:
- decision impact;
- F1 vs F3 ownership;
- D04 governed stopping;
- USER_UNKNOWN / UNMEASURED semantics;
- current Clinical State binding;
- CapabilityBindingRef;
- proposal/commit boundary;
- authoritative delivery/wait lifecycle.

Therefore:

```
legacy question code
= REFERENCE / REFACTOR ASSET

legacy question code
!= C03 authority
!= D04 authority
!= U06 implementation
```

## 4.3 No D04 runtime implementation

On current main:

```
src/main path containing D04
= ABSENT
```

No executable governed Question Stopping resolver exists.

## 4.4 Question capability package is not active

Current:

```
capabilities/adult_respiratory_v1
lifecycle = DRAFT
clinical_review_status = REQUIRES_CLINICAL_REVIEW
runtime_adoption = NOT_IMPLEMENTED
production_eligibility = BLOCKED
```

Question assets:

```
mandatory.yaml
discriminators.yaml
stopping_rules.yaml
```

all have:

```
review_status = REQUIRES_CLINICAL_REVIEW
questions = []
prohibited_runtime_use = true
```

The only question eval case is structural:

```
question-structure-001
clinical_review_status = REQUIRES_CLINICAL_REVIEW
blocked_assertions:
  QUESTION_CLINICAL_VALUE
  PATIENT_WORDING_SAFETY
```

Therefore:

```
question capability skeleton exists
!= C03 Quality Gate passed
!= D04 policy approved
!= patient-facing question content approved
```

## 4.5 No formal Scheduler implementation

On current main:

```
src/main Scheduler path
= ABSENT
```

Current U04/U05 non-production application services explicitly stop before downstream owner execution.

## 4.6 A1 MODE-1 / MODE-3 runtime source objects are not implemented

Frozen Phase 8/9 defines:

```
A1PreReadinessEligibility
PRE_READINESS_A1_F3_C03_ELIGIBLE
F3CurrentVersionRevalidationDecision
ClinicalContinuationRoutingDecision
POST_F3_SAFETY_REVALIDATION_BARRIER
RISK_REEVALUATION_REQUIRED
```

but current `src/main` has no corresponding implementation surface.

Current U04 code still emits only:

```
u05Eligible
u11Eligible
u14Eligible
restrictedContextRequired
```

Therefore the A1 U06 MODE-1 entry path is semantically frozen but not executable.

Likewise MODE-3 continuation/revalidation entry is not executable.

## 4.7 No durable Question delivery infrastructure

Current diagnosis-service `src/main` has no identifiable:

```
DeliveryIntent
DeliveryReceipt
DeliveryOutbox
DeliveryLedger
PendingQuestion runtime/store
```

that implements the Phase-9 ordering:

```
Question SELECTED
→ durable delivery intent
→ stable delivery identity
→ transport
→ durable receipt
→ delivery confirmation
→ DELIVERED_TO_USER + WAITING_USER
```

Existing Question DTOs / legacy dialog responses do not satisfy this requirement.

---

# 5. Blocking readiness findings

## BF-U06-RG-01 — Three-mode inbound / admission and upstream execution contract missing

U06 supports three modes, but the executable inbound contract does not exist.

Must freeze:

```
MODE-1 legal source
= A1PreReadinessEligibility / current U04 routing authorization

MODE-2 legal sources
= U05 TO_U06_QUESTION_PATH
+ exact lawful F1 clarification topology

MODE-3 legal sources
= POST_F3_SAFETY_BARRIER_CURRENT_GATE_READY
  and/or governed TO_F3_CURRENT_VERSION_REVALIDATION consequence
```

Must bind:
- consultation/CDP;
- current Clinical State Version;
- current U04 Gate;
- route/eligibility/continuation identity;
- RESTRICTED permission;
- mode;
- event/correlation/trace;
- currentness;
- replay/admission identity.

Current runtime also lacks executable upstream surfaces for MODE-1 and MODE-3:
- U04 does not emit A1 pre-readiness U06 eligibility;
- no generic Scheduler consumes it;
- no MODE-3 continuation object exists.

The review must not silently solve this by allowing callers to invoke any U06 mode directly.

```
BF-U06-RG-01
= OPEN / BLOCKING

Required
= U06-RDP-01 Consumer Inbound / Admission Contract
  + exact upstream amendment impact inventory
```

The RDP must determine whether required upstream implementation is:
- U04-local amendment;
- shared Scheduler/runtime amendment;
- U09/continuation amendment;
- or a bounded non-production test seam.

No such implementation is authorized yet.

---

## BF-U06-RG-02 — F3 Owner / D04 executable policy contract missing

High-level business semantics are frozen, but there is no executable unit-specific contract for:

```
F1 minimal clarification
F3 Information Gap interpretation
Decision Impact
askable-online qualification
C03 business_status interpretation
D04 CONTINUE / STOP semantics
exactly-one next question
duplicate/already-answered suppression
USER_UNKNOWN / UNMEASURED handling
no-progress consequence
```

Legacy:

```
required > important > optional
completeness >= 0.7
fixed field priority
```

must not become the new D04 policy by accident.

The exact no-progress path must also be frozen without U06 inventing Clinical Readiness.

Engineering must not invent:
- approved mandatory questions;
- discriminator question content;
- medical stopping criteria;
- patient wording safety.

```
BF-U06-RG-02
= OPEN / BLOCKING

Required
= U06-RDP-02 F3 Owner / D04 Question Policy Contract
```

---

## BF-U06-RG-03 — F3 / Question / WAITING mutation and trace contract missing

The Unit Spec now freezes the mode-level mutation boundary, but implementation details remain unresolved.

Must freeze:

### MODE-1

```
F3_CANONICAL_EFFECT_ID
canonical F3 state/effect schema
StatePatch path/value
ADD/REPLACE/NO_OP/conflict
source decision/result refs
field permission/source validation
commit/read-back evidence
effect ledger relationship
```

### MODE-2

```
Question effect identity
Question PROPOSED/SELECTED commit boundary
Gap -> ASKED timing
Pending Question authority
DELIVERED_TO_USER commit
Consultation WAITING_USER commit
Thread AWAITING_USER runtime coordination
cross-store crash/replay semantics
```

### MODE-3

Must enforce:

```
readiness projection only
NO StateChangeProposal
NO canonical F3 mutation
NO Clinical State Version advance
```

Trace must correlate:
- admission/mode;
- C03 call when present;
- binding/releases;
- F3/D04 decision;
- proposal;
- commit;
- delivery refs;
- state versions;
- failure.

Current P05 baseline is insufficient to assume final U06 provenance shape automatically.

```
BF-U06-RG-03
= OPEN / BLOCKING

Required
= U06-RDP-03 State Ownership / K09-P01 Mutation / Trace Contract
```

---

## BF-U06-RG-04 — Question delivery / WAITING_USER / downstream side-effect boundary missing

MODE-2 success requires real semantic delivery ordering, but no durable delivery implementation exists.

Must freeze:

```
Question SELECTED
→ durable delivery intent
→ stable delivery_id / idempotency key
→ transport attempt
→ durable receipt
→ reconciliation
→ delivery confirmation
→ governed DELIVERED_TO_USER + WAITING_USER
→ Runtime AWAITING_USER/checkpoint
```

Must define crash behavior for at least:
- after selection / before intent;
- after intent / before send;
- after send / before receipt;
- after receipt / before delivered commit;
- after WAITING commit / before checkpoint.

Must freeze:

```
delivery failure
!= DELIVERED_TO_USER
!= WAITING_USER

duplicate retry
!= duplicate external send

U07 eligibility
only after valid delivered/wait state
```

For the current non-production slice, RDP-04 must explicitly choose what is allowed:
- synthetic/non-live delivery adapter;
- verifier fake/spy;
- or another bounded test transport.

It must not silently activate real patient-facing delivery.

```
BF-U06-RG-04
= OPEN / BLOCKING

Required
= U06-RDP-04 Delivery / Downstream / Side-effect Boundary
```

---

## BF-U06-RG-05 — C03 / D04 / P06 applicability and Capability Quality Gate not ready

This is the largest external dependency blocker.

Current C03/question capability state is:

```
structural skeleton only
clinical review required
runtime adoption not implemented
production blocked
question content arrays empty
stopping policy empty
prohibited_runtime_use = true
```

Therefore the project cannot claim:

```
C03 ACTIVE
D04 approved
patient-facing Question Policy approved
```

Additionally the current minimal P06/BindingReleaseResolver:
- resolves capability binding;
- does not implement full KnowledgeRelease/RuleRelease registries;
- does not physically bind the complete allowed Prompt/Model/Knowledge/Rule/Tool/Skill ref set frozen by Phase 8.

Conditional P03 applicability is also unresolved until the actual approved C03 implementation is selected.

U06 must freeze per mode:

```
MODE-1:
C03 required
D04 not necessarily question-selection owner
release applicability

MODE-2:
C03 required/fresh
D04 required
Question Policy required

MODE-3:
C03 NOT_INVOKED by default
D04 not used by default
historical binding + semantic compatibility required
```

The design must distinguish two possible implementation scopes:

```
A. real governed C03 integration
→ requires C03 Capability Quality Gate / approved content/policy

B. bounded non-production structural U06 implementation
→ may use explicitly authorized synthetic C03/D04 adapters
→ cannot claim C03/clinical question capability implemented
→ cannot perform live/patient-facing delivery
```

The choice itself requires explicit review; this readiness review does not choose A or B.

```
BF-U06-RG-05
= OPEN / BLOCKING

Required
= U06-RDP-05 Capability / Dependency / Applicability Contract
  + C03 Capability Quality Gate decision
  + shared-runtime/P06 impact assessment
```

---

## BF-U06-RG-06 — Verification / durable evidence plan missing

U06 verification must prove all three modes and all side-effect boundaries.

At minimum:

### Admission/currentness

```
legal/illegal admission per mode
wrong-mode payload
stale Gate
stale route/eligibility
RESTRICTED permission denied/unavailable
binding expired/disabled/incompatible
source version drift
```

### MODE-1

```
C03 invoked exactly when applicable
F3 Owner interpretation
canonical effect commit
exact replay
changed-payload replay conflict
no Question
no delivery
no WAITING
post-F3 Safety barrier consequence
```

### MODE-2

```
F1 vs F3 need
fresh C03 invocation
MODE-1 candidate non-reuse
D04 continue/stop
already-answered suppression
USER_UNKNOWN/UNMEASURED preservation
exactly-one question
selection commit
delivery idempotency
receipt/reconciliation
delivery failure -> no WAITING
crash windows
at-most-one external send
```

### MODE-3

```
REVALIDATED_CURRENT
REASSESSMENT_REQUIRED
FAILED
exact replay
STALE_BEFORE_PUBLISH
C03 count = 0 by default
StateChangeProposal count = 0
Clinical State mutation count = 0
```

### Global

```
typed effect counts
trace/provenance
no policy-expectation invention in test code
no real PHI
no live patient delivery
no external model/tool call unless separately authorized
Foundation/U01-U05 regression
U07/U14 live execution not required for current unit verification unless separately authorized
durable evidence retention
independent evidence review
```

```
BF-U06-RG-06
= OPEN / BLOCKING

Required
= U06-RDP-06 Verification / Durable Evidence Plan
```

---

# 6. Blocker-to-RDP matrix

| Blocker | Primary resolution |
|---|---|
| BF-U06-RG-01 | U06-RDP-01 Consumer Inbound / Admission |
| BF-U06-RG-02 | U06-RDP-02 F3 Owner / D04 Question Policy |
| BF-U06-RG-03 | U06-RDP-03 State Ownership / K09-P01 Mutation / Trace |
| BF-U06-RG-04 | U06-RDP-04 Delivery / Downstream / Side-effect |
| BF-U06-RG-05 | U06-RDP-05 Capability / Dependency / Applicability |
| BF-U06-RG-06 | U06-RDP-06 Verification / Durable Evidence |

Cross-RDP compatibility will be required because:
- RDP-01 mode/admission identities feed RDP-03 replay/effect identities;
- RDP-02 D04/F3 outcomes constrain RDP-03 state changes;
- RDP-03 selected/delivered state is inseparable from RDP-04 delivery reconciliation;
- RDP-05 binding/release applicability constrains RDP-01 admission and RDP-06 fixtures/oracles;
- RDP-06 must verify the exact frozen RDP-01..05 package.

---

# 7. Non-blocking / reusable prerequisites

The following are available and should not be rebuilt unnecessarily:

```
Phase 5 BL-01 / BL-04 semantics
= FROZEN

Phase 6 U06 role + A1 three-mode split
= REFROZEN / V1

Phase 7 C03/P01/P05/P06/D04 dependency model
= FROZEN

Phase 8 K06/K09/K10 + F3 revalidation semantics
= REFROZEN / V1

Phase 9 Delivery / Barrier / Scheduler semantic ordering
= REFROZEN / V1

U05 TO_U06_QUESTION_PATH
= IMPLEMENTED / IN_MAIN

P01 StateCommitter mechanical core
= AVAILABLE

CanonicalEffectLedger
= AVAILABLE

P05 capability-call trace baseline
= AVAILABLE

P06 minimal capability binding guard
= AVAILABLE
```

Availability does not mean U06 consumer wiring is complete.

---

# 8. Explicit non-prerequisites for starting RDP design

U06 RDP design does **not** require U07 to already be implemented.

Current non-production U06 design may terminate at:

```
valid delivered/wait state
+ typed U07 handoff eligibility / verifier seam
```

without invoking U07 live.

Likewise, U14 live execution is not required for U06 RDP design; U06 only needs a typed governed failure handoff boundary.

However:

```
U06 implementation complete
!= U06→U07 business loop complete
```

and no live downstream execution is implied.

---

# 9. Clinical/content dependency boundary

Engineering may define:
- data contracts;
- owner boundaries;
- failure vocabulary;
- idempotency;
- runtime sequencing;
- binding validation;
- verification harnesses.

Engineering may not invent:
- approved patient-facing question wording;
- mandatory/discriminator medical content;
- clinical stopping criteria;
- question value thresholds;
- medical decision-impact truth.

Those require appropriate clinical/product authority and Capability Quality Gate evidence.

If unavailable during implementation design, the only lawful fallback is an explicitly bounded synthetic/non-patient verification scope—not invented clinical content.

---

# 10. Required readiness package

The next design package is:

```
U06-RDP-01
Consumer Inbound / Admission Contract

U06-RDP-02
F3 Owner / D04 Question Policy Contract

U06-RDP-03
State Ownership / K09-P01 Mutation / Trace Contract

U06-RDP-04
Delivery / Downstream / Side-effect Boundary

U06-RDP-05
Capability / Dependency / Applicability Contract

U06-RDP-06
Verification / Durable Evidence Plan
```

Each must receive independent design review.

Any cross-phase/shared-runtime amendment discovered by an RDP must be separately inventoried and reviewed rather than silently absorbed into U06 code.

---

# 11. Recommended design order

Because the RDPs are not independent in construction order, recommended order is:

```
1. U06-RDP-01
   establish legal modes / inbound authorities / upstream gaps

2. U06-RDP-05
   establish C03/D04/P06/P03 applicability and quality-gate scope

3. U06-RDP-02
   freeze F3 Owner + D04 executable business decision semantics
   against the actual allowed dependency scope

4. U06-RDP-03
   freeze canonical F3 / Question / WAITING mutation and trace

5. U06-RDP-04
   freeze delivery/reconciliation/U07/failure side-effect boundaries

6. U06-RDP-06
   bind exact frozen package into executable verification/evidence
```

Rationale:

```
admission
→ determines mode/context

dependency applicability
→ determines what capability/policy may legally run

owner/policy
→ determines decisions

state contract
→ determines what may mutate

delivery boundary
→ determines external/runtime side effects

verification
→ proves all of the above
```

This ordering does not change the formal six-RDP set.

---

# 12. Authorization boundary

This review does not authorize:

```
U06 code implementation
C03 runtime implementation
D04 runtime implementation
U04 A1 pre-readiness amendment implementation
Scheduler implementation
MODE-3 continuation implementation
Question external delivery
U07 invocation
U14 live routing
production Clinical State mutation
production Clinical Runtime
release activation
real-patient traffic
```

It also does not authorize legacy question services as C03.

---

# 13. Final status

```
U06 Initial Implementation Readiness / Gap Review
= COMPLETE_PENDING_INDEPENDENT_REVIEW

U06 Unit Definition
= PASS

U06 Business / Unit Semantics
= SUFFICIENTLY_FROZEN_FOR_RDP_DESIGN

U06 Implementation Readiness
= NOT_READY

Open blocker count
= 6

Next permitted step after this review passes
= U06-RDP-01..06 DESIGN / REVIEW / FREEZE

Recommended first design task
= U06-RDP-01 Consumer Inbound / Admission Contract

Implementation Authorization Review
= NOT_PERMITTED_YET
```
