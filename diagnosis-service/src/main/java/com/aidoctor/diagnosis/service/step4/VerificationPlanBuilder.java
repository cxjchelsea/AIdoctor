package com.aidoctor.diagnosis.service.step4;

import com.aidoctor.diagnosis.dto.threeLayer.ThreeLayerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证计划构建器
 * Step 4：采集关键证据并形成排序与验证计划 - 构建验证计划（脑区D）
 */
@Slf4j
@Service
public class VerificationPlanBuilder {
    
    /**
     * 构建验证计划
     * 确定需要哪些检查来验证或排除诊断
     * 
     * @param threeLayerResult 三层分层结果
     * @return 验证计划（检查名称 -> 优先级）
     */
    public Map<String, Integer> buildVerificationPlan(ThreeLayerResult threeLayerResult) {
        log.info("构建验证计划");
        
        Map<String, Integer> verificationPlan = new HashMap<>();
        
        // TODO: 实现验证计划构建逻辑
        // 1. 检查价值评估
        // 2. 信息增益计算
        // 3. 检查优先级排序
        
        // 示例：简单验证计划
        if (threeLayerResult.getPrimaryHypothesis() != null) {
            String disease = threeLayerResult.getPrimaryHypothesis().getDisease();
            if (disease != null && disease.contains("心")) {
                verificationPlan.put("心电图", 1);
                verificationPlan.put("心肌酶", 1);
            }
        }
        
        return verificationPlan;
    }
}

