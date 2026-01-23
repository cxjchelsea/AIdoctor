package com.aidoctor.diagnosis.dto.questionlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * 主要问题（主诉）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChiefComplaint {
    private String name;  // 归一化后的主诉名称
    private String originalText;  // 用户原始描述
    private String duration;  // 持续时间
    private String onsetMode;  // 起病方式（sudden/gradual）
    private Integer severity;  // 严重程度（0-10）
    private String frequency;  // 频率（continuous/intermittent/occasional）
    private String location;  // 位置
    private Map<String, Object> features;  // 症状特征（诱因、缓解因素等）
    private String triageLevel;  // 分诊等级（L1-L4）
}

