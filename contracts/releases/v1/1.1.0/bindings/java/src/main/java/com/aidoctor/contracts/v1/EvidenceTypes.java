package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * REVIEWED_BINDING锛欵videncePack / SourceArtifact / KnowledgeReleaseRef銆?
 */
public final class EvidenceTypes {
    private EvidenceTypes() {
    }

    public static class EvidenceClaim {
        @JsonProperty("claim_id")
        public String claimId;
        @JsonProperty("text")
        public String text;
        @JsonProperty("claim_type")
        public String claimType;
        @JsonProperty("support_status")
        public String supportStatus;
        @JsonProperty("citation_refs")
        public List<String> citationRefs;
        @JsonProperty("applicability")
        public String applicability;
        @JsonProperty("limitations")
        public List<String> limitations;
    }

    public static class EvidenceSourceSpan {
        @JsonProperty("source_id")
        public String sourceId;
        @JsonProperty("source_type")
        public String sourceType;
        @JsonProperty("title")
        public String title;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("publisher")
        public String publisher;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("release_or_version")
        public String releaseOrVersion;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("jurisdiction")
        public String jurisdiction;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("valid_from")
        public String validFrom;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("valid_to")
        public String validTo;
        @JsonProperty("locator")
        public String locator;
        @JsonProperty("span")
        public String span;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("license")
        public String license;
        @JsonProperty("retrieved_at")
        public String retrievedAt;
    }

    public static class EvidenceConflict {
        @JsonProperty("conflict_id")
        public String conflictId;
        @JsonProperty("claim_refs")
        public List<String> claimRefs;
        @JsonProperty("source_refs")
        public List<String> sourceRefs;
        @JsonProperty("summary")
        public String summary;
    }

    public static class EvidencePack {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("evidence_pack_id")
        public String evidencePackId;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("knowledge_release_id")
        public String knowledgeReleaseId;
        @JsonProperty("created_at")
        public String createdAt;
        @JsonProperty("claims")
        public List<EvidenceClaim> claims;
        @JsonProperty("sources")
        public List<EvidenceSourceSpan> sources;
        @JsonProperty("conflicts")
        public List<EvidenceConflict> conflicts;
        @JsonProperty("limitations")
        public List<String> limitations;
    }

    public static class ArtifactChecksum {
        @JsonProperty("algorithm")
        public String algorithm;
        @JsonProperty("value")
        public String value;
    }

    public static class SourceArtifact {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("artifact_id")
        public String artifactId;
        @JsonProperty("artifact_type")
        public String artifactType;
        @JsonProperty("owner_ref")
        public String ownerRef;
        @JsonProperty("content_type")
        public String contentType;
        @JsonProperty("original_filename")
        public String originalFilename;
        @JsonProperty("size_bytes")
        public Integer sizeBytes;
        @JsonProperty("checksum")
        public ArtifactChecksum checksum;
        @JsonProperty("storage_ref")
        public String storageRef;
        @JsonProperty("created_at")
        public String createdAt;
        @JsonProperty("processing_status")
        public String processingStatus;
        @JsonProperty("derived_artifacts")
        public List<String> derivedArtifacts;
        @JsonProperty("source_artifact_id")
        public String sourceArtifactId;
        @JsonProperty("sensitivity")
        public String sensitivity;
        @JsonProperty("retention_class")
        public String retentionClass;
    }

    public static class KnowledgeReleaseRef {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("knowledge_release_id")
        public String knowledgeReleaseId;
        @JsonProperty("source_registry_version")
        public String sourceRegistryVersion;
        @JsonProperty("released_at")
        public String releasedAt;
        @JsonProperty("status")
        public String status;
        @JsonProperty("source_refs")
        public List<String> sourceRefs;
        @JsonProperty("checksum_manifest_ref")
        public String checksumManifestRef;
        @JsonProperty("withdrawn_at")
        public String withdrawnAt;
        @JsonProperty("superseded_by")
        public String supersededBy;
    }
}
