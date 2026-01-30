package com.aidoctor.diagnosis.service;

import com.aidoctor.diagnosis.client.HealthStateAssessmentClient;
import com.aidoctor.diagnosis.dto.request.DiagnosisRequest;
import com.aidoctor.diagnosis.dto.request.UserAnswer;
import com.aidoctor.diagnosis.dto.response.DiagnosisResponse;
import com.aidoctor.diagnosis.dto.response.DiagnosisResult;
import com.aidoctor.diagnosis.dto.conclusion.ConclusionPackage;
import com.aidoctor.diagnosis.dto.conclusion.Conclusion;
import com.aidoctor.diagnosis.dto.conclusion.MustExcludeStatus;
import com.aidoctor.diagnosis.dto.conclusion.KeyEvidence;
import com.aidoctor.diagnosis.dto.conclusion.ActionAndFollowUp;
import com.aidoctor.diagnosis.entity.CDP;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidoctor.diagnosis.exception.CDPNotFoundException;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import com.aidoctor.diagnosis.service.orchestration.DiagnosisWorkflowOrchestrator;
import com.aidoctor.diagnosis.service.wellness.WellnessScreeningOrchestrator;
import com.aidoctor.diagnosis.util.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 诊断编排服务
 * 负责诊断流程的启动、编排和协调
 * 
 * 参考文档：
 * - 《AI医生系统-业务逻辑详细设计.md》
 * - 《AI医生系统-技术架构设计.md》
 */
@Slf4j
@Service
public class DiagnosisOrchestrationService {
    
    @Autowired
    private CDPManager cdpManager;
    
    @Autowired
    private HealthStateAssessmentClient healthStateAssessmentClient;
    
    @Autowired
    private DiagnosisWorkflowOrchestrator diagnosisWorkflowOrchestrator;
    
    @Autowired
    private WellnessScreeningOrchestrator wellnessScreeningOrchestrator;
    
    /**
     * 启动诊断流程
     * 
     * @param request 诊断请求
     * @return 诊断响应
     */
    @Transactional
    public DiagnosisResponse startDiagnosis(DiagnosisRequest request) {
        log.info("启动诊断流程: userId={}", request.getUserId());
        
        // 1. 生成会话ID
        String sessionId = generateSessionId();
        
        // 2. 创建CDP
        CDP cdp = cdpManager.createCDP(request.getUserId(), sessionId);
        log.info("CDP创建成功: cdpId={}", cdp.getId());
        
        // 设置追踪上下文（在调用健康判定服务前设置，以便追踪）
        TraceContext.setCdpId(cdp.getId());
        
        // 3. 调用健康状态判定服务（容错处理：如果服务不可用，使用默认值）
        // 传入已创建的CDP ID，确保CDP ID一致性
        Map<String, Object> assessmentRequest = buildAssessmentRequest(request, cdp.getId());
        Map<String, Object> assessmentResult = new HashMap<>();
        boolean needsClinicalMode = true; // 默认进入临床诊疗态
        String workMode = "clinical_mode"; // 默认工作态
        
        try {
            Object assessmentResponse = healthStateAssessmentClient.assessHealthState(assessmentRequest);
            log.info("健康状态判定完成: cdpId={}", cdp.getId());
            
            // 解析健康状态判定结果
            assessmentResult = parseAssessmentResponse(assessmentResponse);
            workMode = (String) assessmentResult.getOrDefault("workMode", "clinical_mode");
            needsClinicalMode = (Boolean) assessmentResult.getOrDefault("needsClinicalMode", true);
        } catch (Exception e) {
            log.warn("健康状态判定服务不可用，使用默认值继续流程: cdpId={}, error={}", cdp.getId(), e.getMessage());
            // 服务不可用时，使用默认值
            assessmentResult.put("workMode", "clinical_mode");
            assessmentResult.put("needsClinicalMode", true);
            assessmentResult.put("riskLevel", "L4");
            assessmentResult.put("assessmentReason", "健康状态判定服务不可用，默认进入临床诊疗态");
            needsClinicalMode = true;
            workMode = "clinical_mode";
        }
        
        // 4. 更新CDP的健康状态判定结果和patientState
        Map<String, Object> updates = new HashMap<>();
        updates.put("healthStateAssessment", assessmentResult);
        
        // 将用户输入等信息存储到patientState中，供后续步骤使用（特别是A2步骤需要这些信息计算完整度）
        Map<String, Object> patientState = cdp.getPatientState();
        if (patientState == null) {
            patientState = new HashMap<>();
        }
        
        // 从请求中提取用户输入等信息
        String userInput = request.getUserInput();
        if (userInput == null || userInput.isEmpty()) {
            if (request.getSymptomInfo() != null) {
                userInput = request.getSymptomInfo().getChiefComplaint();
            }
        }
        if (userInput != null && !userInput.isEmpty()) {
            patientState.put("userInput", userInput);
        }
        
        // 存储基本信息、症状、生命体征
        if (request.getBasicInfo() != null && !request.getBasicInfo().isEmpty()) {
            patientState.put("basicInfo", request.getBasicInfo());
        }
        if (request.getSymptoms() != null && !request.getSymptoms().isEmpty()) {
            patientState.put("symptoms", request.getSymptoms());
        }
        if (request.getVitalSigns() != null && !request.getVitalSigns().isEmpty()) {
            patientState.put("vitalSigns", request.getVitalSigns());
        }
        
        updates.put("patientState", patientState);
        
        if (needsClinicalMode) {
            updates.put("cdpStatus", "clinical_mode_collecting");
        } else {
            updates.put("cdpStatus", "wellness_mode");
        }
        
        cdp = cdpManager.updateCDP(cdp.getId(), updates);
        
        // 6. 根据工作态决定后续流程
        // TraceContext 已在调用健康判定服务前设置
        DiagnosisResponse response;
        try {
            if (needsClinicalMode) {
                // 临床诊疗态：返回CDP ID，等待用户继续
                // 提取健康状态判定相关信息
                String assessmentReason = (String) assessmentResult.getOrDefault("assessmentReason", "建议进入临床诊疗态");
                String riskLevel = (String) assessmentResult.getOrDefault("riskLevel", "L4");
                @SuppressWarnings("unchecked")
                List<String> redFlags = (List<String>) assessmentResult.getOrDefault("redFlags", new ArrayList<>());
                @SuppressWarnings("unchecked")
                Map<String, Object> entryAssessment = (Map<String, Object>) assessmentResult.get("entryAssessment");
                
                // 提取patientState摘要
                Map<String, Object> patientStateSummary = extractPatientStateSummary(cdp);
                
                // 计算初始completeness（如果patientState中有completeness则使用，否则默认为0）
                Double initialCompleteness = 0.0;
                if (cdp.getPatientState() != null) {
                    Object completenessObj = cdp.getPatientState().get("completeness");
                    if (completenessObj != null && completenessObj instanceof Number) {
                        initialCompleteness = ((Number) completenessObj).doubleValue();
                    }
                }
                
                response = DiagnosisResponse.builder()
                    .code(200)
                    .message("success")
                    .diagnosisId(cdp.getId())
                    .status("clinical_mode_collecting")
                    .cdpId(cdp.getId())
                    .workMode("clinical_mode")
                    .currentStep("step1_identify_problem")
                    .completeness(initialCompleteness)
                    .nextAction(buildNextAction("question", "请详细描述一下您的症状"))
                    .assessmentReason(assessmentReason)
                    .riskLevel(riskLevel)
                    .redFlags(redFlags)
                    .entryAssessment(entryAssessment)
                    .patientState(patientStateSummary.isEmpty() ? null : patientStateSummary)
                    .timestamp(System.currentTimeMillis())
                    .build();
            } else {
                // 健康管理态：执行健康筛查流程
                cdp = wellnessScreeningOrchestrator.executeWellnessScreening(cdp);
                
                // 从wellnessPlan中提取完整度（A2步骤计算并存储的）
                Double completeness = null;
                if (cdp.getWellnessPlan() != null) {
                    Object completenessObj = cdp.getWellnessPlan().get("completeness");
                    if (completenessObj != null) {
                        // wellnessPlan中存储的是0-1的浮点数，需要转换为0-100
                        if (completenessObj instanceof Number) {
                            double completenessValue = ((Number) completenessObj).doubleValue();
                            completeness = completenessValue * 100; // 转换为0-100
                            log.debug("从wellnessPlan提取完整度: {} -> {}", completenessValue, completeness);
                        }
                    }
                }
                
                // 提取健康状态判定相关信息
                String assessmentReason = (String) assessmentResult.getOrDefault("assessmentReason", "症状在正常范围，建议健康管理");
                String riskLevel = (String) assessmentResult.getOrDefault("riskLevel", "L4");
                @SuppressWarnings("unchecked")
                List<String> redFlags = (List<String>) assessmentResult.getOrDefault("redFlags", new ArrayList<>());
                @SuppressWarnings("unchecked")
                Map<String, Object> entryAssessment = (Map<String, Object>) assessmentResult.get("entryAssessment");
                
                response = DiagnosisResponse.builder()
                    .code(200)
                    .message("success")
                    .diagnosisId(cdp.getId())
                    .status("wellness_mode")
                    .cdpId(cdp.getId())
                    .workMode("wellness_mode")
                    .completeness(completeness) // 设置完整度
                    .wellnessPlan(cdp.getWellnessPlan())
                    .assessmentReason(assessmentReason)
                    .riskLevel(riskLevel)
                    .redFlags(redFlags)
                    .entryAssessment(entryAssessment)
                    .timestamp(System.currentTimeMillis())
                    .build();
            }
        } catch (Exception e) {
            log.error("诊断流程执行失败: cdpId={}", cdp.getId(), e);
            throw new RuntimeException("诊断流程执行失败", e);
        } finally {
            // 清理追踪上下文
            TraceContext.clear();
        }
        
        return response;
    }
    
    /**
     * 继续诊断流程（提供用户回答）
     * 
     * @param answer 用户回答
     * @return 诊断响应
     */
    @Transactional
    public DiagnosisResponse continueDiagnosis(UserAnswer answer) {
        // 兼容旧字段：优先使用cdpId，如果没有则使用已过时的diagnosisId
        String cdpId = answer.getCdpId();
        if (cdpId == null || cdpId.isEmpty()) {
            // 使用已过时的diagnosisId字段（如果存在）
            @SuppressWarnings("deprecation")
            String deprecatedId = answer.getDiagnosisId();
            cdpId = deprecatedId;
        }
        if (cdpId == null || cdpId.isEmpty()) {
            throw new IllegalArgumentException("CDP ID不能为空");
        }
        log.info("继续诊断流程: cdpId={}", cdpId);
        
        // 设置追踪上下文（在调用前设置，以便切面能够获取到 cdpId）
        TraceContext.setCdpId(cdpId);
        
        try {
            // 1. 获取CDP
            Optional<CDP> cdpOpt = cdpManager.getCDPById(cdpId);
            if (!cdpOpt.isPresent()) {
                throw new CDPNotFoundException(cdpId);
            }
            
            CDP cdp = cdpOpt.get();
        
        // 2. 检查CDP状态
        String status = cdp.getCdpStatus();
        if ("completed".equals(status)) {
            return DiagnosisResponse.builder()
                .code(200)
                .message("诊断流程已完成")
                .diagnosisId(cdp.getId())
                .status("completed")
                .cdpId(cdp.getId())
                .timestamp(System.currentTimeMillis())
                .build();
        }
        
        // 3. 更新CDP中的用户回答
        Map<String, Object> patientState = cdp.getPatientState();
        if (patientState == null) {
            patientState = new HashMap<>();
        }
        
        // 将用户回答添加到patientState
        Map<String, Object> userAnswers = (Map<String, Object>) patientState.getOrDefault("userAnswers", new HashMap<>());
        String questionId = answer.getQuestionId();
        if (questionId == null || questionId.isEmpty()) {
            questionId = "default";
        }
        userAnswers.put(questionId, answer.getAnswer());
        patientState.put("userAnswers", userAnswers);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("patientState", patientState);
        cdp = cdpManager.updateCDP(cdp.getId(), updates);
        
        // 4. 根据当前步骤继续执行流程
        String currentStep = getCurrentStep(cdp);
        
        if ("step1_identify_problem".equals(currentStep) || "clinical_mode_collecting".equals(status)) {
            // 只执行Step 1（识别问题），不要一次性执行完所有步骤
            try {
                // 调用Step 1：识别问题并生成追问
                cdp = diagnosisWorkflowOrchestrator.step1IdentifyProblem(cdp);
                
                // 重新从数据库加载CDP，确保获取最新的patientState
                final String currentCdpId = cdp.getId();
                cdp = cdpManager.getCDPById(currentCdpId)
                    .orElseThrow(() -> new CDPNotFoundException("CDP not found: " + currentCdpId));
                
                // 从patientState中获取生成的问题
                String nextQuestion = null;
                if (cdp.getPatientState() != null) {
                    nextQuestion = (String) cdp.getPatientState().get("nextQuestion");
                }
                
                log.debug("Step 1完成后的nextQuestion: {}", nextQuestion);
                
                // 从patientState中提取completeness和已收集信息
                Double completeness = null;
                Map<String, Object> patientStateSummary = new HashMap<>();
                if (cdp.getPatientState() != null) {
                    Map<String, Object> currentPatientState = cdp.getPatientState();
                    
                    // 提取completeness
                    Object completenessObj = currentPatientState.get("completeness");
                    if (completenessObj != null) {
                        if (completenessObj instanceof Number) {
                            completeness = ((Number) completenessObj).doubleValue();
                        }
                    }
                    
                    // 提取已收集信息摘要（用于前端显示）
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> symptoms = (List<Map<String, Object>>) currentPatientState.get("symptoms");
                    if (symptoms != null && !symptoms.isEmpty()) {
                        Map<String, Object> firstSymptom = symptoms.get(0);
                        if (firstSymptom != null) {
                            patientStateSummary.put("symptoms", symptoms);
                            // 提取主诉
                            Object symptomName = firstSymptom.get("name");
                            if (symptomName != null) {
                                patientStateSummary.put("chiefComplaint", symptomName.toString());
                            }
                            // 提取其他字段
                            patientStateSummary.put("duration", firstSymptom.get("duration"));
                            patientStateSummary.put("severity", firstSymptom.get("severity"));
                            patientStateSummary.put("location", firstSymptom.get("location"));
                            patientStateSummary.put("frequency", firstSymptom.get("frequency"));
                        }
                    }
                    
                    // 如果没有从symptoms中提取到主诉，尝试从其他字段获取
                    if (!patientStateSummary.containsKey("chiefComplaint")) {
                        Object userInput = currentPatientState.get("userInput");
                        if (userInput != null) {
                            patientStateSummary.put("chiefComplaint", userInput.toString());
                        }
                    }
                    
                    // 提取structuredData（如果存在）
                    Object structuredData = currentPatientState.get("structuredData");
                    if (structuredData != null) {
                        patientStateSummary.put("structuredData", structuredData);
                    }
                }
                
                // 判断是否继续：基于完整度（>=60%）而不是基于是否有问题
                // 如果完整度达到60%（最低要求），可以继续执行后续步骤
                // 如果完整度 < 60% 或没有完整度信息，返回问题等待用户继续输入
                if (completeness != null && completeness >= 60.0) {
                    // 信息完整度达到最低要求（60%），继续执行后续步骤
                    log.info("信息完整度达到最低要求（{}%），继续执行后续步骤", completeness);
                    cdp = diagnosisWorkflowOrchestrator.executeRemainingSteps(cdp);
                    
                    return DiagnosisResponse.builder()
                        .code(200)
                        .message("诊断流程执行完成")
                        .diagnosisId(cdp.getId())
                        .status("completed")
                        .cdpId(cdp.getId())
                        .workMode("clinical_mode")
                        .completeness(completeness) // 使用实际完整度，而不是100%
                        .ddx(cdp.getDdx())
                        .workupPlan(cdp.getWorkupPlan())
                        .managementPlan(cdp.getManagementPlan())
                        .triage(cdp.getTriage())
                        .patientState(patientStateSummary.isEmpty() ? null : patientStateSummary)
                        .timestamp(System.currentTimeMillis())
                        .build();
                } else {
                    // 信息完整度不足（<60%），返回问题等待用户继续输入
                    log.info("信息完整度不足（{}%），返回问题等待用户继续输入: nextQuestion={}", 
                        completeness != null ? completeness : "未知", nextQuestion);
                    return DiagnosisResponse.builder()
                        .code(200)
                        .message("请继续提供信息")
                        .diagnosisId(cdp.getId())
                        .status("clinical_mode_collecting")
                        .cdpId(cdp.getId())
                        .workMode("clinical_mode")
                        .currentStep("step1_identify_problem")
                        .completeness(completeness != null ? completeness : 0.0)
                        .nextAction(buildNextAction("question", nextQuestion != null ? nextQuestion : "请详细描述一下您的症状"))
                        .patientState(patientStateSummary.isEmpty() ? null : patientStateSummary)
                        .timestamp(System.currentTimeMillis())
                        .build();
                }
            } catch (Exception e) {
                log.error("诊断流程执行失败: cdpId={}", cdp.getId(), e);
                throw new RuntimeException("诊断流程执行失败", e);
            }
        } else {
            // 其他步骤的处理
            return DiagnosisResponse.builder()
                .code(200)
                .message("处理中")
                .diagnosisId(cdp.getId())
                .status(status)
                .cdpId(cdp.getId())
                .currentStep(currentStep)
                .timestamp(System.currentTimeMillis())
                .build();
        }
        } finally {
            // 清理追踪上下文
            TraceContext.clear();
        }
    }
    
    /**
     * 获取诊断状态
     * 
     * @param cdpId CDP ID
     * @return 诊断响应
     */
    public DiagnosisResponse getDiagnosisStatus(String cdpId) {
        Optional<CDP> cdpOpt = cdpManager.getCDPById(cdpId);
        if (!cdpOpt.isPresent()) {
            throw new CDPNotFoundException(cdpId);
        }
        
        CDP cdp = cdpOpt.get();
        
        return DiagnosisResponse.builder()
            .code(200)
            .message("success")
            .diagnosisId(cdpId)
            .status(cdp.getCdpStatus())
            .cdpId(cdpId)
            .workMode(getWorkMode(cdp))
            .currentStep(getCurrentStep(cdp))
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 获取诊断结果
     * 
     * @param cdpId CDP ID
     * @return 诊断响应
     */
    public DiagnosisResponse getDiagnosisResult(String cdpId) {
        Optional<CDP> cdpOpt = cdpManager.getCDPById(cdpId);
        if (!cdpOpt.isPresent()) {
            throw new CDPNotFoundException(cdpId);
        }
        
        CDP cdp = cdpOpt.get();
        
        if (!"completed".equals(cdp.getCdpStatus())) {
            throw new RuntimeException("诊断流程尚未完成");
        }
        
        // 优先尝试从CDP中读取explanation-service生成的结论包
        ConclusionPackage conclusionPackage = extractConclusionPackageFromCDP(cdp);
        
        // 构建诊断结果
        DiagnosisResult.DiagnosisResultBuilder resultBuilder = DiagnosisResult.builder();
        boolean hasSummary = false;
        
        // 如果有explanation-service生成的完整结论包，优先使用它
        if (conclusionPackage != null) {
            log.info("使用explanation-service生成的完整结论包: cdpId={}", cdpId);
            resultBuilder.conclusionPackage(conclusionPackage);
            
            // 从结论包中提取摘要
            if (conclusionPackage.getConclusion() != null) {
                Conclusion conclusion = conclusionPackage.getConclusion();
                String summary = buildSummaryFromConclusion(conclusion, conclusionPackage);
                resultBuilder.summary(summary);
                hasSummary = true;
            }
        } else {
            // 降级方案：从CDP中提取基本信息构建简化的诊断结果
            log.info("explanation-service结论包不存在，使用降级方案从CDP提取基本信息: cdpId={}", cdpId);
            buildSimplifiedResultFromCDP(cdp, resultBuilder);
            hasSummary = true; // buildSimplifiedResultFromCDP已经设置了summary
        }
        
        // 补充可能性列表、检查建议、就医建议等基本信息
        // 这些信息在两种情况下都需要（即使有完整结论包，也需要用于兼容性）
        List<DiagnosisResult.DiseasePossibility> possibilities = new ArrayList<>();
        if (cdp.getDdx() != null && !cdp.getDdx().isEmpty()) {
            for (Map<String, Object> ddxItem : cdp.getDdx()) {
                DiagnosisResult.DiseasePossibility possibility = DiagnosisResult.DiseasePossibility.builder()
                    .disease((String) ddxItem.getOrDefault("disease", ddxItem.getOrDefault("name", "未知疾病")))
                    .confidence(getConfidenceFromDDx(ddxItem))
                    .level(getLevelFromConfidence(getConfidenceFromDDx(ddxItem)))
                    .supportingEvidence(extractList(ddxItem, "supportingEvidence"))
                    .opposingEvidence(extractList(ddxItem, "opposingEvidence"))
                    .missingInfo(extractList(ddxItem, "missingInfo"))
                    .build();
                possibilities.add(possibility);
            }
        }
        resultBuilder.possibilities(possibilities);
        
        // 从workupPlan构建检查建议
        if (cdp.getWorkupPlan() != null && !cdp.getWorkupPlan().isEmpty()) {
            List<DiagnosisResult.ExaminationItem> priorityExaminations = new ArrayList<>();
            List<DiagnosisResult.ExaminationItem> optionalExaminations = new ArrayList<>();
            
            for (Map<String, Object> workupItem : cdp.getWorkupPlan()) {
                DiagnosisResult.ExaminationItem item = DiagnosisResult.ExaminationItem.builder()
                    .name((String) workupItem.getOrDefault("name", workupItem.getOrDefault("examination", "未知检查")))
                    .purpose((String) workupItem.getOrDefault("purpose", workupItem.getOrDefault("reason", "")))
                    .priority((String) workupItem.getOrDefault("priority", "optional"))
                    .reason((String) workupItem.getOrDefault("reason", ""))
                    .build();
                
                if ("priority".equals(item.getPriority()) || "high".equals(item.getPriority())) {
                    priorityExaminations.add(item);
                } else {
                    optionalExaminations.add(item);
                }
            }
            
            DiagnosisResult.ExaminationSuggestion examinationSuggestion = DiagnosisResult.ExaminationSuggestion.builder()
                .priorityExaminations(priorityExaminations)
                .optionalExaminations(optionalExaminations)
                .explanation("根据您的症状，建议进行以下检查以明确诊断")
                .build();
            resultBuilder.examinationSuggestion(examinationSuggestion);
        }
        
        // 从managementPlan构建就医建议
        if (cdp.getManagementPlan() != null && !cdp.getManagementPlan().isEmpty()) {
            Map<String, Object> firstPlan = cdp.getManagementPlan().get(0);
            DiagnosisResult.MedicalAdvice medicalAdvice = DiagnosisResult.MedicalAdvice.builder()
                .department((String) firstPlan.getOrDefault("department", "内科"))
                .timing((String) firstPlan.getOrDefault("timing", "建议尽快就医"))
                .sbarSummary((String) firstPlan.getOrDefault("summary", "建议就医进一步检查"))
                .build();
            resultBuilder.medicalAdvice(medicalAdvice);
        }
        
        // 如果没有从结论包中提取摘要，则从CDP构建摘要
        if (!hasSummary) {
            String summary = buildSummary(cdp, possibilities);
            resultBuilder.summary(summary);
        }
        
        DiagnosisResult result = resultBuilder.build();
        
        return DiagnosisResponse.builder()
            .code(200)
            .message("success")
            .diagnosisId(cdpId)
            .status("completed")
            .cdpId(cdpId)
            .result(result)
            .ddx(cdp.getDdx())
            .workupPlan(cdp.getWorkupPlan())
            .managementPlan(cdp.getManagementPlan())
            .triage(cdp.getTriage())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 从DDx项中提取置信度
     */
    private Double getConfidenceFromDDx(Map<String, Object> ddxItem) {
        Object confidenceObj = ddxItem.get("confidence");
        if (confidenceObj instanceof Number) {
            return ((Number) confidenceObj).doubleValue();
        } else if (confidenceObj instanceof String) {
            try {
                return Double.parseDouble((String) confidenceObj);
            } catch (NumberFormatException e) {
                return 0.5; // 默认值
            }
        }
        return 0.5; // 默认值
    }
    
    /**
     * 根据置信度确定级别
     */
    private String getLevelFromConfidence(Double confidence) {
        if (confidence == null) return "medium";
        if (confidence >= 0.7) return "high";
        if (confidence >= 0.4) return "medium";
        return "low";
    }
    
    /**
     * 从Map中提取List字段
     */
    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
    
    /**
     * 构建诊断摘要
     */
    private String buildSummary(CDP cdp, List<DiagnosisResult.DiseasePossibility> possibilities) {
        if (possibilities == null || possibilities.isEmpty()) {
            return "根据您的症状描述，建议进一步检查以明确诊断。";
        }
        
        StringBuilder summary = new StringBuilder("根据您的症状和健康档案，我考虑以下几个可能的方向：\n\n");
        
        for (int i = 0; i < Math.min(possibilities.size(), 3); i++) {
            DiagnosisResult.DiseasePossibility possibility = possibilities.get(i);
            summary.append(String.format("%d. %s（可能性：%.0f%%）\n", 
                i + 1, 
                possibility.getDisease(), 
                (possibility.getConfidence() != null ? possibility.getConfidence() * 100 : 0)));
        }
        
        summary.append("\n建议进行进一步检查以明确诊断。");
        return summary.toString();
    }
    
    // ========== 辅助方法 ==========
    
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private Map<String, Object> buildAssessmentRequest(DiagnosisRequest request, String cdpId) {
        Map<String, Object> assessmentRequest = new HashMap<>();
        assessmentRequest.put("userId", request.getUserId());
        assessmentRequest.put("cdpId", cdpId);  // 传入已创建的CDP ID
        
        // 优先使用直接传递的userInput（从前端传递）
        String userInput = request.getUserInput();
        
        // 如果没有直接传递userInput，从symptomInfo中提取
        if (userInput == null || userInput.isEmpty()) {
            if (request.getSymptomInfo() != null) {
                String chiefComplaint = request.getSymptomInfo().getChiefComplaint();
                if (chiefComplaint != null && !chiefComplaint.isEmpty()) {
                    userInput = chiefComplaint;
                }
            }
        }
        
        // 设置userInput（健康状态判定服务需要）
        if (userInput != null && !userInput.isEmpty()) {
            assessmentRequest.put("userInput", userInput);
        }
        
        // 处理symptoms列表
        // 优先使用直接传递的symptoms（从前端传递）
        List<String> symptoms = request.getSymptoms();
        
        // 如果没有直接传递symptoms，从symptomInfo中提取
        // 注意：不要将健康管理类输入（如"我想做个体检"）作为症状传递
        if (symptoms == null || symptoms.isEmpty()) {
            symptoms = new ArrayList<>();
            if (request.getSymptomInfo() != null) {
                String chiefComplaint = request.getSymptomInfo().getChiefComplaint();
                // 只有当chiefComplaint包含明确的症状关键词时，才作为症状
                if (chiefComplaint != null && !chiefComplaint.isEmpty()) {
                    // 检查是否包含症状关键词（如"痛"、"疼"、"不适"等）
                    String lowerComplaint = chiefComplaint.toLowerCase();
                    boolean hasSymptomKeywords = lowerComplaint.contains("痛") || 
                                                 lowerComplaint.contains("疼") || 
                                                 lowerComplaint.contains("不适") ||
                                                 lowerComplaint.contains("难受") ||
                                                 lowerComplaint.contains("不舒服") ||
                                                 lowerComplaint.contains("发热") ||
                                                 lowerComplaint.contains("咳嗽") ||
                                                 lowerComplaint.contains("头痛") ||
                                                 lowerComplaint.contains("腹痛") ||
                                                 lowerComplaint.contains("胸痛");
                    
                    // 检查是否包含健康管理关键词（如"体检"、"筛查"等）
                    boolean hasWellnessKeywords = lowerComplaint.contains("体检") ||
                                                  lowerComplaint.contains("筛查") ||
                                                  lowerComplaint.contains("健康管理") ||
                                                  lowerComplaint.contains("预防") ||
                                                  lowerComplaint.contains("健康评估");
                    
                    // 如果有症状关键词且没有健康管理关键词，才作为症状
                    if (hasSymptomKeywords && !hasWellnessKeywords) {
                        symptoms.add(chiefComplaint);
                    }
                }
                
                // 如果有伴随症状，添加进去
                if (request.getSymptomInfo().getAccompanyingSymptoms() != null) {
                    symptoms.addAll(request.getSymptomInfo().getAccompanyingSymptoms());
                }
            }
        }
        
        // 只有当symptoms不为空时才传递（避免将健康管理需求误判为症状）
        if (symptoms != null && !symptoms.isEmpty()) {
            assessmentRequest.put("symptoms", symptoms);
        }
        
        // 传递其他字段
        if (request.getBasicInfo() != null) {
            assessmentRequest.put("basicInfo", request.getBasicInfo());
        }
        if (request.getVitalSigns() != null) {
            assessmentRequest.put("vitalSigns", request.getVitalSigns());
        }
        
        return assessmentRequest;
    }
    
    /**
     * 解析health-state-assessment-service的响应
     * 响应格式：{"code": 200, "message": "success", "data": {...}, "timestamp": ...}
     * data格式：{"cdpId": "...", "needsClinicalMode": true, "workMode": "clinical_mode", ...}
     */
    private Map<String, Object> parseAssessmentResponse(Object response) {
        if (response == null) {
            log.warn("健康状态判定响应为空");
            return new HashMap<>();
        }
        
        try {
            // 响应是Map类型
        if (response instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) response;
                
                // 检查响应码
                Object code = responseMap.get("code");
                if (code != null && !Integer.valueOf(200).equals(code)) {
                    log.warn("健康状态判定响应码异常: code={}", code);
                    // 即使响应码异常，也尝试提取data字段
                }
                
                // 提取data字段
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    
                    // 构建解析后的结果
                    Map<String, Object> parsedResult = new HashMap<>();
                    
                    // 提取关键字段
                    parsedResult.put("cdpId", dataMap.get("cdpId"));
                    parsedResult.put("workMode", dataMap.get("workMode"));
                    parsedResult.put("needsClinicalMode", dataMap.get("needsClinicalMode"));
                    parsedResult.put("riskLevel", dataMap.get("riskLevel"));
                    parsedResult.put("assessmentReason", dataMap.get("assessmentReason"));
                    parsedResult.put("redFlags", dataMap.get("redFlags"));
                    parsedResult.put("wellnessPlan", dataMap.get("wellnessPlan"));
                    
                    // 提取入口判定结果（entryAssessment）
                    Object entryAssessment = dataMap.get("entryAssessment");
                    if (entryAssessment != null) {
                        parsedResult.put("entryAssessment", entryAssessment);
        }
                    
                    log.debug("解析健康状态判定响应成功: workMode={}, needsClinicalMode={}", 
                        parsedResult.get("workMode"), parsedResult.get("needsClinicalMode"));
                    return parsedResult;
                } else {
                    log.warn("健康状态判定响应data字段不是Map类型: {}", data);
                    // 如果data不是Map，直接返回data
                    Map<String, Object> parsedResult = new HashMap<>();
                    parsedResult.put("assessmentResult", data);
                    return parsedResult;
                }
            } else {
                log.warn("健康状态判定响应不是Map类型: {}", response.getClass().getName());
            }
        } catch (Exception e) {
            log.error("解析健康状态判定响应失败", e);
        }
        
        return new HashMap<>();
    }
    
    private String getWorkMode(CDP cdp) {
        if (cdp.getHealthStateAssessment() != null) {
            return (String) cdp.getHealthStateAssessment().getOrDefault("workMode", "clinical_mode");
        }
        return "clinical_mode";
    }
    
    private String getCurrentStep(CDP cdp) {
        String status = cdp.getCdpStatus();
        if ("clinical_mode_collecting".equals(status)) {
            return "step1_identify_problem";
        } else if ("clinical_mode_diagnosing".equals(status)) {
            return "step2_build_ddx_candidates";
        } else if ("clinical_mode_managing".equals(status)) {
            return "step5_backfill_and_conclude";
        }
        return "unknown";
    }
    
    private Map<String, Object> buildNextAction(String type, String message) {
        Map<String, Object> nextAction = new HashMap<>();
        nextAction.put("type", type);
        if ("question".equals(type)) {
            nextAction.put("question", message);
        } else {
            nextAction.put("message", message);
        }
        return nextAction;
    }
    
    /**
     * 从CDP中提取patientState摘要（用于前端显示已收集信息）
     */
    private Map<String, Object> extractPatientStateSummary(CDP cdp) {
        Map<String, Object> summary = new HashMap<>();
        
        if (cdp.getPatientState() == null) {
            return summary;
        }
        
        Map<String, Object> patientState = cdp.getPatientState();
        
        // 提取症状信息
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> symptoms = (List<Map<String, Object>>) patientState.get("symptoms");
        if (symptoms != null && !symptoms.isEmpty()) {
            summary.put("symptoms", symptoms);
            
            // 从第一个症状中提取详细信息
            Map<String, Object> firstSymptom = symptoms.get(0);
            if (firstSymptom != null) {
                Object symptomName = firstSymptom.get("name");
                if (symptomName != null) {
                    summary.put("chiefComplaint", symptomName.toString());
                }
                summary.put("duration", firstSymptom.get("duration"));
                summary.put("severity", firstSymptom.get("severity"));
                summary.put("location", firstSymptom.get("location"));
                summary.put("frequency", firstSymptom.get("frequency"));
            }
        }
        
        // 如果没有从symptoms中提取到主诉，尝试从其他字段获取
        if (!summary.containsKey("chiefComplaint")) {
            Object userInput = patientState.get("userInput");
            if (userInput != null) {
                summary.put("chiefComplaint", userInput.toString());
            }
        }
        
        // 提取structuredData（如果存在）
        Object structuredData = patientState.get("structuredData");
        if (structuredData != null) {
            summary.put("structuredData", structuredData);
        }
        
        return summary;
    }
    
    /**
     * 从CDP中提取explanation-service生成的结论包
     * 检查patientState、audit等字段中是否有conclusion_package
     */
    @SuppressWarnings("unchecked")
    private ConclusionPackage extractConclusionPackageFromCDP(CDP cdp) {
        try {
            // 检查patientState中是否有conclusion_package
            Map<String, Object> patientState = cdp.getPatientState();
            if (patientState != null) {
                Object conclusionPackageObj = patientState.get("conclusion_package");
                if (conclusionPackageObj != null && conclusionPackageObj instanceof Map) {
                    return convertMapToConclusionPackage((Map<String, Object>) conclusionPackageObj);
                }
            }
            
            // 检查audit字段中是否有conclusion_package
            Map<String, Object> audit = cdp.getAudit();
            if (audit != null) {
                Object conclusionPackageObj = audit.get("conclusion_package");
                if (conclusionPackageObj != null && conclusionPackageObj instanceof Map) {
                    return convertMapToConclusionPackage((Map<String, Object>) conclusionPackageObj);
                }
            }
            
            return null;
        } catch (Exception e) {
            log.warn("提取结论包失败: cdpId={}", cdp.getId(), e);
            return null;
        }
    }
    
    /**
     * 将Map转换为ConclusionPackage对象
     */
    @SuppressWarnings("unchecked")
    private ConclusionPackage convertMapToConclusionPackage(Map<String, Object> map) {
        try {
            ConclusionPackage.ConclusionPackageBuilder builder = ConclusionPackage.builder();
            
            // 转换conclusion
            Object conclusionObj = map.get("conclusion");
            if (conclusionObj instanceof Map) {
                Map<String, Object> conclusionMap = (Map<String, Object>) conclusionObj;
                Conclusion.ConclusionBuilder conclusionBuilder = Conclusion.builder();
                Object typeObj = conclusionMap.get("type");
                if (typeObj != null) {
                    String typeStr = typeObj.toString().toUpperCase();
                    if ("CONFIRMED".equals(typeStr) || "可确证".equals(typeObj.toString())) {
                        conclusionBuilder.type(Conclusion.ConclusionType.CONFIRMED);
                    } else {
                        conclusionBuilder.type(Conclusion.ConclusionType.PROBABLE);
                    }
                }
                conclusionBuilder.diagnosis((String) conclusionMap.getOrDefault("diagnosis", conclusionMap.get("name")));
                conclusionBuilder.confidence(getDoubleValue(conclusionMap.get("confidence")));
                conclusionBuilder.uncertaintyReason((String) conclusionMap.get("uncertaintyReason"));
                conclusionBuilder.reviewWindow((String) conclusionMap.get("reviewWindow"));
                conclusionBuilder.upgradeTriggers(extractList(conclusionMap, "upgradeTriggers"));
                builder.conclusion(conclusionBuilder.build());
            }
            
            // 转换mustExcludeStatus
            Object mustExcludeObj = map.get("mustExcludeStatus");
            if (mustExcludeObj instanceof Map) {
                Map<String, Object> mustExcludeMap = (Map<String, Object>) mustExcludeObj;
                MustExcludeStatus.MustExcludeStatusBuilder mustExcludeBuilder = MustExcludeStatus.builder();
                Object statusObj = mustExcludeMap.get("status");
                if (statusObj != null) {
                    String statusStr = statusObj.toString().toUpperCase();
                    try {
                        mustExcludeBuilder.status(MustExcludeStatus.ExcludeStatus.valueOf(statusStr));
                    } catch (IllegalArgumentException e) {
                        mustExcludeBuilder.status(MustExcludeStatus.ExcludeStatus.NONE);
                    }
                }
                mustExcludeBuilder.excludeReason((String) mustExcludeMap.get("excludeReason"));
                builder.mustExcludeStatus(mustExcludeBuilder.build());
            }
            
            // 转换keyEvidence
            Object keyEvidenceObj = map.get("keyEvidence");
            if (keyEvidenceObj instanceof List) {
                List<Map<String, Object>> keyEvidenceList = (List<Map<String, Object>>) keyEvidenceObj;
                List<KeyEvidence> keyEvidence = new ArrayList<>();
                for (Map<String, Object> evidenceMap : keyEvidenceList) {
                    KeyEvidence evidence = KeyEvidence.builder()
                        .item((String) evidenceMap.getOrDefault("item", evidenceMap.get("description")))
                        .type((String) evidenceMap.get("type"))
                        .strength((String) evidenceMap.get("strength"))
                        .role((String) evidenceMap.get("role"))
                        .build();
                    keyEvidence.add(evidence);
                }
                builder.keyEvidence(keyEvidence);
            }
            
            // 转换actionAndFollowUp
            Object actionObj = map.get("actionAndFollowUp");
            if (actionObj instanceof Map) {
                Map<String, Object> actionMap = (Map<String, Object>) actionObj;
                ActionAndFollowUp.ActionAndFollowUpBuilder actionBuilder = ActionAndFollowUp.builder();
                actionBuilder.reviewWindow((String) actionMap.get("reviewWindow"));
                actionBuilder.upgradeTriggers(extractList(actionMap, "upgradeTriggers"));
                
                Object immediateActionObj = actionMap.get("immediateAction");
                if (immediateActionObj instanceof Map) {
                    Map<String, Object> immediateActionMap = (Map<String, Object>) immediateActionObj;
                    List<ActionAndFollowUp.Action> actions = new ArrayList<>();
                    Object examinationsObj = immediateActionMap.get("examinations");
                    if (examinationsObj instanceof List) {
                        for (Object examObj : (List<?>) examinationsObj) {
                            ActionAndFollowUp.Action action = ActionAndFollowUp.Action.builder()
                                .type("examination")
                                .name(examObj.toString())
                                .priority("high")
                                .build();
                            actions.add(action);
                        }
                    }
                    actionBuilder.immediateActions(actions);
                }
                builder.actionAndFollowUp(actionBuilder.build());
            }
            
            return builder.build();
        } catch (Exception e) {
            log.warn("转换结论包失败", e);
            return null;
        }
    }
    
    /**
     * 从结论包构建摘要
     */
    private String buildSummaryFromConclusion(Conclusion conclusion, ConclusionPackage conclusionPackage) {
        StringBuilder summary = new StringBuilder();
        
        if (conclusion.getDiagnosis() != null) {
            summary.append("根据您的症状和健康档案，我考虑最可能的诊断是：").append(conclusion.getDiagnosis());
            if (conclusion.getConfidence() != null) {
                summary.append(String.format("（可能性：%.0f%%）", conclusion.getConfidence() * 100));
            }
            summary.append("。\n\n");
        }
        
        if (conclusion.getType() == Conclusion.ConclusionType.PROBABLE && conclusion.getUncertaintyReason() != null) {
            summary.append("不确定性来源：").append(conclusion.getUncertaintyReason()).append("。\n\n");
        }
        
        if (conclusionPackage.getKeyEvidence() != null && !conclusionPackage.getKeyEvidence().isEmpty()) {
            summary.append("关键依据：\n");
            for (int i = 0; i < Math.min(conclusionPackage.getKeyEvidence().size(), 3); i++) {
                KeyEvidence evidence = conclusionPackage.getKeyEvidence().get(i);
                summary.append(String.format("%d. %s\n", i + 1, evidence.getItem()));
            }
            summary.append("\n");
        }
        
        if (conclusionPackage.getActionAndFollowUp() != null) {
            ActionAndFollowUp action = conclusionPackage.getActionAndFollowUp();
            if (action.getReviewWindow() != null) {
                summary.append("建议在").append(action.getReviewWindow()).append("内复评。");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * 从CDP构建简化的诊断结果（降级方案）
     */
    private void buildSimplifiedResultFromCDP(CDP cdp, DiagnosisResult.DiagnosisResultBuilder resultBuilder) {
        // 从ddx构建可能性列表
        List<DiagnosisResult.DiseasePossibility> possibilities = new ArrayList<>();
        if (cdp.getDdx() != null && !cdp.getDdx().isEmpty()) {
            for (Map<String, Object> ddxItem : cdp.getDdx()) {
                DiagnosisResult.DiseasePossibility possibility = DiagnosisResult.DiseasePossibility.builder()
                    .disease((String) ddxItem.getOrDefault("disease", ddxItem.getOrDefault("name", "未知疾病")))
                    .confidence(getConfidenceFromDDx(ddxItem))
                    .level(getLevelFromConfidence(getConfidenceFromDDx(ddxItem)))
                    .supportingEvidence(extractList(ddxItem, "supportingEvidence"))
                    .opposingEvidence(extractList(ddxItem, "opposingEvidence"))
                    .missingInfo(extractList(ddxItem, "missingInfo"))
                    .build();
                possibilities.add(possibility);
            }
        }
        resultBuilder.possibilities(possibilities);
        
        // 构建摘要
        String summary = buildSummary(cdp, possibilities);
        resultBuilder.summary(summary);
    }
    
    /**
     * 获取Double值
     */
    private Double getDoubleValue(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

