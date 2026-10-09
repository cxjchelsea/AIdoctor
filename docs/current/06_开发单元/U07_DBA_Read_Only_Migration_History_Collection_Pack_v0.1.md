# U07 — 真实环境迁移历史：授权 DBA 只读采集包 v0.1

> **本文件和 SQL 只是交付给已获准访问环境的 DBA 使用。ChatGPT/GitHub Actions 不连接真实数据库。**
>
> **当前现网证据：NOT_COLLECTED / UNKNOWN。** 数据库历史、checksum 和实际表结构尚未验证，不能依据合成 CI 结果推断现网。

## 1. 使用流程

1. 环境负责人审批：明确数据库环境别名、只读权限、数据处理与输出审核责任人。
2. DBA 在**其受控终端**使用已有、只读的数据库访问方式，分别参考下列 SQL 模板逐条执行：
   - MySQL：`diagnosis-service/scripts/u07_legacy_dba_readonly_mysql.sql`
   - Oracle：`diagnosis-service/scripts/u07_legacy_dba_readonly_oracle.sql`
3. 必须先检查 `flyway_schema_history` 是否存在；不存在时**不得运行对该表的第二条查询**，只报告 `absent`。
4. 如果存在迁移历史，DBA 仅记录 `version/checksum/success/type` 等**经审核的元信息**，不要上传原始控制台日志、数据库 URL、用户名、IP、业务表数据。Oracle 的 Flyway history owner/schema 必须单独验证。
5. 对 9 个历史字段，仅记录列名和 SQL 数据类型；不导出真实列值。
6. 按 PR #365 的白名单 JSON 输入规范在内部人工整理信息；在得到环境拥有者批准前，**不要上传到公开 GitHub PR**。如不满足许可，结论维持 `UNKNOWN`。
7. 经审批后的本地离线分类可用：
   `python3 diagnosis-service/scripts/validate_u07_migration_history_evidence.py --input /path/to/approved-sanitized-metadata.json`
   该程序不访问数据库，也不证明输入真实性。
8. E0–E4 仅用于下一步制定部署方案，不是 migration / baseline / repair / merge 授权。

## 2. 必须特别注意的边界

- **只读 ≠ 自动安全**：DBA 必须在批准的环境和帐户下执行；大表的元数据或管理表查询依然可能受审计与权限限制。
- 不要把两个数据库的历史混在一起：MySQL 原始 V1 与 Oracle V1 是不同内容。
- PR #362 的 V1 `CLOB → LONGTEXT` 属于**隔离测试转换**，改变 Flyway checksum，不能部署到有历史的数据库。
- MySQL 8 + Flyway 7/9 的合成隔离矩阵 PASS，不代表任何现网库运行、升级或 Oracle 兼容性已经通过。
- 对 `absent`、`partial`、`mismatch` 和未知 migration history 只能给出审查建议，不运行 `repair`、`baselineOnMigrate` 或任何 DDL。
- 本次脚本**不会**调用 SQL 客户端或环境变量中的 JDBC URL，不包含凭据处理代码，亦不能验证来自他人的截图和报表是否真实。

## 3. 回收结果最小化

| 项目 | 只记录 |
|---|---|
| 环境 | 不含 hostname/IP 的别名 |
| 数据库版本 | MySQL/Oracle 主版本号 |
| Flyway 版本 | 实际运行版本，而不是只看 pom.xml |
| history 状态 | present / absent / unknown |
| 迁移状态 | highest_success_version、失败/部分执行等标签 |
| V1 关联 | 与受控构建 checksum 的匹配/不匹配/未知 |
| 表结构 | 9 个历史字段的类型摘要；没有字段内容 |
| 权限 | 环境所有者审批状态及内部可追溯编号（不公开人员或环境机密） |

**本轮实施就绪判定**：`DBA_QUERY_TEMPLATES_READY` 可以通过静态校验；`LIVE_DATABASE_EVIDENCE` 仍为 `UNKNOWN`，不得宣布 Release Compatibility Ready。
