"""
治疗推理服务主入口
"""
from fastapi import FastAPI
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers

app = FastAPI(
    title="治疗推理服务",
    version="1.0.0",
    description="AI医生系统的治疗推理服务（脑区E：治疗/处置建议引擎）"
)

# 注册路由
app.include_router(router, prefix="/api/v1")

# 设置异常处理
setup_exception_handlers(app)

@app.get("/health")
async def health():
    return {"status": "ok", "service": "treatment-engine-service"}

