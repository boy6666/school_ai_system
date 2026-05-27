<template>
  <div class="learning-path-container" v-loading="loading">
    <el-result v-if="error" icon="error" title="加载失败" sub-title="请稍后重试">
      <template #extra><el-button type="primary" @click="fetchPath">重试</el-button></template>
    </el-result>
    <div v-else>
      <!-- 头部信息 -->
      <el-card class="path-card" shadow="never">
        <div class="path-header">
          <div class="header-left">
            <h2>个性化学习路径</h2>
            <div class="meta"><span class="label">目标：</span><span class="value">{{ pathData.goal }}</span></div>
            <div class="meta"><span class="label">学习画像：</span><span class="value">逻辑分析型 · 基础较弱 · 目标导向</span></div>
          </div>
          <div class="header-right">
            <div class="stat"><div class="stat-label">目标掌握度</div><div class="stat-value">{{ pathData.targetMastery }}</div></div>
            <div class="stat"><div class="stat-label">预计完成时间</div><div class="stat-value">{{ pathData.estimatedCompletion }}</div></div>
          </div>
        </div>
        <el-divider />
        <div class="path-stages">
          <el-tabs v-model="activeStageName" @tab-click="handleStageChange">
            <el-tab-pane v-for="stage in stages" :key="stage.name" :label="stage.name" :name="stage.name"></el-tab-pane>
          </el-tabs>
          <div class="stage-desc">基于学习画像、掌握度与目标，为你规划动态学习路线</div>
          <div class="recommend-time"><el-icon><Clock /></el-icon> 推荐学习时段：每天 19:00-21:00</div>

          <!-- 当前阶段任务列表 -->
          <div class="task-list">
            <div v-for="task in currentTasks" :key="task.title" :class="['task-item', getTaskClass(task)]">
              <div class="task-left">
                <el-checkbox v-model="task.checked" @change="handleTaskCheck(task)" :disabled="task.status === 2">
                  <span class="task-name">{{ task.title }}</span>
                </el-checkbox>
                <div class="task-meta">
                  <span class="duration">{{ task.duration }}分钟</span>
                  <el-tag v-if="task.status === 2" type="success" size="small">已完成</el-tag>
                  <el-tag v-else-if="task.status === 1" type="warning" size="small">进行中</el-tag>
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
              <span>总学习时长 {{ pathData.totalHours }}小时</span>
            </div>
            <el-progress :percentage="overallProgress" :stroke-width="10" color="#409EFF" />
          </div>

          <!-- 调整建议（简化，后续可动态）-->
          <el-card class="suggestion-card" shadow="never">
            <template #header><span>��� 路径调整建议</span></template>
            <div class="suggestion-content">
              <div class="suggestion-item">
                <span class="reason">根据当前学习进度，建议增加编程练习环节。</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentPath } from '@/api/learningPath'

const loading = ref(true)
const error = ref(false)
const pathData = ref({})
const stages = ref([])
const activeStageName = ref('')

const currentTasks = computed(() => {
  const stage = stages.value.find(s => s.name === activeStageName.value)
  return stage ? stage.tasks : []
})
const totalTasks = computed(() => stages.value.reduce((sum, s) => sum + s.tasks.length, 0))
const completedCount = computed(() => stages.value.reduce((sum, s) => sum + s.tasks.filter(t => t.status === 2).length, 0))
const overallProgress = computed(() => totalTasks.value === 0 ? 0 : (completedCount.value / totalTasks.value) * 100)

const getTaskClass = (task) => {
  if (task.status === 2) return 'completed'
  if (task.status === 1) return 'in-progress'
  return 'pending'
}

const handleTaskCheck = (task) => {
  if (!task.checked) return
  ElMessage.info(`任务 "${task.title}" 标记为已完成`)
  task.status = 2
  task.progress = 100
  // 可选：调用后端接口同步状态
}

const handleStageChange = () => {}

const fetchPath = async () => {
  loading.value = true
  error.value = false
  try {
    const res = await getCurrentPath()
    if (res.code === 200) {
      pathData.value = res.data
      stages.value = res.data.stages || []
      if (stages.value.length > 0) activeStageName.value = stages.value[0].name
    } else {
      throw new Error(res.message || '加载失败')
    }
  } catch (err) {
    console.error(err)
    error.value = true
    ElMessage.error('获取学习路径失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchPath)
</script>

<style scoped>
/* 复用之前的样式，此处省略，实际运行时可保留原有样式 */
</style>
<style scoped>
/* 原有样式保持不变，仅增强 .path-header 区域 */
.path-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f4ff 100%);
  padding: 20px 24px;
  border-radius: 16px;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.2);
}
.header-left h2 {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a2a3a;
  position: relative;
  display: inline-block;
}
.header-left h2::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 0;
  width: 60px;
  height: 3px;
  background: #409eff;
  border-radius: 2px;
}
.meta {
  margin-bottom: 10px;
  font-size: 15px;
}
.meta .label {
  color: #606266;
  font-weight: 500;
}
.meta .value {
  color: #2c3e50;
  font-weight: 600;
  background: #fff;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
  margin-left: 6px;
}
.header-right .stat {
  background: rgba(255,255,255,0.7);
  padding: 8px 16px;
  border-radius: 24px;
  backdrop-filter: blur(4px);
}
.stat-value {
  font-size: 22px;
  font-weight: bold;
  color: #409eff;
}
</style>
