<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">ASSIGNMENT MANAGEMENT</p>
        <h1>作业管理</h1>
        <p class="description">
          创建、维护并发布班级作业。
        </p>
      </div>

      <el-button type="primary" @click="openCreate">
        新建作业
      </el-button>
    </header>

    <section class="content-card">
      <div class="toolbar">
        <el-select
          v-model="selectedClassId"
          clearable
          placeholder="全部班级"
          class="class-filter"
          @change="loadAssignments"
        >
          <el-option
            v-for="item in classes"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>

        <el-button @click="loadAll">刷新</el-button>
      </div>

      <div v-loading="loading">
        <el-result
          v-if="loadError"
          icon="warning"
          title="作业加载失败"
          sub-title="请检查教师服务或网关是否可用"
        >
          <template #extra>
            <el-button type="primary" @click="loadAll">
              重新加载
            </el-button>
          </template>
        </el-result>

        <el-empty
          v-else-if="!assignments.length"
          description="暂无作业"
        />

        <el-table
          v-else
          :data="assignments"
          row-key="id"
        >
          <el-table-column
            prop="title"
            label="作业名称"
            min-width="200"
          />

          <el-table-column label="所属班级" min-width="150">
            <template #default="{ row }">
              {{ getClassName(row.classId) }}
            </template>
          </el-table-column>

          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag>
                {{ assignmentTypeText[row.type] }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            prop="itemCount"
            label="题目数"
            width="90"
          />

          <el-table-column label="总分" width="90">
            <template #default="{ row }">
              {{ row.totalScore ?? 0 }}
            </template>
          </el-table-column>

          <el-table-column label="截止时间" min-width="180">
            <template #default="{ row }">
              {{ formatTime(row.deadline) }}
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 1 ? 'success' : 'info'"
              >
                {{ row.status === 1 ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>

                   <el-table-column
            label="操作"
            width="280"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openDetail(row)"
              >
                详情
              </el-button>
              <el-button
                link
                type="primary"
                @click="openEdit(row)"
              >
                编辑
              </el-button>

              <el-button
                link
                type="success"
                @click="handlePublish(row)"
              >
                {{ row.status === 1 ? '重新发布' : '发布' }}
              </el-button>

              <el-button
                link
                type="danger"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog
      v-model="createVisible"
      title="新建作业"
      width="900px"
      class="assignment-dialog"
      destroy-on-close
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="90px"
      >
        <div class="form-grid">
          <el-form-item label="所属班级" prop="classId">
            <el-select
              v-model="createForm.classId"
              placeholder="请选择班级"
              style="width: 100%"
            >
              <el-option
                v-for="item in classes"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="作业类型" prop="type">
            <el-select
              v-model="createForm.type"
              style="width: 100%"
            >
              <el-option label="普通作业" value="homework" />
              <el-option label="代码作业" value="code" />
            </el-select>
          </el-form-item>

          <el-form-item label="作业名称" prop="title">
            <el-input
              v-model="createForm.title"
              maxlength="128"
              show-word-limit
              placeholder="请输入作业名称"
            />
          </el-form-item>

          <el-form-item label="截止时间">
            <el-date-picker
              v-model="createForm.deadline"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="请选择截止时间"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <el-form-item label="作业说明">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入作业说明"
          />
        </el-form-item>

        <el-form-item label="选择题目" prop="questionIds">
          <div class="question-area">
            <el-alert
              title="至少选择一道题，并为每道题设置分值"
              type="info"
              :closable="false"
              show-icon
            />

            <el-table
              ref="questionTableRef"
              :data="availableQuestions"
              row-key="id"
              max-height="330"
              @selection-change="handleQuestionSelection"
            >
              <el-table-column
                type="selection"
                width="50"
                :selectable="questionSelectable"
              />

              <el-table-column
                prop="content"
                label="题目内容"
                min-width="260"
                show-overflow-tooltip
              />

              <el-table-column label="题型" width="100">
                <template #default="{ row }">
                  {{ questionTypeText[row.type] }}
                </template>
              </el-table-column>

              <el-table-column
                prop="chapter"
                label="章节"
                width="130"
                show-overflow-tooltip
              />

              <el-table-column label="分值" width="150">
                <template #default="{ row }">
                  <el-input-number
                    v-model="questionScores[row.id]"
                    :min="1"
                    :max="100"
                    :disabled="!selectedQuestionIds.includes(row.id)"
                    controls-position="right"
                  />
                </template>
              </el-table-column>
            </el-table>

            <el-empty
              v-if="!availableQuestions.length"
              description="题库暂无符合该作业类型的题目"
            />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="submitCreate"
        >
          确认创建
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="editVisible"
      title="编辑作业"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="90px"
      >
        <el-form-item label="作业名称" prop="title">
          <el-input
            v-model="editForm.title"
            maxlength="128"
            show-word-limit
            placeholder="请输入作业名称"
          />
        </el-form-item>

        <el-form-item label="截止时间">
          <el-date-picker
            v-model="editForm.deadline"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择截止时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="作业状态" prop="status">
          <el-select
            v-model="editForm.status"
            style="width: 100%"
          >
            <el-option label="草稿" value="0" />
            <el-option label="已发布" value="1" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="editSubmitting"
          @click="submitEdit"
        >
          保存修改
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="addItemVisible"
      title="追加题目"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="addItemFormRef"
        :model="addItemForm"
        :rules="addItemRules"
        label-width="90px"
      >
        <el-form-item label="选择题目" prop="questionId">
          <el-select
            v-model="addItemForm.questionId"
            filterable
            placeholder="请选择尚未加入作业的题目"
            style="width: 100%"
          >
            <el-option
              v-for="item in additionalQuestions"
              :key="item.id"
              :label="item.content"
              :value="item.id"
            >
              <div class="question-option">
                <span>{{ item.content }}</span>
                <el-tag size="small">
                  {{ questionTypeText[item.type] }}
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="题目分值" prop="score">
          <el-input-number
            v-model="addItemForm.score"
            :min="1"
            :max="100"
            controls-position="right"
          />
        </el-form-item>

        <el-alert
          v-if="!additionalQuestions.length"
          title="没有可追加的题目"
          description="当前题库中没有符合该作业类型且尚未加入的题目"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>

      <template #footer>
        <el-button @click="addItemVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="addItemSubmitting"
          :disabled="!additionalQuestions.length"
          @click="submitAddItem"
        >
          确认添加
        </el-button>
      </template>
    </el-dialog>
    <el-drawer
      v-model="detailVisible"
      title="作业详情"
      size="720px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <el-result
          v-if="detailError"
          icon="warning"
          title="作业详情加载失败"
        >
          <template #extra>
            <el-button
              v-if="currentAssignment"
              type="primary"
              @click="loadDetail(currentAssignment.id)"
            >
              重新加载
            </el-button>
          </template>
        </el-result>

        <template v-else-if="assignmentDetail">
          <el-descriptions
            :column="2"
            border
            class="detail-descriptions"
          >
            <el-descriptions-item label="作业名称">
              {{ assignmentDetail.title }}
            </el-descriptions-item>

            <el-descriptions-item label="所属班级">
              {{ getClassName(assignmentDetail.classId) }}
            </el-descriptions-item>

            <el-descriptions-item label="作业类型">
              {{ getAssignmentTypeText(assignmentDetail.type) }}
            </el-descriptions-item>

            <el-descriptions-item label="状态">
              <el-tag
                :type="
                  assignmentDetail.status === 1
                    ? 'success'
                    : 'info'
                "
              >
                {{
                  assignmentDetail.status === 1
                    ? '已发布'
                    : '草稿'
                }}
              </el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="截止时间">
              {{ formatTime(assignmentDetail.deadline) }}
            </el-descriptions-item>

            <el-descriptions-item label="创建时间">
              {{ formatTime(assignmentDetail.createTime) }}
            </el-descriptions-item>

            <el-descriptions-item
              label="作业说明"
              :span="2"
            >
              {{ assignmentDetail.description || '无' }}
            </el-descriptions-item>
          </el-descriptions>

                    <div class="detail-heading">
            <div>
              <h3>作业题目</h3>
              <p>
                共 {{ assignmentDetail.items.length }} 道题
              </p>
            </div>

            <el-button
              type="primary"
              @click="openAddItem"
            >
              追加题目
            </el-button>
          </div>

          <el-empty
            v-if="!assignmentDetail.items.length"
            description="该作业暂无题目"
          />

          <div v-else class="assignment-items">
            <article
              v-for="(item, index) in assignmentDetail.items"
              :key="item.itemId"
              class="assignment-item"
            >
              <div class="item-header">
                <div>
                  <span class="item-index">
                    第 {{ index + 1 }} 题
                  </span>
                  <el-tag size="small">
                    {{ questionTypeText[item.question.type] }}
                  </el-tag>
                </div>

                <strong>{{ item.score }} 分</strong>
              </div>

              <p class="item-content">
                {{ item.question.content }}
              </p>

              <div class="item-meta">
                <span>
                  章节：{{ item.question.chapter || '未设置' }}
                </span>
                <span>
                  知识点：{{ item.question.topic || '未设置' }}
                </span>
                <span>
                  已提交：{{ item.submittedCount }}
                </span>
                <span>
                  已批改：{{ item.gradedCount }}
                </span>
              </div>
            </article>
          </div>
        </template>
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
import type {
  FormInstance,
  FormRules,
  TableInstance
} from 'element-plus'
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import {
  addTeacherAssignmentItem,
  createTeacherAssignment,
  deleteTeacherAssignment,
  getTeacherAssignment,
  getTeacherAssignments,
  getTeacherClasses,
  getTeacherQuestions,
  publishTeacherAssignment,
  updateTeacherAssignment
} from '@/api/teacher'
import type {
  AssignmentType,
  TeacherAssignment,
  TeacherAssignmentDetail,
  TeacherClass,
  TeacherQuestion
} from '@/api/teacher'
interface AddItemFormModel {
  questionId?: number
  score: number
}
interface EditFormModel {
  title: string
  deadline: string | null
  status: '0' | '1'
}
interface CreateFormModel {
  classId?: number
  title: string
  type: AssignmentType
  deadline: string | null
  description: string
  questionIds: number[]
}

const loading = ref(false)
const submitting = ref(false)
const loadError = ref(false)
const createVisible = ref(false)
const editVisible = ref(false)
const editSubmitting = ref(false)
const addItemVisible = ref(false)
const addItemSubmitting = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref(false)
const selectedClassId = ref<number>()

const classes = ref<TeacherClass[]>([])
const questions = ref<TeacherQuestion[]>([])
const assignments = ref<TeacherAssignment[]>([])
const currentAssignment = ref<TeacherAssignment>()
const assignmentDetail = ref<TeacherAssignmentDetail>()

const createFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()
const addItemFormRef = ref<FormInstance>()
const questionTableRef = ref<TableInstance>()
const selectedQuestionIds = ref<number[]>([])
const questionScores = reactive<Record<number, number>>({})

const assignmentTypeText: Record<AssignmentType, string> = {
  homework: '普通作业',
  code: '代码作业'
}

const questionTypeText = {
  choice: '选择题',
  blank: '填空题',
  code: '代码题'
} as const

const createForm = reactive<CreateFormModel>({
  classId: undefined,
  title: '',
  type: 'homework',
  deadline: null,
  description: '',
  questionIds: []
})
const editForm = reactive<EditFormModel>({
  title: '',
  deadline: null,
  status: '0'
})

const editRules: FormRules<EditFormModel> = {
  title: [
    {
      required: true,
      message: '请输入作业名称',
      trigger: 'blur'
    }
  ],
  status: [
    {
      required: true,
      message: '请选择作业状态',
      trigger: 'change'
    }
  ]
}
const addItemForm = reactive<AddItemFormModel>({
  questionId: undefined,
  score: 10
})

const addItemRules: FormRules<AddItemFormModel> = {
  questionId: [
    {
      required: true,
      message: '请选择需要追加的题目',
      trigger: 'change'
    }
  ],
  score: [
    {
      required: true,
      message: '请设置题目分值',
      trigger: 'change'
    }
  ]
}

const createRules: FormRules<CreateFormModel> = {
  classId: [
    {
      required: true,
      message: '请选择所属班级',
      trigger: 'change'
    }
  ],
  title: [
    {
      required: true,
      message: '请输入作业名称',
      trigger: 'blur'
    }
  ],
  type: [
    {
      required: true,
      message: '请选择作业类型',
      trigger: 'change'
    }
  ],
  questionIds: [
    {
      type: 'array',
      required: true,
      min: 1,
      message: '请至少选择一道题',
      trigger: 'change'
    }
  ]
}

const availableQuestions = computed(() => {
  if (createForm.type === 'code') {
    return questions.value.filter(item => item.type === 'code')
  }

  return questions.value.filter(item => item.type !== 'code')
})
const additionalQuestions = computed(() => {
  const usedQuestionIds = new Set(
    assignmentDetail.value?.items.map(
      item => item.questionId
    ) || []
  )

  const unusedQuestions = questions.value.filter(
    item => !usedQuestionIds.has(item.id)
  )

  if (assignmentDetail.value?.type === 'code') {
    return unusedQuestions.filter(
      item => item.type === 'code'
    )
  }

  if (assignmentDetail.value?.type === 'homework') {
    return unusedQuestions.filter(
      item => item.type !== 'code'
    )
  }

  // 契约预览可能返回非枚举字符串，此时仅按“未重复”筛选
  return unusedQuestions
})

async function loadAssignments() {
  loading.value = true
  loadError.value = false

  try {
    assignments.value = await getTeacherAssignments(
      selectedClassId.value
    )
  } catch {
    assignments.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  loading.value = true
  loadError.value = false

  try {
    const [classData, questionData] = await Promise.all([
      getTeacherClasses(),
      getTeacherQuestions()
    ])

    classes.value = classData
    questions.value = questionData
    assignments.value = await getTeacherAssignments(
      selectedClassId.value
    )
  } catch {
    assignments.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function resetCreateForm() {
  createForm.classId = undefined
  createForm.title = ''
  createForm.type = 'homework'
  createForm.deadline = null
  createForm.description = ''
  createForm.questionIds = []

  selectedQuestionIds.value = []

  Object.keys(questionScores).forEach(key => {
    delete questionScores[Number(key)]
  })

  questions.value.forEach(item => {
    questionScores[item.id] = 10
  })

  createFormRef.value?.clearValidate()
  questionTableRef.value?.clearSelection()
}

function openCreate() {
  resetCreateForm()
  createVisible.value = true
}

function questionSelectable(question: TeacherQuestion) {
  return createForm.type === 'code'
    ? question.type === 'code'
    : question.type !== 'code'
}

function handleQuestionSelection(rows: TeacherQuestion[]) {
  selectedQuestionIds.value = rows.map(item => item.id)
  createForm.questionIds = [...selectedQuestionIds.value]
  createFormRef.value?.validateField('questionIds')
}

async function submitCreate() {
  const valid = await createFormRef.value
    ?.validate()
    .catch(() => false)

  if (!valid || createForm.classId === undefined) {
    return
  }

  submitting.value = true

  try {
    await createTeacherAssignment({
      classId: createForm.classId,
      title: createForm.title.trim(),
      type: createForm.type,
      deadline: createForm.deadline,
      description: createForm.description.trim(),
      items: selectedQuestionIds.value.map(questionId => ({
        questionId,
        score: questionScores[questionId] ?? 10
      }))
    })

    ElMessage.success('作业创建成功')
    createVisible.value = false
    await loadAssignments()
  } catch {
    // 公共请求层统一显示后端错误，不修改本地业务状态
  } finally {
    submitting.value = false
  }
}
function openAddItem() {
  if (!assignmentDetail.value) {
    return
  }

  addItemForm.questionId = undefined
  addItemForm.score = 10
  addItemVisible.value = true
  addItemFormRef.value?.clearValidate()
}

async function submitAddItem() {
  const valid = await addItemFormRef.value
    ?.validate()
    .catch(() => false)

  if (
    !valid ||
    !assignmentDetail.value ||
    addItemForm.questionId === undefined
  ) {
    return
  }

  addItemSubmitting.value = true

  try {
    const assignmentId = assignmentDetail.value.id

    await addTeacherAssignmentItem(assignmentId, {
      questionId: addItemForm.questionId,
      score: addItemForm.score
    })

    ElMessage.success('题目添加成功')
    addItemVisible.value = false
    await loadDetail(assignmentId)
    await loadAssignments()
  } catch {
    // 请求失败时不修改本地题目或显示伪成功
  } finally {
    addItemSubmitting.value = false
  }
}
function openEdit(assignment: TeacherAssignment) {
  currentAssignment.value = assignment
  editForm.title = assignment.title
  editForm.deadline = assignment.deadline
  editForm.status =
    assignment.status === 1 ? '1' : '0'
  editVisible.value = true

  editFormRef.value?.clearValidate()
}

async function submitEdit() {
  const valid = await editFormRef.value
    ?.validate()
    .catch(() => false)

  if (!valid || !currentAssignment.value) {
    return
  }

  editSubmitting.value = true

  try {
    await updateTeacherAssignment(
      currentAssignment.value.id,
      {
        title: editForm.title.trim(),
        deadline: editForm.deadline,
        status: editForm.status
      }
    )

    ElMessage.success('作业修改成功')
    editVisible.value = false
    await loadAssignments()

    if (detailVisible.value) {
      await loadDetail(currentAssignment.value.id)
    }
  } catch {
    // 请求失败时不修改本地列表或显示伪成功
  } finally {
    editSubmitting.value = false
  }
}
async function loadDetail(assignmentId: number) {
  detailLoading.value = true
  detailError.value = false
  assignmentDetail.value = undefined

  try {
    assignmentDetail.value =
      await getTeacherAssignment(assignmentId)
  } catch {
    detailError.value = true
  } finally {
    detailLoading.value = false
  }
}

function openDetail(assignment: TeacherAssignment) {
  currentAssignment.value = assignment
  detailVisible.value = true
  loadDetail(assignment.id)
}

async function handlePublish(
  assignment: TeacherAssignment
) {
  const action =
    assignment.status === 1 ? '重新发布' : '发布'

  try {
    await ElMessageBox.confirm(
      `确认${action}作业“${assignment.title}”吗？`,
      `${action}作业`,
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  try {
    await publishTeacherAssignment(assignment.id)
    ElMessage.success(`作业${action}成功`)
    await loadAssignments()
  } catch {
    // 公共请求层统一处理错误
  }
}

async function handleDelete(
  assignment: TeacherAssignment
) {
  try {
    await ElMessageBox.confirm(
      `删除后将无法继续使用作业“${assignment.title}”，确认删除吗？`,
      '删除作业',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  try {
    await deleteTeacherAssignment(assignment.id)
    ElMessage.success('作业删除成功')

    if (
      currentAssignment.value?.id === assignment.id
    ) {
      detailVisible.value = false
      currentAssignment.value = undefined
      assignmentDetail.value = undefined
    }

    await loadAssignments()
  } catch {
    // 后端失败时不删除本地列表，不显示伪成功
  }
}
function getAssignmentTypeText(value?: string) {
  if (value === 'homework') {
    return '普通作业'
  }

  if (value === 'code') {
    return '代码作业'
  }

  return value || '未提供'
}
function getClassName(classId: number) {
  return (
    classes.value.find(item => item.id === classId)?.name ||
    `班级 ${classId}`
  )
}

function formatTime(value: string | null) {
  if (!value) {
    return '未设置'
  }

  const time = new Date(value)

  return Number.isNaN(time.getTime())
    ? value
    : time.toLocaleString('zh-CN', {
        hour12: false
      })
}

onMounted(loadAll)
</script>

<style scoped>
.page-container {
  padding: var(--space-xl);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.eyebrow {
  margin: 0 0 var(--space-xs);
  color: var(--primary);
  font: var(--text-sm-medium);
  letter-spacing: 1px;
}

.page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: 36px;
}

.description {
  margin: var(--space-xs) 0 0;
  color: var(--muted);
}

.content-card {
  min-height: 520px;
  padding: var(--space-lg);
  background: var(--canvas);
  border: 1px solid var(--hairline);
  border-radius: var(--radius-md);
}

.toolbar {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
}

.class-filter {
  width: 260px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: var(--space-lg);
}

.question-area {
  width: 100%;
}

.question-area .el-alert {
  margin-bottom: var(--space-sm);
}

:deep(.assignment-dialog .el-dialog__body) {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}
.question-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  max-width: 420px;
}

.question-option span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-descriptions {
  margin-bottom: var(--space-lg);
}

.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: var(--space-lg) 0 var(--space-sm);
}

.detail-heading h3 {
  margin: 0;
  color: var(--ink);
}

.detail-heading p {
  margin: var(--space-xs) 0 0;
  color: var(--muted);
}

.assignment-items {
  display: grid;
  gap: var(--space-sm);
}

.assignment-item {
  padding: var(--space-md);
  border: 1px solid var(--hairline);
  border-radius: var(--radius-sm);
  background: var(--canvas);
}

.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}

.item-header > div {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.item-index {
  color: var(--ink);
  font-weight: 600;
}

.item-content {
  margin: var(--space-sm) 0;
  color: var(--ink);
  line-height: 1.7;
  white-space: pre-wrap;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm) var(--space-lg);
  color: var(--muted);
  font-size: 13px;
}
@media (max-width: 800px) {
  .page-container {
    padding: var(--space-lg);
  }

  .page-header {
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .class-filter {
    width: 100%;
  }
}
</style>