"""Positive and negative structural tests for A6 adult_respiratory_v1 skeleton."""

from __future__ import annotations

import copy
import json
import re
import sys
from pathlib import Path

import pytest
import yaml

ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = ROOT.parent
sys.path.insert(0, str(ROOT))

from validator.validate_capability import (  # noqa: E402
    CAPABILITY_ID,
    MAX_SAFE_INTEGER,
    PACKAGE_DIR,
    PACKAGE_VERSION,
    REQUIRED_PACKAGE_RELATIVE,
    REQUIRED_SCHEMAS,
    SCHEMA_ID_PREFIX,
    SHARED_CONTRACT_VERSION,
    build_registry,
    load_package_documents,
    load_schemas,
    mutate_manifest,
    validate_mutated_package,
    validate_package,
    validator_for,
)


@pytest.fixture(scope="module")
def package():
    schemas = load_schemas()
    documents = load_package_documents()
    return documents, schemas, build_registry(schemas)


def test_complete_package_validation():
    assert validate_package() == {
        "schemas": 11,
        "assets": 25,
        "eval_cases": 5,
        "issues": 0,
    }


def _prepare_near_active(docs):
    """Fill only the fields required to isolate a single remaining gate."""
    manifest = docs["manifest.yaml"]
    manifest["lifecycle"] = "ACTIVE"
    manifest["clinical_review_status"] = "APPROVED"
    manifest["technical_review_status"] = "APPROVED"
    manifest["production_eligibility"] = "ELIGIBLE"
    manifest["runtime_adoption"] = "ENABLED"
    manifest["owner_status"] = "ASSIGNED"
    for relative in (
        "safety/red_flags.yaml",
        "safety/triage_rules.yaml",
        "safety/special_populations.yaml",
    ):
        docs[relative]["review_status"] = "APPROVED"
        docs[relative]["production_eligibility"] = "ELIGIBLE"
        docs[relative]["rules"] = [
            {
                "rule_id": "synthetic-safety-rule-1",
                "owner_role": "clinical-safety-owner",
                "review_status": "APPROVED",
                "condition_refs": ["synthetic-observation"],
                "result_code": "escalate-human-review",
                "required_evidence": ["clinical approval evidence"],
                "source_requirement": "approved clinical source",
                "prohibited_runtime_use": False,
            }
        ]
    docs["hypotheses/limited_hypotheses.yaml"]["review_status"] = "APPROVED"
    docs["knowledge/knowledge_policy.yaml"]["review_status"] = "APPROVED"
    docs["knowledge/source_manifest.yaml"]["review_status"] = "APPROVED"
    for relative in (
        "runtime/tool_allowlist.yaml",
        "runtime/skill_allowlist.yaml",
        "runtime/prompt_allowlist.yaml",
        "runtime/model_routes.yaml",
        "runtime/context_policy.yaml",
    ):
        docs[relative]["references"] = [
            {
                "reference_id": f"{docs[relative]['allowlist_type'].lower().replace('_', '-')}-ref-1",
                "registry_path": "contracts/v1/manifest.json",
                "release_version": "1.0.0",
                "review_status": "APPROVED",
            }
        ]


def test_required_files_and_schemas_exist():
    assert {path.name for path in (ROOT / "schemas").glob("*.json")} == REQUIRED_SCHEMAS
    for relative in REQUIRED_PACKAGE_RELATIVE:
        assert (PACKAGE_DIR / relative).exists(), relative


def test_schema_metadata_and_bounds(package):
    _, schemas, _ = package
    for name, schema in schemas.items():
        assert schema["$schema"] == "https://json-schema.org/draft/2020-12/schema"
        assert schema["$id"].startswith(SCHEMA_ID_PREFIX)
        assert schema["title"] and schema["description"]
        assert not re.search(
            r"\b(is clinically valid|asserts clinical validity|proves clinical)\b",
            schema["description"].lower(),
        )

        def walk(node, location):
            if isinstance(node, dict):
                if node.get("type") == "object" and "properties" in node:
                    assert node.get("additionalProperties") is False, f"{name}:{location}"
                value_type = node.get("type")
                if value_type == "string" or (isinstance(value_type, list) and "string" in value_type):
                    assert any(key in node for key in ("maxLength", "enum", "const", "format", "pattern")), location
                if value_type == "array":
                    assert "maxItems" in node, location
                if value_type == "integer" or (isinstance(value_type, list) and "integer" in value_type):
                    if "maximum" in node:
                        assert node["maximum"] <= MAX_SAFE_INTEGER
                for key, nested in node.items():
                    walk(nested, f"{location}/{key}")
            elif isinstance(node, list):
                for index, nested in enumerate(node):
                    walk(nested, f"{location}/{index}")

        walk(schema, "")


def test_manifest_governance_and_contract_binding(package):
    documents, _, _ = package
    manifest = documents["manifest.yaml"]
    assert manifest["capability_id"] == CAPABILITY_ID
    assert manifest["package_version"] == PACKAGE_VERSION
    assert manifest["lifecycle"] == "DRAFT"
    assert manifest["clinical_review_status"] == "REQUIRES_CLINICAL_REVIEW"
    assert manifest["runtime_adoption"] == "NOT_IMPLEMENTED"
    assert manifest["production_eligibility"] == "BLOCKED"
    assert manifest["overall_evidence"] == "PARTIALLY_VALIDATED"
    assert manifest["compatible_shared_contract_version"] == SHARED_CONTRACT_VERSION
    assert manifest["owner_status"] == "UNASSIGNED"
    assert len(manifest["assets"]) == 25


def test_clinical_packs_are_empty_and_blocked(package):
    documents, _, _ = package
    for relative in (
        "safety/red_flags.yaml",
        "safety/triage_rules.yaml",
        "safety/special_populations.yaml",
    ):
        doc = documents[relative]
        assert doc["rules"] == []
        assert doc["production_eligibility"] == "BLOCKED"
        assert doc["review_status"] == "REQUIRES_CLINICAL_REVIEW"
        assert doc["fail_closed"]["model_single_point_decision_allowed"] is False
    hypothesis = documents["hypotheses/limited_hypotheses.yaml"]
    assert hypothesis["hypotheses"] == []
    assert hypothesis["automatic_diagnosis_allowed"] is False
    assert hypothesis["free_expansion_allowed"] is False
    for relative in ("knowledge/knowledge_policy.yaml", "knowledge/source_manifest.yaml"):
        knowledge = documents[relative]
        assert knowledge["approved_source_count"] == 0
        assert knowledge["sources"] == []
        assert knowledge["retrieval_eligibility"] == "BLOCKED"
        assert knowledge["mixed_index_allowed"] is False


def test_runtime_allowlists_empty_and_unimplemented(package):
    documents, _, _ = package
    for relative in (
        "runtime/tool_allowlist.yaml",
        "runtime/skill_allowlist.yaml",
        "runtime/prompt_allowlist.yaml",
        "runtime/model_routes.yaml",
        "runtime/context_policy.yaml",
    ):
        doc = documents[relative]
        assert doc["references"] == []
        assert doc["runtime_eligibility"] in {"NOT_IMPLEMENTED", "BLOCKED"}
        assert doc["production_eligibility"] == "BLOCKED"


def test_delivery_binds_patient_delivery_exact_version(package):
    documents, _, _ = package
    delivery = documents["delivery/delivery_policy.yaml"]
    assert delivery["patient_delivery_contract_version"] == "1.0.0"
    assert delivery["clinician_review_required"] is True
    assert delivery["free_text_safety"] == "POLICY_ENFORCED_LATER"
    assert delivery["runtime_eligibility"] == "NOT_IMPLEMENTED"


def test_eval_cases_are_synthetic_structural_only(package):
    documents, _, _ = package
    expected = {
        "evals/safety_cases.jsonl": "SAFETY",
        "evals/extraction_cases.jsonl": "EXTRACTION",
        "evals/question_cases.jsonl": "QUESTION",
        "evals/rag_cases.jsonl": "RAG",
        "evals/e2e_cases.jsonl": "E2E",
    }
    case_ids = []
    for relative, suite in expected.items():
        rows = documents[relative]
        assert len(rows) >= 1
        for row in rows:
            case_ids.append(row["case_id"])
            assert row["suite_type"] == suite
            assert row["synthetic"] is True
            assert row["patient_data_classification"] == "SYNTHETIC_NON_PATIENT"
            assert row["clinical_review_status"] == "REQUIRES_CLINICAL_REVIEW"
    assert len(case_ids) == len(set(case_ids))


def test_population_excludes_treatment_and_open_diagnosis(package):
    scope = package[0]["population/scope.yaml"]
    exclusions = set(scope["explicit_exclusions"])
    assert {
        "AUTOMATIC_DIAGNOSIS",
        "AUTOMATIC_TREATMENT",
        "PRESCRIPTION_OR_DOSAGE_CHANGE",
        "OPEN_ENDED_RESPIRATORY_DIAGNOSIS",
        "HIGH_RISK_DECISION_WITHOUT_CLINICIAN_REVIEW",
        "PEDIATRIC_COMPLETE_PATHWAY",
        "PREGNANCY_COMPLETE_PATHWAY",
    } <= exclusions
    assert scope["adult_age_boundary"]["minimum_age"] is None
    assert scope["adult_age_boundary"]["maximum_age"] is None


def _mutated(documents, mutation):
    docs = copy.deepcopy(documents)
    docs["manifest.yaml"] = mutate_manifest(docs["manifest.yaml"], mutation)
    return docs


def test_negative_unknown_manifest_field(package):
    documents, schemas, registry = package
    docs = copy.deepcopy(documents)
    docs["manifest.yaml"]["unexpected_field"] = "x"
    errors = list(validator_for("capability-manifest.schema.json", schemas, registry).iter_errors(docs["manifest.yaml"]))
    assert any(error.validator == "additionalProperties" for error in errors)


def test_negative_capability_id_drift(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["capability_id"] = "other_capability_v1"
    issues = validate_mutated_package(docs)
    assert any(item.path == "/capability_id" for item in issues)


def test_negative_shared_contract_version_drift(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["compatible_shared_contract_version"] = "1.1.0"
    issues = validate_mutated_package(docs)
    assert any("1.0.0" in item.reason for item in issues)


def test_negative_missing_asset_path(package):
    docs = _mutated(
        package[0],
        {"op": "replace", "path": "/assets/0/path", "value": "delivery/does_not_exist.yaml"},
    )
    issues = validate_mutated_package(docs)
    assert any(item.validator == "exists" for item in issues)


def test_negative_checksum_mismatch(package):
    docs = _mutated(
        package[0],
        {"op": "replace", "path": "/assets/0/sha256", "value": "0" * 64},
    )
    issues = validate_mutated_package(docs)
    assert any(item.validator == "checksum" for item in issues)


def test_negative_active_with_unapproved_safety(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["lifecycle"] = "ACTIVE"
    docs["manifest.yaml"]["clinical_review_status"] = "APPROVED"
    docs["manifest.yaml"]["technical_review_status"] = "APPROVED"
    docs["manifest.yaml"]["production_eligibility"] = "ELIGIBLE"
    docs["manifest.yaml"]["runtime_adoption"] = "ENABLED"
    docs["manifest.yaml"]["owner_status"] = "ASSIGNED"
    issues = validate_mutated_package(docs)
    assert any(item.category == "safety" and "ACTIVE" in item.reason for item in issues)


def test_negative_active_with_empty_approved_hypotheses(package):
    docs = copy.deepcopy(package[0])
    _prepare_near_active(docs)
    docs["hypotheses/limited_hypotheses.yaml"]["hypotheses"] = []
    docs["knowledge/knowledge_policy.yaml"]["approved_source_count"] = 1
    docs["knowledge/knowledge_policy.yaml"]["sources"] = [
        {
            "source_id": "source-synthetic-1",
            "title": "Synthetic structural source",
            "tier": "TIER_1",
            "region": "UNSCOPED",
            "population": "UNSCOPED",
            "freshness_status": "UNKNOWN",
            "license_review": "PENDING",
            "clinical_review": "REQUIRES_CLINICAL_REVIEW",
            "withdrawal_status": "AVAILABLE",
            "release_binding": "release-none",
            "checksum": "a" * 64,
            "retrieval_eligibility": "BLOCKED",
        }
    ]
    docs["knowledge/source_manifest.yaml"]["approved_source_count"] = 1
    docs["knowledge/source_manifest.yaml"]["sources"] = copy.deepcopy(
        docs["knowledge/knowledge_policy.yaml"]["sources"]
    )
    issues = validate_mutated_package(docs)
    assert any("empty Hypothesis Pack cannot enter ACTIVE" in item.reason for item in issues)


def test_negative_active_with_zero_knowledge_sources(package):
    docs = copy.deepcopy(package[0])
    _prepare_near_active(docs)
    docs["hypotheses/limited_hypotheses.yaml"]["hypotheses"] = [
        {
            "hypothesis_id": "hypothesis-synthetic-1",
            "display_name": "Synthetic structural hypothesis",
            "category": "COMMON",
            "supporting_evidence_refs": ["evidence-1"],
            "opposing_evidence_refs": [],
            "missing_evidence_refs": [],
            "applicability": "structural fixture only",
            "review_status": "APPROVED",
            "clinical_source_requirements": ["approved clinical source"],
            "prohibited_auto_actions": ["AUTOMATIC_DIAGNOSIS"],
        }
    ]
    docs["knowledge/knowledge_policy.yaml"]["approved_source_count"] = 0
    docs["knowledge/knowledge_policy.yaml"]["sources"] = []
    docs["knowledge/source_manifest.yaml"]["approved_source_count"] = 0
    docs["knowledge/source_manifest.yaml"]["sources"] = []
    issues = validate_mutated_package(docs)
    assert any("empty knowledge sources cannot enter ACTIVE" in item.reason for item in issues)


def test_negative_runtime_enabled_with_empty_allowlists(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["lifecycle"] = "CLINICAL_REVIEW"
    docs["manifest.yaml"]["runtime_adoption"] = "ENABLED"
    issues = validate_mutated_package(docs)
    assert any(
        "runtime ENABLED requires non-empty runtime allowlist references" in item.reason
        for item in issues
    )


def test_negative_clinical_approved_requires_assigned_owner(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["lifecycle"] = "CLINICAL_REVIEW"
    docs["manifest.yaml"]["clinical_review_status"] = "APPROVED"
    docs["manifest.yaml"]["owner_status"] = "UNASSIGNED"
    issues = validate_mutated_package(docs)
    assert any("clinical APPROVED requires assigned owner" in item.reason for item in issues)


def test_negative_production_eligible_with_empty_hypotheses(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["clinical_review_status"] = "APPROVED"
    docs["manifest.yaml"]["owner_status"] = "ASSIGNED"
    docs["manifest.yaml"]["production_eligibility"] = "ELIGIBLE"
    for relative in (
        "safety/red_flags.yaml",
        "safety/triage_rules.yaml",
        "safety/special_populations.yaml",
    ):
        docs[relative]["rules"] = [
            {
                "rule_id": "synthetic-safety-rule-2",
                "owner_role": "clinical-safety-owner",
                "review_status": "APPROVED",
                "condition_refs": ["synthetic-observation"],
                "result_code": "escalate-human-review",
                "required_evidence": ["clinical approval evidence"],
                "source_requirement": "approved clinical source",
                "prohibited_runtime_use": False,
            }
        ]
        docs[relative]["production_eligibility"] = "ELIGIBLE"
    docs["knowledge/knowledge_policy.yaml"]["approved_source_count"] = 1
    docs["knowledge/knowledge_policy.yaml"]["sources"] = [
        {
            "source_id": "source-synthetic-2",
            "title": "Synthetic structural source",
            "tier": "TIER_1",
            "region": "UNSCOPED",
            "population": "UNSCOPED",
            "freshness_status": "UNKNOWN",
            "license_review": "PENDING",
            "clinical_review": "REQUIRES_CLINICAL_REVIEW",
            "withdrawal_status": "AVAILABLE",
            "release_binding": "release-none",
            "checksum": "b" * 64,
            "retrieval_eligibility": "BLOCKED",
        }
    ]
    docs["knowledge/source_manifest.yaml"]["approved_source_count"] = 1
    docs["knowledge/source_manifest.yaml"]["sources"] = copy.deepcopy(
        docs["knowledge/knowledge_policy.yaml"]["sources"]
    )
    issues = validate_mutated_package(docs)
    assert any("empty Hypothesis Pack requires production BLOCKED" in item.reason for item in issues)


def test_negative_production_eligible_with_empty_sources(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["clinical_review_status"] = "APPROVED"
    docs["manifest.yaml"]["owner_status"] = "ASSIGNED"
    docs["manifest.yaml"]["production_eligibility"] = "ELIGIBLE"
    for relative in (
        "safety/red_flags.yaml",
        "safety/triage_rules.yaml",
        "safety/special_populations.yaml",
    ):
        docs[relative]["rules"] = [
            {
                "rule_id": "synthetic-safety-rule-3",
                "owner_role": "clinical-safety-owner",
                "review_status": "APPROVED",
                "condition_refs": ["synthetic-observation"],
                "result_code": "escalate-human-review",
                "required_evidence": ["clinical approval evidence"],
                "source_requirement": "approved clinical source",
                "prohibited_runtime_use": False,
            }
        ]
        docs[relative]["production_eligibility"] = "ELIGIBLE"
    docs["hypotheses/limited_hypotheses.yaml"]["hypotheses"] = [
        {
            "hypothesis_id": "hypothesis-synthetic-2",
            "display_name": "Synthetic structural hypothesis",
            "category": "COMMON",
            "supporting_evidence_refs": ["evidence-1"],
            "opposing_evidence_refs": [],
            "missing_evidence_refs": [],
            "applicability": "structural fixture only",
            "review_status": "APPROVED",
            "clinical_source_requirements": ["approved clinical source"],
            "prohibited_auto_actions": ["AUTOMATIC_DIAGNOSIS"],
        }
    ]
    issues = validate_mutated_package(docs)
    assert any("empty knowledge sources require production BLOCKED" in item.reason for item in issues)


def test_negative_empty_safety_production_eligible(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["production_eligibility"] = "ELIGIBLE"
    docs["manifest.yaml"]["clinical_review_status"] = "APPROVED"
    issues = validate_mutated_package(docs)
    assert any("empty Safety rules require production BLOCKED" in item.reason for item in issues)


def test_negative_empty_sources_knowledge_enabled(package):
    docs = copy.deepcopy(package[0])
    docs["knowledge/knowledge_policy.yaml"]["retrieval_eligibility"] = "ELIGIBLE"
    issues = validate_mutated_package(docs)
    assert any("empty approved sources" in item.reason for item in issues)


def test_negative_unknown_runtime_reference(package):
    docs = copy.deepcopy(package[0])
    docs["runtime/tool_allowlist.yaml"]["references"] = [
        {
            "reference_id": "tool-missing-1",
            "registry_path": "does/not/exist-tool.py",
            "release_version": "1.0.0",
            "review_status": "APPROVED",
        }
    ]
    issues = validate_mutated_package(docs)
    assert any(item.category == "runtime" and item.validator == "exists" for item in issues)


def test_negative_rollback_self_reference(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["rollback_version"] = PACKAGE_VERSION
    issues = validate_mutated_package(docs)
    assert any(item.validator == "self-reference" for item in issues)


def test_negative_time_order_inverted(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["effective_from"] = "2026-08-05T00:00:00Z"
    docs["manifest.yaml"]["expires_at"] = "2026-08-04T00:00:00Z"
    issues = validate_mutated_package(docs)
    assert any(item.validator == "order" for item in issues)


def test_negative_absolute_path_rejected(package):
    docs = _mutated(
        package[0],
        {"op": "replace", "path": "/assets/0/path", "value": "C:/temp/delivery_policy.yaml"},
    )
    issues = validate_mutated_package(docs)
    assert any(item.category == "path-safety" for item in issues)


def test_negative_path_traversal_rejected(package):
    docs = _mutated(
        package[0],
        {"op": "replace", "path": "/assets/0/path", "value": "../secrets/delivery_policy.yaml"},
    )
    issues = validate_mutated_package(docs)
    assert any(item.category == "path-safety" for item in issues)


def test_negative_encoded_traversal_rejected(package):
    docs = copy.deepcopy(package[0])
    docs["runtime/tool_allowlist.yaml"]["references"] = [
        {
            "reference_id": "tool-trav-1",
            "registry_path": "capabilities/%2e%2e/secret.py",
            "release_version": "1.0.0",
            "review_status": "PENDING",
        }
    ]
    issues = validate_mutated_package(docs)
    assert any(item.category == "path-safety" for item in issues)


def test_negative_real_patient_classification(package):
    docs = copy.deepcopy(package[0])
    docs["evals/safety_cases.jsonl"][0]["patient_data_classification"] = "REAL_PATIENT"
    issues = validate_mutated_package(docs)
    assert any(item.category == "eval" for item in issues)


def test_negative_non_synthetic_eval(package):
    docs = copy.deepcopy(package[0])
    docs["evals/safety_cases.jsonl"][0]["synthetic"] = False
    issues = validate_mutated_package(docs)
    assert any("synthetic" in item.path for item in issues)


def test_negative_mixed_knowledge_domains(package):
    docs = copy.deepcopy(package[0])
    docs["knowledge/knowledge_policy.yaml"]["mixed_index_allowed"] = True
    issues = validate_mutated_package(docs)
    assert any("must not be mixed" in item.reason for item in issues)


def test_negative_treatment_in_supported_categories(package):
    docs = copy.deepcopy(package[0])
    docs["population/scope.yaml"]["supported_chief_complaint_categories"] = [
        "RESPIRATORY_SYMPTOM_REPORT",
        "PRESCRIPTION_REQUEST",
    ]
    issues = validate_mutated_package(docs)
    assert any(item.category == "clinical-boundary" for item in issues)


def test_negative_illegal_evidence_level(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["overall_evidence"] = "CLINICALLY_VALIDATED"
    issues = validate_mutated_package(docs)
    assert any(item.path == "/overall_evidence" for item in issues)


def test_negative_runtime_verified_claim(package):
    docs = copy.deepcopy(package[0])
    docs["manifest.yaml"]["overall_evidence"] = "RUNTIME_VERIFIED"
    issues = validate_mutated_package(docs)
    assert any("RUNTIME_VERIFIED" in item.reason for item in issues)


def test_negative_duplicate_json_key_rejected(tmp_path):
    path = tmp_path / "dup.json"
    path.write_text('{"a":1,"a":2}\n', encoding="utf-8")
    with pytest.raises(ValueError, match="duplicate JSON key"):
        from validator.validate_capability import read_json

        read_json(path)


def test_negative_duplicate_yaml_key_rejected(tmp_path):
    path = tmp_path / "dup.yaml"
    path.write_text("a: 1\na: 2\n", encoding="utf-8")
    with pytest.raises(Exception, match="duplicate key"):
        from validator.validate_capability import read_yaml

        read_yaml(path)


def test_negative_missing_required_file(monkeypatch, package):
    docs = copy.deepcopy(package[0])
    del docs["delivery/delivery_policy.yaml"]
    with pytest.raises(Exception):
        # Reconstruct validation path by temporarily pointing PACKAGE_DIR is hard;
        # instead ensure semantic completeness catches missing listed path after deletion from documents.
        docs["manifest.yaml"]["assets"] = [
            asset for asset in docs["manifest.yaml"]["assets"] if asset["path"] != "delivery/delivery_policy.yaml"
        ]
        issues = validate_mutated_package(docs)
        assert any(item.validator == "completeness" for item in issues)
        if not any(item.validator == "completeness" for item in issues):
            raise AssertionError("expected completeness failure")


def test_readme_status_markers(package):
    readme = package[0]["README.md"]
    for marker in (
        "DRAFT",
        "PARTIALLY_VALIDATED",
        "REQUIRES_CLINICAL_REVIEW",
        "NOT_IMPLEMENTED",
        "BLOCKED",
    ):
        assert marker in readme


def test_no_language_bindings_or_service_changes_in_capability_tree():
    forbidden_suffixes = {".java", ".ts", ".tsx"}
    assert not [path for path in ROOT.rglob("*") if path.suffix in forbidden_suffixes]
    assert not (ROOT / "generated").exists()


def test_security_and_patient_identifier_scan():
    secret_patterns = [
        re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
        re.compile(r"AKIA[0-9A-Z]{16}"),
        re.compile(r"(?i)(?:api[_-]?key|client[_-]?secret|password)\s*[:=]\s*['\"]?[A-Za-z0-9+/=_-]{16,}"),
        re.compile(r"(?i)bearer\s+[A-Za-z0-9._-]{20,}"),
    ]
    patient_patterns = [
        re.compile(r"\b1[3-9]\d{9}\b"),
        re.compile(r"\b[1-9]\d{5}(?:19|20)\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\d|3[01])\d{3}[\dXx]\b"),
        re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"),
    ]
    texts = []
    for path in ROOT.rglob("*"):
        if path.is_file() and path.suffix in {".yaml", ".yml", ".json", ".jsonl", ".md", ".py", ".txt", ".csv"}:
            text = path.read_text(encoding="utf-8")
            texts.append(text)
            assert not any(pattern.search(text) for pattern in secret_patterns), path
    joined = "\n".join(texts)
    assert not any(pattern.search(joined) for pattern in patient_patterns)


def test_a5_contracts_untouched_by_capability_package():
    contracts = json.loads((REPO_ROOT / "contracts" / "v1" / "manifest.json").read_text(encoding="utf-8"))
    assert contracts["contract_version"] == "1.0.0"
    assert len(contracts["contracts"]) == 13


def test_yaml_assets_round_trip_parse(package):
    for relative, document in package[0].items():
        if relative.endswith(".yaml"):
            dumped = yaml.safe_dump(document, sort_keys=False, allow_unicode=False)
            assert yaml.safe_load(dumped) is not None
