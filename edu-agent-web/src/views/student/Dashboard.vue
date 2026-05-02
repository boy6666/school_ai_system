<template>
  <div class="dashboard-container">
    <!-- 欢迎区域 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-header">
        <div>
          <h2>{{ greeting }}，李明明</h2>
          <p>今天也要继续努力的学习哦！</p>
        </div>
        <el-avatar :size="60" src="https://cube.elemecdn.com/0/88/03b6d3b6a6f4e6b8b6c0e6b4d6b6e6b6.png" />
      </div>
    </el-card>

    <el-row :gutter="20">
      <!-- 左侧：今日任务 + 学习画像雷达图 -->
      <el-col :span="14">
        <el-card class="task-card" shadow="never">
          <template #header>
            <span>今日任务</span>
            <el-button type="text" style="float: right">更多</el-button>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="task in todayTasks"
              :key="task.name"
              :timestamp="task.time"
              placement="top"
            >
              <el-card shadow="hover">
                <h4>{{ task.name }}</h4>
                <p>预计 {{ task.duration }} 分钟</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card class="radar-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>学习画像概览</span>
          </template>
          <div ref="radarChartRef" style="height: 350px; width: 100%"></div>
        </el-card>
      </el-col>

      <!-- 右侧：学习目标 + 学习计划 -->
      <el-col :span="10">
        <el-card class="goal-card" shadow="never">
          <template #header>
            <span>学习目标</span>
          </template>
          <div class="goal-section">
            <div class="goal-item">
              <div class="goal-label">我可以在以下学科中掌握哪些知识？</div>
              <div class="goal-scores">
                <div v-for="subject in subjects" :key="subject.name" class="score-row">
                  <span>{{ subject.name }}</span>
                  <el-rate v-model="subject.score" disabled :colors="colors" />
                </div>
              </div>
            </div>
            <div class="goal-item">
              <div class="goal-label">我的优势：</div>
              <el-tag v-for="tag in strengths" :key="tag" type="success" class="tag">{{ tag }}</el-tag>
            </div>
            <div class="goal-item">
              <div class="goal-label">我的不足：</div>
              <el-tag v-for="tag in weaknesses" :key="tag" type="danger" class="tag">{{ tag }}</el-tag>
            </div>
          </div>
        </el-card>

        <el-card class="plan-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>学习计划</span>
          </template>
          <el-statistic title="你每天的学习时间" :value="dailyStudyTime" suffix="小时" />
          <el-divider />
          <el-statistic title="你的学习状态" :value="studyState" suffix="分">
            <template #suffix>
              <span style="font-size: 14px">/ 5.0</span>
            </template>
          </el-statistic>
          <el-divider />
          <el-statistic title="你的学习成果" :value="studyResult" suffix="分">
            <template #suffix>
              <span style="font-size: 14px">/ 5.0</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as echarts from 'echarts'

// 问候语
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 今日任务 Mock 数据
const todayTasks = ref([
  { name: '高等数学（上）——导数与微分', time: '10:00', duration: 45 },
  { name: '线性代数', time: '10:30', duration: 30 },
  { name: '大学物理（上）——牛顿定律', time: '11:00', duration: 20 }
])

// 雷达图数据
const radarChartRef = ref<HTMLElement>()
const radarData = [
  { name: '高等数学', value: 4.2 },
  { name: '线性代数', value: 3.8 },
  { name: '概率统计', value: 4.5 },
  { name: '数据结构', value: 3.5 },
  { name: '算法', value: 3.9 }
]

// 学习目标学科评分
const subjects = ref([
  { name: '高等数学', score: 4.2 },
  { name: '线性代数', score: 3.8 },
  { name: '概率统计', score: 4.0 },
  { name: '数据结构', score: 3.5 },
  { name: '算法', score: 3.9 }
])
const colors = ['#99A9BF', '#F7BA2A', '#FF9900'] // 进度条颜色

// 优势与不足
const strengths = ref(['线性代数', '概率统计', '数据结构'])
const weaknesses = ref(['算法', '高等数学'])

// 学习计划数据
const dailyStudyTime = ref(4.5)
const studyState = ref(4.0)
const studyResult = ref(4.5)

// 初始化雷达图
const initRadarChart = () => {
  if (!radarChartRef.value) return
  const chart = echarts.init(radarChartRef.value)
  const option = {
    radar: {
      indicator: radarData.map(item => ({ name: item.name, max: 5 })),
      shape: 'circle',
      center: ['50%', '50%'],
      radius: '65%'
    },
    series: [
      {
        type: 'radar',
        data: [{ value: radarData.map(item => item.value), name: '当前能力' }],
        areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' }
      }
    ]
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

onMounted(() => {
  initRadarChart()
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
}
.welcome-header p {
  margin: 0;
  opacity: 0.9;
}
.task-card .el-timeline-item__timestamp {
  font-size: 14px;
  font-weight: bold;
}
.goal-section {
  padding: 0 10px;
}
.goal-item {
  margin-bottom: 20px;
}
.goal-label {
  font-weight: bold;
  margin-bottom: 10px;
}
.score-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.score-row span {
  width: 80px;
  font-size: 14px;
}
.tag {
  margin-right: 8px;
  margin-bottom: 8px;
}
.radar-card,
.goal-card,
.plan-card {
  border-radius: 12px;
}
</style>