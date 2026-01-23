import { Card, Row, Col, Statistic, Divider, Space, List, Typography, Alert, Button, message } from 'antd'
import {
  MedicineBoxOutlined,
  CheckCircleOutlined,
  CopyOutlined,
} from '@ant-design/icons'
import type { MedicalAdvice as MedicalAdviceType } from '@/types/diagnosis'

const { Text } = Typography

interface MedicalAdviceProps {
  advice: MedicalAdviceType
}

const MedicalAdvice: React.FC<MedicalAdviceProps> = ({ advice }) => {
  const { department, timing, preparation, sbarSummary } = advice

  const getTimingColor = () => {
    if (timing.includes('尽快') || timing.includes('立即')) return '#ff4d4f'
    if (timing.includes('2-3天') || timing.includes('几天')) return '#faad14'
    return '#1890ff'
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success('已复制到剪贴板')
    }).catch(() => {
      message.error('复制失败')
    })
  }

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

        {preparation.questions && preparation.questions.length > 0 && (
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
        )}

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
  )
}

export default MedicalAdvice

