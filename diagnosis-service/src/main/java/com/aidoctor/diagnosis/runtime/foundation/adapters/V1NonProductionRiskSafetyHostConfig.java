package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge;
import com.aidoctor.diagnosis.runtime.u03.U03CommitService;
import com.aidoctor.diagnosis.runtime.u03.U03DecisionService;
import com.aidoctor.diagnosis.runtime.u03.U03GovernedCandidateGateway;
import com.aidoctor.diagnosis.runtime.u03.U03OutboundProducer;
import com.aidoctor.diagnosis.runtime.u03.U03PostCommitFinalizer;
import com.aidoctor.diagnosis.runtime.u03.U03StateProposalFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Optional physical risk/safety execution wiring.
 *
 * No production activation, synthetic clinical decision, release authorization,
 * trace acceptance, safety override or fallback is created here. If any
 * independently governed owner is missing, the HTTP U01-U04 route stays absent.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "u01-u04-http-enabled", havingValue = "true", matchIfMissing = false)
public class V1NonProductionRiskSafetyHostConfig {
    @Bean
    @ConditionalOnBean({
            U03GovernedCandidateGateway.class,
            U03DecisionService.class,
            U03StateProposalFactory.class,
            U03CommitService.class,
            U03PostCommitFinalizer.class,
            U03OutboundProducer.class,
            U02U03GovernedSafetyBridge.class,
            TransactionTemplate.class
    })
    public U07NonProductionGovernedRiskSafetyContinuation v1NonProductionRiskSafetyContinuation(
            U03GovernedCandidateGateway gateway,
            U03DecisionService decisions,
            U03StateProposalFactory proposals,
            U03CommitService riskCommitter,
            U03PostCommitFinalizer finalizer,
            U03OutboundProducer outbound,
            U02U03GovernedSafetyBridge safety,
            TransactionTemplate transactions) {
        return new U07NonProductionGovernedRiskSafetyContinuation(
                gateway, decisions, proposals, riskCommitter,
                finalizer, outbound, safety, transactions);
    }
}
