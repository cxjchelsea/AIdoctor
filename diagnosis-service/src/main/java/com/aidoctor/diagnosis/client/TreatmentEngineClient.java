package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 治疗推理服务客户端（脑区E）
 */
@FeignClient(name = "treatment-engine-service", url = "${treatment-engine.service-url:http://localhost:8083}")
public interface TreatmentEngineClient {
    
    /**
     * 生成治疗方案
     */
    @PostMapping("/api/v1/treatment/generate-plan")
    Object generateTreatmentPlan(@RequestBody Object request);
}

