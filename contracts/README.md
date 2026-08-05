# Shared Contracts

The versioned directories below are the language-neutral contract source of
truth. Phase A5 intentionally contains no generated Java, Python or TypeScript
bindings and does not modify any service runtime.

- `v1/`: Shared Contracts 1.0.0, JSON Schema Draft 2020-12.

Consumers must select an explicitly supported exact contract version from the
versioned manifest; a minor classification does not make a payload automatically
wire-compatible with an older strict validator. Contract adoption,
binding generation and runtime compatibility testing require later authorized
migration work.
