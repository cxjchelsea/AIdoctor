# U06-RDP-03 State Ownership / K09-P01 Mutation / Trace Contract v0.1

> Unit: **U06 — F3 Gap评估 / Question选择 / F3当前版本重验证**  
> Readiness blocker: **BF-U06-RG-03**  
> Parent semantic design: **U06-RDP-02 F3 Owner / D04 Question Policy Contract**  
> Parent head: **23fd6764439f55c053691daa500fede5030411ec**  
> Runtime repository basis: **main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8**  
> Scope: **STATE OWNERSHIP / K09-P01 MUTATION / REPLAY / TRACE DESIGN ONLY**  
> Status: **DRAFT / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document grants no C03/D04 activation, U06 implementation, delivery, WAITING_USER activation, merge, production, or real-patient authorization.

---

# 1. Purpose

RDP-01 已冻结 U06 如何合法进入。

RDP-02 已冻结：

~~~text
C03 candidate
!= F3 truth

F3AssessmentOwnerDecision
F3QuestionSelectionDecision
D04QuestionStoppingDecision
F3CurrentVersionRevalidationDecision
~~~

以及：

~~~text
F1 clarification no-progress
F3 question-path no-progress
~~~

本 RDP 只回答：

> 这些已经合法形成的 U06 owner/policy 结果，哪些属于 Clinical State，哪些允许形成 K09 StateChangeProposal，如何通过 P01/G2 成为唯一 authoritative effect，以及完整 provenance/replay/trace 如何表达。

必须保持：

~~~text
Decision
!= StateChangeProposal
!= CommitResult
!= Clinical State
!= Runtime State
!= Trace
~~~

---

# 2. Non-negotiable ownership model

## 2.1 Clinical business ownership

~~~text
Information Gap business owner
= F3

Question lifecycle business owner
= F3

Clinical Readiness owner
= G2 Clinical Readiness Resolver / D03

Consultation lifecycle owner
= consultation business lifecycle owner

Runtime Thread / Run / Checkpoint owner
= Runtime
~~~

U06/F3 不得写：
- Clinical Risk；
- Safety Gate；
- Clinical Readiness；
- DDx；
- Offline Evidence；
- Delivery Readiness；
- Runtime Thread/Run/Checkpoint truth。

## 2.2 State write boundary

任何 authoritative Clinical State mutation 必须：

~~~text
business owner decision
→ K09 StateChangeProposal
→ P01/G2 State Governance
→ CommitResult
→ authoritative state reload
~~~

禁止：

~~~text
C03 result
→ direct state write

D04 result
→ direct state write

Trace record
→ clinical state write

Runtime checkpoint
→ clinical state write
~~~

---

# 3. U06 mode mutation matrix

| Mode / stage | Clinical State mutation | Question side effect | Clinical State Version advance |
|---|---:|---:|---:|
| MODE-1 owner NOT_DECIDABLE / FAILED | NONE | NONE | NO |
| MODE-1 canonical F3 effect | YES | NONE | YES on first authoritative commit |
| MODE-2 C03/D04 NO_SELECTION / FAILED / STOP | NONE | NONE | NO |
| MODE-2 F3QuestionSelectionDecision = SELECTED | YES: Question SELECTED | NONE | YES on first authoritative commit |
| MODE-2 delivery intent/send/receipt | NONE by itself | YES, RDP-04 | NO by transport itself |
| MODE-2 delivery-confirmed business transition | YES: Question DELIVERED + Gap ASKED when applicable + pending-question + Consultation WAITING_USER | already reconciled by RDP-04 | YES for authoritative business mutation |
| MODE-3 REVALIDATED_CURRENT | NONE | NONE | NO |
| MODE-3 REASSESSMENT_REQUIRED | NONE | NONE | NO |
| MODE-3 FAILED | NONE | NONE | NO |
| F1/F3 no-progress consequence | NONE | NONE | NO |

Hard invariant:

~~~text
MODE-3
= projection / routing input only
= NO K09 StateChangeProposal
= NO canonical F3 mutation
= NO Clinical State Version advance
~~~

---

# 4. Current P01/P05 physical baseline constraints

Current main provides reusable but incomplete infrastructure.

## 4.1 StateCommitter

Current StateCommitter:
- supports ADD / REPLACE / REMOVE;
- enforces base_version;
- has idempotency inspection/reservation;
- performs capability/consent/field/source checks;
- performs atomic repository commit;
- returns COMMITTED / REJECTED / CONFLICT / FAILED;
- replays completed same-fingerprint idempotency.

Current mechanical core explicitly rejects expected_current_value.

Therefore U06 RDP-03 does not invent a TEST operation or pretend current P01 already supports compare-and-set values.

U06 uses:

~~~text
authoritative reload
+ exact base_version
+ canonical effect identity
+ stable StatePatch idempotency
+ field permission
+ source validation
~~~

for currentness.

Any future expected-value/CAS extension requires a separately reviewed P01 amendment.

## 4.2 StatePatch structural boundary

Current StatePatch roots include:

~~~text
/patient_state/...
~~~

Current controlled-value support permits:
- primitive;
- primitive list;
- one-level structured map;

but rejects nested maps and list-of-map values.

Therefore U06 state values must either:
1. be representable as flat typed/structural records with ref lists; or
2. receive an explicit Shared Contracts / StatePatch typed extension.

This RDP freezes semantic records and logical paths; implementation may not hide nested U06 objects inside opaque JSON strings merely to bypass validation.

## 4.3 CapabilityTrace baseline

Current CapabilityCallTraceRecord represents one capability call and has:
- one binding id;
- one rule release ref;
- one knowledge release ref;
- one decision/proposal/commit slot.

Current bindReleases(rule, knowledge) requires both refs nonblank.

That is incompatible with RDP-05:

~~~text
release family may be REQUIRED or NOT_APPLICABLE
Prompt/Model may become conditionally applicable
synthetic dependency binding must be typed
one U06 execution may have multiple governed decisions/effects
~~~

RDP-03 therefore freezes a U06 parent governed trace companion rather than overloading CapabilityCallTraceRecord.

---

# 5. Authoritative U06 state objects

RDP-03 separates pre-commit state values from post-commit evidence.

## 5.1 F3CanonicalAssessmentStateValue

Logical current record:

~~~text
F3CanonicalAssessmentStateValue

f3_state_record_id
f3_canonical_effect_id
consultation_id
cdp_id

derived_from_clinical_state_version
assessment_context
assessment_trigger_ref

f3_owner_decision_ref
f3_owner_decision_status

dependency_binding_type
dependency_binding_ref
f3_owner_policy_ref

gap_refs[]
online_gap_basis_status

source_c03_result_ref
rule_release_refs[]
knowledge_release_refs[]
prompt_release_refs[]
model_route_refs[]

state_validity
invalidation_refs[]

proposal_ref
created_at
~~~

online_gap_basis_status only allows:

~~~text
GAP_BASIS_ESTABLISHED
NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED
~~~

NOT_DECIDABLE / FAILED never becomes an authoritative F3 state value.

Post-commit fields are forbidden from this pre-commit object:

~~~text
committed_clinical_state_version
commit_result_ref
audit_ref
committed_at
~~~

## 5.2 InformationGapStateValue

Each authoritative Gap is a separate record:

~~~text
InformationGapStateValue

gap_id
consultation_id

target_concept_ref?
target_decision_ref?
status
decision_impact
askable_online

source_basis_refs[]
source_c03_candidate_ref?
source_f3_owner_decision_ref

dependency_binding_ref
f3_owner_policy_ref

question_refs[]
invalidated_by[]

derived_from_clinical_state_version
effect_id
state_validity
created_at
~~~

Existing allowed status remains:

~~~text
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
~~~

RDP-03 does not invent medical values for these statuses.

## 5.3 QuestionStateValue

An authoritative selected/delivered Question record:

~~~text
QuestionStateValue

question_id
question_semantic_key
consultation_id

question_need_class
question_purpose
source_requirement_ref
source_gap_ref?

candidate_ref
rendered_content_ref?
rendered_content_fingerprint?

target_concepts[]
expected_decision_value_ref?

clinical_state_version_basis

dependency_binding_type
dependency_binding_ref

f3_owner_policy_ref
question_policy_ref
d04_policy_ref
d04_decision_ref
f3_selection_decision_ref

status

selection_effect_id
delivery_effect_id?

delivery_ref?
delivered_at?

created_at
~~~

Allowed lifecycle remains:

~~~text
PROPOSED
SELECTED
DELIVERED_TO_USER
ANSWER_RECEIVED
EXPIRED
SUPERSEDED
~~~

For U06 V1 authoritative mutation:

~~~text
C03 candidate
!= authoritative PROPOSED Question
~~~

RDP-03 does not require a separate Clinical State commit for PROPOSED.

A C03/F3 pre-selection candidate may be support/trace material.

The first authoritative Question state produced by U06 V1 is SELECTED after F3QuestionSelectionDecision = SELECTED.

This avoids a meaningless extra Clinical State version solely to persist candidate generation.

## 5.4 PendingQuestionStateValue

pending question means a question actually delivered and awaiting an answer.

It is not equivalent to selected.

Logical current pointer:

~~~text
PendingQuestionStateValue

question_id
delivery_ref
delivered_clinical_state_version_basis
source_requirement_ref
source_gap_ref?
expires_at?
~~~

Invariant:

~~~text
Question SELECTED
→ pending_question = absent

Question DELIVERED_TO_USER
→ pending_question may become current
~~~

---

# 6. Commit evidence objects

Pre-commit state values must not predict post-commit results.

RDP-03 defines logical:

~~~text
U06ClinicalStateCommitEvidence

effect_id
proposal_ref

commit_status
commit_result_ref

previous_clinical_state_version
committed_clinical_state_version?

audit_ref
committed_at?

authoritative_state_record_refs[]
~~~

It belongs to:
- P01 result evidence;
- P05 trace;
- effect/replay reconciliation.

It is not a second Clinical State truth.

---

# 7. MODE-1 formal mutation sequence

Only:

~~~text
F3AssessmentOwnerDecision.decision_status
= GAP_BASIS_ESTABLISHED
or
= NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED
~~~

may enter the MODE-1 mutation path.

Sequence:

~~~text
U06AdmittedInput
→ C03 result
→ F3AssessmentOwnerDecision
→ derive F3_CANONICAL_EFFECT_ID
→ replay-first effect reconciliation
→ if new effect: validate currentness
→ build U06F3CanonicalProposal
→ P01/G2
→ CommitResult
→ reload authoritative Clinical State
→ emit commit evidence / trace
→ post-F3 Safety barrier may later proceed
~~~

For NOT_DECIDABLE / FAILED there is:

~~~text
no F3 canonical StateChangeProposal
no Clinical State mutation
no version advance
~~~

---

# 8. F3 canonical effect identity

Existing frozen identity remains authoritative:

~~~text
F3_CANONICAL_EFFECT_ID

consultation_id
+ fact/framing basis identity
+ source Clinical State Version
+ dependency binding identity
+ F3 assessment policy/version
+ assessment trigger/event identity
~~~

RDP-05 refinement applies:

~~~text
dependency binding identity
=
dependency_binding_type
+ dependency_binding_ref
~~~

not necessarily a production CapabilityBindingRef.

This creates an explicit aggregate compatibility amendment impact:

~~~text
RDP03-COMPAT-F3-ID-01
~~~

Frozen Phase 8 wording currently uses:

~~~text
C03 CapabilityBindingRef
~~~

as one F3_CANONICAL_EFFECT_ID component.

At U06 aggregate closure that component must be normalized to:

~~~text
dependency_binding_type
+ dependency_binding_ref
~~~

with:

~~~text
REAL_CAPABILITY_BINDING
→ dependency_binding_ref resolves to the real governed P06 C03 CapabilityBindingRef

SYNTHETIC_VERIFICATION_BINDING
→ dependency_binding_ref is a stable synthetic verification binding identity
→ allowed only for explicitly authorized structural non-production verification
→ never claims production/live C03 equivalence
~~~

RDP-03 does not silently rewrite Phase 8; it records this exact compatibility amendment for aggregate review/re-freeze.

For PROFILE-A:

~~~text
dependency_binding_type = REAL_CAPABILITY_BINDING
~~~

For PROFILE-B:

~~~text
dependency_binding_type = SYNTHETIC_VERIFICATION_BINDING
~~~

The effect identity must not use attempt-local:
- retry count;
- trace span id;
- wall-clock retry timestamp;
- Runtime checkpoint id.

---

# 9. F3 canonical payload fingerprint

Define:

~~~text
F3_CANONICAL_PAYLOAD_FINGERPRINT
~~~

It covers at least:

~~~text
online_gap_basis_status
gap record canonical semantic content
consultation_id / cdp_id
derived_from_clinical_state_version

source admission ref
source F3 owner decision ref
source C03 result ref

dependency_binding_type/ref
f3_owner_policy_ref

applicable release refs
state_validity initial = CURRENT

f3_canonical_effect_id
~~~

It excludes:
- attempt timestamp;
- retry number;
- trace span;
- checkpoint;
- proposal transport attempt;
- commit response timing.

Rule:

~~~text
same effect id
+ same canonical payload fingerprint
→ exact replay candidate

same effect id
+ different canonical payload fingerprint
→ U06_F3_EFFECT_REPLAY_CONFLICT
→ fail closed
~~~

---

# 10. Stable record and proposal identities

For one canonical F3 effect:

~~~text
same F3_CANONICAL_EFFECT_ID
→ same f3_state_record_id
→ same proposal_id once assigned
→ same StatePatch idempotency key
~~~

Each Gap record identity must also be stable within the same effect.

Allowed implementation:
- deterministic derivation; or
- durable one-time assignment recorded in the effect descriptor.

Forbidden:

~~~text
retry
→ random new f3_state_record_id
→ random new gap_id
→ random new proposal_id
~~~

---

# 11. U06F3CanonicalProposal

Logical K09 proposal:

~~~text
U06F3CanonicalProposal

proposal_id
proposal_contract_version

consultation_id
cdp_id
base_clinical_state_version

logical_unit_id = U06
business_owner = F3

source_admission_ref
source_f3_owner_decision_ref
source_c03_result_ref

source_effect_id = F3_CANONICAL_EFFECT_ID
canonical_payload_fingerprint

dependency_binding_type
dependency_binding_ref

f3_owner_policy_ref
rule_release_refs[]
knowledge_release_refs[]
prompt_release_refs[]
model_route_refs[]

operations[]

evidence_refs[]
source_refs[]

idempotency_key

canonical_event_ref
business_event_identity
correlation_id
trace_id

created_at
~~~

The K09 proposal maps to one P01 StatePatch execution.

---

# 12. MODE-1 logical state paths

RDP-03 freezes logical paths:

~~~text
/patient_state/f3_gap_assessment
/patient_state/information_gaps/{gap_id}
~~~

/patient_state/f3_gap_assessment is a current canonical summary/pointer and is required so the system can distinguish:

~~~text
F3 never assessed
~~~

from:

~~~text
authoritatively assessed
+ explicit NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED
~~~

without treating absence as no-gap truth.

Each Gap is stored as an independently addressable record so current StatePatch does not need a list-of-map payload.

Aggregate schema review must add/confirm these logical fields; implementation must not serialize the entire Gap graph into one opaque string.

---

# 13. MODE-1 operation semantics

For first canonical F3 effect on a basis:

~~~text
ADD /patient_state/f3_gap_assessment
ADD /patient_state/information_gaps/{gap_id} ...
~~~

For a new authoritative F3 effect replacing prior current F3 basis:

~~~text
REPLACE /patient_state/f3_gap_assessment

ADD / REPLACE individual current Gap records
as required by exact owner decision

old historical versions remain immutable
~~~

Gap removal from current applicability should preferably be represented as a governed state transition to INVALIDATED / RESOLVED / WAIVED rather than physical deletion when business provenance must remain visible.

A physical REMOVE is not the default F3 lifecycle mechanism.

---

# 14. MODE-1 P01 source semantics

Current StatePatch source vocabulary does not include F3_OWNER.

For U06 owner-decided derived state:

~~~text
StatePatch operation.source
= RULE_DERIVED
~~~

is the compatible V1 mechanical source class because the authoritative mutation is created by deterministic F3 owner/policy interpretation.

This does not erase candidate provenance.

C03/model/tool/knowledge provenance remains in:
- source refs;
- dependency binding refs;
- release refs;
- U06 trace.

Forbidden:

~~~text
operation.source = MODEL_INFERRED
merely because C03 happened to use a model
~~~

when the committed truth is an F3 owner decision rather than raw model output.

Any future richer source taxonomy requires a Shared Contracts amendment.

---

# 15. Logical Unit identity vs physical StatePatch authorization identity

Current StatePatchBoundaryValidator requires physical producer fields to use the lower-case service-name contract.

Therefore RDP-03 explicitly separates:

~~~text
logical K09 unit identity
= U06

physical StatePatch.producer
= approved lower-case runtime service identity

physical StatePatch.envelope.producer
= approved lower-case runtime service identity

StatePatch.envelope.capability_id/version
= P01 state-write authorization identity for the U06 mutation path

dependency_binding_type/ref
= C03/U06 dependency identity from RDP-05
~~~

These are four different concerns.

Literal U06 is not a legal current physical StatePatch producer value and must not be written into the physical producer field.

Forbidden:

~~~text
logical_unit_id = U06
→ copy literal U06 into StatePatch.producer

copy C03 CapabilityBindingRef into StatePatch capability_id
and treat it as state-write authority

use SYNTHETIC_C03 identity as production state-write capability

use physical service producer identity
as proof of F3 business ownership
~~~

Logical proposals preserve:

~~~text
logical_unit_id = U06
business_owner = F3
~~~

The physical mapper must independently supply the approved service producer and P01 state-write authorization identity.

The exact approved physical identity values are implementation-readiness inputs; they must satisfy current StatePatch contracts without changing business ownership.

---

# 16. MODE-1 P01 validation

Before first authoritative commit, U06/P01 must establish:

~~~text
admission still lawful/current
F3 owner decision is a mutation-eligible status
owner decision provenance matches admission/C03/binding/policy

authoritative current Clinical State Version
= proposal.base_clinical_state_version
= F3 owner input version

effect id matches frozen derivation
canonical payload fingerprint matches

operation paths are U06/F3-authorized
operation.source = allowed source class
state values pass U06 semantic schema validation

dependency binding/profile remains compatible

required release refs present
NOT_APPLICABLE families do not require fake refs

StatePatch idempotency key stable
~~~

P01 may remain mechanically generic; U06 semantic validation must occur before or through an authorized U06 adapter/validator.

---

# 17. Replay-first reconciliation

Ordering:

~~~text
R0 derive effect id + canonical payload fingerprint

R1 inspect durable canonical effect descriptor / P01 idempotency evidence

R2 if exact authoritative prior commit exists:
   reattach prior result
   no new mutation
   no requirement that current version still equals old base version

R3 if effect absent/uncommitted:
   validate current state/version/dependencies

R4 build stable proposal / StatePatch

R5 P01 final version/idempotency checks
~~~

This prevents the common error:

~~~text
original commit advanced Vn → Vn+1
then replay sees Vn != current
and incorrectly declares the already-committed exact effect stale
~~~

---

# 18. Effect ledger relationship

Current CanonicalEffectLedger is immutable and stores:
- namespace;
- effect identity;
- fingerprint;
- immutable bytes.

It does not itself encode P01 COMMITTED truth.

Therefore:

~~~text
CanonicalEffectLedger record
= stable effect descriptor / replay evidence
!= proof Clinical State mutation committed
~~~

Authoritative commit proof comes from:
- P01 idempotency original CommitResult;
- authoritative Clinical State read-back;
- commit evidence / audit.

RDP-03 forbids:

~~~text
ledger CREATED
→ assume Clinical State COMMITTED
~~~

Suggested namespaces:

~~~text
u06.f3.canonical
u06.question.selection
~~~

Exact storage namespace string is implementation-level but must be stable/versioned.

---

# 19. MODE-1 CommitResult semantics

Current P01 statuses map as:

## COMMITTED

~~~text
first authoritative effect applied
Clinical State Version advances
reload must contain matching F3 canonical record/effect
~~~

## exact replay of COMMITTED

Current P01 may return the stored original COMMITTED result for same idempotency/fingerprint.

Semantically:

~~~text
REATTACHED_AUTHORITATIVE_EFFECT
~~~

not a second commit.

## CONFLICT

~~~text
no U06 state effect from stale proposal
reload state
do not merely bump base_version
re-admit / re-run owner path when still applicable
~~~

## REJECTED

~~~text
no authoritative U06 effect
non-retryable until authorization/schema/source problem corrected
~~~

## FAILED

~~~text
no assumption of authoritative mutation
reconcile P01 idempotency/state before any retry
~~~

RDP-03 does not add a new physical NO_OP status to current CommitResult.

---

# 20. MODE-2 selection mutation eligibility

Only:

~~~text
F3QuestionSelectionDecision.outcome = SELECTED
~~~

may create an authoritative Question selection effect.

Required equality:

~~~text
selection decision consultation/state/source
= admitted U06 execution

selection decision d04_decision_ref
= exact D04 CONTINUE decision

selected candidate
= one member of exact eligible candidate set

question semantic key
= policy-derived stable key

dependency/policy refs
= admitted/owner-resolved refs
~~~

For NO_SELECTION / FAILED / D04 STOP / D04 FAILED there is no Question selection proposal and no Clinical State Version advance.

---

# 21. Question selection effect identity

Define:

~~~text
QUESTION_SELECTION_EFFECT_ID
~~~

minimum semantics:

~~~text
consultation_id
+ U06 admission_id
+ source authority type
+ source requirement ref
+ source Gap ref when applicable
+ input Clinical State Version
+ F3QuestionSelectionDecision id
+ D04 decision id
+ question_semantic_key
+ selected_candidate_ref
+ dependency binding identity
+ F3 owner policy version
+ Question Policy version
+ D04 policy version
+ question-selection contract version
~~~

Different rendered wording alone does not necessarily define a different semantic Question identity if policy says rendering is equivalent.

But if delivered content semantics differ under the governed rendering contract, the selected content fingerprint/ref must participate in the canonical selection payload fingerprint.

---

# 22. Question selection payload fingerprint

Define:

~~~text
QUESTION_SELECTION_CANONICAL_PAYLOAD_FINGERPRINT
~~~

covering at least:

~~~text
question_semantic_key
question_need_class
question_purpose
source requirement/gap refs
selected candidate ref
target concept refs
expected decision value ref

clinical state version basis

dependency binding identity
F3/Question/D04 policy refs
D04 decision ref
F3 selection decision ref

rendered content ref/fingerprint when selection freezes content

initial status = SELECTED
selection_effect_id
~~~

Attempt-local trace/runtime/delivery ids are excluded.

---

# 23. Stable Question identity

For exact same selection effect:

~~~text
same QUESTION_SELECTION_EFFECT_ID
→ same question_id
→ same proposal_id
→ same StatePatch idempotency key
~~~

Retry must not create a second question id.

If the same semantic need is reevaluated under a genuinely new governed basis, it may form a new selection effect only if RDP-02 duplicate/re-ask policy permits it.

---

# 24. U06QuestionSelectionProposal

Logical K09 proposal:

~~~text
U06QuestionSelectionProposal

proposal_id
proposal_contract_version

consultation_id
cdp_id
base_clinical_state_version

logical_unit_id = U06
business_owner = F3

source_admission_ref
source_d04_decision_ref
source_f3_selection_decision_ref

source_effect_id = QUESTION_SELECTION_EFFECT_ID
canonical_payload_fingerprint

dependency_binding_type/ref
f3_owner_policy_ref
question_policy_ref
d04_policy_ref

operations[]
evidence_refs[]
source_refs[]

idempotency_key

canonical_event_ref
business_event_identity
correlation_id
trace_id

created_at
~~~

---

# 25. MODE-2 selection state path

Logical path:

~~~text
/patient_state/questions/{question_id}
~~~

Selection operation:

~~~text
ADD QuestionStateValue(status = SELECTED)
~~~

or REPLACE only when the exact current business record is lawfully transitioned by the same owner path.

At selection time U06 must not mutate:

~~~text
Gap status → ASKED
pending_question
Consultation WAITING_USER
Thread AWAITING_USER
~~~

because:

~~~text
ASKED
= already presented to user

SELECTED
!= DELIVERED_TO_USER
~~~

---

# 26. Exact Gap -> ASKED timing

RDP-03 freezes:

~~~text
Gap status = ASKED
only after authoritative Question delivery confirmation
~~~

For F3_INFORMATION_GAP, after RDP-04 proves delivery:

~~~text
Question SELECTED
→ DELIVERED_TO_USER

source Gap QUESTIONABLE_ONLINE / eligible current status
→ ASKED
~~~

These belong to the same authoritative delivered-question business transition.

For F1_MINIMAL_CLARIFICATION:

~~~text
no synthetic F3 Gap is created merely to support the Question
therefore no Gap -> ASKED mutation is required
~~~

This resolves the deferred Unit Spec question.

---

# 27. Selected Question vs Pending Question authority

RDP-03 freezes the logical pending-question path:

~~~text
/patient_state/pending_question
~~~

Before successful delivery:

~~~text
Question = SELECTED
/patient_state/pending_question = absent for this Question
Consultation != WAITING_USER because of this Question
Thread != AWAITING_USER because of this Question
~~~

Question selection does not mutate the source Gap question_refs list.

At selection time the authoritative linkage is:

~~~text
Question.source_gap_ref
+ selection decision/effect provenance
~~~

After successful authoritative delivery transition:

~~~text
Question = DELIVERED_TO_USER
/patient_state/pending_question = current delivered Question
source Gap question_refs may add this delivered Question ref when F3 need
Consultation = WAITING_USER
Runtime may then enter AWAITING_USER
~~~

A SELECTED question is not a pending user answer.

---

# 28. Delivery-confirmed business mutation decomposition

RDP-04 owns transport/reconciliation, but RDP-03 freezes the exact authoritative mutation objects that may follow delivery confirmation.

All delivery-confirmed mutations share one parent:

~~~text
QUESTION_DELIVERED_WAIT_EFFECT_ID
~~~

and are decomposed into separate authoritative storage-owner sub-effects.

## 28.1 P01 Clinical State sub-effect

Define:

~~~text
QUESTION_DELIVERED_CLINICAL_STATE_EFFECT_ID
=
QUESTION_DELIVERED_WAIT_EFFECT_ID
+ clinical-state-sub-effect contract version

QUESTION_DELIVERED_CLINICAL_STATE_PAYLOAD_FINGERPRINT
=
canonical semantic fingerprint of
Question DELIVERED transition
+ Gap ASKED transition when applicable
+ pending-question pointer
+ delivery confirmation/content identity
+ parent effect identity

U06_DELIVERED_CLINICAL_STATE_IDEMPOTENCY_KEY
=
QUESTION_DELIVERED_CLINICAL_STATE_EFFECT_ID
+ proposal_contract_version
~~~

Stable identity rules:

~~~text
same clinical-state child effect
→ same proposal_id
→ same patch_id
→ same idempotency key
→ same canonical child payload fingerprint

same child effect id
+ different child payload fingerprint
→ U06_DELIVERED_CLINICAL_STATE_REPLAY_CONFLICT
→ fail closed
~~~

and logical K09 proposal:

~~~text
U06QuestionDeliveredClinicalStateProposal

proposal_id
patch_id
proposal_contract_version
consultation_id
cdp_id
base_clinical_state_version

logical_unit_id = U06
business_owner = F3

parent_delivered_wait_effect_id
clinical_state_sub_effect_id

question_id
question_selection_effect_id
delivery_id
delivery_confirmation_ref

source_gap_ref?
question_need_class

operations[]
canonical_payload_fingerprint
idempotency_key

correlation_id
trace_id
created_at
~~~

Logical Clinical State paths are exactly:

~~~text
/patient_state/questions/{question_id}
/patient_state/information_gaps/{gap_id}     when F3 need
/patient_state/pending_question
~~~

For F3_INFORMATION_GAP, the P01 sub-effect is one atomic StatePatch:

~~~text
REPLACE Question SELECTED -> DELIVERED_TO_USER

REPLACE source Gap
→ status = ASKED
→ add delivered Question ref to question_refs as governed

ADD /patient_state/pending_question
when no current pending pointer exists
~~~

For F1_MINIMAL_CLARIFICATION:

~~~text
REPLACE Question SELECTED -> DELIVERED_TO_USER

ADD /patient_state/pending_question
when no current pending pointer exists

no F3 Gap mutation
~~~

Pending-question replacement safety is frozen:

~~~text
pending_question absent
→ ADD is legal

pending_question references the exact same question
+ same QUESTION_DELIVERED_WAIT_EFFECT_ID
→ exact replay / reattach
→ no new semantic REPLACE

pending_question references a different current Question
→ U06_PENDING_QUESTION_CONFLICT
→ fail closed
→ no delivered child commit

pending pointer is stale/superseded
→ replacement allowed only after authoritative lifecycle/owner evidence proves it is no longer current
→ same Clinical State base_version must still be current
~~~

Current P01 has no expected_current_value CAS.

Therefore this safety rule is enforced by:

~~~text
authoritative pre-read
+ exact selected/pending identity validation
+ base_version
+ P01 idempotency
+ RDP-04 reconciliation
~~~

not by inventing unsupported compare-and-set semantics.

The P01 sub-effect does not mutate Consultation lifecycle.

## 28.2 Consultation lifecycle sub-effect

Define:

~~~text
CONSULTATION_WAITING_EFFECT_ID
=
QUESTION_DELIVERED_WAIT_EFFECT_ID
+ consultation-wait-sub-effect contract version

CONSULTATION_WAITING_PAYLOAD_FINGERPRINT
=
canonical semantic fingerprint of
consultation_id
+ expected prior lifecycle
+ target WAITING_USER
+ question_id
+ delivery_id
+ delivery confirmation identity
+ parent delivered-wait effect

CONSULTATION_WAITING_IDEMPOTENCY_KEY
=
CONSULTATION_WAITING_EFFECT_ID
+ transition_contract_version
~~~

Stable identity rules:

~~~text
same Consultation child effect
→ same transition_id
→ same idempotency key
→ same canonical child payload fingerprint

same Consultation child effect id
+ different canonical child payload
→ U06_CONSULTATION_WAITING_REPLAY_CONFLICT
→ fail closed
~~~

and logical command:

~~~text
ConsultationWaitingTransitionCommand

transition_id
transition_contract_version
parent_delivered_wait_effect_id
consultation_waiting_effect_id
canonical_payload_fingerprint

consultation_id
expected_consultation_row_version
expected_prior_lifecycle = ACTIVE
target_lifecycle = WAITING_USER

question_id
delivery_id
delivery_confirmation_ref

idempotency_key
correlation_id
trace_id
created_at
~~~

This command is owned by the Consultation lifecycle persistence boundary, not P01 StatePatch.

First transition eligibility:

~~~text
current lifecycle = ACTIVE
+ expected_consultation_row_version matches
+ no different current pending/wait Question
→ WAITING_USER transition eligible
~~~

Exact replay:

~~~text
current lifecycle = WAITING_USER
+ authoritative wait provenance references the exact same
  QUESTION_DELIVERED_WAIT_EFFECT_ID / question_id / delivery_id
→ reattach same authoritative WAITING transition
→ no second lifecycle mutation
~~~

Conflict:

~~~text
current lifecycle = WAITING_USER
for a different question/effect
→ U06_CONSULTATION_WAITING_CONFLICT
→ fail closed

row_version mismatch
or incompatible lifecycle
→ do not overwrite blindly
→ reconcile parent delivered-wait effect
→ RDP-04 recovery/failure policy
~~~

A plain lifecycle value WAITING_USER without matching effect/question provenance is insufficient to declare exact replay success.

## 28.3 Runtime wait transition

Runtime may form:

~~~text
Thread -> AWAITING_USER
~~~

only after the parent delivered-wait effect has authoritative evidence that:
- delivered Clinical State sub-effect is committed/current;
- Consultation WAITING sub-effect is committed/current.

Runtime transition:
- is not K09;
- is not Clinical State;
- cannot repair missing business-state sub-effects by inference.

RDP-04 will freeze exact durable ordering/crash reconciliation.

---

# 29. Cross-store boundary for Consultation WAITING_USER

Current main stores authoritative Consultation lifecycle in clinical_consultation, separate from generic StateCommitter/CDP mutation.

Therefore current P01 mechanical atomic commit cannot be assumed to atomically write:

~~~text
CDP/Clinical State Question + Gap
AND
clinical_consultation lifecycle
AND
Runtime Thread
~~~

RDP-03 freezes semantic truth but not a fake cross-store transaction.

Required RDP-04/implementation resolution:

~~~text
one canonical delivered-wait effect identity
+ durable reconciliation protocol
+ no externally visible WAITING_USER without delivered Question
+ no Thread AWAITING_USER before business WAITING truth is authoritative
~~~

Allowed physical strategies include:
- transactional outbox/effect ledger choreography;
- shared transaction only if the actual persistence boundary proves it;
- durable saga/reconciliation.

Forbidden:

~~~text
update Question
then best-effort update Consultation
then best-effort Thread
and call the sequence atomic
~~~

---

# 30. Delivery effect identity handoff

RDP-03 freezes the parent semantic identity:

~~~text
QUESTION_DELIVERED_WAIT_EFFECT_ID
~~~

Minimum derivation:

~~~text
consultation_id
+ question_id
+ QUESTION_SELECTION_EFFECT_ID
+ delivery_id
+ delivery payload/content fingerprint
+ delivery confirmation evidence identity
+ authoritative selected-Question Clinical State Version
+ delivery business-transition contract version
~~~

Stable child identities:

~~~text
QUESTION_DELIVERED_CLINICAL_STATE_EFFECT_ID
= parent effect + clinical-state-sub-effect contract version

U06_DELIVERED_CLINICAL_STATE_IDEMPOTENCY_KEY
= QUESTION_DELIVERED_CLINICAL_STATE_EFFECT_ID
  + proposal_contract_version

CONSULTATION_WAITING_EFFECT_ID
= parent effect + consultation-wait-sub-effect contract version

CONSULTATION_WAITING_IDEMPOTENCY_KEY
= CONSULTATION_WAITING_EFFECT_ID
  + transition_contract_version
~~~

RDP-04 will freeze exact transport attempt/receipt/crash-window fields.

RDP-03 requires:

~~~text
same parent delivery effect
→ same child business mutation identities
→ same child idempotency identities

different delivery_id or different confirmed content fingerprint
→ different parent effect or conflict
~~~

Transport attempt identity is not the parent business effect identity.

---

# 31. MODE-3 projection contract

F3CurrentVersionRevalidationDecision is not a Clinical State mutation.

For REVALIDATED_CURRENT, define logical:

~~~text
F3CurrentReadinessProjection

projection_id
consultation_id

f3_canonical_effect_id
f3_source_state_ref

source_clinical_state_version
target_clinical_state_version

f3_revalidation_decision_ref
u04_gate_ref
routing_authorization_ref

dependency_compatibility_ref
policy_compatibility_ref

source_domain = F3
source_owner = F3
input_kind = ONLINE_INFORMATION_GAP
applicability_status = PRESENT
validity = CURRENT

trace_refs[]
~~~

This projection:
- is governed routing input or durable non-clinical evidence;
- is not K09 StateChangeProposal;
- is not a second canonical F3 state;
- does not advance Clinical State Version.

Stable identity remains the frozen F3_REVALIDATION_ID.

---

# 32. MODE-3 result semantics

## REVALIDATED_CURRENT

~~~text
no StatePatch
no P01 commit
no version advance
materialize current readiness input projection
~~~

## REASSESSMENT_REQUIRED

~~~text
no StatePatch
no P01 commit
no mutation of historical F3
route only toward fresh MODE-1 admission
~~~

## FAILED

~~~text
no StatePatch
no P01 commit
governed failure/retry routing
~~~

Trace must never fabricate proposal_ref or commit_ref for MODE-3.

---

# 33. No-progress consequences are non-mutating

From RDP-02:

~~~text
F1_CLARIFICATION_NO_PROGRESS
→ target U01/F1

F3_QUESTION_PATH_NO_PROGRESS
→ READINESS_REEVALUATION_REQUIRED
→ target U05/D03
~~~

RDP-03 freezes:

~~~text
both consequences
= decision/routing evidence
!= Clinical State mutation
~~~

No:
- Question state;
- Gap ASKED;
- Clinical Readiness;
- WAITING_USER;
- version advance.

---

# 34. U06 governed parent trace

RDP-03 selects the P05 compatibility strategy:

> **Add a U06 governed parent trace companion instead of overloading the existing one-call CapabilityCallTraceRecord.**

Define stable parent trace identity:

~~~text
U06_TRACE_ID
=
consultation_id
+ admission_id
+ u06_mode
+ source_authority_type
+ source_authority_ref
+ input_clinical_state_version
+ execution_profile
+ U06 trace contract version
~~~

Rules:

~~~text
same admitted logical U06 execution / exact replay
→ same U06_TRACE_ID

retry attempt / span / transport attempt
→ child attempt evidence
→ not a new parent trace identity

new admission
or mode
or source authority
or input state version
or execution profile
→ different U06_TRACE_ID

U06_TRACE_ID
!= effect identity
!= proposal identity
!= permission to mutate state
~~~

Logical record:

~~~text
U06GovernedExecutionTrace

u06_trace_id
unit_id = U06
mode
execution_profile

consultation_id
cdp_id
thread_id?
run_id?
event_id?

admission_id
source_authority_type
source_authority_ref
input_clinical_state_version

dependency_binding_type
dependency_binding_ref
dependency_profile_fingerprint

release_applicability_entries[]
prompt_model_route_entries[]
tool_skill_entries[]

c03_call_trace_ref?
c03_result_ref?

f3_assessment_decision_ref?
d04_decision_ref?
f3_question_selection_decision_ref?
f3_revalidation_decision_ref?

effect_refs[]
proposal_refs[]
commit_refs[]

state_version_before
state_version_after?

no_progress_consequence_ref?

delivery_intent_ref?
delivery_id?
delivery_receipt_ref?
delivery_confirmation_ref?
delivered_wait_effect_ref?

failure_code?
trace_status

correlation_id
trace_id
started_at
finished_at?
~~~

The record must be PHI-minimal and prefer refs/fingerprints over raw question/user content.

Parent trace lifecycle is evidence-oriented:

~~~text
STARTED
OWNER_DECISION_RECORDED
MUTATION_PENDING
MUTATION_RECONCILED
DELIVERY_PENDING
NO_MUTATION_TERMINAL
FAILED
~~~

Implementation may use append-only stage events plus a materialized current view, or another durable equivalent.

It must preserve:

~~~text
historical terminal stage evidence is not destructively overwritten

exact replay attaches prior effect/proposal/commit refs

attempt-local retry evidence remains distinguishable from parent business trace

a trace update cannot change Clinical State truth
~~~

---

# 35. CapabilityCallTraceRecord relationship

Existing CapabilityCallTraceRecord remains a leaf trace only for an actual governed capability invocation.

For PROFILE-A real C03:

~~~text
U06GovernedExecutionTrace.c03_call_trace_ref
→ CapabilityCallTraceRecord
~~~

The parent trace supplies:
- typed dependency binding identity;
- optional/multi-ref applicability;
- policy refs;
- multiple owner decisions/effects.

For PROFILE-B:

~~~text
synthetic invocation
must be explicitly typed synthetic in U06 parent trace
~~~

It must not masquerade as an ACTIVE production P06 binding merely to satisfy existing binding_id.

Implementation may add a synthetic-specific leaf trace or bounded test trace, but production CapabilityCallTrace semantics cannot be falsified.

---

# 36. Typed release/applicability trace

To resolve the current P05 both-refs problem, define trace entries:

~~~text
U06DependencyTraceEntry

dependency_family
applicability
ref?
version?
status?
~~~

Families may include:

~~~text
RULE_RELEASE
KNOWLEDGE_RELEASE
PROMPT_RELEASE
MODEL_ROUTE
TOOL
SKILL
QUESTION_POLICY
F3_OWNER_POLICY
D04_POLICY
~~~

Applicability:

~~~text
REQUIRED
OPTIONAL
NOT_APPLICABLE
~~~

Rules:

~~~text
REQUIRED + missing ref
→ invalid / fail closed

NOT_APPLICABLE
→ ref absent
→ no fake placeholder
~~~

Current CapabilityTraceService.bindReleases(rule, knowledge) is insufficient for U06 parent provenance and is not used to force fake refs.

---

# 37. P05 authoritative boundary

Trace may prove:

~~~text
admission
→ dependency resolution
→ capability invocation
→ F3/D04 owner decision
→ effect identity
→ proposal
→ commit result
→ delivery refs
→ version transition
~~~

Trace may not:
- become Clinical State;
- substitute for P01 commit evidence;
- make a failed effect authoritative;
- make MODE-3 projection into mutation.

---

# 38. Mode-specific trace chains

## MODE-1 successful commit

~~~text
admission
→ dependency binding
→ C03 call/result
→ F3AssessmentOwnerDecision
→ F3_CANONICAL_EFFECT_ID
→ U06F3CanonicalProposal
→ P01 CommitResult
→ authoritative read-back
→ post-F3 barrier ref
~~~

## MODE-1 not decidable/failure

~~~text
admission
→ C03 result/failure
→ F3 owner NOT_DECIDABLE/FAILED
→ no effect/proposal/commit refs
~~~

## MODE-2 selection

~~~text
admission
→ fresh C03
→ F3 candidate normalization
→ D04 CONTINUE
→ F3QuestionSelectionDecision SELECTED
→ QUESTION_SELECTION_EFFECT_ID
→ U06QuestionSelectionProposal
→ P01 CommitResult
→ authoritative Question SELECTED
~~~

## MODE-2 no selection

~~~text
admission
→ C03/D04/owner
→ NO_SELECTION / STOP / FAILED
→ no proposal/commit
→ no-progress/failure consequence if applicable
~~~

## MODE-3

~~~text
admission
→ F3CurrentVersionRevalidationDecision
→ projection or reassessment/failure consequence
→ no proposal/commit
~~~

---

# 39. Proposal identity and StatePatch idempotency

For each state-mutating U06 effect:

~~~text
U06_STATE_PATCH_IDEMPOTENCY_KEY
=
effect_id
+ proposal_contract_version
~~~

Same effect replay must produce the same:
- proposal id;
- patch id or stable patch business identity;
- idempotency key;
- canonical patch fingerprint.

Attempt-specific transport/request ids may differ but may not redefine business identity.

---

# 40. Canonical patch vs business payload fingerprint

Two fingerprints must not be confused:

~~~text
business canonical payload fingerprint
= U06 semantic equality

StateCommitter CanonicalPatchFingerprint
= exact mechanical StatePatch equality
~~~

Rule:

~~~text
same U06 effect
→ deterministic/stable StatePatch content
→ same P01 patch fingerprint
~~~

If same effect id yields a different mechanical patch:

~~~text
IDEMPOTENCY_MISMATCH / U06 replay conflict
→ fail closed
~~~

Retry-time timestamps must not cause patch inequality for the same business effect.

Therefore first-assigned created_at used in StatePatch/proposal must be stable across replay.

---

# 41. Currentness and conflict handling

For a new/uncommitted effect:

~~~text
owner decision input version
= proposal base version
= authoritative current version at first commit attempt
~~~

If version drift occurs:

~~~text
P01 CONFLICT
→ no blind base_version replacement
→ reload authoritative state
→ reconcile exact prior effect
→ if effect not committed:
   old admission/decision becomes stale as applicable
   re-admit/re-run owner/policy on current basis
~~~

Forbidden:

~~~text
old F3QuestionSelectionDecision
+ just update baseVersion
→ commit
~~~

or:

~~~text
old F3 assessment
+ new state version
→ commit unchanged
~~~

---

# 42. Same semantic value != same effect

Examples:

~~~text
Gap A QUESTIONABLE_ONLINE at V10
and same Gap category at V12
with different source basis
!= exact replay

Question with same displayed wording
but different source requirement/policy basis
!= necessarily same selection effect
~~~

Conversely:

~~~text
same exact effect id + same canonical fingerprint
after commit
→ reattach
even though current state version is later
~~~

---

# 43. Concurrent U06 writers

Same Consultation must not have two uncontrolled authoritative state writers.

If two distinct U06 proposals use the same base version:

~~~text
at most one may COMMIT
other gets CONFLICT
→ reload/reconcile/re-evaluate
~~~

If both are exact same effect:

~~~text
P01 idempotency / effect reconciliation
→ original authoritative result
~~~

No last-write-wins.

---

# 44. Question selection and delivery concurrency

If a Question SELECTED record becomes:
- superseded;
- expired;
- answered by another accepted path;
- invalidated by upstream state change;

before delivery confirmation:

~~~text
RDP-04 must not deliver it as current
~~~

Delivery-confirmed mutation must revalidate the authoritative selected Question identity/currentness.

RDP-03 does not allow RDP-04 to revive a stale selection by only changing its base version.

---

# 45. Commit read-back requirement

After COMMITTED, U06 must reload authoritative state and prove:

For MODE-1:

~~~text
current f3_gap_assessment.effect_id
= expected F3_CANONICAL_EFFECT_ID
~~~

and matching Gap refs are present/current as required.

For MODE-2 selection:

~~~text
Question record question_id/effect_id
= expected selection effect
status = SELECTED
~~~

If CommitResult says COMMITTED but authoritative read-back cannot confirm the intended state:

~~~text
U06_AUTHORITATIVE_READBACK_MISMATCH
→ failure / reconciliation
→ no downstream consequence
~~~

CommitResult alone is not enough to invent downstream routing.

---

# 46. Clinical state invalidation relationship

RDP-03 does not redesign global D05 invalidation.

It freezes U06-specific effect behavior:

~~~text
upstream fact/framing/DDx/evidence change
may invalidate F3/Question business state
through governed owner/invalidation rules

Trace alone does not mark state stale
Runtime alone does not mark state stale
~~~

Where a future U13/D05 path invalidates F3/Question records:
- invalidation must itself be an authoritative governed effect;
- prior historical state remains auditable;
- current routing must not consume invalidated Question/Gap.

Exact global invalidation proposal remains owned by D05/U13 or relevant owner amendment.

---

# 47. State validity vs effective currentness

Stored lifecycle marker and effective currentness are separate.

A stored record may say:

~~~text
state_validity = CURRENT
~~~

at its last authoritative write.

But current consumption still requires:
- dependency currentness;
- source basis not invalidated;
- current Safety/routing applicability;
- exact owner/policy rules.

No consumer may infer current routability solely from an old stored CURRENT marker.

---

# 48. RDP-01 compatibility

RDP-03 consumes only a valid RDP-01 admission.

It preserves:
- mode;
- source authority;
- current Gate/permission;
- admission identity;
- event/replay identity.

Proposal source_admission_ref must equal the exact admitted execution that produced the owner decision.

RDP-03 may not reconstruct admission from Clinical State after the fact.

---

# 49. RDP-02 compatibility

RDP-03 consumes typed RDP-02 outputs without reinterpretation:

~~~text
F3AssessmentOwnerDecision
F3QuestionSelectionDecision
D04QuestionStoppingDecision
F3CurrentVersionRevalidationDecision
F1_CLARIFICATION_NO_PROGRESS
F3_QUESTION_PATH_NO_PROGRESS
~~~

It must not:
- choose a different Question;
- reinterpret D04 STOP;
- convert NO_SELECTION to SELECTED;
- convert NOT_DECIDABLE to no-gap truth;
- create Clinical Readiness.

---

# 50. RDP-05 compatibility

RDP-03 preserves:

~~~text
dependency_binding_type
dependency_binding_ref
dependency_profile_fingerprint
expected_capability_role
release applicability
execution profile
~~~

No fake release refs.

P03/P04 provenance appears only when applicable.

PROFILE-B synthetic identity must never be written as if it were a real active C03 binding.

---

# 51. RDP-04 handoff obligations

RDP-04 receives:
- authoritative current Question SELECTED ref;
- QUESTION_SELECTION_EFFECT_ID;
- exact rendered content ref/fingerprint;
- dependency/policy provenance;
- delivery eligibility/Safety context;
- canonical event/correlation ids.

RDP-04 must return delivery evidence sufficient to authorize only the frozen delivered/wait business mutation.

RDP-03 does not authorize transport.

---

# 52. RDP-06 verification obligations

RDP-06 must verify at least:

~~~text
MODE-1
eligible owner result -> one canonical proposal
NOT_DECIDABLE/FAILED -> zero proposals
effect replay -> no second version advance
same effect + changed payload -> conflict
version drift before first commit -> conflict/re-admission
read-back mismatch -> no downstream

F3 state
no-assessment != explicit no-online-gap assessment
Gap records independently addressable
no nested-map bypass

MODE-2 selection
SELECTED decision -> one Question SELECTED proposal
NO_SELECTION/STOP/FAILED -> zero proposals
Question id stable on replay
Gap not ASKED at selection
pending_question absent at selection
no WAITING_USER at selection

delivery-state contract
Gap ASKED only after delivery confirmation
F1 clarification delivery has no fake F3 Gap
pending question only after delivery
Thread state never treated as Clinical State
cross-store atomicity not falsely assumed

MODE-3
zero StatePatch
zero P01 commit
zero version advance
no fake proposal/commit trace refs

P05
NOT_APPLICABLE release family -> no fake ref
parent trace links one or more decisions/effects
existing capability trace remains leaf
synthetic binding typed separately

P01
current ADD/REPLACE/REMOVE only
expected_current_value not assumed supported
stable StatePatch created_at/idempotency on replay
StatePatch capability authorization != C03 dependency binding
~~~

---

# 53. Current implementation impact inventory

All impacts remain NOT_AUTHORIZED.

## U06-RDP03-IMP-01 — U06 semantic state adapter/schema

Need an implementation surface for:
- F3CanonicalAssessmentStateValue;
- InformationGapStateValue;
- QuestionStateValue;
- PendingQuestionStateValue.

It must remain within controlled StatePatch shapes or introduce an explicitly reviewed Shared Contracts extension.

## U06-RDP03-IMP-02 — P01 field permission

Authorize U06 owner path only for exact F3/Question logical fields.

No broad /patient_state/** write authority.

## U06-RDP03-IMP-03 — physical producer + P01 state-write capability identity

Register:
- an approved lower-case physical service producer identity accepted by StatePatch contracts;
- a U06 state-write authorization capability identity/version distinct from C03 dependency binding.

Logical unit identity remains U06 and business owner remains F3.

## U06-RDP03-IMP-04 — stable proposal/effect adapter

Need stable:
- effect id;
- record ids;
- proposal id;
- patch id/idempotency;
- canonical payload/patch fingerprints.

## U06-RDP03-IMP-05 — U06 governed P05 parent trace

Add companion trace storage/service supporting:
- typed dependency applicability;
- multiple policy refs;
- multiple decisions/effects;
- optional delivery refs;
- no fake release refs.

## U06-RDP03-IMP-06 — real/synthetic C03 leaf trace compatibility

Do not force synthetic PROFILE-B through an untyped real capability binding field.

## U06-RDP03-IMP-07 — delivered/wait cross-store seam

RDP-04 must implement/reconcile the frozen parent/child mutation model:
- QUESTION_DELIVERED_WAIT_EFFECT_ID;
- QUESTION_DELIVERED_CLINICAL_STATE_EFFECT_ID through P01;
- CONSULTATION_WAITING_EFFECT_ID through Consultation lifecycle persistence;
- Runtime Thread/checkpoint transition after business-state reconciliation.

Current ConsultationRecord exposes ACTIVE but no U06 WAITING transition implementation, so this is a real implementation impact.

No cross-store atomicity may be claimed before this is designed/verified.

## U06-RDP03-IMP-08 — Phase-8 F3 binding identity aggregate amendment

At aggregate closure reconcile RDP03-COMPAT-F3-ID-01:
- old Phase-8 C03 CapabilityBindingRef wording;
- normalized dependency_binding_type + dependency_binding_ref;
- PROFILE-A real P06 ref;
- PROFILE-B synthetic verification identity without production equivalence.

---

# 54. Design acceptance scenarios

~~~text
RDP03-AC-01
MODE-1 GAP_BASIS_ESTABLISHED at Vn
→ stable F3 effect
→ K09 proposal
→ P01 COMMITTED Vn+1
→ read-back matches effect

RDP03-AC-02
MODE-1 NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED
with explicit owner evidence
→ f3_gap_assessment committed
→ gap_refs empty
→ absence is not used as no-gap truth

RDP03-AC-03
MODE-1 NOT_DECIDABLE
→ no proposal
→ no version advance

RDP03-AC-04
exact F3 replay after prior commit
→ reattach original authoritative effect
→ no second version advance

RDP03-AC-05
same F3 effect id + changed canonical payload
→ replay conflict

RDP03-AC-06
version advances before first F3 commit
→ conflict
→ no baseVersion-only retry

RDP03-AC-07
MODE-2 F3QuestionSelectionDecision SELECTED
→ Question status SELECTED committed
→ Gap remains not-ASKED
→ pending question absent
→ Consultation not WAITING due to this selection

RDP03-AC-08
MODE-2 D04 STOP / NO_SELECTION
→ no StatePatch

RDP03-AC-09
exact Question selection replay
→ same question_id / effect / proposal
→ no duplicate selected Question

RDP03-AC-10
delivery confirmation for F3 question
→ same QUESTION_DELIVERED_WAIT_EFFECT_ID
→ P01 child effect commits Question DELIVERED + Gap ASKED + pending question
→ Consultation child effect commits WAITING_USER
→ Runtime AWAITING only after both business sub-effects reconcile
→ RDP-04 owns durable choreography

RDP03-AC-11
delivery confirmation for F1 clarification
→ no fake F3 Gap mutation

RDP03-AC-12
delivery failed
→ no DELIVERED
→ no Gap ASKED
→ no WAITING

RDP03-AC-13
MODE-3 REVALIDATED_CURRENT
→ projection only
→ StatePatch count = 0
→ Clinical State Version unchanged

RDP03-AC-14
release NOT_APPLICABLE
→ U06 parent trace records typed NOT_APPLICABLE
→ no placeholder ref

RDP03-AC-15
PROFILE-B synthetic binding
→ parent trace marks synthetic profile
→ no real ACTIVE P06 binding claim

RDP03-AC-16
P01 COMMITTED but read-back mismatches intended effect
→ no downstream route
→ reconciliation failure

RDP03-AC-17
same business effect retry uses new wall-clock time
→ proposal/patch created_at remains first-assigned stable value
→ no P01 idempotency mismatch

RDP03-AC-18
logical_unit_id = U06
→ physical StatePatch producer is approved lower-case service identity
→ C03 binding is not copied into StatePatch capability_id

RDP03-AC-19
exact U06 execution retry
→ same U06_TRACE_ID
→ new attempt evidence may append
→ no new parent trace truth

RDP03-AC-20
PROFILE-B canonical F3 effect
→ effect identity uses SYNTHETIC_VERIFICATION_BINDING + synthetic ref
→ no fabricated real C03 CapabilityBindingRef
→ aggregate compatibility finding remains explicit

RDP03-AC-21
delivery child exact replay
→ same child effect ids
→ same proposal/transition ids
→ same child idempotency keys
→ same child payload fingerprints
→ no second child mutation

RDP03-AC-22
same delivered Clinical State child effect id + changed payload
→ fail closed replay conflict

RDP03-AC-23
different active pending_question exists
→ no REPLACE
→ U06_PENDING_QUESTION_CONFLICT
→ no delivered child commit

RDP03-AC-24
Consultation already WAITING_USER for exact same parent effect/question/delivery
→ exact replay reattach

RDP03-AC-25
Consultation WAITING_USER for different Question/effect
→ U06_CONSULTATION_WAITING_CONFLICT
→ no overwrite
~~~

---

# 55. Readiness blocker disposition

If independent design review passes:

~~~text
BF-U06-RG-03
= CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE
~~~

It remains pending aggregate closure because:
- RDP-04 must freeze delivered/wait cross-store mechanics;
- RDP-06 must verify this contract;
- RDP-01/RDP-05 compatibility amendments must aggregate;
- P01/P05 physical impact choices still need implementation readiness review;
- implementation authorization has not been granted.

---

# 56. Authorization boundary

This design does not authorize:

~~~text
Shared Contracts modification
P01 modification
P05 schema modification
U06 runtime code
C03/D04 activation
Question content
Question delivery
Consultation WAITING_USER
Thread AWAITING_USER
Scheduler activation
merge
production
real-patient traffic
~~~

---

# 57. Independent Design Review Remediation

Initial Independent Design Review:

~~~text
PR #233
review_id = 5287657930
verdict = REVISE_REQUIRED
reviewed_head = 46542fee7b4d84baaa9a500dcecbc761cf056b4f
~~~

Findings:

~~~text
BF-U06-RDP03-IR-01
= LOGICAL_UNIT_PRODUCER_VS_PHYSICAL_STATEPATCH_PRODUCER_UNDERDEFINED

BF-U06-RDP03-IR-02
= DELIVERED_WAIT_MUTATION_DECOMPOSITION_UNDERDEFINED

BF-U06-RDP03-IR-03
= PHASE8_F3_EFFECT_ID_BINDING_COMPATIBILITY_NOT_RECORDED

BF-U06-RDP03-IR-04
= U06_PARENT_TRACE_IDENTITY_AND_REPLAY_LIFECYCLE_UNDERDEFINED
~~~

Remediation applied:

1. separated logical U06/F3 producer ownership from physical lower-case StatePatch producer and P01 state-write authorization identity;

2. froze the delivered/wait parent effect into:
   - P01 Clinical State child effect/proposal;
   - Consultation WAITING child effect/command;
   - Runtime AWAITING only after both business-state sub-effects reconcile;
   - exact pending-question path and no Gap.question_refs mutation at selection;

3. recorded RDP03-COMPAT-F3-ID-01 for Phase-8 F3_CANONICAL_EFFECT_ID binding normalization;

4. froze U06_TRACE_ID derivation, exact replay identity, child attempt evidence, and non-authoritative trace lifecycle.

Targeted Independent Design Re-Review:

~~~text
review_id = 5287675433
verdict = REVISE_REQUIRED
reviewed_head = 907dd78aa40f1557a51aa256d902c2ed92c9ffc0
~~~

Additional findings:

~~~text
BF-U06-RDP03-TR-01
= DELIVERED_CHILD_EFFECT_IDEMPOTENCY_CONTRACT_UNDERDEFINED

BF-U06-RDP03-TR-02
= PENDING_QUESTION_REPLACE_SAFETY_UNDERDEFINED
~~~

Additional remediation:

5. froze stable delivered Clinical State child proposal_id / patch_id / idempotency / canonical payload fingerprint and exact replay conflict semantics;

6. froze stable Consultation WAITING transition_id / idempotency / canonical payload fingerprint and exact replay conflict semantics;

7. prohibited blind pending_question REPLACE:
   - absent -> ADD;
   - same exact parent effect -> replay/reattach;
   - different current pending Question -> conflict;
   - stale pointer replacement requires authoritative stale/superseded proof plus current base version;

8. froze Consultation exact replay to require matching parent effect/question/delivery provenance, not merely lifecycle = WAITING_USER.

Current:

~~~text
BF-U06-RDP03-IR-01
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP03-IR-02
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP03-IR-03
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP03-IR-04
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP03-TR-01
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP03-TR-02
= REMEDIATED / RE-REVIEW_PENDING

U06-RDP-03
= REVISED / READY_FOR_SECOND_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW

BF-U06-RG-03
= OPEN / DESIGN_RE_REVIEW_PENDING

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
~~~

# 58. Revised verdict

~~~text
U06-RDP-03
= REVISED / READY_FOR_SECOND_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW
~~~

No Shared Contracts/P01/P05 modification, C03/D04 activation, runtime implementation, delivery, WAITING_USER activation, merge, production, or real-patient authorization is granted.
