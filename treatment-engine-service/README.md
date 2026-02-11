# 治疗推理服务（Treatment Engine Service）

## 服务说明

治疗推理服务（tool_5）负责：
- 治疗方案推理
- 药物推荐（研发阶段不涉及具体剂量）
- 非药物治疗建议

## 端口

- 服务端口：8091

## 启动方式

```bash
# 本地开发
uvicorn app.main:app --host 0.0.0.0 --port 8091

# Docker
docker build -t treatment-engine-service .
docker run -p 8091:8091 treatment-engine-service
```

