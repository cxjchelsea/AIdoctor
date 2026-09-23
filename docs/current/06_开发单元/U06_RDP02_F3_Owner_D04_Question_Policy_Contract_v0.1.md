# U06-RDP-02 F3 Owner / D04 Question Policy Contract v0.1

> Unit: **U06 — F3 Gap评估 / 关键问题选择 / F3当前版本重验证**  
> Readiness blocker: **BF-U06-RG-02**  
> Parent design: **U06-RDP-05 Capability / Dependency / Applicability Contract**  
> Parent head: **0e9b7be67c349cf4056cc144d4039efe17017241**  
> Runtime repository basis: **main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8**  
> Scope: **F3 OWNER / D04 / QUESTION POLICY DESIGN ONLY**  
> Status: **DRAFT / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document grants no C03/D04 activation, clinical policy approval, patient-facing question approval, implementation, merge, live delivery, production, or real-patient authorization.

---

# 1. Purpose

U06-RDP-02 freezes the executable business-policy boundary between:

~~~text
C03 candidate intelligence
F3 business ownership
D04 deterministic stopping
Question Policy
U06 governed consequence
~~~

Core invariant:

~~~text
C03 Capability Result
!= F3 Business Decision
!= D04 Decision
!= Question truth
!= Clinical Readiness
!= WAITING_USER
~~~

This contract resolves the readiness gap that high-level F3/D04 semantics exist but are not executable enough for U06 implementation.

It freezes:

~~~text
F1 minimal clarification handling
F3 Information Gap owner interpretation
Decision Impact handling
askable-online qualification
C03 business_status interpretation
D04 CONTINUE / STOP semantics
exactly-one next question selection
duplicate / already-answered suppression
USER_UNKNOWN / UNMEASURED handling
no-progress consequence
MODE-3 owner revalidation rules
~~~

It does not invent medical question content, mandatory medical questions, clinical thresholds, diagnostic heuristics, or patient wording safety rules.

---

# 2. Existing frozen ownership

The repository already freezes:

~~~text
Information Gap owner = F3
Question lifecycle owner = F3

Clinical Readiness owner/resolver
= G2 Clinical Readiness Resolver / D03

C03
= candidate capability

D04
= deterministic question stopping policy
~~~

Therefore:

~~~text
F3 may interpret C03
F3 may choose one governed next Question
F3 may produce readiness inputs

F3 may NOT set Clinical Readiness directly

D04 may decide whether questioning may continue
D04 may NOT create clinical facts
D04 may NOT create a Question itself
D04 may NOT set WAITING_USER
~~~

Runtime Thread remains owner of runtime wait/checkpoint state, not Question business truth.

---

# 3. RDP-02 authority model

RDP-02 freezes four different authorities.

## 3.1 C03 — candidate intelligence only

C03 may provide structured candidate material such as:

~~~text
gap candidates
question candidates
decision-impact evidence
askability evidence
duplicate/already-answered indicators
rendering candidate
business_status
~~~

C03 cannot authoritatively decide:

~~~text
canonical F3 truth
whether a Gap is formally current
whether D04 must continue
which Question is selected
Clinical Readiness
WAITING_USER
~~~

## 3.2 F3 Owner — business interpretation authority

F3 owns:

~~~text
what current information gap exists
what decision impact that gap has under the approved policy
whether the gap is a lawful online-question basis
which lawful next question is selected
whether historical F3 may be revalidated to the current version
~~~

F3 interpretation must be deterministic with respect to:

~~~text
authoritative current state
admitted U06 mode/source
exact C03 result when applicable
exact policy refs
exact dependency-binding view
question/history refs
~~~

## 3.3 D04 — continue/stop authority

D04 owns one question:

~~~text
May the current governed question path continue?
~~~

D04 does not own question generation or Clinical Readiness.

## 3.4 Question Policy — versioned policy input

Question Policy supplies the approved deterministic rules required by F3/D04, including as applicable:

~~~text
eligible need classes
allowed question purposes
decision-impact policy categories
askability rules
duplicate/re-ask rules
already-answered rules
USER_UNKNOWN / UNMEASURED rules
selection ordering rules
stopping rules
question budget / turn / cost constraints when approved
rendering-policy constraints
~~~

Question Policy is a governed input, not a code-local constant.

Legacy heuristics must not become Question Policy implicitly.

---

# 4. Explicit legacy rejection

The following legacy patterns are not accepted as new U06 authority:

~~~text
required > important > optional
fixed field priority
completeness >= 0.7
last-3-message keyword duplicate detection
field missing therefore ask
more information may help therefore ask
~~~

They may only be reused if later explicitly reviewed, represented in an approved policy release, and shown compatible with this contract.

Until then:

~~~text
legacy question code
= REFERENCE / REFACTOR ASSET
!= C03 authority
!= F3 authority
!= D04 authority
!= Question Policy
~~~

---

# 5. Policy identities and binding

RDP-02 requires a normalized owner-policy view:

~~~text
U06OwnerPolicyView

f3_owner_policy_ref
question_policy_ref?
d04_policy_ref?

policy_profile
policy_effective_status
policy_scope
policy_population
policy_region
policy_language
policy_channel
policy_contract_version

dependency_binding_type
dependency_binding_ref
execution_profile
~~~

Mode requirements:

| Mode | F3 owner policy | Question policy | D04 policy |
|---|---:|---:|---:|
| MODE-1 PRE_READINESS_GAP_ASSESSMENT | REQUIRED | NOT_REQUIRED_BY_DEFAULT | NOT_REQUIRED_BY_DEFAULT |
| MODE-2 QUESTION_SELECTION_DELIVERY | REQUIRED | REQUIRED | REQUIRED |
| MODE-3 F3_CURRENT_VERSION_REVALIDATION | REQUIRED | NOT_REQUIRED_BY_DEFAULT | NOT_REQUIRED_BY_DEFAULT |

This is a semantic requirement.

Physical P06/registry representation remains an RDP-05/aggregate implementation choice.

RDP-01 currently carries question_policy_ref but does not explicitly carry every normalized policy identity above. Aggregate compatibility review must reconcile this without weakening admission.

---

# 6. Need classes

MODE-2 supports exactly two current U06 need classes:

~~~text
F1_MINIMAL_CLARIFICATION
F3_INFORMATION_GAP
~~~

No third ad-hoc class may be inferred from payload shape.

## 6.1 F1_MINIMAL_CLARIFICATION

Authority source:

~~~text
F1_CLARIFICATION_ROUTING
+ current F1 clarification requirement ref
~~~

Purpose:

~~~text
resolve the minimum missing Subject / Problem Framing information required by F1
~~~

It must not silently expand into broad symptom interrogation, differential diagnosis, or general F3 questioning.

F3 in this path owns Question lifecycle mechanics, but does not take ownership of F1 framing truth.

The selected question must remain bound to:

~~~text
f1_clarification_requirement_ref
source_authority_type = F1_CLARIFICATION_ROUTING
question_need_class = F1_MINIMAL_CLARIFICATION
~~~

Direct F1 runtime activation remains blocked pending the already-recorded controlled amendment.

## 6.2 F3_INFORMATION_GAP

Authority source is normally:

~~~text
U05_QUESTION_ROUTING
+ current readiness/routing refs
+ current F3 gap basis
~~~

The Gap must be:

~~~text
current
not invalidated
not resolved
not superseded
lawfully askable online under the approved policy
material enough under approved decision-impact policy
~~~

No missing field becomes an F3 Information Gap solely because it is null.

---

# 7. F3 Gap interpretation contract

F3 consumes structured C03 output and current authoritative state.

For each candidate Gap, F3 must derive or validate:

~~~text
gap_identity
source_basis_refs[]
target_concept / target_decision
gap_status
decision_impact
askable_online
currentness
resolution_state
prior_question_refs[]
policy_basis
~~~

Allowed existing Gap lifecycle remains:

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

Allowed existing Decision Impact categories remain:

~~~text
BLOCKING
UNCERTAINTY_INCREASING
DEFERABLE
OFFLINE_ONLY
~~~

RDP-02 does not define medical content for assigning these categories.

For PROFILE-A, the values must be supported by an approved real C03/F3 policy package.

For PROFILE-B, values are synthetic predeclared fixtures only.

---

# 8. C03 business_status interpretation

RDP-02 freezes fail-closed interpretation.

## 8.1 SUCCESS

~~~text
SUCCESS
!= automatic F3 acceptance
~~~

F3 must still validate:

~~~text
schema
binding identity
state version
policy compatibility
candidate provenance
currentness
allowed need class
~~~

Only structurally valid and policy-compatible material may enter owner interpretation.

## 8.2 NO_RESULT

~~~text
C03 NO_RESULT
!= no information gap
!= no need to ask
!= D04 STOP
!= READY
~~~

It produces no positive F3 truth.

MODE-1:

~~~text
C03 NO_RESULT
→ F3AssessmentOwnerDecision = NOT_DECIDABLE
→ no canonical positive/negative F3 truth
→ no Question
~~~

MODE-2:

~~~text
C03 NO_RESULT
→ F3QuestionSelectionDecision = NO_SELECTION
→ reason = C03_NO_RESULT
→ D04 CONTINUE/STOP must not be inferred
→ no Question
→ no WAITING_USER
~~~

This capability-no-result branch is not D04 STOP evidence and is not itself F3_QUESTION_PATH_NO_PROGRESS evidence. It remains eligible for governed retry/failure routing or explicit source-owner reevaluation under later RDP/runtime policy.

## 8.3 INSUFFICIENT_INFORMATION

~~~text
INSUFFICIENT_INFORMATION
!= evidence that more questioning is valuable
~~~

It does not automatically authorize another question.

Exact owner consequence is mode-specific.

MODE-1:

~~~text
C03 INSUFFICIENT_INFORMATION
→ F3AssessmentOwnerDecision = NOT_DECIDABLE
→ canonical positive F3 truth = NONE
→ canonical negative/no-gap F3 truth = NONE
→ Question = NONE
~~~

MODE-2:

~~~text
C03 INSUFFICIENT_INFORMATION
→ candidate invention = prohibited
→ D04 CONTINUE inference = prohibited
→ D04 STOP inference = prohibited
→ F3QuestionSelectionDecision = NO_SELECTION
→ reason = C03_INSUFFICIENT_FOR_SELECTION
→ WAITING_USER = prohibited
~~~

If the approved C03 contract classifies the condition as an execution failure rather than a valid insufficient result, the owner decision becomes FAILED instead of NO_SELECTION.

In neither case may INSUFFICIENT_INFORMATION become automatic permission to ask.

## 8.4 DEPENDENCY_FAILURE / TIMEOUT / INVALID_OUTPUT

All are execution/capability failures.

Exact owner mapping:

~~~text
MODE-1
→ F3AssessmentOwnerDecision = FAILED
→ no canonical F3 truth

MODE-2
→ F3QuestionSelectionDecision = FAILED
→ D04 not inferred
→ no Question
→ no WAITING_USER
~~~

They must never become:

~~~text
no gap
STOP because nothing is needed
Clinical Readiness value
patient-facing Question
~~~

They follow governed failure handling and may become U14 eligibility through later runtime/failure routing.

---

# 9. MODE-1 F3 Owner decision

MODE-1 purpose:

~~~text
PRE_READINESS_GAP_ASSESSMENT
= canonical F3 assessment
= no user question
~~~

D04 is not used by default.

RDP-02 defines:

~~~text
F3AssessmentOwnerDecision

decision_id
consultation_id
source_clinical_state_version
source_admission_ref
source_c03_result_ref
dependency_binding_ref
f3_owner_policy_ref
decision_status
normalized_gap_refs[]
canonical_effect_eligibility
reason_codes[]
trace_refs[]
~~~

Allowed decision_status:

~~~text
GAP_BASIS_ESTABLISHED
NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED
NOT_DECIDABLE
FAILED
~~~

## 9.1 GAP_BASIS_ESTABLISHED

Requires:

~~~text
C03 SUCCESS
+ at least one structurally valid owner-accepted Gap basis
+ exact policy/binding compatibility
~~~

This may authorize formation of the canonical F3 StateChangeProposal in RDP-03.

## 9.2 NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED

This is permitted only when the approved C03/F3 owner contract provides explicit positive coverage evidence sufficient for the owner to conclude that no current online-question Gap basis exists.

Forbidden inference:

~~~text
empty candidate list
NO_RESULT
timeout
invalid output
missing field
→ NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED
~~~

For PROFILE-B this outcome may only be fixture-declared and has no clinical meaning.

## 9.3 NOT_DECIDABLE

Used when structurally valid processing completes but the owner lacks sufficient governed evidence to establish either positive Gap basis or explicit no-online-gap basis.

It creates no invented canonical clinical truth.

## 9.4 FAILED

Used for owner-policy/dependency/contract failure.

MODE-1 failure creates no Question and no WAITING_USER.

---

# 10. MODE-1 question-candidate isolation

Any Question candidate material incidentally generated by MODE-1 is:

~~~text
ephemeral
support/trace-only
not authoritative Question state
not reusable by MODE-2
~~~

Therefore MODE-2 must perform a fresh governed C03 invocation/evaluation even when Clinical State Version has not changed.

---

# 11. MODE-2 processing order

After RDP-01 admission succeeds:

~~~text
1. resolve exact owner/question/D04 policies
2. fresh C03 invocation
3. validate C03 result
4. F3 owner normalizes current need/gap and candidate set
5. suppress ineligible / duplicate / already-satisfied candidates
6. invoke D04 on the governed current question path
7. if D04 CONTINUE:
     apply approved deterministic selection ordering
     require exactly one selected Question
8. if D04 STOP:
     produce typed no-progress consequence
9. no delivery occurs in RDP-02 itself
~~~

RDP-02 ends at the business decision boundary.

Physical selection commit is RDP-03.
Delivery is RDP-04.

---

# 12. Candidate eligibility

A Question candidate is eligible only if all applicable checks pass:

~~~text
need class is lawful for admitted source
source requirement/gap is current
target concept/decision is allowed by policy
candidate is bound to current Clinical State Version
dependency binding matches admitted execution
question policy matches admitted execution
candidate is not already satisfied
candidate is not an unresolved duplicate
candidate does not violate USER_UNKNOWN/UNMEASURED rules
candidate is permitted for current channel/language/scope
decision-value evidence required by policy is present
~~~

For F3 Information Gap:

~~~text
askable_online = true
~~~

is required for online Question selection.

For F1 minimal clarification, F3-style clinical decision-impact classification must not be fabricated merely to satisfy a common field. Question Policy must explicitly support the F1 need class.

---

# 13. Duplicate / already-answered suppression

Duplicate control must be semantic and identity-based, not message-window keyword matching.

RDP-02 requires stable:

~~~text
question_semantic_key
~~~

derived from policy-governed semantics such as:

~~~text
question_need_class
source_requirement_ref / gap_ref
target_concepts[]
question_purpose
semantic policy version
~~~

Rendered wording alone is not identity.

Before selection, F3 must inspect authoritative/current references for:

~~~text
current facts/assertions
current Gap status
Question lifecycle history
current Pending Question
ANSWER_RECEIVED
DELIVERED_TO_USER
SELECTED
EXPIRED
SUPERSEDED
USER_UNKNOWN
UNMEASURED
~~~

Suppression rules:

~~~text
current equivalent SELECTED
→ suppress

current equivalent DELIVERED_TO_USER
→ suppress

current equivalent ANSWER_RECEIVED with still-current answer basis
→ suppress

source requirement already RESOLVED / INVALIDATED / superseded
→ suppress
~~~

Re-asking after EXPIRED/SUPERSEDED/changed basis is allowed only if approved re-ask policy explicitly permits it.

No automatic re-ask is authorized.

---

# 14. USER_UNKNOWN handling

USER_UNKNOWN means the user explicitly could not provide the requested information.

It is not:

~~~text
unanswered
negative answer
missing field
permission to repeat the same question
~~~

On unchanged basis:

~~~text
same semantic question
+ USER_UNKNOWN
→ suppress exact re-ask by default
~~~

A different question may only be considered if:
- it targets a materially different allowed concept/decision;
- approved Question Policy permits the alternative;
- D04 still returns CONTINUE.

The system must not mechanically pressure the user by rephrasing the same question indefinitely.

---

# 15. UNMEASURED handling

UNMEASURED means the information has not been measured.

It is not:

~~~text
NORMAL
NEGATIVE
USER_UNKNOWN
~~~

If the required value cannot lawfully be obtained through an online user answer under the policy:

~~~text
UNMEASURED
→ not askable_online
→ no repeated online question
~~~

The Gap may remain an offline-evidence input for later owners.

U06 must not convert UNMEASURED into a fabricated patient answer or resolved Gap.

---

# 16. D04 input contract

RDP-02 defines:

~~~text
D04QuestionStoppingInput

consultation_id
u06_mode = QUESTION_SELECTION_DELIVERY
question_need_class
source_authority_type
source_requirement_ref

clinical_state_version
current_gap_ref?
current_gap_status?
decision_impact?
askable_online?

eligible_candidate_refs[]
suppressed_candidate_refs[]

prior_question_refs[]
pending_question_ref?
user_unknown_refs[]
unmeasured_refs[]

f3_owner_policy_ref
question_policy_ref
d04_policy_ref
dependency_binding_type
dependency_binding_ref
execution_profile

approved_budget_state?
trace_refs[]
~~~

D04 must not fetch hidden mutable clinical state outside the bound input and governed dependencies.

---

# 17. D04 output contract

RDP-02 freezes:

~~~text
D04QuestionStoppingDecision

decision_id
input_fingerprint
decision
reason_code
eligible_candidate_refs[]
policy_ref
clinical_state_version
trace_refs[]
~~~

Allowed decision:

~~~text
CONTINUE
STOP
FAILED
~~~

FAILED is not STOP.

---

# 18. D04 CONTINUE semantics

D04 may return CONTINUE only when:

~~~text
at least one lawful eligible candidate exists
AND approved policy permits continued questioning
AND current need is not already satisfied
AND no current duplicate/pending question blocks selection
AND applicable question budget/policy has not stopped the path
AND required policy inputs are available/current
~~~

For F3 Information Gap, continued questioning must have approved decision-impact justification.

RDP-02 does not define the medical threshold for material enough.

For PROFILE-A that threshold/rule must come from approved clinical Question Policy.

For PROFILE-B it may only be a deterministic synthetic fixture token.

---

# 19. D04 STOP semantics

D04 STOP means:

~~~text
do not select a new Question in this execution
~~~

It does not mean:

~~~text
patient is ready
no clinical gap exists
risk is low
offline evidence is unnecessary
consultation is complete
~~~

Allowed structural reason-code families include:

~~~text
NEED_ALREADY_SATISFIED
NO_LAWFUL_CANDIDATE
NO_CURRENT_ASKABLE_ONLINE_CANDIDATE
NO_APPROVED_DECISION_VALUE_BASIS
DUPLICATE_OR_PENDING_QUESTION
USER_UNKNOWN_REASK_NOT_ALLOWED
UNMEASURED_REQUIRES_NON_ONLINE_PATH
QUESTION_BUDGET_STOP
SOURCE_REQUIREMENT_SUPERSEDED
~~~

These are business-policy reasons, not clinical conclusions.

---

# 20. D04 FAILED semantics

D04 returns FAILED when deterministic stopping cannot be validly evaluated, including:

~~~text
D04_POLICY_MISSING
D04_POLICY_INACTIVE
D04_POLICY_VERSION_MISMATCH
QUESTION_POLICY_MISSING
QUESTION_POLICY_INACTIVE
INVALID_STOPPING_INPUT
STALE_STOPPING_INPUT
DEPENDENCY_BINDING_MISMATCH
AMBIGUOUS_POLICY_RESULT
~~~

Then:

~~~text
Question selection = prohibited
STOP truth = not invented
WAITING_USER = prohibited
~~~

Failure proceeds to governed failure/retry routing outside D04.

---

# 21. Exactly-one next Question

After D04 = CONTINUE, F3 must select exactly one Question through a canonical typed owner decision.

RDP-02 freezes:

~~~text
F3QuestionSelectionDecision

decision_id
input_fingerprint
candidate_set_fingerprint

consultation_id
clinical_state_version
question_need_class
source_authority_type
source_requirement_ref
source_gap_ref?

d04_decision_ref
f3_owner_policy_ref
question_policy_ref
d04_policy_ref
dependency_binding_type
dependency_binding_ref
execution_profile

outcome
selected_candidate_ref?
question_semantic_key?
question_purpose?
target_concepts[]
expected_decision_value_ref?

reason_codes[]
trace_refs[]
~~~

Allowed outcomes:

~~~text
SELECTED
NO_SELECTION
FAILED
~~~

Semantics:

~~~text
SELECTED
→ exactly one selected_candidate_ref
→ exactly one question_semantic_key
→ D04 decision must be CONTINUE

NO_SELECTION
→ no selected_candidate_ref
→ no Question SELECTED state
→ reason must identify a governed no-selection basis

FAILED
→ selection policy/contract could not be validly executed
→ no Question SELECTED state
~~~

The typed decision is the only RDP-02 authority consumed by RDP-03 for Question selection.

RDP-03 must not reconstruct candidate ordering or choose a different candidate.

Authoritative Question status SELECTED exists only after the RDP-03 P01/G2 commit.

---

# 22. Deterministic selection ordering

RDP-02 prohibits:

~~~text
first candidate wins
array order wins
LLM preference wins
random choice
legacy fixed-field order by default
~~~

Question Policy must provide a deterministic total-order or a deterministic policy sufficient to resolve one winner.

The ordering may use only approved policy fields.

If policy cannot resolve a unique winner:

~~~text
QUESTION_SELECTION_AMBIGUOUS
→ no selected Question
→ fail closed
~~~

No arbitrary tie-break may be introduced in code unless the tie-break itself is part of approved policy.

For PROFILE-B, fixture identity may provide deterministic synthetic ordering solely for structural tests.

---

# 23. F1 minimal clarification no-progress

For F1_MINIMAL_CLARIFICATION:

~~~text
goal = resolve exactly the current F1 clarification requirement
~~~

Rules:

~~~text
one current F1 requirement ref
one Question at a time
no broad clinical interrogation expansion
no new F3 canonical Gap invented merely to ask
no Clinical Readiness value invented
no F1 framing truth changed by U06
~~~

If no lawful Question can be selected:

~~~text
F1_CLARIFICATION_NO_PROGRESS
~~~

is emitted with the exact semantic return contract:

~~~text
target_owner = F1
target_unit = U01
source_authority_type = F1_CLARIFICATION_ROUTING
f1_clarification_requirement_ref = preserved
clinical_state_version = preserved/current
u06_admission_ref = preserved
owner/question/D04 policy refs = preserved as applicable
dependency_binding_ref = preserved
question_selection_decision_ref = preserved
trace refs = preserved

Question = NONE
WAITING_USER = NONE
Clinical Readiness decision = NONE
F1 resolution mutation = NONE
~~~

U01/F1 must reevaluate its own clarification requirement under its authority.

U06 may not mark Subject/Problem framing resolved and may not route this no-progress result through U05/D03 as though it were an F3 readiness result.

Runtime activation remains blocked pending the RDP-01 recorded controlled amendment.

---

# 24. F3 no-progress consequence

For F3_INFORMATION_GAP, if D04 = STOP or no lawful candidate remains, U06 must not invent a Clinical Readiness value.

RDP-02 freezes:

~~~text
F3_QUESTION_PATH_NO_PROGRESS
→ READINESS_REEVALUATION_REQUIRED
→ target owner = U05 / D03
~~~

The consequence carries:

~~~text
source U06 execution/admission
current Clinical State Version
current F3 gap refs
D04 decision/ref when present
suppression/no-candidate reasons
policy refs
trace refs
~~~

U05/D03 then decides the next Clinical Readiness value from its own governed inputs.

Therefore:

~~~text
D04 STOP
!= U05 readiness result
~~~

---

# 25. MODE-3 F3 current-version revalidation

MODE-3 remains deterministic F3 Owner logic.

Default:

~~~text
C03 = NOT_INVOKED
D04 = NOT_INVOKED
Question = NONE
StateChangeProposal = NONE
~~~

Allowed outcomes remain exactly:

~~~text
REVALIDATED_CURRENT
REASSESSMENT_REQUIRED
FAILED
~~~

## 25.1 REVALIDATED_CURRENT

May be produced only when all required semantic compatibility evidence is provable, including:

~~~text
canonical F3 source effect exists
historical source refs intact
target Clinical State Version current
current U04 Gate valid for revalidation path
no dependency invalidation invalidates F3 meaning
historical/current binding compatibility proven
policy semantic compatibility proven
source Gap basis not superseded in a way requiring reassessment
~~~

No new canonical F3 mutation is created.

## 25.2 REASSESSMENT_REQUIRED

Required when governed evidence establishes that the old effect cannot be projected as current but a fresh F3 assessment remains a lawful next action, such as:

~~~text
material dependency basis changed
relevant source facts/framing changed
relevant F3 dependencies invalidated
binding/policy semantics changed and compatibility not proven
historical effect cannot safely project to target version
~~~

This does not itself run C03.

It authorizes only governed routing toward fresh MODE-1 admission.

## 25.3 FAILED

Used when revalidation cannot be performed due to missing/corrupt/unavailable required provenance or owner-policy execution failure.

~~~text
FAILED
!= REASSESSMENT_REQUIRED
~~~

A technical inability to verify history must not automatically become a business conclusion that reassessment is lawful.

---

# 26. Policy compatibility for MODE-3

Historical F3 effect may only be revalidated under a newer/current policy if semantic compatibility is explicitly proven.

Forbidden:

~~~text
same policy name
same file path
same package id
newer version exists
→ therefore compatible
~~~

Required:

~~~text
explicit semantic compatibility evidence/ref
~~~

If compatibility is unknown:

~~~text
REVALIDATED_CURRENT = prohibited
~~~

REASSESSMENT_REQUIRED still requires positive governed evidence that fresh reassessment is a lawful next action; otherwise outcome is FAILED.

---

# 27. Question lifecycle boundary

RDP-02 decides business selection only.

It does not perform:

~~~text
Question SELECTED commit
delivery intent
transport send
delivery receipt
DELIVERED_TO_USER commit
WAITING_USER commit
Thread AWAITING_USER
~~~

Those remain:

~~~text
RDP-03
→ selection/business-state mutation

RDP-04
→ delivery/side-effect/reconciliation/wait boundary
~~~

Hard invariant remains:

~~~text
Question SELECTED
!= DELIVERED_TO_USER

WAITING_USER
only after authoritative delivery confirmation
~~~

---

# 28. PROFILE-A real governed policy

For PROFILE-A REAL_GOVERNED_C03, RDP-02 semantics may execute only if RDP-05 requirements are satisfied:

~~~text
registered real C03 role
C03 Capability Quality Gate PASS
approved F3 owner policy
approved Question Policy
approved D04 policy
applicable release/runtime dependencies active
P06 binding valid
environment authorization valid
~~~

Current repository state does not satisfy this profile.

~~~text
PROFILE-A
= BLOCKED
~~~

---

# 29. PROFILE-B synthetic structural policy

For PROFILE-B SYNTHETIC_STRUCTURAL_NONPROD, RDP-02 may be exercised only with deterministic fixture-defined policy results after later explicit implementation authorization.

Synthetic policy may encode:

~~~text
synthetic gap ids
synthetic decision-impact labels
synthetic askable flag
synthetic candidate ids
synthetic CONTINUE / STOP result
synthetic selection order
synthetic USER_UNKNOWN / UNMEASURED branches
~~~

It may not encode or claim:

~~~text
medical question correctness
clinical stopping criteria
patient wording safety
real diagnostic value
mandatory medical question set
real patient prioritization
C03 Quality Gate PASS
D04 clinical approval
~~~

External/live delivery remains zero.

---

# 30. F3/D04 failure taxonomy

RDP-02 freezes at least:

~~~text
F3_OWNER_POLICY_MISSING
F3_OWNER_POLICY_INACTIVE
F3_OWNER_POLICY_VERSION_MISMATCH

C03_RESULT_MISSING
C03_RESULT_INVALID
C03_RESULT_STALE
C03_RESULT_BINDING_MISMATCH
C03_NO_RESULT
C03_INSUFFICIENT_INFORMATION
C03_INSUFFICIENT_FOR_SELECTION
C03_DEPENDENCY_FAILURE
C03_TIMEOUT

QUESTION_POLICY_MISSING
QUESTION_POLICY_INACTIVE
QUESTION_POLICY_VERSION_MISMATCH

D04_POLICY_MISSING
D04_POLICY_INACTIVE
D04_POLICY_VERSION_MISMATCH
D04_INVALID_INPUT
D04_STALE_INPUT
D04_AMBIGUOUS_RESULT

NO_LAWFUL_QUESTION_CANDIDATE
QUESTION_SELECTION_AMBIGUOUS
QUESTION_DUPLICATE_CURRENT
QUESTION_ALREADY_SATISFIED

F3_REVALIDATION_PROVENANCE_MISSING
F3_REVALIDATION_COMPATIBILITY_UNKNOWN
F3_REVALIDATION_STALE_BEFORE_PUBLISH
~~~

Failure reason codes are trace/audit facts, not Clinical Truth.

---

# 31. RDP-01 compatibility

RDP-01 remains owner of admission.

RDP-02 consumes only an ADMITTED U06 input and may not weaken:
- source/mode legality;
- currentness;
- Safety permission;
- dependency binding checks;
- replay identity.

Aggregate amendment impacts:

~~~text
RDP02-COMPAT-01
normalized owner-policy identities
(f3_owner_policy_ref / d04_policy_ref)
must be representable by admission/dependency views

RDP02-COMPAT-02
F1 no-progress consequence must preserve existing
activation-blocked status of direct F1 routing
~~~

No upstream activation is granted.

---

# 32. RDP-05 compatibility

RDP-02 consumes:

~~~text
REAL_CAPABILITY_BINDING
SYNTHETIC_VERIFICATION_BINDING
~~~

and preserves:

~~~text
expected_capability_role = C03
~~~

for real MODE-1/MODE-2 execution.

RDP-02 does not turn adult_respiratory_v1 into a C03 implementation or D04 policy authority.

Question/F3/D04 policy refs must be compatible with the same execution profile and dependency-binding view.

---

# 33. RDP-03 input obligations

RDP-03 must consume owner outputs without reinterpretation:

~~~text
F3AssessmentOwnerDecision
D04QuestionStoppingDecision
F3QuestionSelectionDecision
F1_CLARIFICATION_NO_PROGRESS
F3_QUESTION_PATH_NO_PROGRESS
F3CurrentVersionRevalidationDecision
~~~

RDP-03 must preserve exact policy/binding/state-version provenance.

RDP-03 may define physical StateChangeProposal shape and commit grouping, but may not re-run F3/D04 policy under different rules.

---

# 34. RDP-04 input obligations

RDP-04 may deliver only an authoritative committed Question selection produced from this contract.

RDP-04 may not:
- choose another candidate;
- rewrite question purpose;
- bypass D04;
- mark WAITING_USER after failed delivery;
- convert STOP/no-progress into delivery.

---

# 35. RDP-06 verification obligations

RDP-06 must verify at least:

~~~text
MODE-1:
C03 SUCCESS valid gap -> GAP_BASIS_ESTABLISHED
C03 NO_RESULT -> no invented no-gap truth
empty candidates without explicit coverage -> NOT_DECIDABLE
C03 timeout/failure -> no canonical positive F3 truth
MODE-1 question candidate never reused by MODE-2

MODE-2:
F1 need isolated from F3 clinical interrogation
fresh C03 invocation
duplicate suppression
already-answered suppression
USER_UNKNOWN exact re-ask suppression
UNMEASURED no fabricated answer
D04 CONTINUE only with lawful candidate
D04 STOP != readiness
D04 FAILED != STOP
exactly one selected Question
ambiguous selection -> fail closed
no arbitrary array-order selection
no delivery in RDP-02 stage

no-progress:
F1 -> F1 owner/source reevaluation consequence
F3 -> U05 readiness reevaluation consequence
no direct readiness value invented

MODE-3:
default no C03
default no D04
REVALIDATED_CURRENT requires proven compatibility
unknown compatibility cannot revalidate
technical failure != automatic reassessment

PROFILE-B:
synthetic fixture only
no clinical correctness claim
no live delivery
~~~

---

# 36. Design acceptance scenarios

~~~text
RDP02-AC-01
MODE-1 C03 SUCCESS + valid current synthetic Gap fixture
→ F3 GAP_BASIS_ESTABLISHED
→ no Question

RDP02-AC-02
MODE-1 C03 NO_RESULT
→ not NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED
→ no invented F3 negative truth

RDP02-AC-03
MODE-1 empty candidate list without explicit coverage evidence
→ NOT_DECIDABLE

RDP02-AC-04
MODE-2 F1 clarification
→ selected candidate binds exact F1 clarification requirement
→ no general F3 interrogation expansion

RDP02-AC-04A
MODE-2 F1 clarification with no lawful candidate
→ F1_CLARIFICATION_NO_PROGRESS
→ target owner F1 / target unit U01
→ no F1 resolution mutation
→ no U05/D03 readiness decision

RDP02-AC-05
MODE-2 F3 current Gap + current equivalent DELIVERED Question
→ suppress duplicate
→ no second selection

RDP02-AC-06
same semantic question + USER_UNKNOWN + unchanged basis
→ suppress exact re-ask

RDP02-AC-07
UNMEASURED + policy says non-online obtainable
→ no online selection
→ no fabricated answer

RDP02-AC-08
D04 CONTINUE + two candidates + approved unique ordering
→ F3QuestionSelectionDecision = SELECTED
→ exactly one selected_candidate_ref

RDP02-AC-09
D04 CONTINUE + unresolved top tie
→ F3QuestionSelectionDecision = FAILED
→ QUESTION_SELECTION_AMBIGUOUS
→ no Question

RDP02-AC-09A
MODE-2 C03 INSUFFICIENT_INFORMATION
→ F3QuestionSelectionDecision = NO_SELECTION
→ C03_INSUFFICIENT_FOR_SELECTION
→ no inferred D04 CONTINUE/STOP
→ no WAITING_USER

RDP02-AC-10
D04 STOP
→ no Question
→ no WAITING_USER
→ no Clinical Readiness invented

RDP02-AC-11
F3 no-progress
→ READINESS_REEVALUATION_REQUIRED
→ target owner U05/D03

RDP02-AC-12
D04 policy unavailable
→ FAILED
→ not STOP

RDP02-AC-13
MODE-3 semantic compatibility proven
→ REVALIDATED_CURRENT
→ no C03/D04 invocation

RDP02-AC-14
MODE-3 technical provenance unavailable
→ FAILED
→ not automatic REASSESSMENT_REQUIRED

RDP02-AC-15
PROFILE-B fixture encodes clinical threshold/patient wording claim
→ reject fixture/profile
~~~

---

# 37. Readiness blocker disposition

If independent design review passes:

~~~text
BF-U06-RG-02
= CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE
~~~

It must not be marked fully closed until:
- RDP-01/RDP-05 compatibility impacts are aggregated;
- RDP-03/RDP-04 consume owner outputs consistently;
- RDP-06 verifies the frozen package;
- Implementation Readiness Re-Evaluation passes.

---

# 38. Current implementation posture

Current repository evidence remains:

~~~text
real C03 = not active
real D04 = absent/not approved
real Question Policy = not approved
patient-facing question content = not approved
~~~

Therefore:

~~~text
PROFILE-A REAL_GOVERNED_C03
= BLOCKED

PROFILE-B SYNTHETIC_STRUCTURAL_NONPROD
= DESIGN-ELIGIBLE
= NOT_IMPLEMENTATION_AUTHORIZED
~~~

RDP-02 does not authorize PROFILE-B implementation.

---

# 39. Authorization boundary

This design does not authorize:

~~~text
medical question content creation
mandatory question set
clinical stopping threshold
patient wording approval

C03 activation
D04 activation
Question Policy activation
adult_respiratory runtime adoption

P06/P05 shared-runtime amendment
U06 implementation
Question commit
delivery
WAITING_USER
Scheduler activation
merge
production
real-patient traffic
~~~

---

# 40. Independent Design Review Remediation

Initial Independent Design Review:

~~~text
PR #232
review_id = 5287560207
verdict = REVISE_REQUIRED
reviewed_head = e1bdf0388e94d1d1dfb24c798f133fc80bccead9
~~~

Findings:

~~~text
BF-U06-RDP02-IR-01
= TYPED_QUESTION_SELECTION_OWNER_DECISION_MISSING

BF-U06-RDP02-IR-02
= F1_NO_PROGRESS_RETURN_TARGET_NOT_EXACT

BF-U06-RDP02-IR-03
= C03_INSUFFICIENT_INFORMATION_OUTCOME_UNDERDEFINED
~~~

Remediation applied:

1. froze canonical F3QuestionSelectionDecision with:
   - decision/input/candidate-set identity;
   - exact policy and dependency provenance;
   - SELECTED / NO_SELECTION / FAILED outcomes;
   - exactly-one selected candidate semantics;
   - RDP-03 no-reinterpretation rule;

2. froze F1_CLARIFICATION_NO_PROGRESS exact semantic target:
   - owner F1;
   - unit U01;
   - preserved requirement/admission/version/policy/binding/trace provenance;
   - no F1 resolution;
   - no U05/D03 readiness decision;

3. froze C03 INSUFFICIENT_INFORMATION per mode:
   - MODE-1 -> NOT_DECIDABLE;
   - MODE-2 -> NO_SELECTION / C03_INSUFFICIENT_FOR_SELECTION unless the approved contract classifies it as FAILED;
   - never inferred CONTINUE/STOP or WAITING_USER;

4. hardened the remaining C03 status mappings:
   - NO_RESULT -> MODE-1 NOT_DECIDABLE / MODE-2 NO_SELECTION;
   - DEPENDENCY_FAILURE / TIMEOUT / INVALID_OUTPUT -> owner/selection FAILED;
   - none of these statuses may be inferred as D04 STOP or Clinical Readiness.

Current:

~~~text
BF-U06-RDP02-IR-01
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP02-IR-02
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP02-IR-03
= REMEDIATED / RE-REVIEW_PENDING

U06-RDP-02
= REVISED / READY_FOR_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW

BF-U06-RG-02
= OPEN / DESIGN_RE_REVIEW_PENDING

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
~~~

# 41. Revised verdict

~~~text
U06-RDP-02 F3 Owner / D04 Question Policy Contract
= REVISED / READY_FOR_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW
~~~

No C03/D04 activation, medical question content, clinical stopping threshold, patient-facing question approval, implementation, delivery, merge, production, or real-patient authorization is granted.
