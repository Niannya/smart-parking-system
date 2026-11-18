import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发服务器 5173，/api 代理到后端 8081（00 规格第 33 行：Vite 将 /api 原样代理到 8081）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
