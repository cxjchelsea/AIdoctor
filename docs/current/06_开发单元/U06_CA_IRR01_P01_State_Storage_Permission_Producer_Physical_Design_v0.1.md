# CA-U06-IRR-01 — P01 U06 State Storage / Permission / Producer Authorization Physical Design v0.1

> Parent readiness finding: **BF-U06-IRR-01**  
> Readiness baseline: **498d8ae086c7d9fd0e33017360d07ea1ba89ee73**  
> Aggregate authority: **eb8c52a4e2da1feab1297d820fef0f033ac27698**  
> Scope: **PROFILE-B bounded synthetic structural implementation only**  
> Status: **PASS / PHYSICAL_DESIGN_COMPLETE / PENDING_COMBINED_READINESS_REEVALUATION**  
> This document does not authorize implementation or any production/live Clinical State mutation.

---

# 1. Purpose

This controlled physical design closes the remaining bounded PROFILE-B ambiguity for IMP-U06-AGG-03 and IMP-U06-AGG-04.

It does not redesign RDP-03 semantics. It selects one concrete non-production path that exercises the real StateCommitter mechanical boundary while performing zero production-store writes.

---

# 2. Current constraints

Current StateCommitter supports ADD / REPLACE / REMOVE, base_version, idempotency, CapabilityPolicyPort, FieldPermissionPort, ConsentPolicyPort, SourceValidationPort and StateRepositoryPort.

Current StatePatchBoundaryValidator requires lower-case service-name producer, opaque capability id, semver capability version and frozen source vocabulary.

Current ClinicalCdpStateRepositoryAdapter requires every intermediate JSON-object parent to already exist.

Current SyntheticVersionedStateRepository provides a versioned in-memory StateRepositoryPort, atomic patch application, snapshot read-back, no Spring wiring, no production CDP persistence and no network. Its JSON pointer applier also requires parent paths to exist.

---

# 3. Selected physical strategy for PROFILE-B

For bounded PROFILE-B, the authoritative P01 repository is:

~~~text
SyntheticVersionedStateRepository
~~~

not ClinicalCdpStateRepositoryAdapter.

Therefore:

~~~text
PROFILE-B U06 P01 writes
→ synthetic state store only

production CDP write count
= 0
~~~

PROFILE-A real CDP storage remains OUT_OF_SCOPE_FOR_THIS_CA / NOT_READY.

---

# 4. Synthetic state-store binding

Define:

~~~text
U06SyntheticStateStoreBinding

synthetic_state_store_ref
execution_profile = SYNTHETIC_STRUCTURAL_NONPROD
consultation_id
cdp_id
fixture_manifest_ref
fixture_review_ref
aggregate_authority_ref
created_at
~~~

The same binding ref is consumed by admission/profile guard, state read, state write guard, RDP-06 evidence and SyntheticDeliveryScopeAuthorization where delivery is exercised.

---

# 5. Synthetic P01 execution context

Define one executable binding object:

~~~text
U06SyntheticP01ExecutionContext

synthetic_state_store_ref
consultation_id
cdp_id
execution_profile

state_repository_instance
state_read_port
state_committer

capability_policy_port
field_permission_port
source_validation_port
consent_policy_port

context_fingerprint
~~~

Construction rule:

~~~text
state_repository_instance
= the exact SyntheticVersionedStateRepository instance

state_read_port
= adapter over that exact repository instance

state_committer.StateRepositoryPort
= that exact repository instance
~~~

The context factory must reject:
- a read adapter backed by a different repository instance;
- a StateCommitter backed by a different StateRepositoryPort;
- a store ref that does not identify this exact context binding;
- consultation/cdp/profile mismatch with admission.

Required invariant:

~~~text
state_read_store_ref
=
state_commit_store_ref
=
admitted synthetic_state_store_ref
~~~

A PROFILE-B U06 execution may receive State read and State commit services only through this execution context.

This prevents read-store A / write-store B split-brain even when both stores are synthetic.

RDP-06 evidence must capture:
- synthetic_state_store_ref;
- read adapter store ref;
- StateCommitter repository store ref;
- equality result.

---

# 6. Initial synthetic state shape

The reviewed fixture must pre-materialize only structural containers:

~~~text
patient_state = {
  information_gaps = {},
  questions = {}
}
~~~

Initially absent:

~~~text
patient_state.f3_gap_assessment
patient_state.pending_question
~~~

The empty containers are STRUCTURAL_STORAGE_SHAPE only. They are not F3, Gap or Question truth.

They are created by fixture/state-store bootstrap, not by StatePatch business mutation.

---

# 7. Generic parent auto-materialization is rejected for PROFILE-B

This CA does not modify ClinicalCdpStateRepositoryAdapter to auto-create missing intermediate maps.

Doing so would alter shared behavior for all StatePatch consumers and introduce new implicit-write semantics.

Malformed PROFILE-B fixture with missing required structural parent:

~~~text
→ fail closed
→ no implicit materialization
~~~

---

# 8. U06 state read authority

Define:

~~~text
U06StateReadPort
readCurrent(U06SyntheticStateStoreBinding)
→ U06AuthoritativeStateView
~~~

PROFILE-B adapter:

~~~text
SyntheticU06StateReadAdapter
~~~

backed by SyntheticVersionedStateRepository.snapshot(cdp_id).

The immutable view includes at least:

~~~text
clinical_state_version
f3_gap_assessment?
information_gaps
questions
pending_question?
~~~

All ADD-vs-REPLACE and currentness decisions use this read-back.

---

# 9. Physical producer identity

PROFILE-B StatePatch identities are frozen as:

~~~text
StatePatch.producer = u06-runtime
StatePatch.envelope.producer = u06-runtime

StatePatch.envelope.capability_id = u06-state-writer
StatePatch.envelope.capability_version = 1.0.0
~~~

u06-runtime is a physical service producer identity.

u06-state-writer@1.0.0 is a P01 mechanical write authorization identity.

Neither is a C03 dependency binding, Question capability identity or logical owner identity.

---

# 10. Pre-P01 state-write guard

Define U06StateWriteAuthorityGuard.

It validates before StatePatch construction:

~~~text
execution_profile
synthetic_state_store_ref
consultation_id
cdp_id
admission_id
logical_effect_id
proposal_id
dependency_binding_type/ref
state-write capability id/version
current state view
operation intents
~~~

PASS requires:
- execution_profile = SYNTHETIC_STRUCTURAL_NONPROD;
- state-store binding matches admission;
- capability = u06-state-writer@1.0.0;
- dependency binding is provenance only, never P01 authority;
- all paths are on the exact U06 allowlist;
- operation source = RULE_DERIVED;
- no production repository/runtime/recipient binding.

Failure means no StatePatch and no P01 call.

---

# 11. Exact field-permission allowlist

The bounded U06 FieldPermissionPort may authorize only:

~~~text
/patient_state/f3_gap_assessment
/patient_state/information_gaps/{gap_id}
/patient_state/questions/{question_id}
/patient_state/pending_question
~~~

gap_id and question_id must satisfy existing opaque-id constraints and U06 identity rules.

Everything else is denied.

Explicitly denied include triage, ddx, evidence_graph, workup_plan, management_plan, health_state_assessment, wellness_plan, conclusion_package and observations roots.

---

# 12. Operation matrix

| Logical state | ADD | REPLACE | REMOVE |
|---|---:|---:|---:|
| f3_gap_assessment | when absent | new lawful effect when present | not in bounded slice |
| information_gaps/{gap_id} | new gap | same-gap authoritative transition | not in bounded slice unless separately frozen |
| questions/{question_id} | initial authoritative Question | same Question lifecycle transition | not in bounded slice |
| pending_question | when absent | only with explicit stale/superseded replacement authorization | later resume/lifecycle owner only |

Implementation must never choose REPLACE merely because ADD failed.

Existence comes from authoritative pre-read.

---

# 13. Pending-question replacement authorization

Define:

~~~text
U06PendingQuestionReplacementAuthorization

authorization_id
consultation_id
existing_pending_question_id
existing_pending_effect_id
replacement_question_id
replacement_parent_effect_id
stale_or_superseded_evidence_ref
source_clinical_state_version
decision
trace_refs[]
~~~

Allowed decision:

~~~text
REPLACE_ALLOWED
REPLACE_DENIED
~~~

Without REPLACE_ALLOWED, a different current pending Question is conflict.

Ordinary first bounded delivery should use:

~~~text
pending_question absent
→ ADD
~~~

---

# 14. Source and sensitivity

All U06 owner-derived StatePatch operations use:

~~~text
source = RULE_DERIVED
~~~

PROFILE-B uses:

~~~text
sensitivity = INTERNAL_SENSITIVE
~~~

This does not claim patient PHI.

PROFILE-A sensitivity is deferred.

---

# 15. Controlled-value encoding

Define U06StateValueCodec.

All PROFILE-B values must fit the current controlled-value boundary:
- scalar;
- scalar list;
- one flat object level;
- maximum 32 map keys;
- no nested map.

Cross-object structure is represented by refs.

If a frozen semantic value cannot fit:

~~~text
STOP
→ shared-contract/value-boundary amendment required
~~~

JSON-stringifying nested business objects to bypass the boundary is prohibited.

---

# 16. Patch factory

Define U06StatePatchFactory.

It consumes:
- approved K09 proposal;
- U06StateWriteAuthorityGuard PASS;
- authoritative current state view.

It:
- chooses ADD/REPLACE from the matrix;
- encodes via U06StateValueCodec;
- preserves stable proposal/patch/idempotency identities;
- uses producer u06-runtime;
- uses capability u06-state-writer@1.0.0;
- uses source RULE_DERIVED.

It may not recompute F3/D04/selection, infer new truth, rewrite base_version or retry conflict with a refreshed base version.

---

# 17. Synthetic P01 policy adapters

For PROFILE-B only:

~~~text
U06SyntheticCapabilityPolicyPort
→ authorize only u06-state-writer@1.0.0

U06SyntheticFieldPermissionPort
→ exact U06 path allowlist only

U06SyntheticSourceValidationPort
→ authorize only RULE_DERIVED

U06SyntheticConsentPolicyPort
→ fixture-scoped synthetic authorization only
~~~

All must be non-production-only, fail closed on unknown inputs and expose evidence counters.

---

# 18. Replay and conflict

Order:

~~~text
1. read current synthetic state
2. reconcile exact effect/proposal/patch replay evidence
3. exact committed replay → reattach prior result
4. otherwise validate base_version/currentness
5. construct patch
6. StateCommitter.commit
7. authoritative synthetic read-back
~~~

On version conflict:

~~~text
no blind base_version refresh
no new patch identity for same effect
→ typed conflict/reassessment
~~~

---

# 19. PROFILE-B storage boundary

Allowed:

~~~text
SyntheticVersionedStateRepository
non-production U06 evidence stores
~~~

Forbidden for bounded PROFILE-B:

~~~text
ClinicalCdpStateRepositoryAdapter
production CDPRepository
real patient CDP
legacy direct CDPManager.updateCDP
~~~

RDP-06 must prove production_store_write_count = 0.

---

# 20. Future PROFILE-A impact

Before PROFILE-A can become READY, a separate real-profile design must decide:
- structural parent materialization for real CDPs;
- real U06 field permissions;
- real state-write capability registration;
- real consent/source/sensitivity;
- migration/backfill compatibility.

Synthetic adapters may not be copied into real wiring.

---

# 21. Candidate implementation surface

Future authorization may permit a bounded U06-local surface:

~~~text
runtime/u06/state/
  U06StateReadPort
  SyntheticU06StateReadAdapter
  U06StateWriteAuthorityGuard
  U06StatePatchFactory
  U06StateValueCodec
  U06PendingQuestionReplacementAuthorization

runtime/u06/state/synthetic/
  U06SyntheticStateStoreBinding
  U06SyntheticCapabilityPolicyPort
  U06SyntheticFieldPermissionPort
  U06SyntheticSourceValidationPort
  U06SyntheticConsentPolicyPort
  U06SyntheticStateFixtureFactory
~~~

StateCommitter core, StatePatchBoundaryValidator and ClinicalCdpStateRepositoryAdapter do not need semantic modification for PROFILE-B.

---

# 22. Verification obligations

~~~text
IRR01-V01 structural containers exist, business truth absent
IRR01-V02 producer = u06-runtime
IRR01-V03 capability = u06-state-writer@1.0.0
IRR01-V04 C03 binding never becomes P01 capability
IRR01-V05 only exact U06 paths allowed
IRR01-V06 unknown paths fail closed
IRR01-V07 source = RULE_DERIVED
IRR01-V08 synthetic repository only / production writes = 0
IRR01-V09 malformed missing parent fails closed
IRR01-V10 ADD/REPLACE follows authoritative pre-read
IRR01-V11 different pending Question conflicts
IRR01-V12 nested value cannot be stringified to bypass boundary
IRR01-V13 version conflict does not refresh blindly
IRR01-V14 exact replay causes zero second mutation
IRR01-V15 read-store ref = commit-store ref = admitted store ref
IRR01-V16 mismatched synthetic read/write repositories fail context construction
~~~

---

# 23. Readiness finding disposition

If independent review passes:

~~~text
BF-U06-IRR-01
= PHYSICAL_DESIGN_COMPLETE / PENDING_COMBINED_READINESS_REEVALUATION
~~~

---

# 24. Authorization boundary

This design authorizes no code, shared-runtime modification, production state write, merge or real-patient traffic.

---

# 25. Draft verdict

~~~text
Independent Combined Review:
review_id = 5288434120
verdict = REVISE_REQUIRED
reviewed_head = 0533fa05a896b8588983371038218902a274d208

Targeted Independent Combined Re-Review:
review_id = 5288454931
verdict = PASS
reviewed_head = 9c0893a2892a8b08a9c03ce3a8f214397d893df4

CA-U06-IRR-01
= PASS

BF-U06-CA-IRR01-IR-01
= CLOSED

BF-U06-IRR-01
= PHYSICAL_DESIGN_COMPLETE / PENDING_COMBINED_READINESS_REEVALUATION
~~~
