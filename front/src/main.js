import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './assets/showcase-overrides.css'
import axios from 'axios'
import { sanitizeApiError } from './utils/apiError'
import { installShowcaseMessage } from './utils/showcaseMessage'
import { installStaticDemoMock } from './staticDemoApi'

if (import.meta.env.VITE_STATIC_DEMO === 'true') {
  const b = import.meta.env.BASE_URL || '/'
  if (b !== '/') {
    axios.defaults.baseURL = b.replace(/\/$/, '')
  }
  installStaticDemoMock()
}

Vue.use(ElementUI)
installShowcaseMessage()
Vue.config.productionTip = false

// 默认不设 baseURL：/api 由 Vite 代理到 8080。GitHub Pages 静态演示（VITE_STATIC_DEMO）下会设置 BASE_URL 以匹配 /EGS/ 子路径。
Vue.prototype.$axios = axios

// 若有 xDM-F Token 或 Cookie，随请求带给后端，由后端转发到 8003
axios.interceptors.request.use(
  config => {
    if (!config.url || !config.url.startsWith('/api')) return config
    // 始终带 x-auth-token，OPEN_IAM_TOKEN_VALIDATION=FALSE 时接受任意非空值
    const iamToken = localStorage.getItem('xdm_auth_token')
    config.headers['x-auth-token'] = config.headers['X-Auth-Token'] = iamToken || '1'
    // 始终带 x-csrf-token（无 Cookie 时用占位值）
    let csrfVal = '1'
    let token = (localStorage.getItem('xdm_token') || '').trim()
    if (token) {
      token = token.replace(/\s*\n\s*/g, '; ')
      if (token.indexOf('SESSION=') !== -1 || (token.indexOf('=') !== -1 && token.indexOf(';') !== -1)) {
        config.headers['X-XDM-Cookie'] = token
        const xsrfMatch = token.match(/XSRF-TOKEN=([^;]+)/i)
        const csrfMatch = token.match(/csrf_token=([^;]+)/i)
        const tokenMatch = token.match(/TOKEN=([^;]+)/i)
        csrfVal = (xsrfMatch && xsrfMatch[1] && xsrfMatch[1].trim()) || (csrfMatch && csrfMatch[1] && csrfMatch[1].trim()) || (tokenMatch && tokenMatch[1] && tokenMatch[1].trim()) || csrfVal
      }
    }
    config.headers['x-csrf-token'] = config.headers['X-CSRF-TOKEN'] = config.headers['X-XSRF-TOKEN'] = csrfVal
    // 8003 控制台请求头（可选，代理有默认值）
    const appId = localStorage.getItem('xdm_application_id')
    if (appId) config.headers['X-DME-ApplicationId'] = appId
    const tenantId = localStorage.getItem('xdm_tenant_id')
    if (tenantId) config.headers['X-DME-TenantId'] = tenantId
    const userId = localStorage.getItem('xdm_user_id')
    if (userId) config.headers['X-DME-UserId'] = userId
    const modifier = localStorage.getItem('xdm_modifier')
    if (modifier) config.headers['X-DME-Modifier'] = modifier
    if (token && token.indexOf('SESSION=') === -1 && !(token.indexOf('=') !== -1 && token.indexOf(';') !== -1)) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  err => Promise.reject(err)
)

// 无 HTTP 响应时（代理未启动、断网等）提示；有响应时由各页面读取 success/message，避免与 catch 重复弹窗
axios.interceptors.response.use(
  response => response,
  err => {
    if (err.config && err.config.url && err.config.url.startsWith('/api')) {
      sanitizeApiError(err)
    }
    /* 展示模式：不弹红条，页面内自行使用本地参考数据 */
    if (!err.response && err.config && err.config.url && err.config.url.startsWith('/api')) {
      try {
        console.debug('[展示模式] 请求未达服务器，已忽略弹窗', err.config.url, err.message)
      } catch (e) {
        /* ignore */
      }
    }
    return Promise.reject(err)
  }
)

new Vue({
  render: h => h(App),
}).$mount('#app')