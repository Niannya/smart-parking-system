import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        // 保留浏览器的 Host，使后端看到的 Host 与 Origin 一致。
        changeOrigin: false,
      },
    },
  },
})
