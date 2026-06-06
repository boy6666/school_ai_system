<template>
  <div class="manage-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>课程 / 资源管理</h1>
        <p>统一维护课程、章节资源、学习材料、上下架状态与资源审核状态。</p>
      </div>

      <div class="header-actions">
        <button class="primary-btn" @click="openCreate">新增</button>
        <button class="outline-btn">批量导入</button>
      </div>
    </section>

    <section class="stat-grid">
      <div class="stat-card">
        <span>课程总数</span>
        <strong>{{ stats.courseTotal }}</strong>
      </div>
      <div class="stat-card">
        <span>资源总数</span>
        <strong>{{ stats.resourceTotal }}</strong>
      </div>
      <div class="stat-card">
        <span>待审核</span>
        <strong>{{ stats.reviewing }}</strong>
      </div>
      <div class="stat-card">
        <span>已下架</span>
        <strong>{{ stats.offline }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="tabs">
        <button
          :class="{ active: activeTab === 'course' }"
          @click="activeTab = 'course'"
        >
          课程管理
        </button>
        <button
          :class="{ active: activeTab === 'resource' }"
          @click="activeTab = 'resource'"
        >
          资源管理
        </button>
      </div>

      <div class="toolbar">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索名称、编号、教师、上传人..."
          @keyup.enter="fetchData"
        />

        <select v-model="status">
          <option value="">全部状态</option>
          <option value="published">已发布</option>
          <option value="draft">草稿</option>
          <option value="reviewing">审核中</option>
          <option value="offline">已下架</option>
        </select>

        <select v-if="activeTab === 'resource'" v-model="resourceType">
          <option value="">全部类型</option>
          <option value="文档">文档</option>
          <option value="PPT">PPT</option>
          <option value="视频">视频</option>
          <option value="题库">题库</option>
          <option value="代码案例">代码案例</option>
          <option value="实验项目">实验项目</option>
        </select>

        <button @click="fetchData">查询</button>
      </div>

      <div v-if="loading" class="state-card">数据加载中...</div>

      <div v-else-if="activeTab === 'course'" class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>课程名称</th>
              <th>课程编号</th>
              <th>教师</th>
              <th>院系</th>
              <th>学期</th>
              <th>学生数</th>
              <th>资源数</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="course in filteredCourses" :key="course.id">
              <td>{{ course.name }}</td>
              <td>{{ course.code }}</td>
              <td>{{ course.teacher }}</td>
              <td>{{ course.department }}</td>
              <td>{{ course.semester }}</td>
              <td>{{ course.studentCount }}</td>
              <td>{{ course.resourceCount }}</td>
              <td>
                <span :class="['status-tag', course.status]">
                  {{ getStatusText(course.status) }}
                </span>
              </td>
              <td>{{ course.updateTime }}</td>
              <td>
                <button class="text-btn" @click="editCourse(course)">编辑</button>
                <button class="text-btn" @click="toggleCourseStatus(course)">
                  {{ course.status === 'offline' ? '上架' : '下架' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="filteredCourses.length === 0" class="state-card">
          暂无课程数据
        </div>
      </div>

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>资源标题</th>
              <th>类型</th>
              <th>所属课程</th>
              <th>难度</th>
              <th>上传人</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="resource in filteredResources" :key="resource.id">
              <td>{{ resource.title }}</td>
              <td>{{ resource.type }}</td>
              <td>{{ resource.courseName }}</td>
              <td>{{ resource.difficulty }}</td>
              <td>{{ resource.uploader }}</td>
              <td>
                <span :class="['status-tag', resource.status]">
                  {{ getStatusText(resource.status) }}
                </span>
              </td>
              <td>{{ resource.updateTime }}</td>
              <td>
                <button class="text-btn" @click="editResource(resource)">编辑</button>
                <button class="text-btn" @click="toggleResourceStatus(resource)">
                  {{ resource.status === 'offline' ? '上架' : '下架' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="filteredResources.length === 0" class="state-card">
          暂无资源数据
        </div>
      </div>
    </section>

    <div v-if="dialogVisible" class="dialog-mask">
      <div class="dialog">
        <h3>{{ dialogTitle }}</h3>

        <div class="form-grid">
          <label>
            名称
            <input v-model="form.name" type="text" placeholder="请输入名称" />
          </label>

          <label>
            类型
            <select v-model="form.type">
              <option value="课程">课程</option>
              <option value="文档">文档</option>
              <option value="PPT">PPT</option>
              <option value="视频">视频</option>
              <option value="题库">题库</option>
              <option value="实验项目">实验项目</option>
            </select>
          </label>

          <label>
            状态
            <select v-model="form.status">
              <option value="published">已发布</option>
              <option value="draft">草稿</option>
              <option value="reviewing">审核中</option>
              <option value="offline">已下架</option>
            </select>
          </label>
        </div>

        <div class="dialog-actions">
          <button class="outline-btn" @click="dialogVisible = false">取消</button>
          <button class="primary-btn" @click="saveForm">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  getAdminCourseList,
  getAdminResourceList,
  updateCourseStatus,
  updateResourceStatus
} from '@/api/admin'
import type {
  AdminCourseItem,
  AdminResourceItem,
  ManageStatus
} from '@/api/admin'

const activeTab = ref<'course' | 'resource'>('course')
const keyword = ref('')
const status = ref('')
const resourceType = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增')

const form = reactive({
  name: '',
  type: '课程',
  status: 'published' as ManageStatus
})

const fallbackCourses: AdminCourseItem[] = [
  {
    id: 1,
    name: '人工智能导论',
    code: 'AI101',
    teacher: '王老师',
    department: '计算机学院',
    semester: '2025-2026-2',
    studentCount: 128,
    resourceCount: 36,
    status: 'published',
    updateTime: '2026-04-28'
  },
  {
    id: 2,
    name: 'Python 程序设计',
    code: 'PY102',
    teacher: '李老师',
    department: '软件学院',
    semester: '2025-2026-2',
    studentCount: 206,
    resourceCount: 42,
    status: 'published',
    updateTime: '2026-04-26'
  },
  {
    id: 3,
    name: '机器学习',
    code: 'ML201',
    teacher: '陈老师',
    department: '人工智能学院',
    semester: '2025-2026-2',
    studentCount: 96,
    resourceCount: 28,
    status: 'draft',
    updateTime: '2026-04-20'
  }
]

const fallbackResources: AdminResourceItem[] = [
  {
    id: 101,
    title: '搜索算法知识点讲解',
    type: '文档',
    courseName: '人工智能导论',
    difficulty: '基础',
    uploader: '王老师',
    status: 'published',
    updateTime: '2026-04-28'
  },
  {
    id: 102,
    title: 'A* 算法可视化动画',
    type: '视频',
    courseName: '人工智能导论',
    difficulty: '进阶',
    uploader: '王老师',
    status: 'reviewing',
    updateTime: '2026-04-27'
  },
  {
    id: 103,
    title: 'Python 爬虫实操案例',
    type: '代码案例',
    courseName: 'Python 程序设计',
    difficulty: '进阶',
    uploader: '李老师',
    status: 'published',
    updateTime: '2026-04-25'
  },
  {
    id: 104,
    title: '机器学习入门练习题',
    type: '题库',
    courseName: '机器学习',
    difficulty: '基础',
    uploader: '陈老师',
    status: 'offline',
    updateTime: '2026-04-18'
  }
]

const courses = ref<AdminCourseItem[]>([])
const resources = ref<AdminResourceItem[]>([])

const stats = computed(() => {
  return {
    courseTotal: courses.value.length,
    resourceTotal: resources.value.length,
    reviewing: resources.value.filter(item => item.status === 'reviewing').length,
    offline:
      courses.value.filter(item => item.status === 'offline').length +
      resources.value.filter(item => item.status === 'offline').length
  }
})

const filteredCourses = computed(() => {
  return courses.value.filter(item => {
    const matchKeyword =
      !keyword.value ||
      item.name.includes(keyword.value) ||
      item.code.includes(keyword.value) ||
      item.teacher.includes(keyword.value)

    const matchStatus = !status.value || item.status === status.value

    return matchKeyword && matchStatus
  })
})

const filteredResources = computed(() => {
  return resources.value.filter(item => {
    const matchKeyword =
      !keyword.value ||
      item.title.includes(keyword.value) ||
      item.courseName.includes(keyword.value) ||
      item.uploader.includes(keyword.value)

    const matchStatus = !status.value || item.status === status.value
    const matchType = !resourceType.value || item.type === resourceType.value

    return matchKeyword && matchStatus && matchType
  })
})

const fetchData = async () => {
  loading.value = true

  const query = {
    keyword: keyword.value,
    status: status.value,
    type: resourceType.value,
    page: 1,
    pageSize: 20
  }

  try {
    if (activeTab.value === 'course') {
      const result = await getAdminCourseList(query)
      courses.value = result.list
    } else {
      const result = await getAdminResourceList(query)
      resources.value = result.list
    }
  } catch (error) {
    console.warn('管理接口暂不可用，使用页面静态数据：', error)
    courses.value = fallbackCourses
    resources.value = fallbackResources
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  dialogTitle.value = activeTab.value === 'course' ? '新增课程' : '新增资源'
  form.name = ''
  form.type = activeTab.value === 'course' ? '课程' : '文档'
  form.status = 'published'
  dialogVisible.value = true
}

const editCourse = (course: AdminCourseItem) => {
  dialogTitle.value = '编辑课程'
  form.name = course.name
  form.type = '课程'
  form.status = course.status
  dialogVisible.value = true
}

const editResource = (resource: AdminResourceItem) => {
  dialogTitle.value = '编辑资源'
  form.name = resource.title
  form.type = resource.type
  form.status = resource.status
  dialogVisible.value = true
}

const toggleCourseStatus = async (course: AdminCourseItem) => {
  const nextStatus: ManageStatus = course.status === 'offline' ? 'published' : 'offline'
  course.status = nextStatus

  try {
    await updateCourseStatus(course.id, nextStatus)
  } catch (error) {
    console.warn('课程状态接口暂不可用，仅更新页面状态：', error)
  }
}

const toggleResourceStatus = async (resource: AdminResourceItem) => {
  const nextStatus: ManageStatus = resource.status === 'offline' ? 'published' : 'offline'
  resource.status = nextStatus

  try {
    await updateResourceStatus(resource.id, nextStatus)
  } catch (error) {
    console.warn('资源状态接口暂不可用，仅更新页面状态：', error)
  }
}

const saveForm = () => {
  dialogVisible.value = false
  alert('已保存')
}

const getStatusText = (value: ManageStatus) => {
  const map: Record<ManageStatus, string> = {
    published: '已发布',
    draft: '草稿',
    offline: '已下架',
    reviewing: '审核中'
  }

  return map[value]
}

watch(activeTab, () => {
  fetchData()
})

onMounted(() => {
  courses.value = fallbackCourses
  resources.value = fallbackResources
})
</script>

<style scoped>
.manage-page {
  min-height: 100vh;
  padding: clamp(12px, 2vw, 24px);
  background: #f5f8ff;
  color: #1f2a44;
  overflow-x: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  margin-bottom: 16px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #eaf2ff 100%);
  box-shadow: 0 12px 30px rgba(32, 88, 180, 0.08);
}

.eyebrow {
  margin: 0 0 8px;
  color: #1769ff;
  font-weight: 700;
}

.page-header h1 {
  margin: 0;
  font-size: 30px;
}

.page-header p {
  color: #667085;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.primary-btn,
.outline-btn {
  height: 40px;
  padding: 0 16px;
  border-radius: 12px;
  cursor: pointer;
}

.primary-btn {
  border: none;
  color: #ffffff;
  background: #1769ff;
}

.outline-btn {
  border: 1px solid #dbe4f3;
  color: #1769ff;
  background: #ffffff;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  padding: 16px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.stat-card span {
  color: #667085;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: #1769ff;
  font-size: 30px;
}

.panel {
  padding: 16px;
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tabs button {
  padding: 8px 16px;
  border: none;
  border-radius: 999px;
  color: #52637a;
  background: #f1f5fb;
  cursor: pointer;
}

.tabs button.active {
  color: #ffffff;
  background: #1769ff;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar input,
.toolbar select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dbe4f3;
  border-radius: 12px;
  outline: none;
}

.toolbar input {
  min-width: 280px;
  flex: 1;
}

.toolbar button {
  height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  color: #ffffff;
  background: #1769ff;
  cursor: pointer;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 920px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 12px;
  border-bottom: 1px solid #eef2f8;
  text-align: left;
  font-size: 14px;
}

th {
  color: #667085;
  background: #f7faff;
}

.status-tag {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.status-tag.published {
  color: #15803d;
  background: #ecfdf3;
}

.status-tag.draft {
  color: #52637a;
  background: #f1f5fb;
}

.status-tag.reviewing {
  color: #b45309;
  background: #fff7ed;
}

.status-tag.offline {
  color: #b91c1c;
  background: #fef2f2;
}

.text-btn {
  margin-right: 8px;
  border: none;
  color: #1769ff;
  background: transparent;
  cursor: pointer;
}

.state-card {
  padding: 40px;
  text-align: center;
  color: #75849a;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(15, 23, 42, 0.35);
}

.dialog {
  width: min(520px, 100%);
  padding: 24px;
  border-radius: 22px;
  background: #ffffff;
}

.dialog h3 {
  margin: 0 0 16px;
}

.form-grid {
  display: grid;
  gap: 12px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: #52637a;
  font-size: 14px;
}

.form-grid input,
.form-grid select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dbe4f3;
  border-radius: 12px;
  outline: none;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar input {
    min-width: 100%;
  }
}

@media (max-width: 520px) {
  .manage-page {
    padding: 12px;
  }

  .page-header {
    padding: 16px;
    border-radius: 18px;
  }

  .page-header h1 {
    font-size: 24px;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .tabs {
    overflow-x: auto;
  }

  .tabs button {
    flex-shrink: 0;
  }

  .header-actions,
  .dialog-actions {
    flex-direction: column;
  }

  .primary-btn,
  .outline-btn {
    width: 100%;
  }
}
</style>