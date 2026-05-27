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
import { ref } from 'vue'
import { sendTutorMessage } from '@/api/tutor'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const inputValue = ref('')
const messages = ref<{ role: string; content: string }[]>([])
const sessionId = ref('session_' + Date.now())

const sendMessage = async () => {
  const content = inputValue.value.trim()
  if (!content || loading.value) return

  messages.value.push({ role: 'user', content })
  inputValue.value = ''
  loading.value = true

  try {
    const result = await sendTutorMessage(content, sessionId.value)
    const answer = result?.answer || result?.finalAnswer || result?.data?.answer || ''
    if (answer) {
      messages.value.push({ role: 'assistant', content: answer })
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '请求失败，请确认后端和AI引擎是否运行')
  } finally {
    loading.value = false
  }
}

const resetChat = () => {
  messages.value = []
  sessionId.value = 'session_' + Date.now()
}
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