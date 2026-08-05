#!/usr/bin/env python3
"""Repeatable structural and semantic validation for Shared Contracts v1."""

from __future__ import annotations

import copy
import json
import re
import sys
from datetime import datetime
from pathlib import Path
from urllib.parse import unquote, urljoin, urlsplit

from jsonschema import Draft202012Validator, FormatChecker
from referencing import Registry, Resource

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "schemas"
VALID = ROOT / "fixtures" / "valid"
INVALID = ROOT / "fixtures" / "invalid"
MAX_SAFE_INTEGER = 9007199254740991
WRITABLE_PATCH_DOMAINS = {
    "patient_state", "ddx", "evidence_graph", "workup_plan",
    "management_plan", "triage", "uncertainty",
    "health_state_assessment", "wellness_plan",
}
PROTECTED_PATCH_TOKENS = {
    "version", "cdp_version", "id", "cdp_id", "patient_id", "tenant_id",
    "audit", "audit_info", "audit_trail", "trace", "trace_info",
    "created_at", "updated_at", "created_by", "updated_by", "status",
    "commit_status", "committed_status",
}


def read_json(path: Path):
    def unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError(f"duplicate JSON key {key!r} in {path}")
            result[key] = value
        return result
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)


def load_package():
    manifest = read_json(ROOT / "manifest.json")
    schemas = {item["name"]: read_json(ROOT / item["path"]) for item in manifest["contracts"]}
    valid = {}
    for path in sorted(VALID.glob("*.json")):
        overlap = valid.keys() & read_json(path).keys()
        if overlap:
            raise AssertionError(f"duplicate valid fixture keys: {sorted(overlap)}")
        valid.update(read_json(path))
    invalid = []
    for path in sorted(INVALID.glob("*.json")):
        invalid.extend(read_json(path))
    return manifest, schemas, valid, invalid


def build_registry(schemas):
    return Registry().with_resources(
        [(schema["$id"], Resource.from_contents(schema)) for schema in schemas.values()]
    )


def validator_for(name, schemas, registry):
    return Draft202012Validator(
        schemas[name], registry=registry, format_checker=FormatChecker()
    )


def pointer(parts):
    return "/" + "/".join(str(part).replace("~", "~0").replace("/", "~1") for part in parts)


def canonical_token(token):
    token = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", token)
    return re.sub(r"[-_]+", "_", token).lower()


def decode_pointer(path):
    if not isinstance(path, str) or not path.startswith("/") or path.startswith("//"):
        raise ValueError("path must start with exactly one slash")
    raw_segments = path[1:].split("/")
    if not raw_segments or any(segment == "" for segment in raw_segments):
        raise ValueError("empty path segments and trailing slashes are forbidden")
    decoded = []
    for raw in raw_segments:
        if re.search(r"~(?![01])", raw):
            raise ValueError("invalid JSON Pointer escape")
        token = raw.replace("~1", "/").replace("~0", "~")
        if token in {".", ".."}:
            raise ValueError("dot path segments are forbidden")
        if not token.isascii() or any(ord(char) < 0x20 or char.isspace() for char in token):
            raise ValueError("whitespace, controls, and non-ASCII confusables are forbidden")
        if not re.fullmatch(r"[A-Za-z0-9_-]+", token):
            raise ValueError("decoded path segments must use ASCII letters, digits, underscore, or hyphen")
        if token.isdigit():
            raise ValueError("array-index operations require a future field schema and are forbidden in v1")
        decoded.append(token)
    return decoded


def state_patch_path_errors(instance):
    errors = []
    for index, operation in enumerate(instance.get("operations", [])):
        path = operation.get("path")
        try:
            segments = decode_pointer(path)
        except ValueError as exc:
            errors.append(f"operations[{index}].path: {exc}")
            continue
        if len(segments) < 2:
            errors.append(f"operations[{index}].path: aggregate parent paths are forbidden")
            continue
        if segments[0] not in WRITABLE_PATCH_DOMAINS:
            errors.append(f"operations[{index}].path: top-level proposal domain is not allowed")
        protected = [segment for segment in segments if canonical_token(segment) in PROTECTED_PATCH_TOKENS]
        if protected:
            errors.append(f"operations[{index}].path: protected token {protected[0]!r} is forbidden")
    return errors


def storage_ref_errors(instance):
    errors = []
    ref = instance.get("storage_ref", "")
    if not isinstance(ref, str):
        return errors
    decoded = ref
    for _ in range(3):
        next_value = unquote(decoded)
        if next_value == decoded:
            break
        decoded = next_value
    if "%" in decoded and re.search(r"%[0-9A-Fa-f]{2}", decoded):
        errors.append("storage_ref remains encoded after bounded normalization")
    if not decoded.isascii() or any(ord(char) < 0x20 for char in decoded) or "\\" in decoded:
        errors.append("storage_ref contains unsafe characters")
        return errors
    parsed = urlsplit(decoded)
    if parsed.scheme not in {"artifact", "blob", "s3", "urn"}:
        errors.append("storage_ref scheme is not an approved logical scheme")
        return errors
    if parsed.scheme == "urn":
        segments = re.split(r"[:/]", parsed.path)
    else:
        if not parsed.netloc:
            errors.append("storage_ref authority is required")
        if "//" in parsed.path:
            errors.append("storage_ref contains repeated slashes")
        segments = parsed.path.split("/")[1:] if parsed.path.startswith("/") else parsed.path.split("/")
    if any(segment in {"", ".", ".."} for segment in segments):
        errors.append("storage_ref contains empty or traversal segments")
    if instance.get("artifact_id") in instance.get("derived_artifacts", []):
        errors.append("derived_artifacts cannot directly reference artifact_id")
    return errors


def mutate(instance, mutation):
    result = copy.deepcopy(instance)
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


def semantic_errors(name, instance):
    errors = []
    if name == "StatePatch":
        errors.extend(state_patch_path_errors(instance))
    if name == "CommitResult" and instance.get("status") == "COMMITTED":
        if instance.get("committed_version", -1) <= instance.get("previous_version", -1):
            errors.append("committed_version must be greater than previous_version")
    if name == "ToolContext":
        auth = instance.get("authorization_scope", {})
        if not set(auth.get("requested", [])) <= set(auth.get("granted", [])):
            errors.append("requested scopes cannot exceed granted scopes")
        created = datetime.fromisoformat(instance["envelope"]["created_at"].replace("Z", "+00:00"))
        deadline = datetime.fromisoformat(instance["deadline"].replace("Z", "+00:00"))
        if deadline < created:
            errors.append("deadline must not precede envelope.created_at")
    if name == "ToolResult":
        started = datetime.fromisoformat(instance["started_at"].replace("Z", "+00:00"))
        completed = datetime.fromisoformat(instance["completed_at"].replace("Z", "+00:00"))
        if completed < started:
            errors.append("completed_at must not precede started_at")
    if name == "EvidencePack":
        claims_list = instance.get("claims", [])
        sources_list = instance.get("sources", [])
        conflicts_list = instance.get("conflicts", [])
        claim_ids = [item["claim_id"] for item in claims_list]
        source_ids = [item["source_id"] for item in sources_list]
        conflict_ids = [item["conflict_id"] for item in conflicts_list]
        if len(claim_ids) != len(set(claim_ids)):
            errors.append("claim_id values must be unique")
        if len(source_ids) != len(set(source_ids)):
            errors.append("source_id values must be unique")
        if len(conflict_ids) != len(set(conflict_ids)):
            errors.append("conflict_id values must be unique")
        claims = {item["claim_id"]: item for item in claims_list}
        sources = {item["source_id"]: item for item in sources_list}
        for claim in claims_list:
            if claim.get("claim_type") == "MEDICAL_EVIDENCE" and claim.get("support_status") != "INSUFFICIENT_EVIDENCE" and not claim.get("citation_refs"):
                errors.append("supported medical evidence requires citations")
            if claim.get("claim_type") == "SAFETY_RULE" and claim.get("support_status") == "SUPPORTED" and not claim.get("citation_refs"):
                errors.append("safety rules do not inherit medical-evidence support")
            for source_id in claim.get("citation_refs", []):
                if source_id not in sources:
                    errors.append("citation reference must resolve inside sources")
            if claim.get("claim_type") == "SAFETY_RULE":
                for source_id in claim.get("citation_refs", []):
                    if source_id in sources and sources[source_id]["source_type"] != "RULE":
                        errors.append("safety rule citations must resolve to RULE sources")
            allowed_sources = {
                "MEDICAL_EVIDENCE": {"GUIDELINE", "KNOWLEDGE_GRAPH", "REFERENCE_TABLE", "RESEARCH"},
                "PATIENT_FACT": {"PATIENT_RECORD"},
                "CLINICIAN_DECISION": {"CLINICIAN_RECORD"},
                "SAFETY_RULE": {"RULE"},
            }[claim["claim_type"]]
            for source_id in claim.get("citation_refs", []):
                if source_id in sources and sources[source_id]["source_type"] not in allowed_sources:
                    errors.append(f"{claim['claim_type']} citation source type is not allowed")
        for conflict in conflicts_list:
            for claim_id in conflict.get("claim_refs", []):
                if claim_id not in claims:
                    errors.append("conflict claim reference must resolve inside claims")
            for source_id in conflict.get("source_refs", []):
                if source_id not in sources:
                    errors.append("conflict source reference must resolve inside sources")
        for source in sources_list:
            if source.get("valid_from") and source.get("valid_to"):
                valid_from = datetime.fromisoformat(source["valid_from"].replace("Z", "+00:00"))
                valid_to = datetime.fromisoformat(source["valid_to"].replace("Z", "+00:00"))
                if valid_to < valid_from:
                    errors.append("valid_to must not precede valid_from")
    if name == "SourceArtifact":
        errors.extend(storage_ref_errors(instance))
    if name == "KnowledgeReleaseRef":
        if instance.get("status") == "SUPERSEDED" and instance.get("superseded_by") == instance.get("knowledge_release_id"):
            errors.append("superseded_by cannot reference knowledge_release_id")
        if instance.get("status") == "WITHDRAWN" and instance.get("withdrawn_at"):
            released = datetime.fromisoformat(instance["released_at"].replace("Z", "+00:00"))
            withdrawn = datetime.fromisoformat(instance["withdrawn_at"].replace("Z", "+00:00"))
            if withdrawn < released:
                errors.append("withdrawn_at must not precede released_at")
    if name == "PatientDeliveryView":
        forbidden = {"raw_result","full_cdp","cdp","prompt","private_reasoning","chain_of_thought","reasoning_paths","confidence","tool_input","tool_output","provider_response","internal_hypothesis","internal_policy","execution_trace","other_patient_data"}
        def walk(value):
            if isinstance(value, dict):
                for key, nested in value.items():
                    if key.lower() in forbidden:
                        errors.append(f"patient-forbidden field: {key}")
                    walk(nested)
            elif isinstance(value, list):
                for nested in value:
                    walk(nested)
        walk(instance)
        cards = [card for section in instance.get("evidence_sections", []) for card in section.get("cards", [])]
        claim_ids = {card.get("claim_id") for card in cards}
        citation_ids = [source.get("citation_id") for card in cards for source in card.get("sources", [])]
        if len(citation_ids) != len(set(citation_ids)):
            errors.append("patient citation_id values must be unique")
        for card in cards:
            conflict = card.get("conflict_summary")
            if conflict:
                for claim_id in conflict.get("affected_claim_ids", []):
                    if claim_id not in claim_ids:
                        errors.append("patient conflict claim reference must resolve inside cards")
        has_medical = any(card.get("basis_type") == "MEDICAL_EVIDENCE" for card in cards)
        bindings = instance.get("version_bindings", {})
        if has_medical and (bindings.get("knowledge_release_id") is None or bindings.get("knowledge_release_version") is None):
            errors.append("medical delivery requires knowledge release bindings")
    return errors


def validate_exact_version(manifest, name, instance):
    if instance.get("contract_version") not in manifest.get("supported_versions", []):
        return ["contract_version is not supported by this exact-version manifest"]
    if name not in {item["name"] for item in manifest["contracts"]}:
        return ["contract name is not present in manifest"]
    return []


def validate_new_delivery_release(delivery, release):
    if release["status"] == "WITHDRAWN":
        return ["withdrawn knowledge release cannot be used for a new delivery"]
    bindings = delivery["version_bindings"]
    if bindings["knowledge_release_id"] != release["knowledge_release_id"] or bindings["knowledge_release_version"] != release["source_registry_version"]:
        return ["delivery knowledge binding does not match release"]
    return []


def validate_package():
    manifest, schemas, valid, invalid = load_package()
    files = {p.relative_to(ROOT).as_posix() for p in SCHEMAS.glob("*.json")}
    mapped = {item["path"] for item in manifest["contracts"]}
    assert files == mapped, f"manifest/schema mismatch: files={files ^ mapped}"
    assert manifest["version_negotiation"] == "EXACT"
    assert manifest["supported_versions"] == [manifest["contract_version"]]
    assert len({s["$id"] for s in schemas.values()}) == len(schemas), "schema IDs must be unique"
    assert all(item["id"] == schemas[item["name"]]["$id"] for item in manifest["contracts"])
    assert set(valid) == set(schemas), "every schema requires exactly one named valid fixture"
    counts = {name: 0 for name in schemas}
    registry = build_registry(schemas)
    ids = {s["$id"] for s in schemas.values()}
    for schema in schemas.values():
        Draft202012Validator.check_schema(schema)
        for ref in re.findall(r'"\$ref"\s*:\s*"([^"]+)"', json.dumps(schema)):
            target = urljoin(schema["$id"], ref.split("#", 1)[0]) if ref.split("#", 1)[0] else schema["$id"]
            assert target in ids, f"unresolved schema reference: {ref} from {schema['$id']}"
    for name, instance in valid.items():
        errors = list(validator_for(name, schemas, registry).iter_errors(instance))
        assert not errors, f"valid {name} failed: {[e.message for e in errors]}"
        assert not validate_exact_version(manifest, name, instance)
        assert not semantic_errors(name, instance), f"semantic failure in valid {name}: {semantic_errors(name, instance)}"
    for case in invalid:
        counts[case["schema"]] += 1
        instance = mutate(valid[case["base"]], case["mutation"])
        errors = list(validator_for(case["schema"], schemas, registry).iter_errors(instance))
        expected = case["expected"]
        assert errors, f"invalid fixture unexpectedly passed: {case}"
        assert any(e.validator == expected["validator"] and pointer(e.absolute_path) == expected["path"] for e in errors), f"expected {expected} but got {[(e.validator, pointer(e.absolute_path), e.message) for e in errors]}"
        assert expected["category"] and expected["reason"], "negative fixture metadata is incomplete"
    assert all(count >= 2 for count in counts.values()), f"each schema requires two invalid fixtures: {counts}"
    return {"schemas":len(schemas),"valid":len(valid),"invalid":len(invalid)}


if __name__ == "__main__":
    try:
        summary = validate_package()
    except Exception as exc:
        print(f"A5 CONTRACT VALIDATION FAILED: {exc}", file=sys.stderr)
        raise
    print(f"A5 CONTRACT VALIDATION PASSED: {summary['schemas']} schemas, {summary['valid']} valid fixtures, {summary['invalid']} invalid fixtures")
