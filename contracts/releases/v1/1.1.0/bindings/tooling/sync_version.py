#!/usr/bin/env python3
"""Synchronize release-local binding version stamps from this release manifest."""
from __future__ import annotations
import json
from pathlib import Path
BINDINGS = Path(__file__).resolve().parents[1]
RELEASE = BINDINGS.parent
MANIFEST = json.loads((RELEASE / "manifest.json").read_text(encoding="utf-8-sig"))
VERSION = MANIFEST["contract_version"]
NEGOTIATION = MANIFEST["version_negotiation"]
FAMILY = MANIFEST["schema_family"]
if NEGOTIATION != "EXACT":
    raise SystemExit("unexpected manifest negotiation policy: %s" % NEGOTIATION)
(BINDINGS / "python" / "aidoctor_shared_contracts" / "version.py").write_text(
    'CONTRACT_VERSION = "%s"\nSCHEMA_FAMILY = "%s"\nVERSION_NEGOTIATION = "%s"\nSUPPORTED_VERSIONS = ("%s",)\n' % (VERSION, FAMILY, NEGOTIATION, VERSION), encoding="utf-8")
(BINDINGS / "java" / "src" / "main" / "java" / "com" / "aidoctor" / "contracts" / "v1" / "ContractVersion.java").write_text(
    'package com.aidoctor.contracts.v1;\n\npublic final class ContractVersion {\n    public static final String CONTRACT_VERSION = "%s";\n    public static final String SCHEMA_FAMILY = "%s";\n    public static final String VERSION_NEGOTIATION = "%s";\n\n    private ContractVersion() { }\n}\n' % (VERSION, FAMILY, NEGOTIATION), encoding="utf-8")
(BINDINGS / "typescript" / "src" / "version.ts").write_text(
    'export const CONTRACT_VERSION = "%s" as const;\nexport const SCHEMA_FAMILY = "%s" as const;\nexport const VERSION_NEGOTIATION = "%s" as const;\nexport const SUPPORTED_VERSIONS = ["%s"] as const;\n' % (VERSION, FAMILY, NEGOTIATION, VERSION), encoding="utf-8")
print("synced version stamps to", VERSION)
