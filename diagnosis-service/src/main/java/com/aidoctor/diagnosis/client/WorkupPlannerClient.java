package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 检查建议服务客户端（脑区D）
 */
@FeignClient(name = "workup-planner-service", url = "${workup-planner.service-url:http://localhost:8082}")
public interface WorkupPlannerClient {
    
    /**
     * 构建验证计划
     */
    @PostMapping("/api/v1/workup/build-verification-plan")
    Object buildVerificationPlan(@RequestBody Object request);
}

