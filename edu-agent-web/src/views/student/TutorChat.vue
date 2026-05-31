<template>
  <div class="tutor-page">
    <section class="chat-panel">
      <header class="chat-header">
        <div>
          <p class="eyebrow">智能辅导</p>
          <h1>AI 学习问答助手</h1>
          <p>可以询问课程知识点、题目解析、学习计划和资源推荐。画像将在对话中自动更新。</p>
        </div>
        <button @click="resetChat">新对话</button>
      </header>

      <main class="message-list">
        <div v-if="loading" class="state-card">对话加载中...</div>

        <div v-for="message in messages" :key="message.id" :class="['message-item', message.role]">
          <div class="avatar">{{ message.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">
            <p>{{ message.content }}</p>
            <span>{{ message.time }}</span>
          </div>
        </div>

        <!-- AI 思考中 -->
        <div v-if="sending" class="message-item assistant">
          <div class="avatar">AI</div>
          <div class="bubble thinking-bubble">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            <span class="thinking-text">AI 正在思考中...</span>
          </div>
        </div>
        <div v-if="lastProfileChanges.has_changes" class="profile-update-notice">
          <el-icon><UserFilled /></el-icon>
          <span>画像已更新：
            <template v-for="(c, i) in lastProfileChanges.changed_dimensions" :key="c.dimension">
              {{ i > 0 ? '、' : '' }}{{ dimLabelMap[c.dimension] || c.dimension }}
              <template v-if="c.level_changed">
                {{ c.from_label || c.from_level }}→{{ c.to_label || c.to_level }}
              </template>
              <template v-else-if="c.score_change">
                {{ c.score_change }}
              </template>
            </template>
          </span>
          <router-link to="/student/profile/overview" class="view-link">查看画像</router-link>
        </div>
      </main>

      <footer class="chat-input">
        <textarea
          v-model="inputValue"
          placeholder="请输入你的问题..."
          @keydown.enter.exact.prevent="sendMessage"
          :disabled="sending"
        />
        <button @click="sendMessage" :disabled="sending">
          {{ sending ? '思考中...' : '发送' }}
        </button>
      </footer>
    </section>

    <aside class="side-panel">
      <!-- 画像摘要卡片 -->
      <section class="side-card profile-mini">
        <h3>
          学习画像
          <router-link to="/student/profile/overview" class="view-link">查看详情</router-link>
        </h3>
        <div v-if="profileExists" class="mini-dims">
          <div v-for="dim in miniDimensions" :key="dim.key" class="mini-dim">
            <span class="mini-dim-label">{{ dim.label }}</span>
            <span :class="['mini-dim-level', 'lvl-' + dim.levelNumber]">{{ dim.levelLabel }}</span>
          </div>
        </div>
        <div v-else class="mini-empty">对话中自动生成画像...</div>
      </section>

      <section class="side-card">
        <h3>推荐提问</h3>
        <div
          v-for="item in suggestions"
          :key="item.id"
          class="suggestion-item"
          @click="useSuggestion(item.prompt)"
        >
          {{ item.title }}
        </div>
      </section>

      <section class="side-card">
        <h3>辅导能力</h3>
        <div class="ability-list">
          <span>知识点讲解</span>
          <span>题目解析</span>
          <span>学习计划</span>
          <span>资源推荐</span>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import { getTutorSession, sendTutorMessage, resetSessionId } from '@/api/tutor'
import { getProfileFromAI, syncProfileToBackend, type ProfileData, type ProfileChanges } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import type { TutorMessage, TutorSuggestion } from '@/api/tutor'

const userStore = useUserStore()
const loading = ref(false)
const sending = ref(false)
const inputValue = ref('')
const messages = ref<TutorMessage[]>([])
const suggestions = ref<TutorSuggestion[]>([])
const profileData = ref<ProfileData>({})
const profileExists = ref(false)
const lastProfileChanges = ref<ProfileChanges>({ changed_dimensions: [], has_changes: false })

const dimLabelMap: Record<string, string> = {
  knowledge_mastery: '知识掌握度',
  learning_goal_clarity: '目标清晰度',
  cognitive_adaptation: '认知适配',
  mistake_avoidance: '错误规避',
  learning_autonomy: '学习自主',
  overall_level: '综合能力',
}

const miniDimensions = computed(() => {
  const keys = ['knowledge_mastery', 'learning_goal_clarity', 'cognitive_adaptation', 'mistake_avoidance', 'learning_autonomy', 'overall_level']
  return keys.map(k => {
    const dim = (profileData.value as any)[k] || { level_label: '入门', level_number: 1 }
    return {
      key: k,
      label: dimLabelMap[k] || k,
      levelLabel: dim.level_label || '入门',
      levelNumber: dim.level_number || 1,
    }
  })
})

const fallbackMessages: TutorMessage[] = [
  { id: 1, role: 'assistant', content: '你好，我是你的 AI 学习助手。你可以问我课程知识点、题目解析、学习计划或资源推荐。我们的对话将自动帮助你构建学习画像。', time: '09:00' },
]

const fallbackSuggestions: TutorSuggestion[] = [
  { id: 1, title: '解释 A* 算法核心思想', prompt: '请用简单例子解释 A* 算法的核心思想。' },
  { id: 2, title: '对比 BFS 和 DFS', prompt: 'BFS 和 DFS 的区别是什么？适合哪些场景？' },
  { id: 3, title: '生成本周学习计划', prompt: '请根据人工智能导论课程帮我生成本周学习计划。' },
  { id: 4, title: '推荐搜索算法资源', prompt: '推荐几个学习搜索算法的资源。' },
]

const loadProfile = async () => {
  const username = userStore.userInfo?.username || 'student001'
  try {
    const res = await getProfileFromAI(username)
    if (res.exists && res.profile) {
      profileExists.value = true
      profileData.value = res.profile
    }
  } catch {
    // silently ignore
  }
}

const fetchSession = async () => {
  loading.value = true
  try {
    const result = await getTutorSession()
    messages.value = result.messages
    suggestions.value = result.suggestions
  } catch {
    messages.value = fallbackMessages
    suggestions.value = fallbackSuggestions
  } finally {
    loading.value = false
  }
}

const sendMessage = async () => {
  const content = inputValue.value.trim()
  if (!content || sending.value) return

  messages.value.push({ id: Date.now(), role: 'user', content, time: '刚刚' })
  inputValue.value = ''
  sending.value = true

  try {
    const username = userStore.userInfo?.username || 'student001'
    const reply = await sendTutorMessage(content, username)
    messages.value.push(reply.message)

    if (reply.profile) {
      profileExists.value = true
      profileData.value = reply.profile
      // 同步到 Java 后端 MySQL
      syncProfileToBackend(username, reply.profile)
    }
    if (reply.profileChanges) {
      lastProfileChanges.value = reply.profileChanges
    }
  } catch {
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: `我理解你的问题是："${content}"。目前接口暂不可用，建议先结合课程章节、资源详情和练习题进行学习。`,
      time: '刚刚',
    })
  } finally {
    sending.value = false
  }
}

const useSuggestion = (prompt: string) => {
  inputValue.value = prompt
}

const resetChat = () => {
  messages.value = fallbackMessages.slice(0, 1)
  lastProfileChanges.value = { changed_dimensions: [], has_changes: false }
  resetSessionId()
}

onMounted(async () => {
  await Promise.all([fetchSession(), loadProfile()])
})
</script>

<style scoped>
.tutor-page {
  min-height: 100vh;
  padding: clamp(14px, 2vw, 28px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  background: #f5f8ff;
  color: #1f2a44;
  overflow-x: hidden;
}

.chat-panel, .side-card {
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.chat-panel {
  min-height: calc(100vh - 56px);
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid #eef2f8;
}

.eyebrow { margin: 0 0 8px; color: #1769ff; font-weight: 700; }
.chat-header h1 { margin: 0; }
.chat-header p, .side-card p { color: #667085; line-height: 1.7; }

.chat-header button, .chat-input button {
  border: none; border-radius: 12px; color: #ffffff; background: #1769ff; cursor: pointer;
}
.chat-header button { height: 40px; padding: 0 18px; }

.message-list {
  flex: 1; min-height: 0; padding: 20px; overflow-y: auto;
}

.message-item { display: flex; gap: 12px; margin-bottom: 18px; }
.message-item.user { flex-direction: row-reverse; }

.avatar {
  width: 38px; height: 38px; flex-shrink: 0;
  border-radius: 50%; color: #ffffff; background: #1769ff;
  display: flex; align-items: center; justify-content: center;
}
.message-item.user .avatar { background: #22c55e; }

.bubble {
  max-width: min(680px, 80%);
  padding: 14px 16px; border-radius: 18px; background: #f7faff;
}
.message-item.user .bubble { color: #ffffff; background: #1769ff; }
.bubble p { margin: 0; line-height: 1.7; }
.bubble span { display: block; margin-top: 8px; color: #8a96a8; font-size: 12px; }
.message-item.user .bubble span { color: rgba(255, 255, 255, 0.75); }

.profile-update-notice {
  display: flex; align-items: center; gap: 8px;
  margin: 12px 0; padding: 10px 14px;
  background: #f0f9eb; border-radius: 10px; font-size: 13px; color: #67c23a;
}
.profile-update-notice .view-link { margin-left: auto; color: #409eff; text-decoration: none; font-weight: 600; }

.chat-input {
  padding: 16px; display: flex; gap: 12px; border-top: 1px solid #eef2f8;
}
.chat-input textarea {
  flex: 1; min-height: 46px; max-height: 120px;
  padding: 12px; border: 1px solid #dbe4f3; border-radius: 14px;
  outline: none; resize: vertical;
}
.chat-input button { width: 88px; }

.side-panel { min-width: 0; display: flex; flex-direction: column; gap: 18px; }
.side-card { padding: 18px; border-radius: 20px; }
.side-card h3 { margin: 0 0 14px; display: flex; justify-content: space-between; align-items: center; }

.profile-mini .view-link { font-size: 12px; font-weight: 400; color: #409eff; text-decoration: none; }
.mini-dims { display: flex; flex-direction: column; gap: 8px; }
.mini-dim { display: flex; justify-content: space-between; align-items: center; }
.mini-dim-label { font-size: 13px; color: #606266; }
.mini-dim-level { font-size: 12px; padding: 2px 10px; border-radius: 10px; font-weight: 600; }
.lvl-1 { background: #fdf6ec; color: #e6a23c; }
.lvl-2 { background: #ecf5ff; color: #409eff; }
.lvl-3 { background: #f0f9eb; color: #67c23a; }
.mini-empty { font-size: 13px; color: #909399; text-align: center; padding: 16px 0; }

.suggestion-item {
  padding: 12px; margin-bottom: 10px;
  border-radius: 14px; color: #1769ff; background: #eef5ff; cursor: pointer;
}
.suggestion-item:hover { background: #dfeeff; }

.ability-list { display: flex; flex-wrap: wrap; gap: 10px; }
.ability-list span {
  padding: 7px 12px; border-radius: 999px;
  color: #1769ff; background: #eef5ff; font-size: 13px;
}

.state-card { padding: 40px; text-align: center; color: #75849a; }

.thinking-bubble { display: flex; align-items: center; gap: 4px; padding: 14px 20px; }
.thinking-bubble .dot {
  width: 8px; height: 8px; background: #1769ff; border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}
.thinking-bubble .dot:nth-child(1) { animation-delay: 0s; }
.thinking-bubble .dot:nth-child(2) { animation-delay: 0.2s; }
.thinking-bubble .dot:nth-child(3) { animation-delay: 0.4s; }
.thinking-text { font-size: 13px; color: #909399; margin-left: 8px; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}
.chat-input textarea:disabled,
.chat-input button:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 980px) {
  .tutor-page { grid-template-columns: 1fr; }
  .chat-panel { min-height: 640px; }
}
@media (max-width: 560px) {
  .tutor-page { padding: 12px; }
  .chat-header, .chat-input { flex-direction: column; }
  .chat-header button, .chat-input button { width: 100%; height: 40px; }
  .bubble { max-width: 82%; }
}
</style>
