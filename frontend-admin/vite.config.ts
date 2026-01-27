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
  define: {
    global: 'globalThis',
  },
  server: {
    port: 3001,
    proxy: {
      // 执行追踪服务 - 端口8093
      '/api/v1/trace': {
        target: 'http://localhost:8093',
        changeOrigin: true,
        rewrite: (path) => path,
      },
      // WebSocket代理（用于追踪服务）
      '/api/v1/trace/ws': {
        target: 'ws://localhost:8093',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})

