package com.aidoctor.diagnosis.dto.conclusion;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 终点结论包
 * 根据系统设计方案，包含四要素：
 * 1. 结论（可确证/不可确证）
 * 2. 必须排除项状态
 * 3. 关键依据（至少三条证据）
 * 4. 行动与随访
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConclusionPackage {
    
    /**
     * 结论
     */
    private Conclusion conclusion;
    
    /**
     * 必须排除项状态
     */
    private MustExcludeStatus mustExcludeStatus;
    
    /**
     * 关键依据（至少三条证据）
     */
    private List<KeyEvidence> keyEvidence;
    
    /**
     * 行动与随访
     */
    private ActionAndFollowUp actionAndFollowUp;
}

