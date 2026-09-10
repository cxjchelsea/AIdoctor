# Shared Contracts v1 exact release 1.1.0

This release is a parallel exact Phase B canonical contract release. It does not modify `contracts/v1/**`, migrate runtime consumers, enable Clinical Runtime, or wire production.

## Inventory

The manifest contains exactly 16 contracts:

- 11 semantic reissues: ContractEnvelope, IdentifierSet, ContractConflict, ToolContext, ToolResult, EvidencePack, SourceArtifact, KnowledgeReleaseRef, TraceRef, AuditRef, PatientDeliveryView.
- 2 modified reissues: StatePatch and CommitResult.
- 3 new canonical contracts: Encounter, ClinicalStateSnapshot, ClinicalObservation.

ObservationCandidate, BusinessEncounterView, and AgentEvent are not included.

## Canonical state model

Encounter references the current state version. ClinicalStateSnapshot is the versioned committed state representation. Its keyed `observations` object is the first-release canonical state body, with complete ClinicalObservation objects keyed by stable observation IDs.

Legacy StatePatch roots remain only in the legacy compatibility branch. They are not canonical ClinicalStateSnapshot sections.

## Validation

Run:

```text
python contracts/releases/v1/1.1.0/validator/validate_contracts.py
python -m pytest -p no:cacheprovider contracts/releases/v1/1.1.0/tests contracts/releases/v1/1.1.0/bindings/python/tests -q
python contracts/releases/v1/1.1.0/bindings/tooling/check_drift.py
mvn -f contracts/releases/v1/1.1.0/bindings/java/pom.xml test
npm --prefix contracts/releases/v1/1.1.0/bindings/typescript test
```

Validation proves structural contract authoring only. It is not B1, B2, clinical validation, runtime migration, or production verification.
