<template>
  <div class="dashboard-container">
    <!-- 顶部：欢迎语 + 搜索 -->
    <div class="dashboard-header">
      <div class="welcome-section">
        <h2>{{ greeting }}，李明明</h2>
        <p>今天也要继续努力的学习哦！</p>
      </div>
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索课程、资料、问题等"
          prefix-icon="Search"
          size="large"
          clearable
        />
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧列：今日任务、学习回顾、学习目标、学习进度 -->
      <el-col :span="12">
        <!-- 今日任务卡片 -->
        <el-card class="task-card" shadow="never">
          <template #header><span>📋 今日任务</span></template>
          <div v-for="task in todayTasks" :key="task.name" class="task-item">
            <div class="task-name">{{ task.name }}</div>
            <div class="task-duration">预计 {{ task.duration }} 分钟</div>
          </div>
        </el-card>

        <!-- 学习回顾卡片 -->
        <el-card class="review-card" shadow="never" style="margin-top: 20px">
          <template #header><span>📚 学习回顾</span></template>
          <div v-for="subject in reviewSubjects" :key="subject.name" class="review-item">
            <span>{{ subject.name }}</span>
            <span>{{ subject.hours }} 小时</span>
          </div>
        </el-card>

        <!-- 学习目标卡片 -->
        <el-card class="goal-card" shadow="never" style="margin-top: 20px">
          <template #header><span>🎯 学习目标</span></template>
          <div class="goal-stats">
            <div class="stat">
              <div class="stat-value">{{ totalHours }}</div>
              <div class="stat-label">总学习时长(小时)</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ completedTopics }}</div>
              <div class="stat-label">完成主题数</div>
            </div>
          </div>
        </el-card>

        <!-- 学习进度卡片 -->
        <el-card class="progress-card" shadow="never" style="margin-top: 20px">
          <template #header><span>📈 学习进度</span></template>
          <div v-for="subject in learningProgress" :key="subject.name" class="progress-item">
            <div class="progress-label">{{ subject.name }}</div>
            <el-progress :percentage="subject.percent" :stroke-width="10" />
            <div class="progress-hours">{{ subject.hours }}小时</div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧列：学习总结、学习评价、学习节奏、学习计划 -->
      <el-col :span="12">
        <!-- 学习总结卡片 -->
        <el-card class="summary-card" shadow="never">
          <template #header><span>📝 学习总结</span></template>
          <div class="summary-grid">
            <div class="summary-item">
              <div class="summary-label">知识图谱</div>
              <el-progress :percentage="65" :stroke-width="8" />
            </div>
            <div class="summary-item">
              <div class="summary-label">学习笔记</div>
              <el-progress :percentage="72" :stroke-width="8" />
            </div>
            <div class="summary-item">
              <div class="summary-label">学习反思</div>
              <el-progress :percentage="80" :stroke-width="8" />
            </div>
            <div class="summary-item">
              <div class="summary-label">学习收获</div>
              <el-progress :percentage="70" :stroke-width="8" />
            </div>
            <div class="summary-item">
              <div class="summary-label">学习感悟</div>
              <el-progress :percentage="65" :stroke-width="8" />
            </div>
          </div>
        </el-card>

        <!-- 学习评价卡片 -->
        <el-card class="evaluation-card" shadow="never" style="margin-top: 20px">
          <template #header><span>⭐ 学习评价</span></template>
          <div class="eval-list">
            <div class="eval-item"><span class="eval-label">学习目标：</span><span>个人能力提升，考研深造</span></div>
            <div class="eval-item"><span class="eval-label">认知风格：</span><span>偏重逻辑分析</span></div>
            <div class="eval-item"><span class="eval-label">薄弱环节：</span><span>数据挖掘，线性代数</span></div>
            <div class="eval-item"><span class="eval-label">兴趣偏好：</span><span>人工智能，算法与编程</span></div>
          </div>
        </el-card>

        <!-- 学习节奏卡片 -->
        <el-card class="rhythm-card" shadow="never" style="margin-top: 20px">
          <template #header><span>⏱️ 学习节奏</span></template>
          <div class="rhythm-stats">
            <div class="rhythm-item">
              <div class="rhythm-value">1-3 小时</div>
              <div class="rhythm-label">每日学习量</div>
            </div>
            <div class="rhythm-item">
              <div class="rhythm-value">2-4 小时</div>
              <div class="rhythm-label">每日学习时长</div>
            </div>
          </div>
        </el-card>

        <!-- 学习计划卡片 -->
        <el-card class="plan-card" shadow="never" style="margin-top: 20px">
          <template #header><span>📅 学习计划</span></template>
          <div class="plan-stats">
            <div class="plan-item">
              <div class="plan-value">1-3 小时</div>
              <div class="plan-label">每日学习计划</div>
            </div>
            <div class="plan-item">
              <div class="plan-value">2-4 小时</div>
              <div class="plan-label">每日学习时间</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { getAdminStats } from '@/api/admin'
import { ref, computed, onMounted } from 'vue'

const searchKeyword = ref('')
const dashboardStats = ref({ totalUsers:0, activeUsers:0, totalConversations:0 })

// 问候语



const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 今日任务 Mock
const todayTasks = ref<any[]>([])
// 学习回顾
const reviewSubjects = ref([
  { name: '高数', hours: 72 },
  { name: '概率论', hours: 70 },
  { name: '线性代数', hours: 60 }
])

// 学习目标
const totalHours = ref(12.6)
const completedTopics = ref(2)

// 学习进度
const learningProgress = ref([
  { name: '高等数学', percent: 65, hours: 10 },
  { name: '概率论', percent: 70, hours: 8 },
  { name: '线性代数', percent: 60, hours: 7 },
  { name: '大学物理', percent: 50, hours: 6 }
])
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.welcome-section h2 {
  margin: 0 0 6px 0;
  font-size: 22px;
}
.welcome-section p {
  margin: 0;
  color: #666;
}
.search-section {
  width: 300px;
}
.task-card, .review-card, .goal-card, .progress-card,
.summary-card, .evaluation-card, .rhythm-card, .plan-card {
  border-radius: 16px;
}
.task-item, .review-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.task-item:last-child, .review-item:last-child {
  border-bottom: none;
}
.task-name {
  font-weight: 500;
}
.task-duration, .review-item span:last-child {
  color: #909399;
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
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 6px;
}
.progress-item {
  margin-bottom: 18px;
}
.progress-label {
  margin-bottom: 8px;
  font-weight: 500;
}
.progress-hours {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.summary-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.summary-item {
  width: calc(50% - 8px);
}
.summary-label {
  margin-bottom: 6px;
  font-size: 14px;
}
.eval-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.eval-item {
  font-size: 14px;
}
.eval-label {
  font-weight: 600;
  color: #606266;
  width: 90px;
  display: inline-block;
}
.rhythm-stats, .plan-stats {
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.rhythm-value, .plan-value {
  font-size: 24px;
  font-weight: bold;
  color: #e6a23c;
}
.rhythm-label, .plan-label {
  font-size: 14px;
  color: #909399;
  margin-top: 6px;
}
</style>