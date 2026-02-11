"""
病例理解服务API路由
"""
import logging
import time
from fastapi import APIRouter
from app.models.request import ClinicalParsingRequest
from app.models.response import ClinicalParsingResponse
from app.models.tool_context import ToolContext
from app.models.tool_result import ToolResult, Evidence, Quality, SuggestedWrite, ErrorInfo
from app.services.parsing_service import ClinicalParsingService
from app.utils.response import success_response
from app.utils.cdp_reader import read_cdp_fields

logger = logging.getLogger(__name__)

router = APIRouter()
service = ClinicalParsingService()

@router.post("/parsing/parse")
async def parse_clinical_data(request: ClinicalParsingRequest):
    """
    病例理解与结构化
    
    将非结构化的患者信息转换为结构化的临床要素
    
    - **userId**: 用户ID
    - **sessionId**: 会话ID
    - **text**: 输入文本
    - **cdpId**: CDP ID（可选）
    - **input**: 额外输入信息（可选）
    """
    logger.info(f"收到病例理解请求: userId={request.userId}, sessionId={request.sessionId}")
    result = await service.parse(request)
    return success_response(result.dict())

@router.post("/tools/tool_1/invoke", response_model=ToolResult)
async def invoke_tool_1(tool_context: ToolContext) -> ToolResult:
    """
    统一的工具调用接口（tool_1：病例理解工具）
    
    接收ToolContext，返回ToolResult
    """
    start_time = time.time()
    
    try:
        # 1. 从ToolContext中提取CDP数据
        cdp_id = tool_context.cdp_reference.cdp_id
        cdp_version = tool_context.cdp_reference.version
        read_fields = tool_context.cdp_reference.read_fields
        
        # 2. 从CDP读取数据（根据read_fields）
        cdp_data = await read_cdp_fields(cdp_id, cdp_version, read_fields)
        patient_state = cdp_data.get("cdp.patient_state", {})
        
        # 3. 构建现有服务的请求格式
        text = patient_state.get("user_input", "") or patient_state.get("text", "")
        parsing_request = ClinicalParsingRequest(
            userId=patient_state.get("user_id", ""),
            sessionId=patient_state.get("session_id", ""),
            text=text,
            cdpId=cdp_id,
            input=patient_state
        )
        
        # 4. 调用现有业务逻辑
        parsing_result = await service.parse(parsing_request)
        
        # 5. 转换为ToolResult格式
        duration_ms = int((time.time() - start_time) * 1000)
        
        # 构建evidence（从解析结果中提取）
        evidence_list = []
        for concept in parsing_result.concepts:
            if concept.cui:
                # 确定CUI来源
                cui_source = "UMLS"  # 默认
                if concept.icd:
                    cui_source = "ICD"
                elif concept.snomed:
                    cui_source = "SNOMED"
                elif concept.loinc:
                    cui_source = "LOINC"
                elif concept.atc:
                    cui_source = "ATC"
                
                evidence_list.append(Evidence(
                    source="knowledge_base",
                    reference=f"{cui_source}:{concept.cui}",
                    strength="strong" if concept.confidence > 0.8 else "medium",
                    evidence_name=f"概念归一化: {concept.originalText} -> {concept.normalizedSymptom}"
                ))
        
        # 构建payload（按照规范要求的结构）
        structured_data = parsing_result.structuredData
        payload = {
            "symptoms": [
                {
                    "symptom_name": s.name,
                    "symptom_cui": s.cui,
                    "duration": s.duration,
                    "severity": s.severity,
                    "trigger": s.trigger
                }
                for s in structured_data.symptoms
            ],
            "signs": [
                {
                    "sign_name": s.name,
                    "sign_cui": None,  # 体征通常没有CUI
                    "value": s.value,
                    "unit": s.unit
                }
                for s in structured_data.signs
            ],
            "past_history": [
                {
                    "disease_name": h.disease,
                    "disease_cui": h.icd,  # 使用ICD作为CUI
                    "diagnosis_date": None,  # 需要从原始数据提取
                    "treatment_status": h.status
                }
                for h in structured_data.medicalHistory
            ],
            "medications": [
                {
                    "medication_name": m.name,
                    "medication_cui": m.atc,  # 使用ATC作为CUI
                    "dosage": None,  # 需要从原始数据提取
                    "frequency": None,  # 需要从原始数据提取
                    "start_date": None  # 需要从原始数据提取
                }
                for m in structured_data.medications
            ],
            "allergies": [
                {
                    "allergen_name": a.allergen,
                    "allergen_cui": None,  # 需要从原始数据提取
                    "reaction": a.reaction
                }
                for a in structured_data.allergies
            ],
            "lab_abnormalities": [
                {
                    "test_name": e.name,
                    "test_cui": e.loinc,  # 使用LOINC作为CUI
                    "abnormal_value": e.result,
                    "abnormal_degree": "异常" if e.abnormal else "正常"
                }
                for e in structured_data.examinations if e.abnormal
            ]
        }
        
        # 构建suggested_writes（按照规范要求写回cdp.patient_state）
        suggested_writes = [
            SuggestedWrite(
                field_path="cdp.patient_state",
                value={
                    "symptoms": payload["symptoms"],
                    "signs": payload["signs"],
                    "past_history": payload["past_history"],
                    "medications": payload["medications"],
                    "allergies": payload["allergies"],
                    "lab_abnormalities": payload["lab_abnormalities"]
                },
                reason="更新患者状态（病例理解结果）"
            )
        ]
        
        tool_result = ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_1",
            status="success",
            payload=payload,
            evidence=evidence_list,
            quality=Quality(
                confidence=0.9,
                completeness=0.85,
                accuracy=0.88
            ),
            suggested_writes=suggested_writes,
            errors=[],
            duration_ms=duration_ms,
            metadata={}
        )
        
        return tool_result
        
    except Exception as e:
        duration_ms = int((time.time() - start_time) * 1000)
        logger.error(f"工具调用失败: tool_id=tool_1, trace_id={tool_context.trace_id}, error={str(e)}", exc_info=True)
        return ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_1",
            status="failure",
            payload={},
            evidence=[],
            quality=Quality(confidence=0.0, completeness=0.0, accuracy=0.0),
            suggested_writes=[],
            errors=[ErrorInfo(
                error_type="runtime_error",
                error_message=str(e),
                error_details={}
            )],
            duration_ms=duration_ms,
            metadata={}
        )

@router.get("/parsing/health")
async def health_check():
    """健康检查"""
    return success_response({
        "status": "healthy",
        "service": "clinical-parsing-service"
    })

