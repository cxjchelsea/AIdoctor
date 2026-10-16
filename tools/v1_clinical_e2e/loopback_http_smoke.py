#!/usr/bin/env python3
"""Loopback-only nonproduction U01 TCP HTTP smoke test (synthetic inputs)."""
import argparse
import json
import urllib.error
import urllib.request
import uuid


def post(url, payload, timeout):
    request = urllib.request.Request(
        url, data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(request, timeout=timeout) as response:
        if response.status != 200:
            raise RuntimeError("Unexpected HTTP status: " + str(response.status))
        return json.load(response)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--timeout", type=float, default=10)
    args = parser.parse_args()
    base = args.base_url.rstrip("/")
    if not (base.startswith("http://127.0.0.1:") or base.startswith("http://localhost:")):
        parser.error("A loopback HTTP address with an explicit port is required")
    url = base + "/internal/nonproduction/v1/clinical/start"
    token = uuid.uuid4().hex
    payload = {"eventId": "synthetic-smoke-" + token,
               "idempotencyKey": "synthetic-idem-" + token,
               "userId": "synthetic-only",
               "rawText": "synthetic headache uncertainty"}
    first = post(url, payload, args.timeout)
    for name in ("consultationId", "cdpId", "runId"):
        if not isinstance(first.get(name), str) or not first[name]:
            raise RuntimeError("Missing U01 identity: " + name)
    if (first.get("entryStatus") != "U01_COMMITTED_DOWNSTREAM_NOT_EXECUTED"
            or first.get("completeClinicalE2e") is not False
            or first.get("patientDeliveryAllowed") is not False):
        raise RuntimeError("Unsafe or overstated U01 response")
    replay = post(url, payload, args.timeout)
    if any(first[key] != replay.get(key) for key in ("consultationId", "cdpId", "runId")):
        raise RuntimeError("U01 replay identity mismatch")
    print(json.dumps({"result": "U01_TCP_HTTP_SMOKE_PASS",
                      "downstreamClinicalE2eVerified": False,
                      "liveProviderVerified": False}))


if __name__ == "__main__":
    try:
        main()
    except (urllib.error.URLError, ValueError, RuntimeError) as error:
        raise SystemExit("U01_TCP_HTTP_SMOKE_FAIL: " + str(error))
