# 中文：三语绑定的 Python 侧一致性测试。语义裁决仍走现有 Shared Contract validator。

from __future__ import annotations

import copy
import sys
from pathlib import Path

import pytest
from pydantic import ValidationError

BINDINGS_ROOT = Path(__file__).resolve().parents[1]
CONTRACTS_V1 = BINDINGS_ROOT.parents[1]

sys.path.insert(0, str(BINDINGS_ROOT))
sys.path.insert(0, str(CONTRACTS_V1 / "validator"))

from aidoctor_shared_contracts import (  # noqa: E402
    CONTRACT_VERSION,
    SCHEMA_NAMES,
    VERSION_NEGOTIATION,
    model_for,
)
from aidoctor_shared_contracts.models import dump_binding  # noqa: E402
from validate_contracts import (  # noqa: E402
    build_registry,
    load_package,
    mutate,
    semantic_errors,
    validator_for,
)


def _package():
    return load_package()


def test_manifest_inventory_matches_bindings():
    manifest, schemas, valid, _invalid = _package()
    assert manifest["contract_version"] == CONTRACT_VERSION == "1.0.0"
    assert manifest["version_negotiation"] == VERSION_NEGOTIATION == "EXACT"
    names = [item["name"] for item in manifest["contracts"]]
    assert names == list(SCHEMA_NAMES)
    assert len(names) == 13
    for item in manifest["contracts"]:
        assert (CONTRACTS_V1 / item["path"]).is_file()
        assert item["name"] in schemas
        assert item["id"].endswith(item["path"].split("/")[-1])


def test_valid_fixtures_round_trip_through_python_and_oracle():
    manifest, schemas, valid, _invalid = _package()
    registry = build_registry(schemas)
    assert set(valid) >= set(SCHEMA_NAMES)
    for schema_name in SCHEMA_NAMES:
        original = valid[schema_name]
        parsed = model_for(schema_name).model_validate(original)
        dumped = dump_binding(parsed)
        errors = list(validator_for(schema_name, schemas, registry).iter_errors(dumped))
        assert errors == [], "%s structural errors: %s" % (schema_name, errors)
        assert semantic_errors(schema_name, dumped) == []
        assert dumped["contract_version"] == "1.0.0"


def test_unknown_contract_version_fails_closed():
    _manifest, _schemas, valid, _invalid = _package()
    payload = copy.deepcopy(valid["TraceRef"])
    payload["contract_version"] = "9.9.9"
    with pytest.raises(ValidationError):
        model_for("TraceRef").model_validate(payload)


def test_invalid_enum_fails_closed():
    _manifest, _schemas, valid, _invalid = _package()
    payload = copy.deepcopy(valid["TraceRef"])
    payload["trace_type"] = "NOT_A_TRACE_TYPE"
    with pytest.raises(ValidationError):
        model_for("TraceRef").model_validate(payload)


def test_missing_required_field_fails_closed():
    _manifest, _schemas, valid, _invalid = _package()
    payload = copy.deepcopy(valid["AuditRef"])
    del payload["audit_id"]
    with pytest.raises(ValidationError):
        model_for("AuditRef").model_validate(payload)


def test_state_patch_is_not_commit_result():
    _manifest, _schemas, valid, _invalid = _package()
    patch = model_for("StatePatch").model_validate(valid["StatePatch"])
    commit = model_for("CommitResult").model_validate(valid["CommitResult"])
    patch_dump = dump_binding(patch)
    commit_dump = dump_binding(commit)
    assert patch_dump["envelope"]["contract_name"] == "StatePatch"
    assert commit_dump["envelope"]["contract_name"] == "CommitResult"
    assert "status" not in patch_dump
    assert commit_dump["status"] == "COMMITTED"
    assert "operations" in patch_dump
    assert "operations" not in commit_dump


def test_tool_result_uses_v1_status_and_suggested_patches():
    _manifest, _schemas, valid, _invalid = _package()
    result = dump_binding(model_for("ToolResult").model_validate(valid["ToolResult"]))
    assert result["status"] == "SUCCEEDED"
    assert "suggested_writes" not in result
    assert isinstance(result["suggested_patches"], list)
    assert result["suggested_patches"][0]["envelope"]["contract_name"] == "StatePatch"


def test_invalid_fixtures_fail_binding_or_oracle():
    """结构错误由绑定拒绝；VALIDATOR_ENFORCED 规则仍由现有 oracle 拒绝。"""
    _manifest, schemas, valid, invalid = _package()
    registry = build_registry(schemas)
    assert len(invalid) >= 1
    for case in invalid:
        mutated = mutate(valid[case["base"]], case["mutation"])
        schema_name = case["schema"]
        rejected_by_binding = False
        try:
            parsed = model_for(schema_name).model_validate(mutated)
            dumped = dump_binding(parsed)
        except ValidationError:
            rejected_by_binding = True
        if rejected_by_binding:
            continue
        structural = list(validator_for(schema_name, schemas, registry).iter_errors(dumped))
        if structural:
            continue
        semantic = semantic_errors(schema_name, dumped)
        assert semantic, "invalid fixture must fail: %s" % case


def test_legacy_tool_context_shape_is_rejected():
    legacy = {
        "trace_id": "trace-1",
        "cdp_reference": {"cdp_id": "1001", "version": 1, "read_fields": []},
        "agent_state_summary": {"current_step": 1, "work_mode": "clinical_mode"},
        "constraints": {"max_time_seconds": 30, "max_cost": 1.0},
        "call_params": {},
    }
    with pytest.raises(ValidationError):
        model_for("ToolContext").model_validate(legacy)
