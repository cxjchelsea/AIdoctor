"""
健康状态判定服务主入口（脑区0）
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers
from app.utils.logger import setup_logger

# 配置日志
logger = setup_logger()

app = FastAPI(
    title="健康状态判定服务",
    version="1.0.0",
    description="AI医生系统的健康状态判定服务（脑区0：健康状态判定）"
)

logger.info("健康状态判定服务启动中...")

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

