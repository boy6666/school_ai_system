<template>
  <div class="tutor-page">
    <!-- 左侧：历史会话列表 -->
    <aside class="history-sidebar">
      <div class="history-header">
        <el-button type="primary" size="small" @click="newChat" block>+ 新对话</el-button>
      </div>
      <div class="history-list">
        <div
          v-for="s in sessions"
          :key="s.sessionId"
          class="history-item"
          :class="{ active: s.sessionId === currentSessionId }"
          @click="switchSession(s.sessionId)"
        >
          <div class="history-title">{{ s.title || '新对话' }}</div>
          <div class="history-time">{{ formatTime(s.time) }}</div>
        </div>
        <div v-if="sessions.length === 0" class="history-empty">暂无历史对话</div>
      </div>
    </aside>

    <!-- 右侧：对话区 -->
    <main class="chat-area">
      <div v-if="!currentSessionId && messages.length === 0" class="chat-empty">
        <h2>AI 智能辅导</h2>
        <p>输入你的学习问题，开始对话</p>
      </div>

      <div class="chat-messages" ref="msgContainer" v-else>
        <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="msg-bubble typing">思考中...</div>
        </div>
      </div>

      <div class="chat-input" v-if="currentSessionId || messages.length === 0">
        <el-input
          v-model="input"
          placeholder="输入你的学习问题..."
          @keyup.enter="send"
          :disabled="loading"
          size="large"
        />
        <el-button type="primary" @click="send" :loading="loading" size="large">发送</el-button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { sendTutorMessage } from '@/api/tutor'
import { ElMessage } from 'element-plus'

const input = ref('')
const loading = ref(false)
const messages = ref<{ role: string; content: string }[]>([])
const currentSessionId = ref('')
const sessions = ref<{ sessionId: string; title: string; time: string }[]>([])
const msgContainer = ref<HTMLElement>()

const STORAGE_KEY = 'tutor_sessions'
const MSG_KEY = 'tutor_current_messages'

// 从后端加载会话列表
const loadSessions = async () => {
  try {
    const res = await fetch('/api/tutor/sessions', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    const data = await res.json()
    if (data.code === 200 && data.data) {
      sessions.value = data.data
    }
  } catch { sessions.value = [] }
}

// 保存会话列表（仅当前会话，后端已持久化）
const saveSessions = () => {}  // no-op: server is source of truth

// 加载当前消息
const loadMessages = () => {
  const stored = localStorage.getItem(MSG_KEY)
  if (stored) {
    try { messages.value = JSON.parse(stored) } catch { messages.value = [] }
  }
}
const saveMessages = () => {
  localStorage.setItem(MSG_KEY, JSON.stringify(messages.value))
}

// 滚动到底部
const scrollBottom = async () => {
  await nextTick()
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

// 新建对话
const newChat = () => {
  currentSessionId.value = 'session_' + Date.now()
  messages.value = []
  saveMessages()
}

// 切换会话
const switchSession = (sessionId: string) => {
  currentSessionId.value = sessionId
  // 从后端加载历史消息
  loadHistoryFromServer(sessionId)
}

// 从后端加载历史
const loadHistoryFromServer = async (sessionId: string) => {
  loading.value = true
  try {
    const res = await fetch(`/api/tutor/history?sessionId=${sessionId}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    const data = await res.json()
    if (data.code === 200 && data.data) {
      messages.value = data.data.flatMap((r: any) => [
        { role: 'user', content: '问题记录' },  // conversation 表存的是 question + answer
        { role: 'assistant', content: r.answer }
      ])
    }
  } catch {
    // 静默失败
  }
  loading.value = false
  saveMessages()
}

// 发送消息
const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return

  // 如果没有当前会话，先创建
  if (!currentSessionId.value) {
    currentSessionId.value = 'session_' + Date.now()
  }

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  saveMessages()
  await scrollBottom()

  try {
    const result = await sendTutorMessage(text, currentSessionId.value)
    const answer = result?.answer || result?.finalAnswer || ''
    if (answer) {
      messages.value.push({ role: 'assistant', content: answer })
    }
    
    // 刷新会话列表
    loadSessions()
  } catch {
    ElMessage.error('请求失败，请确认后端和AI引擎是否运行')
  }
  loading.value = false
  saveMessages()
  await scrollBottom()
}

const formatTime = (t: string) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(() => {
  loadSessions()

  // 先检测是否有从题目页传来的预置消息（优先级高于 history）
  const presetRaw = localStorage.getItem('tutor_current_messages')
  localStorage.removeItem('tutor_current_messages')

  if (presetRaw) {
    try {
      const preset = JSON.parse(presetRaw)
      const firstUser = Array.isArray(preset) && preset.find((m: any) => m.role === 'user')
      if (firstUser?.content) {
        // 直接设 input 并发送，让 send() 创建 session + 存后端
        currentSessionId.value = ''
        input.value = firstUser.content
        nextTick().then(() => send())
      }
    } catch { /* ignore */ }
  } else {
    loadMessages()
  }

  if (messages.value.length > 0) {
    currentSessionId.value = localStorage.getItem('tutor_current_session') || ''
  }
})

watch(currentSessionId, (val) => {
  if (val) localStorage.setItem('tutor_current_session', val)
})
</script>

<style scoped>
.tutor-page { display: flex; height: calc(100vh - 60px); background: #fff; }
.history-sidebar {
  width: 240px; background: #f8f9fb; border-right: 1px solid #e4e7ed;
  display: flex; flex-direction: column; flex-shrink: 0;
}
.history-header { padding: 12px; border-bottom: 1px solid #e4e7ed; }
.history-list { flex: 1; overflow-y: auto; padding: 8px; }
.history-item {
  padding: 10px 12px; border-radius: 8px; cursor: pointer; margin-bottom: 4px;
}
.history-item:hover { background: #e8ecf1; }
.history-item.active { background: #409eff10; border: 1px solid #409eff30; }
.history-title { font-size: 14px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.history-time { font-size: 11px; color: #c0c4cc; margin-top: 2px; }
.history-empty { color: #909399; font-size: 13px; text-align: center; padding: 20px; }

.chat-area { flex: 1; display: flex; flex-direction: column; }
.chat-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #909399; }
.chat-empty h2 { color: #303133; margin-bottom: 8px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px 24px; }
.message { margin-bottom: 16px; display: flex; }
.message.user { justify-content: flex-end; }
.msg-bubble {
  max-width: 75%; padding: 12px 16px; border-radius: 12px;
  white-space: pre-wrap; font-size: 14px; line-height: 1.6;
}
.message.user .msg-bubble { background: #409eff; color: #fff; border-bottom-right-radius: 4px; }
.message.assistant .msg-bubble { background: #f0f2f5; color: #303133; border-bottom-left-radius: 4px; }
.typing { color: #909399; font-style: italic; }

.chat-input {
  padding: 16px 24px; border-top: 1px solid #e4e7ed;
  display: flex; gap: 12px; background: #fff;
}
</style>
