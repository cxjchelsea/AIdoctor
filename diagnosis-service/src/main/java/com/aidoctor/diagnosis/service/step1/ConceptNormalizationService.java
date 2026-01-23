package com.aidoctor.diagnosis.service.step1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 概念归一化服务
 * Step 1：识别问题 - 将用户的自然语言描述归一化为标准医学术语（脑区A）
 */
@Slf4j
@Service
public class ConceptNormalizationService {
    
    /**
     * 归一化主诉名称
     * 将用户描述的主诉（如"胸痛"、"心口疼"、"胸口闷"）归一化为标准医学术语
     * 
     * @param originalText 用户原始描述
     * @return 归一化后的主诉名称
     */
    public String normalizeChiefComplaint(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return null;
        }
        
        // TODO: 调用对话服务的概念归一化功能
        // 这里可以使用NLU服务或者规则匹配进行归一化
        
        // 示例：简单的关键词映射（实际应该使用更复杂的NLU模型）
        String normalized = originalText.toLowerCase();
        
        // 胸痛相关
        if (normalized.contains("胸") && (normalized.contains("痛") || normalized.contains("疼") || normalized.contains("闷"))) {
            return "胸痛";
        }
        
        // 头痛相关
        if (normalized.contains("头") && (normalized.contains("痛") || normalized.contains("疼"))) {
            return "头痛";
        }
        
        // 默认返回原文本（后续可以通过NLU服务改进）
        return originalText;
    }
    
    /**
     * 归一化症状特征
     * 提取和归一化症状的诱因、缓解因素等特征
     * 
     * @param description 症状描述
     * @return 归一化后的特征
     */
    public String normalizeSymptomFeature(String description) {
        // TODO: 实现症状特征归一化
        return description;
    }
}

