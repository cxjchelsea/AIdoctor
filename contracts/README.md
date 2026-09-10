# Shared Contracts

The contract tree contains exact-version contract packages. Consumers must opt in to an exact supported version; a minor release classification does not make a payload automatically wire-compatible with an older strict validator.

## `contracts/v1/`

Frozen Shared Contracts `1.0.0` package. This tree is preserved unchanged for existing Phase A / PBNC engineering foundations and current runtime consumers.

## `contracts/releases/v1/1.1.0/`

Parallel exact `1.1.0` v1-family release for Phase B canonical state and mutation contracts. It is self-contained: manifest, schemas, fixtures, validator, tests, bindings, and release-local tooling live under the release path.

`1.0.0` consumers are not automatically `1.1.0` compatible. Runtime migration, adapters, and consumer support declarations require later authorization.
