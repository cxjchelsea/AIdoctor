# AIdoctor 当前权威文档入口

> 状态：CURRENT AUTHORITY ROOT
> 适用分支基线：main
> 用途：后续重构设计、状态建模、业务闭环、能力建设与工程治理的唯一当前文档入口。

## 1. 文档权威规则

证据与设计优先级：

1. 当前 `main` 代码、配置、测试、CI、运行证据；
2. `docs/current/` 中已冻结的当前权威文档；
3. `docs/current/00_现状与治理/evidence/` 本轮 Current State Assessment 过程实证稿；
4. `docs/refactoring/evidence/`、`reviews/` 等历史工程证据；
5. `docs/refactoring/` 旧目标设计与迁移方案；
6. `docs/AI医生/项目文档/` 历史业务与技术设计；
7. 推断。

历史文档中的“已完成”“当前架构”等描述不得覆盖当前代码与 `docs/current/` 的冻结结论。

## 2. 当前 SOP 文档链

```text
00 现状与治理
→ 01 需求与边界
→ 02 功能模块
→ 03 系统级状态主干
→ 04 模块级状态
→ 05 业务闭环
→ 06 开发单元
→ 07 能力设计
→ 08 契约与数据
→ 09 Runtime 与技术架构
→ 10 前端与交付
→ 11 异常、安全、验证与评估
→ 12 工程、发布、迁移与下线
```

当前进度：

- Current State Baseline：V1 已冻结，见 `00_现状与治理/Current_State_Baseline_V1.md`；
- 重构决策：V1 已冻结为迁移原则与建议路径，见 `00_现状与治理/重构决策_V1.md`；
- 需求与系统边界：V1 已冻结；
- 功能模块划分：V1 已冻结；
- 系统级状态主干：V1 已冻结，见 `03_状态/系统级状态主干_V1.md`；
- 模块级状态与状态所有权：V1 已冻结，见 `03_状态/模块级状态与状态所有权_V1.md`；
- 业务闭环：V1 已冻结，见 `05_业务闭环/业务闭环设计_V1.md`；
- 可验证开发单元：V1 已冻结，见 `06_开发单元/可验证开发单元拆分_V1.md`；
- 能力设计：V1 草案已完成，见 `07_能力设计/按开发单元的Capability设计_V1.md`，当前等待冻结前独立审查；
- 契约与数据：尚未开始。Phase 7 冻结后才进入 Phase 8。

## 3. 目录定位

### `docs/AI医生/项目文档/`

定位：`LEGACY DESIGN REFERENCE`

主要保存旧系统的业务、临床、Agent、Tool、CDP、知识、前端、异常和评估设计。有效思想应按 SOP 阶段逐步吸收，不再作为当前系统设计真源。

### `docs/refactoring/`

定位：`SUPERSEDED / MIGRATION REFERENCE + ENGINEERING EVIDENCE`

其中：

- `evidence/`：历史工程证据，保留原貌；
- `reviews/`：历史审查证据，保留原貌；
- `plans/`：历史实施计划，保留但不再决定新 SOP 开发顺序；
- `extensions/`：未来扩展设计，默认 `NOT CURRENT V1 AUTHORITY`；
- 其他顶层方案：作为目标架构、迁移与治理参考，逐步拆入 `docs/current/`。

### `docs/current/00_现状与治理/evidence/`

定位：`EVIDENCE_KEEP`

保存本轮 Current State Assessment 的摸底与三轮实证盘点。它们是 Baseline 的推导过程，不得覆盖已冻结的现状结论或重构决策。

### `docs/refactor2/`

定位：`SUPERSEDED WORKING FOLDER`

本轮重构工作区。有效结论已拆入 `docs/current/00_现状与治理/`，过程稿已降为上述 evidence。该目录不再作为当前权威。

## 4. 迁移规则

任何旧文档只允许以下处理：

- `ABSORB`：有效内容吸收到新的当前权威文件；
- `REFERENCE`：保留为专题参考，不成为当前真源；
- `DEFER`：属于未来阶段，当前不吸收；
- `SUPERSEDED`：已被新设计替代，仅保留历史；
- `EVIDENCE_KEEP`：工程/审查证据原样保留。

不得为了“整理干净”删除尚未完成吸收的历史设计资产。

完整迁移映射见：`docs/current/00_现状与治理/Legacy_Document_Assimilation_Matrix_V1.md`。