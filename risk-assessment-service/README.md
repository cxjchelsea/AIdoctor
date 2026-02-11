# 风险评估服务（Risk Assessment Service）

## 服务说明

风险评估服务（tool_6）负责：
- 高危识别
- 紧急程度分级
- 复评与升级规则
- 终点结论包构建

## 端口

- 服务端口：8092

## 启动方式

```bash
# 本地开发
uvicorn app.main:app --host 0.0.0.0 --port 8092

# Docker
docker build -t risk-assessment-service .
docker run -p 8092:8092 risk-assessment-service
```

