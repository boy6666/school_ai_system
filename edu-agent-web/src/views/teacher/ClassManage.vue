<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">CLASS MANAGEMENT</p>
        <h1>班级管理</h1>
        <p class="description">
          管理本人创建的班级及班级学生。
        </p>
      </div>

      <el-button type="primary" @click="openCreateDialog">
        新建班级
      </el-button>
    </header>

    <el-card class="content-card" shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索班级名称、课程或学期"
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-button :loading="loading" @click="loadClasses">
          刷新
        </el-button>
      </div>

      <div v-if="loadError && !loading" class="state-container">
        <el-result
          icon="warning"
          title="班级加载失败"
          sub-title="请检查教师服务或网关是否可用"
        >
          <template #extra>
            <el-button type="primary" @click="loadClasses">
              重新加载
            </el-button>
          </template>
        </el-result>
      </div>

      <el-table
        v-else
        v-loading="loading"
        :data="filteredClasses"
        row-key="id"
        empty-text="暂无班级"
      >
        <el-table-column prop="name" label="班级名称" min-width="160" />
        <el-table-column prop="course" label="课程" min-width="180">
          <template #default="{ row }">
            {{ row.course || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="semester" label="学期" min-width="120">
          <template #default="{ row }">
            {{ row.semester || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="学生数" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openStudents(row)">
              学生管理
            </el-button>
            <el-button link type="primary" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="confirmDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="classDialogVisible"
      :title="editingClassId === null ? '新建班级' : '编辑班级'"
      width="520px"
      destroy-on-close
      @closed="resetClassForm"
    >
      <el-form
        ref="classFormRef"
        :model="classForm"
        :rules="classRules"
        label-width="84px"
      >
        <el-form-item label="班级名称" prop="name">
          <el-input
            v-model="classForm.name"
            maxlength="128"
            show-word-limit
            placeholder="请输入班级名称"
          />
        </el-form-item>

        <el-form-item label="课程" prop="course">
          <el-input
            v-model="classForm.course"
            maxlength="64"
            show-word-limit
            placeholder="请输入课程名称"
          />
        </el-form-item>

        <el-form-item label="学期" prop="semester">
          <el-input
            v-model="classForm.semester"
            maxlength="32"
            show-word-limit
            placeholder="例如：2026秋"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="classDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="classSubmitting"
          @click="submitClass"
        >
          确认
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="studentDrawerVisible"
      :title="selectedClass ? `${selectedClass.name} · 学生管理` : '学生管理'"
      size="620px"
      destroy-on-close
    >
      <div class="student-toolbar">
        <el-input-number
          v-model="studentId"
          :min="1"
          :precision="0"
          controls-position="right"
          placeholder="学生 ID"
        />
        <el-button
          type="primary"
          :disabled="studentId === undefined"
          :loading="studentSubmitting"
          @click="submitStudent"
        >
          添加学生
        </el-button>
        <el-button
          :loading="studentsLoading"
          @click="loadStudents"
        >
          刷新
        </el-button>
      </div>

      <div
        v-if="studentsLoadError && !studentsLoading"
        class="state-container"
      >
        <el-result
          icon="warning"
          title="学生列表加载失败"
          sub-title="请稍后重试"
        >
          <template #extra>
            <el-button type="primary" @click="loadStudents">
              重新加载
            </el-button>
          </template>
        </el-result>
      </div>

      <el-table
        v-else
        v-loading="studentsLoading"
        :data="students"
        row-key="studentId"
        empty-text="班级中暂无学生"
      >
        <el-table-column prop="studentId" label="学生 ID" width="110" />
        <el-table-column prop="studentName" label="学生姓名" min-width="160">
          <template #default="{ row }">
            {{ row.studentName || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="加入时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.joinedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="danger"
              @click="confirmRemoveStudent(row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addClassStudent,
  createTeacherClass,
  deleteTeacherClass,
  getClassStudents,
  getTeacherClasses,
  removeClassStudent,
  updateTeacherClass
} from '@/api/teacher'
import type {
  ClassStudent,
  TeacherClass
} from '@/api/teacher'

const classes = ref<TeacherClass[]>([])
const students = ref<ClassStudent[]>([])
const selectedClass = ref<TeacherClass | null>(null)

const keyword = ref('')
const loading = ref(false)
const loadError = ref(false)
const studentsLoading = ref(false)
const studentsLoadError = ref(false)

const classDialogVisible = ref(false)
const classSubmitting = ref(false)
const editingClassId = ref<number | null>(null)
const classFormRef = ref<FormInstance>()

const studentDrawerVisible = ref(false)
const studentSubmitting = ref(false)
const studentId = ref<number>()

const classForm = reactive({
  name: '',
  course: '',
  semester: ''
})

const classRules: FormRules = {
  name: [
    {
      required: true,
      message: '请输入班级名称',
      trigger: 'blur'
    },
    {
      max: 128,
      message: '班级名称不能超过 128 个字符',
      trigger: 'blur'
    }
  ],
  course: [
    {
      max: 64,
      message: '课程名称不能超过 64 个字符',
      trigger: 'blur'
    }
  ],
  semester: [
    {
      max: 32,
      message: '学期不能超过 32 个字符',
      trigger: 'blur'
    }
  ]
}

const filteredClasses = computed(() => {
  const value = keyword.value.trim().toLowerCase()

  if (!value) {
    return classes.value
  }

  return classes.value.filter(item =>
    [item.name, item.course, item.semester]
      .some(field => field?.toLowerCase().includes(value))
  )
})

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

async function loadClasses() {
  loading.value = true
  loadError.value = false

  try {
    classes.value = await getTeacherClasses()
  } catch {
    classes.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingClassId.value = null
  resetClassForm()
  classDialogVisible.value = true
}

function openEditDialog(item: TeacherClass) {
  editingClassId.value = item.id
  classForm.name = item.name
  classForm.course = item.course || ''
  classForm.semester = item.semester || ''
  classDialogVisible.value = true
}

function resetClassForm() {
  editingClassId.value = null
  classForm.name = ''
  classForm.course = ''
  classForm.semester = ''
  classFormRef.value?.clearValidate()
}

async function submitClass() {
  const valid = await classFormRef.value
    ?.validate()
    .catch(() => false)

  if (!valid) {
    return
  }

  const data = {
    name: classForm.name.trim(),
    course: classForm.course.trim() || undefined,
    semester: classForm.semester.trim() || undefined
  }

  classSubmitting.value = true

  try {
    if (editingClassId.value === null) {
      await createTeacherClass(data)
      ElMessage.success('班级创建成功')
    } else {
      await updateTeacherClass(editingClassId.value, data)
      ElMessage.success('班级更新成功')
    }

    classDialogVisible.value = false
    await loadClasses()
  } finally {
    classSubmitting.value = false
  }
}

async function confirmDelete(item: TeacherClass) {
  try {
    await ElMessageBox.confirm(
      `确定删除班级“${item.name}”吗？`,
      '删除班级',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  await deleteTeacherClass(item.id)
  ElMessage.success('班级删除成功')
  await loadClasses()
}

async function openStudents(item: TeacherClass) {
  selectedClass.value = item
  studentId.value = undefined
  studentDrawerVisible.value = true
  await loadStudents()
}

async function loadStudents() {
  if (!selectedClass.value) {
    return
  }

  studentsLoading.value = true
  studentsLoadError.value = false

  try {
    students.value = await getClassStudents(
      selectedClass.value.id
    )
  } catch {
    students.value = []
    studentsLoadError.value = true
  } finally {
    studentsLoading.value = false
  }
}

async function submitStudent() {
  if (!selectedClass.value || studentId.value === undefined) {
    return
  }

  studentSubmitting.value = true

  try {
    await addClassStudent(selectedClass.value.id, {
      studentId: studentId.value
    })
    ElMessage.success('学生添加成功')
    studentId.value = undefined
    await Promise.all([
      loadStudents(),
      loadClasses()
    ])
  } finally {
    studentSubmitting.value = false
  }
}

async function confirmRemoveStudent(item: ClassStudent) {
  if (!selectedClass.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定将“${item.studentName || item.studentId}”移出班级吗？`,
      '移除学生',
      {
        type: 'warning',
        confirmButtonText: '移除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  await removeClassStudent(
    selectedClass.value.id,
    item.studentId
  )
  ElMessage.success('学生已移除')

  await Promise.all([
    loadStudents(),
    loadClasses()
  ])
}

onMounted(loadClasses)
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

.content-card {
  min-height: 420px;
}

.toolbar,
.student-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
}

.search-input {
  width: 360px;
}

.state-container {
  display: flex;
  min-height: 320px;
  align-items: center;
  justify-content: center;
}

@media (max-width: 760px) {
  .page-container {
    padding: var(--space-lg);
  }

  .page-header {
    flex-direction: column;
  }

  .toolbar,
  .student-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }
}
</style>