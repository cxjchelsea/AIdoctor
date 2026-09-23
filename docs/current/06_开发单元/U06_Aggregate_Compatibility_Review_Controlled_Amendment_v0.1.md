# U06 Aggregate Compatibility Review / Controlled Amendment v0.1

> Parent package: U06 RDP-01..06 PASS design package  
> Parent head: e4110cf0ca85f9443a19ac01a5e448369c05832a  
> Scope: aggregate semantic compatibility + exact controlled amendment inventory  
> Status: PROPOSED / READY_FOR_INDEPENDENT_COMPATIBILITY_REVIEW  
> This package does not authorize implementation, refreeze, merge, live upstream activation, external delivery, production, or real-patient traffic.

---

# 1. Purpose

U06 RDP-01..06 are individually PASS, but individual PASS does not prove aggregate compatibility.

This review asks:

~~~text
Do the six RDP contracts
+ current Phase-8 frozen data semantics
+ current upstream producer boundaries
describe one implementable bounded U06 contract
without forcing implementation code to invent missing semantics?
~~~

The review distinguishes:

~~~text
A. true frozen semantic contradiction
→ controlled amendment required now

B. missing physical implementation surface
→ implementation impact, not a semantic rewrite

C. live/upstream activation not required by bounded initial PROFILE-B scope
→ explicitly disabled/deferred
→ not silently treated as implemented
~~~

---

# 2. Aggregate target scope

The only implementation scope that may become readiness-eligible after aggregate closure is:

~~~text
U06_INITIAL_BOUNDED_IMPLEMENTATION_SCOPE_V1

environment
= NON_PRODUCTION

execution_profile
= SYNTHETIC_STRUCTURAL_NONPROD

delivery_profile
= SYNTHETIC_NONLIVE_DURABLE_DELIVERY

real C03 activation
= DISABLED

real D04 activation
= DISABLED

real patient delivery
= DISABLED

direct F1 live producer activation
= DISABLED

live A1/MODE-1 upstream producer activation
= DISABLED unless separately authorized

live MODE-3 upstream producer activation
= DISABLED unless separately authorized

synthetic authoritative fixtures
= ALLOWED only after explicit implementation authorization
  and only under RDP-06 verification scope
~~~

This scope may exercise the full structural contracts with synthetic/non-patient authority fixtures.

It must not claim:
- clinical correctness;
- real C03 Quality Gate;
- real D04 approval;
- patient wording safety;
- live source producer completion;
- real delivery.

---

# 3. Compatibility findings summary

| Finding | Conflict | Disposition |
|---|---|---|
| U06-AGR-01 | RDP-01 uses capability_binding_ref while RDP-05 normalizes dependency_binding_type/ref | CONTROLLED AMENDMENT REQUIRED |
| U06-AGR-02 | Phase 8 F3_CANONICAL_EFFECT_ID binds C03 CapabilityBindingRef only | CONTROLLED AMENDMENT REQUIRED |
| U06-AGR-03 | direct F1 source is business-legal but executable safety/routing producer is not frozen | DEFER FROM INITIAL BOUNDED SCOPE / MUST REMAIN DISABLED |
| U06-AGR-04 | several real MODE-1/MODE-3 upstream producers are not implemented | IMPLEMENTATION/ACTIVATION IMPACT / synthetic fixture path allowed for bounded verification |
| U06-AGR-05 | RDP-03/RDP-04 require P01/P05/Consultation/Runtime/delivery physical surfaces | ONE REVIEWED SHARED-RUNTIME IMPACT PACKAGE REQUIRED |
| U06-AGR-06 | RDP-06 authority heads become stale after aggregate amendment/refreeze | POST-REFREEZE AUTHORITY REBIND REQUIRED |

No additional Clinical Truth owner conflict was found.

---

# 4. U06-AGR-01 — dependency binding normalization

## 4.1 Conflict

RDP-01 currently uses:

~~~text
capability_binding_ref
~~~

as the mandatory MODE-1/MODE-2 binding field.

RDP-05 freezes the normalized identity:

~~~text
dependency_binding_type
dependency_binding_ref
~~~

where:

~~~text
REAL_CAPABILITY_BINDING
→ dependency_binding_ref = governed P06 C03 CapabilityBindingRef

SYNTHETIC_VERIFICATION_BINDING
→ dependency_binding_ref = stable synthetic verification identity
→ not a real P06 ACTIVE binding
~~~

If unchanged, PROFILE-B must either:
- fabricate a production CapabilityBindingRef; or
- violate RDP-01.

Both are prohibited.

## 4.2 Controlled amendment

RDP-01 canonical inbound/admitted contract is amended to use:

~~~text
dependency_binding_type?
dependency_binding_ref?
~~~

as the authoritative normalized identity.

For MODE-1 / MODE-2:

~~~text
stable governed dependency binding identity
= REQUIRED
~~~

Profile rules:

~~~text
PROFILE-A
dependency_binding_type = REAL_CAPABILITY_BINDING
dependency_binding_ref = real governed P06 C03 CapabilityBindingRef

PROFILE-B
dependency_binding_type = SYNTHETIC_VERIFICATION_BINDING
dependency_binding_ref = stable synthetic verification identity
~~~

Legacy field:

~~~text
capability_binding_ref
~~~

may exist only as:
- a real-binding-specific resolved detail inside U06DependencyBindingView; or
- historical compatibility data.

It is no longer the source-neutral admission identity.

## 4.3 MODE-3 historical binding

MODE-3 does not require a new/current C03 invocation binding by default.

It must carry historical normalized identity:

~~~text
historical_dependency_binding_type
historical_dependency_binding_ref
~~~

plus applicable historical release/policy provenance.

For old real records, the historical normalized ref resolves to the original real CapabilityBindingRef.

No synthetic identity may be reinterpreted as a real historical binding.

---

# 5. U06-AGR-02 — Phase-8 F3 canonical effect identity

## 5.1 Conflict

Current Phase 8 freezes:

~~~text
F3_CANONICAL_EFFECT_ID
=
consultation_id
+ fact/framing basis identity
+ source Clinical State Version
+ C03 CapabilityBindingRef
+ F3 assessment policy/version
+ assessment trigger/event identity
~~~

RDP-03/RDP-05 require source-neutral:

~~~text
dependency_binding_type
+ dependency_binding_ref
~~~

## 5.2 Controlled amendment

Phase 8 is amended to:

~~~text
F3_CANONICAL_EFFECT_ID
=
consultation_id
+ fact/framing basis identity
+ source Clinical State Version
+ dependency_binding_type
+ dependency_binding_ref
+ F3 assessment policy/version
+ assessment trigger/event identity
~~~

Meaning:

~~~text
REAL_CAPABILITY_BINDING
→ ref resolves to the real governed C03 CapabilityBindingRef

SYNTHETIC_VERIFICATION_BINDING
→ ref is a stable synthetic verification identity
→ valid only inside explicitly authorized structural non-production verification
→ no production/live equivalence claim
~~~

Attempt-local ids remain excluded.

## 5.3 Proposal provenance amendment

Phase-8 A1 F3 proposal provenance is amended from mandatory:

~~~text
C03 CapabilityBindingRef
~~~

to:

~~~text
dependency_binding_type
dependency_binding_ref
resolved real capability binding ref when applicable
applicable Rule/Knowledge/Prompt/Model refs
~~~

No fake real binding/release ref is required for typed NOT_APPLICABLE/synthetic cases.

## 5.4 MODE-3 historical fields

Phase-8 F3CurrentVersionRevalidationDecision replaces the source-neutral meaning of:

~~~text
historical_capability_binding_ref
~~~

with:

~~~text
historical_dependency_binding_type
historical_dependency_binding_ref
historical_capability_binding_ref?  // real-binding resolved detail when applicable
~~~

Historical provenance is preserved; it is not rewritten.

---

# 6. U06-AGR-03 — direct F1 clarification source

## 6.1 Current truth

Frozen business loop says:

~~~text
F1 Problem Framing = CLARIFICATION_REQUIRED
→ U06 forms minimal clarification Question
→ only after delivery may WAITING_USER be established
~~~

RDP-01 correctly identifies the missing executable authority:

~~~text
F1ClarificationRoutingEligibility
+ governed safety clearance
~~~

Current upstream producer is not frozen/implemented.

## 6.2 Aggregate disposition

For the initial bounded U06 implementation:

~~~text
F1_CLARIFICATION_ROUTING
= CONTRACTUALLY_DEFINED
= BUSINESS_LEGAL
= RUNTIME_DISABLED
= OUT_OF_INITIAL_BOUNDED_IMPLEMENTATION_SCOPE
~~~

Therefore this aggregate package does not invent a new entry-safety authority.

Verification requirement remains:

~~~text
consumer rejects live/direct raw F1 invocation
business-legal schema may be tested with synthetic authoritative fixture
no real patient clarification delivery
~~~

A future direct-F1 activation requires a separate controlled amendment that freezes:
- exact producer Owner;
- safety-clearance authority;
- routing eligibility identity;
- replay/currentness;
- environment activation.

This deferred activation does not block bounded PROFILE-B structural implementation readiness.

---

# 7. U06-AGR-04 — MODE-1 / MODE-3 real upstream producers

Existing Phase 5/6/9 business semantics already define:
- A1 pre-readiness eligibility;
- post-F3 Safety barrier;
- F3 current-version revalidation;
- continuation revalidation;
- reassessment return.

Current missing items are primarily physical producer/runtime surfaces.

Aggregate disposition:

~~~text
live upstream producer activation
!= required for initial bounded PROFILE-B implementation
~~~

Initial structural verification may use independently reviewed synthetic authoritative fixtures satisfying RDP-01 source contracts.

It must prove:
- fixture authority type;
- currentness;
- Gate/permission semantics;
- no live upstream invocation;
- no production routing activation.

No amendment here converts synthetic fixtures into real producers.

Future live source activation requires separate implementation authorization against the already frozen source contract.

---

# 8. U06-AGR-05 — shared-runtime physical impact package

RDP-03/RDP-04 are semantically compatible but require implementation surfaces that do not exist today.

The aggregate impact package is frozen as:

~~~text
U06_SHARED_RUNTIME_IMPACT_PACKAGE_V1
~~~

Candidate impact families:

~~~text
SR-01 U06 semantic Clinical State adapter/schema
SR-02 exact P01 field permissions
SR-03 approved lower-case physical StatePatch producer
SR-04 U06 P01 state-write capability identity/version
SR-05 stable effect/proposal/patch identity adapter

SR-06 U06 governed parent P05 trace
SR-07 optional/NOT_APPLICABLE dependency trace entries
SR-08 real/synthetic C03 leaf-trace compatibility

SR-09 durable DeliveryIntent/Ledger
SR-10 Transport Adapter SPI
SR-11 synthetic zero-network transport adapter
SR-12 Delivery Confirmation Resolver
SR-13 one-active-delivery authority / rebinding record

SR-14 Consultation WAITING effect/idempotency/provenance persistence
SR-15 Runtime wait checkpoint / Thread AWAITING_USER surface
SR-16 U07ResumeEligibility projection
SR-17 failure handoff
SR-18 SyntheticDeliveryScopeAuthorization

SR-19 RDP-06 verification/oracle/fixture/environment evidence surfaces
~~~

This package does not authorize those changes.

Before implementation authorization, every changed shared-runtime file must be mapped to one or more SR ids and to an approved amendment/review ref.

RDP-06 then computes actual shared-runtime changes from exact Git diff.

---

# 9. Cross-store ownership remains unchanged

No aggregate amendment changes:

~~~text
Question / Gap / PendingQuestion
→ Clinical State / P01

Consultation WAITING_USER
→ Consultation lifecycle persistence

Thread AWAITING_USER
→ Runtime

U07 eligibility
→ routing/resume prerequisite only
~~~

No global cross-store ACID transaction is invented.

RDP-04 reconciliation choreography remains authoritative.

---

# 10. U06-AGR-06 — RDP-06 authority rebind

RDP-06 currently names pre-amendment semantic heads.

After explicit aggregate refreeze:

~~~text
u06-contract-manifest
oracle review
fixture review
authoritative verification
~~~

must bind the exact re-frozen heads.

Required:

~~~text
RDP06_AGGREGATE_AUTHORITY_REBIND
~~~

Before any verifier/oracle acceptance:

~~~text
old pre-amendment authority digest
!= accepted current authority digest
~~~

Any future verification asset built against the old heads becomes stale.

This is a post-refreeze provenance synchronization step, not permission to alter RDP-06 expected business outcomes.

---

# 11. Exact controlled-amendment file inventory

Semantic controlled amendment scope:

1. docs/current/06_开发单元/U06_RDP01_Consumer_Inbound_Admission_Contract_v0.1.md
   - normalized dependency binding identity
   - admission identity includes normalized dependency binding
   - MODE-3 historical normalized binding identity

2. docs/current/06_开发单元/U06_RDP05_Capability_Dependency_Applicability_Contract_v0.1.md
   - C03 result provenance normalized to dependency_binding_type/ref
   - real capability_binding_ref remains conditional resolved detail
   - no applicability/profile/Quality-Gate change

3. docs/current/08_契约与数据/Contract与数据语义设计.md
   - Phase-8 F3_CANONICAL_EFFECT_ID binding component
   - A1 F3 proposal binding provenance
   - MODE-3 historical binding provenance
   - U06-compatible Capability Result conditional normalized binding
   - U06-compatible Question provenance conditional normalized binding

4. docs/current/06_开发单元/可验证开发单元拆分_V1.md
   - MODE-1 dependency binding normalization
   - direct F1 -> U06 clarified as business destination + runtime-gated edge
   - current initial bounded scope keeps F1_CLARIFICATION_ROUTING disabled

5. docs/current/07_能力设计/按开发单元的Capability设计.md
   - U06 C03 consumption normalized to dependency binding identity
   - real P06 C03 binding resolved only for REAL_CAPABILITY_BINDING

6. docs/current/09_Runtime与技术架构/Runtime与技术架构设计_V1.md
   - U06 runtime dependency resolution normalized
   - direct F1 invocation requires authoritative eligibility/safety/route proof
   - current initial bounded scope keeps direct F1 runtime disabled

Compatibility/provenance synchronization scope:

7. docs/current/06_开发单元/U06_RDP03_State_Ownership_K09_P01_Mutation_Trace_Contract_v0.1.md
   - RDP03-COMPAT-F3-ID-01 resolution provenance only
   - no mutation/ownership semantic change

Post-refreeze-only authority synchronization:

8. docs/current/06_开发单元/U06_RDP06_Verification_Durable_Evidence_Contract_v0.1.md
   - exact authority-head/digest rebind only after explicit refreeze decision

Review package:

9. docs/current/06_开发单元/U06_Aggregate_Compatibility_Review_Controlled_Amendment_v0.1.md

Not amended in this package:
- RDP-02;
- RDP-04;
- Phase 4;
- Phase 5;
- U04 contracts;
- U01 implementation/source code;
- runtime/source code.

If review proves any of those must change semantically:

~~~text
STOP
→ expand controlled amendment scope
→ repeat independent compatibility review
~~~

---

# 12. Aggregate compatibility invariants

Must remain unchanged:

~~~text
Clinical Truth
!= Capability Result
!= Runtime State
!= Trace

F3
= Gap / Question business owner

D04
= deterministic continue/stop owner only

D03
= Clinical Readiness owner

U04
= Safety Gate owner

C03
= candidate capability, not state owner

Question SELECTED
!= DELIVERED_TO_USER

DELIVERED
before WAITING_USER

business WAITING
before Thread AWAITING_USER

PROFILE-A
= BLOCKED

PROFILE-B
= synthetic/non-patient/non-live only
~~~

---

# 13. Readiness interpretation after amendment review

Even if this aggregate amendment receives Compatibility Review PASS:

~~~text
U06 Implementation Readiness
= NOT_READY
~~~

until explicit aggregate re-freeze occurs.

After explicit re-freeze:

~~~text
RG-01..RG-06
may be marked aggregate-closed against one exact baseline
~~~

Then and only then:

~~~text
U06 Implementation Readiness Re-Evaluation
= permitted
~~~

Possible readiness decision must still be bounded.

A later READY result may only mean:

~~~text
READY_FOR_BOUNDED_NONPROD_PROFILE_B_IMPLEMENTATION_REVIEW
~~~

unless separate gates authorize more.

It cannot imply:
- PROFILE-A;
- direct F1 live routing;
- real C03/D04;
- real patient delivery;
- production.

---

# 14. Controlled amendment review questions

Independent compatibility review must answer:

1. Does AGR-01 remove the RDP-01/RDP-05 conflict without weakening binding identity?
2. Does AGR-02 preserve exact F3 replay identity for both real and synthetic profiles?
3. Does normalized synthetic identity remain impossible to consume as production C03 authority?
4. Is direct F1 safely disabled rather than implicitly activated?
5. Can bounded PROFILE-B be structurally implemented with reviewed synthetic authority fixtures without claiming live producer implementation?
6. Are RDP-03/RDP-04 semantic owners unchanged?
7. Is every shared-runtime physical impact enumerable and reviewable?
8. Can RDP-06 compute shared-runtime diff and bind post-refreeze contract heads?
9. Are Phase 6/7/8/9 now mutually consistent about normalized dependency binding and direct-F1 runtime gating?
10. Is Phase 4/5 left unchanged because no additional semantic change is required there?
11. Does the package avoid authorizing implementation/refreeze itself?

---

# 15. Current verdict

~~~text
U06 Aggregate Compatibility Review / Controlled Amendment
= PROPOSED / READY_FOR_INDEPENDENT_COMPATIBILITY_REVIEW

U06-AGR-01
= AMENDMENT_CANDIDATE

U06-AGR-02
= AMENDMENT_CANDIDATE

U06-AGR-03
= DEFERRED / RUNTIME_DISABLED_FOR_INITIAL_SCOPE

U06-AGR-04
= LIVE_ACTIVATION_DEFERRED / SYNTHETIC_FIXTURE_VERIFICATION_ONLY

U06-AGR-05
= SHARED_RUNTIME_IMPACT_PACKAGE_DEFINED / NOT_AUTHORIZED

U06-AGR-06
= POST_REFREEZE_REBIND_REQUIRED

Aggregate re-freeze
= NOT_AUTHORIZED

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
~~~


---

# 16. Independent Compatibility Review Remediation

Initial Independent Compatibility Review:

~~~text
PR #236
review_id = 5288060818
verdict = REVISE_REQUIRED
reviewed_head = e24a8a79f89e866b85231cdbc13f0758985fcac6
~~~

Findings:

~~~text
BF-U06-AGR-IR-01
= ADMISSION_ID_DOES_NOT_BIND_NORMALIZED_DEPENDENCY_IDENTITY

BF-U06-AGR-IR-02
= PHASE8_QUESTION_AND_CAPABILITY_RESULT_STILL_ASSUME_REAL_CAPABILITY_BINDING

BF-U06-AGR-IR-03
= RDP05_C03_RESULT_CONTRACT_NOT_NORMALIZED

BF-U06-AGR-IR-04
= DIRECT_F1_DEFERRED_SCOPE_CONFLICT_WITH_PHASE6_EXECUTABLE_EDGE

BF-U06-AGR-IR-05
= EXACT_AMENDMENT_INVENTORY_INCOMPLETE_AFTER_REQUIRED_SCOPE_EXPANSION
~~~

Remediation applied:

1. added normalized dependency binding identity to U06_ADMISSION_ID where applicable;

2. extended Phase-8 Capability Result and Question provenance so PROFILE-B can carry typed synthetic dependency identity without a real CapabilityBindingRef;

3. normalized RDP-05 C03 result provenance and required equality with admitted dependency binding;

4. amended Phase 6 and Phase 9 so direct F1 remains a business-legal destination but executable invocation requires authoritative F1ClarificationRoutingEligibility + governed safety clearance + route authorization; current bounded scope keeps it disabled;

5. amended Phase 6/7/9 U06 C03 consumption to use dependency_binding_type/ref and resolve a real CapabilityBindingRef only for REAL_CAPABILITY_BINDING;

6. expanded the exact amendment inventory accordingly.

Current:

~~~text
BF-U06-AGR-IR-01
= REMEDIATED / TARGETED_REVIEW_PENDING

BF-U06-AGR-IR-02
= REMEDIATED / TARGETED_REVIEW_PENDING

BF-U06-AGR-IR-03
= REMEDIATED / TARGETED_REVIEW_PENDING

BF-U06-AGR-IR-04
= REMEDIATED / TARGETED_REVIEW_PENDING

BF-U06-AGR-IR-05
= REMEDIATED / TARGETED_REVIEW_PENDING

U06 Aggregate Compatibility Amendment
= REVISED / READY_FOR_TARGETED_COMPATIBILITY_RE_REVIEW

Aggregate Re-Freeze
= NOT_AUTHORIZED

U06 Implementation Readiness
= NOT_READY
~~~
