package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Nonproduction, read-only question/WAIT proof adapter.
 * Does not install a delivery verifier, authorized question owner, or
 * first-round clinical host; those must come independently from the test host.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="first-round-wait-http-enabled",havingValue="true",matchIfMissing=false)
public class V1NonProductionDeliveredQuestionReadbackHostConfig {
    @Bean
    @ConditionalOnMissingBean(V1NonProductionDeliveredQuestionReadback.class)
    @ConditionalOnBean({RuntimeWaitCheckpointRepository.class, CDPRepository.class})
    public V1NonProductionDeliveredQuestionReadback v1DeliveredQuestionReadback(
            RuntimeWaitCheckpointRepository checkpoints, CDPRepository cdps) {
        return new V1NonProductionDeliveredQuestionReadback(checkpoints, cdps);
    }
}
