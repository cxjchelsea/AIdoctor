# Phase A6 adult_respiratory_v1 Capability Package Skeleton Report

## Status and scope

`PARTIALLY_VALIDATED`

A6 establishes a versioned, parseable, schema-governed, review-gated Capability
Package skeleton for `adult_respiratory_v1`. It does not approve clinical
thresholds or rules, does not adopt runtime, does not implement State Committer,
does not start A6.5 legacy-asset validation, and does not start A7 Model Runtime.

- Base branch: `agent/enterprise-agent-refactoring-plan`
- Base SHA: `f406fec3d6c3159f1f229bbb41113117a34a31e3`
- Head branch: `agent/phase-a6-adult-respiratory-capability-skeleton`
- Head SHA: `9377f8424e38edf254307d5e06ee96cfb49c76f5`
- Package lifecycle: `DRAFT`
- Clinical content: `REQUIRES_CLINICAL_REVIEW`
- Runtime adoption: `NOT_IMPLEMENTED`
- Production eligibility: `BLOCKED`
- Overall evidence: `PARTIALLY_VALIDATED`
- Shared Contracts binding: exact `1.0.0` (language-neutral schemas only)

## Delivered package

| Category | Count | Notes |
|---|---:|---|
| Capability schemas | 11 | Draft 2020-12, closed objects, bounded strings/arrays |
| Package YAML assets | 20 | population/terminology/observations/safety/questions/hypotheses/knowledge/runtime/delivery |
| Synthetic JSONL eval suites | 5 | structural fixtures only |
| Manifest asset references | 25 | path + schema_id + sha256 closed |
| Approved clinical rules | 0 | intentionally empty and blocked |
| Approved knowledge sources | 0 | retrieval eligibility blocked |
| Runtime allowlist references | 0 | NOT_IMPLEMENTED / BLOCKED |

Normative-to-actual path mapping uses package directory names:
`population`, `terminology`, `observations`, `safety`, `questions`,
`hypotheses`, `knowledge`, `runtime`, `delivery`, and `evals`.

## Enforcement boundaries

| Area | SCHEMA_ENFORCED | VALIDATOR_ENFORCED | Remaining |
|---|---|---|---|
| Manifest/assets | required governance fields, exact IDs/versions | path existence, checksum, completeness, no `latest` | owner assignment and release workflow later |
| Safety/Hypothesis/Knowledge | closed pack structures and fail-closed defaults | empty/unapproved packs cannot be ACTIVE or production eligible | clinical rule authoring and evaluation later |
| Runtime allowlists | reference object shape | nonexistent Tool/Skill/Prompt/Model paths rejected; eligibility blocked | A7 registry and permission runtime later |
| Knowledge domains | Patient/Medical domain consts | mixed index forbidden; zero sources => blocked retrieval | licensed Knowledge Release later |
| Delivery | PatientDelivery `1.0.0` binding and prohibited fields | runtime not implemented; free-text remains later | Patient API/UI and content safety later |
| Eval fixtures | synthetic-only case schema | unique case IDs; real-patient classification rejected | clinical eval suites later |

`POLICY_ENFORCED_LATER` covers clinical thresholds, triage mapping, red-flag
recall, source licensing conclusions, free-text patient wording safety, and
runtime authorization.

## Executable evidence

Commands used in an isolated virtualenv installed only from
`capabilities/requirements-test.lock.txt`:

```text
pip check
python capabilities/validator/validate_capability.py
python -m pytest capabilities/tests/test_capability.py -v
```

Observed local results:

- `pip check`: no broken requirements
- Validator: `A6 CAPABILITY VALIDATION PASSED: 11 schemas, 25 assets, 5 eval cases, 0 issues`
- Pytest: `37 passed`
- No CI workflow is configured for this package in A6 (`NO_CI_CONFIGURED`)

Validation proves package structure, parseability, schema closure, reference
closure, checksum integrity, lifecycle gates, synthetic eval classification,
and Patient/Medical knowledge separation. It does not prove clinical
correctness, red-flag recall, triage thresholds, model performance, runtime
authorization, patient workflow, or production readiness.

## Clinical governance snapshot

- approved clinical rules: `0`
- approved safety thresholds: `0`
- approved hypotheses: `0`
- approved knowledge sources: `0`
- real patient data in package: `none`
- production eligibility: `BLOCKED`
- runtime eligibility: `NOT_IMPLEMENTED` / `BLOCKED`

## Explicit non-claims

- A5 language bindings (Java/Python/TypeScript) remain unfinished and are an
  A11 Freeze Review prerequisite; they are not part of A6.
- A6.5 legacy design asset validation is not started.
- A7 Model Runtime is not started.
- No Frozen Baseline upgrade is claimed.
- No service, API, database, Prompt, RAG index, or patient UI was modified.

## Evidence artifacts

- [capability-package-skeleton-report.md](./capability-package-skeleton-report.md)
- [capability-asset-manifest.csv](./capability-asset-manifest.csv)
- [capability-review-gap-register.csv](./capability-review-gap-register.csv)
- [capability-validation-matrix.csv](./capability-validation-matrix.csv)

## Remaining blockers

- Clinical/policy approval for adult age boundary, safety rules, questions,
  hypotheses, and knowledge sources.
- Runtime registry and Model Runtime implementation (A7).
- Shared Contract language bindings and cross-language contract tests (A11).
- PatientDelivery API/UI, authorization, and free-text content safety.
- Legacy asset validation (A6.5) before any clinical rule promotion.
