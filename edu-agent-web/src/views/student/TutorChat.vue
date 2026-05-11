<template>
  <div class="tutor-page">
    <section class="chat-panel">
      <header class="chat-header">
        <div>
          <p class="eyebrow">智能辅导</p>
          <h1>AI 学习问答助手</h1>
          <p>可以询问课程知识点、题目解析、学习计划和资源推荐。</p>
        </div>

        <button @click="resetChat">新对话</button>
      </header>

      <main class="message-list">
        <div v-if="loading" class="state-card">
          对话加载中...
        </div>

        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message-item', message.role]"
        >
          <div class="avatar">
            {{ message.role === 'user' ? '我' : 'AI' }}
          </div>

          <div class="bubble">
            <p>{{ message.content }}</p>
            <span>{{ message.time }}</span>
          </div>
        </div>
      </main>

      <footer class="chat-input">
        <textarea
          v-model="inputValue"
          placeholder="请输入你的问题，例如：A* 算法和 BFS 有什么区别？"
          @keydown.enter.exact.prevent="sendMessage"
        />

        <button @click="sendMessage">
          发送
        </button>
      </footer>
    </section>

    <aside class="side-panel">
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
        <h3>学习上下文</h3>
        <p>当前课程：人工智能导论</p>
        <p>当前章节：搜索算法</p>
        <p>当前目标：理解 BFS、DFS、A* 算法区别</p>
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
import { onMounted, ref } from 'vue'
import {
  getTutorSession,
  sendTutorMessage
} from '@/api/tutor'

import type {
  TutorMessage,
  TutorSuggestion
} from '@/api/tutor'

const loading = ref(false)
const inputValue = ref('')

const messages = ref<TutorMessage[]>([])
const suggestions = ref<TutorSuggestion[]>([])

const fallbackMessages: TutorMessage[] = [
  {
    id: 1,
    role: 'assistant',
    content: '你好，我是你的 AI 学习助手。你可以问我课程知识点、题目解析、学习计划或资源推荐。',
    time: '09:00'
  },
  {
    id: 2,
    role: 'user',
    content: 'A* 算法和 BFS 有什么区别？',
    time: '09:01'
  },
  {
    id: 3,
    role: 'assistant',
    content: 'BFS 是无权图中的广度优先搜索，按层扩展节点；A* 算法会结合实际代价和启发式估价，更适合路径规划问题。',
    time: '09:01'
  }
]

const fallbackSuggestions: TutorSuggestion[] = [
  {
    id: 1,
    title: '解释 A* 算法核心思想',
    prompt: '请用简单例子解释 A* 算法的核心思想。'
  },
  {
    id: 2,
    title: '对比 BFS 和 DFS',
    prompt: 'BFS 和 DFS 的区别是什么？适合哪些场景？'
  },
  {
    id: 3,
    title: '生成本周学习计划',
    prompt: '请根据人工智能导论课程帮我生成本周学习计划。'
  },
  {
    id: 4,
    title: '推荐搜索算法资源',
    prompt: '推荐几个学习搜索算法的资源。'
  }
]

const fetchSession = async () => {
  loading.value = true

  try {
    const result = await getTutorSession()

    messages.value = result.messages
    suggestions.value = result.suggestions
  } catch (error) {
    console.warn('智能辅导接口暂不可用，使用页面静态数据：', error)

    messages.value = fallbackMessages
    suggestions.value = fallbackSuggestions
  } finally {
    loading.value = false
  }
}

const sendMessage = async () => {
  const content = inputValue.value.trim()

  if (!content) return

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content,
    time: '刚刚'
  })

  inputValue.value = ''

  const rawProfile = localStorage.getItem('studentProfile')
  const currentProfile = rawProfile ? JSON.parse(rawProfile) : null

  try {
    const res = await sendTutorMessage(content, currentProfile)

  messages.value.push({
  id: res.data.id || Date.now() + 1,
  role: res.data.role || 'assistant',
  content: res.data.content || res.data.message || '已收到你的问题，我会结合你的学习画像进行分析。',
  time: res.data.time || '刚刚'
})
  } catch (error) {
    console.warn('智能辅导问答接口暂不可用，使用模拟回复：', error)

    const profileTip = currentProfile
      ? `\n\n我会参考你的学习画像：${currentProfile.overallType}、${currentProfile.cognitiveStyle}、易错点：${currentProfile.errorTypes?.join('、') || '暂无'}。`
      : ''

    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: `我理解你的问题是：“${content}”。目前接口暂不可用，建议先结合课程章节、资源详情和练习题进行学习。${profileTip}`,
      time: '刚刚'
    })
  }
}


const useSuggestion = (prompt: string) => {
  inputValue.value = prompt
}

const resetChat = () => {
  messages.value = fallbackMessages.slice(0, 1)
}

onMounted(() => {
  fetchSession()
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

.chat-panel,
.side-card {
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

.eyebrow {
  margin: 0 0 8px;
  color: #1769ff;
  font-weight: 700;
}

.chat-header h1 {
  margin: 0;
}

.chat-header p,
.side-card p {
  color: #667085;
  line-height: 1.7;
}

.chat-header button,
.chat-input button {
  border: none;
  border-radius: 12px;
  color: #ffffff;
  background: #1769ff;
  cursor: pointer;
}

.chat-header button {
  height: 40px;
  padding: 0 18px;
}

.message-list {
  flex: 1;
  min-height: 0;
  padding: 20px;
  overflow-y: auto;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 50%;
  color: #ffffff;
  background: #1769ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-item.user .avatar {
  background: #22c55e;
}

.bubble {
  max-width: min(680px, 80%);
  padding: 14px 16px;
  border-radius: 18px;
  background: #f7faff;
}

.message-item.user .bubble {
  color: #ffffff;
  background: #1769ff;
}

.bubble p {
  margin: 0;
  line-height: 1.7;
}

.bubble span {
  display: block;
  margin-top: 8px;
  color: #8a96a8;
  font-size: 12px;
}

.message-item.user .bubble span {
  color: rgba(255, 255, 255, 0.75);
}

.chat-input {
  padding: 16px;
  display: flex;
  gap: 12px;
  border-top: 1px solid #eef2f8;
}

.chat-input textarea {
  flex: 1;
  min-height: 46px;
  max-height: 120px;
  padding: 12px;
  border: 1px solid #dbe4f3;
  border-radius: 14px;
  outline: none;
  resize: vertical;
}

.chat-input button {
  width: 88px;
}

.side-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-card {
  padding: 18px;
  border-radius: 20px;
}

.side-card h3 {
  margin: 0 0 14px;
}

.suggestion-item {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 14px;
  color: #1769ff;
  background: #eef5ff;
  cursor: pointer;
}

.suggestion-item:hover {
  background: #dfeeff;
}

.ability-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ability-list span {
  padding: 7px 12px;
  border-radius: 999px;
  color: #1769ff;
  background: #eef5ff;
  font-size: 13px;
}

.state-card {
  padding: 40px;
  text-align: center;
  color: #75849a;
}

@media (max-width: 980px) {
  .tutor-page {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    min-height: 640px;
  }
}

@media (max-width: 560px) {
  .tutor-page {
    padding: 12px;
  }

  .chat-header,
  .chat-input {
    flex-direction: column;
  }

  .chat-header button,
  .chat-input button {
    width: 100%;
    height: 40px;
  }

  .bubble {
    max-width: 82%;
  }
}
</style>