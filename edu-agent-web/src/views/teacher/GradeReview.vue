<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">GRADE REVIEW</p>
        <h1>批改复核</h1>
        <p class="description">
          查看学生提交结果，并复核分数、评语及 AI 报告。
        </p>
      </div>
    </header>

    <section class="content-card">
      <div class="toolbar">
        <el-select
          v-model="selectedAssignmentId"
          filterable
          placeholder="请选择作业"
          class="assignment-filter"
          @change="loadGrades"
        >
          <el-option
            v-for="item in assignments"
            :key="item.id"
            :label="item.title"
            :value="item.id"
          />
        </el-select>

        <el-input-number
          v-model="studentIdFilter"
          :min="1"
          :controls="false"
          placeholder="学生 ID"
          class="student-filter"
        />

        <el-button
          type="primary"
          :disabled="selectedAssignmentId === undefined"
          @click="loadGrades"
        >
          查询
        </el-button>

        <el-button @click="resetFilter">
          重置
        </el-button>
      </div>

      <div v-loading="loading">
        <el-result
          v-if="loadError"
          icon="warning"
          title="成绩加载失败"
          sub-title="请检查教师服务或网关是否可用"
        >
          <template #extra>
            <el-button type="primary" @click="loadGrades">
              重新加载
            </el-button>
          </template>
        </el-result>

        <el-empty
          v-else-if="selectedAssignmentId === undefined"
          description="请先选择需要批改的作业"
        />

        <el-empty
          v-else-if="!grades.length"
          description="当前作业暂无提交记录"
        />

        <el-table
          v-else
          :data="grades"
          row-key="id"
        >
          <el-table-column
            prop="studentId"
            label="学生 ID"
            width="120"
          />

          <el-table-column
            prop="itemId"
            label="题目项 ID"
            width="130"
          />

          <el-table-column label="题型" width="110">
            <template #default="{ row }">
              {{ gradeTypeText(row.type) }}
            </template>
          </el-table-column>

          <el-table-column label="语言" width="100">
            <template #default="{ row }">
              {{ row.language || '—' }}
            </template>
          </el-table-column>

          <el-table-column
            prop="score"
            label="分数"
            width="90"
          />

          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 1 ? 'success' : 'warning'"
              >
                {{ row.status === 1 ? '已批改' : '待批改' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="AI 报告" width="110">
            <template #default="{ row }">
              <el-tag
                :type="row.hasAiReport ? 'success' : 'info'"
              >
                {{ row.hasAiReport ? '已生成' : '无' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="批改时间" min-width="180">
            <template #default="{ row }">
              {{ formatTime(row.gradedAt) }}
            </template>
          </el-table-column>

          <el-table-column
            label="操作"
            width="120"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openReview(row)"
              >
                查看并复核
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-drawer
      v-model="reviewVisible"
      title="成绩详情与复核"
      size="760px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <el-result
          v-if="detailError"
          icon="warning"
          title="成绩详情加载失败"
        >
          <template #extra>
            <el-button
              v-if="currentGrade"
              type="primary"
              @click="loadGradeDetail(currentGrade.id)"
            >
              重新加载
            </el-button>
          </template>
        </el-result>

        <template v-else-if="gradeDetail">
          <el-descriptions
            :column="2"
            border
            class="detail-descriptions"
          >
            <el-descriptions-item label="学生 ID">
              {{ gradeDetail.studentId }}
            </el-descriptions-item>

            <el-descriptions-item label="题目项 ID">
              {{ gradeDetail.itemId }}
            </el-descriptions-item>

            <el-descriptions-item label="题型">
              {{ gradeTypeText(gradeDetail.type) }}
            </el-descriptions-item>

            <el-descriptions-item label="语言">
              {{ gradeDetail.language || '—' }}
            </el-descriptions-item>

            <el-descriptions-item label="当前分数">
              {{ gradeDetail.score }}
            </el-descriptions-item>

            <el-descriptions-item label="状态">
              <el-tag
                :type="
                  gradeDetail.status === 1
                    ? 'success'
                    : 'warning'
                "
              >
                {{
                  gradeDetail.status === 1
                    ? '已批改'
                    : '待批改'
                }}
              </el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="批改时间" :span="2">
              {{ formatTime(gradeDetail.gradedAt) }}
            </el-descriptions-item>
          </el-descriptions>

          <section class="detail-section">
            <h3>学生答案</h3>
            <pre class="report-block">{{
              gradeDetail.submission || '无提交内容'
            }}</pre>
          </section>

          <template v-if="gradeDetail.type === 'code'">
            <section class="detail-section">
              <h3>代码运行结果</h3>
              <pre class="report-block">{{
                formatReport(gradeDetail.runResult)
              }}</pre>
            </section>

            <section class="detail-section">
              <h3>静态检查报告</h3>
              <pre class="report-block">{{
                formatReport(gradeDetail.staticReport)
              }}</pre>
            </section>
          </template>

          <section class="detail-section">
            <h3>AI 报告</h3>
            <pre class="report-block">{{
              formatReport(gradeDetail.aiReport)
            }}</pre>
          </section>

          <el-divider />

          <section class="detail-section">
            <h3>教师复核</h3>

            <el-form
              ref="reviewFormRef"
              :model="reviewForm"
              :rules="reviewRules"
              label-width="110px"
            >
              <el-form-item label="复核分数" prop="score">
              <el-input-number
                  v-model="reviewForm.score"
                  controls-position="right"
                />
              </el-form-item>

              <el-form-item label="教师评语">
                <el-input
                  v-model="reviewForm.comment"
                  type="textarea"
                  :rows="4"
                  maxlength="2000"
                  show-word-limit
                  placeholder="请输入教师评语"
                />
              </el-form-item>

              <el-form-item label="覆盖 AI 报告">
                <el-input
                  v-model="reviewForm.aiReportOverride"
                  type="textarea"
                  :rows="4"
                  placeholder="如需覆盖 AI 报告，请输入新的报告内容"
                />
              </el-form-item>
            </el-form>

            <div class="review-actions">
              <el-button
                v-if="gradeDetail.type === 'code'"
                type="warning"
                :disabled="
                  gradeDetail.submissionId == null ||
                  gradeDetail.status !== 1
                "
                :loading="regradeSubmitting"
                @click="submitRegrade"
              >
                重新判分
              </el-button>

              <el-button
                type="primary"
                :loading="reviewSubmitting"
                @click="submitReview"
              >
                保存复核
              </el-button>
            </div>
          </section>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import {
  onMounted,
  reactive,
  ref
} from 'vue'
import type {
  FormInstance,
  FormRules
} from 'element-plus'
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import {
  getAssignmentGrades,
  getTeacherAssignments,
  getTeacherGrade,
  updateTeacherGrade
} from '@/api/teacher'
import {
  regradeCodeSubmission
} from '@/api/code'
import type {
  TeacherAssignment,
  TeacherGrade,
  TeacherGradeDetail,
  UpdateGradeRequest
} from '@/api/teacher'

interface ReviewFormModel {
  score: number
  comment: string
  aiReportOverride: string
}

const loading = ref(false)
const loadError = ref(false)
const detailLoading = ref(false)
const detailError = ref(false)
const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const regradeSubmitting = ref(false)

const selectedAssignmentId = ref<number>()
const studentIdFilter = ref<number>()
const assignments = ref<TeacherAssignment[]>([])
const grades = ref<TeacherGrade[]>([])
const currentGrade = ref<TeacherGrade>()
const gradeDetail = ref<TeacherGradeDetail>()

const reviewFormRef = ref<FormInstance>()

const reviewForm = reactive<ReviewFormModel>({
  score: 0,
  comment: '',
  aiReportOverride: ''
})

const reviewRules: FormRules<ReviewFormModel> = {
  score: [
    {
      required: true,
      message: '请输入复核分数',
      trigger: 'change'
    }
  ]
}

async function loadAssignments() {
  loading.value = true
  loadError.value = false

  try {
    assignments.value = await getTeacherAssignments()

    if (
      selectedAssignmentId.value === undefined &&
      assignments.value.length
    ) {
      selectedAssignmentId.value = assignments.value[0].id
    }

    if (selectedAssignmentId.value !== undefined) {
      await loadGrades()
    }
  } catch {
    assignments.value = []
    grades.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function loadGrades() {
  if (selectedAssignmentId.value === undefined) {
    grades.value = []
    return
  }

  loading.value = true
  loadError.value = false

  try {
    grades.value = await getAssignmentGrades(
      selectedAssignmentId.value,
      studentIdFilter.value
    )
  } catch {
    grades.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  studentIdFilter.value = undefined
  loadGrades()
}

async function loadGradeDetail(gradeId: number) {
  detailLoading.value = true
  detailError.value = false
  gradeDetail.value = undefined

  try {
    const detail = await getTeacherGrade(gradeId)
    gradeDetail.value = detail
    reviewForm.score = detail.score
    reviewForm.comment = detail.comment || ''
    reviewForm.aiReportOverride = ''
  } catch {
    detailError.value = true
  } finally {
    detailLoading.value = false
  }
}

function openReview(grade: TeacherGrade) {
  currentGrade.value = grade
  reviewVisible.value = true
  reviewFormRef.value?.clearValidate()
  loadGradeDetail(grade.id)
}

async function submitReview() {
  const valid = await reviewFormRef.value
    ?.validate()
    .catch(() => false)

  if (!valid || !gradeDetail.value) {
    return
  }

  reviewSubmitting.value = true

  try {
    const payload: UpdateGradeRequest = {
      score: reviewForm.score,
      comment: reviewForm.comment.trim()
    }

    const override =
      reviewForm.aiReportOverride.trim()

    if (override) {
      payload.aiReportOverride = override
    }

    const updated = await updateTeacherGrade(
      gradeDetail.value.id,
      payload
    )

    gradeDetail.value = updated
    ElMessage.success('成绩复核成功')
    await loadGrades()
  } catch {
    // 请求失败时不修改本地成绩或显示伪成功
  } finally {
    reviewSubmitting.value = false
  }
}

async function submitRegrade() {
  const detail = gradeDetail.value

  if (
    !detail ||
    detail.type !== 'code' ||
    detail.submissionId == null
  ) {
    ElMessage.warning('当前成绩没有可用的代码提交编号')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要重新运行该学生的代码判分吗？',
      '重新判分确认',
      {
        confirmButtonText: '确认重新判分',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  regradeSubmitting.value = true

  try {
    await regradeCodeSubmission(detail.submissionId)
    ElMessage.success(
      '重新判分请求已提交，请稍后刷新查看结果'
    )
    await loadGrades()
  } catch {
    // 公共请求层统一显示正式接口返回的错误
  } finally {
    regradeSubmitting.value = false
  }
}

function gradeTypeText(value?: string) {
  if (value === 'choice') {
    return '选择题'
  }

  if (value === 'blank') {
    return '填空题'
  }

  if (value === 'code') {
    return '代码题'
  }

  return value || '未提供'
}

function formatReport(value?: string) {
  if (!value) {
    return '无'
  }

  try {
    return JSON.stringify(
      JSON.parse(value),
      null,
      2
    )
  } catch {
    return value
  }
}

function formatTime(value: string | null) {
  if (!value) {
    return '未批改'
  }

  const time = new Date(value)

  return Number.isNaN(time.getTime())
    ? value
    : time.toLocaleString('zh-CN', {
        hour12: false
      })
}

onMounted(loadAssignments)
</script>

<style scoped>
.page-container {
  padding: var(--space-xl);
}

.page-header {
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
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
}

.assignment-filter {
  width: 300px;
}

.student-filter {
  width: 180px;
}

.detail-descriptions {
  margin-bottom: var(--space-lg);
}

.detail-section {
  margin-bottom: var(--space-lg);
}

.detail-section h3 {
  margin: 0 0 var(--space-sm);
  color: var(--ink);
}

.report-block {
  max-height: 280px;
  margin: 0;
  padding: var(--space-md);
  overflow: auto;
  color: var(--ink);
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius-sm);
  font-family: "Courier New", monospace;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 700px) {
  .page-container {
    padding: var(--space-lg);
  }

  .assignment-filter,
  .student-filter {
    width: 100%;
  }
}
</style>
