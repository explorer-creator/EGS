<template>
  <div class="rag-agent">
    <h3>🤖 智慧物流 RAG 助手</h3>
    <div class="chat-box">
      <div v-for="(msg, index) in history" :key="index" :class="msg.role">
        <strong>{{ msg.role === 'user' ? '你' : 'AI' }}:</strong> {{ msg.content }}
      </div>
    </div>
    
    <div class="input-area">
      <input 
        v-model="userInput" 
        @keyup.enter="sendMessage" 
        placeholder="输入关于物流、单证的问题..." 
        :disabled="loading"
      />
      <button @click="sendMessage" :disabled="loading">
        {{ loading ? '思考中...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const userInput = ref('')
const history = ref([])
const loading = ref(false)

const sendMessage = async () => {
  if (!userInput.value.trim()) return

  const query = userInput.value
  history.value.push({ role: 'user', content: query })
  userInput.value = ''
  loading.value = true

  try {
    // 调用 Java 后端的 RAG 接口 (通过 vite 代理转发到 8080)
    const response = await fetch('/api/rag/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: query })
    })

    const data = await response.json()
    
    if (data.success) {
      history.value.push({ role: 'ai', content: data.answer })
    } else {
      history.value.push({ role: 'ai', content: `[错误]: ${data.message}` })
    }
  } catch (error) {
    history.value.push({ role: 'ai', content: '网络请求失败，请检查后端服务是否启动。' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.rag-agent { border: 1px solid #ccc; padding: 20px; border-radius: 8px; width: 400px; }
.chat-box { height: 300px; overflow-y: auto; margin-bottom: 10px; background: #f9f9f9; padding: 10px; }
.user { color: blue; margin-bottom: 8px; }
.ai { color: green; margin-bottom: 8px; }
.input-area { display: flex; gap: 8px; }
input { flex: 1; padding: 8px; }
button { padding: 8px 16px; cursor: pointer; }
</style>