# Windows路径配置说明

## 问题

在Windows系统下，Docker的路径映射可能需要特殊处理。如果Promtail无法收集日志，请按照以下方式配置。

## 解决方案

### 方案1：使用绝对路径（推荐）

修改 `monitoring/docker-compose.yml` 中的 `promtail` 服务的 volumes 配置：

```yaml
promtail:
  volumes:
    - ./promtail-config.yml:/etc/promtail/config.yml
    # 使用绝对路径（替换为您的实际项目路径）
    - E:/pycharmProject/AIdoctor/health-state-assessment-service/logs:/var/log/aidoctor/health-state-assessment-service/logs:ro
    - E:/pycharmProject/AIdoctor/clinical-parsing-service/logs:/var/log/aidoctor/clinical-parsing-service/logs:ro
    - E:/pycharmProject/AIdoctor/diagnosis-engine-service/logs:/var/log/aidoctor/diagnosis-engine-service/logs:ro
    - E:/pycharmProject/AIdoctor/ocr-service/logs:/var/log/aidoctor/ocr-service/logs:ro
    - E:/pycharmProject/AIdoctor/dialog-service/logs:/var/log/aidoctor/dialog-service/logs:ro
    - E:/pycharmProject/AIdoctor/explanation-service/logs:/var/log/aidoctor/explanation-service/logs:ro
    - E:/pycharmProject/AIdoctor/workup-planner-service/logs:/var/log/aidoctor/workup-planner-service/logs:ro
    - E:/pycharmProject/AIdoctor/treatment-engine-service/logs:/var/log/aidoctor/treatment-engine-service/logs:ro
    - E:/pycharmProject/AIdoctor/risk-assessment-service/logs:/var/log/aidoctor/risk-assessment-service/logs:ro
    - E:/pycharmProject/AIdoctor/diagnosis-service/logs:/var/log/aidoctor/diagnosis-service/logs:ro
    - E:/pycharmProject/AIdoctor/examination-service/logs:/var/log/aidoctor/examination-service/logs:ro
    - E:/pycharmProject/AIdoctor/execution-trace-service/logs:/var/log/aidoctor/execution-trace-service/logs:ro
    - promtail-positions:/var/lib/promtail
```

**注意**：将 `E:/pycharmProject/AIdoctor` 替换为您的实际项目路径。

### 方案2：配置Docker Desktop文件共享

1. 打开 Docker Desktop
2. 进入 **Settings** -> **Resources** -> **File Sharing**
3. 添加项目根目录（如 `E:\pycharmProject\AIdoctor`）
4. 点击 **Apply & Restart**

### 方案3：使用符号链接（高级）

如果上述方案都不行，可以创建符号链接：

```bash
# 在项目根目录创建统一的日志目录
mkdir logs-collector

# 为每个服务创建符号链接
mklink /D logs-collector\health-state-assessment-service health-state-assessment-service\logs
mklink /D logs-collector\diagnosis-engine-service diagnosis-engine-service\logs
# ... 其他服务
```

然后在docker-compose.yml中只挂载 `logs-collector` 目录。

## 验证配置

启动Promtail后，检查日志收集：

```bash
# 查看Promtail日志
docker logs promtail

# 检查Promtail是否能访问日志文件
docker exec promtail ls -la /var/log/aidoctor/*/logs/
```

如果看到日志文件列表，说明配置成功。

