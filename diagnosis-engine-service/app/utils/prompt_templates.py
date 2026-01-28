"""
Prompt模板管理（从公共库导入）
为了向后兼容，保留此文件作为导入代理
支持自定义模板目录（使用服务特定的模板）
"""

from pathlib import Path
from typing import Dict, Any, Optional
import yaml
import logging
from jinja2 import Template
from aidoctor_llm import PromptTemplateManager as BasePromptTemplateManager

logger = logging.getLogger(__name__)

# 扩展PromptTemplateManager以支持服务特定的模板目录
class PromptTemplateManager(BasePromptTemplateManager):
    """诊断引擎服务特定的Prompt模板管理器"""
    
    def __init__(self, templates_dir=None):
        """
        初始化模板管理器
        
        Args:
            templates_dir: 模板文件目录，如果为None则使用服务默认目录
        """
        if templates_dir is None:
            # 使用服务特定的模板目录
            templates_dir = Path(__file__).parent.parent / "config" / "prompt_templates"
        
        super().__init__(templates_dir=str(templates_dir))
    
    def format_explanation(
        self,
        diagnosis: dict,
        evidence: list,
        reasoning_path: str = None
    ) -> str:
        """
        格式化解释生成提示词（向后兼容方法）
        
        Args:
            diagnosis: 诊断结果
            evidence: 支持证据
            reasoning_path: 推理路径（可选）
            
        Returns:
            格式化后的提示词
        """
        return self.format_explanation_generation(
            diagnosis=diagnosis,
            evidence=evidence,
            reasoning_path=reasoning_path
        )
    
    def _load_templates_config(self) -> Dict[str, Any]:
        """加载模板配置文件"""
        config_file = self.templates_dir / "templates.yaml"
        if config_file.exists():
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    return yaml.safe_load(f) or {}
            except Exception as e:
                logger.warning(f"加载模板配置文件失败: {str(e)}，使用内置模板")
                return {}
        return {}
    
    def _get_builtin_templates(self) -> Dict[str, str]:
        """获取内置模板"""
        return {
            "diagnosis_reasoning": """你是一位经验丰富的临床医生。请基于以下信息进行诊断推理：

患者症状：{{ symptoms }}
体征信息：{{ signs }}
上下文信息：{{ context }}

{% if paths %}
知识图谱推理路径：
{{ paths }}
{% endif %}

请按照以下格式输出诊断结果（JSON格式）：
{
    "primary_diagnosis": {
        "disease": "疾病名称",
        "confidence": 0.0-1.0,
        "supporting_evidence": ["证据1", "证据2"],
        "contradicting_evidence": ["反对证据1"]
    },
    "alternative_diagnoses": [
        {
            "disease": "疾病名称",
            "confidence": 0.0-1.0,
            "supporting_evidence": ["证据1"]
        }
    ],
    "critical_exclusions": [
        {
            "disease": "必须排除的疾病",
            "reason": "排除原因",
            "confidence": 0.0-1.0
        }
    ],
    "reasoning": "推理过程说明"
}""",
            
            "path_injection": """基于以下知识图谱推理路径进行诊断：

{{ paths }}

路径评分信息：
{% for path in path_scores %}
路径{{ loop.index }}: {{ path.description }}
- 先验概率: {{ path.scores.prior }}
- 似然评分: {{ path.scores.likelihood }}
- 后验概率: {{ path.scores.posterior }}
{% endfor %}""",
            
            "question_generation": """基于以下信息缺口，生成一个自然、友好的问诊问题：

信息缺口：{{ missing_info }}
上下文：{{ context }}

请生成一个问题，要求：
1. 自然、口语化
2. 友好、专业
3. 针对性强
4. 不超过50字

只输出问题，不要输出其他内容。""",
            
            "explanation_generation": """请为以下诊断结果生成自然、易懂的解释：

诊断结果：{{ diagnosis }}
支持证据：{{ evidence }}
推理路径：{{ reasoning_path }}

要求：
1. 使用通俗易懂的语言
2. 解释诊断依据
3. 说明下一步建议
4. 不超过200字""",
        }
    
    def get_template(self, template_name: str) -> Template:
        """
        获取模板
        
        Args:
            template_name: 模板名称
            
        Returns:
            Jinja2模板对象
        """
        # 首先尝试从文件加载
        template_file = self.templates_dir / f"{template_name}.jinja2"
        if template_file.exists():
            try:
                return self.env.get_template(f"{template_name}.jinja2")
            except Exception as e:
                logger.warning(f"从文件加载模板失败: {template_name}, {str(e)}")
        
        # 使用内置模板
        if template_name in self._builtin_templates:
            return Template(self._builtin_templates[template_name])
        
        raise ValueError(f"模板不存在: {template_name}")
    
    def format(
        self,
        template_name: str,
        **kwargs
    ) -> str:
        """
        格式化模板
        
        Args:
            template_name: 模板名称
            **kwargs: 模板变量
            
        Returns:
            格式化后的提示词
        """
        template = self.get_template(template_name)
        return template.render(**kwargs)
    
    def format_diagnosis_reasoning(
        self,
        symptoms: list,
        signs: Dict[str, Any],
        context: Dict[str, Any],
        paths: Optional[str] = None
    ) -> str:
        """
        格式化诊断推理提示词
        
        Args:
            symptoms: 症状列表
            signs: 体征信息
            context: 上下文信息
            paths: 知识图谱路径（可选）
            
        Returns:
            格式化后的提示词
        """
        return self.format(
            "diagnosis_reasoning",
            symptoms=", ".join(symptoms) if isinstance(symptoms, list) else symptoms,
            signs=signs,
            context=context,
            paths=paths or ""
        )
    
    def format_path_injection(
        self,
        paths: str,
        path_scores: Optional[list] = None
    ) -> str:
        """
        格式化路径注入提示词
        
        Args:
            paths: 路径文本
            path_scores: 路径评分列表（可选）
            
        Returns:
            格式化后的提示词
        """
        return self.format(
            "path_injection",
            paths=paths,
            path_scores=path_scores or []
        )
    
    def format_question_generation(
        self,
        missing_info: str,
        context: Dict[str, Any]
    ) -> str:
        """
        格式化问诊问题生成提示词
        
        Args:
            missing_info: 信息缺口
            context: 上下文信息
            
        Returns:
            格式化后的提示词
        """
        return self.format(
            "question_generation",
            missing_info=missing_info,
            context=context
        )
    
    def format_explanation(
        self,
        diagnosis: Dict[str, Any],
        evidence: list,
        reasoning_path: Optional[str] = None
    ) -> str:
        """
        格式化解释生成提示词
        
        Args:
            diagnosis: 诊断结果
            evidence: 支持证据
            reasoning_path: 推理路径（可选）
            
        Returns:
            格式化后的提示词
        """
        return self.format(
            "explanation_generation",
            diagnosis=diagnosis,
            evidence=evidence,
            reasoning_path=reasoning_path or ""
        )


__all__ = ["PromptTemplateManager"]
