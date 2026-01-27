# ICPC-2e-v7.0 中文翻译脚本使用说明

## 📋 概述

`translate_icpc_chinese.py` 脚本用于将ICPC-2e-v7.0英文数据翻译为中文，使用AI模型进行翻译。

## 🚀 快速开始

### 1. 配置AI翻译

确保 `ai_translation_config.json` 文件已正确配置：

```json
{
  "enabled": true,
  "api_url": "http://39.106.80.155:11434/api/generate",
  "api_type": "ollama",
  "api_key": null,
  "model": "qwen2.5:14b",
  "timeout": 30.0,
  "max_workers": 5
}
```

### 2. 运行翻译脚本

```bash
# 使用默认配置（翻译Title和ShortTitle）
python translate_icpc_chinese.py

# 指定输入输出文件
python translate_icpc_chinese.py --input "源数据/ICPC-2e-v7.0/ICPC-2e-v7.0-Title-csv.txt" --output "提取结果/icpc/icpc_chinese.csv"

# 只翻译Title，不翻译ShortTitle
python translate_icpc_chinese.py --no-shorttitle

# 只翻译ShortTitle，不翻译Title
python translate_icpc_chinese.py --no-title
```

## 📁 文件说明

### 输入文件

- **默认路径**: `docs/AI医生/3.知识内容提取/源数据/ICPC-2e-v7.0/ICPC-2e-v7.0-Title-csv.txt`
- **格式**: CSV格式，使用分号分隔，包含以下列：
  - `Code`: ICPC代码（如 "A01", "A02"）
  - `Title`: 完整标题（如 "Pain general/multiple sites"）
  - `ShortTitle`: 短标题（如 "Pain general/multiple sites"）

### 输出文件

- **默认路径**: `docs/AI医生/3.知识内容提取/提取结果/icpc/icpc_chinese_translation.csv`
- **格式**: CSV格式，包含以下列：
  - `Code`: ICPC代码
  - `Title`: 英文完整标题
  - `ShortTitle`: 英文短标题
  - `Title_中文`: 中文完整标题
  - `ShortTitle_中文`: 中文短标题

### 缓存文件

- **路径**: `docs/AI医生/3.知识内容提取/提取结果/icpc/translation_cache.json`
- **作用**: 保存已翻译的术语，避免重复调用API
- **格式**: JSON格式，键为 `ai_英文术语`，值为中文翻译

## ⚙️ 功能特性

### 1. 翻译缓存

- 已翻译的术语会保存到缓存文件中
- 下次运行时自动使用缓存，避免重复翻译
- 提高翻译速度，节省API调用

### 2. 并发翻译

- 支持多线程并发翻译
- 可通过 `max_workers` 配置并发数（建议3-10）
- 根据服务器性能调整，数值越大速度越快但占用资源更多

### 3. 智能去重

- 自动识别相同的英文术语
- 只翻译一次，应用到所有相同术语
- 减少API调用次数

### 4. 错误处理

- API调用失败时自动重试
- 翻译失败的术语留空，不影响其他翻译
- 详细的错误日志输出

## 📊 使用示例

### 示例1：完整翻译

```bash
python translate_icpc_chinese.py
```

输出：
```
✓ 已从配置文件加载AI翻译设置
  API地址: http://39.106.80.155:11434/api/generate
  API类型: ollama
  模型: qwen2.5:14b
  超时: 30.0秒
  并发数: 5

正在读取ICPC数据: ...
✓ 读取完成，共 726 条记录

开始翻译，共 726 个唯一术语...
  需要翻译: 726 条
  已缓存: 0 条
  进度: 10/726
  进度: 20/726
  ...

✓ 翻译完成
  Title翻译: 726/726 条
  ShortTitle翻译: 726/726 条

正在保存翻译缓存...
✓ 缓存已保存

正在保存结果到: ...
✓ 保存完成

================================================================================
翻译统计
================================================================================
总记录数: 726
Title已翻译: 726 (100.0%)
ShortTitle已翻译: 726 (100.0%)

示例数据（前5条）:
  -30: Medical examination/health evaluation-complete -> 医学检查/健康评估-完整
  -31: Medical examination/health evaluation-partial -> 医学检查/健康评估-部分
  A01: Pain general/multiple sites -> 全身/多处疼痛
  A02: Chills -> 寒战
  A03: Fever -> 发热
```

### 示例2：使用缓存继续翻译

如果之前已经翻译过部分数据，再次运行时会自动使用缓存：

```bash
python translate_icpc_chinese.py
```

输出：
```
✓ 已从配置文件加载AI翻译设置
...

开始翻译，共 726 个唯一术语...
  需要翻译: 0 条
  已缓存: 726 条

✓ 翻译完成
  Title翻译: 726/726 条
  ShortTitle翻译: 726/726 条
```

## 🔧 常见问题

### Q1: 如何修改AI翻译配置？

编辑 `ai_translation_config.json` 文件：

```json
{
  "enabled": true,
  "api_url": "http://your-server:11434/api/generate",
  "api_type": "ollama",
  "model": "your-model",
  "timeout": 30.0,
  "max_workers": 5
}
```

### Q2: 翻译速度慢怎么办？

1. **增加并发数**：修改 `max_workers`（建议3-10）
2. **使用缓存**：已翻译的术语会使用缓存，速度很快
3. **检查网络**：确保API服务器响应速度正常

### Q3: 翻译质量不理想怎么办？

1. **调整提示词**：修改 `_build_translation_prompt` 方法中的提示词
2. **更换模型**：使用更专业的医学翻译模型
3. **人工审核**：翻译完成后进行人工审核和修正

### Q4: 如何只翻译部分数据？

可以修改脚本，添加过滤条件，或者先提取需要翻译的数据到新文件。

### Q5: 翻译缓存文件损坏怎么办？

删除 `translation_cache.json` 文件，重新运行脚本即可。

## 📝 注意事项

1. **首次使用**：建议先用少量数据测试，确认翻译质量和API连接正常
2. **翻译缓存**：已翻译的内容会保存，避免重复调用API
3. **超时设置**：根据服务器响应速度调整 `timeout`
4. **API限制**：注意API调用频率限制，避免过载
5. **医学术语**：AI翻译可能不完全准确，建议进行人工审核

## 🎯 下一步

翻译完成后，可以：

1. **审核翻译质量**：检查翻译结果，修正不准确的翻译
2. **合并到主数据**：将中文翻译合并到ICPC主数据集中
3. **构建映射表**：创建ICPC代码到中文名称的映射表
4. **集成到系统**：将翻译结果集成到AI医生系统中

## 📚 相关文档

- [通用数据集中文翻译方案.md](../提取方案/通用数据集中文翻译方案.md)
- [README_AI翻译配置.md](./README_AI翻译配置.md)

