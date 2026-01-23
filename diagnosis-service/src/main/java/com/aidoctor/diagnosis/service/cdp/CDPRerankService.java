package com.aidoctor.diagnosis.service.cdp;

import com.aidoctor.diagnosis.entity.CDP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CDP重排服务
 * 负责CDP的重新排序功能
 */
@Service
public class CDPRerankService {
    
    @Autowired
    private CDPManager cdpManager;
    
    /**
     * 根据优先级重排CDP列表
     */
    @Transactional
    public List<CDP> rerankCDPs(List<CDP> cdps) {
        // 根据状态、时间等规则进行重排
        cdps.sort((c1, c2) -> {
            // 状态优先级：completed > 其他状态
            String status1 = c1.getCdpStatus() != null ? c1.getCdpStatus() : "";
            String status2 = c2.getCdpStatus() != null ? c2.getCdpStatus() : "";
            int statusCompare = status2.compareTo(status1);
            if (statusCompare != 0) {
                return statusCompare;
            }
            // 时间新的在前
            if (c1.getUpdatedAt() != null && c2.getUpdatedAt() != null) {
                return c2.getUpdatedAt().compareTo(c1.getUpdatedAt());
            }
            // 如果更新时间为空，使用创建时间
            if (c1.getCreatedAt() != null && c2.getCreatedAt() != null) {
                return c2.getCreatedAt().compareTo(c1.getCreatedAt());
            }
            return 0;
        });
        return cdps;
    }
}

