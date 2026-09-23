# U06 Aggregate Compatibility Review / Controlled Amendment v0.1

> Unit: **U06 — F3 Gap / Question Selection / Delivery / Revalidation**  
> Aggregate basis: **U06-RDP-01..06 = PASS / PENDING_AGGREGATE_CLOSURE**  
> Parent head: **e4110cf0ca85f9443a19ac01a5e448369c05832a**  
> Runtime repository basis: **main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8**  
> Scope: **AGGREGATE COMPATIBILITY / CONTROLLED AMENDMENT DESIGN ONLY**  
> Status: **DRAFT / READY_FOR_INDEPENDENT_AGGREGATE_REVIEW**  
> This document does not authorize implementation, merge, production, live routing, real C03/D04 activation, or real-patient delivery.

---

# 1. Purpose

U06-RDP-01..06 have individually passed design review, but several contracts intentionally recorded cross-contract compatibility impacts.

This aggregate review answers:

~~~text
Which differences are true semantic conflicts?
Which require a controlled amendment/refreeze?
Which are merely physical implementation impacts?
Which blocked upstream producers may remain disabled in a bounded non-production slice?
What exact aggregate baseline must later Implementation Readiness Re-Evaluation consume?
~~~

The goal is not to redesign U06.

The goal is to make the six RDPs jointly executable without hidden contradiction.

---

# 2. Aggregate closure principle

Must preserve:

~~~text
RDP PASS
!= aggregate compatibility closed

aggregate compatibility closed
!= implementation ready

implementation ready
!= implementation authorized

implementation authorized
!= production authorized
~~~

A compatibility amendment may:
- normalize names/identities;
- clarify cross-RDP ownership;
- select one already-permitted physical compatibility strategy;
- explicitly disable not-yet-authorized sources/profiles.

It may not:
- invent clinical policy;
- activate real C03/D04;
- enable real patient delivery;
- weaken Safety/currentness;
- silently convert synthetic verification identity into production identity.

---

# 3. Aggregate input set

This review binds the following current semantic heads:

~~~text
U06 Unit Spec
= ae332b4a8fc5e179e352a68e8c4762117340c6c5

U06-RDP-01
= a7fc37a17f488670915faf517dc7ea7f4cd18680

U06-RDP-05
= 0e9b7be67c349cf4056cc144d4039efe17017241

U06-RDP-02
= 23fd6764439f55c053691daa500fede5030411ec

U06-RDP-03
= 09bb7f0244300fdfc750065823361ef7846802f0

U06-RDP-04
= 332f4e1d341051232325fdbd60192f3ade6cbffa

U06-RDP-06
= e4110cf0ca85f9443a19ac01a5e448369c05832a
~~~

The exact content digests must later be captured by the RDP-06 contract manifest.

---

# 4. Compatibility finding inventory

Aggregate review identifies the following compatibility families:

~~~text
AC-U06-01
RDP-01 capability_binding_ref
vs
RDP-05 dependency_binding_type / dependency_binding_ref

AC-U06-02
RDP-01 policy representation
vs
RDP-02 normalized F3 owner / Question / D04 policy identities

AC-U06-03
Phase-8 F3_CANONICAL_EFFECT_ID C03 CapabilityBindingRef component
vs
RDP-03 normalized dependency binding identity

AC-U06-04
current P05 CapabilityCallTraceRecord/bindReleases
vs
RDP-05 optional applicability + RDP-03 U06 parent trace

AC-U06-05
RDP-03 Clinical State delivered child
vs
RDP-04 Consultation WAITING / Runtime AWAITING cross-store choreography

AC-U06-06
RDP-01 business-legal upstream source categories
vs
current producer implementation availability

AC-U06-07
direct F1 clarification business path
vs
missing frozen executable safety/routing activation authority

AC-U06-08
PROFILE-B synthetic structural implementation target
vs
PROFILE-A real dependency/delivery blockers

AC-U06-09
RDP-06 oracle/fixture/contract authority
vs
any aggregate amendment/refreeze performed here

AC-U06-10
RDP04-AC-18 recoverable Thread wait wording
vs
final RDP-04 authoritative Thread = AWAITING_USER eligibility rule
~~~

---

# 5. Amendment A01 — normalized dependency binding identity

## 5.1 Problem

RDP-01 uses legacy wording:

~~~text
capability_binding_ref
~~~

for MODE-1/MODE-2 C03 admission.

RDP-05 freezes:

~~~text
dependency_binding_type
dependency_binding_ref
~~~

with:

~~~text
REAL_CAPABILITY_BINDING
SYNTHETIC_VERIFICATION_BINDING
~~~

A synthetic verification binding must not masquerade as a real P06 CapabilityBindingRef.

## 5.2 Refrozen aggregate contract

The canonical U06 admission/dependency identity is:

~~~text
dependency_binding_type
dependency_binding_ref
~~~

For real governed execution:

~~~text
dependency_binding_type
= REAL_CAPABILITY_BINDING

dependency_binding_ref
= real governed P06 C03 CapabilityBindingRef
~~~

For bounded synthetic structural verification:

~~~text
dependency_binding_type
= SYNTHETIC_VERIFICATION_BINDING

dependency_binding_ref
= stable reviewed synthetic verification binding identity
~~~

Legacy field:

~~~text
capability_binding_ref
~~~

is interpreted as:

~~~text
REAL_CAPABILITY_BINDING-only compatibility alias
~~~

and may not be populated for synthetic execution merely to satisfy old shape.

## 5.3 Admission invariant retained

Every MODE-1/MODE-2 admission still requires:

~~~text
one stable governed dependency binding identity
~~~

The amendment changes representation, not authorization strictness.

## 5.4 Disposition

~~~text
AC-U06-01
= RESOLVED_BY_CONTROLLED_AMENDMENT_A01
~~~

---

# 6. Amendment A02 — normalized owner/policy identities

## 6.1 Problem

RDP-02 requires explicit:

~~~text
f3_owner_policy_ref
question_policy_ref
d04_policy_ref
~~~

RDP-01 predates the complete normalized owner-policy view and only carries part of it.

## 6.2 Refrozen aggregate contract

Canonical U06 admission/dependency view must be able to carry:

~~~text
f3_owner_policy_ref
question_policy_ref?
d04_policy_ref?
~~~

Mode rules:

~~~text
MODE-1
f3_owner_policy_ref = REQUIRED
question_policy_ref = NOT_REQUIRED_BY_DEFAULT
d04_policy_ref = NOT_REQUIRED_BY_DEFAULT

MODE-2
f3_owner_policy_ref = REQUIRED
question_policy_ref = REQUIRED
d04_policy_ref = REQUIRED

MODE-3
f3_owner_policy_ref = REQUIRED
question_policy_ref = NOT_REQUIRED_BY_DEFAULT
d04_policy_ref = NOT_REQUIRED_BY_DEFAULT
~~~

Presence remains subject to RDP-05 applicability and execution profile.

No fake ref is allowed for NOT_APPLICABLE families.

## 6.3 Direct F1 status retained

This amendment does not activate direct F1 routing.

~~~text
F1_CLARIFICATION_ROUTING
= ACTIVATION_BLOCKED_PENDING_CONTROLLED_AMENDMENT
~~~

until the separate executable safety/routing authority is reviewed.

## 6.4 Disposition

~~~text
AC-U06-02
= RESOLVED_BY_CONTROLLED_AMENDMENT_A02
~~~

---

# 7. Amendment A03 — F3 canonical effect binding component

## 7.1 Problem

Frozen Phase-8 / Unit Spec wording defines F3_CANONICAL_EFFECT_ID using:

~~~text
C03 CapabilityBindingRef
~~~

RDP-03 recorded:

~~~text
RDP03-COMPAT-F3-ID-01
~~~

because PROFILE-B uses a synthetic verification binding identity.

## 7.2 Refrozen aggregate identity

The binding component of F3_CANONICAL_EFFECT_ID is normalized to:

~~~text
dependency_binding_type
+ dependency_binding_ref
~~~

All other frozen identity components remain unchanged:

~~~text
consultation_id
+ fact/framing basis identity
+ source Clinical State Version
+ normalized dependency binding identity
+ F3 assessment policy/version
+ assessment trigger/event identity
~~~

## 7.3 Real-profile equivalence

For PROFILE-A:

~~~text
REAL_CAPABILITY_BINDING
+ dependency_binding_ref
~~~

must resolve to the exact real governed C03 P06 CapabilityBindingRef.

Thus the amendment preserves the original real-binding meaning.

## 7.4 Synthetic boundary

For PROFILE-B:

~~~text
SYNTHETIC_VERIFICATION_BINDING
+ synthetic dependency_binding_ref
~~~

is valid only in explicitly authorized structural non-production verification.

It does not establish:
- C03 Quality Gate PASS;
- production binding equivalence;
- live capability activation.

## 7.5 Disposition

~~~text
AC-U06-03
= RESOLVED_BY_CONTROLLED_AMENDMENT_A03
~~~

---

# 8. Amendment A04 — P05 trace compatibility strategy

## 8.1 Problem

Current P05 leaf trace:

~~~text
CapabilityCallTraceRecord
~~~

and current:

~~~text
bindReleases(rule, knowledge)
~~~

assume a shape insufficient for U06:
- Rule/Knowledge may independently be NOT_APPLICABLE;
- Prompt/Model may be conditionally applicable;
- one U06 execution may contain multiple decisions/effects;
- synthetic dependency identity must remain typed.

## 8.2 Selected aggregate strategy

Aggregate closure selects the already-designed RDP-03 option:

~~~text
U06 governed parent trace companion
+
existing CapabilityCallTraceRecord as leaf capability-call trace
~~~

Canonical aggregate trace roles:

~~~text
U06GovernedExecutionTrace
= parent U06 execution/governance trace

CapabilityCallTraceRecord
= leaf record for an actual governed capability invocation
~~~

## 8.3 Typed applicability

Parent trace must represent each dependency/release family as:

~~~text
dependency_family
applicability
ref?
version?
status?
~~~

with:

~~~text
REQUIRED
OPTIONAL
NOT_APPLICABLE
~~~

Rules:

~~~text
REQUIRED + missing ref
→ fail closed

NOT_APPLICABLE
→ ref absent
→ no placeholder
~~~

## 8.4 P05 shared-runtime change classification

This is a:

~~~text
PHYSICAL_SHARED_RUNTIME_IMPACT
~~~

not a remaining semantic contradiction.

Later implementation authorization may choose:
- a companion U06 trace store/service;
- a generalized trace extension;
- another implementation equivalent preserving the frozen parent/leaf semantics.

It may not:
- require fake Rule/Knowledge refs;
- overload Rule/Knowledge fields with Prompt/Model refs;
- turn Trace into Clinical State.

## 8.5 Disposition

~~~text
AC-U06-04
= SEMANTICALLY_RESOLVED
= PHYSICAL_IMPLEMENTATION_IMPACT_REMAINS
~~~

---

# 9. Amendment A05 — delivered/wait cross-store authority

## 9.1 Problem

RDP-03 and RDP-04 intentionally separate:

~~~text
Clinical State
Consultation lifecycle
Runtime state
~~~

Current persistence does not prove a single physical atomic transaction across all three.

## 9.2 Refrozen aggregate choreography

One parent:

~~~text
QUESTION_DELIVERED_WAIT_EFFECT_ID
~~~

owns correlation only.

It decomposes into:

~~~text
A. QUESTION_DELIVERED_CLINICAL_STATE_EFFECT_ID
   owner path: U06/F3 -> K09/P01
   writes:
   Question DELIVERED_TO_USER
   Gap ASKED when F3 path
   pending_question

B. CONSULTATION_WAITING_EFFECT_ID
   owner path: Consultation lifecycle persistence
   writes:
   Consultation WAITING_USER
   + exact parent/question/delivery provenance

C. Runtime wait transition
   Thread -> AWAITING_USER
   only after A and B are authoritative
~~~

Normal order:

~~~text
delivery CONFIRMED
→ A commit/read-back
→ B transition/read-back
→ durable wait checkpoint
→ Runtime AWAITING_USER
→ U07 eligibility
~~~

## 9.3 Reconciliation rule

Temporary:

~~~text
A committed
B pending
~~~

is:

~~~text
DELIVERY_RECONCILIATION_PENDING
~~~

and must not:
- enter U07;
- send a second Question;
- expose normal successful U06 completion.

## 9.4 No fake atomicity

Implementation may use:
- transactional outbox;
- durable saga;
- effect ledger/reconciliation;
- a real shared transaction only if physically proven.

It may not claim cross-store atomicity without evidence.

## 9.5 Disposition

~~~text
AC-U06-05
= RESOLVED_BY_CONTROLLED_AMENDMENT_A05
= PHYSICAL_RECONCILIATION_IMPLEMENTATION_REMAINS
~~~

---

# 10. Amendment A06 — upstream producer availability boundary

## 10.1 Problem

RDP-01 defines several business-legal source authorities whose producers are not implemented.

This is not itself a consumer-contract contradiction.

## 10.2 Refrozen aggregate producer matrix

~~~text
U05_QUESTION_ROUTING
contract = DEFINED
producer = AVAILABLE

A1_PRE_READINESS_ROUTING
contract = DEFINED
real producer = NOT_IMPLEMENTED

POST_F3_SAFETY_BARRIER_ROUTING
contract = DEFINED
real producer = NOT_IMPLEMENTED

CLINICAL_CONTINUATION_ROUTING
contract = DEFINED
real producer = NOT_IMPLEMENTED

F3_REASSESSMENT_ROUTING
contract = DEFINED
real producer = NOT_IMPLEMENTED

F1_CLARIFICATION_ROUTING
contract = DEFINED
activation = BLOCKED_PENDING_SEPARATE_CONTROLLED_AMENDMENT
~~~

## 10.3 Bounded structural verification rule

A future explicitly authorized PROFILE-B implementation/verification slice may use:

~~~text
reviewed synthetic authoritative source fixtures
~~~

for sources whose real producer is unavailable.

This may prove only:
- U06 consumer/admission behavior;
- U06 structural downstream behavior.

It may not claim:
- real upstream producer implementation;
- live loop closure;
- Scheduler/live route activation.

## 10.4 Real-loop rule

Any real end-to-end U06 loop involving a missing producer remains blocked until the exact producer is separately designed/authorized/implemented/verified.

## 10.5 Disposition

~~~text
AC-U06-06
= NO_SEMANTIC_CONFLICT
= REAL_UPSTREAM_IMPLEMENTATION_DEFERRED
~~~

---

# 11. Amendment A07 — direct F1 clarification activation

## 11.1 Aggregate decision

Direct F1 clarification remains business-legal but disabled.

~~~text
F1_CLARIFICATION_ROUTING
= CONTRACT_DEFINED
= BUSINESS_LEGAL
= RUNTIME_DISABLED
= ACTIVATION_BLOCKED_PENDING_SEPARATE_CONTROLLED_AMENDMENT
~~~

## 11.2 Why it is not activated here

The unresolved authority concerns:
- exact entry-safety clearance producer;
- route authority;
- currentness;
- minimal-clarification-only scope;
- fail-safe behavior before ordinary U03/U04 chain.

Those are upstream runtime safety semantics, not a naming compatibility issue.

Aggregate closure must not invent them.

## 11.3 Bounded slice behavior

Future bounded PROFILE-B U06 implementation may:
- keep direct F1 source disabled;
- test consumer behavior with synthetic authoritative F1 fixture where RDP-06 explicitly permits.

It may not expose a live F1→U06 path.

## 11.4 Disposition

~~~text
AC-U06-07
= EXPLICITLY_DEFERRED
= DOES_NOT_BLOCK_BOUNDED_PROFILE_B_READINESS_REEVALUATION
= BLOCKS_DIRECT_F1_RUNTIME_ACTIVATION
~~~

---

# 12. Amendment A08 — PROFILE-A vs PROFILE-B scope split

## 12.1 Current dependency reality

Current repository evidence still shows:

~~~text
PROFILE-A REAL_GOVERNED_C03
= BLOCKED

real D04 clinical policy
= NOT_APPROVED / NOT_ACTIVE

real patient delivery
= BLOCKED
~~~

## 12.2 Aggregate implementation-scope posture

Aggregate closure defines two separate readiness targets.

### Target B — bounded structural non-production slice

~~~text
execution_profile
= SYNTHETIC_STRUCTURAL_NONPROD

delivery_profile
= SYNTHETIC_NONLIVE_DURABLE_DELIVERY
~~~

May later be considered by Implementation Readiness Re-Evaluation if:
- all aggregate amendments here are accepted/refrozen;
- physical implementation impacts are enumerated;
- synthetic scope guards remain fail-closed;
- RDP-06 verification contract remains applicable.

### Target A — real governed clinical/patient-facing slice

Remains:

~~~text
NOT_READY
~~~

until real C03/D04/content/delivery/production gates pass.

## 12.3 No fallback

Forbidden:

~~~text
real dependency fails
→ silently use synthetic dependency

real delivery unavailable
→ silently use synthetic delivery
~~~

## 12.4 Disposition

~~~text
AC-U06-08
= RESOLVED_BY_EXPLICIT_SCOPE_SPLIT
~~~

---

# 13. Amendment A09 — RDP-06 authority after aggregate refreeze

## 13.1 Problem

RDP-06 binds exact contract authority.

Any semantic amendment changes the authority package.

## 13.2 Refrozen verification rule

After this aggregate amendment is accepted:

~~~text
U06 Aggregate Compatibility Review / Controlled Amendment
~~~

becomes a required authority artifact in future:

~~~text
u06-contract-manifest.json
~~~

RDP-06 expectation oracle and fixture review must bind the aggregate amendment digest.

If an existing oracle/fixture manifest was created before this amendment:

~~~text
it is stale for authoritative implementation verification
~~~

unless independently reviewed and proven semantically equivalent.

## 13.3 No case deletion by aggregate closure

The aggregate amendment does not delete any required:

~~~text
U06-EV-001..106
U06-CW-01..09
U06-HG-001..003
U06-VG-001..010
~~~

If future amendment changes a case to NOT_APPLICABLE, it requires a separate explicit reviewed RDP-06 amendment.

## 13.4 Disposition

~~~text
AC-U06-09
= RESOLVED_BY_CONTROLLED_AMENDMENT_A09
~~~

---

# 14. Canonical aggregate admission view

After A01/A02, the canonical semantic admission/dependency view includes at least:

~~~text
consultation_id
cdp_id
u06_mode

source_authority_type
source_authority_ref
source_consequence

authoritative Clinical State refs/currentness
Gate / Safety / action permission refs as applicable

dependency_binding_type
dependency_binding_ref
expected_capability_role

f3_owner_policy_ref
question_policy_ref?
d04_policy_ref?

release_applicability entries
Prompt/Model/Tool/Skill applicability as applicable

execution_profile

canonical_event_ref
business_event_identity
correlation_id
trace_id
~~~

Legacy names may exist only as compatibility adapters where their semantics are unambiguous.

---

# 15. Canonical aggregate effect/trace identity rules

After aggregate amendment:

~~~text
F3_CANONICAL_EFFECT_ID
uses normalized dependency binding identity

Question selection effects
preserve normalized dependency + policy identities

Delivery effects
remain independent from C03 binding identity

U06_TRACE_ID
remains U06 execution trace identity
!= effect identity
!= authorization

P01 StatePatch capability identity
remains state-write authorization identity
!= C03 dependency binding

physical StatePatch producer
remains lower-case runtime service identity
!= logical unit id U06
~~~

No identity class may substitute for another merely because values are available.

---

# 16. Canonical aggregate trace model

Refrozen trace hierarchy:

~~~text
U06GovernedExecutionTrace
  ├─ admission refs
  ├─ dependency applicability
  ├─ C03 CapabilityCallTraceRecord when real governed invocation exists
  ├─ F3 owner decision
  ├─ D04 decision
  ├─ Question selection decision
  ├─ effect/proposal/commit refs
  ├─ delivery intent/attempt/receipt/confirmation refs
  ├─ Consultation wait effect
  ├─ Runtime checkpoint/wait refs
  └─ failure/no-progress refs
~~~

Synthetic PROFILE-B must be explicitly marked synthetic and may use a bounded synthetic leaf trace instead of pretending a real P06 binding exists.

---

# 17. Shared-runtime physical impact inventory

The following are compatible in semantics but remain physical implementation impacts requiring later authorization:

~~~text
IMP-U06-AGG-01
normalized dependency binding fields/adapters

IMP-U06-AGG-02
normalized policy refs in admission/dependency view

IMP-U06-AGG-03
P01 exact field permissions for F3/Gap/Question/PendingQuestion

IMP-U06-AGG-04
P01 approved lower-case producer + U06 state-write capability identity

IMP-U06-AGG-05
U06 parent trace / P05 compatibility surface

IMP-U06-AGG-06
Consultation WAITING effect/idempotency/provenance persistence

IMP-U06-AGG-07
delivery intent/ledger/receipt/confirmation persistence

IMP-U06-AGG-08
Runtime wait checkpoint + Thread AWAITING_USER surface

IMP-U06-AGG-09
U07ResumeEligibility projection

IMP-U06-AGG-10
synthetic C03/D04/delivery adapters + synthetic scope authorization

IMP-U06-AGG-11
upstream real source producers where a later real loop requires them

IMP-U06-AGG-12
RDP-06 verifier/oracle/fixture/evidence infrastructure
~~~

None are authorized by this document.

---

# 18. Frozen-artifact amendment inventory and precedence register

The aggregate amendment does not silently edit historical documents.

It explicitly supersedes/refines only the claims registered below.

## 18.1 Amendment precedence

For U06 semantic authority, precedence is:

~~~text
1. latest independently PASSed U06 Aggregate Compatibility Amendment semantic head

2. U06-RDP-01..06 semantic heads
   as modified by the registered aggregate amendments

3. U06 Unit Spec / frozen Phase clauses
   as modified by the registered aggregate amendments

4. superseded historical wording
   = audit/history only
   = not active authority for U06 implementation or verification
~~~

A later status/provenance-only synchronization commit:
- may record PASS/closure provenance;
- may not alter semantic precedence;
- must identify the semantic PASS head it synchronizes.

Any future semantic amendment requires a new independently reviewed controlled amendment.

## 18.2 U06 Amendment Precedence Register

~~~text
U06_AGGREGATE_AMENDMENT_PRECEDENCE_REGISTER_V0_1
~~~

Required fields per entry:

~~~text
amendment_id
source_artifact
source_semantic_head
source_clause_or_claim
original_semantic_identity
replacement_or_refined_claim
disposition
aggregate_semantic_head
effective_scope
downstream_consumers[]
~~~

Allowed disposition:

~~~text
SUPERSEDED_FOR_U06
REFINED_FOR_U06
UNCHANGED_CONFIRMED
DEFERRED_DISABLED
~~~

Current register:

| Amendment | Source artifact / claim | Disposition | Active aggregate claim | Downstream consumers |
|---|---|---|---|---|
| A01 | RDP-01 capability_binding_ref | REFINED_FOR_U06 | dependency_binding_type + dependency_binding_ref; legacy alias REAL only | Admission, RDP-05, RDP-06 |
| A02 | RDP-01 partial policy refs | REFINED_FOR_U06 | mode-specific f3_owner_policy_ref / question_policy_ref / d04_policy_ref | Admission, RDP-02, RDP-06 |
| A03 | Unit Spec / Phase-8 F3 effect C03 CapabilityBindingRef component | SUPERSEDED_FOR_U06 | normalized dependency binding identity | RDP-03, Effect identity, RDP-06 |
| A04 | current P05 both-release-ref physical assumption for U06 | REFINED_FOR_U06 | U06 parent trace + leaf capability trace; typed applicability; no fake refs | RDP-03, P05, RDP-06 |
| A05 | generic single-step delivered/wait impression | REFINED_FOR_U06 | parent effect + Clinical State child + Consultation child + Runtime wait | RDP-03, RDP-04, RDP-06 |
| A06 | source contract implies producer availability | REFINED_FOR_U06 | contract legality != producer implementation; synthetic fixture only for bounded verification | RDP-01, Readiness, RDP-06 |
| A07 | direct F1 business legality | DEFERRED_DISABLED | business legal but live/runtime activation blocked | RDP-01, RDP-02, RDP-06 |
| A08 | one undifferentiated U06 readiness target | REFINED_FOR_U06 | bounded PROFILE-B target separate from blocked PROFILE-A | Readiness, RDP-05, RDP-06 |
| A09 | RDP-06 authority manifest excludes aggregate amendment | REFINED_FOR_U06 | aggregate amendment digest required in contract/oracle/fixture authority | RDP-06 |
| AC-U06-10 | RDP04-AC-18 recoverable Thread wait wording | SUPERSEDED_FOR_U06 | U07 eligibility requires authoritative Thread = AWAITING_USER + compatible durable checkpoint | RDP-04 acceptance authority, RDP-06 |

Any implementation/verifier must consume this register as active U06 amendment authority.

## 18.3 RDP04-AC-18 explicit supersession

Historical RDP04-AC-18 wording:

~~~text
delivered Question
+ pending Question
+ Consultation WAITING
+ recoverable Thread wait
→ U07 eligibility
~~~

is superseded for U06 by:

~~~text
Question = DELIVERED_TO_USER
+ PendingQuestion = current same Question
+ Consultation = WAITING_USER for the same parent effect
+ durable compatible wait checkpoint
+ Runtime Thread = AWAITING_USER
→ U07ResumeEligibility may be emitted/reattached
~~~

If business WAITING exists but:

~~~text
Thread != AWAITING_USER
~~~

then:

~~~text
WAIT_RUNTIME_RECONCILIATION_REQUIRED
U07 eligibility = absent
~~~

This aggregate wording is the active oracle authority for that scenario.

## 18.4 Summary inventory

| Artifact | Original wording/impact | Aggregate refreeze |
|---|---|---|
| RDP-01 | capability_binding_ref | normalized dependency_binding_type/ref; legacy alias real-only |
| RDP-01 | partial policy refs | normalized F3 owner / Question / D04 policy refs by mode |
| RDP-05 | synthetic binding | accepted only as SYNTHETIC_VERIFICATION_BINDING |
| Unit Spec / Phase 8 | F3 effect uses C03 CapabilityBindingRef | normalized dependency binding identity |
| RDP-05 / P05 | both release refs physical mismatch | U06 parent trace + leaf capability trace; no fake refs |
| RDP-03 | cross-store delivered/wait semantics | parent effect + Clinical State child + Consultation child + Runtime transition |
| RDP-04 AC-18 | recoverable Thread wait can support U07 eligibility | authoritative Thread AWAITING_USER + checkpoint required |
| RDP-01 | source contract vs producer availability | bounded synthetic source fixtures allowed only for structural verification |
| RDP-01 | direct F1 | remains disabled pending separate upstream safety/routing amendment |
| RDP-06 | exact frozen authority | aggregate amendment becomes required manifest authority |

Any implementation must consume this aggregate semantic overlay together with RDP-01..06.

---

# 19. Conflict check — resolved semantic contradictions

After applying A01..A09, aggregate review finds no remaining semantic contradiction in:

~~~text
mode vocabulary
source/mode legality
owner boundaries
dependency profile typing
F3 canonical effect identity
Question selection ownership
Gap ASKED timing
pending Question authority
delivery confirmation authority
Consultation WAITING ownership
Runtime AWAITING ownership
MODE-3 no-mutation rule
trace authority
synthetic vs real profile boundary
verification authority model
~~~

---

# 20. Deliberately unresolved physical choices

The following are not semantic blockers at aggregate design level, but must be resolved before implementation authorization for the relevant slice:

~~~text
exact Java/contract field classes
exact database table/schema for U06 parent trace
exact Consultation wait-effect storage
exact delivery ledger storage
exact Runtime checkpoint representation
exact U06 state-write producer/capability registration
exact synthetic adapter classes
exact Scheduler target-intent implementation
~~~

Implementation Readiness Re-Evaluation must classify each as:
- required before coding;
- implementation design detail within frozen semantic envelope;
- separately authorized shared-runtime amendment.

---

# 21. Bounded PROFILE-B readiness boundary

Aggregate closure may permit the next readiness review to evaluate a bounded PROFILE-B implementation target with these explicit exclusions:

~~~text
real upstream A1 producer activation = excluded
direct F1 runtime activation = excluded
real ClinicalContinuation producer activation = excluded
real F3 reassessment producer activation = excluded

real C03 = excluded
real D04 = excluded
real Question content = excluded
real external delivery = excluded

production store = excluded
real PHI = excluded
live U07 = excluded
production Scheduler/live route = excluded
~~~

Synthetic source fixtures and synthetic stores must satisfy RDP-06 review/isolation rules.

This bounded target proves architecture/runtime mechanics only.

---

# 22. Real-profile blocker ledger

Even after aggregate design PASS:

~~~text
PROFILE-A
= NOT_READY
~~~

Remaining real-profile blockers include at least:
- governed real C03 implementation + Quality Gate;
- approved D04 policy;
- approved F3/Question policies;
- patient-safe Question content/rendering;
- real delivery policy/adapter/privacy/consent;
- real upstream producers for the target loop;
- production environment authorization;
- real-patient clinical evaluation.

These are not hidden by aggregate closure.

---

# 23. Aggregate verification obligations

Future RDP-06 implementation verification must prove the amendment package itself.

This controlled amendment does not renumber or silently expand U06-EV-001..106.

It freezes a separate mandatory namespace:

~~~text
U06-AGG-V-001 .. U06-AGG-V-012
~~~

All 12 are required aggregate verification subcases.

~~~text
U06-AGG-V-001
synthetic binding does not populate legacy real capability_binding_ref

U06-AGG-V-002
real binding compatibility alias maps exactly to REAL_CAPABILITY_BINDING

U06-AGG-V-003
mode-specific policy refs enforced

U06-AGG-V-004
F3 effect identity changes when normalized binding identity changes

U06-AGG-V-005
synthetic F3 effect never claims real P06 binding

U06-AGG-V-006
NOT_APPLICABLE release family has no fake P05 ref

U06-AGG-V-007
U06 parent trace links leaf capability trace without becoming Clinical State

U06-AGG-V-008
delivery parent/child effect identities remain cross-store consistent

U06-AGG-V-009
direct F1 live source remains disabled in bounded profile

U06-AGG-V-010
missing real upstream producer cannot be relabeled as implemented because fixture exists

U06-AGG-V-011
PROFILE-A cannot fall back to PROFILE-B

U06-AGG-V-012
RDP-06 contract manifest includes this aggregate amendment digest
and applies AC-U06-10 U07 eligibility supersession
~~~

Aggregate evidence schema:

~~~text
U06_AGGREGATE_VERIFICATION_EVIDENCE_V0_1

aggregate_case_id
aggregate_amendment_digest
source_contract_refs[]
expected_claim
observed_claim
evidence_refs[]
pass
~~~

Acceptance overlay:

~~~text
existing RDP-06:
106 / 106 EV PASS
9 / 9 CW PASS
3 / 3 HG PASS
10 / 10 VG PASS

plus:
12 / 12 U06-AGG-V PASS
~~~

The RDP-06 expectation/oracle/fixture review package must bind:
- this aggregate amendment semantic digest;
- the Amendment Precedence Register digest;
- all 12 aggregate verification subcases.

A missing aggregate subcase:

~~~text
= INCOMPLETE
!= PASS
~~~

This does not alter the semantic identity of EV-001..106 and therefore does not require repurposing an existing EV ID.

---

# 24. Aggregate readiness disposition

If independent aggregate review passes, the intended disposition is:

~~~text
BF-U06-RG-01
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-02
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-03
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-04
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-05
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED

BF-U06-RG-06
= CONTRACT_DESIGNED / AGGREGATE_COMPATIBILITY_CLOSED
~~~

This means only:

~~~text
the six readiness-gap contract families are mutually compatible
~~~

It does not mean U06 implementation readiness has passed.

---

# 25. Aggregate-to-Readiness Handoff

After aggregate PASS/refreeze, the next governance step is:

~~~text
U06 Implementation Readiness Re-Evaluation
~~~

The readiness review must evaluate two distinct targets and may not merge their evidence.

## 25.1 Target B — bounded PROFILE-B structural non-production slice

Entry preconditions are all required:

~~~text
AGG-HANDOFF-B01
aggregate amendment semantic verdict = PASS / REFROZEN

AGG-HANDOFF-B02
AC-U06-01..10 = CLOSED

AGG-HANDOFF-B03
no unresolved semantic contradiction across Unit Spec / RDP-01..06 / aggregate amendment

AGG-HANDOFF-B04
direct F1 runtime activation remains DISABLED

AGG-HANDOFF-B05
real upstream producers not implemented
= explicitly OUT_OF_SCOPE for bounded slice
= may be represented only by independently reviewed synthetic authoritative fixtures
= cannot be claimed implemented

AGG-HANDOFF-B06
PROFILE-A real C03 / D04 / real delivery remain disabled
and cannot be used as fallback authority

AGG-HANDOFF-B07
IMP-U06-AGG-01..12 each classified by readiness review as exactly one:
DESIGN_REQUIRED_BEFORE_IMPLEMENTATION
IMPLEMENTATION_DETAIL_WITHIN_FROZEN_CONTRACT
OUT_OF_SCOPE_FOR_BOUNDED_SLICE

AGG-HANDOFF-B08
any item classified DESIGN_REQUIRED_BEFORE_IMPLEMENTATION
must have a reviewed concrete implementation design before READY may be declared

AGG-HANDOFF-B09
RDP-06 verification authority remains active
including:
106 EV
9 CW
3 HG
10 VG
12 U06-AGG-V

AGG-HANDOFF-B10
future oracle/fixture/contract manifest must bind this aggregate amendment semantic digest and precedence register

AGG-HANDOFF-B11
shared-runtime changes may only come from explicitly reviewed/authorized amendment refs and exact allowed paths

AGG-HANDOFF-B12
Implementation Authorization = NOT_GRANTED at readiness-entry time
~~~

The readiness review may conclude:

~~~text
PROFILE-B
= READY
or NOT_READY
~~~

but READY means only:

~~~text
a bounded non-production structural implementation slice
may be eligible for a later explicit Implementation Authorization Decision
~~~

It does not activate code by itself.

## 25.2 Target A — real governed clinical/patient-facing slice

Under current evidence:

~~~text
PROFILE-A
= NOT_READY
~~~

The readiness review must preserve real blockers including:
- real governed C03 + Quality Gate;
- approved D04/F3/Question policy releases;
- real patient-safe question content/rendering;
- real delivery policy/adapter/privacy/consent;
- required real upstream producers;
- production environment and clinical evaluation gates.

Synthetic fixtures/adapters are not substitutes for these blockers.

The readiness review may update PROFILE-A only if new independently governed evidence exists.

## 25.3 Physical impact classification deliverable

The readiness review must produce a table:

~~~text
U06_IMPLEMENTATION_IMPACT_CLASSIFICATION_V0_1

impact_id
classification
required_design_ref?
authorized_scope
owner
blocking_status
rationale
~~~

covering every:

~~~text
IMP-U06-AGG-01..12
~~~

No impact may be silently omitted.

Implementation authorization remains a later explicit decision.

---

# 26. Authorization boundary

This aggregate amendment does not authorize:

~~~text
U06 code implementation
P01/P05 changes
Consultation schema changes
Runtime checkpoint/thread changes
synthetic adapters
real C03/D04
Question content
real delivery
live upstream producer wiring
direct F1 activation
Scheduler activation
merge
production
real-patient traffic
~~~

---

# 27. Independent Aggregate Review Remediation

Initial Independent Aggregate Review:

~~~text
PR #237
review_id = 5288111493
verdict = REVISE_REQUIRED
reviewed_head = 34d62909827dfe5a274b7c13e36e2e68661742e2
~~~

Findings:

~~~text
BF-U06-AGG-IR-01
= AMENDMENT_PRECEDENCE_AND_EXACT_SUPERSESSION_RULE_MISSING

BF-U06-AGG-IR-02
= RDP04_U07_ELIGIBILITY_STALE_ACCEPTANCE_WORDING_NOT_RECONCILED

BF-U06-AGG-IR-03
= AGGREGATE_VERIFICATION_OBLIGATIONS_NOT_MAPPED_TO_FROZEN_RDP06_IDENTITIES
~~~

Remediation:

1. added U06_AGGREGATE_AMENDMENT_PRECEDENCE_REGISTER_V0_1 and explicit authority precedence;

2. added A10 and explicitly superseded RDP04-AC-18 recoverable-wait wording with authoritative Thread = AWAITING_USER + compatible checkpoint;

3. froze mandatory U06-AGG-V-001..012 namespace and acceptance overlay requiring 12/12 PASS in addition to existing RDP-06 thresholds.

Targeted Independent Aggregate Re-Review:

~~~text
review_id = 5288123097
verdict = REVISE_REQUIRED
reviewed_head = 394d4ac8350775d457f99008e3998a54392f58e0
~~~

Additional findings:

~~~text
BF-U06-AGG-TR-01
= AMENDMENT_ID_SET_INCONSISTENT

BF-U06-AGG-TR-02
= IMPLEMENTATION_READINESS_ENTRY_CRITERIA_NOT_EXACT
~~~

Additional remediation:

4. normalized the canonical amendment inventory to AC-U06-01..10, with AC-U06-10 owning the RDP04-AC-18 supersession;

5. added U06 Aggregate-to-Readiness Handoff with exact bounded PROFILE-B and PROFILE-A entry rules;

6. required explicit readiness classification for every IMP-U06-AGG-01..12 before any PROFILE-B READY verdict.

Current:

~~~text
BF-U06-AGG-IR-01
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-AGG-IR-02
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-AGG-IR-03
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-AGG-TR-01
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-AGG-TR-02
= REMEDIATED / RE-REVIEW_PENDING

AC-U06-01..10
= PROPOSED_RESOLUTION

U06 Aggregate Compatibility Review / Controlled Amendment
= REVISED / READY_FOR_SECOND_TARGETED_INDEPENDENT_AGGREGATE_RE_REVIEW

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
~~~

# 28. Revised aggregate verdict

~~~text
U06 Aggregate Compatibility Review / Controlled Amendment
= REVISED / READY_FOR_SECOND_TARGETED_INDEPENDENT_AGGREGATE_RE_REVIEW
~~~

No implementation, shared-runtime modification, direct F1 activation, real C03/D04 activation, external delivery, merge, production, or real-patient authorization is granted.
