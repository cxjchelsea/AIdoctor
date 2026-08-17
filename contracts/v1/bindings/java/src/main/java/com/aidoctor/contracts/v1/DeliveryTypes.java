package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * REVIEWED_BINDING：PatientDeliveryView 结构白名单，不表示临床批准。
 */
public final class DeliveryTypes {
    private DeliveryTypes() {
    }

    public static class SafetyNotice {
        @JsonProperty("notice_id")
        public String noticeId;
        @JsonProperty("level_ref")
        public String levelRef;
        @JsonProperty("message")
        public String message;
    }

    public static class PatientSourceSummary {
        @JsonProperty("citation_id")
        public String citationId;
        @JsonProperty("source_id")
        public String sourceId;
        @JsonProperty("title")
        public String title;
        @JsonProperty("organization")
        public String organization;
        @JsonProperty("publication_date")
        public String publicationDate;
        @JsonProperty("source_version")
        public String sourceVersion;
        @JsonProperty("source_type")
        public String sourceType;
        @JsonProperty("applicable_population")
        public String applicablePopulation;
        @JsonProperty("region")
        public String region;
        @JsonProperty("patient_friendly_excerpt")
        public String patientFriendlyExcerpt;
        @JsonProperty("access_url")
        public String accessUrl;
        @JsonProperty("link_policy")
        public String linkPolicy;
        @JsonProperty("freshness_status")
        public String freshnessStatus;
    }

    public static class PatientConflictSummary {
        @JsonProperty("exists")
        public Boolean exists;
        @JsonProperty("patient_friendly_message")
        public String patientFriendlyMessage;
        @JsonProperty("affected_claim_ids")
        public List<String> affectedClaimIds;
        @JsonProperty("requires_clinician_review")
        public Boolean requiresClinicianReview;
    }

    public static class PatientApplicabilitySummary {
        @JsonProperty("population_match")
        public String populationMatch;
        @JsonProperty("region_match")
        public String regionMatch;
        @JsonProperty("patient_friendly_message")
        public String patientFriendlyMessage;
    }

    public static class PatientLimitation {
        @JsonProperty("code")
        public String code;
        @JsonProperty("message")
        public String message;
        @JsonProperty("severity")
        public String severity;
        @JsonProperty("user_action")
        public String userAction;
    }

    public static class EvidenceCard {
        @JsonProperty("card_id")
        public String cardId;
        @JsonProperty("claim_id")
        public String claimId;
        @JsonProperty("basis_type")
        public String basisType;
        @JsonProperty("patient_friendly_claim")
        public String patientFriendlyClaim;
        @JsonProperty("rationale_summary")
        public String rationaleSummary;
        @JsonProperty("evidence_status")
        public String evidenceStatus;
        @JsonProperty("certainty_label")
        public String certaintyLabel;
        @JsonProperty("related_patient_fact_ids")
        public List<String> relatedPatientFactIds;
        @JsonProperty("sources")
        public List<PatientSourceSummary> sources;
        @JsonProperty("conflict_summary")
        public PatientConflictSummary conflictSummary;
        @JsonProperty("applicability")
        public PatientApplicabilitySummary applicability;
        @JsonProperty("limitations")
        public List<PatientLimitation> limitations;
        @JsonProperty("clinician_reviewed")
        public Boolean clinicianReviewed;
        @JsonProperty("reviewed_at")
        public String reviewedAt;
        @JsonProperty("display_priority")
        public String displayPriority;
        @JsonProperty("expandable")
        public Boolean expandable;
    }

    public static class EvidenceSection {
        @JsonProperty("section_id")
        public String sectionId;
        @JsonProperty("title")
        public String title;
        @JsonProperty("description")
        public String description;
        @JsonProperty("display_order")
        public Integer displayOrder;
        @JsonProperty("cards")
        public List<EvidenceCard> cards;
    }

    public static class VersionBindings {
        @JsonProperty("delivery_version")
        public Integer deliveryVersion;
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("cdp_version")
        public Integer cdpVersion;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("review_decision_version")
        public Integer reviewDecisionVersion;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("knowledge_release_id")
        public String knowledgeReleaseId;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("knowledge_release_version")
        public String knowledgeReleaseVersion;
    }

    public static class PatientDeliveryView {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("delivery_id")
        public String deliveryId;
        @JsonProperty("delivery_version")
        public Integer deliveryVersion;
        @JsonProperty("delivery_status")
        public String deliveryStatus;
        @JsonProperty("cdp_id")
        public String cdpId;
        @JsonProperty("review_status")
        public String reviewStatus;
        @JsonProperty("generated_at")
        public String generatedAt;
        @JsonProperty("title")
        public String title;
        @JsonProperty("summary")
        public String summary;
        @JsonProperty("safety_notice")
        public List<SafetyNotice> safetyNotice;
        @JsonProperty("recommended_actions")
        public List<String> recommendedActions;
        @JsonProperty("evidence_sections")
        public List<EvidenceSection> evidenceSections;
        @JsonProperty("limitations")
        public List<PatientLimitation> limitations;
        @JsonProperty("follow_up")
        public List<String> followUp;
        @JsonProperty("version_bindings")
        public VersionBindings versionBindings;
    }
}
