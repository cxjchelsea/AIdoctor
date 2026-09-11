package com.aidoctor.diagnosis.runtime.foundation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CanonicalBusinessEventRepository extends JpaRepository<CanonicalBusinessEventRecord, String> {
    Optional<CanonicalBusinessEventRecord> findByIdempotencyKey(String idempotencyKey);
}
