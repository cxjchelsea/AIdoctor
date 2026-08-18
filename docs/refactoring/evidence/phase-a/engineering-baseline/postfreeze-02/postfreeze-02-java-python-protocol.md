# POSTFREEZE-02 Java ↔ Python Runtime Protocol / Adapter Evidence

> Dated: 2026-08-19
>
> Classification: `BOUNDED NON-CLINICAL IMPLEMENTATION` + `CROSS-LANGUAGE REAL-WIRE TEST` + `CI INTEGRATION` + `DRAFT PR`
>
> Batch: `POSTFREEZE-02`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_02_JAVA_PYTHON_PROTOCOL_ADAPTER_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `2b2467f32539d02a2171167cabc6f34161b42645`
>
> Authorized Base tree: `06d39776ecfef85cd1472c570d86112c8ace1a4b`
>
> Frozen engineering snapshot:
> `840a6fc7f83cec4fda6d53d739d7fd6b8013f031` /
> `d68d96d49eda96fe93dfb0c6c013b961a8e4b966`

```text
Creating PythonRuntimeClient
!=
Java production cutover
!=
legacy route repair
!=
OCR wrapper
!=
clinical Runtime
!=
State Committer
!=
Phase B
```

## 1. Authorization

- Token: `POSTFREEZE_02_JAVA_PYTHON_PROTOCOL_ADAPTER_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-02-java-python-protocol-adapter`
- POSTFREEZE-01: `DURABLY_CLOSED`
- Push CI 32121803671: completed / success
- PR #57: remains MERGED at the authorized Base
- Drift: none

## 2. Frozen decisions consumed

- ADR-02: HTTP success != SoR commit; checkpoint != clinical authority; State Committer not in scope
- ADR-03: public Java↔Python Runtime protocol = HTTP/REST + JSON; semantic authority = contracts/v1; unknown version FAIL_CLOSED; SSE/WebSocket not control plane
- ADR-04: public Python Runtime HTTP adapter owned by `packages/python_runtime`; legacy FastAPI remains CONTAINMENT
- ADR-05: LangGraph not authorized; Java remains current production orchestration authority

## 3. Changed files

Exactly 14:

NEW:

1. `packages/python_runtime/http/__init__.py`
2. `packages/python_runtime/http/app.py`
3. `packages/python_runtime/http/routes.py`
4. `packages/python_runtime/http/transport.py`
5. `packages/python_runtime/requirements-http.txt`
6. `packages/python_runtime/tests/test_http_adapter.py`
7. `packages/python_runtime/tests/fixtures/runtime_protocol_invoke.json`
8. `diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/PythonRuntimeClient.java`
9. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/PythonRuntimeProtocolIT.java`
10. `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-02/postfreeze-02-java-python-protocol.md`

MODIFY:

11. `packages/python_runtime/executor.py`
12. `packages/python_runtime/tests/test_architecture_guards.py`
13. `diagnosis-service/pom.xml`
14. `.github/workflows/ci.yml`

Unexpected: 0

## 4. HTTP dependencies

Authorized versions resolved cleanly with existing `pydantic==2.10.3` on Python 3.12:

- `fastapi==0.104.1`
- `uvicorn[standard]==0.24.0`
- `httpx==0.26.0`

No provider SDK, LangGraph, database driver, vector library, or clinical NLP added.
No `pyproject.toml` / `setup.py`.

Compatibility: PASS for this exact bounded set. TestClient smoke: PASS.

## 5. Canonical Runtime HTTP surface

- Package owner: `packages/python_runtime/http`
- App: `create_app(...)` + module-level `app`
- Uvicorn entry: `packages.python_runtime.http.app:app`
- Prefix: `/api/v1/runtime`
- Health: `GET /api/v1/runtime/health`
- Invoke: `POST /api/v1/runtime/tools/invoke`
- Production classification: `NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF`
- Import does not start network services
- Authorized capability only: `engineering.synthetic.runtime_smoke`

## 6. Contract / version / identity / errors

- Binding: `aidoctor_shared_contracts.ContractEnvelope` / `ToolResult`
- Version: `1.0.0` / `VERSION_NEGOTIATION=EXACT`
- Other versions including `2.0.0`: FAIL_CLOSED as `CONTRACT_VERSION_MISMATCH`
- Schema copy: NO
- contracts/v1 semantic diff: 0
- Success semantic type: Shared Contracts `ToolResult`
- Transport error type: transport-local envelope (`error_code`, `retryable`, `correlation_id`, `trace_id`, `message`)
- `X-Trace-Id` required and must equal body `trace_id`
- Missing or mismatched trace: `IDENTITY_MISMATCH` / 4xx
- `X-CDP-Id` optional synthetic echo only
- PHI: 0

Distinguishable codes:

- `CONTRACT_VERSION_MISMATCH` → 400
- `ENVELOPE_INVALID` → 400
- `IDENTITY_MISMATCH` → 400
- `OPERATION_NOT_AUTHORIZED` → 403

No HTTP 200 for protocol failures.

## 7. Runtime executor / F003

- HTTP routes call `DeterministicRuntimeExecutor.execute` via `RuntimeExecutor` port
- Public constructor now types `ToolPort` / `CheckpointPort`
- Default synthetic `FakeToolPort` preserved
- Behavior expansion: 0
- Production orchestration claim: 0

## 8. Architecture guards / F002

- F002 corrected to an exact approved module set
- Approved: `packages.model_runtime`, `packages.model_runtime.gateway`, `packages.model_runtime.gateway.models` (F001 debt)
- Negative proof: `packages.model_runtime.gateway.some_private_module` rejected
- Provider imports: 0
- LangGraph imports: 0
- capabilities imports: 0
- State Committer imports: 0
- legacy-service imports: 0
- model_runtime reverse dependency: 0

## 9. Java Feign consumer

- Coordinates: `com.aidoctor:shared-contracts:1.0.0`
- diagnosis-service dependency added for the new canonical client only
- Existing Tool DTO migration: 0
- Existing production callers changed: 0
- Path: `diagnosis-service/src/main/java/com/aidoctor/diagnosis/client/PythonRuntimeClient.java`
- Feign: Spring Cloud OpenFeign
- Request: `FoundationTypes.ContractEnvelope`
- Response: `ResponseEntity<ToolTypes.ToolResult>`
- Headers: required `X-Trace-Id`; optional `X-CDP-Id`
- Connect timeout: 5000 ms
- Read timeout: 10000 ms
- Retry: `Retryer.NEVER_RETRY` via nested `TransportConfiguration` (no extra config file)
- Production caller count: 0

## 10. Golden fixture

- Path: `packages/python_runtime/tests/fixtures/runtime_protocol_invoke.json`
- Valid Shared Contracts v1: YES
- Synthetic / non-PHI / nonclinical: YES
- Shared by Java and Python: YES
- Java resources copy: NO

## 11. Real-wire topology

```text
Java PythonRuntimeClient (OpenFeign)
→ actual HTTP 127.0.0.1:8099
→ packages.python_runtime.http façade
→ Shared Contracts v1 validation
→ DeterministicRuntimeExecutor / FakeToolPort
→ ToolResult
→ actual HTTP response
→ Java deserialization
```

Mocks substituted: NO

Local real-wire log:

```text
Uvicorn running on http://127.0.0.1:8099
GET /api/v1/runtime/health HTTP/1.1 200 OK
POST /api/v1/runtime/tools/invoke HTTP/1.1 200 OK
POST /api/v1/runtime/tools/invoke HTTP/1.1 200 OK
POST /api/v1/runtime/tools/invoke HTTP/1.1 400 Bad Request
```

`mvn -f diagnosis-service/pom.xml -Dtest=PythonRuntimeProtocolIT test`

- Tests run: 2
- Failures: 0
- Valid case: PASS
- Wrong-version `2.0.0` / `CONTRACT_VERSION_MISMATCH`: PASS
- Trace echo: PASS

## 12. Local tests

| Check | Result |
| --- | --- |
| `git diff --check` | PASS |
| `python -m pytest packages/python_runtime/tests -q` | 34 passed |
| `python -m pytest packages/model_runtime/tests -q` | 301 passed |
| `python -m pytest capabilities/tests -q` | 60 passed |
| contract Python tests | 119 passed |
| `mvn -f contracts/v1/bindings/java/pom.xml test` | 7 passed |
| `mvn -f contracts/v1/bindings/java/pom.xml install -DskipTests` | PASS |
| `mvn -f diagnosis-service/pom.xml test` | 44 passed (IT not in default Surefire `*Test` set) |
| local real-wire `PythonRuntimeProtocolIT` | 2 passed |

Python 3.10 claim: not made (`PYTHON_3_10_TEST_VERIFIED` = NO).

## 13. CI changes

- `contracts`: unchanged / not weakened
- `java`: add local `shared-contracts` install before diagnosis-service tests
- `python-safety`: add `requirements-http.txt` to cache + install; existing test steps retained
- `frontend`: unchanged
- `java-python-protocol`: new bounded real-wire job
- Weakening: 0

Exact-head CI run is observed on the Draft PR after push. Authoring-time label:

- `CI_VERIFIED`: PENDING_AT_AUTHORING

## 14. POSTFREEZE-01 debt

- F001: PRESERVED (`MODEL_RUNTIME_PUBLIC_IMPORT_HYGIENE_DEBT`; `ports.py` / `model_runtime_port.py` not edited)
- F002: CLOSED in this batch (exact allowlist + negative private-module proof)
- F003: CLOSED in this batch (`ToolPort` / `CheckpointPort` constructor typing)
- F004: PRESERVED (POSTFREEZE-01 evidence not rewritten)

## 15. Boundary deltas

| Surface | Delta |
| --- | --- |
| Engineering Baseline semantics | 0 |
| contracts/v1 | 0 |
| packages/model_runtime | 0 |
| A7-NC | 0 |
| capability | 0 |
| legacy services | 0 |
| Java orchestration | 0 |
| clinical | 0 |
| provider | 0 |
| network inference | 0 |
| PHI | 0 |
| Phase B | 0 |
| LangGraph | 0 |

Production orchestration calls `PythonRuntimeClient` = 0

## 16. Evidence truth

- `DOCUMENTED`: YES
- `CODE_CONFIRMED`: YES
- `TEST_VERIFIED`: YES
- `CI_VERIFIED`: PENDING_AT_AUTHORING
- `JAVA_PYTHON_PROTOCOL_TEST_VERIFIED`: YES (local Feign → real HTTP → Python Runtime → ToolResult → Java deserialize)
- `RUNTIME_VERIFIED_LOCAL`: YES
- `REPOSITORY_RUNTIME_VERIFIED`: NO
- `JAVA_PYTHON_CUTOVER_VERIFIED`: NO
- `CLINICAL_VALIDATED`: NO
- `PRODUCTION_VERIFIED`: NO

## 17. Non-claims

This batch does not claim:

- production routing or cutover
- legacy FastAPI migration
- OCR wrapper
- clinical Runtime
- repository-wide Runtime verification
- provider inference
- Phase B / State Committer
- A7 complete
- A11 passed
- Ready / merge
