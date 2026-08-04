# Phase A5 Shared Contracts v1 Report

## Status

`PARTIALLY_VALIDATED`

A5 establishes and tests a language-neutral contract package. It does not
migrate services, generate language bindings, implement State Committer or
Patient UI, change APIs/databases, or validate clinical behavior.

## Base and scope

- Base branch: `agent/enterprise-agent-refactoring-plan`
- Base SHA: `0cf6c687928b44192c505f2d140d7f061e2bd8c4`
- Head branch: `agent/phase-a5-shared-contracts-v1`
- Head SHA: recorded from Git/PR metadata after commit; embedding a commit's own SHA
  in that commit is self-referential and therefore intentionally avoided.
- Contract version: `1.0.0`
- Schema dialect: JSON Schema Draft 2020-12
- Canonical manifest: `contracts/v1/manifest.json`

The repository roadmap originally mentions generated Java/Python/TypeScript
models. The controlling A5 task explicitly excludes language bindings, so this
change provides only JSON Schema, fixtures, validator/tests and evidence. The
deferred binding work remains a recorded gap, not an implied completion.

## Delivered contracts

The manifest maps 13 unique names and IDs exactly to 13 schema files:
ContractEnvelope, IdentifierSet, StatePatch, CommitResult, ContractConflict,
ToolContext, ToolResult, EvidencePack, SourceArtifact, KnowledgeReleaseRef,
TraceRef, AuditRef and PatientDeliveryView. Claim and SourceSpan are governed
inside EvidencePack.

Common rules include explicit versioning, opaque string identifiers, RFC 3339
timestamps, closed enums, unknown-field rejection and bounded named structures.
StatePatch carries caller `base_version`, idempotency identity and a
transitional writable-domain allowlist; it excludes version, ID, audit and trace
system fields by construction. Tool arguments/output no longer use an arbitrary
dictionary escape hatch.

PatientDeliveryView is a strict patient-facing whitelist. It contains summarized
claims, source summaries, limitations, safety notices, next steps and explicit
delivery/contract/CDP/review/knowledge version bindings. It contains no full CDP,
raw result, reasoning path, numeric confidence, tool/provider payload or internal
hypothesis field.

## Executable evidence

- 13 positive fixtures: one for every core schema.
- 26 negative fixtures: two for every core schema.
- Every negative fixture records the expected validator, JSON Pointer path,
  error category and human-readable reason.
- Validator checks the Draft 2020-12 meta-schema, exact manifest/file mapping,
  unique IDs, resolvable local references, positive/negative fixtures and error
  metadata.
- Tests cover committed-version ordering, non-committed result rules, medical
  citations or insufficient evidence, Safety Rule independence, withdrawal,
  patient version bindings, timestamp ordering, auth-scope non-escalation,
  StatePatch protected paths, patient-forbidden fields, duplicate JSON keys,
  unbounded schema values, fictional-patient identifier shapes and secret-like
  content.

## Validation commands and results

Validation ran in the isolated temporary virtual environment
`AIdoctor-a5-contracts-v1-20260804` with Python 3.13.5,
`jsonschema[format]` 4.25.1 and pytest 8.4.1. No repository virtual environment
or generated cache was committed.

    python contracts/v1/validator/validate_contracts.py
    A5 CONTRACT VALIDATION PASSED: 13 schemas, 13 valid fixtures, 26 invalid fixtures

    python -m pytest -p no:cacheprovider contracts/v1/tests -q
    35 passed in 0.38s

    git diff --check
    PASS

Validation is structural and rule-boundary evidence only. It does not constitute
service runtime, database, model-provider, patient workflow, clinical validation
or production-readiness evidence.

## Evidence basis

- A1: Java ToolContext/ToolResult DTOs, CDP writers, version and Trace/Audit paths.
- A2: 156 Pydantic definitions, repeated Tool contracts, arbitrary Any/dict
  fields, eight readers, two direct writers and eight proposal producers.
- A3: frontend contract drift and patient-delivery forbidden-field gaps.
- A4: identifier/schema drift, lifecycle gaps, source provenance, knowledge
  release/withdrawal and Trace/Audit/PHI separation requirements.

See [coverage matrix](./contract-coverage-matrix.csv),
[legacy mapping](./legacy-contract-mapping.csv) and
[risk register](./contract-risk-register.csv).

## Key decisions and compatibility

- JSON Schema is the only normative v1 source; bindings are deferred.
- Boundary identifiers are opaque strings, including decimal-string forms of
  existing Long IDs. Database keys are unchanged.
- UTF-8 JSON, snake_case names, no duplicate keys, explicit nullability and
  timezone-bearing RFC 3339 timestamps are required.
- Unknown fields and enum values fail closed. Optional-field or enum additions
  require a minor version and consumer review; removals, renames, new required
  fields and narrowed representations require a major version.
- StatePatch is an atomic proposal. CommitResult has no partial-success state;
  COMMITTED advances the version and other states cannot expose committed
  version/time.
- Clinical thresholds, approval permissions, retention, consent, jurisdiction
  and licensing are not decided. `POLICY_REQUIRED` references preserve those
  blockers without inventing policy.

## Patient whitelist and prohibited content

PatientDeliveryView permits only delivery/CDP/review/version references,
patient-facing summary, policy-referenced safety notices, recommended actions,
evidence cards, limitations and follow-up. Schemas/tests reject Prompt,
provider responses, private reasoning/chain-of-thought, unapproved hypotheses,
internal policy, full CDP, other-patient data, internal Trace, raw Tool I/O,
raw result, reasoning paths and numeric confidence.

## Follow-up migration order

1. Independently review and approve the language-neutral package.
2. In a separately authorized phase, generate bindings and add cross-language
   serialization tests without hand-maintained competing sources.
3. Add compatibility adapters and consumer contract tests; do not switch
   writers yet.
4. Implement and validate State Committer, authorization and audit linkage.
5. Migrate Tool/CDP consumers and only then remove direct writers.
6. Implement knowledge release enforcement and the reviewed PatientDelivery
   API/UI boundary.

## Remaining blockers and out of scope

- No service consumes these contracts yet.
- No Java, Python or TypeScript binding exists.
- No State Committer, role authorization, release registry or PatientDelivery
  API/UI is implemented.
- No A6 work, API migration, database migration or clinical rule change is made.
- Existing A1-A4 blockers and `PARTIALLY_VALIDATED` conclusions remain unchanged.
