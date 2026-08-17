import React from 'react'
import { Card, Input, Space, Button, Typography } from 'antd'

const { TextArea } = Input
const { Text } = Typography

interface EntryStep1ReceiveInputProps {
  userInput: string
  onInputChange: (value: string) => void
  onSubmit: () => void
}

const EntryStep1ReceiveInput: React.FC<EntryStep1ReceiveInputProps> = ({
  userInput,
  onInputChange,
  onSubmit,
}) => {
  const quickInputs = [
    '我最近不舒服',
    '我想做个体检',
    '帮我看看这个报告',
  ]

  return (
    <Card title="请描述您的问题" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 输入框 */}
        <TextArea
          rows={4}
          placeholder="例如：我最近胸痛，想咨询一下"
          value={userInput}
          onChange={(e) => onInputChange(e.target.value)}
        />

        {/* 快捷输入提示 */}
        <div>
          <Text type="secondary">您可以说：</Text>
          <Space style={{ marginTop: 8 }} wrap>
            {quickInputs.map((input) => (
              <Button
                key={input}
                onClick={() => onInputChange(input)}
                style={{
                  cursor: 'pointer',
                  backgroundColor: '#f5f5f5',
                  borderColor: '#d9d9d9',
                  color: '#000',
                }}
              >
                {input}
              </Button>
            ))}
          </Space>
        </div>

        {/* 提交按钮 */}
        <Button
          onClick={onSubmit}
          block
          disabled={!userInput.trim()}
          style={{
            backgroundColor: !userInput.trim() ? '#f5f5f5' : '#1890ff',
            borderColor: !userInput.trim() ? '#d9d9d9' : '#1890ff',
            color: !userInput.trim() ? 'rgba(0,0,0,0.25)' : '#fff',
            height: '40px',
          }}
        >
          提交
        </Button>
      </Space>
    </Card>
  )
}

export default EntryStep1ReceiveInput

