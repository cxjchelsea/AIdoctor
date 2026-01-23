"""
检查建议服务主入口
"""
from fastapi import FastAPI
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers

app = FastAPI(
    title="检查建议服务",
    version="1.0.0",
    description="AI医生系统的检查建议服务（脑区D：检查/检验建议与价值评估）"
)

# 注册路由
app.include_router(router, prefix="/api/v1")

# 设置异常处理
setup_exception_handlers(app)

@app.get("/health")
async def health():
    return {"status": "ok", "service": "workup-planner-service"}

