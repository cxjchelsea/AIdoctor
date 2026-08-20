# POSTFREEZE-03B-H Java → Engineering Raw OCR Runtime Real-Tool Protocol

> Dated: 2026-08-20
>
> Classification: `BOUNDED NON_CLINICAL ENGINEERING IMPLEMENTATION` + `JAVA_REAL_TOOL_PROTOCOL_PROOF` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-H`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_H_JAVA_REAL_TOOL_PROTOCOL_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `e22ddd3608b26876de91c27dd61e40f23c5185f7`
>
> Authorized Base tree: `da7a315d693d800bd61e26bfe8e5766176fe1013`
>
> Implementation branch: `agent/postfreeze-03b-h-java-real-tool-protocol`
>
> Predecessor: `POSTFREEZE-03B-G = DURABLY_CLOSED`
> (merge `e22ddd…` / post-merge push CI `32339680043`)

```text
Java Shared Contracts ToolContext
→ real localhost HTTP
→ existing 03B-G engineering Raw OCR Runtime process
!=
POSTFREEZE-02 ContractEnvelope synthetic smoke
!=
Java/Python cutover
!=
ToolCaller / AgentLoop production wiring
!=
canonical / default Runtime raw OCR
!=
real Artifact storage
!=
Docker Runtime
!=
OCR accuracy / Chinese quality / clinical validation
```

```text
POSTFREEZE_03B_H_IMPLEMENTATION = IN_PROGRESS
JAVA_REAL_TOOL_PROTOCOL_VERIFIED = NO
JAVA_REAL_TOOL_PROTOCOL_CI = PENDING_EXACT_HEAD_CI
JAVA_PYTHON_CUTOVER_VERIFIED = NO
INDEPENDENT_REVIEW = NOT_PERFORMED
PMV = NOT_PERFORMED
MERGE_AUTHORIZATION_GRANTED = NO
DURABLY_CLOSED = NO
F004 = PRESERVED
REPOSITORY_RUNTIME_VERIFIED = YES
```

## 1. Authorization

- Token: `POSTFREEZE_03B_H_JAVA_REAL_TOOL_PROTOCOL_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-h-java-real-tool-protocol`
- Authorized exact Base: `e22ddd3608b26876de91c27dd61e40f23c5185f7`
- Authorized Base tree: `da7a315d693d800bd61e26bfe8e5766176fe1013`
- Live preflight Enterprise HEAD: `e22ddd3608b26876de91c27dd61e40f23c5185f7`
- Live preflight Enterprise tree: `da7a315d693d800bd61e26bfe8e5766176fe1013`
- Drift: none
- POSTFREEZE-03B-G: `DURABLY_CLOSED`
- 03B-G post-merge push CI: `32339680043` / push / success
  (historical predecessor only; not reused as 03B-H CI)

## 2. Exact evidence question

Can the repository's diagnosis-service `PythonRuntimeClient` send a
Shared Contracts v1 ToolContext containing InputRef only — with no
artifact bytes and no storage implementation knowledge — over real
localhost HTTP to the already-verified engineering-only Raw OCR Runtime
process, receive a real ToolResult produced through HTTP → Runtime
ToolContext parsing → authorization → ToolRouter →
`StaticAllowlistArtifactPort.resolve` → `RawOcrToolAdapter` →
`RawOcrEngine` → actual Tesseract, and deserialize that ToolResult back
into Shared Contracts Java bindings, while preserving the existing
ContractEnvelope synthetic protocol, default `create_app()` smoke-only
authorization, the 03B-G localhost-only process, StaticAllowlist,
legacy `OcrServiceClient`, and unwired ToolCaller / AgentLoop, without
enabling Java/Python cutover, canonical Runtime raw OCR, real Artifact
storage, Docker Runtime, Clinical Runtime, Production, or Phase B?

## 3. Why this stage

03B-G durably closed the repository-defined engineering localhost
process. POSTFREEZE-02 proved Java Feign can send a synthetic
`ContractEnvelope` to default `create_app()`. The missing hop is Java
sending a real `ToolContext` + `InputRef` to the engineering raw OCR
process.

```text
JAVA_REAL_TOOL_PROTOCOL_VERIFIED
!=
JAVA_PYTHON_CUTOVER_VERIFIED
```

## 4. Exact four-file scope

1. MODIFY `diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/PythonRuntimeClient.java`
2. NEW `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/PythonRuntimeRawOcrRealToolIT.java`
3. MODIFY `.github/workflows/ci.yml`
4. NEW this evidence document

```text
POSTFREEZE_03B_H_TOTAL_SCOPE = 4_FILES_EXACT
UNEXPECTED_FILES = 0
```

Unchanged (byte-identical to Base):

- `packages/python_runtime/http/app.py`
- `packages/python_runtime/http/transport.py`
- `packages/python_runtime/http/engineering_raw_ocr.py`
- `packages/python_runtime/artifacts.py`
- `packages/python_runtime/raw_ocr_adapter.py`
- `engineering/raw_ocr_runtime_process.py`
- `ocr-service/app/services/raw_ocr.py`
- `contracts/v1/bindings/java/src/main/java/com/aidoctor/contracts/v1/ToolTypes.java`
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/PythonRuntimeProtocolIT.java`
- ToolCaller / AgentLoop / `OcrServiceClient`

## 5. Java consumer

Existing `invoke(ContractEnvelope)` is preserved.

New method:

```text
invokeToolContext(traceId, cdpId, ToolTypes.ToolContext)
POST /api/v1/runtime/tools/invoke
```

```text
SHARED_CONTRACTS_JAVA_TOOLCONTEXT_USED = YES
LEGACY_DIAGNOSIS_TOOLCONTEXT_USED = NO
PARALLEL_RUNTIME_DTO_CREATED = NO
TRANSPORT_CONFIGURATION_CHANGED = NO
READ_TIMEOUT_MILLIS = 10000
Retryer.NEVER_RETRY
```

## 6. InputRef-only boundary

Java sends exactly one InputRef:

```text
ref_type = ARTIFACT
ref_id = artifact-engineering-raw-ocr-process-1
ref_version = 1
```

```text
JAVA_SENDS_INPUTREF_ONLY = YES
JAVA_SENDS_ARTIFACT_BYTES = NO
JAVA_KNOWS_ARTIFACT_STORAGE = NO
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED = NO
```

The 03B-G process continues to bootstrap a synthetic PNG via
`AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_BASE64` into
`StaticAllowlistArtifactPort`.

## 7. CI

New isolated job: `java-python-raw-ocr-real-tool`

```text
ubuntu-24.04
Python 3.11
JDK 8
AIDOCTOR_REQUIRE_REAL_TESSERACT = 1
launcher = engineering/raw_ocr_runtime_process.py
port = 8099
test = PythonRuntimeRawOcrRealToolIT
```

Existing `java-python-protocol` continues to run
`PythonRuntimeProtocolIT` against default `create_app()`.

Expected matrix: 12 jobs. Historical 11 jobs retained.

## 8. Evidence ceiling

```text
REPOSITORY_RUNTIME_VERIFIED = YES
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
OCR_DOCKER_RUNTIME_VERIFIED = NO
OCR_SERVICE_HTTP_IN_DOCKER_VERIFIED = NO
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED = NO
FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED = NO
JAVA_REAL_TOOL_PROTOCOL_VERIFIED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
REAL_IMAGE_OCR_VERIFIED = NO
CHINESE_OCR_QUALITY_VERIFIED = NO
OCR_ACCURACY_VERIFIED = NO
CLINICAL_VALIDATED = NO
PRODUCTION_VERIFIED = NO
LEGACY_OCR_ROUTE_MISMATCH = PRESERVED
DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED = YES
```

Authoring-time `JAVA_REAL_TOOL_PROTOCOL_CI = PENDING_EXACT_HEAD_CI`
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
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
Phase B = NOT_AUTHORIZED
PHASE_A_ENGINEERING_BASELINE = FROZEN / V1
Frozen commit = 840a6fc7f83cec4fda6d53d739d7fd6b8013f031
F001 = PRESERVED
F002 = CLOSED
F003 = CLOSED
F004 = PRESERVED
adult_respiratory_v1 = DRAFT / PARTIALLY_VALIDATED / REQUIRES_CLINICAL_REVIEW / NOT_IMPLEMENTED
```
