# U07 — Legacy Migration Release Compatibility Review v0.1

**评审日期**：2026-10-09  
**基线**：`main@86e8843197091c8c8172b7e4213537a31bdf0654`；PR #361 `ae315cb7614c2af5e739352d993e387f2891d51e`；PR #362 `fbbc56c8c1ce151645c440bec5b89c2778da10dc`。本评审文件独立于上述未合并的 Draft PR。  
**评审判定**：**RELEASE_COMPATIBILITY = NOT_READY**；测试专用 MySQL 兼容路径 **VALIDATED_IN_ISOLATION**；真实部署状态 **UNKNOWN**；**不授权合并、生产迁移、Flyway repair、baselineOnMigrate 或改写已发布历史**。

## 1. 证据与已验证事实

| 来源 | 已确认内容 | 能证明什么 / 不能证明什么 |
|---|---|---|
| `diagnosis-service/pom.xml` | Spring Boot 2.7.8，Java 8，`flyway-core 7.15.0`，Oracle JDBC 与 MySQL JDBC | **应用实际依赖 Flyway 7.15.0**；此前 CI 使用 Flyway 9.22.3，并非相同版本 |
| `diagnosis-service/src/main/resources/application.yml` | `spring.profiles.active: dev`，注释将 dev 描述为公司 Oracle 部署，mysql 为可选本地测试 | 禁止默认启动 Spring / 自动迁移到 dev；实际运行时 datasource、locations 和部署覆盖配置尚未核验 |
| `db/migration/V1__create_agent_state_and_audit_trail.sql` | Git Blob `dfd87844be26015131fffa57430d3d982b5a4ef6`，MySQL 迁移里 9 处 `CLOB` | 原始 V1 在本次 MySQL 8.0 失败；不能假定已有部署采用同一条历史 |
| `db/migration-oracle/V1__init_diagnosis_tables.sql` | Oracle V1 是不同内容、不同版本链；包含合法 Oracle `CLOB` | 不允许对 Oracle V1 统一做 MySQL 的 `LONGTEXT` 替换；两条链要分别评估 |
| CI [run 37902825402](https://github.com/cxjchelsea/AIdoctor/actions/runs/37902825402) | MySQL 8.0 + Flyway 9.22.3 **原样 V1–V7 在 V1 的 CLOB 处 SQL 1064 失败** | 证明此组合的全新库首次安装受阻，尚未达到 V7 |
| CI [run 37903042412](https://github.com/cxjchelsea/AIdoctor/actions/runs/37903042412) | 原样 V7 在隔离空库单独执行成功；表/键、重复 migrate、合成 DML 回滚通过 | 证明 V7 自身 DDL 在 MySQL 8.0 可执行；不能证明原始 V1–V7 可升级 |
| CI [run 37904033975](https://github.com/cxjchelsea/AIdoctor/actions/runs/37904033975) | 测试副本把 **精确 9 处 V1 CLOB→LONGTEXT**；V2–V7 逐字节复制；Flyway 9 在新 MySQL 8.0 完成 7/7 migrate、validate 和重跑 | 证明**测试专用兼容副本**可安装；它改动 V1 checksum，不代表生产可升级或可以部署 |
| 真实 Oracle / MySQL 部署的 `flyway_schema_history` | 本次无法获得 | **UNKNOWN**；不推测是否已应用 V1、是否手工建表、是否执行 repair/baseline |

## 2. 两种互不等价的兼容性

**A. 新数据库首次安装（install compatibility）**：对于使用原始 MySQL V1 的 Flyway 自动建库，已在 MySQL 8 + Flyway 9 验证失败。测试副本的成功只能证明 `LONGTEXT` 代替旧 `CLOB` 的 DDL 可通过，不代表有安全的发布级改法。

**B. 现有数据库升级（upgrade compatibility）**：即使某个环境已处于 V6，Flyway 在升级前也可能检查历史脚本校验和。修改/替换 V1 将改变校验和，导致历史验证失败；使用 `repair` 强行修改历史记录会掩盖真实结构来源，因此严禁在未核实之前执行。既有库的数据与表结构亦未验证，不能因新空库通过而宣称既有库升级安全。

**C. 数据库引擎与迁移工具差异**：MySQL 8 + Flyway 9 是测试组合；应用依赖 Flyway **7.15.0**。MySQL 5.7/8.0 的实际部署版本、驱动、字符集、sql_mode、大小写规则、事务隔离与 Flyway history checksum 算法及启动配置必须独立核验。Oracle 的迁移另走 `migration-oracle`，Flyway location 的真实配置尚需证明。

## 3. 现网只读事实收集清单（由拥有环境权限的负责人实施；不得在本评审中连接数据库）

采集每个环境的**脱敏**摘要：引擎/版本、部署 artifact ID、Flyway 版本和 datasource/location 配置来源；核查是否存在 `flyway_schema_history`，以及其版本、描述、类型、`script`、`checksum`、`installed_on`、`success`（不需要任何临床表行或 PHI）。

还需确认：
1. `agent_state`、`audit_trail` 是否存在，其 9 个文本列实际类型及各表创建来源。
2. 当前最高已成功迁移版本；是否有失败/跳过/手工 baseline、repair 或变更过历史 SQL。
3. 现有文件 V1 的 Git Blob、部署打包后的 V1 字节及历史记录 checksum 是否一致。
4. 生产/测试 Flyway 具体发行版本是否为 7.15.0，是否通过配置覆盖默认位置、手工启动、Spring Flyway auto-migrate。
5. 新部署是空库新建还是经批准的 schema baseline；已有库如何从旧链升级。
6. 执行上述只读调查前需获得环境所有者许可；结果只汇总字段类型与迁移元信息，不能外传口令、URL、患者数据。

## 4. 候选解决方式对比（条件分支，而不是立即决定发布）

| 方案 | 适用边界 | 主要风险 / 判定 |
|---|---|---|
| **R1 保持已应用 V1 不变、后续只增加新版本 V8+** | 所有已成功应用原始历史的数据库；从 V6→V7 继续以原脚本升级的可行性须另测 | **首选既有库路线**，但 **单靠 V8 无法解决空库在 V1 就失败**。不能将此解释为新安装修复 |
| **R2 新安装专用、显式版本化的 MySQL bootstrap/baseline 路线** | 从零新建、与现有历史隔离的全新 schema，且经产品/迁移 owner 审批 | 候选新安装路线；需明确 schema 等价性、checksum/history 基准、与旧库并存和后续 V8+ 共用规则。不可将测试 overlay 的 V1 作为普通发布迁移放进现有链 |
| **R3 受控修复历史 V1** | **仅证明**它从未被任何支持的安装应用，且不存在保留 checksum 的发布义务时 | 需要全环境历史证据；当前 **NOT_AUTHORIZED**。不应默认选择 |
| **R4 `flyway repair` / `baselineOnMigrate=true` / 直接手改 history** | 不作为普通修复手段 | **REJECT AS DEFAULT**：可能伪造历史一致性、掩盖 schema drift 或跳过必要迁移；任何例外必须独立审计与显式授权 |
| **R5 运行时替换 V1 文件/自动改 SQL** | 仅 PR #362 的一次性 synthetic CI | **TEST_ONLY**：修改 V1 checksum；禁止嵌入生产启动链、镜像或真实库 |

**当前推荐方向**：将**已有库的前向升级**与**新库的受控安装基线**设计为两条可审计路径，最终都汇合到同一规范 schema 与未来 V8+ 迁移；在获得实际 history 之前不选择具体发布操作，也不声称已完成兼容。

## 5. 发布前强制验证矩阵

| 场景 | 必须验证 | 当前状态 |
|---|---|---|
| M1 原样 V1–V7，新 MySQL 8 + Flyway 9 | 安装及校验 | **FAIL at V1 (已证实)** |
| M2 测试兼容副本 V1–V7，新 MySQL 8 + Flyway 9 | 仅证明迁移结构可行 | **PASS, TEST_ONLY** |
| M3 原样 V7，新 MySQL 8 + Flyway 9 | V7 DDL | **PASS, V7_ONLY** |
| M4 当前正式 Flyway 7.15.0 + 目标 MySQL 版本 | 新安装路线、校验和、重复 migrate | **NOT_RUN** |
| M5 已有历史 V1–V6 的脱敏、无 PHI schema clone | 在历史一致下升级 V7、validate、重跑；必要的 checksum 漂移检测 | **NOT_RUN** |
| M6 新安装候选 bootstrap/baseline → 后续 V8+ | 新老路线结构与 Flyway history 可升级性对齐 | **NOT_DESIGNED/NOT_RUN** |
| M7 Oracle 真实隔离实例（含 V1–V7） | 独立 migration location、DDL、差异检查 | **NOT_RUN** |
| M8 失败中断、DDL 部分提交、重新部署恢复 | 确认 Flyway history 与 schema 状态，不将 DML rollback 当成 DDL rollback | **NOT_RUN** |

## 6. 决策与实施门槛

**Independent Review Verdict: NOT_READY_FOR_RELEASE / READY_FOR_BOUNDED_FACT_COLLECTION**

Blocking findings:
- **LRC-01**：缺少已部署数据库的真实 Flyway history、checksum、V1 实际结构和数据库版本；
- **LRC-02**：新库安装与老库升级尚无经过授权的双路线方案；
- **LRC-03**：应用 Flyway 7.15.0 与此前 CI 9.22.3 不一致；
- **LRC-04**：Oracle 隔离引擎的迁移/历史未验证；
- **LRC-05**：运行时自动迁移与 `dev` 公司 Oracle 默认 profile 的隔离配置缺少部署级证明。

Allowed next task: **Legacy Baseline Compatibility — Environment Evidence Intake + Flyway 7 Test Matrix**。先用独立 CI 合成数据库核对 Flyway 7.15.0；再由获授权的 owner 只读提供脱敏历史记录和 schema 摘要；最后为新安装与升级设计分叉方案，逐一实施隔离验证。

Explicitly not authorized: modifying historical V1 on main, `flyway repair`, silent baseline, remote or production DB migration, Oracle company DB access, schema delete, automatic merge, P02/U02 / clinical runtime activation.

> 审查级别：基于代码及已完成 CI 的独立视角文档复审。未接触任何部署数据库，也未运行新一轮引擎测试。本评审对部署事实保留 UNKNOWN。
