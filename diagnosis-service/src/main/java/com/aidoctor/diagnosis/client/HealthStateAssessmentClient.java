package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 健康状态判定服务客户端（脑区0）
 * 调用Python健康状态判定服务
 */
@FeignClient(name = "health-state-assessment-service", url = "${health-state-assessment.service-url:http://localhost:8081}")
public interface HealthStateAssessmentClient {
    
    /**
     * 评估健康状态
     */
    @PostMapping("/api/v1/health-state-assessment/assess")
    Object assessHealthState(@RequestBody Object request);
    
    /**
     * 执行入口判定（兼容旧接口）
     */
    @PostMapping("/api/v1/entry-assessment/perform")
    Object performEntryAssessment(@RequestBody Object request);
    
    /**
     * A1: 需求分类
     */
    @PostMapping("/api/v1/wellness-screening/a1-demand-classification")
    Object performDemandClassification(@RequestBody Object request);
    
    /**
     * A2: 收集健康画像
     */
    @PostMapping("/api/v1/wellness-screening/a2-health-profile-collection")
    Object collectHealthProfile(@RequestBody Object request);
    
    /**
     * A3: 执行分支
     */
    @PostMapping("/api/v1/wellness-screening/a3-branch-execution")
    Object executeBranch(@RequestBody Object request);
    
    /**
     * A4: 生成统一结果
     */
    @PostMapping("/api/v1/wellness-screening/a4-unified-result-generation")
    Object generateUnifiedResult(@RequestBody Object request);
    
    /**
     * A5: 设置随访
     */
    @PostMapping("/api/v1/wellness-screening/a5-follow-up-setup")
    Object setupFollowUp(@RequestBody Object request);
}

