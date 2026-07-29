# AIdoctor 当前系统资产盘点

> 文档状态：Draft v2.6 Inventory  
> 更新时间：2026-07-29  
> 盘点范围：`cxjchelsea/AIdoctor` 当前重构分支及其继承的主分支代码  
> 关联文档：[迁移矩阵](./migration-matrix.md) · [Capability Package](./capability-package-specification.md) · [Model Runtime](./prompt-and-model-runtime-design.md)

---

## 1. 盘点目的与证据等级

本文件回答“当前仓库实际上有什么”，不以 README 中的完成百分比代替编译、运行和测试结果。

证据等级：

| 等级 | 含义 |
|---|---|
| CODE_CONFIRMED | 已查看实际源码或配置 |
| DOC_CLAIMED | 仅有文档或注释声明 |
| RUNTIME_VERIFIED | 已编译、启动或执行测试 |
| DATA_VERIFIED | 已检查真实数据库或索引样本 |
| UNKNOWN | 尚未取得足够证据 |

当前主要结论属于 `CODE_CONFIRMED`；编译、启动、数据库和 E2E 仍需在 Phase A 完成。

---

## 2. 顶层结构

当前仓库主要包含：

```text
Java / Spring Boot
├── diagnosis-service
└── examination-service

Python / FastAPI
├── health-state-assessment-service
├── clinical-parsing-service
├── dialog-service
├── diagnosis-engine-service
├── workup-planner-service
├── treatment-engine-service
├── risk-assessment-service
├── explanation-service
├── ocr-service
└── execution-trace-service

Shared
└── common/aidoctor_llm

Frontend
└── React + Vite + Ant Design + Zustand + SockJS/STOMP

Other
├── science/
├── scripts/
├── docs/
└── docker-compose.yml
```

当前物理结构按照“步骤/工具/脑区”拆分，目标结构按照状态所有权、安全边界、Runtime 和治理职责拆分，两者不能一一改名映射。

---

## 3. Java 资产

### 3.1 技术基线

已观察到：Java 8、Spring Boot 2.7.x、JPA/Hibernate、Redis、WebSocket、Feign、Nacos、Flyway、MySQL/Oracle 适配。

### 3.2 `diagnosis-service`

当前能力：

- `/start`、`/continue`、`/status`、`/result` 等诊断 API；
- 创建 CDP 和 Session；
- 调用健康状态判定服务；
- 固定 Workflow 编排；
- 兼容旧接口；
- CDP 版本历史。

结构问题：

- Java 同时承担业务入口、临床状态、固定流程和部分下一步决策；
- 远程服务失败时存在默认继续流程；
- 状态更新以 `Map<String,Object>` 为主；
- `conclusionPackage`、Trace、Audit 等混入临床聚合对象。

### 3.3 CDP

当前 CDP 主要以单表和 CLOB/JSON 保存：患者状态、DDX、证据图、检查计划、治疗计划、分诊、不确定性、Audit 和 Execution Trace。

风险：

- 字段语义和来源弱约束；
- 临床事实、模型候选、运行状态和审计混合；
- 部分更新依赖悲观锁和重试；
- 旧 JSON 内可能存在未文档化字段。

Phase A 必须抽样真实 CDP，输出字段频率、空值、坏数据、未映射字段和版本分布。

---

## 4. Python 服务资产

| 服务 | 可复用候选 | 主要问题 |
|---|---|---|
| health-state-assessment | 红旗、风险、输入校验、健康流程样例 | 与 risk 服务重叠，最终分诊边界不统一 |
| clinical-parsing | 词表、概念归一化、结构化抽取 | 读取 CDP、自建 ToolResult、质量常量、模型调用边界不统一 |
| dialog | 信息缺口、问题模板、NLU/NLG、通信经验 | 同时承担 Context、Question、CDP、WebSocket 和模型调用 |
| diagnosis-engine | KG、规则、统计、模型 Adapter | 候选、证据和最终结论边界不足 |
| workup-planner | 检查计划原型 | 高风险、证据和医生审核不足 |
| treatment-engine | 治疗原型和术语 | 不进入首个生产 Capability |
| risk-assessment | 分诊、升级、reason code | 与 health 风险职责重叠 |
| explanation | NLG、路径展示 | Evidence、Decision、Presentation 混合 |
| ocr | OCR Adapter | SourceArtifact、质量和确认状态不足 |
| execution-trace | 时间线、调用树、UI | 自定义 Trace 与临床数据混合，不能用于恢复 |

首版目标不是继续新增独立步骤服务，而是把核心逻辑迁入一个 FastAPI/LangGraph Runtime 的内部 packages，旧服务通过 Adapter 过渡。

---

## 5. 前端资产

已观察到 React、Vite、Ant Design、Zustand、Axios、SockJS/STOMP、ReactFlow 等资产。

可保留：

- 页面布局和通用组件；
- 对话、状态、时间线和调用图展示经验；
- ReactFlow 可视化；
- 基础 API Client。

需改造：

- 从“诊断步骤状态”迁为 Encounter/Thread/Interrupt/Review 状态；
- 增加 Reload/Resume；
- 增加患者、医生、管理端角色边界；
- Evidence、Citation、Knowledge Release 和不确定性展示；
- Prompt/Model/Capability/Knowledge 发布管理；
- Unit/Component/E2E/Accessibility 测试。

---

## 6. 数据与基础设施资产

### 6.1 数据存储

当前描述或配置涉及 MySQL、Oracle、Redis、Neo4j、Milvus 等组件。目标 PostgreSQL/pgvector 尚未通过运行验证。

### 6.2 Compose 与配置

当前 Compose 只覆盖部分服务，服务依赖和端口存在不完整或不一致风险；配置中需要检查硬编码密码、供应商密钥、模型名称和直接 URL。

### 6.3 Trace

现有 execution-trace-service 有可复用的 AgentEvent 和 UI 思路，但技术 Trace 应迁 OpenTelemetry，且普通遥测不得保存完整 PHI。

---

## 7. Capability 资产专项盘点

首个目标包：`adult_respiratory_v1`。

必须建立以下清单：

| 资产类别 | 盘点字段 |
|---|---|
| Scope | 支持主诉、年龄、人群、排除项、停止条件 |
| Terminology | 词典文件、编码体系、同义词、版本、来源 |
| Observation | 字段、类型、单位、否定、时间、严重度 |
| Safety | 红旗规则、分诊规则、阈值、来源、测试病例 |
| Question | 问题模板、触发条件、重复控制、信息价值 |
| Hypothesis | 候选疾病、must-not-miss、支持/反对证据 |
| Tool | 允许调用的旧服务、权限、输入字段、失败策略 |
| Delivery | 患者表达、医生摘要、就医导航和限制 |
| Eval | 红旗病例、普通病例、对话样例、攻击样例 |

当前结论：呼吸道规则、术语、问题和候选散落在多个服务和文档中，尚未形成可发布的 Capability Package。

---

## 8. Prompt Inventory

必须扫描 `.py`、`.java`、`.yaml`、`.yml`、`.json`、`.properties` 和数据库初始化脚本，建立：

| 字段 | 说明 |
|---|---|
| location | 文件、类、函数或配置路径 |
| prompt_id_candidate | 迁移后的稳定 ID |
| task_type | extraction、question、summary、evidence、delivery 等 |
| caller | 当前调用模块 |
| capability | 通用或场景专用 |
| input_fields | 当前传入字段 |
| output_format | 自然语言、JSON、Pydantic 等 |
| system_instruction | 是否存在系统级指令 |
| contains_phi | 是否处理 PHI |
| model | 当前模型或配置项 |
| temperature | 当前参数 |
| test | 是否有回归测试 |
| status | active、unused、unknown |
| decision | EXTRACT、ADAPT、ARCHIVE、REMOVE |

禁止把未盘点 Prompt 直接复制进新 Runtime。

---

## 9. Model Call Inventory

每一处模型调用必须登记：

```text
调用文件和函数
供应商与模型名称
任务类型
Prompt 来源
输入数据分类
是否含 PHI
结构化输出方式
超时、重试和 Fallback
Token 与成本记录
错误处理
Trace 字段
当前评估证据
目标 route_id
```

重点扫描：

- `common/aidoctor_llm`；
- 各 FastAPI service 的 provider client；
- 直接 HTTP 调用；
- 环境变量中的模型名；
- 流式调用；
- JSON 修复和重试逻辑；
- Embedding 和 Reranker 调用。

当前结论：`common/aidoctor_llm` 可作为 ProviderAdapter 起点，但是否存在绕过该公共模块的直接调用尚需全仓扫描确认。

---

## 10. Knowledge Asset Inventory

每个知识资产必须登记：

| 字段 | 说明 |
|---|---|
| asset_id | 唯一编号 |
| location | 文件、数据库、Neo4j、向量库或外部源 |
| title/source | 正式名称和发布机构 |
| source_type | guideline、regulatory、review、reference 等 |
| version/date | 版本和时间 |
| region/population | 适用地区和人群 |
| license/access | 许可和访问范围 |
| clinical_review | 审核人和状态 |
| checksum | 原始内容校验 |
| current_index | 当前 BM25/vector/graph 位置 |
| provenance | 节点、关系或 Chunk 是否可追溯 |
| decision | RELEASE、REVIEW、ARCHIVE、REJECT |

禁止进入生产 Knowledge Release：来源不明、许可不明、版本不明、无临床审核、模型生成且无法追踪原始来源的内容。

---

## 11. Neo4j / 知识图谱专项盘点

必须导出并统计：

- 节点标签、数量和属性；
- 关系类型、数量和方向；
- 每条关系的来源、版本和时间；
- 是否包含患者数据；
- 是否有重复、孤立、冲突和无来源关系；
- 当前查询入口和被哪些服务使用；
- 是否支持术语归一化、Query Expansion 或 must-not-miss；
- 与无图检索基线相比的收益。

现有图谱在完成来源治理和对照实验前，只能作为候选资产，不得直接成为循证依据。

---

## 12. 测试与运行资产

必须盘点：

- Java 单元和集成测试；
- Python pytest、fixture 和 mock；
- 前端测试；
- Docker Compose 启动脚本；
- 数据库 Migration；
- 红旗和分诊病例；
- RAG 检索与 Citation 数据集；
- Prompt/Model Eval；
- E2E 和 Crash Matrix；
- 性能与安全测试。

README 中“完成”但无测试或无法运行的能力，状态不得高于 `DOC_CLAIMED`。

---

## 13. Phase A 盘点输出

Phase A 完成时必须交付：

```text
service-inventory.csv
api-inventory.csv
database-inventory.csv
prompt-inventory.csv
model-call-inventory.csv
knowledge-asset-inventory.csv
neo4j-inventory.csv
rule-inventory.csv
test-inventory.csv
configuration-inventory.csv
```

每条记录必须关联 Owner、目标模块、迁移决定、验证证据、目标阶段和下线条件。

---

## 14. 当前结论

1. 当前系统包含可复用的临床解析、规则、KG、问题和展示原型；
2. 最大问题不是“完全没有能力”，而是状态、安全、模型、知识和发布边界混杂；
3. 呼吸道场景资产尚未收敛为 Capability Package；
4. Prompt、Model Call 和 Knowledge Asset 必须在最终冻结前完成全量盘点；
5. 未经运行和数据验证的资产不得直接标记 KEEP、REMOVE 或进入生产 Release。