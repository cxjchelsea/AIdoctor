import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/internal/nonproduction/v1/clinical': {
        target: `http://127.0.0.1:${process.env.V1_CLINICAL_HTTP_PORT || '18084'}`,
        changeOrigin: true,
      },
      // 健康状态判定服务（tool_0）- 端口8081
      '/api/v1/health-state-assessment': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path, // 不重写路径
      },
      // 病例理解服务（tool_1）- 端口8082
      '/api/v1/parsing': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path, // 不重写路径
      },
      // 对话服务（tool_2）- 端口8088
      '/api/v1/dialog': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        rewrite: (path) => path, // 不重写路径
      },
      // 诊断服务等其他服务 - 端口8084
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
    },
  },
})


