"""
医疗NER模型服务：集成医疗领域NER模型进行实体提取
"""
from typing import List, Dict, Any, Optional, Tuple
from app.config.settings import settings
from app.models.nlu import SymptomEntity
from app.utils.logger import logger

# 尝试导入transformers库
try:
    from transformers import AutoTokenizer, AutoModelForTokenClassification
    import torch
    TRANSFORMERS_AVAILABLE = True
except ImportError:
    TRANSFORMERS_AVAILABLE = False
    logger.warning("Transformers库未安装，NER模型功能将不可用")


class NERModel:
    """医疗NER模型服务"""
    
    def __init__(
        self,
        model_name: Optional[str] = None,
        device: Optional[str] = None,
        use_onnx: bool = False
    ):
        """
        初始化NER模型
        
        Args:
            model_name: 模型名称或路径，如果为None则从配置读取
            device: 设备（cpu/cuda），如果为None则自动选择
            use_onnx: 是否使用ONNX Runtime加速
        """
        self.nlu_config = settings.nlu
        self.use_ner = getattr(self.nlu_config, 'use_ner', False)
        self.model_name = model_name or getattr(self.nlu_config, 'ner_model_name', None)
        self.device = device or self._get_device()
        self.use_onnx = use_onnx
        
        self.tokenizer = None
        self.model = None
        self.is_loaded = False
        
        # 实体标签映射（BIO标注）
        self.label_map = {
            'O': 0,
            'B-SYMPTOM': 1,
            'I-SYMPTOM': 2,
            'B-DISEASE': 3,
            'I-DISEASE': 4,
            'B-MEDICATION': 5,
            'I-MEDICATION': 6,
            'B-EXAMINATION': 7,
            'I-EXAMINATION': 8
        }
        self.id_to_label = {v: k for k, v in self.label_map.items()}
        
        # 如果启用NER且模型名称已配置，尝试加载模型
        if self.use_ner and self.model_name:
            self._load_model()
    
    def _get_device(self) -> str:
        """自动选择设备"""
        if not TRANSFORMERS_AVAILABLE:
            return "cpu"
        
        if torch.cuda.is_available():
            return "cuda"
        return "cpu"
    
    def _load_model(self):
        """加载NER模型"""
        if not TRANSFORMERS_AVAILABLE:
            logger.warning("Transformers库未安装，无法加载NER模型")
            self.use_ner = False
            return
        
        if not self.model_name:
            logger.warning("NER模型名称未配置，跳过模型加载")
            self.use_ner = False
            return
        
        try:
            logger.info(f"正在加载NER模型: {self.model_name}, device={self.device}")
            
            # 加载tokenizer和模型
            self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
            self.model = AutoModelForTokenClassification.from_pretrained(
                self.model_name,
                num_labels=len(self.label_map)
            )
            self.model.to(self.device)
            self.model.eval()
            
            self.is_loaded = True
            logger.info(f"NER模型加载成功: {self.model_name}")
        except Exception as e:
            logger.error(f"NER模型加载失败: {e}", exc_info=True)
            self.use_ner = False
            self.is_loaded = False
    
    async def extract_entities(self, text: str) -> List[SymptomEntity]:
        """
        使用NER模型提取实体
        
        Args:
            text: 输入文本
            
        Returns:
            提取的实体列表
        """
        if not self.use_ner or not self.is_loaded:
            return []
        
        if not text or not text.strip():
            return []
        
        try:
            # 分词和编码
            inputs = self.tokenizer(
                text,
                return_tensors="pt",
                padding=True,
                truncation=True,
                max_length=512
            )
            inputs = {k: v.to(self.device) for k, v in inputs.items()}
            
            # 模型推理
            with torch.no_grad():
                outputs = self.model(**inputs)
                predictions = torch.argmax(outputs.logits, dim=-1)
            
            # 解码预测结果
            tokens = self.tokenizer.convert_ids_to_tokens(inputs['input_ids'][0])
            labels = predictions[0].cpu().numpy()
            
            # 提取实体
            entities = self._extract_entities_from_labels(tokens, labels, text)
            
            logger.debug(f"NER模型提取到 {len(entities)} 个实体")
            return entities
            
        except Exception as e:
            logger.error(f"NER模型推理失败: {e}", exc_info=True)
            return []
    
    def _extract_entities_from_labels(
        self,
        tokens: List[str],
        labels: List[int],
        original_text: str
    ) -> List[SymptomEntity]:
        """
        从标签序列中提取实体
        
        Args:
            tokens: 分词后的token列表
            labels: 标签ID列表
            original_text: 原始文本
            
        Returns:
            提取的实体列表
        """
        entities = []
        current_entity = None
        current_label = None
        
        for i, (token, label_id) in enumerate(zip(tokens, labels)):
            label = self.id_to_label.get(label_id, 'O')
            
            # 处理B-标签（实体开始）
            if label.startswith('B-'):
                # 保存之前的实体
                if current_entity:
                    entities.append(current_entity)
                
                # 开始新实体
                entity_type = label[2:]  # 去掉'B-'前缀
                current_label = entity_type
                current_entity = {
                    'text': token.replace('##', '').replace('[CLS]', '').replace('[SEP]', ''),
                    'type': entity_type,
                    'start': i
                }
            
            # 处理I-标签（实体继续）
            elif label.startswith('I-') and current_entity:
                entity_type = label[2:]
                if entity_type == current_label:
                    current_entity['text'] += token.replace('##', '').replace('[CLS]', '').replace('[SEP]', '')
                else:
                    # 标签不匹配，结束当前实体
                    entities.append(current_entity)
                    current_entity = None
                    current_label = None
            
            # 处理O标签（非实体）
            else:
                if current_entity:
                    entities.append(current_entity)
                    current_entity = None
                    current_label = None
        
        # 处理最后一个实体
        if current_entity:
            entities.append(current_entity)
        
        # 转换为SymptomEntity列表（目前主要关注症状）
        symptom_entities = []
        for entity in entities:
            if entity['type'] == 'SYMPTOM':
                # 尝试在原始文本中找到实体位置
                entity_text = entity['text'].strip()
                if entity_text and entity_text in original_text:
                    symptom_entities.append(SymptomEntity(
                        original_text=entity_text,
                        standard_term=entity_text,  # NER模型不提供标准术语，需要后续归一化
                        confidence=0.7,  # NER模型的默认置信度
                        context={
                            'source': 'ner_model',
                            'model_name': self.model_name
                        }
                    ))
        
        return symptom_entities
    
    def is_available(self) -> bool:
        """
        检查NER模型是否可用
        
        Returns:
            是否可用
        """
        return self.use_ner and self.is_loaded


class SimpleNERModel:
    """
    简单的NER模型实现（用于测试和降级）
    基于规则和关键词匹配，不依赖外部模型
    """
    
    def __init__(self):
        """初始化简单NER模型"""
        # 症状关键词模式
        self.symptom_patterns = [
            r'([^，。！？\s]+(?:痛|疼|不适|难受|不舒服|异常|症状|困扰|问题))',
        ]
        import re
        self.re = re
    
    async def extract_entities(self, text: str) -> List[SymptomEntity]:
        """
        使用简单规则提取实体
        
        Args:
            text: 输入文本
            
        Returns:
            提取的实体列表
        """
        entities = []
        
        for pattern in self.symptom_patterns:
            matches = self.re.findall(pattern, text)
            for match in matches:
                if "健康" not in match and "体检" not in match:
                    entities.append(SymptomEntity(
                        original_text=match,
                        standard_term=match,
                        confidence=0.5,  # 简单规则的置信度较低
                        context={'source': 'simple_ner'}
                    ))
        
        return entities
    
    def is_available(self) -> bool:
        """简单NER模型始终可用"""
        return True

