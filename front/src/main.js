import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'

Vue.use(ElementUI)
Vue.config.productionTip = false

// 不设 baseURL：请求走相对路径 /api/...，由 Vite 代理到 8080，再由后端转发到 8003
Vue.prototype.$axios = axios

// 若有 xDM-F Token 或 Cookie，随请求带给后端，由后端转发到 8003 以通过鉴权（避免 403）
axios.interceptors.request.use(
  config => {
    if (!config.url || !config.url.startsWith('/api')) return config
    // 始终带 x-auth-token，OPEN_IAM_TOKEN_VALIDATION=FALSE 时接受任意非空值
    const iamToken = localStorage.getItem('xdm_auth_token')
    config.headers['x-auth-token'] = config.headers['X-Auth-Token'] = iamToken || '1'
    // 始终带 x-csrf-token，避免 403（无 Cookie 时用占位值）
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

new Vue({
  render: h => h(App),
}).$mount('#app')