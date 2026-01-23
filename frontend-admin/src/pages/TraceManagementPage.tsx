import React, { useState, useEffect } from 'react'
import {
  Layout,
  Card,
  Tabs,
  Input,
  Button,
  Space,
  Typography,
  Tag,
  Alert,
  Spin,
  Statistic,
  Row,
  Col,
  message,
} from 'antd'
import {
  SearchOutlined,
  ClearOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons'
import DataFlowGraph from '@/components/trace/DataFlowGraph'
import ServiceCallGraph from '@/components/trace/ServiceCallGraph'
import ExecutionTimeline from '@/components/trace/ExecutionTimeline'
import ModuleCallTree from '@/components/trace/ModuleCallTree'
import { useTraceWebSocket } from '@/hooks/useTraceWebSocket'
import { traceApi } from '@/services/traceApi'
import type { ExecutionTrace, TraceSummary } from '@/types/trace'

const { Header, Content } = Layout
const { Title, Text } = Typography

/**
 * 执行追踪管理页面
 */
const TraceManagementPage: React.FC = () => {
  const [cdpId, setCdpId] = useState<string>('')
  const [inputCdpId, setInputCdpId] = useState<string>('')
  const [traces, setTraces] = useState<ExecutionTrace[]>([])
  const [summary, setSummary] = useState<TraceSummary | null>(null)
  const [loading, setLoading] = useState(false)
  const [realtimeEnabled, setRealtimeEnabled] = useState(true)

  // WebSocket实时追踪
  const { events: realtimeEvents, isConnected, clearEvents } = useTraceWebSocket({
    cdpId,
    enabled: realtimeEnabled && !!cdpId,
  })

  // 加载追踪数据
  const loadTraces = async (id: string) => {
    if (!id) {
      message.warning('请输入CDP ID')
      return
    }

    setLoading(true)
    try {
      const [tracesData, summaryData] = await Promise.all([
        traceApi.getTracesByCdpId(id),
        traceApi.getTraceSummary(id),
      ])
      setTraces(tracesData)
      setSummary(summaryData)
      message.success('追踪数据加载成功')
    } catch (error: any) {
      console.error('加载追踪数据失败:', error)
      message.error('加载追踪数据失败: ' + (error.message || '未知错误'))
    } finally {
      setLoading(false)
    }
  }

  // 合并实时事件和历史数据
  useEffect(() => {
    if (realtimeEvents.length > 0) {
      // 将实时事件转换为ExecutionTrace格式
      const newTraces = realtimeEvents.map((event, index) => ({
        id: Date.now() + index,
        cdpId: event.cdpId,
        traceId: event.traceId,
        eventType: event.type,
        service: event.service,
        module: event.module,
        method: event.method,
        step: event.step,
        status: event.status,
        duration: event.duration,
        timestamp: new Date(event.timestamp).toISOString(),
        inputData: event.input ? JSON.stringify(event.input) : undefined,
        outputData: event.output ? JSON.stringify(event.output) : undefined,
        errorMessage: event.errorMessage,
        requestUrl: event.url,
        createdAt: new Date().toISOString(),
      }))

      setTraces(prev => {
        // 合并并去重
        const merged = [...prev, ...newTraces]
        const unique = merged.filter((trace, index, self) =>
          index === self.findIndex(t => 
            t.traceId === trace.traceId && 
            t.eventType === trace.eventType &&
            t.timestamp === trace.timestamp
          )
        )
        return unique.sort((a, b) => 
          new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        )
      })
    }
  }, [realtimeEvents])

  const handleSearch = () => {
    setCdpId(inputCdpId)
    clearEvents()
    loadTraces(inputCdpId)
  }

  const handleClear = () => {
    setCdpId('')
    setInputCdpId('')
    setTraces([])
    setSummary(null)
    clearEvents()
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', height: '100%' }}>
          <Title level={4} style={{ margin: 0 }}>
            执行追踪管理
          </Title>
          <Space>
            <Input
              placeholder="请输入CDP ID"
              value={inputCdpId}
              onChange={(e) => setInputCdpId(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 300 }}
              prefix={<SearchOutlined />}
            />
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={handleSearch}
              loading={loading}
            >
              查询
            </Button>
            <Button
              icon={<ClearOutlined />}
              onClick={handleClear}
            >
              清空
            </Button>
            {cdpId && (
              <Button
                icon={realtimeEnabled ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                onClick={() => setRealtimeEnabled(!realtimeEnabled)}
                type={realtimeEnabled ? 'default' : 'primary'}
              >
                {realtimeEnabled ? '暂停实时' : '开启实时'}
              </Button>
            )}
          </Space>
        </div>
      </Header>

      <Content style={{ padding: '24px', background: '#f0f2f5' }}>
        {/* 连接状态提示 */}
        {cdpId && (
          <Alert
            message={
              <Space>
                <Text>WebSocket连接状态:</Text>
                <Tag color={isConnected ? 'success' : 'error'}>
                  {isConnected ? '已连接' : '未连接'}
                </Tag>
                {summary && (
                  <>
                    <Text>|</Text>
                    <Text>总追踪数: {traces.length}</Text>
                    <Text>|</Text>
                    <Text>总耗时: {summary.totalDuration}ms</Text>
                    {summary.errorCount > 0 && (
                      <>
                        <Text>|</Text>
                        <Text type="danger">错误数: {summary.errorCount}</Text>
                      </>
                    )}
                  </>
                )}
              </Space>
            }
            type={isConnected ? 'success' : 'warning'}
            style={{ marginBottom: 16 }}
            showIcon
          />
        )}

        {!cdpId ? (
          <Card>
            <div style={{ textAlign: 'center', padding: '60px 0' }}>
              <Text type="secondary" style={{ fontSize: '16px' }}>
                请输入CDP ID开始查询执行追踪
              </Text>
            </div>
          </Card>
        ) : (
          <Spin spinning={loading && traces.length === 0}>
            <Tabs 
              defaultActiveKey="data-flow"
              items={[
                {
                  key: 'data-flow',
                  label: '数据流转图',
                  children: <DataFlowGraph traces={traces} />,
                },
                {
                  key: 'service-call',
                  label: '服务调用图',
                  children: <ServiceCallGraph traces={traces} />,
                },
                {
                  key: 'timeline',
                  label: '执行时间线',
                  children: <ExecutionTimeline traces={traces} />,
                },
                {
                  key: 'module-tree',
                  label: '模块调用树',
                  children: <ModuleCallTree traces={traces} />,
                },
                ...(summary ? [{
                  key: 'statistics',
                  label: '统计信息',
                  children: (
                  <Card>
                    <Row gutter={16}>
                      <Col span={6}>
                        <Statistic
                          title="总执行时间"
                          value={summary.totalDuration}
                          suffix="ms"
                          valueStyle={{ color: '#1890ff' }}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="服务调用次数"
                          value={Object.values(summary.serviceCalls).reduce((sum, count) => sum + count, 0)}
                          suffix="次"
                          valueStyle={{ color: '#52c41a' }}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="涉及服务数"
                          value={Object.keys(summary.serviceCalls).length}
                          suffix="个"
                          valueStyle={{ color: '#722ed1' }}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="错误数量"
                          value={summary.errorCount}
                          suffix="个"
                          valueStyle={{ color: summary.errorCount > 0 ? '#ff4d4f' : '#52c41a' }}
                        />
                      </Col>
                    </Row>

                    <div style={{ marginTop: 24 }}>
                      <Title level={5}>执行步骤</Title>
                      {summary.steps.length > 0 ? (
                        <div>
                          {summary.steps.map((step, index) => (
                            <Card key={index} size="small" style={{ marginBottom: 8 }}>
                              <Space>
                                <Text strong>{step.step}</Text>
                                <Text type="secondary">
                                  {new Date(step.startTime).toLocaleString()}
                                </Text>
                                <Text>涉及服务:</Text>
                                {step.services.map((service, i) => (
                                  <Tag key={i}>{service}</Tag>
                                ))}
                              </Space>
                            </Card>
                          ))}
                        </div>
                      ) : (
                        <Text type="secondary">暂无步骤信息</Text>
                      )}
                    </div>

                    <div style={{ marginTop: 24 }}>
                      <Title level={5}>服务调用统计</Title>
                      <Row gutter={16}>
                        {Object.entries(summary.serviceCalls).map(([service, count]) => (
                          <Col span={6} key={service}>
                            <Card size="small">
                              <Statistic
                                title={service}
                                value={count}
                                suffix="次"
                              />
                            </Card>
                          </Col>
                        ))}
                      </Row>
                    </div>
                  </Card>
                  )
                }] : [])
              ]}
            />
          </Spin>
        )}
      </Content>
    </Layout>
  )
}

export default TraceManagementPage

