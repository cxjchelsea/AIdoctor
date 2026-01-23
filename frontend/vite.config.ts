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
      // 健康状态判定服务（脑区0）- 端口8081
      '/api/v1/health-state-assessment': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path, // 不重写路径
      },
      // 病例理解服务（脑区A）- 端口8082
      '/api/v1/parsing': {
        target: 'http://localhost:8082',
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


