#!/usr/bin/env python3
"""Emit pinned nonproduction U06 owner schema slice and least privilege roles."""
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
MANIFEST=ROOT/"diagnosis-service/src/test/resources/u07/d5o2/stageb/p0-manifest.json"
BLOBS=("73029495c5f46e8576e34f8c9a77644fb8c1c4c4","ccc1aa30b8e9405bd07249bae1d3531853f7510b","7ed5a82bfba3b3fed2666bee969a7fe276e67566")
TABLES=("clinical_consultation","clinical_consultation_wait_effect","clinical_runtime_thread_state","clinical_runtime_wait_checkpoint","u06_delivery_authority","u06_delivery_intent","u06_delivery_attempt","u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger")
def main():
 data=json.loads(MANIFEST.read_text())
 if (data["sourceMainSha"]!="2834714354b02a88ccf73773511af706554cd9fb"
     or data["database"]!="u07_d5o2_stage_b_p0" or data["port"]!=33327
     or data["authority"]!="UNKNOWN" or data["productionTopologyVerified"] is not False
     or data["mode"]!="ISOLATED_TEST_ONLY" or data["realDeliveryAllowed"] is not False
     or data["f8ResumeAllowed"] is not False or len(data["ddl"])!=3):
  raise SystemExit("STAGE_B_P0_MANIFEST_DENIED")
 out=["SET default_storage_engine=InnoDB;"]
 for item,blob in zip(data["ddl"],BLOBS):
  if item["gitBlob"]!=blob or not item["path"].startswith("diagnosis-service/src/main/resources/db/migration/V"):
   raise SystemExit("STAGE_B_P0_DDL_MANIFEST_DENIED")
  path=(ROOT/item["path"]).resolve()
  if not path.is_relative_to(ROOT):raise SystemExit("STAGE_B_P0_PATH_DENIED")
  raw=path.read_bytes()
  actual=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\\0"+raw).hexdigest()
  if actual!=blob:raise SystemExit("STAGE_B_P0_DDL_CHANGED")
  out.append(raw.decode())
 out+=["CREATE USER 'stageb_writer'@'%' IDENTIFIED BY 'synthetic-stageb-writer';",
       "CREATE USER 'stageb_reader'@'%' IDENTIFIED BY 'synthetic-stageb-reader';"]
 for table in TABLES:
  out.append("GRANT SELECT, INSERT, UPDATE, DELETE ON u07_d5o2_stage_b_p0."+table+" TO 'stageb_writer'@'%';")
  out.append("GRANT SELECT ON u07_d5o2_stage_b_p0."+table+" TO 'stageb_reader'@'%';")
 print("\n".join(out))
if __name__=="__main__":main()
