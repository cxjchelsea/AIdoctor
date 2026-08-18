"""Python Runtime HTTP 薄传输适配器。

分类：NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF

本包只负责 HTTP 解析、身份校验与响应归一化；不拥有编排、
Model Runtime、State Committer 或临床工作流。
导入本包不得启动网络服务。
"""

from .app import app, create_app

__all__ = ["app", "create_app"]
