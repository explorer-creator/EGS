<template>
  <div
    class="robot-agent"
    :style="agentStyle"
    @mousedown="onDragStart"
  >
    <!-- 小机器人头像：全页面可拖拽 -->
    <div
      class="robot-avatar"
      :class="{ 'is-open': chatVisible }"
      @click="onAvatarClick"
    >
      <div class="robot-face">
        <span class="robot-eye left"></span>
        <span class="robot-eye right"></span>
        <span class="robot-mouth"></span>
      </div>
    </div>

    <!-- 聊天面板 -->
    <transition name="slide-up">
      <div v-show="chatVisible" class="chat-panel">
        <div class="chat-header">
          <span class="chat-title">🤖 小助手</span>
          <el-button type="text" icon="el-icon-close" @click="chatVisible = false" />
        </div>
        <div class="chat-messages" ref="chatMessages">
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="chat-msg"
            :class="msg.role"
          >
            <div v-if="msg.role === 'robot'" class="msg-avatar">🤖</div>
            <div class="msg-bubble">
              <div class="msg-content">{{ msg.content }}</div>
            </div>
          </div>
          <div v-if="chatLoading" class="chat-msg robot">
            <div class="msg-avatar">🤖</div>
            <div class="msg-bubble">
              <span class="msg-loading">正在思考...</span>
            </div>
          </div>
        </div>
        <div class="chat-input-area">
          <el-input
            v-model="inputText"
            placeholder="输入消息，或双击专业名词让我解释~"
            @keyup.enter.native="sendMessage"
          >
            <el-button slot="append" type="primary" :loading="chatLoading" @click="sendMessage">发送</el-button>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
const GREETINGS = [
  '你好呀！有什么可以帮你的吗？',
  '嗨~ 需要我解释什么专业名词吗？双击即可哦~',
  '今天也要加油呀！有问题随时找我~',
  '我在这里哦，双击看不懂的词我帮你解释！'
]

export default {
  name: 'RobotAgent',
  data() {
    return {
      chatVisible: false,
      inputText: '',
      messages: [],
      chatLoading: false,
      greetTimer: null,
      // 拖拽相关
      posX: 24,
      posY: 24,
      dragStartX: 0,
      dragStartY: 0,
      dragStartLeft: 0,
      dragStartBottom: 0,
      isDragging: false,
      hasDragged: false
    }
  },
  computed: {
    agentStyle() {
      return { left: this.posX + 'px', bottom: this.posY + 'px' }
    }
  },
  mounted() {
    this.startGreetTimer()
    document.addEventListener('mousemove', this.onDragMove)
    document.addEventListener('mouseup', this.onDragEnd)
  },
  beforeDestroy() {
    if (this.greetTimer) clearInterval(this.greetTimer)
    document.removeEventListener('mousemove', this.onDragMove)
    document.removeEventListener('mouseup', this.onDragEnd)
  },
  methods: {
    onDragStart(e) {
      if (e.target.closest('.chat-panel') || e.target.closest('.chat-input-area')) return
      this.isDragging = false
      this.dragStartX = e.clientX
      this.dragStartY = e.clientY
      this.dragStartLeft = this.posX
      this.dragStartBottom = this.posY
    },
    onDragMove(e) {
      if (this.dragStartX === undefined) return
      const dx = e.clientX - this.dragStartX
      const dy = e.clientY - this.dragStartY
      if (!this.isDragging && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) {
        this.isDragging = true
        this.hasDragged = true
      }
      if (this.isDragging) {
        this.posX = Math.max(0, Math.min(window.innerWidth - 70, this.dragStartLeft + dx))
        this.posY = Math.max(0, Math.min(window.innerHeight - 70, this.dragStartBottom - dy))
      }
    },
    onDragEnd() {
      this.dragStartX = undefined
      this.isDragging = false
      setTimeout(() => { this.hasDragged = false }, 0)
    },
    onAvatarClick() {
      if (!this.hasDragged) this.toggleChat()
    },
    toggleChat() {
      this.chatVisible = !this.chatVisible
      if (this.chatVisible && this.messages.length === 0) {
        this.messages.push({
          role: 'robot',
            content: '你好！我是可爱的小助手~ 你可以和我聊天，或者在产品制造问答里双击专业名词，我会帮你解释哦~'
        })
      }
    },
    startGreetTimer() {
      this.greetTimer = setInterval(() => {
        if (!this.chatVisible) return
        if (Math.random() < 0.35) {
          const g = GREETINGS[Math.floor(Math.random() * GREETINGS.length)]
          this.messages.push({ role: 'robot', content: g })
          this.scrollToBottom()
        }
      }, 90000)
    },
    async sendMessage() {
      const text = (this.inputText || '').trim()
      if (!text) return

      this.inputText = ''
      this.messages.push({ role: 'user', content: text })
      this.chatLoading = true
      this.scrollToBottom()

      try {
        const res = await this.$axios.post('/api/llm/chat', {
          message: text,
          explainTerm: false
        })
        const data = res.data || res
        if (data.success) {
          this.messages.push({ role: 'robot', content: data.content || '' })
        } else {
          this.messages.push({ role: 'robot', content: '抱歉，出错了：' + (data.message || '未知错误') })
        }
      } catch (e) {
        this.messages.push({ role: 'robot', content: '网络错误，请稍后再试~' })
      } finally {
        this.chatLoading = false
        this.$nextTick(() => this.scrollToBottom())
      }
    },
    /** 由外部调用：解释专业名词 */
    explainTerm(term) {
      const t = (term || '').trim()
      if (!t) return
      if (!this.chatVisible) this.chatVisible = true
      this.messages.push({ role: 'user', content: '请解释：「' + t + '」' })
      this.chatLoading = true
      this.scrollToBottom()

      this.$axios.post('/api/llm/chat', { message: t, explainTerm: true })
        .then(res => {
          const data = res.data || res
          if (data.success) {
            this.messages.push({ role: 'robot', content: data.content || '' })
          } else {
            this.messages.push({ role: 'robot', content: '暂时无法解释这个词，换个试试？' })
          }
        })
        .catch(() => {
          this.messages.push({ role: 'robot', content: '网络错误，请稍后再试~' })
        })
        .finally(() => {
          this.chatLoading = false
          this.$nextTick(() => this.scrollToBottom())
        })
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.chatMessages
        if (el) el.scrollTop = el.scrollHeight
      })
    }
  }
}
</script>

<style scoped>
.robot-agent {
  position: fixed;
  z-index: 999;
  cursor: grab;
  user-select: none;
}
.robot-agent:active {
  cursor: grabbing;
}
.robot-avatar {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 50%, #fecfef 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(255, 154, 158, 0.5);
  transition: transform 0.2s;
  border: 3px solid #fff;
}
.robot-avatar:hover {
  transform: scale(1.08);
}
.robot-avatar.is-open {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(255, 154, 158, 0.6);
}
.robot-face {
  position: relative;
  width: 28px;
  height: 24px;
}
.robot-eye {
  position: absolute;
  top: 2px;
  width: 6px;
  height: 6px;
  background: #333;
  border-radius: 50%;
  animation: blink 3s infinite;
}
.robot-eye.left { left: 2px; }
.robot-eye.right { right: 2px; }
.robot-mouth {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 14px;
  height: 6px;
  border: 2px solid #333;
  border-top: none;
  border-radius: 0 0 14px 14px;
}
@keyframes blink {
  0%, 90%, 100% { transform: scaleY(1); }
  95% { transform: scaleY(0.1); }
}

.chat-panel {
  position: absolute;
  left: 0;
  bottom: 72px;
  width: 340px;
  max-height: 420px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-header {
  padding: 12px 16px;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chat-title {
  font-weight: 600;
  font-size: 15px;
}
.chat-header .el-button {
  color: #fff;
  padding: 4px;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 200px;
  max-height: 280px;
}
.chat-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.chat-msg.user {
  flex-direction: row-reverse;
}
.chat-msg.user .msg-bubble {
  background: #165DFF;
  color: #fff;
  border-radius: 12px 12px 4px 12px;
}
.chat-msg.robot .msg-avatar {
  flex-shrink: 0;
  font-size: 24px;
}
.msg-bubble {
  background: #f0f2f5;
  padding: 10px 14px;
  border-radius: 12px 12px 12px 4px;
  max-width: 85%;
}
.msg-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-loading {
  font-size: 13px;
  color: #909399;
}
.chat-input-area {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
}
.chat-input-area >>> .el-input-group__append {
  padding: 0;
}
.chat-input-area >>> .el-input-group__append .el-button {
  margin: 0;
  border-radius: 0 4px 4px 0;
  padding: 10px 16px;
}

.slide-up-enter-active, .slide-up-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.slide-up-enter, .slide-up-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
