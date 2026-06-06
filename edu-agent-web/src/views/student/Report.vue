<template>
  <div class="report-page">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">总学习时长</div>
            <div class="stat-value">{{ totalHours }}<span class="unit">小时</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">学习进度</div>
            <div class="stat-value">{{ progress }}<span class="unit">%</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">综合评分</div>
            <div class="stat-value">{{ score }}<span class="unit">分</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header><span>学习时长趋势</span></template>
          <div ref="trendChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header><span>能力雷达图</span></template>
          <div ref="radarEl" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="12">
        <el-card class="content-card">
          <template #header><span>模块学习时长</span></template>
          <div v-for="m in modules" :key="m.module" class="progress-item">
            <div class="progress-info">
              <span class="subject-name">{{ labelMap[m.module] || m.module }}</span>
              <span>{{ Math.round(m.total / 60) }} 分钟</span>
            </div>
          </div>
          <div v-if="!modules.length" style="color:#999;text-align:center;padding:20px">暂无数据</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="content-card">
          <template #header><span>学习建议</span></template>
          <div class="suggestion-list">
            <div v-if="suggestion" class="suggestion-item">{{ suggestion }}</div>
            <div v-else style="color:#999;text-align:center;padding:20px">暂无建议</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const trendChart = ref<HTMLElement>()
const radarEl = ref<HTMLElement>()

const totalHours = ref(0)
const progress = ref(0)
const score = ref(0)
const modules = ref<any[]>([])
const trend = ref<any[]>([])
const suggestion = ref('')
const profileData = ref<any>(null)

const labelMap: Record<string, string> = { mindmap: '思维导图', quiz: '练习题目', reading: '拓展阅读', code: '代码案例' }

const initTrend = () => {
  if (!trendChart.value || !trend.value.length) return
  const chart = echarts.init(trendChart.value)
  const days = [...new Set(trend.value.map((t: any) => t.day))].sort()
  const data = days.map(d => {
    const vals = trend.value.filter((t: any) => t.day === d)
    return vals.reduce((sum: number, v: any) => sum + Math.round((v.total || 0) / 60), 0)
  })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: days.map((d: string) => d.slice(5)) },
    yAxis: { type: 'value', name: '分钟' },
    series: [{ type: 'line', data, smooth: true, areaStyle: { color: 'rgba(64,158,255,0.2)' }, itemStyle: { color: '#409eff' } }]
  })
}

const initRadar = () => {
  if (!radarEl.value || !profileData.value) return
  const dims = profileData.value
  const axes = ['知识掌握度', '目标清晰度', '认知适配', '错误规避', '学习自主', '综合能力']
  const keys = ['knowledge_mastery', 'learning_goal_clarity', 'cognitive_adaptation', 'mistake_avoidance', 'learning_autonomy', 'overall_level']
  const vals = keys.map(k => dims[k]?.score || 0)
  if (vals.every((v: number) => v === 0)) return
  const chart = echarts.init(radarEl.value)
  chart.setOption({
    radar: { indicator: axes.map(n => ({ name: n, max: 100 })), shape: 'polygon' },
    series: [{ type: 'radar', data: [{ value: vals, areaStyle: { color: 'rgba(64,158,255,0.2)' }, lineStyle: { color: '#409eff' }, itemStyle: { color: '#409eff' } }] }]
  })
}

onMounted(async () => {
  try {
    const res: any = await request.get('/dashboard/report')
    if (res) {
      const sec = res.totalSec || 0
      totalHours.value = Math.round(sec / 3600 * 10) / 10
      progress.value = res.progress || 0
      score.value = res.score || 0
      modules.value = Array.isArray(res.modules) ? res.modules : []
      trend.value = Array.isArray(res.trend) ? res.trend : []
      suggestion.value = res.last_suggestion || ''
      if (res.profile_data) profileData.value = res.profile_data
    }
  } catch {}
  await nextTick()
  initTrend()
  initRadar()
})
</script>

<style scoped>
.report-page { padding: 20px; }
.stats-row { margin-bottom: 20px; }
.stat-card { border-radius: 8px; text-align: center; }
.stat-content { padding: 20px 0; }
.stat-label { font-size: 14px; color: #666; margin-bottom: 10px; }
.stat-value { font-size: 32px; font-weight: bold; color: #333; }
.stat-value .unit { font-size: 16px; color: #999; margin-left: 5px; }
.charts-row { margin-bottom: 20px; }
.chart-card { border-radius: 8px; min-height: 350px; }
.chart-container { width: 100%; height: 300px; }
.content-row { margin-bottom: 20px; }
.content-card { border-radius: 8px; min-height: 200px; }
.progress-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; }
.progress-item:last-child { border-bottom: none; }
.progress-info { display: flex; justify-content: space-between; width: 100%; }
.subject-name { font-weight: 500; color: #333; }
.suggestion-list { padding: 10px 0; }
.suggestion-item { padding: 15px; background: #f5f7fa; border-radius: 8px; color: #666; line-height: 1.7; }
</style>
