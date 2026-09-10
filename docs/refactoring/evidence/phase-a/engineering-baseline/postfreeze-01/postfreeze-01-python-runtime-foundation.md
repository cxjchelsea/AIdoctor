# POSTFREEZE-01 Python Runtime Foundation Evidence

> Dated: 2026-08-18
>
> Classification: `BOUNDED NON-CLINICAL IMPLEMENTATION`
>
> Batch: `POSTFREEZE-01`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_01_PYTHON_RUNTIME_FOUNDATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `16864a4ddd959a31677c36bdf72fde8552eaef8e`
>
> Authorized Base tree: `0d4a12bcf8a9bd5d0f1f81be2fda8290694343fd`
>
> Frozen engineering snapshot:
> `840a6fc7f83cec4fda6d53d739d7fd6b8013f031` /
> `d68d96d49eda96fe93dfb0c6c013b961a8e4b966`

```text
Creating packages/python_runtime
!=
Python production orchestration enabled
!=
clinical Agent runtime
!=
provider inference
!=
State Committer
!=
contracts runtime migration complete
```

## 1. Frozen decisions consumed

- ADR-02: checkpoint = execution metadata only; no SoR / State Committer
- ADR-03: consume Shared Contracts v1 `1.0.0` / `EXACT`; no production HTTP
- ADR-04: canonical package `packages/python_runtime`; consume-only Model Runtime
- ADR-05: LangGraph = 0 in this batch

## 2. Changed files

Exactly 12:

1. `packages/python_runtime/__init__.py`
2. `packages/python_runtime/ports.py`
3. `packages/python_runtime/protocol.py`
4. `packages/python_runtime/checkpoint.py`
5. `packages/python_runtime/tools.py`
6. `packages/python_runtime/model_runtime_port.py`
7. `packages/python_runtime/executor.py`
8. `packages/python_runtime/tests/test_architecture_guards.py`
9. `packages/python_runtime/tests/test_runtime_foundation.py`
10. `packages/python_runtime/tests/test_model_runtime_port.py`
11. `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-01/postfreeze-01-python-runtime-foundation.md`
12. `.github/workflows/ci.yml`（仅 `python-safety` 增加 Runtime 测试步骤）

Empty skeleton directories: **NO**.

## 3. Contract consumption

Import: `aidoctor_shared_contracts` via `PYTHONPATH=contracts/v1/bindings/python`

- `CONTRACT_VERSION == 1.0.0`
- `VERSION_NEGOTIATION == EXACT`
- invalid version FAIL_CLOSED
- schema copy: **NO**
- contracts/v1 semantic diff: **0**

Synthetic producer uses schema `serviceName` pattern `^[a-z][a-z0-9-]*$`:
`python-runtime-smoke`（不是带点号的展示名）。

`capability_id`: `engineering.synthetic.runtime_smoke`

## 4. Model Runtime consume-only

Consumed public API: `ModelGateway.prepare` / `validate_output` via
`GatewayModelRuntimePort`.

- `packages/model_runtime/**` modified: **0**
- provider invocation: **0**
- network: **0**
- A7-NC reopened: **NO**
- inference claimed: **NO**

## 5. Local test commands / results

Working directory: repository root.

```text
PYTHONPATH=contracts/v1/bindings/python
python -m pytest packages/python_runtime/tests -q
→ 21 passed

python -m pytest packages/model_runtime/tests -q
→ 301 passed

python -m pytest contracts/v1/tests contracts/v1/bindings/python/tests -q
→ 119 passed

python -m pytest capabilities/tests -q
→ 60 passed

git diff --check
→ clean
```

Architecture guards: PASS（反向依赖、provider SDK、capabilities、schema 复制、
LangGraph、State Committer、遗留服务导入、Java 源混入）。

## 6. Direct local smoke

Executed outside pytest:

```text
import packages.python_runtime
DeterministicRuntimeExecutor.execute(synthetic ContractEnvelope)
→ status=SUCCEEDED tool=synthetic-echo output=[{runtime_status: ok}]
suggested_patches=[]
```

Label: `RUNTIME_VERIFIED_LOCAL` only for this bounded in-process package
smoke. Not `REPOSITORY_RUNTIME_VERIFIED`.

## 7. CI

`python-safety` additive step:

```yaml
- name: Test Python Runtime foundation
  env:
    PYTHONPATH: contracts/v1/bindings/python
  run: python -m pytest packages/python_runtime/tests -q
```

Existing capability / Model Runtime steps preserved.
No new job. No new runtime dependency install.

Exact-head CI result is recorded after the Draft PR run completes.

## 8. Deltas

| Surface | Delta |
|---|---|
| Engineering Baseline V1 semantics | 0 |
| contracts/v1 | 0 |
| A7-NC / `packages/model_runtime` | 0 |
| Capability | 0 |
| Clinical content | 0 |
| Provider / network inference | 0 |
| PHI | 0 |
| Phase B | 0 |
| Java production | 0 |
| Legacy FastAPI services | 0 |
| LangGraph | 0 |

## 9. Evidence labels

```text
DOCUMENTED:                  YES
CODE_CONFIRMED:              YES
TEST_VERIFIED:               YES
CI_VERIFIED:                 PENDING_EXACT_HEAD_CI
RUNTIME_VERIFIED_LOCAL:      YES (bounded in-process package smoke only)
REPOSITORY_RUNTIME_VERIFIED: NO
CLINICAL_VALIDATED:          NO
PRODUCTION_VERIFIED:         NO
JAVA_PYTHON_CUTOVER_VERIFIED: NO
```

## 10. Known limitations

- No production HTTP / Java Feign wiring.
- No legacy service wrappers.
- `InMemoryCheckpointPort` is TEST_ONLY / NON_PRODUCTION.
- `ModelRuntimePort` does not invoke providers.
- Python Runtime is not the production orchestrator.
