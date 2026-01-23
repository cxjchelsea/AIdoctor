"""
解释生成服务主入口（脑区G）
"""
from fastapi import FastAPI
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers

app = FastAPI(
    title="解释生成服务",
    version="1.0.0",
    description="AI医生系统的解释生成服务（脑区G：可解释性与证据链）"
)

# 注册路由
app.include_router(router, prefix="/api/v1")

# 设置异常处理
setup_exception_handlers(app)

@app.get("/health")
async def health():
    return {"status": "ok"}

