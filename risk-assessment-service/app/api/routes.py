"""
风险评估服务API路由
"""
from fastapi import APIRouter
from app.services.risk_assessment_engine import RiskAssessmentEngine
from app.services.triage_engine import TriageEngine
from app.services.upgrade_rule_engine import UpgradeRuleEngine
from app.builders.conclusion_package_builder import ConclusionPackageBuilder
from app.models.request import RiskAssessmentRequest
from app.models.response import RiskAssessmentResponse

router = APIRouter()

risk_assessment_engine = RiskAssessmentEngine()
triage_engine = TriageEngine()
upgrade_rule_engine = UpgradeRuleEngine()
conclusion_package_builder = ConclusionPackageBuilder()


@router.post("/risk/assess", response_model=RiskAssessmentResponse)
async def assess_risk(request: RiskAssessmentRequest):
    """
    风险评估
    """
    # 如果cdp为空，尝试从其他字段构建
    cdp = request.cdp
    if not cdp or len(cdp) == 0:
        cdp = {
            "id": getattr(request, "cdpId", ""),
            "patient_state": request.patient_state,
            "ddx": request.ddx
        }
    
    result = risk_assessment_engine.assess_risk(cdp)
    return RiskAssessmentResponse(**result)


@router.post("/risk/triage")
async def triage(request: dict):
    """
    分诊评估
    """
    result = triage_engine.triage(
        patient_state=request.get("patient_state", {}),
        ddx=request.get("ddx", [])
    )
    return result


@router.post("/risk/upgrade-rules")
async def get_upgrade_rules(request: dict):
    """
    获取升级规则
    """
    result = upgrade_rule_engine.get_upgrade_rules(
        risk_level=request.get("risk_level"),
        patient_state=request.get("patient_state", {})
    )
    return result


@router.post("/risk/conclusion-package")
async def build_conclusion_package(request: dict):
    """
    构建终点结论包
    """
    result = conclusion_package_builder.build_conclusion_package(
        three_layer_result=request.get("three_layer_result", {}),
        evidence_analysis=request.get("evidence_analysis", {})
    )
    return result


@router.post("/risk/assess-final", response_model=RiskAssessmentResponse)
async def assess_final_risk(request: dict):
    """
    最终风险评估
    在诊断流程结束时进行最终风险评估
    """
    cdp = request.get("cdp", {})
    if not cdp:
        # 如果请求中没有cdp字段，尝试从其他字段构建
        cdp = {
            "id": request.get("cdpId", ""),
            "patient_state": request.get("patient_state", {}),
            "ddx": request.get("ddx", []),
            "management_plan": request.get("management_plan", request.get("managementPlan", []))
        }
    
    result = risk_assessment_engine.assess_risk(cdp)
    return RiskAssessmentResponse(**result)
