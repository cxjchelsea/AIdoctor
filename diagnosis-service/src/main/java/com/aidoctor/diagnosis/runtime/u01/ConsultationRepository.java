package com.aidoctor.diagnosis.runtime.u01;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import javax.persistence.LockModeType;import java.util.Optional;
public interface ConsultationRepository extends JpaRepository<ConsultationRecord,String>{Optional<ConsultationRecord> findByStartEventId(String startEventId);@Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select c from ConsultationRecord c where c.consultationId=:consultationId")Optional<ConsultationRecord> findByIdForUpdate(@Param("consultationId")String consultationId);}
