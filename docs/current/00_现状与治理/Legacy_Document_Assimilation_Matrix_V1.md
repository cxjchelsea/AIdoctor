# Legacy Document Assimilation Matrix V1

> 状态：CURRENT MIGRATION CONTROL
> 目标：逐文件说明 `docs/AI医生/项目文档` 与 `docs/refactoring` 的保留、吸收、后置或替代策略。
> 原则：历史文件不因整理而删除；先吸收，再降级；`evidence/reviews` 保留历史真值。

## 1. 状态定义

| 状态 | 含义 |
|---|---|
| ABSORB | 有效内容需要吸收到新权威文档，旧文件随后作为历史参考 |
| REFERENCE | 保留为专题参考，不直接成为当前权威 |
| DEFER | 有价值但不属于当前 V1 / 当前 SOP 阶段 |
| SUPERSEDED | 设计已被当前方向替代，仅保留历史价值 |
| EVIDENCE_KEEP | 历史工程或审查证据，原样保留 |

---

# 2. `docs/AI医生/项目文档`

## 2.1 文档索引

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `0.文档索引/全局文档索引.md` | SUPERSEDED | 由 `docs/current/README.md` 取代 |
| `0.文档索引/按场景索引.md` | ABSORB | 后续 `05_业务闭环/业务闭环目录_V1.md` |
| `0.文档索引/按角色索引.md` | ABSORB | `docs/current/README.md` 后续角色阅读路径 |

## 2.2 系统概述

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `1.系统概述/AI医生系统 - 系统定位与核心理念.md` | SUPERSEDED | 与下项合并，不单独继承 |
| `1.系统概述/系统定位与核心理念.md` | ABSORB | `01_需求/需求与系统边界_V1.md`；保留“先判断是否进入临床流程、受控可解释推理”等思想；完整 Wellness 后置 |
| `1.系统概述/整体架构概述.md` | REFERENCE | `09_Runtime与技术架构/总体架构.md`，仅继承经验证的职责思想，不继承旧服务拓扑 |
| `1.系统概述/核心概念与术语.md` | ABSORB | 后续建立 `docs/current/术语与概念.md`，统一 CDP、DDx、Evidence、Must-Exclude 等术语 |

## 2.3 架构设计

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `2.架构设计/主Agent架构设计.md` | REFERENCE | `09_Runtime与技术架构/Runtime设计.md`；继承单一编排责任，不冻结旧 AgentLoop |
| `2.架构设计/双通道推理架构.md` | ABSORB | `09_Runtime与技术架构/总体架构.md` + `07_能力设计/`；继承结构化临床推理与语言/策略分离思想 |
| `2.架构设计/工具系统架构.md` | ABSORB | `07_能力设计/Capability规范.md`；继承无状态、可替换、可评估、明确 I/O 的能力设计 |

## 2.4 业务功能设计

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `3.业务功能设计/入口判定流程设计.md` | ABSORB | `01_需求`、`02_功能`、后续 F1/F4 业务闭环；吸收混合诉求、最小澄清、安全优先 |
| `3.业务功能设计/临床诊疗态流程设计.md` | ABSORB | `02_功能`、`03_状态`、`05_业务闭环`；吸收问题清单、缺口、候选分层、回退、验证计划 |
| `3.业务功能设计/终点结论包设计.md` | ABSORB | F5/F6/F7、`10_前端与交付/Patient_Delivery`；吸收 Must-Exclude、证据、不确定性；不继承自动确诊/治疗越界表达 |
| `3.业务功能设计/健康管理态流程设计.md` | DEFER | Future Scope；当前 V1 不建设完整健康管理态 |

## 2.5 工具设计

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `4.工具设计/工具接口规范.md` | ABSORB | `07_能力设计/Capability规范.md` + `08_契约与数据/Shared_Contracts.md`；继承职责、输入、输出、evidence、失败、测试；重写 fail-open 与直接 CDP 写入 |
| `4.工具设计/工具业务逻辑设计.md` | ABSORB | 后续各 Clinical Capability Spec；按 F2-F6 重新归属 |
| `4.工具设计/工具技术实现.md` | REFERENCE | 具体 Capability 实现阶段逐项复核，不作为当前技术真值 |

## 2.6 主 Agent 设计

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `5.主agent设计/主Agent运行循环设计.md` | REFERENCE | `09_Runtime与技术架构/Runtime设计.md`，作为策略循环历史参考 |
| `5.主agent设计/主Agent核心算法设计.md` | REFERENCE | Agent Policy / Capability 选择策略；后续按需吸收 |
| `5.主agent设计/主Agent业务逻辑设计.md` | ABSORB | `05_业务闭环`；仅吸收临床业务逻辑，不继承旧 Agent ownership |
| `5.主agent设计/主Agent技术实现.md` | SUPERSEDED | 当前 Runtime 目标替代旧技术实现 |

## 2.7 数据模型设计

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `6.数据模型设计/CDP数据结构设计.md` | ABSORB | `08_契约与数据/临床领域模型与CDP.md`；保留 CDP aggregate，重构状态、来源、写入治理 |
| `6.数据模型设计/AgentState数据结构设计.md` | ABSORB | 拆分到 `03_状态` 与 `09_Runtime`；预算、尝试、回退、停止条件保留，AgentState 不再作为第二业务真值 |
| `6.数据模型设计/AuditTrail数据结构设计.md` | ABSORB | `11_验证与评估/Trace_Audit_Observability.md` |
| `6.数据模型设计/实体类与DTO设计.md` | REFERENCE | `08_契约与数据/Shared_Contracts.md` 重建时逐项核验 |

## 2.8 接口规范

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `7.接口规范/REST API接口规范.md` | SUPERSEDED | 行为语义可提取到 `08_契约与数据/API与事件契约.md`；旧 URL/DTO 不再权威 |
| `7.接口规范/工具调用协议.md` | ABSORB | `07_能力设计/Capability规范.md` + Shared Contracts；继承 ToolContext/ToolResult 思想并升级为受控 StatePatch |

## 2.9 前端设计

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `8.前端设计/前端页面设计.md` | REFERENCE | `10_前端与交付/用户交互与页面设计.md` |
| `8.前端设计/用户交互流程设计.md` | ABSORB | F1-F8 与后续业务闭环/前端交互 |
| `8.前端设计/入口设计规范.md` | ABSORB | F1/F4 + 前端；删除客户端创造临床真值的旧逻辑 |

## 2.10 知识演化设计

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `9.知识演化设计/知识演化架构设计.md` | ABSORB | `07_能力设计/临床范围、医学知识与规则治理.md` |
| `9.知识演化设计/知识演化技术实现.md` | REFERENCE | Knowledge Platform 技术设计阶段 |
| `9.知识演化设计/知识演化业务逻辑.md` | ABSORB | 知识生命周期、Proposal/Review/Publish Gate |
| `9.知识演化设计/知识演化与知识维护-完整设计方案.md` | REFERENCE | 作为母参考，拆入知识治理与发布 Gate；禁止自动修改生产医学知识 |

## 2.11 实现与部署

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `10.实现与部署/项目结构设计.md` | SUPERSEDED | 当前 Repository Map / 目标架构重建后替代 |
| `10.实现与部署/技术栈选型.md` | REFERENCE | `09_Runtime与技术架构/总体架构.md` / ADR |
| `10.实现与部署/部署架构设计.md` | ABSORB | `12_工程与发布/部署与环境.md`，必须基于真实主链重新验证 |
| `10.实现与部署/环境配置指南.md` | REFERENCE | 实际环境核验后重写为 current 版本 |

## 2.12 错误处理与异常

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `11.错误处理与异常/错误处理规范.md` | ABSORB | `11_异常与安全/失败语义、安全与恢复规范.md` |
| `11.错误处理与异常/错误处理策略设计.md` | ABSORB | 同上；升级为 Retry/Repair/Fallback/Rollback/Suspend/Escalate/Fail Closed/Abort |
| `11.错误处理与异常/异常场景处理设计.md` | ABSORB | Failure Policy + 测试场景；保留候选为空、证据不足、资源耗尽等场景 |

## 2.13 性能与评估

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `12.性能与评估/性能优化设计.md` | REFERENCE | 后期性能工程，不提前优化 |
| `12.性能与评估/评估验证体系.md` | ABSORB | `11_验证与评估/Clinical_Eval体系.md`；继承 Static/Interactive/Trajectory 三类评估思路 |
| `12.性能与评估/复杂度分析.md` | REFERENCE | 架构优化阶段参考 |

## 2.14 技术细节

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `13.技术细节/DR.KNOWS路径推理设计.md` | REFERENCE | F5 / G1 / G6 的 Clinical Intelligence / Knowledge 能力 |
| `13.技术细节/路径注入LLM详细说明.md` | REFERENCE | Model Runtime 迁移时重新验证；不得直接恢复 legacy LLM 路径 |
| `13.技术细节/AOP切面设计思路.md` | ABSORB | `11_验证与评估/Trace_Audit_Observability.md` |
| `13.技术细节/LangChain架构开发流程.md` | SUPERSEDED | 框架不是产品/架构硬约束 |

---

# 3. `docs/refactoring` 顶层设计文档

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `README.md` | SUPERSEDED | 新 `docs/current/README.md` 成为当前入口；旧 README 保留历史导航 |
| `总架构与模块设计.md` | ABSORB | Phase 2-4 + `09_Runtime与技术架构/总体架构.md`；不原样继承 11 个技术模块为业务模块 |
| `架构冻结基线.md` | EVIDENCE_KEEP | `00_现状与治理` 的历史治理/ADR 参考 |
| `v2.6跨文档一致性补充.md` | ABSORB | 对应内容逐步进入 Capability、Knowledge、Model、Evidence、Delivery、Versioning 文档 |
| `capability-package规范.md` | ABSORB | `07_能力设计/Capability规范.md`；作为临床场景发布单元的主要参考 |
| `成人呼吸道RAG.md` | REFERENCE | 场景级 Knowledge/Capability 资产，不作为整个系统通用需求 |
| `模型调用与路由矩阵.md` | ABSORB | `09_Runtime与技术架构/Model_Runtime与Prompt治理.md` |
| `prompt-and-model-runtime设计.md` | ABSORB | 同上，成为正式临床模型调用与 Prompt ownership 依据 |
| `前端与业务迁移.md` | ABSORB | `10_前端与交付` + `12_工程与发布/迁移_回滚_下线.md` |
| `患者端证据与引用UI契约.md` | ABSORB | `10_前端与交付/Patient_Delivery与证据展示.md` |
| `原设计资产保留、改造与目标架构映射.md` | EVIDENCE_KEEP | 与本矩阵交叉引用，保留历史迁移判断 |
| `当前系统资产盘点.md` | SUPERSEDED | 新 `00_现状与治理/Current_State_Baseline_V1.md` 取代其“当前现状”角色 |
| `代码、数据与设计资产迁移矩阵.md` | REFERENCE | 后续 Legacy Asset Disposition 更新时吸收 |
| `目标能力与旧设计资产覆盖矩阵.md` | ABSORB | Capability Coverage / Refactor tracking |
| `可执行实施路线.md` | SUPERSEDED | 新 SOP 的业务闭环/开发单元路线取代旧 Phase 顺序 |
| `数据与基础设施迁移.md` | REFERENCE | `12_工程与发布`，到对应业务闭环再吸收 |
| `工程、发布、回滚与下线.md` | ABSORB | `12_工程与发布/CI_CD与Release_Gate.md`、`迁移_回滚_下线.md` |
| `企业级临床agent重构主方案.md` | REFERENCE | 母参考文档；按主题拆入需求、状态、Runtime、安全、Delivery |
| `临床数据与循证智能扩展方案.md` | ABSORB | `07_能力设计/临床证据与来源治理.md` + `08_契约与数据` |
| `agent-runtime-foundations-扩展.md` | ABSORB | `09_Runtime与技术架构/Runtime设计.md`、Context/Security 相关规范 |
| `Durable Execution 与可观测性扩展方案.md` | ABSORB | `09_Runtime与技术架构/Durable_Execution设计.md` + `11_验证与评估/Trace_Audit_Observability.md` |

> 若顶层还有未在本表列出的后续补充设计文件，处理原则相同：先判断其属于需求、状态、能力、契约、Runtime、前端、验证还是工程，再吸收到对应 current 文档；不得仅因位于 `refactoring` 就视为当前权威。

---

# 4. `docs/refactoring/evidence`

整体状态：`EVIDENCE_KEEP`。

处理规则：

- 不移动、不改写、不合并；
- 继续作为 Phase A 等历史编译、API、状态写入、模型调用、Prompt、Python 服务、数据、前端、部署等工程证据；
- Current State Baseline 可以引用它，但设计文档不得用它替代当前代码验证；
- 后续新重构阶段产生的新 evidence 建议进入新的阶段化 evidence 目录，而不是覆盖旧文件。

---

# 5. `docs/refactoring/reviews`

整体状态：`EVIDENCE_KEEP`。

处理规则：保留独立审查历史；有效治理原则抽取到 `00_现状与治理/工程治理规则.md`，原审查记录不得重写成新结论。

---

# 6. `docs/refactoring/plans`

整体状态：`REFERENCE / HISTORICAL EXECUTION PLAN`。

处理规则：

- 保留历史授权、门禁与实施事实；
- 不再决定新的 SOP 开发顺序；
- 通用治理规则（授权、独立审查、Merge Authorization、PMV、standard merge）抽入 `00_现状与治理/工程治理规则.md`；
- 原 Phase A/B/... 任务只作为历史上下文。

---

# 7. `docs/refactoring/extensions`

整体状态：`DEFER / FUTURE_EXTENSION_DESIGN`。

| 文件 | 状态 | 去向 |
|---|---|---|
| `extensions/README.md` | REFERENCE | 保留 Future Scope 导航 |
| `extensions/vNext临床Capability扩展设计.md` | DEFER | Future Capability |
| `extensions/临床检索智能扩展方案.md` | DEFER | Future G1/G6/Clinical Retrieval |
| `extensions/外部产品参考与架构抽象矩阵.md` | REFERENCE | 研究参考，不构成需求真源 |
| `extensions/外部健康数据与设备连接扩展方案.md` | DEFER | 对应 V1 后置的外部检查/设备数据闭环 |
| `extensions/结构化医疗知识与专业Tool扩展方案.md` | DEFER | Future Capability / Knowledge |

---

# 8. `docs/refactor2`

整体状态：`SUPERSEDED WORKING FOLDER`。

本目录是本轮 Current State Assessment 的工作区，不是历史 legacy 设计库。有效结论已拆入 `docs/current/`，过程稿降为 evidence。

| 文件 | 状态 | 吸收/去向 |
|---|---|---|
| `AIdoctor Current State Baseline V1 与重构决策.md` | ABSORB | 拆入 `00_现状与治理/Current_State_Baseline_V1.md`（现状与资产处置）和 `00_现状与治理/重构决策_V1.md`（迁移原则与建议路径） |
| `当前情况摸底.md` | EVIDENCE_KEEP | `00_现状与治理/evidence/当前情况摸底.md` |
| `第一轮实证盘点.md` | EVIDENCE_KEEP | `00_现状与治理/evidence/第一轮实证盘点.md` |
| `第二轮实证盘点.md` | EVIDENCE_KEEP | `00_现状与治理/evidence/第二轮实证盘点.md` |
| `第三轮实证盘点.md` | EVIDENCE_KEEP | `00_现状与治理/evidence/第三轮实证盘点.md` |
| `AIdoctor 重构需求与系统边界 V1.md` | SUPERSEDED | 已被 `01_需求/需求与系统边界_V1.md` 吸收冻结 |
| `AIdoctor V1 功能模块划分.md` | SUPERSEDED | 已被 `02_功能/功能模块划分_V1.md` 吸收冻结 |

处理规则：

- 不再把 `docs/refactor2/` 当作第二套 current；
- 过程稿保留原貌，不回写成新的冻结结论；
- Phase R1 保持“建议实施路径”，不因本目录吸收而升格为 SOP 施工令。

---

# 9. 按新 SOP 阶段的吸收队列

## Phase 1 需求与边界
优先吸收：系统定位、入口判定、临床流程中的产品约束、Must-Exclude、事实来源类型、范围边界、Patient Delivery 边界。

## Phase 2 功能模块
优先吸收：入口、病例理解、主动问诊、风险、DDx、证据缺口、结论包；横向补充 G1-G6。

## Phase 3 系统级状态主干
优先吸收：CDP、AgentState 中预算/停止/回退、Durable Execution、总架构中的 Encounter/Thread/Run/Interrupt/Resume 思想。

## Phase 4 模块级状态
优先吸收：State ownership、ToolContext/ToolResult、Context policy、State Committer、Evidence Ledger、AuditTrail。

## Phase 5 业务闭环
优先吸收：入口流程、五步临床流程、主动问诊、风险升级、回退、终点结论、异常场景。

## Phase 6 开发单元
优先吸收：工具接口规范、失败模式、suggestedWrites 思想（升级为 StatePatch）、可测试点。

## Phase 7 能力按需建设
优先吸收：Capability Package、DR.KNOWS、知识治理、RAG、Model Runtime、Prompt、Evidence Intelligence。

## Phase 8+ 验证、集成、发布
优先吸收：Static/Interactive/Trajectory Eval、AOP/Trace、Durable Execution、CI/CD、Deployment、Rollback、Decommission。

---

# 10. 当前执行结论

1. 不删除 `docs/AI医生/项目文档`；统一降级为 Legacy Design Reference。
2. 不删除 `docs/refactoring`；其 design/plans 降级，evidence/reviews 保持历史真值，extensions 明确 Future Scope。
3. 不把 `docs/refactor2/` 保留为第二权威；本轮冻结结论只进入 `docs/current/`，过程稿进入 `00_现状与治理/evidence/`。
4. 新设计只在 `docs/current/` 冻结。
5. Phase 1/2 在进入 Phase 3 前进行一次 Legacy Design Assimilation Pass。
6. 后续每个 SOP 阶段都必须先查看本矩阵指定的历史资产，再冻结该阶段新权威文档。
