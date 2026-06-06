<template>
  <div class="gp">
    <div class="top">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2>{{ typeLabel }}</h2>
      <div class="diff">
        <span v-for="d in ['简单','适合','困难']" :key="d" :class="['dt',{on:curDiff===d}]" @click="switchDiff(d)">{{ d }}</span>
      </div>
    </div>

    <div v-if="loading" class="ld">加载中...</div>

    <div v-else class="body">
      <!-- 思维导图：用 markmap 渲染 -->
      <div v-if="typeKey === 'mindmap'" ref="mmEl" class="mm-box"></div>

      <!-- 其他：Markdown 渲染 -->
      <div v-else class="md" v-html="html"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

marked.setOptions({ breaks: true, gfm: true, highlight: (c: string, l: string) => l && hljs.getLanguage(l) ? hljs.highlight(c, { language: l }).value : hljs.highlightAuto(c).value })

const route = useRoute()
const typeKey = computed(() => (route.params.type as string) || 'mindmap')
const curDiff = ref('适合')
const loading = ref(true)
const content = ref('')
const mmEl = ref<HTMLElement>()

const userStore = useUserStore()
const typeLabel = computed(() => ({ mindmap: '思维导图', quiz: '练习题目', reading: '拓展阅读', code: '代码案例' }[typeKey.value] || typeKey.value))
const html = computed(() => content.value ? marked.parse(content.value) : '')

const renderMindmap = async () => {
  if (typeKey.value !== 'mindmap' || !content.value || !mmEl.value) return
  await nextTick()
  try {
    const { Transformer } = await import('markmap-lib')
    const { Markmap } = await import('markmap-view')
    const { root } = new Transformer().transform(content.value)
    Markmap.create(mmEl.value, {}, root)
  } catch {}
}

const diffMap: Record<string, string> = { '简单': '入门', '适合': '基础', '困难': '进阶' }
const typeMap: Record<string, string> = { mindmap: '思维导图', quiz: '题库', reading: '拓展阅读', code: '代码案例' }

const doGenerate = async () => {
  const sid = userStore.userInfo?.id || '9'
  loading.value = true
  content.value = ''

  // 1. 查 DB（按学生+类型+难度精确匹配）
  try {
    const list: any = await request.get('/resources', {
      params: { type: typeMap[typeKey.value], difficulty: diffMap[curDiff.value], chapterName: 'Java' }
    })
    if (Array.isArray(list) && list.length && list[0]?.content) {
      content.value = list[0].content
      loading.value = false
      await nextTick()
      renderMindmap()
      return
    }
  } catch {}

  // 2. 调 AI 生成
  try {
    const res: any = await request.post('/resources/generate', {
      studentId: sid,
      type: typeKey.value,
      title: '通用',
      chapterName: 'Java',
      difficulty: curDiff.value
    })
    if (res?.content) content.value = res.content
  } catch {}

  loading.value = false
  await nextTick()
  renderMindmap()
}

const switchDiff = (d: string) => {
  curDiff.value = d
  // 难度变化 → 通知画像更新
  const level = d === '简单' ? -1 : d === '困难' ? 1 : 0
  if (level !== 0) {
    try {
      request.post('/profile/save', {
        userId: userStore.userInfo?.id,
        pace: level > 0 ? '快速' : '慢速'
      })
    } catch {}
  }
  doGenerate()
}

onMounted(() => { doGenerate() })
</script>

<style scoped>
.gp { padding: 24px; max-width: 900px; margin: 0 auto; }
.top { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.top h2 { margin: 0; font-size: 18px; flex: 1; }
.diff { display: flex; gap: 4px; }
.dt { padding: 4px 12px; border-radius: 12px; border: 1px solid #ddd; cursor: pointer; font-size: 12px; background: #fff; }
.dt.on { background: #1479ff; color: #fff; border-color: #1479ff; }
.ld { text-align: center; padding: 80px; color: #909399; }
.body { background: #fff; border-radius: 10px; padding: 24px; border: 1px solid #eee; min-height: 300px; }
.md { line-height: 1.9; font-size: 15px; color: #333; }
.md :deep(p) { margin: 8px 0; }
.md :deep(pre) { background: #f6f8fa; padding: 16px; border-radius: 8px; overflow-x: auto; }
.md :deep(code) { font-size: 14px; }
.mm-box { width: 100%; height: 520px; }
</style>
