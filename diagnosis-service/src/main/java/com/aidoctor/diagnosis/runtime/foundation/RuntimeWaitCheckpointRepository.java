package com.aidoctor.diagnosis.runtime.foundation;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.Optional;
public interface RuntimeWaitCheckpointRepository extends JpaRepository<RuntimeWaitCheckpointRecord,String>{Optional<RuntimeWaitCheckpointRecord> findByQuestionDeliveredWaitEffectId(String effectId);}
