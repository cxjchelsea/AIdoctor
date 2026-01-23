package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 风险评估服务客户端（脑区F）
 */
@FeignClient(name = "risk-assessment-service", url = "${risk-assessment.service-url:http://localhost:8084}")
public interface RiskAssessmentClient {
    
    /**
     * 风险评估
     */
    @PostMapping("/api/v1/risk/assess")
    Object assessRisk(@RequestBody Object request);
    
    /**
     * 最终风险评估
     */
    @PostMapping("/api/v1/risk/assess-final")
    Object assessFinalRisk(@RequestBody Object request);
}

