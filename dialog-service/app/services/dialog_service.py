"""
对话服务
按照《dialog-service - 服务实现方案.md》实现
"""
from typing import Dict, Any, Optional, List
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
        try:
            # 1. 获取patient_state（优先使用请求中的，否则从CDP读取）
            if request.patientState:
                # 如果请求中提供了patientState，直接使用
                patient_state = request.patientState
                ddx = []  # 如果没有提供ddx，从CDP读取
                cdp_data = await self._get_cdp_data(request.cdpId)
                if cdp_data:
                    ddx = cdp_data.get("ddx", [])
            else:
                # 否则从CDP读取
                cdp_data = await self._get_cdp_data(request.cdpId)
                if not cdp_data:
                    raise BusinessException(1205, f"CDP不存在: {request.cdpId}")
                # 注意：diagnosis-service返回的是patientState（驼峰命名），不是patient_state
                patient_state = cdp_data.get("patientState") or cdp_data.get("patient_state", {})
                ddx = cdp_data.get("ddx", [])
            
            # 调试信息：输出到控制台（不写入日志文件）
            print(f"\n[DEBUG] ===== generate_question调用 =====")
            print(f"[DEBUG] cdpId={request.cdpId}")
            print(f"[DEBUG] patient_state类型: {type(patient_state)}")
            if isinstance(patient_state, dict):
                print(f"[DEBUG] patient_state的keys: {list(patient_state.keys())}")
                if "symptoms" in patient_state:
                    print(f"[DEBUG] symptoms内容: {patient_state.get('symptoms')}")
                else:
                    print(f"[DEBUG] ⚠️ patient_state中没有symptoms字段！")
                    print(f"[DEBUG] patient_state完整内容: {patient_state}")
            
            # 2. 获取对话上下文
            context = self._get_context(request.cdpId)
            context.update(request.context or {})
            context["patient_state"] = patient_state
            context["ddx"] = ddx
            
            # 3. 构建/更新problem_list（工作清单）
            problem_list = self._build_or_update_problem_list(patient_state, ddx)
            
            # 4. 识别信息缺口
            information_gaps = self.gap_identifier.identify_gaps(patient_state, ddx)
            
            # 5. 计算信息完整度
            completeness = self.completeness_calculator.calculate_completeness(patient_state)
            
            # 6. 生成追问策略
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
            
            # 7. 生成自然语言问题
            natural_question = await self.nlg.generate_question(question_info, context)
            
            # 8. 更新对话历史
            self._update_conversation_history(request.cdpId, "assistant", natural_question)
            
            # 9. 注意：problem_list已构建，但由于diagnosis-service目前没有公开的CDP更新接口
            # 实际更新需要通过diagnosis-service的内部方法完成
            # 这里只记录日志，不进行HTTP调用
            if problem_list:
                logger.debug(f"已构建problem_list: cdpId={request.cdpId}, completeness={problem_list.get('completeness')}")
            
            # 10. 构建响应
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
            
            # 7. 重新获取CDP数据以获取更新后的patient_state
            updated_cdp_data = await self._get_cdp_data(request.cdpId)
            if updated_cdp_data:
                # 注意：diagnosis-service返回的是patientState（驼峰命名）
                updated_patient_state = updated_cdp_data.get("patientState") or updated_cdp_data.get("patient_state", {})
                ddx = updated_cdp_data.get("ddx", [])
                
                # 构建/更新problem_list（工作清单）
                # 注意：problem_list已构建，但由于diagnosis-service目前没有公开的CDP更新接口
                # 实际更新需要通过diagnosis-service的内部方法完成
                problem_list = self._build_or_update_problem_list(updated_patient_state, ddx)
                if problem_list:
                    logger.debug(f"已构建problem_list: cdpId={request.cdpId}, completeness={problem_list.get('completeness')}")
            
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
        try:
            # 1. 获取patient_state（优先使用请求中的，否则从CDP读取）
            if request.patientState:
                # 如果请求中提供了patientState，直接使用
                patient_state = request.patientState
                ddx = []  # 如果没有提供ddx，从CDP读取
                cdp_data = await self._get_cdp_data(request.cdpId)
                if cdp_data:
                    ddx = cdp_data.get("ddx", [])
            else:
                # 否则从CDP读取
                cdp_data = await self._get_cdp_data(request.cdpId)
                if not cdp_data:
                    raise BusinessException(1205, f"CDP不存在: {request.cdpId}")
                # 注意：diagnosis-service返回的是patientState（驼峰命名），不是patient_state
                patient_state = cdp_data.get("patientState") or cdp_data.get("patient_state", {})
                ddx = cdp_data.get("ddx", [])
            
            # 调试信息：输出到控制台（不写入日志文件）
            print(f"\n[DEBUG] ===== identify_gaps调用 =====")
            print(f"[DEBUG] cdpId={request.cdpId}")
            print(f"[DEBUG] patient_state类型: {type(patient_state)}")
            if isinstance(patient_state, dict):
                print(f"[DEBUG] patient_state的keys: {list(patient_state.keys())}")
                if "symptoms" in patient_state:
                    print(f"[DEBUG] symptoms内容: {patient_state.get('symptoms')}")
                else:
                    print(f"[DEBUG] ⚠️ patient_state中没有symptoms字段！")
                    print(f"[DEBUG] patient_state完整内容: {patient_state}")
            
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
            
            # 调试信息：输出到控制台
            print(f"\n[DEBUG] ===== _get_cdp_data调用 =====")
            print(f"[DEBUG] URL: {url}")
            print(f"[DEBUG] 响应code: {result.get('code')}")
            print(f"[DEBUG] 响应data的keys: {list(result.get('data', {}).keys()) if result.get('data') else 'None'}")
            if result.get("data") and isinstance(result.get("data"), dict):
                data = result.get("data", {})
                if "patientState" in data:
                    print(f"[DEBUG] patientState类型: {type(data.get('patientState'))}")
                    print(f"[DEBUG] patientState内容: {data.get('patientState')}")
                elif "patient_state" in data:
                    print(f"[DEBUG] patient_state类型: {type(data.get('patient_state'))}")
                    print(f"[DEBUG] patient_state内容: {data.get('patient_state')}")
                else:
                    print(f"[DEBUG] ⚠️ 响应data中没有patientState或patient_state字段！")
                    print(f"[DEBUG] 响应data完整内容: {data}")
            
            if result.get("code") == 200:
                return result.get("data", {})
            return None
        except Exception as e:
            logger.warning(f"获取CDP数据失败: {str(e)}")
            print(f"[DEBUG] ⚠️ 获取CDP数据异常: {str(e)}")
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
    
    def _build_or_update_problem_list(
        self, 
        patient_state: Dict[str, Any], 
        ddx: List[Dict] = None
    ) -> Dict[str, Any]:
        """
        构建或更新问题清单（工作清单）
        从symptoms中提取信息，构建problem_list
        
        Args:
            patient_state: 患者状态
            ddx: 诊断候选集（可选）
            
        Returns:
            问题清单
        """
        symptoms = patient_state.get("symptoms", [])
        existing_problem_list = patient_state.get("problem_list", {})
        
        # 如果已有problem_list，则更新；否则新建
        problem_list = existing_problem_list.copy() if existing_problem_list else {}
        
        # 从symptoms中提取主诉（如果还没有）
        if not problem_list.get("chief_complaint") and symptoms:
            first_symptom = symptoms[0]
            if isinstance(first_symptom, dict) and first_symptom.get("name"):
                problem_list["chief_complaint"] = {
                    "name": first_symptom.get("name"),
                    "duration": first_symptom.get("duration"),
                    "location": first_symptom.get("location"),
                    "severity": first_symptom.get("severity"),
                    "trigger": first_symptom.get("trigger"),
                    "frequency": first_symptom.get("frequency")
                }
        
        # 识别信息缺口
        information_gaps = self.gap_identifier.identify_gaps(patient_state, ddx)
        problem_list["information_gaps"] = {
            "required": information_gaps.get("required", []),
            "important": information_gaps.get("important", []),
            "optional": information_gaps.get("optional", [])
        }
        
        # 计算完整度
        completeness = self.completeness_calculator.calculate_completeness(patient_state)
        problem_list["completeness"] = completeness
        
        return problem_list
    
    async def _update_cdp_problem_list(self, cdp_id: str, problem_list: Dict[str, Any]):
        """
        更新CDP的problem_list字段（通过diagnosis-service）
        注意：由于diagnosis-service没有单独的更新接口，我们需要将problem_list合并到patient_state中一起更新
        """
        try:
            # 先获取当前的CDP数据
            cdp_data = await self._get_cdp_data(cdp_id)
            if not cdp_data:
                logger.warning(f"无法获取CDP数据，跳过problem_list更新: cdpId={cdp_id}")
                return
            
            # 获取当前的patient_state
            current_patient_state = cdp_data.get("patient_state", {})
            if not isinstance(current_patient_state, dict):
                current_patient_state = {}
            
            # 将problem_list合并到patient_state中
            updated_patient_state = current_patient_state.copy()
            updated_patient_state["problem_list"] = problem_list
            
            # 通过现有的_update_cdp方法更新（注意：这个方法目前使用的接口也不存在，但至少逻辑一致）
            # 由于diagnosis-service没有公开的更新接口，这里只记录日志
            # 实际更新应该通过diagnosis-service的内部方法完成
            logger.debug(f"准备更新CDP的problem_list: cdpId={cdp_id}, problem_list={problem_list}")
            logger.warning(f"注意：diagnosis-service目前没有公开的CDP更新接口，problem_list更新需要等待接口实现")
            
        except Exception as e:
            logger.warning(f"更新CDP的problem_list失败: {str(e)}")
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
