<template>
  <div class="login-register-page">
    <BackgroundDecor />
    <div v-if="isNarrowLogin" class="login-mascot-strip" aria-hidden="true">
      <div class="login-mascot-strip__pulse login-mascot-strip__pulse--delay">
        <img class="login-mascot-strip__img" :src="loginMascotLeft" alt="" draggable="false" />
      </div>
      <div class="login-mascot-strip__pulse">
        <img class="login-mascot-strip__img" :src="loginMascotRight" alt="" draggable="false" />
      </div>
    </div>
    <div class="login-register-row">
      <aside v-if="!isNarrowLogin" class="login-gutter login-gutter--left" aria-hidden="true">
        <div class="login-gutter__pulse login-gutter__pulse--delay">
          <img class="login-gutter__img" :src="loginMascotLeft" alt="" draggable="false" />
        </div>
      </aside>
      <div class="form-container">
      <h1 class="title">丝路智联</h1>
      <p class="title-sub">国际物流 · 智慧连接 · 服务「跨境电商 + 海外仓」</p>
      <BackgroundMusic variant="entry" />
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-width="80px" class="auth-form">
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password @keyup.enter.native="doLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width: 100%;" :loading="loginLoading" @click="doLogin">登录</el-button>
            </el-form-item>
            <el-form-item>
              <el-button plain style="width: 100%;" @click="$emit('guest-enter')">逛逛（跳过登录）</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" label-width="100px" class="auth-form" :rules="registerRules" ref="registerFormRef">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
            </el-form-item>
            <el-form-item label="性别">
              <el-select v-model="registerForm.gender" placeholder="请选择" style="width: 100%;">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
            <el-form-item label="身份职业">
              <el-input v-model="registerForm.occupation" placeholder="如：工程师、管理员" />
            </el-form-item>
            <el-form-item label="所在部门">
              <el-input v-model="registerForm.department" placeholder="如：生产部、技术部" />
            </el-form-item>
            <el-form-item label="负责业务">
              <el-input v-model="registerForm.responsibleBusiness" type="textarea" :rows="2" placeholder="负责的业务范围" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width: 100%;" :loading="registerLoading" @click="doRegister">注册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <el-collapse>
        <el-collapse-item title="xDM-F 配置（注册需先配置，可选）" name="xdm">
          <el-form inline size="small">
            <el-form-item label="Cookie">
              <el-input v-model="xdmToken" type="textarea" :rows="1" placeholder="访问 8003 登录后 F12 复制 Cookie" style="width: 360px;" />
            </el-form-item>
            <el-form-item>
              <el-button size="small" @click="saveXdmToken">保存</el-button>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
      </div>
      <aside v-if="!isNarrowLogin" class="login-gutter login-gutter--right" aria-hidden="true">
        <div class="login-gutter__pulse">
          <img class="login-gutter__img" :src="loginMascotRight" alt="" draggable="false" />
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import BackgroundDecor from '../components/BackgroundDecor.vue'
import BackgroundMusic from '../components/BackgroundMusic.vue'
import { config } from '../config'
const TENANT_ID = config.tenantId

function buildCreateParams(data) {
  return {
    params: {
      id: '',
      modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
      lastUpdateTime: '',
      creator: 'admin',
      rdmExtensionType: 'User_Infor',
      tenant: { id: TENANT_ID, clazz: 'Tenant' },
      ...data
    }
  }
}

export default {
  name: 'LoginRegisterPage',
  components: { BackgroundDecor, BackgroundMusic },
  data() {
    return {
      loginViewportWidth: typeof window !== 'undefined' ? window.innerWidth : 1200,
      xdmToken: localStorage.getItem('xdm_token') || '',
      activeTab: 'login',
      loginLoading: false,
      registerLoading: false,
      loginForm: { username: '', password: '' },
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        gender: '',
        occupation: '',
        department: '',
        responsibleBusiness: ''
      },
      registerRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
        confirmPassword: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: (rule, value, cb) => { if (value !== this.registerForm.password) cb(new Error('两次密码不一致')); else cb(); }, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    /** 固定：左长征火箭、右卫星组网（public/login-mascots/） */
    loginMascotLeft() {
      const b = (import.meta.env.BASE_URL || '/').replace(/\/?$/, '/')
      return b + 'login-mascots/left.png'
    },
    loginMascotRight() {
      const b = (import.meta.env.BASE_URL || '/').replace(/\/?$/, '/')
      return b + 'login-mascots/right.png'
    },
    isNarrowLogin() {
      return this.loginViewportWidth <= 1024
    }
  },
  mounted() {
    this._onLoginResize = () => {
      this.loginViewportWidth = window.innerWidth
    }
    window.addEventListener('resize', this._onLoginResize, { passive: true })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this._onLoginResize)
  },
  methods: {
    parseCreatedUser(resData, form) {
      if (!resData || !form || !form.username) return null
      const user = { username: form.username, user_name: form.username, gender: form.gender, occupation: form.occupation, department: form.department, responsible_business: form.responsibleBusiness }
      const extractId = (obj) => (obj && typeof obj === 'object' ? (obj.id || obj.ID || null) : null)
      user.id = extractId(resData) || extractId(resData?.data) || extractId(resData?.data?.params) || extractId(resData?.data?.data)
      return user
    },
    async doLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        this.$message.warning('请输入用户名和密码')
        return
      }
      this.loginLoading = true
      try {
        const res = await this.$axios.post('/api/user/login', {
          username: this.loginForm.username,
          password: this.loginForm.password
        }, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
        if (res.data && res.data.success) {
          const user = res.data.user
          localStorage.setItem('current_user', JSON.stringify(user))
          this.$message.success('登录成功')
          this.$emit('login-success', user)
        } else {
          this.$message.warning(res.data?.message || '登录失败，请检查账号或稍后重试')
        }
      } catch (e) {
        this.$message.warning('登录失败：' + (e.response?.data?.message || e.message || '网络异常'))
      } finally {
        this.loginLoading = false
      }
    },
    async doRegister() {
      this.$refs.registerFormRef?.validate(async (valid) => {
        if (!valid) return
        this.registerLoading = true
        try {
          const body = buildCreateParams({
            username: this.registerForm.username,
            password: this.registerForm.password,
            gender: this.registerForm.gender || '',
            occupation: this.registerForm.occupation || '',
            department: this.registerForm.department || '',
            responsible_business: this.registerForm.responsibleBusiness || ''
          })
          const res = await this.$axios.post('/api/dynamic/api/User_Infor/create', body, {
            headers: { 'Content-Type': 'application/json;charset=UTF-8' }
          })
          this.$message.success('注册成功')
          const user = this.parseCreatedUser(res.data, this.registerForm)
          if (user) {
            localStorage.setItem('current_user', JSON.stringify(user))
            this.$emit('login-success', user)
          } else {
            this.activeTab = 'login'
            this.loginForm.username = this.registerForm.username
          }
          this.registerForm = { username: '', password: '', confirmPassword: '', gender: '', occupation: '', department: '', responsibleBusiness: '' }
        } catch (e) {
          this.$message.warning('注册失败：' + (e.response?.data?.message || e.message || '网络异常'))
        } finally {
          this.registerLoading = false
        }
      })
    },
    saveXdmToken() {
      const t = (this.xdmToken || '').trim()
      if (t) {
        localStorage.setItem('xdm_token', t)
        this.$message.success('已保存')
      } else {
        localStorage.removeItem('xdm_token')
        this.$message.info('已清除')
      }
    }
  }
}
</script>

<style scoped>
.login-register-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 12px max(10px, env(safe-area-inset-left, 0px)) max(16px, env(safe-area-inset-bottom, 0px)) max(10px, env(safe-area-inset-right, 0px));
  box-sizing: border-box;
  width: 100%;
  max-width: 100vw;
  overflow-x: clip;
}
.login-mascot-strip {
  display: flex;
  flex-direction: row;
  align-items: flex-end;
  justify-content: center;
  gap: min(6vw, 20px);
  width: 100%;
  max-width: 520px;
  padding: 0 8px 10px;
  flex-shrink: 0;
  pointer-events: none;
  user-select: none;
}
.login-mascot-strip__pulse {
  flex: 1 1 0;
  min-width: 0;
  max-width: 48%;
  display: flex;
  justify-content: center;
  transform-origin: 50% 90%;
  animation: login-gutter-pulse 3.5s ease-in-out infinite;
}
.login-mascot-strip__pulse--delay {
  animation-delay: -1.75s;
}
.login-mascot-strip__img {
  display: block;
  max-width: 100%;
  max-height: min(22vw, 130px);
  object-fit: contain;
  mix-blend-mode: normal;
  opacity: 0.96;
  filter: drop-shadow(0 8px 20px rgba(22, 40, 80, 0.12));
  border-radius: 6px;
}
.login-register-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 1200px;
  gap: 12px;
  position: relative;
  z-index: 1;
  flex: 0 1 auto;
}
.login-gutter {
  flex: 0 0 clamp(96px, 15vw, 220px);
  max-width: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  pointer-events: none;
  user-select: none;
}
.login-gutter__pulse {
  transform-origin: 50% 90%;
  animation: login-gutter-pulse 3.5s ease-in-out infinite;
}
.login-gutter__pulse--delay {
  animation-delay: -1.75s;
}
@keyframes login-gutter-pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}
.login-gutter__img {
  display: block;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: min(50vh, 500px);
  object-fit: contain;
  object-position: center;
  /* 实景图正常叠色；白底卡通图可改 multiply */
  mix-blend-mode: normal;
  opacity: 0.97;
  filter: saturate(1.02) drop-shadow(0 12px 28px rgba(22, 40, 80, 0.14));
  border-radius: 6px;
}
.form-container {
  flex: 0 0 auto;
  width: 420px;
  max-width: 100%;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.12);
  position: relative;
  z-index: 2;
}
.title {
  text-align: center;
  margin-bottom: 8px;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #165dff;
}
.title-sub {
  text-align: center;
  margin: 0 0 20px;
  font-size: 13px;
  line-height: 1.5;
  color: #909399;
}
.auth-form { margin-top: 16px; }
</style>
