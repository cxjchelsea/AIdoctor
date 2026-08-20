# POSTFREEZE-03C Runtime Hardening & Deployment Readiness

> Dated: 2026-08-20
>
> Classification: `BOUNDED NON_CLINICAL ENGINEERING IMPLEMENTATION` + `RUNTIME SEMANTIC GUARD` + `DEDICATED ENGINEERING CONTAINER PROCESS` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03C`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03C_RUNTIME_HARDENING_DEPLOYMENT_READINESS_EXPLICIT_IMPLEMENTATION_AUTHORIZATION_GRANTED`
>
> Authorized Base: `c9b21af5028177657df2e463fb9ba4cac5659316`
>
> Authorized Base tree: `ecd4c89ed9fc38682b167f9e030bd57a39933d11`
>
> Implementation branch: `agent/postfreeze-03c-runtime-hardening-deployment-readiness`
>
> Predecessor: `POSTFREEZE-03B-I = DURABLY_CLOSED`
> (merge `c9b21af…` / post-merge push CI `32350871400`)

```text
POSTFREEZE_03C_IMPLEMENTATION = IN_PROGRESS
FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED = NO
OCR_DOCKER_RUNTIME_VERIFIED = NO
OCR_SERVICE_HTTP_IN_DOCKER_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
INDEPENDENT_REVIEW = NOT_PERFORMED
PMV = NOT_PERFORMED
MERGE_AUTHORIZATION_GRANTED = NO
POSTFREEZE_03C_DURABLY_CLOSED = NO
F004 = PRESERVED
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED = YES
REPOSITORY_RUNTIME_VERIFIED = YES
JAVA_REAL_TOOL_PROTOCOL_VERIFIED = YES
DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED = YES
REAL_IMAGE_OCR_VERIFIED = NO
```

## 1. Evidence question

Can the repository run its already-verified engineering Raw OCR
Runtime as a contract-semantically guarded, dedicated-image,
container-executable engineering process that performs
container-internal localhost health and real-tool invocation
against a bounded sandbox artifact mount with real Tesseract,
while preserving the default Runtime capability surface,
127.0.0.1-only engineering bind, Java InputRef abstraction,
legacy OCR image/route, and all clinical / production / cutover
prohibitions?

## 2. Work packages

- WP-1 = Runtime Full Semantic Validation
- WP-2 = Dedicated Engineering Runtime Container Process

One consolidated PR. No 03C-A / 03C-B stages.

## 3. WP-1 design

Canonical request-level API:

`contracts/v1/validator/validate_contracts.py::validate_contract_instance(name, instance)`

performs:

- cached JSON Schema structural validation
- exact contract version validation
- `semantic_errors(name, instance)`

It does **not** call `validate_package()`.

Runtime HTTP:

1. existing Pydantic / identity parse
2. canonical validation of ToolContext or ContractEnvelope
3. authorization unchanged
4. executor
5. canonical validation of ToolResult before HTTP 200

Default authorized capability set is unchanged.
`engineering.ocr.raw` is not registered on default `create_app()`.

Authoring-time CI pointer (F004 preserved):

```text
FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_CI = PENDING_EXACT_HEAD_CI
```

## 4. WP-2 design

New dedicated image: `engineering/Dockerfile`

- `NON_PRODUCTION_ENGINEERING_RUNTIME`
- Python 3.11 slim
- actual Tesseract `eng` + `chi_sim`
- repo-root build context
- does **not** modify `ocr-service/Dockerfile`
- does **not** bind `0.0.0.0`
- does **not** EXPOSE / host-publish

Stage-closing proof: `engineering/docker_runtime_probe.py`

- starts `engineering/raw_ocr_runtime_process.py`
- container-internal `127.0.0.1`
- sandbox filesystem artifact
- no env-base64
- health + real ToolContext invoke
- real Tesseract
- synthetic HELLO / OCR tokens

Authoring-time CI pointer (F004 preserved):

```text
OCR_DOCKER_RUNTIME_CI = PENDING_EXACT_HEAD_CI
```

## 5. Evidence ceiling

03C implementation, even after later durable closure, does not mean:

- S3 / MinIO / production storage
- canonical Runtime raw OCR
- Java/Python cutover
- legacy OCR HTTP in Docker
- host publish / compose / Kubernetes
- real-world / clinical / PHI image
- OCR accuracy
- Production Runtime

```text
OCR_SERVICE_HTTP_IN_DOCKER_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
REAL_IMAGE_OCR_VERIFIED = NO
CLINICAL_VALIDATED = NO
PRODUCTION_VERIFIED = NO
LEGACY_OCR_ROUTE_MISMATCH = PRESERVED
```

## 6. Governance

```text
A7 = NOT_COMPLETE
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
Phase B = NOT_AUTHORIZED
PHASE_A_ENGINEERING_BASELINE = FROZEN
F001 = PRESERVED
F004 = PRESERVED
```
