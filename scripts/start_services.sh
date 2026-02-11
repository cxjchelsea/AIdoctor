#!/bin/bash
# AIdoctor 项目一键启动脚本 - Linux/Mac版本
# 这是一个快捷方式，实际功能由 start_services.py 提供

echo "========================================"
echo "AIdoctor 项目一键启动脚本"
echo "========================================"
echo ""

# 切换到项目根目录（脚本在scripts文件夹中）
cd "$(dirname "$0")/.."

# 检查Python是否安装
if ! command -v python3 &> /dev/null; then
    echo "[错误] 未找到Python3，请先安装Python3"
    exit 1
fi

# 运行Python启动脚本
python3 scripts/start_services.py

