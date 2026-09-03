<template>
  <div class="ai-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">AI TEACHING ASSISTANT</p>
        <h1>AI 助教</h1>
        <p class="description">
          开展教学答疑，并根据正式作业数据生成成绩解读。
        </p>
      </div>
        </header>

    <el-alert
      v-if="optionError"
      :title="optionError"
      type="warning"
      show-icon
      :closable="false"
      class="option-alert"
    >
      <template #default>
        <el-button
          link
          type="primary"
          @click="loadOptions"
        >
          重新加载选项
        </el-button>
      </template>
    </el-alert>

    <el-tabs
      v-model="activeTab"
      class="assistant-tabs"
    >
      <el-tab-pane
        label="教学答疑"
        name="question"
      >
        <section class="workspace-grid">
          <article class="panel form-panel">
            <div class="panel-heading">
              <div>
                <h2>向 AI 提问</h2>
                <p>
                  可选择班级作为问答范围，问题内容为必填项。
                </p>
              </div>
            </div>

            <el-form label-position="top">
              <el-form-item label="关联班级（可选）">
                <el-select
                  v-model="askForm.classId"
                  clearable
                  filterable
                  placeholder="不指定班级"
                  :loading="loadingOptions"
                >
                  <el-option
                    v-for="item in classes"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item
                label="教学问题"
                required
              >
                <el-input
                  v-model="askForm.message"
                  type="textarea"
                  :rows="8"
                  resize="vertical"
                  placeholder="请输入需要 AI 协助解答的教学问题"
                />
              </el-form-item>

              <div class="form-actions">
                <el-button
                  :disabled="asking"
                  @click="resetQuestion"
                >
                  清空
                </el-button>

                <el-button
                  type="primary"
                  :loading="asking"
                  :disabled="!askForm.message.trim()"
                  @click="submitQuestion"
                >
                  提交问题
                </el-button>
              </div>
            </el-form>
          </article>

          <article class="panel result-panel">
            <div class="panel-heading">
              <div>
                <h2>AI 回答</h2>
                <p>展示教师服务返回的正式回答。</p>
              </div>
            </div>

            <el-result
              v-if="askError"
              icon="warning"
              title="AI 回答生成失败"
              :sub-title="askError"
            />

            <el-skeleton
              v-else-if="asking"
              :rows="7"
              animated
            />

            <div
              v-else-if="aiAnswer"
              class="answer-content"
            >
              <div class="answer-meta">
                <span>意图</span>
                <el-tag>
                  {{ aiAnswer.intent || '未提供' }}
                </el-tag>
              </div>

              <div class="answer-text">
                {{ aiAnswer.answer || '未返回回答内容' }}
              </div>

              <div
                v-if="formattedReferences"
                class="reference-section"
              >
                <h3>参考信息</h3>
                <pre>{{ formattedReferences }}</pre>
              </div>
            </div>

            <el-empty
              v-else
              description="提交教学问题后查看 AI 回答"
            />
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane
        label="成绩解读"
        name="grade"
      >
        <section class="workspace-grid">
          <article class="panel form-panel">
            <div class="panel-heading">
              <div>
                <h2>生成成绩解读</h2>
                <p>
                  选择正式作业并填写学生 ID。
                </p>
              </div>
            </div>

            <el-form label-position="top">
              <el-form-item label="班级筛选（可选）">
                <el-select
                  v-model="gradeClassId"
                  clearable
                  filterable
                  placeholder="全部班级"
                  :loading="loadingOptions"
                  @change="handleGradeClassChange"
                >
                  <el-option
                    v-for="item in classes"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item
                label="作业"
                required
              >
                <el-select
                  v-model="explainForm.assignmentId"
                  filterable
                  placeholder="请选择作业"
                  :loading="loadingAssignments"
                >
                  <el-option
                    v-for="item in assignments"
                    :key="item.id"
                    :label="item.title"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item
                label="学生 ID"
                required
              >
                <el-input-number
                  v-model="explainForm.studentId"
                  :min="1"
                  :controls="false"
                  placeholder="请输入学生 ID"
                  class="student-input"
                />
              </el-form-item>

              <div class="form-actions">
                <el-button
                  :disabled="explaining"
                  @click="resetExplanation"
                >
                  清空
                </el-button>

                <el-button
                  type="primary"
                  :loading="explaining"
                  :disabled="!canExplainGrade"
                  @click="submitExplanation"
                >
                  生成解读
                </el-button>
              </div>
            </el-form>
          </article>

          <article class="panel result-panel">
            <div class="panel-heading">
              <div>
                <h2>成绩解读结果</h2>
                <p>
                  返回结构以正式教师服务响应为准。
                </p>
              </div>
            </div>

            <el-result
              v-if="explanationError"
              icon="warning"
              title="成绩解读生成失败"
              :sub-title="explanationError"
            />

            <el-skeleton
              v-else-if="explaining"
              :rows="7"
              animated
            />

            <div
              v-else-if="formattedExplanation"
              class="explanation-content"
            >
              <pre>{{ formattedExplanation }}</pre>
            </div>

            <el-empty
              v-else
              description="选择作业和学生后生成成绩解读"
            />
          </article>
        </section>
      </el-tab-pane>
    </el-tabs>
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
  askTeacherAi,
  explainTeacherGrade,
  getTeacherAssignments,
  getTeacherClasses
} from '@/api/teacher'
import type {
  GradeExplanation,
  TeacherAiAnswer,
  TeacherAiAskRequest,
  TeacherAssignment,
  TeacherClass
} from '@/api/teacher'

const activeTab = ref('question')

const classes = ref<TeacherClass[]>([])
const assignments = ref<TeacherAssignment[]>([])
const loadingOptions = ref(false)
const loadingAssignments = ref(false)
const optionError = ref('')

const askForm = reactive<{
  message: string
  classId?: number
}>({
  message: ''
})

const asking = ref(false)
const askError = ref('')
const aiAnswer = ref<TeacherAiAnswer>()

const gradeClassId = ref<number>()
const explainForm = reactive<{
  assignmentId?: number
  studentId?: number
}>({})

const explaining = ref(false)
const explanationError = ref('')
const gradeExplanation = ref<GradeExplanation>()

const formattedReferences = computed(() => {
  const references = aiAnswer.value?.references

  if (!references) return ''

  return JSON.stringify(references, null, 2)
})

const formattedExplanation = computed(() => {
  if (
    gradeExplanation.value === undefined ||
    gradeExplanation.value === null
  ) {
    return ''
  }

  return JSON.stringify(
    gradeExplanation.value,
    null,
    2
  )
})

const canExplainGrade = computed(
  () =>
    explainForm.assignmentId !== undefined &&
    explainForm.studentId !== undefined
)

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

async function loadOptions() {
  loadingOptions.value = true
  optionError.value = ''

  try {
    const [classResult, assignmentResult] =
      await Promise.all([
        getTeacherClasses(),
        getTeacherAssignments()
      ])

    classes.value = classResult
    assignments.value = assignmentResult
  } catch (error) {
    classes.value = []
    assignments.value = []
    optionError.value = getErrorMessage(
      error,
      '班级或作业选项加载失败'
    )
  } finally {
    loadingOptions.value = false
  }
}

async function handleGradeClassChange() {
  loadingAssignments.value = true
  optionError.value = ''
  explainForm.assignmentId = undefined

  try {
    assignments.value =
      await getTeacherAssignments(
        gradeClassId.value
      )
  } catch (error) {
    assignments.value = []
    optionError.value = getErrorMessage(
      error,
      '作业列表加载失败'
    )
  } finally {
    loadingAssignments.value = false
  }
}

async function submitQuestion() {
  const message = askForm.message.trim()

  if (!message) return

  asking.value = true
  askError.value = ''
  aiAnswer.value = undefined

  const data: TeacherAiAskRequest = {
    message
  }

  if (askForm.classId !== undefined) {
    data.classId = askForm.classId
  }

  try {
    aiAnswer.value = await askTeacherAi(data)
  } catch (error) {
    askError.value = getErrorMessage(
      error,
      '请检查教师服务或 AI 服务是否可用'
    )
  } finally {
    asking.value = false
  }
}

function resetQuestion() {
  askForm.message = ''
  askForm.classId = undefined
  askError.value = ''
  aiAnswer.value = undefined
}

async function submitExplanation() {
  const assignmentId =
    explainForm.assignmentId
  const studentId = explainForm.studentId

  if (
    assignmentId === undefined ||
    studentId === undefined
  ) {
    return
  }

  explaining.value = true
  explanationError.value = ''
  gradeExplanation.value = undefined

  try {
    gradeExplanation.value =
      await explainTeacherGrade({
        studentId,
        assignmentId
      })
  } catch (error) {
    explanationError.value = getErrorMessage(
      error,
      '请检查教师服务或 AI 服务是否可用'
    )
  } finally {
    explaining.value = false
  }
}

function resetExplanation() {
  gradeClassId.value = undefined
  explainForm.assignmentId = undefined
  explainForm.studentId = undefined
  explanationError.value = ''
  gradeExplanation.value = undefined
  handleGradeClassChange()
}

onMounted(() => {
  loadOptions()
})
</script>
<style scoped>
.ai-page {
  min-height: calc(100vh - 72px);
  padding: 34px 46px 48px;
  background: #f7f6f4;
  color: #181818;
}

.page-header {
  margin-bottom: 24px;
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
  font-weight: 700;
}

.description {
  margin: 10px 0 0;
  color: #aaa5a0;
  font-size: 16px;
  line-height: 1.7;
}

.option-alert {
  margin-bottom: 18px;
  border-radius: 8px;
}

.assistant-tabs {
  padding: 0 24px 28px;
  border: 1px solid #dedbd7;
  border-radius: 12px;
  background: #fff;
}

.assistant-tabs :deep(.el-tabs__header) {
  margin-bottom: 26px;
}

.assistant-tabs :deep(.el-tabs__item) {
  height: 58px;
  padding: 0 26px;
  font-size: 16px;
}

.assistant-tabs :deep(.el-tabs__item.is-active) {
  color: #5140df;
  font-weight: 600;
}

.assistant-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
  background: #5140df;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.85fr)
    minmax(420px, 1.15fr);
  gap: 22px;
  align-items: stretch;
}

.panel {
  min-width: 0;
  min-height: 470px;
  padding: 24px;
  border: 1px solid #e5e2de;
  border-radius: 10px;
  background: #fff;
}

.form-panel {
  background: #faf9f8;
}

.result-panel {
  display: flex;
  flex-direction: column;
}

.panel-heading {
  margin-bottom: 22px;
}

.panel-heading h2 {
  margin: 0 0 7px;
  font-size: 21px;
  line-height: 1.4;
}

.panel-heading p {
  margin: 0;
  color: #aaa5a0;
  font-size: 14px;
  line-height: 1.6;
}

.form-panel :deep(.el-form-item) {
  margin-bottom: 22px;
}

.form-panel :deep(.el-form-item__label) {
  color: #5b5753;
  font-weight: 500;
}

.form-panel :deep(.el-select),
.form-panel :deep(.el-input),
.form-panel :deep(.el-textarea) {
  width: 100%;
}

.form-panel :deep(.el-select__wrapper),
.form-panel :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 8px;
}

.form-panel :deep(.el-textarea__inner) {
  min-height: 150px !important;
  padding: 14px;
  border-radius: 8px;
  line-height: 1.7;
  resize: vertical;
}

.student-input {
  width: 100%;
}

.student-input :deep(.el-input__wrapper) {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

.form-actions :deep(.el-button) {
  min-width: 92px;
  height: 42px;
  border-radius: 8px;
}

.form-actions :deep(.el-button--primary) {
  border-color: #5140df;
  background: #5140df;
}

.form-actions :deep(.el-button--primary:hover) {
  border-color: #6253e8;
  background: #6253e8;
}

.answer-content,
.explanation-content {
  flex: 1;
  padding: 20px;
  border: 1px solid #e6e3df;
  border-radius: 9px;
  background: #faf9f8;
}

.answer-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e4e1dd;
}

.answer-text {
  margin-top: 18px;
  color: #35312e;
  font-size: 15px;
  line-height: 1.85;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.reference-section {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #e4e1dd;
}

.reference-section h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.reference-section pre,
.explanation-content pre {
  max-height: 360px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  border-radius: 8px;
  background: #f0efed;
  color: #4b4743;
  font-family:
    Consolas, "Courier New", monospace;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.result-panel :deep(.el-empty) {
  flex: 1;
  justify-content: center;
}

.result-panel :deep(.el-alert) {
  margin-bottom: 16px;
}

.result-panel :deep(.el-skeleton) {
  padding-top: 8px;
}

@media (max-width: 1100px) {
  .ai-page {
    padding: 28px 28px 40px;
  }

  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .panel {
    min-height: auto;
  }

  .result-panel {
    min-height: 400px;
  }
}

@media (max-width: 700px) {
  .ai-page {
    padding: 22px 16px 32px;
  }

  .page-header h1 {
    font-size: 34px;
  }

  .assistant-tabs {
    padding: 0 15px 20px;
  }

  .assistant-tabs :deep(.el-tabs__item) {
    padding: 0 15px;
  }

  .panel {
    padding: 18px;
  }

  .form-actions {
    flex-wrap: wrap;
  }

  .form-actions :deep(.el-button) {
    flex: 1;
    margin-left: 0;
  }
}
</style>