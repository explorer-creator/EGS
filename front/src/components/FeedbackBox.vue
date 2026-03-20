<template>
  <div class="feedback-box">
    <!-- 悬浮反馈按钮 -->
    <div class="feedback-trigger" @click="dialogVisible = true">
      <i class="el-icon-chat-dot-round"></i>
      <span>反馈</span>
    </div>

    <!-- 反馈弹窗：append-to-body 避免灰屏/遮罩层级问题 -->
    <el-dialog
      title="意见反馈"
      :visible.sync="dialogVisible"
      width="480px"
      :close-on-click-modal="false"
      append-to-body
      @close="resetForm"
    >
      <!-- 表单状态 -->
      <div v-if="!submitted" class="feedback-form">
        <el-form :model="form" label-width="80px">
          <el-form-item label="评价" required>
            <el-rate v-model="form.rating" :max="5" show-text :texts="['很差', '一般', '满意', '很满意', '非常满意']" />
          </el-form-item>
          <el-form-item label="文字意见">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              placeholder="请输入您的意见或建议..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitFeedback">提交反馈</el-button>
            <el-button @click="dialogVisible = false">取消</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 感谢信息 -->
      <div v-else class="thank-you">
        <i class="el-icon-success success-icon"></i>
        <p class="thank-title">感谢您的反馈！</p>
        <p class="thank-desc">您的意见对我们非常重要，我们会认真阅读并持续改进。</p>
        <el-button type="primary" @click="dialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { config } from '../config'
const TENANT_ID = config.tenantId

function buildCreateParams(data) {
  return {
    params: {
      id: '',
      modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
      lastUpdateTime: '',
      creator: 'admin',
      rdmExtensionType: 'Feedback',
      tenant: { id: TENANT_ID, clazz: 'Tenant' },
      ...data
    }
  }
}

export default {
  name: 'FeedbackBox',
  data() {
    return {
      dialogVisible: false,
      submitted: false,
      submitting: false,
      form: {
        rating: 0,
        content: ''
      }
    }
  },
  methods: {
    resetForm() {
      this.form = { rating: 0, content: '' }
      this.submitted = false
    },
    async submitFeedback() {
      if (this.form.rating === 0) {
        this.$message.warning('请先进行星级评价')
        return
      }
      this.submitting = true
      try {
        const body = buildCreateParams({
          rating: this.form.rating,
          content: this.form.content || '',
          feedback_content: this.form.content || ''
        })
        await this.$axios.post('/api/dynamic/api/Feedback/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        this.submitted = true
      } catch (e) {
        this.$message.error('提交失败：' + (e.response?.data?.message || e.message))
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.feedback-trigger {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  z-index: 1000;
  transition: transform 0.2s;
}
.feedback-trigger:hover {
  transform: scale(1.05);
}
.feedback-trigger i {
  font-size: 22px;
}
.feedback-trigger span {
  font-size: 11px;
  margin-top: 2px;
}
.thank-you {
  text-align: center;
  padding: 24px 0;
}
.success-icon {
  font-size: 64px;
  color: #67c23a;
  margin-bottom: 16px;
}
.thank-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}
.thank-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 24px 0;
}
</style>
