package com.aidoctor.contracts.v1;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SchemaCatalog {
    public static final String[] SCHEMA_NAMES = {
        "ContractEnvelope", "IdentifierSet", "StatePatch", "CommitResult", "ContractConflict",
        "ToolContext", "ToolResult", "EvidencePack", "SourceArtifact", "KnowledgeReleaseRef",
        "TraceRef", "AuditRef", "PatientDeliveryView", "Encounter", "ClinicalStateSnapshot", "ClinicalObservation"
    };
    private static final Map<String, Class<?>> TYPES;
    static {
        Map<String, Class<?>> types = new LinkedHashMap<String, Class<?>>();
        types.put("ContractEnvelope", FoundationTypes.ContractEnvelope.class);
        types.put("IdentifierSet", FoundationTypes.IdentifierSet.class);
        types.put("StatePatch", StateTypes.StatePatch.class);
        types.put("CommitResult", StateTypes.CommitResult.class);
        types.put("ContractConflict", FoundationTypes.ContractConflict.class);
        types.put("ToolContext", ToolTypes.ToolContext.class);
        types.put("ToolResult", ToolTypes.ToolResult.class);
        types.put("EvidencePack", EvidenceTypes.EvidencePack.class);
        types.put("SourceArtifact", EvidenceTypes.SourceArtifact.class);
        types.put("KnowledgeReleaseRef", EvidenceTypes.KnowledgeReleaseRef.class);
        types.put("TraceRef", FoundationTypes.TraceRef.class);
        types.put("AuditRef", FoundationTypes.AuditRef.class);
        types.put("PatientDeliveryView", DeliveryTypes.PatientDeliveryView.class);
        types.put("Encounter", ClinicalTypes.Encounter.class);
        types.put("ClinicalStateSnapshot", ClinicalTypes.ClinicalStateSnapshot.class);
        types.put("ClinicalObservation", ClinicalTypes.ClinicalObservation.class);
        TYPES = Collections.unmodifiableMap(types);
    }
    private SchemaCatalog() { }
    public static Class<?> typeFor(String schemaName) {
        Class<?> type = TYPES.get(schemaName);
        if (type == null) throw new IllegalArgumentException("unknown Shared Contract schema: " + schemaName);
        return type;
    }
}
