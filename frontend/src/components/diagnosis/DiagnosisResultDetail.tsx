import React from 'react'
import { Card, List, Tag, Space, Typography, Alert } from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  CalendarOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import type { DiagnosisResult } from '@/types/diagnosis'
import DiseasePossibilityCard from './DiseasePossibilityCard'
import ExaminationSuggestion from './ExaminationSuggestion'
import MedicalAdvice from './MedicalAdvice'

const { Text } = Typography

interface DiagnosisResultDetailProps {
  result: DiagnosisResult
}

const DiagnosisResultDetail: React.FC<DiagnosisResultDetailProps> = ({ result }) => {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

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
                <Text strong style={{ color: '#1890ff' }}>
                  【首要假设】
                </Text>
                <Tag color="blue">最可能</Tag>
              </Space>
            }
          >
            <DiseasePossibilityCard possibility={result.conclusion.primaryHypothesis} showDetail={true} />
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
                  <Text strong style={{ color: '#ff4d4f' }}>
                    【必须排除的高危诊断】
                  </Text>
                  <Tag color="red">高危</Tag>
                </Space>
              }
              description={
                <Space direction="vertical" size="small" style={{ width: '100%' }}>
                  <Text strong style={{ color: '#ff4d4f' }}>
                    {result.conclusion.mustExcludeDiagnosis.disease}
                  </Text>
                  <Text>
                    可能性：
                    {Math.round(result.conclusion.mustExcludeDiagnosis.confidence * 100)}%
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
                  result.exclusionStatus.status === 'excluded'
                    ? 'green'
                    : result.exclusionStatus.status === 'not_excluded'
                      ? 'orange'
                      : 'red'
                }
              >
                {result.exclusionStatus.status === 'excluded'
                  ? '已排除'
                  : result.exclusionStatus.status === 'not_excluded'
                    ? '未排除'
                    : '需线下排除'}
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
            <Text strong style={{ color: '#52c41a' }}>
              阳性证据（支持最可能方向）：
            </Text>
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
              <Text strong style={{ color: '#ff4d4f' }}>
                关键阴性证据（排除其他方向）：
              </Text>
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
      <ExaminationSuggestion
        priorityExaminations={result.examinationSuggestions.priorityExaminations}
        optionalExaminations={result.examinationSuggestions.optionalExaminations}
        explanation={result.examinationSuggestions.explanation}
      />
      <MedicalAdvice advice={result.medicalAdvice} />
    </Space>
  )
}

export default DiagnosisResultDetail

