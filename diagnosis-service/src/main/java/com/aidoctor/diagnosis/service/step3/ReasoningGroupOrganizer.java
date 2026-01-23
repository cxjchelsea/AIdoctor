package com.aidoctor.diagnosis.service.step3;

import com.aidoctor.diagnosis.dto.threeLayer.ThreeLayerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推理子组组织器
 * Step 3：组织候选集并建立分流路径 - 组织推理子组（脑区C）
 */
@Slf4j
@Service
public class ReasoningGroupOrganizer {
    
    /**
     * 组织推理子组
     * 将候选疾病按照相似性、对比性进行分组
     * 
     * @param threeLayerResult 三层分层结果
     * @return 推理子组（组名 -> 疾病列表）
     */
    public Map<String, List<String>> organizeReasoningGroups(ThreeLayerResult threeLayerResult) {
        log.info("组织推理子组");
        
        Map<String, List<String>> groups = new HashMap<>();
        
        // TODO: 实现推理子组组织逻辑
        // 1. 按系统分类（心血管、呼吸、消化等）
        // 2. 按病理生理机制分类
        // 3. 按诊断树理论分类
        
        // 示例：简单分组
        List<String> cardiovascularGroup = new ArrayList<>();
        if (threeLayerResult.getPrimaryHypothesis() != null) {
            String disease = threeLayerResult.getPrimaryHypothesis().getDisease();
            if (disease != null && (disease.contains("心") || disease.contains("血管"))) {
                cardiovascularGroup.add(disease);
            }
        }
        
        if (!cardiovascularGroup.isEmpty()) {
            groups.put("心血管疾病组", cardiovascularGroup);
        }
        
        return groups;
    }
}

