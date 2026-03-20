<template>
  <div class="login-register-page">
    <BackgroundDecor />
    <div class="form-container">
      <h1 class="title">工业管理系统</h1>
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
        <el-collapse-item title="xDM-F 配置（注册需先配置，遇 403 时填写）" name="xdm">
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
  </div>
</template>

<script>
import BackgroundDecor from '../components/BackgroundDecor.vue'
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
  components: { BackgroundDecor },
  data() {
    return {
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
          this.$message.error(res.data?.message || '登录失败')
        }
      } catch (e) {
        this.$message.error('登录失败：' + (e.response?.data?.message || e.message))
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
          this.$message.error('注册失败：' + (e.response?.data?.message || e.message))
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
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.form-container {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.12);
  position: relative;
  z-index: 1;
}
.title {
  text-align: center;
  margin-bottom: 24px;
  font-size: 24px;
  color: #303133;
}
.auth-form { margin-top: 16px; }
</style>
