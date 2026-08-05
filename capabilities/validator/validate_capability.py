#!/usr/bin/env python3
"""Structural and lifecycle validation for adult_respiratory_v1 Capability Package."""

from __future__ import annotations

import copy
import hashlib
import json
import re
import sys
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urljoin

import yaml
from jsonschema import Draft202012Validator, FormatChecker
from referencing import Registry, Resource

CAPABILITIES_ROOT = Path(__file__).resolve().parents[1]
PACKAGE_DIR = CAPABILITIES_ROOT / "adult_respiratory_v1"
SCHEMAS_DIR = CAPABILITIES_ROOT / "schemas"
REPO_ROOT = CAPABILITIES_ROOT.parent
CONTRACTS_MANIFEST = REPO_ROOT / "contracts" / "v1" / "manifest.json"
MAX_SAFE_INTEGER = 9007199254740991
CAPABILITY_ID = "adult_respiratory_v1"
PACKAGE_VERSION = "1.0.0"
SCHEMA_VERSION = "1.0.0"
SHARED_CONTRACT_VERSION = "1.0.0"
SCHEMA_ID_PREFIX = "https://schemas.aidoctor.dev/capabilities/1.0.0/"

REQUIRED_SCHEMAS = {
    "capability-manifest.schema.json",
    "population-scope.schema.json",
    "terminology-pack.schema.json",
    "observation-profile.schema.json",
    "safety-pack.schema.json",
    "question-policy.schema.json",
    "hypothesis-pack.schema.json",
    "knowledge-policy.schema.json",
    "runtime-allowlist.schema.json",
    "delivery-policy.schema.json",
    "eval-case.schema.json",
}

REQUIRED_PACKAGE_RELATIVE = {
    "manifest.yaml",
    "README.md",
    "population/scope.yaml",
    "terminology/concepts.yaml",
    "terminology/synonyms.yaml",
    "terminology/code_mappings.yaml",
    "observations/observation_profile.yaml",
    "safety/red_flags.yaml",
    "safety/triage_rules.yaml",
    "safety/special_populations.yaml",
    "questions/mandatory.yaml",
    "questions/discriminators.yaml",
    "questions/stopping_rules.yaml",
    "hypotheses/limited_hypotheses.yaml",
    "knowledge/knowledge_policy.yaml",
    "knowledge/source_manifest.yaml",
    "runtime/tool_allowlist.yaml",
    "runtime/skill_allowlist.yaml",
    "runtime/prompt_allowlist.yaml",
    "runtime/model_routes.yaml",
    "runtime/context_policy.yaml",
    "delivery/delivery_policy.yaml",
    "evals/safety_cases.jsonl",
    "evals/extraction_cases.jsonl",
    "evals/question_cases.jsonl",
    "evals/rag_cases.jsonl",
    "evals/e2e_cases.jsonl",
}

PATH_TO_EXPECTED = {
    "population/scope.yaml": {"kind": "population"},
    "terminology/concepts.yaml": {"kind": "terminology", "pack_type": "CONCEPTS"},
    "terminology/synonyms.yaml": {"kind": "terminology", "pack_type": "SYNONYMS"},
    "terminology/code_mappings.yaml": {"kind": "terminology", "pack_type": "CODE_MAPPINGS"},
    "observations/observation_profile.yaml": {"kind": "observation"},
    "safety/red_flags.yaml": {"kind": "safety", "pack_type": "RED_FLAGS"},
    "safety/triage_rules.yaml": {"kind": "safety", "pack_type": "TRIAGE_RULES"},
    "safety/special_populations.yaml": {"kind": "safety", "pack_type": "SPECIAL_POPULATIONS"},
    "questions/mandatory.yaml": {"kind": "question", "policy_type": "MANDATORY"},
    "questions/discriminators.yaml": {"kind": "question", "policy_type": "DISCRIMINATOR"},
    "questions/stopping_rules.yaml": {"kind": "question", "policy_type": "STOPPING"},
    "hypotheses/limited_hypotheses.yaml": {"kind": "hypothesis"},
    "knowledge/knowledge_policy.yaml": {"kind": "knowledge", "pack_type": "KNOWLEDGE_POLICY"},
    "knowledge/source_manifest.yaml": {"kind": "knowledge", "pack_type": "SOURCE_MANIFEST"},
    "runtime/tool_allowlist.yaml": {"kind": "runtime", "allowlist_type": "TOOL"},
    "runtime/skill_allowlist.yaml": {"kind": "runtime", "allowlist_type": "SKILL"},
    "runtime/prompt_allowlist.yaml": {"kind": "runtime", "allowlist_type": "PROMPT"},
    "runtime/model_routes.yaml": {"kind": "runtime", "allowlist_type": "MODEL_ROUTE"},
    "runtime/context_policy.yaml": {"kind": "runtime", "allowlist_type": "CONTEXT_POLICY"},
    "delivery/delivery_policy.yaml": {"kind": "delivery"},
    "evals/safety_cases.jsonl": {"kind": "eval", "suite_type": "SAFETY"},
    "evals/extraction_cases.jsonl": {"kind": "eval", "suite_type": "EXTRACTION"},
    "evals/question_cases.jsonl": {"kind": "eval", "suite_type": "QUESTION"},
    "evals/rag_cases.jsonl": {"kind": "eval", "suite_type": "RAG"},
    "evals/e2e_cases.jsonl": {"kind": "eval", "suite_type": "E2E"},
}

EVIDENCE_LEVELS = {
    "DOCUMENTED",
    "CODE_CONFIRMED",
    "BUILD_VERIFIED",
    "RUNTIME_VERIFIED",
    "DATA_VERIFIED",
    "TEST_VERIFIED",
    "UNKNOWN",
    "BLOCKED",
    "PARTIALLY_VALIDATED",
}

ACTIVE_OR_LATER = {
    "EVALUATION",
    "SHADOW",
    "CLINICIAN_ASSIST",
    "RESTRICTED_PATIENT",
    "ACTIVE",
}

TREATMENT_TOKENS = {
    "PRESCRIPTION",
    "DOSAGE",
    "DOSE",
    "TREATMENT_RECOMMENDATION",
    "AUTOMATIC_TREATMENT",
    "PRESCRIPTION_OR_DOSAGE_CHANGE",
    "PRESCRIPTION_REQUEST",
    "DOSAGE_CHANGE_REQUEST",
}


@dataclass(frozen=True)
class ValidationIssue:
    file: str
    path: str
    validator: str
    category: str
    reason: str


class UniqueKeyLoader(yaml.SafeLoader):
    pass


def _unique_key_constructor(loader, node, deep=False):
    mapping = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in mapping:
            raise yaml.constructor.ConstructorError(
                "while constructing a mapping",
                node.start_mark,
                f"found duplicate key {key!r}",
                key_node.start_mark,
            )
        mapping[key] = loader.construct_object(value_node, deep=deep)
    return mapping


UniqueKeyLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
    _unique_key_constructor,
)


def issue(file: str, path: str, validator: str, category: str, reason: str) -> ValidationIssue:
    return ValidationIssue(file=file, path=path, validator=validator, category=category, reason=reason)


def pointer(parts) -> str:
    return "/" + "/".join(str(part).replace("~", "~0").replace("/", "~1") for part in parts)


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def sha256_bytes(data: bytes) -> str:
    # Normalize newlines so Windows CRLF checkouts remain integrity-stable.
    normalized = data.replace(b"\r\n", b"\n").replace(b"\r", b"\n")
    return hashlib.sha256(normalized).hexdigest()


def read_json(path: Path) -> Any:
    def unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError(f"duplicate JSON key {key!r} in {path}")
            result[key] = value
        return result

    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)


def read_yaml(path: Path) -> Any:
    return yaml.load(path.read_text(encoding="utf-8"), Loader=UniqueKeyLoader)


def read_jsonl(path: Path) -> list[Any]:
    rows = []
    for index, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue

        def unique_object(pairs, line_no=index):
            result = {}
            for key, value in pairs:
                if key in result:
                    raise ValueError(f"duplicate JSON key {key!r} in {path}:{line_no}")
                result[key] = value
            return result

        rows.append(json.loads(line, object_pairs_hook=unique_object))
    return rows


def unsafe_path(value: str) -> str | None:
    if not isinstance(value, str):
        return "path must be a string"
    if value.startswith("/") or re.match(r"^[A-Za-z]:[\\/]", value) or value.startswith("\\\\"):
        return "absolute host paths are forbidden"
    decoded = value
    for _ in range(3):
        nxt = unquote(decoded)
        if nxt == decoded:
            break
        decoded = nxt
    normalized = decoded.replace("\\", "/")
    parts = [part for part in normalized.split("/") if part not in {"", "."}]
    if ".." in parts or "%2e%2e" in value.lower() or "%2e." in value.lower():
        return "path traversal is forbidden"
    if not re.fullmatch(r"[A-Za-z0-9._/-]+", value):
        return "path contains disallowed characters"
    return None


def build_registry(schemas: dict[str, Any]) -> Registry:
    return Registry().with_resources(
        [(schema["$id"], Resource.from_contents(schema)) for schema in schemas.values()]
    )


def schema_name_from_id(schema_id: str) -> str:
    if not schema_id.startswith(SCHEMA_ID_PREFIX) or not schema_id.endswith(".schema.json"):
        raise ValueError(f"unexpected schema id: {schema_id}")
    return schema_id[len(SCHEMA_ID_PREFIX) :]


def load_schemas() -> dict[str, Any]:
    present = {path.name for path in SCHEMAS_DIR.glob("*.json")}
    if present != REQUIRED_SCHEMAS:
        missing = REQUIRED_SCHEMAS - present
        extra = present - REQUIRED_SCHEMAS
        raise AssertionError(f"schema set mismatch missing={sorted(missing)} extra={sorted(extra)}")
    schemas = {}
    for name in sorted(REQUIRED_SCHEMAS):
        schema = read_json(SCHEMAS_DIR / name)
        schemas[name] = schema
    ids = [schema["$id"] for schema in schemas.values()]
    if len(ids) != len(set(ids)):
        raise AssertionError("schema $id values must be unique")
    return schemas


def load_package_documents() -> dict[str, Any]:
    documents: dict[str, Any] = {}
    for relative in sorted(REQUIRED_PACKAGE_RELATIVE):
        path = PACKAGE_DIR / relative
        if not path.exists():
            raise FileNotFoundError(f"required package file missing: {relative}")
        if relative.endswith(".yaml"):
            documents[relative] = read_yaml(path)
        elif relative.endswith(".jsonl"):
            documents[relative] = read_jsonl(path)
        elif relative.endswith(".md"):
            documents[relative] = path.read_text(encoding="utf-8")
        else:
            raise AssertionError(f"unsupported package file type: {relative}")
    return documents


def validator_for(schema_name: str, schemas: dict[str, Any], registry: Registry) -> Draft202012Validator:
    return Draft202012Validator(
        schemas[schema_name],
        registry=registry,
        format_checker=FormatChecker(),
    )


def collect_schema_errors(
    schema_name: str,
    instance: Any,
    schemas: dict[str, Any],
    registry: Registry,
    file: str,
) -> list[ValidationIssue]:
    errors = []
    for err in validator_for(schema_name, schemas, registry).iter_errors(instance):
        errors.append(
            issue(
                file,
                pointer(err.absolute_path),
                err.validator,
                "schema",
                err.message,
            )
        )
    return errors


def parse_iso(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def walk_ids(value: Any, path: list[Any], collector: list[tuple[str, list[Any], str]]) -> None:
    if isinstance(value, dict):
        for key, nested in value.items():
            if key.endswith("_id") or key in {"asset_id", "case_id", "record_id", "reference_id", "rule_id"}:
                if isinstance(nested, str):
                    collector.append((key, path + [key], nested))
            walk_ids(nested, path + [key], collector)
    elif isinstance(value, list):
        for index, nested in enumerate(value):
            walk_ids(nested, path + [index], collector)


def semantic_validate(
    manifest: dict[str, Any],
    documents: dict[str, Any],
    schemas: dict[str, Any],
) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    file = "adult_respiratory_v1/manifest.yaml"

    if manifest.get("capability_id") != CAPABILITY_ID:
        issues.append(issue(file, "/capability_id", "const", "identity", "capability_id must be adult_respiratory_v1"))
    if manifest.get("package_version") != PACKAGE_VERSION:
        issues.append(issue(file, "/package_version", "const", "identity", "package_version must be 1.0.0 for A6 skeleton"))
    if manifest.get("schema_version") != SCHEMA_VERSION:
        issues.append(issue(file, "/schema_version", "const", "identity", "schema_version must be 1.0.0"))
    if manifest.get("compatible_shared_contract_version") != SHARED_CONTRACT_VERSION:
        issues.append(
            issue(
                file,
                "/compatible_shared_contract_version",
                "const",
                "contracts",
                "Shared Contracts must be exact 1.0.0",
            )
        )
    if manifest.get("lifecycle") != "DRAFT" and manifest.get("lifecycle") not in {
        "CLINICAL_REVIEW",
        "TECHNICAL_REVIEW",
        "EVALUATION",
        "SHADOW",
        "CLINICIAN_ASSIST",
        "RESTRICTED_PATIENT",
        "ACTIVE",
        "DEPRECATED",
        "RETIRED",
    }:
        issues.append(issue(file, "/lifecycle", "enum", "lifecycle", "unknown lifecycle status"))

    if "latest" in json.dumps(manifest).lower():
        issues.append(issue(file, "/", "forbidden", "versioning", "manifest must not use latest"))

    contracts = read_json(CONTRACTS_MANIFEST)
    if contracts.get("contract_version") != SHARED_CONTRACT_VERSION:
        issues.append(
            issue(
                file,
                "/compatible_shared_contract_version",
                "reference",
                "contracts",
                "contracts/v1 manifest is not exact 1.0.0",
            )
        )

    asset_ids = []
    asset_paths = []
    for index, asset in enumerate(manifest.get("assets", [])):
        asset_ids.append(asset.get("asset_id"))
        asset_paths.append(asset.get("path"))
        path_error = unsafe_path(asset.get("path", ""))
        if path_error:
            issues.append(issue(file, f"/assets/{index}/path", "path", "path-safety", path_error))
        relative = asset.get("path")
        absolute = PACKAGE_DIR / relative if relative else None
        if not relative or not absolute.exists():
            issues.append(
                issue(file, f"/assets/{index}/path", "exists", "reference", f"manifest path does not exist: {relative}")
            )
            continue
        expected_digest = asset.get("sha256")
        actual_digest = sha256_bytes(absolute.read_bytes())
        if expected_digest != actual_digest:
            issues.append(
                issue(
                    file,
                    f"/assets/{index}/sha256",
                    "checksum",
                    "integrity",
                    f"checksum mismatch for {relative}",
                )
            )
        schema_id = asset.get("schema_id", "")
        try:
            schema_name = schema_name_from_id(schema_id)
        except ValueError as exc:
            issues.append(issue(file, f"/assets/{index}/schema_id", "format", "schema", str(exc)))
            continue
        if schema_name not in schemas:
            issues.append(
                issue(file, f"/assets/{index}/schema_id", "reference", "schema", f"unknown schema_id {schema_id}")
            )

    if len(asset_ids) != len(set(asset_ids)):
        issues.append(issue(file, "/assets", "uniqueItems", "identity", "asset_id values must be unique"))
    if len(asset_paths) != len(set(asset_paths)):
        issues.append(issue(file, "/assets", "uniqueItems", "identity", "asset paths must be unique"))

    expected_asset_paths = set(PATH_TO_EXPECTED)
    listed = set(asset_paths)
    if listed != expected_asset_paths:
        issues.append(
            issue(
                file,
                "/assets",
                "completeness",
                "reference",
                f"manifest assets mismatch package files: {sorted(listed ^ expected_asset_paths)}",
            )
        )

    effective = manifest.get("effective_from")
    expires = manifest.get("expires_at")
    if effective and expires:
        if parse_iso(expires) < parse_iso(effective):
            issues.append(issue(file, "/expires_at", "order", "lifecycle", "expires_at must not precede effective_from"))
    rollback = manifest.get("rollback_version")
    if rollback == manifest.get("package_version"):
        issues.append(issue(file, "/rollback_version", "self-reference", "lifecycle", "rollback must not self-reference"))

    lifecycle = manifest.get("lifecycle")
    clinical = manifest.get("clinical_review_status")
    production = manifest.get("production_eligibility")
    runtime = manifest.get("runtime_adoption")
    evidence = manifest.get("overall_evidence")

    if evidence not in EVIDENCE_LEVELS:
        issues.append(issue(file, "/overall_evidence", "enum", "evidence", "illegal evidence level"))
    if evidence in {"RUNTIME_VERIFIED", "DATA_VERIFIED"}:
        issues.append(
            issue(
                file,
                "/overall_evidence",
                "forbidden",
                "evidence",
                "A6 skeleton must not claim RUNTIME_VERIFIED or DATA_VERIFIED",
            )
        )
    if clinical == "APPROVED" and lifecycle == "DRAFT":
        issues.append(issue(file, "/clinical_review_status", "matrix", "lifecycle", "DRAFT cannot claim clinical APPROVED"))
    if clinical == "APPROVED" and manifest.get("owner_status") != "ASSIGNED":
        issues.append(
            issue(
                file,
                "/clinical_review_status",
                "matrix",
                "lifecycle",
                "clinical APPROVED requires assigned owner",
            )
        )
    if production == "ELIGIBLE" and clinical != "APPROVED":
        issues.append(
            issue(
                file,
                "/production_eligibility",
                "matrix",
                "lifecycle",
                "production ELIGIBLE requires clinical APPROVED",
            )
        )
    if lifecycle == "ACTIVE" and (
        clinical != "APPROVED"
        or production != "ELIGIBLE"
        or runtime != "ENABLED"
        or manifest.get("owner_status") != "ASSIGNED"
    ):
        issues.append(
            issue(
                file,
                "/lifecycle",
                "matrix",
                "lifecycle",
                "ACTIVE requires assigned owner, clinical APPROVED, production ELIGIBLE, runtime ENABLED",
            )
        )
    if lifecycle in ACTIVE_OR_LATER and production != "ELIGIBLE":
        issues.append(
            issue(
                file,
                "/lifecycle",
                "matrix",
                "lifecycle",
                f"{lifecycle} requires production_eligibility ELIGIBLE",
            )
        )
    if runtime == "ENABLED" and lifecycle == "DRAFT":
        issues.append(issue(file, "/runtime_adoption", "matrix", "runtime", "DRAFT package cannot claim runtime ENABLED"))
    runtime_docs = [
        documents[relative]
        for relative, expected in PATH_TO_EXPECTED.items()
        if expected["kind"] == "runtime"
    ]
    empty_runtime_allowlists = all(not doc.get("references") for doc in runtime_docs)
    if runtime == "ENABLED" and empty_runtime_allowlists:
        issues.append(
            issue(
                file,
                "/runtime_adoption",
                "matrix",
                "runtime",
                "runtime ENABLED requires non-empty runtime allowlist references",
            )
        )

    safety_docs = [
        documents["safety/red_flags.yaml"],
        documents["safety/triage_rules.yaml"],
        documents["safety/special_populations.yaml"],
    ]
    empty_safety = all(not doc.get("rules") for doc in safety_docs)
    unapproved_safety = any(doc.get("review_status") != "APPROVED" for doc in safety_docs)
    if empty_safety and production == "ELIGIBLE":
        issues.append(
            issue(
                file,
                "/production_eligibility",
                "matrix",
                "safety",
                "empty Safety rules require production BLOCKED",
            )
        )
    if (empty_safety or unapproved_safety) and lifecycle == "ACTIVE":
        issues.append(
            issue(
                file,
                "/lifecycle",
                "matrix",
                "safety",
                "unapproved or empty Safety Pack cannot enter ACTIVE",
            )
        )
    for relative, doc in [
        ("safety/red_flags.yaml", documents["safety/red_flags.yaml"]),
        ("safety/triage_rules.yaml", documents["safety/triage_rules.yaml"]),
        ("safety/special_populations.yaml", documents["safety/special_populations.yaml"]),
    ]:
        if doc.get("rules") == [] and doc.get("production_eligibility") != "BLOCKED":
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/production_eligibility",
                    "matrix",
                    "safety",
                    "empty Safety rules must remain BLOCKED",
                )
            )
        if doc.get("fail_closed", {}).get("model_single_point_decision_allowed") is not False:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/fail_closed/model_single_point_decision_allowed",
                    "const",
                    "safety",
                    "Safety cannot rely on model single-point decisions",
                )
            )

    hypothesis = documents["hypotheses/limited_hypotheses.yaml"]
    empty_hypotheses = not hypothesis.get("hypotheses")
    if hypothesis.get("review_status") != "APPROVED" and lifecycle == "ACTIVE":
        issues.append(
            issue(
                file,
                "/lifecycle",
                "matrix",
                "hypothesis",
                "unapproved Hypothesis Pack cannot enter ACTIVE",
            )
        )
    if empty_hypotheses and lifecycle == "ACTIVE":
        issues.append(
            issue(
                file,
                "/lifecycle",
                "matrix",
                "hypothesis",
                "empty Hypothesis Pack cannot enter ACTIVE",
            )
        )
    if empty_hypotheses and production == "ELIGIBLE":
        issues.append(
            issue(
                file,
                "/production_eligibility",
                "matrix",
                "hypothesis",
                "empty Hypothesis Pack requires production BLOCKED",
            )
        )
    if hypothesis.get("automatic_diagnosis_allowed") is True:
        issues.append(
            issue(
                "adult_respiratory_v1/hypotheses/limited_hypotheses.yaml",
                "/automatic_diagnosis_allowed",
                "forbidden",
                "clinical-boundary",
                "automatic diagnosis is forbidden",
            )
        )
    if hypothesis.get("free_expansion_allowed") is True:
        issues.append(
            issue(
                "adult_respiratory_v1/hypotheses/limited_hypotheses.yaml",
                "/free_expansion_allowed",
                "forbidden",
                "clinical-boundary",
                "free disease-set expansion is forbidden",
            )
        )

    for relative in ("knowledge/knowledge_policy.yaml", "knowledge/source_manifest.yaml"):
        knowledge = documents[relative]
        approved_count = knowledge.get("approved_source_count")
        sources = knowledge.get("sources") or []
        if approved_count != len(sources):
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/approved_source_count",
                    "consistency",
                    "knowledge",
                    "approved_source_count must equal sources length",
                )
            )
        if approved_count == 0 and knowledge.get("retrieval_eligibility") != "BLOCKED":
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/retrieval_eligibility",
                    "matrix",
                    "knowledge",
                    "empty approved sources require knowledge runtime BLOCKED",
                )
            )
        if (approved_count == 0 or not sources) and lifecycle == "ACTIVE":
            issues.append(
                issue(
                    file,
                    "/lifecycle",
                    "matrix",
                    "knowledge",
                    "empty knowledge sources cannot enter ACTIVE",
                )
            )
        if (approved_count == 0 or not sources) and production == "ELIGIBLE":
            issues.append(
                issue(
                    file,
                    "/production_eligibility",
                    "matrix",
                    "knowledge",
                    "empty knowledge sources require production BLOCKED",
                )
            )
        if knowledge.get("review_status") != "APPROVED" and lifecycle == "ACTIVE":
            issues.append(
                issue(
                    file,
                    "/lifecycle",
                    "matrix",
                    "knowledge",
                    "unapproved Knowledge Pack cannot enter ACTIVE",
                )
            )
        if knowledge.get("mixed_index_allowed") is True:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/mixed_index_allowed",
                    "forbidden",
                    "knowledge",
                    "Patient and Medical knowledge must not be mixed",
                )
            )
        if knowledge.get("patient_knowledge_domain") != "PATIENT_PRIVATE":
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/patient_knowledge_domain",
                    "const",
                    "knowledge",
                    "patient knowledge domain must remain PATIENT_PRIVATE",
                )
            )
        if knowledge.get("medical_knowledge_domain") != "MEDICAL_APPROVED":
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/medical_knowledge_domain",
                    "const",
                    "knowledge",
                    "medical knowledge domain must remain MEDICAL_APPROVED",
                )
            )

    for relative, expected in PATH_TO_EXPECTED.items():
        if expected["kind"] != "runtime":
            continue
        runtime_doc = documents[relative]
        if runtime_doc.get("allowlist_type") != expected["allowlist_type"]:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/allowlist_type",
                    "const",
                    "runtime",
                    f"allowlist_type must be {expected['allowlist_type']}",
                )
            )
        for ref_index, reference in enumerate(runtime_doc.get("references") or []):
            registry_path = reference.get("registry_path", "")
            path_error = unsafe_path(registry_path)
            if path_error:
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        f"/references/{ref_index}/registry_path",
                        "path",
                        "path-safety",
                        path_error,
                    )
                )
                continue
            resolved = REPO_ROOT / registry_path
            if not resolved.exists():
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        f"/references/{ref_index}/registry_path",
                        "exists",
                        "runtime",
                        f"runtime reference does not resolve to an existing repository asset: {registry_path}",
                    )
                )
            if reference.get("review_status") != "APPROVED" and runtime_doc.get("runtime_eligibility") == "ELIGIBLE":
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        f"/references/{ref_index}/review_status",
                        "matrix",
                        "runtime",
                        "non-approved runtime references cannot enable eligibility",
                    )
                )
        if runtime_doc.get("references") and runtime_doc.get("runtime_eligibility") == "ELIGIBLE":
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/runtime_eligibility",
                    "matrix",
                    "runtime",
                    "A6 has no Model Runtime; runtime eligibility cannot be ELIGIBLE",
                )
            )
        if runtime_doc.get("runtime_eligibility") not in {"NOT_IMPLEMENTED", "BLOCKED"}:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/runtime_eligibility",
                    "matrix",
                    "runtime",
                    "A6 runtime eligibility must remain NOT_IMPLEMENTED or BLOCKED",
                )
            )

    delivery = documents["delivery/delivery_policy.yaml"]
    if delivery.get("patient_delivery_contract_version") != SHARED_CONTRACT_VERSION:
        issues.append(
            issue(
                "adult_respiratory_v1/delivery/delivery_policy.yaml",
                "/patient_delivery_contract_version",
                "const",
                "delivery",
                "PatientDeliveryView binding must be exact 1.0.0",
            )
        )
    if delivery.get("runtime_eligibility") not in {"NOT_IMPLEMENTED", "BLOCKED"}:
        issues.append(
            issue(
                "adult_respiratory_v1/delivery/delivery_policy.yaml",
                "/runtime_eligibility",
                "matrix",
                "delivery",
                "Delivery runtime is not implemented in A6",
            )
        )

    scope = documents["population/scope.yaml"]
    exclusions = set(scope.get("explicit_exclusions") or [])
    required_exclusions = {
        "AUTOMATIC_DIAGNOSIS",
        "AUTOMATIC_TREATMENT",
        "PRESCRIPTION_OR_DOSAGE_CHANGE",
        "OPEN_ENDED_RESPIRATORY_DIAGNOSIS",
        "HIGH_RISK_DECISION_WITHOUT_CLINICIAN_REVIEW",
        "PEDIATRIC_COMPLETE_PATHWAY",
        "PREGNANCY_COMPLETE_PATHWAY",
    }
    if not required_exclusions <= exclusions:
        issues.append(
            issue(
                "adult_respiratory_v1/population/scope.yaml",
                "/explicit_exclusions",
                "required",
                "clinical-boundary",
                "required first-version exclusions are incomplete",
            )
        )
    for token in TREATMENT_TOKENS:
        if token in (scope.get("supported_chief_complaint_categories") or []):
            issues.append(
                issue(
                    "adult_respiratory_v1/population/scope.yaml",
                    "/supported_chief_complaint_categories",
                    "forbidden",
                    "clinical-boundary",
                    f"treatment/prescription capability {token} cannot enter automatic path",
                )
            )

    case_ids: list[str] = []
    for relative, expected in PATH_TO_EXPECTED.items():
        if expected["kind"] != "eval":
            continue
        for case_index, case in enumerate(documents[relative]):
            case_ids.append(case.get("case_id"))
            if case.get("suite_type") != expected["suite_type"]:
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        f"/{case_index}/suite_type",
                        "const",
                        "eval",
                        f"suite_type must be {expected['suite_type']}",
                    )
                )
            if case.get("synthetic") is not True:
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        f"/{case_index}/synthetic",
                        "const",
                        "eval",
                        "eval cases must be synthetic",
                    )
                )
            if case.get("patient_data_classification") != "SYNTHETIC_NON_PATIENT":
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        f"/{case_index}/patient_data_classification",
                        "const",
                        "eval",
                        "real patient data classification is forbidden",
                    )
                )
            payload = json.dumps(case, ensure_ascii=False)
            if re.search(r"\b(?:REAL_PATIENT|PHI|PII)\b", payload):
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        f"/{case_index}",
                        "forbidden",
                        "eval",
                        "eval case appears to claim real patient data",
                    )
                )
    if len(case_ids) != len(set(case_ids)):
        issues.append(issue("adult_respiratory_v1/evals", "/", "uniqueItems", "eval", "eval case_id values must be unique"))

    for relative, expected in PATH_TO_EXPECTED.items():
        doc = documents[relative]
        if expected["kind"] == "eval":
            continue
        if not isinstance(doc, dict):
            continue
        if doc.get("capability_id") != CAPABILITY_ID:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/capability_id",
                    "const",
                    "identity",
                    "capability_id drift",
                )
            )
        if doc.get("package_version") != PACKAGE_VERSION:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/package_version",
                    "const",
                    "identity",
                    "package_version drift",
                )
            )
        if doc.get("schema_version") != SCHEMA_VERSION:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/schema_version",
                    "const",
                    "identity",
                    "schema_version drift",
                )
            )
        if expected["kind"] == "safety" and doc.get("pack_type") != expected["pack_type"]:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/pack_type",
                    "const",
                    "identity",
                    f"pack_type must be {expected['pack_type']}",
                )
            )
        if expected["kind"] == "terminology" and doc.get("pack_type") != expected["pack_type"]:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/pack_type",
                    "const",
                    "identity",
                    f"pack_type must be {expected['pack_type']}",
                )
            )
        if expected["kind"] == "knowledge" and doc.get("pack_type") != expected["pack_type"]:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/pack_type",
                    "const",
                    "identity",
                    f"pack_type must be {expected['pack_type']}",
                )
            )
        if expected["kind"] == "question" and doc.get("policy_type") != expected["policy_type"]:
            issues.append(
                issue(
                    f"adult_respiratory_v1/{relative}",
                    "/policy_type",
                    "const",
                    "identity",
                    f"policy_type must be {expected['policy_type']}",
                )
            )
        governance = doc.get("governance")
        if isinstance(governance, dict):
            if governance.get("prohibited_runtime_use") is not True:
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        "/governance/prohibited_runtime_use",
                        "const",
                        "governance",
                        "A6 clinical/runtime gaps must prohibit runtime use",
                    )
                )
            if governance.get("review_status") == "APPROVED":
                issues.append(
                    issue(
                        f"adult_respiratory_v1/{relative}",
                        "/governance/review_status",
                        "forbidden",
                        "governance",
                        "A6 skeleton must not mark governance APPROVED without clinical authorization",
                    )
                )

    id_records: list[tuple[str, str, list[Any], str]] = []
    for relative, doc in documents.items():
        if relative.endswith(".md"):
            continue
        collected: list[tuple[str, list[Any], str]] = []
        walk_ids(doc, [], collected)
        for key, path_parts, value in collected:
            id_records.append((relative, key, path_parts, value))
    seen: dict[str, str] = {}
    for relative, key, path_parts, value in id_records:
        file_label = f"adult_respiratory_v1/{relative}"
        if key == "case_id":
            continue
        if key == "record_id":
            previous = seen.get(f"record:{value}")
            if previous:
                issues.append(
                    issue(
                        file_label,
                        pointer(path_parts),
                        "uniqueItems",
                        "identity",
                        f"duplicate record_id {value} also seen at {previous}",
                    )
                )
            seen[f"record:{value}"] = f"{file_label}:{pointer(path_parts)}"
        if key == "asset_id" and relative != "manifest.yaml":
            previous = seen.get(f"asset:{value}")
            if previous:
                issues.append(
                    issue(
                        file_label,
                        pointer(path_parts),
                        "uniqueItems",
                        "identity",
                        f"duplicate asset_id {value} also seen at {previous}",
                    )
                )
            seen[f"asset:{value}"] = f"{file_label}:{pointer(path_parts)}"

    readme = documents["README.md"]
    required_markers = [
        "DRAFT",
        "PARTIALLY_VALIDATED",
        "REQUIRES_CLINICAL_REVIEW",
        "NOT_IMPLEMENTED",
        "BLOCKED",
    ]
    for marker in required_markers:
        if marker not in readme:
            issues.append(
                issue(
                    "adult_respiratory_v1/README.md",
                    "/",
                    "consistency",
                    "documentation",
                    f"README missing required status marker {marker}",
                )
            )
    if "CLINICALLY_VALIDATED" in readme or "production ready" in readme.lower():
        issues.append(
            issue(
                "adult_respiratory_v1/README.md",
                "/",
                "forbidden",
                "documentation",
                "README must not over-claim clinical or production readiness",
            )
        )

    root_readme = (CAPABILITIES_ROOT / "README.md").read_text(encoding="utf-8")
    for marker in ("DRAFT", "BLOCKED", "clinical review"):
        if marker.lower() not in root_readme.lower() and marker not in root_readme:
            issues.append(
                issue(
                    "capabilities/README.md",
                    "/",
                    "consistency",
                    "documentation",
                    f"capabilities README missing marker {marker}",
                )
            )

    return issues


def validate_documents_against_schemas(
    manifest: dict[str, Any],
    documents: dict[str, Any],
    schemas: dict[str, Any],
    registry: Registry,
) -> list[ValidationIssue]:
    issues = collect_schema_errors(
        "capability-manifest.schema.json",
        manifest,
        schemas,
        registry,
        "adult_respiratory_v1/manifest.yaml",
    )
    for asset in manifest.get("assets", []):
        relative = asset.get("path")
        schema_id = asset.get("schema_id")
        if not relative or not schema_id:
            continue
        path_error = unsafe_path(relative)
        if path_error or relative not in documents:
            # Missing/unsafe paths are reported by semantic_validate; avoid KeyError here.
            continue
        try:
            schema_name = schema_name_from_id(schema_id)
        except ValueError:
            continue
        document = documents[relative]
        if relative.endswith(".jsonl"):
            for index, row in enumerate(document):
                issues.extend(
                    collect_schema_errors(
                        schema_name,
                        row,
                        schemas,
                        registry,
                        f"adult_respiratory_v1/{relative}#{index}",
                    )
                )
        else:
            issues.extend(
                collect_schema_errors(
                    schema_name,
                    document,
                    schemas,
                    registry,
                    f"adult_respiratory_v1/{relative}",
                )
            )
    return issues


def validate_schema_metadata(schemas: dict[str, Any]) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    ids = {schema["$id"] for schema in schemas.values()}
    for name, schema in schemas.items():
        file = f"schemas/{name}"
        if schema.get("$schema") != "https://json-schema.org/draft/2020-12/schema":
            issues.append(issue(file, "/$schema", "const", "schema", "must use Draft 2020-12"))
        if not schema.get("$id", "").startswith(SCHEMA_ID_PREFIX):
            issues.append(issue(file, "/$id", "format", "schema", "stable capability schema $id required"))
        if not schema.get("title") or not schema.get("description"):
            issues.append(issue(file, "/", "required", "schema", "title and description are required"))
        description = schema.get("description", "").lower()
        if re.search(r"\b(is clinically valid|asserts clinical validity|proves clinical)\b", description):
            issues.append(issue(file, "/description", "forbidden", "schema", "schema must not claim clinical validity"))
        Draft202012Validator.check_schema(schema)
        for ref in re.findall(r'"\$ref"\s*:\s*"([^"]+)"', json.dumps(schema)):
            target = urljoin(schema["$id"], ref.split("#", 1)[0]) if ref.split("#", 1)[0] else schema["$id"]
            if target not in ids:
                issues.append(issue(file, "/", "reference", "schema", f"unresolved $ref {ref}"))

        def walk(node: Any, location: str) -> None:
            if isinstance(node, dict):
                if node.get("type") == "object" and "properties" in node and node.get("additionalProperties") is not False:
                    issues.append(issue(file, location, "additionalProperties", "schema", "objects must close additionalProperties"))
                value_type = node.get("type")
                if value_type == "string" or (isinstance(value_type, list) and "string" in value_type):
                    if not any(key in node for key in ("maxLength", "enum", "const", "format", "pattern")):
                        issues.append(issue(file, location, "bounded", "schema", "strings require bounds"))
                if value_type == "array" and "maxItems" not in node:
                    issues.append(issue(file, location, "bounded", "schema", "arrays require maxItems"))
                if value_type == "integer" or (isinstance(value_type, list) and "integer" in value_type):
                    maximum = node.get("maximum")
                    if maximum is not None and maximum > MAX_SAFE_INTEGER:
                        issues.append(issue(file, location, "maximum", "schema", "integer maximum exceeds JS safe integer"))
                for key, nested in node.items():
                    walk(nested, f"{location}/{key}")
            elif isinstance(node, list):
                for index, nested in enumerate(node):
                    walk(nested, f"{location}/{index}")

        walk(schema, "")
    return issues


def validate_package(documents: dict[str, Any] | None = None) -> dict[str, Any]:
    schemas = load_schemas()
    meta_issues = validate_schema_metadata(schemas)
    if meta_issues:
        raise AssertionError(format_issues(meta_issues))
    docs = documents if documents is not None else load_package_documents()
    manifest = docs["manifest.yaml"]
    registry = build_registry(schemas)
    issues = []
    issues.extend(validate_documents_against_schemas(manifest, docs, schemas, registry))
    issues.extend(semantic_validate(manifest, docs, schemas))
    if issues:
        raise AssertionError(format_issues(issues))
    return {
        "schemas": len(schemas),
        "assets": len(manifest["assets"]),
        "eval_cases": sum(len(docs[path]) for path in docs if path.endswith(".jsonl")),
        "issues": 0,
    }


def format_issues(issues: list[ValidationIssue]) -> str:
    lines = ["A6 CAPABILITY VALIDATION FAILED:"]
    for item in issues:
        lines.append(
            f"- file={item.file} path={item.path} validator={item.validator} "
            f"category={item.category} reason={item.reason}"
        )
    return "\n".join(lines)


def mutate_manifest(manifest: dict[str, Any], mutation: dict[str, Any]) -> dict[str, Any]:
    result = copy.deepcopy(manifest)
    parts = [p.replace("~1", "/").replace("~0", "~") for p in mutation["path"].split("/")[1:]]
    parent = result
    for part in parts[:-1]:
        parent = parent[int(part)] if isinstance(parent, list) else parent[part]
    key = parts[-1]
    if mutation["op"] == "remove":
        parent.pop(int(key)) if isinstance(parent, list) else parent.pop(key)
    elif mutation["op"] in {"add", "replace"}:
        if isinstance(parent, list):
            parent[int(key)] = mutation["value"]
        else:
            parent[key] = mutation["value"]
    else:
        raise AssertionError(f"unsupported mutation: {mutation['op']}")
    return result


def validate_mutated_package(documents: dict[str, Any]) -> list[ValidationIssue]:
    """Return issues for an in-memory package without raising."""
    schemas = load_schemas()
    registry = build_registry(schemas)
    manifest = documents["manifest.yaml"]
    issues = []
    issues.extend(validate_documents_against_schemas(manifest, documents, schemas, registry))
    issues.extend(semantic_validate(manifest, documents, schemas))
    return issues


def issues_as_dicts(issues: list[ValidationIssue]) -> list[dict[str, str]]:
    return [asdict(item) for item in issues]


if __name__ == "__main__":
    try:
        summary = validate_package()
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1) from exc
    print(
        "A6 CAPABILITY VALIDATION PASSED: "
        f"{summary['schemas']} schemas, {summary['assets']} assets, "
        f"{summary['eval_cases']} eval cases, {summary['issues']} issues"
    )
