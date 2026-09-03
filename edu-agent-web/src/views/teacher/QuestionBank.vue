<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">QUESTION BANK</p>
        <h1>题库管理</h1>
        <p class="description">
          按章节、知识点、题型和难度维护教学题目。
        </p>
      </div>

      <div class="header-actions">
        <el-button @click="aiDialogVisible = true">
          AI 出题
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          新增题目
        </el-button>
      </div>
    </header>

    <el-card class="content-card" shadow="never">
      <div class="filter-grid">
        <el-input
          v-model="filters.chapter"
          clearable
          placeholder="章节"
          @keyup.enter="loadQuestions"
        />
        <el-input
          v-model="filters.topic"
          clearable
          placeholder="知识点"
          @keyup.enter="loadQuestions"
        />
        <el-select
          v-model="filters.type"
          clearable
          placeholder="题型"
        >
          <el-option label="选择题" value="choice" />
          <el-option label="填空题" value="blank" />
          <el-option label="代码题" value="code" />
        </el-select>
        <el-select
          v-model="filters.difficulty"
          clearable
          placeholder="难度"
        >
          <el-option label="简单" value="easy" />
          <el-option label="中等" value="medium" />
          <el-option label="困难" value="hard" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="loadQuestions">
          查询
        </el-button>
        <el-button @click="resetFilters">
          重置
        </el-button>
      </div>

      <div v-if="loadError && !loading" class="state-container">
        <el-result
          icon="warning"
          title="题库加载失败"
          sub-title="请检查教师服务或网关是否可用"
        >
          <template #extra>
            <el-button type="primary" @click="loadQuestions">
              重新加载
            </el-button>
          </template>
        </el-result>
      </div>

      <el-table
        v-else
        v-loading="loading"
        :data="questions"
        row-key="id"
        empty-text="暂无题目"
      >
        <el-table-column label="题目内容" min-width="260">
          <template #default="{ row }">
            <div class="question-content">
              {{ row.content }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="题型" width="100">
          <template #default="{ row }">
            <el-tag>
              {{ getQuestionTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="chapter" label="章节" min-width="140">
          <template #default="{ row }">
            {{ row.chapter || '—' }}
          </template>
        </el-table-column>

        <el-table-column
          prop="topic"
          label="知识点"
          min-width="130"
        >
          <template #default="{ row }">
            {{ row.topic || '—' }}
          </template>
        </el-table-column>

        <el-table-column label="难度" width="100">
          <template #default="{ row }">
            <el-tag
              :type="
                getQuestionDifficultyTagType(
                  row.difficulty
                )
              "
            >
              {{
                getQuestionDifficultyLabel(
                  row.difficulty
                )
              }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :loading="editingQuestionId === row.id && detailLoading"
              @click="openEditDialog(row.id)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="confirmDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="questionDialogVisible"
      :title="editingQuestionId === null ? '新增题目' : '编辑题目'"
      width="720px"
      class="question-dialog"
      destroy-on-close
      @closed="resetQuestionForm"
    >
      <el-form
        ref="questionFormRef"
        :model="questionForm"
        :rules="questionRules"
        label-width="88px"
      >
        <div class="form-grid">
          <el-form-item label="题型" prop="type">
            <el-select
              v-model="questionForm.type"
              placeholder="请选择题型"
              @change="handleTypeChange"
            >
              <el-option label="选择题" value="choice" />
              <el-option label="填空题" value="blank" />
              <el-option label="代码题" value="code" />
            </el-select>
          </el-form-item>

          <el-form-item label="难度" prop="difficulty">
            <el-select
              v-model="questionForm.difficulty"
              placeholder="请选择难度"
            >
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
          </el-form-item>

          <el-form-item label="章节" prop="chapter">
            <el-input
              v-model="questionForm.chapter"
              placeholder="请输入章节"
            />
          </el-form-item>

          <el-form-item label="知识点" prop="topic">
            <el-input
              v-model="questionForm.topic"
              placeholder="请输入知识点"
            />
          </el-form-item>
        </div>

        <el-form-item label="题目内容" prop="content">
          <el-input
            v-model="questionForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入题目内容"
          />
        </el-form-item>

        <el-form-item
          v-if="questionForm.type === 'choice'"
          label="选项"
          prop="options"
        >
          <div class="option-list">
            <div
              v-for="(_, index) in questionForm.options"
              :key="index"
              class="option-row"
            >
              <el-input
                v-model="questionForm.options[index]"
                :placeholder="`选项 ${index + 1}`"
              />
              <el-button
                v-if="questionForm.options.length > 2"
                link
                type="danger"
                @click="removeOption(index)"
              >
                删除
              </el-button>
            </div>

            <el-button
              link
              type="primary"
              @click="addOption"
            >
              添加选项
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="参考答案" prop="answer">
          <el-input
            v-model="questionForm.answer"
            type="textarea"
            :rows="questionForm.type === 'code' ? 4 : 2"
            :placeholder="
              questionForm.type === 'code'
                ? '请输入代码题期望输出'
                : '请输入参考答案'
            "
          />
        </el-form-item>

        <el-form-item label="题目解析" prop="explanation">
          <el-input
            v-model="questionForm.explanation"
            type="textarea"
            :rows="3"
            placeholder="请输入题目解析"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="questionDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="questionSubmitting"
          @click="submitQuestion"
        >
          确认
        </el-button>
      </template>
    </el-dialog>

        <el-dialog
      v-model="aiDialogVisible"
      title="AI 出题"
      width="780px"
    >
      <el-alert
        title="AI生成内容仅作为草稿，确认保存后才会进入正式题库"
        type="info"
        :closable="false"
        show-icon
        class="ai-alert"
      />

      <el-form :model="aiForm" label-width="76px">
        <div class="form-grid">
          <el-form-item label="章节">
            <el-input
              v-model="aiForm.chapter"
              placeholder="请输入章节"
            />
          </el-form-item>

          <el-form-item label="知识点">
            <el-input
              v-model="aiForm.topic"
              placeholder="请输入知识点"
            />
          </el-form-item>

          <el-form-item label="题型">
            <el-select v-model="aiForm.type">
              <el-option label="选择题" value="choice" />
              <el-option label="填空题" value="blank" />
              <el-option label="代码题" value="code" />
            </el-select>
          </el-form-item>

          <el-form-item label="难度">
            <el-select v-model="aiForm.difficulty">
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
          </el-form-item>

          <el-form-item label="数量">
            <el-input-number
              v-model="aiForm.count"
              :min="1"
              :max="20"
              :precision="0"
            />
          </el-form-item>
        </div>
      </el-form>

      <div class="ai-generate-action">
        <el-button
          type="primary"
          :loading="aiGenerating"
          @click="generateDrafts"
        >
          生成草稿
        </el-button>
      </div>

      <div v-if="aiHasGenerated" class="draft-section">
        <div class="draft-heading">
          <h3>生成结果</h3>
          <span>共 {{ aiDrafts.length }} 道</span>
        </div>

        <el-empty
          v-if="aiDrafts.length === 0"
          description="本次没有生成可用草稿"
        />

        <div v-else class="draft-list">
          <article
            v-for="(draft, index) in aiDrafts"
            :key="`${draft.id || 'draft'}-${index}`"
            class="draft-card"
          >
            <div class="draft-meta">
              <el-tag>
                {{ typeLabels[draft.type] || draft.type }}
              </el-tag>
              <el-tag :type="difficultyTagTypes[draft.difficulty]">
                {{
                  difficultyLabels[draft.difficulty] ||
                  draft.difficulty
                }}
              </el-tag>
              <span>{{ draft.chapter || '未设置章节' }}</span>
              <span>{{ draft.topic || '未设置知识点' }}</span>
            </div>

            <p class="draft-content">
              {{ draft.content }}
            </p>

            <div class="draft-footer">
              <span>参考答案：{{ draft.answer || '—' }}</span>
              <el-button
                type="primary"
                :loading="savingDraftIndex === index"
                @click="saveDraft(draft, index)"
              >
                保存到题库
              </el-button>
            </div>
          </article>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules, TagProps } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTeacherQuestion,
  deleteTeacherQuestion,
  generateTeacherQuestions,
  getTeacherQuestion,
  getTeacherQuestions,
  updateTeacherQuestion
} from '@/api/teacher'
import type {
  CreateQuestionRequest,
  QuestionDifficulty,
  QuestionGenerateRequest,
  QuestionQuery,
  QuestionType,
  TeacherQuestion
} from '@/api/teacher'

interface QuestionFormState {
  type: QuestionType
  difficulty: QuestionDifficulty
  chapter: string
  topic: string
  content: string
  options: string[]
  answer: string
  explanation: string
}

const typeLabels: Record<QuestionType, string> = {
  choice: '选择题',
  blank: '填空题',
  code: '代码题'
}

const difficultyLabels: Record<QuestionDifficulty, string> = {
  easy: '简单',
  medium: '中等',
  hard: '困难'
}

const difficultyTagTypes: Record<
  QuestionDifficulty,
  TagProps['type']
> = {
  easy: 'success',
  medium: 'warning',
  hard: 'danger'
}

function getQuestionTypeLabel(value: unknown): string {
  if (typeof value !== 'string') {
    return '未知题型'
  }

  return typeLabels[value as QuestionType] ?? value
}

function getQuestionDifficultyLabel(
  value: unknown
): string {
  if (typeof value !== 'string') {
    return '未知难度'
  }

  return (
    difficultyLabels[value as QuestionDifficulty] ??
    value
  )
}

function getQuestionDifficultyTagType(
  value: unknown
) {
  if (typeof value !== 'string') {
    return undefined
  }

  return difficultyTagTypes[
    value as QuestionDifficulty
  ]
}

const questions = ref<TeacherQuestion[]>([])
const loading = ref(false)
const loadError = ref(false)

const filters = reactive<QuestionQuery>({
  chapter: undefined,
  topic: undefined,
  type: undefined,
  difficulty: undefined
})

const questionDialogVisible = ref(false)
const aiDialogVisible = ref(false)
const aiGenerating = ref(false)
const aiHasGenerated = ref(false)
const aiDrafts = ref<TeacherQuestion[]>([])
const savingDraftIndex = ref<number | null>(null)

const aiForm = reactive<QuestionGenerateRequest>({
  chapter: '',
  topic: '',
  type: 'choice',
  difficulty: 'medium',
  count: 5
})
const questionSubmitting = ref(false)
const detailLoading = ref(false)
const editingQuestionId = ref<number | null>(null)
const questionFormRef = ref<FormInstance>()

const questionForm = reactive<QuestionFormState>({
  type: 'choice',
  difficulty: 'medium',
  chapter: '',
  topic: '',
  content: '',
  options: ['', '', '', ''],
  answer: '',
  explanation: ''
})

const questionRules: FormRules = {
  type: [
    {
      required: true,
      message: '请选择题型',
      trigger: 'change'
    }
  ],
  content: [
    {
      required: true,
      message: '请输入题目内容',
      trigger: 'blur'
    }
  ]
}

function cleanText(value?: string) {
  const result = value?.trim()
  return result || undefined
}

async function loadQuestions() {
  loading.value = true
  loadError.value = false

  try {
    questions.value = await getTeacherQuestions({
      chapter: cleanText(filters.chapter),
      topic: cleanText(filters.topic),
      type: filters.type,
      difficulty: filters.difficulty
    })
  } catch {
    questions.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.chapter = undefined
  filters.topic = undefined
  filters.type = undefined
  filters.difficulty = undefined
  loadQuestions()
}

function openCreateDialog() {
  editingQuestionId.value = null
  resetQuestionForm()
  questionDialogVisible.value = true
}

async function openEditDialog(questionId: number) {
  editingQuestionId.value = questionId
  detailLoading.value = true

  try {
    const detail = await getTeacherQuestion(questionId)

    questionForm.type = detail.type
    questionForm.difficulty = detail.difficulty || 'medium'
    questionForm.chapter = detail.chapter || ''
    questionForm.topic = detail.topic || ''
    questionForm.content = detail.content || ''
    questionForm.options =
      detail.options?.length > 0
        ? [...detail.options]
        : ['', '', '', '']
    questionForm.answer = detail.answer || ''
    questionForm.explanation = detail.explanation || ''
    questionDialogVisible.value = true
  } catch {
    editingQuestionId.value = null
  } finally {
    detailLoading.value = false
  }
}

function resetQuestionForm() {
  editingQuestionId.value = null
  questionForm.type = 'choice'
  questionForm.difficulty = 'medium'
  questionForm.chapter = ''
  questionForm.topic = ''
  questionForm.content = ''
  questionForm.options = ['', '', '', '']
  questionForm.answer = ''
  questionForm.explanation = ''
  questionFormRef.value?.clearValidate()
}

function handleTypeChange(type: QuestionType) {
  if (type === 'choice' && questionForm.options.length < 2) {
    questionForm.options = ['', '', '', '']
  }
}

function addOption() {
  questionForm.options.push('')
}

function removeOption(index: number) {
  questionForm.options.splice(index, 1)
}

function buildQuestionRequest(): CreateQuestionRequest {
  return {
    type: questionForm.type,
    difficulty: questionForm.difficulty,
    chapter: cleanText(questionForm.chapter),
    topic: cleanText(questionForm.topic),
    content: questionForm.content.trim(),
    options:
      questionForm.type === 'choice'
        ? questionForm.options
            .map(item => item.trim())
            .filter(Boolean)
        : undefined,
    answer: cleanText(questionForm.answer),
    explanation: cleanText(questionForm.explanation)
  }
}

async function submitQuestion() {
  const valid = await questionFormRef.value
    ?.validate()
    .catch(() => false)

  if (!valid) {
    return
  }

  questionSubmitting.value = true

  try {
    const data = buildQuestionRequest()

    if (editingQuestionId.value === null) {
      await createTeacherQuestion(data)
      ElMessage.success('题目创建成功')
    } else {
      await updateTeacherQuestion(
        editingQuestionId.value,
        data
      )
      ElMessage.success('题目更新成功')
    }

    questionDialogVisible.value = false
    await loadQuestions()
  } finally {
    questionSubmitting.value = false
  }
}

async function confirmDelete(item: TeacherQuestion) {
  try {
    await ElMessageBox.confirm(
      '确定删除该题目吗？',
      '删除题目',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  await deleteTeacherQuestion(item.id)
  ElMessage.success('题目删除成功')
  await loadQuestions()
}
async function generateDrafts() {
  aiGenerating.value = true
  aiHasGenerated.value = false

  try {
    aiDrafts.value = await generateTeacherQuestions({
      chapter: cleanText(aiForm.chapter),
      topic: cleanText(aiForm.topic),
      type: aiForm.type,
      difficulty: aiForm.difficulty,
      count: aiForm.count
    })
    aiHasGenerated.value = true
  } catch {
    aiDrafts.value = []
  } finally {
    aiGenerating.value = false
  }
}

async function saveDraft(
  draft: TeacherQuestion,
  index: number
) {
  savingDraftIndex.value = index

  try {
    await createTeacherQuestion({
      type: draft.type,
      chapter: cleanText(draft.chapter),
      topic: cleanText(draft.topic),
      content: draft.content,
      options:
        draft.type === 'choice'
          ? draft.options
          : undefined,
      answer: cleanText(draft.answer),
      explanation: cleanText(draft.explanation),
      difficulty: draft.difficulty
    })

    aiDrafts.value.splice(index, 1)
    ElMessage.success('草稿已保存到题库')
    await loadQuestions()
  } finally {
    savingDraftIndex.value = null
  }
}
function formatDateTime(value?: string) {
  if (!value) {
    return '—'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('zh-CN', {
    hour12: false
  })
}

onMounted(loadQuestions)
</script>

<style scoped>
.page-container {
  min-height: 100%;
  padding: var(--space-xxl);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.eyebrow {
  margin: 0 0 var(--space-xs);
  color: var(--primary);
  font: var(--text-caption);
  letter-spacing: 0.08em;
}

.page-header h1 {
  margin-bottom: var(--space-xs);
}

.description {
  margin: 0;
  color: var(--muted);
  font: var(--text-body);
}

.header-actions {
  display: flex;
  gap: var(--space-sm);
}

.content-card {
  min-height: 460px;
}

.filter-grid {
  display: grid;
  grid-template-columns:
    minmax(140px, 1fr)
    minmax(140px, 1fr)
    130px
    130px
    auto
    auto;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
}

.question-content {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: var(--space-md);
}

.option-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--space-xs);
}

.option-row {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.state-container {
  display: flex;
  min-height: 340px;
  align-items: center;
  justify-content: center;
}
.ai-alert {
  margin-bottom: var(--space-lg);
}

.ai-generate-action {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--space-lg);
}

.draft-section {
  padding-top: var(--space-lg);
  border-top: 1px solid var(--hairline);
}

.draft-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}

.draft-heading h3 {
  margin: 0;
}

.draft-heading span {
  color: var(--muted);
  font: var(--text-sm);
}

.draft-list {
  display: flex;
  max-height: 420px;
  flex-direction: column;
  gap: var(--space-sm);
  overflow-y: auto;
}

.draft-card {
  padding: var(--space-md);
  background: var(--surface-soft);
  border: 1px solid var(--hairline);
  border-radius: var(--radius-md);
}

.draft-meta {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--muted);
  font: var(--text-sm);
}

.draft-content {
  margin: var(--space-sm) 0;
  color: var(--ink);
  font: var(--text-body-medium);
}

.draft-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  color: var(--muted);
  font: var(--text-sm);
}
:deep(.question-dialog) {
  max-height: 90vh;
  margin-top: 5vh !important;
  overflow: hidden;
}

:deep(.question-dialog .el-dialog__body) {
  max-height: calc(90vh - 130px);
  overflow-y: auto;
}
@media (max-width: 980px) {
  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 700px) {
  .page-container {
    padding: var(--space-lg);
  }

  .page-header {
    flex-direction: column;
  }

  .form-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>