package com.aidoctor.diagnosis.runtime.foundation;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import javax.persistence.LockModeType;import java.util.Optional;
public interface RuntimeThreadStateRepository extends JpaRepository<RuntimeThreadStateRecord,String>{
 Optional<RuntimeThreadStateRecord> findByConsultationId(String consultationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RuntimeThreadStateRecord r where r.threadId=:threadId") Optional<RuntimeThreadStateRecord> findByThreadIdForUpdate(@Param("threadId")String threadId);
}
