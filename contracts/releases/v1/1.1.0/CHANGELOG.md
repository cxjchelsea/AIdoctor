# Changelog

## 1.1.0 - 2026-09-09

- Add parallel exact v1-family release at `contracts/releases/v1/1.1.0/`.
- Preserve frozen `contracts/v1/**` Shared Contracts 1.0.0 package.
- Reissue 11 existing contracts semantically unchanged under 1.1.0 identity.
- Additive StatePatch reissue with `LEGACY_CDP` and `CANONICAL_ENCOUNTER_STATE` target modes.
- Additive CommitResult reissue with `cdp_id` / `encounter_id` target XOR.
- Add Encounter, ClinicalStateSnapshot, and ClinicalObservation.
- Define ClinicalObservation ObservationValue as a closed tagged union: TEXT, NUMBER, BOOLEAN, CODED, QUANTITY, REFERENCE.
- Defer ObservationCandidate, candidate promotion workflow, runtime migration, Clinical Runtime, and production wiring.
