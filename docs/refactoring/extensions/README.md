# AIdoctor Future Extension Design 导航

> 文档状态：`FUTURE_EXTENSION_DESIGN`  
> 适用基线：当前 `agent/enterprise-agent-refactoring-plan` 冻结架构之上的未来扩展  
> 当前 Runtime 影响：`NONE`  
> Shared Contracts v1 影响：`NONE`  
> 当前 Capability 影响：`NONE`  
> 实现授权：`NOT_AUTHORIZED`

---

## 1. 定位

本目录用于沉淀当前重构之外、但已经证明具有长期架构价值的外部产品思想、数据接入模式、专业知识工具模式和垂直临床能力模式。

本目录不是新一轮推倒重构，也不是当前实施路线的追加 Scope。

```text
当前 Frozen / Freeze Candidate 架构
负责：核心平台边界、状态所有权、安全、Runtime、RAG、Capability、Contracts

Future Extension Design
负责：在稳定平台之上预设计未来扩展点，避免未来接入设备、外部健康数据和专业数据库时重新破坏核心架构
```

所有 Future Extension 文档默认遵守：

1. 不改变当前 Constrained Agentic Workflow；
2. 不绕过 Mandatory Safety、Consent、Permission、State Committer；
3. 不允许外部 Tool、设备模型或第三方数据源直接写临床真值；
4. 不修改当前 `contracts/v1` 已冻结语义；
5. 不自动进入当前 A7-NC、A7-CL 或后续 Phase 实施范围；
6. 任何未来实现都必须经过独立 ADR、Contract Extension、Capability Allowlist、评估与发布门禁。

---

## 2. 当前 Extension 文档

### 2.1 [外部健康数据与设备连接扩展方案](./外部健康数据与设备连接扩展方案.md)

吸收 Health、freddy、Fitness AI Connector 等产品所代表的模式，定义：

- External Health Source；
- Connector Registry；
- Device Adapter；
- OAuth / Consent / Sync；
- 原始设备数据、派生结果和 ClinicalObservation 的边界；
- 未来数字脉诊、舌象分析、可穿戴设备和 EHR 的统一接入方式。

### 2.2 [结构化医疗知识与专业 Tool 扩展方案](./结构化医疗知识与专业Tool扩展方案.md)

吸收 DrugBank、Nyquist AI、Rhizome AI 等产品所代表的模式，定义：

- Medical RAG、Knowledge Graph、Structured Medical Provider 三条知识路径；
- Drug / Regulatory / Device Intelligence 的 Tool 化；
- 专业数据库结果如何进入 EvidencePack；
- 来源、版本、时效性、Citation 与 Provider Governance。

### 2.3 [vNext 临床 Capability 扩展设计](./vNext临床Capability扩展设计.md)

吸收 Pregnancy Progress 等垂直健康产品的产品结构，定义：

- 人群型、阶段型、慢病型和多模态型 Capability；
- 新场景如何扩展而不新建自由 Agent；
- `tcm_four_diagnosis_v1` 等未来候选如何与现有 Capability Package 对齐。

### 2.4 [外部产品参考与架构抽象矩阵](./外部产品参考与架构抽象矩阵.md)

维护“外部产品观察 → 不照搬内容 → 可吸收思想 → AIdoctor 抽象 → 当前/未来状态”的长期映射，避免因看到新产品而重复修改核心架构。

---

## 3. 文档优先级

Future Extension Design 的规范优先级低于当前冻结架构与当前实施文档：

```text
Architecture Freeze Baseline
→ 当前跨文档一致性补充
→ 总架构 / Capability / RAG / Model Runtime / Contracts
→ 当前 Inventory / Migration / Coverage / Roadmap
→ Future Extension Design
→ External Product Notes / Research Notes
```

当 Extension 文档与当前 Frozen Baseline 冲突时，以 Frozen Baseline 为准。

只有在满足以下条件后，某一 Extension 才能升级为正式架构：

```text
明确业务目标
+ 当前版本确有能力缺口
+ ADR 通过
+ Contract Impact 明确
+ Security / Privacy / Clinical Review 完成
+ Eval Plan 完成
+ Implementation Authorization
```

---

## 4. 设计吸收原则

研究外部产品时，不以“是否复制功能”为主要问题，而以以下问题为主：

```text
这个产品解决了什么系统性问题？
↓
AIdoctor 当前是否已有对应抽象？
├── 有：补充该抽象的设计经验和测试要求
└── 无：记录为 Future Extension Candidate
↓
是否需要新 Contract / Connector / Provider / Capability？
↓
在未来版本通过正式治理进入实现
```

禁止形成：

```text
看到一个插件
→ 新增一个服务
→ 新增一个 Agent
→ 新增一套状态
→ 新增一条绕开现有治理的调用路径
```

---

## 5. 当前推荐的长期演进方向

```text
AIdoctor Stable Platform
│
├── Clinical Intelligence
├── Safety & Policy
├── Evidence Intelligence / RAG / KG
├── Agent Runtime
├── Clinical State & Data Foundation
├── Tool / Skill / Model Governance
│
└── Future Extension Layer
    ├── External Health Connectors
    │   ├── EHR
    │   ├── Wearable
    │   ├── Pulse Device
    │   └── Tongue Imaging
    ├── Structured Medical Providers
    │   ├── Drug Intelligence
    │   ├── Regulatory Intelligence
    │   └── Device Intelligence
    └── Advanced Capability Templates
        ├── Temporal / Stage-based
        ├── Chronic Management
        ├── Special Population
        └── Multimodal Clinical Capability
```

本目录只负责提前稳定扩展边界，不授权任何上述能力进入当前 Runtime。
