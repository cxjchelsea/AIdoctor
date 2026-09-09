#!/usr/bin/env python3
"""Release-local binding drift check. Manifest is the source of truth."""
from __future__ import annotations
import json, sys
from pathlib import Path
BINDINGS = Path(__file__).resolve().parents[1]
RELEASE = BINDINGS.parent
MANIFEST = json.loads((RELEASE / "manifest.json").read_text(encoding="utf-8-sig"))
EXPECTED_NAMES = [item["name"] for item in MANIFEST["contracts"]]
VERSION = MANIFEST["contract_version"]
errors = []
python_version = (BINDINGS / "python" / "aidoctor_shared_contracts" / "version.py").read_text(encoding="utf-8-sig")
java_version = (BINDINGS / "java" / "src" / "main" / "java" / "com" / "aidoctor" / "contracts" / "v1" / "ContractVersion.java").read_text(encoding="utf-8-sig")
ts_version = (BINDINGS / "typescript" / "src" / "version.ts").read_text(encoding="utf-8-sig")
python_models = (BINDINGS / "python" / "aidoctor_shared_contracts" / "models.py").read_text(encoding="utf-8-sig")
java_catalog = (BINDINGS / "java" / "src" / "main" / "java" / "com" / "aidoctor" / "contracts" / "v1" / "SchemaCatalog.java").read_text(encoding="utf-8-sig")
ts_types = (BINDINGS / "typescript" / "src" / "types.ts").read_text(encoding="utf-8-sig")
if 'CONTRACT_VERSION = "%s"' % VERSION not in python_version: errors.append("python version stamp drift")
if 'CONTRACT_VERSION = "%s"' % VERSION not in java_version: errors.append("java version stamp drift")
if 'CONTRACT_VERSION = "%s"' % VERSION not in ts_version: errors.append("typescript version stamp drift")
for name in EXPECTED_NAMES:
    if '"%s"' % name not in python_models and "'%s'" % name not in python_models: errors.append("python missing schema name %s" % name)
    if '"%s"' % name not in java_catalog: errors.append("java catalog missing %s" % name)
    if '"%s"' % name not in ts_types: errors.append("typescript missing schema name %s" % name)
    item = next(item for item in MANIFEST["contracts"] if item["name"] == name)
    if not (RELEASE / item["path"]).is_file(): errors.append("missing schema file %s" % item["path"])
if errors:
    sys.stderr.write("\n".join(errors) + "\n")
    raise SystemExit(1)
print("drift check passed for", VERSION, "schemas", len(EXPECTED_NAMES))
