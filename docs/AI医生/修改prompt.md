帮我修改这个文件@docs/AI医生/1.项目结构设计/AI医生系统-技术架构设计.md ，目标是把当前“多智能体(8 agents + orchestrator)”架构，重构为“单主Agent + 多工具Tools + CDP + 审计”的临床可用架构。请在不改变原始业务目标（临床诊断流程Step1-5、CDP核心、模块能力范围）的前提下，完成以下文档级改造：



A. 架构定位改写

1) 用“Clinical Agent Brain（主Agent大脑）”替代 Orchestrator 的定位：主Agent具备自主决策、自主调用工具、停止/升级/拒答能力，是唯一“最终结论提交者”。
2) 将原八大智能体全部降级为 Tools：工具无独立目标、无长期策略状态，只按主Agent调用执行并返回结构化结果与证据引用。
3) 保留并行/协商/冲突处理思想，但协商改为主Agent内部的 evidence fusion / conflict resolution，而非自治agent协商。



B. 新增并补全关键章节（必须生成完整章节内容）

1) 《核心对象与责任边界：CDP / AgentState / Tools / AuditTrail》

   \- CDP=病例事实唯一事实源

   \- AgentState=主Agent策略状态（阈值、预算、失败回退、已尝试工具等）

   \- Tools=能力模块（输入依赖、结构化输出、证据引用、建议写回字段）

   \- AuditTrail=必须记录工具调用、证据、写回字段、版本、时间

2) 《Tool协议与I/O契约》

   \- 定义 ToolContext 字段：traceId、cdp引用、agentState摘要、约束（成本/时间/风险）

   \- 定义 ToolResult 字段：status、payload、evidence、quality、suggestedWrites、errors

   \- 工具分类：Deterministic / Retrieval / Generative，并写清楚哪些结论字段不可由 Generative 工具直接产生

3) 《主Agent运行循环与默认诊断路径》

   \- 描述 Observe→Plan→Act→Update→Evaluate→Stop/Escalate 循环

   \- 将Step1-5写为“默认策略路径”，并补充动态插入：红旗优先、冲突复核、证据不足触发检索

4) 《停止条件、升级与拒答策略》

   \- StopCondition：CDP必填项、证据引用齐全、风险评估完成

   \- Escalation：急危重/超能力/证据不足且风险高

   \- Refusal：无法安全推断的拒答边界与提示



C. 重写原“八大智能体”章节为《工具清单（Tools Catalog）》

对每个工具条目按以下模板写：

\- Tool名称/职责

\- 输入依赖（从CDP读取哪些字段路径）

\- 输出payload结构（字段列表）

\- evidence要求（必须包含哪些证据引用）

\- suggestedWrites（建议写回CDP的字段路径）

\- 质量/失败模式与降级策略

\- 可测试点（单测/回归建议）



D. 修订消息路由/Redis队列定位

将消息系统的职责改为“tool调用调度层（并行、异步、超时、重试、限流）”，明确消息层不拥有决策提交权，主Agent仍是唯一提交者。



E. 文档风格要求

\- 用中文撰写

\- 输出必须是可研发落地的架构文档（字段级、流程级、职责边界明确）

\- 不写代码，但字段结构与路径命名要明确（例如 cdp.problem_representation, cdp.ddx.rank_list, final.summary 等）

\- 保留原文中有价值的内容（CDP理念、临床流程、模块职责），但整体叙述要改为单主Agent+Tools的范式



请直接在原文件基础上进行“全文重构式修改”，输出完整修改后的 markdown 文件内容。