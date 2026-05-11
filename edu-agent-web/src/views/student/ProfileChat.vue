<template>
  <div class="profile-chat-container">
    <el-row :gutter="20">
      <!-- 左侧对话区域 -->
      <el-col :span="16">
        <el-card class="chat-card" shadow="never">
          <template #header>
            <div class="chat-header">
              <div class="avatar">
                <el-avatar :size="40" src="https://cube.elemecdn.com/0/88/03b6d3b6a6f4e6b8b6c0e6b4d6b6e6b6.png" />
                <span class="ai-name">智能画像助手</span>
              </div>
              <div class="reset-btn">
                <el-button type="text" @click="resetChat" :disabled="messages.length <= 1">
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
            <div v-if="isTyping" class="message ai-message">
              <div class="message-avatar"><el-avatar :size="32" :src="aiAvatar" /></div>
              <div class="message-content"><div class="typing-indicator"><span></span><span></span><span></span></div></div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="输入你的回答... 或使用下方快捷选项"
              @keyup.enter.prevent="sendMessage"
              :disabled="isTyping || conversationEnded"
            />
            <div class="input-actions">
              <el-button type="primary" @click="sendMessage" :loading="isTyping" :disabled="conversationEnded">
                发送
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧画像预览 -->
      <el-col :span="8">
        <el-card class="preview-card" shadow="never">
          <template #header>
            <span>当前画像预览</span>
            <el-button type="text" @click="viewFullProfile" v-if="profileComplete" style="float: right">
              查看完整画像
            </el-button>
          </template>
          <div class="profile-preview">
            <div v-if="!profileComplete" class="incomplete-tip">
              <el-icon><InfoFilled /></el-icon> 回答几个问题，即可生成专属学习画像
            </div>
            <div v-else class="profile-summary">
              <div class="preview-item"><span class="label">学习目标：</span><span class="value">{{ profile.goal || '未填写' }}</span></div>
              <div class="preview-item"><span class="label">优势学科：</span><el-tag size="small" type="success" v-for="sub in profile.strengths" :key="sub">{{ sub }}</el-tag></div>
              <div class="preview-item"><span class="label">待提升学科：</span><el-tag size="small" type="danger" v-for="sub in profile.weaknesses" :key="sub">{{ sub }}</el-tag></div>
              <div class="preview-item"><span class="label">学习风格：</span><span class="value">{{ profile.style || '未填写' }}</span></div>
              <div class="preview-item"><span class="label">每日学习时长：</span><span class="value">{{ profile.dailyHours || '?' }} 小时</span></div>
            </div>
            <el-divider />
            <div class="progress">
              <span>画像完整度：{{ Math.floor(completeness * 100) }}%</span>
              <el-progress :percentage="completeness * 100" :stroke-width="8" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()
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
const isTyping = ref(false)
const conversationEnded = ref(false)
const chatContainer = ref<HTMLElement>()

const profile = reactive({
  goal: '',
  strengths: [] as string[],
  weaknesses: [] as string[],
  style: '',
  dailyHours: null as number | null,
})

const buildStudentProfile = () => {
  const hasWeaknesses = profile.weaknesses.length > 0
  const hasStrengths = profile.strengths.length > 0

  let overallType = '稳定提升型'
  let knowledgeBase = '基础一般'
  let masteryLevel = '初步理解'
  let learningAutonomy = '提醒辅助型'

  if (hasWeaknesses && !hasStrengths) {
    overallType = '基础补齐型'
    knowledgeBase = '基础薄弱'
    masteryLevel = '初步理解'
  }

  if (hasStrengths && !hasWeaknesses) {
    overallType = '进阶拓展型'
    knowledgeBase = '基础扎实'
    masteryLevel = '熟练应用'
  }

  if (profile.dailyHours && profile.dailyHours >= 2) {
    learningAutonomy = '半自主型'
  }

  if (profile.dailyHours && profile.dailyHours >= 4) {
    learningAutonomy = '高度自主型'
  }

  const errorTypes = hasWeaknesses
    ? ['概念混淆', '迁移困难']
    : ['暂无明显易错点']

  const cognitiveStyleMap: Record<string, string> = {
    '视觉型': '图像视觉型',
    '阅读型': '理论理解型',
    '实践型': '任务实践型',
    '社交型': '互动问答型',
  }

  return {
    studentId: 1,
    overallType,
    knowledgeBase,
    learningGoal: profile.goal || '目标待明确',
    masteryLevel,
    cognitiveStyle: cognitiveStyleMap[profile.style] || profile.style || '例子驱动型',
    errorTypes,
    learningAutonomy,
    suggestions: [
      `当前综合类型为：${overallType}`,
      `建议围绕「${profile.goal || '当前课程'}」制定阶段性学习计划`,
      hasWeaknesses
        ? `建议优先提升：${profile.weaknesses.join('、')}`
        : '建议继续保持当前优势，并尝试更高难度任务',
      '每个知识点建议先理解概念，再通过练习巩固',
      '建议定期复盘错题，更新个人学习画像'
    ]
  }
}

let step = 0
const steps = [
  { question: '你好！我是你的学习画像助手。为了给你推荐最合适的学习资源，能先告诉我你近期的学习目标吗？（例如：通过期末考试、考研、找工作）', type: 'text' },
  { question: '你比较擅长哪些学科或技能？（可多选）', type: 'options', options: [
    { label: '数学', value: '数学' }, { label: '英语', value: '英语' }, { label: '编程', value: '编程' },
    { label: '物理', value: '物理' }, { label: '写作', value: '写作' }, { label: '设计', value: '设计' }
  ] },
  { question: '哪些学科让你感到比较吃力？想重点提升什么？', type: 'options', options: [
    { label: '数学', value: '数学' }, { label: '英语', value: '英语' }, { label: '编程', value: '编程' },
    { label: '物理', value: '物理' }, { label: '写作', value: '写作' }
  ] },
  { question: '你更喜欢哪种学习方式？（单选）', type: 'options', options: [
    { label: '看视频课程', value: '视觉型' }, { label: '阅读文档', value: '阅读型' },
    { label: '动手实践', value: '实践型' }, { label: '与他人讨论', value: '社交型' }
  ] },
  { question: '你每天大概能投入多少小时学习？', type: 'text' },
  { question: '感谢你的回答！你的学习画像已经生成。你可以随时在“学习画像概览”中查看详细分析。', type: 'text', isEnd: true }
]

const addMessage = async (msg: Message) => {
  messages.value.push(msg)
  await nextTick()
  if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight
}

const aiReply = async (stepIndex: number) => {
  isTyping.value = true
  const stepData = steps[stepIndex]
  await new Promise(resolve => setTimeout(resolve, 800))
  isTyping.value = false
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  if (stepData.options) {
    addMessage({ role: 'ai', type: 'options', options: stepData.options, time: now })
  } else {
    addMessage({ role: 'ai', type: 'text', content: stepData.question, time: now })
  }
  if (stepData.isEnd) conversationEnded.value = true
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  addMessage({ role: 'user', type: 'text', content: text, time: now })

  switch (step) {
    case 0: profile.goal = text; break
    case 1: profile.strengths = text.split(/[ ,]+/).filter(s => s); break
    case 2: profile.weaknesses = text.split(/[ ,]+/).filter(s => s); break
    case 3: profile.style = text; break
    case 4: profile.dailyHours = parseFloat(text) || null; break
    default: break
  }
  step++
  if (step < steps.length) await aiReply(step)
  else {
  conversationEnded.value = true

  const studentProfile = buildStudentProfile()
  localStorage.setItem('studentProfile', JSON.stringify(studentProfile))

  ElMessage.success('画像构建完成！即将跳转到概览页')
  setTimeout(() => router.push('/student/profile/overview'), 1500)
}
}

const sendQuickReply = (opt: { label: string; value: any }) => {
  inputText.value = opt.value
  sendMessage()
}

const resetChat = () => {
  messages.value = []
  step = 0
  conversationEnded.value = false
  profile.goal = ''
  profile.strengths = []
  profile.weaknesses = []
  profile.style = ''
  profile.dailyHours = null
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  addMessage({ role: 'ai', type: 'text', content: steps[0].question, time: now })
}

const completeness = ref(0)
const updateCompleteness = () => {
  let count = 0
  if (profile.goal) count++
  if (profile.strengths.length) count++
  if (profile.weaknesses.length) count++
  if (profile.style) count++
  if (profile.dailyHours) count++
  completeness.value = count / 5
}

watch(() => [profile.goal, profile.strengths, profile.weaknesses, profile.style, profile.dailyHours], updateCompleteness, { deep: true })
const profileComplete = computed(() => completeness.value === 1)

const viewFullProfile = () => {
  const studentProfile = buildStudentProfile()
  localStorage.setItem('studentProfile', JSON.stringify(studentProfile))
  router.push('/student/profile/overview')
}


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
.message-bubble { padding: 10px 14px; border-radius: 18px; background-color: #fff; box-shadow: 0 1px 2px rgba(0,0,0,0.05); display: inline-block; }
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
.incomplete-tip { text-align: center; color: #909399; padding: 40px 0; }
.preview-item { margin-bottom: 12px; }
.label { font-weight: 600; width: 90px; display: inline-block; }
.el-tag { margin-right: 6px; margin-bottom: 4px; }
.progress { margin-top: 12px; }
</style>