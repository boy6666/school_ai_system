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
                  <span class="q-num">{{ Number(qi) + 1 }}</span>
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
                <span class="step-num">{{ Number(si) + 1 }}</span>
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
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import {
  getSessions,
  getTutorHistory,
  sendTutorMessage,
  type TutorSession
} from '@/api/tutor'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

interface StructuredQuestion {
  type?: string
  question?: string
  options?: string[]
  answer?: string
  explanation?: string
}

interface StructuredRecommendation {
  priority?: string
  title?: string
  type?: string
  reason?: string
}

interface StructuredMessage {
  type?: string
  content?: string
  weaknesses_focus?: string[]
  suggestion?: string
  questions?: StructuredQuestion[]
  steps?: string[]
  weaknesses_touched?: string[]
  summary?: string
  recommendations?: StructuredRecommendation[]
}

const route = useRoute()
const userStore = useUserStore()
const input = ref('')
const loading = ref(false)
const messages = ref<ChatMessage[]>([])
const currentSessionId = ref('')
const sessions = ref<TutorSession[]>([])
const msgContainer = ref<HTMLElement>()

// 从后端加载会话列表
const loadSessions = async () => {
  try {
    sessions.value = await getSessions()
  } catch {
    sessions.value = []
    ElMessage.error('历史会话加载失败，请稍后重试')
  }
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
    const history = await getTutorHistory(sessionId)
    messages.value = history.map((item) => ({
      role: item.role,
      content: item.content
    }))
    await scrollBottom()
  } catch {
    messages.value = []
    ElMessage.error('对话记录加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 发送消息
const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return

  const studentId =
    userStore.userInfo?.userId ??
    userStore.userInfo?.id

  if (studentId === undefined) {
    ElMessage.error('无法获取当前用户信息，请重新登录')
    return
  }

  // 如果没有当前会话，先创建
  if (!currentSessionId.value) {
    currentSessionId.value = 'session_' + Date.now()
  }

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollBottom()

  try {
    const result = await sendTutorMessage(
      text,
      String(studentId),
      currentSessionId.value
    )
    const answer = result?.final_answer || ''

    if (answer) {
      messages.value.push({
        role: 'assistant',
        content: answer
      })
    }

    // 刷新会话列表
    await loadSessions()
  } catch {
    ElMessage.error('请求失败，请确认后端和AI引擎是否运行')
  } finally {
    loading.value = false
  }
  await scrollBottom()
}

/** 判断消息内容是否为 JSON */
const isJsonMsg = (msg: ChatMessage) => {
  if (!msg?.content || msg.role !== 'assistant') return false
  const s = String(msg.content).trim()
  return s.startsWith('{') || s.startsWith('[')
}

/** 安全解析 JSON 消息，失败时返回空对象 */
const parseJsonMsg = (msg: ChatMessage): StructuredMessage => {
  try {
    const parsed: unknown = JSON.parse(String(msg.content))
    return typeof parsed === 'object' && parsed !== null
      ? parsed as StructuredMessage
      : {}
  } catch {
    return {}
  }
}

/** 简单 Markdown 转 HTML（支持换行、加粗、代码块） */
const renderMarkdown = (text?: string) => {
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

onMounted(async () => {
  await loadSessions()

  const preset = Array.isArray(route.query.prompt)
    ? route.query.prompt[0]
    : route.query.prompt
  if (preset) {
    input.value = preset
    await send()
  }
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