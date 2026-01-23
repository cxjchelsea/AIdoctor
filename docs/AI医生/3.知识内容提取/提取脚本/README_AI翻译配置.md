# AI翻译配置说明

## 📋 概述

脚本支持通过配置文件来设置AI翻译功能，无需每次在命令行输入参数。

## 📁 配置文件位置

配置文件位于：`提取脚本/ai_translation_config.json`

## ⚙️ 配置方法

### 1. 编辑配置文件

打开 `ai_translation_config.json` 文件，修改以下配置：

```json
{
  "enabled": true,                    // 设置为 true 启用AI翻译
  "api_url": "http://localhost:11434/api/generate",  // Ollama API地址
  "api_type": "ollama",               // API类型：ollama
  "api_key": null,                    // API密钥（Ollama通常不需要）
  "model": "deepseek-r1",            // 模型名称
  "timeout": 30.0                     // 超时时间（秒）
}
```

### 2. 配置说明

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `enabled` | 是否启用AI翻译 | `true` 或 `false` |
| `api_url` | Ollama API地址 | `http://localhost:11434/api/generate` |
| `api_type` | API类型 | `ollama`（固定值） |
| `api_key` | API密钥 | Ollama通常不需要，设为 `null` |
| `model` | 模型名称 | `deepseek-r1` |
| `timeout` | 超时时间（秒） | `30.0` |

### 3. 服务器地址配置

如果Ollama部署在其他服务器上，修改 `api_url`：

- **本机**：`http://localhost:11434/api/generate`
- **局域网其他机器**：`http://192.168.1.100:11434/api/generate`
- **远程服务器**：`http://your-server.com:11434/api/generate`

## 🚀 使用方法

### 方法1：使用配置文件（推荐）

配置好 `ai_translation_config.json` 后，直接运行：

```bash
python merge_hpo_chinese.py --mapping "hpo_chinese_mapping.csv"
```

脚本会自动从配置文件读取AI翻译设置。

### 方法2：命令行参数（覆盖配置文件）

如果需要临时使用不同的配置，可以使用命令行参数：

```bash
python merge_hpo_chinese.py \
  --mapping "hpo_chinese_mapping.csv" \
  --use-ai \
  --ai-api-url "http://other-server:11434/api/generate" \
  --ai-api-type ollama \
  --ai-model "deepseek-r1"
```

**注意**：命令行参数会覆盖配置文件中的设置。

## ✅ 验证配置

运行脚本时，如果配置正确，会显示：

```
✓ 已从配置文件加载AI翻译设置
  API地址: http://localhost:11434/api/generate
  API类型: ollama
  模型: deepseek-r1
  超时: 30.0秒
```

## 🔧 常见问题

### Q1: 如何禁用AI翻译？

将配置文件中的 `enabled` 设置为 `false`：

```json
{
  "enabled": false,
  ...
}
```

### Q2: 如何测试Ollama连接？

在命令行测试：

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "deepseek-r1",
  "prompt": "测试",
  "stream": false
}'
```

### Q3: 配置文件格式错误怎么办？

确保JSON格式正确：
- 使用双引号
- 最后一个配置项后不要有逗号
- 使用 `null` 而不是 `None` 或空字符串

### Q4: 如何查看翻译缓存？

翻译缓存保存在：`提取结果/hpo/translation_cache.json`

可以查看已翻译的内容，避免重复调用API。

## 📝 注意事项

1. **首次使用**：建议先用少量数据测试，确认翻译质量
2. **翻译缓存**：已翻译的内容会保存，避免重复调用
3. **超时设置**：根据服务器响应速度调整 `timeout`
4. **API限制**：注意API调用频率限制，避免过载

