"""Disposable CI bootstrap. Load exact original migrations; no production URL or network I/O."""
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parents[5]
manifest = json.loads(Path(__file__).with_name('schema-manifest.json').read_text())
chunks = []
for item in manifest['ddl']:
    data = (ROOT / item['path']).read_bytes()
    if hashlib.sha256(data).hexdigest() != item['sha256']:
        raise SystemExit('SOURCE_DDL_DIGEST_MISMATCH')
    blob = hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()
    if blob != item['git_blob']:
        raise SystemExit('SOURCE_DDL_BLOB_MISMATCH')
    chunks.append(data.decode())
assert manifest['source_head'] == '750770e81af79b764cc4148d7fa856363c89972a'
print('SET default_storage_engine=InnoDB;')
print('\n'.join(chunks))
print("CREATE USER 'o1_reader'@'%' IDENTIFIED BY 'synthetic-o1-reader';")
print("CREATE USER 'o1_writer'@'%' IDENTIFIED BY 'synthetic-o1-writer';")
for table in manifest['tables']:
    assert table.startswith('clinical_') and table.replace('_', '').isalnum()
    print(f"GRANT SELECT ON u07_owner_readback.{table} TO 'o1_reader'@'%';")
    print(f"GRANT SELECT,UPDATE ON u07_owner_readback.{table} TO 'o1_writer'@'%';")
