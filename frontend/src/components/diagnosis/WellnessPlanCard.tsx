import { Card, Space, Typography } from 'antd'
import { CheckCircleOutlined } from '@ant-design/icons'

const { Text, Title } = Typography

interface WellnessPlanCardProps {
  plan: {
    riskManagement?: string
    lifestyleAdvice?: string
    followUpPlan?: string
    // 健康筛查流程结果（A1-A5）
    demand_type?: string
    profile?: any
    branch_result?: any
    unified_result?: any
    followup_plan?: any
    next_review_date?: string
    summary?: string
    [key: string]: any
  }
}

const WellnessPlanCard: React.FC<WellnessPlanCardProps> = ({ plan }) => {
  // 优先使用健康筛查流程的结果（A4生成的summary和A5的followup_plan）
  // 如果没有，则使用基础的健康管理计划字段
  const riskManagement = plan.riskManagement || plan.summary || '当前健康状况良好，建议保持良好生活习惯'
  const lifestyleAdvice = plan.lifestyleAdvice || '保持健康饮食、规律运动、充足睡眠'
  const followUpPlan = plan.followUpPlan || plan.followup_plan?.reminder || plan.next_review_date 
    ? `建议${plan.next_review_date || '3个月后'}复查，或出现新症状时及时咨询`
    : '建议6个月后复查，或出现新症状时及时咨询'

  return (
    <Card title="健康管理计划" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <div>
          <Title level={5}>
            <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
            风险管理
          </Title>
          <Text>{riskManagement}</Text>
        </div>

        <div>
          <Title level={5}>
            <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
            生活方式建议
          </Title>
          <Text>{lifestyleAdvice}</Text>
        </div>

        <div>
          <Title level={5}>
            <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
            随访计划
          </Title>
          <Text>{followUpPlan}</Text>
        </div>
      </Space>
    </Card>
  )
}

export default WellnessPlanCard

