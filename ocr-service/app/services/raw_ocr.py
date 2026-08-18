"""
非临床 RAW OCR 引擎边界。

只负责图像解码/预处理、Tesseract 调用与技术失败区分。
不得包含报告类型、指标、参考范围、状态判定或患者信息解析。
不得依赖 FastAPI、Shared Contracts 或 Python Runtime。
"""

from __future__ import annotations

import io
import logging
import cv2
import numpy as np
import pytesseract
from PIL import Image
from pytesseract.pytesseract import TesseractNotFoundError

logger = logging.getLogger(__name__)

# 与遗留识别路径的历史语言配置保持一致。
DEFAULT_OCR_LANGUAGE = "chi_sim+eng"


class RawOcrEngineUnavailableError(Exception):
    """Tesseract 引擎不可用（未安装或无法定位二进制）。"""

    error_code = "RAW_OCR_ENGINE_UNAVAILABLE"


class RawOcrExecutionFailedError(Exception):
    """OCR 执行失败（引擎已可见，但本次识别未完成）。"""

    error_code = "RAW_OCR_EXECUTION_FAILED"


class RawOcrEngine:
    """显式可复用的 RAW OCR 工程实现。"""

    def preprocess_image(self, image: Image.Image) -> Image.Image:
        """
        图片预处理。算法与历史遗留预处理保持一致，不做优化。
        """
        # 转换为 numpy 数组
        image_array = np.array(image)

        # 如果是彩色图片，转换为灰度图
        if len(image_array.shape) == 3:
            gray_image = cv2.cvtColor(image_array, cv2.COLOR_RGB2GRAY)
        else:
            gray_image = image_array

        # 二值化
        _, binary_image = cv2.threshold(
            gray_image,
            0,
            255,
            cv2.THRESH_BINARY + cv2.THRESH_OTSU,
        )

        # 去噪
        denoised_image = cv2.fastNlMeansDenoising(binary_image, None, 10, 7, 21)

        # 对比度增强
        enhanced_image = cv2.convertScaleAbs(denoised_image, alpha=1.5, beta=0)

        # 转换回 PIL Image
        return Image.fromarray(enhanced_image)

    def recognize_raw_text(
        self,
        image: Image.Image,
        language: str = DEFAULT_OCR_LANGUAGE,
    ) -> str:
        """
        对已准备好的图像调用 Tesseract，返回原始文本。

        成功但无字返回空串；引擎不可用与执行失败必须抛出技术异常，不得坍缩为空串。
        本方法不执行预处理，以免遗留 recognize() 双重处理。
        """
        try:
            recognized_text = pytesseract.image_to_string(image, lang=language)
        except TesseractNotFoundError as unavailable_error:
            logger.error("RAW OCR 引擎不可用: %s", unavailable_error)
            raise RawOcrEngineUnavailableError(
                "Tesseract OCR engine is unavailable"
            ) from unavailable_error
        except FileNotFoundError as missing_binary_error:
            logger.error("RAW OCR 引擎二进制不存在: %s", missing_binary_error)
            raise RawOcrEngineUnavailableError(
                "Tesseract OCR engine is unavailable"
            ) from missing_binary_error
        except Exception as execution_error:
            logger.error("RAW OCR 执行失败: %s", execution_error)
            raise RawOcrExecutionFailedError(
                "Tesseract OCR execution failed"
            ) from execution_error

        return recognized_text

    def recognize_from_bytes(
        self,
        image_bytes: bytes,
        language: str = DEFAULT_OCR_LANGUAGE,
    ) -> str:
        """
        从字节解码图像后做历史同构预处理，再识别原始文本。
        不依赖 FastAPI UploadFile。
        """
        try:
            decoded_image = Image.open(io.BytesIO(image_bytes))
        except Exception as decode_error:
            raise RawOcrExecutionFailedError(
                "Unable to decode image bytes for RAW OCR"
            ) from decode_error

        processed_image = self.preprocess_image(decoded_image)
        return self.recognize_raw_text(processed_image, language=language)
