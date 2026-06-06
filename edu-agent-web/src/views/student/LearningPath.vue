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
              <div class="meta">
                <span class="label">目标：</span>
                <span class="value">{{ goalText }}</span>
              </div>
              <div class="meta">
                <span class="label">学习画像：</span>
                <span class="value">{{ profileText }}</span>
                <el-button type="text" size="small" style="margin-left: 12px" @click="handleRegenerate" :loading="pathLoading">重新规划路径</el-button>
                <el-button type="text" size="small">导出计划</el-button>
              </div>
            </div>
            <div class="header-right">
              <div class="stat">
                <div class="stat-label">目标掌握度</div>
                <div class="stat-value">{{ targetMastery }}</div>
              </div>
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
        <!-- 当前概览卡片 -->
        <el-card class="overview-card" shadow="never">
          <template #header><span>当前概览</span></template>
          <div class="overview-stats">
            <div class="stat-circle">
              <el-progress type="circle" :percentage="masteryRate" :width="100" :stroke-width="8" color="#409EFF" />
              <div class="stat-label">当前掌握度</div>
            </div>
            <div class="stat-circle">
              <el-progress type="circle" :percentage="learningRateVal" :width="100" :stroke-width="8" color="#F56C6C" />
              <div class="stat-label">学习中</div>
            </div>
            <div class="stat-circle">
              <el-progress type="circle" :percentage="unmasteredRate" :width="100" :stroke-width="8" color="#909399" />
              <div class="stat-label">未掌握</div>
            </div>
          </div>
        </el-card>

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
        <el-card class="resource-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>为你推荐的个性化资源</span>
            <el-button type="text" style="float: right">查看全部资源 ></el-button>
          </template>
          <div class="resource-tabs">
            <el-tabs v-model="activeResourceTab">
              <el-tab-pane label="推荐文档" name="doc"></el-tab-pane>
              <el-tab-pane label="推荐视频" name="video"></el-tab-pane>
              <el-tab-pane label="推荐练习" name="exercise"></el-tab-pane>
              <el-tab-pane label="代码实操" name="code"></el-tab-pane>
            </el-tabs>
          </div>
          <div class="resource-list">
            <div v-for="res in currentResources" :key="res.name" class="resource-item">
              <div class="res-info">
                <div class="res-name">{{ res.name }}</div>
                <div class="res-meta">{{ res.duration }} · {{ res.time }}</div>
              </div>
              <el-button type="primary" size="small" plain>开始学习</el-button>
            </div>
          </div>
        </el-card>

        <!-- 动态调整记录 -->
        <el-card class="adjust-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>动态调整记录</span>
          </template>
          <div class="adjust-list">
            <div v-if="adjustRecords.length === 0" class="adjust-item">
              <div class="adjust-content">暂无调整记录</div>
            </div>
            <div v-for="(record, idx) in adjustRecords" :key="idx" class="adjust-item">
              <div class="adjust-time">{{ record.time }}</div>
              <div class="adjust-content">{{ record.content }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLearningPath, generateLearningPath } from '@/api/learning'
import request from '@/utils/request'

// 阶段
const activeStage = ref('today')
const activeResourceTab = ref('doc')
const calendarDate = ref(new Date())
const pathLoading = ref(false)
const pathError = ref(false)

// ===== 后端数据 =====
const pathData = ref<any>(null)

// ===== 从后端数据映射到模板字段 =====

const stageNameToKey: Record<string, string> = {
  '今日计划': 'today',
  '本周路径': 'week',
  '考试冲刺': 'exam',
  '实践提升': 'practice'
}

const currentTasks = computed(() => {
  if (!pathData.value?.stages) return []
  const currentStage = pathData.value.stages.find((s: any) => stageNameToKey[s.name] === activeStage.value)
  return (currentStage?.tasks || []).map((t: any) => ({
    name: t.title,
    duration: t.duration ? t.duration + '分钟' : '30分钟',
    status: t.status === 2 ? 'completed' : t.status === 1 ? 'in-progress' : 'pending',
    progress: t.progress || 0,
    checked: t.status === 2
  }))
})

const totalTasks = computed(() => currentTasks.value.length)
const completedCount = computed(() => currentTasks.value.filter((t: any) => t.status === 'completed').length)
const overallProgress = computed(() => {
  if (totalTasks.value === 0) return 0
  return Math.round((completedCount.value / totalTasks.value) * 100)
})
const totalHours = computed(() => pathData.value?.totalHours || 0)

// 路径头部数据
const goalText = computed(() => pathData.value?.goal || '加载中...')
const targetMastery = computed(() => pathData.value?.targetMastery || '≥85%')
const estimatedCompletion = computed(() => pathData.value?.estimatedCompletion || '')
const profileText = computed(() => {
  // 从画像数据生成描述
  return '根据画像生成的学习路径'
})

// 建议 & 推荐
const suggestions = computed(() => pathData.value?.suggestions || '暂无调整建议')
const applicationAdvice = computed(() => pathData.value?.applicationAdvice || '暂无应用建议')
const examAdvice = computed(() => pathData.value?.examAdvice || '暂无测评建议')
const recommendTime = computed(() => pathData.value?.recommendTime || '每天 19:00-21:00')

// 概览
const masteryRate = computed(() => pathData.value?.masteryRate || 72)
const learningRateVal = computed(() => pathData.value?.learningRate || 18)
const unmasteredRate = computed(() => pathData.value?.unmasteredRate || 10)

// 推荐资源
const resources = computed(() => {
  const r = pathData.value?.resources || {}
  return {
    doc: r.doc || [],
    video: r.video || [],
    exercise: r.exercise || [],
    code: r.code || []
  }
})
const currentResources = computed(() => resources.value[activeResourceTab.value as keyof typeof resources.value] || [])

// 动态调整记录
const adjustRecords = computed(() => pathData.value?.adjustRecords || [])

// ===== 模板字段兼容 =====
const stageDesc = computed(() => {
  if (!pathData.value?.stages?.length) return '基于学习画像、掌握度与目标，为你规划动态学习路线'
  const names = pathData.value.stages.map((s: any) => s.name).join('、')
  return `基于学习画像，当前阶段：${names}`
})

// ===== 操作函数 =====
/** 任务打勾/取消打勾：直接改 pathData 源数据触发响应式 + 进度条 + 画像更新 */
const handleTaskCheck = (checkedTask: any) => {
  // 找到 pathData 中的原始任务并修改
  if (!pathData.value?.stages) return
  const currentKey = stageNameToKey[activeStage.value] || activeStage.value
  for (const stage of pathData.value.stages) {
    if (stageNameToKey[stage.name] === currentKey || stage.name === activeStage.value) {
      const hit = stage.tasks?.find((t: any) => t.title === checkedTask.name)
      if (hit) {
        if (checkedTask.checked) {
          hit.status = 2
          hit.progress = 100
          ElMessage.success({
            message: `✅ "${hit.title}" 已完成！`,
            duration: 1500,
            offset: 60
          })
          console.log(`[学习路径] ✅ 任务完成: ${hit.title}, 进度: ${completedCount.value}/${totalTasks.value}`)
        } else {
          hit.status = 0
          hit.progress = 0
          console.log(`[学习路径] ↩️ 任务取消: ${hit.title}, 进度: ${completedCount.value}/${totalTasks.value}`)
        }
      }
      break
    }
  }

  // 更新画像
  const userInfoStr = localStorage.getItem('userInfo')
  const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null
  const studentId = Number(userInfo?.id)
  if (studentId) {
    request.post('/profile/save', {
      userId: studentId,
      last_score: Math.round(overallProgress.value)
    }).then(() => {
      console.log(`[学习路径] ✅ 画像已更新: progress=${overallProgress.value}%`)
    }).catch((e: any) => {
      console.warn('[学习路径] ⚠️ 画像更新失败:', e)
    })
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
      console.log('[学习路径] ✅ 从后端加载成功:', data)
    }
  } catch (e) {
    console.warn('[学习路径] ⚠️ 后端不可用，使用 mock 数据:', e)
    pathError.value = true
    // fallback mock 数据保持不变
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
  } catch (e) {
    console.warn('[学习路径] 重新生成失败:', e)
    ElMessage.error('生成失败，请重试')
  } finally {
    pathLoading.value = false
  }
}

onMounted(() => {
  loadPath()
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

.adjust-list {
  max-height: 300px;
  overflow-y: auto;
}
.adjust-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}
.adjust-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.adjust-content {
  font-size: 14px;
}
</style>
