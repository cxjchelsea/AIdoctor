#!/usr/bin/env python3
"""Fail-closed exact-checkout Surefire evidence. No claim of production authority."""
import glob
import hashlib
import json
import os
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
MANIFEST=ROOT/"diagnosis-service/src/test/resources/u07/d5o2/stageb/p0-manifest.json"
def main():
 d=json.loads(MANIFEST.read_text())
 if os.environ.get("GITHUB_ACTIONS")!="true" or not os.environ.get("GITHUB_RUN_ID","").isdigit():
  raise SystemExit("P0_UNTRUSTED_RUNNER")
 sha=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
 subprocess.run(["git","merge-base","--is-ancestor",d["sourceMainSha"],sha],cwd=ROOT,check=True)
 required=set(d["expectedTestMethods"])
 seen=set()
 count=0
 for path in glob.glob(str(ROOT/"diagnosis-service/target/surefire-reports/TEST-*StageB*.xml")):
  xml=ET.parse(path).getroot()
  name=xml.attrib.get("name","").split(".")[-1]
  if name not in d["testClasses"]:raise SystemExit("P0_UNKNOWN_TEST_CLASS")
  if int(xml.attrib.get("failures",0)) or int(xml.attrib.get("errors",0)) or int(xml.attrib.get("skipped",0)):
   raise SystemExit("P0_TEST_FAILED_OR_SKIPPED")
  for case in xml.findall(".//testcase"):
   method=case.attrib.get("name")
   if method in seen or method not in required:raise SystemExit("P0_DUPLICATE_OR_UNEXPECTED_TEST")
   seen.add(method)
   if case.find("failure") is not None or case.find("error") is not None or case.find("skipped") is not None:
    raise SystemExit("P0_METHOD_NOT_SUCCESS")
   count+=1
 if seen!=required:raise SystemExit("P0_REQUIRED_TEST_MISSING")
 ddl=[]
 for entry in d["ddl"]:
  raw=(ROOT/entry["path"]).read_bytes()
  digest=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\\0"+raw).hexdigest()
  if digest!=entry["gitBlob"]:raise SystemExit("P0_DDL_MISMATCH")
  ddl.append({"path":entry["path"],"blob":digest,"sha256":hashlib.sha256(raw).hexdigest()})
 result={"schema":"STAGE_B_P0_ISOLATED_EVIDENCE_V1","sourceMainSha":d["sourceMainSha"],
  "testedCheckoutSha":sha,"workflowRunId":os.environ["GITHUB_RUN_ID"],
  "testCases":sorted(seen),"tests":count,"ddl":ddl,"authority":"UNKNOWN",
  "evidencePlane":"TEST_ONLY_REAL_MYSQL8","productionTopologyVerified":False,
  "realDeliveryAuthorized":False,"f8ResumeAuthorized":False,
  "scope":"P0_DUAL_MANAGER_AND_ONE_SQL_FAULT_ONLY"}
 output=ROOT/"diagnosis-service/target/u07-d5o2-stage-b-p0-evidence.json"
 output.write_text(json.dumps(result,indent=2,sort_keys=True)+"\n")
 print("STAGE_B_P0_TESTS_EXECUTED="+str(count))
if __name__=="__main__":main()
