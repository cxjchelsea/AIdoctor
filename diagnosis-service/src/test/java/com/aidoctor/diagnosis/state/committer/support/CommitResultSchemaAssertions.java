package com.aidoctor.diagnosis.state.committer.support;

import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.SharedContractsMapper;
import com.aidoctor.contracts.v1.StateTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mirrors frozen CommitResult schema constraints without reimplementing
 * the canonical Python JSON Schema validator.
 */
public final class CommitResultSchemaAssertions {
    private static final ObjectMapper MAPPER = SharedContractsMapper.create();

    private CommitResultSchemaAssertions() {
    }

    public static JsonNode assertValid(StateTypes.CommitResult result) {
        assertNotNull(result);
        assertEquals("1.0.0", result.contractVersion);
        assertNotNull(result.envelope);
        assertEquals("CommitResult", result.envelope.contractName);
        assertEquals("1.0.0", result.envelope.contractVersion);
        assertNotNull(result.patchId);
        assertNotNull(result.cdpId);
        assertNotNull(result.status);
        assertNotNull(result.previousVersion);
        assertNotNull(result.reasonCode);
        assertTrue(result.reasonCode.matches("^[A-Z][A-Z0-9_]*$"));
        assertNotNull(result.conflicts);
        assertNotNull(result.rejectedOperations);
        assertNotNull(result.errors);
        assertNotNull(result.retryable);
        assertValidAuditRef(result.auditRef);

        if ("COMMITTED".equals(result.status)) {
            assertNotNull(result.committedVersion);
            assertNotNull(result.committedAt);
            assertEquals(0, result.conflicts.size());
            assertEquals(0, result.rejectedOperations.size());
            assertEquals(0, result.errors.size());
            assertEquals(Boolean.FALSE, result.retryable);
        } else {
            assertEquals(null, result.committedVersion);
            assertEquals(null, result.committedAt);
        }

        if ("REJECTED".equals(result.status)) {
            assertEquals(0, result.conflicts.size());
            assertTrue(result.rejectedOperations.size() >= 1);
            assertEquals(0, result.errors.size());
            assertEquals(Boolean.FALSE, result.retryable);
        }
        if ("CONFLICT".equals(result.status)) {
            assertTrue(result.conflicts.size() >= 1);
            assertEquals(0, result.rejectedOperations.size());
            assertEquals(0, result.errors.size());
            assertEquals(Boolean.TRUE, result.retryable);
            FoundationTypes.ContractConflict conflict = result.conflicts.get(0);
            assertNotNull(conflict.type);
            assertNotNull(conflict.conflictId);
            assertNotNull(conflict.path);
        }
        if ("FAILED".equals(result.status)) {
            assertEquals(0, result.conflicts.size());
            assertEquals(0, result.rejectedOperations.size());
            assertTrue(result.errors.size() >= 1);
            assertNotNull(result.errors.get(0).code);
            assertNotNull(result.errors.get(0).message);
        }

        JsonNode node = MAPPER.valueToTree(result);
        assertEquals(result.status, node.get("status").asText());
        if (!"COMMITTED".equals(result.status)) {
            assertFalse(node.has("committed_version"));
            assertFalse(node.has("committed_at"));
        }
        return node;
    }

    public static void assertValidAuditRef(FoundationTypes.AuditRef auditRef) {
        assertNotNull(auditRef);
        assertEquals("1.0.0", auditRef.contractVersion);
        assertNotNull(auditRef.auditId);
        assertTrue(auditRef.auditId.matches("^[A-Za-z0-9][A-Za-z0-9._:-]*$"));
        assertTrue("STATE_COMMITTED".equals(auditRef.auditType) || "STATE_PATCH_REQUESTED".equals(auditRef.auditType));
        assertNotNull(auditRef.auditVersion);
        assertNotNull(auditRef.createdAt);
        assertNotNull(auditRef.accessLevel);
        assertEquals(Boolean.FALSE, auditRef.phiCapable);
    }
}
