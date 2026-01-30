package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 解释生成服务客户端（脑区G）
 */
@FeignClient(name = "explanation-service", url = "${explanation.service-url:http://localhost:8089}")
public interface ExplanationServiceClient {
    
    /**
     * 生成终点结论包
     */
    @PostMapping("/api/v1/explain/conclusion-package")
    Object generateConclusionPackage(@RequestBody Object request);
}

