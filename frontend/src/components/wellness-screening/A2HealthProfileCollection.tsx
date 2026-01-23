import React from 'react'
import { Card, Space, Button, Progress, Alert, Descriptions, Input, Typography } from 'antd'

const { Text } = Typography

interface HealthProfile {
  completeness: number
  basicInfo?: {
    age?: number
    gender?: string
    bmi?: number
  }
}

interface A2HealthProfileCollectionProps {
  healthProfile: HealthProfile
  isComplete: boolean
  missingRequiredFields: string[]
  followUpQuestions: string[]
  onAnswer: (question: string, answer: string) => void
  onNext: () => void
}

const A2HealthProfileCollection: React.FC<A2HealthProfileCollectionProps> = ({
  healthProfile,
  isComplete,
  missingRequiredFields,
  followUpQuestions,
  onAnswer,
  onNext,
}) => {
  return (
    <Card title="A2｜收集健康画像" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 信息完整度 */}
        <div>
          <Progress
            percent={healthProfile.completeness * 100}
            status={isComplete ? 'success' : 'active'}
          />
          <Text>{healthProfile.completeness * 100}% 完整</Text>
        </div>

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
                  onPressEnter={(e) => onAnswer(question, e.currentTarget.value)}
                />
              </div>
            ))}
          </Card>
        )}

        {/* 已收集的信息展示 */}
        {isComplete && healthProfile.basicInfo && (
          <Card size="small" title="健康画像摘要">
            <Descriptions column={2}>
              {healthProfile.basicInfo.age && (
                <Descriptions.Item label="年龄">
                  {healthProfile.basicInfo.age}
                </Descriptions.Item>
              )}
              {healthProfile.basicInfo.gender && (
                <Descriptions.Item label="性别">
                  {healthProfile.basicInfo.gender}
                </Descriptions.Item>
              )}
              {healthProfile.basicInfo.bmi && (
                <Descriptions.Item label="BMI">
                  {healthProfile.basicInfo.bmi}
                </Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}

        {/* 下一步按钮 */}
        {isComplete && (
          <Button type="primary" onClick={onNext} block>
            继续下一步：执行分支
          </Button>
        )}
      </Space>
    </Card>
  )
}

export default A2HealthProfileCollection

