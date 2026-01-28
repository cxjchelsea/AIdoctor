"""
病例理解服务主入口（脑区A）
"""
import time
import logging
from fastapi import FastAPI, Request
from fastapi.responses import Response
from prometheus_client import generate_latest, CONTENT_TYPE_LATEST
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers
from app.config.settings import settings
from app.utils.metrics import (
    http_requests_total,
    http_request_duration_seconds,
    http_request_errors_total
)

# 配置日志（支持文件输出）
import sys
from pathlib import Path
from logging.handlers import RotatingFileHandler

# 创建日志目录
log_dir = Path("logs")
log_dir.mkdir(exist_ok=True)

# 配置根日志
logging.basicConfig(
    level=getattr(logging, settings.log_level),
    format='[%(asctime)s] [%(levelname)s] [%(name)s] [%(filename)s:%(lineno)d] - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
    handlers=[
        logging.StreamHandler(sys.stdout),  # 控制台输出
        RotatingFileHandler(
            "logs/app.log",
            maxBytes=10 * 1024 * 1024,  # 10MB
            backupCount=5,
            encoding='utf-8'
        )  # 文件输出
    ]
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="病例理解服务",
    version=settings.app_version,
    description="AI医生系统的病例理解服务（脑区A：病例理解与结构化）"
)

# 性能监控中间件（非阻塞）
@app.middleware("http")
async def metrics_middleware(request: Request, call_next):
    """性能监控中间件，记录请求指标"""
    start_time = time.time()
    method = request.method
    path = request.url.path
    
    # 简化路径，避免高基数
    simplified_path = path
    if '/api/v1/' in path:
        parts = path.split('/')
        if len(parts) >= 4:
            simplified_path = '/'.join(parts[:4])
    
    try:
        response = await call_next(request)
        status = response.status_code
        process_time = time.time() - start_time
        
        # 记录指标（内存操作，几乎无性能影响）
        http_requests_total.labels(
            method=method,
            path=simplified_path,
            status=str(status)
        ).inc()
        
        http_request_duration_seconds.labels(
            method=method,
            path=simplified_path
        ).observe(process_time)
        
        return response
    except Exception as e:
        process_time = time.time() - start_time
        error_type = type(e).__name__
        
        http_request_errors_total.labels(
            method=method,
            path=simplified_path,
            error_type=error_type
        ).inc()
        
        http_request_duration_seconds.labels(
            method=method,
            path=simplified_path
        ).observe(process_time)
        
        raise

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


@app.get("/metrics")
async def metrics():
    """
    Prometheus指标端点
    Prometheus会定期拉取此端点的指标数据
    """
    return Response(
        generate_latest(),
        media_type=CONTENT_TYPE_LATEST
    )

@app.on_event("startup")
async def startup_event():
    """服务启动事件"""
    logger.info(f"病例理解服务启动: {settings.app_name} v{settings.app_version}")

@app.on_event("shutdown")
async def shutdown_event():
    """服务关闭事件"""
    logger.info("病例理解服务关闭")

