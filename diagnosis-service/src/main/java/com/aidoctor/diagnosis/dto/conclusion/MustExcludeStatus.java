package com.aidoctor.diagnosis.dto.conclusion;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 必须排除项状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MustExcludeStatus {
    private ExcludeStatus status;
    private String excludeReason;
    
    public enum ExcludeStatus {
        NONE,                    // 无必须排除项
        EXCLUDED,                // 已排除
        NOT_EXCLUDED,            // 未排除
        NEED_OFFLINE_EXCLUDE     // 需线下排除
    }
}

