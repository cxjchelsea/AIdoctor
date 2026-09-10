from __future__ import annotations
import copy, sys
from pathlib import Path
import pytest
from pydantic import ValidationError
BINDINGS_ROOT = Path(__file__).resolve().parents[1]
RELEASE = BINDINGS_ROOT.parents[1]
sys.path.insert(0, str(BINDINGS_ROOT)); sys.path.insert(0, str(RELEASE / "validator"))
from aidoctor_shared_contracts import CONTRACT_VERSION, SCHEMA_NAMES, VERSION_NEGOTIATION, model_for
from aidoctor_shared_contracts.models import dump_binding
from validate_contracts import build_registry, load_package, mutate, semantic_errors, validator_for

def test_manifest_inventory_matches_bindings():
    manifest, schemas, valid, invalid = load_package()
    assert manifest["contract_version"] == CONTRACT_VERSION == "1.1.0"
    assert manifest["version_negotiation"] == VERSION_NEGOTIATION == "EXACT"
    assert [item["name"] for item in manifest["contracts"]] == list(SCHEMA_NAMES)
    assert len(SCHEMA_NAMES) == 16

def test_valid_fixtures_round_trip_through_python_and_oracle():
    _manifest, schemas, valid, _invalid = load_package(); registry = build_registry(schemas)
    for schema_name in SCHEMA_NAMES:
        parsed = model_for(schema_name).model_validate(valid[schema_name])
        dumped = dump_binding(parsed)
        assert list(validator_for(schema_name, schemas, registry).iter_errors(dumped)) == []
        assert semantic_errors(schema_name, dumped) == []

def test_unknown_contract_version_fails_closed():
    _m,_s,valid,_i=load_package(); payload=copy.deepcopy(valid["TraceRef"]); payload["contract_version"]="9.9.9"
    with pytest.raises(ValidationError): model_for("TraceRef").model_validate(payload)

def test_new_clinical_bindings_present():
    _m,_s,valid,_i=load_package()
    assert dump_binding(model_for("Encounter").model_validate(valid["Encounter"]))["envelope"]["contract_name"] == "Encounter"
    assert dump_binding(model_for("ClinicalObservation").model_validate(valid["ClinicalObservation"]))["value"]["kind"] == "TEXT"
    assert "test.observation.001" in dump_binding(model_for("ClinicalStateSnapshot").model_validate(valid["ClinicalStateSnapshot"]))["observations"]

def test_invalid_fixtures_fail_binding_or_oracle():
    _manifest, schemas, valid, invalid = load_package(); registry = build_registry(schemas)
    assert len(invalid) == 42
    for case in invalid:
        mutated = mutate(valid[case["base"]], case["mutation"]); schema_name = case["schema"]
        try:
            dumped = dump_binding(model_for(schema_name).model_validate(mutated))
        except ValidationError:
            continue
        if list(validator_for(schema_name, schemas, registry).iter_errors(dumped)):
            continue
        assert semantic_errors(schema_name, dumped), "invalid fixture must fail: %s" % case
