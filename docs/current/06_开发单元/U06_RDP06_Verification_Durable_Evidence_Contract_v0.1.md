# U06-RDP-06 Verification / Durable Evidence Contract v0.1

> Unit: **U06 — F3 Gap / Question Selection / Delivery / WAITING_USER Entry**  
> Readiness blocker: **BF-U06-RG-06**  
> Parent design: **U06-RDP-04 Delivery / Downstream / Side-effect Boundary**  
> Parent head: **332f4e1d341051232325fdbd60192f3ade6cbffa**  
> Runtime repository basis: **main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8**  
> Scope: **VERIFICATION / DURABLE EVIDENCE DESIGN ONLY**  
> Status: **DRAFT / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document defines how future U06 implementation must be verified. It does not authorize implementation, real C03/D04 activation, real patient delivery, merge, production, or real-patient traffic.

---

# 1. Purpose

U06 RDP-01..05 already define:

~~~text
RDP-01
Consumer Inbound / Admission

RDP-02
F3 Owner / D04 Question Policy

RDP-03
State Ownership / K09-P01 Mutation / Trace

RDP-04
Delivery / Downstream / Side-effect

RDP-05
Capability / Dependency / Applicability
~~~

RDP-06 answers only:

> 当 U06 未来被授权实现后，必须拿出什么独立、可执行、可复核、可长期保存的证据，才能证明实现忠实于 Frozen Contract，而不是测试代码自己为实现行为背书。

必须证明：

~~~text
all three U06 modes
all owner/policy boundaries
all state-mutation boundaries
all delivery/wait boundaries
all dependency applicability rules
all replay/conflict/crash guarantees
all current non-production hard boundaries
all relevant regressions
~~~

---

# 2. Verification principle

Must preserve:

~~~text
tests PASS
!= implementation correct

implementation correct
!= clinical capability validated

clinical capability validated
!= patient-facing delivery approved

patient-facing delivery approved
!= production authorized
~~~

Verification chain:

~~~text
Frozen Contract
→ independent machine-readable expectation
→ executable implementation surface
→ observed evidence
→ expected-vs-observed comparison
→ durable evidence artifact
→ independent evidence review
~~~

Forbidden:

~~~text
implementation output
→ copied into expected result
→ PASS
~~~

---

# 3. Frozen authority package

Every required verification case must carry:

~~~text
expected_authority_refs[]
~~~

Authority package:

~~~text
U06 Unit Spec
status head = ae332b4a8fc5e179e352a68e8c4762117340c6c5

U06-RDP-01
status head = a7fc37a17f488670915faf517dc7ea7f4cd18680

U06-RDP-05
status head = 0e9b7be67c349cf4056cc144d4039efe17017241

U06-RDP-02
status head = 23fd6764439f55c053691daa500fede5030411ec

U06-RDP-03
status head = 09bb7f0244300fdfc750065823361ef7846802f0

U06-RDP-04
status head = 332f4e1d341051232325fdbd60192f3ade6cbffa
~~~

Phase 5/6/8/9 authority may be used only where an RDP explicitly depends on those frozen semantics.

Observed runtime behavior is never authority for expected behavior.

---

# 4. CONTRACT_EXPECTATION_GAP stop rule

If a legally constructible U06 verification scenario:

~~~text
is within the authorized verification profile
+ has valid fixture authority
+ reaches a contract decision point
+ Frozen Contract cannot uniquely derive the expected structural result
~~~

the verifier must emit:

~~~text
CONTRACT_EXPECTATION_GAP
~~~

Then:

~~~text
verification verdict = FAIL_CLOSED
implementation closure = STOP
do not invent expected result
do not silently skip
do not map to runtime failure/business status
~~~

CONTRACT_EXPECTATION_GAP:

~~~text
!= C03 NO_RESULT
!= D04 STOP
!= F3 NOT_DECIDABLE
!= delivery INDETERMINATE
!= test skip
~~~

Normal acceptance requires:

~~~text
contract_expectation_gap_count = 0
~~~

---

# 5. Verification layers

## L1 Contract / schema

Verify:
- enums;
- required and conditional fields;
- stable identity inputs;
- provenance refs;
- execution-profile markers;
- invalid value rejection.

## L2 Deterministic owner/policy behavior

Verify:
- RDP-01 admission;
- F3 owner interpretation;
- D04 STOP/CONTINUE/FAILED;
- Question selection;
- MODE-3 revalidation.

## L3 State / effect / ledger integration

Verify:
- F3 canonical effect;
- Question selection effect;
- P01 proposal/commit;
- replay;
- conflict;
- read-back;
- U06 parent trace.

## L4 Delivery / cross-store orchestration

Verify:
- intent-before-send;
- delivery identity/idempotency;
- receipt/confirmation;
- delivered Clinical State child;
- Consultation WAITING child;
- checkpoint;
- Thread AWAITING_USER;
- U07 eligibility.

## L5 Crash / concurrency / recovery

Verify:
- replay-first recovery;
- C1-C9 crash windows;
- one active delivery effect;
- no blind duplicate resend;
- concurrent writer conflicts.

## L6 Structural authorization guards

Verify:
- PROFILE-A blocked under current repository state;
- PROFILE-B synthetic-only;
- no real PHI;
- zero real external delivery;
- zero unauthorized model/tool/knowledge calls;
- no live U07/U14 execution.

## L7 Regression

Verify:
- Foundation;
- U01;
- U02;
- U03;
- U04;
- U05;
- U06-focused suites.

---

# 6. Verification authorization profile

Future authoritative run must commit a machine-readable:

~~~text
U06_VERIFICATION_AUTH_PROFILE_V0_1
~~~

Minimum fields:

~~~text
environment = NON_PRODUCTION

execution_profile = SYNTHETIC_STRUCTURAL_NONPROD
synthetic_patient_data_only = true
real_patient_traffic = false

profile_a_real_c03_enabled = false
profile_a_real_delivery_enabled = false

external_delivery_side_effects = false
external_model_calls = false
external_tool_calls = false
external_knowledge_calls = false

production_state_store = false
production_consultation_store = false

live_u07_execution = false
live_u14_final_routing = false

synthetic_delivery_scope_required = true

authorized_shared_runtime_change_refs[]
authorized_shared_runtime_paths[]
unreviewed_shared_runtime_change_count = 0
~~~

Any hard boundary violation:

~~~text
verification verdict = FAIL
~~~

Shared-runtime scope rule:

~~~text
reviewed aggregate/amendment package
→ may authorize exact shared-runtime changes

change outside authorized_shared_runtime_change_refs / paths
→ unreviewed_shared_runtime_change_count > 0
→ verification FAIL
~~~

The count must not be self-reported by runtime/implementation code.

Future authoritative verification must build:

~~~text
U06_AUTHORIZED_SHARED_RUNTIME_CHANGE_MANIFEST

manifest_id

implementation_base_sha
implementation_target_sha

authorized_amendment_refs[]
authorized_review_refs[]

authorized_exact_files[]
authorized_path_patterns[]

observed_changed_files[]
unexpected_changed_files[]

observed_git_diff_digest
manifest_digest
verdict
~~~

The verifier must compute:

~~~text
observed_changed_files
=
git diff --name-only implementation_base_sha..implementation_target_sha
~~~

or an equivalent repository-native exact diff bound to those SHAs.

Then:

~~~text
unexpected_changed_files
=
observed shared-runtime files
- files covered by reviewed authorized_exact_files / authorized_path_patterns
~~~

and:

~~~text
unreviewed_shared_runtime_change_count
=
count(unexpected_changed_files)
~~~

Acceptance requires:

~~~text
manifest verdict = PASS
unexpected_changed_files = []
unreviewed_shared_runtime_change_count = 0
observed_git_diff_digest = durable evidence value
~~~

The manifest must itself be included in the evidence bundle and checksum coverage.

RDP-06 does not itself authorize any shared-runtime change.

---

# 7. Current verification scope

Current design-ready verification target is:

~~~text
PROFILE-B
SYNTHETIC_STRUCTURAL_NONPROD
+
SYNTHETIC_NONLIVE_DURABLE_DELIVERY
~~~

It may structurally exercise:

~~~text
MODE-1
MODE-2
MODE-3

C03 synthetic result branches
D04 synthetic policy branches
F3 owner mechanics
K09/P01 mechanics
Question lifecycle
synthetic delivery
cross-store wait choreography
replay/crash/recovery
~~~

It may not claim:

~~~text
clinical question correctness
clinical decision impact validity
patient wording safety
medical stopping correctness
real C03 Quality Gate PASS
real D04 policy approval
real patient delivery success
~~~

PROFILE-A is verified only as a blocked/guarded profile under current repository state.

---

# 8. Test data boundary

All retained U06 fixtures must be:

~~~text
synthetic
non-identifiable
non-patient
contract-driven
predeclared
~~~

Forbidden:
- real PHI;
- production recipient endpoints;
- realistic secrets/tokens;
- developer-created hidden medical policy;
- expected diagnoses;
- medically meaningful thresholds used as oracle authority.

Synthetic tokens such as:

~~~text
GAP-A
DECISION-IMPACT-X
QUESTION-CANDIDATE-1
POLICY-ORDER-A
~~~

are allowed only as structural labels.

---

# 9. Canonical case identities

Required governed cases:

~~~text
U06-EV-001 .. U06-EV-106
~~~

Required harness self-tests:

~~~text
U06-HG-001 .. U06-HG-003
~~~

Required verification gates:

~~~text
U06-VG-001 .. U06-VG-010
~~~

Case identity semantics are immutable after freeze.

Future additions use new IDs.

Existing IDs may not be repurposed.

---

# 10. Required case matrix — RDP-01 Admission / Currentness

~~~text
U06-EV-001
MODE-1 valid A1 pre-readiness eligibility + current ALLOW
→ ADMITTED

U06-EV-002
MODE-1 direct caller without frozen source authority
→ REJECTED

U06-EV-003
MODE-1 stale Gate
→ REJECTED

U06-EV-004
MODE-1 RESTRICTED but action permission denied
→ REJECTED

U06-EV-005
MODE-1 RESTRICTED permission unavailable
→ REJECTED / failure-governance evidence

U06-EV-006
MODE-2 U05 TO_U06_QUESTION_PATH + current refs
→ ADMITTED

U06-EV-007
MODE-2 lawful F1 clarification source with controlled-amendment activation absent
→ business-legal contract present
→ executable admission blocked

U06-EV-008
MODE-2 wrong route consequence
→ REJECTED

U06-EV-009
MODE-3 lawful post-F3 revalidation source
→ ADMITTED

U06-EV-010
MODE-3 direct arbitrary invocation
→ REJECTED

U06-EV-011
wrong-mode payload/source combination
→ REJECTED

U06-EV-012
consultation/CDP mismatch
→ REJECTED

U06-EV-013
source Clinical State Version drift before admission
→ REJECTED / re-admission required

U06-EV-014
inactive/expired dependency binding required by admitted profile
→ REJECTED or dependency-blocked before invocation

U06-EV-015
exact admission replay
→ same admission identity / REATTACHED

U06-EV-016
same admission identity + changed canonical payload
→ REPLAY_CONFLICT

U06-EV-017
same version but source authority superseded
→ old admission not current

U06-EV-018
PROFILE-B admission without valid synthetic execution/scope marker
→ REJECTED
~~~

---

# 11. Required case matrix — RDP-02 F3 Owner / D04 / Question Policy

~~~text
U06-EV-019
MODE-1 C03 SUCCESS + valid synthetic Gap basis
→ F3 GAP_BASIS_ESTABLISHED
→ no Question

U06-EV-020
MODE-1 explicit governed no-online-gap coverage fixture
→ NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED

U06-EV-021
MODE-1 empty candidate list without positive coverage
→ NOT_DECIDABLE

U06-EV-022
MODE-1 C03 NO_RESULT
→ NOT_DECIDABLE
→ no negative F3 truth

U06-EV-023
MODE-1 C03 INSUFFICIENT_INFORMATION
→ NOT_DECIDABLE

U06-EV-024
MODE-1 C03 dependency failure/timeout/invalid output
→ FAILED
→ no canonical F3 truth

U06-EV-025
MODE-2 F1 minimal clarification
→ candidate remains bound to exact F1 requirement
→ no general F3 interrogation

U06-EV-026
MODE-2 F3 current askable Gap
→ lawful candidate set

U06-EV-027
MODE-2 fresh invocation requirement
→ MODE-1 candidate material not reused

U06-EV-028
current equivalent Question already SELECTED
→ duplicate suppressed

U06-EV-029
current equivalent Question DELIVERED
→ duplicate suppressed

U06-EV-030
current equivalent ANSWER_RECEIVED and answer still current
→ candidate suppressed

U06-EV-031
USER_UNKNOWN + unchanged semantic question
→ exact re-ask suppressed

U06-EV-032
UNMEASURED requiring non-online evidence
→ no online selection

U06-EV-033
D04 CONTINUE + one lawful candidate
→ F3QuestionSelectionDecision SELECTED

U06-EV-034
D04 CONTINUE + two candidates + approved deterministic ordering
→ exactly one selected candidate

U06-EV-035
D04 CONTINUE + unresolved ordering tie
→ selection FAILED / ambiguous
→ no Question

U06-EV-036
D04 STOP
→ no Question
→ no Clinical Readiness invented

U06-EV-037
D04 FAILED
→ no Question
→ not converted to STOP

U06-EV-038
C03 NO_RESULT in MODE-2
→ NO_SELECTION
→ no inferred D04 status

U06-EV-039
C03 INSUFFICIENT_INFORMATION in MODE-2
→ NO_SELECTION or governed FAILED
→ never automatic ask

U06-EV-040
F1 no-progress
→ target U01/F1
→ no U05/D03 readiness decision

U06-EV-041
F3 no-progress
→ READINESS_REEVALUATION_REQUIRED
→ target U05/D03
~~~

---

# 12. Required case matrix — RDP-03 Mutation / Replay / Trace

~~~text
U06-EV-042
MODE-1 GAP_BASIS_ESTABLISHED
→ one F3 canonical effect
→ one proposal
→ one P01 commit
→ read-back match

U06-EV-043
MODE-1 explicit no-online-gap assessment
→ explicit F3 assessment state committed
→ absence not used as no-gap truth

U06-EV-044
MODE-1 NOT_DECIDABLE
→ zero proposal / zero commit

U06-EV-045
MODE-1 FAILED
→ zero proposal / zero commit

U06-EV-046
exact F3 effect replay after commit
→ reattach
→ zero second version advance

U06-EV-047
same F3 effect id + changed canonical payload
→ replay conflict

U06-EV-048
version advances before first F3 commit
→ CONFLICT
→ no base-version-only rewrite

U06-EV-049
unauthorized F3/Gap field path
→ P01 REJECTED

U06-EV-050
logical unit U06 maps to approved lower-case physical producer
→ no literal invalid producer

U06-EV-051
StatePatch capability identity
!= C03 dependency binding
→ exact provenance preserved

U06-EV-052
MODE-2 SELECTED owner decision
→ one Question SELECTED effect/proposal/commit

U06-EV-053
selection commit
→ Gap remains not ASKED
→ pending_question absent
→ Consultation not WAITING due to selection alone

U06-EV-054
exact selection replay
→ same question_id/effect/proposal
→ no duplicate Question

U06-EV-055
different active pending Question exists before delivered child
→ conflict
→ no overwrite

U06-EV-056
MODE-3 REVALIDATED_CURRENT
→ zero StatePatch
→ zero P01 commit
→ zero version delta

U06-EV-057
MODE-3 REASSESSMENT_REQUIRED
→ zero mutation
→ route toward fresh MODE-1 only

U06-EV-058
MODE-3 FAILED
→ zero mutation

U06-EV-059
exact U06 execution replay
→ same U06_TRACE_ID
→ retry evidence is child evidence

U06-EV-060
NOT_APPLICABLE dependency family
→ no fake release ref in parent trace

U06-EV-061
P01 COMMITTED but authoritative read-back mismatch
→ no downstream progression
→ reconciliation failure
~~~

---

# 13. Required case matrix — RDP-04 Delivery / WAIT / Side-effect

~~~text
U06-EV-062
authoritative current Question SELECTED
→ durable intent created before send

U06-EV-063
Question stale/superseded before intent
→ no intent / no send

U06-EV-064
same delivery effect replay
→ same delivery_id / idempotency key

U06-EV-065
same delivery effect + changed immutable intent payload
→ replay conflict

U06-EV-066
one Question selection already ACTIVE_PENDING
→ second active delivery effect prohibited

U06-EV-067
pre-send endpoint change + valid REBIND_ALLOWED
→ old intent cancelled
→ one new effect

U06-EV-068
endpoint change after ambiguous attempt
→ rebinding denied

U06-EV-069
send starts only after durable intent
→ ordering proven

U06-EV-070
transport attempt id changes on retry
but delivery_id/idempotency remain stable

U06-EV-071
receipt ACCEPTED only
→ not automatically CONFIRMED

U06-EV-072
receipt DELIVERED under approved synthetic confirmation policy
→ CONFIRMED

U06-EV-073
ambiguous send + status query available
→ query/reconcile before retry

U06-EV-074
ambiguous send + idempotent resend capability
→ same-key retry allowed

U06-EV-075
ambiguous send + no idempotency and no query
→ INDETERMINATE
→ zero blind resend

U06-EV-076
definitive transient non-delivery + retry policy allows
→ RETRYABLE_NOT_CONFIRMED

U06-EV-077
retry exhausted
→ NOT_CONFIRMED_TERMINAL
→ no further send

U06-EV-078
later authoritative evidence supersedes prior INDETERMINATE
→ new confirmation evaluation
→ history preserved

U06-EV-079
prior CONFIRMED + later conflicting evidence
→ evidence conflict
→ historical CONFIRMED not rewritten

U06-EV-080
CONFIRMED
→ one QUESTION_DELIVERED_WAIT_EFFECT_ID

U06-EV-081
Clinical State delivered child
→ Question DELIVERED
→ Gap ASKED when F3 need
→ pending_question current

U06-EV-082
F1 clarification delivered child
→ no fake F3 Gap mutation

U06-EV-083
Clinical State child succeeds, Consultation child pending/fails transiently
→ RECONCILIATION_REQUIRED
→ no U07 eligibility

U06-EV-084
Consultation WAITING for exact same parent effect replay
→ reattach

U06-EV-085
Consultation WAITING for different Question/effect
→ conflict / no overwrite

U06-EV-086
business wait complete but checkpoint missing
→ reconstruct checkpoint
→ no resend

U06-EV-087
business wait complete + checkpoint durable + Thread transition initially fails
→ reconstruct AWAITING_USER
→ no resend

U06-EV-088
Thread not AWAITING_USER
→ WAIT_RUNTIME_RECONCILIATION_REQUIRED
→ no U07 eligibility

U06-EV-089
all wait prerequisites satisfied
→ U06 outcome WAIT_ESTABLISHED
→ exactly one U07 eligibility

U06-EV-090
exact WAIT_ESTABLISHED replay
→ same outcome / same U07 eligibility
→ zero new resume window

U06-EV-091
delivery failure
→ no alternate U08/U10/U11 clinical route invention

U06-EV-092
Safety becomes blocked before new send
→ no new attempt
→ ambiguous old attempt still reconciled

U06-EV-093
Question expires before send
→ no send

U06-EV-094
PROFILE-B valid scope
→ zero external network I/O
→ synthetic receipt/confirmation typed

U06-EV-095
PROFILE-B missing/mismatched scope authorization
→ fail closed

U06-EV-096
PROFILE-B targets live/production store or real recipient
→ fail closed
~~~

---

# 14. Required case matrix — RDP-05 Dependency / Profile Applicability

~~~text
U06-EV-097
PROFILE-A current repository state
→ BLOCKED
→ no real C03/D04 invocation
→ no real delivery

U06-EV-098
PROFILE-B synthetic binding
→ typed SYNTHETIC_VERIFICATION_BINDING
→ never accepted as real P06 ACTIVE C03 binding

U06-EV-099
required dependency/release missing
→ fail closed
→ no fake ref
→ no invocation

U06-EV-100
MODE-3 historical compatibility path
→ no new C03 invocation by default
→ historical applicable refs retained
→ semantic compatibility required for REVALIDATED_CURRENT

U06-EV-101
MODE-1 canonical F3 commit + authoritative read-back
→ post-F3 Safety barrier evaluation occurs after commit/read-back
→ barrier binds exact F3 effect/commit/current state
→ no Question / no delivery / no WAITING directly from MODE-1

U06-EV-102
post-F3 current Safety = BLOCKED
→ ordinary continuation prohibited
→ no MODE-2 Question path
→ no WAITING

U06-EV-103
post-F3 current Safety = UNAVAILABLE
→ ordinary continuation prohibited
→ typed failure-governance consequence
→ no Question / delivery / WAITING

U06-EV-104
MODE-3 exact same F3_REVALIDATION_ID + same canonical evidence
→ exact replay / reattach
→ C03 = 0
→ D04 = 0
→ StatePatch = 0

U06-EV-105
MODE-3 same revalidation identity + changed canonical evidence
→ replay conflict
→ no projection / mutation

U06-EV-106
MODE-3 decision formed but target state/binding/policy currentness changes before publish
→ STALE_BEFORE_PUBLISH
→ no current projection published
→ C03 = 0
→ D04 = 0
→ P01 commit = 0
~~~

---

# 15. Crash-window matrix

RDP-04 C1-C9 are mandatory named verification subcases:

~~~text
U06-CW-01
after Question SELECTED / before intent

U06-CW-02
after intent / before send

U06-CW-03
after send / before receipt

U06-CW-04
after receipt / before confirmation

U06-CW-05
after confirmation / before Clinical State child

U06-CW-06
after Clinical State child / before Consultation WAITING

U06-CW-07
after Consultation WAITING / before checkpoint

U06-CW-08
after checkpoint / before Thread AWAITING_USER

U06-CW-09
after Thread AWAITING_USER / before U07 eligibility
~~~

Each must prove:
- stable identities;
- no duplicate business effect;
- no unauthorized second transport send;
- correct reconciliation stage;
- exact final state or typed non-terminal outcome.

CW cases are additional structured subcases, not replacements for EV-062..096.

---

# 16. Harness self-tests

## U06-HG-001 — CONTRACT_EXPECTATION_GAP detector

Inject a structurally legal fixture for which no frozen contract expectation is available.

Expected:

~~~text
detector = FIRED
verdict = FAIL_CLOSED
no expected business result invented
~~~

## U06-HG-002 — external-side-effect detector

Inject a spy transport/model/tool/knowledge adapter that records an unauthorized external call.

Expected:

~~~text
hard-boundary detector = FIRED
verification FAIL
~~~

## U06-HG-003 — synthetic-scope escape detector

Attempt to run PROFILE-B against a non-synthetic consultation/store.

Expected:

~~~text
scope guard = FIRED
zero business mutation
zero external effect
verification path = FAIL_CLOSED
~~~

HG cases are harness verification, not Clinical Runtime outcomes.

---

# 17. Verification gates

~~~text
U06-VG-001
exact implementation-target / verifier / contract-package SHA binding
+ independently reviewed oracle digest binding
+ authorized shared-runtime change allowlist enforcement

U06-VG-002
all U06-EV-001..106 present and PASS

U06-VG-003
all U06-CW-01..09 present and PASS

U06-VG-004
HG-001..003 pass

U06-VG-005
zero real PHI / production recipient evidence

U06-VG-006
zero real external delivery / model / tool / knowledge side effects
+ isolated verification environment / denied-by-default egress proof

U06-VG-007
PROFILE-A remains blocked under current repository package

U06-VG-008
no Clinical State write bypasses K09/P01
and no Trace/Checkpoint becomes Clinical Truth

U06-VG-009
no live U07 execution or U14 final decision in current slice

U06-VG-010
Foundation + U01-U05 + U06 focused regression gate
~~~

All ten gates are mandatory for current RDP-06 acceptance.

---

# 18. Completeness rule

Current required set:

~~~text
106 EV cases
9 crash-window subcases
3 harness self-tests
10 verification gates
~~~

No required identity may be silently skipped.

If implementation surface is absent:

~~~text
NOT_IMPLEMENTED
→ verification FAIL
~~~

not:

~~~text
SKIP
→ PASS
~~~

Future controlled amendment may mark a case NOT_APPLICABLE only with explicit contract authority and independent review.

---

# 19. Independent machine-readable expectation oracle

Future implementation must commit:

~~~text
u06-verification-expectations.json
schema = U06_VERIFICATION_EXPECTATIONS_V0_1
~~~

Each EV requires at least:

~~~text
case_id
mode
fixture_id
fixture_digest

expected_boundary
expected_status
expected_reason_code?

expected_admission_status?
expected_c03_invocation_count?
expected_d04_invocation_count?

expected_f3_owner_status?
expected_question_selection_status?

expected_state_commit_count?
expected_state_version_delta?

expected_delivery_intent_count?
expected_transport_attempt_count?
expected_external_transport_count?
expected_confirmation_status?

expected_consultation_wait_count?
expected_checkpoint_count?
expected_thread_awaiting_count?
expected_u07_eligibility_count?

expected_failure_handoff_count?

expected_identity_equalities[]
expected_provenance_equalities[]
expected_effect_counts{}

expected_authority_refs[]
authority_semantic_claims[]

contract_manifest_digest
~~~

Oracle must be:
- static;
- contract-derived;
- independently reviewed;
- hashed into evidence.

Production code must not generate it.

## 19.1 Independent Oracle Review Gate

Future authoritative verification requires:

~~~text
U06_ORACLE_REVIEW_GATE_V0_1

oracle_review_id
reviewed_oracle_digest
reviewed_contract_manifest_digest

reviewer_role
independence_marker

verdict
reviewed_at
finding_refs[]
~~~

Allowed verdict:

~~~text
PASS
REVISE_REQUIRED
INVALID
~~~

Authoritative CI must verify:

~~~text
oracle_digest_used_by_run
= reviewed_oracle_digest

contract_manifest_digest_used_by_run
= reviewed_contract_manifest_digest

oracle review verdict
= PASS
~~~

Otherwise:

~~~text
verification = INVALID_EVIDENCE
~~~

The implementation/runtime under test must not be the sole authority approving the oracle.

---

# 20. Fixture manifest

Future verification must commit:

~~~text
u06-verification-fixtures.json
schema = U06_VERIFICATION_FIXTURES_V0_1
~~~

Every fixture includes:

~~~text
fixture_id
fixture_semantic_id
synthetic = true
patient_data_classification = SYNTHETIC_NON_PATIENT

mode
execution_profile

admission source refs
synthetic binding refs
synthetic owner/policy refs

synthetic C03 fixture result when applicable
synthetic D04 fixture result when applicable

synthetic delivery-scope authorization when applicable
synthetic transport behavior when applicable

fixture_digest
review_status
~~~

No fixture may embed an unreviewed medical expectation.

## 20.1 Independent Fixture Review Gate

Future authoritative verification requires:

~~~text
U06_FIXTURE_REVIEW_GATE_V0_1

fixture_review_id
reviewed_fixture_manifest_digest
reviewed_contract_manifest_digest

reviewer_role
independence_marker

verdict
reviewed_at
finding_refs[]
~~~

Allowed verdict:

~~~text
PASS
REVISE_REQUIRED
INVALID
~~~

The independent fixture review must verify at least:

~~~text
fixtures contain only branch stimuli / structural inputs
fixtures do not copy observed SUT outputs into expected fields
fixtures do not hide real medical policy or patient-facing wording claims
fixtures are synthetic / non-patient / non-identifiable
fixtures contain no real recipient endpoint / production secret
fixtures cannot enable external side effects
fixture semantic identity matches the contract authority it claims to exercise
~~~

Authoritative CI must verify:

~~~text
fixture_manifest_digest_used_by_run
= reviewed_fixture_manifest_digest

contract_manifest_digest_used_by_run
= reviewed_contract_manifest_digest

fixture review verdict
= PASS
~~~

Otherwise:

~~~text
verification = INVALID_EVIDENCE
~~~

Oracle and fixture reviews are independent gates.

A reviewed oracle cannot compensate for an unreviewed fixture, and a reviewed fixture cannot compensate for an unreviewed oracle.

---

# 21. Contract manifest

Future authoritative run must generate:

~~~text
u06-contract-manifest.json
schema = U06_CONTRACT_MANIFEST_V0_1
~~~

It binds exact content digests for:
- Unit Spec;
- RDP-01;
- RDP-02;
- RDP-03;
- RDP-04;
- RDP-05;
- RDP-06;
- any aggregate compatibility amendment;
- Shared Contract versions actually consumed;
- verification oracle;
- fixture manifest;
- auth profile.

Verification fails if a required authority artifact differs from the manifest.

---

# 22. Authority-status consistency gate

The verifier must confirm that authority metadata is not stale/internally contradictory.

Examples:

~~~text
RDP says PASS but body contains active blocking verdict
→ FAIL

oracle points to superseded semantic head
→ FAIL

RDP-05 says PROFILE-A BLOCKED
but auth profile enables real C03
→ FAIL

RDP-04 says real delivery BLOCKED
but transport auth enables external delivery
→ FAIL
~~~

This gate is structural governance verification.

---

# 23. Exact-head binding

Authoritative evidence must bind:

~~~text
implementation_target_sha
verifier_sha
verification_contract_sha
contract_manifest_digest
oracle_digest
fixture_manifest_digest
auth_profile_digest

workflow_run_id
job_id
artifact_id/name
artifact_sha256

toolchain
OS/arch
JDK
Maven
Python
~~~

No result from a different implementation head may be reused as authoritative evidence.

If verifier changes after implementation target verification:

~~~text
new authoritative run required
~~~

unless a separately reviewed verifier-only equivalence rule explicitly permits otherwise.

---

# 24. Focused test suites

Future implementation should expose focused suites at least equivalent to:

~~~text
U06ConsumerAdmissionVerificationTest
U06F3OwnerD04VerificationTest
U06StateMutationVerificationTest
U06DeliveryBoundaryVerificationTest
U06DependencyApplicabilityVerificationTest
U06ReplayRecoveryVerificationTest
U06EvidenceHarnessVerificationTest
~~~

Names may differ.

Coverage semantics may not.

---

# 25. Observed evidence must come from implementation objects

Observed evidence must be derived from real implementation/harness objects under test, not handcrafted test summaries.

Examples:
- actual AdmissionResult;
- actual owner/D04 decisions;
- actual P01 CommitResult;
- actual authoritative read-back;
- actual trace records;
- actual DeliveryIntent/attempt/receipt/confirmation;
- actual Consultation transition evidence;
- actual checkpoint/thread state;
- actual U07 eligibility projection.

Forbidden:

~~~text
test constructs "observed = PASS"
without reading implementation outputs
~~~

---

# 26. Structured case evidence

Define:

~~~text
U06_CASE_EVIDENCE_V0_1
~~~

Minimum:

~~~text
case_id
mode
fixture_id
fixture_digest

expected_boundary
observed_boundary

expected_status
observed_status

expected_reason_code?
observed_reason_code?

expected_authority_refs[]
observed_authority_refs[]

expected_identity_equalities[]
observed_identity_equalities[]

expected_provenance_equalities[]
observed_provenance_equalities[]

expected_effect_counts
observed_effect_counts

trace_refs[]
effect_refs[]
proposal_refs[]
commit_refs[]
delivery_refs[]
checkpoint_refs[]

contract_expectation_gap_count

pass
failure_reasons[]
~~~

---

# 27. Typed effect-count evidence

Every EV records:

~~~text
admission_count
c03_invocation_count
d04_invocation_count

f3_owner_decision_count
f3_canonical_effect_count

state_patch_count
state_commit_count
clinical_state_version_delta

question_selection_decision_count
question_selected_effect_count

delivery_intent_count
delivery_attempt_count
delivery_receipt_count
delivery_confirmation_evaluation_count

external_transport_send_count
external_model_call_count
external_tool_call_count
external_knowledge_call_count

delivered_clinical_state_child_count
consultation_wait_child_count

checkpoint_count
thread_awaiting_transition_count
u07_eligibility_count

failure_handoff_count
~~~

For current PROFILE-B authoritative verification:

~~~text
external_transport_send_count = 0
external_model_call_count = 0
external_tool_call_count = 0
external_knowledge_call_count = 0
~~~

for the entire run.

---

# 28. Identity equality evidence

Must be machine-verifiable for cases requiring replay/continuity.

Examples:

~~~text
admission_id exact replay equality

F3_CANONICAL_EFFECT_ID equality

question_id / QUESTION_SELECTION_EFFECT_ID equality

delivery_id / DELIVERY_IDEMPOTENCY_KEY equality

QUESTION_DELIVERED_WAIT_EFFECT_ID equality

clinical-state child effect equality

Consultation WAITING child effect equality

U06_TRACE_ID equality

U06_DELIVERY_OUTCOME_ID equality

U07_RESUME_ELIGIBILITY_ID equality
~~~

String presence alone is insufficient; expected equality relationships must be encoded and checked.

---

# 29. Cross-contract provenance evidence

At least the following chains must be checkable:

## MODE-1

~~~text
RDP-01 admission
→ dependency binding
→ C03 result
→ F3 owner decision
→ F3 effect
→ K09 proposal
→ P01 commit
→ authoritative read-back
~~~

## MODE-2 selection

~~~text
RDP-01 admission
→ dependency binding
→ fresh C03
→ D04
→ F3QuestionSelectionDecision
→ Question selection effect
→ P01 commit
→ Question SELECTED read-back
~~~

## MODE-2 delivery

~~~text
Question selection effect
→ DeliveryIntent
→ delivery_id
→ attempt
→ receipt
→ confirmation
→ delivered-wait parent effect
→ Clinical State child
→ Consultation WAITING child
→ checkpoint
→ Thread AWAITING_USER
→ U07 eligibility
~~~

## MODE-3

~~~text
admission
→ historical F3 provenance
→ compatibility evidence
→ revalidation decision
→ projection/reassessment/failure
→ zero proposal/commit
~~~

---

# 30. Trace requirements

U06GovernedExecutionTrace evidence must prove:
- same admitted logical execution replay uses same U06_TRACE_ID;
- retries are child evidence;
- exact decision/effect refs are attached;
- no fake release refs;
- PROFILE-B is explicitly synthetic;
- delivery stage refs are correlated;
- failures identify exact stage;
- trace does not substitute for authoritative state.

---

# 31. Delivery evidence

For EV-062..096 and CW-01..09 retain at minimum:

~~~text
QuestionDeliveryAuthority
DeliveryIntent
DeliveryAttempt history
DeliveryReceipt history
DeliveryConfirmationDecision history
U06QuestionDeliveryOutcome

SyntheticDeliveryScopeAuthorization
external_side_effect flag

delivered-wait parent effect
Clinical State child result
Consultation child result
checkpoint
Thread state evidence
U07 eligibility
~~~

Raw provider payload is not required for PROFILE-B.

---

# 32. Crash-window evidence

Each CW subcase requires:

~~~text
crash_point
pre_crash durable records
post_restart recovered records

transport send count before
transport send count after

business effect counts before/after

identity equality checks
final reconciliation status
pass
~~~

Critical C3 assertion:

~~~text
ambiguous send
+ adapter lacks idempotency/query
→ total external/synthetic send-attempt count does not increase by blind retry
~~~

For PROFILE-B there is still zero real external I/O.

---

# 33. Concurrency evidence

Must include at least:
- two distinct F3 proposals on same base;
- exact same F3 effect concurrent replay;
- two Question-selection proposals on same base;
- competing pending Question;
- competing Consultation WAITING question;
- competing delivery effects for same selection.

Expected:

~~~text
no last-write-wins
at most one distinct authoritative effect
exact replay reattaches
conflicting distinct effect fails closed
~~~

---

# 34. Verification environment isolation

For the current PROFILE-B authoritative run, zero-external-effect proof must not depend only on implementation counters.

For the authoritative PROFILE-B run, verification environment isolation is mandatory, not best-effort.

It must provide a verifiable:

~~~text
network egress = DENY_BY_DEFAULT
or an independently provable equivalent isolated network boundary

production credentials/secrets = absent
real recipient endpoints = absent
production database/store credentials = absent

synthetic adapters = explicit allowlist only
external-call boundary spy/interceptor = enabled as defense-in-depth
~~~

The external-call spy does not replace network/environment isolation.

If the CI platform cannot prove deny-by-default or equivalent isolation:

~~~text
verification verdict
= INVALID_EVIDENCE
or INCOMPLETE

PASS = prohibited
~~~

Define:

~~~text
U06_VERIFICATION_ENVIRONMENT_ISOLATION_V0_1

environment_id
network_egress_policy
allowed_endpoint_set
credential_inventory_digest
synthetic_adapter_allowlist_digest

external_call_spy_enabled
external_call_spy_observed_count

production_secret_count
real_recipient_endpoint_count

verdict
evidence_refs[]
~~~

A network/spy/environment violation is authoritative even if runtime self-reported external counters are zero.

Environment evidence must be produced outside the U06 SUT itself or by CI/platform controls that the SUT cannot redefine.

Acceptance requires:

~~~text
environment isolation verdict = PASS
production_secret_count = 0
real_recipient_endpoint_count = 0
external_call_spy_observed_count = 0
~~~

for current PROFILE-B.

---

# 35. Structural authorization guards

Static/runtime guard evidence must prove:

~~~text
no real external transport implementation activated
no real recipient endpoint loaded
no production store configured
no real C03 binding accepted
no real D04 clinical policy activated
no unauthorized model/tool/knowledge client call
no direct Clinical State mutation bypassing P01
no direct Consultation WAITING from transport adapter
no direct Thread AWAITING from delivery adapter
no live U07 execution
no live U14 final business routing
~~~

---

# 36. PROFILE-A blocked proof

Current verification must positively test that repository state cannot execute PROFILE-A.

At least:
- current adult_respiratory question package is not approved;
- C03 real role/binding not active;
- D04 real policy not approved;
- delivery policy not runtime eligible;
- production delivery blocked.

This is a guard test.

It is not evidence that PROFILE-A is implemented.

---

# 37. PROFILE-B zero-side-effect proof

Authoritative run hard totals:

~~~text
real_external_delivery_count = 0
real_model_call_count = 0
real_tool_call_count = 0
real_knowledge_call_count = 0
real_patient_record_count = 0
production_store_write_count = 0
~~~

Synthetic adapter activity is recorded separately and must never increment real external counters.

---

# 38. Regression acceptance

Authoritative run must execute:
- focused U06 verification;
- current Foundation regression;
- U01;
- U02;
- U03;
- U04;
- U05;
- repository-wide applicable diagnosis-service regression.

Acceptance:

~~~text
failures = 0
errors = 0
unexpected_skips = 0
~~~

Historical expected/explicit skips must be enumerated.

Test-count decreases from the bound baseline require explanation and independent review.

---

# 39. Skip policy

Required EV/CW/HG/VG identities:

~~~text
must PASS
or overall verification FAIL
~~~

No required case may be marked SKIPPED because the code path is missing.

A conditionally inapplicable external dependency branch may be structurally verified through an explicit guard case, not silently omitted.

---

# 40. Durable evidence bundle

Future CI artifact:

~~~text
u06-rdp06-evidence/
  result-bundle.json
  evidence.json
  cases.jsonl
  crash-windows.jsonl
  harness-gates.json
  verification-gates.json

  contract-manifest.json
  verification-expectations.json
  fixture-manifest.json
  verification-auth-profile.json
  oracle-review-gate.json
  environment-isolation.json
  authorized-shared-runtime-change-manifest.json

  trace-evidence.jsonl
  effect-evidence.jsonl
  delivery-evidence.jsonl
  concurrency-evidence.jsonl

  regression-summary.json
  environment.json
  artifact-manifest.json
  SHA256SUMS
~~~

Exact filenames may be versioned but semantic contents are required.

---

# 41. Evidence summary schema

Define:

~~~text
U06_VERIFICATION_EVIDENCE_V0_1
~~~

Minimum summary:

~~~text
schema_version
verdict

implementation_target_sha
verifier_sha
verification_contract_sha

contract_manifest_digest
oracle_digest
fixture_manifest_digest
auth_profile_digest
oracle_review_id
reviewed_oracle_digest
environment_isolation_digest
authorized_shared_runtime_change_manifest_digest

required_ev_count = 106
passed_ev_count

required_crash_window_count = 9
passed_crash_window_count

required_hg_count = 3
passed_hg_count

required_vg_count = 10
passed_vg_count

contract_expectation_gap_count

real_phi_count
real_external_delivery_count
real_model_call_count
real_tool_call_count
real_knowledge_call_count
production_store_write_count
unreviewed_shared_runtime_change_count

environment_isolation_verdict
external_call_spy_observed_count

regression
workflow
toolchain

artifact_sha256
generated_at
~~~

---

# 42. Evidence builder fail-closed rules

Evidence builder must fail if:
- any required case missing;
- duplicate case id;
- expected/observed schema invalid;
- expectation authority missing;
- oracle/fixture/contract digest mismatch;
- contract expectation gap > 0;
- any hard boundary counter > 0;
- oracle review missing/non-PASS/digest mismatch;
- environment isolation missing/non-PASS;
- external-call spy observed count > 0;
- unreviewed shared-runtime change count > 0;
- any required regression failure;
- any required trace/effect ref missing;
- any crash-window identity missing;
- any evidence file hash mismatch.

Builder must not normalize failure into PASS.

---

# 43. Artifact integrity

Artifact manifest must hash every retained evidence file.

Required:

~~~text
SHA-256
stable canonical serialization where applicable
artifact-level SHA-256
workflow metadata
~~~

Independent reviewer must be able to recompute hashes.

GitHub artifact metadata alone is not sufficient if bundle internal hashes are absent.

---

# 44. Retention

Minimum policy for the future authoritative run:

~~~text
raw CI artifact
→ retain at least 90 days when platform permits

accepted compact evidence snapshot
→ repository-retained after independent acceptance

workflow/run/artifact metadata
→ recorded in closure document

contract/oracle/fixture digests
→ repository-retained
~~~

No raw PHI should require retention because PHI is prohibited from this verification profile.

---

# 45. Independent review sequence after implementation

Future implementation verification sequence:

~~~text
1. bind exact implementation target

2. build/review static oracle + fixtures + auth profile

3. run focused verifier

4. build durable evidence bundle

5. validate evidence bundle independently

6. run regression

7. perform exact-head code review

8. perform evidence-only independent review

9. if findings:
   targeted remediation
   new exact target
   new authoritative run

10. only after accepted evidence:
    implementation verification closure decision
~~~

Implementation test author and evidence reviewer must not collapse into one unreviewed authority path.

---

# 46. Independent evidence review minimum checks

Independent reviewer must verify:
- exact implementation SHA;
- exact verifier SHA;
- RDP contract heads/digests;
- oracle independent of implementation outputs;
- all 106 EV;
- EV-101..103 post-F3 Safety barrier evidence;
- EV-104..106 MODE-3 replay/stale-before-publish evidence;
- all 9 CW;
- all 3 HG;
- all 10 VG;
- expectation gap = 0;
- hard external counters = 0;
- environment isolation and external-call spy evidence pass;
- oracle review digest is exact and independently accepted;
- shared-runtime target changes are within reviewed allowlist;
- PROFILE-A guard works;
- PROFILE-B scope isolation works;
- replay identities;
- no duplicate delivery effect/send;
- cross-store wait choreography;
- U07 eligibility only after AWAITING_USER;
- regressions pass;
- artifact hashes match.

---

# 47. Contract change invalidation

Any semantic change to RDP-01..06, Unit Spec, or relevant Phase contract after accepted verification:

~~~text
may invalidate evidence
~~~

If changed authority can alter expected behavior:

~~~text
new contract manifest
new oracle review
new authoritative run
~~~

Status/provenance-only edits may be handled only through an independently reviewed equivalence determination.

---

# 48. Implementation change invalidation

Any implementation change touching U06 execution semantics after authoritative run requires re-verification unless independently proven non-semantic.

Always re-run for changes to:
- admission;
- C03/D04 adapters;
- owner policy;
- P01 mapping;
- effect identity;
- trace;
- delivery;
- Consultation WAITING;
- checkpoint/thread wait;
- U07 eligibility;
- synthetic scope guard.

---

# 49. Final verification verdict vocabulary

Only:

~~~text
PASS
FAIL
INCOMPLETE
INVALID_EVIDENCE
~~~

Semantics:

~~~text
PASS
= every required EV/CW/HG/VG passes
+ expectation gap = 0
+ hard boundaries preserved
+ regression accepted
+ evidence integrity accepted

FAIL
= executable verification found mismatch

INCOMPLETE
= required implementation/evidence surface absent

INVALID_EVIDENCE
= evidence cannot be trusted/reproduced
~~~

No READY/PRODUCTION status is produced by RDP-06 verification.

---

# 50. Acceptance thresholds

Future U06 implementation verification may PASS only if:

~~~text
106 / 106 EV PASS
9 / 9 crash windows PASS
3 / 3 harness self-tests PASS
10 / 10 verification gates PASS

contract_expectation_gap_count = 0

real_phi_count = 0
real_external_delivery_count = 0
real_model_call_count = 0
real_tool_call_count = 0
real_knowledge_call_count = 0
production_store_write_count = 0
unreviewed_shared_runtime_change_count = 0
external_call_spy_observed_count = 0

oracle review = PASS
fixture review = PASS
environment isolation = PASS
shared-runtime change manifest = PASS

regression failures = 0
regression errors = 0
unexpected skips = 0

artifact integrity = PASS
independent evidence review = PASS
~~~

---

# 51. Coverage traceability matrix

~~~text
RDP-01
→ EV-001..018
→ VG-001/002/007/010

RDP-02
→ EV-019..041
→ HG-001
→ VG-002/010

RDP-03
→ EV-042..061
→ EV-101..106 where state/barrier/revalidation interaction applies
→ VG-008
→ CW-05..09

RDP-04
→ EV-062..096
→ EV-101..103 post-F3 no-delivery/wait boundary
→ CW-01..09
→ HG-002/003
→ VG-003/004/005/006/009

RDP-05
→ EV-097..100
→ VG-004/005/006/007

RDP-01 / Phase-9 post-F3 barrier
→ EV-101..103

RDP-02 / MODE-3 revalidation
→ EV-104..106

global regression/integrity
→ VG-001/006/010
~~~

Every RDP has executable evidence obligations.

---

# 52. Current implementation posture

Current repository still has no authorized U06 implementation.

Therefore this RDP does not claim:
- tests exist;
- verifier exists;
- evidence exists;
- U06 is implementation-ready;
- PROFILE-B is implementation-authorized.

It only freezes what those future verification assets must prove.

---

# 53. Readiness blocker disposition

If Independent Design Review passes:

~~~text
BF-U06-RG-06
= CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE
~~~

At that point:

~~~text
RG-01..RG-06
all have designed contracts
~~~

but:

~~~text
all contracts designed
!= aggregate compatibility closed
!= implementation readiness established
!= implementation authorization
~~~

Because RDP-01/RDP-05 and RDP-03/Phase-8 plus RDP-03/RDP-04 shared impacts still require aggregate compatibility closure.

---

# 54. Required next governance step after RDP-06 PASS

After RDP-06 design PASS, the next step is:

~~~text
U06 Aggregate Compatibility Review / Controlled Amendment
~~~

At minimum it must reconcile:
- RDP-01 normalized dependency binding wording with RDP-05;
- Phase-8 F3 effect binding identity with RDP-03 RDP03-COMPAT-F3-ID-01;
- RDP-03/RDP-04 delivery/wait cross-store impact package;
- P01/P05/Consultation/Runtime physical impact inventory;
- upstream MODE-1 / MODE-3 source amendments;
- direct F1 executable-admission amendment;
- RDP-06 oracle authority heads after any refreeze.

Only after aggregate compatibility is refrozen may U06 Implementation Readiness Re-Evaluation occur.

---

# 55. Authorization boundary

RDP-06 does not authorize:

~~~text
U06 implementation
synthetic adapter implementation
C03 activation
D04 activation
P01/P05 amendment
Consultation schema change
Runtime checkpoint/thread change
U07 implementation
external delivery
model/tool/knowledge calls
merge
production
real-patient traffic
~~~

---

# 56. Independent Design Review Remediation

Initial Independent Design Review:

~~~text
PR #235
review_id = 5287844164
verdict = REVISE_REQUIRED
reviewed_head = c6724b010918f728099299309e27ea9b402d27bb
~~~

Findings:

~~~text
BF-U06-RDP06-IR-01
= RG06_MODE1_POST_F3_SAFETY_BARRIER_COVERAGE_MISSING

BF-U06-RDP06-IR-02
= MODE3_REPLAY_AND_STALE_BEFORE_PUBLISH_COVERAGE_MISSING

BF-U06-RDP06-IR-03
= INDEPENDENT_ORACLE_REVIEW_PROVENANCE_UNDERDEFINED

BF-U06-RDP06-IR-04
= ZERO_EXTERNAL_SIDE_EFFECT_PROOF_TOO_SELF_REPORTED

BF-U06-RDP06-IR-05
= SHARED_RUNTIME_AUTH_PROFILE_OVERCONSTRAINED
~~~

Remediation applied:

1. expanded required EV set to 106 and added EV-101..103 for post-F3 Safety barrier ordering/block/preemption;

2. added EV-104..106 for MODE-3 exact replay, changed-evidence replay conflict, and STALE_BEFORE_PUBLISH;

3. added U06_ORACLE_REVIEW_GATE_V0_1 with exact reviewed oracle/contract digests and independence marker;

4. added externally constrained verification environment evidence: denied-by-default egress, no production secrets/endpoints, synthetic-adapter allowlist and external-call spy/interceptor;

5. replaced blanket shared-runtime-change prohibition with reviewed allowlist refs/paths and unreviewed_shared_runtime_change_count = 0.

Targeted Independent Design Re-Review:

~~~text
review_id = 5287857225
verdict = REVISE_REQUIRED
reviewed_head = 1ddf342ce43de8ac94943ec1afe295e2aa29a7ee
~~~

Additional findings:

~~~text
BF-U06-RDP06-TR-01
= SYNTHETIC_FIXTURE_INDEPENDENT_REVIEW_GATE_MISSING

BF-U06-RDP06-TR-02
= ENVIRONMENT_ISOLATION_REQUIREMENT_SOFTENED_BY_WHERE_AVAILABLE

BF-U06-RDP06-TR-03
= SHARED_RUNTIME_ALLOWLIST_DIFF_PROVENANCE_UNDERDEFINED
~~~

Additional remediation:

6. added U06_FIXTURE_REVIEW_GATE_V0_1 with exact fixture/contract digests and independent review requirements;

7. made authoritative PROFILE-B network isolation mandatory; inability to prove deny-by-default/equivalent isolation now makes PASS impossible;

8. added U06_AUTHORIZED_SHARED_RUNTIME_CHANGE_MANIFEST and required changed-file computation from exact bound Git diff rather than SUT self-report.

Current:

~~~text
BF-U06-RDP06-IR-01
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP06-IR-02
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP06-IR-03
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP06-IR-04
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP06-IR-05
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP06-TR-01
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP06-TR-02
= REMEDIATED / RE-REVIEW_PENDING

BF-U06-RDP06-TR-03
= REMEDIATED / RE-REVIEW_PENDING

U06-RDP-06
= REVISED / READY_FOR_SECOND_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW

BF-U06-RG-06
= OPEN / DESIGN_RE_REVIEW_PENDING

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
~~~

# 57. Revised verdict

~~~text
U06-RDP-06
= REVISED / READY_FOR_SECOND_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW
~~~

No implementation, synthetic adapter implementation, real C03/D04 activation, external delivery, merge, production, or real-patient authorization is granted.
