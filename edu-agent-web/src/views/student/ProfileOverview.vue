<template>
  <div class="profile-overview">
    <el-row :gutter="20">
      <!-- 左侧：雷达图 + 核心指标 -->
      <el-col :span="14">
        <el-card class="radar-card" shadow="never">
          <template #header>
            <span>能力雷达图</span>
            <el-tag type="info" size="small">基于近期学习表现</el-tag>
          </template>
          <div ref="radarChartRef" style="height: 400px; width: 100%"></div>
        </el-card>
        <el-card class="core-metrics" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>核心学习指标</span>
          </template>
          <el-row :gutter="20">
            <el-col :span="12" v-for="metric in coreMetrics" :key="metric.name">
              <div class="metric-item">
                <div class="metric-label">{{ metric.name }}</div>
                <el-progress :percentage="metric.value" :stroke-width="10" :color="metric.color" />
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 右侧：优势 + 待提升 + 建议 -->
      <el-col :span="10">
        <el-card class="strength-card" shadow="never">
          <template #header><span>✅ 优势领域</span></template>
          <div class="tag-group">
            <el-tag v-for="tag in strengths" :key="tag" type="success" size="large" effect="plain">{{ tag }}</el-tag>
          </div>
        </el-card>
        <el-card class="weakness-card" shadow="never" style="margin-top: 20px">
          <template #header><span>📌 待提升领域</span></template>
          <div class="tag-group">
            <el-tag v-for="tag in weaknesses" :key="tag" type="danger" size="large" effect="plain">{{ tag }}</el-tag>
          </div>
        </el-card>
        <el-card class="suggestion-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>✨ 个性化学习建议</span>
            <el-button type="text" style="float: right" @click="refreshSuggestions">刷新建议</el-button>
          </template>
          <ul class="suggestion-list">
            <li v-for="(suggestion, idx) in suggestions" :key="idx">{{ suggestion }}</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部：学习进展趋势图 -->
    <el-card class="timeline-card" shadow="never" style="margin-top: 20px">
      <template #header><span>近30天学习进展</span></template>
      <div ref="lineChartRef" style="height: 300px; width: 100%"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

// 雷达图数据（学科能力）
const radarChartRef = ref<HTMLElement>()
const radarData = [
  { name: '高等数学', value: 85 },
  { name: '线性代数', value: 72 },
  { name: '概率论', value: 68 },
  { name: '数据结构', value: 78 },
  { name: '算法', value: 65 },
  { name: '英语', value: 70 }
]

// 核心指标（进度条数据）
const coreMetrics = ref<any[]>([])

// 优势与待提升领域
const strengths = ref<any[]>([])
const weaknesses = ref<any[]>([])

// 学习建议（可刷新）
const suggestions = ref<any[]>([])

const refreshSuggestions = () => {
  // 模拟刷新新建议，实际可调用后端接口
  const newSuggestions = [
    '针对高数薄弱点，推荐“张宇高等数学18讲”',
    '线性代数可通过做历年真题提升',
    '加入学习小组，与同学讨论疑难问题',
    '利用AI智能体进行个性化题目练习'
  ]
  suggestions.value = newSuggestions
}

// 折线图数据（学习进展）
const lineChartRef = ref<HTMLElement>()
const lineData = {
  dates: ['Day1', 'Day5', 'Day10', 'Day15', 'Day20', 'Day25', 'Day30'],
  scores: [65, 68, 72, 70, 75, 78, 82]
}

// 初始化雷达图
const initRadarChart = () => {
  if (!radarChartRef.value) return
  const chart = echarts.init(radarChartRef.value)
  const option = {
    radar: {
      indicator: radarData.map(item => ({ name: item.name, max: 100 })),
      shape: 'circle',
      center: ['50%', '50%'],
      radius: '65%',
      name: { textStyle: { fontSize: 12, color: '#666' } }
    },
    series: [{
      type: 'radar',
      data: [{ value: radarData.map(item => item.value), name: '当前水平' }],
      areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
      lineStyle: { color: '#409EFF', width: 2 },
      itemStyle: { color: '#409EFF' }
    }]
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 初始化折线图
const initLineChart = () => {
  if (!lineChartRef.value) return
  const chart = echarts.init(lineChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: lineData.dates, name: '日期' },
    yAxis: { type: 'value', name: '学习得分', min: 0, max: 100 },
    series: [{
      data: lineData.scores,
      type: 'line',
      smooth: true,
      lineStyle: { color: '#409EFF', width: 3 },
      areaStyle: { color: 'rgba(64, 158, 255, 0.1)' },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { color: '#409EFF' }
    }]
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

onMounted(() => {
  initRadarChart()
  initLineChart()
})
</script>

<style scoped>
.profile-overview {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.radar-card, .core-metrics, .strength-card, .weakness-card, .suggestion-card, .timeline-card {
  border-radius: 16px;
}
.metric-item {
  margin-bottom: 20px;
}
.metric-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}
.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.el-tag {
  font-size: 14px;
  padding: 6px 14px;
}
.suggestion-list {
  padding-left: 20px;
  margin: 0;
}
.suggestion-list li {
  margin-bottom: 12px;
  line-height: 1.5;
  color: #4a5568;
}
</style>