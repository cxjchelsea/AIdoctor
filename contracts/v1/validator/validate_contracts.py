#!/usr/bin/env python3
"""Repeatable structural and semantic validation for Shared Contracts v1."""

from __future__ import annotations

import copy
import json
import re
import sys
from datetime import datetime
from pathlib import Path
from urllib.parse import urljoin

from jsonschema import Draft202012Validator, FormatChecker
from referencing import Registry, Resource

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "schemas"
VALID = ROOT / "fixtures" / "valid"
INVALID = ROOT / "fixtures" / "invalid"


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
    if name == "CommitResult" and instance.get("status") == "COMMITTED":
        if instance.get("committed_version", -1) <= instance.get("previous_version", -1):
            errors.append("committed_version must be greater than previous_version")
    if name == "ToolContext":
        auth = instance.get("authorization_scope", {})
        if not set(auth.get("requested", [])) <= set(auth.get("granted", [])):
            errors.append("requested scopes cannot exceed granted scopes")
    if name == "ToolResult":
        started = datetime.fromisoformat(instance["started_at"].replace("Z", "+00:00"))
        completed = datetime.fromisoformat(instance["completed_at"].replace("Z", "+00:00"))
        if completed < started:
            errors.append("completed_at must not precede started_at")
    if name == "EvidencePack":
        sources = {item["source_id"]: item for item in instance.get("sources", [])}
        for claim in instance.get("claims", []):
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
        has_medical = any(card.get("card_type") == "MEDICAL_EVIDENCE" for section in instance.get("evidence_sections", []) for card in section.get("cards", []))
        bindings = instance.get("version_bindings", {})
        if has_medical and (bindings.get("knowledge_release_id") is None or bindings.get("knowledge_release_version") is None):
            errors.append("medical delivery requires knowledge release bindings")
    return errors


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
    assert len({s["$id"] for s in schemas.values()}) == len(schemas), "schema IDs must be unique"
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
