# Shared Contracts v1

`contracts/v1` is the language-neutral source of truth for Phase A5. It defines
transport and governance boundaries; it does not migrate services, implement
runtime authorization, or claim clinical correctness.

## Layout

- `manifest.json` is the exact contract-name, exact-version schema ID and file mapping.
- `schemas/` contains JSON Schema Draft 2020-12 documents.
- `fixtures/valid/` contains one canonical valid object for every core Schema.
- `fixtures/invalid/foundation-cases.json` and
  `fixtures/invalid/interaction-cases.json` embed the expected validator, JSON
  Pointer path, category and reason in every negative case.
- `validator/validate_contracts.py` validates structural and explicit semantic rules.
- `tests/` contains state matrices, adversarial boundaries and policy-layer classification.

## Enforcement layers

- `SCHEMA_ENFORCED`: closed structures, required fields, exact envelope names,
  state-dependent fields, basic JSON Pointer domains, bounds and formats.
- `VALIDATOR_ENFORCED`: normalized StatePatch tokens, parent/array/dot rejection,
  cross-object evidence references, time ordering, SourceArtifact normalization,
  authorization-scope subsets and direct lineage self-reference.
- `DOCUMENTED_ONLY`: migration mappings and legacy behavior not yet adopted by services.
- `POLICY_ENFORCED_LATER`: clinical thresholds, permissions, retention, consent,
  licensing and semantic inspection of patient-facing free text.

StatePatch validation is not field authorization. A5 permits only specific
proposal domains and rejects known protected tokens after canonicalization. A
future State Committer must use Capability/Policy Registry rules for exact field
authorization.

PatientDeliveryView rejects unknown structured fields at every object boundary.
JSON Schema cannot determine whether ordinary strings contain a prompt, provider
response, private reasoning, unapproved hypothesis, prescription or dosage.
Those content checks remain `POLICY_ENFORCED_LATER`; A5 fixtures contain no such
patient content and A5 does not claim semantic filtering.

## Exact-version compatibility

The package supports exact version `1.0.0`. Schema IDs include `1.0.0`, every
payload declares the exact `contract_version`, and the manifest selects exactly
that version. Unknown versions fail closed.

Adding an optional field or enum member may be classified as a minor Schema
release, but it is not automatically wire-compatible with an older strict
validator. A producer must not send `1.1.0` to a consumer that only declares
`1.0.0`. Consumer support, adapters and runtime negotiation are later work.
Removing/renaming a field, adding a required field or narrowing an accepted
representation requires a major release. Deprecation retains the field until a
major removal. Display text is never a stable enum value.

All identifiers are opaque bounded strings. Existing numeric database IDs cross
the boundary as decimal strings; A5 does not migrate database keys. Cross-language
JSON integers are bounded to JavaScript's safe integer maximum
`9007199254740991` (or a stricter domain maximum such as artifact size) for
Java/Python/TypeScript interoperability.

Serialization is UTF-8 JSON with snake_case names, no duplicate object keys and
null only where explicitly allowed. Timestamps are RFC 3339 date-time strings
with timezone. Unknown fields and enum members are rejected. Named bounded
structures replace arbitrary dictionaries.

## Validation

Create a new isolated environment using the complete lock:

    python -m pip install -r contracts/v1/requirements-test.lock.txt
    python -m pip check
    python contracts/v1/validator/validate_contracts.py
    python -m pytest -p no:cacheprovider contracts/v1/tests -q

`requirements-test.txt` records the two direct requirements; the lock pins their
complete resolved test graph. These are contract-test dependencies only.

Validation proves structural conformance and the explicitly tested semantic
boundaries. It is not service-runtime, database, model-provider, patient-workflow,
clinical-validation or production-readiness evidence.
