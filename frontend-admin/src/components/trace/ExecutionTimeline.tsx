import React from 'react'
import { Timeline, Card, Tag, Typography, Space, Tooltip } from 'antd'
import type { ExecutionTrace } from '@/types/trace'

const { Text } = Typography

interface ExecutionTimelineProps {
  traces: ExecutionTrace[]
}

/**
 * 执行时间线组件
 * 展示执行的时间顺序
 */
const ExecutionTimeline: React.FC<ExecutionTimelineProps> = ({ traces }) => {
  // 按时间排序
  const sortedTraces = [...traces].sort(
    (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
  )

  if (sortedTraces.length === 0) {
    return (
      <Card>
        <Text type="secondary">暂无执行时间线信息</Text>
      </Card>
    )
  }

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'SUCCESS':
        return 'green'
      case 'ERROR':
        return 'red'
      case 'IN_PROGRESS':
        return 'blue'
      default:
        return 'default'
    }
  }

  const getEventTypeLabel = (eventType: string) => {
    const typeMap: Record<string, string> = {
      'SERVICE_CALL_START': '服务调用开始',
      'SERVICE_CALL_END': '服务调用结束',
      'SERVICE_CALL_ERROR': '服务调用错误',
      'FEIGN_CALL_START': '服务间调用开始',
      'FEIGN_CALL_END': '服务间调用结束',
      'STEP_START': '步骤开始',
      'STEP_END': '步骤结束',
    }
    return typeMap[eventType] || eventType
  }

  return (
    <Card>
      <Timeline mode="left">
        {sortedTraces.map((trace, index) => (
          <Timeline.Item
            key={trace.id || index}
            color={getStatusColor(trace.status)}
            label={
              <div style={{ minWidth: '150px' }}>
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  {new Date(trace.timestamp).toLocaleTimeString()}
                </Text>
                {trace.duration && (
                  <div style={{ fontSize: '11px', color: '#999', marginTop: '2px' }}>
                    {trace.duration}ms
                  </div>
                )}
              </div>
            }
          >
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <div>
                <Tag color={getStatusColor(trace.status)}>
                  {getEventTypeLabel(trace.eventType)}
                </Tag>
                {trace.service && (
                  <Tag>{trace.service}</Tag>
                )}
                {trace.module && (
                  <Tag color="blue">{trace.module}</Tag>
                )}
              </div>
              
              {trace.step && (
                <Text strong style={{ display: 'block', marginTop: '4px' }}>
                  {trace.step}
                </Text>
              )}
              
              {trace.method && (
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  方法: {trace.method}
                </Text>
              )}
              
              {trace.errorMessage && (
                <Tooltip title={trace.errorMessage}>
                  <Text type="danger" style={{ fontSize: '12px', display: 'block' }}>
                    错误: {trace.errorMessage.substring(0, 50)}
                    {trace.errorMessage.length > 50 ? '...' : ''}
                  </Text>
                </Tooltip>
              )}
              
              {trace.requestUrl && (
                <Text type="secondary" style={{ fontSize: '11px', display: 'block' }}>
                  URL: {trace.requestUrl}
                </Text>
              )}
            </Space>
          </Timeline.Item>
        ))}
      </Timeline>
    </Card>
  )
}

export default ExecutionTimeline

