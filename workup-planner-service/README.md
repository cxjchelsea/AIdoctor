# 检查建议服务（Workup Planner Service）

## 服务说明

检查建议服务（脑区D）负责：
- 检查价值评估
- 信息增益计算
- 检查优先级排序
- 验证计划构建

## 端口

- 服务端口：8090

## 启动方式

```bash
# 本地开发
uvicorn app.main:app --host 0.0.0.0 --port 8090

# Docker
docker build -t workup-planner-service .
docker run -p 8090:8090 workup-planner-service
```

