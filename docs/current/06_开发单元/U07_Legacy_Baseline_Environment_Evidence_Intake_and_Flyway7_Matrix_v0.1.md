# U07 Legacy Baseline — Environment Evidence Intake + Flyway 7 Test Matrix v0.1

**范围**：独立 PR / 测试专用，不激活 Spring，不访问任何真实数据库。依据：main 的 Spring Boot 2.7.8 + `flyway-core 7.15.0`，PR #361/#362 的 MySQL 8.0/Flyway 9.22.3 隔离证据，以及 PR #363 的发布兼容性审查。

## A. 执行矩阵（仅 GitHub-hosted disposable MySQL）

`.github/workflows/u07-flyway7-compatibility-matrix.yml` 使用两套 Flyway 实际版本和两种隔离模式。所有结果都仅解释为测试结论；不构成已部署库升级授权。

| 行 | MySQL | Flyway | 模式 | 目标 |
|---|---|---|---|---|
| F7-1 | 8.0，Docker 一次性实例 | **7.15.0**（应用同版本） | `v7_only` | 独立执行未改动的 V7 SQL |
| F7-2 | 8.0 | **7.15.0** | `legacy_compat_full` | test-only V1 的九个 `CLOB→LONGTEXT` + 原样 V2–V7 |
| F9-1 | 8.0 | 9.22.3（之前测试用） | `v7_only` | 复核此前 V7 结果 |
| F9-2 | 8.0 | 9.22.3 | `legacy_compat_full` | 复核此前测试兼容结果 |

验收：Flyway `migrate`、`validate`、二次 `migrate`、history 数量、U07 两表存在、可空唯一键/非空冲突、Outbox 主键、synthetic DML rollback。**`legacy_compat_full` 是修改了 V1 checksum 的测试目录，不得用来评价原样历史迁移 checksum 的兼容性。** 原样 MySQL V1–V7 于此前真实测试已在 V1 SQL1064 失败。

如果 Flyway 7 镜像不存在、受限 JDBC driver 失败或不支持指定 MySQL 版本，记录实际错误，**不可用 Flyway 9 的 PASS 顶替。**

## B. 部署环境只读证据采集——需要环境所有者单独授权

当前会话**没有**连接公司/生产数据库、也没有查看数据库账号、环境地址、任何患者业务表内容。以下是**交给已获授权负责人的操作清单**，不是让机器人自动连接的命令。

### 环境指纹（人工填报，脱敏）

| 字段 | 填报准则 |
|---|---|
| `environment_alias` | DEV-ORACLE / TEST-MYSQL 等不含域名/IP 的代号 |
| `engine_family/version` | Oracle/MySQL；主版本号即可 |
| `application_build_ref` | 可公开的部署 commit/tag，不含内部目录或 host |
| `flyway_runtime_version` | 实际运行版本（不能只引用 pom.xml） |
| `migration_locations` | 声明采用 Oracle/MySQL 哪条文件链；不回传连接串 |
| `history_table_exists` | yes/no/unknown |
| `highest_success_version` | 只记录版本值，不读取任何医疗业务记录 |
| `history_anomalies` | failure / baseline / repair / deleted / unknown 标签 |
| `v1_record_checksum` | 在环境安全许可下提供数值或仅提供与受控构建 checksum 的比较结果 |
| `v1_source_blob_equivalence` | 记录匹配/不匹配/未知，不上传完整数据库导出 |
| `agent_state_audit_trail_exists` | 两表是否存在 |
| `legacy_text_column_types` | 九个列的 SQL 类型，不包含列值、患者数据 |
| `evidence_owner/approval_ref` | 环境管理员及审批编号，避免个人身份信息公开 |

### 可选只读 SQL 模板（仅有授权的 DBA 在受控环境执行）

MySQL 元数据查询例子：
```sql
SELECT version, description, type, script, checksum, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

```sql
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
       (table_name = 'agent_state'
        AND column_name IN ('thresholds','budget','failure_backoff','tried_tools','evidence_fusion_state','stop_conditions'))
       OR
       (table_name = 'audit_trail'
        AND column_name IN ('tool_call','cdp_update','agent_decision'))
      )
ORDER BY table_name, column_name;
```

**不能**运行任何 `repair`、`baseline`、`migrate`、`clean`、`ALTER`、`UPDATE` 或 `DROP`。只返回经过审查的非敏感摘要。Oracle `flyway_schema_history`/数据字典的实际表名及权限由 DBA 确认，不能擅自套用 MySQL 查询。

### 证据接收与分类

- **E0 NO_HISTORY**：不存在 history 且无受控 baseline；新库安装问题，先设计经授权的 MySQL bootstrap，不得启用 `baselineOnMigrate` 掩盖。
- **E1 V1..Vn_SUCCESS**：存在完整成功历史，记录实际 V1 checksum 与当前部署构建关系，设计仅前向升级路径。
- **E2 CHECKSUM_MISMATCH**：停止迁移与自动修复；核对实际部署脚本和历史执行事实。
- **E3 FAILED_OR_PARTIAL**：冻结升级，保留原始残留与历史，使用数据库管理员受控恢复方案。
- **E4 MANUAL_OR_UNKNOWN_BASELINE**：需要人工审计，禁止以任意版本号直接 baseline。

**输出状态必须保留 `UNKNOWN`，不可把没有拿到生产历史的情况写成“生产没有应用 V1”。**

## C. 发布决策门禁

- M1：Flyway 7 + MySQL 目标版本的隔离 CI 结果 PASS，且 V1 overlay 更改范围明确；
- M2：实际环境历史（E0–E4）分类已由授权人核实；
- M3：新安装和已有库升级是两份独立路线，各自证明 schema equivalence、历史校验和及后续迁移可行性；
- M4：Oracle 引擎迁移与 Flyway location 配置独立验证；
- M5：没有历史 SQL 就地改写、没有自动 repair/baseline，没有真实数据库副作用。

缺任何一项：`RELEASE_COMPATIBILITY=NOT_READY`。本文件与矩阵 CI 不提供合并许可，更不授权真实临床/PHI/P02/U02。
