# U06-RDP-04 Delivery / Downstream / Side-effect Boundary v0.1

> Unit: **U06 — Question Delivery / WAITING_USER Entry**  
> Readiness blocker: **BF-U06-RG-04**  
> Parent mutation design: **U06-RDP-03 State Ownership / K09-P01 Mutation / Trace Contract**  
> Parent head: **09bb7f0244300fdfc750065823361ef7846802f0**  
> Runtime repository basis: **main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8**  
> Scope: **DELIVERY / DOWNSTREAM / SIDE-EFFECT DESIGN ONLY**  
> Status: **DRAFT / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document grants no real patient delivery, external transport activation, production traffic, U07 implementation, merge, or production authorization.

---

# 1. Purpose

RDP-03 已冻结：

~~~text
Question SELECTED
!= DELIVERED_TO_USER

Gap ASKED
only after authoritative delivery confirmation

Question DELIVERED_TO_USER
+ Pending Question
+ Consultation WAITING_USER
= business-state effects

Thread AWAITING_USER
= Runtime state only

QUESTION_DELIVERED_WAIT_EFFECT_ID
= parent business effect identity
~~~

RDP-04 回答：

> 一个已经 authoritative SELECTED 的 Question，如何经过 durable intent、稳定 delivery identity、transport、receipt、confirmation、cross-store reconciliation，最终形成可信的 DELIVERED_TO_USER / WAITING_USER / AWAITING_USER 边界，并保证 crash/retry 不重复发送、不伪造送达。

核心不变量：

~~~text
Question SELECTED
!= delivery intent
!= transport attempt
!= transport receipt
!= delivery confirmation
!= DELIVERED_TO_USER
!= WAITING_USER
!= AWAITING_USER
~~~

---

# 2. Current repository reality

Current main has no governed U06 delivery runtime implementing:

~~~text
DeliveryIntent
DeliveryOutbox
DeliveryReceipt
DeliveryLedger
Question delivery adapter
PendingQuestion store
U06 wait checkpoint
~~~

Current Shared Contract PatientDeliveryView exists, but it is a patient-facing delivery view contract for broader delivery semantics and is not by itself a U06 Question transport protocol.

Current capability delivery policy is:

~~~text
review_status = REQUIRES_CLINICAL_REVIEW
runtime_eligibility = NOT_IMPLEMENTED
production_eligibility = BLOCKED
prohibited_runtime_use = true
~~~

Therefore:

~~~text
real patient-facing U06 delivery
= BLOCKED

synthetic structural non-production delivery
= designable
= implementation requires later explicit authorization
~~~

---

# 3. Delivery ownership split

RDP-04 freezes these owners:

~~~text
F3
= Question business truth owner

RDP-03 / P01
= authoritative Question / Gap / PendingQuestion Clinical State mutation

Delivery Coordinator
= durable intent + transport/reconciliation orchestration

Transport Adapter
= external or synthetic I/O attempt only

Delivery Confirmation Resolver
= interprets receipt/reconciliation evidence into CONFIRMED / NOT_CONFIRMED / INDETERMINATE / FAILED

Consultation lifecycle owner
= WAITING_USER business lifecycle transition

Runtime
= Thread AWAITING_USER + checkpoint only after business wait state is authoritative

U07
= answer/resume owner after valid wait boundary
~~~

Forbidden:

~~~text
Transport Adapter
→ direct Question DELIVERED_TO_USER

Frontend response success
→ direct WAITING_USER

Runtime checkpoint
→ delivery truth

HTTP 200 alone
→ patient received question

selected Question
→ U07 eligibility
~~~

---

# 4. Delivery profiles

RDP-04 freezes two execution profiles.

## 4.1 PROFILE-A REAL_PATIENT_DELIVERY

Requires all of:

~~~text
real approved Question content
real approved Question Policy
real approved delivery policy
approved recipient/channel binding
approved transport adapter
transport idempotency/reconciliation capability
privacy/consent authorization
current Safety permission
delivery E2E evidence
production authorization
~~~

Current state:

~~~text
PROFILE-A
= BLOCKED
~~~

## 4.2 PROFILE-B SYNTHETIC_NONLIVE_DELIVERY

Purpose:

~~~text
verify durable intent
verify delivery identity
verify transport idempotency
verify receipt reconciliation
verify confirmed delivery business-state choreography
verify crash/replay
verify U07 eligibility boundary
~~~

Restrictions:

~~~text
no real external endpoint
no patient phone/email/account
no live push/SMS/voice/websocket side effect
no production recipient identity
no clinical correctness claim
no patient wording safety claim
no real-delivery evidence claim
~~~

Synthetic confirmation may drive structural business-state transitions only for explicitly synthetic/non-production fixture consultations and stores.

It may not be interpreted as proof that a real user saw a Question.

---

# 5. Delivery entry precondition

RDP-04 may begin only from an authoritative current:

~~~text
QuestionStateValue.status = SELECTED
~~~

and exact matching:

~~~text
QUESTION_SELECTION_EFFECT_ID
F3QuestionSelectionDecision
D04 CONTINUE decision
U06 admission
dependency/policy provenance
selected content ref/fingerprint
current Clinical State Version
~~~

Before creating a delivery intent, revalidate:

~~~text
Question still SELECTED
Question not EXPIRED
Question not SUPERSEDED
Question not ANSWER_RECEIVED
Question not invalidated by upstream state
no different current pending_question
Consultation lifecycle compatible with delivery
current Safety / permission allows delivery action
delivery profile authorized
~~~

If any check fails:

~~~text
no new delivery intent
no transport attempt
no WAITING_USER
~~~

---

# 6. Delivery content freeze boundary

Transport must send exactly the content authorized by the selected Question effect.

Define:

~~~text
QuestionDeliveryContentBinding

question_id
question_selection_effect_id

rendered_content_ref
rendered_content_fingerprint

language
channel

question_policy_ref
rendering_policy_ref?
safety_content_review_ref?

source_clinical_state_version
~~~

After delivery intent is durable:

~~~text
rendered content
must not silently change
~~~

If content changes materially:

~~~text
new content fingerprint
→ new delivery business identity
or
→ selection must be superseded/reselected according to policy
~~~

Forbidden:

~~~text
same delivery_id
+ changed rendered content
~~~

---

# 7. Recipient / channel authority

Delivery intent must bind a governed recipient/channel route.

Logical:

~~~text
DeliveryEndpointBinding

recipient_scope_ref
recipient_endpoint_ref

channel
channel_binding_ref

consent_ref?
privacy_scope_ref?
restricted_permission_ref?

binding_status
binding_version
~~~

No raw phone/email/token is required in U06 parent trace.

Recipient endpoint data should remain in the delivery adapter/security boundary.

Allowed binding_status:

~~~text
ACTIVE
DISABLED
EXPIRED
UNAVAILABLE
~~~

Only ACTIVE may form a new transport intent.

---

# 8. Stable delivery business identity

Define:

~~~text
QUESTION_DELIVERY_EFFECT_ID
~~~

Minimum semantic derivation:

~~~text
consultation_id
+ question_id
+ QUESTION_SELECTION_EFFECT_ID
+ rendered_content_fingerprint
+ recipient_scope_ref
+ recipient_endpoint_ref
+ channel
+ channel_binding_ref
+ delivery_policy_version
+ delivery_effect_contract_version
~~~

Then:

~~~text
delivery_id
= stable identity derived/assigned once per QUESTION_DELIVERY_EFFECT_ID
~~~

Same exact delivery effect replay:

~~~text
→ same delivery_id
~~~

Different content / endpoint / channel / policy:

~~~text
→ different delivery effect identity
or explicit conflict if mutation is not lawful
~~~

---

# 9. One active delivery effect per selected Question

For one current:

~~~text
QUESTION_SELECTION_EFFECT_ID
~~~

there may be at most one active/pending Question delivery effect.

Define logical durable authority:

~~~text
QuestionDeliveryAuthority

question_selection_effect_id
question_id

active_delivery_effect_id?
active_delivery_id?

authority_status
superseded_delivery_effect_refs[]

updated_at
trace_refs[]
~~~

Allowed authority_status:

~~~text
NO_ACTIVE_DELIVERY
ACTIVE_PENDING
CONFIRMED_TERMINAL
TERMINAL_NOT_DELIVERED
BLOCKED_RECONCILIATION
~~~

Rules:

~~~text
no prior delivery effect
→ one new effect may become ACTIVE_PENDING

before first transport attempt:
changed content / endpoint / channel / policy
→ old READY intent may be governed CANCELLED_BEFORE_SEND
→ authority may replace it with one new delivery effect
→ old effect remains auditable

after any transport attempt with ambiguous outcome:
→ new delivery effect for same selection prohibited
→ authority = BLOCKED_RECONCILIATION
→ old effect must reconcile first

old effect definitively NOT_DELIVERED
→ authority may become TERMINAL_NOT_DELIVERED
→ retry policy may allow another attempt under same effect
→ a distinct new delivery effect requires an explicit governed rebinding decision

old effect CONFIRMED
→ authority = CONFIRMED_TERMINAL
→ no second delivery effect for same Question selection

redelivery / re-ask after a confirmed delivery
→ requires a new lawful Question selection/re-ask effect
→ not ad hoc delivery rebinding
~~~

This authority is non-clinical delivery governance state.

It does not own Question truth.

---

# 10. Delivery idempotency key

Define:

~~~text
DELIVERY_IDEMPOTENCY_KEY
=
QUESTION_DELIVERY_EFFECT_ID
+ transport_contract_version
~~~

Rules:

~~~text
same delivery effect
→ same delivery_id
→ same idempotency key

retry attempt
→ new attempt_id allowed
→ same delivery idempotency key
~~~

Transport attempt identity is not delivery business identity.

---

# 11. Durable DeliveryIntent

Before any send attempt, persist:

~~~text
QuestionDeliveryIntent

delivery_id
delivery_effect_id
delivery_idempotency_key

consultation_id
question_id
question_selection_effect_id

rendered_content_ref
rendered_content_fingerprint

recipient_scope_ref
recipient_endpoint_ref
channel
channel_binding_ref

delivery_profile
delivery_policy_ref

source_clinical_state_version
source_question_state_ref

intent_status

created_at
trace_refs[]
~~~

Allowed intent_status:

~~~text
READY
CANCELLED_BEFORE_SEND
TERMINAL_FAILURE
~~~

Intent is immutable in business-defining fields after READY.

---

# 12. Intent durability rule

Formal ordering:

~~~text
Question SELECTED authoritative
→ DeliveryIntent durable READY
→ only then transport attempt
~~~

Forbidden:

~~~text
send first
→ persist intent later
~~~

because crash after send would lose stable reconciliation identity.

If crash happens after selection but before intent durability:

~~~text
retry reloads current Question
→ derives same QUESTION_DELIVERY_EFFECT_ID
→ creates/reuses same DeliveryIntent
→ no external send happened yet
~~~

---

# 13. Transport attempt contract

Logical:

~~~text
QuestionDeliveryAttempt

attempt_id
delivery_id
delivery_idempotency_key

adapter_id
adapter_version
channel

attempt_number
attempt_started_at
attempt_finished_at?

attempt_status
transport_request_ref?
transport_response_ref?
failure_code?
~~~

Allowed attempt_status:

~~~text
STARTED
ACKNOWLEDGED
DEFINITIVE_FAILURE
AMBIGUOUS
CANCELLED
~~~

Attempt number is operational metadata.

It must not alter:
- delivery effect identity;
- question identity;
- delivery idempotency key.

---

# 14. Transport idempotency capability contract

Each transport adapter must declare:

~~~text
TransportDeliveryCapability

adapter_id
adapter_version
channel

supports_idempotency_key
supports_status_query
supports_receipt_identity
supports_exact_delivery_reconciliation
~~~

For PROFILE-A, at least one of the following must be true:

~~~text
A. adapter honors stable idempotency key end-to-end

or

B. adapter supports authoritative status query/reconciliation
   by stable delivery/provider identity
~~~

If neither is available:

~~~text
ambiguous send retry
= prohibited
~~~

No optimistic duplicate resend.

---

# 15. Delivery receipt contract

Durable transport evidence:

~~~text
QuestionDeliveryReceipt

receipt_id
delivery_id
attempt_id

adapter_id
provider_message_ref?
provider_idempotency_ref?

receipt_status
provider_status_code?
provider_status_ref?

received_at
raw_evidence_ref?
~~~

Allowed receipt_status:

~~~text
ACCEPTED
REJECTED
DELIVERED
FAILED
UNKNOWN
~~~

Meaning:

~~~text
ACCEPTED
= transport/provider accepted request
!= user delivery confirmation

DELIVERED
= provider/adapter evidence meets the configured delivery confirmation standard

REJECTED / FAILED
= no confirmed delivery

UNKNOWN
= transport outcome uncertain
~~~

---

# 16. Delivery confirmation resolver

Define:

~~~text
QuestionDeliveryConfirmationDecision

confirmation_decision_id
delivery_id

source_intent_ref
source_attempt_refs[]
source_receipt_refs[]

delivery_profile
confirmation_policy_ref

confirmation_status
reason_code

confirmed_content_fingerprint?
confirmed_endpoint_ref?
confirmed_at?

trace_refs[]
~~~

Allowed:

~~~text
CONFIRMED
NOT_CONFIRMED
INDETERMINATE
FAILED
~~~

Only CONFIRMED may authorize RDP-03 delivered/wait child effects.

---

# 17. Delivery confirmation identity and evidence evolution

Define:

~~~text
DELIVERY_CONFIRMATION_EVALUATION_ID
=
delivery_id
+ canonical_delivery_evidence_set_identity
+ confirmation_policy_version
+ confirmation_contract_version
~~~

Define:

~~~text
DELIVERY_CONFIRMATION_CANONICAL_FINGERPRINT
~~~

covering:

~~~text
delivery_id
intent identity/fingerprint
evaluated attempt refs
evaluated receipt refs
status-query evidence refs
recipient/channel binding
rendered content fingerprint
confirmation policy/version
confirmation outcome
reason code
~~~

Rules:

~~~text
same exact evidence set + same policy
→ same evaluation identity
→ same confirmation result

new authoritative receipt/query evidence
→ new confirmation evaluation
→ may supersede prior non-terminal INDETERMINATE evaluation
→ prior evaluation remains immutable/auditable

CONFIRMED
→ terminal positive delivery truth
→ cannot later be rewritten to NOT_CONFIRMED

evidence conflicting with prior CONFIRMED
→ DELIVERY_CONFIRMATION_EVIDENCE_CONFLICT
→ reconciliation/failure
→ do not rewrite historical confirmation

NOT_CONFIRMED
→ terminal only when policy/evidence proves non-delivery or terminal rejection/failure

INDETERMINATE
→ non-terminal
→ may be superseded by later evidence
~~~

confirmation_decision_id must equal or stably derive from DELIVERY_CONFIRMATION_EVALUATION_ID.

---

# 18. CONFIRMED semantics

CONFIRMED requires:

~~~text
same delivery_id
same rendered_content_fingerprint
same recipient/channel binding
valid delivery evidence under confirmation policy
Question still current/selectable for delivery reconciliation
no conflicting authoritative delivery effect
~~~

For PROFILE-A:

~~~text
confirmation evidence
must come from approved real adapter semantics
~~~

For PROFILE-B:

~~~text
confirmation evidence
must be explicitly SYNTHETIC
and scoped to synthetic fixture consultation
~~~

Synthetic confirmation cannot be reused as real-patient evidence.

---

# 19. ACCEPTED != CONFIRMED

Critical rule:

~~~text
provider accepted request
!= DELIVERED_TO_USER
~~~

A transport/API success response may mean:
- queued;
- accepted;
- stored;
- handed to downstream provider.

Therefore:

~~~text
receipt_status = ACCEPTED
→ confirmation_status may remain INDETERMINATE
~~~

unless the approved adapter contract explicitly defines ACCEPTED as sufficient delivery evidence for that channel.

That policy must be explicit and versioned.

---

# 20. INDETERMINATE semantics

Used when:

~~~text
send may have happened
but final delivery cannot yet be proven
~~~

Examples:
- connection lost after request transmission;
- provider timeout after acceptance;
- missing receipt;
- provider query temporarily unavailable.

Then:

~~~text
Question remains SELECTED
Gap remains not ASKED
pending_question remains absent
Consultation remains not WAITING_USER because of this Question
Thread remains not AWAITING_USER because of this Question
~~~

The delivery attempt enters reconciliation.

---

# 21. Retry after ambiguous transport

For INDETERMINATE:

If adapter supports authoritative status query:

~~~text
query/reconcile first
→ if delivered -> CONFIRMED
→ if definitively not delivered -> retry may be eligible
→ if still unknown -> no duplicate send
~~~

If adapter supports idempotent resend:

~~~text
same delivery_idempotency_key
→ retry allowed under delivery retry policy
~~~

If adapter supports neither:

~~~text
retry send prohibited
→ governed failure/reconciliation handoff
~~~

This prevents duplicate user messages.

---

# 22. Definitive delivery failure

If receipt/adapter proves:

~~~text
REJECTED
or
FAILED
~~~

then:

~~~text
confirmation_status = NOT_CONFIRMED
~~~

No:
- Question DELIVERED_TO_USER;
- Gap ASKED;
- pending_question;
- Consultation WAITING_USER;
- Thread AWAITING_USER;
- U07 eligibility.

Retry is allowed only by approved delivery retry policy and only while selected Question remains current.

---

# 23. Delivery retry policy

Logical:

~~~text
U06DeliveryRetryPolicy

policy_id
policy_version

max_attempts
retryable_failure_classes[]
non_retryable_failure_classes[]

requires_status_query_before_retry
requires_same_idempotency_key

backoff_policy_ref?
expiry_policy_ref?
~~~

RDP-04 does not choose concrete production retry numbers.

PROFILE-B may use deterministic fixture retry limits.

No implementation may invent infinite retries.

---

# 24. Delivery cancellation before confirmation

Delivery must stop if current authoritative state shows:
- Question SUPERSEDED;
- Question EXPIRED;
- Question ANSWER_RECEIVED by another accepted path;
- Consultation cancelled/expired/safe-exited;
- Safety now blocks delivery;
- endpoint permission revoked.

If no transport send happened:

~~~text
intent may become CANCELLED_BEFORE_SEND
~~~

If send outcome is ambiguous:

~~~text
must reconcile
cannot assume cancellation prevented delivery
~~~

If external delivery already CONFIRMED:

~~~text
business delivered/wait reconciliation must decide authoritative current state
not pretend delivery never occurred
~~~

---

# 25. Delivery confirmation to business-state handoff

Only:

~~~text
QuestionDeliveryConfirmationDecision = CONFIRMED
~~~

may produce:

~~~text
QUESTION_DELIVERED_WAIT_EFFECT_ID
~~~

from RDP-03.

Required equality:

~~~text
confirmation.delivery_id
= parent delivered-wait delivery_id

confirmation content fingerprint
= selected Question delivery content fingerprint

question_id / selection effect
= current authoritative selected Question

recipient/channel provenance
= intent binding

delivery profile
= current authorized profile
~~~

---

# 26. Business-state choreography

RDP-03 froze two business child effects.

RDP-04 freezes the normal order:

~~~text
D1. Delivery Confirmation = CONFIRMED

D2. reconcile/commit
    QUESTION_DELIVERED_CLINICAL_STATE_EFFECT_ID
    through P01

D3. authoritative Clinical State read-back proves:
    Question DELIVERED_TO_USER
    + PendingQuestion current
    + Gap ASKED when F3 need

D4. reconcile/commit
    CONSULTATION_WAITING_EFFECT_ID

D5. authoritative Consultation read-back proves:
    WAITING_USER
    + exact parent effect/question/delivery provenance

D6. create durable Runtime wait checkpoint

D7. transition Thread/Run execution state to AWAITING_USER

D8. emit U07 eligibility projection
~~~

Why Clinical State child first:

~~~text
Consultation must never expose WAITING_USER
without an authoritative delivered Question
~~~

---

# 27. Partial business-state reconciliation

Cross-store mutation is not physically atomic.

Therefore a temporary state may exist internally:

~~~text
Clinical State says Question DELIVERED
but Consultation WAITING child not committed yet
~~~

This state is:

~~~text
DELIVERY_RECONCILIATION_PENDING
~~~

It is not normal downstream-ready success.

During reconciliation pending:

~~~text
no Thread AWAITING_USER
no U07 eligibility
no second Question delivery
no next ordinary U06 progression
~~~

Runtime must finish reconciliation or enter governed failure recovery.

---

# 28. Consultation WAITING failure after delivered Clinical State

If:
- Clinical State delivered child committed;
- Consultation WAITING child fails/conflicts;

then do not roll back historical delivery truth.

Required:

~~~text
preserve Question DELIVERED_TO_USER
preserve delivery evidence
preserve Gap ASKED if committed
block U07 eligibility
reconcile Consultation lifecycle
~~~

If Consultation has moved to an incompatible terminal state:

~~~text
WAITING_USER must not overwrite it
→ typed conflict/failure
→ U14/U15-governed handling as applicable
~~~

Do not mutate Question back to SELECTED merely to create artificial consistency.

---

# 29. Runtime wait checkpoint contract

After both business sub-effects are authoritative, create:

~~~text
U06WaitCheckpoint

checkpoint_id
consultation_id
thread_id
run_id

question_id
pending_question_ref

question_selection_effect_id
question_delivery_effect_id
question_delivered_wait_effect_id

delivery_id
delivery_confirmation_ref

clinical_state_version
consultation_wait_effect_ref

dependency_binding_ref
question_policy_ref

runtime_schema_version
created_at
~~~

Checkpoint:

~~~text
= execution metadata
!= delivery truth
!= Clinical State
!= Consultation lifecycle truth
~~~

---

# 30. Thread AWAITING_USER transition

Thread may transition to AWAITING_USER only when:

~~~text
Clinical State read-back
= delivered child authoritative

Consultation read-back
= WAITING_USER for same parent effect

checkpoint durable
= compatible with both business states
~~~

If Thread transition fails after business states are complete:

~~~text
business wait remains authoritative
Runtime reconstructs/retries AWAITING_USER from business state + checkpoint/effect evidence
~~~

No duplicate Question send.

---

# 31. U07 eligibility contract

U07 eligibility exists only when all are true:

~~~text
Question = DELIVERED_TO_USER
PendingQuestion = current same Question
Consultation = WAITING_USER
Thread = AWAITING_USER
delivery_id / parent effect provenance match
wait checkpoint compatible
Question not expired/superseded
~~~

If business WAITING state exists but Runtime wait state/checkpoint is missing, stale, or not yet reconciled:

~~~text
WAIT_RUNTIME_RECONCILIATION_REQUIRED
→ U07 eligibility = absent
~~~

Runtime must first reconstruct/reconcile Thread AWAITING_USER from authoritative business wait evidence and durable delivery/checkpoint evidence.

Only after Thread = AWAITING_USER may U07ResumeEligibility be emitted or reattached.

Define:

~~~text
U07ResumeEligibility

eligibility_id

consultation_id
question_id
pending_question_ref

question_delivered_wait_effect_id
delivery_id

clinical_state_version
consultation_wait_effect_ref
checkpoint_ref

business_event_scope
validity
trace_refs[]
~~~

Eligibility:

~~~text
!= user answer
!= Business Resume Decision
!= Runtime Resume
~~~

U07 still owns those.

---

# 32. Stable U07 eligibility identity

Define:

~~~text
U07_RESUME_ELIGIBILITY_ID
=
QUESTION_DELIVERED_WAIT_EFFECT_ID
+ checkpoint compatibility identity
+ eligibility_contract_version
~~~

Exact wait replay:

~~~text
same delivered-wait effect
→ same or reattachable U07 eligibility identity
~~~

It must not create multiple independent resume windows for the same pending Question.

---

# 33. U07 ineligibility conditions

No U07 eligibility when:
- Question only SELECTED;
- delivery INDETERMINATE;
- delivery NOT_CONFIRMED;
- delivery FAILED;
- PendingQuestion absent;
- Consultation not WAITING_USER;
- different pending Question;
- Thread has no recoverable wait boundary;
- Question expired/superseded;
- Consultation terminal;
- parent effect provenance mismatch.

---

# 34. Crash window C1 — after Question selection / before intent

State:

~~~text
Question SELECTED
DeliveryIntent absent
~~~

Recovery:

~~~text
reload Question
revalidate currentness/Safety/endpoint
derive same delivery effect
create durable intent
~~~

No external send has occurred.

---

# 35. Crash window C2 — after intent / before send

State:

~~~text
DeliveryIntent READY
no STARTED attempt
~~~

Recovery:

~~~text
reattach same intent
same delivery_id
same idempotency key
revalidate selected Question currentness
then create first/next lawful attempt
~~~

---

# 36. Crash window C3 — after send / before receipt

This is the highest-risk duplication window.

State:

~~~text
attempt STARTED
send may have happened
receipt absent
~~~

Recovery:

~~~text
DO NOT blind resend

if status query available
→ reconcile

else if adapter idempotency guarantees exact resend safety
→ retry with same idempotency key

else
→ INDETERMINATE
→ no retry send
→ failure/reconciliation path
~~~

---

# 37. Crash window C4 — after receipt / before confirmation

State:

~~~text
durable receipt exists
confirmation decision absent
~~~

Recovery:

~~~text
replay confirmation resolver
using same intent + attempts + receipts
→ stable confirmation decision
~~~

No external resend is required merely because confirmation decision is missing.

---

# 38. Crash window C5 — after confirmation / before Clinical State child

State:

~~~text
confirmation CONFIRMED
delivered Clinical State child absent
~~~

Recovery:

~~~text
derive same parent/child effects
commit/reconcile same P01 child
no transport resend
~~~

---

# 39. Crash window C6 — after Clinical State child / before Consultation WAITING

State:

~~~text
Question DELIVERED
PendingQuestion current
Gap ASKED when applicable
Consultation not yet WAITING
~~~

Recovery:

~~~text
reattach/reconcile same Consultation child effect
no transport resend
no new Question selection
no U07 eligibility yet
~~~

---

# 40. Crash window C7 — after Consultation WAITING / before checkpoint

State:

~~~text
business state complete
checkpoint absent
~~~

Recovery:

~~~text
reconstruct checkpoint from:
Clinical State
+ Consultation wait provenance
+ delivery confirmation
+ parent effect
~~~

No clinical/business mutation replay required if already authoritative.

---

# 41. Crash window C8 — after checkpoint / before Thread AWAITING

State:

~~~text
business state complete
checkpoint durable
Thread not AWAITING_USER
~~~

Recovery:

~~~text
Runtime transitions/reconstructs AWAITING_USER
without resending
without recommitting business state
~~~

---

# 42. Crash window C9 — after Thread AWAITING / before U07 eligibility projection

Recovery:

~~~text
recompute/re-emit same U07 eligibility identity
from authoritative wait state
~~~

No new delivery.

---

# 43. Delivery ledger

RDP-04 freezes a durable non-clinical delivery ledger/read model:

~~~text
QuestionDeliveryLedgerRecord

delivery_id
delivery_effect_id
delivery_idempotency_key

intent_ref

attempt_refs[]
receipt_refs[]
confirmation_decision_ref?

delivery_lifecycle

parent_delivered_wait_effect_ref?
clinical_state_child_effect_ref?
consultation_wait_effect_ref?
checkpoint_ref?
u07_eligibility_ref?

failure_ref?

created_at
updated_at
~~~

Allowed delivery_lifecycle:

~~~text
INTENT_READY
ATTEMPT_IN_PROGRESS
RECONCILIATION_REQUIRED
CONFIRMED
BUSINESS_STATE_RECONCILIATION_PENDING
WAITING_ESTABLISHED
TERMINAL_NOT_CONFIRMED
TERMINAL_FAILURE
CANCELLED
~~~

This ledger:

~~~text
!= Clinical State
!= Consultation lifecycle
!= Runtime Thread truth
~~~

---

# 44. Delivery ledger replay semantics

Same delivery effect:

~~~text
same delivery_id
same canonical intent fingerprint
~~~

Different immutable intent payload under same delivery effect:

~~~text
U06_DELIVERY_INTENT_REPLAY_CONFLICT
→ fail closed
~~~

Attempt/receipt history is append-only evidence.

Confirmation decision is stable for an exact evidence set/policy version.

If later authoritative receipt evidence changes an earlier INDETERMINATE assessment:

~~~text
new confirmation evaluation version/effect
may supersede prior non-terminal assessment
~~~

but must preserve history.

---

# 45. Transport-side duplicate prevention

RDP-04 requires:

~~~text
at-most-one intended delivery effect
~~~

It cannot guarantee physical exactly-once delivery unless the adapter/provider supports it.

Therefore claim vocabulary must be precise:

Allowed:

~~~text
idempotent intent
stable delivery identity
duplicate-send prevention/reconciliation
at-most-once application-level send under proven adapter guarantees
~~~

Forbidden generic claim:

~~~text
exactly-once external delivery
~~~

unless verified for the selected adapter/provider semantics.

---

# 46. Safety preemption before send

Before first or retry send:

~~~text
revalidate current Safety / restricted permission
~~~

If current Safety becomes BLOCKED or delivery action denied:

~~~text
no new send attempt
~~~

If there is an ambiguous earlier send:

~~~text
still reconcile it
~~~

Safety preemption cannot erase evidence that a message may already have been sent.

---

# 47. Safety change after confirmed delivery

If Question delivery is already CONFIRMED:

~~~text
do not rewrite historical delivery as not delivered
~~~

Subsequent Safety may:
- prevent ordinary answer/resume continuation;
- route to safe handling;
- invalidate future actions.

But the delivery event remains historical truth.

---

# 48. Delivery expiry

Question may have delivery expiry distinct from answer expiry.

Before send:

~~~text
if delivery expiry reached
→ no send
→ selection becomes eligible for governed EXPIRED/SUPERSEDED handling
~~~

After confirmed delivery:

~~~text
answer-window expiry is U15/U07 lifecycle concern
not delivery cancellation
~~~

Do not use delivery expiry to erase a confirmed delivery.

---

# 49. External side-effect boundary

RDP-04 is the first U06 design that explicitly touches external side effects.

Only Transport Adapter may perform external send.

F3 / D04 / P01 / Scheduler may not.

Transport input is limited to:

~~~text
delivery_id
idempotency key
approved recipient/channel binding
frozen rendered content
approved transport metadata
~~~

Transport must not receive:
- private chain-of-thought;
- arbitrary internal trace;
- unrelated PHI;
- unapproved model output.

---

# 50. PROFILE-B synthetic adapter

RDP-04 chooses the current U06 non-production verification profile:

~~~text
SYNTHETIC_NONLIVE_DURABLE_DELIVERY
~~~

Required behavior:

~~~text
durable intent
stable delivery_id/idempotency
zero network/external I/O
deterministic synthetic receipt
deterministic synthetic confirmation
full crash/replay hooks
full delivered/wait child-effect reconciliation on synthetic fixtures only
~~~

The adapter must expose a hard evidence flag:

~~~text
external_side_effect = false
delivery_evidence_type = SYNTHETIC
~~~

No runtime path may relabel this as real delivery.

---

# 51. Synthetic delivery confirmation semantics

For PROFILE-B:

~~~text
SYNTHETIC receipt
→ may produce confirmation_status = CONFIRMED
only within synthetic verification scope
~~~

That means:

~~~text
structural contract path confirmed
~~~

not:

~~~text
patient received message
~~~

All resulting Question/WAITING fixture state must remain explicitly non-production/synthetic scoped.

---

# 52. Real delivery activation gate

PROFILE-A cannot activate until at least:

~~~text
Question clinical content approved
patient wording safety approved
delivery policy approved
recipient/consent rules approved

transport adapter selected
adapter idempotency/reconciliation verified
privacy/security review complete

delivery crash/replay tests pass
duplicate-send tests pass
cross-store reconciliation tests pass
U07 resume handoff tests pass

environment authorization granted
production authorization granted
~~~

---

# 53. Delivery failure handoff

RDP-04 defines typed failure evidence, not U14 final business outcome.

Logical:

~~~text
U06DeliveryFailureHandoff

handoff_id
consultation_id
question_id
delivery_id

failure_class
failure_stage

retry_exhausted
delivery_uncertain

source_intent_ref
attempt_refs[]
receipt_refs[]
confirmation_ref?

current_question_ref
current_clinical_state_version

trace_refs[]
~~~

Examples failure_class:

~~~text
ENDPOINT_UNAVAILABLE
PERMISSION_DENIED
ADAPTER_UNAVAILABLE
DEFINITIVE_TRANSPORT_FAILURE
AMBIGUOUS_TRANSPORT_OUTCOME
RETRY_EXHAUSTED
DELIVERY_CONFIRMATION_FAILURE
BUSINESS_STATE_RECONCILIATION_FAILURE
CHECKPOINT_FAILURE
~~~

This:

~~~text
!= U14 final decision
~~~

---

# 54. No alternate clinical route invention

If delivery fails:

~~~text
U06 may not decide:
"then go to U08"
"then skip question"
"then assume no answer"
~~~

Only governed no-progress/failure routing may determine next action.

Delivery failure is not:
- USER_UNKNOWN;
- UNMEASURED;
- NO_RESULT;
- D04 STOP;
- Clinical Readiness.

---

# 55. U15 relationship

If Consultation is cancelled/expired while delivery is pending:

~~~text
U15 owns cancellation/expiry outcome
~~~

RDP-04 must:
- stop new send attempts where possible;
- reconcile ambiguous sends;
- never set WAITING_USER after terminal Consultation state;
- preserve actual confirmed-delivery evidence if already sent.

---

# 56. U07 answer boundary

U07 may accept a USER_ANSWER only against a valid current wait boundary.

At minimum answer must bind:

~~~text
consultation_id
pending question / question_id
wait effect identity
business event identity
~~~

RDP-04 does not parse or accept the answer.

It only establishes the prerequisite wait context.

---

# 57. Frontend boundary

Frontend may display:
- selected/delivered Question;
- waiting state projection;
- retry-safe transport status as appropriate.

Frontend may not:
- mark Question DELIVERED;
- mark Consultation WAITING_USER;
- fabricate receipt;
- start U07 without authoritative eligibility;
- retry external transport independently.

---

# 58. Downstream sequence after successful wait establishment

Successful U06 MODE-2 terminal output:

~~~text
Question DELIVERED_TO_USER
+ PendingQuestion current
+ Consultation WAITING_USER
+ Runtime wait boundary established
+ U07ResumeEligibility
~~~

Then U06 stops.

U06 does not:
- wait synchronously for user response;
- process USER_ANSWER;
- activate U02 directly.

Future ordinary flow:

~~~text
USER_ANSWER
→ U07
→ Business Resume Decision
→ Runtime Resume
→ U02
~~~

---

# 59. U06 MODE-2 terminal outcome contract

Define one typed outward result:

~~~text
U06QuestionDeliveryOutcome

outcome_id
consultation_id
question_id

delivery_id?
question_delivery_effect_id?
question_delivered_wait_effect_id?

status
reason_code

delivery_confirmation_ref?
clinical_state_child_effect_ref?
consultation_wait_effect_ref?
checkpoint_ref?
u07_eligibility_ref?
failure_handoff_ref?

current_clinical_state_version
trace_ref
created_at
~~~

Allowed status:

~~~text
WAIT_ESTABLISHED
RECONCILIATION_REQUIRED
NOT_CONFIRMED
CANCELLED
FAILURE_REQUIRED
~~~

Semantics:

~~~text
WAIT_ESTABLISHED
→ all business wait effects authoritative
→ checkpoint compatible
→ Thread AWAITING_USER
→ carries U07 eligibility

RECONCILIATION_REQUIRED
→ one or more delivery/business/runtime stages incomplete or ambiguous
→ no U07 eligibility
→ no new ordinary Question send until reconciled

NOT_CONFIRMED
→ delivery terminally not confirmed
→ no delivered/wait state
→ no U07 eligibility

CANCELLED
→ delivery stopped by authoritative supersede/cancel/expiry before valid confirmation
→ no U07 eligibility

FAILURE_REQUIRED
→ typed failure handoff required
→ no alternate clinical route
→ no U07 eligibility
~~~

Only WAIT_ESTABLISHED is the normal successful terminal output of U06 MODE-2.

Outcome identity is stable for the exact U06 delivery terminal evaluation and is trace/routing evidence, not Clinical State.

---

# 60. Trace obligations

U06GovernedExecutionTrace must correlate:

~~~text
Question selection effect
→ DeliveryIntent
→ delivery effect / delivery_id
→ transport attempts
→ receipts
→ confirmation decision
→ QUESTION_DELIVERED_WAIT_EFFECT_ID
→ Clinical State child effect/commit
→ Consultation WAITING child effect
→ checkpoint
→ Thread wait transition
→ U07 eligibility
~~~

Failure path must correlate exact failure stage.

No raw recipient secret is required in parent trace.

---

# 61. Delivery evidence retention

Durable evidence required for replay/recovery:

~~~text
intent immutable identity
adapter/version
attempt history
receipt identity
confirmation decision
business child effects/results
checkpoint
U07 eligibility
~~~

Retention/security policy is implementation/governance dependent.

RDP-04 requires only that evidence remains sufficient to prevent duplicate send and reconstruct wait boundary.

---

# 62. RDP-03 compatibility

RDP-04 must consume and preserve:

~~~text
QUESTION_DELIVERED_WAIT_EFFECT_ID
QUESTION_DELIVERED_CLINICAL_STATE_EFFECT_ID
CONSULTATION_WAITING_EFFECT_ID
U06_DELIVERED_CLINICAL_STATE_IDEMPOTENCY_KEY
CONSULTATION_WAITING_IDEMPOTENCY_KEY
~~~

It may not redesign:
- Question state ownership;
- Gap ASKED timing;
- pending_question authority;
- Consultation business ownership;
- Thread non-clinical nature.

---

# 63. RDP-01/RDP-02/RDP-05 compatibility

RDP-04 preserves:
- exact admitted MODE-2 source;
- exact selected Question from RDP-02;
- exact dependency binding profile;
- exact Question/F3/D04 policy refs;
- real vs synthetic profile separation.

No delivery adapter may bypass:
- RDP-01 admission;
- RDP-02 selection;
- RDP-03 authoritative SELECTED commit.

---

# 64. RDP-06 verification obligations

RDP-06 must verify at least:

~~~text
entry
only authoritative current SELECTED Question may deliver

intent
intent durable before send
same effect -> same delivery_id
same effect + changed intent payload -> conflict

transport
attempt id separate from delivery id
stable idempotency key on retry
ACCEPTED != CONFIRMED by default
ambiguous send does not blind resend
non-idempotent/non-queryable ambiguous transport fails closed

confirmation
only CONFIRMED triggers delivered/wait effects
INDETERMINATE -> no WAITING
NOT_CONFIRMED -> no WAITING
FAILED -> no WAITING

business reconciliation
Clinical State delivered child first
authoritative read-back required
Consultation WAITING second
Thread AWAITING after both
U07 eligibility last

crash windows
C1 through C9 recover without duplicate external send

pending/consultation conflicts
different active pending Question -> conflict
different WAITING Question -> conflict

synthetic profile
zero external I/O
synthetic evidence typed
cannot claim real delivery

real profile
remains blocked absent all gates
~~~

---

# 65. Current implementation impact inventory

All remain NOT_AUTHORIZED.

## U06-RDP04-IMP-01 — Delivery Intent / Ledger storage

Need durable non-clinical persistence for:
- intent;
- attempts;
- receipts;
- confirmation;
- reconciliation lifecycle.

## U06-RDP04-IMP-02 — Transport Adapter SPI

Need explicit adapter contract exposing:
- idempotency support;
- status query support;
- receipt semantics;
- external-side-effect flag.

## U06-RDP04-IMP-03 — Synthetic non-live adapter

Need deterministic zero-network adapter for PROFILE-B.

## U06-RDP04-IMP-04 — Delivery Confirmation Resolver

Need deterministic confirmation policy implementation.

## U06-RDP04-IMP-05 — Consultation WAITING transition persistence

Need RDP-03-required effect/idempotency/provenance storage for Consultation lifecycle.

## U06-RDP04-IMP-06 — Runtime wait/checkpoint surface

Current ClinicalRunRecord only exposes OPEN/CLOSED and current Python checkpoint is test-only minimal.

Need a reviewed runtime wait/checkpoint implementation for:
- AWAITING_USER;
- Question/wait binding;
- recovery.

## U06-RDP04-IMP-07 — U07 eligibility projection

Need stable typed eligibility output; does not implement U07 itself.

## U06-RDP04-IMP-08 — Safety/permission pre-send check

Need a current delivery-action permission seam.

## U06-RDP04-IMP-09 — Failure handoff

Need typed U06DeliveryFailureHandoff to governed failure path.

## U06-RDP04-IMP-10 — Delivery authority / uniqueness guard

Need durable per-QUESTION_SELECTION_EFFECT_ID authority preventing multiple active/pending delivery effects.

## U06-RDP04-IMP-11 — Confirmation evaluation history

Need immutable/versioned confirmation evaluation storage supporting:
- exact evidence-set replay;
- INDETERMINATE supersession by later evidence;
- terminal CONFIRMED protection;
- evidence-conflict detection.

## U06-RDP04-IMP-12 — U06 terminal delivery outcome

Need typed U06QuestionDeliveryOutcome for Scheduler/governance handoff.

---

# 66. Design acceptance scenarios

~~~text
RDP04-AC-01
Question SELECTED + current + allowed
→ durable intent
→ no send before intent

RDP04-AC-02
crash after SELECTED before intent
→ same delivery effect/id
→ one intent

RDP04-AC-03
intent READY, no attempt
→ same intent reused
→ lawful send may start

RDP04-AC-04
send timeout, no receipt, adapter supports status query
→ reconcile before retry

RDP04-AC-05
send timeout, no receipt, adapter supports neither idempotency nor query
→ INDETERMINATE
→ no blind resend

RDP04-AC-06
receipt ACCEPTED only
→ not automatically DELIVERED

RDP04-AC-07
confirmation CONFIRMED
→ derive same parent delivered-wait effect

RDP04-AC-08
confirmation NOT_CONFIRMED
→ no delivered/wait business effects

RDP04-AC-09
Clinical State delivered child commits
Consultation child fails transiently
→ delivery truth preserved
→ reconciliation pending
→ no U07 eligibility

RDP04-AC-10
both business child effects authoritative
checkpoint absent
→ reconstruct checkpoint
→ no resend

RDP04-AC-11
checkpoint authoritative runtime metadata
Thread transition fails
→ business WAITING remains
→ reconstruct AWAITING
→ no resend

RDP04-AC-12
different current pending Question
→ delivery business child conflict
→ no overwrite

RDP04-AC-13
Consultation WAITING for different Question
→ conflict
→ no overwrite

RDP04-AC-14
Question superseded before send
→ no send

RDP04-AC-15
Question superseded after ambiguous send
→ reconcile send outcome
→ do not erase possible delivery

RDP04-AC-16
PROFILE-B synthetic adapter
→ zero external I/O
→ synthetic receipt/confirmation typed
→ structural wait path may be verified on synthetic fixture only

RDP04-AC-17
PROFILE-A requested with current repository state
→ blocked

RDP04-AC-18
U07 eligibility
→ only after delivered Question + pending Question + Consultation WAITING + recoverable Thread wait

RDP04-AC-19
delivery failure
→ no alternate clinical route invented

RDP04-AC-20
same delivery effect replay after fully established wait
→ reattach all existing evidence/effects
→ zero transport resend

RDP04-AC-21
same Question selection has ACTIVE_PENDING delivery effect
+ endpoint/policy changes
→ no second active effect until governed replacement/reconciliation

RDP04-AC-22
ambiguous old delivery attempt exists
→ new delivery effect for same selection prohibited

RDP04-AC-23
later receipt resolves prior INDETERMINATE
→ new confirmation evaluation supersedes prior non-terminal evaluation
→ history preserved

RDP04-AC-24
prior confirmation = CONFIRMED
+ later conflicting evidence
→ evidence conflict/failure
→ historical CONFIRMED not rewritten

RDP04-AC-25
business WAITING established but Thread not AWAITING_USER
→ WAIT_RUNTIME_RECONCILIATION_REQUIRED
→ no U07 eligibility

RDP04-AC-26
U06QuestionDeliveryOutcome WAIT_ESTABLISHED
→ exactly one U07 eligibility

RDP04-AC-27
any non-WAIT_ESTABLISHED terminal/outstanding outcome
→ no U07 eligibility
~~~

---

# 67. Readiness blocker disposition

If Independent Design Review passes:

~~~text
BF-U06-RG-04
= CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE
~~~

It remains pending aggregate closure because:
- RDP-06 verification design is not yet complete;
- RDP-03/RDP-04 cross-store impacts need aggregate compatibility review;
- physical delivery/runtime infrastructure is not implementation-authorized;
- real patient delivery remains blocked.

---

# 68. Authorization boundary

This design does not authorize:

~~~text
real Question send
SMS/email/push/voice/websocket delivery
patient endpoint access
real delivery policy activation

U06 runtime implementation
U07 runtime implementation
P01/P05 modification
Consultation schema modification
Runtime Thread/checkpoint modification

merge
production
real-patient traffic
~~~

PROFILE-B synthetic structural implementation also requires later explicit implementation authorization.

---

# 69. Independent Design Review Remediation

Initial Independent Design Review:

~~~text
PR #234
review_id = 5287763500
verdict = REVISE_REQUIRED
reviewed_head = f8df7ca17dcfe5a6f61d5042f9417e3c4d0c5c8f
~~~

Findings:

~~~text
BF-U06-RDP04-IR-01
= ONE_ACTIVE_DELIVERY_EFFECT_PER_SELECTED_QUESTION_UNDERDEFINED

BF-U06-RDP04-IR-02
= DELIVERY_CONFIRMATION_IDENTITY_AND_EVIDENCE_EVOLUTION_UNDERDEFINED

BF-U06-RDP04-IR-03
= U07_ELIGIBILITY_RUNTIME_WAIT_PRECONDITION_AMBIGUOUS

BF-U06-RDP04-IR-04
= U06_MODE2_TERMINAL_OUTCOME_CONTRACT_MISSING
~~~

Remediation applied:

1. froze QuestionDeliveryAuthority and at-most-one ACTIVE/PENDING delivery effect per Question selection;

2. froze DELIVERY_CONFIRMATION_EVALUATION_ID, canonical confirmation fingerprint, immutable evidence evolution, non-terminal INDETERMINATE supersession, and terminal CONFIRMED protection;

3. removed recoverably-equivalent shortcut from U07 eligibility and added WAIT_RUNTIME_RECONCILIATION_REQUIRED;

4. added typed U06QuestionDeliveryOutcome with WAIT_ESTABLISHED / RECONCILIATION_REQUIRED / NOT_CONFIRMED / CANCELLED / FAILURE_REQUIRED.

Current:

~~~text
BF-U06-RDP04-IR-01
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP04-IR-02
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP04-IR-03
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP04-IR-04
= REMEDIATED / RE-REVIEW_PENDING

U06-RDP-04
= REVISED / READY_FOR_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW

BF-U06-RG-04
= OPEN / DESIGN_RE_REVIEW_PENDING

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
~~~

# 70. Revised verdict

~~~text
U06-RDP-04
= REVISED / READY_FOR_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW
~~~

No real patient delivery, external transport activation, U06/U07 implementation, merge, production, or real-patient authorization is granted.
