#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import pathlib
import subprocess
import sys

TARGET = "261ee5525c8260e93db19173ffbde89a8af6810d"
EXPECTED = {
    "RDP-01": ("docs/current/06_开发单元/U05_RDP01_Consumer_Inbound_Contract_v0.1.md", "b59430861b153a04773ac7d151b03f22e3d6b296"),
    "RDP-02": ("docs/current/06_开发单元/U05_RDP02_D03_Policy_Owner_Decision_Contract_v0.1.md", "20ca8b707e003e5265f5976999577541bf80543e"),
    "RDP-03": ("docs/current/06_开发单元/U05_RDP03_State_Ownership_K09_P01_Mutation_Trace_Contract_v0.1.md", "f54dfab7352bd796700f5ba9009d5672631f6196"),
    "RDP-04": ("docs/current/06_开发单元/U05_RDP04_Downstream_Routing_SideEffect_Boundary_v0.1.md", "c313d152779da6421d417a9660ee8798f92e8e40"),
    "RDP-05": ("docs/current/06_开发单元/U05_RDP05_Readiness_Input_Dependency_Applicability_Contract_v0.1.md", "e4b5ea930220eaa175e1e1f66184ee56f53eb10a"),
    "RDP-06": ("docs/current/06_开发单元/U05_RDP06_Verification_Durable_Evidence_Plan_v0.1.md", "83242bb6fa1736665afda2c9882215c22b84e1c2"),
}
PHASES = {
    "PHASE-5": ("docs/current/05_业务闭环/业务闭环设计_V1.md", "0920743521d11e14831420a2cb3a8bd3054a6e65"),
    "PHASE-6": ("docs/current/06_开发单元/可验证开发单元拆分_V1.md", "b20ba9a9e28ba82db2737e8383c0c85f96f6674f"),
    "PHASE-8": ("docs/current/08_契约与数据/Contract与数据语义设计.md", "8f47d80314b5352d1dd5ee41b1ff0800b7e70362"),
    "PHASE-9": ("docs/current/09_Runtime与技术架构/Runtime与技术架构设计_V1.md", "20f4d3c7badbc373401c01a9f999d9fe568aa91f"),
}

def canonical(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")

def sha256_bytes(data):
    return hashlib.sha256(data).hexdigest()

def sha256_file(path):
    return sha256_bytes(pathlib.Path(path).read_bytes())

def git(repo, *args):
    return subprocess.check_output(["git", "-C", str(repo)] + list(args), text=True).strip()

def load(path):
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", required=True)
    parser.add_argument("--expectations", required=True)
    parser.add_argument("--precedence", required=True)
    parser.add_argument("--fixtures", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    repo = pathlib.Path(args.repo_root)
    head = git(repo, "rev-parse", "HEAD")
    if head != TARGET:
        raise SystemExit("target HEAD mismatch: %s" % head)

    contracts = []
    for ident, (path, expected_blob) in EXPECTED.items():
        actual = git(repo, "rev-parse", "HEAD:" + path)
        if actual != expected_blob:
            raise SystemExit("%s blob drift: %s" % (ident, actual))
        contracts.append({"id": ident, "path": path, "blob": actual})

    phases = []
    for ident, (path, expected_blob) in PHASES.items():
        actual = git(repo, "rev-parse", "HEAD:" + path)
        if actual != expected_blob:
            raise SystemExit("%s blob drift: %s" % (ident, actual))
        phases.append({"id": ident, "path": path, "blob": actual})

    authority_core = {
        "schema": "U05_CONTRACT_AUTHORITY_CORE_V0_1",
        "implementation_sha": TARGET,
        "contracts": contracts,
        "referenced_phases": phases,
    }
    authority_digest = sha256_bytes(canonical(authority_core))

    expectation = load(args.expectations)
    if expectation.get("contract_manifest_digest") != authority_digest:
        raise SystemExit("expectation contract_manifest_digest mismatch")
    precedence = load(args.precedence)
    if precedence.get("contract_manifest_digest") != authority_digest:
        raise SystemExit("precedence contract_manifest_digest mismatch")

    review_id = os.environ.get("U05_STATIC_REVIEW_ID", "").strip()
    review_head = os.environ.get("U05_STATIC_REVIEW_HEAD", "").strip()
    verifier_head = os.environ.get("U05_VERIFIER_SHA", "").strip()
    reviewed_expectation_sha = os.environ.get("U05_STATIC_EXPECTATION_SHA256", "").strip()
    reviewed_precedence_sha = os.environ.get("U05_STATIC_PRECEDENCE_SHA256", "").strip()
    reviewed_fixture_sha = os.environ.get("U05_STATIC_FIXTURE_SHA256", "").strip()
    if not review_id.isdigit():
        raise SystemExit("U05_STATIC_REVIEW_ID must be a numeric independent review id")
    if not review_head or review_head != verifier_head:
        raise SystemExit("static review head does not equal verifier head")

    actual_expectation_sha = sha256_file(args.expectations)
    actual_precedence_sha = sha256_file(args.precedence)
    actual_fixture_sha = sha256_file(args.fixtures)
    if reviewed_expectation_sha != actual_expectation_sha:
        raise SystemExit("reviewed expectation digest mismatch")
    if reviewed_precedence_sha != actual_precedence_sha:
        raise SystemExit("reviewed precedence digest mismatch")
    if reviewed_fixture_sha != actual_fixture_sha:
        raise SystemExit("reviewed fixture digest mismatch")

    final_manifest = {
        "schema": "U05_CONTRACT_MANIFEST_V0_1",
        "authority_core": authority_core,
        "authority_core_digest": authority_digest,
        "reviewed_inputs": {
            "expectation_oracle": {
                "sha256": actual_expectation_sha,
                "review_id": review_id,
            },
            "precedence_oracle": {
                "sha256": actual_precedence_sha,
                "review_id": review_id,
            },
            "fixture_manifest": {
                "sha256": actual_fixture_sha,
                "review_id": review_id,
            },
            "reviewed_verifier_head": review_head,
        },
    }

    output = pathlib.Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(canonical(final_manifest))
    print(authority_digest)

if __name__ == "__main__":
    main()
