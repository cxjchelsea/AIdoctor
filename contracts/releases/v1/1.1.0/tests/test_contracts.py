import copy
import importlib.util
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
VALIDATOR = ROOT / "validator" / "validate_contracts.py"

spec = importlib.util.spec_from_file_location("contracts_1_1_validator", VALIDATOR)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


def valid(name):
    return copy.deepcopy(validator.load_package()[2][name])


def errors(name, instance):
    return validator.validate_contract_instance(name, instance)


def test_complete_package_validation():
    assert validator.validate_package() == {"schemas": 16, "valid": 16, "invalid": 42}


def test_manifest_governance_fields():
    manifest, schemas, _, _ = validator.load_package()
    assert manifest["contract_version"] == "1.1.0"
    assert manifest["schema_family"] == "v1"
    assert manifest["version_negotiation"] == "EXACT"
    assert manifest["supported_versions"] == ["1.1.0"]
    assert len(manifest["contracts"]) == 16
    assert "ObservationCandidate" not in {item["name"] for item in manifest["contracts"]}
    assert all(schema["$id"].startswith("https://schemas.aidoctor.dev/contracts/1.1.0/") for schema in schemas.values())


@pytest.mark.parametrize("name", ["Encounter", "ClinicalStateSnapshot", "ClinicalObservation"])
def test_new_contracts_are_exact_1_1(name):
    instance = valid(name)
    assert instance["contract_version"] == "1.1.0"
    assert not errors(name, instance)


def test_exact_version_rejects_raw_1_0_payload():
    instance = valid("Encounter")
    instance["contract_version"] = "1.0.0"
    instance["envelope"]["contract_version"] = "1.0.0"
    assert any("contract_version is not supported" in error or "schema:const" in error for error in errors("Encounter", instance))


def test_frozen_1_0_validator_rejects_1_1_payload_name():
    old_path = Path("contracts/v1/validator/validate_contracts.py").resolve()
    old_spec = importlib.util.spec_from_file_location("contracts_1_0_validator_for_negative", old_path)
    old_validator = importlib.util.module_from_spec(old_spec)
    old_spec.loader.exec_module(old_validator)
    assert old_validator.validate_contract_instance("Encounter", valid("Encounter"))


def observation(kind, value):
    item = valid("ClinicalObservation")
    item["observation_id"] = "test.observation." + kind.lower()
    item["value"] = value
    return item


@pytest.mark.parametrize("value", [
    {"kind":"TEXT", "value":"alpha"},
    {"kind":"NUMBER", "value":42},
    {"kind":"BOOLEAN", "value":True},
    {"kind":"CODED", "code":"example-code", "system":"example-system", "display":"alpha"},
    {"kind":"QUANTITY", "value":42, "unit":"unit-x"},
    {"kind":"REFERENCE", "reference_type":"example", "reference_id":"test.reference.001"},
])
def test_observation_value_variants(value):
    item = valid("ClinicalObservation")
    item["value"] = value
    assert not errors("ClinicalObservation", item)


@pytest.mark.parametrize("bad_value", [None, [], {"kind":"UNKNOWN", "value":"alpha"}, {"kind":"CODED", "code":"example-code"}, {"kind":"QUANTITY", "value":42}, {"kind":"REFERENCE", "reference_id":"test.reference.001"}])
def test_observation_value_rejects_deferred_or_incomplete_values(bad_value):
    item = valid("ClinicalObservation")
    item["value"] = bad_value
    assert errors("ClinicalObservation", item)


def canonical_patch(op="ADD"):
    obs = valid("ClinicalObservation")
    patch = valid("StatePatch")
    patch.pop("cdp_id")
    patch["encounter_id"] = obs["encounter_id"]
    patch["operations"] = [{"op":op,"path":"/observations/" + obs["observation_id"],"source":"PATIENT_FACT","sensitivity":obs["sensitivity"]}]
    if op in {"ADD", "REPLACE"}:
        patch["operations"][0]["value"] = obs
    return patch


@pytest.mark.parametrize("op", ["ADD", "REPLACE", "REMOVE"])
def test_canonical_observation_patch_operations(op):
    assert not errors("StatePatch", canonical_patch(op))


def test_protected_looking_observation_id_is_identity_not_field():
    patch = canonical_patch("ADD")
    patch["operations"][0]["path"] = "/observations/status"
    patch["operations"][0]["value"]["observation_id"] = "status"
    assert not errors("StatePatch", patch)


@pytest.mark.parametrize("mutation", [
    lambda p: p.update({"cdp_id":"test.cdp.001"}),
    lambda p: p["operations"].append({"op":"REPLACE","path":"/patient_state/alpha","value":"alpha","source":"PATIENT_FACT","sensitivity":"INTERNAL"}),
    lambda p: p["operations"][0].update({"path":"/observations/test.observation.001/value"}),
    lambda p: p["operations"][0].update({"expected_current_value":"alpha"}),
    lambda p: p["operations"][0].update({"op":"TEST"}),
    lambda p: p["operations"][0]["value"].update({"observation_id":"test.observation.other"}),
    lambda p: p["operations"][0]["value"].update({"encounter_id":"test.encounter.other"}),
    lambda p: p["operations"][0].update({"sensitivity":"PUBLIC"}),
])
def test_canonical_patch_cross_rule_rejections(mutation):
    patch = canonical_patch("ADD")
    mutation(patch)
    assert errors("StatePatch", patch)


def test_legacy_statepatch_semantic_subset_reissued_as_1_1():
    patch = valid("StatePatch")
    assert patch["cdp_id"] == "test.cdp.001"
    assert not errors("StatePatch", patch)


def test_canonical_commit_result_target():
    result = valid("CommitResult")
    result.pop("cdp_id")
    result["encounter_id"] = "test.encounter.001"
    assert not errors("CommitResult", result)
    result["cdp_id"] = "test.cdp.001"
    assert errors("CommitResult", result)


def test_snapshot_key_and_encounter_identity_rules():
    snapshot = valid("ClinicalStateSnapshot")
    assert not errors("ClinicalStateSnapshot", snapshot)
    bad = valid("ClinicalStateSnapshot")
    bad["observations"]["test.observation.other"] = bad["observations"].pop("test.observation.001")
    assert errors("ClinicalStateSnapshot", bad)
    bad = valid("ClinicalStateSnapshot")
    bad["observations"]["test.observation.001"]["encounter_id"] = "test.encounter.other"
    assert errors("ClinicalStateSnapshot", bad)


def test_encounter_lifecycle_time_rules():
    encounter = valid("Encounter")
    assert not errors("Encounter", encounter)
    encounter["closed_at"] = "2026-09-09T05:02:00Z"
    assert errors("Encounter", encounter)
    closed = valid("Encounter")
    closed["lifecycle_status"] = "CLOSED"
    assert errors("Encounter", closed)
    closed["closed_at"] = "2026-09-09T05:02:00Z"
    assert not errors("Encounter", closed)
