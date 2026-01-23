"""
异常处理
"""
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
import logging

logger = logging.getLogger(__name__)


class WorkupPlannerException(Exception):
    """检查建议服务异常基类"""
    pass


def setup_exception_handlers(app: FastAPI):
    """设置异常处理器"""
    
    @app.exception_handler(WorkupPlannerException)
    async def workup_planner_exception_handler(request: Request, exc: WorkupPlannerException):
        logger.error(f"检查建议服务异常: {str(exc)}")
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"error": str(exc), "service": "workup-planner-service"}
        )
    
    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        logger.error(f"请求验证错误: {exc.errors()}")
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={"error": "请求参数验证失败", "details": exc.errors()}
        )

