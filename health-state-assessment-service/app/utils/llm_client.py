"""
LLM客户端（从公共库导入）
为了向后兼容，保留此文件作为导入代理
"""
import sys
from pathlib import Path

# 尝试从公共库导入，如果失败则从本地路径导入
try:
    from aidoctor_llm import LangChainLLMClient, LLMConfig, LLMBackend
except ImportError:
    # 如果公共库未安装，从本地路径导入
    current_dir = Path(__file__).resolve().parent
    project_root = current_dir.parent.parent.parent  # 从 app/utils 到项目根目录
    common_dir = project_root / "common"
    
    # 将 common 目录添加到 sys.path
    if str(common_dir) not in sys.path:
        sys.path.insert(0, str(common_dir))
    
    # 从 aidoctor_llm 导入
    from aidoctor_llm import LangChainLLMClient, LLMConfig, LLMBackend

# 为了向后兼容，导出所有公共接口
__all__ = ["LangChainLLMClient", "LLMConfig", "LLMBackend"]

# 注意：此文件仅作为导入代理，实际的类定义在 common/aidoctor_llm 中

