"""
对话服务主程序
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers

app = FastAPI(
    title="对话服务",
    version="1.0.0",
    description="智能诊断系统的对话服务 - 主动对话管理、智能追问生成、自然语言理解"
)

# CORS配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(router, prefix="/api/v1/dialog")

# 设置异常处理
setup_exception_handlers(app)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "dialog-service"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8088)

