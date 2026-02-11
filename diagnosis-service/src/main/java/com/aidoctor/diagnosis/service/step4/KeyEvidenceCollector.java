package com.aidoctor.diagnosis.service.step4;

import com.aidoctor.diagnosis.dto.questionlist.StructuredQuestionList;
import com.aidoctor.diagnosis.dto.threeLayer.ThreeLayerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 关键证据采集服务
 * Step 4：采集关键证据并形成排序与验证计划 - 采集关键证据（tool_2）
 */
@Slf4j
@Service
public class KeyEvidenceCollector {
    
    /**
     * 采集关键证据
     * 根据分流路径，采集关键证据
     * 
     * @param questionList 结构化问题清单
     * @param threeLayerResult 三层分层结果
     * @param routingPaths 分流路径
     * @return 关键证据列表
     */
    public List<String> collectKeyEvidence(
            StructuredQuestionList questionList,
            ThreeLayerResult threeLayerResult,
            Map<String, Integer> routingPaths) {
        
        log.info("采集关键证据");
        
        List<String> keyEvidence = new ArrayList<>();
        
        // TODO: 实现关键证据采集逻辑
        // 1. 根据分流路径采集问诊证据
        // 2. 根据验证计划采集检查证据
        // 3. 评估证据价值
        
        return keyEvidence;
    }
}

