<template>
  <div class="generate-page">
    <div class="page-header">
      <el-button text @click="goBack">
        ← 返回资源中心
      </el-button>

      <div>
        <h1>生成学习资源</h1>
        <p>根据章节、主题和难度生成学习资源。</p>
      </div>
    </div>

    <el-card class="form-card">
      <el-form
        :model="form"
        label-width="100px"
      >
        <el-form-item label="资源类型">
          <el-select
            v-model="form.type"
            placeholder="请选择资源类型"
          >
            <el-option
              v-for="item in resourceTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="章节编号">
          <el-input
            v-model="form.chapterId"
            placeholder="请输入章节编号"
          />
        </el-form-item>

        <el-form-item label="章节">
          <el-input
            v-model="form.chapter"
            placeholder="例如：第3章 集合"
          />
        </el-form-item>

        <el-form-item label="章节名称">
          <el-input
            v-model="form.chapterName"
            placeholder="例如：Java集合框架"
          />
        </el-form-item>

        <el-form-item label="学习主题">
          <el-input
            v-model="form.topic"
            placeholder="例如：ArrayList"
          />
        </el-form-item>

        <el-form-item label="难度">
          <el-select
            v-model="form.difficulty"
            placeholder="请选择难度"
          >
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>

        <el-form-item label="强制重新生成">
          <el-switch v-model="form.force" />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleGenerate"
          >
            生成资源
          </el-button>

          <el-button @click="goBack">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card
      v-if="generatedResource"
      class="result-card"
    >
      <template #header>
        <div class="result-header">
          <span>生成结果</span>

          <el-tag>
            {{ generatedResource.status }}
          </el-tag>
        </div>
      </template>

      <h2>{{ generatedResource.title }}</h2>

      <div class="result-meta">
        <span>{{ generatedResource.type }}</span>
        <span>{{ generatedResource.difficulty }}</span>
        <span>{{ generatedResource.chapter }}</span>
      </div>

      <p v-if="generatedResource.description">
        {{ generatedResource.description }}
      </p>

      <pre
        v-if="generatedResource.content"
        class="result-content"
      >{{ generatedResource.content }}</pre>

      <el-button
        type="primary"
        @click="openDetail"
      >
        查看资源详情
      </el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  generateResource,
  type ResourceGenerateRequest,
  type ResourceType,
  type ResourceVO
} from '@/api/resource'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const generatedResource = ref<ResourceVO | null>(null)

const resourceTypes: Array<{
  label: string
  value: ResourceType
}> = [
  { label: '思维导图', value: 'mindmap' },
  { label: '练习题目', value: 'quiz' },
  { label: '拓展阅读', value: 'reading' },
  { label: '代码案例', value: 'code' }
]

function queryText(
  value: string | string[] | null | undefined
): string {
  return Array.isArray(value)
    ? value[0] || ''
    : value || ''
}

function initialType(): ResourceType {
  const value = queryText(route.params.type)

  return resourceTypes.some(
    item => item.value === value
  )
    ? value as ResourceType
    : 'reading'
}

const form = reactive<ResourceGenerateRequest>({
  userId:
    userStore.userInfo?.userId ??
    userStore.userInfo?.id,
  chapterId: queryText(route.query.chapterId),
  chapter: '',
  chapterName: '',
  topic: '',
  type: initialType(),
  difficulty: 'medium',
  force: false
})

async function handleGenerate(): Promise<void> {
  const hasSubject =
    Boolean(form.topic?.trim()) ||
    Boolean(form.chapterName?.trim()) ||
    Boolean(form.chapter?.trim())

  if (!hasSubject) {
    ElMessage.warning('请填写章节或学习主题')
    return
  }

  loading.value = true
  generatedResource.value = null

  try {
    generatedResource.value = await generateResource({
      ...form,
      chapter: form.chapter?.trim() || undefined,
      chapterName:
        form.chapterName?.trim() || undefined,
      topic: form.topic?.trim() || undefined,
      chapterId: form.chapterId?.trim() || undefined
    })

    ElMessage.success('资源生成成功')
  } catch {
    ElMessage.error('资源生成失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function openDetail(): void {
  if (!generatedResource.value) return

  router.push(
    `/student/resources/${generatedResource.value.id}`
  )
}

function goBack(): void {
  router.push('/student/resources')
}
</script>

<style scoped>
.generate-page {
  max-width: 960px;
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 32px 40px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px;
  color: var(--charcoal);
}

.page-header p {
  margin: 0;
  color: var(--slate);
}

.form-card,
.result-card {
  margin-bottom: 24px;
}

.form-card :deep(.el-select) {
  width: 100%;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-card h2 {
  margin-top: 0;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  color: var(--slate);
}

.result-content {
  max-height: 360px;
  margin: 16px 0;
  padding: 16px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: var(--surface);
  border-radius: var(--radius-sm);
}

@media (max-width: 640px) {
  .generate-page {
    padding: 20px 16px;
  }

  .page-header {
    flex-direction: column;
  }
}
</style>