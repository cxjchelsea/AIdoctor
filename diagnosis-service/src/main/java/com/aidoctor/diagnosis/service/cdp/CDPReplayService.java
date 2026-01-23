package com.aidoctor.diagnosis.service.cdp;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.entity.CDPVersion;
import com.aidoctor.diagnosis.repository.CDPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CDP回放服务
 * 负责CDP的回放功能
 */
@Service
public class CDPReplayService {
    
    @Autowired
    private CDPRepository cdpRepository;
    
    @Autowired
    private CDPVersionService cdpVersionService;
    
    @Autowired
    private com.aidoctor.diagnosis.util.JsonUtil jsonUtil;
    
    /**
     * 回放指定版本的CDP
     */
    @Transactional
    public CDP replayCDP(String cdpId, Integer versionNumber) {
        CDPVersion version = cdpVersionService.getVersionByCdpIdAndVersionNumber(cdpId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionNumber));
        
        // 恢复CDP数据
        CDP cdp = jsonUtil.fromJson(version.getCdpDataJson(), CDP.class);
        return cdpRepository.save(cdp);
    }
}

