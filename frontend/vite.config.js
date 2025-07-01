import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
   server: {
    host: '0.0.0.0',
    port: 5173, // 或你想要的端口
    open: false // 不自动打开localhost
  }
})