#!/usr/bin/env python3
"""Generate pinned isolated P1-A MySQL schema SQL, no DB connection."""
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[4]
MANIFEST=ROOT/"diagnosis-service/src/test/resources/u07/d5o2/stageb/p1/manifest.json"
BLOBS=("73029495c5f46e8576e34f8c9a77644fb8c1c4c4",
       "ccc1aa30b8e9405bd07249bae1d3531853f7510b",
       "7ed5a82bfba3b3fed2666bee969a7fe276e67566")
PATHS=tuple("diagnosis-service/src/main/resources/db/migration/"+x for x in (
  "V2__create_clinical_runtime_foundation.sql","V3__create_clinical_consultation.sql",
  "V6__add_u06_wait_runtime.sql"))
TABLES=("u06_delivery_authority","u06_delivery_intent","u06_delivery_attempt",
        "u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger")
def main():
 d=json.loads(MANIFEST.read_text())
 if (d["sourceMainSha"]!="3b0589347d90af5ec5b86fbdad5eecabcce41a24"
     or d["database"]!="u07_d5o2_stage_b_p1" or d["port"]!=33328
     or d["profile"]!="U07_D5O2_STAGE_B_P1_SYNTHETIC"
     or d["mode"]!="ISOLATED_TEST_ONLY" or d["authority"]!="UNKNOWN"
     or d["productionTopologyVerified"] is not False
     or d["realDeliveryAllowed"] is not False or d["f8ResumeAllowed"] is not False
     or len(d["ddl"])!=3):
  raise SystemExit("P1A_MANIFEST_DENIED")
 chunks=["SET default_storage_engine=InnoDB;"]
 for item,path,blob in zip(d["ddl"],PATHS,BLOBS):
  if item!={"path":path,"gitBlob":blob}:raise SystemExit("P1A_DDL_NOT_PINNED")
  file=(ROOT/path).resolve()
  if not file.is_relative_to(ROOT):raise SystemExit("P1A_OUTSIDE_REPO")
  raw=file.read_bytes()
  digest=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
  if digest!=blob:raise SystemExit("P1A_DDL_CHANGED")
  chunks.append(raw.decode())
 chunks+=["CREATE USER 'stagebp1_writer'@'%' IDENTIFIED BY 'synthetic-stagebp1-writer';",
          "CREATE USER 'stagebp1_reader'@'%' IDENTIFIED BY 'synthetic-stagebp1-reader';",
          "CREATE USER 'stagebp1_seeder'@'%' IDENTIFIED BY 'synthetic-stagebp1-seeder';"]
 for table in TABLES:
  chunks.append("GRANT SELECT, INSERT, UPDATE, DELETE ON u07_d5o2_stage_b_p1."+table+" TO 'stagebp1_writer'@'%';")
  chunks.append("GRANT SELECT ON u07_d5o2_stage_b_p1."+table+" TO 'stagebp1_reader'@'%';")
  chunks.append("GRANT SELECT, INSERT, UPDATE, DELETE ON u07_d5o2_stage_b_p1."+table+" TO 'stagebp1_seeder'@'%';")
 print("\n".join(chunks))
if __name__=="__main__":main()
