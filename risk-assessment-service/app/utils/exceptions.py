"""
异常处理
"""
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
import logging

logger = logging.getLogger(__name__)


class RiskAssessmentException(Exception):
    """风险评估服务异常基类"""
    pass


def setup_exception_handlers(app: FastAPI):
    """设置异常处理器"""
    
    @app.exception_handler(RiskAssessmentException)
    async def risk_assessment_exception_handler(request: Request, exc: RiskAssessmentException):
        logger.error(f"风险评估服务异常: {str(exc)}")
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"error": str(exc), "service": "risk-assessment-service"}
        )
    
    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        logger.error(f"请求验证错误: {exc.errors()}")
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={"error": "请求参数验证失败", "details": exc.errors()}
        )

