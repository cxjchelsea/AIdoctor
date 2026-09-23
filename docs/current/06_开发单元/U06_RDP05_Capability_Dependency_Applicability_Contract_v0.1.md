# U06-RDP-05 Capability / Dependency / Applicability Contract v0.1

> Unit: U06  
> Readiness blocker: `BF-U06-RG-05`  
> Design basis: approved U06 Unit Spec + U06 Initial Implementation Readiness / Gap Review + U06-RDP-01  
> Parent design head: `a7fc37a17f488670915faf517dc7ea7f4cd18680`  
> Runtime repository basis: `main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8`  
> Scope: **CAPABILITY / DEPENDENCY / APPLICABILITY DESIGN ONLY**  
> Status: **PASS / CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE**  
> This contract grants no C03/D04 activation, clinical-content approval, model invocation, knowledge release, implementation, merge, live delivery, production, or real-patient authorization.

---

# 1. Purpose

RDP-05 回答：

```
U06 每个 Mode 到底依赖什么？
哪些依赖必须 present？
哪些是 conditional？
哪些明确 NOT_APPLICABLE？
CapabilityBindingRef 到底证明什么？
Rule/Knowledge/Prompt/Model refs 什么时候必须存在？
当前 adult_respiratory question package 能不能正式用？
C03/D04 什么时候算“可用于真实 Unit”？
如果临床内容尚未批准，U06 还能否做受限 structural non-production implementation？
```

核心不变量：

```
Capability Exists
!= Capability Active

Capability Binding Exists
!= all dependency releases approved

Capability Result
!= Clinical Truth

Question asset exists
!= clinically approved question policy

schema-valid package
!= runtime-eligible package

synthetic structural adapter
!= C03 clinical capability
!= D04 clinical policy
```

---

# 2. Frozen U06 dependency row

Phase 7 frozen Unit row remains:

```
Clinical Capability:
C03

Platform:
P01
P05
P06

Deterministic Policy:
D04
```

RDP-05 does not rewrite this row.

Conditional cross-cutting dependencies:

```
P03
= conditional when approved C03 implementation performs formal model calls

P04
= conditional when approved C03/D04 path consumes formal medical knowledge or rule releases
```

Therefore:

```
P03/P04 conditional applicability
!= permanent addition to U06 base row
```

---

# 3. Dependency applicability vocabulary

每个 dependency/ref family 对每个 U06 mode 必须有一个 typed applicability：

```
REQUIRED
OPTIONAL_BY_APPROVED_BINDING
NOT_APPLICABLE
HISTORICAL_PROVENANCE_ONLY
BLOCKED_PENDING_APPROVAL
```

禁止：

```
null
→ silently interpreted as NOT_APPLICABLE

missing ref
→ placeholder fake ref

empty allowlist
→ treated as unrestricted

DRAFT asset
→ treated as active because file exists
```

---

# 4. Mode-level dependency matrix

| Dependency | MODE-1 PRE_READINESS_GAP_ASSESSMENT | MODE-2 QUESTION_SELECTION_DELIVERY | MODE-3 F3_CURRENT_VERSION_REVALIDATION |
|---|---|---|---|
| C03 | REQUIRED | REQUIRED | NOT_APPLICABLE by default |
| D04 | NOT_APPLICABLE to question stopping | REQUIRED | NOT_APPLICABLE |
| P01 | REQUIRED if canonical F3 commit occurs | REQUIRED for governed Question/WAITING mutations | NOT_APPLICABLE to revalidation projection |
| P05 | REQUIRED | REQUIRED | REQUIRED |
| P06 | REQUIRED | REQUIRED | REQUIRED for historical/current compatibility resolution |
| P03 | CONDITIONAL | CONDITIONAL | NOT_APPLICABLE by default |
| P04 | CONDITIONAL | CONDITIONAL | HISTORICAL_PROVENANCE_ONLY or CONDITIONAL_COMPATIBILITY |
| current C03 CapabilityBindingRef | REQUIRED | REQUIRED | NOT_APPLICABLE by default |
| historical C03 CapabilityBindingRef | NOT_APPLICABLE except reassessment provenance | NOT_APPLICABLE | REQUIRED |
| Question Policy ref | NOT_APPLICABLE | REQUIRED | NOT_APPLICABLE |
| RuleReleaseRef | OPTIONAL_BY_APPROVED_BINDING | REQUIRED/OPTIONAL according to D04/C03 profile | HISTORICAL_PROVENANCE_ONLY as applicable |
| KnowledgeReleaseRef | OPTIONAL_BY_APPROVED_BINDING | OPTIONAL_BY_APPROVED_BINDING | HISTORICAL_PROVENANCE_ONLY as applicable |
| PromptReleaseRef | CONDITIONAL on P03 | CONDITIONAL on P03 | HISTORICAL only if needed for compatibility |
| ModelRouteRef | CONDITIONAL on P03 | CONDITIONAL on P03 | HISTORICAL only if needed for compatibility |
| Tool/Skill refs | CONDITIONAL on approved C03 binding | CONDITIONAL on approved C03 binding | NOT_APPLICABLE by default |

Important:

```
MODE-3
does not require a current invokable C03 binding
```

because its default action is deterministic owner revalidation.

It does require sufficient historical binding/release provenance to decide semantic compatibility.

---

# 5. C03 contract boundary

C03 purpose remains:

```
Gap Detection
Question Candidate Generation
Question Value / Priority Assessment
Duplicate / Already-answered Filtering
Question Rendering
Decision Impact Estimation
```

and later U09 extension.

C03 does not own:

```
canonical F3 truth
D04 final continue/stop decision
Question authoritative lifecycle
WAITING_USER
Clinical Readiness
```

## 5.1 Required C03 result semantics

A governed C03 invocation must return typed:

```
capability_result_id
capability_id = C03
capability_version

dependency_binding_type
dependency_binding_ref
capability_binding_ref?

business_status
reason_code
retryable

input_clinical_state_version
input_basis_refs[]

gap_candidates[]?
question_candidates[]?
decision_impact_candidates[]?
duplicate/already_answered evidence?
rendering candidate refs?

rule_release_refs[] as applicable
knowledge_release_refs[] as applicable
prompt_release_ref? as applicable
model_route_ref? as applicable
tool_refs[]? as applicable
skill_refs[]? as applicable

created_at
trace_refs[]
```

Business status must reuse the frozen capability vocabulary:

```
SUCCESS
NO_RESULT
INSUFFICIENT_INFORMATION
NOT_APPLICABLE
UNSUPPORTED
DEPENDENCY_FAILURE
TIMEOUT
INVALID_OUTPUT
SAFETY_BLOCKED
```

No `null/[]` inference.

---

# 6. D04 contract boundary

D04 is a deterministic business policy first consumed by U06.

It decides whether a MODE-2 question path may continue or must stop under the approved policy.

D04 does not:
- generate medical facts;
- create F3 truth;
- create Question truth;
- set WAITING_USER;
- override Safety;
- transform C03 failure into a business-negative result.

Minimum governed D04 binding must identify:

```
question_policy_ref
policy_id = D04
policy_version

applicable source class
  F1_MINIMAL_CLARIFICATION
  F3_ACTIONABLE_GAP

allowed inputs / basis refs
stopping rule release refs as applicable
question-policy content refs as applicable

owner/reviewer status
effective/active status
scope/population/region/language/channel constraints
```

Exact decision vocabulary/precedence belongs to RDP-02.

---

# 7. Current capability package assessment

Repository package:

```
capabilities/adult_respiratory_v1
```

currently states:

```
lifecycle = DRAFT
clinical_review_status = REQUIRES_CLINICAL_REVIEW
technical_review_status = PENDING
runtime_adoption = NOT_IMPLEMENTED
production_eligibility = BLOCKED
overall_evidence = PARTIALLY_VALIDATED
```

Question policy assets:

```
mandatory.yaml
discriminators.yaml
stopping_rules.yaml
```

all state:

```
review_status = REQUIRES_CLINICAL_REVIEW
questions = []
prohibited_runtime_use = true
```

Question eval currently proves only structural skeleton behavior:

```
question-structure-001
synthetic = true
expected_structural_outcome = ACCEPT_BLOCKED_SKELETON
blocked_assertions:
  QUESTION_CLINICAL_VALUE
  PATIENT_WORDING_SAFETY
```

Runtime allowlists are also empty and blocked:

```
model_routes.references = []
prompt_allowlist.references = []
tool_allowlist.references = []
skill_allowlist.references = []

runtime_eligibility = NOT_IMPLEMENTED
production_eligibility = BLOCKED
prohibited_runtime_use = true
```

Knowledge package similarly has:

```
approved_source_count = 0
retrieval_eligibility = BLOCKED
sources = []
prohibited_runtime_use = true
```

Therefore:

```
adult_respiratory_v1
= STRUCTURAL PACKAGE SKELETON

!= active C03 binding
!= approved D04 policy
!= approved patient-facing question content
!= approved KnowledgeRelease
!= approved Prompt/Model route
!= runtime-eligible U06 dependency package
```

---

# 8. C03 Capability Quality Gate

A real governed C03 may enter formal U06 execution only after all applicable quality-gate evidence exists.

Minimum required:

```
Capability ID / Version
Purpose
Supported Scope

Input semantic boundary
Output semantic boundary
Failure semantics

CapabilityBindingRef
applicable release/route refs

EvalSet
Baseline
Metrics
Acceptance thresholds

Safety cases
Regression suite
Fallback / unavailable behavior

Owner
clinical review
safety review
technical review
knowledge/license review as applicable

runtime eligibility
```

C03-specific quality dimensions:

```
question purpose traceability
decision value
duplicate question rate
already-answered suppression
invalidated-gap suppression
USER_UNKNOWN / UNMEASURED repeat behavior
patient burden
no-value stopping
diagnostic-suggestion leakage
treatment-overreach leakage
patient wording safety
```

The exact numeric acceptance thresholds are not invented by RDP-05.

They require approved clinical/product/evaluation authority.

Until that evidence exists:

```
REAL_C03_CLINICAL_EXECUTION
= BLOCKED
```

---

# 9. D04 Policy Quality Gate

D04 may enter real MODE-2 execution only when its policy package is reviewed/approved.

At minimum:

```
D04 policy id/version
scope
source class applicability
stop/continue semantics
precedence
decision-value interpretation boundary
duplicate/already-answered behavior
USER_UNKNOWN / UNMEASURED behavior
bounded/no-progress behavior
failure semantics
owner
clinical/safety review
effective/active status
regression cases
```

Current `stopping_rules.yaml` explicitly says no approved clinical stopping condition exists.

Therefore:

```
REAL_D04_QUESTION_POLICY
= BLOCKED
```

RDP-05 does not turn empty stopping rules into an approved default.

---

# 10. P06 current implementation gap

Current `CapabilityBindingRecord` physically binds:

```
binding_id
capability_id/version
capability_set_version
scope_version
contract_version
population
region
language
channel
status
effective window
```

Current `BindingReleaseResolver` validates:
- binding exists;
- ACTIVE;
- expected capability id;
- effective window;
- scope/contract/population/region/language/channel.

It does not currently resolve or enforce:

```
KnowledgeReleaseRef allowlist
RuleReleaseRef allowlist
PromptReleaseRef allowlist
ModelRouteRef allowlist
Tool refs
Skill refs
Question Policy ref
rollback compatibility
full release lifecycle
```

Therefore:

```
current P06 minimal binding
= reusable foundation

!= complete U06 dependency authorization
```

---

# 11. U06 governed dependency binding identity and view

RDP-05 must support two different governed binding identity classes:

```
REAL_CAPABILITY_BINDING
SYNTHETIC_VERIFICATION_BINDING
```

Define normalized identity fields:

```
dependency_binding_type
dependency_binding_ref
```

Rules:

### REAL_CAPABILITY_BINDING

```
dependency_binding_type = REAL_CAPABILITY_BINDING
dependency_binding_ref = real P06 CapabilityBindingRef
```

Required for PROFILE-A.

The referenced binding must resolve through governed P06 and must represent a registered implementation whose governed capability role is C03 when C03 is required.

### SYNTHETIC_VERIFICATION_BINDING

```
dependency_binding_type = SYNTHETIC_VERIFICATION_BINDING
dependency_binding_ref = stable synthetic verification binding identity
```

Required for PROFILE-B.

It must:
- exist only in the authorized synthetic/non-production verification surface;
- never be registered as an ACTIVE clinical capability binding in the real P06 registry;
- never be accepted by production/live capability invocation paths;
- carry explicit `synthetic = true`;
- carry `patient_data_classification = SYNTHETIC_NON_PATIENT`;
- carry an allowed environment/profile identity.

Therefore:

```
Pre-aggregate RDP-01 wording was:

```text
capability_binding_ref = REQUIRED
```

The aggregate amendment candidate replaces that source-neutral wording with:

```text
a governed dependency binding identity is REQUIRED

PROFILE-A
→ real CapabilityBindingRef

PROFILE-B
→ synthetic verification binding ref
```

This is an explicit RDP-01 aggregate compatibility amendment impact and must not be silently implemented as a semantic drift.

RDP-05 then defines a semantic normalized object:

```
U06DependencyBindingView
```

This is a resolved/validated view, not Clinical State.

Minimum fields:

```
dependency_binding_view_id

dependency_binding_type
dependency_binding_ref

consultation_id
cdp_id
u06_mode

expected_capability_role = C03 when C03 applies
registered_capability_role?
capability_binding_ref?
capability_id?
capability_version?
capability_set_version?

scope_version
contract_version
population
region
language
channel

question_policy_ref?
d04_policy_version?

knowledge_release_refs[]
rule_release_refs[]
prompt_release_refs[]
model_route_refs[]
tool_refs[]
skill_refs[]

dependency_applicability_profile_id
dependency_applicability_fingerprint

p03_applicability
p04_applicability

binding_status
release_validation_status
policy_validation_status

resolved_at
validity

trace_refs[]
```

It must be formed from:
- authoritative registries/policies for REAL_CAPABILITY_BINDING; or
- a specifically authorized synthetic verification binding registry/fixture set for SYNTHETIC_VERIFICATION_BINDING.

It must not be caller-populated and blindly trusted.

## 11.1 C03 implementation role identity

RDP-05 freezes:

```
expected_capability_role = C03
```

whenever MODE-1 or MODE-2 requires C03.

A real binding must resolve:

```
CapabilityBindingRef
→ registered implementation/version
→ governed capability role = C03
```

A file path, package directory, broad package id, class name, prompt name, or model route is not sufficient authority.

Specifically:

```
adult_respiratory_v1
```

currently identifies a broad structural capability package.

It may provide asset/package provenance in the future, but:

```
adult_respiratory_v1 exists
!= registered C03 implementation
!= active C03 binding
```

unless a future governed registration explicitly maps an approved implementation/version from that package to capability role C03 and passes the required quality/activation gates.

PROFILE-B uses:

```
registered_capability_role = SYNTHETIC_C03
expected_capability_role = C03
synthetic = true
```

only to exercise the C03 consumer interface shape.

It must never be reported as:

```
C03 ACTIVE
C03 QUALITY GATE PASS
real clinical capability invocation
```

---

# 12. Applicability profile identity

Define:

```
U06_DEPENDENCY_APPLICABILITY_PROFILE
```

A profile binds:

```
u06_mode
execution_scope

C03 applicability
D04 applicability
P01/P05/P06 applicability
conditional P03/P04 applicability

required/optional/not-applicable ref families

allowed dependency classes
forbidden dependency classes

profile policy version
```

Any change in applicability that can alter execution semantics must change:

```
dependency_applicability_profile_id
or
dependency_applicability_fingerprint
```

No silent reuse.

---

# 13. MODE-1 dependency profile

For:

```
PRE_READINESS_GAP_ASSESSMENT
```

required:

```
C03 = REQUIRED
P06 = REQUIRED
P05 = REQUIRED
P01 = REQUIRED for successful canonical F3 mutation
D04 question-stopping = NOT_APPLICABLE
```

conditional:

```
P03
if C03 uses model

P04
if C03 consumes formal medical knowledge/rules

RuleReleaseRef
if approved C03/F3-owner policy depends on rule release

KnowledgeReleaseRef
if approved C03 depends on medical knowledge

PromptReleaseRef + ModelRouteRef
if P03 applicable

Tool/Skill refs
if approved C03 binding uses them
```

forbidden:

```
Question Policy ref used as authorization to ask user
MODE-2 patient-facing delivery permission
```

MODE-1 question candidate material remains ephemeral/support-only.

---

# 14. MODE-2 dependency profile

For:

```
QUESTION_SELECTION_DELIVERY
```

required:

```
C03 = REQUIRED
D04 = REQUIRED
P06 = REQUIRED
P05 = REQUIRED
P01 = REQUIRED for governed Question/WAITING business-state mutations
question_policy_ref = REQUIRED
```

conditional:

```
P03
if C03 rendering/generation uses model

P04
if C03 or D04 consumes formal clinical knowledge/rules

RuleReleaseRefs
according to approved D04/C03 policy

KnowledgeReleaseRefs
according to approved C03 policy

PromptReleaseRef + ModelRouteRef
if P03 applicable

Tool/Skill refs
if approved binding explicitly permits them
```

MODE-2 may not rely on:
- MODE-1 ephemeral candidates;
- unapproved question YAML;
- direct legacy hard-coded question service;
- ungoverned LLM prompt;
- empty allowlist interpreted as wildcard.

---

# 15. MODE-3 dependency profile

For:

```
F3_CURRENT_VERSION_REVALIDATION
```

default:

```
C03 invocation = NOT_APPLICABLE
D04 = NOT_APPLICABLE
P03 = NOT_APPLICABLE
P01 StateChangeProposal = NOT_APPLICABLE
Question Policy = NOT_APPLICABLE
```

required:

```
P05 = REQUIRED
P06 = REQUIRED for compatibility resolution
historical CapabilityBindingRef = REQUIRED
historical release refs = REQUIRED when they were applicable to source F3
semantic binding compatibility evidence = REQUIRED
current Safety/routing authority = REQUIRED through RDP-01
```

P04 may be required only to resolve historical/current release compatibility when source F3 depended on governed knowledge/rules.

MODE-3 must not require a new current C03 invocation binding merely to revalidate history.

If semantic compatibility cannot be proven:

```
REVALIDATED_CURRENT
= prohibited
```

RDP-02/owner revalidation may produce:
- REASSESSMENT_REQUIRED;
- FAILED;

according to the frozen owner policy.

---

# 16. Conditional P03 Model Runtime

P03 becomes applicable only if the approved C03 binding explicitly selects a model/mixed implementation.

Then all of the following become mandatory:

```
PromptReleaseRef
ModelRouteRef

Prompt/Model refs allowed by the governed dependency binding
P03 runtime route active/effective
contract/schema compatibility
privacy/context policy compatibility
fallback/unavailable semantics
trace provenance
```

Forbidden:

```
legacy common LLM direct call
model name string without ModelRouteRef
prompt text without released PromptReleaseRef
automatic latest prompt/model switch
```

Current adult_respiratory model/prompt allowlists are empty and `prohibited_runtime_use=true`.

Therefore current state:

```
P03 for real C03
= BLOCKED
```

unless a future reviewed binding/package activates it.

---

# 17. Conditional P04 knowledge/rule governance

P04 is not an unconditional U06 base dependency.

It becomes applicable when actual C03/D04/F3 owner policy consumes:

```
formal medical knowledge
clinical rule release
knowledge graph evidence
retrieval evidence
safety/clinical policy release
```

Then:

```
KnowledgeReleaseRef / RuleReleaseRef
must be approved/current/effective
must be allowed by the U06 dependency binding
must be traceable
must not silently switch mid-run
```

Current adult_respiratory knowledge assets are blocked and have zero approved sources.

Therefore:

```
current adult_respiratory knowledge package
cannot support real U06 clinical decisions
```

---

# 18. Release applicability — no fake refs

RDP-05 freezes:

```
NOT_APPLICABLE
!= missing accidentally
!= fake placeholder
```

For each ref family:

```
applicability status
+ refs[]
```

must be represented.

Example:

```
knowledge_release_applicability = NOT_APPLICABLE
knowledge_release_refs = []
```

is lawful when the approved profile does not use medical knowledge.

But:

```
knowledge_release_applicability = REQUIRED
knowledge_release_refs = []
```

must fail closed.

Same for Rule/Prompt/Model/Tool/Skill.

---

# 19. P05 release-trace compatibility

Current P05 physical record has nullable:

```
rule_release_ref
knowledge_release_ref
```

but current:

```
CapabilityTraceService.bindReleases(rule, knowledge)
```

requires both values nonblank.

This conflicts with U06:

```
refs are required only as applicable
```

Therefore U06 may not fabricate a missing release ref.

Required compatibility options to be resolved before implementation:

```
A. scoped P05 amendment
   allowing independently optional release families

B. generalized/multi-ref governance trace extension

C. U06 governed trace adapter
   preserving typed NOT_APPLICABLE
   without changing Clinical Truth
```

RDP-05 does not select the physical option.

If P03 becomes applicable, P05 must also represent:

```
PromptReleaseRef
ModelRouteRef
```

without overloading rule/knowledge fields.

---

# 20. Real-governed vs structural-nonproduction execution profiles

RDP-05 freezes two **distinct execution profiles**.

## PROFILE-A — REAL_GOVERNED_C03

Binding identity:

```
dependency_binding_type
= REAL_CAPABILITY_BINDING

dependency_binding_ref
= real governed P06 CapabilityBindingRef

expected_capability_role
= C03
```

May be activated only when:
- C03 Capability Quality Gate passes;
- D04 policy is approved for MODE-2;
- P06 full required dependency authorization exists;
- applicable releases/routes are approved/current;
- P05 provenance can represent them;
- required clinical/safety/eval review is complete.

Then real governed C03 may run within the separately authorized environment.

Current status:

```
PROFILE-A
= BLOCKED
```

## PROFILE-B — SYNTHETIC_STRUCTURAL_NONPROD

Binding identity:

```
dependency_binding_type
= SYNTHETIC_VERIFICATION_BINDING

dependency_binding_ref
= stable synthetic verification binding identity

registered_capability_role
= SYNTHETIC_C03

expected_capability_role
= C03
```

Purpose:

```
verify U06 runtime/contract/state/replay/delivery-boundary mechanics
without claiming clinical question capability
```

It may use only:

```
synthetic non-patient fixtures
deterministic fake/spy C03 adapter
deterministic fake/spy D04 adapter
no clinically meaningful patient-facing wording
no medical decision thresholds
no real PHI
no live delivery
no real model/tool/knowledge retrieval
```

Synthetic C03/D04 outputs must be fixture-driven and predeclared.

They may exercise:
- SUCCESS/NO_RESULT/failure branches;
- candidate count/identity;
- duplicate markers;
- decision-impact labels as synthetic tokens;
- D04 CONTINUE/STOP branch mechanics.

They may not assert:
- clinical correctness;
- clinical question value;
- patient wording safety;
- medically appropriate stopping;
- real C03 Quality Gate PASS.

Current status:

```
PROFILE-B
= DESIGN-ELIGIBLE
= NOT_IMPLEMENTATION_AUTHORIZED
```

---

# 21. Synthetic adapter contract

If PROFILE-B is later authorized, define:

```
SyntheticC03Adapter
SyntheticD04Adapter
```

as verification/non-production dependencies only.

Required properties:

```
explicit profile = SYNTHETIC_STRUCTURAL_NONPROD
synthetic = true
patient_data_classification = SYNTHETIC_NON_PATIENT
no network/model/tool/knowledge call
deterministic fixture input/output
stable capability/policy fixture identity
traceable
fail-closed outside allowed test/nonprod environment
```

Forbidden:
- production bean activation;
- use by live endpoint;
- patient-facing text;
- fallback from failed real C03 to synthetic C03;
- treating synthetic PASS as Capability Quality Gate PASS.

---

# 22. Capability binding state machine for U06

A U06 real dependency is invokable only if:

```
binding exists
AND binding status = ACTIVE
AND effective window valid
AND scope/contract/population/region/language/channel compatible
AND dependency applicability profile matches
AND all REQUIRED releases/routes present
AND all present releases/routes approved/current
AND policy status active
AND environment permits selected execution profile
```

If any required condition fails:

```
no real capability invocation
```

It must become typed:
- admission rejection when detectable at admission;
- dependency failure before invocation;
- governed runtime failure if changed after admission.

No silent latest-version fallback.

---

# 23. Binding immutability and mid-run changes

Once a U06 admitted execution resolves:

```
U06DependencyBindingView
```

the execution must not silently switch:
- capability version;
- Rule/Knowledge release;
- Prompt release;
- Model route;
- question policy version.

If a dependency becomes withdrawn/expired before its first effect:

```
revalidate
→ fail/re-admit/re-resolve as governed
```

Historical completed effects keep historical refs.

MODE-3 specifically uses historical refs for compatibility, not automatic migration.

---

# 24. Failure semantics

Dependency failures must remain distinct:

```
BINDING_NOT_FOUND
BINDING_INACTIVE
BINDING_EXPIRED
BINDING_SCOPE_MISMATCH
CONTRACT_INCOMPATIBLE

REQUIRED_RULE_RELEASE_MISSING
REQUIRED_KNOWLEDGE_RELEASE_MISSING
REQUIRED_PROMPT_RELEASE_MISSING
REQUIRED_MODEL_ROUTE_MISSING

RELEASE_INACTIVE
RELEASE_WITHDRAWN
RELEASE_EXPIRED
RELEASE_NOT_ALLOWED_BY_BINDING

QUESTION_POLICY_NOT_APPROVED
D04_POLICY_NOT_APPROVED
C03_QUALITY_GATE_NOT_PASSED

P03_REQUIRED_BUT_UNAVAILABLE
P04_REQUIRED_BUT_UNAVAILABLE
P05_PROVENANCE_INCOMPATIBLE

SYNTHETIC_PROFILE_NOT_ALLOWED
EXECUTION_PROFILE_MISMATCH
```

These are not:
- C03 `NO_RESULT`;
- D04 `STOP`;
- F3 no-gap truth;
- Clinical Readiness.

---

# 25. Current implementation impact inventory

All impacts remain **NOT_AUTHORIZED**.

## U06-RDP05-IMP-01 — P06 dependency-binding extension

Need a governed way to bind/resolve:
- question policy;
- Rule/Knowledge refs;
- Prompt/Model refs;
- Tool/Skill refs;
- applicability profile.

Current `CapabilityBindingRecord` alone is insufficient.

Possible physical implementation:
- extend binding record;
- companion binding artifact;
- registry-resolved dependency bundle.

Exact choice deferred to implementation design/aggregate review.

Real-profile resolution must also prove:

```
registered capability role = C03
```

rather than accepting a broad capability package id as equivalent to C03.

## U06-RDP05-IMP-02 — release registries/resolvers as applicable

If real profile needs Rule/Knowledge releases:

```
full enough P04 release resolution
```

must exist for U06's exact scope.

## U06-RDP05-IMP-03 — P05 provenance compatibility

Resolve optional/multi-ref release trace and conditional Prompt/Model provenance.

## U06-RDP05-IMP-04 — C03 runtime contract/adapter

No governed C03 runtime implementation currently exists.

Need either:
- PROFILE-A real governed C03 with registered capability role = C03;
- or PROFILE-B synthetic nonprod adapter with explicit role = SYNTHETIC_C03.

The broad `adult_respiratory_v1` package id alone is not a C03 implementation identity.

## U06-RDP05-IMP-05 — D04 executable policy/adapter

No governed D04 runtime implementation currently exists.

Need either:
- approved real D04 policy;
- or PROFILE-B synthetic deterministic adapter.

## U06-RDP05-IMP-06 — conditional P03 adapter

Only if selected real C03 implementation uses model.

No P03 work is required for a deterministic/tool-only or synthetic no-model C03 path.

---

# 26. Capability Quality Gate vs Unit readiness

Must preserve:

```
C03 Quality Gate PASS
!= U06 PASS

D04 policy approved
!= U06 PASS

U06 RDP-05 PASS
!= C03 Quality Gate PASS
```

RDP-05 PASS only means:
- applicability rules are designed;
- current blocked dependencies are truthfully represented;
- implementation may not cross blocked gates.

---

# 27. RDP-01 compatibility

RDP-01 requires:
- MODE-1 current C03 binding;
- MODE-2 current C03 binding + question policy;
- MODE-3 historical binding/release provenance.

RDP-05 refines that as:

```
RDP-01 presence
+ RDP-05 applicability
+ P06 resolution
→ dependency admission validity
```

No contradiction:
- RDP-01 asks whether required refs/source authority are present/current enough to admit;
- RDP-05 defines which refs are required for the exact dependency profile and whether they are authorized.

For PROFILE-B synthetic structural verification, the admitted refs must identify:

```
dependency_binding_type = SYNTHETIC_VERIFICATION_BINDING
dependency_binding_ref = synthetic verification binding identity
```

and must not masquerade as production CapabilityBindingRefs.

This compatibility impact is addressed by the U06 aggregate amendment candidate: RDP-01 now uses dependency_binding_type + dependency_binding_ref as the source-neutral admission identity while preserving the rule that every MODE-1/MODE-2 admission has a stable governed dependency-binding identity. Explicit refreeze is still pending.

---

# 28. RDP-02 compatibility input

RDP-02 may only define owner/policy semantics against dependencies allowed here.

Therefore RDP-02 must support at least:

```
PROFILE-A
real C03 result + approved D04

PROFILE-B
synthetic typed C03 result + synthetic deterministic D04
for structural verification only
```

but RDP-02 must not let PROFILE-B encode clinical thresholds/medical policy.

---

# 29. RDP-03/P05 compatibility input

RDP-03 must preserve:
- `U06DependencyBindingView`;
- applicability profile identity/fingerprint;
- exact refs actually used;
- typed NOT_APPLICABLE;
- historical refs for MODE-3.

It must not require fake release refs to satisfy current P05 implementation.

---

# 30. RDP-04 compatibility input

Delivery authorization is separate from C03/D04 dependency authorization.

Even if PROFILE-A eventually becomes active:

```
C03/D04 authorized
!= live question delivery authorized
```

PROFILE-B always requires:

```
external delivery = 0
```

unless a later explicit synthetic transport seam is separately authorized, and even then it is non-live/non-patient.

---

# 31. RDP-06 verification obligations

RDP-06 must prove:

```
mode-by-mode dependency applicability

missing REQUIRED ref -> fail closed
NOT_APPLICABLE ref family -> no fake ref required

inactive/expired binding
scope mismatch
contract mismatch

release missing/inactive/withdrawn
question policy not approved

MODE-3 no current C03 invocation binding required by default

P03 conditionality
P04 conditionality

PROFILE-A blocked under current repository state

PROFILE-B synthetic marker enforced
no PHI
no live delivery
no model/tool/knowledge external call

synthetic result
!= C03 Quality Gate evidence
```

It must also test P05 compatibility remediation once selected.

---

# 32. Design acceptance scenarios

```
RDP05-AC-01
MODE-1 + approved deterministic C03 with no knowledge/model dependency
→ C03 REQUIRED
→ P03 NOT_APPLICABLE
→ P04 NOT_APPLICABLE
→ no fake release refs

RDP05-AC-02
MODE-1 profile requires KnowledgeRelease
but no approved release
→ dependency resolution FAIL

RDP05-AC-03
MODE-2 without approved D04/question policy
→ real execution BLOCKED

RDP05-AC-04
MODE-2 current repository adult_respiratory question package
→ PROFILE-A BLOCKED

RDP05-AC-05
MODE-3 with historical binding/release refs and proven semantic compatibility
→ no C03 invocation binding required
→ revalidation may proceed

RDP05-AC-06
MODE-3 source used model historically
→ historical Prompt/Model refs retained for compatibility provenance
→ no new model call by default

RDP05-AC-07
CapabilityBindingRecord ACTIVE but required PromptRelease not authorized
→ capability invocation BLOCKED

RDP05-AC-08
empty prompt/model/tool/skill allowlist
→ NOT wildcard

RDP05-AC-09
current P05 requires both release refs but profile says Knowledge NOT_APPLICABLE
→ fake knowledge ref prohibited
→ compatibility amendment required

RDP05-AC-10
PROFILE-B synthetic C03/D04 fixtures
→ structural branch verification allowed after explicit implementation authorization
→ no clinical correctness claim

RDP05-AC-11
attempt to use PROFILE-B in live/production endpoint
→ fail closed

RDP05-AC-12
C03 Quality Gate passes later but live delivery remains unauthorized
→ delivery still blocked by RDP-04/production governance
```

---

# 33. Readiness blocker disposition

If this design passes independent review:

```
BF-U06-RG-05
= CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE
```

Not fully CLOSED because:
- actual implementation profile still requires implementation authorization;
- PROFILE-A current clinical/content gates remain blocked;
- P06/P05 physical compatibility amendments remain unresolved;
- RDP-02/03/04/06 must align;
- aggregate compatibility review must pass.

---

# 34. Current recommended implementation-scope posture

Given current repository evidence:

```
PROFILE-A REAL_GOVERNED_C03
= NOT_READY

PROFILE-B SYNTHETIC_STRUCTURAL_NONPROD
= DESIGN-ELIGIBLE
```

This is not an implementation authorization decision.

The later U06 Implementation Readiness Re-Evaluation should decide whether:

```
A. wait for real C03/D04 Quality Gates before any U06 implementation

or

B. authorize a bounded structural non-production U06 slice
   using PROFILE-B,
   while explicitly excluding clinical/patient-facing capability claims
```

RDP-05 itself does not authorize B.

---

# 35. Authorization boundary

RDP-05 does not authorize:

```
C03 clinical activation
D04 clinical policy activation
adult_respiratory runtime adoption
Prompt/Model activation
Knowledge/Rule release
P03 model call
P04 clinical retrieval
P06/P05 shared-runtime amendment
synthetic adapter implementation
U06 code implementation
question delivery
live endpoint wiring
production
real-patient traffic
```

---

# 36. Design verdict

```
U06-RDP-05 Capability / Dependency / Applicability Contract
= PASS

BF-U06-RG-05
= CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
```

Next after independent design PASS:

```
U06-RDP-02 F3 Owner / D04 Question Policy Contract
```


---

# 37. Independent Design Review Remediation

Initial Independent Design Review:

```
PR #231
review_id = 5287371485
verdict = REVISE_REQUIRED
```

Findings:

```
BF-U06-RDP05-IR-01
= SYNTHETIC_BINDING_IDENTITY_CONFLICT_WITH_RDP01

BF-U06-RDP05-IR-02
= C03_IMPLEMENTATION_IDENTITY_NOT_EXPLICIT
```

Remediation:

1. introduced typed dependency binding identity:
   - `REAL_CAPABILITY_BINDING`;
   - `SYNTHETIC_VERIFICATION_BINDING`;

2. introduced normalized:
   - `dependency_binding_type`;
   - `dependency_binding_ref`;

3. froze PROFILE-A to require a real P06 CapabilityBindingRef;

4. froze PROFILE-B to require a synthetic verification binding that:
   - never enters the real P06 registry as ACTIVE;
   - cannot be consumed by production/live paths;
   - is explicitly synthetic/non-patient;

5. recorded the pre-aggregate RDP-01 capability_binding_ref requirement as an explicit aggregate compatibility impact; the current amendment candidate normalizes it to dependency_binding_type + dependency_binding_ref, pending explicit refreeze;

6. froze:
   `expected_capability_role = C03`
   for real MODE-1/MODE-2 C03 consumption;

7. clarified:
   `adult_respiratory_v1`
   is broad package/asset provenance, not automatically a C03 implementation identity;

8. froze PROFILE-B role as:
   `SYNTHETIC_C03`
   without any claim of C03 activation or Quality Gate PASS.

Current:

```
BF-U06-RDP05-IR-01
= CLOSED

BF-U06-RDP05-IR-02
= CLOSED

Targeted Independent Design Re-Review
= PASS
review_id = 5287380299
reviewed_head = a7fd974faeddd063ba5a178116efc422c94723f5

U06-RDP-05
= PASS
```


---

# 38. Final Design Review Provenance

```
Initial Independent Design Review
= REVISE_REQUIRED
review_id = 5287371485

Targeted Independent Design Re-Review
= PASS
review_id = 5287380299

Reviewed semantic head
= a7fd974faeddd063ba5a178116efc422c94723f5

BF-U06-RDP05-IR-01
= CLOSED

BF-U06-RDP05-IR-02
= CLOSED

U06-RDP-05
= PASS

BF-U06-RG-05
= CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE
```

This status synchronization changes no dependency applicability, binding identity, capability-role rule, Quality Gate, P03/P04 conditionality, PROFILE-A/PROFILE-B boundary, shared-runtime impact inventory, or authorization scope.

Next recommended design:

```
U06-RDP-02
F3 Owner / D04 Question Policy Contract
```

Still:

```
PROFILE-A REAL_GOVERNED_C03
= BLOCKED

PROFILE-B SYNTHETIC_STRUCTURAL_NONPROD
= DESIGN-ELIGIBLE / NOT_IMPLEMENTATION_AUTHORIZED

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
```


---

# Aggregate Compatibility Amendment Resolution — U06-AGR-01

> Aggregate amendment status: REVIEW_PENDING / NOT_REFROZEN

The previously recorded RDP-01 compatibility impact is now resolved by the aggregate amendment candidate as:

~~~text
source-neutral admission identity
= dependency_binding_type + dependency_binding_ref

PROFILE-A
= REAL_CAPABILITY_BINDING + real governed P06 C03 CapabilityBindingRef

PROFILE-B
= SYNTHETIC_VERIFICATION_BINDING + stable synthetic verification identity
~~~

The invariant remains:

~~~text
every MODE-1 / MODE-2 admission
must carry one stable governed dependency-binding identity
~~~

No applicability, C03 role, PROFILE-A/PROFILE-B, Quality Gate, P03/P04, or release semantics change.

Current disposition until explicit re-freeze:

~~~text
RDP05 aggregate compatibility impact
= REMEDIATED / REVIEW_PENDING / REF FREEZE PENDING
~~~


## U06-AGR-01 C03 Result Binding Provenance

Aggregate compatibility requires each governed C03 result consumed by U06 to match the admitted dependency binding view.

Required equality:

~~~text
result.dependency_binding_type
= admitted.dependency_binding_type

result.dependency_binding_ref
= admitted.dependency_binding_ref
~~~

For REAL_CAPABILITY_BINDING, capability_binding_ref carries the resolved governed P06 C03 binding.

For SYNTHETIC_VERIFICATION_BINDING, capability_binding_ref is absent and the synthetic dependency binding identity remains explicitly non-production.

Current disposition:

~~~text
U06-AGR-01 C03 result provenance
= REMEDIATED / COMPATIBILITY_REVIEW_PENDING / NOT_REFROZEN
~~~
