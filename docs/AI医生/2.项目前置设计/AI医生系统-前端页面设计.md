# AI医生系统 - 前端页面设计

> **文档定位**：本文档详细设计AI医生系统的前端页面，包括页面布局、组件设计、交互流程等。  
> **参考产品**：百度健康智能AI预问诊、春雨医生、丁香医生、好大夫在线等  
> **技术栈**：React 18 + TypeScript + Ant Design + Zustand  
> **设计基础**：基于DR.KNOWS论文的八个脑区架构设计  
> **核心理念**：AI医生是**完整的医学推理引擎**，具备八个脑区的完整能力，支持健康管理态和临床诊疗态。

---

## 一、设计理念

### 1.1 核心设计原则

1. **像医生一样**：界面要温暖、专业，像真正的医生问诊
2. **对话为主**：以对话界面为核心，自然流畅的交互
3. **信息清晰**：诊断结果、建议等信息要清晰易懂
4. **响应式设计**：支持PC、平板、手机多端
5. **无障碍设计**：考虑老年用户和特殊需求用户
6. **主动引导**：系统主动引导用户，而不是被动等待用户操作
7. **系统整合**：与智能家庭医生协作，整合健康中心、健康监测等模块，提供整体方案

### 1.2 参考产品分析

**百度健康智能AI预问诊**：
- ✅ 对话式问诊，自然流畅
- ✅ 进度指示，用户知道进行到哪一步
- ✅ 结构化信息展示，清晰明了
- ✅ 追问问题带选项按钮，方便用户选择

**春雨医生/丁香医生**：
- ✅ 专业的医疗风格
- ✅ 清晰的诊断结果展示（可能性、证据、建议）
- ✅ 友好的用户引导
- ✅ 就医建议详细实用

**好大夫在线**：
- ✅ 问诊记录清晰
- ✅ 检查报告上传方便
- ✅ 历史记录管理完善

---

## 二、页面结构设计

### 2.1 整体布局（统一界面架构）

**设计理念**：所有功能（智能诊断、健康检查、报告上传、健康档案、病例记录等）都集成在同一个统一界面中，以对话为核心，无需页面跳转。

```
┌─────────────────────────────────────────────────────────┐
│  智能诊断系统（标题，无导航栏）                           │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────────────────┐ ┌──────────────────────┐ │
│  │  对话区域（70%）          │ │ 信息面板（30%）       │ │
│  │  ┌────────────────────┐  │ │ ┌──────────────────┐ │ │
│  │  │  对话头部          │  │ │ │ 顶部操作区        │ │ │
│  │  │  [医生头像] 智能    │  │ │ │ [开始新诊断]      │ │ │
│  │  │  诊断助手 [状态]    │  │ │ │ [我的病例]        │ │ │
│  │  └────────────────────┘  │ │ │ [健康档案]        │ │ │
│  │  ┌────────────────────┐  │ │ ├──────────────────┤ │ │
│  │  │  对话消息列表       │  │ │ │ 信息完整度卡片    │ │ │
│  │  │  - 用户消息        │  │ │ │ 已收集信息摘要    │ │ │
│  │  │  - 系统消息        │  │ │ ├──────────────────┤ │ │
│  │  │  - 追问问题        │  │ │ │ 快速操作          │ │ │
│  │  │  - 诊断结果卡片    │  │ │ │ • 上传报告        │ │ │
│  │  │  - 健康目标管理结果    │  │ │ │ • 补充体征        │ │ │
│  │  └────────────────────┘  │ │ └──────────────────┘ │ │
│  │  ┌────────────────────┐  │ │                      │ │
│  │  │  进度指示器        │  │ │ 诊断结果详情区域    │ │
│  │  └────────────────────┘  │ │ （诊断完成后显示）   │ │
│  │  ┌────────────────────┐  │ │                      │ │
│  │  │  输入区域          │  │ │ 抽屉/侧边栏区域     │ │
│  │  │  [文本] [图片]     │  │ │ • 健康档案详情      │ │
│  │  │  [上传] [发送]     │  │ │ • 病例列表          │ │
│  │  └────────────────────┘  │ │ • 诊断结果详情      │ │
│  └──────────────────────────┘ └──────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

**核心特点**：
- ✅ **无顶部导航栏**：所有功能都在统一界面中访问
- ✅ **对话为核心**：诊断对话、报告上传都在对话中完成
- ✅ **信息面板集成**：健康档案、病例记录、诊断结果都通过信息面板和抽屉访问
- ✅ **无缝体验**：无需页面跳转，所有操作在同一界面完成
- ✅ **无感入口判定**：入口判定流程（P0模块）在对话中无感完成，用户直接进入对话，系统在前几轮对话中自然完成工作态判定并进入相应流程

### 2.2 响应式布局

**PC端（≥1200px）**：
- 左右分栏：对话区域（70%）+ 信息面板（30%）

**平板端（768px-1199px）**：
- 上下布局：对话区域（70%）+ 信息面板（30%，可折叠）

**手机端（<768px）**：
- 全屏对话：信息面板通过抽屉（Drawer）从右侧滑出

---

## 三、核心页面设计

### 3.0 AI诊断入口判定流程（P0模块）- 在对话中无感完成

#### 3.0.1 业务说明

**设计理念**：入口判定流程（P0模块）不在独立界面中完成，而是在对话过程中无感完成。用户直接进入对话界面，系统在前几轮对话中自然完成工作态判定，然后无感进入相应的推理流程。

**流程概览**：
```
用户输入 → 对话中完成入口判定（Step 1-5） → 无感进入工作态流程
```

**实现方式**：
- 用户进入系统后直接看到对话界面
- 用户输入第一句话后，系统在后台完成入口判定的5个步骤
- 如果需要进行方向澄清，系统会在对话中自然询问
- 如果发现危险信号，系统会在对话中提示
- 判定完成后，系统无感进入健康筛查流程（A路径）或诊断流程（B路径）

#### 3.0.2 对话中的入口判定流程

**对话中的实现**：

入口判定流程的所有步骤都在对话中自然完成，用户无感知：

1. **Step 1：接收用户输入**
   - 用户在对话输入框中输入第一句话
   - 系统接收并处理用户输入

2. **Step 2：识别症状/困扰**
   - 系统在后台分析用户输入
   - 判断是否有症状/困扰
   - 如果明确，直接进入下一步
   - 如果不明确，系统会在对话中自然询问

3. **Step 3：方向澄清（仅在需要时）**
   - 如果用户需求不明确，系统会在对话中询问：
     - "请确认一下，您主要是想进行健康筛查/体检规划，还是想咨询症状问题？"
   - 用户通过对话回答，系统根据回答确定方向

4. **Step 4：危险信号检查**
   - 系统在后台检查危险信号
   - 如果发现危险信号，系统会在对话中提示：
     - "检测到危险信号，建议您立即就医。"
   - 如果未发现，继续正常流程

5. **Step 5：无感进入工作态流程**
   - 系统根据判定结果，无感进入相应流程：
     - 健康管理态 → 进入健康筛查流程（A路径）
     - 临床诊疗态 → 进入诊断流程（B路径）
   - 用户无需感知流程切换，对话自然继续

**对话示例**：

```
用户：我最近胸痛
系统：我了解了，胸痛需要重视。请问疼痛持续多久了？
      [系统在后台完成入口判定，判定为临床诊疗态，无感进入诊断流程]

用户：我想做个体检
系统：好的，我来帮您规划体检方案。请问您的年龄和性别是？
      [系统在后台完成入口判定，判定为健康管理态，无感进入健康筛查流程]
```

---

### 3.1 健康状态判定页面（HealthStateAssessmentPage）- 新增

#### 3.1.1 业务说明

**页面说明**：这是用户发起咨询后，经过P0模块判定后的页面，展示健康状态判定结果（脑区0的输出）。

#### 3.1.2 页面结构

```typescript
// HealthStateAssessmentPage.tsx
const HealthStateAssessmentPage: React.FC = () => {
  return (
    <Layout>
      <Content>
        <Card>
          {/* 健康状态判定结果 */}
          <HealthStateAssessmentCard />
          
          {/* 如果是健康管理态，显示健康管理计划 */}
          {workMode === 'wellness_mode' && <WellnessPlanCard />}
          
          {/* 如果是临床诊疗态，进入诊断流程 */}
          {workMode === 'clinical_mode' && <NavigateToDiagnosis />}
        </Card>
      </Content>
    </Layout>
  );
};
```

#### 3.1.3 健康状态判定卡片（HealthStateAssessmentCard）

```tsx
<Card title="健康状态评估" style={{ marginBottom: 16 }}>
  {/*
    数据来源：POST /api/v1/health-state-assessment/assess
    返回 data.healthStateAssessment（camelCase），并与CDP内部 snake_case 做映射
  */}
  <Space direction="vertical" style={{ width: '100%' }} size="large">
    {/* 工作态显示 */}
    <Tag color={workMode === 'clinical_mode' ? 'red' : 'green'}>
      {workMode === 'clinical_mode' ? '临床诊疗态' : '健康管理态'}
    </Tag>
    
    {/* 风险等级 */}
    <Descriptions>
      <Descriptions.Item label="风险等级">
        <Tag color={riskLevel === 'L1' ? 'red' : riskLevel === 'L2' ? 'orange' : 'blue'}>
          {riskLevel}
        </Tag>
      </Descriptions.Item>
    </Descriptions>
    
    {/* 判定原因 */}
    <Alert message={assessmentReason} type="info" />
    
    {/* 红旗信号（如果有） */}
    {entryAssessment?.redFlagsHit && (entryAssessment?.redFlagsList?.length || 0) > 0 && (
      <Alert 
        message="发现高危信号，建议立即就医" 
        type="error" 
        description={entryAssessment.redFlagsList.join('、')}
      />
    )}
  </Space>
</Card>
```

---

### 3.2 健康筛查流程页面（WellnessScreeningPage）- 新增

#### 3.2.1 业务说明

**页面说明**：这是健康管理态下的核心页面，通过A1-A5五个步骤完成健康筛查服务。

**流程概览**：
```
A1需求分类 → A2收集健康画像 → A3执行分支 → A4生成统一结果 → A5设置随访
```

#### 3.2.2 流程进度指示器

**UI设计**：
```tsx
// WellnessScreeningProgress.tsx
const WellnessScreeningProgress: React.FC = () => {
  const { currentStage } = useWellnessScreening();
  
  const steps = [
    { title: '需求分类', stage: 'A1_DEMAND_CLASSIFICATION' },
    { title: '收集健康画像', stage: 'A2_HEALTH_PROFILE_COLLECTED' },
    { title: '执行分支', stage: 'A3_BRANCH_EXECUTED' },
    { title: '生成统一结果', stage: 'A4_UNIFIED_RESULT_GENERATED' },
    { title: '设置随访', stage: 'A5_FOLLOW_UP_SETUP' },
  ];
  
  const currentIndex = steps.findIndex(s => s.stage === currentStage);
  
  return (
    <Steps current={currentIndex} style={{ marginBottom: 24 }}>
      {steps.map(step => (
        <Steps.Step key={step.stage} title={step.title} />
      ))}
    </Steps>
  );
};
```

#### 3.2.3 A1｜需求分类

**UI设计**：
```tsx
// A1DemandClassification.tsx
const A1DemandClassification: React.FC = () => {
  const { demandType } = useWellnessScreening();
  
  return (
    <Card title="A1｜需求分类" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 需求类型展示 */}
        <Alert
          message={`已识别您的需求：${getDemandTypeLabel(demandType)}`}
          type="success"
          showIcon
        />
        
        {/* 提取的关键信息 */}
        <Card size="small" title="提取的关键信息">
          <Descriptions column={1}>
            <Descriptions.Item label="需求类型">
              {getDemandTypeLabel(demandType)}
            </Descriptions.Item>
            {/* 其他提取的信息 */}
          </Descriptions>
        </Card>
        
        {/* 下一步按钮 */}
        <Button type="primary" onClick={handleNext} block>
          继续下一步：收集健康画像
        </Button>
      </Space>
    </Card>
  );
};
```

#### 3.2.4 A2｜收集健康画像

**UI设计**：
```tsx
// A2HealthProfileCollection.tsx
const A2HealthProfileCollection: React.FC = () => {
  const { healthProfile, isComplete, missingRequiredFields } = useWellnessScreening();
  
  return (
    <Card title="A2｜收集健康画像" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 信息完整度 */}
        <Progress 
          percent={healthProfile.completeness * 100} 
          status={isComplete ? 'success' : 'active'} 
        />
        <Text>{healthProfile.completeness * 100}% 完整</Text>
        
        {/* 如果还有必填项缺失 */}
        {!isComplete && missingRequiredFields.length > 0 && (
          <Alert
            message="需要补充以下信息"
            description={
              <ul>
                {missingRequiredFields.map((field, index) => (
                  <li key={index}>{field}</li>
                ))}
              </ul>
            }
            type="warning"
            showIcon
          />
        )}
        
        {/* 补充提问 */}
        {!isComplete && followUpQuestions.length > 0 && (
          <Card size="small" title="请回答以下问题">
            {followUpQuestions.map((question, index) => (
              <div key={index} style={{ marginBottom: 16 }}>
                <Text strong>{question}</Text>
                <Input 
                  placeholder="请输入" 
                  style={{ marginTop: 8 }}
                  onPressEnter={handleAnswer}
                />
              </div>
            ))}
          </Card>
        )}
        
        {/* 已收集的信息展示 */}
        {isComplete && (
          <Card size="small" title="健康画像摘要">
            <Descriptions column={2}>
              <Descriptions.Item label="年龄">
                {healthProfile.basicInfo.age}
              </Descriptions.Item>
              <Descriptions.Item label="性别">
                {healthProfile.basicInfo.gender}
              </Descriptions.Item>
              <Descriptions.Item label="BMI">
                {healthProfile.basicInfo.bmi}
              </Descriptions.Item>
              {/* 其他信息 */}
            </Descriptions>
          </Card>
        )}
        
        {/* 下一步按钮 */}
        {isComplete && (
          <Button type="primary" onClick={handleNext} block>
            继续下一步：执行分支
          </Button>
        )}
      </Space>
    </Card>
  );
};
```

#### 3.2.5 A3｜执行分支

**UI设计**：
```tsx
// A3BranchExecution.tsx
const A3BranchExecution: React.FC = () => {
  const { branchResult, demandType } = useWellnessScreening();
  
  return (
    <Card title={`A3｜执行分支 - ${getDemandTypeLabel(demandType)}`} style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 根据需求类型展示不同内容 */}
        {demandType === 'SCREENING_RECOMMENDATION' && (
          <ScreeningRecommendationView result={branchResult} />
        )}
        
        {demandType === 'HEALTH_GOAL_MANAGEMENT' && (
          <HealthGoalManagementView result={branchResult} />
        )}
        
        {demandType === 'PLANNED_HEALTH_NEEDS' && (
          <PlannedHealthNeedsView result={branchResult} />
        )}
        
        {/* 下一步按钮 */}
        <Button type="primary" onClick={handleNext} block>
          继续下一步：生成统一结果
        </Button>
      </Space>
    </Card>
  );
};

// 筛查建议视图
const ScreeningRecommendationView: React.FC<{ result: BranchExecutionResult }> = ({ result }) => {
  return (
    <Card size="small" title="筛查建议">
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Tag color="blue">风险等级：{result.riskLevel}</Tag>
        <List
          dataSource={result.recommendations}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={item.name}
                description={
                  <div>
                    <div>{item.description}</div>
                    <div style={{ marginTop: 8 }}>
                      <Text type="secondary">优先级：{item.priority}</Text>
                    </div>
                    <div>
                      <Text type="secondary">原因：{item.reason}</Text>
                    </div>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      </Space>
    </Card>
  );
};
```

#### 3.2.6 A4｜生成统一结果

**UI设计**：
```tsx
// A4UnifiedResult.tsx
const A4UnifiedResult: React.FC = () => {
  const { unifiedResult } = useWellnessScreening();
  
  return (
    <Card title="A4｜统一结果" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 摘要 */}
        <Alert
          message={unifiedResult.summary}
          type="success"
          showIcon
        />
        
        {/* 主要内容 */}
        <Card size="small" title="主要内容">
          {/* 根据需求类型展示不同内容 */}
        </Card>
        
        {/* 现在建议做的事（清单） */}
        <Card size="small" title="现在建议做的事">
          <List
            dataSource={unifiedResult.recommendedActions}
            renderItem={(item) => <List.Item>{item}</List.Item>}
          />
        </Card>

        {/* 暂时不建议做的事（清单） */}
        <Card size="small" title="暂时不建议做的事">
          <List
            dataSource={unifiedResult.notRecommendedActions}
            renderItem={(item) => <List.Item>{item}</List.Item>}
          />
        </Card>

        {/* 下一次复查/更新时间点 */}
        <Card size="small" title="下一次复查/更新时间点">
          <Text>{unifiedResult.nextReviewTime}</Text>
        </Card>

        {/* 退出条件提示 */}
        <Card size="small" title="退出条件提示（出现不适/危险信号→线下评估）">
          <List
            dataSource={unifiedResult.exitConditions}
            renderItem={(item) => <List.Item>{item}</List.Item>}
          />
        </Card>
        
        {/* 下一步按钮 */}
        <Button type="primary" onClick={handleNext} block>
          继续下一步：设置随访
        </Button>
      </Space>
    </Card>
  );
};
```

#### 3.2.7 A5｜设置随访

**UI设计**：
```tsx
// A5FollowUpSetup.tsx
const A5FollowUpSetup: React.FC = () => {
  const { followUpPlan } = useWellnessScreening();
  
  return (
    <Card title="A5｜设置随访" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 随访计划 */}
        <Card size="small" title="随访计划">
          <Descriptions column={1}>
            <Descriptions.Item label="随访日期">
              {followUpPlan.followUpDate}
            </Descriptions.Item>
            <Descriptions.Item label="提醒内容">
              {followUpPlan.reminderContent}
            </Descriptions.Item>
          </Descriptions>
        </Card>
        
        {/* 完成提示 */}
        <Result
          status="success"
          title="健康筛查服务已完成"
          subTitle="系统已为您设置随访计划，会在随访日期前提醒您"
          extra={[
            <Button key="view" onClick={handleViewRecord}>
              查看记录
            </Button>,
            <Button key="new" type="primary" onClick={handleNewScreening}>
              开始新的筛查
            </Button>
          ]}
        />
      </Space>
    </Card>
  );
};
```

---

### 3.3 诊断对话页面（DiagnosisPage）

#### 3.1.1 页面结构

```typescript
// DiagnosisPage.tsx
import { Layout, Row, Col } from 'antd';
import DiagnosisChatPanel from '@/components/diagnosis/DiagnosisChatPanel';
import DiagnosisInfoPanel from '@/components/diagnosis/DiagnosisInfoPanel';

const DiagnosisPage: React.FC = () => {
  return (
    <Layout style={{ height: '100vh' }}>
      <Content style={{ padding: '24px' }}>
        <Row gutter={16} style={{ height: '100%' }}>
          {/* 左侧：对话区域 */}
          <Col xs={24} md={16} lg={17}>
            <DiagnosisChatPanel />
          </Col>
          {/* 右侧：信息面板 */}
          <Col xs={0} md={8} lg={7}>
            <DiagnosisInfoPanel />
          </Col>
        </Row>
      </Content>
    </Layout>
  );
};
```

#### 3.1.2 对话面板组件（DiagnosisChatPanel）

**组件结构**：
```
DiagnosisChatPanel
├── ChatHeader（对话头部）
│   ├── Avatar（医生头像）
│   ├── Title（"智能诊断助手"）
│   └── StatusTag（状态标签：收集中/分析中/已完成）
│
├── ChatMessages（消息列表，可滚动）
│   ├── UserMessage（用户消息）
│   ├── SystemMessage（系统消息）
│   └── QuestionMessage（追问问题，特殊样式）
│
├── CompletenessProgress（进度指示器）
│   └── Progress + Text（信息完整度）
│
├── DiagnosisWorkflowProgress（5步AI循证诊断流程进度指示器，临床诊疗态时显示）
│   └── Steps（Step 1-5进度指示）
│
└── ChatInput（输入区域）
    ├── TextArea（文本输入）
    ├── ToolBar（工具栏：语音、图片、表情）
    └── SendButton（发送按钮）
```

**关键特性**：
- 消息自动滚动到底部
- 追问问题高亮显示
- 选项按钮快速选择
- 语音输入支持（可选）
- 图片上传支持（可选）

#### 3.1.3 信息面板组件（DiagnosisInfoPanel）

**组件结构**：

```
DiagnosisInfoPanel
├── 顶部操作区
│   ├── [开始新诊断] 按钮
│   ├── [我的病例] 按钮 → 打开抽屉显示病例列表
│   └── [健康档案] 按钮 → 打开抽屉显示健康档案
│
├── 信息完整度卡片
│   └── Progress + Text（信息完整度）
│
├── 已收集信息摘要
│   └── List（已收集/待补充信息列表）
│
└── 快速操作
    ├── [上传检查报告] 按钮 → 打开上传对话框
    ├── [补充体征数据] 按钮 → 打开体征输入对话框
    └── [查看健康档案] 按钮 → 打开健康档案抽屉
```

**组件内容**：

0. **顶部操作区**
```tsx
<Card style={{ marginBottom: 16 }}>
  <Space direction="vertical" style={{ width: '100%' }} size="small">
    <Button 
      type="primary" 
      block 
      icon={<PlusOutlined />}
      onClick={handleStartNewDiagnosis}
    >
      开始新诊断
    </Button>
    <Button 
      block 
      icon={<FileTextOutlined />}
      onClick={() => setCaseDrawerVisible(true)}
    >
      我的病例
    </Button>
    <Button 
      block 
      icon={<UserOutlined />}
      onClick={() => setProfileDrawerVisible(true)}
    >
      健康档案
    </Button>
  </Space>
</Card>
```

1. **信息完整度卡片**
```tsx
<Card title="信息完整度" style={{ marginBottom: 16 }}>
  <Progress
    percent={65}
    status="active"
    strokeColor={{
      '0%': '#108ee9',
      '100%': '#87d068',
    }}
    format={(percent) => `${percent}%`}
  />
  <Text type="secondary" style={{ fontSize: 12 }}>
    {completeness < 60 ? '信息不足，需要补充' : 
     completeness < 70 ? '信息基本完整，建议继续补充' : 
     completeness < 80 ? '信息完整，可以开始分析' : 
     '信息完整，诊断准确性更高'}
  </Text>
</Card>
```

**信息完整度阈值说明**：
- **< 60%**：信息不足，需要继续收集信息（最低要求为60%才能进行诊断分析）
- **60% - 70%**：信息基本完整，建议继续补充
- **70% - 80%**：信息完整，可以开始分析（≥70%停止追问，可以开始分析）
- **≥ 80%**：信息完整，诊断准确性更高（理想状态）

**5步AI循证诊断流程进度指示器**（临床诊疗态时显示）：
```tsx
<Card title="诊断流程进度" style={{ marginBottom: 16 }}>
  <Steps 
    current={currentStep} 
    direction="horizontal"
    size="small"
    style={{ marginBottom: 16 }}
  >
    <Steps.Step 
      title="识别问题" 
      description="Step 1"
      status={getStepStatus(1)}
    />
    <Steps.Step 
      title="构建候选集" 
      description="Step 2"
      status={getStepStatus(2)}
    />
    <Steps.Step 
      title="组织分流路径" 
      description="Step 3"
      status={getStepStatus(3)}
    />
    <Steps.Step 
      title="采集证据" 
      description="Step 4"
      status={getStepStatus(4)}
    />
    <Steps.Step 
      title="输出结论包" 
      description="Step 5"
      status={getStepStatus(5)}
    />
  </Steps>
  <Text type="secondary" style={{ fontSize: 12 }}>
    当前步骤：{getStepName(currentStep)}
  </Text>
</Card>
```

**5步流程说明**（临床诊疗态）：
- **Step 1：识别问题** - 把用户的自然语言描述转化成可推理、可复用、可审计的结构化"问题清单"
- **Step 2：构建鉴别诊断候选集并分层** - 建立该主诉的鉴别诊断全集，并按临床风险与证据强度分成三层（首要假设/主要备选/必须排除）
- **Step 3：组织候选集并建立分流路径** - 把分层候选清单组织成可推进的推理结构，并提炼出分流路径
- **Step 4：采集关键证据并形成排序与验证计划** - 系统采集能够"推动排序变化"的关键证据，形成稳定的三层清单，并制定验证计划
- **Step 5：回填证据并输出终点结论包** - 将验证结果回填，更新三层排序，并输出可行动、可审计的终点结论包（四要素：结论、必须排除项状态、关键依据、行动与随访）

2. **已收集信息摘要**
```tsx
<Card title="已收集信息" style={{ marginBottom: 16 }}>
  <List
    size="small"
    dataSource={collectedInfo}
    renderItem={(item) => (
      <List.Item>
        {item.collected ? (
          <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
        ) : (
          <ClockCircleOutlined style={{ color: '#faad14', marginRight: 8 }} />
        )}
        <Text delete={!item.collected}>{item.label}</Text>
        {item.collected && <Text type="secondary">：{item.value}</Text>}
      </List.Item>
    )}
  />
</Card>
```

3. **快速操作**
```tsx
<Card title="快速操作">
  <Space direction="vertical" style={{ width: '100%' }}>
    <Button 
      block 
      icon={<UploadOutlined />}
      onClick={() => setUploadModalVisible(true)}
    >
      上传检查报告
    </Button>
    <Button 
      block 
      icon={<HeartOutlined />}
      onClick={() => setVitalSignsModalVisible(true)}
    >
      补充体征数据
    </Button>
  </Space>
</Card>
```

**说明**：
- "上传检查报告"：打开上传对话框（Modal），上传后自动在对话中处理
- "补充体征数据"：打开体征数据输入对话框（Modal）
- "健康档案"：已移至顶部操作区
- "我的病例"：已移至顶部操作区

---

### 3.2 诊断结果展示（集成在对话中）

#### 3.2.1 设计理念

**统一界面架构下的诊断结果展示**：
- ✅ **在对话中展示**：诊断完成后，结果以消息卡片形式展示在对话中
- ✅ **信息面板展示详情**：点击结果卡片，在信息面板或抽屉中显示详细信息
- ✅ **无需页面跳转**：所有操作都在统一界面中完成

#### 3.2.2 对话中的结果展示

**诊断完成后，在对话消息列表中显示结果卡片**：

```tsx
// 对话消息列表中的诊断结果卡片
<Message
  type="system"
  content={
    <DiagnosisResultCard 
      result={diagnosisResult}
      onViewDetail={() => setResultDrawerVisible(true)}
      onSave={handleSaveReport}
      onShare={handleShare}
      onRediagnosis={handleStartNewDiagnosis}
    />
  }
/>
```

**诊断结果卡片（简化版，在对话中显示）**：
```tsx
<Card
  title={
    <Space>
      <span>诊断结果</span>
      <Tag color={result.conclusion.type === 'confirmable' ? 'green' : 'orange'}>
        {result.conclusion.type === 'confirmable' ? '可确证' : '不可确证'}
      </Tag>
    </Space>
  }
  style={{ marginBottom: 16 }}
  extra={
    <Button 
      type="link" 
      onClick={onViewDetail}
    >
      查看详情
    </Button>
  }
>
  <Space direction="vertical" style={{ width: '100%' }} size="middle">
    <Text>根据您的症状和健康档案，我为您做了详细分析：</Text>
    
    {/* 首要假设 */}
    <Card size="small" style={{ backgroundColor: '#f0f9ff', borderColor: '#1890ff' }}>
      <Space direction="vertical" style={{ width: '100%' }} size="small">
        <Text strong style={{ color: '#1890ff' }}>【首要假设】</Text>
        <Text strong>{result.conclusion.primaryHypothesis.disease}</Text>
        <Text type="secondary">
          可能性：{Math.round(result.conclusion.primaryHypothesis.confidence * 100)}%
        </Text>
      </Space>
    </Card>
    
    {/* 必须排除的高危诊断（如果有） */}
    {result.conclusion.mustExcludeDiagnosis && (
      <Alert
        message={
          <Space>
            <Text strong>【必须排除的高危诊断】</Text>
            <Text strong style={{ color: '#ff4d4f' }}>
              {result.conclusion.mustExcludeDiagnosis.disease}
            </Text>
          </Space>
        }
        description={
          <Text>
            虽然可能性较低（{Math.round(result.conclusion.mustExcludeDiagnosis.confidence * 100)}%），
            但一旦漏诊后果严重，需要优先排除
          </Text>
        }
        type="error"
        showIcon
        style={{ marginBottom: 8 }}
      />
    )}
    
    {/* 主要备选诊断 */}
    {result.conclusion.alternativeDiagnoses.length > 0 && (
      <div>
        <Text strong>【主要备选诊断】</Text>
        <List
          size="small"
          dataSource={result.conclusion.alternativeDiagnoses}
          renderItem={(item) => (
            <List.Item>
              <Text>{item.disease}</Text>
              <Text type="secondary">
                （{Math.round(item.confidence * 100)}%）
              </Text>
            </List.Item>
          )}
        />
      </div>
    )}
    
    {/* 操作按钮 */}
    <Space>
      <Button size="small" onClick={onViewDetail}>查看详情</Button>
      <Button size="small" onClick={onSave}>保存报告</Button>
      <Button size="small" onClick={onShare}>分享</Button>
    </Space>
  </Space>
</Card>
```

#### 3.2.3 详细信息展示（信息面板/抽屉）

**点击"查看详情"后，在信息面板或抽屉中显示完整结果**：

**设计理念**：按照系统设计方案的**终点结论包四要素**结构展示：
1. **结论**（可确证/不可确证）
2. **必须排除项状态**（已排除/未排除/需线下排除）
3. **关键依据**（至少三条证据，明确阳性证据和关键阴性证据）
4. **行动与随访**（立即行动、复评时间窗、升级触发条件）

```
┌─────────────────────────────────────────────────────────┐
│  诊断结果详情（信息面板/抽屉区域）                        │
│  ┌───────────────────────────────────────────────────┐ │
│  │  结论（可确证/不可确证标识）                       │ │
│  │  诊断时间：2025-01-15 10:35                        │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  三层分层结构                                      │ │
│  │  【首要假设】（1个）                               │ │
│  │  【主要备选诊断】（1-2个）                         │ │
│  │  【必须排除的高危诊断】（0-1个，特殊标记）         │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  必须排除项状态                                    │ │
│  │  状态：已排除/未排除/需线下排除                    │ │
│  │  排除理由：...                                     │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  关键依据（至少三条证据）                          │ │
│  │  • 阳性证据（支持最可能方向）                      │ │
│  │  • 关键阴性证据（排除其他方向）                    │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  行动与随访                                        │ │
│  │  • 立即行动：...                                   │ │
│  │  • 复评时间窗：3天后                               │ │
│  │  • 升级触发条件：症状加重、出现新症状等            │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  建议检查                                          │ │
│  │  - 优先检查（高优先级）                            │ │
│  │  - 可选检查（中低优先级）                          │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  就医建议                                          │ │
│  │  - 建议科室：心内科                                │ │
│  │  - 就医时机：尽快                                  │ │
│  │  - 就医准备：健康档案、检查报告                    │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  操作按钮                                          │ │
│  │  [保存报告] [分享] [重新诊断]                      │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

#### 3.2.4 诊断结果数据结构（三层分层 + 终点结论包四要素）

**数据结构定义**（按照系统设计方案的三层分层和终点结论包四要素）：

```typescript
// 疾病可能性
interface DiseasePossibility {
  disease: string;                    // 疾病名称
  confidence: number;                 // 可能性分数（0-1）
  supportingEvidence: string[];       // 支持证据
  opposingEvidence: string[];         // 反对证据
  missingInfo?: string[];             // 缺失信息
  inclusionBasis?: string[];          // 入选依据（问题清单中的线索）
}

// 诊断结论结构（三层分层）
interface DiagnosisConclusion {
  type: 'confirmable' | 'unconfirmable';  // 可确证/不可确证
  primaryHypothesis: DiseasePossibility;   // 首要假设（1个）
  alternativeDiagnoses: DiseasePossibility[]; // 主要备选诊断（1-2个）
  mustExcludeDiagnosis?: DiseasePossibility;  // 必须排除的高危诊断（0-1个）
}

// 必须排除项状态
interface ExclusionStatus {
  status: 'excluded' | 'not_excluded' | 'need_offline_exclude';
  reason: string;                     // 排除理由
}

// 关键依据
interface KeyEvidence {
  positiveEvidence: string[];         // 阳性证据（支持最可能方向，至少三条）
  negativeEvidence: string[];         // 关键阴性证据（排除其他方向）
}

// 行动与随访
interface ActionAndFollowUp {
  immediateAction: string;            // 立即行动
  reviewTimeWindow?: string;          // 复评时间窗（例如："3天后"）
  upgradeTriggerConditions: string[]; // 升级触发条件（例如：症状加重、出现新症状）
}

// 就医建议
interface MedicalAdvice {
  department: string;                 // 建议科室
  timing: string;                     // 就医时机
  preparation: {
    documents: string[];              // 就医准备文档
    questions: string[];              // 建议询问医生的问题
  };
  sbarSummary?: string;               // 就医摘要（SBAR格式）
}

// 完整的诊断结果
interface DiagnosisResult {
  diagnosisId: string;
  conclusion: DiagnosisConclusion;    // 结论（三层分层）
  exclusionStatus?: ExclusionStatus;  // 必须排除项状态
  keyEvidence: KeyEvidence;           // 关键依据
  actionAndFollowUp: ActionAndFollowUp; // 行动与随访
  examinationSuggestions: {
    priorityExaminations: ExaminationItem[];
    optionalExaminations: ExaminationItem[];
    explanation?: string;
  };
  medicalAdvice: MedicalAdvice;       // 就医建议
  createdAt: string;
  completedAt?: string;
}
```

**三层分层说明**：
- **首要假设**（1个）：当前信息最能支持、最符合整体表现的方向
- **主要备选诊断**（1-2个）：与首要假设并列需要对比、仍可能成立的方向
- **必须排除的高危诊断**（0-1个）：一旦漏诊后果严重，即使概率不高也必须纳入并优先排除

**终点结论包四要素说明**：
1. **结论**：可确证/不可确证的标识 + 三层分层结构
2. **必须排除项状态**：已排除/未排除/需线下排除 + 排除理由
3. **关键依据**：至少三条证据，明确阳性证据和关键阴性证据
4. **行动与随访**：立即行动、复评时间窗、升级触发条件

#### 3.2.5 三层分层展示组件设计

**完整诊断结果详情组件**：

```typescript
const DiagnosisResultDetail: React.FC<{ result: DiagnosisResult }> = ({ result }) => {
  return (
    <Space direction="vertical" style={{ width: '100%' }} size="large">
      {/* 1. 结论（可确证/不可确证标识） */}
      <Card>
        <Space direction="vertical" style={{ width: '100%' }} size="small">
          <Space>
            <Text strong>诊断结论</Text>
            <Tag color={result.conclusion.type === 'confirmable' ? 'green' : 'orange'}>
              {result.conclusion.type === 'confirmable' ? '可确证' : '不可确证'}
            </Tag>
            <Text type="secondary">诊断时间：{formatDate(result.createdAt)}</Text>
          </Space>
          <Text type="secondary">
            {result.conclusion.type === 'confirmable' 
              ? '证据充分，可确证诊断' 
              : '证据不足以确证，输出最可能方向'}
          </Text>
        </Space>
      </Card>

      {/* 2. 三层分层结构 */}
      <Card title="鉴别诊断分析">
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          {/* 首要假设 */}
          <Card 
            size="small" 
            style={{ backgroundColor: '#f0f9ff', borderColor: '#1890ff' }}
            title={
              <Space>
                <Text strong style={{ color: '#1890ff' }}>【首要假设】</Text>
                <Tag color="blue">最可能</Tag>
              </Space>
            }
          >
            <DiseasePossibilityCard 
              possibility={result.conclusion.primaryHypothesis}
              showDetail={true}
            />
          </Card>

          {/* 主要备选诊断 */}
          {result.conclusion.alternativeDiagnoses.length > 0 && (
            <Card 
              size="small"
              title={
                <Space>
                  <Text strong>【主要备选诊断】</Text>
                  <Tag color="orange">备选</Tag>
                </Space>
              }
            >
              <List
                dataSource={result.conclusion.alternativeDiagnoses}
                renderItem={(item) => (
                  <List.Item>
                    <DiseasePossibilityCard possibility={item} showDetail={false} />
                  </List.Item>
                )}
              />
            </Card>
          )}

          {/* 必须排除的高危诊断 */}
          {result.conclusion.mustExcludeDiagnosis && (
            <Alert
              message={
                <Space>
                  <Text strong style={{ color: '#ff4d4f' }}>【必须排除的高危诊断】</Text>
                  <Tag color="red">高危</Tag>
                </Space>
              }
              description={
                <Space direction="vertical" size="small" style={{ width: '100%' }}>
                  <Text strong style={{ color: '#ff4d4f' }}>
                    {result.conclusion.mustExcludeDiagnosis.disease}
                  </Text>
                  <Text>
                    可能性：{Math.round(result.conclusion.mustExcludeDiagnosis.confidence * 100)}%
                    （虽然可能性较低，但一旦漏诊后果严重，必须优先排除）
                  </Text>
                  <DiseasePossibilityCard 
                    possibility={result.conclusion.mustExcludeDiagnosis}
                    showDetail={true}
                  />
                </Space>
              }
              type="error"
              showIcon
              style={{ marginTop: 16 }}
            />
          )}
        </Space>
      </Card>

      {/* 3. 必须排除项状态 */}
      {result.exclusionStatus && (
        <Card title="必须排除项状态">
          <Space direction="vertical" style={{ width: '100%' }} size="small">
            <Space>
              <Text strong>状态：</Text>
              <Tag 
                color={
                  result.exclusionStatus.status === 'excluded' ? 'green' :
                  result.exclusionStatus.status === 'not_excluded' ? 'orange' : 'red'
                }
              >
                {result.exclusionStatus.status === 'excluded' ? '已排除' :
                 result.exclusionStatus.status === 'not_excluded' ? '未排除' : '需线下排除'}
              </Tag>
            </Space>
            <Text>{result.exclusionStatus.reason}</Text>
          </Space>
        </Card>
      )}

      {/* 4. 关键依据 */}
      <Card title="关键依据">
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div>
            <Text strong style={{ color: '#52c41a' }}>阳性证据（支持最可能方向）：</Text>
            <List
              size="small"
              dataSource={result.keyEvidence.positiveEvidence}
              renderItem={(item) => (
                <List.Item>
                  <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                  <Text>{item}</Text>
                </List.Item>
              )}
            />
          </div>
          {result.keyEvidence.negativeEvidence.length > 0 && (
            <div>
              <Text strong style={{ color: '#ff4d4f' }}>关键阴性证据（排除其他方向）：</Text>
              <List
                size="small"
                dataSource={result.keyEvidence.negativeEvidence}
                renderItem={(item) => (
                  <List.Item>
                    <CloseCircleOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
                    <Text>{item}</Text>
                  </List.Item>
                )}
              />
            </div>
          )}
        </Space>
      </Card>

      {/* 5. 行动与随访 */}
      <Card title="行动与随访">
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div>
            <Text strong>立即行动：</Text>
            <Text>{result.actionAndFollowUp.immediateAction}</Text>
          </div>
          {result.actionAndFollowUp.reviewTimeWindow && (
            <Alert
              message={
                <Space>
                  <CalendarOutlined />
                  <Text strong>复评时间窗：{result.actionAndFollowUp.reviewTimeWindow}</Text>
                </Space>
              }
              description="请在指定时间进行复评，或根据症状变化提前复评"
              type="info"
              showIcon
            />
          )}
          {result.actionAndFollowUp.upgradeTriggerConditions.length > 0 && (
            <Alert
              message={<Text strong>升级触发条件（出现以下情况请立即就医）：</Text>}
              description={
                <List
                  size="small"
                  dataSource={result.actionAndFollowUp.upgradeTriggerConditions}
                  renderItem={(item) => (
                    <List.Item>
                      <WarningOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
                      <Text>{item}</Text>
                    </List.Item>
                  )}
                />
              }
              type="warning"
              showIcon
            />
          )}
        </Space>
      </Card>

      {/* 建议检查和就医建议 */}
      <ExaminationSuggestion data={result.examinationSuggestions} />
      <MedicalAdvice advice={result.medicalAdvice} />
    </Space>
  );
};
```

#### 3.2.6 可能性卡片组件设计（更新）

```typescript
interface DiseasePossibility {
  disease: string;
  confidence: number;
  supportingEvidence: string[];
  opposingEvidence: string[];
  missingInfo?: string[];
  inclusionBasis?: string[];          // 入选依据（问题清单中的线索）
}

interface DiseasePossibilityCardProps {
  possibility: DiseasePossibility;
  showDetail?: boolean;               // 是否显示详细信息
}

const DiseasePossibilityCard: React.FC<DiseasePossibilityCardProps> = ({
  possibility,
  showDetail = false
}) => {
  const { disease, confidence, supportingEvidence, opposingEvidence, missingInfo, inclusionBasis } = possibility;

  if (!showDetail) {
    // 简化显示（用于列表）
    return (
      <Space>
        <Text strong>{disease}</Text>
        <Text type="secondary">（{Math.round(confidence * 100)}%）</Text>
      </Space>
    );
  }

  // 详细显示（用于卡片）
  return (
    <Space direction="vertical" style={{ width: '100%' }} size="small">
      <Space>
        <Text strong>{disease}</Text>
        <Text type="secondary">可能性：{Math.round(confidence * 100)}%</Text>
      </Space>
      
      {/* 入选依据 */}
      {inclusionBasis && inclusionBasis.length > 0 && (
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            入选依据：{inclusionBasis.join('、')}
          </Text>
        </div>
      )}
      
      {/* 支持证据 */}
      {supportingEvidence.length > 0 && (
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            支持证据：{supportingEvidence.join('、')}
          </Text>
        </div>
      )}
      
      {/* 反对证据 */}
      {opposingEvidence.length > 0 && (
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            反对证据：{opposingEvidence.join('、')}
          </Text>
        </div>
      )}
      
      {/* 缺失信息 */}
      {missingInfo && missingInfo.length > 0 && (
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            缺失信息：{missingInfo.join('、')}
          </Text>
        </div>
      )}
    </Space>
  );
};
```

#### 3.2.7 建议检查组件

```typescript
interface ExaminationSuggestionProps {
  priorityExaminations: ExaminationItem[];
  optionalExaminations: ExaminationItem[];
  explanation: string;
}

const ExaminationSuggestion: React.FC<ExaminationSuggestionProps> = ({
  priorityExaminations,
  optionalExaminations,
  explanation
}) => {
  const columns = [
    {
      title: '检查项目',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '目的',
      dataIndex: 'purpose',
      key: 'purpose',
      render: (purpose: string) => {
        const colors = {
          '确诊': 'red',
          '排除': 'orange',
          '评估': 'blue'
        };
        return <Tag color={colors[purpose]}>{purpose}</Tag>;
      }
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      render: (priority: string) => {
        const colors = {
          'high': 'red',
          'medium': 'orange',
          'low': 'blue'
        };
        const texts = {
          'high': '高',
          'medium': '中',
          'low': '低'
        };
        return <Tag color={colors[priority]}>{texts[priority]}</Tag>;
      }
    },
    {
      title: '说明',
      dataIndex: 'reason',
      key: 'reason',
    }
  ];

  return (
    <Card title="建议检查" style={{ marginBottom: 16 }}>
      {explanation && (
        <Alert
          message={explanation}
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}
      
      <Tabs>
        <TabPane 
          tab={
            <Badge count={priorityExaminations.length} offset={[10, 0]}>
              <span>优先检查</span>
            </Badge>
          } 
          key="priority"
        >
          <Table
            dataSource={priorityExaminations}
            columns={columns}
            pagination={false}
            size="small"
          />
        </TabPane>
        
        <TabPane 
          tab={
            <Badge count={optionalExaminations.length} offset={[10, 0]}>
              <span>可选检查</span>
            </Badge>
          } 
          key="optional"
        >
          <Table
            dataSource={optionalExaminations}
            columns={columns}
            pagination={false}
            size="small"
          />
        </TabPane>
      </Tabs>
    </Card>
  );
};
```

#### 3.2.8 就医建议组件

```typescript
interface MedicalAdviceProps {
  advice: MedicalAdvice;
}

const MedicalAdvice: React.FC<MedicalAdviceProps> = ({ advice }) => {
  const { department, timing, preparation, sbarSummary } = advice;
  const getTimingColor = () => {
    if (timing.includes('尽快') || timing.includes('立即')) return 'red';
    if (timing.includes('2-3天')) return 'orange';
    return 'blue';
  };

  return (
    <Card title="就医建议" style={{ marginBottom: 16 }}>
      <Row gutter={16}>
        <Col span={12}>
          <Statistic
            title="建议科室"
            value={department}
            prefix={<MedicineBoxOutlined />}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="就医时机"
            value={timing}
            valueStyle={{ color: getTimingColor() }}
          />
        </Col>
      </Row>

      <Divider />

      <Space direction="vertical" style={{ width: '100%' }}>
        <div>
          <Text strong>就医准备：</Text>
          <List
            size="small"
            dataSource={preparation.documents}
            renderItem={(item) => (
              <List.Item>
                <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                <Text>{item}</Text>
              </List.Item>
            )}
          />
        </div>

        <div>
          <Text strong>建议询问医生的问题：</Text>
          <List
            size="small"
            dataSource={preparation.questions}
            renderItem={(item, index) => (
              <List.Item>
                <Text>{index + 1}. {item}</Text>
              </List.Item>
            )}
          />
        </div>

        {sbarSummary && (
          <div>
            <Text strong>就医摘要（SBAR格式）：</Text>
            <Alert
              message={sbarSummary}
              type="info"
              style={{ marginTop: 8 }}
            />
            <Button
              type="link"
              icon={<CopyOutlined />}
              onClick={() => copyToClipboard(sbarSummary)}
            >
              复制摘要
            </Button>
          </div>
        )}
      </Space>
    </Card>
  );
};
```

---

### 3.3 健康档案和病例管理（集成设计）

#### 3.3.1 健康档案管理

**访问方式**：
- 信息面板顶部操作区："健康档案"按钮
- 快速操作区："查看健康档案"按钮（可选）

**展示方式**：通过抽屉（Drawer）从右侧滑出

**健康档案内容**：
```tsx
<Drawer
  title="健康档案"
  placement="right"
  width={600}
  open={profileDrawerVisible}
  onClose={() => setProfileDrawerVisible(false)}
>
  <Card title="基本信息">
    <Descriptions column={2}>
      <Descriptions.Item label="年龄">{profile.age}</Descriptions.Item>
      <Descriptions.Item label="性别">{profile.gender}</Descriptions.Item>
      <Descriptions.Item label="身高">{profile.height} cm</Descriptions.Item>
      <Descriptions.Item label="体重">{profile.weight} kg</Descriptions.Item>
    </Descriptions>
  </Card>
  
  <Card title="既往病史" style={{ marginTop: 16 }}>
    <List dataSource={profile.medicalHistory} />
  </Card>
  
  <Card title="用药史" style={{ marginTop: 16 }}>
    <List dataSource={profile.medicationHistory} />
  </Card>
  
  <Card title="过敏史" style={{ marginTop: 16 }}>
    <List dataSource={profile.allergies} />
  </Card>
  
  <Button 
    type="primary" 
    block 
    style={{ marginTop: 16 }}
    onClick={() => setEditModalVisible(true)}
  >
    编辑健康档案
  </Button>
</Drawer>
```

#### 3.3.2 病例/诊断记录管理

**访问方式**：
- 信息面板顶部操作区："我的病例"按钮

**展示方式**：通过抽屉（Drawer）从右侧滑出

**病例列表功能**：
```tsx
<Drawer
  title="我的病例"
  placement="right"
  width={700}
  open={caseDrawerVisible}
  onClose={() => setCaseDrawerVisible(false)}
>
  <List
    dataSource={diagnosisRecords}
    renderItem={(item) => (
      <List.Item
        actions={[
          <Button
            key="view"
            type="link"
            onClick={() => handleViewCase(item.diagnosisId, item.status)}
          >
            {item.status === 'completed' ? '查看结果' : '继续诊断'}
          </Button>
        ]}
      >
        <List.Item.Meta
          title={item.chiefComplaint || '未填写主诉'}
          description={
            <Space>
              <Tag>{getTypeText(item.diagnosisType)}</Tag>
              <Text type="secondary">{formatDate(item.createdAt)}</Text>
              {getStatusTag(item.status)}
            </Space>
          }
        />
      </List.Item>
    )}
  />
</Drawer>
```

**功能说明**：
- **查看已完成诊断**：点击"查看结果" → 在信息面板中展示诊断结果详情
- **继续未完成诊断**：点击"继续诊断" → 恢复对话状态，继续问诊
- **从病例启动新诊断**：可基于历史病例创建新的诊断

---

### 3.4 检查功能（集成到对话中）

#### 3.4.1 设计理念

**统一界面架构下的检查功能**：
- ✅ **报告上传**：通过输入区域上传，或在快速操作中打开上传对话框
- ✅ **检查建议**：诊断完成后，在诊断结果中展示检查建议
- ✅ **检查历史**：集成在病例记录中，可通过病例列表查看

#### 3.4.2 报告上传和解读流程

**方式1：通过输入区域上传**
```tsx
// 输入区域支持文件上传
<Input
  type="file"
  accept="image/*,.pdf"
  onChange={handleFileUpload}
  style={{ display: 'none' }}
  ref={fileInputRef}
/>
<Button 
  icon={<UploadOutlined />}
  onClick={() => fileInputRef.current?.click()}
>
  上传报告
</Button>
```

**方式2：通过快速操作按钮**
```tsx
// 快速操作中的"上传检查报告"按钮
<Button 
  block 
  icon={<UploadOutlined />}
  onClick={() => setUploadModalVisible(true)}
>
  上传检查报告
</Button>

// 上传对话框
<Modal
  title="上传检查报告"
  open={uploadModalVisible}
  onCancel={() => setUploadModalVisible(false)}
  footer={null}
>
  <Upload.Dragger
    customRequest={handleUpload}
    accept="image/*,.pdf"
  >
    <p>点击或拖拽文件上传</p>
  </Upload.Dragger>
</Modal>
```

**上传后的处理流程**：
1. 上传文件 → OCR识别 → 显示识别结果（在对话中）
2. 自动解读 → 显示解读结果（在对话中）
3. 如有异常 → 自动询问是否需要进一步诊断
4. 检查记录 → 自动保存到病例中

---

### 3.5 CDP可视化页面（CDPVisualizationPage）- 新增

#### 3.5.1 业务说明

**页面说明**：展示CDP（临床决策包）的演变过程，包括推理路径可视化（DR.KNOWS核心）、证据链可视化、八个脑区的执行结果等。

#### 3.5.2 页面结构

```typescript
// CDPVisualizationPage.tsx
const CDPVisualizationPage: React.FC<{ cdpId: string }> = ({ cdpId }) => {
  return (
    <Layout>
      <Content>
        <Card title="CDP可视化" extra={<Button onClick={handleReplay}>回放演变过程</Button>}>
          <Tabs>
            {/* 推理路径可视化（DR.KNOWS核心） */}
            <TabPane tab="推理路径" key="reasoning-paths">
              <ReasoningPathsVisualization cdpId={cdpId} />
            </TabPane>
            
            {/* 证据链可视化 */}
            <TabPane tab="证据链" key="evidence-chain">
              <EvidenceChainVisualization cdpId={cdpId} />
            </TabPane>
            
            {/* 八个脑区执行结果 */}
            <TabPane tab="脑区执行结果" key="brain-results">
              <BrainResultsVisualization cdpId={cdpId} />
            </TabPane>
            
            {/* CDP版本历史 */}
            <TabPane tab="版本历史" key="versions">
              <CDPVersionHistory cdpId={cdpId} />
            </TabPane>
          </Tabs>
        </Card>
      </Content>
    </Layout>
  );
};
```

#### 3.5.3 推理路径可视化组件（ReasoningPathsVisualization）- DR.KNOWS核心

```tsx
// ReasoningPathsVisualization.tsx
const ReasoningPathsVisualization: React.FC<{ cdpId: string }> = ({ cdpId }) => {
  const { reasoningPaths } = useCDP(cdpId);
  
  return (
    <Card>
      <Typography.Title level={4}>推理路径（DR.KNOWS核心）</Typography.Title>
      <List
        dataSource={reasoningPaths}
        renderItem={(path) => (
          <List.Item>
            <Card size="small" style={{ width: '100%' }}>
              {/* 路径图 */}
              <Graph
                nodes={path.nodes}
                edges={path.relationships}
                layout="hierarchical"
              />
              
              {/* 路径描述 */}
              <Typography.Text>{path.description}</Typography.Text>
              
              {/* 路径评分 */}
              <Space>
                <Tag>相关性: {path.relevanceScore.toFixed(2)}</Tag>
                <Tag>证据强度: {path.evidenceStrength.toFixed(2)}</Tag>
                <Tag>综合评分: {path.compositeScore.toFixed(2)}</Tag>
              </Space>
            </Card>
          </List.Item>
        )}
      />
    </Card>
  );
};
```

#### 3.5.4 证据链可视化组件（EvidenceChainVisualization）

```tsx
// EvidenceChainVisualization.tsx
const EvidenceChainVisualization: React.FC<{ cdpId: string }> = ({ cdpId }) => {
  const { evidenceChain } = useCDP(cdpId);
  
  return (
    <Card>
      <Typography.Title level={4}>证据链</Typography.Title>
      <Timeline>
        {evidenceChain.map((evidence, index) => (
          <Timeline.Item 
            key={index}
            color={evidence.type === 'support' ? 'green' : evidence.type === 'oppose' ? 'red' : 'gray'}
          >
            <Space direction="vertical">
              <Text strong>{evidence.evidence}</Text>
              <Tag>{evidence.strength}</Tag>
              <Text type="secondary">影响：{evidence.affectedDisease}</Text>
            </Space>
          </Timeline.Item>
        ))}
      </Timeline>
    </Card>
  );
};
```

#### 3.5.5 八个脑区执行结果可视化组件（BrainResultsVisualization）

```tsx
// BrainResultsVisualization.tsx
const BrainResultsVisualization: React.FC<{ cdpId: string }> = ({ cdpId }) => {
  const { brainResults } = useCDP(cdpId);
  
  return (
    <Card>
      <Typography.Title level={4}>八个脑区执行结果</Typography.Title>
      <Row gutter={16}>
        {/* 脑区0：健康状态判定 */}
        <Col span={12}>
          <Card size="small" title="脑区0：健康状态判定">
            <Descriptions size="small" column={1}>
              <Descriptions.Item label="工作态">
                <Tag color={brainResults.brain0.workMode === 'clinical_mode' ? 'red' : 'green'}>
                  {brainResults.brain0.workMode}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="风险等级">
                {brainResults.brain0.riskLevel}
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>
        
        {/* 脑区A：病例理解 */}
        <Col span={12}>
          <Card size="small" title="脑区A：病例理解">
            <Text>识别概念数：{brainResults.brainA.concepts.length}</Text>
          </Card>
        </Col>
        
        {/* 脑区B：主动问诊 */}
        <Col span={12}>
          <Card size="small" title="脑区B：主动问诊">
            <Progress 
              percent={brainResults.brainB.completeness * 100} 
              format={(percent) => `${percent}%`}
            />
          </Card>
        </Col>
        
        {/* 脑区C：鉴别诊断（DR.KNOWS核心） */}
        <Col span={12}>
          <Card size="small" title="脑区C：鉴别诊断（DR.KNOWS核心）">
            <Text>推理路径数：{brainResults.brainC.reasoningPaths.length}</Text>
            <Text>鉴别诊断数：{brainResults.brainC.ddx.length}</Text>
          </Card>
        </Col>
        
        {/* 其他脑区... */}
      </Row>
    </Card>
  );
};
```

---

### 3.6 检查页面（ExaminationPage）- 已移除

**说明**：在统一界面架构下，检查页面（ExaminationPage）已移除，相关功能已集成到诊断对话页面中：
- **报告上传**：通过输入区域或快速操作按钮上传
- **检查建议**：在诊断结果中展示
- **检查历史**：集成在病例记录中

---

**以下为历史设计参考（已整合到统一界面中）**：

#### 3.5.1 文件上传组件（已集成到对话中）

```typescript
const ExaminationUploadPanel: React.FC = () => {
  const [uploading, setUploading] = useState(false);
  const [ocrResult, setOcrResult] = useState<any>(null);

  const handleUpload = async (file: File) => {
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await examinationApi.upload(file, {
        userId: currentUser.id,
        examinationType: 'blood_test'
      });
      
      setOcrResult(response.data);
    } catch (error) {
      message.error('上传失败，请重试');
    } finally {
      setUploading(false);
    }
  };

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="large">
      <Card title="上传检查报告">
        <Upload.Dragger
          name="file"
          multiple={false}
          accept="image/*,.pdf"
          customRequest={({ file }) => handleUpload(file as File)}
          showUploadList={false}
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">
            {uploading ? '上传中...' : '点击或拖拽文件到此区域上传'}
          </p>
          <p className="ant-upload-hint">
            支持图片（JPG、PNG）和PDF文件，最大10MB
          </p>
        </Upload.Dragger>
      </Card>

      {ocrResult && (
        <Card title="识别结果">
          <Tabs>
            <TabPane tab="原始文本" key="raw">
              <Input.TextArea
                value={ocrResult.rawText}
                readOnly
                rows={10}
                style={{ fontFamily: 'monospace' }}
              />
              <Button
                icon={<CopyOutlined />}
                onClick={() => copyToClipboard(ocrResult.rawText)}
                style={{ marginTop: 8 }}
              >
                复制文本
              </Button>
            </TabPane>
            
            <TabPane tab="结构化数据" key="structured">
              <Table
                dataSource={ocrResult.structuredData?.indicators || []}
                columns={[
                  {
                    title: '指标名称',
                    dataIndex: 'name',
                    key: 'name',
                    width: 150,
                  },
                  {
                    title: '数值',
                    dataIndex: 'value',
                    key: 'value',
                    width: 100,
                    align: 'right',
                  },
                  {
                    title: '单位',
                    dataIndex: 'unit',
                    key: 'unit',
                    width: 100,
                  },
                  {
                    title: '参考范围',
                    dataIndex: 'normalRange',
                    key: 'normalRange',
                    width: 120,
                  },
                  {
                    title: '状态',
                    dataIndex: 'status',
                    key: 'status',
                    width: 80,
                    render: (status: string) => (
                      <Tag color={status === 'normal' ? 'green' : 'red'}>
                        {status === 'normal' ? '正常' : '异常'}
                      </Tag>
                    )
                  }
                ]}
                pagination={false}
                size="small"
              />
            </TabPane>
          </Tabs>
        </Card>
      )}

      {ocrResult && (
        <Card
          title="解读结果"
          extra={
            <Button
              type="primary"
              onClick={handleInterpret}
              loading={interpreting}
            >
              开始解读
            </Button>
          }
        >
          {/* 解读结果展示 */}
        </Card>
      )}
    </Space>
  );
};
```

---

### 3.6 历史记录（已集成到病例管理）

**说明**：在统一界面架构下，独立的历史页面（DiagnosisHistoryPage）已移除，历史记录功能已集成到"我的病例"抽屉中，通过信息面板顶部的"我的病例"按钮访问。

**历史记录功能包括**：
- ✅ 诊断历史列表（已完成、进行中、已取消）
- ✅ 查看诊断结果详情
- ✅ 继续未完成的诊断
- ✅ 基于历史病例创建新诊断

---

**以下为历史设计参考（已整合到统一界面中）**：

#### 3.6.1 历史页面设计（参考）

#### 3.4.1 页面布局

```typescript
// DiagnosisHistoryPage.tsx
const DiagnosisHistoryPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('diagnosis');

  return (
    <Page title="诊断历史">
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane
            tab={
              <Badge count={diagnosisCount} offset={[10, 0]}>
                <span>诊断历史</span>
              </Badge>
            }
            key="diagnosis"
          >
            <DiagnosisHistoryList />
          </TabPane>
          
          <TabPane
            tab={
              <Badge count={examinationCount} offset={[10, 0]}>
                <span>检查历史</span>
              </Badge>
            }
            key="examination"
          >
            <ExaminationHistoryList />
          </TabPane>
        </Tabs>
      </Card>
    </Page>
  );
};
```

#### 3.4.2 诊断历史列表

```typescript
const DiagnosisHistoryList: React.FC = () => {
  const { data, loading, pagination } = useDiagnosisHistory();

  const getStatusTag = (status: string) => {
    const config = {
      'collecting': { color: 'blue', text: '收集中' },
      'questioning': { color: 'orange', text: '追问中' },
      'analyzing': { color: 'processing', text: '分析中' },
      'completed': { color: 'success', text: '已完成' },
      'cancelled': { color: 'default', text: '已取消' }
    };
    const { color, text } = config[status] || config['collecting'];
    return <Tag color={color}>{text}</Tag>;
  };

  return (
    <List
      loading={loading}
      dataSource={data?.list || []}
      pagination={pagination}
      renderItem={(item) => (
        <List.Item
          actions={[
            <Button
              type="link"
              onClick={() => router.push(`/diagnosis/result/${item.id}`)}
            >
              查看详情
            </Button>,
            <Button
              type="link"
              onClick={() => router.push(`/diagnosis?diagnosisId=${item.id}`)}
            >
              重新诊断
            </Button>
          ]}
        >
          <List.Item.Meta
            avatar={<Avatar icon={<FileTextOutlined />} />}
            title={
              <Space>
                <Text strong>{item.chiefComplaint || '未填写主诉'}</Text>
                {getStatusTag(item.status)}
              </Space>
            }
            description={
              <Space>
                <Tag>{item.diagnosisType === 'symptom' ? '症状诊断' : 
                      item.diagnosisType === 'examination' ? '检查诊断' : 
                      '综合诊断'}</Tag>
                <Text type="secondary">
                  {formatDate(item.createdAt)}
                </Text>
                {item.completedAt && (
                  <>
                    <Text type="secondary">·</Text>
                    <Text type="secondary">
                      完成于 {formatDate(item.completedAt)}
                    </Text>
                  </>
                )}
              </Space>
            }
          />
        </List.Item>
      )}
    />
  );
};
```

---

## 四、组件设计

### 4.1 对话消息组件（Message）

```typescript
// components/diagnosis/Message.tsx
import { Avatar, Button, Space, Typography } from 'antd';
import { UserOutlined, DoctorOutlined } from '@ant-design/icons';

interface MessageProps {
  type: 'user' | 'system' | 'question';
  content: string;
  timestamp: Date;
  options?: string[]; // 追问问题的选项
  highlight?: boolean; // 是否高亮显示
}

const Message: React.FC<MessageProps> = ({
  type,
  content,
  timestamp,
  options,
  highlight
}) => {
  const isUser = type === 'user';
  
  return (
    <div 
      className={`message message-${type} ${highlight ? 'message-highlight' : ''}`}
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        marginBottom: 16,
        padding: '0 16px'
      }}
    >
      {!isUser && (
        <Avatar
          icon={<DoctorOutlined />}
          style={{ marginRight: 8, backgroundColor: '#1890ff' }}
        />
      )}
      
      <div
        style={{
          maxWidth: '70%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: isUser ? 'flex-end' : 'flex-start'
        }}
      >
        <div
          className="message-bubble"
          style={{
            padding: '12px 16px',
            borderRadius: 8,
            backgroundColor: isUser ? '#1890ff' : highlight ? '#e6f7ff' : '#fff',
            color: isUser ? '#fff' : '#000',
            boxShadow: '0 1px 2px rgba(0,0,0,0.1)',
            border: highlight ? '2px solid #1890ff' : '1px solid #e8e8e8'
          }}
        >
          <Typography.Text style={{ color: isUser ? '#fff' : 'inherit' }}>
            {content}
          </Typography.Text>
          
          {options && options.length > 0 && (
            <Space style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap' }}>
              {options.map((option) => (
                <Button
                  key={option}
                  size="small"
                  type="default"
                  onClick={() => handleOptionClick(option)}
                >
                  {option}
                </Button>
              ))}
            </Space>
          )}
        </div>
        
        <Typography.Text
          type="secondary"
          style={{ fontSize: 12, marginTop: 4 }}
        >
          {formatTime(timestamp)}
        </Typography.Text>
      </div>
      
      {isUser && (
        <Avatar
          style={{ marginLeft: 8, backgroundColor: '#87d068' }}
        >
          我
        </Avatar>
      )}
    </div>
  );
};
```

### 4.2 信息完整度进度条（CompletenessProgress）

**信息完整度阈值说明**（与系统设计方案保持一致）：
- **< 60%**：信息不足，需要继续收集信息（最低要求为60%才能进行诊断分析）
- **60% - 70%**：信息基本完整，建议继续补充（达到60%最低要求，但建议补充到70%停止追问）
- **≥ 70%**：信息完整，可以开始分析（达到停止追问阈值，可以开始分析）
- **≥ 80%**：信息完整，诊断准确性更高（理想状态）

```typescript
// components/diagnosis/CompletenessProgress.tsx
import { Progress, Typography } from 'antd';

interface CompletenessProgressProps {
  completeness: number; // 0-1
  status: 'collecting' | 'analyzing' | 'completed';
}

const CompletenessProgress: React.FC<CompletenessProgressProps> = ({
  completeness,
  status
}) => {
  const getStatus = (): 'exception' | 'active' | 'success' => {
    if (completeness < 0.6) return 'exception';
    if (completeness < 0.7) return 'active';
    if (completeness < 0.8) return 'active';
    return 'success';
  };

  const getText = (): string => {
    if (completeness < 0.6) return '信息不足，需要补充更多信息';
    if (completeness < 0.7) return '信息基本完整，建议继续补充';
    if (completeness < 0.8) return '信息完整，可以开始分析';
    return '信息完整，诊断准确性更高';
  };

  const getStrokeColor = () => {
    if (completeness < 0.6) return '#ff4d4f';
    if (completeness < 0.7) return '#faad14';
    if (completeness < 0.8) return '#faad14';
    return '#52c41a';
  };

  return (
    <div style={{ padding: '16px', backgroundColor: '#fafafa' }}>
      <Progress
        percent={Math.round(completeness * 100)}
        status={getStatus()}
        strokeColor={getStrokeColor()}
        format={(percent) => `${percent}%`}
        style={{ marginBottom: 8 }}
      />
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        {getText()}
      </Typography.Text>
    </div>
  );
};
```

---

## 五、交互流程设计

### 5.1 诊断对话完整流程

```
用户进入诊断页面
    ↓
【系统】显示欢迎消息
    "您好，我是智能诊断助手，请描述您的不适症状"
    ↓
用户输入："我最近胸痛"
    ↓
【系统】显示追问
    "胸痛需要重视。请问疼痛持续多久了？"
    [显示进度：信息完整度 20% → 30%]
    ↓
用户回答："3天了"
    ↓
【系统】显示追问（带选项）
    "疼痛是持续的还是阵发性的？"
    [选项按钮：持续的] [阵发性的] [不确定]
    [显示进度：信息完整度 30% → 50%]
    ↓
用户点击："阵发性的"
    ↓
【系统】继续追问
    "疼痛时有没有其他症状，比如出汗、气短？"
    [显示进度：信息完整度 50% → 70%]
    ↓
用户回答："有点气短"
    ↓
【系统】检查信息完整度
    [显示进度：信息完整度 70% → 85%]
    [完整度达到70%，停止追问，可以开始分析]
    ↓
【系统】显示
    "好的，我了解了。让我为您分析一下可能的原因..."
    [显示加载动画：分析中...]
    ↓
【系统】显示诊断结果
    [跳转到诊断结果页面]
```

### 5.2 检查报告上传流程

```
用户进入检查页面
    ↓
选择"上传报告"Tab
    ↓
拖拽或选择文件（图片/PDF）
    ↓
显示上传进度
    [上传中... 50%]
    ↓
上传完成
    [识别中...]
    ↓
显示OCR识别结果
    [Tab1: 原始文本]
    [Tab2: 结构化数据（表格）]
    ↓
用户确认识别结果
    [点击"识别正确"或"重新识别"]
    ↓
点击"开始解读"按钮
    [解读中...]
    ↓
显示解读结果
    - 异常指标列表
    - 临床意义
    - 可能的疾病方向
    - 建议
```

---

## 六、UI/UX设计规范

### 6.1 颜色规范

**主色调**：
- 主色：`#1890ff`（蓝色，专业、可信）
- 成功：`#52c41a`（绿色）
- 警告：`#faad14`（橙色）
- 错误：`#ff4d4f`（红色）
- 信息：`#1890ff`（蓝色）

**对话颜色**：
- 用户消息背景：`#1890ff`（蓝色）
- 用户消息文字：`#ffffff`（白色）
- 系统消息背景：`#ffffff`（白色）
- 系统消息文字：`#000000`（黑色）
- 追问问题背景：`#e6f7ff`（浅蓝色，高亮）
- 追问问题边框：`#1890ff`（蓝色，2px）

**状态颜色**：
- 可能性较高：`#ff4d4f`（红色）
- 可能性中等：`#faad14`（橙色）
- 可能性较低：`#1890ff`（蓝色）

### 6.2 字体规范

- 页面标题：20px，加粗（`font-weight: 600`）
- 卡片标题：16px，加粗
- 正文：14px，常规
- 辅助文字：12px，灰色（`#8c8c8c`）
- 行高：1.5

### 6.3 间距规范

- 页面边距：24px
- 卡片间距：16px
- 组件间距：12px
- 内容间距：8px
- 消息间距：16px

### 6.4 图标使用

- 医生头像：`<UserOutlined />` 或自定义医生图标
- 消息状态：
  - `CheckCircleOutlined`（已收集）
  - `ClockCircleOutlined`（待补充）
  - `QuestionCircleOutlined`（缺失信息）
- 操作图标：
  - `UploadOutlined`（上传）
  - `FileTextOutlined`（报告）
  - `HeartOutlined`（体征）
  - `HistoryOutlined`（历史）
  - `CopyOutlined`（复制）
  - `MedicineBoxOutlined`（科室）

---

## 七、响应式设计

### 7.1 断点设置

```typescript
const breakpoints = {
  xs: 0,      // 手机（<576px）
  sm: 576,    // 大手机（≥576px）
  md: 768,    // 平板（≥768px）
  lg: 992,    // 小桌面（≥992px）
  xl: 1200,   // 桌面（≥1200px）
  xxl: 1600   // 大桌面（≥1600px）
};
```

### 7.2 布局适配

**手机端（<768px）**：
- 对话区域：全屏（24列）
- 信息面板：通过抽屉（Drawer）从右侧滑出
- 消息气泡：最大宽度80%
- 按钮：全宽显示

**平板端（768px-1199px）**：
- 对话区域：16列（66.7%）
- 信息面板：8列（33.3%），可折叠
- 消息气泡：最大宽度70%

**桌面端（≥1200px）**：
- 对话区域：17列（70.8%）
- 信息面板：7列（29.2%），固定显示
- 消息气泡：最大宽度70%

---

## 八、状态管理

### 8.1 状态定义

**前端状态定义**（Zustand Store）：
- `idle`: 初始状态（尚未创建诊断记录，前端本地状态）
- `collecting`: 信息收集中
- `questioning`: 追问中
- `analyzing`: 分析中
- `completed`: 已完成

**数据库状态定义**（与系统设计方案保持一致）：
- `collecting`: 信息收集中
- `questioning`: 追问中
- `analyzing`: 分析中
- `completed`: 已完成
- `cancelled`: 已取消（用户主动取消诊断）

**CDP状态定义**（`cdpStatus`，用于推理流程编排与持久化，对齐《技术架构设计-CDP数据与状态管理》7.3）：
- `initial`：CDP刚创建，等待健康状态判定
- `wellness_mode`：健康管理态（A路径）
- `clinical_mode_collecting`：临床诊疗态-信息收集
- `clinical_mode_diagnosing`：临床诊疗态-诊断中
- `clinical_mode_managing`：临床诊疗态-处置中
- `completed`：完成（终点结论包已输出）
- `follow_up`：随访阶段（等待复评/升级/回退）

**前端状态与CDP状态的关系（建议映射）**：
- `idle`：尚未创建诊断记录（通常也未创建CDP）
- `collecting` / `questioning`：多对应 `clinical_mode_collecting`（信息补全阶段）
- `analyzing`：多对应 `clinical_mode_diagnosing`（推理/生成阶段）
- `completed`：对应 `completed`（结论包生成完成）

**说明**：
- `idle`状态是前端本地状态，表示尚未创建诊断记录时的状态
- 当创建诊断记录后，前端状态会与数据库状态同步
- `cancelled`状态仅存在于数据库中，表示已创建记录后用户主动取消

### 8.2 使用Zustand管理状态

```typescript
// stores/diagnosisStore.ts
import create from 'zustand';
import { diagnosisApi } from '@/services/api/diagnosisApi';

interface DiagnosisState {
  // 状态
  currentDiagnosis: DiagnosisRecord | null;
  messages: Message[];
  completeness: number;
  status: 'idle' | 'collecting' | 'questioning' | 'analyzing' | 'completed';
  
  // Actions
  startDiagnosis: (request: DiagnosisRequest) => Promise<void>;
  sendMessage: (content: string) => Promise<void>;
  answerQuestion: (answer: string) => Promise<void>;
  getDiagnosisResult: (id: string) => Promise<void>;
  resetDiagnosis: () => void;
}

export const useDiagnosisStore = create<DiagnosisState>((set, get) => ({
  currentDiagnosis: null,
  messages: [],
  completeness: 0,
  status: 'idle',
  
  startDiagnosis: async (request) => {
    try {
      const response = await diagnosisApi.start(request);
      const data = response.data;
      
      set({
        currentDiagnosis: data,
        status: data.status,
        completeness: data.completeness || 0,
        messages: [
          {
            type: 'system',
            content: '您好，我是智能诊断助手，请描述您的不适症状',
            timestamp: new Date()
          }
        ]
      });
      
      // 如果有追问问题，添加到消息列表
      if (data.question) {
        set(state => ({
          messages: [
            ...state.messages,
            {
              type: 'question',
              content: data.question.question,
              options: data.question.options,
              timestamp: new Date(),
              highlight: true
            }
          ]
        }));
      }
    } catch (error) {
      console.error('开始诊断失败', error);
      throw error;
    }
  },
  
  sendMessage: async (content: string) => {
    const state = get();
    if (!state.currentDiagnosis) return;
    
    // 添加用户消息
    const userMessage: Message = {
      type: 'user',
      content,
      timestamp: new Date()
    };
    
    set(state => ({
      messages: [...state.messages, userMessage]
    }));
    
    try {
      // 调用API回答
      const response = await diagnosisApi.answer(
        state.currentDiagnosis.diagnosisId,
        content
      );
      
      const data = response.data;
      
      // 更新状态
      set({
        status: data.status,
        completeness: data.completeness || state.completeness
      });
      
      // 如果有追问问题，添加到消息列表
      if (data.question) {
        const questionMessage: Message = {
          type: 'question',
          content: data.question.question,
          options: data.question.options,
          timestamp: new Date(),
          highlight: true
        };
        
        set(state => ({
          messages: [...state.messages, questionMessage]
        }));
      } else if (data.status === 'analyzing') {
        // 开始分析
        const systemMessage: Message = {
          type: 'system',
          content: '好的，我了解了。让我为您分析一下可能的原因...',
          timestamp: new Date()
        };
        
        set(state => ({
          messages: [...state.messages, systemMessage]
        }));
      }
    } catch (error) {
      console.error('发送消息失败', error);
      throw error;
    }
  },
  
  answerQuestion: async (answer: string) => {
    await get().sendMessage(answer);
  },
  
  getDiagnosisResult: async (id: string) => {
    try {
      const response = await diagnosisApi.getResult(id);
      set({
        currentDiagnosis: response.data,
        status: response.data.status
      });
    } catch (error) {
      console.error('获取诊断结果失败', error);
      throw error;
    }
  },
  
  resetDiagnosis: () => {
    set({
      currentDiagnosis: null,
      messages: [],
      completeness: 0,
      status: 'idle'
    });
  }
}));
```

---

## 九、API集成

### 9.1 API客户端

#### 9.1.1 健康状态判定API（脑区0整合入口，包含P0 Step 1-5）

```typescript
// services/api/healthStateAssessmentApi.ts
import { apiClient } from '@/utils/apiClient';
import type {
  HealthStateAssessmentRequest,
  HealthStateAssessmentResponse
} from '@/types/healthStateAssessment';

export const healthStateAssessmentApi = {
  assess: async (
    request: HealthStateAssessmentRequest
  ): Promise<ApiResponse<HealthStateAssessmentResponse>> => {
    return apiClient.post('/api/v1/health-state-assessment/assess', request);
  }
};
```

```typescript
// services/api/diagnosisApi.ts
import { apiClient } from '@/utils/apiClient';
import type {
  DiagnosisRequest,
  DiagnosisResponse,
  UserAnswer,
  DiagnosisResult
} from '@/types/diagnosis';

export const diagnosisApi = {
  // 开始诊断
  start: async (request: DiagnosisRequest): Promise<ApiResponse<DiagnosisResponse>> => {
    return apiClient.post('/api/v1/diagnosis/start', request);
  },
  
  // 回答追问
  answer: async (
    diagnosisId: string,
    answer: string,
    answerType?: string
  ): Promise<ApiResponse<DiagnosisResponse>> => {
    return apiClient.post(`/api/v1/diagnosis/${diagnosisId}/answer`, {
      answer,
      answerType: answerType || 'symptom'
    });
  },
  
  // 获取诊断结果
  getResult: async (id: string): Promise<ApiResponse<DiagnosisResult>> => {
    return apiClient.get(`/api/v1/diagnosis/${id}`);
  },
  
  // 获取诊断历史
  getHistory: async (
    userId: string,
    page: number = 1,
    pageSize: number = 20
  ): Promise<ApiResponse<PaginatedResponse<DiagnosisRecord>>> => {
    return apiClient.get('/api/v1/diagnosis/history', {
      params: { userId, page, pageSize }
    });
  },
  
  // 执行诊断分析
  analyze: async (diagnosisId: string): Promise<ApiResponse<void>> => {
    return apiClient.post(`/api/v1/diagnosis/${diagnosisId}/analyze`);
  }
};
```

### 9.2 检查API

```typescript
// services/api/examinationApi.ts
export const examinationApi = {
  // 上传检查报告
  upload: async (
    file: File,
    params: {
      userId: string;
      examinationType: string;
    }
  ): Promise<ApiResponse<ExaminationRecord>> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', params.userId);
    formData.append('examinationType', params.examinationType);
    
    return apiClient.post('/api/v1/examination/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },
  
  // OCR识别
  ocrRecognize: async (file: File): Promise<ApiResponse<OcrResult>> => {
    const formData = new FormData();
    formData.append('file', file);
    
    return apiClient.post('/api/v1/examination/ocr', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },
  
  // 解读检查结果
  interpret: async (examinationId: string): Promise<ApiResponse<InterpretationResult>> => {
    return apiClient.post(`/api/v1/examination/${examinationId}/interpret`);
  },
  
  // 设计检查方案
  designPlan: async (request: ExaminationPlanRequest): Promise<ApiResponse<ExaminationPlan>> => {
    return apiClient.post('/api/v1/examination/plan', request);
  },
  
  // 获取检查历史
  getHistory: async (
    userId: string,
    page: number = 1,
    pageSize: number = 20
  ): Promise<ApiResponse<PaginatedResponse<ExaminationRecord>>> => {
    return apiClient.get('/api/v1/examination/history', {
      params: { userId, page, pageSize }
    });
  }
};
```

---

## 十、页面路由配置（统一界面架构）

### 10.1 路由定义

**统一界面架构下的路由设计**：

```typescript
// router/routes.tsx
import { lazy } from 'react';
import { RouteObject } from 'react-router-dom';

const DiagnosisPage = lazy(() => import('@/features/diagnosis/pages/DiagnosisPage'));

export const diagnosisRoutes: RouteObject[] = [
  {
    path: '/diagnosis',
    element: <DiagnosisPage />,
    meta: {
      title: '智能诊断',
      icon: 'MedicineBoxOutlined'
    }
  }
];
```

**说明**：
- ✅ **单一路由**：只有诊断对话页面（DiagnosisPage）
- ✅ **功能集成**：所有功能（诊断、检查、档案、病例）都在同一页面中
- ✅ **无需跳转**：通过抽屉、对话框、信息面板等方式访问各个功能
- ❌ **移除的页面**：
  - DiagnosisResultPage（诊断结果在对话中展示）
  - ExaminationPage（检查功能集成到对话中）
  - DiagnosisHistoryPage（历史记录集成到"我的病例"中）

---

## 十一、参考产品特点总结

### 11.1 百度健康智能AI预问诊

**优点**：
- ✅ 对话流畅自然，像真实医生问诊
- ✅ 进度指示清晰，用户知道进行到哪一步
- ✅ 结构化信息展示，清晰明了
- ✅ 追问问题带选项按钮，方便用户选择

**借鉴**：
- 进度指示器设计（信息完整度进度条）
- 对话流程优化（追问问题带选项）
- 信息摘要展示（已收集信息卡片）

### 11.2 春雨医生/丁香医生

**优点**：
- ✅ 专业的医疗风格（配色、图标）
- ✅ 清晰的诊断结果展示（可能性、证据、建议）
- ✅ 友好的用户引导
- ✅ 就医建议详细实用（科室、时机、准备）

**借鉴**：
- 诊断结果卡片设计（可能性等级、证据展示）
- 就医建议展示方式（SBAR格式摘要）
- 专业术语解释（帮助用户理解）

### 11.3 好大夫在线

**优点**：
- ✅ 问诊记录清晰
- ✅ 检查报告上传方便
- ✅ 历史记录管理完善

**借鉴**：
- 历史记录列表设计
- 文件上传交互优化
- 报告查看和管理

---

## 十二、设计亮点

### 12.1 像医生一样的对话体验

- **医生身份标识**：医生头像和"智能诊断助手"身份
- **温暖专业的语言**：使用温暖、专业的语言风格
- **主动追问**：像真正的医生一样主动询问
- **选项按钮**：追问问题提供选项按钮，方便用户选择

### 12.2 信息可视化

- **进度指示**：信息完整度进度条，清晰显示进度
- **诊断结果可视化**：可能性、置信度、证据清晰展示
- **状态标签**：不同状态使用不同颜色的标签

### 12.3 多端适配

- **响应式设计**：PC、平板、手机完美适配
- **移动端优化**：手机端使用抽屉展示信息面板
- **触摸友好**：按钮大小、间距适合触摸操作

### 12.4 无障碍设计

- **大字体支持**：支持字体大小调整
- **高对比度**：文字和背景对比度符合WCAG标准
- **键盘导航**：支持键盘操作
- **屏幕阅读器**：支持屏幕阅读器

---

## 十三、页面交互细节

### 13.1 消息滚动

- 新消息到达时自动滚动到底部
- 支持手动滚动查看历史消息
- 保持滚动位置（如果用户正在查看历史消息）

### 13.2 输入体验

- 输入框支持多行输入（自动调整高度）
- 支持回车发送（Shift+Enter换行）
- 支持语音输入（可选功能）
- 支持图片上传（可选功能）

### 13.3 加载状态

- 发送消息时显示"发送中..."
- 分析时显示"分析中..."和加载动画
- 上传文件时显示上传进度

### 13.4 错误处理

- 网络错误：显示错误提示，允许重试
- 超时错误：显示超时提示，允许重新发送
- 业务错误：显示友好错误信息

---

## 十四、性能优化

### 14.1 代码分割

- 使用React.lazy进行路由级别的代码分割
- 组件级别的懒加载

### 14.2 虚拟滚动

- 消息列表使用虚拟滚动（如果消息数量很多）
- 历史记录列表使用分页加载

### 14.3 图片优化

- 上传的图片进行压缩
- 使用WebP格式（如果支持）
- 图片懒加载

---

## 十五、主动引导与系统整合设计

### 15.1 主动引导设计

**核心理念**：系统主动引导用户，而不是被动等待用户操作。

#### 15.1.1 主动触发诊断

**场景1：智能家庭医生监测到异常**
```tsx
// 主动提醒组件
<Alert
  message="您的血压处于正常高值，值得关注"
  description={
    <>
      <p>系统检测到您的血压为 130/85 mmHg，处于正常高值。</p>
      <Button 
        type="primary" 
        onClick={() => handleTriggerDiagnosis('bp_abnormal')}
      >
        立即进行健康评估
      </Button>
    </>
  }
  type="warning"
  showIcon
  closable
/>
```

**场景2：用户上传检查报告后**
```tsx
// 自动触发诊断
useEffect(() => {
  if (examinationResult?.hasAbnormalIndicators) {
    // 自动询问症状
    showSystemMessage({
      type: 'question',
      content: '您的检查报告显示有异常指标，请问您最近有什么不适症状吗？',
      onConfirm: () => triggerDiagnosis('examination_abnormal')
    });
  }
}, [examinationResult]);
```

#### 15.1.2 主动引导用户补充信息

```tsx
// 主动引导卡片
<Card 
  title="建议补充信息" 
  extra={<Button size="small">立即补充</Button>}
>
  <List
    dataSource={missingInfo}
    renderItem={(item) => (
      <List.Item>
        <Space>
          <Icon type="info-circle" />
          <span>{item.description}</span>
          <Button 
            type="link" 
            size="small"
            onClick={() => handleFillInfo(item.type)}
          >
            去补充
          </Button>
        </Space>
      </List.Item>
    )}
  />
</Card>
```

### 15.2 系统整合设计

**核心理念**：与智能家庭医生协作，整合健康中心、健康监测等模块，提供整体方案。

#### 15.2.1 诊断结果展示（整合智能家庭医生的照护计划）

```tsx
// 诊断结果页面（整合展示）
const DiagnosisResultPage: React.FC = () => {
  const { diagnosisResult, carePlan } = useDiagnosisStore();
  
  return (
    <div>
      {/* 医学判断结论（来自智能诊断） */}
      <Card title="医学判断结论" style={{ marginBottom: 16 }}>
        <MedicalJudgmentDisplay result={diagnosisResult.medicalJudgment} />
      </Card>
      
      {/* 照护计划（来自智能家庭医生） */}
      <Card title="您的健康管理方案" style={{ marginBottom: 16 }}>
        <CarePlanDisplay plan={carePlan} />
      </Card>
      
      {/* 整合其他模块的建议 */}
      <Card title="相关建议">
        <Tabs>
          <TabPane tab="生活方式" key="lifestyle">
            <LifestyleSuggestions />
          </TabPane>
          <TabPane tab="健康监测" key="monitoring">
            <MonitoringPlan />
          </TabPane>
          <TabPane tab="健康科普" key="education">
            <HealthEducation />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};
```

#### 15.2.2 对"没病的人"的展示

```tsx
// "没病"的判断结果展示
const NoDiseaseResultDisplay: React.FC<{ result: MedicalJudgment }> = ({ result }) => {
  return (
    <div>
      {/* 判断结论 */}
      <Alert
        message={result.conclusion}
        description={result.reason}
        type="success"
        showIcon
        style={{ marginBottom: 16 }}
      />
      
      {/* 为什么安全 */}
      <Card title="为什么是安全的" style={{ marginBottom: 16 }}>
        <List>
          {result.reasonDetails.map((detail, index) => (
            <List.Item key={index}>
              <Space>
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
                <span>{detail}</span>
              </Space>
            </List.Item>
          ))}
        </List>
      </Card>
      
      {/* 需要关注的风险 */}
      {result.riskAssessment && (
        <Card title="需要关注的风险" style={{ marginBottom: 16 }}>
          <Alert
            message={result.riskAssessment}
            type="info"
            showIcon
          />
        </Card>
      )}
      
      {/* 后续建议 */}
      <Card title="后续建议">
        <List>
          {result.followUpSuggestions.needRetest && (
            <List.Item>
              <Space>
                <CalendarOutlined />
                <span>
                  建议在 {result.followUpSuggestions.retestCycle} 后复测
                  {result.followUpSuggestions.retestItems.join('、')}
                </span>
              </Space>
            </List.Item>
          )}
          <List.Item>
            <Space>
              <InfoCircleOutlined />
              <span>{result.followUpSuggestions.whenToSeeDoctor}</span>
            </Space>
          </List.Item>
        </List>
      </Card>
      
      {/* 避免过度医疗的提示 */}
      {result.examinationSuggestions.unnecessaryExams.length > 0 && (
        <Card title="专业建议" style={{ marginTop: 16 }}>
          <Alert
            message="当前阶段不需要做以下检查"
            description={
              <List
                dataSource={result.examinationSuggestions.unnecessaryExams}
                renderItem={(exam) => (
                  <List.Item>
                    <Space>
                      <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                      <span>{exam.name}</span>
                    </Space>
                  </List.Item>
                )}
              />
            }
            type="info"
            showIcon
          />
          <p style={{ marginTop: 8, color: '#666' }}>
            {result.examinationSuggestions.unnecessaryReason}
          </p>
        </Card>
      )}
    </div>
  );
};
```

#### 15.2.3 与智能家庭医生的协作展示

```tsx
// 协作流程展示
const CollaborationFlow: React.FC = () => {
  return (
    <Timeline>
      <Timeline.Item color="blue">
        <p>智能家庭医生监测到异常</p>
        <p className="text-secondary">主动提醒您进行健康评估</p>
      </Timeline.Item>
      <Timeline.Item color="blue">
        <p>智能诊断进行医学判断</p>
        <p className="text-secondary">分析您的症状和检查结果</p>
      </Timeline.Item>
      <Timeline.Item color="green">
        <p>智能家庭医生制定照护计划</p>
        <p className="text-secondary">基于医学判断，为您制定个性化方案</p>
      </Timeline.Item>
      <Timeline.Item color="green">
        <p>整合各模块，提供完整方案</p>
        <p className="text-secondary">健康监测、生活方式、健康科普等</p>
      </Timeline.Item>
    </Timeline>
  );
};
```

### 15.3 主动引导的交互设计

#### 15.3.1 主动提醒

```tsx
// 主动提醒组件
const ProactiveAlert: React.FC = () => {
  const { alerts } = useProactiveAlerts();
  
  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      {alerts.map((alert) => (
        <Alert
          key={alert.id}
          message={alert.title}
          description={alert.description}
          type={alert.type}
          action={
            <Space>
              <Button size="small" onClick={() => handleAction(alert)}>
                {alert.actionText}
              </Button>
              <Button size="small" type="text" onClick={() => dismissAlert(alert.id)}>
                稍后提醒
              </Button>
            </Space>
          }
          closable
          onClose={() => dismissAlert(alert.id)}
        />
      ))}
    </Space>
  );
};
```

#### 15.3.2 智能推荐

```tsx
// 智能推荐组件
const SmartRecommendations: React.FC = () => {
  const { recommendations } = useSmartRecommendations();
  
  return (
    <Card title="为您推荐">
      <List
        dataSource={recommendations}
        renderItem={(item) => (
          <List.Item
            actions={[
              <Button 
                key="action" 
                type="primary" 
                size="small"
                onClick={() => handleRecommendation(item)}
              >
                立即查看
              </Button>
            ]}
          >
            <List.Item.Meta
              avatar={<Avatar icon={item.icon} />}
              title={item.title}
              description={item.description}
            />
          </List.Item>
        )}
      />
    </Card>
  );
};
```

---

## 十六、统一界面架构总结

### 16.1 架构特点

**核心设计理念**：所有功能（智能诊断、健康检查、报告上传、健康档案、病例记录等）都集成在同一个统一界面中，以对话为核心，提供无缝的用户体验。

**主要变更**：
1. ✅ **移除顶部导航栏**：不再有页面级别的导航切换
2. ✅ **单一页面架构**：所有功能都在诊断对话页面（DiagnosisPage）中
3. ✅ **对话为核心**：诊断、报告上传都在对话中完成
4. ✅ **信息面板集成**：健康档案、病例记录、诊断结果通过信息面板和抽屉访问
5. ✅ **功能整合**：
   - 检查建议 → 在诊断结果中展示
   - 报告上传 → 通过输入区域或快速操作按钮
   - 健康档案 → 通过信息面板顶部按钮访问（抽屉）
   - 病例记录 → 通过信息面板顶部按钮访问（抽屉）
   - 诊断结果 → 在对话中展示，详细信息在信息面板/抽屉中

### 16.2 功能访问方式

| 功能 | 访问方式 | 展示方式 |
|------|---------|---------|
| 开始新诊断 | 信息面板顶部按钮 | 清空对话，开始新对话 |
| 我的病例 | 信息面板顶部按钮 | 抽屉（Drawer）显示病例列表 |
| 健康档案 | 信息面板顶部按钮 | 抽屉（Drawer）显示档案详情 |
| 上传报告 | 输入区域 / 快速操作按钮 | 对话框（Modal）上传，对话中处理 |
| 补充体征 | 快速操作按钮 | 对话框（Modal）输入 |
| 诊断结果 | 诊断完成后自动展示 | 对话中显示结果卡片，点击查看详情在信息面板/抽屉中 |
| 查看历史 | 通过"我的病例"访问 | 抽屉中显示病例列表 |

### 16.3 用户体验优势

- ✅ **无缝体验**：无需页面跳转，所有操作在同一界面完成
- ✅ **上下文保持**：对话上下文始终可见，不会丢失
- ✅ **快速访问**：常用功能通过信息面板快速访问
- ✅ **清晰导航**：功能组织清晰，用户容易理解
- ✅ **响应式设计**：支持PC、平板、手机多端

---

**文档版本**：v4.0（基于DR.KNOWS的八个脑区架构）  
**创建日期**：2025年1月  
**更新日期**：2025年1月  
**文档定位**：AI医生系统的前端页面设计（统一界面架构、八个脑区功能展示、CDP可视化、推理路径可视化等）  
**参考产品**：百度健康、春雨医生、丁香医生、好大夫在线等  
**参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》  
**设计基础**：基于DR.KNOWS论文的八个脑区架构设计

**更新说明（v4.0）**：
- ✅ 新增健康状态判定页面（脑区0）
- ✅ 新增CDP可视化页面（推理路径可视化、证据链可视化、八个脑区执行结果）
- ✅ 添加推理路径可视化组件（DR.KNOWS核心）
- ✅ 添加八个脑区执行结果可视化组件
- ✅ 更新诊断结果页面，展示八个脑区的执行结果
- ✅ 添加三层分层结构设计（首要假设、主要备选诊断、必须排除的高危诊断）
- ✅ 添加终点结论包四要素的完整设计（结论、必须排除项状态、关键依据、行动与随访）

