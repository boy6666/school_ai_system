<template>
  <div class="resource-detail-page">
    <div class="top-bar">
      <button type="button" class="back-button" @click="goBack">
        ← 返回资源中心
      </button>
    </div>

    <div v-if="loading" class="state-box">
      正在加载资源……
    </div>

    <div v-else-if="errorMessage" class="state-box error">
      <p>{{ errorMessage }}</p>
      <button type="button" @click="loadResource">
        重新加载
      </button>
    </div>

    <article v-else-if="resource" class="content-box">
      <header class="resource-header">
        <h1>{{ resource.title }}</h1>

        <div class="meta">
          <span>{{ getTypeLabel(resource.type) }}</span>
          <span>{{ getDifficultyLabel(resource.difficulty) }}</span>
          <span v-if="resource.chapter">{{ resource.chapter }}</span>
          <span v-if="resource.views !== undefined">
            浏览 {{ resource.views }}
          </span>
        </div>
      </header>

      <div
        v-if="resource.content"
        class="markdown-body"
        v-html="resourceHtml"
      ></div>

      <div v-else class="empty-content">
        暂无资源内容
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

import {
  getResource,
  type ResourceVO
} from '@/api/resource'

marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, {
        language: lang
      }).value
    }

    return hljs.highlightAuto(code).value
  }
}))

marked.setOptions({
  breaks: true,
  gfm: true
})

const route = useRoute()
const router = useRouter()

const resource = ref<ResourceVO | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const resourceId = computed(() => {
  const id = Number(route.params.id)
  return Number.isInteger(id) && id > 0 ? id : null
})

const resourceHtml = computed(() => {
  return resource.value?.content
    ? marked.parse(resource.value.content)
    : ''
})

function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    mindmap: '思维导图',
    quiz: '练习题目',
    reading: '拓展阅读',
    code: '代码案例'
  }

  return labels[type] ?? type ?? '其他资源'
}

function getDifficultyLabel(difficulty: string): string {
  const labels: Record<string, string> = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }

  return labels[difficulty] ?? difficulty ?? '未设置难度'
}

function goBack(): void {
  router.push('/student/resources')
}

async function loadResource(): Promise<void> {
  if (resourceId.value === null) {
    resource.value = null
    errorMessage.value = '资源编号无效'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    resource.value = await getResource(resourceId.value)
  } catch (error) {
    console.error('加载资源详情失败：', error)
    resource.value = null
    errorMessage.value = '资源详情加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

watch(resourceId, loadResource)
onMounted(loadResource)
</script>

<style scoped>
.resource-detail-page {
  max-width: 1000px;
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 32px 40px;
}

.top-bar {
  margin-bottom: 20px;
}

.back-button {
  padding: 8px 14px;
  color: #4f8cff;
  background: transparent;
  border: 1px solid #d9e5ff;
  border-radius: 8px;
  cursor: pointer;
}

.back-button:hover {
  background: #f5f8ff;
}

.state-box,
.content-box {
  padding: 32px 40px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 14px;
}

.state-box {
  color: #666;
  text-align: center;
}

.state-box.error {
  color: #d93025;
}

.resource-header {
  padding-bottom: 20px;
  margin-bottom: 24px;
  border-bottom: 1px solid #ebeef5;
}

.resource-header h1 {
  margin: 0 0 14px;
  color: #1a1a1a;
  font-size: 28px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.meta span {
  padding: 4px 10px;
  color: #567;
  background: #f3f6fa;
  border-radius: 12px;
  font-size: 13px;
}

.markdown-body {
  color: #333;
  font-size: 15px;
  line-height: 1.9;
}

.markdown-body :deep(pre) {
  padding: 18px;
  overflow-x: auto;
  color: #cdd6f4;
  background: #1e1e2e;
  border-radius: 10px;
}

.markdown-body :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 10px 14px;
  border: 1px solid #e4e7ed;
  text-align: left;
}

.empty-content {
  padding: 50px 0;
  color: #999;
  text-align: center;
}
</style>