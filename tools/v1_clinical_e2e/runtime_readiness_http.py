#!/usr/bin/env python3
"""Fail-closed HTTP readiness probe for a separately running nonproduction V1 host."""
import argparse
import json
import urllib.error
import urllib.request
from urllib.parse import urlsplit

PATH = "/internal/nonproduction/v1/clinical/runtime-readiness"
REQUIRED = frozenset((
    "u01", "trustedTraceAuthority", "governedC01Gateway",
    "stateCommitter", "independentClinicalEvidenceAttestation",
    "verifiedGateCScopeAuthority", "governedGateCInput",
    "governedRiskCandidateGateway", "u02TrustedHandoff",
    "u03U04Bridge", "u03U04Executor", "independentClinicalHost",
    "u01U04HttpEntry",
))


def assess(document):
    if not isinstance(document, dict):
        raise ValueError("Malformed readiness response")
    components = document.get("components")
    if not isinstance(components, dict) or not REQUIRED.issubset(components):
        raise ValueError("Required host diagnostics absent")
    if any(type(components[k]) is not bool for k in REQUIRED):
        raise ValueError("Host diagnostics must be booleans")
    if document.get("patientDeliveryAllowed") is not False:
        raise ValueError("Patient delivery must remain blocked")
    if document.get("fullClinicalE2eVerified") is not False:
        raise ValueError("Component readiness is not clinical E2E evidence")
    ready = all(components[k] for k in REQUIRED)
    status = ("COMPONENTS_PRESENT_NOT_E2E_VERIFIED"
              if ready else "MISSING_REQUIRED_COMPONENTS")
    if document.get("status") != status:
        raise ValueError("Readiness status inconsistent with host components")
    return {"hostComponentsPresent": ready,
            "missingComponents": sorted(k for k in REQUIRED if not components[k]),
            "fullClinicalE2eVerified": False}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--timeout", type=float, default=10.0)
    args = parser.parse_args()
    parsed = urlsplit(args.base_url)
    if parsed.scheme != "http" or parsed.hostname not in ("localhost", "127.0.0.1") \
            or parsed.port is None or parsed.username or parsed.password \
            or parsed.path not in ("", "/") or parsed.query or parsed.fragment:
        parser.error("A loopback-only HTTP URL with explicit port is required")
    url = args.base_url.rstrip("/") + PATH
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=args.timeout) as response:
            result = assess(json.load(response))
    except (OSError, urllib.error.URLError, ValueError) as error:
        raise SystemExit("V1_RUNTIME_HTTP_NOT_VERIFIED: " + str(error))
    print(json.dumps(result, sort_keys=True))
    if not result["hostComponentsPresent"]:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
