"""
健康状态判定服务主入口（脑区0）
"""
import time
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from prometheus_client import generate_latest, CONTENT_TYPE_LATEST
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers
from app.utils.logger import setup_logger
from app.utils.metrics import (
    http_requests_total,
    http_request_duration_seconds,
    http_request_errors_total
)

# 配置日志（支持文件输出）
logger = setup_logger(log_file="logs/app.log")

app = FastAPI(
    title="健康状态判定服务",
    version="1.0.0",
    description="AI医生系统的健康状态判定服务（脑区0：健康状态判定）"
)

logger.info("健康状态判定服务启动中...")

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

# 配置CORS（跨域资源共享）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://127.0.0.1:3000"],  # 前端地址
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有HTTP方法
    allow_headers=["*"],  # 允许所有请求头
)

# 注册路由
app.include_router(router, prefix="/api/v1")

# 设置异常处理
setup_exception_handlers(app)

logger.info("健康状态判定服务启动完成")

@app.get("/health")
async def health():
    """健康检查接口"""
    return {"status": "ok", "service": "health-state-assessment-service"}


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

