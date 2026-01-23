"""
风险评估服务主入口
"""
from fastapi import FastAPI
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers

app = FastAPI(
    title="风险评估服务",
    version="1.0.0",
    description="AI医生系统的风险评估服务（脑区F：风险与急症识别）"
)

# 注册路由
app.include_router(router, prefix="/api/v1")

# 设置异常处理
setup_exception_handlers(app)

@app.get("/health")
async def health():
    return {"status": "ok", "service": "risk-assessment-service"}

