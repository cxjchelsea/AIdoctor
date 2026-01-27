package com.aidoctor.diagnosis.service.orchestration;

import com.aidoctor.diagnosis.annotation.TraceExecution;
import com.aidoctor.diagnosis.client.*;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import com.aidoctor.diagnosis.util.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 诊断流程编排服务
 * 负责编排5步AI循证诊断流程
 * 
 * 参考文档：
 * - 《AI医生系统-业务逻辑详细设计.md》第1.11节
 * - 《AI医生系统-技术架构设计.md》第3.3节
 */
@Slf4j
@Service
@SuppressWarnings("unchecked")
public class DiagnosisWorkflowOrchestrator {
    
    @Autowired
    private CDPManager cdpManager;
    
    @Autowired
    private ClinicalParsingClient clinicalParsingClient;
    
    @Autowired
    private DialogServiceClient dialogServiceClient;
    
    @Autowired
    private DiagnosisEngineClient diagnosisEngineClient;
    
    @Autowired
    private WorkupPlannerClient workupPlannerClient;
    
    @Autowired
    private TreatmentEngineClient treatmentEngineClient;
    
    @Autowired
    private RiskAssessmentClient riskAssessmentClient;
    
    @Autowired
    private ExplanationServiceClient explanationServiceClient;
    
    /**
     * 执行5步AI循证诊断流程
     * 
     * @param cdp CDP对象
     * @return 更新后的CDP
     */
    @TraceExecution(service = "diagnosis-service", module = "orchestration")
    @Transactional
    public CDP executeDiagnosisWorkflow(CDP cdp) {
        // 设置追踪上下文
        TraceContext.setCdpId(cdp.getId());
        
        log.info("开始执行5步AI循证诊断流程: cdpId={}", cdp.getId());
        
        try {
            // Step 1: 识别问题
            cdp = step1IdentifyProblem(cdp);
            
            // Step 2: 构建鉴别诊断候选集并分层
            cdp = step2BuildDDxCandidates(cdp);
            
            // Step 3: 组织候选集并建立分流路径
            cdp = step3OrganizeRoutingPath(cdp);
            
            // Step 4: 采集关键证据并形成排序与验证计划
            cdp = step4CollectEvidenceAndPlan(cdp);
            
            // Step 5: 回填证据并输出终点结论包
            cdp = step5BackfillAndConclude(cdp);
            
            log.info("5步AI循证诊断流程执行完成: cdpId={}", cdp.getId());
            return cdp;
            
        } catch (Exception e) {
            log.error("诊断流程执行失败: cdpId={}", cdp.getId(), e);
            // 更新CDP状态为错误
            Map<String, Object> updates = new HashMap<>();
            updates.put("cdpStatus", "error");
            cdp = cdpManager.updateCDP(cdp.getId(), updates);
            throw e;
        } finally {
            // 清理追踪上下文
            TraceContext.clear();
        }
    }
    
    /**
     * 执行剩余步骤（Step 2-5）
     * 当Step 1信息收集完整后执行
     */
    @Transactional
    public CDP executeRemainingSteps(CDP cdp) {
        log.info("执行剩余诊断步骤: cdpId={}", cdp.getId());
        
        try {
            // Step 2: 构建鉴别诊断候选集并分层
            cdp = step2BuildDDxCandidates(cdp);
            
            // Step 3: 组织候选集并建立分流路径
            cdp = step3OrganizeRoutingPath(cdp);
            
            // Step 4: 采集关键证据并形成排序与验证计划
            cdp = step4CollectEvidenceAndPlan(cdp);
            
            // Step 5: 回填证据并输出终点结论包
            cdp = step5BackfillAndConclude(cdp);
            
            log.info("剩余诊断步骤执行完成: cdpId={}", cdp.getId());
            return cdp;
            
        } catch (Exception e) {
            log.error("剩余诊断步骤执行失败: cdpId={}", cdp.getId(), e);
            Map<String, Object> updates = new HashMap<>();
            updates.put("cdpStatus", "error");
            cdp = cdpManager.updateCDP(cdp.getId(), updates);
            throw e;
        }
    }
    
    /**
     * Step 1: 识别问题
     * 调用脑区A（病例理解）和脑区B（主动问诊）
     */
    @TraceExecution(service = "diagnosis-service", module = "orchestration")
    @Transactional
    public CDP step1IdentifyProblem(CDP cdp) {
        log.info("Step 1: 识别问题 - cdpId={}", cdp.getId());
        
        // 更新状态
        Map<String, Object> updates = new HashMap<>();
        updates.put("cdpStatus", "clinical_mode_collecting");
        
        // 调用clinical-parsing-service进行概念归一化和结构化提取
        Map<String, Object> patientState = cdp.getPatientState();
        if (patientState == null) {
            patientState = new HashMap<>();
        }
        
        // 构建clinical-parsing-service请求（需要userId, sessionId, text等必需字段）
        Map<String, Object> parsingRequest = new HashMap<>();
        parsingRequest.put("userId", cdp.getPatientId());
        parsingRequest.put("sessionId", cdp.getSessionId());
        parsingRequest.put("cdpId", cdp.getId());
        
        // 提取用户输入的文本（从userAnswers或healthStateAssessment中获取）
        String text = extractUserInputText(cdp, patientState);
        log.debug("提取的用户输入文本: text={}, cdpId={}", text, cdp.getId());
        parsingRequest.put("text", text != null ? text : "");
        
        // 将patientState作为input传递（可选字段）
        parsingRequest.put("input", patientState);
        
        try {
            Object parsingResponse = clinicalParsingClient.parse(parsingRequest);
            log.debug("clinical-parsing-service响应: {}", parsingResponse);
            
            // 解析响应并更新patientState
            Map<String, Object> parsedState = parseParsingResponse(parsingResponse);
            if (parsedState != null && !parsedState.isEmpty()) {
                log.info("clinical-parsing-service解析成功，更新patientState: {}", parsedState);
                patientState.putAll(parsedState);
            } else {
                log.warn("clinical-parsing-service响应解析为空，使用原始patientState");
            }
        } catch (Exception e) {
            log.warn("调用clinical-parsing-service失败，继续使用原始patientState", e);
            // 服务调用失败不影响流程继续，使用原始patientState
        }
        
        updates.put("patientState", patientState);
        
        // 调用dialog-service识别信息缺口并生成问题
        String nextQuestion = null;
        try {
            // 先识别信息缺口
            Map<String, Object> dialogRequest = new HashMap<>();
            dialogRequest.put("cdpId", cdp.getId());
            dialogRequest.put("patientState", updates.get("patientState"));
            
            Object dialogResponse = dialogServiceClient.identifyGaps(dialogRequest);
            // 如果有信息缺口，生成问题
            if (dialogResponse != null) {
                // 调用生成问题接口
                com.aidoctor.diagnosis.dto.dialog.QuestionRequest questionRequest = 
                    com.aidoctor.diagnosis.dto.dialog.QuestionRequest.builder()
                        .diagnosisId(cdp.getId())
                        .context((Map<String, Object>) updates.get("patientState"))
                        .build();
                
                try {
                    com.aidoctor.diagnosis.dto.dialog.QuestionResponse questionResponse = 
                        dialogServiceClient.generateQuestion(questionRequest);
                    if (questionResponse != null && questionResponse.getQuestion() != null) {
                        nextQuestion = questionResponse.getQuestion();
                        // 将问题保存到patientState中（重用已定义的patientState变量）
                        patientState.put("nextQuestion", nextQuestion);
                        patientState.put("questionType", questionResponse.getQuestionType());
                    }
                } catch (Exception e) {
                    log.warn("生成问题失败，使用默认问题", e);
                    nextQuestion = "请详细描述一下您的症状";
                }
            }
        } catch (Exception e) {
            log.error("调用dialog-service失败，使用默认问题", e);
            nextQuestion = "请详细描述一下您的症状";
        }
        
        // 如果没有生成问题，使用默认问题
        if (nextQuestion == null) {
            nextQuestion = "请详细描述一下您的症状";
            patientState.put("nextQuestion", nextQuestion);
        }
        
        cdp = cdpManager.updateCDP(cdp.getId(), updates);
        log.info("Step 1完成: cdpId={}, nextQuestion={}", cdp.getId(), nextQuestion);
        return cdp;
    }
    
    /**
     * Step 2: 构建鉴别诊断候选集并分层
     * 调用脑区C（鉴别诊断）和脑区F（风险评估）
     */
    private CDP step2BuildDDxCandidates(CDP cdp) {
        log.info("Step 2: 构建鉴别诊断候选集并分层 - cdpId={}", cdp.getId());
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("cdpStatus", "clinical_mode_diagnosing");
        
        // 调用diagnosis-engine-service生成鉴别诊断候选集
        Map<String, Object> diagnosisRequest = new HashMap<>();
        diagnosisRequest.put("cdpId", cdp.getId());
        diagnosisRequest.put("patientState", cdp.getPatientState());
        
        try {
            Object diagnosisResponse = diagnosisEngineClient.generateDDxCandidates(diagnosisRequest);
            // 解析响应并更新ddx
            List<Map<String, Object>> ddx = parseDDxResponse(diagnosisResponse);
            updates.put("ddx", ddx);
        } catch (Exception e) {
            log.warn("调用diagnosis-engine-service失败，使用空DDx列表继续流程", e);
            // 服务调用失败不影响流程继续，使用空DDx列表
            updates.put("ddx", new ArrayList<>());
        }
        
        // 调用risk-assessment-service进行风险评估
        try {
            Map<String, Object> riskRequest = new HashMap<>();
            riskRequest.put("cdpId", cdp.getId());
            riskRequest.put("ddx", updates.get("ddx"));
            
            Object riskResponse = riskAssessmentClient.assessRisk(riskRequest);
            // 解析响应并更新triage
            Map<String, Object> triage = parseRiskResponse(riskResponse);
            updates.put("triage", triage);
        } catch (Exception e) {
            log.error("调用risk-assessment-service失败", e);
            // 风险评估失败不影响流程继续
        }
        
        cdp = cdpManager.updateCDP(cdp.getId(), updates);
        log.info("Step 2完成: cdpId={}", cdp.getId());
        return cdp;
    }
    
    /**
     * Step 3: 组织候选集并建立分流路径
     * 调用脑区C（鉴别诊断）和脑区B（主动问诊）
     */
    private CDP step3OrganizeRoutingPath(CDP cdp) {
        log.info("Step 3: 组织候选集并建立分流路径 - cdpId={}", cdp.getId());
        
        Map<String, Object> updates = new HashMap<>();
        
        // 调用diagnosis-engine-service组织推理子组
        Map<String, Object> organizeRequest = new HashMap<>();
        organizeRequest.put("cdpId", cdp.getId());
        organizeRequest.put("ddx", cdp.getDdx());
        
        try {
            Object organizeResponse = diagnosisEngineClient.organizeReasoningGroups(organizeRequest);
            // 解析响应并更新ddx（包含推理子组信息）
            List<Map<String, Object>> organizedDDx = parseOrganizeResponse(organizeResponse);
            updates.put("ddx", organizedDDx);
        } catch (Exception e) {
            log.error("调用diagnosis-engine-service失败", e);
            throw new RuntimeException("Step 3执行失败: 推理子组组织失败", e);
        }
        
        // 调用dialog-service设计分流路径
        try {
            Map<String, Object> routingRequest = new HashMap<>();
            routingRequest.put("cdpId", cdp.getId());
            routingRequest.put("ddx", updates.get("ddx"));
            
            Object routingResponse = dialogServiceClient.designRoutingPath(routingRequest);
            // 解析响应，生成分流问题清单
            // TODO: 根据实际响应格式解析
        } catch (Exception e) {
            log.error("调用dialog-service失败", e);
            // 分流路径设计失败不影响流程继续
        }
        
        cdp = cdpManager.updateCDP(cdp.getId(), updates);
        log.info("Step 3完成: cdpId={}", cdp.getId());
        return cdp;
    }
    
    /**
     * Step 4: 采集关键证据并形成排序与验证计划
     * 调用脑区B（主动问诊）、脑区C（证据分析）、脑区D（检查建议）
     */
    private CDP step4CollectEvidenceAndPlan(CDP cdp) {
        log.info("Step 4: 采集关键证据并形成排序与验证计划 - cdpId={}", cdp.getId());
        
        Map<String, Object> updates = new HashMap<>();
        
        // 调用dialog-service采集关键证据
        Map<String, Object> evidenceRequest = new HashMap<>();
        evidenceRequest.put("cdpId", cdp.getId());
        evidenceRequest.put("ddx", cdp.getDdx());
        
        try {
            Object evidenceResponse = dialogServiceClient.collectKeyEvidence(evidenceRequest);
            // 解析响应并更新evidenceGraph
            List<Map<String, Object>> evidenceGraph = parseEvidenceResponse(evidenceResponse);
            updates.put("evidenceGraph", evidenceGraph);
        } catch (Exception e) {
            log.error("调用dialog-service失败", e);
            throw new RuntimeException("Step 4执行失败: 关键证据采集失败", e);
        }
        
        // 调用diagnosis-engine-service分析证据
        try {
            Map<String, Object> analysisRequest = new HashMap<>();
            analysisRequest.put("cdpId", cdp.getId());
            analysisRequest.put("evidenceGraph", updates.get("evidenceGraph"));
            analysisRequest.put("ddx", cdp.getDdx());
            
            Object analysisResponse = diagnosisEngineClient.analyzeEvidence(analysisRequest);
            // 解析响应并更新ddx（三层排序）
            List<Map<String, Object>> rankedDDx = parseAnalysisResponse(analysisResponse);
            updates.put("ddx", rankedDDx);
        } catch (Exception e) {
            log.error("调用diagnosis-engine-service失败", e);
            // 证据分析失败不影响流程继续
        }
        
        // 调用workup-planner-service构建验证计划
        try {
            Map<String, Object> workupRequest = new HashMap<>();
            workupRequest.put("cdpId", cdp.getId());
            workupRequest.put("ddx", updates.get("ddx"));
            
            Object workupResponse = workupPlannerClient.buildVerificationPlan(workupRequest);
            // 解析响应并更新workupPlan
            List<Map<String, Object>> workupPlan = parseWorkupResponse(workupResponse);
            updates.put("workupPlan", workupPlan);
        } catch (Exception e) {
            log.error("调用workup-planner-service失败", e);
            // 验证计划构建失败不影响流程继续
        }
        
        cdp = cdpManager.updateCDP(cdp.getId(), updates);
        log.info("Step 4完成: cdpId={}", cdp.getId());
        return cdp;
    }
    
    /**
     * Step 5: 回填证据并输出终点结论包
     * 调用所有相关服务
     */
    private CDP step5BackfillAndConclude(CDP cdp) {
        log.info("Step 5: 回填证据并输出终点结论包 - cdpId={}", cdp.getId());
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("cdpStatus", "clinical_mode_managing");
        
        // 调用clinical-parsing-service回填证据
        try {
            Map<String, Object> backfillRequest = new HashMap<>();
            backfillRequest.put("cdpId", cdp.getId());
            backfillRequest.put("evidenceGraph", cdp.getEvidenceGraph());
            
            Object backfillResponse = clinicalParsingClient.backfillEvidence(backfillRequest);
            // 解析响应并更新patientState
            Map<String, Object> updatedPatientState = parseBackfillResponse(backfillResponse);
            updates.put("patientState", updatedPatientState);
        } catch (Exception e) {
            log.error("调用clinical-parsing-service失败", e);
            // 证据回填失败不影响流程继续
        }
        
        // 调用diagnosis-engine-service更新三层排序
        try {
            Map<String, Object> rankingRequest = new HashMap<>();
            rankingRequest.put("cdpId", cdp.getId());
            rankingRequest.put("patientState", updates.get("patientState"));
            rankingRequest.put("evidenceGraph", cdp.getEvidenceGraph());
            
            Object rankingResponse = diagnosisEngineClient.updateRanking(rankingRequest);
            // 解析响应并更新ddx
            List<Map<String, Object>> finalDDx = parseRankingResponse(rankingResponse);
            updates.put("ddx", finalDDx);
        } catch (Exception e) {
            log.error("调用diagnosis-engine-service失败", e);
            // 排序更新失败不影响流程继续
        }
        
        // 调用treatment-engine-service生成治疗方案
        try {
            Map<String, Object> treatmentRequest = new HashMap<>();
            treatmentRequest.put("cdpId", cdp.getId());
            treatmentRequest.put("ddx", updates.get("ddx"));
            
            Object treatmentResponse = treatmentEngineClient.generateTreatmentPlan(treatmentRequest);
            // 解析响应并更新managementPlan
            List<Map<String, Object>> managementPlan = parseTreatmentResponse(treatmentResponse);
            updates.put("managementPlan", managementPlan);
        } catch (Exception e) {
            log.error("调用treatment-engine-service失败", e);
            // 治疗方案生成失败不影响流程继续
        }
        
        // 调用risk-assessment-service进行最终风险评估
        try {
            Map<String, Object> finalRiskRequest = new HashMap<>();
            finalRiskRequest.put("cdpId", cdp.getId());
            finalRiskRequest.put("ddx", updates.get("ddx"));
            finalRiskRequest.put("managementPlan", updates.get("managementPlan"));
            
            Object finalRiskResponse = riskAssessmentClient.assessFinalRisk(finalRiskRequest);
            // 解析响应并更新triage
            Map<String, Object> finalTriage = parseRiskResponse(finalRiskResponse);
            updates.put("triage", finalTriage);
        } catch (Exception e) {
            log.error("调用risk-assessment-service失败", e);
            // 最终风险评估失败不影响流程继续
        }
        
        // 调用explanation-service生成终点结论包
        try {
            Map<String, Object> explanationRequest = new HashMap<>();
            explanationRequest.put("cdpId", cdp.getId());
            explanationRequest.put("ddx", updates.get("ddx"));
            explanationRequest.put("evidenceGraph", cdp.getEvidenceGraph());
            explanationRequest.put("workupPlan", cdp.getWorkupPlan());
            explanationRequest.put("managementPlan", updates.get("managementPlan"));
            
            Object explanationResponse = explanationServiceClient.generateConclusionPackage(explanationRequest);
            // 解析响应，生成终点结论包
            // TODO: 根据实际响应格式解析并存储
        } catch (Exception e) {
            log.error("调用explanation-service失败", e);
            // 终点结论包生成失败不影响流程继续
        }
        
        // 更新状态为完成
        updates.put("cdpStatus", "completed");
        
        cdp = cdpManager.updateCDP(cdp.getId(), updates);
        log.info("Step 5完成: cdpId={}", cdp.getId());
        return cdp;
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 从CDP和patientState中提取用户输入的文本
     * 优先从userAnswers中获取最新的回答，如果没有则从healthStateAssessment中获取
     */
    private String extractUserInputText(CDP cdp, Map<String, Object> patientState) {
        // 1. 尝试从userAnswers中获取所有回答并合并（按顺序）
        if (patientState != null) {
            Map<String, Object> userAnswers = (Map<String, Object>) patientState.get("userAnswers");
            if (userAnswers != null && !userAnswers.isEmpty()) {
                // 将所有用户回答合并成一个文本（用空格分隔）
                // 这样可以包含所有用户输入的信息
                StringBuilder textBuilder = new StringBuilder();
                for (Object answer : userAnswers.values()) {
                    if (answer != null) {
                        String answerText = answer.toString().trim();
                        if (!answerText.isEmpty()) {
                            if (textBuilder.length() > 0) {
                                textBuilder.append(" ");
                            }
                            textBuilder.append(answerText);
                        }
                    }
                }
                String combinedText = textBuilder.toString();
                if (!combinedText.isEmpty()) {
                    log.debug("从userAnswers合并的文本: {}", combinedText);
                    return combinedText;
                }
            }
            
            // 2. 尝试从patientState中获取chiefComplaint或其他文本字段
            Object chiefComplaint = patientState.get("chiefComplaint");
            if (chiefComplaint != null) {
                return chiefComplaint.toString();
            }
        }
        
        // 3. 尝试从healthStateAssessment中获取userInput
        if (cdp.getHealthStateAssessment() != null) {
            Object userInput = cdp.getHealthStateAssessment().get("userInput");
            if (userInput != null) {
                return userInput.toString();
            }
        }
        
        // 4. 如果都没有，返回空字符串
        log.warn("未找到用户输入文本: cdpId={}", cdp.getId());
        return "";
    }
    
    // ========== 响应解析方法（TODO: 根据实际响应格式实现） ==========
    
    /**
     * 解析clinical-parsing-service的响应
     * 响应格式：{"code": 200, "message": "success", "data": {...}, "timestamp": ...}
     * data格式：{"concepts": [...], "structuredData": {...}, "ambiguousExpressions": [...]}
     */
    private Map<String, Object> parseParsingResponse(Object response) {
        if (response == null) {
            return new HashMap<>();
        }
        
        try {
            // 响应是Map类型
            if (response instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) response;
                
                // 提取data字段
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    
                    // 构建解析后的状态
                    Map<String, Object> parsedState = new HashMap<>();
                    
                    // 提取归一化概念
                    Object concepts = dataMap.get("concepts");
                    if (concepts != null) {
                        parsedState.put("normalizedConcepts", concepts);
                    }
                    
                    // 提取结构化数据
                    Object structuredData = dataMap.get("structuredData");
                    if (structuredData != null) {
                        parsedState.put("structuredData", structuredData);
                        
                        // 将结构化数据中的症状等信息也提取出来，方便后续使用
                        if (structuredData instanceof Map) {
                            Map<String, Object> structuredMap = (Map<String, Object>) structuredData;
                            
                            // 提取症状列表
                            Object symptoms = structuredMap.get("symptoms");
                            if (symptoms != null) {
                                parsedState.put("symptoms", symptoms);
                            }
                            
                            // 提取体征列表
                            Object signs = structuredMap.get("signs");
                            if (signs != null) {
                                parsedState.put("signs", signs);
                            }
                            
                            // 提取检查列表
                            Object examinations = structuredMap.get("examinations");
                            if (examinations != null) {
                                parsedState.put("examinations", examinations);
                            }
                        }
                    }
                    
                    // 提取歧义表达
                    Object ambiguousExpressions = dataMap.get("ambiguousExpressions");
                    if (ambiguousExpressions != null) {
                        parsedState.put("ambiguousExpressions", ambiguousExpressions);
                    }
                    
                    log.debug("解析后的parsedState: {}", parsedState);
                    return parsedState;
                } else {
                    // 如果data不是Map，直接返回data
                    Map<String, Object> parsedState = new HashMap<>();
                    parsedState.put("parsingResult", data);
                    return parsedState;
                }
            }
        } catch (Exception e) {
            log.error("解析clinical-parsing-service响应失败", e);
        }
        
        return new HashMap<>();
    }
    
    private List<Map<String, Object>> parseDDxResponse(Object response) {
        // TODO: 解析diagnosis-engine-service的响应
        return new ArrayList<>();
    }
    
    private Map<String, Object> parseRiskResponse(Object response) {
        // TODO: 解析risk-assessment-service的响应
        return new HashMap<>();
    }
    
    private List<Map<String, Object>> parseOrganizeResponse(Object response) {
        // TODO: 解析organize响应
        return new ArrayList<>();
    }
    
    private List<Map<String, Object>> parseEvidenceResponse(Object response) {
        // TODO: 解析evidence响应
        return new ArrayList<>();
    }
    
    private List<Map<String, Object>> parseAnalysisResponse(Object response) {
        // TODO: 解析analysis响应
        return new ArrayList<>();
    }
    
    private List<Map<String, Object>> parseWorkupResponse(Object response) {
        // TODO: 解析workup响应
        return new ArrayList<>();
    }
    
    private Map<String, Object> parseBackfillResponse(Object response) {
        // TODO: 解析backfill响应
        return new HashMap<>();
    }
    
    private List<Map<String, Object>> parseRankingResponse(Object response) {
        // TODO: 解析ranking响应
        return new ArrayList<>();
    }
    
    private List<Map<String, Object>> parseTreatmentResponse(Object response) {
        // TODO: 解析treatment响应
        return new ArrayList<>();
    }
}

