import React, { useMemo } from 'react'
import { Tree, Card, Tag, Typography, Empty } from 'antd'
import type { DataNode } from 'antd/es/tree'
import type { ExecutionTrace } from '@/types/trace'

const { Text } = Typography

interface ModuleCallTreeProps {
  traces: ExecutionTrace[]
}

/**
 * 模块调用树组件
 * 展示服务内部模块的调用层次
 */
const ModuleCallTree: React.FC<ModuleCallTreeProps> = ({ traces }) => {
  const treeData = useMemo(() => {
    // 按服务分组
    const serviceMap = new Map<string, Map<string, ExecutionTrace[]>>()

    traces.forEach(trace => {
      if (trace.service) {
        if (!serviceMap.has(trace.service)) {
          serviceMap.set(trace.service, new Map())
        }
        const moduleMap = serviceMap.get(trace.service)!
        
        const moduleName = trace.module || 'default'
        if (!moduleMap.has(moduleName)) {
          moduleMap.set(moduleName, [])
        }
        moduleMap.get(moduleName)!.push(trace)
      }
    })

    // 构建树结构
    const treeNodes: DataNode[] = []

    serviceMap.forEach((moduleMap, serviceName) => {
      const serviceNode: DataNode = {
        title: (
          <div>
            <Tag color="blue">{serviceName}</Tag>
            <Text type="secondary" style={{ fontSize: '12px', marginLeft: '8px' }}>
              {moduleMap.size} 个模块
            </Text>
          </div>
        ),
        key: `service-${serviceName}`,
        children: [],
      }

      moduleMap.forEach((moduleTraces, moduleName) => {
        const totalDuration = moduleTraces.reduce((sum, t) => sum + (t.duration || 0), 0)
        const errorCount = moduleTraces.filter(t => t.status === 'ERROR').length

        const moduleNode: DataNode = {
          title: (
            <div>
              <Text strong>{moduleName}</Text>
              <Text type="secondary" style={{ fontSize: '12px', marginLeft: '8px' }}>
                {moduleTraces.length} 次调用
              </Text>
              {totalDuration > 0 && (
                <Tag color="green" style={{ marginLeft: '8px' }}>
                  总耗时: {totalDuration}ms
                </Tag>
              )}
              {errorCount > 0 && (
                <Tag color="red" style={{ marginLeft: '8px' }}>
                  错误: {errorCount}
                </Tag>
              )}
            </div>
          ),
          key: `module-${serviceName}-${moduleName}`,
          children: moduleTraces.map((trace, index) => ({
            title: (
              <div>
                <Text>{trace.method || '未知方法'}</Text>
                {trace.duration && (
                  <Tag color="default" style={{ marginLeft: '8px' }}>
                    {trace.duration}ms
                  </Tag>
                )}
                {trace.status === 'ERROR' && (
                  <Tag color="red" style={{ marginLeft: '8px' }}>
                    错误
                  </Tag>
                )}
                <Text type="secondary" style={{ fontSize: '11px', marginLeft: '8px' }}>
                  {new Date(trace.timestamp).toLocaleTimeString()}
                </Text>
              </div>
            ),
            key: `trace-${trace.id || index}`,
            isLeaf: true,
          })),
        }

        serviceNode.children!.push(moduleNode)
      })

      treeNodes.push(serviceNode)
    })

    return treeNodes
  }, [traces])

  if (treeData.length === 0) {
    return (
      <Card>
        <Empty description="暂无模块调用信息" />
      </Card>
    )
  }

  return (
    <Card>
      <Tree
        treeData={treeData}
        defaultExpandAll
        showLine={{ showLeafIcon: false }}
      />
    </Card>
  )
}

export default ModuleCallTree

