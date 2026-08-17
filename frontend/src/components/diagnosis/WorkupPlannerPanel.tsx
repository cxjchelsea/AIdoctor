import React from 'react'
import { Card, List, Tag, Space, Typography, Select } from 'antd'
import { CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined, InfoCircleOutlined } from '@ant-design/icons'
import type { ExaminationItem } from '@/types/diagnosis'

const { Text } = Typography

interface WorkupPlannerPanelProps {
  priorityExaminations: ExaminationItem[]
  optionalExaminations?: ExaminationItem[]
  onStatusChange?: (examinationName: string, status: 'completed' | 'pending' | 'unavailable') => void
}

interface ExaminationStatus {
  [key: string]: 'completed' | 'pending' | 'unavailable'
}

const WorkupPlannerPanel: React.FC<WorkupPlannerPanelProps> = ({
  priorityExaminations,
  optionalExaminations = [],
  onStatusChange,
}) => {
  const [statuses, setStatuses] = React.useState<ExaminationStatus>({})

  const handleStatusChange = (name: string, status: 'completed' | 'pending' | 'unavailable') => {
    setStatuses((prev) => ({ ...prev, [name]: status }))
    onStatusChange?.(name, status)
  }

  const getStatusIcon = (status?: 'completed' | 'pending' | 'unavailable') => {
    switch (status) {
      case 'completed':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />
      case 'unavailable':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
      default:
        return <ClockCircleOutlined style={{ color: '#faad14' }} />
    }
  }

  const getStatusTag = (status?: 'completed' | 'pending' | 'unavailable') => {
    switch (status) {
      case 'completed':
        return <Tag color="success">已完成</Tag>
      case 'unavailable':
        return <Tag color="error">无法完成</Tag>
      default:
        return <Tag color="warning">未完成</Tag>
    }
  }

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high':
        return 'red'
      case 'medium':
        return 'orange'
      case 'low':
        return 'blue'
      default:
        return 'default'
    }
  }

  const renderExaminationItem = (item: ExaminationItem, isPriority: boolean) => {
    const currentStatus = statuses[item.name] || 'pending'
    
    return (
      <List.Item
        actions={[
          <Select
            key="status"
            value={currentStatus}
            onChange={(value) => handleStatusChange(item.name, value)}
            style={{ width: 120 }}
            size="small"
          >
            <Select.Option value="pending">未完成</Select.Option>
            <Select.Option value="completed">已完成</Select.Option>
            <Select.Option value="unavailable">无法完成</Select.Option>
          </Select>,
        ]}
      >
        <List.Item.Meta
          avatar={getStatusIcon(currentStatus)}
          title={
            <Space>
              <Text strong>{item.name}</Text>
              <Tag color={getPriorityColor(item.priority)}>
                {item.priority === 'high' ? '高优先级' : item.priority === 'medium' ? '中优先级' : '低优先级'}
              </Tag>
              {isPriority && <Tag color="red">优先检查</Tag>}
              {getStatusTag(currentStatus)}
            </Space>
          }
          description={
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <div>
                <Text type="secondary">目的：</Text>
                <Text>{item.purpose}</Text>
              </div>
              <div>
                <Text type="secondary">原因：</Text>
                <Text>{item.reason}</Text>
              </div>
            </Space>
          }
        />
      </List.Item>
    )
  }

  return (
    <Card
      title={
        <Space>
          <InfoCircleOutlined />
          <span>验证计划 / 检查建议</span>
        </Space>
      }
    >
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 优先检查项 */}
        {priorityExaminations.length > 0 && (
          <div>
            <Text strong style={{ fontSize: 14, color: '#ff4d4f' }}>
              优先检查项
            </Text>
            <List
              dataSource={priorityExaminations}
              renderItem={(item) => renderExaminationItem(item, true)}
              style={{ marginTop: 8 }}
            />
          </div>
        )}

        {/* 可选检查项 */}
        {optionalExaminations.length > 0 && (
          <div>
            <Text strong style={{ fontSize: 14 }}>
              可选检查项
            </Text>
            <List
              dataSource={optionalExaminations}
              renderItem={(item) => renderExaminationItem(item, false)}
              style={{ marginTop: 8 }}
            />
          </div>
        )}

        {priorityExaminations.length === 0 && optionalExaminations.length === 0 && (
          <Text type="secondary">暂无检查建议</Text>
        )}
      </Space>
    </Card>
  )
}

export default WorkupPlannerPanel

