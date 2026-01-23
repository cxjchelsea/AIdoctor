import { Drawer, Form, Input, InputNumber, Select, Button, Space, Typography, message } from 'antd'
import { SaveOutlined } from '@ant-design/icons'
import { useDiagnosisStore } from '@/stores/diagnosisStore'
import type { SymptomInfo } from '@/types/diagnosis'
import { useEffect } from 'react'

const { TextArea } = Input
const { Text } = Typography

interface StructuredIntakePanelProps {
  visible: boolean
  onClose: () => void
}

const StructuredIntakePanel: React.FC<StructuredIntakePanelProps> = ({
  visible,
  onClose,
}) => {
  const [form] = Form.useForm()
  const { collectedInfo, completeness } = useDiagnosisStore()

  // 初始化表单值
  useEffect(() => {
    if (visible) {
      form.setFieldsValue({
        chiefComplaint: collectedInfo.chiefComplaint || '',
        duration: collectedInfo.duration || '',
        severity: collectedInfo.severity,
        frequency: collectedInfo.frequency || '',
        location: collectedInfo.location || '',
        accompanyingSymptoms: collectedInfo.accompanyingSymptoms?.join('、') || '',
        trigger: (collectedInfo as SymptomInfo).features?.trigger || '',
        relief: (collectedInfo as SymptomInfo).features?.relief || '',
      })
    }
  }, [visible, collectedInfo, form])

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      
      // 处理伴随症状（字符串转数组）
      const accompanyingSymptoms = values.accompanyingSymptoms
        ? values.accompanyingSymptoms.split(/[、,，]/).filter((s: string) => s.trim())
        : []

      // 更新store中的collectedInfo
      const updatedInfo: SymptomInfo = {
        chiefComplaint: values.chiefComplaint,
        duration: values.duration,
        severity: values.severity,
        frequency: values.frequency,
        location: values.location,
        accompanyingSymptoms,
        features: {
          trigger: values.trigger,
          relief: values.relief,
        },
      }

      // TODO: 调用API更新信息并重新计算完整度
      // await diagnosisApi.updateSymptomInfo(diagnosisId, updatedInfo)
      console.log('更新信息:', updatedInfo)
      
      message.success('信息已保存')
      onClose()
    } catch (error) {
      console.error('保存失败:', error)
      message.error('保存失败，请检查输入')
    }
  }

  return (
    <Drawer
      title="结构化信息采集"
      placement="right"
      width={600}
      open={visible}
      onClose={onClose}
      extra={
        <Button type="primary" icon={<SaveOutlined />} onClick={handleSave}>
          保存
        </Button>
      }
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          severity: undefined,
        }}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              当前信息完整度: {Math.round(completeness * 100)}%
            </Text>
          </div>

          <Form.Item
            label="主诉"
            name="chiefComplaint"
            rules={[{ required: true, message: '请输入主诉' }]}
          >
            <TextArea
              placeholder="请描述您的主要不适症状"
              rows={3}
              showCount
              maxLength={200}
            />
          </Form.Item>

          <Form.Item
            label="持续时间"
            name="duration"
            rules={[{ required: true, message: '请输入持续时间' }]}
          >
            <Input
              placeholder="例如：3天、1周、2个月"
              maxLength={50}
            />
          </Form.Item>

          <Form.Item
            label="严重程度"
            name="severity"
            rules={[{ required: true, message: '请选择严重程度' }]}
          >
            <InputNumber
              min={1}
              max={10}
              placeholder="1-10分，10分最严重"
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item
            label="频率"
            name="frequency"
          >
            <Select placeholder="请选择频率">
              <Select.Option value="持续">持续</Select.Option>
              <Select.Option value="间歇">间歇</Select.Option>
              <Select.Option value="偶尔">偶尔</Select.Option>
              <Select.Option value="每天">每天</Select.Option>
              <Select.Option value="每周">每周</Select.Option>
              <Select.Option value="每月">每月</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="部位"
            name="location"
          >
            <Input
              placeholder="请描述症状发生的部位"
              maxLength={100}
            />
          </Form.Item>

          <Form.Item
            label="伴随症状"
            name="accompanyingSymptoms"
            tooltip="多个症状请用顿号（、）或逗号（，）分隔"
          >
            <TextArea
              placeholder="例如：发热、恶心、呕吐"
              rows={2}
              showCount
              maxLength={200}
            />
          </Form.Item>

          <Form.Item
            label="诱发因素"
            name="trigger"
          >
            <Input
              placeholder="例如：运动后、进食后、情绪激动时"
              maxLength={100}
            />
          </Form.Item>

          <Form.Item
            label="缓解因素"
            name="relief"
          >
            <Input
              placeholder="例如：休息后、服药后"
              maxLength={100}
            />
          </Form.Item>
        </Space>
      </Form>
    </Drawer>
  )
}

export default StructuredIntakePanel

