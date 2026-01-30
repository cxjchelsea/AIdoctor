"""
OCR服务
"""
from typing import Dict, Any, List, Optional
import logging
import io
import re
from PIL import Image
import pytesseract
import cv2
import numpy as np

logger = logging.getLogger(__name__)


class OcrService:
    """OCR服务"""
    
    def __init__(self):
        """初始化OCR服务"""
        # 检查报告格式模板（初期使用简单规则，后期可扩展）
        self.report_templates = self._load_report_templates()
        
        # 指标名称映射（用于结构化提取）
        self.indicator_mapping = self._load_indicator_mapping()
    
    def _load_report_templates(self) -> Dict[str, Dict[str, Any]]:
        """
        加载检查报告格式模板
        
        Returns:
            报告格式模板字典
        """
        # 示例模板：报告类型 -> 格式规则
        templates = {
            "血常规": {
                "indicators": ["白细胞", "红细胞", "血红蛋白", "血小板"],
                "pattern": r"(\w+)[:：]\s*([\d.]+)\s*([^\s]+)"
            },
            "尿常规": {
                "indicators": ["白细胞", "红细胞", "蛋白质", "葡萄糖"],
                "pattern": r"(\w+)[:：]\s*([\d.]+)\s*([^\s]+)"
            },
            "生化": {
                "indicators": ["血糖", "总胆固醇", "甘油三酯", "肌酐"],
                "pattern": r"(\w+)[:：]\s*([\d.]+)\s*([^\s]+)"
            }
        }
        return templates
    
    def _load_indicator_mapping(self) -> Dict[str, Dict[str, str]]:
        """
        加载指标名称映射
        格式: {indicator_name: {code, unit, normal_range}}
        
        Returns:
            指标映射字典
        """
        # 示例映射：指标名称 -> 编码、单位、参考范围
        mapping = {
            "白细胞": {"code": "WBC", "unit": "10^9/L", "normal_range": "4-10"},
            "红细胞": {"code": "RBC", "unit": "10^12/L", "normal_range": "3.5-5.5"},
            "血红蛋白": {"code": "HGB", "unit": "g/L", "normal_range": "120-160"},
            "血小板": {"code": "PLT", "unit": "10^9/L", "normal_range": "100-300"},
            "血糖": {"code": "GLU", "unit": "mmol/L", "normal_range": "3.9-6.1"},
            "总胆固醇": {"code": "TC", "unit": "mmol/L", "normal_range": "3.1-5.7"},
            "肌酐": {"code": "CREA", "unit": "μmol/L", "normal_range": "44-133"}
        }
        return mapping
    
    def _preprocess_image(self, image: Image.Image) -> Image.Image:
        """
        图片预处理
        包括灰度化、二值化、去噪、倾斜校正等
        
        Args:
            image: PIL图片对象
            
        Returns:
            预处理后的图片
        """
        logger.info("开始图片预处理")
        
        # 转换为numpy数组
        img_array = np.array(image)
        
        # 如果是彩色图片，转换为灰度图
        if len(img_array.shape) == 3:
            img_gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        else:
            img_gray = img_array
        
        # 二值化
        _, img_binary = cv2.threshold(img_gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        
        # 去噪
        img_denoised = cv2.fastNlMeansDenoising(img_binary, None, 10, 7, 21)
        
        # 对比度增强
        img_enhanced = cv2.convertScaleAbs(img_denoised, alpha=1.5, beta=0)
        
        # 转换回PIL Image
        processed_image = Image.fromarray(img_enhanced)
        
        logger.info("图片预处理完成")
        return processed_image
    
    def _ocr_recognize(self, image: Image.Image) -> str:
        """
        使用OCR引擎识别文字
        
        Args:
            image: PIL图片对象
            
        Returns:
            识别的文字
        """
        logger.info("开始OCR识别")
        
        try:
            # 使用Tesseract OCR识别
            # 注意：需要系统安装Tesseract OCR
            text = pytesseract.image_to_string(image, lang='chi_sim+eng')
            
            logger.info(f"OCR识别完成，识别文字长度: {len(text)}")
            return text
            
        except Exception as e:
            logger.error(f"OCR识别失败: {str(e)}")
            # 如果Tesseract不可用，返回空字符串
            return ""
    
    def _parse_indicator_value(self, text: str, indicator_name: str) -> Optional[Dict[str, Any]]:
        """
        从文本中解析指标值
        
        Args:
            text: 文本内容
            indicator_name: 指标名称
            
        Returns:
            指标信息字典，如果未找到则返回None
        """
        # 构建匹配模式
        patterns = [
            rf"{indicator_name}[:：]\s*([\d.]+)\s*([^\s]+)",  # 指标名: 数值 单位
            rf"{indicator_name}\s+([\d.]+)\s+([^\s]+)",  # 指标名 数值 单位
            rf"{indicator_name}[\(（][^\)）]*[\)）][:：]?\s*([\d.]+)",  # 指标名(英文): 数值
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                value_str = match.group(1)
                unit = match.group(2) if len(match.groups()) > 1 else ""
                
                try:
                    value = float(value_str)
                    
                    # 获取指标映射信息
                    indicator_info = self.indicator_mapping.get(indicator_name, {})
                    
                    # 判断状态
                    normal_range = indicator_info.get("normal_range", "")
                    status = self._determine_status(value, normal_range)
                    
                    return {
                        "name": indicator_name,
                        "code": indicator_info.get("code", ""),
                        "value": value,
                        "unit": unit or indicator_info.get("unit", ""),
                        "normal_range": normal_range,
                        "status": status
                    }
                except ValueError:
                    continue
        
        return None
    
    def _determine_status(self, value: float, normal_range: str) -> str:
        """
        根据数值和参考范围判断状态
        
        Args:
            value: 数值
            normal_range: 参考范围（格式：min-max）
            
        Returns:
            状态（normal/abnormal/high/low）
        """
        if not normal_range:
            return "normal"
        
        # 解析参考范围
        try:
            if "-" in normal_range:
                parts = normal_range.split("-")
                min_val = float(parts[0])
                max_val = float(parts[1])
                
                if value < min_val:
                    return "low"
                elif value > max_val:
                    return "high"
                else:
                    return "normal"
        except (ValueError, IndexError):
            pass
        
        return "normal"
    
    def _identify_report_type(self, text: str) -> str:
        """
        识别检查报告类型
        
        Args:
            text: OCR识别的文本
            
        Returns:
            报告类型
        """
        # 根据关键词识别报告类型
        if "血常规" in text or "WBC" in text or "RBC" in text:
            return "血常规"
        elif "尿常规" in text or "尿液" in text:
            return "尿常规"
        elif "生化" in text or "血糖" in text or "胆固醇" in text:
            return "生化"
        else:
            return "未知"
    
    async def recognize(self, image_file) -> Dict[str, Any]:
        """
        识别检查报告
        包括图片预处理、OCR识别、结构化提取
        
        Args:
            image_file: 上传的图片文件
            
        Returns:
            识别结果，包含原始文本和结构化数据
        """
        logger.info("开始OCR识别")
        
        try:
            # 1. 读取图片
            image_bytes = await image_file.read()
            image = Image.open(io.BytesIO(image_bytes))
            
            # 2. 图片预处理
            processed_image = self._preprocess_image(image)
            
            # 3. OCR识别
            raw_text = self._ocr_recognize(processed_image)
            
            if not raw_text:
                logger.warning("OCR识别结果为空")
                return {
                    "raw_text": "",
                    "structured_data": {}
                }
            
            # 4. 结构化提取
            structured_data = await self.extract_structured_data(raw_text)
            
            logger.info("OCR识别完成")
            return {
                "raw_text": raw_text,
                "structured_data": structured_data
            }
            
        except Exception as e:
            logger.error(f"OCR识别失败: {str(e)}", exc_info=True)
            raise
    
    async def extract_structured_data(self, text: str) -> Dict[str, Any]:
        """
        提取结构化数据
        从OCR识别的文本中提取检查指标、数值、单位、参考范围等
        
        Args:
            text: OCR识别的文本
            
        Returns:
            结构化数据
        """
        logger.info("开始结构化数据提取")
        
        if not text:
            return {}
        
        # 识别报告类型
        report_type = self._identify_report_type(text)
        
        # 获取报告模板
        template = self.report_templates.get(report_type, {})
        indicators = template.get("indicators", [])
        
        # 如果没有找到模板，尝试从指标映射中提取所有指标
        if not indicators:
            indicators = list(self.indicator_mapping.keys())
        
        # 提取指标
        extracted_indicators = []
        for indicator_name in indicators:
            indicator_data = self._parse_indicator_value(text, indicator_name)
            if indicator_data:
                extracted_indicators.append(indicator_data)
        
        # 提取检查日期（简化处理）
        date_pattern = r"(\d{4}[-年]\d{1,2}[-月]\d{1,2}[日]?)"
        date_match = re.search(date_pattern, text)
        examination_date = date_match.group(1) if date_match else None
        
        # 提取患者信息（简化处理）
        patient_info = {}
        name_pattern = r"姓名[:：]\s*([^\s]+)"
        name_match = re.search(name_pattern, text)
        if name_match:
            patient_info["name"] = name_match.group(1)
        
        age_pattern = r"年龄[:：]\s*(\d+)"
        age_match = re.search(age_pattern, text)
        if age_match:
            patient_info["age"] = int(age_match.group(1))
        
        gender_pattern = r"性别[:：]\s*([男女])"
        gender_match = re.search(gender_pattern, text)
        if gender_match:
            patient_info["gender"] = gender_match.group(1)
        
        structured_data = {
            "indicators": extracted_indicators,
            "examination_type": report_type,
            "examination_date": examination_date,
            "patient_info": patient_info if patient_info else None
        }
        
        logger.info(f"结构化数据提取完成，提取了{len(extracted_indicators)}个指标")
        return structured_data
