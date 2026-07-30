# Phase A2 Python runtime baseline

## Scope and status

- Base branch: `agent/enterprise-agent-refactoring-plan`
- Base SHA: `bff949a90ac87f61968bbc6e9e00ce19bd4cfd2c`
- Work branch: `agent/phase-a2-python-baseline`
- Overall status: `PARTIALLY_VALIDATED`
- Runtime services found: **10 FastAPI services**. Supporting libraries, offline scripts, and DRKnows research code are inventoried separately.
- Scope is evidence collection only. No Python/Java business code, requirements, database schema, Prompt Registry, shared contracts, or architecture implementation was changed.

## Evidence levels

| Level | Meaning |
|---|---|
| `DOCUMENTED` | Stated in project documentation only. |
| `CODE_CONFIRMED` | Confirmed by source/configuration inspection; no execution implied. |
| `BUILD_VERIFIED` | Real dependency resolution/install or build lifecycle succeeded; no import/startup implied. |
| `RUNTIME_VERIFIED` | A controlled runtime path was executed, such as `/health` or `/openapi.json`. |
| `DATA_VERIFIED` | Real data was inspected/validated. Not claimed by A2. |
| `TEST_VERIFIED` | A repeatable automated test with effective assertions passed. |
| `UNKNOWN` | Evidence was insufficient. |
| `BLOCKED` | A concrete dependency, configuration, external service, or code defect prevented the next verification depth. |

`BUILD_VERIFIED` does not mean import, startup, API, database, model, or clinical-flow success. Import and startup remain check stages whose results are recorded as PASS, FAIL, or BLOCKED; they are not separate Evidence Levels. `/health` and OpenAPI success do not mean business workflow or external dependencies are valid.

## Execution environment

- Host: Windows / PowerShell, Asia/Shanghai.
- Global `python`: 3.13.5; pip 26.1 (`D:\anaconda`). It was inspected but not mutated.
- Isolated base interpreter: Conda env `aidoctor`, Python 3.10.19.
- Isolated venv root: `%TEMP%\AIdoctor-A2-bff949a` (not committed).
- Docker CLI 28.1.1 was present, but daemon access/config was unavailable; Docker work is A3 and was not pursued.
- No service lock files were found; dependencies are plain requirements/setup metadata.
- Service sets were isolated as: `simple` (identical workup/treatment/risk requirements), `health`, `clinical`, `dialog`, `diagnosis`, `explanation`, `ocr`, `knowledge`.

Commands used for discovery included `python --version`, `python -m pip --version`, `where.exe python`, Conda environment inspection, repository scans for dependency/entrypoint files, and Compose inspection. No secret value was printed or persisted.

## Runtime service results

| Service | Dependency | Import | Startup | Health/API | Test | Evidence / blocker |
|---|---|---|---|---|---|---|
| health-state-assessment | PASS | FAIL | BLOCKED | BLOCKED | BLOCKED | common LLM fallback leaves `BaseLLM` undefined |
| clinical-parsing | PASS | PASS | PASS | `/health` 200; OpenAPI 200 | EXECUTED_NON_ASSERTION_SCRIPT | three synthetic parsing scenarios were executed and are `DOCUMENTED`; the script has no assertions and catches exceptions without failing the process |
| dialog | PASS | BLOCKED | BLOCKED | BLOCKED | N/A | `DEBUG=release` ambient collision; with `DEBUG=false`, Redis auth falls back, then missing `OPENAI_API_KEY` blocks import |
| diagnosis-engine | FAIL | BLOCKED | BLOCKED | BLOCKED | N/A | pip `ResolutionImpossible` after extensive LangChain backtracking; exact minimal conflict remains UNKNOWN |
| workup-planner | PASS | FAIL | BLOCKED | BLOCKED | N/A | `ServiceException` import does not exist |
| treatment-engine | PASS | PASS | PASS | `/health` 200; OpenAPI 200 | N/A | controlled runtime path only |
| risk-assessment | PASS | PASS | PASS | `/health` 200; OpenAPI 200 | N/A | controlled runtime path only |
| explanation | FAIL | BLOCKED | BLOCKED | BLOCKED | BLOCKED | direct Pydantic conflict; existing script requires live explanation and diagnosis services/CDP |
| OCR | PASS | FAIL | BLOCKED | BLOCKED | N/A | OpenCV 4.8.1.78 fails against resolved NumPy 2.2.6 ABI |
| knowledge-management | PASS | PASS | PASS | `/health` 200; OpenAPI 200 | BLOCKED | one placeholder pytest exists; `pytest` is not declared/installed |

Four services were started with hidden `uvicorn` processes, polled on their declared ports, and stopped. Ports 8082, 8091, 8092, and 8094 were confirmed closed afterward. The four-service start/health/OpenAPI/stop loop exited 0 in 5.4 seconds. No patient data and no billable model call were used.

## Dependency baseline

- Successful install plus `pip check`: simple, health, dialog, clinical, OCR, knowledge.
- Failed resolution: diagnosis and explanation.
- `dialog` resolved the broad common LLM ranges only after roughly 14 minutes of resolver backtracking, landing on LangChain 0.1.20, langchain-community 0.0.38, langchain-core 0.1.53, langchain-openai 0.1.5, OpenAI 1.10.0, and Pydantic 2.13.4.
- Pydantic declarations drift between `==2.5.0` and `>=2.7.0`; resolved successful environments therefore differ.
- OCR declares no NumPy constraint; pip resolved NumPy 2.2.6 and OpenCV import failed.
- The editable common LLM distribution installs metadata but the source layout is not directly importable as `aidoctor_llm` from the editable target. Service code adds `common` to `sys.path` as a fallback.
- No Flask, Gunicorn, SQLAlchemy (direct), LangGraph (declared), LlamaIndex, Anthropic, DashScope SDK, sentence-transformers, pymilvus, pgvector, Elasticsearch, FAISS, Chroma, Celery, Kafka, or RabbitMQ dependency was declared by the ten runtime services. DRKnows imports PyTorch/Transformers but has no reproducible dependency file.

See [python-dependency-inventory.csv](python-dependency-inventory.csv).

## Test assets and results

| Asset | Kind/count | Command/result | External dependencies |
|---|---|---|---|
| `clinical-parsing-service/test_service.py` | script; 3 scenarios | `python test_service.py`; exit 0 (`DOCUMENTED`, not `TEST_VERIFIED`) | local vocabulary only; the script has no assertions, catches each scenario exception and only prints errors, so exit 0 cannot prove the scenarios passed |
| `health-state-assessment-service/test_basic.py` | script; 3 scenarios | not entered; import blocker | common LLM import path |
| `explanation-service/test_explanation_service.py` | live HTTP script; 3 main calls | BLOCKED | running explanation + diagnosis services and a hard-coded CDP id |
| `knowledge-management-service/tests/test_importers.py` | pytest; 1 placeholder | `python -m pytest -q ...`; exit 1, `No module named pytest` | undeclared test runner |
| `DRKnows-main/test_stack*.py` | research execution scripts; 2 files | BLOCKED | absolute SapBERT/checkpoint/data paths and CUDA |

No mock provider, Prompt/RAG evaluation suite, golden set, or load test was found for the runtime services. A file named `test` is not treated as a passing automated test merely because it exists.

## Prompt and model call findings

- Prompt definitions/variants: **32** rows, counted per source definition/template key. This includes model prompts, fallbacks/questions, graph augmentation, and offline translation prompts. Duplicate definitions are intentionally separate rows.
- Model call/load points: **29** rows, counted per source call/load symbol. Providers include dynamic OpenAI/Ollama, ChatGLM/custom HTTP, Hugging Face/PyTorch research loads, and local Tesseract.
- No model call was executed. Provider credentials were recorded only by environment variable name.
- Governance risks: duplicate builtins, no registry/version field, broad LangChain ranges, caller-specific parsing, silent/empty fallbacks, and PHI transfer dependent on provider routing.

See [prompt-inventory.csv](prompt-inventory.csv) and [model-call-inventory.csv](model-call-inventory.csv).

## Tool and contract variants

- **156 Pydantic class definitions** were found across service/common Python source. No TypedDict/dataclass contract definition was found by the AST scope.
- Repeated `ToolContext`, `ToolResult`, `Evidence`, `SuggestedWrite`, and related classes are copied per service rather than imported from one shared contract.
- Both camelCase and snake_case fields exist; many boundaries contain `Dict[str, Any]`, untyped `dict`, or list/dict defaults.
- Pydantic 2.5.0 and >=2.7.0 declarations coexist. Runtime deprecation warnings and compatibility behavior are not equivalent to a shared schema.
- A2 does not create Shared Contracts v1; rows only nominate A5 candidates.

See [python-contract-variant-inventory.csv](python-contract-variant-inventory.csv).

## CDP access

- Eight services contain near-duplicate HTTP CDP readers targeting diagnosis-service.
- Eight services produce `ToolResult.suggested_writes`; these are proposals, not verified commits.
- dialog and explanation also contain direct HTTP POST writers with no version check/atomicity and error-log-and-continue behavior, creating a multi-writer risk.
- Common reader defaults frequently use `http://localhost:8080`, while the Java diagnosis service is documented/Composed on 8084.
- No CDP read/write was runtime-executed in A2.

See [python-cdp-access-inventory.csv](python-cdp-access-inventory.csv).

## Data and external dependencies

| Dependency | Users | Read/write | Startup/runtime | PHI/fallback |
|---|---|---|---|---|
| Redis | dialog; optional diagnosis/clinical config | dialog context read/write | dialog import attempted connection; auth failed then memory fallback | may contain dialogue/state; fallback can diverge |
| Neo4j | diagnosis-engine; knowledge-management | graph query/import | knowledge health starts without proving Neo4j API operations | clinical graph data; connection not validated |
| Milvus | Compose/environment references only | UNKNOWN | no Python `pymilvus` runtime dependency/service found | documented candidate, not code-confirmed runtime |
| diagnosis/CDP HTTP API | eight readers; two direct writers | CDP read/write | not exercised | possible PHI; empty/log-and-continue fallbacks |
| local CSV/JSON/YAML/XLSX/PDF | clinical parser, knowledge scripts/content | read/import | clinical subset exercised; production data not inspected | may contain medical content; A4 owns data validation |
| model providers | common LLM and offline scripts | external request | not exercised | possible external PHI transfer; provider-dependent |
| Tesseract | OCR | local image read | blocked before service import | image may contain PHI; local execution |

MySQL appears in Compose for Java services, not as a direct Python runtime dependency. PostgreSQL, Oracle, Elasticsearch, FAISS, Chroma, pgvector, and object storage were not confirmed as active dependencies of the ten runtime services.

## Reusable clinical assets

Candidate assets include seven normalization/ambiguity files, health red-flag/risk/severity/work-mode/wellness rules, dialog information-gap/fallback question sets, risk triage/upgrade rules, knowledge importers/content, DRKnows research code, and synthetic parsing cases. None is declared clinically valid by A2. Provenance, licensing, population, review, safety validation, and golden-set coverage remain required.

See [python-clinical-asset-inventory.csv](python-clinical-asset-inventory.csv).

## Verified facts, UNKNOWN, and BLOCKED

Verified facts:

- Ten FastAPI runtime services exist and all expose `/health` in source.
- Six isolated requirement sets installed and passed `pip check`; diagnosis and explanation did not resolve.
- clinical, treatment, risk, and knowledge imported, started, and returned 200 for health/OpenAPI.
- clinical's synthetic script execution across three local paths is `DOCUMENTED`; it does not constitute automated test verification.

UNKNOWN:

- The exact minimal diagnosis resolver conflict after broad LangChain backtracking.
- Production credentials/connectivity, clinical validity, real CDP compatibility, model output quality/cost, and production data correctness.
- Whether Milvus is deployed elsewhere; Compose references configuration but no active Python client/service was found.
- DRKnows reproducible environment, licenses, checkpoints, and data.

BLOCKED:

- Six service startup paths: health, dialog, diagnosis, workup, explanation, OCR.
- Knowledge pytest execution due missing test dependency.
- External provider, Redis, Neo4j business operations, CDP, and OCR/Tesseract paths were not validated.

## Document governance issues

- The task's three requested titles differ from README navigation; the actual files read were `prompt-and-model-runtime设计.md`, `成人呼吸道RAG.md`, and `capability-package规范.md`.
- Ten legacy English alias links at the tops of refactoring documents remain invalid. A2 records but does not broadly repair historical navigation.

## Commands and reproduction

Representative commands (all from the named service directory unless stated):

```powershell
D:\anaconda\envs\aidoctor\python.exe -m venv $env:TEMP\AIdoctor-A2-bff949a\<set>
$env:PYTHONUTF8='1'
<venv>\Scripts\python.exe -m pip install -r requirements.txt
<venv>\Scripts\python.exe -m pip check
<venv>\Scripts\python.exe -c "import app.main; print('IMPORT_OK')"
<venv>\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port <declared-port>
Invoke-WebRequest http://127.0.0.1:<port>/health
Invoke-WebRequest http://127.0.0.1:<port>/openapi.json
<clinical-venv>\Scripts\python.exe test_service.py
<knowledge-venv>\Scripts\python.exe -m pytest -q tests\test_importers.py
```

Install exits were 0 for simple/health/dialog/clinical/OCR/knowledge and 1 for diagnosis/explanation. Import exits were 0 for clinical/treatment/risk/knowledge and 1 for the other six. `DEBUG=false` was used only to isolate dialog's ambient-variable collision; secret variables were not set. Uvicorn processes were stopped by PID and ports were checked closed.

## Out of scope / follow-up candidates

- Do not fix diagnosis-service Java or Python service defects in A2.
- A3 owns frontend/Docker baseline; A4 owns real data validation; A5 owns Shared Contracts v1.
- Separate follow-ups should address Python locks/constraints, package layout, environment variable namespace collisions, import blockers, test dependencies, CDP multi-writer/version semantics, Prompt/model governance, and clinical asset validation.
