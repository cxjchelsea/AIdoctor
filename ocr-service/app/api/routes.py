"""
OCR服务路由
"""
from fastapi import APIRouter, UploadFile, File, HTTPException
from app.services.ocr_service import OcrService
from typing import Dict
import time
import logging

logger = logging.getLogger(__name__)

router = APIRouter()
ocr_service = OcrService()


@router.post("/ocr/recognize")
async def recognize(file: UploadFile = File(...)) -> Dict:
    """
    OCR识别报告
    识别检查报告图片中的文字，并提取结构化数据
    """
    try:
        # 验证文件格式
        if not file.content_type or not file.content_type.startswith("image/"):
            raise HTTPException(
                status_code=400,
                detail="文件格式不支持，请上传图片文件"
            )
        
        # 验证文件大小（限制10MB）
        file_size = 0
        content = await file.read()
        file_size = len(content)
        await file.seek(0)  # 重置文件指针
        
        if file_size > 10 * 1024 * 1024:  # 10MB
            raise HTTPException(
                status_code=400,
                detail="文件大小超过限制（10MB）"
            )
        
        if file_size == 0:
            raise HTTPException(
                status_code=400,
                detail="文件为空"
            )
        
        # OCR识别
        result = await ocr_service.recognize(file)
        
        return {
            "code": 200,
            "message": "success",
            "data": result,
            "timestamp": int(time.time() * 1000)
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"OCR识别失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"OCR识别失败: {str(e)}"
        )
