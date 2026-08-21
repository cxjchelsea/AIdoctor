package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.SharedContractsMapper;
import com.aidoctor.contracts.v1.StateTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical logical fingerprint of a {@link StateTypes.StatePatch}.
 * Routing identifiers such as {@code patch_id} and envelope message ids
 * are excluded so the same logical request can replay.
 */
public final class CanonicalPatchFingerprint {
    private static final ObjectMapper MAPPER = SharedContractsMapper.create();

    private CanonicalPatchFingerprint() {
    }

    public static String fingerprint(StateTypes.StatePatch patch) {
        Map<String, Object> canonical = new LinkedHashMap<String, Object>();
        canonical.put("contract_version", patch.contractVersion);
        canonical.put("cdp_id", patch.cdpId);
        canonical.put("base_version", patch.baseVersion);
        canonical.put("reason_code", patch.reasonCode);
        canonical.put("producer", patch.producer);
        if (patch.envelope != null) {
            canonical.put("capability_id", patch.envelope.capabilityId);
            canonical.put("capability_version", patch.envelope.capabilityVersion);
        }
        canonical.put(
                "evidence_refs",
                patch.evidenceRefs == null ? Collections.emptyList() : patch.evidenceRefs
        );
        List<Map<String, Object>> operations = new ArrayList<Map<String, Object>>();
        if (patch.operations != null) {
            for (int index = 0; index < patch.operations.size(); index++) {
                StateTypes.StatePatchOperation operation = patch.operations.get(index);
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("op", operation.op);
                item.put("path", operation.path);
                item.put("value", operation.value);
                item.put("expected_current_value", operation.expectedCurrentValue);
                item.put("source", operation.source);
                item.put("sensitivity", operation.sensitivity);
                operations.add(item);
            }
        }
        canonical.put("operations", operations);
        try {
            return MAPPER.writeValueAsString(canonical);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("unable to canonicalize StatePatch", exception);
        }
    }
}
