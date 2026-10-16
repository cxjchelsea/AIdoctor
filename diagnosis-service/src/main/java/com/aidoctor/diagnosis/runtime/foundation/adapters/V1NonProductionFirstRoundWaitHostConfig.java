package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService;
import com.aidoctor.diagnosis.runtime.u06.U05QuestionRouteAdmissionAdapter;
import com.aidoctor.diagnosis.runtime.u06.U06NonProductionPhysicalMode23Service;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Assemble the existing U05/U06/WAIT coordinator under the same nonproduction
 * flag as the HTTP first-round endpoint. Every clinical and durable authority
 * is supplied independently; no synthetic delivery evidence is issued here.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "first-round-wait-http-enabled", havingValue = "true", matchIfMissing = false)
public class V1NonProductionFirstRoundWaitHostConfig {
    @Bean
    @ConditionalOnMissingBean(U07NonProductionPhysicalFollowupCoordinator.class)
    @ConditionalOnBean({
            U07NonProductionF3A1U05Flow.class,
            U05QuestionRouteAdmissionAdapter.class,
            U06NonProductionPhysicalMode23Service.class,
            U07NonProductionDeliveryWaitRecovery.class,
            CDPRepository.class, RuntimeBindingService.class,
            ClinicalRunRepository.class, TransactionTemplate.class,
            U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost.class
    })
    public U07NonProductionPhysicalFollowupCoordinator v1HttpFirstRoundCoordinator(
            U07NonProductionF3A1U05Flow flow,
            U05QuestionRouteAdmissionAdapter admission,
            U06NonProductionPhysicalMode23Service selection,
            U07NonProductionDeliveryWaitRecovery wait,
            CDPRepository cdps,
            RuntimeBindingService bindings,
            ClinicalRunRepository runs,
            TransactionTemplate transactions,
            U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost host) {
        return new U07NonProductionPhysicalFollowupCoordinator(
                flow, admission, selection, wait, cdps,
                bindings, runs, transactions, host);
    }
}
