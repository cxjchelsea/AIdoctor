"""
LangChain LLM客户端封装
支持多种LLM后端：OpenAI、ChatGLM、Ollama、自定义HTTP API
"""

import os
import logging
import asyncio
from typing import Optional, Dict, Any

try:
    # 使用新的导入方式（LangChain 0.2.0+）
    try:
        from langchain_core.language_models.llms import BaseLLM
        from langchain_openai import ChatOpenAI
        from langchain_community.llms import Ollama
    except ImportError:
        # 回退到旧版本导入方式
        from langchain.llms.base import BaseLLM
        from langchain.chat_models import ChatOpenAI
        from langchain.llms import Ollama
    LANGCHAIN_AVAILABLE = True
except ImportError:
    LANGCHAIN_AVAILABLE = False
    logging.warning("LangChain not installed, LLM features will be limited")

import httpx
from .config import LLMConfig, LLMBackend
from .exceptions import LLMException, LLMTimeoutException, LLMAPIException, LLMConfigException

logger = logging.getLogger(__name__)


class LangChainLLMClient:
    """LangChain LLM客户端封装"""
    
    def __init__(self, config: Optional[LLMConfig] = None):
        """
        初始化LLM客户端
        
        Args:
            config: LLM配置，如果为None则从环境变量读取
        """
        if not LANGCHAIN_AVAILABLE:
            raise ImportError(
                "LangChain is not installed. Please install it with: "
                "pip install langchain openai"
            )
        
        self.config = config or self._load_config_from_env()
        self.llm = self._create_llm()
        
        # 记录详细的配置信息
        config_info = f"LLM客户端初始化完成: backend={self.config.backend}"
        if self.config.backend == LLMBackend.OLLAMA:
            config_info += f", ollama_model={self.config.ollama_model}, ollama_base_url={self.config.ollama_base_url}"
        elif self.config.backend == LLMBackend.OPENAI:
            config_info += f", model={self.config.model}, base_url={self.config.openai_base_url or 'default'}"
        elif self.config.backend == LLMBackend.CHATGLM:
            config_info += f", model={self.config.model}, chatglm_url={self.config.chatglm_api_url}"
        elif self.config.backend == LLMBackend.CUSTOM:
            config_info += f", model={self.config.model}, custom_url={self.config.custom_api_url}"
        logger.info(config_info)
    
    def _load_config_from_env(self) -> LLMConfig:
        """从环境变量加载配置"""
        backend_str = os.getenv("LLM_BACKEND", "openai").lower()
        backend = LLMBackend(backend_str) if backend_str in [e.value for e in LLMBackend] else LLMBackend.OPENAI
        
        return LLMConfig(
            backend=backend,
            model=os.getenv("OPENAI_MODEL", os.getenv("LLM_MODEL", "gpt-4")),
            temperature=float(os.getenv("OPENAI_TEMPERATURE", os.getenv("LLM_TEMPERATURE", "0.3"))),
            max_tokens=int(os.getenv("OPENAI_MAX_TOKENS", os.getenv("LLM_MAX_TOKENS", "2000"))),
            timeout=int(os.getenv("LLM_TIMEOUT", "30")),
            max_retries=int(os.getenv("LLM_MAX_RETRIES", "3")),
            retry_delay=float(os.getenv("LLM_RETRY_DELAY", "1.0")),
            openai_api_key=os.getenv("OPENAI_API_KEY"),
            openai_base_url=os.getenv("OPENAI_BASE_URL"),
            chatglm_api_url=os.getenv("CHATGLM_API_URL"),
            chatglm_api_key=os.getenv("CHATGLM_API_KEY"),
            ollama_base_url=os.getenv("OLLAMA_BASE_URL", "http://localhost:11434"),
            ollama_model=os.getenv("OLLAMA_MODEL", "llama2"),
            custom_api_url=os.getenv("CUSTOM_LLM_API_URL"),
            custom_api_key=os.getenv("CUSTOM_LLM_API_KEY"),
            custom_headers={}
        )
    
    def _create_llm(self) -> Optional[BaseLLM]:
        """创建LLM实例"""
        if self.config.backend == LLMBackend.OPENAI:
            return self._create_openai_llm()
        elif self.config.backend == LLMBackend.OLLAMA:
            return self._create_ollama_llm()
        elif self.config.backend == LLMBackend.CHATGLM:
            # ChatGLM通过自定义HTTP调用实现
            return None
        elif self.config.backend == LLMBackend.CUSTOM:
            # 自定义API通过HTTP调用实现
            return None
        else:
            raise LLMConfigException(f"Unsupported LLM backend: {self.config.backend}")
    
    def _create_openai_llm(self) -> ChatOpenAI:
        """创建OpenAI LLM"""
        if not self.config.openai_api_key:
            raise LLMConfigException("OPENAI_API_KEY is required for OpenAI backend")
        
        # 兼容新旧版本的参数名称
        kwargs = {
            "temperature": self.config.temperature,
            "max_tokens": self.config.max_tokens,
            "timeout": self.config.timeout,
            "max_retries": self.config.max_retries,
        }
        
        # 新版本使用 model, api_key, base_url
        # 旧版本使用 model_name, openai_api_key, openai_api_base
        try:
            # 尝试新版本参数
            kwargs.update({
                "model": self.config.model,
                "api_key": self.config.openai_api_key,
            })
            if self.config.openai_base_url:
                kwargs["base_url"] = self.config.openai_base_url
            return ChatOpenAI(**kwargs)
        except TypeError:
            # 回退到旧版本参数
            kwargs.update({
                "model_name": self.config.model,
                "openai_api_key": self.config.openai_api_key,
            })
            if self.config.openai_base_url:
                kwargs["openai_api_base"] = self.config.openai_base_url
            return ChatOpenAI(**kwargs)
    
    def _create_ollama_llm(self) -> Ollama:
        """创建Ollama LLM"""
        logger.info(
            f"创建Ollama LLM: model={self.config.ollama_model}, "
            f"base_url={self.config.ollama_base_url}, "
            f"temperature={self.config.temperature}, "
            f"max_tokens={self.config.max_tokens}"
        )
        return Ollama(
            model=self.config.ollama_model,
            base_url=self.config.ollama_base_url,
            temperature=self.config.temperature,
            num_predict=self.config.max_tokens,
        )
    
    async def generate(
        self,
        prompt: str,
        **kwargs
    ) -> str:
        """
        异步生成文本
        
        Args:
            prompt: 提示词
            **kwargs: 其他参数
            
        Returns:
            生成的文本
        """
        # 如果使用ChatGLM或自定义API，使用HTTP调用
        if self.config.backend == LLMBackend.CHATGLM:
            return await self._call_chatglm_api(prompt, **kwargs)
        elif self.config.backend == LLMBackend.CUSTOM:
            return await self._call_custom_api(prompt, **kwargs)
        
        # 使用LangChain的LLM
        try:
            # 记录调用信息
            if self.config.backend == LLMBackend.OLLAMA:
                logger.info(
                    f"调用Ollama LLM: model={self.config.ollama_model}, "
                    f"base_url={self.config.ollama_base_url}, "
                    f"prompt_length={len(prompt)}"
                )
            elif self.config.backend == LLMBackend.OPENAI:
                logger.info(
                    f"调用OpenAI LLM: model={self.config.model}, "
                    f"base_url={self.config.openai_base_url}, "
                    f"prompt_length={len(prompt)}"
                )
            
            # 异步调用LLM
            if hasattr(self.llm, 'apredict'):
                result = await self.llm.apredict(prompt, **kwargs)
            elif hasattr(self.llm, 'agenerate'):
                result = await self.llm.agenerate([prompt], **kwargs)
                result = result.generations[0][0].text
            else:
                # 同步调用（在线程池中执行）
                loop = asyncio.get_event_loop()
                result = await loop.run_in_executor(
                    None,
                    lambda: self.llm.predict(prompt, **kwargs)
                )
            
            return result
        except asyncio.TimeoutError:
            raise LLMTimeoutException(f"LLM调用超时: timeout={self.config.timeout}s")
        except Exception as e:
            # 记录详细的错误信息
            error_details = f"LLM调用失败: backend={self.config.backend}"
            if self.config.backend == LLMBackend.OLLAMA:
                error_details += f", model={self.config.ollama_model}, base_url={self.config.ollama_base_url}"
            elif self.config.backend == LLMBackend.OPENAI:
                error_details += f", model={self.config.model}, base_url={self.config.openai_base_url}"
            error_details += f", error={str(e)}"
            logger.error(error_details, exc_info=True)
            raise LLMAPIException(f"LLM调用失败: {str(e)}")
    
    async def _call_chatglm_api(self, prompt: str, **kwargs) -> str:
        """调用ChatGLM API"""
        if not self.config.chatglm_api_url:
            raise LLMConfigException("CHATGLM_API_URL is required for ChatGLM backend")
        
        url = f"{self.config.chatglm_api_url}/v1/chat/completions"
        headers = {
            "Content-Type": "application/json",
        }
        if self.config.chatglm_api_key:
            headers["Authorization"] = f"Bearer {self.config.chatglm_api_key}"
        
        data = {
            "model": self.config.model,
            "messages": [
                {"role": "user", "content": prompt}
            ],
            "temperature": self.config.temperature,
            "max_tokens": self.config.max_tokens,
        }
        
        async with httpx.AsyncClient(timeout=self.config.timeout) as client:
            for attempt in range(self.config.max_retries):
                try:
                    response = await client.post(url, json=data, headers=headers)
                    response.raise_for_status()
                    result = response.json()
                    return result["choices"][0]["message"]["content"]
                except httpx.TimeoutException:
                    if attempt < self.config.max_retries - 1:
                        await asyncio.sleep(self.config.retry_delay * (attempt + 1))
                        continue
                    raise LLMTimeoutException(f"ChatGLM API调用超时: timeout={self.config.timeout}s")
                except Exception as e:
                    if attempt < self.config.max_retries - 1:
                        await asyncio.sleep(self.config.retry_delay * (attempt + 1))
                        continue
                    raise LLMAPIException(f"ChatGLM API调用失败: {str(e)}")
    
    async def _call_custom_api(self, prompt: str, **kwargs) -> str:
        """调用自定义HTTP API"""
        if not self.config.custom_api_url:
            raise LLMConfigException("CUSTOM_LLM_API_URL is required for custom backend")
        
        url = self.config.custom_api_url
        headers = {
            "Content-Type": "application/json",
            **self.config.custom_headers
        }
        if self.config.custom_api_key:
            headers["Authorization"] = f"Bearer {self.config.custom_api_key}"
        
        data = {
            "prompt": prompt,
            "temperature": self.config.temperature,
            "max_tokens": self.config.max_tokens,
            **kwargs
        }
        
        async with httpx.AsyncClient(timeout=self.config.timeout) as client:
            for attempt in range(self.config.max_retries):
                try:
                    response = await client.post(url, json=data, headers=headers)
                    response.raise_for_status()
                    result = response.json()
                    # 支持不同的响应格式
                    if "response" in result:
                        return result["response"]
                    elif "text" in result:
                        return result["text"]
                    elif "content" in result:
                        return result["content"]
                    else:
                        return str(result)
                except httpx.TimeoutException:
                    if attempt < self.config.max_retries - 1:
                        await asyncio.sleep(self.config.retry_delay * (attempt + 1))
                        continue
                    raise LLMTimeoutException(f"自定义API调用超时: timeout={self.config.timeout}s")
                except Exception as e:
                    if attempt < self.config.max_retries - 1:
                        await asyncio.sleep(self.config.retry_delay * (attempt + 1))
                        continue
                    raise LLMAPIException(f"自定义API调用失败: {str(e)}")
    
    def generate_sync(self, prompt: str, **kwargs) -> str:
        """
        同步生成文本（用于非异步环境）
        
        Args:
            prompt: 提示词
            **kwargs: 其他参数
            
        Returns:
            生成的文本
        """
        if self.config.backend == LLMBackend.CHATGLM:
            return asyncio.run(self._call_chatglm_api(prompt, **kwargs))
        elif self.config.backend == LLMBackend.CUSTOM:
            return asyncio.run(self._call_custom_api(prompt, **kwargs))
        
        # 使用LangChain的同步调用
        try:
            if hasattr(self.llm, 'predict'):
                return self.llm.predict(prompt, **kwargs)
            elif hasattr(self.llm, 'generate'):
                result = self.llm.generate([prompt], **kwargs)
                return result.generations[0][0].text
            else:
                raise LLMConfigException("LLM does not support sync generation")
        except Exception as e:
            logger.error(f"LLM同步调用失败: {str(e)}", exc_info=True)
            raise LLMAPIException(f"LLM同步调用失败: {str(e)}")

