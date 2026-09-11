package com.aidoctor.diagnosis.runtime.u01;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<ConsultationRecord, String> {
    Optional<ConsultationRecord> findByStartEventId(String startEventId);
}
