import React from 'react'
import { Card, List, Tag, Typography } from 'antd'

const { Title, Text } = Typography

interface CDPVersion {
  version: number
  timestamp: string
  changes: string[]
}

interface CDPVersionHistoryProps {
  cdpId: string
}

const CDPVersionHistory: React.FC<CDPVersionHistoryProps> = ({ cdpId }) => {
  // TODO: 从API获取CDP版本历史数据
  const versions: CDPVersion[] = []

  return (
    <Card>
      <Title level={4}>CDP版本历史</Title>
      {versions.length === 0 ? (
        <div>暂无版本历史数据</div>
      ) : (
        <List
          dataSource={versions}
          renderItem={(version) => (
            <List.Item>
              <List.Item.Meta
                title={
                  <Space>
                    <Text strong>版本 {version.version}</Text>
                    <Tag>{new Date(version.timestamp).toLocaleString('zh-CN')}</Tag>
                  </Space>
                }
                description={
                  <ul>
                    {version.changes.map((change, index) => (
                      <li key={index}>{change}</li>
                    ))}
                  </ul>
                }
              />
            </List.Item>
          )}
        />
      )}
    </Card>
  )
}

export default CDPVersionHistory

