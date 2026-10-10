"""Synthetic boundary architecture gate; no DB, source-only scope."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1] / "src/main/java"
u07 = root / "com/aidoctor/diagnosis/runtime/u07"
classes = ("U07D3SyntheticTransactionCoordinator", "U07D4SyntheticRecoveryInspector",
           "U07D4SyntheticOwnedTransactionRunner", "U07SyntheticInput", "U07SyntheticResults",
           "U07SyntheticTestTarget", "U07SyntheticInputValidator")
for path in root.rglob("*.java"):
    if u07 in path.parents:
        continue
    source = path.read_text()
    for name in classes:
        assert not re.search(r"\b" + name + r"\b", source), f"production reference: {path}: {name}"
for name in ("U07D3SyntheticTransactionCoordinator", "U07D4SyntheticRecoveryInspector",
             "U07EventApplicationRepository", "U07EffectOutboxRepository"):
    source = (u07 / (name + ".java")).read_text()
    assert not re.search(r"\.(?:commit|rollback|close|setAutoCommit)\s*\(", source.replace(
        "p.close(", "")), f"terminalization leaked to {name}"
for name in ("U07D3SyntheticTransactionCoordinator", "U07D4SyntheticRecoveryInspector"):
    source = (u07 / (name + ".java")).read_text()
    assert not re.search(r"public\s+(?:final\s+)?class\s+" + name, source), name
    assert not re.search(r"public[^;{}]*\(Connection\s", source), name
inspector = (u07 / "U07D4SyntheticRecoveryInspector.java").read_text()
assert inspector.count(".prepareStatement(") == 1
assert "FOR UPDATE" not in inspector
assert "LEFT JOIN u07_event_application" in inspector
print("U07_D5P_ARCHITECTURE=PASS public_entry=owned_only production_refs=0 recovery_selects=1")
