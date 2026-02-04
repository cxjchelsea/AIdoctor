"""
启动脚本（适用于PyCharm）
用于在PyCharm中直接运行
"""
import os
import sys
from pathlib import Path

# 获取当前脚本所在目录（health-state-assessment-service目录）
BASE_DIR = Path(__file__).resolve().parent

# 获取项目根目录（向上两级：health-state-assessment-service -> AIdoctor）
PROJECT_ROOT = BASE_DIR.parent

# 加载根目录的 .env 文件
try:
    from dotenv import load_dotenv
    env_path = PROJECT_ROOT / ".env"
    if env_path.exists():
        load_dotenv(env_path, override=True)
        print(f"已加载环境变量文件: {env_path}")
    else:
        print(f"警告: 未找到 .env 文件: {env_path}")
        # 尝试加载当前目录的 .env 文件作为备选
        local_env = BASE_DIR / ".env"
        if local_env.exists():
            load_dotenv(local_env, override=True)
            print(f"已加载本地环境变量文件: {local_env}")
except ImportError:
    print("警告: python-dotenv 未安装，无法加载 .env 文件")

# 将项目根目录添加到Python路径
sys.path.insert(0, str(BASE_DIR))

# 切换到服务目录
os.chdir(BASE_DIR)

import uvicorn

if __name__ == "__main__":
    # 从环境变量读取reload配置，默认关闭（性能优化）
    reload = os.getenv("RELOAD", "false").lower() == "true"
    
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8081,
        reload=reload,  # 默认关闭，需要时通过环境变量开启: set RELOAD=true
        log_level="info"
    )

