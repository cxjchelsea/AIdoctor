#!/usr/bin/env python3
"""检测三语版本戳与 13-schema 目录是否相对 manifest 漂移。"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

BINDINGS = Path(__file__).resolve().parents[1]
CONTRACTS_V1 = BINDINGS.parent
MANIFEST = json.loads((CONTRACTS_V1 / "manifest.json").read_text(encoding="utf-8"))
EXPECTED_NAMES = [item["name"] for item in MANIFEST["contracts"]]
VERSION = MANIFEST["contract_version"]
errors = []

if len(EXPECTED_NAMES) != 13:
    errors.append("manifest schema count is %s" % len(EXPECTED_NAMES))

python_version = (BINDINGS / "python" / "aidoctor_shared_contracts" / "version.py").read_text(encoding="utf-8")
java_version = (BINDINGS / "java" / "src" / "main" / "java" / "com" / "aidoctor" / "contracts" / "v1" / "ContractVersion.java").read_text(encoding="utf-8")
ts_version = (BINDINGS / "typescript" / "src" / "version.ts").read_text(encoding="utf-8")
python_models = (BINDINGS / "python" / "aidoctor_shared_contracts" / "models.py").read_text(encoding="utf-8")
java_catalog = (BINDINGS / "java" / "src" / "main" / "java" / "com" / "aidoctor" / "contracts" / "v1" / "SchemaCatalog.java").read_text(encoding="utf-8")
ts_types = (BINDINGS / "typescript" / "src" / "types.ts").read_text(encoding="utf-8")

if 'CONTRACT_VERSION = "%s"' % VERSION not in python_version:
    errors.append("python version stamp drift")
if 'CONTRACT_VERSION = "%s"' % VERSION not in java_version:
    errors.append("java version stamp drift")
if 'CONTRACT_VERSION = "%s"' % VERSION not in ts_version:
    errors.append("typescript version stamp drift")

for name in EXPECTED_NAMES:
    if '"%s"' % name not in python_models and "'%s'" % name not in python_models:
        errors.append("python missing schema name %s" % name)
    if '"%s"' % name not in java_catalog:
        errors.append("java catalog missing %s" % name)
    if '"%s"' % name not in ts_types:
        errors.append("typescript missing schema name %s" % name)
    schema_path = CONTRACTS_V1 / next(item["path"] for item in MANIFEST["contracts"] if item["name"] == name)
    if not schema_path.is_file():
        errors.append("missing schema file %s" % schema_path)

if errors:
    sys.stderr.write("\n".join(errors) + "\n")
    raise SystemExit(1)
print("drift check passed for", VERSION, "schemas", len(EXPECTED_NAMES))
