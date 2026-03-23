import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // 将前端所有 /api 开头的请求，转发到 Java 后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 如果后端接口没有 /api 前缀，需要打开下面这行重写路径；
        // 我们的 LlmController 已经写了 @RequestMapping("/api/rag")，所以保留原样即可
        // rewrite: (path) => path.replace(/^\/api/, '') 
      }
    }
  }
})