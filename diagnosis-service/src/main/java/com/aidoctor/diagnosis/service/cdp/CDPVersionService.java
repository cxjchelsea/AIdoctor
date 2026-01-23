package com.aidoctor.diagnosis.service.cdp;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.entity.CDPVersion;
import com.aidoctor.diagnosis.repository.CDPVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CDP版本服务
 * 负责CDP版本的管理
 */
@Service
public class CDPVersionService {
    
    @Autowired
    private CDPVersionRepository cdpVersionRepository;
    
    @Autowired
    private com.aidoctor.diagnosis.util.JsonUtil jsonUtil;
    
    @Transactional
    public CDPVersion createInitialVersion(CDP cdp) {
        CDPVersion version = new CDPVersion();
        version.setCdpId(cdp.getId());
        version.setVersionNumber(1);
        version.setCdpDataJson(jsonUtil.toJson(cdp));
        version.setCreateTime(LocalDateTime.now());
        return cdpVersionRepository.save(version);
    }
    
    /**
     * 创建新版本
     */
    @Transactional
    public CDPVersion createVersion(CDP cdp) {
        // 获取当前最大版本号
        Integer maxVersion = cdpVersionRepository.findMaxVersionByCdpId(cdp.getId())
                .orElse(0);
        
        CDPVersion version = new CDPVersion();
        version.setCdpId(cdp.getId());
        version.setVersionNumber(maxVersion + 1);
        version.setCdpDataJson(jsonUtil.toJson(cdp));
        version.setCreateTime(LocalDateTime.now());
        return cdpVersionRepository.save(version);
    }
    
    /**
     * 根据CDP ID获取所有版本
     */
    public List<CDPVersion> getVersionsByCdpId(String cdpId) {
        return cdpVersionRepository.findByCdpIdOrderByVersionNumberDesc(cdpId);
    }
    
    /**
     * 根据版本号获取版本
     */
    public Optional<CDPVersion> getVersionByCdpIdAndVersionNumber(String cdpId, Integer versionNumber) {
        return cdpVersionRepository.findByCdpIdAndVersionNumber(cdpId, versionNumber);
    }
}

