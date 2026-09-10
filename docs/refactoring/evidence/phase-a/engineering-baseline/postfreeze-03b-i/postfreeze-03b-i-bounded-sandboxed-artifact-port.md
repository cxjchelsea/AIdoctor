# POSTFREEZE-03B-I Bounded Sandboxed ArtifactPort Storage Backend

> Dated: 2026-08-20
>
> Classification: `BOUNDED NON_CLINICAL ENGINEERING IMPLEMENTATION` + `SANDBOXED LOCAL FILESYSTEM ARTIFACT STORAGE PROOF` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-I`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_I_BOUNDED_SANDBOX_ARTIFACT_BACKEND_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `468a204cffb91d7b72cc48b623655ad080c3d531`
>
> Authorized Base tree: `96d6d31a45a494b0c6e1ea4d423c47857772675f`
>
> Implementation branch: `agent/postfreeze-03b-i-bounded-sandbox-artifact-backend`
>
> Predecessor: `POSTFREEZE-03B-H = DURABLY_CLOSED`
> (merge `468a204…` / post-merge push CI `32344852597`)

```text
InputRef(ref_id, ref_version)
→ ArtifactPort.resolve
→ immutable SandboxedArtifactRecord mapping
→ sandbox-relative internal location
→ bounded local filesystem read
→ metadata / size / checksum / PHI / sandbox validation
→ ResolvedArtifact.content
→ existing RawOcrToolAdapter
→ existing RawOcrEngine
→ actual Tesseract
!=
S3 / MinIO / blob / network object storage
!=
arbitrary local filesystem access
!=
Java receiving or sending filesystem paths
!=
ref_id / storage_ref / original_filename as path
!=
env-base64 in-memory bootstrap
!=
canonical / default Runtime raw OCR
!=
Java/Python cutover
!=
Docker Runtime
!=
real-world / clinical / PHI image
!=
OCR accuracy / Chinese quality / clinical validation
```

```text
POSTFREEZE_03B_I_IMPLEMENTATION = IN_PROGRESS
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED = NO
REAL_ARTIFACT_STORAGE_BACKEND_CI = PENDING_EXACT_HEAD_CI
INDEPENDENT_REVIEW = NOT_PERFORMED
PMV = NOT_PERFORMED
MERGE_AUTHORIZATION_GRANTED = NO
DURABLY_CLOSED = NO
F004 = PRESERVED
REPOSITORY_RUNTIME_VERIFIED = YES
JAVA_REAL_TOOL_PROTOCOL_VERIFIED = YES
JAVA_PYTHON_CUTOVER_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
REAL_IMAGE_OCR_VERIFIED = NO
```

## 1. Authorization

- Token: `POSTFREEZE_03B_I_BOUNDED_SANDBOX_ARTIFACT_BACKEND_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-i-bounded-sandbox-artifact-backend`
- Authorized exact Base: `468a204cffb91d7b72cc48b623655ad080c3d531`
- Authorized Base tree: `96d6d31a45a494b0c6e1ea4d423c47857772675f`
- Live preflight Enterprise HEAD: `468a204cffb91d7b72cc48b623655ad080c3d531`
- Live preflight Enterprise tree: `96d6d31a45a494b0c6e1ea4d423c47857772675f`
- Drift: none
- POSTFREEZE-03B-H: `DURABLY_CLOSED`
- 03B-H post-merge push CI: `32344852597` / push / success
  (historical predecessor only; not reused as 03B-I CI)

## 2. Exact evidence question

Can the repository provide a bounded engineering-only ArtifactPort that
resolves a Shared Contracts InputRef identified only by `ref_id` and
`ref_version` from a real sandboxed local filesystem medium into
`ResolvedArtifact.content` through InputRef → ArtifactPort.resolve →
immutable internal record mapping → sandbox-relative location → bounded
local file read → metadata / size / checksum / PHI / sandbox validation
→ RawOcrToolAdapter → RawOcrEngine, while preserving that Java sends
InputRef only, Java never receives or sends filesystem paths, `ref_id` /
`storage_ref` / `original_filename` are never interpreted as filesystem
paths, RawOcrToolAdapter never opens storage, RawOcrEngine only receives
bytes, default Runtime remains smoke-only, and engineering Runtime
remains localhost-only?

## 3. Why this stage

03B-H durably closed Java Feign real-tool protocol against the
engineering process. The active artifact medium on that process was
still:

```text
env base64 → StaticAllowlistArtifactPort → bytes already in memory
```

That in-memory allowlist is now the dominant artificial boundary. This
stage replaces only the engineering-process composition medium with a
bounded sandboxed filesystem backend. `StaticAllowlistArtifactPort`
remains for synthetic foundation tests.

```text
REAL_ARTIFACT_STORAGE_BACKEND_IMPLEMENTED
!=
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED
!=
production object storage
!=
arbitrary filesystem access
```

## 4. Exact seven-file scope

1. MODIFY `packages/python_runtime/artifacts.py`
2. MODIFY `packages/python_runtime/ports.py`
3. MODIFY `engineering/raw_ocr_runtime_process.py`
4. NEW `packages/python_runtime/tests/test_sandboxed_artifact_backend.py`
5. MODIFY `ocr-service/tests/test_engineering_raw_ocr_runtime_process.py`
6. MODIFY `.github/workflows/ci.yml`
7. NEW this evidence document

```text
EXPECTED_FILES = 7
ACTUAL_FILES = 7
UNEXPECTED_FILES = 0
POSTFREEZE_03B_I_TOTAL_SCOPE = 7_FILES_EXACT
```

Unchanged (byte-identical to Base):

- `contracts/v1/**` including SourceArtifact schema and Java bindings
- `diagnosis-service/.../PythonRuntimeClient.java`
- `diagnosis-service/.../PythonRuntimeRawOcrRealToolIT.java`
- `diagnosis-service/.../ToolCaller.java`
- `diagnosis-service/.../AgentLoop.java`
- `diagnosis-service/.../OcrServiceClient.java`
- `packages/python_runtime/raw_ocr_adapter.py`
- `packages/python_runtime/http/app.py`
- `packages/python_runtime/http/transport.py`
- `packages/python_runtime/http/engineering_raw_ocr.py`
- `packages/python_runtime/executor.py`
- `packages/python_runtime/tool_router.py`
- `packages/python_runtime/tests/test_tool_input_foundation.py`
- `packages/python_runtime/tests/test_architecture_guards.py`
- `ocr-service/app/services/raw_ocr.py`
- `ocr-service/Dockerfile`
- `contracts/v1/validator/validate_contracts.py`

## 5. Sandboxed ArtifactPort design

```text
class = SandboxedFilesystemArtifactPort
record = SandboxedArtifactRecord(metadata, relative_path)
resolve(ref_id, ref_version) unchanged
MAX_ARTIFACT_BYTES = 2097152
allowed storage_ref scheme = artifact://
```

Internal mapping is private and immutable after construction:

```text
("artifact-engineering-raw-ocr-process-1", 1)
→ relative_path = "raw-ocr/input/artifact.png"
```

The relative path is a trusted composition constant. It is not derived
from `ref_id`, `storage_ref`, or `original_filename`.

```text
REF_ID_USED_AS_PATH = NO
STORAGE_REF_USED_AS_PATH = NO
ORIGINAL_FILENAME_USED_AS_PATH = NO
```

Sandbox root env:

```text
AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_SANDBOX_ROOT
```

Required, absolute, already existing directory. No fallback to
repository root, `/`, home, cwd, `/tmp`, or user profile.

Metadata-only env (no payload bytes, no launcher `read_bytes`):

```text
AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_EXPECTED_SIZE_BYTES
AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_EXPECTED_SHA256
```

Containment uses `Path.resolve(strict=True)` plus `relative_to`.
String `startswith` is not used. Any symlink component is rejected.

## 6. Engineering process migration

```text
OLD_ACTIVE_BACKEND = STATIC_ALLOWLIST_ENV_BASE64
NEW_ACTIVE_BACKEND = SANDBOXED_FILESYSTEM
ACTIVE_ENV_BASE64_ARTIFACT_BOOTSTRAP = NO
PROCESS_HOST = 127.0.0.1
ENGINEERING_RAW_OCR_ENV_BASE64_BOOTSTRAP_USED = NO
```

`create_engineering_raw_ocr_app`, `RawOcrToolAdapter`, and
`RawOcrEngine` remain the injected implementations. The composition
root does not read payload bytes.

The fixture is still a synthetic generated HELLO OCR PNG written into
an isolated temp sandbox. A real local file is not a real-world image.

```text
REAL_IMAGE_OCR_VERIFIED = NO
```

## 7. CI

New isolated job: `artifact-storage-sandboxed`

```text
ubuntu-24.04
Python 3.11
AIDOCTOR_REQUIRE_SANDBOX_SYMLINK_TEST = 1
no Tesseract install
tests = test_sandboxed_artifact_backend.py
        + test_architecture_guards.py
```

Existing `ocr-runtime-engineering-process-real-engine` and
`java-python-raw-ocr-real-tool` now bootstrap a real sandbox file
instead of env base64. Java IT source remains byte-identical.

Expected matrix: 13 jobs. Historical 12 jobs retained.

## 8. Evidence ceiling

```text
REPOSITORY_RUNTIME_VERIFIED = YES
JAVA_REAL_TOOL_PROTOCOL_VERIFIED = YES
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
OCR_DOCKER_RUNTIME_VERIFIED = NO
OCR_SERVICE_HTTP_IN_DOCKER_VERIFIED = NO
FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
REAL_IMAGE_OCR_VERIFIED = NO
CHINESE_OCR_QUALITY_VERIFIED = NO
OCR_ACCURACY_VERIFIED = NO
CLINICAL_VALIDATED = NO
PRODUCTION_VERIFIED = NO
LEGACY_OCR_ROUTE_MISMATCH = PRESERVED
DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED = YES
```

Authoring-time `REAL_ARTIFACT_STORAGE_BACKEND_CI = PENDING_EXACT_HEAD_CI`
is F004-preserving. Fresh GitHub Actions evidence belongs in the
implementation completion report, not a recursive docs-only commit.

## 9. Governance

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
PHASE_A_ENGINEERING_BASELINE = FROZEN / V1
Frozen commit = 840a6fc7f83cec4fda6d53d739d7fd6b8013f031
Frozen tree = d68d96d49eda96fe93dfb0c6c013b961a8e4b966
F001 = PRESERVED
F002 = CLOSED
F003 = CLOSED
F004 = PRESERVED
adult_respiratory_v1 = DRAFT / PARTIALLY_VALIDATED / REQUIRES_CLINICAL_REVIEW / NOT_IMPLEMENTED
```
