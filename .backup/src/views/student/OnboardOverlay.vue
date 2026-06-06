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
import { sendTutorMessage } from '@/api/tutor'
import request from '@/utils/request'

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
    const result = await sendTutorMessage(msg, sessionId.value, true, onboardProfile.value)
    const answer = result?.answer || result?.finalAnswer || ''
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
  try {
    const suggestionsStr = localStorage.getItem('onboard_suggestions')
    const body: any = {}
    if (suggestionsStr) {
      try { body.suggestions = JSON.parse(suggestionsStr) } catch {}
    }
    await request.post('/learning/init', body)
  } catch {}
  emit('done')
}

// 挂载时自动发"开始"信号 → 后端 onboarding_agent Phase 1 返回欢迎语
onMounted(async () => {
  loading.value = true
  await scrollToBottom()

  try {
    const result = await sendTutorMessage('开始', sessionId.value, true, onboardProfile.value)
    const answer = result?.answer || result?.finalAnswer || ''
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
  z-index: 9999; background: linear-gradient(135deg, var(--accent) 0%, #3a2a99 100%);
  display: flex; flex-direction: column;
}
.onboard-header {
  text-align: center; padding: 12px; color: var(--on-dark);
}
.onboard-header h2 { margin: 0; font-size: 24px; }
.onboard-header p { margin: 4px 0 0; font-size: 12px; opacity: 0.85; }

.onboard-chat {
  flex: 1; overflow-y: auto; padding: 16px 24px;
  max-width: 116px; width: 100%; margin: 0 auto;
}
.msg { display: flex; gap: 8px; margin-bottom: 16px; }
.msg.user { flex-direction: row-reverse; }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,.2); display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.msg-bubble {
  max-width: 75%; padding: 12px 16px; border-radius: 12px;
  font-size: 14px; line-height: 1.7; white-space: pre-wrap;
}
.msg.assistant .msg-bubble { background: var(--bg); color: var(--text-h); border-bottom-left-radius: 4px; }
.msg.user .msg-bubble { background: var(--accent); color: var(--on-dark); border-bottom-right-radius: 4px; }
.typing { color: var(--text); font-style: italic; }

.onboard-input {
  padding: 16px 24px; max-width: 116px; width: 100%; margin: 0 auto 16px;
}
.onboard-footer {
  text-align: center; padding: 16px;
}
</style>
