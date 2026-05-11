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
import type { StudentProfile } from '@/api/profile'

// 雷达图数据（学科能力）
const profile = ref<StudentProfile | null>(null)

const loadStudentProfile = () => {
  const raw = localStorage.getItem('studentProfile')
  if (raw) {
    profile.value = JSON.parse(raw)
  }
}

const getProfileScore = (value?: string) => {
  const scoreMap: Record<string, number> = {
    '基础薄弱': 40,
    '基础一般': 60,
    '基础扎实': 80,
    '基础优秀': 90,

    '未掌握': 30,
    '初步理解': 55,
    '基本掌握': 70,
    '熟练应用': 85,
    '迁移创新': 95,

    '强依赖型': 40,
    '提醒辅助型': 60,
    '半自主型': 75,
    '高度自主型': 90,

    '基础补齐型': 50,
    '稳定提升型': 70,
    '进阶拓展型': 88,
  }

  return value ? scoreMap[value] || 65 : 60
}
const radarChartRef = ref<HTMLElement>()
const radarData = ref([
  { name: '知识基础', value: 60 },
  { name: '目标清晰度', value: 70 },
  { name: '当前掌握度', value: 55 },
  { name: '认知匹配度', value: 70 },
  { name: '错因识别度', value: 65 },
  { name: '学习自主性', value: 60 }
])

// 核心指标（进度条数据）
const coreMetrics = ref([
  { name: '知识基础', value: 60, color: '#409EFF' },
  { name: '学习目标', value: 70, color: '#67C23A' },
  { name: '当前掌握度', value: 55, color: '#E6A23C' },
  { name: '认知风格', value: 70, color: '#F56C6C' },
  { name: '易错点识别', value: 65, color: '#909399' },
  { name: '学习自主性', value: 60, color: '#409EFF' }
])

// 优势与待提升领域
const strengths = ref(['数据结构', '算法思维', '英语阅读'])
const weaknesses = ref(['高等数学', '线性代数', '概率统计'])

// 学习建议（可刷新）
const suggestions = ref([
  '每天专注学习高等数学30分钟，重点突破极限与导数',
  '尝试用思维导图整理线性代数章节框架',
  '参与每周的编程练习，巩固数据结构知识',
  '观看概率论教学视频并完成课后习题'
])

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
const applyProfileToPage = () => {
  if (!profile.value) return

  const p = profile.value

  radarData.value = [
    { name: '知识基础', value: getProfileScore(p.knowledgeBase) },
    { name: '目标清晰度', value: p.learningGoal ? 80 : 50 },
    { name: '当前掌握度', value: getProfileScore(p.masteryLevel) },
    { name: '认知匹配度', value: p.cognitiveStyle ? 78 : 55 },
    { name: '错因识别度', value: p.errorTypes?.length ? 75 : 50 },
    { name: '学习自主性', value: getProfileScore(p.learningAutonomy) }
  ]

  coreMetrics.value = [
    { name: `知识基础：${p.knowledgeBase}`, value: getProfileScore(p.knowledgeBase), color: '#409EFF' },
    { name: `学习目标：${p.learningGoal}`, value: p.learningGoal ? 80 : 50, color: '#67C23A' },
    { name: `当前掌握度：${p.masteryLevel}`, value: getProfileScore(p.masteryLevel), color: '#E6A23C' },
    { name: `认知风格：${p.cognitiveStyle}`, value: p.cognitiveStyle ? 78 : 55, color: '#F56C6C' },
    { name: `易错点：${p.errorTypes?.join('、') || '暂无'}`, value: p.errorTypes?.length ? 75 : 50, color: '#909399' },
    { name: `学习自主性：${p.learningAutonomy}`, value: getProfileScore(p.learningAutonomy), color: '#409EFF' }
  ]

  strengths.value = [
    p.cognitiveStyle,
    p.learningGoal,
    p.knowledgeBase
  ].filter(Boolean)

  weaknesses.value = p.errorTypes?.length ? p.errorTypes : ['暂无明显薄弱点']

  suggestions.value = p.suggestions?.length
    ? p.suggestions
    : [
        `当前综合类型为：${p.overallType}`,
        '建议根据六维画像制定个性化学习计划',
        '建议定期复盘错题并更新学生画像'
      ]
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
      indicator: radarData.value.map(item => ({ name: item.name, max: 100 })),
      shape: 'circle',
      center: ['50%', '50%'],
      radius: '65%',
      name: { textStyle: { fontSize: 12, color: '#666' } }
    },
    series: [{
      type: 'radar',
      data: [{ value: radarData.value.map(item => item.value), name: '当前水平' }],
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
  loadStudentProfile()
  applyProfileToPage()
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