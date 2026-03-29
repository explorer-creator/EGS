import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import legacy from '@vitejs/plugin-legacy'
import vue2 from '@vitejs/plugin-vue2'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const base = env.VITE_BASE || '/'

  return {
    base,
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
          target: 'http://localhost:8080',
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
  }
})
