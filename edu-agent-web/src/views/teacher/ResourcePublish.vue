<template>
  <div class="resource-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">TEACHING RESOURCES</p>
        <h1>资源管理</h1>
        <p class="description">
          创建、查看并维护教师教学资源。
        </p>
      </div>

      <el-button
        type="primary"
        size="large"
        @click="openCreate"
      >
        新建资源
      </el-button>
    </header>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    >
      <template #default>
        <el-button
          link
          type="primary"
          @click="loadResources"
        >
          重新加载
        </el-button>
      </template>
    </el-alert>

    <section class="resource-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索资源标题、章节或内容"
          class="keyword-input"
        />

        <el-select
          v-model="typeFilter"
          clearable
          placeholder="全部类型"
          class="filter-select"
        >
          <el-option
            label="思维导图"
            value="mindmap"
          />
          <el-option
            label="测验"
            value="quiz"
          />
          <el-option
            label="阅读材料"
            value="reading"
          />
          <el-option
            label="代码资源"
            value="code"
          />
        </el-select>

        <el-select
          v-model="statusFilter"
          clearable
          placeholder="全部状态"
          class="filter-select"
        >
        <el-option
            v-for="status in statusOptions"
            :key="status"
            :label="status"
            :value="status"
          />
        </el-select>

        <el-button @click="loadResources">
          刷新
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredResources"
        empty-text="暂无资源"
        class="resource-table"
      >
        <el-table-column
          prop="title"
          label="资源标题"
          min-width="220"
          show-overflow-tooltip
        />

        <el-table-column
          label="类型"
          width="120"
        >
          <template #default="{ row }">
            <el-tag effect="plain">
              {{ resourceTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="chapter"
          label="章节"
          min-width="160"
          show-overflow-tooltip
        />

        <el-table-column
          label="难度"
          width="100"
        >
          <template #default="{ row }">
            {{ difficultyLabel(row.difficulty) }}
          </template>
        </el-table-column>

        <el-table-column
          label="状态"
          width="110"
        >
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.status)"
              effect="light"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          label="浏览/收藏"
          width="120"
        >
          <template #default="{ row }">
            {{ row.views ?? 0 }} /
            {{ row.favorites ?? 0 }}
          </template>
        </el-table-column>

        <el-table-column
          label="创建时间"
          width="180"
        >
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="openDetail(row.id)"
            >
              详情
            </el-button>

            <el-button
              link
              type="danger"
              :loading="deletingId === row.id"
              @click="confirmDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="createDialogVisible"
      title="新建教学资源"
      width="680px"
      destroy-on-close
      @closed="resetCreateForm"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="92px"
      >
        <el-form-item
          label="资源标题"
          prop="title"
        >
          <el-input
            v-model="createForm.title"
            maxlength="128"
            show-word-limit
            placeholder="请输入资源标题"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item
            label="资源类型"
            prop="type"
          >
            <el-select
              v-model="createForm.type"
              placeholder="请选择类型"
            >
              <el-option
                label="思维导图"
                value="mindmap"
              />
              <el-option
                label="测验"
                value="quiz"
              />
              <el-option
                label="阅读材料"
                value="reading"
              />
              <el-option
                label="代码资源"
                value="code"
              />
            </el-select>
          </el-form-item>

          <el-form-item
            label="难度"
            prop="difficulty"
          >
            <el-select
              v-model="createForm.difficulty"
              placeholder="请选择难度"
            >
              <el-option
                label="简单"
                value="easy"
              />
              <el-option
                label="中等"
                value="medium"
              />
              <el-option
                label="困难"
                value="hard"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item
            label="课程名称"
            prop="courseName"
          >
            <el-input
              v-model="createForm.courseName"
              placeholder="请输入课程名称"
            />
          </el-form-item>

          <el-form-item
            label="所属章节"
            prop="chapter"
          >
            <el-input
              v-model="createForm.chapter"
              placeholder="请输入章节名称"
            />
          </el-form-item>
        </div>

        <el-form-item
          label="资源说明"
          prop="description"
        >
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入资源用途或内容说明"
          />
        </el-form-item>

        <el-form-item
          label="资源内容"
          prop="content"
        >
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入教学资源正文"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button
          :disabled="creating"
          @click="createDialogVisible = false"
        >
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="creating"
          @click="submitCreate"
        >
          确认创建
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      title="资源详情"
      size="52%"
      destroy-on-close
    >
      <div
        v-loading="detailLoading"
        class="detail-wrapper"
      >
        <el-alert
          v-if="detailError"
          :title="detailError"
          type="error"
          show-icon
          :closable="false"
        >
          <template #default>
            <el-button
              v-if="selectedResourceId"
              link
              type="primary"
              @click="openDetail(selectedResourceId)"
            >
              重新加载
            </el-button>
          </template>
        </el-alert>

        <template v-else-if="selectedResource">
          <el-descriptions
            :column="2"
            border
          >
            <el-descriptions-item label="资源标题">
              {{ selectedResource.title }}
            </el-descriptions-item>

            <el-descriptions-item label="资源类型">
              {{
                resourceTypeLabel(
                  selectedResource.type
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="所属章节">
              {{ selectedResource.chapter || '—' }}
            </el-descriptions-item>

            <el-descriptions-item label="难度">
              {{
                difficultyLabel(
                  selectedResource.difficulty
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="状态">
              {{
                statusLabel(
                  selectedResource.status
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="创建时间">
              {{
                formatDate(
                  selectedResource.createTime
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="浏览量">
              {{ selectedResource.views ?? 0 }}
            </el-descriptions-item>

            <el-descriptions-item label="收藏量">
              {{ selectedResource.favorites ?? 0 }}
            </el-descriptions-item>
          </el-descriptions>

          <section class="content-section">
            <h2>资源内容</h2>
            <pre>{{ selectedResource.content || '暂无内容' }}</pre>
          </section>

          <el-alert
            v-if="selectedResource.errorMsg"
            :title="selectedResource.errorMsg"
            type="warning"
            show-icon
            :closable="false"
          />
        </template>

        <el-empty
          v-else-if="!detailLoading"
          description="暂无资源详情"
        />
      </div>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import {
  computed,
  onMounted,
  reactive,
  ref
} from 'vue'
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import type {
  FormInstance,
  FormRules
} from 'element-plus'
import {
  createResource,
  deleteResource,
  getResource,
  getResourceList
} from '@/api/resource'
import type {
  CreateResourceRequest,
  ResourceVO
} from '@/api/resource'

const resources = ref<ResourceVO[]>([])
const loading = ref(false)
const loadError = ref('')

const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')

const createDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const creating = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const selectedResourceId = ref<number>()
const selectedResource = ref<ResourceVO>()

const deletingId = ref<number>()

const createForm =
  reactive<CreateResourceRequest>({
    title: '',
    type: '',
    difficulty: '',
    chapter: '',
    courseName: '',
    description: '',
    content: ''
  })

const createRules: FormRules = {
  title: [
    {
      required: true,
      message: '请输入资源标题',
      trigger: 'blur'
    }
  ],
  type: [
    {
      required: true,
      message: '请选择资源类型',
      trigger: 'change'
    }
  ],
  content: [
    {
      required: true,
      message: '请输入资源内容',
      trigger: 'blur'
    }
  ]
}

const statusOptions = computed(() => {
  return Array.from(
    new Set(
      resources.value
        .map((item) => item.status)
        .filter((status) => Boolean(status))
    )
  )
})

const filteredResources = computed(() => {
  const normalizedKeyword =
    keyword.value.trim().toLowerCase()

  return resources.value.filter((item) => {
    const matchesKeyword =
      !normalizedKeyword ||
      [
        item.title,
        item.chapter,
        item.content
      ].some((value) =>
        String(value ?? '')
          .toLowerCase()
          .includes(normalizedKeyword)
      )

    const matchesType =
      !typeFilter.value ||
      item.type === typeFilter.value

    const matchesStatus =
      !statusFilter.value ||
      item.status === statusFilter.value

    return (
      matchesKeyword &&
      matchesType &&
      matchesStatus
    )
  })
})

function getErrorMessage(
  error: unknown,
  fallback: string
) {
  if (
    error instanceof Error &&
    error.message.trim()
  ) {
    return error.message
  }
  return fallback
}

function resourceTypeLabel(type: string) {
  const labels: Record<string, string> = {
    mindmap: '思维导图',
    quiz: '测验',
    reading: '阅读材料',
    code: '代码资源'
  }
  return labels[type] ?? type ?? '—'
}

function difficultyLabel(
  difficulty: string
) {
  const labels: Record<string, string> = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }
  return (
    labels[difficulty] ??
    difficulty ??
    '—'
  )
}

function statusLabel(status: string) {
  return status || '未标注'
}

function statusTagType(status: string) {
  const normalized = status.toLowerCase()

  if (
    normalized === 'completed' ||
    normalized === 'success'
  ) {
    return 'success'
  }

  if (
    normalized === 'failed' ||
    normalized === 'error'
  ) {
    return 'danger'
  }

  if (
    normalized === 'processing' ||
    normalized === 'pending'
  ) {
    return 'warning'
  }

  return 'info'
}

function formatDate(value?: string) {
  if (!value) return '—'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('zh-CN', {
    hour12: false
  })
}

async function loadResources() {
  loading.value = true
  loadError.value = ''

  try {
    resources.value =
      await getResourceList()
  } catch (error) {
    resources.value = []
    loadError.value = getErrorMessage(
      error,
      '资源列表加载失败'
    )
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createDialogVisible.value = true
}

function resetCreateForm() {
  createFormRef.value?.resetFields()

  Object.assign(createForm, {
    title: '',
    type: '',
    difficulty: '',
    chapter: '',
    courseName: '',
    description: '',
    content: ''
  })
}

async function submitCreate() {
  const valid =
    await createFormRef.value
      ?.validate()
      .catch(() => false)

  if (!valid) return

  creating.value = true

  const data: CreateResourceRequest = {
    title: createForm.title.trim(),
    type: createForm.type,
    content: createForm.content?.trim()
  }

  if (createForm.difficulty) {
    data.difficulty =
      createForm.difficulty
  }

  if (createForm.chapter?.trim()) {
    data.chapter =
      createForm.chapter.trim()
  }

  if (createForm.courseName?.trim()) {
    data.courseName =
      createForm.courseName.trim()
  }

  if (createForm.description?.trim()) {
    data.description =
      createForm.description.trim()
  }

  try {
    await createResource(data)
    ElMessage.success('资源创建成功')
    createDialogVisible.value = false
    await loadResources()
  } catch (error) {
    ElMessage.error(
      getErrorMessage(
        error,
        '资源创建失败'
      )
    )
  } finally {
    creating.value = false
  }
}

async function openDetail(id: number) {
  selectedResourceId.value = id
  selectedResource.value = undefined
  detailError.value = ''
  detailVisible.value = true
  detailLoading.value = true

  try {
    selectedResource.value =
      await getResource(id)
  } catch (error) {
    detailError.value = getErrorMessage(
      error,
      '资源详情加载失败'
    )
  } finally {
    detailLoading.value = false
  }
}

async function confirmDelete(
  resource: ResourceVO
) {
  try {
    await ElMessageBox.confirm(
      `删除后无法继续使用资源“${resource.title}”，确认删除吗？`,
      '删除资源',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  deletingId.value = resource.id

  try {
    await deleteResource(resource.id)
    ElMessage.success('资源删除成功')

    if (
      selectedResourceId.value ===
      resource.id
    ) {
      detailVisible.value = false
      selectedResource.value = undefined
    }

    await loadResources()
  } catch (error) {
    ElMessage.error(
      getErrorMessage(
        error,
        '资源删除失败'
      )
    )
  } finally {
    deletingId.value = undefined
  }
}

onMounted(() => {
  loadResources()
})
</script>
<style scoped>
.resource-page {
  min-height: calc(100vh - 72px);
  padding: 34px 46px 48px;
  background: #f7f6f4;
  color: #181818;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 26px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #5140df;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1.5px;
}

.page-header h1 {
  margin: 0;
  font-size: 42px;
  line-height: 1.2;
}

.description {
  margin: 10px 0 0;
  color: #aaa5a0;
  font-size: 16px;
  line-height: 1.7;
}

.page-header :deep(.el-button--primary),
:deep(.el-dialog__footer .el-button--primary) {
  border-color: #5140df;
  background: #5140df;
}

.page-header :deep(.el-button--primary:hover),
:deep(.el-dialog__footer .el-button--primary:hover) {
  border-color: #6253e8;
  background: #6253e8;
}

.page-alert {
  margin-bottom: 18px;
  border-radius: 8px;
}

.resource-card {
  min-height: 610px;
  padding: 24px;
  border: 1px solid #dedbd7;
  border-radius: 12px;
  background: #fff;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}

.keyword-input {
  width: min(380px, 100%);
}

.filter-select {
  width: 160px;
}

.toolbar :deep(.el-input__wrapper),
.toolbar :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
}

.resource-table {
  width: 100%;
}

.resource-table :deep(th.el-table__cell) {
  height: 50px;
  background: #faf9f8;
  color: #6d6762;
  font-weight: 500;
}

.resource-table :deep(td.el-table__cell) {
  height: 66px;
}

.resource-table :deep(.el-button) {
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.form-grid :deep(.el-select) {
  width: 100%;
}

:deep(.el-dialog) {
  border-radius: 12px;
}

:deep(.el-dialog__header) {
  padding: 24px 26px 12px;
}

:deep(.el-dialog__title) {
  font-size: 21px;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px 26px;
}

:deep(.el-dialog__footer) {
  padding: 12px 26px 24px;
}

:deep(.el-form-item__label) {
  color: #5b5753;
  font-weight: 500;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
}

:deep(.el-textarea__inner) {
  padding: 13px;
  border-radius: 8px;
  line-height: 1.7;
}

.detail-wrapper {
  min-height: 420px;
  padding: 4px 2px 28px;
}

.detail-wrapper :deep(.el-descriptions__label) {
  width: 120px;
  color: #5b5753;
  font-weight: 600;
}

.content-section {
  margin-top: 28px;
}

.content-section h2 {
  margin: 0 0 14px;
  font-size: 22px;
}

.content-section pre {
  min-height: 220px;
  max-height: 520px;
  margin: 0 0 22px;
  padding: 20px;
  overflow: auto;
  border: 1px solid #e4e1dd;
  border-radius: 9px;
  background: #faf9f8;
  color: #35312e;
  font-family:
    "Microsoft YaHei",
    system-ui,
    sans-serif;
  font-size: 15px;
  line-height: 1.85;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 1100px) {
  .resource-page {
    padding: 28px 28px 40px;
  }

  .toolbar {
    flex-wrap: wrap;
  }

  .keyword-input {
    width: 100%;
  }

  .filter-select {
    flex: 1;
    min-width: 150px;
  }
}

@media (max-width: 700px) {
  .resource-page {
    padding: 22px 16px 32px;
  }

  .page-header {
    flex-direction: column;
  }

  .page-header h1 {
    font-size: 34px;
  }

  .page-header :deep(.el-button) {
    width: 100%;
  }

  .resource-card {
    padding: 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  :deep(.el-dialog) {
    width: calc(100% - 28px) !important;
  }

  :deep(.el-drawer) {
    width: 92% !important;
  }
}
</style>