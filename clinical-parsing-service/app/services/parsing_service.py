"""
病例理解服务（tool_1）
"""
import logging
from app.models.request import ClinicalParsingRequest
from app.models.response import (
    ClinicalParsingResponse, 
    NormalizedConcept, 
    StructuredData,
    Symptom,
    Sign,
    Examination,
    MedicalHistory,
    Medication,
    Allergy,
    AmbiguousExpression
)
from app.utils.vocabulary_loader import VocabularyLoader
from app.services.concept_recognizer import ConceptRecognizer
from app.services.extractor import StructuredExtractor
from app.services.ambiguity_detector import AmbiguityDetector
from app.config.settings import settings
from app.utils.specific_exceptions import (
    MedicalConceptRecognitionFailedException,
    ConceptNormalizationFailedException,
    StructuredExtractionFailedException,
    AmbiguityDeterminationFailedException
)

logger = logging.getLogger(__name__)


class ClinicalParsingService:
    """病例理解服务"""
    
    def __init__(self):
        """初始化服务"""
        # 初始化词表加载器
        self.vocabulary_loader = VocabularyLoader(settings.vocabulary_base_path)
        # 加载所有词表
        self.vocabulary_loader.load_all_vocabularies()
        
        # 初始化各个组件
        self.concept_recognizer = ConceptRecognizer(self.vocabulary_loader)
        self.extractor = StructuredExtractor()
        self.ambiguity_detector = AmbiguityDetector()
    
    async def parse(self, request: ClinicalParsingRequest) -> ClinicalParsingResponse:
        """
        病例理解与结构化
        将非结构化的患者信息转换为结构化的临床要素
        """
        try:
            text = request.text
            logger.info(f"收到解析请求: text={text}, userId={request.userId}, sessionId={request.sessionId}")
            if not text:
                logger.warning("输入文本为空")
                return self._create_empty_response()
            
            # Step 1: 医学概念识别
            try:
                concepts = self.concept_recognizer.recognize(text)
                logger.info(f"识别到 {len(concepts)} 个医学概念: {concepts}")
            except Exception as e:
                logger.error(f"医学概念识别失败: {e}", exc_info=True)
                raise MedicalConceptRecognitionFailedException(str(e))
            
            # Step 2: 概念归一化（在识别过程中已完成）
            # 将识别结果转换为响应格式
            normalized_concepts = []
            for concept in concepts:
                normalized_concept = NormalizedConcept(
                    originalText=concept.get('original_text', ''),
                    normalizedSymptom=concept.get('standard_term', ''),
                    cui=concept.get('cui') or None,
                    icd=concept.get('icd') or None,
                    snomed=concept.get('snomed') or None,
                    loinc=concept.get('loinc') or None,
                    atc=concept.get('atc') or None,
                    confidence=concept.get('confidence', 0.0),
                    conceptType=concept.get('concept_type', '')
                )
                normalized_concepts.append(normalized_concept)
            
            # Step 3: 结构化提取
            try:
                symptoms = self.extractor.extract_symptoms(text, concepts)
                diseases = self.extractor.extract_diseases(text, concepts)
                medications = self.extractor.extract_medications(text, concepts)
                examinations = self.extractor.extract_examinations(text, concepts)
                allergies = self.extractor.extract_allergies(text, concepts)
                
                # 转换为响应格式
                structured_symptoms = [
                    Symptom(
                        name=s.get('name', ''),
                        cui=s.get('cui') or None,
                        duration=s.get('duration') or None,
                        severity=s.get('severity') or None,
                        trigger=s.get('trigger') or None,
                        location=s.get('location') or None
                    )
                    for s in symptoms
                ]
                
                structured_diseases = [
                    MedicalHistory(
                        disease=d.get('disease', ''),
                        icd=d.get('icd') or None,
                        status=d.get('status', 'past')
                    )
                    for d in diseases
                ]
                
                structured_medications = [
                    Medication(
                        name=m.get('name', ''),
                        atc=m.get('atc') or None,
                        status=m.get('status', 'past')
                    )
                    for m in medications
                ]
                
                structured_examinations = [
                    Examination(
                        name=e.get('name', ''),
                        loinc=e.get('loinc') or None,
                        result=e.get('result') or None,
                        abnormal=e.get('abnormal', False)
                    )
                    for e in examinations
                ]
                
                structured_allergies = [
                    Allergy(
                        allergen=a.get('allergen', ''),
                        reaction=a.get('reaction') or None
                    )
                    for a in allergies
                ]
                
                structured_data = StructuredData(
                    symptoms=structured_symptoms,
                    signs=[],  # 体征需要进一步实现
                    examinations=structured_examinations,
                    medicalHistory=structured_diseases,
                    medications=structured_medications,
                    allergies=structured_allergies
                )
                
                # 记录提取到的结构化信息
                if structured_symptoms:
                    for symptom in structured_symptoms:
                        symptom_info = f"症状: {symptom.name}"
                        if symptom.duration:
                            symptom_info += f", 持续时间: {symptom.duration}"
                        if symptom.location:
                            symptom_info += f", 部位: {symptom.location}"
                        if symptom.severity:
                            symptom_info += f", 严重度: {symptom.severity}"
                        if symptom.trigger:
                            symptom_info += f", 诱因: {symptom.trigger}"
                        logger.info(f"提取到结构化症状信息: {symptom_info}")
                
            except Exception as e:
                logger.error(f"结构化提取失败: {e}", exc_info=True)
                raise StructuredExtractionFailedException(str(e))
            
            # Step 4: 歧义表达判定
            try:
                ambiguous_list = self.ambiguity_detector.detect(text, concepts)
                ambiguous_expressions = [
                    AmbiguousExpression(
                        text=ae.get('text', ''),
                        type=ae.get('type', 'ambiguous'),
                        suggestedQuestions=ae.get('suggested_questions', [])
                    )
                    for ae in ambiguous_list
                ] if ambiguous_list else None
            except Exception as e:
                logger.error(f"歧义表达判定失败: {e}", exc_info=True)
                raise AmbiguityDeterminationFailedException(str(e))
            
            # 构建响应
            response = ClinicalParsingResponse(
                concepts=normalized_concepts,
                structuredData=structured_data,
                ambiguousExpressions=ambiguous_expressions
            )
            
            logger.info(f"病例理解完成: 识别到 {len(normalized_concepts)} 个概念")
            return response
            
        except (MedicalConceptRecognitionFailedException, 
                ConceptNormalizationFailedException,
                StructuredExtractionFailedException,
                AmbiguityDeterminationFailedException):
            # 重新抛出业务异常
            raise
        except Exception as e:
            logger.error(f"病例理解服务异常: {e}", exc_info=True)
            raise MedicalConceptRecognitionFailedException(f"病例理解失败: {str(e)}")
    
    def _create_empty_response(self) -> ClinicalParsingResponse:
        """创建空响应"""
        return ClinicalParsingResponse(
            concepts=[],
            structuredData=StructuredData(),
            ambiguousExpressions=None
        )

