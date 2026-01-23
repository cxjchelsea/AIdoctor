# 快速启动指南

## 1. 安装依赖

```bash
pip install -r requirements.txt
```

## 2. 启动服务

```bash
python run.py
```

服务将在 `http://localhost:8082` 启动

## 3. 测试服务

### 方式1：使用测试脚本

```bash
python test_service.py
```

### 方式2：使用curl

```bash
curl -X POST "http://localhost:8082/api/v1/parsing/parse" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user",
    "sessionId": "test_session",
    "text": "我最近胸口闷，走几步就喘，之前有高血压"
  }'
```

### 方式3：访问API文档

打开浏览器访问：http://localhost:8082/docs

## 4. 验证服务状态

```bash
curl http://localhost:8082/health
```

## 注意事项

1. **词表数据**：确保 `data/vocabularies/` 目录下有所有归一化词表文件
2. **Python版本**：需要Python 3.10+
3. **端口占用**：确保8082端口未被占用

## 常见问题

### 问题1：词表加载失败

**解决方案**：
- 检查 `data/vocabularies/` 目录是否存在
- 检查词表文件格式是否正确（CSV格式）

### 问题2：服务启动失败

**解决方案**：
- 检查Python版本（需要3.10+）
- 检查依赖是否安装完整：`pip install -r requirements.txt`
- 检查端口是否被占用

### 问题3：识别结果为空

**解决方案**：
- 检查输入文本是否包含在归一化词表中
- 检查词表文件格式是否正确
- 查看日志输出获取详细错误信息

