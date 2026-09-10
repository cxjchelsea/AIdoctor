# POSTFREEZE-03B-F Engineering-only Opt-in Raw OCR Runtime Composition Evidence

> Dated: 2026-08-20
>
> Classification: `BOUNDED NON-CLINICAL ENGINEERING IMPLEMENTATION` + `EXPLICIT NON-DEFAULT RUNTIME COMPOSITION PROOF` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-F`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_F_ENGINEERING_OPT_IN_RAW_OCR_RUNTIME_COMPOSITION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `48871ff3072e64a74ec86d41666f6775d47156a0`
>
> Authorized Base tree: `d2b953b45a757349d133ba5a0842b23eaf0f18d9`
>
> Implementation branch: `agent/postfreeze-03b-f-engineering-opt-in-raw-ocr-runtime`
>
> Predecessor: `POSTFREEZE-03B-E = DURABLY_CLOSED`
> (merge `48871ff…` / post-merge push CI `32326052324`)

```text
repository-defined engineering opt-in composition
!=
pytest-only arbitrary assembly (03B-D)
!=
canonical / default Runtime raw OCR registration
!=
repository Runtime process verification
!=
Java real-tool protocol
!=
real Artifact storage
!=
OCR Docker HTTP / FastAPI service proof
!=
legacy OCR route remediation
!=
OCR accuracy / Chinese quality / clinical validation
```

## 1. Authorization

- Token: `POSTFREEZE_03B_F_ENGINEERING_OPT_IN_RAW_OCR_RUNTIME_COMPOSITION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-f-engineering-opt-in-raw-ocr-runtime`
- Authorized exact Base: `48871ff3072e64a74ec86d41666f6775d47156a0`
- Authorized Base tree: `d2b953b45a757349d133ba5a0842b23eaf0f18d9`
- Live preflight Enterprise HEAD: `48871ff3072e64a74ec86d41666f6775d47156a0`
- Live preflight Enterprise tree: `d2b953b45a757349d133ba5a0842b23eaf0f18d9`
- Drift: none
- POSTFREEZE-01 … 03B-E: `DURABLY_CLOSED`
- 03B-E post-merge push CI: `32326052324` / push / success
  (historical predecessor only; not reused as 03B-F CI)

## 2. Exact evidence question

Can the repository provide an explicit, non-default engineering composition
factory that wires an injected ContextToolPort and ArtifactPort into the
existing Runtime HTTP app such that ToolContext invocation of
`engineering.ocr.raw` succeeds through real RawOcrEngine / Tesseract, while
default `create_app()` continues to authorize only
`engineering.synthetic.runtime_smoke` and rejects raw OCR?

## 3. Why this stage (not Docker HTTP)

03B-E already proved Docker-contained RawOcrEngine execution.

Docker FastAPI /health or legacy `/api/v1/ocr/ocr/recognize` would primarily
validate the legacy OCR microservice surface, not the constrained
Java → Shared Contracts → Python Runtime → ToolAdapter → ArtifactPort →
RawOcrEngine target architecture.

03B-F closes the gap between pytest-only injection (03B-D) and a
repository-defined explicit opt-in composition contract, without canonical
enablement.

## 4. Exact five-file scope

1. NEW `packages/python_runtime/http/engineering_raw_ocr.py`
2. MODIFY `packages/python_runtime/tests/test_architecture_guards.py`
   (`ARCHITECTURE_GUARD_TEST_PATH` uniquely resolved LIVE)
3. NEW `ocr-service/tests/test_engineering_opt_in_raw_ocr_runtime.py`
4. MODIFY `.github/workflows/ci.yml`
5. NEW this evidence document

Unexpected files = 0

Unchanged (byte-identical to Base):

- `packages/python_runtime/http/app.py`
- `packages/python_runtime/executor.py`
- `packages/python_runtime/tool_router.py`
- `packages/python_runtime/ports.py`
- `packages/python_runtime/artifacts.py`
- `packages/python_runtime/raw_ocr_adapter.py`
- `ocr-service/app/services/raw_ocr.py`
- Dockerfile / contracts / Java / legacy OCR routes

```text
POSTFREEZE_03B_F_PRODUCTION_SOURCE_DELTA
= 1_BOUNDED_ENGINEERING_COMPOSITION_MODULE
```

## 5. Engineering composition module

Path: `packages/python_runtime/http/engineering_raw_ocr.py`

Classification: `NON_PRODUCTION_ENGINEERING_OPT_IN_COMPOSITION`

Factory:

```text
create_engineering_raw_ocr_app(
    raw_ocr_tool_port: ContextToolPort,
    artifact_port: ArtifactPort,
) -> FastAPI
```

Internal wiring:

1. `ToolRouter`
2. register context tool `engineering.ocr.raw` / `0.0.1`
   with `require_artifact=True`, `artifact_only=True`
3. `DeterministicRuntimeExecutor(tool_router=..., artifact_port=...)`
4. existing `create_app(runtime_executor=..., authorized_capability_ids={engineering.ocr.raw})`

```text
ENGINEERING_OPT_IN_REQUIRES_EXPLICIT_FACTORY_CALL = YES
```

No module-level `app = ...`.
No uvicorn entrypoint for this composition.
No OCR implementation imports in this module.

Capability:

```text
capability_id = engineering.ocr.raw
capability_version = 0.0.1
operation (ToolContext) = RAW_OCR_RECOGNIZE
```

Authorized surface for opt-in app:

```text
ENGINEERING_OPT_IN_AUTHORIZED_CAPABILITIES = {"engineering.ocr.raw"}
```

## 6. Default Runtime surface

`packages/python_runtime/http/app.py` remains unchanged.

Default:

```text
DEFAULT_AUTHORIZED_CAPABILITIES = {"engineering.synthetic.runtime_smoke"}
```

Behavioral regression (authoritative test):

- default `create_app()` rejects ToolContext for `engineering.ocr.raw` at
  authorization boundary (`OPERATION_NOT_AUTHORIZED`)
- smoke remains authorized

```text
DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED = YES
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
```

## 7. Architecture guard narrow exception

Resolved path (unique LIVE):

```text
ARCHITECTURE_GUARD_TEST_PATH
= packages/python_runtime/tests/test_architecture_guards.py
```

Narrow allowance:

literal `engineering.ocr.raw` may appear only in:

`packages/python_runtime/http/engineering_raw_ocr.py`

Still forbidden across Runtime production tree (including the new module):

cv2 / PIL / pytesseract / app.services.raw_ocr / RawOcrEngine imports

Service production → Runtime import isolation unchanged.

```text
ENGINEERING_RAW_OCR_LITERAL_ALLOWED_LOCATION_COUNT = 1 production module
RUNTIME_CORE_OCR_IMPLEMENTATION_IMPORTS = 0
SERVICE_PRODUCTION_RUNTIME_IMPORT_DELTA = 0
```

## 8. Focused test / proof chain

File: `ocr-service/tests/test_engineering_opt_in_raw_ocr_runtime.py`

Stage-closing construction:

```text
RawOcrEngine
  -> RawOcrToolAdapter
  -> create_engineering_raw_ocr_app(...)   # repository factory REQUIRED
  -> ToolRouter + ArtifactPort + create_app
  -> POST /api/v1/runtime/tools/invoke
  -> HELLO / OCR
```

Artifact boundary:

```text
ToolContext -> InputRef -> StaticAllowlistArtifactPort.resolve
  -> ResolvedArtifact.content -> adapter
```

No storage_ref open. No URL fetch.
No language override. Default `chi_sim+eng`.
Synthetic in-memory `HELLO OCR 123`. No PHI.

## 9. CI strategy

New isolated job:

`ocr-runtime-engineering-opt-in-real-engine`

on `ubuntu-24.04`, Python 3.10, host apt Tesseract eng+chi_sim,
`AIDOCTOR_REQUIRE_REAL_TESSERACT=1`, focused pytest only.

Preserved existing jobs including `ocr-docker-raw-engine` and
`ocr-runtime-real-engine`.

Expected matrix size: 10 jobs.

Authoring-time pointer (F004 preserved):

```text
CI_VERIFIED = PENDING_EXACT_HEAD_CI
```

## 10. Pre-CI evidence state

```text
ENGINEERING_OPT_IN_RAW_OCR_RUNTIME_COMPOSITION_VERIFIED = NO
ENGINEERING_OPT_IN_RAW_OCR_RUNTIME_HTTP_CI = NO
ENGINEERING_OPT_IN_RAW_OCR_DEFAULT_SURFACE_REGRESSION_CI = NO
ENGINEERING_OPT_IN_RAW_OCR_REAL_ENGINE_CI = NO
```

After successful fresh exact-Head CI but before independent review:

```text
ENGINEERING_OPT_IN_RAW_OCR_RUNTIME_COMPOSITION_VERIFIED
= PENDING_INDEPENDENT_REVIEW
ENGINEERING_OPT_IN_RAW_OCR_RUNTIME_HTTP_CI
= VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
ENGINEERING_OPT_IN_RAW_OCR_DEFAULT_SURFACE_REGRESSION_CI
= VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
ENGINEERING_OPT_IN_RAW_OCR_REAL_ENGINE_CI
= VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
```

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

`REPOSITORY_RUNTIME_VERIFIED` remains NO because this stage proves a
repository-defined factory exercised in-process via TestClient HTTP, not a
standalone repository process/startup path.

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
```

Never write: `A7 = CLOSED`.

## 13. Stop state

```text
READY_FOR_POSTFREEZE_03B_F_INDEPENDENT_REVIEW = YES   # only after green CI
READY_FOR_POSTFREEZE_03B_F_PREMERGE_VERIFICATION = NO
MERGE_AUTHORIZATION_GRANTED = NO
POSTFREEZE_03B_F_DURABLY_CLOSED = NO
```

STOP after implementation report. Wait for independent review.
Do not mark Ready, merge, PMV, or start 03B-G.
