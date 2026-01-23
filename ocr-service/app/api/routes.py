"""
OCR服务路由
"""
from fastapi import APIRouter, UploadFile, File
from app.services.ocr_service import OcrService
from typing import Dict

router = APIRouter()
ocr_service = OcrService()


@router.post("/recognize")
async def recognize(file: UploadFile = File(...)) -> Dict:
    """OCR识别报告"""
    result = await ocr_service.recognize(file)
    return {
        "code": 200,
        "message": "success",
        "data": result,
        "timestamp": 0
    }
