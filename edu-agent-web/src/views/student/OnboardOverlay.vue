<template>
  <div class="onboard-overlay">
    <div class="onboard-header">
      <h2>🎓 欢迎加入学习平台</h2>
      <p v-if="loading && messages.length === 0">正在为你准备...</p>
      <p v-else-if="stage === 'generating'">正在为你生成学习路径...</p>
      <p v-else-if="stage === 'done'">已就绪！</p>
    </div>

    <div class="onboard-chat" ref="chatBox">
      <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        <div class="msg-avatar">{{ msg.role === 'assistant' ? '🤖' : '👤' }}</div>
        <div class="msg-bubble">{{ msg.content }}</div>
      </div>
      <div v-if="loading && messages.length > 0" class="msg assistant">
        <div class="msg-avatar">🤖</div>
        <div class="msg-bubble typing">对方正在输入...</div>
      </div>
    </div>

    <div class="onboard-input" v-if="stage !== 'generating' && stage !== 'done' && stage !== 'init'">
      <el-input
        v-model="input"
        placeholder="输入你的回复..."
        @keyup.enter="send"
        :disabled="loading"
        size="large"
      >
        <template #append>
          <el-button @click="send" :loading="loading" :disabled="!input.trim()" type="primary">发送</el-button>
        </template>
      </el-input>
    </div>

    <div class="onboard-footer" v-if="stage === 'done'">
      <el-button type="success" size="large" @click="finish" :loading="finishing">
        进入学习平台
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import axios from 'axios'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const aiClient = axios.create({ baseURL: '/ai', timeout: 60000 })

async function callAI(message: string, sessionId: string, profile: any) {
  const res = await aiClient.post('/chat', {
    user_input: message,
    student_id: String(userStore.userInfo?.id || ''),
    session_id: sessionId,
    profile: profile
  })
  return res.data
}

const userStore = useUserStore()
const emit = defineEmits(['done'])

const input = ref('')
const loading = ref(false)
const finishing = ref(false)
const stage = ref('init')
const messages = ref<{ role: string; content: string }[]>([])
const chatBox = ref<HTMLElement>()
const sessionId = ref('onboard_' + Date.now())
const onboardProfile = ref<any>({})

const scrollToBottom = async () => {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

const send = async () => {
  const msg = input.value.trim()
  if (!msg || loading.value) return

  messages.value.push({ role: 'user', content: msg })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const result = await callAI(msg, sessionId.value, onboardProfile.value)
    const answer = result?.final_answer || ''
    if (result?.profile) {
      onboardProfile.value = { ...onboardProfile.value, ...result.profile }
    }
    if (answer) {
      messages.value.push({ role: 'assistant', content: answer })
      if (answer.includes('画像已完善') || answer.includes('画像采集完成') || answer.includes('正在为你生成')) {
        stage.value = 'generating'
        try {
          await request.post('/auth/onboard-done')
          localStorage.setItem('tutor_init_done', '1')
          // 保存资源路径、画像字段、应用建议
          if (result.resourceDir) localStorage.setItem('resource_dir', result.resourceDir)
          if (result?.profile?.topic) localStorage.setItem('userTopic', result.profile.topic)
          if (result?.profile?.course) localStorage.setItem('userCourse', result.profile.course)
          if (result.suggestions?.length) localStorage.setItem('onboard_suggestions', JSON.stringify(result.suggestions))
          await new Promise(r => setTimeout(r, 1500))
        } catch {}
        stage.value = 'done'
      }
    }
  } catch {
    messages.value.push({ role: 'assistant', content: '网络出问题了，请稍后再试' })
  }
  loading.value = false
  await scrollToBottom()
}

const finish = async () => {
  finishing.value = true
  // 将 AI 收集的画像保存到 Java 后端 MySQL
  try {
    await request.post('/profile', { ...onboardProfile.value, studentId: userStore.userInfo?.id })
  } catch {}
  // 通知后端引导完成
  try {
    await request.post('/auth/onboard-done')
  } catch {}
  emit('done')
}

// 挂载时自动发"开始"信号 → AI 引导智能体返回欢迎语
onMounted(async () => {
  loading.value = true
  await scrollToBottom()

  try {
    const result = await callAI('开始', sessionId.value, onboardProfile.value)
    const answer = result?.final_answer || ''
    if (result?.profile) {
      onboardProfile.value = { ...onboardProfile.value, ...result.profile }
    }
    if (answer) {
      messages.value.push({ role: 'assistant', content: answer })
    }
  } catch {
    messages.value.push({ role: 'assistant', content: '网络出问题了，请稍后再试' })
  }

  loading.value = false
  stage.value = 'collecting'
  await scrollToBottom()
})
</script>

<style scoped>
.onboard-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  z-index: 9999; background: linear-gradient(145deg, #667eea 0%, #764ba2 100%);
  display: flex; flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
.onboard-header {
  text-align: center; padding: 24px 20px 12px; color: #fff;
  flex-shrink: 0;
}
.onboard-header h2 { margin: 0; font-size: 26px; font-weight: 700; letter-spacing: -0.5px; }
.onboard-header p { margin: 6px 0 0; font-size: 13px; opacity: 0.8; }

.onboard-chat {
  flex: 1; overflow-y: auto; padding: 20px 24px;
  max-width: 700px; width: 100%; margin: 0 auto;
  display: flex; flex-direction: column; gap: 12px;
}
.msg { display: flex; gap: 10px; align-items: flex-start; animation: fadeUp .25s ease; }
.msg.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; flex-shrink: 0;
  background: rgba(255,255,255,.15);
  backdrop-filter: blur(4px);
}
.msg-bubble {
  max-width: 70%; padding: 12px 18px; border-radius: 16px;
  font-size: 14.5px; line-height: 1.7; white-space: pre-wrap;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.msg.assistant .msg-bubble {
  background: rgba(255,255,255,.95); color: #1d1d1f;
  border-bottom-left-radius: 4px;
}
.msg.user .msg-bubble {
  background: #fff; color: #1d1d1f;
  border-bottom-right-radius: 4px;
}
.typing { color: #999; font-style: italic; font-size: 13px; }

.onboard-input {
  padding: 16px 24px 24px; max-width: 700px; width: 100%;
  margin: 0 auto; flex-shrink: 0;
}
.onboard-input :deep(.el-input__wrapper) {
  background: rgba(255,255,255,.95);
  border-radius: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
}
.onboard-input :deep(.el-input-group__append) { background: transparent; border: none; }
.onboard-input :deep(.el-button--primary) { border-radius: 20px; }

.onboard-footer { text-align: center; padding: 0 16px 24px; flex-shrink: 0; }
.onboard-footer :deep(.el-button--success) {
  border-radius: 24px; padding: 12px 40px; font-size: 16px;
  background: #34c759; border: none; font-weight: 600;
}
.onboard-footer :deep(.el-button--success:hover) { opacity: .9; }

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
