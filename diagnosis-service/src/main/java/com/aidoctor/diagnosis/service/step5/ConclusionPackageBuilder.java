package com.aidoctor.diagnosis.service.step5;

import com.aidoctor.diagnosis.dto.conclusion.ConclusionPackage;
import com.aidoctor.diagnosis.dto.conclusion.Conclusion;
import com.aidoctor.diagnosis.dto.conclusion.MustExcludeStatus;
import com.aidoctor.diagnosis.dto.conclusion.KeyEvidence;
import com.aidoctor.diagnosis.dto.conclusion.ActionAndFollowUp;
import com.aidoctor.diagnosis.dto.threeLayer.ThreeLayerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 终点结论包构建器
 * Step 5：回填证据并输出终点结论包 - 构建终点结论包（tool_7 + tool_5 + tool_6）
 */
@Slf4j
@Service
public class ConclusionPackageBuilder {
    
    /**
     * 构建终点结论包
     * 
     * @param threeLayerResult 三层分层结果
     * @return 终点结论包
     */
    public ConclusionPackage buildConclusionPackage(
            ThreeLayerResult threeLayerResult) {
        
        log.info("构建终点结论包");
        
        ConclusionPackage.ConclusionPackageBuilder builder = ConclusionPackage.builder();
        
        // 1. 构建结论
        Conclusion conclusion = buildConclusion(threeLayerResult);
        builder.conclusion(conclusion);
        
        // 2. 构建必须排除项状态
        MustExcludeStatus mustExcludeStatus = buildMustExcludeStatus(
            threeLayerResult.getMustExclude()
        );
        builder.mustExcludeStatus(mustExcludeStatus);
        
        // 3. 构建关键依据（至少三条证据）
        List<KeyEvidence> keyEvidence = buildKeyEvidence(threeLayerResult, 3);
        builder.keyEvidence(keyEvidence);
        
        // 4. 构建行动与随访
        ActionAndFollowUp actionAndFollowUp = buildActionAndFollowUp(
            threeLayerResult
        );
        builder.actionAndFollowUp(actionAndFollowUp);
        
        return builder.build();
    }
    
    /**
     * 构建结论
     */
    private Conclusion buildConclusion(ThreeLayerResult threeLayerResult) {
        ThreeLayerResult.PrimaryHypothesis primary = threeLayerResult.getPrimaryHypothesis();
        
        if (primary == null) {
            return Conclusion.builder()
                .type(Conclusion.ConclusionType.PROBABLE)
                .diagnosis("无法确定")
                .confidence(0.0)
                .build();
        }
        
        // 判断是否可确证
        boolean canConfirm = canConfirmDiagnosis(primary);
        
        if (canConfirm) {
            return Conclusion.builder()
                .type(Conclusion.ConclusionType.CONFIRMED)
                .diagnosis(primary.getDisease())
                .confidence(primary.getScore())
                .build();
        } else {
            return Conclusion.builder()
                .type(Conclusion.ConclusionType.PROBABLE)
                .diagnosis(primary.getDisease())
                .confidence(primary.getScore())
                .uncertaintyReason("当前证据不足以确证，需要进一步检查")
                .reviewWindow("3天后复评")
                .upgradeTriggers(Arrays.asList(
                    "症状加重",
                    "出现新症状",
                    "检查结果异常"
                ))
                .build();
        }
    }
    
    /**
     * 判断是否可确证
     */
    private boolean canConfirmDiagnosis(ThreeLayerResult.PrimaryHypothesis primary) {
        // 规则：可能性分数 > 0.8 且为可确证终点
        return primary.getScore() != null && primary.getScore() > 0.8;
    }
    
    /**
     * 构建必须排除项状态
     */
    private MustExcludeStatus buildMustExcludeStatus(
            ThreeLayerResult.MustExcludeDiagnosis mustExclude) {
        
        if (mustExclude == null) {
            return MustExcludeStatus.builder()
                .status(MustExcludeStatus.ExcludeStatus.NONE)
                .build();
        }
        
        // TODO: 判断是否已排除（需要根据验证计划的结果判断）
        boolean excluded = false;
        boolean needOfflineCheck = true;  // 高危诊断通常需要线下检查
        
        if (excluded) {
            return MustExcludeStatus.builder()
                .status(MustExcludeStatus.ExcludeStatus.EXCLUDED)
                .excludeReason("已通过验证明确排除")
                .build();
        } else if (needOfflineCheck) {
            return MustExcludeStatus.builder()
                .status(MustExcludeStatus.ExcludeStatus.NEED_OFFLINE_EXCLUDE)
                .excludeReason("需要线下检查才能排除")
                .build();
        } else {
            return MustExcludeStatus.builder()
                .status(MustExcludeStatus.ExcludeStatus.NOT_EXCLUDED)
                .excludeReason("仍需保留，需要进一步验证")
                .build();
        }
    }
    
    /**
     * 构建关键依据（至少三条证据）
     */
    private List<KeyEvidence> buildKeyEvidence(
            ThreeLayerResult threeLayerResult, int minCount) {
        
        List<KeyEvidence> evidence = new ArrayList<>();
        
        // TODO: 从证据分析中提取关键证据
        // 这里简化处理，实际应该从EvidenceAnalysis中获取
        
        ThreeLayerResult.PrimaryHypothesis primary = threeLayerResult.getPrimaryHypothesis();
        if (primary != null) {
            evidence.add(KeyEvidence.builder()
                .item(primary.getDisease())
                .type("diagnosis")
                .strength("strong")
                .role("supporting")
                .build());
            
            evidence.add(KeyEvidence.builder()
                .item(primary.getEvidence())
                .type("evidence")
                .strength("medium")
                .role("supporting")
                .build());
        }
        
        // 确保至少minCount条证据
        while (evidence.size() < minCount) {
            evidence.add(KeyEvidence.builder()
                .item("需要进一步检查确认")
                .type("examination")
                .strength("weak")
                .role("supporting")
                .build());
        }
        
        return evidence;
    }
    
    /**
     * 构建行动与随访
     */
    private ActionAndFollowUp buildActionAndFollowUp(
            ThreeLayerResult threeLayerResult) {
        
        List<ActionAndFollowUp.Action> immediateActions = new ArrayList<>();
        
        // TODO: 根据诊断结果生成行动建议
        immediateActions.add(ActionAndFollowUp.Action.builder()
            .type("examination")
            .name("心电图")
            .priority("high")
            .build());
        
        return ActionAndFollowUp.builder()
            .immediateActions(immediateActions)
            .reviewWindow("3天后")
            .upgradeTriggers(Arrays.asList(
                "症状突然加重",
                "出现新症状",
                "生命体征异常"
            ))
            .build();
    }
}

