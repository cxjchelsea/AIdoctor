import copy
import json
import re
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from validator.validate_contracts import (  # noqa: E402
    build_registry,
    load_package,
    semantic_errors,
    validate_new_delivery_release,
    validate_package,
    validator_for,
)


@pytest.fixture(scope="module")
def package():
    manifest, schemas, valid, invalid = load_package()
    return manifest, schemas, valid, invalid, build_registry(schemas)


def test_complete_package_validation():
    assert validate_package() == {"schemas": 13, "valid": 13, "invalid": 26}


def test_every_schema_has_required_metadata(package):
    for schema in package[1].values():
        assert schema["$schema"] == "https://json-schema.org/draft/2020-12/schema"
        assert schema["$id"].startswith("https://schemas.aidoctor.dev/contracts/v1/")
        assert schema["title"] and schema["description"]
        assert schema["type"] == "object" and schema["required"]
        assert schema["additionalProperties"] is False


def test_manifest_governance_fields(package):
    for item in package[0]["contracts"]:
        assert set(item) == {"name","id","path","compatibility","status","stability","patient_visible","owner"}
        assert item["stability"] in {"STABLE","EXPERIMENTAL"}


def test_prompt_required_field_coverage(package):
    required = {
        "ContractEnvelope":{"contract_name","contract_version","message_id","correlation_id","trace_id","created_at","producer","capability_id","capability_version"},
        "IdentifierSet":{"contract_version"},
        "StatePatch":{"envelope","cdp_id","base_version","patch_id","idempotency_key","operations","reason_code","evidence_refs","producer","created_at"},
        "CommitResult":{"patch_id","cdp_id","status","previous_version","conflicts","rejected_operations","audit_ref","retryable"},
        "ContractConflict":{"conflict_id","type","path","expected_version","actual_version","expected_value","actual_value","resolution","retryable","details"},
        "ToolContext":{"envelope","actor","identifiers","capability","current_state_ref","authorization_scope","deadline","locale","requested_operation","input_refs"},
        "ToolResult":{"envelope","tool_name","tool_version","invocation_id","status","output","suggested_patches","evidence_refs","errors","started_at","completed_at"},
        "EvidencePack":{"evidence_pack_id","knowledge_release_id","created_at","claims","sources","conflicts","limitations"},
        "SourceArtifact":{"artifact_id","artifact_type","owner_ref","content_type","original_filename","size_bytes","checksum","storage_ref","created_at","processing_status","derived_artifacts","sensitivity","retention_class"},
        "KnowledgeReleaseRef":{"knowledge_release_id","source_registry_version","released_at","status","source_refs","checksum_manifest_ref"},
        "TraceRef":{"trace_id","trace_type","trace_version","created_at","access_level","phi_capable"},
        "AuditRef":{"audit_id","audit_type","audit_version","created_at","access_level","phi_capable"},
        "PatientDeliveryView":{"delivery_id","delivery_version","cdp_id","review_status","generated_at","summary","safety_notice","recommended_actions","evidence_sections","limitations","follow_up","version_bindings"},
    }
    for name, fields in required.items():
        assert fields <= set(package[1][name]["required"]), name


def test_committed_version_advances(package):
    instance = copy.deepcopy(package[2]["CommitResult"])
    instance["committed_version"] = instance["previous_version"]
    assert semantic_errors("CommitResult", instance) == ["committed_version must be greater than previous_version"]


def test_non_committed_result_has_no_committed_version(package):
    instance = copy.deepcopy(package[2]["CommitResult"])
    instance["status"] = "REJECTED"
    errors = list(validator_for("CommitResult", package[1], package[4]).iter_errors(instance))
    assert any(error.validator == "not" for error in errors)


def test_medical_claim_requires_citation_or_insufficient_evidence(package):
    instance = copy.deepcopy(package[2]["EvidencePack"])
    instance["claims"][0]["citation_refs"] = []
    assert any(error.validator == "minItems" for error in validator_for("EvidencePack", package[1], package[4]).iter_errors(instance))
    instance["claims"][0]["support_status"] = "INSUFFICIENT_EVIDENCE"
    assert not list(validator_for("EvidencePack", package[1], package[4]).iter_errors(instance))


def test_safety_rule_does_not_borrow_medical_evidence(package):
    instance = copy.deepcopy(package[2]["EvidencePack"])
    safety = instance["claims"][1]
    safety["support_status"] = "SUPPORTED"
    assert "safety rules do not inherit medical-evidence support" in semantic_errors("EvidencePack", instance)
    safety["citation_refs"] = ["source-1"]
    assert "safety rule citations must resolve to RULE sources" in semantic_errors("EvidencePack", instance)


def test_withdrawn_release_rejected_for_new_delivery(package):
    release = copy.deepcopy(package[2]["KnowledgeReleaseRef"])
    release.update(status="WITHDRAWN", withdrawn_at="2026-08-03T00:00:00Z")
    assert validate_new_delivery_release(package[2]["PatientDeliveryView"], release) == ["withdrawn knowledge release cannot be used for a new delivery"]


def test_patient_delivery_version_bindings(package):
    instance = copy.deepcopy(package[2]["PatientDeliveryView"])
    instance["version_bindings"]["knowledge_release_id"] = None
    assert "medical delivery requires knowledge release bindings" in semantic_errors("PatientDeliveryView", instance)


def test_tool_result_time_order(package):
    instance = copy.deepcopy(package[2]["ToolResult"])
    instance["completed_at"] = "2026-08-04T02:59:59Z"
    assert semantic_errors("ToolResult", instance) == ["completed_at must not precede started_at"]


def test_tool_auth_scope_cannot_escalate(package):
    instance = copy.deepcopy(package[2]["ToolContext"])
    instance["authorization_scope"]["requested"] = ["STATE_PATCH_PROPOSE"]
    assert semantic_errors("ToolContext", instance) == ["requested scopes cannot exceed granted scopes"]


def test_controlled_values_accept_integers(package):
    patch = copy.deepcopy(package[2]["StatePatch"])
    patch["operations"][0]["value"] = 7
    assert not list(validator_for("StatePatch", package[1], package[4]).iter_errors(patch))


def test_patient_card_types_cover_ui_contract(package):
    schema = package[1]["PatientDeliveryView"]
    card_types = set(schema["$defs"]["evidenceCard"]["properties"]["card_type"]["enum"])
    assert card_types == {"SAFETY_RULE","MEDICAL_EVIDENCE","CLINICIAN_DECISION","PATIENT_FACT","SOURCE","APPLICABILITY","LIMITATION","EVIDENCE_CONFLICT"}


@pytest.mark.parametrize("path", ["/version", "/audit_info", "/execution_trace", "/patient_id"])
def test_state_patch_protected_paths(package, path):
    instance = copy.deepcopy(package[2]["StatePatch"])
    instance["operations"][0]["path"] = path
    assert list(validator_for("StatePatch", package[1], package[4]).iter_errors(instance))


@pytest.mark.parametrize("field", ["raw_result", "full_cdp", "prompt", "private_reasoning", "reasoning_paths", "confidence", "tool_input", "tool_output", "provider_response", "internal_hypothesis", "internal_policy", "execution_trace", "other_patient_data"])
def test_patient_forbidden_fields(package, field):
    instance = copy.deepcopy(package[2]["PatientDeliveryView"])
    instance[field] = "forbidden"
    assert list(validator_for("PatientDeliveryView", package[1], package[4]).iter_errors(instance))
    assert semantic_errors("PatientDeliveryView", instance)


def test_fixture_and_contract_security_scan():
    secret_patterns = [
        re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
        re.compile(r"AKIA[0-9A-Z]{16}"),
        re.compile(r"(?i)(?:api[_-]?key|client[_-]?secret|password)\s*[:=]\s*['\"]?[A-Za-z0-9+/=_-]{16,}"),
        re.compile(r"(?i)bearer\s+[A-Za-z0-9._-]{20,}"),
    ]
    for path in ROOT.rglob("*"):
        if path.is_file() and path.suffix in {".json", ".py", ".md", ".txt", ".csv"}:
            text = path.read_text(encoding="utf-8")
            assert not any(pattern.search(text) for pattern in secret_patterns), f"secret-like content in {path}"


def test_fixtures_have_no_real_patient_identifier_shapes():
    text = "\n".join(path.read_text(encoding="utf-8") for path in (ROOT / "fixtures").rglob("*.json"))
    assert not re.search(r"\b1[3-9][0-9]{9}\b", text)
    assert not re.search(r"\b[1-9][0-9]{5}(?:19|20)[0-9]{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12][0-9]|3[01])[0-9]{3}[0-9Xx]\b", text)
    assert not re.search(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", text)


def test_no_unbounded_schema_strings_or_arrays(package):
    def walk(value, location):
        if isinstance(value, dict):
            value_type = value.get("type")
            if value_type == "string" or (isinstance(value_type, list) and "string" in value_type):
                assert any(key in value for key in ("maxLength","enum","const","format","pattern")), location
            if value_type == "array":
                assert "maxItems" in value, location
            for key, nested in value.items():
                walk(nested, f"{location}/{key}")
        elif isinstance(value, list):
            for index, nested in enumerate(value):
                walk(nested, f"{location}/{index}")
    for name, schema in package[1].items():
        walk(schema, name)


def test_no_language_bindings_or_service_changes_in_contract_tree():
    forbidden_suffixes = {".java", ".ts", ".tsx"}
    assert not [path for path in ROOT.rglob("*") if path.suffix in forbidden_suffixes]
    assert not (ROOT / "generated").exists()
