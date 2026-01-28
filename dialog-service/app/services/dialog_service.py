"""
对话服务
按照《dialog-service - 服务实现方案.md》实现
"""
from typing import Dict, Any, Optional
import json
import logging
import httpx
import redis
from app.core.adaptive_questioning import AdaptiveQuestioningStrategy
from app.core.nlu import NaturalLanguageUnderstanding
from app.core.nlg import NaturalLanguageGenerator
from app.identifiers.information_gap_identifier import InformationGapIdentifier
from app.calculators.completeness_calculator import CompletenessCalculator
from app.models.request import QuestionRequest, UserInputRequest, IdentifyGapsRequest
from app.config.settings import settings
from app.utils.exceptions import BusinessException

logger = logging.getLogger(__name__)


class DialogService:
    """对话服务"""
    
    def __init__(self):
        # Redis客户端（带降级处理）
        self.redis_client = None
        self.redis_available = False
        self.memory_context = {}  # 内存存储作为后备
        
        try:
            self.redis_client = redis.Redis(
                host=settings.REDIS_HOST,
                port=settings.REDIS_PORT,
                db=settings.REDIS_DB,
                password=settings.REDIS_PASSWORD,
                decode_responses=True,
                socket_connect_timeout=2,  # 2秒连接超时
                socket_timeout=2
            )
            # 测试连接
            self.redis_client.ping()
            self.redis_available = True
            logger.info("Redis连接成功")
        except Exception as e:
            logger.warning(f"Redis连接失败，将使用内存存储: {str(e)}")
            self.redis_available = False
        
        # 核心组件
        self.adaptive_questioning = AdaptiveQuestioningStrategy()
        self.nlu = NaturalLanguageUnderstanding()
        self.nlg = NaturalLanguageGenerator()
        self.gap_identifier = InformationGapIdentifier()
        self.completeness_calculator = CompletenessCalculator()
        
        # HTTP客户端（用于调用diagnosis-service获取CDP数据）
        self.http_client = httpx.AsyncClient(timeout=30.0)
        self.diagnosis_service_url = settings.DIAGNOSIS_SERVICE_URL
    
    async def generate_question(self, request: QuestionRequest) -> Dict[str, Any]:
        """
        生成追问问题
        
        Args:
            request: 生成问题请求
            
        Returns:
            问题响应
        """
        logger.info(f"生成追问问题: cdpId={request.cdpId}")
        
        try:
            # 1. 获取CDP数据（从diagnosis-service）
            cdp_data = await self._get_cdp_data(request.cdpId)
            if not cdp_data:
                raise BusinessException(1205, f"CDP不存在: {request.cdpId}")
            
            patient_state = cdp_data.get("patient_state", {})
            ddx = cdp_data.get("ddx", [])
            
            # 2. 获取对话上下文
            context = self._get_context(request.cdpId)
            context.update(request.context or {})
            context["patient_state"] = patient_state
            context["ddx"] = ddx
            
            # 3. 识别信息缺口
            information_gaps = self.gap_identifier.identify_gaps(patient_state, ddx)
            
            # 4. 计算信息完整度
            completeness = self.completeness_calculator.calculate_completeness(patient_state)
            
            # 5. 生成追问策略
            question_info = await self.adaptive_questioning.generate_question(
                information_gaps=information_gaps,
                context=context,
                completeness=completeness
            )
            
            if not question_info:
                # 没有需要追问的信息
                return {
                    "question": None,
                    "questionType": None,
                    "missingInfo": [],
                    "completeness": completeness,
                    "informationGaps": {
                        "required": information_gaps.get("required", []),
                        "important": information_gaps.get("important", []),
                        "optional": information_gaps.get("optional", [])
                    }
                }
            
            # 6. 生成自然语言问题
            natural_question = await self.nlg.generate_question(question_info, context)
            
            # 7. 更新对话历史
            self._update_conversation_history(request.cdpId, "assistant", natural_question)
            
            # 8. 构建响应
            missing_info_items = [
                {
                    "field": gap.get("field", ""),
                    "level": "required" if gap in information_gaps.get("required", []) else
                            "important" if gap in information_gaps.get("important", []) else
                            "optional",
                    "description": gap.get("description", "")
                }
                for gap in question_info.get("missing_info", [])
            ]
            
            return {
                "question": natural_question,
                "questionType": question_info.get("type"),
                "missingInfo": missing_info_items,
                "completeness": completeness,
                "informationGaps": {
                    "required": information_gaps.get("required", []),
                    "important": information_gaps.get("important", []),
                    "optional": information_gaps.get("optional", [])
                }
            }
        except BusinessException:
            raise
        except Exception as e:
            logger.error(f"生成追问问题失败: {str(e)}", exc_info=True)
            raise BusinessException(1202, f"智能追问生成失败: {str(e)}")
    
    async def understand(self, request: UserInputRequest) -> Dict[str, Any]:
        """
        理解用户输入
        
        Args:
            request: 用户输入请求
            
        Returns:
            理解响应
        """
        logger.info(f"理解用户输入: cdpId={request.cdpId}")
        
        if not request.userInput or not request.userInput.strip():
            raise BusinessException(1206, "用户输入为空")
        
        try:
            # 1. 获取对话上下文
            context = self._get_context(request.cdpId)
            context.update(request.context or {})
            
            # 2. 更新对话历史（用户输入）
            self._update_conversation_history(request.cdpId, "user", request.userInput)
            
            # 3. 自然语言理解
            understood_info = await self.nlu.understand(
                text=request.userInput,
                context=context
            )
            
            # 4. 提取关键信息
            extracted_info = {
                "symptom_duration": understood_info.get("symptom_duration"),
                "symptom_severity": understood_info.get("symptom_severity"),
                "symptom_location": understood_info.get("symptom_location"),
                "symptom_trigger": understood_info.get("symptom_trigger"),
                "symptom_frequency": understood_info.get("symptom_frequency"),
                "symptom_relief": understood_info.get("symptom_relief"),
                "accompanying_symptoms": understood_info.get("accompanying_symptoms", [])
            }
            
            # 5. 更新上下文
            self._update_context(request.cdpId, extracted_info)
            
            # 6. 更新CDP（通过diagnosis-service）
            await self._update_cdp(request.cdpId, extracted_info)
            
            # 7. 构建响应
            updated_fields = [key for key, value in extracted_info.items() if value is not None]
            
            return {
                "extractedInfo": extracted_info,
                "confidence": understood_info.get("confidence", 0.8),
                "updatedFields": updated_fields
            }
        except BusinessException:
            raise
        except Exception as e:
            logger.error(f"理解用户输入失败: {str(e)}", exc_info=True)
            raise BusinessException(1203, f"自然语言理解失败: {str(e)}")
    
    async def identify_gaps(self, request: IdentifyGapsRequest) -> Dict[str, Any]:
        """
        识别信息缺口
        
        Args:
            request: 识别信息缺口请求
            
        Returns:
            信息缺口响应
        """
        logger.info(f"识别信息缺口: cdpId={request.cdpId}")
        
        try:
            # 1. 获取CDP数据
            cdp_data = await self._get_cdp_data(request.cdpId)
            if not cdp_data:
                raise BusinessException(1205, f"CDP不存在: {request.cdpId}")
            
            patient_state = cdp_data.get("patient_state", {})
            ddx = cdp_data.get("ddx", [])
            
            # 2. 识别信息缺口
            information_gaps = self.gap_identifier.identify_gaps(patient_state, ddx)
            
            # 3. 计算信息完整度
            completeness = self.completeness_calculator.calculate_completeness(patient_state)
            
            return {
                "informationGaps": {
                    "required": information_gaps.get("required", []),
                    "important": information_gaps.get("important", []),
                    "optional": information_gaps.get("optional", [])
                },
                "completeness": completeness
            }
        except BusinessException:
            raise
        except Exception as e:
            logger.error(f"识别信息缺口失败: {str(e)}", exc_info=True)
            raise BusinessException(1201, f"信息缺口识别失败: {str(e)}")
    
    async def handle_websocket_message(self, cdp_id: str, message: str) -> Dict[str, Any]:
        """处理WebSocket消息"""
        try:
            user_input = json.loads(message)
        except json.JSONDecodeError:
            user_input = {"text": message}
        
        # 理解用户输入
        understanding = await self.understand(
            UserInputRequest(
                cdpId=cdp_id,
                userInput=user_input.get("text", message),
                context=None
            )
        )
        
        # 生成回复
        question_request = QuestionRequest(
            cdpId=cdp_id,
            context=self._get_context(cdp_id)
        )
        question_response = await self.generate_question(question_request)
        
        return {
            "type": "response",
            "question": question_response.get("question"),
            "completeness": question_response.get("completeness"),
            "extractedInfo": understanding.get("extractedInfo")
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
    
    async def _update_cdp(self, cdp_id: str, extracted_info: Dict[str, Any]):
        """更新CDP（通过diagnosis-service）"""
        try:
            url = f"{self.diagnosis_service_url}/api/v1/diagnosis/cdp/{cdp_id}/update"
            payload = {
                "patient_state": extracted_info
            }
            response = await self.http_client.post(url, json=payload)
            response.raise_for_status()
        except Exception as e:
            logger.warning(f"更新CDP失败: {str(e)}")
            # 不抛出异常，因为这不是关键路径
    
    def _get_context(self, cdp_id: str) -> Dict[str, Any]:
        """获取对话上下文"""
        context_key = f"dialog:context:{cdp_id}"
        
        if self.redis_available and self.redis_client:
            try:
                context_json = self.redis_client.get(context_key)
                if context_json:
                    try:
                        return json.loads(context_json)
                    except json.JSONDecodeError:
                        return {}
            except Exception as e:
                logger.warning(f"从Redis获取上下文失败，使用内存存储: {str(e)}")
                self.redis_available = False
        
        # 使用内存存储作为后备
        return self.memory_context.get(context_key, {})
    
    def _update_context(self, cdp_id: str, info: Dict[str, Any]):
        """更新对话上下文"""
        context_key = f"dialog:context:{cdp_id}"
        context = self._get_context(cdp_id)
        context.update(info)
        
        if self.redis_available and self.redis_client:
            try:
                self.redis_client.setex(
                    context_key,
                    settings.REDIS_CONTEXT_TTL,
                    json.dumps(context)
                )
            except Exception as e:
                logger.warning(f"更新Redis上下文失败，使用内存存储: {str(e)}")
                self.redis_available = False
                self.memory_context[context_key] = context
        else:
            # 使用内存存储
            self.memory_context[context_key] = context
    
    def _update_conversation_history(self, cdp_id: str, role: str, content: str):
        """更新对话历史"""
        context = self._get_context(cdp_id)
        if "conversationHistory" not in context:
            context["conversationHistory"] = []
        
        context["conversationHistory"].append({
            "role": role,
            "content": content
        })
        
        # 限制历史记录长度（最多保留20条）
        if len(context["conversationHistory"]) > 20:
            context["conversationHistory"] = context["conversationHistory"][-20:]
        
        self._update_context(cdp_id, context)
    
    async def design_routing_path(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """
        设计分流路径
        
        Args:
            request: 设计分流路径请求，包含cdpId、reasoningSubgroups、keyDifferences等
            
        Returns:
            分流路径响应
        """
        logger.info(f"设计分流路径: cdpId={request.get('cdpId')}")
        
        try:
            cdp_id = request.get("cdpId")
            if not cdp_id:
                raise BusinessException(1202, "cdpId不能为空")
            
            # 1. 获取CDP数据
            cdp_data = await self._get_cdp_data(cdp_id)
            if not cdp_data:
                raise BusinessException(1205, f"CDP不存在: {cdp_id}")
            
            # 2. 获取推理子组和关键差异点
            reasoning_subgroups = request.get("reasoningSubgroups", [])
            key_differences = request.get("keyDifferences", [])
            
            # 3. 基于推理子组和关键差异点设计分流路径
            # TODO: 实现完整的分流路径设计逻辑
            routing_path = {
                "paths": [],
                "priority": "high"
            }
            
            # 临时实现：返回基本结构
            if reasoning_subgroups:
                for subgroup in reasoning_subgroups[:3]:  # 取前3个
                    routing_path["paths"].append({
                        "pathId": f"path_{len(routing_path['paths']) + 1}",
                        "subgroup": subgroup,
                        "priority": "high"
                    })
            
            return {
                "routingPath": routing_path,
                "status": "success"
            }
        except BusinessException:
            raise
        except Exception as e:
            logger.error(f"设计分流路径失败: {str(e)}", exc_info=True)
            raise BusinessException(1203, f"分流路径设计失败: {str(e)}")
    
    async def collect_key_evidence(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """
        采集关键证据
        
        Args:
            request: 采集关键证据请求，包含cdpId、routingPath等
            
        Returns:
            关键证据响应
        """
        logger.info(f"采集关键证据: cdpId={request.get('cdpId')}")
        
        try:
            cdp_id = request.get("cdpId")
            if not cdp_id:
                raise BusinessException(1202, "cdpId不能为空")
            
            # 1. 获取CDP数据
            cdp_data = await self._get_cdp_data(cdp_id)
            if not cdp_data:
                raise BusinessException(1205, f"CDP不存在: {cdp_id}")
            
            # 2. 获取分流路径
            routing_path = request.get("routingPath", {})
            paths = routing_path.get("paths", [])
            
            # 3. 基于分流路径采集关键证据
            # TODO: 实现完整的关键证据采集逻辑
            key_evidence = []
            
            # 临时实现：返回基本结构
            for path in paths[:5]:  # 取前5个路径
                key_evidence.append({
                    "evidenceId": f"evidence_{len(key_evidence) + 1}",
                    "pathId": path.get("pathId"),
                    "evidenceType": "question",
                    "priority": "high",
                    "question": "请详细描述相关症状"
                })
            
            return {
                "keyEvidence": key_evidence,
                "status": "success"
            }
        except BusinessException:
            raise
        except Exception as e:
            logger.error(f"采集关键证据失败: {str(e)}", exc_info=True)
            raise BusinessException(1204, f"关键证据采集失败: {str(e)}")
    
    async def cleanup_context(self, cdp_id: str):
        """清理上下文"""
        context_key = f"dialog:context:{cdp_id}"
        
        if self.redis_available and self.redis_client:
            try:
                self.redis_client.delete(context_key)
            except Exception as e:
                logger.warning(f"从Redis删除上下文失败: {str(e)}")
        
        # 同时清理内存存储
        if context_key in self.memory_context:
            del self.memory_context[context_key]
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.http_client.aclose()
