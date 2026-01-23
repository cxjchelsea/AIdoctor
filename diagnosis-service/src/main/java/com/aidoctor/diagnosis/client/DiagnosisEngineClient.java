package com.aidoctor.diagnosis.client;

import com.aidoctor.diagnosis.dto.engine.DiagnosisEngineRequest;
import com.aidoctor.diagnosis.dto.engine.DiagnosisEngineResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 诊断引擎服务客户端（脑区C）
 */
@FeignClient(name = "diagnosis-engine-service", url = "${diagnosis-engine.service-url:http://localhost:8086}")
public interface DiagnosisEngineClient {
    
    /**
     * 诊断（兼容旧接口）
     */
    @PostMapping("/api/v1/engine/diagnose")
    DiagnosisEngineResult diagnose(@RequestBody DiagnosisEngineRequest request);
    
    /**
     * 生成鉴别诊断候选集
     */
    @PostMapping("/api/v1/engine/generate-ddx-candidates")
    Object generateDDxCandidates(@RequestBody Object request);
    
    /**
     * 组织推理子组
     */
    @PostMapping("/api/v1/engine/organize-reasoning-groups")
    Object organizeReasoningGroups(@RequestBody Object request);
    
    /**
     * 分析证据
     */
    @PostMapping("/api/v1/engine/analyze-evidence")
    Object analyzeEvidence(@RequestBody Object request);
    
    /**
     * 更新三层排序
     */
    @PostMapping("/api/v1/engine/update-ranking")
    Object updateRanking(@RequestBody Object request);
}

