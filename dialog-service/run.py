"""
对话服务启动脚本
用于在PyCharm中直接运行
"""
import os
import sys
from pathlib import Path

# 获取当前脚本所在目录（dialog-service目录）
BASE_DIR = Path(__file__).resolve().parent

# 将项目根目录添加到Python路径
sys.path.insert(0, str(BASE_DIR))

# 切换到项目根目录
os.chdir(BASE_DIR)

import uvicorn

if __name__ == "__main__":
    # 从环境变量读取reload配置，默认关闭（性能优化）
    reload = os.getenv("RELOAD", "false").lower() == "true"
    
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8088,
        reload=reload,  # 默认关闭，需要时通过环境变量开启: set RELOAD=true
        log_level="info"
    )

