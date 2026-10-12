#!/usr/bin/env python3
"""Fail closed on absent / contradictory original Surefire + test-origin facts."""
import glob
import hashlib
import json
import os
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[4]
MANIFEST=ROOT/"diagnosis-service/src/test/resources/u07/d5o2/stageb/p1/manifest.json"
import re
HEX64=re.compile(r"^[0-9a-f]{64}$")
ZERO=[0]*6
ONE=[1]*6
def fail(msg):raise SystemExit("P1A_EVIDENCE_DENIED_"+msg)
def main():
 d=json.loads(MANIFEST.read_text())
 if os.getenv("GITHUB_ACTIONS")!="true" or not os.getenv("GITHUB_RUN_ID","").isdigit():fail("RUNNER")
 head=os.getenv("U07_D5O2_PR_HEAD_SHA","")
 if len(head)!=40 or any(x not in "0123456789abcdef" for x in head):fail("PR_HEAD")
 sha=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
 second=subprocess.check_output(["git","rev-parse","HEAD^2"],cwd=ROOT,text=True).strip()
 if second!=head or head==sha:fail("SYNTHETIC_HEAD_IDENTITY")
 subprocess.run(["git","merge-base","--is-ancestor",d["sourceMainSha"],sha],cwd=ROOT,check=True)
 required=d["expectedCaseIds"]
 if len(required)!=8 or len(set(required.values()))!=8:fail("MANIFEST_CASES")
 reports=glob.glob(str(ROOT/"diagnosis-service/target/surefire-reports/TEST-*StageBP1*.xml"))
 methods=set()
 method_classes={}
 if len(reports)!=2:fail("SUREFIRE_CLASS_COUNT")
 for report in reports:
  xml=ET.parse(report).getroot()
  if xml.attrib.get("name","").split(".")[-1] not in d["testClasses"]:fail("CLASS")
  if any(int(xml.attrib.get(k,"0")) for k in ("failures","errors","skipped")):fail("TEST_FAILURE")
  suite_class=xml.attrib.get("name","").split(".")[-1]
  for case in xml.findall(".//testcase"):
   name=case.attrib.get("name")
   if name not in required or name in methods:fail("METHOD_MISMATCH")
   if any(case.find(k) is not None for k in ("failure","error","skipped")):fail("CASE_NOT_PASSED")
   if case.attrib.get("classname","").split(".")[-1]!=suite_class:fail("CASE_CLASS")
   method_classes[name]=suite_class
   methods.add(name)
 if methods!=set(required):fail("METHODS_MISSING")
 obs=ROOT/"diagnosis-service/target/stage-b-p1a-observations"
 actual=list(obs.glob("*.json")) if obs.exists() else []
 if len(actual)!=8:fail("OBSERVATION_COUNT")
 entries={}
 for name,caseid in required.items():
  file=obs/(name+".json")
  if not file.is_file():fail("OBS_MISSING_"+name)
  fact=json.loads(file.read_text())
  def digest(field):return isinstance(fact.get(field),str) and bool(HEX64.fullmatch(fact[field]))
  if (fact.get("testMethod")!=name or fact.get("caseId")!=caseid
      or fact.get("junitClass")!=method_classes[name]
      or fact.get("actualPrHeadSha")!=head
      or fact.get("testedSyntheticMergeCheckoutSha")!=sha
      or fact.get("workflowRunId")!=os.environ["GITHUB_RUN_ID"]
      or fact.get("readerDmlDenied") is not True
      or fact.get("writerIsolation")!=8 or fact.get("sqlSignatureStrict") is not True
      or not digest("candidateEffectSha256")
      or fact.get("sourceMainSha")!=d["sourceMainSha"]
      or fact.get("productionTopologyVerified") is not False
      or fact.get("downstreamAuthority") is not False
      or fact.get("realD5Ready") is not False
      or fact.get("evidencePlane")!="TEST_ONLY_REAL_MYSQL8"):fail("FACT_AUTH_"+name)
  if name.startswith("fault"):
   n=int(name[-2:])
   if (fact.get("faultOrdinal")!=n or fact.get("successfulOriginalSqlWrites")!=n
       or fact.get("methodOutcome")!="ORIGINAL_THROW"
       or fact.get("physicalTransactionResult")!="ROLLED_BACK_OBSERVED"
       or fact.get("sixBefore")!=ZERO or fact.get("sixInFlight")!=ZERO
       or fact.get("sixAfter")!=ZERO or fact.get("healthyBefore")!=ONE
       or fact.get("healthyAfter")!=ONE or fact.get("freshObserver") is not True
       or fact.get("testVerdict")!="ATOMICITY_PASS"
       or not str(fact.get("exceptionClass","")).endswith("StageBP1Evidence$Injected")
       or not isinstance(fact.get("writerConnectionId"),int)
       or not isinstance(fact.get("inFlightReaderConnectionId"),int)
       or fact["writerConnectionId"]<=0 or fact["inFlightReaderConnectionId"]<=0
       or fact["writerConnectionId"]==fact["inFlightReaderConnectionId"]
       or not digest("healthyGraphBeforeSha256") or not digest("healthyGraphAfterSha256")
       or not digest("protectedEffectSha256")
       or fact["healthyGraphBeforeSha256"]!=fact["healthyGraphAfterSha256"]
       or fact["candidateEffectSha256"]==fact["protectedEffectSha256"]
       or not isinstance(fact.get("postFreshReaderConnectionId"),int)
       or fact["postFreshReaderConnectionId"]<=0):
    fail("FAULT_"+name)
  elif name=="happy01":
   if (fact.get("successfulOriginalSqlWrites")!=6 or fact.get("sixAfter")!=ONE
       or fact.get("methodOutcome")!="ORIGINAL_NEW_SUCCESS"
       or fact.get("physicalTransactionResult")!="COMMITTED_OBSERVED"
       or not digest("sixTableGraphSha256")):fail("HAPPY")
  elif name=="replay01":
   if (fact.get("successfulOriginalSqlWrites")!=0 or fact.get("sixBefore")!=ONE
       or fact.get("sixAfter")!=ONE or fact.get("methodOutcome")!="ORIGINAL_REPLAY_TRUE"
       or fact.get("physicalTransactionResult")!="COMMITTED_OBSERVED"
       or not digest("sixTableGraphBeforeSha256")
       or not digest("sixTableGraphAfterSha256")
       or fact["sixTableGraphBeforeSha256"]!=fact["sixTableGraphAfterSha256"]):fail("REPLAY")
  entries[name]=fact
 ddl=[]
 for item in d["ddl"]:
  raw=(ROOT/item["path"]).read_bytes()
  blob=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
  if blob!=item["gitBlob"]:fail("DDL")
  ddl.append({"path":item["path"],"blob":blob,"sha256":hashlib.sha256(raw).hexdigest()})
 result={"schema":"STAGE_B_P1A_EVIDENCE_V1","sourceMainSha":d["sourceMainSha"],
         "actualPrHeadSha":head,"testedSyntheticMergeCheckoutSha":sha,
         "workflowRunId":os.environ["GITHUB_RUN_ID"],"cases":entries,"ddl":ddl,
         "tests":len(methods),"testOnly":True,"productionTopologyVerified":False,
         "downstreamAuthority":False,"realD5Ready":False}
 out=ROOT/"diagnosis-service/target/u07-d5o2-stage-b-p1a-evidence.json"
 out.write_text(json.dumps(result,indent=2,sort_keys=True)+"\n")
 print("STAGE_B_P1A_VERIFIED_CASES="+str(len(methods)))
if __name__=="__main__":main()
