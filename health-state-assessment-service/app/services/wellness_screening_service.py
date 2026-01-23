"""
健康筛查流程服务（A路径）
整合A1-A5步骤
"""
from typing import Dict, Any
# 使用importlib动态导入（因为目录名包含连字符，无法直接import）
import importlib.util
import os

def _import_module(module_name, file_path):
    """动态导入模块"""
    spec = importlib.util.spec_from_file_location(module_name, file_path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

# 获取wellness-screening目录路径
_wellness_screening_dir = os.path.join(os.path.dirname(__file__), 'wellness-screening')

# 动态导入各个模块
_demand_classifier_module = _import_module('demand_classifier', 
    os.path.join(_wellness_screening_dir, 'a1-demand-classifier', 'demand_classifier.py'))
_profile_collector_module = _import_module('profile_collector',
    os.path.join(_wellness_screening_dir, 'a2-profile-collector', 'profile_collector.py'))
_branch_executor_module = _import_module('branch_executor',
    os.path.join(_wellness_screening_dir, 'a3-branch-executor', 'branch_executor.py'))
_result_generator_module = _import_module('result_generator',
    os.path.join(_wellness_screening_dir, 'a4-result-generator', 'result_generator.py'))
_followup_manager_module = _import_module('followup_manager',
    os.path.join(_wellness_screening_dir, 'a5-followup-manager', 'followup_manager.py'))

DemandClassifier = _demand_classifier_module.DemandClassifier
ProfileCollector = _profile_collector_module.ProfileCollector
BranchExecutor = _branch_executor_module.BranchExecutor
ResultGenerator = _result_generator_module.ResultGenerator
FollowupManager = _followup_manager_module.FollowupManager
from app.utils.logger import logger


class WellnessScreeningService:
    """健康筛查流程服务"""
    
    def __init__(self):
        """初始化服务"""
        self.demand_classifier = DemandClassifier()
        self.profile_collector = ProfileCollector()
        self.branch_executor = BranchExecutor()
        self.result_generator = ResultGenerator()
        self.followup_manager = FollowupManager()
    
    def a1_demand_classification(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """
        A1: 需求分类
        
        Args:
            request: 请求数据，包含cdpId、userInput等
            
        Returns:
            需求分类结果
        """
        logger.info(f"A1: 需求分类 - cdpId={request.get('cdpId')}")
        
        user_input = request.get("userInput", "")
        result = self.demand_classifier.classify(user_input)
        
        return {
            "cdpId": request.get("cdpId"),
            "demand_type": result.get("demand_type"),
            "confidence": result.get("confidence"),
            "intent": result.get("intent")
        }
    
    def a2_health_profile_collection(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """
        A2: 收集健康画像
        
        Args:
            request: 请求数据，包含cdpId、patientState等
            
        Returns:
            健康画像收集结果
        """
        logger.info(f"A2: 收集健康画像 - cdpId={request.get('cdpId')}")
        
        patient_state = request.get("patientState", {})
        user_data = {
            "basicInfo": patient_state.get("basicInfo", {}),
            "symptoms": patient_state.get("symptoms", []),
            "userInput": patient_state.get("userInput", ""),
            "vitalSigns": patient_state.get("vitalSigns", {})
        }
        
        result = self.profile_collector.collect(user_data)
        
        return {
            "cdpId": request.get("cdpId"),
            "profile": result.get("profile"),
            "completeness": result.get("completeness"),
            "gaps": result.get("gaps", [])
        }
    
    def a3_branch_execution(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """
        A3: 执行分支
        
        Args:
            request: 请求数据，包含cdpId、wellnessPlan等
            
        Returns:
            分支执行结果
        """
        logger.info(f"A3: 执行分支 - cdpId={request.get('cdpId')}")
        
        wellness_plan = request.get("wellnessPlan", {})
        demand_type = wellness_plan.get("demand_type", "screening")
        profile = wellness_plan.get("profile", {})
        
        result = self.branch_executor.execute(demand_type, profile)
        
        return {
            "cdpId": request.get("cdpId"),
            "branch": result.get("branch"),
            "branch_result": result
        }
    
    def a4_unified_result_generation(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """
        A4: 生成统一结果
        
        Args:
            request: 请求数据，包含cdpId、wellnessPlan等
            
        Returns:
            统一结果
        """
        logger.info(f"A4: 生成统一结果 - cdpId={request.get('cdpId')}")
        
        wellness_plan = request.get("wellnessPlan", {})
        branch_result = wellness_plan.get("branch_result", {})
        
        result = self.result_generator.generate(branch_result)
        
        return {
            "cdpId": request.get("cdpId"),
            "result": result.get("result"),
            "summary": result.get("summary")
        }
    
    def a5_follow_up_setup(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """
        A5: 设置随访
        
        Args:
            request: 请求数据，包含cdpId、wellnessPlan等
            
        Returns:
            随访设置结果
        """
        logger.info(f"A5: 设置随访 - cdpId={request.get('cdpId')}")
        
        wellness_plan = request.get("wellnessPlan", {})
        unified_result = wellness_plan.get("unified_result", {})
        
        result = self.followup_manager.manage(unified_result)
        
        return {
            "cdpId": request.get("cdpId"),
            "followup_plan": result.get("followup_plan"),
            "next_review_date": result.get("next_review_date")
        }

