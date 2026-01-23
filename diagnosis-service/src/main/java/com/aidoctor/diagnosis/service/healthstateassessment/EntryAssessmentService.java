package com.aidoctor.diagnosis.service.healthstateassessment;

import com.aidoctor.diagnosis.client.HealthStateAssessmentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI诊断入口判定服务（P0模块）
 * 负责AI诊断入口的判定逻辑
 */
@Service
public class EntryAssessmentService {
    
    @Autowired
    private HealthStateAssessmentClient healthStateAssessmentClient;
    
    /**
     * 执行入口判定
     * 包括5个步骤：
     * Step 1：接收用户输入
     * Step 2：识别症状/困扰
     * Step 3：方向澄清
     * Step 4：危险信号检查
     * Step 5：输出路径结果
     */
    public Object performEntryAssessment(Object request) {
        return healthStateAssessmentClient.performEntryAssessment(request);
    }
}

