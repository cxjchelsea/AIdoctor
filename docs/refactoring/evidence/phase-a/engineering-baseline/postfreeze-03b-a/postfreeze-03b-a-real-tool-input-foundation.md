# POSTFREEZE-03B-A Real Tool Input / Artifact Resolution Foundation Evidence

> Dated: 2026-08-19
>
> Classification: `BOUNDED NON-CLINICAL IMPLEMENTATION` + `STRUCTURAL TOOLCONTEXT BINDING` + `STATIC ARTIFACT RESOLUTION` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-A`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_A_REAL_TOOL_INPUT_ARTIFACT_RESOLUTION_FOUNDATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `b13615336566911933c20c9182df02f5fe0c6f9e`
>
> Authorized Base tree: `0a587366150e0184d2aab083b6a36d8544e7f30f`
>
> Frozen engineering snapshot:
> `840a6fc7f83cec4fda6d53d739d7fd6b8013f031` /
> `d68d96d49eda96fe93dfb0c6c013b961a8e4b966`

```text
Creating ArtifactPort + ContextToolPort
!=
Shared Contracts v1 schema change
!=
RAW OCR adapter
!=
canonical Runtime OCR capability
!=
PHI-capable inputs
!=
Java real-tool cutover
!=
clinical Runtime
!=
Phase B
```

## 1. Authorization

- Token: `POSTFREEZE_03B_A_REAL_TOOL_INPUT_ARTIFACT_RESOLUTION_FOUNDATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-a-real-tool-input-foundation`
- Authorized exact Base: `b13615336566911933c20c9182df02f5fe0c6f9e`
- Authorized Base tree: `0a587366150e0184d2aab083b6a36d8544e7f30f`
- Live preflight Enterprise HEAD: `b13615336566911933c20c9182df02f5fe0c6f9e`
- Live preflight Enterprise tree: `0a587366150e0184d2aab083b6a36d8544e7f30f`
- PR #59: remains `MERGED` at `b13615336566911933c20c9182df02f5fe0c6f9e`
- POSTFREEZE-01: `DURABLY_CLOSED`
- POSTFREEZE-02: `DURABLY_CLOSED`
- POSTFREEZE-03A: `DURABLY_CLOSED`
- POSTFREEZE-03A post-merge push CI: `32212126073` / push / success
- Drift: none

## 2. Purpose

Establish an honest, bounded path for an input-bearing Tool to consume an
existing Shared Contracts v1 `ToolContext`:

```text
ToolContext
  -> InputRef(ref_type=ARTIFACT)
  -> ArtifactPort
  -> resolved bytes
```

This batch is generic real-tool input infrastructure only.

## 3. Changed files

Exactly 12:

NEW:

1. `packages/python_runtime/artifacts.py`
2. `packages/python_runtime/tool_router.py`
3. `packages/python_runtime/tests/test_tool_input_foundation.py`
4. `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-03b-a/postfreeze-03b-a-real-tool-input-foundation.md`

MODIFY:

5. `packages/python_runtime/ports.py`
6. `packages/python_runtime/executor.py`
7. `packages/python_runtime/http/app.py`
8. `packages/python_runtime/http/routes.py`
9. `packages/python_runtime/http/transport.py`
10. `packages/python_runtime/tests/test_http_adapter.py`
11. `packages/python_runtime/tests/test_runtime_foundation.py`
12. `packages/python_runtime/tests/test_architecture_guards.py`

Unexpected: 0

Not changed (explicitly required):

- `contracts/v1/**`
- `ocr-service/**`
- `packages/python_runtime/requirements-http.txt`
- `packages/python_runtime/tests/fixtures/runtime_protocol_invoke.json`
- `packages/model_runtime/**`
- `capabilities/**`
- `diagnosis-service/**`
- `examination-service/**`
- any Java source
- any Dockerfile
- `.github/workflows/ci.yml`

## 4. Architecture

Existing envelope protocol remains:

```text
ToolPort.invoke(ContractEnvelope) -> ToolResult
```

`FakeToolPort` still consumes only `ContractEnvelope`. It was not widened to
`ToolContext`.

New distinct input-bearing protocol:

```text
ContextToolPort.invoke(context: ToolContext, artifacts: Sequence[ResolvedArtifact]) -> ToolResult
```

New `ArtifactPort`:

```text
ArtifactPort.resolve(ref_id: str, ref_version: int) -> ResolvedArtifact
```

`ResolvedArtifact` wraps the existing Shared Contracts `SourceArtifact` plus
bytes. No Runtime-local ToolContext DTO and no replacement artifact DTO were
invented.

`ToolRouter` is an explicit static registration table:

- envelope-only tools stay on `ToolPort`
- input-bearing tools stay on `ContextToolPort`
- no plugin discovery, entry-point scanning, or reflection auto-registration

`DeterministicRuntimeExecutor` remains single-invocation, deterministic, and
has no retry loop, AgentLoop, LangGraph, or State Committer.

Canonical HTTP route remains exactly:

```text
POST /api/v1/runtime/tools/invoke
```

Parser discrimination is structural:

- top-level `"envelope"` object → ToolContext candidate
- otherwise → ContractEnvelope candidate

`contract_name == "ToolContext"` is not used as the discriminator, because the
POSTFREEZE-02 golden envelope intentionally uses that name while remaining only
a `ContractEnvelope`.

## 5. Backward compatibility

- POSTFREEZE-02 golden fixture is unchanged
- golden fixture still parses as `ContractEnvelope`
- default `create_app()` still authorizes only `engineering.synthetic.runtime_smoke`
- default synthetic invoke still returns `synthetic-echo` / `SUCCEEDED` / `SYNTHETIC_RUNTIME_OK`
- `X-Trace-Id` required-and-equal behavior is unchanged
- unknown capabilities remain 403
- module-level `packages.python_runtime.http.app:app` still constructs without
  OCR / OpenCV / NumPy / Pillow / pytesseract / provider / database / storage /
  legacy FastAPI service imports

## 6. ToolContext handling

Public binding: `aidoctor_shared_contracts.ToolContext`

Fail-closed on:

- `contract_version` mismatch
- invalid structural model
- nested `envelope.contract_name != "ToolContext"`
- missing `X-Trace-Id`
- `X-Trace-Id != context.envelope.trace_id`
- `context.capability.capability_id != context.envelope.capability_id`
- `context.capability.capability_version != context.envelope.capability_version`
- unauthorized capability
- unsupported / non-ARTIFACT input for the synthetic artifact probe
- unknown artifact reference

Evidence wording:

- `TOOL_CONTEXT_STRUCTURAL_BINDING_VERIFIED` = YES
- `FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED` = NO

This batch binds the public Pydantic model. It does not execute
`contracts/v1/validator/validate_contracts.py` as a Runtime request gate.

## 7. Artifact resolver limitations

First implementation: `StaticAllowlistArtifactPort`

It is a deterministic in-memory / static allowlist only.

It does not:

- access S3
- access databases
- perform HTTP downloads
- inspect arbitrary filesystem paths
- resolve arbitrary user-provided paths
- read PHI
- become a production blob store

`storage_ref` remains an opaque logical reference
(`logical://engineering.synthetic.artifact_probe/v1`).

`SourceArtifact.original_filename` is not reinterpreted as a path.

Resolver verifies at least:

- exact allowlist membership
- `artifact_id` consistency
- requested ref version
- `size_bytes ==` actual content length
- SHA-256 equality
- `sensitivity != PHI` for this batch

## 8. PHI exclusions

POSTFREEZE-03B-A test artifacts contain no:

- patient names
- patient identifiers
- medical reports
- examination reports
- symptoms
- diagnoses
- clinical text
- real uploaded files
- real PHI

Synthetic probe bytes are opaque engineering bytes. Sensitivity is `INTERNAL`.

HTTP success output contains metadata and checksum only. Artifact bytes are not
logged, not returned from HTTP, and not written into checkpoints.

`PHI_FIXTURE_USED` = NO

## 9. Capability defaults

Default authorized capability remains:

```text
engineering.synthetic.runtime_smoke
```

Test-injected only:

```text
engineering.synthetic.artifact_probe / 0.0.1
```

`artifact_probe` is not enabled in the default module-level app or default
`create_app()`.

`engineering.ocr.raw` is not registered and is not default-enabled.

Unknown capabilities continue to fail closed with 403.

HTTP routes authorize from an injected set and dispatch to the executor/router.
They are not a capability-specific switch statement.

## 10. Tests

Local authoring environment, Python 3.13.5, no Tesseract, no real network OCR,
no real PHI. Existing `python-safety` CI remains Python 3.12 and was not
retargeted:

```text
PYTHONPATH=contracts/v1/bindings/python
python -m pytest packages/python_runtime/tests -q
68 passed
```

Existing relevant contract/runtime safety checks:

```text
python contracts/v1/validator/validate_contracts.py
A5 CONTRACT VALIDATION PASSED: 13 schemas, 13 valid fixtures, 33 invalid fixtures

python -m pytest -p no:cacheprovider contracts/v1/tests contracts/v1/bindings/python/tests packages/python_runtime/tests -q
187 passed
```

Coverage includes:

- unchanged golden envelope fixture still parses as `ContractEnvelope` and
  invokes `synthetic-echo` with identical logical behavior
- parser discrimination by top-level `envelope` object
- full ToolContext structural bind for `engineering.synthetic.artifact_probe`
- ARTIFACT `InputRef` visible to the input-bearing path
- known synthetic artifact resolves to exact bytes, checksum, size, id, version,
  and non-PHI sensitivity
- unknown ref / wrong version / checksum mismatch / size mismatch / PHI
  sensitivity / non-ARTIFACT / missing ARTIFACT / capability identity mismatch /
  capability version mismatch / missing or mismatched trace / unknown capability
  fail closed
- default `create_app()` does not authorize `artifact_probe`
- architecture guards: Runtime production code still does not import
  ocr-service, cv2, numpy, PIL, pytesseract, provider SDK, langgraph, or a
  State Committer

## 11. CI

`.github/workflows/ci.yml` was not modified.

Existing CI already executes Python Runtime tests under `python-safety`.

Author-time label:

- `CI_VERIFIED` = `PENDING_AT_AUTHORING`

Do not claim `CI_VERIFIED` until GitHub Actions actually completes on the exact
head after push.

## 12. Evidence labels

| Label | Value |
|---|---|
| DOCUMENTED | YES |
| CODE_CONFIRMED | YES |
| TEST_VERIFIED | YES |
| CI_VERIFIED | PENDING_AT_AUTHORING |
| REAL_TOOL_INPUT_FOUNDATION_ESTABLISHED | YES |
| TOOL_CONTEXT_STRUCTURAL_BINDING_VERIFIED | YES |
| ARTIFACT_INPUT_REF_RESOLUTION_VERIFIED | YES |
| SYNTHETIC_ARTIFACT_CHECKSUM_VERIFIED | YES |
| SYNTHETIC_ARTIFACT_SIZE_VERIFIED | YES |
| DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED | YES |
| POSTFREEZE_02_ENVELOPE_PROTOCOL_PRESERVED | YES |
| PHI_FIXTURE_USED | NO |
| OCR_DEPENDENCY_DELTA | NO |
| RAW_OCR_TOOL_ADAPTER_IMPLEMENTED | NO |
| REAL_TOOL_ADAPTER_TEST_VERIFIED | NO |
| CANONICAL_RUNTIME_RAW_OCR_ENABLED | NO |
| TESSERACT_ENGINE_VERIFIED | NO |
| REAL_IMAGE_OCR_VERIFIED | NO |
| REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED | NO |
| FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED | NO |
| JAVA_REAL_TOOL_PROTOCOL_VERIFIED | NO |
| JAVA_PYTHON_CUTOVER_VERIFIED | NO |
| REPOSITORY_RUNTIME_VERIFIED | NO |
| CLINICAL_VALIDATED | NO |
| PRODUCTION_VERIFIED | NO |

## 13. Forbidden claims

This batch does not claim:

- Shared Contracts v1 schema change
- complete Runtime semantic validator
- RAW OCR Tool adapter
- Tesseract engine verification
- real image OCR
- real artifact storage backend
- Java real-tool protocol
- Java → Python cutover
- Clinical Runtime
- production readiness
- Phase B
- A7 complete
- A11 passed
- Ready / merge
- `DURABLY_CLOSED`

## 14. Governance state

Must remain exact:

- A7-NC = COMPLETE / CLOSED / STABLE
- A7 = NOT_COMPLETE
- A7-CL = DEFERRED_OUT_OF_CURRENT_BASELINE / NOT COMPLETE
- A7-CL-02 = NOT_AUTHORIZED
- FB-11 = UNSATISFIED / PRESERVED
- FB-21 = PRESERVED / UNSATISFIED THROUGH A7 NOT_COMPLETE
- A11 = NOT_PASSED
- Original Frozen = NOT_READY_BLOCKED_CLINICAL + UNREACHABLE_UNDER_CURRENT_RESOURCE_MODEL
- Clinical Runtime = NOT_ENABLED
- Production = BLOCKED
- Phase B = NOT_AUTHORIZED

Never write: `A7 = CLOSED`

## 15. Boundary deltas

| Surface | Delta |
| --- | --- |
| Engineering Baseline semantics | 0 |
| contracts/v1 | 0 |
| packages/model_runtime | 0 |
| ocr-service | 0 |
| A7-NC | 0 |
| capability packages | 0 |
| legacy services | 0 |
| Java orchestration | 0 |
| clinical | 0 |
| provider | 0 |
| network inference | 0 |
| PHI fixtures | 0 |
| OCR dependencies | 0 |
| Phase B | 0 |
| LangGraph | 0 |

Production caller added = 0

## 16. Author status

`IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`

Do not write `DURABLY_CLOSED` for POSTFREEZE-03B-A in this document.
