package com.aidoctor.diagnosis.runtime.governance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CapabilityCallTraceRepository extends JpaRepository<CapabilityCallTraceRecord, String> {
}
