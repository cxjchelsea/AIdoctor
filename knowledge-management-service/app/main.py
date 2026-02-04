"""
知识库管理服务主程序
"""
import time
import logging
import asyncio
from contextlib import asynccontextmanager
from pathlib import Path
from fastapi import FastAPI, Request
from fastapi.responses import Response
from prometheus_client import generate_latest, CONTENT_TYPE_LATEST
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers
from app.utils.logger import setup_logger
from app.utils.metrics import (
    http_requests_total,
    http_request_duration_seconds,
    http_request_errors_total,
    update_process_metrics
)

# 配置日志（支持文件输出）
logger = setup_logger("knowledge-management-service", log_file="logs/app.log")


async def update_metrics_task():
    """后台任务：定期更新系统资源指标"""
    while True:
        try:
            update_process_metrics()
            await asyncio.sleep(5)  # 每5秒更新一次
        except Exception as e:
            logger.error(f"更新系统资源指标失败: {str(e)}")
            await asyncio.sleep(5)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动时：启动后台任务
    task = asyncio.create_task(update_metrics_task())
    logger.info("系统资源监控任务已启动")
    logger.info("知识库管理服务启动")
    
    # 创建必要的目录
    data_dir = Path("data")
    data_dir.mkdir(exist_ok=True)
    (data_dir / "source").mkdir(exist_ok=True)
    (data_dir / "processed").mkdir(exist_ok=True)
    logger.info("数据目录初始化完成")
    
    yield
    
    # 关闭时：取消后台任务
    task.cancel()
    try:
        await task
    except asyncio.CancelledError:
        pass
    logger.info("系统资源监控任务已停止")
    logger.info("知识库管理服务关闭")


app = FastAPI(
    title="知识库管理服务",
    version="1.0.0",
    description="AI医生系统的知识库管理服务，负责知识图谱、表格、配置文件的导入、清洗、验证和管理",
    lifespan=lifespan
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
        
        # 记录指标
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
        "status": "ok",
        "service": "knowledge-management-service",
        "version": "1.0.0"
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

