# AI医生系统 - 服务执行顺序与系统完整性

> **文档定位**：本文档详细说明AI医生系统的服务执行顺序、服务依赖关系、系统完整性评估和开发建议。  
> **参考文档**：
> - 《AI医生系统-技术架构设计-核心架构.md》- 服务依赖关系
> - 《AI医生系统-开发流程.md》- 开发顺序
> - 《AI医生系统 - 完整流程图与知识内容映射.md》- 完整流程

---

## 📋 目录

1. [服务执行顺序](#一服务执行顺序)
2. [服务依赖关系](#二服务依赖关系)
3. [系统完整性评估](#三系统完整性评估)
4. [开发顺序建议](#四开发顺序建议)
5. [系统完整性检查清单](#五系统完整性检查清单)

---

## 一、服务执行顺序

### 1.1 系统入口流程

```
用户输入
    ↓
diagnosis-service（协调器，创建CDP）
    ↓
health-state-assessment-service（健康状态判定服务 - 脑区0）
    ├─→ 入口判定流程（Step 1-5）
    ├─→ 工作态判定（wellness_mode / clinical_mode）
    └─→ 危险信号检查（红旗库）
```

### 1.2 路径分叉

#### 路径A：健康筛查流程（健康管理态）

**执行位置**：主要在 `health-state-assessment-service` 内部完成

**流程步骤**：
```
A1: 需求分类
    ↓
A2: 通用最小健康档案
    ↓
A3: 进入对应分支
    ├─→ A3-1: 筛查建议分支
    ├─→ A3-2: 体检报告解读分支
    ├─→ A3-3: 健康目标管理分支
    └─→ A3-4: 计划性健康需求分支
    ↓
A4: 统一结果页输出
    ↓
A5: 随访闭环
```

**涉及服务**：
- `health-state-assessment-service`（主要执行）
- `diagnosis-service`（流程编排，可选）

#### 路径B：症状诊断流程（临床诊疗态）- 5步AI循证诊断流程

**完整流程**：

```
Step 1: 识别问题
    ├─→ clinical-parsing-service（脑区A：病例理解）
    │   └─→ 医学概念识别与归一化（CUI/ICD/SNOMED）
    └─→ dialog-service（脑区B：对话管理）
        └─→ 问题清单构建与信息缺口识别

Step 2: 构建鉴别诊断候选集并分层
    └─→ diagnosis-engine-service（脑区C：诊断引擎）
        ├─→ 知识图谱推理引擎（kg-reasoning-engine）
        ├─→ 多引擎融合诊断（multi-engine-fusion）
        └─→ 三层分层分类器（three_layer_classifier）

Step 3: 组织候选集并建立分流路径
    ├─→ diagnosis-engine-service（脑区C：推理组织器）
    │   └─→ 推理子组组织、分流路径设计
    └─→ dialog-service（脑区B：对话管理）
        └─→ 生成结构化分流问题

Step 4: 采集关键证据并形成排序与验证计划
    ├─→ dialog-service（脑区B：主动问诊）
    ├─→ diagnosis-engine-service（脑区C：证据分析器）
    ├─→ workup-planner-service（脑区D：检查建议）
    └─→ risk-assessment-service（脑区F：风险评估）

Step 5: 回填证据并输出终点结论包
    ├─→ diagnosis-engine-service（脑区C：证据分析器）
    ├─→ explanation-service（脑区G：解释生成）
    ├─→ treatment-engine-service（脑区E：治疗推理）
    └─→ diagnosis-service（流程编排与回退）
```

### 1.3 服务调用时序图

#### 健康管理态（路径A）

```
前端
  ↓ POST /api/v1/diagnosis/start
diagnosis-service
  ↓ 创建CDP
  ↓ 调用健康状态判定
health-state-assessment-service
  ↓ 执行入口判定（Step 1-5）
  ↓ 判定为wellness_mode
  ↓ 执行健康筛查流程（A1-A5）
  ↓ 返回结果
diagnosis-service
  ↓ 更新CDP
  ↓ 返回结果
前端
```

#### 临床诊疗态（路径B）

```
前端
  ↓ POST /api/v1/diagnosis/start
diagnosis-service
  ↓ 创建CDP
  ↓ 调用健康状态判定
health-state-assessment-service
  ↓ 执行入口判定（Step 1-5）
  ↓ 判定为clinical_mode
  ↓ 返回结果
diagnosis-service
  ↓ 执行5步AI循证诊断流程
  
  Step 1: 识别问题
    ├─→ clinical-parsing-service（概念归一化）
    └─→ dialog-service（问题清单构建）
  
  Step 2: 构建鉴别诊断候选集
    └─→ diagnosis-engine-service（生成DDx）
        └─→ risk-assessment-service（风险评估）
  
  Step 3: 组织候选集并建立分流路径
    ├─→ diagnosis-engine-service（推理组织）
    └─→ dialog-service（分流路径设计）
  
  Step 4: 采集关键证据
    ├─→ dialog-service（主动问诊）
    ├─→ diagnosis-engine-service（证据分析）
    ├─→ workup-planner-service（检查建议）
    └─→ risk-assessment-service（风险评估）
  
  Step 5: 回填证据并输出结论
    ├─→ diagnosis-engine-service（证据回填）
    ├─→ explanation-service（解释生成）
    └─→ treatment-engine-service（治疗推理）
  
  ↓ 更新CDP
  ↓ 返回结果
前端
```

---

## 二、服务依赖关系

### 2.1 服务依赖图

```
diagnosis-service (协调器)
    ├─→ health-state-assessment-service (健康状态判定)
    │       └─→ [无依赖]
    │
    ├─→ clinical-parsing-service (病例理解)
    │       ├─→ ocr-service (OCR服务，可选)
    │       └─→ [Neo4j知识图谱，用于概念归一化]
    │
    ├─→ dialog-service (主动问诊)
    │       ├─→ clinical-parsing-service (获取结构化信息)
    │       └─→ diagnosis-engine-service (获取DDx信息)
    │
    ├─→ diagnosis-engine-service (鉴别诊断)
    │       ├─→ clinical-parsing-service (获取结构化病例)
    │       ├─→ risk-assessment-service (获取风险评估)
    │       └─→ [Neo4j知识图谱，用于路径推理]
    │
    ├─→ workup-planner-service (检查建议)
    │       ├─→ diagnosis-engine-service (获取DDx列表)
    │       └─→ risk-assessment-service (获取风险等级)
    │
    ├─→ treatment-engine-service (治疗建议)
    │       ├─→ diagnosis-engine-service (获取诊断结果)
    │       └─→ risk-assessment-service (获取风险等级)
    │
    ├─→ risk-assessment-service (风险评估)
    │       ├─→ diagnosis-engine-service (获取DDx列表)
    │       └─→ [无其他服务依赖]
    │
    └─→ explanation-service (证据链)
            ├─→ workup-planner-service (获取检查建议)
            └─→ treatment-engine-service (获取治疗方案)
```

### 2.2 同步/异步调用策略

#### 同步调用场景（必须等待结果）

| 调用关系 | 超时时间 | 说明 |
|---------|---------|------|
| `diagnosis-service` → `health-state-assessment-service` | 5秒 | 必须等待判定结果才能决定后续流程 |
| `diagnosis-service` → `clinical-parsing-service` | 10秒 | 必须等待结构化结果才能进行诊断 |
| `diagnosis-service` → `diagnosis-engine-service` | 30秒 | 必须等待DDx结果才能进行后续处理 |
| `diagnosis-service` → `dialog-service` | 10秒 | 必须等待问诊结果才能继续流程 |

#### 异步调用场景（不阻塞主流程）

| 调用关系 | 说明 |
|---------|------|
| `diagnosis-service` → `workup-planner-service` | 可以在后台生成，通过回调或消息队列返回结果 |
| `diagnosis-service` → `treatment-engine-service` | 可以在后台生成，通过回调或消息队列返回结果 |
| `diagnosis-service` → `explanation-service` | 可以在后台生成，通过回调或消息队列返回结果 |

### 2.3 数据流向

```
用户输入
    ↓
diagnosis-service (创建CDP)
    ↓
health-state-assessment-service (判定工作态)
    ↓
    ├─→ wellness_mode: 健康管理态流程
    │       └─→ [生成wellness_plan，结束]
    │
    └─→ clinical_mode: 临床诊疗态流程
            ↓
        clinical-parsing-service (结构化提取)
            ↓
        dialog-service (主动问诊)
            ↓
        diagnosis-engine-service (生成DDx)
            ├─→ risk-assessment-service (风险评估)
            └─→ workup-planner-service (检查建议)
                    ↓
                treatment-engine-service (治疗建议)
                    ↓
                explanation-service (生成解释)
                    ↓
                返回诊断结果
```

---

## 三、系统完整性评估

### 3.1 核心服务列表（按优先级）

#### P0 - 核心基础服务（必须首先完成）

| 服务名称 | 对应脑区 | 核心功能 | 开发时间 | 状态 |
|---------|---------|---------|---------|------|
| `health-state-assessment-service` | 脑区0 | 入口判定流程、工作态判定、危险信号检查 | 2-3周 | ✅ 已创建 |
| `diagnosis-service` | 协调器 | CDP管理、流程编排、冲突解决 | 3-4周 | ✅ 已创建 |

#### P1 - 结构化推理通道（临床诊疗态核心）

| 服务名称 | 对应脑区 | 核心功能 | 开发时间 | 状态 |
|---------|---------|---------|---------|------|
| `clinical-parsing-service` | 脑区A | 医学概念识别与归一化、结构化提取 | 2-3周 | ✅ 已创建 |
| `diagnosis-engine-service` | 脑区C | 知识图谱推理、多引擎融合、三层分层 | 4-5周 | ✅ 已创建 |
| `dialog-service` | 脑区B | 信息缺口识别、智能追问生成、NLG | 2-3周 | ✅ 已创建 |
| `workup-planner-service` | 脑区D | 检查价值评估、验证计划构建 | 2周 | ✅ 已创建 |
| `treatment-engine-service` | 脑区E | 治疗方案推理、药物推荐 | 2周 | ✅ 已创建 |
| `risk-assessment-service` | 脑区F | 高危识别、紧急程度评估 | 2-3周 | ✅ 已创建 |

#### P2 - 语言与策略通道

| 服务名称 | 对应脑区 | 核心功能 | 开发时间 | 状态 |
|---------|---------|---------|---------|------|
| `explanation-service` | 脑区G | 证据链构建、解释生成、推理路径可视化 | 2周 | ✅ 已创建 |

#### P3 - 辅助服务

| 服务名称 | 对应脑区 | 核心功能 | 开发时间 | 状态 |
|---------|---------|---------|---------|------|
| `ocr-service` | - | 报告图片识别、结构化数据提取 | 1-2周 | ✅ 已创建 |
| `examination-service` | - | 检查方案管理、报告识别 | 2周 | ✅ 已创建 |
| `frontend` | - | 对话界面、信息面板、诊断结果展示 | 4-5周 | ✅ 已创建 |

### 3.2 系统完整性评估结果

#### ✅ 已完成项

1. **服务目录结构**：所有核心服务目录已创建
2. **基础框架**：部分服务已有基础代码结构
3. **文档体系**：完整的文档体系已建立

#### ⚠️ 待完成项

1. **服务实现完整性**
   - [ ] 各服务是否按实现方案完成核心功能
   - [ ] 服务间调用接口是否实现
   - [ ] 错误处理是否完善

2. **服务间集成**
   - [ ] 服务间调用是否正常
   - [ ] CDP数据流转是否正常
   - [ ] 超时和重试机制是否配置
   - [ ] 熔断机制是否配置

3. **基础设施**
   - [ ] 数据库（MySQL/Oracle）是否配置
   - [ ] Neo4j知识图谱是否初始化
   - [ ] Redis缓存是否配置
   - [ ] 消息队列是否配置（如需要）

4. **知识内容**
   - [ ] 规则库是否完善（红旗库、症状组合规则等）
   - [ ] 知识图谱数据是否导入
   - [ ] 模板库是否建立
   - [ ] 归一化词表是否完善

---

## 四、开发顺序建议

### 4.1 开发阶段划分

#### 阶段1：基础架构（Week 1-2）

**目标**：搭建系统基础架构

**任务清单**：
- [ ] 项目结构搭建
- [ ] 数据库表结构设计
- [ ] CDP数据模型实现
- [ ] 基础配置和工具类
- [ ] 错误处理框架

**验收标准**：
- 所有服务可以启动
- 数据库连接正常
- CDP数据模型可以正常使用

#### 阶段2：核心服务（Week 3-8）

**目标**：实现P0和P1核心服务

**任务清单**：
- [ ] `health-state-assessment-service`（P0模块）
  - [ ] 入口判定流程（Step 1-5）
  - [ ] 工作态判定
  - [ ] 危险信号检查
- [ ] `diagnosis-service`（CDP管理、流程编排）
  - [ ] CDP管理功能
  - [ ] 健康筛查流程编排（A1-A5）
  - [ ] 5步AI循证诊断流程编排
- [ ] `clinical-parsing-service`（病例理解）
  - [ ] 医学概念识别与归一化
  - [ ] 结构化提取
- [ ] `diagnosis-engine-service`（DR.KNOWS核心）
  - [ ] 知识图谱推理引擎
  - [ ] 多引擎融合诊断
  - [ ] 三层分层分类器

**验收标准**：
- 核心服务功能完整
- 服务间调用正常
- 单元测试通过

#### 阶段3：推理服务（Week 9-14）

**目标**：实现P1剩余服务和P2服务

**任务清单**：
- [ ] `dialog-service`（主动问诊）
  - [ ] 信息缺口识别
  - [ ] 智能追问生成
  - [ ] 自然语言生成
- [ ] `workup-planner-service`（检查建议）
  - [ ] 检查价值评估
  - [ ] 验证计划构建
- [ ] `treatment-engine-service`（治疗推理）
  - [ ] 治疗方案推理
  - [ ] 药物推荐
- [ ] `risk-assessment-service`（风险评估）
  - [ ] 高危识别
  - [ ] 紧急程度评估
- [ ] `explanation-service`（可解释性）
  - [ ] 证据链构建
  - [ ] 解释生成

**验收标准**：
- 所有推理服务功能完整
- 服务间集成测试通过
- 端到端测试通过

#### 阶段4：完善服务（Week 15-18）

**目标**：实现P3辅助服务

**任务清单**：
- [ ] `ocr-service`（OCR识别）
- [ ] `examination-service`（检查管理）
- [ ] 知识内容完善
  - [ ] 规则库完善
  - [ ] 知识图谱数据导入
  - [ ] 模板库建立

**验收标准**：
- 辅助服务功能完整
- 知识内容可用

#### 阶段5：前端与集成（Week 19-24）

**目标**：前端开发和系统集成

**任务清单**：
- [ ] 前端界面开发
  - [ ] 对话界面
  - [ ] 信息面板
  - [ ] 诊断结果展示
- [ ] 前后端联调
- [ ] 端到端测试
- [ ] 性能优化
- [ ] 监控和告警配置

**验收标准**：
- 前端功能完整
- 前后端集成正常
- 性能满足要求
- 监控告警正常

### 4.2 优先级排序

#### P0（必须）- 核心基础服务

1. `health-state-assessment-service` - 入口判定流程
2. `diagnosis-service` - CDP管理、流程编排

#### P1（重要）- 结构化推理通道

3. `clinical-parsing-service` - 病例理解
4. `diagnosis-engine-service` - DR.KNOWS核心
5. `dialog-service` - 主动问诊
6. `workup-planner-service` - 检查建议
7. `treatment-engine-service` - 治疗推理
8. `risk-assessment-service` - 风险评估

#### P2（可选）- 语言与策略通道

9. `explanation-service` - 可解释性

#### P3（辅助）- 辅助服务

10. `ocr-service` - OCR识别
11. `examination-service` - 检查管理
12. `frontend` - 前端界面

---

## 五、系统完整性检查清单

### 5.1 服务实现完整性检查

#### 核心服务检查

- [ ] `health-state-assessment-service`
  - [ ] 入口判定流程（Step 1-5）已实现
  - [ ] 工作态判定已实现
  - [ ] 危险信号检查已实现
  - [ ] API接口已实现
  - [ ] 错误处理已完善

- [ ] `diagnosis-service`
  - [ ] CDP管理功能已实现
  - [ ] 健康筛查流程编排已实现
  - [ ] 5步AI循证诊断流程编排已实现
  - [ ] API接口已实现
  - [ ] 错误处理已完善

- [ ] `clinical-parsing-service`
  - [ ] 医学概念识别与归一化已实现
  - [ ] 结构化提取已实现
  - [ ] API接口已实现

- [ ] `diagnosis-engine-service`
  - [ ] 知识图谱推理引擎已实现
  - [ ] 多引擎融合诊断已实现
  - [ ] 三层分层分类器已实现
  - [ ] API接口已实现

- [ ] `dialog-service`
  - [ ] 信息缺口识别已实现
  - [ ] 智能追问生成已实现
  - [ ] 自然语言生成已实现
  - [ ] API接口已实现

- [ ] `workup-planner-service`
  - [ ] 检查价值评估已实现
  - [ ] 验证计划构建已实现
  - [ ] API接口已实现

- [ ] `treatment-engine-service`
  - [ ] 治疗方案推理已实现
  - [ ] 药物推荐已实现
  - [ ] API接口已实现

- [ ] `risk-assessment-service`
  - [ ] 高危识别已实现
  - [ ] 紧急程度评估已实现
  - [ ] API接口已实现

- [ ] `explanation-service`
  - [ ] 证据链构建已实现
  - [ ] 解释生成已实现
  - [ ] API接口已实现

### 5.2 服务间集成检查

- [ ] 服务间调用接口已实现
- [ ] 服务间调用正常（同步/异步）
- [ ] CDP数据流转正常
- [ ] 超时和重试机制已配置
- [ ] 熔断机制已配置
- [ ] 错误传播正常
- [ ] 日志追踪正常（traceId）

### 5.3 基础设施检查

- [ ] 数据库（MySQL/Oracle）已配置
  - [ ] 数据库连接正常
  - [ ] 表结构已创建
  - [ ] 索引已创建
- [ ] Neo4j知识图谱已初始化
  - [ ] Neo4j连接正常
  - [ ] 知识图谱数据已导入
- [ ] Redis缓存已配置
  - [ ] Redis连接正常
  - [ ] 缓存策略已配置
- [ ] 消息队列已配置（如需要）
  - [ ] 消息队列连接正常
  - [ ] 消息处理正常

### 5.4 知识内容检查

- [ ] 规则库已完善
  - [ ] 红旗信号库（red_flag_library）
  - [ ] 症状组合规则库（symptom_combination_rule）
  - [ ] 工作态判定规则库（work_mode_determination_rule）
  - [ ] 其他规则库
- [ ] 知识图谱数据已导入
  - [ ] 节点数据已导入
  - [ ] 关系数据已导入
  - [ ] 路径查询正常
- [ ] 模板库已建立
  - [ ] 主诉关键字段模板
  - [ ] 问诊问题模板
  - [ ] 解释生成模板
- [ ] 归一化词表已完善
  - [ ] 症状归一化词表
  - [ ] 疾病归一化词表
  - [ ] 药物归一化词表

### 5.5 测试检查

- [ ] 单元测试
  - [ ] 测试覆盖率 ≥ 80%
  - [ ] 核心业务逻辑100%覆盖
- [ ] 集成测试
  - [ ] 服务间集成测试通过
  - [ ] 数据流转测试通过
- [ ] 端到端测试
  - [ ] 完整流程测试通过
  - [ ] 性能测试通过

### 5.6 部署检查

- [ ] 配置文件已更新（生产环境配置）
- [ ] 环境变量已设置
- [ ] 日志配置已更新
- [ ] 监控和告警已配置
- [ ] 备份策略已制定
- [ ] 回滚方案已制定

---

## 六、总结

### 6.1 服务执行顺序总结

1. **入口阶段**：`diagnosis-service` → `health-state-assessment-service`
2. **路径分叉**：
   - **路径A**：健康筛查流程（主要在`health-state-assessment-service`内部）
   - **路径B**：5步AI循证诊断流程（多个服务协作）
3. **路径B执行顺序**：
   - Step 1: `clinical-parsing-service` + `dialog-service`
   - Step 2: `diagnosis-engine-service`
   - Step 3: `diagnosis-engine-service` + `dialog-service`
   - Step 4: `dialog-service` + `diagnosis-engine-service` + `workup-planner-service` + `risk-assessment-service`
   - Step 5: `diagnosis-engine-service` + `explanation-service` + `treatment-engine-service`

### 6.2 系统完整性总结

**已完成**：
- ✅ 所有核心服务目录已创建
- ✅ 基础框架已搭建
- ✅ 文档体系已建立

**待完成**：
- ⚠️ 服务实现完整性（核心功能实现）
- ⚠️ 服务间集成（调用接口、数据流转）
- ⚠️ 基础设施配置（数据库、Neo4j、Redis）
- ⚠️ 知识内容完善（规则库、知识图谱、模板库）

### 6.3 开发建议

1. **优先完成P0和P1服务**：确保核心功能可用
2. **分阶段实现**：按照5个阶段逐步实现
3. **及时集成测试**：每完成一个阶段就进行集成测试
4. **完善知识内容**：与开发并行进行，确保知识内容可用

---

**文档版本**：v1.0  
**创建日期**：2025年1月  
**维护人员**：开发团队

