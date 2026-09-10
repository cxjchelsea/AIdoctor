# Shared Contracts v1 Language Bindings

> Classification: `REVIEWED_BINDING` / `TYPES_AND_TESTS_ONLY`
>
> Semantic authority: `contracts/v1/manifest.json` + `contracts/v1/schemas/**`
>
> Semantic oracle: `contracts/v1/validator/validate_contracts.py`
>
> Version: `1.0.0` / `EXACT`

This package is NC-CLOSE-02. It does **not** migrate Java Feign clients,
legacy FastAPI models, or frontend runtime types.

```text
BINDING_CREATED
!=
JAVA_RUNTIME_MIGRATED
!=
LEGACY_SERVICE_MIGRATED
!=
FRONTEND_RUNTIME_MIGRATED
```

## Strategy

Mixed strategy (assessment Option D / B):

1. JSON Schema / manifest is the only semantic source.
2. Each language has reviewed structural types (`REVIEWED_BINDING`).
3. Fixtures round-trip to canonical JSON.
4. The existing Python validator remains the semantic oracle for
   `VALIDATOR_ENFORCED` rules (`if/then`, StatePatch tokens, citations).

Java 8 + Draft 2020-12 full codegen is **not** used.

Generated-file policy: version stamps are produced by
`tooling/sync_version.py`. Type files are reviewed structural bindings.
Do not hand-edit them to match legacy DTOs.

## Layout

```text
contracts/v1/bindings/
  README.md
  java/          com.aidoctor:shared-contracts:1.0.0
  python/        aidoctor_shared_contracts
  typescript/    @aidoctor/shared-contracts
  tooling/       sync_version.py, check_drift.py
```

## Commands (NC-CLOSE-03 handoff)

From repository root, after installing contract-test dependencies:

```text
python contracts/v1/validator/validate_contracts.py
python -m pytest -p no:cacheprovider contracts/v1/tests contracts/v1/bindings/python/tests -q
python contracts/v1/bindings/tooling/check_drift.py
python contracts/v1/bindings/tooling/sync_version.py
mvn -f contracts/v1/bindings/java/pom.xml test
```

TypeScript (DEV_ONLY `typescript@5.2.2` inside the bindings package):

```text
npm --prefix contracts/v1/bindings/typescript install --ignore-scripts
npm --prefix contracts/v1/bindings/typescript test
```

`tsc --noEmit` is the typecheck. Do not add GitHub Actions here.

## 13 schemas

ContractEnvelope, IdentifierSet, StatePatch, CommitResult,
ContractConflict, ToolContext, ToolResult, EvidencePack,
SourceArtifact, KnowledgeReleaseRef, TraceRef, AuditRef,
PatientDeliveryView.

Legacy Java/Python `ToolContext` / `ToolResult` remain parallel models.
This package represents v1 `SUCCEEDED` + `suggested_patches: StatePatch[]`.

StatePatch is a proposal. CommitResult is a commit outcome.
TraceRef / AuditRef are references, not ExecutionTrace / AuditTrail rows.

## Dependencies

| Area | Classification |
|---|---|
| Jackson 2.13.5 (binding module only) | BUILD_TIME_ONLY |
| JUnit 4.13.2 | TEST / DEV_ONLY |
| Pydantic 2.10.3 / pytest / jsonschema | DEV_ONLY |
| TypeScript 5.2.2 | DEV_ONLY |

No diagnosis-service, FastAPI service, or frontend runtime dependency is added.

## Fixtures

Reuse `contracts/v1/fixtures/**`. They are synthetic and `CONTRACT_VALID` only.
They are not clinically valid, approved, or production-safe.
