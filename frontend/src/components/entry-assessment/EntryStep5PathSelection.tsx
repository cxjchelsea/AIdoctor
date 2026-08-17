import React from 'react'
import { Card, Result, Button } from 'antd'

interface PathResult {
  path: 'A' | 'B' | 'exit'
  pathName: string
  nextStep: string
  message?: string
}

interface EntryStep5PathSelectionProps {
  pathResult: PathResult
  onNavigateToPath: (path: 'A' | 'B') => void
  onExit: () => void
}

const EntryStep5PathSelection: React.FC<EntryStep5PathSelectionProps> = ({
  pathResult,
  onNavigateToPath,
  onExit,
}) => {
  return (
    <Card style={{ marginBottom: 16 }}>
      {pathResult.path === 'exit' ? (
        <Result
          status="warning"
          title="建议退出线上流程"
          subTitle={pathResult.message}
          extra={[
            <Button key="exit" onClick={onExit}>
              退出
            </Button>,
          ]}
        />
      ) : (
        <Result
          status="success"
          title={`进入${pathResult.pathName}`}
          subTitle={`下一步：${pathResult.nextStep}`}
          extra={[
            <Button
              key="continue"
              type="primary"
              onClick={() => {
                const selectedPath = pathResult.path
                if (selectedPath === 'A' || selectedPath === 'B') {
                  onNavigateToPath(selectedPath)
                }
              }}
            >
              开始
            </Button>,
          ]}
        />
      )}
    </Card>
  )
}

export default EntryStep5PathSelection

