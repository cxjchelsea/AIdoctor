# AI医生系统 - 大模型在推理流程中的作用与约束机制

> **文档目的**：详细说明大模型在工具内部的使用方式、连接方式、约束机制和实施方法  
> **文档定位**：面向技术团队，用于指导大模型在工具中的集成与约束实现  
> **核心原则**：大模型作为工具内部的能力组件，在主Agent的调用下工作，确保医疗诊断的准确性和可追溯性  
> **架构定位**：主Agent + 工具架构，大模型在工具内部使用，不直接暴露给主Agent  
> **更新时间**：2024年（根据最新系统功能设计和技术架构设计更新）

---

## 更新说明

**本次更新内容**（基于《AI医生系统-系统功能设计.md》和《AI医生系统-技术架构设计.md》）：

1. **架构定位调整**：
   - 明确大模型在工具内部使用，不在主Agent层面直接调用
   - 工具内部使用大模型，返回结构化结果给主Agent
   - 主Agent通过调用工具间接使用大模型能力

2. **完善5步AI循证诊断流程中大模型的作用**：
   - Step 2：诊断工具内部使用大模型进行推理，输出三层分层诊断结构
   - Step 5：解释生成工具内部使用大模型生成自然语言解释

3. **新增流程回退机制中大模型的作用**（第三章）：
   - 回退到Step 1：问题识别工具内部使用大模型重新澄清问题
   - 回退到Step 3：分流路径工具内部使用大模型重新组织分流路径
   - 回退到Step 4：诊断工具内部使用大模型基于新证据重新评估诊断

4. **完善知识图谱路径注入机制**：
   - 基于贝叶斯诊断理论的三层评分体系（先验概率、似然评分、后验概率）
   - 路径格式化时包含三层评分信息
   - 提示词中要求大模型输出三层分层结构和入选依据

5. **修正章节编号**：
   - 新增第三章：大模型在流程回退机制中的作用
   - 原第三章调整为第四章：大模型的作用方式
   - 原第四章调整为第五章：大模型的连接方式
   - 原约束机制章节调整为第六章：大模型的约束机制

---

## 一、大模型在系统中的使用位置与作用

### 1.1 总体架构：工具内部使用大模型

系统采用**主Agent + 工具**架构，大模型在工具内部使用，不直接暴露给主Agent：

```
┌─────────────────────────────────────────────────────────────┐
│                    主Agent（Clinical Agent Brain）            │
│  - 自主决策、工具调用、停止/升级/拒答                          │
│  - 唯一"最终结论提交者"                                        │
└─────────────────────────────────────────────────────────────┘
                            ↓ 调用工具
┌─────────────────────────────────────────────────────────────┐
│                    工具1：诊断工具（Diagnosis Tool）          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  工具内部：大模型推理引擎（LLM Engine）                  │  │
│  │  - 作用：在知识图谱路径约束下进行深度推理                │  │
│  │  - 输入：从CDP读取患者信息 + 知识图谱推理路径            │  │
│  │  - 输出：疾病可能性分析（结构化JSON）                    │  │
│  │  - 约束：必须基于注入的推理路径进行推理                   │  │
│  │  - 返回：ToolResult（status, payload, evidence）       │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓ 写回CDP
                    CDP（Clinical Decision Package）
                            ↓ 主Agent读取
┌─────────────────────────────────────────────────────────────┐
│                    工具2：解释生成工具（Explanation Tool）    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  工具内部：大模型NLG（自然语言生成）                      │  │
│  │  - 作用：将结构化诊断结果转换为自然语言                 │  │
│  │  - 输入：从CDP读取结构化诊断结果 + 终点结论包            │  │
│  │  - 输出：人性化的自然语言解释                           │  │
│  │  - 约束：必须基于结构化的诊断结果，不能自行推理          │  │
│  │  - 返回：ToolResult（status, payload, evidence）       │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**核心理念**：
> **大模型在工具内部使用，工具返回结构化结果给主Agent。主Agent不直接调用大模型，而是通过调用工具间接使用大模型能力。**

### 1.2 大模型使用位置详细说明

#### 位置1：诊断工具（Diagnosis Tool）内部 - 大模型推理引擎

**工具路径**：`diagnosis-tool/app/engines/llm_engine.py`

**工具职责**：
- 主Agent调用诊断工具，工具内部使用大模型进行推理
- 工具从CDP读取患者信息，结合知识图谱推理路径（DR.KNOWS方法）进行深度推理
- 工具返回结构化结果（ToolResult），包含疾病可能性评分、支持证据、反对证据等

**调用流程**：
1. 主Agent调用诊断工具，传入ToolContext（包含CDP引用、agentState摘要等）
2. 诊断工具内部：
   - 从CDP读取患者信息（`cdp.problem_representation`, `cdp.patient_state`等）
   - 调用知识图谱工具获取推理路径
   - 调用大模型引擎进行推理（工具内部使用）
   - 融合多个引擎结果（规则引擎、知识图谱引擎、统计模型引擎、大模型引擎、鉴别诊断引擎）
3. 工具返回ToolResult给主Agent，主Agent根据结果更新CDP

**工具内部实现**：
```python
# diagnosis-tool/app/tools/diagnosis_tool.py
class DiagnosisTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取患者信息
        cdp = context.cdp
        patient_info = cdp.problem_representation
        symptoms = cdp.patient_state.symptoms
        
        # 2. 工具内部调用多个引擎（包括大模型引擎）
        results = await asyncio.gather(
            self.rule_engine.diagnose(patient_info),
            self.kg_engine.diagnose(patient_info),
            self.statistical_engine.diagnose(patient_info),
            self.llm_engine.diagnose(patient_info),  # 工具内部使用大模型
            self.differential_engine.diagnose(patient_info),
            return_exceptions=True
        )
        
        # 3. 融合结果
        fused_result = self._fuse_results(results)
        
        # 4. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "ddx": fused_result.ddx,
                "confidence": fused_result.confidence
            },
            evidence=fused_result.evidence,
            suggestedWrites={
                "cdp.ddx.rank_list": fused_result.ddx,
                "cdp.ddx.confidence": fused_result.confidence
            }
        )
```

**权重分配**（工具内部）：
```python
weights = {
    'rule': 0.25,           # 规则引擎
    'kg': 0.25,             # 知识图谱引擎
    'statistical': 0.20,    # 统计模型引擎
    'llm': 0.25,            # 大模型引擎（25%权重，工具内部使用）
    'differential': 0.05    # 鉴别诊断引擎
}
```

#### 位置2：解释生成工具（Explanation Tool）内部 - 自然语言生成

**工具路径**：`explanation-tool/app/services/explanation_service.py`

**工具职责**：
- 主Agent调用解释生成工具，工具内部使用大模型生成自然语言解释
- 工具从CDP读取结构化诊断结果，转换为用户友好的自然语言
- 工具返回ToolResult，包含自然语言解释和建议写回CDP的字段

**调用流程**：
1. 主Agent在Step 5完成后调用解释生成工具
2. 工具从CDP读取结构化诊断结果（`cdp.conclusion_package`等）
3. 工具内部使用大模型生成自然语言解释
4. 工具返回ToolResult，主Agent根据结果更新CDP

**工具内部实现**：
```python
# explanation-tool/app/tools/explanation_tool.py
class ExplanationTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取结构化诊断结果
        cdp = context.cdp
        conclusion_package = cdp.conclusion_package
        
        # 2. 工具内部使用大模型生成自然语言解释
        natural_language_explanation = await self._llm_generate_explanation(
            conclusion_package,
            cdp
        )
        
        # 3. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "explanation": natural_language_explanation
            },
            evidence=[],
            suggestedWrites={
                "cdp.final.explanation": natural_language_explanation
            }
        )
```

#### 位置3：问题识别工具（Problem Identification Tool）内部 - 自然语言理解

**工具路径**：`problem-identification-tool/app/core/nlu.py`

**工具职责**：
- 主Agent在Step 1调用问题识别工具，工具内部使用大模型进行NLU
- 工具理解用户的自然语言输入，提取结构化问题清单
- 工具返回ToolResult，包含结构化问题清单和建议写回CDP的字段

**调用流程**：
1. 主Agent在Step 1调用问题识别工具，传入用户输入
2. 工具内部使用大模型进行NLU，提取结构化信息
3. 工具返回ToolResult，主Agent根据结果更新CDP（`cdp.problem_representation`）

#### 位置4：问诊问题生成工具（Question Generation Tool）内部 - 自然语言生成

**工具路径**：`question-generation-tool/app/core/nlg.py`

**工具职责**：
- 主Agent在Step 3和Step 4调用问诊问题生成工具
- 工具内部使用大模型生成自然的追问问题
- 工具返回ToolResult，包含生成的问诊问题和建议写回CDP的字段

**调用流程**：
1. 主Agent在Step 3或Step 4调用问诊问题生成工具
2. 工具从CDP读取诊断候选、信息缺口等（`cdp.ddx.rank_list`, `cdp.missing_info`等）
3. 工具内部使用大模型生成问诊问题
4. 工具返回ToolResult，主Agent根据结果更新CDP

**工具内部实现**：
```python
# question-generation-tool/app/tools/question_generation_tool.py
class QuestionGenerationTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取诊断候选和信息缺口
        cdp = context.cdp
        ddx_candidates = cdp.ddx.rank_list
        missing_info = cdp.missing_info
        
        # 2. 工具内部使用大模型生成问诊问题
        question = await self._llm_generate_question(
            ddx_candidates=ddx_candidates,
            missing_info=missing_info,
            context=cdp
        )
        
        # 3. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "question": question
            },
            evidence=[],
            suggestedWrites={
                "cdp.current_question": question
            }
        )
```

#### 位置5：健康状态判定工具（Health State Assessment Tool）内部 - 入口判定（可选增强）

**工具路径**：`health-state-assessment-tool/app/services/entry_assessment/`

**工具职责**（可选增强）：
- 主Agent在入口判定时调用健康状态判定工具
- 工具内部使用大模型进行NLU，理解用户意图，提取症状和需求
- 工具返回ToolResult，包含判定结果和建议写回CDP的字段

**当前状态**：
- 基础版本使用规则和关键词匹配
- 可选增强：使用大模型提高识别准确性

**工具内部实现**（可选增强）：
```python
# health-state-assessment-tool/app/tools/health_state_assessment_tool.py
class HealthStateAssessmentTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取用户输入
        cdp = context.cdp
        user_input = cdp.user_input
        
        # 2. 工具内部使用大模型进行入口判定
        assessment_result = await self._assess_entry_with_llm(user_input)
        
        # 3. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "assessment": assessment_result
            },
            evidence=[],
            suggestedWrites={
                "cdp.health_state_assessment": assessment_result
            }
        )
    
    async def _assess_entry_with_llm(self, user_input: str) -> dict:
        """工具内部：使用大模型进行入口判定"""
        # Step 1：接收用户输入
        prompt_step1 = f"""分析用户的输入，提取以下信息：
        用户输入：{user_input}
        
        请提取：
        1. 是否有症状/困扰（是/否/不确定）
        2. 如果有症状，提取症状列表
        3. 如果是健康管理需求，识别需求类型（筛查/健康目标管理/计划性需求）
        4. 是否有混合诉求
        
        以JSON格式返回：
        {{
            "has_symptom": "是/否/不确定",
            "symptoms": ["症状列表"],
            "demand_type": 1/2/3/4/null,
            "demand_type_name": "筛查建议/健康目标管理/计划性健康需求",
            "is_mixed": true/false
        }}
        """
        step1_result = await self.llm_client.generate(prompt_step1)
        
        # Step 2：识别用户是否有症状/困扰
        prompt_step2 = f"""判断用户输入是否表示有症状/困扰：
        
        用户输入：{user_input}
        提取的症状：{step1_result.get('symptoms', [])}
        
        判断结果：
        - 情况A（明确无症状）：用户表达的是"健康管理/体检规划/筛查/预防"，且没有任何不适描述
        - 情况B（存在症状/困扰）：用户描述任何身体不适、异常感觉、功能变化、心理困扰等（不论轻重）
        - 情况C（不确定/模糊/混合诉求）：用户说不清是否算不适，或同时说"想体检 + 有点不舒服"
        
        返回：
        {{
            "status": "no_symptom/has_symptom/uncertain",
            "symptoms": ["症状列表"],
            "has_wellness_intent": true/false,
            "is_mixed": true/false
        }}
        """
        step2_result = await self.llm_client.generate(prompt_step2)
        
        # Step 3：方向澄清（如果不确定）
        if step2_result.get('status') == 'uncertain':
            prompt_step3 = f"""用户的表达模糊或混合诉求，需要生成一个最小澄清问题：
            
            用户输入：{user_input}
            当前判断：{step2_result.get('status')}
            
            生成一个澄清问题，确认用户主要目标是：
            - A：健康筛查/体检规划
            - B：症状咨询/问题排查
            
            要求：
            1. 问题要简洁明了，只问一次
            2. 问题要自然、口语化
            3. 澄清后不得反复追问
            
            返回：
            {{
                "question": "澄清问题文本",
                "direction": "A/B",
                "clarified": true/false
            }}
            """
            step3_result = await self.llm_client.generate(prompt_step3)
            return {
                **step1_result,
                **step2_result,
                **step3_result
            }
        
        return {
            **step1_result,
            **step2_result
        }
```

#### 位置5：健康管理工具（Health Management Tool）内部 - 健康管理计划生成（可选增强）

**工具路径**：`health-management-tool/app/tools/health_management_tool.py`

**工具职责**（可选增强）：
- 主Agent调用健康管理工具，工具内部使用大模型生成健康管理计划
- **安抚与解释生成**：为健康管理态用户生成安抚性的解释文本
- **健康建议生成**：生成生活方式建议的自然语言表述
- **随访提醒生成**：生成随访提醒的自然语言话术

**工具内部实现**（可选增强）：
```python
# health-management-tool/app/tools/health_management_tool.py
class HealthManagementTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取患者情况
        cdp = context.cdp
        
        # 2. 工具内部使用大模型生成安抚与解释文本
        reassurance_text = await self._generate_reassurance_with_llm(cdp)
        
        # 3. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "reassurance_text": reassurance_text
            },
            evidence=[],
            suggestedWrites={
                "cdp.health_management.reassurance": reassurance_text
            }
        )
    
    async def _generate_reassurance_with_llm(self, cdp: CDP) -> str:
        """工具内部：使用大模型生成安抚与解释文本"""
        prompt = f"""根据以下情况，生成一段安抚性的解释文本：
        
        患者情况：
        - 症状：{cdp.patient_state.symptoms}
        - 严重程度：{cdp.health_state_assessment.symptom_severity}
        - 风险等级：{cdp.health_state_assessment.risk_level}
        
        要求：
        1. 语言温和、安抚，减轻患者焦虑
        2. 解释症状可能的原因（正常范围内的不适）
        3. 提供生活方式的建议
        4. 明确说明什么情况下需要就医
        
        返回一段自然、友好的解释文本。
        """
        # 调用大模型API
        # ...
```

---

## 二、大模型在5步AI循证诊断流程中的作用

### 2.0 总体流程概览

系统采用**5步AI循证诊断流程**，主Agent调用工具，工具内部使用大模型：

```
主Agent运行循环：Observe → Plan → Act → Update → Evaluate → Stop/Escalate

Step 1: 识别问题
  主Agent调用问题识别工具
  ├─ 工具内部：NLU（可选）- 理解用户主诉，提取结构化信息
  └─ 工具内部：概念归一化（可选）- 将口语化表达转换为标准医学术语
  ↓ 工具返回ToolResult，主Agent更新CDP
Step 2: 构建鉴别诊断候选集并分层
  主Agent调用诊断工具
  ├─ 工具内部：大模型引擎（核心）- 在路径约束下生成鉴别诊断
  └─ 工具内部：知识图谱路径注入 - DR.KNOWS方法
  ↓ 工具返回ToolResult，主Agent更新CDP
Step 3: 组织候选集并建立分流路径
  主Agent调用分流路径工具
  └─ 工具内部：NLG（可选）- 生成第一层分叉问题的自然语言表述
  ↓ 工具返回ToolResult，主Agent更新CDP
Step 4: 采集关键证据并形成排序与验证计划
  主Agent调用问诊问题生成工具
  ├─ 工具内部：NLG（可选）- 生成自然的追问问题
  └─ 工具内部：基于临床决策分析的问题生成
  ↓ 工具返回ToolResult，主Agent更新CDP
Step 5: 回填证据并输出终点结论包
  主Agent调用解释生成工具
  ├─ 工具内部：大模型引擎（可选）- 重新评估诊断（基于新证据）
  └─ 工具内部：NLG（核心）- 生成自然语言解释和终点结论包说明
  ↓ 工具返回ToolResult，主Agent更新CDP并提交最终结论
```

### 2.1 Step 1：识别问题中的大模型（工具内部使用）

**工具**：问题识别工具（Problem Identification Tool）

**工具职责**：
- 主Agent调用问题识别工具，工具内部使用大模型进行NLU
- 理解用户的自然语言输入，提取结构化信息
- 将口语化表达转换为标准医学术语

**工具内部实现**：
```python
# problem-identification-tool/app/tools/problem_identification_tool.py
class ProblemIdentificationTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从ToolContext获取用户输入
        user_input = context.cdp.user_input
        
        # 2. 工具内部使用大模型进行概念归一化
        concepts = await self._normalize_concepts_with_llm(user_input)
        
        # 3. 构建结构化问题清单
        problem_list = self._build_problem_list(concepts)
        
        # 4. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "problem_list": problem_list
            },
            evidence=[],
            suggestedWrites={
                "cdp.problem_representation": problem_list
            }
        )
    
    async def _normalize_concepts_with_llm(self, user_input: str) -> List[Concept]:
        """工具内部使用大模型进行概念归一化"""
        prompt = f"""将以下患者描述转换为标准医学术语（CUI编码）：

患者描述：{user_input}

要求：
1. 识别症状、体征、检查结果等医学概念
2. 为每个概念提供标准医学术语（CUI编码）
3. 处理同义表达和歧义表达
4. 如果存在歧义，标注出来

请以JSON格式返回：
{{
    "concepts": [
        {{
            "original_text": "原文",
            "normalized_term": "标准术语",
            "cui": "CUI编码",
            "category": "症状/体征/检查结果",
            "ambiguity": "是否存在歧义"
        }}
    ]
}}
"""
        # 工具内部调用大模型API
        llm_response = await self.llm_client.generate(prompt)
        return self._parse_concepts(llm_response)
```

### 2.2 Step 2：构建鉴别诊断候选集并分层中的大模型（工具内部使用，核心）

**工具**：诊断工具（Diagnosis Tool）

**工具职责**：
- 主Agent调用诊断工具，工具内部使用大模型进行推理
- 在知识图谱路径约束下，生成鉴别诊断候选集
- 结合DR.KNOWS方法的推理路径进行深度推理
- 输出三层分层的诊断候选（首要假设、主要备选诊断、必须排除的高危诊断）

**工具内部实现**：
```python
# diagnosis-tool/app/tools/diagnosis_tool.py
class DiagnosisTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取患者信息
        cdp = context.cdp
        patient_info = cdp.problem_representation
        
        # 2. 调用知识图谱工具获取推理路径
        kg_paths = await self._get_kg_paths(patient_info)
        
        # 3. 工具内部调用大模型引擎进行推理
        llm_result = await self._llm_engine_diagnose(patient_info, kg_paths)
        
        # 4. 融合多个引擎结果（包括大模型引擎）
        fused_result = await self._fuse_engines(patient_info, llm_result)
        
        # 5. 格式化为三层分层结构
        three_layer_ddx = self._format_to_three_layer(fused_result, cdp.problem_representation)
        
        # 6. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "ddx": three_layer_ddx
            },
            evidence=llm_result.evidence,
            suggestedWrites={
                "cdp.ddx.rank_list": three_layer_ddx,
                "cdp.ddx.primary_hypothesis": three_layer_ddx.get("首要假设"),
                "cdp.ddx.main_alternatives": three_layer_ddx.get("主要备选诊断"),
                "cdp.ddx.must_exclude": three_layer_ddx.get("必须排除的高危诊断")
            }
        )
    
    async def _llm_engine_diagnose(self, patient_info: Dict, kg_paths: List[Dict]) -> Dict:
        """工具内部：大模型推理"""
        # 1. 构建增强提示词（包含知识图谱推理路径）
        prompt = self._build_enhanced_prompt(patient_info, kg_paths)
        
        # 2. 工具内部调用LLM API
        llm_response = await self.llm_client.generate(prompt)
        
        # 3. 解析响应并输出三层分层结构
        result = self._parse_response(llm_response)
        
        return result
    
    def _format_to_three_layer(self, result: Dict, problem_list: Dict) -> Dict:
        """将大模型输出格式化为三层分层结构"""
        possibilities = result.get("possibilities", {})
        supporting_evidence = result.get("supporting_evidence", {})
        
        # 按概率排序
        sorted_diseases = sorted(possibilities.items(), key=lambda x: x[1], reverse=True)
        
        # 识别高危诊断（基于风险识别规则）
        high_risk_keywords = ["心肌梗死", "脑梗死", "肺栓塞", "主动脉夹层", "急性", "出血"]
        high_risk_diseases = [
            disease for disease, prob in sorted_diseases
            if any(keyword in disease for keyword in high_risk_keywords)
        ]
        
        # 构建三层结构
        primary_hypothesis = []
        main_alternatives = []
        must_exclude = []
        
        # 首要假设：排除高危诊断后，概率最高的1个
        for disease, prob in sorted_diseases:
            if disease not in high_risk_diseases and len(primary_hypothesis) == 0:
                primary_hypothesis.append({
                    "方向名称": disease,
                    "概率": prob,
                    "入选依据": self._generate_inclusion_reason(disease, supporting_evidence.get(disease, []), problem_list),
                    "当前信息缺口": result.get("missing_info", []),
                    "支持证据": supporting_evidence.get(disease, [])
                })
                break
        
        # 主要备选诊断：1-2个，排除高危诊断
        for disease, prob in sorted_diseases:
            if disease not in high_risk_diseases and len(main_alternatives) < 2:
                if not any(h["方向名称"] == disease for h in primary_hypothesis):
                    main_alternatives.append({
                        "方向名称": disease,
                        "概率": prob,
                        "入选依据": self._generate_inclusion_reason(disease, supporting_evidence.get(disease, []), problem_list),
                        "当前信息缺口": result.get("missing_info", []),
                        "支持证据": supporting_evidence.get(disease, [])
                    })
        
        # 必须排除的高危诊断：0-1个，即使概率不高也必须纳入
        for disease, prob in sorted_diseases:
            if disease in high_risk_diseases and len(must_exclude) == 0:
                must_exclude.append({
                    "方向名称": disease,
                    "概率": prob,
                    "入选依据": self._generate_inclusion_reason(disease, supporting_evidence.get(disease, []), problem_list),
                    "当前信息缺口": result.get("missing_info", []),
                    "必须排除": True,
                    "支持证据": supporting_evidence.get(disease, [])
                })
                break
        
        return {
            "首要假设": primary_hypothesis,
            "主要备选诊断": main_alternatives,
            "必须排除的高危诊断": must_exclude,
            "下一步分流入口": {
                "主要备选诊断": [alt.get("当前信息缺口", []) for alt in main_alternatives],
                "必须排除的高危诊断": [excl.get("当前信息缺口", []) for excl in must_exclude]
            }
        }
    
    def _generate_inclusion_reason(self, disease: str, evidence: List[str], problem_list: Dict) -> str:
        """生成入选依据（对应问题清单的线索）"""
        # 基于证据和问题清单生成入选依据
        main_problems = problem_list.get("主要问题", {})
        accompanying_problems = problem_list.get("伴随问题", [])
        
        reasons = []
        if main_problems:
            reasons.append(f"主诉：{main_problems.get('主诉名称', '')}")
        if evidence:
            reasons.append(f"支持证据：{', '.join(evidence[:3])}")
        
        return "；".join(reasons) if reasons else "基于症状匹配"
```

### 2.3 Step 3：组织候选集并建立分流路径中的大模型（工具内部使用，可选）

**工具**：分流路径工具（Routing Path Tool）

**工具职责**：
- 主Agent调用分流路径工具，工具内部使用大模型进行NLG
- 将第一层分叉问题转换为自然的追问表述
- 生成差异点的标准问法

**工具内部实现**：
```python
# routing-path-tool/app/tools/routing_path_tool.py
class RoutingPathTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取诊断候选和差异点
        cdp = context.cdp
        difference_point = cdp.routing_path.difference_point
        
        # 2. 工具内部使用大模型生成分流问题的自然语言表述
        question = await self._generate_routing_question_with_llm(difference_point, cdp)
        
        # 3. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "question": question
            },
            evidence=[],
            suggestedWrites={
                "cdp.routing_path.current_question": question
            }
        )
    
    async def _generate_routing_question_with_llm(self, difference_point: Dict, context: Dict) -> str:
        """工具内部：使用大模型生成分流问题的自然语言表述"""
        prompt = f"""根据以下差异点，生成一个自然的追问问题：

差异点：{difference_point.get('difference_point_name')}
标准问法模板：{difference_point.get('question_template')}
答案选项：{difference_point.get('answer_options')}

患者主诉：{context.get('chief_complaint')}
当前诊断候选：{context.get('ddx_candidates', [])}

要求：
1. 问题要自然、口语化，符合医生的问诊风格
2. 一次只问一个问题
3. 问题要简洁明了，易于理解

请生成一个追问问题：
"""
        # 调用大模型API
        # ...
```

### 2.4 Step 4：采集关键证据并形成排序与验证计划中的大模型（工具内部使用，可选）

**工具**：问诊问题生成工具（Question Generation Tool）

**工具职责**：
- 主Agent调用问诊问题生成工具，工具内部使用大模型进行NLG
- 基于临床决策分析生成自然的追问问题
- 优先生成能排除高危诊断或信息增益高的问题

**工具内部实现**：
```python
# question-generation-tool/app/tools/question_generation_tool.py
class QuestionGenerationTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取信息缺口和临床决策分析
        cdp = context.cdp
        gap = cdp.missing_info[0]  # 信息缺口
        clinical_analysis = cdp.clinical_analysis
        
        # 2. 工具内部使用大模型生成追问问题
        question = await self._generate_question_with_clinical_analysis(
            gap, clinical_analysis, cdp
        )
        
        # 3. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "question": question
            },
            evidence=[],
            suggestedWrites={
                "cdp.current_question": question
            }
        )
    
    async def _generate_question_with_clinical_analysis(self, 
                                                        gap: InformationGap,
                                                        clinical_analysis: Dict,
                                                        context: Dict) -> str:
        """工具内部：使用大模型生成追问问题（基于临床决策分析）"""
        prompt = f"""根据以下信息，生成一个自然的追问问题：

需要收集的信息：{gap.info_type}
信息增益：{clinical_analysis.get('information_gain', 0)}
风险增益：{clinical_analysis.get('risk_gain', 0)}  # 能排除多少高危诊断
成本效益：{clinical_analysis.get('cost_benefit', 0)}

诊断候选：
首要假设：{context.get('primary_hypothesis', {})}
主要备选诊断：{context.get('main_alternatives', [])}
必须排除的高危诊断：{context.get('must_exclude', [])}

要求：
1. 优先问能排除高危诊断的问题（风险优先）
2. 问题要自然、口语化，符合医生的问诊风格
3. 一次只问一个问题
4. 不要询问已经回答过的问题

请生成一个追问问题：
"""
        # 调用大模型API
        # ...
```

### 2.5 Step 5：回填证据并输出终点结论包中的大模型（工具内部使用，核心）

**工具**：解释生成工具（Explanation Tool）

**工具职责**：
- 主Agent调用解释生成工具，工具内部使用大模型进行NLG
- 重新评估诊断（可选）：基于新证据重新评估诊断可能性
- 生成终点结论包的自然语言说明（核心）
- 生成终点结论包四要素的详细说明（结论、必须排除项状态、关键依据、行动与随访）

**工具内部实现**：
```python
# explanation-tool/app/tools/explanation_tool.py
class ExplanationTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 1. 从CDP读取终点结论包
        cdp = context.cdp
        conclusion_package = cdp.conclusion_package
        
        # 2. 工具内部使用大模型生成终点结论包的自然语言说明
        explanation = await self._generate_conclusion_package_explanation(cdp)
        
        # 3. 返回ToolResult
        return ToolResult(
            status="success",
            payload={
                "explanation": explanation
            },
            evidence=[],
            suggestedWrites={
                "cdp.final.explanation": explanation
            }
        )
    
    async def _generate_conclusion_package_explanation(self, cdp: CDP) -> str:
        """工具内部：使用大模型生成终点结论包的自然语言说明（四要素）"""
        conclusion_pkg = cdp.conclusion_package
        
        prompt = f"""你是一位经验丰富的医生，需要根据以下诊断结果，生成一段自然、友好、专业的解释：

终点结论包（四要素）：

1. 结论：
类型：{conclusion_pkg.conclusion.get('type')}
诊断：{conclusion_pkg.conclusion.get('diagnosis') or conclusion_pkg.conclusion.get('most_likely_direction')}
置信度：{conclusion_pkg.conclusion.get('confidence')}
严重程度：{conclusion_pkg.conclusion.get('severity')}
不确定性来源：{conclusion_pkg.conclusion.get('uncertainty_source')}

2. 必须排除项状态：
已排除：{conclusion_pkg.must_exclude_status.get('excluded', [])}
未排除：{conclusion_pkg.must_exclude_status.get('not_excluded', [])}
需线下排除：{conclusion_pkg.must_exclude_status.get('need_offline_exclude', [])}

3. 关键依据（至少三条证据）：
阳性证据：{conclusion_pkg.key_evidence.get('positive_evidence', [])}
阴性证据：{conclusion_pkg.key_evidence.get('negative_evidence', [])}
检查结果：{conclusion_pkg.key_evidence.get('check_results', [])}

4. 行动与随访：
立即行动：
- 就医建议：{conclusion_pkg.action_and_followup.get('immediate_action', {}).get('medical_advice')}
- 检查建议：{conclusion_pkg.action_and_followup.get('immediate_action', {}).get('examinations', [])}
- 治疗方向：{conclusion_pkg.action_and_followup.get('immediate_action', {}).get('treatment_direction')}
- 紧急情况：{conclusion_pkg.action_and_followup.get('immediate_action', {}).get('urgent_conditions')}

复评时间窗：
- 默认复评时间：{conclusion_pkg.action_and_followup.get('review_time_window', {}).get('default_time')}
- 提前复评条件：{conclusion_pkg.action_and_followup.get('review_time_window', {}).get('early_review_conditions', [])}

升级触发条件：{conclusion_pkg.action_and_followup.get('upgrade_conditions', [])}

请生成一段自然、友好、专业的解释，包括：
1. 诊断结论（使用"可能"、"考虑"等表述，不要给出确诊）
2. 诊断依据（简要说明支持证据，至少三条）
3. 必须排除项的状态说明（是否已排除高危诊断）
4. 建议和指导（检查建议、就医建议等）
5. 复评和随访安排
6. 重要提醒（本分析仅供参考，不替代医生诊断）

要求：
- 语言自然、口语化，易于理解
- 符合医生的沟通风格
- 不要给出绝对性的结论
- 强调需要进一步检查或就医
- 明确说明必须排除项的状态（特别是高危诊断）
"""
        # 调用大模型API
        llm_response = await self._call_llm(prompt)
        return llm_response
    
    async def generate_conclusion_element_explanation(self, element: str, content: Dict) -> str:
        """使用大模型生成单个要素的自然语言说明"""
        element_prompts = {
            "结论": """生成诊断结论的解释，包括：
- 最可能的诊断方向（使用"可能"、"考虑"等表述）
- 诊断的置信度和严重程度
- 不确定性来源（如果不可确证）""",
            
            "必须排除项状态": """生成必须排除项状态的说明，包括：
- 已排除的高危诊断及排除理由
- 未排除的高危诊断及原因
- 需线下排除的高危诊断及排除建议""",
            
            "关键依据": """生成关键依据的说明，包括：
- 至少三条支持证据（阳性证据）
- 关键阴性证据（排除其他诊断的证据）
- 检查结果（如果有）""",
            
            "行动与随访": """生成行动与随访的说明，包括：
- 立即行动建议（就医、检查、治疗方向）
- 复评时间窗和条件
- 升级触发条件"""
        }
        
        prompt = f"""根据以下{element}信息，生成一段自然、友好的解释：

{json.dumps(content, ensure_ascii=False, indent=2)}

{element_prompts.get(element, "生成自然语言解释")}

要求：
- 语言自然、口语化
- 易于理解
- 符合医生沟通风格
"""
        # 调用大模型API
        llm_response = await self._call_llm(prompt)
        return llm_response
```

---

## 三、大模型在流程回退机制中的作用

### 3.0 回退机制概述

**医疗理论依据**：
- **假设-验证模型**（Hypothesis-Testing Model）- Elstein的临床推理模型
- **诊断修正理论**（Diagnostic Revision Theory）
- **治疗性诊断**（Therapeutic Diagnosis）

**核心思想**：临床推理是一个动态过程，诊断假设应该能够根据新证据动态调整。当出现证据冲突、症状演变或处理无效时，主Agent需要回退到之前的步骤，重新调用工具进行推理。

**架构定位**：主Agent在回退时重新调用工具，工具内部使用大模型进行重新评估。

### 3.1 回退触发条件与大模型角色（工具内部使用）

#### 回退到 Step 4（采集关键证据并形成排序与验证计划）

**触发条件**：
- 假设验证失败（证据冲突）
- 治疗无效
- 必须排除项未完成排除

**工具**：诊断工具（Diagnosis Tool）

**工具内部实现**：
```python
# diagnosis-tool/app/tools/diagnosis_tool.py
class DiagnosisTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 主Agent在回退时重新调用诊断工具
        # 工具内部使用大模型重新评估诊断
        cdp = context.cdp
        old_result = cdp.ddx
        new_evidence = cdp.new_evidence
        
        # 工具内部使用大模型重新评估
        reassessed_result = await self._reassess_with_new_evidence(
            old_result, new_evidence, cdp
        )
        
        return ToolResult(
            status="success",
            payload={"ddx": reassessed_result},
            evidence=reassessed_result.evidence,
            suggestedWrites={"cdp.ddx": reassessed_result}
        )
    
    async def _reassess_with_new_evidence(self, 
                                        old_result: Dict, 
                                        new_evidence: List[Evidence],
                                        cdp: CDP) -> Dict:
        """工具内部：基于新证据重新评估诊断（回退到Step 4时）"""
        prompt = f"""之前的诊断结果与新的证据存在冲突或矛盾，需要重新评估：

之前的诊断结果：
{json.dumps(old_result, ensure_ascii=False, indent=2)}

新的证据：
{json.dumps([ev.dict() for ev in new_evidence], ensure_ascii=False, indent=2)}

问题清单：
{json.dumps(cdp.patient_state.problem_list, ensure_ascii=False, indent=2)}

请重新分析：
1. 新证据是否支持之前的诊断
2. 是否存在证据冲突
3. 是否需要调整诊断概率
4. 是否需要重新生成三层分层结构

如果存在证据冲突，请说明冲突类型和原因。

返回：
{{
    "revised_possibilities": {{"疾病1": 0.7, "疾病2": 0.5}},
    "evidence_conflicts": ["冲突描述"],
    "revision_reason": "重新评估的原因",
    "updated_missing_info": ["更新的缺失信息列表"]
}}
"""
        # 调用大模型API
        # ...
```

#### 回退到 Step 3（组织候选集并建立分流路径）

**触发条件**：
- 症状演变
- 新症状出现
- 新红旗信号

**工具**：分流路径工具（Routing Path Tool）

**工具内部实现**：
```python
# routing-path-tool/app/tools/routing_path_tool.py
class RoutingPathTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 主Agent在回退时重新调用分流路径工具
        cdp = context.cdp
        symptom_changes = cdp.symptom_changes
        old_routing_path = cdp.routing_path
        
        # 工具内部使用大模型重新组织分流路径
        updated_path = await self._reorganize_routing_path(
            symptom_changes, old_routing_path, cdp
        )
        
        return ToolResult(
            status="success",
            payload={"routing_path": updated_path},
            evidence=[],
            suggestedWrites={"cdp.routing_path": updated_path}
        )
    
    async def _reorganize_routing_path(self, 
                                     symptom_changes: Dict,
                                     old_routing_path: Dict,
                                     cdp: CDP) -> Dict:
        """工具内部：重新组织分流路径（回退到Step 3时）"""
        prompt = f"""症状发生变化，需要重新组织分流路径：

症状变化：
{json.dumps(symptom_changes, ensure_ascii=False, indent=2)}

之前的分流路径：
{json.dumps(old_routing_path, ensure_ascii=False, indent=2)}

当前诊断候选：
{json.dumps(cdp.ddx, ensure_ascii=False, indent=2)}

请重新分析：
1. 症状变化是否影响诊断方向
2. 是否需要重新组织推理子组
3. 是否需要生成新的分流路径

返回：
{{
    "updated_routing_path": {{}},
    "new_key_differences": ["新的关键差异点"],
    "reorganization_reason": "重新组织的原因"
}}
"""
        # 调用大模型API
        # ...
```

#### 回退到 Step 1（识别问题）

**触发条件**：
- 信息冲突
- 信息不完整
- 关键信息缺失

**工具**：问题识别工具（Problem Identification Tool）

**工具内部实现**：
```python
# problem-identification-tool/app/tools/problem_identification_tool.py
class ProblemIdentificationTool:
    async def execute(self, context: ToolContext) -> ToolResult:
        # 主Agent在回退时重新调用问题识别工具
        cdp = context.cdp
        conflict_info = cdp.conflict_info
        old_problem_list = cdp.problem_representation
        
        # 工具内部使用大模型重新澄清问题
        revised_problem_list = await self._reclarify_problem_with_llm(
            conflict_info, old_problem_list
        )
        
        return ToolResult(
            status="success",
            payload={"problem_list": revised_problem_list},
            evidence=[],
            suggestedWrites={"cdp.problem_representation": revised_problem_list}
        )
    
    async def _reclarify_problem_with_llm(self, 
                                        conflict_info: Dict,
                                        old_problem_list: Dict) -> Dict:
        """工具内部：重新澄清问题（回退到Step 1时）"""
        prompt = f"""检测到信息冲突或不完整，需要重新澄清问题：

信息冲突：
{json.dumps(conflict_info, ensure_ascii=False, indent=2)}

之前的问题清单：
{json.dumps(old_problem_list, ensure_ascii=False, indent=2)}

请：
1. 识别信息冲突的具体内容
2. 生成澄清问题（用于解决冲突）
3. 重新生成结构化问题清单

返回：
{{
    "conflicts": ["冲突1", "冲突2"],
    "clarification_questions": ["澄清问题1", "澄清问题2"],
    "revised_problem_list": {{}}
}}
"""
        # 调用大模型API
        # ...
```

### 3.2 回退机制中的约束原则

**大模型在回退机制中必须遵循的原则**：

1. **假设可修正性**：诊断假设应该能够根据新证据动态调整
   - 医疗依据：Elstein的假设-验证模型
   - 保证：避免诊断锁定（Diagnostic Lock-in）

2. **证据优先性**：新证据优先于旧假设
   - 医疗依据：循证医学原则
   - 保证：当新证据与旧假设冲突时，优先相信新证据

3. **风险优先性**：高危诊断优先处理
   - 医疗依据：风险优先原则（Risk-First Principle）
   - 保证：漏诊高危疾病的代价极大，必须优先排除

4. **可追溯性**：记录回退原因和动作
   - 医疗依据：临床决策的可审计性
   - 保证：便于审计和验证，支持临床决策的透明度

---

## 四、大模型的作用方式

### 4.1 通道1：结构化推理通道中的大模型

#### 4.1.1 知识图谱路径注入（DR.KNOWS核心方法）

**核心思想**：将知识图谱推理路径注入到大模型提示词中，约束大模型的推理方向。路径评分基于**贝叶斯诊断理论**的三层评分体系。

**流程**：
```
1. 知识图谱引擎（KG Engine）检索推理路径
   ↓
2. 对路径进行三层评分排序（基于贝叶斯诊断理论）
   - 层1：先验概率评分（Prior Probability Score）
   - 层2：似然评分（Likelihood Score）
   - 层3：后验概率评分（Posterior Probability Score）
   ↓
3. 构建增强提示词（包含推理路径及评分）
   ↓
4. 大模型基于路径约束进行推理
   ↓
5. 输出结构化JSON结果（三层分层结构）
```

**三层评分体系说明**：

**医疗理论依据**：贝叶斯诊断理论（Bayesian Diagnostic Theory）

**层1：先验概率评分**（Prior Probability Score）
- **依据**：基于疾病患病率，常见病优先考虑（Occam's Razor in Medicine）
- **作用**：告诉大模型哪些疾病更常见，应该优先考虑

**层2：似然评分**（Likelihood Score）
- **依据**：基于证据的敏感性/特异性，诊断试验的准确性
- **作用**：告诉大模型哪些证据对哪些疾病更有诊断价值

**层3：后验概率评分**（Posterior Probability Score）
- **依据**：贝叶斯定理 P(D|E) = (P(E|D) · P(D)) / P(E)
- **作用**：告诉大模型给定证据下疾病的最优诊断概率

**路径格式化示例**：
```
推理路径1（相关性评分：0.85，后验概率：0.75）：
症状：胸痛 --[引起]--> 疾病：急性心肌梗死
  支持证据：ST段抬高、心肌酶升高
  先验概率：0.10（中等常见）
  似然评分：0.95（高特异性）
  后验概率：0.75（综合评分）

推理路径2（相关性评分：0.72，后验概率：0.65）：
症状：胸痛 --[引起]--> 疾病：心绞痛
  支持证据：活动后加重、休息缓解
  先验概率：0.30（较常见）
  似然评分：0.80（中等特异性）
  后验概率：0.65（综合评分）
```

**工具内部实现**：
```python
# diagnosis-tool/app/kg-reasoning-engine/kg_reasoning_engine.py
class KGReasoningEngine:
    def build_enhanced_prompt(self, patient_info: str, ranked_paths: List[RankedPath], problem_list: Dict) -> str:
        """路径注入LLM（DR.KNOWS核心方法，基于贝叶斯诊断理论）"""
        paths_text = self._format_paths_for_llm(ranked_paths)
        
        prompt = f"""你是一位经验丰富的医生，需要根据患者的症状、体征、健康档案等信息，
结合以下医学推理路径（来自知识图谱，基于贝叶斯诊断理论评分排序），分析可能的疾病方向。

患者信息：
{patient_info}

问题清单（结构化）：
主要问题：{problem_list.get('主要问题', {})}
伴随问题：{problem_list.get('伴随问题', [])}
关键背景：{problem_list.get('关键背景', {})}

医学推理路径（来自知识图谱，基于贝叶斯诊断理论三层评分排序）：
{paths_text}

请根据以上医学推理路径和贝叶斯诊断理论，分析：
1. 生成鉴别诊断全集（覆盖：常见原因、易混淆原因、严重但不能漏诊的原因）
2. 为每个候选方向标注"入选依据"（来自问题清单的哪些线索）
3. 进行三层分层：
   - 首要假设（1个）：当前信息最能支持、最符合整体表现的方向
   - 主要备选诊断（1-2个）：与首要假设并列需要对比、仍可能成立的方向
   - 必须排除的高危诊断（0-1个）：一旦漏诊后果严重，即使概率不高也必须纳入并优先排除
4. 每个方向的支持证据（症状、体征、检查结果）
5. 每个方向的反对证据
6. 每个方向的当前信息缺口（缺什么会导致方向无法推进）

注意：
- 优先考虑推理路径中提到的疾病方向（特别是后验概率高的路径）
- 必须排除的高危诊断即使概率低也要纳入（如：急性心肌梗死、肺栓塞等）
- 不要给出确诊结论，使用"可能"、"考虑"等表述
- 如果信息不足，明确说明
- 每个方向必须写清楚是由问题清单中的哪条线索触发纳入（可审计）

请以JSON格式返回：
{{
    "possibilities": {{
        "疾病1": 0.8,
        "疾病2": 0.6,
        "疾病3": 0.4
    }},
    "supporting_evidence": {{
        "疾病1": ["症状1", "体征1"],
        "疾病2": ["症状2", "体征2"]
    }},
    "opposing_evidence": {{
        "疾病1": ["症状3"]
    }},
    "inclusion_reasons": {{
        "疾病1": "来自问题清单的线索：主诉XXX + 伴随症状XXX",
        "疾病2": "来自问题清单的线索：关键背景XXX"
    }},
    "missing_info": {{
        "疾病1": ["缺失信息1", "缺失信息2"],
        "疾病2": ["缺失信息3"]
    }},
    "reasoning_paths_used": ["路径1", "路径2"],
    "recommended_tests": ["检查1", "检查2"],
    "high_risk_diseases": ["必须排除的高危诊断列表"]
}}
"""
        return prompt
    
    def _format_paths_for_llm(self, ranked_paths: List[RankedPath]) -> str:
        """将路径格式化为自然语言描述（包含三层评分）"""
        path_descriptions = []
        
        for ranked_path in ranked_paths[:10]:  # Top-10路径
            path = ranked_path.path
            description = f"""推理路径（相关性评分：{ranked_path.composite_score:.2f}）：
路径：{' --> '.join([node.name for node in path.nodes])}
目标疾病：{path.target_disease.name}

贝叶斯诊断理论评分：
- 先验概率评分：{ranked_path.prior_score:.2f}（基于疾病患病率）
- 似然评分：{ranked_path.likelihood_score:.2f}（基于证据敏感性/特异性）
- 后验概率评分：{ranked_path.posterior_score:.2f}（综合诊断概率）

支持证据：{', '.join([ev.name for ev in path.evidence])}
"""
            path_descriptions.append(description)
        
        return "\n\n".join(path_descriptions)
```

#### 4.1.2 提示词构建流程

**工具内部实现**（基础版本）：
```python
# diagnosis-tool/app/engines/llm_engine.py
def _build_prompt(self, request: DiagnosisEngineRequest) -> str:
    """构建提示词"""
    health_profile = request.health_profile or {}
    symptom_info = request.symptom_info or {}
    vital_signs = request.vital_signs or {}
    examination_results = request.examination_results or []
    
    return f"""你是一位经验丰富的医生，需要根据患者的症状、体征、健康档案等信息，
分析可能的疾病方向。

患者信息：
- 年龄：{health_profile.get('age', '未知')}
- 性别：{health_profile.get('gender', '未知')}
- 既往史：{health_profile.get('medical_history', '无')}
- 当前症状：{symptom_info}
- 生命体征：{vital_signs}
- 检查结果：{examination_results}

请分析：
1. 可能的疾病方向（Top 3-5），按可能性排序
2. 每个方向的支持证据（症状、体征、检查结果）
3. 每个方向的反对证据
4. 还需要哪些信息来进一步判断
5. 建议做哪些检查来辅助诊断

注意：
- 不要给出确诊结论
- 使用"可能"、"考虑"等表述
- 如果信息不足，明确说明

请以JSON格式返回：
{{
    "possibilities": {{
        "疾病1": 0.8,
        "疾病2": 0.6,
        "疾病3": 0.4
    }},
    "supporting_evidence": {{
        "疾病1": ["症状1", "体征1"]
    }},
    "opposing_evidence": {{
        "疾病1": ["症状3"]
    }},
    "missing_info": ["检查X", "症状Y"],
    "recommended_tests": ["检查1", "检查2"]
}}
"""
```

**增强版本**（需要集成知识图谱路径）：
```python
def _build_prompt(self, request: DiagnosisEngineRequest, kg_paths: List[Dict] = None) -> str:
    """构建增强提示词（包含知识图谱推理路径）"""
    # 基础患者信息
    base_prompt = self._build_base_prompt(request)
    
    # 如果提供了知识图谱路径，注入路径
    if kg_paths:
        paths_text = self._format_paths_for_llm(kg_paths)
        enhanced_prompt = f"""{base_prompt}

医学推理路径（来自知识图谱，按相关性排序）：
{paths_text}

请根据以上医学推理路径，分析：
- 优先考虑推理路径中提到的疾病方向
- 如果推理路径与患者情况不符，请说明原因
"""
        return enhanced_prompt
    
    return base_prompt
```

#### 4.1.3 输出解析

**工具内部实现**：
```python
# diagnosis-tool/app/engines/llm_engine.py
def _parse_response(self, response: str) -> Dict:
    """解析大模型返回结果"""
    import json
    import re
    
    # 尝试提取JSON
    json_match = re.search(r'\{.*\}', response, re.DOTALL)
    if json_match:
        try:
            result = json.loads(json_match.group())
            return {
                'possibilities': result.get('possibilities', {}),
                'supporting_evidence': result.get('supporting_evidence', {}),
                'opposing_evidence': result.get('opposing_evidence', {}),
                'missing_info': result.get('missing_info', []),
                'recommended_tests': result.get('recommended_tests', []),
                'engine_type': 'llm'
            }
        except Exception as e:
            logger.error(f"解析LLM响应失败: {str(e)}")
    
    # 如果解析失败，返回空结果
    return {"possibilities": {}, "engine_type": "llm", "error": "解析失败"}
```

### 4.2 通道2：语言与策略通道中的大模型

#### 4.2.1 自然语言解释生成

**作用**：将结构化的诊断结果转换为用户友好的自然语言。

**输入**：
- 结构化的诊断结果（CDP）
- 证据链
- 推理路径
- 终点结论包

**输出**：
- 人性化的自然语言解释
- 诊断依据说明
- 建议和指导

**工具内部实现**：
```python
# explanation-tool/app/tools/explanation_tool.py
class ExplanationTool:
    async def _llm_generate_explanation(
    self, 
    conclusion_package: ConclusionPackage,
    cdp: Dict
) -> str:
    """使用大模型生成自然语言解释"""
    prompt = f"""你是一位经验丰富的医生，需要将以下结构化的诊断结果转换为自然语言，
向患者解释诊断依据、建议和指导。

诊断结果：
{json.dumps(conclusion_package.dict(), ensure_ascii=False, indent=2)}

证据链：
{json.dumps(cdp.get('evidence_graph', {}), ensure_ascii=False, indent=2)}

请生成一段自然、友好、专业的解释，包括：
1. 诊断结论（使用"可能"、"考虑"等表述，不要给出确诊）
2. 诊断依据（简要说明支持证据）
3. 建议和指导（检查建议、就医建议等）
4. 重要提醒（本分析仅供参考，不替代医生诊断）

要求：
- 语言自然、口语化，易于理解
- 符合医生的沟通风格
- 不要给出绝对性的结论
- 强调需要进一步检查或就医
"""
    
    # 调用大模型API
    llm_response = await self._call_llm(prompt)
    return llm_response
```

#### 4.2.2 追问问题生成（可选增强）

**当前实现**：使用模板生成问题

**工具内部实现**（可选增强）：
```python
# question-generation-tool/app/tools/question_generation_tool.py
class QuestionGenerationTool:
    async def _generate_question_with_llm(self, question_info: Dict, context: Dict) -> str:
    """使用大模型生成自然的追问问题"""
    prompt = f"""你是一位经验丰富的医生，需要根据以下信息生成一个追问问题。

需要收集的信息：
{question_info.get('missing_info', [])}

对话上下文：
- 主诉：{context.get('chief_complaint')}
- 已收集信息：{context.get('collected_info', {})}
- 诊断候选：{context.get('ddx_candidates', [])}

要求：
1. 问题要自然、口语化，符合医生的问诊风格
2. 不要询问已经回答过的问题
3. 根据诊断候选，重点询问能区分不同疾病的关键信息
4. 问题要简洁明了，一次只问一个问题

请生成一个追问问题：
"""
    
    # 调用大模型API
    llm_response = await self._call_llm(prompt)
    return llm_response
```

---

## 六、大模型的约束机制：工具内部如何"管住"大模型？

### 6.1 约束机制的总体思路：四道"安全防线"

工具内部使用**四道防线**来约束大模型，就像给车辆加装多重安全保障：

**架构定位**：
- 约束机制在工具内部实施，主Agent不直接约束大模型
- 工具负责确保大模型输出的质量和安全性
- 工具返回给主Agent的结果已经经过约束和验证

```
┌─────────────────────────────────────────────────────────┐
│                    第一道防线：输入约束                      │
│  "给它正确的信息，引导它往正确的方向思考"                    │
│  - 提供标准化的医学概念（而不是口语化表达）                  │
│  - 提供医学推理路径（告诉它"应该往哪想"）                    │
│  - 明确告诉它要做什么、不要做什么                            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    第二道防线：输出约束                      │
│  "检查它输出的内容是否合理"                                  │
│  - 检查输出格式是否正确（能否被程序处理）                    │
│  - 检查概率值是否在合理范围内（0-1之间）                     │
│  - 检查疾病名称是否真实存在                                  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    第三道防线：过程约束                      │
│  "不让它一家独大，与其他方法协同工作"                        │
│  - 大模型的结果只占25%权重（还有其他4个引擎）                 │
│  - 与其他引擎的结果对比，发现异常就降低它的权重               │
│  - 要求它说明推理过程（用了哪些医学路径）                     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    第四道防线：参数约束                      │
│  "控制它的行为模式"                                          │
│  - 降低随机性（让答案更稳定）                                │
│  - 限制输出长度（避免说太多无关内容）                        │
│  - 设置超时时间（避免等待太久）                              │
└─────────────────────────────────────────────────────────┘
```

### 6.2 第一道防线：输入约束（"给它正确的问题和提示"）

**核心思想**：就像考试时要给考生清晰的题目和答题要求，我们要给大模型提供标准化的输入和明确的指令。

#### 6.2.1 方法1：知识图谱路径注入（"给它一个推理路线图"）

**通俗解释**：
- **问题**：大模型可能"胡思乱想"，想出一些不相关的疾病
- **解决**：我们先通过知识图谱找到最相关的医学推理路径，然后告诉大模型："请沿着这些路径思考"
- **类比**：就像给导游一张地图，告诉他应该带游客走哪些路线

**实际效果**：
- ✅ 大模型会优先考虑我们提供的疾病方向
- ✅ 避免它"天马行空"想到不相关的疾病
- ✅ 提高诊断的准确性和相关性

**举例说明**：
```
患者症状：胸痛、气短、出汗

❌ 不给路径约束时，大模型可能想到：
   - 急性心肌梗死 ✓（正确）
   - 心绞痛 ✓（正确）
   - 焦虑症 ✓（可能）
   - 胃病 ✗（不太相关）
   - 感冒 ✗（完全不相关）

✅ 给路径约束后，大模型会优先考虑：
   - 急性心肌梗死 ✓（知识图谱路径1，相关性0.85）
   - 心绞痛 ✓（知识图谱路径2，相关性0.75）
   - 焦虑症 ✓（如果症状支持）
```

#### 6.2.2 方法2：结构化患者信息（"用标准医学术语，避免歧义"）

**通俗解释**：
- **问题**：患者说"胸口疼"，这可能指胸痛、心前区不适、胸闷等多种情况
- **解决**：先把患者的口语化表达转换成标准医学术语（CUI/ICD编码），再给大模型
- **类比**：就像把方言翻译成普通话，确保理解准确

**实际效果**：
- ✅ 大模型接收到的是精确的医学概念，不是模糊的描述
- ✅ 避免因为表达歧义导致的误诊
- ✅ 提高诊断的一致性

**举例说明**：
```
❌ 口语化输入：
   "我胸口疼，喘不过气"

✅ 标准化后输入：
   - 症状1：胸痛（CUI: C0008031）
   - 症状2：气短（CUI: C0030193）
   - 严重程度：中等（6分）
   - 持续时间：3天
```

#### 6.2.3 方法3：提示词模板约束（"明确告诉它要做什么、不要做什么"）

**通俗解释**：
- **问题**：大模型可能给出绝对性的诊断结论，或者输出格式不对
- **解决**：在提问时明确告诉它：
  - ✅ 要做的事：分析疾病可能性、提供支持证据和反对证据
  - ❌ 不要做的事：不要给出确诊结论、不要使用绝对性表述
  - 📋 输出格式：必须以JSON格式返回，包含指定字段
- **类比**：就像给员工一份详细的工作说明，告诉他应该做什么、不应该做什么、结果应该是什么格式

**实际效果**：
- ✅ 大模型的输出更符合我们的要求
- ✅ 输出格式标准化，便于后续处理
- ✅ 避免给出不安全的绝对性结论

**举例说明**：
```
我们在提示词中明确说明：

"请根据以上医学推理路径，分析：
1. 可能的疾病方向（Top 3-5），按可能性排序
2. 每个方向的支持证据
3. 每个方向的反对证据

注意：
- 不要给出确诊结论
- 使用'可能'、'考虑'等表述
- 如果信息不足，明确说明

请以JSON格式返回：{...}"
```

---

### 6.3 第二道防线：输出约束（"检查它输出的内容"）

**核心思想**：就像质检员检查产品质量，我们要检查大模型输出的内容是否正确、完整、合理。

#### 6.3.1 方法1：结构化输出格式（"要求它按固定格式输出"）

**通俗解释**：
- **问题**：大模型可能自由发挥，输出一段文字，难以被程序处理
- **解决**：要求它必须以JSON格式输出，包含固定的字段（如疾病名称、可能性评分等）
- **类比**：就像要求员工填写标准表格，而不是写一段自由文字

**实际效果**：
- ✅ 输出格式统一，程序可以自动处理
- ✅ 便于后续的验证和分析
- ✅ 减少人工干预

#### 6.3.2 方法2：输出解析与验证（"检查内容是否完整和正确"）

**通俗解释**：
- **问题**：大模型可能漏掉某些字段，或者输出格式不对
- **解决**：我们编写程序自动检查：
  - 是否包含所有必要字段？
  - 概率值是否在0-1范围内？
  - JSON格式是否正确？
- **类比**：就像表单验证，检查必填项是否都填了，数字是否在合理范围内

**实际效果**：
- ✅ 及时发现格式错误或内容缺失
- ✅ 自动修正明显的错误（如概率值超出范围）
- ✅ 如果错误严重，返回空结果，不影响其他引擎

**举例说明**：
```
大模型输出：
{
  "possibilities": {
    "急性心肌梗死": 1.5,  // ❌ 超出范围（应该是0-1）
    "心绞痛": 0.7         // ✓ 正常
  }
}

我们的验证程序会自动修正：
{
  "possibilities": {
    "急性心肌梗死": 1.0,  // ✅ 自动限制在最大值1.0
    "心绞痛": 0.7         // ✓ 保持不变
  }
}
```

#### 6.3.3 方法3：输出内容校验（"检查疾病名称是否真实存在"）

**通俗解释**：
- **问题**：大模型可能编造一个不存在的疾病名称
- **解决**：将输出的疾病名称与我们的疾病知识库比对，过滤掉不存在的疾病
- **类比**：就像检查身份证号码是否真实有效

**实际效果**：
- ✅ 过滤掉明显的错误输出（如编造的疾病名称）
- ✅ 提高诊断结果的可靠性
- ⚠️ 注意：这个方法需要完整的疾病知识库支持

---

### 6.4 第三道防线：过程约束（"不让它一家独大"）

**核心思想**：就像投资要分散风险，不能只依赖一种方法，我们让大模型与其他引擎协同工作，互相验证。

#### 6.4.1 方法1：多引擎融合约束（"加权投票"）

**通俗解释**：
- **问题**：如果完全相信大模型，它一旦出错，整个系统就错了
- **解决**：工具内部同时使用5个不同的诊断方法（引擎），每个方法的意见都有权重，最后综合投票
  - 规则引擎：25%（基于医学规则的判断）
  - 知识图谱引擎：25%（基于知识图谱的推理）
  - 统计模型引擎：20%（基于历史数据的预测）
  - **大模型引擎：25%**（基于大模型的深度推理，工具内部使用）⭐
  - 鉴别诊断引擎：5%（区分相似疾病）
- **类比**：就像专家会诊，5位专家各自给出意见，最后综合讨论决定
- **架构定位**：融合在工具内部完成，工具返回融合后的结果给主Agent

**实际效果**：
- ✅ 即使大模型出错，还有其他4个引擎的结果
- ✅ 大模型的错误会被其他引擎的结果"稀释"
- ✅ 最终结果更加稳定可靠

**举例说明**：
```
假设5个引擎对"急性心肌梗死"的评分：

规则引擎：        0.9（90%可能性）
知识图谱引擎：    0.85（85%可能性）
统计模型引擎：    0.75（75%可能性）
大模型引擎：      0.95（95%可能性） ← 可能偏高
鉴别诊断引擎：    0.80（80%可能性）

最终融合结果：
(0.9×25% + 0.85×25% + 0.75×20% + 0.95×25% + 0.80×5%) 
= 0.85（85%可能性）

✅ 即使大模型给了0.95的高分，最终结果也只是0.85，不会过于激进
```

#### 6.4.2 方法2：引擎结果验证（"发现异常就降低权重"）

**通俗解释**：
- **问题**：如果大模型的结果与其他引擎差异太大，说明它可能出错了
- **解决**：我们比较大模型的结果与其他引擎的结果，如果差异超过阈值，就降低大模型的权重（从25%降到10%）
- **类比**：就像评委打分，如果某个评委给分与其他人差距太大，就降低他的权重

**实际效果**：
- ✅ 及时发现大模型的异常输出
- ✅ 自动降低异常输出的影响
- ✅ 提高系统的鲁棒性（容错能力）

**举例说明**：
```
场景1：大模型结果正常
规则引擎Top疾病：急性心肌梗死（0.9）
知识图谱引擎Top疾病：急性心肌梗死（0.85）
大模型引擎Top疾病：急性心肌梗死（0.8）
→ 结果一致，权重保持25% ✅

场景2：大模型结果异常
规则引擎Top疾病：急性心肌梗死（0.9）
知识图谱引擎Top疾病：急性心肌梗死（0.85）
大模型引擎Top疾病：胃病（0.95） ← 明显异常
→ 结果差异大，降低大模型权重到10% ⚠️
```

#### 6.4.3 方法3：推理路径追踪（"要求它说明推理过程"）

**通俗解释**：
- **问题**：不知道大模型是怎么得出某个结论的，难以验证和追溯
- **解决**：要求大模型在输出中说明它使用了哪些医学推理路径
- **类比**：就像要求学生写出解题过程，而不是只给答案

**实际效果**：
- ✅ 可以验证大模型的推理是否遵循了我们提供的路径
- ✅ 便于后续分析和优化
- ✅ 提高诊断结果的可追溯性

---

### 6.5 第四道防线：参数约束（"控制它的行为模式"）

**核心思想**：通过调整大模型的参数，控制它的行为，让它更稳定、更可控。

#### 6.5.1 方法1：Temperature参数控制（"降低随机性"）

**通俗解释**：
- **问题**：大模型的输出有一定随机性，相同问题可能给出不同答案
- **解决**：将Temperature参数设置为0.3（较低值），降低输出的随机性
- **类比**：就像调低音乐的随机播放概率，让它更按顺序播放

**实际效果**：
- ✅ 相同输入下，输出更加一致和稳定
- ✅ 提高诊断结果的可重复性

**参数说明**：
- Temperature = 1.0：完全随机，每次回答都可能不同
- Temperature = 0.5：中等随机性
- **Temperature = 0.3**：较低随机性，答案更稳定 ✅

#### 6.5.2 方法2：最大生成长度限制（"避免说太多无关内容"）

**通俗解释**：
- **问题**：大模型可能说太多话，包含很多无关信息
- **解决**：限制最大输出长度为500个词，避免生成过长内容
- **类比**：就像限制发言时间，避免说得太长

**实际效果**：
- ✅ 输出更简洁，聚焦核心内容
- ✅ 节省处理时间和成本

#### 6.5.3 方法3：超时控制（"避免等待太久"）

**通俗解释**：
- **问题**：大模型可能响应很慢，让用户等很久
- **解决**：设置10秒超时，如果超时就返回错误，不影响其他引擎
- **类比**：就像考试时间限制，时间到了就交卷

**实际效果**：
- ✅ 系统响应更快，用户体验更好
- ✅ 即使大模型出问题，其他引擎仍能正常工作

---

### 6.6 约束机制的效果总结

通过以上**四道防线**，我们实现了：

| 约束类型 | 解决的问题 | 实际效果 |
|---------|-----------|---------|
| **输入约束** | 大模型可能"胡思乱想" | ✅ 引导它沿着正确的医学路径思考 |
| **输出约束** | 大模型输出格式不对或内容错误 | ✅ 自动检查并修正错误输出 |
| **过程约束** | 大模型一家独大，出错影响大 | ✅ 与其他引擎协同，降低单点故障风险 |
| **参数约束** | 大模型输出不稳定、不可控 | ✅ 让输出更稳定、更可控 |

**总体效果**：
- 🎯 **准确性提升**：通过路径约束和多引擎融合，减少错误诊断
- 🔒 **安全性保障**：通过输出约束，确保不会给出不安全的绝对性结论
- 📋 **可追溯性**：通过路径追踪，可以查看推理过程
- 🔄 **稳定性增强**：通过参数控制和多引擎融合，提高结果的一致性

---

### 6.7 约束机制实施建议（技术团队参考）

#### 6.7.1 实施优先级

**高优先级（必须实施）**：
1. ✅ 知识图谱路径注入（核心约束机制）- 引导推理方向
2. ✅ 输出解析与验证（基础约束）- 确保输出可用
3. ✅ 多引擎融合（权重约束）- 分散风险

**中优先级（建议实施）**：
1. 🔄 输出内容校验（疾病名称、概率值）- 过滤错误输出
2. 🔄 一致性检查（引擎结果比对）- 发现异常
3. 🔄 推理路径追踪（可追溯性）- 便于验证

**低优先级（可选实施）**：
1. ⏸️ 参数调优（通过A/B测试）- 优化效果
2. ⏸️ 监控与评估（日志和指标）- 持续改进

#### 6.7.2 技术实现要点

**输入约束**：
- 实现位置：`diagnosis-tool/app/kg-reasoning-engine/kg_reasoning_engine.py`（工具内部）
- 核心功能：将知识图谱推理路径注入提示词

**输出约束**：
- 实现位置：`diagnosis-tool/app/engines/llm_engine.py`（工具内部）
- 核心功能：增强 `_parse_response()` 方法，添加验证逻辑

**过程约束**：
- 实现位置：`diagnosis-tool/app/engines/fusion_engine.py`（工具内部）
- 核心功能：实现 `_is_llm_output_abnormal()` 方法，动态调整权重

### 6.8 约束机制实施建议（详细版）

#### 6.8.1 输入约束实施

**优先级：高**

1. **完善知识图谱路径注入**
   - 位置：`diagnosis-tool/app/kg-reasoning-engine/kg_reasoning_engine.py`（工具内部）
   - 任务：实现 `build_enhanced_prompt()` 方法，将知识图谱推理路径注入提示词
   - 依赖：需要先完成知识图谱引擎的路径检索功能

2. **标准化提示词模板**
   - 位置：`diagnosis-tool/app/engines/llm_engine.py`（工具内部）
   - 任务：创建标准化的提示词模板库，支持不同场景的提示词
   - 建议：将提示词模板抽取到配置文件或数据库中

#### 6.8.2 输出约束实施

**优先级：高**

1. **增强输出解析与验证**
   - 位置：`diagnosis-tool/app/engines/llm_engine.py`（工具内部）
   - 任务：增强 `_parse_response()` 方法，添加更多验证逻辑
   - 建议：
     - 验证概率值范围
     - 验证疾病名称（与知识库比对）
     - 验证必要字段完整性
     - 处理解析失败的情况

2. **输出内容校验**
   - 位置：`diagnosis-tool/app/engines/llm_engine.py`（工具内部）
   - 任务：添加疾病名称校验、概率值校验等功能
   - 依赖：需要疾病知识库或标准化术语库

#### 6.8.3 过程约束实施

**优先级：中**

1. **引擎结果一致性检查**
   - 位置：`diagnosis-tool/app/engines/fusion_engine.py`（工具内部）
   - 任务：添加大模型输出与其他引擎结果的一致性检查
   - 建议：如果差异过大，可以降低权重或标记异常

2. **推理路径追踪**
   - 位置：提示词模板（工具内部）
   - 任务：要求大模型输出使用了哪些推理路径
   - 效果：便于追溯和验证

#### 6.8.4 参数约束实施

**优先级：低**（已基本实现）

1. **参数调优**
   - 位置：`diagnosis-tool/app/engines/llm_engine.py`（工具内部）
   - 任务：根据实际效果调整temperature、num_predict等参数
   - 建议：通过A/B测试找到最佳参数组合

2. **参数可配置化**
   - 位置：环境变量或配置文件（工具内部）
   - 任务：将大模型参数配置化，便于不同环境使用不同参数
   - 建议：支持通过环境变量配置

---

---

## 五、大模型的连接方式：工具内部如何调用LLM？

### 5.1 连接架构

系统采用**主Agent + 工具**架构，大模型在工具内部通过**LangChain框架**统一管理调用，支持多种LLM后端（OpenAI、ChatGLM、Ollama、自定义API），采用异步调用模式，确保系统性能和响应速度。

```
┌─────────────────────────────────────────────────────────┐
│                    主Agent（Clinical Agent Brain）        │
│  - 调用工具，不直接调用大模型                              │
└─────────────────────────────────────────────────────────┘
                            ↓ 调用工具
┌─────────────────────────────────────────────────────────┐
│                    诊断工具 (Diagnosis Tool)             │
│  ┌───────────────────────────────────────────────────┐  │
│  │  工具内部：LLMEngine (大模型引擎)                    │  │
│  │  - LangChainLLMClient (统一LLM客户端)              │  │
│  │  - PromptTemplateManager (提示词模板管理)          │  │
│  │  - 构建提示词（使用Jinja2模板）                    │  │
│  │  - 调用LLM（通过LangChain）                        │  │
│  │  - 解析响应                                         │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                    ↓ LangChain统一接口
┌─────────────────────────────────────────────────────────┐
│          LangChain框架（支持多种后端）                    │
│  ├─ OpenAI API (GPT-4/GPT-3.5)                         │
│  ├─ ChatGLM API                                        │
│  ├─ Ollama (本地模型)                                   │
│  └─ 自定义HTTP API                                      │
└─────────────────────────────────────────────────────────┘
```

**关键点**：
- 主Agent不直接调用大模型，而是通过调用工具间接使用
- 工具内部使用LangChain统一管理大模型调用
- 工具返回结构化结果（ToolResult）给主Agent

### 5.2 连接配置

**环境变量配置**（使用LangChain统一管理）：

```bash
# LLM后端类型：openai, chatglm, ollama, custom
LLM_BACKEND=openai

# OpenAI配置（如果使用OpenAI）
OPENAI_API_KEY=your_openai_api_key
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.3
OPENAI_MAX_TOKENS=2000
# OPENAI_BASE_URL=https://api.openai.com/v1  # 可选，自定义API端点

# ChatGLM配置（如果使用ChatGLM）
# CHATGLM_API_URL=http://chatglm-service:8000
# CHATGLM_API_KEY=your_chatglm_key
# LLM_MODEL=chatglm3-6b

# Ollama配置（如果使用本地模型）
# OLLAMA_BASE_URL=http://localhost:11434
# OLLAMA_MODEL=llama2
# LLM_MODEL=llama2

# 自定义HTTP API配置（如果使用自定义LLM服务）
# CUSTOM_LLM_API_URL=http://llm-service:8080/api/generate
# CUSTOM_LLM_API_KEY=your_custom_key
# LLM_MODEL=medical-llm

# 通用LLM配置
LLM_TIMEOUT=30
LLM_MAX_RETRIES=3
LLM_RETRY_DELAY=1.0
```

**提示词模板配置**：
- 模板位置：`diagnosis-tool/app/config/prompt_templates/`（工具内部）
- 模板格式：Jinja2模板（`.jinja2`文件）
- 模板类型：
  - `diagnosis_reasoning.jinja2` - 诊断推理模板
  - `path_injection.jinja2` - 路径注入模板
  - `question_generation.jinja2` - 问诊问题生成模板
  - `explanation_generation.jinja2` - 解释生成模板

### 5.3 API调用实现

**工具内部实现**（`diagnosis-tool/app/engines/llm_engine.py`，使用LangChain）：

```python
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_templates import PromptTemplateManager
from app.kg_reasoning_engine.path_injector import PathInjector

class DiagnosisTool:
    """诊断工具 - 工具内部使用大模型"""
    
    def __init__(self):
        # 工具内部：使用LangChain统一管理LLM调用
        self.llm_client = LangChainLLMClient()  # 自动从环境变量读取配置
        self.template_manager = PromptTemplateManager()  # 提示词模板管理器
        self.path_injector = PathInjector()
        logger.info("诊断工具初始化完成（工具内部使用LangChain）")
    
    async def execute(self, context: ToolContext) -> ToolResult:
        """工具执行 - 主Agent调用此方法"""
        try:
            # 1. 从CDP读取患者信息
            cdp = context.cdp
            symptoms = cdp.problem_representation.symptoms
            signs = cdp.patient_state.vital_signs
            
            # 2. 检索推理路径
            if self.path_retriever:
                paths = self.path_retriever.retrieve_disease_paths(symptoms)
            else:
                paths = []
            
            # 3. 工具内部使用大模型进行推理
            llm_result = await self._llm_diagnose_async(symptoms, signs, cdp, paths)
            
            # 4. 融合多个引擎结果
            fused_result = await self._fuse_engines(llm_result, cdp)
            
            # 5. 返回ToolResult
            return ToolResult(
                status="success",
                payload={
                    "ddx": fused_result.ddx,
                    "confidence": fused_result.confidence
                },
                evidence=llm_result.evidence,
                suggestedWrites={
                    "cdp.ddx": fused_result.ddx
                }
            )
        except Exception as e:
            logger.error(f"诊断工具执行失败: {str(e)}", exc_info=True)
            return ToolResult(
                status="error",
                payload={},
                evidence=[],
                errors=[str(e)]
            )
    
    async def _llm_diagnose_async(self, symptoms, signs, context, paths):
        """工具内部：大模型推理（异步版本）"""
        # 1. 使用模板管理器构建提示词
        paths_text = self._format_paths(paths) if paths else None
        prompt = self.template_manager.format_diagnosis_reasoning(
            symptoms=symptoms,
            signs=signs,
            context=context,
            paths=paths_text
        )
        
        # 2. 如果路径注入器需要，可以进一步处理
        if paths and self.path_injector:
            prompt = self.path_injector.inject_paths(prompt, paths)
        
        # 3. 工具内部使用LangChain调用LLM（自动重试和错误处理）
        llm_result = await self.llm_client.generate(prompt)
        
        # 4. 解析结果
        parsed_result = self._parse_llm_result(llm_result)
        
        return parsed_result
```

**工具内部实现**（`diagnosis-tool/app/utils/llm_client.py`）：

```python
from langchain.llms.base import BaseLLM
from langchain.chat_models import ChatOpenAI
from langchain.llms import Ollama

class LangChainLLMClient:
    """LangChain LLM客户端封装"""
    
    def __init__(self, config: Optional[LLMConfig] = None):
        self.config = config or self._load_config_from_env()
        self.llm = self._create_llm()  # 根据配置创建LLM实例
    
    def _create_llm(self) -> BaseLLM:
        """创建LLM实例（支持多种后端）"""
        if self.config.backend == LLMBackend.OPENAI:
            return ChatOpenAI(
                model_name=self.config.model,
                temperature=self.config.temperature,
                max_tokens=self.config.max_tokens,
                openai_api_key=self.config.openai_api_key
            )
        elif self.config.backend == LLMBackend.OLLAMA:
            return Ollama(
                model=self.config.ollama_model,
                base_url=self.config.ollama_base_url
            )
        # ... 其他后端支持
    
    async def generate(self, prompt: str, **kwargs) -> str:
        """生成文本（自动重试和错误处理）"""
        # LangChain自动处理重试和错误
        if hasattr(self.llm, 'apredict'):
            return await self.llm.apredict(prompt, **kwargs)
        # ... 其他调用方式
```

### 5.4 提示词模板管理

**使用LangChain的PromptTemplateManager管理提示词**：

```python
from app.utils.prompt_templates import PromptTemplateManager

template_manager = PromptTemplateManager()

# 格式化诊断推理提示词
prompt = template_manager.format_diagnosis_reasoning(
    symptoms=["发热", "咳嗽"],
    signs={"体温": "38.5℃"},
    context={"年龄": 35},
    paths="路径1: 上呼吸道感染..."
)
```

**模板文件位置**：
- `diagnosis-tool/app/config/prompt_templates/`（工具内部）
- 模板文件：`.jinja2`格式（Jinja2语法）
- 配置文件：`templates.yaml`（模板元数据）

**支持的模板类型**：
1. `diagnosis_reasoning.jinja2` - 诊断推理模板
2. `path_injection.jinja2` - 路径注入模板
3. `question_generation.jinja2` - 问诊问题生成模板
4. `explanation_generation.jinja2` - 解释生成模板

### 5.5 API请求格式（LangChain内部处理）

**LangChain自动处理请求格式**，根据不同的后端（OpenAI、ChatGLM等）自动适配：

**OpenAI后端**：
- LangChain内部使用OpenAI SDK
- 自动处理API密钥、请求格式、响应解析

**ChatGLM后端**：
- 通过HTTP API调用
- LangChain客户端自动适配ChatGLM API格式

**自定义后端**：
- 通过HTTP API调用
- 支持自定义请求格式和响应解析

### 5.6 错误处理与重试机制

**LangChain自动提供错误处理和重试机制**：

- **自动重试**：配置`LLM_MAX_RETRIES`和`LLM_RETRY_DELAY`
- **超时处理**：配置`LLM_TIMEOUT`，自动处理超时异常
- **错误分类**：自动识别网络错误、API错误、超时错误等
- **降级处理**：LLM调用失败时返回错误响应，不影响其他引擎

#### 5.6.1 错误分类

| 错误类型 | HTTP状态码 | 处理策略 |
|---------|-----------|---------|
| **超时错误** | - | 返回空结果，不影响其他引擎 |
| **连接错误** | - | 返回空结果，记录日志 |
| **服务错误** | 500+ | 返回空结果，记录错误码 |
| **解析错误** | - | 尝试重新解析，失败返回空结果 |

#### 5.5.2 重试机制（可选增强）

```python
import asyncio
from typing import Optional

class LLMEngine:
    async def diagnose(self, request: DiagnosisEngineRequest, max_retries: int = 2) -> Dict:
        """大模型推理（带重试机制）"""
        prompt = self._build_prompt(request)
        
        for attempt in range(max_retries + 1):
            try:
                result = await self._call_llm_api(prompt)
                return self._parse_response(result)
            except (httpx.TimeoutException, httpx.ConnectError) as e:
                if attempt < max_retries:
                    wait_time = 2 ** attempt  # 指数退避：2秒、4秒
                    logger.warning(f"LLM调用失败，{wait_time}秒后重试 (尝试 {attempt+1}/{max_retries})")
                    await asyncio.sleep(wait_time)
                    continue
                else:
                    logger.error(f"LLM调用失败，已达最大重试次数: {str(e)}")
                    return self._create_error_response(str(e))
            except Exception as e:
                # 非网络错误不重试
                logger.error(f"LLM调用异常: {str(e)}")
                return self._create_error_response(str(e))
        
        return self._create_error_response("LLM调用失败")
```

### 5.6 连接安全性

**安全措施**：
1. **API密钥管理**：通过环境变量或密钥管理服务存储API密钥
2. **请求签名**：对大模型API请求进行签名验证（如需要）
3. **限流保护**：限制对大模型服务的请求频率，防止过载
4. **超时控制**：设置合理的超时时间，避免长时间等待

**实现示例**：
```python
import os
from datetime import datetime
import hmac
import hashlib

class LLMEngine:
    def __init__(self):
        self.llm_url = os.getenv("LLM_API_URL", "http://llm-service:8080")
        self.api_key = os.getenv("LLM_API_KEY", "")  # 从环境变量获取
        # ... 其他配置
    
    def _add_auth_headers(self, headers: dict) -> dict:
        """添加认证头"""
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        return headers
    
    async def _call_llm_api(self, prompt: str) -> str:
        """调用LLM API（带认证）"""
        headers = self._add_auth_headers({
            "Content-Type": "application/json",
            "X-Request-ID": str(uuid.uuid4())  # 请求追踪ID
        })
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(
                f"{self.llm_url}/api/generate",
                json={...},
                headers=headers
            )
            return response.json()
```
