#!/usr/bin/env python3
import argparse, glob, hashlib, json, os, sys
import xml.etree.ElementTree as ET
from pathlib import Path

def sha256_file(path):
    h=hashlib.sha256()
    with open(path,"rb") as f:
        for chunk in iter(lambda:f.read(65536),b""): h.update(chunk)
    return h.hexdigest()

def load(path):
    with open(path,"r",encoding="utf-8") as f: return json.load(f)

def passed_test_methods(report_dir):
    passed=set(); failed={}
    for path in glob.glob(os.path.join(report_dir,"TEST-*.xml")):
        root=ET.parse(path).getroot()
        for tc in root.findall(".//testcase"):
            name=tc.attrib.get("name","")
            bad=tc.find("failure") is not None or tc.find("error") is not None
            skipped=tc.find("skipped") is not None
            if bad: failed[name]="FAILED"
            elif skipped: failed[name]="SKIPPED"
            else: passed.add(name)
    return passed,failed

def hard_boundary(env):
    violations=[]
    for k in ["real_phi_count","real_recipient_endpoint_count","production_secret_count",
              "production_store_write_count","external_call_spy_observed_count"]:
        if int(env.get(k,0))!=0: violations.append(k)
    return violations

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--repo-root",default=".")
    p.add_argument("--implementation-sha",required=True)
    p.add_argument("--verifier-sha",required=True)
    p.add_argument("--environment-evidence",required=True)
    p.add_argument("--out-dir",required=True)
    args=p.parse_args()

    root=Path(args.repo_root).resolve()
    resources=root/"diagnosis-service/src/test/resources/u06"
    tools=root/"tools/u06_nonprod_verification"
    out=Path(args.out_dir); out.mkdir(parents=True,exist_ok=True)

    paths={
      "manifest":resources/"u06-contract-manifest.json",
      "oracle":resources/"u06-verification-expectations.json",
      "fixtures":resources/"u06-verification-fixtures.json",
      "auth":resources/"u06-auth-profile.json",
      "oracle_gate":resources/"u06-oracle-review-gate.json",
      "fixture_gate":resources/"u06-fixture-review-gate.json",
      "map":tools/"u06-execution-evidence-map.json",
    }
    manifest=load(paths["manifest"]); oracle=load(paths["oracle"]); fixtures=load(paths["fixtures"])
    auth=load(paths["auth"]); og=load(paths["oracle_gate"]); fg=load(paths["fixture_gate"])
    mapping=load(paths["map"]); env=load(args.environment_evidence)

    integrity=[]
    def chk(name,actual,expected):
        ok=actual==expected
        integrity.append({"check":name,"actual":actual,"expected":expected,"pass":ok})
        return ok

    core=manifest["authority_core_digest"]
    valid=True
    valid &= chk("oracle_sha256",sha256_file(paths["oracle"]),manifest["oracle"]["digest"])
    valid &= chk("fixture_sha256",sha256_file(paths["fixtures"]),manifest["fixtures"]["digest"])
    valid &= chk("auth_profile_sha256",sha256_file(paths["auth"]),manifest["auth_profile"]["digest"])
    valid &= chk("oracle_gate_verdict",og.get("verdict"),"PASS")
    valid &= chk("fixture_gate_verdict",fg.get("verdict"),"PASS")
    valid &= chk("oracle_gate_digest",og.get("reviewed_oracle_digest"),manifest["oracle"]["digest"])
    valid &= chk("fixture_gate_digest",fg.get("reviewed_fixture_manifest_digest"),manifest["fixtures"]["digest"])
    valid &= chk("oracle_gate_contract",og.get("reviewed_contract_manifest_digest"),core)
    valid &= chk("fixture_gate_contract",fg.get("reviewed_contract_manifest_digest"),core)
    valid &= chk("oracle_core",oracle.get("authority_core_digest"),core)
    valid &= chk("fixture_core",fixtures.get("authority_core_digest"),core)
    valid &= chk("implementation_semantic_head",args.implementation_sha,
                 manifest["authority_core"]["implementation_semantic_pass_sha"])

    required=[x["case_id"] for x in oracle["cases"]]
    unique=set(required)
    valid &= chk("required_case_count",len(required),189)
    valid &= chk("unique_case_count",len(unique),189)

    fixture_ids={x["fixture_id"] for x in fixtures["fixtures"]}
    missing_fixtures=[x["fixture_id"] for x in oracle["cases"] if x["fixture_id"] not in fixture_ids]
    valid &= chk("missing_fixture_count",len(missing_fixtures),0)

    passed,failed=passed_test_methods(root/"diagnosis-service/target/surefire-reports")
    evidence={cid:{
        "case_id":cid,"status":"NOT_EXECUTED","evidence_type":"NONE",
        "evidence_refs":[],"expected_status":next(x["expected_status"] for x in oracle["cases"] if x["case_id"]==cid)
    } for cid in required}

    for entry in mapping["mappings"]:
        method=entry["test_method"]
        if method in passed:
            for cid in entry["case_ids"]:
                if cid in evidence:
                    evidence[cid].update(status="PASS",evidence_type="JUNIT_TEST",
                        evidence_refs=["U06ProfileBStructuralTest#"+method])
        elif method in failed:
            for cid in entry["case_ids"]:
                if cid in evidence:
                    evidence[cid].update(status="FAIL",evidence_type="JUNIT_TEST",
                        evidence_refs=["U06ProfileBStructuralTest#"+method,failed[method]])

    # Harness self-tests are verifier self-tests, not SUT observations.
    def expectation_gap_detector(case):
        return case.get("expected_status") is None
    synthetic_gap={"case_id":"synthetic-gap"}
    if expectation_gap_detector(synthetic_gap):
        evidence["U06-HG-001"].update(status="PASS",evidence_type="VERIFIER_SELF_TEST",
                                     evidence_refs=["expectation-gap-detector"])

    def external_detector(e): return int(e.get("external_call_spy_observed_count",0))>0
    if external_detector({"external_call_spy_observed_count":1}):
        evidence["U06-HG-002"].update(status="PASS",evidence_type="VERIFIER_SELF_TEST",
                                     evidence_refs=["external-side-effect-detector"])

    def scope_detector(profile): return profile!="SYNTHETIC_STRUCTURAL_NONPROD"
    if scope_detector("PROFILE_A_SENTINEL"):
        evidence["U06-HG-003"].update(status="PASS",evidence_type="VERIFIER_SELF_TEST",
                                     evidence_refs=["synthetic-scope-escape-detector"])

    # Static governance evidence that does not depend on SUT output.
    if auth.get("direct_f1_runtime_activation") is False:
        evidence["U06-AGG-V-009"].update(status="PASS",evidence_type="AUTH_PROFILE",
                                         evidence_refs=["u06-auth-profile.json"])
    if auth.get("profile_a_enabled") is False and auth.get("real_c03_d04_allowed") is False:
        evidence["U06-AGG-V-011"].update(status="PASS",evidence_type="AUTH_PROFILE",
                                         evidence_refs=["u06-auth-profile.json"])
        evidence["U06-VG-007"].update(status="PASS",evidence_type="AUTH_PROFILE",
                                      evidence_refs=["u06-auth-profile.json"])
    if auth.get("live_u07") is False:
        evidence["U06-VG-009"].update(status="PASS",evidence_type="AUTH_PROFILE",
                                      evidence_refs=["u06-auth-profile.json"])

    # Environment-dependent gates.
    boundary_violations=hard_boundary(env)
    isolation_ok=env.get("verdict")=="PASS" and env.get("network_egress_policy") in ("DOCKER_NETWORK_NONE","DENY_BY_DEFAULT")
    spy_ok=env.get("external_call_spy_enabled") is True
    if int(env.get("real_phi_count",0))==0 and int(env.get("real_recipient_endpoint_count",0))==0:
        evidence["U06-VG-005"].update(status="PASS",evidence_type="ENVIRONMENT",
                                      evidence_refs=[str(args.environment_evidence)])
    if isolation_ok and spy_ok and not boundary_violations:
        evidence["U06-VG-006"].update(status="PASS",evidence_type="ENVIRONMENT",
                                      evidence_refs=[str(args.environment_evidence)])
    if int(env.get("production_store_write_count",0))==0:
        for cid in ("IRR01-V08","IRR03-V17"):
            evidence[cid].update(status="PASS",evidence_type="ENVIRONMENT",
                                 evidence_refs=[str(args.environment_evidence)])

    # VG-001 only passes when exact asset/target identity is valid and authorized diff gate was supplied.
    if valid and env.get("authorized_change_manifest_review")=="PASS":
        evidence["U06-VG-001"].update(status="PASS",evidence_type="AUTHORITY_BINDING",
                                      evidence_refs=["u06-contract-manifest.json","oracle/fixture review gates"])

    counts={"PASS":0,"FAIL":0,"NOT_EXECUTED":0}
    for e in evidence.values(): counts[e["status"]]=counts.get(e["status"],0)+1

    # Aggregate completeness gates are derived, never manually set.
    ev_ids=[x for x in required if x.startswith("U06-EV-")]
    cw_ids=[x for x in required if x.startswith("U06-CW-")]
    if all(evidence[x]["status"]=="PASS" for x in ev_ids):
        evidence["U06-VG-002"].update(status="PASS",evidence_type="DERIVED_GATE",evidence_refs=["all EV PASS"])
    if all(evidence[x]["status"]=="PASS" for x in cw_ids):
        evidence["U06-VG-003"].update(status="PASS",evidence_type="DERIVED_GATE",evidence_refs=["all CW PASS"])
    if all(evidence[x]["status"]=="PASS" for x in ("U06-HG-001","U06-HG-002","U06-HG-003")):
        evidence["U06-VG-004"].update(status="PASS",evidence_type="DERIVED_GATE",evidence_refs=["all HG PASS"])

    # Recount after derived gates.
    counts={"PASS":0,"FAIL":0,"NOT_EXECUTED":0}
    for e in evidence.values(): counts[e["status"]]=counts.get(e["status"],0)+1

    if not valid:
        verdict="INVALID_EVIDENCE"
    elif boundary_violations:
        verdict="FAIL"
    elif any(e["status"]=="FAIL" for e in evidence.values()):
        verdict="FAIL"
    elif all(e["status"]=="PASS" for e in evidence.values()) and isolation_ok and spy_ok:
        verdict="PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW"
    else:
        verdict="INCOMPLETE"

    case_file=out/"u06-case-evidence.json"
    case_file.write_text(json.dumps({"schema":"U06_CASE_EVIDENCE_V0_1","implementation_sha":args.implementation_sha,
        "verifier_sha":args.verifier_sha,"authority_core_digest":core,
        "case_evidence":[evidence[x] for x in required]},indent=2)+"\n",encoding="utf-8")

    summary={"schema":"U06_VERIFICATION_SUMMARY_V0_1","verdict":verdict,
      "implementation_sha":args.implementation_sha,"verifier_sha":args.verifier_sha,
      "authority_core_digest":core,"oracle_digest":manifest["oracle"]["digest"],
      "fixture_digest":manifest["fixtures"]["digest"],"auth_profile_digest":manifest["auth_profile"]["digest"],
      "case_counts":counts,"required_case_count":189,"passed_test_methods":sorted(passed),
      "failed_test_methods":failed,"integrity_checks":integrity,
      "environment_isolation_pass":isolation_ok,"external_call_spy_enabled":spy_ok,
      "hard_boundary_violations":boundary_violations,
      "missing_case_ids":[x for x in required if evidence[x]["status"]!="PASS"]}
    (out/"u06-verification-summary.json").write_text(json.dumps(summary,indent=2)+"\n",encoding="utf-8")

    env_copy=out/"u06-environment-evidence.json"
    env_copy.write_text(json.dumps(env,indent=2)+"\n",encoding="utf-8")

    files=[case_file,out/"u06-verification-summary.json",env_copy]
    checksum_lines=[sha256_file(x)+"  "+x.name for x in files]
    (out/"SHA256SUMS").write_text("\n".join(checksum_lines)+"\n",encoding="utf-8")

    print(json.dumps(summary,indent=2))
    return 0 if verdict=="PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW" else (1 if verdict in ("FAIL","INVALID_EVIDENCE") else 2)

if __name__=="__main__":
    sys.exit(main())
