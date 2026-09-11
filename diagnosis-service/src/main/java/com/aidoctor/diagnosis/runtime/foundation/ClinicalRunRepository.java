package com.aidoctor.diagnosis.runtime.foundation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalRunRepository extends JpaRepository<ClinicalRunRecord, String> {
}
