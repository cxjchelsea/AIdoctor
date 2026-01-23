package com.aidoctor.diagnosis.service.step2;

import com.aidoctor.diagnosis.dto.threeLayer.ThreeLayerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 三层分层分类器
 * Step 2：构建鉴别诊断候选集并分层 - 将候选疾病分为三层（脑区C + 脑区F）
 */
@Slf4j
@Service
public class ThreeLayerClassifier {
    
    /**
     * 将候选疾病分为三层
     * 
     * @param possibilities 疾病可能性字典
     * @return 三层分层结果
     */
    public ThreeLayerResult classify(Map<String, Double> possibilities) {
        log.info("进行三层分层分类");
        
        Map<String, Double> candidates = possibilities;
        if (candidates == null || candidates.isEmpty()) {
            return ThreeLayerResult.builder()
                .primaryHypothesis(null)
                .mainAlternatives(Collections.emptyList())
                .mustExclude(null)
                .build();
        }
        
        // 按概率排序
        List<Map.Entry<String, Double>> sortedCandidates = candidates.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        // 首要假设：概率最高的1个
        ThreeLayerResult.PrimaryHypothesis primaryHypothesis = null;
        if (!sortedCandidates.isEmpty()) {
            Map.Entry<String, Double> top = sortedCandidates.get(0);
            primaryHypothesis = ThreeLayerResult.PrimaryHypothesis.builder()
                .disease(top.getKey())
                .score(top.getValue())
                .layer("primary")
                .evidence("概率最高")
                .build();
        }
        
        // 主要备选诊断：概率次高的2-5个
        List<ThreeLayerResult.MainAlternative> mainAlternatives = new ArrayList<>();
        int startIdx = primaryHypothesis != null ? 1 : 0;
        int endIdx = Math.min(startIdx + 4, sortedCandidates.size());
        for (int i = startIdx; i < endIdx; i++) {
            Map.Entry<String, Double> entry = sortedCandidates.get(i);
            mainAlternatives.add(ThreeLayerResult.MainAlternative.builder()
                .disease(entry.getKey())
                .score(entry.getValue())
                .layer("alternative")
                .evidence("概率次高")
                .build());
        }
        
        // 必须排除的高危诊断：概率可能不高，但必须排除
        ThreeLayerResult.MustExcludeDiagnosis mustExclude = null;
        // TODO: 根据高危诊断规则库识别必须排除的诊断
        
        return ThreeLayerResult.builder()
            .primaryHypothesis(primaryHypothesis)
            .mainAlternatives(mainAlternatives)
            .mustExclude(mustExclude)
            .build();
    }
}

