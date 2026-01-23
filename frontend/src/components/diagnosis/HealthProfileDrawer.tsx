import { Drawer, Card, Descriptions, List, Button, Typography, Empty } from 'antd'
import { EditOutlined } from '@ant-design/icons'

const { Text } = Typography

interface HealthProfileDrawerProps {
  visible: boolean
  onClose: () => void
  onEdit?: () => void
}

const HealthProfileDrawer: React.FC<HealthProfileDrawerProps> = ({
  visible,
  onClose,
  onEdit,
}) => {
  // TODO: 从API或store获取健康档案数据
  const profile = {
    age: 35,
    gender: '男',
    height: 175,
    weight: 70,
    medicalHistory: ['无'],
    medicationHistory: ['无'],
    allergies: ['无'],
  }

  return (
    <Drawer
      title="健康档案"
      placement="right"
      width={600}
      open={visible}
      onClose={onClose}
    >
      <Card title="基本信息" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="年龄">{profile.age} 岁</Descriptions.Item>
          <Descriptions.Item label="性别">{profile.gender}</Descriptions.Item>
          <Descriptions.Item label="身高">{profile.height} cm</Descriptions.Item>
          <Descriptions.Item label="体重">{profile.weight} kg</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="既往病史" style={{ marginBottom: 16 }}>
        <List
          size="small"
          dataSource={profile.medicalHistory}
          renderItem={(item) => <List.Item>{item}</List.Item>}
        />
      </Card>

      <Card title="用药史" style={{ marginBottom: 16 }}>
        <List
          size="small"
          dataSource={profile.medicationHistory}
          renderItem={(item) => <List.Item>{item}</List.Item>}
        />
      </Card>

      <Card title="过敏史" style={{ marginBottom: 16 }}>
        <List
          size="small"
          dataSource={profile.allergies}
          renderItem={(item) => <List.Item>{item}</List.Item>}
        />
      </Card>

      <Button
        type="primary"
        block
        icon={<EditOutlined />}
        onClick={onEdit}
      >
        编辑健康档案
      </Button>
    </Drawer>
  )
}

export default HealthProfileDrawer

