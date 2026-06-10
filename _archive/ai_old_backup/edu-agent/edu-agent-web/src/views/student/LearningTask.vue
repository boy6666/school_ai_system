<template>
  <div class="task-page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">学习任务 / 计划</p>
        <h1>我的学习计划</h1>
        <p>
          查看今日任务、本周任务、学习进度和任务完成情况，帮助你按计划推进课程学习。
        </p>
      </div>

      <button class="primary-btn" @click="openCreate">新增任务</button>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span>今日任务</span>
        <strong>{{ summary.todayCount }}</strong>
      </div>

      <div class="summary-card">
        <span>本周任务</span>
        <strong>{{ summary.weekCount }}</strong>
      </div>

      <div class="summary-card">
        <span>已完成</span>
        <strong>{{ summary.doneCount }}</strong>
      </div>

      <div class="summary-card">
        <span>平均进度</span>
        <strong>{{ summary.averageProgress }}%</strong>
      </div>
    </section>

    <section class="panel">
      <div class="toolbar">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索任务、课程、章节..."
          @keyup.enter="fetchTasks"
        />

        <select v-model="status">
          <option value="">全部状态</option>
          <option value="todo">未开始</option>
          <option value="doing">进行中</option>
          <option value="done">已完成</option>
        </select>

        <select v-model="priority">
          <option value="">全部优先级</option>
          <option value="high">高</option>
          <option value="middle">中</option>
          <option value="low">低</option>
        </select>

        <button @click="fetchTasks">查询</button>
      </div>

      <div v-if="loading" class="state-card">
        任务加载中...
      </div>

      <div v-else class="task-list">
        <article
          v-for="task in filteredTasks"
          :key="task.id"
          class="task-card"
        >
          <div class="task-main">
            <div class="task-title">
              <h3>{{ task.title }}</h3>
              <span :class="['priority-tag', task.priority]">
                {{ getPriorityText(task.priority) }}
              </span>
            </div>

            <p>{{ task.courseName }} · {{ task.chapterName }}</p>

            <div class="time-row">
              <span>{{ task.startTime }}</span>
              <span>至</span>
              <span>{{ task.endTime }}</span>
            </div>

            <div class="progress-bar">
              <div :style="{ width: task.progress + '%' }"></div>
            </div>
          </div>

          <div class="task-actions">
            <span :class="['status-tag', task.status]">
              {{ getStatusText(task.status) }}
            </span>

            <button
              v-if="task.status !== 'done'"
              @click="markDone(task)"
            >
              标记完成
            </button>
          </div>
        </article>

        <div v-if="filteredTasks.length === 0" class="state-card">
          暂无匹配任务
        </div>
      </div>
    </section>

    <div v-if="dialogVisible" class="dialog-mask">
      <div class="dialog">
        <h3>新增学习任务</h3>

        <label>
          任务名称
          <input
            v-model="form.title"
            type="text"
            placeholder="例如：完成搜索算法章节学习"
          />
        </label>

        <label>
          所属课程
          <input
            v-model="form.courseName"
            type="text"
            placeholder="例如：人工智能导论"
          />
        </label>

        <label>
          优先级
          <select v-model="form.priority">
            <option value="high">高</option>
            <option value="middle">中</option>
            <option value="low">低</option>
          </select>
        </label>

        <div class="dialog-actions">
          <button class="outline-btn" @click="dialogVisible = false">
            取消
          </button>
          <button class="primary-btn" @click="saveTask">
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getLearningTasks,
  updateLearningTaskStatus
} from '@/api/task'
import type {
  LearningTaskItem,
  LearningTaskSummary,
  TaskPriority,
  TaskStatus
} from '@/api/task'

const loading = ref(false)
const keyword = ref('')
const status = ref('')
const priority = ref('')
const dialogVisible = ref(false)

const summary = reactive<LearningTaskSummary>({
  todayCount: 0,
  weekCount: 0,
  doneCount: 0,
  averageProgress: 0
})

const form = reactive({
  title: '',
  courseName: '',
  priority: 'middle' as TaskPriority
})

const fallbackTasks: LearningTaskItem[] = [
  {
    id: 1,
    title: '完成搜索算法章节学习',
    courseName: '人工智能导论',
    chapterName: '第 2 章：搜索算法',
    startTime: '今天 09:00',
    endTime: '今天 22:00',
    priority: 'high',
    status: 'doing',
    progress: 45
  },
  {
    id: 2,
    title: '提交 A* 算法练习题',
    courseName: '人工智能导论',
    chapterName: '第 2 章：搜索算法',
    startTime: '明天 09:00',
    endTime: '明天 18:00',
    priority: 'middle',
    status: 'todo',
    progress: 0
  },
  {
    id: 3,
    title: '整理 Python 数据分析笔记',
    courseName: 'Python 程序设计',
    chapterName: 'Pandas 数据处理',
    startTime: '周三 10:00',
    endTime: '周五 20:00',
    priority: 'low',
    status: 'done',
    progress: 100
  }
]

const tasks = ref<LearningTaskItem[]>([])

const filteredTasks = computed(() => {
  return tasks.value.filter((task: LearningTaskItem) => {
    const matchKeyword =
      !keyword.value ||
      task.title.includes(keyword.value) ||
      task.courseName.includes(keyword.value) ||
      task.chapterName.includes(keyword.value)

    const matchStatus = !status.value || task.status === status.value
    const matchPriority = !priority.value || task.priority === priority.value

    return matchKeyword && matchStatus && matchPriority
  })
})

const updateSummary = () => {
  summary.todayCount = tasks.value.filter((item: LearningTaskItem) =>
    item.startTime.includes('今天')
  ).length

  summary.weekCount = tasks.value.length

  summary.doneCount = tasks.value.filter((item: LearningTaskItem) =>
    item.status === 'done'
  ).length

  summary.averageProgress = Math.round(
    tasks.value.reduce((sum, item) => sum + item.progress, 0) /
      Math.max(tasks.value.length, 1)
  )
}

const fetchTasks = async () => {
  loading.value = true

  try {
    const result = await getLearningTasks({
      keyword: keyword.value,
      status: status.value,
      priority: priority.value
    })

    tasks.value = result.list
    Object.assign(summary, result.summary)
  } catch (error) {
    console.warn('学习任务接口暂不可用，使用页面静态数据：', error)
    tasks.value = fallbackTasks
    updateSummary()
  } finally {
    loading.value = false
  }
}

const markDone = async (task: LearningTaskItem) => {
  const oldStatus = task.status
  const oldProgress = task.progress

  task.status = 'done'
  task.progress = 100
  updateSummary()

  try {
    await updateLearningTaskStatus(task.id, 'done')
  } catch (error) {
    console.warn('任务状态接口暂不可用，仅更新页面状态：', error)
    task.status = oldStatus
    task.progress = oldProgress
    updateSummary()
  }
}

const openCreate = () => {
  form.title = ''
  form.courseName = ''
  form.priority = 'middle'
  dialogVisible.value = true
}

const saveTask = () => {
  if (!form.title.trim()) {
    alert('请输入任务名称')
    return
  }

  tasks.value.unshift({
    id: Date.now(),
    title: form.title,
    courseName: form.courseName || '未选择课程',
    chapterName: '自定义任务',
    startTime: '今天',
    endTime: '待设置',
    priority: form.priority,
    status: 'todo',
    progress: 0
  })

  dialogVisible.value = false
  updateSummary()
}

const getStatusText = (value: TaskStatus) => {
  const map: Record<TaskStatus, string> = {
    todo: '未开始',
    doing: '进行中',
    done: '已完成'
  }

  return map[value]
}

const getPriorityText = (value: TaskPriority) => {
  const map: Record<TaskPriority, string> = {
    high: '高优先级',
    middle: '中优先级',
    low: '低优先级'
  }

  return map[value]
}

onMounted(() => {
  fetchTasks()
})
</script>

<style scoped>
.task-page {
  min-height: 100vh;
  padding: clamp(14px, 2vw, 28px);
  background: #f5f8ff;
  color: #1f2a44;
  overflow-x: hidden;
}

.page-hero,
.panel,
.summary-card,
.task-card,
.dialog {
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.page-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
  margin-bottom: 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #eaf2ff 100%);
}

.eyebrow {
  margin: 0 0 8px;
  color: #1769ff;
  font-weight: 700;
}

.page-hero h1 {
  margin: 0;
  font-size: 30px;
}

.page-hero p {
  color: #667085;
}

.primary-btn,
.outline-btn,
.toolbar button,
.task-actions button {
  height: 40px;
  padding: 0 18px;
  border-radius: 12px;
  cursor: pointer;
}

.primary-btn,
.toolbar button,
.task-actions button {
  border: none;
  color: #ffffff;
  background: #1769ff;
}

.outline-btn {
  border: 1px solid #dbe4f3;
  color: #1769ff;
  background: #ffffff;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 20px;
  border-radius: 20px;
}

.summary-card span {
  color: #667085;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  color: #1769ff;
  font-size: 30px;
}

.panel {
  padding: 20px;
  border-radius: 22px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
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
  min-width: 260px;
  flex: 1;
}

.task-list {
  display: grid;
  gap: 14px;
}

.task-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border-radius: 18px;
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-title {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.task-title h3 {
  margin: 0;
}

.task-main p,
.time-row {
  color: #667085;
  font-size: 14px;
}

.time-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.progress-bar {
  height: 8px;
  margin-top: 12px;
  border-radius: 999px;
  overflow: hidden;
  background: #e8eef7;
}

.progress-bar div {
  height: 100%;
  background: #1769ff;
}

.task-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.priority-tag,
.status-tag {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.priority-tag.high {
  color: #b91c1c;
  background: #fef2f2;
}

.priority-tag.middle {
  color: #b45309;
  background: #fff7ed;
}

.priority-tag.low {
  color: #15803d;
  background: #ecfdf3;
}

.status-tag.todo {
  color: #52637a;
  background: #f1f5fb;
}

.status-tag.doing {
  color: #1769ff;
  background: #eef5ff;
}

.status-tag.done {
  color: #15803d;
  background: #ecfdf3;
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
  padding: 20px;
  background: rgba(15, 23, 42, 0.35);
}

.dialog {
  width: min(520px, 100%);
  padding: 24px;
  border-radius: 22px;
}

.dialog label {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
  color: #52637a;
}

.dialog input,
.dialog select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dbe4f3;
  border-radius: 12px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 900px) {
  .page-hero,
  .task-card {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-actions {
    align-items: flex-start;
  }
}

@media (max-width: 520px) {
  .task-page {
    padding: 12px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .toolbar input {
    min-width: 100%;
  }

  .primary-btn,
  .outline-btn {
    width: 100%;
  }

  .dialog-actions {
    flex-direction: column;
  }
}
</style>