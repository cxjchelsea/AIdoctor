"""
本地开发运行脚本
"""
import os
import uvicorn

if __name__ == "__main__":
    # 从环境变量读取reload配置，默认关闭（性能优化）
    reload = os.getenv("RELOAD", "false").lower() == "true"
    
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8092,
        reload=reload  # 默认关闭，需要时通过环境变量开启: set RELOAD=true
    )

