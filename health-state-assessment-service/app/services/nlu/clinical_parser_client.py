"""
临床解析服务客户端：调用clinical-parsing-service进行医学概念识别和归一化
"""
import httpx
from typing import Dict, Any, List, Optional
from app.config.settings import settings
from app.utils.logger import logger


class ClinicalParserClient:
    """临床解析服务客户端"""
    
    def __init__(self, base_url: Optional[str] = None, timeout: Optional[int] = None):
        """
        初始化临床解析服务客户端
        
        Args:
            base_url: 临床解析服务基础URL，如果为None则从配置读取
            timeout: 超时时间（秒），如果为None则从配置读取
        """
        self.nlu_config = settings.nlu
        self.base_url = base_url or self.nlu_config.clinical_parser_url
        self.timeout = timeout or self.nlu_config.clinical_parser_timeout
        self.use_clinical_parser = self.nlu_config.use_clinical_parser
        
        # 确保URL以/结尾
        if self.base_url and not self.base_url.endswith('/'):
            self.base_url = self.base_url.rstrip('/')
        
        logger.info(f"临床解析服务客户端初始化: base_url={self.base_url}, timeout={self.timeout}")
    
    async def parse(
        self,
        text: str,
        user_id: str = "default_user",
        session_id: str = "default_session",
        cdp_id: Optional[str] = None
    ) -> Optional[Dict[str, Any]]:
        """
        调用临床解析服务进行医学概念识别和归一化
        
        Args:
            text: 输入文本
            user_id: 用户ID
            session_id: 会话ID
            cdp_id: CDP ID（可选）
            
        Returns:
            解析结果字典，包含concepts和structuredData，如果失败返回None
        """
        if not self.use_clinical_parser:
            logger.debug("临床解析服务已禁用")
            return None
        
        if not text or not text.strip():
            logger.warning("输入文本为空，跳过临床解析服务调用")
            return None
        
        try:
            # 构建请求URL
            url = f"{self.base_url}/api/v1/parsing/parse"
            
            # 构建请求体
            request_body = {
                "userId": user_id,
                "sessionId": session_id,
                "text": text
            }
            if cdp_id:
                request_body["cdpId"] = cdp_id
            
            # 发送HTTP请求
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                logger.debug(f"调用临床解析服务: url={url}, text_length={len(text)}")
                response = await client.post(url, json=request_body)
                response.raise_for_status()
                
                # 解析响应
                result = response.json()
                
                # 检查响应格式
                if result.get("code") == 200 and "data" in result:
                    logger.info(f"临床解析服务调用成功: 识别到 {len(result['data'].get('concepts', []))} 个概念")
                    return result["data"]
                else:
                    logger.warning(f"临床解析服务返回异常: {result}")
                    return None
                    
        except httpx.TimeoutException:
            logger.warning(f"临床解析服务调用超时: timeout={self.timeout}s")
            return None
        except httpx.HTTPStatusError as e:
            logger.error(f"临床解析服务HTTP错误: status={e.response.status_code}, response={e.response.text}")
            return None
        except Exception as e:
            logger.error(f"临床解析服务调用失败: {e}", exc_info=True)
            return None
    
    async def normalize_concepts(
        self,
        text: str,
        user_id: str = "default_user",
        session_id: str = "default_session"
    ) -> List[Dict[str, Any]]:
        """
        归一化医学概念
        
        Args:
            text: 输入文本
            user_id: 用户ID
            session_id: 会话ID
            
        Returns:
            归一化后的概念列表
        """
        result = await self.parse(text, user_id, session_id)
        if result:
            return result.get("concepts", [])
        return []
    
    async def get_structured_data(
        self,
        text: str,
        user_id: str = "default_user",
        session_id: str = "default_session"
    ) -> Optional[Dict[str, Any]]:
        """
        获取结构化数据
        
        Args:
            text: 输入文本
            user_id: 用户ID
            session_id: 会话ID
            
        Returns:
            结构化数据字典，如果失败返回None
        """
        result = await self.parse(text, user_id, session_id)
        if result:
            structured_data = result.get("structuredData", {})
            return structured_data
        return None

