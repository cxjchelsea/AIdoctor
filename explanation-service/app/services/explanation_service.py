"""
解释生成服务（脑区G）
"""
import logging
import httpx
from typing import Dict, Any, Optional
from app.models.request import ExplanationRequest
from app.models.response import ExplanationResponse, EvidenceChain, ConclusionPackage, ReasoningPath
from app.builders.evidence_chain_builder import EvidenceChainBuilder
from app.builders.path_visualizer import PathVisualizer
from app.builders.conclusion_package_builder import ConclusionPackageBuilder
from app.builders.explanation_generator import ExplanationGenerator
from app.config.settings import settings
from app.utils.exceptions import BusinessException
from app.utils.specific_exceptions import CDPNotFoundException, DiagnosisDataIncompleteException

logger = logging.getLogger(__name__)


class ExplanationService:
    """解释生成服务"""
    
    def __init__(self):
        """初始化解释服务"""
        self.evidence_chain_builder = EvidenceChainBuilder()
        self.path_visualizer = PathVisualizer()
        self.conclusion_package_builder = ConclusionPackageBuilder()
        self.explanation_generator = ExplanationGenerator()
        self.http_client = httpx.AsyncClient(timeout=30.0)
        self.diagnosis_service_url = settings.DIAGNOSIS_SERVICE_URL
    
    async def explain(self, request: ExplanationRequest) -> ExplanationResponse:
        """
        生成可解释性结果
        生成完整的证据链，让系统的"结论"能被复核
        """
        logger.info(f"生成解释: cdpId={request.cdpId}")
        
        try:
            # 1. 获取CDP数据
            cdp_data = await self._get_cdp_data(request.cdpId)
            if not cdp_data:
                raise CDPNotFoundException(request.cdpId)
            
            # 如果提供了诊断结果，合并到CDP数据中
            if request.diagnosisResult:
                cdp_data.update(request.diagnosisResult)
            
            # 2. 构建证据链
            evidence_chain = self.evidence_chain_builder.build(cdp_data)
            
            # 3. 可视化推理路径
            reasoning_paths = self.path_visualizer.visualize(cdp_data)
            
            # 4. 生成终点结论包
            conclusion_package = self.conclusion_package_builder.build(cdp_data)
            
            # 5. 生成自然语言解释
            natural_language_explanation = await self.explanation_generator.generate(
                conclusion_package=conclusion_package,
                evidence_chain=evidence_chain,
                reasoning_paths=reasoning_paths
            )
            
            # 6. 更新CDP（写入evidence_graph和conclusion_package）
            await self._update_cdp(
                request.cdpId,
                evidence_chain=evidence_chain,
                conclusion_package=conclusion_package
            )
            
            # 7. 构建响应
            reasoning_path_models = [
                ReasoningPath(
                    pathId=path.get("pathId", ""),
                    description=path.get("description", ""),
                    confidence=path.get("confidence", 0.0),
                    nodes=path.get("nodes", []),
                    edges=path.get("edges", [])
                )
                for path in reasoning_paths
            ]
            
            return ExplanationResponse(
                evidenceChain=evidence_chain,
                reasoningPaths=reasoning_path_models,
                conclusionPackage=ConclusionPackage(**conclusion_package),
                naturalLanguageExplanation=natural_language_explanation
            )
        except BusinessException:
            raise
        except Exception as e:
            logger.error(f"解释生成失败: {str(e)}", exc_info=True)
            raise BusinessException(1600, f"解释生成服务内部错误: {str(e)}")
    
    async def generate_evidence_chain(self, request: ExplanationRequest) -> EvidenceChain:
        """
        仅生成证据链
        
        Args:
            request: 解释生成请求
            
        Returns:
            证据链
        """
        logger.info(f"生成证据链: cdpId={request.cdpId}")
        
        cdp_data = await self._get_cdp_data(request.cdpId)
        if not cdp_data:
            raise CDPNotFoundException(request.cdpId)
        
        if request.diagnosisResult:
            cdp_data.update(request.diagnosisResult)
        
        return self.evidence_chain_builder.build(cdp_data)
    
    async def generate_conclusion_package(self, request: ExplanationRequest) -> ConclusionPackage:
        """
        仅生成终点结论包
        
        Args:
            request: 解释生成请求
            
        Returns:
            终点结论包
        """
        logger.info(f"生成终点结论包: cdpId={request.cdpId}")
        
        cdp_data = await self._get_cdp_data(request.cdpId)
        if not cdp_data:
            raise CDPNotFoundException(request.cdpId)
        
        if request.diagnosisResult:
            cdp_data.update(request.diagnosisResult)
        
        conclusion_package_dict = self.conclusion_package_builder.build(cdp_data)
        return ConclusionPackage(**conclusion_package_dict)
    
    async def generate_natural_language(self, request: ExplanationRequest) -> Dict[str, Any]:
        """
        仅生成自然语言解释
        
        Args:
            request: 解释生成请求
            
        Returns:
            自然语言解释
        """
        logger.info(f"生成自然语言解释: cdpId={request.cdpId}")
        
        cdp_data = await self._get_cdp_data(request.cdpId)
        if not cdp_data:
            raise CDPNotFoundException(request.cdpId)
        
        if request.diagnosisResult:
            cdp_data.update(request.diagnosisResult)
        
        # 需要先构建结论包和证据链
        evidence_chain = self.evidence_chain_builder.build(cdp_data)
        reasoning_paths = self.path_visualizer.visualize(cdp_data)
        conclusion_package = self.conclusion_package_builder.build(cdp_data)
        
        natural_language_explanation = await self.explanation_generator.generate(
            conclusion_package=conclusion_package,
            evidence_chain=evidence_chain,
            reasoning_paths=reasoning_paths
        )
        
        return {
            "naturalLanguageExplanation": natural_language_explanation
        }
    
    async def _get_cdp_data(self, cdp_id: str) -> Optional[Dict[str, Any]]:
        """从diagnosis-service获取CDP数据"""
        try:
            url = f"{self.diagnosis_service_url}/api/v1/diagnosis/cdp/{cdp_id}"
            response = await self.http_client.get(url)
            response.raise_for_status()
            result = response.json()
            
            if result.get("code") == 200:
                return result.get("data", {})
            return None
        except Exception as e:
            logger.warning(f"获取CDP数据失败: {str(e)}")
            return None
    
    async def _update_cdp(
        self,
        cdp_id: str,
        evidence_chain: EvidenceChain,
        conclusion_package: ConclusionPackage
    ):
        """更新CDP（通过diagnosis-service）"""
        try:
            url = f"{self.diagnosis_service_url}/api/v1/diagnosis/cdp/{cdp_id}/update"
            
            # 构建更新数据
            updates = {
                "evidence_graph": {
                    "evidence_chain": {
                        "evidence": [
                            {
                                "item": ev.item,
                                "type": ev.type,
                                "strength": ev.strength,
                                "supporting_diseases": ev.supportingDiseases,
                                "contradicting_diseases": ev.contradictingDiseases
                            }
                            for ev in evidence_chain.evidence
                        ],
                        "reasoning_paths": evidence_chain.reasoningPaths
                    }
                },
                "conclusion_package": {
                    "conclusion": conclusion_package.conclusion,
                    "must_exclude_status": conclusion_package.mustExcludeStatus,
                    "key_evidence": conclusion_package.keyEvidence,
                    "action_and_followup": conclusion_package.actionAndFollowUp
                }
            }
            
            response = await self.http_client.post(url, json=updates)
            response.raise_for_status()
            logger.info(f"CDP更新成功: cdpId={cdp_id}")
        except Exception as e:
            logger.warning(f"更新CDP失败: {str(e)}")
            # 不抛出异常，因为这不是关键路径
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.http_client.aclose()

