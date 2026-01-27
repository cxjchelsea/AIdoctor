import React, { useMemo } from 'react'
import { Tree, Card, Tag, Typography, Empty } from 'antd'
import type { DataNode } from 'antd/es/tree'
import type { ExecutionTrace } from '@/types/trace'
import { filterSuccessfulTraces } from '@/utils/traceFilter'

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
    // 使用统一的过滤函数过滤掉失败的服务调用
    const successfulTraces = filterSuccessfulTraces(traces)
    
    // 按服务分组
    const serviceMap = new Map<string, Map<string, ExecutionTrace[]>>()

    successfulTraces.forEach(trace => {
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
    const treeNodes: Array<{ node: DataNode; firstTimestamp: number }> = []

    serviceMap.forEach((moduleMap, serviceName) => {
      let serviceFirstTimestamp = Infinity
      
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
        // 合并 START/END 事件：按 service + module + method 分组，只显示一次
        // 对于同一个方法的 START 和 END 事件，优先保留有 duration 的事件（END 事件）
        const methodMap = new Map<string, ExecutionTrace>()
        moduleTraces.forEach(trace => {
          // 使用 service + module + method 作为 key，确保同一个方法只显示一次
          const key = `${trace.service || 'unknown'}-${moduleName}-${trace.method || 'unknown'}`
          
          if (!methodMap.has(key)) {
            methodMap.set(key, trace)
          } else {
            const existing = methodMap.get(key)!
            // 优先保留有 duration 的事件（END 事件）
            // 如果两个都有 duration，保留 duration 更大的（更完整的 END 事件）
            // 如果两个都没有 duration，保留时间戳更早的（START 事件）
            if (trace.duration) {
              if (!existing.duration || trace.duration > existing.duration) {
                methodMap.set(key, trace)
              }
            } else if (!existing.duration) {
              // 两个都没有 duration，保留时间戳更早的
              if (new Date(trace.timestamp).getTime() < new Date(existing.timestamp).getTime()) {
                methodMap.set(key, trace)
              }
            }
          }
        })

        // 合并后按时间戳排序，确保调用顺序正确
        const mergedTraces = Array.from(methodMap.values())
          .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
        
        // 更新服务首次出现的时间戳
        if (mergedTraces.length > 0) {
          const moduleFirstTime = new Date(mergedTraces[0].timestamp).getTime()
          if (moduleFirstTime < serviceFirstTimestamp) {
            serviceFirstTimestamp = moduleFirstTime
          }
        }
        
        const totalDuration = mergedTraces.reduce((sum, t) => sum + (t.duration || 0), 0)

        const moduleNode: DataNode = {
          title: (
            <div>
              <Text strong>{moduleName}</Text>
              <Text type="secondary" style={{ fontSize: '12px', marginLeft: '8px' }}>
                {mergedTraces.length} 次调用
              </Text>
              {totalDuration > 0 && (
                <Tag color="green" style={{ marginLeft: '8px' }}>
                  总耗时: {totalDuration}ms
                </Tag>
              )}
            </div>
          ),
          key: `module-${serviceName}-${moduleName}`,
          children: mergedTraces.map((trace, index) => ({
            title: (
              <div>
                <Text>{trace.method || '未知方法'}</Text>
                {trace.duration && (
                  <Tag color="default" style={{ marginLeft: '8px' }}>
                    {trace.duration}ms
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

      treeNodes.push({ node: serviceNode, firstTimestamp: serviceFirstTimestamp })
    })

    // 按服务首次出现的时间戳排序
    const sortedTreeNodes = treeNodes
      .sort((a, b) => a.firstTimestamp - b.firstTimestamp)
      .map(item => item.node)

    return sortedTreeNodes
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

