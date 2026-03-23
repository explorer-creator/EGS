import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import legacy from '@vitejs/plugin-legacy'
import vue2 from '@vitejs/plugin-vue2'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue2(),
    legacy({
      targets: ['ie >= 11'],
      additionalLegacyPolyfills: ['regenerator-runtime/runtime']
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 队友模块 A：FastAPI（关键词配图），需本机先启动 EGSfb/A/server.py :8000
      '/api/module-a': {
        target: 'http://127.0.0.1:8000',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => {
          if (path.startsWith('/api/module-a/output')) {
            return path.replace('/api/module-a', '')
          }
          return path.replace(/^\/api\/module-a/, '')
        }
      },
      '/api': {
        target: 'http://localhost:8080', // Java 代理
        changeOrigin: true,
        secure: false,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            if (!proxyReq.getHeader('X-Auth-Token')) {
              proxyReq.setHeader('X-Auth-Token', '1')
            }
          })
        }
      }
    }
  }
})
