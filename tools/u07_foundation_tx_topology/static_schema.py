"""Tier-0 SOURCE_ONLY evidence contracts. Never inspects an executing application."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import PurePosixPath

SOURCE_HEAD = "6d4fd787600e3a57f01f3e17893e6d98893ac546"
SOURCE_TREE = "1bfe776f76c986d4e6199a9b820f2cc20773a181"
ALLOWED_STATES = frozenset({"SOURCE_ONLY", "UNKNOWN", "NOT_IMPLEMENTED"})
FORBIDDEN_KEYS = frozenset({"configured_same_manager", "context_confirmed", "physical_transaction", "jdbc_url", "password", "secret", "patient_id", "tenant_id"})
SHA256 = re.compile(r"^[0-9a-f]{64}$")
GIT_SHA = re.compile(r"^[0-9a-f]{40}$")


class EvidenceError(ValueError):
    """Input is not adequate to support a source-only claim."""


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical_json(document: dict) -> bytes:
    validate_safe(document)
    return (json.dumps(document, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def validate_safe(value, path="$") -> None:
    """Reject forbidden authority fields; do not serialize potentially sensitive values."""
    if isinstance(value, dict):
        for key, item in value.items():
            if not isinstance(key, str):
                raise EvidenceError("non-string key at " + path)
            if key.lower() in FORBIDDEN_KEYS:
                raise EvidenceError("forbidden field at " + path)
            validate_safe(item, path + "." + key)
    elif isinstance(value, list):
        for i, item in enumerate(value):
            validate_safe(item, path + "[" + str(i) + "]")
    elif isinstance(value, str):
        low = value.lower()
        if any(x in low for x in ("jdbc:", "password=", "password:", "authorization: bearer", "-----begin private key", "http://", "https://")):
            raise EvidenceError("potential credential/endpoint at " + path)
        if "\\" in value or (value.startswith("/") and path.endswith(".source_path")):
            raise EvidenceError("unsafe absolute/source path at " + path)


def source_path(path: str) -> str:
    if not isinstance(path, str) or not path or path.startswith("/") or "\\" in path:
        raise EvidenceError("invalid source path")
    p = PurePosixPath(path)
    if ".." in p.parts or "." in p.parts or not path.startswith("diagnosis-service/src/main/java/") or not path.endswith(".java"):
        raise EvidenceError("unexpected source path")
    return path


def verify_inventory(inventory: dict) -> dict:
    if not isinstance(inventory, dict):
        raise EvidenceError("inventory JSON object required")
    if inventory.get("source_head") != SOURCE_HEAD or inventory.get("tree_sha") != SOURCE_TREE:
        raise EvidenceError("source head/tree mismatch")
    files = inventory.get("file_inventory")
    if not isinstance(files, list) or len(files) != 2112:
        raise EvidenceError("tracked file completeness mismatch")
    if (inventory.get("text_scanned"), inventory.get("binary_read_and_classified"), inventory.get("skipped_tracked_paths")) != (2063, 49, 0):
        raise EvidenceError("tracked scan coverage mismatch")
    by_path = {}
    for f in files:
        if not isinstance(f, dict) or not isinstance(f.get("path"), str) or f["path"] in by_path or not SHA256.fullmatch(str(f.get("sha256", ""))):
            raise EvidenceError("invalid or duplicate tracked file metadata")
        by_path[f["path"]] = f
    if len(by_path) != 2112:
        raise EvidenceError("incomplete tracked paths")
    matches = inventory.get("matches")
    if not isinstance(matches, list) or len(matches) != 256:
        raise EvidenceError("lexical match count mismatch")
    for m in matches:
        if not isinstance(m, dict) or m.get("path") not in by_path or not isinstance(m.get("line"), int) or m["line"] < 1:
            raise EvidenceError("invalid match location")
        if m.get("file_sha256") != by_path[m["path"]]["sha256"] or not GIT_SHA.fullmatch(str(m.get("file_git_blob", ""))):
            raise EvidenceError("match file digest/blob mismatch")
    return by_path


def verify_source_snapshot(path: str, raw: bytes, inventory_file: dict) -> dict:
    """Hash verified Java file snapshot; neither execute nor parse patient input."""
    source_path(path)
    if not isinstance(raw, bytes) or b"\0" in raw:
        raise EvidenceError("text snapshot required")
    if digest(raw) != inventory_file.get("sha256"):
        raise EvidenceError("source content digest mismatch")
    blob = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    return {"source_path": path, "source_sha256": digest(raw), "git_blob": blob}


def make_result(*, archive_sha256: str, inventory_sha256: str, nodes: list, edges: list, sources: list) -> dict:
    if not SHA256.fullmatch(archive_sha256) or not SHA256.fullmatch(inventory_sha256):
        raise EvidenceError("archive and inventory digests required")
    for node in nodes:
        if node.get("status") not in ALLOWED_STATES:
            raise EvidenceError("illegal node authority")
    for edge in edges:
        if edge.get("status") not in ALLOWED_STATES or edge.get("authority") != "DECLARED_SOURCE_EDGE":
            raise EvidenceError("illegal edge authority")
    result = {"schema": "U07EffectiveSpringTxTopologyV1-TIER0", "status": "SOURCE_ONLY", "source_head": SOURCE_HEAD,
              "source_tree": SOURCE_TREE, "input_archive_sha256": archive_sha256, "input_inventory_sha256": inventory_sha256,
              "nodes": nodes, "edges": edges, "sources": sources,
              "effective_manager": "UNKNOWN", "entity_manager_factory": "UNKNOWN", "datasource": "UNKNOWN",
              "spring_context_executed": False, "database_access": False,
              "limitations": ["No live proxy, transaction manager, datasource, transaction or COMMIT evidence.",
                              "Dynamic/reflection/external consumer coverage remains UNKNOWN.",
                              "Future U07 admission and binding implementation are NOT_IMPLEMENTED."]}
    canonical_json(result)
    return result
