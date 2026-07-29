# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.1  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

## 1. 阅读顺序

AIdoctor 的企业级重构方案由以下两份文档共同组成：

1. [企业级临床 Agent 重构主方案](./enterprise-agent-refactoring-plan.md)
   - 项目定位与边界；
   - 临床智能、LangGraph Runtime、安全双循环；
   - Evidence Ledger、工具治理、医生接管；
   - Care Navigation、Follow-up、评估与分阶段放量；
   - Phase 0～6 迁移路线。

2. [临床数据与循证智能扩展方案](./clinical-data-and-evidence-intelligence-extension.md)
   - 纵向患者记录与 Encounter CDP 分离；
   - 临床表型、数据来源和研究数据集治理；
   - Evidence Intelligence Service；
   - PICO、来源分级、引用校验和证据冲突处理；
   - 医生 Evidence Copilot；
   - 对原 Phase 1、2、4、5、6 的增量任务与验收标准。

## 2. v2.1 的架构定义

v2.1 不改变 v2.0 的核心原则：

- LangGraph 只管理执行过程，不承担临床真值判断；
- 临床推理、分诊安全、语言表达和高风险动作决策权分离；
- Safety Loop 不可被 Planner 绕过；
- 高风险和受监管动作由医生最终决定；
- 固定 Workflow 保留为可测试的降级路径；
- 首个 Capability 必须收窄并经过分阶段验证。

v2.1 在此基础上新增两项正式平台能力：

### 2.1 Clinical Data & Research Foundation

受 UK Biobank 等纵向生物医学数据平台启发，负责：

- 长期患者画像与单次问诊状态分离；
- 多来源临床事实、推断和确认状态管理；
- 临床表型定义及版本管理；
- 数据集、队列、标签和研究用途治理；
- 生产数据与研究工作区隔离；
- 时间外、机构外和人群子组验证。

### 2.2 Evidence Intelligence Service

受 OpenEvidence 等循证临床决策支持工具启发，负责：

- 临床问题分类和 PICO 结构化；
- 权威指南、综述和研究检索；
- 来源等级、人群匹配和时效性评估；
- 结论级引用绑定与引用支持性验证；
- 指南和研究结论冲突识别；
- 为医生提供 Evidence Copilot，而不是替医生执行高风险决策。

## 3. 完整目标架构

```text
Patient UI / Clinician Console
            │
Business & Care Delivery
用户、权限、预约、转诊、审核、随访
            │
LangGraph Agent Runtime
状态、Checkpoint、Interrupt、工具治理、降级
       ┌────┴──────────────┐
       │                   │
Clinical Intelligence     Evidence Intelligence
问诊策略、诊断推理、       PICO、指南/论文检索、
分诊安全、Care Path        来源分级、引用与冲突校验
       └────┬──────────────┘
            │
Clinical Data & Research Foundation
Longitudinal Record、Encounter CDP、Evidence Ledger、
Phenotype Registry、Dataset Registry、Research Workspace
```

## 4. 实施优先级

v2.1 不意味着立即建设完整医学数据平台或全量文献搜索引擎。实施顺序仍然是：

1. 修复当前编译、接口和固定 Workflow 主链路；
2. 建立 ClinicalObservation、Encounter CDP 和 Evidence Ledger；
3. 建立首个窄 Capability 的 Clinical Intelligence MVP；
4. 建立只覆盖白名单指南的 Evidence Intelligence MVP；
5. 接入 LangGraph Runtime；
6. 完成医生审核、三类输出、Care Navigation 与随访；
7. 建立脱敏研究数据集、Patient Simulator 和分阶段验证。

## 5. 当前明确不做

首个版本不建设：

- 通用医学自主诊断；
- 全量医学期刊搜索引擎；
- 未授权医学全文抓取；
- 基因诊断和复杂多组学推理；
- 面向所有人群和全部疾病的统一模型；
- 直接用研究队列风险模型进行患者实时分诊；
- 没有医生审核的治疗和处方修改；
- 以增加 Agent 数量替代临床推理和评估。

## 6. 当前推荐起点

仍建议以以下 Capability 为首个落地范围：

> 成人常见呼吸道症状的风险分层、结构化信息采集、循证依据展示和就医导航。

首个版本的 Evidence Intelligence 只支持该 Capability 相关的白名单指南和公开权威来源，优先服务医生审核端，不直接根据单篇研究向患者生成治疗结论。
