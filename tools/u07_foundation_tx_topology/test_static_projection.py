"""Synthetic-only unittest suite; no service, context, network or DB."""
from __future__ import annotations

import io
import json
import hashlib
import tempfile
import unittest
import unittest.mock
import zipfile
from pathlib import Path

import static_projection as producer
from static_schema import EvidenceError, canonical_json, digest, make_result, verify_inventory


def fixture():
    examples = {
        "U01ConsultationService": "@Transactional\npublic U01Result start(U01StartCommand x) {\n eventLedger.resolveOrCreate(\n cdpManager.createCDP(\n runtimeBindingService.bind(\n consultationRepository.save(\n runCoordinator.openRun(\n}\n",
        "CanonicalBusinessEventLedger": "@Transactional\npublic CanonicalBusinessEventRecord resolveOrCreate(String key) {return null;}\n",
        "CDPManager": "@Transactional\npublic CDP createCDP(String id) {cdpVersionService.createInitialVersion(\n return null;}\n",
        "CDPVersionService": "@Transactional\npublic CDPVersion createInitialVersion(CDP d) {return null;}\n",
        "RuntimeBindingService": "@Transactional\npublic RuntimeBindingRecord bind(String id) {return null;}\n",
        "ClinicalRunCoordinator": "@Transactional\npublic ClinicalRunRecord openRun(String id) {return null;}\n",
    }
    snapshots = {producer.ROOT + producer.FILES[k]: v.encode() for k, v in examples.items()}
    files = [{"path": p, "sha256": digest(b), "mode": "UTF8_TEXT_SCAN", "bytes": len(b)} for p, b in snapshots.items()]
    for i in range(2106):
        files.append({"path": "fixtures/f%04d.txt" % i, "sha256": digest(str(i).encode()), "mode": "UTF8_TEXT_SCAN", "bytes": 1})
    for i in range(49):
        files[6 + i]["mode"] = "BINARY_READ_NOT_TEXT"
    matches = []
    for i in range(256):
        p = list(snapshots)[i % 6]
        b = snapshots[p]
        blob = hashlib.sha1(b"blob " + str(len(b)).encode() + b"\0" + b).hexdigest()
        matches.append({"path": p, "line": 1, "term": "transaction_annotation", "file_sha256": digest(b), "file_git_blob": blob})
    inventory = {"source_head": "6d4fd787600e3a57f01f3e17893e6d98893ac546",
                 "tree_sha": "1bfe776f76c986d4e6199a9b820f2cc20773a181",
                 "tracked_count": 2112, "text_scanned": 2063, "binary_read_and_classified": 49,
                 "skipped_tracked_paths": 0, "file_inventory": files, "matches": matches}
    raw = json.dumps(inventory).encode()
    buff = io.BytesIO()
    with zipfile.ZipFile(buff, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("foundation-exact-head-inventory.json", raw)
        z.writestr("foundation-matches.tsv", "synthetic\n")
        z.writestr("foundation-summary.md", "synthetic\n")
    return buff.getvalue(), snapshots, inventory


class StaticProducerTests(unittest.TestCase):
    def setUp(self):
        self.archive, self.snapshots, self.inventory = fixture()

    def test_success_is_source_only_with_complete_transitive_graph(self):
        r = producer.project(self.archive, self.snapshots)
        self.assertEqual(r["status"], "SOURCE_ONLY")
        self.assertEqual(r["effective_manager"], "UNKNOWN")
        self.assertFalse(r["spring_context_executed"])
        self.assertIn(("CDPManager", "CDPVersionService"), {(x["from"], x["to"]) for x in r["edges"]})
        self.assertIn(("U01ConsultationService", "ClinicalRunCoordinator"), {(x["from"], x["to"]) for x in r["edges"]})
        self.assertEqual(len(r["edges"]), 6)
        self.assertTrue(all(x["declared_transactional"] for x in r["nodes"] if x.get("method") and x["name"] != "ConsultationRepository"))
        self.assertEqual([x["status"] for x in r["nodes"] if x["name"].startswith("U07")], ["NOT_IMPLEMENTED", "NOT_IMPLEMENTED"])

    def test_deterministic_json_digest(self):
        a = canonical_json(producer.project(self.archive, self.snapshots))
        b = canonical_json(producer.project(self.archive, dict(reversed(list(self.snapshots.items())))))
        self.assertEqual(a, b)
        self.assertEqual(digest(a), digest(b))

    def test_head_or_tree_mismatch_fails(self):
        for k in ("source_head", "tree_sha"):
            x = dict(self.inventory)
            x[k] = "0" * 40
            with self.assertRaises(EvidenceError):
                verify_inventory(x)

    def test_coverage_and_missing_path_fail(self):
        for k, value in (("skipped_tracked_paths", 1), ("text_scanned", 2062)):
            x = dict(self.inventory)
            x[k] = value
            with self.assertRaises(EvidenceError):
                verify_inventory(x)
        snapshots = dict(self.snapshots)
        snapshots.pop(next(iter(snapshots)))
        with self.assertRaises(EvidenceError):
            producer.project(self.archive, snapshots)

    def test_tampered_source_and_spoofed_blob_fail(self):
        snapshots = dict(self.snapshots)
        p = next(iter(snapshots))
        snapshots[p] += b"// tamper"
        with self.assertRaises(EvidenceError):
            producer.project(self.archive, snapshots)
        inventory = dict(self.inventory)
        inventory["matches"] = [dict(x) for x in inventory["matches"]]
        inventory["matches"][0]["file_git_blob"] = "0" * 40
        with self.assertRaises(EvidenceError):
            producer.project(self._rearchive(inventory), self.snapshots)

    def test_missing_or_duplicate_edge_fails_closed(self):
        p = producer.ROOT + producer.FILES["CDPManager"]
        for change in (lambda s: s.replace("cdpVersionService.createInitialVersion(", "noop("),
                       lambda s: s.replace("cdpVersionService.createInitialVersion(", "cdpVersionService.createInitialVersion( cdpVersionService.createInitialVersion(")):
            snapshots = dict(self.snapshots)
            snapshots[p] = change(snapshots[p].decode()).encode()
            with self.assertRaises(EvidenceError):
                producer.project(self.archive, snapshots)

    def test_fake_manager_positive_authority_rejected(self):
        with self.assertRaises(EvidenceError):
            make_result(archive_sha256="a" * 64, inventory_sha256="b" * 64,
                        nodes=[{"status": "CONTEXT_CONFIRMED"}], edges=[], sources=[])
        with self.assertRaises(EvidenceError):
            canonical_json({"configured_same_manager": True})
        with self.assertRaises(EvidenceError):
            canonical_json({"documentation": "jdbc:mysql://secret-host/mydb"})

    def test_no_network_or_subprocess_calls(self):
        with unittest.mock.patch("socket.socket", side_effect=AssertionError("network forbidden")), unittest.mock.patch("subprocess.run", side_effect=AssertionError("process forbidden")):
            producer.project(self.archive, self.snapshots)

    def test_output_path_cannot_overwrite_existing(self):
        with tempfile.TemporaryDirectory() as temp:
            p = Path(temp) / "out.json"
            p.write_text("existing")
            with unittest.mock.patch("sys.argv", ["producer", "--archive", "missing.zip", "--snapshots-dir", temp, "--output", str(p)]):
                with self.assertRaises(SystemExit):
                    producer.main()
            self.assertEqual(p.read_text(), "existing")

    @staticmethod
    def _rearchive(inventory):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as z:
            z.writestr("foundation-exact-head-inventory.json", json.dumps(inventory))
            z.writestr("foundation-matches.tsv", "synthetic\n")
            z.writestr("foundation-summary.md", "synthetic\n")
        return buf.getvalue()


if __name__ == "__main__":
    unittest.main()
