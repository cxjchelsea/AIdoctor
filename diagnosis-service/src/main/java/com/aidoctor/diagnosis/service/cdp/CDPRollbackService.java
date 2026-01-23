package com.aidoctor.diagnosis.service.cdp;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.entity.CDPVersion;
import com.aidoctor.diagnosis.repository.CDPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CDP回退服务
 * 负责CDP的回退功能
 */
@Service
public class CDPRollbackService {
    
    @Autowired
    private CDPRepository cdpRepository;
    
    @Autowired
    private CDPVersionService cdpVersionService;
    
    @Autowired
    private com.aidoctor.diagnosis.util.JsonUtil jsonUtil;
    
    /**
     * 回退到指定版本
     */
    @Transactional
    public CDP rollbackToVersion(String cdpId, Integer versionNumber) {
        // 先保存当前版本
        CDP currentCDP = cdpRepository.findById(cdpId)
                .orElseThrow(() -> new RuntimeException("CDP not found: " + cdpId));
        cdpVersionService.createVersion(currentCDP);
        
        // 回退到指定版本
        CDPVersion targetVersion = cdpVersionService.getVersionByCdpIdAndVersionNumber(cdpId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionNumber));
        
        CDP rolledBackCDP = jsonUtil.fromJson(targetVersion.getCdpDataJson(), CDP.class);
        rolledBackCDP.setId(cdpId);
        return cdpRepository.save(rolledBackCDP);
    }
}

