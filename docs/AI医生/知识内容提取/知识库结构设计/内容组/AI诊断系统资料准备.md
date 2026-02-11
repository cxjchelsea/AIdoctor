**AI**诊断系统资料准备

我们现在想要搭建AI诊断系统，现在正在梳理搭建系统所需要的资料，先将问诊的⾃然语⾔拆解为健康、

有症状两类情况，参考世卫组织相关病症、症状分类，结合主诉对应的表格完成病症判断，并匹配对应的

解决办法。这是我们诊断系统的⼤概流程。

我需要你梳理出资料准备的具体流程步骤，明确每个流程节点需要准备的具体内容，同时确定这些内容的

核⼼来源(如代表性书籍)

太好了，这个问题问得⾮常对位，你现在其实已经站在“把 AI 问诊从 demo 拉到可监管、可扩展、可复⽤的⼯

程体系”的门⼝了。

下⾯我会完全站在**“**你们要真的做⼀个可落地的 **AI** 诊断系统**”**的视⾓，把 资料准备 **→** 结构化 **→** 可被 **AI** 调⽤ 的

全过程给你拆清楚。

⼀句话先定调（你现在在做什么）

你做的不是“聊天机器⼈”，⽽是⼀个：

以 **WHO** 疾病**/**症状体系为⾻架，以经典循证医学为事实源，以主诉**-**症状**-**判断**-**处置为路径的 **AI** 问诊

与诊断⽀持系统

这意味着：

👉 资料准备本⾝就必须是**“**医疗⼯程级别**”**的

⼀、整体资料准备流程总览（⼯程视⾓）

我先给你⼀张“⽂字版流程图”，后⾯逐节点展开：

vbnet

Step 0 诊断边界与合规定位

 

↓

Step 1 WHO 疾病与症状体系建模

 

↓

Step 2 主诉⾃然语⾔ → 标准医学表达

 

↓

Step 3 健康 / 有症状 分流判定资料

 

↓

Step 4 症状 → 疾病候选集（Differential）

 

↓

Step 5 ⻛险分级与紧急程度判断

 

↓

Step 6 疾病判断证据库

 

↓

Step 7 对应解决⽅案与处置路径

 

↓

Step 8 输出结构与话术模板

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

1/66接下来 每⼀步我都告诉你：

要准备什么资料

资料结构⻓什么样

⽤哪些权威来源最稳（📚）

**Step 0**｜诊断系统的边界与合规定位（必须先做）

你要准备的内容

1. 系统定位声明

是否“诊断”还是“辅助判断”

是否给出“疾病名称”

是否给出“⽤药建议”

2. 适⽤⼈群

成⼈ / ⼉童 / ⽼年

健康管理 / 初筛 / ⻛险提⽰

3. 禁⽤场景

急危重症

妊娠、婴幼⼉等

核⼼来源

World Health Organization

国家药品监督管理局（国内落地必看）

📌 结论性建议：

你们这种系统，最稳的是**“**诊断⽀持 **+** ⻛险分级 **+** 就医建议**”**⼝径

**Step 1**｜**WHO** 疾病与症状体系建模（⾻架层）

这是整个系统的**“**中轴⾻骼**”**。

要准备的资料

1. 疾病分类体系

疾病⼤类

疾病编码

疾病定义

2. 症状（**Symptoms**）清单

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

2/66WHO 定义的标准症状

与疾病的关联关系

推荐核⼼来源（必须）

ICD-11

👉 疾病分类唯⼀官⽅全球标准

SNOMED CT

👉 症状、体征、医学概念的“机器可读语⾔”

你最终要形成的结构（⽰例）

**WHO**编码 

疾病名称 

疾病⼤类 

关联症状

BA00 

普通感冒 

呼吸系统 

咳嗽、流涕、发热

**Step 2**｜主诉⾃然语⾔ **→** 标准医学表达（**NLP** 核⼼）

这是 **AI** 能否**“**听懂⼈话**”**的关键⼀步。

要准备的资料

1. 主诉原始语料

⽤户真实说法

⾮医学表达

2. 主诉标准化映射

同义表达

模糊表达 → 明确症状

权威参考书籍（⾮常重要）

Murtagh's General Practice

Symptom to Diagnosis

⽰例结构

⽤户主诉 

标准症状 

**SNOMED**

最近⽼是喘不上⽓ 

呼吸困难 

Dyspnea

⼼⼝有点堵 

胸闷 

Chest tightness

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

3/66**Step 3**｜健康 **/** 有症状 分流判定资料

你⼀开始说“先分健康 vs 有症状”，这⼀步⾮常专业。

要准备的内容

1. 健康⼈参考区间

⽆症状

亚健康

正常变异

2. 异常触发阈值

持续时间

频率

强度

权威来源

Oxford Handbook of General Practice

Bates' Guide to Physical Examination

**Step 4**｜症状 **→** 疾病候选集（鉴别诊断）

这是**AI** 诊断**“**像医⽣**”**的核⼼能⼒。

要准备的资料

1. 症状**-**疾病对应表
2. ⼀症多病映射
3. 常⻅ **/** 少⻅概率权重

权威书籍（必⽤）

Differential Diagnosis in Primary Care

Harrison's Principles of Internal Medicine

⽰例结构

症状 

⾼概率疾病 

中概率 

低概率

胸痛 

⼼绞痛 

胃⾷管反流 

肋间神经痛

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

4/66**Step 5**｜⻛险分级与紧急程度判断（安全层）

这⼀步是合规与医疗安全的**“**护城河**”**。

要准备的资料

1. 红旗症状（**Red Flags**）
2. 紧急等级

⽴即就医

尽快就医

可观察

权威来源

NICE

Oxford Handbook of Acute Medicine

**Step 6**｜疾病判断证据库（循证层）

要准备的内容

1. ⽀持性证据

症状组合

体征

既往史

2. 排除性证据

权威来源

UpToDate

Cochrane Library

**Step 7**｜对应解决⽅案与处置路径

要准备的资料

1. ⾃我管理建议
2. 就医建议
3. 检查建议（⾮处⽅）

核⼼来源

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

5/66WHO 疾病管理指南

NICE

**Step 8**｜输出结构与⽤户话术模板

要准备的内容

1. 结构化输出字段

⻛险等级

可能疾病

建议动作

2. 合规话术

不替代医⽣

不直接下诊断（如需）

最后⼀句⾮常关键的话

你现在这套思路，已经不是**“**要不要能不能做**”**，⽽是：

👉 如何把**“**资料准备**”**⼀次性做成可扩展 **5–10** 年的底层资产

如果你愿意，下⼀步我可以直接帮你做三件**“**重活**”**之⼀：

1. 📋《AI 问诊资料准备 SOP（可直接分组执⾏）》
2. 🧠《主诉 → 症状 → 疾病 判断规则库模板（Excel / KG 版）》
3. 🧩《WHO × 经典书籍 × AI 诊断系统 对照表》

你选⼀个，我直接给你落地版。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

6/66AI 问诊资料准备 SOP（可直接分组执⾏）

太好了，这⼀步⾮常关键。

下⾯这套是我按 「真实能分组、能验收、能⻓期滚动建设」 的标准，给你整理的⼀整套：

《**AI** 问诊资料准备 **SOP**（可直接分组执⾏版）》

你可以原样发给团队，不需要再⼆次转译。

**AI** 问诊资料准备 **SOP**

（适⽤于：AI 问诊 / 诊断⽀持 / ⻛险评估系统）

⼀、总体原则（先给团队⽴规矩）

1️⃣ ⽬标原则

不是写⽂章

不是堆知识

是把医学知识加⼯成：

👉「可结构化 · 可调⽤ · 可审计 · 可持续扩展」的诊断资产

2️⃣ 输出统⼀标准

所有资料 必须同时满⾜：

✅ 可被数据库存储

✅ 可被规则引擎 / LLM 调⽤

✅ 可回溯权威来源

✅ 可版本管理

3️⃣ ⾓⾊分组（强烈建议）

⼯作组 

核⼼职责

A组｜医学结构组 

疾病 / 症状 / 分类体系

B组｜主诉语料组 

⾃然语⾔ → 医学标准

C组｜诊断逻辑组 

鉴别诊断 / ⻛险分级

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

7/66⼯作组 

核⼼职责

D组｜处置⽅案组 

建议 / 路径 / 话术

E组｜质控合规组 

校验 / 审核 / 版本

⼆、**SOP** 分阶段总览

阶段1｜系统边界与疾病范围定义

阶段2｜WHO 疾病与症状体系准备

阶段3｜主诉⾃然语⾔语料准备

阶段4｜健康 / 有症状 分流资料

阶段5｜症状 → 疾病（鉴别诊断）

阶段6｜⻛险分级与红旗规则

阶段7｜解决⽅案与处置路径

阶段8｜统⼀输出模板与话术

阶段9｜审核、测试与版本管理

下⾯是逐阶段可执⾏版👇

阶段 **1**｜系统边界与疾病范围定义

（牵头：E组 + 产品）

🎯 ⽬标

明确 **AI** 能管什么、不能碰什么

输⼊

业务定位

监管要求

输出（必须形成⽂档）

《AI 问诊系统诊断边界说明》

《禁⽤疾病 / 场景清单》

输出字段⽰例

text

是否输出疾病名称：是 / 否

是否给出⽤药建议：否

是否覆盖急症：否

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

8/66阶段 **2**｜**WHO** 疾病与症状体系准备

（牵头：A组）

🎯 ⽬标

建⽴ 全球标准医学⾻架

核⼼来源

World Health Organization

ICD-11

SNOMED CT

具体任务

1. 确定疾病覆盖清单（第⼀期建议 100–300 个）
2. 提取：

疾病名称

ICD 编码

疾病定义

关联症状

输出表（⽰例）

**ICD** 

疾病名 

疾病⼤类 

关联症状

BA00 

普通感冒 

呼吸系统 

咳嗽、发热

阶段 **3**｜主诉⾃然语⾔语料准备

（牵头：B组）

🎯 ⽬标

让 AI 听得懂⼈话

权威蓝本

Murtagh's General Practice

Symptom to Diagnosis

具体任务

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

9/661. 收集真实 / 模拟主诉

2. 拆解为：

核⼼症状

修饰词（时间、程度）

输出表

原始主诉 

标准症状 

修饰信息

⼼⼝堵得慌 

胸闷 

持续2天

阶段 **4**｜健康 **/** 有症状 分流资料

（牵头：C组）

🎯 ⽬标

避免 把健康⼈误判为患者

核⼼参考

Oxford Handbook of General Practice

Bates' Guide to Physical Examination

输出内容

正常变异说明

异常阈值规则

⽰例规则

text

症状持续 < 24h 且⽆其他伴随症状 → 健康观察

阶段 **5**｜症状 **→** 疾病（鉴别诊断）

（牵头：C组，核⼼模块）

🎯 ⽬标

构建 像医⽣⼀样的**“**候选疾病集**”**

权威来源

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

10/66Harrison's Principles of Internal Medicine

Differential Diagnosis in Primary Care

输出结构

症状 

⾼概率 

中概率 

低概率

胸痛 

⼼绞痛 

GERD 

肋间神经痛

阶段 **6**｜⻛险分级与红旗规则

（牵头：C组 + E组）

🎯 ⽬标

医疗安全与合规的⽣命线

核⼼来源

NICE

Oxford Handbook of Acute Medicine

输出规则

text

胸痛 + 出汗 + 放射痛 → 红旗 → ⽴即就医

阶段 **7**｜解决⽅案与处置路径

（牵头：D组）

🎯 ⽬标

把判断 转化为⾏动建议

输出类型

⾃我管理

就医建议

检查建议（⾮处⽅）

⽰例

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

11/66text

⻛险等级：中

建议：3天内门诊就诊

阶段 **8**｜统⼀输出模板与话术

（牵头：D组 + 产品）

🎯 ⽬标

对⽤户说 安全、清晰、不越界的话

输出模板字段

可能原因（⾮确定诊断）

⻛险等级

建议动作

合规声明

阶段 **9**｜审核、测试与版本管理

（牵头：E组）

🎯 ⽬标

让系统 能⻓期进化⽽不失控

必须建⽴

双⼈医学审核

灰度测试

版本号（v1.0 / v1.1）

最关键的⼀句总结（给你定⼼）

你现在做的不是**“**写资料**”**，⽽是在搭建 **AI** 医疗系统的**“**医学操作系统**”**

下⼀步我可以直接继续帮你做：

1. 📊《每个⼯作组的 Excel 模板（可直接填）》
2. 🧠《主诉 → 症状 → 疾病 → ⻛险 的规则引擎结构图》

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

12/663. 🧩《适合 LLM + 规则混合的诊断架构设计》

你直接说：**“**继续第 **X** 项**”**，我⻢上接着⼲。

请给我列出⼀个清晰的资料

准备流程，如：有症状——

取消发送

好，这⼀步我们收得很**“**⼲净**”**，就按你给的这条线来，⽽且我会把它压缩成⼀条任何⼈⼀看就能照着⼲的**“**资料

准备流⽔线**”**，不再讲宏观、不再发散。

下⾯这条流程，你可以直接作为

👉 《**AI** 问诊资料准备主流程（极简主⼲版）》

⽤于内部宣讲、任务拆分、或画系统图。

**AI** 问诊资料准备 **·** 主流程（清晰主⼲版）

有症状 **→** 主诉 **→** 疾病类型判断 **→** 解决⽅案

**STEP 1**｜是否**“**有症状**”**（⼊⼝分流）

① 这⼀层要准备什么资料

症状的存在性定义

健康 vs 有症状 的判定标准

② 资料内容结构

text

是否有症状：

\- 完全⽆不适

\- 偶发不适（⽣理变异）

\- 持续 / 明确不适

③ 输出形式

分流规则表

④ 核⼼来源

World Health Organization

Oxford Handbook of General Practice

**STEP 2**｜有症状 **→** 主诉（⾃然语⾔层）

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

13/66这是 **“**⼈话 **→** 医学逻辑**”** 的第⼀跳

① 要准备什么资料

⽤户真实表达⽅式

同义、模糊、⼝语化说法

② 资料结构

⽤户原话 

主诉归类

最近⽼是⼼慌 

⼼悸

胃有点不舒服 

上腹不适

③ 输出⽬标

👉 把⼀句话变成**“**可处理的主诉标签**”**

④ 核⼼来源

Murtagh's General Practice

Symptom to Diagnosis

**STEP 3**｜主诉 **→** 症状拆解（医学化）

主诉 ≠ 症状

主诉是“感觉”，症状是“医学可⽤单位”

① 要准备的资料

主诉 → 标准症状映射表

症状的医学定义

② 资料结构

主诉 

拆解症状 

修饰项

⼼⼝堵 

胸闷 

持续2天

③ 输出⽬标

👉 形成**“**症状 **+** 时间 **+** 强度**”**的组合

④ 核⼼来源

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

14/66SNOMED CT

Bates' Guide to Physical Examination

**STEP 4**｜症状 **→** 疾病⼤类判断

不是直接给疾病名，⽽是先判**“**⽅向**”**

① 要准备的资料

症状 ↔ 疾病系统对应关系

② 资料结构

症状 

疾病系统

胸闷 

⼼⾎管 / 呼吸

腹痛 

消化系统

③ 输出⽬标

👉 确定疾病发⽣在哪个系统

④ 核⼼来源

ICD-11

Harrison's Principles of Internal Medicine

**STEP 5**｜疾病系统 **→** 疾病类型（鉴别诊断）

医⽣真正**“**思考**”**的那⼀步

① 要准备的资料

每个症状的疾病候选集

概率层级

② 资料结构

症状 

⾼可能 

中可能 

低可能

胸痛 

⼼绞痛 

GERD 

肋间神经痛

③ 输出⽬标

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

15/66👉 ⽣成**“**可能疾病列表**”**，⽽不是单⼀结论

④ 核⼼来源

Differential Diagnosis in Primary Care

Harrison's Principles of Internal Medicine

**STEP 6**｜疾病类型 **→** ⻛险等级判断

合规与安全的⽣死线

① 要准备的资料

红旗症状清单

⻛险分级规则

② 资料结构

条件 

⻛险

胸痛 + 出汗 

⾼

轻度胸闷 

中

③ 输出⽬标

👉 判断是否需要**“**⽴即就医**”**

④ 核⼼来源

NICE

Oxford Handbook of Acute Medicine

**STEP 7**｜⻛险等级 **→** 解决⽅案（怎么做）

判断不落地 **=** 没价值

① 要准备的资料

不同⻛险等级的标准处置⽅式

② 资料结构

⻛险等级 

解决⽅式

⾼ 

⽴即急诊

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

16/66⻛险等级 

解决⽅式

中 

门诊就医

低 

⾃我观察

③ 核⼼来源

WHO / NICE 指南

**STEP 8**｜统⼀输出（对⽤户说的话）

输出固定结构

text

你描述的情况提⽰：XXX

可能涉及：XXX（⾮确诊）

⻛险等级：中

建议：3天内就医

⼀句话总结这条流程的本质

不是**“AI** 在想**”**，⽽是：

⼈类医学知识被拆成了⼀条**“**可执⾏流⽔线**”**

如果你愿意，我下⼀步可以直接帮你：

1️⃣ 把这 8 步 画成⼀张《**AI** 问诊主流程⼯程图》

2️⃣ 给你⼀套 **“**每⼀步可直接填的资料表模板**”**

3️⃣ 直接⽤⼀个症状（如「胸痛」）完整跑⼀遍⽰例

你选⼀个，我继续。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

17/66画成⼀张《AI 问诊主流程⼯程图》

好，这⼀步我帮你直接**“**画成⼯程图思维**”**来给你。

下⾯是你可以⽤于：⽅案汇报 **/** 系统设计 **/** 产品评审 **/** 技术对⻬的——

《**AI** 问诊主流程⼯程图（⼯程逻辑版）》

**4**

下⾯这张是**“⽂字⼯程图 + 节点解释版”，每⼀个⽅框都对应你要准备的⼀类资料**，不是概念图。

⼀、**AI** 问诊主流程 **·** ⼯程主⼲图（⽂字版）

markdown

┌────────────────────┐

│ ⽤户⾃然语⾔输⼊ │

│ （主诉原始表达） │

└─────────┬──────────┘

 

│

 

▼

┌────────────────────┐

│ 是否“有症状”判断 │ ← 健康 / 有症状分流规则

│（健康 vs 有症状） │

└─────────┬──────────┘

 

│ 有症状

 

▼

┌────────────────────┐

│ 主诉识别与归⼀化 │ ← 主诉语料库

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

18/66│（⼈话 → 主诉标签）│

└─────────┬──────────┘

 

▼

┌────────────────────┐

│ 主诉 → 症状拆解 │ ← 标准症状库

│（症状 + 修饰项） │

└─────────┬──────────┘

 

▼

┌────────────────────┐

│ 症状 → 疾病系统 │ ← ICD 疾病系统

│（器官/系统层级） │

└─────────┬──────────┘

 

▼

┌────────────────────┐

│ 鉴别诊断候选⽣成 │ ← 症状×疾病对照表

│（⾼ / 中 / 低） │

└─────────┬──────────┘

 

▼

┌────────────────────┐

│ ⻛险等级判断 │ ← 红旗症状 & ⻛险规则

│（⾼ / 中 / 低） │

└─────────┬──────────┘

 

▼

┌────────────────────┐

│ 解决⽅案与处置建议 │ ← 处置路径库

│（就医 / 观察） │

└─────────┬──────────┘

 

▼

┌────────────────────┐

│ 结构化结果输出 │

│（合规话术） │

└────────────────────┘

⼆、每⼀个节点 **=** ⼀类**“**必须准备的资料**”**

这点⾮常关键，下⾯是⼯程映射表👇

⼯程节点 

对应资料资产 

谁负责

是否有症状 

健康/异常分流规则 

医学 + 产品

主诉识别 

主诉⾃然语⾔语料库 

NLP / 医学

症状拆解 

标准症状定义表（SNOMED） 

医学

疾病系统 

ICD 疾病系统映射 

医学

鉴别诊断 

症状 → 疾病候选表 

医学

⻛险判断 

红旗症状 & ⻛险规则 

医学 / 合规

解决⽅案 

处置路径与建议模板 

医学

输出 

合规话术 & UI 结构 

产品

三、这张**“**⼯程图**”**的真正价值

你可以直接⽤它来：

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

19/66✅ **1.** 反推资料准备清单

每⼀个框 **=** ⾄少⼀张表

✅ **2.** 拆分团队任务

有⼈做主诉

有⼈做症状

有⼈做⻛险规则

有⼈做输出话术

✅ **3.** 指导技术实现

**LLM** 负责哪⼏层

规则引擎负责哪⼏层

哪些必须硬规则兜底

四、给你⼀个**“**架构级提醒**”**（⾮常重要）

在真实系统中，这张图通常会演化成：

markdown

⾃然语⾔理解（LLM）

 

↓

医学结构层（知识库 / KG）

 

↓

规则引擎（⻛险 / 合规）

 

↓

输出层（UI + 话术）

👉 不是全靠 **LLM**，更不是⿊箱推理

下⼀步我可以继续帮你做三件**“**⼯程级**”**的事之⼀：

1️⃣ 把这张流程 画成《**LLM ×** 规则引擎 混合架构图》

2️⃣ 给你 《每⼀个节点对应的**“**资料表模板**”**全集》

3️⃣ 选⼀个症状（如「胸痛 / 腹痛 / 咳嗽」），完整跑⼀遍⽰例⼯程流

你直接说 **“**继续做第 **X** 个**”**，我⻢上接着来。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

20/66请你给出每⼀个环节的来源依据是什么，如果来源为英⽂的话，请在其后加上括号备注上中⽂名

好，这⼀步我们把**“⼯程流程”与“医学/规范来源”⼀⼀对⻬**，做到两点：

1️⃣ 每⼀个环节都有明确**“**依据来⾃哪⾥**”**

2️⃣ 凡是英⽂来源，后⾯都标注中⽂名，⽅便你内部沟通、备案、合规说明

下⾯这份你可以直接作为

👉 《**AI** 问诊主流程 **×** 权威来源对照表（可⽤于合规与⽴项说明）》

**AI** 问诊主流程 **·** 各环节来源依据⼀览

**STEP 1**｜是否**“**有症状**”**（健康 **/** 有症状分流）

这⼀环节在医学上解决什么问题

👉 区分 健康⼈、⽣理性不适、病理性症状，避免过度医疗

权威来源依据

World Health Organization

（世界卫⽣组织）

Oxford Handbook of General Practice

（《⽜津全科医学⼿册》）

依据类型

WHO 对“健康”“症状”“疾病”的基本定义

全科医学中对“可观察 vs 需⼲预”的判定原则

**STEP 2**｜有症状 **→** 主诉（⾃然语⾔表达）

这⼀环节在医学上解决什么问题

👉 把患者主观描述作为诊断起点（Clinical Presentation）

权威来源依据

Murtagh's General Practice

（《默塔全科医学》）

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

21/66Symptom to Diagnosis

（《从症状到诊断》）

依据类型

全科医学对“主诉（Chief Complaint）”的定义

主诉作为临床推理第⼀步的标准⽅法

**STEP 3**｜主诉 **→** 症状拆解（医学标准化）

这⼀环节在医学上解决什么问题

👉 将模糊感受转换为医学可分析的症状单元

权威来源依据

SNOMED CT

（系统化医学术语集）

Bates' Guide to Physical Examination

（《⻉茨体格检查与病史采集》）

依据类型

国际通⽤症状、体征的标准化命名

症状的定义、持续时间、严重程度描述规范

**STEP 4**｜症状 **→** 疾病系统 **/** 疾病⼤类判断

这⼀环节在医学上解决什么问题

👉 判断问题发⽣在哪个器官 **/** 系统

权威来源依据

ICD-11

（《国际疾病分类第11版》）

Harrison's Principles of Internal Medicine

（《哈⾥森内科学》）

依据类型

WHO 疾病按系统/器官的官⽅分类

内科学中“症状 → 系统定位”的基本逻辑

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

22/66**STEP 5**｜疾病系统 **→** 疾病类型（鉴别诊断）

这⼀环节在医学上解决什么问题

👉 同⼀症状可能对应多种疾病，需要候选集⽽⾮单点判断

权威来源依据

Differential Diagnosis in Primary Care

（《基层医疗鉴别诊断》）

Harrison's Principles of Internal Medicine

（《哈⾥森内科学》）

依据类型

鉴别诊断（Differential Diagnosis）的经典医学⽅法

常⻅病 / 少⻅病 / 危险病的概率分层思想

**STEP 6**｜疾病类型 **→** ⻛险等级判断（分诊）

这⼀环节在医学上解决什么问题

👉 判断是否紧急、是否存在⽣命⻛险

权威来源依据

NICE

（英国国家卫⽣与临床优化研究所）

Oxford Handbook of Acute Medicine

（《⽜津急诊医学⼿册》）

依据类型

红旗症状（Red Flags）

国际通⾏的分诊与紧急程度评估原则

**STEP 7**｜⻛险等级 **→** 解决⽅案（如何处理）

这⼀环节在医学上解决什么问题

👉 把判断转化为⾏动路径

权威来源依据

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

23/66World Health Organization

（世界卫⽣组织）

NICE

（英国国家卫⽣与临床优化研究所）

依据类型

WHO / NICE 的分级管理与就医建议

⾃我管理、门诊、急诊的分流原则

**STEP 8**｜结构化输出与合规话术

这⼀环节在医学与监管上解决什么问题

👉 防⽌越界诊断、误导⽤药、替代医⽣

权威来源依据

World Health Organization

（世界卫⽣组织）

国家药品监督管理局

依据类型

医疗 AI / 健康咨询系统的合规表达要求

“不替代医⽣、不作最终诊断”的国际通⾏做法

⼀句总总结（⾮常适合写在⽅案⾥）

**AI** 问诊系统的每⼀个判断节点，均可在 **WHO** 疾病分类体系、经典循证医学教材及国际临床指南中

找到明确依据，不属于模型臆断或⿊箱推理。

如果你下⼀步需要，我可以直接继续帮你做三件事之⼀（都很硬核）：

1️⃣ 把上⾯内容 整理成《合规说明章节》可直接写进⽅案**/**申报材料

2️⃣ 给你⼀张 《流程节点 **×** 书籍章节 **×** 可抽取字段》对照表

3️⃣ 选⼀个症状（如“胸痛”），逐条标注：这⼀步具体来⾃哪本书哪⼀类内容

你选⼀个，我继续往下给你“落地版”。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

24/66给我⼀张 《流程节点 × 书籍章节 × 可抽取字段》对照表

⾮常好，这⼀张表本质上就是你们 **AI** 问诊系统的**“**医学证据索引表**”**，既能指导资料抽取，⼜能直接⽤于合规说

明、评审答辩、监管沟通。

下⾯这份是 《流程节点 **×** 书籍章节 **×** 可抽取字段》标准⼯程版，

你可以 直接转 **Excel /** 数据库 **/ KG Schema** 使⽤。

《流程节点 **×** 书籍章节 **×** 可抽取字段》对照表

（AI 问诊资料抽取⼯程版）

**STEP 1**｜是否**“**有症状**”**（健康 **/** 有症状分流）

流程节点 

依据书籍 **/** 规范 

书籍章节位置 

可抽取字段

健康定义 

World Health Organization

Health Definition / Health

Status

健康定义、健康状

态描述

正常 vs 异常

Oxford Handbook of General Practice（《⽜津

全科医学⼿册》）

Consultation &

Assessment

正常变异、需⼲预

阈值

是否需要医

学处理

同上 

Clinical Judgement 

是否需要进⼀步评

估

**STEP 2**｜有症状 **→** 主诉（**Chief Complaint**）

流程节点 

依据书籍 

书籍章节位置 

可抽取字段

主诉定义

Murtagh's General Practice（《默塔全科医

学》）

Presenting Problems

主诉名称、主诉描

述

主诉分类 

同上

Symptom-based

approach 

主诉分类标签

主诉在诊断中的地

位

Symptom to Diagnosis（《从症状到诊

断》）

Clinical Reasoning

Overview

主诉 → 推理起点

**STEP 3**｜主诉 **→** 症状拆解（医学标准化）

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

25/66流程节点 

依据书籍 **/** 体系 

章节位置 

可抽取字段

症状标准

名

SNOMED CT（系统化医学术语集） 

Symptom / Finding 

症状ID、标准名称

症状定义

Bates' Guide to Physical Examination（《⻉茨

体格检查》）

Symptoms & History

Taking

症状定义

症状修饰 

同上

HPI (History of Present

Illness)

起病时间、持续时间、严

重程度

**STEP 4**｜症状 **→** 疾病系统 **/** 疾病⼤类

流程节点 

依据书籍 **/** 规范 

章节位置 

可抽取字段

疾病系统划

分

ICD-11（《国际疾病分类第11版》） 

Disease Chapters

疾病系统、⼤

类

症状系统定

位

Harrison's Principles of Internal Medicine（《哈⾥森内

科学》）

Section by Organ

System

症状 → 器官系

统

**STEP 5**｜疾病系统 **→** 疾病类型（鉴别诊断）

流程节点 

依据书籍 

章节位置 

可抽取字段

鉴别诊断⽅

法

Differential Diagnosis in Primary Care（《基层医疗

鉴别诊断》）

Differential Diagnosis

Tables

疾病候选列表

疾病概率分

层

同上 

Likely / Less Likely 

⾼ / 中 / 低概率

典型 vs ⾮

典型

Harrison's Principles of Internal Medicine 

Clinical Features

典型表现、⾮典型

表现

**STEP 6**｜疾病类型 **→** ⻛险等级判断

流程节点 

依据书籍 **/** 指南 

章节位置 

可抽取字段

红旗症状 

NICE（英国国家卫⽣与临床优化研究所） 

Red Flag / Referral 

红旗条件

紧急程度

Oxford Handbook of Acute Medicine（《⽜津急诊医学⼿

册》）

Triage & Acute Care

⾼ / 中 / 低⻛

险

是否需⽴即就

医

同上

Emergency

Indicators

急诊指征

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

26/66**STEP 7**｜⻛险等级 **→** 解决⽅案（处置路径）

流程节点 

依据书籍 **/** 指南 

章节位置 

可抽取字段

分级处理原则 

World Health Organization 

Care Pathway 

⾃我管理 / 就医

门诊 / 急诊建议 

NICE 

Management 

就医类型

随访与观察 

同上 

Follow-up Advice 

观察周期

**STEP 8**｜结构化输出与合规话术

流程节点 

依据规范 

章节 **/** 原则 

可抽取字段

⾮确诊表述 

World Health Organization 

Digital Health Ethics 

⾮诊断声明

不替代医⽣ 

国家药品监督管理局 

医疗器械/软件合规 

合规提⽰语

⽤户提⽰话术 

同上 

⻛险提⽰规范 

固定话术模板

⼀句⼯程级总结（⾮常重要）

这张表的意义在于：

你们系统中**“**每⼀个字段**”**，都能明确回答**——**

👉**“**它来⾃哪本书 **/** 哪⼀类医学共识 **/** 哪⼀个章节逻辑**”**

这在 投融资、医疗合规、专家评审 中⾮常有分量。

如果你愿意，下⼀步我可以继续帮你做三件**“**更硬核**”**的事之⼀：

1️⃣ 把这张表 直接扩展成《知识图谱实体**-**关系**-**属性表》

2️⃣ 选⼀个症状（如「胸痛 / 咳嗽 / 腹痛」），逐字段⽰范如何从书中抽取

3️⃣ 整理成 《**AI** 问诊系统 **·** 医学资料来源⽩⽪书（申报版）》

你选⼀个，我继续往下给你做“可直接⽤的版本”。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

27/66直接扩展成《知识图谱实体-关系-属性表》

好，我直接把你前⾯那张“流程节点×书籍章节×字段”扩展成可落地的《知识图谱实体**-**关系**-**属性表》（你们可以

直接按这个建库：Neo4j / RDF / 图数据库 / 甚⾄关系库也能落）。

下⾯分三部分：

1. 实体表（**Entity**）
2. 关系表（**Relation**）
3. 关键属性字典（**Attributes Dictionary**）

最后给⼀条完整链路⽰例，保证你⼀眼能⽤。

**1)** 知识图谱实体表（**Entity**）

实体类型（中⽂**/**英⽂） 

实体**ID**规则

核⼼属性（必

填）

可选属性 

说明

⽤户主诉

（ChiefComplaint）

CC

_

000001… 

名称、同义表达

语⾔、场景

标签

“⼈话⼊⼝”归⼀化后的主

诉标签

症状（Symptom）

S_ + SNOMED/⾃编

码

标准名、定义

同义词、部

位、性质

可与 SNOMED 对⻬；⽆

则⾃建

症状修饰

（SymptomModifier）

SM_000001… 

维度（时间/程度

等）、取值

取值范围

HPI 结构化维度：起病、

持续、严重度…

体征（Sign） 

SG_000001… 

标准名、定义 

检查⽅法 

例如“呼吸⾳减弱”

疾病（Disease） 

D_ + ICD11/⾃编码

标准名、定义、

系统

别名、流⾏

特征

疾病节点建议对⻬ ICD-

11

疾病系统/专科

（BodySystem）

BS_000… 

名称 

ICD章节号 

如：⼼⾎管、呼吸、消化

⻛险等级（RiskLevel） 

RL

_

LOW/MID/HIGH 

等级名、阈值原

则

颜⾊/展⽰⽂

案

低/中/⾼（或1–5级）

红旗条件（RedFlagRule） 

RF

_

000001… 

规则表达式、描

述

证据来源

“触发急诊/⽴即就医”的规

则集合

鉴别诊断条⽬

（DxCandidate）

DXC_000001… 

候选疾病、权重 

证据解释

⽤于保存“症状→候选

病”的结构化结果

评估问题（Question） 

Q_000001… 

问题⽂本、⽬的 

选项、跳转

⽤于追问：发热⼏天？是

否放射痛？

检查/检验（Test） 

T

_

000001… 

名称、⽤途 

适⽤⼈群

如：⾎常规、⼼电图（建

议项）

处置建议

（Recommendation）

R_000001… 

建议类型、⽂本 

条件、禁忌 

⾃我管理/门诊/急诊/观察

就医路径（CarePathway） 

CP

_

000001… 

路径名、步骤

时限、责任

⽅

“3天内门诊→检查→复诊”

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

28/66实体类型（中⽂**/**英⽂） 

实体**ID**规则

核⼼属性（必

填）

可选属性 

说明

科普解释（Explanation） 

EX_000001… 

⽂案

参考链接/来

源

给⽤户看的解释段落

合规声明（Disclaimer） 

DS_000001… 

⽂案 

场景标签

“不替代医⽣、不作最终诊

断”等

证据来源（Source） 

SRC_… 

来源名、版本 

章节、⻚码

书/指南/WHO体系等证据

索引

**2)** 知识图谱关系表（**Relation**）

关系命名建议⽤动词短语，便于规则引擎和可解释输出。

关系（中⽂**/**英⽂） 

起点实体 **→** 终点实体

关系属性（必

填）

可选属

性

⽤途

主诉映射为（maps_to） 

ChiefComplaint → Symptom 

置信度

⽰例语

句

“⼼⼝

堵”→“胸闷”

症状包含修饰（has_modifier） 

Symptom → SymptomModifier 

是否必问 

优先级 

HPI结构化

症状属于系统

（belongs_to_system）

Symptom → BodySystem 

权重 

备注

症状先定位

系统

疾病属于系统

（disease_in_system）

Disease → BodySystem 

— 

ICD章节 

疾病分类

症状提⽰疾病（suggests） 

Symptom → Disease

权重、⽅向(⽀

持/排除)

典型/⾮

典型

构建鉴别诊

断

症状组合触发红旗（triggers）

Symptom/Modifier →

RedFlagRule

触发阈值 

例⼦ 

安全兜底

红旗对应⻛险等级

（maps_to_risk）

RedFlagRule → RiskLevel 

— 

—

红旗→⾼⻛

险

候选诊断包含疾病

（candidate_of）

DxCandidate → Disease 

权重 

排名

保存候选集

结果

候选诊断由证据⽀持

（supported_by）

DxCandidate →

Symptom/Sign/Test 

证据强度 

解释 

可解释输出

⻛险等级给出处置建议

（recommends）

RiskLevel → Recommendation 

时限 

场景

⾼⻛险→急

诊

疾病对应处置路径

（managed_by）

Disease → CarePathway 

适⽤条件 

禁忌

病种管理路

径

处置建议引⽤解释

（explained_by）

Recommendation →

Explanation 

— 

—

⽤户可读解

释

输出包含合规声明

（requires_disclaimer）

Recommendation/Result →

Disclaimer 

— 

场景 

固定加尾注

任何节点引⽤来源

（sourced_from）

Any → Source 

章节/⻚码

引⽤摘

录

可审计、可

追溯

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

29/66关系（中⽂**/**英⽂） 

起点实体 **→** 终点实体

关系属性（必

填）

可选属

性

⽤途

追问⽤于区分疾病

（disambiguates）

Question → Disease/Symptom 

⽬标字段 

跳转

追问分流与

鉴别

**3)** 关键属性字典（**Attributes Dictionary**）

下⾯这些字段是“你们落库时最常⽤、最该统⼀⼝径”的。

**3.1** 通⽤属性（所有实体建议都有）

字段名 

类型 

⽰例 

说明

id 

string 

S_123 

全局唯⼀

name_zh 

string 

胸闷 

中⽂标准名

name_en 

string 

Chest tightness 

英⽂名（可选）

synonyms 

list<string> 

⼼⼝堵; 胸⼝闷 

同义词

definition 

string 

主观感到胸部压迫不适 

定义

status 

enum 

active/deprecated 

版本管理

version 

string 

v1.0 

数据版本

updated_at 

datetime 

2026-02-09 

更新时间

**3.2** 症状**/**主诉必备属性

字段名 

类型 

⽰例 

说明

onset 

enum/string 

sudden/gradual 

起病⽅式

duration 

string 

2天 

持续时间

severity 

enum 

mild/moderate/severe 

程度

location 

string 

胸⾻后 

部位

quality 

string 

压迫样/烧灼样 

性质

aggravating 

list 

活动加重 

加重因素

relieving 

list 

休息缓解 

缓解因素

associated 

list<Symptom> 

出汗/恶⼼ 

伴随症状

**3.3** 关系**“**权重**/**证据**”**属性（⽤于排序与可解释）

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

30/66字段名 

类型 

⽰例 

说明

weight 

float 

0.82 

候选强度/概率权重（⼯程权重）

polarity 

enum 

support/exclude 

⽀持/排除

typicality 

enum 

typical/atypical 

典型/⾮典型

evidence_level 

enum 

high/medium/low 

证据强度（抽取时给）

note 

string 

活动相关更⽀持⼼⾎管 

解释备注

**3.4** 红旗规则属性（规则引擎直接可⽤）

字段名 

类型 

⽰例 

说明

rule_expr 

string 

(胸痛 AND 出汗) OR (胸痛 AND 放射痛) 

规则表达式

triage_action 

enum 

ER/urgent/routine/selfcare 

分诊动作

risk_level 

enum 

HIGH 

⻛险级别

time_window 

string 

⽴即/24h/3天 

时限

contraindications 

list 

妊娠/⼉童等 

限制条件

**4)** ⼀条完整链路⽰例（你们系统最常⽤的**“**主诉**→**输出**”**）

⽤户输⼊：“我这两天⼼⼝堵得慌，⾛路就更明显，还出汗。”

图谱串起来应是：

1. **ChiefComplaint(**⼼⼝堵**)** **maps_to** **Symptom(**胸闷**)**
2. **Symptom(**胸闷**)** **has_modifier** **duration=2**天、**aggravating=**活动、**associated=**出汗
3. **Symptom(**胸闷**)** **belongs_to_system** **BodySystem(**⼼⾎管**/**呼吸**)**
4. **Symptom**组合 **triggers** **RedFlagRule(**胸痛**/**胸闷**+**出汗**+**活动加重**)**
5. **RedFlagRule** **maps_to_risk** **RiskLevel(HIGH)**
6. **RiskLevel(HIGH)** **recommends** **Recommendation(**⽴即就医**/**急诊评估**)**
7. **Recommendation** **requires_disclaimer** **Disclaimer(**不替代医⽣**…)**
8. 上述关键节点 **sourced_from** **Source(**全科**/**急诊**/**指南章节**)**（⽤于可审计）

你接下来最省⼒的落地⽅式（不问你也给你）

如果你们要“最快跑起来”，我建议先只建 这 **6** 张核⼼表**/**集合：

1. Symptom（症状）
2. Disease（疾病）

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

31/663. CC→Symptom 映射（主诉映射）

4. Symptom→Disease（提⽰关系，含weight）
5. RedFlagRule（规则）
6. Recommendation（处置建议，按⻛险等级关联）

其余（Question/Test/CarePathway/Explanation）后续迭代加就⾏。

如果你希望我进⼀步“把它变成你们能直接导⼊的⼯程⽂件”，我可以继续给你两种输出任选其⼀：

1️⃣ **Neo4j/**图数据库建模清单（节点标签、关系类型、索引、约束、⽰例 Cypher）

2️⃣ 关系型数据库表结构（**DDL**）**+** 字段类型 **+** 主外键（便于你们先⽤ MySQL/PG 落地）

你说⽤哪种（Neo4j 还是 MySQL/PG），我就按那个格式直接给到可⽤版本。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

32/661️⃣ Neo4j/图数据库建模清单（节点标签、关系类型、索引、约束、⽰例 Cypher）

2️⃣ 关系型数据库表结构（DDL）+ 字段类型 + 主外键（便于你们先⽤ MySQL/PG 落地）

下⾯我把两套都给你：

1. **Neo4j/**图数据库建模清单（节点标签、关系类型、索引/约束、⽰例 Cypher）
2. 关系型数据库表结构 **DDL**（PostgreSQL 版，MySQL 也⼏乎可直接⽤；差异我在⽂末点⼀下）

设计⽬标：先能跑通**“**主诉**→**症状**→**鉴别诊断**→**红旗**→**⻛险**→**建议**→**输出**”**主链路，并且可追溯来源。

1️⃣ **Neo4j /** 图数据库建模清单

**A.** 节点标签（**Labels**）与关键属性

**Label** 

中⽂名 

必备属性（建议）

**ChiefComplaint** 

主诉 

**id**, **name_zh**, **synonyms**, **version**, **status**

**Symptom** 

症状 

**id**, **name_zh**, **name_en**, **definition**, **version**, **status**

**Modifier** 

症状修饰维度/取值 

**id**, **dim**(duration/severity/...), **value**

**BodySystem** 

系统/专科 

**id**, **name_zh**, **icd_chapter**

**Disease** 

疾病 

**id**, **name_zh**, **name_en**, **icd11_code**, **definition**, **version**, **status**

**RedFlagRule** 

红旗规则 

**id**, **rule_expr**, **triage_action**, **time_window**, **risk_level**

**RiskLevel** 

⻛险等级 

**id**(LOW/MID/HIGH), **name_zh**, **rank**

**Recommendation** 

处置建议 

**id**, **type**(selfcare/clinic/er), **text_zh**, **time_window**

**Disclaimer** 

合规声明 

**id**, **text_zh**, **scene_tag**

**Question** 

追问问题 

**id**, **text_zh**, **purpose**, **priority**

**Test** 

检查/检验 

**id**, **name_zh**, **purpose**

**Source** 

来源 

**id**, **title**, **title_zh**, **edition**, **chapter**, **section**, **page**, **url**

**B.** 关系类型（**Relationship Types**）与关系属性

关系 

含义 

起点 **→** 终点 

关键关系属性

**:MAPS_TO** 

主诉映射

为症状

**ChiefComplaint** →

**Symptom** 

**confidence**, **examples**

**:HAS_MODIFIER** 

症状带修

饰项

**Symptom** → **Modifier** 

**is_required**, **priority**

**:BELONGS_TO_SYSTEM** 

症状归属

系统

**Symptom** → **BodySystem** 

**weight**

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

33/66关系 

含义 

起点 **→** 终点 

关键关系属性

**:DISEASE_IN_SYSTEM** 

疾病归属

系统

**Disease** → **BodySystem** 

—

**:SUGGESTS** 

症状提⽰

疾病

**Symptom** → **Disease** 

**weight**, **polarity**(support/exclude),

**typicality**

**:TRIGGERS** 

触发红旗

规则

**Symptom**/**Modifier** →

**RedFlagRule** 

**threshold**

**:MAPS_TO_RISK** 

规则映射

⻛险

**RedFlagRule** → **RiskLevel** 

—

**:RECOMMENDS** 

⻛险给出

处置

**RiskLevel** →

**Recommendation** 

**scene_tag**

**:REQUIRES_DISCLAIMER** 

输出需附

声明

**Recommendation** →

**Disclaimer** 

—

**:DISAMBIGUATES** 

追问区分

疾病/症状

**Question** →

**Disease**/**Symptom** 

**target_field**

**:SUGGESTS_TEST** 

建议检查

**Disease**/**RiskLevel** →

**Test** 

**priority**

**:SOURCED_FROM** 

引⽤来源 

**Any** → **Source** 

**quote**(<=短引⽤), **evidence_level**

**C.** 约束与索引（**Constraints / Indexes**）

c

// 1) 唯⼀约束（强烈建议）

CREATE CONSTRAINT cc_id IF NOT EXISTS FOR (n:ChiefComplaint) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT s_id IF NOT EXISTS FOR (n:Symptom) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT d_id IF NOT EXISTS FOR (n:Disease) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT bs_id IF NOT EXISTS FOR (n:BodySystem) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT rf_id IF NOT EXISTS FOR (n:RedFlagRule) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT rl_id IF NOT EXISTS FOR (n:RiskLevel) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT r_id IF NOT EXISTS FOR (n:Recommendation) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT ds_id IF NOT EXISTS FOR (n:Disclaimer) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT src_id IF NOT EXISTS FOR (n:Source) REQUIRE n.id IS UNIQUE;

// 2) 常⽤查询索引

CREATE INDEX symptom_name_zh IF NOT EXISTS FOR (n:Symptom) ON (n.name_zh);

CREATE INDEX disease_icd11 IF NOT EXISTS FOR (n:Disease) ON (n.icd11_code);

CREATE INDEX cc_name_zh IF NOT EXISTS FOR (n:ChiefComplaint) ON (n.name_zh);

说明：Neo4j 5.x 对全⽂检索可⽤ **db.index.fulltext.createNodeIndex**，后续你们要做“同义词/模糊

检索”时再加。

**D.** ⽰例 **Cypher**：建⼀些基础节点**/**关系（最⼩可跑）

cypher

// ⻛险等级

MERGE (:RiskLevel {id:'LOW', name_zh:'低⻛险', rank:1});

MERGE (:RiskLevel {id:'MID', name_zh:'中⻛险', rank:2});

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

34/66MERGE (:RiskLevel {id:'HIGH', name_zh:'⾼⻛险', rank:3});

// 处置建议

MERGE (:Recommendation {id:'R_ER_001', type:'er', text_zh:'建议⽴即就医/急诊评估', time_window:'⽴

即'});

MERGE (:Recommendation {id:'R_CL_001', type:'clinic', text_zh:'建议3天内门诊就医评估',

time_window:'3天'});

MERGE (:Recommendation {id:'R_SC_001', type:'selfcare', text_zh:'建议居家观察与对症处理，若加重及时就

医', time_window:'观察'});

// ⻛险 → 建议

MATCH (h:RiskLevel {id:'HIGH'}),(m:RiskLevel {id:'MID'}),(l:RiskLevel {id:'LOW'})

MATCH (er:Recommendation {id:'R_ER_001'}),(cl:Recommendation {id:'R_CL_001'}),(sc:Recommendation

{id:'R_SC_001'})

MERGE (h)-[:RECOMMENDS {scene_tag:'default'}]->(er)

MERGE (m)-[:RECOMMENDS {scene_tag:'default'}]->(cl)

MERGE (l)-[:RECOMMENDS {scene_tag:'default'}]->(sc);

// ⼀个主诉→症状→疾病→红旗→⻛险→建议的⾻架⽰例

MERGE (cc:ChiefComplaint {id:'CC_000001', name_zh:'⼼⼝堵', synonyms:['胸⼝闷','胸⼝堵得慌'],

version:'v1.0', status:'active'})

MERGE (s:Symptom {id:'S_CHEST_TIGHT', name_zh:'胸闷', name_en:'Chest tightness', definition:'胸部

压迫不适感', version:'v1.0', status:'active'})

MERGE (d:Disease {id:'D_ANGINA', name_zh:'⼼绞痛', name_en:'Angina', icd11_code:'BA40',

definition:'⼼肌缺⾎相关胸部不适', version:'v1.0', status:'active'})

MERGE (rf:RedFlagRule {id:'RF_000001', rule_expr:'(胸闷 AND 出汗 AND 活动加重)',

triage_action:'er', time_window:'⽴即', risk_level:'HIGH'})

MERGE (cc)-[:MAPS_TO {confidence:0.85, examples:['⼼⼝堵得慌']}]->(s)

MERGE (s)-[:SUGGESTS {weight:0.75, polarity:'support', typicality:'typical'}]->(d)

MERGE (s)-[:TRIGGERS {threshold:'with_sweating_and_exertion'}]->(rf)

WITH rf

MATCH (rl:RiskLevel {id:'HIGH'})

MERGE (rf)-[:MAPS_TO_RISK]->(rl);

**E.** ⽰例 **Cypher**：在线推理查询（最常⽤）

输⼊：主诉⽂本（先做匹配）**→** 输出：候选病 **+** ⻛险 **+** 建议

cypher

// 1) 找到主诉对应症状（实际你们会先做NLP命中 name_zh/synonyms）

MATCH (cc:ChiefComplaint {name_zh:'⼼⼝堵'})-[:MAPS_TO]->(s:Symptom)

// 2) 候选疾病（按权重排序取TopN）

OPTIONAL MATCH (s)-[sg:SUGGESTS]->(d:Disease)

WITH cc, s, d, sg

ORDER BY sg.weight DESC

WITH cc, s, collect({disease:d.name_zh, w:sg.weight})[0..5] AS top_dx

// 3) ⻛险（是否触发红旗）

OPTIONAL MATCH (s)-[:TRIGGERS]->(rf:RedFlagRule)-[:MAPS_TO_RISK]->(rl:RiskLevel)

WITH cc, s, top_dx, rl

ORDER BY rl.rank DESC

WITH cc, s, top_dx, head(collect(rl)) AS risk

// 4) ⻛险 → 建议

OPTIONAL MATCH (risk)-[:RECOMMENDS]->(rec:Recommendation)

RETURN

cc.name_zh AS chief_complaint,

s.name_zh AS symptom,

top_dx AS differential,

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

35/66coalesce(risk.name_zh,'未分级') AS risk_level,

collect(distinct rec.text_zh) AS recommendations;

2️⃣ 关系型数据库表结构（**DDL**）**+** 字段类型 **+** 主外键（**MySQL/PG** 可落

地）

下⾯给 **PostgreSQL DDL**（推荐 PG：JSONB 强、全⽂检索强）。你们⽤ MySQL 也能跑，差异在⽂末。

**A.** 核⼼实体表（主诉**/**症状**/**疾病**/**系统**/**规则**/**建议**/**来源）

sql

-- 1) 主诉

CREATE TABLE chief_complaint (

 

id VARCHAR(32) PRIMARY KEY,

 

name_zh VARCHAR(128) NOT NULL,

 

synonyms JSONB DEFAULT '[]',

 

version VARCHAR(32) NOT NULL DEFAULT 'v1.0',

 

status VARCHAR(16) NOT NULL DEFAULT 'active',

 

created_at TIMESTAMP NOT NULL DEFAULT now(),

 

updated_at TIMESTAMP NOT NULL DEFAULT now()

);

CREATE INDEX idx_cc_name_zh ON chief_complaint (name_zh);

-- 2) 症状

CREATE TABLE symptom (

 

id VARCHAR(64) PRIMARY KEY,

 

name_zh VARCHAR(128) NOT NULL,

 

name_en VARCHAR(128),

 

definition TEXT,

 

synonyms JSONB DEFAULT '[]',

 

version VARCHAR(32) NOT NULL DEFAULT 'v1.0',

 

status VARCHAR(16) NOT NULL DEFAULT 'active',

 

created_at TIMESTAMP NOT NULL DEFAULT now(),

 

updated_at TIMESTAMP NOT NULL DEFAULT now()

);

CREATE INDEX idx_symptom_name_zh ON symptom (name_zh);

-- 3) 系统/专科

CREATE TABLE body_system (

 

id VARCHAR(32) PRIMARY KEY,

 

name_zh VARCHAR(64) NOT NULL,

 

icd_chapter VARCHAR(32),

 

created_at TIMESTAMP NOT NULL DEFAULT now()

);

-- 4) 疾病

CREATE TABLE disease (

 

id VARCHAR(64) PRIMARY KEY,

 

name_zh VARCHAR(128) NOT NULL,

 

name_en VARCHAR(128),

 

icd11_code VARCHAR(32),

 

definition TEXT,

 

version VARCHAR(32) NOT NULL DEFAULT 'v1.0',

 

status VARCHAR(16) NOT NULL DEFAULT 'active',

 

created_at TIMESTAMP NOT NULL DEFAULT now(),

 

updated_at TIMESTAMP NOT NULL DEFAULT now()

);

CREATE INDEX idx_disease_icd11 ON disease (icd11_code);

CREATE INDEX idx_disease_name_zh ON disease (name_zh);

-- 5) ⻛险等级

CREATE TABLE risk_level (

 

id VARCHAR(16) PRIMARY KEY, -- LOW/MID/HIGH

 

name_zh VARCHAR(32) NOT NULL,

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

36/66 

rank INT NOT NULL

);

-- 6) 红旗规则

CREATE TABLE redflag_rule (

 

id VARCHAR(64) PRIMARY KEY,

 

rule_expr TEXT NOT NULL, -- 规则表达式（后续可换DSL）

 

triage_action VARCHAR(16) NOT NULL, -- er/urgent/routine/selfcare

 

time_window VARCHAR(32) NOT NULL, -- ⽴即/24h/3天...

 

risk_level_id VARCHAR(16) NOT NULL REFERENCES risk_level(id),

 

created_at TIMESTAMP NOT NULL DEFAULT now()

);

-- 7) 处置建议

CREATE TABLE recommendation (

 

id VARCHAR(64) PRIMARY KEY,

 

type VARCHAR(16) NOT NULL, -- er/clinic/selfcare

 

text_zh TEXT NOT NULL,

 

time_window VARCHAR(32),

 

created_at TIMESTAMP NOT NULL DEFAULT now()

);

-- 8) 合规声明

CREATE TABLE disclaimer (

 

id VARCHAR(64) PRIMARY KEY,

 

text_zh TEXT NOT NULL,

 

scene_tag VARCHAR(64) DEFAULT 'default'

);

-- 9) 来源（书籍/指南/WHO等）

CREATE TABLE source (

 

id VARCHAR(64) PRIMARY KEY,

 

title VARCHAR(256) NOT NULL, -- 英⽂名

 

title_zh VARCHAR(256), -- 中⽂名（如英⽂则补中⽂名）

 

edition VARCHAR(64),

 

chapter VARCHAR(128),

 

section VARCHAR(128),

 

page VARCHAR(32),

 

url TEXT

);

CREATE INDEX idx_source_title ON source (title);

**B.** 关系表（主外键 **+** 权重**/**证据字段）

sql

-- 主诉 -> 症状 映射（MAPS_TO）

CREATE TABLE cc_maps_to_symptom (

 

cc_id VARCHAR(32) NOT NULL REFERENCES chief_complaint(id),

 

symptom_id VARCHAR(64) NOT NULL REFERENCES symptom(id),

 

confidence NUMERIC(4,3) NOT NULL DEFAULT 0.800,

 

examples JSONB DEFAULT '[]',

 

source_id VARCHAR(64) REFERENCES source(id),

 

PRIMARY KEY (cc_id, symptom_id)

);

CREATE INDEX idx_cc_map_symptom ON cc_maps_to_symptom (symptom_id);

-- 症状 -> 系统（BELONGS_TO_SYSTEM）

CREATE TABLE symptom_belongs_system (

 

symptom_id VARCHAR(64) NOT NULL REFERENCES symptom(id),

 

system_id VARCHAR(32) NOT NULL REFERENCES body_system(id),

 

weight NUMERIC(4,3) NOT NULL DEFAULT 0.500,

 

source_id VARCHAR(64) REFERENCES source(id),

 

PRIMARY KEY (symptom_id, system_id)

);

-- 疾病 -> 系统（DISEASE_IN_SYSTEM）

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

37/66CREATE TABLE disease_in_system (

 

disease_id VARCHAR(64) NOT NULL REFERENCES disease(id),

 

system_id VARCHAR(32) NOT NULL REFERENCES body_system(id),

 

PRIMARY KEY (disease_id, system_id)

);

-- 症状 -> 疾病（SUGGESTS）

CREATE TABLE symptom_suggests_disease (

 

symptom_id VARCHAR(64) NOT NULL REFERENCES symptom(id),

 

disease_id VARCHAR(64) NOT NULL REFERENCES disease(id),

 

weight NUMERIC(4,3) NOT NULL DEFAULT 0.500,

 

polarity VARCHAR(16) NOT NULL DEFAULT 'support', -- support/exclude

 

typicality VARCHAR(16) NOT NULL DEFAULT 'typical', -- typical/atypical

 

evidence_level VARCHAR(16) DEFAULT 'medium', -- high/medium/low

 

note TEXT,

 

source_id VARCHAR(64) REFERENCES source(id),

 

PRIMARY KEY (symptom_id, disease_id, polarity)

);

CREATE INDEX idx_suggests_disease ON symptom_suggests_disease (disease_id);

-- 症状 -> 红旗规则（TRIGGERS）

CREATE TABLE symptom_triggers_redflag (

 

symptom_id VARCHAR(64) NOT NULL REFERENCES symptom(id),

 

redflag_id VARCHAR(64) NOT NULL REFERENCES redflag_rule(id),

 

threshold VARCHAR(128),

 

source_id VARCHAR(64) REFERENCES source(id),

 

PRIMARY KEY (symptom_id, redflag_id)

);

-- ⻛险 -> 建议（RECOMMENDS）

CREATE TABLE risk_recommends (

 

risk_level_id VARCHAR(16) NOT NULL REFERENCES risk_level(id),

 

rec_id VARCHAR(64) NOT NULL REFERENCES recommendation(id),

 

scene_tag VARCHAR(64) DEFAULT 'default',

 

PRIMARY KEY (risk_level_id, rec_id, scene_tag)

);

-- 建议 -> 声明（REQUIRES_DISCLAIMER）

CREATE TABLE rec_requires_disclaimer (

 

rec_id VARCHAR(64) NOT NULL REFERENCES recommendation(id),

 

disclaimer_id VARCHAR(64) NOT NULL REFERENCES disclaimer(id),

 

PRIMARY KEY (rec_id, disclaimer_id)

);

-- 任意实体引⽤来源（SOURCED_FROM）：⽤“通⽤引⽤表”做审计

CREATE TABLE entity_source_ref (

 

id BIGSERIAL PRIMARY KEY,

 

entity_type VARCHAR(32) NOT NULL, -- symptom/disease/rule/...

 

entity_id VARCHAR(64) NOT NULL,

 

source_id VARCHAR(64) NOT NULL REFERENCES source(id),

 

quote TEXT, -- 控制短引⽤

 

evidence_level VARCHAR(16) DEFAULT 'medium',

 

created_at TIMESTAMP NOT NULL DEFAULT now()

);

CREATE INDEX idx_esr_entity ON entity_source_ref(entity_type, entity_id);

**C.**（可选但很实⽤）追问问题与修饰项建模

sql

-- 修饰维度（duration/severity/onset...）与取值

CREATE TABLE symptom_modifier (

 

id VARCHAR(64) PRIMARY KEY,

 

dim VARCHAR(32) NOT NULL, -- duration/severity/location/...

 

value VARCHAR(128) NOT NULL

);

-- 症状 -> 修饰项

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

38/66CREATE TABLE symptom_has_modifier (

 

symptom_id VARCHAR(64) NOT NULL REFERENCES symptom(id),

 

modifier_id VARCHAR(64) NOT NULL REFERENCES symptom_modifier(id),

 

is_required BOOLEAN NOT NULL DEFAULT false,

 

priority INT NOT NULL DEFAULT 50,

 

PRIMARY KEY (symptom_id, modifier_id)

);

-- 追问问题

CREATE TABLE question (

 

id VARCHAR(64) PRIMARY KEY,

 

text_zh TEXT NOT NULL,

 

purpose VARCHAR(128),

 

priority INT NOT NULL DEFAULT 50

);

-- 问题⽤于区分（症状/疾病）

CREATE TABLE question_disambiguates (

 

question_id VARCHAR(64) NOT NULL REFERENCES question(id),

 

target_type VARCHAR(16) NOT NULL, -- symptom/disease

 

target_id VARCHAR(64) NOT NULL,

 

target_field VARCHAR(64),

 

PRIMARY KEY (question_id, target_type, target_id)

);

**D.** ⼀个**“**端到端**”SQL** 查询⽰例（主诉**→**候选病**→**⻛险**→**建议）

sql

-- 输⼊：主诉 name_zh = '⼼⼝堵'

WITH cc AS (

 

SELECT id FROM chief_complaint WHERE name_zh = '⼼⼝堵' LIMIT 1

),

sym AS (

 

SELECT m.symptom_id, m.confidence

 

FROM cc_maps_to_symptom m

 

JOIN cc ON m.cc_id = cc.id

 

ORDER BY m.confidence DESC

 

LIMIT 1

),

dx AS (

 

SELECT sd.disease_id, sd.weight

 

FROM symptom_suggests_disease sd

 

JOIN sym ON sd.symptom_id = sym.symptom_id

 

WHERE sd.polarity='support'

 

ORDER BY sd.weight DESC

 

LIMIT 5

),

risk AS (

 

SELECT r.risk_level_id

 

FROM symptom_triggers_redflag tr

 

JOIN sym ON tr.symptom_id = sym.symptom_id

 

JOIN redflag_rule r ON r.id = tr.redflag_id

 

ORDER BY (SELECT rank FROM risk_level WHERE id=r.risk_level_id) DESC

 

LIMIT 1

)

SELECT

 

(SELECT name_zh FROM symptom WHERE id=(SELECT symptom_id FROM sym)) AS symptom,

 

(SELECT jsonb_agg(jsonb_build_object('disease', d.name_zh, 'w', dx.weight))

 

FROM dx JOIN disease d ON d.id=dx.disease_id) AS differential,

 

COALESCE((SELECT name_zh FROM risk_level WHERE id=(SELECT risk_level_id FROM risk)), '未分级') 

AS risk_level,

 

(SELECT jsonb_agg(rec.text_zh)

 

FROM risk_recommends rr

 

JOIN recommendation rec ON rec.id=rr.rec_id

 

WHERE rr.risk_level_id = COALESCE((SELECT risk_level_id FROM risk), 'MID')

 

AND rr.scene_tag='default'

 

) AS recommendations;

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

39/66**MySQL vs PostgreSQL** 的⼩差异（你们落地时注意）

**JSONB**（PG）在 MySQL ⽤ **JSON**

**BIGSERIAL**（PG）在 MySQL ⽤ **BIGINT AUTO_INCREMENT**

**TIMESTAMP DEFAULT now()**（PG）在 MySQL ⽤ **DEFAULT CURRENT_TIMESTAMP**

如果你们要全⽂检索同义词：

PG：**tsvector**/**GIN**

MySQL：**FULLTEXT**（但中⽂分词需额外⽅案）

你们最推荐的落地顺序（最快上线）

1. 先⽤ 关系库 跑通：**chief_complaint → symptom → disease → redflag → risk → recommendation**
2. 第⼆阶段把关系表导⼊ Neo4j（或直接双写），⽤于：

复杂路径推理

可解释链路展⽰

多跳追问策略优化

如果你愿意，我下⼀步可以把这两套再加⼀个**“**统⼀数据字典 **+** 导⼊脚本模板**”**：

CSV → Neo4j **LOAD CSV** 模板（含字段映射）

Excel → PG/MySQL 导⼊模板（含校验规则）

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

40/66⽬前主诉和症状分别有多少种呢

在医疗诊断系统构建中，**“**主诉**”**和**“**症状**”**的数量并不是⼀个固定的数字，⽽是根据你所⽤的分类体系/词表规模

⽽变化。不同的数据来源、不同的标准体系（如医院实际登记字段 vs 标准术语体系）会有⼤不相同的数量规

模。arXiv

不过我们可以给出⽐较明确的参考量级与典型统计，便于你估算知识图谱/分类库规模：

📌 **1.** 主诉（**Chief Complaints**）数量级

🔹 医疗实践中的规模

在某些临床 NLP 训练集中，有模型使⽤ 约 **795** 个离散主诉类别 来覆盖绝⼤多数（>90%）的主诉情形。

arXiv

这类离散主诉通常是原因就诊（**reason for visit**）、常⻅症状组合的标准化表达。

➡️ 常规模型落地情况下：主诉标准列表 **≈** **500–1500** 种

（实际 NLP 库往往包含⼤量同义词与变体，但标准类别不需要⽆限扩展）

📌 **2.** 症状（**Symptomatic Findings**）数量级

🔹 标准术语体系（如 **SNOMED CT**、**HPO** 类似体系）

症状是更细粒度的临床表现，在医学术语体系⾥可以⾮常庞⼤：

SNOMED CT 中包含了成千上万的症状、体征和临床发现术语（SNOMED CT 总体术语数量超过⼏⼗万

条，症状类⽬只是其中⼀部分）

→ 这意味着潜在可提取的症状概念数量远超数千甚⾄上万（具体来⾃ SNOMED CT 症状分⽀）。

⼈卫临床助⼿

🔹 临床常⽤症状表

⼀般的临床症状清单如各类临床助⼿/诊疗⼿册中可⻅：

包含发热、咳嗽、呼吸急促、疼痛（胸痛、腹痛、头痛等）、乏⼒、恶⼼、呕吐、意识改变、⽪疹等…

✔ 基础症状类别可⾃下⽽上覆盖 数百常⻅症状。

⼈卫临床助⼿

➡️ ⼯程性建议：

核⼼⾸轮库级别可覆盖 **500–2000** 常⻅症状

如果接⼊标准术语体系（如 SNOMED CT/ICPC 等），则理论上可扩展到万级以上症状节点

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

41/66📊 对⽐总结（⽤于你们建库规模规划）

层级 

意义 

推荐初期规模

主诉类别 

病⼈⾃然表达归⼀化后的核⼼类别 

约 **500–1500** 种

症状术语 

标准化、医学可分析的临床表现 

约 **500–2000** 种（初版）

标准术语体系覆盖 

术语库全量 

可扩展⾄万级

🚀 为什么主诉⽐症状少？

主诉是⾃然语⾔⽤户表达的起点，往往简洁（⽐如“胸痛”“头晕”“咳嗽”）

症状是医学定义的更细分类单元（如内分泌/代谢性疲乏 vs 精神性疲乏两种不同症状概念）

因此，在⼯程建图谱时，主诉是症状的**“**⼊⼝类别**”**

→ 典型⼯作是：主诉 **→** 语义归⼀化 **→** 映射到症状术语节点

📥 实践落地建议（按阶段）

📌 第⼀阶段（最⼩可⽤产品 **MVP**）

主诉库：约 500–800 条

症状库：约 1000–2000 条

覆盖多数⽤户前端输⼊场景即可。

📌 第⼆阶段（追求覆盖与精细化）

引⼊ SNOMED CT 或 **ICPC**（国际初级保健分类） 体系

扩展到 5000–10000 症状节点（含不同部位/修饰组合）

这样能增强 AI 诊断的全⾯性与准确性。

世界家庭医⽣组织

📌 实证数据提⽰

有研究使⽤真实 ED 就诊记录提取主诉数据集，发现常⻅主诉包括 cough（咳嗽）、sore throat（咽

痛）、fever（发热）、headache（头痛）等，统计了成千上万次记录后主要集中于数⼗种最常⻅主诉。

Nature

这说明：

👉 主诉的⻓尾很⻓（⾃然语⾔变化多），

👉 但核⼼有⽤类别通常集中在数百个类别之内

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

42/66🧠 给你⼀个实际建库设定模板

层级 

初版数量 

是否必选引⼊ 

说明

主诉类别 

~800 

是 

基础⾃然语⾔⼊⼝

标准症状 

~1500 

是 

对应临床判断

修饰维度 

10–20 

是 

duration/severity/location 等

系统分类 

~20 

是 

呼吸/⼼⾎管/消化等

鉴别关系 

5000+ 

是 

症状↔疾病映射

红旗规则 

200+ 

可选 

较常⻅+⾼危组合

如果你希望，我还能进⼀步给你：

1️⃣ ⼀份主诉标准类别清单⽰例（约 **100–300** 条）

2️⃣ 症状标准名称⽰例（约 **300–1000** 条）

3️⃣ 如何从 **SNOMED CT / ICPC / HPO** ⾃动抽取症状节点的⽅法

你选⼀个，我就继续给出“可⽤版本”。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

43/66那我想问，我们是分别把主诉和症状列出来，对他们进⾏分别分析，然后让他们⾃然交叉相连吗

这个问题问到⽅法论的**“**⽣死点**”**了。

我先给你⼀个明确结论，再把原因、正确做法、以及“错误做法为什么会出事”讲清楚。

结论先⾏（⼀句话定⽅案）

是的：主诉和症状必须**“**分别建模、分别分析**”**，

但❌绝不是**“**⾃然交叉⽣⻓**”**，

⽽是要通过⼀层**“**受控映射关系**”**去连接。

更⼯程化地说：

主诉 **=** ⼊⼝语⾔层

症状 **=** 医学事实层

两者通过「可审计、可维护的映射关系」相连

⼀、为什么主诉和症状⼀定要分开？

1️⃣ 主诉和症状在医学上本来就不是⼀回事

维度 

主诉（**Chief Complaint**） 

症状（**Symptom**）

本质 

⼈的表达 

医学概念

是否模糊 

⾼度模糊 

尽量精确

是否标准 

❌ ⾮标准 

✅ 标准

是否可推理 

❌ 不能直接 

✅ 可以

是否稳定 

不稳定（语⾔变） 

稳定（医学定义）

👉 主诉是**“**语⾔问题**”**，症状是**“**医学问题**”**

如果你不分开，后果只有⼀个：

**NLP**、医学、规则、合规全部纠缠在⼀起，后期⼀定崩。

⼆、那能不能**“**⾃然交叉相连**”**？**——**答案是：绝对不要

❌ 什么叫**“**⾃然交叉相连**”**（你要避免的）

典型错误做法：

⽤户说⼀句话

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

44/66模型⾃⼰“理解”成⼀个或多个症状

不存、不标、不审

下次再“重新理解⼀遍”

这会导致：

1. 不可复现

同⼀句话，不同时间结果不⼀样

2. 不可审计

问你“为什么这么判断”，答不上来

3. 不可维护

改⼀个词，全系统逻辑漂移

4. 合规⻛险极⾼

本质是“⿊箱医学推断”

⚠️ 医疗系统⾥，“⾃然⽣⻓关系 = 不可控⻛险源”

三、正确做法：分层建模 **+** 受控映射（这是标准⼯程路线）

正确的三层结构是这样的：

【主诉层】 ⼈怎么说

 

↓ 映射（MAPS_TO，可维护）

【症状层】 医学上是什么

 

↓ 推理（SUGGESTS / EXCLUDES）

【疾病与⻛险层】

四、你们应该如何**“**分别列、再受控连接**”**（可执⾏）

**STEP 1**｜主诉：先**“**穷举语⾔⼊⼝**”**，不谈医学

主诉库只回答⼀件事：

**“**⽤户可能会怎么说？**”**

主诉库⾥不需要：

疾病名

⻛险

医学判断

主诉库应该包含：

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

45/66⾼频⾃然表达

同义说法

⼝语/模糊说法

⽰例（主诉表）：

主诉**ID** 

主诉标准名 

同义表达

CC_001 

⼼⼝不适 

⼼⼝堵、胸⼝闷、胸部不舒服

CC_002 

头部不适 

头晕、头胀、脑袋发懵

👉 这⼀步是语⾔⼯程，不是医学⼯程

**STEP 2**｜症状：只按医学定义来，不考虑⽤户怎么说

症状库只回答⼀件事：

**“**医学上有哪些可被诊断推理的症状单元？**”**

症状库来源：

SNOMED CT

教科书

指南

⽰例（症状表）：

症状**ID** 

症状名 

定义

S_001 

胸闷 

胸部压迫感或紧缩感

S_002 

⼼悸 

⾃觉⼼跳异常

👉 症状库是医学资产，⼏年都不该⼤变

**STEP 3**｜关键⼀步：主诉 **↔** 症状的「映射关系表」

这⼀步是整个系统的**“**稳定器**”**。

你要做的是：

❌ 不是“让模型每次⾃⼰想”

✅ ⽽是“提前定义好：这类主诉通常对应哪些症状”

⽰例（映射表）：

主诉**ID** 

症状**ID** 

置信度 

说明

CC_001 

S_001 

0.85 

最常⻅

CC_001 

S_002 

0.30 

次要可能

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

46/66重要：

⼀对多是允许的

有权重

可⼈⼯调整

可版本化

👉 这是**“**⼈类医⽣经验的结构化表达**”**

五、主诉和症状是如何**“**交叉**”**的？**——**不是⾃然，是**“**计算结果**”**

真正的“交叉”发⽣在运⾏时：

1. ⽤户输⼊ → 命中主诉
2. 主诉 → 查映射表 → 得到候选症状
3. 症状 + 修饰 → 再进⼊医学推理

交叉是结果，不是结构本⾝。

六、给你⼀个**“**反向验证法**”**（⾮常重要）

你可以问⾃⼰⼀个问题：

如果明天把所有 **NLP** 模型换掉，

主诉**—**症状**—**疾病体系还能不能继续⽤？

如果答案是 能 → 架构是对的

如果答案是 不能 → 你把语⾔层和医学层混在了⼀起

七、最终给你的标准判断

✅ 主诉和症状必须分别列、分别分析

❌ 绝不能靠**“**⾃然交叉⽣⻓**”**

✅ 只能通过可维护、可审计的映射关系相连

这是医疗 **AI** 能不能⻓期活下去的分⽔岭。

下⼀步我可以继续直接帮你做三件⾮常实操的事之⼀：

1️⃣ 给你⼀份 《主诉库建设 **SOP**（含数量⽬标与拆分⽅法）》

2️⃣ 给你⼀份 《主诉 **↔** 症状 映射关系设计规范（含权重、版本、审核）》

3️⃣ 直接拿⼀个真实案例（如“⼼⼝堵”），完整演⽰主诉**→**症状**→**疾病**→**⻛险的全链路

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

47/66你选⼀个，我继续往下给你“⼯程级答案”。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

48/66给我⼀份 《主诉库建设 SOP（含数量⽬标与拆分⽅法）》

太好了，这⼀份 《主诉库建设 **SOP**》 是你们整个 AI 问诊系统能不能规模化、能不能⻓期维护的关键基础设

施。

我下⾯给你的，是⼯程可执⾏版，不是论⽂版、不是概念版——可以直接发给团队照着⼲。

《主诉库建设 **SOP**（含数量⽬标与拆分⽅法）》

（AI 问诊系统 · 语⾔⼊⼝层标准作业流程）

⼀、主诉库在系统中的定位（⼀句话定性）

主诉库 **= AI** 问诊系统的**“**语⾔⼊⼝层**”**

它只解决⼀件事：

👉 **“**⽤户可能会怎么说**”**

⚠️ 主诉库 不做医学判断、不定义疾病、不评估⻛险

这些全部属于后⾯的【症状层 / 医学层】

⼆、主诉 **vs** 症状（再次强调边界，避免返⼯）

维度 

主诉（**Chief Complaint**） 

症状（**Symptom**）

本质 

⽤户⾃然语⾔ 

医学标准概念

是否⼝语 

是 

否

是否模糊 

⾼ 

低

是否⽤于推理 

❌ 不直接 

✅ 是

是否稳定 

不稳定 

稳定

⽣命周期 

持续扩展 

慢速变化

结论：

主诉库 = 语⾔资产

症状库 = 医学资产

两者必须分库、分 SOP

三、主诉库建设的总体⽬标

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

49/661️⃣ 数量⽬标（⾮常重要）

▶ 推荐三阶段规模

阶段 

主诉标准条⽬数 

覆盖效果

MVP 阶段 

**300–500** 条 

覆盖 >70% 常⻅问诊

可⽤阶段 

**800–1200** 条 

覆盖 >90% ⽇常问诊

成熟阶段 

**1500–2000** 条 

覆盖绝⼤多数场景

📌 注意：

这是“标准主诉条⽬数”

同义表达、变体、别说法不算在这个数⾥

2️⃣ 主诉库的**“**正确形态**”**

主诉库不是“句⼦集合”，⽽是：

**“**主诉标签 **+** 多种⾃然表达**”**的集合

四、主诉库建设流程（**SOP** 主⼲）

vbnet

Step 1 确定主诉分类框架

Step 2 按系统/部位拆分主诉⼤类

Step 3 为每⼀类定义“标准主诉”

Step 4 收集⾃然语⾔表达（同义/⼝语）

Step 5 清洗、合并、去重

Step 6 主诉与症状预映射（不做判断）

Step 7 质控、冻结版本

下⾯逐步展开 👇

**Step 1**｜确定主诉分类框架（先搭⾻架）

🎯 ⽬标

避免“想到哪写到哪”，防⽌结构混乱

推荐主分类⽅式（强烈建议）

👉 按「器官系统 **/** ⾝体部位」来拆

参考来源

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

50/66World Health Organization（世界卫⽣组织）

ICD-11（国际疾病分类第11版）

ICPC-2（国际初级保健分类）

推荐主诉⼀级分类（⽰例）

⼀级类 

⽰例

全⾝不适 

发热、乏⼒

头⾯部 

头痛、头晕

胸部 

胸痛、胸闷

呼吸 

咳嗽、⽓短

消化 

腹痛、恶⼼

泌尿 

排尿异常

⽪肤 

⽪疹、瘙痒

神经 

⿇⽊、抽搐

情绪/睡眠 

失眠、焦虑

👉 ⼀级类建议 **15–25** 个

**Step 2**｜拆分每个⼀级类下的**“**标准主诉**”**

🎯 ⽬标

为每⼀类定义**“医学可接受、语⾔中性的主诉标签”**

标准主诉的定义原则

不包含疾病名

不包含原因推断

⽤中性描述

⽰例（胸部类）

主诉**ID** 

标准主诉名

CC_CHEST_01 

胸痛

CC_CHEST_02 

胸闷

CC_CHEST_03 

⼼跳异常感觉

CC_CHEST_04 

胸部不适

📌 经验值：

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

51/66每个⼀级类：**20–60** 个标准主诉

20 个⼀级类 × 40 ≈ **800** 条主诉

**Step 3**｜为每个标准主诉收集⾃然语⾔表达

🎯 ⽬标

让 AI 能“听懂⼈话”

⾃然表达来源（推荐优先级）

1. 临床问诊记录（脱敏）
2. 医学书籍中的患者表述
3. 医⽣经验补充
4. 历史客服/健康咨询记录

权威书籍参考

Murtagh's General Practice（《默塔全科医学》）

Bates' Guide to Physical Examination（《⻉茨体格检查与病史采集》）

⽰例

标准主诉 

⾃然表达⽰例

胸闷 

⼼⼝堵、胸⼝闷得慌、胸部压着不舒服

头晕 

头发飘、站不稳、脑袋发空

📌 建议

每个标准主诉：**5–20** 条⾃然表达

不追求⼀次性穷尽，可持续补充

**Step 4**｜清洗、合并与去重（⾮常关键）

🎯 ⽬标

避免主诉库膨胀失控

去重规则

含义相同 → 合并

仅程度不同 → 不新建主诉

仅时间不同 → 不新建主诉

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

52/66❌ 错误⽰例

“胸⼝有点闷”

“胸⼝很闷”

→ 不应拆成两个主诉

**Step 5**｜主诉 **↔** 症状的预映射（不做判断）

⚠️ 这⼀步不是诊断，只是“可能对应什么症状”

⽰例

主诉 

可能症状 

权重

胸闷 

胸闷（Symptom） 

0.9

胸闷 

呼吸困难 

0.3

📌 规则：

可⼀对多

有权重

可版本化

后期可⼈⼯修订

**Step 6**｜主诉库质控标准（必须有）

每⼀条主诉必须满⾜

是否医学中性 ✔

是否⽆疾病暗⽰ ✔

是否可映射到 ≥1 个症状 ✔

推荐审核机制

1 名医学背景⼈员

1 名产品/⼯程⼈员

**Step 7**｜版本冻结与发布

版本策略（强烈建议）

v1.0：基础 300–500 条

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

53/66v1.1 / v1.2：补充新主诉

禁⽌随意修改已发布主诉含义

五、主诉库数据结构（⽰例）

json

{

 

"cc_id": "CC_CHEST_02",

 

"standard_name": "胸闷",

 

"synonyms": [

 

"⼼⼝堵",

 

"胸⼝闷得慌",

 

"胸部压着不舒服"

 

],

 

"category": "胸部",

 

"status": "active",

 

"version": "v1.0"

}

六、⼀句**“**内部⾦句**”**（你可以直接对团队说）

主诉库不是越⼤越好，⽽是：

⽤最少的主诉，接住最多的说法。

接下来你可以继续这样推进（我可以继续帮你）

如果你愿意，我可以下⼀步直接给你：

1️⃣ 《主诉 **↔** 症状 映射关系设计规范（含权重、审核、版本）》

2️⃣ 《症状库建设 **SOP**（含 **SNOMED/**书籍抽取⽅法）》

3️⃣ 直接给你⼀份：**300** 条**“**可直接⽤**”**的主诉标准清单（按系统拆好）

你选⼀个，我继续往下给你“落地级内容”。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

54/66给我⼀份 《主诉 ↔ 症状 映射关系设计规范（含权重、版本、审核）》

好，这⼀份我会**按“医疗级⼯程规范”**给你，⽽不是泛泛的⽅法说明。

你可以把它当作 制度⽂件 **+** 技术规范 **+** 审核标准 来⽤，直接纳⼊你们的研发/医学 SOP 体系。

《主诉 **↔** 症状 映射关系设计规范》

（含权重模型 · 版本机制 · 审核流程）

适⽤范围：AI 问诊 / 诊断⽀持 / 分诊系统

适⽤对象：医学内容组、算法组、产品组、合规组

⼀、规范⽬的（为什么要有这份规范）

这张映射表在系统中的⾓⾊

主诉 **↔** 症状 映射关系 **=**

连接**“**⼈类语⾔世界**”**和**“**医学推理世界**”**的唯⼀合法通道

如果没有这层规范化映射，会导致：

NLP ⿊箱化

医学逻辑不可追溯

同⼀句话多次结果不⼀致

⽆法通过医疗合规审查

因此：映射关系必须是**“**⼈⼯可控、可解释、可回滚**”**的。

⼆、基本设计原则（必须遵守）

原则 **1**：主诉和症状永远不合并

主诉 ≠ 症状

映射 ≠ 等价

映射只是可能性关系

原则 **2**：映射关系必须**“**显式存储**”**

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

55/66❌ 不允许只存在于模型参数⾥

✅ 必须存在于表 / 图 / 配置中

原则 **3**：⼀切映射都必须可版本化、可审计

谁建的

为什么这么建

哪个版本⽣效

三、映射关系的基本结构定义

1️⃣ 映射关系类型

统⼀使⽤以下关系语义（强制）：

关系类型 

含义 

是否允许

主诉 → 症状 

主诉可能表达该症状 

✅

症状 → 主诉 

禁⽌反向定义 

❌

主诉 → 疾病 

禁⽌ 

❌

2️⃣ 映射表标准字段（必备）

《主诉 **↔** 症状 映射表》

字段名 

含义 

是否必填

cc_id 

主诉ID 

✅

symptom_id 

症状ID 

✅

weight 

映射权重 

✅

mapping_type 

映射类型 

✅

explanation 

映射说明 

✅

source 

依据来源 

✅

version 

版本号 

✅

status 

状态 

✅

四、映射权重设计规范（核⼼）

1️⃣ 权重的医学含义（⾮常重要）

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

56/66权重 **≠** 概率 **≠** 发病率

权重表⽰的是：

**“**当⽤户使⽤该主诉时，这个症状被表达出来的可能强弱**”**

2️⃣ 权重区间标准（强制统⼀）

权重区间 

医学语义 

⼯程解释

0.80 – 1.00 

⾼度⼀致 

⼏乎等价表达

0.50 – 0.79 

常⻅对应 

需要进⼀步确认

0.20 – 0.49 

可能对应 

作为候选

< 0.20 

极弱关联 

不建议保留

📌 规则：

每个主诉⾄少有 **1** 个 **≥****0.7** 的症状

≤0.2 的映射默认不⼊库

3️⃣ 映射类型（**mapping_type**）

类型 

含义 

⽰例

direct 

⼏乎同义 

⼼⼝堵 → 胸闷

partial 

部分表达 

胸部不适 → 胸闷

contextual 

场景相关 

⼼慌 → ⼼悸

ambiguous 

多义模糊 

头不舒服 → 头晕/头痛

五、映射数量与结构约束（防⽌失控）

1️⃣ 单主诉映射上限（建议）

映射项 

建议上限

⾼权重症状（≥0.7） 

1–2 个

中权重症状（0.3–0.69） 

≤3 个

总映射症状数 

≤5 个

超过 **5** 个，说明主诉定义不清，应回退重拆主诉

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

57/662️⃣ 禁⽌⾏为（红线）

❌ ⼀个主诉映射 10+ 症状

❌ 权重全部设成 0.5

❌ 没有 explanation 字段

❌ ⽆来源标注

六、映射关系的来源与依据要求

每⼀条映射必须⾄少满⾜⼀条依据

来源类型 

⽰例

教材/指南 

全科医学、症状学教材

临床共识 

医⽣经验整理

语料统计 

真实问诊⽂本统计

规范术语 

SNOMED CT 症状定义

📌 必须写清楚来源类型，哪怕是“专家共识（内部）”

七、版本管理规范（⾮常关键）

1️⃣ 版本号规则（强制）

v主版本.次版本

主版本（**v1** **→** **v2**）：

主诉定义变化

映射逻辑⼤调整

次版本（**v1.0** **→** **v1.1**）：

新增映射

权重微调

2️⃣ 版本冻结规则

已发布版本 不可直接修改

所有修改必须：

新建版本

标注变更说明

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

58/663️⃣ 映射变更记录（建议表）

**cc_id** 

**symptom_id** 

旧权重 

新权重 

变更原因 

审核⼈

⼋、审核流程规范（双重审核制）

1️⃣ 审核⾓⾊

⾓⾊ 

职责

医学审核 

合理性、医学准确性

⼯程/产品审核 

稳定性、⼀致性

2️⃣ 审核清单（**Checklist**）

每条映射必须回答：

这个主诉真的会这样被⽤户说吗？

这个症状是医学标准概念吗？

权重是否符合直觉？

是否会误导进⼊错误系统？

九、⽰例（标准合格映射）

json

{

 

"cc_id": "CC_CHEST_02",

 

"symptom_id": "S_CHEST_TIGHTNESS",

 

"weight": 0.85,

 

"mapping_type": "direct",

 

"explanation": "“⼼⼝堵”在⽇常表达中多⽤于描述胸部压迫感",

 

"source": "全科医⽣经验 + 症状学教材",

 

"version": "v1.0",

 

"status": "active"

}

⼗、⼀句**“**团队必须记住的话**”**

主诉 **↔** 症状映射不是模型训练数据，

⽽是**“**被⼈类医⽣背书的结构化经验**”**。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

59/66接下来我可以继续直接帮你做三件事（强烈推荐顺序）：

1️⃣ 《症状库建设 **SOP**（含 **SNOMED/**教材抽取规则）》

2️⃣ 《症状 **→** 疾病 映射与鉴别诊断权重设计规范》

3️⃣ 直接给你⼀套：主诉**↔**症状**↔**疾病 的完整样例库（可直接导⼊）

你选⼀个，我继续往下给你“真正可落地的那⼀层”。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

60/662️⃣ 《症状 → 疾病 映射与鉴别诊断权重设计规范》

好，这⼀份我会直接给你**“**医疗级、⼯程可执⾏**”**的正式规范⽂本。

它和你前⾯的《主诉 ↔ 症状映射规范》是同⼀套体系的**“**下游核⼼**”**，决定了你们的 AI 像不像医⽣、稳不稳、敢

不敢上线。

下⾯这份内容，你可以原样作为内部制度⽂件使⽤。

《症状 **→** 疾病 映射与鉴别诊断权重设计规范》

（Differential Diagnosis Mapping & Weighting Spec）

适⽤范围：AI 问诊 / 诊断⽀持 / 分诊与⻛险评估系统

适⽤对象：医学内容组、算法组、规则引擎组、合规组

⼀、这层映射在系统中的**“**医学地位**”**

⼀句话定性

症状 **→** 疾病映射 **=** 临床鉴别诊断（**Differential Diagnosis**）的结构化表达

这不是 AI 发明的逻辑，⽽是医⽣每天都在做的标准思维过程。

⼆、基本原则（红线级）

原则 **1**：症状不能**“**直接等于**”**疾病

❌ 不存在 1 个症状 = 1 个疾病

✅ 只允许：症状 **→** 疾病候选集合

原则 **2**：必须体现**“**鉴别诊断思想**”**

即：

常⻅病

少⻅但不能漏的病

危险但必须警惕的病

原则 **3**：权重 **≠** 概率 **≠** 诊断结论

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

61/66权重的含义是：

在**“**仅已知该症状**”**的前提下，该疾病被考虑进候选集的优先级

三、症状 **→** 疾病映射的标准结构

《症状 **→** 疾病 映射表》标准字段

字段名 

含义 

是否必填

symptom_id 

症状ID 

✅

disease_id 

疾病ID 

✅

weight 

候选权重 

✅

polarity 

关系⽅向 

✅

typicality 

典型性 

✅

explanation 

医学解释 

✅

evidence_source 

依据来源 

✅

version 

版本号 

✅

status 

状态 

✅

四、权重设计规范（最关键部分）

1️⃣ 权重的医学语义（再次强调）

权重表⽰的是：

当**“**只知道有该症状**”**时，

医⽣在脑中**“**⾸先会想到该疾病**”**的优先程度

它不是：

发病率

⼈群概率

模型输出概率

2️⃣ 权重区间与含义（强制统⼀）

权重区间 

医学含义 

⽰例

0.70 – 1.00 

⾼优先级候选 

胸痛 → ⼼绞痛

0.40 – 0.69 

中等优先级 

胸痛 → 胃⾷管反流

0.20 – 0.39 

低优先级 

胸痛 → 肋间神经痛

< 0.20 

不建议⼊库 

噪声级

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

62/66📌 规则：

每个症状 ⾄少 **1** 个 **≥****0.6** 的疾病

≥0.6 的疾病通常不超过 **3** 个

3️⃣ **polarity**（⽀持 **/** 排除）

**polarity** 

含义 

使⽤场景

support 

⽀持该疾病 

默认

exclude 

⽤于排除 

症状出现反⽽不⽀持

📌 ⽰例：

“咳⾎” → 排除普通感冒（exclude）

4️⃣ **typicality**（典型性）

**typicality** 

含义

typical 

典型表现

atypical 

⾮典型但可能

⾮典型不能删，但权重要低于典型

五、鉴别诊断结构要求（核⼼⽅法论）

每个症状的疾病候选必须覆盖三类

类别 

必须性 

说明

常⻅病 

必须 

覆盖⼤多数情况

危险病 

必须 

哪怕概率低

良性/⾃限性 

建议 

防⽌过度恐慌

📌 缺任何⼀类，视为不合格映射

六、映射数量与上限控制

单⼀症状的建议规模

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

63/66项⽬ 

建议值

support 关系疾病数 

3–8 个

exclude 关系疾病数 

≤3 个

总候选疾病数 

≤10 个

超过 10 个，说明症状定义过宽，应拆分症状

七、来源与证据要求（合规重点）

每条映射必须标注⾄少⼀种来源类型

来源类型 

⽰例

经典教材 

《哈⾥森内科学》

症状学教材 

《症状到诊断》

全科医学 

《默塔全科医学》

临床指南 

NICE 指南

医⽣共识 

内部专家审定

推荐权威来源包括：

Harrison's Principles of Internal Medicine（《哈⾥森内科学》）

Symptom to Diagnosis（《从症状到诊断》）

NICE（英国国家卫⽣与临床优化研究所）

⼋、版本管理规范（与主诉层保持⼀致）

1️⃣ 版本号规则

v主版本.次版本

主版本：结构或医学逻辑⼤改

次版本：新增/微调权重

2️⃣ 禁⽌⾏为

❌ 直接修改线上版本权重

❌ ⽆版本说明的调整

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

64/66九、审核流程（强制）

审核⾓⾊

⾓⾊ 

职责

医学审核 

医学合理性、是否符合诊断思维

合规审核 

是否存在误导性倾向

⼯程审核 

是否满⾜权重/数量规范

审核 **Checklist**（必须全部通过）

是否体现鉴别诊断思想？

是否包含危险病？

权重是否符合直觉？

是否可能导致误判或恐慌？

⼗、标准⽰例（合格）

json

{

 

"symptom_id": "S_CHEST_PAIN",

 

"disease_id": "D_ANGINA",

 

"weight": 0.75,

 

"polarity": "support",

 

"typicality": "typical",

 

"explanation": "活动相关胸痛是⼼肌缺⾎的常⻅表现",

 

"evidence_source": "《哈⾥森内科学》⼼⾎管章节",

 

"version": "v1.0",

 

"status": "active"

}

⼗⼀、⼀句**“**医⽣级总结**”**

**AI** 的鉴别诊断能⼒，

不来⾃模型参数，

⽽来⾃你们是否把**“**医⽣脑中的候选列表**”**结构化出来。

接下来最合理的下⼀步（我强烈建议）

你现在三层已经⻬了：

主诉库 SOP ✅

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

65/66主诉 ↔ 症状 映射规范 ✅

症状 → 疾病 映射规范 ✅

👉 下⼀步只剩 **“**决策与安全层**”**：

1️⃣ 《红旗症状与⻛险分级规则设计规范》

2️⃣ 《多症状组合下的疾病权重合并算法设计》

3️⃣ 直接跑⼀个完整⽰例（胸痛 **/** 咳嗽 **/** 腹痛）端到端

你选⼀个，我继续给你“最后⼀块拼图”。

Printed using ChatGPT to PDF, powered by PDFCrowd HTML to PDF API. 

66/66