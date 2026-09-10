// 结构检查：不声称 RUNTIME_VERIFIED。语义 oracle 仍是 Python validator。
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const contractsV1 = path.resolve(here, "../../..");
const manifest = JSON.parse(fs.readFileSync(path.join(contractsV1, "manifest.json"), "utf8"));
const versionFile = fs.readFileSync(path.join(here, "../src/version.ts"), "utf8");

const SCHEMA_NAMES = [
  "ContractEnvelope",
  "IdentifierSet",
  "StatePatch",
  "CommitResult",
  "ContractConflict",
  "ToolContext",
  "ToolResult",
  "EvidencePack",
  "SourceArtifact",
  "KnowledgeReleaseRef",
  "TraceRef",
  "AuditRef",
  "PatientDeliveryView",
];

assert.equal(manifest.contract_version, "1.0.0");
assert.equal(manifest.version_negotiation, "EXACT");
assert.equal(manifest.contracts.length, 13);
assert.deepEqual(manifest.contracts.map((item) => item.name), SCHEMA_NAMES);
assert.match(versionFile, /export const CONTRACT_VERSION = "1\.0\.0"/);

const fixtures = {};
for (const fileName of fs.readdirSync(path.join(contractsV1, "fixtures/valid"))) {
  if (!fileName.endsWith(".json")) {
    continue;
  }
  Object.assign(fixtures, JSON.parse(fs.readFileSync(path.join(contractsV1, "fixtures/valid", fileName), "utf8")));
}

function requireExactVersion(contractVersion) {
  if (contractVersion !== "1.0.0") {
    throw new Error("unknown contract_version rejected: " + contractVersion);
  }
}

for (const schemaName of SCHEMA_NAMES) {
  const payload = fixtures[schemaName];
  assert.ok(payload, "missing fixture " + schemaName);
  requireExactVersion(payload.contract_version);
}

assert.equal(fixtures.StatePatch.envelope.contract_name, "StatePatch");
assert.equal(fixtures.CommitResult.envelope.contract_name, "CommitResult");
assert.ok(!Object.prototype.hasOwnProperty.call(fixtures.StatePatch, "status"));
assert.equal(fixtures.ToolResult.status, "SUCCEEDED");
assert.ok(!Object.prototype.hasOwnProperty.call(fixtures.ToolResult, "suggested_writes"));
assert.equal(fixtures.ToolResult.suggested_patches[0].envelope.contract_name, "StatePatch");

assert.throws(() => requireExactVersion("9.9.9"), /unknown contract_version/);

console.log("typescript binding structural checks passed");
