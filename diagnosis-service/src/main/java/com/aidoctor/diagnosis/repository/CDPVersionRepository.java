package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.CDPVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CDP版本数据访问接口
 */
@Repository
public interface CDPVersionRepository extends JpaRepository<CDPVersion, Long> {
    
    /**
     * 根据CDP ID查找所有版本，按版本号降序排列
     */
    List<CDPVersion> findByCdpIdOrderByVersionNumberDesc(String cdpId);
    
    /**
     * 根据CDP ID和版本号查找版本
     */
    Optional<CDPVersion> findByCdpIdAndVersionNumber(String cdpId, Integer versionNumber);
    
    /**
     * 查找指定CDP的最大版本号
     */
    @Query("SELECT MAX(v.versionNumber) FROM CDPVersion v WHERE v.cdpId = :cdpId")
    Optional<Integer> findMaxVersionByCdpId(@Param("cdpId") String cdpId);
}

