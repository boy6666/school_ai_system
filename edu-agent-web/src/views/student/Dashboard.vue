<template>
  <div class="dashboard-container">
    <!-- 欢迎区域 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-header">
        <div>
          <h2>{{ greeting }}，{{ userName }}</h2>
          <p>今天也要继续努力的学习哦！</p>
        </div>
        <el-avatar :size="60" src="https://cube.elemecdn.com/0/88/03b6d3b6a6f4e6b8b6c0e6b4d6b6e6b6.png" />
      </div>
    </el-card>

    <el-row :gutter="20" v-loading="loading">
      <!-- 左侧：今日任务 + 学习回顾 + 学习目标 + 学习进度 -->
      <el-col :span="14">
        <!-- 今日任务卡片 -->
        <el-card class="task-card" shadow="never">
          <template #header>
            <span>今日任务</span>
            <el-button type="text" style="float: right" @click="$router.push('/student/tasks')">更多</el-button>
          </template>
          <el-timeline>
            <el-timeline-item v-for="task in todayTasks" :key="task.name" :timestamp="task.duration > 0 ? task.duration + '分钟' : ''" placement="top">
              <el-card shadow="hover"><h4>{{ task.name }}</h4></el-card>
            </el-timeline-item>
            <el-timeline-item v-if="todayTasks.length === 0">
              <el-card shadow="hover"><h4>暂无今日任务，去「学习任务」页面创建吧</h4></el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 学习回顾卡片 -->
        <el-card class="review-card" shadow="never" style="margin-top: 20px">
          <template #header><span>学习回顾</span></template>
          <div v-if="reviewSubjects.length === 0">暂无学习回顾数据</div>
          <div v-else v-for="sub in reviewSubjects" :key="sub.name" class="review-item">
            <span>{{ sub.name }}</span><span>{{ sub.hours }} 小时</span>
          </div>
        </el-card>

        <!-- 学习目标卡片 -->
        <el-card class="goal-card" shadow="never" style="margin-top: 20px">
          <template #header><span>学习目标</span></template>
          <div class="goal-stats" v-if="goal.totalHours || goal.completedTopics">
            <div class="stat"><div class="stat-value">{{ goal.totalHours || 0 }}</div><div class="stat-label">总学习时长(小时)</div></div>
            <div class="stat"><div class="stat-value">{{ goal.completedTopics || 0 }}</div><div class="stat-label">完成主题数</div></div>
          </div>
          <div v-else>暂无学习目标数据</div>
        </el-card>

        <!-- 学习进度卡片 -->
        <el-card class="progress-card" shadow="never" style="margin-top: 20px">
          <template #header><span>学习进度</span></template>
          <div v-if="learningProgress.length === 0">暂无学习进度数据</div>
          <div v-else v-for="sub in learningProgress" :key="sub.name" class="progress-item">
            <div class="progress-label">{{ sub.name }}</div>
            <el-progress :percentage="sub.percent" :stroke-width="10" />
            <div class="progress-hours">{{ sub.hours }}小时</div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：学习总结 + 学习评价 + 学习节奏 + 学习计划 -->
      <el-col :span="10">
        <!-- 学习总结卡片 -->
        <el-card class="summary-card" shadow="never">
          <template #header><span>学习总结</span></template>
          <div v-if="summary.knowledgeGraph || summary.notes || summary.reflection || summary.harvest || summary.insight">
            <div class="summary-grid">
              <div class="summary-item"><div class="summary-label">知识图谱</div><el-progress :percentage="summary.knowledgeGraph" :stroke-width="8" /></div>
              <div class="summary-item"><div class="summary-label">学习笔记</div><el-progress :percentage="summary.notes" :stroke-width="8" /></div>
              <div class="summary-item"><div class="summary-label">学习反思</div><el-progress :percentage="summary.reflection" :stroke-width="8" /></div>
              <div class="summary-item"><div class="summary-label">学习收获</div><el-progress :percentage="summary.harvest" :stroke-width="8" /></div>
              <div class="summary-item"><div class="summary-label">学习感悟</div><el-progress :percentage="summary.insight" :stroke-width="8" /></div>
            </div>
          </div>
          <div v-else>暂无学习总结数据</div>
        </el-card>

        <!-- 学习评价卡片 -->
        <el-card class="evaluation-card" shadow="never" style="margin-top: 20px">
          <template #header><span>学习评价</span></template>
          <div v-if="evaluation.goal || evaluation.cognitiveStyle || evaluation.weakPoints || evaluation.interestPreference">
            <div class="eval-list">
              <div class="eval-item"><span class="eval-label">学习目标：</span>{{ evaluation.goal }}</div>
              <div class="eval-item"><span class="eval-label">认知风格：</span>{{ evaluation.cognitiveStyle }}</div>
              <div class="eval-item"><span class="eval-label">薄弱环节：</span>{{ evaluation.weakPoints }}</div>
              <div class="eval-item"><span class="eval-label">兴趣偏好：</span>{{ evaluation.interestPreference }}</div>
            </div>
          </div>
          <div v-else>暂无学习评价数据</div>
        </el-card>

        <!-- 学习节奏卡片 -->
        <el-card class="rhythm-card" shadow="never" style="margin-top: 20px">
          <template #header><span>学习节奏</span></template>
          <div v-if="rhythm.dailyAmount || rhythm.dailyDuration">
            <div class="rhythm-stats">
              <div class="rhythm-item"><div class="rhythm-value">{{ rhythm.dailyAmount }}</div><div class="rhythm-label">每日学习量</div></div>
              <div class="rhythm-item"><div class="rhythm-value">{{ rhythm.dailyDuration }}</div><div class="rhythm-label">每日学习时长</div></div>
            </div>
          </div>
          <div v-else>暂无学习节奏数据</div>
        </el-card>

        <!-- 学习计划卡片 -->
        <el-card class="plan-card" shadow="never" style="margin-top: 20px">
          <template #header><span>学习计划</span></template>
          <div v-if="plan.dailyPlan || plan.dailyTime">
            <div class="plan-stats">
              <div class="plan-item"><div class="plan-value">{{ plan.dailyPlan }}</div><div class="plan-label">每日学习计划</div></div>
              <div class="plan-item"><div class="plan-value">{{ plan.dailyTime }}</div><div class="plan-label">每日学习时间</div></div>
            </div>
          </div>
          <div v-else>暂无学习计划数据</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getDashboardStats } from '@/api/dashboard'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.name || userStore.userInfo?.username || '学生')
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const loading = ref(true)
const todayTasks = ref([])
const reviewSubjects = ref([])
const learningProgress = ref([])
const goal = ref({})
const summary = ref({})
const evaluation = ref({})
const rhythm = ref({})
const plan = ref({})

const fetchDashboard = async () => {
  try {
    const res = await getDashboardStats()
    if (res.code === 200) {
      const data = res.data
      todayTasks.value = data.todayTasks || []
      reviewSubjects.value = data.reviewSubjects || []
      learningProgress.value = data.learningProgress || []
      goal.value = data.goal || {}
      summary.value = data.summary || {}
      evaluation.value = data.evaluation || {}
      rhythm.value = data.rhythm || {}
      plan.value = data.plan || {}
    } else {
      ElMessage.error('加载数据失败')
    }
  } catch (error) {
    console.error('Dashboard加载失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.welcome-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}
.welcome-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.welcome-header h2 {
  margin: 0 0 8px 0;
  color: white;
  font-size: 24px;
  font-weight: 600;
}
.welcome-header p {
  margin: 0;
  opacity: 0.9;
}
.task-card .el-timeline-item__timestamp {
  font-size: 14px;
  font-weight: bold;
}
.review-item, .progress-item {
  margin-bottom: 12px;
}
.goal-stats {
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}
.progress-label {
  margin-bottom: 6px;
}
.summary-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.summary-item {
  width: calc(50% - 6px);
}
.eval-list .eval-item {
  margin-bottom: 8px;
}
.eval-label {
  font-weight: 600;
  width: 90px;
  display: inline-block;
}
.rhythm-stats, .plan-stats {
  display: flex;
  justify-content: space-around;
}
.rhythm-value, .plan-value {
  font-size: 24px;
  font-weight: bold;
  color: #e6a23c;
}
</style>
