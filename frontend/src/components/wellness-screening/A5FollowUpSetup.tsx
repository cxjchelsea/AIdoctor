import React from 'react'
import { Card, Space, Button, Descriptions, Result } from 'antd'

interface FollowUpPlan {
  followUpDate: string
  reminderContent: string
}

interface A5FollowUpSetupProps {
  followUpPlan: FollowUpPlan
  onViewRecord: () => void
  onNewScreening: () => void
}

const A5FollowUpSetup: React.FC<A5FollowUpSetupProps> = ({
  followUpPlan,
  onViewRecord,
  onNewScreening,
}) => {
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
            <Button key="view" onClick={onViewRecord}>
              查看记录
            </Button>,
            <Button key="new" type="primary" onClick={onNewScreening}>
              开始新的筛查
            </Button>,
          ]}
        />
      </Space>
    </Card>
  )
}

export default A5FollowUpSetup

