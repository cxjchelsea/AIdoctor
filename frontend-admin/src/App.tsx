import { BrowserRouter, Routes, Route } from 'react-router-dom'
import TraceManagementPage from './pages/TraceManagementPage'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<TraceManagementPage />} />
        <Route path="/trace" element={<TraceManagementPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App

