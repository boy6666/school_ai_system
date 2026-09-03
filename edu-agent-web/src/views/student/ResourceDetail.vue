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
      <div class="resource-actions">
        <el-button
          type="primary"
          plain
          :loading="regenerating"
          @click="handleRegenerate"
        >
          重新生成
        </el-button>
      </div>
      <div class="feedback-panel">
        <span class="feedback-label">使用反馈</span>

        <el-radio-group v-model="liked">
          <el-radio :value="true">喜欢</el-radio>
          <el-radio :value="false">不喜欢</el-radio>
        </el-radio-group>

        <el-input
          v-model="difficultyFeedback"
          maxlength="100"
          placeholder="请输入难度反馈（可选）"
        />

        <el-button
          type="primary"
          :loading="feedbackSubmitting"
          @click="handleSubmitFeedback"
        >
          提交反馈
        </el-button>
      </div>
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
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

import {
  getResource,
  regenerateResource,
  submitResourceFeedback,
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
const regenerating = ref(false)
const feedbackSubmitting = ref(false)
const liked = ref<boolean>()
const difficultyFeedback = ref('')
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

async function handleRegenerate(): Promise<void> {
  if (resourceId.value === null) {
    ElMessage.error('资源编号无效')
    return
  }

  regenerating.value = true

  try {
    resource.value = await regenerateResource(
      resourceId.value
    )
    ElMessage.success('资源重新生成成功')
  } catch {
    ElMessage.error('资源重新生成失败，请稍后重试')
  } finally {
    regenerating.value = false
  }
}

async function handleSubmitFeedback(): Promise<void> {
  if (resourceId.value === null) {
    ElMessage.error('资源编号无效')
    return
  }

  if (
    liked.value === undefined &&
    !difficultyFeedback.value.trim()
  ) {
    ElMessage.warning('请选择使用感受或填写难度反馈')
    return
  }

  feedbackSubmitting.value = true

  try {
    await submitResourceFeedback(resourceId.value, {
      liked: liked.value,
      difficultyFeedback:
        difficultyFeedback.value.trim() || undefined
    })

    ElMessage.success('反馈提交成功')
    liked.value = undefined
    difficultyFeedback.value = ''
  } catch {
    ElMessage.error('反馈提交失败，请稍后重试')
  } finally {
    feedbackSubmitting.value = false
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

.resource-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.feedback-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  margin-bottom: 24px;
  background: #f7f9fc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.feedback-label {
  flex-shrink: 0;
  color: #303133;
  font-weight: 600;
}

.feedback-panel :deep(.el-input) {
  flex: 1;
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