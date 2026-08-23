<template>
  <div class="learning-path-container">
    <el-row :gutter="20">
      <!-- 左侧主内容区 -->
      <el-col :span="16">
        <el-card class="path-card" shadow="never">
          <!-- 路径头部：目标、画像、时间等 -->
          <div class="path-header">
            <div class="header-left">
              <h2>个性化学习路径</h2>
              <div v-if="pathLoading" style="padding: 20px 0; color: #909399;">⏳ 正在生成学习路径...</div>
              <div v-else-if="pathError" style="padding: 20px 0; color: #f56c6c;">
                学习路径加载失败
                <el-button type="primary" link @click="loadPath">重新加载</el-button>
              </div>
              <div class="meta">
                <span class="label">目标：</span>
                <span class="value">{{ goalText }}</span>
                                <el-button type="text" size="small" style="margin-left: 12px" @click="handleRegenerate" :loading="pathLoading">重新规划路径</el-button>
                <el-button type="text" size="small">导出计划</el-button>
              </div>
            </div>
            <div class="header-right">

              <div class="stat">
                <div class="stat-label">预计完成时间</div>
                <div class="stat-value">{{ estimatedCompletion }}</div>
              </div>
            </div>
          </div>
          <el-divider />

          <!-- 学习路径阶段 -->
          <div class="path-stages">
            <el-tabs v-model="activeStage" @tab-click="handleStageChange">
              <el-tab-pane label="今日计划" name="today"></el-tab-pane>
              <el-tab-pane label="本周路径" name="week"></el-tab-pane>
              <el-tab-pane label="考试冲刺" name="exam"></el-tab-pane>
              <el-tab-pane label="实践提升" name="practice"></el-tab-pane>
            </el-tabs>

            <!-- 阶段描述 -->
            <div class="stage-desc">{{ stageDesc }}</div>

            <!-- 推荐学习时段 -->
            <div class="recommend-time">
              <el-icon><Clock /></el-icon> 推荐学习时段：{{ recommendTime }}
            </div>

            <!-- 任务列表 -->
            <div class="task-list">
              <div v-for="(task, idx) in currentTasks" :key="idx" class="task-item" :class="task.status">
                <div class="task-left">
                  <el-checkbox v-model="task.checked" @change="handleTaskCheck(task)">
                    <span class="task-name">{{ task.name }}</span>
                  </el-checkbox>
                  <div class="task-meta">
                    <span class="duration">{{ task.duration }}</span>
                    <el-tag v-if="task.status === 'completed'" type="success" size="small">已完成</el-tag>
                    <el-tag v-else-if="task.status === 'in-progress'" type="warning" size="small">进行中</el-tag>
                    <el-tag v-else type="info" size="small">待开始</el-tag>
                  </div>
                </div>
                <div class="task-right">
                  <el-progress :percentage="task.progress" :stroke-width="6" :show-text="false" />
                  <span class="progress-text">{{ task.progress }}%</span>
                </div>
              </div>
            </div>

            <!-- 整体进度 -->
            <div class="overall-progress">
              <div class="progress-info">
                <span>已完成任务 {{ completedCount }}/{{ totalTasks }}</span>
                <span>总学习时长 {{ totalHours }}小时</span>
              </div>
              <el-progress :percentage="overallProgress" :stroke-width="10" :color="'#409EFF'" />
            </div>

            <!-- 路径调整建议 -->
            <el-card class="suggestion-card" shadow="never">
              <template #header>
                <span>📌 路径调整建议</span>
              </template>
              <div class="suggestion-content">
                <div class="suggestion-item">
                  <span class="reason">{{ suggestions }}</span>
                </div>
              </div>
            </el-card>

            <!-- 应用建议 + 阶段测评 -->
            <el-row :gutter="16">
              <el-col :span="12">
                <el-card class="app-suggestion" shadow="never">
                  <template #header><span>应用建议</span></template>
                  <div class="app-item">{{ applicationAdvice }}</div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card class="stage-test" shadow="never">
                  <template #header><span>阶段测评</span></template>
                  <div class="test-item">{{ examAdvice }}</div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧辅助区 -->
      <el-col :span="8">

        <!-- 学习日历 -->
        <el-card class="calendar-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>学习日历</span>
            <el-button type="text" style="float: right">更多 ></el-button>
          </template>
          <el-calendar v-model="calendarDate">
            <template #date-cell="{ data }">
              <div class="calendar-day" :class="{ completed: isCompletedDay(data.day), planned: isPlannedDay(data.day), needStrengthen: isNeedStrengthenDay(data.day) }">
                {{ data.day.split('-')[2] }}
              </div>
            </template>
          </el-calendar>
        </el-card>

        <!-- 为你推荐的个性化资源 -->

        <!-- 历史错题 -->
        <el-card class="wrong-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>历史错题</span>
            <el-tag v-if="wrongQuestions.length" type="danger" size="small" style="float: right">
              共 {{ wrongQuestions.length }} 题
            </el-tag>
          </template>

          <!-- 加载中 -->
          <div v-if="wrongLoading" class="wrong-loading">加载中...</div>

          <!-- 错题列表 -->
          <div v-else-if="wrongQuestions.length" class="wrong-list">
            <div
              v-for="(q, i) in visibleWrongQuestions"
              :key="q.id"
              class="wrong-item"
              @click="goToWrongDetail(q)"
            >
              <div class="wrong-header">
                <span class="wrong-num">{{ i + 1 }}</span>
                <span class="wrong-question">{{ q.question }}</span>
              </div>
              <div class="wrong-body">
                <div class="wrong-answer wrong-user-answer">
                  <span class="wrong-label">你的答案：</span>
                  <span class="wrong-text">{{ q.userAnswer || '（未作答）' }}</span>
                </div>
                <div class="wrong-answer wrong-correct-answer">
                  <span class="wrong-label">正确答案：</span>
                  <span class="wrong-text">{{ q.correctAnswer }}</span>
                </div>
              </div>
              <div class="wrong-go-hint">查看详情 →</div>
            </div>
            <!-- 查看全部 -->
            <div v-if="wrongQuestions.length > 1" class="wrong-load-more">
              <el-button text size="small" @click.stop="goToList">
                查看全部 {{ wrongQuestions.length }} 题 →
              </el-button>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="wrong-empty">暂无错题记录，继续加油！</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getLearningPath,
  generateLearningPath,
  updateTaskStatus,
  type LearningPathData,
  type LearningPathTask
} from '@/api/learning'
import { getWrongQuestions, type WrongQuestionItem } from '@/api/tutor'

// 阶段
const activeStage = ref('today')
const calendarDate = ref(new Date())
const pathLoading = ref(false)
const pathError = ref(false)

const router = useRouter()

// ===== 后端数据 =====
const pathData = ref<LearningPathData | null>(null)

// ===== 从后端数据映射到模板字段 =====

const stageNameToKey: Record<string, string> = {
  '今日计划': 'today',
  '本周路径': 'week',
  '考试冲刺': 'exam',
  '实践提升': 'practice'
}

const currentTasks = computed(() => {
  if (!pathData.value?.stages) return []
  const currentStage = pathData.value.stages.find(s => stageNameToKey[s.name] === activeStage.value)
  return (currentStage?.tasks || []).map((t: LearningPathTask) => ({
    name: t.title,
    duration: t.duration ? t.duration + '分钟' : '未设置',
    status: t.status === 2 ? 'completed' : t.status === 1 ? 'in-progress' : 'pending',
    progress: t.progress || 0,
    checked: t.status === 2
  }))
})

const totalTasks = computed(() => currentTasks.value.length)
const completedCount = computed(() => currentTasks.value.filter(t => t.status === 'completed').length)
const overallProgress = computed(() => {
  if (totalTasks.value === 0) return 0
  return Math.round((completedCount.value / totalTasks.value) * 100)
})
const totalHours = computed(() => pathData.value?.totalHours || 0)

// 路径头部数据
const goalText = computed(() => pathData.value?.goal || '加载中...')
const estimatedCompletion = computed(() => pathData.value?.estimatedCompletion || '')

// 建议 & 推荐
const suggestions = computed(() => pathData.value?.suggestions || '暂无调整建议')
const applicationAdvice = computed(() => pathData.value?.applicationAdvice || '暂无应用建议')
const examAdvice = computed(() => pathData.value?.examAdvice || '暂无测评建议')
const recommendTime = computed(() => pathData.value?.recommendTime || '暂无推荐时段')

// ===== 历史错题 =====
const wrongQuestions = ref<WrongQuestionItem[]>([])
const wrongLoading = ref(false)

/** 当前显示的错题（只展示最新 1 道） */
const visibleWrongQuestions = computed(() =>
  wrongQuestions.value.slice(0, 1)
)

/** 加载历史错题 */
const loadWrongQuestions = async () => {
  wrongLoading.value = true
  try {
    const data = await getWrongQuestions()
    wrongQuestions.value = data || []
  } catch {
    // 后端不可用时静默失败
  } finally {
    wrongLoading.value = false
  }
}

/** 点击错题跳转到详情页 */
const goToWrongDetail = (q: WrongQuestionItem) => {
  router.push(`/student/wrong-questions/${q.id}`)
}

/** 跳转到错题列表页 */
const goToList = () => {
  router.push('/student/wrong-questions')
}

// ===== 模板字段兼容 =====
const stageDesc = computed(() => {
  if (!pathData.value?.stages?.length) return '基于学习画像、掌握度与目标，为你规划动态学习路线'
  const names = pathData.value.stages.map(s => s.name).join('、')
  return `基于学习画像，当前阶段：${names}`
})

// ===== 操作函数 =====
/** 任务打勾/取消打勾：调后端持久化 + 本地响应式更新 */
const handleTaskCheck = async (checkedTask: { name: string; checked: boolean }) => {
  if (!pathData.value?.stages) return
  const currentKey = stageNameToKey[activeStage.value] || activeStage.value

  // 找到要更新的 stage 名和 task 标题
  let foundStage = ''
  let foundTask = ''
  for (const stage of pathData.value.stages) {
    if (stageNameToKey[stage.name] === currentKey || stage.name === activeStage.value) {
      const hit = stage.tasks?.find(t => t.title === checkedTask.name)
      if (hit) {
        foundStage = stage.name
        foundTask = hit.title
      }
      break
    }
  }

  if (!foundTask) return

  try {
    await updateTaskStatus(foundStage, foundTask, checkedTask.checked)
    if (!pathData.value?.stages) return
    const stage = pathData.value.stages.find(item => item.name === foundStage)
    const task = stage?.tasks?.find(item => item.title === foundTask)
    if (task) {
      task.status = checkedTask.checked ? 2 : 0
      task.progress = checkedTask.checked ? 100 : 0
    }
    ElMessage.success(checkedTask.checked ? '任务已完成' : '任务已恢复为未完成')
  } catch {
    checkedTask.checked = !checkedTask.checked
    ElMessage.error('任务状态更新失败，请稍后重试')
  }
}

const handleStageChange = () => {
  // 切换阶段
}

// ===== 日历 =====
const completedDays = ref<string[]>([])
const plannedDays = ref<string[]>([])
const needStrengthenDays = ref<string[]>([])

const isCompletedDay = (date: string) => completedDays.value.includes(date)
const isPlannedDay = (date: string) => plannedDays.value.includes(date)
const isNeedStrengthenDay = (date: string) => needStrengthenDays.value.includes(date)

// ===== 加载路径数据 =====
const loadPath = async () => {
  pathLoading.value = true
  pathError.value = false
  try {
    const data = await getLearningPath()
    if (data) {
      pathData.value = data
    }
  } catch {
    pathData.value = null
    pathError.value = true
    ElMessage.error('学习路径加载失败，请稍后重试')
  } finally {
    pathLoading.value = false
  }
}

/** 重新规划 */
const handleRegenerate = async () => {
  pathLoading.value = true
  try {
    const data = await generateLearningPath()
    if (data) {
      pathData.value = data
      ElMessage.success('学习路径已重新生成！')
    }
  } catch {
    ElMessage.error('生成失败，请重试')
  } finally {
    pathLoading.value = false
  }
}

onMounted(() => {
  loadPath()
  loadWrongQuestions()
})
</script>

<style scoped>
.learning-path-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.path-card {
  border-radius: 16px;
}
.path-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.header-left h2 {
  margin: 0 0 12px 0;
}
.meta {
  margin-bottom: 8px;
  font-size: 14px;
}
.meta .label {
  color: #909399;
}
.meta .value {
  color: #303133;
  font-weight: 500;
}
.header-right {
  display: flex;
  gap: 24px;
}
.stat {
  text-align: center;
}
.stat-label {
  font-size: 12px;
  color: #909399;
}
.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}
.stat-value .small {
  font-size: 12px;
  font-weight: normal;
}
.stage-desc {
  margin: 12px 0;
  color: #606266;
  font-size: 14px;
}
.recommend-time {
  background: #ecf5ff;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 20px;
  color: #409eff;
  font-size: 14px;
}
.task-list {
  margin-bottom: 20px;
}
.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}
.task-left {
  flex: 1;
}
.task-name {
  margin-left: 8px;
  font-weight: 500;
}
.task-meta {
  margin-left: 28px;
  font-size: 12px;
  color: #909399;
}
.duration {
  margin-right: 12px;
}
.task-right {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 120px;
}
.progress-text {
  font-size: 12px;
  color: #606266;
}
.task-item.completed .task-name {
  text-decoration: line-through;
  color: #909399;
}
.overall-progress {
  margin: 16px 0;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}
.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}
.suggestion-card {
  margin: 16px 0;
  background: #fff9e6;
}
.suggestion-content .suggestion-item {
  font-size: 14px;
}
.suggestion-item .reason {
  color: #e6a23c;
}
.actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.app-suggestion, .stage-test {
  background: #f0f9ff;
}
.app-item, .test-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #dcdfe6;
}
.overview-stats {
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.stat-circle {
  text-align: center;
}
.calendar-day {
  text-align: center;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  line-height: 28px;
  margin: 0 auto;
}
.calendar-day.completed {
  background-color: #67c23a;
  color: white;
}
.calendar-day.planned {
  background-color: #409eff;
  color: white;
}
.calendar-day.needStrengthen {
  background-color: #e6a23c;
  color: white;
}
.resource-list {
  margin-top: 12px;
}
.resource-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}
.res-name {
  font-weight: 500;
  margin-bottom: 4px;
}
.res-meta {
  font-size: 12px;
  color: #909399;
}
/* ===== 任务完成动画 ===== */
.task-item.completed {
  animation: taskComplete 0.6s ease-in-out;
}
.task-item.completed .task-name {
  color: #67c23a !important;
  font-weight: 600;
}
.task-item .task-left .el-checkbox {
  margin-right: 0;
}
.task-item .task-left .el-checkbox .el-checkbox__label {
  font-weight: 500;
}
.task-item.completed .task-left .el-checkbox .el-checkbox__label {
  text-decoration: line-through;
  color: #a8abb2;
}
@keyframes taskComplete {
  0% { background-color: transparent; transform: scale(1); }
  30% { background-color: #f0f9eb; transform: scale(1.01); }
  60% { background-color: #ecfdf3; }
  100% { background-color: transparent; transform: scale(1); }
}

/* ===== 历史错题 ===== */
.wrong-loading {
  text-align: center;
  padding: 20px 0;
  color: #909399;
  font-size: 14px;
}
.wrong-empty {
  text-align: center;
  padding: 20px 0;
  color: #909399;
  font-size: 14px;
}
.wrong-list {
  max-height: 420px;
  overflow-y: auto;
}
.wrong-item {
  background: #f8f9fb;
  padding: 14px;
  border-radius: 8px;
  margin-bottom: 10px;
  border-left: 3px solid #e64553;
}
.wrong-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 10px;
}
.wrong-num {
  display: inline-flex;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e64553;
  color: #fff;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex-shrink: 0;
}
.wrong-question {
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
}
.wrong-body {
  margin-left: 30px;
}
.wrong-answer {
  font-size: 13px;
  margin-bottom: 4px;
}
.wrong-label {
  color: #909399;
  margin-right: 4px;
}
.wrong-user-answer .wrong-text {
  color: #e64553;
  font-weight: 500;
}
.wrong-correct-answer .wrong-text {
  color: #52c41a;
  font-weight: 500;
}
.wrong-load-more {
  text-align: center;
  padding: 8px 0;
}
.wrong-load-more .el-button {
  color: #409eff !important;
  font-size: 12px !important;
}
.wrong-item {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.wrong-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.wrong-go-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #409eff;
  text-align: right;
}
</style>