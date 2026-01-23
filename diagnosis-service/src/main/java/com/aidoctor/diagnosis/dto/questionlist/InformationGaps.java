package com.aidoctor.diagnosis.dto.questionlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 信息缺口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformationGaps {
    
    /**
     * 必填缺口（缺失则不能进入阶段3）
     */
    private List<String> requiredGaps;
    
    /**
     * 重要缺口（可进入但必须提示不确定性与风险）
     */
    private List<String> importantGaps;
    
    /**
     * 可选缺口（后续补充即可）
     */
    private List<String> optionalGaps;
}

