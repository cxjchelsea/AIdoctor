# U05 A1 Exact Frozen-Artifact Diff Inventory v0.1

> Selected architecture:
>
> ```text
> OD-U05-BOOTSTRAP-01 = A1
> ```
>
> Design branch:
>
> ```text
> design/u05-a1-detailed-controlled-amendment
> ```
>
> Owner-selection-ready baseline:
>
> ```text
> 2d9c2f3c98085e6938441919f5a74fab5e94cc71
> ```
>
> This inventory is a proposed amendment plan only.
> No Frozen artifact listed below has been modified by this package.

---

# 1. Inventory rules

Each row must answer：

```text
artifact
current frozen boundary
change type
exact proposed semantic change
owner/unit/capability/version impact
required re-review
```

Change type vocabulary：

```text
ADD
REPLACE
NARROW
CLARIFY
RESEQUENCE
MODE_SPLIT
```

All material semantic changes require：

```text
independent re-review
+ explicit re-freeze at exact head
```

---

# 2. Summary matrix

| ID | Frozen artifact | Baseline | Change | Material semantic impact | Re-review |
|---|---|---|---|---|---|
| A1-DIFF-01 | Phase 4 / 模块级状态与状态所有权_V1.md | main@6e68fd9 | ADD + CLARIFY | F3 legal activation timing before first U05 | REQUIRED |
| A1-DIFF-02 | U04_RDP04_Downstream_Routing_Boundary_v0.1.md | main@6e68fd9 | REPLACE + ADD | U04 ordinary eligibility becomes A1 pre-readiness eligibility when bootstrap required | REQUIRED |
| A1-DIFF-03 | Phase 5 / 业务闭环设计_V1.md | main@6e68fd9 | RESEQUENCE + CLARIFY | initial business loop becomes Safety→A1 F3→Safety barrier→Readiness | REQUIRED |
| A1-DIFF-04 | Phase 6 / 可验证开发单元拆分_V1.md | main@6e68fd9 | MODE_SPLIT + RESEQUENCE | U06 gains PRE_READINESS mode; U04/U05 S_out/S_in change | REQUIRED |
| A1-DIFF-05 | Phase 7 / 按开发单元的Capability设计.md | main@6e68fd9 | CLARIFY + ADD | C03 remains first consumed by U06 but gains pre-readiness usage timing | REQUIRED |
| A1-DIFF-06 | Phase 8 / Contract与数据语义设计.md | main@6e68fd9 | ADD + CLARIFY | A1 eligibility, F3 effect identity/proposal, F3-owned revalidation decision | REQUIRED |
| A1-DIFF-07 | Phase 9 / Runtime与技术架构设计_V1.md | main@6e68fd9 | RESEQUENCE + ADD | Scheduler gains A1 pre-readiness path + Safety barrier | REQUIRED |
| A1-DIFF-08 | U05_RDP05_Readiness_Input_Dependency_Applicability_Contract_v0.1.md | fd0e88e | REPLACE + NARROW | POST_SAFETY_INITIAL F3 applicability changes under A1 | REQUIRED |
| A1-DIFF-09 | U05_RDP02_D03_Policy_Owner_Decision_Contract_v0.1.md | PR127@2d9c2f3 | REPLACE + CLARIFY | bootstrap sentinel no longer executable initial A1 path; pre-D03 admission changes | REQUIRED |

---

# 3. A1-DIFF-01 — Phase 4 F3 Owner / activation timing

Artifact：

```text
docs/current/03_状态/模块级状态与状态所有权_V1.md
```

## Current frozen boundary

Current semantics include：

```text
F3 owns:
- what information is missing
- decision impact
- whether worth asking online
- next-question related state

F3 != Clinical Readiness Owner

new facts may trigger:
F3 Gap reevaluation
F4 Risk reevaluation
...
Clinical Readiness reevaluation
```

Canonical Gap lifecycle：

```text
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
```

## Proposed amendment

ADD：

```text
A1 PRE_READINESS is a lawful F3 activation context.

When:
BootstrapArchitectureBindingRef = A1
+ current U04 Gate permits continuation
+ no current valid A1 bootstrap F3 assessment

F3 may be activated before the first U05 D03 evaluation.
```

CLARIFY：

```text
pre-readiness activation changes timing only;
it does not change F3 ownership
and does not give F3 ownership of Clinical Readiness.
```

ADD invalidation consequence：

```text
canonical F3 commit may advance Clinical State Version
and therefore require current Risk/Safety re-establishment
before U05.
```

## Not changed

```text
F3 lifecycle vocabulary
F3 business owner
Clinical Readiness unique resolver
F5/F6 ownership
```

## Why material

This changes when F3 is legally active in the business lifecycle.

Re-review：

```text
REQUIRED
```

---

# 4. A1-DIFF-02 — U04-RDP-04 Downstream Routing Boundary

Artifact：

```text
docs/current/06_开发单元/U04_RDP04_Downstream_Routing_Boundary_v0.1.md
```

## Current frozen text/boundary

```text
Routing may consume only the current committed U04 Safety Gate
bound to the current Clinical State Version.

ALLOW
→ may make U05 ordinary-path eligibility available.

RESTRICTED
→ only restricted downstream path explicitly allowed by policy.

BLOCKED
→ ordinary U05 prohibited.

UNAVAILABLE
→ ordinary U05 prohibited.

One committed U04 result
→ at most one routing side effect for same business event identity.
```

## Proposed replacement

REPLACE ALLOW ordinary projection with：

```text
ALLOW
→ if BootstrapArchitectureBindingRef = A1
   and A1 bootstrap F3 effect is required/not current:
   PRE_READINESS_A1_F3_C03_ELIGIBLE

→ if A1 bootstrap F3 effect is already current/current-version revalidated:
   U05_ELIGIBLE
```

REPLACE/CLARIFY RESTRICTED：

```text
RESTRICTED
→ A1 pre-readiness or U05 eligibility only if explicitly allowed
   by governed restricted policy
→ restricted_context_ref required
```

Preserve：

```text
BLOCKED
→ no A1 pre-readiness
→ no U05

UNAVAILABLE
→ no A1 pre-readiness
→ no U05
```

ADD：

```text
BootstrapArchitectureBindingRef = A1
is governance/configuration binding,
not a U04 Safety decision.

Eligibility projection != Unit invocation.
U04 never executes U06/C03.
```

ADD routing authorization：

```text
routing_authorization_id
= current U04 result
+ business event
+ Clinical State Version
+ A1 binding
+ restricted context when applicable
```

VS-B rule：

```text
F3 state commit advances version
→ old U04 Gate/authorization/eligibility stale
→ after Safety barrier, new Gate creates new routing authorization
→ if same canonical F3 effect is current/revalidated:
   first eligible consequence = U05
   not A1 pre-readiness again
```

## Not changed

```text
Safety Gate owner
Gate vocabulary
BLOCKED/U11 family
UNAVAILABLE/U14 family
frontend/model cannot override Gate
```

Re-review：

```text
REQUIRED
```

---

# 5. A1-DIFF-03 — Phase 5 Business Loop

Artifact：

```text
docs/current/05_业务闭环/业务闭环设计_V1.md
```

## Current frozen initial sequence

```text
F2 facts
→ F4 Risk
→ G4 Safety Gate
→ Clinical Readiness Resolver

CAN_ASK_MORE
→ F3 / BL-04
```

## Proposed resequence

For A1 bootstrap only：

```text
F2 facts
→ F4 Risk
→ G4 current Safety Gate
→ A1 pre-readiness F3 assessment
→ canonical F3 commit
→ POST_F3_SAFETY_REVALIDATION_BARRIER
→ Risk decision valid for U04 evaluation basis
→ current committed G4 Safety Gate
→ F3 current-version revalidation
→ Clinical Readiness Resolver
```

ADD explicit distinction：

```text
A1 pre-readiness F3 assessment
!= BL-04 Question Delivery

A1 pre-readiness
→ no WAITING_USER
→ no delivered question
```

Preserve post-D03：

```text
NEEDS_CLARIFICATION / CAN_ASK_MORE
→ BL-04 active question loop
```

Preserve：

```text
same current Clinical State Version
→ one unique Clinical Readiness
→ one unique ordinary next business path
```

Re-review：

```text
REQUIRED
```

---

# 6. A1-DIFF-04 — Phase 6 Unit contracts

Artifact：

```text
docs/current/06_开发单元/可验证开发单元拆分_V1.md
```

## 6.1 U04 S_out

Current：

```text
ALLOW / legal RESTRICTED
→ U05
```

Proposed A1-bound branch：

```text
ALLOW / permitted RESTRICTED
→ if A1 bootstrap required:
   PRE_READINESS_A1_F3_C03_ELIGIBLE
   → U06 PRE_READINESS_GAP_ASSESSMENT

→ if A1 bootstrap already current:
   U05
```

## 6.2 U05 S_in

Current：

```text
Safety Gate permits ordinary continuation
+ applicable F1/F3/F5/F6 inputs
+ current Clinical State Version
```

Proposed clarification：

```text
When A1 binding is active and bootstrap F3 is required:
U05 admission requires:
- current U04 Gate
- current-version compatible F3 readiness input
- A1 bootstrap completion/revalidation ref
```

No direct U05→C03 invocation.

## 6.3 U06 mode split

Current U06 is Question Selection / Delivery.

Proposed：

```text
U06 MODE-1 = PRE_READINESS_GAP_ASSESSMENT
U06 MODE-2 = QUESTION_SELECTION_DELIVERY
U06 MODE-3 = F3_CURRENT_VERSION_REVALIDATION
```

MODE-1 admission：

```text
current A1 eligibility
current Gate/version
current Facts/framing
valid C03 binding
no current valid A1 bootstrap F3 completion
```

MODE-1 action：

```text
C03 gap/decision-impact
→ U06/F3 interpretation
→ canonical F3 intended effect
```

MODE-1 forbidden：

```text
Question SELECTED
Question DELIVERED_TO_USER
WAITING_USER
AWAITING_USER
```

MODE-2 preserves existing：

```text
F1 clarification
or Clinical Readiness = CAN_ASK_MORE
→ select/deliver one question
→ WAITING_USER only after delivery
```

## 6.4 U03/U04 barrier consequence

ADD A1 internal continuation：

```text
F3 commit
→ Clinical State changed
→ U03 when Risk dependency requires reevaluation
→ Risk decision valid for U04 evaluation basis
→ U04 current committed Gate
→ U06 F3_CURRENT_VERSION_REVALIDATION
→ current F3 readiness input
→ U05
```

Dependency validity, not literal version-number equality, determines whether Risk/Safety must re-run.

Re-review：

```text
REQUIRED
```

---

# 7. A1-DIFF-05 — Phase 7 C03 usage timing

Artifact：

```text
docs/current/07_能力设计/按开发单元的Capability设计.md
```

## Current frozen boundary

```text
C03 = question / information-gap Capability
FIRST_CONSUMER_UNIT = U06

Runtime sequence:
U05
→ D03
→ U06
→ C03 question/gap
→ D04 stopping
→ Question SELECTED
→ delivery
```

## Proposed amendment

Preserve：

```text
FIRST_CONSUMER_UNIT = U06
```

ADD mode-aware U06/C03 usage：

```text
U06 PRE_READINESS_GAP_ASSESSMENT
→ validate C03 binding
→ C03 Gap Detection / Decision Impact
→ U06/F3 Owner interpretation
→ no Question selection/delivery side effect

U06 QUESTION_SELECTION_DELIVERY
→ validate current C03 binding/question policy
→ C03 question/gap
→ D04 stopping
→ Question SELECTED
→ delivery
```

ADD V1 candidate lifetime rule：

```text
pre-readiness C03 question candidates
= support/trace-only
= never reused by QUESTION_SELECTION_DELIVERY

Question mode always performs fresh governed candidate evaluation/C03 invocation.
```

ADD revalidation mode：

```text
U06 F3_CURRENT_VERSION_REVALIDATION
→ deterministic F3 Owner decision
→ no C03 by default
→ no Clinical State mutation
```

Not changed：

```text
C03 is not Clinical Readiness Owner
C03 cannot commit Clinical State directly
```

Re-review：

```text
REQUIRED
```

---

# 8. A1-DIFF-06 — Phase 8 Contract/Data semantics

Artifact：

```text
docs/current/08_契约与数据/Contract与数据语义设计.md
```

## Proposed ADD — A1 pre-readiness eligibility

```text
eligibility_id
eligibility_type = PRE_READINESS_A1_F3_C03_ELIGIBLE
routing_authorization_id
consultation_id
cdp_id
clinical_state_version
u04_gate_ref
bootstrap_architecture_binding_ref = A1
restricted_context_ref?
validity
created_at
trace_refs[]
```

## Proposed ADD — F3 canonical effect identity

```text
F3_CANONICAL_EFFECT_ID
```

Minimum derivation：

```text
consultation
+ fact/framing basis
+ source version
+ C03 binding
+ F3 assessment policy version
+ assessment event/trigger identity
```

## Proposed CLARIFY — K09 F3 proposal

Must carry：

```text
source version
F3_CANONICAL_EFFECT_ID
C03 result ref
CapabilityBindingRef
Rule/Knowledge refs
expected current version
effect idempotency key
trace refs
```

Preserve：

```text
Capability Result != StateChangeProposal
```

## Proposed ADD — F3 current-version revalidation decision

```text
Owner = F3
Host = U06 F3_CURRENT_VERSION_REVALIDATION
Trigger = POST_F3_SAFETY_BARRIER_CURRENT_GATE_READY

F3CurrentVersionRevalidationDecision:
- F3_REVALIDATION_ID
- source canonical F3 refs
- target Clinical State Version
- current U04 Gate ref
- before/current dependency fingerprints
- semantic_binding_compatibility_ref
- historical CapabilityBindingRef / Rule/Knowledge refs
- outcome = REVALIDATED_CURRENT / REASSESSMENT_REQUIRED / FAILED
- reason/policy/trace refs
```

No state mutation occurs for reference binding alone.

## Proposed ADD — current-version F3 readiness input

```text
current_version_revalidation_ref
source_state_ref
source_decision_ref = revalidation decision ref
bound_current_clinical_state_version
bound_current_u04_gate_ref
validity
evidence/policy refs
```

This is a readiness-input projection, not duplicate F3 state.

## Proposed CLARIFY — barrier metadata

```text
POST_F3_SAFETY_REVALIDATION_BARRIER
```

may exist in Runtime/checkpoint contracts only.

It is not Clinical Truth.

Re-review：

```text
REQUIRED
```

---

# 9. A1-DIFF-07 — Phase 9 Scheduler / Runtime

Artifact：

```text
docs/current/09_Runtime与技术架构/Runtime与技术架构设计_V1.md
```

## Current frozen route

```text
Facts
→ U03
→ U04
→ U05

U05
→ D03
→ commit

U06
→ C03
→ D04
→ Question SELECTED
→ delivery
```

## Proposed A1 route

```text
Facts
→ U03 current Risk
→ U04 current Gate
→ route projection
→ U06 PRE_READINESS_GAP_ASSESSMENT
→ validate C03 binding
→ C03
→ U06/F3 interpretation
→ K09
→ G2/P01 F3 commit
→ reload authoritative Clinical State
→ POST_F3_SAFETY_REVALIDATION_BARRIER
→ U03 only when declared Risk dependencies require reevaluation
→ U04 current Gate from valid Risk/Safety evaluation basis
→ U06 F3_CURRENT_VERSION_REVALIDATION
→ deterministic revalidation decision
→ current F3 readiness input
→ new route projection
→ U05
→ D03
```

ADD barrier exit conditions：

```text
Risk decision valid for U04 evaluation basis
current committed U04 Gate
no post-Gate declared dependency invalidation
F3 revalidation = REVALIDATED_CURRENT
current F3 readiness input for D03 target version
current routing authorization
```

Do not require literal Risk/Gate/F3 version equality when only downstream derived commits advanced the version.

ADD no-cycle：

```text
same F3_CANONICAL_EFFECT_ID
+ Risk/Safety-only version changes
→ no duplicate F3 canonical commit
```

ADD crash/replay refs：

```text
routing_authorization_id
F3_CANONICAL_EFFECT_ID
commit result
barrier stage
current Gate ref
F3 revalidation ref
```

Preserve：

```text
Scheduler routes from committed state
state change -> commit -> reload authoritative state
blind stale retry prohibited
one Consultation authoritative clinical writer
```

Re-review：

```text
REQUIRED
```

---

# 10. A1-DIFF-08 — U05-RDP-05

Artifact：

```text
docs/current/06_开发单元/U05_RDP05_Readiness_Input_Dependency_Applicability_Contract_v0.1.md
```

Frozen exact baseline：

```text
fd0e88e21aaab2a2ab67ffd1449dce8e946d7ed5
```

## Current frozen conflict with A1

Current：

```text
POST_SAFETY_INITIAL
→ F3 = NOT_YET_APPLICABLE

U05 must not wait for U06/C03 before U05 can run

U05 must not call C03
```

The last rule remains valid.
The first two require A1-specific amendment/narrowing.

## Proposed applicability matrix extension

ADD A1 bootstrap contexts：

```text
A1_POST_SAFETY_BOOTSTRAP_REQUIRED
F3 = NOT_YET_APPLICABLE
U05 ordinary admission = PROHIBITED
route = A1 pre-readiness

A1_PRE_READINESS_IN_PROGRESS
F3 = NOT_YET_APPLICABLE / producer pending
U05 ordinary admission = PROHIBITED

A1_F3_COMMITTED_BARRIER_PENDING
canonical F3 source state exists
readiness input = STALE / NOT_CURRENT_FOR_D03
U05 ordinary admission = PROHIBITED

A1_POST_BARRIER_CURRENT
F3 readiness input = PRESENT / CURRENT
U05 admission may proceed
```

NARROW current text：

```text
"U05 must not wait for U06/C03 before U05 can run"
```

to：

```text
Outside an explicitly governed A1 bootstrap path,
U05 does not invent a dependency on U06/C03.

When A1 binding is active and bootstrap F3 is required,
ordinary U05 admission occurs only after the governed
pre-readiness A1 path completes.
```

Preserve：

```text
U05 directly calling C03 = PROHIBITED
stale F3 cannot be mixed with current inputs
current-version revalidation/reference binding required
source_domain remains F3
```

Re-review：

```text
REQUIRED
```

---

# 11. A1-DIFF-09 — U05-RDP-02

Artifact：

```text
docs/current/06_开发单元/U05_RDP02_D03_Policy_Owner_Decision_Contract_v0.1.md
```

Baseline：

```text
PR #127 exact head
2d9c2f3c98085e6938441919f5a74fab5e94cc71
```

## Current bootstrap sentinel

```text
F1 FRAMED_IN_SCOPE
+ F3/F5/F6 NOT_YET_APPLICABLE
→ POLICY_EXPECTATION_GAP
→ no runtime D03 authorization
```

## Proposed A1 amendment

REPLACE executable initial-path interpretation：

```text
When BootstrapArchitectureBindingRef = A1
and bootstrap F3 is required/not current:

U04 routing / U05 inbound admission
→ U05 ordinary readiness evaluation is not eligible
→ D03 is not invoked
→ no D03 decision_id
→ no D03 decision_status

because the governed path remains in A1 pre-readiness / Safety-barrier / F3-revalidation processing.
```

After A1 completion：

```text
current Gate
+ current F3 readiness input
→ normal D03 admission/evaluation
```

CLARIFY：

```text
POLICY_EXPECTATION_GAP remains a design/readiness sentinel
for incomplete/uncovered configurations,
not a patient runtime result.
```

Preserve D03 runtime statuses：

```text
DECIDED
INPUT_FAILURE
INPUT_CONFLICT
```

Preserve separate decision：

```text
OD-U05-READY-01
= NOT_APPROVED
```

Therefore：

```text
D03-POL-005
F1 FRAMED_IN_SCOPE
+ F3 NO_ACTIVE_ONLINE_BLOCKING_GAP
+ no higher blocker
→ READY_FOR_CLINICAL_ANALYSIS
```

remains：

```text
PROPOSED OWNER EXPECTATION
not frozen executable expectation
```

Re-review：

```text
REQUIRED
```

---

# 12. Cross-artifact consistency constraints

The nine amendments must satisfy simultaneously：

```text
CROSS-01
Phase 4 says F3 can activate pre-U05
iff Phase 5/6/9 route exists.

CROSS-02
U04-RDP-04 exposes eligibility only;
Phase 9 Scheduler performs execution.

CROSS-03
Phase 6 U06 PRE mode cannot enter WAITING_USER;
Phase 7/9 Question mode remains delivery owner.

CROSS-04
Phase 7 C03 first consumer remains U06.

CROSS-05
Phase 8 canonical effect/revalidation/idempotency contracts
match Phase 9 replay/no-cycle behavior.

CROSS-06
F3 commit advancing Clinical State Version
must stale old Risk/Gate/routing authorization.

CROSS-07
RDP-05 current-version input semantics
must match Phase 8 revalidation contract.

CROSS-08
A1 bootstrap incomplete
→ U05/D03 not invoked
→ no D03 object/status;
RDP-02 must not invent admission-status vocabulary.

CROSS-09
A1 selection != OD-U05-READY-01 approval.

CROSS-10
No artifact may imply U05 directly calls C03.
```

Any contradiction：

```text
→ amendment package = NOT_READY_FOR_AMENDMENT_AUTHORIZATION
```

---

# 13. Exact amendment authorization boundary

Even if this inventory passes design review：

```text
PASS
!= authorization to edit frozen artifacts
```

Required sequence：

```text
1. A1 Detailed Amendment Independent Design Review
2. PASS
3. explicit Owner/Governance authorization to amend named frozen artifacts
4. edit only authorized artifacts
5. independent re-review each amended frozen artifact
6. re-freeze exact heads
7. reconcile RDP-05/RDP-02
8. separately resolve OD-U05-READY-01
9. U05 implementation readiness review
```

Current status：

```text
A1 Exact Frozen-Artifact Diff Inventory
= PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW

Frozen artifacts modified
= 0

Amendment authorization
= NOT_GRANTED
```


---

# 14. Independent-review remediation mapping

```text
BF-U05-A1-IR-01
→ A1-DIFF-03 / 04 / 07 refined:
dependency-validity barrier;
no recursive U03 rerun due U04 commit version advancement alone.

BF-U05-A1-IR-02
→ A1-DIFF-04 / 06 / 07 refined:
F3-owned U06 F3_CURRENT_VERSION_REVALIDATION mode,
deterministic decision contract,
binding compatibility,
idempotency/failure rules.

BF-U05-A1-IR-03
→ A1-DIFF-09 refined:
A1 bootstrap incomplete means no U05/D03 invocation and no D03 object/status.

RQ-U05-A1-IR-04
→ A1-DIFF-05 refined:
pre-readiness C03 question candidates are support/trace-only and never reused.
```

Current status：

```text
A1 Exact Frozen-Artifact Diff Inventory
= REVISED / READY_FOR_TARGETED_INDEPENDENT_DESIGN_REVIEW

Frozen artifacts modified
= 0

Amendment authorization
= NOT_GRANTED
```
