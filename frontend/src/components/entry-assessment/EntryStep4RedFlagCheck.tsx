import React from 'react'
import { Card, Alert, Button, Result } from 'antd'

interface RedFlag {
  description: string
}

interface RedFlagsCheck {
  redFlagsHit: boolean
  redFlags: RedFlag[]
  safetyMessage?: string
}

interface EntryStep4RedFlagCheckProps {
  redFlagsCheck: RedFlagsCheck
  onExit: () => void
}

const EntryStep4RedFlagCheck: React.FC<EntryStep4RedFlagCheckProps> = ({
  redFlagsCheck,
  onExit,
}) => {
  return (
    <Card style={{ marginBottom: 16 }}>
      {redFlagsCheck.redFlagsHit ? (
        <Alert
          message="发现危险信号"
          description={
            <div>
              <div>检测到以下危险信号：</div>
              <ul>
                {redFlagsCheck.redFlags.map((flag, index) => (
                  <li key={index}>{flag.description}</li>
                ))}
              </ul>
              {redFlagsCheck.safetyMessage && (
                <div style={{ marginTop: 16 }}>
                  <strong>{redFlagsCheck.safetyMessage}</strong>
                </div>
              )}
            </div>
          }
          type="error"
          showIcon
          action={
            <Button size="small" danger onClick={onExit}>
              退出线上流程
            </Button>
          }
        />
      ) : (
        <Result
          status="success"
          title="安全检查通过"
          subTitle="未发现危险信号，可以继续"
        />
      )}
    </Card>
  )
}

export default EntryStep4RedFlagCheck

