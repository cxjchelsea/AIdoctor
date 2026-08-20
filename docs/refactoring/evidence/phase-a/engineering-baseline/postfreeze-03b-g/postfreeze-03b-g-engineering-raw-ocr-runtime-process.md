# POSTFREEZE-03B-G Engineering-only Raw OCR Runtime Process Evidence

> Dated: 2026-08-20
>
> Classification: `BOUNDED NON-CLINICAL ENGINEERING IMPLEMENTATION` + `ENGINEERING_ONLY LOCALHOST PROCESS PROOF` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-G`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_G_ENGINEERING_RAW_OCR_RUNTIME_PROCESS_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `85bc49befcca5b94c9e183f81f1f4cd02272f501`
>
> Authorized Base tree: `9e197d5b88a2db85006af79efb453172ba6caf51`
>
> Implementation branch: `agent/postfreeze-03b-g-engineering-raw-ocr-runtime-process`
>
> Predecessor: `POSTFREEZE-03B-F = DURABLY_CLOSED`
> (merge `85bc49…` / post-merge push CI `32333954431`)

```text
repository-defined engineering localhost process
!=
03B-F TestClient factory proof
!=
default smoke uvicorn process (POSTFREEZE-02)
!=
canonical / default Runtime raw OCR registration
!=
real Artifact storage
!=
Java real-tool protocol
!=
OCR Docker Runtime / FastAPI service proof
!=
legacy OCR route remediation
!=
OCR accuracy / Chinese quality / clinical validation
```

```text
POSTFREEZE_03B_G_IMPLEMENTATION = IN_PROGRESS
CI_VERIFIED = PENDING_EXACT_HEAD_CI
INDEPENDENT_REVIEW = NOT_PERFORMED
PMV = NOT_PERFORMED
MERGE_AUTHORIZATION_GRANTED = NO
DURABLY_CLOSED = NO
F004 = PRESERVED
```

## 1. Authorization

- Token: `POSTFREEZE_03B_G_ENGINEERING_RAW_OCR_RUNTIME_PROCESS_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-g-engineering-raw-ocr-runtime-process`
- Authorized exact Base: `85bc49befcca5b94c9e183f81f1f4cd02272f501`
- Authorized Base tree: `9e197d5b88a2db85006af79efb453172ba6caf51`
- Live preflight Enterprise HEAD: `85bc49befcca5b94c9e183f81f1f4cd02272f501`
- Live preflight Enterprise tree: `9e197d5b88a2db85006af79efb453172ba6caf51`
- Drift: none
- POSTFREEZE-03B-F: `DURABLY_CLOSED`
- 03B-F post-merge push CI: `32333954431` / push / success
  (historical predecessor only; not reused as 03B-G CI)

## 2. Exact evidence question

Can the repository start an explicit, non-default, engineering-only
Raw OCR Runtime process that binds only to 127.0.0.1, serves existing
Runtime HTTP over a real socket, uses
`create_engineering_raw_ocr_app(...)`, resolves one bounded synthetic
non-PHI artifact through `StaticAllowlistArtifactPort`, invokes
`RawOcrToolAdapter` / actual `RawOcrEngine` / actual Tesseract with
`DEFAULT_OCR_LANGUAGE = chi_sim+eng`, and returns a valid ToolResult,
while leaving default `create_app()` smoke-only and not enabling
canonical raw OCR, production, Java cutover, real Artifact storage,
Docker Runtime, OCR accuracy, PHI, or Clinical Runtime?

## 3. Why this stage

03B-F proved a repository-defined factory exercised in-process via
TestClient. POSTFREEZE-02 proved default smoke
`packages.python_runtime.http.app:app` can run as a process, but that
process authorizes only `engineering.synthetic.runtime_smoke`.

```text
TestClient HTTP != repository process HTTP
default smoke process != engineering.ocr.raw process
```

## 4. Exact five-file scope

1. NEW `engineering/raw_ocr_runtime_process.py`
2. MODIFY `packages/python_runtime/tests/test_architecture_guards.py`
3. NEW `ocr-service/tests/test_engineering_raw_ocr_runtime_process.py`
4. MODIFY `.github/workflows/ci.yml`
5. NEW this evidence document

```text
POSTFREEZE_03B_G_TOTAL_SCOPE = 5_FILES_EXACT
UNEXPECTED_FILES = 0
```

Unchanged (byte-identical to Base):

- `packages/python_runtime/http/app.py`
- `packages/python_runtime/http/engineering_raw_ocr.py`
- `packages/python_runtime/raw_ocr_adapter.py`
- `ocr-service/app/services/raw_ocr.py`
- `packages/python_runtime/artifacts.py`

## 5. Launcher placement

Path: `engineering/raw_ocr_runtime_process.py`

Classification: `NON_PRODUCTION_ENGINEERING_LOCALHOST_PROCESS_ENTRYPOINT`

```text
ENGINEERING_COMPOSITION_ROOT_SELECTED =
engineering/raw_ocr_runtime_process.py
```

This top-level `engineering/` root is the composition root. It may
import both `RawOcrEngine` and `create_engineering_raw_ocr_app`.

```text
packages/python_runtime production
  does not import OCR implementation

ocr-service/app production
  does not import python_runtime
```

No `sys.path.insert` / `sys.path.append`. PYTHONPATH is supplied by
the process test / CI.

```text
MODULE_LEVEL_APP_PRESENT = NO
IMPORT_SIDE_EFFECT_NETWORK_START = NO
HOST = 127.0.0.1
HOST_CONFIGURABLE = NO
PUBLIC_BIND_PRESENT = NO
```

Port is engineering-only via `AIDOCTOR_ENGINEERING_RAW_OCR_PORT`.
Invalid or missing values fail closed.

## 6. Factory reuse

```text
USES_CREATE_ENGINEERING_RAW_OCR_APP = YES
IMPORTS_TOOL_ROUTER = NO
IMPORTS_DETERMINISTIC_RUNTIME_EXECUTOR = NO
IMPORTS_DEFAULT_CREATE_APP = NO
REBUILDS_03B_F_COMPOSITION = NO
```

The launcher does not contain the `engineering.ocr.raw` literal.
Capability identity remains in the 03B-F factory and in test payloads.

## 7. Artifact bootstrap

```text
ARTIFACT_PORT = StaticAllowlistArtifactPort
BOOTSTRAP = BOUNDED_ENVIRONMENT_BASE64
REAL_ARTIFACT_BACKEND = NO
FILESYSTEM_ARTIFACT_READ = NO
NETWORK_ARTIFACT_RETRIEVAL = NO
STORAGE_REF_OPEN = NO
PHI = NO
MAX_ARTIFACT_BYTES = 2097152
```

Environment: `AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_BASE64`

Missing / invalid base64 / empty / oversized bytes fail closed.
`size_bytes` and SHA-256 are computed from decoded actual bytes.
`storage_ref` is a fixed opaque `artifact://` value and is never opened.

## 8. Python 3.11 interpreter

```text
process proof interpreter = Python 3.11
```

Rationale:

- Python 3.11 has native `typing.Self`
- NumPy 1.24.3 supports Python 3.11
- avoids promoting the 03B-F test-only `typing.Self` shim into the launcher
- preserves F001
- preserves current dependency pins

Preflight (local isolated venv, no requirement file changes):

```text
ocr-service/requirements.txt
packages/model_runtime/requirements.txt
packages/python_runtime/requirements-http.txt
+ PYTHONPATH Shared Contracts binding
= IMPORT_PROBE=PASS on Python 3.11.14
```

```text
Python 3.11 process proof
!= OCR Docker Runtime verification
```

Current OCR Docker image remains `python:3.10-slim`.

```text
OCR_DOCKER_RUNTIME_VERIFIED = NO
```

## 9. Process proof chain

```text
synthetic in-memory PNG "HELLO OCR 123"
  → base64 environment bootstrap
  → subprocess engineering/raw_ocr_runtime_process.py
  → RawOcrEngine + RawOcrToolAdapter + StaticAllowlistArtifactPort
  → create_engineering_raw_ocr_app(...)
  → uvicorn 127.0.0.1:<test-port>
  → GET /api/v1/runtime/health
  → POST /api/v1/runtime/tools/invoke ToolContext
  → ArtifactPort.resolve
  → RawOcrToolAdapter
  → RawOcrEngine
  → actual Tesseract default chi_sim+eng
  → ToolResult HELLO / OCR
```

Authoritative proof is subprocess + real TCP socket.
TestClient is used only for default-surface negative regression.

## 10. CI strategy

New isolated job:

`ocr-runtime-engineering-process-real-engine`

on `ubuntu-24.04`, Python 3.11, host apt Tesseract eng+chi_sim,
`AIDOCTOR_REQUIRE_REAL_TESSERACT=1`.

Also re-runs the existing 03B-F default-surface test.

`ocr-separation` precisely ignores only:

`tests/test_engineering_raw_ocr_runtime_process.py`

Preserved existing 10 jobs. Expected matrix size: 11 jobs.

Authoring-time pointer (F004 preserved):

```text
CI_VERIFIED = PENDING_EXACT_HEAD_CI
```

Do not add a documentation-only commit after fresh CI to rewrite this line.

## 11. Evidence ceiling (must remain)

```text
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
OCR_DOCKER_RUNTIME_VERIFIED = NO
OCR_SERVICE_HTTP_IN_DOCKER_VERIFIED = NO
REPOSITORY_RUNTIME_VERIFIED = NO
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED = NO
FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED = NO
JAVA_REAL_TOOL_PROTOCOL_VERIFIED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
REAL_IMAGE_OCR_VERIFIED = NO
CHINESE_OCR_QUALITY_VERIFIED = NO
OCR_ACCURACY_VERIFIED = NO
CLINICAL_VALIDATED = NO
PRODUCTION_VERIFIED = NO
```

```text
LEGACY_OCR_ROUTE_MISMATCH = PRESERVED / OUT_OF_SCOPE
RAW_OCR_CLINICAL_BOUNDARY_PRESERVED = YES
PHI_REQUIRED = NO
PHI_FIXTURE_USED = NO
```

`REPOSITORY_RUNTIME_VERIFIED` remains NO until Independent Review, PMV,
standard merge, and fresh Enterprise push CI all complete.

## 12. Governance preservation

```text
A7-NC = COMPLETE / CLOSED / STABLE
A7 = NOT_COMPLETE
A7-CL = DEFERRED_OUT_OF_CURRENT_BASELINE / NOT COMPLETE
A7-CL-02 = NOT_AUTHORIZED
FB-11 = UNSATISFIED / PRESERVED
FB-21 = PRESERVED / UNSATISFIED THROUGH A7 NOT_COMPLETE
A11 = NOT_PASSED
Original Frozen = NOT_READY_BLOCKED_CLINICAL + UNREACHABLE_UNDER_CURRENT_RESOURCE_MODEL
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
Phase B = NOT_AUTHORIZED
F001 = PRESERVED
F004 = PRESERVED
adult_respiratory_v1 = DRAFT / PARTIALLY_VALIDATED / REQUIRES_CLINICAL_REVIEW / NOT_IMPLEMENTED
PHASE_A_ENGINEERING_BASELINE = FROZEN / V1
```

Never write: `A7 = CLOSED`.

## 13. Stop state

```text
READY_FOR_POSTFREEZE_03B_G_INDEPENDENT_REVIEW = YES   # only after green exact-Head CI
READY_FOR_POSTFREEZE_03B_G_PREMERGE_VERIFICATION = NO
MERGE_AUTHORIZATION_GRANTED = NO
POSTFREEZE_03B_G_DURABLY_CLOSED = NO
```

STOP after implementation report. Wait for independent review.
Do not mark Ready, merge, PMV, or start 03B-H.
