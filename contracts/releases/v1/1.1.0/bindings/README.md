# Shared Contracts v1.1.0 Language Bindings

> Classification: `REVIEWED_BINDING` / `TYPES_AND_TESTS_ONLY`
>
> Semantic authority: `contracts/releases/v1/1.1.0/manifest.json` + `contracts/releases/v1/1.1.0/schemas/**`
>
> Semantic oracle: `contracts/releases/v1/1.1.0/validator/validate_contracts.py`
>
> Version: `1.1.0` / `EXACT`

These bindings belong to the self-contained A5-PB-01 exact release. They do not migrate Java Feign clients, legacy FastAPI models, frontend runtime consumers, State Committer runtime behavior, Clinical Runtime, or production wiring.

```text
BINDING_CREATED
!=
STATE_COMMITTER_1_1_RUNTIME_MIGRATED
!=
LEGACY_SERVICE_MIGRATED
!=
FRONTEND_RUNTIME_MIGRATED
```

## Strategy

1. The release-local JSON Schema and manifest are the semantic source.
2. The release-local Python validator is the semantic oracle for validator-owned cross-field/path rules.
3. Java, Python, and TypeScript bindings are reviewed structural artifacts.
4. Fixtures round-trip to canonical JSON where applicable.

The bindings do not replace schema validation or the release-local semantic validator.

## Layout

```text
contracts/releases/v1/1.1.0/bindings/
  README.md
  java/          com.aidoctor:shared-contracts:1.1.0
  python/        aidoctor_shared_contracts
  typescript/    @aidoctor/shared-contracts
  tooling/       sync_version.py, check_drift.py
```

## Commands

From repository root, after installing contract-test dependencies:

```text
python contracts/releases/v1/1.1.0/validator/validate_contracts.py
python -m pytest -p no:cacheprovider contracts/releases/v1/1.1.0/tests contracts/releases/v1/1.1.0/bindings/python/tests -q
python contracts/releases/v1/1.1.0/bindings/tooling/check_drift.py
mvn -f contracts/releases/v1/1.1.0/bindings/java/pom.xml test
npm --prefix contracts/releases/v1/1.1.0/bindings/typescript test
```

`sync_version.py` writes generated version stamps from the release-local manifest. Do not run it as an independent prose or semantic edit.

## 16 contracts

ContractEnvelope, IdentifierSet, StatePatch, CommitResult,
ContractConflict, ToolContext, ToolResult, EvidencePack,
SourceArtifact, KnowledgeReleaseRef, TraceRef, AuditRef,
PatientDeliveryView, Encounter, ClinicalStateSnapshot,
ClinicalObservation.

ObservationCandidate, BusinessEncounterView, and AgentEvent remain deferred.

## Binding limits

The Java binding provides typed `ClinicalObservation.value` variants and typed StatePatch legacy/canonical operation branches. The Python and TypeScript bindings provide useful structural types, but cross-field rules such as target XOR, canonical path identity, and Encounter timestamp ordering remain validator-owned.

## Dependencies

| Area | Classification |
|---|---|
| Jackson 2.13.5 (binding module only) | BUILD_TIME_ONLY |
| JUnit 4.13.2 | TEST / DEV_ONLY |
| Pydantic 2.10.3 / pytest / jsonschema | DEV_ONLY |
| TypeScript 5.2.2 | DEV_ONLY |

No diagnosis-service, FastAPI service, frontend runtime, Clinical Runtime, provider, PHI, or production dependency is added.

## Fixtures

Use `contracts/releases/v1/1.1.0/fixtures/**`. They are synthetic and `CONTRACT_VALID` only. They are not clinically valid, approved, or production-safe.

Frozen `contracts/v1/**` remains the exact 1.0.0 package for existing consumers; raw 1.0.0 payloads are not automatically 1.1.0-compatible.
