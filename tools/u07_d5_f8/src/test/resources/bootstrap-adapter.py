"""Render only the fixed disposable adapter schema grant mapping; no arbitrary DB input."""
from pathlib import Path
import hashlib
source = Path('tools/u07_d5_source_binding/src/test/resources/schema-mysql.sql').read_bytes()
print('-- SOURCE_SCHEMA_SHA256=' + hashlib.sha256(source).hexdigest())
print(source.decode().replace('u07_source_binding.', 'u07_f8_adapter.').replace("CREATE USER 'synthetic_issuer'", "CREATE USER IF NOT EXISTS 'synthetic_issuer'").replace("CREATE USER 'synthetic_adapter'", "CREATE USER IF NOT EXISTS 'synthetic_adapter'"))
