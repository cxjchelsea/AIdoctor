# Shared Contracts v1

contracts/v1 is the language-neutral source of truth for Phase A5. It defines
transport and governance boundaries; it does not migrate services or claim
clinical correctness.

## Layout

- manifest.json is the exact contract-name, schema-ID and file mapping.
- schemas/ contains JSON Schema Draft 2020-12 documents.
- fixtures/valid/ and fixtures/invalid/ contain executable examples.
- fixtures/invalid/expectations.json records the expected validator, path and
  reason for every negative fixture.
- validator/validate_contracts.py validates the complete package.
- tests/ adds semantic and security-boundary checks that JSON Schema alone
  cannot express.

## Compatibility

The package version is 1.0.0. Adding an optional field or enum member requires
a minor release and consumer review. Removing/renaming a field, making an
optional field required, changing an identifier/timestamp representation, or
narrowing an accepted value requires a major release. Patch releases may only
clarify documentation or constraints without changing accepted instances.

All identifiers are opaque strings. All timestamps are RFC 3339 date-time
strings. Unknown fields and unknown enum members are rejected. Contract payloads
use named, bounded structures instead of ungoverned dictionaries.

Serialization is UTF-8 JSON with snake_case property names, JSON booleans and
numbers, no duplicate object keys, and null only where the schema explicitly
allows it. Existing numeric database identifiers cross this boundary as decimal
strings; A5 does not change database key types. Message negotiation uses the
exact `contract_version`; a consumer that does not support its major version
must fail closed instead of coercing the payload.

New optional fields require a minor version. Deprecation first marks a field as
deprecated in schema/documentation while retaining validation compatibility;
removal is a major change. Enum members are closed: an unknown member is rejected,
and adding one requires a minor release plus consumer compatibility review.
Display text is never used as a stable enum value.

Clinical thresholds, risk levels, approval authority, retention durations,
jurisdiction, consent and source licensing remain policy/semantic blockers.
Schemas use explicit references such as `POLICY_REQUIRED` where a structural
field is necessary and do not invent those decisions.

## Validation

    python -m pip install -r contracts/v1/requirements-test.txt
    python contracts/v1/validator/validate_contracts.py
    python -m pytest contracts/v1/tests -q

The validator dependency is pinned. Validation proves structural conformance
and the explicitly tested semantic boundaries only; it is not service-runtime
or clinical validation.
