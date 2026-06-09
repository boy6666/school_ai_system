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
        <div v-for="(q, i) in questions" :key="i" class="quiz-item" :class="{ answered: q._submitted }">
          <div class="q-header">
            <span class="q-num">{{ i + 1 }}</span>
            <span>{{ q.question || q.title || '题目' }}</span>
            <span v-if="q._submitted" :class="['q-badge', q._isCorrect ? 'correct' : 'wrong']">
              {{ q._isCorrect ? '✓ 正确' : '✗ 错误' }}
            </span>
          </div>

          <!-- 选择题选项 -->
          <div v-if="q.options?.length" class="q-options">
            <div
              v-for="(opt, j) in q.options"
              :key="j"
              :class="[
                'opt-btn',
                { selected: q._selected === opt },
                { 'is-correct': q._submitted && getOptLabel(opt) === q.answer },
                { 'is-wrong': q._submitted && q._selected === opt && getOptLabel(opt) !== q.answer },
              ]"
              @click="submitChoice(q, opt, i)"
            >
              {{ opt }}
            </div>
          </div>

          <!-- 简答题输入 -->
          <div v-else class="q-short">
            <el-input
              v-model="q._inputVal"
              type="textarea"
              :rows="3"
              :disabled="q._submitted"
              placeholder="输入你的答案..."
            />
            <el-button
              v-if="!q._submitted"
              type="primary"
              size="small"
              class="q-submit-btn"
              :loading="q._loading"
              @click="submitShort(q, i)"
            >提交答案</el-button>
          </div>

          <!-- AI 讲解 -->
          <div v-if="q._submitted && q._explanation" class="q-explain">
            <div class="q-explain-header">📖 AI 个性化讲解</div>
            <div class="q-explain-body" v-html="q._explanation"></div>
          </div>
          <div v-if="q._submitted && !q._explanation && q._loading" class="q-explain">
            <div class="q-explain-header">⏳ AI 正在生成讲解...</div>
          </div>

          <!-- 继续提问 -->
          <div v-if="q._submitted && q._explanation" class="q-continue">
            <el-button type="primary" size="small" @click="continueToTutor(q, i)">
              继续提问 →
            </el-button>
          </div>
        </div>
      </div>

      <!-- 拓展阅读 -->
      <div v-else-if="resourceType === 'reading'" class="reading-box" v-html="readingHtml"></div>

      <!-- 代码案例 -->
      <div v-else class="code-box">
        <div class="code-header">
          <span class="code-lang">{{ detectedLang }}</span>
          <el-button text size="small" class="code-copy-btn" @click="copyCode">📋 复制</el-button>
        </div>
        <pre><code class="hljs" v-html="highlightedCode"></code></pre>
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
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

// marked + highlight 插件
marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  }
}))
marked.setOptions({ breaks: true, gfm: true })
import { generateResource, getChapterResource } from '@/api/resource'
import { saveProfile } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import { getExplain, getAnsweredQuestions } from '@/api/tutor'

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
const currentResourceId = ref<number>(0)
const liked = ref(false)
const disliked = ref(false)
const difficulty = ref('ok')
const mmEl = ref<HTMLElement>()

const typeMap: Record<string, string> = {
  mindmap: '思维导图', quiz: '练习题目',
  reading: '拓展阅读', code: '代码案例',
}
const typeLabel = computed(() => typeMap[resourceType.value] || resourceType.value)

/** 拓展阅读：Markdown → HTML */
const readingHtml = computed(() => {
  if (!content.value || resourceType.value !== 'reading') return ''
  return marked.parse(content.value, { breaks: true, gfm: true })
})

/** 代码案例：语法高亮 */
const highlightedCode = computed(() => {
  if (!content.value || resourceType.value !== 'code') return content.value
  try {
    return hljs.highlightAuto(content.value).value
  } catch {
    return content.value
  }
})

/** 代码案例：检测语言 */
const detectedLang = computed(() => {
  if (!content.value || resourceType.value !== 'code') return ''
  try {
    const result = hljs.highlightAuto(content.value)
    return result.language || 'code'
  } catch {
    return 'code'
  }
})

/** 代码案例：复制 */
const copyCode = async () => {
  if (!content.value) return
  try {
    await navigator.clipboard.writeText(content.value)
    ElMessage.success('代码已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

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
      // 保存 resourceId 用于查已答题
      if (res?.id) currentResourceId.value = res.id
      // 初始化每题交互状态
      questions.value = questions.value.map((q: any) => ({
        ...q,
        _selected: '',
        _inputVal: '',
        _submitted: false,
        _isCorrect: false,
        _explanation: '',
        _loading: false,
      }))
      // 查已答题并填补空缺
      checkAndFillAnswered()
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

/** 提取选项的字母标签（"A. xxx" → "A"） */
function getOptLabel(opt: string): string {
  const m = opt.match(/^([A-Za-z])/)
  return m ? m[1].toUpperCase() : opt
}

/** 提取选项文本（"A. xxx" → "xxx"） */


/** 提交选择题答案 */
async function submitChoice(q: any, opt: string, _idx?: number) {
  if (q._submitted) return
  q._selected = opt
  q._submitted = true
  const userLabel = getOptLabel(opt)
  const correctLabel = typeof q.answer === 'string' ? q.answer.toUpperCase() : String(q.answer)
  q._isCorrect = userLabel === correctLabel
  q._loading = true

  try {
    const res = await getExplain({
      resourceId: currentResourceId.value || undefined,
      question: q.question,
      questionType: 'choice',
      userAnswer: userLabel,
      correctAnswer: correctLabel,
      isCorrect: q._isCorrect,
    })
    q._explanation = res.explanation
  } catch {
    q._explanation = q._isCorrect
      ? '✅ 回答正确！' + (q.explanation ? '\n\n' + q.explanation : '')
      : '❌ 正确答案是 ' + correctLabel + '。' + (q.explanation ? '\n\n' + q.explanation : '')
  }
  q._loading = false
}

/** 提交简答题答案 */
async function submitShort(q: any, _idx: number) {
  if (q._submitted || !q._inputVal?.trim()) return
  q._submitted = true
  q._loading = true

  try {
    const res = await getExplain({
      resourceId: currentResourceId.value || undefined,
      question: q.question,
      questionType: 'short_answer',
      userAnswer: q._inputVal.trim(),
      correctAnswer: q.answer || q.explanation || '',
      isCorrect: false,
    })
    q._isCorrect = res.correct
    q._explanation = res.explanation
  } catch {
    q._explanation = 'AI 评判失败，请重试。'
  }
  q._loading = false
}

/** 继续提问 → 跳转智能辅导 */
function continueToTutor(q: any, _idx: number) {
  const question = q.question || ''
  const userAns = q._selected || q._inputVal || ''
  const correctAns = q.answer || ''
  const explainText = q._explanation || ''

  // 写入 localStorage
  const messages = [
    {
      role: 'user',
      content: `我遇到一道题：${question}\n我的答案：${userAns}\n正确答案：${correctAns}\n请帮我深入讲解一下。`,
      time: Date.now(),
    },
    {
      role: 'assistant',
      content: explainText,
      time: Date.now(),
    },
  ]
  localStorage.setItem('tutor_current_messages', JSON.stringify(messages))
  localStorage.removeItem('tutor_current_session')
  // 跳转
  window.open('/student/tutor', '_self')
}

/** 检查已答题并生成新题填补空缺 */
async function checkAndFillAnswered() {
  if (resourceType.value !== 'quiz' || !currentResourceId.value) return
  try {
    const answered = await getAnsweredQuestions(currentResourceId.value)
    if (!answered || answered.length === 0) return

    // 按题目文本匹配已答题
    const answeredTexts = new Set(answered.map((a: any) => a.question))
    const answeredIndices: number[] = []
    questions.value.forEach((q: any, i: number) => {
      if (answeredTexts.has(q.question)) {
        answeredIndices.push(i)
        // 标记为已做（展示历史答案）
        q._submitted = true
        const matched = answered.find((a: any) => a.question === q.question)
        if (matched) {
          q._isCorrect = matched.isCorrect === 1
          q._explanation = matched.explanation || ''
          q._selected = matched.correctAnswer // 高亮正确答案
        }
      }
    })

    if (answeredIndices.length === 0) return

    // 收集已有的题目文本，避免重复
    const existingTexts = questions.value.map((q: any) => q.question)

    // 调 AI 生成新题填补
    const newRes = await generateResource({
      chapterId: chapterId.value > 0 ? chapterId.value : 0,
      chapterName: 'Java 程序设计',
      topic: 'Java',
      type: 'quiz',
      difficulty: 'medium',
      force: true,
    })

    if (newRes?.content) {
      const clean = cleanJson(newRes.content)
      const newParsed = JSON.parse(clean)
      const newQuestions = Array.isArray(newParsed) ? newParsed : [newParsed]

      // 过滤掉与已有题目重复的新题
      const uniqueNew = newQuestions.filter(
        (nq: any) => !existingTexts.includes(nq.question)
      )

      // 替换已答题位置
      let newIdx = 0
      const updated = [...questions.value]
      for (const idx of answeredIndices) {
        if (newIdx < uniqueNew.length) {
          updated[idx] = {
            ...uniqueNew[newIdx],
            _selected: '',
            _inputVal: '',
            _submitted: false,
            _isCorrect: false,
            _explanation: '',
            _loading: false,
          }
          newIdx++
        }
      }
      questions.value = updated
    }
  } catch {
    // 填空失败不影响已有题目展示
  }
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

/* 代码案例 — 语法高亮 + 复制按钮 */
.code-box {
  border-radius: 10px; overflow: hidden;
  border: 1px solid #2d2d3d;
}
.code-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 16px; background: #1a1a2e;
  border-bottom: 1px solid #2d2d3d;
}
.code-lang {
  font-size: 12px; color: #6c7086; text-transform: uppercase;
  letter-spacing: 0.5px; font-weight: 600;
}
.code-copy-btn { color: #6c7086 !important; font-size: 12px !important; }
.code-copy-btn:hover { color: #cdd6f4 !important; }
.code-box pre {
  margin: 0; padding: 16px; overflow-x: auto;
  background: #1e1e2e; font-size: 13px; line-height: 1.7;
}
.code-box pre code.hljs { background: none; padding: 0; }

/* 拓展阅读 — Markdown 渲染优化 */
.reading-box {
  line-height: 1.9; color: #333; font-size: 15px;
  padding: 8px 4px;
}
.reading-box :deep(h1) { font-size: 24px; margin: 28px 0 14px; font-weight: 700; color: #1a1a1a; padding-bottom: 8px; border-bottom: 2px solid #409eff; }
.reading-box :deep(h2) { font-size: 20px; margin: 24px 0 12px; font-weight: 600; color: #1a1a1a; }
.reading-box :deep(h3) { font-size: 17px; margin: 20px 0 10px; font-weight: 600; color: #2c3e50; }
.reading-box :deep(h4) { font-size: 15px; margin: 16px 0 8px; font-weight: 600; color: #2c3e50; }
.reading-box :deep(p) { margin: 0 0 12px; }
.reading-box :deep(ul), .reading-box :deep(ol) { margin: 8px 0 12px; padding-left: 24px; }
.reading-box :deep(li) { margin: 4px 0; }
.reading-box :deep(blockquote) {
  margin: 12px 0; padding: 10px 16px; border-left: 4px solid #409eff;
  background: #f8faff; color: #555; border-radius: 0 6px 6px 0;
}
.reading-box :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  background: #f0f2f5; padding: 2px 6px; border-radius: 4px;
  font-size: 13px; color: #d63384;
}
.reading-box :deep(pre) {
  background: #1e1e2e; color: #cdd6f4; padding: 16px;
  border-radius: 10px; overflow-x: auto; margin: 14px 0;
  font-size: 13px; line-height: 1.6;
}
.reading-box :deep(pre code) {
  background: none; padding: 0; color: inherit; font-size: inherit;
}
.reading-box :deep(a) { color: #409eff; text-decoration: none; }
.reading-box :deep(a:hover) { text-decoration: underline; }
.reading-box :deep(img) { max-width: 100%; border-radius: 8px; margin: 12px 0; }
.reading-box :deep(hr) { border: none; border-top: 1px solid #e4e7ed; margin: 24px 0; }
.reading-box :deep(strong) { color: #1a1a1a; }
.reading-box :deep(table) {
  width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 14px;
}
.reading-box :deep(th), .reading-box :deep(td) {
  border: 1px solid #e4e7ed; padding: 8px 12px; text-align: left;
}
.reading-box :deep(th) { background: #f5f7fa; font-weight: 600; }
.reading-box :deep(tr:nth-child(even)) { background: #fafafa; }

/* 练习题目 */
.quiz-item { background: #f8f9fb; padding: 16px; border-radius: 8px; margin-bottom: 10px; }
.quiz-item.answered { border-left: 3px solid #4f8cff; }
.q-header { display: flex; align-items: center; gap: 8px; font-weight: 500; flex-wrap: wrap; }
.q-badge { font-size: 12px; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.q-badge.correct { background: #e6f7e6; color: #52c41a; }
.q-badge.wrong { background: #fde8e8; color: #e64553; }
.q-num { display: inline-flex; width: 24px; height: 24px; border-radius: 50%; background: #4f8cff; color: #fff; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.q-options { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 0 32px; }
.opt-btn {
  padding: 8px 16px; background: #fff; border: 1px solid #d9d9d9;
  border-radius: 8px; font-size: 13px; cursor: pointer;
  transition: all .2s; user-select: none;
}
.opt-btn:hover { border-color: #4f8cff; color: #4f8cff; }
.opt-btn.selected { border-color: #4f8cff; background: #eef4ff; color: #4f8cff; font-weight: 600; }
.opt-btn.is-correct { border-color: #52c41a; background: #e6f7e6; color: #52c41a; font-weight: 600; cursor: default; }
.opt-btn.is-wrong { border-color: #e64553; background: #fde8e8; color: #e64553; font-weight: 600; cursor: default; }
.opt-btn.is-correct:hover, .opt-btn.is-wrong:hover { transform: none; }

/* 简答 */
.q-short { margin: 10px 0 0 32px; }
.q-submit-btn { margin-top: 8px; }

/* AI 讲解 */
.q-explain { margin: 12px 0 0 32px; background: #fff; border-radius: 8px; border: 1px solid #e8e8e8; overflow: hidden; }
.q-explain-header { font-size: 13px; font-weight: 600; color: #4f8cff; padding: 10px 14px; background: #f8faff; border-bottom: 1px solid #e8e8e8; }
.q-explain-body { padding: 12px 14px; font-size: 14px; line-height: 1.8; color: #333; white-space: pre-wrap; }

/* 继续提问 */
.q-continue { margin: 10px 0 0 32px; }

/* 反馈栏 */
.feedback-bar {
  margin-top: 20px; padding: 16px; border-top: 1px solid #ebeef5;
  display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266;
  flex-wrap: wrap;
}
.diff-btns { display: flex; gap: 4px; }

.empty-area { padding: 80px 0; }
</style>
