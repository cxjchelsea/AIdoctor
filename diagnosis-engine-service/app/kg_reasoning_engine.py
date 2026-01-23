"""
知识图谱推理引擎模块别名
由于目录名包含连字符，创建此文件作为导入别名
使用importlib动态导入
"""
import importlib.util
from pathlib import Path

# kg-reasoning-engine目录路径
_kg_reasoning_engine_dir = Path(__file__).parent / "kg-reasoning-engine"

def _load_module(module_name, file_name):
    """动态加载模块"""
    file_path = _kg_reasoning_engine_dir / file_name
    spec = importlib.util.spec_from_file_location(module_name, file_path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

# 加载所有模块
_kg_client_module = _load_module("kg_client", "kg_client.py")
_path_retriever_module = _load_module("path_retriever", "path_retriever.py")
_path_scorer_module = _load_module("path_scorer", "path_scorer.py")
_path_injector_module = _load_module("path_injector", "path_injector.py")
_kg_reasoning_engine_module = _load_module("kg_reasoning_engine", "kg_reasoning_engine.py")
_prior_scorer_module = _load_module("prior_scorer", "prior_scorer.py")
_likelihood_scorer_module = _load_module("likelihood_scorer", "likelihood_scorer.py")
_posterior_scorer_module = _load_module("posterior_scorer", "posterior_scorer.py")

# 导出类
Neo4jClient = _kg_client_module.Neo4jClient
PathRetriever = _path_retriever_module.PathRetriever
PathScorer = _path_scorer_module.PathScorer
PathInjector = _path_injector_module.PathInjector
KGReasoningEngine = _kg_reasoning_engine_module.KGReasoningEngine
PriorScorer = _prior_scorer_module.PriorScorer
LikelihoodScorer = _likelihood_scorer_module.LikelihoodScorer
PosteriorScorer = _posterior_scorer_module.PosteriorScorer

__all__ = [
    'Neo4jClient',
    'PathRetriever',
    'PathScorer',
    'PathInjector',
    'KGReasoningEngine',
    'PriorScorer',
    'LikelihoodScorer',
    'PosteriorScorer'
]

