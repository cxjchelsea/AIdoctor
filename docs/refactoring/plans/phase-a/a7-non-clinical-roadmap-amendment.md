# Phase A7-NC Non-Clinical Model Runtime Roadmap Amendment

> Amendment status: `MERGED_AND_VERIFIED`
>
> Merged Enterprise commit: `2c9dbf866c2c6b064f68fcab1f557635e64a73d2`
>
> Reviewed Head: `87e9780220e0fa219f1d0e0e91c2f06dea79b21d`
>
> PR: `#29`
>
> A7-NC implementation authorization: `NOT_GRANTED`
>
> A7-NC implementation: `NOT_STARTED`
>
> Amendment type: `IMPLEMENTATION_ORDER_AMENDMENT`
>
> Architecture Refreeze: `NOT_REQUIRED`

## 1. Background

The historical Phase A sequence is:

```text
A6 → A6.5 → A7
```

That sequence remains the historical baseline. The independent Non-Clinical A7 Roadmap Amendment Assessment concluded that the platform mechanics in A7 can be separated from clinical activation without changing frozen architecture. This document records the resulting planning exception; it does not implement or authorize Model Runtime.

## 2. Current blocker

The clinical governance lane remains fail-closed:

```text
TASK-B04: BLOCKED (6/11 unlock conditions satisfied)
Clinical Owner: NOT_ASSIGNED
Human Clinical Reviewer: NOT_ASSIGNED
Written content-access authorization: ABSENT
TASK-C02: BLOCKED_BY_TASK_B04
TASK-E01: NOT_ELIGIBLE
TASK-E02: NOT_ELIGIBLE
A6.5: INCOMPLETE_BLOCKED_DEPENDENCY
```

The nine B04 content-level assets remain `BLOCKED / NOT_STARTED`. This amendment does not create a B04 branch, owner, reviewer, authorization, clinical read, or Capability content.

## 3. Amendment rationale

An external clinical-governance dependency prevents B04 from starting, while Model Runtime structural contracts, registries, deterministic loading/building mechanics, gateway boundaries and offline provider abstraction do not require clinical content. A controlled parallel lane prevents unnecessary platform-planning delay while preserving every clinical gate.

The exception means:

```text
A6.5 clinical lane remains fail-closed.
A7 non-clinical platform mechanics may proceed in parallel only after separate authorization.
The exception does not satisfy A6.5 Exit.
The exception does not activate clinical A7.
The exception does not make A7 complete.
```

## 4. Historical roadmap

The normal path remains authoritative and is not deleted or rewritten:

```text
A1 → A2 → A3 → A4 → A5 → A6 → A6.5 → A7 → A8 → A9 → A10 → A11
```

The original authoritative A7 scope remains exactly these 14 items:

1. ModelSpec.
2. ModelRoutePolicy.
3. PromptSpec.
4. Prompt Registry.
5. Prompt Loader.
6. Prompt Builder.
7. Output Schema Registry.
8. Model Gateway.
9. ProviderAdapter interface.
10. `common/aidoctor_llm` Legacy Adapter.
11. Prompt YAML specification.
12. Provider SDK architecture/lint rule.
13. Observation Extraction initial route.
14. Question Wording initial route.

This dated amendment adds a temporary controlled execution-order exception because B04 is waiting on human clinical governance. If the exception is revoked, work returns to the normal path without changing architecture or clinical state.

## 5. Parallel roadmap

```text
Clinical Governance Lane

B04
  ↓
C02
  ↓
E01
  ↓
E02
  ↓
A6.5 Exit
  ↓
A7-CL eligibility

Non-Clinical Platform Lane

A7-NC Amendment
  ↓
separately authorized A7-NC platform implementation
  ↓
A7-NC Exit

A6.5 Exit + A7-NC Exit + clinical governance/review evidence
  ↓
A7-CL
  ↓
Full A7 Exit
```

## 6. A7-NC scope

The authoritative A7 scope is split in [the scope register](./a7-non-clinical-scope-register.csv). A7-NC contains:

1. `NC-01` ModelSpec structural contract: provider, model identifier, capabilities, context limits, structured-output capability, timeout-policy shape, retry class, cost-metadata shape, status and version. `CLINICALLY_APPROVED` is forbidden.
2. `NC-02` ModelRoutePolicy structural model: route matching, fallback topology, capability requirements, eligibility mechanics and provider/model selection structure. Clinical routes default to `BLOCKED` and `ELIGIBLE=false`.
3. `NC-03` PromptSpec: identifier, version, variables, output contract, checksum, metadata and status; no clinical body or medical instruction.
4. `NC-04` empty/non-clinical Prompt Registry.
5. `NC-05` deterministic Prompt Loader with explicit-version lookup, schema/hash validation and missing/invalid/checksum-mismatch behavior.
6. `NC-06` deterministic Prompt Builder with variable substitution, required-input validation, context-size handling and version manifest.
7. `NC-07` Output Schema Registry limited to existing Shared Contracts v1.
8. `NC-08` Model Gateway boundary for validation, lookup, selection mechanics, timeout/error normalization, result validation and provenance.
9. `NC-09` ProviderAdapter interface.
10. `NC-10` disabled-by-default `common/aidoctor_llm` mechanical compatibility facade.
11. `NC-11` Prompt YAML specification with synthetic non-clinical examples only.
12. `NC-12` architecture rule that new business code cannot call provider SDKs directly.
13. `NC-13` Observation Extraction route identifier and contract references with a default BLOCKED shell.
14. `NC-14` Question Wording route identifier and contract references with a default BLOCKED shell.
15. `SUP-01` DeterministicFakeProviderAdapter for offline validation.
16. `SUP-02` non-clinical synthetic deterministic fixtures owned by A7-NC scope.

## 7. A7-CL scope

`A7-CL` is `CLINICAL_ACTIVATION_DEPENDENT` and currently `BLOCKED_BY_A6_5_CLINICAL_LANE`.

- Observation Extraction clinical Prompt, clinical eligibility, Capability binding and clinical evaluation.
- Question Wording clinical Prompt, clinical eligibility, Capability binding and clinical evaluation.

Clinical Prompt registration, route eligibility, Capability binding and clinical model evaluation cannot begin before the Full A7 Rejoin Gate.

## 8. Explicit non-scope

This amendment and the future A7-NC lane exclude:

- application or runtime implementation in this planning change;
- clinical Prompt bodies, extraction instructions or question-wording templates;
- adult respiratory rules, thresholds, hypotheses or medical-source content;
- clinical diagnosis outputs, patient examples, PHI or real-patient fixtures;
- clinical gold labels or medical-correctness labels;
- Capability approval, activation, binding or runtime allowlist mutation;
- production eligibility or clinical Runtime enablement;
- real provider adapters, provider traffic, paid APIs or external model calls;
- Shared Contract semantic changes;
- direct clinical-state writes;
- fixtures, tests or lint rules in this planning PR.

## 9. Safety boundaries

Mandatory Safety, Triage, Consent, Permission and Capability policy remain deterministic and must not depend on a fake provider, real provider or LLM availability. Approved clinical rules, thresholds, hypotheses and medical sources remain zero. A7-NC cannot approve a model output or make a clinical decision.

## 10. Capability boundary

`adult_respiratory_v1` remains:

```text
lifecycle=DRAFT
overall_evidence=PARTIALLY_VALIDATED
clinical_review_status=REQUIRES_CLINICAL_REVIEW
runtime_adoption=NOT_IMPLEMENTED
production_eligibility=BLOCKED
```

Capability lifecycle mutation, Prompt reference activation, Model Route binding and Tool/Skill allowlist activation are prohibited. A7-NC implementation must produce `capabilities/** diff = 0`.

## 11. State ownership boundary

Every future `ModelInvocationResult` is candidate/draft data. It cannot directly write `EncounterCDP`, `ClinicalObservation` or `TriageAssessment`. Deterministic validation and the unique State Committer clinical-write path remain mandatory. No State Committer, Mandatory Safety or Triage decision moves into Model Runtime.

## 12. Real-provider boundary

```text
A7-NC real provider: FORBIDDEN
network model calls: 0
paid API calls: 0
external model calls: 0
```

Even after the Full A7 Rejoin Gate, a real provider requires separate provider authorization, privacy and external-access review, cost policy, quality evaluation, fallback policy and secret management.

## 13. Fixture policy

Future A7-NC fixtures must be non-clinical, synthetic, deterministic, free of PHI/patient data, free of clinical gold and free of medical-correctness labels. Suitable semantic examples include `summarize_demo_text`, `classify_color` and `echo_structured_input`.

These fixtures are new A7-NC-owned test inputs. They must not be described as `DATA-EV001`, `DATA-EV002` or `DATA-EV003` and do not change D01 historical inventory facts.

## 14. A7-NC exit gate

All 24 numbered conditions and the retained-boundary condition are mandatory:

1. ModelSpec structural validation passes.
2. PromptSpec structural validation passes.
3. ModelRoutePolicy structural validation passes.
4. Prompt Registry is empty or non-clinical-only.
5. Prompt Loader explicit-version behavior is deterministic.
6. Prompt Builder rendering is deterministic.
7. Output Schema Registry uses existing Shared Contracts v1 only.
8. Shared Contracts semantic diff equals zero.
9. Model Gateway contract validation passes.
10. DeterministicFakeProvider has no network path.
11. Fake-provider responses are fixture-defined.
12. Failure, timeout and invalid-output injection work deterministically.
13. Provider SDK architecture rule passes.
14. Legacy Adapter is disabled by default and produces no provider traffic.
15. Observation Extraction remains a default BLOCKED shell only.
16. Question Wording remains a default BLOCKED shell only.
17. Capability diff equals zero.
18. Capability runtime references remain zero.
19. Activated clinical-content count equals zero.
20. Activated clinical-Prompt count equals zero.
21. Patient-data/PHI count equals zero.
22. Clinical-gold count equals zero.
23. External model/API call count equals zero.
24. Independent review confirms `A7-NC COMPLETE != A7 COMPLETE`.

Retained boundary gate: State Committer, Mandatory Safety, Triage, Permission and Consent remain unchanged.

## 15. Full A7 rejoin gate

All 13 conditions are mandatory:

1. TASK-B04 is `COMPLETE`.
2. TASK-C02 is `COMPLETE`.
3. TASK-E01 is `COMPLETE`.
4. TASK-E02 is `COMPLETE`.
5. A6.5 Exit is `PASSED`.
6. Clinical Owner evidence exists.
7. Human Clinical Reviewer evidence exists.
8. Written clinical content-access authorization exists.
9. Provenance, license and privacy governance is complete for selected clinical content.
10. Clinical Prompt review is complete.
11. Clinical route evaluation and fallback evidence is complete.
12. Capability binding passes its formal lifecycle/release gate.
13. A7-NC Exit is `PASSED`.

Only then may clinical Prompt registration, clinical route eligibility, clinical Capability binding and real clinical model evaluation be considered.

## 16. Stop conditions

Stop A7-NC immediately if it needs a clinical Prompt body, B04 output, C02 clinical mapping, patient data/PHI, clinical gold, an external provider for correctness, Capability activation, Shared Contract semantic mutation, eligible clinical routes, non-zero approved clinical counts, legacy clinical Prompt reads, legacy provider traffic, direct clinical-state writes, a changed State Committer/Safety/Triage boundary, a business-code provider SDK bypass, or any frozen-architecture change.

Use `A7_NC_CONTRACT_CHANGE_REQUIRES_SEPARATE_REVIEW` for a proposed Shared Contract semantic change. A frozen-architecture change requires architecture review and refreeze rather than this amendment.

## 17. Status vocabulary

```text
A7-NC: NOT_STARTED | AUTHORIZED | IN_PROGRESS | COMPLETE
A7-CL: BLOCKED_BY_A6_5_CLINICAL_LANE
A7 overall: NOT_COMPLETE
```

Current control state after verified merge:

```text
A7-NC amendment planning: COMPLETE
A7-NC Roadmap Amendment: MERGED_AND_VERIFIED
A7-NC implementation authorization: NOT_GRANTED
A7-NC implementation: NOT_STARTED
A7-CL: BLOCKED_BY_A6_5_CLINICAL_LANE
A7: NOT_COMPLETE
Clinical Runtime: NOT_ENABLED
Production: BLOCKED
```

Planning completion is not implementation authorization. Even if A7-NC later becomes `COMPLETE`, A7 remains `NOT_COMPLETE` until A7-CL and Full A7 Exit pass.

## 18. Rollback and revoke conditions

The amendment is revoked and the repository returns to the normal A6.5-before-A7 order if:

- an A7-NC stop condition is encountered;
- independent review rejects separation or finds clinical leakage;
- implementation requires frozen-architecture or Shared Contract semantic changes;
- safety truth, ownership boundaries or approved clinical counts move unexpectedly;
- a future governance decision withdraws the controlled exception.

Revocation means stopping unmerged A7-NC work and preserving evidence. It does not authorize reset, destructive deletion, clinical activation or rewriting historical documents.

## 19. Relationship to A6.5

This is a controlled parallel execution exception, not an A6.5 bypass, waiver or completion claim. A6.5 remains `INCOMPLETE_BLOCKED_DEPENDENCY`; B04 and C02 remain fail-closed. The A6.5 backlog remains a historical planning artifact and is not rewritten by this amendment.

## 20. Relationship to D01

D01 remains historical planning evidence and does not authorize A7-NC fixtures. Its verified truth remains:

```text
targets=6
inventory_rows=21
EXISTING=0
synthetic_fixture=0
harness=0
Synthetic Regression=NOT_EXECUTED
DATA-EV001..003=ABSENT
```

## 21. Implementation batches proposal

This is a future proposal only; no batch is authorized here.

| Batch | Planned scope | Dependency |
|---|---|---|
| A7-NC-P1 | ModelSpec, ModelRoutePolicy and PromptSpec structures | separate implementation authorization |
| A7-NC-P2 | Prompt Registry, Loader and Builder | P1 |
| A7-NC-P3 | Output Schema Registry and Model Gateway | P2 |
| A7-NC-P4 | ProviderAdapter and DeterministicFakeProvider | P3 |
| A7-NC-P5 | disabled Legacy Adapter boundary and provider-SDK architecture rule | P4 |
| A7-NC-P6 | two BLOCKED route shells and integrated deterministic validation | P4; P5 may proceed in parallel where independent |
| A7-NC-P7 | independent review and A7-NC Exit Gate | P5 and P6 |

The dependency shape is `P1 → P2 → P3 → P4 → P5/P6 → P7`. Implementations must use bounded reviewable changes rather than one large PR.

## 22. Review and authorization process

The amendment followed this completed governance history:

1. Planning completed with the historical status `PLANNED_PENDING_INDEPENDENT_REVIEW`.
2. Independent Review passed against reviewed Head `87e9780220e0fa219f1d0e0e91c2f06dea79b21d`.
3. A separate Amendment Merge Review passed against the same reviewed Head.
4. PR #29 merged through standard merge commit `2c9dbf866c2c6b064f68fcab1f557635e64a73d2`; graph, tree and exact six-file scope were post-merge verified.
5. A separate A7-NC Implementation Authorization Assessment may now be considered, but authorization remains `NOT_GRANTED`.

The planning PR remained Draft until Independent Review passed and was subsequently reviewed and merged as PR #29. Neither that merge nor this status reconciliation grants Model Runtime implementation authorization or permits A7-CL, B04 or C02 to start.
