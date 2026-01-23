"""
诊断引擎服务主程序
"""
from fastapi import FastAPI
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers

app = FastAPI(
    title="诊断引擎服务",
    version="1.0.0",
    description="智能诊断系统的诊断引擎服务"
)

# 注册路由
app.include_router(router, prefix="/api/v1")

# 设置异常处理
setup_exception_handlers(app)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "diagnosis-engine-service"}
