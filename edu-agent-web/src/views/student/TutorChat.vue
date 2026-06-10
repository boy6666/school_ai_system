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
          <!-- 解析 JSON 结构化消息 -->
          <div v-if="isJsonMsg(msg)" class="msg-bubble structured">
            <!-- 讲解 -->
            <div v-if="parseJsonMsg(msg).type === 'explain'" class="structured-explain">
              <div class="struct-label">📖 个性化讲解</div>
              <div class="struct-content" v-html="renderMarkdown(parseJsonMsg(msg).content)"></div>
              <div v-if="parseJsonMsg(msg).weaknesses_focus?.length" class="struct-tags">
                <el-tag v-for="w in parseJsonMsg(msg).weaknesses_focus" :key="w" type="warning" size="small">{{ w }}</el-tag>
              </div>
              <div v-if="parseJsonMsg(msg).suggestion" class="struct-suggestion">
                💡 {{ parseJsonMsg(msg).suggestion }}
              </div>
            </div>
            <!-- 题目 -->
            <div v-else-if="parseJsonMsg(msg).type === 'quiz'" class="structured-quiz">
              <div class="struct-label">📝 个性化练习题</div>
              <div v-for="(q, qi) in parseJsonMsg(msg).questions" :key="qi" class="quiz-item">
                <div class="q-header">
                  <span class="q-num">{{ qi + 1 }}</span>
                  <span class="q-type-tag">{{ q.type === 'choice' ? '选择题' : '简答题' }}</span>
                  <span class="q-text">{{ q.question }}</span>
                </div>
                <div v-if="q.options?.length" class="q-options">
                  <div v-for="(opt, oj) in q.options" :key="oj" class="opt-btn" :class="{ 'is-correct': opt === q.answer }">
                    {{ opt }}
                  </div>
                </div>
                <div class="q-answer">
                  <span class="ans-label">参考答案：</span>
                  <span class="ans-text">{{ q.answer }}</span>
                </div>
                <div v-if="q.explanation" class="q-explain">
                  <div class="explain-hd">📖 解析</div>
                  <div class="explain-bd">{{ q.explanation }}</div>
                </div>
              </div>
            </div>
            <!-- 分步骤辅导 -->
            <div v-else-if="parseJsonMsg(msg).type === 'tutor'" class="structured-tutor">
              <div class="struct-label">🎯 智能辅导</div>
              <div v-for="(step, si) in parseJsonMsg(msg).steps" :key="si" class="tutor-step">
                <span class="step-num">{{ si + 1 }}</span>
                <span class="step-text" v-html="renderMarkdown(step)"></span>
              </div>
              <div v-if="parseJsonMsg(msg).weaknesses_touched?.length" class="struct-tags">
                <el-tag v-for="w in parseJsonMsg(msg).weaknesses_touched" :key="w" type="danger" size="small">{{ w }}</el-tag>
              </div>
              <div v-if="parseJsonMsg(msg).summary" class="struct-summary">{{ parseJsonMsg(msg).summary }}</div>
            </div>
            <!-- 资源推荐 -->
            <div v-else-if="parseJsonMsg(msg).type === 'retrieval'" class="structured-retrieval">
              <div class="struct-label">📚 资源推荐</div>
              <div v-for="(rec, ri) in parseJsonMsg(msg).recommendations" :key="ri" class="rec-item">
                <div class="rec-title">
                  <el-tag :type="rec.priority === '高' ? 'danger' : rec.priority === '中' ? 'warning' : 'info'" size="small">{{ rec.priority }}</el-tag>
                  <span class="rec-name">{{ rec.title }}</span>
                  <el-tag type="success" size="small" style="margin-left:6px">{{ rec.type }}</el-tag>
                </div>
                <div class="rec-reason">{{ rec.reason }}</div>
              </div>
              <div v-if="parseJsonMsg(msg).summary" class="struct-summary">{{ parseJsonMsg(msg).summary }}</div>
            </div>
            <!-- 未知类型兜底 -->
            <div v-else class="struct-raw">{{ msg.content }}</div>
          </div>
          <!-- 纯文本消息 -->
          <div v-else class="msg-bubble">{{ msg.content }}</div>
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
        { role: 'user', content: r.question || '（问题记录）' },
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

/** 判断消息内容是否为 JSON */
const isJsonMsg = (msg: any) => {
  if (!msg?.content || msg.role !== 'assistant') return false
  const s = String(msg.content).trim()
  return s.startsWith('{') || s.startsWith('[')
}

/** 安全解析 JSON 消息，失败时返回空对象 */
const parseJsonMsg = (msg: any) => {
  try {
    return JSON.parse(String(msg.content))
  } catch {
    return {}
  }
}

/** 简单 Markdown 转 HTML（支持换行、加粗、代码块） */
const renderMarkdown = (text: string) => {
  if (!text) return ''
  let html = text
    .replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
  return html
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

/* ===== 结构化消息 ===== */
.msg-bubble.structured {
  max-width: 85% !important;
  background: #fff !important;
  border: 1px solid #e4e7ed;
  padding: 16px !important;
  border-radius: 12px !important;
}
.struct-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
}
.struct-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}
.struct-content :deep(pre) {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  margin: 8px 0;
}
.struct-content :deep(code) {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: #d63384;
}
.struct-tags {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.struct-suggestion {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 8px;
  font-size: 13px;
  color: #409eff;
}
.struct-summary {
  margin-top: 12px;
  padding: 10px 14px;
  background: #f8faff;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

/* 题目样式 */
.structured-quiz .quiz-item {
  background: #f8f9fb;
  padding: 14px;
  border-radius: 8px;
  margin-bottom: 10px;
  border-left: 3px solid #4f8cff;
}
.structured-quiz .q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.structured-quiz .q-num {
  display: inline-flex;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #4f8cff;
  color: #fff;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}
.structured-quiz .q-type-tag {
  font-size: 11px;
  color: #909399;
  background: #e8e8e8;
  padding: 1px 6px;
  border-radius: 4px;
}
.structured-quiz .q-text {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.structured-quiz .q-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0 0 32px;
}
.structured-quiz .opt-btn {
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
}
.structured-quiz .opt-btn.is-correct {
  border-color: #52c41a;
  background: #e6f7e6;
  color: #52c41a;
  font-weight: 600;
}
.structured-quiz .q-answer {
  margin: 8px 0 0 32px;
  font-size: 13px;
}
.structured-quiz .ans-label {
  color: #909399;
}
.structured-quiz .ans-text {
  color: #52c41a;
  font-weight: 500;
}
.structured-quiz .q-explain {
  margin: 8px 0 0 32px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}
.structured-quiz .explain-hd {
  font-size: 12px;
  font-weight: 600;
  color: #4f8cff;
  padding: 8px 12px;
  background: #f8faff;
  border-bottom: 1px solid #e8e8e8;
}
.structured-quiz .explain-bd {
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.6;
  color: #333;
}

/* 辅导步骤 */
.structured-tutor .tutor-step {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #f8f9fb;
  border-radius: 8px;
}
.structured-tutor .step-num {
  display: inline-flex;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex-shrink: 0;
  margin-top: 2px;
}
.structured-tutor .step-text {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  flex: 1;
}

/* 资源推荐 */
.structured-retrieval .rec-item {
  padding: 10px 12px;
  background: #f8f9fb;
  border-radius: 8px;
  margin-bottom: 8px;
}
.structured-retrieval .rec-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.structured-retrieval .rec-name {
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}
.structured-retrieval .rec-reason {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin-left: 4px;
}
</style>
