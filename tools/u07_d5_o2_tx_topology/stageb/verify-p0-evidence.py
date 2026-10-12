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
 if len(required)!=len(d["expectedTestMethods"]) or set(d["expectedCaseIds"])!=required:
  raise SystemExit("P0_DUPLICATE_OR_MISSING_MANIFEST_CASES")
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
 observation_dir=ROOT/"diagnosis-service/target/stage-b-p0-observations"
 observations={}
 if not observation_dir.is_dir():raise SystemExit("P0_OBSERVATIONS_MISSING")
 found_paths=list(observation_dir.glob("*.json"))
 if len(found_paths)!=len(required):raise SystemExit("P0_OBSERVATION_COUNT_MISMATCH")
 for method in sorted(required):
  path=observation_dir/(method+".json")
  if not path.is_file():raise SystemExit("P0_METHOD_FACTS_MISSING_"+method)
  fact=json.loads(path.read_text(encoding="utf-8"))
  if fact.get("testMethod")!=method or fact.get("caseId")!=d["expectedCaseIds"][method]:
   raise SystemExit("P0_CASE_FACT_MISMATCH_"+method)
  if fact.get("evidencePlane")!="TEST_ONLY_REAL_MYSQL8" or fact.get("productionTopologyVerified") is not False or fact.get("guardEnforced") is not True:
   raise SystemExit("P0_AUTHORITY_OR_GUARD_FACT_MISSING_"+method)
  if method=="p0OwnerIdentityJdbcAndJpaEffectiveIsolation":
   if (fact.get("jdbcIsolation")!=8 or fact.get("jpaIsolation")!=8 or
       fact.get("sequentialOrdersVerified")!=2 or fact.get("managerResourceIdentityVerified") is not True or
       not isinstance(fact.get("jdbcWriterConnectionId"),int) or fact["jdbcWriterConnectionId"]<=0 or
       not isinstance(fact.get("jpaWriterConnectionId"),int) or fact["jpaWriterConnectionId"]<=0):
    raise SystemExit("P0_MANAGER_PHYSICAL_FACTS_MISSING")
  elif method=="p0NestedManagerAndResidualResourcesFailClosed":
   if fact.get("nestedRejectedDirections")!=2:raise SystemExit("P0_NESTED_NEGATIVE_UNVERIFIED")
  elif method=="p0WrongManagerDifferentDatasourceCannotBindExpectedResource":
   if fact.get("wrongManagerDeniedBeforeCallback") is not True:raise SystemExit("P0_MANAGER_GUARD_UNVERIFIED")
  elif method=="p0PostWriteHookIsRealAndRollsBack":
   if (fact.get("faultOrdinal")!=1 or fact.get("successfulPhysicalSqlWrites")!=1 or
       fact.get("baselineCount")!=0 or fact.get("uncommittedReaderCount")!=0 or
       fact.get("postRollbackSumSixTables")!=0 or fact.get("readOnlyDeleteDenied") is not True or
       fact.get("writerTxActiveAtFault") is not True or
       not isinstance(fact.get("writerConnectionId"),int) or
       not isinstance(fact.get("readerConnectionId"),int) or
       fact["writerConnectionId"]<=0 or fact["readerConnectionId"]<=0 or
       fact["writerConnectionId"]==fact["readerConnectionId"]):
    raise SystemExit("P0_ROLLBACK_PHYSICAL_FACTS_MISSING")
  elif method=="p0UnexpectedDeliverySqlFailsBeforeMutation":
   if (fact.get("unexpectedDeliveryInsertDenied") is not True or
       fact.get("unexpectedDmlDenied") is not True or fact.get("successfulPhysicalSqlWrites")!=0):
    raise SystemExit("P0_SQL_NEGATIVE_UNVERIFIED")
  observations[method]=fact
 ddl=[]
 for entry in d["ddl"]:
  raw=(ROOT/entry["path"]).read_bytes()
  digest=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
  if digest!=entry["gitBlob"]:raise SystemExit("P0_DDL_MISMATCH")
  ddl.append({"path":entry["path"],"blob":digest,"sha256":hashlib.sha256(raw).hexdigest()})
 result={"schema":"STAGE_B_P0_ISOLATED_EVIDENCE_V1","sourceMainSha":d["sourceMainSha"],
  "testedCheckoutSha":sha,"workflowRunId":os.environ["GITHUB_RUN_ID"],
  "testCases":sorted(seen),"tests":count,"observations":observations,"ddl":ddl,"authority":"UNKNOWN",
  "evidencePlane":"TEST_ONLY_REAL_MYSQL8","productionTopologyVerified":False,
  "realDeliveryAuthorized":False,"f8ResumeAuthorized":False,
  "scope":"P0_DUAL_MANAGER_AND_ONE_SQL_FAULT_ONLY"}
 output=ROOT/"diagnosis-service/target/u07-d5o2-stage-b-p0-evidence.json"
 output.write_text(json.dumps(result,indent=2,sort_keys=True)+"\n")
 print("STAGE_B_P0_TESTS_EXECUTED="+str(count))
if __name__=="__main__":main()
