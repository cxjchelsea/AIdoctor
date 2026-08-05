# Phase A5 Shared Contracts v1 Report

## Status and scope

`PARTIALLY_VALIDATED`

A5 provides a language-neutral JSON Schema package, fixtures, validator/tests
and evidence. It does not migrate services, generate language bindings, change
APIs/databases, implement State Committer or Patient UI, decide clinical policy,
or begin A6.

- Base: `agent/enterprise-agent-refactoring-plan` at `0cf6c687928b44192c505f2d140d7f061e2bd8c4`
- Head: `agent/phase-a5-shared-contracts-v1`
- Contract/Schema selection: exact `1.0.0`
- Dialect: JSON Schema Draft 2020-12
- Canonical manifest: `contracts/v1/manifest.json`

## Delivered package

The manifest maps 13 unique contract names to 13 exact-version `$id` values and
13 files. Claim and SourceSpan remain governed definitions inside EvidencePack.
All named objects are closed and all instance strings/arrays are bounded.
Cross-language integers use at most `9007199254740991`, with stricter limits
where the domain already has one.

The complete contract-test graph is pinned in
`contracts/v1/requirements-test.lock.txt`; it does not alter service runtime
dependencies.

## Enforcement boundaries

| Area | SCHEMA_ENFORCED | VALIDATOR_ENFORCED | Remaining |
|---|---|---|---|
| Envelope | required bounded fields and exact outer contract name | none | service adoption later |
| StatePatch | legal Pointer shape, proposal domain and leaf depth | decoded/canonical tokens, protected fields, dot/array/control/confusable rejection | exact field authorization by future State Committer/Policy Registry |
| Commit/Tool outcomes | required/forbidden state fields, reason/error/retry matrices | version and time ordering | runtime concurrency/idempotency/tool execution later |
| EvidencePack | bounded typed claims/sources and duplicate array items | ID uniqueness, citation/conflict resolution, source-type matrix and validity order | source quality, licensing and clinical review later |
| SourceArtifact | approved logical schemes, host path/dot rejection, unique derived IDs | decoded traversal and direct self-reference | multi-node cycle and resolver safety in registry/storage layer |
| KnowledgeRelease | status-dependent fields | self-reference/time order and withdrawn-delivery gate | runtime release registry later |
| PatientDelivery | closed structured Source/Conflict/Applicability/Limitation and review/display fields | cross-card references and knowledge bindings | free-text content safety and authorization later |

`DOCUMENTED_ONLY` covers legacy mappings and migration decisions.
`POLICY_ENFORCED_LATER` covers clinical thresholds, permissions, retention,
consent, licensing, and semantic inspection of patient-facing strings.

## PatientDelivery UI mapping

| Reviewed UI concept | v1 representation | Decision |
|---|---|---|
| Delivery states including limited/degraded | `delivery_status` closed enum | KEEP/ADAPT |
| clinician reviewed/time | `clinician_reviewed`, `reviewed_at` | KEEP |
| display priority | `display_priority` | KEEP |
| PatientSourceSummary | structured `sources[]` | KEEP/ADAPT to snake_case |
| PatientConflictSummary | structured `conflict_summary` | KEEP/ADAPT |
| PatientApplicabilitySummary | structured `applicability` | KEEP/ADAPT |
| PatientLimitation | structured `limitations[]` | KEEP/ADAPT |
| Safety Notice | structured policy-referenced notices | KEEP/ADAPT |
| Medical/Clinician/Patient basis | `basis_type` | KEEP |
| follow-up | bounded strings | KEEP; semantic content policy later |
| Delivery/CDP/Review/Knowledge versions | `version_bindings` | KEEP |
| PatientSummaryBlock/RiskBlock implementation | title/summary plus structured cards/notices | ADAPT; API/UI implementation deferred |

Schema rejects forbidden *fields* recursively because every object is closed.
It cannot understand whether `summary`, `recommended_actions`, `follow_up` or
other ordinary strings contain Prompt text, provider output, private reasoning,
unapproved hypotheses, prescriptions or dosage changes. Those checks are
`POLICY_ENFORCED_LATER`; A5 does not claim semantic content filtering.

## Exact-version compatibility

Every payload declares an exact version and consumers validate using the exact
matching Schema ID. Unknown `1.1.0` fails under the current manifest. Optional
field or enum additions may be a minor release classification, but are not
automatically wire-compatible with a strict 1.0.0 consumer. Runtime negotiation,
adapters and consumer support declarations remain later work.

## Executable evidence

- 13 schemas and 13 canonical valid fixtures.
- 33 structural invalid fixtures with expected validator/path/category/reason.
- 107 tests covering meta-schema, manifest/ref integrity, duplicate keys,
  state matrices, adversarial paths, Evidence cross-rules, Patient UI structures,
  artifact traversal, exact versions and integer boundaries.
- Direct and transitive dependencies pinned; `pip check` required.

Validation proves only the listed structural and semantic boundaries. It is not
service runtime, database, provider, patient workflow, clinical validation or
production-readiness evidence.

## Evidence basis and remaining blockers

- A1–A4 Java/Python/frontend/data inventories remain the legacy evidence basis.
- Existing direct CDP writers, arbitrary legacy maps, identifier drift and PHI
  trace/audit payloads are unchanged.
- No service consumes these contracts; no binding, State Committer, release
  registry, authorization gate or PatientDelivery API/UI exists.
- Global SourceArtifact lineage-cycle checks and storage resolver normalization
  remain registry/storage responsibilities.
- Existing A1–A4 blockers and `PARTIALLY_VALIDATED` conclusions are unchanged.

See [coverage matrix](./contract-coverage-matrix.csv),
[legacy mapping](./legacy-contract-mapping.csv) and
[risk register](./contract-risk-register.csv).
