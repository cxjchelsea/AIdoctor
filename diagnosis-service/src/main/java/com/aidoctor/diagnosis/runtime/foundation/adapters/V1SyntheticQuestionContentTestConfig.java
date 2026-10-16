package com.aidoctor.diagnosis.runtime.foundation.adapters;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Narrow fixture-only content owner for synthetic local engineering tests.
 * Not an evidence authority, question selector, delivery receipt or clinical policy.
 * The content reference must be an exact fixture, never user-supplied prose.
 */
@Configuration
@Profile("clinical-e2e-test")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="synthetic-question-content-enabled",havingValue="true",matchIfMissing=false)
public class V1SyntheticQuestionContentTestConfig {
    @Bean
    @ConditionalOnMissingBean(V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent.class)
    public V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent syntheticQuestionContent() {
        return (consultationId,runId,questionId,contentRef) -> {
            if(consultationId==null || consultationId.trim().isEmpty()
                    || runId==null || runId.trim().isEmpty()
                    || questionId==null || !questionId.startsWith("test-u06-question-")
                    || !"synthetic follow-up question".equals(contentRef))
                throw new IllegalStateException("V1_SYNTHETIC_QUESTION_NOT_ALLOWLISTED");
            return "【仅合成测试】请补充描述症状是否变化。";
        };
    }
}
