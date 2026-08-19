# POSTFREEZE-03B-B Raw OCR Tool Adapter + Injected Runtime Composition Evidence

> Dated: 2026-08-19
>
> Classification: `BOUNDED NON-CLINICAL IMPLEMENTATION` + `INJECTED RAW OCR ADAPTER` + `TEST-ONLY RUNTIME COMPOSITION` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-B`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_B_RAW_OCR_TOOL_ADAPTER_INJECTED_RUNTIME_COMPOSITION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `eb6e372953845e1aab4402af9198dedde6451328`
>
> Authorized Base tree: `4f9d7c98376bfac3819911306ede72cf03c4d949`
>
> Implementation branch: `agent/postfreeze-03b-b-raw-ocr-adapter-runtime-composition`
>
> Frozen engineering snapshot:
> `840a6fc7f83cec4fda6d53d739d7fd6b8013f031` /
> `d68d96d49eda96fe93dfb0c6c013b961a8e4b966`

```text
Injected Runtime Composition
!=
default Runtime enablement
!=
canonical raw OCR capability
!=
real Tesseract engine verification
!=
real image OCR
!=
Java cutover
!=
clinical Runtime
!=
Phase B
```

## 1. Authorization

- Token: `POSTFREEZE_03B_B_RAW_OCR_TOOL_ADAPTER_INJECTED_RUNTIME_COMPOSITION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-b-raw-ocr-adapter-runtime-composition`
- Authorized exact Base: `eb6e372953845e1aab4402af9198dedde6451328`
- Authorized Base tree: `4f9d7c98376bfac3819911306ede72cf03c4d949`
- Live preflight Enterprise HEAD: `eb6e372953845e1aab4402af9198dedde6451328`
- Live preflight Enterprise tree: `4f9d7c98376bfac3819911306ede72cf03c4d949`
- Drift: none
- POSTFREEZE-01 / 02 / 03A / 03B-A: `DURABLY_CLOSED`
- 03B-A merge: `eb6e372953845e1aab4402af9198dedde6451328`
- 03B-A post-merge push CI: `32219871074` / push / success
  (historical only; not reused as 03B-B CI)

## 2. Purpose

Establish a production-shaped, OCR-library-free `RawOcrToolAdapter` and prove
the injected composition path:

```text
ToolContext
  -> ARTIFACT InputRef
  -> ArtifactPort
  -> ResolvedArtifact.content bytes
  -> RawOcrToolAdapter
  -> injected RawOcrEngine
  -> ToolResult
```

This batch does not enable raw OCR on the default canonical Runtime.

## 3. Exact six-file scope

NEW:

1. `packages/python_runtime/raw_ocr_adapter.py`
2. `packages/python_runtime/tests/test_raw_ocr_adapter.py`
3. `ocr-service/tests/test_raw_ocr_runtime_composition.py`
4. `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-03b-b/postfreeze-03b-b-raw-ocr-tool-adapter.md`

MODIFY:

5. `.github/workflows/ci.yml` (only `ocr-separation`)
6. `packages/python_runtime/tests/test_architecture_guards.py`

Unexpected files = 0

Not changed:

- `contracts/v1/**`
- `ocr-service/app/services/raw_ocr.py`
- `ocr-service/app/services/ocr_service.py`
- `ocr-service/app/api/routes.py`
- `ocr-service/app/main.py`
- `ocr-service/requirements.txt`
- `ocr-service/Dockerfile`
- `packages/python_runtime/artifacts.py`
- `packages/python_runtime/tool_router.py`
- `packages/python_runtime/ports.py`
- `packages/python_runtime/executor.py`
- `packages/python_runtime/http/app.py`
- `packages/python_runtime/http/routes.py`
- `packages/python_runtime/http/transport.py`
- `packages/python_runtime/requirements-http.txt`
- `packages/model_runtime/**`
- `capabilities/**`
- `diagnosis-service/**`
- any Java source
- any Dockerfile
- any real artifact/image fixture

## 4. Architecture

Production Runtime adapter remains injection-only.

```text
RawOcrEnginePort.recognize_from_bytes(image_bytes) -> str
RawOcrToolAdapter.invoke(ToolContext, Sequence[ResolvedArtifact]) -> ToolResult
```

`RawOcrEnginePort` lives in `raw_ocr_adapter.py`. It is not added to frozen
`ports.py`.

The adapter:

- consumes exactly one already-resolved `ResolvedArtifact`
- passes `content` bytes to the injected engine
- does not resolve `storage_ref`
- does not open `original_filename`
- does not download HTTP / S3 / database / arbitrary filesystem paths
- does not import `cv2`, `numpy`, `PIL`, `Pillow`, `pytesseract`
- does not import `app.services.raw_ocr`, `app.services.ocr_service`, or
  `ocr_service`
- does not contain the production literal `engineering.ocr.raw`

Default Runtime remains:

```text
create_app()
app = create_app()
```

authorized capability:

```text
engineering.synthetic.runtime_smoke
```

Default Runtime does not register `RawOcrToolAdapter` and does not import
`RawOcrEngine`.

## 5. Dependency direction

```text
packages/python_runtime/raw_ocr_adapter.py
  -> Shared Contracts ToolContext / ToolResult
  -> injected RawOcrEnginePort
  -x ocr-service
  -x OCR image stack

ocr-service/app/services/raw_ocr.py
  -> PIL / OpenCV / NumPy / pytesseract
  -x packages.python_runtime

ocr-service/tests/test_raw_ocr_runtime_composition.py
  -> RawOcrEngine
  -> packages.python_runtime
  (TEST-ONLY composition boundary)
```

Python 3.10 composition note:

`packages.model_runtime.api.models` executes `from typing import Self`.
`typing.Self` exists only on Python 3.11+. `ocr-separation` must stay on 3.10.
Installing `packages/model_runtime/requirements-ci.txt` would upgrade OCR
`pydantic==2.5.0` to `pydantic==2.10.3` and is therefore not used.
The composition test installs a test-only `typing.Self` name if missing.
This is not a production Runtime change and not a requirements change.

Python Runtime requirements were not changed.
OCR service requirements were not changed.
`packages/model_runtime/requirements.txt` was not changed.
`packages/model_runtime/requirements-ci.txt` was not installed.

CI `ocr-separation` additionally installs the existing files:

- `packages/model_runtime/requirements.txt` (PyYAML for `packages.model_runtime`)
- `packages/python_runtime/requirements-http.txt` (httpx / FastAPI TestClient)

## 6. Test-only composition

Capability identity appears only in tests and this evidence document:

```text
capability_id = engineering.ocr.raw
capability_version = 0.0.1
requested_operation = RAW_OCR_RECOGNIZE
```

Composition constructs, by test injection only:

```text
RawOcrEngine
  -> RawOcrToolAdapter
  -> ToolRouter.register_context_tool(...)
  -> StaticAllowlistArtifactPort
  -> DeterministicRuntimeExecutor
  -> create_app(
       runtime_executor=<injected>,
       authorized_capability_ids=<test-only raw OCR + smoke>
     )
  -> POST /api/v1/runtime/tools/invoke
```

`X-Trace-Id` equals the nested `envelope.trace_id`.

Real `RawOcrEngine.recognize_from_bytes()` performs PIL decode and
OpenCV/NumPy preprocessing. `pytesseract.image_to_string` is monkeypatched.
No Tesseract binary is installed or invoked.

This proves only:

```text
INJECTED_RUNTIME_RAW_OCR_COMPOSITION_TEST_VERIFIED
```

It does not prove:

```text
CANONICAL_RUNTIME_RAW_OCR_ENABLED
REPOSITORY_RUNTIME_VERIFIED
PRODUCTION_VERIFIED
TESSERACT_ENGINE_VERIFIED
REAL_IMAGE_OCR_VERIFIED
```

## 7. ToolResult semantics

Frozen Shared Contracts v1 `ToolResult` only. No second contract model.

| Engine / adapter outcome | status | retryable | reason_code | category |
|---|---|---|---|---|
| non-empty text, `len <= 4000` | `SUCCEEDED` | false | `RAW_OCR_OK` | (no errors) |
| engine returns `""` | `NO_RESULT` | false | `RAW_OCR_NO_RESULT` | (no errors) |
| `len(raw_text) > 4000` | `NON_RETRYABLE_FAILURE` | false | `RAW_OCR_OUTPUT_TOO_LONG` | `VALIDATION` |
| `RAW_OCR_ENGINE_UNAVAILABLE` | `RETRYABLE_FAILURE` | true | `RAW_OCR_ENGINE_UNAVAILABLE` | `DEPENDENCY` |
| `RAW_OCR_EXECUTION_FAILED` | `NON_RETRYABLE_FAILURE` | false | `RAW_OCR_EXECUTION_FAILED` | `INTERNAL` |
| unexpected exception | `NON_RETRYABLE_FAILURE` | false | `RAW_OCR_ADAPTER_INTERNAL` | `INTERNAL` |
| artifact count `!= 1` | `NON_RETRYABLE_FAILURE` | false | `RAW_OCR_INPUT_CARDINALITY_INVALID` | `VALIDATION` |

Success output is exactly:

```text
[{"name": "raw_text", "value": <exact raw OCR string>}]
```

Always:

```text
suggested_patches = []
evidence_refs = []
```

Empty OCR is not converted into a technical failure.
Overlong text is not truncated and is not leaked into the error message.
Raw exception traceback, image bytes, and OCR text are not placed in errors.
`artifact_id` is not written into `evidence_refs`.

`tool_name = raw-ocr`
`tool_version` comes from the injected `ToolContext.capability.capability_version`.

## 8. PHI / fixture exclusion

All test artifacts are synthetic, in-memory, non-medical, non-PHI.

Allowed example text: `HELLO OCR 123`

No patient name, patient ID, medical report, examination report, symptoms,
diagnosis, medication, clinical reference ranges, or real uploaded patient
file.

No image files were added to the repository. PNG bytes are generated in
memory. Sensitivity remains `INTERNAL`.

Existing `StaticAllowlistArtifactPort` PHI rejection is unchanged.

`PHI_FIXTURE_USED` = NO

## 9. Tesseract exclusion

This stage does not install or run a real Tesseract binary.

```text
TESSERACT_ENGINE_VERIFIED = NO
REAL_IMAGE_OCR_VERIFIED = NO
```

```text
mocked pytesseract
!=
Tesseract engine verification

real decode/preprocess
!=
real OCR engine execution
```

## 10. Default Runtime unchanged

`packages/python_runtime/http/app.py` was not modified.

```text
DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED = YES
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
OCR_DEPENDENCY_ADDED_TO_CANONICAL_RUNTIME = NO
```

## 11. CI change

Only the `ocr-separation` job was modified.

Unchanged:

- workflow triggers
- permissions
- concurrency
- contracts
- java
- python-safety
- frontend
- java-python-protocol

`ocr-separation` still uses Python 3.10.
It does not `apt install` tesseract packages.

Historical raw-engine separation step is preserved and still runs
`tests/test_raw_ocr.py` + `tests/test_legacy_ocr_compat.py`.
The composition file is ignored in that step so the historical
`sys.modules` isolation proof remains uncontaminated.

A separate step runs `tests/test_raw_ocr_runtime_composition.py` with:

```text
PYTHONPATH=../contracts/v1/bindings/python:..
```

The new test does not replace the old separation proof.

## 12. Local tests

Local authoring environment, Python 3.13.5, no Tesseract binary, no real PHI.

```text
python contracts/v1/validator/validate_contracts.py
A5 CONTRACT VALIDATION PASSED: 13 schemas, 13 valid fixtures, 33 invalid fixtures

PYTHONPATH=contracts/v1/bindings/python
python -m pytest packages/python_runtime/tests/test_raw_ocr_adapter.py -q
11 passed

PYTHONPATH=contracts/v1/bindings/python
python -m pytest packages/python_runtime/tests -q
80 passed

ocr-service:
python -m pytest tests/test_raw_ocr.py tests/test_legacy_ocr_compat.py -q
13 passed

ocr-service PYTHONPATH=../contracts/v1/bindings/python;..
python -m pytest tests/test_raw_ocr_runtime_composition.py -q
3 passed

ocr-service:
python -m pytest tests --ignore=tests/test_raw_ocr_runtime_composition.py -q
13 passed

ocr-service PYTHONPATH=../contracts/v1/bindings/python;..
python -m pytest tests -q
16 passed
```

## 13. Fresh CI identity

Do not reuse `32219871074`.

The 03B-B Draft PR must have a fresh run on its own Head.

First exact-Head run:

- `32222655598`
- event = `pull_request`
- head = `763f32a97593051ced4f07c89a874d55e48ab358`
- conclusion = `failure`
- `ocr-separation` historical raw-engine step = success
- `ocr-separation` composition step = failure
- cause = `ImportError: cannot import name 'Self' from 'typing'` on Python 3.10
  while importing `packages.model_runtime`

Remediation stays inside the authorized composition test file: test-only
`typing.Self` shim. `requirements-ci.txt` is not installed.

Until the remediated Head run succeeds:

```text
CI_VERIFIED = PENDING_REMEDIATION_CI
```

## 14. Evidence labels

| Label | Value |
|---|---|
| DOCUMENTED | YES |
| CODE_CONFIRMED | YES |
| TEST_VERIFIED | YES |
| CI_VERIFIED | PENDING_REMEDIATION_CI |
| RAW_OCR_TOOL_ADAPTER_IMPLEMENTED | YES |
| REAL_TOOL_ADAPTER_TEST_VERIFIED | YES |
| RAW_OCR_PREPROCESSING_INTEGRATION_VERIFIED | YES |
| INJECTED_RUNTIME_RAW_OCR_COMPOSITION_TEST_VERIFIED | YES |
| DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED | YES |
| PHI_FIXTURE_USED | NO |
| OCR_DEPENDENCY_ADDED_TO_CANONICAL_RUNTIME | NO |
| TESSERACT_ENGINE_VERIFIED | NO |
| REAL_IMAGE_OCR_VERIFIED | NO |
| CANONICAL_RUNTIME_RAW_OCR_ENABLED | NO |
| REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED | NO |
| FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED | NO |
| JAVA_REAL_TOOL_PROTOCOL_VERIFIED | NO |
| JAVA_PYTHON_CUTOVER_VERIFIED | NO |
| REPOSITORY_RUNTIME_VERIFIED | NO |
| CLINICAL_VALIDATED | NO |
| PRODUCTION_VERIFIED | NO |

## 15. Forbidden claims

This batch does not claim and must not be written as:

- `CANONICAL_RUNTIME_RAW_OCR_ENABLED = YES`
- `TESSERACT_ENGINE_VERIFIED = YES`
- `REAL_IMAGE_OCR_VERIFIED = YES`
- `PRODUCTION_VERIFIED = YES`
- Shared Contracts change
- default raw OCR registration
- Java OCR cutover
- legacy OCR route repair
- Clinical Runtime
- Phase B
- Ready / merge
- `DURABLY_CLOSED`

```text
in-process/injected HTTP composition
!=
default Runtime enablement

CI green
!=
repository runtime verification

synthetic non-PHI image
!=
clinical validation
```

## 16. Governance state

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

POSTFREEZE-03B-B is an engineering POSTFREEZE stage only.

## 17. Known limitations

- Default Runtime still authorizes only `engineering.synthetic.runtime_smoke`.
- No Tesseract binary is present in CI or local verification.
- Artifact resolution remains the 03B-A static allowlist; no real storage backend.
- Composition is test-injected HTTP only; it is not repository / production runtime.
- Engine technical errors are mapped by stable `error_code` attributes, not by
  importing `ocr-service` exception types into production Runtime.
- Legacy OCR URL mismatch is intentionally unrepaired.

## 18. Author status

`IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`

Do not write `DURABLY_CLOSED` for POSTFREEZE-03B-B in this document.
