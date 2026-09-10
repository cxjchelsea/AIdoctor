# 当前状态说明

> STATUS: SUPERSEDED / MIGRATION REFERENCE + ENGINEERING EVIDENCE

`docs/refactoring/` 保存上一轮 Enterprise 重构的目标设计、迁移方案、计划、评审与工程证据。

自新的状态驱动 SOP 与 `docs/current/` 建立后，本目录不再整体作为当前设计权威。

## 分类

- `evidence/`：`EVIDENCE_KEEP`，历史工程证据，原样保留；
- `reviews/`：`EVIDENCE_KEEP`，历史独立审查记录，原样保留；
- `plans/`：`HISTORICAL EXECUTION PLAN`，保留历史授权和实施事实，但不再决定新 SOP 顺序；
- `extensions/`：`FUTURE_EXTENSION_DESIGN`，默认不属于当前 V1；
- 顶层总体/专题设计：`MIGRATION REFERENCE`，有效内容按 SOP 阶段拆入 `docs/current/`；
- 旧 `当前系统资产盘点`、旧实施路线等：其“当前”角色已被新的 Current State Baseline / SOP 取代。

上一轮 refactoring 中仍有效的状态、安全、Capability、Model Runtime、Evidence、Durable Execution、审计、评估和工程治理思想不会丢弃，而是通过迁移矩阵有选择地继承。

逐文件处置见：
`docs/current/00_现状与治理/Legacy_Document_Assimilation_Matrix_V1.md`。

当前权威入口：`docs/current/README.md`。
