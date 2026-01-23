import { Card, Tag, Space, Alert, Descriptions, Typography } from 'antd'
import type { HealthStateAssessmentResult } from '@/types/diagnosis'

const { Text } = Typography

interface HealthStateAssessmentCardProps {
  result: HealthStateAssessmentResult
}

const HealthStateAssessmentCard: React.FC<HealthStateAssessmentCardProps> = ({ result }) => {
  const { workMode, riskLevel, assessmentReason, redFlags, entryAssessment } = result

  const getRiskLevelColor = (level: string) => {
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

  // 判断是否有高危信号
  const hasRedFlags = redFlags.length > 0
  // 判断是否为高风险（L1或L2）
  const isHighRisk = riskLevel === 'L1' || riskLevel === 'L2'

  return (
    <Card 
      title="健康状态评估" 
      style={{ 
        marginBottom: 16,
        // 提升视觉重量：更强的背景对比和阴影
        backgroundColor: hasRedFlags || isHighRisk ? '#fff5f5' : '#f0f9ff',
        border: hasRedFlags || isHighRisk ? '2px solid #ff4d4f' : '2px solid #1890ff',
        boxShadow: '0 4px 16px rgba(0,0,0,0.12)',
        borderRadius: '12px',
      }}
      styles={{
        header: {
          backgroundColor: hasRedFlags || isHighRisk ? '#fff1f0' : '#e6f7ff',
          borderBottom: hasRedFlags || isHighRisk ? '1px solid #ffccc7' : '1px solid #91d5ff',
          borderRadius: '12px 12px 0 0',
          padding: '16px 24px',
        },
        body: {
          padding: '24px',
        }
      }}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 工作态显示 */}
        <div>
          <Text strong style={{ marginRight: 8 }}>工作态：</Text>
          <Tag color={workMode === 'clinical_mode' ? 'red' : 'green'}>
            {workMode === 'clinical_mode' ? '临床诊疗态' : '健康管理态'}
          </Tag>
        </div>

        {/* 风险等级 */}
        <Descriptions column={1} size="small">
          <Descriptions.Item label="风险等级">
            <Tag color={getRiskLevelColor(riskLevel)}>{riskLevel}</Tag>
          </Descriptions.Item>
        </Descriptions>

        {/* 如果有高危信号，将蓝色提示收进红色卡内部作为二级说明 */}
        {hasRedFlags ? (
          <Alert
            message="发现高危信号，建议立即就医"
            type="error"
            description={
              <Space direction="vertical" size="small" style={{ width: '100%', marginTop: 12 }}>
                {redFlags.map((flag, index) => (
                  <Text key={index}>• {flag}</Text>
                ))}
                {/* 将判定原因作为二级说明收进红色卡内部 */}
                <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid #ffccc7' }}>
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    {assessmentReason}
                  </Text>
                </div>
              </Space>
            }
            showIcon
            style={{ marginTop: 8 }}
          />
        ) : (
          // 如果没有高危信号，显示蓝色信息提示
          <Alert message={assessmentReason} type="info" showIcon />
        )}

        {/* 入口判定结果展示（可折叠） */}
        {entryAssessment && (
          <div style={{ marginTop: 16, padding: 12, backgroundColor: '#fafafa', borderRadius: 8, border: '1px solid #e8e8e8' }}>
            <Text strong style={{ fontSize: 13, color: '#666' }}>入口判定结果（P0模块）</Text>
            <Descriptions column={1} size="small" style={{ marginTop: 8 }}>
              <Descriptions.Item label="用户输入">
                <Text>{entryAssessment.userInput || '无'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="症状状态">
                <Tag color={
                  entryAssessment.symptomStatus === 'has_symptom' ? 'orange' :
                  entryAssessment.symptomStatus === 'no_symptom' ? 'green' : 'default'
                }>
                  {entryAssessment.symptomStatus === 'has_symptom' ? '有症状' :
                   entryAssessment.symptomStatus === 'no_symptom' ? '无症状' : '不确定'}
                </Tag>
              </Descriptions.Item>
              {entryAssessment.symptoms && entryAssessment.symptoms.length > 0 && (
                <Descriptions.Item label="识别症状">
                  <Space wrap>
                    {entryAssessment.symptoms.map((symptom, index) => (
                      <Tag key={index}>{symptom}</Tag>
                    ))}
                  </Space>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="选择路径">
                <Tag color={entryAssessment.pathSelected === 'A' ? 'blue' : entryAssessment.pathSelected === 'B' ? 'red' : 'default'}>
                  {entryAssessment.pathSelected === 'A' ? 'A路径（健康筛查）' :
                   entryAssessment.pathSelected === 'B' ? 'B路径（症状诊断）' : '退出流程'}
                </Tag>
              </Descriptions.Item>
              {entryAssessment.clarificationNeeded && (
                <Descriptions.Item label="方向澄清">
                  <Text type="secondary">已进行方向澄清</Text>
                </Descriptions.Item>
              )}
              {entryAssessment.redFlagsHit && (
                <Descriptions.Item label="危险信号">
                  <Alert
                    message="检测到危险信号"
                    type="error"
                    size="small"
                    description={
                      <Space direction="vertical" size="small" style={{ marginTop: 8 }}>
                        {entryAssessment.redFlagsList.map((flag, index) => (
                          <Text key={index} style={{ fontSize: 12 }}>• {flag}</Text>
                        ))}
                      </Space>
                    }
                  />
                </Descriptions.Item>
              )}
            </Descriptions>
          </div>
        )}
      </Space>
    </Card>
  )
}

export default HealthStateAssessmentCard

