package com.aidoctor.diagnosis.runtime.u06.wait;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.Optional;
public interface ConsultationWaitEffectRepository extends JpaRepository<ConsultationWaitEffectRecord,String>{
 Optional<ConsultationWaitEffectRecord> findByIdempotencyKey(String idempotencyKey);Optional<ConsultationWaitEffectRecord> findByParentDeliveredWaitEffectId(String parentDeliveredWaitEffectId);
}
