package com.aidoctor.diagnosis.service.healthstateassessment;

import com.aidoctor.diagnosis.client.HealthStateAssessmentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 健康状态判定服务代理
 * 调用Python健康状态判定服务（tool_0）
 */
@Service
public class HealthStateAssessmentService {
    
    @Autowired
    private HealthStateAssessmentClient healthStateAssessmentClient;
    
    /**
     * 评估健康状态
     */
    public Object assessHealthState(Object request) {
        return healthStateAssessmentClient.assessHealthState(request);
    }
}

