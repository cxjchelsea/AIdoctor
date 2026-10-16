package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactApplicationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Nonproduction F8 HTTP assembly: no answer authentication or clinical authority is minted. */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="answer-resume-http-enabled", havingValue="true", matchIfMissing=false)
public class V1NonProductionAnswerResumeHostConfig {
    @Bean
    @ConditionalOnMissingBean(U07NonProductionAnswerResumeAdmission.class)
    @ConditionalOnBean({ConsultationRepository.class,RuntimeThreadStateRepository.class,
            RuntimeWaitCheckpointRepository.class,CDPRepository.class})
    public U07NonProductionAnswerResumeAdmission v1AnswerAdmission(
            ConsultationRepository consultations, RuntimeThreadStateRepository threads,
            RuntimeWaitCheckpointRepository checkpoints, CDPRepository cdps) {
        return new U07NonProductionAnswerResumeAdmission(
                consultations,threads,checkpoints,cdps);
    }

    @Bean
    @ConditionalOnMissingBean(U07NonProductionDurableF8Consume.class)
    @ConditionalOnBean({JdbcTemplate.class,TransactionTemplate.class,
            CanonicalBusinessEventLedger.class,ClinicalRunCoordinator.class,
            ConsultationRepository.class,RuntimeThreadStateRepository.class,
            RuntimeWaitCheckpointRepository.class})
    public U07NonProductionDurableF8Consume v1DurableAnswerConsumption(
            JdbcTemplate jdbc,TransactionTemplate tx,CanonicalBusinessEventLedger ledger,
            ClinicalRunCoordinator runs, ConsultationRepository consultations,
            RuntimeThreadStateRepository threads, RuntimeWaitCheckpointRepository checkpoints) {
        return new U07NonProductionDurableF8Consume(
                jdbc,tx,ledger,runs,consultations,threads,checkpoints);
    }

    @Bean
    @ConditionalOnMissingBean(U07NonProductionTrustedAnswerU02Bridge.class)
    @ConditionalOnBean({ClinicalRunRepository.class,CanonicalBusinessEventRepository.class,
            U07NonProductionDurableF8Consume.class})
    public U07NonProductionTrustedAnswerU02Bridge v1TrustedResumedRun(
            ClinicalRunRepository runs,CanonicalBusinessEventRepository events,
            U07NonProductionDurableF8Consume consumer) {
        return new U07NonProductionTrustedAnswerU02Bridge(runs,events,consumer);
    }
}
