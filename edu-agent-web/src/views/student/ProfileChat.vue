<template>
  <div class="profile-chat-container">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="chat-card" shadow="never">
          <template #header>
            <div class="chat-header">
              <div class="avatar">
                <el-avatar :size="40" src="https://cube.elemecdn.com/0/88/03b6d3b6a6f4e6b8b6c0e6b4d6b6e6b6.png" />
                <span class="ai-name">Java学习画像助手</span>
              </div>
              <div class="reset-btn">
                <el-button type="text" @click="resetChat" :disabled="aiProcessing">
                  <el-icon><Refresh /></el-icon>重新开始
                </el-button>
              </div>
            </div>
          </template>

          <div class="chat-messages" ref="chatContainer">
            <div
              v-for="(msg, idx) in messages"
              :key="idx"
              :class="['message', msg.role === 'user' ? 'user-message' : 'ai-message']"
            >
              <div class="message-avatar">
                <el-avatar :size="32" :src="msg.role === 'user' ? userAvatar : aiAvatar" />
              </div>
              <div class="message-content">
                <div class="message-bubble">
                  <div v-if="msg.type === 'text'">{{ msg.content }}</div>
                  <div v-else-if="msg.type === 'options'" class="options-group">
                    <div v-for="opt in msg.options" :key="opt.value" class="option-item" @click="sendQuickReply(opt)">
                      {{ opt.label }}
                    </div>
                  </div>
                </div>
                <div class="message-time">{{ msg.time }}</div>
              </div>
            </div>
            <div v-if="aiProcessing" class="message ai-message">
              <div class="message-avatar"><el-avatar :size="32" :src="aiAvatar" /></div>
              <div class="message-content">
                <div class="typing-indicator"><span></span><span></span><span></span></div>
              </div>
            </div>
          </div>

          <div class="chat-input" v-if="!aiProcessing && !conversationEnded">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="输入你的回答..."
              @keyup.enter.prevent="sendMessage"
            />
            <div class="input-actions">
              <el-button type="primary" @click="sendMessage">发送</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="preview-card" shadow="never">
          <template #header>
            <span>六维画像预览</span>
            <el-tag v-if="profile.overall_type" size="small" :type="typeTagType" style="float: right; margin-right: 8px;">
              {{ profile.overall_type }}
            </el-tag>
            <el-button type="text" @click="viewFullProfile" v-if="conversationEnded" style="float: right">
              查看完整画像
            </el-button>
          </template>
          <div class="profile-preview">
            <div class="dimension-list">
              <div class="dim-item" v-for="dim in dimensions" :key="dim.key">
                <span class="dim-label">{{ dim.label }}</span>
                <span class="dim-value" :class="{ filled: dim.filled }">{{ dim.summary }}</span>
              </div>
            </div>
            <el-divider />
            <div class="progress">
              <span>画像完整度：{{ completedDims }}/6</span>
              <el-progress :percentage="(completedDims / 6) * 100" :stroke-width="8" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { buildProfile, saveProfile } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const userAvatar = 'https://cube.elemecdn.com/1/2e/3b6d3b6a6f4e6b8b6c0e6b4d6b6e6b6.png'
const aiAvatar = 'https://cube.elemecdn.com/0/88/03b6d3b6a6f4e6b8b6c0e6b4d6b6e6b6.png'

interface Message {
  role: 'user' | 'ai'
  type: 'text' | 'options'
  content?: string
  options?: { label: string; value: any }[]
  time: string
}

const messages = ref<Message[]>([])
const inputText = ref('')
const aiProcessing = ref(false)
const conversationEnded = ref(false)
const chatContainer = ref<HTMLElement>()

const profile = reactive({
  learning_goal: '',
  knowledge_base: [] as string[],
  current_mastery: '',
  cognitive_style: '',
  mistake_patterns: [] as string[],
  learning_behavior: '',
  daily_hours: 0,
  overall_type: '',
})

const dimensions = computed(() => [
  { key: 'learning_goal', label: '学习目标', filled: !!profile.learning_goal, summary: profile.learning_goal || '待采集' },
  { key: 'knowledge_base', label: '知识基础', filled: profile.knowledge_base.length > 0, summary: profile.knowledge_base.length ? profile.knowledge_base.join('、') : '待采集' },
  { key: 'current_mastery', label: '当前掌握度', filled: !!profile.current_mastery, summary: profile.current_mastery || '待采集' },
  { key: 'cognitive_style', label: '认知风格', filled: !!profile.cognitive_style, summary: profile.cognitive_style || '待采集' },
  { key: 'mistake_patterns', label: '易错点类型', filled: profile.mistake_patterns.length > 0, summary: profile.mistake_patterns.length ? profile.mistake_patterns.join('、') : '待采集' },
  { key: 'learning_behavior', label: '学习行为', filled: !!profile.learning_behavior, summary: profile.learning_behavior || '待采集' },
])

const completedDims = computed(() => dimensions.value.filter(d => d.filled).length)
const typeTagType = computed(() => {
  if (profile.overall_type === '进阶拓展型') return 'success'
  if (profile.overall_type === '稳定提升型') return ''
  return 'warning'
})

let step = 0
const steps = [
  {
    dimension: '学习目标',
    question: '你好！我是你的Java学习画像助手，会通过6个问题为你构建专属画像。\n\n首先，你的Java学习目标是什么？（例如：通过期末考试、掌握Spring Boot、准备校招面试）',
    type: 'text' as const,
  },
  {
    dimension: '知识基础',
    question: '你目前Java学到什么程度了？已掌握哪些知识点？（可多选）',
    type: 'options' as const,
    options: [
      { label: 'Java基础语法', value: 'Java基础语法' },
      { label: '面向对象', value: '面向对象' },
      { label: '集合框架', value: '集合框架' },
      { label: '多线程并发', value: '多线程并发' },
      { label: 'JVM虚拟机', value: 'JVM虚拟机' },
      { label: 'Spring/Spring Boot', value: 'Spring/Spring Boot' },
      { label: '数据库/MyBatis', value: '数据库/MyBatis' },
      { label: '网络编程', value: '网络编程' },
    ],
  },
  {
    dimension: '当前掌握度',
    question: '在上面这些知识点里，哪些你掌握得好，哪些还不太熟？简单描述一下你的掌握情况。（例如：基础语法比较熟，多线程和JVM只了解皮毛）',
    type: 'text' as const,
  },
  {
    dimension: '认知风格',
    question: '你更喜欢哪种学习方式？（单选）',
    type: 'options' as const,
    options: [
      { label: '看视频课程', value: '视觉型' },
      { label: '阅读文档/书籍', value: '阅读型' },
      { label: '动手写代码练习', value: '实践型' },
      { label: '和他人讨论交流', value: '社交型' },
    ],
  },
  {
    dimension: '易错点类型',
    question: '写Java代码时，你经常遇到哪些类型的错误或困难？（可多选）',
    type: 'options' as const,
    options: [
      { label: '空指针异常', value: '空指针异常' },
      { label: '类型转换错误', value: '类型转换错误' },
      { label: '并发/线程安全问题', value: '并发/线程安全问题' },
      { label: '逻辑设计错误', value: '逻辑设计错误' },
      { label: '边界条件遗漏', value: '边界条件遗漏' },
      { label: '语法/编译错误', value: '语法/编译错误' },
    ],
  },
  {
    dimension: '学习行为',
    question: '最后一个问题，你每天怎么安排Java学习？大概花多少小时？会主动找资料或做额外练习吗？（例如：每天课后学2小时，会刷LeetCode和看技术博客）',
    type: 'text' as const,
  },
]

const addMessage = async (msg: Message) => {
  messages.value.push(msg)
  await nextTick()
  if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight
}

const aiReply = async (stepIndex: number) => {
  await new Promise(resolve => setTimeout(resolve, 600))
  const stepData = steps[stepIndex]
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  if (stepData.options) {
    await addMessage({ role: 'ai', type: 'options', options: stepData.options, time: now })
  } else {
    await addMessage({ role: 'ai', type: 'text', content: stepData.question, time: now })
  }
}

const extractHours = (text: string): number => {
  const match = text.match(/(\d+(?:\.\d+)?)\s*(小?时|h|H|个?钟|个?小)/)
  return match ? parseFloat(match[1]) : 0
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  await addMessage({ role: 'user', type: 'text', content: text, time: now })

  switch (step) {
    case 0: profile.learning_goal = text; break
    case 1: profile.knowledge_base = text.split(/[ ,，、]+/).filter(s => s); break
    case 2: profile.current_mastery = text; break
    case 3: profile.cognitive_style = text; break
    case 4: profile.mistake_patterns = text.split(/[ ,，、]+/).filter(s => s); break
    case 5:
      profile.learning_behavior = text
      profile.daily_hours = extractHours(text)
      break
  }
  step++

  if (step < steps.length) {
    await aiReply(step)
  } else {
    await finishAndBuildProfile()
  }
}

const sendQuickReply = (opt: { label: string; value: any }) => {
  inputText.value = opt.value
  sendMessage()
}

const finishAndBuildProfile = async () => {
  conversationEnded.value = true
  aiProcessing.value = true
  await addMessage({
    role: 'ai', type: 'text',
    content: '信息收集完毕，正在调用AI分析你的六维学习画像，请稍候...',
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  })

  const studentId = userStore.userInfo?.username || 'student001'

  try {
    const result = await buildProfile({
      student_id: studentId,
      learning_goal: profile.learning_goal,
      knowledge_base: profile.knowledge_base,
      current_mastery: profile.current_mastery,
      cognitive_style: profile.cognitive_style,
      mistake_patterns: profile.mistake_patterns,
      learning_behavior: profile.learning_behavior,
      daily_hours: profile.daily_hours,
    })

    aiProcessing.value = false
    const aiProfile = result.profile

    if (aiProfile && Object.keys(aiProfile).length > 0) {
      profile.overall_type = aiProfile.overall_type || ''

      const lines = [
        aiProfile.overall_type ? `综合类型：${aiProfile.overall_type}` : '',
        aiProfile.knowledge_base ? `知识基础：${aiProfile.knowledge_base}` : '',
        aiProfile.current_mastery ? `当前掌握度：${aiProfile.current_mastery}` : '',
        aiProfile.learning_behavior ? `学习行为：${aiProfile.learning_behavior}` : '',
        aiProfile.profile_suggestions?.length ? `\n建议：${aiProfile.profile_suggestions.join('；')}` : '',
      ].filter(Boolean).join('\n')

      await addMessage({
        role: 'ai', type: 'text',
        content: `AI六维画像构建完成！\n\n${lines}\n\n正在保存到数据库...`,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      })

      try {
        await saveProfile(studentId, aiProfile)
        ElMessage.success('六维画像已保存到数据库')
      } catch {
        ElMessage.warning('画像已生成，但保存数据库失败')
      }

      setTimeout(() => router.push('/student/profile/overview'), 2500)
    }
  } catch (err: any) {
    aiProcessing.value = false
    await addMessage({
      role: 'ai', type: 'text',
      content: `AI画像构建失败：${err.message || '网络错误'}。请确保AI后端已启动后重试。`,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    })
    ElMessage.error('AI画像构建失败')
  }
}

const resetChat = () => {
  messages.value = []
  step = 0
  conversationEnded.value = false
  aiProcessing.value = false
  profile.learning_goal = ''
  profile.knowledge_base = []
  profile.current_mastery = ''
  profile.cognitive_style = ''
  profile.mistake_patterns = []
  profile.learning_behavior = ''
  profile.daily_hours = 0
  profile.overall_type = ''
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  addMessage({ role: 'ai', type: 'text', content: steps[0].question, time: now })
}

const viewFullProfile = () => router.push('/student/profile/overview')

onMounted(() => resetChat())
</script>

<style scoped>
.profile-chat-container { padding: 20px; background-color: #f5f7fa; min-height: 100vh; }
.chat-card, .preview-card { border-radius: 16px; }
.chat-header { display: flex; justify-content: space-between; align-items: center; }
.avatar { display: flex; align-items: center; gap: 12px; }
.ai-name { font-weight: 600; }
.chat-messages { height: 500px; overflow-y: auto; padding: 16px; background: #fafafa; border-radius: 12px; margin-bottom: 16px; }
.message { display: flex; margin-bottom: 20px; }
.message-avatar { margin-right: 12px; }
.user-message { justify-content: flex-end; }
.user-message .message-avatar { order: 2; margin-left: 12px; margin-right: 0; }
.message-content { max-width: 80%; }
.message-bubble { padding: 10px 14px; border-radius: 18px; background-color: #fff; box-shadow: 0 1px 2px rgba(0,0,0,0.05); display: inline-block; white-space: pre-wrap; }
.user-message .message-bubble { background-color: #409eff; color: white; }
.options-group { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; }
.option-item { background-color: #f0f2f5; padding: 6px 12px; border-radius: 20px; cursor: pointer; transition: all 0.2s; }
.option-item:hover { background-color: #e0e3e8; transform: scale(1.02); }
.message-time { font-size: 11px; color: #aaa; margin-top: 4px; }
.typing-indicator { display: flex; gap: 4px; padding: 8px 12px; background: #f0f0f0; border-radius: 18px; width: 48px; }
.typing-indicator span { width: 8px; height: 8px; background: #909399; border-radius: 50%; animation: typing 1.4s infinite ease-in-out; }
.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing { 0%,60%,100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-6px); opacity: 1; } }
.chat-input { border-top: 1px solid #eee; padding-top: 16px; }
.input-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
.profile-preview { min-height: 300px; }
.dimension-list { display: flex; flex-direction: column; gap: 10px; }
.dim-item { display: flex; justify-content: space-between; align-items: baseline; }
.dim-label { font-weight: 600; font-size: 13px; color: #303133; white-space: nowrap; }
.dim-value { font-size: 12px; color: #c0c4cc; text-align: right; max-width: 60%; }
.dim-value.filled { color: #409eff; }
.progress { margin-top: 12px; }
</style>
