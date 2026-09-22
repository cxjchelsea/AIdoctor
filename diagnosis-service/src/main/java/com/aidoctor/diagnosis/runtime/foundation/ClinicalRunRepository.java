package com.aidoctor.diagnosis.runtime.foundation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicalRunRepository extends JpaRepository<ClinicalRunRecord, String> {
    Optional<ClinicalRunRecord> findFirstByConsultationIdAndEventIdOrderByCreatedAtAsc(String consultationId, String eventId);
}
