import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { Suspense, lazy } from 'react'
import { Spin } from 'antd'
import DiagnosisPage from './pages/DiagnosisPage'
import ClinicalParsingTestPage from './pages/ClinicalParsingTestPage'
import './App.css'

const CDPVisualizationPage = lazy(() => import('./components/cdp-visualization/CDPVisualizationPage'))

// CDP可视化页面包装组件
const CDPVisualizationPageWrapper = () => {
  // 从URL路径中提取cdpId
  const pathname = window.location.pathname
  const cdpId = pathname.replace('/cdp/', '') || ''
  return <CDPVisualizationPage cdpId={cdpId} />
}

// 加载中组件
const Loading = () => (
  <div
    style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
    }}
  >
    <Spin size="large" tip="加载中..." />
  </div>
)

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Loading />}>
        <Routes>
          <Route path="/" element={<DiagnosisPage />} />
          <Route path="/diagnosis" element={<DiagnosisPage />} />
          <Route path="/cdp/:cdpId" element={<CDPVisualizationPageWrapper />} />
          <Route path="/test/clinical-parsing" element={<ClinicalParsingTestPage />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}

export default App

