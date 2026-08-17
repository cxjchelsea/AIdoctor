import copy
import json
import re
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from validator.validate_contracts import (  # noqa: E402
    MAX_SAFE_INTEGER,
    build_registry,
    decode_pointer,
    load_package,
    semantic_errors,
    storage_ref_errors,
    validate_exact_version,
    validate_new_delivery_release,
    validate_package,
    validator_for,
)


@pytest.fixture(scope="module")
def package():
    manifest, schemas, valid, invalid = load_package()
    return manifest, schemas, valid, invalid, build_registry(schemas)


def test_complete_package_validation():
    assert validate_package() == {"schemas": 13, "valid": 13, "invalid": 33}


def test_every_schema_has_required_metadata(package):
    for schema in package[1].values():
        assert schema["$schema"] == "https://json-schema.org/draft/2020-12/schema"
        assert schema["$id"].startswith("https://schemas.aidoctor.dev/contracts/1.0.0/")
        assert schema["title"] and schema["description"]
        assert schema["type"] == "object" and schema["required"]
        assert schema["additionalProperties"] is False


def test_manifest_governance_fields(package):
    assert package[0]["version_negotiation"] == "EXACT"
    assert package[0]["supported_versions"] == ["1.0.0"]
    for item in package[0]["contracts"]:
        assert set(item) == {"name","id","path","compatibility","status","stability","patient_visible","owner"}
        assert item["stability"] in {"STABLE","EXPERIMENTAL"}
        assert item["compatibility"] == "EXACT_VERSION_MINOR_REQUIRES_CONSUMER_SUPPORT"


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
        "PatientDeliveryView":{"delivery_id","delivery_version","cdp_id","review_status","generated_at","summary","safety_notice","recommended_actions","evidence_sections","limitations","version_bindings"},
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
    basis_types = set(schema["$defs"]["evidenceCard"]["properties"]["basis_type"]["enum"])
    assert basis_types == {"SAFETY_RULE","MEDICAL_EVIDENCE","CLINICIAN_DECISION","PATIENT_FACT"}
    assert {"patientSourceSummary","patientConflictSummary","patientApplicabilitySummary","patientLimitation"} <= set(schema["$defs"])


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
    # NC-CLOSE-02 授权 bindings/ 存放 REVIEWED_BINDING。schema/fixture/validator
    # 仍不得混入语言源码；generated/ 仍禁止。
    forbidden_suffixes = {".java", ".ts", ".tsx"}
    bindings_root = ROOT / "bindings"
    leaked = [
        path for path in ROOT.rglob("*")
        if path.suffix in forbidden_suffixes
        and bindings_root not in path.parents
        and path != bindings_root
    ]
    assert not leaked
    assert not (ROOT / "generated").exists()


def schema_errors(package, name, instance):
    return list(validator_for(name, package[1], package[4]).iter_errors(instance))


@pytest.mark.parametrize("name", ["StatePatch", "CommitResult", "ToolContext", "ToolResult"])
def test_envelope_contract_name_is_bound_by_schema(package, name):
    good = copy.deepcopy(package[2][name])
    assert not schema_errors(package, name, good)
    good["envelope"]["contract_name"] = "ContractEnvelope"
    assert any(error.validator == "const" for error in schema_errors(package, name, good))


def test_manifest_names_match_envelope_bindings(package):
    for name in ("StatePatch", "CommitResult", "ToolContext", "ToolResult"):
        envelope = package[1][name]["properties"]["envelope"]
        assert envelope["allOf"][1]["properties"]["contract_name"]["const"] == name


@pytest.mark.parametrize("path", [
    "/patient_state/system/version", "/patient_state/~1version",
    "/patient_state/system/audit_info", "/patient_state/../version",
    "/patient_state/.", "/patient_state/0", "/patient_state/system/createdAt",
    "/patient_state/system/cdp-version", "/patient_state/system/TRACE_INFO",
    "/patient_state/-", "/patient_state/state/versions/cdp",
])
def test_state_patch_semantic_path_bypasses_are_rejected(package, path):
    instance = copy.deepcopy(package[2]["StatePatch"])
    instance["operations"][0]["path"] = path
    assert semantic_errors("StatePatch", instance)


@pytest.mark.parametrize("path", [
    "/version", "/patient_state", "/patient_state/", "/Patient_state/version",
    "/patient-state/cdp-version", "/patient_state//version", "/patient_state/~2version",
])
def test_state_patch_structural_paths_are_rejected(package, path):
    instance = copy.deepcopy(package[2]["StatePatch"])
    instance["operations"][0]["path"] = path
    assert schema_errors(package, "StatePatch", instance)


def test_state_patch_control_and_unicode_paths_are_validator_enforced(package):
    for path in ["/patient_state/chief\u0001complaint", "/patient_state/ｖersion"]:
        instance = copy.deepcopy(package[2]["StatePatch"])
        instance["operations"][0]["path"] = path
        assert semantic_errors("StatePatch", instance)


def test_state_patch_specific_leaf_remains_valid(package):
    instance = copy.deepcopy(package[2]["StatePatch"])
    assert not schema_errors(package, "StatePatch", instance)
    assert not semantic_errors("StatePatch", instance)
    assert decode_pointer(instance["operations"][0]["path"]) == ["patient_state", "chief_complaint"]


@pytest.mark.parametrize("field,message", [
    ("claim_id", "claim_id values must be unique"),
    ("source_id", "source_id values must be unique"),
])
def test_evidence_ids_are_unique(package, field, message):
    instance = copy.deepcopy(package[2]["EvidencePack"])
    collection = "claims" if field == "claim_id" else "sources"
    instance[collection].append(copy.deepcopy(instance[collection][0]))
    assert message in semantic_errors("EvidencePack", instance)


def test_evidence_conflict_ids_and_references_are_validated(package):
    instance = copy.deepcopy(package[2]["EvidencePack"])
    instance["conflicts"] = [
        {"conflict_id": "conflict-1", "claim_refs": ["claim-1", "missing-claim"], "source_refs": ["missing-source"], "summary": "Fictional conflict."},
        {"conflict_id": "conflict-1", "claim_refs": ["claim-1", "claim-2"], "source_refs": ["source-1"], "summary": "Duplicate conflict."},
    ]
    errors = semantic_errors("EvidencePack", instance)
    assert "conflict_id values must be unique" in errors
    assert "conflict claim reference must resolve inside claims" in errors
    assert "conflict source reference must resolve inside sources" in errors


def test_evidence_claim_source_type_matrix(package):
    instance = copy.deepcopy(package[2]["EvidencePack"])
    instance["sources"][0]["source_type"] = "PATIENT_RECORD"
    assert "MEDICAL_EVIDENCE citation source type is not allowed" in semantic_errors("EvidencePack", instance)


def test_evidence_validity_order(package):
    instance = copy.deepcopy(package[2]["EvidencePack"])
    instance["sources"][0].update(valid_from="2026-08-05T00:00:00Z", valid_to="2026-08-01T00:00:00Z")
    assert "valid_to must not precede valid_from" in semantic_errors("EvidencePack", instance)


def test_evidence_duplicate_citation_rejected_by_schema(package):
    instance = copy.deepcopy(package[2]["EvidencePack"])
    instance["claims"][0]["citation_refs"] = ["source-1", "source-1"]
    assert any(error.validator == "uniqueItems" for error in schema_errors(package, "EvidencePack", instance))


@pytest.mark.parametrize("status", ["READY_WITH_LIMITED_EVIDENCE", "DEGRADED"])
def test_patient_limited_and_degraded_deliveries(package, status):
    instance = copy.deepcopy(package[2]["PatientDeliveryView"])
    instance["delivery_status"] = status
    assert not schema_errors(package, "PatientDeliveryView", instance)


def test_patient_ui_structures_are_present(package):
    card = package[2]["PatientDeliveryView"]["evidence_sections"][0]["cards"][0]
    assert card["sources"] and card["conflict_summary"] and card["applicability"] and card["limitations"]
    assert card["clinician_reviewed"] is True and card["display_priority"] == "HIGH"


def test_patient_ui_optional_fields_follow_reviewed_contract(package):
    instance = copy.deepcopy(package[2]["PatientDeliveryView"])
    section = instance["evidence_sections"][0]
    card = section["cards"][0]
    source = card["sources"][0]
    section.pop("description")
    for field in ("rationale_summary", "certainty_label", "conflict_summary", "reviewed_at"):
        card.pop(field)
    card["applicability"].pop("patient_friendly_message")
    section["cards"][1].pop("applicability")
    for field in ("publication_date", "source_version", "applicable_population", "region", "patient_friendly_excerpt", "access_url"):
        source.pop(field)
    instance["limitations"][0]["user_action"] = "Use the authorized follow-up channel."
    instance.pop("follow_up")
    assert not schema_errors(package, "PatientDeliveryView", instance)
    assert not semantic_errors("PatientDeliveryView", instance)


def test_patient_conflict_references_resolve(package):
    instance = copy.deepcopy(package[2]["PatientDeliveryView"])
    instance["evidence_sections"][0]["cards"][0]["conflict_summary"]["affected_claim_ids"][1] = "missing"
    assert "patient conflict claim reference must resolve inside cards" in semantic_errors("PatientDeliveryView", instance)


@pytest.mark.parametrize(("field", "value"), [
    ("summary", "SYSTEM PROMPT: fictional policy-later example"),
    ("recommended_actions", ["Take a fictional 500 mg dose"]),
    ("follow_up", ["Fictional private reasoning text"]),
])
def test_patient_free_text_is_structurally_valid_but_policy_enforced_later(package, field, value):
    instance = copy.deepcopy(package[2]["PatientDeliveryView"])
    instance[field] = value
    assert not schema_errors(package, "PatientDeliveryView", instance)
    assert not semantic_errors("PatientDeliveryView", instance)


@pytest.mark.parametrize("storage_ref", [
    "artifact://uploads/../secret", "artifact://uploads/%2e%2e/secret",
    "artifact://uploads/%252e%252e/secret", "artifact://uploads//secret",
    "artifact://uploads\\..\\secret", "file:///etc/passwd", "C:/patient/report.pdf",
    "\\\\server\\share\\report.pdf",
])
def test_source_artifact_traversal_rejected(package, storage_ref):
    instance = copy.deepcopy(package[2]["SourceArtifact"])
    instance["storage_ref"] = storage_ref
    assert schema_errors(package, "SourceArtifact", instance) or storage_ref_errors(instance)


def test_source_artifact_lineage_rejects_self_and_duplicates(package):
    instance = copy.deepcopy(package[2]["SourceArtifact"])
    instance["derived_artifacts"] = [instance["artifact_id"]]
    assert "derived_artifacts cannot directly reference artifact_id" in semantic_errors("SourceArtifact", instance)
    instance["derived_artifacts"] = []
    instance["artifact_type"] = "OCR_TEXT"
    instance["source_artifact_id"] = instance["artifact_id"]
    assert "source_artifact_id cannot directly reference artifact_id" in semantic_errors("SourceArtifact", instance)
    instance["derived_artifacts"] = ["derived-1", "derived-1"]
    assert any(error.validator == "uniqueItems" for error in schema_errors(package, "SourceArtifact", instance))


def test_source_artifact_safe_logical_uri(package):
    instance = copy.deepcopy(package[2]["SourceArtifact"])
    assert not schema_errors(package, "SourceArtifact", instance)
    assert not semantic_errors("SourceArtifact", instance)


def commit_result_for(package, status):
    instance = copy.deepcopy(package[2]["CommitResult"])
    if status != "COMMITTED":
        instance["status"] = status
        instance.pop("committed_version", None)
        instance.pop("committed_at", None)
        instance["reason_code"] = f"PATCH_{status}"
    if status == "REJECTED":
        instance["rejected_operations"] = [{"operation_index": 0, "reason_code": "VALIDATION_FAILED"}]
    elif status == "CONFLICT":
        instance["conflicts"] = [copy.deepcopy(package[2]["ContractConflict"])]
        instance["retryable"] = True
    elif status == "FAILED":
        instance["errors"] = [{"code": "DEPENDENCY_FAILURE", "message": "Fictional failure."}]
        instance["retryable"] = True
    return instance


@pytest.mark.parametrize("status", ["COMMITTED", "REJECTED", "CONFLICT", "NO_OP", "FAILED"])
def test_commit_result_complete_valid_matrix(package, status):
    instance = commit_result_for(package, status)
    assert not schema_errors(package, "CommitResult", instance)
    assert not semantic_errors("CommitResult", instance)


@pytest.mark.parametrize("status", ["REJECTED", "CONFLICT", "NO_OP", "FAILED"])
def test_non_committed_matrix_rejects_committed_fields(package, status):
    instance = commit_result_for(package, status)
    instance["committed_version"] = 2
    assert schema_errors(package, "CommitResult", instance)


def test_commit_status_specific_failures(package):
    rejected = commit_result_for(package, "REJECTED")
    rejected["rejected_operations"] = []
    assert schema_errors(package, "CommitResult", rejected)
    no_op = commit_result_for(package, "NO_OP")
    no_op["retryable"] = True
    assert schema_errors(package, "CommitResult", no_op)
    failed = commit_result_for(package, "FAILED")
    failed["errors"] = []
    assert schema_errors(package, "CommitResult", failed)


def tool_result_for(package, status):
    instance = copy.deepcopy(package[2]["ToolResult"])
    instance["status"] = status
    instance["reason_code"] = status
    instance["errors"] = []
    instance["retryable"] = False
    if status != "SUCCEEDED":
        instance["output"] = []
        instance["suggested_patches"] = []
    if status in {"RETRYABLE_FAILURE", "NON_RETRYABLE_FAILURE", "TIMED_OUT", "POLICY_BLOCKED"}:
        category = "POLICY" if status == "POLICY_BLOCKED" else "TIMEOUT" if status == "TIMED_OUT" else "DEPENDENCY"
        instance["errors"] = [{"code": "SYNTHETIC_ERROR", "category": category, "message": "Fictional error.", "retryable": status in {"RETRYABLE_FAILURE", "TIMED_OUT"}}]
    if status in {"RETRYABLE_FAILURE", "TIMED_OUT"}:
        instance["retryable"] = True
    return instance


@pytest.mark.parametrize("status", ["SUCCEEDED", "NO_RESULT", "RETRYABLE_FAILURE", "NON_RETRYABLE_FAILURE", "TIMED_OUT", "POLICY_BLOCKED"])
def test_tool_result_complete_valid_matrix(package, status):
    assert not schema_errors(package, "ToolResult", tool_result_for(package, status))


def test_tool_result_status_specific_failures(package):
    succeeded = tool_result_for(package, "SUCCEEDED")
    succeeded["errors"] = [{"code": "SYNTHETIC_ERROR", "category": "INTERNAL", "message": "Fictional.", "retryable": False}]
    assert schema_errors(package, "ToolResult", succeeded)
    failed = tool_result_for(package, "RETRYABLE_FAILURE")
    failed["errors"] = []
    assert schema_errors(package, "ToolResult", failed)
    blocked = tool_result_for(package, "POLICY_BLOCKED")
    blocked["errors"][0]["category"] = "DEPENDENCY"
    assert schema_errors(package, "ToolResult", blocked)


@pytest.mark.parametrize("status", ["DRAFT", "ACTIVE", "WITHDRAWN", "SUPERSEDED"])
def test_knowledge_release_valid_matrix(package, status):
    instance = copy.deepcopy(package[2]["KnowledgeReleaseRef"])
    instance["status"] = status
    if status == "WITHDRAWN":
        instance["withdrawn_at"] = "2026-08-05T00:00:00Z"
    if status == "SUPERSEDED":
        instance["superseded_by"] = "knowledge-2026-09"
    assert not schema_errors(package, "KnowledgeReleaseRef", instance)
    assert not semantic_errors("KnowledgeReleaseRef", instance)


def test_knowledge_release_invalid_matrix(package):
    active = copy.deepcopy(package[2]["KnowledgeReleaseRef"])
    active["withdrawn_at"] = "2026-08-05T00:00:00Z"
    assert schema_errors(package, "KnowledgeReleaseRef", active)
    superseded = copy.deepcopy(package[2]["KnowledgeReleaseRef"])
    superseded.update(status="SUPERSEDED", superseded_by=superseded["knowledge_release_id"])
    assert "superseded_by cannot reference knowledge_release_id" in semantic_errors("KnowledgeReleaseRef", superseded)


def test_tool_context_deadline_and_scope(package):
    valid = copy.deepcopy(package[2]["ToolContext"])
    valid["authorization_scope"] = {"granted": [], "requested": []}
    assert not semantic_errors("ToolContext", valid)
    valid["deadline"] = "2026-08-03T00:00:00Z"
    assert "deadline must not precede envelope.created_at" in semantic_errors("ToolContext", valid)


def test_all_integer_instances_use_javascript_safe_bounds(package):
    def walk(value, location):
        if isinstance(value, dict):
            value_type = value.get("type")
            if value_type == "integer" or isinstance(value_type, list) and "integer" in value_type:
                assert "maximum" in value and value["maximum"] <= MAX_SAFE_INTEGER, location
            for key, nested in value.items():
                walk(nested, f"{location}/{key}")
        elif isinstance(value, list):
            for index, nested in enumerate(value):
                walk(nested, f"{location}/{index}")
    for name, schema in package[1].items():
        walk(schema, name)


def test_safe_integer_boundary(package):
    instance = copy.deepcopy(package[2]["StatePatch"])
    instance["base_version"] = MAX_SAFE_INTEGER
    assert not schema_errors(package, "StatePatch", instance)
    instance["base_version"] = MAX_SAFE_INTEGER + 1
    assert schema_errors(package, "StatePatch", instance)
    instance["base_version"] = 1.5
    assert schema_errors(package, "StatePatch", instance)


def test_exact_version_negotiation(package):
    instance = copy.deepcopy(package[2]["StatePatch"])
    assert not validate_exact_version(package[0], "StatePatch", instance)
    instance["contract_version"] = "1.1.0"
    assert validate_exact_version(package[0], "StatePatch", instance)
    assert schema_errors(package, "StatePatch", instance)
    assert validate_exact_version(package[0], "MissingContract", package[2]["StatePatch"])
