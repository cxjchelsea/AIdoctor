import React, { useMemo } from 'react'
import ReactFlow, {
  Node,
  Edge,
  Background,
  Controls,
  MiniMap,
  MarkerType,
} from 'reactflow'
import 'reactflow/dist/base.css'
import { Card, Typography, Statistic, Row, Col } from 'antd'
import type { ExecutionTrace } from '@/types/trace'
import { filterSuccessfulTraces } from '@/utils/traceFilter'

const { Text } = Typography

interface ServiceCallGraphProps {
  traces: ExecutionTrace[]
}

/**
 * 服务调用关系图组件
 * 展示服务间的调用关系和依赖
 */
const ServiceCallGraph: React.FC<ServiceCallGraphProps> = ({ traces }) => {
  const { nodes, edges, statistics } = useMemo(() => {
    // 先过滤掉失败的服务调用
    const successfulTraces = filterSuccessfulTraces(traces)
    
    const nodeMap = new Map<string, Node>()
    const edgeMap = new Map<string, Edge>()
    const serviceStats = new Map<string, { count: number; totalDuration: number; errorCount: number }>()

    // 统计服务调用
    successfulTraces.forEach(trace => {
      if (trace.service) {
        if (!serviceStats.has(trace.service)) {
          serviceStats.set(trace.service, { count: 0, totalDuration: 0, errorCount: 0 })
        }
        const stats = serviceStats.get(trace.service)!
        stats.count++
        if (trace.duration) {
          stats.totalDuration += trace.duration
        }
        // 由于已经过滤掉失败的服务，这里不需要检查 ERROR 状态
      }
    })

    // 创建服务节点
    let xPosition = 0
    const nodeSpacing = 250
    const maxNodesPerRow = 4

    serviceStats.forEach((stats, serviceName) => {
      const nodeId = `service-${serviceName}`
      const avgDuration = stats.count > 0 ? Math.round(stats.totalDuration / stats.count) : 0
      // 由于已经过滤掉失败的服务，errorRate 应该始终为 0
      const errorRate = 0

      const node: Node = {
        id: nodeId,
        type: 'default',
        position: { 
          x: (xPosition % maxNodesPerRow) * nodeSpacing,
          y: Math.floor(xPosition / maxNodesPerRow) * nodeSpacing
        },
        data: {
          label: (
            <div style={{ padding: '12px', textAlign: 'center' }}>
              <div style={{ fontWeight: 'bold', fontSize: '14px', marginBottom: '8px' }}>
                {serviceName}
              </div>
              <div style={{ fontSize: '12px', color: '#666' }}>
                调用次数: {stats.count}
              </div>
              {avgDuration > 0 && (
                <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                  平均耗时: {avgDuration}ms
                </div>
              )}
            </div>
          ),
        },
        style: {
          background: errorRate > 0 ? '#fff1f0' : '#f0f5ff',
          border: `2px solid ${errorRate > 0 ? '#ff4d4f' : '#1890ff'}`,
          borderRadius: '8px',
          minWidth: 180,
        },
      }

      nodeMap.set(nodeId, node)
      xPosition++
    })

    // 创建调用关系边（基于时间顺序）
    const sortedTraces = [...successfulTraces].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    )

    let previousService: string | null = null
    sortedTraces.forEach(trace => {
      if (trace.service && previousService && trace.service !== previousService) {
        const edgeId = `edge-${previousService}-${trace.service}`
        if (!edgeMap.has(edgeId)) {
          edgeMap.set(edgeId, {
            id: edgeId,
            source: `service-${previousService}`,
            target: `service-${trace.service}`,
            animated: true,
            style: { stroke: '#1890ff', strokeWidth: 2 },
            markerEnd: {
              type: MarkerType.ArrowClosed,
              color: '#1890ff',
            },
          })
        }
        previousService = trace.service
      } else if (trace.service) {
        previousService = trace.service
      }
    })

    return {
      nodes: Array.from(nodeMap.values()),
      edges: Array.from(edgeMap.values()),
      statistics: Array.from(serviceStats.entries()).map(([service, stats]) => ({
        service,
        ...stats,
        avgDuration: stats.count > 0 ? Math.round(stats.totalDuration / stats.count) : 0,
      })),
    }
  }, [traces])

  if (nodes.length === 0) {
    return (
      <Card>
        <Text type="secondary">暂无服务调用信息</Text>
      </Card>
    )
  }

  return (
    <div>
      {/* 统计信息 */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          {statistics.map(stat => (
            <Col span={6} key={stat.service}>
              <Statistic
                title={stat.service}
                value={stat.count}
                suffix="次"
                valueStyle={{ fontSize: '20px' }}
              />
              <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                平均耗时: {stat.avgDuration}ms
              </div>
            </Col>
          ))}
        </Row>
      </Card>

      {/* 调用关系图 */}
      <Card>
        <div style={{ width: '100%', height: '600px', border: '1px solid #d9d9d9', borderRadius: '4px' }}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            fitView
          >
            <Background />
            <Controls />
            <MiniMap />
          </ReactFlow>
        </div>
      </Card>
    </div>
  )
}

export default ServiceCallGraph

