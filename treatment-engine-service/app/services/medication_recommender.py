"""
药物推荐器
"""
from typing import Dict, Any, List, Set
import logging

logger = logging.getLogger(__name__)


class MedicationRecommender:
    """药物推荐器"""
    
    def __init__(self):
        """初始化药物推荐器"""
        # 药物知识库（初期使用简单规则，后期可扩展为知识库）
        self.medication_library = self._load_medication_library()
        
        # 药物相互作用库
        self.drug_interactions = self._load_drug_interactions()
    
    def _load_medication_library(self) -> Dict[str, Dict[str, Any]]:
        """
        加载药物知识库
        格式: {disease: [medications]}
        
        Returns:
            药物知识库字典
        """
        # 示例规则：疾病 -> 药物列表
        library = {
            "急性心肌梗死": [
                {
                    "name": "阿司匹林",
                    "indication": "抗血小板聚集",
                    "contraindications": ["对阿司匹林过敏", "活动性出血"],
                    "drugInteractions": [],
                    "note": "注意：患者已在使用，需确认剂量"
                },
                {
                    "name": "氯吡格雷",
                    "indication": "抗血小板聚集",
                    "contraindications": [],
                    "drugInteractions": ["与阿司匹林联用需注意出血风险"],
                    "note": "建议与阿司匹林双联抗血小板治疗"
                },
                {
                    "name": "阿托伐他汀",
                    "indication": "降脂稳定斑块",
                    "contraindications": ["活动性肝病"],
                    "drugInteractions": [],
                    "note": ""
                }
            ],
            "不稳定心绞痛": [
                {
                    "name": "阿司匹林",
                    "indication": "抗血小板聚集",
                    "contraindications": ["对阿司匹林过敏"],
                    "drugInteractions": [],
                    "note": ""
                },
                {
                    "name": "美托洛尔",
                    "indication": "降低心率，减少心肌耗氧",
                    "contraindications": ["严重心动过缓", "严重心衰"],
                    "drugInteractions": [],
                    "note": ""
                }
            ],
            "肺炎": [
                {
                    "name": "阿莫西林",
                    "indication": "抗感染",
                    "contraindications": ["对青霉素过敏"],
                    "drugInteractions": [],
                    "note": ""
                },
                {
                    "name": "左氧氟沙星",
                    "indication": "抗感染",
                    "contraindications": ["对喹诺酮类过敏", "18岁以下"],
                    "drugInteractions": [],
                    "note": ""
                }
            ],
            "高血压": [
                {
                    "name": "依那普利",
                    "indication": "降压",
                    "contraindications": ["妊娠", "双侧肾动脉狭窄"],
                    "drugInteractions": [],
                    "note": ""
                },
                {
                    "name": "氨氯地平",
                    "indication": "降压",
                    "contraindications": [],
                    "drugInteractions": [],
                    "note": ""
                }
            ]
        }
        return library
    
    def _load_drug_interactions(self) -> Dict[str, List[str]]:
        """
        加载药物相互作用库
        格式: {drug1: [interacting_drugs]}
        
        Returns:
            药物相互作用字典
        """
        # 示例相互作用规则
        interactions = {
            "阿司匹林": ["华法林", "氯吡格雷"],
            "氯吡格雷": ["阿司匹林", "华法林"],
            "华法林": ["阿司匹林", "氯吡格雷"]
        }
        return interactions
    
    def _check_contraindications(
        self,
        medication: Dict[str, Any],
        patient_state: Dict[str, Any]
    ) -> bool:
        """
        检查药物禁忌症
        
        Args:
            medication: 药物信息
            patient_state: 患者状态
            
        Returns:
            True表示有禁忌症，False表示无禁忌症
        """
        contraindications = medication.get("contraindications", [])
        allergies = patient_state.get("allergies", [])
        current_medications = patient_state.get("current_medications", [])
        
        # 检查过敏史
        medication_name = medication.get("name", "")
        for allergy in allergies:
            if allergy in medication_name or medication_name in allergy:
                logger.warning(f"患者对{medication_name}过敏，存在禁忌症")
                return True
        
        # 检查禁忌症列表
        for contraindication in contraindications:
            # 简化检查：如果禁忌症关键词在患者状态中，则认为有禁忌
            # 实际应用中需要更复杂的匹配逻辑
            if any(keyword in str(patient_state).lower() for keyword in contraindication.lower().split()):
                logger.warning(f"药物{medication_name}存在禁忌症: {contraindication}")
                return True
        
        return False
    
    def _check_drug_interactions(
        self,
        medication: Dict[str, Any],
        patient_state: Dict[str, Any]
    ) -> List[str]:
        """
        检查药物相互作用
        
        Args:
            medication: 药物信息
            patient_state: 患者状态
            
        Returns:
            相互作用列表
        """
        interactions = []
        medication_name = medication.get("name", "")
        current_medications = patient_state.get("current_medications", [])
        
        # 检查与当前用药的相互作用
        drug_interactions = self.drug_interactions.get(medication_name, [])
        for current_med in current_medications:
            if current_med in drug_interactions:
                interactions.append(f"与{current_med}联用需注意相互作用")
        
        # 检查药物信息中的相互作用提示
        medication_interactions = medication.get("drugInteractions", [])
        interactions.extend(medication_interactions)
        
        return interactions
    
    def recommend(
            self,
            diagnosis: Dict[str, Any],
            patient_state: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        推荐药物
        基于诊断和患者情况，推荐合适的药物
        
        Args:
            diagnosis: 诊断信息（包含disease字段）
            patient_state: 患者状态（包含allergies、current_medications等）
            
        Returns:
            药物推荐列表
        """
        logger.info("推荐药物")
        
        disease = diagnosis.get("disease", "")
        if not disease:
            logger.warning("诊断疾病为空，无法推荐药物")
            return []
        
        # 从药物知识库中获取相关药物
        medications = self.medication_library.get(disease, [])
        
        if not medications:
            logger.warning(f"未找到{disease}的药物推荐规则")
            return []
        
        # 筛选和验证药物
        recommended_medications = []
        
        for medication in medications:
            # 检查禁忌症
            if self._check_contraindications(medication, patient_state):
                logger.info(f"药物{medication.get('name')}存在禁忌症，跳过推荐")
                continue
            
            # 检查药物相互作用
            interactions = self._check_drug_interactions(medication, patient_state)
            
            # 构建药物推荐项
            medication_advice = {
                "name": medication.get("name", ""),
                "indication": medication.get("indication", ""),
                "contraindications": medication.get("contraindications", []),
                "drugInteractions": interactions,
                "note": medication.get("note", "")
            }
            
            # 如果患者已在使用的药物，添加提示
            current_medications = patient_state.get("current_medications", [])
            if medication_advice["name"] in current_medications:
                if not medication_advice["note"]:
                    medication_advice["note"] = "注意：患者已在使用，需确认剂量"
                else:
                    medication_advice["note"] += "；患者已在使用，需确认剂量"
            
            recommended_medications.append(medication_advice)
        
        # 按适应症匹配度排序（简化处理，实际可更复杂）
        recommended_medications.sort(key=lambda x: len(x.get("indication", "")), reverse=True)
        
        logger.info(f"为{disease}推荐了{len(recommended_medications)}种药物")
        return recommended_medications

