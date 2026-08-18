# A7-CL-01 Clinical Governance Charter

> Dated: 2026-08-18
>
> Classification: `CLINICAL_GOVERNANCE_AUTHORITY`
>
> Lane: `A7-CL`
>
> Batch: `A7-CL-01`
>
> Scope: `adult_respiratory_v1` only
>
> Status: `GOVERNANCE_AUTHORED_PENDING_REVIEW`
>
> Authorization token:
> `A7_CL_01_CLINICAL_GOVERNANCE_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Exact Base: `b03a4d0d915039c4377a98af194dcc45fcb034c9`
>
> Companion matrix:
> [a7-cl-rejoin-exit-matrix.csv](./a7-cl-rejoin-exit-matrix.csv)

```text
THIS_BATCH_DEFINES_AUTHORITY
THIS_BATCH_DOES_NOT_AUTHOR_CLINICAL_CONTENT
THIS_BATCH_DOES_NOT_AUTHORIZE_A7_CL_02
```

## 1. Purpose

Establish the authoritative bridge required **before** any new clinical
content may be authored.

This charter answers:

1. what supersedes obsolete RG-01 / RG-02;
2. which gates must pass before clinical content authoring;
3. what FB-11 currently requires;
4. what belongs to A7-CL implementation;
5. what is required for A7-CL Exit / Full A7 Exit;
6. what remains Phase B;
7. what remains A11-only;
8. what evidence is required to assign Clinical Owner / Reviewer and
   grant content-access authorization;
9. which next batch becomes eligible after those pre-content gates.

## 2. Authority / provenance

Binding sources (not rewritten here):

- PR #29 [A7-NC roadmap amendment](../a7-non-clinical-roadmap-amendment.md)
  historical Full A7 Rejoin Gate RG-01…RG-13
- [A7-NC scope register](../a7-non-clinical-scope-register.csv) CL-01 / CL-02
- [A6.5 legacy clinical non-adoption strategy](../a6-5/a6-5-legacy-clinical-non-adoption-strategy.md)
  §11–§14
- A6.5 Exit: `PASS_LEGACY_GOVERNANCE_ONLY`
- A7-NC: `COMPLETE` / `CLOSED` / `STABLE`
- [NC-CLOSE-07 Exit Record](../../../evidence/phase-a/nc-close-07/nc-close-07-exit-review-2026-08-18.md)
- `capabilities/adult_respiratory_v1` package governance
- [患者端证据与引用UI契约](../../../患者端证据与引用UI契约.md)

Historical PR #29 text is **preserved**. This charter records **current
effective** status. It does not delete or rewrite PR #29.

## 2A. Current-effective amendment identity

A6.5 non-adoption §13 superseded historical Full A7 Rejoin items
RG-01 / RG-02 and recorded:

```text
A7_ROADMAP_AMENDMENT_REQUIRED
```

for later current-effective Full A7 Rejoin / clinical-rejoin
semantics. Historical PR #29 remains `HISTORICAL_AUTHORITY_PRESERVED`.
It is not deleted and is not declared invalid.

Repository Owner later authorized this batch with:

```text
A7_CL_01_CLINICAL_GOVERNANCE_EXPLICIT_AUTHORIZATION_GRANTED
```

for Clinical Governance plus the Rejoin / Exit matrix.

Therefore A7-CL-01 is the Repository Owner-authorized
current-effective A7 roadmap / clinical-rejoin amendment required by
A6.5 non-adoption §13 (`A7_ROADMAP_AMENDMENT_REQUIRED`).

```text
A7_CL_01_SATISFIES_A7_ROADMAP_AMENDMENT_REQUIRED
THE_OWNER_AUTHORIZED_CURRENT_EFFECTIVE_A7_ROADMAP_AND_CLINICAL_REJOIN_AMENDMENT
```

This amendment preserves historical PR #29 text but, once reviewed
and merged, supersedes its current-effective execution ordering
where this charter and matrix explicitly identify it:

```text
HISTORICAL_AUTHORITY_PRESERVED
CURRENT_EFFECTIVE_EXECUTION_ORDER_SUPERSEDED_BY_A7_CL_01
```

Supersession is limited to RG-10 / RG-11 / RG-12 and related Prompt
registration / route eligibility / Capability binding / evaluation
sequencing. It does **not** supersede all PR #29 governance.

```text
A7_CL_01_CHANGES_ORDERING_NOT_SUBSTANTIVE_GATE_REQUIREMENTS
```

A7-CL-01 changes **WHEN** RG-10 / RG-11 / RG-12 evidence is produced
and consumed. It does **not** change **WHAT** those gates require.
The following remain mandatory before A7-CL / Full A7 completion and
are **not** waived:

- RG-10: Clinical Prompt review complete
- RG-11: Clinical route evaluation and fallback evidence complete
- RG-12: Capability binding lifecycle/release evidence complete

Until PR #53 merges, this identity remains:

```text
AUTHORED_PENDING_REVIEW
```

It is **not** durable current Enterprise authority.

## 3. Current durable state

```text
PHASE_A_NC_CLOSURE:           COMPLETE
PHASE_A_NC_CLOSURE_EXIT:      PASS
NC-CLOSE-07:                  COMPLETE
A6.5:                         LEGACY_GOVERNANCE_CLOSED
A7-NC:                        COMPLETE / CLOSED / STABLE
A7-CL:                        BLOCKED
A7:                           NOT_COMPLETE
FB-11:                        UNSATISFIED
A11:                          NOT_PASSED
Phase A Frozen Baseline:      NOT_READY_BLOCKED_CLINICAL
Clinical Runtime:             NOT_ENABLED
Production:                   BLOCKED
Phase B:                      NOT_AUTHORIZED
Clinical Owner:               UNASSIGNED
Human Clinical Reviewer:      UNASSIGNED
Content-access authorization: ABSENT / NOT_GRANTED
```

This batch does **not** upgrade those states.

Post-authoring control label:

```text
A7_CL_GOVERNANCE_DEFINED
+
A7_CL_CLINICAL_EXPERT_REVIEW_REQUIRED
```

`GOVERNANCE_DEFINED != A7-CL STARTED`.
`GOVERNANCE_DEFINED != A7-CL-02 AUTHORIZED`.

## 4. adult_respiratory_v1 scope

Phase A A7-CL is locked to `adult_respiratory_v1`.

Current package state remains:

```text
lifecycle:                  DRAFT
overall_evidence:           PARTIALLY_VALIDATED
clinical_review_status:     REQUIRES_CLINICAL_REVIEW
runtime_adoption:           NOT_IMPLEMENTED
production_eligibility:     BLOCKED
owner_status:               UNASSIGNED
```

No lifecycle upgrade in this batch. No all-disease generalization.

Explicit exclusions (package `population/scope.yaml` plus global
boundaries):

- pediatrics complete pathway
- pregnancy complete pathway
- open-ended all-disease diagnosis
- automatic diagnosis
- autonomous treatment
- prescription generation
- dosage change
- high-risk decision without clinician review
- real booking / referral
- emergency-care replacement
- unrestricted medical web retrieval
- real provider
- PHI / real patient data

Scope expansion requires `A7_CL_SCOPE_EXPANSION_REQUIRES_SEPARATE_REVIEW`.

## 5. Legacy non-migration rule

```text
LEGACY_CLINICAL_ASSETS_WILL_NOT_BE_MIGRATED
```

New clinical content must be `NEWLY_GOVERNED` / `NEWLY_REVIEWED` /
`NEWLY_VERSIONED`.

Forbidden:

- `MAPPED_FROM_LEGACY`
- `MIGRATED_FROM_LEGACY`
- `COPIED_FROM_REJECTED_ASSETS`
- B04 extraction
- C02 rejected-asset mapping

## 6. Role model

Define, do **not** fabricate identities.

| Role | Current state | Purpose |
|---|---|---|
| `CLINICAL_OWNER` | `UNASSIGNED` | Accountable owner of `adult_respiratory_v1` clinical content |
| `HUMAN_CLINICAL_REVIEWER` | `UNASSIGNED` | Independent clinical review of first-edition content |

Required future assignment evidence fields (all currently empty):

- `role`
- `person_identifier`
- `qualification_basis`
- `scope` (must be `adult_respiratory_v1` unless separately reviewed)
- `effective_date`
- `approval_authority`
- `independence_or_conflict_declaration`
- `artifact_or_version_coverage`
- `status` (`UNASSIGNED` / `ASSIGNED` / `REVOKED`)

Do not invent names, credentials, licenses, specialties, approval
dates, or signatures.

## 7. Content-access authorization

Gate: `CLINICAL_CONTENT_ACCESS_AUTHORIZATION`

Current state: `ABSENT` / `NOT_GRANTED`

No Safety / Question / Hypothesis / clinical Prompt content may be
authored under A7-CL until:

1. Clinical Owner is `ASSIGNED`;
2. Human Clinical Reviewer is `ASSIGNED`;
3. written content-access authorization is `GRANTED`.

This batch **does not** grant that authorization and **does not**
simulate a signature.

## 8. FB-11 definition

Supported current definition only:

```text
FB-11 = new clinical Safety / Question / Hypothesis
        first-edition baseline approval
```

Required first-edition families:

- `SAFETY`
- `QUESTION`
- `HYPOTHESIS`

`NOT_REQUIRED_BY_CURRENT_FB11_AUTHORITY`:

- clinical gold
- numeric thresholds as a separate FB-11 family
- Medical RAG implementation
- production Knowledge Release
- clinical Prompt text
- Phase B executable Safety Engine

Those items may appear later under A7-CL-03, Phase B, or A11. They are
not silently added to FB-11.

Current state: `UNSATISFIED`.

FB-11 is a `CONTENT_APPROVAL_GATE`. It is not satisfied by this
governance batch.

## 9. A7-CL CL-01 / CL-02 boundary

Existing scope (not activated here):

| ID | Meaning |
|---|---|
| CL-01 | Observation Extraction clinical activation |
| CL-02 | Question Wording clinical activation |

A7-CL-01 does **not** activate CL-01 or CL-02.

After pre-content gates, later batches may **register reviewed**:

- Prompt
- eligibility
- Capability binding
- evaluation evidence

Activation (`ELIGIBLE=true`, runtime adopt, production eligibility)
remains separately gated. Capability lifecycle stays `DRAFT` until a
later formal gate. Production remains `BLOCKED`.

## 10. Safety baseline vs Phase B Safety runtime

| Concept | Owner | This batch |
|---|---|---|
| A. FB-11 Safety first-edition **clinical baseline** (capability pack content) | future A7-CL-02 | governed, not authored |
| B. Phase B executable Safety Engine / Safety Pack **runtime** | Phase B B4 | `PHASE_B_DEFERRED` / `NOT_AUTHORIZED` |

A7-CL may later govern A. A7-CL-01 does **not** authorize B.

Mandatory Safety must remain deterministic and must not depend solely
on Medical RAG or LLM output (A7-NC amendment §9). That rule is
preserved; it is not implemented here.

## 11. Rejoin / Exit gate semantics

Historical RG-01…RG-13 remain recorded. Current effective roles:

| Gates | `gate_role` |
|---|---|
| RG-01 / RG-02 | `SUPERSEDED` |
| RG-03 / RG-04 / RG-13 | `SATISFIED_PREREQUISITE` |
| RG-05 | `SATISFIED_PREREQUISITE` as `SATISFIED_LEGACY_GOVERNANCE_ONLY` |
| RG-06 / RG-07 / RG-08 | `PRE_CONTENT_START_GATE` |
| RG-09 | `CONTENT_APPROVAL_GATE` (provenance for **selected** new content) |
| FB-11 | `CONTENT_APPROVAL_GATE` |
| RG-10 / RG-11 / RG-12 | `A7_CL_EXIT_GATE` |
| CL-01 / CL-02 | `A7_CL_EXIT_GATE` (activation evidence) |
| Full A7 Exit | consumes A7-CL Exit + satisfied/superseded prerequisites |
| remaining Frozen / Freeze Review | `A11_ONLY_GATE` |

### RG-10 / RG-11 / RG-12 reconciliation

PR #29 §7 / §15 say clinical Prompt **registration**, route
**eligibility**, Capability **binding**, and clinical model
**evaluation** cannot begin before the Full A7 Rejoin Gate, and they
list RG-10…RG-12 among the 13 mandatory items.

That historical ordering is circular if RG-10…RG-12 must be complete
**before** the content they review can be authored. Circularity
explains why A6.5 §13 recorded `A7_ROADMAP_AMENDMENT_REQUIRED`. It
does **not** itself amend PR #29. The current-effective ordering
below is authorized only by §2A
(`A7_CL_01_SATISFIES_A7_ROADMAP_AMENDMENT_REQUIRED`).

Current effective roles remain:

```text
RG-06 / RG-07 / RG-08 = PRE_CONTENT_START_GATE
RG-09                 = CONTENT_APPROVAL_GATE
FB-11 S/Q/H           = CONTENT_APPROVAL_GATE
RG-10 / RG-11 / RG-12 = A7_CL_EXIT_GATE
CL-01 / CL-02         = A7_CL_EXIT_GATE
```

`A7_CL_01_CHANGES_ORDERING_NOT_SUBSTANTIVE_GATE_REQUIREMENTS`.

“Cannot begin before Rejoin Gate” is read, for current-effective
execution only, as: cannot begin **activation** (eligible routes,
runtime adopt, production eligibility) before Owner / Reviewer /
written authorization / A6.5 legacy Exit / A7-NC Exit are in force.

It is **not** read as: Prompt review, route evaluation, and Capability
binding must already exist before the content they govern is authored.

Remaining wording tension in historical PR #29 is documented, not
rewritten (`HISTORICAL_AUTHORITY_PRESERVED`). Current-effective
execution ordering is superseded only as identified in §2A and the
matrix (`CURRENT_EFFECTIVE_EXECUTION_ORDER_SUPERSEDED_BY_A7_CL_01`).

## 12. A7-NC integrity

```text
A7-NC: COMPLETE / CLOSED / STABLE
```

No semantic change to ModelSpec, ModelRoutePolicy, Prompt Runtime
framework, Output Schema Registry, Gateway, ProviderAdapter, Fake,
blocked-shell, or legacy containment.

If later work needs such a change:

`A7_NC_SEMANTIC_REOPEN_REQUIRES_SEPARATE_ARCHITECTURE_REVIEW`

Shared Contracts `contracts/v1/**` semantic change:

`A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW`

## 13. Provider / PHI boundaries

```text
real provider = FORBIDDEN / 0
PHI = 0
real patient data = 0
Clinical Runtime = NOT_ENABLED
```

A7-CL / Full A7 evaluation evidence, if required, must use Fake /
blocked-shell / offline / synthetic material.

Real provider: `PHASE_A_CLINICAL_REAL_PROVIDER_SEPARATE_AUTHORIZATION_REQUIRED`
PHI: `PHASE_A_CLINICAL_PHI_SEPARATE_AUTHORIZATION_REQUIRED`

## 14. Future batch topology

Documented only. **Not created. Not authorized.**

| Batch | Goal |
|---|---|
| A7-CL-01 | this governance charter + matrix |
| A7-CL-02 | FB-11 Safety / Question / Hypothesis first edition |
| A7-CL-03 | reviewed clinical Prompt + CL-01/CL-02 binding + bounded Fake/offline evaluation |
| A7-CL-EXIT | independent clinical review + Full A7 Exit |

## 15. A7-CL-02 eligibility

A7-CL-02 is **not** authorized by this batch.

Required pre-content evidence before A7-CL-02 may be considered:

1. Clinical Owner `ASSIGNED`
2. Human Clinical Reviewer `ASSIGNED`
3. written `CLINICAL_CONTENT_ACCESS_AUTHORIZATION` `GRANTED`
4. `adult_respiratory_v1` scope remains locked
5. legacy non-migration remains in force
6. A7-NC remains `COMPLETE` / `CLOSED` / `STABLE`

If roles or authorization remain absent after A7-CL-01 merge:

```text
A7_CL_CLINICAL_EXPERT_REVIEW_REQUIRED
!=
A7_CL_02_AUTHORIZED
```

## 16. Stop conditions

- `A7_CL_01_BASE_DRIFT_STOPPED`
- `A7_CL_01_REJOIN_EXIT_AUTHORITY_AMBIGUITY_STOPPED`
- `A7_CL_SCOPE_EXPANSION_REQUIRES_SEPARATE_REVIEW`
- `A7_NC_SEMANTIC_REOPEN_REQUIRES_SEPARATE_ARCHITECTURE_REVIEW`
- `A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW`
- `PHASE_A_CLINICAL_REAL_PROVIDER_SEPARATE_AUTHORIZATION_REQUIRED`
- `PHASE_A_CLINICAL_PHI_SEPARATE_AUTHORIZATION_REQUIRED`
- `PHASE_B_IMPLEMENTATION_NOT_AUTHORIZED`
- `A11_FREEZE_REVIEW_NOT_AUTHORIZED`
- `A7_CL_CLINICAL_EXPERT_REVIEW_REQUIRED`

## 17. Non-claims

This batch does **not** claim:

- A7-CL STARTED
- A7-CL-02 AUTHORIZED
- FB-11 SATISFIED
- Full A7 Exit PASS
- A11 PASS / Frozen Baseline
- Clinical Runtime enabled
- production readiness
- Clinical Owner or Reviewer assigned
- content-access authorization granted
- any clinical rule, question, hypothesis, Prompt, threshold, or gold

Engineering-only preparation (schemas, validators, versioning,
Fake/offline harness, synthetic fixtures, evidence structures) may be
identified for later batches. It is **not** implemented here.
