# AIdoctor V1 — 本地合成数据人工验收（Windows）

> **仅供研发合成数据测试**，不是医疗问诊服务，也没有经过生产临床授权。
> 此模式的 C01 输出、临床规则、问题选择和交付确认均为固定的合成测试夹具；它只能用于验证多轮执行链、状态机和数据库持久化，不能用于评价真实医疗问诊效果。

## 前提

- Windows + Docker Desktop，Docker Engine 已启动
- Java **8**、Maven、Python 3、Node.js 20 / npm
- 本机的 **8084**、**33333** 和 **3000** 端口空闲
- Git checkout 为 PR #507 当前分支 `feat/v1-clinical-runtime-cutover`
- 不要输入真实患者信息。脚本只会创建独立容器 `aidoctor-v1-synthetic-manual-mysql`，不会使用现有数据库

## 第一步：启动 MySQL 与合成后端

在仓库根目录打开 PowerShell：

```powershell
py -3 tools/v1_clinical_e2e/local_synthetic_manual_server.py --accept-disposable-db
```

脚本会依次：

1. 以 Docker Desktop 启动 MySQL 8，绑定 `127.0.0.1:33333`
2. 应用与 CI 相同的临时 CDP、U01、U06/WAIT/F8 表结构
3. 安装 contracts/v1 Java8 构件，编译测试 classpath
4. 启动 `V1LocalSyntheticManualServer`，固定绑定 `127.0.0.1:8084`

这个 PowerShell 窗口保持运行。按 Ctrl+C 结束后，脚本会停止并删除**由它启动**的临时 MySQL 容器及其数据。如果容器名已被占用，脚本会拒绝启动，不会删除现有容器。

如需只在终端检查宿主，打开另一个 PowerShell 窗口：

```powershell
Invoke-RestMethod http://127.0.0.1:8084/internal/nonproduction/v1/clinical/runtime-readiness
```

应显示 `COMPONENTS_PRESENT_NOT_E2E_VERIFIED`，`syntheticFixturesOnly=true`，不可把它理解为正式临床认证。

## 第二步：启动前端

在第二个 PowerShell 中：

```powershell
cd frontend
npm ci
npm run dev
```

打开 **http://localhost:3000/test/v1-clinical**。页面通过 Vite 代理访问后端 `localhost:8084`。

使用默认的 `synthetic-` 开头的合成用户、合成症状，点击“启动首轮并建立 WAIT”，应显示追问文本及 Checkpoint；提交回答，应显示第二轮状态。

**重要：** 由于目前为固定测试夹具，临床风险输出和追问不是模型对任意输入的真实理解。人工验收要验证的是可靠状态流转和可交互性，不能据此推断医疗能力或临床有效性。

## 第三步：检查数据库和异常路径

可以通过新终端查看临时容器数据（只在 synthetic 容器内）：

```powershell
docker exec -e MYSQL_PWD=synthetic-u01-u02-root aidoctor-v1-synthetic-manual-mysql mysql -uroot v1_u01_u02 -e "SELECT consultation_id, lifecycle_status FROM clinical_consultation ORDER BY created_at DESC LIMIT 5;"
docker exec -e MYSQL_PWD=synthetic-u01-u02-root aidoctor-v1-synthetic-manual-mysql mysql -uroot v1_u01_u02 -e "SELECT checkpoint_id, checkpoint_status, clinical_state_version FROM clinical_runtime_wait_checkpoint ORDER BY created_at DESC LIMIT 5;"
```

核对：首轮 WAIT 存在，回答后原 Checkpoint 由 `ACTIVE` 转为 `CONSUMED`，CDP 版本递增，第二轮风险、安全门禁有记录。使用错误 Checkpoint 时必须拒绝；同一回答不得触发第二次状态写入。

## CI 证据

GitHub Workflow `V1 U01-U02 Trusted MySQL Physical Readback (Nonproduction)` 包含 `V1SyntheticTwoTurnTcpMysqlTest`，它通过真实 TCP 请求走第一轮 WAIT、第二轮 F8/U02/U03/U04，要求 MySQL 读回且测试不能 skip。

`V1 Clinical Synthetic Manual Frontend Build` 验证浏览器页面可以构建。

生产与临床就绪仍然 **NOT PRODUCTION READY**。此测试宿主仅在 `diagnosis-service/src/test/java` 中，不能部署为患者服务。
