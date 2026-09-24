# U06 Entry Gate Assessment v0.1

> Review target: post-U05 next Development Unit / Gate  
> Review basis: `main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8`  
> Scope: repository planning / readiness only  
> This assessment grants no U06 implementation, merge, production, live delivery, or real-patient authorization.

---

# 1. Executive verdict

```
U05
= IMPLEMENTED
= VERIFIED
= MERGED_TO_MAIN
= REPOSITORY_INTEGRATION_COMPLETE

Next Development Unit
= U06

U06 frozen business-semantic baseline
= AVAILABLE / A1 REFROZEN V1

U06 dedicated Unit Spec
= MISSING

U06 dedicated RDP-01..06 implementation-governance package
= MISSING

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
```

Therefore the next concrete deliverable and formal review gate are:

```
NEXT CONCRETE DELIVERABLE
= U06 Unit Spec v0.1

NEXT FORMAL REVIEW GATE
= U06 Initial Implementation Readiness / Gap Review
```

That initial readiness review consumes the Unit Spec together with the already-frozen Phase 6/7/8/9 semantics and current repository archaeology. Its purpose is to identify the exact missing implementation-governance contracts/dependencies and turn them into explicit blockers/tasks.

The expected blocker-resolution structure is U06-RDP-01..06, but those RDP contracts do not need to be complete before the initial Gap Review begins.

It is not yet permitted to enter U06 runtime/code implementation.

---

# 2. Why U06 is the next Development Unit

The frozen Phase 6 sequence defines:

```
U01
→ U02
→ U03
→ U04
→ U05
→ U06
→ U07
→ U02
```

for the first Intake / Wait / Resume slice.

U05 canonical downstream mapping is frozen as:

```
NEEDS_CLARIFICATION
→ U06

CAN_ASK_MORE
→ U06

READY_FOR_CLINICAL_ANALYSIS
→ U08

NEEDS_OFFLINE_EVIDENCE
→ U10

OUT_OF_SCOPE / NO_RELIABLE_DIRECTION
→ U11
```

U05 RDP-04 further freezes:

```
NEEDS_CLARIFICATION / CAN_ASK_MORE
→ TO_U06_QUESTION_PATH
```

and explicitly prohibits U05 from:
- selecting the actual question;
- invoking C03;
- creating Question truth;
- setting WAITING_USER;
- delivering the question.

Therefore the ordinary downstream owner after the now-complete U05 question-route eligibility is U06.

---

# 3. Why U08 is not the immediate next Unit

U08 is also a lawful downstream target from U05:

```
READY_FOR_CLINICAL_ANALYSIS
→ TO_U08_CLINICAL_ANALYSIS
```

but the frozen construction wave is:

```
Wave 1
Core: U01 U02 U03 U04 U05 U06 U07
Closure: U11 U14 U15 slice-required scope

Wave 2
U08 U09
```

The project is currently completing Wave 1 core in sequence.

Therefore U08 is not the next default Development Unit while U06/U07 remain unimplemented.

This does not mean U08 is semantically blocked forever; it means the frozen recommended construction order selects U06 first.

---

# 4. U06 frozen responsibility

U06 owns the governed question / gap business unit boundary.

Frozen Phase 6 role:

```
U06
= F3 Gap assessment
+ current-version revalidation
+ key-question selection
+ Question lifecycle
+ transition to WAITING_USER only after delivery
```

Frozen Phase 7 dependency row:

```
Clinical Capability
= C03

Platform
= P01
+ P05
+ P06

Deterministic Policy
= D04
```

C03:

```
FIRST_CONSUMER_UNIT = U06
```

C03 does not own:
- canonical F3 truth;
- WAITING_USER;
- final continue/stop decision.

U06/F3 Owner and D04 remain the business owners.

---

# 5. U06 three-mode model already frozen

A1 controlled amendments freeze three distinct U06 modes.

## MODE-1 — PRE_READINESS_GAP_ASSESSMENT

Entry originates from the A1 pre-readiness route after current U04 Safety Gate:

```
U04 current Gate
→ PRE_READINESS_A1_F3_C03_ELIGIBLE
→ Scheduler
→ U06 PRE_READINESS_GAP_ASSESSMENT
```

U06 must:

```
validate current C03 CapabilityBindingRef
→ invoke C03 Gap Detection / Decision Impact
→ U06/F3 Owner interpretation
→ canonical F3 intended effect
→ K09/P01 governed commit
```

This mode must not:
- deliver a Question;
- set WAITING_USER;
- reuse ephemeral candidate material later as a delivered question.

## MODE-2 — QUESTION_SELECTION_DELIVERY

Entry may come from:

```
U05
NEEDS_CLARIFICATION / CAN_ASK_MORE
→ TO_U06_QUESTION_PATH
```

U06 must:

```
validate current binding / Question Policy
→ fresh C03 invocation / fresh candidate evaluation
→ D04 stopping
→ Question SELECTED
→ durable delivery
→ DELIVERED_TO_USER
→ governed WAITING_USER / AWAITING_USER transition
```

Only after successful delivery:

```
Question = DELIVERED_TO_USER
Consultation = WAITING_USER
Thread = AWAITING_USER
```

## MODE-3 — F3_CURRENT_VERSION_REVALIDATION

Entry originates from controlled continuation routing:

```
TO_F3_CURRENT_VERSION_REVALIDATION
→ U06 MODE-3
```

Default:

```
C03 = NOT_INVOKED
Clinical State mutation = NONE
Question side effect = NONE
```

Outcomes:

```
REVALIDATED_CURRENT
→ materialize current-version F3 readiness input
→ continue governed routing/readiness

REASSESSMENT_REQUIRED
→ fresh MODE-1 with current bindings

FAILED
→ no U05/D03
→ governed failure path
```

---

# 6. Current repository archaeology

At the reviewed main head:

```
runtime/u06 package
= ABSENT

U06-specific Unit Spec file
= ABSENT

U06-specific RDP files
= ABSENT

U06 implementation authorization
= ABSENT / NOT_GRANTED
```

This is consistent with the current development position.

The repository does contain frozen cross-phase U06 semantics embedded in:
- Phase 6 Development Units;
- Phase 7 Capability Design;
- Phase 8 Contract/Data semantics;
- Phase 9 Runtime architecture;
- U05 RDP-04 downstream boundary.

Those documents are semantic inputs to U06 design, not a substitute for a U06 implementation-governance package.

---

# 7. Next deliverable and formal gate

Before U06 implementation, Phase 6 requires an independent Unit Spec.

The immediate next deliverable is:

```
U06 Unit Spec v0.1
```

The next formal review gate is:

```
U06 Initial Implementation Readiness / Gap Review
```

This follows the established U04/U05 governance pattern: the initial readiness review does not require the full RDP package to already exist. Instead it evaluates the frozen semantic baseline, Unit Spec, repository state and dependency archaeology, then identifies missing unit-specific contracts as explicit readiness blockers.

Minimum Unit Spec must define:

```
Unit ID
Purpose
Covered Business Loops
S_in
Event / Trigger
Preconditions
Business Decision
Required Capability
State Change Proposal
S_out
Failure Policy
Idempotency / Concurrency
Observability
Acceptance Cases
Regression Cases
Legacy Asset Mapping
Out-of-Scope
```

For U06, the Unit Spec must explicitly model all three frozen modes and their different side-effect boundaries.

---

# 8. Expected U06 RDP blocker-resolution package

Following the governed U04/U05 implementation-readiness pattern, the initial U06 Gap Review is expected to identify six unit-specific implementation-governance contract families.

These are the expected blocker-resolution structure before U06 can eventually become Implementation-Ready; they are not prerequisites for starting the initial Gap Review.

## U06-RDP-01 — Consumer Inbound / Admission Contract

Must freeze legal admission for all three modes:

```
PRE_READINESS_GAP_ASSESSMENT

QUESTION_SELECTION_DELIVERY

F3_CURRENT_VERSION_REVALIDATION
```

It must bind:
- consultation / CDP identity;
- Clinical State Version/currentness;
- U04 Gate / restricted permission;
- U05 eligibility when MODE-2 comes from U05;
- ClinicalContinuationRoutingDecision when MODE-3 applies;
- A1 pre-readiness routing authorization when MODE-1 applies;
- mode identity and replay identity.

No caller may select an unauthorized mode.

## U06-RDP-02 — F3 Owner / D04 Question Policy Contract

Must freeze:
- F1 Clarification vs F3 Gap distinction;
- Gap Decision Impact;
- canonical F3 Owner interpretation;
- D04 stopping;
- exactly-one next question;
- no-value stop semantics;
- no infinite question loop;
- USER_UNKNOWN / UNMEASURED preservation;
- MODE-1 / MODE-2 candidate non-reuse;
- MODE-3 deterministic revalidation outcome.

C03 output must remain:

```
Capability Result
!= canonical F3 truth
!= Question truth
!= WAITING_USER
```

## U06-RDP-03 — State Ownership / Mutation / Trace Contract

Must freeze authoritative owners and governed mutations for:
- canonical F3 effect/state;
- Question PROPOSED / SELECTED / DELIVERED_TO_USER lifecycle;
- Pending Question;
- Consultation WAITING_USER;
- Thread AWAITING_USER;
- F3 current-version readiness projection.

Formal Clinical State changes must use P01/G2/K09.

The WAITING transition must not happen before delivery success.

## U06-RDP-04 — Delivery / Downstream / Side-Effect Boundary

Must freeze:
- MODE-1 = no external delivery;
- MODE-2 durable delivery intent / idempotency / receipt / reconciliation;
- Question delivery success before WAITING_USER;
- delivery failure semantics;
- U07 eligibility only after valid waiting state;
- MODE-3 = no delivery and no C03 by default;
- no silent alternate business route on failure;
- Scheduler may not reinterpret U06 business truth.

## U06-RDP-05 — Capability / Dependency / Applicability Contract

Must freeze U06 dependencies:

```
C03
P01
P05
P06
D04
```

and exact mode-aware C03 usage.

It must also define:
- C03 CapabilityBindingRef validation;
- Question Policy binding;
- rule/knowledge/prompt/model refs as applicable;
- RESTRICTED action-specific permission;
- unavailable/expired/incompatible binding semantics;
- C03 failure vocabulary;
- applicability of C03 per mode.

## U06-RDP-06 — Verification / Durable Evidence Plan

Must cover at minimum:
- legal/illegal admission for each mode;
- stale eligibility / stale version / stale Gate;
- exact replay and changed-payload replay conflict;
- C03 success/failure/no-result distinctions;
- D04 stop/continue;
- duplicate/already-answered filtering;
- no-value question stop;
- MODE-1 no delivery proof;
- MODE-1 candidate cannot be reused by MODE-2;
- MODE-2 fresh C03 invocation;
- delivery idempotency;
- crash after delivery before WAITING commit;
- exactly-once delivered/wait transition;
- delivery failure -> no WAITING_USER;
- MODE-3 no C03/default no mutation;
- REVALIDATED_CURRENT vs REASSESSMENT_REQUIRED vs FAILED;
- no duplicate canonical F3 effect;
- no external/live side effects outside the authorized non-production verification scope;
- regression against U01-U05/Foundation/shared-runtime behavior.

---

# 9. Capability Quality Gate is a prerequisite, not the final Unit Gate

C03 entering formal U06 execution must satisfy the Phase 7 Capability Quality Gate.

At minimum:

```
Capability ID / Version
Supported Scope
Input semantic boundary
Output semantic boundary
Failure semantics
binding/version refs
EvalSet
Baseline
Metrics
Acceptance threshold
Safety cases
Regression suite
fallback/unavailable behavior
Owner / Review status
```

C03-specific quality focus:

```
decision value
duplicate question rate
user burden
already-answered suppression
invalidated-gap suppression
no-value stopping
no diagnostic/treatment suggestion leakage
```

But:

```
C03 Eval PASS
!= U06 Unit PASS
```

The capability gate is one prerequisite consumed by U06 Implementation Readiness.

---

# 10. Exact gate sequence from current state

The governed sequence should be:

```
U06 Business-Semantic Baseline
= AVAILABLE / REFROZEN

↓ NOW

U06 Unit Spec v0.1

↓

U06 Initial Implementation Readiness / Gap Review

↓ expected output:
  explicit readiness blockers
  dependency gaps
  U06-RDP-01..06 design tasks

↓

U06-RDP blocker remediation / design / review / freeze

↓

U06 Aggregate Contract Compatibility Review

↓

U06 Implementation Readiness Re-Evaluation

↓

U06 Implementation Authorization Review

↓

Explicit Owner Authorization

↓

U06 non-production implementation

↓

Exact-head implementation verification

↓

Independent evidence review

↓

Merge authorization / post-merge verification
```

Therefore the immediate next formal gate is not:

```
U06 Implementation Authorization
```

and not:

```
U07
U08
Production Activation
```

The immediate next formal review gate is:

```
U06 Initial Implementation Readiness / Gap Review
```

---

# 11. Current status matrix

| Item | Current status |
|---|---|
| U05 implementation | PASS / IN_MAIN |
| U05 RDP-06 verification | PASS |
| U05 repository integration | COMPLETE |
| U06 frozen business role | AVAILABLE / REFROZEN V1 |
| U06 three-mode semantics | REFROZEN / V1 |
| C03 first-consumer assignment | U06 / FROZEN |
| U06 dedicated Unit Spec | MISSING |
| U06 RDP-01..06 | MISSING |
| C03 U06-specific Quality Gate evidence | NOT_ASSESSED |
| U06 Aggregate Contract Compatibility | NOT_ASSESSED |
| U06 Implementation Readiness | NOT_READY |
| U06 Implementation Authorization | NOT_GRANTED |
| U06 code implementation | NOT_STARTED |
| Production/live authorization | NOT_GRANTED |

---

# 12. Final decision

```
NEXT DEVELOPMENT UNIT
= U06

NEXT CONCRETE DELIVERABLE
= U06 Unit Spec v0.1

NEXT FORMAL REVIEW GATE
= U06 Initial Implementation Readiness / Gap Review

EXPECTED GATE OUTPUT
= explicit readiness blockers
  + dependency gap inventory
  + U06-RDP-01..06 design tasks

IMPLEMENTATION MAY BEGIN NOW
= NO
```

Recommended first concrete action:

```
Create U06 Unit Spec v0.1
then perform
U06 Initial Implementation Readiness / Gap Review
```

Only after that gate identifies and the project closes the required U06-specific contract/dependency blockers should U06 Implementation Readiness be re-evaluated and Implementation Authorization considered.

---

# 13. Independent Review Remediation

Initial Independent Review:

```
PR #227
review_id = 5286991299
verdict = REVISE_REQUIRED
```

Finding:

```
BF-U06-ENTRY-IR-01
= INVENTED_FORMAL_GATE_NAME_AND_ORDERING
```

Remediation:

- removed the descriptive but non-frozen gate name `U06 UNIT ENTRY / DEFINITION-DEPENDENCY-CONTRACT READINESS` as a claimed formal gate;
- aligned the next formal review with the established U04/U05 pattern:
  `U06 Initial Implementation Readiness / Gap Review`;
- moved U06-RDP-01..06 from precondition to expected blocker-resolution output;
- retained U06 as the confirmed next Development Unit.

Current:

```
BF-U06-ENTRY-IR-01
= REMEDIATED / TARGETED_RE_REVIEW_PENDING

U06 Entry Gate Assessment
= REVISED
```
