<template>
  <div class="generate-page">
    <div class="top-bar">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2>{{ typeLabel }} · AI 生成</h2>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-area">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>AI 正在为你生成 {{ typeLabel }}...</p>
    </div>

    <!-- 内容区 -->
    <div v-else-if="content || questions.length" class="content-area">
      <!-- 思维导图：mind-elixir 渲染（直接接受 JSON） -->
      <div v-if="resourceType === 'mindmap'" class="mm-wrapper">
        <div ref="mmEl" class="mindmap-box map-container"></div>
        <div class="mm-toolbar">
          <el-button size="small" circle @click="mmZoomOut" title="缩小">−</el-button>
          <el-button size="small" @click="mmZoomReset" title="适应屏幕">⊡</el-button>
          <el-button size="small" circle @click="mmZoomIn" title="放大">+</el-button>
          <el-divider direction="vertical" />
          <el-button size="small" @click="mmDownload" title="下载图片">⬇ 下载</el-button>
        </div>
      </div>

      <!-- 练习题目 -->
      <div v-else-if="resourceType === 'quiz'" class="quiz-box">
        <div v-for="(q, i) in questions" :key="i" class="quiz-item">
          <div class="q-header">
            <span class="q-num">{{ i + 1 }}</span>
            <span>{{ q.question || q.title || '题目' }}</span>
          </div>
          <div v-if="q.options?.length" class="q-options">
            <div v-for="(opt, j) in q.options" :key="j" :class="['opt', { correct: q.showAnswer && j === q.answer }]">
              {{ opt }}
            </div>
          </div>
          <el-button v-if="!q.showAnswer" text type="primary" size="small" @click="q.showAnswer = true">
            显示答案
          </el-button>
          <div v-if="q.showAnswer" class="q-answer">
            答案：{{ q.options ? q.options[q.answer] : q.answer || q.explanation }}
          </div>
        </div>
      </div>

      <!-- 拓展阅读 -->
      <div v-else-if="resourceType === 'reading'" class="reading-box" v-html="content"></div>

      <!-- 代码案例 -->
      <div v-else class="code-box">
        <pre><code>{{ content }}</code></pre>
      </div>

      <!-- ===== 反馈区：记录画像 + 换个方向重新生成 ===== -->
      <div class="feedback-bar">
        <span>这个内容对你有帮助吗？</span>
        <el-button :type="liked ? 'primary' : 'default'" size="small" circle @click="like">👍</el-button>
        <el-button :type="disliked ? 'danger' : 'default'" size="small" circle @click="dislike">👎</el-button>
        <el-divider direction="vertical" />
        <span>评价：</span>
        <!-- 用按钮代替 radio，确保重复点击同一选项也能触发 -->
        <div class="diff-btns">
          <el-button
            :type="difficulty === 'easy' ? 'warning' : 'default'"
            size="small"
            plain
            @click="onDiffClick('easy')"
          >太简单</el-button>
          <el-button
            :type="difficulty === 'ok' ? 'success' : 'default'"
            size="small"
            plain
            @click="onDiffClick('ok')"
          >刚好</el-button>
          <el-button
            :type="difficulty === 'hard' ? 'danger' : 'default'"
            size="small"
            plain
            @click="onDiffClick('hard')"
          >太难</el-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-area">
      <el-empty description="点击按钮开始生成">
        <el-button type="primary" @click="doGenerate" :loading="loading">
          生成 {{ typeLabel }}
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import 'mind-elixir/style'
import MindElixir from 'mind-elixir'
import { generateResource, getChapterResource } from '@/api/resource'
import { saveProfile } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const resourceType = computed(() => (route.params.type as string) || 'mindmap')
const chapterId = computed(() => {
  const raw = route.query.chapterId
  return raw ? Number(raw) : 0
})
const loading = ref(false)
const content = ref('')
const questions = ref<any[]>([])
const liked = ref(false)
const disliked = ref(false)
const difficulty = ref('ok')
const mmEl = ref<HTMLElement>()

const typeMap: Record<string, string> = {
  mindmap: '思维导图', quiz: '练习题目',
  reading: '拓展阅读', code: '代码案例',
}
const typeLabel = computed(() => typeMap[resourceType.value] || resourceType.value)

/** mind-elixir 实例引用 */
let mindInstance: any = null

/** 组件卸载时清理 mind-elixir 实例 */
onUnmounted(() => {
  if (mindInstance) {
    mindInstance.destroy()
    mindInstance = null
  }
})

/** 清理 AI 返回内容中的 Markdown 代码块标记，提取纯净 JSON */
function cleanJson(raw: string): string {
  let s = raw.trim()
  // 去掉开头的 ```json 或 ```
  s = s.replace(/^```(?:json)?\s*/i, '')
  // 去掉结尾的 ```
  s = s.replace(/```\s*$/i, '')
  // 去掉首尾空白后，如果整体被花括号/方括号包裹，尝试提取 JSON
  s = s.trim()
  // 查找第一个 { 或 [ 到最后一个 } 或 ]
  const firstBrace = s.indexOf('{')
  const firstBracket = s.indexOf('[')
  const first = firstBrace === -1 ? firstBracket : firstBracket === -1 ? firstBrace : Math.min(firstBrace, firstBracket)
  const lastBrace = s.lastIndexOf('}')
  const lastBracket = s.lastIndexOf(']')
  const last = lastBrace === -1 ? lastBracket : lastBracket === -1 ? lastBrace : Math.max(lastBrace, lastBracket)
  if (first !== -1 && last !== -1 && last > first) {
    s = s.slice(first, last + 1)
  }
  return s
}

/** 用 mind-elixir 渲染思维导图（直接接受 AI 返回的 JSON 树） */
async function renderMindmap(raw: string) {
  if (!mmEl.value) return

  if (mindInstance) {
    mindInstance.destroy()
    mindInstance = null
  }
  mmEl.value.innerHTML = ''

  let nodeData: any
  try {
    nodeData = JSON.parse(cleanJson(raw))
    if (!nodeData.topic) throw new Error('不是有效的思维导图 JSON')
  } catch {
    mmEl.value.textContent = raw
    return
  }

  await nextTick()

  try {
    mindInstance = new MindElixir({
      el: mmEl.value,
      direction: MindElixir.RIGHT,
      editable: false,
      contextMenu: false,
      toolBar: false,
      keypress: false,
      theme: MindElixir.THEME,
    })
    const err = mindInstance.init({ nodeData })
    if (err) throw err
    mindInstance.scaleFit()
  } catch {
    mmEl.value.textContent = raw
  }
}

/** 放大 */
function mmZoomIn() {
  if (!mindInstance) return
  const cur = mindInstance.scaleVal
  mindInstance.scale(Math.min(cur * 1.3, 3))
}

/** 缩小 */
function mmZoomOut() {
  if (!mindInstance) return
  const cur = mindInstance.scaleVal
  mindInstance.scale(Math.max(cur / 1.3, 0.15))
}

/** 适应屏幕 */
function mmZoomReset() {
  if (!mindInstance) return
  mindInstance.scaleFit()
}

/** 下载 PNG 图片 */
async function mmDownload() {
  if (!mindInstance) return
  try {
    const blob = await mindInstance.exportPng()
    if (!blob) {
      ElMessage.warning('导出失败')
      return
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `思维导图_${Date.now()}.png`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('图片已下载')
  } catch {
    ElMessage.error('下载失败')
  }
}

/** 统一处理 AI 返回的内容展示 */
function applyContent(res: any) {
  if (!res?.content) return
  const raw = cleanJson(res.content)

  if (resourceType.value === 'quiz') {
    try {
      const parsed = JSON.parse(raw)
      questions.value = Array.isArray(parsed) ? parsed : [parsed]
      questions.value = questions.value.map((q: any) => ({ ...q, showAnswer: false }))
    } catch {
      content.value = res.content
    }
  } else if (resourceType.value === 'mindmap') {
    content.value = res.content
    nextTick().then(() => renderMindmap(res.content))
  } else {
    content.value = res.content
  }
}

/** 首次加载：有 chapterId 则查DB缓存，没有则直接 AI 生成 */
const initialLoad = async () => {
  loading.value = true
  content.value = ''
  questions.value = []

  try {
    if (chapterId.value > 0) {
      const res = await getChapterResource(
        chapterId.value,
        resourceType.value,
        'medium',
        'Java 程序设计',
        'Java'
      )
      // 先关 loading 再设内容，确保 nextTick 触发时 content-area 已在 DOM 中
      loading.value = false
      applyContent(res)
    } else {
      const res = await generateResource({
        chapterId: 0,
        chapterName: 'Java 程序设计',
        topic: 'Java',
        type: resourceType.value,
        difficulty: 'medium',
      })
      loading.value = false
      applyContent(res)
    }
  } catch {
    loading.value = false
    ElMessage.error('AI 生成失败，请重试')
  }
}

/** 反馈触发生成：强制调 AI，跳过缓存，存 DB */
const doGenerate = async () => {
  loading.value = true
  content.value = ''
  questions.value = []

  try {
    const res = await generateResource({
      chapterId: chapterId.value > 0 ? chapterId.value : 0,
      chapterName: 'Java 程序设计',
      topic: 'Java',
      type: resourceType.value,
      difficulty: 'medium',
      force: true,
    })
    // 先关 loading 再设内容，确保 nextTick 时 content-area 已在 DOM
    loading.value = false
    applyContent(res)
  } catch {
    loading.value = false
    ElMessage.error('AI 生成失败，请重试')
  }
}

/** 评价当前内容：记录画像 + AI 换个方向重新生成 */
const onDiffClick = async (val: string) => {
  // 更新高亮状态
  difficulty.value = val

  // 1. 记录画像
  const userId = userStore.userInfo?.id
  if (userId) {
    try {
      await saveProfile({
        userId,
        difficulty_preference: val,
        resource_type: resourceType.value,
      })
    } catch {
      // 画像保存失败不影响核心流程
    }
  }

  // 2. AI 换个方向重新生成（不改变难度，只换讲解角度）
  ElMessage.info('正在根据你的反馈调整内容方向...')
  await doGenerate()
  ElMessage.success('已换个方向重新生成')
}

const like = () => {
  liked.value = !liked.value
  if (liked.value) disliked.value = false
  ElMessage.success('感谢反馈！')
}

const dislike = () => {
  disliked.value = !disliked.value
  if (disliked.value) liked.value = false
  ElMessage.info('我们会优化内容')
}

onMounted(() => {
  initialLoad()
})
</script>

<style scoped>
.generate-page { padding: 24px; max-width: 900px; margin: 0 auto; }
.top-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.top-bar h2 { margin: 0; font-size: 18px; }

.loading-area { text-align: center; padding: 80px 0; color: #909399; }
.loading-area p { margin-top: 16px; font-size: 15px; }

.content-area { background: #fff; border-radius: 10px; padding: 24px; border: 1px solid #ebeef5; }

/* 思维导图 — 外层只给尺寸，内部由 mind-elixir 全权控制 */
.mm-wrapper { position: relative; }
.mindmap-box { width: 100%; height: 520px; }
.mm-toolbar {
  display: flex; align-items: center; gap: 4px;
  padding: 8px 0 0; justify-content: center;
  color: #909399; font-size: 13px;
}
.mm-toolbar .el-button { font-size: 15px; }

/* 代码 */
.code-box pre { background: #f5f7fa; padding: 16px; border-radius: 8px; overflow-x: auto; font-size: 13px; }

/* 拓展阅读 */
.reading-box { line-height: 1.8; color: #333; }
.reading-box :deep(h4) { margin: 16px 0 6px; }
.reading-box :deep(p) { margin: 0 0 10px; }

/* 练习题目 */
.quiz-item { background: #f8f9fb; padding: 16px; border-radius: 8px; margin-bottom: 10px; }
.q-header { display: flex; align-items: center; gap: 8px; font-weight: 500; }
.q-num { display: inline-flex; width: 24px; height: 24px; border-radius: 50%; background: #4f8cff; color: #fff; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.q-options { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 0 32px; }
.opt { padding: 6px 14px; background: #f0f2f5; border-radius: 6px; font-size: 13px; }
.opt.correct { background: #e6f7e6; color: #52c41a; font-weight: 600; }
.q-answer { margin: 8px 0 0 32px; color: #52c41a; font-weight: 500; }

/* 反馈栏 */
.feedback-bar {
  margin-top: 20px; padding: 16px; border-top: 1px solid #ebeef5;
  display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266;
  flex-wrap: wrap;
}
.diff-btns { display: flex; gap: 4px; }

.empty-area { padding: 80px 0; }
</style>
