#!/usr/bin/env python3
"""从 contracts/v1/manifest.json 同步三语版本戳。SOURCE OF TRUTH 不是本脚本。"""

from __future__ import annotations

import json
from pathlib import Path

BINDINGS = Path(__file__).resolve().parents[1]
CONTRACTS_V1 = BINDINGS.parent
MANIFEST = json.loads((CONTRACTS_V1 / "manifest.json").read_text(encoding="utf-8"))
VERSION = MANIFEST["contract_version"]
NEGOTIATION = MANIFEST["version_negotiation"]
FAMILY = MANIFEST["schema_family"]

if VERSION != "1.0.0" or NEGOTIATION != "EXACT":
    raise SystemExit("unexpected manifest version policy: %s %s" % (VERSION, NEGOTIATION))

python_version = BINDINGS / "python" / "aidoctor_shared_contracts" / "version.py"
python_version.write_text(
    "# REVIEWED_BINDING version stamp. 必须与 contracts/v1/manifest.json 同步。\n"
    "# 由 tooling/sync_version.py 从 manifest 写入；禁止独立改版本号。\n\n"
    "CONTRACT_VERSION = \"%s\"\n"
    "SCHEMA_FAMILY = \"%s\"\n"
    "VERSION_NEGOTIATION = \"%s\"\n"
    "SUPPORTED_VERSIONS = (\"%s\",)\n" % (VERSION, FAMILY, NEGOTIATION, VERSION),
    encoding="utf-8",
)

java_version = BINDINGS / "java" / "src" / "main" / "java" / "com" / "aidoctor" / "contracts" / "v1" / "ContractVersion.java"
java_version.write_text(
    "package com.aidoctor.contracts.v1;\n\n"
    "/**\n"
    " * REVIEWED_BINDING 版本戳。必须与 contracts/v1/manifest.json 同步。\n"
    " * 由 tooling/sync_version.py 写入；禁止独立修改。\n"
    " */\n"
    "public final class ContractVersion {\n"
    "    public static final String CONTRACT_VERSION = \"%s\";\n"
    "    public static final String SCHEMA_FAMILY = \"%s\";\n"
    "    public static final String VERSION_NEGOTIATION = \"%s\";\n\n"
    "    private ContractVersion() {\n"
    "    }\n"
    "}\n" % (VERSION, FAMILY, NEGOTIATION),
    encoding="utf-8",
)

ts_version = BINDINGS / "typescript" / "src" / "version.ts"
ts_version.write_text(
    "// REVIEWED_BINDING version stamp. Must match contracts/v1/manifest.json.\n"
    "// Written by tooling/sync_version.py. Do not edit independently.\n\n"
    "export const CONTRACT_VERSION = \"%s\" as const;\n"
    "export const SCHEMA_FAMILY = \"%s\" as const;\n"
    "export const VERSION_NEGOTIATION = \"%s\" as const;\n"
    "export const SUPPORTED_VERSIONS = [\"%s\"] as const;\n" % (VERSION, FAMILY, NEGOTIATION, VERSION),
    encoding="utf-8",
)

print("synced version stamps to", VERSION)
