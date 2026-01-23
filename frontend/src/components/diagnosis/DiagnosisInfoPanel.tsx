import { Card, Progress, List, Button, Space, Typography, message, Steps, Tag, Descriptions } from 'antd'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  HeartOutlined,
  FileTextOutlined,
  PlusOutlined,
  UserOutlined,
  EditOutlined,
  EyeOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import { useDiagnosisStore } from '@/stores/diagnosisStore'

const { Text } = Typography

interface DiagnosisInfoPanelProps {
  onStartNewDiagnosis?: () => void
  onViewCases?: () => void
  onViewProfile?: () => void
  onOpenStructuredIntake?: () => void
  onViewCDP?: (cdpId: string) => void
}

const DiagnosisInfoPanel: React.FC<DiagnosisInfoPanelProps> = ({
  onStartNewDiagnosis,
  onViewCases,
  onViewProfile,
  onOpenStructuredIntake,
  onViewCDP,
}) => {
  const { completeness, collectedInfo, status, diagnosisId, resetDiagnosis, workMode, cdpId, messages, healthAssessmentDone } = useDiagnosisStore()
  
  // 从消息中获取最新的健康状态评估结果
  const latestAssessment = messages
    .filter(msg => msg.type === 'assessment' && msg.assessment)
    .map(msg => msg.assessment)
    .pop()

  // 临床诊疗态的已收集信息列表
  const clinicalCollectedInfoList = [
    {
      label: '主诉',
      value: collectedInfo.chiefComplaint,
      collected: !!collectedInfo.chiefComplaint,
    },
    {
      label: '持续时间',
      value: collectedInfo.duration,
      collected: !!collectedInfo.duration,
    },
    {
      label: '严重程度',
      value: collectedInfo.severity ? `${collectedInfo.severity}/10` : undefined,
      collected: collectedInfo.severity !== undefined,
    },
    {
      label: '频率',
      value: collectedInfo.frequency,
      collected: !!collectedInfo.frequency,
    },
    {
      label: '部位',
      value: collectedInfo.location,
      collected: !!collectedInfo.location,
    },
    {
      label: '伴随症状',
      value: collectedInfo.accompanyingSymptoms?.join('、'),
      collected: !!collectedInfo.accompanyingSymptoms?.length,
    },
  ]

  // 健康管理态的已收集信息列表（从wellnessPlan.profile中提取）
  const wellnessProfile = latestAssessment?.wellnessPlan?.profile || {}
  
  // 格式化基本信息显示
  const formatBasicInfo = (basicInfo: any) => {
    if (!basicInfo || Object.keys(basicInfo).length === 0) return undefined
    const parts: string[] = []
    if (basicInfo.age) parts.push(`年龄: ${basicInfo.age}岁`)
    if (basicInfo.gender) parts.push(`性别: ${basicInfo.gender}`)
    if (basicInfo.bmi) parts.push(`BMI: ${basicInfo.bmi}`)
    return parts.length > 0 ? parts.join('，') : undefined
  }
  
  // 格式化生命体征显示
  const formatVitalSigns = (vitalSigns: any) => {
    if (!vitalSigns || Object.keys(vitalSigns).length === 0) return undefined
    const parts: string[] = []
    if (vitalSigns.bp && vitalSigns.bp.systolic && vitalSigns.bp.diastolic) {
      parts.push(`血压: ${vitalSigns.bp.systolic}/${vitalSigns.bp.diastolic} mmHg`)
    }
    if (vitalSigns.heartRate) {
      parts.push(`心率: ${vitalSigns.heartRate} bpm`)
    }
    if (vitalSigns.temperature) {
      parts.push(`体温: ${vitalSigns.temperature} ℃`)
    }
    return parts.length > 0 ? parts.join('，') : undefined
  }
  
  const wellnessCollectedInfoList = [
    {
      label: '用户输入',
      value: wellnessProfile.userInput,
      collected: !!wellnessProfile.userInput,
    },
    {
      label: '基本信息',
      value: formatBasicInfo(wellnessProfile.basicInfo),
      collected: !!(wellnessProfile.basicInfo && Object.keys(wellnessProfile.basicInfo).length > 0),
    },
    {
      label: '症状信息',
      value: Array.isArray(wellnessProfile.symptoms) && wellnessProfile.symptoms.length > 0
        ? wellnessProfile.symptoms.join('、')
        : undefined,
      collected: !!(Array.isArray(wellnessProfile.symptoms) && wellnessProfile.symptoms.length > 0),
    },
    {
      label: '生命体征',
      value: formatVitalSigns(wellnessProfile.vitalSigns),
      collected: !!(wellnessProfile.vitalSigns && Object.keys(wellnessProfile.vitalSigns).length > 0),
    },
  ]

  // 根据工作态选择显示的信息列表
  const collectedInfoList = workMode === 'wellness_mode' 
    ? wellnessCollectedInfoList 
    : clinicalCollectedInfoList

  const handleStartNewDiagnosis = () => {
    resetDiagnosis()
    onStartNewDiagnosis?.()
  }

  const handleViewCases = () => {
    onViewCases?.()
  }

  const handleViewProfile = () => {
    onViewProfile?.()
  }

  const handleAddVitalSigns = () => {
    // 打开体征数据输入对话框
    // TODO: 实现体征数据输入功能
    message.info('补充体征数据功能开发中')
  }

  // 判断是否已开始结构化提问（有诊断ID或状态不是idle）
  const hasStartedStructuredQuestioning = status !== 'idle' && diagnosisId !== null

  // 5步AI循证诊断流程进度指示器（临床诊疗态时显示）
  const getDiagnosisStepStatus = (step: number): 'wait' | 'process' | 'finish' | 'error' => {
    if (status === 'idle' || !diagnosisId) return 'wait'
    if (status === 'collecting' || status === 'questioning') {
      if (step <= 3) return 'finish'
      if (step === 4) return 'process'
      return 'wait'
    }
    if (status === 'analyzing') {
      if (step <= 4) return 'finish'
      if (step === 5) return 'process'
      return 'wait'
    }
    if (status === 'completed') {
      return 'finish'
    }
    return 'wait'
  }

  const getStepName = (step: number): string => {
    const stepNames = {
      1: '识别问题',
      2: '构建候选集',
      3: '组织分流路径',
      4: '采集证据',
      5: '输出结论包',
    }
    return stepNames[step as keyof typeof stepNames] || ''
  }

  const getCurrentStep = (): number => {
    if (status === 'idle' || !diagnosisId) return 0
    if (status === 'collecting' || status === 'questioning') return 4
    if (status === 'analyzing') return 5
    if (status === 'completed') return 5
    return 0
  }

  // 健康筛查流程进度指示器相关函数（健康管理态时显示）
  const getWellnessStepStatus = (step: number): 'wait' | 'process' | 'finish' | 'error' => {
    const plan = latestAssessment?.wellnessPlan
    if (!plan) return 'wait'
    
    switch (step) {
      case 1: // 需求分类
        return plan.demand_type ? 'finish' : 'wait'
      case 2: // 收集健康画像
        if (plan.profile) return 'finish'
        if (plan.demand_type) return 'process'
        return 'wait'
      case 3: // 执行分支
        if (plan.branch_result) return 'finish'
        if (plan.profile) return 'process'
        return 'wait'
      case 4: // 生成统一结果
        if (plan.unified_result) return 'finish'
        if (plan.branch_result) return 'process'
        return 'wait'
      case 5: // 设置随访
        if (plan.followup_plan || plan.next_review_date) return 'finish'
        if (plan.unified_result) return 'process'
        return 'wait'
      default:
        return 'wait'
    }
  }

  const getWellnessStepName = (step: number): string => {
    const stepNames = {
      1: '需求分类',
      2: '收集健康画像',
      3: '执行分支',
      4: '生成统一结果',
      5: '设置随访',
    }
    return stepNames[step as keyof typeof stepNames] || ''
  }

  const getCurrentWellnessStep = (): number => {
    const plan = latestAssessment?.wellnessPlan
    if (!plan) return -1
    
    if (plan.followup_plan || plan.next_review_date) return 4 // A5完成
    if (plan.unified_result) return 3 // A4完成
    if (plan.branch_result) return 2 // A3完成
    if (plan.profile) return 1 // A2完成
    if (plan.demand_type) return 0 // A1完成
    return -1 // 未开始
  }

  // 获取风险等级颜色
  const getRiskLevelColor = (level?: string) => {
    switch (level) {
      case 'L1':
        return 'red'
      case 'L2':
        return 'orange'
      case 'L3':
        return 'blue'
      case 'L4':
        return 'green'
      default:
        return 'default'
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* 状态信息卡片（健康状态判定完成后显示） */}
      {healthAssessmentDone && (workMode || cdpId) && (
        <Card 
          title={
            <Space>
              <InfoCircleOutlined />
              <span>当前状态</span>
            </Space>
          }
          size="small"
        >
          <Descriptions column={1} size="small">
            {workMode && (
              <Descriptions.Item label="工作态">
                <Tag color={workMode === 'clinical_mode' ? 'red' : 'green'}>
                  {workMode === 'clinical_mode' ? '临床诊疗态' : '健康管理态'}
                </Tag>
              </Descriptions.Item>
            )}
            {latestAssessment?.riskLevel && (
              <Descriptions.Item label="风险等级">
                <Tag color={getRiskLevelColor(latestAssessment.riskLevel)}>
                  {latestAssessment.riskLevel}
                </Tag>
              </Descriptions.Item>
            )}
            {cdpId && (
              <Descriptions.Item label="CDP ID">
                <Text copyable={{ text: cdpId }} style={{ fontSize: 12 }}>
                  {cdpId}
                </Text>
                {onViewCDP && (
                  <Button
                    type="link"
                    size="small"
                    icon={<EyeOutlined />}
                    onClick={() => onViewCDP(cdpId)}
                    style={{ marginLeft: 8, padding: 0 }}
                  >
                    查看
                  </Button>
                )}
              </Descriptions.Item>
            )}
            {latestAssessment?.entryAssessment?.pathSelected && (
              <Descriptions.Item label="路径选择">
                <Tag color={
                  latestAssessment.entryAssessment.pathSelected === 'A' ? 'blue' :
                  latestAssessment.entryAssessment.pathSelected === 'B' ? 'red' : 'default'
                }>
                  {latestAssessment.entryAssessment.pathSelected === 'A' ? 'A路径（健康筛查）' :
                   latestAssessment.entryAssessment.pathSelected === 'B' ? 'B路径（症状诊断）' : '退出流程'}
                </Tag>
              </Descriptions.Item>
            )}
          </Descriptions>
        </Card>
      )}

      {/* ① 信息完整度卡片（当前进度） */}
      <Card title="信息完整度">
        {hasStartedStructuredQuestioning ? (
          <>
            <Progress
              percent={Math.round(completeness * 100)}
              status={completeness < 0.6 ? 'exception' : completeness < 0.7 ? 'active' : completeness < 0.8 ? 'active' : 'success'}
              strokeColor={
                completeness < 0.6
                  ? '#ff4d4f'
                  : completeness < 0.7
                    ? '#faad14'
                    : completeness < 0.8
                      ? '#faad14'
                      : '#52c41a'
              }
              format={(percent) => `${percent}%`}
            />
            <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 8 }}>
              {completeness < 0.6
                ? '信息不足，需要补充更多信息（最低要求为60%才能进行诊断分析）'
                : completeness < 0.7
                  ? '信息基本完整，建议继续补充（达到60%最低要求，建议补充到70%停止追问）'
                  : completeness < 0.8
                    ? '信息完整，可以开始分析（达到停止追问阈值，可以开始分析）'
                    : '信息完整，诊断准确性更高（理想状态）'}
            </Text>
          </>
        ) : (
          // 初始状态：灰色占位条，避免0%的视觉打击
          <div>
            <Progress
              percent={0}
              strokeColor="#d9d9d9"
              showInfo={false}
            />
            <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 8, color: '#8c8c8c' }}>
              未开始评估
            </Text>
          </div>
        )}
      </Card>

      {/* 5步AI循证诊断流程进度指示器（临床诊疗态时显示） */}
      {workMode === 'clinical_mode' && hasStartedStructuredQuestioning && (
        <Card title="诊断流程进度" style={{ marginBottom: 16 }}>
          <Steps
            current={getCurrentStep()}
            direction="horizontal"
            size="small"
            style={{ marginBottom: 16 }}
          >
            <Steps.Step
              title="识别问题"
              description="Step 1"
              status={getDiagnosisStepStatus(1)}
            />
            <Steps.Step
              title="构建候选集"
              description="Step 2"
              status={getDiagnosisStepStatus(2)}
            />
            <Steps.Step
              title="组织分流路径"
              description="Step 3"
              status={getDiagnosisStepStatus(3)}
            />
            <Steps.Step
              title="采集证据"
              description="Step 4"
              status={getDiagnosisStepStatus(4)}
            />
            <Steps.Step
              title="输出结论包"
              description="Step 5"
              status={getDiagnosisStepStatus(5)}
            />
          </Steps>
          <Text type="secondary" style={{ fontSize: 12 }}>
            当前步骤：{getStepName(getCurrentStep()) || '未开始'}
          </Text>
          {cdpId && onViewCDP && (
            <div style={{ marginTop: 8 }}>
              <Button
                type="link"
                size="small"
                icon={<EyeOutlined />}
                onClick={() => onViewCDP(cdpId)}
              >
                查看CDP可视化
              </Button>
            </div>
          )}
        </Card>
      )}

      {/* 健康筛查流程进度指示器（健康管理态时显示） */}
      {workMode === 'wellness_mode' && healthAssessmentDone && latestAssessment?.wellnessPlan && (
        <Card title="健康筛查流程进度" style={{ marginBottom: 16 }}>
          <Steps
            current={(() => {
              const step = getCurrentWellnessStep()
              return step < 0 ? 0 : Math.min(step + 1, 4)
            })()}
            direction="horizontal"
            size="small"
            style={{ marginBottom: 16 }}
          >
            <Steps.Step
              title="需求分类"
              description="Step 1"
              status={getWellnessStepStatus(1)}
            />
            <Steps.Step
              title="收集健康画像"
              description="Step 2"
              status={getWellnessStepStatus(2)}
            />
            <Steps.Step
              title="执行分支"
              description="Step 3"
              status={getWellnessStepStatus(3)}
            />
            <Steps.Step
              title="生成统一结果"
              description="Step 4"
              status={getWellnessStepStatus(4)}
            />
            <Steps.Step
              title="设置随访"
              description="Step 5"
              status={getWellnessStepStatus(5)}
            />
          </Steps>
          <Text type="secondary" style={{ fontSize: 12 }}>
            当前步骤：{(() => {
              const currentStep = getCurrentWellnessStep()
              if (currentStep < 0) return '未开始'
              if (currentStep >= 4) return '已完成'
              const nextStepName = getWellnessStepName(currentStep + 2) // currentStep是已完成步骤索引，+2表示下一个步骤号
              return nextStepName || '进行中'
            })()}
          </Text>
        </Card>
      )}

      {/* ② 已收集信息摘要（缺什么） */}
      {(hasStartedStructuredQuestioning || (workMode === 'wellness_mode' && healthAssessmentDone)) && (
        <Card 
          title="已收集信息"
          extra={
            onOpenStructuredIntake && workMode === 'clinical_mode' && (
              <Button
                type="primary"
                size="small"
                icon={<EditOutlined />}
                onClick={onOpenStructuredIntake}
                style={{ marginRight: -8 }}
              >
                编辑信息
              </Button>
            )
          }
        >
          <List
            size="small"
            dataSource={collectedInfoList}
            renderItem={(item) => (
              <List.Item>
                {item.collected ? (
                  <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                ) : (
                  <ClockCircleOutlined style={{ color: '#faad14', marginRight: 8 }} />
                )}
                <Text delete={!item.collected}>{item.label}</Text>
                {item.collected && item.value && (
                  <Text type="secondary">：{item.value}</Text>
                )}
              </List.Item>
            )}
          />
        </Card>
      )}

      {/* ③ 快速操作（补数据） */}
      <Card title="快速操作">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Button
            block
            icon={<HeartOutlined />}
            onClick={handleAddVitalSigns}
          >
            补充体征数据
          </Button>
        </Space>
      </Card>

      {/* ④ 功能入口（新诊断 / 病例） */}
      <Card style={{ marginTop: 0 }}>
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
            onClick={handleViewCases}
          >
            我的病例
          </Button>
          <Button
            block
            icon={<UserOutlined />}
            onClick={handleViewProfile}
          >
            健康档案
          </Button>
        </Space>
      </Card>
    </div>
  )
}

export default DiagnosisInfoPanel

