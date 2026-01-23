"""
病例理解服务主入口（脑区A）
"""
import logging
from fastapi import FastAPI
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers
from app.config.settings import settings

# 配置日志
logging.basicConfig(
    level=getattr(logging, settings.log_level),
    format='[%(asctime)s] [%(levelname)s] [%(name)s] [%(filename)s:%(lineno)d] - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="病例理解服务",
    version=settings.app_version,
    description="AI医生系统的病例理解服务（脑区A：病例理解与结构化）"
)

# 注册路由
app.include_router(router, prefix="/api/v1")

# 设置异常处理
setup_exception_handlers(app)

@app.get("/health")
async def health():
    """健康检查接口"""
    return {
        "status": "healthy",
        "version": settings.app_version,
        "service": "clinical-parsing-service"
    }

@app.on_event("startup")
async def startup_event():
    """服务启动事件"""
    logger.info(f"病例理解服务启动: {settings.app_name} v{settings.app_version}")

@app.on_event("shutdown")
async def shutdown_event():
    """服务关闭事件"""
    logger.info("病例理解服务关闭")

