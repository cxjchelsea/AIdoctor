#!/usr/bin/env python3
"""Emit original main-bound OWNER_SCHEMA_SLICE SQL for disposable MySQL8 only.

No network I/O. Emits SQL to stdout, never connects or applies it. The caller
must enforce localhost:33326, schema name and an ephemeral MySQL service.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "diagnosis-service/src/test/resources/u07/d5o2/stage-a-manifest.json"
EXPECTED_MAIN = "435c456e7bf43601f0445f82932c3584fa1b8170"
EXPECTED_SCHEMA = "u07_d5o2_stage_a"
EXPECTED_BLOBS = (
    "73029495c5f46e8576e34f8c9a77644fb8c1c4c4",
    "ccc1aa30b8e9405bd07249bae1d3531853f7510b",
    "7ed5a82bfba3b3fed2666bee969a7fe276e67566",
)
EXPECTED_PATHS = (
    "diagnosis-service/src/main/resources/db/migration/V2__create_clinical_runtime_foundation.sql",
    "diagnosis-service/src/main/resources/db/migration/V3__create_clinical_consultation.sql",
    "diagnosis-service/src/main/resources/db/migration/V6__add_u06_wait_runtime.sql",
)


def main():
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if (manifest.get("source_main_sha") != EXPECTED_MAIN
            or manifest.get("schema_mode") != "OWNER_SCHEMA_SLICE"
            or manifest.get("database") != EXPECTED_SCHEMA
            or manifest.get("port") != 33326
            or manifest.get("profile") != "U07_D5O2_STAGE_A_SYNTHETIC"):
        raise SystemExit("STAGE_A_MANIFEST_DENIED")
    items = manifest["ddl"]
    if len(items) != 3 or tuple(x["path"] for x in items) != EXPECTED_PATHS:
        raise SystemExit("STAGE_A_DDL_PATH_DENIED")
    chunks = []
    for item, expected_blob in zip(items, EXPECTED_BLOBS):
        if item["git_blob"] != expected_blob:
            raise SystemExit("STAGE_A_MANIFEST_BLOB_DENIED")
        path = (ROOT / item["path"]).resolve()
        if not path.is_relative_to(ROOT):  # Python 3.9+ expected on CI.
            raise SystemExit("STAGE_A_PATH_ESCAPES_REPOSITORY")
        raw = path.read_bytes()
        actual = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
        if actual != expected_blob:
            raise SystemExit("STAGE_A_SOURCE_DDL_CHANGED")
        chunks.append(raw.decode("utf-8"))
    print("SET default_storage_engine=InnoDB;")
    print("\n".join(chunks))
    print("CREATE USER 'stagea_writer'@'%' IDENTIFIED BY 'synthetic-stagea-writer';")
    print("CREATE USER 'stagea_reader'@'%' IDENTIFIED BY 'synthetic-stagea-reader';")
    for table in (
        "clinical_runtime_binding", "canonical_business_event", "clinical_runtime_run",
        "clinical_consultation", "clinical_consultation_wait_effect",
        "clinical_runtime_thread_state", "clinical_runtime_wait_checkpoint",
        "u06_governed_execution_trace", "u06_delivery_authority", "u06_delivery_intent",
        "u06_delivery_attempt", "u06_delivery_receipt", "u06_delivery_confirmation",
        "u06_delivery_ledger",
    ):
        print(f"GRANT SELECT, INSERT, UPDATE, DELETE ON {EXPECTED_SCHEMA}.{table} TO 'stagea_writer'@'%';")
        print(f"GRANT SELECT ON {EXPECTED_SCHEMA}.{table} TO 'stagea_reader'@'%';")


if __name__ == "__main__":
    main()
