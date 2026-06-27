<template>
  <div class="onboard-overlay">
    <div class="onboard-header">
      <h2>🎓 欢迎加入学习平台</h2>
      <p v-if="stage === 'init'">正在为你准备...</p>
      <p v-else-if="stage === 'collecting'">跟我说说你的学习情况吧 😊</p>
      <p v-else-if="stage === 'generating'">正在为你生成个性化学习方案...</p>
      <p v-else-if="stage === 'done'">✅ 全部就绪！</p>
    </div>

    <!-- 聊天区 -->
    <div class="onboard-chat" ref="chatBox">
      <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        <div class="msg-avatar">{{ msg.role === 'assistant' ? '🤖' : '👤' }}</div>
        <div class="msg-bubble">{{ msg.content }}</div>
      </div>

      <!-- 生成阶段进度列表（每个步骤真实等待接口返回） -->
      <div v-if="stage === 'generating'" class="gen-progress">
        <div v-for="(step, i) in genSteps" :key="i" class="gen-step" :class="step.status">
          <span class="gen-step-icon">
            <span v-if="step.status === 'done'">✅</span>
            <span v-else-if="step.status === 'active'">⏳</span>
            <span v-else-if="step.status === 'error'">❌</span>
            <span v-else>⭕</span>
          </span>
          <span class="gen-step-label">{{ step.label }}</span>
          <span v-if="step.status === 'done'" class="gen-step-time">{{ step.time }}</span>
        </div>
      </div>

      <div v-if="loading && stage === 'collecting'" class="msg assistant">
        <div class="msg-avatar">🤖</div>
        <div class="msg-bubble typing">对方正在输入...</div>
      </div>
    </div>

    <!-- 输入框 -->
    <div class="onboard-input" v-if="stage === 'collecting'">
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

    <!-- 完成按钮 -->
    <div class="onboard-footer" v-if="stage === 'done'">
      <el-button type="success" size="large" @click="finish" :loading="finishing">
        进入学习平台
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, reactive } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const emit = defineEmits(['done'])

const input = ref('')
const loading = ref(false)
const finishing = ref(false)
const stage = ref('init')           // init | collecting | generating | done
const messages = ref<{ role: string; content: string }[]>([])
const chatBox = ref<HTMLElement>()
const sessionId = ref('onboard_' + Date.now())
const onboardProfile = ref<any>({})

const genSteps = reactive([
  { label: '保存学习画像',        status: 'pending', time: '' },
  { label: '生成学习路径',        status: 'pending', time: '' },
  { label: '生成思维导图',        status: 'pending', time: '' },
  { label: '生成练习题',          status: 'pending', time: '' },
  { label: '生成拓展阅读',        status: 'pending', time: '' },
  { label: '生成代码案例',        status: 'pending', time: '' }
])

const scrollToBottom = async () => {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

function sleep(ms: number) {
  return new Promise(r => setTimeout(r, ms))
}

function formatTime(ms: number) {
  return (ms / 1000).toFixed(1) + 's'
}

function timestamp() {
  return Date.now()
}

// 聊天 — 全走 Java 后端 /api/onboard/chat
async function onboardChat(message: string, sessionId: string, profile: any) {
  const res = await request.post('/onboard/chat', {
    message,
    session_id: sessionId,
    profile: profile
  })
  return res
}

const send = async () => {
  const msg = input.value.trim()
  if (!msg || loading.value) return

  messages.value.push({ role: 'user', content: msg })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const result = await onboardChat(msg, sessionId.value, onboardProfile.value)
    const answer = result?.final_answer || ''
    if (result?.profile) {
      onboardProfile.value = { ...onboardProfile.value, ...result.profile }
    }
    if (answer) {
      messages.value.push({ role: 'assistant', content: answer })
      if (result?.profile_complete === true) {
        await startGeneration()
      }
    }
  } catch {
    messages.value.push({ role: 'assistant', content: '网络出问题了，请稍后再试' })
  }
  loading.value = false
  await scrollToBottom()
}

// 生成阶段 — 逐步调 6 个独立接口，每步真实等待
async function startGeneration() {
  stage.value = 'generating'
  loading.value = true
  const tStart = timestamp()
  const profile = onboardProfile.value
  const course = profile.course || ''
  const topic = profile.topic || course || '编程学习'
  const difficulty = { '零基础': 'easy', '了解概念': 'easy', '有一定基础': 'medium', '熟练': 'hard' }[profile.knowledge_base] || 'medium'

  // ---- Step 1: 保存画像到 student_profiles ----
  genSteps[0].status = 'active'
  await scrollToBottom()
  try {
    await request.post('/profile/save', {
      course: profile.course,
      topic: profile.topic,
      learning_goal: profile.learning_goal,
      knowledge_base: profile.knowledge_base,
      pace: profile.pace,
      cognitive_style: profile.cognitive_style,
      weaknesses: profile.weaknesses || [],
      resource_preference: profile.resource_preference || [],
      mistake_patterns: profile.mistake_patterns || [],
      overall_type: profile.overall_type,
      // Python AI 把六维分数放在 profile 顶层而非 profile.dimensions 中
      // 手动组装 dimensions 对象，确保数据存入数据库
      dimensions: profile.dimensions || {
        knowledge_mastery: profile.knowledge_mastery,
        learning_goal_clarity: profile.learning_goal_clarity,
        cognitive_adaptation: profile.cognitive_adaptation,
        mistake_avoidance: profile.mistake_avoidance,
        learning_autonomy: profile.learning_autonomy,
        overall_level: profile.overall_level
      }
    })
    genSteps[0].status = 'done'
  } catch {
    genSteps[0].status = 'error'
  }
  genSteps[0].time = formatTime(timestamp() - tStart)
  await sleep(300)

  // ---- Step 2: 生成学习路径 → learning_paths 表 ----
  genSteps[1].status = 'active'
  await scrollToBottom()
  try {
    await request.post('/student/learning-path/generate')
    genSteps[1].status = 'done'
  } catch {
    genSteps[1].status = 'error'
  }
  genSteps[1].time = formatTime(timestamp() - tStart)
  await sleep(300)

  // ---- Step 3-6: 生成资源 → resources 表 ----
  const resourceTypes = [
    { type: 'mindmap', label: '思维导图', stepIdx: 2 },
    { type: 'quiz',    label: '练习题',   stepIdx: 3 },
    { type: 'reading', label: '拓展阅读', stepIdx: 4 },
    { type: 'code',    label: '代码案例', stepIdx: 5 }
  ]

  for (const rt of resourceTypes) {
    genSteps[rt.stepIdx].status = 'active'
    await scrollToBottom()
    try {
      await request.post('/resources/generate', {
        chapterName: course,
        topic: topic,
        type: rt.type,
        difficulty: difficulty,
        source: 'onboarding'
      })
      genSteps[rt.stepIdx].status = 'done'
    } catch {
      genSteps[rt.stepIdx].status = 'error'
    }
    genSteps[rt.stepIdx].time = formatTime(timestamp() - tStart)
    await sleep(300)
  }

  // ---- 标记引导完成 ----
  try {
    await request.post('/auth/onboard-done')
  } catch {
    console.warn('[Onboard] /auth/onboard-done 失败，本地标记仍然写入')
  }
  // 无论后端是否成功，都写入本地标记，防止用户重复走引导
  localStorage.setItem('tutor_init_done', '1')

  loading.value = false
  stage.value = 'done'
  messages.value.push({
    role: 'assistant',
    content: '🎉 全部完成！你的个性化学习方案已就绪，点击下方按钮开始学习吧！'
  })
  await scrollToBottom()
}

const finish = async () => {
  finishing.value = true
  emit('done')
}

// 挂载时自动发"开始"
onMounted(async () => {
  loading.value = true
  await scrollToBottom()

  try {
    const result = await onboardChat('开始', sessionId.value, onboardProfile.value)
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

/* 生成进度列表 */
.gen-progress {
  display: flex; flex-direction: column; gap: 8px;
  padding: 16px 20px; margin: 4px 0;
  background: rgba(255,255,255,.08);
  border-radius: 16px;
  backdrop-filter: blur(4px);
}
.gen-step {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 0; font-size: 14px; color: rgba(255,255,255,.7);
  transition: all .3s ease;
}
.gen-step.active { color: #fff; font-weight: 600; }
.gen-step.done { color: rgba(255,255,255,.9); }
.gen-step.error { color: #f56c6c; }
.gen-step-icon { width: 24px; text-align: center; font-size: 16px; }
.gen-step-label { flex: 1; }
.gen-step-time { font-size: 12px; opacity: .6; }

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
