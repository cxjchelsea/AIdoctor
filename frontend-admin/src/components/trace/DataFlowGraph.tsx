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
import { Card, Tag, Typography } from 'antd'
import type { ExecutionTrace } from '@/types/trace'
import { filterSuccessfulTraces } from '@/utils/traceFilter'

const { Text } = Typography

interface DataFlowGraphProps {
  traces: ExecutionTrace[]
}

/**
 * 数据流转图组件
 * 展示数据在服务间的流转
 */
const DataFlowGraph: React.FC<DataFlowGraphProps> = ({ traces }) => {
  const { nodes, edges } = useMemo(() => {
    // 先过滤掉失败的服务调用
    const successfulTraces = filterSuccessfulTraces(traces)
    
    const nodeMap = new Map<string, Node>()
    const edgeList: Edge[] = []
    let yPosition = 0
    const xSpacing = 300
    const ySpacing = 150

    // 按时间排序
    const sortedTraces = [...successfulTraces].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    )

    // 按服务分组
    const serviceGroups = new Map<string, ExecutionTrace[]>()
    sortedTraces.forEach(trace => {
      if (trace.service) {
        if (!serviceGroups.has(trace.service)) {
          serviceGroups.set(trace.service, [])
        }
        serviceGroups.get(trace.service)!.push(trace)
      }
    })

    let xPosition = 0
    let previousNodeId: string | null = null

    serviceGroups.forEach((serviceTraces, serviceName) => {
      const nodeId = `node-${serviceName}`
      
      // 计算服务执行时间
      const startTrace = serviceTraces[0]
      const endTrace = serviceTraces[serviceTraces.length - 1]
      const duration = endTrace.duration || 0
      // 由于已经过滤掉失败的服务，所以这里应该都是成功的
      const status = 'success'

      const node: Node = {
        id: nodeId,
        type: 'default',
        position: { x: xPosition, y: yPosition },
        data: {
          label: (
            <div style={{ padding: '8px' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                {serviceName}
              </div>
              {startTrace.module && (
                <div style={{ fontSize: '12px', color: '#666', marginBottom: '4px' }}>
                  {startTrace.module}
                </div>
              )}
              <div style={{ fontSize: '10px', color: '#999' }}>
                {new Date(startTrace.timestamp).toLocaleTimeString()}
              </div>
              {duration > 0 && (
                <Tag color={status === 'error' ? 'red' : 'green'} style={{ marginTop: '4px' }}>
                  {duration}ms
                </Tag>
              )}
            </div>
          ),
        },
        style: {
          background: status === 'error' ? '#fff1f0' : '#f6ffed',
          border: `2px solid ${status === 'error' ? '#ff4d4f' : '#52c41a'}`,
          borderRadius: '8px',
          minWidth: 150,
        },
      }

      nodeMap.set(nodeId, node)

      // 创建连接边
      if (previousNodeId) {
        edgeList.push({
          id: `edge-${previousNodeId}-${nodeId}`,
          source: previousNodeId,
          target: nodeId,
          animated: true,
          style: { stroke: '#1890ff', strokeWidth: 2 },
          markerEnd: {
            type: MarkerType.ArrowClosed,
            color: '#1890ff',
          },
        })
      }

      previousNodeId = nodeId
      xPosition += xSpacing
    })

    return {
      nodes: Array.from(nodeMap.values()),
      edges: edgeList,
    }
  }, [traces])

  if (nodes.length === 0) {
    return (
      <Card>
        <Text type="secondary">暂无数据流转信息</Text>
      </Card>
    )
  }

  return (
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
  )
}

export default DataFlowGraph

