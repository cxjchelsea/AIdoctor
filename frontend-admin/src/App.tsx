import { BrowserRouter, Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu } from 'antd'
import { DatabaseOutlined, BugOutlined } from '@ant-design/icons'
import TraceManagementPage from './pages/TraceManagementPage'
import KnowledgeSchemaPage from './pages/KnowledgeSchemaPage'
import './App.css'

const { Sider, Content } = Layout

function AppContent() {
  const navigate = useNavigate()
  const location = useLocation()

  const menuItems = [
    {
      key: '/trace',
      icon: <BugOutlined />,
      label: '执行追踪管理',
    },
    {
      key: '/knowledge/schema',
      icon: <DatabaseOutlined />,
      label: '知识图谱结构',
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={200} style={{ background: '#fff' }}>
        <div style={{ padding: '16px', textAlign: 'center', borderBottom: '1px solid #f0f0f0' }}>
          <h3 style={{ margin: 0 }}>管理后台</h3>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ height: '100%', borderRight: 0 }}
        />
      </Sider>
      <Layout>
        <Content style={{ background: '#f0f2f5' }}>
          <Routes>
            <Route path="/" element={<TraceManagementPage />} />
            <Route path="/trace" element={<TraceManagementPage />} />
            <Route path="/knowledge/schema" element={<KnowledgeSchemaPage />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  )
}

export default App

