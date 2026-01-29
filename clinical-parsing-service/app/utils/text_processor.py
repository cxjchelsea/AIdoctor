"""
文本处理工具
"""
import re
import logging
from typing import List, Tuple, Optional

logger = logging.getLogger(__name__)


class TextProcessor:
    """文本处理器"""
    
    @staticmethod
    def clean_text(text: str) -> str:
        """
        文本清洗
        去除标点、特殊字符，统一编码
        """
        if not text:
            return ""
        
        # 去除多余空白字符
        text = re.sub(r'\s+', ' ', text)
        
        # 去除首尾空白
        text = text.strip()
        
        return text
    
    @staticmethod
    def extract_time_expressions(text: str) -> List[Tuple[str, str, Optional[int]]]:
        """
        提取时间表达式
        返回: [(时间表达式, 标准化时间, 天数), ...]
        天数：如果是具体数字，返回数字；如果是相对时间，返回None
        """
        results = []
        
        # 中文数字到阿拉伯数字的映射
        chinese_num_map = {
            '一': 1, '二': 2, '三': 3, '四': 4, '五': 5,
            '六': 6, '七': 7, '八': 8, '九': 9, '十': 10,
            '十一': 11, '十二': 12, '十三': 13, '十四': 14, '十五': 15,
            '十六': 16, '十七': 17, '十八': 18, '十九': 19, '二十': 20,
            '两': 2, '俩': 2
        }
        
        def convert_to_number(num_str: str) -> Optional[int]:
            """将中文数字或阿拉伯数字字符串转换为整数"""
            # 先尝试直接转换为阿拉伯数字
            try:
                return int(num_str)
            except ValueError:
                pass
            
            # 尝试中文数字转换
            if num_str in chinese_num_map:
                return chinese_num_map[num_str]
            
            # 处理"十X"格式（如"十三"）
            if num_str.startswith('十') and len(num_str) == 2:
                second_char = num_str[1]
                if second_char in chinese_num_map:
                    return 10 + chinese_num_map[second_char]
            
            # 处理"X十"格式（如"三十"）
            if num_str.endswith('十') and len(num_str) == 2:
                first_char = num_str[0]
                if first_char in chinese_num_map:
                    return chinese_num_map[first_char] * 10
            
            return None
        
        # 提取具体天数（支持多种格式，包括中文数字）
        # 已经X天了、X天了、持续X天了、X天来、X天左右等
        duration_patterns = [
            (r'已经([一二三四五六七八九十两俩\d]+)天[了来]', 1),  # 已经三天了、已经三天来
            (r'([一二三四五六七八九十两俩\d]+)天[了来]', 1),  # 三天了、三天来
            (r'持续([一二三四五六七八九十两俩\d]+)天[了来]', 1),  # 持续三天了
            (r'([一二三四五六七八九十两俩\d]+)天左右', 1),  # 三天左右
            (r'([一二三四五六七八九十两俩\d]+)天前', 1),  # 三天前
            (r'最近([一二三四五六七八九十两俩\d]+)天', 1),  # 最近三天
            (r'([一二三四五六七八九十两俩\d]+)周[了来]', 2),  # 三周了（转换为天数）
            (r'([一二三四五六七八九十两俩\d]+)个月[了来]', 3),  # 三个月了（转换为天数，按30天/月）
            (r'([一二三四五六七八九十两俩\d]+)年[了来]', 4),  # 三年了（转换为天数，按365天/年）
        ]
        
        for pattern, pattern_type in duration_patterns:
            matches = re.finditer(pattern, text)
            for match in matches:
                days_str = match.group(1)
                days = convert_to_number(days_str)
                if days is not None:
                    # 根据类型转换
                    if pattern_type == 2:  # 周
                        days = days * 7
                    elif pattern_type == 3:  # 月
                        days = days * 30
                    elif pattern_type == 4:  # 年
                        days = days * 365
                    results.append((match.group(0), 'duration', days))
        
        # 提取相对时间（如果没有找到具体天数）
        if not results:
            relative_patterns = [
                (r'最近', 'recent'),
                (r'刚才', 'just_now'),
                (r'今天', 'today'),
                (r'昨天', 'yesterday'),
                (r'前天', 'day_before_yesterday'),
            ]
            
            for pattern, normalized in relative_patterns:
                if re.search(pattern, text):
                    results.append((pattern, normalized, None))
        
        return results
    
    @staticmethod
    def extract_severity_expressions(text: str) -> List[Tuple[str, str]]:
        """
        提取严重度表达式
        返回: [(严重度表达式, 标准化严重度), ...]
        """
        severity_mapping = {
            '轻微': 'mild',
            '轻度': 'mild',
            '有点': 'mild',
            '稍微': 'mild',
            '中等': 'moderate',
            '中度': 'moderate',
            '比较': 'moderate',
            '严重': 'severe',
            '重度': 'severe',
            '很严重': 'severe',
            '非常严重': 'severe',
        }
        
        results = []
        for expr, normalized in severity_mapping.items():
            if expr in text:
                results.append((expr, normalized))
        
        return results
    
    @staticmethod
    def extract_trigger_expressions(text: str) -> List[str]:
        """
        提取诱因表达式
        返回: [诱因, ...]
        """
        trigger_keywords = [
            '活动后', '运动后', '劳累后', '情绪激动后',
            '进食后', '饭后', '空腹时',
            '夜间', '晚上', '白天',
            '受凉后', '感冒后',
        ]
        
        triggers = []
        for keyword in trigger_keywords:
            if keyword in text:
                triggers.append(keyword)
        
        return triggers
    
    @staticmethod
    def extract_location_expressions(text: str) -> List[str]:
        """
        提取部位表达式
        返回: [部位, ...]，优先匹配更具体的部位（更长的关键词）
        """
        # 常见身体部位关键词
        location_keywords = [
            # 头部
            '头部', '头', '额头', '太阳穴', '后脑勺',
            # 面部
            '面部', '脸', '脸颊', '下巴', '鼻子', '眼睛', '耳朵', '嘴巴',
            # 颈部
            '颈部', '脖子', '喉咙', '咽喉',
            # 胸部
            '胸部', '胸', '心口', '胸口', '乳房', '乳头',
            # 腹部（具体部位在前，通用在后）
            '左上腹', '右上腹', '左下腹', '右下腹', '上腹部', '下腹部', '中腹部',
            '胆囊区', '肝区', '胃部', '脐周', '脐部',
            '腹部', '肚子', '胃',
            # 背部
            '背部', '背', '腰部', '腰', '肩部', '肩', '脊柱',
            # 四肢
            '手臂', '胳膊', '上臂', '前臂', '手腕', '手', '手指',
            '腿部', '腿', '大腿', '小腿', '膝盖', '脚踝', '脚', '脚趾',
            # 其他
            '全身', '局部', '一侧', '两侧', '双侧',
        ]
        
        # 按长度从长到短排序，优先匹配更具体的部位
        location_keywords_sorted = sorted(location_keywords, key=len, reverse=True)
        
        locations = []
        matched_ranges = []  # 记录已匹配的范围 [(start, end), ...]
        
        # 按顺序匹配，避免重叠
        for keyword in location_keywords_sorted:
            if keyword in text:
                # 找到所有匹配位置
                start = 0
                while True:
                    pos = text.find(keyword, start)
                    if pos == -1:
                        break
                    
                    end = pos + len(keyword)
                    # 检查这个范围是否与已匹配的范围重叠
                    overlap = False
                    for matched_start, matched_end in matched_ranges:
                        if not (end <= matched_start or pos >= matched_end):
                            overlap = True
                            break
                    
                    if not overlap:
                        # 没有重叠，添加匹配
                        locations.append(keyword)
                        matched_ranges.append((pos, end))
                        break
                    
                    start = pos + 1
        
        return locations
    
    @staticmethod
    def split_sentences(text: str) -> List[str]:
        """
        分句
        使用标点符号分割句子
        """
        # 中文句号、问号、感叹号
        sentences = re.split(r'[。！？；\n]', text)
        # 过滤空句子
        sentences = [s.strip() for s in sentences if s.strip()]
        return sentences
    
    @staticmethod
    def normalize_text(text: str) -> str:
        """
        文本标准化
        统一编码、大小写等
        """
        # 统一全角半角
        text = text.replace('，', ',').replace('。', '.').replace('！', '!')
        text = text.replace('？', '?').replace('：', ':').replace('；', ';')
        
        return text

