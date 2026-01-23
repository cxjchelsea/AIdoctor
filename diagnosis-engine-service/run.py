"""
诊断引擎服务启动脚本
用于在PyCharm中直接运行
"""
import os
import sys
from pathlib import Path

# 获取当前脚本所在目录（diagnosis-engine-service目录）
BASE_DIR = Path(__file__).resolve().parent

# 将项目根目录添加到Python路径
sys.path.insert(0, str(BASE_DIR))

# 切换到项目根目录
os.chdir(BASE_DIR)

import uvicorn

if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8086,
        reload=True,  # 开发模式，代码修改后自动重启
        log_level="info"
    )

